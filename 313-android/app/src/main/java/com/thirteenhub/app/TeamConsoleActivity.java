package com.thirteenhub.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Phone UI for the Termux-hosted 313 multi-agent project review service. */
public class TeamConsoleActivity extends Activity {
    private final int bg = Color.rgb(9, 11, 20);
    private final int panel = Color.rgb(20, 24, 39);
    private final int muted = Color.rgb(160, 169, 190);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private TextView output;
    private EditText task;
    private Button run;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        build();
        refreshStatus();
        if (!hasToken()) showTokenDialog(false);
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), Color.rgb(48, 55, 78));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return t;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.setBackgroundColor(bg);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("فريق مشاريع 313", 21, Color.WHITE, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button settings = new Button(this);
        settings.setText("إعداد");
        settings.setAllCaps(false);
        header.addView(settings, new LinearLayout.LayoutParams(dp(76), dp(44)));
        settings.setOnClickListener(v -> showTokenDialog(true));
        root.addView(header);

        TextView note = text("ثلاثة مراجعين يعملون بالتوازي • مجاني فقط • لا تعديل تلقائي على ملفات المصدر", 12, muted, false);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(42)));

        status = text("جارٍ فحص اتصال Termux...", 12, muted, false);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(42)));
        Button diagnostics = new Button(this);
        diagnostics.setText("فحص اتصال GitHub والنموذج المجاني");
        diagnostics.setAllCaps(false);
        root.addView(diagnostics, new LinearLayout.LayoutParams(-1, dp(46)));
        diagnostics.setOnClickListener(v -> refreshStatus());

        task = new EditText(this);
        task.setTextColor(Color.WHITE);
        task.setHintTextColor(muted);
        task.setHint("مثال: راجع مشروع 313 واكتشف الأخطاء...");
        task.setTextSize(14);
        task.setMinLines(3);
        task.setMaxLines(5);
        task.setGravity(Gravity.TOP | Gravity.RIGHT);
        task.setTextDirection(View.TEXT_DIRECTION_RTL);
        task.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        task.setPadding(dp(14), dp(12), dp(14), dp(12));
        task.setBackground(shape(panel));
        root.addView(task, new LinearLayout.LayoutParams(-1, dp(126)));

        run = new Button(this);
        run.setText("شغّل فريق المراجعة");
        run.setAllCaps(false);
        root.addView(run, new LinearLayout.LayoutParams(-1, dp(50)));
        run.setOnClickListener(v -> runReview());

        ScrollView scroll = new ScrollView(this);
        output = text("النتائج وتقارير المساعدين ستظهر هنا.", 13, Color.WHITE, false);
        output.setGravity(Gravity.RIGHT | Gravity.TOP);
        output.setPadding(dp(14), dp(14), dp(14), dp(14));
        output.setBackground(shape(panel));
        scroll.addView(output, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1);
        sp.topMargin = dp(12);
        root.addView(scroll, sp);
    }

    private boolean hasToken() {
        try {
            String token = TeamGatewayStore.read(this);
            return token != null && !token.trim().isEmpty();
        } catch (Exception e) { return false; }
    }

    private void showTokenDialog(boolean canCancel) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Gateway token from Termux");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
            .setTitle("ربط 313 مع Termux")
            .setMessage("شغّل خدمة 313 في Termux، ثم الصق رمز الربط الذي ظهر مرة واحدة أثناء الإعداد. الرمز يُحفظ مشفّراً على الهاتف.")
            .setView(input)
            .setPositiveButton("حفظ", (d, w) -> {
                try {
                    TeamGatewayStore.save(this, input.getText().toString());
                    refreshStatus();
                } catch (Exception e) {
                    Toast.makeText(this, "تعذر حفظ رمز الربط", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton(canCancel ? "إلغاء" : "لاحقاً", (d, w) -> { })
            .show();
    }

    private JSONObject request(String path, JSONObject body) throws Exception {
        String token = TeamGatewayStore.read(this);
        if (token == null || token.trim().isEmpty()) throw new IllegalStateException("أدخل رمز الربط أولاً.");
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:3131" + path).openConnection();
        try {
            c.setConnectTimeout(3500);
            c.setReadTimeout(body == null ? 5000 : 180000);
            c.setRequestProperty("X-313-Token", token);
            c.setRequestProperty("Accept", "application/json");
            if (body != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            StringBuilder raw = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) raw.append(line);
                }
            }
            JSONObject result = raw.length() == 0 ? new JSONObject() : new JSONObject(raw.toString());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException(result.optString("error", "HTTP " + code));
            }
            return result;
        } finally {
            c.disconnect();
        }
    }

    private void refreshStatus() {
        if (!hasToken()) {
            status.setText("○ أدخل رمز Termux لربط الفريق.");
            return;
        }
        status.setText("جارٍ الاتصال بخدمة Termux...");
        worker.execute(() -> {
            JSONObject result = null;
            String error = null;
            try { result = request("/api/diagnostics", null); }
            catch (Exception e) { error = e.getMessage(); }
            final JSONObject data = result;
            final String failure = error;
            main.post(() -> {
                if (data != null && data.optBoolean("ready")) {
                    status.setText("● GitHub متصل وOpenRouter يقبل المفتاح • مجاني فقط");
                    status.setTextColor(Color.rgb(113, 214, 164));
                    output.setText("فحص الاتصالات ناجح.\nGitHub: متصل • " + data.optString("github_repo") +
                        "\nOpenRouter: المفتاح صالح" +
                        "\nFree tier: " + (data.has("is_free_tier") ? data.optString("is_free_tier") : "لم يحدد المزود نوع الخطة") +
                        "\nالاستخدام: " + (data.has("usage") ? data.optString("usage") : "غير متاح") +
                        "\nالحد: " + (data.has("limit") ? data.optString("limit") : "غير متاح") +
                        "\nلا يوجد تحويل مدفوع في 313.");
                } else if (data != null) {
                    status.setText("○ فشل فحص مزود واحد أو أكثر. افتح التفاصيل بالأسفل.");
                    status.setTextColor(Color.rgb(235, 170, 115));
                    output.setText("نتيجة التشخيص:\nGitHub: " +
                        (data.optBoolean("github_ok") ? "متصل" : data.optString("github_error", "فشل")) +
                        "\nOpenRouter: " + (data.optBoolean("openrouter_ok") ? "المفتاح صالح" :
                        data.optString("openrouter_error", "فشل")) +
                        "\nلا يتم استخدام أي مسار مدفوع.");
                } else {
                    status.setText("○ لم يتصل: " + (failure == null ? "خدمة Termux غير جاهزة" : failure));
                    status.setTextColor(Color.rgb(235, 170, 115));
                }
            });
        });
    }

    private void runReview() {
        if (busy) return;
        String prompt = task.getText().toString().trim();
        if (prompt.isEmpty()) {
            task.setError("اكتب المهمة أولاً");
            return;
        }
        busy = true;
        run.setEnabled(false);
        run.setText("الفريق يعمل...");
        output.setText("يقرأ 313 ملفات المشروع من GitHub، ثم يرسل مراجعات مستقلة بالتوازي. قد يستغرق ذلك بعض الوقت.");
        worker.execute(() -> {
            JSONObject result = null;
            String error = null;
            try {
                JSONObject body = new JSONObject();
                body.put("task", prompt);
                result = request("/api/team/review", body);
            } catch (Exception e) {
                error = e.getMessage();
            }
            final JSONObject data = result;
            final String failure = error;
            main.post(() -> {
                busy = false;
                run.setEnabled(true);
                run.setText("شغّل فريق المراجعة");
                if (data == null) {
                    output.setText("لم تكتمل المهمة:\n" + (failure == null ? "خطأ غير معروف" : failure)
                        + "\n\nلم يتم التحويل إلى نموذج مدفوع.");
                    return;
                }
                String summary = data.optString("summary", "لا يوجد ملخص.");
                output.setText("اكتملت المراجعة\n"
                    + "المستودع: " + data.optString("repo") + "\n"
                    + "المساعدون: " + data.optInt("agents_succeeded") + "/" + data.optInt("agents_total") + "\n"
                    + "فرع المراجعة: " + data.optString("review_branch") + "\n"
                    + "التقرير: " + data.optString("report_path") + "\n\n"
                    + summary + "\n\nتم حفظ التقرير والذاكرة في GitHub. لم يتم تعديل ملفات المصدر أو دمج أي فرع.");
            });
        });
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
