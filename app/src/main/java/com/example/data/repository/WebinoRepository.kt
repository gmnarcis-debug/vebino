package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.buildengine.BuildConfiguration
import com.example.buildengine.BuildEngine
import com.example.buildengine.BuildReportGenerator
import com.example.buildengine.BuildSpec
import com.example.buildengine.BuildStorageManager
import com.example.buildengine.EnvironmentChecker
import com.example.buildengine.OnDeviceBuildOrchestrator
import com.example.buildengine.OutputValidator
import com.example.data.crypto.SigningManager
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
import com.example.data.local.db.WebinoDatabase
import com.example.data.local.prefs.UserPreferences
import com.example.data.local.prefs.UserPreferencesDataStore
import com.example.data.network.WebsiteInspector
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStatus
import com.example.domain.model.BuildStep
import com.example.domain.model.CacheModeOption
import com.example.domain.model.CalendarType
import com.example.domain.model.EnvironmentCheckReport
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.LoadingAnimationType
import com.example.domain.model.MixedContentOption
import com.example.domain.model.OrientationMode
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SiteMetadata
import com.example.domain.model.SslPolicy
import com.example.domain.model.ThemePreset
import com.example.domain.model.ToolbarStyle
import com.example.work.BuildWorker
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ProjectHistoryEntry(
    val timestamp: Long,
    val summary: String,
)

