package com.example.spectroom.data.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.spectroom.R

enum class SensoryPreferenceOption(
    val rawValue: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
) {
    LOW_NOISE(
        "lowNoiseEnvironments",
        R.string.profile_sensory_low_noise_title,
        R.string.profile_sensory_low_noise_subtitle,
    ),
    DIM_LIGHTING(
        "dimSoftLighting",
        R.string.profile_sensory_dim_lighting_title,
        R.string.profile_sensory_dim_lighting_subtitle,
    ),
    ACTIVE_WALKING(
        "activeWalkingDates",
        R.string.profile_sensory_active_walking_title,
        R.string.profile_sensory_active_walking_subtitle,
    ),
    SHORT_MEETUPS(
        "shortLowPressureMeetups",
        R.string.profile_sensory_short_meetups_title,
        R.string.profile_sensory_short_meetups_subtitle,
    ),
    FIDGET_STIM(
        "fidgetStimFriendlySpaces",
        R.string.profile_sensory_fidget_stim_title,
        R.string.profile_sensory_fidget_stim_subtitle,
    ),
    SAFE_FOODS(
        "predictableSafeFoods",
        R.string.profile_sensory_safe_foods_title,
        R.string.profile_sensory_safe_foods_subtitle,
    ),
    SCENT_FREE(
        "scentFreeEnvironments",
        R.string.profile_sensory_scent_free_title,
        R.string.profile_sensory_scent_free_subtitle,
    ),
    TEMPERATURE(
        "comfortableTemperatureZones",
        R.string.profile_sensory_temperature_title,
        R.string.profile_sensory_temperature_subtitle,
    ),
    VISUALLY_CALM(
        "visuallyCalmSpaces",
        R.string.profile_sensory_visually_calm_title,
        R.string.profile_sensory_visually_calm_subtitle,
    ),
    CLEAR_TOUCH(
        "deepPressureClearTouch",
        R.string.profile_sensory_clear_touch_title,
        R.string.profile_sensory_clear_touch_subtitle,
    );

    companion object {
        fun fromRaw(value: String): SensoryPreferenceOption? =
            entries.firstOrNull { it.rawValue == value }

        fun formattedTitles(rawValues: List<String>, context: Context): String =
            rawValues.mapNotNull { fromRaw(it)?.let { option -> context.getString(option.titleRes) } }
                .joinToString(", ")
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
