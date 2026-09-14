package com.example.spectroom.data.remote

import android.content.Context
import android.util.Log
import com.example.spectroom.SpectRoomApplication
import com.example.spectroom.data.local.PlanEntitlementsStore
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.BillingServiceException
import com.example.spectroom.data.model.BillingSnapshot
import com.example.spectroom.data.model.CardBrand
import com.example.spectroom.data.model.CurrentSubscription
import com.example.spectroom.data.model.Invoice
import com.example.spectroom.data.model.InvoiceStatus
import com.example.spectroom.data.model.NewCardDraft
import com.example.spectroom.data.model.PaymentMethod
import com.example.spectroom.data.model.PaymentMethodType
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.SubscriptionPlan
import com.example.spectroom.data.model.SubscriptionStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

interface BillingServicing {
    suspend fun fetchBillingSnapshot(): BillingSnapshot
    suspend fun changePlan(planId: String, interval: BillingInterval): CurrentSubscription
    suspend fun cancelSubscription(): CurrentSubscription
    suspend fun setDefaultPaymentMethod(id: String): List<PaymentMethod>
    suspend fun deletePaymentMethod(id: String): List<PaymentMethod>
    suspend fun addCard(draft: NewCardDraft): List<PaymentMethod>
    suspend fun toggleApplePay(enabled: Boolean): Boolean
    suspend fun toggleGooglePay(enabled: Boolean): Boolean
}

/**
 * Best-effort branded plan-change emails (same Cloud Function as iOS).
 * Uses an app-scoped scope so UI recomposition / cancellation cannot abort the send
 * (mirrors iOS `Task { await PlanChangeEmailNotifier... }`).
 */
object PlanChangeEmailNotifier {
    private const val TAG = "PlanChangeEmail"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun notifyChangedAsync(previous: CurrentSubscription, next: CurrentSubscription) {
        scope.launch {
            send(
                hashMapOf<String, Any>(
                    "changeType" to "changed",
                    "fromPlan" to previous.tier.emailPlanName,
                    "fromInterval" to previous.tier.emailIntervalOrEmpty(previous.interval),
                    "toPlan" to next.tier.emailPlanName,
                    "toInterval" to next.tier.emailIntervalOrEmpty(next.interval),
                ),
            )
        }
    }

    fun notifyCancelledAsync(subscription: CurrentSubscription) {
        scope.launch {
            val payload = hashMapOf<String, Any>(
                "changeType" to "cancelled",
                "fromPlan" to subscription.tier.emailPlanName,
                "fromInterval" to subscription.interval.emailIntervalName,
                "toPlan" to "Free",
                "toInterval" to "",
            )
            subscription.renewsOnMillis?.let { millis ->
                payload["accessUntil"] = SimpleDateFormat("dd/MM/yyyy", Locale.UK)
                    .format(Date(millis))
            }
            send(payload)
        }
    }

    private suspend fun send(data: HashMap<String, Any>) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "Skip plan-change email: no signed-in Firebase user")
            return
        }
        try {
            // Same pattern as MFA branded email — refresh token then call us-central1.
            user.getIdToken(true).await()
            Log.i(
                TAG,
                "Calling sendSpectRoomPlanChangeEmail uid=${user.uid} email=${user.email} data=$data",
            )
            FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("sendSpectRoomPlanChangeEmail")
                .call(data)
                .await()
            Log.i(TAG, "Plan change email requested OK")
        } catch (e: Exception) {
            Log.e(TAG, "Plan change email failed", e)
        }
    }

    private val PlanTier.emailPlanName: String
        get() = when (this) {
            PlanTier.FREE -> "Free"
            PlanTier.PRO -> "Pro"
            PlanTier.PREMIUM -> "Premium"
        }

    private val BillingInterval.emailIntervalName: String
        get() = when (this) {
            BillingInterval.MONTHLY -> "monthly"
            BillingInterval.YEARLY -> "yearly"
        }

    private fun PlanTier.emailIntervalOrEmpty(interval: BillingInterval): String =
        if (this == PlanTier.FREE) "" else interval.emailIntervalName
}

