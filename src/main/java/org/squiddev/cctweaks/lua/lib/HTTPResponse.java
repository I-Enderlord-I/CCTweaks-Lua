package org.squiddev.cctweaks.lua.lib;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;

public class HTTPResponse implements ILuaObject, IMethodDescriptor {
	private final int responseCode;
	private final byte[] result;
	private final Map<String, String> headers;
	private int index = 0;
	private boolean open = true;

	public HTTPResponse(int responseCode, byte[] result, Map<String, String> headers) {
		this.responseCode = responseCode;
		this.result = result;
		this.headers = headers;
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		return new String[]{"readLine", "readAll", "read", "close", "getResponseCode", "getResponseHeaders"};
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
		switch (method) {
			case 0: {
				// readLine
				if (!open) throw new LuaException("attempt to use a closed response");
				// We have these as separate methods to ensure select('#', ...) works
				if (index >= result.length) return new Object[1];

				int start = index, end = -1, newIndex = -1;

				for (int i = start; i < result.length; i++) {
					// Yeah. Kinda ugly
					if (result[i] == '\r') {
						if (i + 1 < result.length && result[i + 1] == '\n') {
							end = i;
							newIndex = i + 2;
							break;
						} else {
							end = i;
							newIndex = i + 1;
							break;
						}
					} else if (result[i] == '\n') {
						end = i;
						newIndex = i + 1;
						break;
					}
				}

				if (end == -1) {
					// If we read until the end
					end = index = result.length;
				} else {
					index = newIndex;
				}

				// If we were at the end of the line then return empty string
				if (end < start) return new Object[]{""};
				return new Object[]{Arrays.copyOfRange(result, start, end)};
			}
			case 1:
				// readLine
				if (!open) throw new LuaException("attempt to use a closed response");
				if (index >= result.length) return new Object[]{""};

				int start = index;
				int end = result.length;
				index = end;

				return new Object[]{Arrays.copyOfRange(result, start, end)};
			case 2: {
				// read
				if (!open) throw new LuaException("attempt to use a closed response");
				if (index >= result.length) return new Object[1];

				byte character = result[index];
				index++;
				return new Object[]{character};
			}
			case 3:
				open = false;
				break;
			case 4:
				return new Object[]{responseCode};
			case 5:
				return new Object[]{headers};
		}

		return null;
	}

	@Override
	public boolean willYield(int method) {
		return false;
	}
}
