/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Name change to TpLink Lightstrip.

Verified on L900-5(US).
=================================================================================================*/
//	=====	NAMESPACE	in library davegut.Logging	============

metadata {
	definition (name: "TpLink Lightstrip", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_lightStrip.groovy")
	{
		capability "Light"
		command "setLightingEffect", [[
			name: "Lighting Effect",
			constraints: effectList(),
			type: "ENUM"
			]]
		attribute "effectName", "string"
		attribute "effectEnable", "string"
	}
	preferences {
		commonPreferences()
	}
}

def installed() {
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	logInfo(logData)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}

def setLightingEffect(effect) {
	Map effData = getEffect(effect)
	List requests  = [[
		method: "set_lighting_effect",
		params: effData
	]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "setLightingEffect", "parseUpdates")
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

def parse_get_device_info(result, data) {
	Map logData = [method: "parse_get_device_info", data: data]
	if (result.device_on != null) {
		def onOff = "off"
		if (result.device_on == true) { onOff = "on" }
		sendEvent(name: "switch", value: onOff, type: state.eventType)
		state.eventType = "physical"
		logData << [switch: onOff]
	}
	if (result.brightness != null) {
		updateAttr("level", result.brightness)
		logData << [level: result.brightness]
	}
	if (result.color_temp != null) {
		if (result.color_temp == 0) {
			updateAttr("colorMode", "COLOR")
			def hubHue = (result.hue / 3.6).toInteger()
			updateAttr("hue", hubHue)
			updateAttr("saturation", result.saturation)
			updateAttr("color", "[hue: ${hubHue}, saturation: ${result.saturation}]")
			def colorName = convertHueToGenericColorName(hubHue)
			updateAttr("colorName", colorName)
			def rgb = hubitat.helper.ColorUtils.hsvToRGB([hubHue,
														  result.saturation,
														  result.brightness])
			updateAttr("RGB", rgb)
			updateAttr("colorTemperature", 0)
			logData << [colorMode: "COLOR", colorName: colorName, color: color, 
						RGB: RGB, colorTemperature: 0]
		} else {
			updateAttr("colorMode", "CT")
			updateAttr("colorTemperature", result.color_temp)
			def colorName = convertTemperatureToGenericColorName(result.color_temp.toInteger())
			updateAttr("hue", 0)
			updateAttr("saturation", 0)
			updateAttr("colorName", colorName)
			updateAttr("color", "[:]")
			updateAttr("RGB", "[]")
			logData << [colorMode: "CT", colorName: colorName, color: color, 
						RGB: RGB, colorTemperature: result.color_temp]
		}
	}
	if (result.lighting_effect != null) {
		if (result.lighting_effect.enable == 0) { 
			updateAttr("effectName", " ")
			updateAttr("effectEnable", "false")
			logData << [effectName: "", effectEnable: "false"]
		} else {
			updateAttr("effectName", result.lighting_effect.name)
			updateAttr("effectEnable", "true")
			logData << [effectName: result.lighting_effect.name, effectEnable: "true"]
		}
	}
	logDebug(logData)
}

//	Library Inclusion

Level






// ~~~~~ start include (32) davegut.tpLinkCapSwitch ~~~~~
library ( // library marker davegut.tpLinkCapSwitch, line 1
	name: "tpLinkCapSwitch", // library marker davegut.tpLinkCapSwitch, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapSwitch, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapSwitch, line 4
	description: "Hubitat capability Switch methods", // library marker davegut.tpLinkCapSwitch, line 5
	category: "utilities", // library marker davegut.tpLinkCapSwitch, line 6
	documentationLink: "" // library marker davegut.tpLinkCapSwitch, line 7
) // library marker davegut.tpLinkCapSwitch, line 8

capability "Switch" // library marker davegut.tpLinkCapSwitch, line 10

def on() { setPower(true) } // library marker davegut.tpLinkCapSwitch, line 12

def off() { setPower(false) } // library marker davegut.tpLinkCapSwitch, line 14

def setPower(onOff) { // library marker davegut.tpLinkCapSwitch, line 16
	state.eventType = "digital" // library marker davegut.tpLinkCapSwitch, line 17
	logDebug("setPower: [device_on: ${onOff}]") // library marker davegut.tpLinkCapSwitch, line 18
	List requests = [[ // library marker davegut.tpLinkCapSwitch, line 19
		method: "set_device_info", // library marker davegut.tpLinkCapSwitch, line 20
		params: [device_on: onOff]]] // library marker davegut.tpLinkCapSwitch, line 21
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCapSwitch, line 22
	sendDevCmd(requests, "setPower", "parseUpdates")  // library marker davegut.tpLinkCapSwitch, line 23
	if (getDataValue("type") == "Plug EM") { // library marker davegut.tpLinkCapSwitch, line 24
		runIn(5, plugEmRefresh) // library marker davegut.tpLinkCapSwitch, line 25
	} // library marker davegut.tpLinkCapSwitch, line 26
} // library marker davegut.tpLinkCapSwitch, line 27

// ~~~~~ end include (32) davegut.tpLinkCapSwitch ~~~~~

// ~~~~~ start include (33) davegut.tpLinkCapSwitchLevel ~~~~~
library ( // library marker davegut.tpLinkCapSwitchLevel, line 1
	name: "tpLinkCapSwitchLevel", // library marker davegut.tpLinkCapSwitchLevel, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapSwitchLevel, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapSwitchLevel, line 4
	description: "Hubitat capability Switch Level and Change Level methods", // library marker davegut.tpLinkCapSwitchLevel, line 5
	category: "utilities", // library marker davegut.tpLinkCapSwitchLevel, line 6
	documentationLink: "" // library marker davegut.tpLinkCapSwitchLevel, line 7
) // library marker davegut.tpLinkCapSwitchLevel, line 8

capability "Switch Level" // library marker davegut.tpLinkCapSwitchLevel, line 10
capability "Change Level" // library marker davegut.tpLinkCapSwitchLevel, line 11

def setLevel(level, transTime=0) { // library marker davegut.tpLinkCapSwitchLevel, line 13
	logDebug([method: "setLevel", level: level, transTime: transTime]) // library marker davegut.tpLinkCapSwitchLevel, line 14
	if (level == null) { level = device.currentValue("level") toInteger() } // library marker davegut.tpLinkCapSwitchLevel, line 15
	if (transTime < 0) { transTime = 0 } // library marker davegut.tpLinkCapSwitchLevel, line 16
	if (transTime > 0) { // library marker davegut.tpLinkCapSwitchLevel, line 17
		startLevelTransition(level, transTime) // library marker davegut.tpLinkCapSwitchLevel, line 18
	} else { // library marker davegut.tpLinkCapSwitchLevel, line 19
		if (level == 0) { // library marker davegut.tpLinkCapSwitchLevel, line 20
			off() // library marker davegut.tpLinkCapSwitchLevel, line 21
		} else { // library marker davegut.tpLinkCapSwitchLevel, line 22
			List requests = [[ // library marker davegut.tpLinkCapSwitchLevel, line 23
				method: "set_device_info", // library marker davegut.tpLinkCapSwitchLevel, line 24
				params: [ // library marker davegut.tpLinkCapSwitchLevel, line 25
					brightness: level // library marker davegut.tpLinkCapSwitchLevel, line 26
				]]] // library marker davegut.tpLinkCapSwitchLevel, line 27
			requests << [method: "get_device_info"] // library marker davegut.tpLinkCapSwitchLevel, line 28
			sendDevCmd(requests, "setLevel", "parseUpdates") // library marker davegut.tpLinkCapSwitchLevel, line 29
		} // library marker davegut.tpLinkCapSwitchLevel, line 30
	} // library marker davegut.tpLinkCapSwitchLevel, line 31
} // library marker davegut.tpLinkCapSwitchLevel, line 32

def startLevelTransition(level, transTime) { // library marker davegut.tpLinkCapSwitchLevel, line 34
	def startTime = (now()/1000).toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 35
	def endTime = startTime + transTime // library marker davegut.tpLinkCapSwitchLevel, line 36
	Map transData = [endTime: endTime, targetLevel: level, cmdIncr: 180] // library marker davegut.tpLinkCapSwitchLevel, line 37
	//	Command increment derived from experimentation with Tapo Lan devices. // library marker davegut.tpLinkCapSwitchLevel, line 38
	def totalIncrs = (transTime * 5).toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 39

	//	Level Increment (based on total level Change, cmdIncr, and transTime) // library marker davegut.tpLinkCapSwitchLevel, line 41
	def currLevel = device.currentValue("level").toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 42
	def levelChange = level - currLevel // library marker davegut.tpLinkCapSwitchLevel, line 43
	def levelIncr = levelChange/totalIncrs // library marker davegut.tpLinkCapSwitchLevel, line 44
	if (levelIncr < 0 ) { levelIncr = (levelIncr - 0.5).toInteger() } // library marker davegut.tpLinkCapSwitchLevel, line 45
	else { levelIncr = (levelIncr + 0.5).toInteger() } // library marker davegut.tpLinkCapSwitchLevel, line 46
	transData << [currLevel: currLevel, levelIncr: levelIncr] // library marker davegut.tpLinkCapSwitchLevel, line 47

	logDebug([method: "startCtTransition", transData: transData]) // library marker davegut.tpLinkCapSwitchLevel, line 49
	doLevelTransition(transData) // library marker davegut.tpLinkCapSwitchLevel, line 50
} // library marker davegut.tpLinkCapSwitchLevel, line 51

def doLevelTransition(Map transData) { // library marker davegut.tpLinkCapSwitchLevel, line 53
	def newLevel = transData.targetLevel // library marker davegut.tpLinkCapSwitchLevel, line 54
	def doAgain = true // library marker davegut.tpLinkCapSwitchLevel, line 55
	def curTime = (now()/1000).toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 56
	if (newLevel == transData.currLevel || curTime >= transData.endTime) { // library marker davegut.tpLinkCapSwitchLevel, line 57
		doAgain = false // library marker davegut.tpLinkCapSwitchLevel, line 58
	} else { // library marker davegut.tpLinkCapSwitchLevel, line 59
		newLevel = transData.currLevel + transData.levelIncr // library marker davegut.tpLinkCapSwitchLevel, line 60
		if (transData.levelIncr >= 0 && newLevel > transData.targetLevel) { // library marker davegut.tpLinkCapSwitchLevel, line 61
			newLevel = transData.targetLevel // library marker davegut.tpLinkCapSwitchLevel, line 62
		} else if (transData.levelIncr < 0 && newLevel < transData.targetLevel) { // library marker davegut.tpLinkCapSwitchLevel, line 63
			newLevel = transData.targetLevel // library marker davegut.tpLinkCapSwitchLevel, line 64
		} // library marker davegut.tpLinkCapSwitchLevel, line 65
	} // library marker davegut.tpLinkCapSwitchLevel, line 66
	transData << [currLevel: newLevel] // library marker davegut.tpLinkCapSwitchLevel, line 67
	if (currLevel != 0) { // library marker davegut.tpLinkCapSwitchLevel, line 68
		sendSingleCmd([method: "set_device_info", params: [brightness: newLevel]], // library marker davegut.tpLinkCapSwitchLevel, line 69
				  "doLevelTransition", "nullParse") // library marker davegut.tpLinkCapSwitchLevel, line 70
		if (doAgain == true) { // library marker davegut.tpLinkCapSwitchLevel, line 71
			runInMillis(transData.cmdIncr, doLevelTransition, [data: transData]) // library marker davegut.tpLinkCapSwitchLevel, line 72
		} else { // library marker davegut.tpLinkCapSwitchLevel, line 73
			runInMillis(500, setLevel, [data: transData.targetLevel]) // library marker davegut.tpLinkCapSwitchLevel, line 74
		} // library marker davegut.tpLinkCapSwitchLevel, line 75
	} else { // library marker davegut.tpLinkCapSwitchLevel, line 76
		off() // library marker davegut.tpLinkCapSwitchLevel, line 77
	} // library marker davegut.tpLinkCapSwitchLevel, line 78
} // library marker davegut.tpLinkCapSwitchLevel, line 79

def startLevelChange(direction) { // library marker davegut.tpLinkCapSwitchLevel, line 81
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]") // library marker davegut.tpLinkCapSwitchLevel, line 82
	if (direction == "up") { levelUp() } // library marker davegut.tpLinkCapSwitchLevel, line 83
	else { levelDown() } // library marker davegut.tpLinkCapSwitchLevel, line 84
} // library marker davegut.tpLinkCapSwitchLevel, line 85

def stopLevelChange() { // library marker davegut.tpLinkCapSwitchLevel, line 87
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]") // library marker davegut.tpLinkCapSwitchLevel, line 88
	unschedule(levelUp) // library marker davegut.tpLinkCapSwitchLevel, line 89
	unschedule(levelDown) // library marker davegut.tpLinkCapSwitchLevel, line 90
} // library marker davegut.tpLinkCapSwitchLevel, line 91

