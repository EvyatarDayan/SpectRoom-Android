package com.example.spectroom.data.local

import android.content.Context
import com.example.spectroom.R
import com.example.spectroom.SpectRoomApplication
import com.example.spectroom.data.model.DiscoverDistanceOption
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.domain.DiscoverEligibility
import com.example.spectroom.ui.locale.AppLocale
import com.example.spectroom.data.local.AppPrefsStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

data class DiscoverDeckFilterSignature(
    val preferredAgeMin: Int,
    val preferredAgeMax: Int,
    val gender: String,
    val interestedIn: List<String>,
    val lookingFor: List<String>,
    val latitude: Double?,
    val longitude: Double?,
    val distanceLevel: Int,
) {
    fun encode(): String = JSONObject().apply {
        put("preferredAgeMin", preferredAgeMin)
        put("preferredAgeMax", preferredAgeMax)
        put("gender", gender)
        put("interestedIn", JSONArray(interestedIn))
        put("lookingFor", JSONArray(lookingFor))
        put("latitude", latitude ?: JSONObject.NULL)
        put("longitude", longitude ?: JSONObject.NULL)
        put("distanceLevel", distanceLevel)
    }.toString()

    companion object {
        fun from(user: SpectRoomUser, distance: DiscoverDistanceOption): DiscoverDeckFilterSignature =
            DiscoverDeckFilterSignature(
                preferredAgeMin = user.preferredAgeMin,
                preferredAgeMax = user.preferredAgeMax,
                gender = user.gender,
                interestedIn = user.interestedIn.sorted(),
                lookingFor = user.lookingFor.sorted(),
                latitude = user.location.latitude,
                longitude = user.location.longitude,
                distanceLevel = distance.level,
            )

        fun decode(raw: String?): DiscoverDeckFilterSignature? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val obj = JSONObject(raw)
                DiscoverDeckFilterSignature(
                    preferredAgeMin = obj.optInt("preferredAgeMin"),
                    preferredAgeMax = obj.optInt("preferredAgeMax"),
                    gender = obj.optString("gender"),
                    interestedIn = obj.optJSONArray("interestedIn").toStringList().sorted(),
                    lookingFor = obj.optJSONArray("lookingFor").toStringList().sorted(),
                    latitude = obj.optDoubleOrNull("latitude"),
                    longitude = obj.optDoubleOrNull("longitude"),
                    distanceLevel = obj.optInt("distanceLevel"),
                )
            }.getOrNull()
        }
    }
}

data class PersistedDiscoverDeck(
    val profiles: List<ProfileCard>,
    val currentIndex: Int,
    val signature: DiscoverDeckFilterSignature,
)

class DiscoverDeckCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadIfValid(
        accountKey: String?,
        expected: DiscoverDeckFilterSignature,
    ): PersistedDiscoverDeck? {
        if (accountKey.isNullOrBlank()) return null
        val storedSig = DiscoverDeckFilterSignature.decode(prefs.getString(sigKey(accountKey), null))
            ?: return null
        if (storedSig != expected) return null
        val profilesJson = prefs.getString(profilesKey(accountKey), null) ?: return null
        val profiles = decodeProfiles(profilesJson)
        if (profiles.isEmpty()) return null
        val index = prefs.getInt(indexKey(accountKey), 0).coerceIn(0, profiles.size)
        return PersistedDiscoverDeck(profiles, index, storedSig)
    }

    fun persist(
        accountKey: String?,
        profiles: List<ProfileCard>,
        currentIndex: Int,
        signature: DiscoverDeckFilterSignature,
    ) {
        if (accountKey.isNullOrBlank()) return
        if (profiles.isEmpty()) {
            clear(accountKey)
            return
        }
        prefs.edit()
            .putString(profilesKey(accountKey), encodeProfiles(profiles))
            .putInt(indexKey(accountKey), currentIndex.coerceIn(0, profiles.size))
            .putString(sigKey(accountKey), signature.encode())
            .apply()
    }

    fun clear(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit()
            .remove(profilesKey(accountKey))
            .remove(indexKey(accountKey))
            .remove(sigKey(accountKey))
            .apply()
    }

    private fun profilesKey(accountKey: String) = "discoverProfilesCache_$accountKey"
    private fun indexKey(accountKey: String) = "discoverCardIndexCache_$accountKey"
    private fun sigKey(accountKey: String) = "discoverFilterSignatureCache_$accountKey"

    companion object {
        private const val PREFS = "spectroom_discover_deck"

        fun encodeProfiles(profiles: List<ProfileCard>): String {
            val array = JSONArray()
            profiles.forEach { array.put(encodeProfile(it)) }
            return array.toString()
        }

        fun decodeProfiles(raw: String): List<ProfileCard> =
            runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        decodeProfile(array.getJSONObject(i))?.let(::add)
                    }
                }
            }.getOrDefault(emptyList())

        private fun encodeProfile(card: ProfileCard): JSONObject = JSONObject().apply {
            put("userId", card.userId)
            put("name", card.name)
            put("age", card.age)
            put("bio", card.bio)
            put("interests", JSONArray(card.interests))
            put("distance", card.distance)
            put("imageURL", card.imageURL)
            put("gender", card.gender)
            put("interestedIn", JSONArray(card.interestedIn))
            put("lookingFor", JSONArray(card.lookingFor))
            put("photos", JSONArray(card.photos))
            put("lifestyle", card.lifestyle)
            put("shareLifestyle", card.shareLifestyle)
            put("bodyType", card.bodyType)
            put("shareBodyType", card.shareBodyType)
            put("preferredAgeMin", card.preferredAgeMin)
            put("preferredAgeMax", card.preferredAgeMax)
            put("isOnline", card.isOnline)
            put("lastSeen", card.lastSeen.time)
            put("idealFirstDate", JSONArray(card.idealFirstDate))
            put("shareIdealFirstDate", card.shareIdealFirstDate)
            put("sensoryPreferences", JSONArray(card.sensoryPreferences))
            put("shareSensoryPreferences", card.shareSensoryPreferences)
            put("energyLevel", card.energyLevel)
            put("matchPercentage", card.matchPercentage)
        }

        private fun decodeProfile(obj: JSONObject): ProfileCard? = runCatching {
            ProfileCard(
                userId = obj.getString("userId"),
                name = obj.optString("name"),
                age = obj.optInt("age"),
                bio = obj.optString("bio"),
                interests = obj.optJSONArray("interests").toStringList(),
                distance = upgradeCachedDistance(obj.optString("distance")),
                imageURL = obj.optNullableString("imageURL"),
                gender = obj.optString("gender"),
                interestedIn = obj.optJSONArray("interestedIn").toStringList(),
                lookingFor = obj.optJSONArray("lookingFor").toStringList(),
                photos = obj.optJSONArray("photos").toStringList(),
                lifestyle = obj.optString("lifestyle"),
                shareLifestyle = obj.optBoolean("shareLifestyle"),
                bodyType = obj.optString("bodyType"),
                shareBodyType = obj.optBoolean("shareBodyType"),
                preferredAgeMin = obj.optInt("preferredAgeMin", 18),
                preferredAgeMax = obj.optInt("preferredAgeMax", 80),
                isOnline = obj.optBoolean("isOnline"),
                lastSeen = Date(obj.optLong("lastSeen", System.currentTimeMillis())),
                idealFirstDate = obj.optJSONArray("idealFirstDate").toStringList(),
                shareIdealFirstDate = obj.optBoolean("shareIdealFirstDate"),
                sensoryPreferences = obj.optJSONArray("sensoryPreferences").toStringList(),
                shareSensoryPreferences = obj.optBoolean("shareSensoryPreferences"),
                energyLevel = obj.optInt("energyLevel", 4),
                matchPercentage = obj.optInt("matchPercentage"),
            )
        }.getOrNull()
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            optString(i)?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

/** Migrates legacy cached "1.5 km" labels to localized "1.5 km away". */
private fun upgradeCachedDistance(raw: String): String {
    val app = runCatching { SpectRoomApplication.instance }.getOrNull() ?: return raw
    val languageCode = AppPrefsStore.readLanguageCode(app)
    val context = AppLocale.localizedContext(app, languageCode)
    return DiscoverEligibility.upgradeDistanceLabel(raw) { amount ->
        context.getString(R.string.search_distance_km, amount)
    }
}
