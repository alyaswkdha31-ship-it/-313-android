package com.thirteenhub.app;

import android.app.Activity;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 11, 20);
    private final int panel = Color.rgb(20, 24, 39);
    private final int muted = Color.rgb(160, 169, 190);
    private final int accent = Color.rgb(174, 145, 255);
    private LinearLayout root;
    private LinearLayout messagesBox;
    private ScrollView chatScroll;
    private EditText composer;
    private Spinner modelPicker;
    private TextView connectionStatus;
    private final JSONArray history = new JSONArray();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean sending = false;
    private SpeechRecognizer speechRecognizer;
    private Button micButton;
    private static final int AUDIO_PERMISSION_REQUEST = 313;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildChat();
        refreshConnectionStatus();
        if (!hasKey()) showKeyDialog(false);
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private void buildChat() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(bg);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = label("313", 25, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER);
        header.addView(brand, lp(58, dp(48)));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("محادثة الذكاء الاصطناعي", 16, Color.WHITE, true);
        TextView subtitle = label("نماذج مجانية فقط • بدون تحويل مدفوع", 11, muted, false);
        heading.addView(title, lp(-1, -2));
        heading.addView(subtitle, lp(-1, -2));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

        Button settings = button("⚙", false);
        settings.setOnClickListener(v -> showKeyDialog(true));
        header.addView(settings, lp(dp(48), dp(44)));
        root.addView(header, lp(-1, -2));

        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        modelRow.setPadding(dp(10), dp(4), dp(10), dp(4));
        modelRow.setBackground(shape(panel, Color.rgb(43, 49, 70)));
        TextView modelLabel = label("النموذج", 12, muted, true);
        modelRow.addView(modelLabel, lp(dp(62), dp(42)));
        modelPicker = new Spinner(this);
        String[] names = new String[]{"OpenRouter Free Router • تلقائي"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelPicker.setAdapter(adapter);
        modelRow.addView(modelPicker, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(modelRow, withMargins(-1, -2, 0, dp(10), 0, dp(10)));

        connectionStatus = label("", 11, muted, false);
        root.addView(connectionStatus, withMargins(-1, -2, dp(4), 0, dp(4), dp(6)));

        Button teamButton = button("فريق المشاريع", true);
        teamButton.setOnClickListener(v -> startActivity(new Intent(this, TeamConsoleActivity.class)));
        root.addView(teamButton, withMargins(-1, -2, 0, 0, 0, dp(8)));

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        messagesBox = new LinearLayout(this);
        messagesBox.setOrientation(LinearLayout.VERTICAL);
        messagesBox.setPadding(0, dp(8), 0, dp(12));
        chatScroll.addView(messagesBox, new ScrollView.LayoutParams(-1, -2));
        root.addView(chatScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addWelcome();

        LinearLayout composerPanel = new LinearLayout(this);
        composerPanel.setOrientation(LinearLayout.HORIZONTAL);
        composerPanel.setGravity(Gravity.BOTTOM);
        composerPanel.setPadding(dp(8), dp(6), dp(8), dp(6));
        composerPanel.setBackground(shape(panel, Color.rgb(48, 54, 76)));
        composer = new EditText(this);
        composer.setTextColor(Color.WHITE);
        composer.setHintTextColor(muted);
        composer.setHint("اكتب رسالتك هنا...");
        composer.setTextSize(14);
        composer.setSingleLine(false);
        composer.setMaxLines(4);
        composer.setMinLines(1);
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.setImeOptions(EditorInfo.IME_ACTION_SEND);
        composer.setBackgroundColor(Color.TRANSPARENT);
        composer.setTextDirection(View.TEXT_DIRECTION_RTL);
        composer.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        composer.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });
        composerPanel.addView(composer, new LinearLayout.LayoutParams(0, -2, 1));
        micButton = button("🎙", false);
        micButton.setContentDescription("إملاء صوتي");
        micButton.setOnClickListener(v -> startVoiceInput());
        composerPanel.addView(micButton, lp(dp(48), dp(46)));
        Button send = button("إرسال", true);
        send.setOnClickListener(v -> sendMessage());
        composerPanel.addView(send, lp(dp(76), dp(46)));
        root.addView(composerPanel, withMargins(-1, -2, 0, dp(8), 0, 0));
    }

    private LinearLayout.LayoutParams withMargins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = lp(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(shape(primary ? Color.rgb(105, 78, 180) : panel,
                primary ? 0 : Color.rgb(55, 61, 82)));
        return b;
    }

    private void addWelcome() {
        TextView welcome = label("أهلاً بك في 313", 21, Color.WHITE, true);
        welcome.setGravity(Gravity.CENTER);
        messagesBox.addView(welcome, withMargins(-1, dp(58), 0, dp(48), 0, dp(8)));
        TextView hint = label("ابدأ محادثتك مباشرة. اختر نموذجاً مجانياً من القائمة، واكتب سؤالك بالأسفل.", 14, muted, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(18), 0, dp(18), 0);
        messagesBox.addView(hint, lp(-1, -2));
    }

    private void addBubble(String text, boolean user) {
        TextView bubble = label(text, 14, Color.WHITE, false);
        bubble.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setBackground(shape(user ? Color.rgb(67, 55, 112) : panel,
                user ? Color.rgb(104, 83, 163) : Color.rgb(47, 55, 75)));
        LinearLayout line = new LinearLayout(this);
        line.setGravity(user ? Gravity.RIGHT : Gravity.LEFT);
        line.addView(bubble, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams p = withMargins(-1, -2, 0, dp(5), 0, dp(9));
        messagesBox.addView(line, p);
        if (!user) {
            Button copy = button("نسخ", false);
            copy.setTextSize(11);
            copy.setOnClickListener(v -> copyText(text));
            LinearLayout copyLine = new LinearLayout(this);
            copyLine.setGravity(Gravity.LEFT);
            copyLine.addView(copy, lp(dp(64), dp(34)));
            messagesBox.addView(copyLine, withMargins(-1, -2, 0, 0, 0, dp(8)));
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private boolean hasKey() {
        try {
            String token = TeamGatewayStore.read(this);
            return token != null && !token.trim().isEmpty();
        } catch (Exception e) { return false; }
    }

    private void refreshConnectionStatus() {
        boolean configured = hasKey();
        connectionStatus.setText(configured
                ? "● رمز ربط Termux محفوظ • المحادثة عبر الموجّه المجاني"
                : "○ اربط 313 بخدمة Termux من ⚙ لبدء المحادثة");
        connectionStatus.setTextColor(configured ? Color.rgb(113, 214, 164) : muted);

    }

    private void showKeyDialog(boolean allowSkip) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView explain = label("الصق رمز الربط الذي طبعته خدمة 313 في Termux. مفتاح OpenRouter يبقى داخل Termux ولا يوضع في التطبيق.", 13, muted, false);
        box.addView(explain, lp(-1, -2));
        EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextSize(14);
        keyInput.setHint("313 Gateway token");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(keyInput, withMargins(-1, dp(52), 0, dp(10), 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ربط 313 مع Termux")
                .setView(box)
                .setPositiveButton("حفظ", null)
                .setNegativeButton(allowSkip ? "إلغاء" : "لاحقاً", (d, w) -> { })
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = keyInput.getText().toString().trim();
            if (value.isEmpty()) {
                keyInput.setError("أدخل رمز الربط أولاً");
                return;
            }
            try {
                TeamGatewayStore.save(this, value);
                keyInput.setText("");
                dialog.dismiss();
                refreshConnectionStatus();
                Toast.makeText(this, "تم حفظ رمز الربط بأمان", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "تعذر حفظ رمز الربط", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void sendMessage() {
        if (sending) return;
        String prompt = composer.getText().toString().trim();
        if (prompt.isEmpty()) return;
        if (!hasKey()) {
            showKeyDialog(false);
            Toast.makeText(this, "اربط 313 مع Termux أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        composer.setText("");
        addBubble(prompt, true);
        try {
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", prompt);
            history.put(user);
        } catch (Exception ignored) { }

        sending = true;
        addBubble("جاري الاتصال بالموجّه المجاني...", false);
        final JSONArray requestHistory = copyArray(history);
        worker.execute(() -> {
            String answer = null;
            String failure = null;
            try {
                String token = TeamGatewayStore.read(this);
                answer = TermuxGatewayApi.chat(token, requestHistory);
            } catch (Exception e) {
                failure = describeLocalFailure(e);
            }
            final String response = answer;
            final String error = failure;
            mainHandler.post(() -> {
                sending = false;
                if (messagesBox.getChildCount() > 0) messagesBox.removeViewAt(messagesBox.getChildCount() - 1);
                if (response != null) {
                    addBubble(response, false);
                    try {
                        JSONObject assistant = new JSONObject();
                        assistant.put("role", "assistant");
                        assistant.put("content", response);
                        history.put(assistant);
                    } catch (Exception ignored) { }
                } else {
                    addBubble(error == null ? "تعذر تحديد سبب فشل الاتصال." : error, false);
                }
            });
        });
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(composer.getWindowToken(), 0);
    }


    private void copyText(String value) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "تعذر الوصول إلى الحافظة", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("313 reply", value));
        Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show();
    }

    private void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "خدمة الإملاء الصوتي غير متاحة على الجهاز", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
            return;
        }
        try {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    if (micButton != null) micButton.setText("●");
                }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() {
                    if (micButton != null) micButton.setText("🎙");
                }
                @Override public void onError(int error) {
                    if (micButton != null) micButton.setText("🎙");
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(MainActivity.this, "تعذر فهم الصوت، حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onResults(Bundle results) {
                    if (micButton != null) micButton.setText("🎙");
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String current = composer.getText().toString().trim();
                        String recognized = matches.get(0).trim();
                        composer.setText(current.isEmpty() ? recognized : current + " " + recognized);
                        composer.setSelection(composer.getText().length());
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) { }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            speechRecognizer.startListening(intent);
            Toast.makeText(this, "تحدث الآن", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (micButton != null) micButton.setText("🎙");
            Toast.makeText(this, "تعذر بدء المايكروفون", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "اسمح باستخدام المايكروفون لتفعيل الإملاء الصوتي", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String describeLocalFailure(Exception error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        String lower = message.toLowerCase();
        if (lower.contains("failed to connect") || lower.contains("connection refused"))
            return "خدمة 313 غير شغالة في Termux. افتح Termux وشغّل خادم 313 أولاً.";
        if (lower.contains("unauthorized") || lower.contains("http 401"))
            return "رمز الربط غير صحيح. افتح إعداد فريق المشاريع وأعد حفظ الرمز الذي ظهر في Termux.";
        if (lower.contains("openrouter") || lower.contains("http 402") || lower.contains("http 403")
                || lower.contains("http 429") || lower.contains("quota") || lower.contains("rate limit"))
            return "المزوّد المجاني رفض الطلب أو وصل إلى حد الاستخدام: " + safeDetail(message);
        if (error instanceof java.net.SocketTimeoutException)
            return "انتهت مهلة انتظار النموذج المجاني. حاول مرة أخرى بعد قليل.";
        return "تعذر تنفيذ الطلب: " + safeDetail(message);
    }

    private String safeDetail(String value) {
        if (value == null || value.trim().isEmpty()) return "لا توجد تفاصيل إضافية";
        String safe = value.replaceAll("(?i)(sk-or-v1-|gh[pousr]_|github_pat_)[A-Za-z0-9_-]+", "[hidden]");
        return safe.length() > 400 ? safe.substring(0, 400) : safe;
    }

    private JSONArray copyArray(JSONArray source) {
        JSONArray copy = new JSONArray();
        for (int i = 0; i < source.length(); i++) copy.put(source.opt(i));
        return copy;
    }

    @Override protected void onDestroy() {
        if (speechRecognizer != null) { speechRecognizer.destroy(); speechRecognizer = null; }
        worker.shutdownNow();
        super.onDestroy();
    }
}
