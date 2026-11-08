/*
 * BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）
 * Copyright (C) 2026 Yaklo
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.bili.splashkiller.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.bili.splashkiller.Config;
import com.bili.splashkiller.HookItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int COLOR_BG      = Color.parseColor("#F2F3F5");
    private static final int COLOR_CARD    = Color.WHITE;
    private static final int COLOR_TEXT    = Color.parseColor("#18191C");
    private static final int COLOR_SUB     = Color.parseColor("#61666D");
    private static final int COLOR_MONO    = Color.parseColor("#9499A0");
    private static final int COLOR_ACCENT  = Color.parseColor("#00AEEC");
    private static final int COLOR_WARN    = Color.parseColor("#FF6699");

    private SharedPreferences sp;
    private final Map<String, Switch> switches = new LinkedHashMap<String, Switch>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(COLOR_BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        root.setPadding(pad, dp(22), pad, dp(28));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 标题
        TextView title = new TextView(this);
        title.setText("哔哩哔哩 · 开屏广告开关");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(21f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        // 说明
        TextView head = new TextView(this);
        head.setText("适配版本：7.61.0 (7610300)\n"
                + "每个开关独立生效，改完请到 LSPosed 里强制停止哔哩哔哩再启动（配置在整个进程生命周期内只读一次）。");
        head.setTextColor(COLOR_SUB);
        head.setTextSize(12.5f);
        head.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.topMargin = dp(8);
        root.addView(head, headLp);

        // 批量按钮
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnRowLp.topMargin = dp(14);
        root.addView(btnRow, btnRowLp);
        btnRow.addView(makeButton("全部开启", true));
        btnRow.addView(makeButton("全部关闭", false));
        btnRow.addView(makeButton("恢复推荐", null));

        // 分组列表
        List<String> groups = HookItem.groups();
        for (String g : groups) {
            List<HookItem> items = HookItem.ofGroup(g);
            if (items.isEmpty()) {
                continue;
            }
            TextView section = new TextView(this);
            section.setText(g);
            section.setTextColor(COLOR_ACCENT);
            section.setTextSize(13f);
            section.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sectionLp.topMargin = dp(20);
            sectionLp.bottomMargin = dp(2);
            root.addView(section, sectionLp);

            for (HookItem it : items) {
                root.addView(itemCard(it));
            }
        }

        // 页脚
        TextView footer = new TextView(this);
        footer.setText("\n说明\n"
                + "· 本模块只改返回值，不 patch APK，B 站升级后类名一旦变化会静默失效，可在 LSPosed 日志里搜 [BSK] 查看安装结果。\n"
                + "· 「安全」= 走 B 站自身的兜底分支，不影响其它功能；「注意」= 会顺带影响热启动 / deeplink 开屏或上报埋点，出问题先关这类。\n"
                + "· 开屏广告是 MainActivityV2 里的 Fragment，不是独立 Activity，所以禁用组件的方法关不掉它。");
        footer.setTextColor(COLOR_MONO);
        footer.setTextSize(11.5f);
        footer.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(24);
        root.addView(footer, footerLp);

        return scroll;
    }

    private Button makeButton(String text, final Boolean value) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13f);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(6);
        b.setLayoutParams(lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAll(value);
            }
        });
        return b;
    }

    private void applyAll(Boolean value) {
        SharedPreferences.Editor ed = sp.edit();
        for (HookItem it : HookItem.ALL) {
            boolean v = value == null ? it.defaultOn : value;
            ed.putBoolean(it.id, v);
            Switch sw = switches.get(it.id);
            if (sw != null) {
                sw.setChecked(v);
            }
        }
        ed.apply();
    }

    private View itemCard(final HookItem it) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(COLOR_CARD);
        int p = dp(13);
        card.setPadding(p, dp(11), p, dp(12));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(9);
        card.setLayoutParams(cardLp);

        // 第一行：标题 + 风险标签 + 开关
        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(it.title);
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(14.5f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = new TextView(this);
        badge.setText(it.risk);
        boolean warn = !"安全".equals(it.risk);
        badge.setTextColor(warn ? Color.WHITE : COLOR_ACCENT);
        badge.setTextSize(10.5f);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setBackgroundColor(warn ? COLOR_WARN : Color.parseColor("#E3F7FD"));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.rightMargin = dp(8);
        headRow.addView(badge, badgeLp);

        final Switch sw = new Switch(this);
        sw.setChecked(sp.getBoolean(it.id, it.defaultOn));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(it.id, isChecked).apply();
            }
        });
        headRow.addView(sw);
        switches.put(it.id, sw);

        card.addView(headRow);

        // 说明：这个开关处理了什么
        TextView summary = new TextView(this);
        summary.setText(it.summary);
        summary.setTextColor(COLOR_SUB);
        summary.setTextSize(12.5f);
        summary.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryLp.topMargin = dp(6);
        card.addView(summary, summaryLp);

        // 技术位置
        TextView target = new TextView(this);
        target.setText(it.target);
        target.setTextColor(COLOR_MONO);
        target.setTextSize(11f);
        target.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams targetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        targetLp.topMargin = dp(5);
        card.addView(target, targetLp);

        // 点卡片任意空白处也能切换
        View.OnClickListener toggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
            }
        };
        card.setOnClickListener(toggle);
        summary.setOnClickListener(toggle);
        headRow.setOnClickListener(toggle);

        return card;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