def levelUp() { // library marker davegut.tpLinkCapSwitchLevel, line 93
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 94
	if (curLevel != 100) { // library marker davegut.tpLinkCapSwitchLevel, line 95
		def newLevel = curLevel + 4 // library marker davegut.tpLinkCapSwitchLevel, line 96
		if (newLevel > 100) { newLevel = 100 } // library marker davegut.tpLinkCapSwitchLevel, line 97
		setLevel(newLevel) // library marker davegut.tpLinkCapSwitchLevel, line 98
		runIn(1, levelUp) // library marker davegut.tpLinkCapSwitchLevel, line 99
	} // library marker davegut.tpLinkCapSwitchLevel, line 100
} // library marker davegut.tpLinkCapSwitchLevel, line 101

def levelDown() { // library marker davegut.tpLinkCapSwitchLevel, line 103
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.tpLinkCapSwitchLevel, line 104
	if (device.currentValue("switch") == "on") { // library marker davegut.tpLinkCapSwitchLevel, line 105
		def newLevel = curLevel - 4 // library marker davegut.tpLinkCapSwitchLevel, line 106
		if (newLevel <= 0) { off() } // library marker davegut.tpLinkCapSwitchLevel, line 107
		else { // library marker davegut.tpLinkCapSwitchLevel, line 108
			setLevel(newLevel) // library marker davegut.tpLinkCapSwitchLevel, line 109
			runIn(1, levelDown) // library marker davegut.tpLinkCapSwitchLevel, line 110
		} // library marker davegut.tpLinkCapSwitchLevel, line 111
	} // library marker davegut.tpLinkCapSwitchLevel, line 112
} // library marker davegut.tpLinkCapSwitchLevel, line 113

// ~~~~~ end include (33) davegut.tpLinkCapSwitchLevel ~~~~~

// ~~~~~ start include (31) davegut.tpLinkCapColorControl ~~~~~
library ( // library marker davegut.tpLinkCapColorControl, line 1
	name: "tpLinkCapColorControl", // library marker davegut.tpLinkCapColorControl, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapColorControl, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapColorControl, line 4
	description: "Hubitat capability ColorControl, Color Mode, and Color Temperature Methods.", // library marker davegut.tpLinkCapColorControl, line 5
	category: "utilities", // library marker davegut.tpLinkCapColorControl, line 6
	documentationLink: "" // library marker davegut.tpLinkCapColorControl, line 7
) // library marker davegut.tpLinkCapColorControl, line 8

capability "Color Control" // library marker davegut.tpLinkCapColorControl, line 10
capability "Color Temperature" // library marker davegut.tpLinkCapColorControl, line 11
capability "Color Mode" // library marker davegut.tpLinkCapColorControl, line 12

def setHue(hue){ // library marker davegut.tpLinkCapColorControl, line 14
	logDebug("setHue: ${hue}") // library marker davegut.tpLinkCapColorControl, line 15
	hue = (3.6 * hue).toInteger() // library marker davegut.tpLinkCapColorControl, line 16
	logDebug("setHue: ${hue}") // library marker davegut.tpLinkCapColorControl, line 17
	List requests = [[ // library marker davegut.tpLinkCapColorControl, line 18
		method: "set_device_info", // library marker davegut.tpLinkCapColorControl, line 19
		params: [ // library marker davegut.tpLinkCapColorControl, line 20
			device_on: true, // library marker davegut.tpLinkCapColorControl, line 21
			hue: hue, // library marker davegut.tpLinkCapColorControl, line 22
			color_temp: 0 // library marker davegut.tpLinkCapColorControl, line 23
		]]] // library marker davegut.tpLinkCapColorControl, line 24
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCapColorControl, line 25
	asyncSend(createMultiCmd(requests), "setHue", "parseUpdates") // library marker davegut.tpLinkCapColorControl, line 26
} // library marker davegut.tpLinkCapColorControl, line 27

def setSaturation(saturation) { // library marker davegut.tpLinkCapColorControl, line 29
	logDebug("setSatiratopm: ${saturation}") // library marker davegut.tpLinkCapColorControl, line 30
	List requests = [[ // library marker davegut.tpLinkCapColorControl, line 31
		method: "set_device_info", // library marker davegut.tpLinkCapColorControl, line 32
		params: [ // library marker davegut.tpLinkCapColorControl, line 33
			device_on: true, // library marker davegut.tpLinkCapColorControl, line 34
			saturation: saturation, // library marker davegut.tpLinkCapColorControl, line 35
			color_temp: 0 // library marker davegut.tpLinkCapColorControl, line 36
		]]] // library marker davegut.tpLinkCapColorControl, line 37
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCapColorControl, line 38
	asyncSend(createMultiCmd(requests), "setSaturation", "parseUpdates") // library marker davegut.tpLinkCapColorControl, line 39
} // library marker davegut.tpLinkCapColorControl, line 40

