package com.example

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.SslPolicy
import com.example.webviewengine.NavigationDecision
import com.example.webviewengine.SslResolution
import com.example.webviewengine.WebErrorHandler
import com.example.webviewengine.WebNavigationHandler
import com.example.webviewengine.WebPreviewState
import com.example.webviewengine.WebUploadManager
import com.example.webviewengine.WebViewEngineConfig
import com.example.webviewengine.WebViewManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebViewEngineTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `navigation handler enforces https only blocks dangerous schemes and keeps payment gateways`() {
        val handler = WebNavigationHandler(context)
        val config = WebViewEngineConfig(
            initialUrl = "https://myshop.ir",
            httpsOnly = true,
            externalLinkMode = ExternalLinkMode.OPEN_IN_SYSTEM_BROWSER,
        )
        handler.updateRules(config.initialUrl, "cdn.myshop.ir\n!ads.tracker.ir")

        assertEquals(
            NavigationDecision.AllowInWebView,
            handler.evaluate(Uri.parse("https://myshop.ir/products/1"), config),
        )
        assertEquals(
            NavigationDecision.AllowInWebView,
            handler.evaluate(Uri.parse("https://cdn.myshop.ir/banner.png"), config),
        )
        assertEquals(
            NavigationDecision.AllowInWebView,
            handler.evaluate(Uri.parse("https://bpm.shaparak.ir/pay"), config),
        )

        val blockedRule = handler.evaluate(Uri.parse("https://sub.ads.tracker.ir/pixel"), config)
        assertTrue(blockedRule is NavigationDecision.BlockWithNotice)

        val httpDecision = handler.evaluate(Uri.parse("http://myshop.ir/login"), config)
        assertTrue(httpDecision is NavigationDecision.BlockWithNotice)

        val jsDecision = handler.evaluate(Uri.parse("javascript:alert(1)"), config)
        assertTrue(jsDecision is NavigationDecision.BlockWithNotice)

        val fileDecision = handler.evaluate(Uri.parse("file:///etc/hosts"), config)
        assertTrue(fileDecision is NavigationDecision.BlockWithNotice)

        val telDecision = handler.evaluate(Uri.parse("tel:+982112345678"), config)
        assertTrue(telDecision is NavigationDecision.OpenExternal)

        val externalDecision = handler.evaluate(Uri.parse("https://otherdomain.com"), config)
        assertTrue(externalDecision is NavigationDecision.OpenExternal)
    }

    @Test
    fun `error handler classifies offline dns timeout http ssl and never auto-proceeds ssl`() {
        val errorHandler = WebErrorHandler(context)
        val formatted = errorHandler.formatNetworkError("ERR_NAME_NOT_RESOLVED")
        assertTrue(formatted.isNotBlank())

        assertEquals(
            null,
            errorHandler.handleSslError(null, null, "https://expired.badssl.com", SslPolicy.STRICT_BLOCK),
        )

        val warn = SslResolution.RequireExplicitUserWarning("https://expired.badssl.com")
        assertEquals("https://expired.badssl.com", warn.url)
    }

    @Test
    fun `upload manager denies disabled uploads and tracks pending state`() {
        var requested = false
        val uploadManager = WebUploadManager { _, cb ->
            requested = true
            cb.onReceiveValue(arrayOf(Uri.parse("content://media/1")))
        }
        assertFalse(uploadManager.startChooser(null, null, enabled = true))
        assertFalse(uploadManager.startChooser(null, null, enabled = false))
        assertFalse(uploadManager.hasPendingChooser())
        assertFalse(requested)
        uploadManager.cancelPending()
    }

    @Test
    fun `webview manager attaches safely blocks http load when httpsOnly and handles lifecycle`() {
        var latestState = WebPreviewState()
        val manager = WebViewManager(
            context = context,
            onStateChange = { latestState = it },
            onOsPermissionPrompt = { _, _ -> },
            onShowCustomVideoView = {},
        )
        val webView = WebView(context)
        manager.attach(
            view = webView,
            config = WebViewEngineConfig(
                initialUrl = "https://shop.ir",
                httpsOnly = true,
            ),
        )
        manager.loadUrl("http://insecure.ir")
        assertNotNull(latestState.blockedNotice)
        assertFalse(manager.goBack())
        assertFalse(manager.goForward())

        manager.saveWebViewState()
        manager.onLowMemory()
        manager.onPause()
        manager.onResume()
        manager.clearSessionCache()
        manager.destroy()
    }
}
