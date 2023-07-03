/*	Tapo Light Strip
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

===================================================================================================*/
import groovy.transform.Field
@Field volatile static Map<Long,Map>   g_mSmartDeviceStatusMap = [:]
def type() {return "tapoLightStrip" }

metadata {
	definition (name: type(), namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/${type()}.groovy")
	{
		capability "Switch"
		capability "Light"
		capability "Switch Level"
		capability "Change Level"
		command "setBrightWhite", [[name: "level", type: "NUMBER"]]
		attribute "colorTemerpature", "NUMBER"
		capability "Color Mode"
		capability "Color Control"
		capability "Refresh"
		attribute "commsError", "string"
		command "deviceLogin"
		command "setLightingEffect", [[
			name: "Lighting Effect",
			constraints: effectList(),
			type: "ENUM"
			]]
		attribute "effectName", "string"
		attribute "effectEnabled", "string"
	}
	preferences {
		input ("gradualOnOff", "bool", title: "Set Bulb to Gradual ON/OFF", defaultValue: false)
		input ("aesKey", "password", title: "Storage for the AES Key")
		input ("deviceCookie", "password", title: "Storage for the cookie")
		input ("deviceToken", "password", title: "Storage for the token")
	}
}

def installed() { 
	updateAttr("commsError", "OK")
	runIn(1, updated)
}
def updated() {
	unschedule()
	Map logData = [:]
	logData << [loginResults: deviceLogin()]
	logData << [login: setLoginInterval()]
	logData << [refresh: setRefreshInterval()]
	runIn(3, setGradualOnOff)
	runIn(6, getDeviceData)
	logData << [status: "OK"]
	logData << setLogsOff()
	if (logData.status == "ERROR") {
		logError("updated: ${logData}")
	} else {
		logInfo("updated: ${logData}")
	}
}

def getDeviceData() {
	Map logData = [:]
	Map cmdBody = [method: "get_device_info"]
	def cmdResp = securePassthrough(cmdBody, false).result
	if (cmdResp != "ERROR") {
		updateDataValue("ctLow", cmdResp.color_temp_range[0].toString())
		updateDataValue("ctHigh", cmdResp.color_temp_range[1].toString())
		logData << [ctLow: cmdResp.color_temp_range[0], ctHigh: cmdResp.color_temp_range[1]]
		deviceParse(cmdResp)
		logInfo("getDeviceData: ${logData}")
	} else {
		logData << ["ERROR": "Error in Sysn Comms"]
		logWarn("getDeviceData: ${logData}")
	}
}

def setGradualOnOff() {
	Map cmdBody = [
		method: "multipleRequest",
		params: [
			requests:[
				[method: "set_on_off_gradually_info", params: [enable: gradualOnOff]],
				[method: "get_on_off_gradually_info"]]]]
	def cmdResp = securePassthrough(cmdBody, false).result.responses
	def resp = cmdResp.find { it.method == "get_on_off_gradually_info" }
	device.updateSetting("gradualOnOff",[type:"bool", value: resp.result.enable])
	if (cmdResp[0].error_code != 0) {
		newGradualOnOff = "Failed"
	}
	logInfo("setGradualOnOff: [gradulaOnOff: ${resp.result.enable}]")
}

//	Switch and Light
def on() { setPower(true) }
def off() { setPower(false) }
def setPower(onOff) {
	logDebug("setPower: [device_on: ${onOff}]")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: onOff
		]]
	sendMultiCmd(cmdBody, true, "setPower")
}

//	Switch Level
def setLevel(level, transTime=null) {
	//	Note: Tapo Devices do not support transition time.  Set preference "Set Bulb to Gradual ON/OFF"
	logDebug("setLevel: [brightness: ${level}]")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: true,
			brightness: level
		]]
	sendMultiCmd(cmdBody, true, "setLevel")
}

//	Change Level
def startLevelChange(direction) {
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]")
	if (direction == "up") { levelUp() }
	else { levelDown() }
}
def stopLevelChange() {
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]")
	unschedule(levelUp)
	unschedule(levelDown)
}
def levelUp() {
	def curLevel = device.currentValue("level").toInteger()
	if (curLevel != 100) {
		def newLevel = curLevel + 4
		if (newLevel > 100) { newLevel = 100 }
		setLevel(newLevel)
		runIn(1, levelUp)
	}
}
def levelDown() {
	def curLevel = device.currentValue("level").toInteger()
	if (device.currentValue("switch") == "on") {
		def newLevel = curLevel - 4
		if (newLevel <= 0) { off() }
		else {
			setLevel(newLevel)
			runIn(1, levelDown)
		}
	}
}

//	Color Temperature
def setBrightWhite(level = device.currentValue("level")) {
	//	Note: Tapo Light Strip only supports a color temp setting of 9000."
	logDebug("setBrightWhite")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: true,
			brightness: level,
			color_temp: 9000
		]]
	sendMultiCmd(cmdBody, true, "setColorTemperature")
}

//	Color Control
def setHue(hue){
	logDebug("setHue: ${hue}")
	hue = (3.6 * hue).toInteger()
	logInfo("setHue: ${hue}")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: true,
			hue: hue,
			color_temp: 0
		]]
	sendMultiCmd(cmdBody, true, "setHue")
}
def setSaturation(saturation) {
	logDebug("setSatiratopm: ${saturation}")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: true,
			saturation: saturation,
			color_temp: 0
		]]
	sendMultiCmd(cmdBody, true, "setSaturation")
}
def setColor(color) {
	def hue = (3.6 * color.hue).toInteger()
	logDebug("setColor: ${color}")
	Map cmdBody = [
		method: "set_device_info",
		params: [
			device_on: true,
			hue: hue,
			saturation: color.saturation,
			brightness: color.level,
			color_temp: 0
		]]
	sendMultiCmd(cmdBody, true, "setColor")
}

def setLightingEffect(effect) {
	Map effData = getEffect(effect)
	Map cmdBody = [
		method: "set_lighting_effect",
		params: effData
	]
	sendMultiCmd(cmdBody, true, "setColor")
}

