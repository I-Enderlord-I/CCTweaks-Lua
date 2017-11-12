package org.squiddev.cctweaks.lua.launch;

import org.squiddev.patcher.transformer.TransformationChain;

import javax.annotation.Nonnull;

/**
 * An abstract class loader which will modify a set of classes
 */
public interface RewritingLoader {
	/**
	 * Get the transformation chain with which to modify classes.
	 *
	 * @return The transformation chain to use.
	 */
	@Nonnull
	TransformationChain chain();

	/**
	 * Mark a class or package as excluded.
	 *
	 * Any class begining with {@code prefix} will be loaded using the
	 * parent class loader instead.
	 *
	 * @param prefix The prefix to ignore.
	 */
	void addClassLoaderExclusion(String prefix);

	/**
	 * Determine whether modified class files should be dumped when loaded
	 *
	 * @param on Whether files should be dumped
	 */
	void dump(boolean on);
}
