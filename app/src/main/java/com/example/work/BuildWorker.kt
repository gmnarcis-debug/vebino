package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.WebinoApplication
import com.example.domain.model.AppLanguage
import com.example.domain.model.BuildStatus
import com.example.domain.model.BuildStep
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return@coroutineScope Result.failure()

        val app = applicationContext as? WebinoApplication ?: return@coroutineScope Result.failure()
        val buildDao = app.database.buildDao()
        val projectDao = app.database.projectDao()

        val isFa = runCatching { app.repository.preferences.first().language == AppLanguage.FA }.getOrDefault(true)
        val texts = textsFor(isFa)
        val notifier = BuildNotifier(applicationContext)
        notifier.ensureChannel(texts)

        val initialTask = buildDao.getBuildTask(taskId)
        val appName = initialTask
            ?.let { runCatching { projectDao.getProjectById(it.projectId)?.project?.name }.getOrNull() }
            ?: "WEBINO"

        val cancelIntent = androidx.work.WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        // Promote to a foreground service so the build survives the app going to background.
        runCatching {
            val first = notifier.progressNotification(
                taskId, appName, stepLabel(BuildStep.CHECKING_ENV.name, isFa), 0, true, texts, cancelIntent,
            )
            setForeground(notifier.foregroundInfo(taskId, first))
        }

        // Keep the notification in sync with the DB progress while the build runs.
        val progressJob = launch {
            var lastShown = -1 to ""
            while (isActive) {
                val t = buildDao.getBuildTask(taskId)
                if (t != null) {
                    val key = t.progressPercent to t.currentStep
                    if (key != lastShown) {
                        lastShown = key
                        val n = notifier.progressNotification(
                            taskId = taskId,
                            appName = appName,
                            stepLabel = stepLabel(t.currentStep, isFa),
                            percent = t.progressPercent,
                            queued = t.status == BuildStatus.QUEUED.name,
                            texts = texts,
                            cancelIntent = cancelIntent,
                        )
                        runCatching { setForeground(notifier.foregroundInfo(taskId, n)) }
                    }
                }
                delay(1000)
            }
        }

        val ok = try {
            app.repository.runBuildTask(taskId, externalCancelSignal = { isStopped })
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancelled from the notification (or by the system): make the DB reflect it, otherwise
            // the history would keep showing "building" forever.
            withContext(NonCancellable) {
                val t = buildDao.getBuildTask(taskId)
                if (t != null && t.status != BuildStatus.CANCELLED.name &&
                    t.status != BuildStatus.SUCCEEDED.name && t.status != BuildStatus.FAILED.name
                ) {
                    buildDao.updateBuildTask(
                        t.copy(
                            status = BuildStatus.CANCELLED.name,
                            finishedAt = System.currentTimeMillis(),
                            errorMessage = if (isFa) "ساخت لغو شد." else "Build cancelled.",
                        )
                    )
                }
            }
            throw e
        } finally {
            progressJob.cancel()
        }

        val finalTask = buildDao.getBuildTask(taskId)
        val status = runCatching { BuildStatus.valueOf(finalTask?.status ?: "") }
            .getOrDefault(if (ok) BuildStatus.SUCCEEDED else BuildStatus.FAILED)
        notifier.showResult(taskId, appName, status, finalTask?.errorMessage, texts)

        if (ok) Result.success() else Result.failure()
    }

    private fun stepLabel(step: String, isFa: Boolean): String {
        val known = runCatching { BuildStep.valueOf(step) }.getOrNull()
        return known?.let { if (isFa) it.titleFa else it.titleEn } ?: step
    }

    private fun textsFor(isFa: Boolean) = if (isFa) {
        BuildNotifier.Texts(
            channelName = "ساخت اپلیکیشن",
            channelDesc = "پیشرفت و نتیجه‌ی ساخت APK و AAB",
            running = "در حال ساخت",
            queued = "در صف ساخت",
            succeeded = "ساخت موفق",
            failed = "خطا در ساخت",
            cancelled = "ساخت لغو شد",
            cancelAction = "لغو",
            tapToOpen = "برای مشاهده‌ی نتیجه لمس کنید",
        )
    } else {
        BuildNotifier.Texts(
            channelName = "App builds",
            channelDesc = "Progress and result of APK and AAB builds",
            running = "Building",
            queued = "Queued",
            succeeded = "Build succeeded",
            failed = "Build failed",
            cancelled = "Build cancelled",
            cancelAction = "Cancel",
            tapToOpen = "Tap to see the result",
        )
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}
