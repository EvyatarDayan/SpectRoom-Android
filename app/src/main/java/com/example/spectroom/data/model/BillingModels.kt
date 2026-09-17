package com.example.spectroom.data.model

import androidx.annotation.StringRes
import com.example.spectroom.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Unified billing price display: always `"9.99 USD"` (dot decimal, USD code). Matches iOS. */
object BillingCurrency {
    const val CODE = "USD"

    private val amountFormatter = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).apply {
        isGroupingUsed = true
        groupingSize = 3
    }

    fun format(amount: BigDecimal, currencyCode: String = CODE): String {
        val number = amountFormatter.format(amount)
        val resolved = currencyCode.trim().uppercase(Locale.US).ifEmpty { CODE }
        return "$number $resolved"
    }
}

enum class BillingInterval(
    @param:StringRes val titleRes: Int,
    /** Lowercase adjective used in CTAs (“Change to yearly Pro”). */
    @param:StringRes val adjectiveRes: Int,
    /** Firestore / cross-platform raw value (`monthly` / `yearly`). */
    val rawValue: String,
) {
    MONTHLY(
        titleRes = R.string.billing_interval_monthly,
        adjectiveRes = R.string.billing_interval_monthly_adj,
        rawValue = "monthly",
    ),
    YEARLY(
        titleRes = R.string.billing_interval_yearly,
        adjectiveRes = R.string.billing_interval_yearly_adj,
        rawValue = "yearly",
    );

    companion object {
        fun fromRaw(raw: String?): BillingInterval {
            if (raw.isNullOrBlank()) return MONTHLY
            return entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: MONTHLY
        }
    }
}

enum class PlanTier(
    @param:StringRes val displayNameRes: Int,
    @param:StringRes val profilePlanLabelRes: Int,
    @param:StringRes val usageSummaryRes: Int,
    val dailyDiscoverSwipeLimit: Int,
    /** Daily Discover swipe undos. `null` means unlimited (Premium). */
    val dailyDiscoverUndoLimit: Int?,
    val showsFullMatchBreakdown: Boolean,
    /** Days between profile boosts. `null` means boost is not available (Free). */
    val profileBoostCooldownDays: Int?,
    val dailyMediaMessageLimit: Int,
    /** Max chat video file size in bytes (picked original + upload). */
    val maxVideoUploadBytes: Int,
    val profilePlanSymbol: String,
    val featureResIds: List<Int>,
) {
    FREE(
        displayNameRes = R.string.billing_plan_free,
        profilePlanLabelRes = R.string.profile_plan_free,
        usageSummaryRes = R.string.billing_usage_free,
        dailyDiscoverSwipeLimit = 10,
        dailyDiscoverUndoLimit = 3,
        showsFullMatchBreakdown = false,
        profileBoostCooldownDays = null,
        dailyMediaMessageLimit = 5,
        maxVideoUploadBytes = 10 * 1024 * 1024,
        profilePlanSymbol = "",
        featureResIds = listOf(
            R.string.billing_feature_swipes_10,
            R.string.billing_feature_undos_3,
            R.string.billing_feature_match_basic,
            R.string.billing_feature_boost_none,
            R.string.billing_feature_media_5,
            R.string.billing_feature_video_10,
        ),
    ),
    PRO(
        displayNameRes = R.string.billing_plan_pro,
        profilePlanLabelRes = R.string.profile_plan_pro,
        usageSummaryRes = R.string.billing_usage_pro,
        dailyDiscoverSwipeLimit = 15,
        dailyDiscoverUndoLimit = 6,
        showsFullMatchBreakdown = true,
        profileBoostCooldownDays = 14,
        dailyMediaMessageLimit = 20,
        maxVideoUploadBytes = 20 * 1024 * 1024,
        profilePlanSymbol = "⭐️",
        featureResIds = listOf(
            R.string.billing_feature_swipes_15,
            R.string.billing_feature_undos_6,
            R.string.billing_feature_match_full,
            R.string.billing_feature_boost_14,
            R.string.billing_feature_media_20,
            R.string.billing_feature_video_20,
        ),
    ),
    PREMIUM(
        displayNameRes = R.string.billing_plan_premium,
        profilePlanLabelRes = R.string.profile_plan_premium,
        usageSummaryRes = R.string.billing_usage_premium,
        dailyDiscoverSwipeLimit = 20,
        dailyDiscoverUndoLimit = null,
        showsFullMatchBreakdown = true,
        profileBoostCooldownDays = 7,
        dailyMediaMessageLimit = 50,
        maxVideoUploadBytes = 40 * 1024 * 1024,
        profilePlanSymbol = "👑",
        featureResIds = listOf(
            R.string.billing_feature_swipes_20,
            R.string.billing_feature_undos_unlimited,
            R.string.billing_feature_match_full,
            R.string.billing_feature_boost_7,
            R.string.billing_feature_media_50,
            R.string.billing_feature_video_40,
        ),
    );

    val maxVideoUploadMegabytes: Int
        get() = maxVideoUploadBytes / (1024 * 1024)

    /** Icon next to plan titles in Account & Billing (Free sprout, Pro star, Premium crown). */
    val titleStars: String
        get() = when (this) {
            FREE -> "🌱"
            PRO, PREMIUM -> profilePlanSymbol
        }

    val rawValue: String
        get() = name.lowercase()

    companion object {
        fun fromRaw(raw: String?): PlanTier {
            if (raw.isNullOrBlank()) return FREE
            return entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) } ?: FREE
        }
    }
}

