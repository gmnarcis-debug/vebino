package com.example.webviewengine

import android.content.Context
import android.net.http.SslError
import android.webkit.SslErrorHandler
import com.example.domain.model.SslPolicy

enum class WebErrorCategory(val titleFa: String) {
    NO_INTERNET("عدم اتصال به اینترنت"),
    DNS_FAILURE("خطا در شناسایی دامنه (DNS)"),
    TIMEOUT("اتمام زمان پاسخگویی سرور"),
    HTTP_NOT_FOUND("صفحه مورد نظر پیدا نشد (۴۰۴)"),
    HTTP_SERVER_ERROR("خطای داخلی سرور وب‌سایت"),
    HTTP_ERROR("خطای پروتکل HTTP"),
    SSL_ERROR("خطای امنیتی گواهی SSL"),
    FILE_LOAD_ERROR("خطا در بارگذاری فایل یا منبع"),
    WEBVIEW_ERROR("خطای موتور نمایش صفحه"),
}

data class WebErrorInfo(
    val category: WebErrorCategory,
    val titleFa: String,
    val messageFa: String,
    val technicalDetail: String,
    val canRetry: Boolean = true,
)

sealed interface SslResolution {
    data object CancelledStrictly : SslResolution
    data class RequireExplicitUserWarning(val url: String) : SslResolution
}

/**
 * Classifies WebView network, DNS, Timeout, HTTP, File, and SSL failures into
 * clear Persian diagnostics without leaking sensitive URL tokens or cookies.
 */
class WebErrorHandler(private val context: Context) {

    private val connectivityManager = WebConnectivityManager(context)
    private var pendingSslHandler: SslErrorHandler? = null

    fun classifyNetworkError(errorCode: Int, rawDescription: String?, failingUrl: String?): WebErrorInfo {
        val desc = rawDescription.orEmpty()
        val offline = !connectivityManager.isNetworkAvailable()
        val safeUrl = WebSecurityManager.sanitizeUrlForLog(failingUrl)

        return when {
            desc.contains("INTERNET_DISCONNECTED", ignoreCase = true) -> WebErrorInfo(
                category = WebErrorCategory.NO_INTERNET,
                titleFa = WebErrorCategory.NO_INTERNET.titleFa,
                messageFa = "اتصال اینترنت دستگاه برقرار نیست. پس از اتصال به Wi-Fi یا داده همراه، صفحه به‌صورت خودکار یا با دکمه تلاش مجدد بارگذاری می‌شود.",
                technicalDetail = "OFFLINE ($errorCode) • $safeUrl",
            )
            desc.contains("NAME_NOT_RESOLVED", ignoreCase = true) || errorCode == -2 -> WebErrorInfo(
                category = WebErrorCategory.DNS_FAILURE,
                titleFa = WebErrorCategory.DNS_FAILURE.titleFa,
                messageFa = "آدرس سرور وب‌سایت پیدا نشد. نشانی سایت یا DNS شبکه خود را بررسی کنید.",
                technicalDetail = "DNS ($errorCode) • $safeUrl",
            )
            desc.contains("TIMED_OUT", ignoreCase = true) || errorCode == -8 -> WebErrorInfo(
                category = WebErrorCategory.TIMEOUT,
                titleFa = WebErrorCategory.TIMEOUT.titleFa,
                messageFa = "پاسخگویی سرور بیش از حد طول کشید. کیفیت اینترنت را بررسی و دوباره تلاش کنید.",
                technicalDetail = "TIMEOUT ($errorCode) • $safeUrl",
            )
            desc.contains("FILE", ignoreCase = true) || errorCode == -13 || errorCode == -14 -> WebErrorInfo(
                category = WebErrorCategory.FILE_LOAD_ERROR,
                titleFa = WebErrorCategory.FILE_LOAD_ERROR.titleFa,
                messageFa = "بارگذاری فایل یا منبع درخواستی امکان‌پذیر نیست.",
                technicalDetail = "FILE ($errorCode) • $safeUrl",
            )
            offline -> WebErrorInfo(
                category = WebErrorCategory.NO_INTERNET,
                titleFa = WebErrorCategory.NO_INTERNET.titleFa,
                messageFa = "اتصال اینترنت دستگاه برقرار نیست. پس از اتصال به Wi-Fi یا داده همراه، صفحه به‌صورت خودکار یا با دکمه تلاش مجدد بارگذاری می‌شود.",
                technicalDetail = "OFFLINE ($errorCode) • $safeUrl",
            )
            else -> WebErrorInfo(
                category = WebErrorCategory.WEBVIEW_ERROR,
                titleFa = WebErrorCategory.WEBVIEW_ERROR.titleFa,
                messageFa = "بارگذاری صفحه با خطا مواجه شد. می‌توانید دوباره تلاش کنید یا به صفحه قبل برگردید.",
                technicalDetail = "${desc.ifBlank { "WEBVIEW_ERR" }} ($errorCode) • $safeUrl",
            )
        }
    }

    fun classifyHttpError(statusCode: Int, failingUrl: String?): WebErrorInfo {
        val safeUrl = WebSecurityManager.sanitizeUrlForLog(failingUrl)
        return when {
            statusCode == 404 -> WebErrorInfo(
                category = WebErrorCategory.HTTP_NOT_FOUND,
                titleFa = WebErrorCategory.HTTP_NOT_FOUND.titleFa,
                messageFa = "صفحه یا فایل درخواستی روی سرور وب‌سایت یافت نشد.",
                technicalDetail = "HTTP 404 • $safeUrl",
            )
            statusCode >= 500 -> WebErrorInfo(
                category = WebErrorCategory.HTTP_SERVER_ERROR,
                titleFa = WebErrorCategory.HTTP_SERVER_ERROR.titleFa,
                messageFa = "سرور وب‌سایت موقتاً با خطای داخلی ($statusCode) مواجه شده است.",
                technicalDetail = "HTTP $statusCode • $safeUrl",
            )
            else -> WebErrorInfo(
                category = WebErrorCategory.HTTP_ERROR,
                titleFa = WebErrorCategory.HTTP_ERROR.titleFa,
                messageFa = "درخواست وب‌سایت با کد وضعیت HTTP $statusCode رد شد.",
                technicalDetail = "HTTP $statusCode • $safeUrl",
            )
        }
    }

    fun formatNetworkError(rawDescription: String?): String =
        classifyNetworkError(-1, rawDescription, null).messageFa

    fun handleSslError(
        handler: SslErrorHandler?,
        error: SslError?,
        fallbackUrl: String,
        policy: SslPolicy,
    ): SslResolution? {
        if (handler == null) return null
        if (policy == SslPolicy.STRICT_BLOCK) {
            handler.cancel()
            pendingSslHandler = null
            return SslResolution.CancelledStrictly
        }
        // Never silently proceed on SSL errors
        pendingSslHandler = handler
        return SslResolution.RequireExplicitUserWarning(error?.url ?: fallbackUrl)
    }

    fun resolvePendingSsl(proceed: Boolean) {
        if (proceed) pendingSslHandler?.proceed() else pendingSslHandler?.cancel()
        pendingSslHandler = null
    }

    fun isNetworkAvailable(): Boolean = connectivityManager.isNetworkAvailable()

    fun startReconnectMonitor(onNetworkRestored: () -> Unit) =
        connectivityManager.startReconnectMonitor(onNetworkRestored)

    fun stopReconnectMonitor() =
        connectivityManager.stopReconnectMonitor()
}
