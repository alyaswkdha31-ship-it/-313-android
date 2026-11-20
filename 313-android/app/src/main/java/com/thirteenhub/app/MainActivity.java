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
import android.content.SharedPreferences;
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
    private boolean nativeListening = false;
    private static final int AUDIO_PERMISSION_REQUEST = 313;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildChat();
        restoreHistory();
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
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.setBackgroundColor(Color.rgb(3, 10, 25));
        setContentView(root);

        // Header inspired by the supplied ONYX CONTROL command-center reference.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(5), dp(8), dp(9));
        header.setBackground(shape(Color.rgb(5, 18, 43), Color.rgb(0, 105, 255)));
        TextView menuMark = label("☰", 25, Color.WHITE, true);
        menuMark.setGravity(Gravity.CENTER);
        header.addView(menuMark, lp(dp(42), dp(54)));
        TextView eagle = label("🦅", 34, Color.WHITE, true);
        eagle.setGravity(Gravity.CENTER);
        header.addView(eagle, lp(dp(52), dp(54)));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("مركز قيادة إيلاف", 19, Color.WHITE, true);
        TextView subtitle = label("ONYX CONTROL  ×  313 AI TEAM", 10, Color.rgb(165, 205, 255), true);
        heading.addView(title, lp(-1, -2));
        heading.addView(subtitle, lp(-1, -2));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView connect = label("☁\nONYX CONNECT", 10, Color.rgb(57, 196, 255), true);
        connect.setGravity(Gravity.CENTER);
        connect.setBackground(shape(Color.rgb(5, 31, 72), Color.rgb(0, 116, 255)));
        header.addView(connect, lp(dp(82), dp(52)));
        root.addView(header, withMargins(-1, -2, 0, 0, 0, dp(8)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        // Compact icon rail keeps the layout usable on real phone screens.
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(dp(2), dp(4), dp(2), dp(4));
        nav.setBackground(shape(Color.rgb(8, 24, 52), Color.rgb(26, 72, 133)));
        body.addView(nav, lp(dp(78), -1));
        addNavItem(nav, "▣", "المحادثة", () -> chatScroll.fullScroll(View.FOCUS_DOWN), true);
        addNavItem(nav, "🎙", "يا إيلاف", this::toggleElafWakeService, false);
        addNavItem(nav, "▦", "التحكم", () -> startActivity(new Intent(this, OnyxControlActivity.class)), false);
        addNavItem(nav, "♙", "إدارة 313", () -> startActivity(new Intent(this, TeamConsoleActivity.class)), false);
        addNavItem(nav, "⚙", "الخدمات", () -> showKeyDialog(true), false);
        addNavItem(nav, "▣", "الكاميرات", () -> showSectionNotice("الكاميرات", "واجهة القسم قيد الإكمال؛ لم يتم ربط بث كاميرا بعد."), false);
        addNavItem(nav, "⌁", "الراوتر", () -> showSectionNotice("الراوتر", "إعداد صفحة الراوتر غير مضاف في هذه النسخة."), false);
        addNavItem(nav, "▤", "ONYX TV", () -> showSectionNotice("تلفزيون ONYX", "التحكم بالتلفزيون يحتاج جسر الشبكة."), false);
        addNavItem(nav, "▥", "التقارير", () -> showSectionNotice("التقارير", "قسم التقارير قيد الإكمال."), false);
        addNavItem(nav, "⬡", "الأمان", () -> showSectionNotice("الأمان", "إعدادات الأمان قيد الإكمال."), false);
        addNavItem(nav, "⚙", "الإعدادات", () -> showKeyDialog(true), false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(7), 0, 0, 0);
        body.addView(content, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(10), dp(7), dp(10), dp(7));
        statusCard.setBackground(shape(Color.rgb(7, 22, 46), Color.rgb(22, 74, 135)));
        TextView statusTitle = label("🟢  محادثة 313 الذكية", 15, Color.WHITE, true);
        TextView statusSub = label("أونكس - العقل المركزي", 11, Color.rgb(176, 198, 230), false);
        statusCard.addView(statusTitle, lp(-1, -2));
        statusCard.addView(statusSub, lp(-1, -2));
        content.addView(statusCard, withMargins(-1, -2, 0, 0, 0, dp(6)));

        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        modelRow.setPadding(dp(8), dp(2), dp(8), dp(2));
        modelRow.setBackground(shape(Color.rgb(10, 28, 58), Color.rgb(30, 76, 135)));
        TextView modelLabel = label("النموذج", 11, muted, true);
        modelRow.addView(modelLabel, lp(dp(48), dp(38)));
        modelPicker = new Spinner(this);
        String[] names = new String[]{"✨ OmniRoute • مجاني فقط"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelPicker.setAdapter(adapter);
        modelRow.addView(modelPicker, new LinearLayout.LayoutParams(0, dp(38), 1));
        content.addView(modelRow, withMargins(-1, -2, 0, 0, 0, dp(5)));

        connectionStatus = label("", 10, muted, false);
        connectionStatus.setPadding(dp(4), 0, dp(4), 0);
        content.addView(connectionStatus, withMargins(-1, -2, 0, 0, 0, dp(4)));

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        messagesBox = new LinearLayout(this);
        messagesBox.setOrientation(LinearLayout.VERTICAL);
        messagesBox.setPadding(dp(2), dp(8), dp(2), dp(8));
        chatScroll.addView(messagesBox, new ScrollView.LayoutParams(-1, -2));
        content.addView(chatScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addWelcome();

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(4), 0, dp(4));
        actions.addView(actionChip("📷", "كاميرا", () -> showSectionNotice("الكاميرا", "التقاط الصور يحتاج إكمال ربط الكاميرا.")), new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(actionChip("▧", "معرض", () -> showSectionNotice("المعرض", "اختيار الصور وإرسالها غير مفعّل في هذه النسخة.")), new LinearLayout.LayoutParams(0, dp(48), 1));
        micButton = actionChip("🎙", "تسجيل صوتي", this::startVoiceInput);
        actions.addView(micButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(actionChip("▢", "نسخ", () -> copyText(composer.getText().toString())), new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(actions, withMargins(-1, -2, 0, dp(2), 0, dp(4)));

        LinearLayout composerPanel = new LinearLayout(this);
        composerPanel.setOrientation(LinearLayout.HORIZONTAL);
        composerPanel.setGravity(Gravity.CENTER_VERTICAL);
        composerPanel.setPadding(dp(7), dp(5), dp(7), dp(5));
        composerPanel.setBackground(shape(Color.rgb(7, 25, 58), Color.rgb(0, 105, 255)));
        composer = new EditText(this);
        composer.setTextColor(Color.WHITE);
        composer.setHintTextColor(muted);
        composer.setHint("اكتب رسالتك هنا...");
        composer.setTextSize(13);
        composer.setSingleLine(false);
        composer.setMaxLines(3);
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
        Button send = button("➤", true);
        send.setTextSize(20);
        send.setOnClickListener(v -> sendMessage());
        composerPanel.addView(send, lp(dp(48), dp(46)));
        content.addView(composerPanel, lp(-1, -2));
    }

    private void addNavItem(LinearLayout nav, String icon, String title, Runnable action, boolean selected) {
        Button item = new Button(this);
        item.setText(icon + "\n" + title);
        item.setTextSize(9);
        item.setTextColor(selected ? Color.WHITE : Color.rgb(205, 220, 245));
        item.setAllCaps(false);
        item.setMinHeight(0);
        item.setPadding(dp(1), dp(2), dp(1), dp(2));
        item.setBackground(shape(selected ? Color.rgb(0, 75, 190) : Color.rgb(8, 24, 52),
                selected ? Color.rgb(0, 139, 255) : Color.rgb(24, 55, 96)));
        item.setOnClickListener(v -> action.run());
        nav.addView(item, withMargins(-1, dp(51), 0, dp(2), 0, dp(3)));
    }

    private Button actionChip(String icon, String title, Runnable action) {
        Button b = button(icon + "\n" + title, false);
        b.setTextSize(9);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showSectionNotice(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("حسناً", null).show();
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
        b.setMinHeight(0);
        b.setPadding(dp(4), dp(2), dp(4), dp(2));
        b.setBackground(shape(primary ? Color.rgb(0, 91, 230) : Color.rgb(12, 32, 66),
                primary ? Color.rgb(0, 145, 255) : Color.rgb(35, 75, 130)));
        return b;
    }

    private void addWelcome() {
        TextView welcome = label("🦅  أهلاً بك في مركز قيادة إيلاف", 17, Color.WHITE, true);
        welcome.setGravity(Gravity.CENTER);
        messagesBox.addView(welcome, withMargins(-1, dp(58), 0, dp(48), 0, dp(8)));
        TextView hint = label("ONYX CONTROL  ×  313 AI TEAM\nابدأ المحادثة مع العقل المركزي. النماذج المجانية فقط.", 12, muted, false);
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
        Button copy = button("📋 نسخ", false);
        copy.setTextSize(11);
        copy.setOnClickListener(v -> copyText(text));
        LinearLayout copyLine = new LinearLayout(this);
        copyLine.setGravity(user ? Gravity.RIGHT : Gravity.LEFT);
        copyLine.addView(copy, lp(dp(82), dp(34)));
        messagesBox.addView(copyLine, withMargins(-1, -2, 0, 0, 0, dp(8)));
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private boolean hasKey() {
        try {
            String token = TeamGatewayStore.read(this);
            return token != null && !token.trim().isEmpty();
        } catch (Exception e) { return false; }
    }

    private void refreshConnectionStatus() {
        if (!hasKey()) {
            connectionStatus.setText("○ اربط 313 بخدمة Termux من ⚙ لبدء المحادثة");
            connectionStatus.setTextColor(muted);
            return;
        }
        connectionStatus.setText("◌ رمز الربط محفوظ • جارٍ فحص خدمة 313...");
        connectionStatus.setTextColor(muted);
        worker.execute(() -> {
            JSONObject diagnostics = null;
            String failure = null;
            try {
                diagnostics = TermuxGatewayApi.diagnostics(TeamGatewayStore.read(this));
            } catch (Exception e) {
                failure = e.getMessage();
            }
            final JSONObject result = diagnostics;
            final String error = failure;
            mainHandler.post(() -> {
                if (isFinishing() || connectionStatus == null) return;
                if (result != null && result.optBoolean("ready")) {
                    connectionStatus.setText("● 313 وGitHub وOmniRoute متصلة • وضع المجاني الصارم مؤكد");
                    connectionStatus.setTextColor(Color.rgb(113, 214, 164));
                } else if (result != null) {
                    connectionStatus.setText("◌ 313 تعمل جزئياً • فحص OmniRoute/Strict يحتاج مراجعة");
                    connectionStatus.setTextColor(Color.rgb(235, 170, 115));
                } else {
                    connectionStatus.setText("○ تعذر فحص خدمة 313: " + safeDetail(error));
                    connectionStatus.setTextColor(Color.rgb(235, 170, 115));
                }
            });
        });
    }

    private void restoreHistory() {
        messagesBox.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("elafyas_chat", MODE_PRIVATE);
        String saved = prefs.getString("messages", "[]");
        try {
            JSONArray restored = new JSONArray(saved);
            for (int i = 0; i < restored.length(); i++) {
                JSONObject item = restored.optJSONObject(i);
                if (item == null) continue;
                String role = item.optString("role", "");
                String content = item.optString("content", "");
                if (!("user".equals(role) || "assistant".equals(role)) || content.trim().isEmpty()) continue;
                JSONObject clean = new JSONObject();
                clean.put("role", role);
                clean.put("content", content);
                history.put(clean);
                addBubble(content, "user".equals(role));
            }
            trimAndSaveHistory();
            if (history.length() == 0) addWelcome();
        } catch (Exception e) {
            while (history.length() > 0) history.remove(history.length() - 1);
            prefs.edit().remove("messages").apply();
            addWelcome();
        }
    }

    private void trimAndSaveHistory() {
        JSONArray trimmed = new JSONArray();
        int start = Math.max(0, history.length() - 40);
        for (int i = start; i < history.length(); i++) trimmed.put(history.opt(i));
        while (history.length() > 0) history.remove(history.length() - 1);
        for (int i = 0; i < trimmed.length(); i++) history.put(trimmed.opt(i));
        getSharedPreferences("elafyas_chat", MODE_PRIVATE).edit()
                .putString("messages", history.toString()).apply();
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
            trimAndSaveHistory();
        } catch (Exception ignored) { }

        sending = true;
        final int loadingStart = messagesBox.getChildCount();
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
                while (messagesBox.getChildCount() > loadingStart) {
                    messagesBox.removeViewAt(messagesBox.getChildCount() - 1);
                }
                if (response != null) {
                    addBubble(response, false);
                    try {
                        JSONObject assistant = new JSONObject();
                        assistant.put("role", "assistant");
                        assistant.put("content", response);
                        history.put(assistant);
                        trimAndSaveHistory();
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

    private void toggleElafWakeService() {
        if (ElafWakeService.isRunning()) {
            stopService(new Intent(this, ElafWakeService.class));
            Toast.makeText(this, "تم إيقاف الاستماع: يا إيلاف", Toast.LENGTH_SHORT).show();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 314);
            return;
        }
        try {
            Intent service = new Intent(this, ElafWakeService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            Toast.makeText(this, "بدأت خدمة يا إيلاف. اترك إشعارها ظاهراً.", Toast.LENGTH_LONG).show();
            // Android 13+ requires a runtime notification permission. The listener
            // can still run if it is denied, but its persistent notification may be hidden.
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 315);
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل خدمة يا إيلاف: " + safeDetail(e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    private void startVoiceInput() {
        if (nativeListening && speechRecognizer != null) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) { }
            nativeListening = false;
            if (micButton != null) micButton.setText("🎙\nتسجيل صوتي");
            return;
        }
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
                    nativeListening = true;
                    if (micButton != null) micButton.setText("⏹\nإيقاف التسجيل");
                }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onError(int error) {
                    nativeListening = false;
                    if (micButton != null) micButton.setText("🎙\nتسجيل صوتي");
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(MainActivity.this, "تعذر فهم الصوت، حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onResults(Bundle results) {
                    nativeListening = false;
                    if (micButton != null) micButton.setText("🎙\nتسجيل صوتي");
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
            nativeListening = false;
            if (micButton != null) micButton.setText("🎙\nتسجيل صوتي");
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
        } else if (requestCode == 314) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleElafWakeService();
            } else {
                Toast.makeText(this, "يلزم السماح بالميكروفون لتشغيل يا إيلاف", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 315) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "الاستماع يعمل، لكن قد لا يظهر إشعار يا إيلاف. فعّل الإشعارات من إعدادات التطبيق لإظهار زر الإيقاف.", Toast.LENGTH_LONG).show();
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
        if (speechRecognizer != null) {
            try { speechRecognizer.cancel(); } catch (Exception ignored) { }
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        worker.shutdownNow();
        super.onDestroy();
    }
}
