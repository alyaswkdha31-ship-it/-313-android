package com.thirteenhub.app;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        List<String> names = new ArrayList<>();
        for (RuncrateFreeModels.Model m : RuncrateFreeModels.all()) names.add(m.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelPicker.setAdapter(adapter);
        modelRow.addView(modelPicker, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(modelRow, withMargins(-1, -2, 0, dp(10), 0, dp(10)));

        connectionStatus = label("", 11, muted, false);
        root.addView(connectionStatus, withMargins(-1, -2, dp(4), 0, dp(4), dp(6)));

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
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private boolean hasKey() {
        try {
            String key = SecureCredentialStore.readRuncrateKey(this);
            return key != null && !key.trim().isEmpty();
        } catch (Exception e) { return false; }
    }

    private void refreshConnectionStatus() {
        boolean configured = hasKey();
        connectionStatus.setText(configured
                ? "● مفتاح Runcrate محفوظ بأمان على الجهاز"
                : "○ اربط مفتاح Runcrate المجاني من ⚙ لبدء المحادثة");
        connectionStatus.setTextColor(configured ? Color.rgb(113, 214, 164) : muted);
    }

    private void showKeyDialog(boolean allowSkip) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView explain = label("أدخل مفتاح Runcrate API مرة واحدة. سيُحفظ مشفّراً داخل الجهاز ولا يُرسل إلى GitHub.", 13, muted, false);
        box.addView(explain, lp(-1, -2));
        EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextSize(14);
        keyInput.setHint("Runcrate API key");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(keyInput, withMargins(-1, dp(52), 0, dp(10), 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("إعداد الاتصال داخل 313")
                .setView(box)
                .setPositiveButton("حفظ", null)
                .setNegativeButton(allowSkip ? "إلغاء" : "لاحقاً", (d, w) -> { })
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = keyInput.getText().toString().trim();
            if (value.isEmpty()) {
                keyInput.setError("أدخل المفتاح أولاً");
                return;
            }
            try {
                SecureCredentialStore.saveRuncrateKey(this, value);
                keyInput.setText("");
                dialog.dismiss();
                refreshConnectionStatus();
                Toast.makeText(this, "تم حفظ المفتاح بأمان", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "تعذر حفظ المفتاح على هذا الجهاز", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private String selectedModelId() {
        int index = modelPicker.getSelectedItemPosition();
        List<RuncrateFreeModels.Model> models = RuncrateFreeModels.all();
        if (index < 0 || index >= models.size()) return RuncrateFreeModels.GPT_OSS_120B;
        return models.get(index).id;
    }

    private void sendMessage() {
        if (sending) return;
        String prompt = composer.getText().toString().trim();
        if (prompt.isEmpty()) return;
        if (!hasKey()) {
            showKeyDialog(false);
            Toast.makeText(this, "أضف مفتاح Runcrate المجاني أولاً", Toast.LENGTH_SHORT).show();
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
        addBubble("جاري التفكير...", false);
        final String modelId = selectedModelId();
        final JSONArray requestHistory = copyArray(history);
        worker.execute(() -> {
            RuncrateApi.ChatResult result;
            try {
                String key = SecureCredentialStore.readRuncrateKey(this);
                result = RuncrateApi.chat(key, modelId, requestHistory);
            } catch (Exception e) {
                result = null;
            }
            final RuncrateApi.ChatResult response = result;
            mainHandler.post(() -> {
                sending = false;
                if (messagesBox.getChildCount() > 0) messagesBox.removeViewAt(messagesBox.getChildCount() - 1);
                if (response != null && response.success) {
                    addBubble(response.text, false);
                    try {
                        JSONObject assistant = new JSONObject();
                        assistant.put("role", "assistant");
                        assistant.put("content", response.text);
                        history.put(assistant);
                    } catch (Exception ignored) { }
                } else if (response != null && RuncrateFreeModels.isFreeLimitError(response.errorCode)) {
                    addBubble("الحصة المجانية غير متاحة حالياً أو الضغط مرتفع. لم يتم استخدام أي نموذج مدفوع. جرّب لاحقاً أو اختر نموذجاً مجانياً آخر.", false);
                } else {
                    addBubble("تعذر إكمال الطلب. تحقق من اتصال الإنترنت ومفتاح Runcrate، ثم حاول مرة أخرى.", false);
                }
            });
        });
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(composer.getWindowToken(), 0);
    }

    private JSONArray copyArray(JSONArray source) {
        JSONArray copy = new JSONArray();
        for (int i = 0; i < source.length(); i++) copy.put(source.opt(i));
        return copy;
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
