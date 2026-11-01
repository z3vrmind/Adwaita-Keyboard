package org.gnome.adwaitakeyboard.ime

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.theme.AdwaitaIcons
import org.gnome.adwaitakeyboard.theme.AdwaitaPalette
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.theme.SymbolicIcon
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class AdwaitaKeyboardView(
    context: Context,
    private val prefs: KeyboardPreferences,
    private val onKeyTriggered: (KeySpec) -> Unit,
    private val onSpaceCursorSlide: (Int) -> Unit,
    private val onLongPressSpaceLayoutSwitch: () -> Unit
) : View(context) {

    /**
     * Separates visible key geometry (`visualRect`), inner core zone (`coreSafeRect`),
     * and baseline hit-testing geometry (`touchRect`).
     * Visible key positions never move when adaptive touch weights change.
     */
    private data class KeyBox(
        val spec: KeySpec,
        val visualRect: RectF,
        val coreSafeRect: RectF,
        val touchRect: RectF,
        val rowIndex: Int,
        val colIndex: Int
    )

    private var rows: List<List<KeySpec>> = emptyList()
    private val keyBoxes = ArrayList<KeyBox>()
    private var nextCharWeights: Map<Char, Float> = emptyMap()
    private var palette: AdwaitaPalette = AdwaitaTheme.resolvePalette(context, prefs)

    private var activeKeyBox: KeyBox? = null
    private var lastAnimatedKeyBox: KeyBox? = null
    private var animProgress: Float = 0f

    private val pressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 115L
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            animProgress = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }

    private var showAlternativesPopup = false
    private var alternativeRects = ArrayList<Pair<String, RectF>>()
    private var selectedAlternativeIndex = -1

    private var downX = 0f
    private var downY = 0f
    private var isSpaceSliding = false
    private var spaceSlideAccumulatedX = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceRepeating = false

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = AdwaitaTheme.ubuntuRegular(context)
    }
    private val specialTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = AdwaitaTheme.ubuntuMedium(context)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = AdwaitaTheme.ubuntuMedium(context)
    }
    private val previewBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val previewBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }
    private val tempDrawRect = RectF()
    private val tempShadowRect = RectF()

    private val longPressRunnable = Runnable {
        val current = activeKeyBox ?: return@Runnable
        if (current.spec.type == KeyActionType.BACKSPACE) {
            isBackspaceRepeating = true
            scheduleBackspaceRepeat()
        } else if (current.spec.type == KeyActionType.SPACE && !isSpaceSliding) {
            onLongPressSpaceLayoutSwitch()
            updateActiveKey(null)
        } else if (current.spec.longPressAlternatives.isNotEmpty()) {
            showAlternativesPopup = true
            buildAlternativesGeometry(current)
            invalidate()
        } else if (current.spec.secondaryHint.isNotEmpty()) {
            showAlternativesPopup = true
            buildAlternativesGeometry(
                current.copy(spec = current.spec.copy(longPressAlternatives = listOf(current.spec.secondaryHint)))
            )
            invalidate()
        }
    }

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            val current = activeKeyBox
            if (isBackspaceRepeating && current != null && current.spec.type == KeyActionType.BACKSPACE) {
                onKeyTriggered(current.spec)
                handler.postDelayed(this, 55L)
            }
        }
    }

    private fun scheduleBackspaceRepeat() {
        handler.post(backspaceRepeatRunnable)
    }

    private fun updateActiveKey(newKey: KeyBox?) {
        val previous = activeKeyBox
        if (previous === newKey) return
        activeKeyBox = newKey

        if (!prefs.keyAnimationsEnabled) {
            lastAnimatedKeyBox = newKey
            animProgress = if (newKey != null) 1f else 0f
            invalidate()
            return
        }

        pressAnimator.cancel()
        if (newKey != null) {
            lastAnimatedKeyBox = newKey
            pressAnimator.setFloatValues(0.25f, 1f)
            pressAnimator.duration = 95L
            pressAnimator.start()
        } else if (previous != null) {
            lastAnimatedKeyBox = previous
            pressAnimator.setFloatValues(animProgress, 0f)
            pressAnimator.duration = 125L
            pressAnimator.start()
        } else {
            animProgress = 0f
            invalidate()
        }
    }

    /**
     * Updates the invisible adaptive boundary weights for expected next characters
     * without redrawing or shifting any visible key positions.
     */
    fun setNextCharacterWeights(weights: Map<Char, Float>) {
        nextCharWeights = weights
    }

    fun setKeyboardRows(newRows: List<List<KeySpec>>) {
        rows = newRows
        palette = AdwaitaTheme.resolvePalette(context, prefs)
        computeKeyGeometry(width, height)
        requestLayout()
        invalidate()
    }

    fun refreshThemeAndPreferences() {
        palette = AdwaitaTheme.resolvePalette(context, prefs)
        textPaint.typeface = AdwaitaTheme.ubuntuRegular(context)
        specialTextPaint.typeface = AdwaitaTheme.ubuntuMedium(context)
        hintPaint.typeface = AdwaitaTheme.ubuntuMedium(context)
        computeKeyGeometry(width, height)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val baseRowHeightDp = if (isLandscape) 42f else 52f
        val rowCount = max(rows.size, 4)
        val desiredHeight = (dp(baseRowHeightDp) * rowCount * prefs.keyboardHeightScale + dp(14f)).toInt()
        setMeasuredDimension(w, desiredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeKeyGeometry(w, h)
    }

    private fun computeKeyGeometry(viewWidth: Int, viewHeight: Int) {
        keyBoxes.clear()
        if (viewWidth <= 0 || viewHeight <= 0 || rows.isEmpty()) return

        val sidePad = dp(5f)
        val verticalPad = dp(6f)
        val spacing = dp(prefs.keySpacingDp.toFloat())
        val usableHeight = viewHeight - verticalPad * 2
        val rowHeight = usableHeight / rows.size
        val lastRowIdx = rows.lastIndex

        rows.forEachIndexed { rowIndex, row ->
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val rowIndent = if (row.size == 9 && totalWeight == 9f) viewWidth * 0.045f else 0f
            val usableRowWidth = viewWidth - (sidePad * 2) - (rowIndent * 2)
            val unitWidth = usableRowWidth / totalWeight

            var currentX = sidePad + rowIndent
            val top = verticalPad + rowIndex * rowHeight
            val bottom = top + rowHeight
            val lastColIdx = row.lastIndex

            row.forEachIndexed { colIndex, spec ->
                val cellW = unitWidth * spec.widthWeight
                val insetX = spacing / 2f
                val insetY = spacing / 2f
                val scaleInset = (1f - prefs.keySizeScale) * (cellW * 0.25f)

                val visualRect = RectF(
                    currentX + insetX + scaleInset,
                    top + insetY + scaleInset * 0.5f,
                    currentX + cellW - insetX - scaleInset,
                    bottom - insetY - scaleInset * 0.5f
                )

                // Inner 68% core safe zone: any touch inside this zone is never overridden by adaptive boundaries
                val coreMarginX = visualRect.width() * 0.16f
                val coreMarginY = visualRect.height() * 0.16f
                val coreSafeRect = RectF(
                    visualRect.left + coreMarginX,
                    visualRect.top + coreMarginY,
                    visualRect.right - coreMarginX,
                    visualRect.bottom - coreMarginY
                )

                val touchRect = RectF(visualRect)
                if (spec.isNonCharacterControl) {
                    val gutterBonusX = insetX + dp(3.5f)
                    val gutterBonusY = insetY + dp(3.0f)
                    touchRect.left -= gutterBonusX
                    touchRect.right += gutterBonusX
                    touchRect.top -= gutterBonusY
                    touchRect.bottom += gutterBonusY

                    if (colIndex == 0) touchRect.left = 0f
                    if (colIndex == lastColIdx) touchRect.right = viewWidth.toFloat()
                    if (rowIndex == lastRowIdx) touchRect.bottom = viewHeight.toFloat()
                    if (rowIndex == 0) touchRect.top = 0f
                } else {
                    touchRect.left = currentX
                    touchRect.right = currentX + cellW
                    touchRect.top = top
                    touchRect.bottom = bottom
                    if (rowIndex == lastRowIdx) touchRect.bottom = viewHeight.toFloat()
                }

                keyBoxes.add(
                    KeyBox(
                        spec = spec,
                        visualRect = visualRect,
                        coreSafeRect = coreSafeRect,
                        touchRect = touchRect,
                        rowIndex = rowIndex,
                        colIndex = colIndex
                    )
                )
                currentX += cellW
            }
        }
    }

    private fun buildAlternativesGeometry(keyBox: KeyBox) {
        alternativeRects.clear()
        val alts = keyBox.spec.longPressAlternatives
        if (alts.isEmpty()) return

        val itemW = dp(38f)
        val itemH = dp(44f)
        val totalW = itemW * alts.size
        var startX = keyBox.visualRect.centerX() - totalW / 2f
        startX = startX.coerceIn(dp(6f), width - totalW - dp(6f))
        val topY = max(dp(4f), keyBox.visualRect.top - itemH - dp(8f))

        alts.forEachIndexed { idx, alt ->
            val r = RectF(
                startX + idx * itemW,
                topY,
                startX + (idx + 1) * itemW,
                topY + itemH
            )
            alternativeRects.add(alt to r)
        }
        selectedAlternativeIndex = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(palette.windowBg)

        val cornerRadius = dp(prefs.cornerRadiusDp.toFloat())
        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = palette.keyBorder
        shadowPaint.color = palette.keyShadow

        val focalBox = activeKeyBox ?: lastAnimatedKeyBox
        val effectStrength = if (prefs.keyAnimationsEnabled && focalBox != null) animProgress else 0f
        val focalCx = focalBox?.visualRect?.centerX() ?: 0f
        val focalCy = focalBox?.visualRect?.centerY() ?: 0f
        val neighborRadiusPx = if (focalBox != null) {
            max(focalBox.visualRect.width(), focalBox.visualRect.height()) * 1.85f
        } else {
            1f
        }

        for (box in keyBoxes) {
            val isFocal = box === focalBox && effectStrength > 0.01f
            tempDrawRect.set(box.visualRect)
            var neighborFactor = 0f

            if (isFocal) {
                val expand = dp(2.2f) * effectStrength
                val lift = dp(1.2f) * effectStrength
                tempDrawRect.inset(-expand, -expand)
                tempDrawRect.offset(0f, -lift)
            } else if (effectStrength > 0.01f && focalBox != null) {
                val dx = box.visualRect.centerX() - focalCx
                val dy = box.visualRect.centerY() - focalCy
                val dist = hypot(dx, dy)
                if (dist in 1f..neighborRadiusPx) {
                    val rawInfluence = 1f - (dist / neighborRadiusPx)
                    neighborFactor = rawInfluence * rawInfluence * effectStrength

                    val pushPx = dp(1.5f) * neighborFactor
                    val shrinkPx = dp(0.65f) * neighborFactor
                    val ux = dx / dist
                    val uy = dy / dist
                    tempDrawRect.inset(shrinkPx, shrinkPx)
                    tempDrawRect.offset(ux * pushPx, uy * pushPx)
                }
            }

            if (!isFocal || effectStrength < 0.7f) {
                tempShadowRect.set(
                    tempDrawRect.left,
                    tempDrawRect.top + dp(1.2f),
                    tempDrawRect.right,
                    tempDrawRect.bottom + dp(1.2f)
                )
                canvas.drawRoundRect(tempShadowRect, cornerRadius, cornerRadius, shadowPaint)
            }

            val baseColor = when {
                box.spec.isAccent -> palette.accentBlue
                box.spec.isSpecial -> palette.specialKeyBg
                else -> palette.keyBg
            }
            val pressedColor = when {
                box.spec.isAccent -> palette.accentBluePressed
                box.spec.isSpecial -> palette.specialKeyPressedBg
                else -> palette.keyPressedBg
            }

            keyPaint.color = when {
                isFocal -> blendColor(baseColor, pressedColor, effectStrength)
                neighborFactor > 0.01f -> blendColor(baseColor, pressedColor, neighborFactor * 0.42f)
                else -> baseColor
            }
            canvas.drawRoundRect(tempDrawRect, cornerRadius, cornerRadius, keyPaint)

            if (prefs.keyBordersVisible && !box.spec.isAccent) {
                canvas.drawRoundRect(tempDrawRect, cornerRadius, cornerRadius, borderPaint)
            }

            val fgColor = if (box.spec.isAccent) palette.accentText else palette.textPrimary
            val symIcon = box.spec.symbolicIcon

            if (symIcon != null) {
                val iconScale = if (isFocal) 1f + 0.06f * effectStrength else 1f
                AdwaitaIcons.draw(
                    canvas = canvas,
                    icon = symIcon,
                    cx = tempDrawRect.centerX(),
                    cy = tempDrawRect.centerY(),
                    sizePx = dp(20f) * iconScale,
                    color = fgColor
                )
            } else {
                val paintToUse = if (box.spec.isSpecial || box.spec.type == KeyActionType.SPACE) {
                    specialTextPaint
                } else {
                    textPaint
                }
                paintToUse.color = fgColor
                val baseTextSize = when {
                    box.spec.type == KeyActionType.SPACE -> dp(13.5f)
                    box.spec.label.length > 3 -> dp(13f)
                    box.spec.isSpecial -> dp(15.5f)
                    else -> dp(20f)
                }
                paintToUse.textSize = if (isFocal) {
                    baseTextSize * (1f + 0.05f * effectStrength)
                } else {
                    baseTextSize
                }
                val fm = paintToUse.fontMetrics
                val textY = tempDrawRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(box.spec.label, tempDrawRect.centerX(), textY, paintToUse)

                if (box.spec.type == KeyActionType.SPACE && tempDrawRect.width() > dp(90f)) {
                    AdwaitaIcons.draw(
                        canvas = canvas,
                        icon = SymbolicIcon.GLOBE,
                        cx = tempDrawRect.right - dp(16f),
                        cy = tempDrawRect.centerY(),
                        sizePx = dp(14f),
                        color = palette.textMuted
                    )
                }
            }

            if (box.spec.secondaryHint.isNotEmpty()) {
                hintPaint.color = if (box.spec.isAccent) palette.accentText else palette.textMuted
                hintPaint.textSize = dp(9.5f)
                canvas.drawText(
                    box.spec.secondaryHint,
                    tempDrawRect.right - dp(5.5f),
                    tempDrawRect.top + dp(11.5f),
                    hintPaint
                )
            }
        }

        val active = activeKeyBox
        if (active != null) {
            if (showAlternativesPopup && alternativeRects.isNotEmpty()) {
                drawAlternativesPopup(canvas, cornerRadius)
            } else if (prefs.keyPreviewEnabled &&
                active.spec.type == KeyActionType.CHAR &&
                !isSpaceSliding
            ) {
                drawKeyPreviewPopup(canvas, active, cornerRadius)
            }
        }
    }

    private fun blendColor(fromColor: Int, toColor: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val inv = 1f - t
        val a = (Color.alpha(fromColor) * inv + Color.alpha(toColor) * t).toInt()
        val r = (Color.red(fromColor) * inv + Color.red(toColor) * t).toInt()
        val g = (Color.green(fromColor) * inv + Color.green(toColor) * t).toInt()
        val b = (Color.blue(fromColor) * inv + Color.blue(toColor) * t).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun drawKeyPreviewPopup(canvas: Canvas, active: KeyBox, cornerRadius: Float) {
        val pw = dp(52f)
        val ph = dp(58f)
        val left = (active.visualRect.centerX() - pw / 2f).coerceIn(dp(6f), width - pw - dp(6f))
        val top = max(dp(2f), active.visualRect.top - ph - dp(6f))
        val previewRect = RectF(left, top, left + pw, top + ph)

        previewBgPaint.color = palette.popoverBg
        previewBorderPaint.color = palette.popoverBorder
        canvas.drawRoundRect(previewRect, cornerRadius * 1.2f, cornerRadius * 1.2f, previewBgPaint)
        canvas.drawRoundRect(previewRect, cornerRadius * 1.2f, cornerRadius * 1.2f, previewBorderPaint)

        textPaint.color = palette.textPrimary
        textPaint.textSize = dp(25f)
        val fm = textPaint.fontMetrics
        val ty = previewRect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(active.spec.label, previewRect.centerX(), ty, textPaint)
    }

    private fun drawAlternativesPopup(canvas: Canvas, cornerRadius: Float) {
        val first = alternativeRects.first().second
        val last = alternativeRects.last().second
        val container = RectF(first.left - dp(4f), first.top - dp(4f), last.right + dp(4f), last.bottom + dp(4f))

        previewBgPaint.color = palette.popoverBg
        previewBorderPaint.color = palette.popoverBorder
        canvas.drawRoundRect(container, cornerRadius * 1.2f, cornerRadius * 1.2f, previewBgPaint)
        canvas.drawRoundRect(container, cornerRadius * 1.2f, cornerRadius * 1.2f, previewBorderPaint)

        alternativeRects.forEachIndexed { index, (altText, rect) ->
            if (index == selectedAlternativeIndex) {
                keyPaint.color = palette.accentBlue
                canvas.drawRoundRect(rect, cornerRadius * 0.85f, cornerRadius * 0.85f, keyPaint)
                textPaint.color = palette.accentText
            } else {
                textPaint.color = palette.textPrimary
            }
            textPaint.textSize = dp(18f)
            val fm = textPaint.fontMetrics
            val ty = rect.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(altText, rect.centerX(), ty, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(longPressRunnable)
                isBackspaceRepeating = false
                showAlternativesPopup = false
                isSpaceSliding = false
                spaceSlideAccumulatedX = 0f
                downX = x
                downY = y

                val hit = findKeyAt(x, y)
                updateActiveKey(hit)
                if (hit != null) {
                    handler.postDelayed(longPressRunnable, 340L)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val current = activeKeyBox
                if (showAlternativesPopup && alternativeRects.isNotEmpty()) {
                    var closestIdx = 0
                    var minDist = Float.MAX_VALUE
                    alternativeRects.forEachIndexed { idx, (_, r) ->
                        val d = abs(x - r.centerX())
                        if (d < minDist) {
                            minDist = d
                            closestIdx = idx
                        }
                    }
                    if (selectedAlternativeIndex != closestIdx) {
                        selectedAlternativeIndex = closestIdx
                        invalidate()
                    }
                    return true
                }

                if (current != null && current.spec.type == KeyActionType.SPACE) {
                    val dx = x - downX
                    if (!isSpaceSliding && abs(dx) > dp(16f)) {
                        isSpaceSliding = true
                        handler.removeCallbacks(longPressRunnable)
                    }
                    if (isSpaceSliding) {
                        spaceSlideAccumulatedX += (x - downX)
                        downX = x
                        val stepPx = dp(12f)
                        while (abs(spaceSlideAccumulatedX) >= stepPx) {
                            val dir = if (spaceSlideAccumulatedX > 0) 1 else -1
                            onSpaceCursorSlide(dir)
                            spaceSlideAccumulatedX -= dir * stepPx
                        }
                        return true
                    }
                }

                if (!isBackspaceRepeating) {
                    val hovered = findKeyAt(x, y)
                    if (hovered !== current) {
                        handler.removeCallbacks(longPressRunnable)
                        updateActiveKey(hovered)
                        if (hovered != null) {
                            handler.postDelayed(longPressRunnable, 340L)
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                val wasBackspaceRepeating = isBackspaceRepeating
                isBackspaceRepeating = false

                val current = activeKeyBox
                if (event.actionMasked == MotionEvent.ACTION_UP && current != null) {
                    if (showAlternativesPopup && alternativeRects.isNotEmpty()) {
                        val chosen = alternativeRects.getOrNull(selectedAlternativeIndex)?.first
                        if (chosen != null) {
                            onKeyTriggered(KeySpec(label = chosen, outputText = chosen))
                        }
                    } else if (!isSpaceSliding && !wasBackspaceRepeating) {
                        onKeyTriggered(current.spec)
                    }
                }

                showAlternativesPopup = false
                isSpaceSliding = false
                updateActiveKey(null)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Signed distance from point (x, y) to a rectangle's boundary:
     * - Negative when (x, y) is inside `rect` (magnitude = distance to nearest edge).
     * - Positive when (x, y) is outside `rect` (magnitude = Euclidean distance to nearest edge).
     */
    fun resolveKeySpecAt(x: Float, y: Float): KeySpec? = findKeyAt(x, y)?.spec

    private fun findKeyAt(x: Float, y: Float): KeyBox? {
        val regions = keyBoxes.mapIndexed { idx, box ->
            TouchKeyRegion(
                index = idx,
                spec = box.spec,
                vLeft = box.visualRect.left,
                vTop = box.visualRect.top,
                vRight = box.visualRect.right,
                vBottom = box.visualRect.bottom,
                coreLeft = box.coreSafeRect.left,
                coreTop = box.coreSafeRect.top,
                coreRight = box.coreSafeRect.right,
                coreBottom = box.coreSafeRect.bottom,
                tLeft = box.touchRect.left,
                tTop = box.touchRect.top,
                tRight = box.touchRect.right,
                tBottom = box.touchRect.bottom
            )
        }
        val hitIndex = TouchRegionResolver.resolveHitIndex(
            x = x,
            y = y,
            regions = regions,
            adaptiveEnabled = prefs.adaptiveTouchEnabled,
            nextCharWeights = nextCharWeights,
            density = resources.displayMetrics.density
        )
        return keyBoxes.getOrNull(hitIndex)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
