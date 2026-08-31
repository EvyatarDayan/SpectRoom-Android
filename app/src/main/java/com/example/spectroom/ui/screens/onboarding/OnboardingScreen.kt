package com.example.spectroom.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val stepRes: Int? = null,
    val titleRes: Int,
    val titleLine1Res: Int? = null,
    val titleLine2Res: Int? = null,
    val descriptionRes: Int,
    val noticeLeadRes: Int? = null,
    val noticeBodyRes: Int? = null,
    @param:DrawableRes val imageRes: Int? = null,
)

@Composable
fun OnboardingScreen(
    onGetStarted: (dontShowAgain: Boolean) -> Unit,
    preferenceBar: @Composable () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_intro_title,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        preferenceBar()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        val isLast = pagerState.currentPage == pages.lastIndex
        if (isLast) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dontShowAgain = !dontShowAgain }
                    .padding(horizontal = 30.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (dontShowAgain) {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Filled.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    tint = TabSelected,
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    stringResource(R.string.onboarding_dont_show_again),
                    color = colors.text,
                    fontSize = 14.sp,
                )
            }
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text(stringResource(R.string.onboarding_previous), color = TabSelected)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!isLast) {
                Button(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = { onGetStarted(dontShowAgain) },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp, top = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                TabSelected
                            } else {
                                colors.secondaryText.copy(alpha = 0.3f)
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        page.imageRes?.let { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        page.stepRes?.let {
            Text(
                stringResource(it),
                color = colors.secondaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (page.titleLine1Res != null && page.titleLine2Res != null) {
            Text(
                stringResource(page.titleLine1Res),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
            )
            Text(
                stringResource(page.titleLine2Res),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
            )
        } else {
            Text(
                stringResource(page.titleRes),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(page.descriptionRes),
            color = colors.secondaryText,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (page.noticeLeadRes != null && page.noticeBodyRes != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TabSelected.copy(alpha = 0.08f))
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(page.noticeLeadRes) + " " + stringResource(page.noticeBodyRes),
                    color = colors.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
