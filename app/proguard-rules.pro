# Xposed 入口类不能被混淆
-keep class com.bili.splashkiller.ModuleEntry { *; }
-keep class com.bili.splashkiller.ConfigProvider { *; }
-keep class com.bili.splashkiller.Hooks { *; }
-keep class com.bili.splashkiller.HookItem { *; }
