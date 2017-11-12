package org.squiddev.cctweaks.lua.launch;

import org.squiddev.patcher.transformer.TransformationChain;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * This class loader wraps another class loader, transforming its classes.
 *
 * When loading a class, we first attempt to use the parent class loader. This is just
 * the bootstrap loader in, so we will load all core-JDK classes as normal.
 *
 * Otherwise, we will determine whether a class is excluded by ({@link #addClassLoaderExclusion(String)}).
 * If so, we'll pass this off to the delegate class loader again.
 *
 * If the class is not blacklisted then we use the delegate class loader to load the bytes,
 * run the transformer on them, and load them into our own class loader.
 */
public class DelegatingRewritingLoader extends SecureClassLoader implements RewritingLoader {
	private final WrapperClassLoader delegateWrapper;
	private final ClassLoader delegate;
	private final TransformationChain chain = new TransformationChain();
	private final Set<String> classLoaderExceptions = new HashSet<String>();
	private final File dumpFolder;
	private boolean dumpAsm;

	public DelegatingRewritingLoader(ClassLoader delegate) {
		this(delegate, new File("asm/cctweaks"));
	}

	public DelegatingRewritingLoader(ClassLoader delegate, File dumpFolder) {
		super(null);
		this.delegate = delegate;
		this.delegateWrapper = new WrapperClassLoader(delegate);
		this.dumpFolder = dumpFolder;
		ClassLoaderHelpers.setupExclusions(this);
	}

	@Nonnull
	@Override
	public TransformationChain chain() {
		return chain;
	}

	@Override
	public void addClassLoaderExclusion(String toExclude) {
		classLoaderExceptions.add(toExclude);
	}

	@Override
	public void dump(boolean on) {
		dumpAsm = on;
	}

	@Override
	public Class<?> findClass(final String name) throws ClassNotFoundException {
		for (final String exception : classLoaderExceptions) {
			if (name.startsWith(exception)) {
				return delegate.loadClass(name);
			}
		}

		try {
			String fileName = name.replace('.', '/') + ".class";
			CodeSource codeSource = ClassLoaderHelpers.findSource(delegate, name);

			byte[] original = ClassLoaderHelpers.getClassBytes(delegate, fileName);
			byte[] transformed = chain.transform(name, original);
			if (transformed == null) throw new ClassNotFoundException(name);

			if (original != transformed && dumpAsm) {
				ClassLoaderHelpers.dump(new File(dumpFolder, fileName), transformed);
			}

			return defineClass(name, transformed, 0, transformed.length, codeSource);
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	@Override
	protected URL findResource(String name) {
		URL url = super.findResource(name);
		return url != null ? url : delegate.getResource(name);
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		final Enumeration<URL> myRes = super.findResources(name);
		final Enumeration<URL> delegateRes = delegate.getResources(name);

		return new Enumeration<URL>() {
			@Override
			public boolean hasMoreElements() {
				return myRes.hasMoreElements() || delegateRes.hasMoreElements();
			}

			@Override
			public URL nextElement() {
				return myRes.hasMoreElements() ? myRes.nextElement() : delegateRes.nextElement();
			}
		};
	}

	@Override
	@Deprecated
	protected Package getPackage(String name) {
		Package pck = super.getPackage(name);
		return pck != null ? pck : delegateWrapper.getPackage(name);
	}

	/**
	 * A simple class loader which allows us to access several protected methods
	 */
	private static class WrapperClassLoader extends ClassLoader {
		WrapperClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		@Deprecated
		public Package getPackage(String name) {
			return super.getPackage(name);
		}
	}
}
