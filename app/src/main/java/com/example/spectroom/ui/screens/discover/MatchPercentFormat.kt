package com.example.spectroom.ui.screens.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.spectroom.R

@Composable
fun buildMatchSentence(
    percentage: Int,
    firstName: String,
    percentColor: Color,
): AnnotatedString {
    val clampedPercent = percentage.coerceIn(0, 100)
    val sentence = stringResource(R.string.search_match_sentence, clampedPercent, firstName)
    val percentToken = "$clampedPercent%"
    return remember(sentence, percentColor) {
        buildAnnotatedString {
            append(sentence)
            val start = sentence.indexOf(percentToken)
            if (start >= 0) {
                addStyle(
                    SpanStyle(color = percentColor, fontWeight = FontWeight.SemiBold),
                    start,
                    start + percentToken.length,
                )
            }
        }
    }
}
