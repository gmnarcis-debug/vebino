package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface WebinoRoute {
    @Serializable
    data object Splash : WebinoRoute

    @Serializable
    data object Dashboard : WebinoRoute

    @Serializable
    data object Projects : WebinoRoute

    @Serializable
    data object Outputs : WebinoRoute

    @Serializable
    data object Settings : WebinoRoute

    @Serializable
    data class CreateProject(val presetTemplateId: String? = null) : WebinoRoute

    @Serializable
    data class ProjectStudio(val projectId: Long, val initialTab: Int = 0) : WebinoRoute

    @Serializable
    data class LivePreview(val projectId: Long) : WebinoRoute

    @Serializable
    data class BuildManager(val projectId: Long, val taskId: Long = 0L) : WebinoRoute

    @Serializable
    data object SigningManager : WebinoRoute

    @Serializable
    data object BuildHistory : WebinoRoute

    @Serializable
    data object HelpCenter : WebinoRoute

    @Serializable
    data object About : WebinoRoute
}
