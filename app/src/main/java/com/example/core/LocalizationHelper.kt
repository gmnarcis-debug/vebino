package com.example.core

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.AppLanguage
import java.util.Locale

val LocalWebinoResources = staticCompositionLocalOf<Resources?> { null }

@Composable
fun ProvideLocalizedResources(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val localizedResources = remember(baseContext, language) {
        val locale = if (language == AppLanguage.FA) Locale("fa", "IR") else Locale.ENGLISH
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
        }
        baseContext.createConfigurationContext(config).resources
    }
    CompositionLocalProvider(LocalWebinoResources provides localizedResources) {
        content()
    }
}

@Composable
@ReadOnlyComposable
fun appString(@StringRes id: Int): String {
    val res = LocalWebinoResources.current ?: LocalContext.current.resources
    return res.getString(id)
}

@Composable
@ReadOnlyComposable
fun appString(@StringRes id: Int, vararg formatArgs: Any): String {
    val res = LocalWebinoResources.current ?: LocalContext.current.resources
    return res.getString(id, *formatArgs)
}