class WebinoRepository(
    private val context: Context,
    private val db: WebinoDatabase,
    private val prefsStore: UserPreferencesDataStore,
    private val signingManager: SigningManager,
    private val websiteInspector: WebsiteInspector,
) {
    private val projectDao = db.projectDao()
    private val buildDao = db.buildDao()
    private val signingDao = db.signingDao()
    private val templateDao = db.templateAndHistoryDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val buildEngine: BuildEngine = OnDeviceBuildOrchestrator(context)
    private val cancelledTaskIds = ConcurrentHashMap.newKeySet<Long>()

    val preferences: Flow<UserPreferences> = prefsStore.preferences
    val allProjects: Flow<List<FullProjectBundle>> = projectDao.observeAllProjects()
    val projectCount: Flow<Int> = projectDao.observeProjectCount()
    val allBuildResults: Flow<List<BuildResultEntity>> = buildDao.observeAllBuildResults()
    val latestBuildTask: Flow<BuildTaskEntity?> = buildDao.observeLatestBuildTask()
    val recentBuildTasks: Flow<List<BuildTaskEntity>> = buildDao.observeRecentBuildTasks()
    val allBuildTasks: Flow<List<BuildTaskEntity>> = buildDao.observeAllBuildTasks()
    val signingKeys: Flow<List<SigningKeyMetadataEntity>> = signingDao.observeSigningKeys()
    val templates: Flow<List<AppTemplateEntity>> = templateDao.observeTemplates()
    val recentWebsites: Flow<List<RecentWebsiteEntity>> = templateDao.observeRecentWebsites()

    fun observeProject(projectId: Long): Flow<FullProjectBundle?> = projectDao.observeProject(projectId)
    fun searchProjects(query: String): Flow<List<FullProjectBundle>> = projectDao.searchProjects(query)
    suspend fun isPackageNameTaken(packageName: String, excludeProjectId: Long = 0L): Boolean =
        projectDao.countByPackageName(packageName.trim(), excludeProjectId) > 0
    fun observeBuildTask(taskId: Long): Flow<BuildTaskEntity?> = buildDao.observeBuildTask(taskId)
    fun observeBuildLogs(taskId: Long): Flow<List<BuildLogEntity>> = buildDao.observeBuildLogs(taskId)

    suspend fun initializeDefaults() = withContext(Dispatchers.IO) {
        templateDao.insertTemplates(WebinoDatabase.defaultTemplates())
        if (signingDao.getAllSigningKeys().isEmpty()) {
            val defaultPassword = "webino_${UUID.randomUUID().toString().take(8)}"
            val created = signingManager.createNewKeystore(
                alias = "webino_default",
                password = defaultPassword,
                commonName = "WEBINO Publisher",
                organization = "WEBINO",
                country = "IR",
                validityYears = 25,
            )
            signingDao.insertSigningKey(
                SigningKeyMetadataEntity(
                    alias = created.alias,
                    keystorePath = created.keystorePath,
                    keystoreType = created.keystoreType,
                    commonName = created.commonName,
                    organization = created.organization,
                    country = created.country,
                    validityYears = created.validityYears,
                    sha256Fingerprint = created.sha256Fingerprint,
                    sha1Fingerprint = created.sha1Fingerprint,
                    encryptedPasswordToken = created.encryptedPasswordToken,
                    isDefault = true,
                )
            )
        }
    }

    fun inspectEnvironment(): EnvironmentCheckReport = EnvironmentChecker.inspect(context)

    suspend fun inspectWebsite(rawUrl: String): SiteMetadata {
        val meta = websiteInspector.inspect(rawUrl)
        if (meta.finalUrl.isNotBlank()) {
            templateDao.upsertRecentWebsite(
                RecentWebsiteEntity(
                    url = meta.finalUrl,
                    title = meta.title,
                    description = meta.description,
                    faviconUrl = meta.faviconUrl,
                    themeColorHex = meta.themeColorHex,
                    isHttps = meta.isHttps,
                )
            )
        }
        return meta
    }

    suspend fun createProject(
        name: String,
        websiteUrl: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        description: String,
        template: AppTemplateType,
        faviconUrl: String?,
        themeColorOverride: String?,
    ): Long {
        val now = System.currentTimeMillis()
        val history = json.encodeToString(
            listOf(ProjectHistoryEntry(now, "ایجاد پروژه با قالب ${template.titleFa}"))
        )
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                websiteUrl = websiteUrl.trim(),
                packageName = packageName.trim(),
                versionName = versionName.trim().ifBlank { "1.0.0" },
                versionCode = versionCode.coerceAtLeast(1),
                description = description.trim(),
                templateId = template.id,
                faviconUrl = faviconUrl,
                changeHistoryJson = history,
                createdAt = now,
                updatedAt = now,
            )
        )
        val templatePreset = WebinoDatabase.defaultTemplates().firstOrNull { it.id == template.id }
        val primary = themeColorOverride?.takeIf { it.isNotBlank() } ?: template.primaryHex
        projectDao.upsertProjectSettings(
            ProjectSettingsEntity(
                projectId = projectId,
                appName = name.trim(),
                primaryColorHex = primary,
                backgroundColorHex = template.backgroundHex,
                toolbarStyle = templatePreset?.recommendedToolbar ?: ToolbarStyle.TOP_BAR.name,
                enablePaymentGatewayBridge = templatePreset?.recommendedPaymentBridge ?: true,
            )
        )
        projectDao.upsertWebSettings(
            WebSettingsEntity(
                projectId = projectId,
                javaScriptEnabled = templatePreset?.recommendedJs ?: true,
                domStorageEnabled = templatePreset?.recommendedDom ?: true,
                cacheMode = templatePreset?.recommendedCache ?: CacheModeOption.DEFAULT.name,
                httpsOnly = websiteUrl.startsWith("https://", ignoreCase = true),
            )
        )
        return projectId
    }

    suspend fun updateProjectBundle(
        project: ProjectEntity,
        settings: ProjectSettingsEntity,
        webSettings: WebSettingsEntity,
        changeNote: String,
    ) {
        val existingHistory = runCatching {
            json.decodeFromString<List<ProjectHistoryEntry>>(project.changeHistoryJson)
        }.getOrDefault(emptyList())
        val updatedHistory = (listOf(ProjectHistoryEntry(System.currentTimeMillis(), changeNote)) + existingHistory).take(25)
        projectDao.updateProject(
            project.copy(
                updatedAt = System.currentTimeMillis(),
                changeHistoryJson = json.encodeToString(updatedHistory),
            )
        )
        projectDao.upsertProjectSettings(settings)
        projectDao.upsertWebSettings(webSettings)
    }

    suspend fun duplicateProject(projectId: Long): Long? {
        val bundle = projectDao.getProjectById(projectId) ?: return null
        val now = System.currentTimeMillis()
        val newPkg = "${bundle.project.packageName}.copy"
        val newId = projectDao.insertProject(
            bundle.project.copy(
                id = 0,
                name = "${bundle.project.name} (کپی)",
                packageName = newPkg,
                createdAt = now,
                updatedAt = now,
                changeHistoryJson = json.encodeToString(
                    listOf(ProjectHistoryEntry(now, "کپی از پروژه ${bundle.project.name}"))
                ),
            )
        )
        bundle.settings?.let {
            projectDao.upsertProjectSettings(it.copy(projectId = newId, appName = "${it.appName} (کپی)"))
        }
        bundle.webSettings?.let {
            projectDao.upsertWebSettings(it.copy(projectId = newId))
        }
        return newId
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun exportProjectJson(projectId: Long): String? {
        val bundle = projectDao.getProjectById(projectId) ?: return null
        return com.example.core.ProjectConfigSerializer.exportToJson(bundle)
    }

    suspend fun importProjectJson(rawJson: String): Result<Long> = withContext(Dispatchers.IO) {
        val parsedResult = com.example.core.ProjectConfigSerializer.parseAndValidate(rawJson)
        val parsed = parsedResult.getOrElse { return@withContext Result.failure(it) }
        val (project, settings, web) = com.example.core.ProjectConfigSerializer.toEntities(parsed)
        val history = json.encodeToString(
            listOf(ProjectHistoryEntry(project.createdAt, "ورود تنظیمات از فایل JSON"))
        )
        // One transaction: a failure in the second or third insert must not leave a half-imported project.
        val newId = db.withTransaction {
            val id = projectDao.insertProject(project.copy(changeHistoryJson = history))
            projectDao.upsertProjectSettings(settings.copy(projectId = id))
            projectDao.upsertWebSettings(web.copy(projectId = id))
            id
        }
        Result.success(newId)
    }

    suspend fun saveCroppedProjectImage(projectId: Long, sourceUri: Uri, isSplash: Boolean): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                // Two-pass decode: a 50 MP camera photo decoded at full size can exhaust memory.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                val raw = context.contentResolver.openInputStream(sourceUri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@withContext null
                val size = minOf(raw.width, raw.height)
                val x = (raw.width - size) / 2
                val y = (raw.height - size) / 2
                val square = Bitmap.createBitmap(raw, x, y, size, size)
                val scaled = Bitmap.createScaledBitmap(square, if (isSplash) 512 else 256, if (isSplash) 512 else 256, true)

                val mediaDir = File(context.filesDir, "project_media/$projectId").apply { mkdirs() }
                val dest = File(mediaDir, if (isSplash) "splash_${System.currentTimeMillis()}.png" else "icon_${System.currentTimeMillis()}.png")
                dest.outputStream().use { fos ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                dest.absolutePath
            }.getOrNull()
        }

    suspend fun validateBuildSpec(
        projectId: Long,
        outputFormat: BuildOutputFormat,
        signingKeyId: Long,
    ): Result<Pair<BuildSpec, BuildStorageManager.StorageEstimate>> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = projectDao.getProjectById(projectId)
                ?: throw IllegalArgumentException("پروژه انتخاب‌شده یافت نشد.")
            val keyMeta = (if (signingKeyId > 0L) signingDao.getSigningKeyById(signingKeyId) else null)
                ?: signingDao.getAllSigningKeys().firstOrNull()
                ?: throw IllegalStateException("هیچ کلید امضای دیجیتالی در سیستم ثبت نشده است.")

            val signingMaterial = signingManager.loadSigningMaterial(
                keystorePath = keyMeta.keystorePath,
                alias = keyMeta.alias,
                encryptedPasswordToken = keyMeta.encryptedPasswordToken,
            )

            val settings = bundle.settings ?: ProjectSettingsEntity(projectId = bundle.project.id, appName = bundle.project.name)
            val web = bundle.webSettings ?: WebSettingsEntity(projectId = bundle.project.id)
            val iconBytes = settings.iconFilePath?.let { File(it).takeIf(File::exists)?.readBytes() }
            val splashBytes = settings.splashImagePath?.let { File(it).takeIf(File::exists)?.readBytes() }

            val spec = BuildSpec(
                projectId = bundle.project.id,
                appName = settings.appName.ifBlank { bundle.project.name },
                packageName = bundle.project.packageName,
                versionName = bundle.project.versionName,
                versionCode = bundle.project.versionCode,
                websiteUrl = bundle.project.websiteUrl,
                startPagePath = settings.startPagePath,
                developerName = settings.developerName,
                primaryColorHex = settings.primaryColorHex,
                backgroundColorHex = settings.backgroundColorHex,
                themePreset = runCatching { ThemePreset.valueOf(settings.themePreset) }.getOrDefault(ThemePreset.DARK),
                toolbarStyle = runCatching { ToolbarStyle.valueOf(settings.toolbarStyle) }.getOrDefault(ToolbarStyle.TOP_BAR),
                loadingAnimation = runCatching { LoadingAnimationType.valueOf(settings.loadingAnimation) }.getOrDefault(LoadingAnimationType.PROGRESS_BAR),
                splashEnabled = settings.splashEnabled,
                splashDurationMs = settings.splashDurationMs,
                orientationMode = runCatching { OrientationMode.valueOf(settings.orientationMode) }.getOrDefault(OrientationMode.AUTO),
                fullscreenMode = settings.fullscreenMode,
                showStatusBar = settings.showStatusBar,
                showNavigationBar = settings.showNavigationBar,
                backBehavior = runCatching { BackNavigationBehavior.valueOf(settings.backBehavior) }.getOrDefault(BackNavigationBehavior.HISTORY_THEN_EXIT),
                supportRtl = settings.supportRtl,
                javaScriptEnabled = web.javaScriptEnabled,
                domStorageEnabled = web.domStorageEnabled,
                webSqlEnabled = web.webSqlEnabled,
                clipboardAccessEnabled = web.clipboardAccessEnabled,
                cookiesEnabled = web.cookiesEnabled,
                thirdPartyCookiesEnabled = web.thirdPartyCookiesEnabled,
                fileUploadEnabled = web.fileUploadEnabled,
                downloadManagerEnabled = web.downloadManagerEnabled,
                cameraPermission = web.cameraPermission,
                microphonePermission = web.microphonePermission,
                geolocationEnabled = web.geolocationEnabled,
                webRtcEnabled = web.webRtcEnabled,
                fullscreenVideoEnabled = web.fullscreenVideoEnabled,
                multipleWindowsEnabled = web.multipleWindowsEnabled,
                popupsEnabled = web.popupsEnabled,
                popupBehavior = runCatching { PopupBehaviorMode.valueOf(web.popupBehavior) }.getOrDefault(PopupBehaviorMode.SAME_PAGE),
                customUserAgent = web.customUserAgent,
                cacheMode = runCatching { CacheModeOption.valueOf(web.cacheMode) }.getOrDefault(CacheModeOption.DEFAULT),
                zoomEnabled = web.zoomEnabled,
                mixedContentMode = runCatching { MixedContentOption.valueOf(web.mixedContentMode) }.getOrDefault(MixedContentOption.NEVER_ALLOW),
                sslPolicy = runCatching { SslPolicy.valueOf(web.sslPolicy) }.getOrDefault(SslPolicy.STRICT_BLOCK),
                httpsOnly = web.httpsOnly,
                navigationRulesRaw = web.navigationRulesRaw,
                enableDeepLinks = settings.enableDeepLinks,
                deepLinkScheme = settings.deepLinkScheme,
                enableAppLinks = settings.enableAppLinks,
                appLinkHost = settings.appLinkHost,
                enableFileSharing = settings.enableFileSharing,
                enablePhoneDialer = settings.enablePhoneDialer,
                enableEmailIntent = settings.enableEmailIntent,
                enableMapsIntent = settings.enableMapsIntent,
                enablePaymentGatewayBridge = settings.enablePaymentGatewayBridge,
                externalLinkMode = runCatching { ExternalLinkMode.valueOf(settings.externalLinkMode) }.getOrDefault(ExternalLinkMode.ASK_OR_ALLOWLIST),
                iconPngBytes = iconBytes,
                splashPngBytes = splashBytes,
                outputFormat = outputFormat,
                keyAlias = signingMaterial.alias,
                privateKey = signingMaterial.privateKey,
                certificateChain = signingMaterial.certificateChain,
            )

            val validation = BuildConfiguration.validate(spec)
            if (!validation.isValid) {
                throw IllegalArgumentException(validation.errors.joinToString("\n"))
            }
            val estimate = buildEngine.estimateStorage(spec)
            if (!estimate.hasEnoughSpace) {
                throw IllegalStateException(estimate.summaryFa)
            }
            spec to estimate
        }
    }

    suspend fun validateAndEnqueueBuild(
        projectId: Long,
        outputFormat: BuildOutputFormat,
        signingKeyId: Long,
    ): Result<Long> {
        val check = validateBuildSpec(projectId, outputFormat, signingKeyId)
        return check.map { enqueueBuild(projectId, outputFormat, signingKeyId) }
    }

    suspend fun retryBuildTask(taskId: Long): Result<Long> {
        val prev = buildDao.getBuildTask(taskId)
            ?: return Result.failure(IllegalArgumentException("تسک قبلی یافت نشد."))
        val format = runCatching { BuildOutputFormat.valueOf(prev.outputFormat) }.getOrDefault(BuildOutputFormat.APK)
        return validateAndEnqueueBuild(prev.projectId, format, prev.signingKeyId)
    }

    suspend fun recoverInterruptedBuilds(): Int = withContext(Dispatchers.IO) {
        val activeTasks = buildDao.getActiveBuildTasks()
        if (activeTasks.isEmpty()) return@withContext 0
        var recovered = 0
        val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull()

        for (task in activeTasks) {
            val isStillScheduledOrRunning = task.workRequestId?.let { uuidStr ->
                runCatching {
                    val info = workManager?.getWorkInfoById(UUID.fromString(uuidStr))?.get()
                    info != null && !info.state.isFinished
                }.getOrDefault(false)
            } ?: false

            if (!isStillScheduledOrRunning) {
                val msg = "عملیات ساخت به دلیل بسته شدن ناگهانی برنامه نیمه‌تمام ماند. می‌توانید با «تلاش مجدد» ادامه دهید."
                buildDao.updateBuildTask(
                    task.copy(
                        status = BuildStatus.FAILED.name,
                        finishedAt = System.currentTimeMillis(),
                        errorMessage = msg,
                    )
                )
                buildDao.insertBuildLog(
                    BuildLogEntity(
                        taskId = task.id,
                        level = "ERROR",
                        stepName = task.currentStep,
                        message = msg,
                    )
                )
                recovered++
            }
        }
        recovered
    }

    suspend fun enqueueBuild(
        projectId: Long,
        outputFormat: BuildOutputFormat,
        signingKeyId: Long,
    ): Long {
        val keyId = if (signingKeyId > 0L) {
            signingKeyId
        } else {
            signingDao.getAllSigningKeys().firstOrNull()?.id ?: 0L
        }
        val taskId = buildDao.insertBuildTask(
            BuildTaskEntity(
                projectId = projectId,
                outputFormat = outputFormat.name,
                signingKeyId = keyId,
                status = BuildStatus.QUEUED.name,
                currentStep = BuildStep.CHECKING_ENV.name,
                progressPercent = 0,
                startedAt = System.currentTimeMillis(),
            )
        )
        val inputData = Data.Builder()
            .putLong(BuildWorker.KEY_TASK_ID, taskId)
            .build()
        val request = OneTimeWorkRequestBuilder<BuildWorker>()
            .setInputData(inputData)
            .addTag("webino_build_$taskId")
            .build()
        val existingTask = buildDao.getBuildTask(taskId)
        if (existingTask != null) {
            buildDao.updateBuildTask(existingTask.copy(workRequestId = request.id.toString()))
        }
        WorkManager.getInstance(context).enqueueUniqueWork(
            "webino_build_task_$taskId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return taskId
    }

    suspend fun cancelBuild(taskId: Long) {
        cancelledTaskIds.add(taskId)
        val task = buildDao.getBuildTask(taskId) ?: return
        task.workRequestId?.let { uuidStr ->
            runCatching {
                WorkManager.getInstance(context).cancelWorkById(UUID.fromString(uuidStr))
            }
        }
        buildDao.updateBuildTask(
            task.copy(
                status = BuildStatus.CANCELLED.name,
                finishedAt = System.currentTimeMillis(),
                errorMessage = "عملیات ساخت توسط کاربر لغو شد.",
            )
        )
        buildDao.insertBuildLog(
            BuildLogEntity(
                taskId = taskId,
                level = "WARN",
                stepName = task.currentStep,
                message = "فرایند ساخت لغو و فایلهای موقت پاکسازی شدند.",
            )
        )
    }

    suspend fun runBuildTask(
        taskId: Long,
        externalCancelSignal: () -> Boolean = { false },
    ): Boolean {
        val task = buildDao.getBuildTask(taskId) ?: return false
        if (task.status == BuildStatus.CANCELLED.name || cancelledTaskIds.contains(taskId)) {
            return false
        }

        val format = runCatching { BuildOutputFormat.valueOf(task.outputFormat) }.getOrDefault(BuildOutputFormat.APK)
        val specResult = validateBuildSpec(task.projectId, format, task.signingKeyId)
        if (specResult.isFailure) {
            val err = specResult.exceptionOrNull()?.message ?: "مشخصات پروژه برای ساخت معتبر نیست."
            buildDao.updateBuildTask(
                task.copy(
                    status = BuildStatus.FAILED.name,
                    finishedAt = System.currentTimeMillis(),
                    errorMessage = err,
                )
            )
            buildDao.insertBuildLog(
                BuildLogEntity(
                    taskId = taskId,
                    level = "ERROR",
                    stepName = BuildStep.CHECKING_ENV.name,
                    message = err,
                )
            )
            return false
        }

        val (spec, storageEstimate) = specResult.getOrThrow()
        val redactLogs = runCatching { prefsStore.preferences.first().redactSensitiveLogs }.getOrDefault(true)

        buildDao.updateBuildTask(task.copy(status = BuildStatus.RUNNING.name))
        val outputDir = BuildStorageManager.getOutputsDir(context)

        return try {
            val execResult = buildEngine.executeBuild(
                spec = spec,
                outputDir = outputDir,
                isCancelled = { cancelledTaskIds.contains(taskId) || externalCancelSignal() },
            ) { update ->
                val current = buildDao.getBuildTask(taskId) ?: return@executeBuild
                if (current.status == BuildStatus.CANCELLED.name || cancelledTaskIds.contains(taskId)) return@executeBuild
                buildDao.updateBuildTask(
                    current.copy(
                        status = BuildStatus.RUNNING.name,
                        currentStep = update.step.name,
                        progressPercent = update.progressPercent,
                    )
                )
                update.logEntry?.let { entry ->
                    buildDao.insertBuildLog(
                        BuildLogEntity(
                            taskId = taskId,
                            timestamp = entry.timestamp,
                            level = entry.level,
                            stepName = entry.step.name,
                            message = BuildReportGenerator.redactSensitive(entry.message, enabled = redactLogs),
                        )
                    )
                }
            }

            runCatching {
                val report = BuildReportGenerator.generateReport(
                    taskId = taskId,
                    spec = spec,
                    result = execResult,
                    storageEstimate = storageEstimate,
                    redactSecrets = redactLogs,
                )
                val reportFile = BuildReportGenerator.saveReport(context, report)
                buildDao.insertBuildLog(
                    BuildLogEntity(
                        taskId = taskId,
                        level = "INFO",
                        stepName = BuildStep.COMPLETED.name,
                        message = "گزارش ساخت ذخیره شد: ${reportFile.name}",
                    )
                )
            }

            val finalTask = buildDao.getBuildTask(taskId) ?: task
            if (finalTask.status == BuildStatus.CANCELLED.name || execResult.cancelled) {
                execResult.artifacts.forEach { File(it.filePath).delete() }
                if (finalTask.status != BuildStatus.CANCELLED.name) {
                    buildDao.updateBuildTask(
                        finalTask.copy(
                            status = BuildStatus.CANCELLED.name,
                            finishedAt = System.currentTimeMillis(),
                            errorMessage = execResult.errorMessage ?: "عملیات ساخت لغو شد.",
                        )
                    )
                }
                return false
            }

            if (execResult.success && execResult.artifacts.isNotEmpty()) {
                execResult.artifacts.forEach { art ->
                    buildDao.insertBuildResult(
                        BuildResultEntity(
                            taskId = taskId,
                            projectId = spec.projectId,
                            appName = spec.appName,
                            packageName = spec.packageName,
                            versionName = spec.versionName,
                            versionCode = spec.versionCode,
                            format = art.format.name,
                            filePath = art.filePath,
                            fileSizeBytes = art.fileSizeBytes,
                            isSigned = art.isSigned,
                            isVerified = art.isVerified,
                            certFingerprintSha256 = art.sha256CertFingerprint,
                            validationSummary = art.validationSummary,
                        )
                    )
                }
                buildDao.updateBuildTask(
                    finalTask.copy(
                        status = BuildStatus.SUCCEEDED.name,
                        currentStep = BuildStep.COMPLETED.name,
                        progressPercent = 100,
                        finishedAt = System.currentTimeMillis(),
                    )
                )
                true
            } else {
                execResult.logs.filter { it.level == "ERROR" }.forEach { err ->
                    buildDao.insertBuildLog(
                        BuildLogEntity(
                            taskId = taskId,
                            timestamp = err.timestamp,
                            level = err.level,
                            stepName = err.step.name,
                            message = BuildReportGenerator.redactSensitive(err.message, enabled = redactLogs),
                        )
                    )
                }
                buildDao.updateBuildTask(
                    finalTask.copy(
                        status = BuildStatus.FAILED.name,
                        finishedAt = System.currentTimeMillis(),
                        errorMessage = BuildReportGenerator.redactSensitive(
                            execResult.errorMessage ?: "خطای ناشناخته در ساخت خروجی",
                            enabled = redactLogs,
                        ),
                    )
                )
                false
            }
        } finally {
            cancelledTaskIds.remove(taskId)
        }
    }

    suspend fun verifyOutput(resultId: Long): String {
        val result = buildDao.getBuildResultById(resultId) ?: return "خروجی یافت نشد."
        val file = File(result.filePath)
        val report = if (result.format == BuildOutputFormat.AAB.name) {
            OutputValidator.validateAab(file)
        } else {
            OutputValidator.validateApk(file)
        }
        buildDao.updateBuildResult(
            result.copy(
                isVerified = report.isValid,
                isSigned = report.isSigned,
                validationSummary = report.summary,
                certFingerprintSha256 = report.certSha256.ifEmpty { result.certFingerprintSha256 },
            )
        )
        return report.summary
    }

    suspend fun deleteOutput(resultId: Long) {
        val result = buildDao.getBuildResultById(resultId)
        if (result != null) {
            File(result.filePath).delete()
            buildDao.deleteBuildResultById(resultId)
        }
    }

    suspend fun createSigningKey(
        alias: String,
        password: String,
        commonName: String,
        organization: String,
        country: String,
        validityYears: Int,
    ): Long = withContext(Dispatchers.IO) {
        // RSA-2048 key generation can take seconds on low-end phones: never run it on the main thread.
        val info = signingManager.createNewKeystore(alias, password, commonName, organization, country, validityYears)
        signingDao.clearDefaultKeys()
        signingDao.insertSigningKey(
            SigningKeyMetadataEntity(
                alias = info.alias,
                keystorePath = info.keystorePath,
                keystoreType = info.keystoreType,
                commonName = info.commonName,
                organization = info.organization,
                country = info.country,
                validityYears = info.validityYears,
                sha256Fingerprint = info.sha256Fingerprint,
                sha1Fingerprint = info.sha1Fingerprint,
                encryptedPasswordToken = info.encryptedPasswordToken,
                isDefault = true,
            )
        )
    }

    suspend fun importSigningKey(uri: Uri, password: String): Long = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("امکان خواندن فایل Keystore وجود ندارد.")
        val info = signingManager.importKeystoreBytes(bytes, "imported.p12", password)
        signingDao.clearDefaultKeys()
        signingDao.insertSigningKey(
            SigningKeyMetadataEntity(
                alias = info.alias,
                keystorePath = info.keystorePath,
                keystoreType = info.keystoreType,
                commonName = info.commonName,
                organization = info.organization,
                country = info.country,
                validityYears = info.validityYears,
                sha256Fingerprint = info.sha256Fingerprint,
                sha1Fingerprint = info.sha1Fingerprint,
                encryptedPasswordToken = info.encryptedPasswordToken,
                isDefault = true,
            )
        )
    }

    suspend fun setDefaultSigningKey(keyId: Long) {
        signingDao.clearDefaultKeys()
        signingDao.markAsDefault(keyId)
    }

    suspend fun changeSigningPassword(keyId: Long, newPassword: String): Unit = withContext(Dispatchers.IO) {
        val entity = signingDao.getSigningKeyById(keyId) ?: return@withContext
        // Keep a backup until the new token is stored, so file and token can never end up mismatched.
        val keyFile = File(entity.keystorePath)
        val backup = File(keyFile.parentFile, keyFile.name + ".bak")
        keyFile.copyTo(backup, overwrite = true)
        try {
            val newToken = signingManager.changeKeystorePassword(
                keystorePath = entity.keystorePath,
                alias = entity.alias,
                oldEncryptedToken = entity.encryptedPasswordToken,
                newPassword = newPassword,
            )
            signingDao.updateSigningKey(entity.copy(encryptedPasswordToken = newToken))
        } catch (e: Throwable) {
            runCatching { backup.copyTo(keyFile, overwrite = true) }
            throw e
        } finally {
            backup.delete()
        }
    }

    suspend fun verifySigningKey(keyId: Long): Boolean = withContext(Dispatchers.IO) {
        val entity = signingDao.getSigningKeyById(keyId) ?: return@withContext false
        signingManager.verifyKeystoreIntegrity(entity.keystorePath, entity.alias, entity.encryptedPasswordToken)
    }

    suspend fun deleteSigningKey(keyId: Long): Unit = withContext(Dispatchers.IO) {
        val entity = signingDao.getSigningKeyById(keyId) ?: return@withContext
        File(entity.keystorePath).delete()
        signingDao.deleteSigningKey(keyId)
    }

    fun calculateStorageBreakdown(): Triple<Long, Long, Long> {
        val b = BuildStorageManager.calculateBreakdown(context)
        return Triple(b.outputsBytes + b.reportsBytes, b.cacheBytes, b.mediaBytes)
    }

    fun clearTempBuildCache(): Long = BuildStorageManager.clearTempBuildCache(context)

    suspend fun setLanguage(lang: AppLanguage) = prefsStore.updateLanguage(lang)
    suspend fun setThemeMode(mode: AppThemeMode) = prefsStore.updateThemeMode(mode)
    suspend fun setFontChoice(font: AppFontChoice) = prefsStore.updateFontChoice(font)
    suspend fun setCalendarType(cal: CalendarType) = prefsStore.updateCalendarType(cal)
    suspend fun setPersianDigits(enabled: Boolean) = prefsStore.updatePersianDigits(enabled)
    suspend fun setClearCacheOnExit(enabled: Boolean) = prefsStore.updateClearCacheOnExit(enabled)
    suspend fun setRedactLogs(enabled: Boolean) = prefsStore.updateRedactLogs(enabled)

    suspend fun setOnboardingCompleted(done: Boolean) = prefsStore.updateOnboardingCompleted(done)

    /**
     * Plain-text support report for one build: device info + task summary + full log.
     * Secrets are always redacted here, regardless of the user's log setting, because the text leaves the app.
     */
    suspend fun buildSupportReport(taskId: Long, isFa: Boolean): String = withContext(Dispatchers.IO) {
        val task = buildDao.getBuildTask(taskId) ?: return@withContext ""
        val logs = buildDao.getBuildLogs(taskId)
        val project = runCatching { projectDao.getProjectById(task.projectId)?.project }.getOrNull()
        val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        fun redact(s: String) = BuildReportGenerator.redactSensitive(s, enabled = true)
        buildString {
            appendLine("WEBINO build report")
            appendLine("App version: ${red.line.vebino.BuildConfig.VERSION_NAME} (${red.line.vebino.BuildConfig.VERSION_CODE})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} • Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Task: #${task.id}")
            appendLine("Project: ${project?.name ?: "-"} • ${project?.packageName ?: "-"}")
            appendLine("Format: ${task.outputFormat}")
            appendLine("Status: ${task.status} (${task.progressPercent}%) • step ${task.currentStep}")
            appendLine("Started: ${df.format(java.util.Date(task.startedAt))}")
            task.finishedAt?.let { appendLine("Finished: ${df.format(java.util.Date(it))}") }
            task.errorMessage?.let { appendLine("Error: ${redact(it)}") }
            appendLine("----- LOG (${logs.size}) -----")
            logs.forEach { l ->
                appendLine("${df.format(java.util.Date(l.timestamp))} [${l.level}] ${l.stepName}: ${redact(l.message)}")
            }
        }
    }
}