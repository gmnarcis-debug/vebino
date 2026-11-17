package com.example.webviewengine

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Configures WebView instances via [WebViewConfiguration] and attaches a strictly
 * origin-gated native JavaScript bridge ([WebinoNativeBridge]) when JS is enabled.
 */
object WebViewFactory {

    const val JS_BRIDGE_NAME = "WebinoBridge"

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        view: WebView,
        config: WebViewEngineConfig,
        allowedOriginHost: String = "",
        onShareRequested: ((String) -> Unit)? = null,
    ) {
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        WebViewConfiguration.applySettings(view.settings, config)
        WebSecurityManager.enforceSafeSettings(view.settings)
        WebStorageManager.applyStorageAndCache(
            webView = view,
            domStorageEnabled = config.domStorageEnabled,
            webSqlEnabled = config.webSqlEnabled,
            cacheMode = config.cacheMode,
        )

        runCatching { view.removeJavascriptInterface(JS_BRIDGE_NAME) }
        if (config.javaScriptEnabled) {
            view.addJavascriptInterface(
                WebinoNativeBridge(
                    context = view.context,
                    webView = view,
                    allowedOriginHost = allowedOriginHost,
                    httpsOnly = config.httpsOnly,
                    clipboardAllowed = config.clipboardAccessEnabled,
                    onShareRequested = onShareRequested,
                ),
                JS_BRIDGE_NAME,
            )
        }
    }

    class WebinoNativeBridge(
        private val context: Context,
        private val webView: WebView,
        private val allowedOriginHost: String,
        private val httpsOnly: Boolean,
        private val clipboardAllowed: Boolean,
        private val onShareRequested: ((String) -> Unit)?,
    ) {
        @JavascriptInterface
        fun shareText(text: String?) {
            if (!isTrusted()) return
            onShareRequested?.invoke(text.orEmpty())
        }

        @JavascriptInterface
        fun copyToClipboard(text: String?): Boolean {
            if (!clipboardAllowed || !isTrusted() || text.isNullOrEmpty()) return false
            return runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
                cm.setPrimaryClip(ClipData.newPlainText("WEBINO", text.take(4096)))
                true
            }.getOrDefault(false)
        }

        @JavascriptInterface
        fun getEngineCapabilitiesJson(): String {
            if (!isTrusted()) return "{}"
            val report = WebViewConfiguration.inspectCapabilities(context)
            return """{"sdk":${report.androidSdkInt},"webView":"${report.webViewVersionName}","indexedDb":${report.supportsIndexedDb},"webShare":${report.supportsWebShareApi},"notifications":${report.supportsWebNotifications}}"""
        }

        private fun isTrusted(): Boolean {
            val currentUrl: String? = if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                webView.url
            } else {
                var fetched: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                webView.post {
                    fetched = webView.url
                    latch.countDown()
                }
                runCatching { latch.await(400, java.util.concurrent.TimeUnit.MILLISECONDS) }
                fetched
            }
            return WebSecurityManager.isOriginTrusted(currentUrl, allowedOriginHost, httpsOnly)
        }
    }
}
