package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.ExternalIntentHelper
import com.example.core.JalaliCalendar
import com.example.data.local.db.BuildLogEntity
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.BuildStatus
import com.example.domain.model.CalendarType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.components.WebinoSecondaryButton
import com.example.ui.components.localizeNumber
import com.example.ui.components.softEnter
import com.example.ui.components.statusColorFor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BuildHistoryScreen(
    prefs: UserPreferences,
    tasks: List<BuildTaskEntity>,
    projects: List<FullProjectBundle>,
    observeLogs: (Long) -> Flow<List<BuildLogEntity>>,
    loadReport: suspend (Long) -> String,
    onBack: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val isFa = prefs.language == AppLanguage.FA
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val nameById = remember(projects) { projects.associate { it.project.id to it.project.name } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = if (isFa) "تاریخچه‌ی ساخت" else "Build history",
                subtitle = if (isFa) "لاگ هر ساخت را ببینید و برای پشتیبانی بفرستید" else "Review logs and send them to support",
                iconType = Webino3DIconType.HISTORY,
                onBack = onBack,
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Rounded.Share,
                    title = if (isFa) "هنوز ساختی انجام نشده" else "No builds yet",
                    subtitle = if (isFa) "بعد از اولین ساخت، لاگ آن اینجا نمایش داده می‌شود." else "Logs of your builds will appear here.",
                    icon3DType = Webino3DIconType.HISTORY,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    val expanded = expandedId == task.id
                    HistoryTaskCard(
                        task = task,
                        projectName = nameById[task.projectId] ?: (if (isFa) "پروژه‌ی حذف‌شده" else "Deleted project"),
                        prefs = prefs,
                        expanded = expanded,
                        onToggle = { expandedId = if (expanded) null else task.id },
                        observeLogs = observeLogs,
                        onShare = {
                            scope.launch {
                                val text = loadReport(task.id)
                                if (text.isBlank()) onNotify(if (isFa) "گزارشی برای این ساخت پیدا نشد." else "No report found.")
                                else shareReportFile(context, task.id, text, isFa) { onNotify(it) }
                            }
                        },
                        onCopy = {
                            scope.launch {
                                val text = loadReport(task.id)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("webino-build-log", text))
                                onNotify(if (isFa) "لاگ کپی شد." else "Log copied.")
                            }
                        },
                        onEmail = {
                            scope.launch {
                                val text = loadReport(task.id)
                                val intent = ExternalIntentHelper.buildSupportEmailIntent(
                                    "WEBINO build #${task.id} (${task.status})",
                                ).apply {
                                    // mailto: bodies have a size limit; send the head and keep the full log shareable as a file.
                                    putExtra(Intent.EXTRA_TEXT, text.take(8000))
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                                    .onFailure { onNotify(if (isFa) "برنامه‌ی ایمیل پیدا نشد." else "No email app found.") }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTaskCard(
    task: BuildTaskEntity,
    projectName: String,
    prefs: UserPreferences,
    expanded: Boolean,
    onToggle: () -> Unit,
    observeLogs: (Long) -> Flow<List<BuildLogEntity>>,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onEmail: () -> Unit,
) {
    val isFa = prefs.language == AppLanguage.FA
    val status = runCatching { BuildStatus.valueOf(task.status) }.getOrNull()
    val statusText = if (isFa) (status?.titleFa ?: task.status) else (status?.name ?: task.status)
    val whenText = JalaliCalendar.formatDateTime(
        millis = task.startedAt,
        useJalali = prefs.calendarType == CalendarType.JALALI,
        isPersian = isFa,
        usePersianDigits = prefs.usePersianDigits,
    )
    GlassCard(
        onClick = onToggle,
        borderColor = statusColorFor(task.status).copy(alpha = 0.35f),
        modifier = Modifier.testTag("history_task_${task.id}"),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${task.outputFormat} • $whenText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(text = statusText, color = statusColorFor(task.status))
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!task.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = task.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorCoral,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(visible = expanded) {
            val logs by remember(task.id) { observeLogs(task.id) }.collectAsState(initial = emptyList())
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = if (isFa) {
                        "لاگ (${localizeNumber(logs.size, prefs.language, prefs.usePersianDigits)} خط)"
                    } else {
                        "Log (${logs.size} lines)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                GlassCard(contentPadding = PaddingValues(10.dp)) {
                    if (logs.isEmpty()) {
                        Text(
                            text = if (isFa) "لاگی ثبت نشده است." else "No log lines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Show the last lines inline; the shared report always contains the full log.
                        logs.takeLast(40).forEach { l ->
                            Text(
                                text = "[${l.level}] ${l.message}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = when (l.level) {
                                    "ERROR" -> ErrorCoral
                                    "WARN" -> WarningAmber
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    WebinoSecondaryButton(
                        text = if (isFa) "اشتراک‌گذاری" else "Share",
                        icon = Icons.Rounded.Share,
                        onClick = onShare,
                        modifier = Modifier.weight(1f).testTag("history_share_${task.id}"),
                        accent = CyanAccent,
                    )
                    WebinoSecondaryButton(
                        text = if (isFa) "کپی" else "Copy",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = onCopy,
                        modifier = Modifier.weight(1f),
                    )
                    WebinoSecondaryButton(
                        text = if (isFa) "پشتیبانی" else "Support",
                        icon = Icons.Rounded.Email,
                        onClick = onEmail,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun shareReportFile(
    context: Context,
    taskId: Long,
    text: String,
    isFa: Boolean,
    notify: (String) -> Unit,
) {
    runCatching {
        val dir = File(context.cacheDir, "shared_logs").apply { mkdirs() }
        val file = File(dir, "webino-build-$taskId.txt")
        file.writeText(text, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WEBINO build #$taskId")
            clipData = ClipData.newRawUri("webino-build-log", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, if (isFa) "ارسال لاگ ساخت" else "Share build log"))
    }.onFailure {
        notify(if (isFa) "اشتراک‌گذاری لاگ ممکن نشد." else "Could not share the log.")
    }
}
