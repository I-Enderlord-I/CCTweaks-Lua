# CCTweaks Lua [![Build Status](https://travis-ci.org/SquidDev-CC/CCTweaks-Lua.svg?branch=master)](https://travis-ci.org/SquidDev-CC/CCTweaks-Lua)
Modifications to ComputerCraft

This is a standalone version of [CCTweaks](https://github.com/SquidDev-CC/CCTweaks) that just contains modifications to the LuaVM and surrounding infrastructure.

## Getting started
 - [Download the latest release](https://github.com/SquidDev-CC/CCTweaks-Lua/releases/latest)
 - Add that and [Guava](https://github.com/google/guava) to your class path
 - Run `java -cp cctweaks.jar:guava.jar org.squiddev.cctweaks.lua.launch.Launcher [actual main class and arguments...]`
 
This will inject CCTweaks into the class loader, adding modifications.

## Features
 - Custom computer timeout
 - Whitelist globals (such as debug)
 - TCP and websocket socket API (`socket`)
 - Compression API (`data`)
 - [Cobalt](https://github.com/SquidDev/Cobalt) VM (reentrant fork of LuaJ)
   - Custom termination handler
   - Several bugs fixed (any object error messages, string pattern matching, number format strings)
   - Run multiple computers at once
 - Additional VMs provided by [CCTweaks-Runtimes](https://github.com/SquidDev-CC/CCTweaks-Runtimes)
 - API for adding custom APIs
 - Boot from custom `bios.lua` file

## Documentation
There is extensive documentation on the [CCTweaks-Lua wiki](https://github.com/SquidDev-CC/CCTweaks-Lua/wiki).

### Basic options
Whilst there are several tweaks applied by default, many more can be explicitly enabled
via the command line or (if in Minecraft) through the config GUI. Some useful options are as 
follows - full documentation can be found in the 
[documentation](https://github.com/SquidDev-CC/CCTweaks-Lua/wiki).

 - `-Dcctweaks.Computer.LuaJC.enabled=true`: Enable LuaJC compilation (a `luaJC` flag also exists).
 - `-Dcctweaks.Computer.cobalt=true`: Enable the [Cobalt VM](https://github.com/SquidDev/Cobalt).
 - `-Dcctweaks.Computer.computerThreadTimeout=7000`: Set the computer thread timeout. Time is measured in milliseconds.
 - `-Dcctweaks.Computer.timeoutError=true`: Enable timeouts anywhere, rather than just from CC functions (Cobalt only).
 - `-Dcctweaks.APIs.debug=true`: Enable the debug API.

### CCEmuRedux
This project is partly aimed at CCEmuRedux and so contains a custom launcher for CCEmuRedux.

 - Place the jar in `.ccemuredux/bin/`
 - Change directory to `.ccemuredux`
 - Execute `java -cp "bin/*" org.squiddev.cctweaks.lua.launch.CCEmuRedux`

Several configuration options also exist:

 - `-Dcctweaks.ccemu.width=51`: Manually set the width of all computers
 - `-Dcctweaks.ccemu.height=19`: Manually set the height of all computers

