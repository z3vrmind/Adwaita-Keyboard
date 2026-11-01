package org.gnome.adwaitakeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

enum class ThemeMode(val id: String, val label: String) {
    FOLLOW_SYSTEM("system", "Follow System"),
    LIGHT("light", "Adwaita Light"),
    DARK("dark", "Adwaita Dark");

    companion object {
        fun fromId(id: String): ThemeMode = entries.firstOrNull { it.id == id } ?: FOLLOW_SYSTEM
    }
}

enum class KeyboardLayoutType(val id: String, val label: String) {
    QWERTY("qwerty", "QWERTY (English)"),
    QWERTZ("qwertz", "QWERTZ (German)"),
    AZERTY("azerty", "AZERTY (French)"),
    DVORAK("dvorak", "Dvorak"),
    COLEMAK("colemak", "Colemak");

    companion object {
        fun fromId(id: String): KeyboardLayoutType = entries.firstOrNull { it.id == id } ?: QWERTY
    }
}

enum class OneHandedMode(val id: String, val label: String) {
    OFF("off", "Full Width"),
    LEFT("left", "Left-Handed"),
    RIGHT("right", "Right-Handed");

    companion object {
        fun fromId(id: String): OneHandedMode = entries.firstOrNull { it.id == id } ?: OFF
    }
}

