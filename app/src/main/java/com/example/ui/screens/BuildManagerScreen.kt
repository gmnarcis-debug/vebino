package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import red.line.vebino.R
import com.example.core.ValidationUtils
import com.example.core.appString
import com.example.data.local.db.BuildLogEntity
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.SigningKeyMetadataEntity
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.BuildEngineStatus
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStatus
import com.example.domain.model.BuildStep
import com.example.domain.model.EnvironmentCheckReport
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.components.localizeNumber
import com.example.ui.components.statusColorFor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import kotlinx.coroutines.delay

@Composable
fun BuildManagerScreen(
    bundle: FullProjectBundle?,
    allProjects: List<FullProjectBundle> = emptyList(),
    task: BuildTaskEntity?,
    logs: List<BuildLogEntity>,
    signingKeys: List<SigningKeyMetadataEntity>,
    envReport: EnvironmentCheckReport,
    prefs: UserPreferences,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
    onSelectProject: (Long) -> Unit = {},
    onCreateProject: () -> Unit = {},
    onRecheckEngine: () -> Unit = {},
    onStartBuild: (Long, BuildOutputFormat, Long) -> Unit,
    onCancelBuild: (Long) -> Unit,
    onOpenOutputs: () -> Unit,
) {
    val isFa = prefs.language == AppLanguage.FA
    val usePersianDigits = prefs.usePersianDigits
    val tokens = WebinoThemeTokens.colors
    val engineStatusLabel = when (envReport.status) {
        BuildEngineStatus.NOT_CHECKED -> appString(R.string.engine_status_not_checked)
        BuildEngineStatus.CHECKING -> appString(R.string.engine_status_checking)
        BuildEngineStatus.READY -> appString(R.string.engine_status_ready)
        BuildEngineStatus.UNSUPPORTED -> appString(R.string.engine_status_unsupported)
        BuildEngineStatus.ERROR -> appString(R.string.engine_status_error)
    }
    val engineColor = when (envReport.status) {
        BuildEngineStatus.READY -> MintGreen
        BuildEngineStatus.CHECKING, BuildEngineStatus.NOT_CHECKED -> WarningAmber
        BuildEngineStatus.UNSUPPORTED, BuildEngineStatus.ERROR -> ErrorCoral
    }

    if (bundle == null) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                WebinoScreenHeader(
                    title = appString(R.string.build_manager_title),
                    subtitle = if (isFa) "کامپایلر محلی بدون نیاز به سرور" else "On-device APK & AAB compiler",
                    iconType = Webino3DIconType.BUILD,
                    onBack = if (showBackButton) onBack else null,
                    backContentDescription = appString(R.string.back_cd),
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    GlassCard(borderColor = engineColor.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appString(R.string.stat_build_engine), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(
                                    text = "$engineStatusLabel • ${localizeNumber(envReport.freeRamMb, prefs.language, usePersianDigits)}MB RAM • ${localizeNumber(envReport.freeStorageMb, prefs.language, usePersianDigits)}MB",
                                    color = engineColor,
                                )
                            }
                            Surface(
                                onClick = onRecheckEngine,
                                shape = CircleShape,
                                color = SkyBlue.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("build_recheck_btn"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = appString(R.string.engine_recheck), tint = SkyBlue)
                                }
                            }
                        }
                    }
                }

                if (allProjects.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Build,
                            icon3DType = Webino3DIconType.BUILD,
                            title = appString(R.string.empty_projects_title),
                            subtitle = appString(R.string.build_no_projects),
                            actionText = appString(R.string.hero_create_cta),
                            onAction = onCreateProject,
                        )
                    }
                } else {
                    item {
                        Text(
                            text = appString(R.string.build_select_project),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(allProjects, key = { it.project.id }) { projBundle ->
                        GlassCard(onClick = { onSelectProject(projBundle.project.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Webino3DGlassIcon(type = Webino3DIconType.PROJECTS, size = 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = projBundle.settings?.appName ?: projBundle.project.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "${projBundle.project.packageName} • v${projBundle.project.versionName}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                textDirection = TextDirection.Ltr,
                                            ),
                                            color = SkyBlue,
                                        )
                                    }
                                }
                                StatusBadge(
                                    text = appString(R.string.action_build),
                                    color = ElectricViolet,
                                    icon = Icons.Rounded.Build,
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    var selectedFormat by remember { mutableStateOf(BuildOutputFormat.APK) }
    var selectedKeyId by remember(signingKeys) {
        mutableLongStateOf(signingKeys.firstOrNull { it.isDefault }?.id ?: signingKeys.firstOrNull()?.id ?: 0L)
    }

    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isRunning = task != null && (task.status == BuildStatus.RUNNING.name || task.status == BuildStatus.QUEUED.name)
    LaunchedEffect(isRunning) {
        while (isRunning) {
            nowTick = System.currentTimeMillis()
            delay(500)
        }
    }

    val elapsedMs = if (task != null) {
        (task.finishedAt ?: nowTick) - task.startedAt
    } else {
        0L
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = bundle.settings?.appName ?: bundle.project.name,
                subtitle = appString(R.string.build_manager_title),
                iconType = Webino3DIconType.BUILD,
                onBack = if (showBackButton) onBack else null,
                backContentDescription = appString(R.string.back_cd),
                actions = {
                    IconButton(onClick = onRecheckEngine, modifier = Modifier.testTag("build_recheck_btn")) {
                        Icon(Icons.Rounded.Refresh, contentDescription = appString(R.string.engine_recheck), tint = SkyBlue)
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard {
                    Text(if (isFa) "پیکربندی خروجی و امضا" else "Output & Signing Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuildOutputFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { if (!isRunning) selectedFormat = fmt },
                                label = { Text(fmt.titleFa) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(if (isFa) "کلید امضای دیجیتال:" else "Signing Key:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        signingKeys.forEach { key ->
                            FilterChip(
                                selected = selectedKeyId == key.id,
                                onClick = { if (!isRunning) selectedKeyId = key.id },
                                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(key.alias) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isFa) {
                            "توجه: برای آپدیت نسخه‌های بعدی اپلیکیشن، استفاده از کلید امضای سازگار (همسان) الزامی است."
                        } else {
                            "Note: Updating an installed app requires signing future builds with the same compatible key."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    val estimatedNeedMb = if (selectedFormat == BuildOutputFormat.BOTH) 26L else 24L
                    Text(
                        text = if (isFa) {
                            "برآورد فضای موقت و خروجی: ~${localizeNumber(estimatedNeedMb, prefs.language, usePersianDigits)} مگابایت • فضای آزاد: ${localizeNumber(envReport.freeStorageMb, prefs.language, usePersianDigits)} مگابایت"
                        } else {
                            "Estimated space: ~${estimatedNeedMb}MB • Available: ${envReport.freeStorageMb}MB"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyBlue,
                    )
                    Spacer(Modifier.height(10.dp))
                    StatusBadge(
                        text = "$engineStatusLabel • ${localizeNumber(envReport.freeRamMb, prefs.language, usePersianDigits)}MB RAM • ${localizeNumber(envReport.freeStorageMb, prefs.language, usePersianDigits)}MB",
                        color = engineColor,
                    )
                    Spacer(Modifier.height(14.dp))
                    if (isRunning) {
                        OutlinedButton(
                            onClick = { task?.let { onCancelBuild(it.id) } },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ErrorCoral),
                        ) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = ErrorCoral)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isFa) "لغو امن عملیات ساخت" else "Safely Cancel Build", color = ErrorCoral)
                        }
                    } else {
                        GradientPrimaryButton(
                            text = if (isFa) "شروع کامپایل و ساخت واقعی روی گوشی" else "Start On-Device Compilation",
                            icon = Icons.Rounded.Build,
                            onClick = { onStartBuild(bundle.project.id, selectedFormat, selectedKeyId) },
                            enabled = envReport.isReady,
                            testTag = "build_start_btn",
                        )
                    }
                }
            }

            if (task != null) {
                val stepEnum = runCatching { BuildStep.valueOf(task.currentStep) }.getOrDefault(BuildStep.CHECKING_ENV)
                val statusColor = statusColorFor(task.status)

                item {
                    GlassCard(borderColor = statusColor.copy(alpha = 0.55f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusBadge(
                                text = runCatching { BuildStatus.valueOf(task.status).titleFa }.getOrDefault(task.status),
                                color = statusColor,
                            )
                            Text(
                                text = "${localizeNumber(task.progressPercent, prefs.language, usePersianDigits)}٪",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(50)),
                            color = statusColor,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "مرحله: ${stepEnum.titleFa}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "زمان: ${ValidationUtils.formatDuration(elapsedMs, isFa, usePersianDigits)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        task.errorMessage?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = err, color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                        }

                        if (task.status == BuildStatus.SUCCEEDED.name) {
                            Spacer(Modifier.height(12.dp))
                            GradientPrimaryButton(
                                text = if (isFa) "مشاهده و نصب خروجی‌ها" else "View & Install Outputs",
                                icon = Icons.Rounded.Android,
                                onClick = onOpenOutputs,
                                testTag = "build_view_outputs_btn",
                            )
                        } else if (task.status == BuildStatus.FAILED.name || task.status == BuildStatus.CANCELLED.name) {
                            Spacer(Modifier.height(12.dp))
                            val retryFormat = runCatching { BuildOutputFormat.valueOf(task.outputFormat) }.getOrDefault(selectedFormat)
                            val retryKeyId = task.signingKeyId.takeIf { it > 0L } ?: selectedKeyId
                            OutlinedButton(
                                onClick = { onStartBuild(bundle.project.id, retryFormat, retryKeyId) },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("build_retry_btn"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SkyBlue),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = SkyBlue)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isFa) "تلاش مجدد برای ساخت" else "Retry Build", color = SkyBlue)
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        Text("مراحل ۸گانه ساخت واقعی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        BuildStep.entries.forEach { step ->
                            val reached = stepEnum.order > step.order || task.status == BuildStatus.SUCCEEDED.name
                            val active = stepEnum == step && isRunning
                            val dotColor = when {
                                reached -> SuccessEmerald
                                active -> ElectricViolet
                                task.status == BuildStatus.FAILED.name && stepEnum == step -> ErrorCoral
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(dotColor.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (reached) Icons.Rounded.CheckCircle else if (task.status == BuildStatus.FAILED.name && stepEnum == step) Icons.Rounded.Error else Icons.Rounded.Build,
                                        contentDescription = null,
                                        tint = dotColor,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${localizeNumber(step.order, prefs.language, usePersianDigits)}. ${step.titleFa}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (active || reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = tokens.terminalContainer,
                        border = BorderStroke(1.dp, tokens.terminalBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Terminal, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("گزارش زنده Build", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            if (logs.isEmpty()) {
                                Text("در انتظار ثبت گزارش...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                logs.forEach { entry ->
                                    val logColor = when (entry.level) {
                                        "ERROR" -> ErrorCoral
                                        "WARN" -> WarningAmber
                                        else -> CyanAccent
                                    }
                                    Text(
                                        text = "[${entry.stepName}] ${entry.message}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = logColor,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
