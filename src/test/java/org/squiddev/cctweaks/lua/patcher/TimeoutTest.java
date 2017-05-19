package org.squiddev.cctweaks.lua.patcher;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

@RunWith(Parameterized.class)
public class TimeoutTest {
	@Parameterized.Parameters(name = "Version: {0}")
	public static List<Object[]> getVersions() {
		return Lists.newArrayList(
			// Non-timeoutError using tests fail as the inner loop never calls
			// a CC method and so the timeout error doesn't trigger.

			// new Object[]{new VersionHandler.Runtime("cobalt", false, false)},
			new Object[]{new VersionHandler.Runtime("cobalt", false, true)},
			// new Object[]{new VersionHandler.Runtime("cobalt", true, false)},
			new Object[]{new VersionHandler.Runtime("cobalt", true, true)}
		);
	}

	@Parameterized.Parameter
	public VersionHandler.Runtime runtime;

	@Before
	public void before() throws Exception {
		runtime.setup();
	}

	@After
	public void after() throws Exception {
		runtime.tearDown();
	}

	/**
	 * Ensures a "Too long without yielding" exception is thrown if a computer
	 * is shutdown half way through.
	 */
	@Test
	public void testTimeout() throws Throwable {
		try {
			ClassLoader loader = VersionHandler.getLatestLoader();
			VersionHandler.runFile(loader, "timeout", 100);
		} catch (AssertionError e) {
			if (e.getMessage().contains("Too long without yielding")) return;
			throw e;
		}

		Assert.fail("Expected timeout error");
	}
}
