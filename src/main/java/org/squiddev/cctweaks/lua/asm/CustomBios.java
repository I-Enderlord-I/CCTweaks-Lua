package org.squiddev.cctweaks.lua.asm;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.computer.Computer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.squiddev.patcher.transformer.IPatcher;
import org.squiddev.patcher.visitors.FindingVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Allows changing the bios file
 *
 * @see Computer#initLua()
 * @see org.squiddev.cctweaks.lua.patch.Computer_Patch#setRomMount(String, IMount)
 */
public class CustomBios implements IPatcher {
	@Override
	public boolean matches(String className) {
		return className.equals("dan200.computercraft.core.computer.Computer");
	}

	@Override
	public ClassVisitor patch(String className, ClassVisitor delegate) throws Exception {
		return new FindingVisitor(
			delegate,
			new LdcInsnNode("lua/bios.lua")
		) {
			@Override
			public void handle(InsnList nodes, MethodVisitor visitor) {
				Label finish = new Label(), def = new Label();

				visitor.visitVarInsn(ALOAD, 0);
				visitor.visitFieldInsn(GETFIELD, "dan200/computercraft/core/computer/Computer", "biosPath", "Ljava/lang/String;");
				visitor.visitJumpInsn(IFNULL, def);

				visitor.visitVarInsn(ALOAD, 0);
				visitor.visitFieldInsn(GETFIELD, "dan200/computercraft/core/computer/Computer", "biosPath", "Ljava/lang/String;");
				visitor.visitJumpInsn(GOTO, finish);

				visitor.visitLabel(def);
				visitor.visitLdcInsn("lua/bios.lua");

				visitor.visitLabel(finish);
			}
		}.onMethod("initLua").mustFind();
	}
}
