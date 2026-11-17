package com.example.webviewengine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.view.View
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.CacheModeOption
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.MixedContentOption
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SslPolicy

data class JsDialogRequest(
    val titleFa: String,
    val message: String,
    val isConfirm: Boolean,
)

data class WebViewEngineConfig(
    val initialUrl: String,
    val navigationRulesRaw: String = "",
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val webSqlEnabled: Boolean = false,
    val clipboardAccessEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val fileUploadEnabled: Boolean = true,
    val downloadManagerEnabled: Boolean = true,
    val cameraAllowed: Boolean = false,
    val microphoneAllowed: Boolean = false,
    val geolocationAllowed: Boolean = false,
    val webRtcAllowed: Boolean = false,
    val fullscreenVideoAllowed: Boolean = true,
    val multipleWindowsAllowed: Boolean = false,
    val popupsAllowed: Boolean = false,
    val popupBehavior: PopupBehaviorMode = PopupBehaviorMode.SAME_PAGE,
    val backBehavior: BackNavigationBehavior = BackNavigationBehavior.HISTORY_THEN_EXIT,
    val customUserAgent: String = "",
    val cacheMode: CacheModeOption = CacheModeOption.DEFAULT,
    val zoomEnabled: Boolean = false,
    val mixedContentMode: MixedContentOption = MixedContentOption.NEVER_ALLOW,
    val sslPolicy: SslPolicy = SslPolicy.STRICT_BLOCK,
    val httpsOnly: Boolean = true,
    val externalLinkMode: ExternalLinkMode = ExternalLinkMode.ASK_OR_ALLOWLIST,
    val enableDeepLinks: Boolean = false,
    val deepLinkScheme: String = "webino",
    val enableAppLinks: Boolean = false,
    val appLinkHost: String = "",
)

data class WebPreviewState(
    val currentUrl: String = "",
    val pageTitle: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val errorDescription: String? = null,
    val errorInfo: WebErrorInfo? = null,
    val sslWarningUrl: String? = null,
    val blockedNotice: String? = null,
    val hasFullscreenVideo: Boolean = false,
    val hasInternalPopup: Boolean = false,
    val hasSessionCookies: Boolean = false,
    val isOnline: Boolean = true,
    val pendingRationale: PermissionRationaleRequest? = null,
    val pendingExternalConfirm: NavigationDecision.ConfirmExternal? = null,
    val pendingUploadOptions: UploadRequestOptions? = null,
    val pendingJsDialog: JsDialogRequest? = null,
    val activeDownloads: List<WebDownloadItem> = emptyList(),
    val capabilityReport: WebViewCapabilityReport? = null,
    val confirmClearSiteData: Boolean = false,
    val confirmExitPrompt: Boolean = false,
)

typealias WebViewEngineController = WebViewManager

/**
 * Orchestrating facade for WEBINO's 16-module WebView architecture.
 * Coordinates WebViewConfiguration, WebViewFactory, WebViewLifecycleManager, WebNavigationHandler,
 * WebExternalLinkHandler, WebPermissionManager, WebUploadManager, WebDownloadManager,
 * WebCookieManager, WebStorageManager, WebErrorHandler, WebPopupHandler, WebFullscreenManager,
 * WebConnectivityManager, and WebSecurityManager.
 */
