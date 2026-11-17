package com.example.core

import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.ProjectEntity
import com.example.data.local.db.ProjectSettingsEntity
import com.example.data.local.db.WebSettingsEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Safe, schema-validated JSON serializer for exporting and importing WEBINO project
 * configurations. Rejects oversized payloads, dangerous URL schemes, invalid package
 * names, and malformed color tokens.
 */
object ProjectConfigSerializer {

    const val MAX_IMPORT_BYTES = 256 * 1024 // 256 KB strict safety cap
    const val SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")
    private val safeSchemeRegex = Regex("^https?://.+", RegexOption.IGNORE_CASE)

    @Serializable
    data class ExportedProjectConfig(
        val schemaVersion: Int = SCHEMA_VERSION,
        val exportedAt: Long = System.currentTimeMillis(),
        val appName: String,
        val websiteUrl: String,
        val packageName: String,
        val versionName: String = "1.0.0",
        val versionCode: Int = 1,
        val description: String = "",
        val templateId: String = "store",
        val developerName: String = "",
        val primaryColorHex: String = "#7C3AED",
        val backgroundColorHex: String = "#0B1026",
        val themePreset: String = "DARK",
        val fontName: String = "VAZIRMATN",
        val toolbarStyle: String = "TOP_BAR",
        val startPagePath: String = "",
        val loadingAnimation: String = "PROGRESS_BAR",
        val splashEnabled: Boolean = true,
        val splashDurationMs: Int = 1400,
        val orientationMode: String = "AUTO",
        val fullscreenMode: Boolean = false,
        val showStatusBar: Boolean = true,
        val showNavigationBar: Boolean = true,
        val backBehavior: String = "HISTORY_THEN_EXIT",
        val supportRtl: Boolean = true,
        val enableDeepLinks: Boolean = false,
        val deepLinkScheme: String = "webino",
        val enableAppLinks: Boolean = false,
        val appLinkHost: String = "",
        val enableFileSharing: Boolean = true,
        val enablePhoneDialer: Boolean = true,
        val enableEmailIntent: Boolean = true,
        val enableMapsIntent: Boolean = true,
        val enablePaymentGatewayBridge: Boolean = true,
        val externalLinkMode: String = "ASK_OR_ALLOWLIST",
        val web: ExportedWebSettings = ExportedWebSettings(),
    )

    @Serializable
    data class ExportedWebSettings(
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
        val popupBehavior: String = "SAME_PAGE",
        val customUserAgent: String = "",
        val cacheMode: String = "DEFAULT",
        val zoomEnabled: Boolean = false,
        val mixedContentMode: String = "NEVER_ALLOW",
        val sslPolicy: String = "STRICT_BLOCK",
        val navigationRulesRaw: String = "",
        val httpsOnly: Boolean = true,
    )

