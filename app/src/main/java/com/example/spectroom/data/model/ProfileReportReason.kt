package com.example.spectroom.data.model

import androidx.annotation.StringRes
import com.example.spectroom.R

enum class ProfileReportReason(
    val storageCategory: String,
    @param:StringRes val labelRes: Int,
) {
    INAPPROPRIATE_CONTENT(
        storageCategory = "Inappropriate Content",
        labelRes = R.string.profile_report_reason_inappropriate,
    ),
    FAKE_PROFILE_OR_SPAM(
        storageCategory = "Fake profile or Spam",
        labelRes = R.string.profile_report_reason_fake_spam,
    ),
    HARASSMENT_OR_BULLYING(
        storageCategory = "Harassment or Bullying",
        labelRes = R.string.profile_report_reason_harassment,
    ),
    SOMETHING_ELSE(
        storageCategory = "Something else",
        labelRes = R.string.profile_report_reason_something_else,
    ),
}
