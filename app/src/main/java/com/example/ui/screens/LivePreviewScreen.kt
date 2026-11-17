package com.example.ui.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Https
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import red.line.vebino.R
import com.example.core.appString
import com.example.data.local.db.FullProjectBundle
import com.example.domain.model.BackNavigationBehavior
import com.example.domain.model.CacheModeOption
import com.example.domain.model.ExternalLinkMode
import com.example.domain.model.MixedContentOption
import com.example.domain.model.PopupBehaviorMode
import com.example.domain.model.SslPolicy
import com.example.ui.components.GlassCard
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoScreenHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens
import com.example.webviewengine.DownloadItemStatus
import com.example.webviewengine.WebPreviewState
import com.example.webviewengine.WebViewEngineConfig
import com.example.webviewengine.WebViewManager
import kotlinx.coroutines.delay

@Composable
fun LivePreviewScreen(
    bundle: FullProjectBundle?,
    clearCacheOnExit: Boolean = false,
    onBack: () -> Unit,
) {
    if (bundle == null) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tokens = WebinoThemeTokens.colors
    val savedWebBundle = rememberSaveable(bundle.project.id) { Bundle() }

    var previewState by remember { mutableStateOf(WebPreviewState(currentUrl = bundle.project.websiteUrl)) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isLandscapeFrame by remember { mutableStateOf(false) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var pendingPermResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val webCfg = remember(bundle) {
        val w = bundle.webSettings
        val s = bundle.settings
        val startUrl = buildString {
            append(bundle.project.websiteUrl.trimEnd('/'))
            val path = s?.startPagePath.orEmpty().trim()
            if (path.isNotEmpty()) {
                if (!path.startsWith("/")) append("/")
                append(path)
            }
        }
        WebViewEngineConfig(
            initialUrl = startUrl,
            navigationRulesRaw = w?.navigationRulesRaw.orEmpty(),
            javaScriptEnabled = w?.javaScriptEnabled ?: true,
            domStorageEnabled = w?.domStorageEnabled ?: true,
            webSqlEnabled = w?.webSqlEnabled ?: false,
            clipboardAccessEnabled = w?.clipboardAccessEnabled ?: true,
            cookiesEnabled = w?.cookiesEnabled ?: true,
            thirdPartyCookiesEnabled = w?.thirdPartyCookiesEnabled ?: false,
            fileUploadEnabled = w?.fileUploadEnabled ?: true,
            downloadManagerEnabled = w?.downloadManagerEnabled ?: true,
            cameraAllowed = w?.cameraPermission ?: false,
            microphoneAllowed = w?.microphonePermission ?: false,
            geolocationAllowed = w?.geolocationEnabled ?: false,
            webRtcAllowed = w?.webRtcEnabled ?: false,
            fullscreenVideoAllowed = w?.fullscreenVideoEnabled ?: true,
            multipleWindowsAllowed = w?.multipleWindowsEnabled ?: false,
            popupsAllowed = w?.popupsEnabled ?: false,
            popupBehavior = runCatching { PopupBehaviorMode.valueOf(w?.popupBehavior ?: "") }
                .getOrDefault(PopupBehaviorMode.SAME_PAGE),
            backBehavior = runCatching { BackNavigationBehavior.valueOf(s?.backBehavior ?: "") }
                .getOrDefault(BackNavigationBehavior.HISTORY_THEN_EXIT),
            customUserAgent = w?.customUserAgent.orEmpty(),
            cacheMode = runCatching { CacheModeOption.valueOf(w?.cacheMode ?: "") }.getOrDefault(CacheModeOption.DEFAULT),
            zoomEnabled = w?.zoomEnabled ?: false,
            mixedContentMode = runCatching { MixedContentOption.valueOf(w?.mixedContentMode ?: "") }
                .getOrDefault(MixedContentOption.NEVER_ALLOW),
            sslPolicy = runCatching { SslPolicy.valueOf(w?.sslPolicy ?: "") }.getOrDefault(SslPolicy.STRICT_BLOCK),
            httpsOnly = w?.httpsOnly ?: true,
            externalLinkMode = runCatching { ExternalLinkMode.valueOf(s?.externalLinkMode ?: "") }
                .getOrDefault(ExternalLinkMode.ASK_OR_ALLOWLIST),
            enableDeepLinks = s?.enableDeepLinks ?: false,
            deepLinkScheme = s?.deepLinkScheme ?: "webino",
            enableAppLinks = s?.enableAppLinks ?: false,
            appLinkHost = s?.appLinkHost.orEmpty(),
        )
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.any { it }
        pendingPermResult?.invoke(granted)
        pendingPermResult = null
    }

    val wiredController = remember {
        WebViewManager(
            context = context,
            onStateChange = { previewState = it },
            onOsPermissionPrompt = { perms, resultCb ->
                pendingPermResult = resultCb
                permLauncher.launch(perms.toTypedArray())
            },
            onShowCustomVideoView = { v -> customVideoView = v },
            onShowPopupWebView = { p -> popupWebView = p },
        )
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        wiredController.completeUploadResult(res.resultCode, res.data)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        wiredController.completeCameraCapture(ok)
    }

    val activeUploadOptions = previewState.pendingUploadOptions
    val offerCameraChoice = remember(activeUploadOptions, webCfg.cameraAllowed) {
        activeUploadOptions != null && (
            activeUploadOptions.captureEnabled ||
                (webCfg.cameraAllowed && activeUploadOptions.acceptMimeTypes.any {
                    it.startsWith("image", ignoreCase = true) || it == "*/*" || it.equals(".jpg", true) || it.equals(".png", true)
                })
            )
    }

    LaunchedEffect(activeUploadOptions, offerCameraChoice) {
        val opts = activeUploadOptions ?: return@LaunchedEffect
        if (!offerCameraChoice) {
            runCatching { fileLauncher.launch(opts.chooserIntent) }.onFailure {
                wiredController.cancelUpload()
            }
        }
    }

    LaunchedEffect(previewState.activeDownloads) {
        if (previewState.activeDownloads.any { it.status == DownloadItemStatus.RUNNING || it.status == DownloadItemStatus.PENDING }) {
            while (true) {
                delay(1200)
                wiredController.refreshDownloads()
                val stillActive = wiredController.getCurrentState().activeDownloads.any {
                    it.status == DownloadItemStatus.RUNNING || it.status == DownloadItemStatus.PENDING
                }
                if (!stillActive) break
            }
        }
    }

    DisposableEffect(lifecycleOwner, clearCacheOnExit, wiredController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> wiredController.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    wiredController.saveWebViewState(savedWebBundle)
                    wiredController.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            wiredController.saveWebViewState(savedWebBundle)
            if (clearCacheOnExit) wiredController.clearSessionCache()
            wiredController.destroy()
        }
    }

    BackHandler {
        wiredController.handleBackPress(onExitScreen = onBack)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!isFullscreen) {
                WebinoScreenHeader(
                    title = appString(R.string.preview_title),
                    subtitle = bundle.settings?.appName ?: bundle.project.name,
                    iconType = Webino3DIconType.PREVIEW,
                    onBack = onBack,
                    backContentDescription = appString(R.string.back_cd),
                    actions = {
                        if (previewState.activeDownloads.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    wiredController.refreshDownloads()
                                    showDownloadsDialog = true
                                },
                                modifier = Modifier.testTag("preview_downloads_btn"),
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(previewState.activeDownloads.size.toString())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = "لیست دانلودها", tint = SkyBlue)
                                }
                            }
                        }
                        IconButton(onClick = { isLandscapeFrame = !isLandscapeFrame }) {
                            Icon(Icons.Rounded.ScreenRotation, contentDescription = "چرخش")
                        }
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Rounded.Fullscreen, contentDescription = "تمام‌صفحه")
                        }
                    },
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (isFullscreen) 0.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isFullscreen) {
                GlassCard(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        when {
                                            !previewState.isOnline -> ErrorCoral.copy(alpha = 0.14f)
                                            previewState.currentUrl.startsWith("https") -> SuccessEmerald.copy(alpha = 0.14f)
                                            else -> WarningAmber.copy(alpha = 0.14f)
                                        },
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (previewState.isOnline) Icons.Rounded.Https else Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    tint = when {
                                        !previewState.isOnline -> ErrorCoral
                                        previewState.currentUrl.startsWith("https") -> SuccessEmerald
                                        else -> WarningAmber
                                    },
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = previewState.currentUrl,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    textDirection = TextDirection.Ltr,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { wiredController.goBack() },
                                enabled = previewState.canGoBack,
                                modifier = Modifier.testTag("preview_back_btn"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appString(R.string.preview_back_cd))
                            }
                            IconButton(
                                onClick = { wiredController.goForward() },
                                enabled = previewState.canGoForward,
                                modifier = Modifier.testTag("preview_forward_btn"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = appString(R.string.preview_forward_cd))
                            }
                            IconButton(
                                onClick = { wiredController.reload() },
                                modifier = Modifier.testTag("preview_refresh_btn"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = appString(R.string.preview_refresh_cd), tint = ElectricViolet)
                            }
                            IconButton(
                                onClick = { wiredController.openCurrentInExternalBrowser() },
                                modifier = Modifier.testTag("preview_external_btn"),
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = appString(R.string.preview_open_external_cd), tint = SkyBlue)
                            }
                            IconButton(
                                onClick = { wiredController.requestClearSiteDataConfirmation() },
                                modifier = Modifier.testTag("preview_clear_cache_btn"),
                            ) {
                                Icon(Icons.Rounded.CleaningServices, contentDescription = appString(R.string.preview_clear_cache_cd))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            val frameModifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else if (isLandscapeFrame) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 17f)
            }

            val frameShape = RoundedCornerShape(if (isFullscreen) 0.dp else 28.dp)
            Box(
                modifier = frameModifier
                    .shadow(
                        elevation = if (isFullscreen) 0.dp else 14.dp,
                        shape = frameShape,
                        ambientColor = tokens.cardShadowTint,
                        spotColor = tokens.cardShadowTint,
                    )
                    .clip(frameShape)
                    .border(
                        width = if (isFullscreen) 0.dp else 3.dp,
                        color = if (tokens.isDark) Color(0xFF2B3867) else Color(0xFFCBD5E1),
                        shape = frameShape,
                    )
                    .background(if (tokens.isDark) NavyVoid else Color.White),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (previewState.isLoading) {
                        LinearProgressIndicator(
                            progress = { (previewState.progress / 100f).coerceIn(0.05f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = ElectricViolet,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).also { wv ->
                                    wiredController.attach(wv, webCfg, savedWebBundle.takeIf { !it.isEmpty })
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        val errInfo = previewState.errorInfo
                        if (errInfo != null || previewState.errorDescription != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
                                    .padding(20.dp)
                                    .testTag("preview_error_overlay"),
                                contentAlignment = Alignment.Center,
                            ) {
                                GlassCard(borderColor = ErrorCoral) {
                                    Text(
                                        text = errInfo?.titleFa ?: appString(R.string.preview_error_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ErrorCoral,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = errInfo?.messageFa ?: previewState.errorDescription.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (errInfo != null && errInfo.technicalDetail.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = errInfo.technicalDetail,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = WarningAmber,
                                        )
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { wiredController.reload() },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(appString(R.string.preview_retry_btn))
                                        }
                                        if (previewState.canGoBack) {
                                            OutlinedButton(
                                                onClick = { wiredController.goBack() },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("صفحه قبل")
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("بررسی تنظیمات اتصال اینترنت")
                                    }
                                }
                            }
                        }

                        popupWebView?.let { pWeb ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .testTag("preview_internal_popup"),
                            ) {
                                Surface(color = tokens.glassContainerStrong, border = BorderStroke(1.dp, tokens.navPillBorder)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("پنجره داخلی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { wiredController.closeInternalPopup() }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "بستن پنجره")
                                        }
                                    }
                                }
                                AndroidView(
                                    factory = { pWeb },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                )
                            }
                        }

                        previewState.blockedNotice?.let { note ->
                            Surface(
                                color = WarningAmber.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = note,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }

                        customVideoView?.let { vid ->
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        addView(vid)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                    ) {
                        Icon(Icons.Rounded.FullscreenExit, contentDescription = "خروج", tint = Color.White)
                    }
                }
            }
        }
    }

    previewState.sslWarningUrl?.let { badUrl ->
        AlertDialog(
            onDismissRequest = { wiredController.resolveSslDecision(false) },
            title = { Text(appString(R.string.preview_ssl_title)) },
            text = { Text("گواهی امنیتی آدرس زیر معتبر نیست:\n$badUrl\nبرای حفظ امنیت، اتصال مسدود شود؟") },
            confirmButton = {
                TextButton(onClick = { wiredController.resolveSslDecision(false) }) {
                    Text("مسدود کن (امن)", color = SuccessEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.resolveSslDecision(true) }) {
                    Text("عبور موقت", color = ErrorCoral)
                }
            },
        )
    }

    previewState.pendingRationale?.let { rationale ->
        AlertDialog(
            onDismissRequest = { wiredController.resolveRationale(false) },
            title = { Text(rationale.titleFa) },
            text = { Text(rationale.messageFa) },
            confirmButton = {
                TextButton(onClick = { wiredController.resolveRationale(true) }) {
                    Text("موافقم و ادامه")
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.resolveRationale(false) }) {
                    Text("انصراف", color = ErrorCoral)
                }
            },
        )
    }

    previewState.pendingExternalConfirm?.let { confirmExt ->
        AlertDialog(
            onDismissRequest = { wiredController.resolveExternalNavigation(false) },
            title = { Text("باز کردن لینک خارجی") },
            text = { Text(confirmExt.reasonFa) },
            confirmButton = {
                TextButton(onClick = { wiredController.resolveExternalNavigation(true) }) {
                    Text("باز کردن با برنامه دیگر")
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.resolveExternalNavigation(false) }) {
                    Text("انصراف")
                }
            },
        )
    }

    if (offerCameraChoice && activeUploadOptions != null) {
        AlertDialog(
            onDismissRequest = { wiredController.cancelUpload() },
            title = { Text("انتخاب منبع بارگذاری فایل") },
            text = { Text("فایل یا تصویر را از گالری انتخاب کنید یا عکس جدید بگیرید.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching { fileLauncher.launch(activeUploadOptions.chooserIntent) }
                            .onFailure { wiredController.cancelUpload() }
                    }
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("انتخاب فایل")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            wiredController.requestCameraForUpload { granted ->
                                if (granted) {
                                    val pair = wiredController.prepareCameraCapture()
                                    if (pair != null) {
                                        runCatching { cameraLauncher.launch(pair.second) }
                                            .onFailure { wiredController.completeCameraCapture(false) }
                                    } else {
                                        wiredController.completeCameraCapture(false)
                                    }
                                } else {
                                    wiredController.cancelUpload()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("دوربین")
                    }
                    TextButton(onClick = { wiredController.cancelUpload() }) {
                        Text("لغو", color = ErrorCoral)
                    }
                }
            },
        )
    }

    if (previewState.confirmClearSiteData) {
        AlertDialog(
            onDismissRequest = { wiredController.confirmClearSiteData(false) },
            title = { Text("پاکسازی کوکی و حافظه سایت") },
            text = { Text("تمام کوکی‌های ورود، حافظه LocalStorage و کش موقت این سایت پاک می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = { wiredController.confirmClearSiteData(true) }) {
                    Text("پاکسازی کن", color = ErrorCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.confirmClearSiteData(false) }) {
                    Text("انصراف")
                }
            },
        )
    }

    if (previewState.confirmExitPrompt) {
        AlertDialog(
            onDismissRequest = { wiredController.dismissExitConfirm(false, onBack) },
            title = { Text("خروج از پیش‌نمایش") },
            text = { Text("آیا مطمئن هستید که می‌خواهید پیش‌نمایش وب‌سایت را ببندید؟") },
            confirmButton = {
                TextButton(onClick = { wiredController.dismissExitConfirm(true, onBack) }) {
                    Text("بله، خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.dismissExitConfirm(false, onBack) }) {
                    Text("ادامه مشاهده")
                }
            },
        )
    }

    previewState.pendingJsDialog?.let { jsDlg ->
        AlertDialog(
            onDismissRequest = { wiredController.resolveJsDialog(false) },
            title = { Text(jsDlg.titleFa) },
            text = { Text(jsDlg.message) },
            confirmButton = {
                TextButton(onClick = { wiredController.resolveJsDialog(true) }) {
                    Text("تأیید")
                }
            },
            dismissButton = if (jsDlg.isConfirm) {
                {
                    TextButton(onClick = { wiredController.resolveJsDialog(false) }) {
                        Text("لغو", color = ErrorCoral)
                    }
                }
            } else {
                null
            },
        )
    }

    if (showDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            title = { Text("مدیریت دانلودها") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                ) {
                    items(previewState.activeDownloads) { dl ->
                        GlassCard {
                            Text(dl.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${dl.status.titleFa} • ${dl.progressPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (dl.status == DownloadItemStatus.RUNNING || dl.status == DownloadItemStatus.PENDING) {
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (dl.progressPercent / 100f).coerceIn(0.05f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dl.status == DownloadItemStatus.SUCCESSFUL) {
                                    TextButton(onClick = { wiredController.openDownload(dl) }) {
                                        Text("باز کردن فایل")
                                    }
                                }
                                if (dl.status == DownloadItemStatus.RUNNING || dl.status == DownloadItemStatus.PENDING || dl.status == DownloadItemStatus.PAUSED) {
                                    TextButton(onClick = { wiredController.cancelDownload(dl.downloadId) }) {
                                        Text("لغو", color = ErrorCoral)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadsDialog = false }) {
                    Text("بستن")
                }
            },
            dismissButton = {
                TextButton(onClick = { wiredController.refreshDownloads() }) {
                    Text("بروزرسانی")
                }
            },
        )
    }
}
