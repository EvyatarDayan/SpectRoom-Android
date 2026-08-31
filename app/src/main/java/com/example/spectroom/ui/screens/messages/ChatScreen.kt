package com.example.spectroom.ui.screens.messages

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.MessageType
import com.example.spectroom.ui.theme.ChatBubbleText
import com.example.spectroom.ui.theme.ChatComposerBar
import com.example.spectroom.ui.theme.ChatReadMore
import com.example.spectroom.ui.theme.IncomingChatBubble
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.OutgoingChatBubble
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ChatScreen(
    partnerName: String,
    partnerPhotoUrl: String?,
    currentUserId: String?,
    messages: List<MessageDisplay>,
    isLoadingMessages: Boolean = false,
    isSending: Boolean,
    isRecording: Boolean,
    composerLocked: Boolean = false,
    hasMoreOlder: Boolean = false,
    isLoadingOlder: Boolean = false,
    hasPendingVoicePreview: Boolean = false,
    pendingVoiceDuration: Double = 0.0,
    pendingVoicePath: String? = null,
    shouldShowPhotoWarning: Boolean = true,
    onPhotoWarningContinue: (dontShowAgain: Boolean) -> Unit = {},
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecordingAndSend: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onPlayPendingVoice: () -> Unit = {},
    onSendPendingVoice: () -> Unit = {},
    onDiscardPendingVoice: () -> Unit = {},
    onPlayVoice: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onLoadOlder: () -> Unit = {},
    onOpenPartnerProfile: () -> Unit = {},
    onBlock: () -> Unit = {},
    onReport: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }
    var showQuickMessages by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingScrollPreserve by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val listState = rememberLazyListState()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { pendingImageUri = it } }
    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onStartRecording() }
    var showPhotoWarning by remember { mutableStateOf(false) }
    var photoWarningActive by remember { mutableStateOf(shouldShowPhotoWarning) }
    var showMicLongPressTip by remember { mutableStateOf(false) }
    var expandedLongMessageIds by remember { mutableStateOf(setOf<String>()) }
    var pinToBottomOnOpen by remember(partnerName) { mutableStateOf(true) }

    fun conversationBottomIndex(): Int {
        val loadingOffset = if (isLoadingOlder) 1 else 0
        return loadingOffset + messages.size
    }

    fun requestSendImage() {
        if (photoWarningActive) {
            showPhotoWarning = true
        } else {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun requestStartRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
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

    LaunchedEffect(messages.size, isLoadingMessages, isLoadingOlder, pinToBottomOnOpen) {
        val preserve = pendingScrollPreserve
        if (preserve != null) {
            pendingScrollPreserve = null
            val loadingOffset = if (isLoadingOlder) 1 else 0
            listState.scrollToItem(
                index = (preserve.first + (messages.size - preserve.second) + loadingOffset).coerceAtLeast(0),
            )
            return@LaunchedEffect
        }
        if (messages.isEmpty() || isLoadingMessages) return@LaunchedEffect

        val bottomIndex = conversationBottomIndex()
        if (pinToBottomOnOpen) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > bottomIndex }
            listState.scrollToItem(bottomIndex)
            pinToBottomOnOpen = false
        } else if (isAtBottom.value) {
            listState.animateScrollToItem(bottomIndex)
        }
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
                    ChatMessageRow(
                        message = message,
                        outgoing = outgoing,
                        expandedLongMessageIds = expandedLongMessageIds,
                        onExpandLongMessage = { id ->
                            expandedLongMessageIds = expandedLongMessageIds + id
                        },
                        onOpenImage = onOpenImage,
                    )
                }
                item(key = CONVERSATION_BOTTOM_ANCHOR_KEY) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            }
        }

        if (composerLocked) {
            Text(
                text = stringResource(R.string.messages_composer_blocked),
                color = colors.secondaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatComposerBar)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textAlign = TextAlign.Center,
            )
        } else if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LikeRed.copy(alpha = 0.12f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recording…", color = LikeRed, modifier = Modifier.weight(1f))
                Text(
                    "Cancel",
                    color = colors.secondaryText,
                    modifier = Modifier
                        .clickable(onClick = onCancelRecording)
                        .padding(end = 16.dp),
                )
                Text(
                    "Stop",
                    color = TabSelected,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onStopRecordingAndSend),
                )
            }
        } else {
            ChatComposerBar(
                draft = draft,
                onDraftChange = { draft = it },
                isSending = isSending,
                onSend = {
                    onSend(draft)
                    draft = ""
                },
                onAttachImage = { requestSendImage() },
                onRequestStartRecording = { requestStartRecording() },
                onShortMicTap = { showMicLongPressTip = true },
            )
        }
    }

    if (showPhotoWarning) {
        PhotoShareWarningOverlay(
            onContinue = { dontShowAgain ->
                showPhotoWarning = false
                onPhotoWarningContinue(dontShowAgain)
                if (dontShowAgain) photoWarningActive = false
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCancel = { showPhotoWarning = false },
        )
    }

    if (showMicLongPressTip) {
        AlertDialog(
            onDismissRequest = { showMicLongPressTip = false },
            title = { Text(stringResource(R.string.messages_voice_long_press_tip_title)) },
            text = { Text(stringResource(R.string.messages_voice_long_press_tip_message)) },
            confirmButton = {
                TextButton(onClick = { showMicLongPressTip = false }) {
                    Text(stringResource(R.string.messages_voice_long_press_tip_got_it))
                }
            },
        )
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
            onSend = {
                onSendImage(uri)
                pendingImageUri = null
            },
            onDiscard = { pendingImageUri = null },
        )
    }

    if (hasPendingVoicePreview) {
        VoiceRecordingPreviewSheet(
            durationSeconds = pendingVoiceDuration,
            localPath = pendingVoicePath,
            isSending = isSending,
            onSend = onSendPendingVoice,
            onDiscard = onDiscardPendingVoice,
        )
    }
}

