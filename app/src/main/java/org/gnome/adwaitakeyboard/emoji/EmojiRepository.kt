package org.gnome.adwaitakeyboard.emoji

enum class EmojiCategory(val id: String, val title: String, val icon: String) {
    RECENT("recent", "Frequently & Recently Used", "🕒"),
    SMILEYS("smileys", "Smileys & Emotions", "😀"),
    PEOPLE("people", "People & Body", "👋"),
    ANIMALS("animals", "Animals & Nature", "🦊"),
    FOOD("food", "Food & Drink", "🍎"),
    TRAVEL("travel", "Travel & Places", "🚀"),
    ACTIVITIES("activities", "Activities", "⚽"),
    OBJECTS("objects", "Objects", "💡"),
    SYMBOLS("symbols", "Symbols", "❤️"),
    FLAGS("flags", "Flags", "🏳️")
}

data class EmojiItem(
    val emoji: String,
    val name: String,
    val keywords: List<String>,
    val category: EmojiCategory,
    val supportsSkinTone: Boolean = false
)

object EmojiRepository {

    val skinTones: List<Pair<String, String>> = listOf(
        "" to "Default",
        "\uD83C\uDFFB" to "Light",
        "\uD83C\uDFFC" to "Medium-Light",
        "\uD83C\uDFFD" to "Medium",
        "\uD83C\uDFFE" to "Medium-Dark",
        "\uD83C\uDFFF" to "Dark"
    )

