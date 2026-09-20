package com.example.spectroom.ui.screens.messages

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.AdminSender
import com.example.spectroom.data.model.ChatImageLimits
import com.example.spectroom.data.model.MessageDeleteOption
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.MessageType
import com.example.spectroom.data.model.ProfileReportReason
import com.example.spectroom.ui.screens.discover.ReportReasonHeaderStyle
import com.example.spectroom.ui.screens.discover.ReportReasonOverlay
import com.example.spectroom.ui.screens.discover.ReportThankYouOverlay
import com.example.spectroom.ui.theme.ChatBubbleText
import com.example.spectroom.ui.theme.ChatComposerBar
import com.example.spectroom.ui.theme.ChatReadMore
import com.example.spectroom.ui.theme.IncomingChatBubble
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.OutgoingChatBubble
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

@Composable
fun ChatScreen(
    partnerName: String,
    partnerPhotoUrl: String?,
    currentUserId: String?,
    messages: List<MessageDisplay>,
    isLoadingMessages: Boolean = false,
    isSending: Boolean,
    isRecording: Boolean,
    recordingDurationSeconds: Double = 0.0,
    composerLocked: Boolean = false,
    isAdminThread: Boolean = false,
    hasMoreOlder: Boolean = false,
    isLoadingOlder: Boolean = false,
    hasPendingVoicePreview: Boolean = false,
    pendingVoiceDuration: Double = 0.0,
    pendingVoicePath: String? = null,
    imageLocalPreviews: Map<String, String> = emptyMap(),
    videoLocalThumbnails: Map<String, String> = emptyMap(),
    uploadingImageContentKey: String? = null,
    uploadingVideoContentKey: String? = null,
    isProcessingVideo: Boolean = false,
    pendingPreparedVideoThumbPath: String? = null,
    pendingPreparedVideoDuration: Double = 0.0,
    hasPendingVideoPreview: Boolean = false,
    onRegisterPhotoShareMediaTap: () -> Boolean = { false },
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onPrepareVideo: (Uri) -> Unit = {},
    onSendPreparedVideo: () -> Unit = {},
    onDiscardPreparedVideo: () -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    canSendMedia: Boolean = true,
    mediaRemaining: Int = 0,
    onMediaLimitReached: () -> Unit = {},
    onViewPlansFromMedia: (() -> Unit)? = null,
    onStartRecording: () -> Unit = {},
    onStopRecordingAndSend: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onPlayPendingVoice: () -> Unit = {},
    onSendPendingVoice: () -> Unit = {},
    onDiscardPendingVoice: () -> Unit = {},
    onPlayVoice: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onIncomingImageDisplayed: (String) -> Unit = {},
    onLoadOlder: () -> Unit = {},
    onOpenPartnerProfile: () -> Unit = {},
    onBlock: () -> Unit = {},
    onReport: () -> Unit = {},
    onReactMessage: (MessageDisplay, String) -> Unit = { _, _ -> },
    onSoftDeleteMessage: (MessageDisplay, MessageDeleteOption) -> Unit = { _, _ -> },
    onSubmitChatReport: (ProfileReportReason, String?, Boolean) -> Unit = { _, _, _ -> },
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    var showQuickMessages by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageTooLargeAlert by remember { mutableStateOf(false) }
    var pendingScrollPreserve by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val listState = rememberLazyListState()
    var messagePendingActions by remember { mutableStateOf<MessageDisplay?>(null) }
    var messagePendingDelete by remember { mutableStateOf<MessageDisplay?>(null) }
    var showingReportConfirmation by remember { mutableStateOf(false) }
    var showingReportReason by remember { mutableStateOf(false) }
    var showingReportThankYou by remember { mutableStateOf(false) }
    var reportReasonHeaderStyle by remember { mutableStateOf(ReportReasonHeaderStyle.REPORT_ONLY) }
    var shouldDismissAfterReportFlow by remember { mutableStateOf(false) }
    val partnerFirstName = remember(partnerName) {
        partnerName.substringBefore(' ').ifBlank { partnerName }
    }

    fun canShowMessageActions(msg: MessageDisplay): Boolean =
        !isAdminThread &&
            !msg.isDeleted &&
            !msg.content.startsWith("pending://")

    fun canCopyMessageText(msg: MessageDisplay): Boolean {
        if (msg.isDeleted || msg.content.startsWith("pending://")) return false
        return msg.messageType == MessageType.TEXT || msg.messageType == MessageType.SYSTEM
    }

    fun copyMessageText(msg: MessageDisplay) {
        if (!canCopyMessageText(msg)) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("message", msg.content))
    }

    fun finishReportThankYou() {
        val dismissChat = shouldDismissAfterReportFlow
        showingReportThankYou = false
        shouldDismissAfterReportFlow = false
        reportReasonHeaderStyle = ReportReasonHeaderStyle.REPORT_ONLY
        if (dismissChat) onBack()
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        if (mime.startsWith("video/")) {
            onPrepareVideo(uri)
            return@rememberLauncherForActivityResult
        }
        when (ChatImageLimits.validateOriginal(context, uri)) {
            ChatImageLimits.ValidationResult.TooLarge -> showImageTooLargeAlert = true
            ChatImageLimits.ValidationResult.Ok,
            ChatImageLimits.ValidationResult.Unavailable,
            -> pendingImageUri = uri
        }
    }
    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onStartRecording() }
    var showPhotoWarning by remember { mutableStateOf(false) }
    var pendingMediaAfterWarning by remember { mutableStateOf<PendingMediaAfterWarning?>(null) }
    var showMicLongPressTip by remember { mutableStateOf(false) }
    var playingVideoUrl by remember { mutableStateOf<String?>(null) }
    var expandedLongMessageIds by remember { mutableStateOf(setOf<String>()) }
    var pinToBottomOnOpen by remember(partnerName) { mutableStateOf(true) }
    var previousTailMessageId by remember(partnerName) { mutableStateOf<String?>(null) }
    var stickToBottomTrigger by remember(partnerName) { mutableIntStateOf(0) }

    fun conversationBottomIndex(): Int {
        val loadingOffset = if (isLoadingOlder) 1 else 0
        return loadingOffset + messages.size
    }

    fun startRecordingAfterWarning() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun requestSendMedia() {
        if (!canSendMedia) {
            onMediaLimitReached()
            return
        }
        if (onRegisterPhotoShareMediaTap()) {
            pendingMediaAfterWarning = PendingMediaAfterWarning.Media
            showPhotoWarning = true
        } else {
            mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        }
    }

    fun requestStartRecording() {
        if (!canSendMedia) {
            onMediaLimitReached()
            return
        }
        if (onRegisterPhotoShareMediaTap()) {
            pendingMediaAfterWarning = PendingMediaAfterWarning.Voice
            showPhotoWarning = true
            return
        }
        startRecordingAfterWarning()
    }

    val isAtBottom = remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (info.totalItemsCount - 2).coerceAtLeast(0)
        }
    }

    LaunchedEffect(partnerName) {
        pinToBottomOnOpen = true
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.id, isLoadingMessages, isLoadingOlder, pinToBottomOnOpen) {
        val tailMessageId = messages.lastOrNull()?.id
        val receivedNewMessage = tailMessageId != null &&
            previousTailMessageId != null &&
            tailMessageId != previousTailMessageId &&
            !isLoadingOlder
        val preserve = pendingScrollPreserve
        if (preserve != null) {
            pendingScrollPreserve = null
            val loadingOffset = if (isLoadingOlder) 1 else 0
            val targetIndex = (preserve.first + (messages.size - preserve.second) + loadingOffset)
                .coerceIn(0, (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            if (listState.layoutInfo.totalItemsCount > targetIndex) {
                listState.scrollToItem(targetIndex)
            }
            previousTailMessageId = tailMessageId
            return@LaunchedEffect
        }
        if (messages.isEmpty() || isLoadingMessages) {
            previousTailMessageId = tailMessageId
            return@LaunchedEffect
        }

        when {
            pinToBottomOnOpen -> {
                listState.scheduleScrollToBottom(animated = false)
                pinToBottomOnOpen = false
            }
            receivedNewMessage || isAtBottom.value -> {
                listState.scheduleScrollToBottom(animated = true)
                stickToBottomTrigger++
            }
        }
        previousTailMessageId = tailMessageId
    }

    LaunchedEffect(stickToBottomTrigger) {
        if (stickToBottomTrigger == 0 || messages.isEmpty() || isLoadingMessages) return@LaunchedEffect
        listState.scheduleScrollToBottom(animated = false)
    }

    LaunchedEffect(listState.firstVisibleItemIndex, hasMoreOlder, isLoadingOlder) {
        if (listState.firstVisibleItemIndex <= 1 && hasMoreOlder && !isLoadingOlder) {
            pendingScrollPreserve = listState.firstVisibleItemIndex to messages.size
            onLoadOlder()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        ChatHeader(
            partnerName = partnerName,
            partnerPhotoUrl = partnerPhotoUrl,
            composerLocked = composerLocked,
            isAdminThread = isAdminThread,
            onBack = onBack,
            onOpenPartnerProfile = onOpenPartnerProfile,
            onQuickMessage = { showQuickMessages = true },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ChatWallpaperBackground()
            when {
                isLoadingMessages && messages.isEmpty() -> ChatLoadingView()
                messages.isEmpty() && !composerLocked -> ChatEmptyConversationView(
                    partnerName = partnerName,
                    onQuickMessage = { showQuickMessages = true },
                )
                else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isLoadingOlder) {
                    item(key = "loading_older") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = TabSelected,
                            )
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    val index = messages.indexOfFirst { it.id == message.id }
                    val previous = messages.getOrNull(index - 1)
                    if (previous == null || !sameChatDay(previous.createdAt, message.createdAt)) {
                        ChatDateSeparator(title = chatDayTitle(message.createdAt))
                    }
                    val outgoing = message.senderId == currentUserId
                    val localImageUri = imageLocalPreviews[message.content]
                    val localVideoThumb = videoLocalThumbnails[message.content]
                        ?: message.thumbnailURL
                    val isMediaUploading = message.content == uploadingImageContentKey ||
                        message.content == uploadingVideoContentKey
                    val isLastMessage = message.id == messages.lastOrNull()?.id
                    ChatMessageRow(
                        message = message,
                        outgoing = outgoing,
                        localImageUri = localImageUri,
                        localVideoThumbnail = localVideoThumb,
                        isMediaUploading = isMediaUploading,
                        expandedLongMessageIds = expandedLongMessageIds,
                        onExpandLongMessage = { id ->
                            expandedLongMessageIds = expandedLongMessageIds + id
                        },
                        onOpenImage = onOpenImage,
                        onOpenVideo = { playingVideoUrl = it },
                        onIncomingImageDisplayed = onIncomingImageDisplayed,
                        onContentLayout = if (isLastMessage) {
                            { stickToBottomTrigger++ }
                        } else {
                            null
                        },
                        onRequestActions = if (canShowMessageActions(message) && !isMediaUploading) {
                            { messagePendingActions = message }
                        } else {
                            null
                        },
                    )
                }
                item(key = CONVERSATION_BOTTOM_ANCHOR_KEY) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            }
        }

        if (isAdminThread && composerLocked) {
            Text(
                text = stringResource(R.string.messages_composer_admin),
                color = colors.secondaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatComposerBar)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            // Keep composer visible when blocked — only disable send/media (iOS parity).
            Box(
                modifier = Modifier.alpha(if (composerLocked || isLoadingMessages) 0.55f else 1f),
            ) {
                ChatComposerBar(
                    draft = draft,
                    onDraftChange = { if (!composerLocked) draft = it },
                    isSending = isSending,
                    isRecording = isRecording,
                    enabled = !composerLocked && !isLoadingMessages,
                    onSend = {
                        if (composerLocked) return@ChatComposerBar
                        onSend(draft)
                        draft = ""
                    },
                    onAttachImage = {
                        if (!composerLocked) requestSendMedia()
                    },
                    onRequestStartRecording = {
                        if (!composerLocked) requestStartRecording()
                    },
                    onShortMicTap = {
                        if (!composerLocked) showMicLongPressTip = true
                    },
                )
            }
        }
    }

    if (showPhotoWarning) {
        PhotoShareWarningOverlay(
            onContinue = {
                val action = pendingMediaAfterWarning
                pendingMediaAfterWarning = null
                showPhotoWarning = false
                when (action) {
                    PendingMediaAfterWarning.Media -> mediaPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )
                    PendingMediaAfterWarning.Voice -> startRecordingAfterWarning()
                    null -> Unit
                }
            },
            onCancel = {
                pendingMediaAfterWarning = null
                showPhotoWarning = false
            },
        )
    }

    if (showMicLongPressTip) {
        VoiceLongPressTipOverlay(onDismiss = { showMicLongPressTip = false })
    }

    if (showImageTooLargeAlert) {
        ImageFileSizeTooLargeOverlay(onDismiss = { showImageTooLargeAlert = false })
    }

    if (showQuickMessages) {
        PreMessageOptionsSheet(
            onSelect = { message ->
                showQuickMessages = false
                draft = message
            },
            onDismiss = { showQuickMessages = false },
        )
    }

    pendingImageUri?.let { uri ->
        ImagePreviewReadySheet(
            uri = uri,
            isSending = isSending,
            mediaRemaining = mediaRemaining,
            onUpgrade = onViewPlansFromMedia,
            onSend = {
                onSendImage(uri)
                pendingImageUri = null
            },
            onDiscard = { pendingImageUri = null },
        )
    }

    if (isRecording) {
        VoiceRecordingIndicatorSheet(
            durationSeconds = recordingDurationSeconds,
            onDone = onStopRecordingAndSend,
            onDelete = onCancelRecording,
        )
    }

    if (hasPendingVoicePreview) {
        VoiceRecordingPreviewSheet(
            durationSeconds = pendingVoiceDuration,
            localPath = pendingVoicePath,
            isSending = isSending,
            mediaRemaining = mediaRemaining,
            onUpgrade = onViewPlansFromMedia,
            onSend = onSendPendingVoice,
            onDiscard = onDiscardPendingVoice,
        )
    }

    if (hasPendingVideoPreview) {
        VideoPreviewReadySheet(
            thumbnailModel = pendingPreparedVideoThumbPath,
            durationSeconds = pendingPreparedVideoDuration,
            isSending = isSending,
            mediaRemaining = mediaRemaining,
            onUpgrade = onViewPlansFromMedia,
            onSend = onSendPreparedVideo,
            onDiscard = onDiscardPreparedVideo,
        )
    }

    if (isProcessingVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.messages_video_processing),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    playingVideoUrl?.let { url ->
        ChatVideoPlayerDialog(
            videoUrl = url,
            onDismiss = { playingVideoUrl = null },
        )
    }

    when {
        messagePendingActions != null -> {
            val pending = messagePendingActions!!
            MessageQuickActionsOverlay(
                canCopy = canCopyMessageText(pending),
                onReact = { emoji ->
                    onReactMessage(pending, emoji)
                    messagePendingActions = null
                },
                onCopy = {
                    copyMessageText(pending)
                    messagePendingActions = null
                },
                onReport = {
                    messagePendingActions = null
                    showingReportConfirmation = true
                },
                onDelete = {
                    messagePendingActions = null
                    messagePendingDelete = pending
                },
                onCancel = { messagePendingActions = null },
            )
        }
        showingReportConfirmation -> {
            ChatReportConfirmationOverlay(
                firstName = partnerFirstName,
                onCancel = { showingReportConfirmation = false },
                onReport = {
                    showingReportConfirmation = false
                    shouldDismissAfterReportFlow = false
                    reportReasonHeaderStyle = ReportReasonHeaderStyle.REPORT_ONLY
                    showingReportReason = true
                },
                onReportAndBlock = {
                    showingReportConfirmation = false
                    shouldDismissAfterReportFlow = true
                    reportReasonHeaderStyle = ReportReasonHeaderStyle.BLOCKED
                    onBlock()
                    showingReportReason = true
                },
            )
        }
        showingReportReason -> {
            ReportReasonOverlay(
                firstName = partnerFirstName,
                headerStyle = reportReasonHeaderStyle,
                onCancel = {
                    showingReportReason = false
                    shouldDismissAfterReportFlow = false
                    reportReasonHeaderStyle = ReportReasonHeaderStyle.REPORT_ONLY
                },
                onSubmit = { reason, details ->
                    // Block already happened via onBlock() on report+block path.
                    onSubmitChatReport(reason, details, false)
                    showingReportReason = false
                    showingReportThankYou = true
                },
            )
        }
        showingReportThankYou -> {
            ReportThankYouOverlay(
                didBlock = shouldDismissAfterReportFlow,
                onDismiss = { finishReportThankYou() },
            )
        }
        messagePendingDelete != null -> {
            val pending = messagePendingDelete!!
            MessageDeleteOptionsOverlay(
                allowsDeleteForEveryone = pending.canDeleteForEveryone(),
                onDelete = { option ->
                    onSoftDeleteMessage(pending, option)
                    messagePendingDelete = null
                },
                onCancel = { messagePendingDelete = null },
            )
        }
    }
}

