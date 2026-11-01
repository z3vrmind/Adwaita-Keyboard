package org.gnome.adwaitakeyboard.ime

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.gnome.adwaitakeyboard.clipboard.ClipboardController
import org.gnome.adwaitakeyboard.clipboard.ClipboardEntry
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.theme.AdwaitaIcons
import org.gnome.adwaitakeyboard.theme.AdwaitaPalette
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

class ClipboardPanelView(
    context: Context,
    private val prefs: KeyboardPreferences,
    private val clipboardController: ClipboardController,
    private val onPasteText: (String) -> Unit,
    private val onBackToKeyboard: () -> Unit
) : LinearLayout(context) {

    private var palette: AdwaitaPalette = AdwaitaTheme.resolvePalette(context, prefs)
    private var currentSearchQuery: String = ""

    private val headerRow = LinearLayout(context)
    private val searchInput = EditText(context)
    private val scrollView = ScrollView(context)
    private val listContainer = LinearLayout(context)

    init {
        orientation = VERTICAL
        setPadding(dp(8), dp(6), dp(8), dp(6))

        headerRow.orientation = HORIZONTAL
        headerRow.gravity = Gravity.CENTER_VERTICAL

        searchInput.hint = "Filter clipboard history…"
        searchInput.setSingleLine(true)
        searchInput.typeface = AdwaitaTheme.ubuntuRegular(context)
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        searchInput.setPadding(dp(10), dp(6), dp(10), dp(6))
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                refreshEntries()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listContainer.orientation = VERTICAL
        scrollView.addView(listContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(headerRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(38)))
        val searchLp = LayoutParams(LayoutParams.MATCH_PARENT, dp(36)).apply {
            topMargin = dp(4)
            bottomMargin = dp(6)
        }
        addView(searchInput, searchLp)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, dp(190)))

        refreshEntries()
    }

    fun refreshEntries() {
        palette = AdwaitaTheme.resolvePalette(context, prefs)
        setBackgroundColor(palette.windowBg)

        headerRow.removeAllViews()
        val backBtn = buildIconPillButton(SymbolicIcon.BACK, "Keyboard", palette.pillBg, palette.textPrimary) {
            onBackToKeyboard()
        }
        val titleView = TextView(context).apply {
            text = "Clipboard (${prefs.clipboardExpirationMinutes}m Auto-Expire)"
            typeface = AdwaitaTheme.ubuntuBold(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
        }
        val clearBtn = buildIconPillButton(SymbolicIcon.DELETE, "Clear", palette.destructiveRed, palette.accentText) {
            clipboardController.clearAllUnpinned()
            refreshEntries()
        }

        headerRow.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32)))
        headerRow.addView(titleView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(clearBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32)))

        searchInput.setTextColor(palette.textPrimary)
        searchInput.setHintTextColor(palette.textMuted)
        searchInput.background = roundedBg(palette.cardBg, dp(9).toFloat(), palette.keyBorder)

        listContainer.removeAllViews()
        val (pinned, recent) = clipboardController.queryEntries(currentSearchQuery)
        val now = System.currentTimeMillis()

        if (pinned.isEmpty() && recent.isEmpty()) {
            val emptyView = TextView(context).apply {
                text = "Clipboard history is empty.\nUnpinned items expire automatically after ${prefs.clipboardExpirationMinutes} minutes."
                typeface = AdwaitaTheme.ubuntuRegular(context)
                setTextColor(palette.textSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(28), dp(16), dp(28))
            }
            listContainer.addView(emptyView)
            return
        }

        if (pinned.isNotEmpty()) {
            listContainer.addView(buildSectionHeader(SymbolicIcon.PIN, "Pinned (Never Expires)"))
            for (entry in pinned) {
                listContainer.addView(buildEntryCard(entry, now))
            }
        }

        if (recent.isNotEmpty()) {
            listContainer.addView(buildSectionHeader(SymbolicIcon.RECENT_CLOCK, "Recent (Auto-Expires)"))
            for (entry in recent) {
                listContainer.addView(buildEntryCard(entry, now))
            }
        }
    }

    private fun buildSectionHeader(icon: SymbolicIcon, title: String): View {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(4))
            val iv = ImageView(context).apply {
                setImageDrawable(AdwaitaIcons.createDrawable(context, icon, 14, palette.accentBlue))
            }
            val tv = TextView(context).apply {
                text = title
                typeface = AdwaitaTheme.ubuntuBold(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setTextColor(palette.accentBlue)
                setPadding(dp(5), 0, 0, 0)
            }
            addView(iv, LayoutParams(dp(14), dp(14)))
            addView(tv)
        }
    }

    private fun buildEntryCard(entry: ClipboardEntry, nowMillis: Long): View {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = roundedBg(
                palette.cardBg,
                dp(10).toFloat(),
                if (entry.isPinned) palette.accentBlue else palette.keyBorder
            )
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        val textPreview = TextView(context).apply {
            val display = if (entry.fullLength > entry.previewText.length) {
                "${entry.previewText}… (${entry.fullLength} chars)"
            } else {
                entry.previewText
            }
            text = display
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            typeface = AdwaitaTheme.ubuntuRegular(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTextColor(palette.textPrimary)
            setOnClickListener {
                val full = clipboardController.getFullContentById(entry.id)?.first ?: entry.previewText
                onPasteText(full)
            }
        }
        card.addView(textPreview, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val footerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }

        val badge = TextView(context).apply {
            text = if (entry.isPinned) {
                "Pinned • ${entry.fullLength} chars"
            } else {
                "Expires in ${entry.remainingMinutes(nowMillis)}m • ${entry.fullLength} chars"
            }
            typeface = AdwaitaTheme.ubuntuMedium(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(palette.textSecondary)
        }
        footerRow.addView(badge, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val copyBtn = buildSmallActionButton(SymbolicIcon.COPY, "Copy") {
            clipboardController.copyEntryToSystemClipboard(entry.id)
            refreshEntries()
        }
        val pinBtn = buildSmallActionButton(SymbolicIcon.PIN, if (entry.isPinned) "Unpin" else "Pin") {
            clipboardController.togglePin(entry.id, entry.isPinned)
            refreshEntries()
        }
        val delBtn = buildSmallActionButton(SymbolicIcon.DELETE, "") {
            clipboardController.deleteEntry(entry.id)
            refreshEntries()
        }

        footerRow.addView(copyBtn)
        footerRow.addView(pinBtn)
        footerRow.addView(delBtn)
        card.addView(footerRow)

        val cardLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(6)
        }
        card.layoutParams = cardLp
        return card
    }

    private fun buildIconPillButton(
        icon: SymbolicIcon,
        label: String,
        bg: Int,
        fg: Int,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(4), dp(9), dp(4))
            background = roundedBg(bg, dp(8).toFloat())
            val iv = ImageView(context).apply {
                setImageDrawable(AdwaitaIcons.createDrawable(context, icon, 14, fg))
            }
            addView(iv, LayoutParams(dp(14), dp(14)))
            if (label.isNotEmpty()) {
                val tv = TextView(context).apply {
                    text = label
                    typeface = AdwaitaTheme.ubuntuMedium(context)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(fg)
                    setPadding(dp(4), 0, 0, 0)
                }
                addView(tv)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun buildSmallActionButton(
        icon: SymbolicIcon,
        label: String,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = roundedBg(palette.pillBg, dp(6).toFloat())
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(4)
            }
            val iv = ImageView(context).apply {
                setImageDrawable(AdwaitaIcons.createDrawable(context, icon, 13, palette.textPrimary))
            }
            addView(iv, LayoutParams(dp(13), dp(13)))
            if (label.isNotEmpty()) {
                val tv = TextView(context).apply {
                    text = label
                    typeface = AdwaitaTheme.ubuntuMedium(context)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setTextColor(palette.textPrimary)
                    setPadding(dp(4), 0, 0, 0)
                }
                addView(tv)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun roundedBg(fillColor: Int, radiusPx: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(fillColor)
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
