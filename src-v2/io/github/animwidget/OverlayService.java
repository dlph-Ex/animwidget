package io.github.animwidget;
import android.graphics.BitmapFactory.Options;
import kotlin.jvm.internal.Ref.IntRef;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

/* compiled from: OverlayService.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u009f\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010#\n��\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000b\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u000e*\u0001\u0010\u0018�� M2\u00020\u0001:\u0003MNOB\u0005¢\u0006\u0002\u0010\u0002J\u0010\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\"H\u0002J\b\u0010#\u001a\u00020\u001eH\u0002J \u0010$\u001a\u00020 2\u0006\u0010%\u001a\u00020&2\u0006\u0010'\u001a\u00020\b2\u0006\u0010(\u001a\u00020\bH\u0002J\u0010\u0010)\u001a\u00020 2\u0006\u0010*\u001a\u00020+H\u0014J\u0018\u0010,\u001a\u00020 2\u0006\u0010-\u001a\u00020.2\u0006\u0010/\u001a\u000200H\u0002J \u00101\u001a\u0002022\u0006\u0010!\u001a\u00020\"2\u0006\u00103\u001a\u00020\b2\u0006\u00104\u001a\u00020\bH\u0002J \u00105\u001a\u00020\u00042\u0006\u0010!\u001a\u00020\"2\u0006\u0010/\u001a\u0002002\u0006\u0010-\u001a\u00020.H\u0002J\b\u00106\u001a\u000207H\u0002J*\u00108\u001a\u0002022\u0006\u0010!\u001a\u00020\"2\b\u00109\u001a\u0004\u0018\u00010:2\u0006\u00103\u001a\u00020\b2\u0006\u00104\u001a\u00020\bH\u0002J\b\u0010;\u001a\u00020\u001eH\u0002J\b\u0010<\u001a\u00020 H\u0002J\b\u0010=\u001a\u00020 H\u0002J\u0014\u0010>\u001a\u0004\u0018\u00010?2\b\u0010@\u001a\u0004\u0018\u00010AH\u0016J\b\u0010B\u001a\u00020 H\u0016J\b\u0010C\u001a\u00020 H\u0016J\"\u0010D\u001a\u00020\b2\b\u0010@\u001a\u0004\u0018\u00010A2\u0006\u0010E\u001a\u00020\b2\u0006\u0010F\u001a\u00020\bH\u0016J\b\u0010G\u001a\u00020 H\u0002J\b\u0010H\u001a\u00020 H\u0002J\b\u0010I\u001a\u00020 H\u0002J\u0010\u0010J\u001a\u00020 2\u0006\u0010K\u001a\u00020\bH\u0002J\u0018\u0010L\u001a\u00020 2\u0006\u0010!\u001a\u00020\"2\u0006\u0010/\u001a\u000200H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\bX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\f\u001a\u00020\u0006X\u0082.¢\u0006\u0002\n��R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00040\u000eX\u0082\u0004¢\u0006\u0002\n��R\u0010\u0010\u000f\u001a\u00020\u0010X\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u0011R#\u0010\u0012\u001a\n \u0014*\u0004\u0018\u00010\u00130\u00138BX\u0082\u0084\u0002¢\u0006\f\n\u0004\b\u0017\u0010\u0018\u001a\u0004\b\u0015\u0010\u0016R*\u0010\u0019\u001a\u001e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u001b0\u001aj\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u001b`\u001cX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/OverlayService;", "Landroid/app/Service;", "()V", "anchorView", "Landroid/view/View;", "bgHandler", "Landroid/os/Handler;", "editingId", "", "holderThread", "Landroid/os/HandlerThread;", "lastGeneration", "mainHandler", "trackedViews", "", "watchdog", "io/github/animwidget/Runnable", "Lio/github/animwidget/Runnable;", "windowManager", "Landroid/view/WindowManager;", "kotlin.jvm.PlatformType", "getWindowManager", "()Landroid/view/WindowManager;", "windowManager$delegate", "Lkotlin/Lazy;", "windows", "Ljava/util/HashMap;", "Lio/github/animwidget/OverlayService$Holder;", "Lkotlin/collections/HashMap;", "windowsVisible", "", "addWindow", "", "config", "Lio/github/animwidget/WidgetConfig;", "anchorEnabled", "applyVideoTransform", "texture", "Landroid/view/TextureView;", "videoWidth", "videoHeight", "attachBaseContext", "newBase", "Landroid/content/Context;", "attachDragAndScale", "root", "Landroid/widget/FrameLayout;", "params", "Landroid/view/WindowManager$LayoutParams;", "buildContent", "Lio/github/animwidget/OverlayService$Content;", "widthPx", "heightPx", "buildEditFrame", "buildNotification", "Landroid/app/Notification;", "buildVideoView", "uri", "Landroid/net/Uri;", "canDrawOverlays", "ensureAnchor", "ensureChannel", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "flags", "startId", "reload", "removeAllWindows", "removeAnchor", "removeWindow", "widgetId", "saveEditedPosition", "Companion", "Content", "Holder"})
@SourceDebugExtension({"SMAP\nOverlayService.kt\nKotlin\n*S Kotlin\n*F\n+ 1 OverlayService.kt\nio/github/animwidget/OverlayService\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,658:1\n1855#2,2:659\n1855#2,2:661\n766#2:663\n857#2,2:664\n1045#2:666\n288#2,2:667\n288#2,2:669\n1#3:671\n*S KotlinDebug\n*F\n+ 1 OverlayService.kt\nio/github/animwidget/OverlayService\n*L\n149#1:659,2\n238#1:661,2\n256#1:663\n256#1:664,2\n257#1:666\n261#1:667,2\n262#1:669,2\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/OverlayService.class */
public final class OverlayService extends Service {

