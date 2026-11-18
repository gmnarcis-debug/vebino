package com.example.domain.model

import kotlinx.serialization.Serializable

enum class AppLanguage { FA, EN }
enum class AppThemeMode { NAVY_DARK, LIGHT, SYSTEM }
enum class AppFontChoice { VAZIRMATN, LALEZAR, NOTO_NASKH }
enum class CalendarType { JALALI, GREGORIAN }

enum class BuildEngineStatus(val titleFa: String, val titleEn: String) {
    NOT_CHECKED("بررسی نشده", "Not Checked"),
    CHECKING("در حال بررسی...", "Checking..."),
    READY("آماده ساخت محلی", "Ready"),
    UNSUPPORTED("پشتیبانی نمی‌شود", "Unsupported"),
    ERROR("خطای محیط", "Error"),
}

enum class AppTemplateType(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val primaryHex: String,
    val backgroundHex: String,
) {
    STORE("store", "فروشگاهی", "E-Commerce", "مناسب فروشگاه اینترنتی با پشتیبانی سبد خرید، درگاه پرداخت و کش هوشمند", "#7C3AED", "#0B1026"),
    CORPORATE("corporate", "شرکتی", "Corporate", "ظاهر رسمی، لود سریع و لینکهای مستقیم تماس و نقشه", "#0284C7", "#0F172A"),
    NEWS("news", "خبری", "News Portal", "کش بهینه برای مقالات، فونت خوانا و نوار اشتراکگذاری", "#DC2626", "#111827"),
    EDUCATIONAL("educational", "آموزشی", "LMS & Academy", "پشتیبانی تمامصفحه ویدئو، آپلود تکلیف و دانلود جزوه", "#059669", "#061E1A"),
    PERSONAL("personal", "شخصی", "Portfolio", "سبک، مینیمال، بدون نوار ابزار اضافی و Splash سریع", "#06B6D4", "#0B1026"),
    SOCIAL("social", "شبکه اجتماعی", "Community", "پشتیبانی کامل دوربین، میکروفون، کوکیهای نشست و آپلود فایل", "#EC4899", "#160B24"),
    BLOG("blog", "وبلاگ", "Blog & Magazine", "حالت مطالعه روان، نوار پایین سبک و به اشتراکگذاری مطالب", "#D97706", "#1C1408"),
    SERVICE("service", "خدماتی", "Service App", "دسترسی مکان، تماس مستقیم، نقشه و پرداخت امن خدمات", "#4F46E5", "#0B1026"),
}

enum class ThemePreset(val titleFa: String, val titleEn: String) {
    LIGHT("تم روشن", "Light"),
    DARK("تم تاریک", "Dark"),
    CUSTOM("تم سفارشی", "Custom"),
}

enum class ToolbarStyle(val titleFa: String, val titleEn: String) {
    NONE("تمامصفحه بدون نوار", "Fullscreen (No Bar)"),
    TOP_BAR("نوار ابزار بالا", "Top Toolbar"),
    BOTTOM_BAR("نوار ناوبری پایین", "Bottom Bar"),
    FLOATING_PILL("دکمه شناور کنترل", "Floating Controls"),
}

enum class LoadingAnimationType(val titleFa: String, val titleEn: String) {
    PROGRESS_BAR("نوار پیشرفت خطی", "Linear Progress"),
    CIRCULAR_PULSE("حلقه چرخان مدرن", "Circular Pulse"),
    SHIMMER_WAVE("موج بارگذاری", "Shimmer Wave"),
    DOTS_ORBIT("نقاط متحرک", "Orbiting Dots"),
}

enum class OrientationMode(val titleFa: String, val manifestValue: String) {
    AUTO("چرخش خودکار", "unspecified"),
    PORTRAIT("فقط عمودی", "portrait"),
    LANDSCAPE("فقط افقی", "landscape"),
}

enum class SslPolicy(val titleFa: String, val descriptionFa: String) {
    STRICT_BLOCK("مسدودسازی کامل (امنیت حداکثری)", "خطاهای گواهی SSL را بهطور کامل رد میکند و صفحه را باز نمیکند."),
    WARN_USER("نمایش هشدار امنیتی و عدم عبور پیشفرض", "صفحه خطای امنیتی نمایش داده میشود تا کاربر از ریسک مطلع شود."),
}

