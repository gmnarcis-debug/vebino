package com.example.webviewengine

import android.view.View
import android.webkit.WebChromeClient

/**
 * Manages HTML5 fullscreen video/media custom view enter, exit, and Back-button cleanup.
 */
class WebFullscreenManager(
    private val onShowCustomVideoView: (View?) -> Unit,
) {
    private var activeCallback: WebChromeClient.CustomViewCallback? = null
    private var isFullscreenActive: Boolean = false

    fun enterFullscreen(
        customView: View?,
        callback: WebChromeClient.CustomViewCallback?,
        allowed: Boolean,
    ): Boolean {
        if (!allowed || customView == null) {
            runCatching { callback?.onCustomViewHidden() }
            return false
        }
        activeCallback = callback
        isFullscreenActive = true
        onShowCustomVideoView(customView)
        return true
    }

    fun exitFullscreen(): Boolean {
        val wasActive = isFullscreenActive
        isFullscreenActive = false
        onShowCustomVideoView(null)
        runCatching { activeCallback?.onCustomViewHidden() }
        activeCallback = null
        return wasActive
    }

    fun isInFullscreen(): Boolean = isFullscreenActive
}
