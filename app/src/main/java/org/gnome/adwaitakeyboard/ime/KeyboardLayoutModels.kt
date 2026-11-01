package org.gnome.adwaitakeyboard.ime

import org.gnome.adwaitakeyboard.prefs.KeyboardLayoutType
import org.gnome.adwaitakeyboard.theme.SymbolicIcon

enum class KeyActionType {
    CHAR,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    MODE_SYMBOLS,
    MODE_ABC,
    MODE_MORE_SYMBOLS,
    EMOJI_PANEL,
    CLIPBOARD_PANEL,
    LAYOUT_SWITCH
}

enum class ShiftState {
    LOWERCASE,
    SHIFTED_SINGLE,
    CAPS_LOCK
}

data class KeySpec(
    val label: String,
    val outputText: String = label,
    val type: KeyActionType = KeyActionType.CHAR,
    val widthWeight: Float = 1.0f,
    val isSpecial: Boolean = false,
    val isAccent: Boolean = false,
    val secondaryHint: String = "",
    val symbolicIcon: SymbolicIcon? = null,
    val longPressAlternatives: List<String> = emptyList()
) {
    val isNonCharacterControl: Boolean
        get() = type != KeyActionType.CHAR && type != KeyActionType.SPACE
}

object KeyboardLayouts {

    private val longPressMap: Map<String, List<String>> = mapOf(
        "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā", "@"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę", "€"),
        "i" to listOf("ì", "í", "î", "ï", "ī", "į"),
        "o" to listOf("ò", "ó", "ô", "ö", "œ", "õ", "ø", "ō"),
        "u" to listOf("ù", "ú", "û", "ü", "ū"),
        "s" to listOf("ß", "ś", "š", "$"),
        "c" to listOf("ç", "ć", "č", "©"),
        "n" to listOf("ñ", "ń"),
        "y" to listOf("ÿ", "¥"),
        "z" to listOf("ž", "ź", "ż"),
        "l" to listOf("ł", "£"),
        "." to listOf(",", "?", "!", ";", ":", "…", "/", "'", "\""),
        "," to listOf(";", ":", "'", "\"", "-"),
        "1" to listOf("¹", "½", "⅓", "¼", "¡"),
        "2" to listOf("²", "⅔", "@"),
        "3" to listOf("³", "¾", "#"),
        "4" to listOf("⁴", "$", "€", "£"),
        "5" to listOf("⁵", "%", "‰"),
        "6" to listOf("⁶", "^"),
        "7" to listOf("⁷", "&"),
        "8" to listOf("⁸", "*"),
        "9" to listOf("⁹", "("),
        "0" to listOf("⁰", ")", "°", "∅")
    )