def deviceParse(devData, data=null) {
	logDebug("deviceParse: ${devData}")
	def onOff = "off"
	if (devData.device_on == true) { onOff = "on" }
	updateAttr("switch", onOff)
	updateAttr("level", devData.brightness)
	if (devData.color_temp == 0) {
		updateAttr("colorMode", "COLOR")
		def hubHue = (devData.hue / 3.6).toInteger()
		updateAttr("hue", hubHue)
		updateAttr("saturation", devData.saturation)
		updateAttr("color", "[hue: ${hubHue}, saturation: ${devData.saturation}]")
		updateAttr("colorName", colorName = convertHueToGenericColorName(hubHue))
		def rgb = hubitat.helper.ColorUtils.hsvToRGB([hubHue,
													  devData.saturation,
													  devData.brightness])
		updateAttr("RGB", rgb)
		updateAttr("colorTemperature", 0)
	} else {
		updateAttr("colorMode", "CT")
		updateAttr("colorTemperature", devData.color_temp)
		updateAttr("colorName", convertTemperatureToGenericColorName(devData.color_temp.toInteger()))
		updateAttr("color", "[:]")
		updateAttr("RGB", "[]")
	}
	if (devData.lighting_effect.enable == 0) { 
		updateAttr("effectName", " ")
		updateAttr("effectEnable", "false")
	} else {
		updateAttr("effectName", devData.lighting_effect.name)
		updateAttr("effectEnable", "true")
	}
}

