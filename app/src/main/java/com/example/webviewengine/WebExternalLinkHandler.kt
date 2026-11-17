package com.example.webviewengine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.util.Locale

/**
 * Safely dispatches external links and native intents (tel, mailto, smsto, WhatsApp,
 * Telegram, Maps, validated intent://, and system browser) without requesting prohibited
 * SMS/Call/Package permissions.
 */
class WebExternalLinkHandler(
    private val context: Context,
) {
    fun isNativeAppUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        return scheme in setOf("tel", "mailto", "sms", "smsto", "geo", "whatsapp", "tg", "intent") ||
            host.contains("wa.me") ||
            host.contains("api.whatsapp.com") ||
            host.contains("t.me") ||
            host.contains("telegram.me") ||
            host.contains("maps.google") ||
            host.contains("neshan.org") ||
            host.contains("balad.ir")
    }

    fun openExternal(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        if (WebSecurityManager.isDangerousScheme(scheme)) return false

        if (scheme == "intent") {
            val (parsedIntent, fallbackUrl) = WebSecurityManager.parseSafeIntentUri(uri.toString())
            if (parsedIntent != null) {
                val started = runCatching {
                    context.startActivity(parsedIntent)
                    true
                }.getOrDefault(false)
                if (started) return true
            }
            if (!fallbackUrl.isNullOrBlank()) {
                return openExternal(Uri.parse(fallbackUrl))
            }
            return false
        }

        val intent = when (scheme) {
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            "sms", "smsto" -> Intent(Intent.ACTION_SENDTO, uri)
            else -> Intent(Intent.ACTION_VIEW, uri)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun shareText(text: String, title: String): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