def setColor(color) { // library marker davegut.tpLinkCapColorControl, line 42
	logDebug("setColor: ${color}") // library marker davegut.tpLinkCapColorControl, line 43
	def level = color.level // library marker davegut.tpLinkCapColorControl, line 44
	if (level == 0) { level = device.currentValue("level") } // library marker davegut.tpLinkCapColorControl, line 45
	def hue = (3.6 * color.hue).toInteger() // library marker davegut.tpLinkCapColorControl, line 46
	List requests = [[ // library marker davegut.tpLinkCapColorControl, line 47
		method: "set_device_info", // library marker davegut.tpLinkCapColorControl, line 48
		params: [ // library marker davegut.tpLinkCapColorControl, line 49
			device_on: true, // library marker davegut.tpLinkCapColorControl, line 50
			hue: hue, // library marker davegut.tpLinkCapColorControl, line 51
			saturation: color.saturation, // library marker davegut.tpLinkCapColorControl, line 52
			brightness: level, // library marker davegut.tpLinkCapColorControl, line 53
			color_temp: 0 // library marker davegut.tpLinkCapColorControl, line 54
		]]] // library marker davegut.tpLinkCapColorControl, line 55
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCapColorControl, line 56
	asyncSend(createMultiCmd(requests), "setColor", "parseUpdates") // library marker davegut.tpLinkCapColorControl, line 57
} // library marker davegut.tpLinkCapColorControl, line 58

def setColorTemperature(colorTemp, level = device.currentValue("level").toInteger(), transTime = 0) { // library marker davegut.tpLinkCapColorControl, line 60
	logDebug([method: "setColorTemperature", level: level, colorTemp: colorTemp, transTime: transTime]) // library marker davegut.tpLinkCapColorControl, line 61
	def lowCt = getDataValue("ctLow").toInteger() // library marker davegut.tpLinkCapColorControl, line 62
	def highCt = getDataValue("ctHigh").toInteger() // library marker davegut.tpLinkCapColorControl, line 63
	if (colorTemp < lowCt) { colorTemp = lowCt } // library marker davegut.tpLinkCapColorControl, line 64
	else if (colorTemp > highCt) { colorTemp = highCt } // library marker davegut.tpLinkCapColorControl, line 65
	if (level == null) { level = device.currentValue("level") toInteger() } // library marker davegut.tpLinkCapColorControl, line 66
	if (transTime < 0) { transTime = 0 } // library marker davegut.tpLinkCapColorControl, line 67
	if (getDataValue("type") == "Color Bulb" && transTime > 0) { // library marker davegut.tpLinkCapColorControl, line 68
		def ctRange = highCt - lowCt // library marker davegut.tpLinkCapColorControl, line 69
		startCtTransition(colorTemp, ctRange, level, transTime) // library marker davegut.tpLinkCapColorControl, line 70
	} else { // library marker davegut.tpLinkCapColorControl, line 71
		if (level == 0) { // library marker davegut.tpLinkCapColorControl, line 72
			off() // library marker davegut.tpLinkCapColorControl, line 73
		} else { // library marker davegut.tpLinkCapColorControl, line 74
			List requests = [[ // library marker davegut.tpLinkCapColorControl, line 75
				method: "set_device_info", // library marker davegut.tpLinkCapColorControl, line 76
				params: [ // library marker davegut.tpLinkCapColorControl, line 77
					brightness: level, // library marker davegut.tpLinkCapColorControl, line 78
					color_temp: colorTemp // library marker davegut.tpLinkCapColorControl, line 79
				]]] // library marker davegut.tpLinkCapColorControl, line 80
			requests << [method: "get_device_info"] // library marker davegut.tpLinkCapColorControl, line 81
			asyncSend(createMultiCmd(requests), "setColorTemperature", "parseUpdates") // library marker davegut.tpLinkCapColorControl, line 82
		} // library marker davegut.tpLinkCapColorControl, line 83
	} // library marker davegut.tpLinkCapColorControl, line 84
} // library marker davegut.tpLinkCapColorControl, line 85

def startCtTransition(colorTemp, ctRange, level, transTime) { // library marker davegut.tpLinkCapColorControl, line 87
	def startTime = (now()/1000).toInteger() // library marker davegut.tpLinkCapColorControl, line 88
	def endTime = startTime + transTime // library marker davegut.tpLinkCapColorControl, line 89
	Map transData = [endTime: endTime, targetLevel: level, targetCt: colorTemp, cmdIncr: 180] // library marker davegut.tpLinkCapColorControl, line 90
	//	Command increment derived from experimentation with Tapo Lan devices. // library marker davegut.tpLinkCapColorControl, line 91
	def totalIncrs = (transTime * 5).toInteger() // library marker davegut.tpLinkCapColorControl, line 92

	//	CT Increment (based on total CT Change, cmdIncr, and transTime) // library marker davegut.tpLinkCapColorControl, line 94
	def currCt = device.currentValue("colorTemperature").toInteger() // library marker davegut.tpLinkCapColorControl, line 95
	def ctChange = colorTemp - currCt // library marker davegut.tpLinkCapColorControl, line 96
	def ctIncr = (0.5 + (ctChange/totalIncrs)).toInteger() // library marker davegut.tpLinkCapColorControl, line 97
	transData << [currCt: currCt, ctIncr: ctIncr] // library marker davegut.tpLinkCapColorControl, line 98

	//	Level Increment (based on total level Change, cmdIncr, and transTime) // library marker davegut.tpLinkCapColorControl, line 100
	def currLevel = device.currentValue("level").toInteger() // library marker davegut.tpLinkCapColorControl, line 101
	def levelChange = level - currLevel // library marker davegut.tpLinkCapColorControl, line 102
	def levelIncr = levelChange/totalIncrs // library marker davegut.tpLinkCapColorControl, line 103
	if (levelIncr < 0 ) { levelIncr = (levelIncr - 0.5).toInteger() } // library marker davegut.tpLinkCapColorControl, line 104
	else { levelIncr = (levelIncr + 0.5).toInteger() } // library marker davegut.tpLinkCapColorControl, line 105
	transData << [currLevel: currLevel, levelIncr: levelIncr] // library marker davegut.tpLinkCapColorControl, line 106

	logDebug([method: "startCtTransition", transData: transData]) // library marker davegut.tpLinkCapColorControl, line 108
	doCtTransition(transData) // library marker davegut.tpLinkCapColorControl, line 109
} // library marker davegut.tpLinkCapColorControl, line 110

def doCtTransition(Map transData) { // library marker davegut.tpLinkCapColorControl, line 112
	def newLevel = transData.targetLevel // library marker davegut.tpLinkCapColorControl, line 113
	def newCt = transData.targetCt // library marker davegut.tpLinkCapColorControl, line 114
	def doAgain = true // library marker davegut.tpLinkCapColorControl, line 115
	def curTime = (now()/1000).toInteger() // library marker davegut.tpLinkCapColorControl, line 116
	if (newLevel == transData.currLevel && newCt == transData.currCt) { // library marker davegut.tpLinkCapColorControl, line 117
		doAgain = false // library marker davegut.tpLinkCapColorControl, line 118
	} else if (curTime >= transData.endTime) { // library marker davegut.tpLinkCapColorControl, line 119
		doAgain = false // library marker davegut.tpLinkCapColorControl, line 120
	} else { // library marker davegut.tpLinkCapColorControl, line 121
		if (newLevel != transData.currLevel) { // library marker davegut.tpLinkCapColorControl, line 122
			newLevel = transData.currLevel + transData.levelIncr // library marker davegut.tpLinkCapColorControl, line 123
			if (transData.levelIncr >= 0 && newLevel > transData.targetLevel) { // library marker davegut.tpLinkCapColorControl, line 124
				newLevel = transData.targetLevel // library marker davegut.tpLinkCapColorControl, line 125
			} else if (transData.levelIncr < 0 && newLevel < transData.targetLevel) { // library marker davegut.tpLinkCapColorControl, line 126
				newLevel = transData.targetLevel // library marker davegut.tpLinkCapColorControl, line 127
			} // library marker davegut.tpLinkCapColorControl, line 128
		} // library marker davegut.tpLinkCapColorControl, line 129
		if (newCt != transData.currCt) { // library marker davegut.tpLinkCapColorControl, line 130
			newCt = transData.currCt + transData.ctIncr // library marker davegut.tpLinkCapColorControl, line 131
			if (transData.ctIncr >= 0 && newCt > transData.targetCt) { // library marker davegut.tpLinkCapColorControl, line 132
				newCt = transData.targetCt // library marker davegut.tpLinkCapColorControl, line 133
			} else if (transData.ctIncr < 0 && newCt < transData.targetCt) { // library marker davegut.tpLinkCapColorControl, line 134
				newCt = transData.targetCt // library marker davegut.tpLinkCapColorControl, line 135
			} // library marker davegut.tpLinkCapColorControl, line 136
		} // library marker davegut.tpLinkCapColorControl, line 137
	} // library marker davegut.tpLinkCapColorControl, line 138
	transData << [currLevel: newLevel, currCt: newCt] // library marker davegut.tpLinkCapColorControl, line 139
	if (currLevel != 0) { // library marker davegut.tpLinkCapColorControl, line 140
		asyncSend([method: "set_device_info", params: [brightness: newLevel, color_temp: newCt]], // library marker davegut.tpLinkCapColorControl, line 141
				  "doCtTransition", "nullParse") // library marker davegut.tpLinkCapColorControl, line 142
		if (doAgain == true) { // library marker davegut.tpLinkCapColorControl, line 143
			runInMillis(transData.cmdIncr, doCtTransition, [data: transData]) // library marker davegut.tpLinkCapColorControl, line 144
		} else { // library marker davegut.tpLinkCapColorControl, line 145
			runInMillis(500, setLevel, [data: transData.targetLevel]) // library marker davegut.tpLinkCapColorControl, line 146
		} // library marker davegut.tpLinkCapColorControl, line 147
	} else { // library marker davegut.tpLinkCapColorControl, line 148
		off() // library marker davegut.tpLinkCapColorControl, line 149
	} // library marker davegut.tpLinkCapColorControl, line 150
} // library marker davegut.tpLinkCapColorControl, line 151

