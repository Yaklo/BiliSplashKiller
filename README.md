# BiliSplashKiller — 哔哩哔哩开屏广告开关（LSPosed 模块）

针对 **哔哩哔哩 7.61.0** 的开屏广告拦截模块。和常见"一键全关"的模块不同，它把开屏链路上的
每个点拆成**独立开关**，App 里每一项都写明"这个开关到底处理了什么"，可以只关你不想要的那一层。

一切都在方法层面拦截：不改 APK、不 hook 系统、不禁用组件。

---

## 1. 目录结构

```
BiliSplashKiller/
├── scripts/build.sh                   # 命令行构建脚本（不依赖 gradle wrapper）
├── settings.gradle
├── build.gradle                       # AGP 8.1.4 + Xposed 仓库
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   # Gradle 8.2
└── app/
    ├── build.gradle                   # compileSdk 34 / minSdk 24 / Java 17
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml        # xposedmodule / xposedscope / provider
        ├── assets/xposed_init         # com.bili.splashkiller.ModuleEntry
        ├── res/values/strings.xml
        ├── res/values/arrays.xml      # 作用域：tv.danmaku.bili
        └── java/com/bili/splashkiller/
            ├── ModuleEntry.java       # IXposedHookLoadPackage 入口
            ├── Hooks.java             # 全部 hook 点（命中时才看开关）
            ├── HookItem.java          # 13 个开关的注册表（标题/说明/签名/风险/默认值）
            ├── Config.java            # 在 B 站进程里读开关（provider → XSharedPreferences → 默认值）
            ├── ConfigProvider.java    # 向 B 站进程暴露开关状态
            └── ui/MainActivity.java   # 开关界面（纯代码构建，不依赖 androidx）
```

---

## 2. 安装与启用

1. 安装模块 APK。
2. LSPosed 管理器 → **模块** → 启用「B站开屏广告开关」。
3. **作用域**里勾选「哔哩哔哩」（`tv.danmaku.bili`）→ 保存。
4. 打开模块 App 调开关 → 到 LSPosed 里**强制停止**哔哩哔哩 → 冷启动验证。

> 开关配置在每个 B 站进程里只读一次并缓存，**改完开关必须让 B 站进程重启**才生效。

---

## 3. 开关对照表

默认值 = 首次安装时的推荐组合：只开 ① ② 两组，③ 兜底全关。

### ① 开屏显示拦截（默认开）

| 开关 | 默认 | 处理了什么 | Hook 位置 |
|---|---|---|---|
| 广告开屏（图片 / 视频 / 半屏 / 竖屏故事） | ✅ | 冷启动不再创建广告开屏 Fragment，直接进主界面。只影响显示，后台预取仍在。 | `MainSplashHelper#f(Splash,boolean,boolean)` → `null` |
| 活动开屏（节日 / 活动专题页） | ✅ | B 站做活动时替换整屏的活动页（Fragment tag `UserSplashFragment`），与广告开屏是两条独立通道。 | `ui.splash.event.k#c()` → `null` |
| 品牌开屏（品牌定制广告） | ✅ | 品牌方定制开屏（Fragment tag `BrandSplashFragment`）。 | `brand.BrandSplashHelper#f(Context)` → `null` |

### ② 启动预取 / 请求（默认开）

| 开关 | 默认 | 处理了什么 | Hook 位置 |
|---|---|---|---|
| 启动时不预取广告开屏数据 | ✅ | 启动任务不再调度开屏预取：不请求 `/x/v2/splash/list`，不写 `splash2/splash.json`。旧缓存仍可能被读，拦显示靠 ①。 | `ad.c0#W()` → 跳过原方法 |
| 启动时不拉取活动开屏列表 | ✅ | 不再请求 `/x/v2/splash/event/list2`。 | `event.k#h()` → 跳过原方法 |
| 启动时不拉取品牌开屏配置 | ✅ | 不再请求 `/x/v2/splash/brand/config`、`/x/v2/splash/brand/list`。 | `BrandSplashHelper#s(Context)` → 跳过原方法 |
| 不加载"实时开屏" | ✅ | 冷启动时读取本次启动刚下发的实时开屏（热门开屏），命中就直接展示；关掉后恒为空。 | `ad.c0#w()` + `#v()` → `null` |

