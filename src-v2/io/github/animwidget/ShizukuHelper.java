package io.github.animwidget;

import android.app.Activity;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.io.TextStreamsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

/* compiled from: ShizukuHelper.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��:\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\b\n��\n\u0002\u0010\u000b\n��\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u001e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\t2\b\u0010\u000b\u001a\u0004\u0018\u00010\tJ\u0006\u0010\f\u001a\u00020\u0006J\u0006\u0010\r\u001a\u00020\u000eJ\u0006\u0010\u000f\u001a\u00020\u0006J\"\u0010\u0010\u001a\u00020\u000e2\u0006\u0010\u0011\u001a\u00020\u00122\u0012\u0010\u0013\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u000e0\u0014J\u0014\u0010\u0015\u001a\u00020\t2\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\t0\bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/ShizukuHelper;", "", "()V", "PERMISSION_REQUEST", "", "listenerRegistered", "", "backgroundCommands", "", "", "packageName", "launcherPackage", "hasPermission", "init", "", "isReady", "requestPermission", "activity", "Landroid/app/Activity;", "callback", "Lkotlin/Function1;", "runCommands", "commands"})
@SourceDebugExtension({"SMAP\nShizukuHelper.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ShizukuHelper.kt\nio/github/animwidget/ShizukuHelper\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,104:1\n1#2:105\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/ShizukuHelper.class */
public final class ShizukuHelper {

    @NotNull
    public static final ShizukuHelper INSTANCE = new ShizukuHelper();
    private static final int PERMISSION_REQUEST = 100;
    static volatile boolean listenerRegistered;

    private ShizukuHelper() {
    }

    public final void init() {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        try {
            Shizuku.addBinderReceivedListenerSticky(ShizukuHelper::init$lambda$0);
        } catch (Throwable th) {
        }
    }

    private static final void init$lambda$0() {
    }

    public final boolean isReady() {
        boolean zPingBinder;
        try {
            zPingBinder = Shizuku.pingBinder();
        } catch (Throwable th) {
            zPingBinder = false;
        }
        return zPingBinder;
    }

    /* JADX WARN: Removed duplicated region for block: B:7:0x0012  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean hasPermission() {
        /*
            r2 = this;
            r0 = r2
            boolean r0 = r0.isReady()     // Catch: java.lang.Throwable -> L17
            if (r0 == 0) goto L12
            int r0 = rikka.shizuku.Shizuku.checkSelfPermission()     // Catch: java.lang.Throwable -> L17
            if (r0 != 0) goto L12
            r0 = 1
            goto L13
        L12:
            r0 = 0
        L13:
            r3 = r0
            goto L1a
        L17:
            r4 = move-exception
            r0 = 0
            r3 = r0
        L1a:
            r0 = r3
            return r0
        */
        try {
            return isReady() && Shizuku.checkSelfPermission() == 0;
        } catch (Throwable th) {
            return false;
        }
    }

    public final void requestPermission(@NotNull Activity activity, @NotNull Function1<? super Boolean, Unit> function1) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        Intrinsics.checkNotNullParameter(function1, "callback");
        try {
            if (!isReady()) {
                function1.invoke(false);
            } else if (hasPermission()) {
                function1.invoke(true);
            } else {
                Shizuku.addRequestPermissionResultListener((v1, v2) -> {
                    requestPermission$lambda$1(function1, v1, v2);
                });
                Shizuku.requestPermission(PERMISSION_REQUEST);
            }
        } catch (Throwable th) {
            function1.invoke(false);
        }
    }

    private static int waitForQuietly(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static final void requestPermission$lambda$1(Function1 $callback, int requestCode, int grantResult) {
        Intrinsics.checkNotNullParameter($callback, "$callback");
        if (requestCode == PERMISSION_REQUEST) {
            $callback.invoke(Boolean.valueOf(grantResult == 0));
        }
    }

    @NotNull
    public final String runCommands(@NotNull List<String> list) {
        ShizukuRemoteProcess process = null;
        BufferedReader bufferedReader = null;
        Throwable th;
        Intrinsics.checkNotNullParameter(list, "commands");
        StringBuilder report = new StringBuilder();
        for (String command : list) {
            report.append("$ ").append(command).append('\n');
            try {
                process = Shizuku.newProcess(new String[]{"sh", "-c", command}, (String[]) null, (String) null);
                InputStream inputStream = process.getInputStream();
                Intrinsics.checkNotNullExpressionValue(inputStream, "getInputStream(...)");
                Reader inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8);
                bufferedReader = inputStreamReader instanceof BufferedReader ? (BufferedReader) inputStreamReader : new BufferedReader(inputStreamReader, 8192);
                th = null;
            } catch (Throwable t) {
                StringBuilder sbAppend = report.append("失败: ");
                String message = t.getMessage();
                if (message == null) {
                    message = t.getClass().getSimpleName();
                }
                sbAppend.append(message).append('\n');
            }
            try {
                try {
                    BufferedReader it = bufferedReader;
                    String output = TextStreamsKt.readText(it);
                    CloseableKt.closeFinally(bufferedReader, (Throwable) null);
                    InputStream errorStream = process.getErrorStream();
                    Intrinsics.checkNotNullExpressionValue(errorStream, "getErrorStream(...)");
                    Reader inputStreamReader2 = new InputStreamReader(errorStream, Charsets.UTF_8);
                    bufferedReader = inputStreamReader2 instanceof BufferedReader ? (BufferedReader) inputStreamReader2 : new BufferedReader(inputStreamReader2, 8192);
                    Throwable th2 = null;
                    try {
                        try {
                            BufferedReader it2 = bufferedReader;
                            String error = TextStreamsKt.readText(it2);
                            CloseableKt.closeFinally(bufferedReader, (Throwable) null);
                            int code = waitForQuietly(process);
                            if (!StringsKt.isBlank(output)) {
                                report.append(StringsKt.trim(output).toString()).append('\n');
                            }
                            if (!StringsKt.isBlank(error)) {
                                report.append(StringsKt.trim(error).toString()).append('\n');
                            }
                            report.append("exit=").append(code).append('\n');
                        } finally {
                        }
                    } catch (Throwable th3) {
                        th2 = th3;
                        throw th3;
                    }
                } finally {
                }
            } catch (Throwable th4) {
                th = th4;
                throw th4;
            }
        }
        String string = report.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    @NotNull
    public final List<String> backgroundCommands(@NotNull String packageName, @Nullable String launcherPackage) {
        Intrinsics.checkNotNullParameter(packageName, "packageName");
        List commands = CollectionsKt.mutableListOf(new String[]{"cmd deviceidle whitelist +" + packageName, "am set-standby-bucket " + packageName + " active", "cmd appops set " + packageName + " RUN_IN_BACKGROUND allow", "cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND allow"});
        String str = launcherPackage;
        if (!(str == null || StringsKt.isBlank(str)) && !Intrinsics.areEqual(launcherPackage, packageName)) {
            commands.add("cmd deviceidle whitelist +" + launcherPackage);
            commands.add("am set-standby-bucket " + launcherPackage + " active");
        }
        commands.add("cmd power set-fixed-performance-mode-enabled true");
        commands.add("settings put global cached_apps_freezer disabled");
        return commands;
    }
}