    private val numberRow: List<KeySpec> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
        KeySpec(
            label = digit,
            outputText = digit,
            longPressAlternatives = longPressMap[digit] ?: emptyList()
        )
    }

    fun buildAlphabetRows(
        layoutType: KeyboardLayoutType,
        includeNumberRow: Boolean,
        shiftState: ShiftState
    ): List<List<KeySpec>> {
        val rawRows: List<List<String>> = when (layoutType) {
            KeyboardLayoutType.QWERTY -> listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
            KeyboardLayoutType.QWERTZ -> listOf(
                listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("y", "x", "c", "v", "b", "n", "m")
            )
            KeyboardLayoutType.AZERTY -> listOf(
                listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
                listOf("w", "x", "c", "v", "b", "n")
            )
            KeyboardLayoutType.DVORAK -> listOf(
                listOf("'", ",", ".", "p", "y", "f", "g", "c", "r", "l"),
                listOf("a", "o", "e", "u", "i", "d", "h", "t", "n", "s"),
                listOf(";", "q", "j", "k", "x", "b", "m", "w", "v", "z")
            )
            KeyboardLayoutType.COLEMAK -> listOf(
                listOf("q", "w", "f", "p", "g", "j", "l", "u", "y", ";"),
                listOf("a", "r", "s", "t", "d", "h", "n", "e", "i", "o"),
                listOf("z", "x", "c", "v", "b", "k", "m")
            )
        }

        val topHintMap = mapOf(
            "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
            "y" to "6", "z" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"
        )

        val rows = ArrayList<List<KeySpec>>()
        if (includeNumberRow) {
            rows.add(numberRow)
        }

        val uppercase = shiftState != ShiftState.LOWERCASE
        rawRows.forEachIndexed { rowIndex, letters ->
            val mappedLetters = letters.map { ch ->
                val display = if (uppercase) ch.uppercase() else ch
                val baseAlts = longPressMap[ch] ?: emptyList()
                val altsWithDigit = if (!includeNumberRow && rowIndex == 0 && topHintMap.containsKey(ch)) {
                    listOf(topHintMap[ch]!!) + baseAlts
                } else {
                    baseAlts
                }
                val finalAlts = if (uppercase) altsWithDigit.map { it.uppercase() } else altsWithDigit
                KeySpec(
                    label = display,
                    outputText = display,
                    secondaryHint = if (!includeNumberRow && rowIndex == 0) topHintMap[ch] ?: "" else "",
                    longPressAlternatives = finalAlts
                )
            }

            if (rowIndex == 2) {
                val shiftIcon = when (shiftState) {
                    ShiftState.LOWERCASE -> SymbolicIcon.SHIFT
                    ShiftState.SHIFTED_SINGLE -> SymbolicIcon.SHIFT_FILLED
                    ShiftState.CAPS_LOCK -> SymbolicIcon.CAPS_LOCK
                }
                val row3 = ArrayList<KeySpec>()
                row3.add(
                    KeySpec(
                        label = "Shift",
                        type = KeyActionType.SHIFT,
                        widthWeight = 1.58f,
                        isSpecial = true,
                        isAccent = shiftState == ShiftState.CAPS_LOCK,
                        symbolicIcon = shiftIcon
                    )
                )
                row3.addAll(mappedLetters)
                row3.add(
                    KeySpec(
                        label = "Bksp",
                        type = KeyActionType.BACKSPACE,
                        widthWeight = 1.58f,
                        isSpecial = true,
                        symbolicIcon = SymbolicIcon.BACKSPACE
                    )
                )
                rows.add(row3)
            } else {
                rows.add(mappedLetters)
            }
        }

        rows.add(
            listOf(
                KeySpec(label = "?123", type = KeyActionType.MODE_SYMBOLS, widthWeight = 1.45f, isSpecial = true),
                KeySpec(
                    label = "Emoji",
                    type = KeyActionType.EMOJI_PANEL,
                    widthWeight = 1.1f,
                    isSpecial = true,
                    symbolicIcon = SymbolicIcon.EMOJI
                ),
                KeySpec(
                    label = ",",
                    outputText = ",",
                    widthWeight = 0.95f,
                    longPressAlternatives = longPressMap[","] ?: emptyList()
                ),
                KeySpec(
                    label = layoutType.label.substringBefore(" "),
                    outputText = " ",
                    type = KeyActionType.SPACE,
                    widthWeight = 3.65f
                ),
                KeySpec(
                    label = ".",
                    outputText = ".",
                    widthWeight = 0.95f,
                    longPressAlternatives = longPressMap["."] ?: emptyList()
                ),
                KeySpec(
                    label = "Enter",
                    type = KeyActionType.ENTER,
                    widthWeight = 1.68f,
                    isSpecial = true,
                    isAccent = true,
                    symbolicIcon = SymbolicIcon.ENTER
                )
            )
        )

        return rows
    }

    fun buildSymbolsRows(pageTwo: Boolean): List<List<KeySpec>> {
        return if (!pageTwo) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeySpec(it) },
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/").map { KeySpec(it) },
                listOf(
                    KeySpec(label = "=\\<", type = KeyActionType.MODE_MORE_SYMBOLS, widthWeight = 1.58f, isSpecial = true),
                    KeySpec("*"), KeySpec("\""), KeySpec("'"), KeySpec(":"), KeySpec(";"), KeySpec("!"), KeySpec("?"),
                    KeySpec(
                        label = "Bksp",
                        type = KeyActionType.BACKSPACE,
                        widthWeight = 1.58f,
                        isSpecial = true,
                        symbolicIcon = SymbolicIcon.BACKSPACE
                    )
                ),
                listOf(
                    KeySpec(label = "ABC", type = KeyActionType.MODE_ABC, widthWeight = 1.45f, isSpecial = true),
                    KeySpec(
                        label = "Emoji",
                        type = KeyActionType.EMOJI_PANEL,
                        widthWeight = 1.1f,
                        isSpecial = true,
                        symbolicIcon = SymbolicIcon.EMOJI
                    ),
                    KeySpec(label = ","),
                    KeySpec(label = "Space", outputText = " ", type = KeyActionType.SPACE, widthWeight = 3.65f),
                    KeySpec(label = "."),
                    KeySpec(
                        label = "Enter",
                        type = KeyActionType.ENTER,
                        widthWeight = 1.68f,
                        isSpecial = true,
                        isAccent = true,
                        symbolicIcon = SymbolicIcon.ENTER
                    )
                )
            )
        } else {
            listOf(
                listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map { KeySpec(it) },
                listOf("€", "£", "¥", "¢", "^", "°", "=", "{", "}", "\\").map { KeySpec(it) },
                listOf(
                    KeySpec(label = "?123", type = KeyActionType.MODE_SYMBOLS, widthWeight = 1.58f, isSpecial = true),
                    KeySpec("%"), KeySpec("©"), KeySpec("®"), KeySpec("™"), KeySpec("✓"), KeySpec("["), KeySpec("]"),
                    KeySpec(
                        label = "Bksp",
                        type = KeyActionType.BACKSPACE,
                        widthWeight = 1.58f,
                        isSpecial = true,
                        symbolicIcon = SymbolicIcon.BACKSPACE
                    )
                ),
                listOf(
                    KeySpec(label = "ABC", type = KeyActionType.MODE_ABC, widthWeight = 1.45f, isSpecial = true),
                    KeySpec(label = "<", outputText = "<"),
                    KeySpec(label = ">", outputText = ">"),
                    KeySpec(label = "Space", outputText = " ", type = KeyActionType.SPACE, widthWeight = 3.65f),
                    KeySpec(label = "…", outputText = "…"),
                    KeySpec(
                        label = "Enter",
                        type = KeyActionType.ENTER,
                        widthWeight = 1.68f,
                        isSpecial = true,
                        isAccent = true,
                        symbolicIcon = SymbolicIcon.ENTER
                    )
                )
            )
        }
    }

    fun buildNumericCalculatorRows(): List<List<KeySpec>> {
        return listOf(
            listOf(
                KeySpec("+", isSpecial = true),
                KeySpec("1"),
                KeySpec("2"),
                KeySpec("3"),
                KeySpec("/", isSpecial = true),
                KeySpec("%", isSpecial = true)
            ),
            listOf(
                KeySpec("-", isSpecial = true),
                KeySpec("4"),
                KeySpec("5"),
                KeySpec("6"),
                KeySpec("*", isSpecial = true),
                KeySpec("(", isSpecial = true)
            ),
            listOf(
                KeySpec(",", isSpecial = true),
                KeySpec("7"),
                KeySpec("8"),
                KeySpec("9"),
                KeySpec(")", isSpecial = true),
                KeySpec(
                    label = "Bksp",
                    type = KeyActionType.BACKSPACE,
                    isSpecial = true,
                    symbolicIcon = SymbolicIcon.BACKSPACE
                )
            ),
            listOf(
                KeySpec(label = "ABC", type = KeyActionType.MODE_ABC, isSpecial = true),
                KeySpec(label = "Space", outputText = " ", type = KeyActionType.SPACE),
                KeySpec(label = "0", widthWeight = 1.4f),
                KeySpec(label = "."),
                KeySpec(
                    label = "Enter",
                    type = KeyActionType.ENTER,
                    widthWeight = 1.68f,
                    isSpecial = true,
                    isAccent = true,
                    symbolicIcon = SymbolicIcon.ENTER
                )
            )
        )
    }

    fun buildPhoneRows(): List<List<KeySpec>> {
        return listOf(
            listOf(
                KeySpec("1", secondaryHint = ""),
                KeySpec("2", secondaryHint = "ABC"),
                KeySpec("3", secondaryHint = "DEF"),
                KeySpec("-", isSpecial = true)
            ),
            listOf(
                KeySpec("4", secondaryHint = "GHI"),
                KeySpec("5", secondaryHint = "JKL"),
                KeySpec("6", secondaryHint = "MNO"),
                KeySpec(label = "Space", outputText = " ", type = KeyActionType.SPACE, isSpecial = true)
            ),
            listOf(
                KeySpec("7", secondaryHint = "PQRS"),
                KeySpec("8", secondaryHint = "TUV"),
                KeySpec("9", secondaryHint = "WXYZ"),
                KeySpec(
                    label = "Bksp",
                    type = KeyActionType.BACKSPACE,
                    isSpecial = true,
                    symbolicIcon = SymbolicIcon.BACKSPACE
                )
            ),
            listOf(
                KeySpec("*", longPressAlternatives = listOf(",")),
                KeySpec("0", secondaryHint = "+", longPressAlternatives = listOf("+")),
                KeySpec("#", longPressAlternatives = listOf(";")),
                KeySpec(
                    label = "Enter",
                    type = KeyActionType.ENTER,
                    isSpecial = true,
                    isAccent = true,
                    symbolicIcon = SymbolicIcon.ENTER
                )
            )
        )
    }
}
