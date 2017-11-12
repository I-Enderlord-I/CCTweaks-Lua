package org.squiddev.cctweaks.lua.patcher;

import org.squiddev.cctweaks.lua.launch.RewritingLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Handles different versions
 */
public class VersionHandler {
	public static List<Object[]> getVersions() {
		return Arrays.asList(new Object[][]{
			new Object[]{"1.80pr0-build12"},
			new Object[]{"1.80pr1-build0"},
		});
	}

	public static class Runtime {
		public final String runtime;
		public final boolean multiThreading;
		public final boolean timeoutError;

		public Runtime(String runtime) {
			this(runtime, false, false);
		}

		public Runtime(String runtime, boolean multiThreading, boolean timeoutError) {
			this.runtime = runtime;
			this.multiThreading = multiThreading;
			this.timeoutError = timeoutError;
		}

		public void setup() {
			System.setProperty("cctweaks.Computer.runtime", runtime);
			System.setProperty("cctweaks.Computer.timeoutError", timeoutError ? "true" : "false");

			if (multiThreading) {
				System.setProperty("cctweaks.Computer.MultiThreading.enabled", "true");
				System.setProperty("cctweaks.Computer.MultiThreading.threads", "4");
			} else {
				System.setProperty("cctweaks.Computer.MultiThreading.enabled", "false");
			}
		}

		public void tearDown() {
			System.clearProperty("cctweaks.Computer.runtime");
			System.clearProperty("cctweaks.Computer.timeoutError");
			System.clearProperty("cctweaks.Computer.MultiThreading.enabled");
			System.clearProperty("cctweaks.Computer.MultiThreading.threads");
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(runtime);
			if (multiThreading && timeoutError) {
				builder.append(" (multi-threading, timeout error)");
			} else if (multiThreading) {
				builder.append(" (multi-threading)");
			} else if (timeoutError) {
				builder.append(" (timeout error)");
			}

			return builder.toString();
		}
	}

	private static final Runtime[] runtimes = new Runtime[]{
		new Runtime("luaj", false, false),

		new Runtime("cobalt", false, false),
		new Runtime("cobalt", false, true),
		new Runtime("cobalt", true, false),
		new Runtime("cobalt", true, true),
	};

	public static List<Object[]> getVersionsWithRuntimes() {
		List<Object[]> versions = getVersions();
		List<Object[]> withRuntimes = new ArrayList<Object[]>(versions.size() * runtimes.length);
		for (Object[] version : versions) {
			for (Runtime runtime : runtimes) {
				Object[] with = new Object[version.length + 1];
				System.arraycopy(version, 0, with, 0, version.length);
				with[with.length - 1] = runtime;
				withRuntimes.add(with);
			}
		}

		return withRuntimes;
	}

	public static ClassLoader getLatestLoader() throws Exception {
		List<Object[]> versions = getVersions();
		return getLoader((String) versions.get(versions.size() - 1)[0]);
	}

	public static ClassLoader getLoader(String version) throws Exception {
		URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		URL[] urls = loader.getURLs();

		File ccJar = new File("lib/ComputerCraft-" + version + ".jar").getAbsoluteFile();
		if (!ccJar.isFile()) throw new IllegalStateException(ccJar + " does not exist");

		// Build a class list without any other CC jar on the path
		List<URL> newUrls = new ArrayList<>(urls.length);
		for (URL url : urls) {
			String name = new File(url.getFile()).getName();
			if (!name.toLowerCase(Locale.ENGLISH).contains("computercraft")) {
				newUrls.add(url);
			}
		}
		newUrls.add(ccJar.toURI().toURL());

		System.setProperty("cctweaks.Testing.dumpAsm", "true");

		RewritingLoader newLoader = new RewritingLoader(
			newUrls.toArray(new URL[newUrls.size()]),
			new File("asm/cctweaks-" + version)
		);
		newLoader.addClassLoaderExclusion("org.junit.");
		newLoader.addClassLoaderExclusion("org.hamcrest.");
		newLoader.loadConfig();
		newLoader.loadChain();
		newLoader.loadClass("org.squiddev.cctweaks.lua.lib.ApiRegister")
			.getMethod("init")
			.invoke(null);
		newLoader.chain.finalise();
		return newLoader;
	}

	@SuppressWarnings("unchecked")
	public static <T> T runClass(ClassLoader loader, String source, String name) throws Throwable {
		source = "org.squiddev.cctweaks.lua.patcher." + source;
		Class<?> runner = loader.loadClass(source);
		try {
			return (T) runner.getMethod(name).invoke(null);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static void run(ClassLoader loader, String source) throws Throwable {
		run(loader, source, -1);
	}

	public static void run(ClassLoader loader, String source, int timeout) throws Throwable {
		Class<?> runner = loader.loadClass("org.squiddev.cctweaks.lua.patcher.runner.RunOnComputer");
		try {
			runner.getMethod("run", String.class, int.class).invoke(null, source, timeout);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static void runFile(ClassLoader loader, String source) throws Throwable {
		runFile(loader, source, -1);
	}

	public static void runFile(ClassLoader loader, String source, int timeout) throws Throwable {
		Class<?> runner = loader.loadClass("org.squiddev.cctweaks.lua.patcher.runner.RunOnComputer");
		try {
			Scanner s = new Scanner(loader.getResourceAsStream(("org/squiddev/cctweaks/lua/patcher/" + source + ".lua"))).useDelimiter("\\A");
			runner.getMethod("run", String.class, int.class).invoke(null, s.hasNext() ? s.next() : "", timeout);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
}
