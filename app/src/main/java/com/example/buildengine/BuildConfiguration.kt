package com.example.buildengine

import android.graphics.BitmapFactory
import com.example.core.ValidationUtils
import com.example.domain.model.BuildOutputFormat

/**
 * Immutable validated configuration derived from a BuildSpec before starting
 * on-device compilation and packaging.
 */
data class BuildConfiguration(
    val spec: BuildSpec,
    val wantApk: Boolean = spec.outputFormat == BuildOutputFormat.APK || spec.outputFormat == BuildOutputFormat.BOTH,
    val wantAab: Boolean = spec.outputFormat == BuildOutputFormat.AAB || spec.outputFormat == BuildOutputFormat.BOTH,
) {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
    ) {
        val primaryError: String?
            get() = errors.firstOrNull()
    }

    companion object {
        fun validate(spec: BuildSpec): ValidationResult {
            val errors = mutableListOf<String>()

            if (spec.projectId <= 0L) {
                errors.add("شناسه پروژه نامعتبر است.")
            }
            val cleanAppName = spec.appName.trim()
            if (cleanAppName.isEmpty()) {
                errors.add("نام اپلیکیشن نمی‌تواند خالی باشد.")
            } else if (cleanAppName.length > 50) {
                errors.add("نام اپلیکیشن نباید بیش از ۵۰ کاراکتر باشد.")
            }

            val normalizedUrl = ValidationUtils.normalizeUrl(spec.websiteUrl)
            if (!ValidationUtils.isValidWebUrl(normalizedUrl)) {
                errors.add("آدرس وب‌سایت معتبر نیست (مثال: https://example.com).")
            } else if (spec.httpsOnly && normalizedUrl.startsWith("http://", ignoreCase = true)) {
                errors.add("نشانی با http:// تنظیم شده است در حالی که حالت HTTPS اجباری فعال است.")
            }

            if (!ValidationUtils.isValidPackageName(spec.packageName)) {
                errors.add("نام بسته (Package Name) نامعتبر است؛ از فرمت استاندارد مثل ir.webino.app استفاده کنید.")
            }

            if (spec.versionName.isBlank() || spec.versionName.contains('\n') || spec.versionName.contains('\r')) {
                errors.add("نام نسخه (Version Name) نامعتبر است.")
            }

            if (spec.versionCode < 1) {
                errors.add("کد نسخه (Version Code) باید حداقل ۱ باشد.")
            }

            if (spec.minSdk < 21 || spec.minSdk > 36) {
                errors.add("حداقل نسخه اندروید (minSdk) باید بین ۲۱ تا ۳۶ باشد.")
            }
            if (spec.targetSdk < spec.minSdk || spec.targetSdk > 36) {
                errors.add("نسخه هدف (targetSdk) باید بزرگتر یا مساوی minSdk و حداکثر ۳۶ باشد.")
            }

            if (!ValidationUtils.isValidHexColor(spec.primaryColorHex)) {
                errors.add("کد رنگ اصلی (${spec.primaryColorHex}) نامعتبر است.")
            }
            if (!ValidationUtils.isValidHexColor(spec.backgroundColorHex)) {
                errors.add("کد رنگ پس‌زمینه (${spec.backgroundColorHex}) نامعتبر است.")
            }

            if (spec.iconPngBytes != null) {
                val decoded = BitmapFactory.decodeByteArray(spec.iconPngBytes, 0, spec.iconPngBytes.size)
                if (decoded == null || decoded.width <= 0 || decoded.height <= 0) {
                    errors.add("فایل آیکون انتخاب‌شده قابل خواندن یا رمزگشایی نیست.")
                }
            }

            if (spec.splashPngBytes != null) {
                val decoded = BitmapFactory.decodeByteArray(spec.splashPngBytes, 0, spec.splashPngBytes.size)
                if (decoded == null || decoded.width <= 0 || decoded.height <= 0) {
                    errors.add("فایل تصویر Splash انتخاب‌شده نامعتبر است.")
                }
            }

            if (spec.keyAlias.isBlank()) {
                errors.add("نام مستعار کلید امضا (Key Alias) مشخص نشده است.")
            }
            if (spec.certificateChain.isEmpty()) {
                errors.add("گواهی دیجیتال X.509 برای امضای خروجی یافت نشد.")
            } else {
                val cert = spec.certificateChain.first()
                runCatching { cert.checkValidity() }.onFailure {
                    errors.add("گواهی دیجیتال امضا منقضی شده یا هنوز معتبر نیست.")
                }
            }

            return ValidationResult(isValid = errors.isEmpty(), errors = errors)
        }

        fun fromSpecOrThrow(spec: BuildSpec): BuildConfiguration {
            val result = validate(spec)
            require(result.isValid) { result.errors.joinToString("\n") }
            return BuildConfiguration(spec = spec)
        }
    }
}
