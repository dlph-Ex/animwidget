package io.github.animwidget;
import android.graphics.BitmapFactory.Options;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.io.FilesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/* compiled from: WidgetConfig.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��J\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\b\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\u0006\u0010\t\u001a\u00020\nJ\u0010\u0010\u000b\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\nH\u0002J\u0016\u0010\r\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fJ\u0016\u0010\u0010\u001a\u00020\u00112\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fJ\u001e\u0010\u0012\u001a\u00020\u00112\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u000fJ\u0016\u0010\u0014\u001a\u00020\u00112\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0015\u001a\u00020\u000f2\u0006\u0010\t\u001a\u00020\nJ\u0018\u0010\u0016\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fJ4\u0010\u0017\u001a\u0004\u0018\u00010\u00182\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u000f2\b\b\u0002\u0010\u001a\u001a\u00020\u000f2\b\b\u0002\u0010\u001b\u001a\u00020\u000fJ\u0018\u0010\u001c\u001a\n \u001e*\u0004\u0018\u00010\u001d0\u001d2\u0006\u0010\t\u001a\u00020\nH\u0002J(\u0010\u001f\u001a\u00020\u000f2\u0006\u0010 \u001a\u00020\u000f2\u0006\u0010!\u001a\u00020\u000f2\u0006\u0010\"\u001a\u00020\u000f2\u0006\u0010#\u001a\u00020\u000fH\u0002J\u0016\u0010$\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\bJ\u001e\u0010%\u001a\u00020\u00112\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/WidgetStore;", "", "()V", "KEY_GENERATION", "", "PREF", "all", "", "Lio/github/animwidget/WidgetConfig;", "context", "Landroid/content/Context;", "bump", "", "delete", "widgetId", "", "dir", "Ljava/io/File;", "frameFile", "index", "framesDir", "KEY_GENERATION", "load", "loadFrame", "Landroid/graphics/Bitmap;", "config", "targetWidth", "targetHeight", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "sampleSize", "sw", "sh", "tw", "th", "save", "tempFile"})
@SourceDebugExtension({"SMAP\nWidgetConfig.kt\nKotlin\n*S Kotlin\n*F\n+ 1 WidgetConfig.kt\nio/github/animwidget/WidgetStore\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,324:1\n1#2:325\n1#2:340\n766#3:326\n857#3,2:327\n1045#3:329\n1603#3,9:330\n1855#3:339\n1856#3:341\n1612#3:342\n*S KotlinDebug\n*F\n+ 1 WidgetConfig.kt\nio/github/animwidget/WidgetStore\n*L\n280#1:340\n278#1:326\n278#1:327,2\n279#1:329\n280#1:330,9\n280#1:339\n280#1:341\n280#1:342\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/WidgetStore.class */
public final class WidgetStore {

    @NotNull
    public static final WidgetStore INSTANCE = new WidgetStore();

    @NotNull
    private static final String PREF = "widgets";

    @NotNull
    private static final String KEY_GENERATION = "generation";

    private WidgetStore() {
    }

    /**
     * v1.10.8：帧率小数（×10）。配置里的 fps 是整数（滑块只能整），
     * 但素材真实帧率常常是 33.3 / 23.976 —— 播放间隔必须按 ×10 精确算，
     * 否则 1 秒素材就差十几毫秒。
     */
    public final void setFpsX10(@NotNull Context context, int widgetId, int fpsX10) {
        Intrinsics.checkNotNullParameter(context, "context");
        prefs(context).edit().putInt("fpsx10_" + widgetId, fpsX10).apply();
    }

    public final int fpsX10(@NotNull Context context, int widgetId, int fallbackFps) {
        Intrinsics.checkNotNullParameter(context, "context");
        int value = prefs(context).getInt("fpsx10_" + widgetId, 0);
        if (value > 0) {
            return value;
        }
        return Math.max(fallbackFps, 1) * 10;
    }

