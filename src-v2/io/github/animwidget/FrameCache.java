package io.github.animwidget;

import android.graphics.Bitmap;
import android.util.LruCache;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: WidgetRender.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��-\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000e\n\u0002\b\u0002*\u0001\u0006\u0018��2\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0006\u0010\b\u001a\u00020\tJ\u0010\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\rJ\u0016\u0010\u000e\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u000bR\u0010\u0010\u0005\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u0007"}, d2 = {"Lio/github/animwidget/FrameCache;", "", "maxKb", "", "(I)V", "cache", "io/github/animwidget/LruCache<String, Bitmap>", "Lio/github/animwidget/LruCache<String, Bitmap>;", "clear", "", "get", "Landroid/graphics/Bitmap;", "key", "", "put", "bitmap"})
/* loaded from: classes.jar:io/github/animwidget/FrameCache.class */
public final class FrameCache {

    @NotNull
    final LruCache<String, Bitmap> cache;

    /* JADX WARN: Type inference failed for: r1v2, types: [io.github.animwidget.LruCache<String, Bitmap>] */
    public FrameCache(int maxKb) {
        final int iCoerceAtLeast = RangesKt.coerceAtLeast(maxKb, 1024);
        this.cache = new LruCache<String, Bitmap>(iCoerceAtLeast) { // from class: io.github.animwidget.LruCache<String, Bitmap>
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public int sizeOf(@NotNull String key, @NotNull Bitmap value) {
                Intrinsics.checkNotNullParameter(key, "key");
                Intrinsics.checkNotNullParameter(value, "value");
                return value.getByteCount() / 1024;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public void entryRemoved(boolean evicted, @NotNull String key, @NotNull Bitmap oldValue, @Nullable Bitmap newValue) {
                Intrinsics.checkNotNullParameter(key, "key");
                Intrinsics.checkNotNullParameter(oldValue, "oldValue");
                // v1.8.11：淘汰时不再 recycle —— 取出来的帧可能正被另一条线程
                // 拿去 setImageViewBitmap / compose，recycle 就是 use-after-recycle 崩溃。
                // 缓存帧都是小图，丢引用交 GC 即可。
            }
        };
    }

    @Nullable
    public final Bitmap get(@NotNull String key) {
        Intrinsics.checkNotNullParameter(key, "key");
        // v1.8.3：反编译时把 `this.cache.` 前缀吃掉了，变成自己调自己 →
        // 每一帧都 StackOverflowError（被 PlaybackService 的 catch 吞掉），
        // 表现就是「多帧组件根本不动」。补回缓存调用。
        return this.cache.get(key);
    }

    public final void put(@NotNull String key, @NotNull Bitmap bitmap) {
        Intrinsics.checkNotNullParameter(key, "key");
        Intrinsics.checkNotNullParameter(bitmap, "bitmap");
        this.cache.put(key, bitmap);
    }

    public final void clear() {
        this.cache.evictAll();
    }

    /** v1.10.262：当前占用的 KB（sizeOf 的单位就是 KB，见构造里的 `getByteCount()/1024`）。 */
    public final int sizeKb() {
        return this.cache.size();
    }
}
