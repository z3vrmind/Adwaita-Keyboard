package org.gnome.adwaitakeyboard.ime

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class TouchKeyRegion(
    val index: Int,
    val spec: KeySpec,
    val vLeft: Float,
    val vTop: Float,
    val vRight: Float,
    val vBottom: Float,
    val coreLeft: Float,
    val coreTop: Float,
    val coreRight: Float,
    val coreBottom: Float,
    val tLeft: Float,
    val tTop: Float,
    val tRight: Float,
    val tBottom: Float
) {
    val visualCenterX: Float get() = (vLeft + vRight) * 0.5f
    val visualCenterY: Float get() = (vTop + vBottom) * 0.5f

    fun containsCore(x: Float, y: Float): Boolean =
        x >= coreLeft && x <= coreRight && y >= coreTop && y <= coreBottom

    fun containsVisual(x: Float, y: Float): Boolean =
        x >= vLeft && x <= vRight && y >= vTop && y <= vBottom

    fun containsTouch(x: Float, y: Float): Boolean =
        x >= tLeft && x <= tRight && y >= tTop && y <= tBottom
}

object TouchRegionResolver {

    /**
     * Signed distance from point (x, y) to a rectangle [left, top, right, bottom]:
     * - Negative inside the rectangle (magnitude = distance to nearest edge).
     * - Positive outside the rectangle (magnitude = Euclidean distance to nearest edge).
     */
    fun signedDistanceToRect(
        x: Float,
        y: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Float {
        val dxOut = max(0f, max(left - x, x - right))
        val dyOut = max(0f, max(top - y, y - bottom))
        if (dxOut > 0f || dyOut > 0f) {
            return hypot(dxOut, dyOut)
        }
        val distToLeft = x - left
        val distToRight = right - x
        val distToTop = y - top
        val distToBottom = bottom - y
        return -min(min(distToLeft, distToRight), min(distToTop, distToBottom))
    }

    /**
     * Deterministic, non-overlapping touch resolution:
     * 1. Core Safe Zone (inner 68% of visual keycap): Always wins immediately.
     * 2. Adaptive Boundary Zone (when adaptiveEnabled == true and nextCharWeights is non-empty):
     *    Shifts the effective boundary between adjacent character keys by up to 5.5dp * weight.
     * 3. Direct Visual Keycap containment.
     * 4. Forgiving Non-Character Control touchRect (Shift, Backspace, Enter, ?123, Emoji).
     * 5. Standard cell touchRect containment.
     * 6. Nearest-neighbor fallback within 24dp.
     */
    fun resolveHitIndex(
        x: Float,
        y: Float,
        regions: List<TouchKeyRegion>,
        adaptiveEnabled: Boolean,
        nextCharWeights: Map<Char, Float>,
        density: Float
    ): Int {
        if (regions.isEmpty()) return -1

        // 1. Core safe zone check
        for (region in regions) {
            if (region.containsCore(x, y)) return region.index
        }

        // 2. Adaptive boundary resolution for character keys near (x, y)
        if (adaptiveEnabled && nextCharWeights.isNotEmpty()) {
            val maxExpansionPx = 5.5f * density
            val candidateRadiusPx = 18.0f * density
            var bestCandidate: TouchKeyRegion? = null
            var bestEffectiveDist = Float.MAX_VALUE
            var bestCenterDist = Float.MAX_VALUE

            for (region in regions) {
                if (region.spec.type == KeyActionType.CHAR && region.spec.outputText.length == 1) {
                    val rawSignedDist = signedDistanceToRect(
                        x, y,
                        region.vLeft, region.vTop, region.vRight, region.vBottom
                    )
                    if (rawSignedDist <= candidateRadiusPx) {
                        val ch = region.spec.outputText[0].lowercaseChar()
                        val weight = (nextCharWeights[ch] ?: 0f).coerceIn(0f, 1f)
                        val effectiveDist = rawSignedDist - (maxExpansionPx * weight)
                        val centerDist = hypot(x - region.visualCenterX, y - region.visualCenterY)

                        if (effectiveDist < bestEffectiveDist - 0.01f ||
                            (abs(effectiveDist - bestEffectiveDist) <= 0.01f && centerDist < bestCenterDist)
                        ) {
                            bestEffectiveDist = effectiveDist
                            bestCenterDist = centerDist
                            bestCandidate = region
                        }
                    }
                }
            }

            if (bestCandidate != null && bestEffectiveDist <= 4.5f * density) {
                return bestCandidate.index
            }
        }

        // 3. Direct visual keycap containment
        for (region in regions) {
            if (region.containsVisual(x, y)) return region.index
        }

        // 4. Forgiving control key touchRect (Shift, Backspace, Enter, Symbols, Emoji)
        for (region in regions) {
            if (region.spec.isNonCharacterControl && region.containsTouch(x, y)) {
                return region.index
            }
        }

        // 5. Standard cell touchRect
        for (region in regions) {
            if (region.containsTouch(x, y)) return region.index
        }

        // 6. Nearest-neighbor fallback
        var nearestIdx = -1
        var bestDist = 24.0f * density
        for (region in regions) {
            val dx = max(0f, max(region.tLeft - x, x - region.tRight))
            val dy = max(0f, max(region.tTop - y, y - region.tBottom))
            val rawDist = dx + dy
            val weightedDist = if (region.spec.isNonCharacterControl) rawDist * 0.82f else rawDist
            if (weightedDist < bestDist) {
                bestDist = weightedDist
                nearestIdx = region.index
            }
        }
        return nearestIdx
    }
}