    fun exportToJson(bundle: FullProjectBundle): String {
        val s = bundle.settings
        val w = bundle.webSettings
        val dto = ExportedProjectConfig(
            appName = s?.appName?.ifBlank { bundle.project.name } ?: bundle.project.name,
            websiteUrl = bundle.project.websiteUrl,
            packageName = bundle.project.packageName,
            versionName = bundle.project.versionName,
            versionCode = bundle.project.versionCode,
            description = bundle.project.description,
            templateId = bundle.project.templateId,
            developerName = s?.developerName.orEmpty(),
            primaryColorHex = s?.primaryColorHex ?: "#7C3AED",
            backgroundColorHex = s?.backgroundColorHex ?: "#0B1026",
            themePreset = s?.themePreset ?: "DARK",
            fontName = s?.fontName ?: "VAZIRMATN",
            toolbarStyle = s?.toolbarStyle ?: "TOP_BAR",
            startPagePath = s?.startPagePath.orEmpty(),
            loadingAnimation = s?.loadingAnimation ?: "PROGRESS_BAR",
            splashEnabled = s?.splashEnabled ?: true,
            splashDurationMs = s?.splashDurationMs ?: 1400,
            orientationMode = s?.orientationMode ?: "AUTO",
            fullscreenMode = s?.fullscreenMode ?: false,
            showStatusBar = s?.showStatusBar ?: true,
            showNavigationBar = s?.showNavigationBar ?: true,
            backBehavior = s?.backBehavior ?: "HISTORY_THEN_EXIT",
            supportRtl = s?.supportRtl ?: true,
            enableDeepLinks = s?.enableDeepLinks ?: false,
            deepLinkScheme = s?.deepLinkScheme ?: "webino",
            enableAppLinks = s?.enableAppLinks ?: false,
            appLinkHost = s?.appLinkHost.orEmpty(),
            enableFileSharing = s?.enableFileSharing ?: true,
            enablePhoneDialer = s?.enablePhoneDialer ?: true,
            enableEmailIntent = s?.enableEmailIntent ?: true,
            enableMapsIntent = s?.enableMapsIntent ?: true,
            enablePaymentGatewayBridge = s?.enablePaymentGatewayBridge ?: true,
            externalLinkMode = s?.externalLinkMode ?: "ASK_OR_ALLOWLIST",
            web = ExportedWebSettings(
                javaScriptEnabled = w?.javaScriptEnabled ?: true,
                domStorageEnabled = w?.domStorageEnabled ?: true,
                webSqlEnabled = w?.webSqlEnabled ?: false,
                clipboardAccessEnabled = w?.clipboardAccessEnabled ?: true,
                cookiesEnabled = w?.cookiesEnabled ?: true,
                thirdPartyCookiesEnabled = w?.thirdPartyCookiesEnabled ?: false,
                fileUploadEnabled = w?.fileUploadEnabled ?: true,
                downloadManagerEnabled = w?.downloadManagerEnabled ?: true,
                cameraPermission = w?.cameraPermission ?: false,
                microphonePermission = w?.microphonePermission ?: false,
                geolocationEnabled = w?.geolocationEnabled ?: false,
                webRtcEnabled = w?.webRtcEnabled ?: false,
                fullscreenVideoEnabled = w?.fullscreenVideoEnabled ?: true,
                multipleWindowsEnabled = w?.multipleWindowsEnabled ?: false,
                popupsEnabled = w?.popupsEnabled ?: false,
                popupBehavior = w?.popupBehavior ?: "SAME_PAGE",
                customUserAgent = w?.customUserAgent.orEmpty(),
                cacheMode = w?.cacheMode ?: "DEFAULT",
                zoomEnabled = w?.zoomEnabled ?: false,
                mixedContentMode = w?.mixedContentMode ?: "NEVER_ALLOW",
                sslPolicy = w?.sslPolicy ?: "STRICT_BLOCK",
                navigationRulesRaw = w?.navigationRulesRaw.orEmpty(),
                httpsOnly = w?.httpsOnly ?: true,
            ),
        )
        return json.encodeToString(dto)
    }

