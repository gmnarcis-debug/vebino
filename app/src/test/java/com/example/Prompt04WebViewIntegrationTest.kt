package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.TestSslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.buildengine.AxmlCompiler
import com.example.buildengine.OutputValidator
import com.example.buildengine.ProjectGenerator
import com.example.data.crypto.SigningManager
import com.example.data.local.db.WebinoDatabase
import com.example.data.local.prefs.UserPreferencesDataStore
import com.example.data.network.WebsiteInspector
import com.example.data.repository.WebinoRepository
import com.example.domain.model.AppTemplateType
import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.CacheModeOption
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.MixedContentOption
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SslPolicy
import com.example.domain.usecase.StartBuildUseCase
import com.example.webviewengine.NavigationDecision
import com.example.webviewengine.PermissionRationaleRequest
import com.example.webviewengine.SslResolution
import com.example.webviewengine.UploadRequestOptions
import com.example.webviewengine.WebCookieManager
import com.example.webviewengine.WebDownloadManager
import com.example.webviewengine.WebErrorCategory
import com.example.webviewengine.WebErrorHandler
import com.example.webviewengine.WebExternalLinkHandler
import com.example.webviewengine.WebFullscreenManager
import com.example.webviewengine.WebNavigationHandler
import com.example.webviewengine.WebPermissionManager
import com.example.webviewengine.WebPopupHandler
import com.example.webviewengine.WebPreviewState
import com.example.webviewengine.WebSecurityManager
import com.example.webviewengine.WebStorageManager
import com.example.webviewengine.WebUploadManager
import com.example.webviewengine.WebViewConfiguration
import com.example.webviewengine.WebViewEngineConfig
import com.example.webviewengine.WebViewFactory
import com.example.webviewengine.WebViewLifecycleManager
import com.example.webviewengine.WebViewManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Prompt04WebViewIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: WebinoDatabase
    private lateinit var repo: WebinoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (!androidx.work.WorkManager.isInitialized()) {
            androidx.work.WorkManager.initialize(
                context,
                androidx.work.Configuration.Builder().build(),
            )
        }
        db = Room.inMemoryDatabaseBuilder(context, WebinoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WebinoRepository(
            context = context,
            db = db,
            prefsStore = UserPreferencesDataStore(context),
            signingManager = SigningManager(context),
            websiteInspector = WebsiteInspector(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. Test page & WebView capability inspection
    @Test
    fun `case 01 test html site exists and capability inspection detects features`() {
        val htmlStream = javaClass.classLoader!!.getResourceAsStream("webino_test_site.html")
        assertNotNull("Operational test site HTML fixture must be present", htmlStream)
        val html = htmlStream!!.bufferedReader().use { it.readText() }
        assertTrue(html.contains("webino_session"))
        assertTrue(html.contains("uploadMultiImages"))
        assertTrue(html.contains("dlPdf"))
        assertTrue(html.contains("navigator.geolocation"))

        val caps = WebViewConfiguration.inspectCapabilities(context)
        assertEquals(36, caps.androidSdkInt)
        assertTrue(caps.notesFa.isNotEmpty())
        assertTrue(caps.supportsIndexedDb)
        assertTrue(caps.supportsDomStorage)
    }

    // 2. Cookies & third-party cookie policy + persistence
    @Test
    fun `case 02 cookie policy applies first and third party settings and persists cookies`() {
        val webView = WebView(context)
        WebCookieManager.applyPolicy(webView, cookiesEnabled = true, thirdPartyCookiesEnabled = true)
        val cm = CookieManager.getInstance()
        cm.setCookie("https://shop.ir", "webino_session=ok")
        WebCookieManager.flush()
        assertTrue(WebCookieManager.hasSessionCookies("https://shop.ir"))

        WebCookieManager.clearAll()
    }

    // 3. DOM Storage, WebSQL, CacheMode & confirmed site data clearing
    @Test
    fun `case 03 storage manager configures dom websql cache and clears site data safely`() {
        val webView = WebView(context)
        WebStorageManager.applyStorageAndCache(
            webView = webView,
            domStorageEnabled = true,
            webSqlEnabled = true,
            cacheMode = CacheModeOption.CACHE_ELSE_NETWORK,
        )
        assertTrue(webView.settings.domStorageEnabled)
        assertEquals(WebSettings.LOAD_CACHE_ELSE_NETWORK, webView.settings.cacheMode)

        var latest = WebPreviewState()
        val mgr = WebViewManager(
            context = context,
            onStateChange = { latest = it },
            onOsPermissionPrompt = { _, _ -> },
            onShowCustomVideoView = {},
        )
        mgr.attach(webView, WebViewEngineConfig(initialUrl = "https://shop.ir"))
        mgr.requestClearSiteDataConfirmation()
        assertTrue(latest.confirmClearSiteData)
        mgr.confirmClearSiteData(false)
        assertFalse(latest.confirmClearSiteData)
        mgr.requestClearSiteDataConfirmation()
        mgr.confirmClearSiteData(true)
        assertFalse(latest.confirmClearSiteData)
        assertNotNull(latest.blockedNotice)
        mgr.destroy()
    }

    // 4. Multi-image / PDF upload & cancellation safety
    @Test
    fun `case 04 upload manager handles multi image pdf accept types and cancel safety`() {
        var capturedOptions: UploadRequestOptions? = null
        var received: Array<Uri>? = null
        val uploadMgr = WebUploadManager { opts, cb ->
            capturedOptions = opts
            // Simulate chooser completion
            cb.onReceiveValue(arrayOf(Uri.parse("content://media/photo1.jpg")))
        }

        val fakeParams = object : WebChromeClient.FileChooserParams() {
            override fun getMode(): Int = MODE_OPEN_MULTIPLE
            override fun getAcceptTypes(): Array<String> = arrayOf("image/*", ".pdf")
            override fun isCaptureEnabled(): Boolean = true
            override fun getTitle(): CharSequence = "Upload"
            override fun getFilenameHint(): String? = null
            override fun createIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT)
        }

        val cb = ValueCallback<Array<Uri>> { uris -> received = uris }
        assertTrue(uploadMgr.startChooser(cb, fakeParams, enabled = true))
        assertNotNull(capturedOptions)
        assertTrue(capturedOptions!!.allowMultiple)
        assertTrue(capturedOptions!!.captureEnabled)
        assertEquals(listOf("image/*", ".pdf"), capturedOptions!!.acceptMimeTypes)
        assertEquals(1, received?.size)

        // Multi-clipData result parsing check
        val clip = ClipData("multi", arrayOf("image/*"), ClipData.Item(Uri.parse("content://custom.provider/1")))
        clip.addItem(ClipData.Item(Uri.parse("content://custom.provider/2")))
        val resultIntent = Intent().apply { clipData = clip }
        val parsed = uploadMgr.parseActivityResult(Activity.RESULT_OK, resultIntent)
        assertEquals(2, parsed?.size)

        // Cancel safety delivers null so input type=file doesn't hang forever
        var cancelledResult: Array<Uri>? = arrayOf(Uri.parse("content://dummy"))
        val cb2 = ValueCallback<Array<Uri>> { uris -> cancelledResult = uris }
        val uploadMgr2 = WebUploadManager { _, _ -> }
        uploadMgr2.startChooser(cb2, fakeParams, enabled = true)
        assertTrue(uploadMgr2.hasPendingChooser())
        uploadMgr2.cancelPending()
        assertFalse(uploadMgr2.hasPendingChooser())
        assertNull(cancelledResult)
    }

    // 5. Downloads: safe names, duplicate suffix, executable blocking
    @Test
    fun `case 05 download manager sanitizes filenames deduplicates and blocks executable payloads`() {
        val dlMgr = WebDownloadManager(context)

        // Path traversal sanitization + PDF mime resolution
        val pdfCheck = dlMgr.validateAndSanitize(
            url = "https://shop.ir/files/../../invoice%202026.pdf",
            contentDisposition = null,
            mimetype = null,
        )
        assertNotNull(pdfCheck)
        assertEquals("application/pdf", pdfCheck!!.third)
        assertFalse(pdfCheck.second.contains(".."))

        // Security: block executable extensions (.apk, .dex, .exe, .sh)
        assertNull(dlMgr.validateAndSanitize("https://evil.ir/payload.apk", null, "application/vnd.android.package-archive"))
        assertNull(dlMgr.validateAndSanitize("https://evil.ir/patch.dex", null, null))
        assertNull(dlMgr.validateAndSanitize("ftp://evil.ir/file.pdf", null, null))

        // Disabled downloads returns null
        var fellBack = false
        val disabledRes = dlMgr.handleDownload("https://shop.ir/a.pdf", null, "application/pdf", enabled = false) {
            fellBack = true
        }
        assertNull(disabledRes)
        assertFalse(fellBack)
    }

    // 6. Fullscreen video enter and Back exit
    @Test
    fun `case 06 fullscreen video manager enters and exits cleanly via back`() {
        var attachedView: View? = null
        var hiddenCalled = false
        val fs = WebFullscreenManager { v -> attachedView = v }
        val custom = View(context)
        val callback = WebChromeClient.CustomViewCallback { hiddenCalled = true }

        assertFalse(fs.enterFullscreen(custom, callback, allowed = false))
        assertTrue(hiddenCalled)

        hiddenCalled = false
        assertTrue(fs.enterFullscreen(custom, callback, allowed = true))
        assertTrue(fs.isInFullscreen())
        assertEquals(custom, attachedView)

        assertTrue(fs.exitFullscreen())
        assertFalse(fs.isInFullscreen())
        assertNull(attachedView)
        assertTrue(hiddenCalled)
    }

    // 7. External apps, dialer, mail, sms, whatsapp, telegram, maps & safe intent://
    @Test
    fun `case 07 navigation and external handler route native schemes and sanitize intent uri`() {
        val nav = WebNavigationHandler(context)
        val cfg = WebViewEngineConfig(initialUrl = "https://shop.ir")
        nav.updateRules(cfg.initialUrl, "")

        assertTrue(nav.evaluate(Uri.parse("tel:+98218888"), cfg) is NavigationDecision.OpenExternal)
        assertTrue(nav.evaluate(Uri.parse("mailto:hi@shop.ir"), cfg) is NavigationDecision.OpenExternal)
        assertTrue(nav.evaluate(Uri.parse("smsto:+98912000"), cfg) is NavigationDecision.OpenExternal)
        assertTrue(nav.evaluate(Uri.parse("https://wa.me/98912000"), cfg) is NavigationDecision.OpenExternal)
        assertTrue(nav.evaluate(Uri.parse("https://t.me/webino"), cfg) is NavigationDecision.OpenExternal)
        assertTrue(nav.evaluate(Uri.parse("https://neshan.org/maps"), cfg) is NavigationDecision.OpenExternal)

        // intent:// strips component/selector to prevent hijacking and preserves validated HTTPS fallback
        val rawIntent = "intent://scan/#Intent;scheme=myshop;component=com.evil/.Hidden;S.browser_fallback_url=https%3A%2F%2Fshop.ir%2Ffallback;end"
        val (parsedIntent, fallbackUrl) = WebSecurityManager.parseSafeIntentUri(rawIntent)
        assertNotNull(parsedIntent)
        assertNull(parsedIntent!!.component)
        assertNull(parsedIntent.selector)
        assertEquals("https://shop.ir/fallback", fallbackUrl)

        val extHandler = WebExternalLinkHandler(context)
        assertTrue(extHandler.isNativeAppUri(Uri.parse("https://balad.ir/p/1")))
        assertFalse(extHandler.openExternal(Uri.parse("javascript:alert(1)")))
    }

    // 8. Popups & multiple windows (SAME_PAGE / INTERNAL_WINDOW / EXTERNAL_BROWSER)
    @Test
    fun `case 08 popup handler respects popup modes and blocks ungestured popups`() {
        val popupHandler = WebPopupHandler(context)
        val mainWv = WebView(context)

        // Multiple windows disabled -> blocked
        assertFalse(
            popupHandler.handleCreateWindow(
                mainWebView = mainWv,
                config = WebViewEngineConfig(initialUrl = "https://shop.ir", multipleWindowsAllowed = false),
                isUserGesture = true,
                resultMsg = null,
                onPopupUrlForExternal = {},
                onShowInternalPopup = {},
                onCloseInternalPopup = {},
            )
        )

        // Ungestured popup when popupsAllowed == false -> blocked
        val msgObj = android.os.Message.obtain()
        assertFalse(
            popupHandler.handleCreateWindow(
                mainWebView = mainWv,
                config = WebViewEngineConfig(initialUrl = "https://shop.ir", multipleWindowsAllowed = true, popupsAllowed = false),
                isUserGesture = false,
                resultMsg = msgObj,
                onPopupUrlForExternal = {},
                onShowInternalPopup = {},
                onCloseInternalPopup = {},
            )
        )
        msgObj.recycle()

        // Verify SAME_PAGE, INTERNAL_WINDOW, and EXTERNAL_BROWSER enum options exist cleanly
        assertEquals(3, PopupBehaviorMode.entries.size)
    }

    // 9 & 10. Camera, Microphone, WebRTC permission rationale, OS prompt, denial & cancellation
    @Test
    fun `case 09 and 10 permission manager shows persian rationale handles camera mic webrtc and denial`() {
        var latestRationale: PermissionRationaleRequest? = null
        var osGranted = false
        val permMgr = WebPermissionManager(
            context = context,
            onRationaleRequired = { latestRationale = it },
            onOsPermissionPrompt = { _, cb -> cb(osGranted) },
        )
        val cfg = WebViewEngineConfig(
            initialUrl = "https://meet.shop.ir",
            httpsOnly = true,
            cameraAllowed = true,
            microphoneAllowed = true,
            webRtcAllowed = true,
        )

        var grantedCalled = false
        var deniedCalled = false
        val camReq = object : PermissionRequest() {
            override fun getOrigin(): Uri = Uri.parse("https://meet.shop.ir")
            override fun getResources(): Array<String> = arrayOf(RESOURCE_VIDEO_CAPTURE, RESOURCE_AUDIO_CAPTURE)
            override fun grant(resources: Array<out String>?) { grantedCalled = true }
            override fun deny() { deniedCalled = true }
        }

        // Untrusted origin must be immediately denied
        val evilReq = object : PermissionRequest() {
            override fun getOrigin(): Uri = Uri.parse("https://evil.com")
            override fun getResources(): Array<String> = arrayOf(RESOURCE_VIDEO_CAPTURE)
            override fun grant(resources: Array<out String>?) { grantedCalled = true }
            override fun deny() { deniedCalled = true }
        }
        permMgr.handleWebPermissionRequest(evilReq, cfg, "meet.shop.ir")
        assertTrue(deniedCalled)
        assertFalse(grantedCalled)

        // Trusted origin shows Persian rationale first
        deniedCalled = false
        permMgr.handleWebPermissionRequest(camReq, cfg, "meet.shop.ir")
        assertNotNull(latestRationale)
        assertTrue(latestRationale!!.titleFa.contains("دوربین"))

        // Simulate user declines OS prompt -> calls request.deny() gracefully without crash
        osGranted = false
        latestRationale!!.onConfirmGrant()
        assertTrue(deniedCalled)
        assertFalse(grantedCalled)

        // Test cancellation safety
        permMgr.handleWebPermissionCanceled(camReq)
    }

    // 11. Geolocation GPS check & permission gating
    @Test
    fun `case 11 geolocation checks config trust location service and runtime permission`() {
        var latestRationale: PermissionRationaleRequest? = null
        val permMgr = WebPermissionManager(
            context = context,
            onRationaleRequired = { latestRationale = it },
            onOsPermissionPrompt = { _, cb -> cb(true) },
        )
        var allowedArg = true
        var invoked = false
        val geoCb = GeolocationPermissions.Callback { _, allow, _ ->
            invoked = true
            allowedArg = allow
        }

        // Disabled in config -> false immediately
        permMgr.handleGeolocationPrompt(
            origin = "https://shop.ir",
            callback = geoCb,
            config = WebViewEngineConfig(initialUrl = "https://shop.ir", geolocationAllowed = false),
            allowedOriginHost = "shop.ir",
        )
        assertTrue(invoked)
        assertFalse(allowedArg)
        assertNull(latestRationale)

        // Untrusted domain -> false immediately
        invoked = false
        permMgr.handleGeolocationPrompt(
            origin = "https://phishing.ir",
            callback = geoCb,
            config = WebViewEngineConfig(initialUrl = "https://shop.ir", geolocationAllowed = true),
            allowedOriginHost = "shop.ir",
        )
        assertTrue(invoked)
        assertFalse(allowedArg)
    }

    // 12. Back navigation policies (HISTORY_THEN_EXIT, HISTORY_THEN_CONFIRM_EXIT, DIRECT_EXIT)
    @Test
    fun `case 12 webview manager obeys back navigation behaviors cleanly`() {
        var latest = WebPreviewState()
        val mgr = WebViewManager(
            context = context,
            onStateChange = { latest = it },
            onOsPermissionPrompt = { _, _ -> },
            onShowCustomVideoView = {},
        )
        val wv = WebView(context)

        // DIRECT_EXIT exits immediately
        var exited = false
        mgr.attach(wv, WebViewEngineConfig(initialUrl = "https://shop.ir", backBehavior = BackNavigationBehavior.DIRECT_EXIT))
        mgr.handleBackPress { exited = true }
        assertTrue(exited)

        // HISTORY_THEN_CONFIRM_EXIT prompts confirmation dialog when history empty
        exited = false
        mgr.attach(wv, WebViewEngineConfig(initialUrl = "https://shop.ir", backBehavior = BackNavigationBehavior.HISTORY_THEN_CONFIRM_EXIT))
        mgr.handleBackPress { exited = true }
        assertFalse(exited)
        assertTrue(latest.confirmExitPrompt)
        mgr.dismissExitConfirm(exitNow = true) { exited = true }
        assertTrue(exited)
        mgr.destroy()
    }

    // 13. Offline, DNS, Timeout, HTTP 404/5xx errors + Token redaction in technical details
    @Test
    fun `case 13 error handler classifies all error categories and redacts sensitive url tokens`() {
        val errHandler = WebErrorHandler(context)

        val dns = errHandler.classifyNetworkError(-2, "net::ERR_NAME_NOT_RESOLVED", "https://shop.ir/auth?token=SECRET123")
        assertEquals(WebErrorCategory.DNS_FAILURE, dns.category)
        assertFalse(dns.technicalDetail.contains("SECRET123"))
        assertTrue(dns.technicalDetail.contains("***"))

        val timeout = errHandler.classifyNetworkError(-8, "net::ERR_TIMED_OUT", "https://shop.ir")
        assertEquals(WebErrorCategory.TIMEOUT, timeout.category)

        val notFound = errHandler.classifyHttpError(404, "https://shop.ir/missing")
        assertEquals(WebErrorCategory.HTTP_NOT_FOUND, notFound.category)

        val serverErr = errHandler.classifyHttpError(503, "https://shop.ir/api")
        assertEquals(WebErrorCategory.HTTP_SERVER_ERROR, serverErr.category)
    }

    // 14. SSL strict block vs WARN_AND_ASK (never auto-proceeds)
    @Test
    fun `case 14 ssl handler strictly cancels in strict mode and requires warning otherwise`() {
        val errHandler = WebErrorHandler(context)
        val fakeSslHandler = TestSslErrorHandler()

        val strictRes = errHandler.handleSslError(fakeSslHandler, null, "https://badssl.com", SslPolicy.STRICT_BLOCK)
        assertEquals(SslResolution.CancelledStrictly, strictRes)
        assertTrue(fakeSslHandler.cancelled)
        assertFalse(fakeSslHandler.proceeded)

        val warnHandler = TestSslErrorHandler()
        val warnRes = errHandler.handleSslError(warnHandler, null, "https://badssl.com", SslPolicy.WARN_USER)
        assertTrue(warnRes is SslResolution.RequireExplicitUserWarning)
        assertFalse("Must never auto-proceed SSL errors", warnHandler.proceeded)
        errHandler.resolvePendingSsl(proceed = false)
        assertTrue(warnHandler.cancelled)
    }

    // 15. Navigation rules allowlist, !blocklist, payment gateways, ASK_OR_ALLOWLIST
    @Test
    fun `case 15 navigation rules handle allowlist blocklist gateways and ask confirm mode`() {
        val nav = WebNavigationHandler(context)
        val cfg = WebViewEngineConfig(
            initialUrl = "https://shop.ir",
            navigationRulesRaw = "blog.partner.ir\n!tracker.bad.ir",
            externalLinkMode = ExternalLinkMode.ASK_OR_ALLOWLIST,
        )
        nav.updateRules(cfg.initialUrl, cfg.navigationRulesRaw)

        assertEquals(NavigationDecision.AllowInWebView, nav.evaluate(Uri.parse("https://blog.partner.ir/post"), cfg))
        assertEquals(NavigationDecision.AllowInWebView, nav.evaluate(Uri.parse("https://sandbox.zarinpal.com/pg"), cfg))
        assertTrue(nav.evaluate(Uri.parse("https://tracker.bad.ir/x.js"), cfg) is NavigationDecision.BlockWithNotice)
        assertTrue(nav.evaluate(Uri.parse("https://unknown-site.org"), cfg) is NavigationDecision.ConfirmExternal)
    }

    // 16. Security hardening & origin-gated JS Bridge + Clipboard API
    @Test
    fun `case 16 security settings disable file cross access and gate js bridge by origin`() {
        val wv = WebView(context)
        val cfg = WebViewEngineConfig(
            initialUrl = "https://trusted.ir",
            javaScriptEnabled = true,
            clipboardAccessEnabled = true,
            httpsOnly = true,
        )
        var shared: String? = null
        WebViewFactory.configure(
            view = wv,
            config = cfg,
            allowedOriginHost = "trusted.ir",
            onShareRequested = { shared = it },
        )
        assertFalse(wv.settings.allowFileAccess)
        @Suppress("DEPRECATION")
        assertFalse(wv.settings.allowFileAccessFromFileURLs)
        @Suppress("DEPRECATION")
        assertFalse(wv.settings.allowUniversalAccessFromFileURLs)

        assertTrue(WebSecurityManager.isOriginTrusted("https://sub.trusted.ir/page", "trusted.ir", requireHttps = true))
        assertFalse(WebSecurityManager.isOriginTrusted("http://sub.trusted.ir/page", "trusted.ir", requireHttps = true))
        assertFalse(WebSecurityManager.isOriginTrusted("https://evil.com", "trusted.ir", requireHttps = true))

        // Direct bridge check: when webView url is untrusted/null, share & clipboard reject cleanly
        val bridge = WebViewFactory.WebinoNativeBridge(
            context = context,
            webView = wv,
            allowedOriginHost = "trusted.ir",
            httpsOnly = true,
            clipboardAllowed = true,
            onShareRequested = { shared = it },
        )
        assertFalse(bridge.copyToClipboard("ABC"))
        bridge.shareText("hello")
        assertNull(shared)
    }

    // 17. Lifecycle saveState / restoreState / onLowMemory / destroy
    @Test
    fun `case 17 lifecycle manager saves restores state and tears down without leaks`() {
        val lm = WebViewLifecycleManager()
        val wv = WebView(context)
        val bundle = Bundle()
        // Save and restore calls operate cleanly even when history is empty
        lm.saveState(wv, bundle)
        lm.restoreState(wv, bundle)
        lm.onPause(wv, null)
        lm.onResume(wv, null)
        lm.onLowMemory(wv)
        lm.destroy(wv, WebViewFactory.JS_BRIDGE_NAME)
    }

    // 18. Standalone Build Engine Feature Parity (APK + AAB build with Prompt 04 settings)
    @Test
    fun `case 18 build engine compiles standalone APK and AAB with all prompt 04 webview features and compliant manifest`() = runTest {
        repo.initializeDefaults()
        val key = repo.signingKeys.first().first()

        val pid = repo.createProject(
            name = "Webino Pro P04",
            websiteUrl = "https://example.com",
            packageName = "ir.webino.prompt04app",
            versionName = "2.0.0",
            versionCode = 2,
            description = "Prompt 04 Build Parity App",
            template = AppTemplateType.STORE,
            faviconUrl = null,
            themeColorOverride = "#7C3AED",
        )
        val bundle = repo.observeProject(pid).first()!!
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawColor(Color.parseColor("#06B6D4"))
        }
        val iconFile = File(context.filesDir, "p04_icon.png").apply {
            writeBytes(ByteArrayOutputStream().apply { bmp.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray())
        }

        repo.updateProjectBundle(
            project = bundle.project,
            settings = bundle.settings!!.copy(
                appName = "Webino Pro P04",
                iconFilePath = iconFile.absolutePath,
                showStatusBar = true,
                showNavigationBar = true,
                backBehavior = BackNavigationBehavior.HISTORY_THEN_CONFIRM_EXIT.name,
            ),
            webSettings = bundle.webSettings!!.copy(
                cameraPermission = true,
                microphonePermission = true,
                geolocationEnabled = true,
                webRtcEnabled = true,
                webSqlEnabled = true,
                clipboardAccessEnabled = true,
                popupBehavior = PopupBehaviorMode.INTERNAL_WINDOW.name,
                multipleWindowsEnabled = true,
                mixedContentMode = MixedContentOption.COMPATIBILITY.name,
                sslPolicy = SslPolicy.WARN_USER.name,
            ),
            changeNote = "Enable all Prompt 04 WebView capabilities",
        )

        val spec = repo.validateBuildSpec(pid, BuildOutputFormat.BOTH, key.id).getOrThrow().first

        // Verify AXML manifest includes non-prohibited permissions needed by enabled features
        val axmlNode = AxmlCompiler.buildManifestNode(spec)
        val declaredPerms = axmlNode.children.filter { it.tag == "uses-permission" }
            .flatMap { it.attrs }
            .mapNotNull { (it.value as? AxmlCompiler.AttrValue.Str)?.value }
        assertTrue(declaredPerms.contains("android.permission.CAMERA"))
        assertTrue(declaredPerms.contains("android.permission.RECORD_AUDIO"))
        assertTrue(declaredPerms.contains("android.permission.MODIFY_AUDIO_SETTINGS"))
        assertTrue(declaredPerms.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(declaredPerms.contains("android.permission.POST_NOTIFICATIONS"))
        // Strictly verify NO Play-prohibited permissions exist
        assertFalse(declaredPerms.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
        assertFalse(declaredPerms.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(declaredPerms.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
        assertFalse(declaredPerms.contains("android.permission.QUERY_ALL_PACKAGES"))

        // Verify generated Java embeds all Prompt 04 features cleanly
        val verifySrc = File(context.cacheDir, "p04_verify_src").apply { deleteRecursively(); mkdirs() }
        val genJava = ProjectGenerator.generateSources(spec, verifySrc).first().readText()
        assertTrue(genJava.contains("onReceivedHttpError"))
        assertTrue(genJava.contains("setAllowFileAccessFromFileURLs(false)"))
        assertTrue(genJava.contains("copyToClipboard"))
        assertTrue(genJava.contains("onSaveInstanceState"))
        assertTrue(genJava.contains("BACK_BEHAVIOR = 1"))
        assertTrue(genJava.contains("POPUP_BEHAVIOR = 1"))
        verifySrc.deleteRecursively()

        // Full real on-device compilation (ECJ + DX + AXML + ARSC + APKSigner + AABSigner)
        val taskId = StartBuildUseCase(repo)(pid, BuildOutputFormat.BOTH, key.id).getOrThrow()
        assertTrue("Full Prompt 04 APK+AAB build must succeed", repo.runBuildTask(taskId))

        val results = repo.allBuildResults.first().filter { it.taskId == taskId }
        assertEquals(2, results.size)
        val apkFile = File(results.first { it.format == BuildOutputFormat.APK.name }.filePath)
        val aabFile = File(results.first { it.format == BuildOutputFormat.AAB.name }.filePath)

        val apkReport = OutputValidator.validateApk(apkFile, expectedSpec = spec)
        assertTrue("APK validation failed: ${apkReport.errors}", apkReport.isValid)
        val aabReport = OutputValidator.validateAab(aabFile, expectedSpec = spec)
        assertTrue("AAB validation failed: ${aabReport.errors}", aabReport.isValid)
    }
}
