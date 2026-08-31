package com.example.spectroom.ui.navigation

import androidx.annotation.StringRes
import com.example.spectroom.R

object Routes {
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val MANDATORY_PROFILE = "mandatory_profile"
    const val MAIN = "main"
}

enum class MainTab(val index: Int, @param:StringRes val labelRes: Int) {
    MESSAGES(0, R.string.tab_messages),
    MATCHES(1, R.string.tab_matches),
    HOME(2, R.string.tab_home),
    PROFILE(3, R.string.tab_profile),
    SETTINGS(4, R.string.tab_settings);

    companion object {
        fun fromIndex(index: Int): MainTab = entries.firstOrNull { it.index == index } ?: HOME
    }
}
