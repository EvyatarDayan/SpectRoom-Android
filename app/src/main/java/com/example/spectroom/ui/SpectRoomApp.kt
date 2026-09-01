package com.example.spectroom.ui

import android.content.Intent
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spectroom.NotificationDeepLink
import com.example.spectroom.R
import com.example.spectroom.data.model.DiscoverDistanceOption
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.repository.ModerationRepository
import com.example.spectroom.ui.components.AuthFlowPreferenceBar
import com.example.spectroom.ui.components.PhotoViewerDialog
import com.example.spectroom.ui.components.SpectRoomBottomBar
import com.example.spectroom.ui.components.TabBarMainContentInset
import com.example.spectroom.ui.components.SystemWarningOverlay
import com.example.spectroom.ui.locale.ProvideAppLocale
import com.example.spectroom.ui.screens.admin.AdminConsoleSheet
import com.example.spectroom.ui.screens.auth.AuthenticationScreen
import com.example.spectroom.ui.screens.auth.MfaVerificationScreen
import com.example.spectroom.ui.screens.discover.BlockConfirmationOverlay
import com.example.spectroom.ui.screens.discover.DiscoverScreen
import com.example.spectroom.ui.screens.discover.MatchModal
import com.example.spectroom.ui.screens.discover.UserProfileModal
import com.example.spectroom.ui.screens.matches.MatchesScreen
import com.example.spectroom.ui.screens.messages.BlockedUserAlertOverlay
import com.example.spectroom.ui.screens.messages.ChatScreen
import com.example.spectroom.ui.screens.messages.MessagesScreen
import com.example.spectroom.ui.screens.messages.NewMessageComposeSheet
import com.example.spectroom.ui.screens.messages.NewMessageNotificationModal
import com.example.spectroom.ui.screens.onboarding.OnboardingScreen
import com.example.spectroom.ui.screens.profile.AddressSearchSheet
import com.example.spectroom.ui.screens.profile.DateOfBirthPickerRequest
import com.example.spectroom.ui.screens.profile.ProfileDateOfBirthPickerHost
import com.example.spectroom.ui.screens.profile.ProfileEditScreen
import com.example.spectroom.ui.screens.settings.AboutSheet
import com.example.spectroom.ui.screens.settings.HelpSupportSheet
import com.example.spectroom.ui.screens.settings.LanguagePickerSheet
import com.example.spectroom.ui.screens.settings.BlockedUsersSheet
import com.example.spectroom.ui.screens.settings.DeleteAccountSheet
import com.example.spectroom.ui.screens.settings.PrivacySafetySheet
import com.example.spectroom.ui.screens.settings.ReportedProfilesSheet
import com.example.spectroom.ui.screens.settings.ReportUserSheet
import com.example.spectroom.ui.screens.settings.SettingsScreen
import com.example.spectroom.ui.navigation.MainTab
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.SpectRoomTheme
import com.example.spectroom.viewmodel.AppDestination
import com.example.spectroom.viewmodel.AppStateViewModel

