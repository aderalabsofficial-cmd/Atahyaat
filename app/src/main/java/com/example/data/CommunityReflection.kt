package com.example.data

data class ReflectionQuote(
    val id: Int,
    val arabicText: String,
    val translation: String,
    val source: String,
    val category: String, // "Prayer", "Patience", "Gratitude", "Dhikr"
    val reflectionPrompt: String
)

object ReflectionProvider {
    val DAILY_REFLECTIONS = listOf(
        ReflectionQuote(
            id = 1,
            arabicText = "وَأَقِيمُوا الصَّلَاةَ وَآتُوا الزَّكَاةَ وَارْكَعُوا مَعَ الرَّاكِعِينَ",
            translation = "And establish prayer and give zakah and bow with those who bow [in worship].",
            source = "Surah Al-Baqarah (2:43)",
            category = "Prayer",
            reflectionPrompt = "How can you make your prayer today a tranquil refuge from daily distractions?"
        ),
        ReflectionQuote(
            id = 2,
            arabicText = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
            translation = "Indeed, prayer has been decreed upon the believers a decree of specified times.",
            source = "Surah An-Nisa (4:103)",
            category = "Punctuality",
            reflectionPrompt = "Plan your routine around prayer times today rather than fitting prayer into spare moments."
        ),
        ReflectionQuote(
            id = 3,
            arabicText = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            translation = "Unquestionably, by the remembrance of Allah do hearts find rest.",
            source = "Surah Ar-Ra'd (13:28)",
            category = "Dhikr",
            reflectionPrompt = "Pause for 2 minutes after prayer to recite Subhanallah, Alhamdulillah, and Allahu Akbar."
        ),
        ReflectionQuote(
            id = 4,
            arabicText = "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ",
            translation = "And seek help through patience and prayer.",
            source = "Surah Al-Baqarah (2:45)",
            category = "Patience",
            reflectionPrompt = "Turn your worries into supplications during prostration (Sujood)."
        ),
        ReflectionQuote(
            id = 5,
            arabicText = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
            translation = "My Lord, make me an establisher of prayer, and from my descendants. Our Lord, and accept my supplication.",
            source = "Surah Ibrahim (14:40)",
            category = "Supplication",
            reflectionPrompt = "Recite this dua after finishing your daily worship."
        )
    )

    fun getTodayQuote(): ReflectionQuote {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return DAILY_REFLECTIONS[dayOfYear % DAILY_REFLECTIONS.size]
    }
}
