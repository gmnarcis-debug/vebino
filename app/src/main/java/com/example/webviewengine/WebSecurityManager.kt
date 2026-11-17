package com.example.webviewengine

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import java.util.Locale

/**
 * Central security gatekeeper for WEBINO's WebView engine:
 * - Blocks dangerous URI schemes (javascript:, file:, data:, vbscript:)
 * - Validates Origin trust before granting JS bridge or sensitive capabilities
 * - Sanitizes intent:// URIs against arbitrary component/selector hijacking
 * - Redacts sensitive query params or tokens before logging technical errors
 */
object WebSecurityManager {

    private val blockedSchemes = setOf("javascript", "file", "data", "vbscript", "blob")
    private val sensitiveQueryKeys = setOf("token", "password", "passwd", "pass", "secret", "auth", "code", "session", "cookie", "key")

    fun isDangerousScheme(scheme: String?): Boolean =
        scheme?.lowercase(Locale.US) in blockedSchemes

    fun isOriginTrusted(url: String?, allowedOriginHost: String, requireHttps: Boolean): Boolean {
        if (url.isNullOrBlank() || allowedOriginHost.isBlank()) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        if (requireHttps && scheme != "https") return false
        if (scheme != "https" && scheme != "http") return false
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        return host == allowedOriginHost || host.endsWith(".$allowedOriginHost")
    }

    fun isHttpAllowed(scheme: String, httpsOnly: Boolean): Boolean =
        !(httpsOnly && scheme.equals("http", ignoreCase = true))

    fun enforceSafeSettings(settings: WebSettings) {
        settings.allowFileAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false
    }

    /**
     * Parses an Android intent:// URI safely, removing embedded selectors/explicit dangerous
     * components and validating an optional browser_fallback_url.
     */
    fun parseSafeIntentUri(rawUri: String): Pair<Intent?, String?> {
        val parsed = runCatching { Intent.parseUri(rawUri, Intent.URI_INTENT_SCHEME) }.getOrNull()
            ?: return null to null

        val fallback = parsed.getStringExtra("browser_fallback_url")?.takeIf { fb ->
            val u = runCatching { Uri.parse(fb) }.getOrNull()
            val s = u?.scheme?.lowercase(Locale.US)
            s == "https" || s == "http"
        }

        // Prevent targeted internal component hijacking and explicit selector abuse
        parsed.component = null
        parsed.selector = null
        // A hostile page must not be able to smuggle URI-grant flags into the intent (intent redirection).
        parsed.flags = parsed.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            ).inv()
        parsed.addCategory(Intent.CATEGORY_BROWSABLE)
        parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return parsed to fallback
    }

    fun sanitizeUrlForLog(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return rawUrl.take(120)
        val hostPart = listOfNotNull(uri.scheme, uri.host).joinToString("://")
        val pathPart = uri.path.orEmpty()
        val query = uri.query
        if (query.isNullOrBlank()) return "$hostPart$pathPart"
        val safeParams = query.split("&").map { pair ->
            val key = pair.substringBefore("=").lowercase(Locale.US)
            if (sensitiveQueryKeys.any { key.contains(it) }) {
                "${pair.substringBefore("=")}=***"
            } else {
                pair
            }
        }.joinToString("&")
        return "$hostPart$pathPart?$safeParams"
    }
}
