package com.example.webviewengine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat

data class PermissionRationaleRequest(
    val titleFa: String,
    val messageFa: String,
    val origin: String,
    val androidPermissions: List<String>,
    val onConfirmGrant: () -> Unit,
    val onUserDecline: () -> Unit,
)

/**
 * Coordinates Android OS runtime permissions and WebView permission prompts
 * (Camera, Microphone, WebRTC, GPS Geolocation) with:
 * - Origin verification
 * - Device Location service availability check
 * - Mandatory Persian rationale explanation before OS dialog
 * - Graceful deny on rejection or mid-session revocation
 */
class WebPermissionManager(
    private val context: Context,
    private val onRationaleRequired: (PermissionRationaleRequest) -> Unit,
    private val onOsPermissionPrompt: (List<String>, (Boolean) -> Unit) -> Unit,
) {
    private var activeWebRequest: PermissionRequest? = null

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isLocationServiceEnabled(): Boolean = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }.getOrDefault(false)

    fun handleGeolocationPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
        config: WebViewEngineConfig,
        allowedOriginHost: String,
        onStatusNotice: (String) -> Unit = {},
    ) {
        if (!config.geolocationAllowed || callback == null) {
            callback?.invoke(origin, false, false)
            return
        }
        val cleanOrigin = origin.orEmpty()
        val trusted = allowedOriginHost.isBlank() ||
            WebSecurityManager.isOriginTrusted(cleanOrigin, allowedOriginHost, config.httpsOnly)
        if (!trusted) {
            callback.invoke(origin, false, false)
            onStatusNotice("درخواست موقعیت مکانی از دامنه غیرمعتبر مسدود شد.")
            return
        }
        if (!isLocationServiceEnabled()) {
            callback.invoke(origin, false, false)
            onStatusNotice("سرویس مکان‌یاب (GPS) گوشی خاموش است.")
            return
        }
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            callback.invoke(origin, true, false)
            return
        }

        val perms = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        onRationaleRequired(
            PermissionRationaleRequest(
                titleFa = "دسترسی به موقعیت مکانی (GPS)",
                messageFa = "وب‌سایت «$cleanOrigin» برای نمایش نقشه یا ارسال خدمات محلی درخواست دسترسی موقت به موقعیت مکانی دارد.",
                origin = cleanOrigin,
                androidPermissions = perms,
                onConfirmGrant = {
                    onOsPermissionPrompt(perms) { granted ->
                        callback.invoke(origin, granted, false)
                        if (!granted) onStatusNotice("دسترسی موقعیت مکانی توسط کاربر رد شد.")
                    }
                },
                onUserDecline = {
                    callback.invoke(origin, false, false)
                },
            )
        )
    }

    fun handleWebPermissionRequest(
        request: PermissionRequest?,
        config: WebViewEngineConfig,
        allowedOriginHost: String,
        onStatusNotice: (String) -> Unit = {},
    ) {
        if (request == null) return
        val originStr = request.origin?.toString().orEmpty()
        val trusted = allowedOriginHost.isBlank() ||
            WebSecurityManager.isOriginTrusted(originStr, allowedOriginHost, config.httpsOnly)
        if (!trusted) {
            request.deny()
            onStatusNotice("درخواست دسترسی سخت‌افزاری از مبدأ غیرمجاز رد شد.")
            return
        }

        val allowedResources = request.resources.filter { res ->
            (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && config.cameraAllowed) ||
                (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && (config.microphoneAllowed || config.webRtcAllowed))
        }
        if (allowedResources.isEmpty()) {
            request.deny()
            onStatusNotice("دسترسی دوربین یا میکروفون در تنظیمات این پروژه غیرفعال است.")
            return
        }

        val neededOsPerms = buildList {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in allowedResources && !hasPermission(Manifest.permission.CAMERA)) {
                add(Manifest.permission.CAMERA)
            }
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in allowedResources && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (neededOsPerms.isEmpty()) {
            request.grant(allowedResources.toTypedArray())
            return
        }

        activeWebRequest = request
        val featureDesc = buildList {
            if (Manifest.permission.CAMERA in neededOsPerms) add("دوربین")
            if (Manifest.permission.RECORD_AUDIO in neededOsPerms) add("میکروفون")
        }.joinToString(" و ")

        onRationaleRequired(
            PermissionRationaleRequest(
                titleFa = "درخواست دسترسی به $featureDesc",
                messageFa = "وب‌سایت «$originStr» برای تماس تصویری، ضبط صدا یا ثبت تصویر به $featureDesc نیاز دارد. این دسترسی فقط در همین نشست استفاده می‌شود.",
                origin = originStr,
                androidPermissions = neededOsPerms,
                onConfirmGrant = {
                    onOsPermissionPrompt(neededOsPerms) { granted ->
                        if (granted && activeWebRequest == request) {
                            request.grant(allowedResources.toTypedArray())
                        } else {
                            runCatching { request.deny() }
                            onStatusNotice("مجوز $featureDesc صادر نشد و وب‌سایت بدون خطا ادامه می‌دهد.")
                        }
                        if (activeWebRequest == request) activeWebRequest = null
                    }
                },
                onUserDecline = {
                    runCatching { request.deny() }
                    if (activeWebRequest == request) activeWebRequest = null
                },
            )
        )
    }

    fun handleWebPermissionCanceled(request: PermissionRequest?) {
        if (request == activeWebRequest) {
            activeWebRequest = null
        }
    }

    fun requestCameraForUpload(
        origin: String,
        onResult: (Boolean) -> Unit,
    ) {
        if (hasPermission(Manifest.permission.CAMERA)) {
            onResult(true)
            return
        }
        onRationaleRequired(
            PermissionRationaleRequest(
                titleFa = "استفاده از دوربین برای ارسال تصویر",
                messageFa = "برای ثبت عکس جدید جهت بارگذاری در فرم وب‌سایت، مجوز دوربین مورد نیاز است.",
                origin = origin,
                androidPermissions = listOf(Manifest.permission.CAMERA),
                onConfirmGrant = {
                    onOsPermissionPrompt(listOf(Manifest.permission.CAMERA), onResult)
                },
                onUserDecline = { onResult(false) },
            )
        )
    }
}
