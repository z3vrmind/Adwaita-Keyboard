package org.gnome.adwaitakeyboard.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences

data class ClipboardEntry(
    val id: Long,
    val previewText: String,
    val fullLength: Int,
    val htmlText: String?,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val isPinned: Boolean
) {
    fun remainingMinutes(nowMillis: Long): Long {
        if (isPinned) return Long.MAX_VALUE
        val diff = expiresAtMillis - nowMillis
        return if (diff <= 0L) 0L else (diff + 59_999L) / 60_000L
    }
}

class ClipboardDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "adwaita_clipboard.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE clipboard_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                html_text TEXT,
                char_length INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                is_pinned INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clip_pinned_created ON clipboard_entries(is_pinned DESC, created_at DESC)")
        db.execSQL("CREATE INDEX idx_clip_expires ON clipboard_entries(is_pinned, expires_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS clipboard_entries")
        onCreate(db)
    }
}

/**
 * Full-featured local Clipboard Manager with:
 * - Individual per-item 1-hour (configurable) automatic expiration for unpinned items.
 * - Permanent Pinned section that survives phone reboots and never expires.
 * - Efficient SQLite storage supporting multiline text and formatting (HTML where provided)
 *   without loading huge strings into memory during list rendering.
 * - Honest integration with Android's ClipboardManager (clearing system clipboard when expired
 *   and permitted by Android's foreground/IME focus rules).
 */
