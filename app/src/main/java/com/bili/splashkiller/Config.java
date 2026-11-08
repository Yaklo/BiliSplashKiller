/*
 * BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）
 * Copyright (C) 2026 Yaklo
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.bili.splashkiller;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 在被 hook 的 B 站进程里读取模块 App 的开关状态。
 *
 * 读取顺序：
 *   1. 模块 App 提供的 ContentProvider（Android 10+ 唯一可靠方式）
 *   2. XSharedPreferences（老方案，部分环境可用）
 *   3. 内置推荐默认值
 *
 * 结果在每个进程里只读一次并缓存 —— 所以改完开关必须让 B 站进程重启才生效。
 */
public final class Config {

    public static final String MODULE_PKG   = "com.bili.splashkiller";
    public static final String AUTHORITY    = MODULE_PKG + ".config";
    public static final String PREFS_NAME   = "config";
    public static final String TAG          = "[BSK] ";

    private static volatile Map<String, Boolean> sValues;

    private Config() {
    }

    public static boolean on(String id) {
        Map<String, Boolean> v = values();
        Boolean b = v.get(id);
        return b != null ? b : HookItem.defaultOf(id);
    }

    public static boolean isDebug() {
        return on(HookItem.DEBUG_LOG);
    }

    private static Map<String, Boolean> values() {
        Map<String, Boolean> v = sValues;
        if (v != null) {
            return v;
        }
        synchronized (Config.class) {
            if (sValues == null) {
                sValues = read();
            }
            return sValues;
        }
    }

    private static Map<String, Boolean> read() {
        // 1) ContentProvider
        try {
            Context ctx = appContext();
            if (ctx != null) {
                Bundle b = ctx.getContentResolver()
                        .call(Uri.parse("content://" + AUTHORITY), "get", null, null);
                if (b != null && b.containsKey(HookItem.AD_SPLASH_DISPLAY)) {
                    Map<String, Boolean> m = new HashMap<String, Boolean>();
                    for (HookItem it : HookItem.ALL) {
                        m.put(it.id, b.getBoolean(it.id, it.defaultOn));
                    }
                    log("配置来源: ContentProvider");
                    return m;
                }
            }
        } catch (Throwable t) {
            log("ContentProvider 读取失败: " + t);
        }

        // 2) XSharedPreferences
        try {
            XSharedPreferences p = new XSharedPreferences(MODULE_PKG, PREFS_NAME);
            p.reload();
            java.util.Map<String, ?> all = p.getAll();
            if (all != null && all.containsKey(HookItem.AD_SPLASH_DISPLAY)) {
                Map<String, Boolean> m = new HashMap<String, Boolean>();
                for (HookItem it : HookItem.ALL) {
                    m.put(it.id, p.getBoolean(it.id, it.defaultOn));
                }
                log("配置来源: XSharedPreferences");
                return m;
            }
        } catch (Throwable t) {
            log("XSharedPreferences 读取失败: " + t);
        }

        // 3) 内置推荐值
        Map<String, Boolean> m = new HashMap<String, Boolean>();
        for (HookItem it : HookItem.ALL) {
            m.put(it.id, it.defaultOn);
        }
        log("读不到模块配置，使用内置默认值（请检查 LSPosed 作用域是否勾选了哔哩哔哩）");
        return m;
    }

    /** 拿一个可用的 Context（B 站进程刚起来时 Application 可能还没 attach） */
    private static Context appContext() {
        try {
            Class<?> helper = Class.forName("android.app.AndroidAppHelper");
            Object app = XposedHelpers.callStaticMethod(helper, "currentApplication");
            if (app instanceof Context) {
                return (Context) app;
            }
        } catch (Throwable ignored) {
            // AndroidAppHelper 不可用时走下面的反射
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = XposedHelpers.callStaticMethod(at, "currentActivityThread");
            if (thread != null) {
                Object ctx = XposedHelpers.callMethod(thread, "getSystemContext");
                if (ctx instanceof Context) {
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void log(String msg) {
        try {
            XposedBridge.log(TAG + msg);
        } catch (Throwable ignored) {
        }
    }
}
