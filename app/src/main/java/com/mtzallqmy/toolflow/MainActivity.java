package com.mtzallqmy.toolflow;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

public class MainActivity extends Activity {
    private static final String PROJECT_URL = "https://flow.google.com/project/460d7b3e-e421-4f78-a019-82630bc50fa5";
    private static final String HOME_URL = "https://flow.google.com/";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        setContentView(createFallbackView());
        if (savedInstanceState == null) openFlow(resolveInitialUrl());
    }

    private String resolveInitialUrl() {
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data != null && "https".equalsIgnoreCase(data.getScheme())
                && "flow.google.com".equalsIgnoreCase(data.getHost())) return data.toString();
        return PROJECT_URL;
    }

    private LinearLayout createFallbackView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(28), dp(32), dp(28), dp(32));
        layout.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText("ToolFlow");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30f);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, matchWrap(0, 0));

        TextView message = new TextView(this);
        message.setText("يُفتح Google Flow باستخدام جلسة Chrome المحفوظة. سجّل الدخول في Chrome مرة واحدة، ثم سيستخدم ToolFlow الحساب نفسه تلقائيًا.");
        message.setTextColor(0xFFBDBDBD);
        message.setTextSize(16f);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = matchWrap(dp(22), dp(26));
        layout.addView(message, messageParams);

        Button projectButton = createButton("فتح المشروع");
        projectButton.setOnClickListener(v -> openFlow(PROJECT_URL));
        layout.addView(projectButton, matchWrap(0, dp(8)));

        Button homeButton = createButton("فتح Flow الرئيسية");
        homeButton.setOnClickListener(v -> openFlow(HOME_URL));
        layout.addView(homeButton, matchWrap(0, dp(8)));

        Button chromeSettingsButton = createButton("إعدادات Chrome");
        chromeSettingsButton.setOnClickListener(v -> openChromeSettings());
        layout.addView(chromeSettingsButton, matchWrap(0, dp(8)));
        return layout;
    }

    private Button createButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16f);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(0xFF2B2B2B);
        button.setMinHeight(dp(52));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
    }

    private void openFlow(String url) {
        CustomTabColorSchemeParams colors = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(Color.BLACK).setNavigationBarColor(Color.BLACK).build();
        CustomTabsIntent customTab = new CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colors)
                .setShowTitle(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setUrlBarHidingEnabled(true)
                .build();
        if (isPackageInstalled(CHROME_PACKAGE)) customTab.intent.setPackage(CHROME_PACKAGE);
        try {
            customTab.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException noBrowser) {
                Toast.makeText(this, "لم يتم العثور على متصفح لفتح Google Flow", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void openChromeSettings() {
        if (!isPackageInstalled(CHROME_PACKAGE)) {
            Toast.makeText(this, "Chrome غير مثبت على هذا الجهاز", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + CHROME_PACKAGE)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
