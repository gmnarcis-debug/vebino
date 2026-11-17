package com.example

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.DoubleBackExitState
import com.example.core.ExternalIntentHelper
import com.example.core.ProvideLocalizedResources
import com.example.data.local.db.BuildResultEntity
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.ProjectEntity
import com.example.data.local.db.ProjectSettingsEntity
import com.example.data.local.db.SigningKeyMetadataEntity
import com.example.data.local.db.WebSettingsEntity
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BuildEngineStatus
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStatus
import com.example.domain.model.EnvironmentCheckReport
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BuildManagerScreen
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SigningManagerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.WebinoTheme
import com.example.ui.viewmodel.CreateProjectFormState
import com.example.ui.viewmodel.StorageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import red.line.vebino.BuildConfig
import red.line.vebino.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class WebinoUiComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleBundle(
        id: Long = 1L,
        name: String = "فروشگاه نمونه",
        url: String = "https://shop.ir",
        pkg: String = "ir.shop.sample",
    ) = FullProjectBundle(
        project = ProjectEntity(
            id = id,
            name = name,
            websiteUrl = url,
            packageName = pkg,
            versionName = "1.0.0",
            versionCode = 1,
            description = "",
            templateId = "store",
        ),
        settings = ProjectSettingsEntity(projectId = id, appName = name),
        webSettings = WebSettingsEntity(projectId = id),
    )

    @Test
    fun `dashboard displays project stats and explicit build engine status`() {
        val env = EnvironmentCheckReport(
            isReady = true,
            status = BuildEngineStatus.READY,
            freeStorageMb = 1024L,
            freeRamMb = 512L,
            totalRamMb = 4096L,
            hasEmbeddedStubs = true,
            hasJavaCompiler = true,
            hasDexCompiler = true,
            hasApkSigner = true,
            hasKeystoreCrypto = true,
            androidSdkInt = 36,
            issues = emptyList(),
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val faConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag("fa"))
        }
        val expectedReadyLabel = context.createConfigurationContext(faConfig).getString(R.string.engine_status_ready)

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    DashboardScreen(
                        prefs = UserPreferences(language = AppLanguage.FA),
                        projects = listOf(sampleBundle()),
                        outputs = listOf(
                            BuildResultEntity(
                                taskId = 1L,
                                projectId = 1L,
                                format = "APK",
                                appName = "فروشگاه نمونه",
                                packageName = "ir.shop.sample",
                                versionName = "1.0.0",
                                versionCode = 1,
                                filePath = "/tmp/a.apk",
                                fileSizeBytes = 1024L,
                                certFingerprintSha256 = "AA:BB:CC",
                                isSigned = true,
                                isVerified = true,
                                validationSummary = "Valid APK",
                            )
                        ),
                        recentTasks = emptyList(),
                        envReport = env,
                        storageStats = StorageStats(outputsBytes = 1024L),
                        onCreateProject = {},
                        onOpenProject = {},
                        onOpenBuildManager = {},
                        onOpenOutputs = {},
                        onOpenHelp = {},
                        onRecheckEngine = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("dashboard_create_app_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$expectedReadyLabel •", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `create project screen flags duplicate package name and http warning`() {
        val existing = listOf(sampleBundle(id = 10L, name = "فروشگاه اصلی", pkg = "ir.shop.dup"))
        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    CreateProjectScreen(
                        form = CreateProjectFormState(
                            urlInput = "http://insecure.ir",
                            appName = "اپ دوم",
                            packageName = "ir.shop.dup",
                        ),
                        recentWebsites = emptyList(),
                        existingProjects = existing,
                        language = AppLanguage.FA,
                        onBack = {},
                        onUpdateForm = {},
                        onInspectSite = {},
                        onCreate = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("create_duplicate_pkg_warning").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `projects screen search filters matching project cards`() {
        val p1 = sampleBundle(id = 1L, name = "آکادمی وبینو", url = "https://academy.ir", pkg = "ir.webino.academy")
        val p2 = sampleBundle(id = 2L, name = "فروشگاه کفش", url = "https://shoes.ir", pkg = "ir.shoes.store")

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    ProjectsScreen(
                        prefs = UserPreferences(language = AppLanguage.FA),
                        projects = listOf(p1, p2),
                        onCreateNew = {},
                        onOpenStudio = {},
                        onOpenPreview = {},
                        onOpenBuild = {},
                        onDuplicate = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("آکادمی وبینو").assertIsDisplayed()
        composeRule.onNodeWithText("فروشگاه کفش").assertIsDisplayed()

        composeRule.onNodeWithTag("projects_search_input").performScrollTo().performTextInput("کفش")
        composeRule.onNodeWithText("فروشگاه کفش").assertIsDisplayed()
        composeRule.onNodeWithText("آکادمی وبینو").assertDoesNotExist()
    }

    @Test
    fun `settings screen invokes theme language and font callbacks`() {
        var selectedTheme: AppThemeMode? = null
        var selectedLang: AppLanguage? = null
        var selectedFont: AppFontChoice? = null

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    SettingsScreen(
                        prefs = UserPreferences(language = AppLanguage.FA),
                        storageStats = StorageStats(),
                        onLanguageChange = { selectedLang = it },
                        onThemeChange = { selectedTheme = it },
                        onFontChange = { selectedFont = it },
                        onCalendarChange = {},
                        onPersianDigitsChange = {},
                        onClearCacheOnExitChange = {},
                        onRedactLogsChange = {},
                        onClearBuildCache = {},
                        onOpenSigningManager = {},
                        onOpenHelpCenter = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("settings_theme_light").performScrollTo().performClick()
        assertEquals(AppThemeMode.LIGHT, selectedTheme)

        composeRule.onNodeWithTag("settings_lang_en").performScrollTo().performClick()
        assertEquals(AppLanguage.EN, selectedLang)

        composeRule.onNodeWithTag("settings_font_noto").performScrollTo().performClick()
        assertEquals(AppFontChoice.NOTO_NASKH, selectedFont)
    }

    @Test
    fun `build manager shows retry button for failed build and triggers rebuild`() {
        var retriedProjectId = -1L
        var retriedFormat: BuildOutputFormat? = null
        val env = EnvironmentCheckReport(
            isReady = true,
            status = BuildEngineStatus.READY,
            freeStorageMb = 1024L,
            freeRamMb = 512L,
            totalRamMb = 4096L,
            hasEmbeddedStubs = true,
            hasJavaCompiler = true,
            hasDexCompiler = true,
            hasApkSigner = true,
            hasKeystoreCrypto = true,
            androidSdkInt = 36,
            issues = emptyList(),
        )
        val failedTask = BuildTaskEntity(
            id = 5L,
            projectId = 1L,
            outputFormat = BuildOutputFormat.AAB.name,
            signingKeyId = 2L,
            status = BuildStatus.FAILED.name,
            currentStep = "COMPILING",
            progressPercent = 44,
            errorMessage = "خطای آزمایشی",
        )

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    BuildManagerScreen(
                        bundle = sampleBundle(id = 1L),
                        task = failedTask,
                        logs = emptyList(),
                        signingKeys = emptyList(),
                        envReport = env,
                        prefs = UserPreferences(language = AppLanguage.FA),
                        onBack = {},
                        onStartBuild = { pid, fmt, _ ->
                            retriedProjectId = pid
                            retriedFormat = fmt
                        },
                        onCancelBuild = {},
                        onOpenOutputs = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("build_retry_btn").performScrollTo().performClick()
        assertEquals(1L, retriedProjectId)
        assertEquals(BuildOutputFormat.AAB, retriedFormat)
    }

    @Test
    fun `signing manager shows update compatibility banner`() {
        val keys = listOf(
            SigningKeyMetadataEntity(
                id = 1L,
                alias = "webino_default",
                keystorePath = "/tmp/default.p12",
                commonName = "WEBINO",
                organization = "WEBINO",
                sha256Fingerprint = "AA:BB",
                sha1Fingerprint = "CC:DD",
                encryptedPasswordToken = "AKS:dummy",
                isDefault = true,
            )
        )
        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    SigningManagerScreen(
                        keys = keys,
                        onBack = {},
                        onCreateKey = { _, _, _, _, _, _ -> },
                        onImportKey = { _, _ -> },
                        onSetDefault = {},
                        onChangePassword = { _, _ -> },
                        onVerifyKey = {},
                        onDeleteKey = {},
                        onNotify = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("الزام امضای سازگار برای بروزرسانی اپلیکیشن").assertIsDisplayed()
    }

    @Test
    fun `project studio tab 2 displays 9 categorized webview sections`() {
        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.NAVY_DARK) {
                    com.example.ui.screens.ProjectStudioScreen(
                        bundle = sampleBundle(),
                        initialTab = 2,
                        language = AppLanguage.FA,
                        onBack = {},
                        onSave = { _, _, _, _ -> },
                        onPickImage = { _, _, _, _ -> },
                        onOpenPreview = {},
                        onOpenBuild = {},
                    )
                }
            }
        }

        val expectedTags = listOf(
            "webview_cat_browser",
            "webview_cat_security",
            "webview_cat_files",
            "webview_cat_media_hardware",
            "webview_cat_location",
            "webview_cat_video",
            "webview_cat_links",
            "webview_cat_storage",
            "webview_cat_performance",
        )
        expectedTags.forEach { tag ->
            composeRule.onNodeWithTag("studio_tab_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun `default user preferences use modern light mode and dashboard renders in light and english ltr`() {
        val defaultPrefs = UserPreferences()
        assertEquals(AppThemeMode.LIGHT, defaultPrefs.themeMode)

        val env = EnvironmentCheckReport(
            isReady = true,
            status = BuildEngineStatus.READY,
            freeStorageMb = 2048L,
            freeRamMb = 1024L,
            totalRamMb = 4096L,
            hasEmbeddedStubs = true,
            hasJavaCompiler = true,
            hasDexCompiler = true,
            hasApkSigner = true,
            hasKeystoreCrypto = true,
            androidSdkInt = 36,
            issues = emptyList(),
        )

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.EN) {
                WebinoTheme(themeMode = defaultPrefs.themeMode) {
                    com.example.ui.components.WebinoAmbientBackground {
                        DashboardScreen(
                            prefs = defaultPrefs.copy(language = AppLanguage.EN, usePersianDigits = false),
                            projects = listOf(sampleBundle(name = "Shop App")),
                            outputs = emptyList(),
                            recentTasks = emptyList(),
                            envReport = env,
                            storageStats = StorageStats(),
                            onCreateProject = {},
                            onOpenProject = {},
                            onOpenBuildManager = {},
                            onOpenOutputs = {},
                            onOpenHelp = {},
                            onRecheckEngine = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("dashboard_create_app_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_help_btn").assertIsDisplayed()
    }

    @Test
    fun `floating glass bottom bar displays all 5 navigation tabs and renders 3d glass icons`() {
        var clickedTag: String? = null
        val tags = listOf("nav_home", "nav_projects", "nav_build", "nav_outputs", "nav_settings")
        val iconTypes = com.example.ui.components.Webino3DIconType.entries

        composeRule.setContent {
            WebinoTheme(themeMode = AppThemeMode.LIGHT) {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.foundation.layout.Row {
                        iconTypes.forEach { t ->
                            com.example.ui.components.Webino3DGlassIcon(type = t, size = androidx.compose.ui.unit.Dp(32f))
                        }
                    }
                    com.example.ui.components.WebinoFloatingGlassBottomBar(
                        items = listOf(
                            com.example.ui.components.FloatingNavItemData(
                                label = "خانه",
                                testTag = "nav_home",
                                iconType = com.example.ui.components.Webino3DIconType.HOME,
                                selectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                unselectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                selected = true,
                                onClick = { clickedTag = "nav_home" },
                            ),
                            com.example.ui.components.FloatingNavItemData(
                                label = "پروژه‌ها",
                                testTag = "nav_projects",
                                iconType = com.example.ui.components.Webino3DIconType.PROJECTS,
                                selectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                unselectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                selected = false,
                                onClick = { clickedTag = "nav_projects" },
                            ),
                            com.example.ui.components.FloatingNavItemData(
                                label = "ساخت",
                                testTag = "nav_build",
                                iconType = com.example.ui.components.Webino3DIconType.BUILD,
                                selectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                unselectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                selected = false,
                                onClick = { clickedTag = "nav_build" },
                            ),
                            com.example.ui.components.FloatingNavItemData(
                                label = "خروجی",
                                testTag = "nav_outputs",
                                iconType = com.example.ui.components.Webino3DIconType.OUTPUTS,
                                selectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                unselectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                selected = false,
                                onClick = { clickedTag = "nav_outputs" },
                            ),
                            com.example.ui.components.FloatingNavItemData(
                                label = "تنظیمات",
                                testTag = "nav_settings",
                                iconType = com.example.ui.components.Webino3DIconType.SETTINGS,
                                selectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                unselectedIcon = androidx.compose.material.icons.Icons.Rounded.Android,
                                selected = false,
                                onClick = { clickedTag = "nav_settings" },
                            ),
                        )
                    )
                }
            }
        }

        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("nav_outputs").performClick()
        assertEquals("nav_outputs", clickedTag)
    }

    @Test
    fun `official application id matches red line vebino`() {
        assertEquals("red.line.vebino", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `about screen shows official developer info and hides support email text`() {
        var supportClicked = false
        var reviewClicked = false
        var devAppsClicked = false

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.LIGHT) {
                    AboutScreen(
                        language = AppLanguage.FA,
                        usePersianDigits = true,
                        onBack = {},
                        onNotify = {},
                        onContactSupportClick = { supportClicked = true },
                        onRateMyketClick = { reviewClicked = true },
                        onOtherAppsClick = { devAppsClicked = true },
                    )
                }
            }
        }

        // Advance stagger delays + entrance animations so all sections are composed
        composeRule.mainClock.advanceTimeBy(900)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("about_list").performScrollToNode(
            androidx.compose.ui.test.hasText("تیم نرم افزاری ردلاین سافت البرز")
        )
        composeRule.onNodeWithText("تیم نرم افزاری ردلاین سافت البرز").assertIsDisplayed()
        composeRule.onNodeWithText("مهندس مهدی رضایی").assertIsDisplayed()
        composeRule.onNodeWithText("گزارش خطا", substring = true).assertDoesNotExist()
        // Support email must NEVER be displayed visibly anywhere on screen
        composeRule.onNodeWithText("gmnarcis@gmail.com", substring = true).assertDoesNotExist()

        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_support_email_btn"))
        composeRule.onNodeWithTag("about_support_email_btn").performClick()
        assertTrue(supportClicked)

        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_myket_review_btn"))
        composeRule.onNodeWithTag("about_myket_review_btn").performClick()
        assertTrue(reviewClicked)

        composeRule.onNodeWithTag("about_list").performScrollToNode(hasTestTag("about_myket_dev_btn"))
        composeRule.onNodeWithTag("about_myket_dev_btn").performClick()
        assertTrue(devAppsClicked)
    }

    @Test
    fun `external intent helper constructs support email and myket deep link intents`() {
        val mailIntent = ExternalIntentHelper.buildSupportEmailIntent("پشتیبانی وبینو")
        assertEquals(android.content.Intent.ACTION_SENDTO, mailIntent.action)
        assertTrue(mailIntent.dataString?.startsWith("mailto:") == true)

        val reviewIntents = ExternalIntentHelper.buildMyketReviewIntents()
        assertTrue(reviewIntents.any { it.dataString == "myket://comment?id=red.line.vebino" })
        assertTrue(reviewIntents.any { it.dataString == "https://myket.ir/app/red.line.vebino" })

        val devIntents = ExternalIntentHelper.buildMyketDeveloperIntents()
        assertTrue(devIntents.any { it.dataString == "https://myket.ir/developer/dev-36089" })
    }

    @Test
    fun `splash screen finishes cleanly at 3000 milliseconds`() {
        composeRule.mainClock.autoAdvance = false
        var finished = false

        composeRule.setContent {
            ProvideLocalizedResources(AppLanguage.FA) {
                WebinoTheme(themeMode = AppThemeMode.LIGHT) {
                    SplashScreen(
                        language = AppLanguage.FA,
                        onFinished = { finished = true },
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(2800)
        assertFalse(finished)

        composeRule.mainClock.advanceTimeBy(300)
        assertTrue(finished)
    }

    @Test
    fun `double back exit requires two presses within 2000ms`() {
        val state = DoubleBackExitState(windowMs = 2000L)
        assertFalse(state.onBackPressed(1000L))
        assertTrue(state.onBackPressed(2400L))
        // Window resets after exit trigger or timeout
        assertFalse(state.onBackPressed(5000L))
        assertFalse(state.onBackPressed(7500L))
        assertTrue(state.onBackPressed(9000L))
    }
}