    val allEmojis: List<EmojiItem> = listOf(
        // Smileys & Emotions
        EmojiItem("😀", "grinning face", listOf("smile", "happy", "joy"), EmojiCategory.SMILEYS),
        EmojiItem("😃", "grinning face with big eyes", listOf("happy", "smile"), EmojiCategory.SMILEYS),
        EmojiItem("😄", "grinning face with smiling eyes", listOf("happy", "laugh"), EmojiCategory.SMILEYS),
        EmojiItem("😁", "beaming face", listOf("grin", "smile"), EmojiCategory.SMILEYS),
        EmojiItem("😆", "grinning squinting face", listOf("laugh", "haha"), EmojiCategory.SMILEYS),
        EmojiItem("😅", "grinning face with sweat", listOf("relief", "phew"), EmojiCategory.SMILEYS),
        EmojiItem("🤣", "rolling on the floor laughing", listOf("rofl", "lol", "laugh"), EmojiCategory.SMILEYS),
        EmojiItem("😂", "face with tears of joy", listOf("lol", "laugh", "cry"), EmojiCategory.SMILEYS),
        EmojiItem("🙂", "slightly smiling face", listOf("smile", "fine"), EmojiCategory.SMILEYS),
        EmojiItem("🙃", "upside-down face", listOf("silly", "irony"), EmojiCategory.SMILEYS),
        EmojiItem("😉", "winking face", listOf("wink", "flirt"), EmojiCategory.SMILEYS),
        EmojiItem("😊", "smiling face with smiling eyes", listOf("blush", "warm", "happy"), EmojiCategory.SMILEYS),
        EmojiItem("😇", "smiling face with halo", listOf("angel", "innocent"), EmojiCategory.SMILEYS),
        EmojiItem("🥰", "smiling face with hearts", listOf("love", "adore"), EmojiCategory.SMILEYS),
        EmojiItem("😍", "smiling face with heart-eyes", listOf("love", "crush"), EmojiCategory.SMILEYS),
        EmojiItem("🤩", "star-struck", listOf("excited", "wow", "star"), EmojiCategory.SMILEYS),
        EmojiItem("😘", "face blowing a kiss", listOf("kiss", "love"), EmojiCategory.SMILEYS),
        EmojiItem("😋", "face savoring food", listOf("yum", "delicious"), EmojiCategory.SMILEYS),
        EmojiItem("😛", "face with tongue", listOf("playful", "tongue"), EmojiCategory.SMILEYS),
        EmojiItem("😜", "winking face with tongue", listOf("joke", "crazy"), EmojiCategory.SMILEYS),
        EmojiItem("🤪", "zany face", listOf("goofy", "wild"), EmojiCategory.SMILEYS),
        EmojiItem("🤗", "smiling face with open hands", listOf("hug", "warm"), EmojiCategory.SMILEYS),
        EmojiItem("🤔", "thinking face", listOf("hmm", "think", "question"), EmojiCategory.SMILEYS),
        EmojiItem("🫡", "saluting face", listOf("respect", "salute", "yes"), EmojiCategory.SMILEYS),
        EmojiItem("🤫", "shushing face", listOf("quiet", "secret"), EmojiCategory.SMILEYS),
        EmojiItem("😐", "neutral face", listOf("meh", "blank"), EmojiCategory.SMILEYS),
        EmojiItem("😑", "expressionless face", listOf("deadpan", "annoyed"), EmojiCategory.SMILEYS),
        EmojiItem("🙄", "face with rolling eyes", listOf("eyeroll", "whatever"), EmojiCategory.SMILEYS),
        EmojiItem("😌", "relieved face", listOf("calm", "peace"), EmojiCategory.SMILEYS),
        EmojiItem("😔", "pensive face", listOf("sad", "reflective"), EmojiCategory.SMILEYS),
        EmojiItem("😴", "sleeping face", listOf("sleep", "tired", "zzz"), EmojiCategory.SMILEYS),
        EmojiItem("🥳", "partying face", listOf("party", "celebrate", "birthday"), EmojiCategory.SMILEYS),
        EmojiItem("😎", "smiling face with sunglasses", listOf("cool", "chill"), EmojiCategory.SMILEYS),
        EmojiItem("🤓", "nerd face", listOf("geek", "glasses", "smart"), EmojiCategory.SMILEYS),
        EmojiItem("🧐", "face with monocle", listOf("inspect", "curious"), EmojiCategory.SMILEYS),
        EmojiItem("😕", "confused face", listOf("unsure", "puzzled"), EmojiCategory.SMILEYS),
        EmojiItem("😟", "worried face", listOf("anxious", "concern"), EmojiCategory.SMILEYS),
        EmojiItem("😮", "face with open mouth", listOf("surprise", "oh"), EmojiCategory.SMILEYS),
        EmojiItem("😳", "flushed face", listOf("shock", "blush"), EmojiCategory.SMILEYS),
        EmojiItem("🥺", "pleading face", listOf("please", "puppy"), EmojiCategory.SMILEYS),
        EmojiItem("😢", "crying face", listOf("sad", "tear"), EmojiCategory.SMILEYS),
        EmojiItem("😭", "loudly crying face", listOf("sob", "tears"), EmojiCategory.SMILEYS),
        EmojiItem("😱", "face screaming in fear", listOf("scream", "shock"), EmojiCategory.SMILEYS),
        EmojiItem("😤", "face with steam from nose", listOf("frustrated", "proud"), EmojiCategory.SMILEYS),
        EmojiItem("😡", "enraged face", listOf("angry", "mad"), EmojiCategory.SMILEYS),
        EmojiItem("🤯", "exploding head", listOf("mindblown", "shock"), EmojiCategory.SMILEYS),
        EmojiItem("🤖", "robot", listOf("bot", "ai", "android"), EmojiCategory.SMILEYS),
        EmojiItem("👻", "ghost", listOf("spooky", "halloween"), EmojiCategory.SMILEYS),

        // People & Body (with skin tone modifier support)
        EmojiItem("👋", "waving hand", listOf("hello", "hi", "bye", "wave"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤚", "raised back of hand", listOf("stop", "hand"), EmojiCategory.PEOPLE, true),
        EmojiItem("✋", "raised hand", listOf("highfive", "stop"), EmojiCategory.PEOPLE, true),
        EmojiItem("🖖", "vulcan salute", listOf("spock", "prosper"), EmojiCategory.PEOPLE, true),
        EmojiItem("👌", "OK hand", listOf("okay", "perfect", "good"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤌", "pinched fingers", listOf("italian", "chefs kiss"), EmojiCategory.PEOPLE, true),
        EmojiItem("✌️", "victory hand", listOf("peace", "two"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤞", "crossed fingers", listOf("luck", "hope"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤟", "love-you gesture", listOf("love", "rock"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤘", "sign of the horns", listOf("metal", "rock"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤙", "call me hand", listOf("shaka", "call"), EmojiCategory.PEOPLE, true),
        EmojiItem("👍", "thumbs up", listOf("like", "yes", "approve", "good"), EmojiCategory.PEOPLE, true),
        EmojiItem("👎", "thumbs down", listOf("dislike", "no", "bad"), EmojiCategory.PEOPLE, true),
        EmojiItem("✊", "raised fist", listOf("solidarity", "power"), EmojiCategory.PEOPLE, true),
        EmojiItem("👊", "oncoming fist", listOf("fistbump", "punch"), EmojiCategory.PEOPLE, true),
        EmojiItem("👏", "clapping hands", listOf("applause", "bravo", "clap"), EmojiCategory.PEOPLE, true),
        EmojiItem("🙌", "raising hands", listOf("hooray", "celebrate"), EmojiCategory.PEOPLE, true),
        EmojiItem("👐", "open hands", listOf("hug", "open"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤲", "palms up together", listOf("prayer", "offer"), EmojiCategory.PEOPLE, true),
        EmojiItem("🤝", "handshake", listOf("deal", "agree", "meet"), EmojiCategory.PEOPLE, false),
        EmojiItem("🙏", "folded hands", listOf("thank", "please", "pray"), EmojiCategory.PEOPLE, true),
        EmojiItem("✍️", "writing hand", listOf("write", "note"), EmojiCategory.PEOPLE, true),
        EmojiItem("💪", "flexed biceps", listOf("strong", "muscle", "workout"), EmojiCategory.PEOPLE, true),
        EmojiItem("🧠", "brain", listOf("smart", "mind", "think"), EmojiCategory.PEOPLE, false),
        EmojiItem("👀", "eyes", listOf("look", "watch", "see"), EmojiCategory.PEOPLE, false),

        // Animals & Nature
        EmojiItem("🐧", "penguin", listOf("linux", "tux", "bird", "cold"), EmojiCategory.ANIMALS),
        EmojiItem("🦊", "fox", listOf("clever", "animal"), EmojiCategory.ANIMALS),
        EmojiItem("🐱", "cat face", listOf("kitty", "pet", "meow"), EmojiCategory.ANIMALS),
        EmojiItem("🐶", "dog face", listOf("puppy", "pet", "woof"), EmojiCategory.ANIMALS),
        EmojiItem("🐻", "bear", listOf("animal", "wild"), EmojiCategory.ANIMALS),
        EmojiItem("🐼", "panda", listOf("bamboo", "cute"), EmojiCategory.ANIMALS),
        EmojiItem("🦁", "lion", listOf("king", "safari"), EmojiCategory.ANIMALS),
        EmojiItem("🐯", "tiger", listOf("big cat", "stripes"), EmojiCategory.ANIMALS),
        EmojiItem("🐨", "koala", listOf("australia", "cute"), EmojiCategory.ANIMALS),
        EmojiItem("🐸", "frog", listOf("toad", "green"), EmojiCategory.ANIMALS),
        EmojiItem("🦉", "owl", listOf("wise", "night", "bird"), EmojiCategory.ANIMALS),
        EmojiItem("🦅", "eagle", listOf("bird", "fly"), EmojiCategory.ANIMALS),
        EmojiItem("🐢", "turtle", listOf("slow", "shell"), EmojiCategory.ANIMALS),
        EmojiItem("🐙", "octopus", listOf("sea", "tentacles"), EmojiCategory.ANIMALS),
        EmojiItem("🐬", "dolphin", listOf("ocean", "swim"), EmojiCategory.ANIMALS),
        EmojiItem("🐝", "honeybee", listOf("bee", "buzz", "honey"), EmojiCategory.ANIMALS),
        EmojiItem("🦋", "butterfly", listOf("insect", "pretty"), EmojiCategory.ANIMALS),
        EmojiItem("🌲", "evergreen tree", listOf("forest", "pine", "nature"), EmojiCategory.ANIMALS),
        EmojiItem("🌸", "cherry blossom", listOf("flower", "spring"), EmojiCategory.ANIMALS),
        EmojiItem("🌻", "sunflower", listOf("sun", "flower"), EmojiCategory.ANIMALS),
        EmojiItem("🌍", "globe showing Europe-Africa", listOf("earth", "world", "planet"), EmojiCategory.ANIMALS),
        EmojiItem("☀️", "sun", listOf("sunny", "weather", "warm"), EmojiCategory.ANIMALS),
        EmojiItem("🌙", "crescent moon", listOf("night", "moon", "dark"), EmojiCategory.ANIMALS),
        EmojiItem("⭐", "star", listOf("favorite", "night"), EmojiCategory.ANIMALS),
        EmojiItem("⚡", "high voltage", listOf("lightning", "fast", "power"), EmojiCategory.ANIMALS),
        EmojiItem("🔥", "fire", listOf("hot", "lit", "flame"), EmojiCategory.ANIMALS),

        // Food & Drink
        EmojiItem("🍎", "red apple", listOf("fruit", "healthy"), EmojiCategory.FOOD),
        EmojiItem("🍌", "banana", listOf("fruit", "potassium"), EmojiCategory.FOOD),
        EmojiItem("🍇", "grapes", listOf("fruit", "wine"), EmojiCategory.FOOD),
        EmojiItem("🍓", "strawberry", listOf("berry", "sweet"), EmojiCategory.FOOD),
        EmojiItem("🥑", "avocado", listOf("guacamole", "toast"), EmojiCategory.FOOD),
        EmojiItem("🍕", "pizza", listOf("slice", "italian", "dinner"), EmojiCategory.FOOD),
        EmojiItem("🍔", "hamburger", listOf("burger", "fast food"), EmojiCategory.FOOD),
        EmojiItem("🍟", "french fries", listOf("chips", "snack"), EmojiCategory.FOOD),
        EmojiItem("🌮", "taco", listOf("mexican", "food"), EmojiCategory.FOOD),
        EmojiItem("🍜", "steaming bowl", listOf("ramen", "noodles", "soup"), EmojiCategory.FOOD),
        EmojiItem("🍣", "sushi", listOf("japanese", "fish"), EmojiCategory.FOOD),
        EmojiItem("🥐", "croissant", listOf("bakery", "breakfast"), EmojiCategory.FOOD),
        EmojiItem("🍞", "bread", listOf("loaf", "toast"), EmojiCategory.FOOD),
        EmojiItem("🧀", "cheese wedge", listOf("dairy", "cheddar"), EmojiCategory.FOOD),
        EmojiItem("🥗", "green salad", listOf("healthy", "lunch"), EmojiCategory.FOOD),
        EmojiItem("🍰", "shortcake", listOf("cake", "dessert", "sweet"), EmojiCategory.FOOD),
        EmojiItem("🍫", "chocolate bar", listOf("candy", "sweet"), EmojiCategory.FOOD),
        EmojiItem("🍪", "cookie", listOf("biscuit", "snack"), EmojiCategory.FOOD),
        EmojiItem("☕", "hot beverage", listOf("coffee", "espresso", "tea", "morning"), EmojiCategory.FOOD),
        EmojiItem("🍵", "teacup without handle", listOf("green tea", "matcha"), EmojiCategory.FOOD),
        EmojiItem("🥤", "cup with straw", listOf("drink", "soda"), EmojiCategory.FOOD),
        EmojiItem("🍺", "beer mug", listOf("cheers", "drink", "pub"), EmojiCategory.FOOD),

        // Travel & Places
        EmojiItem("🚀", "rocket", listOf("launch", "space", "fast"), EmojiCategory.TRAVEL),
        EmojiItem("✈️", "airplane", listOf("flight", "travel", "trip"), EmojiCategory.TRAVEL),
        EmojiItem("🚆", "train", listOf("rail", "commute"), EmojiCategory.TRAVEL),
        EmojiItem("🚗", "automobile", listOf("car", "drive"), EmojiCategory.TRAVEL),
        EmojiItem("🚲", "bicycle", listOf("bike", "cycle"), EmojiCategory.TRAVEL),
        EmojiItem("🏠", "house", listOf("home", "building"), EmojiCategory.TRAVEL),
        EmojiItem("🏢", "office building", listOf("work", "city"), EmojiCategory.TRAVEL),
        EmojiItem("🏔️", "snow-capped mountain", listOf("alps", "hike", "peak"), EmojiCategory.TRAVEL),
        EmojiItem("🏖️", "beach with umbrella", listOf("vacation", "sea", "sand"), EmojiCategory.TRAVEL),
        EmojiItem("🗺️", "world map", listOf("map", "navigation"), EmojiCategory.TRAVEL),
        EmojiItem("🧭", "compass", listOf("direction", "explore"), EmojiCategory.TRAVEL),

        // Activities
        EmojiItem("⚽", "soccer ball", listOf("football", "sport"), EmojiCategory.ACTIVITIES),
        EmojiItem("🏀", "basketball", listOf("hoops", "sport"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎾", "tennis", listOf("racket", "sport"), EmojiCategory.ACTIVITIES),
        EmojiItem("🏆", "trophy", listOf("winner", "champion", "award"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎯", "bullseye", listOf("target", "goal", "accurate"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎮", "video game", listOf("controller", "gaming", "play"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎲", "game die", listOf("dice", "boardgame", "random"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎨", "artist palette", listOf("art", "design", "paint", "gnome"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎵", "musical note", listOf("music", "song"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎧", "headphone", listOf("audio", "listen", "podcast"), EmojiCategory.ACTIVITIES),
        EmojiItem("🎉", "party popper", listOf("tada", "celebrate", "congrats"), EmojiCategory.ACTIVITIES),
        EmojiItem("✨", "sparkles", listOf("magic", "clean", "new", "shine"), EmojiCategory.ACTIVITIES),

        // Objects
        EmojiItem("💻", "laptop", listOf("computer", "code", "linux", "work"), EmojiCategory.OBJECTS),
        EmojiItem("⌨️", "keyboard", listOf("type", "input", "adwaita"), EmojiCategory.OBJECTS),
        EmojiItem("📱", "mobile phone", listOf("android", "smartphone"), EmojiCategory.OBJECTS),
        EmojiItem("🔋", "battery", listOf("power", "charge"), EmojiCategory.OBJECTS),
        EmojiItem("💡", "light bulb", listOf("idea", "bright", "tip"), EmojiCategory.OBJECTS),
        EmojiItem("🔒", "locked", listOf("security", "privacy", "encrypted", "safe"), EmojiCategory.OBJECTS),
        EmojiItem("🛡️", "shield", listOf("privacy", "protect", "guard"), EmojiCategory.OBJECTS),
        EmojiItem("🔑", "key", listOf("password", "access", "auth"), EmojiCategory.OBJECTS),
        EmojiItem("📎", "paperclip", listOf("attachment", "office"), EmojiCategory.OBJECTS),
        EmojiItem("📋", "clipboard", listOf("paste", "copy", "notes"), EmojiCategory.OBJECTS),
        EmojiItem("📁", "file folder", listOf("files", "nautilus", "directory"), EmojiCategory.OBJECTS),
        EmojiItem("📚", "books", listOf("read", "study", "dictionary"), EmojiCategory.OBJECTS),
        EmojiItem("⚙️", "gear", listOf("settings", "preferences", "config"), EmojiCategory.OBJECTS),
        EmojiItem("🔧", "wrench", listOf("tool", "fix", "build"), EmojiCategory.OBJECTS),

        // Symbols
        EmojiItem("❤️", "red heart", listOf("love", "like", "heart"), EmojiCategory.SYMBOLS),
        EmojiItem("💙", "blue heart", listOf("adwaita", "gnome", "love"), EmojiCategory.SYMBOLS),
        EmojiItem("💚", "green heart", listOf("love", "nature"), EmojiCategory.SYMBOLS),
        EmojiItem("💛", "yellow heart", listOf("love", "warm"), EmojiCategory.SYMBOLS),
        EmojiItem("💜", "purple heart", listOf("love"), EmojiCategory.SYMBOLS),
        EmojiItem("🖤", "black heart", listOf("dark", "love"), EmojiCategory.SYMBOLS),
        EmojiItem("💯", "hundred points", listOf("100", "perfect", "score"), EmojiCategory.SYMBOLS),
        EmojiItem("✅", "check mark button", listOf("done", "yes", "complete", "success"), EmojiCategory.SYMBOLS),
        EmojiItem("❌", "cross mark", listOf("no", "cancel", "error", "delete"), EmojiCategory.SYMBOLS),
        EmojiItem("⚠️", "warning", listOf("alert", "caution"), EmojiCategory.SYMBOLS),
        EmojiItem("ℹ️", "information", listOf("info", "details"), EmojiCategory.SYMBOLS),
        EmojiItem("♻️", "recycling symbol", listOf("environment", "reuse"), EmojiCategory.SYMBOLS),
        EmojiItem("➕", "plus", listOf("add", "math"), EmojiCategory.SYMBOLS),
        EmojiItem("➖", "minus", listOf("subtract", "math"), EmojiCategory.SYMBOLS),
        EmojiItem("➡️", "right arrow", listOf("next", "direction"), EmojiCategory.SYMBOLS),
        EmojiItem("⬅️", "left arrow", listOf("back", "previous"), EmojiCategory.SYMBOLS),

        // Flags
        EmojiItem("🏳️", "white flag", listOf("flag", "peace"), EmojiCategory.FLAGS),
        EmojiItem("🏴", "black flag", listOf("flag", "pirate"), EmojiCategory.FLAGS),
        EmojiItem("🏁", "chequered flag", listOf("finish", "race"), EmojiCategory.FLAGS),
        EmojiItem("🏳️‍🌈", "rainbow flag", listOf("pride", "lgbtq"), EmojiCategory.FLAGS),
        EmojiItem("🇪🇺", "flag: European Union", listOf("eu", "europe"), EmojiCategory.FLAGS),
        EmojiItem("🇺🇸", "flag: United States", listOf("us", "usa"), EmojiCategory.FLAGS),
        EmojiItem("🇬🇧", "flag: United Kingdom", listOf("uk", "britain"), EmojiCategory.FLAGS),
        EmojiItem("🇩🇪", "flag: Germany", listOf("de", "deutschland"), EmojiCategory.FLAGS),
        EmojiItem("🇫🇷", "flag: France", listOf("fr", "france"), EmojiCategory.FLAGS),
        EmojiItem("🇮🇳", "flag: India", listOf("in", "india"), EmojiCategory.FLAGS),
        EmojiItem("🇯🇵", "flag: Japan", listOf("jp", "japan"), EmojiCategory.FLAGS),
        EmojiItem("🇨🇦", "flag: Canada", listOf("ca", "canada"), EmojiCategory.FLAGS),
        EmojiItem("🇧🇷", "flag: Brazil", listOf("br", "brasil"), EmojiCategory.FLAGS)
    )

    fun applySkinTone(item: EmojiItem, modifier: String): String {
        if (!item.supportsSkinTone || modifier.isEmpty()) return item.emoji
        // Strip variation selector FE0F when appending Fitzpatrick modifier
        val base = item.emoji.replace("\uFE0F", "")
        return base + modifier
    }

    fun getByCategory(category: EmojiCategory, modifier: String): List<String> {
        return allEmojis
            .filter { it.category == category }
            .map { applySkinTone(it, modifier) }
    }

    fun search(query: String, modifier: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allEmojis.map { applySkinTone(it, modifier) }
        return allEmojis.filter { item ->
            item.name.contains(q) || item.keywords.any { it.contains(q) } || item.emoji.contains(q)
        }.map { applySkinTone(it, modifier) }
    }
}
