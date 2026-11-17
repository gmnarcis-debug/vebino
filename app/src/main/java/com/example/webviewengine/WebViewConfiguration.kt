package com.example.webviewengine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.domain.model.MixedContentOption

data class WebViewCapabilityReport(
    val androidSdkInt: Int,
    val webViewPackageName: String,
    val webViewVersionName: String,
    val supportsDomStorage: Boolean = true,
    val supportsIndexedDb: Boolean = true,
    val supportsCookies: Boolean = true,
    val supportsThirdPartyCookies: Boolean = true,
    val supportsSameSiteStrict: Boolean,
    val supportsWebRtc: Boolean = true,
    val supportsFileApi: Boolean = true,
    val supportsClipboardApi: Boolean = true,
    val supportsGeolocation: Boolean = true,
    val supportsWebShareApi: Boolean,
    val supportsWebNotifications: Boolean = false,
    val supportsWebSql: Boolean,
    val notesFa: List<String>,
)

/**
 * Inspects device WebView runtime capabilities and builds hardened WebSettings
 * tailored to each project's configuration and Android OS level.
 */
object WebViewConfiguration {

    fun inspectCapabilities(context: Context): WebViewCapabilityReport {
        val sdk = Build.VERSION.SDK_INT
        val pkgInfo = runCatching { WebView.getCurrentWebViewPackage() }.getOrNull()
        val pkgName = pkgInfo?.packageName ?: "com.google.android.webview"
        val versionName = pkgInfo?.versionName ?: "unknown"
        val majorVersion = versionName.substringBefore('.').toIntOrNull() ?: 0
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val hasMic = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

        val notes = buildList {
            if (!hasCamera) add("این دستگاه فاقد دوربین سخت‌افزاری است.")
            if (!hasMic) add("این دستگاه فاقد میکروفون سخت‌افزاری است.")
            add("Web Notifications استاندارد مرورگر در Android WebView پشتیبانی کامل ندارد و برای پوش‌نوتیفیکیشن از سرویس بومی استفاده می‌شود.")
            if (sdk >= 33) {
                add("WebSQL در نسخه‌های جدید WebView منسوخ شده و استفاده از DOM Storage / IndexedDB توصیه می‌شود.")
            }
            add("پخش ویدئوهای دارای DRM اختصاصی (Widevine L1) یا کدک‌های غیراستاندارد ممکن است در برخی وب‌سایت‌ها با محدودیت WebView مواجه شود.")
        }

        return WebViewCapabilityReport(
            androidSdkInt = sdk,
            webViewPackageName = pkgName,
            webViewVersionName = versionName,
            supportsSameSiteStrict = majorVersion == 0 || majorVersion >= 80 || sdk >= 29,
            supportsWebShareApi = majorVersion == 0 || majorVersion >= 89,
            supportsWebNotifications = false,
            supportsWebSql = sdk < 33,
            notesFa = notes,
        )
    }

    @Suppress("SetJavaScriptEnabled", "DEPRECATION")
    fun applySettings(settings: WebSettings, config: WebViewEngineConfig) {
        settings.javaScriptEnabled = config.javaScriptEnabled
        settings.domStorageEnabled = config.domStorageEnabled
        settings.databaseEnabled = config.webSqlEnabled || config.domStorageEnabled

        // Strict file-access hardening against local file exfiltration
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false

        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(config.zoomEnabled)
        settings.builtInZoomControls = config.zoomEnabled
        settings.displayZoomControls = false

        settings.setGeolocationEnabled(config.geolocationAllowed)
        settings.javaScriptCanOpenWindowsAutomatically = config.popupsAllowed
        settings.setSupportMultipleWindows(config.multipleWindowsAllowed)
        settings.mediaPlaybackRequiresUserGesture = !config.webRtcAllowed
        settings.cacheMode = config.cacheMode.androidConst

        settings.mixedContentMode = when (config.mixedContentMode) {
            MixedContentOption.NEVER_ALLOW -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
            MixedContentOption.COMPATIBILITY -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        if (config.customUserAgent.isNotBlank()) {
            settings.userAgentString = config.customUserAgent.trim()
        }
    }
}
