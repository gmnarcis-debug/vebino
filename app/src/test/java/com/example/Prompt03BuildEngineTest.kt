package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.buildengine.BuildConfiguration
import com.example.buildengine.BuildReportGenerator
import com.example.buildengine.BuildSpec
import com.example.buildengine.BuildStorageManager
import com.example.buildengine.OutputValidator
import com.example.buildengine.ProjectGenerator
import com.example.data.crypto.SigningManager
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.WebinoDatabase
import com.example.data.local.prefs.UserPreferencesDataStore
import com.example.data.network.WebsiteInspector
import com.example.data.repository.WebinoRepository
import com.example.domain.model.AppTemplateType
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStatus
import com.example.domain.usecase.CancelBuildUseCase
import com.example.domain.usecase.RecoverInterruptedBuildsUseCase
import com.example.domain.usecase.RetryBuildUseCase
import com.example.domain.usecase.StartBuildUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Prompt03BuildEngineTest {

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

    private fun createCustomPngBytes(): ByteArray {
        val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#7C3AED"))
        return ByteArrayOutputStream().apply {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
    }

    @Test
    fun `operational test builds WEBINO Test App APK and AAB with 10-point validation`() = runTest {
        repo.initializeDefaults()
        val key = repo.signingKeys.first().first()

        val projectId = repo.createProject(
            name = "WEBINO Test App",
            websiteUrl = "https://example.com",
            packageName = "ir.webino.testapp",
            versionName = "1.0",
            versionCode = 1,
            description = "Operational Prompt 03 verification project",
            template = AppTemplateType.STORE,
            faviconUrl = null,
            themeColorOverride = "#7C3AED",
        )

        val iconFile = File(context.filesDir, "test_custom_icon.png").apply {
            writeBytes(createCustomPngBytes())
        }
        val bundle = repo.observeProject(projectId).first()!!
        repo.updateProjectBundle(
            project = bundle.project,
            settings = bundle.settings!!.copy(
                appName = "WEBINO Test App",
                iconFilePath = iconFile.absolutePath,
            ),
            webSettings = bundle.webSettings!!.copy(httpsOnly = true),
            changeNote = "Set custom icon",
        )

        val startUseCase = StartBuildUseCase(repo)
        val taskId = startUseCase(projectId, BuildOutputFormat.BOTH, key.id).getOrThrow()
        assertTrue(taskId > 0L)

        val ok = repo.runBuildTask(taskId)
        assertTrue("Operational build task must succeed", ok)

        val task = repo.observeBuildTask(taskId).first()!!
        assertEquals(BuildStatus.SUCCEEDED.name, task.status)
        assertEquals(100, task.progressPercent)

        val results = repo.allBuildResults.first().filter { it.taskId == taskId }
        assertEquals(2, results.size)

        val apkEntity = results.first { it.format == BuildOutputFormat.APK.name }
        val aabEntity = results.first { it.format == BuildOutputFormat.AAB.name }
        val apkFile = File(apkEntity.filePath)
        val aabFile = File(aabEntity.filePath)

        val expectedSpec = repo.validateBuildSpec(projectId, BuildOutputFormat.BOTH, key.id).getOrThrow().first

        // 10-point APK validation against expected BuildSpec
        val apkReport = OutputValidator.validateApk(apkFile, expectedSpec = expectedSpec)
        assertTrue("APK validation failed: ${apkReport.errors}", apkReport.isValid)
        assertTrue(apkReport.isSigned)
        assertTrue(apkReport.v1Verified)
        assertTrue(apkReport.v2Verified)
        assertTrue(apkReport.v3Verified)
        assertEquals("ir.webino.testapp", apkReport.parsedManifest?.packageName)
        assertEquals(1, apkReport.parsedManifest?.versionCode)
        assertEquals("1.0", apkReport.parsedManifest?.versionName)

        // PackageManager archive inspection
        @Suppress("DEPRECATION")
        val pkgInfo = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS,
        )
        assertNotNull(pkgInfo)
        assertEquals("ir.webino.testapp", pkgInfo!!.packageName)
        assertEquals("1.0", pkgInfo.versionName)
        assertEquals("ir.webino.testapp.MainActivity", pkgInfo.activities?.firstOrNull()?.name)

        // Verify AAB CMS cryptographic signature & spec match
        val aabReport = OutputValidator.validateAab(aabFile, expectedSpec = expectedSpec)
        assertTrue("AAB validation failed: ${aabReport.errors}", aabReport.isValid)
        assertTrue(aabReport.isSigned)
        assertTrue(aabReport.certSha256.isNotBlank())

        // Verify report JSON was persisted & temp build folder was cleaned
        val reportFile = File(BuildStorageManager.getReportsDir(context), "build_report_$taskId.json")
        assertTrue("Build report JSON should exist", reportFile.exists())
        val tempLeftovers = BuildStorageManager.getBuildsCacheRoot(context).listFiles().orEmpty()
        assertTrue("Temp workspace should be cleaned up", tempLeftovers.isEmpty())

        // Verify generated target Java source contains expected URL & Back-navigation handler
        val srcVerifyDir = File(context.cacheDir, "verify_src_tmp").apply { deleteRecursively(); mkdirs() }
        val srcFiles = ProjectGenerator.generateSources(expectedSpec, srcVerifyDir)
        val javaCode = srcFiles.first().readText()
        assertTrue(javaCode.contains("https://example.com"))
        assertTrue(javaCode.contains("KeyEvent.KEYCODE_BACK"))
        assertTrue(javaCode.contains("webView.goBack()"))
        srcVerifyDir.deleteRecursively()
    }

    @Test
    fun `pre-build validation rejects invalid BuildSpec with Persian messages`() = runTest {
        repo.initializeDefaults()
        val key = repo.signingKeys.first().first()
        val signingMaterial = SigningManager(context).loadSigningMaterial(
            key.keystorePath, key.alias, key.encryptedPasswordToken
        )

        val invalidSpec = BuildSpec(
            projectId = 1L,
            appName = "",
            packageName = "bad_pkg",
            versionName = "",
            versionCode = 0,
            minSdk = 30,
            targetSdk = 28,
            websiteUrl = "http://insecure.ir",
            httpsOnly = true,
            primaryColorHex = "not-a-color",
            iconPngBytes = byteArrayOf(1, 2, 3),
            keyAlias = signingMaterial.alias,
            privateKey = signingMaterial.privateKey,
            certificateChain = signingMaterial.certificateChain,
        )

        val validation = BuildConfiguration.validate(invalidSpec)
        assertFalse(validation.isValid)
        assertTrue(validation.errors.size >= 6)

        // Also test via repository StartBuildUseCase on invalid persisted project
        val badId = repo.createProject(
            name = "تست نامعتبر",
            websiteUrl = "http://plain-http.ir",
            packageName = "invalid",
            versionName = "1.0",
            versionCode = 1,
            description = "",
            template = AppTemplateType.STORE,
            faviconUrl = null,
            themeColorOverride = null,
        )
        val b = repo.observeProject(badId).first()!!
        repo.updateProjectBundle(
            project = b.project,
            settings = b.settings!!,
            webSettings = b.webSettings!!.copy(httpsOnly = true),
            changeNote = "force httpsOnly",
        )
        val startResult = StartBuildUseCase(repo)(badId, BuildOutputFormat.APK, key.id)
        assertTrue("StartBuildUseCase must fail on invalid project config", startResult.isFailure)
    }

    @Test
    fun `cancel retry and interrupted build recovery work cleanly`() = runTest {
        repo.initializeDefaults()
        val key = repo.signingKeys.first().first()
        val projectId = repo.createProject(
            name = "بازیابی تست",
            websiteUrl = "https://example.com",
            packageName = "ir.webino.recoverytest",
            versionName = "1.0",
            versionCode = 1,
            description = "",
            template = AppTemplateType.PERSONAL,
            faviconUrl = null,
            themeColorOverride = null,
        )

        // 1. Cancel test
        val cancelledTaskId = StartBuildUseCase(repo)(projectId, BuildOutputFormat.APK, key.id).getOrThrow()
        CancelBuildUseCase(repo)(cancelledTaskId)
        val cancelledTask = repo.observeBuildTask(cancelledTaskId).first()!!
        assertEquals(BuildStatus.CANCELLED.name, cancelledTask.status)
        assertFalse("Cancelled task must not build", repo.runBuildTask(cancelledTaskId))

        // 2. Retry test
        val retriedTaskId = RetryBuildUseCase(repo)(cancelledTaskId).getOrThrow()
        assertTrue(retriedTaskId > cancelledTaskId)
        assertTrue("Retried build should succeed", repo.runBuildTask(retriedTaskId))

        // 3. Interrupted build recovery (simulate orphaned RUNNING task after process death)
        val stuckTaskId = db.buildDao().insertBuildTask(
            BuildTaskEntity(
                projectId = projectId,
                outputFormat = BuildOutputFormat.APK.name,
                signingKeyId = key.id,
                status = BuildStatus.RUNNING.name,
                currentStep = "COMPILING",
                progressPercent = 44,
                workRequestId = null,
            )
        )
        val recoveredCount = RecoverInterruptedBuildsUseCase(repo)()
        assertTrue(recoveredCount >= 1)
        val recoveredTask = db.buildDao().getBuildTask(stuckTaskId)!!
        assertEquals(BuildStatus.FAILED.name, recoveredTask.status)
        assertNotNull(recoveredTask.errorMessage)
    }

    @Test
    fun `build report generator redacts passwords and vault tokens`() {
        val raw = "Keystore password=MySecret123 token=AKS:AbCdEfGhIjKlMnOpQrStUvWxYz=="
        val redacted = BuildReportGenerator.redactSensitive(raw, enabled = true)
        assertFalse(redacted.contains("MySecret123"))
        assertFalse(redacted.contains("AbCdEfGhIjKlMnOpQrStUvWxYz"))
        assertTrue(redacted.contains("***REDACTED***"))
    }
}