enum class CacheModeOption(val titleFa: String, val androidConst: Int) {
    DEFAULT("هوشمند (پیشفرض سیستم)", -1),
    NO_CACHE("بدون کش (همیشه آنلاین)", 2),
    CACHE_ELSE_NETWORK("استفاده از کش در نبود اینترنت", 1),
}

enum class MixedContentOption(val titleFa: String) {
    NEVER_ALLOW("مسدودسازی محتوای غیرHTTPS (پیشنهادی)"),
    COMPATIBILITY("حالت سازگاری محدود با منابع جانبی"),
}

enum class ExternalLinkMode(val titleFa: String) {
    OPEN_IN_SYSTEM_BROWSER("باز کردن دامنههای خارجی در مرورگر گوشی"),
    KEEP_IN_APP("باز ماندن تمام لینکها داخل اپلیکیشن"),
    ASK_OR_ALLOWLIST("مدیریت هوشمند درگاهها و اپلیکیشنها"),
}

enum class PopupBehaviorMode(val titleFa: String, val descriptionFa: String) {
    SAME_PAGE("باز شدن در همان صفحه", "پنجرههای جدید و target=_blank در همان صفحه اصلی بارگذاری میشوند."),
    INTERNAL_WINDOW("باز شدن در پنجره داخلی جدید", "یک پنجره مودال داخلی روی اپ باز میشود و با بستن آن به صفحه قبل برمیگردید."),
    EXTERNAL_BROWSER("باز شدن در مرورگر خارجی", "لینکهای پاپآپ مستقیماً در مرورگر پیشفرض گوشی باز میشوند."),
}

enum class BackNavigationBehavior(val titleFa: String, val descriptionFa: String) {
    HISTORY_THEN_EXIT("بازگشت در تاریخچه WebView و خروج", "تا زمانی که صفحه قبلی وجود دارد برمیگردد و در انتها خارج میشود."),
    HISTORY_THEN_CONFIRM_EXIT("بازگشت در تاریخچه با تایید خروج", "در صفحه اول پیش از خروج از کاربر تاییدیه میگیرد."),
    DIRECT_EXIT("خروج مستقیم با دکمه بازگشت", "بدون پیمایش تاریخچه WebView از صفحه خارج میشود."),
}

enum class BuildOutputFormat(val titleFa: String, val extension: String) {
    APK("فایل نصبی APK", "apk"),
    AAB("بسته انتشار AAB", "aab"),
    BOTH("هردو", "both"),
}

enum class BuildStep(val order: Int, val titleFa: String, val titleEn: String, val weightPercent: Int) {
    CHECKING_ENV(1, "بررسی محیط ساخت", "Checking Environment", 10),
    PREPARING_RESOURCES(2, "آمادهسازی منابع", "Preparing Resources", 22),
    GENERATING_PROJECT(3, "تولید پروژه", "Generating Project", 36),
    COMPILING(4, "کامپایل کد جاوا و DEX", "Compiling Java & DEX", 58),
    PACKAGING(5, "بستهبندی APK / AAB", "Packaging Archive", 74),
    SIGNING(6, "امضای دیجیتال", "Digital Signing", 88),
    VALIDATING(7, "اعتبارسنجی خروجی", "Validating Output", 96),
    COMPLETED(8, "تکمیل خروجی", "Completed", 100),
}

enum class BuildStatus(val titleFa: String) {
    QUEUED("در صف ساخت"),
    RUNNING("در حال ساخت"),
    SUCCEEDED("ساخت موفق"),
    FAILED("خطا در ساخت"),
    CANCELLED("لغو شده"),
}

@Serializable
data class SiteMetadata(
    val requestedUrl: String,
    val finalUrl: String,
    val title: String,
    val description: String,
    val faviconUrl: String?,
    val themeColorHex: String?,
    val isHttps: Boolean,
    val isIranDomain: Boolean,
    val httpStatusCode: Int,
    val isReachable: Boolean,
    val errorMessage: String? = null,
)

data class EnvironmentCheckReport(
    val isReady: Boolean,
    val status: BuildEngineStatus = if (isReady) BuildEngineStatus.READY else BuildEngineStatus.ERROR,
    val freeStorageMb: Long,
    val freeRamMb: Long,
    val totalRamMb: Long,
    val hasEmbeddedStubs: Boolean,
    val hasJavaCompiler: Boolean,
    val hasDexCompiler: Boolean,
    val hasApkSigner: Boolean,
    val hasKeystoreCrypto: Boolean,
    val androidSdkInt: Int,
    val issues: List<String>,
)
