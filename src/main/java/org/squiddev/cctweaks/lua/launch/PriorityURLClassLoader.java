package org.squiddev.cctweaks.lua.launch;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * A classloader which places a series of URLs *before* the parent class loader.
 */
public class PriorityURLClassLoader extends URLClassLoader {
	private final WrapperClassLoader parent;

	public PriorityURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, null);
		this.parent = new WrapperClassLoader(parent);
	}

	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			// First, check if the class has already been loaded
			Class<?> c = findLoadedClass(name);
			if (c == null) {
				try {
					c = findClass(name);
				} catch (ClassNotFoundException e) {
					// ClassNotFoundException thrown if class not found
					// from the this class loader
				}

				if (c == null) {
					c = parent.loadClass(name, false);
				}
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}
	}

	public URL getResource(String name) {
		URL url = findResource(name);
		if (url == null) url = parent.getResource(name);
		return url;
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		final Enumeration<URL> mine = findResources(name);
		final Enumeration<URL> parents = parent.getResources(name);

		return new Enumeration<URL>() {
			@Override
			public boolean hasMoreElements() {
				return mine.hasMoreElements() || parents.hasMoreElements();
			}

			@Override
			public URL nextElement() {
				return mine.hasMoreElements() ? mine.nextElement() : parents.nextElement();
			}
		};
	}

	private static class WrapperClassLoader extends ClassLoader {
		WrapperClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			return super.loadClass(name, resolve);
		}
	}
}
