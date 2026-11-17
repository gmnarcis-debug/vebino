package com.example.core

import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

object ValidationUtils {

    private val PACKAGE_REGEX = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){1,6}$")
    private val HEX_COLOR_REGEX = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun isValidWebUrl(raw: String): Boolean {
        val normalized = normalizeUrl(raw)
        if (normalized.isBlank()) return false
        return try {
            val uri = URI(normalized)
            val scheme = uri.scheme?.lowercase(Locale.US)
            val host = uri.host
            (scheme == "https" || scheme == "http") && !host.isNullOrBlank() && host.contains('.')
        } catch (_: Exception) {
            false
        }
    }

    fun isHttpsUrl(raw: String): Boolean {
        val normalized = normalizeUrl(raw)
        return normalized.startsWith("https://", ignoreCase = true)
    }

    fun isIranDomain(raw: String): Boolean {
        val normalized = normalizeUrl(raw)
        return try {
            val host = URI(normalized).host?.lowercase(Locale.US) ?: return false
            host.endsWith(".ir") || host.endsWith(".xn--mgba3a4f16a")
        } catch (_: Exception) {
            false
        }
    }

    fun extractHost(raw: String): String {
        val normalized = normalizeUrl(raw)
        return try {
            URI(normalized).host?.lowercase(Locale.US) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /** Reserved Java words cannot be package segments: the generated Java sources would not compile. */
    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null",
    )

    fun isValidPackageName(pkg: String): Boolean {
        val trimmed = pkg.trim()
        return PACKAGE_REGEX.matcher(trimmed).matches() &&
            !trimmed.contains("..") &&
            trimmed.split('.').none { it in JAVA_KEYWORDS }
    }

    fun suggestPackageName(url: String, appName: String): String {
        val host = extractHost(url).removePrefix("www.")
        if (host.isNotBlank()) {
            val parts = host.split('.')
                .map { seg -> seg.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "") }
                .filter { it.isNotEmpty() }
                .reversed()
            if (parts.size >= 2) {
                val safeParts = parts.mapIndexed { idx, p ->
                    val cleaned = if (p.first().isDigit()) "app$p" else p
                    if (cleaned in JAVA_KEYWORDS || (idx == 0 && cleaned in setOf("in", "is", "as"))) {
                        "site$cleaned"
                    } else {
                        cleaned
                    }
                }
                val candidate = safeParts.joinToString(".")
                if (isValidPackageName(candidate)) return candidate
            }
        }
        val slug = appName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifBlank { "webapp" }
        val prefix = if (slug.first().isDigit() || slug in JAVA_KEYWORDS) "app$slug" else slug
        return "ir.webino.$prefix"
    }

    fun isValidHexColor(hex: String): Boolean = HEX_COLOR_REGEX.matcher(hex.trim()).matches()

    fun formatFileSize(bytes: Long, isPersian: Boolean, usePersianDigits: Boolean): String {
        val raw = when {
            bytes < 1024L -> if (isPersian) "$bytes بایت" else "$bytes B"
            bytes < 1024L * 1024L -> {
                val kb = String.format(Locale.US, "%.1f", bytes / 1024.0)
                if (isPersian) "$kb کیلوبایت" else "$kb KB"
            }
            bytes < 1024L * 1024L * 1024L -> {
                val mb = String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024.0))
                if (isPersian) "$mb مگابایت" else "$mb MB"
            }
            else -> {
                val gb = String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024.0 * 1024.0))
                if (isPersian) "$gb گیگابایت" else "$gb GB"
            }
        }
        return JalaliCalendar.localizeDigits(raw, usePersianDigits && isPersian)
    }

    fun formatDuration(millis: Long, isPersian: Boolean, usePersianDigits: Boolean): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        val raw = if (min > 0) {
            if (isPersian) "$min دقیقه و $sec ثانیه" else "${min}m ${sec}s"
        } else {
            if (isPersian) "$sec ثانیه" else "${sec}s"
        }
        return JalaliCalendar.localizeDigits(raw, usePersianDigits && isPersian)
    }
}
