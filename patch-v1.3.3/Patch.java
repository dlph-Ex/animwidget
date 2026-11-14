import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;

public class Patch {
    static final Set<String> TARGETS = new HashSet<>(Arrays.asList(
        "onCreate$lambda$1",
        "onCreate$lambda$9$lambda$8",
        "buildPresets$lambda$28$lambda$24",
        "buildPresets$lambda$28$lambda$27$lambda$26",
        "showColorEditor$lambda$51"));

    public static void main(String[] a) throws Exception {
        byte[] in = Files.readAllBytes(Paths.get(a[0]));
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (!TARGETS.contains(name)) {
                    return mv;
                }
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
        }, 0);
        Files.write(Paths.get(a[1]), cw.toByteArray());
        System.out.println("patched -> " + a[1]);
    }
}
