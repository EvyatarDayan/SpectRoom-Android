package com.example.spectroom.ui.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

object AppLocale {
    fun localizedContext(base: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    key(languageCode) {
        val baseContext = LocalContext.current
        // Activity-result launchers resolve the owner from LocalContext when no explicit
        // owner is provided. Keep the real Activity owner after swapping in a localized context.
        val activityResultOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
            "ProvideAppLocale must be used inside a ComponentActivity"
        }
        val localizedContext = remember(baseContext, languageCode) {
            AppLocale.localizedContext(baseContext, languageCode)
        }
        val configuration = remember(localizedContext) {
            Configuration(localizedContext.resources.configuration)
        }
        val resources = remember(localizedContext) {
            localizedContext.resources
        }
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides configuration,
            LocalResources provides resources,
            LocalActivityResultRegistryOwner provides activityResultOwner,
        ) {
            content()
        }
    }
}