class WebViewManager(
    private val context: Context,
    private val onStateChange: (WebPreviewState) -> Unit,
    private val onOsPermissionPrompt: (List<String>, (Boolean) -> Unit) -> Unit,
    private val onShowCustomVideoView: (View?) -> Unit,
    private val onShowPopupWebView: (WebView?) -> Unit = {},
) {
    private val lifecycleManager = WebViewLifecycleManager()
    private val navigationHandler = WebNavigationHandler(context)
    private val externalLinkHandler = WebExternalLinkHandler(context)
    private val downloadManager = WebDownloadManager(context)
    private val errorHandler = WebErrorHandler(context)
    private val popupHandler = WebPopupHandler(context)
    private val fullscreenManager = WebFullscreenManager(onShowCustomVideoView)

    private var uploadCallbackSink: ValueCallback<Array<Uri>>? = null
    private val uploadManager = WebUploadManager { options, cb ->
        uploadCallbackSink = cb
        updateState { it.copy(pendingUploadOptions = options) }
    }

    private val permissionManager = WebPermissionManager(
        context = context,
        onRationaleRequired = { rationale ->
            updateState { it.copy(pendingRationale = rationale) }
        },
        onOsPermissionPrompt = onOsPermissionPrompt,
    )

    private var webView: WebView? = null
    private var popupWebView: WebView? = null
    private var pendingJsResult: JsResult? = null
    private var currentConfig = WebViewEngineConfig("")
    private var state = WebPreviewState()

    private fun updateState(transform: (WebPreviewState) -> WebPreviewState) {
        state = transform(state)
        onStateChange(state)
    }

    fun getCurrentState(): WebPreviewState = state

    fun getCapabilityReport(): WebViewCapabilityReport =
        WebViewConfiguration.inspectCapabilities(context)

    fun attach(
        view: WebView,
        config: WebViewEngineConfig,
        savedBundle: Bundle? = null,
    ) {
        this.webView = view
        this.currentConfig = config
        navigationHandler.updateRules(config.initialUrl, config.navigationRulesRaw)

        val capabilities = WebViewConfiguration.inspectCapabilities(context)
        val online = errorHandler.isNetworkAvailable()
        updateState {
            it.copy(
                capabilityReport = capabilities,
                isOnline = online,
                currentUrl = it.currentUrl.ifBlank { config.initialUrl },
            )
        }

        WebViewFactory.configure(
            view = view,
            config = config,
            allowedOriginHost = navigationHandler.getAllowedOriginHost(),
            onShareRequested = { text ->
                externalLinkHandler.shareText(text, state.pageTitle.ifBlank { "WEBINO" })
            },
        )
        WebCookieManager.applyPolicy(view, config.cookiesEnabled, config.thirdPartyCookiesEnabled)

        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(wv: WebView?, url: String?, favicon: Bitmap?) {
                errorHandler.stopReconnectMonitor()
                updateState {
                    it.copy(
                        currentUrl = url ?: it.currentUrl,
                        isLoading = true,
                        errorDescription = null,
                        errorInfo = null,
                        blockedNotice = null,
                        canGoBack = wv?.canGoBack() ?: false,
                        canGoForward = wv?.canGoForward() ?: false,
                        isOnline = errorHandler.isNetworkAvailable(),
                    )
                }
            }

            override fun onPageFinished(wv: WebView?, url: String?) {
                WebCookieManager.flush()
                val current = url ?: state.currentUrl
                updateState {
                    it.copy(
                        currentUrl = current,
                        isLoading = false,
                        progress = 100,
                        canGoBack = wv?.canGoBack() ?: false,
                        canGoForward = wv?.canGoForward() ?: false,
                        hasSessionCookies = WebCookieManager.hasSessionCookies(current),
                    )
                }
            }

            override fun onReceivedError(wv: WebView?, req: WebResourceRequest?, error: WebResourceError?) {
                if (req?.isForMainFrame == true) {
                    val info = errorHandler.classifyNetworkError(
                        errorCode = error?.errorCode ?: -1,
                        rawDescription = error?.description?.toString(),
                        failingUrl = req.url?.toString(),
                    )
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorDescription = info.messageFa,
                            errorInfo = info,
                            isOnline = errorHandler.isNetworkAvailable(),
                            canGoBack = wv?.canGoBack() ?: false,
                            canGoForward = wv?.canGoForward() ?: false,
                        )
                    }
                    errorHandler.startReconnectMonitor {
                        wv?.post {
                            updateState { s -> s.copy(isOnline = true) }
                            reload()
                        }
                    }
                }
            }

            override fun onReceivedHttpError(
                wv: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                    val info = errorHandler.classifyHttpError(
                        statusCode = errorResponse?.statusCode ?: 500,
                        failingUrl = request.url?.toString(),
                    )
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorDescription = info.messageFa,
                            errorInfo = info,
                            canGoBack = wv?.canGoBack() ?: false,
                            canGoForward = wv?.canGoForward() ?: false,
                        )
                    }
                }
            }

            override fun onReceivedSslError(wv: WebView?, handler: SslErrorHandler?, error: SslError?) {
                val res = errorHandler.handleSslError(
                    handler = handler,
                    error = error,
                    fallbackUrl = state.currentUrl,
                    policy = config.sslPolicy,
                ) ?: return
                when (res) {
                    SslResolution.CancelledStrictly -> {
                        val sslErr = WebErrorInfo(
                            category = WebErrorCategory.SSL_ERROR,
                            titleFa = WebErrorCategory.SSL_ERROR.titleFa,
                            messageFa = "گواهی امنیتی SSL این صفحه نامعتبر است و اتصال برای حفاظت از اطلاعات مسدود شد.",
                            technicalDetail = "SSL_ERROR • ${WebSecurityManager.sanitizeUrlForLog(error?.url ?: state.currentUrl)}",
                        )
                        updateState {
                            it.copy(
                                isLoading = false,
                                errorDescription = sslErr.messageFa,
                                errorInfo = sslErr,
                            )
                        }
                    }
                    is SslResolution.RequireExplicitUserWarning -> updateState {
                        it.copy(
                            isLoading = false,
                            sslWarningUrl = res.url,
                        )
                    }
                }
            }

            override fun shouldOverrideUrlLoading(wv: WebView?, req: WebResourceRequest?): Boolean {
                val uri = req?.url ?: return false
                return when (val decision = navigationHandler.evaluate(uri, config)) {
                    NavigationDecision.AllowInWebView -> false
                    is NavigationDecision.OpenExternal -> {
                        val opened = navigationHandler.openExternal(decision.uri)
                        if (!opened) {
                            updateState { it.copy(blockedNotice = "برنامه‌ای برای باز کردن ${decision.categoryLabelFa} روی دستگاه یافت نشد.") }
                        }
                        true
                    }
                    is NavigationDecision.ConfirmExternal -> {
                        updateState { it.copy(pendingExternalConfirm = decision) }
                        true
                    }
                    is NavigationDecision.BlockWithNotice -> {
                        updateState { it.copy(blockedNotice = decision.reason) }
                        true
                    }
                }
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(wv: WebView?, newProgress: Int) {
                updateState { it.copy(progress = newProgress, isLoading = newProgress < 100) }
            }

            override fun onReceivedTitle(wv: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    updateState { it.copy(pageTitle = title) }
                }
            }

            override fun onShowCustomView(customView: View?, callback: CustomViewCallback?) {
                val entered = fullscreenManager.enterFullscreen(customView, callback, config.fullscreenVideoAllowed)
                if (entered) {
                    updateState { it.copy(hasFullscreenVideo = true) }
                }
            }

            override fun onHideCustomView() {
                fullscreenManager.exitFullscreen()
                updateState { it.copy(hasFullscreenVideo = false) }
            }

            override fun onShowFileChooser(
                wv: WebView?,
                cb: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?,
            ): Boolean = uploadManager.startChooser(cb, params, config.fileUploadEnabled)

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?,
            ) {
                permissionManager.handleGeolocationPrompt(
                    origin = origin,
                    callback = callback,
                    config = config,
                    allowedOriginHost = navigationHandler.getAllowedOriginHost(),
                    onStatusNotice = { notice -> updateState { it.copy(blockedNotice = notice) } },
                )
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                permissionManager.handleWebPermissionRequest(
                    request = request,
                    config = config,
                    allowedOriginHost = navigationHandler.getAllowedOriginHost(),
                    onStatusNotice = { notice -> updateState { it.copy(blockedNotice = notice) } },
                )
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                permissionManager.handleWebPermissionCanceled(request)
            }

            override fun onCreateWindow(
                wv: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                val handled = popupHandler.handleCreateWindow(
                    mainWebView = view,
                    config = config,
                    isUserGesture = isUserGesture,
                    resultMsg = resultMsg,
                    onPopupUrlForExternal = { popupUrl ->
                        runCatching { Uri.parse(popupUrl) }.getOrNull()?.let { externalLinkHandler.openExternal(it) }
                    },
                    onShowInternalPopup = { pView ->
                        popupWebView = pView
                        onShowPopupWebView(pView)
                        updateState { it.copy(hasInternalPopup = true) }
                    },
                    onCloseInternalPopup = {
                        closeInternalPopup()
                    },
                )
                if (!handled && resultMsg != null) {
                    updateState { it.copy(blockedNotice = "باز شدن پنجره یا پاپ‌آپ طبق تنظیمات پروژه مسدود شد.") }
                }
                return handled
            }

            override fun onCloseWindow(window: WebView?) {
                if (window == popupWebView) {
                    closeInternalPopup()
                }
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                pendingJsResult?.cancel()
                pendingJsResult = result
                updateState {
                    it.copy(
                        pendingJsDialog = JsDialogRequest(
                            titleFa = state.pageTitle.ifBlank { "پیام صفحه" },
                            message = message.orEmpty(),
                            isConfirm = false,
                        )
                    )
                }
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                pendingJsResult?.cancel()
                pendingJsResult = result
                updateState {
                    it.copy(
                        pendingJsDialog = JsDialogRequest(
                            titleFa = state.pageTitle.ifBlank { "تأییدیه" },
                            message = message.orEmpty(),
                            isConfirm = true,
                        )
                    )
                }
                return true
            }
        }

        view.setDownloadListener(DownloadListener { url, _, contentDisposition, mimetype, contentLength ->
            if (!config.downloadManagerEnabled) {
                updateState { it.copy(blockedNotice = "مدیریت دانلود در تنظیمات این پروژه غیرفعال است.") }
                return@DownloadListener
            }
            val item = downloadManager.handleDownload(
                url = url,
                contentDisposition = contentDisposition,
                mimetype = mimetype,
                contentLength = contentLength,
                enabled = true,
                onExternalFallback = { uri -> navigationHandler.openExternal(uri) },
            )
            if (item != null) {
                updateState {
                    it.copy(
                        activeDownloads = (listOf(item) + it.activeDownloads).take(15),
                        blockedNotice = "دانلود آغاز شد: ${item.fileName}",
                    )
                }
            } else {
                updateState { it.copy(blockedNotice = "این فایل به دلایل امنیتی یا خطای لینک مسدود شد.") }
            }
        })

        val restored = if (savedBundle != null) {
            lifecycleManager.restoreState(view, savedBundle)
        } else {
            lifecycleManager.restoreState(view)
        }
        if (!restored && config.initialUrl.isNotBlank()) {
            loadUrl(config.initialUrl)
        }
    }

    fun loadUrl(url: String) {
        val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull().orEmpty()
        if (WebSecurityManager.isDangerousScheme(scheme)) {
            updateState { it.copy(blockedNotice = "طرح آدرس غیرمجاز مسدود شد.") }
            return
        }
        if (currentConfig.httpsOnly && url.startsWith("http://", ignoreCase = true)) {
            updateState { it.copy(blockedNotice = "حالت HTTPS-Only فعال است و آدرس HTTP مسدود شد.") }
            return
        }
        webView?.loadUrl(url)
    }

    fun reload() {
        updateState { it.copy(errorDescription = null, errorInfo = null, blockedNotice = null) }
        webView?.reload()
    }

    fun zoomIn() {
        webView?.zoomIn()
    }

    fun zoomOut() {
        webView?.zoomOut()
    }

    fun goBack(): Boolean {
        if (fullscreenManager.isInFullscreen()) {
            fullscreenManager.exitFullscreen()
            updateState { it.copy(hasFullscreenVideo = false) }
            return true
        }
        val popup = popupWebView
        if (popup != null) {
            if (popup.canGoBack()) {
                popup.goBack()
            } else {
                closeInternalPopup()
            }
            return true
        }
        val wv = webView ?: return false
        if (wv.canGoBack()) {
            wv.goBack()
            return true
        }
        return false
    }

    fun handleBackPress(onExitScreen: () -> Unit) {
        if (fullscreenManager.isInFullscreen()) {
            fullscreenManager.exitFullscreen()
            updateState { it.copy(hasFullscreenVideo = false) }
            return
        }
        if (popupWebView != null) {
            goBack()
            return
        }
        when (currentConfig.backBehavior) {
            BackNavigationBehavior.DIRECT_EXIT -> onExitScreen()
            BackNavigationBehavior.HISTORY_THEN_EXIT -> {
                if (!goBack()) onExitScreen()
            }
            BackNavigationBehavior.HISTORY_THEN_CONFIRM_EXIT -> {
                if (!goBack()) {
                    updateState { it.copy(confirmExitPrompt = true) }
                }
            }
        }
    }

    fun dismissExitConfirm(exitNow: Boolean, onExitScreen: () -> Unit) {
        updateState { it.copy(confirmExitPrompt = false) }
        if (exitNow) onExitScreen()
    }

    fun goForward(): Boolean {
        val wv = webView ?: return false
        if (wv.canGoForward()) {
            wv.goForward()
            return true
        }
        return false
    }

    fun openCurrentInExternalBrowser(): Boolean {
        val target = state.currentUrl.ifBlank { currentConfig.initialUrl }
        val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return false
        return navigationHandler.openExternal(uri)
    }

    fun resolveRationale(confirmed: Boolean) {
        val pending = state.pendingRationale
        updateState { it.copy(pendingRationale = null) }
        if (pending != null) {
            if (confirmed) pending.onConfirmGrant() else pending.onUserDecline()
        }
    }

    fun resolveExternalNavigation(openExternal: Boolean) {
        val decision = state.pendingExternalConfirm
        updateState { it.copy(pendingExternalConfirm = null) }
        if (decision != null && openExternal) {
            navigationHandler.openExternal(decision.uri)
        }
    }

    fun resolveJsDialog(confirmed: Boolean) {
        val res = pendingJsResult
        pendingJsResult = null
        updateState { it.copy(pendingJsDialog = null) }
        if (confirmed) res?.confirm() else res?.cancel()
    }

    fun resolveSslDecision(proceed: Boolean) {
        errorHandler.resolvePendingSsl(proceed)
        updateState { it.copy(sslWarningUrl = null) }
    }

    fun prepareCameraCapture(): Pair<Intent, Uri>? {
        return uploadManager.prepareCameraCaptureIntent(context)
    }

    fun requestCameraForUpload(onResult: (Boolean) -> Unit) {
        val origin = state.currentUrl.ifBlank { currentConfig.initialUrl }
        permissionManager.requestCameraForUpload(origin, onResult)
    }

    fun completeUploadResult(resultCode: Int, data: Intent?) {
        val uris = uploadManager.parseActivityResult(resultCode, data)
        uploadManager.complete(uris)
        uploadCallbackSink = null
        updateState { it.copy(pendingUploadOptions = null) }
    }

    fun completeCameraCapture(success: Boolean) {
        uploadManager.completeWithCameraSuccess(success)
        uploadCallbackSink = null
        updateState { it.copy(pendingUploadOptions = null) }
    }

    fun cancelUpload() {
        uploadManager.cancelPending()
        uploadCallbackSink = null
        updateState { it.copy(pendingUploadOptions = null) }
    }

    fun refreshDownloads() {
        val updated = state.activeDownloads.map { item ->
            if (item.status == DownloadItemStatus.RUNNING || item.status == DownloadItemStatus.PENDING || item.status == DownloadItemStatus.PAUSED) {
                downloadManager.refreshDownloadStatus(item)
            } else {
                item
            }
        }
        updateState { it.copy(activeDownloads = updated) }
    }

    fun cancelDownload(downloadId: Long) {
        downloadManager.cancelDownload(downloadId)
        updateState { s ->
            s.copy(
                activeDownloads = s.activeDownloads.map {
                    if (it.downloadId == downloadId) it.copy(status = DownloadItemStatus.CANCELLED) else it
                }
            )
        }
    }

    fun openDownload(item: WebDownloadItem): Boolean = downloadManager.openDownloadedFile(item)

    fun closeInternalPopup() {
        val pView = popupWebView
        popupWebView = null
        onShowPopupWebView(null)
        lifecycleManager.destroy(pView, WebViewFactory.JS_BRIDGE_NAME)
        updateState { it.copy(hasInternalPopup = false) }
    }

    fun requestClearSiteDataConfirmation() {
        updateState { it.copy(confirmClearSiteData = true) }
    }

    fun confirmClearSiteData(proceed: Boolean) {
        updateState { it.copy(confirmClearSiteData = false) }
        if (proceed) {
            WebStorageManager.clearAllSiteData(webView)
            updateState {
                it.copy(
                    hasSessionCookies = false,
                    blockedNotice = "کوکی‌ها، حافظه سایت و کش موقت پاک شدند.",
                )
            }
        }
    }

    fun clearSessionCache() {
        WebStorageManager.clearAllSiteData(webView)
        updateState { it.copy(hasSessionCookies = false) }
    }

    fun saveWebViewState(outState: Bundle? = null): Bundle? =
        lifecycleManager.saveState(webView, outState)

    fun restoreWebViewState(inState: Bundle? = null): Boolean =
        lifecycleManager.restoreState(webView, inState)

    fun onLowMemory() {
        lifecycleManager.onLowMemory(webView)
    }

    fun onResume() {
        lifecycleManager.onResume(webView, popupWebView)
    }

    fun onPause() {
        lifecycleManager.saveState(webView)
        lifecycleManager.onPause(webView, popupWebView)
    }

    fun destroy() {
        errorHandler.stopReconnectMonitor()
        pendingJsResult?.cancel()
        pendingJsResult = null
        uploadManager.cancelPending()
        uploadCallbackSink = null
        closeInternalPopup()
        fullscreenManager.exitFullscreen()
        lifecycleManager.destroy(webView, WebViewFactory.JS_BRIDGE_NAME)
        webView = null
    }
}
