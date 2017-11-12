package org.squiddev.cctweaks.lua.patcher;

import dan200.computercraft.ComputerCraft;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class VersionTest {
	@Parameterized.Parameters(name = "Version: {0}, Runtime: {1}")
	public static List<Object[]> getVersions() {
		return VersionHandler.getVersionsWithRuntimes();
	}

	@Parameterized.Parameter
	public String version;

	@Parameterized.Parameter(1)
	public VersionHandler.Runtime runtime;

	@Before
	public void before() throws Exception {
		runtime.setup();
	}

	@After
	public void tearDown() {
		runtime.tearDown();
	}

	@Test
	public void printVersion() throws Throwable {
		ClassLoader loader = VersionHandler.getLoader(version);
		String reportedVersion = VersionHandler.runClass(loader, "VersionTest", "getVersion");
		assertTrue(String.format("Version is '%s', but reports as '%s'", version, reportedVersion),
			version.startsWith(reportedVersion));
	}

	public static String getVersion() {
		return ComputerCraft.getVersion();
	}
}
