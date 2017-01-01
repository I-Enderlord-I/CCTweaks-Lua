package org.squiddev.cctweaks.lua.lib.cobalt;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileSystemException;
import org.squiddev.cctweaks.lua.patch.IFileSystemPatched;
import org.squiddev.cobalt.lib.platform.AbstractResourceManipulator;
import org.squiddev.cobalt.lib.profiler.ProfilerLib;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CobaltResourceProvider extends AbstractResourceManipulator implements ProfilerLib.OutputProvider {
	private final Computer computer;
	private IFileSystemPatched fileSystem;

	public CobaltResourceProvider(Computer computer) {
		this.computer = computer;
	}

	private IFileSystemPatched getFileSystem() {
		if (fileSystem != null) return fileSystem;
		return fileSystem = (IFileSystemPatched) computer.getAPIEnvironment().getFileSystem();
	}

	@Override
	public InputStream findResource(String path) {
		try {
			return getFileSystem().openForReadStream(path);
		} catch (FileSystemException e) {
			return null;
		}
	}

	@Override
	public void rename(String from, String to) throws IOException {
		try {
			getFileSystem().move(from, to);
		} catch (FileSystemException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public void remove(String file) throws IOException {
		try {
			getFileSystem().delete(file);
		} catch (FileSystemException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public DataOutputStream createWriter(String path) throws IOException {
		try {
			return new DataOutputStream(getFileSystem().openForWriteStream(path));
		} catch (FileSystemException e) {
			throw new IOException(e.getMessage());
		}
	}
}
