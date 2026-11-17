package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.JalaliCalendar
import com.example.core.ProjectConfigSerializer
import com.example.core.ValidationUtils
import com.example.data.crypto.SigningManager
import com.example.data.local.db.WebinoDatabase
import com.example.data.local.prefs.UserPreferencesDataStore
import com.example.data.network.WebsiteInspector
import com.example.data.repository.WebinoRepository
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BuildOutputFormat
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebinoCoreAndRepositoryTest {

    private lateinit var db: WebinoDatabase
    private lateinit var prefsStore: UserPreferencesDataStore
    private lateinit var repo: WebinoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (!androidx.work.WorkManager.isInitialized()) {
            androidx.work.WorkManager.initialize(
                context,
                androidx.work.Configuration.Builder().build(),
            )
        }
        db = Room.inMemoryDatabaseBuilder(context, WebinoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsStore = UserPreferencesDataStore(context)
        repo = WebinoRepository(
            context = context,
            db = db,
            prefsStore = prefsStore,
            signingManager = SigningManager(context),
            websiteInspector = WebsiteInspector(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `jalali calendar and persian digits format accurately`() {
        // 2026-03-21 is 1405-01-01 (Nowruz 1405)
        val jalali = JalaliCalendar.gregorianToJalali(2026, 3, 21, 14, 30)
        assertEquals(1405, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)

        val localized = JalaliCalendar.toPersianDigits("1405/01/01 - 98%")
        assertEquals("۱۴۰۵/۰۱/۰۱ - ۹۸%", localized)
    }

    @Test
    fun `validation utils supports ir domains https detection and package suggestion`() {
        assertTrue(ValidationUtils.isValidWebUrl("digikala.ir"))
        assertTrue(ValidationUtils.isHttpsUrl("digikala.ir"))
        assertTrue(ValidationUtils.isIranDomain("https://myshop.ir/products"))
        assertFalse(ValidationUtils.isIranDomain("https://example.com"))
        assertEquals("ir.myshop", ValidationUtils.suggestPackageName("https://www.myshop.ir", "فروشگاه"))
        assertTrue(ValidationUtils.isValidPackageName("ir.myshop.app"))
        assertFalse(ValidationUtils.isValidPackageName("123.invalid"))
        assertFalse(ValidationUtils.isValidWebUrl("javascript:alert(1)"))
    }

    @Test
    fun `repository initializes defaults creates project duplicates and runs full build task`() = runTest {
        repo.initializeDefaults()
        val keys = repo.signingKeys.first()
        assertTrue("Default signing key must be created", keys.isNotEmpty())
        val templates = repo.templates.first()
        assertEquals(8, templates.size)

        val projectId = repo.createProject(
            name = "فروشگاه تست",
            websiteUrl = "https://shop.ir",
            packageName = "ir.shop.testapp",
            versionName = "1.0.0",
            versionCode = 1,
            description = "تست کامل",
            template = AppTemplateType.STORE,
            faviconUrl = null,
            themeColorOverride = null,
        )
        assertTrue(projectId > 0L)

        val duplicatedId = repo.duplicateProject(projectId)
        assertTrue(duplicatedId != null && duplicatedId > projectId)
        assertEquals(2, repo.allProjects.first().size)

        val taskId = repo.enqueueBuild(
            projectId = projectId,
            outputFormat = BuildOutputFormat.APK,
            signingKeyId = keys.first().id,
        )
        val ok = repo.runBuildTask(taskId)
        assertTrue("Repository build task should succeed", ok)

        val results = repo.allBuildResults.first()
        assertEquals(1, results.size)
        assertTrue(results.first().isSigned)
        assertTrue(results.first().isVerified)
        assertEquals("ir.shop.testapp", results.first().packageName)
    }

    @Test
    fun `repository supports update search duplicate package check and delete`() = runTest {
        repo.initializeDefaults()
        val p1 = repo.createProject(
            name = "فروشگاه یک",
            websiteUrl = "https://one.ir",
            packageName = "ir.shop.one",
            versionName = "1.0.0",
            versionCode = 1,
            description = "توضیح اول",
            template = AppTemplateType.STORE,
            faviconUrl = null,
            themeColorOverride = null,
        )
        repo.createProject(
            name = "اخبار فناوری",
            websiteUrl = "https://technews.ir",
            packageName = "ir.news.tech",
            versionName = "1.0.0",
            versionCode = 1,
            description = "خبری",
            template = AppTemplateType.NEWS,
            faviconUrl = null,
            themeColorOverride = null,
        )

        assertTrue(repo.isPackageNameTaken("ir.shop.one"))
        assertFalse(repo.isPackageNameTaken("ir.shop.one", excludeProjectId = p1))
        assertEquals(1, repo.searchProjects("فروشگاه").first().size)
        assertEquals(1, repo.searchProjects("technews").first().size)

        val bundle = repo.observeProject(p1).first()!!
        repo.updateProjectBundle(
            project = bundle.project.copy(name = "فروشگاه ویرایش‌شده", versionName = "1.1.0"),
            settings = bundle.settings!!.copy(appName = "فروشگاه ویرایش‌شده"),
            webSettings = bundle.webSettings!!.copy(httpsOnly = true),
            changeNote = "ویرایش مشخصات",
        )
        val updated = repo.observeProject(p1).first()!!
        assertEquals("فروشگاه ویرایش‌شده", updated.project.name)
        assertEquals("1.1.0", updated.project.versionName)

        repo.deleteProject(p1)
        assertNull(repo.observeProject(p1).first())
        assertFalse(repo.isPackageNameTaken("ir.shop.one"))
    }

    @Test
    fun `project config serializer roundtrip and security rejection`() = runTest {
        repo.initializeDefaults()
        val id = repo.createProject(
            name = "کلینیک آنلاین",
            websiteUrl = "https://clinic.ir",
            packageName = "ir.clinic.app",
            versionName = "2.0.0",
            versionCode = 4,
            description = "نوبت‌دهی",
            template = AppTemplateType.SERVICE,
            faviconUrl = null,
            themeColorOverride = null,
        )

        val exportedJson = repo.exportProjectJson(id)
        assertNotNull(exportedJson)

        val importedId = repo.importProjectJson(exportedJson!!).getOrThrow()
        val importedBundle = repo.observeProject(importedId).first()!!
        assertEquals("کلینیک آنلاین", importedBundle.project.name)
        assertEquals("ir.clinic.app", importedBundle.project.packageName)
        assertEquals(4, importedBundle.project.versionCode)

        // Reject unsafe javascript scheme
        val maliciousJson = exportedJson.replace("https://clinic.ir", "javascript:alert(1)")
        assertTrue(ProjectConfigSerializer.parseAndValidate(maliciousJson).isFailure)

        // Reject invalid JSON & oversized payload
        assertTrue(ProjectConfigSerializer.parseAndValidate("{not valid json").isFailure)
        val oversized = "a".repeat(ProjectConfigSerializer.MAX_IMPORT_BYTES + 10)
        assertTrue(ProjectConfigSerializer.parseAndValidate(oversized).isFailure)
    }

    @Test
    fun `user preferences persist theme language and font changes`() = runTest {
        repo.setThemeMode(AppThemeMode.LIGHT)
        repo.setLanguage(AppLanguage.EN)
        repo.setFontChoice(AppFontChoice.NOTO_NASKH)

        val current = repo.preferences.first()
        assertEquals(AppThemeMode.LIGHT, current.themeMode)
        assertEquals(AppLanguage.EN, current.language)
        assertEquals(AppFontChoice.NOTO_NASKH, current.fontChoice)
    }
}
