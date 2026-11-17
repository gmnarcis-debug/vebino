package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.ExternalIntentHelper
import com.example.core.appString
import com.example.domain.model.AppLanguage
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.components.WebinoSecondaryButton
import com.example.ui.components.localizeNumber
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintGreen
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WebinoThemeTokens
import kotlinx.coroutines.delay
import red.line.vebino.BuildConfig
import red.line.vebino.R

@Composable
fun AboutScreen(
    language: AppLanguage,
    usePersianDigits: Boolean,
    onBack: () -> Unit,
    onNotify: (String) -> Unit,
    onContactSupportClick: (() -> Unit)? = null,
    onRateMyketClick: (() -> Unit)? = null,
    onOtherAppsClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tokens = WebinoThemeTokens.colors
    val isFa = language == AppLanguage.FA

    var showHeaderCard by remember { mutableStateOf(false) }
    var showIntroCard by remember { mutableStateOf(false) }
    var showFeaturesCard by remember { mutableStateOf(false) }
    var showOrgCard by remember { mutableStateOf(false) }
    var showStoreCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeaderCard = true
        delay(55)
        showIntroCard = true
        delay(55)
        showFeaturesCard = true
        delay(55)
        showOrgCard = true
        delay(55)
        showStoreCard = true
    }

    val features = remember(isFa) {
        if (isFa) {
            listOf(
                "تبدیل وب‌سایت به اپلیکیشن اندرویدی",
                "پیش‌نمایش وب‌سایت پیش از ساخت",
                "شخصی‌سازی نام، آیکون و صفحه شروع برنامه",
                "تنظیمات پیشرفته WebView",
                "مدیریت پروژه‌ها و خروجی‌ها",
                "مدیریت نسخه و امضای دیجیتال",
                "پشتیبانی از قالب‌های متنوع",
                "محیط کاملاً فارسی و راست‌چین",
            )
        } else {
            listOf(
                "Convert websites into standalone Android apps",
                "Live interactive website preview before compilation",
                "Customize app name, icon, splash screen and colors",
                "Comprehensive categorized WebView engine settings",
                "Complete project and APK/AAB output management",
                "Versioning and digital keystore signing management",
                "Commercial presets and tailored templates",
                "Native Persian RTL and localized calendar support",
            )
        }
    }

    val noEmailMsg = appString(R.string.about_no_email_app)
    val noStoreMsg = appString(R.string.about_no_store_browser)
    val supportBtnLabel = appString(R.string.about_support_btn)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = appString(R.string.about_screen_title),
                subtitle = appString(R.string.about_brand_display),
                iconType = Webino3DIconType.HELP,
                onBack = onBack,
                backContentDescription = appString(R.string.back_cd),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("about_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Official Brand Hero Card
            item {
                AnimatedVisibility(
                    visible = showHeaderCard,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 },
                ) {
                    GlassCard(
                        borderColor = ElectricViolet.copy(alpha = 0.35f),
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(30.dp),
                                color = tokens.glassContainerStrong,
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(ElectricViolet.copy(alpha = 0.45f), SkyBlue.copy(alpha = 0.45f))
                                    ),
                                ),
                                modifier = Modifier.shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    ambientColor = ElectricViolet.copy(alpha = 0.25f),
                                    spotColor = SkyBlue.copy(alpha = 0.25f),
                                ),
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_webino_icon),
                                    contentDescription = appString(R.string.about_brand_display),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = appString(R.string.about_brand_display),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = appString(R.string.brand_tagline),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val versionText = if (isFa) {
                                    "نسخه ${localizeNumber(BuildConfig.VERSION_NAME, language, usePersianDigits)}"
                                } else {
                                    "v${BuildConfig.VERSION_NAME}"
                                }
                                StatusBadge(
                                    text = versionText,
                                    color = ElectricViolet,
                                    icon = Icons.Rounded.Verified,
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    StatusBadge(
                                        text = BuildConfig.APPLICATION_ID,
                                        color = SkyBlue,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Introduction Card
            item {
                AnimatedVisibility(
                    visible = showIntroCard,
                    enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 8 },
                ) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Webino3DGlassIcon(type = Webino3DIconType.PREVIEW, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.about_intro_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = appString(R.string.about_intro_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 3. Verified Core Features List
            item {
                AnimatedVisibility(
                    visible = showFeaturesCard,
                    enter = fadeIn(tween(340)) + slideInVertically(tween(340)) { it / 8 },
                ) {
                    GlassCard(borderColor = CyanAccent.copy(alpha = 0.28f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Webino3DGlassIcon(type = Webino3DIconType.BUILD, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.about_features_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            features.forEach { feature ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(SuccessEmerald.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessEmerald,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Official Developer, Management & Hidden-Email Support Card
            item {
                AnimatedVisibility(
                    visible = showOrgCard,
                    enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 8 },
                ) {
                    GlassCard(borderColor = SkyBlue.copy(alpha = 0.32f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Webino3DGlassIcon(type = Webino3DIconType.SIGNING, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.about_org_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoBadgeRow(
                            icon = Icons.Rounded.Code,
                            label = appString(R.string.about_developer_label),
                            value = appString(R.string.about_developer_value),
                            accent = ElectricViolet,
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoBadgeRow(
                            icon = Icons.Rounded.Badge,
                            label = appString(R.string.about_manager_label),
                            value = appString(R.string.about_manager_value),
                            accent = SkyBlue,
                        )
                        Spacer(Modifier.height(14.dp))
                        GradientPrimaryButton(
                            text = supportBtnLabel,
                            icon = Icons.Rounded.Email,
                            onClick = {
                                if (onContactSupportClick != null) {
                                    onContactSupportClick()
                                } else {
                                    ExternalIntentHelper.openSupportEmail(
                                        context = context,
                                        chooserTitle = supportBtnLabel,
                                        noEmailClientMessage = noEmailMsg,
                                        onError = onNotify,
                                    )
                                }
                            },
                            testTag = "about_support_email_btn",
                        )
                    }
                }
            }

            // 5. Myket Rating & Other Apps Card
            item {
                AnimatedVisibility(
                    visible = showStoreCard,
                    enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 8 },
                ) {
                    GlassCard(borderColor = MintGreen.copy(alpha = 0.35f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Webino3DGlassIcon(type = Webino3DIconType.OUTPUTS, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.about_myket_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        WebinoSecondaryButton(
                            text = appString(R.string.about_myket_review_btn),
                            icon = Icons.Rounded.StarRate,
                            accent = SoftOrange,
                            onClick = {
                                if (onRateMyketClick != null) {
                                    onRateMyketClick()
                                } else {
                                    ExternalIntentHelper.openMyketReview(
                                        context = context,
                                        noTargetMessage = noStoreMsg,
                                        onError = onNotify,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("about_myket_review_btn"),
                        )
                        Spacer(Modifier.height(10.dp))
                        WebinoSecondaryButton(
                            text = appString(R.string.about_myket_dev_btn),
                            icon = Icons.Rounded.Apps,
                            accent = ElectricViolet,
                            onClick = {
                                if (onOtherAppsClick != null) {
                                    onOtherAppsClick()
                                } else {
                                    ExternalIntentHelper.openMyketDeveloperApps(
                                        context = context,
                                        noTargetMessage = noStoreMsg,
                                        onError = onNotify,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("about_myket_dev_btn"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBadgeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    val tokens = WebinoThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = tokens.subtleSurface,
        border = BorderStroke(1.dp, tokens.navPillBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