def getEffect(effect) {
	List effects = [
		[custom:0, id:"TapoStrip_1MClvV18i15Jq3bvJVf0eP", brightness:100, name:"Aurora", enable:1, segments:[0], expansion_strategy:1, display_colors:[[120, 100, 100], [240, 100, 100], [260, 100, 100], [280, 100, 100]], type:"sequence", duration:0, transition:1500, direction:4, spread:7, repeat_times:0, sequence:[[120, 100, 100], [240, 100, 100], [260, 100, 100], [280, 100, 100]]],
		[custom:0, id:"TapoStrip_6DlumDwO2NdfHppy50vJtu", brightness:100, name:"Bubbling Cauldron", enable:1, segments:[0], expansion_strategy:1, display_colors:[[100, 100, 100], [270, 100, 100]], type:"random", hue_range:[100, 270], saturation_range:[80, 100], brightness_range:[50, 100], duration:0, transition:200, init_states:[[270, 100, 100]], fadeoff:1000, random_seed:24, backgrounds:[[270, 40, 50]]],
		[custom:0, id:"TapoStrip_6Dy0Nc45vlhFPEzG021Pe9", brightness:100, name:"Candy Cane", enable:1, segments:[0], expansion_strategy:1, display_colors:[[0, 0, 100], [360, 81, 100]], type:"sequence", duration:700, transition:500, direction:1, spread:1, repeat_times:0, sequence:[[0, 0, 100], [0, 0, 100], [360, 81, 100], [0, 0, 100], [0, 0, 100], [360, 81, 100], [360, 81, 100], [0, 0, 100], [0, 0, 100], [360, 81, 100], [360, 81, 100], [360, 81, 100], [360, 81, 100], [0, 0, 100], [0, 0, 100], [360, 81, 100]]],
		[custom:0, id:"TapoStrip_5zkiG6avJ1IbhjiZbRlWvh", brightness:100, name:"Christmas", enable:1, segments:[0], expansion_strategy:1, display_colors:[[136, 98, 100], [350, 97, 100]], type:"random", hue_range:[136, 146], saturation_range:[90, 100], brightness_range:[50, 100], duration:5000, transition:0, init_states:[[136, 0, 100]], fadeoff:2000, random_seed:100, backgrounds:[[136, 98, 75], [136, 0, 0], [350, 0, 100], [350, 97, 94]]],
		[custom:0, id:"TapoStrip_4HVKmMc6vEzjm36jXaGwMs", brightness:100, name:"Flicker", enable:1, segments:[1], expansion_strategy:1, display_colors:[[30, 81, 100], [40, 100, 100]], type:"random", hue_range:[30, 40], saturation_range:[100, 100], brightness_range:[50, 100], duration:0, transition:0, transition_range:[375, 500], init_states:[[30, 81, 80]]],
		[custom:0, id:"TapoStrip_3Gk6CmXOXbjCiwz9iD543C", brightness:100, name:"""Grandma's Christmas Lights""", enable:1, segments:[0], expansion_strategy:1, display_colors:[[30, 100, 100], [240, 100, 100], [130, 100, 100], [0, 100, 100]], type:"sequence", duration:5000, transition:100, direction:1, spread:1, repeat_times:0, sequence:[[30, 100, 100], [30, 0, 0], [30, 0, 0], [240, 100, 100], [240, 0, 0], [240, 0, 0], [240, 0, 100], [240, 0, 0], [240, 0, 0], [130, 100, 100], [130, 0, 0], [130, 0, 0], [0, 100, 100], [0, 0, 0], [0, 0, 0]]],
		[custom:0, id:"TapoStrip_2YTk4wramLKv5XZ9KFDVYm", brightness:100, name:"Hanukkah", enable:1, segments:[1], expansion_strategy:1, display_colors:[[200, 100, 100]], type:"random", hue_range:[200, 210], saturation_range:[0, 100], brightness_range:[50, 100], duration:1500, transition:0, transition_range:[400, 500], init_states:[[35, 81, 80]]],
		[custom:0, id:"TapoStrip_4rJ6JwC7I9st3tQ8j4lwlI", brightness:100, name:"Haunted Mansion", enable:1, segments:[80], expansion_strategy:2, display_colors:[[45, 10, 100]], type:"random", hue_range:[45, 45], saturation_range:[10, 10], brightness_range:[0, 80], duration:0, transition:0, transition_range:[50, 1500], init_states:[[45, 10, 100]], fadeoff:200, random_seed:1, backgrounds:[[45, 10, 100]]],
		[custom:0, id:"TapoStrip_7UcYLeJbiaxVIXCxr21tpx", brightness:100, name:"Icicle", enable:1, segments:[0], expansion_strategy:1, display_colors:[[190, 100, 100]], type:"sequence", duration:0, transition:400, direction:4, spread:3, repeat_times:0, sequence:[[190, 100, 70], [190, 100, 70], [190, 30, 50], [190, 100, 70], [190, 100, 70]]],
		[custom:0, id:"TapoStrip_7OGzfSfnOdhoO2ri4gOHWn", brightness:100, name:"Lightning", enable:1, segments:[7], expansion_strategy:1, display_colors:[[210, 10, 100], [200, 50, 100], [200, 100, 100]], type:"random", hue_range:[240, 240], saturation_range:[10, 11], brightness_range:[90, 100], duration:0, transition:50, init_states:[[240, 30, 100]], fadeoff:150, random_seed:50, backgrounds:[[200, 100, 100], [200, 50, 10], [210, 10, 50], [240, 10, 0]]],
		[custom:0, id:"TapoStrip_0fOleCdwSgR0nfjkReeYfw", brightness:100, name:"Ocean", enable:1, segments:[0], expansion_strategy:1, display_colors:[[198, 84, 100]], type:"sequence", duration:0, transition:2000, direction:3, spread:16, repeat_times:0, sequence:[[198, 84, 30], [198, 70, 30], [198, 10, 30]]],
		[custom:0, id:"TapoStrip_7CC5y4lsL8pETYvmz7UOpQ", brightness:100, name:"Rainbow", enable:1, segments:[0], expansion_strategy:1, display_colors:[[0, 100, 100], [100, 100, 100], [200, 100, 100], [300, 100, 100]], type:"sequence", duration:0, transition:1500, direction:1, spread:12, repeat_times:0, sequence:[[0, 100, 100], [100, 100, 100], [200, 100, 100], [300, 100, 100]]],
		[custom:0, id:"TapoStrip_1t2nWlTBkV8KXBZ0TWvBjs", brightness:100, name:"Raindrop", enable:1, segments:[0], expansion_strategy:1, display_colors:[[200, 10, 100], [200, 20, 100]], type:"random", hue_range:[200, 200], saturation_range:[10, 20], brightness_range:[10, 30], duration:0, transition:1000, init_states:[[200, 40, 100]], fadeoff:1000, random_seed:24, backgrounds:[[200, 40, 0]]],
		[custom:0, id:"TapoStrip_1nL6GqZ5soOxj71YDJOlZL", brightness:100, name:"Spring", enable:1, segments:[0], expansion_strategy:1, display_colors:[[0, 30, 100], [130, 100, 100]], type:"random", hue_range:[0, 90], saturation_range:[30, 100], brightness_range:[90, 100], duration:600, transition:0, transition_range:[2000, 6000], init_states:[[80, 30, 100]], fadeoff:1000, random_seed:20, backgrounds:[[130, 100, 40]]],
		[custom:0, id:"TapoStrip_1OVSyXIsDxrt4j7OxyRvqi", brightness:100, name:"Sunrise", enable:1, segments:[0], expansion_strategy:2, display_colors:[[30, 0, 100], [30, 95, 100], [0, 100, 100]], type:"pulse", duration:600, transition:60000, direction:1, spread:1, repeat_times:1, run_time:0, sequence:[[0, 100, 5], [0, 100, 5], [10, 100, 6], [15, 100, 7], [20, 100, 8], [20, 100, 10], [30, 100, 12], [30, 95, 15], [30, 90, 20], [30, 80, 25], [30, 75, 30], [30, 70, 40], [30, 60, 50], [30, 50, 60], [30, 20, 70], [30, 0, 100]], trans_sequence:[]],
		[custom:0, id:"TapoStrip_5NiN0Y8GAUD78p4neKk9EL", brightness:100, name:"Sunset", enable:1, segments:[0], expansion_strategy:2, display_colors:[[0, 100, 100], [30, 95, 100], [30, 0, 100]], type:"pulse", duration:600, transition:60000, direction:1, spread:1, repeat_times:1, run_time:0, sequence:[[30, 0, 100], [30, 20, 100], [30, 50, 99], [30, 60, 98], [30, 70, 97], [30, 75, 95], [30, 80, 93], [30, 90, 90], [30, 95, 85], [30, 100, 80], [20, 100, 70], [20, 100, 60], [15, 100, 50], [10, 100, 40], [0, 100, 30], [0, 100, 0]], trans_sequence:[]],
		[custom:0, id:"TapoStrip_2q1Vio9sSjHmaC7JS9d30l", brightness:100, name:"Valentines", enable:1, segments:[0], expansion_strategy:1, display_colors:[[340, 20, 100], [20, 50, 100], [0, 100, 100], [340, 40, 100]], type:"random", hue_range:[340, 340], saturation_range:[30, 40], brightness_range:[90, 100], duration:600, transition:2000, init_states:[[340, 30, 100]], fadeoff:3000, random_seed:100, backgrounds:[[340, 20, 50], [20, 50, 50], [0, 100, 50]]]
	]
	return effects.find { it.name == effect }
}

def effectList() {
	List effects = [
		"Aurora", "Bubbling Cauldron", "Candy Cane", "Christmas", 
		"Flicker", """Grandma's Christmas Lights""", "Hanukkah", 
		"Haunted Mansion", "Icicle", "Lightning", "Ocean", 
		"Rainbow", "Raindrop", "Spring", "Sunrise", "Sunset", "Valentines"]
	return effects
}

//	Library Inclusion




// ~~~~~ start include (1326) davegut.tapoCommon ~~~~~
library ( // library marker davegut.tapoCommon, line 1
	name: "tapoCommon", // library marker davegut.tapoCommon, line 2
	namespace: "davegut", // library marker davegut.tapoCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tapoCommon, line 4
	description: "Method common to Tapo devices", // library marker davegut.tapoCommon, line 5
	category: "utilities", // library marker davegut.tapoCommon, line 6
	documentationLink: "" // library marker davegut.tapoCommon, line 7
) // library marker davegut.tapoCommon, line 8
def driverVer() { return "B.1.0" } // library marker davegut.tapoCommon, line 9