class ClipboardController(
    context: Context,
    private val prefs: KeyboardPreferences
) {
    private val appContext = context.applicationContext
    private val dbHelper = ClipboardDatabaseHelper(appContext)
    private val systemClipboard =
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var onClipboardChangedCallback: (() -> Unit)? = null
    private var suppressNextSystemCapture = false
    private var lastObservedSystemLatestText: String? = null
    private var lastObservedExpireMillis: Long = 0L

    private val primaryClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (suppressNextSystemCapture) {
            suppressNextSystemCapture = false
            return@OnPrimaryClipChangedListener
        }
        syncFromSystemClipboard(allowCapture = prefs.clipboardHistoryEnabled && !prefs.privateModeSession)
    }

    init {
        try {
            systemClipboard.addPrimaryClipChangedListener(primaryClipListener)
        } catch (_: Exception) {
        }
    }

    fun setOnClipboardUpdatedListener(listener: (() -> Unit)?) {
        onClipboardChangedCallback = listener
    }

    /**
     * Sweeps expired unpinned entries from SQLite and, if the system clipboard still holds
     * an expired clip, clears the system clipboard when Android allows the active IME to do so.
     */
    fun purgeExpiredEntries(nowMillis: Long = System.currentTimeMillis()): Int {
        if (!prefs.clipboardAutoExpire) return 0
        val db = dbHelper.writableDatabase
        val deleted = db.delete(
            "clipboard_entries",
            "is_pinned = 0 AND expires_at <= ?",
            arrayOf(nowMillis.toString())
        )

        if (prefs.clearSystemClipboardOnExpire &&
            lastObservedExpireMillis in 1..nowMillis
        ) {
            clearSystemClipboardBestEffort()
            lastObservedExpireMillis = 0L
        }

        if (deleted > 0) {
            onClipboardChangedCallback?.invoke()
        }
        return deleted
    }

    /**
     * Inspects Android's ClipboardManager when the IME starts or receives a clip event.
     * Respects Android 10+ sensitive clip flag (ClipDescription.EXTRA_IS_SENSITIVE).
     */
    fun syncFromSystemClipboard(allowCapture: Boolean) {
        purgeExpiredEntries()
        if (!allowCapture) return

        val clip: ClipData = try {
            systemClipboard.primaryClip ?: return
        } catch (_: SecurityException) {
            return
        } catch (_: Exception) {
            return
        }

        if (clip.itemCount <= 0) return

        // Respect Android 13+ EXTRA_IS_SENSITIVE flag if set on ClipDescription
        val extras = clip.description?.extras
        if (extras != null && extras.getBoolean("android.content.extra.IS_SENSITIVE", false)) {
            return
        }

        val item = clip.getItemAt(0)
        val rawText = item.text?.toString() ?: return
        if (rawText.isBlank()) return

        val html = item.htmlText
        insertOrUpdateClip(rawText, html)
    }

    fun insertOrUpdateClip(text: String, htmlText: String? = null) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val durationMs = prefs.clipboardExpirationMinutes * 60_000L
        val expiresAt = now + durationMs

        lastObservedSystemLatestText = text
        lastObservedExpireMillis = expiresAt

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Check if an identical entry already exists; if so, refresh its timestamp while preserving pin status
            val cursor = db.query(
                "clipboard_entries",
                arrayOf("id", "is_pinned"),
                "content = ?",
                arrayOf(text),
                null,
                null,
                "id DESC",
                "1"
            )
            var existingId: Long? = null
            var existingPinned = false
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(0)
                existingPinned = cursor.getInt(1) == 1
            }
            cursor.close()

            if (existingId != null) {
                val cv = ContentValues().apply {
                    put("created_at", now)
                    put("expires_at", expiresAt)
                    put("is_pinned", if (existingPinned) 1 else 0)
                    if (htmlText != null) put("html_text", htmlText)
                }
                db.update("clipboard_entries", cv, "id = ?", arrayOf(existingId.toString()))
            } else {
                val cv = ContentValues().apply {
                    put("content", text)
                    put("html_text", htmlText)
                    put("char_length", text.length)
                    put("created_at", now)
                    put("expires_at", expiresAt)
                    put("is_pinned", 0)
                }
                db.insert("clipboard_entries", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        onClipboardChangedCallback?.invoke()
    }

    /**
     * Queries clipboard entries using SUBSTR(content, 1, 600) so even multi-megabyte entries
     * never cause OOM or UI lag when browsing or searching the clipboard panel.
     */
    fun queryEntries(searchQuery: String = ""): Pair<List<ClipboardEntry>, List<ClipboardEntry>> {
        purgeExpiredEntries()
        val db = dbHelper.readableDatabase
        val pinnedList = ArrayList<ClipboardEntry>()
        val recentList = ArrayList<ClipboardEntry>()

        val selection: String?
        val selectionArgs: Array<String>?
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isNotEmpty()) {
            selection = "content LIKE ?"
            selectionArgs = arrayOf("%$trimmedQuery%")
        } else {
            selection = null
            selectionArgs = null
        }

        val sql = buildString {
            append("SELECT id, SUBSTR(content, 1, 600), char_length, html_text, created_at, expires_at, is_pinned ")
            append("FROM clipboard_entries ")
            if (selection != null) append("WHERE $selection ")
            append("ORDER BY is_pinned DESC, created_at DESC LIMIT 150")
        }

        val cursor = db.rawQuery(sql, selectionArgs)
        while (cursor.moveToNext()) {
            val entry = ClipboardEntry(
                id = cursor.getLong(0),
                previewText = cursor.getString(1) ?: "",
                fullLength = cursor.getInt(2),
                htmlText = if (cursor.isNull(3)) null else cursor.getString(3),
                createdAtMillis = cursor.getLong(4),
                expiresAtMillis = cursor.getLong(5),
                isPinned = cursor.getInt(6) == 1
            )
            if (entry.isPinned) {
                pinnedList.add(entry)
            } else {
                recentList.add(entry)
            }
        }
        cursor.close()
        return pinnedList to recentList
    }

    /**
     * Loads the complete text of a single entry on-demand when pasting or copying back.
     */
    fun getFullContentById(id: Long): Pair<String, String?>? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "clipboard_entries",
            arrayOf("content", "html_text"),
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        var result: Pair<String, String?>? = null
        if (cursor.moveToFirst()) {
            val content = cursor.getString(0) ?: ""
            val html = if (cursor.isNull(1)) null else cursor.getString(1)
            result = content to html
        }
        cursor.close()
        return result
    }

    fun togglePin(id: Long, currentlyPinned: Boolean) {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val newExpires = now + prefs.clipboardExpirationMinutes * 60_000L
        val cv = ContentValues().apply {
            put("is_pinned", if (currentlyPinned) 0 else 1)
            if (currentlyPinned) {
                // Unpinning starts a fresh 1-hour expiration countdown
                put("expires_at", newExpires)
            }
        }
        db.update("clipboard_entries", cv, "id = ?", arrayOf(id.toString()))
        onClipboardChangedCallback?.invoke()
    }

    fun deleteEntry(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete("clipboard_entries", "id = ?", arrayOf(id.toString()))
        onClipboardChangedCallback?.invoke()
    }

    fun clearAllUnpinned() {
        val db = dbHelper.writableDatabase
        db.delete("clipboard_entries", "is_pinned = 0", null)
        clearSystemClipboardBestEffort()
        onClipboardChangedCallback?.invoke()
    }

    fun copyEntryToSystemClipboard(id: Long): String? {
        val pair = getFullContentById(id) ?: return null
        val (fullText, html) = pair
        suppressNextSystemCapture = true
        try {
            val clip = if (!html.isNullOrBlank()) {
                ClipData.newHtmlText("Adwaita Clipboard", fullText, html)
            } else {
                ClipData.newPlainText("Adwaita Clipboard", fullText)
            }
            systemClipboard.setPrimaryClip(clip)
            lastObservedExpireMillis = System.currentTimeMillis() + prefs.clipboardExpirationMinutes * 60_000L
        } catch (_: Exception) {
            suppressNextSystemCapture = false
        }
        return fullText
    }

    fun clearSystemClipboardBestEffort() {
        try {
            suppressNextSystemCapture = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                systemClipboard.clearPrimaryClip()
            } else {
                systemClipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {
            suppressNextSystemCapture = false
        }
    }

    fun wipeAllClipboardData(clearSystemClipboardToo: Boolean) {
        val db = dbHelper.writableDatabase
        db.delete("clipboard_entries", null, null)
        if (clearSystemClipboardToo) {
            clearSystemClipboardBestEffort()
        }
        onClipboardChangedCallback?.invoke()
    }
}
