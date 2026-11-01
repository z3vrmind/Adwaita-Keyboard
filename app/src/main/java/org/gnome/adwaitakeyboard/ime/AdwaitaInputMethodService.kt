package org.gnome.adwaitakeyboard.ime

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.gnome.adwaitakeyboard.clipboard.ClipboardController
import org.gnome.adwaitakeyboard.dictionary.LocalDictionaryEngine
import org.gnome.adwaitakeyboard.editing.EditHistoryManager
import org.gnome.adwaitakeyboard.prefs.KeyboardLayoutType
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import org.gnome.adwaitakeyboard.prefs.OneHandedMode
import org.gnome.adwaitakeyboard.prefs.ToolbarAction
import org.gnome.adwaitakeyboard.privacy.DetectedInputMode
import org.gnome.adwaitakeyboard.privacy.InputSecurityContext
import org.gnome.adwaitakeyboard.privacy.PrivacyManager
import org.gnome.adwaitakeyboard.theme.AdwaitaTheme
import org.gnome.adwaitakeyboard.ui.SettingsActivity

class AdwaitaInputMethodService : InputMethodService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private enum class ActivePanel {
        KEYBOARD,
        CLIPBOARD,
        EMOJI
    }

    private enum class KeyboardPage {
        ALPHABET,
        SYMBOLS_1,
        SYMBOLS_2,
        NUMERIC_CALC,
        PHONE
    }

    private lateinit var prefs: KeyboardPreferences
    private lateinit var dictionaryEngine: LocalDictionaryEngine
    private lateinit var clipboardController: ClipboardController
    private val editHistory = EditHistoryManager()

    private var securityContext: InputSecurityContext? = null
    private var activePanel: ActivePanel = ActivePanel.KEYBOARD
    private var currentPage: KeyboardPage = KeyboardPage.ALPHABET
    private var shiftState: ShiftState = ShiftState.LOWERCASE
    private var lastShiftTapTime = 0L
    private var lastSpaceTapTime = 0L

    private var rootDockWrapper: LinearLayout? = null
    private var mainColumn: LinearLayout? = null
    private var topBarView: AdwaitaTopBarView? = null
    private var contentFrame: FrameLayout? = null
    private var keyboardView: AdwaitaKeyboardView? = null
    private var clipboardPanelView: ClipboardPanelView? = null
    private var emojiPickerView: EmojiPickerView? = null

    override fun onCreate() {
        super.onCreate()
        prefs = KeyboardPreferences(this)
        dictionaryEngine = LocalDictionaryEngine(this, prefs)
        clipboardController = ClipboardController(this, prefs)
        prefs.registerListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterListener(this)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        rootDockWrapper = wrapper

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainColumn = column

        val topBar = AdwaitaTopBarView(
            context = this,
            prefs = prefs,
            onSuggestionClicked = { suggestion -> commitSuggestion(suggestion) },
            onToolbarActionTriggered = { action -> handleToolbarAction(action) }
        )
        topBarView = topBar

        val frame = FrameLayout(this)
        contentFrame = frame

        val kbView = AdwaitaKeyboardView(
            context = this,
            prefs = prefs,
            onKeyTriggered = { spec -> handleKeyPress(spec) },
            onSpaceCursorSlide = { dir ->
                triggerFeedback()
                editHistory.moveCursor(currentInputConnection, dir)
            },
            onLongPressSpaceLayoutSwitch = {
                cycleKeyboardLayout()
            }
        )
        keyboardView = kbView

        val clipView = ClipboardPanelView(
            context = this,
            prefs = prefs,
            clipboardController = clipboardController,
            onPasteText = { text ->
                commitDirectText(text)
                switchPanel(ActivePanel.KEYBOARD)
            },
            onBackToKeyboard = { switchPanel(ActivePanel.KEYBOARD) }
        )
        clipboardPanelView = clipView

        val emojiView = EmojiPickerView(
            context = this,
            prefs = prefs,
            onEmojiSelected = { emoji -> commitDirectText(emoji) },
            onBackspaceClicked = { handleBackspace() },
            onBackToKeyboard = { switchPanel(ActivePanel.KEYBOARD) }
        )
        emojiPickerView = emojiView

        frame.addView(kbView)
        frame.addView(clipView.apply { visibility = View.GONE })
        frame.addView(emojiView.apply { visibility = View.GONE })

        column.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        column.addView(
            frame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        applyOneHandedLayout()
        refreshFullKeyboardUI()
        return wrapper
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editHistory.clearHistory()
        val ctx = PrivacyManager.inspectEditorInfo(attribute, prefs)
        securityContext = ctx

        currentPage = when (ctx.inputMode) {
            DetectedInputMode.PHONE_NUMBER -> KeyboardPage.PHONE
            DetectedInputMode.NUMERIC_CALC,
            DetectedInputMode.DECIMAL_NUMBER,
            DetectedInputMode.SIGNED_NUMBER,
            DetectedInputMode.SENSITIVE_NUMBER -> KeyboardPage.NUMERIC_CALC
            else -> KeyboardPage.ALPHABET
        }

        updateAutoCapitalizationState()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val ctx = PrivacyManager.inspectEditorInfo(info, prefs)
        securityContext = ctx

        clipboardController.purgeExpiredEntries()
        clipboardController.syncFromSystemClipboard(allowCapture = ctx.allowClipboardCapture)

        activePanel = ActivePanel.KEYBOARD
        refreshFullKeyboardUI()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateAutoCapitalizationState()
        updateSuggestionsBar()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOneHandedLayout()
        refreshFullKeyboardUI()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        securityContext = PrivacyManager.inspectEditorInfo(currentInputEditorInfo, prefs)
        applyOneHandedLayout()
        refreshFullKeyboardUI()
    }

    private fun applyOneHandedLayout() {
        val wrapper = rootDockWrapper ?: return
        val column = mainColumn ?: return
        val palette = AdwaitaTheme.resolvePalette(this, prefs)
        wrapper.removeAllViews()
        wrapper.setBackgroundColor(palette.windowBg)

        val mode = prefs.oneHandedMode
        if (mode == OneHandedMode.OFF) {
            wrapper.addView(
                column,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }

        val expandBtn = TextView(this).apply {
            text = if (mode == OneHandedMode.LEFT) "⇥\nFull" else "⇤\nFull"
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
            setOnClickListener {
                prefs.oneHandedMode = OneHandedMode.OFF
                applyOneHandedLayout()
                refreshFullKeyboardUI()
            }
        }

        if (mode == OneHandedMode.LEFT) {
            wrapper.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.82f))
            wrapper.addView(expandBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.18f))
        } else {
            wrapper.addView(expandBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.18f))
            wrapper.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.82f))
        }
    }

    private fun switchPanel(panel: ActivePanel) {
        activePanel = panel
        keyboardView?.visibility = if (panel == ActivePanel.KEYBOARD) View.VISIBLE else View.GONE
        clipboardPanelView?.visibility = if (panel == ActivePanel.CLIPBOARD) View.VISIBLE else View.GONE
        emojiPickerView?.visibility = if (panel == ActivePanel.EMOJI) View.VISIBLE else View.GONE

        if (panel == ActivePanel.CLIPBOARD) {
            clipboardPanelView?.refreshEntries()
        } else if (panel == ActivePanel.EMOJI) {
            emojiPickerView?.refreshEmojiView()
        } else {
            refreshFullKeyboardUI()
        }
    }

    private fun refreshFullKeyboardUI() {
        val rows = when (currentPage) {
            KeyboardPage.ALPHABET -> KeyboardLayouts.buildAlphabetRows(
                layoutType = prefs.layoutType,
                includeNumberRow = prefs.showNumberRow,
                shiftState = shiftState
            )
            KeyboardPage.SYMBOLS_1 -> KeyboardLayouts.buildSymbolsRows(pageTwo = false)
            KeyboardPage.SYMBOLS_2 -> KeyboardLayouts.buildSymbolsRows(pageTwo = true)
            KeyboardPage.NUMERIC_CALC -> KeyboardLayouts.buildNumericCalculatorRows()
            KeyboardPage.PHONE -> KeyboardLayouts.buildPhoneRows()
        }
        keyboardView?.setKeyboardRows(rows)
        keyboardView?.refreshThemeAndPreferences()
        updateSuggestionsBar()
    }

    private fun handleKeyPress(spec: KeySpec) {
        triggerFeedback()
        topBarView?.closeMenuIfOpen()

        when (spec.type) {
            KeyActionType.CHAR -> handleCharInput(spec.outputText)
            KeyActionType.SPACE -> handleSpaceInput()
            KeyActionType.BACKSPACE -> handleBackspace()
            KeyActionType.SHIFT -> handleShiftTap()
            KeyActionType.ENTER -> handleEnterKey()
            KeyActionType.MODE_SYMBOLS -> {
                currentPage = KeyboardPage.SYMBOLS_1
                refreshFullKeyboardUI()
            }
            KeyActionType.MODE_MORE_SYMBOLS -> {
                currentPage = KeyboardPage.SYMBOLS_2
                refreshFullKeyboardUI()
            }
            KeyActionType.MODE_ABC -> {
                currentPage = KeyboardPage.ALPHABET
                refreshFullKeyboardUI()
            }
            KeyActionType.EMOJI_PANEL -> switchPanel(ActivePanel.EMOJI)
            KeyActionType.CLIPBOARD_PANEL -> switchPanel(ActivePanel.CLIPBOARD)
            KeyActionType.LAYOUT_SWITCH -> cycleKeyboardLayout()
        }
    }

    private fun handleCharInput(text: String) {
        val ic = currentInputConnection ?: return
        if (text.length == 1 && (text[0] in listOf('.', ',', '!', '?', ';', ':'))) {
            applyAutocorrectBeforeSeparatorIfApplicable()
        }

        val cursorBefore = getCursorPosition()
        ic.commitText(text, 1)
        editHistory.recordInsertion(cursorBefore, text)

        if (shiftState == ShiftState.SHIFTED_SINGLE) {
            shiftState = ShiftState.LOWERCASE
            updateAutoCapitalizationState()
            refreshFullKeyboardUI()
        } else {
            updateSuggestionsBar()
        }
    }

    private fun handleSpaceInput() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        val ctx = securityContext ?: PrivacyManager.inspectEditorInfo(currentInputEditorInfo, prefs)

        if (prefs.doubleSpacePeriod &&
            currentPage == KeyboardPage.ALPHABET &&
            (now - lastSpaceTapTime) < 320L
        ) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                ic.endBatchEdit()
                lastSpaceTapTime = 0L
                updateAutoCapitalizationState()
                refreshFullKeyboardUI()
                return
            }
        }
        lastSpaceTapTime = now

        val wordBefore = getCurrentWordBeforeCursor()
        val corrected = applyAutocorrectBeforeSeparatorIfApplicable()
        if (corrected == null && wordBefore.length >= 2) {
            dictionaryEngine.learnWordLocally(wordBefore, canLearn = ctx.isPersonalizedLearningAllowed)
        }

        val cursorBefore = getCursorPosition()
        ic.commitText(" ", 1)
        editHistory.recordInsertion(cursorBefore, " ")
        updateAutoCapitalizationState()
        refreshFullKeyboardUI()
    }

    private fun applyAutocorrectBeforeSeparatorIfApplicable(): String? {
        val ic = currentInputConnection ?: return null
        val ctx = securityContext ?: return null
        val currentWord = getCurrentWordBeforeCursor()
        if (currentWord.isEmpty()) return null

        val replacement = dictionaryEngine.resolveAutocorrectOrShortcut(
            currentWord = currentWord,
            allowAutocorrect = ctx.allowAutocorrect
        ) ?: return null

        if (replacement != currentWord) {
            val cursorPos = getCursorPosition()
            val wordStart = (cursorPos - currentWord.length).coerceAtLeast(0)
            ic.beginBatchEdit()
            ic.deleteSurroundingText(currentWord.length, 0)
            ic.commitText(replacement, 1)
            ic.endBatchEdit()
            editHistory.recordReplacement(wordStart, currentWord, replacement)
            return replacement
        }
        return null
    }

    private fun commitSuggestion(suggestion: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return
        val ctx = securityContext ?: PrivacyManager.inspectEditorInfo(currentInputEditorInfo, prefs)
        val currentWord = getCurrentWordBeforeCursor()
        val cursorPos = getCursorPosition()

        ic.beginBatchEdit()
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        val toCommit = "$suggestion "
        ic.commitText(toCommit, 1)
        ic.endBatchEdit()

        val wordStart = (cursorPos - currentWord.length).coerceAtLeast(0)
        editHistory.recordReplacement(wordStart, currentWord, toCommit)
        dictionaryEngine.learnWordLocally(suggestion, canLearn = ctx.isPersonalizedLearningAllowed)

        updateAutoCapitalizationState()
        refreshFullKeyboardUI()
    }

    private fun commitDirectText(text: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return
        val cursorBefore = getCursorPosition()
        ic.commitText(text, 1)
        editHistory.recordInsertion(cursorBefore, text)
        updateSuggestionsBar()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            val cursorPos = getCursorPosition()
            ic.commitText("", 1)
            editHistory.recordDeletion(cursorPos, selected)
        } else {
            val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (before.isNotEmpty()) {
                val deleteCount = if (before.length == 2 &&
                    Character.isSurrogatePair(before[0], before[1])
                ) 2 else 1
                val deletedStr = before.takeLast(deleteCount)
                val cursorAfter = (getCursorPosition() - deleteCount).coerceAtLeast(0)
                ic.deleteSurroundingText(deleteCount, 0)
                editHistory.recordDeletion(cursorAfter, deletedStr)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        }
        updateAutoCapitalizationState()
        refreshFullKeyboardUI()
    }

    private fun handleShiftTap() {
        val now = System.currentTimeMillis()
        shiftState = when (shiftState) {
            ShiftState.LOWERCASE -> {
                lastShiftTapTime = now
                ShiftState.SHIFTED_SINGLE
            }
            ShiftState.SHIFTED_SINGLE -> {
                if (now - lastShiftTapTime < 380L) {
                    ShiftState.CAPS_LOCK
                } else {
                    ShiftState.LOWERCASE
                }
            }
            ShiftState.CAPS_LOCK -> ShiftState.LOWERCASE
        }
        refreshFullKeyboardUI()
    }

    private fun handleEnterKey() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        if (info != null) {
            val actionId = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (!noEnterAction && actionId in listOf(
                    EditorInfo.IME_ACTION_GO,
                    EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_SEND,
                    EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_DONE
                )
            ) {
                ic.performEditorAction(actionId)
                return
            }
        }
        val cursorBefore = getCursorPosition()
        ic.commitText("\n", 1)
        editHistory.recordInsertion(cursorBefore, "\n")
        updateAutoCapitalizationState()
        refreshFullKeyboardUI()
    }

    private fun handleToolbarAction(action: ToolbarAction) {
        triggerFeedback()
        val ic = currentInputConnection
        when (action) {
            ToolbarAction.UNDO -> {
                editHistory.performUndo(ic)
                updateSuggestionsBar()
            }
            ToolbarAction.REDO -> {
                editHistory.performRedo(ic)
                updateSuggestionsBar()
            }
            ToolbarAction.COPY -> {
                ic?.performContextMenuAction(android.R.id.copy)
                val ctx = securityContext
                if (ctx?.allowClipboardCapture != false) {
                    clipboardController.syncFromSystemClipboard(allowCapture = true)
                }
            }
            ToolbarAction.CUT -> {
                val selected = ic?.getSelectedText(0)?.toString() ?: ""
                val cursorAfter = getCursorPosition()
                ic?.performContextMenuAction(android.R.id.cut)
                if (selected.isNotEmpty()) {
                    editHistory.recordDeletion(cursorAfter, selected)
                }
                val ctx = securityContext
                if (ctx?.allowClipboardCapture != false) {
                    clipboardController.syncFromSystemClipboard(allowCapture = true)
                }
            }
            ToolbarAction.PASTE -> {
                ic?.performContextMenuAction(android.R.id.paste)
            }
            ToolbarAction.SELECT_ALL -> {
                ic?.performContextMenuAction(android.R.id.selectAll)
            }
            ToolbarAction.CURSOR_LEFT -> {
                editHistory.moveCursor(ic, -1)
            }
            ToolbarAction.CURSOR_RIGHT -> {
                editHistory.moveCursor(ic, 1)
            }
            ToolbarAction.CLIPBOARD -> {
                switchPanel(
                    if (activePanel == ActivePanel.CLIPBOARD) ActivePanel.KEYBOARD else ActivePanel.CLIPBOARD
                )
            }
            ToolbarAction.EMOJI -> {
                switchPanel(
                    if (activePanel == ActivePanel.EMOJI) ActivePanel.KEYBOARD else ActivePanel.EMOJI
                )
            }
            ToolbarAction.PRIVATE_MODE -> {
                prefs.privateModeSession = !prefs.privateModeSession
                securityContext = PrivacyManager.inspectEditorInfo(currentInputEditorInfo, prefs)
                refreshFullKeyboardUI()
            }
            ToolbarAction.ONE_HANDED -> {
                prefs.oneHandedMode = when (prefs.oneHandedMode) {
                    OneHandedMode.OFF -> OneHandedMode.RIGHT
                    OneHandedMode.RIGHT -> OneHandedMode.LEFT
                    OneHandedMode.LEFT -> OneHandedMode.OFF
                }
                applyOneHandedLayout()
                refreshFullKeyboardUI()
            }
            ToolbarAction.SETTINGS -> {
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    private fun cycleKeyboardLayout() {
        val entries = KeyboardLayoutType.entries
        val nextIdx = (entries.indexOf(prefs.layoutType) + 1) % entries.size
        prefs.layoutType = entries[nextIdx]
        currentPage = KeyboardPage.ALPHABET
        refreshFullKeyboardUI()
    }

    private fun updateAutoCapitalizationState() {
        if (shiftState == ShiftState.CAPS_LOCK) return
        if (!prefs.autoCapitalization) {
            if (shiftState == ShiftState.SHIFTED_SINGLE) shiftState = ShiftState.LOWERCASE
            return
        }
        val info = currentInputEditorInfo
        val ic = currentInputConnection
        if (info == null || ic == null) return

        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            shiftState = ShiftState.LOWERCASE
            return
        }

        val cursorCaps = ic.getCursorCapsMode(
            info.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        shiftState = if (cursorCaps != 0) ShiftState.SHIFTED_SINGLE else ShiftState.LOWERCASE
    }

    private fun updateSuggestionsBar() {
        val ctx = securityContext ?: PrivacyManager.inspectEditorInfo(currentInputEditorInfo, prefs)
        val currentWord = getCurrentWordBeforeCursor()
        val prevWord = getPreviousWordBeforeCursor()
        val isAlphabetPage = currentPage == KeyboardPage.ALPHABET

        val suggestions = if (isAlphabetPage) {
            dictionaryEngine.getSuggestions(
                currentWord = currentWord,
                previousWord = prevWord,
                allowSuggestions = ctx.allowSuggestions
            )
        } else {
            emptyList()
        }

        val charWeights = if (isAlphabetPage) {
            dictionaryEngine.getNextCharacterWeights(
                currentPrefix = currentWord,
                allowAdaptive = prefs.adaptiveTouchEnabled && !ctx.isSensitiveField && !ctx.isPrivateSessionActive
            )
        } else {
            emptyMap()
        }
        keyboardView?.setNextCharacterWeights(charWeights)

        topBarView?.updateState(
            suggestions = suggestions,
            isSensitive = ctx.isSensitiveField,
            isPrivateMode = ctx.isPrivateSessionActive
        )
    }

    private fun getCurrentWordBeforeCursor(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(32, 0)?.toString() ?: return ""
        var idx = before.length - 1
        while (idx >= 0 && !before[idx].isWhitespace()) {
            idx--
        }
        return before.substring(idx + 1)
    }

    private fun getPreviousWordBeforeCursor(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: return ""
        val parts = before.trim().split(Regex("\\s+"))
        return if (parts.size >= 2) parts[parts.size - 2] else parts.firstOrNull() ?: ""
    }

    private fun getCursorPosition(): Int {
        val ic = currentInputConnection ?: return 0
        return try {
            ic.getExtractedText(ExtractedTextRequest(), 0)?.selectionStart ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun triggerFeedback() {
        if (prefs.hapticsEnabled) {
            try {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator?.hasVibrator() == true) {
                    val durationMs = prefs.hapticIntensityMs.toLong()
                    val amplitude = ((prefs.hapticIntensityMs / 60f) * 255).toInt().coerceIn(25, 255)
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                }
            } catch (_: Exception) {
            }
        }

        if (prefs.soundEnabled) {
            try {
                val am = getSystemService(AUDIO_SERVICE) as? AudioManager
                val vol = prefs.soundVolumePercent / 100f
                am?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol)
            } catch (_: Exception) {
            }
        }
    }
}
