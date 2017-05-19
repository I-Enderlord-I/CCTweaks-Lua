package org.squiddev.cctweaks.lua.lib.cobalt;

import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import org.squiddev.cctweaks.api.lua.IExtendedLuaMachine;
import org.squiddev.cctweaks.lua.Config;
import org.squiddev.cctweaks.lua.TweaksLogger;
import org.squiddev.cctweaks.lua.lib.AbstractLuaContext;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.profiler.ProfilerLib;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.squiddev.cctweaks.lua.lib.LuaMachineHelpers.ILLEGAL_NAMES;
import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * An rewrite of the Lua machine using cobalt
 *
 * @see dan200.computercraft.core.lua.LuaJLuaMachine
 */
public class CobaltMachine extends AbstractLuaContext implements IExtendedLuaMachine {
	private final LuaState state;
	private final LuaTable globals;
	private LuaThread mainThread;

	private String eventFilter = null;
	private String hardAbort = null;
	private String softAbort = null;

	public CobaltMachine(final Computer computer) {
		super(computer);

		CobaltResourceProvider resources = new CobaltResourceProvider(computer);
		final LuaState state = this.state = new LuaState(resources);

		state.debug = new DebugHandler(state) {
			private int count = 0;
			private boolean hasSoftAbort;

			@Override
			public void onInstruction(DebugState ds, DebugFrame di, int pc, Varargs extras, int top) throws LuaError {
				int count = ++this.count;
				if (count > 100000) {
					if (hardAbort != null) LuaThread.yield(state, NONE);
					this.count = 0;
				} else if (Config.Computer.timeoutError) {
					handleSoftAbort();
				}

				super.onInstruction(ds, di, pc, extras, top);
			}

			@Override
			public void poll() throws LuaError {
				if (hardAbort != null) LuaThread.yield(state, NONE);
				if (Config.Computer.timeoutError) handleSoftAbort();
			}

			private void handleSoftAbort() throws LuaError {
				// If the soft abort has been cleared then we can reset our flags and continue.
				String message = softAbort;
				if (message == null) {
					hasSoftAbort = false;
					return;
				}

				if (hasSoftAbort && hardAbort == null) {
					// If we have been soft aborted but not hard aborted then everything is OK.
					return;
				}

				hasSoftAbort = true;
				throw new LuaError(message);
			}
		};

		LuaTable globals = this.globals = new LuaTable();
		state.setupThread(globals);

		// Add basic libraries
		globals.load(state, new BaseLib());
		globals.load(state, new TableLib());
		globals.load(state, new StringLib());
		globals.load(state, new MathLib());
		globals.load(state, new CoroutineLib());

		LibFunction.bind(state, globals, PrefixLoader.class, new String[]{"load", "loadstring"});

		if (Config.APIs.debug) globals.load(state, new DebugLib());
		if (Config.APIs.profiler) globals.load(state, new ProfilerLib(resources));
		if (Config.APIs.bigInteger) BigIntegerValue.setup(globals);
		if (Config.APIs.bitop) BitOpLib.setup(globals);

		for (String global : ILLEGAL_NAMES) {
			globals.rawset(global, Constants.NIL);
		}
	}

	@Override
	public void setGlobal(@Nonnull String name, @Nullable Object object) {
		globals.rawset(name, toValue(object, null));
	}

	@Override
	public void enableDebug() {
		globals.load(state, new DebugLib());
	}

	@Override
	public void addAPI(ILuaAPI api) {
		LuaValue table = wrapLuaObject(api);
		for (String name : api.getNames()) {
			globals.rawset(name, table);
		}
	}

	@Override
	public void loadBios(InputStream bios) {
		if (mainThread != null) return;
		try {
			LuaFunction value = LoadState.load(state, bios, "@bios.lua", globals);
			mainThread = new LuaThread(state, value, globals);
		} catch (CompileException e) {
			if (mainThread != null) {
				state.abandon();
				mainThread = null;
			}
		} catch (IOException e) {
			if (mainThread != null) {
				state.abandon();
				mainThread = null;
			}
		}
	}

	@Override
	public void handleEvent(String eventName, Object[] arguments) {
		if (mainThread == null) return;

		if (eventFilter == null || eventName == null || eventName.equals(eventFilter) || eventName.equals("terminate")) {
			try {
				Varargs args = Constants.NONE;
				if (eventName != null) {
					Varargs params = toValues(arguments);
					if (params.count() == 0) {
						args = valueOf(eventName);
					} else {
						args = varargsOf(valueOf(eventName), params);
					}
				}

				Varargs results = mainThread.resume(args);
				if (hardAbort != null) {
					throw new LuaError(hardAbort);
				}

				if (!results.first().checkBoolean()) {
					throw new LuaError(results.arg(2).checkString());
				}

				LuaValue filter = results.arg(2);
				if (filter.isString()) {
					eventFilter = filter.toString();
				} else {
					eventFilter = null;
				}

				if (mainThread.getStatus().equals("dead")) {
					mainThread = null;
				}
			} catch (LuaError e) {
				state.abandon();
				mainThread = null;
			} finally {
				softAbort = null;
				hardAbort = null;
			}

		}
	}

	@Override
	public void softAbort(String message) {
		TweaksLogger.debug(message);
		softAbort = message;
	}

