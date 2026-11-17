package com.example.data.local.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val websiteUrl: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val description: String = "",
    val templateId: String = "store",
    val faviconUrl: String? = null,
    val changeHistoryJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "project_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ProjectSettingsEntity(
    @PrimaryKey val projectId: Long,
    val appName: String,
    val developerName: String = "",
    val iconFilePath: String? = null,
    val splashImagePath: String? = null,
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
    // Advanced features
    val enablePushIntegration: Boolean = false,
    val pushProviderNote: String = "",
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
)

@Entity(
    tableName = "web_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class WebSettingsEntity(
    @PrimaryKey val projectId: Long,
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

@Entity(
    tableName = "build_tasks",
    indices = [Index("projectId")]
)
data class BuildTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val outputFormat: String = "APK",
    val signingKeyId: Long = 0L,
    val status: String = "QUEUED",
    val currentStep: String = "CHECKING_ENV",
    val progressPercent: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val errorMessage: String? = null,
    val workRequestId: String? = null,
)

@Entity(
    tableName = "build_results",
    indices = [Index("projectId"), Index("taskId")]
)
data class BuildResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val projectId: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val format: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val isSigned: Boolean,
    val isVerified: Boolean,
    val certFingerprintSha256: String,
    val validationSummary: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "signing_keys")
data class SigningKeyMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val keystorePath: String,
    val keystoreType: String = "PKCS12",
    val commonName: String,
    val organization: String,
    val country: String = "IR",
    val validityYears: Int = 25,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val encryptedPasswordToken: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "build_logs",
    indices = [Index("taskId")]
)
data class BuildLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val stepName: String,
    val message: String,
)

@Entity(tableName = "app_templates")
data class AppTemplateEntity(
    @PrimaryKey val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val primaryHex: String,
    val backgroundHex: String,
    val recommendedToolbar: String,
    val recommendedCache: String,
    val recommendedJs: Boolean,
    val recommendedDom: Boolean,
    val recommendedPaymentBridge: Boolean,
)

@Entity(tableName = "recent_websites")
data class RecentWebsiteEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String = "",
    val faviconUrl: String? = null,
    val themeColorHex: String? = null,
    val isHttps: Boolean = true,
    val inspectedAt: Long = System.currentTimeMillis(),
)

data class FullProjectBundle(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val settings: ProjectSettingsEntity?,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val webSettings: WebSettingsEntity?,
)
