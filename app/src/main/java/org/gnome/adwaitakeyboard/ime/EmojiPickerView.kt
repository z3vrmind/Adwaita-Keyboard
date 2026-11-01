package org.gnome.adwaitakeyboard.ime

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.gnome.adwaitakeyboard.emoji.EmojiCategory
import org.gnome.adwaitakeyboard.emoji.EmojiRepository
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.theme.AdwaitaIcons
import org.gnome.adwaitakeyboard.theme.AdwaitaPalette
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

class EmojiPickerView(
    context: Context,
    private val prefs: KeyboardPreferences,
    private val onEmojiSelected: (String) -> Unit,
    private val onBackspaceClicked: () -> Unit,
    private val onBackToKeyboard: () -> Unit
) : LinearLayout(context) {

    private var palette: AdwaitaPalette = AdwaitaTheme.resolvePalette(context, prefs)
    private var selectedCategory: EmojiCategory = EmojiCategory.SMILEYS
    private var searchFilter: String = ""

    private val topBar = LinearLayout(context)
    private val categoryScroll = HorizontalScrollView(context)
    private val categoryRow = LinearLayout(context)
    private val skinToneRow = LinearLayout(context)
    private val gridScroll = ScrollView(context)
    private val gridContainer = LinearLayout(context)

    init {
        orientation = VERTICAL
        setPadding(dp(6), dp(4), dp(6), dp(4))

        topBar.orientation = HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL

        categoryScroll.isHorizontalScrollBarEnabled = false
        categoryRow.orientation = HORIZONTAL
        categoryScroll.addView(categoryRow)

        skinToneRow.orientation = HORIZONTAL
        skinToneRow.gravity = Gravity.CENTER

        gridContainer.orientation = VERTICAL
        gridScroll.addView(gridContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(topBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(36)))
        addView(categoryScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(36)))
        addView(skinToneRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(28)))
        addView(gridScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(172)))

        refreshEmojiView()
    }

    fun refreshEmojiView() {
        palette = AdwaitaTheme.resolvePalette(context, prefs)
        setBackgroundColor(palette.windowBg)

        topBar.removeAllViews()
        val abcBtn = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(4), dp(9), dp(4))
            background = roundedBg(palette.pillBg, dp(8).toFloat())
            addView(
                ImageView(context).apply {
                    setImageDrawable(AdwaitaIcons.createDrawable(context, SymbolicIcon.BACK, 14, palette.textPrimary))
                },
                LayoutParams(dp(14), dp(14))
            )
            addView(
                TextView(context).apply {
                    text = "ABC"
                    typeface = AdwaitaTheme.ubuntuMedium(context)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(palette.textPrimary)
                    setPadding(dp(4), 0, 0, 0)
                }
            )
            setOnClickListener {
                searchFilter = ""
                onBackToKeyboard()
            }
        }

        val catTitle = TextView(context).apply {
            text = if (searchFilter.isNotEmpty()) {
                "Filter: \"$searchFilter\" (Tap to reset)"
            } else {
                selectedCategory.title
            }
            typeface = AdwaitaTheme.ubuntuBold(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
            setOnClickListener {
                if (searchFilter.isNotEmpty()) {
                    searchFilter = ""
                    refreshEmojiView()
                }
            }
        }

        val quickSearchRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        listOf("love", "laugh", "linux").forEach { tag ->
            val active = searchFilter == tag
            val tagChip = TextView(context).apply {
                text = tag
                typeface = AdwaitaTheme.ubuntuMedium(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(if (active) palette.accentText else palette.textSecondary)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = roundedBg(
                    if (active) palette.accentBlue else palette.cardBg,
                    dp(7).toFloat()
                )
                setOnClickListener {
                    searchFilter = if (active) "" else tag
                    refreshEmojiView()
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(26)).apply {
                marginEnd = dp(3)
            }
            quickSearchRow.addView(tagChip, lp)
        }

        val delBtn = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageDrawable(AdwaitaIcons.createDrawable(context, SymbolicIcon.BACKSPACE, 17, palette.textPrimary))
            background = roundedBg(palette.pillBg, dp(8).toFloat())
            setOnClickListener { onBackspaceClicked() }
        }

        topBar.addView(abcBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(30)))
        topBar.addView(catTitle, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(quickSearchRow, LayoutParams(LayoutParams.WRAP_CONTENT, dp(28)))
        topBar.addView(delBtn, LayoutParams(dp(36), dp(30)))

        // Text-based Adwaita Category Switcher Pills (No Emoji Used as UI Icons)
        categoryRow.removeAllViews()
        val shortLabels = mapOf(
            EmojiCategory.RECENT to "Recent",
            EmojiCategory.SMILEYS to "Smileys",
            EmojiCategory.PEOPLE to "People",
            EmojiCategory.ANIMALS to "Nature",
            EmojiCategory.FOOD to "Food",
            EmojiCategory.TRAVEL to "Travel",
            EmojiCategory.ACTIVITIES to "Activity",
            EmojiCategory.OBJECTS to "Objects",
            EmojiCategory.SYMBOLS to "Symbols",
            EmojiCategory.FLAGS to "Flags"
        )
        for (cat in EmojiCategory.entries) {
            val isSelected = cat == selectedCategory && searchFilter.isEmpty()
            val tab = TextView(context).apply {
                text = shortLabels[cat] ?: cat.title
                typeface = AdwaitaTheme.ubuntuMedium(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(if (isSelected) palette.accentText else palette.textPrimary)
                gravity = Gravity.CENTER
                setPadding(dp(11), dp(3), dp(11), dp(3))
                background = roundedBg(
                    if (isSelected) palette.accentBlue else palette.cardBg,
                    dp(8).toFloat()
                )
                setOnClickListener {
                    searchFilter = ""
                    selectedCategory = cat
                    refreshEmojiView()
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(30)).apply {
                marginEnd = dp(4)
            }
            categoryRow.addView(tab, lp)
        }

        // Skin tone selector row (Clean text labels)
        skinToneRow.removeAllViews()
        val currentMod = prefs.selectedSkinToneModifier
        EmojiRepository.skinTones.forEach { (mod, label) ->
            val active = currentMod == mod
            val chip = TextView(context).apply {
                text = label
                typeface = AdwaitaTheme.ubuntuMedium(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTextColor(if (active) palette.accentText else palette.textSecondary)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = roundedBg(
                    if (active) palette.accentBlue else palette.pillBg,
                    dp(6).toFloat()
                )
                setOnClickListener {
                    prefs.selectedSkinToneModifier = mod
                    refreshEmojiView()
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(22)).apply {
                marginEnd = dp(4)
            }
            skinToneRow.addView(chip, lp)
        }

        // Emoji Content Grid
        gridContainer.removeAllViews()
        val emojisToDisplay: List<String> = when {
            searchFilter.isNotEmpty() -> EmojiRepository.search(searchFilter, currentMod)
            selectedCategory == EmojiCategory.RECENT -> {
                val favs = prefs.getFavoriteEmojis()
                val recents = prefs.getRecentEmojis()
                (favs + recents).distinct()
            }
            else -> EmojiRepository.getByCategory(selectedCategory, currentMod)
        }

        val favoritesSet = prefs.getFavoriteEmojis().toSet()
        val rows = emojisToDisplay.chunked(7)
        for (rowList in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            for (cleanEmoji in rowList) {
                val isFav = favoritesSet.contains(cleanEmoji)
                val cell = TextView(context).apply {
                    text = cleanEmoji
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
                    gravity = Gravity.CENTER
                    background = roundedBg(
                        if (isFav && selectedCategory == EmojiCategory.RECENT) palette.cardBg else palette.windowBg,
                        dp(8).toFloat(),
                        if (isFav) palette.accentBlue else null
                    )
                    setOnClickListener {
                        prefs.recordEmojiUsed(cleanEmoji)
                        onEmojiSelected(cleanEmoji)
                    }
                    setOnLongClickListener {
                        prefs.toggleFavoriteEmoji(cleanEmoji)
                        refreshEmojiView()
                        true
                    }
                }
                val lp = LayoutParams(0, dp(42), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                rowLayout.addView(cell, lp)
            }
            val remaining = 7 - rowList.size
            repeat(remaining) {
                rowLayout.addView(View(context), LayoutParams(0, dp(42), 1f))
            }
            gridContainer.addView(rowLayout)
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
