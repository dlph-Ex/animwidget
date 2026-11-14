package io.github.animwidget;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Window;
import android.widget.SeekBar.OnSeekBarChangeListener;
import kotlin.jvm.internal.Ref.ObjectRef;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import io.github.animwidget.MediaImporter;
import io.github.animwidget.PreviewFrames;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.Deprecated;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

/* compiled from: ConfigActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��ÿ\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0015\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0002\b\u0019\n\u0002\u0018\u0002\n\u0002\b\u0012*\u0001V\u0018�� ½\u00012\u00020\u0001:\u0002½\u0001B\u0005¢\u0006\u0002\u0010\u0002J\b\u0010e\u001a\u00020fH\u0002J\b\u0010g\u001a\u00020fH\u0002J\u0010\u0010h\u001a\u00020f2\u0006\u0010i\u001a\u00020\u001cH\u0002J\b\u0010j\u001a\u00020fH\u0002J\b\u0010k\u001a\u00020fH\u0002J\u0010\u0010l\u001a\u00020f2\u0006\u0010m\u001a\u00020\u0004H\u0002J\u0010\u0010n\u001a\u00020f2\u0006\u0010o\u001a\u00020pH\u0014J\b\u0010q\u001a\u00020fH\u0002J-\u0010r\u001a\u00020 2\b\u0010s\u001a\u0004\u0018\u00010 2\b\u0010t\u001a\u0004\u0018\u00010$2\n\b\u0002\u0010u\u001a\u0004\u0018\u00010PH\u0002¢\u0006\u0002\u0010vJ\u0010\u0010w\u001a\u00020 2\u0006\u0010x\u001a\u00020\"H\u0002J\b\u0010y\u001a\u00020fH\u0002J\u0010\u0010z\u001a\u00020f2\u0006\u0010{\u001a\u00020\u0006H\u0002J\b\u0010|\u001a\u00020\u0004H\u0002J\u0010\u0010}\u001a\u00020 2\u0006\u0010~\u001a\u00020PH\u0002J\u0018\u0010\u007f\u001a\u00020f2\u0006\u0010{\u001a\u00020\u00062\u0006\u0010x\u001a\u00020\"H\u0002J\u0019\u0010\u0080\u0001\u001a\u00020f2\u0006\u0010x\u001a\u00020\"2\u0006\u0010i\u001a\u00020\u001cH\u0002J\u0019\u0010\u0081\u0001\u001a\u00020f2\u0006\u0010x\u001a\u00020\"2\u0006\u0010i\u001a\u00020\u001cH\u0002J\t\u0010\u0082\u0001\u001a\u00020HH\u0002J'\u0010\u0083\u0001\u001a\u00020f2\u0007\u0010\u0084\u0001\u001a\u00020$2\u0007\u0010\u0085\u0001\u001a\u00020$2\n\u0010\u0086\u0001\u001a\u0005\u0018\u00010\u0087\u0001H\u0015J\u0013\u0010\u0088\u0001\u001a\u00020f2\b\u0010\u0089\u0001\u001a\u00030\u008a\u0001H\u0016J\u0015\u0010\u008b\u0001\u001a\u00020f2\n\u0010\u008c\u0001\u001a\u0005\u0018\u00010\u008d\u0001H\u0014J\t\u0010\u008e\u0001\u001a\u00020fH\u0014J3\u0010\u008f\u0001\u001a\u00020f2\u0007\u0010\u0084\u0001\u001a\u00020$2\u0010\u0010\u0090\u0001\u001a\u000b\u0012\u0006\b\u0001\u0012\u00020 0\u0091\u00012\u0007\u0010\u0092\u0001\u001a\u00020\u0013H\u0016¢\u0006\u0003\u0010\u0093\u0001J\t\u0010\u0094\u0001\u001a\u00020fH\u0014J\t\u0010\u0095\u0001\u001a\u00020fH\u0002J\t\u0010\u0096\u0001\u001a\u00020fH\u0002J\u0011\u0010\u0097\u0001\u001a\u00020f2\u0006\u0010i\u001a\u00020\u001cH\u0002J\t\u0010\u0098\u0001\u001a\u00020$H\u0002J\t\u0010\u0099\u0001\u001a\u00020$H\u0002J\t\u0010\u009a\u0001\u001a\u00020fH\u0002J\u0012\u0010\u009b\u0001\u001a\u00020f2\u0007\u0010\u009c\u0001\u001a\u00020\u0013H\u0002J\t\u0010\u009d\u0001\u001a\u00020fH\u0002J\t\u0010\u009e\u0001\u001a\u00020fH\u0002J\u0012\u0010\u009f\u0001\u001a\u00020f2\u0007\u0010 \u0001\u001a\u00020$H\u0002J\u0012\u0010¡\u0001\u001a\u00020f2\u0007\u0010 \u0001\u001a\u00020$H\u0002J\t\u0010¢\u0001\u001a\u00020fH\u0002J\t\u0010£\u0001\u001a\u00020fH\u0002J\t\u0010¤\u0001\u001a\u00020fH\u0002J\t\u0010¥\u0001\u001a\u00020fH\u0002J\u0012\u0010¦\u0001\u001a\u00020f2\u0007\u0010§\u0001\u001a\u00020\u0004H\u0002J1\u0010¨\u0001\u001a\u00020f2\u0007\u0010©\u0001\u001a\u00020 2\u0007\u0010§\u0001\u001a\u00020$2\u0014\u0010ª\u0001\u001a\u000f\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020f0«\u0001H\u0002J\u0012\u0010¬\u0001\u001a\u00020f2\u0007\u0010\u00ad\u0001\u001a\u00020\u0004H\u0002J\t\u0010®\u0001\u001a\u00020HH\u0002J\u0014\u0010¯\u0001\u001a\u00020f2\t\b\u0002\u0010°\u0001\u001a\u00020\u0004H\u0002J\t\u0010±\u0001\u001a\u00020fH\u0002J\t\u0010²\u0001\u001a\u00020fH\u0002J\t\u0010³\u0001\u001a\u00020$H\u0002J\u0012\u0010´\u0001\u001a\u00020f2\u0007\u0010µ\u0001\u001a\u00020 H\u0002J\t\u0010¶\u0001\u001a\u00020fH\u0002J\t\u0010·\u0001\u001a\u00020fH\u0002J\t\u0010¸\u0001\u001a\u00020fH\u0002J\u0012\u0010¹\u0001\u001a\u00020f2\u0007\u0010 \u0001\u001a\u00020$H\u0002J\t\u0010º\u0001\u001a\u00020fH\u0002J\t\u0010»\u0001\u001a\u00020fH\u0002J\t\u0010¼\u0001\u001a\u00020HH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u000e\u0010\t\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u000e\u0010\n\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\f\u001a\u00020\rX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000e\u001a\u00020\rX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000f\u001a\u00020\rX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082.¢\u0006\u0002\n��R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082.¢\u0006\u0002\n��R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082.¢\u0006\u0002\n��R\u000e\u0010\u001a\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u001b\u001a\u00020\u001cX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u001d\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u001e\u001a\u0004\u0018\u00010\u001cX\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u001f\u001a\u0004\u0018\u00010 X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010!\u001a\u0004\u0018\u00010\"X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010#\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010%\u001a\u00020&X\u0082\u0004¢\u0006\u0002\n��R\u0010\u0010'\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010(\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010)\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010*\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010+\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u0016\u0010,\u001a\n .*\u0004\u0018\u00010-0-X\u0082\u0004¢\u0006\u0002\n��R\u0010\u0010/\u001a\u0004\u0018\u000100X\u0082\u000e¢\u0006\u0002\n��R\u000e\u00101\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u00102\u001a\u000203X\u0082\u0004¢\u0006\u0002\n��R\u000e\u00104\u001a\u00020&X\u0082\u0004¢\u0006\u0002\n��R\u0010\u00105\u001a\u0004\u0018\u000106X\u0082\u000e¢\u0006\u0002\n��R\u0010\u00107\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n��R\u000e\u00108\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u00109\u001a\u00020\u0019X\u0082.¢\u0006\u0002\n��R\u0010\u0010:\u001a\u0004\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010;\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010=\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010>\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010?\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010@\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010A\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010B\u001a\u00020<X\u0082.¢\u0006\u0002\n��R\u000e\u0010C\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010D\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010E\u001a\u00020FX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010G\u001a\u00020HX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010I\u001a\u00020HX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010J\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010K\u001a\u00020LX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010M\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010N\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010O\u001a\u00020PX\u0082\u000e¢\u0006\u0002\n��R\u0012\u0010Q\u001a\u0004\u0018\u00010$X\u0082\u000e¢\u0006\u0004\n\u0002\u0010RR\u000e\u0010S\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010T\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010U\u001a\u00020VX\u0082\u0004¢\u0006\u0004\n\u0002\u0010WR\u000e\u0010X\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010Z\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010[\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010\\\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010]\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010^\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010_\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010`\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010a\u001a\u00020YX\u0082.¢\u0006\u0002\n��R\u000e\u0010b\u001a\u00020cX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010d\u001a\u00020$X\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/ConfigActivity;", "Landroid/app/Activity;", "()V", "appliedDark", "", "base", "Lio/github/animwidget/WidgetConfig;", "btnRotate", "Landroid/widget/Button;", "btnRotateShape", "btnSave", "busy", "cbBorder", "Landroid/widget/CheckBox;", "cbOverlay", "cbOverlayVisible", "imgPreview", "Landroid/widget/ImageView;", "layoutChipIds", "", "mediaSelector", "Lio/github/animwidget/MediaSelectorView;", "paramTask", "Ljava/lang/Runnable;", "pbPreview", "Landroid/widget/ProgressBar;", "pendingFirstFrame", "pendingKind", "Lio/github/animwidget/MediaKind;", "pendingPreview", "pickedKind", "pickedName", "", "pickedUri", "Landroid/net/Uri;", "playIndex", "", "playbackBusy", "Ljava/util/concurrent/atomic/AtomicBoolean;", "playbackRun", "playing", "previewBoxHeight", "previewBoxWidth", "previewDirty", "previewExecutor", "Ljava/util/concurrent/ExecutorService;", "kotlin.jvm.PlatformType", "previewFrames", "Lio/github/animwidget/PreviewFrames$Result;", "previewFramesLoading", "previewGeneration", "Ljava/util/concurrent/atomic/AtomicInteger;", "previewRenderBusy", "previewSource", "Landroid/graphics/Bitmap;", "previewTask", "previewing", "progress", "refillTask", "seekAlpha", "Landroid/widget/SeekBar;", "seekEdge", "seekFps", "seekPlayback", "seekQuality", "seekScale", "seekShadow", "selectedContentScale", "selectedFitContent", "selectedLayout", "Lio/github/animwidget/LayoutMode;", "selectedOffsetX", "", "selectedOffsetY", "selectedRotation", "selectedShape", "Lio/github/animwidget/ShapeKind;", "selectedShapeRotation", "shapesExpanded", "sourceDurationMs", "", "sourceFps", "Ljava/lang/Integer;", "sourceHeight", "sourceWidth", "themePoll", "io/github/animwidget/Runnable", "Lio/github/animwidget/Runnable;", "tvAlpha", "Landroid/widget/TextView;", "tvEdge", "tvFps", "tvPick", "tvPlayTime", "tvQuality", "tvResolution", "tvScale", "tvShadow", "ui", "Landroid/os/Handler;", "widgetId", "applyAutoParams", "", "applyDeskBackground", "applyKindUi", "kind", "applyPreviewClip", "applyPreviewFrames", "applyTabColors", "content", "attachBaseContext", "newBase", "Landroid/content/Context;", "buildShapeGrid", "buildSourceInfo", "resolution", "fps", "durationMs", "(Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Long;)Ljava/lang/String;", "displayName", "uri", "ensureWallpaperPermission", "finishSave", "config", "fitContentLocked", "formatTime", "millis", "importAsync", "loadPreview", "loadPreviewFrames", "mediaAspect", "onActivityResult", "requestCode", "resultCode", "data", "Landroid/content/Intent;", "onConfigurationChanged", "newConfig", "Landroid/content/res/Configuration;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onRequestPermissionsResult", "permissions", "", "grantResults", "(I[Ljava/lang/String;[I)V", "onResume", "openContentGestureEditor", "openOverlayEditor", "pick", "previewHeight", "previewWidth", "refreshFitButton", "refreshLayoutChips", "chips", "refreshPreviewNow", "refreshThemeInPlace", "renderPlaybackFrame", "index", "renderPreviewFrameAt", "renderPreviewNow", "save", "schedulePreview", "scheduleRefillFrames", "setBusy", "value", "showNumberDialog", "title", "onApply", "Lkotlin/Function1;", "showRotateDialog", "isShape", "sourceAspect", "startPlayback", "holdFirst", "startPreviewFramesPlayback", "stopPlaybackLoop", "systemControlColor", "toast", "message", "updateEmptyHint", "updateLabels", "updateMediaVisibility", "updatePlayTime", "updatePreviewBox", "updateRotateButtons", "widgetAspect", "Companion"})
@SourceDebugExtension({"SMAP\nConfigActivity.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ConfigActivity.kt\nio/github/animwidget/ConfigActivity\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,1730:1\n13374#2,3:1731\n13404#2,3:1734\n3792#2:1738\n4307#2,2:1739\n13374#2,3:1744\n1#3:1737\n1864#4,3:1741\n1855#4,2:1747\n*S KotlinDebug\n*F\n+ 1 ConfigActivity.kt\nio/github/animwidget/ConfigActivity\n*L\n345#1:1731,3\n357#1:1734,3\n787#1:1738\n787#1:1739,2\n875#1:1744,3\n801#1:1741,3\n1529#1:1747,2\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/ConfigActivity.class */
public final class ConfigActivity extends I18nActivity {

    @NotNull
    public static final Companion Companion = new Companion(null);
    int widgetId;

    @Nullable
    WidgetConfig base;

    Uri pickedUri;

    @Nullable
    MediaKind pickedKind;

    @Nullable
    String pickedName;

    @Nullable
    Integer sourceFps;
    int sourceWidth;
    int sourceHeight;
    long sourceDurationMs;
    boolean shapesExpanded;
    int selectedRotation;
    int selectedShapeRotation;
    float selectedOffsetX;
    float selectedOffsetY;
    float selectedMaskX;
    float selectedMaskY;
    boolean busy;
    boolean appliedDark;
    ImageView imgPreview;
    ProgressBar pbPreview;
    // v1.9.3：解析进度条（新位置）
    ProgressBar pbParse;
    SeekBar seekPlayback;
    TextView tvPlayTime;
    TextView tvResolution;
    TextView tvPick;
    ProgressBar progress;
    TextView tvFps;
    TextView tvEdge;
    TextView tvQuality;
    /** v1.10.59：质量滑块下方那行小字（当前导出参数一览）。 */
    @Nullable
    TextView tvParams;
    TextView tvAlpha;
    SeekBar seekFps;
    SeekBar seekEdge;
    SeekBar seekQuality;
    SeekBar seekAlpha;
    SeekBar seekScale;
    SeekBar seekShadow;
    Button btnRotate;
    Button btnRotateShape;
    TextView tvScale;
    TextView tvShadow;
    CheckBox cbBorder;
    CheckBox cbOverlay;
    /** v1.10.252：这个组件自己要不要 1:1 高清（只对单帧图片有意义）。 */
    CheckBox cbHires;
    /** v1.10.260：这个组件自己的动图档位（0/1/2），只对 GIF/视频有意义。 */
    int animLevel = 0;
    CheckBox cbOverlayVisible;
    Button btnSave;
    /**
     * v1.10.497（用户要求 4）：这个组件**是不是桌面已导出的组件**。
     *
     * <p>判据只有一条：WidgetStore 里已经有它的配置 —— 配置只在「提交导出」那一步落盘，
     * 所以「有配置」⟺「这个组件真的在桌面上出现过」。两者对应两种按钮文案：</p>
     * <ul>
     *   <li>{@code false}（从桌面「添加组件」进来的新组件）→ 「导出组件」；</li>
     *   <li>{@code true}（从首页点「编辑」进来的已导出组件）→ 「保存配置」。</li>
     * </ul>
     * <p>除这一句文案以外，两种入口的界面与功能**完全一致**（用户要求 5）。</p>
     */
    boolean alreadyExported;
    MediaSelectorView mediaSelector;

    @Nullable
    Runnable previewTask;

    @Nullable
    Runnable paramTask;
    int previewBoxWidth;
    int previewBoxHeight;

    @Nullable
    Bitmap previewSource;

    @Nullable
    PreviewFrames.Result previewFrames;
    boolean previewFramesLoading;
    // v1.8.10：抽帧开始时间，用于「卡死自愈」（loading 永真时强制复位）
    long loadStartedAt;
    // v1.8.10：首帧上屏诊断只记一次
    boolean previewFirstLogged;
    // v1.8.14：内容/外观两个标签页各自记住滚动位置（新素材时清零回顶部）
    int scrollContent;
    int scrollAppearance;
    // v1.8.15：边解析边预览的节流时间戳
    long lastPublishMs;
    long lastTextMs;
    // v1.8.17：抽帧代次 —— 换素材即失效，旧线程的迟到发布作废
    int extractGeneration;
    // v1.8.18：上次抽帧所用的参数（变了才需要重新解析）
    int activeFps = -1;
    int activeEdge = -1;
    int activeQuality = -1;
    // v1.9.5：探测看门狗状态（2.5 秒未完成则先行开抽）
    volatile boolean probeDone;
    // v1.9.6：探测代次 —— 换素材后旧探测结果一律作废（二次导入无解析条的元凶）
    int probeGeneration;
    // v1.9.8：基本信息是否已由探测填充（否 → 解析完成时兜底填充）
    boolean infoFilled;
    // v1.10.0：帧率保留一位小数（fpsX10），帧数与「是否推算」一起展示
    int sourceFpsX10;
    int sourceFrames;
    boolean sourceFpsEstimated;
    // v1.10.0：动图帧率探测失败时的提示只弹一次
    boolean fpsHintShown;
    // v1.9.9：解析条收尾淡出任务（新解析开始时取消，防概率性消失）
    Runnable parseHideTask;
    // v1.9.8：上一帧渲染耗时 —— 播放 tick 自节流用（避免丢帧）
    volatile long previewRenderCostMs;
    // v1.10.1：任务状态（圆环/进度条只由它决定）+ 导出代次
    boolean parseTaskActive;
    int parseProgress;
    long lastProgressAt;
    int exportGeneration;
    volatile boolean exportCancelled;
    @org.jetbrains.annotations.Nullable
    WidgetConfig exportSnapshot;
    @org.jetbrains.annotations.Nullable
    Uri exportUri;
    // v1.10.26（需求 4）：这一轮导出用的确切帧率 ×10（337 = 33.7fps），
    // 不再从 config.getFps()（整数档位）反推，避免 33.7 → 34 的漂移
    int exportFpsX10;
    // v1.10.28（需求 2）：导出期间「冻结帧」的帧号 —— 切布局/蒙版仍然实时预览，
    // 但永远画这一帧，所以既看得见效果、又不会前进/倒退
    int previewFrozenIndex = -1;
    /** v1.10.54：已导出、等用户点「保存配置」才提交的结果。 */
    @Nullable
    MediaImporter.Result pendingExport;
    /**
     * v1.10.452：**一轮导出只许收尾一次**。
     *
     * <p>451 的实机现象是「导出了两遍、桌面还是没有画面」。结构上的原因不是缩略图也不是画布，
     * 而是「烘焙 + 提交」这条收尾有**两条入口**：导出完成（T22）和离开应用（T20），
     * 而 awaitBake 在"批次没起来"时还会再强制开一批。于是同一轮导出能开出 2~3 个烘焙批次、
     * 提交两次：后开的批次把前一批的代次顶掉，前一批正在读的 235 帧全记成"失败"；
     * 提交第二次时暂存目录已经被第一次搬空 —— 用户看到的就是"跑了两遍，还是没画面"。
     *
     * <p>这两个闸门是**结构**上的保证，不是超时兜底：重复触发只会留一行日志，什么都不会发生。
     */
    private final java.util.concurrent.atomic.AtomicBoolean exportFinished =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean commitDone =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean bakeStarted =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** v1.10.452：本轮的烘焙由导出链负责到底（提交后立刻烘），「导出后预热」那条老路不许再排一次。 */
    private volatile boolean bakeOwnedByExport;
    /**
     * v1.10.453（用户要求）：**当前处在"烘焙阶段"** —— 这一阶段只允许写数字「x/y」，
     * 导出那条进度条已经跑满淡出、不许再被复用（复用 = 用户嘴里的"导出两遍"）。
     */
    private volatile boolean bakeStage;
    /**
     * v1.10.454：**这一轮导出真的在写盘**（startExport → finishExport/failExport/cancel）。
     *
     * <p>它只表达一件事：帧正往 `frames.new` 里写。`save()` 用它当闸门，避免"重开一轮导出
     * 把刚写好的帧清掉"。**不能用 `busy || parseTaskActive` 代替** —— 预览解析期间它们也是 true，
     * 那样"选完素材立刻点保存"就会被静默吞掉（用户原话："怎么解析时还不能导出了"）。
     */
    private volatile boolean exportRunning;
    @org.jetbrains.annotations.Nullable
    android.widget.Toast parseToast;
    // v1.10.3：解析中的文字提示（固定在预览区底部，绝不依赖细节可见性）
    @org.jetbrains.annotations.Nullable
    TextView tvParseHint;
    int parseDone;
    int parseTotal;
    // v1.10.25：任务 UI 真正落地 —— 这三样原来「只算不贴」，
    // 导致解析条永远停在 0、tv_play_time 永远显示 0:00（用户看到的就是「没有任何动作」）
    boolean taskExporting;
    int lastLoggedPct = -1;
    int playTimeColor;
    int taskTextColor;
    boolean taskUiWasActive;
    // v1.10.2：选取回调去重 + 当前解析目标（防重复解析）
    long lastPickAt;
    @org.jetbrains.annotations.Nullable
    Uri activeUri;
    // v1.10.1：实测帧率统计（需求 2：用数据判断抽帧，不靠肉眼）
    long fpsWindowStart;
    int fpsWindowFrames;
    int fpsWindowDropped;
    // v1.10.0：预览帧解码缓存（heap/4）—— 播放时不再每帧 decodeFile
    @Nullable
    FrameCache previewFrameCache;
    /** v1.10.59：**合成结果**缓存（key 带外观参数签名）。
     *  播放时命中即 0 耗时上屏 —— 20 帧的动图第一圈跑完就再也不合成、不丢帧。 */
    @Nullable
    FrameCache previewComposedCache;
    // v1.10.0：F10 比例自检签名（同一尺寸只记一次）
    @Nullable
    volatile String aspectSignature;
    // v1.10.0：进后台前是否在播（回前台恢复）
    boolean wasPlaying;
    /** v1.10.56：这个页面现在是「用户看得见」的吗（导出收尾要不要弹框，就看它）。 */
    volatile boolean pageVisible;

    @Nullable
    Runnable refillTask;
    volatile boolean previewing;
    volatile boolean pendingPreview;
    /**
     * v1.10.76：正拖着「画面」里的参数滑块（帧率 / 清晰度 / 压缩质量）。
     * 拖动期间**不重抽**（重抽要几秒，画面会僵住），改为在内存源图上原地重组 + 换帧率重启播放，
     * 松手后才做真正的重抽对齐。
     */
    volatile boolean paramsDragging;
    volatile boolean previewDirty;
    boolean playing;
    int playIndex;

    @Nullable
    Runnable playbackRun;
    boolean pendingFirstFrame;

    @NotNull
    public static final String EXTRA_WIDGET_ID = "widget_id";
    private static final int REQUEST_PICK = 11;

    @NotNull
    private MediaKind pendingKind = MediaKind.VIDEO;

    @NotNull
    private ShapeKind selectedShape = ShapeKind.ROUNDED;

    @NotNull
    private LayoutMode selectedLayout = LayoutMode.FILL;
    private int selectedContentScale = 100;
    private boolean selectedFitContent = true;
    /**
     * v1.10.221：阴影曲线（存档串；空串 = 默认曲线）。
     *
     * <p>存**串**不存对象：合成入口（{@code FrameComposer}）本来就要串，界面上拖动时
     * 由 {@code ShadowCurveView} 直接给出对象、这里只更新串 —— 少一层来回转换。</p>
     */
    @NotNull
    private String selectedShadowCurve = "";

    @NotNull
    private final Runnable themePoll = new Runnable() { // from class: io.github.animwidget.Runnable
        @Override // java.lang.Runnable
        public void run() {
            boolean dark = ThemeManager.INSTANCE.isDark(ConfigActivity.this);
            if (dark != ConfigActivity.this.appliedDark) {
                ConfigActivity.this.appliedDark = dark;
                Morph.crossfade(ConfigActivity.this, () -> ConfigActivity.this.refreshThemeInPlace());
            }
            ConfigActivity.this.getWindow().getDecorView().postDelayed(this, 500L);
        }
    };

    /**
     * v1.10.519：回到前台后重取壁纸底图。
     *
     * <p>旧代码只在 {@code onCreate} 之后的那个 post 里取一次 —— 用户去换壁纸、返回本页，
     * {@code onResume} 什么都不做，看到的还是旧图（{@code WallpaperManager} 没有可靠的变更回调）。
     * 这里 post 一下是为了让预览区已经有位置（切哪一块要用「本 View 在屏幕上的坐标」）。
     * 代号没变时视图自己直接返回，不会白解 14MB。</p>
     */
    @NotNull
    private final Runnable deskWallpaperRefresh = new Runnable() { // from class: io.github.animwidget.Runnable
        @Override // java.lang.Runnable
        public void run() {
            ConfigActivity.this.applyDeskBackground();
        }
    };

    @NotNull
    private final int[] layoutChipIds = {R.id.ly0, R.id.ly1, R.id.ly2, R.id.ly3, R.id.ly4, R.id.ly5, R.id.ly6, R.id.ly7};
    private LayoutPickerView layoutPicker;

    @NotNull
    final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    /**
     * v1.10.499：**预取单独一条线程**。
     *
     * <p>原来 {@code prefetchPreview(index)} 就跑在 {@link #previewExecutor} 里 ——
     * 那条线程是**单线程**，预取 3 帧（真机每次 decode ≈21ms）就把下一帧的合成整整推后 60ms，
     * 50fps 只有 20ms 预算，于是 {@code P12 预览合成跟不上} 一丢一大片。
     * 预取的作用是「把未来的 decode 提前做掉」，它本来就不该占着渲染线程。</p>
     */
    private final ExecutorService previewPrefetch = Executors.newSingleThreadExecutor();

    @NotNull
    private final AtomicBoolean previewRenderBusy = new AtomicBoolean(false);

    @NotNull
    private final AtomicInteger previewGeneration = new AtomicInteger(0);

    @NotNull
    private final AtomicBoolean playbackBusy = new AtomicBoolean(false);

    /* compiled from: ConfigActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/ConfigActivity$WhenMappings.class */
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

    /* compiled from: ConfigActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u0016\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u000e\n��\n\u0002\u0010\b\b\u0086\u0003\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/ConfigActivity$Companion;", "", "()V", "EXTRA_WIDGET_ID", "", "REQUEST_PICK", ""})
    /* loaded from: classes.jar:io/github/animwidget/ConfigActivity$Companion.class */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        private Companion() {
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        Intrinsics.checkNotNullParameter(newConfig, "newConfig");
        super.onConfigurationChanged(newConfig);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        // v1.10.103（反馈）：系统切深浅色 / 配置变化 → 整页叠化，不要硬刷
        Morph.crossfade(this, () -> refreshThemeInPlace());
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        this.pageVisible = true; // v1.10.56
        // v1.10.1（需求 0）：应用自己的界面在前台 = 不受限
        DeskWatcher.setAppForeground(true);
        // v1.10.4：回到前台
        // 1) 帧还在 → 恢复预览播放（离开时只是停了播放）
        if (this.previewFrames != null && this.previewFrames.getFrames().size() > 1 && !this.playing) {
            this.ui.post(this::startPreviewFramesPlayback);
            return;
        }
        // 2) 本轮解析已被「离开应用」取消（帧被删） → 按**新一次**重新解析。
        //    门闸：探测已完成、离开选取超过 3 秒、没有在跑的任务 —— 避免选择器刚回来就重复开解析。
        if (this.previewFrames == null && this.pickedUri != null && this.pickedKind != null
                && this.probeDone && !this.previewFramesLoading && !this.parseTaskActive && !this.busy
                && System.currentTimeMillis() - this.lastPickAt > 3000L) {
            Diag.warn(this, "L28", "回前台：重新开始预览解析（上一轮已被取消）");
            this.ui.post(() -> {
                if (this.pickedUri != null && this.pickedKind != null && this.previewFrames == null
                        && !this.previewFramesLoading && !this.parseTaskActive) {
                    loadPreviewFrames(this.pickedUri, this.pickedKind);
                }
            });
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.pageVisible = false; // v1.10.56
        DeskWatcher.setAppForeground(false);
        // 需求 0 的「离开应用」= 整个 App 都不在前台了（去自己的设置页/日志页不算离开）
        if (!DeskWatcher.isAppForeground()) {
            // v1.10.56：**导出不再因为「离开应用」被取消。**
            // v1.10.1 起来这里是 cancelExportQuietly()：动图导出要几十秒到几分钟，用户点完
            // 「保存配置」顺手回桌面看组件 —— 导出被取消、暂存被删，组件一帧都没有，
            // 桌面只剩占位图（就是那张应用图标）。用户看到的就是「导出动图后没画面」。
            // 现在：导出让它在后台跑完，跑完时页面不可见 → 直接提交（帧 + 配置 + 刷新组件）。
            // 只有「自己换素材/改参数」或「主动放弃」才会丢暂存。
            if (this.pendingExport != null) {
                // v1.10.56：**已经导出完、只差人点「保存生效」的这一份，不能因为人走了就作废。**
                // v1.10.452：**只做一次提交 + 一次烘焙**，不再另起一条"边等边提交"的线程。
                // 451 里这里是第二个烘焙入口：主线程的 awaitBake 还在轮询，这边又开一批，
                // 代次一顶，前一批就在读帧途中被判"过期"，235 帧全记失败（用户："导出两遍"）。
                Diag.warn(this, "T20", "离开应用：这份导出还没收尾 → 立刻提交，提交后继续烘焙");
                final MediaImporter.Result r = this.pendingExport;
                this.bakeOwnedByExport = true;
                commitPendingExport(false);
                bakeAfterCommit(r != null ? r.getFrameCount() : 0, false);
            } else if (this.busy || this.parseTaskActive) {
                Diag.warn(this, "T20", "离开应用：导出继续在后台跑，完成即自动提交（不丢结果）");
            }
            // 预览解析照旧取消（省电，而且回前台会自动重来）
            if (this.previewFramesLoading) {
                this.extractGeneration++;
                this.previewFramesLoading = false;
                this.parseTaskActive = false;
            }
            releaseHeavyCaches();
            syncTaskUi();
            Diag.warn(this, "T07", "离开应用：预览解析已取消、缓存已释放（导出不受影响）");
        }
        super.onStop();
    }

    /** 只保留必要的配置与「屏上那一帧」，其余大对象交回给 GC。 */
    final void releaseHeavyCaches() {
        if (this.previewFrameCache != null) {
            this.previewFrameCache.clear();
        }
        if (this.previewComposedCache != null) {
            this.previewComposedCache.clear();
        }
        this.previewSource = null;
        this.lastTextMs = 0L;
    }

    /** v1.10.499：提示层显示时，返回键先收它（原来是 Dialog 自己接 OnKeyListener）。 */
    @Override // android.app.Activity
    public void onBackPressed() {
        BusyOverlay overlay = this.busyOverlay;
        if (overlay != null && overlay.handleBack()) {
            return;
        }
        // v1.10.500：输出中按返回**不能退出这一页** —— 退出会走 cancelExportQuietly，
        // 真机上表现为「导出到一半突然 0 帧、放弃保存」。这里改成与点参数同一条提示。
        if (outputRunning()) {
            noteOutputLock();
            showOutputBusyDialog();
            return;
        }
        super.onBackPressed();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        try {
            getWindow().getDecorView().removeCallbacks(this.busyOverlayPrepare);
        } catch (Throwable ignored) {
        }
        BusyOverlay overlay = this.busyOverlay;
        this.busyOverlay = null;
        if (overlay != null) {
            overlay.destroy();
        }
        super.onDestroy();
    }

    @Override // android.app.Activity
    protected void onPause() {
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        // v1.10.0：后台绝不空转（否则前台预览的 CPU 开销会一直烧到后台去）
        this.wasPlaying = this.playing;
        stopPlaybackLoop();
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        getWindow().getDecorView().post(this.themePoll);
        // v1.10.1（需求 9）：语言变了就地换文案，不重建、不丢状态、不闪
        syncLocale();
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            refreshThemeInPlace();
        }
        // v1.10.495：顶部「剩余性能空间」卡——回到前台就重算（别的组件可能刚被加/删）
        refreshHeadroom();
        // v1.10.519：壁纸底图重新对一次。用户出去换过壁纸再回来，旧代码这里什么都不做
        // → 预览区还挂着上一张（判据 B：换壁纸后不许再看到旧壁纸）。
        getWindow().getDecorView().removeCallbacks(this.deskWallpaperRefresh);
        getWindow().getDecorView().post(this.deskWallpaperRefresh);
        // v1.10.499：提示层在页面静止时准备好（建树 + 后台 PixelCopy 抓底图 + 一次 1/6 模糊）。
        // 400ms 是「布局与首屏彻底静下来」的经验值；这段时间里用户不可能点参数（页面还在进）。
        getWindow().getDecorView().postDelayed(this.busyOverlayPrepare, 400L);
        // v1.10.0：回到前台恢复播放
        if (this.wasPlaying) {
            this.wasPlaying = false;
            if (this.previewFrames != null) {
                startPreviewFramesPlayback();
            } else {
                startPlayback(false);
            }
        }
    }

    /**
     * 顶部常驻的「剩余性能空间」卡（v1.10.495 起有，v1.10.496 改成卡片）。
     *
     * <p>需求要求它**实时跟随系统模式并同步更新**：换深浅色时这张卡会被
     * {@code refreshThemeInPlace} 整棵重染（tag="card" / "section" / "chip_value" / "muted"），
     * 百分比与分段表则由本方法每次按 {@link ThemeColors} 现下发；文案在 onResume、每次换肤、
     * 以及换语言（{@code onTextsRefreshed}）里重算。所以切到后台再回来、系统切主题、切语言、
     * 加了新组件，看到的都是当下的数。</p>
     *
     * <p>v1.10.496：上屏目标从「一行 TextView」换成「整张卡的根视图」——
     * 卡片位置也从预览区下方搬到了 ScrollView 顶部（见 {@code activity_config.xml} 的注释）。</p>
     */
    private void refreshHeadroom() {
        try {
            View card = findViewById(R.id.ll_perf_headroom);
            if (card != null) {
                PerfBudget.refresh(this, card, this.widgetId);
            }
        } catch (Throwable ignored) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshThemeInPlace() {
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        themeManager.applyTo(viewFindViewById);
        ThemeManager.INSTANCE.applySystemBars(this);
        refreshLayoutChips(this.layoutChipIds);
        if (this.layoutPicker != null) {
            // v1.8.5：切黑白模式时布局卡片要立刻跟着换色
            this.layoutPicker.applyTheme();
        }
        buildShapeGrid();
        // v1.10.496：换肤时整张「剩余性能」卡重算+重染（卡片结构由 applyTo 的 tag 负责，
        // 百分比/分段表的强调色只有本方法会下发 —— 它必须是 applyTo 之后才跑）
        refreshHeadroom();
        // v1.9.3：主题/黑白切换时重染解析动画与解析条
        applyProgressTheme();
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        MediaSelectorView mediaSelectorView = this.mediaSelector;
        if (mediaSelectorView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
            mediaSelectorView = null;
        }
        mediaSelectorView.applyColors(c.getPrimary(), c.getNeutral(), c.getText(), c.getOnPrimary());
        MediaSelectorView mediaSelectorView2 = this.mediaSelector;
        if (mediaSelectorView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
            mediaSelectorView2 = null;
        }
        String string = getString(R.string.cfg_video);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        String string2 = getString(R.string.cfg_gif);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        String string3 = getString(R.string.cfg_image);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        mediaSelectorView2.setLabels(new String[]{string, string2, string3});
        boolean content = findViewById(R.id.ll_content).getVisibility() == 0;
        applyTabColors(content);
    }

    @Override // android.app.Activity
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // v1.8.10：从桌面「添加小部件」会直达本页，MainActivity 从未运行，
        // 崩溃钩子没装 → 任何异常都不会留 last_error.txt。这里补装（幂等）。
        CrashLog.install(this);
        setContentView(R.layout.activity_config);
        setI18nLayout(R.layout.activity_config);
        EdgeToEdge.INSTANCE.apply(this);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        try {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.preferredRefreshRate = 0.0f;
            getWindow().setAttributes(attrs);
        } catch (Throwable th) {
        }
        int fromPin = getIntent().getIntExtra("appWidgetId", 0);
        int editId = getIntent().getIntExtra(EXTRA_WIDGET_ID, 0);
        this.widgetId = fromPin != 0 ? fromPin : editId;
        if (this.widgetId == 0) {
            finish();
            return;
        }
        if (fromPin != 0) {
            setResult(0);
        }
        // C01：确认配置页真的起来了（诊断链路的起点）
        Diag.warn(this, "C01", "配置页启动 widgetId=" + this.widgetId + (fromPin != 0 ? " 新建" : " 编辑"));
        WidgetConfig widgetConfigLoad = WidgetStore.INSTANCE.load(this, this.widgetId);
        // v1.10.497（用户要求 4）：「有配置」= 这个组件真的在桌面上导出过（配置只在提交那一刻落盘）。
        // 它只决定主按钮写「导出组件」还是「保存配置」，别的一概不区分。
        this.alreadyExported = widgetConfigLoad != null;
        if (widgetConfigLoad == null) {
            // v1.8.7：新建组件的默认布局改成「原始比例」
            widgetConfigLoad = new WidgetConfig(this.widgetId, MediaKind.VIDEO, "", "", 12, 720, 90, 0, 0, 0,
                    ShapeKind.ROUNDED, 100, false, LayoutMode.FIT, false, false, 0, 0, 0, 100, 0, 0, 0, true,
                    0.0f, 0.0f, 100, 0.0f, 0.0f, "");
        }
        WidgetConfig loaded = widgetConfigLoad;
        this.base = loaded;
        // v1.8.2：旧版本的整数除法 bug 曾把素材帧导成 1×1，检测到就提醒重选
        if (loaded.getFrameCount() > 0 && (loaded.getFrameWidth() <= 2 || loaded.getFrameHeight() <= 2)) {
            this.ui.postDelayed(() -> {
                String broken = ConfigActivity.this.getString(R.string.cfg_frames_broken);
                Intrinsics.checkNotNullExpressionValue(broken, "getString(...)");
                ConfigActivity.this.toast(broken);
            }, 800L);
        }
        this.selectedShape = loaded.getShape();
        this.selectedLayout = loaded.getLayout();
        this.selectedRotation = loaded.getRotation();
        this.selectedShapeRotation = loaded.getShapeRotation();
        this.selectedOffsetX = loaded.getOffsetXPercent();
        this.selectedOffsetY = loaded.getOffsetYPercent();
        this.selectedMaskX = loaded.getMaskOffsetXPercent();
        this.selectedMaskY = loaded.getMaskOffsetYPercent();
        this.selectedContentScale = loaded.getContentScalePercent();
        this.selectedShadowCurve = loaded.getShadowCurve();
        this.selectedFitContent = loaded.getFitContent();
        if (loaded.getFrameWidth() > 0 && loaded.getFrameHeight() > 0) {
            this.sourceWidth = loaded.getFrameWidth();
            this.sourceHeight = loaded.getFrameHeight();
        }
        View viewFindViewById = findViewById(R.id.img_preview);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        this.imgPreview = (ImageView) viewFindViewById;
        View viewFindViewById2 = findViewById(R.id.pb_preview);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
        this.pbPreview = (ProgressBar) viewFindViewById2;
        // v1.9.3：解析条（预览区下方、播放进度条上方一行，主系统配色）
        View viewParse = findViewById(R.id.pb_parse);
        Intrinsics.checkNotNullExpressionValue(viewParse, "findViewById(...)");
        this.pbParse = (ProgressBar) viewParse;
        View viewParseHint = findViewById(R.id.tv_parse_hint);
        if (viewParseHint instanceof TextView) {
            this.tvParseHint = (TextView) viewParseHint;
            // 高对比：主色药丸 + 白字，保证在任意壁纸/主题下都看得见
            this.tvParseHint.setTextColor(0xFFFFFFFF);
        }
        View viewFindViewById3 = findViewById(R.id.seek_playback);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
        this.seekPlayback = (SeekBar) viewFindViewById3;
        // v1.10.26（需求 3）：播放进度条只做「进度显示器」，永久不可拖动 ——
        // 它从小部件播放时钟推导，手动拖动没有任何意义（拖了也会被下一帧覆盖）
        SeekBar playbackSeek = this.seekPlayback;
        playbackSeek.setEnabled(false);
        playbackSeek.setFocusable(false);
        playbackSeek.setClickable(false);
        playbackSeek.setLongClickable(false);
        playbackSeek.setOnTouchListener((v, event) -> true);
        View viewFindViewById4 = findViewById(R.id.tv_play_time);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById4, "findViewById(...)");
        this.tvPlayTime = (TextView) viewFindViewById4;
        TextView textView = this.tvPlayTime;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvPlayTime");
            textView = null;
        }
        textView.setText(formatTime(0L));
        View viewFindViewById5 = findViewById(R.id.tv_resolution);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById5, "findViewById(...)");
        this.tvResolution = (TextView) viewFindViewById5;
        View viewFindViewById6 = findViewById(R.id.tv_pick);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById6, "findViewById(...)");
        this.tvPick = (TextView) viewFindViewById6;
        View viewFindViewById7 = findViewById(R.id.pb_preview);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById7, "findViewById(...)");
        this.progress = (ProgressBar) viewFindViewById7;
        View viewFindViewById8 = findViewById(R.id.tv_fps);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById8, "findViewById(...)");
        this.tvFps = (TextView) viewFindViewById8;
        View viewFindViewById9 = findViewById(R.id.tv_edge);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById9, "findViewById(...)");
        this.tvEdge = (TextView) viewFindViewById9;
        View viewFindViewById10 = findViewById(R.id.tv_quality);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById10, "findViewById(...)");
        this.tvQuality = (TextView) viewFindViewById10;
        // v1.10.59：参数小字（质量滑块下面）
        try {
            View paramLine = findViewById(R.id.tv_params);
            if (paramLine instanceof TextView) {
                this.tvParams = (TextView) paramLine;
            }
        } catch (Throwable ignored) {
        }
        View viewFindViewById11 = findViewById(R.id.tv_alpha);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById11, "findViewById(...)");
        this.tvAlpha = (TextView) viewFindViewById11;
        View viewFindViewById12 = findViewById(R.id.seek_fps);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById12, "findViewById(...)");
        this.seekFps = (SeekBar) viewFindViewById12;
        View viewFindViewById13 = findViewById(R.id.seek_edge);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById13, "findViewById(...)");
        this.seekEdge = (SeekBar) viewFindViewById13;
        View viewFindViewById14 = findViewById(R.id.seek_quality);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById14, "findViewById(...)");
        this.seekQuality = (SeekBar) viewFindViewById14;
        View viewFindViewById15 = findViewById(R.id.seek_alpha);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById15, "findViewById(...)");
        this.seekAlpha = (SeekBar) viewFindViewById15;
        View viewFindViewById16 = findViewById(R.id.seek_scale);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById16, "findViewById(...)");
        this.seekScale = (SeekBar) viewFindViewById16;
        View viewFindViewById17 = findViewById(R.id.seek_shadow);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById17, "findViewById(...)");
        this.seekShadow = (SeekBar) viewFindViewById17;
        View viewFindViewById18 = findViewById(R.id.btn_rotate);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById18, "findViewById(...)");
        this.btnRotate = (Button) viewFindViewById18;
        View viewFindViewById19 = findViewById(R.id.btn_rotate_shape);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById19, "findViewById(...)");
        this.btnRotateShape = (Button) viewFindViewById19;
        View viewFindViewById20 = findViewById(R.id.tv_scale);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById20, "findViewById(...)");
        this.tvScale = (TextView) viewFindViewById20;
        View viewFindViewById21 = findViewById(R.id.tv_shadow);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById21, "findViewById(...)");
        this.tvShadow = (TextView) viewFindViewById21;
        View viewFindViewById22 = findViewById(R.id.cb_border);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById22, "findViewById(...)");
        this.cbBorder = (CheckBox) viewFindViewById22;
        View viewFindViewById23 = findViewById(R.id.cb_overlay);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById23, "findViewById(...)");
        this.cbOverlay = (CheckBox) viewFindViewById23;
        View viewFindViewById24 = findViewById(R.id.cb_overlay_visible);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById24, "findViewById(...)");
        this.cbOverlayVisible = (CheckBox) viewFindViewById24;
        // v1.10.252：每个组件自己的「高清 1:1」勾选（默认跟随设置里的全局默认）
        View viewHires = findViewById(R.id.cb_hires);
        if (viewHires instanceof CheckBox) {
            this.cbHires = (CheckBox) viewHires;
            this.cbHires.setChecked(WidgetRender.hiResWidget(this, this.widgetId,
                    WidgetRender.hiResStaticPref(this)));
        }
        // v1.10.260：这个组件自己的动图档位（0/1/2），默认跟随设置里的全局默认
        this.animLevel = WidgetRender.animQualityWidget(this, this.widgetId,
                WidgetRender.animQuality(this));
        bindAnimQuality(R.id.aq0, 0);
        bindAnimQuality(R.id.aq1, 1);
        bindAnimQuality(R.id.aq2, 2);
        applyAnimQualityUi(this.animLevel);
        View viewFindViewById25 = findViewById(R.id.btn_save);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById25, "findViewById(...)");
        this.btnSave = (Button) viewFindViewById25;
        // v1.10.497（用户要求 4）：主按钮文案两态。两种入口的**其余界面与功能完全一致**。
        refreshSaveButtonText();
        ((Button) findViewById(R.id.btn_adapt)).setOnClickListener((v1) -> {
            onCreate$lambda$0(ConfigActivity.this, v1);
        });
        View viewFindViewById26 = findViewById(R.id.media_selector);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById26, "findViewById(...)");
        this.mediaSelector = (MediaSelectorView) viewFindViewById26;
        MediaSelectorView mediaSelectorView = this.mediaSelector;
        if (mediaSelectorView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
            mediaSelectorView = null;
        }
        mediaSelectorView.setOnKindClick(new AnonymousClass2());
        View maskButtonView = findViewById(R.id.btn_mask_gesture);
        if (maskButtonView != null) {
            maskButtonView.setOnClickListener((v2) -> {
                ConfigActivity.this.openMaskGestureEditor();
            });
        }
        ((Button) findViewById(R.id.btn_overlay_edit)).setOnClickListener((v1) -> {
            onCreate$lambda$1(ConfigActivity.this, v1);
        });
        // v1.10.221：阴影曲线面板（在下面所有滑块初始化完再建，见 applyKindUi 之前）
        Button button = this.btnSave;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnSave");
            button = null;
        }
        button.setOnClickListener((v1) -> {
            try {
                onCreate$lambda$2(ConfigActivity.this, v1);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        SeekBar seekBar = this.seekFps;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            seekBar = null;
        }
        seekBar.setProgress(RangesKt.coerceIn(loaded.getFps(), 1, 50) - 1);
        SeekBar seekBar2 = this.seekEdge;
        if (seekBar2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekEdge");
            seekBar2 = null;
        }
        seekBar2.setProgress(RangesKt.coerceIn((RangesKt.coerceIn(loaded.getMaxEdge(), 120, 2560) - 120) / 8, 0, 305));
        SeekBar seekBar3 = this.seekQuality;
        if (seekBar3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar3 = null;
        }
        seekBar3.setProgress(RangesKt.coerceIn(loaded.getQuality(), 50, 100) - 50);
        SeekBar seekBar4 = this.seekAlpha;
        if (seekBar4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
            seekBar4 = null;
        }
        seekBar4.setProgress(RangesKt.coerceIn(loaded.getShapeAlpha(), 0, 100));
        SeekBar seekBar5 = this.seekScale;
        if (seekBar5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekScale");
            seekBar5 = null;
        }
        // v1.10.75：外观页只允许**缩小**（-100 ~ 0），progress 0..100 对应 -100~0
        seekBar5.setProgress(RangesKt.coerceIn(loaded.getScalePercent(), 0, 100));
        SeekBar seekBar6 = this.seekShadow;
        if (seekBar6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
            seekBar6 = null;
        }
        seekBar6.setProgress(RangesKt.coerceIn(loaded.getShadowPercent(), 0, ShadowSpec.STRENGTH_MAX));
        CheckBox checkBox = this.cbBorder;
        if (checkBox == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
            checkBox = null;
        }
        checkBox.setChecked(loaded.getBorder());
        CheckBox checkBox2 = this.cbOverlay;
        if (checkBox2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbOverlay");
            checkBox2 = null;
        }
        checkBox2.setChecked(loaded.getOverlayEnabled());
        CheckBox checkBox3 = this.cbOverlayVisible;
        if (checkBox3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbOverlayVisible");
            checkBox3 = null;
        }
        checkBox3.setChecked(loaded.getOverlayVisible());
        // v1.10.14：「显示覆盖层」勾上必须同时勾「流畅覆盖层」，
        // 否则 overlayEnabled=false，覆盖层根本不会创建（用户就是这么被坑的）
        checkBox3.setOnCheckedChangeListener((cb, checked) -> {
            if (checked && this.cbOverlay != null && !this.cbOverlay.isChecked()) {
                this.cbOverlay.setChecked(true);
            }
        });
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity$onCreate$listener$1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar7, int progressValue, boolean fromUser) {
                ConfigActivity.this.updateLabels();
                boolean z = seekBar7 != null && seekBar7.getId() == 2131230759;
                if (z) {
                    ConfigActivity.this.schedulePreview();
                } else if (fromUser) {
                    // v1.10.76：拖动中先**原地实时重渲**（毫秒级），松手后才真正重抽
                    if (ConfigActivity.this.paramsDragging) {
                        ConfigActivity.this.liveParamsPreview();
                    } else {
                        ConfigActivity.this.onRefillParamChanged();
                    }
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar7) {
                boolean z = seekBar7 != null && seekBar7.getId() == 2131230759;
                if (!z) {
                    ConfigActivity.this.paramsDragging = true;
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar7) {
                boolean z = seekBar7 != null && seekBar7.getId() == 2131230759;
                if (z) {
                    ConfigActivity.this.schedulePreview();
                } else {
                    // 松手：结束实时状态，按最终参数做一次真正的重抽
                    ConfigActivity.this.paramsDragging = false;
                    ConfigActivity.this.onRefillParamChanged();
                }
            }
        };
        SeekBar seekBar7 = this.seekFps;
        if (seekBar7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            seekBar7 = null;
        }
        seekBar7.setOnSeekBarChangeListener(onSeekBarChangeListener);
        SeekBar seekBar8 = this.seekEdge;
        if (seekBar8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekEdge");
            seekBar8 = null;
        }
        seekBar8.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.onCreate.5
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar9, int value, boolean fromUser) {
                ConfigActivity.this.updateLabels();
                if (fromUser) {
                    // v1.10.76：清晰度拖动中 → 直接在内存图上按新边长重组（能立刻看出清晰度变化）
                    if (ConfigActivity.this.paramsDragging) {
                        ConfigActivity.this.liveParamsPreview();
                    } else {
                        ConfigActivity.this.onRefillParamChanged();
                    }
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar9) {
                ConfigActivity.this.paramsDragging = true;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar9) {
                ConfigActivity.this.paramsDragging = false;
                ConfigActivity.this.onRefillParamChanged();
            }
        });
        SeekBar seekBar9 = this.seekQuality;
        if (seekBar9 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar9 = null;
        }
        seekBar9.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.onCreate.6
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar10, int value, boolean fromUser) {
                // v1.10.76：质量只作用于**导出**（预览帧本身是无损的），拖动时没有任何可重抽的东西 ——
                // 以前每动一格就触发一次重抽，纯属白等。现在只刷新文字和小字参数行。
                ConfigActivity.this.updateLabels();
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar10) {
                ConfigActivity.this.paramsDragging = true;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar10) {
                ConfigActivity.this.paramsDragging = false;
            }
        });
        SeekBar seekBar10 = this.seekAlpha;
        if (seekBar10 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
            seekBar10 = null;
        }
        seekBar10.setOnSeekBarChangeListener(onSeekBarChangeListener);
        SeekBar seekBar11 = this.seekScale;
        if (seekBar11 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekScale");
            seekBar11 = null;
        }
        seekBar11.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.onCreate.7
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar12, int value, boolean fromUser) {
                ConfigActivity.this.updateLabels();
                ConfigActivity.this.refreshPreviewNow();
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar12) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar12) {
            }
        });
        SeekBar seekBar12 = this.seekShadow;
        if (seekBar12 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
            seekBar12 = null;
        }
        seekBar12.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.onCreate.8
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar13, int value, boolean fromUser) {
                ConfigActivity.this.updateLabels();
                // v1.10.230：预设动画/程序写回期间，滑块只动、不提交不重算（否则每帧合并一次预览）
                if (ConfigActivity.this.curveSyncing) {
                    return;
                }
                // v1.10.222：主滑块与 6 个曲线滑块走**同一个**渲染入口（签名判重 + refreshPreviewNow）。
                // 合成时读的是 this.selectedShadowCurve，所以拖动主滑块用的就是当前的自定义曲线，
                // 不会回到默认值、松手也不会跳变。
                ConfigActivity.this.renderShadowParams();
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar13) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar13) {
            }
        });
        TextView textView2 = this.tvScale;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvScale");
            textView2 = null;
        }
        textView2.setOnClickListener((v1) -> {
            onCreate$lambda$3(ConfigActivity.this, v1);
        });
        TextView textView3 = this.tvShadow;
        if (textView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvShadow");
            textView3 = null;
        }
        textView3.setOnClickListener((v1) -> {
            onCreate$lambda$4(ConfigActivity.this, v1);
        });
        CheckBox checkBox4 = this.cbBorder;
        if (checkBox4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
            checkBox4 = null;
        }
        checkBox4.setOnCheckedChangeListener((v1, v2) -> {
            onCreate$lambda$5(ConfigActivity.this, v1, v2);
        });
        updateRotateButtons();
        refreshFitButton();
        ((Button) findViewById(R.id.btn_fit_content)).setOnClickListener((v1) -> {
            onCreate$lambda$6(ConfigActivity.this, v1);
        });
        Button button2 = this.btnRotate;
        if (button2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRotate");
            button2 = null;
        }
        button2.setOnClickListener((v1) -> {
            onCreate$lambda$7(ConfigActivity.this, v1);
        });
        Button button3 = this.btnRotateShape;
        if (button3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRotateShape");
            button3 = null;
        }
        button3.setOnClickListener((v1) -> {
            onCreate$lambda$8(ConfigActivity.this, v1);
        });
        ((Button) findViewById(R.id.btn_shapes_toggle)).setOnClickListener((v1) -> {
            onCreate$lambda$9(ConfigActivity.this, v1);
        });
        buildShapeGrid();
        View pickerHost = findViewById(R.id.ly_scroll);
        if (!(pickerHost instanceof LayoutPickerView)) {
            pickerHost = findPicker((android.view.ViewGroup) findViewById(android.R.id.content));
        }
        if (pickerHost instanceof LayoutPickerView) {
            this.layoutPicker = (LayoutPickerView) pickerHost;
            this.layoutPicker.setWidgetRatio(widgetAspect());
            this.layoutPicker.setMediaRatio(mediaRatio());
            this.layoutPicker.setMediaExactRatio(mediaOnlyRatio());
            this.layoutPicker.setSelected(this.selectedLayout);
            this.layoutPicker.setListener(new LayoutPickerView.Listener() {
                /** v1.10.500：**改自身选中态之前**先判一次（否则高亮与 chip 行会先切过去）。 */
                @Override
                public boolean blockPick(LayoutMode mode) {
                    if (!outputRunning()) {
                        return false;
                    }
                    noteOutputLock();
                    showOutputBusyDialog();
                    return true;
                }

                @Override
                public void onPick(LayoutMode mode) {
                    // v1.10.497（用户：导出时仍然可以切换布局）：**动作层兜底**。
                    // 触摸层已经按「松手才算点击」拦过一次，但布局卡片是自绘 View、自己实现
                    // onTouchEvent + performClick，任何一格漏过去都会直接改布局 —— 这里再判一次，
                    // 命中就只弹「当前方案正在输出」，selectedLayout 一个字节都不动。
                    if (outputRunning()) {
                        noteOutputLock();
                        showOutputBusyDialog();
                        return;
                    }
                    ConfigActivity.this.selectedLayout = mode;
                    ConfigActivity.this.previewDirty = true;
                    ConfigActivity.this.refreshFitButton();
                    ConfigActivity.this.updatePreviewBox();
                    ConfigActivity.this.refreshPreviewNow();
                    ConfigActivity.this.schedulePreview();
                }
            });
        }
        if ((loaded.getSourceUri().length() > 0) && loaded.getKind() != MediaKind.IMAGE) {
            Uri sourceUri = Uri.parse(loaded.getSourceUri());
            new Thread(() -> {
                try {
                    onCreate$lambda$15(ConfigActivity.this, sourceUri, loaded);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        if (loaded.getSourceName().length() > 0) {
            TextView textView4 = this.tvPick;
            if (textView4 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvPick");
                textView4 = null;
            }
            textView4.setText(getString(R.string.cfg_picked, new Object[]{loaded.getSourceName()}));
            if (loaded.getFrameWidth() > 0 && loaded.getFrameHeight() > 0) {
                TextView textView5 = this.tvResolution;
                if (textView5 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("tvResolution");
                    textView5 = null;
                }
                textView5.setText(getString(R.string.cfg_resolution, new Object[]{new StringBuilder().append(loaded.getFrameWidth()).append('x').append(loaded.getFrameHeight()).toString()}));
            }
        }
        // v1.10.222：曲线面板必须在**所有滑块的初值和监听都挂好之后**再建 ——
        // 否则它拿到的「当前强度/当前曲线」还是 XML 默认值（0 / 默认曲线），
        // 面板一展开就显示错的数值，灰置状态也会停在错的那一档。
        setupShadowCurvePanel();
        applyKindUi(loaded.getKind());
        updateLabels();
        renderPreviewNow();
        ((FrameLayout) findViewById(R.id.preview_frame)).addOnLayoutChangeListener((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> {
            onCreate$lambda$16(ConfigActivity.this, v1, v2, v3, v4, v5, v6, v7, v8, v9);
        });
        SeekBar seekBar13 = this.seekPlayback;
        if (seekBar13 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
            seekBar13 = null;
        }
        seekBar13.setMax(RangesKt.coerceAtLeast(loaded.getFrameCount() - 1, 0));
        SeekBar seekBar14 = this.seekPlayback;
        if (seekBar14 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
            seekBar14 = null;
        }
        seekBar14.setEnabled(loaded.getFrameCount() > 1);
        SeekBar seekBar15 = this.seekPlayback;
        if (seekBar15 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
            seekBar15 = null;
        }
        seekBar15.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.onCreate.20
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar16, int value, boolean fromUser) {
                if (fromUser) {
                    ConfigActivity.this.playIndex = value;
                    ConfigActivity.this.updatePlayTime(value);
                    ConfigActivity.this.renderPreviewFrameAt(value);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar16) {
                ConfigActivity.this.stopPlaybackLoop();
            }

            /* JADX WARN: Removed duplicated region for block: B:7:0x001b  */
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void onStopTrackingTouch(@org.jetbrains.annotations.Nullable android.widget.SeekBar r6) {
                /*
                    r5 = this;
                    r0 = r5
                    io.github.animwidget.ConfigActivity r0 = io.github.animwidget.ConfigActivity.this
                    io.github.animwidget.PreviewFrames$Result r0 = io.github.animwidget.ConfigActivity.access$getPreviewFrames$p(r0)
                    r1 = r0
                    if (r1 == 0) goto L1b
                    java.util.List r0 = r0.getFrames()
                    r1 = r0
                    if (r1 == 0) goto L1b
                    int r0 = r0.size()
                    goto L1d
                L1b:
                    r0 = 0
                L1d:
                    r1 = 1
                    if (r0 <= r1) goto L2b
                    r0 = r5
                    io.github.animwidget.ConfigActivity r0 = io.github.animwidget.ConfigActivity.this
                    io.github.animwidget.ConfigActivity.access$startPreviewFramesPlayback(r0)
                    goto L4c
                L2b:
                    r0 = r5
                    io.github.animwidget.ConfigActivity r0 = io.github.animwidget.ConfigActivity.this
                    io.github.animwidget.WidgetConfig r0 = io.github.animwidget.ConfigActivity.access$getBase$p(r0)
                    r1 = r0
                    if (r1 == 0) goto L3c
                    int r0 = r0.getFrameCount()
                    goto L3e
                L3c:
                    r0 = 0
                L3e:
                    r1 = 1
                    if (r0 <= r1) goto L4c
                    r0 = r5
                    io.github.animwidget.ConfigActivity r0 = io.github.animwidget.ConfigActivity.this
                    r1 = 0
                    r2 = 1
                    r3 = 0
                    io.github.animwidget.ConfigActivity.startPlayback$default(r0, r1, r2, r3)
                L4c:
                    return
                */
                int count = 0;
                PreviewFrames.Result frames = ConfigActivity.this.previewFrames;
                if (frames != null && frames.getFrames() != null) {
                    count = frames.getFrames().size();
                }
                if (count > 1) {
                    ConfigActivity.this.startPreviewFramesPlayback();
                } else if (ConfigActivity.this.pickedUri == null) {
                    // 没有未保存的新素材时，才允许回退到已保存的旧帧
                    WidgetConfig config = ConfigActivity.this.base;
                    if (config != null && config.getFrameCount() > 1) {
                        ConfigActivity.startPlayback$default(ConfigActivity.this, false, 1, null);
                    }
                } else {
                    // L10：新素材只有 ≤1 帧 —— 绝不回退旧素材（v1.8.14）
                    Diag.warn(ConfigActivity.this, "L10", "新素材只有 " + count + " 帧，无法播放(不回退旧素材)");
                }
            }
        });
        if (loaded.getFrameCount() > 1) {
            this.pendingFirstFrame = true;
            this.ui.postDelayed(() -> {
                onCreate$lambda$17(ConfigActivity.this);
            }, 900L);
        }
        ((FrameLayout) findViewById(R.id.preview_frame)).post(() -> {
            onCreate$lambda$18(ConfigActivity.this);
        });
        TextView tabContent = (TextView) findViewById(R.id.tab_content);
        TextView tabAppearance = (TextView) findViewById(R.id.tab_appearance);
        LinearLayout llContent = (LinearLayout) findViewById(R.id.ll_content);
        LinearLayout llAppearance = (LinearLayout) findViewById(R.id.ll_appearance);
        View indicator = findViewById(R.id.tab_indicator);
        applyTabColors(true);
        indicator.post(() -> {
            onCreate$lambda$20(ConfigActivity.this, indicator);
        });
        tabContent.setOnClickListener((v4) -> {
            onCreate$lambda$22(llContent, llAppearance, ConfigActivity.this, indicator, v4);
        });
        tabAppearance.setOnClickListener((v4) -> {
            onCreate$lambda$23(llContent, llAppearance, ConfigActivity.this, indicator, v4);
        });
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById27 = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById27, "findViewById(...)");
        themeManager.applyTo(viewFindViewById27);
        ThemeManager.INSTANCE.applySystemBars(this);
        onCreate$selectTab$default(llContent, llAppearance, this, indicator, true, false, 32, null);
        // v1.8.6：onCreate 收尾再同步一次——之前出现过「打开编辑页，布局卡片高亮
        // 与真实布局不一致」的情况，这里以 selectedLayout 为准强制对齐。
        refreshLayoutChips(this.layoutChipIds);
        refreshFitButton();
        applyProgressTheme();
        // v1.10.190：全应用锁竖屏（清单里 9 个 Activity 都是 screenOrientation="portrait"），
        // 转屏不再重建本页 —— 素材/参数不会再因为旋转而丢，v1.10.189 那套
        // onSaveInstanceState 素材恢复已随本版撤掉（用户要求：与其绕，不如不许转）。
        if (fromPin != 0) {
            this.ui.postDelayed(() -> {
                String hintPin = ConfigActivity.this.getString(R.string.cfg_pin_hint);
                Intrinsics.checkNotNullExpressionValue(hintPin, "getString(...)");
                ConfigActivity.this.toast(hintPin);
            }, 600L);
        }
    }

    private static final void onCreate$lambda$0(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.applyAutoParams();
        // v1.9.9：一键适配后参数若与在抽/已抽参数不同 → 走同一套重解析流程（进度条归 0 等）
        this$0.onRefillParamChanged();
    }

    /* compiled from: ConfigActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u000e\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004"}, d2 = {"<anonymous>", "", "kind", "Lio/github/animwidget/MediaKind;", "invoke"})
    /* renamed from: io.github.animwidget.ConfigActivity$onCreate$2, reason: invalid class name */
    /* loaded from: classes.jar:io/github/animwidget/ConfigActivity$onCreate$2.class */
    final class AnonymousClass2 implements Function1<MediaKind, Unit> {
        AnonymousClass2() {
        }

        public final Unit invoke(@NotNull MediaKind kind) throws NoWhenBranchMatchedException {
            Intrinsics.checkNotNullParameter(kind, "kind");
            ConfigActivity.this.pick(kind);
            return Unit.INSTANCE;
        }
    }

    private static final void onCreate$lambda$1(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.openContentGestureEditor();
    }

    private static final void onCreate$lambda$2(ConfigActivity this$0, View it) throws JSONException {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.save();
    }

    private static final void onCreate$lambda$3(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String string = this$0.getString(R.string.cfg_scale_dialog);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        SeekBar seekBar = this$0.seekScale;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekScale");
            seekBar = null;
        }
        this$0.showNumberDialog(string, seekBar.getProgress() - 100, -100, 0, new ConfigActivity$onCreate$9$1(this$0));
    }

    private static final void onCreate$lambda$4(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String string = this$0.getString(R.string.cfg_shadow_dialog);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        SeekBar seekBar = this$0.seekShadow;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
            seekBar = null;
        }
        this$0.showNumberDialog(string, seekBar.getProgress(), 0, ShadowSpec.STRENGTH_MAX, new ConfigActivity$onCreate$10$1(this$0));
    }

    private static final void onCreate$lambda$5(ConfigActivity this$0, CompoundButton compoundButton, boolean z) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.schedulePreview();
    }

    private static final void onCreate$lambda$6(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.fitContentLocked()) {
            return;
        }
        this$0.selectedFitContent = !this$0.selectedFitContent;
        this$0.refreshFitButton();
        this$0.refreshPreviewNow();
    }

    private static final void onCreate$lambda$7(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.showRotateDialog(false);
    }

    private static final void onCreate$lambda$8(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.showRotateDialog(true);
    }

    private static final void onCreate$lambda$9(ConfigActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.shapesExpanded = !this$0.shapesExpanded;
        ((Button) this$0.findViewById(R.id.btn_shapes_toggle)).setText(this$0.shapesExpanded ? this$0.getString(R.string.cfg_collapse_shapes) : this$0.getString(R.string.cfg_expand_shapes));
        this$0.toggleShapeGridAnimated();
    }

    /**
     * 形状格「收起 / 展开」的曲线动画：高度插值 + 淡入，Material 标准缓动曲线
     * (0.2, 0, 0, 1)。宽度还没量到（首次布局前）时退化为直接显示。
     */
    private final void toggleShapeGridAnimated() {
        final LinearLayout container = (LinearLayout) findViewById(R.id.ll_shapes);
        final int from = container != null ? container.getHeight() : 0;
        buildShapeGrid();
        if (container == null) {
            return;
        }
        int width = container.getWidth();
        if (from <= 0 || width <= 0) {
            return;
        }
        container.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
        final int to = container.getMeasuredHeight();
        if (to <= 0 || to == from) {
            return;
        }
        ViewGroup.LayoutParams params = container.getLayoutParams();
        if (params == null) {
            return;
        }
        params.height = from;
        container.setLayoutParams(params);

        final android.view.animation.PathInterpolator easing =
                new android.view.animation.PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(from, to);
        animator.setDuration(260L);
        animator.setInterpolator(easing);
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                ViewGroup.LayoutParams lp = container.getLayoutParams();
                if (lp != null) {
                    lp.height = ((Integer) animation.getAnimatedValue()).intValue();
                    container.setLayoutParams(lp);
                }
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup.LayoutParams lp = container.getLayoutParams();
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    container.setLayoutParams(lp);
                }
            }
        });
        animator.start();
    }

    private static final void onCreate$lambda$11$lambda$10(ConfigActivity this$0, LayoutMode $mode, int[] $layoutChips, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($mode, "$mode");
        Intrinsics.checkNotNullParameter($layoutChips, "$layoutChips");
        this$0.selectedLayout = $mode;
        this$0.updatePreviewBox();
        this$0.refreshLayoutChips($layoutChips);
        this$0.refreshFitButton();
        this$0.refreshPreviewNow();
    }

    private static final void onCreate$lambda$15(ConfigActivity this$0, Uri $sourceUri, WidgetConfig $loaded) throws IOException {
        Integer numVideo;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($loaded, "$loaded");
        Intrinsics.checkNotNull($sourceUri);
        String resolution = SourceFps.INSTANCE.resolution(this$0, $sourceUri, $loaded.getKind());
        if ($loaded.getKind() == MediaKind.GIF) {
            numVideo = SourceFps.INSTANCE.gif(this$0, $sourceUri);
        } else {
            numVideo = SourceFps.INSTANCE.video(this$0, $sourceUri);
        }
        Integer fps = numVideo;
        Long duration = SourceFps.INSTANCE.duration(this$0, $sourceUri, $loaded.getKind());
        this$0.ui.post(() -> {
            onCreate$lambda$15$lambda$14(resolution, this$0, fps, duration);
        });
    }

    private static final void onCreate$lambda$15$lambda$14(String $resolution, ConfigActivity this$0, Integer $fps, Long $duration) {
        List parts;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if ($resolution != null && (parts = StringsKt.split($resolution, new String[]{"x"}, false, 0)) != null && parts.size() == 2) {
            Integer intOrNull = StringsKt.toIntOrNull((String) parts.get(0));
            this$0.sourceWidth = intOrNull != null ? intOrNull.intValue() : this$0.sourceWidth;
            Integer intOrNull2 = StringsKt.toIntOrNull((String) parts.get(1));
            this$0.sourceHeight = intOrNull2 != null ? intOrNull2.intValue() : this$0.sourceHeight;
        }
        this$0.sourceFps = $fps;
        this$0.sourceDurationMs = $duration != null ? $duration.longValue() : 0L;
        TextView textView = this$0.tvResolution;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvResolution");
            textView = null;
        }
        textView.setText(this$0.buildSourceInfo($resolution, $fps, $duration));
        this$0.updatePreviewBox();
    }

    private static final void onCreate$lambda$16(ConfigActivity this$0, View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.updatePreviewBox();
    }

    private static final void onCreate$lambda$17(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.pendingFirstFrame) {
            this$0.pendingFirstFrame = false;
            this$0.startPlayback(true);
        }
    }

    private static final void onCreate$lambda$18(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.ensureWallpaperPermission();
        this$0.applyDeskBackground();
        this$0.updatePreviewBox();
        this$0.applyPreviewClip();
    }

    private static final void onCreate$lambda$20(ConfigActivity this$0, View $indicator) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        int pad = (int) (24 * this$0.getResources().getDisplayMetrics().density);
        Object parent = $indicator.getParent();
        Intrinsics.checkNotNull(parent, "null cannot be cast to non-null type android.view.View");
        int half = (((View) parent).getWidth() - (pad * 2)) / 2;
        ViewGroup.LayoutParams $this$onCreate_u24lambda_u2420_u24lambda_u2419 = $indicator.getLayoutParams();
        $this$onCreate_u24lambda_u2420_u24lambda_u2419.width = half;
        $indicator.setLayoutParams($this$onCreate_u24lambda_u2420_u24lambda_u2419);
        $indicator.requestLayout();
        $indicator.setTranslationX(0.0f);
    }

    static /* synthetic */ void onCreate$selectTab$default(LinearLayout linearLayout, LinearLayout linearLayout2, ConfigActivity configActivity, View view, boolean z, boolean z2, int i, Object obj) {
        if ((i & 32) != 0) {
            z2 = true;
        }
        onCreate$selectTab(linearLayout, linearLayout2, configActivity, view, z, z2);
    }

    /** 布局里 ll_content / ll_appearance 同在唯一一个 ScrollView 下（ScrollView>内层LL>两页）。 */
    private final ScrollView configScrollView() {
        View v = findViewById(R.id.ll_content);
        if (v == null) {
            return null;
        }
        ViewParent p1 = v.getParent();
        ViewParent p2 = p1 != null ? p1.getParent() : null;
        return p2 instanceof ScrollView ? (ScrollView) p2 : null;
    }

    private static final void onCreate$selectTab(LinearLayout llContent, LinearLayout llAppearance, ConfigActivity this$0, View indicator, boolean content, boolean animated) {
        LinearLayout target = content ? llContent : llAppearance;
        LinearLayout other = content ? llAppearance : llContent;
        // v1.8.14：切走前记下旧页的滚动位置，切进新页后恢复它上次的位置
        // v1.8.15：不再「先回顶 300ms 后跳回来」—— 目标页在 alpha=0 状态下先滚到位再淡入
        final ScrollView sv = this$0.configScrollView();
        final int targetY;
        if (sv != null) {
            if (content) {
                this$0.scrollAppearance = sv.getScrollY();
            } else {
                this$0.scrollContent = sv.getScrollY();
            }
            targetY = content ? this$0.scrollContent : this$0.scrollAppearance;
        } else {
            targetY = 0;
        }
        this$0.applyTabColors(content);
        indicator.animate().translationX(content ? 0.0f : indicator.getWidth()).setDuration(animated ? 180L : 0L).start();
        if (!animated) {
            other.setVisibility(8);
            target.setVisibility(0);
            if (sv != null) {
                target.setAlpha(0.0f);
                sv.post(() -> {
                    sv.scrollTo(0, targetY);
                    target.setAlpha(1.0f);
                });
            } else {
                target.setAlpha(1.0f);
            }
            return;
        }
        other.animate().alpha(0.0f).setDuration(90L).withEndAction(() -> {
            onCreate$selectTab$lambda$21(other, target, sv, targetY);
        }).start();
    }

    private static final void onCreate$selectTab$lambda$21(LinearLayout $other, LinearLayout $target, ScrollView $sv, int $targetY) {
        $other.setVisibility(8);
        $other.setAlpha(1.0f);
        $target.setAlpha(0.0f);
        $target.setVisibility(0);
        if ($sv != null) {
            $sv.post(() -> {
                $sv.scrollTo(0, $targetY);
                $target.animate().alpha(1.0f).setDuration(160L).start();
            });
        } else {
            $target.animate().alpha(1.0f).setDuration(160L).start();
        }
    }

    private static final void onCreate$lambda$22(LinearLayout $llContent, LinearLayout $llAppearance, ConfigActivity this$0, View $indicator, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        onCreate$selectTab$default($llContent, $llAppearance, this$0, $indicator, true, false, 32, null);
    }

    private static final void onCreate$lambda$23(LinearLayout $llContent, LinearLayout $llAppearance, ConfigActivity this$0, View $indicator, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        onCreate$selectTab$default($llContent, $llAppearance, this$0, $indicator, false, false, 32, null);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockSplitter
        jadx.core.utils.exceptions.JadxRuntimeException: Unexpected missing predecessor for block: B:8:0x001a
        	at jadx.core.dex.visitors.blocks.BlockSplitter.addTempConnectionsForExcHandlers(BlockSplitter.java:280)
        	at jadx.core.dex.visitors.blocks.BlockSplitter.visit(BlockSplitter.java:79)
        */
    private final void ensureWallpaperPermission() {
        /*
            r5 = this;
            int r0 = android.os.Build.VERSION.SDK_INT
            r1 = 33
            if (r0 < r1) goto Le
            java.lang.String r0 = "android.permission.READ_MEDIA_IMAGES"
            goto L11
        Le:
            java.lang.String r0 = "android.permission.READ_EXTERNAL_STORAGE"
        L11:
            r6 = r0
            r0 = r5
            r1 = r6
            int r0 = r0.checkSelfPermission(r1)
            if (r0 == 0) goto L30
        L1b:
            r0 = r5
            r1 = 1
            java.lang.String[] r1 = new java.lang.String[r1]     // Catch: java.lang.Throwable -> L2f
            r7 = r1
            r1 = r7
            r2 = 0
            r3 = r6
            r1[r2] = r3     // Catch: java.lang.Throwable -> L2f
            r1 = r7
            r2 = 200(0xc8, float:2.8E-43)
            r0.requestPermissions(r1, r2)     // Catch: java.lang.Throwable -> L2f
            goto L30
        L2f:
            r7 = move-exception
        L30:
            return
        */
        String permission = Build.VERSION.SDK_INT >= 33
                ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE";
        if (checkSelfPermission(permission) != 0) {
            try {
                requestPermissions(new String[]{permission}, 200);
            } catch (Throwable th) {
            }
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            applyDeskBackground();
        }
    }

    private final void applyPreviewClip() {
        FrameLayout frame = (FrameLayout) findViewById(R.id.preview_frame);
        final float radius = 0.0f;
        frame.setClipToOutline(true);
        frame.setOutlineProvider(new ViewOutlineProvider() { // from class: io.github.animwidget.ConfigActivity.applyPreviewClip.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(@NotNull View view, @NotNull Outline outline) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(outline, "outline");
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        frame.invalidateOutline();
    }

    /**
     * v1.10.519：壁纸底图 → {@link WallpaperBackdropView}（按「整块显示区」对位）。
     *
     * <p>这里只剩接线：装回调（图到了要按新壁纸重算预览虚线色）+ 让它跟上当前壁纸。
     * 取图 / 判代号 / 解码线程 / 切哪一块全在视图里 —— 因为「旧壁纸」和「整体上移」这两个毛病
     * 都出在取到图之后怎么摆这条链上，散在 Activity 里就又会各改各的。</p>
     */
    private final void applyDeskBackground() {
        try {
            View desk = findViewById(R.id.img_desk);
            if (!(desk instanceof WallpaperBackdropView)) {
                return;
            }
            WallpaperBackdropView backdrop = (WallpaperBackdropView) desk;
            backdrop.setOnWallpaperListener(this::applyOutlineFromWallpaper);
            backdrop.reload();
        } catch (Throwable th) {
        }
    }

    /**
     * v1.10.460（用户实测）：预览虚线原来写死成半透明蓝灰 `#66707a9a`，
     * 壁纸只要是蓝灰调就几乎看不见（用户原话："肉眼须仔细才能看出来"）。
     *
     * <p>现在按用户的判据取色：**壁纸全部像素里占比最大的那一档颜色，取它的相反值**。
     * 这样框永远落在壁纸主色的对立面 —— 不需要知道壁纸是什么内容，也不需要给用户加设置项。
     *
     * <p>采样 48×48（2304 像素）画进一张 48×48 的位图再统计：4~6ms，一次性，不是每帧。
     * 读不到壁纸就保持 XML 里的兜底色（绝不让框"消失"）。
     */
    private final void applyOutlineFromWallpaper(@org.jetbrains.annotations.Nullable Bitmap wallpaper) {
        try {
            View box = findViewById(R.id.preview_box);
            if (box == null || wallpaper == null || wallpaper.isRecycled()
                    || wallpaper.getWidth() <= 0 || wallpaper.getHeight() <= 0) {
                return;
            }
            final int n = 48;
            Bitmap small = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(small);
            // v1.10.519：底图从 Drawable 换成 Bitmap，采样口径明确成「cover 铺满 48×48」。
            // 原来是 setBounds(0,0,48,48) 直接拉伸：位图一旦带 density，拉成什么比例是说不清的
            // （W43 判的是「占比最大的那一档颜色」，比例被非等比拉伸就会偏）。
            float s = Math.max(n / (float) wallpaper.getWidth(), n / (float) wallpaper.getHeight());
            float dx = (n - wallpaper.getWidth() * s) / 2f;
            float dy = (n - wallpaper.getHeight() * s) / 2f;
            android.graphics.Paint sampler = new android.graphics.Paint();
            sampler.setFilterBitmap(true);
            c.drawBitmap(wallpaper,
                    new android.graphics.Rect(0, 0, wallpaper.getWidth(), wallpaper.getHeight()),
                    new android.graphics.RectF(dx, dy, dx + wallpaper.getWidth() * s,
                            dy + wallpaper.getHeight() * s),
                    sampler);
            int[] px = new int[n * n];
            small.getPixels(px, 0, n, 0, 0, n, n);
            if (!small.isRecycled()) {
                small.recycle();
            }
            int dominant = OutlineColor.dominant(px);
            // v1.10.463：三档口径的唯一来源搬到 OutlineSettings（ThemePrefs 存键 + 壁纸主色缓存）。
            // 462 这里直接读 settings 的 outline_mode、又反手调 SettingsActivity.prefsOutlineCustom ——
            // 语义色页和预览框各算各的，一旦哪边口径改了就会「页面上看到的和画出来的不是一个色」。
            OutlineSettings.noteWallpaperBg(dominant);
            OutlineColor.Mode mode = OutlineSettings.mode(this);
            int line = OutlineColor.resolve(mode, dominant, OutlineSettings.customRgb(this));
            float d = getResources().getDisplayMetrics().density;
            // 与 XML 完全一致：笔画 2dp、虚 6dp、隔 5dp、圆角 2dp（只换颜色，不改观感）
            android.graphics.drawable.GradientDrawable gd =
                    new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setColor(0x00000000);
            gd.setCornerRadius(2.0f * d);
            gd.setStroke(Math.round(2.0f * d), line, 6.0f * d, 5.0f * d);
            box.setForeground(gd);
            Diag.warn(this, "W43", "预览虚线取色 档位=" + mode.name()
                    + " 壁纸主色=" + OutlineColor.hex(dominant)
                    + " 虚线=" + OutlineColor.hex(line)
                    + (dominant < 0 ? "（壁纸读不到，用兜底色）" : "（= 主色的相反值）"));
        } catch (Throwable th) {
            Diag.warn(this, "W43", "预览虚线取色失败，保留原色: " + th.getClass().getSimpleName());
        }
    }

    private final void applyAutoParams() {
        Integer fps = this.sourceFps;
        int srcX10 = this.sourceFpsX10;
        if (srcX10 > 0) {
            // v1.10.26（需求 4）：默认参数直接落在「识别到的确切刷新率」档位上，
            // 33.7fps 就落 34 档（effectiveFpsX10 会还原成 337，不丢精度）；
            // 60fps 素材才被上限 50 截断，这是能力边界不是四舍五入。
            SeekBar seekBar = this.seekFps;
            if (seekBar == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekFps");
                seekBar = null;
            }
            seekBar.setProgress(RangesKt.coerceIn(Math.round(srcX10 / 10.0f), 1, 50) - 1);
        } else if (fps != null && fps.intValue() > 0) {
            SeekBar seekBar = this.seekFps;
            if (seekBar == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekFps");
                seekBar = null;
            }
            seekBar.setProgress(RangesKt.coerceIn(fps.intValue(), 1, 50) - 1);
        }
        if (this.sourceWidth > 0 && this.sourceHeight > 0) {
            // v1.10.59：素材在阈值内（图片 ≤2.5K / 动图·视频 ≤1080p）→ 默认落在「原图直出」档。
            // 老实现用 (longest-120)/8 向下取整：300px 的素材落到 296，一开始就白缩了一点。
            int cap = edgeCap();
            int longest = RangesKt.coerceIn(Math.max(this.sourceWidth, this.sourceHeight), 120, cap);
            int steps = (longest - 120 + 7) / 8;   // 向上取整，保证 120+8*steps >= 素材
            SeekBar seekBar2 = this.seekEdge;
            if (seekBar2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekEdge");
                seekBar2 = null;
            }
            seekBar2.setProgress(RangesKt.coerceIn(steps, 0, 305));
        }
        SeekBar seekBar3 = this.seekQuality;
        if (seekBar3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar3 = null;
        }
        seekBar3.setProgress(50);
        updateLabels();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String formatTime(long millis) {
        long totalSeconds = RangesKt.coerceAtLeast(millis / 1000, 0L);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (millis % 1000) / 10;
        Object[] objArr = {Long.valueOf(minutes), Long.valueOf(seconds), Long.valueOf(hundredths)};
        String str = String.format("%d:%02d.%02d", Arrays.copyOf(objArr, objArr.length));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return str;
    }

    static /* synthetic */ String buildSourceInfo$default(ConfigActivity configActivity, String str, Integer num, Long l, int i, Object obj) {
        if ((i & 4) != 0) {
            l = null;
        }
        return configActivity.buildSourceInfo(str, num, l);
    }

    private final String buildSourceInfo(String resolution, Integer fps, Long durationMs) {
        int fpsX10 = (fps != null && fps.intValue() > 0) ? fps.intValue() * 10 : this.sourceFpsX10;
        long duration = (durationMs != null && durationMs.longValue() > 0) ? durationMs.longValue() : this.sourceDurationMs;
        return buildSourceInfoFull(resolution, fpsX10, this.sourceFrames, duration, this.sourceFpsEstimated);
    }

    /** v1.10.0：基本信息（分辨率 · 帧数 · 帧率 · 时长），帧率保留一位小数并标注是否推算。 */
    final String buildSourceInfoFull(String resolution, int fpsX10, int frames, long durationMs, boolean estimated) {
        List<String> parts = new ArrayList<>();
        if (resolution != null && !StringsKt.isBlank(resolution)) {
            parts.add(getString(R.string.cfg_source_resolution, new Object[]{resolution}));
        }
        if (frames > 0) {
            parts.add(getString(R.string.cfg_source_frames, new Object[]{Integer.valueOf(frames)}));
        }
        if (fpsX10 > 0) {
            float value = fpsX10 / 10.0f;
            // v1.10.127（反馈「显示 20 帧，原帧率却显示 33.3 帧」）：33.3 = 20 帧 ÷ 0.6s，是**平均**值。
            // 把「平均每帧多少毫秒」一并显示出来，就不必再猜这个帧率是怎么来的了。
            long perFrame = (frames > 1 && durationMs > 0L)
                    ? Math.max(1L, durationMs / frames)
                    : Math.round(1000.0f / Math.max(value, 0.1f));
            Object[] args = {Float.valueOf(value), Long.valueOf(perFrame)};
            parts.add(getString(estimated ? R.string.cfg_source_fps_est : R.string.cfg_source_fps_one, args));
        }
        if (durationMs > 0) {
            parts.add(getString(R.string.cfg_source_duration, new Object[]{formatTime(durationMs)}));
        }
        return CollectionsKt.joinToString(parts, " · ", "", "", -1, "...", (Function1) null);
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x002b  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x002f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final void updateEmptyHint() {
        /*
            r3 = this;
            r0 = r3
            android.graphics.Bitmap r0 = r0.previewSource
            if (r0 != 0) goto L2f
            r0 = r3
            io.github.animwidget.PreviewFrames$Result r0 = r0.previewFrames
            r1 = r0
            if (r1 == 0) goto L26
            java.util.List r0 = r0.getFrames()
            r1 = r0
            if (r1 == 0) goto L26
            boolean r0 = r0.isEmpty()
            if (r0 != 0) goto L22
            r0 = 1
            goto L28
        L22:
            r0 = 0
            goto L28
        L26:
            r0 = 0
        L28:
            if (r0 != 0) goto L2f
            r0 = 1
            goto L30
        L2f:
            r0 = 0
        L30:
            r4 = r0
            r0 = r3
            r1 = 2131230724(0x7f080004, float:1.8077509E38)
            android.view.View r0 = r0.findViewById(r1)
            android.widget.TextView r0 = (android.widget.TextView) r0
            r1 = r0
            if (r1 != 0) goto L43
        L40:
            goto L50
        L43:
            r1 = r4
            if (r1 == 0) goto L4b
            r1 = 0
            goto L4d
        L4b:
            r1 = 8
        L4d:
            r0.setVisibility(r1)
        L50:
            return
        */
        boolean hasFrames = false;
        if (this.previewSource == null) {
            PreviewFrames.Result frames = this.previewFrames;
            if (frames != null && frames.getFrames() != null && !frames.getFrames().isEmpty()) {
                hasFrames = true;
            }
        }
        // v1.10.0：已经选了素材就在解析中 —— 也不该再显示「预览」二字（用户明确要求）
        if (this.pickedUri != null) {
            hasFrames = true;
        }
        boolean empty = !hasFrames;
        View hint = findViewById(R.id.tv_preview_empty);
        if (hint != null) {
            hint.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private final int previewWidth() {
        ImageView imageView = this.imgPreview;
        if (imageView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("imgPreview");
            imageView = null;
        }
        int width = imageView.getWidth();
        if (width > 0) {
            return width;
        }
        if (this.previewBoxWidth > 0) {
            return this.previewBoxWidth;
        }
        return 640;
    }

    private final int previewHeight() {
        ImageView imageView = this.imgPreview;
        if (imageView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("imgPreview");
            imageView = null;
        }
        int height = imageView.getHeight();
        if (height > 0) {
            return height;
        }
        if (this.previewBoxWidth > 0 && this.previewBoxHeight > 0) {
            return this.previewBoxHeight;
        }
        float ratio = previewRatio();
        return RangesKt.coerceIn((int) (640.0f / ratio), 120, 1080);
    }

    private final void updatePreviewBox() {
        FrameLayout frame = (FrameLayout) findViewById(R.id.preview_frame);
        FrameLayout area = (FrameLayout) findViewById(R.id.preview_area);
        FrameLayout box = (FrameLayout) findViewById(R.id.preview_box);
        int parentWidth = frame.getWidth();
        int parentHeight = area.getHeight();
        if (parentWidth <= 0 || parentHeight <= 0) {
            return;
        }
        float ratio = previewRatio();
        LayoutPickerView picker = this.layoutPicker;
        if (picker != null) {
            picker.setMediaRatio(mediaRatio());
            picker.setMediaExactRatio(mediaOnlyRatio());
            picker.setWidgetRatio(widgetAspect());
        }
        // v1.10.0：虚线预览框要比画面「外扩一圈」—— 画面尺寸仍严格等于 previewRatio()，
        // 外框 = 画面 + 2×pad（padding 只给外框，ImageView 拿到的是画面本身，比例不会被拉歪）。
        // v1.10.214：pad 6dp → 2dp。虚线笔画 2dp 是**骑在**框线上的（内 1dp + 外 1dp），
        // 所以 pad=2dp 时画面到虚线内沿正好 1dp，画面紧贴虚线、且不会盖住虚线。
        // 配套：preview_outline 的圆角必须 ≤ pad（2dp），否则直角蒙版的四个角会压到虚线弧。
        int pad = Math.round(2.0f * getResources().getDisplayMetrics().density);
        int contentWidth = (int) (parentWidth * 0.72f);
        int contentHeight = (int) (contentWidth / ratio);
        if (contentHeight > parentHeight * 0.88f) {
            contentHeight = (int) (parentHeight * 0.88f);
            contentWidth = (int) (contentHeight * ratio);
        }
        int boxWidth = contentWidth + pad * 2;
        int boxHeight = contentHeight + pad * 2;
        box.setPadding(pad, pad, pad, pad);
        box.setLayoutParams(new FrameLayout.LayoutParams(boxWidth, boxHeight, 17));
        box.requestLayout();
        // v1.10.1（需求 3）：切布局后按「当前有没有任务」重新显示圆环/进度条
        syncTaskUi();
        if (contentWidth != this.previewBoxWidth || contentHeight != this.previewBoxHeight) {
            this.previewBoxWidth = contentWidth;
            this.previewBoxHeight = contentHeight;
            box.post(() -> {
                updatePreviewBox$lambda$24(ConfigActivity.this);
            });
        }
    }

    private static final void updatePreviewBox$lambda$24(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.refreshPreviewNow();
    }

    private final float sourceAspect() {
        return mediaRatio();
    }

    /**
     * 素材真实宽高比（多级兜底）。
     *
     * v1.7.0：原来只认 sourceWidth/sourceHeight，探针还没回来时直接退回组件比例，
     * 于是「原始比例」下预览框一直是组件比例，看起来和素材完全不符。
     */
    /**
     * v1.10.93：**只表示素材比例**；没有素材时返回 0。
     *
     * 布局卡片的前景图形要用它：0 → 画默认标准 3:2（用户指定），
     * 而不是像 {@link #mediaRatio()} 那样退回组件比例（那样「前景＝素材比例」就名不副实）。
     */
    private final float mediaOnlyRatio() {
        Bitmap thumb = this.previewSource;
        if (thumb != null && !thumb.isRecycled() && thumb.getWidth() > 0 && thumb.getHeight() > 0) {
            return thumb.getWidth() / (float) thumb.getHeight();
        }
        if (this.sourceWidth > 0 && this.sourceHeight > 0) {
            return this.sourceWidth / (float) this.sourceHeight;
        }
        if (this.pickedUri == null) {
            WidgetConfig base = this.base;
            if (base != null && base.getFrameWidth() > 0 && base.getFrameHeight() > 0) {
                return base.getFrameWidth() / (float) base.getFrameHeight();
            }
        }
        PreviewFrames.Result frames = this.previewFrames;
        if (frames != null && frames.getFrames() != null && !frames.getFrames().isEmpty()) {
            String firstPath = frames.getFrames().get(0);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(firstPath, opts);
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return opts.outWidth / (float) opts.outHeight;
            }
        }
        return 0.0f;
    }

    private final float mediaRatio() {
        // v1.9.2：预览缩略图最优先 —— 它永远是「当前所选」素材的像素，切换素材立即生效。
        // （原来 sourceWidth 排第一，切素材后它还是上一个素材的探测值 → 比例/线框错乱）
        Bitmap thumb = this.previewSource;
        if (thumb != null && !thumb.isRecycled() && thumb.getWidth() > 0 && thumb.getHeight() > 0) {
            return RangesKt.coerceIn(thumb.getWidth() / (float) thumb.getHeight(), 0.2f, 5.0f);
        }
        if (this.sourceWidth > 0 && this.sourceHeight > 0) {
            return RangesKt.coerceIn(this.sourceWidth / (float) this.sourceHeight, 0.2f, 5.0f);
        }
        // 有未保存的新素材时，绝不用旧素材的已存尺寸
        if (this.pickedUri == null) {
            WidgetConfig base = this.base;
            if (base != null && base.getFrameWidth() > 0 && base.getFrameHeight() > 0) {
                return RangesKt.coerceIn(base.getFrameWidth() / (float) base.getFrameHeight(), 0.2f, 5.0f);
            }
        }
        PreviewFrames.Result frames = this.previewFrames;
        if (frames != null && frames.getFrames() != null && !frames.getFrames().isEmpty()) {
            // v1.9.0：帧存的是无损 PNG 字节 —— 只读 bounds 拿比例，不解码
            String firstPath = frames.getFrames().get(0);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(firstPath, opts);
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return RangesKt.coerceIn(opts.outWidth / (float) opts.outHeight, 0.2f, 5.0f);
            }
        }
        return widgetAspect();
    }

    /** 预览画布（= 预览框 = 合成位图）应有的宽高比：FIT 跟素材，其余跟布局/组件。 */
    private final float previewRatio() {
        if (this.selectedLayout != null && this.selectedLayout.getRatio() > 0.0f) {
            return RangesKt.coerceIn(this.selectedLayout.getRatio(), 0.2f, 5.0f);
        }
        if (this.selectedLayout == LayoutMode.FIT) {
            return mediaRatio();
        }
        return widgetAspect();
    }

    private final float widgetAspect() {
        // v1.10.56：**和桌面同一套推导**。
        // 老实现是 `minWidth / minHeight`（两个 int 相除）：3 格宽 2 格高 = 340/220 → 1（不是 1.55），
        // 竖一点的比例甚至直接得 0。于是编辑页预览框被压成正方形，桌面上却是 1.55:1，
        // 用户看到的就是「导出后画面比例变了、和素材不一致」。
        // 现在直接问 WidgetRender：dp × density、再按 22 万像素等比封顶，与真正渲染的位图同尺寸。
        return WidgetRender.INSTANCE.aspect(this, this.widgetId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateLabels() {
        // v1.10.497：换语言时主按钮也要跟着换（「导出组件」/「保存配置」两种态各自有译文）
        refreshSaveButtonText();
        TextView fpsLabel = this.tvFps;
        if (fpsLabel == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvFps");
            fpsLabel = null;
        }
        SeekBar fpsBar = this.seekFps;
        if (fpsBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            fpsBar = null;
        }
        int fpsSlider = fpsBar.getProgress() + 1;
        int srcX10 = this.sourceFpsX10;
        if (srcX10 > 0 && Math.max(Math.round(srcX10 / 10.0f), 1) == fpsSlider) {
            // v1.10.26（需求 4）：正好停在源帧率档位 —— 显示确切值（33.7），
            // 不再写成 34 让人以为被系统改过（实际播放一直是 33.7）
            fpsLabel.setText(getString(R.string.cfg_fps_source,
                    new Object[]{String.format(java.util.Locale.US, "%.1f", srcX10 / 10.0f)}));
        } else {
            fpsLabel.setText(getString(R.string.cfg_fps_value, new Object[]{Integer.valueOf(fpsSlider)}));
        }
        // v1.10.128（反馈「20 帧却显示 33.3fps」顺带确认的体验问题）：60Hz 屏上只有 60 的整数分之一
        // 才不会有节奏抖动；不是的话给一句提示（只提示，不改用户设置）
        try {
            int effFps = Math.max(1, Math.round(this.effectiveFpsX10() / 10.0f));
            int better = friendlyFps(effFps);
            if (better > 0) {
                fpsLabel.setText(fpsLabel.getText() + "\n" + getString(R.string.cfg_fps_judder,
                        new Object[]{String.valueOf(effFps), String.valueOf(better)}));
            }
        } catch (Throwable ignored) {
        }
        TextView textView2 = this.tvEdge;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvEdge");
            textView2 = null;
        }
        // v1.10.59：标签显示**实际会用的**边长（原图直出时写「原图直出」）
        int edgeNow = effectiveEdge();
        boolean edgeOriginal = isOriginalEdge(edgeNow);
        // v1.10.404：括号里的「最高」按素材类别给（图片 2560 / 动图·视频 1920），
        // 原来写死「最高 2560 / 2.5K」，视频拖到最右也只生效到 1920，看起来像被偷偷放开到 2.5K。
        textView2.setText(getString(edgeOriginal ? R.string.cfg_edge_original : R.string.cfg_edge_value,
                new Object[]{Integer.valueOf(edgeNow), Integer.valueOf(edgeCap())}));
        TextView textView3 = this.tvQuality;
        if (textView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvQuality");
            textView3 = null;
        }
        // v1.10.59：100 = 不压缩（真无损），标签直接这么写，别再显示「100」让人猜
        int qualityNow = 50 + progress(this.seekQuality);
        textView3.setText(qualityNow >= 100
                ? getString(R.string.cfg_quality_off)
                : getString(R.string.cfg_quality_value, new Object[]{Integer.valueOf(qualityNow)}));
        updateParamsLine();
        TextView textView4 = this.tvAlpha;
        if (textView4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvAlpha");
            textView4 = null;
        }
        Object[] objArr4 = new Object[1];
        SeekBar seekBar4 = this.seekAlpha;
        if (seekBar4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
            seekBar4 = null;
        }
        objArr4[0] = Integer.valueOf(seekBar4.getProgress());
        textView4.setText(getString(R.string.cfg_alpha_value, objArr4));
        TextView textView5 = this.tvScale;
        if (textView5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvScale");
            textView5 = null;
        }
        Object[] objArr5 = new Object[1];
        SeekBar seekBar5 = this.seekScale;
        if (seekBar5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekScale");
            seekBar5 = null;
        }
        // v1.10.73：滑条是 -100 ~ +100（0 = 原尺寸），progress 0..200 → 显示值 = progress - 100
        objArr5[0] = String.format(java.util.Locale.US, "%+d", Integer.valueOf(seekBar5.getProgress() - 100));
        textView5.setText(getString(R.string.cfg_scale_value, objArr5));
        TextView textView6 = this.tvShadow;
        if (textView6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvShadow");
            textView6 = null;
        }
        Object[] objArr6 = new Object[1];
        SeekBar seekBar6 = this.seekShadow;
        if (seekBar6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
            seekBar6 = null;
        }
        objArr6[0] = Integer.valueOf(seekBar6.getProgress());
        textView6.setText(getString(R.string.cfg_shadow_value, objArr6));
        syncCurveUi();
    }

    // ------------------------------------------------ v1.10.222 阴影：曲线 6 滑块 + 唯一渲染入口
    /**
     * 剖面的 3 个可调量（顺序 = [扩散, 渐变长度, 渐变形状]）。
     *
     * <p>v1.10.223：从「6 个百分比」换成「3 个能听见名字就知道是什么」的量 ——
     * 用户的原话是「参数里连一个类似渐变的参数都没有」，而 6 个里面 3 个是位置、3 个是浓度，
     * 没有一个是「渐变」本身。</p>
     */
    /**
     * v1.10.228：面板上只剩**两个物理量**（都是参考实现真正拥有的东西）：
     * <ul>
     *   <li>{@code curve_spread} = 扩散（轮廓外扩多少再模糊，0 = 参考实现原样）；</li>
     *   <li>{@code curve_grad} = 模糊半径（默认 2.9% 宽 = 参考实现的 宽/35）。</li>
     * </ul>
     * 第三个滑块（{@code curve_gamma}）和只读曲线图在 {@code setupShadowCurvePanel} 里隐藏：
     * 它们是 v1.10.223 那套「渐变形状」模型的残骸，现在没有任何像素归它管。
     */
    private static final int[] CURVE_BARS = {
            R.id.curve_spread, R.id.curve_grad};
    private static final int[] CURVE_LABELS = {
            R.id.tv_curve_spread, R.id.tv_curve_grad};
    private static final int[] CURVE_LABEL_TEXT = {
            R.string.cfg_curve_spread, R.string.cfg_curve_grad};

    /**
     * v1.10.224：三档「精细调」预设 = {扩散, 渐变长度, 渐变形状}。
     *
     * <p>每一档都是把用户历史上某一句反馈翻译成一组具体数字，**而且都能用上面三个滑块原样复现**
     * （不是暗门：点完之后滑块会停在同样的位置，你能看清每一档改了什么）。</p>
     *
     * <ul>
     *   <li><b>投影感</b> = 扩散 1.2% 画布宽 + 渐变 3.2% + 形状 1.80：修 v1.10.217「很接近了但不够深」+ v1.10.223「像半透明雾」。
     *       （v1.10.225 起默认已经 8.0% 宽，这一档代表"紧凑但实"的另一种口味。）
     *       贴边先实 12px（画布 1030 时）、之后按 1.8 次幂快速收掉 —— 眼睛读到的是"物体压在地面上"，
     *       而**峰值一点没加**（还是强度决定的 33%），所以不会变成 v1.10.216 骂的"一条线"。
     *       可见跨度 3.81% 比默认 4.76% 更短 ⇒ 画面反而更大。</li>
     *   <li><b>大范围</b> = 扩散 0 + 渐变 11.2% + 形状 1.00：修 v1.10.216「太刻板像一条线」。
     *       10.7% 跨度（w=1030 时 110px）的均匀渐变，是"大面积柔和投影"那一档。
     *       代价写清楚：画面每边要多缩 ~31px（相对默认 8.0%）。</li>
     *   <li><b>省画面</b> = 扩散 0 + 渐变 2.4% + 形状 1.50：修 v1.10.220「阴影没多少却回收了这么多」。
     *       可见跨度只有 2.09%（21px @1030），画面几乎不缩，但贴边仍是 33% 黑 —— 一条紧实、看得见的边。</li>
     * </ul>
     *
     * <p>数值是**轴坐标**：1.0 = 画布宽的 8%，所以 1.2% 画布宽 = 0.150、3.2% = 0.400。
     * （这里最容易写错的就是直接填百分比 —— 那写出来只有 1px；门禁会把三档的跨度打出来对照。）</p>
     */
    /**
     * v1.10.233：三档「精细调」= **一整套阴影参数** {强度, 范围 L, 紧实度 ρ}。
     *
     * <p>复核结论：参考实机是「单层高斯尾、贴边 0.70A、可见宽 5.0%、半高点占跨度 0.32」，
     * 没有肩也没有断崖。所以默认取那一组（L=5% / ρ=0.55），三档围绕它各走一个方向，
     * 每档都能被两个滑块原样复现：</p>
     *
     * <ul>
     *   <li><b>投影感</b> = 100 / 4.0% / 1.00：更紧更实（贴边 0.84A），近距离接触阴影的观感；</li>
     *   <li><b>大范围</b> = 100 / 7.0% / 0.20：范围拉大、紧实度压低，大面积柔光（贴边 0.58A）；</li>
     *   <li><b>省画面</b> = 100 / 3.0% / 0.55：形状同默认，只是范围缩到 3%，画面几乎不缩。</li>
     * </ul>
     */
    private static final float[] PRESET_EDGE = {100.0f, 0.040f, 1.00f};
    private static final float[] PRESET_WIDE = {100.0f, 0.070f, 0.20f};
    private static final float[] PRESET_TIGHT = {100.0f, 0.041f, 0.58f};

    /** 程序写回滑块时置 true：那条路径绝不能再当成「用户改了一个参数」。 */
    private boolean curveSyncing;

    /** v1.10.229：「自定义」面板展开/收起的动画（连点时取消上一段，只保留最后一次）。 */
    @Nullable
    private android.animation.ValueAnimator shadowPanelAnim;

    /** v1.10.230：预设/重置时三个滑块一起滑过去的动画。 */
    @Nullable
    private android.animation.ValueAnimator shadowSliderAnim;

    /**
     * 「会影响像素的阴影参数」签名（强度 + 剖面）。
     *
     * <p>这是「值没变就不许重算」的唯一判据 —— 只按下不拖动、写回夹紧后的同一个值、
     * 点到已经是最值的滑块，都不会触发一次合成。</p>
     */
    @NotNull
    private String shadowParamsSig = "";

    @Nullable
    private final SeekBar curveBar(int id) {
        View v = findViewById(id);
        return v instanceof SeekBar ? (SeekBar) v : null;
    }

    private final int curveBarProgress(int id) {
        SeekBar b = curveBar(id);
        return b != null ? b.getProgress() : 0;
    }

    private final void setCurveBarProgress(int id, int value) {
        SeekBar b = curveBar(id);
        if (b != null && b.getProgress() != value) {
            b.setProgress(value);
        }
    }

    /** 文本真的变了才 setText（拖动时每帧都会重算一遍数值标签）。 */
    private static void setTextIfChanged(@Nullable TextView view, String text) {
        if (view == null || text == null) {
            return;
        }
        CharSequence now = view.getText();
        if (now != null && text.contentEquals(now)) {
            return;
        }
        view.setText(text);
    }

    private static String pct(float value, int digits) {
        return String.format(java.util.Locale.US, "%." + digits + "f%%", Float.valueOf(value * 100.0f));
    }

    private final String shadowParamsSig() {
        return progress(this.seekShadow) + "|" + this.selectedShadowCurve;
    }

    /**
     * v1.10.222/223：**阴影参数的唯一渲染入口**（主滑块 / 3 个剖面滑块 / 重置 全调这一个方法）。
     *
     * <p>为什么要「唯一」：v1.10.221 里曲线是曲线视图自己回调 `refreshPreviewNow()`，
     * 主滑块又是另一条路 —— 结果预览的合成缓存 key 漏了曲线，曲线改了命中旧缓存、
     * 画面纹丝不动，直到碰一下主滑块才真的用新剖面合成一次（用户看到的「抖一下」）。
     * 现在所有入口收敛到这一处，并且**由签名决定要不要重算**，不靠谁去补一次刷新。</p>
     */
    private final void renderShadowParams() {
        String sig = shadowParamsSig();
        if (sig.equals(this.shadowParamsSig)) {
            return;
        }
        this.shadowParamsSig = sig;
        refreshPreviewNow();
    }

    /** 2 个滑块（范围 / 紧实度）→ 阴影规格（构造器会夹紧越界值）→ 模型 + 数值标签。 */
    private final void commitCurveFromSliders() {
        ShadowSpec spec = new ShadowSpec(
                ShadowSpec.progressToRange(curveBarProgress(R.id.curve_spread)),
                ShadowSpec.progressToRho(curveBarProgress(R.id.curve_grad)));
        this.selectedShadowCurve = spec.isDefault() ? "" : spec.encode();
        syncCurveSliders(spec);
        syncCurveUi();
    }

    /**
     * 应用一档预设：与三个滑块、重置**走同一条链路**
     * （写回滑块 → 刷新数值标签 → 唯一渲染入口 renderShadowParams）。
     *
     * <p>不做「点了就生效但滑块没动」那种事：滑块必须跟着停在对应的位置，
     * 用户才能看清这一档到底改了什么、也才能从它继续微调。</p>
     */
    /**
     * v1.10.230/233：套用一组参数时，**滑块一起滑过去**（不用瞬间跳）。
     *
     * <p>和 SettingsActivity 的对比度滑杆同一套：`ValueAnimator.ofFloat` + `Ease.IN_OUT`，
     * 时长按跨度算（180~420ms）。动画期间 `curveSyncing = true`：滑块监听只刷文字、
     * 不提交模型、不重算预览（预览合成一次几十毫秒，每帧重算是把滑块拖慢），落地时提交并重算一次。</p>
     */
    private final void applyShadowPreset(float strength, float range, float rho) {
        ShadowSpec spec = new ShadowSpec(range, rho);
        final int sTo = (int) Math.max(0.0f, Math.min(Math.round(strength), (float) ShadowSpec.STRENGTH_MAX));
        final int rTo = ShadowSpec.rangeToProgress(spec.range());
        final int pTo = ShadowSpec.rhoToProgress(spec.rho());
        SeekBar strengthBar = this.seekShadow;
        SeekBar rangeBar = curveBar(R.id.curve_spread);
        SeekBar rhoBar = curveBar(R.id.curve_grad);
        final int sFrom = strengthBar != null ? strengthBar.getProgress() : sTo;
        final int rFrom = rangeBar != null ? rangeBar.getProgress() : rTo;
        final int pFrom = rhoBar != null ? rhoBar.getProgress() : pTo;
        if (sFrom == sTo && rFrom == rTo && pFrom == pTo) {
            this.selectedShadowCurve = spec.isDefault() ? "" : spec.encode();
            syncCurveSliders(spec);
            syncCurveUi();
            renderShadowParams();
            return;
        }
        android.animation.ValueAnimator running = this.shadowSliderAnim;
        if (running != null) {
            running.cancel();
            this.shadowSliderAnim = null;
        }
        int distance = Math.max(Math.abs(sTo - sFrom), Math.max(Math.abs(rTo - rFrom), Math.abs(pTo - pFrom)));
        long duration = Math.min(420L, 180L + (distance * 240L) / 2000L);
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(duration);
        anim.setInterpolator((t) -> Ease.IN_OUT.at(t));
        anim.addUpdateListener((animation) -> {
            float f = ((Float) animation.getAnimatedValue()).floatValue();
            ConfigActivity.this.curveSyncing = true;
            try {
                SeekBar a = ConfigActivity.this.seekShadow;
                if (a != null) {
                    a.setProgress(Math.round(sFrom + (sTo - sFrom) * f));
                }
                setCurveBarProgress(R.id.curve_spread, Math.round(rFrom + (rTo - rFrom) * f));
                setCurveBarProgress(R.id.curve_grad, Math.round(pFrom + (pTo - pFrom) * f));
            } finally {
                ConfigActivity.this.curveSyncing = false;
            }
            ConfigActivity.this.syncCurveUi();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ConfigActivity.this.shadowSliderAnim = null;
                ConfigActivity.this.commitCurveFromSliders();
                ConfigActivity.this.syncCurveUi();
                ConfigActivity.this.renderShadowParams();
            }
        });
        this.shadowSliderAnim = anim;
        anim.start();
    }

    /**
     * v1.10.260：动图档位三选一（0 标准 / 1 高清 / 2 1:1）。
     *
     * 选中态直接用 `pill_outline` 的既有机制：`Button.isSelected()==true` 时
     * ThemeManager 会把它画成主色实底 + onPrimary 文字（见 styleButton），
     * 所以这里只切 isSelected，再让 styleTree 刷一遍 —— 不新造控件、不硬编码颜色。
     */
    private final void bindAnimQuality(int id, final int level) {
        View v = findViewById(id);
        if (v == null) {
            return;
        }
        v.setOnClickListener(view -> {
            ConfigActivity.this.animLevel = level;
            ConfigActivity.this.applyAnimQualityUi(level);
            Diag.warn(ConfigActivity.this, "A05", "组件动图档位=" + level + " 组件=" + ConfigActivity.this.widgetId);
        });
    }

    private final void applyAnimQualityUi(int level) {
        int[] ids = {R.id.aq0, R.id.aq1, R.id.aq2};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v == null) {
                continue;
            }
            v.setSelected(i == level);
        }
        View row = findViewById(R.id.row_anim);
        if (row != null) {
            ThemeManager.INSTANCE.styleTree(row);
        }
    }

    private final void bindPreset(int id, @NotNull float[] params) {
        View button = findViewById(id);
        if (button != null) {
            button.setOnClickListener((v) -> applyShadowPreset(params[0], params[1], params[2]));
        }
    }

    /** 把规格（含夹紧后的真实值）写回 2 个滑块。写回期间 curveSyncing=true，不会再触发渲染。 */
    private final void syncCurveSliders(@NotNull ShadowSpec spec) {
        this.curveSyncing = true;
        try {
            setCurveBarProgress(R.id.curve_spread, ShadowSpec.rangeToProgress(spec.range()));
            setCurveBarProgress(R.id.curve_grad, ShadowSpec.rhoToProgress(spec.rho()));
        } finally {
            this.curveSyncing = false;
        }
    }

    /**
     * v1.10.233：把「范围 + 紧实度 + 强度」的结果写到面板上（只读，不改任何滑块）。
     *
     * <p>汇总写的是**结果**：阴影可见多宽（= 画面每边缩多少，两者按定义相等）+ 贴边不透明度。</p>
     */
    private final void syncCurveUi() {
        ShadowSpec spec = ShadowSpec.parse(this.selectedShadowCurve);
        for (int i = 0; i < CURVE_LABELS.length; i++) {
            View v = findViewById(CURVE_LABELS[i]);
            if (v instanceof TextView) {
                setTextIfChanged((TextView) v, s(CURVE_LABEL_TEXT[i],
                        i == 0 ? pct(spec.range(), 1) : pct(spec.rho(), 2)));
            }
        }
        int strength = progress(this.seekShadow);
        int a255 = ShadowSpec.effectiveAlpha(strength, 100);
        float edge = ShadowSpec.edgeAlpha(a255, spec.rho());
        TextView sum = (TextView) findViewById(R.id.tv_curve_width);
        if (sum != null) {
            setTextIfChanged(sum, s(R.string.cfg_curve_summary,
                    pct(strength <= 0 ? 0.0f : spec.range(), 1), pct(edge, 0)));
        }
        View off = findViewById(R.id.tv_curve_off);
        if (off != null) {
            off.setVisibility(strength == 0 ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * v1.10.223：阴影剖面面板 —— 「自定义」按钮 + 行内展开的 3 个参数滑块。
     *
     * <p>为什么是行内而不是弹窗：硬约束「不能盖住预览区域」，而 Dialog / 底部弹层的高度随内容变，
     * 没法保证不压到预览；长在控制区卡片里天生压不到，还能跟着 ScrollView 一起滚。</p>
     */
    private final void setupShadowCurvePanel() {
        // v1.10.234：进面板时让 App 自己在真机上量一次 Skia 的两个常量（σ 与 extractAlpha 外扩量），
        // 打一行 S01 到日志。模型里最后一个自由参数（m/σ）就定下来了。
        ShadowProbe.runOnce(this);
        final View panel = findViewById(R.id.ll_shadow_curve);
        View toggle = findViewById(R.id.btn_shadow_curve);
        View reset = findViewById(R.id.btn_curve_reset);
        if (panel == null || toggle == null || reset == null) {
            return;
        }
        // v1.10.228：面板回来了（用户：「不可自定义之后，我并不觉得这会让我认同你」）。
        // 但参数换成了**参考实现真正拥有的物理量**，而且只剩两个 + 强度；
        // 下面这三样是 v1.10.223「渐变形状」模型的残骸，现在不归任何像素管 —— 隐藏，不留死控件。
        for (int id : new int[]{R.id.curve_shadow, R.id.curve_gamma, R.id.tv_curve_gamma}) {
            View dead = findViewById(id);
            if (dead != null) {
                dead.setVisibility(View.GONE);
            }
        }
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser || ConfigActivity.this.curveSyncing) {
                    return;
                }
                ConfigActivity.this.commitCurveFromSliders();
                ConfigActivity.this.renderShadowParams();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        };
        for (int id : CURVE_BARS) {
            SeekBar bar = curveBar(id);
            if (bar != null) {
                bar.setOnSeekBarChangeListener(listener);
            }
        }
        reset.setOnClickListener((v) -> applyShadowPreset(
                ShadowSpec.STRENGTH_MAX, ShadowSpec.DEFAULT.range(), ShadowSpec.DEFAULT.rho()));
        bindPreset(R.id.btn_preset_edge, PRESET_EDGE);
        bindPreset(R.id.btn_preset_wide, PRESET_WIDE);
        bindPreset(R.id.btn_preset_tight, PRESET_TIGHT);
        toggle.setOnClickListener((v) -> {
            boolean open = panel.getVisibility() == View.VISIBLE;
            animateShadowPanel(panel, toggle, !open);
        });
        // 初值：面板在 XML 里是 gone（收起），展开动画的"起点"要一致
        if (panel.getVisibility() != View.VISIBLE) {
            android.view.ViewGroup.LayoutParams lp = panel.getLayoutParams();
            if (lp != null) {
                lp.height = 0;
                panel.setLayoutParams(lp);
            }
            panel.setAlpha(0.0f);
        }
        syncCurveSliders(ShadowSpec.parse(this.selectedShadowCurve));
        syncCurveUi();
        setParamsEnabled(this.pickedUri != null
                || (this.base != null && this.base.getSourceUri().length() > 0));
        this.shadowParamsSig = shadowParamsSig();
    }

    /**
     * v1.10.229：「自定义」面板的**展开 / 收起**动画（用户要的「曲线感动画」）。
     *
     * <h3>曲线</h3>
     * 用项目里的 {@link Ease#IN_OUT}（三次贝塞尔 0.45,0 → 0.40,1，等价 CSS
     * {@code cubic-bezier(.45,0,.4,1)}）。不用系统的 Decelerate：它在 t=0 处速度突跳到最大，
     * 一按下去"猛地窜出来"再收尾，正好是用户说的"没有曲线感"。贝塞尔两端速度都≈0，
     * 中间快、两头软，看起来才是"拉开"而不是"弹出"。
     *
     * <h3>为什么只改高度 + alpha</h3>
     * 面板里是 SeekBar / 按钮，改它们的 margin 或 scale 会让触摸热区跟着动画跑（拖到一半滑条会漂）；
     * 改高度是唯一"结构与热区同时到位"的做法（和 SettingsActivity.animateExpand 同一套，v1.10.139 起）。
     *
     * <h3>成对镜像</h3>
     * 展开与收起用同一条曲线、同一时长（280ms），收起就是展开的时间反演 ——
     * LESSONS：进退场必须成对镜像，否则用户会觉得"关掉的时候很生硬"。
     * 另外按钮自己带一点点回弹（{@link Ease#OVERSHOOT}），只作用在 scale 上，不影响布局。
     */
    private final void animateShadowPanel(@NotNull final View panel, @NotNull final View toggle, final boolean expand) {
        android.animation.ValueAnimator running = this.shadowPanelAnim;
        if (running != null) {
            running.cancel();
            this.shadowPanelAnim = null;
        }
        int width = panel.getWidth() > 0 ? panel.getWidth() : toggle.getWidth();
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        // 量出"完全展开"的高度。容器用 UNSPECIFIED 量高度 —— 子视图不许吃 AT_MOST
        // （AT_MOST 会让 match_parent 的孩子只量到 0，展开后是空的）。
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int target = Math.max(expand ? panel.getMeasuredHeight() : 0, 0);
        int from = panel.getLayoutParams() != null ? panel.getLayoutParams().height : 0;
        if (from < 0) {
            from = expand ? 0 : panel.getHeight();
        }
        final int start = expand ? Math.min(from, target) : Math.max(from, 0);
        if (expand) {
            panel.setVisibility(View.VISIBLE);
        }
        if (start == target) {
            android.view.ViewGroup.LayoutParams lp = panel.getLayoutParams();
            if (lp != null) {
                lp.height = expand ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                panel.setLayoutParams(lp);
            }
            panel.setAlpha(expand ? 1.0f : 0.0f);
            if (!expand) {
                panel.setVisibility(View.GONE);
            }
            return;
        }
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, target);
        animator.setDuration(280L);
        animator.setInterpolator((t) -> Ease.IN_OUT.at(t));
        animator.addUpdateListener((animation) -> {
            float eased = animation.getAnimatedFraction();
            android.view.ViewGroup.LayoutParams lp = panel.getLayoutParams();
            if (lp != null) {
                lp.height = (int) animation.getAnimatedValue();
                panel.setLayoutParams(lp);
            }
            // v1.10.230：alpha 必须**反向**（展开 0→1、收起 1→0）。
            // 上一版两种方向都写成 IN_OUT.at(eased)，收起时第一帧 alpha 就被打成 0，
            // 整块面板先"闪"一下再缩高 —— 用户报的「收起时明显闪烁」就是它。
            panel.setAlpha(expand ? Ease.IN_OUT.at(eased) : 1.0f - Ease.IN_OUT.at(eased));
            // 按钮自己弹一下（只有 scale，不动布局）：展开带 1% 出头的回弹，收起直接落回
            float pop = expand ? Ease.OVERSHOOT.at(eased) : Ease.IN_OUT.at(eased);
            float scale = 1.0f - 0.05f * (1.0f - Math.min(pop, 1.0f));
            toggle.setScaleX(scale);
            toggle.setScaleY(scale);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                android.view.ViewGroup.LayoutParams lp = panel.getLayoutParams();
                if (lp != null) {
                    lp.height = expand ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                    panel.setLayoutParams(lp);
                }
                panel.setAlpha(expand ? 1.0f : 0.0f);
                if (!expand) {
                    panel.setVisibility(View.GONE);
                }
                toggle.setScaleX(1.0f);
                toggle.setScaleY(1.0f);
                ConfigActivity.this.shadowPanelAnim = null;
            }
        });
        this.shadowPanelAnim = animator;
        animator.start();
    }

    private final void updateRotateButtons() {
        Button button = this.btnRotate;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRotate");
            button = null;
        }
        button.setText(getString(R.string.cfg_rotate_content, new Object[]{Integer.valueOf(this.selectedRotation)}));
        Button button2 = this.btnRotateShape;
        if (button2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRotateShape");
            button2 = null;
        }
        button2.setText(getString(R.string.cfg_rotate_mask, new Object[]{Integer.valueOf(this.selectedShapeRotation)}));
    }

    private final boolean fitContentLocked() {
        return this.selectedLayout == LayoutMode.FILL || this.selectedLayout == LayoutMode.FIT;
    }

    private final void refreshFitButton() {
        String string;
        Button button = (Button) findViewById(R.id.btn_fit_content);
        boolean locked = fitContentLocked();
        button.setEnabled(!locked);
        button.setAlpha(locked ? 0.55f : 1.0f);
        if (locked) {
            string = getString(R.string.cfg_fit_auto);
        } else {
            string = this.selectedFitContent ? getString(R.string.cfg_fit_on) : getString(R.string.cfg_fit_off);
        }
        button.setText(string);
        button.setSelected(this.selectedFitContent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshPreviewNow() {
        Runnable it = this.paramTask;
        if (it != null) {
            this.ui.removeCallbacks(it);
        }
        Runnable task = () -> {
            refreshPreviewNow$lambda$26(ConfigActivity.this);
        };
        this.paramTask = task;
        this.ui.postDelayed(task, 0L);
    }

    private static final void refreshPreviewNow$lambda$26(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.10.28（需求 2）：导出中仍要能预览 —— 但必须画「冻结帧」，
        // 按 seek 位置重绘会跟着进度走，看着就是前进/倒退
        if (this$0.busy) {
            int frozen = this$0.previewFrozenIndex;
            if (frozen >= 0 && this$0.previewFrames != null
                    && !this$0.previewFrames.getFrames().isEmpty()) {
                this$0.renderPreviewFrameAt(frozen);
                return;
            }
            // v1.10.51：没有预览帧时退回静态重渲（否则导出中改蒙版/开关毫无反应）
            this$0.renderPreviewNow();
            return;
        }
        PreviewFrames.Result frames = this$0.previewFrames;
        if (frames != null) {
            if (!frames.getFrames().isEmpty()) {
                SeekBar seekBar = this$0.seekPlayback;
                if (seekBar == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
                    seekBar = null;
                }
                this$0.renderPreviewFrameAt(seekBar.getProgress());
                return;
            }
        }
        this$0.schedulePreview();
    }

    /** v1.10.70：统一走 NumDialog（标题 + 圆角输入框 + 紧凑居中的取消/确认，跟图2 一致）。 */
    private final void showNumberDialog(String title, int value, final Function1<? super Integer, Unit> function1) {
        showNumberDialog(title, value, 0, 100, function1);
    }

    /** v1.10.73：可指定范围的版本（等比例缩放是 -100 ~ +100）。 */
    private final void showNumberDialog(String title, final int value, final int min, final int max,
                                        final Function1<? super Integer, Unit> function1) {
        new NumDialog(this, getString(R.string.cfg_number_range, new Object[]{title}),
                String.valueOf(value), min + " ~ " + max, NumDialog.signedIntInputType())
                .onOk(text -> {
                    Integer parsed = StringsKt.toIntOrNull(StringsKt.trim(text).toString());
                    int typed = parsed != null ? parsed.intValue() : value;
                    function1.invoke(Integer.valueOf(RangesKt.coerceIn(typed, min, max)));
                })
                .show();
    }

    private static final void showNumberDialog$lambda$29(EditText $input, int $value, Function1 $onApply, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter($input, "$input");
        Intrinsics.checkNotNullParameter($onApply, "$onApply");
        Integer intOrNull = StringsKt.toIntOrNull(StringsKt.trim($input.getText().toString()).toString());
        int typed = intOrNull != null ? intOrNull.intValue() : $value;
        $onApply.invoke(Integer.valueOf(RangesKt.coerceIn(typed, 0, 100)));
    }

    private final void showRotateDialog(boolean isShape) {
        if (outputRunning()) {
            noteOutputLock();
            showOutputBusyDialog();
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        ThemeColors theme = ThemeManager.INSTANCE.colors(this);
        int original = isShape ? this.selectedShapeRotation : this.selectedRotation;
        SeekBar $this$showRotateDialog_u24lambda_u2430 = new SeekBar(this);
        $this$showRotateDialog_u24lambda_u2430.setMax(720);
        $this$showRotateDialog_u24lambda_u2430.setProgress(original + 360);
        final TextView $this$showRotateDialog_u24lambda_u2431 = new TextView(this);
        $this$showRotateDialog_u24lambda_u2431.setText(getString(R.string.cfg_angle, new Object[]{Integer.valueOf(original)}));
        $this$showRotateDialog_u24lambda_u2431.setGravity(17);
        $this$showRotateDialog_u24lambda_u2431.setPadding(0, (int) (10 * density), 0, (int) (8 * density));
        $this$showRotateDialog_u24lambda_u2431.setTextColor(theme.getText());
        $this$showRotateDialog_u24lambda_u2431.setTextSize(15.0f);
        final EditText $this$showRotateDialog_u24lambda_u2432 = new EditText(this);
        $this$showRotateDialog_u24lambda_u2432.setText(String.valueOf(original));
        $this$showRotateDialog_u24lambda_u2432.setHint("-360 ~ 360");
        $this$showRotateDialog_u24lambda_u2432.setInputType(4098);
        $this$showRotateDialog_u24lambda_u2432.setSingleLine();
        $this$showRotateDialog_u24lambda_u2432.setTextColor(theme.getText());
        $this$showRotateDialog_u24lambda_u2432.setHintTextColor(theme.getTextMuted());
        // v1.10.70：统一走 NumDialog（上方附加「角度文字 + 滑条」，下方圆角输入框，按钮紧凑居中）
        final NumDialog dlg = new NumDialog(this,
                isShape ? getString(R.string.cfg_rotate_mask_title) : getString(R.string.cfg_rotate_content_title),
                String.valueOf(original), "-360 ~ 360", NumDialog.signedIntInputType());
        dlg.addTop($this$showRotateDialog_u24lambda_u2431);
        dlg.addTop($this$showRotateDialog_u24lambda_u2430);
        // v1.10.214：拖动期间**实时渲染**（和透明度滑条同一条链路 refreshPreviewNow），
        // 同时进入隔离态（卡片淡出 + 灰滤镜撤掉，只留滑条）。取消＝撤回，松手/确认＝保留。
        final boolean shapeFlagNow = isShape;
        final boolean[] committed = {false};
        // 「角度 N°」和滑条一起留在隔离态里（结构/位置/动画都和滑条完全同步），
        // 卡片底色、标题、输入框、按钮照常淡出。
        dlg.isolateWhileDrag($this$showRotateDialog_u24lambda_u2431, $this$showRotateDialog_u24lambda_u2430);
        // v1.10.394：切系统深浅色后，「角度 N°」和滑条本来会停在打开那一刻的颜色
        // （弹窗是独立窗口，不在页面的换肤链路里）。这里把它俩的重新上色交给弹窗回调。
        dlg.onThemeChanged(() -> {
            ThemeColors now = ThemeManager.INSTANCE.colors(ConfigActivity.this);
            $this$showRotateDialog_u24lambda_u2431.setTextColor(now.getText());
            int bar = ThemeManager.INSTANCE.sliderSurfaceColor(ConfigActivity.this, now);
            $this$showRotateDialog_u24lambda_u2430.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(bar));
            $this$showRotateDialog_u24lambda_u2430.setThumbTintList(
                    android.content.res.ColorStateList.valueOf(bar));
        });
        $this$showRotateDialog_u24lambda_u2430.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.ConfigActivity.showRotateDialog.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar, int value, boolean fromUser) {
                int angle = value - 360;
                $this$showRotateDialog_u24lambda_u2431.setText(ConfigActivity.this.getString(R.string.cfg_angle, new Object[]{Integer.valueOf(angle)}));
                dlg.input().setText(String.valueOf(angle));
                if (fromUser) {
                    // 所见即所得：先落到字段，再原地重渲一帧（ms 级，和透明度滑条一样）
                    if (shapeFlagNow) {
                        ConfigActivity.this.selectedShapeRotation = angle;
                    } else {
                        ConfigActivity.this.selectedRotation = angle;
                    }
                    ConfigActivity.this.refreshPreviewNow();
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar) {
                dlg.setDragging(true);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar) {
                dlg.setDragging(false);
                ConfigActivity.this.updateRotateButtons();
            }
        });
        final boolean shapeFlag = isShape;
        // 取消/返回键 = 撤回拖动过程中的实时改动（LESSONS 13：取消必须能撤销一切）
        dlg.onDismiss(() -> {
            if (!committed[0]) {
                if (shapeFlagNow) {
                    this.selectedShapeRotation = original;
                } else {
                    this.selectedRotation = original;
                }
                this.updateRotateButtons();
                this.refreshPreviewNow();
            }
        });
        dlg.onOk(text -> {
            // v1.10.497（用户澄清）：输出进行中，这个「确定」就是「点击保存」→ **不落值**，只弹窗。
            // 两个副作用正好都是想要的：
            //   ① NumDialog 的确定是「先回调再关窗」→ 这张面板自己退出（用户说的「退出该界面」）；
            //   ② 没走到 committed，onDismiss 会把拖动过程中的角度撤回（= 不可保存）。
            // 弹窗用 post 延后一拍，免得压在正在关的面板上闪一下。
            if (outputRunning()) {
                ConfigActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        showOutputBusyDialog();
                    }
                });
                return;
            }
            Integer typed = StringsKt.toIntOrNull(StringsKt.trim(text).toString());
            int angle = RangesKt.coerceIn(typed != null ? typed.intValue()
                    : $this$showRotateDialog_u24lambda_u2430.getProgress() - 360, -360, 360);
            if (shapeFlag) {
                this.selectedShapeRotation = angle;
            } else {
                this.selectedRotation = angle;
            }
            committed[0] = true;
            this.updateRotateButtons();
            this.refreshPreviewNow();
        });
        dlg.show();
    }

    private static final void showRotateDialog$lambda$34(EditText $input, SeekBar $seek, boolean $isShape, ConfigActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter($input, "$input");
        Intrinsics.checkNotNullParameter($seek, "$seek");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Integer typed = StringsKt.toIntOrNull(StringsKt.trim($input.getText().toString()).toString());
        int angle = RangesKt.coerceIn(typed != null ? typed.intValue() : $seek.getProgress() - 360, -360, 360);
        if ($isShape) {
            this$0.selectedShapeRotation = angle;
        } else {
            this$0.selectedRotation = angle;
        }
        this$0.updateRotateButtons();
        this$0.refreshPreviewNow();
    }

    private static final void showRotateDialog$lambda$35(boolean $isShape, ConfigActivity this$0, int $original, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if ($isShape) {
            this$0.selectedShapeRotation = $original;
        } else {
            this$0.selectedRotation = $original;
        }
    }

    private final void buildShapeGrid() {
        Iterable iterable;
        LinearLayout container = (LinearLayout) findViewById(R.id.ll_shapes);
        container.removeAllViews();
        Set duplicated = SetsKt.setOf(new ShapeKind[]{ShapeKind.SQUARE_1_1, ShapeKind.ROUND_1_1, ShapeKind.RECT_4_3, ShapeKind.ROUND_4_3, ShapeKind.RECT_3_4, ShapeKind.ROUND_3_4, ShapeKind.RECT_16_9, ShapeKind.RECT_9_16, ShapeKind.RECT_3_2, ShapeKind.RECT_2_3, ShapeKind.RECT_21_9, ShapeKind.RECT_2_1});
        // v1.10.518（用户报「形状里有两个一样的圆形」）：ELLIPSE 的 getAspect() 是 0（随框拉伸），
        // 而选择器格子是方形 —— 于是它和 CIRCLE 画出来是同一个正圆，肉眼就是两个重复项。
        // 处理：**只从选择器里隐掉**，枚举与渲染都不动 —— 老配置若正用着 ELLIPSE，
        // 仍然照原样渲染，且勾选状态照旧能显示（下面 head 的逻辑会把它补回来）。
        Set hiddenShapes = SetsKt.setOf(new ShapeKind[]{ShapeKind.ELLIPSE});
        ShapeKind[] shapeKindArrValues = ShapeKind.values();
        Collection destination$iv$iv = new ArrayList();
        for (ShapeKind shapeKind : shapeKindArrValues) {
            if (!duplicated.contains(shapeKind) && !hiddenShapes.contains(shapeKind)) {
                destination$iv$iv.add(shapeKind);
            }
        }
        Iterable ordered = (List) destination$iv$iv;
        if (this.shapesExpanded) {
            iterable = ordered;
        } else {
            List head = CollectionsKt.toMutableList(CollectionsKt.take(ordered, 12));
            if (!head.contains(this.selectedShape) && !duplicated.contains(this.selectedShape)
                    && !hiddenShapes.contains(this.selectedShape)) {
                if (head.size() >= 12) {
                    head.remove(head.size() - 1);
                }
                head.add(this.selectedShape);
            }
            iterable = head;
        }
        Iterable list = iterable;
        float density = getResources().getDisplayMetrics().density;
        Ref.ObjectRef row = new Ref.ObjectRef();
        Iterable $this$forEachIndexed$iv = list;
        int index$iv = 0;
        for (Object item$iv : $this$forEachIndexed$iv) {
            int index = index$iv;
            index$iv++;
            if (index < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            ShapeKind shape = (ShapeKind) item$iv;
            if (index % 4 == 0) {
                LinearLayout $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2437 = new LinearLayout(this);
                $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2437.setOrientation(0);
                row.element = $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2437;
                container.addView((View) row.element);
            }
            ThemeColors theme = ThemeManager.INSTANCE.colors(this);
            int shapeColor = ThemePrefs.INSTANCE.getShapeFollowSystem(this) ? systemControlColor() : theme.getText();
            ShapeChipView $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440 = new ShapeChipView(this);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setShape(shape);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setAccentColor(theme.getPrimary());
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setBgColor(theme.getNeutral());
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setShapeColor(shapeColor);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setTextColor(theme.getTextMuted());
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setChosen(shape == this.selectedShape);
            LinearLayout.LayoutParams $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440_u24lambda_u2438 = new LinearLayout.LayoutParams(0, (int) (64 * density), 1.0f);
            int m = (int) (3 * density);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440_u24lambda_u2438.setMargins(m, m, m, m);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setLayoutParams($this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440_u24lambda_u2438);
            $this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440.setOnClickListener((v2) -> {
                buildShapeGrid$lambda$41$lambda$40$lambda$39(ConfigActivity.this, shape, v2);
            });
            LinearLayout linearLayout = (LinearLayout) row.element;
            if (linearLayout != null) {
                linearLayout.addView($this$buildShapeGrid_u24lambda_u2441_u24lambda_u2440);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:7:0x0032  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static final void buildShapeGrid$lambda$41$lambda$40$lambda$39(io.github.animwidget.ConfigActivity r4, io.github.animwidget.ShapeKind r5, android.view.View r6) {
        ConfigActivity this$0 = r4;
        ShapeKind $shape = r5;
        /*
            r0 = r4
            java.lang.String r1 = "this$0"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r0, r1)
            r0 = r5
            java.lang.String r1 = "$shape"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r0, r1)
            r0 = r4
            r1 = r5
            r0.selectedShape = r1
            r0 = r4
            r0.buildShapeGrid()
            r0 = r4
            r0.schedulePreview()
            r0 = r4
            io.github.animwidget.PreviewFrames$Result r0 = r0.previewFrames
            r1 = r0
            if (r1 == 0) goto L32
            java.util.List r0 = r0.getFrames()
            r1 = r0
            if (r1 == 0) goto L32
            int r0 = r0.size()
            goto L34
        L32:
            r0 = 0
        L34:
            if (r0 <= 0) goto L4e
            r0 = r4
            r1 = r4
            android.widget.SeekBar r1 = r1.seekPlayback
            r2 = r1
            if (r2 != 0) goto L48
        L41:
            java.lang.String r1 = "seekPlayback"
            kotlin.jvm.internal.Intrinsics.throwUninitializedPropertyAccessException(r1)
            r1 = 0
        L48:
            int r1 = r1.getProgress()
            r0.renderPreviewFrameAt(r1)
        L4e:
            return
        */
        this$0.selectedShape = $shape;
        this$0.buildShapeGrid();
        this$0.schedulePreview();
        int count = 0;
        PreviewFrames.Result frames = this$0.previewFrames;
        if (frames != null && frames.getFrames() != null) {
            count = frames.getFrames().size();
        }
        if (count > 0) {
            SeekBar bar = this$0.seekPlayback;
            if (bar == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
                bar = null;
            }
            this$0.renderPreviewFrameAt(bar.getProgress());
        }
    }

    private final int systemControlColor() {
        int i;
        int fallback = ThemeManager.INSTANCE.colors(this).getText();
        try {
            boolean dark = ThemeManager.INSTANCE.isDark(this);
            ContextThemeWrapper themed = new ContextThemeWrapper(this, dark ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
            TypedArray ta = themed.obtainStyledAttributes(new int[]{android.R.attr.colorControlNormal});
            Intrinsics.checkNotNullExpressionValue(ta, "obtainStyledAttributes(...)");
            int color = ta.getColor(0, fallback);
            ta.recycle();
            i = color == 0 ? fallback : color;
        } catch (Throwable th) {
            i = fallback;
        }
        return i;
    }

    private final void applyTabColors(boolean content) {
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        ((TextView) findViewById(R.id.tab_content)).setTextColor(content ? c.getPrimary() : c.getTextMuted());
        ((TextView) findViewById(R.id.tab_appearance)).setTextColor(content ? c.getTextMuted() : c.getPrimary());
        findViewById(R.id.tab_indicator).setBackgroundColor(c.getPrimary());
    }

    private final void refreshLayoutChips(int[] chips) {
        // v1.8.0：布局改成卡片式横滑选择器，这里只负责把当前选中项同步过去。
        LayoutPickerView picker = this.layoutPicker;
        if (picker != null) {
            picker.setSelected(this.selectedLayout);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x00af  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
   private final void applyKindUi(MediaKind kind) {
      ThemeColors colors = ThemeManager.INSTANCE.colors((Context)this);
      MediaSelectorView var10000 = this.mediaSelector;
      if (this.mediaSelector == null) {
         Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
         var10000 = null;
      }

      var10000.applyColors(colors.getPrimary(), colors.getNeutral(), colors.getText(), colors.getOnPrimary());
      var10000 = this.mediaSelector;
      if (this.mediaSelector == null) {
         Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
         var10000 = null;
      }

      {
         String[] labelArray = new String[3];
         String var10003 = this.getString(2131099696);
         Intrinsics.checkNotNullExpressionValue(var10003, "getString(...)");
         labelArray[0] = var10003;
         var10003 = this.getString(2131099697);
         Intrinsics.checkNotNullExpressionValue(var10003, "getString(...)");
         labelArray[1] = var10003;
         var10003 = this.getString(2131099698);
         Intrinsics.checkNotNullExpressionValue(var10003, "getString(...)");
         labelArray[2] = var10003;
         var10000.setLabels(labelArray);
      }

      boolean hasSource = this.pickedUri != null
            || (this.base != null && this.base.getSourceUri() != null && this.base.getSourceUri().length() > 0);

      var10000 = this.mediaSelector;
      if (this.mediaSelector == null) {
         Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
         var10000 = null;
      }

      var10000.setSelected(hasSource ? kind : null, false);
      boolean enabled = kind != MediaKind.IMAGE;
      SeekBar var12 = this.seekFps;
      if (this.seekFps == null) {
         Intrinsics.throwUninitializedPropertyAccessException("seekFps");
         var12 = null;
      }

      var12.setEnabled(enabled);
      TextView var13 = this.tvFps;
      if (this.tvFps == null) {
         Intrinsics.throwUninitializedPropertyAccessException("tvFps");
         var13 = null;
      }

      var13.setAlpha(enabled ? 1.0F : 0.35F);
      boolean overlayAllowed = kind != MediaKind.IMAGE;
      CheckBox var14 = this.cbOverlay;
      if (this.cbOverlay == null) {
         Intrinsics.throwUninitializedPropertyAccessException("cbOverlay");
         var14 = null;
      }

      var14.setEnabled(overlayAllowed);
      CheckBox var15 = this.cbOverlayVisible;
      if (this.cbOverlayVisible == null) {
         Intrinsics.throwUninitializedPropertyAccessException("cbOverlayVisible");
         var15 = null;
      }

      var15.setEnabled(overlayAllowed);
      if (!overlayAllowed) {
         CheckBox var16 = this.cbOverlay;
         if (this.cbOverlay == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbOverlay");
            var16 = null;
         }

         var16.setChecked(false);
         CheckBox var17 = this.cbOverlayVisible;
         if (this.cbOverlayVisible == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbOverlayVisible");
            var17 = null;
         }

         var17.setChecked(false);
      }

      // v1.10.252：高清 1:1 只对静态图片成立 —— 动图/视频每帧都是 8.8MB 的合成+编码，
      // 开了只会把帧率打死（那条路留在「设置→性能」里给懂的人自己开）。
      View hiresRow = findViewById(R.id.row_hires);
      if (hiresRow != null) {
         hiresRow.setVisibility(kind == MediaKind.IMAGE ? View.VISIBLE : View.GONE);
      }
      // v1.10.260：动图档位只对 GIF/视频有意义
      View animRow = findViewById(R.id.row_anim);
      if (animRow != null) {
         animRow.setVisibility(kind == MediaKind.IMAGE ? View.GONE : View.VISIBLE);
      }
      // v1.10.26（需求 2）：静态图片没有帧率概念 —— 「播放帧率」参数块整个移除（连分隔线一起），
      // 播放进度条同时灰置（原来只有滑块被禁用，进度条还是亮的）
      this.setFpsBlockVisible(kind != MediaKind.IMAGE);
      SeekBar playbackSeek = this.seekPlayback;
      if (playbackSeek != null) {
         playbackSeek.setAlpha(this.playbackSeekAlpha());
      }
      this.updateMediaVisibility();
   }

   /**
    * v1.10.26（需求 2）：按素材类型显示/移除「播放帧率」参数块。
    * 该块在布局里没有 id，用 tv_fps 的父容器定位，顺带把紧邻的分隔线一起收起来
    * （否则卡片里会出现两条挨着的横线）。
    */
   private final void setFpsBlockVisible(boolean visible) {
      TextView fpsLabel = this.tvFps;
      if (fpsLabel == null) {
         return;
      }
      ViewGroup parent = fpsLabel.getParent() instanceof ViewGroup ? (ViewGroup) fpsLabel.getParent() : null;
      if (parent == null) {
         return;
      }
      parent.setVisibility(visible ? View.VISIBLE : View.GONE);
      ViewParent grand = parent.getParent();
      if (grand instanceof ViewGroup) {
         ViewGroup card = (ViewGroup) grand;
         int idx = card.indexOfChild(parent);
         if (idx >= 0 && idx + 1 < card.getChildCount()) {
            View next = card.getChildAt(idx + 1);
            if (next != null && "divider".contentEquals(String.valueOf(next.getTag()))) {
               next.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
         }
      }
   }

    /* JADX WARN: Removed duplicated region for block: B:15:0x0037  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final void updateMediaVisibility() {
        /*
            r3 = this;
            r0 = r3
            io.github.animwidget.WidgetConfig r0 = r0.base
            r1 = r0
            if (r1 != 0) goto La
        L9:
            return
        La:
            r4 = r0
            r0 = r3
            io.github.animwidget.MediaKind r0 = r0.pickedKind
            r1 = r0
            if (r1 != 0) goto L18
        L14:
            r0 = r4
            io.github.animwidget.MediaKind r0 = r0.getKind()
        L18:
            r5 = r0
            r0 = r3
            android.net.Uri r0 = r0.pickedUri
            if (r0 != 0) goto L37
            r0 = r4
            java.lang.String r0 = r0.getSourceUri()
            java.lang.CharSequence r0 = (java.lang.CharSequence) r0
            int r0 = r0.length()
            if (r0 <= 0) goto L33
            r0 = 1
            goto L34
        L33:
            r0 = 0
        L34:
            if (r0 == 0) goto L3b
        L37:
            r0 = 1
            goto L3c
        L3b:
            r0 = 0
        L3c:
            r6 = r0
            r0 = r6
            if (r0 == 0) goto L4c
            r0 = r5
            io.github.animwidget.MediaKind r1 = io.github.animwidget.MediaKind.IMAGE
            if (r0 == r1) goto L4c
            r0 = 1
            goto L4d
        L4c:
            r0 = 0
        L4d:
            r7 = r0
            r0 = r7
            if (r0 == 0) goto L58
            r0 = 0
            goto L5a
        L58:
            r0 = 8
        L5a:
            r8 = r0
            r0 = r3
            r1 = 2131230720(0x7f080000, float:1.80775E38)
            android.view.View r0 = r0.findViewById(r1)
            r9 = r0
            r0 = r3
            r1 = 2131230723(0x7f080003, float:1.8077507E38)
            android.view.View r0 = r0.findViewById(r1)
            r10 = r0
            r0 = r3
            r1 = 2131230735(0x7f08000f, float:1.8077531E38)
            android.view.View r0 = r0.findViewById(r1)
            r11 = r0
            r0 = r3
            r1 = 2131230726(0x7f080006, float:1.8077513E38)
            android.view.View r0 = r0.findViewById(r1)
            r12 = r0
            r0 = r9
            r1 = 0
            r0.setVisibility(r1)
            r0 = r10
            int r0 = r0.getVisibility()
            if (r0 == 0) goto La0
            r0 = r10
            r1 = 0
            r0.setVisibility(r1)
            r0 = r9
            r1 = r3
            void r1 = () -> { // java.lang.Runnable.run():void
                updateMediaVisibility$lambda$43(ConfigActivity.this);
            }
            boolean r0 = r0.post(r1)
        La0:
            r0 = r11
            int r0 = r0.getVisibility()
            r1 = r8
            if (r0 == r1) goto Lb1
            r0 = r11
            r1 = r8
            r0.setVisibility(r1)
        Lb1:
            r0 = r6
            if (r0 != 0) goto Lbc
            r0 = r12
            r1 = 8
            r0.setVisibility(r1)
        Lbc:
            return
        */
        WidgetConfig base = this.base;
        if (base == null) {
            return;
        }
        MediaKind kind = this.pickedKind != null ? this.pickedKind : base.getKind();
        boolean hasSource = this.pickedUri != null || base.getSourceUri().length() > 0;
        int sectionVisibility = (hasSource && kind != MediaKind.IMAGE) ? View.VISIBLE : View.GONE;
        View previewFrame = findViewById(R.id.preview_frame);
        View previewBox = findViewById(R.id.preview_box);
        View previewSection = findViewById(R.id.ll_preview_section);
        View progress = findViewById(R.id.pb_preview);
        if (previewFrame != null) {
            previewFrame.setVisibility(View.VISIBLE);
        }
        if (previewBox != null && previewBox.getVisibility() != View.VISIBLE) {
            previewBox.setVisibility(View.VISIBLE);
            if (previewFrame != null) {
                previewFrame.post(() -> updateMediaVisibility$lambda$43(this));
            }
        }
        if (previewSection != null && previewSection.getVisibility() != sectionVisibility) {
            previewSection.setVisibility(sectionVisibility);
        }
        if (progress != null && !hasSource) {
            progress.setVisibility(View.GONE);
        }
        setParamsEnabled(hasSource);
    }

    /** 预览解码采样：长边压到 1600 以内（只读 bounds，不再整图解码）。 */
    private static final int previewSample(ConfigActivity activity, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream probe = activity.getContentResolver().openInputStream(uri);
            if (probe != null) {
                BitmapFactory.decodeStream(probe, null, bounds);
                probe.close();
            }
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (longest / (sample * 2) >= 1600) {
                sample *= 2;
            }
            return sample;
        } catch (Throwable th) {
            return 2;
        }
    }

    private static final View findPicker(android.view.ViewGroup root) {
        if (root == null) {
            return null;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof LayoutPickerView) {
                return child;
            }
            if (child instanceof android.view.ViewGroup) {
                View found = findPicker((android.view.ViewGroup) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 「画面」下的三个参数：没有素材时整体灰置（半透明 + 不可拖）。 */
    private final void setParamsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.35f;
        // v1.8.19：fps 只对 GIF/视频有意义 —— 图片素材必须灰置
        // （原来这里按 hasSource 无条件重新打开，把 applyKindUi 的灰置覆盖了）
        MediaKind kind = this.pickedKind != null ? this.pickedKind
                : (this.base != null ? this.base.getKind() : null);
        boolean fpsEnabled = enabled && kind != MediaKind.IMAGE;
        applyEnabled(this.seekFps, this.tvFps, fpsEnabled, fpsEnabled ? 1.0f : 0.35f);
        applyEnabled(this.seekEdge, this.tvEdge, enabled, alpha);
        applyEnabled(this.seekQuality, this.tvQuality, enabled, alpha);
        // v1.10.74：**画面下方这三条在没有素材时也没有任何意义**（原来只有上面三条灰置，
        // 这三条照样能拖，拖了画面也没反应）。一起灰置，语义才一致。
        applyEnabled(this.seekAlpha, this.tvAlpha, enabled, alpha);
        applyEnabled(this.seekShadow, this.tvShadow, enabled, alpha);
        applyEnabled(this.seekScale, this.tvScale, enabled, alpha);
        TextView scaleHint = (TextView) findViewById(R.id.tv_scale_hint);
        if (scaleHint != null) {
            scaleHint.setAlpha(alpha);
        }
        // v1.10.222/224：剖面面板里的 3 个滑块 / 名称 / 重置 / 三档预设 / 只读剖面图 也必须跟着灰置 ——
        // 221 只把它们建了出来、没接进这条链路，于是没导素材时还能拖（拖了画面当然没反应）。
        // 样式与上面完全一致：setEnabled(false) + alpha 0.35。
        for (int i = 0; i < CURVE_BARS.length; i++) {
            SeekBar bar = (SeekBar) findViewById(CURVE_BARS[i]);
            TextView label = (TextView) findViewById(CURVE_LABELS[i]);
            applyEnabled(bar, label, enabled, alpha);
        }
        for (int id : new int[]{R.id.btn_curve_reset, R.id.btn_preset_edge,
                R.id.btn_preset_wide, R.id.btn_preset_tight}) {
            View button = findViewById(id);
            if (button != null) {
                button.setEnabled(enabled);
                button.setAlpha(alpha);
            }
        }
        for (int id : new int[]{R.id.curve_shadow, R.id.tv_curve_width, R.id.tv_curve_hint, R.id.tv_curve_off}) {
            View v = findViewById(id);
            if (v != null) {
                v.setEnabled(enabled);
                v.setAlpha(alpha);
            }
        }
    }

    private static final void applyEnabled(SeekBar bar, TextView label, boolean enabled, float alpha) {
        if (bar != null) {
            bar.setEnabled(enabled);
            bar.setAlpha(alpha);
        }
        if (label != null) {
            label.setEnabled(enabled);
            label.setAlpha(alpha);
        }
    }

    private static final void updateMediaVisibility$lambda$43(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.updatePreviewBox();
    }

    private final float mediaAspect() {
        // v1.7.0：原来这里优先读 imgPreview 自己的宽高，导致「预览框量出来的是自己」，
        // 换素材/换布局永远不会让框变形。现在统一按布局+素材算。
        return previewRatio();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
    public final void pick(MediaKind kind) throws NoWhenBranchMatchedException {
        // v1.10.310（用户硬性要求：**导出时不得导入素材**）：
        // 导出期间换素材会把帧表/代次搅在一起（旧的抽取线程还在跑、新素材又要落盘），
        // 之前靠 busy 只挡住了"重新播放预览"，没挡导入入口。这里统一挡在入口。
        if (this.busy) {
            // 只 toast，不打新日志：门禁 [8]「一个代号一种含义」，T07 已有既定文本。
            toast("导出中，暂不能导入素材");
            return;
        }
        String str;
        this.pendingKind = kind;
        // v1.10.30：动图页改用内置选择器 —— 系统选择器只能按 MIME 过滤，
        // 而 APNG 的 MIME 是 image/png、动图 WebP 是 image/webp，和普通图完全一样。
        // 内置选择器扫相册后读文件头判定「真会动」，所以这里只列动图。
        if (kind == MediaKind.GIF) {
            try {
                Intent intent = new Intent(this, AnimatedPickerActivity.class);
                startActivityForResult(intent, REQUEST_PICK);
            } catch (Throwable t) {
                String message = t.getMessage();
                if (message == null) {
                    message = getString(R.string.err_no_frames);
                }
                toast(message);
            }
            return;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[kind.ordinal()]) {
            case 1:
                str = "video/*";
                break;
            case 2:
                // v1.10.29：GIF 页回到 image/gif —— 放开到 image/* 后相册里所有截图/照片
                // 都能点（用户明确说「不该看到普通图片」）。APNG / 动图 WebP 改从「图片」页进，
                // 那边选到动图会自动按动图导入（M10）。
                str = "image/gif";
                break;
            case 3:
                str = "image/*";
                break;
            default:
                throw new NoWhenBranchMatchedException();
        }
        String mime = str;
        // v1.9.4：参考 Material Photo Widget —— 用 GET_CONTENT 而非 OPEN_DOCUMENT：
        // vivo 的 DocumentsUI 无视 MIME 什么都列，而 GET_CONTENT 会走系统相册选择器，
        // 按 MIME 真正过滤（GIF 页不再出现视频）。
        Intent $this$pick_u24lambda_u2444 = new Intent("android.intent.action.GET_CONTENT");
        $this$pick_u24lambda_u2444.addCategory("android.intent.category.OPENABLE");
        $this$pick_u24lambda_u2444.setType(mime);
        $this$pick_u24lambda_u2444.addFlags(65);
        try {
            startActivityForResult($this$pick_u24lambda_u2444, REQUEST_PICK);
        } catch (Throwable t) {
            String message = t.getMessage();
            if (message == null) {
                message = getString(R.string.err_no_frames);
                Intrinsics.checkNotNullExpressionValue(message, "getString(...)");
            }
            toast(message);
        }
    }

    @Override // android.app.Activity
    @Deprecated(message = "ActivityResult 需要 AndroidX，这里刻意零依赖")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        MediaKind mediaKind;
        MediaKind kind = null;
        boolean z;
        String sourceUri;
        Uri uri;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK || resultCode != -1) {
            if (requestCode == REQUEST_PICK) {
                if (this.pickedUri != null) {
                    mediaKind = this.pickedKind;
                } else {
                    WidgetConfig widgetConfig = this.base;
                    if (widgetConfig == null || (kind = widgetConfig.getKind()) == null) {
                        mediaKind = null;
                    } else {
                        WidgetConfig widgetConfig2 = this.base;
                        if (widgetConfig2 == null || (sourceUri = widgetConfig2.getSourceUri()) == null) {
                            z = false;
                        } else {
                            z = sourceUri.length() > 0;
                        }
                        mediaKind = z ? kind : null;
                    }
                }
                MediaKind confirmed = mediaKind;
                MediaSelectorView mediaSelectorView = this.mediaSelector;
                if (mediaSelectorView == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
                    mediaSelectorView = null;
                }
                mediaSelectorView.setSelected(confirmed, true);
                return;
            }
            return;
        }
        if (data == null || (uri = data.getData()) == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, 1);
        } catch (Throwable th) {
        }
        // 反编译把 lambda 捕获的变量搞错了（捕获了恒为 null 的 kind），
        // 真机选完素材立刻 NPE。这里显式解析出一个非 null 的 kind 再往下传。
        MediaKind pending = this.pendingKind;
        if (pending == null) {
            WidgetConfig currentConfig = this.base;
            pending = currentConfig != null ? currentConfig.getKind() : MediaKind.VIDEO;
        }
        MediaKind resolved = pending != null ? pending : MediaKind.VIDEO;
        // v1.10.2：同一个素材的重复回调（选择器偶发回两次/双击）直接忽略，
        // 否则会同时跑两条抽帧，互相作废 → 预览永远停在首帧
        long nowPick = System.currentTimeMillis();
        if (this.pickedUri != null && this.pickedUri.equals(uri) && nowPick - this.lastPickAt < 2000L) {
            Diag.warn(this, "M08", "重复的选取回调，忽略（同一素材 " + (nowPick - this.lastPickAt) + "ms 内）");
            return;
        }
        this.lastPickAt = nowPick;
        // v1.9.5：链路编号 —— M03 进来了，M04 嗅探结果
        Diag.warn(this, "M03", "onActivityResult 进入, 所选=" + resolved);
        // v1.8.15：不信选择器的 MIME（GIF 页也能选中 MP4）—— 读文件头魔数。
        // v1.8.18：GIF 页选了视频 → 直接拒绝（用户要求：GIF 页绝不能出现视频）。
        String sniffed = MediaSniff.sniff(this, uri);
        Diag.warn(this, "M04", "嗅探结果: " + sniffed);
        // v1.10.28（需求 3/4）：GIF 页改成「不是动图就拒」——
        // 原来只比对 video/image 两个字符串，嗅探不出来的格式（null）会直接放行，
        // 于是静态图能混进动图页。现在用「嗅探 + ImageDecoder 兜底」统一判定。
        boolean gifPageNotAnim = resolved == MediaKind.GIF
                && ("video".equals(sniffed) || !MediaSniff.isAnimatedImage(this, uri, sniffed));
        if (gifPageNotAnim) {
            Diag.warn(this, "M02", "GIF 页拒绝非动图文件: 所选=" + resolved + " 实际=" + sniffed);
            String gifOnly = getString(R.string.cfg_gif_only);
            Intrinsics.checkNotNullExpressionValue(gifOnly, "getString(...)");
            toast(gifOnly);
            // 恢复选择器到之前的状态
            MediaKind confirmed = this.pickedUri != null ? this.pickedKind : null;
            if (confirmed == null) {
                WidgetConfig widgetConfig = this.base;
                if (widgetConfig != null && widgetConfig.getSourceUri() != null
                        && widgetConfig.getSourceUri().length() > 0) {
                    confirmed = widgetConfig.getKind();
                }
            }
            MediaSelectorView mediaSelectorView = this.mediaSelector;
            if (mediaSelectorView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("mediaSelector");
                mediaSelectorView = null;
            }
            mediaSelectorView.setSelected(confirmed, true);
            return;
        }
        // v1.10.30：动图有专门的内置选择器了，图片页恢复「拒收动图」——
        // 避免把动图当静态图悄悄收下（那才是真正的坑）。
        if (resolved == MediaKind.IMAGE && MediaSniff.isAnimatedImage(this, uri, sniffed)) {
            Diag.warn(this, "M09", "图片页拒绝动图: 实际=" + sniffed);
            toast(getString(R.string.cfg_image_only));
            MediaKind confirmed = this.pickedUri != null ? this.pickedKind : null;
            if (confirmed == null) {
                WidgetConfig wc = this.base;
                if (wc != null && wc.getSourceUri() != null && wc.getSourceUri().length() > 0) {
                    confirmed = wc.getKind();
                }
            }
            MediaSelectorView selectorView = this.mediaSelector;
            if (selectorView != null) {
                selectorView.setSelected(confirmed, true);
            }
            return;
        }
        MediaKind bySniff = MediaSniff.kindOf(sniffed, resolved);
        if (bySniff != resolved) {
            Diag.warn(this, "M01", "嗅探改判: 所选=" + resolved + " 实际=" + sniffed + " → 按 " + bySniff + " 处理");
        }
        final MediaKind resolvedKind = bySniff;
        // v1.8.7：换成新素材时先把旧预览帧扔掉，否则抽帧还没完成时
        // 「手势确认 → refreshPreviewNow」会渲染回上一张素材。
        clearPreviewFrames();
        // v1.9.2：清掉上一个素材的探测数据 —— 否则比例/线框沿用旧素材（切视频→动图时错乱）
        this.sourceWidth = 0;
        this.sourceHeight = 0;
        this.sourceFps = null;
        this.sourceDurationMs = 0L;
        this.sourceFpsX10 = 0;
        this.sourceFrames = 0;
        this.sourceFpsEstimated = false;
        this.fpsHintShown = false;
        this.aspectSignature = null;
        this.pickedUri = uri;
        this.pickedKind = resolvedKind;
        this.pickedName = displayName(uri);
        this.selectedContentScale = 100;
        this.selectedOffsetX = 0.0f;
        this.selectedOffsetY = 0.0f;
        this.selectedMaskX = 0.0f;
        this.selectedMaskY = 0.0f;
        // v1.8.14：新素材一切从头 —— 两页滚动位置清零、回到内容页顶部
        this.scrollContent = 0;
        this.scrollAppearance = 0;
        ScrollView svPick = configScrollView();
        if (svPick != null) {
            svPick.scrollTo(0, 0);
        }
        LinearLayout lcPick = (LinearLayout) findViewById(R.id.ll_content);
        LinearLayout laPick = (LinearLayout) findViewById(R.id.ll_appearance);
        View indPick = findViewById(R.id.tab_indicator);
        if (lcPick != null && laPick != null && indPick != null) {
            onCreate$selectTab$default(lcPick, laPick, this, indPick, true, false, 32, null);
        }
        loadPreview(uri, resolvedKind);
        MediaKind kind2 = resolvedKind;
        TextView textView = this.tvPick;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvPick");
            textView = null;
        }
        Object[] objArr = new Object[1];
        String str = this.pickedName;
        if (str == null) {
            str = "";
        }
        objArr[0] = str;
        textView.setText(getString(R.string.cfg_picked, objArr));
        applyKindUi(kind2);
        // v1.9.3：新素材默认压缩质量 100%
        SeekBar seekQ = this.seekQuality;
        if (seekQ == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekQ = null;
        }
        seekQ.setProgress(50);
        updateLabels();
        // v1.9.5：基本参数立即给出「读取中」占位，探测完成后填真值
        TextView tvResNow = this.tvResolution;
        if (tvResNow != null) {
            tvResNow.setText(getString(R.string.cfg_reading_info));
        }
        // v1.10.0：中央「预览」二字立刻隐藏（素材已经导入，不该再提示空状态）
        updateEmptyHint();
        // v1.9.5：探测看门狗 —— 2.5 秒内探测没完成就先用当前参数开抽，
        // 解析条/播放绝不再依赖探测链。探测后参数不同再作废重抽。
        this.probeDone = false;
        this.infoFilled = false;
        // v1.9.6：探测代次（旧素材的迟到探测一律作废）
        this.probeGeneration++;
        final int probeGen = this.probeGeneration;
        // v1.10.0：只读文件头（≤128KB）先把尺寸顶上去 —— 大动图也不必等整份读完
        final int headerGen = probeGen;
        new Thread(() -> {
            int[] size = SourceFps.headerSize(ConfigActivity.this, uri);
            if (size == null) {
                return;
            }
            final int w = size[0];
            final int h = size[1];
            this.ui.post(() -> {
                if (headerGen != ConfigActivity.this.probeGeneration
                        || ConfigActivity.this.pickedUri != uri || ConfigActivity.this.probeDone) {
                    return;
                }
                TextView tv = ConfigActivity.this.tvResolution;
                if (tv != null) {
                    tv.setText(ConfigActivity.this.getString(R.string.cfg_res_reading,
                            new Object[]{w + "x" + h}));
                }
                ConfigActivity.this.sourceWidth = w;
                ConfigActivity.this.sourceHeight = h;
                ConfigActivity.this.updatePreviewBox();
                Diag.warn(ConfigActivity.this, "K02", "头部尺寸 " + w + "x" + h);
            });
        }).start();

        this.ui.postDelayed(() -> {
            if (!ConfigActivity.this.probeDone && ConfigActivity.this.pickedUri == uri
                    && probeGen == ConfigActivity.this.probeGeneration) {
                Diag.warn(ConfigActivity.this, "M06", "探测超时，先行开抽");
                ConfigActivity.this.loadPreviewFrames(uri, resolvedKind);
            }
        }, 2500L);
        // v1.9.3：探测已全部字节化（GIF 不再走 Movie），速度快且不会挂 ——
        // 统一等探测返回后按正确参数一次性解析（消灭「连续解析两遍」）
        final MediaKind finalKind = resolvedKind;
        new Thread(() -> {
            try {
                onActivityResult$lambda$48(finalKind, ConfigActivity.this, uri, probeGen);
            } catch (Throwable t) {
                // 探素材信息失败（编码器/URI 不配合）不该把整个 App 带走
                CrashLog.save(ConfigActivity.this, Thread.currentThread(), t);
                final String message = t.getClass().getSimpleName();
                this.ui.post(() -> ConfigActivity.this.toast(
                        ConfigActivity.this.getString(R.string.status_failed, new Object[]{message})));
            }
        }).start();
    }

    private static final void onActivityResult$lambda$48(MediaKind $kind, ConfigActivity this$0, Uri $uri, int $gen) throws IOException {
        Integer numVideo = null;
        int fpsX10 = 0;
        int frames = 0;
        boolean estimated = false;
        String resolution = null;
        Long duration = null;
        if ($kind == MediaKind.VIDEO) {
            Diag.warn(this$0, "V02", "视频: 开始探测元数据");
        }
        if ($kind == MediaKind.GIF) {
            // v1.10.0：一次读全 + 一次解析（尺寸/帧数/时长/帧率/每帧延时全从字节里来）
            try {
                // v1.10.498：GIF 改**流式**探测 —— 210MB 的素材在这里 readAll 就先 OOM 了，
                // 用户选完图界面直接停在「读取中」。APNG / 动图 WebP 仍走整份读取。
                String sniffPick = MediaSniff.sniff(this$0, $uri);
                SourceFps.Anim anim;
                if ("gif".equals(sniffPick)) {
                    anim = SourceFps.probeAnimGifStream(this$0, $uri);
                } else {
                    byte[] animBytes = SourceFps.readAll(this$0, $uri);
                    anim = animBytes != null ? SourceFps.probeAnim(animBytes) : null;
                }
                if (anim != null) {
                    fpsX10 = anim.fpsX10;
                    frames = anim.frames;
                    estimated = anim.estimated;
                    if (anim.width > 0 && anim.height > 0) {
                        resolution = anim.width + "x" + anim.height;
                    }
                    if (anim.durationMs > 0) {
                        duration = Long.valueOf(anim.durationMs);
                    }
                }
            } catch (Throwable th) {
                CrashLog.save(this$0, Thread.currentThread(), th);
            }
            if (fpsX10 <= 0) {
                Diag.warn(this$0, "K03", "动图: 读不到每帧时长，帧率按 10fps 兜底");
                fpsX10 = 100;
                estimated = true;
            }
            numVideo = Integer.valueOf(Math.max(Math.round(fpsX10 / 10.0f), 1));
        } else {
            try {
                numVideo = $kind == MediaKind.VIDEO ? SourceFps.INSTANCE.video(this$0, $uri) : null;
            } catch (Throwable th) {
                CrashLog.save(this$0, Thread.currentThread(), th);
            }
            if (numVideo != null && numVideo.intValue() > 0) {
                fpsX10 = numVideo.intValue() * 10;
            }
            try {
                resolution = SourceFps.INSTANCE.resolution(this$0, $uri, $kind);
            } catch (Throwable th) {
                CrashLog.save(this$0, Thread.currentThread(), th);
            }
            try {
                duration = SourceFps.INSTANCE.duration(this$0, $uri, $kind);
            } catch (Throwable th) {
                CrashLog.save(this$0, Thread.currentThread(), th);
            }
        }
        Integer fps = numVideo;
        final Integer fpsFinal = fps;
        final int fpsX10Final = fpsX10;
        final int framesFinal = frames;
        final boolean estimatedFinal = estimated;
        final String resolutionFinal = resolution;
        final Long durationFinal = duration;
        if ($kind == MediaKind.VIDEO) {
            Diag.warn(this$0, "V03", "视频: 元数据 fpsX10=" + fpsX10 + " 尺寸=" + resolution + " 时长=" + duration);
        } else if ($kind == MediaKind.GIF) {
            Diag.warn(this$0, "G12", "GIF: 元数据 fpsX10=" + fpsX10 + " 尺寸=" + resolution
                    + " 时长=" + duration + " 帧数=" + frames + (estimated ? " (推算)" : ""));
        }
        this$0.ui.post(() -> {
            onActivityResult$lambda$48$lambda$47(this$0, resolutionFinal, fpsFinal, fpsX10Final, framesFinal,
                    estimatedFinal, durationFinal, $uri, $kind, $gen);
        });
    }

    private static final void onActivityResult$lambda$48$lambda$47(ConfigActivity this$0, String $resolution,
                                                                  Integer $fps, int $fpsX10, int $frames,
                                                                  boolean $estimated, Long $duration,
                                                                  Uri $uri, MediaKind $kind, int $gen) {
        List parts;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($uri, "$uri");
        Intrinsics.checkNotNullParameter($kind, "$kind");
        // v1.9.6：旧素材的迟到探测作废（否则会用旧 URI 重新开抽，把新素材的解析挤掉）
        if ($gen != this$0.probeGeneration) {
            Diag.warn(this$0, "M07", "迟到探测作废 gen=" + $gen + " 当前=" + this$0.probeGeneration);
            return;
        }
        this$0.probeDone = true;
        Diag.warn(this$0, "M05", "探测完成 fpsX10=" + $fpsX10 + " 尺寸=" + $resolution
                + " 时长=" + $duration + " 帧数=" + $frames + ($estimated ? " 推算" : ""));
        if ($resolution != null || $fps != null || $duration != null) {
            this$0.infoFilled = true;
        }
        this$0.sourceFpsX10 = $fpsX10;
        this$0.sourceFrames = $frames;
        this$0.sourceFpsEstimated = $estimated;
        TextView textView = this$0.tvResolution;
        if (textView != null) {
            textView.setText(this$0.buildSourceInfoFull($resolution, $fpsX10, $frames,
                    $duration != null ? $duration.longValue() : 0L, $estimated));
        }
        // v1.10.133：只有**视频**会按 40s 截断；动图不再截（靠帧数上限管规模），别吓人
        if ($duration != null && $duration.longValue() > 40000 && $kind == MediaKind.VIDEO) {
            String string = this$0.getString(R.string.cfg_too_long);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
        }
        this$0.sourceFps = $fps;
        this$0.sourceDurationMs = $duration != null ? $duration.longValue() : 0L;
        if ($resolution != null && (parts = StringsKt.split($resolution, new String[]{"x"}, false, 0)) != null && parts.size() == 2) {
            Integer intOrNull = StringsKt.toIntOrNull((String) parts.get(0));
            this$0.sourceWidth = intOrNull != null ? intOrNull.intValue() : 0;
            Integer intOrNull2 = StringsKt.toIntOrNull((String) parts.get(1));
            this$0.sourceHeight = intOrNull2 != null ? intOrNull2.intValue() : 0;
        }
        this$0.applyAutoParams();
        this$0.updatePreviewBox();
        // v1.9.5：探测完成后 —— 看门狗若已开抽（探测慢），只在参数变了时才作废重抽；
        // 否则这是首次解析（探测快，按正确参数一次抽好）
        if (this$0.previewFramesLoading) {
            this$0.refillIfParamsChanged($uri, $kind);
        } else {
            this$0.loadPreviewFrames($uri, $kind);
        }
        if ($fps != null && $fps.intValue() > 0) {
            TextView textView2 = this$0.tvPick;
            Object[] objArr = new Object[]{this$0.pickedName != null ? this$0.pickedName : "", $fps};
            if (textView2 != null) {
                textView2.setText(this$0.getString(R.string.cfg_picked_fps, objArr));
            }
        }
        if ($estimated && !this$0.fpsHintShown) {
            this$0.fpsHintShown = true;
            String hint = this$0.getString(R.string.cfg_fps_estimated_hint);
            this$0.toast(hint);
        }
    }

    /* JADX WARN: Failed to calculate best type for var: r11v0 ??
    java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "changeArg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.moveListener(TypeUpdate.java:439)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runListeners(TypeUpdate.java:232)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:212)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.updateTypeForSsaVar(TypeUpdate.java:183)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.updateTypeChecked(TypeUpdate.java:112)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:83)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:56)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.calculateFromBounds(FixTypesVisitor.java:156)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.setBestType(FixTypesVisitor.java:133)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.deduceType(FixTypesVisitor.java:238)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryDeduceTypes(FixTypesVisitor.java:221)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:91)
     */
    /* JADX WARN: Failed to calculate best type for var: r11v0 ??
    java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.InsnArg.getType()" because "changeArg" is null
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.moveListener(TypeUpdate.java:439)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.runListeners(TypeUpdate.java:232)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.requestUpdate(TypeUpdate.java:212)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.updateTypeForSsaVar(TypeUpdate.java:183)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.updateTypeChecked(TypeUpdate.java:112)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:83)
    	at jadx.core.dex.visitors.typeinference.TypeUpdate.apply(TypeUpdate.java:56)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.calculateFromBounds(TypeInferenceVisitor.java:145)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.setBestType(TypeInferenceVisitor.java:123)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.lambda$runTypePropagation$2(TypeInferenceVisitor.java:101)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runTypePropagation(TypeInferenceVisitor.java:101)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:75)
     */
    /* JADX WARN: Multi-variable type inference failed. Error: java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.RegisterArg.getSVar()" because the return value of "jadx.core.dex.nodes.InsnNode.getResult()" is null
    	at jadx.core.dex.visitors.typeinference.AbstractTypeConstraint.collectRelatedVars(AbstractTypeConstraint.java:31)
    	at jadx.core.dex.visitors.typeinference.AbstractTypeConstraint.<init>(AbstractTypeConstraint.java:19)
    	at jadx.core.dex.visitors.typeinference.TypeSearch$1.<init>(TypeSearch.java:376)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.makeMoveConstraint(TypeSearch.java:376)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.makeConstraint(TypeSearch.java:361)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.collectConstraints(TypeSearch.java:341)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:60)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.runMultiVariableSearch(FixTypesVisitor.java:116)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:91)
     */
    /* JADX WARN: Not initialized variable reg: 11, insn: 0x008d: MOVE (r1 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r11 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]), block:B:23:0x008d */
    /* JADX WARN: Type inference failed for: r0v4, types: [android.content.ContentResolver] */
    /* JADX WARN: Type inference failed for: r11v0, types: [java.lang.Throwable] */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.io.Closeable, java.lang.String[]] */
    private final String displayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex("_display_name");
                if (column >= 0) {
                    String value = cursor.getString(column);
                    return value == null ? "" : value;
                }
            }
        } catch (Throwable th) {
            // ignore
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Throwable ignored) { }
            }
        }
        String last = uri.getLastPathSegment();
        return last == null ? "" : last;
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0031  */
    /**
     * 保存配置（= 导出）。
     *
     * v1.10.1 重写，对齐「导出期间可调整」：
     * • 需要重新解析的参数（帧率/清晰度/质量）变化 → 取消旧任务（连暂存一起删）、
     *   用最新参数重开、进度归 0、再次轻提示；
     * • 不需要重新解析的参数（布局/形状/外观…）→ 不碰任务，只影响保存时的取值；
     * • 保存时**重新读一遍界面**（buildConfigFromUi）：保存的布局 = 最后选的布局；
     * • 旧任务哪怕刚好跑完，代次不符也不会保存、也不会显示「完成」。
     */
    private final void save() {
        WidgetConfig base = this.base;
        if (base == null) {
            return;
        }
        // v1.10.454（用户："怎么解析时还不能导出了"）：**闸门只针对「本轮导出还在写盘」这一段。**
        //
        // 452 这里是 `busy || parseTaskActive` —— 而 `parseTaskActive` 在**预览解析**时就是 true
        // （选完素材正在抽预览帧），于是"选完立刻点保存"被静默吞掉，按钮还是亮的，用户以为坏了。
        // 真正需要挡的只有一件事：这一轮导出还没结束（帧正往 `frames.new` 里写），
        // 这时重开一轮会把刚写好的帧清掉。撑不上 200ms 的窗口，用 `exportRunning` 精确表达。
        if (this.exportRunning) {
            Diag.warn(this, "W29", "保存被忽略：本轮导出还在写盘（不重开一轮，等它收尾）gen="
                    + this.exportGeneration);
            return;
        }
        // v1.10.54：刚导出完、还没提交 → 这一下就是「确认保存」
        if (this.pendingExport != null) {
            commitPendingExport();
            return;
        }
        MediaKind kind = this.pickedKind != null ? this.pickedKind : base.getKind();
        String sourceUri = this.pickedUri != null ? this.pickedUri.toString() : base.getSourceUri();
        if (sourceUri == null || sourceUri.length() == 0) {
            toast(getString(R.string.cfg_need_source));
            return;
        }
        // v1.10.252：**先落盘「这个组件要不要 1:1 高清」，再导出/重推** ——
        // 导出尾部的 T22 会立刻 render 一次，画布尺寸就是在那里按这个勾选算的。
        boolean hiresOn = this.cbHires != null && this.cbHires.isChecked()
                && kind == MediaKind.IMAGE;
        WidgetRender.setHiResWidget(this, this.widgetId, hiresOn);
        // v1.10.260：动图档位也按组件落盘（图片组件不写，避免无意义条目）
        if (kind != MediaKind.IMAGE) {
            WidgetRender.setAnimQualityWidget(this, this.widgetId, this.animLevel);
        }
        boolean needsImport = this.pickedUri != null
                || base.getFrameCount() == 0
                || !Intrinsics.areEqual(base.getSourceUri(), sourceUri)
                || base.getKind() != kind;
        if (needsImport) {
            int fpsX10 = effectiveFpsX10();
            // v1.10.54：导出帧率不许超过「这个组件推得动的上限」。
            // 否则你设 50fps、组件只能推 31.8fps，每帧都要跳过 1.6 帧 —— 就是「极其细微的一卡一卡」。
            // 压到上限后组件 1:1 播放：速度、时长都与素材一致，且不再跳帧。
            int capX10;
            if (isWidgetPlaced(this.widgetId)) {
                capX10 = WidgetRender.INSTANCE.pushFpsCapX10(this, this.widgetId);
            } else {
                // v1.10.55：组件还没上桌面，尺寸是「默认 110x110dp」——照它算会得出 54fps，
                // 可一旦拖大就是 858KB/帧、只能推 31.8fps，导出 50fps 依旧要跳帧。
                // 未上桌面时按上限（22 万像素 ≈ 31.8fps）保守估。
                capX10 = 318;
            }
            // v1.10.59：**不再把导出帧率压低到组件的推帧上限。**
            // 用户底线：30~50fps 任意调时，播放时长与速度必须与素材 100% 一致。
            // 压低导出帧率会让「帧数 × 1/帧率」凑不出素材时长 —— 例：600ms 的 20 帧动图按 31.8fps
            // 导出 = 20 帧 × 31.4ms = 629ms，整整慢了 5%，而且帧被复制/丢掉，看起来就是「速度不对」。
            // 现在导出严格按用户帧率；桌面推不动时按墙钟跳帧（时长仍然分毫不差）。
            if (fpsX10 > capX10) {
                Diag.warn(this, "T16", "组件每帧 " + "（估算）"
                        + " 只能推 " + String.format(java.util.Locale.US, "%.1f", capX10 / 10.0f)
                        + "fps，本素材按 " + String.format(java.util.Locale.US, "%.1f", fpsX10 / 10.0f)
                        + "fps 导出 —— 桌面会跳帧，但**时长与速度与素材一致**（不再改导出帧率）");
            }
            int fps = Math.max(Math.round(fpsX10 / 10.0f), 1);
            int edge = effectiveEdge();   // v1.10.59：滑块 + 素材尺寸 + 格式阈值一起算
            int quality = 50 + progress(this.seekQuality);
            WidgetConfig snapshot = buildConfigFromUi(fps, edge, quality,
                    base.getFrameCount(), base.getFrameWidth(), base.getFrameHeight());
            if (snapshot == null) {
                return;
            }
            // v1.10.56：把**限流后**的 fpsX10 一起传下去。
            // 以前 startExport 里又调了一次 effectiveFpsX10() 把滑块原值盖回去 ——
            // T16 限流形同虚设：用户设 50fps、组件只能推 31.8fps，导出还是 50fps，
            // 桌面每帧跳 1.6 帧，就是「极其细微的一卡一卡」。
            startExport(snapshot, Uri.parse(sourceUri), fpsX10);
            return;
        }
        WidgetConfig now = buildConfigFromUi(base.getFps(), base.getMaxEdge(), base.getQuality(),
                base.getFrameCount(), base.getFrameWidth(), base.getFrameHeight());
        if (now == null) {
            return;
        }
        try {
            WidgetStore.INSTANCE.save(this, now);
        } catch (Throwable th) {
            Diag.warn(this, "T28", "保存配置失败: " + th.getClass().getSimpleName());
            return;
        }
        finishSave(now);
    }

    /**
     * 用「当前界面 + 指定导出参数/帧数」拼一份配置。
     * 布局/形状/外观一律取界面最新值 —— 这就是「保存的布局 = 最后一次切换的布局」。
     */
    @Nullable
    final WidgetConfig buildConfigFromUi(int fps, int edge, int quality, int frameCount, int frameWidth, int frameHeight) {
        WidgetConfig base = this.base;
        if (base == null) {
            return null;
        }
        // v1.10.499（用户要求）：**取消「只预览、不写入」这条特例。**
        //
        // 原来这四项（旋转内容 / 蒙版旋转 / 调整蒙版位置 / 调整内容位置）在输出期间还能拖，
        // 画面立刻跟着变、只是不写进这一版。问题是：拖一下就要按 887×685 重新合成一帧
        // （真机 25ms 起、导出占核时更久），而那正是用户说的「拖滑块明显迟钝」。
        // 现在这四项与其它参数完全一致：输出期间**不可点击**，摸到就弹「当前方案正在输出」。
        // 因此出口不再需要冻结值 —— 界面值不可能在输出期间被改写。
        int rot = this.selectedRotation;
        int shapeRot = this.selectedShapeRotation;
        float offX = this.selectedOffsetX;
        float offY = this.selectedOffsetY;
        int contentScale = this.selectedContentScale;
        float maskX = this.selectedMaskX;
        float maskY = this.selectedMaskY;
        MediaKind kind = this.pickedKind != null ? this.pickedKind : base.getKind();
        String sourceUri = this.pickedUri != null ? this.pickedUri.toString() : base.getSourceUri();
        String name = this.pickedName != null ? this.pickedName : base.getSourceName();
        if (sourceUri == null) {
            sourceUri = "";
        }
        if (name == null) {
            name = "";
        }
        boolean overlay = isChecked(this.cbOverlay) && kind != MediaKind.IMAGE;
        boolean overlayVisible = overlay && isChecked(this.cbOverlayVisible);
        return new WidgetConfig(this.widgetId, kind, name, sourceUri,
                RangesKt.coerceIn(fps, 1, 50),
                RangesKt.coerceIn(edge, 120, 2560),
                RangesKt.coerceIn(quality, 50, 100),
                Math.max(frameCount, 0), Math.max(frameWidth, 0), Math.max(frameHeight, 0),
                this.selectedShape,
                RangesKt.coerceIn(progress(this.seekAlpha), 0, 100),
                isChecked(this.cbBorder),
                this.selectedLayout,
                overlay, overlayVisible,
                base.getOverlayX(), base.getOverlayY(), base.getOverlayWidth(),
                RangesKt.coerceIn(progress(this.seekScale), 0, 100),   // v1.10.75：外观页只可缩小
                RangesKt.coerceIn(progress(this.seekShadow), 0, 100),
                rot, shapeRot, this.selectedFitContent,
                offX, offY,
                RangesKt.coerceIn(contentScale, 0, 200),
                maskX, maskY,
                this.selectedShadowCurve);
    }

    /** 开始一轮导出（先静默取消上一轮）。 */
    private final void startExport(@NotNull WidgetConfig snapshot, @NotNull Uri uri) {
        startExport(snapshot, uri, effectiveFpsX10());
    }

    /**
     * @param fpsX10 本轮导出**实际使用**的帧率 ×10（已经过 T16 按组件尺寸限流）。
     *               必须把限流后的值传下去，否则 MediaImporter 又拿到滑块原值，限流白做。
     */
    private final void startExport(@NotNull WidgetConfig snapshot, @NotNull Uri uri, int fpsX10) {
        // v1.10.513（需求①·限制模式）：非原版签名不给成品。
        // 放在 base 判空之前：导出这条路必须**先**过身份这一关，否则改包版能白拿导出结果。
        if (Integrity.restricted(this)) {
            return;
        }
        if (this.base == null) {
            return;
        }
        cancelExportQuietly();
        // v1.10.6：导出开始 → 作废在途的预览解析（否则两个任务同时往一根条写进度 = 抖动）
        this.extractGeneration++;
        this.previewFramesLoading = false;
        final int gen = ++this.exportGeneration;
        this.exportCancelled = false;
        this.exportSnapshot = snapshot;
        // v1.10.452：新一轮导出 → 三道一次性闸门复位。**必须在这里复位而不是收尾处**：
        // 收尾可能因为异常/页面销毁提前 return，那样下一轮就会永远提交不了。
        this.exportFinished.set(false);
        this.commitDone.set(false);
        this.bakeStarted.set(false);
        this.bakeOwnedByExport = false;
        this.bakedBeforeCommit = false;
        this.bakeStage = false;   // v1.10.453：新一轮导出 = 不是烘焙阶段（进度条归导出）
        this.exportRunning = true;   // v1.10.454：从这一刻起帧开始往 frames.new 里写
        this.exportUri = uri;
        this.exportFpsX10 = fpsX10 > 0 ? fpsX10 : effectiveFpsX10();
        this.previewFrozenIndex = this.playIndex;
        // v1.10.51：把播放条对齐到冻结帧（任务收尾时就不会拿别的下标把滑块闪到尾部）
        syncPlaybackBarOnly(this.playIndex);
        // v1.10.25：先归零再 setBusy —— setBusy 内部会 syncTaskUi，
        // 顺序反了会先按上一轮的 1000 闪一下满条再跳回 0
        this.parseTaskActive = true;
        this.taskExporting = true;
        this.lastLoggedPct = -1;
        this.parseProgress = 0;
        this.parseDone = 0;
        this.parseTotal = 0;
        this.lastProgressAt = 0L;
        setBusy(true);
        // v1.10.20：归零后立刻显示（上一版的 120ms 花招会和进度回调抢，反而闪结尾）
        ProgressBar resetBar = this.pbParse;
        if (resetBar != null) {
            resetBar.setProgress(0, false);
            resetBar.setAlpha(1.0f);
            resetBar.setVisibility(View.VISIBLE);
        }
        syncTaskUi();
        Diag.warn(this, "T11", "任务UI(导出开始) 圆环=" + (this.pbPreview != null ? this.pbPreview.getVisibility() : -1)
                + " 解析条=" + (this.pbParse != null ? this.pbParse.getVisibility() : -1)
                + " 条值=" + (this.pbParse != null ? this.pbParse.getProgress() : -1)
                + " 文字=" + (this.tvPlayTime != null ? String.valueOf(this.tvPlayTime.getText()) : "-"));
        applyProgressTheme();
        showParseToast();
        // v1.10.499：导出期间页面是静止的 —— 正好在后台补一张最新的底图，
        // 等用户第一次摸参数时覆盖层已经有了「刚导出的那个画面」的模糊底（显示本身只是切 visibility）。
        try {
            getWindow().getDecorView().postDelayed(this.busyOverlayPrepare, 500L);
        } catch (Throwable ignored) {
        }
        // v1.10.5（用户要求）：导出时冻结在当前帧，播放循环停下，进度条灰置
        stopPlaybackLoop();
        setPlaybackSeekActive(false);
        Diag.warn(this, "T01", "开始导出 gen=" + gen + " fps=" + snapshot.getFps()
                + " 边长=" + snapshot.getMaxEdge() + " 质量=" + snapshot.getQuality()
                + " 布局=" + snapshot.getLayout());
        new Thread(() -> {
            // 需求 4：导出在界面线程之外，且让界面线程优先（圆环/滚动不被抢）
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            } catch (Throwable ignored) {
            }
            runExport(ConfigActivity.this, gen, snapshot, uri);
        }).start();
    }

    private static final void runExport(@NotNull ConfigActivity activity, int gen, @NotNull WidgetConfig snapshot, @NotNull Uri uri) {
        try {
            MediaImporter.Result result = MediaImporter.INSTANCE.m9import(activity, snapshot, uri,
                    activity.exportFpsX10,
                    new ConfigActivity$importAsync$1$result$1(activity, gen),
                    new MediaImporter.Cancel() {
                        @Override
                        public boolean isCancelled() {
                            return activity.isExportCancelled(gen);
                        }
                    });
            if (result == null || activity.isExportCancelled(gen)) {
                Diag.warn(activity, "T02", "导出已取消，丢弃结果 gen=" + gen);
                return;
            }
            postToUi(activity, () -> activity.finishExport(gen, result));
        } catch (Throwable th) {
            if (activity.isExportCancelled(gen)) {
                Diag.warn(activity, "T02", "取消引发的异常，忽略: " + th.getClass().getSimpleName());
                return;
            }
            Diag.warn(activity, "T04", "导出失败: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            postToUi(activity, () -> activity.failExport(gen, th));
        }
    }

    /**
     * v1.10.56：所有「导出收尾」都经过这里投递到主线程。
     * 以前是裸的 `ui.post(() -> finishExport(...))` —— 一旦里面抛异常，主线程直接崩，
     * 用户看到的是「导出完就闪退」；而且此时帧已经在暂存区，配置还没写，组件就停在占位图上。
     */
    private static final void postToUi(@NotNull ConfigActivity activity, @NotNull Runnable what) {
        try {
            activity.ui.post(() -> {
                try {
                    what.run();
                } catch (Throwable th) {
                    Diag.warn(activity, "T24", "导出收尾异常: " + th.getClass().getSimpleName()
                            + (th.getMessage() != null ? "/" + th.getMessage() : ""));
                    CrashLog.save(activity, Thread.currentThread(), th);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.55：这个组件是否已经放到桌面上了（没放之前尺寸是默认值，不能拿来估帧率上限）。 */
    private final boolean isWidgetPlaced(int widgetId) {
        try {
            int[] ids = android.appwidget.AppWidgetManager.getInstance(this).getAppWidgetIds(
                    new android.content.ComponentName(this, WidgetProvider.class));
            if (ids == null) {
                return false;
            }
            for (int id : ids) {
                if (id == widgetId) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    final boolean isExportCancelled(int gen) {
        return this.exportCancelled || gen != this.exportGeneration;
    }

    /** 导出成功：暂存转正 + 用「最新界面参数」保存 + 生效。 */
    final void finishExport(int gen, @Nullable MediaImporter.Result result) {
        if (gen != this.exportGeneration || result == null) {
            Diag.warn(this, "T05", "旧任务结果作废 gen=" + gen + " 当前=" + this.exportGeneration);
            return;
        }
        // v1.10.454：导出线程已经交回结果 → 不再往 frames.new 里写，闸门放开。
        // （本方法与 save() 都在主线程，不存在"放开的瞬间被重开一轮"的交错。）
        this.exportRunning = false;
        WidgetConfig snapshot = this.exportSnapshot;
        if (snapshot == null) {
            return;
        }
        // v1.10.22：0 帧绝对不落盘（离开应用取消后残存的帧表会走到这里）
        if (result.getFrameCount() <= 0) {
            Diag.warn(this, "T14", "结果 0 帧，放弃保存（不污染已有组件）");
            this.parseTaskActive = false;
            this.busy = false;
            WidgetPrewarm.setForegroundBusy(false);   // v1.10.333：任务结束，后台预热可以接着烘
            syncTaskUi();
            setPlaybackSeekActive(true);
            restorePickLine();
            WidgetStore.INSTANCE.discardStaging(this, this.widgetId);
            // v1.10.56：**这里以前是完全静默的** —— 导出跑了几分钟、进度条跑满，
            // 然后什么都不发生、页面也不关、桌面组件还是那张占位图。
            // 现在必须明确告诉用户「0 帧、没保存、下一步怎么办」。
            toast(getString(R.string.cfg_export_empty));
            return;
        }
        // v1.10.57：导出完成 = 立即生效（v1.10.54/55 那套「先暂存、再弹框确认」已经按用户要求删掉）。
        // 点「保存配置」就是同意保存，导出跑完就落盘 + 刷桌面；期间调过的布局/手势也一并带上
        // （「所见即所存」，不再有第二步确认）。
        this.pendingExport = result;
        this.parseTaskActive = false;
        this.busy = false;
        WidgetPrewarm.setForegroundBusy(false);   // v1.10.333：任务结束，后台预热可以接着烘
        this.parseProgress = 1000;
        // v1.10.25：成功也要「跑满 + 淡出」，不能从 87% 直接消失。
        // v1.10.453（用户要求）：这条条**只属于导出**，跑满就淡出；烘焙进度不再共用它，
        // 改为下面那行数字「正在烘焙 x/y」。所以这里恢复「跑满 + 淡出」是安全的。
        finishTaskBar();
        syncTaskUi();
        restorePlayTime();
        setPlaybackSeekActive(true);
        if (result.getFps() != snapshot.getFps()) {
            toast(getString(R.string.cfg_fps_capped,
                    new Object[]{Integer.valueOf(snapshot.getFps()), Integer.valueOf(result.getFps())}));
        }
        Diag.warn(this, "T06", "导出完成 " + result.getFrameCount() + " 帧（立即生效）");
        // v1.10.59：**时长自检** —— 把「素材时长 vs 导出时长」算给他看。
        // 只要这两数相等，播放速度和总时长就必然与素材一致（桌面按 fpsX10 的墙钟取帧）。
        try {
            int fpsX10 = Math.max(result.getFpsX10(), 1);
            long outMs = (long) result.getFrameCount() * 10000L / fpsX10;
            long srcMs = this.sourceDurationMs;
            // v1.10.133：视频仍按设计截断到 40s（动图已不截），自检要跟「期望时长」比，不然必误报
            MediaKind kindNow = this.pickedKind != null ? this.pickedKind
                    : (this.base != null ? this.base.getKind() : MediaKind.IMAGE);
            boolean truncated = kindNow == MediaKind.VIDEO && srcMs > 40000L;
            long expectMs = truncated ? 40000L : srcMs;
            if (srcMs > 0L) {
                Diag.warn(this, "T26", "导出时长自检 素材=" + srcMs + "ms → "
                        + result.getFrameCount() + " 帧 × "
                        + String.format(java.util.Locale.US, "%.1f", fpsX10 / 10.0f) + "fps = "
                        + outMs + "ms（差 " + (outMs - expectMs) + "ms）"
                        // v1.10.132：容差改相对；v1.10.133：视频 40s 截断算「符合设计」，不再黄牌
                        + (Math.abs(outMs - expectMs) <= Math.max(40L, expectMs / 200L) ? " ✓" : " ✗ 需要排查")
                        + (truncated ? "（素材超 40s，按设计截断）" : ""));
            } else {
                Diag.warn(this, "T26", "导出时长 " + result.getFrameCount() + " 帧 × "
                        + String.format(java.util.Locale.US, "%.1f", fpsX10 / 10.0f) + "fps = " + outMs
                        + "ms（素材时长未知，不做对比）");
            }
        } catch (Throwable ignored) {
        }
        // v1.10.57（用户要求）：**不再弹「保存生效 / 放弃」确认框。**
        // 点「保存配置」就是同意保存，导出完直接生效：
        //   • 人在这个页面 → 提交 + 提示 + 关页面
        //   • 人已经去桌面看组件了 → 提交 + 刷新组件（绝不碰界面、也不丢结果）
        // 这样既没有多余的一步，也不会出现「导出完了却什么都没保存」。
        boolean uiAlive = isPageVisible();
        Diag.warn(this, "T22", "导出完成 → 直接生效（界面=" + (uiAlive ? "前台" : "后台") + "）");
        // v1.10.452（用户第 7 次实测："还是没画面，而且还导出两遍"）：**顺序整个反过来 —— 先提交，再烘焙。**
        //
        // 428~451 一直是"先烘完再提交"，于是烘焙必须去读 `frames.new`（暂存目录）：那些帧还没转正，
        // 名字是 compact 出来的 `f*.webp`，而运行时读的是 `frames/`。只要时序差一点点
        // （提交改名、取消重开、暂存被清、读回调对不上），烘焙就是 235 帧全失败、5ms 跑完、进度 0/235
        // —— 446~451 六个版本全部栽在这同一处，每版都只差"一层目录"。
        //
        // 现在：① 提交（暂存转正 + 配置落盘）→ ② 在**页面原地**显示「正在烘焙 x/y」→ ③ 烘完推组件、关页。
        // 烘焙读的就是运行时那条路径（frames/ 正式目录），不可能再"一帧都读不到"。
        // 提交与烘焙各有一道一次性闸门：T20/T22 撞在一起也只会发生一次（见字段注释）。
        if (!this.exportFinished.compareAndSet(false, true)) {
            Diag.warn(this, "W29", "重复的导出完成回调已忽略（本轮已经收尾过）gen=" + gen
                    + " 当前=" + this.exportGeneration);
            return;
        }
        this.bakeOwnedByExport = true;   // 本轮的烘焙归导出链，「导出后预热」那条老路不许再排一次
        commitPendingExport(uiAlive);    // ① 提交：暂存转正 + 配置落盘 +（后台时）立刻推首帧
        bakeAfterCommit(result.getFrameCount(), uiAlive);   // ② 烘焙：读正式目录，页面留原地显示进度
    }

    /**
     * v1.10.452：**提交之后的烘焙** —— 这是本轮唯一的一条烘焙入口。
     *
     * <p>为什么顺序是"提交 → 烘焙"（而不是 428~451 的"烘焙 → 提交"）：
     * 提交（`commitStaging`）才是把帧从 `frames.new` 搬到 `frames/` 的那一步，而烘焙读帧走的是
     * `WidgetStore.loadFrame` → `frames/fXXXXX.webp`。先烘后提，等于让烘焙去读一个"还没搬好的目录"，
     * 只要时序差一层，就是 235 帧全失败（450/451 都是 `完成=0 失败=235 用时=5ms`）。
     * 先提交之后，烘焙读的和运行时读的**是同一个文件**，不存在还能对不上的情况。
     *
     * @param frames  本轮导出的真实帧数（不是 snapshot 里的，那个是导出开始时的 0）。
     * @param uiAlive 页面还在前台 → 留在原地显示「正在烘焙 x/y」，烘完才 finishSave（提示 + 关页）；
     *                已经在后台 → 只烘不碰界面，桌面每一拍自己取新的烘焙帧。
     */
    private void bakeAfterCommit(int frames, final boolean uiAlive) {
        final WidgetConfig cfg = this.base;
        if (cfg == null) {
            Diag.warn(this, "W29", "提交后烘焙跳过：没有可用配置（界面状态不可读）");
            if (uiAlive) {
                finishSaveNow();
            }
            return;
        }
        final int widgetId = cfg.getWidgetId();
        // v1.10.452：**本轮的烘焙只开一批**。T20/T22 撞车、awaitBake 重试、旧代码的"导出后预热"
        // 都不可能再往同一条时间线上插第二个批次（插进去就会顶代次，把正在读的那批判成过期）。
        if (!this.bakeStarted.compareAndSet(false, true)) {
            Diag.warn(this, "W29", "提交后烘焙已在本轮跑过，重复触发已忽略 组件=" + widgetId);
            if (uiAlive) {
                finishSaveNow();
            }
            return;
        }
        final int total = frames > 0 ? frames : cfg.getFrameCount();
        try {
            // v1.10.452（判据）：**先数一遍正式目录里到底有几个帧文件**。
            // 446~451 的每一版都只能从「完成=0 失败=235」反推，这次把答案直接写进日志。
            final int onDisk = countFrameFiles(widgetId);
            Diag.warn(this, "W29", "提交后烘焙 组件=" + widgetId + " 帧=" + total
                    + " 正式目录帧文件=" + onDisk
                    + " 界面=" + (uiAlive ? "前台" : "后台"));
            if (onDisk <= 0) {
                Diag.warn(this, "W29", "提交后烘焙放弃：正式目录里没有帧文件"
                        + "（这条就是根因，别再往下猜烘焙）");
                // 不在这里 toast：commitPendingExport 的首帧探针（T23）已经会提示「0 帧」，
                // 再弹一次就变成「0 帧」+「已保存」两条自相矛盾的提示。
                if (uiAlive) {
                    finishSaveNow();
                }
                return;
            }
            if (total <= 0) {
                Diag.warn(this, "W29", "提交后烘焙跳过：帧数为 0");
                if (uiAlive) {
                    finishSaveNow();
                }
                return;
            }
            final int[] canvas = WidgetRender.INSTANCE.canvasSize(this, widgetId);
            Diag.warn(this, "W29", "烘焙画布 组件=" + widgetId + " " + canvas[0] + "x" + canvas[1]
                    + "（运行时同款 canvasSize，不等桌面上报）");
            if (canvas[0] < 96 || canvas[1] < 96) {
                Diag.warn(this, "W29", "提交后烘焙跳过：画布过小 " + canvas[0] + "x" + canvas[1]);
                if (uiAlive) {
                    finishSaveNow();
                }
                return;
            }
            // v1.10.446：把烘焙用的格子**冻结**进缓存 —— 运行时 widgetSize() 在"桌面不报尺寸"
            // 的机型上读的就是这份缓存，于是回桌面算出的画布与这里逐像素相同，不会再重烘。
            WidgetRender.INSTANCE.freezeBoxFromRuntime(this, widgetId);
            showBakeProgress(0, total);
            WidgetPrewarm.bakeNow(this, cfg, canvas[0], canvas[1], total);
            if (uiAlive) {
                // 用户口径（v1.10.443）："真正意义上一次导出、绝不再烘，等这一分钟没关系"。
                // 所以页面留在这里，进度行就是「正在烘焙 x/y」，烘完才提示并关页。
                awaitBake(cfg, widgetId, total, canvas[0], canvas[1], this::finishSaveNow);
            } else {
                Diag.warn(this, "W29", "后台烘焙已开跑（页面不在，不显示进度）组件=" + widgetId
                        + " 帧=" + total);
            }
        } catch (Throwable th) {
            Diag.warn(this, "W29", "提交后烘焙异常: " + th.getClass().getSimpleName());
            CrashLog.save(this, Thread.currentThread(), th);
            if (uiAlive) {
                finishSaveNow();
            }
        }
    }

    /** v1.10.452：正式目录里到底有几个帧文件（`f00000.webp`…）。**只作日志判据，不参与任何决策**。 */
    private int countFrameFiles(int widgetId) {
        try {
            java.io.File[] files = WidgetStore.INSTANCE.framesDir(this, widgetId).listFiles();
            if (files == null) {
                return 0;
            }
            int n = 0;
            for (java.io.File f : files) {
                String name = f.getName();
                if (name != null && name.startsWith("f") && name.endsWith(".webp")) {
                    n++;
                }
            }
            return n;
        } catch (Throwable th) {
            return -1;
        }
    }

    /** v1.10.452：烘焙收尾（或烘焙根本没跑成）之后，把「已提交的这份配置」正式生效并关页。 */
    private void finishSaveNow() {
        WidgetConfig pending = this.pendingFinishSave;
        this.pendingFinishSave = null;
        if (pending == null) {
            Diag.warn(this, "W29", "收尾跳过：没有待生效的配置（可能已经在后台提交过）");
            return;
        }
        // v1.10.453：关页前把导出条收干净（不跑满、不淡出 —— 那条条只属于导出阶段）
        hideTaskBar();
        finishSave(pending);
    }

    /** 烘焙是否已经在提交之前做完（v1.10.452 起改由 {@link #bakeOwnedByExport} 表达，保留字段防漏改）。 */
    private boolean bakedBeforeCommit;

    /**
     * v1.10.452：原来的「先烘 → 再提交」整段（startBakeThenCommit）已经删除。
     * 它存在的唯一理由是把烘焙塞在"提交之前"，而那正是 446~451 全部失败的那层时序；
     * 现在由 {@link #bakeAfterCommit(int, boolean)} 接管，顺序是「提交 → 烘焙 → 关页」。
     */

    /** v1.10.56：这个页面现在还能不能弹框/改控件（在后台或被销毁时不能）。 */
    private final boolean isPageVisible() {
        try {
            return this.pageVisible && !isFinishing() && !isDestroyed();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** v1.10.54：把「刚导出完的暂存帧」真正提交 + 落配置 + 关闭页面（点保存时才走这里）。 */
    private final void commitPendingExport() {
        commitPendingExport(true);
    }

    /**
     * v1.10.56：提交这份导出。
     *
     * @param uiAlive 页面还在前台（true）→ 走 finishSave（渲染 + 提示 + 关页面）；
     *                false → 用户已经回桌面了，只做「数据落盘 + 刷新组件」，绝不碰界面。
     *                这条路径存在的意义：**导出结果不能再因为「人不在这个页面」而丢掉**。
     */
    private final void commitPendingExport(boolean uiAlive) {
        MediaImporter.Result result = this.pendingExport;
        WidgetConfig snapshot = this.exportSnapshot;
        if (result == null || snapshot == null) {
            return;
        }
        // v1.10.452：**提交只许发生一次**。451 里「导出完成（T22）」和「离开应用（T20）」
        // 会同时走到这里：第一次把 `frames.new` 搬进 `frames/`，第二次拿到的暂存目录已经空了 ——
        // 表面上没崩，实际是把"提交"这件事跑了两遍（用户看到的就是"导出两遍"）。
        if (!this.commitDone.compareAndSet(false, true)) {
            Diag.warn(this, "W29", "重复提交已忽略（本轮已经提交过）界面=" + (uiAlive ? "前台" : "后台"));
            return;
        }
        WidgetConfig done = buildConfigFromUi(result.getFps(), snapshot.getMaxEdge(), snapshot.getQuality(),
                result.getFrameCount(), result.getWidth(), result.getHeight());
        if (done == null) {
            // 界面状态已经不可读（页面被销毁）→ 用「导出开始时的快照」补上帧信息，绝不因此丢结果
            done = snapshot.copy(snapshot.getWidgetId(), snapshot.getKind(), snapshot.getSourceName(),
                    snapshot.getSourceUri(), result.getFps(), snapshot.getMaxEdge(), snapshot.getQuality(),
                    result.getFrameCount(), result.getWidth(), result.getHeight(),
                    snapshot.getShape(), snapshot.getShapeAlpha(), snapshot.getBorder(), snapshot.getLayout(),
                    snapshot.getOverlayEnabled(), snapshot.getOverlayVisible(),
                    snapshot.getOverlayX(), snapshot.getOverlayY(), snapshot.getOverlayWidth(),
                    snapshot.getScalePercent(), snapshot.getShadowPercent(),
                    snapshot.getRotation(), snapshot.getShapeRotation(), snapshot.getFitContent(),
                    snapshot.getOffsetXPercent(), snapshot.getOffsetYPercent(),
                    snapshot.getContentScalePercent(),
                    snapshot.getMaskOffsetXPercent(), snapshot.getMaskOffsetYPercent(),
                    snapshot.getShadowCurve());
            Diag.warn(this, "T05", "界面状态不可读，改用导出快照提交（布局=" + done.getLayout() + "）");
        }
        try {
            WidgetStore.INSTANCE.commitStaging(this, this.widgetId);
            WidgetStore.INSTANCE.save(this, done);
            // v1.10.8：桌面播放按 fpsX10 精确计时
            WidgetStore.INSTANCE.setFpsX10(this, this.widgetId, Math.max(result.getFpsX10(), 10));
        } catch (Throwable th) {
            Diag.warn(this, "T05", "提交导出失败: " + th.getClass().getSimpleName());
            if (uiAlive) {
                toast(getString(R.string.status_failed, new Object[]{th.getClass().getSimpleName()}));
            }
            return;
        }
        this.pendingExport = null;
        // v1.10.497：这一轮已经落盘 → 解冻，之后（页面还开着时）再改蒙版/内容位置就按界面值写
        this.base = done;
        // v1.10.56：提交完就把「新素材」标记清掉 —— 否则后台自动提交后再点一次「保存配置」，
        // 会拿着同一个 uri 又导出一遍（几分钟白等）。清掉之后这一下就是「重新保存现有配置」。
        this.pickedUri = null;
        this.pickedName = null;
        Diag.warn(this, "T06", "保存导出 " + result.getFrameCount() + " 帧 → 保存布局=" + done.getLayout()
                + "（界面=" + (uiAlive ? "前台" : "后台自动提交") + "）");
        // v1.10.119：**不在这里开热**。保存这一刻 vivo 桌面还只回「110×110dp」的默认尺寸，
        // 按它热出来的 300 帧（360×360）跟真正渲染的 597×626 一个字都对不上（v1.10.118 的坑）。
        // 这里只作废「已热」记忆，等第一次真正渲染、画布确定之后，由 WidgetRender.render →
        // WidgetPrewarm.ensure 用**真实画布**开热。
        try {
            WidgetPrewarm.reset(this, done.getWidgetId());
        } catch (Throwable th) {
            Diag.warn(this, "W13", "预热重置失败: " + th.getClass().getSimpleName());
        }
        // v1.10.56：提交后**立刻验一次**首帧能不能解出来。
        // 解不出来就说明「帧写盘/改名/解码」这条链路有问题 —— 宁可当场报错，也不要让用户在桌面上
        // 看到一张占位图、然后完全不知道发生了什么。
        Bitmap probe = null;
        try {
            probe = WidgetStore.INSTANCE.loadFrame(this, done, 0, 360, 360);
        } catch (Throwable ignored) {
        }
        if (probe == null) {
            Diag.warn(this, "T23", "提交后首帧读取失败 id=" + done.getWidgetId()
                    + " count=" + done.getFrameCount() + " fw/fh=" + done.getFrameWidth() + "x" + done.getFrameHeight());
            if (uiAlive) {
                toast(getString(R.string.cfg_export_empty));
            }
        } else if (!probe.isRecycled()) {
            probe.recycle();
        }
        // v1.10.110：日志里直接能看懂「这个组件用了哪个蒙版、哪个布局」——
        // 用与界面上完全同名的中文（蒙版=3:4 圆角 / 布局=原始比例），不用再猜枚举名。
        Diag.warn(this, "T12", "保存瞬间 布局=" + this.selectedLayout.getLabel()
                + (this.selectedLayout.getRatio() > 0.0f
                    ? "(框比例=" + String.format(java.util.Locale.US, "%.3f", this.selectedLayout.getRatio()) + ")"
                    : "(比例跟随素材)")
                + " 蒙版=" + this.selectedShape.label(this)
                + " 缩放=" + progress(this.seekScale)
                + " 素材帧率=" + String.format(java.util.Locale.US, "%.1f", result.getFpsX10() / 10.0f));
        if (uiAlive) {
            // v1.10.425（用户："导出后才搞，那不等于没加"）：**先不关页**。
            // 提交必须此刻做（下面 schedulePrewarmAfterExport 就挂在这里），
            // 但"完成并关页"推迟到烘焙收尾 —— 否则用户只看到"导出完成"就回桌面，
            // 那一轮烘焙全在后台跑，等于什么都没加。
            this.pendingFinishSave = done;
        } else {
            publishCommitted(done);
        }
        // v1.10.196：配置已经落盘 → 如果开了「导出后提前烘帧」，顺手把桌面那轮烘掉
        schedulePrewarmAfterExport(done);
    }

    /**
     * v1.10.196（需求）：**导出完就把桌面那一轮烘帧做掉**，省掉回桌面后「命中 0% → 顶帧」的 30 秒。
     *
     * 为什么现在才做得到：v1.10.193 起「画布无关帧」让画布只由素材/布局比例决定，
     * 配置页算出来的画布与桌面 render() 算的完全一致（连 stamp 都一致），烘完桌面直接命中。
     * 以前画布依赖桌面格子（vivo 只给 110×110dp 默认值），预先烘必然对不上 → 白烘。
     *
     * 只在这一条通道成立时才做；填充布局的画布依赖格子，配置页不知道 → 直接跳过（保持现状）。
     * 默认关（设置页「性能 → 导出后提前烘帧」），打开才生效。
     */
    private void schedulePrewarmAfterExport(@NotNull WidgetConfig config) {
        if (this.bakedBeforeCommit || this.bakeOwnedByExport) {
            // v1.10.452：本轮的烘焙由导出链自己接管（提交后立刻烘、页面原地显示进度），
            // 「导出后预热」这条 1.5 秒定时器**绝不能再排一次** —— 451 的用户观感里
            // "第二次烘焙从 0/235 重新开始"就是它 + T20 那条等待线程叠出来的。
            this.bakedBeforeCommit = false;
            Diag.warn(this, "W29", "导出后预热跳过：本轮烘焙由导出链接管 组件=" + config.getWidgetId());
            return;
        }
        try {
            if (!WidgetRender.prefHeatOnExport(this)) {
                return;
            }
            // v1.10.256（实测暴露的**白烘**）：原来这里是
            //   ① 闸门用 noCanvasOk（它现在对「档 ≥1」也返回 true，包括**按卡片算**的档 2）；
            //   ② 画布用 WidgetRender.frameCanvas(config)（写死 1080/90 万）。
            // 于是档 2 烘出来是 989×910，而运行时 canvasFor 要的是 1030×947 —— stamp 不同，
            // 165 帧 4.3 秒的预热全部作废，回桌面照样重烘一轮（日志里 W29 后面紧跟一条 W13 预热开始）。
            // 现在改成直接问「运行时到底会用什么画布」：canvasSize() 内部就是 canvasFor，
            // 同一个函数、同一个入参，不可能再对不上。
            if (!WidgetRender.needUri(this, config)) {
                Diag.warn(this, "W29", "导出后预热跳过：当前不走文件推送（URI 开关/高清/1:1 都没开）");
                return;
            }
            // 推迟 1.5 秒：先让「导出完成」的提示/关页面动作走完，别和 UI 抢主线程
            this.ui.postDelayed(() -> {
                try {
                    // v1.10.258：**画布必须在延时之后才算**。
                    // 实测（21:45 那批）：刚导出完桌面还只报「110×110dp」的默认格子，
                    // 于是 W29 记的是 360×331，而运行时真正要的是 597×549 —— 165 帧又白烘一次。
                    // 1.5 秒足够桌面把真实格子（OPTION_APPWIDGET_*）报上来，那时再算才是运行时的值。
                    final int[] fc = WidgetRender.INSTANCE.canvasSize(ConfigActivity.this, config.getWidgetId());
                    if (fc[0] < 96 || fc[1] < 96) {
                        Diag.warn(ConfigActivity.this, "W29", "导出后预热跳过：算不出画布（素材尺寸未知）");
                        return;
                    }
                    WidgetPrewarm.ensure(ConfigActivity.this, config, fc[0], fc[1]);
                    // v1.10.423（用户口径「导出时一并烘完」）：**烘完之前不放用户走**。
                    // 复用导出那条任务条显示「正在烘焙 47/235」，烘完再淡出。
                    // 它不是新进度条：parseDone/parseTotal 就是同一套，syncTaskUi 会画。
                    awaitBake(config.getWidgetId(), config.getFrameCount(),
                            () -> {
                                WidgetConfig pending = this.pendingFinishSave;
                                this.pendingFinishSave = null;
                                if (pending != null) {
                                    finishSave(pending);
                                }
                            });
                    Diag.warn(ConfigActivity.this, "W29", "导出后预热开始 组件=" + config.getWidgetId()
                            + " 画布=" + fc[0] + "x" + fc[1]
                            + " 帧=" + config.getFrameCount()
                            + " 线程=" + WidgetPrewarm.effectiveThreads(ConfigActivity.this)
                            + "（回桌面应直接命中，不再有冷窗口）");
                } catch (Throwable th) {
                    Diag.warn(ConfigActivity.this, "W29",
                            "导出后预热异常: " + th.getClass().getSimpleName());
                }
            }, 1500L);
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.56：不经过界面直接让新配置生效（页面已经不在前台时用）。
     * 只做三件事：把首帧推上桌面、拉起推帧服务、记日志。任何异常都只记账，不杀进程。
     */
    private final void publishCommitted(@NotNull WidgetConfig config) {
        try {
            WidgetRender.INSTANCE.render(this, android.appwidget.AppWidgetManager.getInstance(this), config, 0, null);
        } catch (Throwable th) {
            Diag.warn(this, "T21", "后台提交后首帧推送失败: " + th.getClass().getSimpleName());
        }
        try {
            PlaybackService.Companion.start(this);
        } catch (Throwable ignored) {
        }
        try {
            // Toast 必须在主线程（这条路径可能跑在后台线程上）
            final String text = getString(R.string.cfg_export_auto_saved);
            this.ui.post(() -> {
                try {
                    toast(text);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
        Diag.warn(this, "T21", "后台提交生效 id=" + config.getWidgetId()
                + " 帧=" + config.getFrameCount() + " fps=" + config.getFps());
    }

    final void failExport(int gen, @NotNull Throwable error) {
        if (gen != this.exportGeneration) {
            return;
        }
        this.exportRunning = false;   // v1.10.454：这一轮导出失败收摊，闸门放开（否则以后再也点不动保存）
        this.parseTaskActive = false;
        this.busy = false;
        WidgetPrewarm.setForegroundBusy(false);   // v1.10.333：任务结束，后台预热可以接着烘
        syncTaskUi();
        setPlaybackSeekActive(true);
        restorePickLine();
        restorePlayTime();
        WidgetStore.INSTANCE.discardStaging(this, this.widgetId);
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        toast(getString(R.string.status_failed, new Object[]{message}));
    }

    /** 静默取消（换素材/重开/离开页面）：不提示、不保存、删暂存。 */
    final void cancelExportQuietly() {
        // v1.10.454：静默取消 = 这一轮导出结束（不管是换素材还是重开），闸门必须放开
        this.exportRunning = false;
        // v1.10.54：导出完但没点保存就离开 → 暂存作废（不录用）
        if (this.pendingExport != null) {
            this.pendingExport = null;
            WidgetStore.INSTANCE.discardStaging(this, this.widgetId);
        }
        if (!this.busy && !this.parseTaskActive) {
            return;
        }
        this.exportGeneration++;
        this.exportCancelled = true;
        this.busy = false;
        WidgetPrewarm.setForegroundBusy(false);   // v1.10.333：任务结束，后台预热可以接着烘
        this.parseTaskActive = false;
        if (this.base != null) {
            WidgetStore.INSTANCE.discardStaging(this, this.widgetId);
        }
        syncTaskUi();
        setPlaybackSeekActive(true);
        // 恢复素材卡片文字与播放
        restorePickLine();
        if (this.previewFrames != null && this.previewFrames.getFrames().size() > 1 && !this.playing
                && DeskWatcher.isAppForeground()) {
            this.ui.post(this::startPreviewFramesPlayback);
        }
    }

    /** 需求 5：每次开始导出都轻提示一次（先取消上一条，避免排队丢提示）。 */
    private final void showParseToast() {
        try {
            if (this.parseToast != null) {
                this.parseToast.cancel();
            }
            this.parseToast = android.widget.Toast.makeText(this, R.string.parse_export_toast,
                    android.widget.Toast.LENGTH_SHORT);
            this.parseToast.show();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 需求 3：圆环 + 解析条只由「有没有任务」决定。
     * 切标签、切布局、换肤、回前台等任何会重建/遮挡预览的地方都要调它。
     */
    final void syncTaskUi() {
        boolean active = this.parseTaskActive || this.busy;
        // v1.10.18 兜底：只要有任务在跑，播放进度条一定是灰置状态，
        // 任何其他代码路径都改不回来（下次调用 syncTaskUi 又会纠正）
        SeekBar playbackSeek = this.seekPlayback;
        if (playbackSeek != null) {
            // v1.10.26（需求 2/3）：永久不可拖动，只用透明度表达「能不能看」——
            // 任务中 / 静态图片 / 帧不足一律灰置
            playbackSeek.setAlpha(playbackSeekAlpha());
        }
        TextView playTime = this.tvPlayTime;
        if (playTime != null) {
            if (active) {
                // v1.10.25：任务中这一行让给百分比（唯一的真实反馈源，无障碍树里也读得到）
                if (this.playTimeColor == 0) {
                    this.playTimeColor = playTime.getCurrentTextColor();
                }
                String text;
                if (this.bakeStage) {
                    // v1.10.453（用户明确要求）：**烘焙进度只用数字「x/y」，绝不占用导出那条进度条。**
                    // 共用一条条的问题：导出跑满 100% → 烘焙又从 0/235 重新填，看起来就是"导出了两遍"。
                    // 现在条只属于导出（跑满就淡出），烘焙只在这一行数字上走。
                    text = getString(R.string.cfg_bake_running,
                            new Object[]{Integer.valueOf(Math.max(this.parseDone, 0)),
                                    Integer.valueOf(Math.max(this.parseTotal, 1))});
                } else if (this.parseTotal > 0) {
                    int pct = Math.min(100, Math.max(0, this.parseProgress / 10));
                    text = getString(this.taskExporting ? R.string.cfg_export_running_short : R.string.cfg_parse_running_short,
                            new Object[]{Integer.valueOf(pct)});
                } else {
                    text = getString(this.taskExporting ? R.string.cfg_export_started : R.string.cfg_parse_started);
                }
                if (!text.contentEquals(String.valueOf(playTime.getText()))) {
                    playTime.setText(text);
                }
                playTime.setAlpha(1.0f);
                if (this.taskTextColor != 0) {
                    playTime.setTextColor(this.taskTextColor);
                }
            } else {
                playTime.setAlpha(1.0f);
                if (this.playTimeColor != 0) {
                    playTime.setTextColor(this.playTimeColor);
                    this.playTimeColor = 0;
                }
                if (this.taskUiWasActive) {
                    // v1.10.51：任务收尾时把陈旧下标夹回范围，绝不让播放条闪到最右端
                    int total = (this.previewFrames != null && this.previewFrames.getFrames() != null)
                            ? this.previewFrames.getFrames().size() : 0;
                    if (total > 0 && this.playIndex >= total) {
                        this.playIndex = 0;
                    }
                    updatePlayTime(Math.max(0, this.playIndex));
                }
            }
        }
        if (active) {
            View previewSection = findViewById(R.id.ll_preview_section);
            if (previewSection != null && previewSection.getVisibility() != View.VISIBLE) {
                previewSection.setVisibility(View.VISIBLE);
            }
        }
        ProgressBar ring = this.pbPreview;
        if (ring != null) {
            if (active) {
                ring.animate().cancel();
                ring.setAlpha(1.0f);
                ring.setVisibility(View.VISIBLE);
            } else {
                ring.setVisibility(View.GONE);
            }
        }
        ProgressBar bar = this.pbParse;
        if (bar != null) {
            if (this.bakeStage) {
                // v1.10.453：**烘焙阶段这条条一律不出现**（用户要求：烘焙只用数字 x/y）。
                // 导出已经跑满并淡出了，这里再让它出现（哪怕是 0→100%）就是在"共用"。
                hideTaskBar();
            } else if (active) {
                if (this.parseHideTask != null) {
                    bar.removeCallbacks(this.parseHideTask);
                    this.parseHideTask = null;
                }
                bar.animate().cancel();
                bar.setAlpha(1.0f);
                bar.setVisibility(View.VISIBLE);
                bar.setIndeterminate(false);
                // v1.10.25：真正的单一写入方 —— 进度值在这里落到控件上（原来整条链路
                // 只有 parseProgress 这个内存字段在变，没有任何一处 setProgress，条当然不动）
                if (bar.getMax() != 1000) {
                    bar.setMax(1000);
                }
                int target = Math.min(1000, Math.max(0, this.parseProgress));
                if (bar.getProgress() != target) {
                    bar.setProgress(target, false);
                }
            } else {
                // v1.10.25：收尾淡出中（parseHideTask 挂着）不归零，否则跑满了会瞬间跳回 0
                if (this.parseHideTask == null) {
                    if (bar.getProgress() != 0) {
                        bar.setProgress(0, false);
                    }
                    bar.setVisibility(View.GONE);
                }
            }
        }
        if (!active) {
            this.previewFrozenIndex = -1;
        }
        this.taskUiWasActive = active;
        // n/n 文字已按用户要求撤掉，只保留条 + 圆环
        TextView hint = this.tvParseHint;
        if (hint != null && hint.getVisibility() != View.GONE) {
            hint.setVisibility(View.GONE);
        }
    }

    /**
     * v1.10.423：**等烘焙真的烘完**（导出时一次烘完，不再"先放你回桌面、后台慢慢升质"）。
     *
     * <p>为什么要等：用户明确要求"导出后桌面就该是最终画质"。等待期间复用导出那条任务条，
     * 显示「正在烘焙 x/y」。两个兜底：
     * <ul>
     *   <li>5 秒内没开始（例如这一代已经烘过、`ensure` 直接跳过）→ 当作已完成；</li>
     *   <li>超过 6 分钟 → 放弃等待（长素材/后台被限核），不让用户卡死在这一页。</li>
     * </ul>
     * 任何异常都收尾 —— 绝不能因为一段轮询把页面挂在 busy 状态。
     */
    /** v1.10.425：等烘焙收尾后才「完成并关页」的那一次导出。 */
    @Nullable
    private WidgetConfig pendingFinishSave;

    /** v1.10.425：烘焙结束后要执行的动作（导出链用它来「烘完才关页」）。 */
    @Nullable
    private Runnable bakeAfter;

    private void awaitBake(final int widgetId, final int frames) {
        awaitBake(widgetId, frames, null);
    }

    /** v1.10.425：多一个收尾动作 —— 导出这条链要**等烘完才提交并关页**。 */
    private void awaitBake(final int widgetId, final int frames, @Nullable final Runnable after) {
        awaitBake(null, widgetId, frames, 0, 0, after);
    }

    /**
     * v1.10.443：带"重开一批"能力的等待 —— 批次没起来时用 config + 画布再叫一次，
     * 而不是像以前那样 20 秒就放行（那是"半成品"的来源之一）。
     */
    private void awaitBake(@Nullable final WidgetConfig retryCfg, final int widgetId,
                          final int frames, final int canvasW, final int canvasH,
                          @Nullable final Runnable after) {
        try {
            final long t0 = android.os.SystemClock.elapsedRealtime();
            // v1.10.453（用户要求）：进入烘焙阶段 —— 这一阶段只用数字「x/y」报进度，
            // 导出那条进度条已经跑满淡出，syncTaskUi 会把它继续藏住。
            this.bakeStage = true;
            this.busy = true;
            this.taskExporting = true;
            this.parseTotal = Math.max(frames, 1);
            this.parseDone = 0;
            this.parseProgress = 0;
            this.bakeAfter = after;
            showBakeProgress(0, Math.max(frames, 1));
            final boolean[] retried = new boolean[1];
            final int[] stallDone = new int[]{-1};
            final long[] stallAt = new long[]{t0};
            final Runnable[] tick = new Runnable[1];
            tick[0] = () -> {
                try {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    int[] p = WidgetPrewarm.bakeProgress(widgetId);
                    long waited = android.os.SystemClock.elapsedRealtime() - t0;
                    // v1.10.444：**永不无限等**。443 把超时全删了，结果"批次根本没起来"
                    // 的机器上会卡一辈子（用户截图 0/235）。这里按"进度是否还在动 + 是否真在烘"
                    // 判停滞：没有新帧、而且**连批次都没了** → 放行并记日志。
                    // v1.10.452：一帧都没烘出来时从 60 秒缩到 15 秒 —— 用户已经明确说过
                    // "不会去等一个看不见的过程"，卡在 0/235 干等一分钟是这六个版本里最伤的一件事。
                    final boolean alive = WidgetPrewarm.busy(widgetId);
                    if (p[0] != stallDone[0]) {
                        stallDone[0] = p[0];
                        stallAt[0] = android.os.SystemClock.elapsedRealtime();
                    } else {
                        long quiet = android.os.SystemClock.elapsedRealtime() - stallAt[0];
                        long limit = p[0] <= 0 ? 15000L : 60000L;
                        if (!alive && quiet > limit) {
                            Diag.warn(ConfigActivity.this, "W29", "烘焙停滞 " + (quiet / 1000L)
                                    + " 秒且无批次在跑 → 放行 组件=" + widgetId
                                    + " 帧=" + p[0] + "/" + p[1]
                                    + (p[0] <= 0 ? "（一帧都没烘出来，按失败收尾）" : ""));
                            endBakeWait();
                            return;
                        }
                    }
                    if (p[2] == 0) {
                        // v1.10.443：还没开始 —— 可能是排队（BAKE_SLOT 被别的组件占着），
                        // 也可能是批次根本没起来。v1.10.452：**只有"批次已经没了"才重开**，
                        // 否则重开就是第二个批次、把正在排队的那批顶掉（451 里进度会从 0 重来）。
                        if (!alive && waited > 5000L && !retried[0] && retryCfg != null
                                && canvasW > 0 && canvasH > 0) {
                            retried[0] = true;
                            Diag.warn(ConfigActivity.this, "W29",
                                    "等待烘焙：批次已不在、5 秒无进度 → 重开一批 组件="
                                    + widgetId + " 画布=" + canvasW + "x" + canvasH
                                    + " 帧=" + frames);
                            try {
                                WidgetPrewarm.bakeNow(ConfigActivity.this, retryCfg,
                                        canvasW, canvasH, frames);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (waited > 30000L && waited % 30000L < 200L) {
                            Diag.warn(ConfigActivity.this, "W29", "等待烘焙：" + (waited / 1000L)
                                    + " 秒仍未开始 组件=" + widgetId
                                    + " 批次在=" + alive + " 帧=" + p[0] + "/" + p[1]);
                        }
                        this.ui.postDelayed(tick[0], 200L);
                        return;
                    }
                    this.parseDone = p[0];
                    this.parseTotal = Math.max(p[1], 1);
                    this.parseProgress = (int) Math.min(1000L,
                            Math.max(0L, p[0] * 1000L / Math.max(p[1], 1)));
                    // v1.10.424：**直接写控件**。syncTaskUi() 会把这一行改回「导出 47%」，
                    // 用户就看不到「正在烘焙 x/y」——423 里我复用它，等于没显示。
                    showBakeProgress(p[0], p[1]);
                    if (p[0] >= p[1]) {
                        Diag.warn(ConfigActivity.this, "W29", "烘焙完成 组件=" + widgetId
                                + " 帧=" + p[0] + "/" + p[1] + " 等=" + waited + "ms");
                        endBakeWait();
                        return;
                    }
                    // v1.10.443：**取消 6 分钟超时**。用户口径："一次导出、绝不再烘，换一分钟完全能接受"，
                    // 而且各品牌/各芯性能差得远，任何固定超时都会变成"某台机器上永远烘不完"。
                    // 现在只按 10 分钟记一条进度日志，等它真的烘到 n/n 才放行。
                    if (waited > 600000L && waited / 600000L != (waited - 200L) / 600000L) {
                        Diag.warn(ConfigActivity.this, "W29", "等待烘焙已超 10 分钟（继续等） 组件="
                                + widgetId + " 帧=" + p[0] + "/" + p[1] + " 等=" + waited + "ms");
                    }
                    this.ui.postDelayed(tick[0], 200L);
                } catch (Throwable th) {
                    endBakeWait();
                }
            };
            this.ui.postDelayed(tick[0], 200L);
        } catch (Throwable th) {
            endBakeWait();
        }
    }

    /** v1.10.424：把「正在烘焙 x/y」直接写到播放时间那一行 + 任务条（不经 syncTaskUi）。 */
    /**
     * v1.10.453（用户要求）：烘焙进度**只写数字**「正在烘焙 x/y」。
     *
     * <p>原来这里还顺手驱动导出那条进度条（`pbParse`）：导出跑满 100% → 烘焙又从 0/235 填一遍，
     * 同一根条跑两次，任何人看都会说"导出了两遍"。现在条只属于导出阶段（跑满即淡出），
     * 烘焙只有这一行数字；`syncTaskUi()` 里也会在 {@link #bakeStage} 期间把条藏掉。
     */
    private void showBakeProgress(int done, int total) {
        try {
            TextView time = this.tvPlayTime;
            if (time != null) {
                if (this.playTimeColor == 0) {
                    this.playTimeColor = time.getCurrentTextColor();
                }
                time.setText(getString(R.string.cfg_bake_running,
                        new Object[]{Integer.valueOf(Math.max(done, 0)),
                                Integer.valueOf(Math.max(total, 1))}));
                time.setAlpha(1.0f);
                if (this.taskTextColor != 0) {
                    time.setTextColor(this.taskTextColor);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void endBakeWait() {
        Runnable after = this.bakeAfter;
        this.bakeAfter = null;
        try {
            this.bakeStage = false;   // v1.10.453：退出烘焙阶段（进度条不再被藏，也不会再出现）
            this.busy = false;
            this.parseProgress = 1000;
            // v1.10.453：这里原来调 finishTaskBar()（把条跑满再淡出）—— 那条条属于导出，
            // 烘焙阶段结束再去"跑满"一下，等于又出现一次，直接收掉。
            hideTaskBar();
            syncTaskUi();
        } catch (Throwable ignored) {
        }
        if (after != null) {
            try {
                after.run();
            } catch (Throwable ignored) {
            }
        }
    }

    /** v1.10.453：立刻收起导出进度条（不跑满、不淡出、不留残余回调）。 */
    private void hideTaskBar() {
        try {
            ProgressBar bar = this.pbParse;
            if (bar == null) {
                return;
            }
            if (this.parseHideTask != null) {
                bar.removeCallbacks(this.parseHideTask);
                this.parseHideTask = null;
            }
            bar.animate().cancel();
            bar.setAlpha(0.0f);
            bar.setVisibility(View.GONE);
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.25：任务成功收尾 —— 解析条跑满再淡出（导出/解析共用一套）。 */
    final void finishTaskBar() {
        try {
            ProgressBar bar = this.pbParse;
            if (bar == null) {
                return;
            }
            if (this.parseHideTask != null) {
                bar.removeCallbacks(this.parseHideTask);
                this.parseHideTask = null;
            }
            bar.animate().cancel();
            bar.setAlpha(1.0f);
            bar.setVisibility(View.VISIBLE);
            bar.setProgress(Math.min(bar.getMax() > 0 ? bar.getMax() : 1000, 1000), false);
            final Runnable hideTask = () -> {
                this.parseHideTask = null;
                bar.animate().alpha(0.0f).setDuration(220L)
                        .withEndAction(() -> bar.setVisibility(View.GONE)).start();
            };
            this.parseHideTask = hideTask;
            bar.postDelayed(hideTask, 320L);
        } catch (Throwable ignored) {
        }
    }

    /** 预览解析进度：n/n 文字 + 解析条 + 圆环，节流 100ms。 */
    final void onPreviewParseProgress(int done, int total) {
        onTaskProgress(false, done, total);
    }

    /** 导出进度：n/n 文字 + 圆环（不碰解析条）。 */
    final void onExportProgress(int done, int total) {
        onTaskProgress(true, done, total);
    }

    private void onTaskProgress(boolean exporting, int done, int total) {
        // v1.10.120：前台有任务在跑 → 让后台预热线程先让路（否则导出会被抢慢 40%，
        // 日志实测：154 的预热带 5 线程时，155 的导出从 6.3s 变 8.8s）
        // v1.10.333（实测抓到的口径 bug）：这里原来写 `setForegroundBusy(exporting)` ——
        // 等于**只有导出算"忙"，解析算"闲"**，可解析同样是 6 线程解 1080p、同样在抢 CPU。
        // 日志证据（21:11:30~21:11:35）：312 正在解析预览，311 的预热还在跑，
        // 这一批的编码均从 88ms 涨到 136ms、实测 33 帧/秒 → 自校准把系数一步砸到 0.57。
        // 任何一条进度回调都说明"前台有任务在跑"，一律让路；任务结束处再清掉。
        try {
            WidgetPrewarm.setForegroundBusy(true);
        } catch (Throwable ignored) {
        }
        // 导出中（busy=true）只认导出自己的进度：预览的迟到回调不许改标签
        if (this.busy && !exporting) {
            // v1.10.52：导出期间「解析」的迟到进度直接丢 —— 它会把导出条冲到 100%
            return;
        }
        if (!this.busy) {
            this.taskExporting = exporting;
        }
        this.parseDone = Math.max(done, 0);
        this.parseTotal = Math.max(total, 0);
        if (this.parseTotal > 0) {
            this.parseProgress = (int) Math.min(1000L, Math.max(0L, this.parseDone * 1000L / this.parseTotal));
        }
        // v1.10.25：每跨 10% 打一条 L32 —— 下轮出问题时先看它有没有在涨，
        // 一眼区分「任务没推进」和「推进了但界面没画」
        logTaskProgress(exporting, done, total);
        long now = SystemClock.elapsedRealtime();
        if (now - this.lastProgressAt < 100L && done < total) {
            return;
        }
        this.lastProgressAt = now;
        // v1.10.18：导出中每次进度回调都重新断言「播放条灰置 + 停止播放」，
        // 不受任何其它路径（帧表更新、seek 触摸、解析完成）影响
        if (exporting) {
            setPlaybackSeekActive(false);
            if (this.playing) {
                stopPlaybackLoop();
            }
        }
        syncTaskUi();
    }

    /** v1.10.25：进度诊断（每 10% 一条，含控件真实值 —— 证明链路走到了控件上）。 */
    private void logTaskProgress(boolean exporting, int done, int total) {
        try {
            int pct = total > 0 ? (int) Math.min(100L, Math.max(0L, (long) done * 100L / total)) : 0;
            if (this.lastLoggedPct >= 0 && pct / 10 == this.lastLoggedPct / 10 && !(total > 0 && done >= total)) {
                return;
            }
            this.lastLoggedPct = pct;
            // v1.10.27：这里只报「任务真实进度」。之前顺带打印控件的当前值/文字，
            // 但本方法在 syncTaskUi 之前调用（控件还没刷新），日志里就成了「条实际=0」的假象
            Diag.warn(this, "L32", (exporting ? "导出进度 " : "解析进度 ") + done + "/" + total
                    + " (" + pct + "%)");
        } catch (Throwable ignored) {
        }
    }

    /** 导出进度：只写状态，节流 100ms 再刷界面（需求 4）；gen 不符（旧任务）直接丢弃。 */
    final void onParseProgress(int done, int total, int gen) {
        if (gen != this.exportGeneration || this.exportCancelled) {
            return;
        }
        this.taskExporting = true;
        this.parseDone = Math.max(done, 0);
        this.parseTotal = Math.max(total, 0);
        if (total <= 0) {
            return;
        }
        this.parseProgress = (int) Math.min(1000L, Math.max(0L, done * 1000L / total));
        logTaskProgress(true, done, total);
        long now = SystemClock.elapsedRealtime();
        if (now - this.lastProgressAt < 100L && done < total) {
            return;
        }
        this.lastProgressAt = now;
        syncTaskUi();
    }

    private final void finishSave(WidgetConfig config) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            Intrinsics.checkNotNull(manager);
            // v1.10.323（用户日志 18:41:51 那一拍）：**先作废尺寸缓存再推首帧**。
            // 实测：配置页开着的时候 launcher 报的还是默认 110dp（F11 130x115dp → 画布 426x377），
            // 而同一秒 W22 读到的真尺寸是 182x191dp（→ 202x196dp = 662x643px）。
            // widgetSize() 有 3 秒缓存，于是"导出完成"这一帧按 426x377 的错比例画布推上去，
            // 桌面 fitCenter 内接 → 就是用户说的"先是比本应的小一圈"，等真画布烘好才恢复。
            // 作废缓存后这一拍会重新问 launcher，首帧直接按真格子烘。
            WidgetRender.INSTANCE.invalidateSize(config.getWidgetId());
            WidgetRender.INSTANCE.render(this, manager, config, 0, null);
        } catch (Throwable th) {
        }
        getSharedPreferences("settings", 0).edit().putBoolean("all_stopped", false).putInt("last_overlay_id", config.getOverlayEnabled() ? config.getWidgetId() : getSharedPreferences("settings", 0).getInt("last_overlay_id", -1)).apply();
        PlaybackService.Companion.start(this);
        OverlayService.Companion.refresh(this);
        if (getIntent().hasExtra("appWidgetId")) {
            Intent result = new Intent().putExtra("appWidgetId", this.widgetId);
            Intrinsics.checkNotNullExpressionValue(result, "putExtra(...)");
            setResult(-1, result);
        }
        Diag.warn(this, "T13", "保存开关: overlay=" + config.getOverlayEnabled()
                + " visible=" + config.getOverlayVisible() + " kind=" + config.getKind()
                + " frames=" + config.getFrameCount()
                + " hires=" + (WidgetRender.hiResStatic(this, config) ? 1 : 0));
        String string = getString(R.string.cfg_saved);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        finish();
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0029  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
       /* JADX INFO: Access modifiers changed from: private */
    public final void openMaskGestureEditor() {
        WidgetConfig current = this.base;
        if (current == null) {
            return;
        }
        boolean hasSource = this.pickedUri != null || current.getSourceUri().length() > 0;
        if (!hasSource) {
            String message = getString(R.string.gesture_no_source);
            Intrinsics.checkNotNullExpressionValue(message, "getString(...)");
            this.toast(message);
            return;
        }
        Bitmap cached = this.previewSource;
        if (cached != null && cached.isRecycled()) {
            cached = null;
        }
        Bitmap src = cached;
        if (src == null && current.getFrameCount() > 0) {
            src = WidgetStore.INSTANCE.loadFrame(this, current, 0, 720, 720);
        }
        // v1.10.518（用户报「有素材、非导出时手势拖不动蒙版」）：根因是判据顺序 ——
        // 这里原来只认「previewSource 或**已烘好的第 0 帧**」，而刚导入、还没烘帧时两者都没有，
        // 于是直接走"没有素材"的提示。素材明明已经选了。
        // 与 v1.10.503 修 openOverlayEditor 同一条规则：**先看有没有素材，再看有没有帧**；
        // 没有帧就现解一帧（firstFrame 是同步解码单帧，720 长边，几毫秒到几十毫秒）。
        if (src == null && this.pickedUri != null) {
            try {
                src = MediaImporter.firstFrame(this, this.pickedUri, 720);
            } catch (Throwable ignored) {
                src = null;
            }
        }
        if (src == null) {
            String message2 = getString(R.string.gesture_no_source);
            Intrinsics.checkNotNullExpressionValue(message2, "getString(...)");
            this.toast(message2);
            return;
        }
        final boolean ownedFrame = src != cached;
        final Bitmap previewFrame = src;
        this.stopPlaybackLoop();
        int alpha = this.seekAlpha != null ? this.seekAlpha.getProgress() : 100;
        boolean border = this.cbBorder != null && this.cbBorder.isChecked();
        int shadow = this.seekShadow != null ? this.seekShadow.getProgress() : 0;
        MaskGestureDialog.show(this, previewFrame, mediaAspect(), this.selectedLayout, this.selectedShape,
                alpha, border, shadow, this.selectedShadowCurve, this.selectedRotation, this.selectedShapeRotation,
                this.selectedFitContent, this.selectedOffsetX, this.selectedOffsetY, this.selectedContentScale,
                this.selectedMaskX, this.selectedMaskY,
                new MaskGestureDialog.Listener() {
                    @Override
                    public void onMaskOffset(float xPercent, float yPercent) {
                        selectedMaskX = xPercent;
                        selectedMaskY = yPercent;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (ownedFrame && !previewFrame.isRecycled()) {
                            previewFrame.recycle();
                        }
                        refreshPreviewNow();
                        PreviewFrames.Result frames = previewFrames;
                        if (frames != null && frames.getFrames() != null && frames.getFrames().size() > 1) {
                            startPreviewFramesPlayback();
                        }
                    }
                },
                // v1.10.497（用户反馈）：这个面板是独立 Dialog，触摸事件不经过 Activity 的
                // dispatchTouchEvent —— 所以「完成」必须在这里单独上闸，否则导出中照样提交、
                // 而且一句提示都没有。确认弹窗关掉后，这个面板一起退出（不落值）。
                new MaskGestureDialog.Gate() {
                    @Override
                    public boolean blockDone(@NotNull android.app.Dialog panel) {
                        if (!outputRunning()) {
                            return false;
                        }
                        noteOutputLock();
                        showOutputBusyDialog(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    panel.dismiss();
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return true;
                    }
                });
    }

private final void openContentGestureEditor() {
      // v1.10.503（用户口径）：导出时**不弹提示、弹对话框**。
      // 必须放在所有「有没有素材/有没有帧」的判断之前 —— 否则导出中会先弹「未导入素材」。
      if (outputRunning()) {
         noteOutputLock();
         showOutputBusyDialog();
         return;
      }
      if (this.base != null) {
         WidgetConfig current = this.base;
         boolean hasSource = this.pickedUri != null || current.getSourceUri().length() > 0;
         if (!hasSource) {
            String var45 = this.getString(2131099731);
            Intrinsics.checkNotNullExpressionValue(var45, "getString(...)");
            this.toast(var45);
         } else {
            Bitmap var10000;
            if (this.previewSource != null) {
               Bitmap owned = this.previewSource;
               var10000 = !owned.isRecycled() ? owned : null;
            } else {
               var10000 = null;
            }

            Bitmap cached = var10000;
            var10000 = cached;
            if (cached == null) {
               ConfigActivity $this$openContentGestureEditor_u24lambda_u2454 = this;
               var10000 = current.getFrameCount() <= 0
                  ? null
                  : WidgetStore.INSTANCE.loadFrame((Context)$this$openContentGestureEditor_u24lambda_u2454, current, 0, 720, 720);
            }

            Bitmap src = var10000;
            if (src == null && this.pickedUri != null) {
               // v1.10.503：「解析时无限制」—— 帧还没烘出来时，直接解素材首帧给面板用。
               // 原来这里会 toast「未导入素材」，明明已经选了素材。
               try {
                  src = MediaImporter.firstFrame(this, this.pickedUri, 720);
               } catch (Throwable ignored) {
               }
            }
            if (src == null) {
               String var44 = this.getString(2131099845);
               Intrinsics.checkNotNullExpressionValue(var44, "getString(...)");
               this.toast(var44);
            } else {
               boolean owned = src != cached;
               this.stopPlaybackLoop();
               ThemeColors colors = ThemeManager.INSTANCE.colors((Context)this);
               float density = this.getResources().getDisplayMetrics().density;
               Dialog dialog = new Dialog((Context)this);
               dialog.requestWindowFeature(1);
               ThemeManager.INSTANCE.clearDialogWindow(dialog);   // v1.10.71：圆角不露方底
               View view = this.getLayoutInflater().inflate(2130903044, null);
               dialog.setContentView(view);
               Intrinsics.checkNotNull(view, "null cannot be cast to non-null type android.widget.LinearLayout");
               LinearLayout root = (LinearLayout)view;
               GradientDrawable title = new GradientDrawable();
               title.setColor(colors.getSurface());
               title.setCornerRadius(24.0F * density);
               root.setBackground((Drawable)title);
               TextView titlex = (TextView)view.findViewById(2131230816);
               TextView hint = (TextView)view.findViewById(2131230818);
               TextView close = (TextView)view.findViewById(2131230817);
               titlex.setTextColor(colors.getText());
               hint.setTextColor(colors.getTextMuted());
               close.setTextColor(colors.getText());
               close.setOnClickListener((v) -> ConfigActivity.openContentGestureEditor$lambda$56(dialog, v));
               FrameLayout host = (FrameLayout)view.findViewById(2131230819);
               host.setBackground((Drawable)ThemeManager.INSTANCE.rounded((Context)this, colors.getNeutral(), 18.0F));
               float ratio = this.mediaAspect();
               AspectRatioFrameLayout gesture = new AspectRatioFrameLayout((Context)this);
               gesture.setRatio(ratio);
               gesture.setLayoutParams((android.view.ViewGroup.LayoutParams)(new android.widget.FrameLayout.LayoutParams(-1, -1, 17)));
               host.addView((View)gesture);
               final ContentGestureView scaleLabel = new ContentGestureView((Context)this);
               scaleLabel.setSource(src);
               scaleLabel.setLayout(this.selectedLayout);
               scaleLabel.setShape(this.selectedShape);
               scaleLabel.setShapeRotation(this.selectedShapeRotation);
               scaleLabel.setFitContent(this.selectedFitContent);
               SeekBar var10001 = this.seekAlpha;
               if (this.seekAlpha == null) {
                  Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
                  var10001 = null;
               }

               scaleLabel.setAlphaPercent(var10001.getProgress());
               CheckBox var42 = this.cbBorder;
               if (this.cbBorder == null) {
                  Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
                  var42 = null;
               }

               scaleLabel.setBorder(var42.isChecked());
               var10001 = this.seekShadow;
               if (this.seekShadow == null) {
                  Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
                  var10001 = null;
               }

               scaleLabel.setShadowPercent(var10001.getProgress());
               scaleLabel.setContentScalePercent(this.selectedContentScale);
               scaleLabel.setMaskRotation(this.selectedRotation);
               scaleLabel.setOffsetXPercent(this.selectedOffsetX);
               scaleLabel.setOffsetYPercent(this.selectedOffsetY);
               gesture.addView(scaleLabel, (android.view.ViewGroup.LayoutParams)(new android.widget.FrameLayout.LayoutParams(-1, -1)));
               final TextView scaleLabelx = (TextView)view.findViewById(2131230820);
               // v1.10.74：滑条下面那行灰小字（-100 ~ +100，0 = 原尺寸）
               TextView scaleHint = (TextView)view.findViewById(R.id.gesture_scale_hint);
               if (scaleHint != null) {
                  scaleHint.setTextColor(colors.getTextMuted());
               }
               SeekBar scaleBar = (SeekBar)view.findViewById(2131230821);
               final TextView values = (TextView)view.findViewById(2131230822);
               scaleLabelx.setTextColor(colors.getText());
               values.setTextColor(colors.getText());
               scaleBar.setProgressTintList(ColorStateList.valueOf(colors.getPrimary()));
               scaleBar.setThumbTintList(ColorStateList.valueOf(colors.getPrimary()));
               scaleBar.setProgress(RangesKt.coerceIn(scaleLabel.getContentScalePercent(), 0, 200));
               openContentGestureEditor$refreshScaleLabel(scaleLabelx, this, scaleLabel);
               openContentGestureEditor$refreshValues(values, this, scaleLabel);
               scaleBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                  public void onProgressChanged(@Nullable SeekBar bar, int value, boolean fromUser) {
                     // v1.10.73：以前把标签写到了 hint 上（提示语被顶掉），这里改成写真正的标签
                     scaleLabel.setContentScale(value);
                     ConfigActivity.this.selectedContentScale = scaleLabel.getContentScalePercent();
                     ConfigActivity.openContentGestureEditor$refreshScaleLabel(scaleLabelx, ConfigActivity.this, scaleLabel);
                  }

                  public void onStartTrackingTouch(@Nullable SeekBar bar) {
                  }

                  public void onStopTrackingTouch(@Nullable SeekBar bar) {
                  }
               });
               scaleLabel.setOnChange((Function2<? super Float, ? super Float, Unit>)(new Function2<Float, Float, Unit>() {
                  public final Unit invoke(Float ox, Float oy) {
                     ConfigActivity.this.selectedOffsetX = ox.floatValue();
                     ConfigActivity.this.selectedOffsetY = oy.floatValue();
                     // v1.10.73：**双指缩放只改了画面，滑块和数值都没跟上**（而且 selectedContentScale
                     // 还是旧值 —— 不点完成就保存会存回旧的）。这里把三件事一起对齐：
                     //   ① 内部值 → ② 滑块位置 → ③ 标签文字
                     int scaleNow = scaleLabel.getContentScalePercent();
                     if (scaleNow != ConfigActivity.this.selectedContentScale) {
                        ConfigActivity.this.selectedContentScale = scaleNow;
                        scaleBar.setProgress(RangesKt.coerceIn(scaleNow, 0, 200));
                        ConfigActivity.openContentGestureEditor$refreshScaleLabel(scaleLabelx, ConfigActivity.this, scaleLabel);
                     }
                     ConfigActivity.openContentGestureEditor$refreshValues(values, ConfigActivity.this, scaleLabel);
                     return Unit.INSTANCE;
                  }
               }));
               Button reset = (Button)view.findViewById(2131230823);
               Button done = (Button)view.findViewById(2131230824);
               reset.setBackground((Drawable)ThemeManager.INSTANCE.outlined((Context)this, 0, colors.getNeutral(), 999.0F));
               reset.setTextColor(colors.getText());
               done.setBackground((Drawable)ThemeManager.INSTANCE.rounded((Context)this, colors.getPrimary(), 999.0F));
               done.setTextColor(colors.getOnPrimary());
               final float openOffsetX = this.selectedOffsetX;
               final float openOffsetY = this.selectedOffsetY;
               final int openContentScale = this.selectedContentScale;
               final boolean[] committed = {false};
               reset.setOnClickListener((v) -> ConfigActivity.openContentGestureEditor$lambda$59(scaleLabel, scaleBar, ConfigActivity.this, values, hint, v));
               done.setOnClickListener((v) -> {
                   // v1.10.497（用户反馈：「内容蒙版手势仍可以点击确定，且没弹出提示」）：
                   // 这个面板是独立 Dialog，触摸事件根本不经过 Activity 的 dispatchTouchEvent，
                   // 所以参数锁对它**天然失效** —— 必须在「完成」这一下单独上闸。
                   // 拦下时 committed 保持 false，弹窗确认把它关掉时 onDismiss 会把位移/缩放回滚，
                   // 既没落值，也等于退出了这个界面（用户口径）。
                   if (ConfigActivity.this.outputRunning()) {
                       ConfigActivity.this.noteOutputLock();
                       ConfigActivity.this.showOutputBusyDialog(new Runnable() {
                           @Override
                           public void run() {
                               try {
                                   dialog.dismiss();
                               } catch (Throwable ignored) {
                               }
                           }
                       });
                       return;
                   }
                   committed[0] = true;
                   ConfigActivity.openContentGestureEditor$lambda$60(dialog, v);
               });
               dialog.setCanceledOnTouchOutside(false);
               dialog.setOnDismissListener((d) -> {
                   ConfigActivity.openContentGestureEditor$lambda$61(false, (android.graphics.Bitmap) null, ConfigActivity.this, d);
                   if (!committed[0]) {
                       // v1.8.4：没点「完成」就关掉 → 回滚，不再“悄悄保存”
                       this.selectedOffsetX = openOffsetX;
                       this.selectedOffsetY = openOffsetY;
                       this.selectedContentScale = openContentScale;
                       this.refreshPreviewNow();
                   }
               });
               Window var37 = dialog.getWindow();
               if (var37 != null) {
                  var37.setBackgroundDrawable((Drawable)(new ColorDrawable(0)));
               }

               Window var38 = dialog.getWindow();
               if (var38 != null) {
                  var38.addFlags(2);
               }

               Window var39 = dialog.getWindow();
               if (var39 != null) {
                  var39.setDimAmount(0.55F);
               }

               Window var40 = dialog.getWindow();
               if (var40 != null) {
                  var40.setGravity(17);
               }

               dialog.show();
               int screenWidth = this.getResources().getDisplayMetrics().widthPixels;
               int screenHeight = this.getResources().getDisplayMetrics().heightPixels;
               Window var41 = dialog.getWindow();
               if (var41 != null) {
                  var41.setLayout((int)((float)screenWidth - (float)24 * density), (int)((float)screenHeight * 0.9F));
               }
            }
         }
      }
   }

    private static final void openContentGestureEditor$lambda$56(Dialog $dialog, View it) {
        Intrinsics.checkNotNullParameter($dialog, "$dialog");
        $dialog.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void openContentGestureEditor$refreshScaleLabel(TextView scaleLabel, ConfigActivity this$0, ContentGestureView gesture) {
        // v1.10.73：滑条 -100 ~ +100，内部 contentScalePercent 是 0~200 → 显示 (v - 100)
        scaleLabel.setText(this$0.getString(R.string.gesture_scale_value,
                new Object[]{String.format(java.util.Locale.US, "%+d", Integer.valueOf(gesture.getContentScalePercent() - 100))}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void openContentGestureEditor$refreshValues(TextView values, ConfigActivity this$0, ContentGestureView gesture) {
        values.setText(this$0.getString(R.string.gesture_values, new Object[]{Float.valueOf(gesture.getOffsetXPercent()), Float.valueOf(gesture.getOffsetYPercent())}));
    }

    /* compiled from: ConfigActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u0010\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\u0007\n\u0002\b\u0002\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0005"}, d2 = {"<anonymous>", "", "ox", "", "oy", "invoke"})
    /* renamed from: io.github.animwidget.ConfigActivity$openContentGestureEditor$4, reason: invalid class name */
    /* loaded from: classes.jar:io/github/animwidget/ConfigActivity$openContentGestureEditor$4.class */
    final class AnonymousClass4 implements Function2<Float, Float, Unit> {
        final /* synthetic */ TextView $values;
        final /* synthetic */ ContentGestureView $gesture;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass4(TextView $values, ContentGestureView $gesture) {
            this.$values = $values;
            this.$gesture = $gesture;
        }

        public final Unit invoke(Float ox, Float oy) {
            ConfigActivity.this.selectedOffsetX = ox;
            ConfigActivity.this.selectedOffsetY = oy;
            ConfigActivity.openContentGestureEditor$refreshValues(this.$values, ConfigActivity.this, this.$gesture);
            return Unit.INSTANCE;
        }
    }

    private static final void openContentGestureEditor$lambda$59(ContentGestureView $gesture, SeekBar $scaleBar, ConfigActivity this$0, TextView $scaleLabel, TextView $values, View it) {
        Intrinsics.checkNotNullParameter($gesture, "$gesture");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        $gesture.resetParams();
        $scaleBar.setProgress(100);
        this$0.selectedContentScale = 100;
        this$0.selectedOffsetX = 0.0f;
        this$0.selectedOffsetY = 0.0f;
        openContentGestureEditor$refreshScaleLabel($scaleLabel, this$0, $gesture);
        openContentGestureEditor$refreshValues($values, this$0, $gesture);
    }

    private static final void openContentGestureEditor$lambda$60(Dialog $dialog, View it) {
        Intrinsics.checkNotNullParameter($dialog, "$dialog");
        $dialog.dismiss();
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x002b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static final void openContentGestureEditor$lambda$61(boolean r5, android.graphics.Bitmap r6, io.github.animwidget.ConfigActivity r7, android.content.DialogInterface r8) {
        boolean owned = r5;
        Bitmap frame = r6;
        ConfigActivity this$0 = r7;
        /*
            r0 = r7
            java.lang.String r1 = "this$0"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r0, r1)
            r0 = r5
            if (r0 == 0) goto Lf
            r0 = r6
            r0.recycle()
        Lf:
            r0 = r7
            r0.refreshPreviewNow()
            r0 = r7
            io.github.animwidget.PreviewFrames$Result r0 = r0.previewFrames
            r1 = r0
            if (r1 == 0) goto L2b
            java.util.List r0 = r0.getFrames()
            r1 = r0
            if (r1 == 0) goto L2b
            int r0 = r0.size()
            goto L2d
        L2b:
            r0 = 0
        L2d:
            r1 = 1
            if (r0 <= r1) goto L38
            r0 = r7
            r0.startPreviewFramesPlayback()
            goto L53
        L38:
            r0 = r7
            io.github.animwidget.WidgetConfig r0 = r0.base
            r1 = r0
            if (r1 == 0) goto L46
            int r0 = r0.getFrameCount()
            goto L48
        L46:
            r0 = 0
        L48:
            r1 = 1
            if (r0 <= r1) goto L53
            r0 = r7
            r1 = 0
            r2 = 1
            r3 = 0
            startPlayback$default(r0, r1, r2, r3)
        L53:
            return
        */
        if (owned && frame != null && !frame.isRecycled()) {
            frame.recycle();
        }
        this$0.refreshPreviewNow();
        int count = 0;
        PreviewFrames.Result frames = this$0.previewFrames;
        if (frames != null && frames.getFrames() != null) {
            count = frames.getFrames().size();
        }
        if (count > 1) {
            this$0.startPreviewFramesPlayback();
        } else if (this$0.pickedUri == null) {
            // 没有未保存的新素材时，才允许回退到已保存的旧帧
            WidgetConfig config = this$0.base;
            if (config != null && config.getFrameCount() > 1) {
                startPlayback$default(this$0, false, 1, null);
            }
        } else {
            Diag.warn(this$0, "L10", "新素材只有 " + count + " 帧，无法播放(不回退旧素材)");
        }
    }

    private final void openOverlayEditor() throws JSONException {
        // v1.10.503：导出中先弹对话框，且**不弹提示**（用户口径）。
        if (outputRunning()) {
            noteOutputLock();
            showOutputBusyDialog();
            return;
        }
        WidgetConfig current = this.base;
        if (current == null) {
            return;
        }
        // v1.10.503（用户口径）：判据是「有没有素材」，不是「有没有烘好的帧」——
        // 解析中/刚导入时帧还没烘出来，但素材明明已经选了，不该再报「未导入素材」。
        boolean hasSource = this.pickedUri != null || current.getSourceUri().length() > 0;
        if (!hasSource) {
            String string = getString(R.string.cfg_need_source);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
        } else {
            if (!Settings.canDrawOverlays(this)) {
                String string2 = getString(R.string.overlay_permission);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                toast(string2);
                try {
                    startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Throwable th) {
                    return;
                }
            }
            getSharedPreferences("settings", 0).edit().putBoolean("all_stopped", false).putInt("last_overlay_id", this.widgetId).apply();
            WidgetConfig updated = WidgetConfig.copy$default(current, 0, null, null, null, 0, 0, 0, 0, 0, 0, null, 0, false, null, true, true, 0, 0, 0, 0, 0, 0, 0, false, 0.0f, 0.0f, 0, 0.0f, 0.0f, null, 1073692671, null);
            WidgetStore.INSTANCE.save(this, updated);
            OverlayService.Companion.edit(this, this.widgetId);
            finish();
        }
    }

    private final void setBusy(boolean value) {
        // v1.10.497（用户要求 6）：**参数不置灰**（主按钮也不禁用）——
        // 摸到参数不再「点了没反应」，而是弹一次「当前方案正在输出」（见 dispatchTouchEvent）。
        //
        // **唯一的例外：素材选项。** 用户明确「导出时素材选项依然要灰置」：
        // 换素材会重开一整轮导出，这不该靠弹窗去劝，直接灰掉最清楚（这里保持 v1.10.313 的样子）。
        try {
            if (this.mediaSelector != null) {
                this.mediaSelector.setEnabled(!value);
                this.mediaSelector.setAlpha(value ? 0.38f : 1.0f);
            }
        } catch (Throwable ignored) {
        }
        this.busy = value;
        syncTaskUi();
        if (!value) {
            ProgressBar progressBar = this.progress;
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
        }
    }

    /**
     * v1.10.497（用户要求 4）：主按钮文案 —— 「导出组件」/「保存配置」。
     *
     * <p>换语言时也要跟着换（走 {@link #updateLabels()} → 这里），所以单独一个幂等方法。</p>
     */
    private void refreshSaveButtonText() {
        try {
            Button button = this.btnSave;
            if (button == null) {
                return;
            }
            CharSequence text = getString(this.alreadyExported ? R.string.cfg_save : R.string.cfg_export_widget);
            if (text != null && !text.equals(button.getText())) {
                button.setText(text);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.497（用户要求 6）：**这一刻是不是「正在输出」**。
     *
     * <p>两个阶段都算：导出写帧（{@link #exportRunning}）与烘焙写帧（{@link #bakeStage}）。
     * 这两段里改参数没有任何意义 —— 出去的已经是开始那一刻的快照，改了只会让人以为生效了。</p>
     */
    private boolean outputRunning() {
        return this.exportRunning || this.bakeStage;
    }

    /**
     * v1.10.497（用户要求 6）：输出期间**被锁住的参数**。
     *
     * <p>按「点得动、能改值」的控件逐个登记 —— 不用整页蒙一层，因为整页蒙层会把滚动也吃掉，
     * 那就不叫「仍可触摸」了。命中判定用**矩形**而不是子控件分派，所以像 {@code ll_shapes}
     * （动态生成的形状格）和 {@code ll_shadow_curve}（整块曲线面板）这种容器只需要登记容器本身。</p>
     *
     * <p>清单对应需求原文：一键适配素材 / 布局 / 形状蒙版（含旋转与位置）/ 画面（帧率·清晰度·
     * 压缩质量）/ 外观（不透明度·阴影（含曲线面板）·等比例缩放）/ 细边框 / 还原指数（标准·高清·1:1）/
     * 流畅覆盖层 / 素材选择，外加主按钮自己（点它也要弹同一句话）。</p>
     */
    private static final int[] LOCKED_PARAM_IDS = {
            R.id.btn_adapt,
            R.id.ly_scroll, R.id.ll_shapes,
            R.id.btn_fit_content,          // 「自动适配」会改画布口径 → 锁
            // 注意：「全部形状（展开/收起）」(btn_shapes_toggle) **不锁** —— 用户明确要求它随时能展开收起，
            // 它只切换显示，不改任何写入的参数（形状本身由 ll_shapes 里的格子决定，格子是锁住的）。
            R.id.seek_fps, R.id.seek_quality, R.id.seek_edge,
            R.id.seek_alpha, R.id.seek_shadow, R.id.seek_scale,
            // 「阴影自定义」(btn_shadow_curve) **不锁** —— 用户要求它能展开/收起；
            // 展开后的面板（ll_shadow_curve）连同里面的曲线与预设一起锁住。
            R.id.ll_shadow_curve,
            R.id.cb_border, R.id.row_anim, R.id.row_hires,
            R.id.cb_overlay, R.id.cb_overlay_visible,
            // v1.10.499（用户要求）：原来这四项是「只预览、不写入」的例外（能拖、画面立刻变）。
            // 用户明确要求**取消这个例外**：输出期间不可点击，摸到就弹「当前方案正在输出」。
            // 理由也是性能：每拖一下都要按 887×685 重新合成一帧（≈25ms 起），
            // 导出正占着核时就是肉眼可见的迟钝。
            R.id.btn_rotate, R.id.btn_rotate_shape,
            R.id.btn_mask_gesture, R.id.btn_overlay_edit,
            R.id.btn_save,
    };

    /** 懒建的控件缓存（{@link #onCreate} 里建一次；布局没变的页面不会重建）。 */
    @Nullable
    private View[] lockedViews;

    /** v1.10.499：提示层改成页内覆盖层（不再是 Dialog，见 {@link BusyOverlay}）。 */
    @Nullable
    private BusyOverlay busyOverlay;

    /** 页面静止时把提示层准备好（建树 + 后台抓底图）；触摸那一下只切 visibility。 */
    private final Runnable busyOverlayPrepare = new Runnable() {
        @Override
        public void run() {
            try {
                busyOverlay().refreshBackdrop();
            } catch (Throwable ignored) {
            }
        }
    };

    /**
     * v1.10.497（用户口径 · 手势分档）：
     * <ul>
     *   <li>{@link #LOCK_TAP} 点击类 —— **不吞 DOWN**（原生滚动/fling 照常），松手时判定「是不是想点」；</li>
     *   <li>{@link #LOCK_TRACK} 滑块轨道 —— 没摸到拇指 = 没想滑动：吞掉 DOWN（否则 SeekBar 会直接跳到这个位置），
     *       但一拖动就手动滚页面；</li>
     *   <li>{@link #LOCK_SLIDE} 滑块拇指 —— 摸到拇指 = 想滑动：立刻弹窗并整段吞掉。</li>
     * </ul>
     */
    private static final int LOCK_NONE = 0;
    private static final int LOCK_TAP = 1;
    private static final int LOCK_TRACK = 2;
    private static final int LOCK_SLIDE = 3;

    private int lockKind = LOCK_NONE;
    @Nullable
    private View lockTarget;
    private float lockDownX;
    private float lockDownY;
    private float lockLastY;
    private boolean lockMoved;

    private void buildLockedViews() {
        if (this.lockedViews != null) {
            return;
        }
        this.lockedViews = new View[LOCKED_PARAM_IDS.length];
        for (int i = 0; i < LOCKED_PARAM_IDS.length; i++) {
            this.lockedViews[i] = findViewById(LOCKED_PARAM_IDS[i]);
        }
    }

    /** 命中哪一个被锁控件（返回控件本身，便于分档：滑块 / 点击）。隐藏的 tab 页 getGlobalVisibleRect 自然 false。 */
    @Nullable
    private View lockedViewAt(float rawX, float rawY) {
        buildLockedViews();
        View[] views = this.lockedViews;
        if (views == null) {
            return null;
        }
        android.graphics.Rect rect = new android.graphics.Rect();
        for (View view : views) {
            if (view == null || view.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (!view.getGlobalVisibleRect(rect)) {
                continue;
            }
            if (rawX >= rect.left && rawX < rect.right && rawY >= rect.top && rawY < rect.bottom) {
                return view;
            }
        }
        return null;
    }

    /** v1.10.503：点下去就开面板的按钮 —— 输出期间整段吞掉（连拖动都不许透传）。 */
    private boolean isFullBlock(@NotNull View hit) {
        int id = hit.getId();
        return id == R.id.btn_rotate || id == R.id.btn_rotate_shape
                || id == R.id.btn_mask_gesture || id == R.id.btn_overlay_edit;
    }

    /** 手指是不是落在滑块的「拇指」上 —— 用户口径：没接触拇指 = 用户没有想滑动。 */
    private boolean thumbHit(@NotNull SeekBar bar, float rawX, float rawY) {
        int[] loc = new int[2];
        try {
            bar.getLocationOnScreen(loc);
        } catch (Throwable ignored) {
            return false;
        }
        float density = getResources().getDisplayMetrics().density;
        float cx;
        float cy;
        android.graphics.drawable.Drawable thumb = bar.getThumb();
        if (thumb != null) {
            android.graphics.Rect bounds = thumb.getBounds();
            cx = loc[0] + (bounds.left + bounds.right) * 0.5f;
            cy = loc[1] + (bounds.top + bounds.bottom) * 0.5f;
        } else {
            // 没有拇指 drawable：按当前进度位置估一个中心（不弹窗的依据仍然成立）
            int inner = bar.getWidth() - bar.getPaddingLeft() - bar.getPaddingRight();
            float ratio = bar.getMax() <= 0 ? 0.0f : (bar.getProgress() / (float) bar.getMax());
            cx = loc[0] + bar.getPaddingLeft() + Math.max(0, inner) * ratio;
            cy = loc[1] + bar.getHeight() * 0.5f;
        }
        float halfW = 14.0f * density;
        float halfH = Math.max(bar.getHeight() * 0.5f, 14.0f * density);
        return Math.abs(rawX - cx) <= halfW && Math.abs(rawY - cy) <= halfH;
    }

    /**
     * 吞掉 UP 时给控件补一个 CANCEL，否则它会一直停在「按下」状态。
     *
     * <p>**要从最深的那个子控件往上补**：命中点常常是容器（`ll_shapes` 里的形状格、
     * `ll_shadow_curve` 里的按钮），而真正按下的是里面的格子。只给容器发 CANCEL 时，
     * ViewGroup 因为没有活动触摸目标会把事件吞在自己那层，里面的格子会一直亮着 —— 看着像没松开。</p>
     */
    private void cancelLockTarget(@NotNull android.view.MotionEvent up) {
        View target = this.lockTarget;
        if (target == null) {
            return;
        }
        View view = deepestAt(target, up.getRawX(), up.getRawY());
        for (int guard = 0; view != null && guard < 12; guard++) {
            sendCancel(view, up);
            if (view == target) {
                break;
            }
            android.view.ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
    }

    private static void sendCancel(@NotNull View view, @NotNull android.view.MotionEvent up) {
        try {
            android.view.MotionEvent cancel = android.view.MotionEvent.obtain(
                    up.getDownTime(), up.getEventTime(), android.view.MotionEvent.ACTION_CANCEL,
                    up.getX(), up.getY(), up.getMetaState());
            view.dispatchTouchEvent(cancel);
            cancel.recycle();
        } catch (Throwable ignored) {
        }
    }

    /** 这一点下面最深的那个可见子控件（矩形命中；找不到就是传进来的那个）。 */
    @NotNull
    private static View deepestAt(@NotNull View root, float rawX, float rawY) {
        if (!(root instanceof ViewGroup)) {
            return root;
        }
        ViewGroup group = (ViewGroup) root;
        android.graphics.Rect rect = new android.graphics.Rect();
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (!child.getGlobalVisibleRect(rect)) {
                continue;
            }
            if (rawX >= rect.left && rawX < rect.right && rawY >= rect.top && rawY < rect.bottom) {
                return deepestAt(child, rawX, rawY);
            }
        }
        return root;
    }

    /** 松手点还在不在那个控件里（移出去松手 = 取消点击，用户口径）。 */
    private boolean insideLockTarget(float rawX, float rawY) {
        View target = this.lockTarget;
        if (target == null) {
            return false;
        }
        android.graphics.Rect rect = new android.graphics.Rect();
        if (!target.getGlobalVisibleRect(rect)) {
            return false;
        }
        return rawX >= rect.left && rawX < rect.right && rawY >= rect.top && rawY < rect.bottom;
    }

    /** v1.10.497：参数被拦下的**唯一日志口径**（W50 全仓只有这一处拼文本，diag 门禁 A 才不会报多义）。 */
    private void noteOutputLock() {
        Diag.warn(this, "W50", "输出锁参 阶段=" + (this.bakeStage ? "烘焙" : "导出"));
    }

    private void resetLockGesture() {
        this.lockKind = LOCK_NONE;
        this.lockTarget = null;
        this.lockMoved = false;
    }

    /**
     * v1.10.497（用户要求 6 + 手势口径）：输出进行中「参数能摸但不执行」，只弹一次
     * 「当前方案正在输出」；**同时不许把页面滚死**。
     *
     * <p>三条判定（用户原话）：</p>
     * <ul>
     *   <li><b>滑块</b>：没接触拇指 = 用户没有想滑动 → **不弹窗**；摸到拇指 → 立刻弹；</li>
     *   <li><b>点击</b>：没在按下区域附近松开 = 用户没有想点击 → **不弹窗**（这条路径就是滚动）；</li>
     *   <li>剩下的（确实想点、确实摸了拇指）→ 吞掉这一次点击动作 + 弹窗，控件本身收不到 UP。</li>
     * </ul>
     */
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        try {
            // v1.10.500（用户反馈「导出中弹窗点不了确认，只能按返回键」）：
            // 命中判定只看**被锁控件的矩形**，不看谁在最上层 —— 覆盖层盖在页面上，
            // 手指落在「确认」上时下面正好压着某个参数（seek_* / ll_*），于是整段被当成锁参吞掉。
            // 覆盖层显示期间，事件原样交给视图树（它自己会吃掉并在卡片外点按时收起）。
            BusyOverlay overlay = this.busyOverlay;
            if (overlay != null && overlay.isShowing()) {
                return super.dispatchTouchEvent(ev);
            }
            if (!outputRunning()) {
                resetLockGesture();
                return super.dispatchTouchEvent(ev);
            }
            final int action = ev.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                resetLockGesture();
                View hit = lockedViewAt(ev.getRawX(), ev.getRawY());
                if (hit == null) {
                    return super.dispatchTouchEvent(ev);        // 空白/标题/预览：随便滚
                }
                // v1.10.500（用户反馈「阴影自定义界面会判定滑块条」）：
                // 命中常见的是一整个容器（ll_shadow_curve / ll_shapes）。原来容器一律按「点击」处理，
                // 于是里面的滑块被**拖动**时反而直接透传给了原生 SeekBar（值真的变），
                // 而轻点轨道又会被判成点击弹窗 —— 两种口径都不对。
                // 这里往下找一层：手指下面真的是滑块，就按滑块的拇指/轨道分档处理。
                if (!(hit instanceof SeekBar)) {
                    View deep = deepestAt(hit, ev.getRawX(), ev.getRawY());
                    if (deep instanceof SeekBar) {
                        hit = deep;
                    }
                }
                this.lockTarget = hit;
                this.lockDownX = ev.getRawX();
                this.lockDownY = ev.getRawY();
                this.lockLastY = this.lockDownY;
                // v1.10.503：旋转/手势这四颗**按钮**整段吞掉。
                // LOCK_TAP 是给「既能点又能滚」的行用的（不吞 DOWN）；放到按钮上，
                // 手指只要移动超过 slop 就被判成「不是点击」→ UP 透传给按钮 → 执行器照跑，
                // 于是导出中不是弹对话框，而是弹「未导入素材」。
                if (isFullBlock(hit)) {
                    this.lockKind = LOCK_SLIDE;
                    noteOutputLock();
                    showOutputBusyDialog();
                    return true;
                }
                if (hit instanceof SeekBar) {
                    if (thumbHit((SeekBar) hit, ev.getRawX(), ev.getRawY())) {
                        // 摸到拇指 = 想滑动：立刻弹窗，整段吞掉（滑块不许动、也不许有按下反馈）
                        this.lockKind = LOCK_SLIDE;
                        noteOutputLock();
                        showOutputBusyDialog();
                    } else {
                        // 摸到轨道 = 没想滑动：先吞掉 DOWN（否则 SeekBar 会直接跳到这个位置）
                        this.lockKind = LOCK_TRACK;
                    }
                    return true;
                }
                // 点击类：**不吞 DOWN** —— 原生滚动与 fling 照常工作，松手那一刻才判定
                this.lockKind = LOCK_TAP;
                return super.dispatchTouchEvent(ev);
            }

            if (this.lockKind == LOCK_NONE) {
                return super.dispatchTouchEvent(ev);
            }
            float slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();

            if (action == android.view.MotionEvent.ACTION_MOVE) {
                if (Math.abs(ev.getRawX() - this.lockDownX) > slop
                        || Math.abs(ev.getRawY() - this.lockDownY) > slop) {
                    this.lockMoved = true;
                }
                if (this.lockKind == LOCK_TRACK) {
                    // 轨道上的拖动 = 想滚页面：自己滚（这几行只有滑块高，够用；fling 交给别处）
                    ScrollView scroller = configScrollView();
                    if (scroller != null) {
                        int dy = Math.round(this.lockLastY - ev.getRawY());
                        if (dy != 0) {
                            scroller.scrollBy(0, dy);
                        }
                    }
                    this.lockLastY = ev.getRawY();
                    return true;
                }
                if (this.lockKind == LOCK_TAP) {
                    return super.dispatchTouchEvent(ev);        // 拖走 = 想滚动，放行
                }
                return true;                                    // SLIDE：整段吞掉
            }

            if (action == android.view.MotionEvent.ACTION_UP) {
                if (this.lockKind == LOCK_TAP) {
                    boolean tapped = !this.lockMoved
                            && Math.abs(ev.getRawX() - this.lockDownX) <= slop
                            && Math.abs(ev.getRawY() - this.lockDownY) <= slop
                            && insideLockTarget(ev.getRawX(), ev.getRawY());
                    if (tapped) {
                        cancelLockTarget(ev);   // 控件收不到 UP = 不执行；补 CANCEL 清掉按下态
                        noteOutputLock();
                        showOutputBusyDialog();
                        resetLockGesture();
                        return true;
                    }
                }
                boolean consumed = this.lockKind == LOCK_SLIDE || this.lockKind == LOCK_TRACK;
                resetLockGesture();
                return consumed || super.dispatchTouchEvent(ev);
            }
            if (action == android.view.MotionEvent.ACTION_CANCEL) {
                resetLockGesture();
                return super.dispatchTouchEvent(ev);
            }
        } catch (Throwable ignored) {
        }
        return super.dispatchTouchEvent(ev);
    }

    /** v1.10.497：那一句「当前方案正在输出」——同一个页面只留一个实例，连点不会叠成一摞。 */
    private void showOutputBusyDialog() {
        showOutputBusyDialog(null);
    }

    /**
     * @param alsoClose 弹窗点「确认」之后要一并关掉的**子界面**（蒙版/内容那几张参数面板）。
     *                  用户口径：确认退出的是那些界面，不是编辑页、更不是应用。
     */
    private void showOutputBusyDialog(@Nullable final Runnable alsoClose) {
        try {
            BusyOverlay overlay = busyOverlay();
            if (overlay.isShowing()) {
                return;
            }
            // v1.10.497（用户澄清）：点「确认」= **只把这个提示收掉**，绝不退编辑页、更不退出应用；
            // 若这次是从某张参数面板里触发的（alsoClose 非空），确认时把那面板一起关掉
            // —— 这就是用户说的「退出那些界面」（面板不会落值：它走的是 cancel 路径）。
            //
            // v1.10.499：这里**只切 visibility**。视图树与底图在页面静止时已经备好
            // （见 busyOverlayPrepare），输入分发路径上不再有截图 / inflate / 新窗口。
            overlay.show(alsoClose == null ? null : new Runnable() {
                @Override
                public void run() {
                    try {
                        alsoClose.run();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
            // 提示失败只影响观感（参数照样被拦住）：不新增代号，也不许把页面拖崩
        }
    }

    @NotNull
    private BusyOverlay busyOverlay() {
        BusyOverlay overlay = this.busyOverlay;
        if (overlay == null) {
            overlay = new BusyOverlay(this);
            this.busyOverlay = overlay;
        }
        overlay.prepare();
        return overlay;
    }

    /** 调用点（诊断用）。 */
    private static String caller() {
        StackTraceElement[] trace = new Throwable().getStackTrace();
        for (int i = 2; i < trace.length && i < 6; i++) {
            String name = trace[i].getMethodName();
            if (name != null && !name.startsWith("lambda$")) {
                return name;
            }
        }
        return "?";
    }

    /** 丢弃当前预览帧（换素材时调用）。 */
    private final void clearPreviewFrames() {
        PreviewFrames.Result old = this.previewFrames;
        this.previewFrames = null;
        this.previewFramesLoading = false;
        // v1.8.17：作废所有在途抽帧的发布
        this.extractGeneration++;
        // v1.8.10：这里原来会 recycle 旧帧，但渲染线程可能正拿着其中一帧在画，
        // 于是崩溃日志里出现 “Canvas: trying to use a recycled bitmap”，
        // 预览也就永远不再更新。改成只丢引用，交给 GC 回收。

        this.stopPlaybackLoop();
        // v1.10.0：旧素材的解码帧没用了，清掉缓存（只丢引用，不 recycle —— 可能还在屏上）
        if (this.previewFrameCache != null) {
            this.previewFrameCache.clear();
        }
        if (this.seekPlayback != null) {
            this.seekPlayback.setMax(0);
            this.seekPlayback.setProgress(0);
        }
    }

    private final void loadPreview(final Uri uri, final MediaKind kind) {
        new Thread(() -> {
            try {
                loadPreview$lambda$66(kind, ConfigActivity.this, uri);
            } catch (Throwable t) {
                CrashLog.save(ConfigActivity.this, Thread.currentThread(), t);
                Diag.warn(ConfigActivity.this, "P01", "缩略图解码失败: " + t.getClass().getSimpleName());
                final String reason = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                this.ui.post(() -> {
                    // v1.10.2：只是缩略图失败，圆环仍由任务状态决定（别把在跑的解析环藏掉）
                    ConfigActivity.this.syncTaskUi();
                    String failed = ConfigActivity.this.getString(R.string.status_failed, new Object[]{reason});
                    Intrinsics.checkNotNullExpressionValue(failed, "getString(...)");
                    ConfigActivity.this.toast(failed);
                });
            }
        }).start();
    }

    private static final void loadPreview$lambda$66(MediaKind $kind, ConfigActivity this$0, Uri $uri) throws java.io.IOException {
        Bitmap bitmap;
        Bitmap bitmap2;
        Movie movie;
        Intrinsics.checkNotNullParameter($kind, "$kind");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($uri, "$uri");
        try {
        } catch (Throwable th) {
            bitmap = null;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[$kind.ordinal()]) {
            case 1:
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(this$0, $uri);
                    Bitmap frameAtTime = ColorFix.toSrgb(retriever.getFrameAtTime(0L));
                    retriever.release();
                    bitmap2 = frameAtTime;
                    bitmap = bitmap2;
                    Bitmap bitmap3 = bitmap;
                    final Bitmap bmpFinal = bitmap;
                    this$0.ui.post(() -> {
                        loadPreview$lambda$66$lambda$65(this$0, bmpFinal);
                    });
                    return;
                } catch (Throwable th2) {
                    retriever.release();
                    throw th2;
                }
            case 2:
                InputStream inputStreamOpenInputStream = this$0.getContentResolver().openInputStream($uri);
                if (inputStreamOpenInputStream != null) {
                    InputStream inputStream = inputStreamOpenInputStream;
                    try {
                        InputStream it = inputStream;
                        Movie movieDecodeStream = Gif.decode(it);
                        CloseableKt.closeFinally(inputStream, (Throwable) null);
                        movie = movieDecodeStream;
                    } catch (Throwable th3) {
                        CloseableKt.closeFinally(inputStream, (Throwable) null);
                        throw th3;
                    }
                } else {
                    movie = null;
                }
                Movie movie2 = movie;
                if (movie2 != null) {
                    int w = RangesKt.coerceAtLeast(movie2.width(), 1);
                    int h = RangesKt.coerceAtLeast(movie2.height(), 1);
                    // v1.8.3：大 GIF 直接按原尺寸解码同样会 OOM；长边封顶 1600 并等比缩放。
                    float gifScale = Math.min(1.0f, 1600.0f / Math.max(w, h));
                    int outW = RangesKt.coerceAtLeast((int) (w * gifScale), 1);
                    int outH = RangesKt.coerceAtLeast((int) (h * gifScale), 1);
                    Bitmap b = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
                    Intrinsics.checkNotNullExpressionValue(b, "createBitmap(...)");
                    movie2.setTime(0);
                    Canvas gifCanvas = new Canvas(b);
                    gifCanvas.scale(outW / (float) w, outH / (float) h);
                    movie2.draw(gifCanvas, 0.0f, 0.0f);
                    bitmap2 = b;
                } else {
                    bitmap2 = null;
                }
                bitmap = bitmap2;
                Bitmap bitmap32 = bitmap;
                final Bitmap bmpFinal2 = bitmap;
                this$0.ui.post(() -> {
                    loadPreview$lambda$66$lambda$65(this$0, bmpFinal2);
                });
                return;
            default:
                InputStream inputStreamOpenInputStream2 = this$0.getContentResolver().openInputStream($uri);
                if (inputStreamOpenInputStream2 != null) {
                    InputStream inputStream2 = inputStreamOpenInputStream2;
                    try {
                        InputStream it2 = inputStream2;
                        BitmapFactory.Options $this$loadPreview_u24lambda_u2466_u24lambda_u2464_u24lambda_u2463 = new BitmapFactory.Options();
                        // v1.8.3：原来固定 inSampleSize = 2，8000px 的原图会解出 4000×4000（64MB）
                        // 直接把进程 OOM 掉。改成按长边 1600 采样。
                        $this$loadPreview_u24lambda_u2466_u24lambda_u2464_u24lambda_u2463.inSampleSize = previewSample(this$0, $uri);
                        Unit unit = Unit.INSTANCE;
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(it2, null, $this$loadPreview_u24lambda_u2466_u24lambda_u2464_u24lambda_u2463);
                        CloseableKt.closeFinally(inputStream2, (Throwable) null);
                        bitmap2 = ColorFix.toSrgb(bitmapDecodeStream);
                    } catch (Throwable th4) {
                        CloseableKt.closeFinally(inputStream2, (Throwable) null);
                        throw th4;
                    }
                } else {
                    bitmap2 = null;
                }
                bitmap = bitmap2;
                Bitmap bitmap322 = bitmap;
                final Bitmap bmpFinal3 = bitmap;
                this$0.ui.post(() -> {
                    loadPreview$lambda$66$lambda$65(this$0, bmpFinal3);
                });
                return;
        }
    }

    private static final void loadPreview$lambda$66$lambda$65(ConfigActivity this$0, Bitmap $bitmap) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.8.11：这里原来会 recycle 旧 previewSource，但渲染线程的队列里
        // 可能还有拿着旧图的任务（renderPreviewNow/renderPlaybackFrame），
        // 一画就是 “Canvas: trying to use a recycled bitmap” → 杀进程。
        // 同预览帧策略：只丢引用，交 GC。
        this$0.previewSource = $bitmap;
        this$0.updatePreviewBox();
        // v1.9.2：缩略图（当前素材真实比例）到位后立刻刷新布局卡片与线框
        this$0.refreshLayoutChips(this$0.layoutChipIds);
        this$0.schedulePreview();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void stopPlaybackLoop() {
        // L09：谁把播放停了（带调用点，节流防刷屏）—— 预览「动两帧就停」就看它
        if (this.playing) {
            StackTraceElement[] trace = new Throwable().getStackTrace();
            String caller = trace.length > 1 ? trace[1].getMethodName() : "?";
            Diag.warnThrottled(this, "L09", "预览播放停止, 来自=" + caller);
        }
        this.playing = false;
        Runnable it = this.playbackRun;
        if (it != null) {
            this.ui.removeCallbacks(it);
        }
        this.playbackRun = null;
    }

    static /* synthetic */ void startPlayback$default(ConfigActivity configActivity, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = false;
        }
        configActivity.startPlayback(z);
    }

    private final void startPlayback(boolean holdFirst) {
        // v1.10.50：和 startPreviewFramesPlayback 一样，任务进行中一律拒绝启动
        //（原来这条路径没有门闸 —— 迟到的静态重渲回调能把预览重新播起来）
        if (this.parseTaskActive || this.busy) {
            Diag.warn(this, "L30", "任务进行中，忽略预览播放请求(startPlayback)");
            return;
        }
        final WidgetConfig config = this.base;
        if (config != null && config.getFrameCount() > 1) {
            stopPlaybackLoop();
            this.playing = true;
            final long interval = RangesKt.coerceAtLeast(1000 / RangesKt.coerceIn(config.getFps(), 1, 50), 16L);
            final long startAt = SystemClock.elapsedRealtime() - (holdFirst ? interval : 0L);
            final int fpsForTime = RangesKt.coerceAtLeast(config.getFps(), 1);
            final int total = config.getFrameCount();
            Runnable runnable = new Runnable() { // from class: io.github.animwidget.ConfigActivity$startPlayback$tick$1
                @Override // java.lang.Runnable
                public void run() {
                    if (!ConfigActivity.this.playing) {
                        return;
                    }
                    // v1.10.50：每一拍都验任务状态 —— 任务一开，播放立刻停死且不再重排
                    if (ConfigActivity.this.parseTaskActive || ConfigActivity.this.busy) {
                        ConfigActivity.this.stopPlaybackLoop();
                        return;
                    }
                    long now = SystemClock.elapsedRealtime();
                    long step = Math.max(now - startAt, 0L) / interval;
                    int index = (int) (step % total);
                    ConfigActivity.this.playIndex = index;
                    ConfigActivity.this.renderPlaybackFrame(index);
                    ConfigActivity.this.publishPlaybackPosition(index,
                            ConfigActivity.this.formatTime((index * 1000L) / fpsForTime));
                    long nextAt = startAt + (step + 1) * interval;
                    ConfigActivity.this.ui.postDelayed(this, Math.max(nextAt - SystemClock.elapsedRealtime(), 1L));
                }
            };
            this.playbackRun = runnable;
            this.ui.postDelayed(runnable, holdFirst ? interval : 0L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void renderPlaybackFrame(int index) {
        // v1.10.50：任务（解析/导出）进行中一律不合成 —— 导出期间画面必须完全冻结
        if (this.parseTaskActive || this.busy) {
            Diag.warnThrottled(this, "L31", "任务中：忽略播放帧合成 idx=" + index);
            return;
        }
        WidgetConfig config = this.base;
        if (config != null && config.getFrameCount() > 0 && this.playbackBusy.compareAndSet(false, true)) {
            int generation = this.previewGeneration.incrementAndGet();
            new Thread(() -> {
                renderPlaybackFrame$lambda$69(ConfigActivity.this, config, index, generation);
            }).start();
        }
    }

    private static final void renderPlaybackFrame$lambda$69(ConfigActivity this$0, WidgetConfig $config, int $index, int $generation) {
        Bitmap bitmapCompose;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($config, "$config");
        Bitmap frame = null;
        boolean frameCached = false;
        final String frameKey = "w" + $config.getWidgetId() + ":" + $index;
        // v1.8.11：loadFrame 以前在 try 外面 —— 帧文件损坏/解码 OOM 直接杀线程，
        // 默认处理器把整个进程带走（闪退），playbackBusy 还卡成永真。
        try {
            // v1.10.0：先查内存缓存（顺序循环播放基本不落盘）
            frame = this$0.previewCache().get(frameKey);
            if (frame != null && !frame.isRecycled()) {
                frameCached = true;
            } else {
                frame = WidgetStore.INSTANCE.loadFrame(this$0, $config, $index, 720, 720);
                if (frame != null) {
                    this$0.previewCache().put(frameKey, frame);
                    frameCached = true;
                }
            }
        } catch (Throwable th) {
            Diag.warn(this$0, "R02", "回放帧读取异常: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            CrashLog.save(this$0, Thread.currentThread(), th);
            this$0.playbackBusy.set(false);
            return;
        }
        if (frame == null) {
            this$0.playbackBusy.set(false);
            return;
        }
        try {
            FrameComposer frameComposer = FrameComposer.INSTANCE;
            int iPreviewWidth = this$0.previewWidth();
            int iPreviewHeight = this$0.previewHeight();
            LayoutMode layoutMode = this$0.selectedLayout;
            ShapeKind shapeKind = this$0.selectedShape;
            SeekBar seekBar = this$0.seekAlpha;
            if (seekBar == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
                seekBar = null;
            }
            int progress = seekBar.getProgress();
            CheckBox checkBox = this$0.cbBorder;
            if (checkBox == null) {
                Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
                checkBox = null;
            }
            boolean zIsChecked = checkBox.isChecked();
            SeekBar seekBar2 = this$0.seekScale;
            if (seekBar2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekScale");
                seekBar2 = null;
            }
            int progress2 = seekBar2.getProgress();
            int i = this$0.selectedRotation;
            int i2 = this$0.selectedShapeRotation;
            boolean z = this$0.selectedFitContent;
            SeekBar seekBar3 = this$0.seekShadow;
            if (seekBar3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
                seekBar3 = null;
            }
            bitmapCompose = frameComposer.compose(frame, iPreviewWidth, iPreviewHeight, layoutMode, shapeKind, progress, zIsChecked, progress2, i, i2, z, seekBar3.getProgress(), this$0.selectedShadowCurve, this$0.selectedOffsetX, this$0.selectedOffsetY, this$0.selectedContentScale, this$0.selectedMaskX, this$0.selectedMaskY);
            if (!frameCached && frame != null && !frame.isRecycled()) {
                frame.recycle();
            }
            this$0.playbackBusy.set(false);
        } catch (Throwable th) {
            bitmapCompose = null;
            if (!frameCached && frame != null && !frame.isRecycled()) {
                frame.recycle();
            }
            this$0.playbackBusy.set(false);
            // v1.8.11：合成异常以前只静默吞掉，现在留编号 + 崩溃日志
            Diag.warn(this$0, "R03", "回放帧合成异常: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            CrashLog.save(this$0, Thread.currentThread(), th);
        }
        Bitmap out = bitmapCompose;
        if (out == null) {
            return;
        }
        this$0.ui.post(() -> {
            renderPlaybackFrame$lambda$69$lambda$68($generation, this$0, out);
        });
    }

    private static final void renderPlaybackFrame$lambda$69$lambda$68(int $generation, ConfigActivity this$0, Bitmap $out) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.10.50：任务中迟到的合成结果不许上屏（否则导出时冻结画面会突然前进一帧）
        if (this$0.parseTaskActive || this$0.busy) {
            Diag.warnThrottled(this$0, "L31", "任务中：丢弃迟到的播放帧上屏");
            return;
        }
        // v1.8.15：播放中不被代次丢弃（同 renderPreviewFrameAt 上屏逻辑）
        if ($generation == this$0.previewGeneration.get() || this$0.playing) {
            ImageView imageView = this$0.imgPreview;
            if (imageView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("imgPreview");
                imageView = null;
            }
            imageView.setImageBitmap($out);
        }
    }

    private final void loadPreviewFrames(Uri uri, MediaKind kind) {
        Diag.warn(this, "L24", "beginParse 调用点=" + caller());
        int wantFps = 1 + progress(this.seekFps);
        int wantEdge = effectiveEdge();   // v1.10.59
        // v1.10.2：完全相同的解析请求（同一素材 + 同参数）且刚开始不久 → 忽略，
        // 避免「选择器回两次」时两条线程互相作废
        if (this.previewFramesLoading && uri != null && uri.equals(this.activeUri)
                && wantFps == this.activeFps && wantEdge == this.activeEdge
                && System.currentTimeMillis() - this.loadStartedAt < 2500L) {
            Diag.warn(this, "L23", "重复的解析请求，忽略 fps=" + wantFps + " edge=" + wantEdge);
            return;
        }
        this.activeUri = uri;
        if (this.previewFramesLoading) {
            // v1.9.6：新请求永远赢 —— 作废在途解析（旧线程的发布/进度全部代次失格），
            // 立即重开。原来这里是「跳过」：二次导入时新素材会被旧解析的 loading 闸门
            // 挡掉，表现就是「没有解析进度条」。
            Diag.warn(this, "L03", "预览帧: 作废在途解析并重开");
            this.extractGeneration++;
        }
        this.previewFramesLoading = true;
        this.loadStartedAt = System.currentTimeMillis();
        this.pendingFirstFrame = false;
        stopPlaybackLoop();
        // v1.9.3：解析 UI —— 预览区中央等待动画（任何标签页都看得到）+ 预览区下方解析条
        // v1.10.1：圆环与解析条统一由任务状态驱动（切布局/换肤后也能按状态恢复）
        this.parseTaskActive = true;
        this.taskExporting = false;
        this.lastLoggedPct = -1;
        this.parseProgress = 0;
        this.parseDone = 0;
        this.parseTotal = 0;
        this.lastProgressAt = 0L;
        ProgressBar parseBar = this.pbParse;
        if (parseBar != null) {
            parseBar.setIndeterminate(false);
            parseBar.setMax(1000);
            parseBar.setProgress(0, false);
        }
        syncTaskUi();
        Diag.warn(this, "L22", "任务UI: 解析开始 圆环=" + (this.pbPreview != null ? this.pbPreview.getVisibility() : -1)
                + " 解析条=" + (this.pbParse != null ? this.pbParse.getVisibility() : -1)
                + " 条值=" + (this.pbParse != null ? this.pbParse.getProgress() : -1)
                + " 文字=" + (this.tvPlayTime != null ? String.valueOf(this.tvPlayTime.getText()) : "-")
                + " 预览卡片=" + (findViewById(R.id.ll_preview_section) != null
                        ? findViewById(R.id.ll_preview_section).getVisibility() : -1));
        // v1.9.9：取消上一次收尾的「淡出」任务 + 恢复不透明度 ——
        // 否则上一次解析的淡出动画会把本次的解析条藏掉（概率性消失）
        if (this.parseHideTask != null) {
            parseBar.removeCallbacks(this.parseHideTask);
            this.parseHideTask = null;
        }
        parseBar.animate().cancel();
        parseBar.setAlpha(1.0f);
        // v1.9.7：每次开抽都重染一次（黑白切换中途/主题改动后颜色始终正确）
        applyProgressTheme();
        // v1.8.18：解析期间播放进度条与时间灰置（叠化动画）
        setPlaybackSeekActive(false);
        SeekBar seekBar = this.seekFps;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            seekBar = null;
        }
        int fps = effectiveFpsX10();
        int edge = effectiveEdge();   // v1.10.59：滑块 + 素材尺寸 + 格式阈值，同一处取值
        SeekBar seekBar3 = this.seekQuality;
        if (seekBar3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar3 = null;
        }
        int quality = 50 + seekBar3.getProgress();
        // v1.8.18：记录本次抽帧所用参数（「参数变了才重新解析」的对比基准）
        this.activeFps = fps;
        this.activeEdge = edge;
        this.activeQuality = quality;
        // L02：确认抽帧线程真的启动了（若诊断里连这行都没有，说明根本没走到这）
        Diag.warn(this, "L02", "预览帧: 开始解析 kind=" + kind + " fps=" + String.format(java.util.Locale.US, "%.1f", fps / 10.0f) + " 边长=" + edge);
        Diag.warn(this, "L29", "生效帧率=" + String.format(java.util.Locale.US, "%.1f", fps / 10.0f) + "fps（滑块=" + (1 + progress(seekBar)) + "）");
        // v1.8.17：代次编号 —— 换素材/清帧时 +1，旧线程的迟到发布一律作废
        this.extractGeneration++;
        final int gen = this.extractGeneration;
        final int parseGen = gen;
        new Thread(() -> {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            } catch (Throwable ignored) {
            }
            loadPreviewFrames$lambda$72(ConfigActivity.this, uri, kind, fps, edge, quality, parseGen);
        }).start();
        // v1.10.501：**解析期先上第 0 帧**（用户要求：导入后立刻能看到画面）。
        // 只解一帧（GIF 解到第 0 帧就停），与解析线程并行，几百毫秒内上屏；
        // 解析完成时 applyPreviewFrames 会用自己的第 0 帧覆盖它，所以不会有「两个第一帧」的冲突。
        scheduleFirstFrame(uri, parseGen);
        return;
    }

    /**
     * v1.10.501：解析期先上第 0 帧。
     *
     * <p>原来的路径只有「解析 593 帧 → 应用帧表 → 渲染第 0 帧」，真机上要等 19 秒
     * （`L22 解析开始` → `L05 首帧上屏`）。这里改成：解析线程照跑，同时另开一条线程
     * 只解素材的第 0 帧 → 按当前画布/参数合成 → 上屏。</p>
     *
     * <p>两道闸：① 代次变了（换素材/重开解析）就丢弃；② 已经有帧表了（解析已完成）也丢弃，
     * 让正式的第 0 帧上屏。上屏不写任何缓存 —— 它只是个「占位画面」，不该污染合成缓存。</p>
     */
    private void scheduleFirstFrame(@Nullable final Uri uri, final int gen) {
        if (uri == null) {
            return;
        }
        try {
            this.ui.post(() -> new Thread(() -> {
                Bitmap source = null;
                try {
                    source = MediaImporter.firstFrame(ConfigActivity.this, uri, effectiveEdge());
                } catch (Throwable ignored) {
                }
                if (source == null) {
                    return;
                }
                Bitmap composed = null;
                try {
                    int[] canvas = previewCanvas();
                    composed = composePreview(source, canvas[0], canvas[1]);
                } catch (Throwable ignored) {
                } finally {
                    if (!source.isRecycled()) {
                        source.recycle();
                    }
                }
                if (composed == null) {
                    return;
                }
                final Bitmap out = composed;
                this.ui.post(() -> {
                    try {
                        if (gen != ConfigActivity.this.extractGeneration
                                || ConfigActivity.this.previewFrames != null
                                || ConfigActivity.this.isFinishing()) {
                            if (!out.isRecycled()) {
                                out.recycle();
                            }
                            return;
                        }
                        publishPreviewFrame(ConfigActivity.this.previewGeneration.incrementAndGet(), out, 0);
                    } catch (Throwable ignored) {
                    }
                });
            }, "first-frame").start());
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.59：质量滑块下方那行小字 —— 把「这次到底会导出成什么」直接写出来。
     * 例：`300 px（原图直出）· 20 帧 · 0:00.60 · 无损 WebP（无色差）`
     */
    final void updateParamsLine() {
        TextView line = this.tvParams;
        if (line == null) {
            return;
        }
        try {
            int edge = effectiveEdge();
            int quality = 50 + progress(this.seekQuality);
            String size = isOriginalEdge(edge)
                    ? getString(R.string.cfg_param_size_original, new Object[]{Integer.valueOf(edge)})
                    : getString(R.string.cfg_param_size, new Object[]{Integer.valueOf(edge)});
            // 帧数/时长：能从素材探测拿到就用真实值，拿不到就说「按帧率导出」
            int frames = this.sourceFrames > 0 ? this.sourceFrames : 0;
            long durMs = this.sourceDurationMs;
            String framesText;
            if (frames > 0 && durMs > 0L) {
                framesText = getString(R.string.cfg_param_frames,
                        new Object[]{Integer.valueOf(frames), formatTime(durMs)});
            } else if (frames > 0) {
                framesText = getString(R.string.cfg_param_frames,
                        new Object[]{Integer.valueOf(frames), "-"});
            } else {
                framesText = "-";
            }
            String encoded = quality >= 100
                    ? getString(R.string.cfg_param_lossless)
                    : getString(R.string.cfg_param_lossy, new Object[]{Integer.valueOf(Math.min(quality, 95))});
            line.setText(getString(R.string.cfg_params_line, new Object[]{size, framesText, encoded}));
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.59：当前素材类别的「原图直出」天花板 —— 图片 2.5K(2560)，动图/视频 1080p(1920)。 */
    final int edgeCap() {
        MediaKind kind = this.pickedKind != null ? this.pickedKind
                : (this.base != null ? this.base.getKind() : MediaKind.IMAGE);
        return kind == MediaKind.IMAGE ? 2560 : 1920;
    }

    /** 素材最长边（未知返回 0）。 */
    final int sourceEdge() {
        if (this.sourceWidth <= 0 || this.sourceHeight <= 0) {
            return 0;
        }
        return Math.max(this.sourceWidth, this.sourceHeight);
    }

    /**
     * v1.10.59：**实际会用的边长**（清晰度滑块 → 真正导出/预览用的像素）。
     *
     * 三条底线：
     * 1) 素材在阈值内（图片 ≤2.5K / 动图·视频 ≤1080p）+ 滑块到顶 → **原图直出**，一个像素都不缩；
     * 2) 永不放大（edge 不超过素材最长边）；
     * 3) 超过阈值的素材，滑块可以任调，但不会越过该格式的天花板。
     */
    final int effectiveEdge() {
        int wanted = 120 + progress(this.seekEdge) * 8;
        int cap = edgeCap();
        int src = sourceEdge();
        int edge;
        if (src > 0) {
            edge = wanted >= src ? Math.min(src, cap) : Math.min(wanted, src);
        } else {
            edge = Math.min(wanted, cap);
        }
        return RangesKt.coerceIn(edge, 120, cap);
    }

    /** 当前边长是不是「素材原尺寸直出」。 */
    final boolean isOriginalEdge(int edge) {
        int src = sourceEdge();
        return src > 0 && edge >= src;
    }

    /** v1.10.8：本次生效的帧率 ×10 —— 素材帧率优先（33.3fps 就是 333），滑块改过才用滑块值。 */
    final int effectiveFpsX10() {
        int slider = 1 + progress(this.seekFps);
        int sourceX10 = this.sourceFpsX10;
        if (sourceX10 > 0 && Math.max(Math.round(sourceX10 / 10.0f), 1) == slider) {
            return sourceX10;
        }
        return Math.max(slider, 1) * 10;
    }

    /**
     * v1.10.128：60Hz 屏上「不会有节奏抖动」的帧率。
     * 判定：60 能被 fps 整除（2:2 / 3:3 完美）→ 返回 0，不用提示；
     *       2×fps 能被 60 整除（24 / 40fps 的 3:2 拉片，电影通行做法）→ 也返回 0；
     *       其余（33.3 / 25 / 45 …）→ 返回最接近、且差距 ≤22% 的「整分频」帧率，否则 0。
     */
    static int friendlyFps(int fps) {
        if (fps <= 1) {
            return 0;
        }
        if (60 % fps == 0) {
            return 0;
        }
        if (120 % fps == 0) {
            // 120/fps 为整数 = 每 2 帧正好铺满整数个 vsync 周期：
            // 24fps(5:2)、40fps(3:2) 这类 3:2 拉片是电影通行做法，不算抖动
            return 0;
        }
        int[] candidates = {60, 30, 20, 15, 12, 10, 6, 5, 4, 3, 2};
        int best = 0;
        float bestDiff = 1.0f;
        for (int c : candidates) {
            if (c > fps) {
                continue;
            }
            float d = (fps - c) / (float) fps;
            if (d < bestDiff) {
                bestDiff = d;
                best = c;
            }
        }
        return (best > 0 && bestDiff <= 0.22f) ? best : 0;
    }

    /** v1.10.4：这一轮抽帧是否已作废（换素材/离开应用）。 */
    final boolean isParseCancelled(int gen) {
        return gen != this.extractGeneration;
    }

    private static final void loadPreviewFrames$lambda$72(ConfigActivity this$0, Uri $uri, MediaKind $kind, int $fps, int $edge, int $quality, int $gen) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($uri, "$uri");
        Intrinsics.checkNotNullParameter($kind, "$kind");
        PreviewFrames.Result result;
        try {
            result = PreviewFrames.INSTANCE.load(this$0, $uri, $kind, $fps, $edge, $quality, 0,
                    new ConfigActivity$loadPreviewFrames$1$result$1(this$0, $gen),
                    new PreviewFrames.Cancel() {
                        @Override
                        public boolean isCancelled() {
                            return this$0.isParseCancelled($gen);
                        }
                    });
        } catch (Throwable th) {
            CrashLog.save(this$0, Thread.currentThread(), th);
            Diag.warn(this$0, "L01", "预览帧解析异常: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? ("/" + th.getMessage()) : ""));
            final String reason = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            final int genForFailure = $gen;
            this$0.ui.post(new Runnable() {
                @Override
                public void run() {
                    if (this$0.extractGeneration != genForFailure) {
                        return;
                    }
                    this$0.previewFramesLoading = false;
                    this$0.parseTaskActive = false;
                    this$0.syncTaskUi();
                    String failed = this$0.getString(R.string.status_failed, new Object[]{reason});
                    Intrinsics.checkNotNullExpressionValue(failed, "getString(...)");
                    this$0.toast(failed);
                }
            });
            return;
        }
        PreviewFrames.Result result2 = this$0.previewFrames;
        List old = result2 != null ? result2.getFrames() : null;
        this$0.ui.post(() -> {
            loadPreviewFrames$lambda$72$lambda$71(this$0, result, old, $gen, $uri, $fps, $edge);
        });
    }

    private static final void loadPreviewFrames$lambda$72$lambda$71(ConfigActivity this$0, PreviewFrames.Result $result, List $old, int $gen, Uri $uri, int $fps, int $edge) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($result, "$result");
        if ($gen != this$0.extractGeneration) {
            // v1.10.3：迟到结果如果「目标就是当前素材 + 当前参数」，说明在途任务已经没了，
            // 这份数据依然是对的 —— 直接接手（否则会永久停在首帧）。
            boolean sameTarget = $uri != null && $uri.equals(this$0.activeUri)
                    && $fps == this$0.activeFps && $edge == this$0.activeEdge;
            if (!sameTarget || !DeskWatcher.isAppForeground()) {
                Diag.warn(this$0, "L12", "迟到结果作废 gen=" + $gen + " 当前=" + this$0.extractGeneration
                        + " 前台=" + DeskWatcher.isAppForeground());
                return;
            }
            Diag.warn(this$0, "L25", "迟到结果目标一致，接手上屏 gen=" + $gen + " 当前=" + this$0.extractGeneration);
            this$0.extractGeneration = $gen;
        }
        this$0.previewFramesLoading = false;
        // v1.8.10：同上 —— 旧帧可能还在渲染线程手里，recycle 会直接抛
        // “Canvas: trying to use a recycled bitmap”。丢掉引用即可。
        int frameTotal = $result != null && $result.getFrames() != null ? $result.getFrames().size() : 0;
        // v1.10.26（需求 5）：画质链路证据 —— 源 / 预览帧 / 画布三档尺寸一次打全。
        // 「预览帧 ≤ 画布」= 上屏时被放大 = 肉眼发虚；反过来才是正常（缩小）。
        try {
            if (frameTotal > 0) {
                BitmapFactory.Options probe = new BitmapFactory.Options();
                probe.inJustDecodeBounds = true;
                BitmapFactory.decodeFile($result.getFrames().get(0), probe);
                // v1.10.64：这里以前报的是「预览框尺寸」而不是「实际合成画布」，
                // 于是小素材（280px）永远显示一句吓人的「预览帧比画布小，上屏被放大」——
                // 其实从 v1.10.63 起画布已按素材封顶（280 → 308），放大只发生在 ImageView 上屏那一步，
                // 是「素材本来就小」而不是链路问题。
                int[] canvas = this$0.previewCanvas();
                int canvasW = canvas[0];
                int canvasH = canvas[1];
                Diag.warn(this$0, "F11", "画质链路 源=" + this$0.sourceWidth + "x" + this$0.sourceHeight
                        + " 预览帧=" + probe.outWidth + "x" + probe.outHeight
                        + " 合成画布=" + canvasW + "x" + canvasH
                        + " 上屏=" + this$0.previewWidth() + "x" + this$0.previewHeight()
                        + (probe.outWidth <= 0
                        ? ""
                        : canvasW < probe.outWidth
                        ? " (画布≤素材，1:1 或缩小合成)"
                        : canvasW > probe.outWidth
                        ? " (画布大于素材：合成时会把素材放大)"
                        : " (画布=素材，1:1 合成)"));
            }
        } catch (Throwable ignored) {
        }

        this$0.parseTaskActive = false;
        if (this$0.busy) {
            // v1.10.19：导出进行中，解析条归导出管。
            // v1.10.52 关键：原来这里先写了 parseProgress = 1000 再判断 busy ——
            // 导出刚点下去、旧解析的迟到结果正好回来（小素材/动图解析极快，必然撞上），
            // 这一行会把**导出条**一口气写到 100%：用户看到的就是「点导出后解析条瞬间冲满」。
            // 现在 busy 分支里绝不碰 parseProgress。
            this$0.syncTaskUi();
            this$0.restorePickLine();
            this$0.publishPartialFrames($result.getFrames(), $result.getFpsX10(), $gen);
            return;
        }
        this$0.parseProgress = 1000;
        // v1.9.8：解析完成 —— 进度条先跑满再淡出（解析再快也能看到完整的收尾）
        // v1.10.25：统一走 finishTaskBar（与导出收尾同一套，不再两份实现）
        if (frameTotal > 0) {
            this$0.finishTaskBar();
        } else {
            ProgressBar parseBar = this$0.pbParse;
            if (parseBar != null) {
                parseBar.setVisibility(View.GONE);
            }
        }
        // v1.9.8：探测没给参数时，从解析结果兜底填充基本信息（保证一定有值）
        if (!this$0.infoFilled && frameTotal > 0) {
            String firstPath = $result.getFrames().get(0);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(firstPath, opts);
            String resFallback = opts.outWidth > 0 ? opts.outWidth + "x" + opts.outHeight : null;
            TextView tvResF = this$0.tvResolution;
            if (tvResF != null) {
                tvResF.setText(this$0.buildSourceInfo(resFallback,
                        Integer.valueOf($result.getFps()), Long.valueOf($result.getDurationMs())));
            }
        }
        // v1.8.15：最终结果也走 publishPartialFrames —— 播放中只扩展帧表，不打断
        this$0.syncTaskUi();
        this$0.restorePickLine();
        // v1.10.9：必须传 ×10 的精确帧率 —— 传 getFps()（33）会被当成 3.3fps 播放（慢 10 倍）
        this$0.publishPartialFrames($result.getFrames(), $result.getFpsX10(), $gen);
        // v1.8.7：解析结果为空时明确提示，避免“看着像没解析”
        if (frameTotal <= 0) {
            String emptyFrames = this$0.getString(R.string.status_failed, new Object[]{"预览帧 0 张"});
            Intrinsics.checkNotNullExpressionValue(emptyFrames, "getString(...)");
            this$0.toast(emptyFrames);
        } else {
            // v1.10.18：导出进行中不许恢复 —— 否则导出刚开始/解析刚好完成时，
            // 播放进度条会被这一行重新点亮（用户反复反馈的「导出中没变灰」）
            this$0.setPlaybackSeekActive(frameTotal > 1 && !this$0.busy);
        }
    }

    /**
     * v1.9.3：解析等待动画与解析条纳入主系统配色（黑白模式切换时也要重染）。
     */
    private final void applyProgressTheme() {
        try {
            ThemeColors c = ThemeManager.INSTANCE.colors(this);
            ProgressBar spinner = this.pbPreview;
            if (spinner instanceof SpinRingView) {
                ((SpinRingView) spinner).setColors(c.getPrimary(), c.getNeutral());
            } else if (spinner != null) {
                spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(c.getPrimary()));
            }
            ProgressBar bar = this.pbParse;
            if (bar != null) {
                bar.setProgressTintList(android.content.res.ColorStateList.valueOf(c.getPrimary()));
                bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(c.getNeutral()));
            }
            // v1.10.25：任务百分比文字用主色（缓存，避免每 100ms 再算一次主题）
            this.taskTextColor = c.getPrimary();
            // v1.10.26（需求 3）：播放进度条永久 disabled（不可拖动），
            // 不显式给 tint 会用系统 disabled 色，正常播放时也整条发灰
            SeekBar playbackSeek = this.seekPlayback;
            if (playbackSeek != null) {
                try {
                    playbackSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(c.getPrimary()));
                    playbackSeek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(c.getNeutral()));
                    playbackSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(c.getNeutral()));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.8.18：解析中把播放进度条+时间文字灰置（叠化动画），解析完恢复色彩。
     */
    private final void setPlaybackSeekActive(boolean active) {
        // v1.10.50：任务进行中无论谁传 true 都不许点亮（防「一瞬间脱锁」）
        if (this.parseTaskActive || this.busy) {
            active = false;
        }
        SeekBar seekBar = this.seekPlayback;
        if (seekBar != null) {
            // v1.10.16：立即生效（原来 220ms 渐变，导出刚开始时看着「没变灰」）
            seekBar.animate().cancel();
            // v1.10.26：永久不可拖动（setEnabled(false) 在 onCreate 一次性设好），
            // 这里只改透明度：false = 强制灰（任务中/静态图片），true = 按真实可用性判断
            seekBar.setAlpha(active ? playbackSeekAlpha() : 0.35f);
        }
        TextView textView = this.tvPlayTime;
        if (textView != null) {
            textView.animate().cancel();
            // v1.10.25：任务中这一行是「百分比指示器」，不参与灰置（否则百分比看不清）
            boolean task = this.parseTaskActive || this.busy;
            textView.setAlpha((active || task) ? 1.0f : 0.35f);
        }
    }

    /**
     * v1.10.26：播放进度条能不能「亮着」的唯一判据。
     * 静态图片 / 任务进行中 / 帧数 ≤1 都算不可用（灰置）。
     */
    private final float playbackSeekAlpha() {
        if (this.parseTaskActive || this.busy) {
            return 0.35f;
        }
        MediaKind kind = this.pickedKind;
        if (kind == null && this.base != null) {
            kind = this.base.getKind();
        }
        if (kind == MediaKind.IMAGE) {
            return 0.35f;
        }
        PreviewFrames.Result frames = this.previewFrames;
        if (frames == null || frames.getFrames() == null || frames.getFrames().size() <= 1) {
            return 0.35f;
        }
        return 1.0f;
    }

    /**
     * v1.8.15：边解析边预览 —— 发布（部分）帧。
     * 播放中只扩展帧列表不打断；未播放则从 0 启动。
     * gen：抽帧代次，不符（换素材后的迟到发布）直接作废。
     */
    final void publishPartialFrames(List<String> frames, int fpsX10, int gen) {
        if (gen != this.extractGeneration) {
            Diag.warn(this, "L12", "预览帧: 迟到发布作废 gen=" + gen + " 当前=" + this.extractGeneration);
            return;
        }
        if (frames == null || frames.isEmpty()) {
            return;
        }
        // L04：帧已到手、准备上屏。有 L02 没有 L04 = 解析线程出问题。
        Diag.warn(this, "L04", "预览帧: 应用 " + frames.size() + " 张 fps="
                + String.format(java.util.Locale.US, "%.1f", fpsX10 / 10.0f));
        int safeFps = RangesKt.coerceAtLeast(fpsX10, 10);
        this.previewFrames = new PreviewFrames.Result(frames, safeFps, (long) frames.size() * 10000L / (long) safeFps);
        if (this.playing) {
            SeekBar seekBar = this.seekPlayback;
            if (seekBar != null) {
                // v1.10.51：只许把 max 变大 —— 变小会把 progress 夹到 max，看起来就是「滑块贴到尾部」
                int newMax = RangesKt.coerceAtLeast(frames.size() - 1, 0);
                if (newMax > seekBar.getMax()) {
                    seekBar.setMax(newMax);
                }
            }
            return;
        }
        if (this.busy) {
            // v1.10.17：导出中画面是冻结的 —— 只登记帧表，不重置进度/不换帧
            Diag.warn(this, "L31", "导出中：帧表已更新，不上屏（保持冻结）");
            return;
        }
        this.applyPreviewFrames();
    }

    private final void applyPreviewFrames() {
        PreviewFrames.Result result = this.previewFrames;
        if (result == null || result.getFrames().isEmpty()) {
            return;
        }
        this.previewFirstLogged = false;
        SeekBar seekBar = this.seekPlayback;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
            seekBar = null;
        }
        seekBar.setMax(RangesKt.coerceAtLeast(result.getFrames().size() - 1, 0));
        SeekBar seekBar2 = this.seekPlayback;
        if (seekBar2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekPlayback");
            seekBar2 = null;
        }
        seekBar2.setEnabled(result.getFrames().size() > 1);
        this.playIndex = 0;
        // v1.10.51：这里必须**无条件**把条子归零。
        // 上面 setMax(frames-1) 只要比当前进度小，AbsSeekBar 就会把 progress 夹到 max ——
        // 滑块瞬间贴到最右端；而 updatePlayTime 在任务中有门闸，不一定写回 0，
        // 于是「一瞬间跳到尾部」就留在屏上了。这是跳尾部的直接原因。
        this.seekPlayback.setProgress(0);
        updatePlayTime(0);
        updatePreviewBox();
        renderPreviewFrameAt(0);
        if (result.getFrames().size() > 1) {
            startPreviewFramesPlayback();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void startPreviewFramesPlayback() {
        PreviewFrames.Result result = this.previewFrames;
        if (result == null || result.getFrames().size() <= 1) {
            return;
        }
        if (!DeskWatcher.isAppForeground()) {
            // v1.10.4：不在前台就别开播放（否则会白烧 CPU）
            Diag.warn(this, "L27", "不在前台，延后播放");
            return;
        }
        if (this.busy) {
            // v1.10.11：导出期间画面必须冻结 —— 任何入口都不许把预览重新播起来
            Diag.warn(this, "L30", "导出中，忽略预览播放请求");
            return;
        }
        stopPlaybackLoop();
        this.playing = true;
        // v1.10.76：拖动帧率滑块时立刻按**新帧率**跑（帧表还是旧的，松手后重抽对齐）
        final int fpsX10 = this.paramsDragging
                ? RangesKt.coerceIn(effectiveFpsX10(), 10, 500)
                : RangesKt.coerceIn(result.getFpsX10(), 10, 500);
        final int fps = Math.max(Math.round(fpsX10 / 10.0f), 1);
        // 第 j 帧的时间点 = j * 10000 / fpsX10 毫秒（整型毫秒，误差 <1ms）
        long iv = Math.max(10000L / fpsX10, 4L);
        // v1.10.296（用户："**仅导入素材后的那一次预览**画面明显加快，桌面组件和再次进编辑页正常"）：
        // 病根是上面这个 interval 只信 fpsX10 —— 刚导入时帧表是"估计"出来的，
        // fps 可能还是默认/估计值（偏大），于是第一遍预览的**总时长**比素材短 → 看起来在快进；
        // 再次进编辑页会重新解析，拿到真实时长后就正常了。
        // 现在**以素材总时长为准**反推间隔：总时长 = 帧数 × 间隔，和 fps 无关，
        // 这样无论 fps 估计得多离谱，预览的播放速度与时长都不会错（用户硬性要求：误差到小数点后 2 位）。
        try {
            // v1.10.303（用户："四秒素材画面只跑了三秒"）：**必须用素材真实总时长**。
            // result.getDurationMs() 是「已抽帧数 × 间隔」反推出来的（94×33=3133ms），
            // 它把"丢帧"藏进了时长里，自己校验自己永远打勾。
            // sourceDurationMs 才是选中素材时读到的真实时长（界面那行 0:04.66）。
            long srcMs = this.sourceDurationMs > 0L ? this.sourceDurationMs : result.getDurationMs();
            int n = result.getFrames().size();
            if (srcMs > 0L && n > 1) {
                // 不新增 L07 文本（门禁 [8] 要求一个代号一种含义）——现有的
                // 「L07 预览播放启动: … 间隔=?ms」本来就会打出最终采用的这个值。
                iv = Math.max(srcMs / n, 1L);
            }
        } catch (Throwable ignored) {
        }
        final long interval = iv;
        final long startAt = SystemClock.elapsedRealtime();
        this.fpsWindowStart = startAt;
        this.fpsWindowFrames = 0;
        this.fpsWindowDropped = 0;
        // v1.10.306（L35 差 -46/-57ms ✗）：140 帧 × 33ms=4620、172 帧 × 33ms=5676，
        // 素材是 4666/5733 —— **整数毫秒的间隔必然丢零头**（33.3 被截成 33）。
        // 改成按素材总时长归一：index = elapsed × 帧数 ÷ 总时长，总时长分毫不差、不漂移。
        final long spanMs = this.sourceDurationMs > 0L ? this.sourceDurationMs : 0L;
        Diag.warn(this, "L07", "预览播放启动: " + result.getFrames().size() + " 张, fps=" + fps + ", 间隔=" + interval + "ms");
        // v1.10.59：预览时长自检（帧数 × 间隔 必须等于素材时长）
        try {
            // v1.10.307：L35 是**自检行**，原来按"帧数×整数间隔"算（33.3 被截成 33 → 恒差 -46ms ✗）。
            // 实际播放已按总时长归一（v1.10.306），这里也必须用同一个口径，否则自检永远打叉。
            long previewMs = spanMs > 0L ? spanMs : (long) result.getFrames().size() * interval;
            long srcMs = result.getDurationMs();
            Diag.warn(this, "L35", "预览时长自检 " + result.getFrames().size() + " 张 × " + interval
                    + "ms = " + previewMs + "ms，素材=" + srcMs + "ms（差 " + (previewMs - srcMs) + "ms）"
                    + (srcMs <= 0L || Math.abs(previewMs - srcMs) <= 40 ? " ✓" : " ✗"));
        } catch (Throwable ignored) {
        }
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!ConfigActivity.this.playing) {
                    return;
                }
                // v1.10.50：每一拍都验任务状态
                if (ConfigActivity.this.parseTaskActive || ConfigActivity.this.busy) {
                    ConfigActivity.this.stopPlaybackLoop();
                    return;
                }
                PreviewFrames.Result current = ConfigActivity.this.previewFrames;
                if (current == null) {
                    return;
                }
                int total = current.getFrames().size();
                if (total <= 1) {
                    return;
                }
                long now = SystemClock.elapsedRealtime();
                long elapsed = Math.max(now - startAt, 0L);
                long step = spanMs > 0L ? (elapsed * total) / spanMs : elapsed / interval;
                int index = (int) (step % total);
                ConfigActivity.this.renderPreviewFrameAt(index);
                // v1.10.50：同一个写入方（任务中会被它自己拒掉）
                ConfigActivity.this.publishPlaybackPosition(index,
                        ConfigActivity.this.formatTime((index * 1000L) / Math.max(fps, 1)));
                // v1.10.1（需求 2）：实测帧率汇总，3 秒一条（不逐帧写日志）
                if (now - ConfigActivity.this.fpsWindowStart >= 3000L) {
                    long span = Math.max(now - ConfigActivity.this.fpsWindowStart, 1L);
                    float measured = ConfigActivity.this.fpsWindowFrames * 1000.0f / span;
                    Diag.warn(ConfigActivity.this, "P20", "预览实测 "
                            + String.format(java.util.Locale.US, "%.1f", measured) + "fps（目标 " + fps
                            + "）/" + ConfigActivity.this.fpsWindowFrames + " 帧/" + span + "ms"
                            + (ConfigActivity.this.fpsWindowDropped > 0
                                    ? "，合成跟不上丢 " + ConfigActivity.this.fpsWindowDropped + " 帧" : ""));
                    ConfigActivity.this.fpsWindowStart = now;
                    ConfigActivity.this.fpsWindowFrames = 0;
                    ConfigActivity.this.fpsWindowDropped = 0;
                }
                // v1.10.0：按墙钟对齐下一帧（不累加误差），掉帧也不会让整段变慢
                // v1.10.308（P20 实测 34.6fps > 目标 30）：下标仍按总时长归一（精确），
                // 但**调度不能抢跑** —— 原来一旦落后就 max(delay,1) 连发，启动那一秒会多刷约 5 帧。
                // 现在落后时直接对齐到"现在之后的第一格"，每次 tick 之间至少隔一格。
                long span = spanMs > 0L ? spanMs : interval * Math.max(total, 1);
                long nextAt = startAt + ((step + 1) * span) / Math.max(total, 1);
                long nowTick = SystemClock.elapsedRealtime();
                if (nextAt <= nowTick) {
                    long behind = (nowTick - startAt) * Math.max(total, 1) / Math.max(span, 1) + 1;
                    nextAt = startAt + (behind * span) / Math.max(total, 1);
                }
                ConfigActivity.this.ui.postDelayed(this, Math.max(nextAt - SystemClock.elapsedRealtime(), 1L));
            }
        };
        this.playbackRun = tick;
        this.ui.post(tick);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void renderPreviewFrameAt(int index) {
        PreviewFrames.Result result = this.previewFrames;
        if (result == null || result.getFrames().isEmpty()) {
            return;
        }
        List<String> list = result.getFrames();
        int size = list.size();
        int safe = ((index % size) + size) % size;
        renderFramePath(list.get(safe), safe);
    }

    /**
     * v1.10.63：预览合成画布也按「素材原尺寸 × 1.1」封顶。
     *
     * 280×280 的 GIF 在 907×907 的预览框里本来就是放大 3.2 倍看的，
     * 按 907 合成不会多出任何细节，只会让每帧合成贵 9 倍 —— 首圈必丢帧
     * （日志里 `P12 预览合成跟不上` 都发生在 idx=0/4，就是缓存还没热的时候）。
     * 封顶后首圈也能稳在 33fps，画质完全一样（ImageView 反正要放大到 907）。
     */
    @NotNull
    private final int[] previewCanvas() {
        int w = previewWidth();
        int h = previewHeight();
        int src = sourceEdge();
        if (src > 0) {
            // v1.10.255：**不再放大素材**。原来留 10% 余量（src*1.1），于是 850×850 的 GIF
            // 会被合成到 907×907（预览框尺寸）—— 多出来的 6.7% 全是插值，纯亏；
            // 而且 F11 那条日志会写成「画布按素材封顶」，与事实（画布>素材）相反。
            float limit = src;
            float longest = Math.max(w, h);
            if (longest > limit && longest > 0f) {
                float k = limit / longest;
                w = Math.max(96, Math.round(w * k));
                h = Math.max(96, Math.round(h * k));
            }
        }
        return new int[]{w, h};
    }

    /** v1.10.59：外观参数签名 —— 任何一项变了，合成缓存立即作废（绝不拿旧外观上屏）。 */
    private final String paramsKey() {
        StringBuilder b = new StringBuilder(64);
        b.append(this.selectedLayout).append(',').append(this.selectedShape).append(',')
                .append(progress(this.seekAlpha)).append(',').append(isChecked(this.cbBorder) ? 1 : 0).append(',')
                .append(progress(this.seekScale)).append(',').append(this.selectedRotation).append(',')
                .append(this.selectedShapeRotation).append(',').append(this.selectedFitContent ? 1 : 0).append(',')
                // v1.10.222（根因）：曲线**必须**进 key。221 只加了 shadowPercent，
                // 于是「只改曲线不改强度」时这里算出来的 key 一模一样 → 命中旧合成帧 →
                // 画面纹丝不动；等用户再碰一下主滑块（强度变了 → key 变了）才第一次用新曲线合成，
                // 画面于是"跳"一下 —— 用户看到的两个 bug 是同一个根因。
                .append(progress(this.seekShadow)).append(',').append(this.selectedShadowCurve).append(',')
                .append(this.selectedOffsetX).append(',')
                .append(this.selectedOffsetY).append(',').append(this.selectedContentScale).append(',')
                .append(this.selectedMaskX).append(',').append(this.selectedMaskY).append(',')
                .append(this.selectedShape);
        return b.toString();
    }

    @NotNull
    final FrameCache composedCache() {
        FrameCache cache = this.previewComposedCache;
        if (cache == null) {
            int heapKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
            cache = new FrameCache(Math.max(heapKb / 4, 4096));
            this.previewComposedCache = cache;
        }
        return cache;
    }

    /** v1.10.0：解码走内存缓存 + 合成在渲染线程，主线程绝不做磁盘 I/O。 */
    final void renderFramePath(@NotNull final String path, final int index) {
        final int[] canvas = previewCanvas();
        final int width = canvas[0];
        final int height = canvas[1];
        // v1.10.56：缓存键带「解码目标尺寸」—— 预览框大小变了就是另一份缓存，
        // 否则换布局（预览框比例变化）后会拿旧尺寸的图去合成。
        final String cacheKey = width + "x" + height + "|" + path;
        // v1.10.59：**合成结果缓存**。
        // 30fps 一拍只有 33ms，而 907×907 合成一次要 25~40ms —— 必丢帧、看起来就是「一卡一卡/速度不对」。
        // 命中合成缓存后上屏是 0 成本的，动图（帧数不多）第一圈之后完全丝滑。
        final String composedKey = width + "x" + height + "|" + paramsKey() + "|" + path;
        try {
            Bitmap hit = composedCache().get(composedKey);
            if (hit != null && !hit.isRecycled()) {
                // 命中即上屏（publishPreviewFrame 自己会记 fpsWindowFrames，这里不要再加）
                publishPreviewFrame(this.previewGeneration.incrementAndGet(), hit, index);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (!this.previewRenderBusy.compareAndSet(false, true)) {
            // 上一帧还在合成：这一帧直接放弃。时钟是按墙钟走的，下一 tick 会取「当前该显示的那帧」，
            // 所以放弃一帧只会让画面稍微跳一下，不会变成慢动作（v1.9.x 的自节流会让整段变慢）。
            if (this.playing) {
                this.fpsWindowDropped++;
                Diag.warnThrottled(this, "P12", "预览合成跟不上，丢一帧 idx=" + index);
            } else {
                // 非播放态（拖滑块）：标记一下，当前这帧画完再补渲一次
                this.previewDirty = true;
            }
            return;
        }
        final int generation = this.previewGeneration.incrementAndGet();
        this.previewExecutor.execute(() -> {
            Bitmap out = null;
            try {
                Bitmap source = null;
                try {
                    source = previewCache().get(cacheKey);
                    if (source != null && source.isRecycled()) {
                        source = null;
                    }
                    if (source == null) {
                        // v1.10.56：直接解到预览框尺寸（省 2~4 倍像素 → 每帧省 3~8ms，30fps 不再丢帧）
                        source = PreviewFrames.decodeFrame(path, width, height);
                        if (source != null) {
                            previewCache().put(cacheKey, source);
                        }
                    }
                } catch (Throwable th) {
                    source = null;
                }
                if (source == null) {
                    return;
                }
                out = composePreview(source, width, height);
            } catch (Throwable th) {
                Diag.warn(this, "R01", "预览合成异常: " + th.getClass().getSimpleName()
                        + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            } finally {
                this.previewRenderBusy.set(false);
            }
            final Bitmap composed = out;
            if (composed != null) {
                try {
                    composedCache().put(composedKey, composed);
                } catch (Throwable ignored) {
                }
            }
            if (composed != null) {
                this.ui.post(() -> {
                    publishPreviewFrame(generation, composed, index);
                    if (this.previewDirty && !this.playing) {
                        this.previewDirty = false;
                        renderPreviewFrameAt(this.playIndex);
                    }
                });
            }
            // v1.10.499：预取挪到独立线程（见 previewPrefetch 的注释）——渲染线程不再被它顶住
            try {
                previewPrefetch.execute(() -> prefetchPreview(index));
            } catch (Throwable ignored) {
            }
        });
    }

    /** 任务结束后把播放时间那行恢复成时间（进度文字撤掉）。 */
    final void restorePlayTime() {
        if (this.tvPlayTime != null) {
            updatePlayTime(this.playIndex);
        }
    }

    /** 任务结束后把素材行恢复成「已选择：xxx」。 */
    final void restorePickLine() {
        TextView pick = this.tvPick;
        if (pick == null) {
            return;
        }
        String name = this.pickedName != null ? this.pickedName
                : (this.base != null ? this.base.getSourceName() : "");
        pick.setText(getString(R.string.cfg_picked, new Object[]{name != null ? name : ""}));
    }

    /** 语言原地切换后：重刷代码里设置过的文字（不改结构、不动状态）。 */
    @Override
    protected void onTextsRefreshed() {
        try {
            updateLabels();
            updateRotateButtons();
            refreshFitButton();
            updatePlayTime(this.playIndex);
            updateEmptyHint();
            buildShapeGrid();
            if (this.mediaSelector != null) {
                ThemeColors c = ThemeManager.INSTANCE.colors(this);
                this.mediaSelector.applyColors(c.getPrimary(), c.getNeutral(), c.getText(), c.getOnPrimary());
                this.mediaSelector.setLabels(new String[]{getString(R.string.cfg_video),
                        getString(R.string.cfg_gif), getString(R.string.cfg_image)});
            }
            if (this.tvPick != null) {
                String sourceName = this.pickedName != null ? this.pickedName
                        : (this.base != null ? this.base.getSourceName() : "");
                this.tvPick.setText(getString(R.string.cfg_picked, new Object[]{sourceName}));
            }
            if (this.tvResolution != null && this.infoFilled) {
                this.tvResolution.setText(buildSourceInfoFull(null, this.sourceFpsX10, this.sourceFrames,
                        this.sourceDurationMs, this.sourceFpsEstimated));
            }
            // v1.10.496：换语言要整张卡重算 —— 结论行是代码里 setText 的，
            // 影子布局只能把「正在估算…」抄回来，不重算就会停在一句占位文案上。
            refreshHeadroom();
        } catch (Throwable ignored) {
        }
    }

    /** 预览合成（渲染线程）：参数与导出/桌面完全同一套 FrameComposer。 */
    @Nullable
    final Bitmap composePreview(@NotNull Bitmap source, int width, int height) {
        // v1.10.0：F10 比例自检 —— 素材比例 vs 合成画布比例，偏差 >2% 才记一行（换尺寸/换素材时重记）
        try {
            float srcRatio = source.getWidth() / (float) Math.max(source.getHeight(), 1);
            float boxRatio = width / (float) Math.max(height, 1);
            String sig = source.getWidth() + "x" + source.getHeight() + "->" + width + "x" + height;
            if (!sig.equals(this.aspectSignature)) {
                this.aspectSignature = sig;
                // v1.10.56：把**桌面那边**的尺寸也一起写进来。
                // 「预览是这个比例、贴到桌面变成那个比例」这类问题，一行就能对比出来：
                // 编辑页画布 vs 组件渲染 px 的比例必须相同（v1.10.56 起 widgetAspect 与桌面同一套推导）。
                String desk = "?";
                try {
                    int[] size = WidgetRender.INSTANCE.widgetSize(this,
                            android.appwidget.AppWidgetManager.getInstance(this), this.widgetId);
                    desk = size[0] + "x" + size[1] + "(" + String.format(java.util.Locale.US, "%.3f",
                            size[0] / (float) Math.max(size[1], 1)) + ")";
                } catch (Throwable ignored) {
                }
                Diag.warn(this, "F10", "比例 素材=" + String.format(java.util.Locale.US, "%.3f", srcRatio)
                        + " 画布=" + String.format(java.util.Locale.US, "%.3f", boxRatio)
                        + " 源=" + source.getWidth() + "x" + source.getHeight()
                        + " 画布px=" + width + "x" + height
                        + " 桌面=" + desk
                        + " 布局=" + this.selectedLayout + " 适配=" + this.selectedFitContent
                        + " 缩放=" + progress(this.seekScale) + "%"
                        + (Math.abs(srcRatio - boxRatio) / Math.max(srcRatio, 0.001f) > 0.02f ? " (差>2%，布局非原始比例时属正常)" : " (一致)"));
            }
        } catch (Throwable ignored) {
        }
        // v1.10.76：按**当前实际会用的边长**先把源图缩下来再合成 —— 拖清晰度滑块时能立刻看到
        // 「变糊 / 变清」，而不用等几秒钟的重抽。（松手后的真实重抽会再用精确边长重来一遍）
        Bitmap scaled = null;
        Bitmap src = source;
        try {
            int edgeNow = effectiveEdge();
            int longest = Math.max(source.getWidth(), source.getHeight());
            if (edgeNow > 0 && longest > edgeNow + 1) {
                Bitmap fitted = FrameUtil.fit(source, edgeNow);
                if (fitted != null && fitted != source) {
                    scaled = fitted;
                    src = fitted;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return FrameComposer.INSTANCE.compose(src, width, height, this.selectedLayout, this.selectedShape,
                    progress(this.seekAlpha), isChecked(this.cbBorder), progress(this.seekScale),
                    this.selectedRotation, this.selectedShapeRotation, this.selectedFitContent,
                    progress(this.seekShadow), this.selectedShadowCurve, this.selectedOffsetX, this.selectedOffsetY,
                    this.selectedContentScale, this.selectedMaskX, this.selectedMaskY);
        } finally {
            if (scaled != null && scaled != src && !scaled.isRecycled()) {
                scaled.recycle();
            }
        }
    }

    private static int progress(@Nullable SeekBar bar) {
        return bar != null ? bar.getProgress() : 0;
    }

    private static boolean isChecked(@Nullable CheckBox box) {
        return box != null && box.isChecked();
    }

    /** 上屏（主线程）。 */
    final void publishPreviewFrame(int generation, @NotNull Bitmap out, int index) {
        // v1.10.50：导出期间画面必须冻结 —— 迟到的预览帧不许上屏。
        // 注意只判 busy、不判 parseTaskActive：解析过程中「首帧先上屏」是刻意保留的行为
        // （K02/L05），解析时动画循环本来就停着，不存在「继续前进」。
        // v1.10.51：唯一放行的是**冻结帧本身** —— 导出中改蒙版/布局/开关要靠它重画，
        // 但帧号必须等于 previewFrozenIndex，所以画面只会原地重绘、不会前进。
        if (this.busy && index != this.previewFrozenIndex) {
            Diag.warnThrottled(this, "L31", "导出中：丢弃非冻结帧上屏 idx=" + index
                    + " 冻结=" + this.previewFrozenIndex);
            return;
        }
        if (generation == this.previewGeneration.get() || this.playing) {
            ImageView imageView = this.imgPreview;
            if (imageView != null && !out.isRecycled()) {
                imageView.setImageBitmap(out);
                this.fpsWindowFrames++;
                if (!this.previewFirstLogged) {
                    this.previewFirstLogged = true;
                    Diag.warn(this, "L05", "预览帧: 首帧上屏 " + out.getWidth() + "x" + out.getHeight());
                    this.updateEmptyHint();
                }
            }
        }
    }

    /** 预取后面几帧进缓存（渲染线程，顺序播放时几乎全命中）。 */
    final void prefetchPreview(int index) {
        try {
            PreviewFrames.Result result = this.previewFrames;
            if (result == null) {
                return;
            }
            List<String> list = result.getFrames();
            int size = list.size();
            if (size <= 1) {
                return;
            }
            FrameCache cache = previewCache();
            for (int k = 1; k <= 3; k++) {
                int at = ((index + k) % size + size) % size;
                if (at >= list.size()) {
                    continue;
                }
                String next = list.get(at);
                // v1.10.56/63：键和 renderFramePath 保持一致（带解码目标尺寸），预取才真的命中
                int[] canvas = previewCanvas();
                String key = canvas[0] + "x" + canvas[1] + "|" + next;
                if (cache.get(key) != null) {
                    continue;
                }
                Bitmap decoded = PreviewFrames.decodeFrame(next, canvas[0], canvas[1]);
                if (decoded != null) {
                    cache.put(key, decoded);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    final FrameCache previewCache() {
        FrameCache cache = this.previewFrameCache;
        if (cache == null) {
            int heapKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
            // 手机 16GB：内存换 CPU —— 缓存预算 heap/4，顺序播放基本不落盘
            cache = new FrameCache(heapKb / 4);
            this.previewFrameCache = cache;
        }
        return cache;
    }

    /**
     * v1.10.50：播放进度条 + 时间文字的**唯一**写入方。
     *
     * 为什么必须只有一个：以前两条播放循环各自 seekBar.setProgress + tvPlayTime.setText，
     * 文字走的是 updatePlayTime（有任务门闸），条子是裸写（没门闸）——
     * 于是「文字 0:00.00、条子停在 88%」这种自相矛盾的画面就可能出现，
     * 任务期间也可能被迟到的 tick 推着走。
     *
     * 现在：任何任务（解析 / 导出）进行中一律拒写；条子与文字同一帧、同一个 index 一起写。
     */
    private final void publishPlaybackPosition(int index, @Nullable String timeText) {
        if (this.parseTaskActive || this.busy) {
            // 正常不会走到这里。谁在任务中想推播放条，日志里会留下调用点。
            Diag.warnThrottled(this, "L34", "任务中拒绝写播放条 idx=" + index + " 来自=" + caller());
            return;
        }
        SeekBar bar = this.seekPlayback;
        if (bar != null) {
            int max = bar.getMax();
            int safe = Math.max(0, index);
            if (max > 0 && safe > max) {
                safe = max;
            }
            if (max > 0 && safe >= max && !this.playing) {
                // 没在播放却把滑块顶到最右端 = 可疑（历史 bug：任务收尾用陈旧下标把条子闪到尾部）
                Diag.warnThrottled(this, "L34", "播放条贴右端 idx=" + index + " max=" + max
                        + " 来自=" + caller());
            }
            bar.setProgress(safe);
        }
        if (timeText != null && this.tvPlayTime != null) {
            this.tvPlayTime.setText(timeText);
        }
    }

    /**
     * v1.10.51：只对齐播放条（不写文字）—— 用于「任务开始/冻结」时刻。
     * 它不吃「任务中不写」的门闸，因为写进去的永远是**冻结帧下标**，不可能让进度前进。
     */
    private final void syncPlaybackBarOnly(int index) {
        SeekBar bar = this.seekPlayback;
        if (bar == null) {
            return;
        }
        int max = bar.getMax();
        int safe = Math.max(0, index);
        if (max > 0 && safe > max) {
            safe = max;
        }
        if (bar.getProgress() != safe) {
            bar.setProgress(safe);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updatePlayTime(int index) {
        // v1.10.25：任务进行中这一行归任务 UI（显示「解析 42%」），播放循环不许覆盖
        if (this.parseTaskActive || this.busy) {
            return;
        }
        int fps;
        PreviewFrames.Result result = this.previewFrames;
        if (result != null) {
            fps = result.getFps();
        } else {
            WidgetConfig widgetConfig = this.base;
            fps = widgetConfig != null ? widgetConfig.getFps() : 1;
        }
        int fps2 = fps;
        // v1.10.50：条子和文字一起写（同一个 index），不允许只改文字
        publishPlaybackPosition(index, formatTime((index * 1000) / RangesKt.coerceAtLeast(fps2, 1)));
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x004e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    /** v1.8.18：fps/边长/质量手动变化且与上次抽帧参数不同 → 重新解析。 */
    /**
     * v1.10.76：拖动「画面」三项参数时的**实时重渲**。
     *
     * 以前每动一格就触发 onRefillParamChanged() → 重抽整段素材（解码 + 落盘，几秒），
     * 拖动期间画面是僵住的。现在拖动中只做「毫秒级」的事：
     *   • 清晰度：按新边长把内存里的源图缩下来再合成（能立刻看出变糊/变清）
     *   • 帧率：用新帧率重启预览循环（间隔立刻变）
     *   • 压缩质量：对预览没有任何像素影响，只刷新文字
     * 松手时才做真正的重抽，保证最终状态精确。
     */
    private final void liveParamsPreview() {
        try {
            List<String> frames = this.previewFrames != null ? this.previewFrames.getFrames() : null;
            boolean animated = frames != null && frames.size() > 1;
            if (animated) {
                if (this.playing) {
                    startPreviewFramesPlayback();   // 用新帧率重启（只改间隔，不动帧表）
                } else {
                    renderPreviewFrameAt(this.playIndex);
                }
            } else {
                refreshPreviewNow();
            }
        } catch (Throwable th) {
            Diag.warnThrottled(this, "L36", "拖动参数实时重渲异常: " + th.getClass().getSimpleName());
        }
    }

    private final void onRefillParamChanged() {
        // v1.10.76：拖动中一律不重抽（松手时才调到这里）
        if (this.paramsDragging) {
            return;
        }
        SeekBar seekBar = this.seekFps;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            seekBar = null;
        }
        int fps = effectiveFpsX10();
        int edge = effectiveEdge();   // v1.10.59：滑块 + 素材尺寸 + 格式阈值，同一处取值
        SeekBar seekBar3 = this.seekQuality;
        if (seekBar3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar3 = null;
        }
        // v1.10.0：预览帧是无损 PNG，"质量"只作用于导出 —— 不再因为质量变化重抽预览
        if (fps == this.activeFps && edge == this.activeEdge) {
            // 与上次抽帧参数一致（程序性赋值会走到这），不重复解析
            return;
        }
        // v1.10.1（需求 3）：导出进行中改了需要重新解析的参数 →
        // 先真正取消旧任务（连暂存一起删），再用最新参数重开，圆环归 0、再次提示。
        if (this.busy) {
            Diag.warn(this, "T03", "导出中修改解析参数 → 取消旧任务并重开 fps=" + fps + " edge=" + edge);
            cancelExportQuietly();
            save();
            return;
        }
        // v1.9.3：若正在解析 —— 作废在途解析（旧线程的发布/进度全部代次失格），
        // 否则 loadPreviewFrames 会被 loading 闸门跳过、参数改动丢失
        if (this.previewFramesLoading) {
            this.extractGeneration++;
            this.previewFramesLoading = false;
        }
        Diag.warn(this, "R05", "参数变化触发重解析 fps=" + fps + " edge=" + edge);
        scheduleRefillFrames();
    }

    /** v1.9.5：探测改了参数且看门狗已在抽 → 作废在途重抽（参数一致则不动）。 */
    private final void refillIfParamsChanged(Uri uri, MediaKind kind) {
        SeekBar seekBar = this.seekFps;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekFps");
            seekBar = null;
        }
        int fps = effectiveFpsX10();
        int edge = effectiveEdge();   // v1.10.59：滑块 + 素材尺寸 + 格式阈值，同一处取值
        SeekBar seekBar3 = this.seekQuality;
        if (seekBar3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
            seekBar3 = null;
        }
        if (fps == this.activeFps && edge == this.activeEdge) {
            return;
        }
        this.extractGeneration++;
        this.previewFramesLoading = false;
        Diag.warn(this, "R06", "探测后参数变化，作废在途重抽 fps=" + fps + " edge=" + edge);
        this.loadPreviewFrames(uri, kind);
    }

    private final void scheduleRefillFrames() {
        /*
            r5 = this;
            r0 = r5
            android.net.Uri r0 = r0.pickedUri
            r1 = r0
            if (r1 != 0) goto L50
        L9:
            r0 = r5
            io.github.animwidget.WidgetConfig r0 = r0.base
            r1 = r0
            if (r1 == 0) goto L4e
            java.lang.String r0 = r0.getSourceUri()
            r1 = r0
            if (r1 == 0) goto L4e
            r10 = r0
            r0 = r10
            r11 = r0
            r0 = 0
            r12 = r0
            r0 = r11
            java.lang.CharSequence r0 = (java.lang.CharSequence) r0
            int r0 = r0.length()
            if (r0 <= 0) goto L32
            r0 = 1
            goto L33
        L32:
            r0 = 0
        L33:
            if (r0 == 0) goto L3c
            r0 = r10
            goto L3d
        L3c:
            r0 = 0
        L3d:
            r1 = r0
            if (r1 == 0) goto L4e
            r11 = r0
            r0 = 0
            r12 = r0
            r0 = r11
            android.net.Uri r0 = android.net.Uri.parse(r0)
            goto L50
        L4e:
            r0 = 0
        L50:
            r6 = r0
            r0 = r5
            io.github.animwidget.MediaKind r0 = r0.pickedKind
            r1 = r0
            if (r1 != 0) goto L70
        L5a:
            r0 = r5
            io.github.animwidget.WidgetConfig r0 = r0.base
            r1 = r0
            if (r1 == 0) goto L68
            io.github.animwidget.MediaKind r0 = r0.getKind()
            goto L6a
        L68:
            r0 = 0
        L6a:
            r1 = r0
            if (r1 != 0) goto L70
        L6f:
            return
        L70:
            r7 = r0
            r0 = r6
            if (r0 != 0) goto L76
            return
        L76:
            r0 = r5
            java.lang.Runnable r0 = r0.refillTask
            r1 = r0
            if (r1 == 0) goto L8f
            r9 = r0
            r0 = 0
            r10 = r0
            r0 = r5
            android.os.Handler r0 = r0.ui
            r1 = r9
            r0.removeCallbacks(r1)
            goto L91
        L8f:
        L91:
            r0 = r5
            r1 = r6
            r2 = r7
            void r0 = () -> { // java.lang.Runnable.run():void
                scheduleRefillFrames$lambda$79(ConfigActivity.this, uri, kind);
            }
            r8 = r0
            r0 = r5
            r1 = r8
            r0.refillTask = r1
            r0 = r5
            android.os.Handler r0 = r0.ui
            r1 = r8
            r2 = 500(0x1f4, double:2.47E-321)
            boolean r0 = r0.postDelayed(r1, r2)
            return
        */
        final Uri uri;
        if (this.pickedUri != null) {
            uri = this.pickedUri;
        } else {
            WidgetConfig base = this.base;
            String sourceUri = base != null ? base.getSourceUri() : null;
            uri = (sourceUri != null && sourceUri.length() > 0) ? Uri.parse(sourceUri) : null;
        }
        final MediaKind kind;
        if (this.pickedKind != null) {
            kind = this.pickedKind;
        } else {
            WidgetConfig base2 = this.base;
            kind = base2 != null ? base2.getKind() : null;
        }
        if (kind == null || uri == null) {
            return;
        }
        Runnable previous = this.refillTask;
        if (previous != null) {
            this.ui.removeCallbacks(previous);
        }
        Runnable task = () -> scheduleRefillFrames$lambda$79(this, uri, kind);
        this.refillTask = task;
        this.ui.postDelayed(task, 500L);
    }

    private static final void scheduleRefillFrames$lambda$79(ConfigActivity this$0, Uri $uri, MediaKind $kind) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($kind, "$kind");
        this$0.loadPreviewFrames($uri, $kind);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void schedulePreview() {
        // v1.10.28（需求 2）：导出期间不冻结画面 —— 切布局/形状/蒙版必须立刻能看到效果。
        // 做法：永远重绘「导出开始时的那一帧」（previewFrozenIndex），帧号不变 →
        // 既不会前进/倒退，又能实时预览。
        if (this.busy) {
            int frozen = this.previewFrozenIndex;
            if (frozen >= 0 && this.previewFrames != null && !this.previewFrames.getFrames().isEmpty()) {
                Diag.warnThrottled(this, "L33", "导出中：按冻结帧 " + frozen + " 重绘（布局/蒙版实时预览）");
                renderPreviewFrameAt(frozen);
                return;
            }
            // v1.10.51：还没有预览帧（长视频刚开导出、解析就被取消了）——
            // 以前这里直接 return，于是导出期间改蒙版/开关**画面完全没反应**。
            // 现在退回静态重渲：用缩略图 + 当前布局/蒙版重画，改一下就能看到效果。
            Diag.warnThrottled(this, "L33", "导出中：无预览帧，退回静态重渲（布局/蒙版实时预览）");
            renderPreviewNow();
            return;
        }
        ImageView imageView = this.imgPreview;
        if (imageView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("imgPreview");
            imageView = null;
        }
        // v1.10.2：圆环只由任务状态决定，这里不再自行显隐（否则会和解析任务打架）
        syncTaskUi();
        Runnable it = this.previewTask;
        if (it != null) {
            this.ui.removeCallbacks(it);
        }
        Runnable task = () -> {
            schedulePreview$lambda$81(ConfigActivity.this);
        };
        this.previewTask = task;
        this.ui.postDelayed(task, 0L);
    }

    private static final void schedulePreview$lambda$81(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.renderPreviewNow();
    }

    private final void renderPreviewNow() {
        if (this.previewing) {
            this.pendingPreview = true;
            return;
        }
        WidgetConfig current = this.base;
        if (current == null) {
            // v1.10.2：没有 base 时也别直接藏圆环 —— 交给任务状态
            syncTaskUi();
            return;
        }
        ShapeKind shapeKind = this.selectedShape;
        SeekBar seekBar = this.seekAlpha;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
            seekBar = null;
        }
        int progress = seekBar.getProgress();
        CheckBox checkBox = this.cbBorder;
        if (checkBox == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
            checkBox = null;
        }
        WidgetConfig config = WidgetConfig.copy$default(current, 0, null, null, null, 0, 0, 0, 0, 0, 0, shapeKind, progress, checkBox.isChecked(), this.selectedLayout, false, false, 0, 0, 0, 0, 0, 0, 0, false, 0.0f, 0.0f, 0, 0.0f, 0.0f, null, 1073726463, null);
        Bitmap cached = this.previewSource;
        if ((cached == null || cached.isRecycled()) && config.getFrameCount() <= 0) {
            // v1.10.2：首次导入（还没帧、缩略图也还没到）以前会在这里把圆环藏掉，
            // 而这时正好是解析进行中 —— 用户看到的就是「没有解析进度、画面停在首帧」。
            syncTaskUi();
            return;
        }
        this.previewing = true;
        int generation = this.previewGeneration.incrementAndGet();
        new Thread(() -> {
            renderPreviewNow$lambda$86(cached, ConfigActivity.this, config, generation);
        }).start();
    }

    /* JADX WARN: Removed duplicated region for block: B:8:0x001e A[Catch: Throwable -> 0x016c, all -> 0x01a1, TryCatch #2 {all -> 0x01a1, Throwable -> 0x016c, blocks: (B:5:0x0013, B:17:0x0071, B:19:0x0099, B:20:0x00a0, B:22:0x00ac, B:23:0x00b3, B:25:0x00c1, B:26:0x00c8, B:28:0x00da, B:29:0x00e1, B:31:0x00ee, B:32:0x00f5, B:34:0x010d, B:35:0x0114, B:8:0x001e, B:16:0x0067), top: B:57:0x0013 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
   private static final void renderPreviewNow$lambda$86(Bitmap $cached, ConfigActivity this$0, WidgetConfig $config, int $generation) {
      Intrinsics.checkNotNullParameter(this$0, "this$0");
      Intrinsics.checkNotNullParameter($config, "$config");

      try {
         try {
            Bitmap var13;
            if ($cached != null && !$cached.isRecycled()) {
               var13 = $cached;
            } else {
               var13 = WidgetStore.INSTANCE.loadFrame((Context)this$0, $config, 0, 720, 720);
               if (var13 == null) {
                  return;
               }

               Bitmap loaded = var13;
               this$0.previewSource = loaded;
               var13 = loaded;
            }

            Bitmap source = var13;
            int viewWidth = this$0.previewWidth();
            int viewHeight = this$0.previewHeight();
            PreviewRenderer var14 = PreviewRenderer.INSTANCE;
            LayoutMode var10004 = this$0.selectedLayout;
            ShapeKind var10005 = this$0.selectedShape;
            SeekBar var10006 = this$0.seekAlpha;
            if (this$0.seekAlpha == null) {
               Intrinsics.throwUninitializedPropertyAccessException("seekAlpha");
               var10006 = null;
            }

            int var15 = var10006.getProgress();
            CheckBox var10007 = this$0.cbBorder;
            if (this$0.cbBorder == null) {
               Intrinsics.throwUninitializedPropertyAccessException("cbBorder");
               var10007 = null;
            }

            boolean var16 = var10007.isChecked();
            SeekBar var10009 = this$0.seekEdge;
            if (this$0.seekEdge == null) {
               Intrinsics.throwUninitializedPropertyAccessException("seekEdge");
               var10009 = null;
            }

            int var10008 = 120 + var10009.getProgress() * 8;
            SeekBar var10010 = this$0.seekQuality;
            if (this$0.seekQuality == null) {
               Intrinsics.throwUninitializedPropertyAccessException("seekQuality");
               var10010 = null;
            }

            int var17 = 50 + var10010.getProgress();
            var10010 = this$0.seekScale;
            if (this$0.seekScale == null) {
               Intrinsics.throwUninitializedPropertyAccessException("seekScale");
               var10010 = null;
            }

            int var19 = var10010.getProgress();
            SeekBar var10014 = this$0.seekShadow;
            if (this$0.seekShadow == null) {
               Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
               var10014 = null;
            }

            Bitmap out = var14.render(
               source,
               viewWidth,
               viewHeight,
               var10004,
               var10005,
               var15,
               var16,
               var10008,
               var17,
               var19,
               this$0.selectedRotation,
               this$0.selectedShapeRotation,
               this$0.selectedFitContent,
               var10014.getProgress(),
               this$0.selectedShadowCurve,
               this$0.selectedOffsetX,
               this$0.selectedOffsetY,
               this$0.selectedContentScale,
               this$0.selectedMaskX,
               this$0.selectedMaskY
            );
            final int gen = $generation; final Bitmap bmp = out;
            this$0.ui.post(() -> ConfigActivity.renderPreviewNow$lambda$86$lambda$83(this$0, gen, bmp));
        } catch (Throwable var10) {
            // v1.8.0：合成失败不要留一片空白 —— 记下来，并直接把原始缩略图贴出来
            CrashLog.save(this$0, Thread.currentThread(), var10);
            final Bitmap raw = $cached != null ? $cached : this$0.previewSource;
            this$0.ui.post(new Runnable() {
               @Override
               public void run() {
                  if (raw != null && !raw.isRecycled()) {
                     ImageView view = this$0.imgPreview;
                     if (view != null) {
                        view.setImageBitmap(raw);
                     }
                     this$0.updateEmptyHint();
                  }
               }
            });
        }
      } finally {
         this$0.previewing = false;
         if (this$0.pendingPreview) {
            this$0.pendingPreview = false;
            this$0.ui.post(() -> ConfigActivity.renderPreviewNow$lambda$86$lambda$84(this$0));
         } else {
            this$0.ui.post(() -> ConfigActivity.renderPreviewNow$lambda$86$lambda$85(this$0));
         }
      }
   }

    private static final void renderPreviewNow$lambda$86$lambda$83(ConfigActivity this$0, int $generation, Bitmap $out) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($out, "$out");
        this$0.updateEmptyHint();
        // v1.8.15：播放中让位 —— 静态重渲不再抢屏，动画帧全权接管 ImageView
        if ($generation == this$0.previewGeneration.get() && !this$0.playing) {
            ImageView imageView = this$0.imgPreview;
            if (imageView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("imgPreview");
                imageView = null;
            }
            imageView.setImageBitmap($out);
            if (this$0.pendingFirstFrame && !$out.isRecycled()) {
                this$0.pendingFirstFrame = false;
                this$0.startPlayback(true);
            }
        }
        // v1.10.6 关键修复：这里原来直接写 parseTaskActive=false —— 缩略图在导入后
        // 约 1 秒上屏，就把「正在进行的解析」的任务状态清掉，于是解析条/圆环
        // 只闪一下就消失（用户的「只显示一秒钟」）。静态渲染无权改任务状态。
        this$0.syncTaskUi();
    }

    private static final void renderPreviewNow$lambda$86$lambda$84(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.renderPreviewNow();
    }

    private static final void renderPreviewNow$lambda$86$lambda$85(ConfigActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.9.1：抽帧进行中不许藏解析条（原来静态渲染一完成就藏，
        // 把 loadPreviewFrames 刚显示的解析进度条立刻盖掉了 —— “没有解析进度条”）
        if (this$0.previewFramesLoading) {
            return;
        }
        // v1.10.2：任务状态只由抽帧/导出流程维护，静态渲染不要碰
        this$0.syncTaskUi();
    }

    private final void toast(String message) {
        Toast.makeText(this, message, 0).show();
    }
}