def setRefreshInterval() { // library marker davegut.tapoCommon, line 11
	runEvery10Minutes(refresh) // library marker davegut.tapoCommon, line 12
	return "10 mins" // library marker davegut.tapoCommon, line 13
} // library marker davegut.tapoCommon, line 14

def setLoginInterval() { // library marker davegut.tapoCommon, line 16
	runEvery3Hours(deviceLogin) // library marker davegut.tapoCommon, line 17
	return "3 hrs" // library marker davegut.tapoCommon, line 18
} // library marker davegut.tapoCommon, line 19

command "getDeveloperData" // library marker davegut.tapoCommon, line 21
def getDeveloperData() { // library marker davegut.tapoCommon, line 22
	def attrData = device.getCurrentStates() // library marker davegut.tapoCommon, line 23
	Map attrs = [:] // library marker davegut.tapoCommon, line 24
	attrData.each { // library marker davegut.tapoCommon, line 25
		attrs << ["${it.name}": it.value] // library marker davegut.tapoCommon, line 26
	} // library marker davegut.tapoCommon, line 27
	Date date = new Date() // library marker davegut.tapoCommon, line 28
	Map devData = [ // library marker davegut.tapoCommon, line 29
		currentTime: date.toString(), // library marker davegut.tapoCommon, line 30
		lastLogin: state.lastSuccessfulLogin, // library marker davegut.tapoCommon, line 31
		name: device.getName(), // library marker davegut.tapoCommon, line 32
		status: device.getStatus(), // library marker davegut.tapoCommon, line 33
		aesKeyLen: aesKey.length(), // library marker davegut.tapoCommon, line 34
		cookieLen: deviceCookie.length(), // library marker davegut.tapoCommon, line 35
		tokenLen: deviceToken.length(), // library marker davegut.tapoCommon, line 36
		dataValues: device.getData(), // library marker davegut.tapoCommon, line 37
		attributes: attrs, // library marker davegut.tapoCommon, line 38
		cmdResp: securePassthrough([method: "get_device_info"], false), // library marker davegut.tapoCommon, line 39
		children: getChildDevices() // library marker davegut.tapoCommon, line 40
	] // library marker davegut.tapoCommon, line 41
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.tapoCommon, line 42
} // library marker davegut.tapoCommon, line 43

//	===== Login Process ===== // library marker davegut.tapoCommon, line 45
def deviceLogin() { // library marker davegut.tapoCommon, line 46
	Map logData = [:] // library marker davegut.tapoCommon, line 47
	def handshakeData = handshake() // library marker davegut.tapoCommon, line 48
	if (handshakeData.respStatus == "OK") { // library marker davegut.tapoCommon, line 49
		logData << [deviceCookie: "REDACTED for logs",  // library marker davegut.tapoCommon, line 50
				    aesKey: "REDACTED for logs"] // library marker davegut.tapoCommon, line 51
		def tokenData = loginDevice(handshakeData.cookie, handshakeData.aesKey) // library marker davegut.tapoCommon, line 52
		if (tokenData.respStatus == "OK") { // library marker davegut.tapoCommon, line 53
			device.updateSetting("aesKey", [type:"password", value: handshakeData.aesKey]) // library marker davegut.tapoCommon, line 54
			device.updateSetting("deviceCookie", [type:"password", value: handshakeData.cookie]) // library marker davegut.tapoCommon, line 55
			device.updateSetting("deviceToken", [type:"password", value: tokenData.token]) // library marker davegut.tapoCommon, line 56
			Date date = new Date() // library marker davegut.tapoCommon, line 57
			state.lastSuccessfulLogin = date.toString() // library marker davegut.tapoCommon, line 58
			state.lastLogin = now() // library marker davegut.tapoCommon, line 59
			logData << [deviceToken: "REDACTED for logs"] // library marker davegut.tapoCommon, line 60
		} else { // library marker davegut.tapoCommon, line 61
			logData << [tokenData: tokenData] // library marker davegut.tapoCommon, line 62
		} // library marker davegut.tapoCommon, line 63
	} else { // library marker davegut.tapoCommon, line 64
		logData << [handshakeData: handshakeData] // library marker davegut.tapoCommon, line 65
	} // library marker davegut.tapoCommon, line 66
	pauseExecution(2000) // library marker davegut.tapoCommon, line 67
	return logData // library marker davegut.tapoCommon, line 68
} // library marker davegut.tapoCommon, line 69

def handshake() { // library marker davegut.tapoCommon, line 71
	Map handshakeData = [method: "handshakeData"] // library marker davegut.tapoCommon, line 72
	Map cmdBody = [ method: "handshake", params: [ key: publicPem()]] // library marker davegut.tapoCommon, line 73
	def uri = "http://${getDataValue("deviceIp")}/app" // library marker davegut.tapoCommon, line 74
	def respData = syncPost(uri, cmdBody) // library marker davegut.tapoCommon, line 75

	if (respData.respStatus == "OK") { // library marker davegut.tapoCommon, line 77
		if (respData.data.error_code == 0) { // library marker davegut.tapoCommon, line 78
			String deviceKey = respData.data.result.key // library marker davegut.tapoCommon, line 79
			def aesArray = readDeviceKey(deviceKey) // library marker davegut.tapoCommon, line 80
			if (aesArray == "ERROR") { // library marker davegut.tapoCommon, line 81
				logData: [readDeviceKey: "FAILED"] // library marker davegut.tapoCommon, line 82
				handshakeData << [respStatus: "Failed decoding deviceKey", // library marker davegut.tapoCommon, line 83
								  deviceKey: deviceKey] // library marker davegut.tapoCommon, line 84
			} else { // library marker davegut.tapoCommon, line 85
				handshakeData << [respStatus: "OK"] // library marker davegut.tapoCommon, line 86
				def cookieHeader = respData.headers["set-cookie"].toString() // library marker davegut.tapoCommon, line 87
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tapoCommon, line 88
				handshakeData << [cookie: cookie, aesKey: aesArray] // library marker davegut.tapoCommon, line 89
			} // library marker davegut.tapoCommon, line 90
		} else { // library marker davegut.tapoCommon, line 91
			handshakeData << [respStatus: "Command Error", data: respData.data] // library marker davegut.tapoCommon, line 92
		} // library marker davegut.tapoCommon, line 93
	} else { // library marker davegut.tapoCommon, line 94
		handshakeData << respData // library marker davegut.tapoCommon, line 95
	} // library marker davegut.tapoCommon, line 96
	return handshakeData // library marker davegut.tapoCommon, line 97
} // library marker davegut.tapoCommon, line 98

