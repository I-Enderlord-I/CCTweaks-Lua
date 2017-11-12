package org.squiddev.cctweaks.lua.launch;

import java.util.Arrays;

/**
 * The main launcher
 */
public class Launcher {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Expected main class");
			System.exit(1);
		}

		RewritingLoader loader = setupLoader();
		loader.chain().finalise();
		execute(loader, args[0], Arrays.copyOfRange(args, 1, args.length));
	}

	public static RewritingLoader setupLoader() throws Exception {
		DelegatingRewritingLoader classLoader = new DelegatingRewritingLoader(ClassLoader.getSystemClassLoader());
		Thread.currentThread().setContextClassLoader(classLoader);

		ClassLoaderHelpers.loadPropertyConfig(classLoader);
		ClassLoaderHelpers.syncDump(classLoader);
		ClassLoaderHelpers.setupChain(classLoader);

		return classLoader;
	}

	public static void execute(RewritingLoader loader, String className, String[] arguments) throws Exception {
		ClassLoader classLoader = (ClassLoader) loader;
		Class<?> api = classLoader.loadClass("org.squiddev.cctweaks.lua.lib.ApiRegister");
		api
			.getMethod("init")
			.invoke(null);

		api
			.getMethod("loadPlugins")
			.invoke(null);

		classLoader.loadClass(className)
			.getMethod("main", String[].class)
			.invoke(null, new Object[]{arguments});
	}

	public static Integer parseNumber(String key) {
		String value = System.getProperty(key);
		if (value == null) return null;

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Cannot parse " + key + ": " + e.getMessage());
		}
	}
}
