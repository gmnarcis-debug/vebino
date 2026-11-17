package com.example.webviewengine

import android.content.Context
import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.model.PopupBehaviorMode

sealed interface PopupAction {
    data object Blocked : PopupAction
    data class OpenInSameWebView(val resultMsg: Message) : PopupAction
    data class OpenInternalModal(val popupWebView: WebView) : PopupAction
    data class ResolveInExternalBrowser(val tempTransportView: WebView) : PopupAction
}

/**
 * Coordinates window.open / target="_blank" / onCreateWindow behavior according to the
 * project's PopupBehaviorMode while blocking ungestured unwanted pop-ups.
 */
class WebPopupHandler(
    private val context: Context,
) {
    fun handleCreateWindow(
        mainWebView: WebView,
        config: WebViewEngineConfig,
        isUserGesture: Boolean,
        resultMsg: Message?,
        onPopupUrlForExternal: (String) -> Unit,
        onShowInternalPopup: (WebView) -> Unit,
        onCloseInternalPopup: () -> Unit,
    ): Boolean {
        if (resultMsg == null) return false
        if (!config.multipleWindowsAllowed) return false
        if (!isUserGesture && !config.popupsAllowed) return false

        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false

        return when (config.popupBehavior) {
            PopupBehaviorMode.SAME_PAGE -> {
                transport.webView = mainWebView
                resultMsg.sendToTarget()
                true
            }
            PopupBehaviorMode.INTERNAL_WINDOW -> {
                val popupView = WebView(context).also { wv ->
                    WebViewFactory.configure(wv, config)
                    WebCookieManager.applyPolicy(wv, config.cookiesEnabled, config.thirdPartyCookiesEnabled)
                    wv.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val u = request?.url ?: return false
                            val scheme = u.scheme?.lowercase().orEmpty()
                            if (WebSecurityManager.isDangerousScheme(scheme)) return true
                            if (config.httpsOnly && scheme == "http") return true
                            return false
                        }
                    }
                    wv.webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onCloseWindow(window: WebView?) {
                            onCloseInternalPopup()
                        }
                    }
                }
                transport.webView = popupView
                resultMsg.sendToTarget()
                onShowInternalPopup(popupView)
                true
            }
            PopupBehaviorMode.EXTERNAL_BROWSER -> {
                val bridgeView = WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            request?.url?.toString()?.let(onPopupUrlForExternal)
                            runCatching { view?.destroy() }
                            return true
                        }
                    }
                }
                transport.webView = bridgeView
                resultMsg.sendToTarget()
                true
            }
        }
    }
}
