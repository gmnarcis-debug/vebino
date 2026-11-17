package com.example.webviewengine

import android.content.Context
import android.net.Uri
import com.example.domain.model.ExternalLinkMode
import java.util.Locale

sealed interface NavigationDecision {
    data object AllowInWebView : NavigationDecision
    data class OpenExternal(val uri: Uri, val categoryLabelFa: String = "لینک خارجی") : NavigationDecision
    data class ConfirmExternal(val uri: Uri, val reasonFa: String) : NavigationDecision
    data class BlockWithNotice(val reason: String) : NavigationDecision
}

/**
 * Evaluates URL navigation rules, HTTPS-Only enforcement, domain allowlists/blocklists,
 * special native schemes (tel, mailto, smsto, WhatsApp, Telegram, Maps, validated intent://),
 * Deep Links / App Links, and Iranian payment gateways.
 */
class WebNavigationHandler(
    private val context: Context,
) {
    private val externalLinkHandler = WebExternalLinkHandler(context)
    private var allowedOriginHost: String = ""
    private var customAllowlist: Set<String> = emptySet()
    private var customBlocklist: Set<String> = emptySet()

    fun updateRules(initialUrl: String, navigationRulesRaw: String = "") {
        allowedOriginHost = runCatching { Uri.parse(initialUrl).host?.lowercase(Locale.US) }
            .getOrNull()
            .orEmpty()
        val lines = navigationRulesRaw.split(',', '\n', ';')
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        customBlocklist = lines.filter { it.startsWith("!") }
            .map { it.removePrefix("!").trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        customAllowlist = lines.filter { !it.startsWith("!") }.toSet()
    }

    fun getAllowedOriginHost(): String = allowedOriginHost

    fun evaluate(uri: Uri, config: WebViewEngineConfig): NavigationDecision {
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val host = uri.host?.lowercase(Locale.US).orEmpty()

        if (WebSecurityManager.isDangerousScheme(scheme)) {
            return NavigationDecision.BlockWithNotice("طرح URI غیرمجاز ($scheme:) مسدود شد.")
        }

        if (scheme == "tel") return NavigationDecision.OpenExternal(uri, "تماس تلفنی")
        if (scheme == "mailto") return NavigationDecision.OpenExternal(uri, "ارسال ایمیل")
        if (scheme == "sms" || scheme == "smsto") return NavigationDecision.OpenExternal(uri, "پیامک")
        if (scheme == "geo" || host.contains("maps.google") || host.contains("neshan.org") || host.contains("balad.ir")) {
            return NavigationDecision.OpenExternal(uri, "مسیریاب و نقشه")
        }
        if (scheme == "whatsapp" || host.contains("wa.me") || host.contains("api.whatsapp.com")) {
            return NavigationDecision.OpenExternal(uri, "واتساپ")
        }
        if (scheme == "tg" || host.contains("t.me") || host.contains("telegram.me")) {
            return NavigationDecision.OpenExternal(uri, "تلگرام")
        }
        if (scheme == "intent") {
            val (parsed, fallback) = WebSecurityManager.parseSafeIntentUri(uri.toString())
            if (parsed == null && fallback == null) {
                return NavigationDecision.BlockWithNotice("ساختار لینک intent:// معتبر نیست و مسدود شد.")
            }
            return NavigationDecision.ConfirmExternal(uri, "این لینک درخواست باز کردن یک اپلیکیشن دیگر در گوشی را دارد. اجازه می‌دهید؟")
        }

        if (config.httpsOnly && scheme == "http") {
            return NavigationDecision.BlockWithNotice("اتصال ناامن HTTP به دلیل فعال بودن HTTPS-Only مسدود شد.")
        }

        if (scheme != "https" && scheme != "http") {
            return NavigationDecision.ConfirmExternal(
                uri = uri,
                reasonFa = "طرح ناشناس ($scheme:) درخواست اجرای برنامه خارجی دارد. ادامه می‌دهید؟",
            )
        }

        val isBlocked = customBlocklist.any { rule -> host == rule || host.endsWith(".$rule") }
        if (isBlocked) {
            return NavigationDecision.BlockWithNotice("دسترسی به دامنه $host طبق قوانین پروژه مسدود شده است.")
        }

        val isSameHost = allowedOriginHost.isNotEmpty() &&
            (host == allowedOriginHost || host.endsWith(".$allowedOriginHost"))
        val isAllowlisted = customAllowlist.any { rule -> host == rule || host.endsWith(".$rule") }
        val isPaymentGateway = isPaymentGatewayHost(host)

        if (isSameHost || isAllowlisted || isPaymentGateway) {
            return NavigationDecision.AllowInWebView
        }

        return when (config.externalLinkMode) {
            ExternalLinkMode.OPEN_IN_SYSTEM_BROWSER -> NavigationDecision.OpenExternal(uri, "مرورگر خارجی")
            ExternalLinkMode.KEEP_IN_APP -> NavigationDecision.AllowInWebView
            ExternalLinkMode.ASK_OR_ALLOWLIST -> NavigationDecision.ConfirmExternal(
                uri = uri,
                reasonFa = "لینک به دامنه خارجی ($host) اشاره دارد. در مرورگر خارجی باز شود یا همینجا؟",
            )
        }
    }

    fun isOriginTrusted(url: String?, requireHttps: Boolean = true): Boolean =
        WebSecurityManager.isOriginTrusted(url, allowedOriginHost, requireHttps)

    fun openExternal(uri: Uri): Boolean = externalLinkHandler.openExternal(uri)

    fun shareText(text: String, title: String): Boolean = externalLinkHandler.shareText(text, title)

    private fun isPaymentGatewayHost(host: String): Boolean =
        host.contains("shaparak.ir") ||
            host.contains("zarinpal.com") ||
            host.contains("idpay.ir") ||
            host.contains("behpardakht.com") ||
            host.contains("sadadpsp.ir") ||
            host.contains("pep.co.ir")
}
