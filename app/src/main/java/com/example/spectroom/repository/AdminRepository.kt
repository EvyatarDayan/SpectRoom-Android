package com.example.spectroom.repository

import com.example.spectroom.data.model.AdminAccess
import com.example.spectroom.data.model.AdminUserProfile
import com.example.spectroom.data.model.ModerationFreezeDuration
import com.example.spectroom.data.model.ReportModerationAction
import com.example.spectroom.data.model.UserReport
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

class AdminRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun isCurrentUserAdmin(): Boolean = AdminAccess.isAdmin(auth.currentUser?.email)

    private fun requireAdmin() {
        check(isCurrentUserAdmin()) { "Not an admin" }
    }

    suspend fun fetchAllReports(): List<UserReport> {
        requireAdmin()
        val snapshot = db.collection("reports").get().await()
        return snapshot.documents.mapNotNull { doc ->
            parseUserReport(doc.id, doc.data.orEmpty())
        }.sortedByDescending { it.createdAt?.time ?: 0L }
    }

    suspend fun fetchAdminUsers(limit: Int = 60, search: String = ""): List<AdminUserProfile> {
        requireAdmin()
        val snapshot = db.collection("users")
            .orderBy(com.google.firebase.firestore.FieldPath.documentId())
            .limit(120)
            .get()
            .await()
        val query = search.trim().lowercase()
        return snapshot.documents.mapNotNull { doc ->
            parseAdminUser(doc.id, doc.data.orEmpty())
        }.filter { profile ->
            if (query.isEmpty()) true
            else profile.name.lowercase().contains(query) ||
                profile.email.lowercase().contains(query)
        }.take(limit)
    }

    suspend fun dismissReport(reportId: String) {
        requireAdmin()
        applyReportModeration(reportId, "dismissed", ReportModerationAction.DISMISS)
    }

    suspend fun issueWarning(
        userId: String,
        message: String,
        reportId: String? = null,
    ) {
        requireAdmin()
        val trimmed = message.trim().ifEmpty { DEFAULT_WARNING }
        val resolvedReportId = reportId?.takeIf { it.isNotBlank() } ?: "admin-warning-$userId-${Date().time}"
        val noticeId = "$resolvedReportId-${Date().time / 1000}"
        db.collection("users").document(userId).set(
            mapOf(
                "pendingSystemWarning" to mapOf(
                    "message" to trimmed,
                    "report_id" to resolvedReportId,
                    "notice_id" to noticeId,
                    "created_at" to Date().time / 1000.0,
                    "acknowledged" to false,
                ),
            ),
            SetOptions.merge(),
        ).await()
        if (reportId != null && reportId.isNotBlank() && !reportId.startsWith("admin-")) {
            applyReportModeration(reportId, "actioned", ReportModerationAction.WARNING)
        }
    }

    suspend fun freezeUser(
        userId: String,
        duration: ModerationFreezeDuration,
        reportId: String? = null,
    ) {
        requireAdmin()
        val freezeUntil = Date(System.currentTimeMillis() + duration.hours * 3_600_000L)
        val resolvedReportId = reportId?.takeIf { it.isNotBlank() }
            ?: "admin-freeze-$userId-${Date().time / 1000}"
        db.collection("users").document(userId).set(
            mapOf(
                "discoverFrozenUntil" to Timestamp(freezeUntil),
                "discoverFreezeHours" to duration.hours,
                "discoverFrozenReportId" to resolvedReportId,
                "pendingAccountUnfreeze" to FieldValue.delete(),
                "pendingAccountFreeze" to mapOf(
                    "report_id" to resolvedReportId,
                    "kind" to "new",
                    "freeze_until" to freezeUntil.time / 1000.0,
                    "hours" to duration.hours,
                    "created_at" to Date().time / 1000.0,
                    "acknowledged" to false,
                    "message" to "Your account has been temporarily frozen for ${duration.hours} hours.",
                ),
            ),
            SetOptions.merge(),
        ).await()
        if (reportId != null &&
            reportId.isNotBlank() &&
            !reportId.startsWith("admin-freeze-") &&
            !reportId.startsWith("admin-extend-")
        ) {
            applyReportModeration(
                reportId,
                "actioned",
                ReportModerationAction.FREEZE,
                extra = mapOf(
                    "freeze_hours" to duration.hours,
                    "freeze_until" to Timestamp(freezeUntil),
                ),
            )
        }
    }

    suspend fun banUser(userId: String, reportId: String? = null) {
        requireAdmin()
        val adminUserId = auth.currentUser?.uid ?: error("Not signed in")
        val resolvedReportId = reportId?.trim()?.takeIf { it.isNotEmpty() }
        val payload = hashMapOf<String, Any?>(
            "isBanned" to true,
            "bannedAt" to FieldValue.serverTimestamp(),
            "isOnline" to false,
            "discoverFrozenUntil" to FieldValue.delete(),
            "discoverFreezeHours" to FieldValue.delete(),
            "discoverFrozenReportId" to FieldValue.delete(),
            "pendingAccountFreeze" to FieldValue.delete(),
            "pendingAccountUnfreeze" to FieldValue.delete(),
            "pendingAccountUnban" to FieldValue.delete(),
            "lastBanLiftedAt" to FieldValue.delete(),
            "pendingAccountBan" to mapOf(
                "notice_id" to "ban-$userId-${Date().time / 1000}",
                "created_at" to Date().time / 1000.0,
                "acknowledged" to false,
                "banned_by" to adminUserId,
                "message" to "Your SpectRoom account has been permanently banned.",
            ),
            "bannedReportId" to (resolvedReportId ?: FieldValue.delete()),
        )
        db.collection("users").document(userId).set(payload, SetOptions.merge()).await()
        if (resolvedReportId != null &&
            !resolvedReportId.startsWith("admin-freeze-") &&
            !resolvedReportId.startsWith("admin-extend-") &&
            !resolvedReportId.startsWith("admin-ban-")
        ) {
            applyReportModeration(resolvedReportId, "actioned", ReportModerationAction.BAN)
        }
    }

    suspend fun removeAccountBan(userId: String) {
        requireAdmin()
        val adminUserId = auth.currentUser?.uid ?: error("Not signed in")
        val snapshot = db.collection("users").document(userId).get().await()
        val previousBannedAt = snapshot.getTimestamp("bannedAt")
        val previousReportId = snapshot.getString("bannedReportId")
        val payload = hashMapOf<String, Any?>(
            "isBanned" to FieldValue.delete(),
            "bannedAt" to FieldValue.delete(),
            "bannedReportId" to FieldValue.delete(),
            "pendingAccountBan" to FieldValue.delete(),
            "lastBanLiftedAt" to FieldValue.serverTimestamp(),
            "lastBannedAt" to (previousBannedAt ?: FieldValue.delete()),
            "lastBannedReportId" to (previousReportId ?: FieldValue.delete()),
            "pendingAccountUnban" to mapOf(
                "notice_id" to "unban-$userId-${Date().time / 1000}",
                "created_at" to Date().time / 1000.0,
                "acknowledged" to false,
                "lifted_by" to adminUserId,
                "message" to "Your SpectRoom account ban has been lifted. You can log in again.",
            ),
        )
        db.collection("users").document(userId).set(payload, SetOptions.merge()).await()
    }

    suspend fun removeAccountFreeze(userId: String) {
        requireAdmin()
        val payload = hashMapOf<String, Any?>(
            "discoverFrozenUntil" to FieldValue.delete(),
            "discoverFreezeHours" to FieldValue.delete(),
            "discoverFrozenReportId" to FieldValue.delete(),
            "pendingAccountFreeze" to FieldValue.delete(),
            "pendingAccountUnfreeze" to mapOf(
                "notice_id" to "unfreeze-$userId-${Date().time / 1000}",
                "created_at" to Date().time / 1000.0,
                "message" to "Your SpectRoom account freeze has been lifted. You can log in again.",
            ),
        )
        db.collection("users").document(userId).set(payload, SetOptions.merge()).await()
    }

    private suspend fun applyReportModeration(
        reportId: String,
        status: String,
        action: ReportModerationAction,
        extra: Map<String, Any> = emptyMap(),
    ) {
        val adminId = auth.currentUser?.uid ?: error("Not signed in")
        val data = hashMapOf<String, Any>(
            "status" to status,
            "moderation_action" to action.rawValue,
            "moderated_at" to FieldValue.serverTimestamp(),
            "moderated_by" to adminId,
        )
        data.putAll(extra)
        db.collection("reports").document(reportId).set(data, SetOptions.merge()).await()
    }

    companion object {
        const val DEFAULT_WARNING =
            "Your account was recently reported for content or behavior that violates our guidelines. Please keep interactions respectful to ensure SpectRoom remains safe for everyone. Repeated violations may result in account suspension."
    }
}

