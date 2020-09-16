package me.modmuss50.optifabric.patcher.fixes;

import me.modmuss50.optifabric.util.RemappingUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class WorldRendererFix implements ClassFixer {

    @Override
    public void fix(ClassNode classNode, ClassNode old) {
        for (MethodNode methodNode : classNode.methods) {
            for (int i = 0; i < methodNode.instructions.size(); i++) {
                AbstractInsnNode insnNode = methodNode.instructions.get(i);

                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

                    if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        if ("renderParticles".equals(methodInsnNode.name)) {
							/* For reference:

							class_4587 - net/minecraft/client/util/math/MatrixStack
							class_4597$class_4598 - net/minecraft/client/render/VertexConsumerProvider.Immediate
							class_765 - net/minecraft/client/render/LightmapTextureManager
							class_4184 - net/minecraft/client/render/Camera
							 */

                            String desc = "(Lnet/minecraft/class_4587;"
                                    + "Lnet/minecraft/class_4597$class_4598;"
                                    + "Lnet/minecraft/class_765;"
                                    + "Lnet/minecraft/class_4184;"
                                    + "F)V";
                            String name = RemappingUtils.getMethodName("class_702", "method_3049", desc);

                            System.out.printf("Replacement `renderParticles` call:  %s.%s%n", name, desc);

                            //Replaces the method call with the vanilla one, this calls down to the same method just without extra data for particle culling
                            methodInsnNode.name = name;
                            methodInsnNode.desc = RemappingUtils.mapMethodDescriptor(desc);

                            //Remove the old extra method call
                            methodNode.instructions.remove(methodNode.instructions.get(i - 1));
                        }
                    }
                }
            }
        }
    }

}
