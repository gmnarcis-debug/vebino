package com.example.buildengine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

/**
 * Generates adaptive/legacy PNG icons, splash image payload, and webino_config.json
 * for the target compiled APK/AAB.
 */
object ResourceGenerator {

    data class GeneratedResources(
        val iconPng: ByteArray,
        val roundIconPng: ByteArray,
        val splashPng: ByteArray,
        val configJsonBytes: ByteArray,
        val primaryColorArgb: Int,
        val backgroundColorArgb: Int,
    )

    fun generate(spec: BuildSpec): GeneratedResources {
        val primaryArgb = parseColorSafe(spec.primaryColorHex, 0xFF7C3AED.toInt())
        val bgArgb = parseColorSafe(spec.backgroundColorHex, 0xFF0B1026.toInt())

        val squareIconBitmap = decodeOrRenderIcon(spec.iconPngBytes, spec.appName, primaryArgb, bgArgb, size = 144)
        val roundIconBitmap = maskCircle(squareIconBitmap)

        val splashBitmap = if (spec.splashPngBytes != null) {
            BitmapFactory.decodeByteArray(spec.splashPngBytes, 0, spec.splashPngBytes.size)
                ?: squareIconBitmap
        } else {
            squareIconBitmap
        }

        val configJson: JsonObject = buildJsonObject {
            put("appName", spec.appName)
            put("packageName", spec.packageName)
            put("versionName", spec.versionName)
            put("versionCode", spec.versionCode)
            put("websiteUrl", spec.websiteUrl)
            put("developerName", spec.developerName)
            put("httpsOnly", spec.httpsOnly)
            put("toolbarStyle", spec.toolbarStyle.name)
            put("loadingAnimation", spec.loadingAnimation.name)
            put("generatedBy", "WEBINO On-Device Engine")
        }

        return GeneratedResources(
            iconPng = bitmapToPng(squareIconBitmap),
            roundIconPng = bitmapToPng(roundIconBitmap),
            splashPng = bitmapToPng(splashBitmap),
            configJsonBytes = configJson.toString().toByteArray(Charsets.UTF_8),
            primaryColorArgb = primaryArgb,
            backgroundColorArgb = bgArgb,
        )
    }

    fun parseColorSafe(hex: String, fallback: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }

    private fun decodeOrRenderIcon(
        rawBytes: ByteArray?,
        appName: String,
        primaryArgb: Int,
        bgArgb: Int,
        size: Int,
    ): Bitmap {
        if (rawBytes != null) {
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (decoded != null) {
                return Bitmap.createScaledBitmap(decoded, size, size, true)
            }
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                bgArgb, primaryArgb, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * 0.22f, size * 0.22f, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.05f
            color = Color.WHITE
            alpha = 210
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.31f, ringPaint)

        val letter = appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val bounds = Rect()
        textPaint.getTextBounds(letter, 0, letter.length, bounds)
        canvas.drawText(letter, size / 2f, size / 2f - bounds.exactCenterY(), textPaint)
        return bmp
    }

    private fun maskCircle(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }
}
