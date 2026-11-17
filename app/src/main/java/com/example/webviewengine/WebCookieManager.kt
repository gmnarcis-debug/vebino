package com.example.webviewengine

import android.webkit.CookieManager
import android.webkit.WebView

/**
 * Manages HTTP/HTTPS Cookies, SameSite & Third-Party Cookie policies,
 * cross-launch session persistence via flush(), and user-confirmed cookie clearing.
 * Never writes cookies or login session values into logs.
 */
object WebCookieManager {

    fun applyPolicy(webView: WebView, cookiesEnabled: Boolean, thirdPartyCookiesEnabled: Boolean) {
        runCatching {
            CookieManager.getInstance()?.apply {
                setAcceptCookie(cookiesEnabled)
                setAcceptThirdPartyCookies(webView, cookiesEnabled && thirdPartyCookiesEnabled)
            }
        }
    }

    fun hasSessionCookies(url: String): Boolean = runCatching {
        !CookieManager.getInstance()?.getCookie(url).isNullOrBlank()
    }.getOrDefault(false)

    fun flush() {
        runCatching { CookieManager.getInstance()?.flush() }
    }

    fun clearAll(onCleared: (() -> Unit)? = null) {
        runCatching {
            val cm = CookieManager.getInstance()
            cm?.removeAllCookies {
                runCatching { cm.flush() }
                onCleared?.invoke()
            }
        }
    }
}
