package org.gnome.adwaitakeyboard.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import org.gnome.adwaitakeyboard.R
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.prefs.ThemeMode

/**
 * Authentic GNOME 46 / GTK4 / libadwaita 1.5 color tokens and Ubuntu typography.
 */
data class AdwaitaPalette(
    val isDark: Boolean,
    val windowBg: Int,
    val headerBarBg: Int,
    val headerBarBorder: Int,
    val cardBg: Int,
    val keyBg: Int,
    val keyPressedBg: Int,
    val specialKeyBg: Int,
    val specialKeyPressedBg: Int,
    val keyBorder: Int,
    val keyShadow: Int,
    val accentBlue: Int,
    val accentBluePressed: Int,
    val accentText: Int,
    val destructiveRed: Int,
    val warningOrange: Int,
    val successGreen: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val popoverBg: Int,
    val popoverBorder: Int,
    val pillBg: Int
)

object AdwaitaTheme {

    // libadwaita 1.5 Light Specification
    val LightPalette = AdwaitaPalette(
        isDark = false,
        windowBg = 0xFFE6E6E8.toInt(),          // Squeekboard / Adwaita keyboard tray surface
        headerBarBg = 0xFFF2F2F4.toInt(),       // Adwaita headerbar surface
        headerBarBorder = 0xFFDCDCE0.toInt(),   // Subtle 1px divider
        cardBg = 0xFFFFFFFF.toInt(),            // AdwPreferencesGroup card surface
        keyBg = 0xFFFFFFFF.toInt(),             // Raised white keycap
        keyPressedBg = 0xFFEAEAEB.toInt(),      // Active keycap surface
        specialKeyBg = 0xFFD6D6DA.toInt(),      // Action/modifier keycap surface
        specialKeyPressedBg = 0xFFC4C4C9.toInt(),
        keyBorder = 0xFFD8D8DC.toInt(),         // Subtle divider / optional key outline
        keyShadow = 0x24000000,                 // Soft bottom key elevation lip
        accentBlue = 0xFF3584E4.toInt(),        // Official @accent_bg_color
        accentBluePressed = 0xFF1C71D8.toInt(),
        accentText = 0xFFFFFFFF.toInt(),
        destructiveRed = 0xFFE01B24.toInt(),    // Official @destructive_bg_color
        warningOrange = 0xFFE66100.toInt(),
        successGreen = 0xFF2EC27E.toInt(),
        textPrimary = 0xFF2E2E32.toInt(),       // Official Adwaita dark ink
        textSecondary = 0xFF5E5C64.toInt(),
        textMuted = 0xFF86848C.toInt(),
        popoverBg = 0xFFFFFFFF.toInt(),
        popoverBorder = 0xFFD4D4D8.toInt(),
        pillBg = 0xFFE0E0E4.toInt()
    )

    // libadwaita 1.5 Dark Specification
    val DarkPalette = AdwaitaPalette(
        isDark = true,
        windowBg = 0xFF1E1E1E.toInt(),          // Official @window_bg_color dark
        headerBarBg = 0xFF262626.toInt(),       // Official @headerbar_bg_color dark
        headerBarBorder = 0xFF363636.toInt(),
        cardBg = 0xFF2E2E30.toInt(),            // Official @card_bg_color dark
        keyBg = 0xFF38383C.toInt(),             // Elevated dark keycap
        keyPressedBg = 0xFF4C4C52.toInt(),
        specialKeyBg = 0xFF2B2B2E.toInt(),      // Modifier keycap surface
        specialKeyPressedBg = 0xFF3F3F44.toInt(),
        keyBorder = 0xFF3C3C40.toInt(),
        keyShadow = 0x50000000,
        accentBlue = 0xFF3584E4.toInt(),
        accentBluePressed = 0xFF62A0EA.toInt(),
        accentText = 0xFFFFFFFF.toInt(),
        destructiveRed = 0xFFC01C28.toInt(),
        warningOrange = 0xFFF5C211.toInt(),
        successGreen = 0xFF2EC27E.toInt(),
        textPrimary = 0xFFFFFFFF.toInt(),
        textSecondary = 0xFF9A9996.toInt(),
        textMuted = 0xFF77767B.toInt(),
        popoverBg = 0xFF303034.toInt(),
        popoverBorder = 0xFF45454A.toInt(),
        pillBg = 0xFF36363A.toInt()
    )

    @Volatile
    private var cachedRegular: Typeface? = null

    @Volatile
    private var cachedMedium: Typeface? = null

    @Volatile
    private var cachedBold: Typeface? = null

    val composeUbuntuFontFamily = FontFamily(
        Font(R.font.ubuntu_regular, FontWeight.Normal),
        Font(R.font.ubuntu_medium, FontWeight.Medium),
        Font(R.font.ubuntu_bold, FontWeight.Bold)
    )

    fun ubuntuRegular(context: Context): Typeface {
        cachedRegular?.let { return it }
        val loaded = try {
            ResourcesCompat.getFont(context, R.font.ubuntu_regular)
        } catch (_: Exception) {
            null
        } ?: Typeface.create("sans-serif", Typeface.NORMAL)
        cachedRegular = loaded
        return loaded
    }

    fun ubuntuMedium(context: Context): Typeface {
        cachedMedium?.let { return it }
        val loaded = try {
            ResourcesCompat.getFont(context, R.font.ubuntu_medium)
        } catch (_: Exception) {
            null
        } ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        cachedMedium = loaded
        return loaded
    }

    fun ubuntuBold(context: Context): Typeface {
        cachedBold?.let { return it }
        val loaded = try {
            ResourcesCompat.getFont(context, R.font.ubuntu_bold)
        } catch (_: Exception) {
            null
        } ?: Typeface.create("sans-serif", Typeface.BOLD)
        cachedBold = loaded
        return loaded
    }

    fun resolvePalette(context: Context, prefs: KeyboardPreferences): AdwaitaPalette {
        val useDark = when (prefs.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.FOLLOW_SYSTEM -> {
                val nightFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return if (useDark) DarkPalette else LightPalette
    }
}
