package org.squiddev.cctweaks.lua.patch;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.computer.Computer;
import org.squiddev.cctweaks.api.lua.ArgumentDelegator;
import org.squiddev.cctweaks.api.lua.IArguments;
import org.squiddev.cctweaks.api.lua.ILuaObjectWithArguments;
import org.squiddev.patcher.visitors.MergeVisitor;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Extends the peripheral API to handle {@link org.squiddev.cctweaks.api.lua.IPeripheralWithArguments} instances.
 */
public class PeripheralAPI_Patch extends PeripheralAPI implements ILuaObjectWithArguments {
	@MergeVisitor.Stub
	private final PeripheralWrapper[] m_peripherals;

	@MergeVisitor.Stub
	public PeripheralAPI_Patch(IAPIEnvironment _environment) {
		super(_environment);
		m_peripherals = new PeripheralWrapper[6];
	}

	private int parseSide(IArguments arguments) throws LuaException {
		String side = arguments.getString(0);
		for (int n = 0; n < Computer.s_sideNames.length; n++) {
			if (side.equals(Computer.s_sideNames[n])) return n;
		}
		return -1;
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull IArguments args) throws LuaException, InterruptedException {
		if (method == 3) {
			if (args.size() >= 2 && args.getArgument(1) != null && args.getArgument(1) instanceof String) {
				String methodName = args.getString(1);
				int side = parseSide(args);
				if (side >= 0) {
					PeripheralWrapper p;
					synchronized (m_peripherals) {
						p = m_peripherals[side];
					}

					if (p != null) {
						return p.call(context, methodName, args.subArgs(2));
					}
				}

				throw new LuaException("No peripheral attached");
			}

			throw new LuaException("Expected string, string");
		}

		return callMethod(context, method, args.asArguments());
	}

	private abstract class PeripheralWrapper implements IComputerAccess {
		@MergeVisitor.Stub
		private final IPeripheral m_peripheral;
		@MergeVisitor.Stub
		private Map<String, Integer> m_methodMap;

		@MergeVisitor.Stub
		public PeripheralWrapper(IPeripheral peripheral) {
			m_peripheral = peripheral;
		}

		public Object[] call(ILuaContext context, String methodName, IArguments arguments) throws InterruptedException, LuaException {
			int method = -1;

			synchronized (this) {
				if (m_methodMap.containsKey(methodName)) {
					method = m_methodMap.get(methodName);
				}
			}

			if (method >= 0) {
				return ArgumentDelegator.delegatePeripheral(m_peripheral, this, context, method, arguments);
			}

			throw new LuaException("No such method " + methodName);
		}
	}
}
