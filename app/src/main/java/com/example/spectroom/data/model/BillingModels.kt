package com.example.spectroom.data.model

import androidx.annotation.StringRes
import com.example.spectroom.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class BillingInterval(
    @param:StringRes val titleRes: Int,
    /** Lowercase adjective used in CTAs (“Change to yearly Pro”). */
    @param:StringRes val adjectiveRes: Int,
) {
    MONTHLY(
        titleRes = R.string.billing_interval_monthly,
        adjectiveRes = R.string.billing_interval_monthly_adj,
    ),
    YEARLY(
        titleRes = R.string.billing_interval_yearly,
        adjectiveRes = R.string.billing_interval_yearly_adj,
    ),
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
        profilePlanSymbol = "",
        featureResIds = listOf(
            R.string.billing_feature_swipes_10,
            R.string.billing_feature_undos_3,
            R.string.billing_feature_match_basic,
            R.string.billing_feature_boost_none,
            R.string.billing_feature_media_5,
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
        profilePlanSymbol = "⭐️",
        featureResIds = listOf(
            R.string.billing_feature_swipes_15,
            R.string.billing_feature_undos_6,
            R.string.billing_feature_match_full,
            R.string.billing_feature_boost_14,
            R.string.billing_feature_media_20,
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
        profilePlanSymbol = "👑",
        featureResIds = listOf(
            R.string.billing_feature_swipes_20,
            R.string.billing_feature_undos_unlimited,
            R.string.billing_feature_match_full,
            R.string.billing_feature_boost_7,
            R.string.billing_feature_media_50,
        ),
    );

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
) {
    ACTIVE(R.string.billing_status_active),
    RENEWING(R.string.billing_status_renewing),
    CANCELLED(R.string.billing_status_cancelled),
    PAST_DUE(R.string.billing_status_past_due),
    NONE(R.string.billing_status_none),
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

    fun formattedPrice(forInterval: BillingInterval, locale: Locale = Locale.getDefault()): String {
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val amount = formatter.format(price(forInterval))
        return "$amount $currencyCode"
    }
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

enum class InvoiceStatus(
    @param:StringRes val titleRes: Int,
) {
    PAID(R.string.billing_invoice_status_paid),
    REFUNDED(R.string.billing_invoice_status_refunded),
    PENDING(R.string.billing_invoice_status_pending),
}

data class Invoice(
    val id: String,
    val dateMillis: Long,
    val description: String,
    val planTier: PlanTier? = null,
    val interval: BillingInterval = BillingInterval.MONTHLY,
    val amount: BigDecimal,
    val currencyCode: String,
    val status: InvoiceStatus,
    val pdfUrl: String? = null,
) {
    fun formattedAmount(locale: Locale = Locale.getDefault()): String {
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
        }
        return formatter.format(amount)
    }

    /** Plain title e.g. "Monthly pro plan" (matches iOS `displayTitle`). */
    fun displayTitle(intervalLabel: String, planWord: String): String {
        val planName = (planTier ?: PlanTier.FREE).rawValue
        return "$intervalLabel $planName $planWord"
    }
}

data class BillingSnapshot(
    val subscription: CurrentSubscription,
    val availablePlans: List<SubscriptionPlan>,
    val paymentMethods: List<PaymentMethod>,
    val invoices: List<Invoice>,
    val applePayEnabled: Boolean,
    val googlePayEnabled: Boolean,
)

sealed class BillingServiceException(message: String) : Exception(message) {
    data object PlanNotFound : BillingServiceException("plan_not_found")
    data object PaymentMethodNotFound : BillingServiceException("payment_not_found")
    data object InvalidCard : BillingServiceException("invalid_card")
    data object NothingToCancel : BillingServiceException("nothing_to_cancel")
}
