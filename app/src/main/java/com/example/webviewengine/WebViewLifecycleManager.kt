package com.example.webviewengine

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView

/**
 * Manages WebView lifecycle transitions (onResume, onPause), state preservation across
 * Activity rotation/recreation (saveState/restoreState), memory trimming, and leak-free teardown.
 */
class WebViewLifecycleManager {

    private var savedWebState: Bundle? = null

    fun onResume(mainWebView: WebView?, popupWebView: WebView? = null) {
        runCatching { mainWebView?.onResume() }
        runCatching { popupWebView?.onResume() }
    }

    fun onPause(mainWebView: WebView?, popupWebView: WebView? = null) {
        runCatching { popupWebView?.onPause() }
        runCatching { mainWebView?.onPause() }
    }

    fun saveState(webView: WebView?, outState: Bundle? = null): Bundle? {
        val target = outState ?: Bundle()
        val ok = runCatching { webView?.saveState(target) != null }.getOrDefault(false)
        if (ok) {
            savedWebState = Bundle(target)
            return target
        }
        return null
    }

    fun restoreState(webView: WebView?, inState: Bundle? = null): Boolean {
        val source = inState ?: savedWebState ?: return false
        return runCatching {
            webView?.restoreState(source) != null
        }.getOrDefault(false)
    }

    fun onLowMemory(webView: WebView?) {
        runCatching {
            webView?.clearCache(false)
            webView?.freeMemory()
        }
    }

    fun destroy(webView: WebView?, jsBridgeName: String = "WebinoBridge") {
        if (webView == null) return
        runCatching {
            webView.stopLoading()
            webView.removeJavascriptInterface(jsBridgeName)
            webView.loadUrl("about:blank")
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
