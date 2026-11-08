/*
 * BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）
 * Copyright (C) 2026 Yaklo
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.bili.splashkiller;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个可独立开关的 hook 项。
 *
 * id        —— 配置键（SharedPreferences / Bundle 里用）
 * title     —— 界面上显示的名字
 * summary   —— 这个开关具体处理了什么内容（给用户看的大白话）
 * target    —— 对应的类#方法签名（给想改代码的人看）
 * risk      —— 安全 / 注意 / 调试
 * defaultOn —— 首次安装时的默认值（推荐组合）
 */
public final class HookItem {

    public static final String GROUP_DISPLAY  = "① 开屏显示拦截";
    public static final String GROUP_PRELOAD  = "② 启动预取 / 请求";
    public static final String GROUP_FALLBACK = "③ 兜底开关（可能影响其它功能）";
    public static final String GROUP_DEBUG    = "④ 调试";

    // ===== 配置键常量 =====
    public static final String AD_SPLASH_DISPLAY   = "ad_splash_display";
    public static final String EVENT_SPLASH        = "event_splash";
    public static final String BRAND_SPLASH        = "brand_splash";
    public static final String PRELOAD_AD          = "preload_ad";
    public static final String PRELOAD_EVENT       = "preload_event";
    public static final String PRELOAD_BRAND       = "preload_brand";
    public static final String REALTIME_SPLASH     = "realtime_splash";
    public static final String CACHE_SPLASH        = "cache_splash";
    public static final String API_NULL            = "api_null";
    public static final String FRAGMENT_FACTORY    = "fragment_factory";
    public static final String M7_HARD             = "m7_hard";
    public static final String OFFICIAL_C8         = "official_c8";
    public static final String DEBUG_LOG           = "debug_log";

    public final String group;
    public final String id;
    public final String title;
    public final String summary;
    public final String target;
    public final String risk;
    public final boolean defaultOn;

    public HookItem(String group, String id, String title, String summary,
                    String target, String risk, boolean defaultOn) {
        this.group = group;
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.target = target;
        this.risk = risk;
        this.defaultOn = defaultOn;
    }