    @NotNull
    public static final Companion Companion = new Companion(null);
    Handler bgHandler;
    Handler mainHandler;

    @Nullable
    View anchorView;
    boolean windowsVisible;

    @NotNull
    private static final String TAG = "OverlayService";

    @NotNull
    private static final String ACTION_REFRESH = "io.github.animwidget.overlay.REFRESH";

    @NotNull
    private static final String ACTION_EDIT = "io.github.animwidget.overlay.EDIT";

    @NotNull
    private static final String ACTION_STOP = "io.github.animwidget.overlay.STOP";

    @NotNull
    private static final String CHANNEL_ID = "overlay";
    private static final int NOTIFICATION_ID = 2001;

    @NotNull
    private static final String EXTRA_ID = "widget_id";

    @NotNull
    private final HandlerThread holderThread = new HandlerThread("overlay-loop", -4);

    @NotNull
    private final HashMap<Integer, Holder> windows = new HashMap<>();

    @NotNull
    private final Set<View> trackedViews = new LinkedHashSet();
    private int editingId = -1;
    private int lastGeneration = -1;

    @NotNull
    private final Lazy windowManager$delegate = LazyKt.lazy(new OverlayService$windowManager$2(this));

    @NotNull
    private final Runnable watchdog = new Runnable() { // from class: io.github.animwidget.Runnable
        @Override // java.lang.Runnable
        public void run() throws NoWhenBranchMatchedException {
            // v1.10.67：PlaybackService 在跑的时候它自己会 tick 桌面状态机（零掉帧模式下还在后台线程）。
            // 这里每 300ms 再查一次 UsageStats 是纯重复：一次查询 5~20ms，和 30ms 的推帧节拍抢
            // system_server 的 binder —— 用户看到的偶发丢帧有一部分就是这么来的。
            if (!PlaybackService.Companion.isRunning()) {
                DeskWatcher.INSTANCE.tick(OverlayService.this);
            }
            if (OverlayService.this.anchorEnabled()) {
                OverlayService.this.ensureAnchor();
            } else {
                OverlayService.this.removeAnchor();
            }
            boolean shouldShow = OverlayService.this.editingId >= 0 || DeskWatcher.INSTANCE.getCanPlay() || !DeskWatcher.INSTANCE.hasAccess(OverlayService.this);
            Diag.warnThrottled(OverlayService.this, "O11", "shouldShow=" + shouldShow
                    + " editing=" + OverlayService.this.editingId
                    + " canPlay=" + DeskWatcher.INSTANCE.getCanPlay()
                    + " windows=" + OverlayService.this.windows.size()
                    + " 组件数=" + WidgetStore.INSTANCE.all(OverlayService.this).size()
                    // v1.10.275：**把"为什么判成这样"打出来**。用户报"在桌面停了十几秒画面没动"，
                    // 有这三项才能分清是「前台包名读错」「桌面包名不对」还是「窗口里没事件」：
                    //   前台=? 且 未知>0ms  → UsageStats 这段时间没吐事件（正常，安静待着就是没事件）
                    //   前台=某个别的包      → 真的在别的应用里
                    //   前台=桌面包名        → 判断正确，问题不在这里
                    + " 前台=" + String.valueOf(DeskWatcher.INSTANCE.foreground())
                    + " 桌面=" + String.valueOf(DeskWatcher.INSTANCE.launcher(OverlayService.this))
                    + " 未知=" + DeskWatcher.unknownFor() + "ms");
            if (shouldShow && !OverlayService.this.windowsVisible) {
                OverlayService.this.windowsVisible = true;
                OverlayService.this.reload();
            } else if (shouldShow || !OverlayService.this.windowsVisible) {
                if (OverlayService.this.windowsVisible && WidgetStore.INSTANCE.generation(OverlayService.this) != OverlayService.this.lastGeneration && OverlayService.this.editingId < 0) {
                    OverlayService.this.reload();
                }
            } else {
                OverlayService.this.windowsVisible = false;
                OverlayService.this.removeAllWindows();
            }
            Handler handler = OverlayService.this.mainHandler;
            if (handler == null) {
                Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
                handler = null;
            }
            handler.postDelayed(this, 700L);
        }
    };