// ~~~~~ end include (31) davegut.tpLinkCapColorControl ~~~~~

// ~~~~~ start include (35) davegut.tpLinkCommon ~~~~~
library ( // library marker davegut.tpLinkCommon, line 1
	name: "tpLinkCommon", // library marker davegut.tpLinkCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCommon, line 4
	description: "Common driver methods including capability Refresh and Configuration methods", // library marker davegut.tpLinkCommon, line 5
	category: "utilities", // library marker davegut.tpLinkCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkCommon, line 7
) // library marker davegut.tpLinkCommon, line 8

capability "Refresh" // library marker davegut.tpLinkCommon, line 10
capability "Configuration" // library marker davegut.tpLinkCommon, line 11
attribute "commsError", "string" // library marker davegut.tpLinkCommon, line 12

def commonPreferences() { // library marker davegut.tpLinkCommon, line 14
	List pollOptions = ["5 sec", "10 sec", "30 sec", "5 min", "10 min", "15 min", "30 min"] // library marker davegut.tpLinkCommon, line 15
	input ("pollInterval", "enum", title: "Poll/Refresh Interval", // library marker davegut.tpLinkCommon, line 16
		   options: pollOptions, defaultValue: "30 min") // library marker davegut.tpLinkCommon, line 17
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkCommon, line 18
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkCommon, line 19
} // library marker davegut.tpLinkCommon, line 20

def commonInstalled() { // library marker davegut.tpLinkCommon, line 22
	updateAttr("commsError", "false") // library marker davegut.tpLinkCommon, line 23
	state.errorCount = 0 // library marker davegut.tpLinkCommon, line 24
	state.lastCmd = "" // library marker davegut.tpLinkCommon, line 25
	state.eventType = "digital" // library marker davegut.tpLinkCommon, line 26
	Map logData = [configure: configure(false)] // library marker davegut.tpLinkCommon, line 27
	return logData // library marker davegut.tpLinkCommon, line 28
} // library marker davegut.tpLinkCommon, line 29

def commonUpdated() { // library marker davegut.tpLinkCommon, line 31
	def commsErr = device.currentValue("commsError") // library marker davegut.tpLinkCommon, line 32
	Map logData = [commsError: commsErr] // library marker davegut.tpLinkCommon, line 33
	if (commsErr == "true") { // library marker davegut.tpLinkCommon, line 34
		logData << [configure: configure(true)] // library marker davegut.tpLinkCommon, line 35
	} // library marker davegut.tpLinkCommon, line 36
	updateAttr("commsError", "false") // library marker davegut.tpLinkCommon, line 37
	state.errorCount = 0 // library marker davegut.tpLinkCommon, line 38
	state.lastCmd = "" // library marker davegut.tpLinkCommon, line 39
	logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCommon, line 40
	logData << [logging: setLogsOff()] // library marker davegut.tpLinkCommon, line 41
	logData << [updateDevSettings: updDevSettings()] // library marker davegut.tpLinkCommon, line 42
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 43
	return logData // library marker davegut.tpLinkCommon, line 44
} // library marker davegut.tpLinkCommon, line 45

def updDevSettings() { // library marker davegut.tpLinkCommon, line 47
	Map logData = [method: "updDevSettings"] // library marker davegut.tpLinkCommon, line 48
	List requests = [] // library marker davegut.tpLinkCommon, line 49
	if (ledRule != null) { // library marker davegut.tpLinkCommon, line 50
		logData << [ledRule: ledRule] // library marker davegut.tpLinkCommon, line 51
		requests << [method: "get_led_info"] // library marker davegut.tpLinkCommon, line 52
	} // library marker davegut.tpLinkCommon, line 53
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCommon, line 54
	asyncSend(createMultiCmd(requests), "updDevSettings", "parseUpdates") // library marker davegut.tpLinkCommon, line 55
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 56
	return logData // library marker davegut.tpLinkCommon, line 57
} // library marker davegut.tpLinkCommon, line 58

//	===== Capability Configuration ===== // library marker davegut.tpLinkCommon, line 60
def configure(checkApp = true) { // library marker davegut.tpLinkCommon, line 61
	unschedule() // library marker davegut.tpLinkCommon, line 62
	Map logData = [method: "configure"] // library marker davegut.tpLinkCommon, line 63
	if (checkApp == true) { // library marker davegut.tpLinkCommon, line 64
		logData << [updateData: parent.tpLinkCheckForDevices(5)] // library marker davegut.tpLinkCommon, line 65
	} // library marker davegut.tpLinkCommon, line 66
	logData << [handshake: deviceHandshake()] // library marker davegut.tpLinkCommon, line 67
	runEvery3Hours(deviceHandshake) // library marker davegut.tpLinkCommon, line 68
	logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCommon, line 69
	logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCommon, line 70
	logData << [logging: setLogsOff()] // library marker davegut.tpLinkCommon, line 71
	logInfo(logData) // library marker davegut.tpLinkCommon, line 72
	runIn(2, initSettings) // library marker davegut.tpLinkCommon, line 73
	return logData // library marker davegut.tpLinkCommon, line 74
} // library marker davegut.tpLinkCommon, line 75

def initSettings() { // library marker davegut.tpLinkCommon, line 77
	Map logData = [method: "initSettings"] // library marker davegut.tpLinkCommon, line 78
	Map prefs = state.compData // library marker davegut.tpLinkCommon, line 79
	List requests = [] // library marker davegut.tpLinkCommon, line 80
	if (ledRule) { // library marker davegut.tpLinkCommon, line 81
		requests << [method: "get_led_info"] // library marker davegut.tpLinkCommon, line 82
	} // library marker davegut.tpLinkCommon, line 83
	if (getDataValue("type") == "Plug EM") { requests << [method: "get_energy_usage"] } // library marker davegut.tpLinkCommon, line 84
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCommon, line 85
	asyncSend(createMultiCmd(requests), "initSettings", "parseUpdates") // library marker davegut.tpLinkCommon, line 86
	return logData // library marker davegut.tpLinkCommon, line 87
} // library marker davegut.tpLinkCommon, line 88

def setPollInterval(pInterval = pollInterval) { // library marker davegut.tpLinkCommon, line 90
	String devType = getDataValue("type") // library marker davegut.tpLinkCommon, line 91
	def pollMethod = "minRefresh" // library marker davegut.tpLinkCommon, line 92
	if (devType == "Plug EM") { // library marker davegut.tpLinkCommon, line 93
		pollMethod = "plugEmRefresh" // library marker davegut.tpLinkCommon, line 94
	} else if (devType == "Hub"|| devType == "Parent") { // library marker davegut.tpLinkCommon, line 95
		pollMethod = "parentRefresh" // library marker davegut.tpLinkCommon, line 96
	} // library marker davegut.tpLinkCommon, line 97

	if (pInterval.contains("sec")) { // library marker davegut.tpLinkCommon, line 99
		def interval = pInterval.replace(" sec", "").toInteger() // library marker davegut.tpLinkCommon, line 100
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 101
		schedule("${start}/${interval} * * * * ?", pollMethod) // library marker davegut.tpLinkCommon, line 102
	} else { // library marker davegut.tpLinkCommon, line 103
		def interval= pInterval.replace(" min", "").toInteger() // library marker davegut.tpLinkCommon, line 104
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 105
		schedule("${start} */${interval} * * * ?", pollMethod) // library marker davegut.tpLinkCommon, line 106
	} // library marker davegut.tpLinkCommon, line 107
	return pInterval // library marker davegut.tpLinkCommon, line 108
} // library marker davegut.tpLinkCommon, line 109

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkCommon, line 111
def parseUpdates(resp, data= null) { // library marker davegut.tpLinkCommon, line 112
	Map logData = [method: "parseUpdates", data: data] // library marker davegut.tpLinkCommon, line 113
	def respData = parseData(resp).cmdResp // library marker davegut.tpLinkCommon, line 114
	if (respData != null && respData.error_code == 0) { // library marker davegut.tpLinkCommon, line 115
		respData.result.responses.each { // library marker davegut.tpLinkCommon, line 116
			if (it.error_code == 0) { // library marker davegut.tpLinkCommon, line 117
				if (!it.method.contains("set_")) { // library marker davegut.tpLinkCommon, line 118
					distGetData(it, data) // library marker davegut.tpLinkCommon, line 119
				} else { // library marker davegut.tpLinkCommon, line 120
					logData << [devMethod: it.method] // library marker davegut.tpLinkCommon, line 121
					logDebug(logData) // library marker davegut.tpLinkCommon, line 122
				} // library marker davegut.tpLinkCommon, line 123
			} else { // library marker davegut.tpLinkCommon, line 124
				logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCommon, line 125
				logWarn(logData) // library marker davegut.tpLinkCommon, line 126
			} // library marker davegut.tpLinkCommon, line 127
		} // library marker davegut.tpLinkCommon, line 128
	} else { // library marker davegut.tpLinkCommon, line 129
		logData << [status: "invalidRequest", data: respData] // library marker davegut.tpLinkCommon, line 130
		logWarn(logData)				 // library marker davegut.tpLinkCommon, line 131
	} // library marker davegut.tpLinkCommon, line 132
} // library marker davegut.tpLinkCommon, line 133

