package org.squiddev.cctweaks.lua.lib.fs;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.filesystem.IMountedFile;
import org.squiddev.cctweaks.api.lua.IArguments;
import org.squiddev.cctweaks.api.lua.ILuaObjectWithArguments;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.patch.iface.MountedNormalFilePatched;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Basic file objects
 */
public class ReaderObject implements ILuaObjectWithArguments, IMethodDescriptor {
	private final MountedNormalFilePatched stream;
	private boolean open = true;

	public ReaderObject(IMountedFile stream) {
		this.stream = (MountedNormalFilePatched) stream;
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		return new String[]{"readLine", "readAll", "close"};
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
		switch (method) {
			case 0:
				//readLine
				if (!open) throw new LuaException("attempt to use a closed file");
				try {
					byte[] result = stream.readLineByte();
					if (result != null) return new Object[]{result};
					return null;
				} catch (IOException ignored) {
				}
				return null;
			case 1:
				// readAll
				if (!open) throw new LuaException("attempt to use a closed file");
				try {
					byte[] result = stream.readAllByte();
					if (result != null) return new Object[]{result};
					return null;
				} catch (IOException ignored) {
				}
				return null;
			case 2:
				// close
				try {
					stream.close();
					open = false;
				} catch (IOException ignored) {
				}
				return null;
			default:
				return null;
		}
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull IArguments arguments) throws LuaException, InterruptedException {
		return callMethod(context, method, arguments.asBinary());
	}

	@Override
	public boolean willYield(int method) {
		return false;
	}
}
