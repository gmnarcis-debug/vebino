package com.example.buildengine

import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStep
import com.example.domain.model.CacheModeOption
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.LoadingAnimationType
import com.example.domain.model.MixedContentOption
import com.example.domain.model.OrientationMode
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SslPolicy
import com.example.domain.model.ThemePreset
import com.example.domain.model.ToolbarStyle
import java.security.PrivateKey
import java.security.cert.X509Certificate

data class BuildSpec(
    val projectId: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val websiteUrl: String,
    val startPagePath: String = "",
    val developerName: String = "",
    val primaryColorHex: String = "#7C3AED",
    val backgroundColorHex: String = "#0B1026",
    val themePreset: ThemePreset = ThemePreset.DARK,
    val toolbarStyle: ToolbarStyle = ToolbarStyle.TOP_BAR,
    val loadingAnimation: LoadingAnimationType = LoadingAnimationType.PROGRESS_BAR,
    val splashEnabled: Boolean = true,
    val splashDurationMs: Int = 1400,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val fullscreenMode: Boolean = false,
    val showStatusBar: Boolean = true,
    val showNavigationBar: Boolean = true,
    val backBehavior: BackNavigationBehavior = BackNavigationBehavior.HISTORY_THEN_EXIT,
    val supportRtl: Boolean = true,
    // WebView settings
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val webSqlEnabled: Boolean = false,
    val clipboardAccessEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val fileUploadEnabled: Boolean = true,
    val downloadManagerEnabled: Boolean = true,
    val cameraPermission: Boolean = false,
    val microphonePermission: Boolean = false,
    val geolocationEnabled: Boolean = false,
    val webRtcEnabled: Boolean = false,
    val fullscreenVideoEnabled: Boolean = true,
    val multipleWindowsEnabled: Boolean = false,
    val popupsEnabled: Boolean = false,
    val popupBehavior: PopupBehaviorMode = PopupBehaviorMode.SAME_PAGE,
    val customUserAgent: String = "",
    val cacheMode: CacheModeOption = CacheModeOption.DEFAULT,
    val zoomEnabled: Boolean = false,
    val mixedContentMode: MixedContentOption = MixedContentOption.NEVER_ALLOW,
    val sslPolicy: SslPolicy = SslPolicy.STRICT_BLOCK,
    val httpsOnly: Boolean = true,
    val navigationRulesRaw: String = "",
    // Advanced features
    val enableDeepLinks: Boolean = false,
    val deepLinkScheme: String = "webino",
    val enableAppLinks: Boolean = false,
    val appLinkHost: String = "",
    val enableFileSharing: Boolean = true,
    val enablePhoneDialer: Boolean = true,
    val enableEmailIntent: Boolean = true,
    val enableMapsIntent: Boolean = true,
    val enablePaymentGatewayBridge: Boolean = true,
    val externalLinkMode: ExternalLinkMode = ExternalLinkMode.ASK_OR_ALLOWLIST,
    // Media & Output
    val iconPngBytes: ByteArray? = null,
    val splashPngBytes: ByteArray? = null,
    val outputFormat: BuildOutputFormat = BuildOutputFormat.APK,
    val keyAlias: String = "webino_key",
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
)

data class BuildLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val step: BuildStep,
    val message: String,
)

data class BuildProgressUpdate(
    val step: BuildStep,
    val progressPercent: Int,
    val statusMessage: String,
    val logEntry: BuildLogEntry? = null,
)

data class GeneratedArtifactInfo(
    val format: BuildOutputFormat,
    val filePath: String,
    val fileSizeBytes: Long,
    val isSigned: Boolean,
    val isVerified: Boolean,
    val sha256CertFingerprint: String,
    val validationSummary: String,
)

data class BuildExecutionResult(
    val success: Boolean,
    val artifacts: List<GeneratedArtifactInfo>,
    val logs: List<BuildLogEntry>,
    val durationMs: Long,
    val errorMessage: String? = null,
    val cancelled: Boolean = false,
)

class BuildCancelledException(message: String = "عملیات ساخت توسط کاربر لغو شد.") : Exception(message)
