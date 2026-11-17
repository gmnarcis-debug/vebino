package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WebinoApplication
import com.example.core.ValidationUtils
import com.example.data.local.db.AppTemplateEntity
import com.example.data.local.db.BuildLogEntity
import com.example.data.local.db.BuildResultEntity
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.ProjectEntity
import com.example.data.local.db.ProjectSettingsEntity
import com.example.data.local.db.RecentWebsiteEntity
import com.example.data.local.db.SigningKeyMetadataEntity
import com.example.data.local.db.WebSettingsEntity
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.CalendarType
import com.example.domain.model.EnvironmentCheckReport
import com.example.domain.model.SiteMetadata
import com.example.domain.usecase.CancelBuildUseCase
import com.example.domain.usecase.InspectBuildEnvironmentUseCase
import com.example.domain.usecase.RetryBuildUseCase
import com.example.domain.usecase.StartBuildUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreateProjectFormState(
    val urlInput: String = "",
    val appName: String = "",
    val packageName: String = "",
    val versionName: String = "1.0.0",
    val versionCode: String = "1",
    val description: String = "",
    val selectedTemplate: AppTemplateType = AppTemplateType.STORE,
    val allowInsecureHttp: Boolean = false,
    val isInspecting: Boolean = false,
    val inspectedSite: SiteMetadata? = null,
    val errorMessage: String? = null,
)

data class StorageStats(
    val outputsBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val mediaBytes: Long = 0L,
)

class WebinoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as WebinoApplication).repository
    private val inspectBuildEnvUseCase = InspectBuildEnvironmentUseCase(repo)
    private val startBuildUseCase = StartBuildUseCase(repo)
    private val cancelBuildUseCase = CancelBuildUseCase(repo)
    private val retryBuildUseCase = RetryBuildUseCase(repo)

    val preferences: StateFlow<UserPreferences> = repo.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences(),
    )

    val projects: StateFlow<List<FullProjectBundle>> = repo.allProjects.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val buildResults: StateFlow<List<BuildResultEntity>> = repo.allBuildResults.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val latestBuildTask: StateFlow<BuildTaskEntity?> = repo.latestBuildTask.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null,
    )

    val recentBuildTasks: StateFlow<List<BuildTaskEntity>> = repo.recentBuildTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val allBuildTasks: StateFlow<List<BuildTaskEntity>> = repo.allBuildTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val signingKeys: StateFlow<List<SigningKeyMetadataEntity>> = repo.signingKeys.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val templates: StateFlow<List<AppTemplateEntity>> = repo.templates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val recentWebsites: StateFlow<List<RecentWebsiteEntity>> = repo.recentWebsites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    private val _environmentReport = MutableStateFlow(repo.inspectEnvironment())
    val environmentReport: StateFlow<EnvironmentCheckReport> = _environmentReport.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _createForm = MutableStateFlow(CreateProjectFormState())
    val createForm: StateFlow<CreateProjectFormState> = _createForm.asStateFlow()

    /** Picks the snackbar text matching the user's chosen app language. */
    private fun tr(fa: String, en: String): String =
        if (preferences.value.language == AppLanguage.FA) fa else en

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _hasCompletedSplash = MutableStateFlow(false)
    val hasCompletedSplash: StateFlow<Boolean> = _hasCompletedSplash.asStateFlow()

    init {
        refreshSystemStats()
    }

    fun refreshSystemStats() {
        _environmentReport.value = inspectBuildEnvUseCase()
        val (outB, cacheB, mediaB) = repo.calculateStorageBreakdown()
        _storageStats.value = StorageStats(outB, cacheB, mediaB)
    }

    fun recheckEnvironment() {
        _environmentReport.value = _environmentReport.value.copy(
            status = com.example.domain.model.BuildEngineStatus.CHECKING
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(180)
            refreshSystemStats()
            val status = _environmentReport.value.status
            _snackbarMessage.value = tr("وضعیت موتور Build: ${status.titleFa}", "Build engine: ${status.name}")
        }
    }

    suspend fun exportProjectJson(projectId: Long): String? = repo.exportProjectJson(projectId)

    fun importProjectJson(rawJson: String, onImported: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            repo.importProjectJson(rawJson)
                .onSuccess { newId ->
                    refreshSystemStats()
                    _snackbarMessage.value = tr("تنظیمات پروژه با موفقیت وارد شد.", "Project settings imported successfully.")
                    onImported?.invoke(newId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: tr("فایل JSON نامعتبر است.", "Invalid JSON file.")
                }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun markSplashCompleted() {
        _hasCompletedSplash.value = true
    }

    fun observeProject(projectId: Long): Flow<FullProjectBundle?> = repo.observeProject(projectId)
    fun observeBuildTask(taskId: Long): Flow<BuildTaskEntity?> = repo.observeBuildTask(taskId)
    fun observeBuildLogs(taskId: Long): Flow<List<BuildLogEntity>> = repo.observeBuildLogs(taskId)

    fun resetCreateForm(presetTemplateId: String? = null) {
        val tmpl = AppTemplateType.entries.firstOrNull { it.id == presetTemplateId } ?: AppTemplateType.STORE
        _createForm.value = CreateProjectFormState(selectedTemplate = tmpl)
    }

    fun updateCreateForm(transform: (CreateProjectFormState) -> CreateProjectFormState) {
        _createForm.value = transform(_createForm.value)
    }

    fun inspectSiteForCreateForm() {
        val raw = _createForm.value.urlInput.trim()
        if (!ValidationUtils.isValidWebUrl(raw)) {
            _createForm.value = _createForm.value.copy(errorMessage = "لطفاً آدرس وبسایت معتبر وارد کنید (مثلاً https://site.ir)")
            return
        }
        viewModelScope.launch {
            _createForm.value = _createForm.value.copy(isInspecting = true, errorMessage = null)
            val meta = repo.inspectWebsite(raw)
            val current = _createForm.value
            val suggestedName = current.appName.ifBlank { meta.title.take(28) }
            val suggestedPkg = current.packageName.ifBlank {
                ValidationUtils.suggestPackageName(meta.finalUrl, suggestedName)
            }
            val suggestedDesc = current.description.ifBlank { meta.description }
            _createForm.value = current.copy(
                urlInput = meta.finalUrl,
                appName = suggestedName,
                packageName = suggestedPkg,
                description = suggestedDesc,
                isInspecting = false,
                inspectedSite = meta,
                errorMessage = if (!meta.isReachable) meta.errorMessage else null,
            )
        }
    }

    fun createProjectFromForm(onCreated: (Long) -> Unit) {
        val f = _createForm.value
        val normalizedUrl = ValidationUtils.normalizeUrl(f.urlInput)
        if (!ValidationUtils.isValidWebUrl(normalizedUrl)) {
            _createForm.value = f.copy(errorMessage = "آدرس وب‌سایت معتبر نیست.")
            return
        }
        if (!ValidationUtils.isHttpsUrl(normalizedUrl) && !f.allowInsecureHttp) {
            _createForm.value = f.copy(
                errorMessage = "آدرس‌های غیرامن HTTP به‌صورت پیش‌فرض پذیرفته نمی‌شوند. از HTTPS استفاده کنید یا مجوز موقت HTTP را فعال نمایید."
            )
            return
        }
        if (f.appName.trim().isEmpty()) {
            _createForm.value = f.copy(errorMessage = "نام اپلیکیشن نمی‌تواند خالی باشد.")
            return
        }
        val pkg = f.packageName.trim().ifBlank { ValidationUtils.suggestPackageName(normalizedUrl, f.appName) }
        if (!ValidationUtils.isValidPackageName(pkg)) {
            _createForm.value = f.copy(errorMessage = "نام بسته (Package Name) نامعتبر است. مثال: ir.mysite.app")
            return
        }
        val verCode = f.versionCode.toIntOrNull()?.coerceAtLeast(1) ?: 1
        viewModelScope.launch {
            val id = repo.createProject(
                name = f.appName.trim(),
                websiteUrl = normalizedUrl,
                packageName = pkg,
                versionName = f.versionName.trim().ifBlank { "1.0.0" },
                versionCode = verCode,
                description = f.description.trim(),
                template = f.selectedTemplate,
                faviconUrl = f.inspectedSite?.faviconUrl,
                themeColorOverride = f.inspectedSite?.themeColorHex,
            )
            refreshSystemStats()
            onCreated(id)
        }
    }

    fun saveProjectChanges(
        project: ProjectEntity,
        settings: ProjectSettingsEntity,
        webSettings: WebSettingsEntity,
        changeNote: String,
    ) {
        viewModelScope.launch {
            repo.updateProjectBundle(project, settings, webSettings, changeNote)
            _snackbarMessage.value = tr("تغییرات پروژه ذخیره شد.", "Project changes saved.")
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            repo.duplicateProject(projectId)
            _snackbarMessage.value = tr("نسخه کپی پروژه ایجاد شد.", "Project copy created.")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repo.deleteProject(projectId)
            refreshSystemStats()
            _snackbarMessage.value = tr("پروژه حذف شد.", "Project deleted.")
        }
    }

    fun importProjectImage(projectId: Long, uri: Uri, isSplash: Boolean, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val savedPath = repo.saveCroppedProjectImage(projectId, uri, isSplash)
            if (savedPath != null) {
                onSaved(savedPath)
                _snackbarMessage.value = if (isSplash) tr("تصویر Splash با برش مربعی ذخیره شد.", "Splash image saved with square crop.") else tr("آیکون برنامه با برش مربعی ذخیره شد.", "App icon saved with square crop.")
            } else {
                _snackbarMessage.value = tr("خطا در پردازش تصویر انتخابشده.", "Could not process the selected image.")
            }
        }
    }

    fun startBuild(
        projectId: Long,
        outputFormat: BuildOutputFormat,
        signingKeyId: Long,
        onTaskCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            startBuildUseCase(projectId, outputFormat, signingKeyId)
                .onSuccess { taskId ->
                    onTaskCreated(taskId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: tr("خطا در اعتبارسنجی مشخصات پیش از ساخت.", "Pre-build validation failed.")
                }
        }
    }

    fun retryBuild(
        taskId: Long,
        onTaskCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            retryBuildUseCase(taskId)
                .onSuccess { newTaskId ->
                    onTaskCreated(newTaskId)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: tr("خطا در شروع مجدد ساخت.", "Could not restart the build.")
                }
        }
    }

    fun cancelBuild(taskId: Long) {
        viewModelScope.launch {
            cancelBuildUseCase(taskId)
            refreshSystemStats()
            _snackbarMessage.value = tr("فرایند ساخت لغو شد.", "Build cancelled.")
        }
    }

    fun verifyOutput(resultId: Long) {
        viewModelScope.launch {
            val summary = repo.verifyOutput(resultId)
            _snackbarMessage.value = summary
        }
    }

    fun deleteOutput(resultId: Long) {
        viewModelScope.launch {
            repo.deleteOutput(resultId)
            refreshSystemStats()
            _snackbarMessage.value = tr("فایل خروجی حذف شد.", "Output file deleted.")
        }
    }

    fun createSigningKey(
        alias: String,
        password: String,
        commonName: String,
        organization: String,
        country: String,
        validityYears: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                repo.createSigningKey(alias, password, commonName, organization, country, validityYears)
            }.onSuccess {
                _snackbarMessage.value = tr("کلید امضای دیجیتال جدید ایجاد و فعال شد.", "New signing key created and set as default.")
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: tr("خطا در ایجاد کلید امضا", "Could not create the signing key.")
            }
        }
    }

    fun importSigningKey(uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                repo.importSigningKey(uri, password)
            }.onSuccess {
                _snackbarMessage.value = tr("فایل Keystore با موفقیت وارد شد.", "Keystore imported successfully.")
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: tr("خطا در خواندن Keystore", "Could not read the keystore.")
            }
        }
    }

    fun setDefaultSigningKey(keyId: Long) {
        viewModelScope.launch {
            repo.setDefaultSigningKey(keyId)
            _snackbarMessage.value = tr("کلید پیشفرض امضا تغییر کرد.", "Default signing key changed.")
        }
    }

    fun changeSigningPassword(keyId: Long, newPassword: String) {
        viewModelScope.launch {
            runCatching {
                repo.changeSigningPassword(keyId, newPassword)
            }.onSuccess {
                _snackbarMessage.value = tr("رمز کلید امضا با موفقیت تغییر یافت.", "Signing key password changed.")
            }.onFailure {
                _snackbarMessage.value = tr("تغییر رمز ناموفق بود.", "Password change failed.")
            }
        }
    }

    fun verifySigningKey(keyId: Long) {
        viewModelScope.launch {
            val ok = repo.verifySigningKey(keyId)
            _snackbarMessage.value = if (ok) tr("گواهی امضا کاملاً معتبر و سالم است.", "The signing certificate is valid and intact.") else tr("اعتبارسنجی گواهی ناموفق بود.", "Certificate validation failed.")
        }
    }

    fun deleteSigningKey(keyId: Long) {
        viewModelScope.launch {
            repo.deleteSigningKey(keyId)
            _snackbarMessage.value = tr("کلید امضا حذف شد.", "Signing key deleted.")
        }
    }

    fun clearBuildCache() {
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) { repo.clearTempBuildCache() }
            refreshSystemStats()
            val lang = preferences.value.language
            val persianDigits = preferences.value.usePersianDigits
            val formatted = ValidationUtils.formatFileSize(freed, lang == AppLanguage.FA, persianDigits)
            _snackbarMessage.value = tr("حافظه موقت ساخت پاکسازی شد ($formatted).", "Build cache cleared ($formatted).")
        }
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { repo.setLanguage(lang) }
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setFontChoice(font: AppFontChoice) = viewModelScope.launch { repo.setFontChoice(font) }
    fun setCalendarType(cal: CalendarType) = viewModelScope.launch { repo.setCalendarType(cal) }
    fun setPersianDigits(enabled: Boolean) = viewModelScope.launch { repo.setPersianDigits(enabled) }
    fun setClearCacheOnExit(enabled: Boolean) = viewModelScope.launch { repo.setClearCacheOnExit(enabled) }
    fun setRedactLogs(enabled: Boolean) = viewModelScope.launch { repo.setRedactLogs(enabled) }
    fun setOnboardingCompleted(done: Boolean) = viewModelScope.launch { repo.setOnboardingCompleted(done) }
    suspend fun buildSupportReport(taskId: Long, isFa: Boolean): String = repo.buildSupportReport(taskId, isFa)
    fun observeBuildHistoryLogs(taskId: Long): Flow<List<BuildLogEntity>> = repo.observeBuildLogs(taskId)
}
