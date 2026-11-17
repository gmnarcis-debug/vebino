package com.example.webviewengine

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

enum class DownloadItemStatus(val titleFa: String) {
    PENDING("در صف دانلود"),
    RUNNING("در حال دانلود"),
    PAUSED("متوقف‌شده"),
    SUCCESSFUL("تکمیل شد"),
    FAILED("ناموفق"),
    CANCELLED("لغو شده"),
}

data class WebDownloadItem(
    val downloadId: Long,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val progressPercent: Int = 0,
    val status: DownloadItemStatus = DownloadItemStatus.PENDING,
    val localUri: String? = null,
    val errorReason: String? = null,
)

/**
 * Real Android DownloadManager & SAF integration with:
 * - URL & MIME validation (blocks unsafe auto-executable payloads)
 * - Path-traversal-safe filename sanitization & duplicate filename avoidance
 * - Real progress polling, cancel support, and safe document viewing
 */
class WebDownloadManager(
    private val context: Context,
) {
    private val blockedExtensions = setOf("apk", "xapk", "apks", "dex", "jar", "so", "exe", "bat", "sh")

    fun validateAndSanitize(
        url: String,
        contentDisposition: String?,
        mimetype: String?,
    ): Triple<Uri, String, String>? {
        if (url.isBlank()) return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        if (scheme != "https" && scheme != "http") return null

        val guessed = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val cleanName = guessed
            .replace("..", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_')
            .take(96)
            .ifBlank { "download.bin" }

        val ext = cleanName.substringAfterLast('.', "").lowercase(Locale.US)
        if (ext in blockedExtensions) return null

        val safeMime = mimetype?.takeIf { it.contains('/') } ?: when (ext) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            else -> "application/octet-stream"
        }

        return Triple(uri, makeUniqueFilename(cleanName), safeMime)
    }

    fun makeUniqueFilename(desiredName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir == null || !downloadsDir.exists()) return desiredName
        if (!File(downloadsDir, desiredName).exists()) return desiredName

        val base = desiredName.substringBeforeLast('.', desiredName)
        val ext = desiredName.substringAfterLast('.', "")
        for (i in 1..99) {
            val candidate = if (ext.isNotEmpty()) "${base}_($i).$ext" else "${base}_($i)"
            if (!File(downloadsDir, candidate).exists()) return candidate
        }
        return "${System.currentTimeMillis()}_$desiredName"
    }

    fun handleDownload(
        url: String,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long = -1L,
        enabled: Boolean,
        onExternalFallback: (Uri) -> Unit,
    ): WebDownloadItem? {
        if (!enabled) return null
        val (uri, uniqueName, safeMime) = validateAndSanitize(url, contentDisposition, mimetype) ?: return null

        val enqueuedId = runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return@runCatching -1L
            val request = DownloadManager.Request(uri).apply {
                setMimeType(safeMime)
                setTitle(uniqueName)
                setDescription("دانلود از وبینو")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // Without the session cookie, downloads from logged-in sites return a login page instead of the file.
                runCatching { android.webkit.CookieManager.getInstance().getCookie(uri.toString()) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { addRequestHeader("Cookie", it) }
                runCatching { android.webkit.WebSettings.getDefaultUserAgent(context) }
                    .getOrNull()
                    ?.let { addRequestHeader("User-Agent", it) }
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uniqueName)
            }
            dm.enqueue(request)
        }.getOrDefault(-1L)

        if (enqueuedId < 0L) {
            onExternalFallback(uri)
            return null
        }

        return WebDownloadItem(
            downloadId = enqueuedId,
            url = url,
            fileName = uniqueName,
            mimeType = safeMime,
            totalBytes = contentLength,
            downloadedBytes = 0L,
            progressPercent = 0,
            status = DownloadItemStatus.RUNNING,
        )
    }

    fun refreshDownloadStatus(item: WebDownloadItem): WebDownloadItem {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return item
        val cursor = runCatching { dm.query(DownloadManager.Query().setFilterById(item.downloadId)) }.getOrNull()
            ?: return item
        cursor.use { c ->
            if (!c.moveToFirst()) return item
            val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val dlIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

            val rawStatus = if (statusIdx >= 0) c.getInt(statusIdx) else DownloadManager.STATUS_RUNNING
            val dlBytes = if (dlIdx >= 0) c.getLong(dlIdx) else item.downloadedBytes
            val total = if (totalIdx >= 0) c.getLong(totalIdx) else item.totalBytes
            val localUri = if (uriIdx >= 0) c.getString(uriIdx) else item.localUri

            val mappedStatus = when (rawStatus) {
                DownloadManager.STATUS_PENDING -> DownloadItemStatus.PENDING
                DownloadManager.STATUS_RUNNING -> DownloadItemStatus.RUNNING
                DownloadManager.STATUS_PAUSED -> DownloadItemStatus.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadItemStatus.SUCCESSFUL
                DownloadManager.STATUS_FAILED -> DownloadItemStatus.FAILED
                else -> item.status
            }
            val pct = when {
                mappedStatus == DownloadItemStatus.SUCCESSFUL -> 100
                total > 0L -> ((dlBytes * 100L) / total).toInt().coerceIn(0, 100)
                else -> item.progressPercent
            }
            return item.copy(
                downloadedBytes = dlBytes,
                totalBytes = total,
                progressPercent = pct,
                status = mappedStatus,
                localUri = localUri,
                errorReason = if (mappedStatus == DownloadItemStatus.FAILED) "خطا در دریافت فایل از سرور." else null,
            )
        }
    }

    fun cancelDownload(downloadId: Long): Boolean = runCatching {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
        dm.remove(downloadId) > 0
    }.getOrDefault(false)

    fun openDownloadedFile(item: WebDownloadItem): Boolean {
        val rawUri = item.localUri ?: return false
        val parsed = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return false
        val viewUri = if (parsed.scheme == "file" && parsed.path != null) {
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(parsed.path!!))
            }.getOrDefault(parsed)
        } else {
            parsed
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewUri, item.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, item.fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }
}
