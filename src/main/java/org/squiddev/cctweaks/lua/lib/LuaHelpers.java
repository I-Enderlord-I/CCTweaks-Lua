package org.squiddev.cctweaks.lua.lib;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.ClassVisitor;

/**
 * Various classes for helping with Lua conversion
 */
public class LuaHelpers {
	/**
	 * Wraps an exception, defaulting to another string on an empty message
	 *
	 * @param e   The exception to wrap
	 * @param def The default message
	 * @return The created exception
	 */
	public static LuaException rewriteException(Throwable e, String def) {
		String message = e.getMessage();
		return new LuaException((message == null || message.isEmpty()) ? def : message);
	}

	/**
	 * Wraps an exception, including its type
	 *
	 * @param e The exception to wrap
	 * @return The created exception
	 */
	public static LuaException rewriteWholeException(Throwable e) {
		return e instanceof LuaException ? (LuaException) e : new LuaException(e.toString());
	}
}
