package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaObject;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Extended functionality for Lua objects and APIs
 *
 * @deprecated Not currently supported.
 */
@Deprecated
public interface IExtendedLuaObject extends ILuaObject {
	/**
	 * Get additional fields to add to this API. These will be converted as normal.
	 *
	 * @return Additional fields. Do not return {@code null}
	 */
	@Nonnull
	Map<Object, Object> getAdditionalData();
}
