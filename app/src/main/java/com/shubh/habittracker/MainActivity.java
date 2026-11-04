package com.shubh.habittracker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    // Start.io App ID (Habit Tracker Shubh)
    private static final String START_IO_APP_ID = "210181677";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android 7-9 ke liye storage permission (PDF save)
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // ---- Start.io SDK ----
        // Debug APK = test ads (safe). Release APK = real ads.
        try {
            StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG);
            StartAppSDK.initParams(this, START_IO_APP_ID)
                    .setReturnAdsEnabled(false)
                    .init();
        } catch (Throwable ignored) {
        }

        // ---- Layout: upar WebView, neeche Banner ----
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        try {
            Banner banner = new Banner(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.gravity = Gravity.CENTER_HORIZONTAL;
            root.addView(banner, bp);
        } catch (Throwable ignored) {
        }

        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // localStorage (habit data save) ke liye zaroori
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new Downloader(), "AndroidDownloader");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");

            // Interstitial ad: app khulne ke ~5 second baad, sirf ek baar
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    StartAppAd.showAd(MainActivity.this);
                } catch (Throwable ignored) {
                }
            }, 5000);
        }
    }

    /** JS se aayi file ko phone ke Downloads folder me save karta hai. */
    private class Downloader {
        @JavascriptInterface
        public boolean save(String filename, String mime, String base64) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safeName = filename.replaceAll("[\\\\/:*?\"<>|]", "_");

                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    ContentResolver r = getContentResolver();
                    Uri uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return false;
                    OutputStream out = r.openOutputStream(uri);
                    if (out == null) return false;
                    out.write(bytes);
                    out.close();
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    FileOutputStream out = new FileOutputStream(new File(dir, safeName));
                    out.write(bytes);
                    out.close();
                }

                final String shown = safeName;
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Saved in Downloads: " + shown, Toast.LENGTH_LONG).show());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