def readDeviceKey(deviceKey) { // library marker davegut.tapoCommon, line 100
	String privateKey = parent.privateKey() // library marker davegut.tapoCommon, line 101
	Map logData = [privateKey: privateKey] // library marker davegut.tapoCommon, line 102
	def response = "ERROR" // library marker davegut.tapoCommon, line 103
	try { // library marker davegut.tapoCommon, line 104
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.tapoCommon, line 105
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.tapoCommon, line 106
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.tapoCommon, line 107
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.tapoCommon, line 108
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.tapoCommon, line 109
		response = cryptoArray // library marker davegut.tapoCommon, line 110
		logData << [cryptoArray: "REDACTED for logs", status: "OK"] // library marker davegut.tapoCommon, line 111
		logDebug("readDeviceKey: ${logData}") // library marker davegut.tapoCommon, line 112
	} catch (e) { // library marker davegut.tapoCommon, line 113
		logData << [status: "READ ERROR", data: e] // library marker davegut.tapoCommon, line 114
		logWarn("readDeviceKey: ${logData}") // library marker davegut.tapoCommon, line 115
	} // library marker davegut.tapoCommon, line 116
	return response // library marker davegut.tapoCommon, line 117
} // library marker davegut.tapoCommon, line 118

def loginDevice(cookie, cryptoArray) { // library marker davegut.tapoCommon, line 120
	Map tokenData = [method: "loginDevice"] // library marker davegut.tapoCommon, line 121
	def credentials = parent.getCredentials() // library marker davegut.tapoCommon, line 122
	def uri = "http://${getDataValue("deviceIp")}/app" // library marker davegut.tapoCommon, line 123
	String cmdBody = """{"method": "login_device", "params": {"password": "${credentials.encPassword}", "username": "${credentials.encUsername}"}, "requestTimeMils": 0}""" // library marker davegut.tapoCommon, line 124
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdBody, cryptoArray)]] // library marker davegut.tapoCommon, line 125
	def respData = syncPost(uri, reqBody, cookie) // library marker davegut.tapoCommon, line 126
	if (respData.respStatus == "OK") { // library marker davegut.tapoCommon, line 127
		if (respData.data.error_code == 0) { // library marker davegut.tapoCommon, line 128
			def cmdResp = decrypt(respData.data.result.response, cryptoArray) // library marker davegut.tapoCommon, line 129
			cmdResp = new JsonSlurper().parseText(cmdResp) // library marker davegut.tapoCommon, line 130
			if (cmdResp.error_code == 0) { // library marker davegut.tapoCommon, line 131
				tokenData << [respStatus: "OK", token: cmdResp.result.token] // library marker davegut.tapoCommon, line 132
			} else { // library marker davegut.tapoCommon, line 133
				tokenData << [respStatus: "Error in cmdResp", data: cmdResp] // library marker davegut.tapoCommon, line 134
			} // library marker davegut.tapoCommon, line 135
		} else { // library marker davegut.tapoCommon, line 136
			tokenData << [respStatus: "Error in respData,data", data: respData.data] // library marker davegut.tapoCommon, line 137
		} // library marker davegut.tapoCommon, line 138
	} else { // library marker davegut.tapoCommon, line 139
		tokenData << [respStatus: "Error in respData", data: respData] // library marker davegut.tapoCommon, line 140
	} // library marker davegut.tapoCommon, line 141
	return tokenData // library marker davegut.tapoCommon, line 142
} // library marker davegut.tapoCommon, line 143

def refresh() { // library marker davegut.tapoCommon, line 145
	logDebug("refresh") // library marker davegut.tapoCommon, line 146
	state.commsCheck = true // library marker davegut.tapoCommon, line 147
	runIn(10, checkForError) // library marker davegut.tapoCommon, line 148
	securePassthrough([method: "get_device_info"], true, "refresh") // library marker davegut.tapoCommon, line 149
} // library marker davegut.tapoCommon, line 150

def checkForError() { // library marker davegut.tapoCommon, line 152
	if (state.commsCheck && device.currentValue("commsError") == "OK") { // library marker davegut.tapoCommon, line 153
		updateDeviceIps(device.getDeviceNetworkId()) // library marker davegut.tapoCommon, line 154
		//	Try device login to generate error.  Will only do this once. // library marker davegut.tapoCommon, line 155
		def loginStatus = deviceLogin() // library marker davegut.tapoCommon, line 156
		if (!loginStatus.cookie) { // library marker davegut.tapoCommon, line 157
			updateAttr("commsError", "failedHandshake") // library marker davegut.tapoCommon, line 158
			log.error "COMMS ERROR: <b>Check Public/Private keys and device IP</b>" // library marker davegut.tapoCommon, line 159
			state.COMMSERROR = "<b>Check Public/Private keys and device IP</b>" // library marker davegut.tapoCommon, line 160
		} else if (!loginStatus.deviceToken) { // library marker davegut.tapoCommon, line 161
			updateAttr("commsError", "failedLogin") // library marker davegut.tapoCommon, line 162
			log.error "COMMS ERROR: <b>Check login credentials</b>" // library marker davegut.tapoCommon, line 163
			state.COMMSERROR = "<b>Check login credentials</b>" // library marker davegut.tapoCommon, line 164
		} else { // library marker davegut.tapoCommon, line 165
//			updated() // library marker davegut.tapoCommon, line 166
		} // library marker davegut.tapoCommon, line 167
	} else if (state.commsCheck == false && device.currentValue("commsError") != "OK") { // library marker davegut.tapoCommon, line 168
		updateAttr("commsError", "OK") // library marker davegut.tapoCommon, line 169
		logInfo("checkForError: Executing updated() to assure login methods are properly scheduled") // library marker davegut.tapoCommon, line 170
		state.remove("COMMSERROR") // library marker davegut.tapoCommon, line 171
		//	Assure all scheduled events are properly scheduled. // library marker davegut.tapoCommon, line 172
		updated() // library marker davegut.tapoCommon, line 173
	} // library marker davegut.tapoCommon, line 174
} // library marker davegut.tapoCommon, line 175

