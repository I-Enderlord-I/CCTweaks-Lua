package org.squiddev.cctweaks.lua.lib;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import org.squiddev.cctweaks.api.lua.IExtendedComputerAccess;
import org.squiddev.cctweaks.api.lua.ILuaAPI;
import org.squiddev.cctweaks.api.lua.ILuaAPIFactory;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.Config;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static org.squiddev.cctweaks.lua.lib.ArgumentHelper.getString;

/**
 * Adds inflate/deflate APIs
 */
public class DataAPI implements ILuaAPI, ILuaAPIFactory, IMethodDescriptor {
	@Override
	public void startup() {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void advance(double timestep) {
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		return new String[]{"inflate", "deflate"};
	}

	@Override
	public ILuaAPI create(@Nonnull IExtendedComputerAccess computer) {
		return Config.APIs.Data.enabled ? this : null;
	}

	@Nonnull
	@Override
	public String[] getNames() {
		return new String[]{"data"};
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
		switch (method) {
			case 0:
				return inflate(BinaryConverter.toBytes(getString(args, 0)));

			case 1:
				return deflate(BinaryConverter.toBytes(getString(args, 0)));
		}

		return null;
	}

	private Object[] inflate(byte[] data) throws LuaException {
		if (data.length >= Config.APIs.Data.limit) throw new LuaException("Data is too long");

		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		InflaterOutputStream inos = new InflaterOutputStream(baos, new Inflater(true));
		try {
			inos.write(data);
			inos.finish();
		} catch (IOException e) {
			throw LuaHelpers.rewriteException(e, "Inflating error");
		}

		return new Object[]{baos.toByteArray()};
	}

	private Object[] deflate(byte[] data) throws LuaException {
		if (data.length >= Config.APIs.Data.limit) throw new LuaException("Data is too long");

		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		DeflaterOutputStream inos = new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
		try {
			inos.write(data);
			inos.finish();
		} catch (IOException e) {
			throw LuaHelpers.rewriteException(e, "Deflating error");
		}

		return new Object[]{baos.toByteArray()};
	}

	@Override
	public boolean willYield(int method) {
		return false;
	}
}
