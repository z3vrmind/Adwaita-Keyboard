package org.gnome.adwaitakeyboard.ime

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.prefs.ToolbarAction
import org.gnome.adwaitakeyboard.theme.AdwaitaIcons
import org.gnome.adwaitakeyboard.theme.AdwaitaPalette
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

class AdwaitaTopBarView(
    context: Context,
    private val prefs: KeyboardPreferences,
    private val onSuggestionClicked: (String) -> Unit,
    private val onToolbarActionTriggered: (ToolbarAction) -> Unit
) : LinearLayout(context) {

    private var palette: AdwaitaPalette = AdwaitaTheme.resolvePalette(context, prefs)
    private var isActionMenuExpanded = false
    private var currentSuggestions: List<String> = emptyList()
    private var isSensitiveOrPrivateBanner = false

    private val headerStrip = LinearLayout(context)
    private val bottomDivider = View(context)
    private val suggestionContainer = LinearLayout(context)
    private val pinnedToolbarScroll = HorizontalScrollView(context)
    private val pinnedToolbarRow = LinearLayout(context)
    private val popoverGrid = LinearLayout(context)

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var activeCursorRepeatAction: ToolbarAction? = null

    private val cursorRepeatRunnable = object : Runnable {
        override fun run() {
            val action = activeCursorRepeatAction ?: return
            onToolbarActionTriggered(action)
            repeatHandler.postDelayed(this, 65L)
        }
    }

    init {
        orientation = VERTICAL
        headerStrip.orientation = HORIZONTAL
        headerStrip.gravity = Gravity.CENTER_VERTICAL
        headerStrip.setPadding(dp(4), 0, dp(4), 0)

        suggestionContainer.orientation = HORIZONTAL
        suggestionContainer.gravity = Gravity.CENTER_VERTICAL

        pinnedToolbarScroll.isHorizontalScrollBarEnabled = false
        pinnedToolbarRow.orientation = HORIZONTAL
        pinnedToolbarRow.gravity = Gravity.CENTER_VERTICAL
        pinnedToolbarScroll.addView(pinnedToolbarRow)

        popoverGrid.orientation = VERTICAL
        popoverGrid.visibility = View.GONE
        popoverGrid.setPadding(dp(8), dp(6), dp(8), dp(8))

        addView(headerStrip, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        addView(bottomDivider, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        addView(popoverGrid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        rebuildHeaderAndMenu()
    }

    fun updateState(
        suggestions: List<String>,
        isSensitive: Boolean,
        isPrivateMode: Boolean
    ) {
        currentSuggestions = suggestions
        isSensitiveOrPrivateBanner = isSensitive || isPrivateMode
        palette = AdwaitaTheme.resolvePalette(context, prefs)
        rebuildHeaderAndMenu()
    }

    fun closeMenuIfOpen() {
        if (isActionMenuExpanded) {
            isActionMenuExpanded = false
            popoverGrid.visibility = View.GONE
            rebuildHeaderAndMenu()
        }
    }

    private fun rebuildHeaderAndMenu() {
        setBackgroundColor(palette.headerBarBg)
        bottomDivider.setBackgroundColor(palette.headerBarBorder)
        headerStrip.removeAllViews()
        suggestionContainer.removeAllViews()
        pinnedToolbarRow.removeAllViews()
        popoverGrid.removeAllViews()

        if (!prefs.toolbarVisible) {
            headerStrip.visibility = View.GONE
            bottomDivider.visibility = View.GONE
            popoverGrid.visibility = View.GONE
            return
        }
        headerStrip.visibility = View.VISIBLE
        bottomDivider.visibility = View.VISIBLE

        // 1. Symbolic GNOME Popover Menu Button (44dp x 44dp hit target, inset 38x34dp visual pill)
        val menuIcon = if (isActionMenuExpanded) SymbolicIcon.CLOSE else SymbolicIcon.MENU
        val menuFg = if (isActionMenuExpanded) palette.accentText else palette.textPrimary
        val menuBg = if (isActionMenuExpanded) palette.accentBlue else palette.pillBg
        val menuBtn = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageDrawable(AdwaitaIcons.createDrawable(context, menuIcon, 18, menuFg))
            background = insetPillBg(menuBg, dp(8).toFloat(), insetH = dp(3), insetV = dp(5))
            setOnClickListener {
                isActionMenuExpanded = !isActionMenuExpanded
                popoverGrid.visibility = if (isActionMenuExpanded) View.VISIBLE else View.GONE
                rebuildHeaderAndMenu()
            }
        }
        headerStrip.addView(menuBtn, LayoutParams(dp(44), dp(44)))

        // 2. Suggestions Strip (Full 44dp vertical hit height with inset visual chips)
        if (currentSuggestions.isNotEmpty()) {
            currentSuggestions.forEachIndexed { index, word ->
                val chip = TextView(context).apply {
                    text = word
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    typeface = if (index == 0) AdwaitaTheme.ubuntuMedium(context) else AdwaitaTheme.ubuntuRegular(context)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    setTextColor(if (index == 0) palette.textPrimary else palette.textSecondary)
                    gravity = Gravity.CENTER
                    setPadding(dp(10), 0, dp(10), 0)
                    background = insetPillBg(
                        if (index == 0) palette.keyBg else palette.headerBarBg,
                        dp(8).toFloat(),
                        insetH = dp(2),
                        insetV = dp(5)
                    )
                    setOnClickListener { onSuggestionClicked(word) }
                }
                suggestionContainer.addView(chip, LayoutParams(0, dp(44), 1f))
            }
        } else {
            val statusRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
            }
            if (prefs.privateModeSession || isSensitiveOrPrivateBanner) {
                val shieldView = ImageView(context).apply {
                    setImageDrawable(
                        AdwaitaIcons.createDrawable(
                            context,
                            SymbolicIcon.PRIVATE_SHIELD,
                            15,
                            palette.accentBlue
                        )
                    )
                }
                statusRow.addView(shieldView, LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(5) })
            }
            val statusLabel = TextView(context).apply {
                text = when {
                    prefs.privateModeSession -> "Private Session"
                    isSensitiveOrPrivateBanner -> "Protected Field"
                    else -> "Adwaita"
                }
                typeface = AdwaitaTheme.ubuntuMedium(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTextColor(palette.textSecondary)
            }
            statusRow.addView(statusLabel)
            suggestionContainer.addView(
                statusRow,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        }
        headerStrip.addView(suggestionContainer, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        // 3. Pinned Symbolic Toolbar Buttons (42dp x 44dp hit target, inset 36x34dp visual pill)
        for (action in prefs.pinnedToolbarActions) {
            val btn = buildCompactActionButton(action)
            pinnedToolbarRow.addView(btn, LayoutParams(dp(42), dp(44)))
        }
        headerStrip.addView(
            pinnedToolbarScroll,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )

        // 4. Expanded GNOME Popover Action Grid
        if (isActionMenuExpanded) {
            popoverGrid.background = roundedBg(palette.popoverBg, dp(12).toFloat(), palette.popoverBorder)
            val menuActions = prefs.menuToolbarActions
            val chunked = menuActions.chunked(4)
            for (rowActions in chunked) {
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER
                }
                for (action in rowActions) {
                    val itemView = buildMenuGridTile(action)
                    val lp = LayoutParams(0, dp(48), 1f).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                    row.addView(itemView, lp)
                }
                popoverGrid.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private fun buildCompactActionButton(action: ToolbarAction): View {
        val isActivePrivate = action == ToolbarAction.PRIVATE_MODE && prefs.privateModeSession
        val fg = if (isActivePrivate) palette.accentText else palette.textPrimary
        val bg = if (isActivePrivate) palette.accentBlue else palette.pillBg
        return ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageDrawable(AdwaitaIcons.createDrawable(context, action.icon, 18, fg))
            background = insetPillBg(bg, dp(8).toFloat(), insetH = dp(2), insetV = dp(5))
            attachActionBehavior(this, action, closeMenuOnClick = false)
        }
    }

    private fun buildMenuGridTile(action: ToolbarAction): View {
        val isActivePrivate = action == ToolbarAction.PRIVATE_MODE && prefs.privateModeSession
        val fg = if (isActivePrivate) palette.accentText else palette.textPrimary
        val bg = if (isActivePrivate) palette.accentBlue else palette.keyBg

        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = roundedBg(bg, dp(9).toFloat(), palette.keyBorder)

            val iconView = ImageView(context).apply {
                setImageDrawable(AdwaitaIcons.createDrawable(context, action.icon, 15, fg))
            }
            val labelView = TextView(context).apply {
                text = action.label
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                typeface = AdwaitaTheme.ubuntuMedium(context)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setTextColor(fg)
                setPadding(dp(5), 0, 0, 0)
            }
            addView(iconView, LayoutParams(dp(16), dp(16)))
            addView(labelView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

            attachActionBehavior(
                this,
                action,
                closeMenuOnClick = action != ToolbarAction.CURSOR_LEFT &&
                    action != ToolbarAction.CURSOR_RIGHT &&
                    action != ToolbarAction.UNDO &&
                    action != ToolbarAction.REDO
            )
        }
    }

    private fun attachActionBehavior(view: View, action: ToolbarAction, closeMenuOnClick: Boolean) {
        if (action == ToolbarAction.CURSOR_LEFT || action == ToolbarAction.CURSOR_RIGHT) {
            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        onToolbarActionTriggered(action)
                        activeCursorRepeatAction = action
                        repeatHandler.postDelayed(cursorRepeatRunnable, 300L)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        activeCursorRepeatAction = null
                        repeatHandler.removeCallbacks(cursorRepeatRunnable)
                        true
                    }
                    else -> false
                }
            }
        } else {
            view.setOnClickListener {
                onToolbarActionTriggered(action)
                if (closeMenuOnClick) {
                    closeMenuIfOpen()
                } else {
                    rebuildHeaderAndMenu()
                }
            }
        }
    }

    private fun insetPillBg(fillColor: Int, radiusPx: Float, insetH: Int, insetV: Int): InsetDrawable {
        val base = roundedBg(fillColor, radiusPx)
        return InsetDrawable(base, insetH, insetV, insetH, insetV)
    }

    private fun roundedBg(fillColor: Int, radiusPx: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(fillColor)
            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