### ③ 兜底开关（可能影响其它功能，默认关）

| 开关 | 默认 | 处理了什么 | Hook 位置 |
|---|---|---|---|
| 不读取本地开屏缓存（显示侧总入口置空） | ⬜ | `ad.c0.z(SplashSource)` 是显示侧读开屏的入口，冷启动 / 热启动 / deeplink 三种来源都走它。副作用：`bilibili://main/hot-splash` 的热门开屏也会失效。 | `ad.c0#z(SplashSource)` → `null` |
| 数据层置空：强制刷新与展示策略读不到数据 | ⬜ | 影响"强制更新素材"与"展示策略（strategyList）"两条读路径，置空后素材视为缺失、策略列表为空。它不负责启动时的异步列表请求，拦预取请用 ② 组。 | `ad.service.c#a(String,String,String)` + `#b()` → `null` |
| Fragment 工厂兜底（任何类型都建不出来） | ⬜ | 所有类型的开屏 Fragment 都经此创建，图片 / 视频 / 半屏 / 竖屏 / 故事以及以后新增的类型一起挡。总闸级方案。 | `ad.page.x#a(Splash)` → `null` |
| 事件 / 品牌开屏硬拦截 | ⬜ | `M7(ZZ)` 决定事件与品牌两类开屏，返回 false 一次拦死。① 的第 2、3 项失效时用这条。 | `MainActivityV2#M7(boolean,boolean)` → `false` |
| 复用官方"不展示开屏"分支 | ⬜ | `c8()` 是 B 站自己的判定（青少年模式 / 课程模式命中时为 true），为 true 时走 `DISABLE_SPLASH` 分支。等于用官方逻辑关开屏；副作用是会按"不展示"上报埋点，且只覆盖广告侧，不覆盖事件 / 品牌。 | `MainActivityV2#c8()` → `true` |

### ④ 调试（默认关）

| 开关 | 默认 | 处理了什么 | Hook 位置 |
|---|---|---|---|
| 详细日志 | ⬜ | 每个 hook 首次命中时往 LSPosed 日志打一行 `[BSK]`，用来确认开关是否生效。不改行为。 | （不挂钩） |

**典型用法**

- 只想干净进主界面、不留后台上报：保持默认（① + ②）。
- 只想省流量、不介意偶尔缓存命中：只开 ② 的四个。
- ① 失效（B 站结构变了）时：开「Fragment 工厂兜底」。
- 想让服务端也拿不到数据：加开「数据层置空」。

---

## 4. 工作原理

**为什么 hook 全部无条件安装？**
`handleLoadPackage` 时机太早，Application 还没 attach，拿不到稳定 Context 去读配置。
所以所有 hook 点先装上，命中时再问一次开关（`Config.on(id)`），关着就走原逻辑；
读不到配置也不会崩，回落到各开关的默认值。

**为什么用 ContentProvider 传配置？**
Android 10+ 上 `XSharedPreferences` 基本读不到模块的 prefs（不再有世界可读文件）。
模块 App 里的 `ConfigProvider`（`content://com.bili.splashkiller.config`）通过
`ContentResolver.call(uri, "get", ...)` 把 13 个布尔值以 Bundle 交给 B 站进程，这是主通道；
XSharedPreferences 作为第二兜底，再读不到就用内置默认值。

**返回值策略**
能返回 `null` 的 hook 统一返回 `null`（让调用方走"没有开屏"分支），
不能返回 `null` 的跳过原方法。模块假定调用方对空值有处理；
若某个版本因此出现异常，可在 App 里逐项关掉对应开关定位。

---

## 5. 构建

