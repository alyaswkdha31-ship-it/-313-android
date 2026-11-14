package com.thirteenhub.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 11, 20);
    private final int panel = Color.rgb(20, 24, 39);
    private final int muted = Color.rgb(160, 169, 190);
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildHome();
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(20));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28)); root.setBackgroundColor(bg);
        scroll.addView(root); setContentView(scroll);

        TextView brand = text("313", 34, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER); root.addView(brand, matchWrap());
        TextView subtitle = text("مركز قيادة الذكاء الاصطناعي", 15, muted, false);
        subtitle.setGravity(Gravity.CENTER); root.addView(subtitle, matchWrap());
        space(22);

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20)); hero.setBackground(shape(Color.rgb(30, 28, 58), Color.rgb(91, 75, 160)));
        TextView welcome = text("كل أدواتك الذكية في مكان واحد", 20, Color.WHITE, true);
        hero.addView(welcome, matchWrap());
        TextView desc = text("ابدأ محادثة، أدر مشاريعك، ونسّق العمل بين المساعدين.", 14, Color.rgb(207, 205, 232), false);
        LinearLayout.LayoutParams dpms = matchWrap(); dpms.topMargin = dp(8); hero.addView(desc, dpms);
        root.addView(hero, matchWidth());
        space(24);

        TextView section = text("المساعدون والخدمات", 18, Color.WHITE, true); root.addView(section, matchWrap());
        space(10);
        addService("✦", "ChatGPT", "التخطيط والتحليل والمراجعة", "https://chatgpt.com/");
        addService("◈", "Claude", "الكتابة والبرمجة وتحليل الملفات", "https://claude.ai/");
        addService("✧", "Gemini", "البحث والمساعدة الذكية", "https://gemini.google.com/");
        addService("⌘", "GitHub", "المستودعات والفروع وطلبات الدمج", "https://github.com/");
        addService("▣", "Codex", "مهام البرمجة والعمل على المشاريع", "https://chatgpt.com/codex/");
        space(20);
        TextView note = text("الإصدار الأول: واجهة المركز جاهزة. الربط المباشر وتبادل المهام يحتاجان إعداد تسجيل الدخول ومفاتيح الخدمات بأمان.", 12, muted, false);
        note.setGravity(Gravity.CENTER); root.addView(note, matchWrap());
    }

    private void addService(String icon, String name, String detail, String url) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(15), dp(13), dp(15), dp(13));
        card.setBackground(shape(panel, Color.rgb(42, 48, 69)));
        TextView symbol = text(icon, 25, Color.rgb(185, 164, 255), true);
        symbol.setGravity(Gravity.CENTER); card.addView(symbol, new LinearLayout.LayoutParams(dp(42), dp(48)));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL); labels.setPadding(dp(10), 0, dp(8), 0);
        TextView title = text(name, 16, Color.WHITE, true); labels.addView(title, matchWrap());
        TextView sub = text(detail, 12, muted, false); labels.addView(sub, matchWrap());
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView go = text("فتح ←", 12, Color.rgb(190, 174, 255), true); go.setGravity(Gravity.CENTER);
        card.addView(go, new LinearLayout.LayoutParams(dp(58), dp(42)));
        card.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception e) { Toast.makeText(this, "تعذر فتح الخدمة", Toast.LENGTH_SHORT).show(); }
        });
        LinearLayout.LayoutParams p = matchWidth(); p.bottomMargin = dp(10); root.addView(card, p);
    }
    private void space(int h) { View v = new View(this); root.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams matchWidth() { return new LinearLayout.LayoutParams(-1, -2); }
}
