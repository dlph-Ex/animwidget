import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;

/**
 * 字节码补丁工具。
 *
 * mode=settings : SettingsActivity 里 5 处「换主题后 recreate()」→ ThemePatch.inPlaceRefresh(Activity)
 * mode=delays   : ConfigActivity 里 schedulePreview()/refreshPreviewNow() 的防抖延迟 120ms/90ms → 0ms
 *                 （拖滑块时每帧都能拿到新预览，配合去掉 WebP 往返 = 真·实时）
 */
public class Patch {

    private static final Set<String> RECREATE_METHODS = new HashSet<>(Arrays.asList(
            "onCreate$lambda$1",
            "onCreate$lambda$9$lambda$8",
            "buildPresets$lambda$28$lambda$24",
            "buildPresets$lambda$28$lambda$27$lambda$26",
            "showColorEditor$lambda$51"));

    private static final Set<String> DELAY_METHODS = new HashSet<>(Arrays.asList(
            "schedulePreview", "refreshPreviewNow"));

    public static void main(String[] a) throws Exception {
        final String mode = a.length > 2 ? a[2] : "settings";
        byte[] in = Files.readAllBytes(Paths.get(a[0]));
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if ("settings".equals(mode) && RECREATE_METHODS.contains(name)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                            if (n.equals("recreate") && d.equals("()V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                        "io/github/animwidget/ThemePatch", "inPlaceRefresh",
                                        "(Landroid/app/Activity;)V", false);
                            } else {
                                super.visitMethodInsn(op, owner, n, d, itf);
                            }
                        }
                    };
                }
                if ("delays".equals(mode) && DELAY_METHODS.contains(name)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof Long && (((Long) value).longValue() == 90L || ((Long) value).longValue() == 120L)) {
                                super.visitLdcInsn(Long.valueOf(0L));
                            } else {
                                super.visitLdcInsn(value);
                            }
                        }
                    };
                }
                return mv;
            }
        }, 0);
        Files.write(Paths.get(a[1]), cw.toByteArray());
        System.out.println("patched (" + mode + ") -> " + a[1]);
    }
}
