package com.example.webviewengine

import android.webkit.WebStorage
import android.webkit.WebView
import com.example.domain.model.CacheModeOption

/**
 * Manages DOM Storage, LocalStorage, SessionStorage, IndexedDB/WebSQL availability,
 * cache mode policy, and user-confirmed clearing of cached site data.
 */
object WebStorageManager {

    fun applyStorageAndCache(
        webView: WebView,
        domStorageEnabled: Boolean,
        webSqlEnabled: Boolean,
        cacheMode: CacheModeOption,
    ) {
        webView.settings.apply {
            this.domStorageEnabled = domStorageEnabled
            @Suppress("DEPRECATION")
            this.databaseEnabled = webSqlEnabled || domStorageEnabled
            this.cacheMode = cacheMode.androidConst
        }
    }

    fun clearCacheOnly(webView: WebView?, includeDiskFiles: Boolean = true) {
        runCatching { webView?.clearCache(includeDiskFiles) }
    }

    fun clearAllSiteData(webView: WebView?) {
        runCatching {
            webView?.clearCache(true)
            webView?.clearFormData()
            webView?.clearHistory()
            WebStorage.getInstance().deleteAllData()
            WebCookieManager.clearAll()
        }
    }
}
