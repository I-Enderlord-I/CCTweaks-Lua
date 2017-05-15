package org.squiddev.cctweaks.lua.patch;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.terminal.Terminal;
import org.squiddev.cctweaks.lua.patch.iface.ComputerPatched;
import org.squiddev.cctweaks.lua.patch.iface.IComputerEnvironmentExtended;
import org.squiddev.patcher.visitors.MergeVisitor;

public class Computer_Patch extends Computer implements ComputerPatched {
	private IMount romMount;
	private String biosPath;

	@MergeVisitor.Stub
	private static IMount s_romMount;
	@MergeVisitor.Stub
	private State m_state;
	@MergeVisitor.Stub
	private boolean m_startRequested;
	@MergeVisitor.Stub
	private final IComputerEnvironment m_environment = null;
	@MergeVisitor.Stub
	private FileSystem m_fileSystem;

	@MergeVisitor.Stub
	public Computer_Patch(IComputerEnvironment environment, Terminal terminal, int id) {
		super(environment, terminal, id);
	}

	/**
	 * Determine whether a computer is on or is starting
	 *
	 * @return If the computer is on
	 */
	public boolean isMostlyOn() {
		synchronized (this) {
			return m_startRequested || m_state != State.Off;
		}
	}

	/**
	 * If this computer should no longer handle events
	 *
	 * @return If this computer should not resume
	 * @see IComputerEnvironmentExtended#suspendEvents()
	 */
	public boolean suspendEvents() {
		return m_environment instanceof IComputerEnvironmentExtended && ((IComputerEnvironmentExtended) m_environment).suspendEvents();
	}

	@Override
	public void setRomMount(String biosPath, IMount mount) {
		this.biosPath = biosPath;
		this.romMount = mount;
	}

	private boolean initFileSystem() {
		assignID();

		try {
			m_fileSystem = new FileSystem("hdd", getRootMount());

			IMount mount = romMount;
			if (mount == null) {
				if (s_romMount == null) s_romMount = m_environment.createResourceMount("computercraft", "lua/rom");
				mount = s_romMount;
			}

			if (mount != null) {
				m_fileSystem.mount("rom", "rom", mount);
				return true;
			} else {
				return false;
			}
		} catch (FileSystemException var3) {
			var3.printStackTrace();
			return false;
		}
	}

	@MergeVisitor.Stub
	private enum State {
		Off,
		Starting,
		Running,
		Stopping,
	}
}