@Suppress("UNCHECKED_CAST")
internal fun parseUserReport(
    documentId: String,
    data: Map<String, Any?>,
): UserReport? {
    val reporterUserId = data["reporter_user_id"] as? String ?: return null
    val reportedUserId = data["reported_user_id"] as? String ?: return null
    val category = data["category"] as? String ?: return null
    val snapshot = data["context_snapshot"] as? Map<String, Any?> ?: emptyMap()
    val name = (snapshot["name"] as? String)?.trim().orEmpty()
    val createdAt = when (val raw = data["created_at"]) {
        is Timestamp -> raw.toDate()
        is Number -> {
            val n = raw.toDouble()
            Date(if (n > 10_000_000_000) n.toLong() else (n * 1000).toLong())
        }
        else -> null
    }
    return UserReport(
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

@Suppress("UNCHECKED_CAST")
private fun parseAdminUser(id: String, data: Map<String, Any?>): AdminUserProfile? {
    val name = (data["name"] as? String)?.trim().orEmpty()
    val email = (
        data["email"] as? String
            ?: data["userEmail"] as? String
            ?: ""
        ).trim()
    if (name.isEmpty() && email.isEmpty()) return null
    val photos = data["photos"] as? List<String> ?: emptyList()
    val photo = (data["profilePhotoURL"] as? String)?.takeIf { it.isNotBlank() }
        ?: photos.firstOrNull { it.isNotBlank() }
    val frozenUntil = when (val raw = data["discoverFrozenUntil"]) {
        is Timestamp -> raw.toDate()
        is Number -> {
            val n = raw.toDouble()
            Date(if (n > 10_000_000_000) n.toLong() else (n * 1000).toLong())
        }
        else -> null
    }
    return AdminUserProfile(
        id = id,
        name = name.ifBlank { "Unknown" },
        age = (data["age"] as? Number)?.toInt(),
        email = email,
        bio = data["bio"] as? String ?: "",
        photoUrl = photo,
        isOnline = data["isOnline"] as? Boolean == true,
        isBanned = data["isBanned"] as? Boolean == true,
        isFrozen = frozenUntil != null && frozenUntil.after(Date()),
        frozenUntil = frozenUntil,
        isDeleted = data["isDeleted"] as? Boolean == true,
    )
}