enum class SubscriptionStatus(
    @param:StringRes val titleRes: Int,
    /** Firestore / cross-platform raw value. */
    val rawValue: String,
) {
    ACTIVE(R.string.billing_status_active, "active"),
    RENEWING(R.string.billing_status_renewing, "renewing"),
    CANCELLED(R.string.billing_status_cancelled, "cancelled"),
    PAST_DUE(R.string.billing_status_past_due, "pastDue"),
    NONE(R.string.billing_status_none, "none"),
    ;

    companion object {
        fun fromRaw(raw: String?): SubscriptionStatus? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }
}

data class SubscriptionPlan(
    val id: String,
    val tier: PlanTier,
    val name: String,
    val monthlyPrice: BigDecimal,
    val yearlyPrice: BigDecimal,
    val currencyCode: String,
) {
    fun price(forInterval: BillingInterval): BigDecimal =
        if (forInterval == BillingInterval.YEARLY) yearlyPrice else monthlyPrice

    fun formattedPrice(forInterval: BillingInterval): String =
        BillingCurrency.format(price(forInterval), currencyCode)
}

data class CurrentSubscription(
    val planId: String,
    val tier: PlanTier,
    val status: SubscriptionStatus,
    val interval: BillingInterval,
    val renewsOnMillis: Long? = null,
    val cancelledAtMillis: Long? = null,
)

enum class PaymentMethodType {
    CARD,
    APPLE_PAY,
    GOOGLE_PAY,
}

enum class CardBrand(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("Amex"),
    DISCOVER("Discover"),
    OTHER("Card"),
}

data class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val brand: CardBrand? = null,
    val last4: String? = null,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val isDefault: Boolean = false,
    val walletEnabled: Boolean = false,
) {
    val maskedNumber: String
        get() = if (last4.isNullOrEmpty()) "••••" else "•••• •••• •••• $last4"

    val expirationDisplay: String?
        get() {
            val month = expMonth ?: return null
            val year = expYear ?: return null
            return String.format(Locale.US, "%02d/%02d", month, year % 100)
        }
}

data class NewCardDraft(
    val cardholderName: String = "",
    val cardNumber: String = "",
    val expMonth: String = "",
    val expYear: String = "",
    val cvc: String = "",
    val postalCode: String = "",
)

enum class AccountHistoryEventKind(
    val rawValue: String,
    @param:StringRes val titleRes: Int,
) {
    NEW_PLAN("newPlan", R.string.billing_history_event_new_plan),
    CHANGE_PLAN("changePlan", R.string.billing_history_event_change_plan),
    CANCEL_PLAN("cancelPlan", R.string.billing_history_event_cancel_plan),
    AUTOMATIC_BILLING("automaticBilling", R.string.billing_history_event_automatic_billing),
    ;

    companion object {
        fun fromRaw(raw: String?): AccountHistoryEventKind? =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }
    }
}

enum class InvoiceStatus(
    val rawValue: String,
    @param:StringRes val titleRes: Int,
) {
    PAID("paid", R.string.billing_invoice_status_paid),
    REFUNDED("refunded", R.string.billing_invoice_status_refunded),
    PENDING("pending", R.string.billing_invoice_status_pending),
    ;

    companion object {
        fun fromRaw(raw: String?): InvoiceStatus? =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }
    }
}

/** Plan / billing activity shown under Account History (matches iOS `AccountHistoryEvent`). */
data class AccountHistoryEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val kind: AccountHistoryEventKind,
    val planTier: PlanTier? = null,
    val interval: BillingInterval? = null,
    val amount: BigDecimal? = null,
    val currencyCode: String = "USD",
    val status: InvoiceStatus? = null,
) {
    val showsAmount: Boolean
        get() = kind == AccountHistoryEventKind.AUTOMATIC_BILLING && amount != null

    fun planLabel(context: android.content.Context): String {
        val tier = planTier ?: PlanTier.FREE
        val tierName = context.getString(tier.displayNameRes)
        val billingInterval = interval
        return if (billingInterval != null && tier != PlanTier.FREE) {
            "${context.getString(billingInterval.titleRes)} $tierName"
        } else {
            tierName
        }
    }

    fun displayTitle(context: android.content.Context): String =
        context.getString(kind.titleRes, planLabel(context))

    fun formattedDateTime(): String = dateTimeFormatter.format(java.util.Date(dateMillis))

    fun formattedAmount(): String {
        val value = amount ?: return "—"
        return BillingCurrency.format(value, currencyCode)
    }

    companion object {
        private val dateTimeFormatter =
            java.text.SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK)
    }
}

/** Migration alias — older call sites referred to invoices. */
typealias Invoice = AccountHistoryEvent

data class BillingSnapshot(
    val subscription: CurrentSubscription,
    val availablePlans: List<SubscriptionPlan>,
    val paymentMethods: List<PaymentMethod>,
    val accountHistory: List<AccountHistoryEvent>,
    val applePayEnabled: Boolean,
    val googlePayEnabled: Boolean,
) {
    /** Backward-compatible alias used by older view code. */
    val invoices: List<AccountHistoryEvent>
        get() = accountHistory
}

/** Firestore billing fields observed live across devices. */
data class RemoteBillingPlan(
    val tier: PlanTier,
    val interval: BillingInterval?,
    val status: SubscriptionStatus? = null,
)

sealed class BillingServiceException(message: String) : Exception(message) {
    data object PlanNotFound : BillingServiceException("plan_not_found")
    data object PaymentMethodNotFound : BillingServiceException("payment_not_found")
    data object InvalidCard : BillingServiceException("invalid_card")
    data object NothingToCancel : BillingServiceException("nothing_to_cancel")
}
