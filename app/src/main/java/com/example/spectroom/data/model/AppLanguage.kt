package com.example.spectroom.data.model

data class AppLanguage(
    val code: String,
    val name: String,
    val flag: String,
) {
    companion object {
        val English = AppLanguage("en", "English", "🇺🇸")
        val Spanish = AppLanguage("es", "Spanish", "🇪🇸")
        val Swedish = AppLanguage("sv", "Swedish", "🇸🇪")
        val French = AppLanguage("fr", "French", "🇫🇷")
        val German = AppLanguage("de", "German", "🇩🇪")
        val Danish = AppLanguage("da", "Danish", "🇩🇰")
        val Norwegian = AppLanguage("nb", "Norwegian", "🇳🇴")
        val Finnish = AppLanguage("fi", "Finnish", "🇫🇮")
        val Icelandic = AppLanguage("is", "Icelandic", "🇮🇸")

        val all = listOf(
            English, Spanish, Swedish, French, German,
            Danish, Norwegian, Finnish, Icelandic,
        )

        fun fromCode(code: String): AppLanguage =
            all.firstOrNull { it.code == code } ?: English
    }
}
