package com.example.spectroom.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spectroom.data.local.AppPrefsStore
import com.example.spectroom.data.local.DiscoverDailyLimitStore
import com.example.spectroom.data.local.DiscoverDeckCache
import com.example.spectroom.data.local.DiscoverDeckFilterSignature
import com.example.spectroom.data.local.DiscoverDistanceStore
import com.example.spectroom.data.local.LocalNotificationHelper
import com.example.spectroom.data.local.MessagesExport
import com.example.spectroom.data.local.MessagesExportConversation
import com.example.spectroom.data.local.SoundService
import com.example.spectroom.data.model.AdminUserProfile
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.data.model.ModerationFreezeDuration
import com.example.spectroom.data.model.ModerationNotice
import com.example.spectroom.data.model.SupportTopic
import com.example.spectroom.data.model.UserReport
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import androidx.core.content.FileProvider
import com.example.spectroom.R
import com.example.spectroom.repository.AccessGate
import com.example.spectroom.repository.AdminRepository
import com.example.spectroom.data.model.BlockedUser
import com.example.spectroom.data.model.ConversationDisplay
import com.example.spectroom.data.model.DiscoverDistanceOption
import com.example.spectroom.data.model.LookingForOption
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.data.model.MatchDisplay
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.data.model.SwipeType
import com.example.spectroom.data.model.UserProfileStatus
import com.example.spectroom.data.remote.AddressSearchRepository
import com.example.spectroom.data.remote.AddressSuggestion
import com.example.spectroom.data.remote.MfaAuthRepository
import com.example.spectroom.data.remote.MfaSignInResult
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

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
    private val dailyLimitStore = DiscoverDailyLimitStore(application)
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
    private var hasPrimedIncomingListener = false
    private var lastPresentedMatchId: String? = null
    private var presenceHeartbeatJob: Job? = null
    private var messageSearchJob: Job? = null
    private var chatAutoRefreshJob: Job? = null
    private var savedProfilesBeforeSecondThoughts: List<ProfileCard> = emptyList()
    private var savedCardIndexBeforeSecondThoughts: Int = 0

    private val _uiState = MutableStateFlow(
        AppUiState(
            isDarkMode = appPrefs.isDarkMode,
            hasCompletedOnboarding = appPrefs.hasCompletedOnboarding,
            languageCode = appPrefs.languageCode,
            notificationsEnabled = appPrefs.notificationsEnabled,
            soundEnabled = appPrefs.soundEnabled,
            destination = if (appPrefs.hasCompletedOnboarding) {
                AppDestination.LOADING
            } else {
                AppDestination.ONBOARDING
            },
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

    private val _infoSheet = MutableStateFlow<InfoSheetType?>(null)
    val infoSheet: StateFlow<InfoSheetType?> = _infoSheet.asStateFlow()

    private val _showHelpSupport = MutableStateFlow(false)
    val showHelpSupport: StateFlow<Boolean> = _showHelpSupport.asStateFlow()

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
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            isAdmin = false,
                            activeChatPartnerId = null,
                            destination = if (it.hasCompletedOnboarding) {
                                AppDestination.AUTH
                            } else {
                                AppDestination.ONBOARDING
                            },
                        )
                    }
                    _discoverState.value = DiscoverUiState()
                    _socialState.value = SocialUiState()
                } else {
                    refreshSession()
                }
            }
        }
    }

    fun attachActivity(provider: () -> Activity?) {
        activityProvider = provider
    }

    override fun onCleared() {
        voiceMessageRepository.release()
        soundService.release()
        super.onCleared()
    }

    fun completeOnboarding(dontShowAgain: Boolean = true) {
        if (dontShowAgain) {
            appPrefs.hasCompletedOnboarding = true
        }
        _uiState.update {
            it.copy(
                hasCompletedOnboarding = appPrefs.hasCompletedOnboarding,
                destination = AppDestination.AUTH,
            )
        }
    }

    fun shouldShowPhotoShareWarning(): Boolean = !appPrefs.skipPhotoShareWarning

    fun setSkipPhotoShareWarning(skip: Boolean) {
        if (skip) appPrefs.skipPhotoShareWarning = true
    }

    fun dismissModerationNotice() {
        val notice = _moderationNotice.value ?: return
        viewModelScope.launch {
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
                val needsMfa = !mfaAuthRepository.isMfaEnrolled
                val emailVerified = if (needsMfa) {
                    runCatching { mfaAuthRepository.reloadAndIsEmailVerified() }.getOrDefault(false)
                } else {
                    true
                }
                if (needsMfa && !emailVerified) {
                    runCatching { mfaAuthRepository.sendEmailVerificationForMfa() }
                }
                _uiState.update {
                    it.copy(
                        currentUser = saved,
                        hasCompletedInitialProfile = true,
                        isNewSignup = false,
                        destination = if (needsMfa) AppDestination.MFA else AppDestination.MAIN,
                        mfaMode = MfaUiMode.ENROLL,
                        mfaEmailAddress = mfaAuthRepository.currentEmail(),
                        mfaEmailVerified = emailVerified,
                        mfaResolvedPhoneE164 = "",
                        mfaSmsSent = false,
                        mfaSmsFailed = false,
                        mfaSmsRateLimited = false,
                        mfaEditingPhone = false,
                        profileSaveError = null,
                        isSavingProfile = false,
                    )
                }
                applyUnlimitedDistanceIfNeeded(saved)
                if (needsMfa) {
                    resolveMfaEnrollmentPhone()
                } else {
                    loadDiscoverDeck()
                    refreshSocial()
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
            runCatching { authRepository.fetchCurrentUserProfile() }.getOrNull()?.let { profile ->
                _uiState.update { it.copy(currentUser = profile) }
            }
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
    }

    fun setOnline(isOnline: Boolean) {
        isAppInForeground = isOnline
        appPrefs.appIsActive = isOnline
        viewModelScope.launch {
            runCatching { authRepository.updateOnlineStatus(isOnline) }
            if (isOnline) {
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

    fun submitSupportTicket(topic: SupportTopic, message: String) {
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            _isSendingSupport.value = true
            _supportError.value = null
            runCatching {
                val label = getApplication<Application>().getString(topic.labelRes)
                moderationRepository.submitSupportTicket(
                    topicLabel = "${topic.subjectPrefix} $label",
                    message = message,
                    userName = user?.name.orEmpty().ifBlank { "Unknown" },
                    userEmail = user?.email.orEmpty(),
                )
            }.onSuccess {
                _supportSendSucceeded.value = true
                delay(350)
                hideHelpSupport()
            }.onFailure { error ->
                _supportError.value = error.message ?: "Could not send message"
            }
            _isSendingSupport.value = false
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
                _uiState.update { it.copy(viewingProfile = card) }
                _socialState.update { social ->
                    social.copy(
                        matches = social.matches.map { match ->
                            if (match.otherUser.userId == profileId) {
                                match.copy(otherUser = card)
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
        _uiState.update { it.copy(authError = null, authNotice = null) }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            authRepository.resetPassword(email).onSuccess {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authNotice = getApplication<Application>()
                            .getString(R.string.auth_reset_password_sent, email.trim()),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = error.message
                            ?: getApplication<Application>().getString(R.string.auth_reset_password_error),
                    )
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            runCatching {
                when (val result = mfaAuthRepository.signInWithPossibleMfa(email, password)) {
                    is MfaSignInResult.Completed -> refreshSession()
                    is MfaSignInResult.MfaRequired -> {
                        val activity = activityProvider?.invoke()
                            ?: error("Activity required for MFA")
                        mfaAuthRepository.sendSignInMfaCode(activity)
                        _uiState.update {
                            it.copy(
                                isAuthLoading = false,
                                destination = AppDestination.MFA,
                                mfaMode = MfaUiMode.SIGN_IN,
                                mfaMaskedPhone = result.maskedPhone,
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isAuthLoading = false, authError = error.message ?: "Sign-in failed")
                }
            }
        }
    }

    fun confirmMfa(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            runCatching {
                when (_uiState.value.mfaMode) {
                    MfaUiMode.SIGN_IN -> mfaAuthRepository.confirmSignInMfa(code)
                    MfaUiMode.ENROLL -> mfaAuthRepository.confirmEnroll(code)
                }
                appPrefs.pendingMfaPhoneE164 = null
                refreshSession()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = error.message ?: "Verification failed",
                        mfaSmsRateLimited = mfaAuthRepository.isRateLimited(error),
                    )
                }
            }
        }
    }

    fun sendMfaCode(phoneE164: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthLoading = true,
                    authError = null,
                    mfaSmsFailed = false,
                    mfaSmsRateLimited = false,
                )
            }
            runCatching {
                val activity = activityProvider?.invoke() ?: error("Activity required for MFA")
                when (_uiState.value.mfaMode) {
                    MfaUiMode.SIGN_IN -> mfaAuthRepository.sendSignInMfaCode(activity)
                    MfaUiMode.ENROLL -> {
                        if (!_uiState.value.mfaEmailVerified) {
                            error("Verify your email before enabling SMS MFA")
                        }
                        val phone = phoneE164?.trim().orEmpty()
                            .ifBlank { _uiState.value.mfaResolvedPhoneE164 }
                        require(phone.startsWith("+") && phone.length >= 10) { "Enter a valid E.164 phone" }
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
                _uiState.update {
                    it.copy(
                        authError = if (mfaAuthRepository.isRateLimited(error)) null else {
                            error.message ?: "Could not send code"
                        },
                        mfaSmsFailed = _uiState.value.mfaMode == MfaUiMode.ENROLL,
                        mfaSmsRateLimited = mfaAuthRepository.isRateLimited(error),
                        mfaSmsSent = false,
                    )
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
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            runCatching {
                require(phoneE164.startsWith("+") && phoneE164.length >= 10)
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
                    it.copy(authError = error.message ?: "Could not save phone")
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
        }
    }

    fun saveEditedMfaPhone(phoneE164: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, mfaEditingPhone = false) }
            runCatching {
                require(phoneE164.startsWith("+") && phoneE164.length >= 10)
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
                        authError = error.message ?: "Could not update phone",
                    )
                }
            }
        }
    }

    fun setMfaEditingPhone(editing: Boolean) {
        _uiState.update { it.copy(mfaEditingPhone = editing, authError = null) }
    }

    fun sendMfaVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            runCatching {
                mfaAuthRepository.sendEmailVerificationForMfa()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(authError = error.message ?: "Could not send verification email")
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
        }
    }

    fun checkMfaEmailVerified() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            runCatching {
                val verified = mfaAuthRepository.reloadAndIsEmailVerified()
                _uiState.update {
                    it.copy(
                        mfaEmailVerified = verified,
                        authError = if (verified) {
                            null
                        } else {
                            "Email not verified yet. Open the link we sent, then try again."
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(authError = error.message ?: "Could not check email verification")
                }
            }
            _uiState.update { it.copy(isAuthLoading = false) }
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
            val user = _uiState.value.currentUser
            val profileComplete = user != null &&
                ProfileCompletion.evaluate(user) == UserProfileStatus.COMPLETE
            if (!profileComplete) {
                authRepository.signOut()
            }
            appPrefs.pendingMfaPhoneE164 = null
            _uiState.update {
                it.copy(
                    destination = if (profileComplete) AppDestination.MAIN else AppDestination.AUTH,
                    isAuthLoading = false,
                    authError = null,
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
            val verified = runCatching { mfaAuthRepository.reloadAndIsEmailVerified() }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    destination = AppDestination.MFA,
                    mfaMode = MfaUiMode.ENROLL,
                    authError = null,
                    mfaEmailAddress = mfaAuthRepository.currentEmail(),
                    mfaEmailVerified = verified,
                    mfaResolvedPhoneE164 = "",
                    mfaSmsSent = false,
                    mfaSmsFailed = false,
                    mfaSmsRateLimited = false,
                    mfaEditingPhone = false,
                )
            }
            if (!verified) {
                runCatching { mfaAuthRepository.sendEmailVerificationForMfa() }
            }
            resolveMfaEnrollmentPhone()
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

    fun startVoiceRecording() {
        if (_uiState.value.chatBlockedByPartner) return
        viewModelScope.launch {
            voiceMessageRepository.startRecording().onSuccess {
                _uiState.update { it.copy(isRecordingVoice = true) }
            }.onFailure {
                _uiState.update { state -> state.copy(authError = it.message) }
            }
        }
    }

    fun stopVoiceRecordingAndSend() {
        stopVoiceRecordingForPreview()
    }

    fun stopVoiceRecordingForPreview() {
        if (_uiState.value.chatBlockedByPartner) {
            cancelVoiceRecording()
            return
        }
        val recorded = voiceMessageRepository.stopRecording()
        _uiState.update { it.copy(isRecordingVoice = false) }
        if (recorded == null) return
        pendingVoiceFile = recorded.first
        pendingVoiceDuration = recorded.second
        _uiState.update {
            it.copy(
                hasPendingVoicePreview = true,
                pendingVoicePreviewDuration = recorded.second,
                pendingVoicePreviewPath = recorded.first.absolutePath,
            )
        }
    }

    fun playPendingVoicePreview() {
        // Handled by VoiceMessagePlayer in the preview sheet.
    }

    fun sendPendingVoiceMessage() {
        if (_uiState.value.chatBlockedByPartner) {
            discardPendingVoicePreview()
            return
        }
        val partnerId = _uiState.value.activeChatPartnerId ?: return
        val file = pendingVoiceFile ?: return
        val duration = pendingVoiceDuration
        viewModelScope.launch {
            _socialState.update { it.copy(isSendingMessage = true) }
            runCatching {
                val url = voiceMessageRepository.uploadVoice(file)
                messageRepository.sendVoiceMessage(partnerId, url, duration)
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
            )
        }
    }

    fun cancelVoiceRecording() {
        voiceMessageRepository.cancelRecording()
        _uiState.update {
            it.copy(
                isRecordingVoice = false,
                hasPendingVoicePreview = false,
                pendingVoicePreviewDuration = 0.0,
                pendingVoicePreviewPath = null,
            )
        }
    }

    fun playVoiceMessage(url: String) {
        voiceMessageRepository.play(url)
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            when (val result = authRepository.signUp(name, email, password)) {
                is AuthResult.Success -> {
                    val uid = authRepository.currentUid
                    if (!uid.isNullOrBlank()) {
                        appPrefs.prepareCompleteProfileWelcomeForSignup(uid)
                    }
                    _uiState.update {
                        it.copy(
                            isNewSignup = true,
                            hasCompletedInitialProfile = false,
                            isAuthLoading = false,
                            showingCompleteProfileWelcome = true,
                        )
                    }
                    refreshSession()
                }
                is AuthResult.MfaRequired ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                is AuthResult.Error ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                is AuthResult.Banned ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                is AuthResult.Frozen ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
                is AuthResult.Deleted ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = result.message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            setOnline(false)
            stopRealtime()
            authRepository.signOut()
            _discoverState.value = DiscoverUiState()
            _socialState.value = SocialUiState()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    isAdmin = false,
                    isNewSignup = false,
                    destination = AppDestination.AUTH,
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
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { messageRepository.sendTextMessage(profile.userId, trimmed) }
            _uiState.update { it.copy(composeMessageProfile = null) }
            openChat(profile.userId, profile.name, profile.imageURL)
        }
    }

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
        _socialState.value.conversations.filter { !isMessagingBlocked(it.otherUserId) }

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
        viewModelScope.launch {
            _socialState.update {
                it.copy(
                    chatMessages = emptyList(),
                    chatHasMoreOlder = false,
                    isLoadingChatMessages = true,
                )
            }
            runCatching { authRepository.updateMessagesTabActive(true) }
            runCatching { authRepository.updateOpenConversation(partnerId) }
            loadChatMessages(partnerId)
            runCatching { messageRepository.markMessagesAsRead(partnerId) }
            refreshConversations()
            val blocked = runCatching { moderationRepository.isBlockedBy(partnerId) }.getOrDefault(false)
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
        openChat(senderId, name, photo)
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
        if (_uiState.value.chatBlockedByPartner) return
        val partnerId = _uiState.value.activeChatPartnerId ?: return
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
        if (_uiState.value.chatBlockedByPartner) return
        val partnerId = _uiState.value.activeChatPartnerId ?: return
        viewModelScope.launch {
            _socialState.update { it.copy(isSendingMessage = true, isUploadingPhoto = true) }
            runCatching {
                val url = photoStorageRepository.uploadMessageImage(uri)
                messageRepository.sendImageMessage(partnerId, url)
            }
            loadChatMessages(partnerId)
            refreshConversations()
            _socialState.update { it.copy(isSendingMessage = false, isUploadingPhoto = false) }
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

    fun confirmBlockOnly() {
        val profile = _uiState.value.pendingBlockProfile ?: return
        viewModelScope.launch {
            runCatching { moderationRepository.blockUser(profile.userId, profile.name) }
            if (_uiState.value.activeChatPartnerId == profile.userId) closeChat()
            if (_uiState.value.viewingProfile?.userId == profile.userId) closeProfileDetail()
            loadDiscoverDeck()
            refreshSocial()
        }
    }

    fun confirmBlockAndReport() {
        val profile = _uiState.value.pendingBlockProfile ?: return
        viewModelScope.launch {
            runCatching { moderationRepository.blockUser(profile.userId, profile.name) }
            if (_uiState.value.activeChatPartnerId == profile.userId) closeChat()
            closeProfileDetail()
            _uiState.update { it.copy(pendingBlockProfile = null) }
            loadDiscoverDeck()
            refreshSocial()
            showReport(profile)
        }
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
    }

    fun submitReport(category: String) {
        val target = _reportTarget.value ?: return
        viewModelScope.launch {
            runCatching {
                val recent = runCatching {
                    messageRepository.fetchConversationMessages(target.userId)
                }.getOrDefault(emptyList())
                moderationRepository.submitReport(
                    reportedUserId = target.userId,
                    reportedName = target.name,
                    category = category,
                    details = null,
                    bio = target.bio,
                    photoUrls = listOfNotNull(target.imageURL) + target.photos,
                    recentMessages = recent,
                )
            }
            hideReport()
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
        val count = if (state.lastSwipeCountedTowardLimit) {
            dailyLimitStore.undoLastSwipe(accountKey)
        } else {
            state.dailySwipeCount
        }
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

        val currentUserId = _uiState.value.currentUser?.firebaseDocumentId
        val hasNewIncoming = uniqueNew.any { it.senderId != currentUserId }
        if (hasNewIncoming) {
            runCatching { messageRepository.markMessagesAsRead(partnerId) }
            refreshConversations()
        }
    }

    private fun startRealtime() {
        stopRealtime()
        hasPrimedIncomingListener = false
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
            messageRepository.observeIncomingMessageEvents().collect { hint ->
                if (!hasPrimedIncomingListener) {
                    hasPrimedIncomingListener = true
                    return@collect
                }
                if (hint == null) return@collect
                if (isMessagingBlocked(hint.senderId)) return@collect
                refreshConversations()
                val state = _uiState.value
                val openPartnerId = state.activeChatPartnerId
                if (openPartnerId == hint.senderId) {
                    refreshChatMessagesInBackground(openPartnerId)
                    localNotifications.cancelMessageNotification(hint.senderId)
                    return@collect
                }
                val onMessagesTab = state.selectedTab == 0
                if (state.soundEnabled && !onMessagesTab && isAppInForeground) {
                    soundService.playNewMessageSound()
                }
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
                if (notice != null) {
                    _moderationNotice.value = notice
                }
            }
        }
        realtimeJobs = listOf(matchesJob, matchCountJob, messagesJob, moderationJob)
    }

    private fun stopRealtime() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs = emptyList()
        stopPresenceHeartbeat()
    }

    private suspend fun refreshSession() {
        when (val gate = authRepository.evaluateAccessGate()) {
            is AccessGate.Banned -> {
                authRepository.signOut()
                _moderationNotice.value = ModerationNotice.LoginBanned
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = getApplication<Application>().getString(R.string.auth_error_account_banned),
                    )
                }
                return
            }
            is AccessGate.Frozen -> {
                authRepository.signOut()
                _moderationNotice.value = ModerationNotice.LoginBlocked(gate.until)
                val unlock = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(gate.until)
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = getApplication<Application>()
                            .getString(R.string.auth_error_account_frozen, unlock),
                    )
                }
                return
            }
            is AccessGate.Deleted -> {
                authRepository.signOut()
                _moderationNotice.value = ModerationNotice.LoginDeleted
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        destination = AppDestination.AUTH,
                        isAuthLoading = false,
                        authError = getApplication<Application>().getString(R.string.auth_error_account_deleted),
                    )
                }
                return
            }
            AccessGate.Allowed -> Unit
        }

        val profile = authRepository.fetchCurrentUserProfile()
            ?: SpectRoomUser(
                name = authRepository.currentEmail.orEmpty().substringBefore("@"),
                email = authRepository.currentEmail.orEmpty(),
                firebaseDocumentId = authRepository.currentUid,
            )
        val complete = ProfileCompletion.evaluate(profile) == UserProfileStatus.COMPLETE
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
            _moderationNotice.value = authRepository.fetchPendingModerationNotice()
        }
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
