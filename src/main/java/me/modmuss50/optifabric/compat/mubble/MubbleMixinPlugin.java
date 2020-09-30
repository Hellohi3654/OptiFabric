package me.modmuss50.optifabric.compat.mubble;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.tinyremapper.IMappingProvider.Member;

import me.modmuss50.optifabric.compat.InterceptingMixinPlugin;
import me.modmuss50.optifabric.util.RemappingUtils;

public class MubbleMixinPlugin extends InterceptingMixinPlugin {
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if ("ItemRendererMixin".equals(mixinInfo.getName())) {//ItemStack, isDamaged
			Member bobViewWhenHurt = RemappingUtils.mapMethod("class_1799", "method_7986", "()Z");
			String renderGuiItemOverlayDesc = "(Lnet/minecraft/class_327;Lnet/minecraft/class_1799;IILjava/lang/String;)V"; //(TextRenderer, ItemStack)
			String renderGuiItemOverlay = RemappingUtils.getMethodName("class_918", "method_4022", renderGuiItemOverlayDesc);
			renderGuiItemOverlayDesc = RemappingUtils.mapMethodDescriptor(renderGuiItemOverlayDesc);

			for (MethodNode method : targetClass.methods) {
				if (renderGuiItemOverlay.equals(method.name) && renderGuiItemOverlayDesc.equals(method.desc)) {
					LabelNode skip = new LabelNode();

					InsnList extra = new InsnList();
					extra.add(new JumpInsnNode(Opcodes.GOTO, skip));
					extra.add(new InsnNode(Opcodes.ACONST_NULL));
					extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, bobViewWhenHurt.owner, bobViewWhenHurt.name, bobViewWhenHurt.desc, false));
					extra.add(new InsnNode(Opcodes.POP));
					extra.add(skip);

					method.instructions.insertBefore(method.instructions.getLast(), extra);
					break;
				}
			}
		}

		super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
	}
}