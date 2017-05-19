package org.squiddev.cctweaks.lua.patcher.runner;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerThread;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;

import java.lang.reflect.Field;

/**
 * Utilities to run a script on a computer
 */
public class RunOnComputer {
	public static void run(String program, int shutdownAfter) throws Throwable {
		ComputerCraft.logPeripheralErrors = true;
		ComputerCraft.log = LogManager.getLogger("ComputerCraft");

		MemoryMount mount = new MemoryMount()
			.addFile("test", program)
			.addFile("startup", "assert.assert(pcall(loadfile('test', _ENV or getfenv()) or error)) os.shutdown()");
		Terminal term = new Terminal(51, 19);
		final Computer computer = new Computer(
			new BasicEnvironment(mount),
			term,
			0
		);

		AssertionAPI api = new AssertionAPI();
		computer.addAPI(api);

		try {
			computer.turnOn();

			for (int i = 0; i < 2000; i++) {
				long start = System.currentTimeMillis();

				computer.advance(0.05);
				MainThread.executePendingTasks();

				Throwable exception = api.getException();
				if (exception != null) {
					if (computer.isOn()) computer.shutdown();
					throw exception;
				}

				long remaining = (1000 / 20) - (System.currentTimeMillis() - start);
				if (remaining > 0) Thread.sleep(remaining);

				// Only break if the computer is *actually* off.
				Field field = Computer.class.getDeclaredField("m_state");
				field.setAccessible(true);
				String status = field.get(computer).toString();
				if (i > 5 && status.equals("Off")) break;

				// Shutdown the computer after a period of time
				if (shutdownAfter > 0 && i != 0 && i % shutdownAfter == 0) {
					computer.shutdown();
				}
			}

			Throwable exception = api.getException();
			if (exception != null) {
				if (computer.isOn()) computer.shutdown();
				throw exception;
			}

			if (computer.isOn()) {
				StringBuilder builder = new StringBuilder();
				for (int line = 0; line < 19; line++) {
					if (!term.getLine(line).toString().replace(" ", "").isEmpty()) {
						builder.append(line).append("|").append(term.getLine(line)).append('\n');
					}
				}
				computer.shutdown();

				String message = builder.length() == 0 ? " No result " : "\n" + builder.toString();
				Assert.fail("Still running:" + message);
			}
		} finally {
			ComputerThread.stop();
		}
	}
}
