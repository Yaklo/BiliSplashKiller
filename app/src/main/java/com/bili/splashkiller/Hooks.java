/*
 * BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）
 * Copyright (C) 2026 Yaklo
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.bili.splashkiller;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 所有 hook 点都在这里。每个 hook 点都对应 HookItem 里的一个开关：
 * 方法照常安装，命中时先看开关，开着才改变返回值，关着就走原逻辑。
 *
 * 这样设计的好处：不需要在进程启动阶段就知道配置（那时拿不到稳定的 Context），
 * 而且开关读不到时也不会让 App 崩。
 */
public final class Hooks {

    public static final String PKG = "tv.danmaku.bili";

    /** 目标版本 */
    private static final String TARGET_VERSION = "7.61.0 (7610300)";

    private static final Set<String> sNoted = new HashSet<String>();

    private Hooks() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lp) {
        final ClassLoader cl = lp.classLoader;

        // 版本/结构自检：核心类找不到说明 B 站已改结构，直接放弃，避免一堆无意义的报错
        if (XposedHelpers.findClassIfExists("tv.danmaku.bili.ui.splash.MainSplashHelper", cl) == null
                || XposedHelpers.findClassIfExists("tv.danmaku.bili.MainActivityV2", cl) == null) {
            Config.log("未找到开屏核心类，当前 B 站版本可能已不适配（本模块适配备注: " + TARGET_VERSION + "）");
            return;
        }

        int ok = 0;
        int total = 0;

        // ===== ① 开屏显示拦截 =====
        total++; if (hookValue(cl, HookItem.AD_SPLASH_DISPLAY,
                "tv.danmaku.bili.ui.splash.MainSplashHelper", "f", null,
                "tv.danmaku.bili.ui.splash.ad.model.Splash", "boolean", "boolean")) ok++;

        total++; if (hookValue(cl, HookItem.EVENT_SPLASH,
                "tv.danmaku.bili.ui.splash.event.k", "c", null)) ok++;

        total++; if (hookValue(cl, HookItem.BRAND_SPLASH,
                "tv.danmaku.bili.ui.splash.brand.BrandSplashHelper", "f", null,
                "android.content.Context")) ok++;

        // ===== ② 启动预取 / 请求 =====
        total++; if (hookValue(cl, HookItem.PRELOAD_AD,
                "tv.danmaku.bili.ui.splash.ad.c0", "W", null)) ok++;

        total++; if (hookValue(cl, HookItem.PRELOAD_EVENT,
                "tv.danmaku.bili.ui.splash.event.k", "h", null)) ok++;

        total++; if (hookValue(cl, HookItem.PRELOAD_BRAND,
                "tv.danmaku.bili.ui.splash.brand.BrandSplashHelper", "s", null,
                "android.content.Context")) ok++;

        total++; if (hookValue(cl, HookItem.REALTIME_SPLASH,
                "tv.danmaku.bili.ui.splash.ad.c0", "w", null)) ok++;
        total++; if (hookValue(cl, HookItem.REALTIME_SPLASH,
                "tv.danmaku.bili.ui.splash.ad.c0", "v", null)) ok++;

        // ===== ③ 兜底开关 =====
        total++; if (hookValue(cl, HookItem.CACHE_SPLASH,
                "tv.danmaku.bili.ui.splash.ad.c0", "z", null,
                "tv.danmaku.bili.ui.splash.ad.model.SplashSource")) ok++;

        total++; if (hookValue(cl, HookItem.API_NULL,
                "tv.danmaku.bili.ui.splash.ad.service.c", "a", null,
                "java.lang.String", "java.lang.String", "java.lang.String")) ok++;
        total++; if (hookValue(cl, HookItem.API_NULL,
                "tv.danmaku.bili.ui.splash.ad.service.c", "b", null)) ok++;

        total++; if (hookValue(cl, HookItem.FRAGMENT_FACTORY,
                "tv.danmaku.bili.ui.splash.ad.page.x", "a", null,
                "tv.danmaku.bili.ui.splash.ad.model.Splash")) ok++;

        total++; if (hookValue(cl, HookItem.M7_HARD,
                "tv.danmaku.bili.MainActivityV2", "M7", Boolean.FALSE,
                "boolean", "boolean")) ok++;

        total++; if (hookValue(cl, HookItem.OFFICIAL_C8,
                "tv.danmaku.bili.MainActivityV2", "c8", Boolean.TRUE)) ok++;

        Config.log("hook 安装完成: " + ok + "/" + total + " 个点成功（适配 " + TARGET_VERSION + "）");
    }

    /**
     * 安装一个“开关打开时让方法直接返回指定值”的 hook。
     *
     * @param id         开关 id
     * @param className  目标类（会自动在 B 站 ClassLoader 里解析）
     * @param method     目标方法名
     * @param ret        开关打开时的返回值（void 方法传 null 表示跳过原方法体）
     * @param paramTypes 参数类型名，支持 boolean/int/long/void 与完整类名
     */
    private static boolean hookValue(ClassLoader cl, final String id, String className,
                                     String method, final Object ret, String... paramTypes) {
        try {
            final Class<?> cls = XposedHelpers.findClass(className, cl);
            int n = paramTypes == null ? 0 : paramTypes.length;
            Class<?>[] types = new Class<?>[n];
            for (int i = 0; i < n; i++) {
                types[i] = resolve(cl, paramTypes[i]);
            }
            Object[] args = new Object[n + 1];
            System.arraycopy(types, 0, args, 0, n);
            args[n] = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    boolean enabled;
                    try {
                        enabled = Config.on(id);
                    } catch (Throwable t) {
                        enabled = HookItem.defaultOf(id);
                    }
                    if (enabled) {
                        param.setResult(ret);
                        note(id);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(cls, method, args);
            return true;
        } catch (Throwable t) {
            Config.log("hook 安装失败 [" + id + "] " + className + "#" + method + " : " + t);
            return false;
        }
    }

    private static Class<?> resolve(ClassLoader cl, String name) throws ClassNotFoundException {
        if ("boolean".equals(name)) {
            return boolean.class;
        }
        if ("int".equals(name)) {
            return int.class;
        }
        if ("long".equals(name)) {
            return long.class;
        }
        if ("void".equals(name)) {
            return void.class;
        }
        return Class.forName(name, false, cl);
    }

    /** 调试日志：每个进程里每个开关只记第一行，避免刷屏 */
    private static void note(String id) {
        try {
            if (!Config.isDebug()) {
                return;
            }
            synchronized (sNoted) {
                if (!sNoted.add(id)) {
                    return;
                }
            }
            Config.log("命中 " + id);
        } catch (Throwable ignored) {
        }
    }
}
