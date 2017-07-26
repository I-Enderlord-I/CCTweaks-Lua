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

-- Prevent access to metatables or environments of strings, as these are global between all computers
local native_getmetatable = getmetatable
local native_getfenv = getfenv
local native_error = error
local native_type = type
local string_metatable = native_getmetatable("")

function getmetatable(t)
	local mt = native_getmetatable(t)
	if mt == string_metatable then
		native_error("Attempt to access string metatable", 2)
	else
		return mt
	end
end

if native_getfenv then
	local string_env = native_getfenv(("").gsub)
	function getfenv(env)
		if env == nil then
			env = 2
		elseif native_type(env) == "number" and env > 0 then
			env = env + 1
		end
		local fenv = native_getfenv(env)
		if fenv == string_env then
			return native_getfenv(0)
		else
			return fenv
		end
	end
end

return fun()
