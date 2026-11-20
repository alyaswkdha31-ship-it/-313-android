package com.onyx.elafyas.commandcenter;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://onyx-control-git-feature-313-chat-mic-b3e0cd-alyaswkdha31-8088.vercel.app/";
    private WebView web;
    private PermissionRequest pendingRequest;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B1220);
        getWindow().setNavigationBarColor(0xFF0B1220);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1220);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean needsAudio = false;
                    boolean needsCamera = false;
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needsAudio = true;
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needsCamera = true;
                    }
                    boolean audioGranted = !needsAudio || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    boolean cameraGranted = !needsCamera || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (audioGranted && cameraGranted) request.grant(request.getResources());
                    else {
                        pendingRequest = request;
                        if (Build.VERSION.SDK_INT >= 23) {
                            java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
                            if (needsAudio && !audioGranted) permissions.add(Manifest.permission.RECORD_AUDIO);
                            if (needsCamera && !cameraGranted) permissions.add(Manifest.permission.CAMERA);
                            requestPermissions(permissions.toArray(new String[0]), 41);
                        } else request.deny();
                    }
                });
            }
        });
        setContentView(web);
        if (state == null) web.loadUrl(APP_URL); else web.restoreState(state);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 41 && pendingRequest != null) {
            boolean granted = results.length > 0;
            for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
            if (granted) pendingRequest.grant(pendingRequest.getResources()); else pendingRequest.deny();
            pendingRequest = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); web = null; }
        super.onDestroy();
    }
}
