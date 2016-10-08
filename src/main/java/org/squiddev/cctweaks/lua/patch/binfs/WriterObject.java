package org.squiddev.cctweaks.lua.patch.binfs;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import org.squiddev.cctweaks.api.lua.IArguments;
import org.squiddev.cctweaks.api.lua.ILuaObjectWithArguments;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.lib.BinaryConverter;
import org.squiddev.patcher.visitors.MergeVisitor;

import java.io.IOException;

/**
 * Basic file objects
 */
@MergeVisitor.Rewrite
@MergeVisitor.Rename(
	from = "org/squiddev/cctweaks/lua/patch/binfs/INormalFile",
	to = "dan200/computercraft/core/filesystem/IMountedFileNormal"
)
public class WriterObject implements ILuaObjectWithArguments, IMethodDescriptor {
	private final INormalFile stream;

	public WriterObject(INormalFile stream) {
		this.stream = stream;
	}

	@Override
	public String[] getMethodNames() {
		return new String[]{"write", "writeLine", "close", "flush"};
	}

	private void write(Object[] args, boolean newLine) throws LuaException {
		byte[] result;
		if (args.length > 0 && args[0] != null) {
			result = args[0] instanceof byte[] ? (byte[]) args[0] : BinaryConverter.toBytes(args[0].toString());
		} else {
			result = new byte[0];
		}

		try {
			stream.write(result, 0, result.length, newLine);
		} catch (IOException var8) {
			throw new LuaException(var8.getMessage());
		}
	}

	@Override
	public Object[] callMethod(ILuaContext context, int method, Object[] args) throws LuaException {
		switch (method) {
			case 0: {
				write(args, false);
				return null;
			}
			case 1:
				write(args, true);
				return null;
			case 2:
				try {
					stream.close();
					return null;
				} catch (IOException ignored) {
					return null;
				}
			case 3:
				try {
					stream.flush();
					return null;
				} catch (IOException ignored) {
					return null;
				}
			default:
				return null;
		}
	}

	@Override
	public Object[] callMethod(ILuaContext context, int method, IArguments arguments) throws LuaException, InterruptedException {
		return callMethod(context, method, arguments.asBinary());
	}

	@Override
	public boolean willYield(int method) {
		return false;
	}
}