private const val LONG_MESSAGE_COLLAPSE_LIMIT = 300
private const val MIC_LONG_PRESS_MS = 1_000L
private const val COMPOSER_MIN_HEIGHT_DP = 36
private const val COMPOSER_MAX_LINES = 4
private const val CONVERSATION_BOTTOM_ANCHOR_KEY = "conversation_bottom_anchor"

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
            Text("💬", fontSize = 48.sp)
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
            text = stringResource(R.string.messages_no_messages_found_body),
            fontSize = 15.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(onClick = onQuickMessage),
        )
    }
}

@Composable
private fun ChatWallpaperBackground() {
    val colors = LocalSpectRoomColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background))
        Image(
            painter = painterResource(
                if (colors.isDark) R.drawable.chat_back_dark else R.drawable.chat_back,
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ChatHeader(
    partnerName: String,
    partnerPhotoUrl: String?,
    composerLocked: Boolean,
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
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenPartnerProfile)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (partnerPhotoUrl.isNullOrBlank()) {
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
                Text(
                    text = stringResource(R.string.messages_tap_to_see_profile),
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                )
            }
        }
        TextButton(
            onClick = onQuickMessage,
            enabled = !composerLocked,
            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = TabSelected,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.matches_pre_message_button_short),
                color = TabSelected,
                fontSize = 15.sp,
                maxLines = 1,
            )
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

@Composable
private fun ChatMessageRow(
    message: MessageDisplay,
    outgoing: Boolean,
    expandedLongMessageIds: Set<String>,
    onExpandLongMessage: (String) -> Unit,
    onOpenImage: (String) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val bubbleColor = if (outgoing) OutgoingChatBubble else IncomingChatBubble
    val bubbleShape = ChatBubbleShape(isOutgoing = outgoing)
    val timeText = formatBubbleTime(message.createdAt)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
        when (message.messageType) {
            MessageType.IMAGE -> Box(
                modifier = Modifier
                    .align(alignment)
                    .widthIn(max = 240.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor, bubbleShape)
                    .padding(
                        start = if (outgoing) 0.dp else 7.dp,
                        end = if (outgoing) 7.dp else 0.dp,
                        top = 3.dp,
                        bottom = 3.dp,
                    ),
            ) {
                AsyncImage(
                    model = message.content,
                    contentDescription = "Image message",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 234.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenImage(message.content) },
                )
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            MessageType.VOICE -> VoiceMessagePlayer(
                audioUrl = message.content,
                displayDurationSeconds = message.voiceDurationSeconds,
                isOutgoing = outgoing,
                timestampText = timeText,
                modifier = Modifier.align(alignment),
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
                        .align(alignment)
                        .widthIn(max = 280.dp)
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
                ) {
                    Text(
                        text = displayed,
                        modifier = Modifier.widthIn(max = 252.dp),
                        color = ChatBubbleText,
                        fontSize = 16.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End,
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
}

@Composable
private fun ChatComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    isSending: Boolean,
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
            onValueChange = onDraftChange,
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
                enabled = !isSending,
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
                enabled = !isSending,
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
                    .pointerInput(isSending) {
                        if (isSending) return@pointerInput
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
                    tint = colors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatBubbleTime(date: java.util.Date): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)

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