enum class ToolbarAction(
    val id: String,
    val label: String,
    val symbol: String,
    val icon: SymbolicIcon
) {
    UNDO("undo", "Undo", "Undo", SymbolicIcon.UNDO),
    REDO("redo", "Redo", "Redo", SymbolicIcon.REDO),
    COPY("copy", "Copy", "Copy", SymbolicIcon.COPY),
    CUT("cut", "Cut", "Cut", SymbolicIcon.CUT),
    PASTE("paste", "Paste", "Paste", SymbolicIcon.PASTE),
    SELECT_ALL("select_all", "Select All", "All", SymbolicIcon.SELECT_ALL),
    CURSOR_LEFT("cursor_left", "Cursor Left", "<", SymbolicIcon.CURSOR_LEFT),
    CURSOR_RIGHT("cursor_right", "Cursor Right", ">", SymbolicIcon.CURSOR_RIGHT),
    CLIPBOARD("clipboard", "Clipboard", "Clip", SymbolicIcon.CLIPBOARD),
    EMOJI("emoji", "Emoji Picker", "Emoji", SymbolicIcon.EMOJI),
    PRIVATE_MODE("private_mode", "Private Mode", "Priv", SymbolicIcon.PRIVATE_SHIELD),
    ONE_HANDED("one_handed", "One-Handed", "1H", SymbolicIcon.ONE_HANDED),
    SETTINGS("settings", "Settings", "Prefs", SymbolicIcon.SETTINGS);

    companion object {
        fun fromId(id: String): ToolbarAction? = entries.firstOrNull { it.id == id }
        val DEFAULT_PINNED = listOf(CURSOR_LEFT, CURSOR_RIGHT, CLIPBOARD, UNDO)
        val DEFAULT_MENU = entries.toList()
    }
}

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("adwaita_keyboard_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromId(prefs.getString("theme_mode", ThemeMode.FOLLOW_SYSTEM.id) ?: "system")
        set(value) = prefs.edit().putString("theme_mode", value.id).apply()

    var keyboardHeightScale: Float
        get() = prefs.getFloat("keyboard_height_scale", 1.0f).coerceIn(0.8f, 1.35f)
        set(value) = prefs.edit().putFloat("keyboard_height_scale", value.coerceIn(0.8f, 1.35f)).apply()

    var keySizeScale: Float
        get() = prefs.getFloat("key_size_scale", 1.0f).coerceIn(0.85f, 1.15f)
        set(value) = prefs.edit().putFloat("key_size_scale", value.coerceIn(0.85f, 1.15f)).apply()

    var keySpacingDp: Int
        get() = prefs.getInt("key_spacing_dp", 6).coerceIn(2, 10)
        set(value) = prefs.edit().putInt("key_spacing_dp", value.coerceIn(2, 10)).apply()

    var cornerRadiusDp: Int
        get() = prefs.getInt("corner_radius_dp", 10).coerceIn(4, 18)
        set(value) = prefs.edit().putInt("corner_radius_dp", value.coerceIn(4, 18)).apply()

    var toolbarVisible: Boolean
        get() = prefs.getBoolean("toolbar_visible", true)
        set(value) = prefs.edit().putBoolean("toolbar_visible", value).apply()

    var keyBordersVisible: Boolean
        get() = prefs.getBoolean("key_borders_visible_adw", false)
        set(value) = prefs.edit().putBoolean("key_borders_visible_adw", value).apply()

    var keyAnimationsEnabled: Boolean
        get() = prefs.getBoolean("key_animations_enabled", true)
        set(value) = prefs.edit().putBoolean("key_animations_enabled", value).apply()

    var keyPreviewEnabled: Boolean
        get() = prefs.getBoolean("key_preview_enabled", true)
        set(value) = prefs.edit().putBoolean("key_preview_enabled", value).apply()

    var showNumberRow: Boolean
        get() = prefs.getBoolean("show_number_row", true)
        set(value) = prefs.edit().putBoolean("show_number_row", value).apply()

    var oneHandedMode: OneHandedMode
        get() = OneHandedMode.fromId(prefs.getString("one_handed_mode", OneHandedMode.OFF.id) ?: "off")
        set(value) = prefs.edit().putString("one_handed_mode", value.id).apply()

    var layoutType: KeyboardLayoutType
        get() = KeyboardLayoutType.fromId(prefs.getString("layout_type", KeyboardLayoutType.QWERTY.id) ?: "qwerty")
        set(value) = prefs.edit().putString("layout_type", value.id).apply()

    // Typing & Intelligent Touch Areas
    var autocorrectEnabled: Boolean
        get() = prefs.getBoolean("autocorrect_enabled", true)
        set(value) = prefs.edit().putBoolean("autocorrect_enabled", value).apply()

    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean("suggestions_enabled", true)
        set(value) = prefs.edit().putBoolean("suggestions_enabled", value).apply()

    var adaptiveTouchEnabled: Boolean
        get() = prefs.getBoolean("adaptive_touch_enabled", true)
        set(value) = prefs.edit().putBoolean("adaptive_touch_enabled", value).apply()

    var autoCapitalization: Boolean
        get() = prefs.getBoolean("auto_capitalization", true)
        set(value) = prefs.edit().putBoolean("auto_capitalization", value).apply()

    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean("double_space_period", true)
        set(value) = prefs.edit().putBoolean("double_space_period", value).apply()

    var privateModeSession: Boolean
        get() = prefs.getBoolean("private_mode_session", false)
        set(value) = prefs.edit().putBoolean("private_mode_session", value).apply()

    // Haptics and Sound
    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var hapticIntensityMs: Int
        get() = prefs.getInt("haptic_intensity_ms", 18).coerceIn(5, 60)
        set(value) = prefs.edit().putInt("haptic_intensity_ms", value.coerceIn(5, 60)).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", false)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var soundVolumePercent: Int
        get() = prefs.getInt("sound_volume_percent", 45).coerceIn(5, 100)
        set(value) = prefs.edit().putInt("sound_volume_percent", value.coerceIn(5, 100)).apply()

    // Clipboard
    var clipboardHistoryEnabled: Boolean
        get() = prefs.getBoolean("clipboard_history_enabled", true)
        set(value) = prefs.edit().putBoolean("clipboard_history_enabled", value).apply()

    var clipboardAutoExpire: Boolean
        get() = prefs.getBoolean("clipboard_auto_expire", true)
        set(value) = prefs.edit().putBoolean("clipboard_auto_expire", value).apply()

    var clipboardExpirationMinutes: Int
        get() = prefs.getInt("clipboard_expiration_minutes", 60).coerceIn(5, 1440)
        set(value) = prefs.edit().putInt("clipboard_expiration_minutes", value.coerceIn(5, 1440)).apply()

    var clearSystemClipboardOnExpire: Boolean
        get() = prefs.getBoolean("clear_system_clipboard_on_expire", true)
        set(value) = prefs.edit().putBoolean("clear_system_clipboard_on_expire", value).apply()

    // Toolbar customization
    var pinnedToolbarActions: List<ToolbarAction>
        get() {
            val raw = prefs.getString("pinned_toolbar_actions", null)
                ?: return ToolbarAction.DEFAULT_PINNED
            if (raw.isBlank()) return emptyList()
            return raw.split(",").mapNotNull { ToolbarAction.fromId(it.trim()) }
        }
        set(value) {
            prefs.edit().putString("pinned_toolbar_actions", value.joinToString(",") { it.id }).apply()
        }

    var menuToolbarActions: List<ToolbarAction>
        get() {
            val raw = prefs.getString("menu_toolbar_actions", null)
                ?: return ToolbarAction.DEFAULT_MENU
            if (raw.isBlank()) return ToolbarAction.DEFAULT_MENU
            return raw.split(",").mapNotNull { ToolbarAction.fromId(it.trim()) }
        }
        set(value) {
            prefs.edit().putString("menu_toolbar_actions", value.joinToString(",") { it.id }).apply()
        }

    fun restoreDefaultToolbar() {
        pinnedToolbarActions = ToolbarAction.DEFAULT_PINNED
        menuToolbarActions = ToolbarAction.DEFAULT_MENU
    }

    // Personal Dictionary
    fun getPersonalDictionary(): Set<String> {
        return prefs.getStringSet("personal_dictionary", setOf("GNOME", "Adwaita", "libadwaita", "Debian", "Kotlin"))
            ?.toSet() ?: emptySet()
    }

    fun addPersonalWord(word: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val updated = getPersonalDictionary().toMutableSet()
        updated.add(clean)
        prefs.edit().putStringSet("personal_dictionary", updated).apply()
    }

    fun removePersonalWord(word: String) {
        val updated = getPersonalDictionary().toMutableSet()
        updated.remove(word)
        prefs.edit().putStringSet("personal_dictionary", updated).apply()
    }

    // Text Shortcuts
    fun getTextShortcuts(): Map<String, String> {
        val raw = prefs.getString("text_shortcuts_v1", "@@=user@gnome.org\nomw=On my way!\nshrug=¯\\_(ツ)_/¯") ?: ""
        val map = LinkedHashMap<String, String>()
        raw.lines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                val trigger = line.substring(0, idx).trim()
                val expansion = line.substring(idx + 1).trim()
                if (trigger.isNotEmpty() && expansion.isNotEmpty()) {
                    map[trigger] = expansion
                }
            }
        }
        return map
    }

    fun saveTextShortcuts(shortcuts: Map<String, String>) {
        val serialized = shortcuts.entries.joinToString("\n") { "${it.key.replace("=", "")}=${it.value}" }
        prefs.edit().putString("text_shortcuts_v1", serialized).apply()
    }

    // Emoji Skin Tone & Favorites / Recents
    var selectedSkinToneModifier: String
        get() = prefs.getString("emoji_skin_tone", "") ?: ""
        set(value) = prefs.edit().putString("emoji_skin_tone", value).apply()

    fun getFavoriteEmojis(): List<String> {
        val raw = prefs.getString("favorite_emojis", "😀,👍,❤️,🎉,🐧,✨,🔥,🙏") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun toggleFavoriteEmoji(emoji: String): Boolean {
        val current = getFavoriteEmojis().toMutableList()
        val added = if (current.contains(emoji)) {
            current.remove(emoji)
            false
        } else {
            current.add(0, emoji)
            true
        }
        prefs.edit().putString("favorite_emojis", current.joinToString(",")).apply()
        return added
    }

    fun getRecentEmojis(): List<String> {
        val raw = prefs.getString("recent_emojis", "😀,😂,❤️,👍,🎉,🤔,😊,🔥,✨,🐧") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun recordEmojiUsed(emoji: String) {
        if (privateModeSession) return
        val current = getRecentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val trimmed = current.take(32)
        prefs.edit().putString("recent_emojis", trimmed.joinToString(",")).apply()
    }

    fun clearAllEmojiAndPrivateHistory() {
        prefs.edit()
            .remove("recent_emojis")
            .remove("favorite_emojis")
            .remove("personal_dictionary")
            .remove("text_shortcuts_v1")
            .putBoolean("private_mode_session", false)
            .apply()
    }

    fun resetAllToDefaults() {
        prefs.edit().clear().apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
