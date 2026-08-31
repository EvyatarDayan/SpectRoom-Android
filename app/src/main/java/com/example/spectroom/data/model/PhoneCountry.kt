package com.example.spectroom.data.model

data class PhoneCountry(
    val id: String,
    val name: String,
    val flag: String,
    val dialCode: String,
) {
    val displayLabel: String get() = "$flag $dialCode"

    companion object {
        val Sweden = PhoneCountry("SE", "Sweden", "🇸🇪", "+46")

        val all = listOf(
            Sweden,
            PhoneCountry("NO", "Norway", "🇳🇴", "+47"),
            PhoneCountry("DK", "Denmark", "🇩🇰", "+45"),
            PhoneCountry("FI", "Finland", "🇫🇮", "+358"),
            PhoneCountry("DE", "Germany", "🇩🇪", "+49"),
            PhoneCountry("NL", "Netherlands", "🇳🇱", "+31"),
            PhoneCountry("BE", "Belgium", "🇧🇪", "+32"),
            PhoneCountry("FR", "France", "🇫🇷", "+33"),
            PhoneCountry("ES", "Spain", "🇪🇸", "+34"),
            PhoneCountry("IT", "Italy", "🇮🇹", "+39"),
            PhoneCountry("PT", "Portugal", "🇵🇹", "+351"),
            PhoneCountry("GB", "United Kingdom", "🇬🇧", "+44"),
            PhoneCountry("IE", "Ireland", "🇮🇪", "+353"),
            PhoneCountry("AT", "Austria", "🇦🇹", "+43"),
            PhoneCountry("CH", "Switzerland", "🇨🇭", "+41"),
            PhoneCountry("PL", "Poland", "🇵🇱", "+48"),
            PhoneCountry("US", "United States", "🇺🇸", "+1"),
            PhoneCountry("CA", "Canada", "🇨🇦", "+1"),
            PhoneCountry("AU", "Australia", "🇦🇺", "+61"),
            PhoneCountry("IL", "Israel", "🇮🇱", "+972"),
            PhoneCountry("IN", "India", "🇮🇳", "+91"),
            PhoneCountry("BR", "Brazil", "🇧🇷", "+55"),
            PhoneCountry("MX", "Mexico", "🇲🇽", "+52"),
            PhoneCountry("JP", "Japan", "🇯🇵", "+81"),
            PhoneCountry("KR", "South Korea", "🇰🇷", "+82"),
            PhoneCountry("SG", "Singapore", "🇸🇬", "+65"),
            PhoneCountry("AE", "United Arab Emirates", "🇦🇪", "+971"),
            PhoneCountry("ZA", "South Africa", "🇿🇦", "+27"),
            PhoneCountry("TR", "Turkey", "🇹🇷", "+90"),
            PhoneCountry("GR", "Greece", "🇬🇷", "+30"),
        )

        fun buildE164(country: PhoneCountry, nationalNumber: String): String? {
            var digits = nationalNumber.filter { it.isDigit() }
            if (digits.length !in 6..15) return null
            if (digits.startsWith("0")) digits = digits.drop(1)
            if (digits.isEmpty()) return null
            val dial = country.dialCode.filter { it == '+' || it.isDigit() }
            if (!dial.startsWith("+") || dial.length <= 1) return null
            return dial + digits
        }
    }
}
