package com.example.domain.usecase

import com.example.data.repository.WebinoRepository
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.EnvironmentCheckReport

class InspectBuildEnvironmentUseCase(private val repository: WebinoRepository) {
    operator fun invoke(): EnvironmentCheckReport = repository.inspectEnvironment()
}

class StartBuildUseCase(private val repository: WebinoRepository) {
    suspend operator fun invoke(
        projectId: Long,
        outputFormat: BuildOutputFormat,
        signingKeyId: Long,
    ): Result<Long> = repository.validateAndEnqueueBuild(projectId, outputFormat, signingKeyId)
}

class CancelBuildUseCase(private val repository: WebinoRepository) {
    suspend operator fun invoke(taskId: Long) = repository.cancelBuild(taskId)
}

class RetryBuildUseCase(private val repository: WebinoRepository) {
    suspend operator fun invoke(taskId: Long): Result<Long> = repository.retryBuildTask(taskId)
}

class RecoverInterruptedBuildsUseCase(private val repository: WebinoRepository) {
    suspend operator fun invoke(): Int = repository.recoverInterruptedBuilds()
}
