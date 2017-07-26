local function fail(msg)
	-- Reset the terminal
	term.setCursorBlink(false)
	term.clear()
	term.setCursorPos(1, 1)

	-- Print some red text
	if term.isColour() then term.setTextColour(16384) end
	term.write(msg)

	term.setCursorPos(1, 2)
	term.setTextColour(1)
	term.write("Press any key to continue")

	os.pullEvent("key")
	os.shutdown()
end

local handle = fs.open("/rom/bios.lua", "r")
if not handle then
	fail("Cannot find /rom/bios.lua")
	return
end

local contents = handle.readAll()
handle.close()

local fun, msg
if _VERSION == "Lua 5.1" then
	fun, msg = loadstring(contents, "bios.lua")
	if fun then setfenv(fun, _G) end
else
	fun, msg = load(contents, "bios.lua", "t", _G)
end

if not fun then
	fail("Cannot load bios.lua: " .. msg)
	return
end

return fun()
