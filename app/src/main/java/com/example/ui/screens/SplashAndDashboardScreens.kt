package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import com.example.ui.components.softEnter
import com.example.ui.theme.AuroraTeal
import com.example.ui.theme.CoralRed
import com.example.ui.theme.GrapePurple
import com.example.ui.theme.GrapeSoft
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OrchidDeep
import com.example.ui.theme.OrchidPink
import com.example.ui.theme.RosePink
import com.example.ui.theme.SunriseAmber
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.PinkGlow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.rememberAsyncImagePainter
import red.line.vebino.R
import com.example.core.JalaliCalendar
import com.example.core.ValidationUtils
import com.example.core.appString
import com.example.data.local.db.BuildResultEntity
import com.example.data.local.db.BuildTaskEntity
import com.example.data.local.db.FullProjectBundle
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppTemplateType
import com.example.domain.model.BuildEngineStatus
import com.example.domain.model.BuildStatus
import com.example.domain.model.CalendarType
import com.example.domain.model.EnvironmentCheckReport
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoAmbientBackground
import com.example.ui.components.localizeNumber
import com.example.ui.components.statusColorFor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import com.example.ui.viewmodel.StorageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    language: AppLanguage,
    onFinished: () -> Unit,
) {
    val tokens = WebinoThemeTokens.colors
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            )
        }
        delay(3000L)
        onFinished()
    }

    val p = progress.value
    // Phase choreography:
    // 0.00..0.28: Browser card fades & scales in
    // 0.20..0.58: Webpage skeleton bars reveal smoothly
    // 0.52..0.84: Morph from browser window -> rounded app icon container
    // 0.68..1.00: WEBINO logo, Persian title & tagline appear cleanly
    val morphFraction = ((p - 0.52f) / 0.30f).coerceIn(0f, 1f)
    val browserEnterFraction = (p / 0.26f).coerceIn(0f, 1f)
    val skeletonFraction = ((p - 0.18f) / 0.34f).coerceIn(0f, 1f)
    val logoRevealFraction = ((p - 0.62f) / 0.24f).coerceIn(0f, 1f)
    val textRevealFraction = ((p - 0.72f) / 0.24f).coerceIn(0f, 1f)

    val cardWidth = lerp(228.dp, 122.dp, morphFraction)
    val cardHeight = lerp(164.dp, 122.dp, morphFraction)
    val cornerRadius = lerp(22.dp, 34.dp, morphFraction)
    val cardScale = 0.86f + 0.14f * browserEnterFraction

    WebinoAmbientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("splash_root"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
                // Morphing Browser -> App Icon Frame
                Surface(
                    shape = RoundedCornerShape(cornerRadius),
                    color = tokens.glassContainerStrong,
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.45f),
                                SkyBlue.copy(alpha = 0.40f),
                            )
                        ),
                    ),
                    modifier = Modifier
                        .size(width = cardWidth, height = cardHeight)
                        .scale(cardScale)
                        .alpha(browserEnterFraction)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(cornerRadius),
                            ambientColor = ElectricViolet.copy(alpha = 0.28f),
                            spotColor = SkyBlue.copy(alpha = 0.25f),
                        ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Browser Window Content (fades out during morph)
                        val browserContentAlpha = (1f - morphFraction * 1.45f).coerceIn(0f, 1f)
                        if (browserContentAlpha > 0.01f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(browserContentAlpha)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Browser top bar (window dots + URL pill)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(Modifier.size(7.dp).background(ErrorCoral.copy(alpha = 0.75f), CircleShape))
                                    Box(Modifier.size(7.dp).background(WarningAmber.copy(alpha = 0.80f), CircleShape))
                                    Box(Modifier.size(7.dp).background(MintGreen.copy(alpha = 0.85f), CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        ElectricViolet.copy(alpha = 0.16f),
                                                        SkyBlue.copy(alpha = 0.16f),
                                                    )
                                                ),
                                                RoundedCornerShape(50),
                                            ),
                                    )
                                }
                                // Animated webpage skeleton blocks
                                val skelAlpha = skeletonFraction
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f * (0.4f + 0.6f * skelAlpha))
                                        .height(38.dp)
                                        .alpha(skelAlpha)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    ElectricViolet.copy(alpha = 0.22f),
                                                    SkyBlue.copy(alpha = 0.18f),
                                                )
                                            ),
                                            RoundedCornerShape(10.dp),
                                        ),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f * (0.3f + 0.7f * skelAlpha))
                                        .height(10.dp)
                                        .alpha(skelAlpha)
                                        .background(SkyBlue.copy(alpha = 0.25f), RoundedCornerShape(50)),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.56f * (0.3f + 0.7f * skelAlpha))
                                        .height(10.dp)
                                        .alpha(skelAlpha)
                                        .background(CyanAccent.copy(alpha = 0.22f), RoundedCornerShape(50)),
                                )
                            }
                        }

                        // Final App Icon inside morphed container
                        if (logoRevealFraction > 0.01f) {
                            Image(
                                painter = painterResource(R.drawable.img_webino_icon),
                                contentDescription = appString(R.string.app_name),
                                modifier = Modifier
                                    .padding(9.dp)
                                    .fillMaxSize()
                                    .scale(0.84f + 0.16f * logoRevealFraction)
                                    .alpha(logoRevealFraction)
                                    .clip(RoundedCornerShape(26.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(textRevealFraction)
                        .offset { IntOffset(0, ((1f - textRevealFraction) * 18f).roundToInt()) },
                ) {
                    Text(
                        text = if (language == AppLanguage.FA) "وبینو" else "WEBINO",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = appString(R.string.brand_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    StatusBadge(
                        text = if (language == AppLanguage.FA) "تبدیل هوشمند وب‌سایت به اپلیکیشن" else "On-Device App Builder",
                        color = ElectricViolet,
                        icon = Icons.Rounded.RocketLaunch,
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    prefs: UserPreferences,
    projects: List<FullProjectBundle>,
    outputs: List<BuildResultEntity>,
    recentTasks: List<BuildTaskEntity>,
    envReport: EnvironmentCheckReport,
    storageStats: StorageStats,
    onCreateProject: (String?) -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenBuildManager: (Long) -> Unit,
    onOpenOutputs: () -> Unit,
    onOpenHelp: () -> Unit,
    onRecheckEngine: () -> Unit,
    onOpenSigningManager: () -> Unit = {},
    onOpenProjectsList: () -> Unit = {},
    onOpenBuildHistory: () -> Unit = {},
) {
    val isFa = prefs.language == AppLanguage.FA
    val usePersianDigits = prefs.usePersianDigits
    val tokens = WebinoThemeTokens.colors
    val totalUsedBytes = storageStats.outputsBytes + storageStats.cacheBytes + storageStats.mediaBytes

    val engineStatusLabel = when (envReport.status) {
        BuildEngineStatus.NOT_CHECKED -> appString(R.string.engine_status_not_checked)
        BuildEngineStatus.CHECKING -> appString(R.string.engine_status_checking)
        BuildEngineStatus.READY -> appString(R.string.engine_status_ready)
        BuildEngineStatus.UNSUPPORTED -> appString(R.string.engine_status_unsupported)
        BuildEngineStatus.ERROR -> appString(R.string.engine_status_error)
    }
    val engineAccent = when (envReport.status) {
        BuildEngineStatus.READY -> MintGreen
        BuildEngineStatus.CHECKING, BuildEngineStatus.NOT_CHECKED -> SkyBlue
        BuildEngineStatus.UNSUPPORTED, BuildEngineStatus.ERROR -> ErrorCoral
    }
    val engineIcon = when (envReport.status) {
        BuildEngineStatus.READY -> Icons.Rounded.CheckCircle
        BuildEngineStatus.CHECKING, BuildEngineStatus.NOT_CHECKED -> Icons.Rounded.Memory
        BuildEngineStatus.UNSUPPORTED, BuildEngineStatus.ERROR -> Icons.Rounded.Error
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header: brand logo + greeting + help
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .softEnter(0),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.85f),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f)),
                        shadowElevation = 10.dp,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_webino_icon),
                            contentDescription = appString(R.string.app_name),
                            modifier = Modifier
                                .padding(3.dp)
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isFa) "خوش آمدی" else "Welcome back",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = appString(R.string.studio_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onOpenHelp,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.75f),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("dashboard_help_btn"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Webino3DGlassIcon(
                            type = Webino3DIconType.HELP,
                            size = 36.dp,
                            contentDescription = appString(R.string.help_cd),
                        )
                    }
                }
            }
        }

        // Hero: big "Build app" card with 3D website -> app illustration
        item {
            HeroBuildCard(
                modifier = Modifier.softEnter(90),
                badge = appString(R.string.hero_badge_offline),
                title = appString(R.string.hero_title),
                ctaText = appString(R.string.hero_create_cta),
                onCreate = { onCreateProject(null) },
            )
        }

        // Colorful gradient stat cards (2 x 2)
        item {
            Column(
                modifier = Modifier.softEnter(180),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        iconType = Webino3DIconType.PROJECTS,
                        label = appString(R.string.stat_projects),
                        value = localizeNumber(projects.size, prefs.language, usePersianDigits),
                        accent = SunriseAmber,
                        secondary = CoralRed,
                        onClick = onOpenProjectsList,
                    )
                    DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        iconType = Webino3DIconType.OUTPUTS,
                        label = appString(R.string.stat_outputs),
                        value = localizeNumber(outputs.size, prefs.language, usePersianDigits),
                        accent = GrapeSoft,
                        secondary = GrapePurple,
                        onClick = onOpenOutputs,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        iconType = Webino3DIconType.TEMPLATES,
                        label = appString(R.string.stat_storage),
                        value = ValidationUtils.formatFileSize(totalUsedBytes, isFa, usePersianDigits),
                        accent = AuroraTeal,
                        secondary = OceanBlue,
                    )
                    DashboardStatTile(
                        modifier = Modifier.weight(1f),
                        iconType = Webino3DIconType.SIGNING,
                        label = if (isFa) "خروجی‌های تأییدشده" else "Verified outputs",
                        value = localizeNumber(outputs.count { it.isVerified }, prefs.language, usePersianDigits),
                        accent = OrchidPink,
                        secondary = OrchidDeep,
                        onClick = onOpenSigningManager,
                    )
                }
            }
        }

        // Build engine status
        item {
            SectionTitle(title = appString(R.string.stat_build_engine), subtitle = null)
            Spacer(Modifier.height(10.dp))
            GlassCard(
                modifier = Modifier.softEnter(260),
                onClick = onRecheckEngine,
                borderColor = engineAccent.copy(alpha = 0.45f),
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            drawCircle(
                                color = engineAccent.copy(alpha = 0.18f),
                                style = Stroke(width = stroke),
                            )
                            val sweep = when (envReport.status) {
                                BuildEngineStatus.READY -> 360f
                                BuildEngineStatus.CHECKING -> 200f
                                BuildEngineStatus.NOT_CHECKED -> 90f
                                else -> 40f
                            }
                            drawArc(
                                color = engineAccent,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                        Icon(
                            imageVector = engineIcon,
                            contentDescription = null,
                            tint = engineAccent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (envReport.status == BuildEngineStatus.READY) {
                                "$engineStatusLabel • ${localizeNumber(envReport.freeRamMb, prefs.language, usePersianDigits)}MB"
                            } else {
                                engineStatusLabel
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = engineAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Quick shortcuts
        item {
            SectionTitle(title = if (isFa) "میانبرها" else "Shortcuts", subtitle = null)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .softEnter(320),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickShortcutPill(
                    modifier = Modifier.weight(1f),
                    iconType = Webino3DIconType.SIGNING,
                    title = if (isFa) "امضای دیجیتال" else "Signing",
                    onClick = onOpenSigningManager,
                )
                QuickShortcutPill(
                    modifier = Modifier.weight(1f),
                    iconType = Webino3DIconType.BUILD,
                    title = if (isFa) "موتور ساخت" else "Build Engine",
                    onClick = { onOpenBuildManager(0L) },
                )
                QuickShortcutPill(
                    modifier = Modifier.weight(1f),
                    iconType = Webino3DIconType.OUTPUTS,
                    title = if (isFa) "خروجی‌ها" else "Outputs",
                    onClick = onOpenOutputs,
                )
                QuickShortcutPill(
                    modifier = Modifier.weight(1f).testTag("dashboard_history_shortcut"),
                    iconType = Webino3DIconType.HISTORY,
                    title = if (isFa) "تاریخچه" else "History",
                    onClick = onOpenBuildHistory,
                )
            }
        }

        // Commercial Templates Showcase
        item {
            SectionTitle(
                title = appString(R.string.section_templates),
                subtitle = appString(R.string.section_templates_sub),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(AppTemplateType.entries) { template ->
                    val primary = runCatching { Color(android.graphics.Color.parseColor(template.primaryHex)) }
                        .getOrDefault(ElectricViolet)
                    GlassCard(
                        onClick = { onCreateProject(template.id) },
                        borderColor = primary.copy(alpha = 0.35f),
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.width(166.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.65f))),
                                        RoundedCornerShape(10.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(primary, CircleShape),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (isFa) template.titleFa else template.titleEn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = template.subtitleFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Recent Projects
        item {
            SectionTitle(
                title = appString(R.string.section_recent_projects),
                subtitle = null,
            )
        }

        if (projects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AddCircle,
                    icon3DType = Webino3DIconType.PROJECTS,
                    title = appString(R.string.empty_projects_title),
                    subtitle = appString(R.string.empty_projects_sub),
                    actionText = appString(R.string.empty_projects_cta),
                    onAction = { onCreateProject(null) },
                )
            }
        } else {
            items(projects.take(4), key = { it.project.id }) { bundle ->
                val projAccent = runCatching {
                    Color(android.graphics.Color.parseColor(bundle.settings?.primaryColorHex ?: "#7C3AED"))
                }.getOrDefault(ElectricViolet)
                GlassCard(onClick = { onOpenProject(bundle.project.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val iconFile = bundle.settings?.iconFilePath?.let(::File)?.takeIf(File::exists)
                        if (iconFile != null) {
                            Image(
                                painter = rememberAsyncImagePainter(iconFile),
                                contentDescription = bundle.project.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(listOf(projAccent.copy(alpha = 0.22f), SkyBlue.copy(alpha = 0.16f))),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .border(1.dp, projAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = projAccent)
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
                                text = bundle.project.websiteUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            onClick = { onOpenBuildManager(bundle.project.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricViolet.copy(alpha = 0.12f),
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Build,
                                    contentDescription = appString(R.string.action_build),
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activities / History
        if (recentTasks.isNotEmpty()) {
            item {
                SectionTitle(
                    title = appString(R.string.section_recent_activities),
                    subtitle = null,
                )
            }
            items(recentTasks.take(4), key = { "task_${it.id}" }) { task ->
                val projName = projects.firstOrNull { it.project.id == task.projectId }?.project?.name ?: "#${task.projectId}"
                val statusEnum = runCatching { BuildStatus.valueOf(task.status) }.getOrNull()
                val sColor = statusColorFor(task.status)
                GlassCard(onClick = { onOpenBuildManager(task.projectId) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Webino3DGlassIcon(type = Webino3DIconType.HISTORY, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$projName • ${task.outputFormat}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = JalaliCalendar.formatDateTime(
                                    millis = task.startedAt,
                                    useJalali = prefs.calendarType == CalendarType.JALALI,
                                    isPersian = isFa,
                                    usePersianDigits = usePersianDigits,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(
                            text = statusEnum?.titleFa ?: task.status,
                            color = sColor,
                        )
                    }
                }
            }
        }

        // Recent Outputs
        if (outputs.isNotEmpty()) {
            item {
                SectionTitle(
                    title = appString(R.string.section_recent_outputs),
                    subtitle = null,
                )
            }
            items(outputs.take(3), key = { it.id }) { out ->
                GlassCard(onClick = onOpenOutputs) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    (if (out.isVerified) SuccessEmerald else WarningAmber).copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (out.isVerified) Icons.Rounded.Verified else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (out.isVerified) SuccessEmerald else WarningAmber,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${out.appName} (${out.format})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = JalaliCalendar.formatDateTime(
                                    millis = out.createdAt,
                                    useJalali = prefs.calendarType == CalendarType.JALALI,
                                    isPersian = isFa,
                                    usePersianDigits = usePersianDigits,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(
                            text = ValidationUtils.formatFileSize(out.fileSizeBytes, isFa, usePersianDigits),
                            color = SkyBlue,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DashboardStatTile(
    iconType: Webino3DIconType,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    secondary: Color = accent,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    val base = modifier
        .height(124.dp)
        .shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = secondary.copy(alpha = 0.45f),
            spotColor = secondary.copy(alpha = 0.45f),
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(accent, secondary)))
        .border(
            1.2.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.1f))),
            shape,
        )
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                    endY = size.height * 0.55f,
                ),
            )
        }
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(
        modifier = clickable.padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.34f))
                .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Webino3DGlassIcon(type = iconType, size = 36.dp)
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.95f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroBuildCard(
    badge: String,
    title: String,
    ctaText: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = PinkGlow.copy(alpha = 0.5f),
                spotColor = SunsetOrange.copy(alpha = 0.5f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(SunsetOrange, RosePink, GrapePurple)))
            .border(
                1.3.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.12f))),
                shape,
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(size.width * 0.2f, 0f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, 0f),
                )
            }
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = onCreate,
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier.testTag("dashboard_create_app_btn"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ctaText,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFC2307A),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = Color(0xFFC2307A),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            WebsiteToAppIllustration(modifier = Modifier.size(width = 132.dp, height = 156.dp))
        }
    }
}

/** 3D glass illustration: a browser window turning into a phone app. Pure Canvas, no assets. */
@Composable
private fun WebsiteToAppIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Browser window (back, tilted)
        rotate(degrees = -9f, pivot = Offset(w * 0.30f, h * 0.30f)) {
            val bx = w * 0.00f
            val by = h * 0.12f
            val bw = w * 0.72f
            val bh = h * 0.40f
            drawRoundRect(
                color = Color(0x40240B50),
                topLeft = Offset(bx + w * 0.02f, by + h * 0.06f),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(w * 0.10f),
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.38f)),
                    start = Offset(bx, by),
                    end = Offset(bx + bw, by + bh),
                ),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(w * 0.10f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(w * 0.10f),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(Color(0xFFFF6AA8), radius = w * 0.022f, center = Offset(bx + bw * 0.12f, by + bh * 0.16f))
            drawCircle(Color(0xFFFFC24D), radius = w * 0.022f, center = Offset(bx + bw * 0.22f, by + bh * 0.16f))
            drawCircle(Color(0xFF34D3C3), radius = w * 0.022f, center = Offset(bx + bw * 0.32f, by + bh * 0.16f))
            val barH = bh * 0.10f
            listOf(0.38f to 0.80f, 0.55f to 0.62f, 0.72f to 0.70f).forEach { (yy, ww) ->
                drawRoundRect(
                    color = Color(0xFFB79CFF).copy(alpha = 0.55f),
                    topLeft = Offset(bx + bw * 0.10f, by + bh * yy),
                    size = Size(bw * ww, barH),
                    cornerRadius = CornerRadius(barH / 2f),
                )
            }
        }

        // Phone (front, tilted other way)
        rotate(degrees = 7f, pivot = Offset(w * 0.68f, h * 0.60f)) {
            val px = w * 0.42f
            val py = h * 0.30f
            val pw = w * 0.52f
            val ph = h * 0.66f
            drawRoundRect(
                color = Color(0x55240B50),
                topLeft = Offset(px + w * 0.03f, py + h * 0.05f),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(w * 0.12f),
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFA78BFA), Color(0xFF4FB4FF)),
                    start = Offset(px, py),
                    end = Offset(px + pw, py + ph),
                ),
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(w * 0.12f),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    startY = py,
                    endY = py + ph * 0.55f,
                ),
                topLeft = Offset(px, py),
                size = Size(pw, ph * 0.55f),
                cornerRadius = CornerRadius(w * 0.12f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(w * 0.12f),
                style = Stroke(width = 2.dp.toPx()),
            )
            val tile = pw * 0.30f
            listOf(0.16f to 0.20f, 0.54f to 0.20f, 0.16f to 0.50f, 0.54f to 0.50f).forEach { (fx, fy) ->
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(px + pw * fx, py + ph * fy),
                    size = Size(tile, tile),
                    cornerRadius = CornerRadius(tile * 0.32f),
                )
            }
        }

        // Arrow bubble between browser and phone
        val ac = Offset(w * 0.46f, h * 0.55f)
        val ar = w * 0.14f
        drawCircle(color = Color(0x40240B50), radius = ar, center = Offset(ac.x, ac.y + h * 0.03f))
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color.White, Color(0xFFFFD6EA)),
                start = Offset(ac.x - ar, ac.y - ar),
                end = Offset(ac.x + ar, ac.y + ar),
            ),
            radius = ar,
            center = ac,
        )
        val arrow = Path().apply {
            moveTo(ac.x - ar * 0.45f, ac.y)
            lineTo(ac.x + ar * 0.45f, ac.y)
            moveTo(ac.x + ar * 0.08f, ac.y - ar * 0.38f)
            lineTo(ac.x + ar * 0.45f, ac.y)
            lineTo(ac.x + ar * 0.08f, ac.y + ar * 0.38f)
        }
        drawPath(
            path = arrow,
            color = Color(0xFFE0408A),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Glass sphere accent
        val sc = Offset(w * 0.10f, h * 0.86f)
        val sr = w * 0.11f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFF7DE3FF), Color(0xFF4F8CFF)),
                center = Offset(sc.x - sr * 0.35f, sc.y - sr * 0.35f),
                radius = sr * 1.5f,
            ),
            radius = sr,
            center = sc,
        )
    }
}

@Composable
private fun QuickShortcutPill(
    iconType: Webino3DIconType,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Webino3DGlassIcon(type = iconType, size = 34.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