//	===== Utilities ===== // library marker davegut.tapoCommon, line 177
def getAesKey() { // library marker davegut.tapoCommon, line 178
	return new JsonSlurper().parseText(aesKey) // library marker davegut.tapoCommon, line 179
} // library marker davegut.tapoCommon, line 180

def publicPem() { // library marker davegut.tapoCommon, line 182
	def pem = "-----BEGIN PUBLIC KEY-----\n${parent.publicKey()}-----END PUBLIC KEY-----\n" // library marker davegut.tapoCommon, line 183
	return pem // library marker davegut.tapoCommon, line 184
} // library marker davegut.tapoCommon, line 185

// ~~~~~ end include (1326) davegut.tapoCommon ~~~~~

// ~~~~~ start include (1327) davegut.tapoComms ~~~~~
library ( // library marker davegut.tapoComms, line 1
	name: "tapoComms", // library marker davegut.tapoComms, line 2
	namespace: "davegut", // library marker davegut.tapoComms, line 3
	author: "Dave Gutheinz", // library marker davegut.tapoComms, line 4
	description: "Tapo Communications", // library marker davegut.tapoComms, line 5
	category: "utilities", // library marker davegut.tapoComms, line 6
	documentationLink: "" // library marker davegut.tapoComms, line 7
) // library marker davegut.tapoComms, line 8
import groovy.json.JsonOutput // library marker davegut.tapoComms, line 9
import groovy.json.JsonBuilder // library marker davegut.tapoComms, line 10
import groovy.json.JsonSlurper // library marker davegut.tapoComms, line 11
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tapoComms, line 12
import javax.crypto.spec.SecretKeySpec // library marker davegut.tapoComms, line 13
import javax.crypto.spec.IvParameterSpec // library marker davegut.tapoComms, line 14
import javax.crypto.Cipher // library marker davegut.tapoComms, line 15
import java.security.KeyFactory // library marker davegut.tapoComms, line 16

def sendMultiCmd(request, async, method=null, action="distData") { // library marker davegut.tapoComms, line 18
	Map cmdBody = [ // library marker davegut.tapoComms, line 19
		method: "multipleRequest", // library marker davegut.tapoComms, line 20
		params: [ // library marker davegut.tapoComms, line 21
			requests:[ // library marker davegut.tapoComms, line 22
				request, // library marker davegut.tapoComms, line 23
				[method: "get_device_info"]]]] // library marker davegut.tapoComms, line 24
	securePassthrough(cmdBody, async, method, action) // library marker davegut.tapoComms, line 25
} // library marker davegut.tapoComms, line 26

def securePassthrough(cmdBody, async=true, reqData=null, action = "distData") { // library marker davegut.tapoComms, line 28
	logDebug("securePassthrough: [cmdBody: ${cmdBody}, async: ${async}]") // library marker davegut.tapoComms, line 29
	cmdBody = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tapoComms, line 30
	def uri = "http://${getDataValue("deviceIp")}/app?token=${deviceToken}" // library marker davegut.tapoComms, line 31
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdBody, getAesKey())]] // library marker davegut.tapoComms, line 32
	if (async) { // library marker davegut.tapoComms, line 33
		asyncPost(uri, reqBody, action, deviceCookie, reqData)	 // library marker davegut.tapoComms, line 34
	} else { // library marker davegut.tapoComms, line 35
		def respData = syncPost(uri, reqBody, deviceCookie) // library marker davegut.tapoComms, line 36
		Map logData = [reqDni: reqDni] // library marker davegut.tapoComms, line 37
		def cmdResp = "ERROR" // library marker davegut.tapoComms, line 38
		if (respData.respStatus == "OK") { // library marker davegut.tapoComms, line 39
			logData << [respStatus: "OK"] // library marker davegut.tapoComms, line 40
			respData = respData.data.result.response // library marker davegut.tapoComms, line 41
			cmdResp = new JsonSlurper().parseText(decrypt(respData, getAesKey())) // library marker davegut.tapoComms, line 42
		} else { // library marker davegut.tapoComms, line 43
			logData << respData // library marker davegut.tapoComms, line 44
		} // library marker davegut.tapoComms, line 45
		if (logData.respStatus == "OK") { // library marker davegut.tapoComms, line 46
			logDebug("securePassthrough - SYNC: ${logData}") // library marker davegut.tapoComms, line 47
		} else { // library marker davegut.tapoComms, line 48
			logWarn("securePassthrough - SYNC: ${logData}") // library marker davegut.tapoComms, line 49
		} // library marker davegut.tapoComms, line 50
		return cmdResp // library marker davegut.tapoComms, line 51
	} // library marker davegut.tapoComms, line 52
} // library marker davegut.tapoComms, line 53

