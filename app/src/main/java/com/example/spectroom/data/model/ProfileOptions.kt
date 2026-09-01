package com.example.spectroom.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.spectroom.R

enum class SensoryPreferenceOption(val rawValue: String, val title: String, val subtitle: String) {
    LOW_NOISE(
        "lowNoiseEnvironments",
        "Low-noise environments",
        "Quieter places help me stay present",
    ),
    DIM_LIGHTING(
        "dimSoftLighting",
        "Dim / soft lighting",
        "Harsh lighting can be overwhelming",
    ),
    ACTIVE_WALKING(
        "activeWalkingDates",
        "Active walking dates",
        "Moving together feels easier than sitting still",
    ),
    SHORT_MEETUPS(
        "shortLowPressureMeetups",
        "Short, low-pressure meetups",
        "Brief plans with an easy exit",
    ),
    FIDGET_STIM(
        "fidgetStimFriendlySpaces",
        "Fidget / stim-friendly spaces",
        "Places where stimming is welcome",
    ),
    SAFE_FOODS(
        "predictableSafeFoods",
        "Predictable / safe foods",
        "Familiar menus reduce stress",
    ),
    SCENT_FREE(
        "scentFreeEnvironments",
        "Scent-free environments",
        "Strong fragrances can be a barrier",
    ),
    TEMPERATURE(
        "comfortableTemperatureZones",
        "Comfortable temperature zones",
        "Not too hot, not too cold",
    ),
    VISUALLY_CALM(
        "visuallyCalmSpaces",
        "Visually calm spaces",
        "Less visual clutter feels better",
    ),
    CLEAR_TOUCH(
        "deepPressureClearTouch",
        "Deep pressure / clear touch",
        "Clear consent and firm contact preferred",
    );

    companion object {
        fun fromRaw(value: String): SensoryPreferenceOption? =
            entries.firstOrNull { it.rawValue == value }

        fun formattedTitles(rawValues: List<String>): String =
            rawValues.mapNotNull { fromRaw(it)?.title }.joinToString(", ")
    }
}

enum class DiscoverDistanceOption(val level: Int, val maxKilometers: Double?) {
    FIVE_KM(0, 5.0),
    TEN_KM(1, 10.0),
    TWENTY_KM(2, 20.0),
    FIFTY_KM(3, 50.0),
    UNLIMITED(4, null);

    val shortButtonTitle: String
        get() = when (this) {
            FIVE_KM -> "5km"
            TEN_KM -> "10km"
            TWENTY_KM -> "20km"
            FIFTY_KM -> "50km"
            UNLIMITED -> "∞"
        }

    @StringRes
    fun titleRes(): Int = when (this) {
        FIVE_KM -> R.string.settings_distance_5km
        TEN_KM -> R.string.settings_distance_10km
        TWENTY_KM -> R.string.settings_distance_20km
        FIFTY_KM -> R.string.settings_distance_50km
        UNLIMITED -> R.string.settings_distance_unlimited
    }

    @DrawableRes
    fun iconRes(): Int = when (this) {
        UNLIMITED -> R.drawable.ic_world
        else -> R.drawable.ic_radius
    }

    val illustrationBarWidthDp: Float?
        get() = when (this) {
            FIVE_KM -> 47f
            TEN_KM -> 64f
            TWENTY_KM -> 80f
            FIFTY_KM -> 97f
            UNLIMITED -> null
        }

    companion object {
        val DEFAULT = TWENTY_KM

        fun fromLevel(level: Int): DiscoverDistanceOption =
            entries.firstOrNull { it.level == level } ?: DEFAULT
    }
}

object GenderPreferenceOption {
    const val MAN = "Man"
    const val WOMAN = "Woman"
    const val NON_BINARY = "Non-binary"

    val all = listOf(MAN, WOMAN, NON_BINARY)

    fun normalizeInterestedIn(values: List<String>): List<String> {
        val selected = linkedSetOf<String>()
        var hadAnyone = false
        for (value in values) {
            when {
                value.equals("Anyone", ignoreCase = true) -> hadAnyone = true
                value.equals("Other", ignoreCase = true) -> selected.add(NON_BINARY)
                else -> all.firstOrNull { it.equals(value, ignoreCase = true) }?.let(selected::add)
            }
        }
        if (hadAnyone) selected.addAll(all)
        return all.filter { it in selected }
    }

    fun hasValidInterestedIn(values: List<String>): Boolean =
        normalizeInterestedIn(values).isNotEmpty()
}

enum class LookingForOption(val rawValue: String, val displayName: String) {
    ROMANTIC("Romantic relationship", "Romantic relationship"),
    IN_PERSON_FRIENDSHIP("In-Person Friendship", "In-Person Friendship"),
    ONLINE_FRIENDSHIP("Online Friendship", "Online Friendship"),
    HOBBY_BUDDY("Hobby Buddy", "Hobby Buddy"),
    NOT_SURE("Not Sure Yet", "Not Sure Yet"),
}

enum class LifestyleOption(val rawValue: String, val displayName: String) {
    PREFER_NOT("preferNot", "Prefer not to say"),
    INDEPENDENT("independent", "Independent"),
    FAMILY_SUPPORT("familySupport", "Family support"),
    ASSISTED("assistedSetting", "Assisted setting"),
}

enum class BodyTypeOption(val rawValue: String, val displayName: String) {
    PREFER_NOT("preferNot", "Prefer not to say"),
    SLIM("slim", "Slim"),
    AVERAGE("average", "Average"),
    ATHLETIC("athletic", "Athletic"),
    FULL_BODIED("fullBodied", "Full-bodied"),
}

enum class IdealFirstDateOption(val rawValue: String, val displayName: String) {
    PARK_WALK("relaxingParkWalk", "Relaxing park walk"),
    CAFE("cozyCafeMeet", "Cozy café meet"),
    MUSEUM("museumGalleryStroll", "Museum / gallery stroll"),
    CINEMA("cinemaMovieNight", "Cinema / movie night"),
    VIDEO_CHAT("friendlyVideoChat", "Friendly video chat"),
}

object EnergyLevel {
    const val LOW = 0
    const val MODERATE = 2
    const val FULLY_SOCIAL = 4

    val all = listOf(LOW, MODERATE, FULLY_SOCIAL)

    fun resolve(level: Int): Int = when {
        level <= 1 -> LOW
        level <= 3 -> MODERATE
        else -> FULLY_SOCIAL
    }

    fun label(level: Int): String = when (resolve(level)) {
        LOW -> "Low Battery"
        MODERATE -> "Moderate Social"
        else -> "Fully Social Mode"
    }

    @StringRes
    fun titleRes(level: Int): Int = when (resolve(level)) {
        LOW -> R.string.profile_energy_low_battery
        MODERATE -> R.string.profile_energy_moderate_social
        else -> R.string.profile_energy_fully_social
    }

    fun emoji(level: Int): String = when (resolve(level)) {
        LOW -> "🌙"
        MODERATE -> "⛅"
        else -> "☀️"
    }

    fun detail(level: Int): String = when (resolve(level)) {
        LOW -> "Taking time to recharge. Slow to reply today."
        MODERATE -> "Open to casual text chats at a steady pace."
        else -> "High energy! Ready for fast chats, calls, or making plans."
    }

    @StringRes
    fun detailRes(level: Int): Int = when (resolve(level)) {
        LOW -> R.string.profile_energy_low_battery_detail
        MODERATE -> R.string.profile_energy_moderate_social_detail
        else -> R.string.profile_energy_fully_social_detail
    }
}
