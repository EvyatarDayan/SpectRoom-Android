package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.spectroom.R
import com.example.spectroom.data.local.BillingHistoryExport
import com.example.spectroom.data.model.AccountHistoryEvent
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.BillingServiceException
import com.example.spectroom.data.model.BillingSnapshot
import com.example.spectroom.data.model.CurrentSubscription
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.SubscriptionPlan
import com.example.spectroom.data.model.SubscriptionStatus
import com.example.spectroom.data.remote.BillingServicing
import com.example.spectroom.data.remote.MockBillingService
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Matches iOS system `Color.green` (#34C759). */
private val BillingGreen = Color(0xFF34C759)

private data class PendingPlanChange(
    val plan: SubscriptionPlan,
    val interval: BillingInterval,
    val title: String,
    val message: String,
)

private class AccountBillingState(
    private val service: BillingServicing = MockBillingService.shared,
) {
    var isLoading by mutableStateOf(true)
    var loadErrorRes by mutableStateOf<Int?>(null)
    var loadErrorMessage by mutableStateOf<String?>(null)
    var toastMessage by mutableStateOf<String?>(null)
    var selectedInterval by mutableStateOf(BillingInterval.MONTHLY)
    var subscription by mutableStateOf<CurrentSubscription?>(null)
    var plans by mutableStateOf<List<SubscriptionPlan>>(emptyList())
    var accountHistory by mutableStateOf<List<AccountHistoryEvent>>(emptyList())
    var isBusy by mutableStateOf(false)
    var isExportingBillingHistory by mutableStateOf(false)
    var pendingPlanChange by mutableStateOf<PendingPlanChange?>(null)
    /** Shown after confirm alert dismisses — mirrors iOS `planChangeSuccessLabel`. */
    var planChangeSuccessLabel by mutableStateOf<String?>(null)
    var showingCancelConfirm by mutableStateOf(false)
    /** Shown after cancel confirm dismisses — mirrors iOS `cancelSuccessPlanLabel`. */
    var cancelSuccessPlanLabel by mutableStateOf<String?>(null)

    fun recentAccountHistory(): List<AccountHistoryEvent> =
        BillingHistoryExport.recentEvents(accountHistory)

    suspend fun load() {
        isLoading = true
        loadErrorRes = null
        loadErrorMessage = null
        try {
            apply(service.fetchBillingSnapshot())
        } catch (e: Exception) {
            loadErrorMessage = e.message
            loadErrorRes = R.string.common_retry
        }
        isLoading = false
    }

    /**
     * Call after capturing/clearing [pendingPlanChange] so the confirm alert dismisses
     * immediately, then the success alert is presented (iOS `performPlanChange`).
     */
    suspend fun performPlanChange(
        plan: SubscriptionPlan,
        interval: BillingInterval,
        successLabel: String,
        errorMessage: (Throwable) -> String,
        onPlanChanged: (PlanTier, BillingInterval) -> Unit = { _, _ -> },
    ) {
        isBusy = true
        try {
            subscription = service.changePlan(plan.id, interval)
            selectedInterval = subscription?.interval ?: interval
            syncAccountHistoryFromService()
            onPlanChanged(plan.tier, selectedInterval)
            planChangeSuccessLabel = successLabel
        } catch (e: Exception) {
            showToast(errorMessage(e))
        }
        isBusy = false
    }

    fun dismissPlanChangeDialog() {
        pendingPlanChange = null
        planChangeSuccessLabel = null
    }

    /**
     * Call after dismissing the cancel confirm alert (iOS `performCancelSubscription`).
     */
    suspend fun performCancelSubscription(
        planLabel: String,
        errorMessage: (Throwable) -> String,
    ) {
        isBusy = true
        try {
            subscription = service.cancelSubscription()
            syncAccountHistoryFromService()
            cancelSuccessPlanLabel = planLabel
        } catch (e: Exception) {
            showToast(errorMessage(e))
        }
        isBusy = false
    }

    private fun syncAccountHistoryFromService() {
        val billing = service as? MockBillingService ?: return
        accountHistory = billing.accountHistory.sortedByDescending { it.dateMillis }
    }

    fun isCurrent(plan: SubscriptionPlan): Boolean =
        subscription?.planId == plan.id || subscription?.tier == plan.tier

    /** Current paid plan that can still be cancelled (same billing interval selected). */
    fun canCancelCurrentPlan(plan: SubscriptionPlan): Boolean {
        if (!isCurrent(plan) || plan.tier == PlanTier.FREE) return false
        val current = subscription ?: return false
        if (current.status == SubscriptionStatus.CANCELLED) return false
        return current.interval == selectedInterval
    }

    /** Cancelled paid plan that can be renewed for its own billing interval. */
    fun canRenewCancelledPlan(plan: SubscriptionPlan): Boolean {
        if (!isCurrent(plan) || plan.tier == PlanTier.FREE) return false
        val current = subscription ?: return false
        if (current.status != SubscriptionStatus.CANCELLED) return false
        // Renew keeps the cancelled plan's interval; switching tabs is "Change to…".
        return current.interval == selectedInterval
    }

    /**
     * Same tier + interval as the live subscription (Free ignores interval).
     * Cancelled plans are never treated as exact current so renew stays tappable.
     */
    fun isExactCurrentSelection(plan: SubscriptionPlan): Boolean {
        if (!isCurrent(plan)) return false
        if (plan.tier == PlanTier.FREE) return true
        if (subscription?.status == SubscriptionStatus.CANCELLED) return false
        return subscription?.interval == selectedInterval
    }

    private fun apply(snap: BillingSnapshot) {
        subscription = snap.subscription
        plans = snap.availablePlans
        accountHistory = snap.accountHistory.sortedByDescending { it.dateMillis }
        selectedInterval = snap.subscription.interval
    }

    fun showToast(message: String, scope: kotlinx.coroutines.CoroutineScope? = null) {
        toastMessage = message
    }
}

@Composable
fun AccountBillingSheet(
    onDismiss: () -> Unit,
    onPlanChanged: (PlanTier, BillingInterval) -> Unit = { _, _ -> },
    onSubscriptionCancelled: (CurrentSubscription) -> Unit = {},
    accountEmail: String = "",
    seedPlanTier: PlanTier = PlanTier.FREE,
    seedBillingInterval: BillingInterval = BillingInterval.MONTHLY,
    seedSubscriptionStatus: SubscriptionStatus = SubscriptionStatus.NONE,
    onShareExportedPdf: (android.net.Uri) -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { AccountBillingState() }

    fun errorMessage(e: Throwable): String = when (e) {
        is BillingServiceException.PlanNotFound -> context.getString(R.string.billing_error_plan_not_found)
        is BillingServiceException.PaymentMethodNotFound -> context.getString(R.string.billing_error_payment_not_found)
        is BillingServiceException.InvalidCard -> context.getString(R.string.billing_error_invalid_card)
        is BillingServiceException.NothingToCancel -> context.getString(R.string.billing_error_nothing_to_cancel)
        else -> e.message ?: context.getString(R.string.billing_error_plan_not_found)
    }

    LaunchedEffect(Unit) {
        MockBillingService.shared.setOnPlanChangedListener(onPlanChanged)
        MockBillingService.shared.setOnSubscriptionCancelledListener(onSubscriptionCancelled)
    }

    LaunchedEffect(seedPlanTier, seedBillingInterval, seedSubscriptionStatus) {
        MockBillingService.shared.alignWithPlanTier(
            seedPlanTier,
            seedBillingInterval,
            seedSubscriptionStatus,
        )
        state.load()
    }

    LaunchedEffect(state.toastMessage) {
        val message = state.toastMessage ?: return@LaunchedEffect
        delay(2_400)
        if (state.toastMessage == message) {
            state.toastMessage = null
        }
    }

    SettingsFullScreenSheet(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsSheetColumn {
                BillingSheetHeader(onDismiss = onDismiss)

                when {
                    state.isLoading -> BillingSkeleton()
                    state.loadErrorMessage != null -> BillingErrorState(
                        message = state.loadErrorMessage.orEmpty(),
                        onRetry = { scope.launch { state.load() } },
                    )
                    else -> BillingContent(
                        state = state,
                        accountEmail = accountEmail,
                        onShareExportedPdf = onShareExportedPdf,
                        onPlanAction = { plan ->
                            when {
                                state.isBusy -> Unit
                                state.canCancelCurrentPlan(plan) -> {
                                    state.showingCancelConfirm = true
                                }
                                state.canRenewCancelledPlan(plan) ||
                                    !state.isExactCurrentSelection(plan) -> {
                                    state.planChangeSuccessLabel = null
                                    state.pendingPlanChange = PendingPlanChange(
                                        plan = plan,
                                        interval = state.selectedInterval,
                                        title = planChangeAlertTitleText(context, state, plan),
                                        message = planChangeAlertMessageText(context, state, plan),
                                    )
                                }
                            }
                        },
                        onIntervalChange = { state.selectedInterval = it },
                    )
                }
            }

            state.toastMessage?.let { toast ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = toast,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            state.pendingPlanChange?.let { pending ->
                PlanChangeConfirmOverlay(
                    title = pending.title,
                    message = pending.message,
                    onCancel = { state.dismissPlanChangeDialog() },
                    onConfirm = {
                        if (state.isBusy) return@PlanChangeConfirmOverlay
                        val successLabel = planChangeTargetLabelForInterval(
                            context,
                            pending.plan,
                            pending.interval,
                        )
                        state.isBusy = true
                        state.pendingPlanChange = null
                        scope.launch {
                            state.performPlanChange(
                                plan = pending.plan,
                                interval = pending.interval,
                                successLabel = successLabel,
                                errorMessage = ::errorMessage,
                                onPlanChanged = onPlanChanged,
                            )
                        }
                    },
                )
            }

            state.planChangeSuccessLabel?.let { label ->
                PlanChangeSuccessOverlay(
                    planLabel = label,
                    onDismiss = { state.planChangeSuccessLabel = null },
                )
            }
        }
    }

    if (state.showingCancelConfirm) {
        val planName = stringResource(
            state.subscription?.tier?.displayNameRes ?: R.string.billing_plan_pro,
        )
        AlertDialog(
            onDismissRequest = { state.showingCancelConfirm = false },
            title = {
                Text(stringResource(R.string.billing_cancel_confirm_title, planName))
            },
            text = { Text(stringResource(R.string.billing_cancel_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.isBusy) return@TextButton
                        // Dismiss confirm immediately, then run cancel + success alert (iOS).
                        state.isBusy = true
                        state.showingCancelConfirm = false
                        scope.launch {
                            state.performCancelSubscription(
                                planLabel = planName,
                                errorMessage = ::errorMessage,
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.billing_cancel_confirm_yes), color = LikeRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showingCancelConfirm = false }) {
                    Text(stringResource(R.string.billing_cancel_confirm_no))
                }
            },
        )
    }

    state.cancelSuccessPlanLabel?.let { label ->
        AlertDialog(
            onDismissRequest = { state.cancelSuccessPlanLabel = null },
            title = { Text(stringResource(R.string.billing_cancel_success_title)) },
            text = {
                Text(stringResource(R.string.billing_cancel_success_message, label))
            },
            confirmButton = {
                TextButton(onClick = { state.cancelSuccessPlanLabel = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

@Composable
private fun planChangeTargetLabel(state: AccountBillingState, plan: SubscriptionPlan): String {
    val planName = stringResource(plan.tier.displayNameRes)
    if (plan.tier == PlanTier.FREE) return planName
    val adjective = stringResource(state.selectedInterval.adjectiveRes)
    return "$adjective $planName"
}

@Composable
private fun planActionTitle(state: AccountBillingState, plan: SubscriptionPlan): String {
    val current = state.subscription
        ?: return stringResource(
            R.string.billing_action_change_to,
            planChangeTargetLabel(state, plan),
        )
    if (state.isCurrent(plan)) {
        if (plan.tier != PlanTier.FREE && current.status == SubscriptionStatus.CANCELLED) {
            return if (current.interval == state.selectedInterval) {
                stringResource(
                    R.string.billing_action_renew_interval,
                    stringResource(current.interval.adjectiveRes),
                )
            } else {
                stringResource(
                    R.string.billing_action_change_to,
                    planChangeTargetLabel(state, plan),
                )
            }
        }
        if (state.canCancelCurrentPlan(plan)) {
            return stringResource(R.string.billing_action_cancel_plan)
        }
        if (
            plan.tier != PlanTier.FREE &&
            current.status != SubscriptionStatus.CANCELLED &&
            current.interval != state.selectedInterval
        ) {
            return stringResource(
                R.string.billing_action_change_to,
                planChangeTargetLabel(state, plan),
            )
        }
        return stringResource(R.string.billing_action_current)
    }
    return stringResource(
        R.string.billing_action_change_to,
        planChangeTargetLabel(state, plan),
    )
}

private fun planChangeTargetLabelText(
    context: android.content.Context,
    state: AccountBillingState,
    plan: SubscriptionPlan,
): String = planChangeTargetLabelForInterval(context, plan, state.selectedInterval)

private fun planChangeTargetLabelForInterval(
    context: android.content.Context,
    plan: SubscriptionPlan,
    interval: BillingInterval,
): String {
    val planName = context.getString(plan.tier.displayNameRes)
    if (plan.tier == PlanTier.FREE) return planName
    val adjective = context.getString(interval.adjectiveRes)
    return "$adjective $planName"
}

private fun currentPlanLabelText(
    context: android.content.Context,
    state: AccountBillingState,
): String {
    val current = state.subscription
        ?: return context.getString(R.string.billing_plan_free)
    val planName = context.getString(current.tier.displayNameRes)
    if (current.tier == PlanTier.FREE) return planName
    // Always use the live subscription interval — never the browse toggle.
    val adjective = context.getString(current.interval.adjectiveRes)
    return "$adjective $planName"
}

private fun planChangeAlertTitleText(
    context: android.content.Context,
    state: AccountBillingState,
    plan: SubscriptionPlan,
): String = context.getString(
    R.string.billing_action_change_to,
    planChangeTargetLabelText(context, state, plan),
)

private fun billingOutcomeExplanationText(
    context: android.content.Context,
    state: AccountBillingState,
    plan: SubscriptionPlan,
): String {
    if (plan.tier == PlanTier.FREE) {
        return context.getString(R.string.billing_modal_downgrade_free)
    }
    val price = plan.formattedPrice(state.selectedInterval)
    val cycle = context.getString(state.selectedInterval.titleRes)
    return context.getString(R.string.billing_modal_proration, price, cycle)
}

private fun planChangeAlertMessageText(
    context: android.content.Context,
    state: AccountBillingState,
    plan: SubscriptionPlan,
): String {
    val confirm = context.getString(
        R.string.billing_change_confirm_message,
        currentPlanLabelText(context, state),
        planChangeTargetLabelText(context, state, plan),
    )
    return "$confirm\n\n${billingOutcomeExplanationText(context, state, plan)}"
}

@Composable
private fun BillingSheetHeader(onDismiss: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.common_close),
            color = colors.text,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(50))
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .background(colors.card)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        Text(
            text = stringResource(R.string.billing_title),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp)
                .align(Alignment.Center),
        )
    }
}

@Composable
private fun BillingContent(
    state: AccountBillingState,
    accountEmail: String,
    onShareExportedPdf: (android.net.Uri) -> Unit,
    onPlanAction: (SubscriptionPlan) -> Unit,
    onIntervalChange: (BillingInterval) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned above the scroll, matching iOS billingContent.
        BillingIntervalSection(
            state = state,
            onIntervalChange = onIntervalChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ChangePlanSection(
                state = state,
                onPlanAction = onPlanAction,
            )
            InvoicesSection(
                state = state,
                accountEmail = accountEmail,
                onShareExportedPdf = onShareExportedPdf,
            )
        }
    }
}

@Composable
private fun BillingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun BillingIntervalSection(
    state: AccountBillingState,
    onIntervalChange: (BillingInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val selectedInterval = state.selectedInterval
    val currentPlanLabel = stringResource(
        R.string.billing_current_plan_label,
        currentPlanLabelText(context, state),
    )
    BillingCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BillingInterval.entries.forEach { interval ->
                val selected = selectedInterval == interval
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) TabSelected else colors.background)
                        .clickable { onIntervalChange(interval) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(interval.titleRes),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = if (selected) Color.White else colors.text,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (interval == BillingInterval.YEARLY) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.billing_save_percent),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = if (selected) {
                                Color.White
                            } else {
                                Color(0xFF0D7333)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) {
                                        BillingGreen
                                    } else {
                                        BillingGreen.copy(alpha = 0.22f)
                                    },
                                )
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        var planLabelSize by remember(currentPlanLabel) { mutableStateOf(15.sp) }
        Text(
            text = currentPlanLabel,
            color = colors.secondaryText,
            fontSize = planLabelSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && planLabelSize > 11.sp) {
                    planLabelSize = (planLabelSize.value * 0.9f).coerceAtLeast(11f).sp
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChangePlanSection(
    state: AccountBillingState,
    onPlanAction: (SubscriptionPlan) -> Unit,
) {
    BillingCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.plans.forEach { plan ->
                val canCancel = state.canCancelCurrentPlan(plan)
                val isExactCurrent = state.isExactCurrentSelection(plan)
                PlanCard(
                    plan = plan,
                    isCurrent = isExactCurrent,
                    actionTitle = planActionTitle(state, plan),
                    interval = state.selectedInterval,
                    canCancel = canCancel,
                    isExactCurrent = isExactCurrent,
                    enabled = !state.isBusy && !(isExactCurrent && !canCancel),
                    onSelect = { onPlanAction(plan) },
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    actionTitle: String,
    interval: BillingInterval,
    canCancel: Boolean,
    isExactCurrent: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val buttonTextColor = when {
        canCancel -> LikeRed
        isExactCurrent -> colors.secondaryText
        else -> Color.White
    }
    val buttonBackground = when {
        canCancel -> LikeRed.copy(alpha = 0.08f)
        isExactCurrent -> colors.text.copy(alpha = 0.08f)
        else -> TabSelected
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) BillingGreen.copy(alpha = 0.45f) else colors.border,
                shape = RoundedCornerShape(14.dp),
            )
            .background(if (isCurrent) BillingGreen.copy(alpha = 0.1f) else colors.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(plan.tier.displayNameRes) +
                        if (plan.tier.titleStars.isNotEmpty()) {
                            " ${plan.tier.titleStars}"
                        } else {
                            ""
                        },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                )
            }
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.billing_badge_current),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TabSelected,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(50))
                        .background(TabSelected.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (plan.tier == PlanTier.FREE) {
            Text(
                text = stringResource(R.string.billing_price_no_charge),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.text,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plan.formattedPrice(interval),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        if (interval == BillingInterval.YEARLY) {
                            R.string.billing_per_year
                        } else {
                            R.string.billing_per_month
                        },
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                )
            }
        }
        plan.tier.featureResIds.forEach { featureRes ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BillingGreen,
                    modifier = Modifier.size(20.dp),
                )
                Text(stringResource(featureRes), fontSize = 14.sp, color = colors.secondaryText)
            }
        }
        Text(
            text = actionTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = buttonTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(buttonBackground)
                .clickable(enabled = enabled, onClick = onSelect)
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun InvoicesSection(
    state: AccountBillingState,
    accountEmail: String,
    onShareExportedPdf: (android.net.Uri) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recent = state.recentAccountHistory()

    BillingCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.billing_section_invoices),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.text,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = !state.isExportingBillingHistory && recent.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                state.isExportingBillingHistory = true
                                runCatching {
                                    val appContext = context.applicationContext
                                    val file = withContext(Dispatchers.IO) {
                                        BillingHistoryExport.writePdfFile(
                                            context = appContext,
                                            accountEmail = accountEmail,
                                            events = recent,
                                        )
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        appContext,
                                        "${appContext.packageName}.fileprovider",
                                        file,
                                    )
                                    onShareExportedPdf(uri)
                                }.onFailure { error ->
                                    android.util.Log.e(
                                        "BillingHistoryExport",
                                        "Failed to export account history PDF",
                                        error,
                                    )
                                    state.showToast(
                                        context.getString(R.string.billing_invoices_export_error),
                                    )
                                }
                                state.isExportingBillingHistory = false
                            }
                        },
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.isExportingBillingHistory) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = TabSelected,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        tint = if (recent.isEmpty()) colors.secondaryText else TabSelected,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.billing_invoices_export),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (recent.isEmpty()) colors.secondaryText else TabSelected,
                    )
                }
            }
        }

        if (recent.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = colors.secondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.billing_invoices_empty),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val maxVisibleRows = 6
            val canScroll = recent.size > maxVisibleRows
            // Tall enough for date + title + vertical padding (Android text is larger than iOS 62pt).
            val rowHeight = 70.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (canScroll) {
                            // Cap at 6 rows, then scroll the rest.
                            Modifier
                                .height(rowHeight * maxVisibleRows)
                                .verticalScroll(rememberScrollState())
                        } else {
                            // Expand to fit every item fully (no clipping, no inner scroll).
                            Modifier
                        },
                    ),
            ) {
                recent.forEachIndexed { index, event ->
                    AccountHistoryRow(event = event)
                    if (index != recent.lastIndex) {
                        HorizontalDivider(
                            color = colors.border,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountHistoryRow(event: AccountHistoryEvent) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(event.formattedDateTime(), fontSize = 13.sp, color = colors.secondaryText)
            Text(
                text = event.displayTitle(context),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.text,
            )
        }
        if (event.showsAmount) {
            Text(
                text = event.formattedAmount(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.text,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun BillingSkeleton() {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.text.copy(alpha = 0.06f)),
            )
        }
    }
}

@Composable
private fun BillingErrorState(message: String, onRetry: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = colors.secondaryText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.common_retry),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(TabSelected)
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

/** Matches iOS `PlanChangeConfirmOverlay`. */
@Composable
private fun PlanChangeConfirmOverlay(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = message,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider(color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = colors.text,
                        fontSize = 16.sp,
                    )
                }
                VerticalDivider(modifier = Modifier.height(44.dp), color = colors.border)
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.billing_modal_confirm),
                        color = BillingGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

/** Matches iOS `PlanChangeSuccessOverlay`. */
@Composable
private fun PlanChangeSuccessOverlay(
    planLabel: String,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BillingGreen,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.billing_change_success_message, planLabel),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider(color = colors.border)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = stringResource(R.string.common_ok),
                    color = BillingGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
