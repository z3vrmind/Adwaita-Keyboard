package org.gnome.adwaitakeyboard.privacy

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.gnome.adwaitakeyboard.clipboard.ClipboardController
import org.gnome.adwaitakeyboard.dictionary.LocalDictionaryEngine
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences

enum class DetectedInputMode {
    STANDARD_TEXT,
    SENSITIVE_TEXT,
    SENSITIVE_NUMBER,
    NUMERIC_CALC,
    DECIMAL_NUMBER,
    SIGNED_NUMBER,
    PHONE_NUMBER,
    EMAIL_OR_URI
}

data class InputSecurityContext(
    val inputMode: DetectedInputMode,
    val isSensitiveField: Boolean,
    val isPersonalizedLearningAllowed: Boolean,
    val isPrivateSessionActive: Boolean,
    val allowSuggestions: Boolean,
    val allowAutocorrect: Boolean,
    val allowClipboardCapture: Boolean,
    val isSingleLine: Boolean
)

object PrivacyManager {

    fun inspectEditorInfo(
        editorInfo: EditorInfo?,
        prefs: KeyboardPreferences
    ): InputSecurityContext {
        val privateSession = prefs.privateModeSession
        if (editorInfo == null) {
            return InputSecurityContext(
                inputMode = DetectedInputMode.STANDARD_TEXT,
                isSensitiveField = false,
                isPersonalizedLearningAllowed = !privateSession,
                isPrivateSessionActive = privateSession,
                allowSuggestions = prefs.suggestionsEnabled && !privateSession,
                allowAutocorrect = prefs.autocorrectEnabled && !privateSession,
                allowClipboardCapture = !privateSession,
                isSingleLine = true
            )
        }

        val inputType = editorInfo.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val flags = inputType and InputType.TYPE_MASK_FLAGS

        val noPersonalizedLearningFlag =
            (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0

        val isPasswordText = inputClass == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )

        val isPasswordNumber = inputClass == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

        val isNoSuggestionsVariation = inputClass == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_FILTER ||
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            (flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        )

        val isSensitive = isPasswordText || isPasswordNumber || noPersonalizedLearningFlag

        val detectedMode = when {
            isPasswordNumber -> DetectedInputMode.SENSITIVE_NUMBER
            isPasswordText -> DetectedInputMode.SENSITIVE_TEXT
            inputClass == InputType.TYPE_CLASS_PHONE -> DetectedInputMode.PHONE_NUMBER
            inputClass == InputType.TYPE_CLASS_NUMBER -> {
                val isDecimal = (flags and InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0
                val isSigned = (flags and InputType.TYPE_NUMBER_FLAG_SIGNED) != 0
                when {
                    isDecimal -> DetectedInputMode.DECIMAL_NUMBER
                    isSigned -> DetectedInputMode.SIGNED_NUMBER
                    else -> DetectedInputMode.NUMERIC_CALC
                }
            }
            inputClass == InputType.TYPE_CLASS_DATETIME -> DetectedInputMode.NUMERIC_CALC
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI -> DetectedInputMode.EMAIL_OR_URI
            else -> DetectedInputMode.STANDARD_TEXT
        }

        val isMultiline = (flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ||
            (flags and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) != 0

        val canSuggest = !isSensitive &&
            !privateSession &&
            !isNoSuggestionsVariation &&
            prefs.suggestionsEnabled &&
            detectedMode == DetectedInputMode.STANDARD_TEXT

        val canAutocorrect = canSuggest &&
            prefs.autocorrectEnabled &&
            (flags and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0 ||
            (canSuggest && prefs.autocorrectEnabled && variation == InputType.TYPE_NULL)

        return InputSecurityContext(
            inputMode = detectedMode,
            isSensitiveField = isSensitive,
            isPersonalizedLearningAllowed = !isSensitive && !privateSession,
            isPrivateSessionActive = privateSession,
            allowSuggestions = canSuggest,
            allowAutocorrect = canAutocorrect,
            allowClipboardCapture = !isSensitive && !privateSession && prefs.clipboardHistoryEnabled,
            isSingleLine = !isMultiline
        )
    }

    /**
     * Erases all locally stored data: learned words, personal dictionary,
     * text shortcuts, clipboard database (including pinned if requested),
     * emoji usage history, and resets preferences if requested.
     */
    fun wipeAllLocalData(
        context: Context,
        prefs: KeyboardPreferences,
        dictionaryEngine: LocalDictionaryEngine,
        clipboardController: ClipboardController,
        includePreferencesReset: Boolean = false
    ) {
        dictionaryEngine.clearAllLearnedAndPersonalData()
        clipboardController.wipeAllClipboardData(clearSystemClipboardToo = true)
        prefs.clearAllEmojiAndPrivateHistory()
        if (includePreferencesReset) {
            prefs.resetAllToDefaults()
        }
    }
}
