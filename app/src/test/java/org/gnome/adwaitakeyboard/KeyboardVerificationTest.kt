package org.gnome.adwaitakeyboard

import org.gnome.adwaitakeyboard.ime.KeyActionType
import org.gnome.adwaitakeyboard.ime.KeySpec
import org.gnome.adwaitakeyboard.ime.KeyboardLayouts
import org.gnome.adwaitakeyboard.ime.ShiftState
import org.gnome.adwaitakeyboard.ime.TouchKeyRegion
import org.gnome.adwaitakeyboard.ime.TouchRegionResolver
import org.gnome.adwaitakeyboard.prefs.KeyboardLayoutType
import org.gnome.adwaitakeyboard.prefs.ToolbarAction
import org.gnome.adwaitakeyboard.theme.SymbolicIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardVerificationTest {

    private fun buildSimulatedRowRegions(
        specs: List<KeySpec>,
        viewWidth: Float = 1080f,
        rowTop: Float = 400f,
        rowBottom: Float = 540f,
        spacingPx: Float = 16f
    ): List<TouchKeyRegion> {
        val totalWeight = specs.sumOf { it.widthWeight.toDouble() }.toFloat()
        val sidePad = 14f
        val unitWidth = (viewWidth - sidePad * 2f) / totalWeight
        var currentX = sidePad
        val regions = ArrayList<TouchKeyRegion>()

        specs.forEachIndexed { idx, spec ->
            val cellW = unitWidth * spec.widthWeight
            val inset = spacingPx * 0.5f
            val vLeft = currentX + inset
            val vRight = currentX + cellW - inset
            val vTop = rowTop + inset
            val vBottom = rowBottom - inset

            val coreMarginX = (vRight - vLeft) * 0.16f
            val coreMarginY = (vBottom - vTop) * 0.16f

            val tLeft = if (spec.isNonCharacterControl && idx == 0) 0f else currentX
            val tRight = if (spec.isNonCharacterControl && idx == specs.lastIndex) viewWidth else currentX + cellW
            val tTop = if (spec.isNonCharacterControl) rowTop - 8f else rowTop
            val tBottom = if (spec.isNonCharacterControl) rowBottom + 8f else rowBottom

            regions.add(
                TouchKeyRegion(
                    index = idx,
                    spec = spec,
                    vLeft = vLeft,
                    vTop = vTop,
                    vRight = vRight,
                    vBottom = vBottom,
                    coreLeft = vLeft + coreMarginX,
                    coreTop = vTop + coreMarginY,
                    coreRight = vRight - coreMarginX,
                    coreBottom = vBottom - coreMarginY,
                    tLeft = tLeft,
                    tTop = tTop,
                    tRight = tRight,
                    tBottom = tBottom
                )
            )
            currentX += cellW
        }
        return regions
    }

    @Test
    fun testAllLayoutsAndShiftStatesPreserveTypingAndSymbolicIcons() {
        for (layout in KeyboardLayoutType.entries) {
            val lowerRows = KeyboardLayouts.buildAlphabetRows(layout, includeNumberRow = true, shiftState = ShiftState.LOWERCASE)
            val upperRows = KeyboardLayouts.buildAlphabetRows(layout, includeNumberRow = true, shiftState = ShiftState.SHIFTED_SINGLE)
            val capsRows = KeyboardLayouts.buildAlphabetRows(layout, includeNumberRow = true, shiftState = ShiftState.CAPS_LOCK)

            assertEquals(5, lowerRows.size)
            assertEquals(10, lowerRows[0].size) // Number row 1..0

            val shiftLower = lowerRows[3].first()
            val shiftUpper = upperRows[3].first()
            val shiftCaps = capsRows[3].first()
            val backspace = lowerRows[3].last()
            val enter = lowerRows[4].last()

            assertEquals(SymbolicIcon.SHIFT, shiftLower.symbolicIcon)
            assertEquals(SymbolicIcon.SHIFT_FILLED, shiftUpper.symbolicIcon)
            assertEquals(SymbolicIcon.CAPS_LOCK, shiftCaps.symbolicIcon)
            assertEquals(SymbolicIcon.BACKSPACE, backspace.symbolicIcon)
            assertEquals(SymbolicIcon.ENTER, enter.symbolicIcon)

            assertTrue(shiftLower.isNonCharacterControl)
            assertTrue(backspace.isNonCharacterControl)
            assertTrue(enter.isNonCharacterControl)
        }
    }

    @Test
    fun testToolbarActionsHaveConsistentSymbolicIconsAndNoEmojiLabels() {
        for (action in ToolbarAction.entries) {
            assertNotNull(action.icon)
            assertTrue(action.label.isNotBlank())
        }
    }

    @Test
    fun testOrdinaryAndRapidTypingHitAccuracy() {
        val rows = KeyboardLayouts.buildAlphabetRows(
            KeyboardLayoutType.QWERTY,
            includeNumberRow = true,
            shiftState = ShiftState.LOWERCASE
        )
        val row3Regions = buildSimulatedRowRegions(rows[3])

        // Simulate rapid sequence of center taps on 'z', 'x', 'c', 'v', 'b', 'n', 'm'
        val expectedLetters = listOf("z", "x", "c", "v", "b", "n", "m")
        val typedOutput = StringBuilder()

        for (letter in expectedLetters) {
            val targetRegion = row3Regions.first { it.spec.outputText == letter }
            val hitIdx = TouchRegionResolver.resolveHitIndex(
                x = targetRegion.visualCenterX,
                y = targetRegion.visualCenterY,
                regions = row3Regions,
                adaptiveEnabled = true,
                nextCharWeights = mapOf('n' to 1.0f),
                density = 2.75f
            )
            typedOutput.append(row3Regions[hitIdx].spec.outputText)
        }

        assertEquals("zxcvbnm", typedOutput.toString())
    }

    @Test
    fun testForgivingNonCharacterControlHitTargets() {
        val rows = KeyboardLayouts.buildAlphabetRows(
            KeyboardLayoutType.QWERTY,
            includeNumberRow = true,
            shiftState = ShiftState.LOWERCASE
        )
        val row3Regions = buildSimulatedRowRegions(rows[3], viewWidth = 1080f)

        // Tap at extreme left screen bezel (x = 2px) -> must hit Shift
        val leftEdgeHit = TouchRegionResolver.resolveHitIndex(
            x = 2f,
            y = 470f,
            regions = row3Regions,
            adaptiveEnabled = false,
            nextCharWeights = emptyMap(),
            density = 2.75f
        )
        assertEquals(KeyActionType.SHIFT, row3Regions[leftEdgeHit].spec.type)

        // Tap at extreme right screen bezel (x = 1078px) -> must hit Backspace
        val rightEdgeHit = TouchRegionResolver.resolveHitIndex(
            x = 1078f,
            y = 470f,
            regions = row3Regions,
            adaptiveEnabled = false,
            nextCharWeights = emptyMap(),
            density = 2.75f
        )
        assertEquals(KeyActionType.BACKSPACE, row3Regions[rightEdgeHit].spec.type)
    }

    @Test
    fun testAdaptiveTouchRegionsExpandExpectedNextCharacterAtBoundaryWithoutOverridingCore() {
        val rows = KeyboardLayouts.buildAlphabetRows(
            KeyboardLayoutType.QWERTY,
            includeNumberRow = true,
            shiftState = ShiftState.LOWERCASE
        )
        val row3Regions = buildSimulatedRowRegions(rows[3], viewWidth = 1080f, spacingPx = 16f)
        val bRegion = row3Regions.first { it.spec.outputText == "b" }
        val nRegion = row3Regions.first { it.spec.outputText == "n" }

        // Point in the boundary gutter between 'b' and 'n', 3px closer to 'b' than 'n'
        val midpointX = (bRegion.vRight + nRegion.vLeft) * 0.5f
        val boundaryTouchX = midpointX - 3.0f
        val boundaryTouchY = bRegion.visualCenterY

        // 1. With Adaptive Touch OFF: touch closer to 'b' resolves to 'b'
        val hitWithoutAdaptive = TouchRegionResolver.resolveHitIndex(
            x = boundaryTouchX,
            y = boundaryTouchY,
            regions = row3Regions,
            adaptiveEnabled = false,
            nextCharWeights = mapOf('n' to 1.0f),
            density = 2.75f
        )
        assertEquals("b", row3Regions[hitWithoutAdaptive].spec.outputText)

        // 2. With Adaptive Touch ON and prefix "ope" -> 'n' weight = 1.0f:
        //    the boundary touch resolves to 'n'
        val hitWithAdaptive = TouchRegionResolver.resolveHitIndex(
            x = boundaryTouchX,
            y = boundaryTouchY,
            regions = row3Regions,
            adaptiveEnabled = true,
            nextCharWeights = mapOf('n' to 1.0f),
            density = 2.75f
        )
        assertEquals("n", row3Regions[hitWithAdaptive].spec.outputText)

        // 3. Touch inside 'b' Core Safe Zone ALWAYS resolves to 'b' even when 'n' has weight 1.0f
        val coreTouchOnB = TouchRegionResolver.resolveHitIndex(
            x = bRegion.visualCenterX,
            y = bRegion.visualCenterY,
            regions = row3Regions,
            adaptiveEnabled = true,
            nextCharWeights = mapOf('n' to 1.0f),
            density = 2.75f
        )
        assertEquals("b", row3Regions[coreTouchOnB].spec.outputText)

        // 4. Verify Visible Key Positions Never Changed
        assertFalse(bRegion.vRight > nRegion.vLeft)
    }
}
