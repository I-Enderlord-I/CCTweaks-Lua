package org.squiddev.cctweaks.lua.patcher.runner;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ILuaAPI;
import org.junit.Assert;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.TweaksLogger;

/**
 * Adds various assertions to Lua
 */
public class AssertionAPI implements ILuaAPI, IMethodDescriptor {
	private Throwable exception;

	@Override
	public String[] getNames() {
		return new String[]{"assert"};
	}

	@Override
	public void startup() {
	}

	@Override
	public void advance(double v) {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String[] getMethodNames() {
		return new String[]{"assert", "assertEquals", "debug", "oneTick"};
	}

	private String getMessage(Object[] objects, int index) {
		if (objects.length > index && objects[index] != null) {
			Object value = objects[index];
			if (value instanceof byte[]) {
				return new String((byte[]) value);
			}
			return value.toString();
		}

		return "<no message>";
	}

	@Override
	public Object[] callMethod(ILuaContext context, int method, Object[] objects) throws LuaException, InterruptedException {
		try {
			switch (method) {
				case 0: {
					if (objects.length == 0) Assert.fail("Assertion failed");
					String message = getMessage(objects, 1);
					if (objects[0] == null || (objects[0] instanceof Boolean && !(Boolean) objects[0])) {
						Assert.fail(message);
					}
					return null;
				}
				case 1:
					if (objects.length >= 2) {
						String message = getMessage(objects, 2);
						if (objects[0] instanceof byte[] && objects[1] instanceof byte[]) {
							Assert.assertArrayEquals(message, (byte[]) objects[0], (byte[]) objects[1]);
						} else {
							Assert.assertEquals(message, objects[0], objects[1]);
						}
					} else {
						throw new IllegalArgumentException("Expected 'expected' and 'actual'");
					}

					return null;
				case 2: {
					StringBuilder buffer = new StringBuilder().append("[Lua] ");
					for (int i = 0; i < objects.length; i++) {
						buffer.append(getMessage(objects, i)).append("\t");
					}
					TweaksLogger.debug(buffer.toString());
					return null;
				}
				case 3:
					return context.executeMainThreadTask(new ILuaTask() {
						@Override
						public Object[] execute() throws LuaException {
							return new Object[]{"One tick later"};
						}
					});
			}

			return null;
		} catch (Throwable e) {
			exception = e;
			throw new LuaException(e.getMessage());
		}
	}

	public Throwable getException() {
		return exception;
	}

	@Override
	public boolean willYield(int method) {
		return method == 3;
	}
}