private const val LONG_MESSAGE_COLLAPSE_LIMIT = 300
private const val MIC_LONG_PRESS_MS = 1_000L
private const val COMPOSER_MIN_HEIGHT_DP = 36
private const val COMPOSER_MAX_LINES = 4
private const val CONVERSATION_BOTTOM_ANCHOR_KEY = "conversation_bottom_anchor"

private suspend fun LazyListState.scheduleScrollToBottom(animated: Boolean) {
    val delays = if (animated) listOf(0L, 50L, 200L, 500L, 800L) else listOf(0L, 80L, 200L, 500L, 800L)
    for ((index, delayMs) in delays.withIndex()) {
        if (delayMs > 0) delay(delayMs)
        scrollToBottomFully(animated = animated && index == 0)
    }
}

private suspend fun LazyListState.scrollToBottomFully(animated: Boolean) {
    withTimeoutOrNull(500) {
        snapshotFlow { layoutInfo.totalItemsCount }
            .first { it > 0 }
    }
    val lastIndex = (layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
    if (layoutInfo.totalItemsCount <= 0) return

    if (animated) {
        animateScrollToItem(lastIndex)
    } else {
        scrollToItem(lastIndex)
    }

    pinScrollToBottomEnd()
    yield()
    pinScrollToBottomEnd()
}

private suspend fun LazyListState.pinScrollToBottomEnd() {
    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    scroll {
        while (canScrollForward) {
            scrollBy(viewportHeight)
        }
    }
}

@Composable
private fun ChatLoadingView() {
    val colors = LocalSpectRoomColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TabSelected, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.messages_loading_conversation),
                color = colors.secondaryText,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ChatEmptyConversationView(
    partnerName: String,
    onQuickMessage: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val firstName = partnerName.substringBefore(' ').ifBlank { partnerName }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(12.dp, CircleShape, ambientColor = TabSelected.copy(0.16f))
                .clip(CircleShape)
                .background(TabSelected.copy(0.14f))
                .border(1.dp, TabSelected.copy(0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chat_empty_bubble),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.messages_no_messages_found, firstName),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.messages_no_messages_found_body).trimEnd(),
            fontSize = 15.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val quickMessagesLink = stringResource(R.string.messages_no_messages_found_quick_messages)
        val linkText = buildAnnotatedString {
            append("from\u00A0")
            pushStringAnnotation(tag = "quick_messages", annotation = "quick_messages")
            withStyle(
                SpanStyle(
                    color = TabSelected,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(quickMessagesLink)
            }
            pop()
            append(".")
        }
        ClickableText(
            text = linkText,
            style = TextStyle(
                fontSize = 15.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                linkText.getStringAnnotations(tag = "quick_messages", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onQuickMessage() }
            },
        )
    }
}

