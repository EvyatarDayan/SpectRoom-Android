package com.example.spectroom.data.remote

import android.util.Log
import com.example.spectroom.data.model.AccountHistoryEvent
import com.example.spectroom.data.model.AccountHistoryEventKind
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.InvoiceStatus
import com.example.spectroom.data.model.PlanTier
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal

/**
 * Persists account history events under `users/{uid}/accountHistory/{eventId}`.
 * Local prefs remain a cache; Firestore is source of truth when signed in.
 */
object AccountHistoryCloudSync {
    private const val TAG = "AccountHistoryCloud"
    private const val COLLECTION = "accountHistory"

    suspend fun pushEvent(event: AccountHistoryEvent): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "Skip push: no signed-in Firebase user for event=${event.id}")
            return false
        }
        val ref = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(COLLECTION)
            .document(event.id)
        return try {
            ref.set(firestoreData(event), com.google.firebase.firestore.SetOptions.merge()).await()
            Log.i(TAG, "Pushed account history event id=${event.id} kind=${event.kind.rawValue} uid=$uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync account history event id=${event.id}", e)
            false
        }
    }

    /** Uploads any local-only events (e.g. created offline / before cloud sync existed). */
    suspend fun pushMissingEvents(events: List<AccountHistoryEvent>) {
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        val remoteIds = fetchEvents().map { it.id }.toSet()
        for (event in events) {
            if (event.id !in remoteIds) {
                pushEvent(event)
            }
        }
    }

    suspend fun fetchEvents(): List<AccountHistoryEvent> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Skip fetch: no signed-in Firebase user")
            return emptyList()
        }
        val collection = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(COLLECTION)
        return try {
            val snapshot = collection
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val events = snapshot.documents.mapNotNull { doc ->
                parseEvent(id = doc.id, data = doc.data ?: return@mapNotNull null)
            }
            Log.i(TAG, "Fetched ${events.size} account history events for uid=$uid")
            events
        } catch (orderedError: Exception) {
            Log.w(TAG, "Ordered account history fetch failed; retrying without orderBy", orderedError)
            try {
                val snapshot = collection.get().await()
                val events = snapshot.documents.mapNotNull { doc ->
                    parseEvent(id = doc.id, data = doc.data ?: return@mapNotNull null)
                }.sortedByDescending { it.dateMillis }
                Log.i(TAG, "Fetched ${events.size} account history events (unordered) for uid=$uid")
                events
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch account history for uid=$uid", e)
                emptyList()
            }
        }
    }

    fun firestoreData(event: AccountHistoryEvent): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "kind" to event.kind.rawValue,
            "date" to Timestamp(event.dateMillis / 1000, ((event.dateMillis % 1000) * 1_000_000).toInt()),
            "currencyCode" to event.currencyCode,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        event.planTier?.let { data["planTier"] = it.rawValue }
        event.interval?.let { data["billingInterval"] = it.rawValue }
        event.amount?.let { data["amount"] = it.toPlainString() }
        event.status?.let { data["status"] = it.rawValue }
        return data
    }

    fun parseEvent(id: String, data: Map<String, Any?>): AccountHistoryEvent? {
        val kind = AccountHistoryEventKind.fromRaw(data["kind"] as? String) ?: return null
        val dateMillis = when (val raw = data["date"]) {
            is Timestamp -> raw.toDate().time
            is Number -> {
                val value = raw.toDouble()
                // Seconds vs millis heuristic (Firestore TimeInterval / epoch millis).
                if (value < 1e12) (value * 1000).toLong() else value.toLong()
            }
            else -> System.currentTimeMillis()
        }
        val planTier = (data["planTier"] as? String)?.let(PlanTier::fromRaw)
        val interval = (data["billingInterval"] as? String)?.let(BillingInterval::fromRaw)
        val currencyCode = (data["currencyCode"] as? String) ?: "USD"
        val status = InvoiceStatus.fromRaw(data["status"] as? String)
        val amount = when (val raw = data["amount"]) {
            is String -> raw.toBigDecimalOrNull()
            is Number -> BigDecimal(raw.toString())
            else -> null
        }
        return AccountHistoryEvent(
            id = id,
            dateMillis = dateMillis,
            kind = kind,
            planTier = planTier,
            interval = interval,
            amount = amount,
            currencyCode = currencyCode,
            status = status,
        )
    }

    /** Merge remote + local by event id (remote wins on conflict). */
    fun merge(
        local: List<AccountHistoryEvent>,
        remote: List<AccountHistoryEvent>,
    ): List<AccountHistoryEvent> {
        val byId = LinkedHashMap<String, AccountHistoryEvent>()
        for (event in local) byId[event.id] = event
        for (event in remote) byId[event.id] = event
        return byId.values.sortedByDescending { it.dateMillis }
    }
}
