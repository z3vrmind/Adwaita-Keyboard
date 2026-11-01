package org.gnome.adwaitakeyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnome.adwaitakeyboard.clipboard.ClipboardController
import org.gnome.adwaitakeyboard.dictionary.LocalDictionaryEngine
import org.gnome.adwaitakeyboard.prefs.*
import org.gnome.adwaitakeyboard.privacy.PrivacyManager
import org.gnome.adwaitakeyboard.theme.AdwaitaIcons
import org.gnome.adwaitakeyboard.theme.AdwaitaPalette
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

enum class SettingsTab(val title: String, val icon: SymbolicIcon) {
    ONBOARDING("Setup", SymbolicIcon.KEYBOARD),
    APPEARANCE("Appearance", SymbolicIcon.APPEARANCE),
    TYPING("Typing", SymbolicIcon.TYPING),
    HAPTICS("Haptics & Sound", SymbolicIcon.SOUND),
    CLIPBOARD("Clipboard", SymbolicIcon.CLIPBOARD),
    TOOLBAR("Toolbar", SymbolicIcon.MENU),
    PRIVACY("Privacy", SymbolicIcon.PRIVATE_SHIELD),
    ABOUT("About", SymbolicIcon.INFO)
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = KeyboardPreferences(this)
        val dictionaryEngine = LocalDictionaryEngine(this, prefs)
        val clipboardController = ClipboardController(this, prefs)

        setContent {
            AdwaitaSettingsApp(
                prefs = prefs,
                dictionaryEngine = dictionaryEngine,
                clipboardController = clipboardController
            )
        }
    }
}