@Composable
private fun ChatWallpaperBackground() {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val wallpaperRes = if (colors.isDark) R.drawable.chat_back_dark else R.drawable.chat_back
    val hasWallpaper = remember(wallpaperRes) {
        runCatching { context.resources.getResourceName(wallpaperRes) }.isSuccess
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background))
        if (hasWallpaper) {
            Image(
                painter = painterResource(wallpaperRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    partnerName: String,
    partnerPhotoUrl: String?,
    composerLocked: Boolean,
    isAdminThread: Boolean,
    onBack: () -> Unit,
    onOpenPartnerProfile: () -> Unit,
    onQuickMessage: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val firstName = partnerName.substringBefore(' ').ifBlank { partnerName }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(36.dp)
                .shadow(2.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f))
                .clip(CircleShape)
                .border(1.dp, colors.border, CircleShape)
                .background(colors.card)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.size(20.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (isAdminThread) Modifier
                    else Modifier.clickable(onClick = onOpenPartnerProfile),
                )
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isAdminThread) {
                Image(
                    painter = painterResource(AdminSender.photoRes),
                    contentDescription = partnerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.text.copy(alpha = 0.12f), CircleShape),
                )
            } else if (partnerPhotoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TabSelected.copy(alpha = 0.18f))
                        .border(1.dp, colors.text.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = firstName.take(1).uppercase(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TabSelected,
                    )
                }
            } else {
                AsyncImage(
                    model = partnerPhotoUrl,
                    contentDescription = partnerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.text.copy(alpha = 0.12f), CircleShape),
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = partnerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    maxLines = 1,
                )
                if (!isAdminThread) {
                    Text(
                        text = stringResource(R.string.messages_tap_to_see_profile),
                        fontSize = 12.sp,
                        color = colors.secondaryText,
                        maxLines = 1,
                    )
                }
            }
        }
        if (!isAdminThread) {
        Row(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(50), clip = false, ambientColor = Color.Black.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(50))
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .background(colors.card)
                .clickable(enabled = !composerLocked, onClick = onQuickMessage)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_text_bubble),
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.matches_pre_message_button_short),
                color = colors.text,
                fontSize = 15.sp,
                maxLines = 1,
            )
        }
        }
    }
}

