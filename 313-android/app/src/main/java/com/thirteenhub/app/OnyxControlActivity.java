package com.thirteenhub.app;

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
import android.widget.Toast;

/** Secure in-app entry point for the hosted ONYX CONTROL dashboard. */
public final class OnyxControlActivity extends Activity {
    private static final String APP_URL =
        "https://onyx-control-git-feature-313-chat-mic-b3e0cd-alyaswkdha31-8088.vercel.app/";
    private WebView web;
    private PermissionRequest pendingRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B1220);
        getWindow().setNavigationBarColor(0xFF0B1220);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1220);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }
        });
        setContentView(web);
        if (state == null) web.loadUrl(APP_URL); else web.restoreState(state);
    }

    private void handleWebPermission(PermissionRequest request) {
        if (request.getOrigin() == null ||
            !"https".equals(request.getOrigin().getScheme()) ||
            !"onyx-control-git-feature-313-chat-mic-b3e0cd-alyaswkdha31-8088.vercel.app"
                .equals(request.getOrigin().getHost())) {
            request.deny();
            return;
        }
        boolean audio = false, camera = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) audio = true;
            else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) camera = true;
            else { request.deny(); return; }
        }
        boolean audioGranted = !audio || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean cameraGranted = !camera || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (audioGranted && cameraGranted) {
            request.grant(request.getResources());
            return;
        }
        pendingRequest = request;
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        if (audio && !audioGranted) permissions.add(Manifest.permission.RECORD_AUDIO);
        if (camera && !cameraGranted) permissions.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 23 && !permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), 313);
        } else {
            request.deny();
            pendingRequest = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != 313 || pendingRequest == null) return;
        boolean granted = results.length > 0;
        for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (granted) pendingRequest.grant(pendingRequest.getResources());
        else {
            pendingRequest.deny();
            Toast.makeText(this, "لم يتم منح صلاحية المايك أو الكاميرا", Toast.LENGTH_SHORT).show();
        }
        pendingRequest = null;
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (pendingRequest != null) { pendingRequest.deny(); pendingRequest = null; }
        if (web != null) { web.stopLoading(); web.destroy(); web = null; }
        super.onDestroy();
    }
}
