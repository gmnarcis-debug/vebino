package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.db.ProjectEntity
import com.example.data.local.db.ProjectSettingsEntity
import com.example.data.local.db.WebSettingsEntity
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.CacheModeOption
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.LoadingAnimationType
import com.example.domain.model.MixedContentOption
import com.example.domain.model.OrientationMode
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SslPolicy
import com.example.domain.model.ThemePreset
import com.example.domain.model.ToolbarStyle
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.SettingToggleRow
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import com.example.webviewengine.WebViewConfiguration
import java.io.File

@Composable
fun ProjectStudioScreen(
    bundle: FullProjectBundle?,
    initialTab: Int,
    language: AppLanguage,
    onBack: () -> Unit,
    onSave: (ProjectEntity, ProjectSettingsEntity, WebSettingsEntity, String) -> Unit,
    onPickImage: (Long, Uri, Boolean, (String) -> Unit) -> Unit,
    onOpenPreview: (Long) -> Unit,
    onOpenBuild: (Long) -> Unit,
) {
    if (bundle == null) return
    val context = LocalContext.current
    val webCapabilities = remember(context) { WebViewConfiguration.inspectCapabilities(context) }
    val isFa = language == AppLanguage.FA
    val tokens = WebinoThemeTokens.colors
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    var project by remember(bundle.project.id) { mutableStateOf(bundle.project) }
    var settings by remember(bundle.project.id) {
        mutableStateOf(bundle.settings ?: ProjectSettingsEntity(projectId = bundle.project.id, appName = bundle.project.name))
    }
    var web by remember(bundle.project.id) {
        mutableStateOf(bundle.webSettings ?: WebSettingsEntity(projectId = bundle.project.id))
    }
    var showAdvancedWeb by remember { mutableStateOf(false) }

    LaunchedEffect(bundle) {
        project = bundle.project
        bundle.settings?.let { settings = it }
        bundle.webSettings?.let { web = it }
    }

    // استفاده از GetContent به جای PickVisualMedia برای اطمینان از باز شدن گالری محلی در همه گوشی‌ها
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onPickImage(project.id, uri, false) { savedPath ->
                settings = settings.copy(iconFilePath = savedPath)
                onSave(project, settings.copy(iconFilePath = savedPath), web, "بروزرسانی آیکون برنامه")
            }
        }
    }

    val splashPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onPickImage(project.id, uri, true) { savedPath ->
                settings = settings.copy(splashImagePath = savedPath)
                onSave(project, settings.copy(splashImagePath = savedPath), web, "بروزرسانی تصویر Splash")
            }
        }
    }

    val tabs = listOf(
        if (isFa) "هویت برنامه" else "Identity",
        if (isFa) "شخصی‌سازی" else "Style",
        if (isFa) "تنظیمات WebView" else "WebView",
        if (isFa) "امکانات پیشرفته" else "Advanced",
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = settings.appName.ifBlank { project.name },
                subtitle = project.packageName,
                iconType = Webino3DIconType.CUSTOMIZE,
                onBack = onBack,
                actions = {
                    Surface(
                        onClick = { onOpenPreview(project.id) },
                        shape = CircleShape,
                        color = SkyBlue.copy(alpha = 0.14f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "پیش‌نمایش زنده", tint = SkyBlue)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        onClick = {
                            onSave(project, settings, web, "بروزرسانی تنظیمات استودیو")
                        },
                        shape = CircleShape,
                        color = ElectricViolet.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("studio_save_btn"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Save, contentDescription = "ذخیره", tint = ElectricViolet)
                        }
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Modern Pill Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        onClick = { selectedTab = index },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) ElectricViolet else tokens.glassContainerStrong,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ElectricViolet else tokens.navPillBorder,
                        ),
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).testTag("studio_tab_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            GlassCard {
                                Text("هویت و نامگذاری اپلیکیشن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = settings.appName,
                                    onValueChange = { v ->
                                        settings = settings.copy(appName = v)
                                        project = project.copy(name = v)
                                    },
                                    label = { Text("نام اپلیکیشن") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = project.packageName,
                                    onValueChange = { v -> project = project.copy(packageName = v) },
                                    label = { Text(if (isFa) "نام بسته (Package Name)" else "Package Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        textDirection = TextDirection.Ltr,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = project.versionName,
                                        onValueChange = { v -> project = project.copy(versionName = v) },
                                        label = { Text(if (isFa) "نام نسخه" else "Version Name") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = project.versionCode.toString(),
                                        onValueChange = { v ->
                                            project = project.copy(versionCode = v.filter(Char::isDigit).toIntOrNull() ?: 1)
                                        },
                                        label = { Text(if (isFa) "کد نسخه" else "Version Code") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = settings.developerName,
                                    onValueChange = { v -> settings = settings.copy(developerName = v) },
                                    label = { Text(if (isFa) "نام توسعه‌دهنده / برند" else "Developer / Brand") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        item {
                            GlassCard {
                                Text("آیکون و تصویر Splash (با برش مربعی)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    settings.iconFilePath?.let { path ->
                                        if (File(path).exists()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(File(path)),
                                                contentDescription = "آیکون برنامه",
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(14.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            // تغییر به GetContent با نوع image/*
                                            iconPicker.launch("image/*")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(48.dp),
                                    ) {
                                        Icon(Icons.Rounded.Image, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("انتخاب آیکون از گالری")
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    settings.splashImagePath?.let { path ->
                                        if (File(path).exists()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(File(path)),
                                                contentDescription = "تصویر Splash",
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(14.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            // تغییر به GetContent با نوع image/*
                                            splashPicker.launch("image/*")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(48.dp),
                                    ) {
                                        Icon(Icons.Rounded.Image, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("انتخاب تصویر Splash")
                                    }
                                }
                            }
                        }

                        item {
                            GlassCard {
                                Text("رنگ اصلی برنامه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ColorSwatchRow(
                                    selectedHex = settings.primaryColorHex,
                                    onSelectHex = { settings = settings.copy(primaryColorHex = it) },
                                )
                                Spacer(Modifier.height(14.dp))
                                Text("رنگ پس‌زمینه برنامه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ColorSwatchRow(
                                    selectedHex = settings.backgroundColorHex,
                                    onSelectHex = { settings = settings.copy(backgroundColorHex = it) },
                                    presets = listOf("#0B1026", "#0F172A", "#111827", "#FFFFFF", "#F8FAFC", "#1E1B4B", "#061E1A", "#1F122B"),
                                )
                            }
                        }
                    }

                    1 -> {
                        // Visual Live Color & Toolbar Mini Preview Card
                        item {
                            val primaryPreview = runCatching {
                                Color(android.graphics.Color.parseColor(settings.primaryColorHex))
                            }.getOrDefault(ElectricViolet)
                            val bgPreview = runCatching {
                                Color(android.graphics.Color.parseColor(settings.backgroundColorHex))
                            }.getOrDefault(Color.White)
                            GlassCard(borderColor = primaryPreview.copy(alpha = 0.4f)) {
                                Text(
                                    text = if (isFa) "پیش‌نمایش ظاهری هدر و رنگ‌بندی اپ مقصد" else "Target App Header Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = bgPreview,
                                    border = BorderStroke(1.dp, primaryPreview.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        if (settings.toolbarStyle != ToolbarStyle.NONE.name) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(primaryPreview)
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                            ) {
                                                Text(
                                                    text = settings.appName.ifBlank { project.name },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                        ) {
                                            Text(
                                                text = project.websiteUrl,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    textDirection = TextDirection.Ltr,
                                                ),
                                                color = primaryPreview,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            GlassCard {
                                Text("تم و ظاهر اپلیکیشن مقصد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ThemePreset.entries.forEach { preset ->
                                        FilterChip(
                                            selected = settings.themePreset == preset.name,
                                            onClick = { settings = settings.copy(themePreset = preset.name) },
                                            label = { Text(preset.titleFa) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("فونت پیش‌فرض", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppFontChoice.entries.forEach { f ->
                                        val labelText = when (f) {
                                            AppFontChoice.VAZIRMATN -> if (isFa) "وزیرمتن" else "Vazirmatn"
                                            AppFontChoice.LALEZAR -> if (isFa) "لاله‌زار" else "Lalezar"
                                            AppFontChoice.NOTO_NASKH -> if (isFa) "نسخ خوانا" else "Noto Naskh"
                                        }
                                        FilterChip(
                                            selected = settings.fontName == f.name,
                                            onClick = { settings = settings.copy(fontName = f.name) },
                                            label = { Text(labelText) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("نوار ابزار برنامه", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ToolbarStyle.entries.forEach { tb ->
                                        FilterChip(
                                            selected = settings.toolbarStyle == tb.name,
                                            onClick = { settings = settings.copy(toolbarStyle = tb.name) },
                                            label = { Text(tb.titleFa) },
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            GlassCard {
                                Text("صفحه شروع، Splash و چرخش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = settings.startPagePath,
                                    onValueChange = { settings = settings.copy(startPagePath = it) },
                                    label = { Text("مسیر صفحه شروع (اختیاری، مثلاً /shop)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("انیمیشن بارگذاری", style = MaterialTheme.typography.titleSmall)
                                LoadingAnimationType.entries.forEach { anim ->
                                    FilterChip(
                                        selected = settings.loadingAnimation == anim.name,
                                        onClick = { settings = settings.copy(loadingAnimation = anim.name) },
                                        label = { Text(anim.titleFa) },
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                SettingToggleRow(
                                    title = "نمایش صفحه Splash هنگام ورود",
                                    description = "لوگو و نام برنامه در ابتدای باز شدن اپ نمایش داده شود.",
                                    checked = settings.splashEnabled,
                                    onCheckedChange = { settings = settings.copy(splashEnabled = it) },
                                )
                                if (settings.splashEnabled) {
                                    Text("مدت نمایش Splash: ${settings.splashDurationMs} میلی‌ثانیه", style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = settings.splashDurationMs.toFloat(),
                                        onValueChange = { settings = settings.copy(splashDurationMs = it.toInt()) },
                                        valueRange = 500f..3500f,
                                    )
                                }
                                SettingToggleRow(
                                    title = "حالت تمام‌صفحه (Fullscreen)",
                                    description = "مخفی کردن نوار وضعیت و ناوبری گوشی داخل اپ مقصد",
                                    checked = settings.fullscreenMode,
                                    onCheckedChange = { settings = settings.copy(fullscreenMode = it) },
                                )
                                SettingToggleRow(
                                    title = "پشتیبانی کامل راست‌چین (RTL)",
                                    description = "چیدمان استاندارد فارسی برای رابط کاربری برنامه",
                                    checked = settings.supportRtl,
                                    onCheckedChange = { settings = settings.copy(supportRtl = it) },
                                )
                                Text("جهت نمایشگر", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OrientationMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = settings.orientationMode == mode.name,
                                            onClick = { settings = settings.copy(orientationMode = mode.name) },
                                            label = { Text(mode.titleFa) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // 1. تنظیمات مرورگر
                        item {
                            GlassCard {
                                Text(
                                    "۱. تنظیمات مرورگر",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_browser"),
                                )
                                SettingToggleRow(
                                    title = "اجرای JavaScript",
                                    description = "برای عملکرد صحیح سایت‌های مدرن، فروشگاه‌ها و فرم‌ها ضروری است.",
                                    checked = web.javaScriptEnabled,
                                    onCheckedChange = { web = web.copy(javaScriptEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "زوم لمسی (Pinch to Zoom)",
                                    description = "امکان بزرگنمایی صفحات با دو انگشت.",
                                    checked = web.zoomEnabled,
                                    onCheckedChange = { web = web.copy(zoomEnabled = it) },
                                )
                                OutlinedTextField(
                                    value = web.customUserAgent,
                                    onValueChange = { web = web.copy(customUserAgent = it) },
                                    label = { Text("User Agent سفارشی (خالی = پیش‌فرض)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("webview_custom_ua_input"),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("رفتار دکمه بازگشت (Back)", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BackNavigationBehavior.entries.forEach { behavior ->
                                        FilterChip(
                                            selected = settings.backBehavior == behavior.name,
                                            onClick = { settings = settings.copy(backBehavior = behavior.name) },
                                            label = { Text(behavior.titleFa) },
                                        )
                                    }
                                }
                            }
                        }

                        // 2. امنیت و قابلیت‌های موتور WebView
                        item {
                            GlassCard(borderColor = CyanAccent.copy(alpha = 0.4f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, tint = SkyBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "۲. امنیت و گواهی SSL",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("webview_cat_security"),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(
                                    text = "WebView نسخه ${webCapabilities.webViewVersionName} • SDK ${webCapabilities.androidSdkInt}",
                                    color = SuccessEmerald,
                                )
                                Spacer(Modifier.height(8.dp))
                                SettingToggleRow(
                                    title = "فقط HTTPS (حداکثر امنیت)",
                                    description = "مسدود کردن خودکار تمام درخواست‌های غیرامن HTTP.",
                                    checked = web.httpsOnly,
                                    onCheckedChange = { web = web.copy(httpsOnly = it) },
                                )
                                Text("محتوای ترکیبی (Mixed Content)", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MixedContentOption.entries.forEach { m ->
                                        FilterChip(
                                            selected = web.mixedContentMode == m.name,
                                            onClick = { web = web.copy(mixedContentMode = m.name) },
                                            label = { Text(m.titleFa) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("مدیریت خطای گواهی SSL", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SslPolicy.entries.forEach { ssl ->
                                        FilterChip(
                                            selected = web.sslPolicy == ssl.name,
                                            onClick = { web = web.copy(sslPolicy = ssl.name) },
                                            label = { Text(ssl.titleFa) },
                                        )
                                    }
                                }
                            }
                        }

                        // 3. فایل و دانلود
                        item {
                            GlassCard {
                                Text(
                                    "۳. فایل، آپلود و دانلود",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_files"),
                                )
                                SettingToggleRow(
                                    title = "آپلود فایل، چندتصویر و PDF",
                                    description = "پشتیبانی از انتخاب چندگانه، گالری و دوربین در فرم‌های سایت.",
                                    checked = web.fileUploadEnabled,
                                    onCheckedChange = { web = web.copy(fileUploadEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "مدیریت دانلود (Download Manager)",
                                    description = "دانلود مستقیم فایل‌ها، فاکتورها و PDF بدون نیاز به مجوزهای ذخیره‌سازی پرخطر.",
                                    checked = web.downloadManagerEnabled,
                                    onCheckedChange = { web = web.copy(downloadManagerEnabled = it) },
                                )
                            }
                        }

                        // 4. دوربین و میکروفون
                        item {
                            GlassCard(borderColor = WarningAmber.copy(alpha = 0.4f)) {
                                Text(
                                    "۴. دوربین، میکروفون و WebRTC",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_media_hardware"),
                                )
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(
                                    text = "نیازمند اعلان دلیل دسترسی به کاربر پیش از درخواست مجوز اندروید",
                                    color = WarningAmber,
                                )
                                Spacer(Modifier.height(6.dp))
                                SettingToggleRow(
                                    title = "دسترسی دوربین (Camera)",
                                    description = "برای اسکن مدارک، احراز هویت تصویری یا ثبت تصویر فرم‌ها.",
                                    checked = web.cameraPermission,
                                    onCheckedChange = { web = web.copy(cameraPermission = it) },
                                )
                                SettingToggleRow(
                                    title = "دسترسی میکروفون (Microphone)",
                                    description = "ضبط صدا یا مکالمه صوتی در وب‌اپلیکیشن.",
                                    checked = web.microphonePermission,
                                    onCheckedChange = { web = web.copy(microphonePermission = it) },
                                )
                                SettingToggleRow(
                                    title = "پشتیبانی WebRTC",
                                    description = "ارتباط صوتی و تصویری زنده تحت وب در دامنه‌های مورد تأیید.",
                                    checked = web.webRtcEnabled,
                                    onCheckedChange = { web = web.copy(webRtcEnabled = it) },
                                )
                            }
                        }

                        // 5. موقعیت مکانی
                        item {
                            GlassCard {
                                Text(
                                    "۵. موقعیت مکانی (Geolocation)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_location"),
                                )
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(
                                    text = "نیازمند روشن بودن GPS دستگاه و تایید کاربر",
                                    color = WarningAmber,
                                )
                                Spacer(Modifier.height(6.dp))
                                SettingToggleRow(
                                    title = "موقعیت مکانی GPS در وب‌سایت",
                                    description = "ارسال مختصات به نقشه یا سرویس‌های ارسال کالا فقط با رضایت کاربر.",
                                    checked = web.geolocationEnabled,
                                    onCheckedChange = { web = web.copy(geolocationEnabled = it) },
                                )
                            }
                        }

                        // 6. ویدئو و صوت
                        item {
                            GlassCard {
                                Text(
                                    "۶. ویدئو و صوت",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_video"),
                                )
                                SettingToggleRow(
                                    title = "پخش ویدئو تمام‌صفحه",
                                    description = "بزرگنمایی کامل پلیرهای HTML5 و خروج تمیز با دکمه بازگشت.",
                                    checked = web.fullscreenVideoEnabled,
                                    onCheckedChange = { web = web.copy(fullscreenVideoEnabled = it) },
                                )
                                Text(
                                    "نکته فنی: پخش محتوای دارای قفل DRM اختصاصی یا کدک‌های غیررایج وابسته به موتور WebView دستگاه است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber,
                                )
                            }
                        }

                        // 7. لینک‌ها و پنجره‌ها
                        item {
                            GlassCard {
                                Text(
                                    "۷. لینک‌ها، پنجره‌ها و مسیریابی",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_links"),
                                )
                                SettingToggleRow(
                                    title = "پنجره‌های چندگانه (Multiple Windows)",
                                    description = "پشتیبانی از لینک‌هایی که در تب یا پنجره جدید باز می‌شوند.",
                                    checked = web.multipleWindowsEnabled,
                                    onCheckedChange = { web = web.copy(multipleWindowsEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "باز شدن خودکار پاپ‌آپ (Pop-ups)",
                                    description = "توصیه می‌شود غیرفعال بماند مگر سایت نیاز قطعی داشته باشد.",
                                    checked = web.popupsEnabled,
                                    onCheckedChange = { web = web.copy(popupsEnabled = it) },
                                )
                                Text("نحوه نمایش پنجره جدید و پاپ‌آپ", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PopupBehaviorMode.entries.forEach { pMode ->
                                        FilterChip(
                                            selected = web.popupBehavior == pMode.name,
                                            onClick = { web = web.copy(popupBehavior = pMode.name) },
                                            label = { Text(pMode.titleFa) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("رفتار لینک‌های دامنه خارجی", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ExternalLinkMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = settings.externalLinkMode == mode.name,
                                            onClick = { settings = settings.copy(externalLinkMode = mode.name) },
                                            label = { Text(mode.titleFa) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = web.navigationRulesRaw,
                                    onValueChange = { web = web.copy(navigationRulesRaw = it) },
                                    label = { Text("قوانین دامنه‌ها (مجاز: cdn.site.ir ، مسدود: !ads.ir)") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // 8. کوکی و حافظه
                        item {
                            GlassCard {
                                Text(
                                    "۸. کوکی، حافظه سایت و کلیپ‌بورد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("webview_cat_storage"),
                                )
                                SettingToggleRow(
                                    title = "ذخیره کوکی‌ها (Cookies)",
                                    description = "حفظ نشست ورود کاربر پس از بستن و باز کردن مجدد برنامه.",
                                    checked = web.cookiesEnabled,
                                    onCheckedChange = { web = web.copy(cookiesEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "کوکی‌های شخص ثالث (Third-Party Cookies)",
                                    description = "مورد نیاز برخی درگاه‌های پرداخت یا سرویس‌های ورود با سرویس‌های خارجی.",
                                    checked = web.thirdPartyCookiesEnabled,
                                    onCheckedChange = { web = web.copy(thirdPartyCookiesEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "حافظه DOM Storage و IndexedDB",
                                    description = "ذخیره LocalStorage، SessionStorage و وضعیت سبد خرید.",
                                    checked = web.domStorageEnabled,
                                    onCheckedChange = { web = web.copy(domStorageEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "پشتیبانی WebSQL (نسخه‌های قدیمی)",
                                    description = if (webCapabilities.supportsWebSql) {
                                        "برای سایت‌های قدیمی دارای پایگاه‌داده WebSQL."
                                    } else {
                                        "در اندروید ۱۳ و بالاتر منسوخ شده؛ سایت‌ها از IndexedDB استفاده کنند."
                                    },
                                    checked = web.webSqlEnabled,
                                    onCheckedChange = { web = web.copy(webSqlEnabled = it) },
                                )
                                SettingToggleRow(
                                    title = "دسترسی کلیپ‌بورد برای وب‌سایت",
                                    description = "اجازه کپی کد تخفیف یا شماره پیگیری از طریق JavaScript Bridge.",
                                    checked = web.clipboardAccessEnabled,
                                    onCheckedChange = { web = web.copy(clipboardAccessEnabled = it) },
                                )
                            }
                        }

                        // 9. عملکرد و نمایشگر
                        item {
                            GlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "۹. عملکرد، کش و نمایشگر",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("webview_cat_performance"),
                                    )
                                    IconButton(onClick = { showAdvancedWeb = !showAdvancedWeb }) {
                                        Icon(
                                            if (showAdvancedWeb) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = "توضیحات فنی نمایشگر",
                                        )
                                    }
                                }
                                Text("سیاست کش (Cache Mode)", style = MaterialTheme.typography.titleSmall)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CacheModeOption.entries.forEach { c ->
                                        FilterChip(
                                            selected = web.cacheMode == c.name,
                                            onClick = { web = web.copy(cacheMode = c.name) },
                                            label = { Text(c.titleFa) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                SettingToggleRow(
                                    title = "نمایش نوار وضعیت (Status Bar)",
                                    description = "نمایش ساعت، باتری و نوتیفیکیشن‌های بالای صفحه در اپلیکیشن خروجی.",
                                    checked = settings.showStatusBar,
                                    onCheckedChange = { settings = settings.copy(showStatusBar = it) },
                                )
                                SettingToggleRow(
                                    title = "نمایش نوار ناوبری گوشی (Navigation Bar)",
                                    description = "نمایش دکمه‌های سیستمی پایین صفحه در اپلیکیشن خروجی.",
                                    checked = settings.showNavigationBar,
                                    onCheckedChange = { settings = settings.copy(showNavigationBar = it) },
                                )
                                AnimatedVisibility(visible = showAdvancedWeb) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        webCapabilities.notesFa.forEach { note ->
                                            Text("• $note", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            GlassCard(borderColor = WarningAmber.copy(alpha = 0.45f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = WarningAmber)
                                    Spacer(Modifier.width(8.dp))
                                    Text("اعلان‌ها (Push Notification)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(text = "نیازمند تنظیم سرویس جانبی (FCM / نجوا / پوشه)", color = WarningAmber)
                                Spacer(Modifier.height(8.dp))
                                SettingToggleRow(
                                    title = "آماده‌سازی اتصال Push Notification",
                                    description = "برای ارسال نوتیفیکیشن واقعی، پیکربندی کلید سرویس پوش سایت خود را وارد کنید.",
                                    checked = settings.enablePushIntegration,
                                    onCheckedChange = { settings = settings.copy(enablePushIntegration = it) },
                                )
                                if (settings.enablePushIntegration) {
                                    OutlinedTextField(
                                        value = settings.pushProviderNote,
                                        onValueChange = { settings = settings.copy(pushProviderNote = it) },
                                        label = { Text("شناسه اپلیکیشن در سرویس پوش") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        item {
                            GlassCard {
                                Text("Deep Links و App Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                SettingToggleRow(
                                    title = "پشتیبانی Deep Link اختصاصی",
                                    description = "باز شدن مستقیم اپلیکیشن با لینک‌های دارای Scheme اختصاصی",
                                    checked = settings.enableDeepLinks,
                                    onCheckedChange = { settings = settings.copy(enableDeepLinks = it) },
                                )
                                if (settings.enableDeepLinks) {
                                    OutlinedTextField(
                                        value = settings.deepLinkScheme,
                                        onValueChange = { settings = settings.copy(deepLinkScheme = it) },
                                        label = { Text("Scheme (مثال: myshop)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                SettingToggleRow(
                                    title = "پشتیبانی App Links (تأیید دامنه)",
                                    description = "نیازمند قرار دادن فایل assetlinks.json روی هاست وب‌سایت شماست.",
                                    checked = settings.enableAppLinks,
                                    onCheckedChange = { settings = settings.copy(enableAppLinks = it) },
                                )
                                if (settings.enableAppLinks) {
                                    OutlinedTextField(
                                        value = settings.appLinkHost,
                                        onValueChange = { settings = settings.copy(appLinkHost = it) },
                                        label = { Text("دامنه App Link (مثال: myshop.ir)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        item {
                            GlassCard {
                                Text("امکانات بومی و درگاه پرداخت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                SettingToggleRow(
                                    title = "اشتراک‌گذاری لینک و فایل",
                                    description = "دکمه اشتراک‌گذاری بومی اندروید در نوار ابزار و JavaScript Bridge",
                                    checked = settings.enableFileSharing,
                                    onCheckedChange = { settings = settings.copy(enableFileSharing = it) },
                                )
                                SettingToggleRow(
                                    title = "تماس تلفنی (tel:)",
                                    description = "باز کردن شماره‌گیر گوشی هنگام لمس شماره تماس‌های سایت",
                                    checked = settings.enablePhoneDialer,
                                    onCheckedChange = { settings = settings.copy(enablePhoneDialer = it) },
                                )
                                SettingToggleRow(
                                    title = "ارسال ایمیل (mailto:)",
                                    description = "باز کردن برنامه ایمیل با لمس آدرس‌های ایمیل",
                                    checked = settings.enableEmailIntent,
                                    onCheckedChange = { settings = settings.copy(enableEmailIntent = it) },
                                )
                                SettingToggleRow(
                                    title = "لینک‌های نقشه و مسیریاب",
                                    description = "باز کردن نشان، بلد یا گوگل‌مپ برای لینک‌های مکانی",
                                    checked = settings.enableMapsIntent,
                                    onCheckedChange = { settings = settings.copy(enableMapsIntent = it) },
                                )
                                SettingToggleRow(
                                    title = "مدیریت درگاه‌های پرداخت شاپرک و ایرانی",
                                    description = "باز نگه‌داشتن شاپرک، زرین‌پال، آیدی‌پی و به‌پرداخت در نشست امن برنامه",
                                    checked = settings.enablePaymentGatewayBridge,
                                    onCheckedChange = { settings = settings.copy(enablePaymentGatewayBridge = it) },
                                )
                                Text("رفتار لینک‌های خارجی", style = MaterialTheme.typography.titleSmall)
                                ExternalLinkMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = settings.externalLinkMode == mode.name,
                                        onClick = { settings = settings.copy(externalLinkMode = mode.name) },
                                        label = { Text(mode.titleFa) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSave(project, settings, web, "ذخیره پیش از پیش‌نمایش")
                                onOpenPreview(project.id)
                            },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, tokens.navPillBorder),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = SkyBlue)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFa) "پیش‌نمایش" else "Preview")
                        }
                        GradientPrimaryButton(
                            text = "ساخت APK / AAB",
                            icon = Icons.Rounded.Build,
                            onClick = {
                                onSave(project, settings, web, "ذخیره و ارسال به موتور ساخت")
                                onOpenBuild(project.id)
                            },
                            modifier = Modifier.weight(1.4f),
                            testTag = "studio_build_btn",
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}