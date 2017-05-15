package org.squiddev.cctweaks.lua.lib;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ITask;
import dan200.computercraft.core.computer.MainThread;

import javax.annotation.Nonnull;

public abstract class AbstractLuaContext implements ILuaContext {
	private final Computer computer;

	public AbstractLuaContext(Computer computer) {
		this.computer = computer;
	}

	@Nonnull
	@Override
	public final Object[] pullEvent(String filter) throws LuaException, InterruptedException {
		Object[] results = pullEventRaw(filter);
		if (results.length >= 1 && results[0].equals("terminate")) {
			throw new LuaException("Terminated", 0);
		} else {
			return results;
		}
	}

	@Override
	public final Object[] pullEventRaw(String filter) throws InterruptedException {
		return yield(new Object[]{filter});
	}

	@Override
	public final Object[] executeMainThreadTask(@Nonnull final ILuaTask task) throws LuaException, InterruptedException {
		long taskID = issueMainThreadTask(task);

		Object[] response;
		do {
			do {
				response = this.pullEvent("task_complete");
			} while (response.length < 3);
		}
		while (!(response[1] instanceof Number) || !(response[2] instanceof Boolean) || (long) ((Number) response[1]).intValue() != taskID);

		if (!(Boolean) response[2]) {
			if (response.length >= 4 && response[3] instanceof String) {
				throw new LuaException((String) response[3]);
			} else {
				throw new LuaException();
			}
		} else {
			Object[] returnValues = new Object[response.length - 3];
			System.arraycopy(response, 3, returnValues, 0, returnValues.length);
			return returnValues;
		}
	}

	@Override
	public final long issueMainThreadTask(@Nonnull final ILuaTask task) throws LuaException {
		final long taskID = MainThread.getUniqueTaskID();
		ITask generatedTask = new ITask() {
			@Override
			public Computer getOwner() {
				return computer;
			}

			@Override
			public void execute() {
				try {
					Object[] t = task.execute();
					if (t != null) {
						Object[] eventArguments = new Object[t.length + 2];
						eventArguments[0] = taskID;
						eventArguments[1] = true;
						System.arraycopy(t, 0, eventArguments, 2, t.length);

						computer.queueEvent("task_complete", eventArguments);
					} else {
						computer.queueEvent("task_complete", new Object[]{taskID, true});
					}
				} catch (LuaException e) {
					computer.queueEvent("task_complete", new Object[]{taskID, false, e.getMessage()});
				} catch (Throwable e) {
					computer.queueEvent("task_complete", new Object[]{taskID, false, "Java Exception Thrown: " + e.toString()});
				}
			}
		};
		if (MainThread.queueTask(generatedTask)) {
			return taskID;
		} else {
			throw new LuaException("Task limit exceeded");
		}
	}
}
