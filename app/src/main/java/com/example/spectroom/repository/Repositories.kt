package com.example.spectroom.repository

import com.example.spectroom.data.model.AdminSender
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.ConversationDisplay
import com.example.spectroom.data.model.ConversationMessagesPage
import com.example.spectroom.data.model.MatchDisplay
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.MessageType
import com.example.spectroom.data.model.ModerationNotice
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.data.model.SwipeType
import com.example.spectroom.data.model.UserLocation
import com.example.spectroom.data.model.UserProfileStatus
import com.example.spectroom.domain.DiscoverEligibility
import com.example.spectroom.domain.MatchScoring
import com.example.spectroom.domain.ProfileCompletion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun firestoreCreatedAtMillis(data: Map<String, Any>): Long? {
    return when (val raw = data["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Number -> (raw.toDouble() * 1000.0).toLong()
        else -> null
    }
}

private suspend fun deleteFirestoreDocuments(
    db: FirebaseFirestore,
    documents: List<DocumentSnapshot>,
) {
    documents.chunked(450).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class MfaRequired(val message: String = "SMS verification required") : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class Banned(val message: String) : AuthResult()
    data class Frozen(val until: Date, val message: String, val hours: Int = 24) : AuthResult()
    data class Deleted(val message: String) : AuthResult()
}

sealed class AccessGate {
    data object Allowed : AccessGate()
    data class Banned(val message: String) : AccessGate()
    data class Frozen(val until: Date, val message: String, val hours: Int = 24) : AccessGate()
    data class Deleted(val message: String) : AccessGate()
}

data class UsersPage(
    val users: List<SpectRoomUser>,
    val lastDocumentId: String?,
    val hasMore: Boolean,
)

data class SwipeFilterState(
    val likedUserIds: Set<String> = emptySet(),
    val passedUserIds: Set<String> = emptySet(),
    val permanentlyPassedUserIds: Set<String> = emptySet(),
    val reviewablePassedUserIds: Set<String> = emptySet(),
)

data class IncomingMessageHint(
    val messageDocId: String,
    val senderId: String,
    val preview: String,
    val type: String,
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUid: String? get() = auth.currentUser?.uid
    val currentEmail: String? get() = auth.currentUser?.email
    val accountKey: String?
        get() = currentUid?.takeIf { it.isNotEmpty() }
            ?: currentEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): AuthResult = try {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: return AuthResult.Error("Sign-in failed")
        when (val gate = evaluateAccessGate(user.uid)) {
            is AccessGate.Allowed -> AuthResult.Success(user)
            is AccessGate.Banned -> AuthResult.Banned(gate.message)
            is AccessGate.Frozen -> AuthResult.Frozen(gate.until, gate.message, gate.hours)
            is AccessGate.Deleted -> AuthResult.Deleted(gate.message)
        }
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Sign-in failed")
    }

    suspend fun evaluateAccessGate(uid: String = currentUid.orEmpty()): AccessGate {
        if (uid.isEmpty()) return AccessGate.Allowed
        val snapshot = db.collection("users").document(uid).get().await()
        if (!snapshot.exists()) return AccessGate.Allowed
        if (snapshot.getBoolean("isDeleted") == true) {
            return AccessGate.Deleted("This account has been deleted.")
        }
        if (snapshot.getBoolean("isBanned") == true) {
            return AccessGate.Banned("This account has been permanently banned.")
        }
        val freezeUntil = parseDiscoverFrozenUntil(snapshot)
        if (freezeUntil != null && freezeUntil.after(Date())) {
            return AccessGate.Frozen(
                until = freezeUntil,
                message = "Your account is temporarily frozen.",
                hours = freezeDurationHours(snapshot.getLong("discoverFreezeHours")?.toInt(), freezeUntil),
            )
        }
        return AccessGate.Allowed
    }

    suspend fun fetchPendingModerationNotice(): ModerationNotice? {
        val uid = currentUid ?: return null
        val snapshot = db.collection("users").document(uid).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toPendingModerationNotice()
    }

    suspend fun acknowledgeModerationNotice(notice: ModerationNotice) {
        val uid = currentUid ?: return
        val updates = when (notice) {
            is ModerationNotice.Warning -> mapOf(
                "pendingSystemWarning" to com.google.firebase.firestore.FieldValue.delete(),
            )
            is ModerationNotice.FreezeAcknowledgement -> mapOf(
                "pendingAccountFreeze.acknowledged" to true,
            )
            is ModerationNotice.BanLifted -> mapOf(
                "pendingAccountUnban" to com.google.firebase.firestore.FieldValue.delete(),
                "pendingBanLifted" to com.google.firebase.firestore.FieldValue.delete(),
            )
            is ModerationNotice.FreezeLifted -> mapOf(
                "pendingAccountUnfreeze" to com.google.firebase.firestore.FieldValue.delete(),
            )
            else -> emptyMap()
        }
        if (updates.isNotEmpty()) {
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
        }
    }

    fun observeModerationNotices(): Flow<ModerationNotice?> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toPendingModerationNotice())
            }
        awaitClose { registration.remove() }
    }

    suspend fun signUp(name: String, email: String, password: String, phoneE164: String): AuthResult {
        return try {
            val normalized = email.trim().lowercase()
            ensureEmailAllowedForAuth(normalized)
            val result = auth.createUserWithEmailAndPassword(normalized, password).await()
            val user = result.user ?: return AuthResult.Error("Sign-up failed")
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build(),
            ).await()
            createIncompleteUserDocument(user.uid, name.trim(), normalized, phoneE164.trim())
            AuthResult.Success(user)
        } catch (e: Exception) {
            val normalized = email.trim().lowercase()
            if (normalized.contains("@") && isEmailAlreadyInUse(e)) {
                val pending = runCatching { pendingDeletionInfo(normalized) }.getOrNull()
                if (pending?.isPending == true) {
                    return AuthResult.Error(pendingDeletionMessage(pending.deletedByAdmin))
                }
            }
            AuthResult.Error(e.message ?: "Sign-up failed")
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String): Result<Unit> = try {
        val normalized = email.trim().lowercase()
        require(normalized.contains("@")) { "Enter a valid email" }
        try {
            com.google.firebase.functions.FirebaseFunctions
                .getInstance("us-central1")
                .getHttpsCallable("sendSpectRoomPasswordResetEmail")
                .call(mapOf("email" to normalized))
                .await()
        } catch (_: Exception) {
            auth.sendPasswordResetEmail(normalized).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    data class PendingDeletionInfo(
        val isPending: Boolean,
        val deletedByAdmin: Boolean,
    )

    suspend fun pendingDeletionInfo(email: String): PendingDeletionInfo {
        val normalized = email.trim().lowercase()
        if (!normalized.contains("@")) return PendingDeletionInfo(false, false)
        return try {
            val result = com.google.firebase.functions.FirebaseFunctions
                .getInstance("us-central1")
                .getHttpsCallable("checkEmailPendingDeletion")
                .call(mapOf("email" to normalized))
                .await()
            val data = result.getData() as? Map<*, *> ?: return PendingDeletionInfo(false, false)
            PendingDeletionInfo(
                isPending = boolValue(data["pendingDeletion"]),
                deletedByAdmin = boolValue(data["deletedByAdmin"]),
            )
        } catch (_: Exception) {
            PendingDeletionInfo(false, false)
        }
    }

    suspend fun ensureEmailAllowedForAuth(email: String) {
        val info = pendingDeletionInfo(email)
        if (!info.isPending) return
        error(pendingDeletionMessage(info.deletedByAdmin))
    }

    private fun pendingDeletionMessage(deletedByAdmin: Boolean): String =
        if (deletedByAdmin) {
            "This account has been deleted."
        } else {
            "This email is linked to a recently deleted account. Try again later or contact support."
        }

    private fun boolValue(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true)
        else -> false
    }

    private fun isEmailAlreadyInUse(error: Exception): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("email") &&
            (message.contains("already") || message.contains("in use") || message.contains("exists"))
    }

    suspend fun fetchCurrentUserProfile(fromServer: Boolean = false): SpectRoomUser? {
        val uid = currentUid ?: return null
        val reference = db.collection("users").document(uid)
        val snapshot = if (fromServer) {
            reference.get(Source.SERVER).await()
        } else {
            reference.get().await()
        }
        if (!snapshot.exists()) return null
        return snapshot.toSpectRoomUser(uid)
    }

    /** Live updates when iOS (or another client) changes billing plan on the user document. */
    fun observeBillingPlan(): Flow<Pair<PlanTier, BillingInterval?>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val tier = PlanTier.fromRaw(snapshot.getString("planTier"))
                val intervalRaw = snapshot.getString("billingInterval")
                val interval = intervalRaw
                    ?.takeIf { it.isNotBlank() }
                    ?.let { BillingInterval.fromRaw(it) }
                trySend(tier to interval)
            }
        awaitClose { registration.remove() }
    }

    fun observePlanTier(): Flow<PlanTier> = observeBillingPlan().map { it.first }

    suspend fun saveUserProfile(user: SpectRoomUser): Result<SpectRoomUser> {
        val uid = currentUid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val status = ProfileCompletion.evaluate(user)
            val data = hashMapOf<String, Any?>(
                "name" to user.name,
                "age" to user.age,
                "dateOfBirth" to user.dateOfBirth?.let { it.time / 1000.0 },
                "email" to user.email,
                "bio" to user.bio,
                "interests" to user.interests,
                "profilePhotoURL" to user.profilePhotoURL,
                "photos" to user.photos,
                "gender" to user.gender,
                "interestedIn" to user.interestedIn,
                "lookingFor" to user.lookingFor,
                "preferredAgeMin" to user.preferredAgeMin,
                "preferredAgeMax" to user.preferredAgeMax,
                "lifestyle" to user.lifestyle,
                "shareLifestyle" to user.shareLifestyle,
                "bodyType" to user.bodyType,
                "shareBodyType" to user.shareBodyType,
                "idealFirstDate" to user.idealFirstDate,
                "shareIdealFirstDate" to user.shareIdealFirstDate,
                "sensoryPreferences" to user.sensoryPreferences,
                "shareSensoryPreferences" to user.shareSensoryPreferences,
                "energyLevel" to user.energyLevel,
                "location" to mapOf(
                    "displayString" to user.location.displayString,
                    "city" to user.location.city,
                    "state" to user.location.state,
                    "country" to user.location.country,
                    "latitude" to user.location.latitude,
                    "longitude" to user.location.longitude,
                ),
                UserProfileStatus.FIRESTORE_KEY to status.rawValue,
            )
            db.collection("users").document(uid).set(data, SetOptions.merge()).await()
            Result.success(user.copy(firebaseDocumentId = uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBillingPlan(tier: PlanTier, interval: BillingInterval?) {
        val uid = currentUid ?: return
        val data = linkedMapOf<String, Any>("planTier" to tier.rawValue)
        when {
            tier == PlanTier.FREE -> data["billingInterval"] = ""
            interval != null -> data["billingInterval"] = interval.rawValue
        }
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun updatePlanTier(tier: PlanTier) {
        updateBillingPlan(tier, interval = null)
    }

    suspend fun activateProfileBoost(activeUntilMillis: Long, lastBoostAtMillis: Long = System.currentTimeMillis()) {
        val uid = currentUid ?: return
        db.collection("users").document(uid).set(
            mapOf(
                "boostedUntil" to activeUntilMillis / 1000.0,
                "lastProfileBoostAt" to lastBoostAtMillis / 1000.0,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun updateEnergyLevel(level: Int) {
        val uid = currentUid ?: return
        val clamped = when {
            level <= 1 -> 0
            level <= 3 -> 2
            else -> 4
        }
        db.collection("users").document(uid)
            .set(mapOf("energyLevel" to clamped), SetOptions.merge())
            .await()
    }

    suspend fun updateOnlineStatus(isOnline: Boolean) {
        val uid = currentUid ?: return
        val now = Date().time / 1000.0
        val payload = mutableMapOf<String, Any?>(
            "isOnline" to isOnline,
            "lastSeen" to now,
            "appIsActive" to isOnline,
            "appStateUpdatedAt" to now,
        )
        if (!isOnline) {
            payload["openConversationUserId"] = com.google.firebase.firestore.FieldValue.delete()
            payload["messagesTabActive"] = false
        }
        db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
    }

    suspend fun touchPresenceHeartbeat() {
        val uid = currentUid ?: return
        val now = Date().time / 1000.0
        db.collection("users").document(uid).set(
            mapOf(
                "appIsActive" to true,
                "appStateUpdatedAt" to now,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun updateMessagesTabActive(isActive: Boolean) {
        val uid = currentUid ?: return
        db.collection("users").document(uid).set(
            mapOf(
                "messagesTabActive" to isActive,
                "appStateUpdatedAt" to Date().time / 1000.0,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun updateOpenConversation(partnerId: String?) {
        val uid = currentUid ?: return
        val now = Date().time / 1000.0
        val payload = mutableMapOf<String, Any?>(
            "appIsActive" to true,
            "appStateUpdatedAt" to now,
        )
        if (partnerId.isNullOrBlank()) {
            payload["openConversationUserId"] = com.google.firebase.firestore.FieldValue.delete()
        } else {
            payload["openConversationUserId"] = partnerId
        }
        db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
    }

    suspend fun saveFcmToken(token: String) {
        val uid = currentUid ?: return
        db.collection("users").document(uid).set(
            mapOf("fcmToken" to token),
            SetOptions.merge(),
        ).await()
    }

    suspend fun reauthenticate(password: String) {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("Missing email")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
    }

    suspend fun requestAccountDeletion() {
        val user = auth.currentUser ?: error("Not signed in")
        val uid = user.uid
        val now = Date().time / 1000.0
        val hardDeleteAt = now + (14 * 24 * 60 * 60)
        val normalizedEmail = user.email?.trim()?.lowercase().orEmpty()
        db.collection("users").document(uid).set(
            mapOf(
                "isDeleted" to true,
                "deletedAt" to now,
                "scheduledHardDeleteAt" to hardDeleteAt,
                "isOnline" to false,
                "fcmToken" to com.google.firebase.firestore.FieldValue.delete(),
            ),
            SetOptions.merge(),
        ).await()
        if (normalizedEmail.isNotEmpty()) {
            runCatching {
                db.collection("deletedEmails").document(normalizedEmail).set(
                    mapOf(
                        "uid" to uid,
                        "email" to normalizedEmail,
                        "deletedAt" to now,
                        "scheduledHardDeleteAt" to hardDeleteAt,
                    ),
                ).await()
            }
        }
        auth.signOut()
    }

    private suspend fun createIncompleteUserDocument(
        uid: String,
        name: String,
        email: String,
        phoneE164: String,
    ) {
        val data = hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "name" to name,
            "email" to email,
            "age" to 0,
            "bio" to "",
            "interests" to emptyList<String>(),
            "photos" to emptyList<String>(),
            "gender" to "",
            "interestedIn" to emptyList<String>(),
            "lookingFor" to emptyList<String>(),
            "idealFirstDate" to emptyList<String>(),
            "preferredAgeMin" to 18,
            "preferredAgeMax" to 35,
            "energyLevel" to 4,
            "isOnline" to false,
            "lastSeen" to Date().time / 1000.0,
            "createdAt" to Date().time / 1000.0,
            "location" to mapOf(
                "displayString" to "",
                "city" to "",
                "state" to "",
                "country" to "",
            ),
            UserProfileStatus.FIRESTORE_KEY to UserProfileStatus.INCOMPLETE.rawValue,
        )
        if (phoneE164.startsWith("+") && phoneE164.length >= 10) {
            data["phoneE164"] = phoneE164
        }
        db.collection("users").document(uid).set(data).await()
    }
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toSpectRoomUser(uid: String): SpectRoomUser {
    val locationMap = get("location") as? Map<String, Any?> ?: emptyMap()
    val age = (get("age") as? Number)?.toInt() ?: 0
    val dateOfBirthSeconds = (get("dateOfBirth") as? Number)?.toDouble()
    val dateOfBirth = dateOfBirthSeconds?.let { Date((it * 1000).toLong()) }
    return SpectRoomUser(
        id = getString("id") ?: uid,
        firebaseDocumentId = uid,
        name = getString("name") ?: "",
        age = age,
        dateOfBirth = dateOfBirth,
        email = getString("email") ?: "",
        bio = getString("bio") ?: "",
        interests = get("interests") as? List<String> ?: emptyList(),
        profilePhotoURL = getString("profilePhotoURL"),
        photos = get("photos") as? List<String> ?: emptyList(),
        location = UserLocation(
            displayString = locationMap["displayString"] as? String ?: "",
            city = locationMap["city"] as? String ?: "",
            state = locationMap["state"] as? String ?: "",
            country = locationMap["country"] as? String ?: "",
            latitude = (locationMap["latitude"] as? Number)?.toDouble(),
            longitude = (locationMap["longitude"] as? Number)?.toDouble(),
        ),
        isOnline = getBoolean("isOnline") ?: false,
        gender = getString("gender") ?: "",
        interestedIn = get("interestedIn") as? List<String> ?: emptyList(),
        lookingFor = get("lookingFor") as? List<String> ?: emptyList(),
        preferredAgeMin = (get("preferredAgeMin") as? Number)?.toInt() ?: 18,
        preferredAgeMax = (get("preferredAgeMax") as? Number)?.toInt() ?: 35,
        lifestyle = getString("lifestyle") ?: "",
        shareLifestyle = getBoolean("shareLifestyle") ?: false,
        bodyType = getString("bodyType") ?: "",
        shareBodyType = getBoolean("shareBodyType") ?: false,
        idealFirstDate = get("idealFirstDate") as? List<String> ?: emptyList(),
        shareIdealFirstDate = getBoolean("shareIdealFirstDate") ?: false,
        sensoryPreferences = get("sensoryPreferences") as? List<String> ?: emptyList(),
        shareSensoryPreferences = getBoolean("shareSensoryPreferences") ?: false,
        energyLevel = (get("energyLevel") as? Number)?.toInt() ?: 4,
        planTier = PlanTier.fromRaw(getString("planTier")),
        billingInterval = getString("billingInterval")
            ?.takeIf { it.isNotBlank() }
            ?.let { BillingInterval.fromRaw(it) },
        lastProfileBoostAtMillis = (get("lastProfileBoostAt") as? Number)?.toDouble()?.let { (it * 1000).toLong() }
            ?: (get("lastProfileBoostAt") as? Number)?.toLong(),
        boostedUntilMillis = (get("boostedUntil") as? Number)?.toDouble()?.let { seconds ->
            val millis = if (seconds < 10_000_000_000.0) (seconds * 1000).toLong() else seconds.toLong()
            millis
        },
    )
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toPendingModerationNotice(): ModerationNotice? {
    if (getBoolean("isDeleted") == true) {
        return ModerationNotice.DeletedAcknowledgement
    }
    if (getBoolean("isBanned") == true) {
        return ModerationNotice.BanAcknowledgement
    }
    if (get("pendingAccountUnban") != null || get("pendingBanLifted") != null) {
        return ModerationNotice.BanLifted
    }
    val pendingUnfreeze = get("pendingAccountUnfreeze") as? Map<String, Any?>
    if (pendingUnfreeze != null) {
        return ModerationNotice.FreezeLifted
    }
    val pendingFreeze = get("pendingAccountFreeze") as? Map<String, Any?>
    if (pendingFreeze != null && pendingFreeze["acknowledged"] as? Boolean != true) {
        val until = parseFreezeUntilValue(pendingFreeze["freeze_until"])
            ?: parseDiscoverFrozenUntil(this)
        if (until != null && until.after(Date())) {
            val hours = freezeDurationHours(
                (pendingFreeze["hours"] as? Number)?.toInt()
                    ?: (get("discoverFreezeHours") as? Number)?.toInt(),
                until,
            )
            return ModerationNotice.FreezeAcknowledgement(until, hours)
        }
    }
    val warning = get("pendingSystemWarning") as? Map<String, Any?>
    if (warning != null && warning["acknowledged"] as? Boolean != true) {
        val message = warning["message"] as? String
            ?: "Your account was recently reported for content or behavior that violates our guidelines."
        val noticeId = (warning["notice_id"] as? String)?.takeIf { it.isNotBlank() }
            ?: (warning["report_id"] as? String)?.takeIf { it.isNotBlank() }
        val createdAt = (warning["created_at"] as? Number)?.toDouble()
        return ModerationNotice.Warning(
            message = message,
            noticeId = noticeId,
            createdAtSeconds = createdAt,
        )
    }
    val activeFreeze = parseDiscoverFrozenUntil(this)
    if (activeFreeze != null && activeFreeze.after(Date())) {
        val hours = freezeDurationHours(
            (get("discoverFreezeHours") as? Number)?.toInt(),
            activeFreeze,
        )
        return ModerationNotice.FreezeAcknowledgement(activeFreeze, hours)
    }
    return null
}

/** Original freeze length (24 / 48 / 72), never remaining time. */
fun freezeDurationHours(storedHours: Int?, until: Date): Int {
    val stored = storedHours ?: 0
    if (stored == 24 || stored == 48 || stored == 72) return stored
    val remainingHours = ((until.time - System.currentTimeMillis()) / 3_600_000.0)
        .coerceAtLeast(0.0)
    return when {
        remainingHours <= 24 -> 24
        remainingHours <= 48 -> 48
        else -> 72
    }
}

fun parseDiscoverFrozenUntil(snapshot: DocumentSnapshot): Date? {
    snapshot.getTimestamp("discoverFrozenUntil")?.toDate()?.let { return it }
    return parseFreezeUntilValue(snapshot.get("discoverFrozenUntil"))
        ?: parseFreezeUntilValue(snapshot.get("freezeUntil"))
}

fun parseFreezeUntilValue(value: Any?): Date? = when (value) {
    is Timestamp -> value.toDate()
    is Date -> value
    is Number -> {
        val raw = value.toDouble()
        // Support seconds or milliseconds.
        Date(if (raw > 10_000_000_000) raw.toLong() else (raw * 1000).toLong())
    }
    else -> null
}

class DiscoverRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun fetchUsersPage(limit: Int = 30, startAfterDocumentId: String? = null): UsersPage {
        val currentUserId = auth.currentUser?.uid
        val snapshot = try {
            var query: Query = db.collection("users")
                .orderBy(FieldPath.documentId())
                .limit(limit.toLong())
            if (startAfterDocumentId != null) {
                val previous = db.collection("users").document(startAfterDocumentId).get().await()
                if (previous.exists()) {
                    query = query.startAfter(previous)
                }
            }
            query.get().await()
        } catch (_: Exception) {
            db.collection("users").get().await()
        }

        val users = snapshot.documents.mapNotNull { doc ->
            parseDiscoverUser(doc, excludingUserId = currentUserId)
        }
        val lastId = snapshot.documents.lastOrNull()?.id
        return UsersPage(
            users = users,
            lastDocumentId = lastId,
            hasMore = snapshot.documents.size == limit,
        )
    }

    suspend fun fetchSwipeFilterState(): SwipeFilterState {
        val uid = auth.currentUser?.uid ?: return SwipeFilterState()
        val snapshot = db.collection("users").document(uid).collection("swipes").get().await()
        val latestByUser = mutableMapOf<String, Pair<String, Double>>()
        val passDocsByUser = mutableMapOf<String, MutableList<Map<String, Any>>>()

        for (document in snapshot.documents) {
            val data = document.data ?: continue
            val toUserId = data["toUserId"] as? String ?: continue
            val action = data["action"] as? String ?: continue
            val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: 0.0
            val existing = latestByUser[toUserId]
            if (existing == null || existing.second < createdAt) {
                latestByUser[toUserId] = action to createdAt
            }
            if (action == SwipeType.PASS.rawValue) {
                passDocsByUser.getOrPut(toUserId) { mutableListOf() }.add(data)
            }
        }

        val liked = mutableSetOf<String>()
        val passed = mutableSetOf<String>()
        val permanent = mutableSetOf<String>()
        val reviewable = mutableSetOf<String>()
        val now = System.currentTimeMillis() / 1000.0

        for ((userId, state) in latestByUser) {
            when (state.first) {
                SwipeType.LIKE.rawValue, SwipeType.SUPER_LIKE.rawValue -> liked.add(userId)
                SwipeType.PASS.rawValue -> {
                    val passDocs = passDocsByUser[userId].orEmpty()
                    val passCount = effectivePassCount(passDocs)
                    val latestPassAt = passDocs.maxOfOrNull {
                        (it["createdAt"] as? Number)?.toDouble() ?: 0.0
                    } ?: state.second
                    if (passCount >= 2) {
                        permanent.add(userId)
                        passed.add(userId)
                    } else if (DiscoverEligibility.isPassStillHidden(passCount, latestPassAt, now)) {
                        passed.add(userId)
                        reviewable.add(userId)
                    }
                }
            }
        }

        return SwipeFilterState(
            likedUserIds = liked,
            passedUserIds = passed,
            permanentlyPassedUserIds = permanent,
            reviewablePassedUserIds = reviewable,
        )
    }

    suspend fun nextPassCount(targetUserId: String): Int {
        val uid = auth.currentUser?.uid ?: return 1
        val snapshot = db.collection("users").document(uid).collection("swipes")
            .whereEqualTo("toUserId", targetUserId)
            .whereEqualTo("action", SwipeType.PASS.rawValue)
            .get()
            .await()
        val maxStored = snapshot.documents.mapNotNull {
            (it.data?.get("passCount") as? Number)?.toInt()
        }.maxOrNull() ?: 0
        return maxOf(maxStored, snapshot.size()) + 1
    }

    suspend fun recordSwipe(toUserId: String, action: SwipeType) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val data = hashMapOf<String, Any>(
            "id" to UUID.randomUUID().toString(),
            "fromUserId" to uid,
            "toUserId" to toUserId,
            "action" to action.rawValue,
            "createdAt" to Date().time / 1000.0,
        )
        if (action == SwipeType.PASS) {
            data["passCount"] = nextPassCount(toUserId)
        }
        db.collection("users").document(uid).collection("swipes").add(data).await()
    }

    fun matchDocumentId(userId1: String, userId2: String): String =
        listOf(userId1, userId2).sorted().joinToString("_")

    suspend fun matchExists(otherUserId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val matchId = matchDocumentId(uid, otherUserId)
        return db.collection("users").document(uid).collection("matches").document(matchId)
            .get().await().exists()
    }

    private suspend fun latestSwipeAction(fromUserId: String, toUserId: String): SwipeType? {
        val snapshot = db.collection("users").document(fromUserId).collection("swipes")
            .whereEqualTo("toUserId", toUserId)
            .get()
            .await()
        var latestAction: String? = null
        var latestCreatedAt = 0.0
        for (document in snapshot.documents) {
            val data = document.data ?: continue
            val action = data["action"] as? String ?: continue
            val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: 0.0
            if (createdAt >= latestCreatedAt) {
                latestCreatedAt = createdAt
                latestAction = action
            }
        }
        return latestAction?.let(SwipeType::fromRaw)
    }

    suspend fun hasMutualLike(otherUserId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val current = latestSwipeAction(fromUserId = uid, toUserId = otherUserId)
        val other = latestSwipeAction(fromUserId = otherUserId, toUserId = uid)
        fun positive(action: SwipeType?) =
            action == SwipeType.LIKE || action == SwipeType.SUPER_LIKE
        return positive(current) && positive(other)
    }

    suspend fun checkForMatch(otherUserId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (!hasMutualLike(otherUserId)) return false
        val matchId = matchDocumentId(uid, otherUserId)
        val existing = db.collection("users").document(uid).collection("matches")
            .document(matchId).get().await()
        if (existing.exists()) return true
        createMatch(otherUserId = otherUserId, matchId = matchId)
        return true
    }

    private suspend fun createMatch(otherUserId: String, matchId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (!hasMutualLike(otherUserId)) return
        val sorted = listOf(uid, otherUserId).sorted()
        val matchData = hashMapOf<String, Any?>(
            "id" to matchId,
            "user1Id" to sorted[0],
            "user2Id" to sorted[1],
            "matchedAt" to Date().time / 1000.0,
            "isActive" to true,
            "lastMessageAt" to null,
            "conversationId" to UUID.randomUUID().toString(),
        )
        val currentRef = db.collection("users").document(uid).collection("matches").document(matchId)
        val otherRef = db.collection("users").document(otherUserId).collection("matches").document(matchId)
        if (!currentRef.get().await().exists()) {
            currentRef.set(matchData).await()
        }
        if (!otherRef.get().await().exists()) {
            otherRef.set(matchData).await()
        }
    }

    suspend fun deleteMatch(otherUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        val matchId = matchDocumentId(uid, otherUserId)
        db.collection("users").document(uid).collection("matches").document(matchId)
            .delete().await()
        runCatching {
            db.collection("users").document(otherUserId).collection("matches").document(matchId)
                .delete().await()
        }
    }

    suspend fun undoLastSwipe(toUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        val snapshot = db.collection("users").document(uid).collection("swipes")
            .whereEqualTo("toUserId", toUserId)
            .get()
            .await()
        val newest = snapshot.documents.maxByOrNull {
            (it.data?.get("createdAt") as? Number)?.toDouble() ?: 0.0
        } ?: return
        val action = newest.getString("action")
        newest.reference.delete().await()
        if (action == SwipeType.LIKE.rawValue || action == SwipeType.SUPER_LIKE.rawValue) {
            runCatching { deleteMatch(toUserId) }
        }
    }

    suspend fun fetchTodaysPassedUserIds(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis / 1000.0
        val snapshot = db.collection("users").document(uid).collection("swipes")
            .whereEqualTo("action", SwipeType.PASS.rawValue)
            .get()
            .await()
        val latestPassToday = mutableMapOf<String, Double>()
        for (document in snapshot.documents) {
            val data = document.data ?: continue
            val toUserId = data["toUserId"] as? String ?: continue
            val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: continue
            if (createdAt < startOfDay) continue
            val existing = latestPassToday[toUserId]
            if (existing == null || createdAt > existing) {
                latestPassToday[toUserId] = createdAt
            }
        }
        return latestPassToday.entries.sortedByDescending { it.value }.map { it.key }
    }

    suspend fun fetchUsersByIds(userIds: List<String>): List<SpectRoomUser> {
        if (userIds.isEmpty()) return emptyList()
        val currentUserId = auth.currentUser?.uid
        val users = mutableListOf<SpectRoomUser>()
        userIds.chunked(10).forEach { chunk ->
            val snapshot = db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                parseDiscoverUser(doc, excludingUserId = currentUserId)?.let(users::add)
            }
        }
        return users
    }

    fun toProfileCard(
        user: SpectRoomUser,
        currentUser: SpectRoomUser,
        maxDistanceKm: Double?,
        forMatches: Boolean = false,
    ): ProfileCard {
        val distance = distanceString(currentUser.location, user.location, maxDistanceKm, forMatches)
        val percentage = MatchScoring.calculatePercentage(
            interests = user.interests,
            age = user.age,
            lookingFor = user.lookingFor,
            sensoryPreferences = user.sensoryPreferences,
            shareSensoryPreferences = user.shareSensoryPreferences,
            lifestyle = user.lifestyle,
            shareLifestyle = user.shareLifestyle,
            idealFirstDate = user.idealFirstDate,
            shareIdealFirstDate = user.shareIdealFirstDate,
            currentUser = currentUser,
        )
        return ProfileCard(
            userId = user.firebaseDocumentId ?: user.id,
            name = user.name,
            age = user.age,
            bio = user.bio,
            interests = user.interests,
            distance = distance,
            imageURL = user.profilePhotoURL ?: user.photos.firstOrNull(),
            gender = user.gender,
            interestedIn = user.interestedIn,
            lookingFor = user.lookingFor,
            photos = user.photos,
            lifestyle = user.lifestyle,
            shareLifestyle = user.shareLifestyle,
            bodyType = user.bodyType,
            shareBodyType = user.shareBodyType,
            preferredAgeMin = user.preferredAgeMin,
            preferredAgeMax = user.preferredAgeMax,
            isOnline = user.isOnline,
            lastSeen = user.lastSeen,
            idealFirstDate = user.idealFirstDate,
            shareIdealFirstDate = user.shareIdealFirstDate,
            sensoryPreferences = user.sensoryPreferences,
            shareSensoryPreferences = user.shareSensoryPreferences,
            energyLevel = user.energyLevel,
            matchPercentage = percentage,
            planTier = user.planTier,
        )
    }

    private fun distanceString(
        from: UserLocation,
        to: UserLocation,
        maxDistanceKm: Double?,
        forMatches: Boolean = false,
    ): String {
        val lat1 = from.latitude
        val lon1 = from.longitude
        val lat2 = to.latitude
        val lon2 = to.longitude
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return if (maxDistanceKm == null) {
                if (forMatches) "1 km away" else "Nearby"
            } else {
                "—"
            }
        }
        val km = DiscoverEligibility.haversineKm(lat1, lon1, lat2, lon2)
        return DiscoverEligibility.formatDistanceKm(km, forMatches = forMatches)
    }

    private fun effectivePassCount(passDocs: List<Map<String, Any>>): Int {
        val maxStored = passDocs.mapNotNull { (it["passCount"] as? Number)?.toInt() }.maxOrNull() ?: 0
        return maxOf(maxStored, passDocs.size)
    }

    private fun parseDiscoverUser(doc: DocumentSnapshot, excludingUserId: String?): SpectRoomUser? {
        if (doc.id == excludingUserId) return null
        val data = doc.data ?: return null
        if (data["isDeleted"] == true || data["isBanned"] == true) return null
        val freezeUntil = (data["freezeUntil"] as? Number)?.toDouble()
        if (freezeUntil != null && freezeUntil > System.currentTimeMillis() / 1000.0) return null
        // Incomplete profiles are not shown in discover.
        val status = data[UserProfileStatus.FIRESTORE_KEY] as? String
        if (status == UserProfileStatus.INCOMPLETE.rawValue) return null
        return doc.toSpectRoomUser(doc.id)
    }

    suspend fun deleteAllSwipes() {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val snapshot = db.collection("users").document(uid).collection("swipes").get().await()
        deleteFirestoreDocuments(db, snapshot.documents)
    }

    suspend fun deleteAllMatches() {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val snapshot = db.collection("users").document(uid).collection("matches").get().await()
        for (document in snapshot.documents) {
            val data = document.data ?: continue
            val user1Id = data["user1Id"] as? String
            val user2Id = data["user2Id"] as? String
            val batch = db.batch()
            batch.delete(document.reference)
            if (user1Id != null && user2Id != null) {
                val otherUserId = if (user1Id == uid) user2Id else user1Id
                val otherRef = db.collection("users").document(otherUserId)
                    .collection("matches")
                    .document(document.id)
                batch.delete(otherRef)
            }
            batch.commit().await()
        }
    }

    suspend fun deleteAllUserInteractions(messageRepository: MessageRepository) {
        deleteAllSwipes()
        deleteAllMatches()
        messageRepository.deleteAllMessages()
    }
}

class MatchRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val discoverRepository: DiscoverRepository = DiscoverRepository(),
) {
    suspend fun fetchMatches(currentUser: SpectRoomUser): List<MatchDisplay> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("users").document(uid).collection("matches")
            .orderBy("matchedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val processed = mutableSetOf<String>()
        val matches = mutableListOf<MatchDisplay>()
        for (document in snapshot.documents) {
            val data = document.data ?: continue
            val user1Id = data["user1Id"] as? String ?: continue
            val user2Id = data["user2Id"] as? String ?: continue
            val matchedAt = (data["matchedAt"] as? Number)?.toDouble() ?: continue
            val otherUserId = if (user1Id == uid) user2Id else user1Id
            if (!processed.add(otherUserId)) continue
            val otherDoc = db.collection("users").document(otherUserId).get().await()
            val otherUser = otherDoc.toSpectRoomUser(otherUserId)
            matches += MatchDisplay(
                id = document.id,
                otherUser = discoverRepository.toProfileCard(
                    otherUser,
                    currentUser,
                    maxDistanceKm = null,
                    forMatches = true,
                ),
                matchedAt = Date((matchedAt * 1000).toLong()),
                lastMessageAt = null,
            )
        }
        return matches
    }

    fun observeMatchCount(): Flow<Int> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid).collection("matches")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { registration.remove() }
    }

    fun observeRecentMatches(currentUserProvider: () -> SpectRoomUser?): Flow<MatchDisplay> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid).collection("matches")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val currentUser = currentUserProvider() ?: return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) continue
                    val data = change.document.data
                    val user1Id = data["user1Id"] as? String ?: continue
                    val user2Id = data["user2Id"] as? String ?: continue
                    val matchedAt = (data["matchedAt"] as? Number)?.toDouble() ?: continue
                    val ageSeconds = (System.currentTimeMillis() / 1000.0) - matchedAt
                    if (ageSeconds > 30.0) continue
                    val otherUserId = if (user1Id == uid) user2Id else user1Id
                    val matchId = change.document.id
                    launch {
                        runCatching {
                            val otherDoc = db.collection("users").document(otherUserId).get().await()
                            val otherUser = otherDoc.toSpectRoomUser(otherUserId)
                            MatchDisplay(
                                id = matchId,
                                otherUser = discoverRepository.toProfileCard(
                                    otherUser,
                                    currentUser,
                                    maxDistanceKm = null,
                                    forMatches = true,
                                ),
                                matchedAt = Date((matchedAt * 1000).toLong()),
                                lastMessageAt = null,
                            )
                        }.onSuccess { trySend(it) }
                    }
                }
            }
        awaitClose { registration.remove() }
    }
}

class MessageRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeIncomingMessageEvents(): Flow<IncomingMessageHint?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid).collection("incomingMessages")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // iOS parity: ignore the initial cache snapshot of existing history.
                if (snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val data = change.document.data ?: continue
                    val senderId = data["senderId"] as? String
                    if (senderId.isNullOrBlank() || senderId == uid || AdminSender.isAdmin(senderId)) continue
                    val createdAtMs = firestoreCreatedAtMillis(data) ?: continue
                    val ageMs = now - createdAtMs
                    if (ageMs > 30_000L || ageMs < -5_000L) continue
                    trySend(
                        IncomingMessageHint(
                            messageDocId = change.document.id,
                            senderId = senderId,
                            preview = data["content"] as? String ?: "",
                            type = data["type"] as? String ?: "text",
                        ),
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun persistAdminWarningMessage(
        message: String,
        noticeId: String?,
        createdAtSeconds: Double?,
    ) {
        val uid = auth.currentUser?.uid ?: return
        val trimmed = message.trim()
        if (trimmed.isBlank()) return
        val createdAt = createdAtSeconds?.takeIf { it > 0 } ?: (Date().time / 1000.0)
        val documentId = noticeId?.takeIf { it.isNotBlank() && !it.contains('/') }
            ?: "admin-warning-${(createdAt * 1000).toLong()}"
        val ref = db.collection("users").document(uid).collection("incomingMessages").document(documentId)
        if (ref.get().await().exists()) return
        ref.set(
            mapOf(
                "id" to documentId,
                "senderId" to AdminSender.ID,
                "recipientId" to uid,
                "content" to trimmed,
                "type" to "text",
                "createdAt" to createdAt,
                "isRead" to false,
                "readAt" to null,
                "fromAdmin" to true,
            ),
        ).await()
    }

    suspend fun markMessagesAsRead(fromUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        val incoming = db.collection("users").document(uid).collection("incomingMessages")
        val snapshot = incoming.whereEqualTo("senderId", fromUserId).get().await()
        val unread = snapshot.documents.filter { (it.getBoolean("isRead") == false) }
        if (unread.isEmpty()) return
        val batch = db.batch()
        val readAt = Date().time / 1000.0
        unread.forEach { doc ->
            batch.update(doc.reference, mapOf("isRead" to true, "readAt" to readAt))
        }
        batch.commit().await()
    }

    suspend fun resolveSenderProfile(senderId: String): Pair<String, String?> {
        if (AdminSender.isAdmin(senderId)) return AdminSender.DISPLAY_NAME to null
        val doc = db.collection("users").document(senderId).get().await()
        return (doc.getString("name") ?: "Match") to doc.getString("profilePhotoURL")
    }

    suspend fun sendTextMessage(recipientId: String, content: String): String =
        sendMessage(recipientId = recipientId, content = content.trim(), type = "text")

    suspend fun sendImageMessage(recipientId: String, imageUrl: String): String =
        sendMessage(recipientId = recipientId, content = imageUrl, type = "image")

    suspend fun sendVoiceMessage(recipientId: String, audioUrl: String, durationSeconds: Double): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val messageId = UUID.randomUUID().toString()
        val payload = hashMapOf<String, Any?>(
            "id" to messageId,
            "senderId" to uid,
            "recipientId" to recipientId,
            "content" to audioUrl,
            "type" to "voice",
            "voiceDuration" to durationSeconds,
            "createdAt" to Date().time / 1000.0,
            "isRead" to false,
            "readAt" to null,
        )
        db.collection("users").document(uid).collection("sentMessages").document(messageId)
            .set(payload).await()
        db.collection("users").document(recipientId).collection("incomingMessages").document(messageId)
            .set(payload).await()
        return messageId
    }

    private suspend fun sendMessage(recipientId: String, content: String, type: String): String {
        val normalizedRecipientId = recipientId.trim()
        require(normalizedRecipientId.isNotEmpty()) { "Invalid recipient" }
        require(!AdminSender.isAdmin(normalizedRecipientId)) { "Cannot reply to SpectRoom" }
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val messageId = UUID.randomUUID().toString()
        val payload = hashMapOf<String, Any?>(
            "id" to messageId,
            "senderId" to uid,
            "recipientId" to normalizedRecipientId,
            "content" to content,
            "type" to type,
            "createdAt" to Date().time / 1000.0,
            "isRead" to false,
            "readAt" to null,
        )
        db.collection("users").document(uid).collection("sentMessages").document(messageId)
            .set(payload).await()
        db.collection("users").document(normalizedRecipientId).collection("incomingMessages").document(messageId)
            .set(payload).await()
        return messageId
    }

    suspend fun fetchConversationMessages(otherUserId: String): List<MessageDisplay> =
        fetchConversationMessagesPage(otherUserId, limit = 40).messages

    suspend fun fetchConversationMessagesPage(
        otherUserId: String,
        limit: Int = 40,
        before: Date? = null,
    ): ConversationMessagesPage {
        val uid = auth.currentUser?.uid ?: return ConversationMessagesPage(emptyList(), false)
        val isAdminThread = AdminSender.isAdmin(otherUserId)
        val sent = if (isAdminThread) {
            emptyList()
        } else {
            fetchFilteredMessageDocuments(
                collection = "sentMessages",
                filterField = "recipientId",
                filterValue = otherUserId,
                userId = uid,
                limit = limit,
                before = before,
            )
        }
        val incoming = fetchFilteredMessageDocuments(
            collection = "incomingMessages",
            filterField = "senderId",
            filterValue = otherUserId,
            userId = uid,
            limit = limit,
            before = before,
        )

        val meName = auth.currentUser?.displayName ?: "You"
        val otherName: String
        val otherPhoto: String?
        val myPhoto: String?
        if (isAdminThread) {
            otherName = AdminSender.DISPLAY_NAME
            otherPhoto = null
            myPhoto = db.collection("users").document(uid).get().await().getString("profilePhotoURL")
        } else {
            val otherDoc = db.collection("users").document(otherUserId).get().await()
            otherName = otherDoc.getString("name") ?: "Match"
            otherPhoto = otherDoc.getString("profilePhotoURL")
            myPhoto = db.collection("users").document(uid).get().await().getString("profilePhotoURL")
        }

        val merged = mutableListOf<MessageDisplay>()
        for (doc in sent) {
            messageDisplayFrom(
                data = doc.data,
                docId = doc.id,
                uid = uid,
                meName = meName,
                otherName = otherName,
                otherPhoto = otherPhoto,
                myPhoto = myPhoto,
            )?.let(merged::add)
        }
        for (doc in incoming) {
            messageDisplayFrom(
                data = doc.data,
                docId = doc.id,
                uid = uid,
                meName = meName,
                otherName = otherName,
                otherPhoto = otherPhoto,
                myPhoto = myPhoto,
            )?.let(merged::add)
        }

        val newestFirst = merged
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt.time }
        val page = newestFirst.take(limit).sortedBy { it.createdAt.time }
        val hasMoreOlder = newestFirst.size > limit || sent.size == limit || incoming.size == limit
        return ConversationMessagesPage(messages = page, hasMoreOlder = hasMoreOlder)
    }

    suspend fun fetchConversationMessagesNewerThan(
        otherUserId: String,
        since: Date,
    ): List<MessageDisplay> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val sent = fetchFilteredMessageDocumentsAfter(
            collection = "sentMessages",
            filterField = "recipientId",
            filterValue = otherUserId,
            userId = uid,
            after = since,
        )
        val incoming = fetchFilteredMessageDocumentsAfter(
            collection = "incomingMessages",
            filterField = "senderId",
            filterValue = otherUserId,
            userId = uid,
            after = since,
        )

        val meName = auth.currentUser?.displayName ?: "You"
        val otherDoc = db.collection("users").document(otherUserId).get().await()
        val otherName = otherDoc.getString("name") ?: "Match"
        val otherPhoto = otherDoc.getString("profilePhotoURL")
        val myPhoto = db.collection("users").document(uid).get().await().getString("profilePhotoURL")

        val merged = mutableListOf<MessageDisplay>()
        for (doc in sent + incoming) {
            messageDisplayFrom(
                data = doc.data,
                docId = doc.id,
                uid = uid,
                meName = meName,
                otherName = otherName,
                otherPhoto = otherPhoto,
                myPhoto = myPhoto,
            )?.let(merged::add)
        }
        return merged.sortedBy { it.createdAt.time }
    }

    suspend fun findConversationPartnerIds(matching: String): Set<String> {
        val trimmed = matching.trim()
        if (trimmed.isEmpty()) return emptySet()
        val uid = auth.currentUser?.uid ?: return emptySet()
        val sent = db.collection("users").document(uid).collection("sentMessages").get().await()
        val incoming = db.collection("users").document(uid).collection("incomingMessages").get().await()
        val ids = mutableSetOf<String>()
        val needle = trimmed.lowercase()
        for (doc in sent.documents) {
            val data = doc.data ?: continue
            val content = (data["content"] as? String)?.lowercase().orEmpty()
            if (!content.contains(needle)) continue
            (data["recipientId"] as? String)?.let(ids::add)
        }
        for (doc in incoming.documents) {
            val data = doc.data ?: continue
            val content = (data["content"] as? String)?.lowercase().orEmpty()
            if (!content.contains(needle)) continue
            (data["senderId"] as? String)?.let(ids::add)
        }
        return ids
    }

    private suspend fun fetchFilteredMessageDocuments(
        collection: String,
        filterField: String,
        filterValue: String,
        userId: String,
        limit: Int,
        before: Date?,
    ): List<DocumentSnapshot> {
        return try {
            fetchFilteredMessageDocumentsQuery(
                collection = collection,
                filterField = filterField,
                filterValue = filterValue,
                userId = userId,
                limit = limit,
                before = before,
            )
        } catch (_: Exception) {
            val scanLimit = (limit * 8).coerceAtLeast(100)
            val all = db.collection("users").document(userId).collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(scanLimit.toLong())
                .get()
                .await()
                .documents
            all.filter { doc ->
                val data = doc.data ?: return@filter false
                if (data[filterField] != filterValue) return@filter false
                if (before == null) return@filter true
                val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: return@filter false
                createdAt < before.time / 1000.0
            }.take(limit)
        }
    }

    private suspend fun fetchFilteredMessageDocumentsQuery(
        collection: String,
        filterField: String,
        filterValue: String,
        userId: String,
        limit: Int,
        before: Date?,
    ): List<DocumentSnapshot> {
        var query: Query = db.collection("users").document(userId).collection(collection)
            .whereEqualTo(filterField, filterValue)
        query = if (before != null) {
            query
                .whereLessThan("createdAt", before.time / 1000.0)
                .orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            query.orderBy("createdAt", Query.Direction.DESCENDING)
        }
        return query.limit(limit.toLong()).get().await().documents
    }

    private suspend fun fetchFilteredMessageDocumentsAfter(
        collection: String,
        filterField: String,
        filterValue: String,
        userId: String,
        after: Date,
    ): List<DocumentSnapshot> {
        return try {
            fetchFilteredMessageDocumentsAfterQuery(
                collection = collection,
                filterField = filterField,
                filterValue = filterValue,
                userId = userId,
                after = after,
            )
        } catch (_: Exception) {
            val sinceSeconds = after.time / 1000.0
            val all = db.collection("users").document(userId).collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()
                .documents
            all.filter { doc ->
                val data = doc.data ?: return@filter false
                if (data[filterField] != filterValue) return@filter false
                val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: return@filter false
                createdAt > sinceSeconds
            }
        }
    }

    private suspend fun fetchFilteredMessageDocumentsAfterQuery(
        collection: String,
        filterField: String,
        filterValue: String,
        userId: String,
        after: Date,
    ): List<DocumentSnapshot> {
        return db.collection("users").document(userId).collection(collection)
            .whereEqualTo(filterField, filterValue)
            .whereGreaterThan("createdAt", (after.time / 1000.0) - 0.001)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
    }

    private fun messageDisplayFrom(
        data: Map<String, Any>?,
        docId: String,
        uid: String,
        meName: String,
        otherName: String,
        otherPhoto: String?,
        myPhoto: String?,
    ): MessageDisplay? {
        if (data == null) return null
        val senderId = data["senderId"] as? String ?: return null
        val recipientId = data["recipientId"] as? String ?: return null
        val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: 0.0
        val isOutgoing = senderId == uid
        return MessageDisplay(
            id = data["id"] as? String ?: docId,
            content = data["content"] as? String ?: "",
            messageType = MessageType.fromRaw(data["type"] as? String ?: "text"),
            senderName = when {
                isOutgoing -> meName
                AdminSender.isAdmin(senderId) -> AdminSender.DISPLAY_NAME
                else -> otherName
            },
            recipientName = if (isOutgoing) otherName else meName,
            senderId = senderId,
            recipientId = recipientId,
            createdAt = Date((createdAt * 1000).toLong()),
            isRead = data["isRead"] as? Boolean ?: false,
            senderProfilePhotoURL = if (isOutgoing) myPhoto else otherPhoto,
            recipientProfilePhotoURL = if (isOutgoing) otherPhoto else myPhoto,
            voiceDurationSeconds = (data["voiceDuration"] as? Number)?.toDouble(),
        )
    }

    suspend fun fetchConversationList(currentUser: SpectRoomUser): List<ConversationDisplay> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val sent = db.collection("users").document(uid).collection("sentMessages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
        val incoming = db.collection("users").document(uid).collection("incomingMessages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()

        data class Latest(
            val partnerId: String,
            val content: String,
            val createdAt: Double,
            val unread: Boolean,
        )

        val latestByPartner = linkedMapOf<String, Latest>()
        fun consider(partnerId: String, content: String, createdAt: Double, unread: Boolean) {
            if (partnerId.isBlank()) return
            val existing = latestByPartner[partnerId]
            if (existing == null || existing.createdAt < createdAt) {
                latestByPartner[partnerId] = Latest(partnerId, content, createdAt, unread)
            } else if (unread) {
                latestByPartner[partnerId] = existing.copy(unread = true)
            }
        }

        for (doc in sent.documents) {
            val data = doc.data ?: continue
            val partnerId = data["recipientId"] as? String ?: continue
            val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: 0.0
            consider(partnerId, data["content"] as? String ?: "", createdAt, unread = false)
        }
        for (doc in incoming.documents) {
            val data = doc.data ?: continue
            val partnerId = data["senderId"] as? String ?: continue
            val createdAt = (data["createdAt"] as? Number)?.toDouble() ?: 0.0
            val unread = !(data["isRead"] as? Boolean ?: false)
            consider(partnerId, data["content"] as? String ?: "", createdAt, unread)
        }

        val unreadBySender = fetchUnreadCountsBySender()
        return latestByPartner.values.sortedByDescending { it.createdAt }.map { latest ->
            val messageDate = Date((latest.createdAt * 1000).toLong())
            val preview = when {
                latest.content.contains("voice_messages") -> "🎙️ Voice message"
                latest.content.startsWith("http") && latest.content.contains("message_images") -> "📷 Photo"
                latest.content.startsWith("http") && latest.content.contains("firebasestorage") -> "📷 Photo"
                else -> latest.content.ifBlank { "No messages yet" }
            }
            if (AdminSender.isAdmin(latest.partnerId)) {
                ConversationDisplay(
                    id = latest.partnerId,
                    otherUserId = latest.partnerId,
                    otherUserName = AdminSender.DISPLAY_NAME,
                    otherUserPhotoURL = null,
                    lastMessage = preview,
                    lastMessageAt = messageDate,
                    timestamp = "",
                    isOnline = false,
                    unreadCount = unreadBySender[latest.partnerId] ?: 0,
                )
            } else {
                val other = db.collection("users").document(latest.partnerId).get().await()
                ConversationDisplay(
                    id = latest.partnerId,
                    otherUserId = latest.partnerId,
                    otherUserName = other.getString("name") ?: "Match",
                    otherUserPhotoURL = other.getString("profilePhotoURL"),
                    lastMessage = preview,
                    lastMessageAt = messageDate,
                    timestamp = "",
                    isOnline = other.getBoolean("isOnline") ?: false,
                    unreadCount = unreadBySender[latest.partnerId] ?: 0,
                )
            }
        }
    }

    suspend fun fetchUnreadCountsBySender(): Map<String, Int> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        val snapshot = db.collection("users").document(uid).collection("incomingMessages")
            .get()
            .await()
        val counts = mutableMapOf<String, Int>()
        for (document in snapshot.documents) {
            val data = document.data ?: continue
            if (data["isRead"] as? Boolean == true) continue
            val senderId = data["senderId"] as? String ?: continue
            counts[senderId] = (counts[senderId] ?: 0) + 1
        }
        return counts
    }

    suspend fun countUnreadIncomingMessages(): Int =
        fetchUnreadCountsBySender().values.sum()

    suspend fun deleteAllMessages() {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val incoming = db.collection("users").document(uid).collection("incomingMessages").get().await()
        val sent = db.collection("users").document(uid).collection("sentMessages").get().await()
        deleteFirestoreDocuments(db, incoming.documents + sent.documents)
    }
}

class ModerationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun isBlockedBy(otherUserId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid)
            .collection("blockedBy").document(otherUserId)
            .get()
            .await()
        return doc.exists()
    }

    suspend fun blockUser(userId: String, name: String) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val blockedAt = Date().time / 1000.0
        val data = hashMapOf(
            "name" to name,
            "blockedAt" to blockedAt,
        )
        db.collection("users").document(uid).collection("blockedUsers").document(userId)
            .set(data).await()
        val blockerName = auth.currentUser?.displayName ?: "Someone"
        db.collection("users").document(userId).collection("blockedBy").document(uid)
            .set(
                hashMapOf(
                    "name" to blockerName,
                    "blockedAt" to blockedAt,
                ),
            ).await()
    }

    suspend fun unblockUser(userId: String) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        db.collection("users").document(uid).collection("blockedUsers").document(userId)
            .delete().await()
        db.collection("users").document(userId).collection("blockedBy").document(uid)
            .delete().await()
    }

    suspend fun fetchBlockedUsers(): List<com.example.spectroom.data.model.BlockedUser> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("users").document(uid).collection("blockedUsers").get().await()
        return snapshot.documents.mapNotNull { doc ->
            parseBlockedUser(doc.id, doc.data.orEmpty())
        }
    }

    suspend fun fetchProfilePhotoUrl(userId: String): String? {
        val doc = db.collection("users").document(userId).get().await()
        return doc.getString("profilePhotoURL")?.takeIf { it.isNotBlank() }
    }

    suspend fun fetchBlockedByUsers(): List<com.example.spectroom.data.model.BlockedUser> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("users").document(uid).collection("blockedBy").get().await()
        return snapshot.documents.mapNotNull { doc ->
            parseBlockedUser(doc.id, doc.data.orEmpty())
        }
    }

    private fun parseBlockedUser(
        id: String,
        data: Map<String, Any?>,
    ): com.example.spectroom.data.model.BlockedUser? {
        return com.example.spectroom.data.model.BlockedUser(
            id = id,
            name = data["name"] as? String ?: "User",
            blockedAt = (data["blockedAt"] as? Number)?.toDouble()?.let { Date((it * 1000).toLong()) },
        )
    }

    suspend fun submitReport(
        reportedUserId: String,
        reportedName: String,
        category: String,
        details: String?,
        bio: String = "",
        photoUrls: List<String> = emptyList(),
        recentMessages: List<MessageDisplay> = emptyList(),
    ) {
        val reporterId = auth.currentUser?.uid ?: error("Not signed in")
        val reportRef = db.collection("reports").document()
        val messageSnapshots = recentMessages.takeLast(10).map { message ->
            buildMap<String, Any> {
                put("message_id", message.id)
                put("sender_id", message.senderId)
                put("recipient_id", message.recipientId)
                put("content", message.content)
                put("type", message.messageType.rawValue)
                put("created_at", message.createdAt.time / 1000.0)
                message.voiceDurationSeconds?.let { put("voice_duration", it) }
            }
        }
        val data = hashMapOf<String, Any?>(
            "report_id" to reportRef.id,
            "reporter_user_id" to reporterId,
            "reported_user_id" to reportedUserId,
            "category" to category,
            "details" to details?.trim()?.takeIf { it.isNotEmpty() },
            "status" to "pending",
            "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "context_snapshot" to mapOf(
                "name" to reportedName,
                "bio" to bio,
                "profile_photo_urls" to photoUrls,
                "messages" to messageSnapshots,
            ),
        )
        reportRef.set(data).await()
    }

    suspend fun fetchMyReports(): List<com.example.spectroom.data.model.UserReport> {
        val reporterId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("reports")
            .whereEqualTo("reporter_user_id", reporterId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            parseUserReport(doc.id, doc.data.orEmpty())
        }.sortedByDescending { it.createdAt?.time ?: 0L }
    }

    suspend fun submitSupportTicket(
        topicLabel: String,
        message: String,
        userName: String,
        userEmail: String,
    ): String {
        val uid = auth.currentUser?.uid ?: "unknown"
        val ticketId = makeSupportTicketId()
        val data = hashMapOf<String, Any>(
            "ticketId" to ticketId,
            "userId" to uid,
            "userName" to userName,
            "userEmail" to userEmail,
            "topic" to topicLabel,
            "message" to message,
            "status" to "new",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        )
        db.collection("supportMessages").add(data).await()
        return ticketId
    }

    companion object {
        val reportCategories = listOf(
            "Inappropriate Content",
            "Fake profile or Spam",
            "Harassment or Bullying",
            "Something else",
        )

        private fun makeSupportTicketId(): String {
            val datePart = java.text.SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
            val randomPart = String.format(Locale.US, "%05d", (0..99_999).random())
            return "$datePart-$randomPart"
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseUserReport(
            documentId: String,
            data: Map<String, Any?>,
        ): com.example.spectroom.data.model.UserReport? {
            val reporterUserId = data["reporter_user_id"] as? String ?: return null
            val reportedUserId = data["reported_user_id"] as? String ?: return null
            val category = data["category"] as? String ?: return null
            val snapshot = data["context_snapshot"] as? Map<String, Any?> ?: emptyMap()
            val name = (snapshot["name"] as? String)?.trim().orEmpty()
            val createdAt = when (val raw = data["created_at"]) {
                is com.google.firebase.Timestamp -> raw.toDate()
                is Number -> Date((raw.toDouble() * 1000).toLong())
                else -> null
            }
            return com.example.spectroom.data.model.UserReport(
                id = data["report_id"] as? String ?: documentId,
                reporterUserId = reporterUserId,
                reportedUserId = reportedUserId,
                reportedName = name.ifBlank { "Unknown" },
                category = category,
                details = data["details"] as? String,
                status = data["status"] as? String ?: "pending",
                createdAt = createdAt,
                bio = snapshot["bio"] as? String ?: "",
                profilePhotoUrls = snapshot["profile_photo_urls"] as? List<String> ?: emptyList(),
            )
        }
    }
}