//	===== AES Methods ===== // library marker davegut.tapoComms, line 55
def encrypt(plainText, keyData) { // library marker davegut.tapoComms, line 56
	byte[] keyenc = keyData[0..15] // library marker davegut.tapoComms, line 57
	byte[] ivenc = keyData[16..31] // library marker davegut.tapoComms, line 58

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tapoComms, line 60
	SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.tapoComms, line 61
	IvParameterSpec iv = new IvParameterSpec(ivenc) // library marker davegut.tapoComms, line 62
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tapoComms, line 63
	String result = cipher.doFinal(plainText.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tapoComms, line 64
	return result.replace("\r\n","") // library marker davegut.tapoComms, line 65
} // library marker davegut.tapoComms, line 66

def decrypt(cypherText, keyData) { // library marker davegut.tapoComms, line 68
	byte[] keyenc = keyData[0..15] // library marker davegut.tapoComms, line 69
	byte[] ivenc = keyData[16..31] // library marker davegut.tapoComms, line 70

    byte[] decodedBytes = cypherText.decodeBase64() // library marker davegut.tapoComms, line 72
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tapoComms, line 73
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.tapoComms, line 74
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivenc)) // library marker davegut.tapoComms, line 75
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tapoComms, line 76
	return result // library marker davegut.tapoComms, line 77
} // library marker davegut.tapoComms, line 78

//	===== Sync comms for device update ===== // library marker davegut.tapoComms, line 80
def syncPost(uri, reqBody, cookie=null) { // library marker davegut.tapoComms, line 81
	def reqParams = [ // library marker davegut.tapoComms, line 82
		uri: uri, // library marker davegut.tapoComms, line 83
		headers: [ // library marker davegut.tapoComms, line 84
			Cookie: cookie // library marker davegut.tapoComms, line 85
		], // library marker davegut.tapoComms, line 86
		body : new JsonBuilder(reqBody).toString() // library marker davegut.tapoComms, line 87
	] // library marker davegut.tapoComms, line 88
	logDebug("syncPost: [cmdParams: ${reqParams}]") // library marker davegut.tapoComms, line 89
	Map respData = [:] // library marker davegut.tapoComms, line 90
	try { // library marker davegut.tapoComms, line 91
		httpPostJson(reqParams) {resp -> // library marker davegut.tapoComms, line 92
			respData << [status: resp.status, data: resp.data] // library marker davegut.tapoComms, line 93
			if (resp.status == 200) { // library marker davegut.tapoComms, line 94
				respData << [respStatus: "OK", headers: resp.headers] // library marker davegut.tapoComms, line 95
			} else { // library marker davegut.tapoComms, line 96
				respData << [respStatus: "Return Error"] // library marker davegut.tapoComms, line 97
			} // library marker davegut.tapoComms, line 98
		} // library marker davegut.tapoComms, line 99
	} catch (e) { // library marker davegut.tapoComms, line 100
		respData << [status: "HTTP Failed", data: e] // library marker davegut.tapoComms, line 101
	} // library marker davegut.tapoComms, line 102
	return respData // library marker davegut.tapoComms, line 103
} // library marker davegut.tapoComms, line 104

def asyncPost(uri, reqBody, parseMethod, cookie=null, reqData=null) { // library marker davegut.tapoComms, line 106
	Map logData = [:] // library marker davegut.tapoComms, line 107
	def reqParams = [ // library marker davegut.tapoComms, line 108
		uri: uri, // library marker davegut.tapoComms, line 109
		requestContentType: 'application/json', // library marker davegut.tapoComms, line 110
		contentType: 'application/json', // library marker davegut.tapoComms, line 111
		headers: [ // library marker davegut.tapoComms, line 112
			Cookie: cookie // library marker davegut.tapoComms, line 113
		], // library marker davegut.tapoComms, line 114
		timeout: 5, // library marker davegut.tapoComms, line 115
		body : new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.tapoComms, line 116
	] // library marker davegut.tapoComms, line 117
	try { // library marker davegut.tapoComms, line 118
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.tapoComms, line 119
//		asynchttpPost(parseMethod, reqParams, reqData) // library marker davegut.tapoComms, line 120
		logData << [status: "OK"] // library marker davegut.tapoComms, line 121
	} catch (e) { // library marker davegut.tapoComms, line 122
		logData << [status: e, reqParams: reqParams] // library marker davegut.tapoComms, line 123
	} // library marker davegut.tapoComms, line 124
	if (logData.status == "OK") { // library marker davegut.tapoComms, line 125
		logDebug("asyncPost: ${logData}") // library marker davegut.tapoComms, line 126
	} else { // library marker davegut.tapoComms, line 127
		logWarn("asyncPost: ${logData}") // library marker davegut.tapoComms, line 128
	} // library marker davegut.tapoComms, line 129
} // library marker davegut.tapoComms, line 130

//	===== Initial Parse Methods ===== // library marker davegut.tapoComms, line 132

def distData(resp, data) { // library marker davegut.tapoComms, line 134
	def cmdResp = parseData(resp) // library marker davegut.tapoComms, line 135
	if (cmdResp.status == "OK") { // library marker davegut.tapoComms, line 136
		switch(data.data) { // library marker davegut.tapoComms, line 137
			case "pollChildren": // library marker davegut.tapoComms, line 138
				childPollParse(cmdResp.cmdResp.result.child_device_list) // library marker davegut.tapoComms, line 139
				break // library marker davegut.tapoComms, line 140
			case "refreshChildren": // library marker davegut.tapoComms, line 141
				childRefreshParse(cmdResp.cmdResp.result.child_device_list) // library marker davegut.tapoComms, line 142
				break // library marker davegut.tapoComms, line 143
			default: // library marker davegut.tapoComms, line 144
				def devData = cmdResp.cmdResp.result // library marker davegut.tapoComms, line 145
				if (devData.responses) { // library marker davegut.tapoComms, line 146
					devData = devData.responses.find{it.method == "get_device_info"}.result // library marker davegut.tapoComms, line 147
					deviceParse(devData, data.data) // library marker davegut.tapoComms, line 148
				} // library marker davegut.tapoComms, line 149
		} // library marker davegut.tapoComms, line 150
	} // library marker davegut.tapoComms, line 151
} // library marker davegut.tapoComms, line 152

