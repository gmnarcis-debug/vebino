package com.example.webviewengine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File

data class UploadRequestOptions(
    val acceptMimeTypes: List<String>,
    val allowMultiple: Boolean,
    val captureEnabled: Boolean,
    val chooserIntent: Intent,
)

/**
 * Coordinates WebView HTML5 file chooser upload requests:
 * - Single & multiple file selection
 * - Images, PDF, and specific MIME type filters
 * - Optional camera photo capture via FileProvider
 * - Clean cancellation & Activity rotation safety
 */
class WebUploadManager(
    private val onFileChooserRequest: (UploadRequestOptions, ValueCallback<Array<Uri>>) -> Unit,
) {
    private var pendingCallback: ValueCallback<Array<Uri>>? = null
    private var pendingCameraOutputUri: Uri? = null

    fun hasPendingChooser(): Boolean = pendingCallback != null

    fun startChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?,
        enabled: Boolean,
    ): Boolean {
        if (!enabled || filePathCallback == null || params == null) {
            filePathCallback?.onReceiveValue(null)
            return false
        }
        pendingCallback?.onReceiveValue(null)
        pendingCallback = filePathCallback
        pendingCameraOutputUri = null

        return runCatching {
            val rawAccept = params.acceptTypes
                ?.flatMap { it.split(',') }
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            val allowMultiple = params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
            val primaryMime = when {
                rawAccept.size == 1 -> normalizeMimeType(rawAccept.first())
                rawAccept.any { it.startsWith("image/") || it.equals(".jpg", true) || it.equals(".png", true) } &&
                    rawAccept.all { it.startsWith("image/") || it.startsWith(".") } -> "image/*"
                else -> "*/*"
            }

            val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = primaryMime
                if (rawAccept.size > 1) {
                    putExtra(Intent.EXTRA_MIME_TYPES, rawAccept.map(::normalizeMimeType).toTypedArray())
                }
                if (allowMultiple) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
            val chooser = Intent.createChooser(contentIntent, "انتخاب فایل برای ارسال")

            val options = UploadRequestOptions(
                acceptMimeTypes = rawAccept.ifEmpty { listOf("*/*") },
                allowMultiple = allowMultiple,
                captureEnabled = params.isCaptureEnabled,
                chooserIntent = chooser,
            )
            onFileChooserRequest(options) { uris ->
                complete(uris)
            }
            true
        }.getOrElse {
            pendingCallback = null
            false
        }
    }

    fun prepareCameraCaptureIntent(context: Context): Pair<Intent, Uri>? = runCatching {
        val dir = File(context.cacheDir, "camera_uploads").apply { mkdirs() }
        val photoFile = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraOutputUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent to uri
    }.getOrNull()

    fun completeWithCameraSuccess(success: Boolean) {
        val uri = pendingCameraOutputUri
        pendingCameraOutputUri = null
        if (success && uri != null) {
            complete(arrayOf(uri))
        } else {
            complete(null)
        }
    }

    fun parseActivityResult(resultCode: Int, data: Intent?): Array<Uri>? {
        val clip = data?.clipData
        if (resultCode == android.app.Activity.RESULT_OK && clip != null && clip.itemCount > 0) {
            return Array(clip.itemCount) { idx -> clip.getItemAt(idx).uri }
        }
        return WebChromeClient.FileChooserParams.parseResult(resultCode, data)
    }

    fun complete(uris: Array<Uri>?) {
        pendingCallback?.onReceiveValue(uris)
        pendingCallback = null
        pendingCameraOutputUri = null
    }

    fun cancelPending() {
        pendingCallback?.onReceiveValue(null)
        pendingCallback = null
        pendingCameraOutputUri = null
    }

    private fun normalizeMimeType(raw: String): String = when (raw.lowercase()) {
        ".pdf" -> "application/pdf"
        ".jpg", ".jpeg" -> "image/jpeg"
        ".png" -> "image/png"
        ".webp" -> "image/webp"
        ".mp4" -> "video/mp4"
        ".mp3" -> "audio/mpeg"
        else -> if (raw.contains('/')) raw else "*/*"
    }
}
