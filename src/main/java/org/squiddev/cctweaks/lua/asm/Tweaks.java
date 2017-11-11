package org.squiddev.cctweaks.lua.asm;

import org.squiddev.cctweaks.lua.TweaksLogger;
import org.squiddev.patcher.transformer.*;

/**
 * Setup everything
 */
public class Tweaks {
	public static void setup(TransformationChain chain) {
		chain.add(new ClassReplaceSource(TweaksLogger.instance, "org.luaj.vm2.lib.DebugLib"));
		chain.add(new ClassReplaceSource(TweaksLogger.instance, "org.luaj.vm2.lib.StringLib"));

		chain.add(new AddMethodDescriptor());
		chain.add(new CustomAPIs());
		chain.add(new CustomBios());
		chain.add(new CustomMachine());
		addMulti(chain, new CustomThreading());
		chain.add(new CustomTimeout());
		chain.add(new WhitelistDebug());

		chain.add(new ClassMerger(TweaksLogger.instance,
			"dan200.computercraft.core.computer.Computer",
			"org.squiddev.cctweaks.lua.patch.Computer_Patch"
		));
	}

	private static void addMulti(TransformationChain chain, ClassReplacer replacer) {
		chain.add((IPatcher) replacer);
		chain.add((ISource) replacer);
	}
}
