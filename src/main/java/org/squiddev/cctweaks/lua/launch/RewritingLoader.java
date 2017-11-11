package org.squiddev.cctweaks.lua.launch;

import org.squiddev.cctweaks.lua.StreamHelpers;
import org.squiddev.cctweaks.lua.TweaksLogger;
import org.squiddev.patcher.transformer.TransformationChain;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Loads classes, rewriting them
 */
public class RewritingLoader extends URLClassLoader {
	private ClassLoader parent = getClass().getClassLoader();

	public final TransformationChain chain = new TransformationChain();

	private Set<String> classLoaderExceptions = new HashSet<String>();
	private final File dumpFolder;
	private boolean dumpAsm;

	public RewritingLoader(URL[] urls) {
		this(urls, new File("asm/cctweaks"));
	}

	public RewritingLoader(URL[] urls, File folder) {
		super(urls, null);

		dumpFolder = folder;

		// classloader exclusions
		addClassLoaderExclusion("java.");
		addClassLoaderExclusion("sun.");
		addClassLoaderExclusion("org.objectweb.asm.");
		addClassLoaderExclusion("org.squiddev.patcher.");
		addClassLoaderExclusion("com.google.");

		addClassLoaderExclusion("org.squiddev.cctweaks.lua.StreamHelpers");
		addClassLoaderExclusion("org.squiddev.cctweaks.lua.launch.");
		addClassLoaderExclusion("org.squiddev.cctweaks.lua.asm.CustomChain");
	}

	@Override
	public Class<?> findClass(final String name) throws ClassNotFoundException {
		for (final String exception : classLoaderExceptions) {
			if (name.startsWith(exception)) {
				return parent.loadClass(name);
			}
		}

		try {
			final int lastDot = name.lastIndexOf('.');
			final String fileName = name.replace('.', '/') + ".class";
			URLConnection urlConnection = findCodeSourceConnectionFor(fileName);

			CodeSigner[] signers = null;
			if (lastDot > -1) {
				if (urlConnection instanceof JarURLConnection) {
					final JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
					final JarFile jarFile = jarURLConnection.getJarFile();

					if (jarFile != null && jarFile.getManifest() != null) {
						signers = jarFile.getJarEntry(fileName).getCodeSigners();
					}
				}
			}

			byte[] original = getClassBytes(fileName);
			byte[] transformed = chain.transform(name, original);
			if (transformed == null) throw new ClassNotFoundException(name);
			if (transformed != original) writeDump(fileName, transformed);

			CodeSource codeSource = null;
			if (urlConnection != null) {
				URL url = urlConnection.getURL();
				if (urlConnection instanceof JarURLConnection) {
					url = ((JarURLConnection) urlConnection).getJarFileURL();
				}

				codeSource = new CodeSource(url, signers);
			}

			return defineClass(name, transformed, 0, transformed.length, codeSource);
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (Throwable e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	private URLConnection findCodeSourceConnectionFor(final String name) {
		final URL resource = findResource(name);
		if (resource != null) {
			try {
				return resource.openConnection();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return null;
	}

	public void addClassLoaderExclusion(String toExclude) {
		classLoaderExceptions.add(toExclude);
	}

	public void loadChain() throws Exception {
		loadClass("org.squiddev.cctweaks.lua.asm.Tweaks")
			.getMethod("setup", TransformationChain.class)
			.invoke(null, chain);
	}

	public void loadConfig() throws Exception {
		loadClass("org.squiddev.cctweaks.lua.ConfigPropertyLoader")
			.getMethod("init")
			.invoke(null);

		dumpAsm = (Boolean) loadClass("org.squiddev.cctweaks.lua.Config$Testing")
			.getField("dumpAsm")
			.get(null);
	}

	private byte[] getClassBytes(String name) throws IOException {
		InputStream classStream = null;
		try {
			final URL classResource = findResource(name);
			if (classResource == null) return null;

			classStream = classResource.openStream();
			return StreamHelpers.toByteArray(classStream);
		} finally {
			if (classStream != null) {
				try {
					classStream.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void writeDump(String fileName, byte[] bytes) {
		if (dumpAsm) {
			File file = new File(dumpFolder, fileName);
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
}
