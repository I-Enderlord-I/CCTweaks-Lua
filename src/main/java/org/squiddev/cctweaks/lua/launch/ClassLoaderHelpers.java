package org.squiddev.cctweaks.lua.launch;

import org.squiddev.cctweaks.lua.StreamHelpers;
import org.squiddev.cctweaks.lua.TweaksLogger;
import org.squiddev.patcher.transformer.TransformationChain;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.jar.JarFile;

public final class ClassLoaderHelpers {
	private ClassLoaderHelpers() {
	}

	/**
	 * Add the default list of classloader exclusions to a rewriting loader
	 *
	 * @param loader The loader to register with.
	 */
	public static void setupExclusions(RewritingLoader loader) {
		// Java/JDK internals
		loader.addClassLoaderExclusion("com.sun.");
		loader.addClassLoaderExclusion("java.");
		loader.addClassLoaderExclusion("javax.");
		loader.addClassLoaderExclusion("jdk.");
		loader.addClassLoaderExclusion("org.w3c.dom.");
		loader.addClassLoaderExclusion("org.xml.sax.");
		loader.addClassLoaderExclusion("sun.");

		// Common external libraries
		loader.addClassLoaderExclusion("com.google.");
		loader.addClassLoaderExclusion("org.objectweb.asm.");
		loader.addClassLoaderExclusion("org.squiddev.patcher.");

		// CCTweaks internals
		loader.addClassLoaderExclusion("org.squiddev.cctweaks.lua.StreamHelpers");
		loader.addClassLoaderExclusion("org.squiddev.cctweaks.lua.launch.");
	}

	/**
	 * Setup the default transformation chain.
	 *
	 * @param loader The loader to register modifiers with.
	 */
	public static <T extends ClassLoader & RewritingLoader> void setupChain(T loader) throws Exception {
		loader.loadClass("org.squiddev.cctweaks.lua.asm.Tweaks")
			.getMethod("setup", TransformationChain.class)
			.invoke(null, loader.chain());
	}

	/**
	 * Sync {@link RewritingLoader#dump(boolean)} with the given config file.
	 *
	 * @param loader The loader to sync within
	 */
	public static <T extends ClassLoader & RewritingLoader> void syncDump(T loader) throws Exception {
		loader.dump(
			(Boolean) loader.loadClass("org.squiddev.cctweaks.lua.Config$Testing")
				.getField("dumpAsm")
				.get(null));
	}

	/**
	 * Load config entries using the property loader
	 *
	 * @param loader The loader to load within
	 */
	public static void loadPropertyConfig(ClassLoader loader) throws Exception {
		loader.loadClass("org.squiddev.cctweaks.lua.ConfigPropertyLoader")
			.getMethod("init")
			.invoke(null);
	}

	static CodeSource findSource(ClassLoader loader, String name) {
		String fileName = name.replace('.', '/') + ".class";
		URL url = loader.getResource(fileName);
		if (url == null) return null;

		CodeSigner[] signers = null;
		if (name.lastIndexOf('.') > -1) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection) {
					JarURLConnection jarConnection = (JarURLConnection) connection;
					url = jarConnection.getJarFileURL();

					JarFile jarFile = jarConnection.getJarFile();
					if (jarFile != null && jarFile.getManifest() != null) {
						signers = jarFile.getJarEntry(fileName).getCodeSigners();
					}
				}
			} catch (IOException e) {
				return null;
			}
		}

		return new CodeSource(url, signers);
	}

	static byte[] getClassBytes(ClassLoader loader, String fileName) throws IOException {
		InputStream classStream = null;
		try {
			classStream = loader.getResourceAsStream(fileName);
			return classStream == null ? null : StreamHelpers.toByteArray(classStream);
		} finally {
			if (classStream != null) {
				try {
					classStream.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	static void dump(File file, byte[] bytes) {
		File directory = file.getParentFile();
		if (directory.exists() || directory.mkdirs()) {
			try {
				OutputStream stream = new FileOutputStream(file);
				try {
					stream.write(bytes);
				} finally {
					stream.close();
				}
			} catch (FileNotFoundException e) {
				TweaksLogger.error("Cannot write " + file, e);
			} catch (IOException e) {
				TweaksLogger.error("Cannot write " + file, e);
			}
		} else {
			TweaksLogger.warn("Cannot create folder for " + file);
		}
	}
}
