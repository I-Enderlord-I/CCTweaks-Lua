package org.squiddev.cctweaks.lua.launch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;
import org.squiddev.cctweaks.lua.asm.AsmUtils;
import org.squiddev.patcher.transformer.IPatcher;
import org.squiddev.patcher.visitors.FindingVisitor;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.cctweaks.lua.launch.Launcher.*;

/**
 * Special launcher for CCEmuRedux
 */
public class CCEmuRedux {
	public static void main(String[] args) throws Exception {
		Integer width = parseNumber("cctweaks.ccemu.width");
		Integer height = parseNumber("cctweaks.ccemu.height");

		Integer back = parseHexColor("cctweaks.ccemu.color");

		RewritingLoader loader = setupLoader();

		if (width != null || height != null) {
			loader.chain().add(new SizePatcher(
				width == null ? 51 : width,
				height == null ? 19 : height
			));
		}

		if (back != null) {
			int val = back;
			loader.chain().add(new ColorPatcher(
				(val >> (8 * 2) & 255) / 255.0f,
				(val >> (8 * 1) & 255) / 255.0f,
				(val >> (8 * 0) & 255) / 255.0f
			));
		}

		loader.chain().finalise();
		execute(loader, "com.xtansia.ccemu.desktop.DesktopLauncher", new String[0]);
	}

	private static Integer parseHexColor(String key) {
		String value = System.getProperty(key);
		if (value == null) return null;

		if (value.charAt(0) == '#') value = value.substring(1);

		if (value.length() == 3) {
			value = "" +
				value.charAt(0) + value.charAt(0) +
				value.charAt(1) + value.charAt(1) +
				value.charAt(2) + value.charAt(2);
		} else if (value.length() != 6) {
			throw new NumberFormatException("Cannot parse " + key + ": " + "expected string of length 3 or 6, got " + value.length());
		}

		try {
			return Integer.valueOf(value, 16);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Cannot parse " + key + ": " + e.getMessage());
		}
	}

	private static class SizePatcher implements IPatcher {
		private final int width;
		private final int height;

		public SizePatcher(int width, int height) {
			this.width = width;
			this.height = height;
		}

		@Override
		public boolean matches(String className) {
			return className.equals("com.xtansia.ccemu.computercraft.computer.ComputerCraftComputer");
		}

		@Override
		public ClassVisitor patch(String className, ClassVisitor delegate) throws Exception {
			return new FindingVisitor(delegate,
				new VarInsnNode(ALOAD, 3),
				new MethodInsnNode(INVOKEVIRTUAL, "com/xtansia/ccemu/api/computer/ComputerType", "getTerminalWidth", "()I", false),
				new VarInsnNode(ALOAD, 3),
				new MethodInsnNode(INVOKEVIRTUAL, "com/xtansia/ccemu/api/computer/ComputerType", "getTerminalHeight", "()I", false)
			) {
				@Override
				public void handle(InsnList nodes, MethodVisitor visitor) {
					AsmUtils.constantOpcode(visitor, width);
					AsmUtils.constantOpcode(visitor, height);
				}
			}.onMethod("<init>").once().mustFind();
		}
	}

	private static class ColorPatcher implements IPatcher {
		private final float r;
		private final float g;
		private final float b;

		private ColorPatcher(float r, float g, float b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public boolean matches(String className) {
			return className.equals("com.xtansia.ccemu.CCEmulator");
		}

		@Override
		public ClassVisitor patch(String className, ClassVisitor delegate) throws Exception {
			return new FindingVisitor(delegate,
				new LdcInsnNode(0.87f),
				new LdcInsnNode(0.87f),
				new LdcInsnNode(0.87f),
				new InsnNode(FCONST_1),
				new MethodInsnNode(INVOKEINTERFACE, "com/badlogic/gdx/graphics/GLCommon", "glClearColor", "(FFFF)V", true)
			) {
				@Override
				public void handle(InsnList nodes, MethodVisitor visitor) {
					visitor.visitLdcInsn(r);
					visitor.visitLdcInsn(g);
					visitor.visitLdcInsn(b);
					visitor.visitInsn(FCONST_1);
					visitor.visitMethodInsn(INVOKEINTERFACE, "com/badlogic/gdx/graphics/GLCommon", "glClearColor", "(FFFF)V", true);
				}
			}.onMethod("draw").once().mustFind();
		}
	}

}
