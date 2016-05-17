package org.squiddev.cctweaks.lua.launch;

import org.squiddev.cctweaks.lua.StreamHelpers;
import org.squiddev.cctweaks.lua.asm.CustomChain;
import org.squiddev.patcher.Logger;

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

	public final CustomChain chain = new CustomChain();

	private Set<String> classLoaderExceptions = new HashSet<String>();
	private final File dumpFolder = new File("asm/cctweaks");
	private boolean dumpAsm;

	public RewritingLoader(URL[] urls) {
		super(urls, null);

		// classloader exclusions
		addClassLoaderExclusion("java.");
		addClassLoaderExclusion("sun.");
		addClassLoaderExclusion("org.objectweb.asm.");
		addClassLoaderExclusion("org.squiddev.patcher.");
		addClassLoaderExclusion("com.google.");

		addClassLoaderExclusion("org.squiddev.cctweaks.lua.StreamHelpers");
		addClassLoaderExclusion("org.squiddev.cctweaks.lua.launch.");
		addClassLoaderExclusion("org.squiddev.cctweaks.lua.asm.");
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
					} catch (IOException e) {
						Logger.error("Cannot write " + file, e);
					} finally {
						stream.close();
					}
				} catch (FileNotFoundException e) {
					Logger.error("Cannot write " + file, e);
				} catch (IOException e) {
					Logger.error("Cannot write " + file, e);
				}
			} else {
				Logger.warn("Cannot create folder for " + file);
			}
		}
	}
}