/** Local mock billing backend until Play Billing / Stripe is wired. */
class MockBillingService private constructor(
    context: Context,
) : BillingServicing {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val planEntitlementsStore = PlanEntitlementsStore(appContext)
    private var snapshot: BillingSnapshot = makeInitialSnapshot()
    private var onPlanChanged: ((PlanTier) -> Unit)? = null

    init {
        // Never downgrade a higher local entitlement with a Free mock snapshot (iOS: local billing wins).
        val existing = planEntitlementsStore.currentPlanTier
        val restored = snapshot.subscription.tier
        val resolved = if (existing.ordinal > restored.ordinal) existing else restored
        if (resolved != restored) {
            val aligned = subscriptionForTier(resolved)
            snapshot = snapshot.copy(subscription = aligned)
            persistSubscription(aligned)
        }
        planEntitlementsStore.syncPlanTier(resolved)
    }

    fun setOnPlanChangedListener(listener: ((PlanTier) -> Unit)?) {
        onPlanChanged = listener
    }

    /**
     * Align mock subscription + local entitlements to [tier] (persisted).
     * Used after plan changes and when resolving the active plan on login.
     */
    fun alignWithPlanTier(tier: PlanTier) {
        if (snapshot.subscription.tier == tier) {
            planEntitlementsStore.syncPlanTier(tier)
            return
        }
        val aligned = subscriptionForTier(
            tier = tier,
            interval = snapshot.subscription.interval,
            preferExistingRenewal = snapshot.subscription.takeIf { it.tier == tier },
        )
        snapshot = snapshot.copy(subscription = aligned)
        persistSubscription(aligned)
        planEntitlementsStore.syncPlanTier(tier)
    }

    override suspend fun fetchBillingSnapshot(): BillingSnapshot {
        delay(DELAY_MS)
        // Entitlements are updated from Firestore (including iOS plan changes). Mirror them here.
        alignWithPlanTier(planEntitlementsStore.currentPlanTier)
        return snapshot
    }

    override suspend fun changePlan(planId: String, interval: BillingInterval): CurrentSubscription {
        delay(DELAY_MS)
        val plan = snapshot.availablePlans.firstOrNull { it.id == planId }
            ?: throw BillingServiceException.PlanNotFound
        val previous = snapshot.subscription
        val calendar = Calendar.getInstance()
        calendar.add(
            if (interval == BillingInterval.YEARLY) Calendar.YEAR else Calendar.MONTH,
            1,
        )
        val subscription = CurrentSubscription(
            planId = plan.id,
            tier = plan.tier,
            status = if (plan.tier == PlanTier.FREE) {
                SubscriptionStatus.NONE
            } else {
                SubscriptionStatus.RENEWING
            },
            interval = interval,
            renewsOnMillis = if (plan.tier == PlanTier.FREE) null else calendar.timeInMillis,
            cancelledAtMillis = null,
        )
        snapshot = snapshot.copy(subscription = subscription)
        persistSubscription(subscription)
        planEntitlementsStore.syncPlanTier(plan.tier)

        val next = snapshot.subscription
        val changed =
            previous.tier != next.tier ||
                previous.interval != next.interval ||
                previous.status == SubscriptionStatus.CANCELLED
        // Fire-and-forget on an app-scoped job BEFORE notifying UI (iOS: Task { await ... }).
        // Awaiting here on the Compose scope gets cancelled when onPlanChanged recomposes.
        if (changed) {
            PlanChangeEmailNotifier.notifyChangedAsync(previous, next)
        }
        onPlanChanged?.invoke(plan.tier)
        return next
    }

    override suspend fun cancelSubscription(): CurrentSubscription {
        delay(DELAY_MS)
        if (snapshot.subscription.tier == PlanTier.FREE) {
            throw BillingServiceException.NothingToCancel
        }
        val current = snapshot.subscription
        val renewsOn = current.renewsOnMillis ?: Calendar.getInstance().run {
            add(
                if (current.interval == BillingInterval.YEARLY) Calendar.YEAR else Calendar.MONTH,
                1,
            )
            timeInMillis
        }
        val subscription = current.copy(
            status = SubscriptionStatus.CANCELLED,
            cancelledAtMillis = System.currentTimeMillis(),
            renewsOnMillis = renewsOn,
        )
        snapshot = snapshot.copy(subscription = subscription)
        persistSubscription(subscription)
        PlanChangeEmailNotifier.notifyCancelledAsync(subscription)
        // Cancelled paid plans keep remaining access; entitlements stay on current tier.
        return snapshot.subscription
    }

    override suspend fun setDefaultPaymentMethod(id: String): List<PaymentMethod> {
        delay(DELAY_MS / 2)
        if (snapshot.paymentMethods.none { it.id == id }) {
            throw BillingServiceException.PaymentMethodNotFound
        }
        snapshot = snapshot.copy(
            paymentMethods = snapshot.paymentMethods.map { it.copy(isDefault = it.id == id) },
        )
        return snapshot.paymentMethods
    }

    override suspend fun deletePaymentMethod(id: String): List<PaymentMethod> {
        delay(DELAY_MS / 2)
        val index = snapshot.paymentMethods.indexOfFirst { it.id == id }
        if (index < 0) throw BillingServiceException.PaymentMethodNotFound
        val wasDefault = snapshot.paymentMethods[index].isDefault
        val remaining = snapshot.paymentMethods.filterNot { it.id == id }.toMutableList()
        if (wasDefault && remaining.isNotEmpty()) {
            remaining[0] = remaining[0].copy(isDefault = true)
        }
        snapshot = snapshot.copy(paymentMethods = remaining)
        return snapshot.paymentMethods
    }

    override suspend fun addCard(draft: NewCardDraft): List<PaymentMethod> {
        delay(DELAY_MS)
        val digits = draft.cardNumber.filter { it.isDigit() }
        if (digits.length < 12) throw BillingServiceException.InvalidCard
        val month = draft.expMonth.toIntOrNull()
        if (month == null || month !in 1..12) throw BillingServiceException.InvalidCard
        val year = draft.expYear.toIntOrNull()
        if (year == null || year < 24) throw BillingServiceException.InvalidCard
        if (draft.cvc.filter { it.isDigit() }.length < 3) throw BillingServiceException.InvalidCard

        val brand = when {
            digits.startsWith("4") -> CardBrand.VISA
            digits.startsWith("5") -> CardBrand.MASTERCARD
            digits.startsWith("3") -> CardBrand.AMEX
            else -> CardBrand.OTHER
        }
        val makeDefault = snapshot.paymentMethods.isEmpty() ||
            draft.cardholderName.lowercase().contains("default")
        val method = PaymentMethod(
            id = UUID.randomUUID().toString(),
            type = PaymentMethodType.CARD,
            brand = brand,
            last4 = digits.takeLast(4),
            expMonth = month,
            expYear = if (year < 100) 2000 + year else year,
            isDefault = makeDefault,
        )
        val existing = if (makeDefault) {
            snapshot.paymentMethods.map { it.copy(isDefault = false) }
        } else {
            snapshot.paymentMethods
        }
        snapshot = snapshot.copy(paymentMethods = listOf(method) + existing)
        return snapshot.paymentMethods
    }

    override suspend fun toggleApplePay(enabled: Boolean): Boolean {
        delay(DELAY_MS / 3)
        snapshot = snapshot.copy(applePayEnabled = enabled)
        return snapshot.applePayEnabled
    }

    override suspend fun toggleGooglePay(enabled: Boolean): Boolean {
        delay(DELAY_MS / 3)
        snapshot = snapshot.copy(googlePayEnabled = enabled)
        return snapshot.googlePayEnabled
    }

    private fun makeInitialSnapshot(): BillingSnapshot {
        val plans = defaultPlans()
        val validPlanIds = plans.map { it.id }.toSet()
        val subscription = loadPersistedSubscription(validPlanIds)
            ?: subscriptionFromEntitlements(validPlanIds)
            ?: CurrentSubscription(
                planId = "plan_free",
                tier = PlanTier.FREE,
                status = SubscriptionStatus.NONE,
                interval = BillingInterval.MONTHLY,
            )

        return BillingSnapshot(
            subscription = subscription,
            availablePlans = plans,
            paymentMethods = listOf(
                PaymentMethod(
                    id = "pm_visa",
                    type = PaymentMethodType.CARD,
                    brand = CardBrand.VISA,
                    last4 = "4242",
                    expMonth = 8,
                    expYear = 2028,
                    isDefault = true,
                ),
                PaymentMethod(
                    id = "pm_mc",
                    type = PaymentMethodType.CARD,
                    brand = CardBrand.MASTERCARD,
                    last4 = "4444",
                    expMonth = 12,
                    expYear = 2027,
                    isDefault = false,
                ),
            ),
            invoices = defaultInvoices(),
            applePayEnabled = true,
            googlePayEnabled = false,
        )
    }

    private fun loadPersistedSubscription(validPlanIds: Set<String>): CurrentSubscription? {
        val planId = prefs.getString(KEY_PLAN_ID, null) ?: return null
        if (planId !in validPlanIds) return null
        val tier = PlanTier.fromRaw(prefs.getString(KEY_TIER, null))
        val status = runCatching {
            SubscriptionStatus.valueOf(prefs.getString(KEY_STATUS, null).orEmpty())
        }.getOrElse { return null }
        val interval = runCatching {
            BillingInterval.valueOf(prefs.getString(KEY_INTERVAL, null).orEmpty())
        }.getOrElse { BillingInterval.MONTHLY }
        val renewsOn = prefs.getLong(KEY_RENEWS_ON, -1L).takeIf { it > 0L }
        val cancelledAt = prefs.getLong(KEY_CANCELLED_AT, -1L).takeIf { it > 0L }
        return CurrentSubscription(
            planId = planId,
            tier = tier,
            status = status,
            interval = interval,
            renewsOnMillis = renewsOn,
            cancelledAtMillis = cancelledAt,
        )
    }

    private fun subscriptionFromEntitlements(validPlanIds: Set<String>): CurrentSubscription? {
        val tier = planEntitlementsStore.currentPlanTier
        if (tier == PlanTier.FREE) return null
        val restored = subscriptionForTier(tier)
        if (restored.planId !in validPlanIds) return null
        persistSubscription(restored)
        return restored
    }

    private fun subscriptionForTier(
        tier: PlanTier,
        interval: BillingInterval = BillingInterval.MONTHLY,
        preferExistingRenewal: CurrentSubscription? = null,
    ): CurrentSubscription {
        val planId = when (tier) {
            PlanTier.FREE -> "plan_free"
            PlanTier.PRO -> "plan_pro"
            PlanTier.PREMIUM -> "plan_premium"
        }
        if (tier == PlanTier.FREE) {
            return CurrentSubscription(
                planId = planId,
                tier = PlanTier.FREE,
                status = SubscriptionStatus.NONE,
                interval = interval,
            )
        }
        val renewsOn = preferExistingRenewal?.renewsOnMillis ?: Calendar.getInstance().run {
            add(
                if (interval == BillingInterval.YEARLY) Calendar.YEAR else Calendar.MONTH,
                1,
            )
            timeInMillis
        }
        val status = preferExistingRenewal?.status?.takeIf {
            it == SubscriptionStatus.RENEWING ||
                it == SubscriptionStatus.ACTIVE ||
                it == SubscriptionStatus.CANCELLED
        } ?: SubscriptionStatus.RENEWING
        return CurrentSubscription(
            planId = planId,
            tier = tier,
            status = status,
            interval = preferExistingRenewal?.interval ?: interval,
            renewsOnMillis = renewsOn,
            cancelledAtMillis = preferExistingRenewal?.cancelledAtMillis,
        )
    }

    private fun persistSubscription(subscription: CurrentSubscription) {
        // commit() so a plan change is durable before the next profile/billing refresh.
        prefs.edit()
            .putString(KEY_PLAN_ID, subscription.planId)
            .putString(KEY_TIER, subscription.tier.rawValue)
            .putString(KEY_STATUS, subscription.status.name)
            .putString(KEY_INTERVAL, subscription.interval.name)
            .apply {
                if (subscription.renewsOnMillis != null) {
                    putLong(KEY_RENEWS_ON, subscription.renewsOnMillis)
                } else {
                    remove(KEY_RENEWS_ON)
                }
                if (subscription.cancelledAtMillis != null) {
                    putLong(KEY_CANCELLED_AT, subscription.cancelledAtMillis)
                } else {
                    remove(KEY_CANCELLED_AT)
                }
            }
            .commit()
    }

    companion object {
        val shared: MockBillingService by lazy {
            MockBillingService(SpectRoomApplication.instance)
        }

        private const val DELAY_MS = 650L
        private const val PREFS_NAME = "spectroom_mock_billing"
        private const val KEY_PLAN_ID = "mockBillingPlanId"
        private const val KEY_TIER = "mockBillingTier"
        private const val KEY_STATUS = "mockBillingStatus"
        private const val KEY_INTERVAL = "mockBillingInterval"
        private const val KEY_RENEWS_ON = "mockBillingRenewsOn"
        private const val KEY_CANCELLED_AT = "mockBillingCancelledAt"

        private fun defaultPlans(): List<SubscriptionPlan> = listOf(
            SubscriptionPlan(
                id = "plan_free",
                tier = PlanTier.FREE,
                name = "Free",
                monthlyPrice = BigDecimal.ZERO,
                yearlyPrice = BigDecimal.ZERO,
                currencyCode = "USD",
            ),
            SubscriptionPlan(
                id = "plan_pro",
                tier = PlanTier.PRO,
                name = "Pro",
                monthlyPrice = BigDecimal("9.99"),
                yearlyPrice = BigDecimal("95.90"),
                currencyCode = "USD",
            ),
            SubscriptionPlan(
                id = "plan_premium",
                tier = PlanTier.PREMIUM,
                name = "Premium",
                monthlyPrice = BigDecimal("19.99"),
                yearlyPrice = BigDecimal("191.90"),
                currencyCode = "USD",
            ),
        )

        private fun defaultInvoices(): List<Invoice> {
            fun monthsAgo(n: Int): Long {
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -n)
                return c.timeInMillis
            }
            return listOf(
                Invoice(
                    id = "inv_1003",
                    dateMillis = monthsAgo(1),
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PAID,
                    pdfUrl = "https://example.com/invoices/inv_1003.pdf",
                ),
                Invoice(
                    id = "inv_1002",
                    dateMillis = monthsAgo(2),
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PAID,
                    pdfUrl = "https://example.com/invoices/inv_1002.pdf",
                ),
                Invoice(
                    id = "inv_1001",
                    dateMillis = monthsAgo(3),
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.REFUNDED,
                    pdfUrl = "https://example.com/invoices/inv_1001.pdf",
                ),
                Invoice(
                    id = "inv_1000",
                    dateMillis = monthsAgo(4),
                    description = "SpectRoom Premium — Monthly",
                    planTier = PlanTier.PREMIUM,
                    interval = BillingInterval.YEARLY,
                    amount = BigDecimal("19.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PAID,
                    pdfUrl = "https://example.com/invoices/inv_1000.pdf",
                ),
                Invoice(
                    id = "inv_0999",
                    dateMillis = monthsAgo(5),
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PENDING,
                    pdfUrl = null,
                ),
                Invoice(
                    id = "inv_0998",
                    dateMillis = monthsAgo(6),
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PAID,
                    pdfUrl = "https://example.com/invoices/inv_0998.pdf",
                ),
                Invoice(
                    id = "inv_old",
                    dateMillis = run {
                        val c = Calendar.getInstance()
                        c.add(Calendar.YEAR, -4)
                        c.timeInMillis
                    },
                    description = "SpectRoom Pro — Monthly",
                    planTier = PlanTier.PRO,
                    interval = BillingInterval.MONTHLY,
                    amount = BigDecimal("9.99"),
                    currencyCode = "USD",
                    status = InvoiceStatus.PAID,
                    pdfUrl = null,
                ),
            )
        }
    }
}
