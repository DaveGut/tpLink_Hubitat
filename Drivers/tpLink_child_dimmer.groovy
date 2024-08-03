/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Verified on P306(US) night light.
=================================================================================================*/

metadata {
	definition (name: "TpLink Child Dimmer", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_child_dimmer.groovy")
	{ }
	preferences {
		commonPreferences()
	}
}

def installed() {
	Map logData = [method: "installed"]
	logData << [commonInst: commonInstalled()]
	logInfo(logData)
	runIn(1, updated)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}

def parse_get_device_info(devData, data = null) {
	Map logData = [method: "parse_get_device_info"]
	try {
		def onOff = "off"
		if (devData.device_on == true) { onOff = "on" }
		if (device.currentValue("switch") != onOff) {
			sendEvent(name: "switch", value: onOff, type: state.eventType)
			state.eventType = "physical"
		}
		updateAttr("level", devData.brightness)
		logData << [onOff: onOff, level: devData.brightness, status: "OK"]
		logDebug(logData)
	} catch (err) {
		logData << [status: "FAILED", error: err]
		logWarn(logData)
	}
}


Level



// ~~~~~ start include (51) davegut.tpLinkCapSwitch ~~~~~
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

// ~~~~~ end include (51) davegut.tpLinkCapSwitch ~~~~~

// ~~~~~ start include (61) davegut.tpLinkCapSwitchLevel ~~~~~
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

// ~~~~~ end include (61) davegut.tpLinkCapSwitchLevel ~~~~~

// ~~~~~ start include (62) davegut.tpLinkChildCommon ~~~~~
library ( // library marker davegut.tpLinkChildCommon, line 1
	name: "tpLinkChildCommon", // library marker davegut.tpLinkChildCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkChildCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkChildCommon, line 4
	description: "Common Child driver methods including capability Refresh and Configuration methods", // library marker davegut.tpLinkChildCommon, line 5
	category: "utilities", // library marker davegut.tpLinkChildCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkChildCommon, line 7
) // library marker davegut.tpLinkChildCommon, line 8

capability "Refresh" // library marker davegut.tpLinkChildCommon, line 10

def commonPreferences() { // library marker davegut.tpLinkChildCommon, line 12
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkChildCommon, line 13
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkChildCommon, line 14
} // library marker davegut.tpLinkChildCommon, line 15

def commonInstalled() { // library marker davegut.tpLinkChildCommon, line 17
	state.eventType = "digital" // library marker davegut.tpLinkChildCommon, line 18
	return logData // library marker davegut.tpLinkChildCommon, line 19
} // library marker davegut.tpLinkChildCommon, line 20

def commonUpdated() { // library marker davegut.tpLinkChildCommon, line 22
	Map logData = [logging: setLogsOff()] // library marker davegut.tpLinkChildCommon, line 23
	refresh() // library marker davegut.tpLinkChildCommon, line 24
	return logData // library marker davegut.tpLinkChildCommon, line 25
} // library marker davegut.tpLinkChildCommon, line 26

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkChildCommon, line 28
def distChildData(respData, data) { // library marker davegut.tpLinkChildCommon, line 29
	respData.each { // library marker davegut.tpLinkChildCommon, line 30
		if (it.error_code == 0) { // library marker davegut.tpLinkChildCommon, line 31
			if (!it.method.contains("set_")) { // library marker davegut.tpLinkChildCommon, line 32
				distChildGetData(it, data) // library marker davegut.tpLinkChildCommon, line 33
			} else { // library marker davegut.tpLinkChildCommon, line 34
				logDebug([devMethod: it.method]) // library marker davegut.tpLinkChildCommon, line 35
			} // library marker davegut.tpLinkChildCommon, line 36
		} else { // library marker davegut.tpLinkChildCommon, line 37
			logWarn(["${it.method}": [status: "cmdFailed", data: it]]) // library marker davegut.tpLinkChildCommon, line 38
		} // library marker davegut.tpLinkChildCommon, line 39
	} // library marker davegut.tpLinkChildCommon, line 40
} // library marker davegut.tpLinkChildCommon, line 41

def distChildGetData(devData, data) { // library marker davegut.tpLinkChildCommon, line 43
	switch(devData.method) { // library marker davegut.tpLinkChildCommon, line 44
		case "get_device_info": // library marker davegut.tpLinkChildCommon, line 45
			parse_get_device_info(devData.result, data) // library marker davegut.tpLinkChildCommon, line 46
			break // library marker davegut.tpLinkChildCommon, line 47
		default:  // library marker davegut.tpLinkChildCommon, line 48
			Map logData = [method: "distGetData", data: data, // library marker davegut.tpLinkChildCommon, line 49
						   devMethod: devResp.method, status: "unprocessed"] // library marker davegut.tpLinkChildCommon, line 50
			logDebug(logData) // library marker davegut.tpLinkChildCommon, line 51
	} // library marker davegut.tpLinkChildCommon, line 52
} // library marker davegut.tpLinkChildCommon, line 53

//	===== Refresh /Misc ===== // library marker davegut.tpLinkChildCommon, line 55
def refresh() { parent.refresh() } // library marker davegut.tpLinkChildCommon, line 56

def createMultiCmd(requests) { // library marker davegut.tpLinkChildCommon, line 58
	Map cmdBody = [ // library marker davegut.tpLinkChildCommon, line 59
		method: "multipleRequest", // library marker davegut.tpLinkChildCommon, line 60
		params: [requests: requests]] // library marker davegut.tpLinkChildCommon, line 61
	return cmdBody // library marker davegut.tpLinkChildCommon, line 62
} // library marker davegut.tpLinkChildCommon, line 63

def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkChildCommon, line 65
	Map cmdBody = [method: "control_child", // library marker davegut.tpLinkChildCommon, line 66
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.tpLinkChildCommon, line 67
							requestData: createMultiCmd(requests)]] // library marker davegut.tpLinkChildCommon, line 68
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "childRespDist") // library marker davegut.tpLinkChildCommon, line 69
} // library marker davegut.tpLinkChildCommon, line 70

def sendSingleCmd(request, data, action) { // library marker davegut.tpLinkChildCommon, line 72
	Map cmdBody = [method: "control_child", // library marker davegut.tpLinkChildCommon, line 73
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.tpLinkChildCommon, line 74
							requestData: request]] // library marker davegut.tpLinkChildCommon, line 75
	parent.asyncSend(cmdBody, data, action) // library marker davegut.tpLinkChildCommon, line 76
} // library marker davegut.tpLinkChildCommon, line 77

def updateAttr(attr, value) { // library marker davegut.tpLinkChildCommon, line 79
	if (device.currentValue(attr) != value) { // library marker davegut.tpLinkChildCommon, line 80
		sendEvent(name: attr, value: value) // library marker davegut.tpLinkChildCommon, line 81
	} // library marker davegut.tpLinkChildCommon, line 82
} // library marker davegut.tpLinkChildCommon, line 83

// ~~~~~ end include (62) davegut.tpLinkChildCommon ~~~~~

// ~~~~~ start include (49) davegut.Logging ~~~~~
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

// ~~~~~ end include (49) davegut.Logging ~~~~~
