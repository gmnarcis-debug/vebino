package com.example.buildengine

import com.example.domain.model.EnvironmentCheckReport
import java.io.File

/**
 * Top-level contract for WEBINO's standalone On-Device Android Build Engine.
 */
interface BuildEngine {
    fun inspectEnvironment(): EnvironmentCheckReport
    fun validateConfiguration(spec: BuildSpec): BuildConfiguration.ValidationResult
    fun estimateStorage(spec: BuildSpec): BuildStorageManager.StorageEstimate
    suspend fun executeBuild(
        spec: BuildSpec,
        outputDir: File,
        isCancelled: () -> Boolean = { false },
        onProgress: suspend (BuildProgressUpdate) -> Unit,
    ): BuildExecutionResult
}