@Composable
fun AdwaitaSettingsApp(
    prefs: KeyboardPreferences,
    dictionaryEngine: LocalDictionaryEngine,
    clipboardController: ClipboardController
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val palette = remember(revision) { AdwaitaTheme.resolvePalette(context, prefs) }
    var activeTab by remember { mutableStateOf(SettingsTab.ONBOARDING) }
    var testInputText by remember { mutableStateOf("") }
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(palette.windowBg))
    ) {
        // GNOME libadwaita AdwHeaderBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(palette.headerBarBg))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdwaitaIcons.SymbolicIconView(
                    icon = SymbolicIcon.KEYBOARD,
                    tint = palette.accentBlue,
                    size = 22.dp
                )
                Column {
                    BasicText(
                        text = "Adwaita Keyboard",
                        style = TextStyle(
                            fontFamily = ubuntuFont,
                            color = Color(palette.textPrimary),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    BasicText(
                        text = "GNOME libadwaita Offline Privacy IME",
                        style = TextStyle(
                            fontFamily = ubuntuFont,
                            color = Color(palette.textSecondary),
                            fontSize = 12.sp
                        )
                    )
                }
            }
            AdwaitaIconPillButton(
                icon = SymbolicIcon.PRIVATE_SHIELD,
                label = if (prefs.privateModeSession) "Private ON" else "Private OFF",
                bg = if (prefs.privateModeSession) Color(palette.accentBlue) else Color(palette.pillBg),
                fg = if (prefs.privateModeSession) palette.accentText else palette.textPrimary,
                onClick = {
                    prefs.privateModeSession = !prefs.privateModeSession
                    revision++
                }
            )
        }

        // GNOME ViewSwitcher Bar with Symbolic Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(palette.headerBarBg))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsTab.entries.forEach { tab ->
                val selected = tab == activeTab
                AdwaitaIconPillButton(
                    icon = tab.icon,
                    label = tab.title,
                    bg = if (selected) Color(palette.accentBlue) else Color(palette.cardBg),
                    fg = if (selected) palette.accentText else palette.textPrimary,
                    onClick = { activeTab = tab }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(palette.headerBarBorder))
        )

        // Live Keyboard Test Entry Box (AdwEntryRow style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(palette.cardBg))
                .border(1.dp, Color(palette.keyBorder), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdwaitaIcons.SymbolicIconView(
                icon = SymbolicIcon.TYPING,
                tint = palette.textSecondary,
                size = 18.dp
            )
            BasicTextField(
                value = testInputText,
                onValueChange = { testInputText = it },
                textStyle = TextStyle(
                    fontFamily = ubuntuFont,
                    color = Color(palette.textPrimary),
                    fontSize = 14.5.sp
                ),
                cursorBrush = SolidColor(Color(palette.accentBlue)),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (testInputText.isEmpty()) {
                        BasicText(
                            text = "Tap here to test Adwaita Keyboard live…",
                            style = TextStyle(
                                fontFamily = ubuntuFont,
                                color = Color(palette.textMuted),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Main AdwPreferencesPage Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (activeTab) {
                SettingsTab.ONBOARDING -> OnboardingSection(context, palette)
                SettingsTab.APPEARANCE -> AppearanceSection(prefs, palette) { revision++ }
                SettingsTab.TYPING -> TypingSection(prefs, palette) { revision++ }
                SettingsTab.HAPTICS -> HapticsSoundSection(prefs, palette) { revision++ }
                SettingsTab.CLIPBOARD -> ClipboardSettingsSection(prefs, clipboardController, palette) { revision++ }
                SettingsTab.TOOLBAR -> ToolbarCustomizationSection(prefs, palette) { revision++ }
                SettingsTab.PRIVACY -> PrivacySection(context, prefs, dictionaryEngine, clipboardController, palette) { revision++ }
                SettingsTab.ABOUT -> AboutSection(palette)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingSection(context: Context, palette: AdwaitaPalette) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    var refreshCheck by remember { mutableIntStateOf(0) }
    val isEnabled = remember(refreshCheck) {
        imm.enabledInputMethodList.any { it.packageName == context.packageName }
    }

    AdwaitaGroup(title = "Enable & Select Adwaita Keyboard", palette = palette) {
        AdwaitaActionRow(
            title = "1. Enable in System Settings",
            subtitle = if (isEnabled) "Enabled in Android Input Methods" else "Turn on 'Adwaita Keyboard' in Android's On-screen keyboard list.",
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Open Settings",
                    bg = Color(palette.accentBlue),
                    fg = Color(palette.accentText),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        refreshCheck++
                    }
                )
            }
        )
        AdwaitaDivider(palette)
        AdwaitaActionRow(
            title = "2. Set as Default Keyboard",
            subtitle = "Select 'Adwaita Keyboard' in the system input method dialog.",
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Switch IME",
                    bg = Color(palette.accentBlue),
                    fg = Color(palette.accentText),
                    onClick = {
                        imm.showInputMethodPicker()
                        refreshCheck++
                    }
                )
            }
        )
        AdwaitaDivider(palette)
        AdwaitaActionRow(
            title = "3. Verify Status",
            subtitle = "Adwaita Keyboard never alters system settings without explicit user action.",
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Refresh",
                    bg = Color(palette.pillBg),
                    fg = Color(palette.textPrimary),
                    onClick = { refreshCheck++ }
                )
            }
        )
    }
}

