package org.squiddev.cctweaks.lua.lib;

import dan200.computercraft.api.filesystem.IMount;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Variant on {@link dan200.computercraft.core.filesystem.FileMount} but
 * without any editing abilities or file size tracking.
 */
public final class ReadOnlyFileMount implements IMount {
	private final File rootPath;

	public ReadOnlyFileMount(File rootPath) {
		this.rootPath = rootPath;
	}

	private File getRealPath(String path) {
		return new File(rootPath, path);
	}

	private boolean created() {
		return rootPath.exists();
	}

	@Override
	public boolean exists(@Nonnull String path) throws IOException {
		if (created()) {
			return getRealPath(path).exists();
		} else {
			return path.length() == 0;
		}
	}

	@Override
	public boolean isDirectory(@Nonnull String path) throws IOException {
		if (created()) {
			File file = getRealPath(path);
			return file.exists() && file.isDirectory();
		} else {
			return path.length() == 0;
		}
	}

	@Override
	public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
		if (created()) {
			File file = getRealPath(path);
			if (!file.exists() || !file.isDirectory()) {
				throw new IOException("Not a directory");
			}

			String[] paths = file.list();
			if (paths != null) {
				for (String subPath : paths) {
					if (new File(file, subPath).exists()) contents.add(subPath);
				}
			}
		} else {
			if (path.length() != 0) {
				throw new IOException("Not a directory");
			}
		}
	}

	@Override
	public long getSize(@Nonnull String path) throws IOException {
		if (created()) {
			File file = getRealPath(path);
			if (file.exists()) return file.isDirectory() ? 0 : file.length();
		} else {
			if (path.length() == 0) return 0;
		}

		throw new IOException("No such file");
	}

	@Override
	public InputStream openForRead(@Nonnull String path) throws IOException {
		if (created()) {
			File file = getRealPath(path);
			if (file.exists() && !file.isDirectory()) {
				return new FileInputStream(file);
			}
		}

		throw new IOException("No such file");
	}
}