**环境要求**：JDK 17、Android SDK（`platforms;android-34` + `build-tools;34.0.0`）、Gradle 8.2。

### 方式 A：命令行

仓库里没有 `gradle-wrapper.jar` / `gradlew`，用已安装的 Gradle 8.2 直接构建：

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_SDK_ROOT=/path/to/android-sdk
export GRADLE_BIN=/path/to/gradle-8.2/bin/gradle
./scripts/build.sh          # 产物在 app/build/outputs/apk/debug/
```

脚本会用 `$ANDROID_SDK_ROOT` 生成 `local.properties`，并把 Gradle / Android 的用户目录
指向仓库内的 `.build/`（该目录已被忽略），避免依赖 `$HOME` 可写。

### 方式 B：Android Studio

1. **Open** → 选择 `BiliSplashKiller` 目录（不是 `app`）。
2. 等待 Gradle Sync（首次会下载 AGP 8.1.4 + Gradle 8.2 + Xposed API 82）。
   若 `https://api.xposed.info/` 拉不到 `de.robv.android.xposed:api:82`：
   下载 `api-82.jar` 放进 `app/libs/`，把 `app/build.gradle` 里的
   `compileOnly 'de.robv.android.xposed:api:82'` 换成注释里那行 fileTree 写法。
3. **Build → Build Bundle(s)/APK(s) → Build APK(s)**。

需要 `./gradlew` 时，先执行一次 `gradle wrapper --gradle-version 8.2` 生成 wrapper 文件即可。

### 关于 debug / release

`debug` / `release` 只是 Gradle 的 buildType 名，hook 行为完全相同。
两者都**故意不开混淆**（`minifyEnabled false`），避免 hook 目标的名字被改写。
区别只有 manifest 里的 `android:debuggable`：debug 变体带这个标记，release 不带。
签名用你自己的 keystore：

```bash
gradle assembleRelease
zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk aligned.apk
apksigner sign --ks my-release.keystore --out app-release.apk aligned.apk
```

---

## 6. 排障

| 现象 | 原因 / 处理 |
|---|---|
| 开屏还在 | ① 没生效。看 LSPosed 日志有没有 `[BSK]` 行；若提示"未找到开屏核心类"，说明该版本结构变了，需要按 dex 重新定位后改 `Hooks.java`。 |
| 日志出现"读不到模块配置" | 作用域没勾 B 站，或模块 App 从未启动过（provider 需要 App 已被系统识别，先手动打开一次模块 App）。 |
| 改了开关没反应 | 没重启 B 站进程。LSPosed → 哔哩哔哩 → 强行停止。 |
| 热启动 / deeplink 也想要开屏 | 关掉「不读取本地开屏缓存」。 |
| B 站启动变慢或异常 | 全关开关后逐个开，定位是哪一项；优先怀疑 ③ 组。 |

---

## 7. 兼容性

- 只针对 **7.61.0** 编写。B 站内部类名 / 方法签名随版本变化，升级后大概率失效。
- 失效时不需要动本模块的逻辑，改 `HookItem.java` 与 `Hooks.java` 里的目标字符串即可。
- 各开关的默认值按"最小侵入"设计：默认只开显示拦截与预取拦截，兜底项一律关闭。

---

## 8. 免责

仅供个人学习研究，用于自己设备上的广告过滤。请勿分发修改后的 B 站客户端。

---

## 9. 许可

Copyright (C) 2026 **Yaklo**

本工程以 [GNU General Public License v3.0](LICENSE)（SPDX：`GPL-3.0-only`）发布：
可以自由使用、修改、再分发；**若分发修改版，需同样以 GPL-3.0 开放源码并保留版权声明**。
个人自用、自己改着玩不受任何影响。

第三方依赖：`de.robv.android.xposed:api:82` 为 `compileOnly`，仅编译期使用、**不随 APK 分发**
（其许可为 Apache-2.0）。本模块与哔哩哔哩官方无任何关联，不包含其任何代码或资源。