@Composable
private fun AppearanceSection(
    prefs: KeyboardPreferences,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    AdwaitaGroup(title = "Color Scheme (GTK4 / libadwaita)", palette = palette) {
        ThemeMode.entries.forEachIndexed { idx, mode ->
            if (idx > 0) AdwaitaDivider(palette)
            val active = prefs.themeMode == mode
            AdwaitaActionRow(
                title = mode.label,
                subtitle = "Use ${mode.label} surfaces across keyboard, popovers, and settings",
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = if (active) "Active" else "Apply",
                        bg = if (active) Color(palette.accentBlue) else Color(palette.pillBg),
                        fg = if (active) Color(palette.accentText) else Color(palette.textPrimary),
                        onClick = {
                            prefs.themeMode = mode
                            onChanged()
                        }
                    )
                }
            )
        }
    }

    AdwaitaGroup(title = "Dimensions & Key Surfaces", palette = palette) {
        AdwaitaStepperRow(
            title = "Keyboard Height",
            valueLabel = "${(prefs.keyboardHeightScale * 100).toInt()}%",
            palette = palette,
            onDecrement = {
                prefs.keyboardHeightScale -= 0.05f
                onChanged()
            },
            onIncrement = {
                prefs.keyboardHeightScale += 0.05f
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Key Size Scale",
            valueLabel = "${(prefs.keySizeScale * 100).toInt()}%",
            palette = palette,
            onDecrement = {
                prefs.keySizeScale -= 0.05f
                onChanged()
            },
            onIncrement = {
                prefs.keySizeScale += 0.05f
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Key Spacing",
            valueLabel = "${prefs.keySpacingDp} dp",
            palette = palette,
            onDecrement = {
                prefs.keySpacingDp -= 1
                onChanged()
            },
            onIncrement = {
                prefs.keySpacingDp += 1
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Key Corner Radius",
            valueLabel = "${prefs.cornerRadiusDp} dp",
            palette = palette,
            onDecrement = {
                prefs.cornerRadiusDp -= 1
                onChanged()
            },
            onIncrement = {
                prefs.cornerRadiusDp += 1
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Top Number Row",
            subtitle = "Always display 1–0 row above alphabet keys",
            checked = prefs.showNumberRow,
            palette = palette
        ) {
            prefs.showNumberRow = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Show Header Toolbar",
            subtitle = "Display suggestions strip and symbolic action buttons",
            checked = prefs.toolbarVisible,
            palette = palette
        ) {
            prefs.toolbarVisible = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "High-Contrast Key Outlines",
            subtitle = "Off by default for genuine borderless Adwaita keycaps",
            checked = prefs.keyBordersVisible,
            palette = palette
        ) {
            prefs.keyBordersVisible = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Key Popover Preview & Press Animation",
            subtitle = "Show character popover above finger while typing",
            checked = prefs.keyPreviewEnabled,
            palette = palette
        ) {
            prefs.keyPreviewEnabled = it
            prefs.keyAnimationsEnabled = it
            onChanged()
        }
    }

    AdwaitaGroup(title = "One-Handed Mode & Layout", palette = palette) {
        OneHandedMode.entries.forEachIndexed { idx, mode ->
            if (idx > 0) AdwaitaDivider(palette)
            val active = prefs.oneHandedMode == mode
            AdwaitaActionRow(
                title = "One-Handed: ${mode.label}",
                subtitle = "Shift keyboard dock for single-thumb reach",
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = if (active) "Active" else "Select",
                        bg = if (active) Color(palette.accentBlue) else Color(palette.pillBg),
                        fg = if (active) Color(palette.accentText) else Color(palette.textPrimary),
                        onClick = {
                            prefs.oneHandedMode = mode
                            onChanged()
                        }
                    )
                }
            )
        }
        AdwaitaDivider(palette)
        KeyboardLayoutType.entries.forEachIndexed { idx, layout ->
            if (idx > 0) AdwaitaDivider(palette)
            val active = prefs.layoutType == layout
            AdwaitaActionRow(
                title = "Layout: ${layout.label}",
                subtitle = "Long-press spacebar to cycle layouts while typing",
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = if (active) "Active" else "Select",
                        bg = if (active) Color(palette.accentBlue) else Color(palette.pillBg),
                        fg = if (active) Color(palette.accentText) else Color(palette.textPrimary),
                        onClick = {
                            prefs.layoutType = layout
                            onChanged()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun TypingSection(
    prefs: KeyboardPreferences,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    var newDictWord by remember { mutableStateOf("") }
    var shortcutTrigger by remember { mutableStateOf("") }
    var shortcutExpansion by remember { mutableStateOf("") }
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily

    AdwaitaGroup(title = "Local Autocorrect & Suggestions", palette = palette) {
        AdwaitaToggleRow(
            title = "Offline Word Suggestions",
            subtitle = "Suggest word completions and next words locally",
            checked = prefs.suggestionsEnabled,
            palette = palette
        ) {
            prefs.suggestionsEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Offline Autocorrect",
            subtitle = "Automatically correct common typos on space or punctuation",
            checked = prefs.autocorrectEnabled,
            palette = palette
        ) {
            prefs.autocorrectEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Adaptive Touch Regions",
            subtitle = "Subtly expand invisible touch boundaries for expected next letters (100% offline)",
            checked = prefs.adaptiveTouchEnabled,
            palette = palette
        ) {
            prefs.adaptiveTouchEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Auto-Capitalization",
            subtitle = "Capitalize the first character of each new sentence",
            checked = prefs.autoCapitalization,
            palette = palette
        ) {
            prefs.autoCapitalization = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Double-Space Full Stop",
            subtitle = "Double-tap spacebar to insert a period and space",
            checked = prefs.doubleSpacePeriod,
            palette = palette
        ) {
            prefs.doubleSpacePeriod = it
            onChanged()
        }
    }

    AdwaitaGroup(title = "Personal Dictionary", palette = palette) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(palette.windowBg))
                    .border(1.dp, Color(palette.keyBorder), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = newDictWord,
                    onValueChange = { newDictWord = it },
                    textStyle = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textPrimary), fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(palette.accentBlue)),
                    decorationBox = { inner ->
                        if (newDictWord.isEmpty()) {
                            BasicText("Add custom word…", style = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textMuted), fontSize = 14.sp))
                        }
                        inner()
                    }
                )
            }
            AdwaitaPillButton(
                label = "Add Word",
                bg = Color(palette.accentBlue),
                fg = Color(palette.accentText),
                onClick = {
                    if (newDictWord.isNotBlank()) {
                        prefs.addPersonalWord(newDictWord)
                        newDictWord = ""
                        onChanged()
                    }
                }
            )
        }
        prefs.getPersonalDictionary().sorted().forEach { word ->
            AdwaitaDivider(palette)
            AdwaitaActionRow(
                title = word,
                subtitle = "Never changed by autocorrect",
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = "Remove",
                        bg = Color(palette.destructiveRed),
                        fg = Color(palette.accentText),
                        onClick = {
                            prefs.removePersonalWord(word)
                            onChanged()
                        }
                    )
                }
            )
        }
    }

    AdwaitaGroup(title = "Local Text Shortcuts", palette = palette) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(palette.windowBg))
                        .padding(10.dp)
                ) {
                    BasicTextField(
                        value = shortcutTrigger,
                        onValueChange = { shortcutTrigger = it },
                        textStyle = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textPrimary), fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (shortcutTrigger.isEmpty()) {
                                BasicText("Trigger (@@)", style = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textMuted), fontSize = 13.sp))
                            }
                            inner()
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(palette.windowBg))
                        .padding(10.dp)
                ) {
                    BasicTextField(
                        value = shortcutExpansion,
                        onValueChange = { shortcutExpansion = it },
                        textStyle = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textPrimary), fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (shortcutExpansion.isEmpty()) {
                                BasicText("Replacement phrase…", style = TextStyle(fontFamily = ubuntuFont, color = Color(palette.textMuted), fontSize = 13.sp))
                            }
                            inner()
                        }
                    )
                }
            }
            AdwaitaPillButton(
                label = "Save Shortcut",
                bg = Color(palette.accentBlue),
                fg = Color(palette.accentText),
                onClick = {
                    if (shortcutTrigger.isNotBlank() && shortcutExpansion.isNotBlank()) {
                        val updated = prefs.getTextShortcuts().toMutableMap()
                        updated[shortcutTrigger.trim()] = shortcutExpansion.trim()
                        prefs.saveTextShortcuts(updated)
                        shortcutTrigger = ""
                        shortcutExpansion = ""
                        onChanged()
                    }
                }
            )
        }
        prefs.getTextShortcuts().forEach { (trigger, expansion) ->
            AdwaitaDivider(palette)
            AdwaitaActionRow(
                title = "$trigger  →  $expansion",
                subtitle = "Expands locally on spacebar",
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = "Delete",
                        bg = Color(palette.destructiveRed),
                        fg = Color(palette.accentText),
                        onClick = {
                            val updated = prefs.getTextShortcuts().toMutableMap()
                            updated.remove(trigger)
                            prefs.saveTextShortcuts(updated)
                            onChanged()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun HapticsSoundSection(
    prefs: KeyboardPreferences,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    AdwaitaGroup(title = "Tactile Haptics & Audio", palette = palette) {
        AdwaitaToggleRow(
            title = "Keypress Haptic Feedback",
            subtitle = "Vibrate subtly when pressing keys",
            checked = prefs.hapticsEnabled,
            palette = palette
        ) {
            prefs.hapticsEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Haptic Intensity",
            valueLabel = "${prefs.hapticIntensityMs} ms",
            palette = palette,
            onDecrement = {
                prefs.hapticIntensityMs -= 4
                onChanged()
            },
            onIncrement = {
                prefs.hapticIntensityMs += 4
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Keypress Sound",
            subtitle = "Play subtle click sound on keypress (Off by default)",
            checked = prefs.soundEnabled,
            palette = palette
        ) {
            prefs.soundEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Keypress Sound Volume",
            valueLabel = "${prefs.soundVolumePercent}%",
            palette = palette,
            onDecrement = {
                prefs.soundVolumePercent -= 10
                onChanged()
            },
            onIncrement = {
                prefs.soundVolumePercent += 10
                onChanged()
            }
        )
    }
}

@Composable
private fun ClipboardSettingsSection(
    prefs: KeyboardPreferences,
    clipboardController: ClipboardController,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    val (pinned, recent) = clipboardController.queryEntries()

    AdwaitaGroup(title = "Clipboard History & Expiration", palette = palette) {
        AdwaitaToggleRow(
            title = "Local Clipboard History",
            subtitle = "Store copied items locally in SQLite (paused in password fields)",
            checked = prefs.clipboardHistoryEnabled,
            palette = palette
        ) {
            prefs.clipboardHistoryEnabled = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Auto-Expire Unpinned Clips",
            subtitle = "Each unpinned item expires individually after its countdown",
            checked = prefs.clipboardAutoExpire,
            palette = palette
        ) {
            prefs.clipboardAutoExpire = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaStepperRow(
            title = "Expiration Timeout",
            valueLabel = "${prefs.clipboardExpirationMinutes} min",
            palette = palette,
            onDecrement = {
                prefs.clipboardExpirationMinutes -= 15
                onChanged()
            },
            onIncrement = {
                prefs.clipboardExpirationMinutes += 15
                onChanged()
            }
        )
        AdwaitaDivider(palette)
        AdwaitaToggleRow(
            title = "Clear System Clipboard on Expiry",
            subtitle = "Clears Android's global clipboard when the IME is active",
            checked = prefs.clearSystemClipboardOnExpire,
            palette = palette
        ) {
            prefs.clearSystemClipboardOnExpire = it
            onChanged()
        }
        AdwaitaDivider(palette)
        AdwaitaActionRow(
            title = "Stored Items: ${pinned.size} Pinned, ${recent.size} Recent",
            subtitle = "Pinned items never expire automatically",
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Clear Unpinned",
                    bg = Color(palette.destructiveRed),
                    fg = Color(palette.accentText),
                    onClick = {
                        clipboardController.clearAllUnpinned()
                        onChanged()
                    }
                )
            }
        )
    }
}

@Composable
private fun ToolbarCustomizationSection(
    prefs: KeyboardPreferences,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    val pinned = prefs.pinnedToolbarActions

    AdwaitaGroup(title = "Pinned Headerbar Actions", palette = palette) {
        AdwaitaActionRow(
            title = "Restore Default Toolbar",
            subtitle = "Reset headerbar and popover menu actions to defaults",
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Restore Defaults",
                    bg = Color(palette.pillBg),
                    fg = Color(palette.textPrimary),
                    onClick = {
                        prefs.restoreDefaultToolbar()
                        onChanged()
                    }
                )
            }
        )
        pinned.forEachIndexed { idx, action ->
            AdwaitaDivider(palette)
            AdwaitaActionRow(
                title = action.label,
                subtitle = "Pinned slot #${idx + 1}",
                leadingIcon = action.icon,
                palette = palette,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (idx > 0) {
                            AdwaitaPillButton("Up", Color(palette.pillBg), Color(palette.textPrimary)) {
                                val mut = pinned.toMutableList()
                                val item = mut.removeAt(idx)
                                mut.add(idx - 1, item)
                                prefs.pinnedToolbarActions = mut
                                onChanged()
                            }
                        }
                        if (idx < pinned.lastIndex) {
                            AdwaitaPillButton("Down", Color(palette.pillBg), Color(palette.textPrimary)) {
                                val mut = pinned.toMutableList()
                                val item = mut.removeAt(idx)
                                mut.add(idx + 1, item)
                                prefs.pinnedToolbarActions = mut
                                onChanged()
                            }
                        }
                        AdwaitaPillButton("Remove", Color(palette.destructiveRed), Color(palette.accentText)) {
                            prefs.pinnedToolbarActions = pinned.filter { it != action }
                            onChanged()
                        }
                    }
                }
            )
        }
    }

    AdwaitaGroup(title = "Available Actions (Add to Headerbar)", palette = palette) {
        ToolbarAction.entries.filter { it !in pinned }.forEachIndexed { idx, action ->
            if (idx > 0) AdwaitaDivider(palette)
            AdwaitaActionRow(
                title = action.label,
                subtitle = "Shown inside popover menu; tap Pin to place in headerbar",
                leadingIcon = action.icon,
                palette = palette,
                trailing = {
                    AdwaitaPillButton(
                        label = "Pin to Bar",
                        bg = Color(palette.accentBlue),
                        fg = Color(palette.accentText),
                        onClick = {
                            prefs.pinnedToolbarActions = pinned + action
                            onChanged()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PrivacySection(
    context: Context,
    prefs: KeyboardPreferences,
    dictionaryEngine: LocalDictionaryEngine,
    clipboardController: ClipboardController,
    palette: AdwaitaPalette,
    onChanged: () -> Unit
) {
    var wipeConfirmationBanner by remember { mutableStateOf(false) }

    AdwaitaGroup(title = "Local Data Erasure", palette = palette) {
        AdwaitaActionRow(
            title = "Erase All Locally Stored Data",
            subtitle = "Deletes learned words, personal dictionary, shortcuts, emoji recents, and all clipboard entries.",
            leadingIcon = SymbolicIcon.DELETE,
            palette = palette,
            trailing = {
                AdwaitaPillButton(
                    label = "Erase All Data",
                    bg = Color(palette.destructiveRed),
                    fg = Color(palette.accentText),
                    onClick = {
                        PrivacyManager.wipeAllLocalData(
                            context = context,
                            prefs = prefs,
                            dictionaryEngine = dictionaryEngine,
                            clipboardController = clipboardController,
                            includePreferencesReset = false
                        )
                        wipeConfirmationBanner = true
                        onChanged()
                    }
                )
            }
        )
        if (wipeConfirmationBanner) {
            AdwaitaDivider(palette)
            AdwaitaActionRow(
                title = "All local keyboard data has been erased.",
                subtitle = "Clipboard SQLite database, learned words, and history are now empty.",
                palette = palette,
                trailing = {}
            )
        }
    }

    AdwaitaGroup(title = "Privacy Architecture & Platform Honesty", palette = palette) {
        AdwaitaInfoRow(
            title = "1. Zero Network Access (No INTERNET Permission)",
            body = "Adwaita Keyboard does not declare android.permission.INTERNET. Android's kernel blocks this process from opening any network sockets.",
            palette = palette
        )
        AdwaitaDivider(palette)
        AdwaitaInfoRow(
            title = "2. Strictly Local Storage",
            body = "All dictionary entries, learned frequencies, and SQLite clipboard history reside in app-private storage with cloud backup disabled.",
            palette = palette
        )
        AdwaitaDivider(palette)
        AdwaitaInfoRow(
            title = "3. Honest Text Delivery Disclosure",
            body = "An Android keyboard delivers typed characters to the currently focused app via InputConnection. While Adwaita Keyboard never logs or transmits your text, the target app receives what you type into it.",
            palette = palette
        )
    }
}

@Composable
private fun AboutSection(palette: AdwaitaPalette) {
    AdwaitaGroup(title = "About Adwaita Keyboard", palette = palette) {
        AdwaitaInfoRow(
            title = "Adwaita Keyboard v1.1.0-libadwaita",
            body = "Designed in the GNOME Adwaita visual language using bundled Ubuntu typography and monochrome symbolic vector icons.",
            palette = palette
        )
        AdwaitaDivider(palette)
        AdwaitaInfoRow(
            title = "Permissions & Licenses",
            body = "• Permission: android.permission.VIBRATE (optional keypress haptics)\n• Ubuntu Font License 1.0 (Ubuntu Typography)\n• Apache License 2.0 (AndroidX & Kotlin)",
            palette = palette
        )
    }
}

// --- Standardized libadwaita Compose Components ---

@Composable
private fun AdwaitaGroup(
    title: String,
    palette: AdwaitaPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                fontFamily = ubuntuFont,
                color = Color(palette.textPrimary),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(palette.cardBg))
                .border(1.dp, Color(palette.keyBorder), RoundedCornerShape(12.dp)),
            content = content
        )
    }
}

@Composable
private fun AdwaitaActionRow(
    title: String,
    subtitle: String,
    palette: AdwaitaPalette,
    leadingIcon: SymbolicIcon? = null,
    trailing: @Composable () -> Unit
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (leadingIcon != null) {
            AdwaitaIcons.SymbolicIconView(
                icon = leadingIcon,
                tint = palette.textSecondary,
                size = 18.dp,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            BasicText(
                text = title,
                style = TextStyle(
                    fontFamily = ubuntuFont,
                    color = Color(palette.textPrimary),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                BasicText(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = ubuntuFont,
                        color = Color(palette.textSecondary),
                        fontSize = 12.sp
                    )
                )
            }
        }
        trailing()
    }
}

@Composable
private fun AdwaitaToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: AdwaitaPalette,
    onToggle: (Boolean) -> Unit
) {
    AdwaitaActionRow(
        title = title,
        subtitle = subtitle,
        palette = palette,
        trailing = {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(palette.accentBlue) else Color(palette.pillBg))
                    .clickable { onToggle(!checked) }
                    .padding(3.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    )
}

@Composable
private fun AdwaitaStepperRow(
    title: String,
    valueLabel: String,
    palette: AdwaitaPalette,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    AdwaitaActionRow(
        title = title,
        subtitle = "Current value: $valueLabel",
        palette = palette,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdwaitaPillButton("−", Color(palette.pillBg), Color(palette.textPrimary), onDecrement)
                BasicText(
                    text = valueLabel,
                    style = TextStyle(
                        fontFamily = ubuntuFont,
                        color = Color(palette.textPrimary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                AdwaitaPillButton("+", Color(palette.pillBg), Color(palette.textPrimary), onIncrement)
            }
        }
    )
}

@Composable
private fun AdwaitaInfoRow(
    title: String,
    body: String,
    palette: AdwaitaPalette
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                fontFamily = ubuntuFont,
                color = Color(palette.textPrimary),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = body,
            style = TextStyle(
                fontFamily = ubuntuFont,
                color = Color(palette.textSecondary),
                fontSize = 12.5.sp
            )
        )
    }
}

@Composable
private fun AdwaitaPillButton(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = ubuntuFont,
                color = fg,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AdwaitaIconPillButton(
    icon: SymbolicIcon,
    label: String,
    bg: Color,
    fg: Int,
    onClick: () -> Unit
) {
    val ubuntuFont = AdwaitaTheme.composeUbuntuFontFamily
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AdwaitaIcons.SymbolicIconView(icon = icon, tint = fg, size = 16.dp)
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = ubuntuFont,
                color = Color(fg),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AdwaitaDivider(palette: AdwaitaPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(palette.keyBorder))
    )
}
