package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import red.line.vebino.R
import com.example.core.JalaliCalendar
import com.example.core.appString
import com.example.data.local.db.BuildResultEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.prefs.UserPreferences
import com.example.data.repository.ProjectHistoryEntry
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.domain.model.CalendarType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.localizeNumber
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WebinoThemeTokens
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

private enum class ProjectSortMode(val titleRes: Int) {
    RECENT(R.string.projects_sort_recent),
    NAME(R.string.projects_sort_name),
    VERSION(R.string.projects_sort_version),
}

@Composable
fun ProjectsScreen(
    prefs: UserPreferences,
    projects: List<FullProjectBundle>,
    outputs: List<BuildResultEntity> = emptyList(),
    onCreateNew: () -> Unit,
    onOpenStudio: (Long) -> Unit,
    onOpenPreview: (Long) -> Unit,
    onOpenBuild: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onExportJson: suspend (Long) -> String? = { null },
    onImportJson: (String) -> Unit = {},
    onNotify: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFa = prefs.language == AppLanguage.FA
    val tokens = WebinoThemeTokens.colors
    var query by remember { mutableStateOf("") }
    var selectedTemplateFilter by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(ProjectSortMode.RECENT) }
    var deleteCandidate by remember { mutableStateOf<FullProjectBundle?>(null) }
    var historyCandidate by remember { mutableStateOf<FullProjectBundle?>(null) }
    var pendingExportProjectId by remember { mutableStateOf<Long?>(null) }
    val json = remember { Json { ignoreUnknownKeys = true } }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: error("Empty file")
            }.onSuccess { raw ->
                onImportJson(raw)
            }.onFailure {
                onNotify("خطا در خواندن فایل JSON.")
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { destUri ->
        val projId = pendingExportProjectId
        pendingExportProjectId = null
        if (destUri != null && projId != null) {
            scope.launch {
                val payload = onExportJson(projId)
                if (payload == null) {
                    onNotify("پروژه یافت نشد.")
                } else {
                    runCatching {
                        context.contentResolver.openOutputStream(destUri)?.use { out ->
                            out.write(payload.toByteArray(Charsets.UTF_8))
                        }
                    }.onSuccess {
                        onNotify("تنظیمات پروژه با فرمت JSON ذخیره شد.")
                    }.onFailure {
                        onNotify("خطا در ذخیره فایل JSON.")
                    }
                }
            }
        }
    }

    val filtered = remember(projects, query, selectedTemplateFilter, sortMode) {
        projects
            .filter { b ->
                val matchesQuery = query.isBlank() ||
                    b.project.name.contains(query, ignoreCase = true) ||
                    b.project.websiteUrl.contains(query, ignoreCase = true) ||
                    b.project.packageName.contains(query, ignoreCase = true)
                val matchesTemplate = selectedTemplateFilter == null || b.project.templateId == selectedTemplateFilter
                matchesQuery && matchesTemplate
            }
            .let { list ->
                when (sortMode) {
                    ProjectSortMode.RECENT -> list.sortedByDescending { it.project.updatedAt }
                    ProjectSortMode.NAME -> list.sortedBy { it.project.name }
                    ProjectSortMode.VERSION -> list.sortedByDescending { it.project.versionCode }
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = ElectricViolet,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier.testTag("projects_fab_create"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = appString(R.string.projects_new_btn))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Webino3DGlassIcon(type = Webino3DIconType.PROJECTS, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = appString(R.string.projects_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (isFa) {
                                    "${localizeNumber(projects.size, prefs.language, prefs.usePersianDigits)} پروژه ثبت‌شده"
                                } else {
                                    "${projects.size} saved projects"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, tokens.navPillBorder),
                        modifier = Modifier.testTag("projects_import_json_btn"),
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(appString(R.string.projects_import_json))
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(appString(R.string.projects_search_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricViolet) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.glassContainerStrong,
                        unfocusedContainerColor = tokens.glassContainer,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = tokens.navPillBorder,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("projects_search_input"),
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProjectSortMode.entries) { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(appString(mode.titleRes)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTemplateFilter == null,
                            onClick = { selectedTemplateFilter = null },
                            label = { Text(appString(R.string.projects_all_templates)) },
                        )
                    }
                    items(AppTemplateType.entries) { tmpl ->
                        FilterChip(
                            selected = selectedTemplateFilter == tmpl.id,
                            onClick = { selectedTemplateFilter = if (selectedTemplateFilter == tmpl.id) null else tmpl.id },
                            label = { Text(if (isFa) tmpl.titleFa else tmpl.titleEn) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.FolderOff,
                        icon3DType = Webino3DIconType.PROJECTS,
                        title = appString(R.string.projects_empty_title),
                        subtitle = appString(R.string.projects_empty_sub),
                        actionText = appString(R.string.projects_new_btn),
                        onAction = onCreateNew,
                    )
                }
            } else {
                items(filtered, key = { it.project.id }) { bundle ->
                    val primary = runCatching {
                        Color(android.graphics.Color.parseColor(bundle.settings?.primaryColorHex ?: "#7C3AED"))
                    }.getOrDefault(ElectricViolet)
                    val projectOutputCount = outputs.count { it.projectId == bundle.project.id }
                    val lastEditedText = JalaliCalendar.formatDateTime(
                        millis = bundle.project.updatedAt,
                        useJalali = prefs.calendarType == CalendarType.JALALI,
                        isPersian = isFa,
                        usePersianDigits = prefs.usePersianDigits,
                    )

                    GlassCard(
                        onClick = { onOpenStudio(bundle.project.id) },
                        borderColor = primary.copy(alpha = 0.25f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconPath = bundle.settings?.iconFilePath
                            val iconFile = iconPath?.let { File(it) }?.takeIf { it.exists() }
                            if (iconFile != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(iconFile),
                                    contentDescription = bundle.project.name,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(15.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            Brush.linearGradient(listOf(primary.copy(alpha = 0.22f), SkyBlue.copy(alpha = 0.16f))),
                                            RoundedCornerShape(15.dp),
                                        )
                                        .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, tint = primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bundle.settings?.appName ?: bundle.project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = bundle.project.packageName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        textDirection = TextDirection.Ltr,
                                    ),
                                    color = SkyBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = bundle.project.websiteUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(
                                    text = "v${bundle.project.versionName}",
                                    color = SkyBlue,
                                )
                                StatusBadge(
                                    text = if (projectOutputCount > 0) {
                                        appString(R.string.projects_status_built, projectOutputCount)
                                    } else {
                                        appString(R.string.projects_status_ready)
                                    },
                                    color = if (projectOutputCount > 0) SuccessEmerald else ElectricViolet,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${appString(R.string.projects_last_updated)} $lastEditedText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = tokens.subtleSurface,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                IconButton(onClick = { onOpenStudio(bundle.project.id) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = ElectricViolet)
                                }
                                IconButton(onClick = { onOpenPreview(bundle.project.id) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = appString(R.string.action_preview), tint = CyanAccent)
                                }
                                IconButton(onClick = { onOpenBuild(bundle.project.id) }) {
                                    Icon(Icons.Rounded.Build, contentDescription = appString(R.string.action_build), tint = SkyBlue)
                                }
                                IconButton(onClick = { onDuplicate(bundle.project.id) }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی")
                                }
                                IconButton(
                                    onClick = {
                                        pendingExportProjectId = bundle.project.id
                                        exportJsonLauncher.launch("${bundle.project.packageName}_webino.json")
                                    }
                                ) {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = appString(R.string.projects_export_json))
                                }
                                IconButton(onClick = { historyCandidate = bundle }) {
                                    Icon(Icons.Rounded.History, contentDescription = "تاریخچه")
                                }
                                IconButton(onClick = { deleteCandidate = bundle }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = ErrorCoral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(appString(R.string.projects_delete_title)) },
            text = { Text(appString(R.string.projects_delete_confirm, candidate.project.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(candidate.project.id)
                        deleteCandidate = null
                    }
                ) {
                    Text(appString(R.string.action_delete_final), color = ErrorCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(appString(R.string.action_cancel)) }
            },
        )
    }

    historyCandidate?.let { candidate ->
        val entries = runCatching {
            json.decodeFromString<List<ProjectHistoryEntry>>(candidate.project.changeHistoryJson)
        }.getOrDefault(emptyList())
        AlertDialog(
            onDismissRequest = { historyCandidate = null },
            title = { Text("${candidate.project.name} • تاریخچه تغییرات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entries.isEmpty()) {
                        Text("تاریخچه‌ای ثبت نشده است.")
                    } else {
                        entries.take(8).forEach { entry ->
                            val date = JalaliCalendar.formatDateTime(
                                millis = entry.timestamp,
                                useJalali = prefs.calendarType == CalendarType.JALALI,
                                isPersian = isFa,
                                usePersianDigits = prefs.usePersianDigits,
                            )
                            Text("• ${entry.summary}\n  $date", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyCandidate = null }) { Text(appString(R.string.action_close)) }
            },
        )
    }
}
