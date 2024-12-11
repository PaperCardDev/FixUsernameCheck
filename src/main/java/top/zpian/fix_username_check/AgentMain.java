package top.zpian.fix_username_check;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentMain {
    public static void premain(String arg, Instrumentation inst) {

        System.out.println("FixUsernameCheck - premain");

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

                // 不是需要的类
                if (!className.equals("com/destroystokyo/paper/profile/CraftPlayerProfile")) {
                    return ClassFileTransformer.super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                }

                System.out.println("Transform class: " + className);

                final ClassReader cr = new ClassReader(classfileBuffer);

                final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

                cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

                        if (!name.equals("createAuthLibProfile")) {
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }

                        System.out.println("Transform method: " + name);

                        final var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

//                                System.out.printf("%d %s %s %s %s%n", opcode, owner, name, descriptor, isInterface);

                                if (opcode == Opcodes.INVOKESTATIC &&
                                        name.equals("checkArgument") &&
                                        owner.equals("com/google/common/base/Preconditions") &&
                                        descriptor.equals("(ZLjava/lang/String;Ljava/lang/Object;)V") && !isInterface
                                ) {
                                    System.out.println("取消静态方法调用: " + owner + "/" + name);

                                    // 弹出参数
                                    this.visitInsn(Opcodes.POP);
                                    this.visitInsn(Opcodes.POP);
                                    this.visitInsn(Opcodes.POP);

                                    return;
                                }

                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        };
                    }
                }, ClassReader.EXPAND_FRAMES);

                return cw.toByteArray();
            }
        });
    }
}