def distGetData(devResp, data) { // library marker davegut.tpLinkCommon, line 135
	switch(devResp.method) { // library marker davegut.tpLinkCommon, line 136
		case "get_device_info": // library marker davegut.tpLinkCommon, line 137
			parse_get_device_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 138
			break // library marker davegut.tpLinkCommon, line 139
		case "get_energy_usage": // library marker davegut.tpLinkCommon, line 140
			parse_get_energy_usage(devResp.result, data) // library marker davegut.tpLinkCommon, line 141
			break // library marker davegut.tpLinkCommon, line 142
		case "get_child_device_list": // library marker davegut.tpLinkCommon, line 143
			parse_get_child_device_list(devResp.result, data) // library marker davegut.tpLinkCommon, line 144
			break // library marker davegut.tpLinkCommon, line 145
		case "get_alarm_configure": // library marker davegut.tpLinkCommon, line 146
			parse_get_alarm_configure(devResp.result, data) // library marker davegut.tpLinkCommon, line 147
			break // library marker davegut.tpLinkCommon, line 148
		case "get_led_info": // library marker davegut.tpLinkCommon, line 149
			parse_get_led_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 150
			break // library marker davegut.tpLinkCommon, line 151
		default: // library marker davegut.tpLinkCommon, line 152
			Map logData = [method: "distGetData", data: data, // library marker davegut.tpLinkCommon, line 153
						   devMethod: devResp.method, status: "unprocessed"] // library marker davegut.tpLinkCommon, line 154
			logDebug(logData) // library marker davegut.tpLinkCommon, line 155
	} // library marker davegut.tpLinkCommon, line 156
} // library marker davegut.tpLinkCommon, line 157

def parse_get_led_info(result, data) { // library marker davegut.tpLinkCommon, line 159
	Map logData = [method: "parse_get_led_info", data: data] // library marker davegut.tpLinkCommon, line 160
	if (ledRule != result.led_rule) { // library marker davegut.tpLinkCommon, line 161
		Map request = [ // library marker davegut.tpLinkCommon, line 162
			method: "set_led_info", // library marker davegut.tpLinkCommon, line 163
			params: [ // library marker davegut.tpLinkCommon, line 164
				led_rule: ledRule, // library marker davegut.tpLinkCommon, line 165
				//	Uses mode data from device.  This driver does not update these. // library marker davegut.tpLinkCommon, line 166
				night_mode: [ // library marker davegut.tpLinkCommon, line 167
					night_mode_type: result.night_mode.night_mode_type, // library marker davegut.tpLinkCommon, line 168
					sunrise_offset: result.night_mode.sunrise_offset,  // library marker davegut.tpLinkCommon, line 169
					sunset_offset:result.night_mode.sunset_offset, // library marker davegut.tpLinkCommon, line 170
					start_time: result.night_mode.start_time, // library marker davegut.tpLinkCommon, line 171
					end_time: result.night_mode.end_time // library marker davegut.tpLinkCommon, line 172
				]]] // library marker davegut.tpLinkCommon, line 173
		asyncSend(request, "delayedUpdates", "parseUpdates") // library marker davegut.tpLinkCommon, line 174
		device.updateSetting("ledRule", [type:"enum", value: ledRule]) // library marker davegut.tpLinkCommon, line 175
		logData << [status: "updatingLedRule"] // library marker davegut.tpLinkCommon, line 176
	} // library marker davegut.tpLinkCommon, line 177
	logData << [ledRule: ledRule] // library marker davegut.tpLinkCommon, line 178
	logDebug(logData) // library marker davegut.tpLinkCommon, line 179
} // library marker davegut.tpLinkCommon, line 180

//	===== Capability Refresh ===== // library marker davegut.tpLinkCommon, line 182
def refresh() { // library marker davegut.tpLinkCommon, line 183
	def type = getDataValue("type") // library marker davegut.tpLinkCommon, line 184
	if (type == "Plug EM") { // library marker davegut.tpLinkCommon, line 185
		plugEmRefresh() // library marker davegut.tpLinkCommon, line 186
	} else if (type == "Hub" || type == "Parent") { // library marker davegut.tpLinkCommon, line 187
		parentRefresh() // library marker davegut.tpLinkCommon, line 188
	} else { // library marker davegut.tpLinkCommon, line 189
		minRefresh() // library marker davegut.tpLinkCommon, line 190
	} // library marker davegut.tpLinkCommon, line 191
} // library marker davegut.tpLinkCommon, line 192

def plugEmRefresh() { // library marker davegut.tpLinkCommon, line 194
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 195
	requests << [method:"get_energy_usage"] // library marker davegut.tpLinkCommon, line 196
	asyncSend(createMultiCmd(requests), "plugEmRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 197
} // library marker davegut.tpLinkCommon, line 198

def parentRefresh() { // library marker davegut.tpLinkCommon, line 200
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 201
	requests << [method:"get_child_device_list"] // library marker davegut.tpLinkCommon, line 202
	asyncSend(createMultiCmd(requests), "parentRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 203
} // library marker davegut.tpLinkCommon, line 204

def minRefresh() { // library marker davegut.tpLinkCommon, line 206
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 207
	asyncSend(createMultiCmd(requests), "minRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 208
} // library marker davegut.tpLinkCommon, line 209

def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkCommon, line 211
	asyncSend(createMultiCmd(requests), data, action) // library marker davegut.tpLinkCommon, line 212
} // library marker davegut.tpLinkCommon, line 213

def sendSingleCmd(request, data, action) { // library marker davegut.tpLinkCommon, line 215
	asyncSend(request, data, action) // library marker davegut.tpLinkCommon, line 216
} // library marker davegut.tpLinkCommon, line 217

def createMultiCmd(requests) { // library marker davegut.tpLinkCommon, line 219
	Map cmdBody = [ // library marker davegut.tpLinkCommon, line 220
		method: "multipleRequest", // library marker davegut.tpLinkCommon, line 221
		params: [requests: requests]] // library marker davegut.tpLinkCommon, line 222
	return cmdBody // library marker davegut.tpLinkCommon, line 223
} // library marker davegut.tpLinkCommon, line 224

def nullParse(resp, data) { } // library marker davegut.tpLinkCommon, line 226

def updateAttr(attr, value) { // library marker davegut.tpLinkCommon, line 228
	if (device.currentValue(attr) != value) { // library marker davegut.tpLinkCommon, line 229
		sendEvent(name: attr, value: value) // library marker davegut.tpLinkCommon, line 230
	} // library marker davegut.tpLinkCommon, line 231
} // library marker davegut.tpLinkCommon, line 232

//	===== Device Handshake ===== // library marker davegut.tpLinkCommon, line 234
def deviceHandshake() { // library marker davegut.tpLinkCommon, line 235
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkCommon, line 236
	Map logData = [method: "deviceHandshake", protocol: protocol] // library marker davegut.tpLinkCommon, line 237
	if (protocol == "KLAP") { // library marker davegut.tpLinkCommon, line 238
		klapHandshake() // library marker davegut.tpLinkCommon, line 239
	} else if (protocol == "AES") { // library marker davegut.tpLinkCommon, line 240
		aesHandshake() // library marker davegut.tpLinkCommon, line 241
	} else { // library marker davegut.tpLinkCommon, line 242
		logData << [ERROR: "Protocol not supported"] // library marker davegut.tpLinkCommon, line 243
	} // library marker davegut.tpLinkCommon, line 244
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 245
	return logData // library marker davegut.tpLinkCommon, line 246
} // library marker davegut.tpLinkCommon, line 247

// ~~~~~ end include (35) davegut.tpLinkCommon ~~~~~

// ~~~~~ start include (36) davegut.tpLinkComms ~~~~~
library ( // library marker davegut.tpLinkComms, line 1
	name: "tpLinkComms", // library marker davegut.tpLinkComms, line 2
	namespace: "davegut", // library marker davegut.tpLinkComms, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkComms, line 4
	description: "Communication methods for TP-Link Integration", // library marker davegut.tpLinkComms, line 5
	category: "utilities", // library marker davegut.tpLinkComms, line 6
	documentationLink: "" // library marker davegut.tpLinkComms, line 7
) // library marker davegut.tpLinkComms, line 8
import org.json.JSONObject // library marker davegut.tpLinkComms, line 9
import groovy.json.JsonOutput // library marker davegut.tpLinkComms, line 10
import groovy.json.JsonBuilder // library marker davegut.tpLinkComms, line 11
import groovy.json.JsonSlurper // library marker davegut.tpLinkComms, line 12

//	===== Async Commsunications Methods ===== // library marker davegut.tpLinkComms, line 14
def asyncSend(cmdBody, reqData, action) { // library marker davegut.tpLinkComms, line 15
	Map cmdData = [cmdBody: cmdBody, reqData: reqData, action: action] // library marker davegut.tpLinkComms, line 16
	state.lastCmd = cmdData // library marker davegut.tpLinkComms, line 17
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkComms, line 18
	Map reqParams = [:] // library marker davegut.tpLinkComms, line 19
	if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 20
		reqParams = getKlapParams(cmdBody) // library marker davegut.tpLinkComms, line 21
	} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 22
		reqParams = getAesParams(cmdBody) // library marker davegut.tpLinkComms, line 23
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 24
		reqParams = getVacAesParams(cmdBody) // library marker davegut.tpLinkComms, line 25
	} // library marker davegut.tpLinkComms, line 26
	asyncPost(reqParams, action, reqData) // library marker davegut.tpLinkComms, line 27
} // library marker davegut.tpLinkComms, line 28