def parseData(resp) { // library marker davegut.tapoComms, line 154
	def logData = [:] // library marker davegut.tapoComms, line 155
	try { // library marker davegut.tapoComms, line 156
		logData << [respDataLength: resp.data.length()] // library marker davegut.tapoComms, line 157
		resp = new JsonSlurper().parseText(resp.data) // library marker davegut.tapoComms, line 158
		def cmdResp = new JsonSlurper().parseText(decrypt(resp.result.response, getAesKey())) // library marker davegut.tapoComms, line 159
		logData << [cmdResp: cmdResp] // library marker davegut.tapoComms, line 160
		if (cmdResp.error_code == 0) { // library marker davegut.tapoComms, line 161
			logData << [status: "OK"] // library marker davegut.tapoComms, line 162
		} else { // library marker davegut.tapoComms, line 163
			logData << [status: "Error from device response"] // library marker davegut.tapoComms, line 164
		} // library marker davegut.tapoComms, line 165
		state.commsCheck = false // library marker davegut.tapoComms, line 166
	} catch (e) { // library marker davegut.tapoComms, line 167
		logData << [status: e, data: resp] // library marker davegut.tapoComms, line 168
	} // library marker davegut.tapoComms, line 169
	if (logData.status == "OK") { // library marker davegut.tapoComms, line 170
		logDebug("parseData: ${logData}") // library marker davegut.tapoComms, line 171
	} else { // library marker davegut.tapoComms, line 172
		logWarn("parseData: ${logData}") // library marker davegut.tapoComms, line 173
	} // library marker davegut.tapoComms, line 174
	return logData // library marker davegut.tapoComms, line 175
} // library marker davegut.tapoComms, line 176

// ~~~~~ end include (1327) davegut.tapoComms ~~~~~

// ~~~~~ start include (1320) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

preferences { // library marker davegut.Logging, line 10
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.Logging, line 11
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.Logging, line 12
	input ("traceLog", "bool", title: "Enable trace logging as directed by developer", defaultValue: false) // library marker davegut.Logging, line 13
} // library marker davegut.Logging, line 14

def updateAttr(attr, value) { // library marker davegut.Logging, line 16
	if (device.currentValue(attr) != value) { // library marker davegut.Logging, line 17
		sendEvent(name: attr, value: value) // library marker davegut.Logging, line 18
	} // library marker davegut.Logging, line 19
} // library marker davegut.Logging, line 20

def setLogsOff() { // library marker davegut.Logging, line 22
	def logData = [logEnagle: logEnable, infoLog: infoLog, traceLog:traceLog] // library marker davegut.Logging, line 23
	if (logEnable) { // library marker davegut.Logging, line 24
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 25
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 26
	} // library marker davegut.Logging, line 27
	if (traceLog) { // library marker davegut.Logging, line 28
		runIn(1800, traceLogOff) // library marker davegut.Logging, line 29
		logData << [traceLogOff: "scheduled"] // library marker davegut.Logging, line 30
	} // library marker davegut.Logging, line 31
	return logData // library marker davegut.Logging, line 32
} // library marker davegut.Logging, line 33

//	Logging during development // library marker davegut.Logging, line 35
def listAttributes(trace = false) { // library marker davegut.Logging, line 36
	def attrs = device.getSupportedAttributes() // library marker davegut.Logging, line 37
	def attrList = [:] // library marker davegut.Logging, line 38
	attrs.each { // library marker davegut.Logging, line 39
		def val = device.currentValue("${it}") // library marker davegut.Logging, line 40
		attrList << ["${it}": val] // library marker davegut.Logging, line 41
	} // library marker davegut.Logging, line 42
	if (trace == true) { // library marker davegut.Logging, line 43
		logInfo("Attributes: ${attrList}") // library marker davegut.Logging, line 44
	} else { // library marker davegut.Logging, line 45
		logDebug("Attributes: ${attrList}") // library marker davegut.Logging, line 46
	} // library marker davegut.Logging, line 47
} // library marker davegut.Logging, line 48

def flexLog(method, respData, err) { // library marker davegut.Logging, line 50
	if (err == true) { // library marker davegut.Logging, line 51
		logWarn("${method}: ${respData}") // library marker davegut.Logging, line 52
	} else { // library marker davegut.Logging, line 53
		logInfo("${method}: ${respData}") // library marker davegut.Logging, line 54
	} // library marker davegut.Logging, line 55
} // library marker davegut.Logging, line 56

def logTrace(msg){ // library marker davegut.Logging, line 58
	if (traceLog == true) { // library marker davegut.Logging, line 59
		log.trace "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 60
	} // library marker davegut.Logging, line 61
} // library marker davegut.Logging, line 62

def traceLogOff() { // library marker davegut.Logging, line 64
	device.updateSetting("traceLog", [type:"bool", value: false]) // library marker davegut.Logging, line 65
	logInfo("traceLogOff") // library marker davegut.Logging, line 66
} // library marker davegut.Logging, line 67

def logInfo(msg) {  // library marker davegut.Logging, line 69
	if (textEnable || infoLog) { // library marker davegut.Logging, line 70
		log.info "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 71
	} // library marker davegut.Logging, line 72
} // library marker davegut.Logging, line 73

def debugLogOff() { // library marker davegut.Logging, line 75
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 76
	logInfo("debugLogOff") // library marker davegut.Logging, line 77
} // library marker davegut.Logging, line 78

def logDebug(msg) { // library marker davegut.Logging, line 80
	if (logEnable || debugLog) { // library marker davegut.Logging, line 81
		log.debug "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 82
	} // library marker davegut.Logging, line 83
} // library marker davegut.Logging, line 84

def logWarn(msg) { log.warn "${device.displayName}-${driverVer()}: ${msg}" } // library marker davegut.Logging, line 86

def logError(msg) { log.error "${device.displayName}-${driverVer()}: ${msg}" }			   // library marker davegut.Logging, line 88

// ~~~~~ end include (1320) davegut.Logging ~~~~~
