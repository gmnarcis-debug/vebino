package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Https
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import red.line.vebino.R
import com.example.core.ValidationUtils
import com.example.core.appString
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.RecentWebsiteEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import com.example.ui.viewmodel.CreateProjectFormState

@Composable
fun CreateProjectScreen(
    form: CreateProjectFormState,
    recentWebsites: List<RecentWebsiteEntity>,
    existingProjects: List<FullProjectBundle> = emptyList(),
    language: AppLanguage,
    onBack: () -> Unit,
    onUpdateForm: ((CreateProjectFormState) -> CreateProjectFormState) -> Unit,
    onInspectSite: () -> Unit,
    onCreate: () -> Unit,
) {
    val isFa = language == AppLanguage.FA
    val tokens = WebinoThemeTokens.colors
    val normalized = ValidationUtils.normalizeUrl(form.urlInput)
    val urlValid = ValidationUtils.isValidWebUrl(normalized)
    val isHttps = ValidationUtils.isHttpsUrl(normalized)
    val isIr = ValidationUtils.isIranDomain(normalized)
    val pkgTrimmed = form.packageName.trim()
    val pkgValid = pkgTrimmed.isBlank() || ValidationUtils.isValidPackageName(pkgTrimmed)
    val duplicateOwner = remember(pkgTrimmed, existingProjects) {
        if (pkgTrimmed.isBlank()) null
        else existingProjects.firstOrNull { it.project.packageName.equals(pkgTrimmed, ignoreCase = true) }?.project?.name
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = appString(R.string.create_title),
                subtitle = if (isFa) "۳ گام ساده تا ساخت اپلیکیشن مستقل" else "3 steps to build your standalone app",
                iconType = Webino3DIconType.BUILD,
                onBack = onBack,
                backContentDescription = appString(R.string.back_cd),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                GlassCard(borderColor = ElectricViolet.copy(alpha = 0.25f)) {
                    StepHeaderBadge(number = "۱", title = appString(R.string.create_step_url), accent = ElectricViolet)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.urlInput,
                        onValueChange = { next ->
                            onUpdateForm { it.copy(urlInput = next, errorMessage = null) }
                        },
                        label = { Text(appString(R.string.create_url_label)) },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null, tint = ElectricViolet) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            textDirection = TextDirection.Ltr,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_url_input"),
                    )

                    if (form.urlInput.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(
                                text = when {
                                    isHttps -> appString(R.string.create_https_safe)
                                    urlValid -> appString(R.string.create_http_unsafe)
                                    else -> appString(R.string.create_url_invalid)
                                },
                                color = if (isHttps) SuccessEmerald else if (urlValid) WarningAmber else ErrorCoral,
                                icon = if (isHttps) Icons.Rounded.Https else Icons.Rounded.Warning,
                            )
                            if (isIr) {
                                StatusBadge(
                                    text = appString(R.string.create_ir_domain),
                                    color = SkyBlue,
                                    icon = Icons.Rounded.Public,
                                )
                            }
                        }

                        if (urlValid && !isHttps) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = if (isFa) "اجازه موقت برای آدرس غیرامن HTTP" else "Allow unsafe HTTP temporarily",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = form.allowInsecureHttp,
                                    onCheckedChange = { allowed ->
                                        onUpdateForm { it.copy(allowInsecureHttp = allowed, errorMessage = null) }
                                    },
                                    modifier = Modifier.testTag("create_allow_http_switch"),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onInspectSite,
                        enabled = !form.isInspecting && form.urlInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("inspect_site_btn"),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.45f)),
                    ) {
                        if (form.isInspecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(appString(R.string.create_inspecting))
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = SkyBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(appString(R.string.create_inspect_btn))
                        }
                    }

                    form.inspectedSite?.let { site ->
                        if (site.isReachable) {
                            Spacer(Modifier.height(10.dp))
                            StatusBadge(
                                text = "HTTP ${site.httpStatusCode} • ${site.title.take(24)}",
                                color = SuccessEmerald,
                                icon = Icons.Rounded.CheckCircle,
                            )
                        }
                    }
                }
            }

            if (recentWebsites.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = if (isFa) "سایت‌های بررسی‌شده اخیر" else "Recent Websites",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentWebsites) { site ->
                                AssistChip(
                                    onClick = {
                                        onUpdateForm {
                                            it.copy(
                                                urlInput = site.url,
                                                appName = it.appName.ifBlank { site.title.take(28) },
                                                packageName = it.packageName.ifBlank {
                                                    ValidationUtils.suggestPackageName(site.url, site.title)
                                                },
                                            )
                                        }
                                    },
                                    label = { Text(site.title.ifBlank { site.url }.take(22)) },
                                )
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    StepHeaderBadge(number = "۲", title = appString(R.string.create_step_template), accent = SkyBlue)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AppTemplateType.entries) { template ->
                            val selected = form.selectedTemplate == template
                            val primary = runCatching { Color(android.graphics.Color.parseColor(template.primaryHex)) }
                                .getOrDefault(ElectricViolet)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) primary.copy(alpha = 0.16f) else tokens.subtleSurface,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) primary else tokens.navPillBorder,
                                ),
                                modifier = Modifier
                                    .width(148.dp)
                                    .clickable { onUpdateForm { it.copy(selectedTemplate = template) } },
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(primary, CircleShape),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (isFa) template.titleFa else template.titleEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = template.subtitleFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    StepHeaderBadge(number = "۳", title = appString(R.string.create_step_identity), accent = CyanAccent)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.appName,
                        onValueChange = { v -> onUpdateForm { it.copy(appName = v, errorMessage = null) } },
                        label = { Text(appString(R.string.create_app_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_app_name_input"),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.packageName,
                        onValueChange = { v -> onUpdateForm { it.copy(packageName = v, errorMessage = null) } },
                        label = { Text(appString(R.string.create_package_name)) },
                        placeholder = { Text("ir.mysite.app") },
                        isError = !pkgValid,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            textDirection = TextDirection.Ltr,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val suggested = ValidationUtils.suggestPackageName(form.urlInput, form.appName)
                                    onUpdateForm { it.copy(packageName = suggested) }
                                }
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = "پیشنهاد خودکار", tint = ElectricViolet)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_package_input"),
                    )
                    if (duplicateOwner != null) {
                        Spacer(Modifier.height(8.dp))
                        StatusBadge(
                            text = appString(R.string.create_duplicate_pkg_warn, duplicateOwner),
                            color = WarningAmber,
                            icon = Icons.Rounded.Warning,
                            modifier = Modifier.testTag("create_duplicate_pkg_warning"),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.versionName,
                            onValueChange = { v -> onUpdateForm { it.copy(versionName = v) } },
                            label = { Text(appString(R.string.create_version_name)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.versionCode,
                            onValueChange = { v -> onUpdateForm { it.copy(versionCode = v.filter(Char::isDigit)) } },
                            label = { Text(appString(R.string.create_version_code)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.description,
                        onValueChange = { v -> onUpdateForm { it.copy(description = v) } },
                        label = { Text(appString(R.string.create_description)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            form.errorMessage?.let { err ->
                item {
                    GlassCard(
                        borderColor = ErrorCoral,
                        modifier = Modifier.testTag("create_error_card"),
                    ) {
                        Text(text = err, color = ErrorCoral, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                GradientPrimaryButton(
                    text = appString(R.string.create_submit_btn),
                    icon = Icons.Rounded.RocketLaunch,
                    onClick = onCreate,
                    testTag = "create_submit_btn",
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StepHeaderBadge(number: String, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