    public static final HookItem[] ALL = new HookItem[]{

        // ---------------- ① 开屏显示拦截 ----------------
        new HookItem(GROUP_DISPLAY, AD_SPLASH_DISPLAY,
            "广告开屏（图片 / 视频 / 半屏 / 竖屏故事）",
            "冷启动时不再创建广告开屏 Fragment，直接进主界面。走的是 B 站自己的“素材缺失”兜底路径。"
                + "只影响显示，B 站后台仍会预取开屏数据（要一并关掉请开②里的预取开关）。",
            "tv.danmaku.bili.ui.splash.MainSplashHelper#f(Splash, boolean, boolean) → null",
            "安全", true),

        new HookItem(GROUP_DISPLAY, EVENT_SPLASH,
            "活动开屏（节日 / 活动专题页）",
            "B 站做活动时替换整屏的活动页（Fragment tag = UserSplashFragment，含节日 logo、“新春快乐”这类页面）。"
                + "关掉后不再出现；它和广告开屏是两条独立通道，只关广告开屏拦不住它。",
            "tv.danmaku.bili.ui.splash.event.k#c() → null",
            "安全", true),

        new HookItem(GROUP_DISPLAY, BRAND_SPLASH,
            "品牌开屏（品牌定制广告）",
            "品牌方投放的定制开屏（Fragment tag = BrandSplashFragment，通常带品牌 logo 和导演稿）。"
                + "关掉后不再出现。",
            "tv.danmaku.bili.ui.splash.brand.BrandSplashHelper#f(Context) → null",
            "安全", true),

        // ---------------- ② 启动预取 / 请求 ----------------
        new HookItem(GROUP_PRELOAD, PRELOAD_AD,
            "启动时不预取广告开屏数据",
            "启动任务 Init Await 不再调度开屏广告预取：不请求 /x/v2/splash/list，也不写 splash2/splash.json 缓存，省流量、少一条后台线程。"
                + "注意：磁盘上已有的旧缓存仍可能被读取，拦显示靠①的第一项。",
            "tv.danmaku.bili.ui.splash.ad.c0#W() → 跳过原方法",
            "安全", true),

        new HookItem(GROUP_PRELOAD, PRELOAD_EVENT,
            "启动时不拉取活动开屏列表",
            "启动时不再请求活动开屏（/x/v2/splash/event/list2），活动开屏的素材与配置不会更新。",
            "tv.danmaku.bili.ui.splash.event.k#h() → 跳过原方法",
            "安全", true),

        new HookItem(GROUP_PRELOAD, PRELOAD_BRAND,
            "启动时不拉取品牌开屏配置",
            "启动时不再请求品牌开屏（/x/v2/splash/brand/config、/x/v2/splash/brand/list），品牌素材不会预下载。",
            "tv.danmaku.bili.ui.splash.brand.BrandSplashHelper#s(Context) → 跳过原方法",
            "安全", true),

        new HookItem(GROUP_PRELOAD, REALTIME_SPLASH,
            "不加载“实时开屏”（本次启动内下发的热门开屏）",
            "G8() 在每次冷启动时读取本次启动刚拿到的实时开屏（热门/热启动开屏），命中就直接展示。"
                + "关掉后这两处恒为空，只有磁盘缓存那条路能出开屏（由①兜住）。",
            "tv.danmaku.bili.ui.splash.ad.c0#w() → null ; #v() → null",
            "安全", true),

        // ---------------- ③ 兜底开关 ----------------
        new HookItem(GROUP_FALLBACK, CACHE_SPLASH,
            "不读取本地开屏缓存（显示侧总入口置空）",
            "ad.c0.z(SplashSource) 是显示侧读取开屏的总入口，冷启动、热启动、deeplink 三种来源都走它。"
                + "返回 null 后这三种来源一律没有开屏。副作用：bilibili://main/hot-splash 的热门开屏也会失效。",
            "tv.danmaku.bili.ui.splash.ad.c0#z(SplashSource) → null",
            "注意", false),

        new HookItem(GROUP_FALLBACK, API_NULL,
            "数据层置空：强制刷新与展示策略读不到数据",
            "service.c.a(...) 是“强制更新开屏素材”接口，service.c.b() 是展示策略（strategyList）接口。"
                + "置空后：强制刷新取不到素材、按素材缺失处理，展示策略列表为空。"
                + "注意它拦不住启动时的异步列表请求（那条走 retrofit 接口 service.b），要拦预取请用②组。",
            "tv.danmaku.bili.ui.splash.ad.service.c#a(String, String, String) → null ; #b() → null",
            "注意", false),

        new HookItem(GROUP_FALLBACK, FRAGMENT_FACTORY,
            "Fragment 工厂兜底（任何开屏类型都建不出来）",
            "page.x.a(Splash) 负责创建所有类型的开屏 Fragment，图片/视频/半屏/竖屏/故事以及以后新增的类型都从这里出。"
                + "返回 null 后等于“总闸”，任何开屏都建不出来。终极方案，但连未知类型一起挡掉。",
            "tv.danmaku.bili.ui.splash.ad.page.x#a(Splash) → null",
            "注意", false),

        new HookItem(GROUP_FALLBACK, M7_HARD,
            "事件 / 品牌开屏硬拦截（M7 直接返回 false）",
            "P7() 里事件与品牌两类开屏都在 M7(ZZ) 里决定。让它返回 false 可一次性拦死这两类，"
                + "并且不再去 remove 已有 Fragment。①的 2、3 项失效时用这条。",
            "tv.danmaku.bili.MainActivityV2#M7(boolean, boolean) → false",
            "注意", false),

        new HookItem(GROUP_FALLBACK, OFFICIAL_C8,
            "复用官方“不展示开屏”分支（c8 → true）",
            "c8() 是 B 站自己的判定（青少年模式 / 课程模式命中时返回 true），为 true 时走 DISABLE_SPLASH 分支、"
                + "打点 SplashShowType.NOT_SHOW，并让 G8() 不再加载实时开屏。"
                + "等于用官方逻辑关开屏；副作用是会往服务端上报“不展示”埋点，且只覆盖广告侧，不覆盖事件/品牌。",
            "tv.danmaku.bili.MainActivityV2#c8() → true",
            "注意", false),

        // ---------------- ④ 调试 ----------------
        new HookItem(GROUP_DEBUG, DEBUG_LOG,
            "详细日志",
            "每次进程生命周期内，每个 hook 首次命中时往 LSPosed 日志打一行 [BSK]，用来确认开关是否真的生效。"
                + "不影响任何行为。",
            "（不挂钩任何方法）",
            "调试", false),
    };

    public static HookItem byId(String id) {
        if (id == null) {
            return null;
        }
        for (HookItem it : ALL) {
            if (it.id.equals(id)) {
                return it;
            }
        }
        return null;
    }

    /** 配置读不到时的兜底值：沿用推荐值 */
    public static boolean defaultOf(String id) {
        HookItem it = byId(id);
        return it != null && it.defaultOn;
    }

    public static List<String> groups() {
        List<String> gs = new ArrayList<String>();
        for (HookItem it : ALL) {
            if (!gs.contains(it.group)) {
                gs.add(it.group);
            }
        }
        return gs;
    }

    public static List<HookItem> ofGroup(String group) {
        List<HookItem> out = new ArrayList<HookItem>();
        for (HookItem it : ALL) {
            if (it.group.equals(group)) {
                out.add(it);
            }
        }
        return out;
    }
}
