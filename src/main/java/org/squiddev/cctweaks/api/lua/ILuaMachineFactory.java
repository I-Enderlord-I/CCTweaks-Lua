package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.core.computer.Computer;

import javax.annotation.Nonnull;

/**
 * Create a {@link IExtendedLuaMachine} for a computer;
 */
public interface ILuaMachineFactory<T extends IExtendedLuaMachine> {
	/**
	 * A pre-bios file which protects the string metatable from being accessed or mutated.
	 */
	String PRE_BIOS_STRING = "lua/prebios-string.lua";

	/**
	 * A pre-bios file which does not protect the string metatable.
	 */
	String PRE_BIOS = "lua/prebios.lua";


	/**
	 * Get the ID of this machine
	 *
	 * @return The ID of this machine
	 */
	@Nonnull
	String getID();

	/**
	 * Create a machine for a given computer
	 *
	 * @param computer The computer to create this for
	 * @return The created machine.
	 */
	@Nonnull
	T create(Computer computer);

	/**
	 * Check if multi-threading is supported for this runtime.
	 *
	 * @return Whether this machine supports multi-threading.
	 */
	boolean supportsMultithreading();

	/**
	 * Get the "pre-bios" path for this runtime.
	 *
	 * This is used when using custom ROMs in order to sandbox the environment. It should finish by loading and running
	 * run `rom/bios.lua".
	 *
	 * See {@link #PRE_BIOS} and {@link #PRE_BIOS_STRING}.
	 *
	 * @return The pre-bios path.
	 */
	@Nonnull
	String getPreBios();
}