def asyncPost(reqParams, parseMethod, reqData=null) { // library marker davegut.tpLinkComms, line 30
	Map logData = [method: "asyncPost", parseMethod: parseMethod, data:reqData] // library marker davegut.tpLinkComms, line 31
	try { // library marker davegut.tpLinkComms, line 32
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.tpLinkComms, line 33
		logData << [status: "OK"] // library marker davegut.tpLinkComms, line 34
	} catch (err) { // library marker davegut.tpLinkComms, line 35
		logData << [status: "FAILED", reqParams: reqParams, error: err] // library marker davegut.tpLinkComms, line 36
		runIn(1, handleCommsError, [data: logData]) // library marker davegut.tpLinkComms, line 37
	} // library marker davegut.tpLinkComms, line 38
	logDebug(logData) // library marker davegut.tpLinkComms, line 39
} // library marker davegut.tpLinkComms, line 40

def parseData(resp, protocol = getDataValue("protocol")) { // library marker davegut.tpLinkComms, line 42
	Map logData = [method: "parseData"] // library marker davegut.tpLinkComms, line 43
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 44
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 45
			logData << parseKlapData(resp) // library marker davegut.tpLinkComms, line 46
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 47
			logData << parseAesData(resp) // library marker davegut.tpLinkComms, line 48
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 49
			logData << parseVacAesData(resp) // library marker davegut.tpLinkComms, line 50
		} // library marker davegut.tpLinkComms, line 51
	} else { // library marker davegut.tpLinkComms, line 52
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.tpLinkComms, line 53
		logWarn(logData) // library marker davegut.tpLinkComms, line 54
		handleCommsError("httpFailure") // library marker davegut.tpLinkComms, line 55
	} // library marker davegut.tpLinkComms, line 56
	return logData // library marker davegut.tpLinkComms, line 57
} // library marker davegut.tpLinkComms, line 58

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 60
def handleCommsError(retryReason) { // library marker davegut.tpLinkComms, line 61
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.tpLinkComms, line 62
	if (state.lastCmd != "") { // library marker davegut.tpLinkComms, line 63
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 64
		state.errorCount = count // library marker davegut.tpLinkComms, line 65
		logData << [count: count, lastCmd: state.lastCmd] // library marker davegut.tpLinkComms, line 66
		switch (count) { // library marker davegut.tpLinkComms, line 67
			case 1: // library marker davegut.tpLinkComms, line 68
				logData << [action: "resendCommand"] // library marker davegut.tpLinkComms, line 69
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 70
				break // library marker davegut.tpLinkComms, line 71
			case 2: // library marker davegut.tpLinkComms, line 72
				logData << [attemptHandshake: deviceHandshake(), // library marker davegut.tpLinkComms, line 73
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 74
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 75
				break // library marker davegut.tpLinkComms, line 76
			case 3: // library marker davegut.tpLinkComms, line 77
				logData << [configure: configure(), // library marker davegut.tpLinkComms, line 78
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 79
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 80
			case 4: // library marker davegut.tpLinkComms, line 81
				logData << [setError: setCommsError(true), retries: "disabled"] // library marker davegut.tpLinkComms, line 82
				break // library marker davegut.tpLinkComms, line 83
			default: // library marker davegut.tpLinkComms, line 84
				logData << [retries: "disabled"] // library marker davegut.tpLinkComms, line 85
				break // library marker davegut.tpLinkComms, line 86
		} // library marker davegut.tpLinkComms, line 87
	} else { // library marker davegut.tpLinkComms, line 88
		logData << [status: "noCommandToRetry"] // library marker davegut.tpLinkComms, line 89
	} // library marker davegut.tpLinkComms, line 90
	logDebug(logData) // library marker davegut.tpLinkComms, line 91
} // library marker davegut.tpLinkComms, line 92

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 94
	def cmdData = new JSONObject(state.lastCmd) // library marker davegut.tpLinkComms, line 95
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 96
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 97
} // library marker davegut.tpLinkComms, line 98

def setCommsError(status) { // library marker davegut.tpLinkComms, line 100
	if (device.currentValue("commsError") == true && status == false) { // library marker davegut.tpLinkComms, line 101
		updateAttr("commsError", false) // library marker davegut.tpLinkComms, line 102
		setPollInterval() // library marker davegut.tpLinkComms, line 103
		logInfo([method: "setCommsError", result: "Comms Error set to false"]) // library marker davegut.tpLinkComms, line 104
	} else if (device.currentValue("commsError") == false && status == true) { // library marker davegut.tpLinkComms, line 105
		updateAttr("commsError", true) // library marker davegut.tpLinkComms, line 106
		setPollInterval("30 min") // library marker davegut.tpLinkComms, line 107
		logWarn([method: "setCommsError", result: "Comms Error Set to true"]) // library marker davegut.tpLinkComms, line 108
	} // library marker davegut.tpLinkComms, line 109
} // library marker davegut.tpLinkComms, line 110

// ~~~~~ end include (36) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (37) davegut.tpLinkCrypto ~~~~~
library ( // library marker davegut.tpLinkCrypto, line 1
	name: "tpLinkCrypto", // library marker davegut.tpLinkCrypto, line 2
	namespace: "davegut", // library marker davegut.tpLinkCrypto, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkCrypto, line 4
	description: "Handshake methods for TP-Link Integration", // library marker davegut.tpLinkCrypto, line 5
	category: "utilities", // library marker davegut.tpLinkCrypto, line 6
	documentationLink: "" // library marker davegut.tpLinkCrypto, line 7
) // library marker davegut.tpLinkCrypto, line 8
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tpLinkCrypto, line 9
import javax.crypto.Cipher // library marker davegut.tpLinkCrypto, line 10
import java.security.KeyFactory // library marker davegut.tpLinkCrypto, line 11
import java.util.Random // library marker davegut.tpLinkCrypto, line 12
import javax.crypto.spec.SecretKeySpec // library marker davegut.tpLinkCrypto, line 13
import javax.crypto.spec.IvParameterSpec // library marker davegut.tpLinkCrypto, line 14
import java.security.MessageDigest // library marker davegut.tpLinkCrypto, line 15

//	===== AES Handshake and Login ===== // library marker davegut.tpLinkCrypto, line 17
def aesHandshake(baseUrl = getDataValue("baseUrl"), devData = null) { // library marker davegut.tpLinkCrypto, line 18
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.tpLinkCrypto, line 19
	Map rsaKey = getRsaKey() // library marker davegut.tpLinkCrypto, line 20
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKey.public}-----END PUBLIC KEY-----\n" // library marker davegut.tpLinkCrypto, line 21
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.tpLinkCrypto, line 22
	Map reqParams = [uri: baseUrl, // library marker davegut.tpLinkCrypto, line 23
					 body: new groovy.json.JsonBuilder(cmdBody).toString(), // library marker davegut.tpLinkCrypto, line 24
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 25
					 timeout: 10] // library marker davegut.tpLinkCrypto, line 26
	asyncPost(reqParams, "parseAesHandshake", reqData) // library marker davegut.tpLinkCrypto, line 27
} // library marker davegut.tpLinkCrypto, line 28

