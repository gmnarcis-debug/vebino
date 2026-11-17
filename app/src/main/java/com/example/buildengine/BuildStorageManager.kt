package com.example.buildengine

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Manages isolated temporary build workspaces, pre-build disk headroom estimation,
 * post-build / post-cancel cleanup, and output/report storage accounting.
 */
object BuildStorageManager {

    private const val BASE_TEMP_OVERHEAD_BYTES = 14L * 1024L * 1024L // android-stubs + ECJ classes + dx temp
    private const val BASE_ARTIFACT_BYTES = 2L * 1024L * 1024L
    private const val SAFETY_HEADROOM_BYTES = 8L * 1024L * 1024L

    data class StorageEstimate(
        val estimatedTempBytes: Long,
        val estimatedOutputBytes: Long,
        val requiredTotalBytes: Long,
        val availableBytes: Long,
        val hasEnoughSpace: Boolean,
        val summaryFa: String,
    )

    data class StorageBreakdown(
        val outputsBytes: Long,
        val reportsBytes: Long,
        val tempBuildBytes: Long,
        val cacheBytes: Long,
        val mediaBytes: Long,
        val freeInternalBytes: Long,
    )

    fun estimateBuildStorage(context: Context, spec: BuildSpec): StorageEstimate {
        val mediaBytes = (spec.iconPngBytes?.size?.toLong() ?: 0L) +
            (spec.splashPngBytes?.size?.toLong() ?: 0L)
        val formatMultiplier = when (spec.outputFormat) {
            com.example.domain.model.BuildOutputFormat.BOTH -> 2L
            else -> 1L
        }
        val estimatedTemp = BASE_TEMP_OVERHEAD_BYTES + mediaBytes * 2L
        val estimatedOutput = (BASE_ARTIFACT_BYTES + mediaBytes) * formatMultiplier
        val requiredTotal = estimatedTemp + estimatedOutput + SAFETY_HEADROOM_BYTES
        val available = queryAvailableBytes(context.filesDir)
        val enough = available >= requiredTotal

        val reqMb = (requiredTotal / (1024.0 * 1024.0))
        val availMb = (available / (1024.0 * 1024.0))
        val summaryFa = if (enough) {
            String.format(
                java.util.Locale.US,
                "فضای موردنیاز برآوردی: %.1f مگابایت • فضای آزاد دستگاه: %.1f مگابایت",
                reqMb,
                availMb,
            )
        } else {
            String.format(
                java.util.Locale.US,
                "فضای خالی دستگاه کافی نیست (نیاز: %.1f مگابایت، موجود: %.1f مگابایت).",
                reqMb,
                availMb,
            )
        }

        return StorageEstimate(
            estimatedTempBytes = estimatedTemp,
            estimatedOutputBytes = estimatedOutput,
            requiredTotalBytes = requiredTotal,
            availableBytes = available,
            hasEnoughSpace = enough,
            summaryFa = summaryFa,
        )
    }

    fun queryAvailableBytes(path: File): Long {
        val target = if (path.exists()) path else (path.parentFile ?: path)
        return runCatching {
            val stat = StatFs(target.absolutePath)
            val statAvail = stat.availableBlocksLong * stat.blockSizeLong
            if (statAvail > 0L) statAvail else target.usableSpace
        }.getOrElse { target.usableSpace }
    }

    fun getBuildsCacheRoot(context: Context): File =
        File(context.cacheDir, "webino_builds").apply { mkdirs() }

    fun createWorkDir(context: Context, projectId: Long, buildId: Long): File =
        File(getBuildsCacheRoot(context), "build_${projectId}_$buildId").apply { mkdirs() }

    fun getOutputsDir(context: Context): File =
        File(context.filesDir, "outputs").apply { mkdirs() }

    fun getReportsDir(context: Context): File =
        File(context.filesDir, "build_reports").apply { mkdirs() }

    fun cleanupWorkDir(workDir: File?) {
        if (workDir != null && workDir.exists()) {
            runCatching { workDir.deleteRecursively() }
        }
    }

    fun cleanupOrphanedWorkDirs(context: Context, maxAgeMs: Long = 3_600_000L): Int {
        val now = System.currentTimeMillis()
        var deletedCount = 0
        getBuildsCacheRoot(context).listFiles()?.forEach { dir ->
            if (now - dir.lastModified() >= maxAgeMs) {
                if (dir.deleteRecursively()) deletedCount++
            }
        }
        // Also sweep legacy webino_build_* cache folders
        context.cacheDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("webino_build_") && now - dir.lastModified() >= maxAgeMs) {
                if (dir.deleteRecursively()) deletedCount++
            }
        }
        return deletedCount
    }

    fun calculateBreakdown(context: Context): StorageBreakdown {
        val outputs = dirSizeBytes(getOutputsDir(context))
        val reports = dirSizeBytes(getReportsDir(context))
        val tempBuilds = dirSizeBytes(getBuildsCacheRoot(context))
        val cache = dirSizeBytes(context.cacheDir)
        val media = dirSizeBytes(File(context.filesDir, "project_media"))
        val free = queryAvailableBytes(context.filesDir)
        return StorageBreakdown(
            outputsBytes = outputs,
            reportsBytes = reports,
            tempBuildBytes = tempBuilds,
            cacheBytes = cache,
            mediaBytes = media,
            freeInternalBytes = free,
        )
    }

    fun clearTempBuildCache(context: Context): Long {
        val freed = dirSizeBytes(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return freed
    }

    private fun dirSizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
