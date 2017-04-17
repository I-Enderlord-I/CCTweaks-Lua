package org.squiddev.cctweaks.lua.lib;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.ILuaMachine;
import org.squiddev.cctweaks.api.lua.*;
import org.squiddev.cctweaks.lua.Config;
import org.squiddev.patcher.Logger;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LuaEnvironment implements ILuaEnvironment {
	private final ILuaTask sleepTask = new ILuaTask() {
		@Override
		public Object[] execute() throws LuaException {
			return null;
		}
	};
	/**
	 * The instance of the Lua environment - this exists as ASM is easier this way
	 */
	public static final LuaEnvironment instance = new LuaEnvironment();

	private final Set<ILuaAPIFactory> apis = Sets.newHashSet();
	private final Map<String, ILuaMachineFactory<?>> machines = Maps.newHashMap();

	private LuaEnvironment() {
	}

	@Override
	public void registerAPI(ILuaAPIFactory factory) {
		Preconditions.checkNotNull("factory cannot be null");
		apis.add(factory);
	}

	@Override
	public void registerMachine(ILuaMachineFactory<?> factory) {
		if (factory == null) throw new IllegalArgumentException("factory cannot be null");
		machines.put(factory.getID(), factory);
	}

	@Override
	public long issueTask(IComputerAccess access, ILuaTask task, int delay) throws LuaException {
		long id = DelayedTasks.getNextId();
		if (!DelayedTasks.addTask(access, task, delay, id)) throw new LuaException("Too many tasks");

		return id;
	}

	@Override
	public Object[] executeTask(IComputerAccess access, ILuaContext context, ILuaTask task, int delay) throws LuaException, InterruptedException {
		long id = issueTask(access, task, delay);

		Object[] response;
		try {
			do {
				response = context.pullEvent(ILuaEnvironment.EVENT_NAME);
			}
			while (response.length < 3 || !(response[1] instanceof Number) || !(response[2] instanceof Boolean) || (long) ((Number) response[1]).intValue() != id);
		} catch (InterruptedException e) {
			DelayedTasks.cancel(id);
			throw e;
		} catch (LuaException e) {
			DelayedTasks.cancel(id);
			throw e;
		}

		Object[] returnValues = new Object[response.length - 3];
		if (!(Boolean) response[2]) {
			if (response.length >= 4 && response[3] instanceof String) {
				throw new LuaException((String) response[3]);
			} else {
				throw new LuaException();
			}
		} else {
			System.arraycopy(response, 3, returnValues, 0, returnValues.length);
			return returnValues;
		}
	}

	@Override
	public void sleep(IComputerAccess access, ILuaContext context, int delay) throws LuaException, InterruptedException {
		executeTask(access, context, sleepTask, delay);
	}

	public static void inject(Computer computer) {
		if (instance.apis.size() == 0) return;

		IExtendedComputerAccess access = new ComputerAccess(computer);
		for (ILuaAPIFactory factory : instance.apis) {
			org.squiddev.cctweaks.api.lua.ILuaAPI api = factory.create(access);
			if (api != null) {
				computer.addAPI(new LuaAPI(api, factory));
			}
		}
	}

	/**
	 * Get the currently selected machine
	 *
	 * @return The currently selected VM. May be {@code null} if it doesn't exist.
	 */
	public static ILuaMachineFactory<?> getCurrentMachine() {
		return instance.machines.get(Config.Computer.runtime);
	}

	/**
	 * Get the machine which will be used
	 *
	 * @return The machine which will be used
	 */
	public static ILuaMachineFactory<?> getUsedMachine() {
		ILuaMachineFactory<?> factory = instance.machines.get(Config.Computer.runtime);
		if (factory == null) {
			Logger.warn("Unknown runtime '" + Config.Computer.runtime + "', falling back to luaj");
			factory = instance.machines.get("luaj");

			if (factory == null) throw new IllegalStateException("Cannot find any runtime");
		}

		return factory;
	}

	public static ILuaMachine createMachine(Computer computer) {
		ILuaMachineFactory<?> factory = getUsedMachine();
		IExtendedLuaMachine machine = factory.create(computer);

		machine.setGlobal("_HOST", computer.getAPIEnvironment().getComputerEnvironment().getHostString());
		machine.setGlobal("_CC_DEFAULT_SETTINGS", ComputerCraft.default_computer_settings);
		machine.setGlobal("_RUNTIME", factory.getID());
		if (ComputerCraft.disable_lua51_features) {
			machine.setGlobal("_CC_DISABLE_LUA51_FEATURES", true);
		}


		return machine;
	}

	/**
	 * Get the pre-bios path to use
	 *
	 * @return The pre-bios path to use
	 * @see ILuaMachineFactory#getPreBios()
	 */
	public static String getPreBios() {
		ILuaMachineFactory<?> factory = getUsedMachine();
		String preBios = Config.Computer.preBiosPath;
		if (preBios != null && !preBios.isEmpty()) return preBios;

		try {
			preBios = factory.getPreBios();
		} catch (AbstractMethodError ignored) {
		}

		return preBios == null ? ILuaMachineFactory.PRE_BIOS_STRING : preBios;
	}

	private static class LuaAPI implements ILuaAPI, ILuaObjectWithArguments, IExtendedLuaObject, IMethodDescriptor {
		private final org.squiddev.cctweaks.api.lua.ILuaAPI api;
		private final ILuaAPIFactory factory;

		private LuaAPI(org.squiddev.cctweaks.api.lua.ILuaAPI api, ILuaAPIFactory factory) {
			this.api = api;
			this.factory = factory;
		}

		@Override
		public String[] getNames() {
			return factory.getNames();
		}

		@Override
		public void startup() {
			api.startup();
		}

		@Override
		public void advance(double v) {
			api.advance(v);
		}

		@Override
		public void shutdown() {
			api.shutdown();
		}

		@Override
		public String[] getMethodNames() {
			return api.getMethodNames();
		}

		@Override
		public Object[] callMethod(ILuaContext context, int method, Object[] args) throws LuaException, InterruptedException {
			return api.callMethod(context, method, args);
		}

		@Override
		public Object[] callMethod(ILuaContext context, int method, IArguments arguments) throws LuaException, InterruptedException {
			return ArgumentDelegator.delegateLuaObject(api, context, method, arguments);
		}

		@Override
		public Map<Object, Object> getAdditionalData() {
			return api instanceof IExtendedLuaObject ? ((IExtendedLuaObject) api).getAdditionalData() : Collections.emptyMap();
		}

		@Override
		public boolean willYield(int method) {
			return !(api instanceof IMethodDescriptor) || ((IMethodDescriptor) api).willYield(method);
		}
	}

	private static final class ComputerAccess implements IExtendedComputerAccess {
		private final IAPIEnvironment environment;
		private final Computer computer;
		private final Set<String> mounts = new HashSet<String>();
		private FileSystem fs;

		private ComputerAccess(Computer computer) {
			this.computer = computer;
			this.environment = computer.getAPIEnvironment();
		}

		private FileSystem getFs() {
			if (fs == null) fs = environment.getFileSystem();
			return fs;
		}


		private String findFreeLocation(String desiredLoc) {
			try {
				synchronized (getFs()) {
					return !fs.exists(desiredLoc) ? desiredLoc : null;
				}
			} catch (FileSystemException ignored) {
				return null;
			}
		}

		@Override
		public String mount(String desiredLoc, IMount mount) {
			return mount(desiredLoc, mount, desiredLoc);
		}

		@Override
		public synchronized String mount(String desiredLoc, IMount mount, String driveName) {
			synchronized (getFs()) {
				String location = findFreeLocation(desiredLoc);
				if (location != null) {
					try {
						getFs().mount(driveName, location, mount);
					} catch (FileSystemException ignored) {
					}

					mounts.add(location);
				}

				return location;
			}
		}

		@Override
		public String mountWritable(String desiredLoc, IWritableMount mount) {
			return this.mountWritable(desiredLoc, mount, desiredLoc);
		}

		@Override
		public synchronized String mountWritable(String desiredLoc, IWritableMount mount, String driveName) {
			synchronized (getFs()) {
				String location = findFreeLocation(desiredLoc);
				if (location != null) {
					try {
						getFs().mountWritable(driveName, location, mount);
					} catch (FileSystemException ignored) {
					}

					mounts.add(location);
				}

				return location;
			}
		}

		@Override
		public synchronized void unmount(String location) {
			if (location != null) {
				if (!mounts.contains(location)) {
					throw new RuntimeException("You didn\'t mount this location");
				}

				getFs().unmount(location);
				mounts.remove(location);
			}
		}

		@Override
		public int getID() {
			return environment.getComputerID();
		}

		@Override
		public void queueEvent(String s, Object[] objects) {
			environment.queueEvent(s, objects);
		}

		@Override
		public String getAttachmentName() {
			return environment.getLabel();
		}

		@Override
		public File getRootMountPath() {
			IWritableMount mount = computer.getRootMount();
			if (mount instanceof FileMount) {
				return ((FileMount) mount).getRealPath("");
			} else {
				return null;
			}
		}

		@Override
		public IWritableMount getRootMount() {
			return computer.getRootMount();
		}
	}
}