@Composable
private fun ChatDateSeparator(title: String) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text.copy(alpha = 0.75f),
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(0.08f))
                .clip(RoundedCornerShape(50))
                .background(colors.card.copy(alpha = 0.82f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private val CHAT_IMAGE_BUBBLE_SIZE = 150.dp
private val CHAT_IMAGE_CORNER = 12.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatImageMessageContent(
    imageModel: Any?,
    isImageUploading: Boolean,
    isIncoming: Boolean,
    messageKey: String,
    canOpenImage: Boolean,
    onOpenImage: (String) -> Unit,
    onLongPress: (() -> Unit)? = null,
    remoteContent: String,
    onContentLayout: (() -> Unit)?,
    onIncomingImageDisplayed: (String) -> Unit,
) {
    val density = LocalDensity.current
    var isReady by remember(imageModel) { mutableStateOf(false) }
    var aspectRatio by remember(imageModel) { mutableFloatStateOf(1f) }
    var hasNotifiedIncoming by remember(messageKey) { mutableStateOf(false) }

    fun notifyIncomingIfNeeded() {
        if (!isIncoming || hasNotifiedIncoming) return
        hasNotifiedIncoming = true
        onIncomingImageDisplayed(messageKey)
    }

    // Mirror iOS maxWidth/maxHeight 150 with .fit — frame hugs the photo's aspect ratio.
    val ratio = aspectRatio.coerceIn(0.4f, 2.5f)
    val maxPx = with(density) { CHAT_IMAGE_BUBBLE_SIZE.toPx() }
    val (widthDp, heightDp) = with(density) {
        if (ratio >= 1f) {
            CHAT_IMAGE_BUBBLE_SIZE to (maxPx / ratio).toDp()
        } else {
            (maxPx * ratio).toDp() to CHAT_IMAGE_BUBBLE_SIZE
        }
    }

    Box(
        modifier = Modifier
            .size(width = widthDp, height = heightDp)
            .clip(RoundedCornerShape(CHAT_IMAGE_CORNER))
            .then(
                when {
                    canOpenImage || onLongPress != null -> Modifier.combinedClickable(
                        onClick = {
                            if (canOpenImage) onOpenImage(remoteContent)
                        },
                        onLongClick = { onLongPress?.invoke() },
                    )
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isReady) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Gray.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = TabSelected,
                    strokeWidth = 2.5.dp,
                )
            }
        }
        AsyncImage(
            model = imageModel,
            contentDescription = "Image message",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .matchParentSize()
                .alpha(if (isReady) 1f else 0f)
                .onSizeChanged { size ->
                    if (isReady && size.height > 0) onContentLayout?.invoke()
                },
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.isSpecified && size.width > 0f && size.height > 0f) {
                    aspectRatio = size.width / size.height
                }
                isReady = true
                notifyIncomingIfNeeded()
                onContentLayout?.invoke()
            },
            onError = {
                notifyIncomingIfNeeded()
            },
        )
        if (isImageUploading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(CHAT_IMAGE_CORNER)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }
}