	@Override
	public void hardAbort(String message) {
		TweaksLogger.debug(message);
		softAbort = message;
		hardAbort = message;
	}

	@Override
	public boolean saveState(OutputStream outputStream) {
		return false;
	}

	@Override
	public boolean restoreState(InputStream inputStream) {
		return false;
	}

	@Override
	public boolean isFinished() {
		return mainThread == null;
	}

	@Override
	public void unload() {
		if (this.mainThread == null) return;
		state.abandon();
		mainThread = null;
	}

	private LuaValue wrapLuaObject(final ILuaObject object) {
		String[] methods = object.getMethodNames();
		LuaTable result = new LuaTable(0, methods.length);

		for (int i = 0; i < methods.length; i++) {
			final int method = i;
			result.rawset(methods[i], new VarArgFunction() {
				@Override
				public Varargs invoke(LuaState state, Varargs args) throws LuaError {
					if (!Config.Computer.timeoutError) {
						String message = softAbort;
						if (message != null) {
							softAbort = null;
							hardAbort = null;
							throw new LuaError(message);
						}
					}

					try {
						@SuppressWarnings("deprecation")
						Object[] results = object.callMethod(CobaltMachine.this, method, CobaltConverter.toObjects(args, 1, false));
						return toValues(results);
					} catch (LuaException e) {
						throw new LuaError(e.getMessage(), e.getLevel());
					} catch (InterruptedException e) {
						throw new OrphanedThread();
					} catch (Throwable e) {
						throw new LuaError("Java Exception Thrown: " + e.toString(), 0);
					}
				}
			});
		}

		return result;
	}

	//region Conversion
	private LuaValue toValue(Object object, Map<Object, LuaValue> tables) {
		if (object == null) {
			return NIL;
		} else if (object instanceof Number) {
			return valueOf(((Number) object).doubleValue());
		} else if (object instanceof Boolean) {
			return valueOf((Boolean) object);
		} else if (object instanceof String) {
			return valueOf(object.toString());
		} else if (object instanceof byte[]) {
			return valueOf((byte[]) object);
		} else if (object instanceof Map) {
			if (tables == null) {
				tables = new IdentityHashMap<Object, LuaValue>();
			} else {
				LuaValue value = tables.get(object);
				if (value != null) return value;
			}

			LuaTable table = new LuaTable();
			tables.put(object, table);

			for (Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet()) {
				LuaValue key = toValue(pair.getKey(), tables);
				LuaValue value = toValue(pair.getValue(), tables);
				if (!key.isNil() && !value.isNil()) {
					table.rawset(key, value);
				}
			}

			return table;
		} else if (object instanceof ILuaObject) {
			return wrapLuaObject((ILuaObject) object);
		} else {
			return NIL;
		}
	}

	private Varargs toValues(Object[] objects) {
		if (objects != null && objects.length != 0) {
			LuaValue[] values = new LuaValue[objects.length];

			for (int i = 0; i < objects.length; ++i) {
				Object object = objects[i];
				values[i] = toValue(object, null);
			}

			return varargsOf(values);
		} else {
			return NONE;
		}
	}
	//endregion

	@Nonnull
	@Override
	public Object[] yield(Object[] objects) throws InterruptedException {
		try {
			Varargs results = LuaThread.yield(state, toValues(objects));
			return CobaltConverter.toObjects(results, 1, false);
		} catch (OrphanedThread e) {
			throw new InterruptedException();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static class PrefixLoader extends VarArgFunction {
		private static final LuaString FUNCTION_STR = valueOf("function");
		private static final LuaString EQ_STR = valueOf("=");

		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError {
			switch (opcode) {
				case 0: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
				{
					LuaValue func = args.arg(1).checkFunction();
					LuaString chunkname = args.arg(2).optLuaString(FUNCTION_STR);
					if (!chunkname.startsWith('@') && !chunkname.startsWith('=')) {
						chunkname = OperationHelper.concat(EQ_STR, chunkname);
					}
					return BaseLib.loadStream(state, new StringInputStream(state, func), chunkname);
				}
				case 1: // "loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
				{
					LuaString script = args.arg(1).checkLuaString();
					LuaString chunkname = args.arg(2).optLuaString(script);
					if (!chunkname.startsWith('@') && !chunkname.startsWith('=')) {
						chunkname = OperationHelper.concat(EQ_STR, chunkname);
					}
					return BaseLib.loadStream(state, script.toInputStream(), chunkname);
				}
			}

			return NONE;
		}
	}

	private static class StringInputStream extends InputStream {
		private final LuaState state;
		private final LuaValue func;
		private byte[] bytes;
		private int offset, remaining = 0;

		public StringInputStream(LuaState state, LuaValue func) {
			this.state = state;
			this.func = func;
		}

		@Override
		public int read() throws IOException {
			if (remaining <= 0) {
				LuaValue s;
				try {
					s = OperationHelper.call(state, func);
				} catch (LuaError e) {
					throw new IOException(e);
				}

				if (s.isNil()) {
					return -1;
				}
				LuaString ls;
				try {
					ls = s.strvalue();
				} catch (LuaError e) {
					throw new IOException(e);
				}
				bytes = ls.bytes;
				offset = ls.offset;
				remaining = ls.length;
				if (remaining <= 0) {
					return -1;
				}
			}
			--remaining;
			return bytes[offset++];
		}
	}
}
