package com.example.data.network

import com.example.core.ValidationUtils
import com.example.domain.model.SiteMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * Fetches live website metadata (title, description, favicon, theme-color, HTTPS status)
 * directly from the target URL using OkHttp + Jsoup.
 */
class WebsiteInspector {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun inspect(rawUrl: String): SiteMetadata = withContext(Dispatchers.IO) {
        val normalized = ValidationUtils.normalizeUrl(rawUrl)
        if (!ValidationUtils.isValidWebUrl(normalized)) {
            return@withContext SiteMetadata(
                requestedUrl = rawUrl,
                finalUrl = normalized,
                title = "",
                description = "",
                faviconUrl = null,
                themeColorHex = null,
                isHttps = false,
                isIranDomain = false,
                httpStatusCode = 0,
                isReachable = false,
                errorMessage = "نشانی وبسایت معتبر نیست.",
            )
        }

        try {
            val request = Request.Builder()
                .url(normalized)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/128.0 Mobile Safari/537.36"
                )
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                // Only the <head> matters; cap the read so a huge page cannot exhaust memory.
                val bodyStr = runCatching { response.peekBody(1_500_000L).string() }.getOrDefault("")
                val doc = Jsoup.parse(bodyStr, finalUrl)

                val title = doc.title().trim()
                    .ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty() }
                    .ifBlank { ValidationUtils.extractHost(finalUrl) }

                val description = doc.selectFirst("meta[name=description]")?.attr("content")?.trim()
                    ?.ifBlank { doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim() }
                    .orEmpty()

                val themeColor = doc.selectFirst("meta[name=theme-color]")?.attr("content")?.trim()
                    ?.takeIf { ValidationUtils.isValidHexColor(it) }

                val iconHref = doc.selectFirst("link[rel~=^(apple-touch-icon|icon|shortcut icon)$]")
                    ?.absUrl("href")
                    ?.takeIf { it.isNotBlank() }
                    ?: "${response.request.url.scheme}://${response.request.url.host}/favicon.ico"

                SiteMetadata(
                    requestedUrl = rawUrl,
                    finalUrl = finalUrl,
                    title = title,
                    description = description,
                    faviconUrl = iconHref,
                    themeColorHex = themeColor,
                    isHttps = finalUrl.startsWith("https://", ignoreCase = true),
                    isIranDomain = ValidationUtils.isIranDomain(finalUrl),
                    httpStatusCode = response.code,
                    isReachable = response.isSuccessful,
                    errorMessage = if (response.isSuccessful) null else "کد پاسخ سرور: ${response.code}",
                )
            }
        } catch (e: Exception) {
            SiteMetadata(
                requestedUrl = rawUrl,
                finalUrl = normalized,
                title = ValidationUtils.extractHost(normalized),
                description = "",
                faviconUrl = null,
                themeColorHex = null,
                isHttps = ValidationUtils.isHttpsUrl(normalized),
                isIranDomain = ValidationUtils.isIranDomain(normalized),
                httpStatusCode = 0,
                isReachable = false,
                errorMessage = e.localizedMessage ?: "عدم امکان برقراری ارتباط با وبسایت",
            )
        }
    }
}
