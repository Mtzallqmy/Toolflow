package com.mtzallqmy.toolflow;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String PROJECT_URL = "https://flow.google.com/project/460d7b3e-e421-4f78-a019-82630bc50fa5";
    private static final String HOME_URL = "https://flow.google.com/";
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private static final String PREFS = "toolflow_prefs";
    private static final String KEY_LAST_FLOW_URL = "last_flow_url";

    private static final int FILE_CHOOSER_REQUEST = 2001;
    private static final int WEB_PERMISSION_REQUEST = 2002;
    private static final int STORAGE_PERMISSION_REQUEST = 2003;

    private FrameLayout root;
    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private PermissionRequest pendingPermissionRequest;
    private final Map<WebView, Dialog> popupDialogs = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView menuButton = createMenuButton();
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        menuParams.gravity = Gravity.END | Gravity.BOTTOM;
        menuParams.setMargins(dp(8), dp(8), dp(10), dp(12));
        root.addView(menuButton, menuParams);

        setContentView(root);
        configureWebView(webView);
        installDownloadHandler(webView);

        String initialUrl = resolveInitialUrl();
        webView.loadUrl(initialUrl);
    }

    private String resolveInitialUrl() {
        Uri deepLink = getIntent() != null ? getIntent().getData() : null;
        if (deepLink != null && isFlowHost(deepLink.getHost())) {
            return deepLink.toString();
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String last = prefs.getString(KEY_LAST_FLOW_URL, null);
        if (last != null && last.startsWith("https://flow.google.com/")) {
            return last;
        }
        return PROJECT_URL;
    }

    private TextView createMenuButton() {
        TextView button = new TextView(this);
        button.setText("⋮");
        button.setTextSize(26f);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription("قائمة ToolFlow");
        button.setElevation(dp(6));

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCC171717);
        background.setCornerRadius(dp(22));
        background.setStroke(dp(1), 0x66444444);
        button.setBackground(background);
        button.setOnClickListener(this::showMenu);
        return button;
    }

    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setSaveFormData(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(DESKTOP_UA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(view, true);

        view.setWebViewClient(new FlowWebViewClient());
        view.setWebChromeClient(new FlowChromeClient());
    }

    private void installDownloadHandler(WebView target) {
        target.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                downloadFile(url, userAgent, contentDisposition, mimeType)
        );
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "إعادة تحميل");
        popup.getMenu().add(0, 2, 1, "فتح المشروع الأساسي");
        popup.getMenu().add(0, 3, 2, "الرئيسية في Flow");
        popup.getMenu().add(0, 4, 3, "فتح في المتصفح الخارجي");
        popup.getMenu().add(0, 5, 4, "مسح جلسة تسجيل الدخول");
        popup.getMenu().add(0, 6, 5, "حول ToolFlow");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    webView.reload();
                    return true;
                case 2:
                    webView.loadUrl(PROJECT_URL);
                    return true;
                case 3:
                    webView.loadUrl(HOME_URL);
                    return true;
                case 4:
                    openExternal(webView.getUrl() != null ? webView.getUrl() : PROJECT_URL);
                    return true;
                case 5:
                    confirmClearSession();
                    return true;
                case 6:
                    showAbout();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void confirmClearSession() {
        new AlertDialog.Builder(this)
                .setTitle("مسح الجلسة")
                .setMessage("سيتم حذف كوكيز وبيانات WebView المحلية ثم فتح Flow من جديد. لن يتم حذف أي شيء من حساب Google نفسه.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("مسح", (dialog, which) -> clearSession())
                .show();
    }

    private void clearSession() {
        CookieManager.getInstance().removeAllCookies(value -> {
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();
            webView.clearCache(true);
            webView.clearHistory();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_LAST_FLOW_URL).apply();
            webView.loadUrl(HOME_URL);
            Toast.makeText(this, "تم مسح الجلسة المحلية", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAbout() {
        String message = "ToolFlow يعرض نسخة سطح المكتب من Google Flow داخل WebView مع حفظ الكوكيز محليًا لتقليل تكرار تسجيل الدخول.\n\n"
                + "التطبيق لا يطلب كلمة مرور Google في نموذج خاص به ولا يخزنها. صفحة الدخول، إن ظهرت، تأتي من Google نفسها.\n\n"
                + "ملاحظة: قد تمنع Google تسجيل الدخول داخل WebView على بعض الأجهزة أو الحسابات. عند ذلك استخدم خيار «فتح في المتصفح الخارجي».";

        new AlertDialog.Builder(this)
                .setTitle("ToolFlow")
                .setMessage(message)
                .setPositiveButton("حسنًا", null)
                .show();
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        Uri origin = request.getOrigin();
        String host = origin != null ? origin.getHost() : null;
        if (!isTrustedGoogleHost(host)) {
            request.deny();
            return;
        }

        List<String> runtimePermissions = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                runtimePermissions.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                runtimePermissions.add(Manifest.permission.RECORD_AUDIO);
            }
        }

        List<String> missing = new ArrayList<>();
        for (String permission : runtimePermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED && !missing.contains(permission)) {
                missing.add(permission);
            }
        }

        if (missing.isEmpty()) {
            request.grant(request.getResources());
        } else {
            pendingPermissionRequest = request;
            requestPermissions(missing.toArray(new String[0]), WEB_PERMISSION_REQUEST);
        }
    }

    private boolean isTrustedGoogleHost(String host) {
        if (host == null) return false;
        return host.equals("google.com")
                || host.endsWith(".google.com")
                || host.endsWith(".googleusercontent.com")
                || host.endsWith(".gstatic.com");
    }

    private boolean isFlowHost(String host) {
        return host != null && host.equals("flow.google.com");
    }

    private void downloadFile(String url, String userAgent, String contentDisposition, String mimeType) {
        if (url == null) return;
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            Toast.makeText(this, "هذا التنزيل يستخدم رابطًا داخليًا. استخدم زر الحفظ/التنزيل داخل Flow إن ظهر.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
            Toast.makeText(this, "امنح إذن التخزين ثم اضغط التنزيل مرة أخرى.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            if (mimeType != null) request.setMimeType(mimeType);
            request.setTitle(fileName);
            request.setDescription("تنزيل من Google Flow");
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "بدأ التنزيل: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر بدء التنزيل", Toast.LENGTH_LONG).show();
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "لا يوجد متصفح متاح", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean handleSpecialUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("http") || scheme.equals("https") || scheme.equals("about") || scheme.equals("blob")) {
            return false;
        }

        try {
            if (scheme.equals("intent")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الرابط الخارجي", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WEB_PERMISSION_REQUEST && pendingPermissionRequest != null) {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            PermissionRequest request = pendingPermissionRequest;
            pendingPermissionRequest = null;
            if (allGranted) {
                request.grant(request.getResources());
            } else {
                request.deny();
            }
        }
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        CookieManager.getInstance().flush();
        for (Map.Entry<WebView, Dialog> entry : new ArrayList<>(popupDialogs.entrySet())) {
            try {
                entry.getValue().dismiss();
                entry.getKey().destroy();
            } catch (Exception ignored) {
            }
        }
        popupDialogs.clear();

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class FlowWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleSpecialUrl(request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleSpecialUrl(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            CookieManager.getInstance().flush();
            Uri uri = Uri.parse(url);
            if (isFlowHost(uri.getHost())) {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putString(KEY_LAST_FLOW_URL, url)
                        .apply();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                Toast.makeText(MainActivity.this, "تعذر تحميل الصفحة. تحقق من الاتصال ثم أعد المحاولة.", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (view == webView) {
                Toast.makeText(MainActivity.this, "تمت إعادة تشغيل WebView بعد توقفه.", Toast.LENGTH_SHORT).show();
                root.removeView(webView);
                try {
                    webView.destroy();
                } catch (Exception ignored) {
                }
                webView = null;
                recreate();
                return true;
            }
            return false;
        }
    }

    private class FlowChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (fileChooserCallback != null) {
                fileChooserCallback.onReceiveValue(null);
            }
            fileChooserCallback = filePathCallback;

            try {
                Intent intent = fileChooserParams.createIntent();
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                return true;
            } catch (ActivityNotFoundException e) {
                fileChooserCallback = null;
                Toast.makeText(MainActivity.this, "لا يوجد تطبيق لاختيار الملف", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            runOnUiThread(() -> handleWebPermissionRequest(request));
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (pendingPermissionRequest == request) pendingPermissionRequest = null;
            super.onPermissionRequestCanceled(request);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
            WebView popup = new WebView(MainActivity.this);
            popup.setBackgroundColor(Color.BLACK);
            configureWebView(popup);
            installDownloadHandler(popup);
            dialog.setContentView(popup, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            dialog.setOnDismissListener(d -> {
                popupDialogs.remove(popup);
                try {
                    popup.stopLoading();
                    popup.destroy();
                } catch (Exception ignored) {
                }
            });
            popupDialogs.put(popup, dialog);
            dialog.show();

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Dialog dialog = popupDialogs.remove(window);
            if (dialog != null) dialog.dismiss();
            super.onCloseWindow(window);
        }
    }
}
