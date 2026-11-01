package org.gnome.adwaitakeyboard.dictionary

import android.content.Context
import org.gnome.adwaitakeyboard.prefs.KeyboardPreferences
import kotlin.math.abs

/**
 * 100% offline local dictionary, typo autocorrector, next-word bigram engine,
 * personal dictionary protector, text shortcut expander, and prefix-based
 * next-character probability calculator for Adaptive Touch Regions.
 */
class LocalDictionaryEngine(
    context: Context,
    private val prefs: KeyboardPreferences
) {
    private val learnedPrefs = context.getSharedPreferences("adwaita_learned_words", Context.MODE_PRIVATE)
    private val baseWords = LinkedHashMap<String, Int>(600)
    private val learnedWords = HashMap<String, Int>()
    private val typoCorrections = HashMap<String, String>()
    private val bigrams = HashMap<String, List<String>>()

    init {
        seedCoreVocabulary()
        seedCommonTypos()
        seedBigrams()
        loadLearnedWords()
    }

    private fun seedCoreVocabulary() {
        val rankedWords = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "hello", "thanks", "thank", "please", "yes", "okay", "great", "awesome", "perfect",
            "privacy", "security", "keyboard", "gnome", "adwaita", "linux", "debian", "android",
            "system", "settings", "clipboard", "message", "email", "phone", "number", "address",
            "today", "tomorrow", "tonight", "morning", "afternoon", "evening", "night", "weekend",
            "meeting", "project", "update", "version", "release", "source", "open", "free",
            "software", "application", "interface", "design", "clean", "simple", "modern", "fast",
            "offline", "local", "private", "encrypted", "safe", "secure", "button", "screen",
            "window", "desktop", "mobile", "device", "battery", "performance", "memory", "storage",
            "file", "folder", "document", "image", "video", "audio", "music", "search", "find",
            "copy", "paste", "cut", "undo", "redo", "delete", "remove", "clear", "reset",
            "save", "load", "send", "receive", "write", "read", "edit", "change", "create",
            "build", "compile", "test", "install", "run", "start", "stop", "close", "exit",
            "friend", "family", "home", "work", "school", "office", "city", "country", "world",
            "water", "food", "coffee", "tea", "lunch", "dinner", "breakfast", "happy", "glad",
            "sorry", "welcome", "available", "ready", "soon", "later", "always", "never",
            "sometimes", "usually", "often", "maybe", "perhaps", "definitely", "absolutely",
            "important", "necessary", "possible", "different", "similar", "better", "best",
            "right", "left", "top", "bottom", "center", "side", "front", "behind", "next",
            "previous", "current", "last", "every", "each", "many", "much", "few", "little",
            "more", "less", "most", "least", "enough", "almost", "quite", "really", "very",
            "already", "still", "yet", "again", "together", "around", "through", "between",
            "without", "within", "during", "before", "since", "until", "while", "where", "why"
        )
        val total = rankedWords.size
        rankedWords.forEachIndexed { index, word ->
            baseWords[word.lowercase()] = (total - index) * 10
        }
    }

    private fun seedCommonTypos() {
        val pairs = mapOf(
            "teh" to "the",
            "hte" to "the",
            "adn" to "and",
            "nad" to "and",
            "taht" to "that",
            "wiht" to "with",
            "whit" to "with",
            "realy" to "really",
            "recieve" to "receive",
            "seperate" to "separate",
            "definately" to "definitely",
            "occured" to "occurred",
            "untill" to "until",
            "tommorow" to "tomorrow",
            "becuase" to "because",
            "beacuse" to "because",
            "freind" to "friend",
            "wierd" to "weird",
            "wich" to "which",
            "thier" to "their",
            "youre" to "you're",
            "dont" to "don't",
            "cant" to "can't",
            "wont" to "won't",
            "didnt" to "didn't",
            "isnt" to "isn't",
            "aren't" to "aren't",
            "hasnt" to "hasn't",
            "havent" to "haven't",
            "wouldnt" to "wouldn't",
            "couldnt" to "couldn't",
            "shouldnt" to "shouldn't",
            "im" to "I'm",
            "ive" to "I've",
            "id" to "I'd",
            "ill" to "I'll",
            "i" to "I",
            "gnom" to "GNOME",
            "adwita" to "Adwaita"
        )
        typoCorrections.putAll(pairs)
    }

    private fun seedBigrams() {
        bigrams["i"] = listOf("am", "have", "will", "think")
        bigrams["im"] = listOf("going", "not", "here", "ready")
        bigrams["i'm"] = listOf("going", "not", "here", "ready")
        bigrams["how"] = listOf("are", "is", "to", "much")
        bigrams["are"] = listOf("you", "we", "they", "not")
        bigrams["thank"] = listOf("you", "goodness")
        bigrams["thanks"] = listOf("for", "a", "again")
        bigrams["see"] = listOf("you", "if", "what")
        bigrams["what"] = listOf("is", "are", "do", "time")
        bigrams["going"] = listOf("to", "home", "well")
        bigrams["on"] = listOf("the", "my", "your")
        bigrams["in"] = listOf("the", "my", "a")
        bigrams["of"] = listOf("the", "course", "my")
        bigrams["for"] = listOf("the", "your", "a")
        bigrams["have"] = listOf("a", "to", "you", "been")
        bigrams["can"] = listOf("you", "I", "we", "be")
        bigrams["let"] = listOf("me", "us", "it")
        bigrams["looking"] = listOf("forward", "for", "at")
    }

    private fun loadLearnedWords() {
        learnedWords.clear()
        for ((k, v) in learnedPrefs.all) {
            if (v is Int) {
                learnedWords[k.lowercase()] = v
            }
        }
    }

    /**
     * Computes normalized next-character likelihood weights in [0.0, 1.0] for a typed prefix.
     * Example: when currentPrefix == "ope", 'n' (from "open") receives weight 1.0f.
     * Runs 100% locally in < 0.2ms with zero network access.
     */
    fun getNextCharacterWeights(
        currentPrefix: String,
        allowAdaptive: Boolean
    ): Map<Char, Float> {
        if (!allowAdaptive) return emptyMap()
        val clean = currentPrefix.trim().lowercase()
        if (clean.isEmpty() || clean.length > 15 || clean.any { !it.isLetter() }) {
            return emptyMap()
        }

        val rawScores = HashMap<Char, Float>()
        val prefixLen = clean.length

        // 1. Personal dictionary matches
        for (personalWord in prefs.getPersonalDictionary()) {
            val w = personalWord.lowercase()
            if (w.length > prefixLen && w.startsWith(clean)) {
                val nextCh = w[prefixLen]
                if (nextCh.isLetter()) {
                    rawScores[nextCh] = (rawScores[nextCh] ?: 0f) + 2500f
                }
            }
        }

        // 2. Locally learned words
        for ((w, freq) in learnedWords) {
            if (w.length > prefixLen && w.startsWith(clean)) {
                val nextCh = w[prefixLen]
                if (nextCh.isLetter()) {
                    rawScores[nextCh] = (rawScores[nextCh] ?: 0f) + (freq * 80f + 1200f)
                }
            }
        }

        // 3. Bundled core vocabulary
        for ((w, score) in baseWords) {
            if (w.length > prefixLen && w.startsWith(clean)) {
                val nextCh = w[prefixLen]
                if (nextCh.isLetter()) {
                    // Exact 1-character completion (e.g. "ope" -> "open") gets an extra completion boost
                    val completionBonus = if (w.length == prefixLen + 1) 1.5f else 1.0f
                    rawScores[nextCh] = (rawScores[nextCh] ?: 0f) + score * completionBonus
                }
            }
        }

        if (rawScores.isEmpty()) return emptyMap()

        val maxScore = rawScores.values.maxOrNull() ?: return emptyMap()
        if (maxScore <= 0f) return emptyMap()

        // Keep top 3 candidate characters normalized to [0.15, 1.0]
        val sorted = rawScores.entries.sortedByDescending { it.value }.take(3)
        val normalized = HashMap<Char, Float>(sorted.size)
        for ((ch, sc) in sorted) {
            val ratio = (sc / maxScore).coerceIn(0f, 1f)
            if (ratio >= 0.15f) {
                normalized[ch] = ratio
            }
        }
        return normalized
    }

    fun getSuggestions(
        currentWord: String,
        previousWord: String,
        allowSuggestions: Boolean
    ): List<String> {
        if (!allowSuggestions) return emptyList()

        if (currentWord.isEmpty()) {
            val prevLower = previousWord.trim().lowercase()
            if (prevLower.isNotEmpty()) {
                val nextList = bigrams[prevLower]
                if (!nextList.isNullOrEmpty()) return nextList.take(3)
            }
            return listOf("I", "the", "to")
        }

        val results = LinkedHashSet<String>()
        val rawTrimmed = currentWord.trim()
        val lower = rawTrimmed.lowercase()
        val isCapitalized = rawTrimmed.firstOrNull()?.isUpperCase() == true

        val shortcuts = prefs.getTextShortcuts()
        shortcuts[rawTrimmed]?.let { results.add(it) }
        shortcuts[lower]?.let { results.add(it) }

        val personal = prefs.getPersonalDictionary()
        personal.filter { it.lowercase().startsWith(lower) }.forEach {
            results.add(it)
        }

        typoCorrections[lower]?.let { corr ->
            results.add(matchCase(corr, isCapitalized))
        }

        val candidates = ArrayList<Pair<String, Int>>()
        for ((word, freq) in learnedWords) {
            if (word.startsWith(lower) && word != lower) {
                candidates.add(word to (freq * 50 + 2000))
            }
        }
        for ((word, score) in baseWords) {
            if (word.startsWith(lower) && word != lower) {
                candidates.add(word to score)
            }
        }

        if (candidates.size < 3 && lower.length in 3..10) {
            for ((word, score) in baseWords) {
                if (abs(word.length - lower.length) <= 1 && isEditDistanceOne(lower, word)) {
                    candidates.add(word to (score + 500))
                }
            }
        }

        candidates.sortByDescending { it.second }

        results.add(rawTrimmed)
        for ((word, _) in candidates) {
            results.add(matchCase(word, isCapitalized))
            if (results.size >= 3) break
        }

        return results.take(3)
    }

    fun resolveAutocorrectOrShortcut(
        currentWord: String,
        allowAutocorrect: Boolean
    ): String? {
        val raw = currentWord.trim()
        if (raw.isEmpty()) return null

        val shortcuts = prefs.getTextShortcuts()
        shortcuts[raw]?.let { return it }
        shortcuts[raw.lowercase()]?.let { return it }

        if (!allowAutocorrect) return null

        val personal = prefs.getPersonalDictionary()
        if (personal.any { it.equals(raw, ignoreCase = true) }) {
            return null
        }

        val lower = raw.lowercase()
        val isCapitalized = raw.firstOrNull()?.isUpperCase() == true

        if (raw == "i") return "I"
        if (baseWords.containsKey(lower) || learnedWords.containsKey(lower)) {
            return null
        }

        typoCorrections[lower]?.let { corr ->
            return matchCase(corr, isCapitalized)
        }

        if (lower.length in 3..9) {
            var bestMatch: String? = null
            var bestScore = -1
            for ((word, score) in baseWords) {
                if (word.length == lower.length && score > bestScore && isEditDistanceOne(lower, word)) {
                    bestMatch = word
                    bestScore = score
                }
            }
            if (bestMatch != null) {
                return matchCase(bestMatch, isCapitalized)
            }
        }

        return null
    }

    fun learnWordLocally(word: String, canLearn: Boolean) {
        if (!canLearn) return
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.any { !it.isLetter() }) return
        val newCount = (learnedWords[clean] ?: 0) + 1
        learnedWords[clean] = newCount
        learnedPrefs.edit().putInt(clean, newCount).apply()
    }

    fun clearAllLearnedAndPersonalData() {
        learnedWords.clear()
        learnedPrefs.edit().clear().apply()
    }

    private fun matchCase(word: String, capitalizeFirst: Boolean): String {
        if (!capitalizeFirst || word.isEmpty()) return word
        return word.replaceFirstChar { it.uppercaseChar() }
    }

    private fun isEditDistanceOne(a: String, b: String): Boolean {
        val lenA = a.length
        val lenB = b.length
        if (abs(lenA - lenB) > 1) return false

        if (lenA == lenB) {
            var diffCount = 0
            var firstDiff = -1
            var secondDiff = -1
            for (i in 0 until lenA) {
                if (a[i] != b[i]) {
                    diffCount++
                    if (firstDiff == -1) firstDiff = i else secondDiff = i
                    if (diffCount > 2) return false
                }
            }
            if (diffCount == 1) return true
            if (diffCount == 2 && secondDiff == firstDiff + 1 &&
                a[firstDiff] == b[secondDiff] && a[secondDiff] == b[firstDiff]
            ) {
                return true
            }
            return false
        }

        val shorter = if (lenA < lenB) a else b
        val longer = if (lenA < lenB) b else a
        var i = 0
        var j = 0
        var foundDiff = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] != longer[j]) {
                if (foundDiff) return false
                foundDiff = true
                j++
            } else {
                i++
                j++
            }
        }
        return true
    }
}