    private final SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, 0);
    }

    @NotNull
    public final File dir(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        return new File(context.getFilesDir(), "w/" + widgetId);
    }

    @NotNull
    public final File framesDir(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        File $this$framesDir_u24lambda_u240 = new File(dir(context, widgetId), "frames");
        $this$framesDir_u24lambda_u240.mkdirs();
        return $this$framesDir_u24lambda_u240;
    }

    /**
     * v1.10.1：导出的「暂存目录」。
     * 导出先写这里，全部成功后再整体换过去 —— 导出中途被取消（切应用/回桌面/锁屏）
     * 只删暂存，旧帧毫发无损，组件不会变成半截。
     */
    @NotNull
    public final File stagingDir(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        File file = new File(dir(context, widgetId), "frames.new");
        file.mkdirs();
        return file;
    }

    @NotNull
    public final File stagingFile(@NotNull Context context, int widgetId, String prefix, int index) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(prefix, "prefix");
        Object[] objArr = {Integer.valueOf(index)};
        String str = String.format(prefix + "%05d.webp", Arrays.copyOf(objArr, objArr.length));
        return new File(stagingDir(context, widgetId), str);
    }

    /** 导出成功：暂存整体转正（先删旧帧，再改名）。 */
    public final void commitStaging(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        File target = new File(dir(context, widgetId), "frames");
        File staging = new File(dir(context, widgetId), "frames.new");
        if (!staging.isDirectory()) {
            return;
        }
        deleteTree(target);
        target.mkdirs();
        File[] files = staging.listFiles();
        if (files != null) {
            for (File file : files) {
                file.renameTo(new File(target, file.getName()));
            }
        }
        deleteTree(staging);
    }

    /** 导出被取消：删掉暂存，什么都不留。 */
    public final void discardStaging(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        deleteTree(new File(dir(context, widgetId), "frames.new"));
    }

    /** 启动时清理上次残留的暂存（进程被杀等情况）。 */
    public final void cleanupStaging(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        File root = new File(context.getFilesDir(), "w");
        File[] subs = root.listFiles();
        if (subs == null) {
            return;
        }
        for (File sub : subs) {
            deleteTree(new File(sub, "frames.new"));
        }
    }

    private static void deleteTree(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }

    public final File frameFile(@NotNull Context context, int widgetId, int index) {
        Intrinsics.checkNotNullParameter(context, "context");
        File fileFramesDir = framesDir(context, widgetId);
        Object[] objArr = {Integer.valueOf(index)};
        String str = String.format("f%05d.webp", Arrays.copyOf(objArr, objArr.length));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return new File(fileFramesDir, str);
    }

    @NotNull
    public final File tempFile(@NotNull Context context, int widgetId, int index) {
        Intrinsics.checkNotNullParameter(context, "context");
        File fileFramesDir = framesDir(context, widgetId);
        Object[] objArr = {Integer.valueOf(index)};
        String str = String.format("tmp_%05d.webp", Arrays.copyOf(objArr, objArr.length));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return new File(fileFramesDir, str);
    }

    public final int generation(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        return prefs(context).getInt(KEY_GENERATION, 0);
    }

    private final void bump(Context context) {
        prefs(context).edit().putInt(KEY_GENERATION, generation(context) + 1).apply();
    }

    public final void save(@NotNull Context context, @NotNull WidgetConfig config) throws JSONException {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(config, "config");
        JSONObject $this$save_u24lambda_u241 = new JSONObject();
        $this$save_u24lambda_u241.put("id", config.getWidgetId());
        $this$save_u24lambda_u241.put("kind", config.getKind().name());
        $this$save_u24lambda_u241.put("name", config.getSourceName());
        $this$save_u24lambda_u241.put("uri", config.getSourceUri());
        $this$save_u24lambda_u241.put("fps", config.getFps());
        $this$save_u24lambda_u241.put("edge", config.getMaxEdge());
        $this$save_u24lambda_u241.put("quality", config.getQuality());
        $this$save_u24lambda_u241.put("count", config.getFrameCount());
        $this$save_u24lambda_u241.put("fw", config.getFrameWidth());
        $this$save_u24lambda_u241.put("fh", config.getFrameHeight());
        $this$save_u24lambda_u241.put("shape", config.getShape().name());
        $this$save_u24lambda_u241.put("alpha", config.getShapeAlpha());
        $this$save_u24lambda_u241.put("border", config.getBorder());
        $this$save_u24lambda_u241.put("layout", config.getLayout().name());
        $this$save_u24lambda_u241.put("scale", config.getScalePercent());
        $this$save_u24lambda_u241.put("shadow", config.getShadowPercent());
        // v1.10.221：阴影曲线跟着组件走（默认曲线存空串，老存档读不到就是空串）
        $this$save_u24lambda_u241.put("shadowCurve", config.getShadowCurve());
        $this$save_u24lambda_u241.put("rotate", config.getRotation());
        $this$save_u24lambda_u241.put("shapeRotate", config.getShapeRotation());
        $this$save_u24lambda_u241.put("fitContent", config.getFitContent());
        $this$save_u24lambda_u241.put("offsetX", config.getOffsetXPercent());
        $this$save_u24lambda_u241.put("offsetY", config.getOffsetYPercent());
        $this$save_u24lambda_u241.put("contentScale", config.getContentScalePercent());
        $this$save_u24lambda_u241.put("maskX", config.getMaskOffsetXPercent());
        $this$save_u24lambda_u241.put("maskY", config.getMaskOffsetYPercent());
        $this$save_u24lambda_u241.put("overlay", config.getOverlayEnabled());
        $this$save_u24lambda_u241.put("overlayVisible", config.getOverlayVisible());
        $this$save_u24lambda_u241.put("ox", config.getOverlayX());
        $this$save_u24lambda_u241.put("oy", config.getOverlayY());
        $this$save_u24lambda_u241.put("ow", config.getOverlayWidth());
        prefs(context).edit().putString(String.valueOf(config.getWidgetId()), $this$save_u24lambda_u241.toString()).apply();
        bump(context);
    }

    @Nullable
    public final WidgetConfig load(@NotNull Context context, int widgetId) {
        WidgetConfig widgetConfig;
        Object obj;
        Object obj2;
        Object obj3;
        Intrinsics.checkNotNullParameter(context, "context");
        String raw = prefs(context).getString(String.valueOf(widgetId), null);
        if (raw == null) {
            return null;
        }
        try {
            JSONObject j = new JSONObject(raw);
            try {
                Result.Companion companion = Result.Companion;
                WidgetStore widgetStore = this;
                String strOptString = j.optString("kind", "VIDEO");
                Intrinsics.checkNotNullExpressionValue(strOptString, "optString(...)");
                obj = (MediaKind.valueOf(strOptString));
            } catch (Throwable th) {
                Result.Companion companion2 = Result.Companion;
                obj = (ResultKt.createFailure(th));
            }
            Object obj4 = obj;
            MediaKind mediaKind = (MediaKind) ((obj4 instanceof kotlin.Result.Failure) ? MediaKind.VIDEO : obj4);
            String strOptString2 = j.optString("name", "");
            String strOptString3 = j.optString("uri", "");
            int iCoerceIn = RangesKt.coerceIn(j.optInt("fps", 12), 1, 50);
            int iCoerceIn2 = RangesKt.coerceIn(j.optInt("edge", 720), 120, 2560);
            int iCoerceIn3 = RangesKt.coerceIn(j.optInt("scale", RangesKt.coerceIn(100 - (j.optInt("pad", 0) / 2), 0, 100)), 0, 100);
            int iCoerceIn4 = RangesKt.coerceIn(j.optInt("shadow", 0), 0, ShadowSpec.STRENGTH_MAX);
            // v1.10.221：阴影曲线（空串/坏串由 ShadowCurve.parse 兜底成默认曲线）
            String strShadowCurve = j.optString("shadowCurve", "");
            int iCoerceIn5 = RangesKt.coerceIn(j.optInt("rotate", 0), -360, 360);
            int iCoerceIn6 = RangesKt.coerceIn(j.optInt("shapeRotate", 0), -360, 360);
            boolean zOptBoolean = j.optBoolean("fitContent", true);
            float fCoerceIn = RangesKt.coerceIn((float) j.optDouble("offsetX", 0.0d), -50.0f, 50.0f);
            float fCoerceIn2 = RangesKt.coerceIn((float) j.optDouble("offsetY", 0.0d), -50.0f, 50.0f);
            int iCoerceIn7 = RangesKt.coerceIn(j.optInt("contentScale", 100), 0, 200);
            float fCoerceIn3 = RangesKt.coerceIn((float) j.optDouble("maskX", 0.0d), -50.0f, 50.0f);
            float fCoerceIn4 = RangesKt.coerceIn((float) j.optDouble("maskY", 0.0d), -50.0f, 50.0f);
            int iCoerceIn8 = RangesKt.coerceIn(j.optInt("quality", 90), 50, 100);
            int iOptInt = j.optInt("count", 0);
            int iOptInt2 = j.optInt("fw", 0);
            int iOptInt3 = j.optInt("fh", 0);
            try {
                Result.Companion companion3 = Result.Companion;
                WidgetStore widgetStore2 = this;
                String strOptString4 = j.optString("shape", "ROUNDED");
                Intrinsics.checkNotNullExpressionValue(strOptString4, "optString(...)");
                obj2 = (ShapeKind.valueOf(strOptString4));
            } catch (Throwable th2) {
                Result.Companion companion4 = Result.Companion;
                obj2 = (ResultKt.createFailure(th2));
            }
            Object obj5 = obj2;
            ShapeKind shapeKind = (ShapeKind) ((obj5 instanceof kotlin.Result.Failure) ? ShapeKind.ROUNDED : obj5);
            int iCoerceIn9 = RangesKt.coerceIn(j.optInt("alpha", 100), 0, 100);
            boolean zOptBoolean2 = j.optBoolean("border", false);
            try {
                Result.Companion companion5 = Result.Companion;
                WidgetStore widgetStore3 = this;
                String strOptString5 = j.optString("layout", "FILL");
                Intrinsics.checkNotNullExpressionValue(strOptString5, "optString(...)");
                obj3 = (LayoutMode.valueOf(strOptString5));
            } catch (Throwable th3) {
                Result.Companion companion6 = Result.Companion;
                obj3 = (ResultKt.createFailure(th3));
            }
            Object obj6 = obj3;
            LayoutMode layoutMode = (LayoutMode) ((obj6 instanceof kotlin.Result.Failure) ? LayoutMode.FILL : obj6);
            boolean zOptBoolean3 = j.optBoolean("overlay", false);
            boolean zOptBoolean4 = j.optBoolean("overlayVisible", false);
            int iOptInt4 = j.optInt("ox", 0);
            int iOptInt5 = j.optInt("oy", 0);
            int iOptInt6 = j.optInt("ow", 0);
            Intrinsics.checkNotNull(strOptString2);
            Intrinsics.checkNotNull(strOptString3);
            widgetConfig = new WidgetConfig(widgetId, mediaKind, strOptString2, strOptString3, iCoerceIn, iCoerceIn2, iCoerceIn8, iOptInt, iOptInt2, iOptInt3, shapeKind, iCoerceIn9, zOptBoolean2, layoutMode, zOptBoolean3, zOptBoolean4, iOptInt4, iOptInt5, iOptInt6, iCoerceIn3, iCoerceIn4, iCoerceIn5, iCoerceIn6, zOptBoolean, fCoerceIn, fCoerceIn2, iCoerceIn7, fCoerceIn3, fCoerceIn4, strShadowCurve);
        } catch (Throwable th4) {
            widgetConfig = null;
        }
        return widgetConfig;
    }

    @NotNull
    public final List<WidgetConfig> all(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        Iterable $this$filter$iv = prefs(context).getAll().keySet();
        Collection destination$iv$iv = new ArrayList();
        for (Object element$iv$iv : $this$filter$iv) {
            String it = (String) element$iv$iv;
            Intrinsics.checkNotNull(it);
            if (StringsKt.toIntOrNull(it) != null) {
                destination$iv$iv.add(element$iv$iv);
            }
        }
        Iterable $this$sortedBy$iv = (List) destination$iv$iv;
        Iterable keys = CollectionsKt.sortedWith($this$sortedBy$iv, new Comparator() { // from class: io.github.animwidget.WidgetStore$all$$inlined$sortedBy$1
            /* JADX WARN: Multi-variable type inference failed */
            @Override // java.util.Comparator
            public final int compare(Object t, Object t2) {
                String it2 = (String) t;
                Intrinsics.checkNotNull(it2);
                Integer numValueOf = Integer.valueOf(Integer.parseInt(it2));
                String it3 = (String) t2;
                Intrinsics.checkNotNull(it3);
                return ComparisonsKt.compareValues(numValueOf, Integer.valueOf(Integer.parseInt(it3)));
            }
        });
        Iterable $this$mapNotNull$iv = keys;
        Collection destination$iv$iv2 = new ArrayList();
        for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
            String it2 = (String) element$iv$iv$iv;
            WidgetStore widgetStore = INSTANCE;
            Intrinsics.checkNotNull(it2);
            WidgetConfig widgetConfigLoad = widgetStore.load(context, Integer.parseInt(it2));
            if (widgetConfigLoad != null) {
                destination$iv$iv2.add(widgetConfigLoad);
            }
        }
        return (List) destination$iv$iv2;
    }

    public final void delete(@NotNull Context context, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        prefs(context).edit().remove(String.valueOf(widgetId)).apply();
        FilesKt.deleteRecursively(dir(context, widgetId));
        // v1.10.119：URI 推送缓存（files/push/w<id>/…）也要删，否则删掉的组件会把几百帧留在盘上
        WidgetUriPush.clearWidget(context, widgetId);
        bump(context);
    }

    public static /* synthetic */ Bitmap loadFrame$default(WidgetStore widgetStore, Context context, WidgetConfig widgetConfig, int i, int i2, int i3, int i4, Object obj) {
        if ((i4 & 8) != 0) {
            i2 = 0;
        }
        if ((i4 & 16) != 0) {
            i3 = 0;
        }
        return widgetStore.loadFrame(context, widgetConfig, i, i2, i3);
    }

    @Nullable
    public final Bitmap loadFrame(@NotNull Context context, @NotNull WidgetConfig config, int index, int targetWidth, int targetHeight) {
        Bitmap bitmapDecodeFile;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(config, "config");
        if (config.getFrameCount() <= 0) {
            return null;
        }
        int safe = ((index % config.getFrameCount()) + config.getFrameCount()) % config.getFrameCount();
        File file = frameFile(context, config.getWidgetId(), safe);
        if (!file.exists()) {
            // v1.10.451：**导出刚完成、还没提交时，帧在 `frames.new`（staging）里。**
            // 导出链是「先烘完、再提交」（428 的设计），而烘焙要读帧 ——
            // 不读这里就一帧都读不到。实机日志（12:04:59 / 12:05:07）：
            //   W13 预热完成 … 完成=0 失败=235 用时=6ms 速率=0帧/秒
            // 235 次全部在 `!file.exists()` 这一行返回 null，一个字节都没读。
            File staged = stagingFile(context, config.getWidgetId(), "f", safe);
            if (staged.exists()) {
                file = staged;
            }
        }
        if (!file.exists()) {
            return null;
        }
        // v1.10.54：**按目标尺寸解码**。
        // 原来只有 inSampleSize（2 的幂）可用：2560 的帧只能采到 1280×720，
        // 再缩到 458×480 —— 白白多解 2~3 倍像素，小部件就卡在 17~18fps（日志 W10 实测）。
        // API 28+ 用 ImageDecoder.setTargetSize 直接解到「覆盖目标框」的尺寸。
        if (android.os.Build.VERSION.SDK_INT >= 28 && targetWidth > 0 && targetHeight > 0) {
            try {
                Bitmap decoded = decodeAtSize(file, targetWidth, targetHeight);
                if (decoded != null) {
                    Bitmap fixed = ColorFix.toSrgb(decoded);
                    // v1.10.58：转换失败（null）就不要把硬件位图带出去，退回 BitmapFactory 软解
                    if (fixed != null) {
                        return fixed;
                    }
                    Diag.warnThrottled(context, "W15", "ImageDecoder 结果不可用（疑似硬件位图），退回软解");
                }
            } catch (Throwable ignored) {
            }
        }
        // v1.10.1：统一按 sRGB + ARGB_8888 解码（色差修复）
        BitmapFactory.Options $this$loadFrame_u24lambda_u248 = ColorFix.options();
        if (targetWidth > 0 && targetHeight > 0 && config.getFrameWidth() > 0 && config.getFrameHeight() > 0) {
            $this$loadFrame_u24lambda_u248.inSampleSize = INSTANCE.sampleSize(config.getFrameWidth(), config.getFrameHeight(), targetWidth, targetHeight);
        }
        try {
            bitmapDecodeFile = ColorFix.toSrgb(BitmapFactory.decodeFile(file.getAbsolutePath(), $this$loadFrame_u24lambda_u248));
        } catch (Throwable th) {
            bitmapDecodeFile = null;
        }
        return bitmapDecodeFile;
    }

    /**
     * v1.10.54：解码到「刚好覆盖目标框」的尺寸（保持比例，留 15% 余量给旋转/裁剪）。
     * 例如 2560×1440 的帧、目标 458×480 → 解到 981×552，而不是 1280×720。
     */
    @Nullable
    private final Bitmap decodeAtSize(@NotNull File file, final int tw, final int th) {
        if (android.os.Build.VERSION.SDK_INT < 28) {
            return null;
        }
        try {
            android.graphics.ImageDecoder.Source src = android.graphics.ImageDecoder.createSource(file);
            return android.graphics.ImageDecoder.decodeBitmap(src,
                    (decoder, info, source) -> {
                        // v1.10.58（致命 bug 修复）：**必须先无条件设成软件分配器。**
                        // ImageDecoder 在 API 28+ 默认 ALLOCATOR_HARDWARE，解出来是硬件位图；
                        // 而 FrameComposer 是在**软件 Canvas** 上画的 —— 一旦帧比目标框还小（k 被夹到 1，
                        // 不缩放）就走进了「提前 return、分配器没设」的分支 → 硬件位图 →
                        // Canvas.drawBitmap 抛 IllegalArgumentException("Software rendering doesn't
                        // support hardware bitmaps") → 每一帧都异常 → 组件永远不更新（只剩占位图标）。
                        // 动图帧普遍比组件框小（比如 400px 的 GIF 放进 458×480），所以只有动图中招。
                        decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                        int w = info.getSize().getWidth();
                        int h = info.getSize().getHeight();
                        if (w <= 0 || h <= 0) {
                            return;
                        }
                        float cover = Math.max((float) tw / w, (float) th / h);
                        float k = Math.min(1.0f, cover * 1.15f);
                        if (k >= 0.999f) {
                            return;
                        }
                        decoder.setTargetSize(Math.max(1, Math.round(w * k)), Math.max(1, Math.round(h * k)));
                    });
        } catch (Throwable th2) {
            return null;
        }
    }

    private final int sampleSize(int sw, int sh, int tw, int th) {
        int sample;
        int i = 1;
        while (true) {
            sample = i;
            if (sw / (sample * 2) < tw || sh / (sample * 2) < th) {
                break;
            }
            i = sample * 2;
        }
        return sample;
    }
}
