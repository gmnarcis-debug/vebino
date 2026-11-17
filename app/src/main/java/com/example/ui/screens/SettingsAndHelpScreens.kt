package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import red.line.vebino.BuildConfig
import red.line.vebino.R
import com.example.core.ValidationUtils
import com.example.core.appString
import com.example.data.local.prefs.UserPreferences
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.CalendarType
import com.example.ui.components.GlassCard
import com.example.ui.components.SettingToggleRow
import com.example.ui.components.StatusBadge
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.components.localizeNumber
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.StorageStats

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    storageStats: StorageStats,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onFontChange: (AppFontChoice) -> Unit,
    onCalendarChange: (CalendarType) -> Unit,
    onPersianDigitsChange: (Boolean) -> Unit,
    onClearCacheOnExitChange: (Boolean) -> Unit,
    onRedactLogsChange: (Boolean) -> Unit,
    onClearBuildCache: () -> Unit,
    onOpenSigningManager: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenBuildHistory: () -> Unit = {},
    onReplayTutorial: () -> Unit = {},
) {
    val isFa = prefs.language == AppLanguage.FA
    val usePersianDigits = prefs.usePersianDigits

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Webino3DGlassIcon(type = Webino3DIconType.SETTINGS, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = appString(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, // اصلاح رنگ
                    )
                    Text(
                        text = if (isFa) "ظاهر، زبان، امنیت امضا و فضای ذخیره‌سازی" else "Appearance, language, signing & storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            GlassCard {
                Text(
                    text = appString(R.string.settings_theme_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.LIGHT,
                        onClick = { onThemeChange(AppThemeMode.LIGHT) },
                        label = { Text(appString(R.string.settings_theme_light)) },
                        modifier = Modifier.testTag("settings_theme_light"),
                    )
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.NAVY_DARK,
                        onClick = { onThemeChange(AppThemeMode.NAVY_DARK) },
                        label = { Text(appString(R.string.settings_theme_dark)) },
                        modifier = Modifier.testTag("settings_theme_dark"),
                    )
                    FilterChip(
                        selected = prefs.themeMode == AppThemeMode.SYSTEM,
                        onClick = { onThemeChange(AppThemeMode.SYSTEM) },
                        label = { Text(appString(R.string.settings_theme_system)) },
                        modifier = Modifier.testTag("settings_theme_system"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.fontChoice == AppFontChoice.VAZIRMATN,
                        onClick = { onFontChange(AppFontChoice.VAZIRMATN) },
                        label = { Text(appString(R.string.settings_font_vazirmatn)) },
                        modifier = Modifier.testTag("settings_font_vazirmatn"),
                    )
                    FilterChip(
                        selected = prefs.fontChoice == AppFontChoice.LALEZAR,
                        onClick = { onFontChange(AppFontChoice.LALEZAR) },
                        label = { Text(appString(R.string.settings_font_lalezar)) },
                        modifier = Modifier.testTag("settings_font_lalezar"),
                    )
                    FilterChip(
                        selected = prefs.fontChoice == AppFontChoice.NOTO_NASKH,
                        onClick = { onFontChange(AppFontChoice.NOTO_NASKH) },
                        label = { Text(appString(R.string.settings_font_noto)) },
                        modifier = Modifier.testTag("settings_font_noto"),
                    )
                }
            }
        }

        item {
            GlassCard {
                Text(
                    text = appString(R.string.settings_language_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                )
                Spacer(Modifier.height(10.dp))
                Text(appString(R.string.settings_language_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.language == AppLanguage.FA,
                        onClick = { onLanguageChange(AppLanguage.FA) },
                        label = { Text("فارسی (RTL)") },
                        modifier = Modifier.testTag("settings_lang_fa"),
                    )
                    FilterChip(
                        selected = prefs.language == AppLanguage.EN,
                        onClick = { onLanguageChange(AppLanguage.EN) },
                        label = { Text("English (LTR)") },
                        modifier = Modifier.testTag("settings_lang_en"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(appString(R.string.settings_calendar_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.calendarType == CalendarType.JALALI,
                        onClick = { onCalendarChange(CalendarType.JALALI) },
                        label = { Text(if (isFa) "هجری شمسی" else "Jalali (Solar)") },
                    )
                    FilterChip(
                        selected = prefs.calendarType == CalendarType.GREGORIAN,
                        onClick = { onCalendarChange(CalendarType.GREGORIAN) },
                        label = { Text(if (isFa) "میلادی" else "Gregorian") },
                    )
                }
                SettingToggleRow(
                    title = appString(R.string.settings_persian_digits),
                    description = appString(R.string.settings_persian_digits_desc),
                    checked = prefs.usePersianDigits,
                    onCheckedChange = onPersianDigitsChange,
                )
            }
        }

        item {
            GlassCard(onClick = onOpenSigningManager) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Webino3DGlassIcon(type = Webino3DIconType.SIGNING, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appString(R.string.settings_signing_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                        Text(
                            text = if (isFa) "ایجاد، وارد کردن، تغییر رمز و پشتیبان‌گیری امن کلیدهای امضا" else "Create, import, rotate passwords and backup signing keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Text(
                    text = appString(R.string.settings_storage_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = if (isFa) "خروجی: ${ValidationUtils.formatFileSize(storageStats.outputsBytes, true, usePersianDigits)}" else "Outputs: ${ValidationUtils.formatFileSize(storageStats.outputsBytes, false, false)}",
                        color = SkyBlue,
                    )
                    StatusBadge(
                        text = if (isFa) "کش: ${ValidationUtils.formatFileSize(storageStats.cacheBytes, true, usePersianDigits)}" else "Cache: ${ValidationUtils.formatFileSize(storageStats.cacheBytes, false, false)}",
                        color = ElectricViolet,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isFa) {
                        "رسانه پروژه‌ها: ${ValidationUtils.formatFileSize(storageStats.mediaBytes, true, usePersianDigits)}"
                    } else {
                        "Project Media: ${ValidationUtils.formatFileSize(storageStats.mediaBytes, false, false)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearBuildCache,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = SkyBlue)
                    Spacer(Modifier.width(8.dp))
                    Text(appString(R.string.settings_clear_cache_btn))
                }
                Spacer(Modifier.height(6.dp))
                SettingToggleRow(
                    title = if (isFa) "پاکسازی کش پیش‌نمایش هنگام خروج" else "Clear Preview Cache on Exit",
                    description = if (isFa) "حذف کوکی‌ها و کش موقت تست وب‌سایت پس از بستن پیش‌نمایش" else "Wipe temporary preview cookies and cache when leaving preview",
                    checked = prefs.clearPreviewCacheOnExit,
                    onCheckedChange = onClearCacheOnExitChange,
                    icon = Icons.Rounded.PrivacyTip,
                )
                SettingToggleRow(
                    title = if (isFa) "محافظت گزارش‌ها (Log Redaction)" else "Log Redaction",
                    description = if (isFa) "عدم ثبت رمزها یا کلیدهای خصوصی در گزارش‌های ساخت" else "Mask passwords and secrets from build logs",
                    checked = prefs.redactSensitiveLogs,
                    onCheckedChange = onRedactLogsChange,
                )
            }
        }

        item {
            GlassCard(onClick = onOpenHelpCenter) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Webino3DGlassIcon(type = Webino3DIconType.HELP, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appString(R.string.settings_help_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                        Text(
                            text = if (isFa) "آموزش ساخت اپلیکیشن، نکات امنیتی و راهنمای فنی موتور Build" else "Build architecture, signing safety and store publishing tips",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(onClick = onOpenBuildHistory, modifier = Modifier.testTag("settings_history_card")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Webino3DGlassIcon(type = Webino3DIconType.HISTORY, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "تاریخچه‌ی ساخت و لاگ" else "Build history & logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                        Text(
                            text = if (isFa) "مشاهده و ارسال لاگ ساخت‌ها برای پشتیبانی" else "View and send build logs to support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(onClick = onReplayTutorial, modifier = Modifier.testTag("settings_tutorial_card")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Webino3DGlassIcon(type = Webino3DIconType.TEMPLATES, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "آموزش مرحله‌به‌مرحله" else "Step-by-step tutorial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                        Text(
                            text = if (isFa) "مرور دوباره‌ی آموزش ساخت اپلیکیشن" else "Replay the app-building walkthrough",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(
                onClick = onOpenAbout,
                borderColor = CyanAccent.copy(alpha = 0.35f),
                modifier = Modifier.testTag("settings_about_card"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = CyanAccent)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appString(R.string.settings_about_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                        val localizedVer = localizeNumber(BuildConfig.VERSION_NAME, prefs.language, usePersianDigits)
                        Text(
                            text = if (isFa) {
                                "نسخه $localizedVer • تیم ردلاین سافت البرز • پشتیبانی و مایکت"
                            } else {
                                "Version ${BuildConfig.VERSION_NAME} • RedLine Soft Alborz • Support & Myket"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WebinoScreenHeader(
                title = "راهنما و استاندارد انتشار مایکت",
                subtitle = "آشنایی با موتور ساخت محلی و نکات فروشگاه‌ها",
                iconType = Webino3DIconType.HELP,
                onBack = onBack,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Webino3DGlassIcon(type = Webino3DIconType.BUILD, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "معماری واقعی ساخت روی گوشی (On-Device Build)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "در سیستم‌های دسکتاپ، ابزارهایی مثل Gradle و AAPT2 نیاز به اجرای باینری‌های دسکتاپ و ۲ تا ۴ گیگابایت رم دارند که در محیط محدود Sandbox اندروید بدون روت قابل اجرا نیستند.\n" +
                            "وبینو به جای کپی کردن فایل آماده یا نیاز به سرور ابری، از یک زنجیره کامپایلر خالص JVM درون‌فرایند استفاده می‌کند:\n" +
                            "• تولید کد Java اختصاصی برای بسته و تنظیمات شما\n" +
                            "• کامپایل با Eclipse JDT Compiler (ECJ) روی دستگاه\n" +
                            "• تبدیل بایت‌کد به classes.dex با موتور Dalvik DX\n" +
                            "• کامپایل باینری AndroidManifest.xml و resources.arsc\n" +
                            "• هم‌ترازی ۴بایتی (ZipAlign) و امضای v1/v2/v3 با Google apksig\n" +
                            "• تولید بسته AAB با Protobuf و امضای CMS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                    )
                }
            }

            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Storefront, contentDescription = null, tint = SuccessEmerald)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "چک‌لیست انتشار در مایکت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "استفاده از Package Name اختصاصی و منحصربه‌فرد (مثال: ir.shop.app)",
                        "ساخت یا وارد کردن کلید امضای اختصاصی و نگهداری پشتیبان امن آن",
                        "فعال نگه‌داشتن HTTPS-Only و سیاست مسدودسازی خطای SSL",
                        "تست پیش‌نمایش زنده قبل از تولید نسخه نهایی",
                    ).forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = SkyBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "پشتیبانی فنی وبینو",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // اصلاح رنگ
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "در صورت بروز خطا در کامپایل یا تنظیم دامنه‌های App Links، گزارش Build را از صفحه مدیریت ساخت بررسی کنید. تمام اطلاعات پروژه و کلیدها به‌صورت محلی و رمزنگاری‌شده روی گوشی شما نگهداری می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}