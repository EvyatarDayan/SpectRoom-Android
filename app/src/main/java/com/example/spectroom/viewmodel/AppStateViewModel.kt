package com.example.spectroom.viewmodel

import android.net.Uri
import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spectroom.data.local.AppPrefsStore
import com.example.spectroom.data.local.DiscoverDailyLimitStore
import com.example.spectroom.data.local.DiscoverDeckCache
import com.example.spectroom.data.local.DiscoverDeckFilterSignature
import com.example.spectroom.data.local.DiscoverDistanceStore
import com.example.spectroom.data.local.LocalNotificationHelper
import com.example.spectroom.data.local.MediaDailyLimitStore
import com.example.spectroom.data.local.MessagesExport
import com.example.spectroom.data.local.MessagesExportConversation
import com.example.spectroom.data.local.PlanEntitlementsStore
import com.example.spectroom.data.local.ProfileBoostLimitStore
import com.example.spectroom.data.local.SoundService
import com.example.spectroom.data.model.AdminSender
import com.example.spectroom.data.model.AdminUserProfile
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.data.model.ModerationFreezeDuration
import com.example.spectroom.data.model.ModerationNotice
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.SupportTopic
import com.example.spectroom.data.model.UserReport
import com.example.spectroom.R
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import androidx.core.content.FileProvider
import com.example.spectroom.repository.AccessGate
import com.example.spectroom.repository.AdminRepository
import com.example.spectroom.data.model.BlockedUser
import com.example.spectroom.data.model.ConversationDisplay
import com.example.spectroom.data.model.DiscoverDistanceOption
import com.example.spectroom.data.model.LookingForOption
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.data.model.MatchDisplay
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.MessageType
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.ProfileReportReason
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.data.model.SwipeType
import com.example.spectroom.data.model.UserProfileStatus
import com.example.spectroom.data.remote.AddressSearchRepository
import com.example.spectroom.data.remote.AddressSuggestion
import com.example.spectroom.data.remote.MfaAuthRepository
import com.example.spectroom.data.remote.MfaSignInResult
import com.example.spectroom.data.remote.MockBillingService
import com.example.spectroom.data.remote.PhotoStorageRepository
import com.example.spectroom.data.remote.VoiceMessageRepository
import com.example.spectroom.data.remote.toUserLocation
import com.example.spectroom.domain.DiscoverEligibility
import com.example.spectroom.domain.MatchScoring
import com.example.spectroom.domain.ProfileCompletion
import com.example.spectroom.repository.AuthRepository
import com.example.spectroom.repository.AuthResult
import com.example.spectroom.repository.DiscoverRepository
import com.example.spectroom.repository.MatchRepository
import com.example.spectroom.repository.MessageRepository
import com.example.spectroom.repository.ModerationRepository
import com.example.spectroom.ui.navigation.MainTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID

enum class AppDestination {
    LOADING,
    ONBOARDING,
    AUTH,
    MFA,
    MANDATORY_PROFILE,
    MAIN,
}

enum class MfaUiMode {
    SIGN_IN,
    ENROLL,
}

data class AppUiState(
    val destination: AppDestination = AppDestination.LOADING,
    val hasResolvedAuthState: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isNewSignup: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val hasCompletedInitialProfile: Boolean = false,
    val currentUser: SpectRoomUser? = null,
    val isDarkMode: Boolean = false,
    val selectedTab: Int = 2,
    val showingMatchNotification: Boolean = false,
    val newMatchProfile: ProfileCard? = null,
    val unreadMessagesCount: Int = 0,
    val authError: String? = null,
    val authNotice: String? = null,
    val authPopupTitle: String? = null,
    val isAuthLoading: Boolean = false,
    val profileSaveError: String? = null,
    val isSavingProfile: Boolean = false,
    val activeChatPartnerId: String? = null,
    val activeChatPartnerName: String? = null,
    val activeChatPartnerPhoto: String? = null,
    val chatBlockedByPartner: Boolean = false,
    val showingBlockedUserAlert: Boolean = false,
    val showingNewMessageNotification: Boolean = false,
    val newMessageSenderId: String? = null,
    val newMessageSenderName: String = "",
    val newMessageSenderPhoto: String? = null,
    val mfaMode: MfaUiMode = MfaUiMode.SIGN_IN,
    val mfaMaskedPhone: String = "",
    val mfaEmailAddress: String = "",
    val mfaEmailVerified: Boolean = false,
    val mfaResolvedPhoneE164: String = "",
    val mfaSmsSent: Boolean = false,
    val mfaSmsFailed: Boolean = false,
    val mfaSmsRateLimited: Boolean = false,
    val mfaEditingPhone: Boolean = false,
    val viewingProfile: ProfileCard? = null,
    val isRecordingVoice: Boolean = false,
    val recordingDurationSeconds: Double = 0.0,
    val languageCode: String = "en",
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val showingCompleteProfileWelcome: Boolean = false,
    val composeMessageProfile: ProfileCard? = null,
    val pendingBlockProfile: ProfileCard? = null,
    val pendingVoicePreviewDuration: Double = 0.0,
    val hasPendingVoicePreview: Boolean = false,
    val pendingVoicePreviewPath: String? = null,
    val photoSaveNotice: String? = null,
    val isAdmin: Boolean = false,
)

data class DiscoverUiState(
    val profiles: List<ProfileCard> = emptyList(),
    val currentIndex: Int = 0,
    val dailySwipeCount: Int = 0,
    val hasUsedSecondThoughtsToday: Boolean = false,
    val isReviewingTodaysPasses: Boolean = false,
    val isLoadingSecondThoughts: Boolean = false,
    val secondThoughtsError: String? = null,
    val distanceOption: DiscoverDistanceOption = DiscoverDistanceOption.DEFAULT,
    val canUndo: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lastSwipedProfile: ProfileCard? = null,
    val lastSwipeType: SwipeType? = null,
    val lastSwipeCountedTowardLimit: Boolean = false,
    val lastDocumentId: String? = null,
    val hasMore: Boolean = true,
    /** Triggers the top card fly-off animation (Pass / Like buttons, block-from-profile). */
    val programmaticSwipe: SwipeType? = null,
    /** When set, the next programmatic pass only advances the pile (no Firestore swipe). */
    val blockPassUserId: String? = null,
)

data class SocialUiState(
    val matches: List<MatchDisplay> = emptyList(),
    val conversations: List<ConversationDisplay> = emptyList(),
    val chatMessages: List<MessageDisplay> = emptyList(),
    val chatHasMoreOlder: Boolean = false,
    val isLoadingOlderChat: Boolean = false,
    val isLoadingChatMessages: Boolean = false,
    val blockedUsers: List<BlockedUser> = emptyList(),
    val blockedByUsers: List<BlockedUser> = emptyList(),
    val isLoadingMatches: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val chatImageLocalPreviews: Map<String, String> = emptyMap(),
    val uploadingImageContentKey: String? = null,
    val messageHistoryMatchIds: Set<String> = emptySet(),
)

data class PhotoViewerState(
    val photos: List<String>,
    val startIndex: Int = 0,
)

data class AddressSearchUiState(
    val visible: Boolean = false,
    val isSearching: Boolean = false,
    val suggestions: List<AddressSuggestion> = emptyList(),
)

class AppStateViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val discoverRepository = DiscoverRepository()
    private val matchRepository = MatchRepository()
    private val messageRepository = MessageRepository()
    private val moderationRepository = ModerationRepository()
    private val adminRepository = AdminRepository()
    private val photoStorageRepository = PhotoStorageRepository(application)
    private val addressSearchRepository = AddressSearchRepository(application)
    private val voiceMessageRepository = VoiceMessageRepository(application)
    private val mfaAuthRepository = MfaAuthRepository()
    private val planEntitlementsStore = PlanEntitlementsStore(application)
    private val dailyLimitStore = DiscoverDailyLimitStore(application, planEntitlementsStore)
    private val mediaDailyLimitStore = MediaDailyLimitStore(application, planEntitlementsStore)
    private val profileBoostLimitStore = ProfileBoostLimitStore(application, planEntitlementsStore)
    private val distanceStore = DiscoverDistanceStore(application)
    private val discoverDeckCache = DiscoverDeckCache(application)
    private val appPrefs = AppPrefsStore(application)
    private val soundService = SoundService(application)
    private val localNotifications = LocalNotificationHelper(application)
    private var isAppInForeground: Boolean = true
    private var addressSearchJob: Job? = null
    private var pendingVoiceFile: File? = null
    private var pendingVoiceDuration: Double = 0.0
    private var activityProvider: (() -> Activity?)? = null
    private var realtimeJobs: List<Job> = emptyList()
    private var lastPresentedMatchId: String? = null
    private var presenceHeartbeatJob: Job? = null
    private var messageSearchJob: Job? = null
    private var chatAutoRefreshJob: Job? = null
    private var voiceRecordingTickerJob: Job? = null
    private var lastPlayedMessageSoundId: String? = null
    private var suppressIncomingMessageSoundUntil: Long = 0L
    private var knownChatMessageKeys: Set<String> = emptySet()
    private var chatSoundBaselineReady: Boolean = false
    private var lastChimeAtMs: Long = 0L
    private val pendingIncomingImageSoundKeys = mutableSetOf<String>()
    private var savedProfilesBeforeSecondThoughts: List<ProfileCard> = emptyList()
    private var savedCardIndexBeforeSecondThoughts: Int = 0

    private val _uiState = MutableStateFlow(
        AppUiState(
            isDarkMode = appPrefs.isDarkMode,
            hasCompletedOnboarding = appPrefs.hasCompletedOnboarding,
            languageCode = appPrefs.languageCode,
            notificationsEnabled = appPrefs.notificationsEnabled,
            soundEnabled = appPrefs.soundEnabled,
            isAuthenticated = authRepository.currentUid != null,
            destination = AppDestination.LOADING,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _socialState = MutableStateFlow(SocialUiState())
    val socialState: StateFlow<SocialUiState> = _socialState.asStateFlow()

    private val _addressSearchState = MutableStateFlow(AddressSearchUiState())
    val addressSearchState: StateFlow<AddressSearchUiState> = _addressSearchState.asStateFlow()

    private val _showBlockedUsers = MutableStateFlow(false)
    val showBlockedUsers: StateFlow<Boolean> = _showBlockedUsers.asStateFlow()

    private val _reportTarget = MutableStateFlow<ProfileCard?>(null)
    val reportTarget: StateFlow<ProfileCard?> = _reportTarget.asStateFlow()

    private val _reportAfterBlock = MutableStateFlow(false)
    val reportAfterBlock: StateFlow<Boolean> = _reportAfterBlock.asStateFlow()

    private val _showReportThankYou = MutableStateFlow(false)
    val showReportThankYou: StateFlow<Boolean> = _showReportThankYou.asStateFlow()

    private val _infoSheet = MutableStateFlow<InfoSheetType?>(null)
    val infoSheet: StateFlow<InfoSheetType?> = _infoSheet.asStateFlow()

    private val _showHelpSupport = MutableStateFlow(false)
    val showHelpSupport: StateFlow<Boolean> = _showHelpSupport.asStateFlow()

    private val _showAccountBilling = MutableStateFlow(false)
    val showAccountBilling: StateFlow<Boolean> = _showAccountBilling.asStateFlow()

    private val _currentPlanTier = MutableStateFlow(planEntitlementsStore.currentPlanTier)
    val currentPlanTierState: StateFlow<PlanTier> = _currentPlanTier.asStateFlow()

    /** Ignores stale Firestore plan snapshots right after an Android-initiated change. */
    private var localPlanWriteAtMillis: Long = 0L

    private val _showMediaLimitOverlay = MutableStateFlow(false)
    val showMediaLimitOverlay: StateFlow<Boolean> = _showMediaLimitOverlay.asStateFlow()

    private val _showUndoLimitOverlay = MutableStateFlow(false)
    val showUndoLimitOverlay: StateFlow<Boolean> = _showUndoLimitOverlay.asStateFlow()

    private val _helpLockedTopic = MutableStateFlow<SupportTopic?>(null)
    val helpLockedTopic: StateFlow<SupportTopic?> = _helpLockedTopic.asStateFlow()

    private val _isSendingSupport = MutableStateFlow(false)
    val isSendingSupport: StateFlow<Boolean> = _isSendingSupport.asStateFlow()

    private val _supportSendSucceeded = MutableStateFlow(false)
    val supportSendSucceeded: StateFlow<Boolean> = _supportSendSucceeded.asStateFlow()

    private val _supportError = MutableStateFlow<String?>(null)
    val supportError: StateFlow<String?> = _supportError.asStateFlow()

    private val _showReportedProfiles = MutableStateFlow(false)
    val showReportedProfiles: StateFlow<Boolean> = _showReportedProfiles.asStateFlow()

    private val _myReports = MutableStateFlow<List<UserReport>>(emptyList())
    val myReports: StateFlow<List<UserReport>> = _myReports.asStateFlow()

    private val _isLoadingReports = MutableStateFlow(false)
    val isLoadingReports: StateFlow<Boolean> = _isLoadingReports.asStateFlow()

    private val _showDeleteAccount = MutableStateFlow(false)
    val showDeleteAccount: StateFlow<Boolean> = _showDeleteAccount.asStateFlow()
    private val _deleteAccountSucceeded = MutableStateFlow(false)
    val deleteAccountSucceeded: StateFlow<Boolean> = _deleteAccountSucceeded.asStateFlow()

    private val _photoViewer = MutableStateFlow<PhotoViewerState?>(null)
    val photoViewer: StateFlow<PhotoViewerState?> = _photoViewer.asStateFlow()

    private val _showLanguagePicker = MutableStateFlow(false)
    val showLanguagePicker: StateFlow<Boolean> = _showLanguagePicker.asStateFlow()

    private val _moderationNotice = MutableStateFlow<ModerationNotice?>(null)
    val moderationNotice: StateFlow<ModerationNotice?> = _moderationNotice.asStateFlow()

    private val _exportShareUri = MutableStateFlow<android.net.Uri?>(null)
    val exportShareUri: StateFlow<android.net.Uri?> = _exportShareUri.asStateFlow()

    private val _isExportingMessages = MutableStateFlow(false)
    val isExportingMessages: StateFlow<Boolean> = _isExportingMessages.asStateFlow()

    enum class InfoSheetType { ABOUT, PRIVACY }

    init {
        viewModelScope.launch {
            authRepository.authState().collect { user ->
                if (user == null) {
                    val pendingMfaSignIn = mfaAuthRepository.hasPendingSignIn ||
                        (_uiState.value.destination == AppDestination.MFA &&
                            _uiState.value.mfaMode == MfaUiMode.SIGN_IN)
                    _uiState.update { state ->
                        val cleared = state.copy(
                            hasResolvedAuthState = true,
                            isAuthenticated = false,
                            currentUser = null,
                            isAdmin = false,
                            activeChatPartnerId = null,
                        )
                        if (pendingMfaSignIn) {
                            cleared
                        } else {
                            cleared.copy(
                                destination = if (state.hasCompletedOnboarding) {
                                    AppDestination.AUTH
                                } else {
                                    AppDestination.ONBOARDING
                                },
                            )
                        }
                    }
                    if (!pendingMfaSignIn) {
                        _discoverState.value = DiscoverUiState()
                        _socialState.value = SocialUiState()
                    }
                } else {
                    if (!appPrefs.hasCompletedOnboarding) {
                        appPrefs.hasCompletedOnboarding = true
                    }
                    _uiState.update {
                        it.copy(
                            hasResolvedAuthState = true,
                            hasCompletedOnboarding = true,
                        )
                    }
                    refreshSession()
                }
            }
        }
        viewModelScope.launch {
            socialState.collect { social ->
                val partnerId = _uiState.value.activeChatPartnerId
                if (partnerId.isNullOrBlank() || !chatSoundBaselineReady) return@collect
                val keys = social.chatMessages.map { it.deduplicationKey }.toSet()
                // iOS parity: only chime for messages from the open chat partner.
                val newIncoming = social.chatMessages.filter {
                    it.deduplicationKey !in knownChatMessageKeys && it.senderId == partnerId
                }
                knownChatMessageKeys = keys
                if (newIncoming.isNotEmpty()) {
                    handleNewIncomingChatMessages(newIncoming)
                }
            }
        }
    }

    private fun resetChatSoundBaseline() {
        chatSoundBaselineReady = false
        knownChatMessageKeys = emptySet()
        pendingIncomingImageSoundKeys.clear()
    }

    private fun establishChatSoundBaseline() {
        knownChatMessageKeys = _socialState.value.chatMessages.map { it.deduplicationKey }.toSet()
        chatSoundBaselineReady = true
    }

    fun attachActivity(provider: () -> Activity?) {
        activityProvider = provider
    }

    override fun onCleared() {
        voiceMessageRepository.release()
        soundService.release()
        super.onCleared()
    }

    fun completeOnboarding(dontShowAgain: Boolean = false) {
        appPrefs.hasCompletedOnboarding = true
        appPrefs.dontShowWelcomeAgain = dontShowAgain
        _uiState.update {
            it.copy(
                hasCompletedOnboarding = true,
                destination = AppDestination.AUTH,
            )
        }
    }

    fun registerPhotoShareMediaTapAndShouldShow(): Boolean =
        appPrefs.registerPhotoShareMediaTapAndShouldShow(authRepository.accountKey)

    fun resetPhotoShareWarning() {
        appPrefs.resetPhotoShareWarning(authRepository.accountKey)
    }

    fun dismissModerationNotice() {
        val notice = _moderationNotice.value ?: return
        viewModelScope.launch {
            if (notice is ModerationNotice.Warning) {
                persistAdminWarningMessage(notice)
            }
            runCatching { authRepository.acknowledgeModerationNotice(notice) }
            _moderationNotice.value = null
            if (notice.signsOutOnDismiss) {
                signOut()
            }
        }
    }

    fun openSupportFromModeration(notice: ModerationNotice) {
        showHelpSupport(SupportTopic.forModerationNotice(notice))
    }

    fun clearExportShareUri() {
        _exportShareUri.value = null
    }

    fun shareExportedPdf(uri: android.net.Uri) {
        _exportShareUri.value = uri
    }

    fun exportMessages() {
        val currentUser = _uiState.value.currentUser ?: return
        val uid = currentUser.firebaseDocumentId ?: authRepository.currentUid ?: return
        viewModelScope.launch {
            _isExportingMessages.value = true
            runCatching {
                val conversations = messageRepository.fetchConversationList(currentUser)
                val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
                val exportConversations = conversations.map { conversation ->
                    val messages = messageRepository.fetchConversationMessages(conversation.otherUserId)
                        .filter { it.createdAt.time >= sixMonthsAgo }
                    MessagesExportConversation(
                        partnerName = conversation.otherUserName,
                        messages = messages,
                    )
                }
                val context = getApplication<Application>()
                val file = MessagesExport.writePdfFile(
                    context = context,
                    currentUserName = currentUser.name.ifBlank { currentUser.email },
                    currentUserId = uid,
                    conversations = exportConversations,
                    youLabel = context.getString(R.string.settings_export_messages_you),
                    title = context.getString(R.string.settings_export_messages_file_title),
                    exportedAtTemplate = context.getString(R.string.settings_export_messages_exported_at),
                    accountTemplate = context.getString(R.string.settings_export_messages_account),
                    conversationWithTemplate = context.getString(R.string.settings_export_messages_conversation_with),
                    emptyLabel = context.getString(R.string.settings_export_messages_empty),
                    voiceLabel = context.getString(R.string.settings_export_messages_voice),
                    photoLabel = context.getString(R.string.settings_export_messages_photo),
                    videoLabel = context.getString(R.string.settings_export_messages_video),
                )
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                _exportShareUri.value = uri
            }.onFailure {
                _uiState.update {
                    it.copy(authError = getApplication<Application>().getString(R.string.settings_export_messages_error))
                }
            }
            _isExportingMessages.value = false
        }
    }

    fun completeInitialProfile(updatedUser: SpectRoomUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileSaveError = null) }
            val result = authRepository.saveUserProfile(updatedUser)
            result.onSuccess { saved ->
                val needsMfa = !mfaAuthRepository.isMfaEnrolledOrBypassed()
                _uiState.update {
                    it.copy(
                        currentUser = saved,
                        hasCompletedInitialProfile = true,
                        isNewSignup = false,
                    )
                }
                applyUnlimitedDistanceIfNeeded(saved)
                if (needsMfa) {
                    enterMfaEnrollment(emailAlreadyChecked = false)
                } else {
                    _uiState.update {
                        it.copy(
                            destination = AppDestination.MAIN,
                            profileSaveError = null,
                            isSavingProfile = false,
                        )
                    }
                    loadDiscoverDeck()
                    refreshSocial()
                    startRealtime()
                    syncFcmToken()
                    setOnline(true)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        profileSaveError = error.message ?: "Could not save profile",
                        isSavingProfile = false,
                    )
                }
            }
        }
    }

    private suspend fun enterMfaEnrollment(emailAlreadyChecked: Boolean) {
        val emailVerified = if (emailAlreadyChecked) {
            _uiState.value.mfaEmailVerified
        } else {
            runCatching { mfaAuthRepository.reloadAndIsEmailVerified() }.getOrDefault(false)
        }
        if (!emailVerified) {
            runCatching { mfaAuthRepository.sendEmailVerificationForMfa() }
        }
        _uiState.update {
            it.copy(
                hasCompletedInitialProfile = it.hasCompletedInitialProfile,
                isNewSignup = it.isNewSignup,
                destination = AppDestination.MFA,
                mfaMode = MfaUiMode.ENROLL,
                mfaEmailAddress = mfaAuthRepository.currentEmail(),
                mfaEmailVerified = emailVerified,
                mfaResolvedPhoneE164 = appPrefs.pendingMfaPhoneE164.orEmpty()
                    .ifBlank { it.mfaResolvedPhoneE164 },
                mfaSmsSent = false,
                mfaSmsFailed = false,
                mfaSmsRateLimited = false,
                mfaEditingPhone = false,
                profileSaveError = null,
                isSavingProfile = false,
                isAuthLoading = false,
                authError = null,
                authNotice = null,
                authPopupTitle = null,
            )
        }
        resolveMfaEnrollmentPhone()
    }

    fun saveProfile(updatedUser: SpectRoomUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileSaveError = null) }
            authRepository.saveUserProfile(updatedUser).onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        currentUser = saved,
                        profileSaveError = null,
                        isSavingProfile = false,
                    )
                }
                applyUnlimitedDistanceIfNeeded(saved)
                invalidateDiscoverDeckAfterProfileUpdate(saved)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        profileSaveError = error.message ?: "Could not save profile",
                        isSavingProfile = false,
                    )
                }
            }
        }
    }

    fun refreshCurrentUserProfile() {
        viewModelScope.launch {
            runCatching { authRepository.fetchCurrentUserProfile(fromServer = true) }.getOrNull()?.let { profile ->
                val resolved = applyPlanEntitlementsFromUser(profile)
                _uiState.update { it.copy(currentUser = resolved) }
            }
        }
    }

    fun showAccountBilling() {
        viewModelScope.launch {
            runCatching { authRepository.fetchCurrentUserProfile(fromServer = true) }.getOrNull()?.let { profile ->
                val resolved = applyPlanEntitlementsFromUser(profile)
                _uiState.update { it.copy(currentUser = resolved) }
            } ?: run {
                MockBillingService.shared.alignWithPlanTier(
                    planEntitlementsStore.currentPlanTier,
                    planEntitlementsStore.currentBillingInterval,
                )
            }
            _showAccountBilling.value = true
        }
    }

    private fun applyUnlimitedDistanceIfNeeded(user: SpectRoomUser) {
        val wantsOnlineFriendship = user.lookingFor.any {
            it.equals(LookingForOption.ONLINE_FRIENDSHIP.rawValue, ignoreCase = true)
        }
        if (!wantsOnlineFriendship) return
        if (_discoverState.value.distanceOption == DiscoverDistanceOption.UNLIMITED) return
        setDistanceOption(DiscoverDistanceOption.UNLIMITED)
    }

    fun setSelectedTab(tab: Int) {
        val previous = _uiState.value.selectedTab
        val closingChat = tab != 0 && _uiState.value.activeChatPartnerId != null
        _uiState.update {
            it.copy(
                selectedTab = tab,
                activeChatPartnerId = if (tab != 0) null else it.activeChatPartnerId,
                activeChatPartnerName = if (tab != 0) null else it.activeChatPartnerName,
                activeChatPartnerPhoto = if (tab != 0) null else it.activeChatPartnerPhoto,
            )
        }
        appPrefs.messagesTabActive = tab == 0
        if (closingChat) {
            appPrefs.openConversationUserId = null
            _socialState.update {
                it.copy(
                    chatMessages = emptyList(),
                    chatHasMoreOlder = false,
                    isLoadingChatMessages = false,
                )
            }
            viewModelScope.launch {
                runCatching { authRepository.updateOpenConversation(null) }
            }
        }
        if (previous != tab) {
            viewModelScope.launch {
                runCatching { authRepository.updateMessagesTabActive(tab == 0) }
            }
        }
        if (tab == 0 || tab == 1) refreshSocial()
        // Mirror iOS SearchView.handleSelectedTabChange: re-read local-day quota when
        // opening Discover so a midnight rollover unlocks the deck without a cold start.
        if (tab == MainTab.HOME.index) {
            refreshDiscoverDailyLimitIfNeeded(reloadDeckIfUnlocked = true)
        }
    }

    fun setOnline(isOnline: Boolean) {
        isAppInForeground = isOnline
        appPrefs.appIsActive = isOnline
        viewModelScope.launch {
            runCatching { authRepository.updateOnlineStatus(isOnline) }
            if (isOnline) {
                refreshDiscoverDailyLimitIfNeeded(reloadDeckIfUnlocked = true)
                startPresenceHeartbeat()
                val tab = _uiState.value.selectedTab
                val partnerId = _uiState.value.activeChatPartnerId
                appPrefs.messagesTabActive = tab == 0
                appPrefs.openConversationUserId = partnerId
                runCatching { authRepository.updateMessagesTabActive(tab == 0) }
                runCatching { authRepository.updateOpenConversation(partnerId) }
            } else {
                // Keep suppress flags false while backgrounded so tray notifications can show.
                appPrefs.messagesTabActive = false
                appPrefs.openConversationUserId = null
                stopPresenceHeartbeat()
                syncAppIconBadgeBeforeBackground()
            }
        }
    }

    fun syncAppIconBadgeBeforeBackground() {
        val count = _uiState.value.unreadMessagesCount
        if (count > 0) {
            localNotifications.syncLauncherBadge(count)
        } else {
            localNotifications.clearBadge()
        }
    }

    private fun startPresenceHeartbeat() {
        stopPresenceHeartbeat()
        presenceHeartbeatJob = viewModelScope.launch {
            while (true) {
                delay(20_000)
                runCatching { authRepository.touchPresenceHeartbeat() }
            }
        }
    }

    private fun stopPresenceHeartbeat() {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
    }

    fun dismissCompleteProfileWelcome() {
        val uid = _uiState.value.currentUser?.firebaseDocumentId ?: authRepository.currentUid
        appPrefs.markCompleteProfileWelcomeSeen(uid)
        _uiState.update { it.copy(showingCompleteProfileWelcome = false) }
    }

    fun shouldShowCompleteProfileWelcome(): Boolean {
        val uid = _uiState.value.currentUser?.firebaseDocumentId ?: authRepository.currentUid
        return appPrefs.shouldShowCompleteProfileWelcome(uid)
    }

    fun setDarkMode(enabled: Boolean) {
        appPrefs.isDarkMode = enabled
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun toggleDarkMode() {
        setDarkMode(!_uiState.value.isDarkMode)
    }

    fun showInfo(type: InfoSheetType) {
        _infoSheet.value = type
    }

    fun hideInfo() {
        _infoSheet.value = null
    }

    fun showHelpSupport(lockedTopic: SupportTopic? = null) {
        _helpLockedTopic.value = lockedTopic
        _supportError.value = null
        _supportSendSucceeded.value = false
        _showHelpSupport.value = true
    }

    fun hideHelpSupport() {
        _showHelpSupport.value = false
        _helpLockedTopic.value = null
        _supportError.value = null
        _supportSendSucceeded.value = false
    }

    fun hideAccountBilling() {
        _showAccountBilling.value = false
        refreshDiscoverStateAfterBillingChange()
    }

    fun currentPlanTier(): PlanTier = _currentPlanTier.value

    fun canSendChatMedia(): Boolean = mediaDailyLimitStore.canSend(authRepository.accountKey)

    fun mediaRemainingToday(): Int = mediaDailyLimitStore.remaining(authRepository.accountKey)

    fun mediaLimitMessageRes(): Int = when (planEntitlementsStore.currentPlanTier) {
        PlanTier.FREE -> R.string.messages_media_limit_message_free
        PlanTier.PRO -> R.string.messages_media_limit_message_pro
        PlanTier.PREMIUM -> R.string.messages_media_limit_message_premium
    }

    fun showMediaLimitOverlay() {
        _showMediaLimitOverlay.value = true
    }

    fun hideMediaLimitOverlay() {
        _showMediaLimitOverlay.value = false
    }

    fun hideUndoLimitOverlay() {
        _showUndoLimitOverlay.value = false
    }

    fun onBillingPlanChanged(tier: PlanTier, interval: BillingInterval = BillingInterval.MONTHLY) {
        markLocalPlanWrite()
        resolvePlanTier(tier, interval)
        viewModelScope.launch {
            pushBillingPlanToFirestore(tier, interval)
            _uiState.update { state ->
                val user = state.currentUser ?: return@update state
                state.copy(
                    currentUser = user.copy(
                        planTier = tier,
                        billingInterval = interval.takeUnless { tier == PlanTier.FREE },
                    ),
                )
            }
            refreshDiscoverStateAfterBillingChange()
            ensureAutomaticProfileBoost()
        }
    }

    fun refreshDiscoverStateAfterBillingChange() {
        val accountKey = authRepository.accountKey
        val freshCount = dailyLimitStore.currentCount(accountKey)
        val max = dailyLimitStore.maxSwipesPerDay()
        _discoverState.update {
            it.copy(
                dailySwipeCount = freshCount,
                hasUsedSecondThoughtsToday = dailyLimitStore.hasUsedSecondThoughtsToday(accountKey),
            )
        }
        if (freshCount < max && !_discoverState.value.isReviewingTodaysPasses) {
            // Unlock deck after upgrade without forcing a full reload.
        }
    }

    /**
     * Plan-based Discover visibility boost — activated automatically when the plan
     * allows it and the cooldown has elapsed (no manual Profile Boost UI).
     */
    fun ensureAutomaticProfileBoost() {
        val accountKey = authRepository.accountKey
        if (!profileBoostLimitStore.canActivate(accountKey)) return
        val now = System.currentTimeMillis()
        val until = now + ProfileBoostLimitStore.ACTIVE_DURATION_MS
        profileBoostLimitStore.recordActivation(accountKey, now)
        viewModelScope.launch {
            runCatching { authRepository.activateProfileBoost(until, now) }
            _uiState.update { state ->
                val user = state.currentUser ?: return@update state
                state.copy(
                    currentUser = user.copy(
                        lastProfileBoostAtMillis = now,
                        boostedUntilMillis = until,
                    ),
                )
            }
        }
    }

    fun submitSupportTicket(topic: SupportTopic, message: String) {
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            _isSendingSupport.value = true
            _supportError.value = null
            runCatching {
                val label = getApplication<Application>().getString(topic.labelRes)
                val uid = authRepository.currentUid
                if (uid.isNullOrBlank()) {
                    error("Sign in to send a support message, or email spectroom.support@gmail.com")
                }
                moderationRepository.submitSupportTicket(
                    topicLabel = "${topic.subjectPrefix} $label",
                    message = message,
                    userName = user?.name.orEmpty().ifBlank { "Unknown" },
                    userEmail = user?.email.orEmpty().ifBlank {
                        authRepository.currentEmail.orEmpty()
                    },
                )
            }.onSuccess {
                _isSendingSupport.value = false
                _supportSendSucceeded.value = true
                delay(2_000)
                hideHelpSupport()
            }.onFailure { error ->
                _isSendingSupport.value = false
                _supportError.value = error.message ?: "Could not send message"
            }
        }
    }

    fun showReportedProfiles() {
        _showReportedProfiles.value = true
        viewModelScope.launch {
            _isLoadingReports.value = true
            _myReports.value = runCatching {
                moderationRepository.fetchMyReports()
            }.getOrDefault(emptyList())
            _isLoadingReports.value = false
        }
    }

    fun hideReportedProfiles() {
        _showReportedProfiles.value = false
    }

    private val _showAdminConsole = MutableStateFlow(false)
    val showAdminConsole: StateFlow<Boolean> = _showAdminConsole.asStateFlow()

    private val _adminReports = MutableStateFlow<List<UserReport>>(emptyList())
    val adminReports: StateFlow<List<UserReport>> = _adminReports.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUserProfile>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserProfile>> = _adminUsers.asStateFlow()

    private val _isLoadingAdmin = MutableStateFlow(false)
    val isLoadingAdmin: StateFlow<Boolean> = _isLoadingAdmin.asStateFlow()

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    private val _adminActionMessage = MutableStateFlow<String?>(null)
    val adminActionMessage: StateFlow<String?> = _adminActionMessage.asStateFlow()

    fun showAdminConsole() {
        _showAdminConsole.value = true
        _adminError.value = null
        _adminActionMessage.value = null
    }

    fun hideAdminConsole() {
        _showAdminConsole.value = false
    }

    fun loadAdminData(search: String = "") {
        viewModelScope.launch {
            val showLoader = _adminReports.value.isEmpty() && _adminUsers.value.isEmpty()
            if (showLoader) _isLoadingAdmin.value = true
            _adminError.value = null
            runCatching {
                _adminReports.value = adminRepository.fetchAllReports()
                _adminUsers.value = adminRepository.fetchAdminUsers(search = search)
            }.onFailure { error ->
                _adminError.value = error.message ?: "Couldn't load admin data"
            }
            _isLoadingAdmin.value = false
        }
    }

    fun adminDismissReport(report: UserReport) {
        viewModelScope.launch {
            runAdminAction { adminRepository.dismissReport(report.id) }
        }
    }

    fun adminWarn(userId: String, message: String, reportId: String?) {
        viewModelScope.launch {
            runAdminAction { adminRepository.issueWarning(userId, message, reportId) }
        }
    }

    fun adminFreeze(userId: String, duration: ModerationFreezeDuration, reportId: String?) {
        viewModelScope.launch {
            runAdminAction { adminRepository.freezeUser(userId, duration, reportId) }
        }
    }

    fun adminBan(userId: String, reportId: String?) {
        viewModelScope.launch {
            runAdminAction { adminRepository.banUser(userId, reportId) }
        }
    }

    fun adminLiftBan(userId: String) {
        viewModelScope.launch {
            runAdminAction { adminRepository.removeAccountBan(userId) }
        }
    }

    fun adminLiftFreeze(userId: String) {
        viewModelScope.launch {
            runAdminAction { adminRepository.removeAccountFreeze(userId) }
        }
    }

    private suspend fun runAdminAction(block: suspend () -> Unit) {
        _adminError.value = null
        runCatching { block() }
            .onSuccess {
                _adminActionMessage.value =
                    getApplication<Application>().getString(R.string.admin_all_profiles_restrict_success)
                loadAdminData()
            }
            .onFailure { error ->
                _adminError.value = error.message
                    ?: getApplication<Application>().getString(R.string.admin_reports_action_error_title)
            }
    }

    fun showDeleteAccount() {
        _deleteAccountSucceeded.value = false
        _showDeleteAccount.value = true
        _uiState.update { it.copy(authError = null) }
    }

    fun hideDeleteAccount() {
        if (_deleteAccountSucceeded.value) return
        _showDeleteAccount.value = false
        _uiState.update { it.copy(authError = null) }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            runCatching {
                authRepository.reauthenticate(password)
                authRepository.requestAccountDeletion()
            }.onSuccess {
                stopRealtime()
                _discoverState.value = DiscoverUiState()
                _socialState.value = SocialUiState()
                _deleteAccountSucceeded.value = true
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        isAuthLoading = false,
                        authError = null,
                        selectedTab = 2,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = error.message ?: "Could not delete account",
                    )
                }
            }
        }
    }

    fun acknowledgeDeleteAccountSuccess() {
        _showDeleteAccount.value = false
        _deleteAccountSucceeded.value = false
        _uiState.update {
            it.copy(
                destination = AppDestination.AUTH,
                isAuthenticated = false,
                currentUser = null,
                selectedTab = 2,
                authError = null,
            )
        }
    }

    fun openPhotoViewer(photos: List<String>, startIndex: Int = 0) {
        if (photos.isEmpty()) return
        _photoViewer.value = PhotoViewerState(
            photos = photos,
            startIndex = startIndex.coerceIn(0, photos.lastIndex),
        )
    }

    fun openChatImage(url: String) {
        if (url.isNotBlank()) openPhotoViewer(listOf(url), 0)
    }

    fun closePhotoViewer() {
        _photoViewer.value = null
    }

    fun refreshViewingProfile() {
        val profileId = _uiState.value.viewingProfile?.userId ?: return
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            runCatching {
                val user = discoverRepository.fetchUsersByIds(listOf(profileId)).firstOrNull()
                    ?: return@runCatching
                val card = discoverRepository.toProfileCard(user, current, maxDistanceKm = null)
                val matchCard = discoverRepository.toProfileCard(
                    user,
                    current,
                    maxDistanceKm = null,
                    forMatches = true,
                )
                _uiState.update { it.copy(viewingProfile = card) }
                _socialState.update { social ->
                    social.copy(
                        matches = social.matches.map { match ->
                            if (match.otherUser.userId == profileId) {
                                match.copy(otherUser = matchCard)
                            } else {
                                match
                            }
                        },
                    )
                }
            }
        }
    }

    fun goToProfileTab() {
        setSelectedTab(3)
    }

    fun isCurrentProfileComplete(): Boolean {
        val user = _uiState.value.currentUser ?: return false
        return ProfileCompletion.evaluate(user) == UserProfileStatus.COMPLETE
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null, authNotice = null, authPopupTitle = null) }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            // iOS shows the success alert locally and does not surface reset failures on the auth screen.
            authRepository.resetPassword(email)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()
            if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authNotice = null,
                        authError = nextCredentialsErrorMessage(),
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            val signInResult = runCatching {
                mfaAuthRepository.signInWithPossibleMfa(trimmedEmail, trimmedPassword)
            }.getOrElse { error ->
                handleSignInFailure(error)
                return@launch
            }
            when (signInResult) {
                is MfaSignInResult.Completed -> {
                    appPrefs.failedSignInAttempts = 0
                    runCatching { refreshSession() }.onFailure { error ->
                        handleSessionRefreshFailure(error)
                    }
                }
                is MfaSignInResult.MfaRequired -> {
                    // Password was correct — always show MFA, never "invalid credentials".
                    appPrefs.failedSignInAttempts = 0
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            destination = AppDestination.MFA,
                            mfaMode = MfaUiMode.SIGN_IN,
                            mfaMaskedPhone = signInResult.maskedPhone,
                            mfaSmsSent = false,
                            mfaSmsFailed = false,
                            mfaSmsRateLimited = false,
                            authError = null,
                            authNotice = null,
                            authPopupTitle = null,
                        )
                    }
                    sendMfaCode(null)
                }
            }
        }
    }

    private fun nextCredentialsErrorMessage(): String {
        val attempts = appPrefs.failedSignInAttempts + 1
        appPrefs.failedSignInAttempts = attempts
        val app = getApplication<Application>()
        return if (attempts % 5 == 0) {
            app.getString(R.string.auth_error_credentials_with_forgot)
        } else {
            app.getString(R.string.auth_error_credentials)
        }
    }

    private fun isInvalidCredentialsError(error: Throwable): Boolean {
        if (error is FirebaseAuthInvalidCredentialsException) return true
        val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
        if (code.equals("ERROR_INVALID_CREDENTIAL", ignoreCase = true)) return true
        if (code.equals("ERROR_USER_NOT_FOUND", ignoreCase = true)) return true
        if (code.equals("ERROR_WRONG_PASSWORD", ignoreCase = true)) return true
        val lower = error.message.orEmpty().lowercase()
        return lower.contains("wrong password") ||
            lower.contains("no user record") ||
            (lower.contains("invalid") && lower.contains("credential"))
    }

    private fun handleSignInFailure(error: Throwable) {
        val message = error.message.orEmpty()
        val lower = message.lowercase()
        val app = getApplication<Application>()
        // Soft-deleted accounts must look like invalid credentials (iOS parity).
        val softPendingDeletion = lower.contains("recently deleted")
        val adminDeleted = lower.contains("account has been deleted") && !softPendingDeletion
        when {
            adminDeleted -> {
                _moderationNotice.value = ModerationNotice.LoginDeleted
                _uiState.update { it.copy(isAuthLoading = false, authError = null, authNotice = null) }
            }
            softPendingDeletion || isInvalidCredentialsError(error) -> {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authNotice = null,
                        authError = nextCredentialsErrorMessage(),
                    )
                }
            }
            mfaAuthRepository.hasPendingSignIn -> {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        destination = AppDestination.MFA,
                        mfaMode = MfaUiMode.SIGN_IN,
                        authError = null,
                        authNotice = null,
                        authPopupTitle = null,
                    )
                }
                sendMfaCode(null)
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authNotice = null,
                        authError = message.ifBlank { app.getString(R.string.auth_error_generic) },
                        authPopupTitle = app.getString(R.string.auth_notice),
                    )
                }
            }
        }
    }

    private fun handleSessionRefreshFailure(error: Throwable) {
        val app = getApplication<Application>()
        val message = error.message.orEmpty()
        _uiState.update {
            it.copy(
                isAuthLoading = false,
                authNotice = null,
                authError = message.ifBlank { app.getString(R.string.auth_error_generic) },
                authPopupTitle = app.getString(R.string.auth_notice),
            )
        }
    }

    fun confirmMfa(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authPopupTitle = null) }
            runCatching {
                when (_uiState.value.mfaMode) {
                    MfaUiMode.SIGN_IN -> mfaAuthRepository.confirmSignInMfa(code)
                    MfaUiMode.ENROLL -> mfaAuthRepository.confirmEnroll(code)
                }
                appPrefs.pendingMfaPhoneE164 = null
                refreshSession()
            }.onFailure { error ->
                val app = getApplication<Application>()
                val mapped = mapMfaConfirmError(error, app)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = mapped,
                        authPopupTitle = null,
                        mfaSmsRateLimited = mfaAuthRepository.isRateLimited(error),
                    )
                }
            }
        }
    }

    private fun mapMfaConfirmError(error: Throwable, app: Application): String {
        val message = error.message.orEmpty().lowercase()
        val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
        return when {
            code.contains("INVALID_VERIFICATION_CODE", ignoreCase = true) ||
                code.contains("MISSING_VERIFICATION_CODE", ignoreCase = true) ||
                message.contains("invalid verification code") ||
                message.contains("invalid code") ->
                app.getString(R.string.mfa_error_invalid_code)
            code.contains("SESSION_EXPIRED", ignoreCase = true) ||
                message.contains("session") && message.contains("expir") ->
                app.getString(R.string.mfa_error_session)
            else -> error.message ?: app.getString(R.string.mfa_error_invalid_code)
        }
    }

    fun sendMfaCode(phoneE164: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthLoading = true,
                    authError = null,
                    authPopupTitle = null,
                    mfaSmsFailed = false,
                    mfaSmsRateLimited = false,
                )
            }
            runCatching {
                val activity = activityProvider?.invoke() ?: error("Activity required for MFA")
                when (_uiState.value.mfaMode) {
                    MfaUiMode.SIGN_IN -> {
                        mfaAuthRepository.sendSignInMfaCode(activity)
                        _uiState.update {
                            it.copy(
                                mfaSmsSent = true,
                                mfaSmsFailed = false,
                                mfaSmsRateLimited = false,
                            )
                        }
                    }
                    MfaUiMode.ENROLL -> {
                        if (!_uiState.value.mfaEmailVerified) {
                            error(getApplication<Application>().getString(R.string.mfa_error_email_unverified))
                        }
                        val phone = phoneE164?.trim().orEmpty()
                            .ifBlank { _uiState.value.mfaResolvedPhoneE164 }
                        require(phone.startsWith("+") && phone.length >= 10) {
                            getApplication<Application>().getString(R.string.auth_error_phone_invalid)
                        }
                        mfaAuthRepository.startEnroll(activity, phone)
                        appPrefs.pendingMfaPhoneE164 = phone
                        _uiState.update {
                            it.copy(
                                mfaResolvedPhoneE164 = phone,
                                mfaSmsSent = true,
                                mfaSmsFailed = false,
                                mfaEditingPhone = false,
                            )
                        }
                    }
                }
            }.onFailure { error ->
                val rateLimited = mfaAuthRepository.isRateLimited(error)
                val emailUnverified = error.message.orEmpty().contains("email", ignoreCase = true) &&
                    error.message.orEmpty().contains("verif", ignoreCase = true)
                android.util.Log.e("MFA", "sendMfaCode failed", error)
                if (emailUnverified && _uiState.value.mfaMode == MfaUiMode.ENROLL) {
                    _uiState.update {
                        it.copy(
                            mfaEmailVerified = false,
                            mfaSmsFailed = false,
                            mfaSmsRateLimited = false,
                            mfaSmsSent = false,
                            authError = null,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            // Keep a dismissible popup with the real reason (emulator / SHA / test number).
                            authError = error.message
                                ?: getApplication<Application>().getString(R.string.mfa_error_send),
                            authPopupTitle = getApplication<Application>().getString(R.string.auth_notice),
                            mfaSmsFailed = _uiState.value.mfaMode == MfaUiMode.ENROLL,
                            mfaSmsRateLimited = rateLimited,
                            mfaSmsSent = false,
                        )
                    }
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
        }
    }

    fun resolveMfaEnrollmentPhone() {
        viewModelScope.launch {
            val fromPending = appPrefs.pendingMfaPhoneE164
                ?: mfaAuthRepository.pendingEnrollPhone()
            val stored = fromPending ?: runCatching {
                mfaAuthRepository.fetchStoredPhoneE164()
            }.getOrNull()
            if (stored.isNullOrBlank()) {
                delay(400)
                val retry = runCatching { mfaAuthRepository.fetchStoredPhoneE164() }.getOrNull()
                _uiState.update { it.copy(mfaResolvedPhoneE164 = retry.orEmpty()) }
            } else {
                appPrefs.pendingMfaPhoneE164 = stored
                _uiState.update { it.copy(mfaResolvedPhoneE164 = stored) }
            }
        }
    }

    fun saveMissingMfaPhone(phoneE164: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authPopupTitle = null) }
            runCatching {
                require(phoneE164.startsWith("+") && phoneE164.length >= 10) {
                    getApplication<Application>().getString(R.string.auth_error_phone_invalid)
                }
                mfaAuthRepository.savePendingPhone(phoneE164)
                appPrefs.pendingMfaPhoneE164 = phoneE164
                _uiState.update {
                    it.copy(
                        mfaResolvedPhoneE164 = phoneE164,
                        mfaEditingPhone = false,
                        mfaSmsSent = false,
                        mfaSmsFailed = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        authPopupTitle = getApplication<Application>().getString(R.string.auth_notice),
                        authError = error.message
                            ?: getApplication<Application>().getString(R.string.auth_error_phone_invalid),
                    )
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
        }
    }

    fun saveEditedMfaPhone(phoneE164: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authPopupTitle = null, mfaEditingPhone = false) }
            runCatching {
                require(phoneE164.startsWith("+") && phoneE164.length >= 10) {
                    getApplication<Application>().getString(R.string.auth_error_phone_invalid)
                }
                mfaAuthRepository.savePendingPhone(phoneE164)
                appPrefs.pendingMfaPhoneE164 = phoneE164
                _uiState.update {
                    it.copy(
                        mfaResolvedPhoneE164 = phoneE164,
                        mfaSmsSent = false,
                        mfaSmsFailed = false,
                        mfaSmsRateLimited = false,
                    )
                }
                sendMfaCode(phoneE164)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authPopupTitle = getApplication<Application>().getString(R.string.auth_notice),
                        authError = error.message
                            ?: getApplication<Application>().getString(R.string.auth_error_phone_invalid),
                    )
                }
            }
        }
    }

    fun setMfaEditingPhone(editing: Boolean) {
        _uiState.update { it.copy(mfaEditingPhone = editing, authError = null, authPopupTitle = null) }
    }

    fun sendMfaVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authPopupTitle = null) }
            runCatching {
                mfaAuthRepository.sendEmailVerificationForMfa()
                val app = getApplication<Application>()
                _uiState.update {
                    it.copy(
                        authPopupTitle = app.getString(R.string.mfa_email_sent_title),
                        authError = app.getString(R.string.mfa_email_sent),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        authPopupTitle = getApplication<Application>().getString(R.string.auth_notice),
                        authError = error.message ?: "Could not send verification email",
                    )
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
        }
    }

    fun checkMfaEmailVerified(manual: Boolean = true) {
        viewModelScope.launch {
            if (manual) {
                _uiState.update { it.copy(isAuthLoading = true, authError = null, authPopupTitle = null) }
            }
            runCatching {
                val verified = mfaAuthRepository.reloadAndIsEmailVerified()
                if (verified) {
                    _uiState.update {
                        it.copy(mfaEmailVerified = true, authError = null, authPopupTitle = null)
                    }
                } else if (manual) {
                    _uiState.update {
                        it.copy(
                            authPopupTitle = null,
                            authError = getApplication<Application>()
                                .getString(R.string.mfa_error_email_unverified),
                        )
                    }
                }
            }.onFailure { error ->
                if (manual) {
                    _uiState.update {
                        it.copy(
                            authPopupTitle = getApplication<Application>().getString(R.string.auth_notice),
                            authError = error.message ?: "Could not check email verification",
                        )
                    }
                }
            }
            if (manual) {
                _uiState.update { it.copy(isAuthLoading = false) }
            }
        }
    }

    fun cancelMfa() {
        mfaAuthRepository.clearPending()
        viewModelScope.launch {
            val mode = _uiState.value.mfaMode
            if (mode == MfaUiMode.SIGN_IN) {
                authRepository.signOut()
                _uiState.update {
                    it.copy(
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = null,
                        authPopupTitle = null,
                        mfaMaskedPhone = "",
                        mfaEmailVerified = false,
                        mfaEmailAddress = "",
                        mfaResolvedPhoneE164 = "",
                        mfaSmsSent = false,
                        mfaSmsFailed = false,
                        mfaSmsRateLimited = false,
                        mfaEditingPhone = false,
                    )
                }
                return@launch
            }
            // Enroll cancel = sign out & start over (iOS).
            authRepository.signOut()
            appPrefs.pendingMfaPhoneE164 = null
            val showOnboarding = !appPrefs.hasCompletedOnboarding
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    isNewSignup = false,
                    destination = if (showOnboarding) AppDestination.ONBOARDING else AppDestination.AUTH,
                    isAuthLoading = false,
                    authError = null,
                    authPopupTitle = null,
                    mfaMaskedPhone = "",
                    mfaEmailVerified = false,
                    mfaEmailAddress = "",
                    mfaResolvedPhoneE164 = "",
                    mfaSmsSent = false,
                    mfaSmsFailed = false,
                    mfaSmsRateLimited = false,
                    mfaEditingPhone = false,
                )
            }
        }
    }

    fun startMfaEnrollment() {
        viewModelScope.launch {
            enterMfaEnrollment(emailAlreadyChecked = false)
        }
    }

    fun openProfileDetail(profile: ProfileCard) {
        _uiState.update { it.copy(viewingProfile = profile) }
    }

    fun closeProfileDetail() {
        _uiState.update { it.copy(viewingProfile = null) }
    }

    fun matchBreakdownFor(profile: ProfileCard): MatchBreakdown? {
        val current = _uiState.value.currentUser ?: return null
        return MatchScoring.calculateBreakdown(
            interests = profile.interests,
            age = profile.age,
            lookingFor = profile.lookingFor,
            sensoryPreferences = profile.sensoryPreferences,
            shareSensoryPreferences = profile.shareSensoryPreferences,
            lifestyle = profile.lifestyle,
            shareLifestyle = profile.shareLifestyle,
            idealFirstDate = profile.idealFirstDate,
            shareIdealFirstDate = profile.shareIdealFirstDate,
            currentUser = current,
        )
    }

    fun showsFullMatchBreakdown(): Boolean =
        planEntitlementsStore.currentPlanTier.showsFullMatchBreakdown

    fun startVoiceRecording() {
        if (_uiState.value.chatBlockedByPartner || AdminSender.isAdmin(_uiState.value.activeChatPartnerId)) return
        if (!mediaDailyLimitStore.canSend(authRepository.accountKey)) {
            _showMediaLimitOverlay.value = true
            return
        }
        viewModelScope.launch {
            voiceMessageRepository.startRecording().onSuccess {
                _uiState.update {
                    it.copy(
                        isRecordingVoice = true,
                        recordingDurationSeconds = 0.0,
                        hasPendingVoicePreview = false,
                        pendingVoicePreviewDuration = 0.0,
                        pendingVoicePreviewPath = null,
                    )
                }
                startVoiceRecordingTicker()
            }.onFailure {
                _uiState.update { state -> state.copy(authError = it.message) }
            }
        }
    }

    private fun startVoiceRecordingTicker() {
        voiceRecordingTickerJob?.cancel()
        voiceRecordingTickerJob = viewModelScope.launch {
            while (isActive && voiceMessageRepository.isRecording) {
                _uiState.update {
                    it.copy(recordingDurationSeconds = voiceMessageRepository.currentRecordingDurationSeconds())
                }
                delay(200)
            }
        }
    }

    private fun stopVoiceRecordingTicker() {
        voiceRecordingTickerJob?.cancel()
        voiceRecordingTickerJob = null
    }

    fun stopVoiceRecordingAndSend() {
        stopVoiceRecordingForPreview()
    }

    fun stopVoiceRecordingForPreview() {
        if (_uiState.value.chatBlockedByPartner) {
            cancelVoiceRecording()
            return
        }
        stopVoiceRecordingTicker()
        val recorded = voiceMessageRepository.stopRecording()
        _uiState.update { it.copy(isRecordingVoice = false, recordingDurationSeconds = 0.0) }
        if (recorded == null) return
        pendingVoiceFile = recorded.first
        pendingVoiceDuration = recorded.second
        viewModelScope.launch {
            delay(350)
            _uiState.update {
                it.copy(
                    hasPendingVoicePreview = true,
                    pendingVoicePreviewDuration = recorded.second,
                    pendingVoicePreviewPath = recorded.first.absolutePath,
                )
            }
        }
    }

    fun playPendingVoicePreview() {
        // Handled by VoiceMessagePlayer in the preview sheet.
    }

    fun sendPendingVoiceMessage() {
        if (_uiState.value.chatBlockedByPartner || AdminSender.isAdmin(_uiState.value.activeChatPartnerId)) {
            discardPendingVoicePreview()
            return
        }
        val accountKey = authRepository.accountKey
        if (!mediaDailyLimitStore.canSend(accountKey)) {
            _showMediaLimitOverlay.value = true
            return
        }
        val partnerId = _uiState.value.activeChatPartnerId ?: return
        val file = pendingVoiceFile ?: return
        val duration = pendingVoiceDuration
        viewModelScope.launch {
            _socialState.update { it.copy(isSendingMessage = true) }
            val result = runCatching {
                val url = voiceMessageRepository.uploadVoice(file)
                messageRepository.sendVoiceMessage(partnerId, url, duration)
            }
            if (result.isSuccess) {
                mediaDailyLimitStore.recordSend(accountKey)
            }
            file.delete()
            pendingVoiceFile = null
            pendingVoiceDuration = 0.0
            _uiState.update {
                it.copy(
                    hasPendingVoicePreview = false,
                    pendingVoicePreviewDuration = 0.0,
                    pendingVoicePreviewPath = null,
                )
            }
            loadChatMessages(partnerId)
            refreshConversations()
            _socialState.update { it.copy(isSendingMessage = false) }
        }
    }

    fun discardPendingVoicePreview() {
        voiceMessageRepository.stopPlayback()
        pendingVoiceFile?.delete()
        pendingVoiceFile = null
        pendingVoiceDuration = 0.0
        _uiState.update {
            it.copy(
                hasPendingVoicePreview = false,
                pendingVoicePreviewDuration = 0.0,
                pendingVoicePreviewPath = null,
                recordingDurationSeconds = 0.0,
            )
        }
    }

    fun cancelVoiceRecording() {
        stopVoiceRecordingTicker()
        voiceMessageRepository.cancelRecording()
        pendingVoiceFile?.delete()
        pendingVoiceFile = null
        pendingVoiceDuration = 0.0
        _uiState.update {
            it.copy(
                isRecordingVoice = false,
                recordingDurationSeconds = 0.0,
                hasPendingVoicePreview = false,
                pendingVoicePreviewDuration = 0.0,
                pendingVoicePreviewPath = null,
            )
        }
    }

    fun playVoiceMessage(url: String) {
        voiceMessageRepository.play(url)
    }

    fun signUp(name: String, email: String, password: String, phoneE164: String) {
        if (_uiState.value.isAuthLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            appPrefs.pendingMfaPhoneE164 = phoneE164
            when (val result = authRepository.signUp(name, email, password, phoneE164)) {
                is AuthResult.Success -> {
                    val uid = authRepository.currentUid
                    if (!uid.isNullOrBlank()) {
                        appPrefs.prepareCompleteProfileWelcomeForSignup(uid)
                    }
                    _uiState.update {
                        it.copy(
                            isNewSignup = true,
                            hasCompletedInitialProfile = false,
                            mfaResolvedPhoneE164 = phoneE164,
                        )
                    }
                    runCatching { refreshSession() }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isAuthLoading = false,
                                authError = error.message ?: "Sign-up failed",
                            )
                        }
                    }
                }
                is AuthResult.MfaRequired -> {
                    appPrefs.pendingMfaPhoneE164 = null
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                }
                is AuthResult.Error -> {
                    appPrefs.pendingMfaPhoneE164 = null
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                }
                is AuthResult.Banned -> {
                    appPrefs.pendingMfaPhoneE164 = null
                    _moderationNotice.value = ModerationNotice.LoginBanned
                    _uiState.update { it.copy(isAuthLoading = false, authError = null) }
                }
                is AuthResult.Frozen -> {
                    appPrefs.pendingMfaPhoneE164 = null
                    _moderationNotice.value = ModerationNotice.LoginBlocked(result.until, result.hours)
                    _uiState.update { it.copy(isAuthLoading = false, authError = null) }
                }
                is AuthResult.Deleted -> {
                    appPrefs.pendingMfaPhoneE164 = null
                    _moderationNotice.value = ModerationNotice.LoginDeleted
                    _uiState.update { it.copy(isAuthLoading = false, authError = null) }
                }
            }
        }
    }

    fun resetAllAppState() {
        viewModelScope.launch {
            val accountKey = authRepository.accountKey
            runCatching {
                discoverRepository.deleteAllUserInteractions(messageRepository)
            }
            applyLocalAppResetAfterUserDataReset(accountKey)
        }
    }

    private suspend fun applyLocalAppResetAfterUserDataReset(accountKey: String?) {
        stopRealtime()
        appPrefs.hasCompletedOnboarding = false
        appPrefs.dontShowWelcomeAgain = false
        dailyLimitStore.reset(accountKey)
        mediaDailyLimitStore.reset(accountKey)
        profileBoostLimitStore.reset(accountKey)
        appPrefs.resetPhotoShareWarning(accountKey)
        markLocalPlanWrite()
        resolvePlanTier(PlanTier.FREE)
        distanceStore.resetToDefault(accountKey)
        discoverDeckCache.clear(accountKey)
        _discoverState.value = DiscoverUiState()
        _socialState.value = SocialUiState()
        _uiState.update {
            it.copy(
                isNewSignup = false,
                hasCompletedInitialProfile = false,
                currentUser = null,
                showingMatchNotification = false,
                newMatchProfile = null,
                activeChatPartnerId = null,
                activeChatPartnerName = null,
                activeChatPartnerPhoto = null,
                unreadMessagesCount = 0,
            )
        }
        if (_uiState.value.isAuthenticated) {
            refreshSession()
            loadDiscoverDeck()
            refreshSocial()
            startRealtime()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            setOnline(false)
            stopRealtime()
            authRepository.signOut()
            _discoverState.value = DiscoverUiState()
            _socialState.value = SocialUiState()
            if (!appPrefs.dontShowWelcomeAgain) {
                appPrefs.hasCompletedOnboarding = false
            }
            val showOnboarding = !appPrefs.hasCompletedOnboarding
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    isAdmin = false,
                    isNewSignup = false,
                    hasCompletedOnboarding = appPrefs.hasCompletedOnboarding,
                    destination = if (showOnboarding) {
                        AppDestination.ONBOARDING
                    } else {
                        AppDestination.AUTH
                    },
                    selectedTab = 2,
                    activeChatPartnerId = null,
                )
            }
        }
    }

    fun dismissMatchNotification() {
        _uiState.update { it.copy(showingMatchNotification = false, newMatchProfile = null) }
    }

    fun presentMatchNotification(profile: ProfileCard, matchId: String? = null) {
        if (matchId != null && matchId == lastPresentedMatchId) return
        if (_uiState.value.showingMatchNotification) return
        if (matchId != null) lastPresentedMatchId = matchId
        if (!isAppInForeground) {
            localNotifications.notifyNewMatch(
                matchUserId = profile.userId,
                matchName = profile.name,
                matchId = matchId ?: profile.userId,
                matchPhoto = profile.imageURL,
                badgeCount = (_uiState.value.unreadMessagesCount + 1).coerceAtLeast(1),
            )
            return
        }
        soundService.playMatchSound()
        _uiState.update { it.copy(showingMatchNotification = true, newMatchProfile = profile) }
    }

    fun openMatchFromNotification(
        userId: String,
        userName: String,
        userPhoto: String?,
        matchId: String?,
    ) {
        if (!matchId.isNullOrBlank()) {
            localNotifications.cancelMatchNotification(matchId)
        }
        val fromMatches = _socialState.value.matches.firstOrNull { it.otherUser.userId == userId }?.otherUser
        val profile = fromMatches ?: ProfileCard(
            userId = userId,
            name = userName.ifBlank { "Match" },
            age = 0,
            bio = "",
            interests = emptyList(),
            distance = "",
            imageURL = userPhoto,
            gender = "",
            interestedIn = emptyList(),
            lookingFor = emptyList(),
            photos = listOfNotNull(userPhoto),
            lifestyle = "",
            shareLifestyle = false,
            bodyType = "",
            shareBodyType = false,
            preferredAgeMin = 18,
            preferredAgeMax = 80,
            isOnline = false,
            lastSeen = java.util.Date(),
            idealFirstDate = emptyList(),
            shareIdealFirstDate = false,
        )
        _uiState.update {
            it.copy(
                selectedTab = 1,
                showingMatchNotification = true,
                newMatchProfile = profile,
            )
        }
        appPrefs.messagesTabActive = false
        appPrefs.openConversationUserId = null
        viewModelScope.launch {
            runCatching { authRepository.updateMessagesTabActive(false) }
            runCatching { authRepository.updateOpenConversation(null) }
        }
        refreshMatches()
    }

    fun openComposeMessage(profile: ProfileCard) {
        _uiState.update {
            it.copy(
                showingMatchNotification = false,
                newMatchProfile = null,
                composeMessageProfile = profile,
            )
        }
    }

    fun dismissComposeMessage() {
        _uiState.update { it.copy(composeMessageProfile = null) }
    }

    fun sendComposeMessage(text: String) {
        val profile = _uiState.value.composeMessageProfile ?: return
        val trimmed = text.trim()
        val userId = profile.userId.trim()
        if (trimmed.isEmpty() || userId.isEmpty()) return
        val name = profile.name
        val photo = profile.imageURL
        viewModelScope.launch {
            runCatching { messageRepository.sendTextMessage(userId, trimmed) }
            delay(320)
            openChat(userId, name, photo)
        }
    }

    /**
     * Apply [tier] (+ optional [interval]) to local cache + mock billing.
     * Callers must pass the Firestore values when syncing from the cloud.
     */
    private fun resolvePlanTier(
        tier: PlanTier,
        interval: BillingInterval? = null,
    ): PlanTier {
        val resolvedInterval = when {
            tier == PlanTier.FREE -> BillingInterval.MONTHLY
            interval != null -> interval
            else -> planEntitlementsStore.currentBillingInterval
        }
        planEntitlementsStore.syncBillingPlan(tier, resolvedInterval)
        MockBillingService.shared.alignWithPlanTier(tier, resolvedInterval)
        _currentPlanTier.value = tier
        return tier
    }

    private fun markLocalPlanWrite() {
        localPlanWriteAtMillis = System.currentTimeMillis()
    }

    private fun isWithinLocalPlanWriteGrace(): Boolean =
        System.currentTimeMillis() - localPlanWriteAtMillis < LOCAL_PLAN_WRITE_GRACE_MS

    private suspend fun pushBillingPlanToFirestore(
        tier: PlanTier,
        interval: BillingInterval?,
    ) {
        repeat(3) { attempt ->
            val ok = runCatching {
                authRepository.updateBillingPlan(tier, interval)
            }.isSuccess
            if (ok) return
            delay(400L * (attempt + 1))
        }
    }

    private suspend fun pushPlanTierToFirestore(tier: PlanTier) {
        pushBillingPlanToFirestore(
            tier = tier,
            interval = planEntitlementsStore.currentBillingInterval
                .takeUnless { tier == PlanTier.FREE },
        )
    }

    /**
     * Firestore is the source of truth for plan tier + billing interval.
     * Local mock billing / prefs are only a cache (except a short grace window after
     * a write from this device, to avoid a stale listener snapshot undoing it).
     */
    private fun applyPlanEntitlementsFromUser(profile: SpectRoomUser): SpectRoomUser {
        if (isWithinLocalPlanWriteGrace()) {
            val localTier = planEntitlementsStore.currentPlanTier
            val localInterval = planEntitlementsStore.currentBillingInterval
            resolvePlanTier(localTier, localInterval)
            return profile.copy(
                planTier = localTier,
                billingInterval = localInterval.takeUnless { localTier == PlanTier.FREE },
            )
        }

        val remoteTier = profile.planTier
        val remoteInterval = when {
            remoteTier == PlanTier.FREE -> BillingInterval.MONTHLY
            profile.billingInterval != null -> profile.billingInterval
            // Paid plan in DB without interval yet — converge on monthly and backfill.
            else -> BillingInterval.MONTHLY
        }
        resolvePlanTier(remoteTier, remoteInterval)

        val needsIntervalBackfill =
            remoteTier != PlanTier.FREE && profile.billingInterval == null
        if (needsIntervalBackfill) {
            viewModelScope.launch {
                pushBillingPlanToFirestore(remoteTier, remoteInterval)
            }
        }

        profileBoostLimitStore.syncFromRemote(
            accountKey = authRepository.accountKey,
            lastBoostAtMillis = profile.lastProfileBoostAtMillis,
            boostedUntilMillis = profile.boostedUntilMillis,
        )
        ensureAutomaticProfileBoost()
        return profile.copy(
            planTier = remoteTier,
            billingInterval = remoteInterval.takeUnless { remoteTier == PlanTier.FREE },
        )
    }

    fun messagingUserId(): String? =
        _uiState.value.currentUser?.firebaseDocumentId?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUid

    fun isMessagingBlocked(userId: String): Boolean {
        val user = _uiState.value.currentUser
        val social = _socialState.value
        return social.blockedUsers.any { it.id == userId } ||
            social.blockedByUsers.any { it.id == userId } ||
            user?.blockedUsers?.any { it.id == userId } == true ||
            user?.blockedByUsers?.any { it.id == userId } == true
    }

    /** Matches visible in the list — excludes either-side blocks (parity with iOS). */
    fun visibleMatches(): List<MatchDisplay> =
        _socialState.value.matches.filter { !isMessagingBlocked(it.otherUser.userId) }

    /** Conversations visible in the list — excludes either-side blocks (parity with iOS). */
    fun visibleConversations(): List<ConversationDisplay> =
        _socialState.value.conversations
            .filter { it.otherUserId.isNotBlank() && !isMessagingBlocked(it.otherUserId) }
            .distinctBy { it.id }

    fun searchMessageHistory(query: String) {
        messageSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _socialState.update { it.copy(messageHistoryMatchIds = emptySet()) }
            return
        }
        messageSearchJob = viewModelScope.launch {
            delay(300)
            val ids = runCatching {
                messageRepository.findConversationPartnerIds(trimmed)
            }.getOrDefault(emptySet())
            _socialState.update { it.copy(messageHistoryMatchIds = ids) }
        }
    }

    fun loadOlderChatMessages() {
        val partnerId = _uiState.value.activeChatPartnerId ?: return
        val state = _socialState.value
        if (!state.chatHasMoreOlder || state.isLoadingOlderChat) return
        val oldest = state.chatMessages.minByOrNull { it.createdAt.time } ?: return
        viewModelScope.launch {
            _socialState.update { it.copy(isLoadingOlderChat = true) }
            val page = runCatching {
                messageRepository.fetchConversationMessagesPage(
                    otherUserId = partnerId,
                    limit = 40,
                    before = oldest.createdAt,
                )
            }.getOrNull()
            if (page != null) {
                val existingIds = state.chatMessages.map { it.id }.toSet()
                val merged = (page.messages.filter { it.id !in existingIds } + state.chatMessages)
                    .sortedBy { it.createdAt.time }
                _socialState.update {
                    it.copy(
                        chatMessages = merged,
                        chatHasMoreOlder = page.hasMoreOlder,
                        isLoadingOlderChat = false,
                    )
                }
            } else {
                _socialState.update { it.copy(isLoadingOlderChat = false) }
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        appPrefs.soundEnabled = enabled
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        appPrefs.notificationsEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        if (enabled) {
            syncFcmToken()
        }
    }

    fun onNotificationPermissionGranted() {
        if (_uiState.value.notificationsEnabled) {
            syncFcmToken()
        }
    }

    fun showLanguagePicker() {
        _showLanguagePicker.value = true
    }

    fun hideLanguagePicker() {
        _showLanguagePicker.value = false
    }

    fun setLanguage(language: AppLanguage) {
        _showLanguagePicker.value = false
        if (appPrefs.languageCode == language.code) return
        appPrefs.setLanguage(language)
        _uiState.update { it.copy(languageCode = language.code) }
    }

    fun openChat(partnerId: String, partnerName: String, partnerPhoto: String?) {
        _uiState.update {
            it.copy(
                selectedTab = 0,
                activeChatPartnerId = partnerId,
                activeChatPartnerName = partnerName,
                activeChatPartnerPhoto = partnerPhoto,
                chatBlockedByPartner = false,
                showingBlockedUserAlert = false,
                showingNewMessageNotification = if (it.newMessageSenderId == partnerId) {
                    false
                } else {
                    it.showingNewMessageNotification
                },
                newMessageSenderId = if (it.newMessageSenderId == partnerId) null else it.newMessageSenderId,
                newMessageSenderName = if (it.newMessageSenderId == partnerId) "" else it.newMessageSenderName,
                newMessageSenderPhoto = if (it.newMessageSenderId == partnerId) null else it.newMessageSenderPhoto,
            )
        }
        appPrefs.messagesTabActive = true
        appPrefs.openConversationUserId = partnerId
        localNotifications.cancelMessageNotification(partnerId)
        resetChatSoundBaseline()
        viewModelScope.launch {
            _socialState.update {
                it.copy(
                    chatMessages = emptyList(),
                    chatHasMoreOlder = false,
                    isLoadingChatMessages = true,
                    chatImageLocalPreviews = emptyMap(),
                    uploadingImageContentKey = null,
                )
            }
            runCatching { authRepository.updateMessagesTabActive(true) }
            runCatching { authRepository.updateOpenConversation(partnerId) }
            loadChatMessages(partnerId)
            establishChatSoundBaseline()
            runCatching { messageRepository.markMessagesAsRead(partnerId) }
            refreshConversations()
            val blocked = if (AdminSender.isAdmin(partnerId)) {
                false
            } else {
                runCatching { moderationRepository.isBlockedBy(partnerId) }.getOrDefault(false)
            }
            _uiState.update {
                it.copy(
                    chatBlockedByPartner = blocked,
                    showingBlockedUserAlert = blocked &&
                        partnerId == it.activeChatPartnerId &&
                        appPrefs.shouldShowBlockedAlert(partnerId),
                )
            }
            startChatAutoRefresh(partnerId)
        }
    }

    fun closeChat() {
        stopChatAutoRefresh()
        resetChatSoundBaseline()
        _uiState.update {
            it.copy(
                activeChatPartnerId = null,
                activeChatPartnerName = null,
                activeChatPartnerPhoto = null,
                chatBlockedByPartner = false,
                showingBlockedUserAlert = false,
            )
        }
        appPrefs.openConversationUserId = null
        appPrefs.messagesTabActive = _uiState.value.selectedTab == 0
        _socialState.update {
            it.copy(
                chatMessages = emptyList(),
                chatHasMoreOlder = false,
                isLoadingChatMessages = false,
                chatImageLocalPreviews = emptyMap(),
                uploadingImageContentKey = null,
            )
        }
        viewModelScope.launch {
            runCatching { authRepository.updateOpenConversation(null) }
        }
    }

    fun dismissBlockedUserAlert(dontShowAgain: Boolean) {
        val partnerId = _uiState.value.activeChatPartnerId
        if (dontShowAgain && !partnerId.isNullOrBlank()) {
            appPrefs.skipBlockedAlert(partnerId)
        }
        _uiState.update { it.copy(showingBlockedUserAlert = false) }
    }

    fun dismissNewMessageNotification() {
        _uiState.update {
            it.copy(
                showingNewMessageNotification = false,
                newMessageSenderId = null,
                newMessageSenderName = "",
                newMessageSenderPhoto = null,
            )
        }
    }

    fun openNewMessageFromNotification() {
        val state = _uiState.value
        val senderId = state.newMessageSenderId ?: return
        val name = state.newMessageSenderName.ifBlank { "Match" }
        val photo = state.newMessageSenderPhoto
        dismissNewMessageNotification()
        suppressIncomingMessageSoundBriefly()
        openChat(senderId, name, photo)
    }

    /** Avoid replaying the chime when a notification/deep-link opens the chat. */
    fun suppressIncomingMessageSoundBriefly() {
        suppressIncomingMessageSoundUntil = System.currentTimeMillis() + 3_000L
    }

    private fun handleNewIncomingChatMessages(messages: List<MessageDisplay>) {
        if (messages.isEmpty()) return
        val incomingImages = messages.filter { it.messageType == MessageType.IMAGE }
        if (incomingImages.isNotEmpty()) {
            pendingIncomingImageSoundKeys.addAll(incomingImages.map { it.deduplicationKey })
        }
        if (messages.any { it.messageType != MessageType.IMAGE }) {
            playIncomingMessageChime()
        }
    }

    fun onIncomingChatImageDisplayed(deduplicationKey: String) {
        if (deduplicationKey.isBlank()) return
        if (!pendingIncomingImageSoundKeys.remove(deduplicationKey)) return
        playIncomingMessageChime()
    }

    private fun playIncomingMessageChime() {
        if (System.currentTimeMillis() < suppressIncomingMessageSoundUntil) return
        if (!_uiState.value.soundEnabled || !isAppInForeground) return
        val now = System.currentTimeMillis()
        if (now - lastChimeAtMs < 400L) return
        lastChimeAtMs = now
        viewModelScope.launch(Dispatchers.Main.immediate) {
            soundService.playNewMessageSound()
        }
    }

    private fun isFromCurrentUser(senderId: String): Boolean {
        if (senderId.isBlank()) return false
        val myId = messagingUserId() ?: authRepository.currentUid ?: return false
        return senderId == myId
    }

    private fun playIncomingMessageSoundIfNeeded(messageId: String, senderId: String) {
        if (messageId.isBlank() || messageId == lastPlayedMessageSoundId) return
        if (isFromCurrentUser(senderId)) return
        lastPlayedMessageSoundId = messageId
        playIncomingMessageChime()
    }

    fun updateEnergyLevel(level: Int) {
        val clamped = when {
            level <= 1 -> 0
            level <= 3 -> 2
            else -> 4
        }
        _uiState.update { state ->
            state.copy(currentUser = state.currentUser?.copy(energyLevel = clamped))
        }
        viewModelScope.launch {
            runCatching { authRepository.updateEnergyLevel(clamped) }
        }
    }

    fun sendChatMessage(text: String) {
        if (_uiState.value.chatBlockedByPartner || AdminSender.isAdmin(_uiState.value.activeChatPartnerId)) return
        val partnerId = _uiState.value.activeChatPartnerId?.trim().orEmpty()
        if (partnerId.isEmpty()) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _socialState.update { it.copy(isSendingMessage = true) }
            runCatching { messageRepository.sendTextMessage(partnerId, trimmed) }
            loadChatMessages(partnerId)
            refreshConversations()
            _socialState.update { it.copy(isSendingMessage = false) }
        }
    }

    fun sendChatImage(uri: Uri) {
        if (_uiState.value.chatBlockedByPartner || AdminSender.isAdmin(_uiState.value.activeChatPartnerId)) return
        val accountKey = authRepository.accountKey
        if (!mediaDailyLimitStore.canSend(accountKey)) {
            _showMediaLimitOverlay.value = true
            return
        }
        val partnerId = _uiState.value.activeChatPartnerId ?: return
        val currentUserId = messagingUserId() ?: return
        val partnerName = _uiState.value.activeChatPartnerName.orEmpty()
        val myPhoto = _uiState.value.currentUser?.profilePhotoURL
        val partnerPhoto = _uiState.value.activeChatPartnerPhoto
        val pendingContent = "$PENDING_IMAGE_PREFIX${UUID.randomUUID()}"
        val optimistic = MessageDisplay(
            id = pendingContent,
            content = pendingContent,
            messageType = MessageType.IMAGE,
            senderName = "You",
            recipientName = partnerName,
            senderId = currentUserId,
            recipientId = partnerId,
            createdAt = Date(),
            isRead = true,
            senderProfilePhotoURL = myPhoto,
            recipientProfilePhotoURL = partnerPhoto,
        )
        _socialState.update { state ->
            state.copy(
                chatMessages = (state.chatMessages + optimistic).sortedBy { it.createdAt.time },
                chatImageLocalPreviews = state.chatImageLocalPreviews + (pendingContent to uri.toString()),
                uploadingImageContentKey = pendingContent,
                isSendingMessage = true,
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                val url = photoStorageRepository.uploadMessageImage(uri)
                val messageId = messageRepository.sendImageMessage(partnerId, url)
                url to messageId
            }
            if (result.isSuccess) {
                mediaDailyLimitStore.recordSend(accountKey)
                val (url, messageId) = result.getOrThrow()
                _socialState.update { state ->
                    val updatedMessages = state.chatMessages.map { message ->
                        if (message.content == pendingContent) {
                            message.copy(id = messageId, content = url)
                        } else {
                            message
                        }
                    }
                    val previews = state.chatImageLocalPreviews.toMutableMap()
                    previews.remove(pendingContent)
                    previews[url] = uri.toString()
                    state.copy(
                        chatMessages = updatedMessages,
                        chatImageLocalPreviews = previews,
                        uploadingImageContentKey = null,
                        isSendingMessage = false,
                    )
                }
                refreshConversations()
            } else {
                _socialState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages.filterNot { it.content == pendingContent },
                        chatImageLocalPreviews = state.chatImageLocalPreviews - pendingContent,
                        uploadingImageContentKey = null,
                        isSendingMessage = false,
                    )
                }
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _socialState.update { it.copy(isUploadingPhoto = true) }
            runCatching {
                val url = photoStorageRepository.uploadProfilePhoto(uri)
                val current = _uiState.value.currentUser ?: return@runCatching
                val updated = current.copy(profilePhotoURL = url)
                authRepository.saveUserProfile(updated).onSuccess { saved ->
                    _uiState.update { it.copy(currentUser = saved) }
                }
            }
            _socialState.update { it.copy(isUploadingPhoto = false) }
        }
    }

    fun uploadGalleryPhoto(uri: Uri) {
        viewModelScope.launch {
            _socialState.update { it.copy(isUploadingPhoto = true) }
            runCatching {
                val url = photoStorageRepository.uploadGalleryPhoto(uri)
                val current = _uiState.value.currentUser ?: return@runCatching
                val photos = (current.photos + url).distinct().take(5)
                val updated = current.copy(photos = photos)
                authRepository.saveUserProfile(updated).onSuccess { saved ->
                    _uiState.update { it.copy(currentUser = saved) }
                }
            }
            _socialState.update { it.copy(isUploadingPhoto = false) }
        }
    }

    fun deleteGalleryPhoto(url: String) {
        viewModelScope.launch {
            _socialState.update { it.copy(isUploadingPhoto = true) }
            runCatching {
                photoStorageRepository.deletePhoto(url)
                val current = _uiState.value.currentUser ?: return@runCatching
                val photos = current.photos.filterNot { it == url }
                authRepository.saveUserProfile(current.copy(photos = photos)).onSuccess { saved ->
                    _uiState.update { it.copy(currentUser = saved) }
                }
            }
            _socialState.update { it.copy(isUploadingPhoto = false) }
        }
    }

    fun savePhotoToGallery(url: String) {
        viewModelScope.launch {
            val result = photoStorageRepository.saveImageToGallery(url)
            _uiState.update {
                it.copy(
                    photoSaveNotice = if (result.isSuccess) {
                        getApplication<Application>().getString(R.string.messages_photo_saved)
                    } else {
                        result.exceptionOrNull()?.message
                            ?: getApplication<Application>().getString(R.string.messages_photo_save_error)
                    },
                )
            }
        }
    }

    fun clearPhotoSaveNotice() {
        _uiState.update { it.copy(photoSaveNotice = null) }
    }

    fun requestBlock(profile: ProfileCard) {
        _uiState.update { it.copy(pendingBlockProfile = profile) }
    }

    fun requestBlockChatPartner() {
        val id = _uiState.value.activeChatPartnerId ?: return
        requestBlock(
            ProfileCard(
                userId = id,
                name = _uiState.value.activeChatPartnerName.orEmpty().ifBlank { "User" },
                age = 0,
                bio = "",
                interests = emptyList(),
                distance = "",
                imageURL = _uiState.value.activeChatPartnerPhoto,
                gender = "",
                interestedIn = emptyList(),
                lookingFor = emptyList(),
                photos = emptyList(),
                lifestyle = "",
                shareLifestyle = false,
                bodyType = "",
                shareBodyType = false,
                preferredAgeMin = 18,
                preferredAgeMax = 80,
                isOnline = false,
                lastSeen = java.util.Date(),
                idealFirstDate = emptyList(),
                shareIdealFirstDate = false,
            ),
        )
    }

    fun dismissBlockConfirmation() {
        _uiState.update { it.copy(pendingBlockProfile = null) }
    }

    fun completeBlockConfirmation() {
        val blockedId = _uiState.value.pendingBlockProfile?.userId
        val shouldAnimateBlockPass = blockedId != null &&
            _discoverState.value.blockPassUserId == blockedId
        _uiState.update { state ->
            state.copy(
                pendingBlockProfile = null,
                viewingProfile = if (state.viewingProfile?.userId == blockedId) {
                    null
                } else {
                    state.viewingProfile
                },
            )
        }
        if (shouldAnimateBlockPass) {
            viewModelScope.launch {
                delay(50)
                requestProgrammaticSwipe(SwipeType.PASS)
            }
        }
    }

    fun confirmBlockOnly() {
        val profile = _uiState.value.pendingBlockProfile ?: return
        viewModelScope.launch {
            runCatching { moderationRepository.blockUser(profile.userId, profile.name) }
            if (_uiState.value.activeChatPartnerId == profile.userId) closeChat()
            addBlockedUserLocally(profile.userId, profile.name)
            refreshBlockLists()
            val discover = _discoverState.value
            val isTopDiscoverCard = discover.profiles.getOrNull(discover.currentIndex)?.userId == profile.userId
            if (isTopDiscoverCard) {
                _discoverState.update { it.copy(blockPassUserId = profile.userId) }
            } else {
                removeHiddenBlockedProfilesFromDeck(setOf(profile.userId))
            }
            refreshSocial()
        }
    }

    fun confirmBlockAndReport() {
        val profile = _uiState.value.pendingBlockProfile ?: return
        if (_uiState.value.activeChatPartnerId == profile.userId) closeChat()
        addBlockedUserLocally(profile.userId, profile.name)
        val discover = _discoverState.value
        val isTopDiscoverCard = discover.profiles.getOrNull(discover.currentIndex)?.userId == profile.userId
        if (isTopDiscoverCard) {
            _discoverState.update { it.copy(blockPassUserId = profile.userId) }
        } else {
            removeHiddenBlockedProfilesFromDeck(setOf(profile.userId))
        }
        _uiState.update { it.copy(pendingBlockProfile = null) }
        _reportAfterBlock.value = true
        _reportTarget.value = profile
        viewModelScope.launch {
            runCatching { moderationRepository.blockUser(profile.userId, profile.name) }
            refreshBlockLists()
            refreshSocial()
        }
    }

    fun requestProgrammaticSwipe(action: SwipeType) {
        _discoverState.update { it.copy(programmaticSwipe = action) }
    }

    fun onProgrammaticSwipeComplete(action: SwipeType) {
        val blockUserId = _discoverState.value.blockPassUserId
        _discoverState.update { it.copy(programmaticSwipe = null) }
        if (action == SwipeType.PASS && blockUserId != null) {
            completeBlockPass(blockUserId)
        } else {
            swipe(action)
        }
    }

    private fun addBlockedUserLocally(userId: String, name: String) {
        val blocked = BlockedUser(id = userId, name = name, blockedAt = java.util.Date())
        _socialState.update { state ->
            if (state.blockedUsers.any { it.id == userId }) state
            else state.copy(blockedUsers = state.blockedUsers + blocked)
        }
        _uiState.update { state ->
            val user = state.currentUser ?: return@update state
            if (user.blockedUsers.any { it.id == userId }) return@update state
            state.copy(currentUser = user.copy(blockedUsers = user.blockedUsers + blocked))
        }
    }

    private fun removeHiddenBlockedProfilesFromDeck(blockedIds: Set<String>) {
        if (blockedIds.isEmpty()) return
        _discoverState.update { state ->
            val topId = state.profiles.getOrNull(state.currentIndex)?.userId
            if (topId != null && topId in blockedIds) return@update state
            val kept = state.profiles.filterIndexed { index, card ->
                !(index > state.currentIndex && card.userId in blockedIds)
            }
            if (kept.size == state.profiles.size) state else state.copy(profiles = kept)
        }
        persistDiscoverDeckCache()
        prefetchMoreProfilesIfNeeded()
    }

    private fun completeBlockPass(userId: String) {
        val state = _discoverState.value
        if (state.profiles.getOrNull(state.currentIndex)?.userId != userId) {
            _discoverState.update { it.copy(blockPassUserId = null) }
            return
        }
        val blockedIds = _socialState.value.blockedUsers.map { it.id }.toSet() + userId
        val nextIndex = state.currentIndex + 1
        _discoverState.update {
            val kept = it.profiles.filterIndexed { index, card ->
                !(index > it.currentIndex && card.userId in blockedIds)
            }
            it.copy(
                profiles = kept,
                currentIndex = nextIndex.coerceAtMost(kept.size),
                blockPassUserId = null,
                canUndo = false,
                lastSwipedProfile = null,
                lastSwipeType = null,
                lastSwipeCountedTowardLimit = false,
            )
        }
        persistDiscoverDeckCache()
        prefetchMoreProfilesIfNeeded()
    }

    fun showAddressSearch() {
        _addressSearchState.value = AddressSearchUiState(visible = true)
    }

    fun hideAddressSearch() {
        _addressSearchState.value = AddressSearchUiState()
    }

    fun searchAddress(query: String) {
        addressSearchJob?.cancel()
        addressSearchJob = viewModelScope.launch {
            if (query.trim().length < 2) {
                _addressSearchState.update { it.copy(isSearching = false, suggestions = emptyList()) }
                return@launch
            }
            _addressSearchState.update { it.copy(isSearching = true) }
            val results = runCatching { addressSearchRepository.searchSweden(query) }
                .getOrDefault(emptyList())
            _addressSearchState.update { it.copy(isSearching = false, suggestions = results) }
        }
    }

    fun selectAddress(suggestion: AddressSuggestion) {
        val current = _uiState.value.currentUser ?: return
        val updated = current.copy(location = suggestion.toUserLocation())
        _uiState.update { it.copy(currentUser = updated) }
        hideAddressSearch()
        viewModelScope.launch { authRepository.saveUserProfile(updated) }
    }

    fun showBlockedUsers() {
        _showBlockedUsers.value = true
        refreshBlockLists()
    }

    fun hideBlockedUsers() {
        _showBlockedUsers.value = false
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            runCatching { moderationRepository.unblockUser(userId) }
            refreshBlockLists()
            loadDiscoverDeck()
        }
    }

    fun blockChatPartner() {
        requestBlockChatPartner()
    }

    fun blockProfile(profile: ProfileCard) {
        requestBlock(profile)
    }

    fun showReport(profile: ProfileCard) {
        _reportTarget.value = profile
    }

    fun hideReport() {
        _reportTarget.value = null
        _reportAfterBlock.value = false
    }

    fun submitReport(category: String) {
        submitReport(category, details = null)
    }

    fun submitReport(category: String, details: String?) {
        val target = _reportTarget.value ?: return
        viewModelScope.launch {
            runCatching {
                submitReportForProfile(target, category, details)
            }
            hideReport()
        }
    }

    fun submitBlockReport(
        reason: ProfileReportReason,
        details: String?,
    ) {
        val target = _uiState.value.viewingProfile ?: _reportTarget.value ?: return
        _reportTarget.value = null
        _reportAfterBlock.value = false
        viewModelScope.launch {
            runCatching {
                submitReportForProfile(target, reason.storageCategory, details)
            }
            _showReportThankYou.value = true
        }
    }

    fun finishBlockReportFlow() {
        val blockedId = _discoverState.value.blockPassUserId
        _showReportThankYou.value = false
        _reportTarget.value = null
        _reportAfterBlock.value = false
        _uiState.update { it.copy(viewingProfile = null) }
        if (blockedId != null) {
            viewModelScope.launch {
                delay(50)
                requestProgrammaticSwipe(SwipeType.PASS)
            }
        }
    }

    private suspend fun submitReportForProfile(
        target: ProfileCard,
        category: String,
        details: String?,
    ) {
        val recent = runCatching {
            messageRepository.fetchConversationMessages(target.userId)
        }.getOrDefault(emptyList())
        moderationRepository.submitReport(
            reportedUserId = target.userId,
            reportedName = target.name,
            category = category,
            details = details,
            bio = target.bio,
            photoUrls = listOfNotNull(target.imageURL) + target.photos,
            recentMessages = recent,
        )
    }

    /**
     * Re-reads the on-device daily swipe quota. The store resets at local midnight
     * (`yyyy-MM-dd`), but in-memory [DiscoverUiState.dailySwipeCount] can stay at 10
     * overnight until something calls this — leaving the "You've reached your limit"
     * overlay stuck until a full process restart.
     */
    fun refreshDiscoverDailyLimitIfNeeded(reloadDeckIfUnlocked: Boolean = false) {
        if (_uiState.value.destination != AppDestination.MAIN) return
        val accountKey = authRepository.accountKey ?: return
        val wasAtLimit = _discoverState.value.dailySwipeCount >= dailyLimitStore.maxSwipesPerDay() &&
            !_discoverState.value.isReviewingTodaysPasses
        val freshCount = dailyLimitStore.currentCount(accountKey)
        val usedSecondThoughts = dailyLimitStore.hasUsedSecondThoughtsToday(accountKey)
        _discoverState.update {
            it.copy(
                dailySwipeCount = freshCount,
                hasUsedSecondThoughtsToday = usedSecondThoughts,
            )
        }
        val nowUnderLimit = freshCount < dailyLimitStore.maxSwipesPerDay()
        if (reloadDeckIfUnlocked && wasAtLimit && nowUnderLimit) {
            discoverDeckCache.clear(accountKey)
            loadDiscoverDeck()
        }
    }

    fun loadDiscoverDeck(ignorePassedFilter: Boolean = false) {
        val currentUser = _uiState.value.currentUser ?: return
        val accountKey = authRepository.accountKey
        viewModelScope.launch {
            val distanceOption = distanceStore.currentOption(accountKey)
            _discoverState.update {
                it.copy(
                    isLoading = true,
                    dailySwipeCount = dailyLimitStore.currentCount(accountKey),
                    hasUsedSecondThoughtsToday = dailyLimitStore.hasUsedSecondThoughtsToday(accountKey),
                    distanceOption = distanceOption,
                    // Leaving second-thoughts mode if a new day unlocked the quota.
                    isReviewingTodaysPasses = if (
                        dailyLimitStore.currentCount(accountKey) < dailyLimitStore.maxSwipesPerDay()
                    ) {
                        false
                    } else {
                        it.isReviewingTodaysPasses
                    },
                )
            }

            if (!ignorePassedFilter) {
                val signature = DiscoverDeckFilterSignature.from(currentUser, distanceOption)
                val cached = discoverDeckCache.loadIfValid(accountKey, signature)
                if (cached != null) {
                    _discoverState.update {
                        it.copy(
                            profiles = cached.profiles,
                            currentIndex = cached.currentIndex,
                            isLoading = false,
                            isLoadingMore = false,
                            canUndo = false,
                            lastSwipedProfile = null,
                            lastSwipeType = null,
                            isReviewingTodaysPasses = false,
                            secondThoughtsError = null,
                            hasMore = true,
                        )
                    }
                    prefetchMoreProfilesIfNeeded()
                    return@launch
                }
            }

            val result = runCatching {
                fetchEligibleDiscoverCards(
                    currentUser = currentUser,
                    ignorePassedFilter = ignorePassedFilter,
                    startAfterDocumentId = null,
                    limit = 40,
                )
            }

            result.onSuccess { (cards, lastId, hasMore) ->
                _discoverState.update {
                    it.copy(
                        profiles = cards.ifEmpty { it.profiles },
                        currentIndex = 0,
                        lastDocumentId = lastId,
                        hasMore = hasMore,
                        isLoading = false,
                        isLoadingMore = false,
                        canUndo = false,
                        lastSwipedProfile = null,
                        lastSwipeType = null,
                        isReviewingTodaysPasses = false,
                        secondThoughtsError = null,
                    )
                }
                persistDiscoverDeckCache()
                if (cards.isEmpty()) {
                    loadDemoDiscoverDeckFallback()
                }
            }.onFailure {
                loadDemoDiscoverDeckFallback()
            }
        }
    }

    private fun currentDeckSignature(): DiscoverDeckFilterSignature? {
        val user = _uiState.value.currentUser ?: return null
        return DiscoverDeckFilterSignature.from(user, _discoverState.value.distanceOption)
    }

    private fun persistDiscoverDeckCache() {
        val signature = currentDeckSignature() ?: return
        val state = _discoverState.value
        if (state.isReviewingTodaysPasses) return
        discoverDeckCache.persist(
            accountKey = authRepository.accountKey,
            profiles = state.profiles,
            currentIndex = state.currentIndex,
            signature = signature,
        )
    }

    private fun invalidateDiscoverDeckAfterProfileUpdate(user: SpectRoomUser) {
        val accountKey = authRepository.accountKey
        if (dailyLimitStore.hasReachedLimit(accountKey) && !_discoverState.value.isReviewingTodaysPasses) {
            // Defer full reload until tomorrow / second thoughts; clear signature so next load refetches.
            discoverDeckCache.clear(accountKey)
            return
        }
        discoverDeckCache.clear(accountKey)
        loadDiscoverDeck()
    }

    private fun prefetchMoreProfilesIfNeeded() {
        val state = _discoverState.value
        val accountKey = authRepository.accountKey
        val underDailyLimit = !dailyLimitStore.hasReachedLimit(accountKey)
        if (!underDailyLimit && !state.isReviewingTodaysPasses) return
        val remaining = state.profiles.size - state.currentIndex
        if (remaining > PREFETCH_REMAINING_THRESHOLD) return
        if (!state.hasMore || state.isLoadingMore || state.isLoading) return
        val currentUser = _uiState.value.currentUser ?: return
        val cursor = state.lastDocumentId ?: return
        viewModelScope.launch {
            _discoverState.update { it.copy(isLoadingMore = true) }
            val result = runCatching {
                fetchEligibleDiscoverCards(
                    currentUser = currentUser,
                    ignorePassedFilter = false,
                    startAfterDocumentId = cursor,
                    limit = PREFETCH_BATCH_SIZE,
                )
            }
            result.onSuccess { (cards, lastId, hasMore) ->
                _discoverState.update { discover ->
                    val existingIds = discover.profiles.map { it.userId }.toSet()
                    val appended = cards.filter { it.userId !in existingIds }
                    discover.copy(
                        profiles = discover.profiles + appended,
                        lastDocumentId = lastId ?: discover.lastDocumentId,
                        hasMore = hasMore,
                        isLoadingMore = false,
                    )
                }
                persistDiscoverDeckCache()
            }.onFailure {
                _discoverState.update { it.copy(isLoadingMore = false, hasMore = false) }
            }
        }
    }

    private suspend fun fetchEligibleDiscoverCards(
        currentUser: SpectRoomUser,
        ignorePassedFilter: Boolean,
        startAfterDocumentId: String?,
        limit: Int,
    ): Triple<List<ProfileCard>, String?, Boolean> {
        val filter = discoverRepository.fetchSwipeFilterState()
        val blocked = currentUser.blockedUsers.map { it.id }.toSet() +
            currentUser.blockedByUsers.map { it.id }.toSet()
        val page = discoverRepository.fetchUsersPage(
            limit = limit,
            startAfterDocumentId = startAfterDocumentId,
        )
        val maxKm = _discoverState.value.distanceOption.maxKilometers
        val cards = page.users
            .filter { user ->
                DiscoverEligibility.isUserEligibleForDeck(
                    user = user,
                    currentUser = currentUser,
                    likedUserIds = filter.likedUserIds,
                    passedUserIds = if (ignorePassedFilter) {
                        filter.permanentlyPassedUserIds
                    } else {
                        filter.passedUserIds
                    },
                    blockedUserIds = blocked,
                    ignorePassedFilter = false,
                    maxDistanceKm = maxKm,
                )
            }
            .map { discoverRepository.toProfileCard(it, currentUser, maxKm) }
        return Triple(cards, page.lastDocumentId, page.hasMore)
    }

    companion object {
        private const val PREFETCH_REMAINING_THRESHOLD = 4
        private const val PREFETCH_BATCH_SIZE = 20
        private const val CHAT_AUTO_REFRESH_INTERVAL_MS = 10_000L
        private const val PENDING_IMAGE_PREFIX = "pending://"
        /** Protect Android plan changes from being overwritten by stale Firestore cache. */
        private const val LOCAL_PLAN_WRITE_GRACE_MS = 15_000L
    }

    fun swipe(action: SwipeType) {
        if (!isCurrentProfileComplete()) return
        val state = _discoverState.value
        val accountKey = authRepository.accountKey
        val reviewing = state.isReviewingTodaysPasses
        if (!reviewing && dailyLimitStore.hasReachedLimit(accountKey)) return
        val profile = state.profiles.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            val countedTowardLimit = !reviewing
            val newCount = if (countedTowardLimit) {
                dailyLimitStore.recordSwipe(accountKey)
            } else {
                state.dailySwipeCount
            }
            val nextIndex = state.currentIndex + 1
            _discoverState.update {
                it.copy(
                    currentIndex = nextIndex.coerceAtMost(it.profiles.size),
                    dailySwipeCount = newCount,
                    canUndo = true,
                    lastSwipedProfile = profile,
                    lastSwipeType = action,
                    lastSwipeCountedTowardLimit = countedTowardLimit,
                )
            }
            persistDiscoverDeckCache()
            prefetchMoreProfilesIfNeeded()

            runCatching { discoverRepository.recordSwipe(profile.userId, action) }

            if (reviewing && nextIndex >= state.profiles.size) {
                finishSecondThoughtsReview()
            }

            if (action == SwipeType.LIKE || action == SwipeType.SUPER_LIKE) {
                val matched = runCatching { discoverRepository.checkForMatch(profile.userId) }
                    .getOrDefault(false)
                if (matched) {
                    presentMatchNotification(profile)
                    refreshMatches()
                    return@launch
                }
                repeat(4) {
                    delay(500)
                    if (discoverRepository.matchExists(profile.userId)) {
                        presentMatchNotification(profile)
                        refreshMatches()
                        return@launch
                    }
                }
            }
        }
    }

    fun undoLastSwipe() {
        val state = _discoverState.value
        val profile = state.lastSwipedProfile ?: return
        if (!state.canUndo) return
        val accountKey = authRepository.accountKey
        if (dailyLimitStore.hasReachedUndoLimit(accountKey)) {
            _showUndoLimitOverlay.value = true
            return
        }
        val count = if (state.lastSwipeCountedTowardLimit) {
            dailyLimitStore.undoLastSwipe(accountKey)
        } else {
            state.dailySwipeCount
        }
        dailyLimitStore.recordUndo(accountKey)
        _discoverState.update {
            it.copy(
                currentIndex = (it.currentIndex - 1).coerceAtLeast(0),
                dailySwipeCount = count,
                canUndo = false,
                lastSwipedProfile = null,
                lastSwipeType = null,
                lastSwipeCountedTowardLimit = false,
            )
        }
        persistDiscoverDeckCache()
        viewModelScope.launch {
            runCatching { discoverRepository.undoLastSwipe(profile.userId) }
            refreshMatches()
        }
    }

    fun useSecondThoughts() {
        val accountKey = authRepository.accountKey
        val state = _discoverState.value
        if (!dailyLimitStore.hasReachedLimit(accountKey)) return
        if (dailyLimitStore.hasUsedSecondThoughtsToday(accountKey)) return
        if (state.isReviewingTodaysPasses || state.isLoadingSecondThoughts) return
        val currentUser = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            _discoverState.update {
                it.copy(isLoadingSecondThoughts = true, secondThoughtsError = null)
            }
            val result = runCatching {
                val passedIds = discoverRepository.fetchTodaysPassedUserIds()
                if (passedIds.isEmpty()) emptyList()
                else {
                    val maxKm = _discoverState.value.distanceOption.maxKilometers
                    discoverRepository.fetchUsersByIds(passedIds)
                        .map { discoverRepository.toProfileCard(it, currentUser, maxKm) }
                        .sortedByDescending { it.matchPercentage }
                }
            }
            result.onSuccess { cards ->
                dailyLimitStore.markSecondThoughtsUsed(accountKey)
                if (cards.isEmpty()) {
                    _discoverState.update {
                        it.copy(
                            hasUsedSecondThoughtsToday = true,
                            isLoadingSecondThoughts = false,
                            secondThoughtsError = "No passes to review today",
                        )
                    }
                    return@launch
                }
                savedProfilesBeforeSecondThoughts = state.profiles
                savedCardIndexBeforeSecondThoughts = state.currentIndex
                _discoverState.update {
                    it.copy(
                        profiles = cards,
                        currentIndex = 0,
                        isReviewingTodaysPasses = true,
                        hasUsedSecondThoughtsToday = true,
                        isLoadingSecondThoughts = false,
                        canUndo = false,
                        lastSwipedProfile = null,
                        lastSwipeType = null,
                        secondThoughtsError = null,
                    )
                }
            }.onFailure {
                _discoverState.update {
                    it.copy(
                        isLoadingSecondThoughts = false,
                        secondThoughtsError = "Couldn't load today's passes",
                    )
                }
            }
        }
    }

    private fun finishSecondThoughtsReview() {
        _discoverState.update {
            it.copy(
                isReviewingTodaysPasses = false,
                profiles = savedProfilesBeforeSecondThoughts,
                currentIndex = savedCardIndexBeforeSecondThoughts
                    .coerceIn(0, maxOf(savedProfilesBeforeSecondThoughts.size - 1, 0)),
                canUndo = false,
                lastSwipedProfile = null,
                lastSwipeType = null,
            )
        }
        savedProfilesBeforeSecondThoughts = emptyList()
        savedCardIndexBeforeSecondThoughts = 0
    }

    fun setDistanceOption(option: DiscoverDistanceOption) {
        val accountKey = authRepository.accountKey
        distanceStore.setOption(accountKey, option)
        _discoverState.update { it.copy(distanceOption = option) }
        discoverDeckCache.clear(accountKey)
        if (!_discoverState.value.isReviewingTodaysPasses) {
            loadDiscoverDeck()
        }
    }

    fun refreshSocial() {
        refreshBlockLists()
        refreshMatches()
        refreshConversations()
    }

    private fun refreshBlockLists() {
        viewModelScope.launch {
            val blocked = runCatching { moderationRepository.fetchBlockedUsers() }
                .getOrDefault(emptyList())
            val blockedBy = runCatching { moderationRepository.fetchBlockedByUsers() }
                .getOrDefault(emptyList())
            _socialState.update {
                it.copy(blockedUsers = blocked, blockedByUsers = blockedBy)
            }
        }
    }

    private fun refreshMatches() {
        val currentUser = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            _socialState.update { it.copy(isLoadingMatches = true) }
            val matches = runCatching { matchRepository.fetchMatches(currentUser) }.getOrDefault(emptyList())
            _socialState.update { it.copy(matches = matches, isLoadingMatches = false) }
        }
    }

    private fun refreshConversations() {
        val currentUser = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            _socialState.update { it.copy(isLoadingMessages = true) }
            val conversations = runCatching {
                messageRepository.fetchConversationList(currentUser)
            }.getOrDefault(emptyList())
            val unread = conversations.sumOf { it.unreadCount }
            _socialState.update { it.copy(conversations = conversations, isLoadingMessages = false) }
            _uiState.update { it.copy(unreadMessagesCount = unread) }
            if (isAppInForeground) {
                if (unread <= 0) localNotifications.clearBadge()
            } else {
                if (unread > 0) localNotifications.syncLauncherBadge(unread)
                else localNotifications.clearBadge()
            }
        }
    }

    private suspend fun loadChatMessages(partnerId: String) {
        val page = runCatching {
            messageRepository.fetchConversationMessagesPage(partnerId, limit = 40)
        }.getOrNull()
        _socialState.update {
            it.copy(
                chatMessages = page?.messages.orEmpty(),
                chatHasMoreOlder = page?.hasMoreOlder == true,
                isLoadingOlderChat = false,
                isLoadingChatMessages = false,
            )
        }
    }

    private fun startChatAutoRefresh(partnerId: String) {
        stopChatAutoRefresh()
        chatAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(CHAT_AUTO_REFRESH_INTERVAL_MS)
                if (_uiState.value.activeChatPartnerId != partnerId) break
                refreshChatMessagesInBackground(partnerId)
            }
        }
    }

    private fun stopChatAutoRefresh() {
        chatAutoRefreshJob?.cancel()
        chatAutoRefreshJob = null
    }

    private suspend fun refreshChatMessagesInBackground(partnerId: String) {
        if (_uiState.value.activeChatPartnerId != partnerId) return
        val currentMessages = _socialState.value.chatMessages
        val newestTimestamp = currentMessages.maxByOrNull { it.createdAt.time }?.createdAt
        if (newestTimestamp == null) {
            if (currentMessages.isEmpty()) {
                loadChatMessages(partnerId)
            }
            return
        }

        val newerMessages = runCatching {
            messageRepository.fetchConversationMessagesNewerThan(partnerId, newestTimestamp)
        }.getOrDefault(emptyList())
        if (newerMessages.isEmpty()) return

        val existingKeys = currentMessages.map { it.deduplicationKey }.toSet()
        val uniqueNew = newerMessages.filter { it.deduplicationKey !in existingKeys }
        if (uniqueNew.isEmpty()) return

        val merged = (currentMessages + uniqueNew).sortedBy { it.createdAt.time }
        _socialState.update { it.copy(chatMessages = merged) }

        val hasNewIncoming = uniqueNew.any { it.senderId == partnerId }
        if (hasNewIncoming) {
            handleNewIncomingChatMessages(uniqueNew.filter { it.senderId == partnerId })
            runCatching { messageRepository.markMessagesAsRead(partnerId) }
            refreshConversations()
        }
    }

    private fun startRealtime() {
        stopRealtime()
        val matchesJob = viewModelScope.launch {
            matchRepository.observeRecentMatches { _uiState.value.currentUser }.collect { match ->
                refreshMatches()
                presentMatchNotification(match.otherUser, matchId = match.id)
            }
        }
        val matchCountJob = viewModelScope.launch {
            matchRepository.observeMatchCount().collect {
                refreshMatches()
            }
        }
        val messagesJob = viewModelScope.launch {
            messageRepository.observeIncomingMessageEvents().collect { event ->
                val hint = event ?: return@collect
                if (isMessagingBlocked(hint.senderId) || isFromCurrentUser(hint.senderId)) return@collect
                refreshConversations()
                val state = _uiState.value
                val openPartnerId = state.activeChatPartnerId
                val chatIsOpenWithSender = openPartnerId == hint.senderId
                if (chatIsOpenWithSender && openPartnerId != null) {
                    refreshChatMessagesInBackground(openPartnerId)
                    localNotifications.cancelMessageNotification(hint.senderId)
                    return@collect
                }
                if (isAppInForeground) {
                    playIncomingMessageSoundIfNeeded(hint.messageDocId, hint.senderId)
                }
                val onMessagesTab = state.selectedTab == 0
                val fromConversation = _socialState.value.conversations
                    .firstOrNull { it.otherUserId == hint.senderId }
                val (resolvedName, resolvedPhoto) = if (fromConversation != null) {
                    fromConversation.otherUserName to fromConversation.otherUserPhotoURL
                } else {
                    runCatching { messageRepository.resolveSenderProfile(hint.senderId) }
                        .getOrElse { "Match" to null }
                }
                val badgeCount = (_uiState.value.unreadMessagesCount).coerceAtLeast(1)
                if (!isAppInForeground && state.notificationsEnabled) {
                    localNotifications.notifyNewMessage(
                        senderId = hint.senderId,
                        senderName = resolvedName,
                        messagePreview = hint.preview.ifBlank { "New message" },
                        badgeCount = badgeCount,
                        senderPhoto = resolvedPhoto,
                    )
                    return@collect
                }
                if (!state.notificationsEnabled || onMessagesTab || state.showingNewMessageNotification) {
                    return@collect
                }
                _uiState.update {
                    it.copy(
                        showingNewMessageNotification = true,
                        newMessageSenderId = hint.senderId,
                        newMessageSenderName = resolvedName,
                        newMessageSenderPhoto = resolvedPhoto,
                    )
                }
            }
        }
        val moderationJob = viewModelScope.launch {
            authRepository.observeModerationNotices().collect { notice ->
                if (notice is ModerationNotice.Warning) {
                    persistAdminWarningMessage(notice)
                }
                if (notice != null) {
                    _moderationNotice.value = notice
                }
            }
        }
        val planTierJob = viewModelScope.launch {
            authRepository.observeBillingPlan().collect { (remoteTier, remoteInterval) ->
                // Don't let a stale snapshot undo a plan change just made on Android.
                if (isWithinLocalPlanWriteGrace()) return@collect
                val localTier = _currentPlanTier.value
                val localInterval = MockBillingService.shared.currentSubscription.interval
                val tierChanged = remoteTier != localTier
                val intervalChanged =
                    remoteInterval != null &&
                        remoteTier != PlanTier.FREE &&
                        remoteInterval != localInterval
                if (!tierChanged && !intervalChanged) return@collect
                val intervalToApply = when {
                    remoteTier == PlanTier.FREE -> BillingInterval.MONTHLY
                    remoteInterval != null -> remoteInterval
                    else -> localInterval
                }
                resolvePlanTier(remoteTier, intervalToApply)
                _uiState.update { state ->
                    val user = state.currentUser ?: return@update state
                    state.copy(
                        currentUser = user.copy(
                            planTier = remoteTier,
                            billingInterval = intervalToApply.takeUnless {
                                remoteTier == PlanTier.FREE
                            },
                        ),
                    )
                }
                refreshDiscoverStateAfterBillingChange()
                ensureAutomaticProfileBoost()
            }
        }
        realtimeJobs = listOf(matchesJob, matchCountJob, messagesJob, moderationJob, planTierJob)
    }

    private fun stopRealtime() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs = emptyList()
        stopPresenceHeartbeat()
    }

    private suspend fun refreshSession() {
        when (val gate = authRepository.evaluateAccessGate()) {
            is AccessGate.Banned -> {
                _moderationNotice.value = ModerationNotice.LoginBanned
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = null,
                        authNotice = null,
                    )
                }
                return
            }
            is AccessGate.Frozen -> {
                _moderationNotice.value = ModerationNotice.LoginBlocked(gate.until, gate.hours)
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = null,
                        authNotice = null,
                    )
                }
                return
            }
            is AccessGate.Deleted -> {
                _moderationNotice.value = ModerationNotice.LoginDeleted
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = null,
                        authNotice = null,
                    )
                }
                return
            }
            AccessGate.Allowed -> Unit
        }

        val profile = applyPlanEntitlementsFromUser(
            authRepository.fetchCurrentUserProfile()
                ?: SpectRoomUser(
                    name = authRepository.currentEmail.orEmpty().substringBefore("@"),
                    email = authRepository.currentEmail.orEmpty(),
                    firebaseDocumentId = authRepository.currentUid,
                ),
        )
        val complete = ProfileCompletion.evaluate(profile) == UserProfileStatus.COMPLETE
        val needsMfa = !mfaAuthRepository.isMfaEnrolledOrBypassed()
        // iOS: MFA enrollment before mandatory profile.
        if (needsMfa) {
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    currentUser = profile,
                    isAdmin = adminRepository.isCurrentUserAdmin(),
                    hasCompletedInitialProfile = complete || it.hasCompletedInitialProfile,
                    isNewSignup = it.isNewSignup || !complete,
                )
            }
            enterMfaEnrollment(emailAlreadyChecked = false)
            return
        }
        val destination = when {
            _uiState.value.isNewSignup && !complete -> AppDestination.MANDATORY_PROFILE
            !complete && !_uiState.value.hasCompletedInitialProfile -> AppDestination.MANDATORY_PROFILE
            else -> AppDestination.MAIN
        }
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                currentUser = profile,
                isAdmin = adminRepository.isCurrentUserAdmin(),
                hasCompletedInitialProfile = complete || it.hasCompletedInitialProfile,
                destination = destination,
                isAuthLoading = false,
                authError = null,
                authPopupTitle = null,
                showingCompleteProfileWelcome = destination == AppDestination.MANDATORY_PROFILE &&
                    appPrefs.shouldShowCompleteProfileWelcome(profile.firebaseDocumentId),
            )
        }
        if (destination == AppDestination.MAIN) {
            loadDiscoverDeck()
            refreshSocial()
            startRealtime()
            syncFcmToken()
            setOnline(true)
            ensureAutomaticProfileBoost()
            _moderationNotice.value = authRepository.fetchPendingModerationNotice()
            persistAdminWarningMessage(_moderationNotice.value)
        }
    }

    private suspend fun persistAdminWarningMessage(notice: ModerationNotice?) {
        val warning = notice as? ModerationNotice.Warning ?: return
        runCatching {
            messageRepository.persistAdminWarningMessage(
                message = warning.message,
                noticeId = warning.noticeId,
                createdAtSeconds = warning.createdAtSeconds,
            )
        }
        refreshConversations()
    }

    private fun syncFcmToken() {
        viewModelScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                authRepository.saveFcmToken(token)
            }
        }
    }

    private fun loadDemoDiscoverDeckFallback() {
        val currentUser = _uiState.value.currentUser ?: return
        val accountKey = authRepository.accountKey
        // Minimal offline preview cards so UI remains usable without Firestore data.
        val demo = listOf("Maya", "Jonas", "Elena", "Sam", "Freja").mapIndexed { index, name ->
            ProfileCard(
                userId = "demo_$index",
                name = "$name Lind",
                age = 24 + index,
                bio = "Looking for genuine connection around Stockholm.",
                interests = listOf("Photography", "Music", "Cooking"),
                distance = "${3 + index * 2} km",
                imageURL = "https://picsum.photos/seed/demo$index/800/1000",
                gender = if (index % 2 == 0) "Woman" else "Man",
                interestedIn = listOf("Anyone"),
                lookingFor = listOf("In-Person Friendship"),
                photos = listOf("https://picsum.photos/seed/demo$index/800/1000"),
                lifestyle = "independent",
                shareLifestyle = true,
                bodyType = "average",
                shareBodyType = false,
                preferredAgeMin = 22,
                preferredAgeMax = 40,
                isOnline = index % 2 == 0,
                lastSeen = java.util.Date(),
                idealFirstDate = listOf("cozyCafeMeet"),
                shareIdealFirstDate = true,
                sensoryPreferences = emptyList(),
                shareSensoryPreferences = false,
                energyLevel = listOf(0, 2, 4)[index % 3],
                matchPercentage = 40 + index * 8,
            )
        }
        _discoverState.update {
            it.copy(
                profiles = demo,
                currentIndex = 0,
                dailySwipeCount = dailyLimitStore.currentCount(accountKey),
                hasUsedSecondThoughtsToday = dailyLimitStore.hasUsedSecondThoughtsToday(accountKey),
                distanceOption = distanceStore.currentOption(accountKey),
                isLoading = false,
                canUndo = false,
            )
        }
    }
}
