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
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import org.json.JSONObject;
import java.util.ArrayList;
import android.widget.Toast;

/** Secure in-app entry point for the hosted ONYX CONTROL dashboard. */
public final class OnyxControlActivity extends Activity {
    private static final String APP_URL =
        "https://onyx-control-git-feature-313-chat-mic-b3e0cd-alyaswkdha31-8088.vercel.app/";
    private WebView web;
    private PermissionRequest pendingRequest;
    private SpeechRecognizer nativeRecognizer;
    private static final int NATIVE_AUDIO_PERMISSION = 314;

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
        web.addJavascriptInterface(new NativeSpeechBridge(), "onyxNativeSpeech");
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }
        });
        setContentView(web);
        if (state == null) web.loadUrl(APP_URL); else web.restoreState(state);
    }

    private final class NativeSpeechBridge {
        @JavascriptInterface public void start() { runOnUiThread(() -> startNativeSpeech()); }
        @JavascriptInterface public void stop() { runOnUiThread(() -> stopNativeSpeech()); }
    }

    private void startNativeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            dispatchSpeechEvent("error", "خدمة التعرف الصوتي غير متاحة على الجهاز");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, NATIVE_AUDIO_PERMISSION);
            return;
        }
        try {
            if (nativeRecognizer != null) nativeRecognizer.destroy();
            nativeRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            nativeRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { dispatchSpeechEvent("listening", ""); }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onError(int error) {
                    dispatchSpeechEvent("error", error == SpeechRecognizer.ERROR_NO_MATCH
                        ? "لم أسمع كلاماً واضحاً" : "تعذر الوصول إلى المايك أو خدمة التعرف الصوتي");
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    dispatchSpeechEvent("result", matches == null || matches.isEmpty() ? "" : matches.get(0));
                    dispatchSpeechEvent("stopped", "");
                }
                @Override public void onPartialResults(Bundle partialResults) { }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            nativeRecognizer.startListening(intent);
        } catch (Exception e) {
            dispatchSpeechEvent("error", "تعذر بدء المايكروفون");
        }
    }

    private void stopNativeSpeech() {
        try { if (nativeRecognizer != null) nativeRecognizer.stopListening(); }
        catch (Exception ignored) { }
        dispatchSpeechEvent("stopped", "");
    }

    private void dispatchSpeechEvent(String type, String text) {
        if (web == null) return;
        String safeText = JSONObject.quote(text == null ? "" : text);
        String script = "window.dispatchEvent(new CustomEvent('onyx-native-speech',{detail:{type:'"
            + type + "',text:" + safeText + "}}));";
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript(script, null); });
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
        if (requestCode == NATIVE_AUDIO_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startNativeSpeech();
            else dispatchSpeechEvent("error", "اسمح باستخدام المايكروفون لتفعيل الإملاء الصوتي");
            return;
        }
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
        if (nativeRecognizer != null) { nativeRecognizer.cancel(); nativeRecognizer.destroy(); nativeRecognizer = null; }
        if (web != null) { web.stopLoading(); web.removeJavascriptInterface("onyxNativeSpeech"); web.destroy(); web = null; }
        super.onDestroy();
    }
}
