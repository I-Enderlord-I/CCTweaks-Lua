package org.squiddev.cctweaks.lua;

import org.squiddev.cctweaks.lua.lib.socket.AddressMatcher;
import org.squiddev.configgen.*;

/**
 * The main config class
 */
@org.squiddev.configgen.Config(languagePrefix = "gui.config.cctweaks.", propertyPrefix = "cctweaks")
public final class Config {
	private static final String BIOS_PATH = "/assets/computercraft/lua/bios.lua";

	@OnSync
	public static void sync() {
		if (!Computer.biosPath.startsWith("/")) {
			TweaksLogger.warn("bios path (" + Computer.biosPath + ") does not start with '/', reverting to default");
			Computer.biosPath = BIOS_PATH;
		} else if (Config.class.getResource(Computer.biosPath) == null) {
			TweaksLogger.warn("Cannot find custom bios (" + Computer.biosPath + "), reverting to default");
			Computer.biosPath = BIOS_PATH;
		}

		if (Computer.preBiosPath != null && !Computer.preBiosPath.isEmpty()) {
			if (!Computer.preBiosPath.startsWith("/")) {
				TweaksLogger.warn("Pre-bios path (" + Computer.preBiosPath + ") does not start with '/', reverting to default");
				Computer.preBiosPath = "";
			} else if (Config.class.getResource(Computer.preBiosPath) == null) {
				TweaksLogger.warn("Cannot find custom pre-bios (" + Computer.preBiosPath + "), reverting to default");
				Computer.preBiosPath = "";
			}
		}
	}

	/**
	 * Computer tweaks and items.
	 */
	public static final class Computer {
		/**
		 * Time in milliseconds before 'Too long without yielding' errors.
		 * You cannot shutdown/reboot the computer during this time.
		 * Use carefully.
		 */
		@DefaultInt(7000)
		@Range(min = 0)
		public static int computerThreadTimeout;

		/**
		 * Specifies the Lua runtime to use for computers.
		 */
		@DefaultString("luaj")
		public static String runtime;

		/**
		 * Error much earlier on a timeout.
		 * Note: This only applies to the Cobalt VM
		 */
		@DefaultBoolean(false)
		public static boolean timeoutError;

		/**
		 * Specify a custom bios path to use.
		 * You must include the initial slash.
		 */
		@RequiresRestart(mc = false, world = true)
		@DefaultString(BIOS_PATH)
		public static String biosPath;

		/**
		 * Specify a custom pre-bios path to use when executing under a custom ROM.
		 * You must include the initial slash.
		 */
		@RequiresRestart(mc = false, world = true)
		@DefaultString("")
		public static String preBiosPath;

		/**
		 * Configuration options to enable running computers across multiple
		 * threads.
		 */
		@RequiresRestart
		public static class MultiThreading {
			/**
			 * Whether the custom multi-threaded executor is enabled.
			 * This can be used with any runtime but may function differently
			 * to normal ComputerCraft.
			 */
			@DefaultBoolean(false)
			public static boolean enabled;

			/**
			 * Number of threads to execute computers on. More threads means
			 * more computers can run at once, but may consume more resources.
			 * This requires the Cobalt VM.
			 */
			@DefaultInt(1)
			@Range(min = 1)
			public static int threads;

			/**
			 * The priority for computer threads. A lower number means
			 * they will take up less CPU time but as a result will run slower.
			 */
			@DefaultInt(Thread.NORM_PRIORITY)
			@Range(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
			public static int priority;
		}
	}

	/**
	 * Custom APIs for computers
	 */
	public static final class APIs {
		/**
		 * TCP connections from the socket API
		 */
		public static final class Socket {
			/**
			 * Enable TCP connections.
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean tcp;

			/**
			 * Enable websockets.
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean websocket;

			/**
			 * Blacklisted domain names.
			 *
			 * Entries are either domain names (www.example.com) or IP addresses in
			 * string format (10.0.0.3), optionally in CIDR notation to make it easier
			 * to define address ranges (1.0.0.0/8). Domains are resolved to their
			 * actual IP once on startup, future requests are resolved and compared
			 * to the resolved addresses.
			 */
			@DefaultString({"127.0.0.0/8", "10.0.0.0/8", "192.168.0.0/16", "172.16.0.0/12"})
			public static AddressMatcher blacklist;

			/**
			 * Whitelisted domain names.
			 * If something is mentioned in both the blacklist and whitelist then
			 * the blacklist takes priority.
			 */
			public static AddressMatcher whitelist;

			/**
			 * Maximum connections a computer can have at any time
			 */
			@DefaultInt(4)
			@Range(min = 1)
			public static int maxConnections;

			/**
			 * Number of threads to use for processing name lookups.
			 */
			@DefaultInt(4)
			@Range(min = 1)
			@RequiresRestart
			public static int threads;

			/**
			 * Number of threads to use for processing netty connections
			 */
			@DefaultInt(4)
			@Range(min = 1)
			@RequiresRestart
			public static int nettyThreads;

			/**
			 * Maximum number of characters to read from a socket.
			 */
			@DefaultInt(2048)
			@Range(min = 1)
			public static int maxRead = 2048;
		}

		/**
		 * Basic data manipulation
		 */
		public static final class Data {
			/**
			 * If the data API is enabled
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean enabled;

			/**
			 * Maximum number of bytes to process.
			 * The default is 1MiB
			 */
			@DefaultInt(1048576)
			public static int limit;
		}

		/**
		 * Enable the debug API.
		 * This is NOT recommended for servers, use at your own risk.
		 * It should be save on servers if using Cobalt though.
		 */
		@RequiresRestart(mc = false, world = true)
		public static boolean debug;

		/**
		 * Enable the profiler API.
		 * Only works on Cobalt
		 */
		@RequiresRestart(mc = false, world = true)
		public static boolean profiler;

		/**
		 * Enable the biginteger API.
		 */
		@RequiresRestart(mc = false, world = true)
		@DefaultBoolean(true)
		public static boolean bigInteger;

		/**
		 * Enable the extended bit operator library
		 */
		@RequiresRestart(mc = false, world = true)
		@DefaultBoolean(true)
		public static boolean bitop;
	}

	/**
	 * Only used when testing and developing the mod.
	 * Nothing to see here, move along...
	 */
	public static final class Testing {
		/**
		 * Show debug messages.
		 * If you hit a bug, enable this, rerun and send the log
		 */
		@DefaultBoolean(false)
		public static boolean debug;

		/**
		 * Throw exceptions on calling deprecated methods
		 *
		 * Only for development/testing
		 */
		@DefaultBoolean(false)
		public static boolean deprecatedWarnings;

		/**
		 * Dump the modified class files to asm/CCTweaks
		 */
		@DefaultBoolean(false)
		public static boolean dumpAsm;
	}
}
