package com.example.buildengine

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class BuildReportArtifactItem(
    val format: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val isSigned: Boolean,
    val isVerified: Boolean,
    val certSha256: String,
    val validationSummary: String,
)

@Serializable
data class BuildReportLogItem(
    val timestamp: Long,
    val level: String,
    val step: String,
    val message: String,
)

@Serializable
data class BuildReport(
    val taskId: Long,
    val projectId: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val outputFormat: String,
    val keyAlias: String,
    val success: Boolean,
    val durationMs: Long,
    val generatedAtMs: Long,
    val estimatedTempBytes: Long,
    val estimatedOutputBytes: Long,
    val errorMessage: String? = null,
    val artifacts: List<BuildReportArtifactItem> = emptyList(),
    val logs: List<BuildReportLogItem> = emptyList(),
)

/**
 * Generates structured persistent JSON build reports and enforces sensitive-data redaction
 * so keystore passwords, vault tokens, or private keys never leak into saved logs.
 */
object BuildReportGenerator {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val sensitivePatterns = listOf(
        Regex("""(?i)(password|passwd|pwd|storePassword|keyPassword|token|secret)\s*[:=]\s*[^\s,;]+"""),
        Regex("""(AKS|LOC):[A-Za-z0-9+/=]{12,}"""),
        Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----"""),
    )

    fun redactSensitive(
        message: String,
        enabled: Boolean = true,
        explicitSecrets: List<String> = emptyList(),
    ): String {
        if (!enabled) return message
        var out = message
        explicitSecrets.filter { it.length >= 4 }.forEach { secret ->
            out = out.replace(secret, "***REDACTED***")
        }
        sensitivePatterns.forEach { regex ->
            out = regex.replace(out) { match ->
                val raw = match.value
                val sepIdx = raw.indexOfAny(charArrayOf(':', '='))
                if (sepIdx > 0 && !raw.startsWith("AKS:") && !raw.startsWith("LOC:")) {
                    "${raw.substring(0, sepIdx + 1)} ***REDACTED***"
                } else {
                    "***REDACTED***"
                }
            }
        }
        return out
    }

    fun generateReport(
        taskId: Long,
        spec: BuildSpec,
        result: BuildExecutionResult,
        storageEstimate: BuildStorageManager.StorageEstimate?,
        redactSecrets: Boolean = true,
    ): BuildReport {
        val redactedLogs = result.logs.map { entry ->
            BuildReportLogItem(
                timestamp = entry.timestamp,
                level = entry.level,
                step = entry.step.name,
                message = redactSensitive(entry.message, enabled = redactSecrets),
            )
        }
        val redactedError = result.errorMessage?.let { redactSensitive(it, enabled = redactSecrets) }
        return BuildReport(
            taskId = taskId,
            projectId = spec.projectId,
            appName = spec.appName,
            packageName = spec.packageName,
            versionName = spec.versionName,
            versionCode = spec.versionCode,
            outputFormat = spec.outputFormat.name,
            keyAlias = spec.keyAlias,
            success = result.success,
            durationMs = result.durationMs,
            generatedAtMs = System.currentTimeMillis(),
            estimatedTempBytes = storageEstimate?.estimatedTempBytes ?: 0L,
            estimatedOutputBytes = storageEstimate?.estimatedOutputBytes ?: 0L,
            errorMessage = redactedError,
            artifacts = result.artifacts.map { art ->
                BuildReportArtifactItem(
                    format = art.format.name,
                    filePath = art.filePath,
                    fileSizeBytes = art.fileSizeBytes,
                    isSigned = art.isSigned,
                    isVerified = art.isVerified,
                    certSha256 = art.sha256CertFingerprint,
                    validationSummary = art.validationSummary,
                )
            },
            logs = redactedLogs,
        )
    }

    fun saveReport(
        context: Context,
        report: BuildReport,
    ): File {
        val dir = BuildStorageManager.getReportsDir(context)
        val outFile = File(dir, "build_report_${report.taskId}.json")
        outFile.writeText(json.encodeToString(report), Charsets.UTF_8)
        return outFile
    }
}