@Composable
fun SpectRoomApp(
    viewModel: AppStateViewModel = viewModel(),
    pendingNotificationDeepLink: NotificationDeepLink? = null,
    onNotificationDeepLinkConsumed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val socialState by viewModel.socialState.collectAsStateWithLifecycle()
    val addressSearchState by viewModel.addressSearchState.collectAsStateWithLifecycle()
    val showBlockedUsers by viewModel.showBlockedUsers.collectAsStateWithLifecycle()
    val reportTarget by viewModel.reportTarget.collectAsStateWithLifecycle()
    val reportAfterBlock by viewModel.reportAfterBlock.collectAsStateWithLifecycle()
    val showReportThankYou by viewModel.showReportThankYou.collectAsStateWithLifecycle()
    val infoSheet by viewModel.infoSheet.collectAsStateWithLifecycle()
    val showDeleteAccount by viewModel.showDeleteAccount.collectAsStateWithLifecycle()
    val deleteAccountSucceeded by viewModel.deleteAccountSucceeded.collectAsStateWithLifecycle()
    val photoViewer by viewModel.photoViewer.collectAsStateWithLifecycle()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsStateWithLifecycle()
    val moderationNotice by viewModel.moderationNotice.collectAsStateWithLifecycle()
    val exportShareUri by viewModel.exportShareUri.collectAsStateWithLifecycle()
    val showHelpSupport by viewModel.showHelpSupport.collectAsStateWithLifecycle()
    val helpLockedTopic by viewModel.helpLockedTopic.collectAsStateWithLifecycle()
    val isSendingSupport by viewModel.isSendingSupport.collectAsStateWithLifecycle()
    val supportSendSucceeded by viewModel.supportSendSucceeded.collectAsStateWithLifecycle()
    val supportError by viewModel.supportError.collectAsStateWithLifecycle()
    val showReportedProfiles by viewModel.showReportedProfiles.collectAsStateWithLifecycle()
    val myReports by viewModel.myReports.collectAsStateWithLifecycle()
    val isLoadingReports by viewModel.isLoadingReports.collectAsStateWithLifecycle()
    val showAdminConsole by viewModel.showAdminConsole.collectAsStateWithLifecycle()
    val adminReports by viewModel.adminReports.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val isLoadingAdmin by viewModel.isLoadingAdmin.collectAsStateWithLifecycle()
    val adminError by viewModel.adminError.collectAsStateWithLifecycle()
    val adminActionMessage by viewModel.adminActionMessage.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? ComponentActivity
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(exportShareUri) {
        val uri = exportShareUri ?: return@LaunchedEffect
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, null))
        viewModel.clearExportShareUri()
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onNotificationPermissionGranted()
        }
    }

    val pickProfilePhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::uploadProfilePhoto) }

    val pickGalleryPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::uploadGalleryPhoto) }

    val imagePickRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val requestProfilePhotoPick = {
        pickProfilePhoto.launch(imagePickRequest)
    }
    val requestGalleryPhotoPick = {
        pickGalleryPhoto.launch(imagePickRequest)
    }

    var dateOfBirthPickerRequest by remember { mutableStateOf<DateOfBirthPickerRequest?>(null) }
    val requestDateOfBirthPick: (Long, (Long) -> Unit, () -> Unit) -> Unit =
        { initialMillis, onSelected, onDismissed ->
            dateOfBirthPickerRequest = DateOfBirthPickerRequest(
                initialDateMillis = initialMillis,
                onSelected = onSelected,
                onDismissed = onDismissed,
            )
        }

    ProfileDateOfBirthPickerHost(
        request = dateOfBirthPickerRequest,
        activity = activity,
        onDismiss = { dateOfBirthPickerRequest = null },
    )

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onNotificationPermissionGranted()
        }
    }

    DisposableEffect(activity) {
        viewModel.attachActivity { activity }
        onDispose { viewModel.attachActivity { null } }
    }

    DisposableEffect(lifecycleOwner, uiState.isAuthenticated) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (uiState.isAuthenticated) viewModel.setOnline(true)
                Lifecycle.Event.ON_STOP -> if (uiState.isAuthenticated) viewModel.setOnline(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.destination) {
        if (uiState.destination == AppDestination.MAIN) {
            requestNotificationsIfNeeded()
        }
    }

    LaunchedEffect(pendingNotificationDeepLink, uiState.destination) {
        val link = pendingNotificationDeepLink ?: return@LaunchedEffect
        if (uiState.destination != AppDestination.MAIN) return@LaunchedEffect
        when (link) {
            is NotificationDeepLink.Chat -> {
                viewModel.suppressIncomingMessageSoundBriefly()
                viewModel.openChat(
                    partnerId = link.userId,
                    partnerName = link.userName.ifBlank { "Match" },
                    partnerPhoto = link.userPhoto,
                )
            }
            is NotificationDeepLink.Match -> viewModel.openMatchFromNotification(
                userId = link.userId,
                userName = link.userName.ifBlank { "Match" },
                userPhoto = link.userPhoto,
                matchId = link.matchId,
            )
        }
        onNotificationDeepLinkConsumed()
    }

    ProvideAppLocale(languageCode = uiState.languageCode) {
        SpectRoomTheme(darkTheme = uiState.isDarkMode) {
            if (!uiState.hasResolvedAuthState && !uiState.isAuthenticated) {
                LoadingScreen()
            } else when (uiState.destination) {
            AppDestination.LOADING -> LoadingScreen()
            AppDestination.ONBOARDING -> OnboardingScreen(
                onGetStarted = viewModel::completeOnboarding,
                preferenceBar = {
                    AuthFlowPreferenceBar(
                        isDarkMode = uiState.isDarkMode,
                        languageCode = uiState.languageCode,
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onLanguageClick = viewModel::showLanguagePicker,
                    )
                },
            )
            AppDestination.AUTH -> {
                val colors = LocalSpectRoomColors.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background),
                ) {
                    AuthFlowPreferenceBar(
                        isDarkMode = uiState.isDarkMode,
                        languageCode = uiState.languageCode,
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onLanguageClick = viewModel::showLanguagePicker,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        AuthenticationScreen(
                            isLoading = uiState.isAuthLoading,
                            errorMessage = uiState.authError,
                            noticeMessage = uiState.authNotice,
                            onSignIn = viewModel::signIn,
                            onSignUp = viewModel::signUp,
                            onForgotPassword = viewModel::resetPassword,
                            onClearError = viewModel::clearAuthError,
                        )
                    }
                }
            }
            AppDestination.MFA -> {
                val colors = LocalSpectRoomColors.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background),
                ) {
                    AuthFlowPreferenceBar(
                        isDarkMode = uiState.isDarkMode,
                        languageCode = uiState.languageCode,
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onLanguageClick = viewModel::showLanguagePicker,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        MfaVerificationScreen(
                            mode = uiState.mfaMode,
                            maskedPhone = uiState.mfaMaskedPhone,
                            emailAddress = uiState.mfaEmailAddress,
                            emailVerified = uiState.mfaEmailVerified,
                            resolvedPhoneE164 = uiState.mfaResolvedPhoneE164,
                            smsSent = uiState.mfaSmsSent,
                            smsFailed = uiState.mfaSmsFailed,
                            smsRateLimited = uiState.mfaSmsRateLimited,
                            editingPhone = uiState.mfaEditingPhone,
                            isLoading = uiState.isAuthLoading,
                            errorMessage = uiState.authError,
                            popupTitle = uiState.authPopupTitle,
                            onSendVerificationEmail = viewModel::sendMfaVerificationEmail,
                            onCheckEmailVerified = viewModel::checkMfaEmailVerified,
                            onResolvePhone = viewModel::resolveMfaEnrollmentPhone,
                            onSaveMissingPhone = viewModel::saveMissingMfaPhone,
                            onSaveEditedPhone = viewModel::saveEditedMfaPhone,
                            onSetEditingPhone = viewModel::setMfaEditingPhone,
                            onSendCode = viewModel::sendMfaCode,
                            onConfirm = viewModel::confirmMfa,
                            onCancel = viewModel::cancelMfa,
                            onClearError = viewModel::clearAuthError,
                        )
                    }
                }
            }
            AppDestination.MANDATORY_PROFILE -> ProfileEditScreen(
                user = uiState.currentUser,
                mandatory = true,
                isUploadingPhoto = socialState.isUploadingPhoto,
                isSaving = uiState.isSavingProfile,
                saveError = uiState.profileSaveError,
                showWelcomeOverlay = uiState.showingCompleteProfileWelcome,
                onDismissWelcome = viewModel::dismissCompleteProfileWelcome,
                onSave = viewModel::completeInitialProfile,
                onRequestProfilePhotoPick = requestProfilePhotoPick,
                onRequestGalleryPhotoPick = requestGalleryPhotoPick,
                onDeleteGalleryPhoto = viewModel::deleteGalleryPhoto,
                onSearchAddress = viewModel::showAddressSearch,
                onRequestDateOfBirthPick = requestDateOfBirthPick,
            )
            AppDestination.MAIN -> {
                val inChat = uiState.activeChatPartnerId != null
                val visibleMatches = viewModel.visibleMatches()
                MainShell(
                    selectedTab = uiState.selectedTab,
                    unreadCount = uiState.unreadMessagesCount,
                    matchesCount = visibleMatches.size,
                    showBottomBar = !inChat,
                    onTabSelected = viewModel::setSelectedTab,
                    discoverContent = {
                        DiscoverScreen(
                            state = discoverState,
                            currentEnergyLevel = uiState.currentUser?.energyLevel
                                ?: EnergyLevel.FULLY_SOCIAL,
                            profileIncomplete = !viewModel.isCurrentProfileComplete(),
                            onSwipe = viewModel::swipe,
                            onProgrammaticSwipe = viewModel::requestProgrammaticSwipe,
                            onProgrammaticSwipeComplete = viewModel::onProgrammaticSwipeComplete,
                            onUndo = viewModel::undoLastSwipe,
                            onSecondThoughts = viewModel::useSecondThoughts,
                            onOpenProfile = viewModel::openProfileDetail,
                            onUpdateEnergyLevel = viewModel::updateEnergyLevel,
                            onSetDistance = viewModel::setDistanceOption,
                            onFinishProfile = viewModel::goToProfileTab,
                        )
                    },
                    matchesContent = {
                        MatchesScreen(
                            matches = visibleMatches,
                            isLoading = socialState.isLoadingMatches,
                            isBlocked = viewModel::isMessagingBlocked,
                            onOpenMatch = { match ->
                                viewModel.openProfileDetail(match.otherUser)
                            },
                            onMessageMatch = { match ->
                                if (!viewModel.isMessagingBlocked(match.otherUser.userId)) {
                                    val user = match.otherUser
                                    viewModel.openChat(
                                        partnerId = user.userId,
                                        partnerName = user.name,
                                        partnerPhoto = user.imageURL,
                                    )
                                }
                            },
                            onExploreProfiles = { viewModel.setSelectedTab(2) },
                        )
                    },
                    messagesContent = {
                        if (inChat) {
                            ChatScreen(
                                partnerName = uiState.activeChatPartnerName.orEmpty(),
                                partnerPhotoUrl = uiState.activeChatPartnerPhoto,
                                currentUserId = viewModel.messagingUserId(),
                                messages = socialState.chatMessages,
                                isLoadingMessages = socialState.isLoadingChatMessages,
                                isSending = socialState.isSendingMessage,
                                isRecording = uiState.isRecordingVoice,
                                recordingDurationSeconds = uiState.recordingDurationSeconds,
                                composerLocked = uiState.chatBlockedByPartner,
                                hasMoreOlder = socialState.chatHasMoreOlder,
                                isLoadingOlder = socialState.isLoadingOlderChat,
                                hasPendingVoicePreview = uiState.hasPendingVoicePreview,
                                pendingVoiceDuration = uiState.pendingVoicePreviewDuration,
                                pendingVoicePath = uiState.pendingVoicePreviewPath,
                                imageLocalPreviews = socialState.chatImageLocalPreviews,
                                uploadingImageContentKey = socialState.uploadingImageContentKey,
                                shouldShowPhotoWarning = viewModel.shouldShowPhotoShareWarning(),
                                onPhotoWarningContinue = viewModel::setSkipPhotoShareWarning,
                                onBack = viewModel::closeChat,
                                onSend = viewModel::sendChatMessage,
                                onSendImage = viewModel::sendChatImage,
                                onStartRecording = viewModel::startVoiceRecording,
                                onStopRecordingAndSend = viewModel::stopVoiceRecordingForPreview,
                                onCancelRecording = viewModel::cancelVoiceRecording,
                                onPlayPendingVoice = viewModel::playPendingVoicePreview,
                                onSendPendingVoice = viewModel::sendPendingVoiceMessage,
                                onDiscardPendingVoice = viewModel::discardPendingVoicePreview,
                                onPlayVoice = viewModel::playVoiceMessage,
                                onOpenImage = viewModel::openChatImage,
                                onIncomingImageDisplayed = viewModel::onIncomingChatImageDisplayed,
                                onLoadOlder = viewModel::loadOlderChatMessages,
                                onOpenPartnerProfile = {
                                    val id = uiState.activeChatPartnerId ?: return@ChatScreen
                                    val fromMatch = socialState.matches
                                        .firstOrNull { it.otherUser.userId == id }
                                        ?.otherUser
                                    if (fromMatch != null) {
                                        viewModel.openProfileDetail(fromMatch)
                                    } else {
                                        viewModel.openProfileDetail(
                                            ProfileCard(
                                                userId = id,
                                                name = uiState.activeChatPartnerName.orEmpty(),
                                                age = 0,
                                                bio = "",
                                                interests = emptyList(),
                                                distance = "",
                                                imageURL = uiState.activeChatPartnerPhoto,
                                                gender = "",
                                                interestedIn = emptyList(),
                                                lookingFor = emptyList(),
                                                photos = listOfNotNull(uiState.activeChatPartnerPhoto),
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
                                },
                                onBlock = viewModel::blockChatPartner,
                                onReport = {
                                    val id = uiState.activeChatPartnerId ?: return@ChatScreen
                                    viewModel.showReport(
                                        ProfileCard(
                                            userId = id,
                                            name = uiState.activeChatPartnerName.orEmpty(),
                                            age = 0,
                                            bio = "",
                                            interests = emptyList(),
                                            distance = "",
                                            imageURL = uiState.activeChatPartnerPhoto,
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
                                },
                            )
                        } else {
                            MessagesScreen(
                                conversations = viewModel.visibleConversations(),
                                isLoading = socialState.isLoadingMessages,
                                hasMatches = viewModel.visibleMatches().isNotEmpty(),
                                historyMatchIds = socialState.messageHistoryMatchIds,
                                onSearchQueryChange = viewModel::searchMessageHistory,
                                onOpenConversation = { conversation ->
                                    viewModel.openChat(
                                        partnerId = conversation.otherUserId,
                                        partnerName = conversation.otherUserName,
                                        partnerPhoto = conversation.otherUserPhotoURL,
                                    )
                                },
                                onViewMatches = { viewModel.setSelectedTab(1) },
                                onExploreProfiles = { viewModel.setSelectedTab(2) },
                            )
                        }
                    },
                    profileContent = {
                        var formResetKey by remember { mutableIntStateOf(0) }
                        LaunchedEffect(Unit) {
                            viewModel.refreshCurrentUserProfile()
                        }
                        key(formResetKey) {
                            ProfileEditScreen(
                                user = uiState.currentUser,
                                isUploadingPhoto = socialState.isUploadingPhoto,
                                isSaving = uiState.isSavingProfile,
                                saveError = uiState.profileSaveError,
                                onSave = viewModel::saveProfile,
                                onCancel = {
                                    if (!uiState.isSavingProfile) {
                                        viewModel.refreshCurrentUserProfile()
                                        formResetKey++
                                        viewModel.setSelectedTab(MainTab.HOME.index)
                                    }
                                },
                                onRequestProfilePhotoPick = requestProfilePhotoPick,
                                onRequestGalleryPhotoPick = requestGalleryPhotoPick,
                                onDeleteGalleryPhoto = viewModel::deleteGalleryPhoto,
                                onSearchAddress = viewModel::showAddressSearch,
                                onRequestDateOfBirthPick = requestDateOfBirthPick,
                            )
                        }
                    },
                    settingsContent = {
                        SettingsScreen(
                            isDarkMode = uiState.isDarkMode,
                            languageCode = uiState.languageCode,
                            notificationsEnabled = uiState.notificationsEnabled,
                            blockedUsersCount = socialState.blockedUsers.size,
                            reportedProfilesCount = myReports.size,
                            onDarkModeChange = viewModel::setDarkMode,
                            onLanguageClick = viewModel::showLanguagePicker,
                            onNotificationsChange = { enabled ->
                                viewModel.setNotificationsEnabled(enabled)
                                if (enabled) requestNotificationsIfNeeded()
                            },
                            onBlockedUsers = viewModel::showBlockedUsers,
                            onReportedProfiles = viewModel::showReportedProfiles,
                            onPrivacy = { viewModel.showInfo(AppStateViewModel.InfoSheetType.PRIVACY) },
                            onHelp = { viewModel.showHelpSupport() },
                            onAbout = { viewModel.showInfo(AppStateViewModel.InfoSheetType.ABOUT) },
                            onExportMessages = viewModel::exportMessages,
                            isAdmin = uiState.isAdmin,
                            onAdmin = viewModel::showAdminConsole,
                            onDeleteAccount = viewModel::showDeleteAccount,
                            onResetUserData = viewModel::resetAllAppState,
                            onSignOut = viewModel::signOut,
                        )
                    },
                )
            }
        }

        if (uiState.showingMatchNotification && uiState.newMatchProfile != null) {
            MatchModal(
                profile = uiState.newMatchProfile!!,
                currentUserPhotoUrl = uiState.currentUser?.profilePhotoURL,
                onSendMessage = {
                    val profile = uiState.newMatchProfile!!
                    viewModel.openComposeMessage(profile)
                },
                onMaybeLater = viewModel::dismissMatchNotification,
            )
        }

        uiState.composeMessageProfile?.let { profile ->
            NewMessageComposeSheet(
                profile = profile,
                onSend = viewModel::sendComposeMessage,
                onDismiss = viewModel::dismissComposeMessage,
            )
        }

        if (uiState.showingNewMessageNotification &&
            !uiState.newMessageSenderId.isNullOrBlank() &&
            uiState.newMessageSenderName.isNotBlank()
        ) {
            NewMessageNotificationModal(
                senderName = uiState.newMessageSenderName,
                senderPhotoUrl = uiState.newMessageSenderPhoto,
                onOpen = viewModel::openNewMessageFromNotification,
                onLater = viewModel::dismissNewMessageNotification,
            )
        }

        if (uiState.showingBlockedUserAlert) {
            val firstName = uiState.activeChatPartnerName
                ?.substringBefore(' ')
                ?.ifBlank { uiState.activeChatPartnerName.orEmpty() }
                .orEmpty()
                .ifBlank { "User" }
            BlockedUserAlertOverlay(
                firstName = firstName,
                onDismiss = viewModel::dismissBlockedUserAlert,
            )
        }

        uiState.viewingProfile?.let { profile ->
            val blockFirstName = uiState.pendingBlockProfile
                ?.takeIf { it.userId == profile.userId }
                ?.let { pending ->
                    pending.name.substringBefore(' ').ifBlank { pending.name }
                }
            UserProfileModal(
                profile = profile,
                matchBreakdown = viewModel.matchBreakdownFor(profile),
                onDismiss = viewModel::closeProfileDetail,
                onMessage = {
                    viewModel.closeProfileDetail()
                    viewModel.openChat(profile.userId, profile.name, profile.imageURL)
                },
                onBlock = { viewModel.requestBlock(profile) },
                onReport = {
                    viewModel.closeProfileDetail()
                    viewModel.showReport(profile)
                },
                onOpenPhotos = viewModel::openPhotoViewer,
                onRefresh = viewModel::refreshViewingProfile,
                blockConfirmationFirstName = blockFirstName,
                onDismissBlockConfirmation = viewModel::dismissBlockConfirmation,
                onConfirmBlockOnly = viewModel::confirmBlockOnly,
                onConfirmBlockAndReport = viewModel::confirmBlockAndReport,
                onCompleteBlockConfirmation = viewModel::completeBlockConfirmation,
                showBlockReport = reportAfterBlock && reportTarget?.userId == profile.userId,
                showReportThankYou = showReportThankYou,
                onSubmitBlockReport = viewModel::submitBlockReport,
                onCancelBlockReport = viewModel::finishBlockReportFlow,
                onDismissReportThankYou = viewModel::finishBlockReportFlow,
            )
        }

        uiState.pendingBlockProfile?.let { profile ->
            if (uiState.viewingProfile?.userId != profile.userId) {
                BlockConfirmationOverlay(
                    firstName = profile.name.substringBefore(' ').ifBlank { profile.name },
                    onDismiss = viewModel::dismissBlockConfirmation,
                    onBlock = viewModel::confirmBlockOnly,
                    onBlockAndReport = viewModel::confirmBlockAndReport,
                    onBlockComplete = viewModel::completeBlockConfirmation,
                )
            }
        }

        if (addressSearchState.visible) {
            AddressSearchSheet(
                isSearching = addressSearchState.isSearching,
                suggestions = addressSearchState.suggestions,
                onQueryChange = viewModel::searchAddress,
                onSelect = viewModel::selectAddress,
                onDismiss = viewModel::hideAddressSearch,
            )
        }

        if (showBlockedUsers) {
            BlockedUsersSheet(
                blockedUsers = socialState.blockedUsers,
                onUnblock = viewModel::unblockUser,
                onDismiss = viewModel::hideBlockedUsers,
            )
        }

        reportTarget?.let { target ->
            val showInProfile = reportAfterBlock && uiState.viewingProfile?.userId == target.userId
            if (!showInProfile) {
                ReportUserSheet(
                    userName = target.name,
                    categories = ModerationRepository.reportCategories,
                    onSubmit = viewModel::submitReport,
                    onDismiss = viewModel::hideReport,
                )
            }
        }

        when (infoSheet) {
            AppStateViewModel.InfoSheetType.ABOUT -> AboutSheet(
                onDismiss = viewModel::hideInfo,
            )
            AppStateViewModel.InfoSheetType.PRIVACY -> PrivacySafetySheet(
                onDismiss = viewModel::hideInfo,
            )
            null -> Unit
        }

        if (showHelpSupport) {
            HelpSupportSheet(
                lockedTopic = helpLockedTopic,
                isSending = isSendingSupport,
                sendSucceeded = supportSendSucceeded,
                errorMessage = supportError,
                onSend = viewModel::submitSupportTicket,
                onDismiss = viewModel::hideHelpSupport,
            )
        }

        if (showReportedProfiles) {
            ReportedProfilesSheet(
                reports = myReports,
                isLoading = isLoadingReports,
                onDismiss = viewModel::hideReportedProfiles,
            )
        }

        if (showAdminConsole) {
            AdminConsoleSheet(
                reports = adminReports,
                users = adminUsers,
                isLoading = isLoadingAdmin,
                errorMessage = adminError,
                actionMessage = adminActionMessage,
                onLoad = viewModel::loadAdminData,
                onDismissReport = viewModel::adminDismissReport,
                onWarn = viewModel::adminWarn,
                onFreeze = viewModel::adminFreeze,
                onBan = viewModel::adminBan,
                onLiftFreeze = viewModel::adminLiftFreeze,
                onLiftBan = viewModel::adminLiftBan,
                onDismiss = viewModel::hideAdminConsole,
            )
        }

        if (showDeleteAccount) {
            DeleteAccountSheet(
                isLoading = uiState.isAuthLoading,
                errorMessage = uiState.authError,
                succeeded = deleteAccountSucceeded,
                onConfirm = viewModel::deleteAccount,
                onSuccessAcknowledged = viewModel::acknowledgeDeleteAccountSuccess,
                onDismiss = viewModel::hideDeleteAccount,
            )
        }

        photoViewer?.let { viewer ->
            PhotoViewerDialog(
                photos = viewer.photos,
                startIndex = viewer.startIndex,
                onDismiss = viewModel::closePhotoViewer,
                onSave = viewModel::savePhotoToGallery,
            )
        }

        uiState.photoSaveNotice?.let { notice ->
            AlertDialog(
                onDismissRequest = viewModel::clearPhotoSaveNotice,
                confirmButton = {
                    TextButton(onClick = viewModel::clearPhotoSaveNotice) {
                        Text(stringResource(R.string.settings_done))
                    }
                },
                text = { Text(notice) },
            )
        }

        if (showLanguagePicker) {
            LanguagePickerSheet(
                selectedCode = uiState.languageCode,
                onSelect = viewModel::setLanguage,
                onDismiss = viewModel::hideLanguagePicker,
            )
        }

        moderationNotice?.let { notice ->
            SystemWarningOverlay(
                notice = notice,
                onDismiss = viewModel::dismissModerationNotice,
                onContactSupport = { viewModel.openSupportFromModeration(notice) },
            )
        }
        }
    }
}

@Composable
private fun LoadingScreen() {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    )
}

@Composable
private fun MainShell(
    selectedTab: Int,
    unreadCount: Int,
    matchesCount: Int = 0,
    showBottomBar: Boolean,
    onTabSelected: (Int) -> Unit,
    discoverContent: @Composable () -> Unit,
    matchesContent: @Composable () -> Unit,
    messagesContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) TabBarMainContentInset else 0.dp),
        ) {
            when (selectedTab) {
                0 -> messagesContent()
                1 -> matchesContent()
                2 -> discoverContent()
                3 -> profileContent()
                4 -> settingsContent()
                else -> discoverContent()
            }
        }
        if (showBottomBar) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                SpectRoomBottomBar(
                    selectedTab = selectedTab,
                    unreadCount = unreadCount,
                    matchesCount = matchesCount,
                    onTabSelected = onTabSelected,
                )
            }
        }
    }
}