def parseAesHandshake(resp, data){ // library marker davegut.tpLinkCrypto, line 30
	Map logData = [method: "parseAesHandshake"] // library marker davegut.tpLinkCrypto, line 31
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 32
		try { // library marker davegut.tpLinkCrypto, line 33
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 34
			Map cmdResp =  new JsonSlurper().parseText(resp.data) // library marker davegut.tpLinkCrypto, line 35
			//	cookie // library marker davegut.tpLinkCrypto, line 36
			def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 37
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 38
			//	keys // library marker davegut.tpLinkCrypto, line 39
			byte[] privateKeyBytes = getRsaKey().private.decodeBase64() // library marker davegut.tpLinkCrypto, line 40
			byte[] deviceKeyBytes = cmdResp.result.key.getBytes("UTF-8").decodeBase64() // library marker davegut.tpLinkCrypto, line 41
    		Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.tpLinkCrypto, line 42
			instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.tpLinkCrypto, line 43
			byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.tpLinkCrypto, line 44
			byte[] encKey = cryptoArray[0..15] // library marker davegut.tpLinkCrypto, line 45
			byte[] encIv = cryptoArray[16..31] // library marker davegut.tpLinkCrypto, line 46
			logData << [respStatus: "Cookies/Keys Updated", cookie: cookie, // library marker davegut.tpLinkCrypto, line 47
						encKey: encKey, encIv: encIv] // library marker davegut.tpLinkCrypto, line 48
			String password = encPassword // library marker davegut.tpLinkCrypto, line 49
			String username = encUsername // library marker davegut.tpLinkCrypto, line 50
			if (device) { // library marker davegut.tpLinkCrypto, line 51
				password = parent.encPassword // library marker davegut.tpLinkCrypto, line 52
				username = parent.encUsername // library marker davegut.tpLinkCrypto, line 53
				device.updateSetting("cookie",[type:"password", value: cookie]) // library marker davegut.tpLinkCrypto, line 54
				device.updateSetting("encKey",[type:"password", value: encKey]) // library marker davegut.tpLinkCrypto, line 55
				device.updateSetting("encIv",[type:"password", value: encIv]) // library marker davegut.tpLinkCrypto, line 56
			} else { // library marker davegut.tpLinkCrypto, line 57
				reqData << [cookie: cookie, encIv: encIv, encKey: encKey] // library marker davegut.tpLinkCrypto, line 58
			} // library marker davegut.tpLinkCrypto, line 59
			Map cmdBody = [method: "login_device", // library marker davegut.tpLinkCrypto, line 60
						   params: [password: password, // library marker davegut.tpLinkCrypto, line 61
									username: username], // library marker davegut.tpLinkCrypto, line 62
						   requestTimeMils: 0] // library marker davegut.tpLinkCrypto, line 63
			def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 64
			Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 65
						   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 66
			Map reqParams = [uri: reqData.baseUrl, // library marker davegut.tpLinkCrypto, line 67
							  body: reqBody, // library marker davegut.tpLinkCrypto, line 68
							  timeout:10,  // library marker davegut.tpLinkCrypto, line 69
							  headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 70
							  contentType: "application/json", // library marker davegut.tpLinkCrypto, line 71
							  requestContentType: "application/json"] // library marker davegut.tpLinkCrypto, line 72
			asyncPost(reqParams, "parseAesLogin", reqData) // library marker davegut.tpLinkCrypto, line 73
			logDebug(logData) // library marker davegut.tpLinkCrypto, line 74
		} catch (err) { // library marker davegut.tpLinkCrypto, line 75
			logData << [respStatus: "ERROR parsing HTTP resp.data", // library marker davegut.tpLinkCrypto, line 76
						respData: resp.data, error: err] // library marker davegut.tpLinkCrypto, line 77
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 78
		} // library marker davegut.tpLinkCrypto, line 79
	} else { // library marker davegut.tpLinkCrypto, line 80
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 81
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 82
	} // library marker davegut.tpLinkCrypto, line 83
} // library marker davegut.tpLinkCrypto, line 84

def parseAesLogin(resp, data) { // library marker davegut.tpLinkCrypto, line 86
	if (device) { // library marker davegut.tpLinkCrypto, line 87
		Map logData = [method: "parseAesLogin"] // library marker davegut.tpLinkCrypto, line 88
		if (resp.status == 200) { // library marker davegut.tpLinkCrypto, line 89
			if (resp.json.error_code == 0) { // library marker davegut.tpLinkCrypto, line 90
				try { // library marker davegut.tpLinkCrypto, line 91
					byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 92
					byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 93
					def clearResp = aesDecrypt(resp.json.result.response, encKey, encIv) // library marker davegut.tpLinkCrypto, line 94
					Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.tpLinkCrypto, line 95
					if (cmdResp.error_code == 0) { // library marker davegut.tpLinkCrypto, line 96
						def token = cmdResp.result.token // library marker davegut.tpLinkCrypto, line 97
						logData << [respStatus: "OK", token: token] // library marker davegut.tpLinkCrypto, line 98
						device.updateSetting("token",[type:"password", value: token]) // library marker davegut.tpLinkCrypto, line 99
						logDebug(logData) // library marker davegut.tpLinkCrypto, line 100
					} else { // library marker davegut.tpLinkCrypto, line 101
						logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.tpLinkCrypto, line 102
									error_code: cmdResp.error_code, // library marker davegut.tpLinkCrypto, line 103
									check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.tpLinkCrypto, line 104
						logWarn(logData) // library marker davegut.tpLinkCrypto, line 105
					} // library marker davegut.tpLinkCrypto, line 106
				} catch (err) { // library marker davegut.tpLinkCrypto, line 107
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.tpLinkCrypto, line 108
								error: err] // library marker davegut.tpLinkCrypto, line 109
					logWarn(logData) // library marker davegut.tpLinkCrypto, line 110
				} // library marker davegut.tpLinkCrypto, line 111
			} else { // library marker davegut.tpLinkCrypto, line 112
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.tpLinkCrypto, line 113
							respJson: resp.json] // library marker davegut.tpLinkCrypto, line 114
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 115
			} // library marker davegut.tpLinkCrypto, line 116
		} else { // library marker davegut.tpLinkCrypto, line 117
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.tpLinkCrypto, line 118
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 119
		} // library marker davegut.tpLinkCrypto, line 120
	} else { // library marker davegut.tpLinkCrypto, line 121
		getAesToken(resp, data.data) // library marker davegut.tpLinkCrypto, line 122
	} // library marker davegut.tpLinkCrypto, line 123
} // library marker davegut.tpLinkCrypto, line 124

//	===== KLAP Handshake ===== // library marker davegut.tpLinkCrypto, line 126
def klapHandshake(baseUrl = getDataValue("baseUrl"), localHash = parent.localHash, devData = null) { // library marker davegut.tpLinkCrypto, line 127
	byte[] localSeed = new byte[16] // library marker davegut.tpLinkCrypto, line 128
	new Random().nextBytes(localSeed) // library marker davegut.tpLinkCrypto, line 129
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkCrypto, line 130
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkCrypto, line 131
					 body: localSeed, // library marker davegut.tpLinkCrypto, line 132
					 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 133
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 134
					 timeout:10] // library marker davegut.tpLinkCrypto, line 135
	asyncPost(reqParams, "parseKlapHandshake", reqData) // library marker davegut.tpLinkCrypto, line 136
} // library marker davegut.tpLinkCrypto, line 137

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkCrypto, line 139
	Map logData = [method: "parseKlapHandshake", data: data] // library marker davegut.tpLinkCrypto, line 140
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 141
		try { // library marker davegut.tpLinkCrypto, line 142
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 143
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkCrypto, line 144
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkCrypto, line 145
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkCrypto, line 146
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkCrypto, line 147
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkCrypto, line 148
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 149
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkCrypto, line 150
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkCrypto, line 151
				//	cookie // library marker davegut.tpLinkCrypto, line 152
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 153
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 154
				logData << [cookie: cookie] // library marker davegut.tpLinkCrypto, line 155
				//	seqNo and encIv // library marker davegut.tpLinkCrypto, line 156
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 157
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 158
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkCrypto, line 159

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkCrypto, line 161
				atomicState.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 162

//				if (device) {  // library marker davegut.tpLinkCrypto, line 164
//				} // library marker davegut.tpLinkCrypto, line 165

				logData << [seqNo: seqNo, encIv: fullIv[0..11]] // library marker davegut.tpLinkCrypto, line 167
				//	encKey // library marker davegut.tpLinkCrypto, line 168
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 169
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkCrypto, line 170
				logData << [encKey: encKey] // library marker davegut.tpLinkCrypto, line 171
				//	encSig // library marker davegut.tpLinkCrypto, line 172
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 173
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkCrypto, line 174
				if (device) { // library marker davegut.tpLinkCrypto, line 175
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkCrypto, line 176
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkCrypto, line 177
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkCrypto, line 178
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkCrypto, line 179
				} else { // library marker davegut.tpLinkCrypto, line 180
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkCrypto, line 181
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkCrypto, line 182
				} // library marker davegut.tpLinkCrypto, line 183
				logData << [encSig: encSig] // library marker davegut.tpLinkCrypto, line 184
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 185
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkCrypto, line 186
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkCrypto, line 187
								 body: body, // library marker davegut.tpLinkCrypto, line 188
								 timeout:10, // library marker davegut.tpLinkCrypto, line 189
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 190
								 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 191
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 192
				asyncPost(reqParams, "parseKlapHandshake2", reqData) // library marker davegut.tpLinkCrypto, line 193
			} else { // library marker davegut.tpLinkCrypto, line 194
				logData << [respStatus: "ERROR: locakAuthHash != serverHash", // library marker davegut.tpLinkCrypto, line 195
							localAuthHash: localAuthHash, serverHash: serverHash] // library marker davegut.tpLinkCrypto, line 196
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 197
			} // library marker davegut.tpLinkCrypto, line 198
		} catch (err) { // library marker davegut.tpLinkCrypto, line 199
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkCrypto, line 200
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 201
		} // library marker davegut.tpLinkCrypto, line 202
	} else { // library marker davegut.tpLinkCrypto, line 203
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 204
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 205
	} // library marker davegut.tpLinkCrypto, line 206
} // library marker davegut.tpLinkCrypto, line 207

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkCrypto, line 209
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkCrypto, line 210
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkCrypto, line 211
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkCrypto, line 212
		logDebug(logData) // library marker davegut.tpLinkCrypto, line 213
	} else { // library marker davegut.tpLinkCrypto, line 214
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkCrypto, line 215
					resp: resp.properties] // library marker davegut.tpLinkCrypto, line 216
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 217
	} // library marker davegut.tpLinkCrypto, line 218
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkCrypto, line 219
} // library marker davegut.tpLinkCrypto, line 220