private val CHAT_VIDEO_BUBBLE_SIZE = 150.dp

@Composable
private fun ChatVideoBubbleContent(
    thumbnailModel: Any?,
    durationSeconds: Double?,
    isUploading: Boolean,
) {
    Box(
        modifier = Modifier
            .size(CHAT_VIDEO_BUBBLE_SIZE)
            .clip(RoundedCornerShape(CHAT_IMAGE_CORNER))
            .background(Color.Black.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = thumbnailModel,
            contentDescription = "Video message",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        if (!isUploading) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(8.dp),
            )
        }
        if (durationSeconds != null) {
            Text(
                text = formatVideoDuration(durationSeconds),
                color = Color.White,
                style = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (isUploading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageRow(
    message: MessageDisplay,
    outgoing: Boolean,
    localImageUri: String? = null,
    localVideoThumbnail: String? = null,
    isMediaUploading: Boolean = false,
    expandedLongMessageIds: Set<String>,
    onExpandLongMessage: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit = {},
    onIncomingImageDisplayed: (String) -> Unit = {},
    onContentLayout: (() -> Unit)? = null,
    onRequestActions: (() -> Unit)? = null,
) {
    val colors = LocalSpectRoomColors.current
    val bubbleColor = if (outgoing) OutgoingChatBubble else IncomingChatBubble
    val bubbleShape = ChatBubbleShape(isOutgoing = outgoing)
    val timeText = formatBubbleTime(message.createdAt)
    val isPending = message.content.startsWith("pending://")
    val canRequestActions = onRequestActions != null && !isPending && !isMediaUploading

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
        Column(
            modifier = Modifier.align(alignment),
            horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
        ) {
            if (message.isDeleted) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .width(IntrinsicSize.Max)
                        .wrapContentWidth(
                            align = if (outgoing) Alignment.End else Alignment.Start,
                        )
                        .clip(bubbleShape)
                        .background(bubbleColor, bubbleShape)
                        .padding(
                            start = if (outgoing) 10.dp else 17.dp,
                            end = if (outgoing) 17.dp else 10.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = stringResource(R.string.messages_deleted_placeholder),
                        modifier = Modifier.widthIn(max = 252.dp),
                        color = colors.secondaryText,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = timeText,
                        fontSize = 11.sp,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            } else {
                when (message.messageType) {
                    MessageType.IMAGE -> {
                        val imageModel = localImageUri ?: message.content
                        val canOpenImage = !isPending && !isMediaUploading
                        Box(
                            modifier = Modifier
                                .widthIn(max = 156.dp)
                                .clip(bubbleShape)
                                .background(bubbleColor, bubbleShape)
                                .padding(3.dp)
                                .padding(
                                    start = if (outgoing) 0.dp else 7.dp,
                                    end = if (outgoing) 7.dp else 0.dp,
                                ),
                        ) {
                            ChatImageMessageContent(
                                imageModel = imageModel,
                                isImageUploading = isMediaUploading,
                                isIncoming = !outgoing,
                                messageKey = message.deduplicationKey,
                                canOpenImage = canOpenImage,
                                onOpenImage = onOpenImage,
                                onLongPress = if (canRequestActions) onRequestActions else null,
                                remoteContent = message.content,
                                onContentLayout = onContentLayout,
                                onIncomingImageDisplayed = onIncomingImageDisplayed,
                            )
                            Text(
                                text = timeText,
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    MessageType.VIDEO -> {
                        val canOpen = !isPending && !isMediaUploading
                        Box(
                            modifier = Modifier
                                .widthIn(max = 156.dp)
                                .clip(bubbleShape)
                                .background(bubbleColor, bubbleShape)
                                .padding(3.dp)
                                .padding(
                                    start = if (outgoing) 0.dp else 7.dp,
                                    end = if (outgoing) 7.dp else 0.dp,
                                )
                                .then(
                                    when {
                                        canOpen || canRequestActions -> Modifier.combinedClickable(
                                            onClick = {
                                                if (canOpen) onOpenVideo(message.content)
                                            },
                                            onLongClick = {
                                                if (canRequestActions) onRequestActions?.invoke()
                                            },
                                        )
                                        else -> Modifier
                                    },
                                ),
                        ) {
                            ChatVideoBubbleContent(
                                thumbnailModel = localVideoThumbnail ?: message.thumbnailURL,
                                durationSeconds = message.videoDurationSeconds,
                                isUploading = isMediaUploading || isPending,
                            )
                            Text(
                                text = timeText,
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    MessageType.VOICE -> VoiceMessagePlayer(
                        audioUrl = message.content,
                        displayDurationSeconds = message.voiceDurationSeconds,
                        isOutgoing = outgoing,
                        timestampText = timeText,
                        modifier = Modifier
                            .then(
                                if (canRequestActions) {
                                    Modifier.pointerInput(message.id) {
                                        detectTapGestures(
                                            onLongPress = { onRequestActions?.invoke() },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    else -> {
                        val isLong = message.content.length > LONG_MESSAGE_COLLAPSE_LIMIT
                        val expanded = message.id in expandedLongMessageIds
                        val displayed = if (isLong && !expanded) {
                            message.content.take(LONG_MESSAGE_COLLAPSE_LIMIT)
                        } else {
                            message.content
                        }
                        Column(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .width(IntrinsicSize.Max)
                                .wrapContentWidth(
                                    align = if (outgoing) Alignment.End else Alignment.Start,
                                )
                                .clip(bubbleShape)
                                .background(bubbleColor, bubbleShape)
                                .then(
                                    if (canRequestActions) {
                                        Modifier.combinedClickable(
                                            onClick = {},
                                            onLongClick = { onRequestActions?.invoke() },
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(
                                    start = if (outgoing) 10.dp else 17.dp,
                                    end = if (outgoing) 17.dp else 10.dp,
                                    top = 6.dp,
                                    bottom = 6.dp,
                                ),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = displayed,
                                modifier = Modifier.widthIn(max = 252.dp),
                                color = ChatBubbleText,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                if (isLong && !expanded) {
                                    Text(
                                        text = stringResource(R.string.messages_read_more),
                                        color = ChatReadMore,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .clickable { onExpandLongMessage(message.id) }
                                            .padding(end = 8.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = timeText,
                                    fontSize = 11.sp,
                                    color = colors.secondaryText,
                                )
                            }
                        }
                    }
                }
            }

            if (!message.isDeleted && message.reactionSummary.isNotEmpty()) {
                // Overlap the bubble's bottom edge (iOS MessageBubble reaction chip).
                Row(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .padding(
                            start = if (outgoing) 0.dp else 8.dp,
                            end = if (outgoing) 8.dp else 0.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    message.reactionSummary.forEach { (emoji, count) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.card.copy(alpha = 0.95f))
                                .border(
                                    1.dp,
                                    colors.secondaryText.copy(alpha = 0.18f),
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(text = emoji, fontSize = 13.sp)
                            if (count > 1) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    color = colors.secondaryText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    isSending: Boolean,
    isRecording: Boolean = false,
    enabled: Boolean = true,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onRequestStartRecording: () -> Unit,
    onShortMicTap: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val hasText = draft.trim().isNotEmpty()
    var lineCount by remember { mutableIntStateOf(1) }
    val density = LocalDensity.current
    val composerLineHeight = 20.sp
    val composerMaxHeight = with(density) {
        (composerLineHeight * COMPOSER_MAX_LINES).toDp() + 10.dp
    }

    LaunchedEffect(draft) {
        if (draft.isEmpty()) {
            lineCount = 1
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatComposerBar)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = if (lineCount <= 1) Alignment.CenterVertically else Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = { if (enabled) onDraftChange(it) },
            enabled = enabled,
            onTextLayout = { result ->
                lineCount = result.lineCount.coerceAtLeast(1)
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = COMPOSER_MIN_HEIGHT_DP.dp, max = composerMaxHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.card)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                lineHeight = composerLineHeight,
                color = colors.text,
            ),
            maxLines = COMPOSER_MAX_LINES,
            cursorBrush = SolidColor(TabSelected),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    innerTextField()
                }
            },
        )
        if (hasText) {
            IconButton(
                onClick = onSend,
                enabled = enabled && !isSending,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LikeGreen),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        } else {
            IconButton(
                onClick = onAttachImage,
                enabled = enabled && !isSending,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = colors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .then(
                        if (isRecording) {
                            Modifier.border(2.dp, LikeRed.copy(alpha = 0.4f), CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .pointerInput(isSending, enabled) {
                        if (isSending || !enabled) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val releasedEarly = withTimeoutOrNull(MIC_LONG_PRESS_MS) {
                                waitForUpOrCancellation()
                            }
                            if (releasedEarly != null) {
                                onShortMicTap()
                            } else {
                                onRequestStartRecording()
                                waitForUpOrCancellation()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice",
                    tint = if (isRecording) LikeRed else colors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatBubbleTime(date: java.util.Date): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)

private enum class PendingMediaAfterWarning {
    Media,
    Voice,
}

@Composable
private fun chatDayTitle(date: java.util.Date): String {
    val day = java.util.Calendar.getInstance().apply { time = date }
    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    return when {
        sameDay(day, today) -> stringResource(R.string.messages_date_today)
        sameDay(day, yesterday) -> stringResource(R.string.messages_date_yesterday)
        else -> java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(date)
    }
}

private fun sameChatDay(a: java.util.Date, b: java.util.Date): Boolean {
    val ca = java.util.Calendar.getInstance().apply { time = a }
    val cb = java.util.Calendar.getInstance().apply { time = b }
    return sameDay(ca, cb)
}

private fun sameDay(a: java.util.Calendar, b: java.util.Calendar): Boolean =
    a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
        a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
