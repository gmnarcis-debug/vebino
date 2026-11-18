package com.example.buildengine

import com.example.domain.model.CacheModeOption
import com.example.domain.model.MixedContentOption
import com.example.domain.model.SslPolicy
import com.example.domain.model.ToolbarStyle
import java.io.File

/**
 * Generates clean, self-contained Java 8 source code for the target standalone
 * Android WebView application tailored to the user's BuildSpec with full Prompt 04 parity.
 */
object ProjectGenerator {

    fun generateSources(spec: BuildSpec, srcRootDir: File): List<File> {
        val pkgDir = File(srcRootDir, spec.packageName.replace('.', '/'))
        pkgDir.mkdirs()

        val mainActivityFile = File(pkgDir, "MainActivity.java")
        mainActivityFile.writeText(buildMainActivitySource(spec), Charsets.UTF_8)
        return listOf(mainActivityFile)
    }

    private fun escapeJava(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")

    private fun buildMainActivitySource(spec: BuildSpec): String {
        val initialUrl = buildString {
            append(spec.websiteUrl.trimEnd('/'))
            val path = spec.startPagePath.trim()
            if (path.isNotEmpty()) {
                if (!path.startsWith("/")) append("/")
                append(path)
            }
        }
        val primaryColor = spec.primaryColorHex.ifBlank { "#7C3AED" }
        val bgColor = spec.backgroundColorHex.ifBlank { "#0B1026" }
        val cacheConst = when (spec.cacheMode) {
            CacheModeOption.DEFAULT -> "WebSettings.LOAD_DEFAULT"
            CacheModeOption.NO_CACHE -> "WebSettings.LOAD_NO_CACHE"
            CacheModeOption.CACHE_ELSE_NETWORK -> "WebSettings.LOAD_CACHE_ELSE_NETWORK"
        }
        val mixedConst = when (spec.mixedContentMode) {
            MixedContentOption.NEVER_ALLOW -> "WebSettings.MIXED_CONTENT_NEVER_ALLOW"
            MixedContentOption.COMPATIBILITY -> "WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE"
        }
        val showTopBar = spec.toolbarStyle == ToolbarStyle.TOP_BAR
        val showBottomBar = spec.toolbarStyle == ToolbarStyle.BOTTOM_BAR || spec.toolbarStyle == ToolbarStyle.FLOATING_PILL
        val isDark = spec.themePreset != com.example.domain.model.ThemePreset.LIGHT
        val textOnBg = if (isDark) "#F8FAFC" else "#0F172A"
        val errorCardBg = if (isDark) "#161E3D" else "#FFFFFF"

        return """
package ${spec.packageName};

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String APP_NAME = "${escapeJava(spec.appName)}";
    private static final String INITIAL_URL = "${escapeJava(initialUrl)}";
    private static final String PRIMARY_COLOR = "${escapeJava(primaryColor)}";
    private static final String BG_COLOR = "${escapeJava(bgColor)}";
    private static final String TEXT_COLOR = "${escapeJava(textOnBg)}";
    private static final String CARD_COLOR = "${escapeJava(errorCardBg)}";
    private static final String CUSTOM_UA = "${escapeJava(spec.customUserAgent)}";
    private static final boolean SUPPORT_RTL = ${spec.supportRtl};
    private static final boolean FULLSCREEN = ${spec.fullscreenMode};
    private static final boolean SHOW_STATUS_BAR = ${spec.showStatusBar};
    private static final boolean SHOW_NAV_BAR = ${spec.showNavigationBar};
    private static final int BACK_BEHAVIOR = ${spec.backBehavior.ordinal};
    private static final boolean SPLASH_ENABLED = ${spec.splashEnabled};
    private static final int SPLASH_DURATION_MS = ${spec.splashDurationMs};
    private static final int SPLASH_LOGO_DP = ${if (spec.splashPngBytes != null) 200 else 108};
    private static final boolean SHOW_TOP_BAR = $showTopBar;
    private static final boolean SHOW_BOTTOM_BAR = $showBottomBar;

    private static final boolean JS_ENABLED = ${spec.javaScriptEnabled};
    private static final boolean DOM_STORAGE = ${spec.domStorageEnabled};
    private static final boolean WEB_SQL_ENABLED = ${spec.webSqlEnabled};
    private static final boolean CLIPBOARD_ACCESS = ${spec.clipboardAccessEnabled};
    private static final boolean COOKIES_ENABLED = ${spec.cookiesEnabled};
    private static final boolean THIRD_PARTY_COOKIES = ${spec.thirdPartyCookiesEnabled};
    private static final boolean FILE_UPLOAD = ${spec.fileUploadEnabled};
    private static final boolean DOWNLOAD_ENABLED = ${spec.downloadManagerEnabled};
    private static final boolean CAMERA_ALLOWED = ${spec.cameraPermission};
    private static final boolean MIC_ALLOWED = ${spec.microphonePermission};
    private static final boolean GEO_ALLOWED = ${spec.geolocationEnabled};
    private static final boolean WEBRTC_ALLOWED = ${spec.webRtcEnabled};
    private static final boolean FULLSCREEN_VIDEO = ${spec.fullscreenVideoEnabled};
    private static final boolean MULTI_WINDOW = ${spec.multipleWindowsEnabled};
    private static final boolean POPUPS_ALLOWED = ${spec.popupsEnabled};
    private static final int POPUP_BEHAVIOR = ${spec.popupBehavior.ordinal};
    private static final boolean ZOOM_ENABLED = ${spec.zoomEnabled};
    private static final boolean HTTPS_ONLY = ${spec.httpsOnly};
    private static final boolean STRICT_SSL = ${spec.sslPolicy == SslPolicy.STRICT_BLOCK};
    private static final boolean FILE_SHARING = ${spec.enableFileSharing};
    private static final boolean PHONE_DIALER = ${spec.enablePhoneDialer};
    private static final boolean EMAIL_INTENT = ${spec.enableEmailIntent};
    private static final boolean MAPS_INTENT = ${spec.enableMapsIntent};
    private static final boolean PAYMENT_BRIDGE = ${spec.enablePaymentGatewayBridge};
    private static final int EXTERNAL_MODE = ${spec.externalLinkMode.ordinal};
    private static final String NAVIGATION_RULES = "${escapeJava(spec.navigationRulesRaw)}";

    private static final int REQ_FILE_CHOOSER = 1001;
    private static final int REQ_PERMISSIONS = 1002;
    private static final int REQ_GEO_PERMISSION = 1003;

    private WebView webView;
    private WebView popupWebView;
    private AlertDialog popupDialog;
    private FrameLayout rootContainer;
    private FrameLayout customVideoContainer;
    private LinearLayout splashOverlay;
    private LinearLayout errorOverlay;
    private TextView errorTitleView;
    private TextView errorMsgView;
    private ProgressBar progressBar;
    private TextView toolbarTitle;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingWebPermRequest;
    private String[] pendingWebPermResources;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customVideoView;
    private ConnectivityManager.NetworkCallback reconnectCallback;
    private String allowedOriginHost = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        configureSystemBars();

        Uri initialUri = Uri.parse(INITIAL_URL);
        if (initialUri != null && initialUri.getHost() != null) {
            allowedOriginHost = initialUri.getHost().toLowerCase(Locale.US);
        }

        buildUserInterface();
        configureWebView();

        String launchUrl = resolveLaunchUrl(getIntent());
        if (SPLASH_ENABLED) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (splashOverlay != null) {
                        splashOverlay.setVisibility(View.GONE);
                    }
                }
            }, SPLASH_DURATION_MS);
        }
        if (savedInstanceState != null && webView != null && webView.restoreState(savedInstanceState) != null) {
            return;
        }
        loadTargetUrl(launchUrl);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null && outState != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (webView != null && savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
    }

    private void configureSystemBars() {
        Window w = getWindow();
        if (w == null) return;
        int uiFlags = 0;
        if (FULLSCREEN || !SHOW_STATUS_BAR) {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (FULLSCREEN || !SHOW_NAV_BAR) {
            uiFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        if (uiFlags != 0) {
            View decor = w.getDecorView();
            if (decor != null) {
                decor.setSystemUiVisibility(uiFlags);
            }
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(safeColor(PRIMARY_COLOR, 0xFF7C3AED));
            w.setNavigationBarColor(safeColor(BG_COLOR, 0xFF0B1026));
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private int safeColor(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void buildUserInterface() {
        int bgInt = safeColor(BG_COLOR, 0xFF0B1026);
        int primaryInt = safeColor(PRIMARY_COLOR, 0xFF7C3AED);
        int textInt = safeColor(TEXT_COLOR, 0xFFF8FAFC);

        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(bgInt);
        if (SUPPORT_RTL) {
            rootContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        LinearLayout mainColumn = new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);

        if (SHOW_TOP_BAR) {
            LinearLayout topBar = new LinearLayout(this);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(Gravity.CENTER_VERTICAL);
            topBar.setBackgroundColor(primaryInt);
            topBar.setPadding(dp(12), dp(10), dp(12), dp(10));

            toolbarTitle = new TextView(this);
            toolbarTitle.setText(APP_NAME);
            toolbarTitle.setTextColor(Color.WHITE);
            toolbarTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            toolbarTitle.setTypeface(Typeface.DEFAULT_BOLD);
            toolbarTitle.setSingleLine(true);

            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            );
            topBar.addView(toolbarTitle, titleLp);

            Button reloadBtn = createBarButton("بارگذاری مجدد", 0x33FFFFFF);
            reloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (webView != null) webView.reload();
                }
            });
            topBar.addView(reloadBtn);

            if (FILE_SHARING) {
                Button shareBtn = createBarButton("اشتراک", 0x33FFFFFF);
                shareBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shareCurrentPage();
                    }
                });
                topBar.addView(shareBtn);
            }

            mainColumn.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(4)
        );
        mainColumn.addView(progressBar, pbLp);

        FrameLayout webHolder = new FrameLayout(this);
        webView = new WebView(this);
        webHolder.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        errorOverlay = buildErrorOverlay(primaryInt, textInt);
        errorOverlay.setVisibility(View.GONE);
        webHolder.addView(errorOverlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mainColumn.addView(webHolder, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        if (SHOW_BOTTOM_BAR) {
            LinearLayout bottomBar = new LinearLayout(this);
            bottomBar.setOrientation(LinearLayout.HORIZONTAL);
            bottomBar.setGravity(Gravity.CENTER);
            bottomBar.setBackgroundColor(safeColor(CARD_COLOR, 0xFF161E3D));
            bottomBar.setPadding(dp(8), dp(8), dp(8), dp(8));

            Button backBtn = createBarButton("بازگشت", primaryInt);
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (webView != null && webView.canGoBack()) webView.goBack();
                }
            });
            Button fwdBtn = createBarButton("بعدی", primaryInt);
            fwdBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (webView != null && webView.canGoForward()) webView.goForward();
                }
            });
            Button homeBtn = createBarButton("خانه", primaryInt);
            homeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadTargetUrl(INITIAL_URL);
                }
            });
            Button reloadBtn = createBarButton("بروزرسانی", primaryInt);
            reloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (webView != null) webView.reload();
                }
            });
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            );
            btnLp.setMargins(dp(4), 0, dp(4), 0);
            bottomBar.addView(backBtn, btnLp);
            bottomBar.addView(fwdBtn, btnLp);
            bottomBar.addView(homeBtn, btnLp);
            bottomBar.addView(reloadBtn, btnLp);

            mainColumn.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        rootContainer.addView(mainColumn, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        customVideoContainer = new FrameLayout(this);
        customVideoContainer.setBackgroundColor(Color.BLACK);
        customVideoContainer.setVisibility(View.GONE);
        rootContainer.addView(customVideoContainer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (SPLASH_ENABLED) {
            splashOverlay = buildSplashOverlay(bgInt, primaryInt, textInt);
            rootContainer.addView(splashOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        setContentView(rootContainer);
    }

    private Button createBarButton(String label, int bgColor) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(10));
        btn.setBackground(gd);
        btn.setPadding(dp(10), dp(6), dp(10), dp(6));
        return btn;
    }

    private LinearLayout buildSplashOverlay(int bgInt, int primaryInt, int textInt) {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        GradientDrawable grad = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] { bgInt, primaryInt }
        );
        splash.setBackground(grad);
        splash.setPadding(dp(32), dp(32), dp(32), dp(32));

        ImageView logo = new ImageView(this);
        try {
            InputStream is = getAssets().open("splash.png");
            Bitmap bm = BitmapFactory.decodeStream(is);
            is.close();
            if (bm != null) {
                logo.setImageBitmap(bm);
            }
        } catch (Exception ignored) {}
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(SPLASH_LOGO_DP), dp(SPLASH_LOGO_DP));
        logoLp.setMargins(0, 0, 0, dp(20));
        splash.addView(logo, logoLp);

        TextView title = new TextView(this);
        title.setText(APP_NAME);
        title.setTextColor(textInt);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        splash.addView(title);

        ProgressBar spinner = new ProgressBar(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        spLp.setMargins(0, dp(28), 0, 0);
        splash.addView(spinner, spLp);
        return splash;
    }

    private LinearLayout buildErrorOverlay(int primaryInt, int textInt) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(safeColor(BG_COLOR, 0xFF0B1026));
        box.setPadding(dp(28), dp(28), dp(28), dp(28));

        errorTitleView = new TextView(this);
        errorTitleView.setText("خطا در اتصال به وبسایت");
        errorTitleView.setTextColor(textInt);
        errorTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        errorTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        errorTitleView.setGravity(Gravity.CENTER);
        box.addView(errorTitleView);

        errorMsgView = new TextView(this);
        errorMsgView.setText("لطفاً اتصال اینترنت خود را بررسی کنید و دوباره تلاش نمایید.");
        errorMsgView.setTextColor(textInt);
        errorMsgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorMsgView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        msgLp.setMargins(0, dp(12), 0, dp(20));
        box.addView(errorMsgView, msgLp);

        Button retryBtn = createBarButton("تلاش مجدد", primaryInt);
        retryBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        retryBtn.setPadding(dp(28), dp(12), dp(28), dp(12));
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorOverlay.setVisibility(View.GONE);
                if (webView != null) webView.reload();
            }
        });
        box.addView(retryBtn);

        Button netBtn = createBarButton("تنظیمات اینترنت", 0xFF334155);
        LinearLayout.LayoutParams netLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        netLp.setMargins(0, dp(10), 0, 0);
        netBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent("android.settings.WIRELESS_SETTINGS"));
                } catch (Exception ignored) {}
            }
        });
        box.addView(netBtn, netLp);
        return box;
    }

    private void showCategorizedError(String title, String message) {
        if (errorTitleView != null) errorTitleView.setText(title);
        if (errorMsgView != null) errorMsgView.setText(message);
        if (errorOverlay != null) errorOverlay.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    private void startReconnectMonitor() {
        stopReconnectMonitor();
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            reconnectCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (errorOverlay != null) errorOverlay.setVisibility(View.GONE);
                            if (webView != null) webView.reload();
                        }
                    });
                }
            };
            cm.registerDefaultNetworkCallback(reconnectCallback);
        } catch (Exception ignored) {}
    }

    private void stopReconnectMonitor() {
        try {
            if (reconnectCallback != null) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) cm.unregisterNetworkCallback(reconnectCallback);
            }
        } catch (Exception ignored) {}
        reconnectCallback = null;
    }

    @SuppressWarnings("deprecation")
    private void applyHardenedWebSettings(WebView target) {
        WebSettings ws = target.getSettings();
        ws.setJavaScriptEnabled(JS_ENABLED);
        ws.setDomStorageEnabled(DOM_STORAGE);
        ws.setDatabaseEnabled(WEB_SQL_ENABLED || DOM_STORAGE);
        ws.setAllowFileAccess(false);
        ws.setAllowFileAccessFromFileURLs(false);
        ws.setAllowUniversalAccessFromFileURLs(false);
        ws.setAllowContentAccess(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(ZOOM_ENABLED);
        ws.setBuiltInZoomControls(ZOOM_ENABLED);
        ws.setDisplayZoomControls(false);
        ws.setGeolocationEnabled(GEO_ALLOWED);
        ws.setJavaScriptCanOpenWindowsAutomatically(POPUPS_ALLOWED);
        ws.setSupportMultipleWindows(MULTI_WINDOW);
        ws.setMediaPlaybackRequiresUserGesture(!WEBRTC_ALLOWED);
        ws.setCacheMode($cacheConst);
        ws.setMixedContentMode($mixedConst);
        if (!CUSTOM_UA.isEmpty()) {
            ws.setUserAgentString(CUSTOM_UA);
        }

        CookieManager cm = CookieManager.getInstance();
        if (cm != null) {
            cm.setAcceptCookie(COOKIES_ENABLED);
            cm.setAcceptThirdPartyCookies(target, COOKIES_ENABLED && THIRD_PARTY_COOKIES);
        }
    }

    private void configureWebView() {
        applyHardenedWebSettings(webView);
        if (JS_ENABLED) {
            webView.addJavascriptInterface(new WebinoBridge(), "WebinoBridge");
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                stopReconnectMonitor();
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (errorOverlay != null) errorOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                CookieManager c = CookieManager.getInstance();
                if (c != null) c.flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    if (!isNetworkAvailable()) {
                        showCategorizedError("عدم اتصال به اینترنت", "اینترنت گوشی متصل نیست. پس از برقراری اتصال، صفحه به‌صورت خودکار بارگذاری می‌شود.");
                    } else {
                        String desc = error != null && error.getDescription() != null ? error.getDescription().toString() : "";
                        if (desc.contains("NAME_NOT_RESOLVED")) {
                            showCategorizedError("خطا در شناسایی دامنه (DNS)", "آدرس سرور وب‌سایت پیدا نشد. نشانی یا شبکه خود را بررسی کنید.");
                        } else if (desc.contains("TIMED_OUT")) {
                            showCategorizedError("اتمام زمان پاسخگویی سرور", "پاسخگویی سرور طول کشید. لطفاً دوباره تلاش کنید.");
                        } else {
                            showCategorizedError("خطا در بارگذاری صفحه", "امکان نمایش این صفحه وجود ندارد. لطفاً دوباره تلاش کنید.");
                        }
                    }
                    startReconnectMonitor();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request != null && request.isForMainFrame() && errorResponse != null) {
                    int code = errorResponse.getStatusCode();
                    if (code == 404) {
                        showCategorizedError("صفحه یافت نشد (۴۰۴)", "صفحه درخواستی روی سرور وب‌سایت موجود نیست.");
                    } else if (code >= 500) {
                        showCategorizedError("خطای سرور وب‌سایت (" + code + ")", "سرور وب‌سایت موقتاً با مشکل فنی مواجه است.");
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                if (STRICT_SSL) {
                    handler.cancel();
                    showCategorizedError("خطای امنیتی گواهی SSL", "گواهی امنیتی این صفحه معتبر نیست و اتصال مسدود شد.");
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("هشدار امنیتی SSL")
                        .setMessage("گواهی امنیتی این صفحه معتبر نیست. ادامه دادن توصیه نمیشود.")
                        .setCancelable(false)
                        .setNegativeButton("مسدود کن", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        })
                        .setPositiveButton("تایید با مسئولیت کاربر", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.proceed();
                            }
                        })
                        .show();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                return handleUriNavigation(request.getUrl());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (toolbarTitle != null && title != null && !title.isEmpty()) {
                    toolbarTitle.setText(title);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (!FULLSCREEN_VIDEO) {
                    if (callback != null) callback.onCustomViewHidden();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;
                customVideoContainer.removeAllViews();
                customVideoContainer.addView(view);
                customVideoContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onHideCustomView() {
                if (customVideoView != null) {
                    customVideoContainer.removeAllViews();
                    customVideoContainer.setVisibility(View.GONE);
                    customVideoView = null;
                }
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }

            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (!FILE_UPLOAD || cb == null || params == null) {
                    if (cb != null) cb.onReceiveValue(null);
                    return false;
                }
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
                    picker.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] accepts = params.getAcceptTypes();
                    String mime = "*/*";
                    if (accepts != null && accepts.length == 1 && accepts[0] != null && !accepts[0].trim().isEmpty()) {
                        String raw = accepts[0].trim();
                        mime = raw.contains("/") ? raw : (".".equals(raw.substring(0, 1)) && ".pdf".equalsIgnoreCase(raw) ? "application/pdf" : "*/*");
                    }
                    picker.setType(mime);
                    if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    startActivityForResult(Intent.createChooser(picker, "انتخاب فایل برای ارسال"), REQ_FILE_CHOOSER);
                    return true;
                } catch (Exception e) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                    return false;
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
                if (!GEO_ALLOWED || callback == null) {
                    if (callback != null) callback.invoke(origin, false, false);
                    return;
                }
                if (!isTrustedUrlString(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (!isLocationServiceEnabled()) {
                    callback.invoke(origin, false, false);
                    Toast.makeText(MainActivity.this, "لطفاً سرویس موقعیت مکانی (GPS) گوشی را روشن کنید.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                    return;
                }
                pendingGeoOrigin = origin;
                pendingGeoCallback = callback;
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("دسترسی به موقعیت مکانی")
                    .setMessage("وب‌سایت برای ارائه خدمات مکانی درخواست دسترسی به GPS دستگاه دارد. موافقید؟")
                    .setPositiveButton("موافقم", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, REQ_GEO_PERMISSION);
                        }
                    })
                    .setNegativeButton("انصراف", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (pendingGeoCallback != null) pendingGeoCallback.invoke(pendingGeoOrigin, false, false);
                            pendingGeoCallback = null;
                        }
                    })
                    .show();
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (request == null) return;
                String origin = request.getOrigin() != null ? request.getOrigin().toString() : "";
                if (!isTrustedUrlString(origin)) {
                    request.deny();
                    return;
                }
                final List<String> grantedResources = new ArrayList<String>();
                final List<String> needOsPerms = new ArrayList<String>();
                String[] reqRes = request.getResources();
                if (reqRes != null) {
                    for (String res : reqRes) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && CAMERA_ALLOWED) {
                            grantedResources.add(res);
                            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                needOsPerms.add(Manifest.permission.CAMERA);
                            }
                        } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && (MIC_ALLOWED || WEBRTC_ALLOWED)) {
                            grantedResources.add(res);
                            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                needOsPerms.add(Manifest.permission.RECORD_AUDIO);
                            }
                        }
                    }
                }
                if (grantedResources.isEmpty()) {
                    request.deny();
                    return;
                }
                final String[] resArr = new String[grantedResources.size()];
                for (int i = 0; i < grantedResources.size(); i++) resArr[i] = grantedResources.get(i);
                if (needOsPerms.isEmpty()) {
                    request.grant(resArr);
                } else {
                    pendingWebPermRequest = request;
                    pendingWebPermResources = resArr;
                    final String[] osArr = new String[needOsPerms.size()];
                    for (int i = 0; i < needOsPerms.size(); i++) osArr[i] = needOsPerms.get(i);
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("درخواست مجوز سخت‌افزاری")
                        .setMessage("این وب‌سایت برای دوربین یا میکروفون نیاز به مجوز موقت دارد.")
                        .setPositiveButton("ادامه", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(osArr, REQ_PERMISSIONS);
                            }
                        })
                        .setNegativeButton("انصراف", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (pendingWebPermRequest != null) pendingWebPermRequest.deny();
                                pendingWebPermRequest = null;
                                pendingWebPermResources = null;
                            }
                        })
                        .show();
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (request == pendingWebPermRequest) {
                    pendingWebPermRequest = null;
                    pendingWebPermResources = null;
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (!MULTI_WINDOW || resultMsg == null) return false;
                if (!isUserGesture && !POPUPS_ALLOWED) return false;
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                if (transport == null) return false;
                if (POPUP_BEHAVIOR == 2) {
                    WebView bridge = new WebView(MainActivity.this);
                    bridge.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                            if (req != null && req.getUrl() != null) {
                                openExternalUri(req.getUrl());
                            }
                            v.destroy();
                            return true;
                        }
                    });
                    transport.setWebView(bridge);
                    resultMsg.sendToTarget();
                    return true;
                } else if (POPUP_BEHAVIOR == 1) {
                    closePopupDialog();
                    popupWebView = new WebView(MainActivity.this);
                    applyHardenedWebSettings(popupWebView);
                    popupWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                            if (req == null || req.getUrl() == null) return false;
                            String sch = req.getUrl().getScheme() == null ? "" : req.getUrl().getScheme().toLowerCase(Locale.US);
                            if (HTTPS_ONLY && "http".equals(sch)) return true;
                            return false;
                        }
                    });
                    popupWebView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onCloseWindow(WebView window) {
                            closePopupDialog();
                        }
                    });
                    popupDialog = new AlertDialog.Builder(MainActivity.this)
                        .setView(popupWebView)
                        .setNegativeButton("بستن", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                closePopupDialog();
                            }
                        })
                        .show();
                    transport.setWebView(popupWebView);
                    resultMsg.sendToTarget();
                    return true;
                } else {
                    transport.setWebView(webView);
                    resultMsg.sendToTarget();
                    return true;
                }
            }

            @Override
            public void onCloseWindow(WebView window) {
                if (window == popupWebView) {
                    closePopupDialog();
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (!DOWNLOAD_ENABLED) {
                    Toast.makeText(MainActivity.this, "دانلود فایل غیرفعال است.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                String safeName = sanitizeFilename(fileName);
                String lower = safeName.toLowerCase(Locale.US);
                if (lower.endsWith(".apk") || lower.endsWith(".dex") || lower.endsWith(".jar") || lower.endsWith(".exe") || lower.endsWith(".sh")) {
                    Toast.makeText(MainActivity.this, "دانلود این نوع فایل به دلایل امنیتی مسدود شد.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm == null) return;
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setMimeType(mimetype);
                    req.setTitle(safeName);
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName);
                    dm.enqueue(req);
                    Toast.makeText(MainActivity.this, "دانلود فایل آغاز شد: " + safeName, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    openExternalUri(Uri.parse(url));
                }
            }
        });
    }

    private String sanitizeFilename(String raw) {
        if (raw == null || raw.isEmpty()) return "download.bin";
        String noDots = raw.replace("..", "_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < noDots.length(); i++) {
            char c = noDots.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String res = sb.toString();
        return res.isEmpty() ? "download.bin" : res;
    }

    private void closePopupDialog() {
        try {
            if (popupDialog != null) popupDialog.dismiss();
            if (popupWebView != null) popupWebView.destroy();
        } catch (Exception ignored) {}
        popupDialog = null;
        popupWebView = null;
    }

    private boolean isLocationServiceEnabled() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return false;
            if (Build.VERSION.SDK_INT >= 28) {
                return lm.isLocationEnabled();
            }
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean handleUriNavigation(final Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);

        if ("javascript".equals(scheme) || "file".equals(scheme) || "data".equals(scheme) || "vbscript".equals(scheme) || "blob".equals(scheme)) {
            Toast.makeText(this, "لینک غیرمجاز مسدود شد.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if ("tel".equals(scheme) && PHONE_DIALER) {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, uri));
            } catch (Exception ignored) {}
            return true;
        }
        if (("mailto".equals(scheme) || "sms".equals(scheme) || "smsto".equals(scheme)) && EMAIL_INTENT) {
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, uri));
            } catch (Exception ignored) {}
            return true;
        }
        if (("geo".equals(scheme) || host.contains("maps.google") || host.contains("neshan.org") || host.contains("balad.ir")) && MAPS_INTENT) {
            openExternalUri(uri);
            return true;
        }
        if ("whatsapp".equals(scheme) || "tg".equals(scheme) || host.contains("wa.me") || host.contains("t.me") || host.contains("telegram.me")) {
            openExternalUri(uri);
            return true;
        }
        if ("intent".equals(scheme)) {
            try {
                Intent parsed = Intent.parseUri(uri.toString(), 1);
                Intent cleanIntent = new Intent(parsed.getAction(), parsed.getData());
                cleanIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                cleanIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(cleanIntent);
            } catch (Exception e) {
                try {
                    Intent parsed2 = Intent.parseUri(uri.toString(), 1);
                    String fb = parsed2.getStringExtra("browser_fallback_url");
                    if (fb != null && (fb.startsWith("https://") || fb.startsWith("http://"))) {
                        openExternalUri(Uri.parse(fb));
                    }
                } catch (Exception ignored) {}
            }
            return true;
        }
        if (HTTPS_ONLY && "http".equals(scheme)) {
            Toast.makeText(this, "اتصال ناامن HTTP مسدود شد.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            askExternalConfirm(uri, "باز کردن برنامه خارجی با طرح " + scheme + "؟");
            return true;
        }
        if (isHostBlockedByRules(host)) {
            Toast.makeText(this, "دسترسی به این دامنه طبق قوانین پروژه مجاز نیست.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (PAYMENT_BRIDGE && isPaymentGatewayHost(host)) {
            return false;
        }
        boolean sameHost = !allowedOriginHost.isEmpty() && (host.equals(allowedOriginHost) || host.endsWith("." + allowedOriginHost));
        boolean allowlisted = isHostAllowedByRules(host);
        if (!sameHost && !allowlisted) {
            if (EXTERNAL_MODE == 0) {
                openExternalUri(uri);
                return true;
            } else if (EXTERNAL_MODE == 2) {
                askExternalConfirm(uri, "لینک به دامنه خارجی (" + host + ") اشاره دارد. در مرورگر سیستم باز شود؟");
                return true;
            }
        }
        return false;
    }

    private void askExternalConfirm(final Uri uri, String message) {
        new AlertDialog.Builder(this)
            .setTitle("باز کردن لینک خارجی")
            .setMessage(message)
            .setPositiveButton("باز کردن", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    openExternalUri(uri);
                }
            })
            .setNegativeButton("انصراف", null)
            .show();
    }

    private boolean isHostBlockedByRules(String host) {
        if (NAVIGATION_RULES.isEmpty() || host.isEmpty()) return false;
        String[] lines = NAVIGATION_RULES.split("[,\\n;]+");
        for (int i = 0; i < lines.length; i++) {
            String rule = lines[i].trim().toLowerCase(Locale.US);
            if (rule.startsWith("!")) {
                String domain = rule.substring(1).trim();
                if (!domain.isEmpty() && (host.equals(domain) || host.endsWith("." + domain))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHostAllowedByRules(String host) {
        if (NAVIGATION_RULES.isEmpty() || host.isEmpty()) return false;
        String[] lines = NAVIGATION_RULES.split("[,\\n;]+");
        for (int i = 0; i < lines.length; i++) {
            String rule = lines[i].trim().toLowerCase(Locale.US);
            if (!rule.isEmpty() && !rule.startsWith("!")) {
                if (host.equals(rule) || host.endsWith("." + rule)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPaymentGatewayHost(String host) {
        return host.contains("shaparak.ir")
            || host.contains("zarinpal.com")
            || host.contains("idpay.ir")
            || host.contains("behpardakht.com")
            || host.contains("sadadpsp.ir")
            || host.contains("pep.co.ir");
    }

    private void openExternalUri(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void shareCurrentPage() {
        String current = webView != null && webView.getUrl() != null ? webView.getUrl() : INITIAL_URL;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, APP_NAME);
        share.putExtra(Intent.EXTRA_TEXT, current);
        startActivity(Intent.createChooser(share, APP_NAME));
    }

    private String resolveLaunchUrl(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getDataString() != null) {
            String data = intent.getDataString();
            if (data.startsWith("https://") || (!HTTPS_ONLY && data.startsWith("http://"))) {
                return data;
            }
        }
        return INITIAL_URL;
    }

    private void loadTargetUrl(String url) {
        if (HTTPS_ONLY && url.startsWith("http://")) {
            Toast.makeText(this, "فقط آدرسهای امن HTTPS مجاز هستند.", Toast.LENGTH_LONG).show();
            return;
        }
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String next = resolveLaunchUrl(intent);
        if (!next.equals(INITIAL_URL)) {
            loadTargetUrl(next);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQ_GEO_PERMISSION && pendingGeoCallback != null) {
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
        } else if (requestCode == REQ_PERMISSIONS && pendingWebPermRequest != null) {
            if (granted && pendingWebPermResources != null) {
                pendingWebPermRequest.grant(pendingWebPermResources);
            } else {
                pendingWebPermRequest.deny();
            }
            pendingWebPermRequest = null;
            pendingWebPermResources = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customVideoView != null) {
                customVideoContainer.removeAllViews();
                customVideoContainer.setVisibility(View.GONE);
                customVideoView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                return true;
            }
            if (popupWebView != null && popupDialog != null) {
                if (popupWebView.canGoBack()) {
                    popupWebView.goBack();
                } else {
                    closePopupDialog();
                }
                return true;
            }
            if (BACK_BEHAVIOR != 2 && webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            if (BACK_BEHAVIOR == 1) {
                new AlertDialog.Builder(this)
                    .setTitle("خروج از برنامه")
                    .setMessage("آیا مطمئن هستید که می‌خواهید از " + APP_NAME + " خارج شوید؟")
                    .setPositiveButton("خروج", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            finish();
                        }
                    })
                    .setNegativeButton("ماندن", null)
                    .show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isTrustedUrlString(String url) {
        if (url == null || url.isEmpty() || allowedOriginHost.isEmpty()) return false;
        try {
            Uri u = Uri.parse(url);
            String sch = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.US);
            if (HTTPS_ONLY && !"https".equals(sch)) return false;
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.US);
            return host.equals(allowedOriginHost) || host.endsWith("." + allowedOriginHost);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopReconnectMonitor();
        mainHandler.removeCallbacksAndMessages(null);
        closePopupDialog();
        if (webView != null) {
            webView.removeJavascriptInterface("WebinoBridge");
            webView.destroy();
        }
        super.onDestroy();
    }

    public class WebinoBridge {
        @JavascriptInterface
        public void shareText(String text) {
            if (!isTrustedOrigin()) return;
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text == null ? INITIAL_URL : text);
            startActivity(Intent.createChooser(i, APP_NAME));
        }

        @JavascriptInterface
        public boolean copyToClipboard(String text) {
            if (!CLIPBOARD_ACCESS || !isTrustedOrigin() || text == null) return false;
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return false;
            cm.setPrimaryClip(ClipData.newPlainText(APP_NAME, text));
            return true;
        }

        @JavascriptInterface
        public String getAppVersion() {
            if (!isTrustedOrigin()) return "";
            return "${escapeJava(spec.versionName)}";
        }

        private boolean isTrustedOrigin() {
            if (webView == null || webView.getUrl() == null) return false;
            return isTrustedUrlString(webView.getUrl());
        }
    }
}
""".trimIndent()
    }
}
