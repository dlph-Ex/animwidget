# v1.10.356：R8 混淆规则。**只改名，不裁剪、不优化** —— 把风险压到最低。
#
# 为什么 -dontshrink：裁剪会把「看起来没人调用、实际由系统/反射调用」的代码删掉。
# 用户只有一台手机，删错了只能在真机上炸，测不出来。
# 混淆的目标只是「让 jadx 反编译出来的代码读不懂」，不需要它变小。

-dontshrink
-dontoptimize
-dontpreverify
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses


# ---------------------------------------------------------------- 一、被系统按名字找的类
# **这份清单是从 patch-v1.4.0/AndroidManifest.xml 用 android:name 自动提取的，不要手写。**
# v1.10.356 第一版漏了 DiagCatalogActivity，verify-apk 的 [1] 立刻报「缺失=1」——
# R8 命令行（不经 AGP）**不会读 Manifest 自动保留组件**，漏一个就是一个 ClassNotFound。
-keep class io.github.animwidget.AnimatedPickerActivity { *; }
-keep class io.github.animwidget.ConfigActivity { *; }
-keep class io.github.animwidget.CreatorIntroActivity { *; }
-keep class io.github.animwidget.DeveloperActivity { *; }
-keep class io.github.animwidget.DiagCatalogActivity { *; }
-keep class io.github.animwidget.ExportHistoryActivity { *; }
-keep class io.github.animwidget.FrameProvider { *; }
-keep class io.github.animwidget.LogActivity { *; }
-keep class io.github.animwidget.MainActivity { *; }
-keep class io.github.animwidget.OverlayService { *; }
-keep class io.github.animwidget.PinWidgetActivity { *; }
-keep class io.github.animwidget.PlaybackService { *; }
-keep class io.github.animwidget.SettingsActivity { *; }
-keep class io.github.animwidget.WidgetProvider { *; }
-keep class rikka.shizuku.ShizukuProvider { *; }

# ---------------------------------------------------------------- 二、布局 XML 里写的自定义 View
# 这是 R8 混淆最经典的崩溃点：layout/*.xml 里写的是全限定类名，
# 类被改名后 LayoutInflater 会抛 ClassNotFoundException —— **一进界面就崩**。
# 实测本仓库有 9 个（AuroraView / GlyphView / IconTileView / LanguageRowView /
# LayoutPickerView / MediaSelectorView / ShadowCurveView / SpinRingView / TuneMarkView）。
# 这里用通配规则把「所有自定义 View」都保住类名与三个构造，避免漏一个。
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ---------------------------------------------------------------- 三、枚举常量名（最危险的一条）
# WidgetStore 用 `getShape().name()` 存盘、`ShapeKind.valueOf(str)` 读回。
# 枚举常量被改名后：设置里存的是 "a"，升级后读旧值 "CIRCLE" → valueOf 抛异常 →
# **用户升级一次，所有组件配置全丢**。所以枚举的常量名一律不许动。
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    *;
}

# ---------------------------------------------------------------- 四、反射点
# ThemePatch 用 getDeclaredField("appliedDark") 改主题状态。
-keepclassmembers class io.github.animwidget.SettingsActivity {
    boolean appliedDark;
}

# ---------------------------------------------------------------- 五、第三方（Shizuku）
# 通过 AIDL + 动态代理交互，包名类名都不能动。注意是 rikka.shizuku，不是 moe.shizuku
# （moe.shizuku 只是权限名里的字符串，不是类）。
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn kotlin.**
-dontwarn sun.**
-dontwarn jdk.**
-dontwarn org.jetbrains.annotations.**
-dontwarn android.**

# ---------------------------------------------------------------- 六、Kotlin 运行时
# kotlin-stdlib 是作为 input 打进 dex 的（不打包就一启动崩）。但它内部有反射与注解处理，
# 混淆收益极低、风险很高 —— 保持原名。
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ---------------------------------------------------------------- 七、Shizuku SDK 的编译期依赖
# shizuku-all.jar 里 moe.shizuku.api.BinderContainer 引用了 androidx.annotation.RestrictTo，
# 但本项目零第三方依赖、不打包 androidx —— 这些注解在运行期不需要（CLASS 保留级别）。
-dontwarn moe.shizuku.**
-dontwarn androidx.**