    fun parseAndValidate(rawJson: String): Result<ExportedProjectConfig> = runCatching {
        val bytes = rawJson.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= MAX_IMPORT_BYTES) {
            "حجم فایل JSON نامعتبر یا بیش از حد مجاز است."
        }
        val parsed = json.decodeFromString<ExportedProjectConfig>(rawJson)
        require(parsed.schemaVersion in 1..SCHEMA_VERSION) {
            "نسخه فایل پیکربندی پشتیبانی نمی‌شود."
        }
        val cleanName = parsed.appName.trim()
        require(cleanName.isNotEmpty() && cleanName.length <= 64) {
            "نام اپلیکیشن در فایل JSON معتبر نیست."
        }
        val cleanUrl = ValidationUtils.normalizeUrl(parsed.websiteUrl)
        require(safeSchemeRegex.matches(cleanUrl) && ValidationUtils.isValidWebUrl(cleanUrl)) {
            "آدرس وب‌سایت در فایل JSON غیرمجاز یا ناامن است."
        }
        require(ValidationUtils.isValidPackageName(parsed.packageName.trim())) {
            "نام بسته (Package Name) در فایل JSON معتبر نیست."
        }
        require(hexColorRegex.matches(parsed.primaryColorHex) && hexColorRegex.matches(parsed.backgroundColorHex)) {
            "کد رنگ در فایل JSON معتبر نیست."
        }
        require(parsed.versionCode in 1..1_000_000) {
            "کد نسخه نامعتبر است."
        }
        parsed.copy(
            appName = cleanName,
            websiteUrl = cleanUrl,
            packageName = parsed.packageName.trim(),
            splashDurationMs = parsed.splashDurationMs.coerceIn(400, 5000),
            startPagePath = parsed.startPagePath.take(256),
            deepLinkScheme = parsed.deepLinkScheme.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "webino" },
        )
    }

    fun toEntities(config: ExportedProjectConfig, projectId: Long = 0L): Triple<ProjectEntity, ProjectSettingsEntity, WebSettingsEntity> {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = projectId,
            name = config.appName,
            websiteUrl = config.websiteUrl,
            packageName = config.packageName,
            versionName = config.versionName.ifBlank { "1.0.0" }.take(24),
            versionCode = config.versionCode,
            description = config.description.take(500),
            templateId = config.templateId,
            createdAt = now,
            updatedAt = now,
        )
        val settings = ProjectSettingsEntity(
            projectId = projectId,
            appName = config.appName,
            developerName = config.developerName.take(64),
            primaryColorHex = config.primaryColorHex,
            backgroundColorHex = config.backgroundColorHex,
            themePreset = config.themePreset,
            fontName = config.fontName,
            toolbarStyle = config.toolbarStyle,
            startPagePath = config.startPagePath,
            loadingAnimation = config.loadingAnimation,
            splashEnabled = config.splashEnabled,
            splashDurationMs = config.splashDurationMs,
            orientationMode = config.orientationMode,
            fullscreenMode = config.fullscreenMode,
            showStatusBar = config.showStatusBar,
            showNavigationBar = config.showNavigationBar,
            backBehavior = config.backBehavior,
            supportRtl = config.supportRtl,
            enableDeepLinks = config.enableDeepLinks,
            deepLinkScheme = config.deepLinkScheme,
            enableAppLinks = config.enableAppLinks,
            appLinkHost = config.appLinkHost.take(128),
            enableFileSharing = config.enableFileSharing,
            enablePhoneDialer = config.enablePhoneDialer,
            enableEmailIntent = config.enableEmailIntent,
            enableMapsIntent = config.enableMapsIntent,
            enablePaymentGatewayBridge = config.enablePaymentGatewayBridge,
            externalLinkMode = config.externalLinkMode,
        )
        val web = WebSettingsEntity(
            projectId = projectId,
            javaScriptEnabled = config.web.javaScriptEnabled,
            domStorageEnabled = config.web.domStorageEnabled,
            webSqlEnabled = config.web.webSqlEnabled,
            clipboardAccessEnabled = config.web.clipboardAccessEnabled,
            cookiesEnabled = config.web.cookiesEnabled,
            thirdPartyCookiesEnabled = config.web.thirdPartyCookiesEnabled,
            fileUploadEnabled = config.web.fileUploadEnabled,
            downloadManagerEnabled = config.web.downloadManagerEnabled,
            cameraPermission = config.web.cameraPermission,
            microphonePermission = config.web.microphonePermission,
            geolocationEnabled = config.web.geolocationEnabled,
            webRtcEnabled = config.web.webRtcEnabled,
            fullscreenVideoEnabled = config.web.fullscreenVideoEnabled,
            multipleWindowsEnabled = config.web.multipleWindowsEnabled,
            popupsEnabled = config.web.popupsEnabled,
            popupBehavior = config.web.popupBehavior,
            customUserAgent = config.web.customUserAgent.take(256),
            cacheMode = config.web.cacheMode,
            zoomEnabled = config.web.zoomEnabled,
            mixedContentMode = config.web.mixedContentMode,
            sslPolicy = config.web.sslPolicy,
            navigationRulesRaw = config.web.navigationRulesRaw.take(1024),
            httpsOnly = config.web.httpsOnly,
        )
        return Triple(project, settings, web)
    }
}
