package com.example.spectroom.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val stepRes: Int? = null,
    val titleRes: Int? = null,
    val titleLine1Res: Int? = null,
    val titleLine2Res: Int? = null,
    val descriptionRes: Int,
    val noticeLeadRes: Int? = null,
    val noticeBodyRes: Int? = null,
    @param:DrawableRes val imageRes: Int? = null,
) {
    val hasNotice: Boolean get() = noticeLeadRes != null && noticeBodyRes != null
    val showsImage: Boolean get() = imageRes != null
}

private val StepImageRes = listOf(
    R.drawable.onboarding_profile,
    R.drawable.onboarding_home,
    R.drawable.onboarding_chat,
)

@Composable
fun OnboardingScreen(
    onGetStarted: (dontShowAgain: Boolean) -> Unit,
    preferenceBar: @Composable () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val pages = listOf(
        OnboardingPage(
            titleLine1Res = R.string.onboarding_intro_title_line1,
            titleLine2Res = R.string.onboarding_intro_title_line2,
            descriptionRes = R.string.onboarding_intro_description,
            noticeLeadRes = R.string.onboarding_intro_notice_lead,
            noticeBodyRes = R.string.onboarding_intro_notice_body,
        ),
        OnboardingPage(
            stepRes = R.string.onboarding_step_1,
            titleRes = R.string.onboarding_welcome_title,
            descriptionRes = R.string.onboarding_welcome_description,
            imageRes = R.drawable.onboarding_profile,
        ),
        OnboardingPage(
            stepRes = R.string.onboarding_step_2,
            titleRes = R.string.onboarding_match_title,
            descriptionRes = R.string.onboarding_match_description,
            imageRes = R.drawable.onboarding_home,
        ),
        OnboardingPage(
            stepRes = R.string.onboarding_step_3,
            titleRes = R.string.onboarding_communication_title,
            descriptionRes = R.string.onboarding_communication_description,
            imageRes = R.drawable.onboarding_chat,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var dontShowAgain by remember { mutableStateOf(false) }
    val isLast = pagerState.currentPage == pages.lastIndex
    // Match iOS UIColor(white: 0.78 / 0.28) — not black-with-alpha.
    val descriptionColor = if (colors.isDark) {
        Color(0.78f, 0.78f, 0.78f)
    } else {
        Color(0.28f, 0.28f, 0.28f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Match iOS: TabView fills remaining space above footer controls.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                OnboardingPageContent(
                    page = pages[pageIndex],
                    descriptionColor = descriptionColor,
                )
            }

            // Always reserve checkbox row so nav/dots don't jump (iOS parity).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 16.dp)
                    .alpha(if (isLast) 1f else 0f)
                    .then(
                        if (isLast) {
                            Modifier.clickable { dontShowAgain = !dontShowAgain }
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    imageVector = if (dontShowAgain) {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Filled.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    tint = TabSelected,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_dont_show_again),
                    color = colors.text,
                    fontSize = 14.sp,
                )
            }

            // iOS: .padding(.horizontal, 30).padding(.bottom, 50)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_previous),
                            color = TabSelected,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isLast) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                        contentPadding = PaddingValues(
                            horizontal = 24.dp,
                            vertical = 12.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                } else {
                    Button(
                        onClick = { onGetStarted(dontShowAgain) },
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                        contentPadding = PaddingValues(
                            horizontal = 24.dp,
                            vertical = 12.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_get_started),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            // iOS: HStack(spacing: 8), .padding(.bottom, 24)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) {
                                    TabSelected
                                } else {
                                    Color.Gray.copy(alpha = 0.3f)
                                },
                            ),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            preferenceBar()
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    descriptionColor: Color,
) {
    val colors = LocalSpectRoomColors.current
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val noticeMaxHeight = if (screenHeight >= 900) 380.dp else 180.dp

    // Mirror iOS OnboardingPageView: VStack(spacing: 30) with flexible Spacers.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (page.hasNotice) {
            Spacer(modifier = Modifier.height(48.dp))
        } else {
            Spacer(modifier = Modifier.height(36.dp))
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(40.dp))

        page.imageRes?.let { currentImage ->
            OnboardingImageCarousel(currentImageRes = currentImage)
            Spacer(modifier = Modifier.height(40.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (page.showsImage) 52.dp else 60.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            page.stepRes?.let {
                Text(
                    text = stringResource(it),
                    color = colors.secondaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
            }

            if (page.titleLine1Res != null && page.titleLine2Res != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 88.dp),
                ) {
                    Text(
                        text = stringResource(page.titleLine1Res),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(page.titleLine2Res),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (page.titleRes != null) {
                Text(
                    text = stringResource(page.titleRes),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp,
                )
            }

            Text(
                text = stringResource(page.descriptionRes),
                color = descriptionColor,
                fontSize = 16.sp,
                // iOS .body + lineSpacing(4) ≈ 20sp line height + 4
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }

        if (page.hasNotice) {
            Spacer(modifier = Modifier.height(40.dp))
            NoticeCard(
                lead = stringResource(page.noticeLeadRes!!),
                body = stringResource(page.noticeBodyRes!!),
                maxHeight = noticeMaxHeight,
                modifier = Modifier.padding(top = 28.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NoticeCard(
    lead: String,
    body: String,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(lead)
        }
        append(" ")
        append(body)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(max = maxHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(TabSelected.copy(alpha = 0.08f))
            .border(1.5.dp, TabSelected.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
    ) {
        Text(
            text = text,
            color = colors.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun OnboardingImageCarousel(
    @DrawableRes currentImageRes: Int,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val centerMaxHeight = if (screenHeightDp >= 900) 260.dp else 300.dp
    val sideMaxHeight = if (screenHeightDp >= 900) 190.dp else 220.dp
    val index = StepImageRes.indexOf(currentImageRes).coerceAtLeast(0)
    val leftRes = StepImageRes[(index - 1 + StepImageRes.size) % StepImageRes.size]
    val rightRes = StepImageRes[(index + 1) % StepImageRes.size]

    Column(modifier = Modifier.padding(top = 40.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(centerMaxHeight),
            horizontalArrangement = Arrangement.spacedBy((-36).dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(leftRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = sideMaxHeight)
                    .alpha(0.35f)
                    .zIndex(0f),
            )
            Image(
                painter = painterResource(currentImageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = centerMaxHeight)
                    .zIndex(1f),
            )
            Image(
                painter = painterResource(rightRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = sideMaxHeight)
                    .alpha(0.35f)
                    .zIndex(0f),
            )
        }
    }
}
