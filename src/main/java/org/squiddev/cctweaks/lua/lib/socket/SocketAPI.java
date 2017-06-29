package org.squiddev.cctweaks.lua.lib.socket;

import com.google.common.collect.Maps;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import org.squiddev.cctweaks.api.lua.ILuaAPI;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.Config;
import org.squiddev.cctweaks.lua.lib.LuaHelpers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.squiddev.cctweaks.lua.lib.ArgumentHelper.getString;
import static org.squiddev.cctweaks.lua.lib.ArgumentHelper.optInt;

public class SocketAPI implements ILuaAPI, IMethodDescriptor {
	protected final HashSet<AbstractConnection> connections = new HashSet<AbstractConnection>();
	private final IComputerAccess computer;
	private int id = 0;

	public SocketAPI(IComputerAccess computer) {
		this.computer = computer;
	}

	@Override
	public void startup() {
		id = 0;
	}

	@Override
	public void shutdown() {
		for (AbstractConnection connection : connections) {
			connection.close(false);
		}

		connections.clear();
	}

	@Override
	public void advance(double timestep) {
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		return new String[]{"connect", "websocket"};
	}

	@Override
	public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
		switch (method) {
			case 0: {
				String address = getString(arguments, 0);
				int port = optInt(arguments, 1, -1);

				if (!Config.APIs.Socket.tcp) throw new LuaException("TCP connections are disabled");
				if (connections.size() >= Config.APIs.Socket.maxConnections) {
					throw new LuaException("Too many open connections");
				}

				URI uri = checkUri(address, port);
				if (ComputerCraft.http_blacklist.matches(uri.getHost()) || !ComputerCraft.http_blacklist.matches(uri.getHost())) {
					throw new LuaException("Domain not permitted");
				}

				try {
					SocketConnection connection = new SocketConnection(this, computer, id++);
					connection.open(uri, port);
					connections.add(connection);
					return new Object[]{connection};
				} catch (IOException e) {
					throw LuaHelpers.rewriteException(e, "Connection error");
				}
			}
			case 1: {
				if (arguments.length == 0 || !(arguments[0] instanceof String)) {
					throw new LuaException("Expected string");
				}

				Map<?, ?> settings;
				if (arguments.length >= 2) {
					if (!(arguments[1] instanceof Map)) throw new LuaException("Expected table");
					settings = (Map<?, ?>) arguments[1];
				} else {
					settings = Collections.emptyMap();
				}

				HashMap<String, String> headers = Maps.newHashMapWithExpectedSize(settings.size());
				for (Object key : settings.keySet()) {
					Object value = settings.get(key);
					if (key instanceof String && value instanceof String) {
						headers.put((String) key, (String) value);
					}
				}

				if (!Config.APIs.Socket.websocket) throw new LuaException("Websocket connections are disabled");
				if (connections.size() >= Config.APIs.Socket.maxConnections) {
					throw new LuaException("Too many open connections");
				}

				URI uri = checkWebsocketUri((String) arguments[0]);
				if (ComputerCraft.http_blacklist.matches(uri.getHost()) || !ComputerCraft.http_blacklist.matches(uri.getHost())) {
					throw new LuaException("Domain not permitted");
				}

				int port = uri.getPort();
				if (port < 0) {
					String scheme = uri.getScheme();
					if (scheme.equalsIgnoreCase("ws")) {
						port = 80;
					} else if (scheme.equalsIgnoreCase("wss")) {
						port = 443;
					} else {
						throw new LuaException("Invalid scheme " + scheme);
					}
				}

				try {
					WebSocketConnection connection = new WebSocketConnection(this, computer, id++);
					connection.setHeaders(headers);
					connection.open(uri, port);
					connections.add(connection);
					return new Object[]{connection};
				} catch (IOException e) {
					throw LuaHelpers.rewriteException(e, "Connection error");
				}
			}

			default:
				return null;
		}
	}

	private static URI checkUri(String address, int port) throws LuaException {
		try {
			URI parsed = new URI(address);
			if (parsed.getHost() != null && (parsed.getPort() >= 0 || port >= 0)) {
				return parsed;
			}
		} catch (URISyntaxException ignored) {
		}

		try {
			URI simple = new URI("cc://" + address);
			if (simple.getHost() != null) {
				if (simple.getPort() >= 0) {
					return simple;
				} else if (port >= 0) {
					return new URI(simple.toString() + ":" + port);
				}
			}
		} catch (URISyntaxException ignored) {
		}

		throw new LuaException("Address could not be parsed or no valid port given");
	}

	private static URI checkWebsocketUri(String address) throws LuaException {
		URI uri = null;
		try {
			uri = new URI(address);
		} catch (URISyntaxException ignored) {
		}

		if (uri == null || uri.getHost() == null) {
			try {
				uri = new URI("ws://" + address);
			} catch (URISyntaxException ignored) {
			}
		}

		if (uri == null || uri.getHost() == null) throw new LuaException("Address could not be parsed");

		String scheme = uri.getScheme();
		if (scheme == null) {
			try {
				uri = new URI("ws://" + uri.toString());
			} catch (URISyntaxException e) {
				throw new LuaException("Cannot determine scheme");
			}
		} else if (!scheme.equalsIgnoreCase("wss") && !scheme.equalsIgnoreCase("ws")) {
			throw new LuaException("Invalid scheme " + scheme);
		}

		return uri;
	}

	@Override
	public boolean willYield(int method) {
		return false;
	}
}