    /* compiled from: OverlayService.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/OverlayService$WhenMappings.class */
    public static /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[MediaKind.values().length];
            try {
                iArr[MediaKind.VIDEO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[MediaKind.GIF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[MediaKind.IMAGE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    /* compiled from: OverlayService.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��*\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\nJ\u000e\u0010\u0011\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0012\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\b\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\t\u001a\u00020\nX\u0082T¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/OverlayService$Companion;", "", "()V", "ACTION_EDIT", "", "ACTION_REFRESH", "ACTION_STOP", "CHANNEL_ID", "EXTRA_ID", "NOTIFICATION_ID", "", "TAG", "edit", "", "context", "Landroid/content/Context;", "widgetId", "refresh", "stop"})
    /* loaded from: classes.jar:io/github/animwidget/OverlayService$Companion.class */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        private Companion() {
        }

        public final void refresh(@NotNull Context context) {
            // v1.10.13：覆盖层为什么起不来，必须是可观测的
            try {
                boolean allowed = android.provider.Settings.canDrawOverlays(context);
                int lastId = context.getSharedPreferences("settings", 0).getInt("last_overlay_id", -1);
                Diag.warn(context, "O09", "refresh: 悬浮窗权限=" + allowed + " last_overlay_id=" + lastId);
                if (!allowed) {
                    return;
                }
            } catch (Throwable ignored) {
            }
            Intrinsics.checkNotNullParameter(context, "context");
            if (context.getSharedPreferences("settings", 0).getBoolean("all_stopped", false)) {
                return;
            }
            try {
                Intent intent = new Intent(context, (Class<?>) OverlayService.class).setAction(OverlayService.ACTION_REFRESH);
                Intrinsics.checkNotNullExpressionValue(intent, "setAction(...)");
                context.startForegroundService(intent);
            } catch (Throwable th) {
            }
        }

        public final void edit(@NotNull Context context, int widgetId) {
            Intrinsics.checkNotNullParameter(context, "context");
            try {
                Intent intent = new Intent(context, (Class<?>) OverlayService.class).setAction(OverlayService.ACTION_EDIT).putExtra("widget_id", widgetId);
                Intrinsics.checkNotNullExpressionValue(intent, "putExtra(...)");
                context.startForegroundService(intent);
            } catch (Throwable th) {
            }
        }

        public final void stop(@NotNull Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            try {
                context.stopService(new Intent(context, (Class<?>) OverlayService.class));
            } catch (Throwable th) {
            }
        }
    }

    /* compiled from: OverlayService.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��$\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0002\u0018��2\u00020\u0001B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\t¢\u0006\u0002\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n��\u001a\u0004\b\r\u0010\u000eR\u001c\u0010\b\u001a\u0004\u0018\u00010\tX\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n��\u001a\u0004\b\u0013\u0010\u0014"}, d2 = {"Lio/github/animwidget/OverlayService$Holder;", "", "config", "Lio/github/animwidget/WidgetConfig;", "params", "Landroid/view/WindowManager$LayoutParams;", "root", "Landroid/widget/FrameLayout;", "player", "Landroid/media/MediaPlayer;", "(Lio/github/animwidget/WidgetConfig;Landroid/view/WindowManager$LayoutParams;Landroid/widget/FrameLayout;Landroid/media/MediaPlayer;)V", "getConfig", "()Lio/github/animwidget/WidgetConfig;", "getParams", "()Landroid/view/WindowManager$LayoutParams;", "getPlayer", "()Landroid/media/MediaPlayer;", "setPlayer", "(Landroid/media/MediaPlayer;)V", "getRoot", "()Landroid/widget/FrameLayout;"})
    /* loaded from: classes.jar:io/github/animwidget/OverlayService$Holder.class */
    private static final class Holder {

        @NotNull
        private final WidgetConfig config;

        @NotNull
        private final WindowManager.LayoutParams params;

        @NotNull
        private final FrameLayout root;

        @Nullable
        private MediaPlayer player;

        public Holder(@NotNull WidgetConfig config, @NotNull WindowManager.LayoutParams params, @NotNull FrameLayout root, @Nullable MediaPlayer player) {
            Intrinsics.checkNotNullParameter(config, "config");
            Intrinsics.checkNotNullParameter(params, "params");
            Intrinsics.checkNotNullParameter(root, "root");
            this.config = config;
            this.params = params;
            this.root = root;
            this.player = player;
        }

        public /* synthetic */ Holder(WidgetConfig widgetConfig, WindowManager.LayoutParams layoutParams, FrameLayout frameLayout, MediaPlayer mediaPlayer, int i, DefaultConstructorMarker defaultConstructorMarker) {
            this(widgetConfig, layoutParams, frameLayout, (i & 8) != 0 ? null : mediaPlayer);
        }

        @NotNull
        public final WidgetConfig getConfig() {
            return this.config;
        }

        @NotNull
        public final WindowManager.LayoutParams getParams() {
            return this.params;
        }

        @NotNull
        public final FrameLayout getRoot() {
            return this.root;
        }

        @Nullable
        public final MediaPlayer getPlayer() {
            return this.player;
        }

        public final void setPlayer(@Nullable MediaPlayer mediaPlayer) {
            this.player = mediaPlayer;
        }
    }

    /* compiled from: OverlayService.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u0018\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0002\u0018��2\u00020\u0001B\u0019\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005¢\u0006\u0002\u0010\u0006R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005¢\u0006\b\n��\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\t\u0010\n"}, d2 = {"Lio/github/animwidget/OverlayService$Content;", "", "view", "Landroid/view/View;", "player", "Landroid/media/MediaPlayer;", "(Landroid/view/View;Landroid/media/MediaPlayer;)V", "getPlayer", "()Landroid/media/MediaPlayer;", "getView", "()Landroid/view/View;"})
    /* loaded from: classes.jar:io/github/animwidget/OverlayService$Content.class */
    private static final class Content {

        @NotNull
        private final View view;

        @Nullable
        private final MediaPlayer player;

        public Content(@NotNull View view, @Nullable MediaPlayer player) {
            Intrinsics.checkNotNullParameter(view, "view");
            this.view = view;
            this.player = player;
        }

        public /* synthetic */ Content(View view, MediaPlayer mediaPlayer, int i, DefaultConstructorMarker defaultConstructorMarker) {
            this(view, (i & 2) != 0 ? null : mediaPlayer);
        }

        @NotNull
        public final View getView() {
            return this.view;
        }

        @Nullable
        public final MediaPlayer getPlayer() {
            return this.player;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final WindowManager getWindowManager() {
        return (WindowManager) this.windowManager$delegate.getValue();
    }

    @Override // android.app.Service, android.content.ContextWrapper
    protected void attachBaseContext(@NotNull Context newBase) {
        Intrinsics.checkNotNullParameter(newBase, "newBase");
        super.attachBaseContext(LocaleHelper.INSTANCE.wrap(newBase));
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        // v1.8.11：悬浮窗服务同样可能在纯净进程启动，补装崩溃钩子（幂等）。
        CrashLog.install(this);
        this.holderThread.start();
        this.bgHandler = new Handler(this.holderThread.getLooper());
        this.mainHandler = new Handler(getMainLooper());
        ensureChannel();
        Diag.warn(this, "O10", "覆盖层服务启动");
        Handler handler = this.mainHandler;
        if (handler == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
            handler = null;
        }
        handler.postDelayed(this.watchdog, 300L);
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    @Override // android.app.Service
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        if (!canDrawOverlays()) {
            stopSelf();
            return 2;
        }
        String action = intent != null ? intent.getAction() : null;
        if (action != null) {
            switch (action.hashCode()) {
                case -1357889698:
                    if (action.equals(ACTION_EDIT)) {
                        int id = intent.getIntExtra("widget_id", -1);
                        WidgetConfig config = WidgetStore.INSTANCE.load(this, id);
                        if (config != null && config.getOverlayEnabled()) {
                            this.editingId = id;
                            Handler handler = this.mainHandler;
                            if (handler == null) {
                                Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
                                handler = null;
                            }
                            handler.post(() -> {
                                onStartCommand$lambda$0(OverlayService.this);
                            });
                            return 2;
                        }
                        return 2;
                    }
                    break;
                case -1357457066:
                    if (action.equals(ACTION_STOP)) {
                        getSharedPreferences("settings", 0).edit().putBoolean("all_stopped", true).apply();
                        stopSelf();
                        return 2;
                    }
                    break;
            }
        }
        Handler handler2 = this.mainHandler;
        if (handler2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
            handler2 = null;
        }
        handler2.post(() -> {
            onStartCommand$lambda$1(OverlayService.this);
        });
        return 2;
    }

    private static final void onStartCommand$lambda$0(OverlayService this$0) throws NoWhenBranchMatchedException {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.reload();
    }

    private static final void onStartCommand$lambda$1(OverlayService this$0) throws NoWhenBranchMatchedException {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.reload();
    }

    @Override // android.app.Service
    public void onDestroy() {
        Handler handler = this.mainHandler;
        if (handler == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
            handler = null;
        }
        handler.removeCallbacksAndMessages(null);
        Handler handler2 = this.bgHandler;
        if (handler2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("bgHandler");
            handler2 = null;
        }
        handler2.removeCallbacksAndMessages(null);
        Set<Integer> setKeySet = this.windows.keySet();
        Intrinsics.checkNotNullExpressionValue(setKeySet, "<get-keys>(...)");
        Iterable $this$forEach$iv = CollectionsKt.toList(setKeySet);
        for (Object element$iv : $this$forEach$iv) {
            Integer it = (Integer) element$iv;
            Intrinsics.checkNotNull(it);
            removeWindow(it.intValue());
        }
        for (View view : CollectionsKt.toList(this.trackedViews)) {
            try {
                getWindowManager().removeView(view);
            } catch (Throwable th) {
            }
        }
        this.trackedViews.clear();
        removeAnchor();
        this.holderThread.quitSafely();
        super.onDestroy();
    }

    @Override // android.app.Service
    @Nullable
    public IBinder onBind(@Nullable Intent intent) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void ensureAnchor() {
        if (this.anchorView != null) {
            return;
        }
        View view = new View(this);
        WindowManager.LayoutParams $this$ensureAnchor_u24lambda_u243 = new WindowManager.LayoutParams(1, 1, 2038, 568, -3);
        $this$ensureAnchor_u24lambda_u243.gravity = 8388659;
        $this$ensureAnchor_u24lambda_u243.x = -20;
        $this$ensureAnchor_u24lambda_u243.y = -20;
        $this$ensureAnchor_u24lambda_u243.alpha = 0.0f;
        try {
            getWindowManager().addView(view, $this$ensureAnchor_u24lambda_u243);
            this.anchorView = view;
        } catch (Throwable t) {
            Log.w(TAG, "anchor addView failed", t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void removeAnchor() {
        View view = this.anchorView;
        if (view == null) {
            return;
        }
        try {
            getWindowManager().removeView(view);
        } catch (Throwable th) {
        }
        this.anchorView = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void removeAllWindows() {
        Set<Integer> setKeySet = this.windows.keySet();
        Intrinsics.checkNotNullExpressionValue(setKeySet, "<get-keys>(...)");
        for (Integer id : CollectionsKt.toList(setKeySet)) {
            Intrinsics.checkNotNull(id);
            removeWindow(id.intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void reload() throws NoWhenBranchMatchedException {
        WidgetConfig widgetConfig;
        Object obj;
        Object obj2;
        if (this.windowsVisible || this.editingId >= 0) {
            Iterable iterableValues = this.windows.values();
            Intrinsics.checkNotNullExpressionValue(iterableValues, "<get-values>(...)");
            Iterable<Holder> $this$forEach$iv = iterableValues;
            for (Holder it : $this$forEach$iv) {
                try {
                    MediaPlayer player = it.getPlayer();
                    if (player != null) {
                        player.stop();
                    }
                    MediaPlayer player2 = it.getPlayer();
                    if (player2 != null) {
                        player2.release();
                    }
                } catch (Throwable th) {
                }
            }
            this.windows.clear();
            for (View view : CollectionsKt.toList(this.trackedViews)) {
                try {
                    getWindowManager().removeView(view);
                } catch (Throwable th2) {
                }
            }
            this.trackedViews.clear();
            Iterable $this$filter$iv = WidgetStore.INSTANCE.all(this);
            Collection destination$iv$iv = new ArrayList();
            for (Object element$iv$iv : $this$filter$iv) {
                WidgetConfig it2 = (WidgetConfig) element$iv$iv;
                if (it2.getOverlayEnabled()) {
                    destination$iv$iv.add(element$iv$iv);
                }
            }
            Iterable $this$sortedBy$iv = (List) destination$iv$iv;
            List enabled = CollectionsKt.sortedWith($this$sortedBy$iv, new Comparator() { // from class: io.github.animwidget.OverlayService$reload$$inlined$sortedBy$1
                /* JADX WARN: Multi-variable type inference failed */
                @Override // java.util.Comparator
                public final int compare(Object t, Object t2) {
                    WidgetConfig it3 = (WidgetConfig) t;
                    WidgetConfig it4 = (WidgetConfig) t2;
                    return ComparisonsKt.compareValues(Integer.valueOf(it3.getWidgetId()), Integer.valueOf(it4.getWidgetId()));
                }
            });
            int preferredId = getSharedPreferences("settings", 0).getInt("last_overlay_id", -1);
            if (this.editingId >= 0) {
                List $this$firstOrNull$iv = enabled;
                Iterator it3 = $this$firstOrNull$iv.iterator();
                while (true) {
                    if (it3.hasNext()) {
                        Object element$iv = it3.next();
                        WidgetConfig it4 = (WidgetConfig) element$iv;
                        if (it4.getWidgetId() == this.editingId) {
                            obj2 = element$iv;
                            break;
                        }
                    } else {
                        obj2 = null;
                        break;
                    }
                }
                widgetConfig = (WidgetConfig) obj2;
            } else if (preferredId >= 0) {
                List $this$firstOrNull$iv2 = enabled;
                Iterator it5 = $this$firstOrNull$iv2.iterator();
                while (true) {
                    if (it5.hasNext()) {
                        Object element$iv2 = it5.next();
                        WidgetConfig it6 = (WidgetConfig) element$iv2;
                        if (it6.getWidgetId() == preferredId) {
                            obj = element$iv2;
                            break;
                        }
                    } else {
                        obj = null;
                        break;
                    }
                }
                widgetConfig = (WidgetConfig) obj;
                if (widgetConfig == null) {
                    widgetConfig = (WidgetConfig) CollectionsKt.firstOrNull(enabled);
                }
            } else {
                widgetConfig = (WidgetConfig) CollectionsKt.firstOrNull(enabled);
            }
            WidgetConfig chosen = widgetConfig;
            if (chosen != null) {
                addWindow(chosen);
            }
            this.lastGeneration = WidgetStore.INSTANCE.generation(this);
            if (this.windows.isEmpty() && this.editingId < 0 && !anchorEnabled()) {
                stopSelf();
            }
        }
    }

    private final void removeWindow(int widgetId) {
        Holder holder = this.windows.remove(Integer.valueOf(widgetId));
        if (holder == null) {
            return;
        }
        try {
            MediaPlayer player = holder.getPlayer();
            if (player != null) {
                player.stop();
            }
            MediaPlayer player2 = holder.getPlayer();
            if (player2 != null) {
                player2.release();
            }
        } catch (Throwable th) {
        }
        holder.setPlayer(null);
        try {
            getWindowManager().removeView(holder.getRoot());
        } catch (Throwable th2) {
        }
        this.trackedViews.remove(holder.getRoot());
    }

    private final void addWindow(WidgetConfig config) throws NoWhenBranchMatchedException {
        int overlayWidth;
        float frameHeight;
        int x;
        int y;
        Content content;
        float density = getResources().getDisplayMetrics().density;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        boolean isEditing = config.getWidgetId() == this.editingId;
        boolean visible = isEditing || config.getOverlayVisible();
        boolean positioned = config.getOverlayWidth() > 0;
        if (positioned) {
            overlayWidth = (int) (config.getOverlayWidth() * density);
        } else {
            overlayWidth = (int) (screenWidth * 0.6f);
        }
        int widthPx = RangesKt.coerceIn(overlayWidth, 160, screenWidth);
        if (config.getKind() == MediaKind.VIDEO && config.getFrameWidth() > 0 && config.getFrameHeight() > 0) {
            frameHeight = config.getFrameHeight() / config.getFrameWidth();
        } else if (config.getLayout().getRatio() > 0.0f) {
            frameHeight = 1.0f / config.getLayout().getRatio();
        } else if (config.getFrameWidth() > 0 && config.getFrameHeight() > 0) {
            frameHeight = config.getFrameHeight() / config.getFrameWidth();
        } else {
            frameHeight = 0.5625f;
        }
        float ratio = frameHeight;
        int heightPx = RangesKt.coerceIn((int) (widthPx * ratio), 120, screenHeight);
        if (!visible) {
            x = -(widthPx + 200);
            y = -(heightPx + 200);
        } else if (positioned) {
            x = (int) (config.getOverlayX() * density);
            y = (int) (config.getOverlayY() * density);
        } else {
            x = (screenWidth - widthPx) / 2;
            y = (screenHeight - heightPx) / 3;
        }
        int flags = 8 | (isEditing ? 0 : 16) | 512;
        WindowManager.LayoutParams $this$addWindow_u24lambda_u249 = new WindowManager.LayoutParams(widthPx, heightPx, 2038, flags, -3);
        $this$addWindow_u24lambda_u249.gravity = 8388659;
        $this$addWindow_u24lambda_u249.x = x;
        $this$addWindow_u24lambda_u249.y = y;
        $this$addWindow_u24lambda_u249.alpha = visible ? RangesKt.coerceIn(config.getShapeAlpha(), 0, 100) / 100.0f : 0.0f;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0);
        if (visible) {
            content = buildContent(config, widthPx, heightPx);
        } else {
            content = new Content(new View(this), null);
        }
        Content content2 = content;
        root.addView(content2.getView(), new FrameLayout.LayoutParams(-1, -1));
        if (isEditing) {
            root.addView(buildEditFrame(config, $this$addWindow_u24lambda_u249, root));
            attachDragAndScale(root, $this$addWindow_u24lambda_u249);
        }
        try {
            getWindowManager().addView(root, $this$addWindow_u24lambda_u249);
            this.trackedViews.add(root);
            this.windows.put(Integer.valueOf(config.getWidgetId()), new Holder(config, $this$addWindow_u24lambda_u249, root, content2.getPlayer()));
        } catch (Throwable t) {
            Log.w(TAG, "addView failed", t);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
    private final Content buildContent(WidgetConfig config, int widthPx, int heightPx) throws NoWhenBranchMatchedException {
        Bitmap bitmap;
        Bitmap bitmap2 = null;
        InputStream it;
        Movie movie;
        Movie movie2 = null;
        String it2 = config.getSourceUri();
        String it3 = it2.length() > 0 ? it2 : null;
        Uri uri = it3 != null ? Uri.parse(it3) : null;
        switch (WhenMappings.$EnumSwitchMapping$0[config.getKind().ordinal()]) {
            case 1:
                return buildVideoView(config, uri, widthPx, heightPx);
            case 2:
                if (uri != null) {
                    try {
                        InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
                        if (inputStreamOpenInputStream != null) {
                            it = inputStreamOpenInputStream;
                            Throwable th = null;
                            try {
                                try {
                                    Movie movieDecodeStream = Gif.decode(it);
                                    CloseableKt.closeFinally(it, (Throwable) null);
                                    movie2 = movieDecodeStream;
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th2;
                                }
                            } finally {
                            }
                        } else {
                            movie2 = null;
                        }
                    } catch (Throwable th3) {
                        movie = null;
                    }
                } else {
                    movie2 = null;
                }
                movie = movie2;
                Movie movie3 = movie;
                // v1.10.0：总时长从字节解析（Movie.duration() 可能为 0 → 覆盖层 GIF 定在第一帧）
                int gifDurationMs = 0;
                try {
                    // v1.10.498：流式数延时（覆盖层素材也可能是几十 MB 的 GIF）
                    SourceFps.Anim animMeta = SourceFps.probeAnimGifStream(this, uri);
                    if (animMeta != null) {
                        gifDurationMs = animMeta.durationMs;
                    }
                } catch (Throwable ignored) {
                }
                return new Content(new MediaOverlayView(this, null, movie3, config, gifDurationMs), null, 2, null);
            case 3:
                if (uri != null) {
                    try {
                        BitmapFactory.Options $this$buildContent_u24lambda_u2418_u24lambda_u2414 = new BitmapFactory.Options();
                        $this$buildContent_u24lambda_u2418_u24lambda_u2414.inJustDecodeBounds = true;
                        InputStream inputStreamOpenInputStream2 = getContentResolver().openInputStream(uri);
                        if (inputStreamOpenInputStream2 != null) {
                            it = inputStreamOpenInputStream2;
                            Throwable th4 = null;
                            try {
                                try {
                                    BitmapFactory.decodeStream(it, null, $this$buildContent_u24lambda_u2418_u24lambda_u2414);
                                    CloseableKt.closeFinally(it, (Throwable) null);
                                } catch (Throwable th5) {
                                    th4 = th5;
                                    throw th5;
                                }
                            } finally {
                            }
                        }
                        int sample = $this$buildContent_u24lambda_u2418_u24lambda_u2414.outWidth > widthPx ? RangesKt.coerceAtLeast(Integer.highestOneBit($this$buildContent_u24lambda_u2418_u24lambda_u2414.outWidth / widthPx), 1) : 1;
                        BitmapFactory.Options $this$buildContent_u24lambda_u2418_u24lambda_u2416 = new BitmapFactory.Options();
                        $this$buildContent_u24lambda_u2418_u24lambda_u2416.inSampleSize = sample;
                        InputStream inputStreamOpenInputStream3 = getContentResolver().openInputStream(uri);
                        if (inputStreamOpenInputStream3 != null) {
                            InputStream it4 = inputStreamOpenInputStream3;
                            try {
                                Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(it4, null, $this$buildContent_u24lambda_u2418_u24lambda_u2416);
                                CloseableKt.closeFinally(it4, (Throwable) null);
                                bitmap2 = bitmapDecodeStream;
                            } catch (Throwable th6) {
                                CloseableKt.closeFinally(it4, (Throwable) null);
                                throw th6;
                            }
                        } else {
                            bitmap2 = null;
                        }
                    } catch (Throwable th7) {
                        bitmap = null;
                    }
                } else {
                    bitmap2 = null;
                }
                bitmap = bitmap2;
                Bitmap bitmap3 = bitmap;
                return new Content(new MediaOverlayView(this, bitmap3, null, config), null, 2, null);
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    private final Content buildVideoView(WidgetConfig config, Uri uri, int widthPx, int heightPx) {
        Bitmap bitmapLoadFrame;
        final TextureView texture = new TextureView(this);
        final MediaPlayer player = new MediaPlayer();
        if (uri != null) {
            try {
                player.setDataSource(this, uri);
                player.setLooping(true);
                player.setVolume(0.0f, 0.0f);
                player.setSurface(new Surface(texture.getSurfaceTexture()));
                player.prepareAsync();
                player.setOnPreparedListener((v2) -> {
                    buildVideoView$lambda$19(texture, OverlayService.this, v2);
                });
            } catch (Throwable t) {
                Log.w(TAG, "video prepare failed", t);
            }
        }
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() { // from class: io.github.animwidget.OverlayService.buildVideoView.2
            @Override // android.view.TextureView.SurfaceTextureListener
            public void onSurfaceTextureAvailable(@NotNull SurfaceTexture st, int width, int height) {
                Intrinsics.checkNotNullParameter(st, "st");
                try {
                    player.setSurface(new Surface(st));
                    if (!player.isPlaying()) {
                        player.start();
                    }
                } catch (Throwable th) {
                }
            }

            @Override // android.view.TextureView.SurfaceTextureListener
            public void onSurfaceTextureSizeChanged(@NotNull SurfaceTexture st, int width, int height) {
                Intrinsics.checkNotNullParameter(st, "st");
                OverlayService.this.applyVideoTransform(texture, player.getVideoWidth(), player.getVideoHeight());
            }

            @Override // android.view.TextureView.SurfaceTextureListener
            public boolean onSurfaceTextureDestroyed(@NotNull SurfaceTexture st) {
                Intrinsics.checkNotNullParameter(st, "st");
                try {
                    player.setSurface(null);
                    return true;
                } catch (Throwable th) {
                    return true;
                }
            }

            @Override // android.view.TextureView.SurfaceTextureListener
            public void onSurfaceTextureUpdated(@NotNull SurfaceTexture st) {
                Intrinsics.checkNotNullParameter(st, "st");
            }
        });
        try {
            bitmapLoadFrame = WidgetStore.INSTANCE.loadFrame(this, config, 0, widthPx, heightPx);
        } catch (Throwable th) {
            bitmapLoadFrame = null;
        }
        Bitmap fallback = bitmapLoadFrame;
        FrameLayout container = new FrameLayout(this);
        if (fallback != null) {
            WidgetConfig fallbackConfig = WidgetConfig.copy$default(config, 0, null, null, null, 0, 0, 0, 0, 0, 0, null, 0, false, LayoutMode.FILL, false, false, 0, 0, 0, 100, 0, 0, 0, false, 0.0f, 0.0f, 0, 0.0f, 0.0f, null, 1065869311, null);
            container.addView(new MediaOverlayView(this, fallback, null, fallbackConfig));
        }
        container.addView(texture, new FrameLayout.LayoutParams(-1, -1));
        return new Content(container, player);
    }

    private static final void buildVideoView$lambda$19(TextureView $texture, OverlayService this$0, MediaPlayer it) throws IllegalStateException {
        Intrinsics.checkNotNullParameter($texture, "$texture");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        SurfaceTexture surfaceTexture = $texture.getSurfaceTexture();
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(it.getVideoWidth(), it.getVideoHeight());
        }
        this$0.applyVideoTransform($texture, it.getVideoWidth(), it.getVideoHeight());
        it.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void applyVideoTransform(TextureView texture, int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) {
            return;
        }
        float viewWidth = texture.getWidth();
        float viewHeight = texture.getHeight();
        if (viewWidth <= 0.0f || viewHeight <= 0.0f) {
            return;
        }
        float scale = Math.max(viewWidth / videoWidth, viewHeight / videoHeight);
        float dx = (viewWidth - (videoWidth * scale)) / 2.0f;
        float dy = (viewHeight - (videoHeight * scale)) / 2.0f;
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        texture.setTransform(matrix);
    }

    private final View buildEditFrame(WidgetConfig config, WindowManager.LayoutParams params, FrameLayout root) {
        View border = new View(this);
        border.setBackgroundColor(0);
        GradientDrawable $this$buildEditFrame_u24lambda_u2420 = new GradientDrawable();
        $this$buildEditFrame_u24lambda_u2420.setColor(0);
        $this$buildEditFrame_u24lambda_u2420.setStroke(4, Color.argb(210, 255, 255, 255));
        $this$buildEditFrame_u24lambda_u2420.setCornerRadius(18.0f);
        border.setBackground($this$buildEditFrame_u24lambda_u2420);
        border.setClickable(false);
        TextView $this$buildEditFrame_u24lambda_u2423 = new TextView(this);
        $this$buildEditFrame_u24lambda_u2423.setText("✓");
        $this$buildEditFrame_u24lambda_u2423.setTextSize(16.0f);
        $this$buildEditFrame_u24lambda_u2423.setTextColor(-16777216);
        $this$buildEditFrame_u24lambda_u2423.setGravity(17);
        GradientDrawable $this$buildEditFrame_u24lambda_u2423_u24lambda_u2421 = new GradientDrawable();
        $this$buildEditFrame_u24lambda_u2423_u24lambda_u2421.setColor(-1);
        $this$buildEditFrame_u24lambda_u2423_u24lambda_u2421.setCornerRadius(40.0f);
        $this$buildEditFrame_u24lambda_u2423.setBackground($this$buildEditFrame_u24lambda_u2423_u24lambda_u2421);
        $this$buildEditFrame_u24lambda_u2423.setOnClickListener((v3) -> {
            try {
                buildEditFrame$lambda$23$lambda$22(OverlayService.this, config, params, v3);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        FrameLayout container = new FrameLayout(this);
        container.addView(border, new FrameLayout.LayoutParams(-1, -1));
        int size = (int) (36 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams $this$buildEditFrame_u24lambda_u2424 = new FrameLayout.LayoutParams(size, size, 8388661);
        $this$buildEditFrame_u24lambda_u2424.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
        $this$buildEditFrame_u24lambda_u2424.rightMargin = (int) (6 * getResources().getDisplayMetrics().density);
        container.addView($this$buildEditFrame_u24lambda_u2423, $this$buildEditFrame_u24lambda_u2424);
        container.setClickable(false);
        return container;
    }

    private static final void buildEditFrame$lambda$23$lambda$22(OverlayService this$0, WidgetConfig $config, WindowManager.LayoutParams $params, View it) throws JSONException {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($config, "$config");
        Intrinsics.checkNotNullParameter($params, "$params");
        this$0.saveEditedPosition($config, $params);
    }

    private final void attachDragAndScale(final FrameLayout root, final WindowManager.LayoutParams params) {
        final float density = getResources().getDisplayMetrics().density;
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() { // from class: io.github.animwidget.OverlayService$attachDragAndScale$scaleDetector$1
            @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
            public boolean onScale(@NotNull ScaleGestureDetector detector) {
                Intrinsics.checkNotNullParameter(detector, "detector");
                int newWidth = RangesKt.coerceIn((int) (params.width * detector.getScaleFactor()), (int) (120 * density), OverlayService.this.getResources().getDisplayMetrics().widthPixels);
                // v1.8.2：int / int 会丢小数，缩放时比例会被拉成整数倍。
                float ratio = params.width > 0 ? params.height / (float) params.width : 1.0f;
                params.width = newWidth;
                params.height = RangesKt.coerceAtLeast((int) (newWidth * ratio), (int) (80 * density));
                try {
                    OverlayService.this.getWindowManager().updateViewLayout(root, params);
                    return true;
                } catch (Throwable th) {
                    return true;
                }
            }
        });
        Ref.FloatRef downRawX = new Ref.FloatRef();
        Ref.FloatRef downRawY = new Ref.FloatRef();
        Ref.IntRef startX = new Ref.IntRef();
        Ref.IntRef startY = new Ref.IntRef();
        root.setOnTouchListener((v8, v9) -> {
            return attachDragAndScale$lambda$25(scaleDetector, downRawX, downRawY, startX, params, startY, OverlayService.this, root, v8, v9);
        });
    }

    private static final boolean attachDragAndScale$lambda$25(ScaleGestureDetector $scaleDetector, Ref.FloatRef $downRawX, Ref.FloatRef $downRawY, Ref.IntRef $startX, WindowManager.LayoutParams $params, Ref.IntRef $startY, OverlayService this$0, FrameLayout $root, View view, MotionEvent event) {
        Intrinsics.checkNotNullParameter($scaleDetector, "$scaleDetector");
        Intrinsics.checkNotNullParameter($downRawX, "$downRawX");
        Intrinsics.checkNotNullParameter($downRawY, "$downRawY");
        Intrinsics.checkNotNullParameter($startX, "$startX");
        Intrinsics.checkNotNullParameter($params, "$params");
        Intrinsics.checkNotNullParameter($startY, "$startY");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($root, "$root");
        $scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case 0:
                $downRawX.element = event.getRawX();
                $downRawY.element = event.getRawY();
                $startX.element = $params.x;
                $startY.element = $params.y;
                break;
            case 2:
                if (!$scaleDetector.isInProgress()) {
                    $params.x = $startX.element + ((int) (event.getRawX() - $downRawX.element));
                    $params.y = $startY.element + ((int) (event.getRawY() - $downRawY.element));
                    try {
                        this$0.getWindowManager().updateViewLayout($root, $params);
                        break;
                    } catch (Throwable th) {
                        return true;
                    }
                }
                break;
        }
        return true;
    }

    private final void saveEditedPosition(WidgetConfig config, WindowManager.LayoutParams params) throws JSONException {
        float density = getResources().getDisplayMetrics().density;
        WidgetConfig updated = WidgetConfig.copy$default(config, 0, null, null, null, 0, 0, 0, 0, 0, 0, null, 0, false, null, false, false, RangesKt.coerceAtLeast((int) (params.x / density), 0), RangesKt.coerceAtLeast((int) (params.y / density), 0), RangesKt.coerceAtLeast((int) (params.width / density), 120), 0, 0, 0, 0, false, 0.0f, 0.0f, 0, 0.0f, 0.0f, null, 1073283071, null);
        WidgetStore.INSTANCE.save(this, updated);
        this.editingId = -1;
        Handler handler = this.mainHandler;
        if (handler == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mainHandler");
            handler = null;
        }
        handler.post(() -> {
            saveEditedPosition$lambda$26(OverlayService.this);
        });
    }

    private static final void saveEditedPosition$lambda$26(OverlayService this$0) throws NoWhenBranchMatchedException {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.reload();
    }

    private final void ensureChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, getString(R.string.overlay_channel), 2));
        }
    }

    private final Notification buildNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) MainActivity.class), 201326592);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, (Class<?>) OverlayService.class).setAction(ACTION_STOP), 201326592);
        Notification notificationBuild = new Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_anim).setContentTitle(getString(R.string.overlay_notif_title)).setContentText(getString(R.string.overlay_notif_text)).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_anim), getString(R.string.overlay_stop), stop).build()).build();
        Intrinsics.checkNotNullExpressionValue(notificationBuild, "build(...)");
        return notificationBuild;
    }

    /** v1.10.497：锚点开关的存储键。首页与设置页都读它，**只此一份**（原来两处各写一遍字面量）。 */
    public static final String PREF_ANCHOR = "anchor_enabled";

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean anchorEnabled() {
        return getSharedPreferences("settings", 0).getBoolean(PREF_ANCHOR, true);
    }

    /** v1.10.497：给设置页读「当前是开还是关」（与运行期判据同一个键、同一个默认值）。 */
    public static boolean anchorEnabledFor(@NotNull Context context) {
        return context.getSharedPreferences("settings", 0).getBoolean(PREF_ANCHOR, true);
    }

    private final boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
}