//	===== Comms Support ===== // library marker davegut.tpLinkCrypto, line 222
def getKlapParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 223
	Map reqParams = [timeout: 10, headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 224
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkCrypto, line 225
	state.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 226
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 227
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 228
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkCrypto, line 229
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 230

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkCrypto, line 232
									encSig, seqNo) // library marker davegut.tpLinkCrypto, line 233
	reqParams << [uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkCrypto, line 234
				  body: encryptedData.cipherData, // library marker davegut.tpLinkCrypto, line 235
				  contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 236
				  requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 237
	return reqParams // library marker davegut.tpLinkCrypto, line 238
} // library marker davegut.tpLinkCrypto, line 239

def getAesParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 241
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 242
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 243
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 244
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 245
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 246
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkCrypto, line 247
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkCrypto, line 248
					 contentType: "application/json", // library marker davegut.tpLinkCrypto, line 249
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 250
					 timeout: 10, // library marker davegut.tpLinkCrypto, line 251
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 252
	return reqParams // library marker davegut.tpLinkCrypto, line 253
} // library marker davegut.tpLinkCrypto, line 254

def parseKlapData(resp) { // library marker davegut.tpLinkCrypto, line 256
	Map parseData = [parseMethod: "parseKlapData"] // library marker davegut.tpLinkCrypto, line 257
	try { // library marker davegut.tpLinkCrypto, line 258
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 259
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 260
		int seqNo = state.seqNo // library marker davegut.tpLinkCrypto, line 261
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkCrypto, line 262
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkCrypto, line 263
														   encIv, seqNo)) // library marker davegut.tpLinkCrypto, line 264
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 265
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 266
		setCommsError(false) // library marker davegut.tpLinkCrypto, line 267
		logDebug(parseData) // library marker davegut.tpLinkCrypto, line 268
	} catch (err) { // library marker davegut.tpLinkCrypto, line 269
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 270
		logWarn(parseData) // library marker davegut.tpLinkCrypto, line 271
		handleCommsError("deviceDataParseError") // library marker davegut.tpLinkCrypto, line 272
	} // library marker davegut.tpLinkCrypto, line 273
	return parseData // library marker davegut.tpLinkCrypto, line 274
} // library marker davegut.tpLinkCrypto, line 275

def parseAesData(resp) { // library marker davegut.tpLinkCrypto, line 277
	Map parseData = [parseMethod: "parseAesData"] // library marker davegut.tpLinkCrypto, line 278
	try { // library marker davegut.tpLinkCrypto, line 279
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 280
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 281
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCrypto, line 282
														 encKey, encIv)) // library marker davegut.tpLinkCrypto, line 283
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 284
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 285
		setCommsError(false) // library marker davegut.tpLinkCrypto, line 286
		logDebug(parseData) // library marker davegut.tpLinkCrypto, line 287
	} catch (err) { // library marker davegut.tpLinkCrypto, line 288
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 289
		logWarn(parseData) // library marker davegut.tpLinkCrypto, line 290
		handleCommsError("deviceDataParseError") // library marker davegut.tpLinkCrypto, line 291
	} // library marker davegut.tpLinkCrypto, line 292
	return parseData // library marker davegut.tpLinkCrypto, line 293
} // library marker davegut.tpLinkCrypto, line 294

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 296
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 297
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 298
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 299
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 300
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 301
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 302
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 303
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 304

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 306
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 307
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 308
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 309
} // library marker davegut.tpLinkCrypto, line 310

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 312
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 313
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 314
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 315
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 316
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 317
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 318
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 319
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 320
} // library marker davegut.tpLinkCrypto, line 321

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 323
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 324
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 325
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 326
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 327
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 328
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 329
} // library marker davegut.tpLinkCrypto, line 330

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 332
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 333
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 334
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 335
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 336
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 337
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 338
} // library marker davegut.tpLinkCrypto, line 339

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 341
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 342
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 343
	md.update(data) // library marker davegut.tpLinkCrypto, line 344
	return md.digest() // library marker davegut.tpLinkCrypto, line 345
} // library marker davegut.tpLinkCrypto, line 346

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 348
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 349
	return new String(arr) // library marker davegut.tpLinkCrypto, line 350
} // library marker davegut.tpLinkCrypto, line 351

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 353
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 354
	try { // library marker davegut.tpLinkCrypto, line 355
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 356
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 357
	} catch (error) { // library marker davegut.tpLinkCrypto, line 358
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 359
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 360
	} // library marker davegut.tpLinkCrypto, line 361
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 362
} // library marker davegut.tpLinkCrypto, line 363

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 365
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 366
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 367
	return byteValue // library marker davegut.tpLinkCrypto, line 368
} // library marker davegut.tpLinkCrypto, line 369

def getRsaKey() { // library marker davegut.tpLinkCrypto, line 371
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkCrypto, line 372
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkCrypto, line 373
} // library marker davegut.tpLinkCrypto, line 374

// ~~~~~ end include (37) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (30) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def nameSpace() { return "davegut" } // library marker davegut.Logging, line 10

def version() { return "2.3.9a" } // library marker davegut.Logging, line 12

def label() { // library marker davegut.Logging, line 14
	if (device) {  // library marker davegut.Logging, line 15
		return device.displayName + "-${version()}" // library marker davegut.Logging, line 16
	} else {  // library marker davegut.Logging, line 17
		return app.getLabel() + "-${version()}" // library marker davegut.Logging, line 18
	} // library marker davegut.Logging, line 19
} // library marker davegut.Logging, line 20

def listAttributes() { // library marker davegut.Logging, line 22
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 23
	Map attrs = [:] // library marker davegut.Logging, line 24
	attrData.each { // library marker davegut.Logging, line 25
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 26
	} // library marker davegut.Logging, line 27
	return attrs // library marker davegut.Logging, line 28
} // library marker davegut.Logging, line 29

def setLogsOff() { // library marker davegut.Logging, line 31
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 32
	if (logEnable) { // library marker davegut.Logging, line 33
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 34
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 35
	} // library marker davegut.Logging, line 36
	return logData // library marker davegut.Logging, line 37
} // library marker davegut.Logging, line 38

def logTrace(msg){ log.trace "${label()}: ${msg}" } // library marker davegut.Logging, line 40

def logInfo(msg) {  // library marker davegut.Logging, line 42
	if (infoLog) { log.info "${label()}: ${msg}" } // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def debugLogOff() { // library marker davegut.Logging, line 46
	if (device) { // library marker davegut.Logging, line 47
		device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 48
	} else { // library marker davegut.Logging, line 49
		app.updateSetting("logEnable", false) // library marker davegut.Logging, line 50
	} // library marker davegut.Logging, line 51
	logInfo("debugLogOff") // library marker davegut.Logging, line 52
} // library marker davegut.Logging, line 53

def logDebug(msg) { // library marker davegut.Logging, line 55
	if (logEnable) { log.debug "${label()}: ${msg}" } // library marker davegut.Logging, line 56
} // library marker davegut.Logging, line 57

def logWarn(msg) { log.warn "${label()}: ${msg}" } // library marker davegut.Logging, line 59

def logError(msg) { log.error "${label()}: ${msg}" } // library marker davegut.Logging, line 61

// ~~~~~ end include (30) davegut.Logging ~~~~~
