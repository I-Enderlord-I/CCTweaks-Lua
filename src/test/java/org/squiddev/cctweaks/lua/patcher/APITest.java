package org.squiddev.cctweaks.lua.patcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

@RunWith(Parameterized.class)
public class APITest {
	@Parameterized.Parameters(name = "Version: {0}, Runtime: {1}")
	public static List<Object[]> getVersions() {
		return VersionHandler.getVersionsWithRuntimes();
	}

	@Parameterized.Parameter(0)
	public String version;

	@Parameterized.Parameter(1)
	public VersionHandler.Runtime runtime;

	private ClassLoader loader;

	@Before
	public void setup() throws Exception {
		runtime.setup();
		System.setProperty("cctweaks.APIs.bit", "true");
		loader = VersionHandler.getLoader(version);
	}

	@After
	public void tearDown() {
		runtime.tearDown();
	}

	@Test
	public void testBitop() throws Throwable {
		VersionHandler.runFile(loader, "bitop");
	}

	@Test
	public void testBigInt() throws Throwable {
		VersionHandler.runFile(loader, "bigint");
	}

	@Test
	public void testYield() throws Throwable {
		VersionHandler.runFile(loader, "yield");
	}
}
