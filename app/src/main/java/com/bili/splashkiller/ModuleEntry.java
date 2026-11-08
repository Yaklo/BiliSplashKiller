/*
 * BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）
 * Copyright (C) 2026 Yaklo
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.bili.splashkiller;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed 入口（见 assets/xposed_init）。
 */
public class ModuleEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (lp == null || lp.packageName == null) {
            return;
        }
        if (!Hooks.PKG.equals(lp.packageName)) {
            return;
        }
        try {
            Hooks.install(lp);
        } catch (Throwable t) {
            Config.log("初始化异常: " + t);
        }
    }
}
