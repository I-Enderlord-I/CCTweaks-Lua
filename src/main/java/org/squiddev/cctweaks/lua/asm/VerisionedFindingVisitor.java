package org.squiddev.cctweaks.lua.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.squiddev.patcher.visitors.FindingVisitor;

public abstract class VerisionedFindingVisitor extends FindingVisitor {
	private int version = Opcodes.V1_6;

	public VerisionedFindingVisitor(ClassVisitor classVisitor, AbstractInsnNode... nodes) {
		super(classVisitor, nodes);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.version = version;
	}

	protected boolean requiresFrames() {
		return version > Opcodes.V1_6;
	}
}
