package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.DoubleBackExitState
import com.example.core.ProvideLocalizedResources
import com.example.core.appString
import com.example.domain.model.AppLanguage
import com.example.domain.model.BuildStatus
import com.example.ui.components.FloatingNavItemData
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoAmbientBackground
import com.example.ui.components.WebinoFloatingGlassBottomBar
import com.example.ui.screens.BuildHistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.navigation.WebinoRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BuildManagerScreen
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HelpCenterScreen
import com.example.ui.screens.LivePreviewScreen
import com.example.ui.screens.OutputsScreen
import com.example.ui.screens.ProjectStudioScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SigningManagerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.WebinoTheme
import com.example.ui.theme.WebinoThemeTokens
import com.example.ui.viewmodel.WebinoViewModel
import kotlinx.coroutines.launch
import red.line.vebino.R

private data class TopNavItem(
    val route: WebinoRoute,
    val labelRes: Int,
    val testTag: String,
    val iconType: Webino3DIconType,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebinoAppRoot()
        }
    }
}

@Composable
fun WebinoAppRoot(viewModel: WebinoViewModel = viewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val outputs by viewModel.buildResults.collectAsStateWithLifecycle()
    val recentTasks by viewModel.recentBuildTasks.collectAsStateWithLifecycle()
    val signingKeys by viewModel.signingKeys.collectAsStateWithLifecycle()
    val recentWebsites by viewModel.recentWebsites.collectAsStateWithLifecycle()
    val envReport by viewModel.environmentReport.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val createForm by viewModel.createForm.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val doubleBackState = remember { DoubleBackExitState(2000L) }
    var showActiveBuildExitDialog by remember { mutableStateOf(false) }

    val hasCompletedSplash by viewModel.hasCompletedSplash.collectAsStateWithLifecycle()
    val startDestination = remember {
        if (hasCompletedSplash) WebinoRoute.Dashboard else WebinoRoute.Splash
    }

    val isFa = prefs.language == AppLanguage.FA

    // Android 13+: ask once for notification permission so build progress can be shown.
    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(hasCompletedSplash, prefs.onboardingCompleted) {
        if (hasCompletedSplash && prefs.onboardingCompleted &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val layoutDir = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr
    val snackbarHostState = remember { SnackbarHostState() }
    val doubleBackPromptMsg = appString(R.string.double_back_exit_prompt)

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        ProvideLocalizedResources(prefs.language) {
            WebinoTheme(themeMode = prefs.themeMode, fontChoice = prefs.fontChoice) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                val currentBuildRoute = remember(navBackStackEntry) {
                    if (currentDest?.hasRoute<WebinoRoute.BuildManager>() == true) {
                        runCatching { navBackStackEntry?.toRoute<WebinoRoute.BuildManager>() }.getOrNull()
                    } else {
                        null
                    }
                }

                val topNavItems = remember {
                    listOf(
                        TopNavItem(
                            route = WebinoRoute.Dashboard,
                            labelRes = R.string.nav_home,
                            testTag = "nav_home",
                            iconType = Webino3DIconType.HOME,
                            selectedIcon = Icons.Rounded.Home,
                            unselectedIcon = Icons.Outlined.Home,
                        ),
                        TopNavItem(
                            route = WebinoRoute.Projects,
                            labelRes = R.string.nav_projects,
                            testTag = "nav_projects",
                            iconType = Webino3DIconType.PROJECTS,
                            selectedIcon = Icons.Rounded.FolderCopy,
                            unselectedIcon = Icons.Outlined.FolderCopy,
                        ),
                        TopNavItem(
                            route = WebinoRoute.BuildManager(0L),
                            labelRes = R.string.nav_build,
                            testTag = "nav_build",
                            iconType = Webino3DIconType.BUILD,
                            selectedIcon = Icons.Rounded.Build,
                            unselectedIcon = Icons.Outlined.Build,
                        ),
                        TopNavItem(
                            route = WebinoRoute.Outputs,
                            labelRes = R.string.nav_outputs,
                            testTag = "nav_outputs",
                            iconType = Webino3DIconType.OUTPUTS,
                            selectedIcon = Icons.Rounded.Android,
                            unselectedIcon = Icons.Outlined.Android,
                        ),
                        TopNavItem(
                            route = WebinoRoute.Settings,
                            labelRes = R.string.nav_settings,
                            testTag = "nav_settings",
                            iconType = Webino3DIconType.SETTINGS,
                            selectedIcon = Icons.Rounded.Settings,
                            unselectedIcon = Icons.Outlined.Settings,
                        ),
                    )
                }

                val showMainNav = currentDest?.let { dest ->
                    dest.hasRoute<WebinoRoute.Dashboard>() ||
                        dest.hasRoute<WebinoRoute.Projects>() ||
                        (dest.hasRoute<WebinoRoute.BuildManager>() && currentBuildRoute?.projectId == 0L) ||
                        dest.hasRoute<WebinoRoute.Outputs>() ||
                        dest.hasRoute<WebinoRoute.Settings>()
                } ?: false

                val tokens = WebinoThemeTokens.colors

                WebinoAmbientBackground {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp

                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                if (showMainNav && !useRail) {
                                    val floatingItems = topNavItems.map { item ->
                                        val selected = when (item.route) {
                                            WebinoRoute.Dashboard -> currentDest?.hasRoute<WebinoRoute.Dashboard>() == true
                                            WebinoRoute.Projects -> currentDest?.hasRoute<WebinoRoute.Projects>() == true
                                            is WebinoRoute.BuildManager -> currentDest?.hasRoute<WebinoRoute.BuildManager>() == true
                                            WebinoRoute.Outputs -> currentDest?.hasRoute<WebinoRoute.Outputs>() == true
                                            WebinoRoute.Settings -> currentDest?.hasRoute<WebinoRoute.Settings>() == true
                                            else -> false
                                        }
                                        val label = appString(item.labelRes)
                                        FloatingNavItemData(
                                            label = label,
                                            testTag = item.testTag,
                                            iconType = item.iconType,
                                            selectedIcon = item.selectedIcon,
                                            unselectedIcon = item.unselectedIcon,
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(WebinoRoute.Dashboard) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        )
                                    }
                                    WebinoFloatingGlassBottomBar(items = floatingItems)
                                }
                            },
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                            ) {
                                if (showMainNav && useRail) {
                                    NavigationRail(
                                        containerColor = tokens.navPillContainer,
                                    ) {
                                        Spacer(Modifier.height(12.dp))
                                        topNavItems.forEach { item ->
                                            val selected = when (item.route) {
                                                WebinoRoute.Dashboard -> currentDest?.hasRoute<WebinoRoute.Dashboard>() == true
                                                WebinoRoute.Projects -> currentDest?.hasRoute<WebinoRoute.Projects>() == true
                                                is WebinoRoute.BuildManager -> currentDest?.hasRoute<WebinoRoute.BuildManager>() == true
                                                WebinoRoute.Outputs -> currentDest?.hasRoute<WebinoRoute.Outputs>() == true
                                                WebinoRoute.Settings -> currentDest?.hasRoute<WebinoRoute.Settings>() == true
                                                else -> false
                                            }
                                            val label = appString(item.labelRes)
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(item.route) {
                                                        popUpTo(WebinoRoute.Dashboard) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                modifier = Modifier.testTag(item.testTag),
                                                icon = {
                                                    if (selected) {
                                                        Webino3DGlassIcon(
                                                            type = item.iconType,
                                                            size = 28.dp,
                                                            contentDescription = label,
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = item.unselectedIcon,
                                                            contentDescription = label,
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = startDestination,
                                        enterTransition = {
                                            fadeIn(tween(240)) + slideIntoContainer(
                                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                                animationSpec = tween(240),
                                                initialOffset = { it / 10 },
                                            )
                                        },
                                        exitTransition = {
                                            fadeOut(tween(180)) + slideOutOfContainer(
                                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                                animationSpec = tween(180),
                                                targetOffset = { it / 14 },
                                            )
                                        },
                                        popEnterTransition = {
                                            fadeIn(tween(220)) + slideIntoContainer(
                                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                                animationSpec = tween(220),
                                                initialOffset = { it / 12 },
                                            )
                                        },
                                        popExitTransition = {
                                            fadeOut(tween(180)) + slideOutOfContainer(
                                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                                animationSpec = tween(180),
                                                targetOffset = { it / 10 },
                                            )
                                        },
                                    ) {
                                        composable<WebinoRoute.Splash> {
                                            // جلوگیری از بسته شدن برنامه با زدن دکمه Back در زمان لود شدن اولیه
                                            BackHandler(enabled = true) { /* Do nothing */ }
                                            SplashScreen(
                                                language = prefs.language,
                                                onFinished = {
                                                    viewModel.markSplashCompleted()
                                                    navController.navigate(WebinoRoute.Dashboard) {
                                                        popUpTo(WebinoRoute.Splash) { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.Dashboard> {
                                            val hasActiveBuild = recentTasks.any {
                                                it.status == BuildStatus.RUNNING.name || it.status == BuildStatus.QUEUED.name
                                            }
                                            BackHandler {
                                                if (hasActiveBuild) {
                                                    showActiveBuildExitDialog = true
                                                } else if (doubleBackState.onBackPressed()) {
                                                    (context as? Activity)?.finish()
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(
                                                            message = doubleBackPromptMsg,
                                                            duration = SnackbarDuration.Short,
                                                        )
                                                    }
                                                }
                                            }
                                            DashboardScreen(
                                                prefs = prefs,
                                                projects = projects,
                                                outputs = outputs,
                                                recentTasks = recentTasks,
                                                envReport = envReport,
                                                storageStats = storageStats,
                                                onCreateProject = { presetId ->
                                                    viewModel.resetCreateForm(presetId)
                                                    navController.navigate(WebinoRoute.CreateProject(presetId))
                                                },
                                                onOpenProject = { id ->
                                                    navController.navigate(WebinoRoute.ProjectStudio(id))
                                                },
                                                onOpenBuildManager = { id ->
                                                    navController.navigate(WebinoRoute.BuildManager(id))
                                                },
                                                onOpenOutputs = {
                                                    navController.navigate(WebinoRoute.Outputs)
                                                },
                                                onOpenHelp = {
                                                    navController.navigate(WebinoRoute.HelpCenter)
                                                },
                                                onRecheckEngine = viewModel::recheckEnvironment,
                                                onOpenSigningManager = {
                                                    navController.navigate(WebinoRoute.SigningManager)
                                                },
                                                onOpenProjectsList = {
                                                    navController.navigate(WebinoRoute.Projects)
                                                },
                                                onOpenBuildHistory = {
                                                    navController.navigate(WebinoRoute.BuildHistory)
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.Projects> {
                                            ProjectsScreen(
                                                prefs = prefs,
                                                projects = projects,
                                                outputs = outputs,
                                                onCreateNew = {
                                                    viewModel.resetCreateForm(null)
                                                    navController.navigate(WebinoRoute.CreateProject(null))
                                                },
                                                onOpenStudio = { id ->
                                                    navController.navigate(WebinoRoute.ProjectStudio(id))
                                                },
                                                onOpenPreview = { id ->
                                                    navController.navigate(WebinoRoute.LivePreview(id))
                                                },
                                                onOpenBuild = { id ->
                                                    navController.navigate(WebinoRoute.BuildManager(id))
                                                },
                                                onDuplicate = { id -> viewModel.duplicateProject(id) },
                                                onDelete = { id -> viewModel.deleteProject(id) },
                                                onExportJson = { id -> viewModel.exportProjectJson(id) },
                                                onImportJson = { raw ->
                                                    viewModel.importProjectJson(raw) { importedId ->
                                                        navController.navigate(WebinoRoute.ProjectStudio(importedId))
                                                    }
                                                },
                                                onNotify = viewModel::postMessage,
                                            )
                                        }

                                        composable<WebinoRoute.Outputs> {
                                            OutputsScreen(
                                                prefs = prefs,
                                                outputs = outputs,
                                                onVerifyOutput = { viewModel.verifyOutput(it) },
                                                onDeleteOutput = { viewModel.deleteOutput(it) },
                                                onNotify = { viewModel.postMessage(it) },
                                            )
                                        }

                                        composable<WebinoRoute.Settings> {
                                            SettingsScreen(
                                                prefs = prefs,
                                                storageStats = storageStats,
                                                onLanguageChange = viewModel::setLanguage,
                                                onThemeChange = viewModel::setThemeMode,
                                                onFontChange = viewModel::setFontChoice,
                                                onCalendarChange = viewModel::setCalendarType,
                                                onPersianDigitsChange = viewModel::setPersianDigits,
                                                onClearCacheOnExitChange = viewModel::setClearCacheOnExit,
                                                onRedactLogsChange = viewModel::setRedactLogs,
                                                onClearBuildCache = viewModel::clearBuildCache,
                                                onOpenSigningManager = { navController.navigate(WebinoRoute.SigningManager) },
                                                onOpenHelpCenter = { navController.navigate(WebinoRoute.HelpCenter) },
                                                onOpenAbout = { navController.navigate(WebinoRoute.About) },
                                                onOpenBuildHistory = { navController.navigate(WebinoRoute.BuildHistory) },
                                                onReplayTutorial = { viewModel.setOnboardingCompleted(false) },
                                            )
                                        }

                                        composable<WebinoRoute.CreateProject> { backStack ->
                                            val route = backStack.toRoute<WebinoRoute.CreateProject>()
                                            LaunchedEffect(route.presetTemplateId) {
                                                if (route.presetTemplateId != null && createForm.urlInput.isBlank()) {
                                                    viewModel.resetCreateForm(route.presetTemplateId)
                                                }
                                            }
                                            CreateProjectScreen(
                                                form = createForm,
                                                recentWebsites = recentWebsites,
                                                existingProjects = projects,
                                                language = prefs.language,
                                                onBack = { navController.popBackStack() },
                                                onUpdateForm = viewModel::updateCreateForm,
                                                onInspectSite = viewModel::inspectSiteForCreateForm,
                                                onCreate = {
                                                    viewModel.createProjectFromForm { newId ->
                                                        navController.navigate(WebinoRoute.ProjectStudio(newId)) {
                                                            popUpTo(WebinoRoute.Dashboard)
                                                        }
                                                    }
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.ProjectStudio> { backStack ->
                                            val route = backStack.toRoute<WebinoRoute.ProjectStudio>()
                                            val bundle by viewModel.observeProject(route.projectId).collectAsStateWithLifecycle(null)
                                            ProjectStudioScreen(
                                                bundle = bundle,
                                                initialTab = route.initialTab,
                                                language = prefs.language,
                                                onBack = { navController.popBackStack() },
                                                onSave = viewModel::saveProjectChanges,
                                                onPickImage = viewModel::importProjectImage,
                                                onOpenPreview = { id ->
                                                    navController.navigate(WebinoRoute.LivePreview(id))
                                                },
                                                onOpenBuild = { id ->
                                                    navController.navigate(WebinoRoute.BuildManager(id))
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.LivePreview> { backStack ->
                                            val route = backStack.toRoute<WebinoRoute.LivePreview>()
                                            val bundle by viewModel.observeProject(route.projectId).collectAsStateWithLifecycle(null)
                                            LivePreviewScreen(
                                                bundle = bundle,
                                                clearCacheOnExit = prefs.clearPreviewCacheOnExit,
                                                onBack = { navController.popBackStack() },
                                            )
                                        }

                                        composable<WebinoRoute.BuildManager> { backStack ->
                                            val route = backStack.toRoute<WebinoRoute.BuildManager>()
                                            val bundle by viewModel.observeProject(route.projectId).collectAsStateWithLifecycle(null)
                                            val latestTask by viewModel.latestBuildTask.collectAsStateWithLifecycle()
                                            val activeTaskId = if (route.taskId > 0L) route.taskId else {
                                                latestTask?.takeIf { it.projectId == route.projectId }?.id ?: 0L
                                            }
                                            val task by viewModel.observeBuildTask(activeTaskId).collectAsStateWithLifecycle(null)
                                            val logs by viewModel.observeBuildLogs(activeTaskId).collectAsStateWithLifecycle(emptyList())

                                            BuildManagerScreen(
                                                bundle = bundle,
                                                allProjects = projects,
                                                task = task,
                                                logs = logs,
                                                signingKeys = signingKeys,
                                                envReport = envReport,
                                                prefs = prefs,
                                                showBackButton = route.projectId != 0L,
                                                onBack = { navController.popBackStack() },
                                                onSelectProject = { projId ->
                                                    navController.navigate(WebinoRoute.BuildManager(projId))
                                                },
                                                onCreateProject = {
                                                    viewModel.resetCreateForm(null)
                                                    navController.navigate(WebinoRoute.CreateProject(null))
                                                },
                                                onRecheckEngine = viewModel::recheckEnvironment,
                                                onStartBuild = { projId, fmt, keyId ->
                                                    viewModel.startBuild(projId, fmt, keyId) { createdTaskId ->
                                                        navController.navigate(WebinoRoute.BuildManager(projId, createdTaskId)) {
                                                            popUpTo(WebinoRoute.BuildManager(projId, route.taskId)) { inclusive = true }
                                                        }
                                                    }
                                                },
                                                onCancelBuild = viewModel::cancelBuild,
                                                onOpenOutputs = {
                                                    navController.navigate(WebinoRoute.Outputs) {
                                                        popUpTo(WebinoRoute.Dashboard)
                                                    }
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.SigningManager> {
                                            SigningManagerScreen(
                                                keys = signingKeys,
                                                onBack = { navController.popBackStack() },
                                                onCreateKey = viewModel::createSigningKey,
                                                onImportKey = viewModel::importSigningKey,
                                                onSetDefault = viewModel::setDefaultSigningKey,
                                                onChangePassword = viewModel::changeSigningPassword,
                                                onVerifyKey = viewModel::verifySigningKey,
                                                onDeleteKey = viewModel::deleteSigningKey,
                                                onNotify = viewModel::postMessage,
                                            )
                                        }

                                        composable<WebinoRoute.BuildHistory> {
                                            val allTasks by viewModel.allBuildTasks.collectAsStateWithLifecycle()
                                            BuildHistoryScreen(
                                                prefs = prefs,
                                                tasks = allTasks,
                                                projects = projects,
                                                observeLogs = viewModel::observeBuildHistoryLogs,
                                                loadReport = { id -> viewModel.buildSupportReport(id, isFa) },
                                                onBack = { navController.popBackStack() },
                                                onNotify = { msg ->
                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        snackbarHostState.showSnackbar(msg)
                                                    }
                                                },
                                            )
                                        }

                                        composable<WebinoRoute.HelpCenter> {
                                            HelpCenterScreen(onBack = { navController.popBackStack() })
                                        }

                                        composable<WebinoRoute.About> {
                                            AboutScreen(
                                                language = prefs.language,
                                                usePersianDigits = prefs.usePersianDigits,
                                                onBack = { navController.popBackStack() },
                                                onNotify = viewModel::postMessage,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (hasCompletedSplash && !prefs.onboardingCompleted) {
                            BackHandler(enabled = true) { viewModel.setOnboardingCompleted(true) }
                            OnboardingScreen(
                                language = prefs.language,
                                usePersianDigits = prefs.usePersianDigits,
                                onFinished = { viewModel.setOnboardingCompleted(true) },
                            )
                        }

                        if (showActiveBuildExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showActiveBuildExitDialog = false },
                                title = { Text(appString(R.string.exit_active_build_title)) },
                                text = { Text(appString(R.string.exit_active_build_msg)) },
                                confirmButton = {
                                    TextButton(onClick = { showActiveBuildExitDialog = false }) {
                                        Text(appString(R.string.exit_active_build_stay))
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showActiveBuildExitDialog = false
                                            (context as? Activity)?.finish()
                                        }
                                    ) {
                                        Text(appString(R.string.exit_active_build_leave), color = ErrorCoral)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
