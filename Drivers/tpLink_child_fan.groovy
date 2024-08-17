/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Not verified.  Simulated using TP-25(US) using two lines of added code to simulate fan level.
=================================================================================================*/
import groovy.json.JsonBuilder

metadata {
	definition (name: "TpLink Child Fan", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_child_fan.groovy")
	{
		capability "Refresh"
		capability "FanControl"
		attribute "level", "NUMBER"
	}
	preferences {
		commonPreferences()
	}

}

def installed() {
	Map logData = [method: "installed"]
	List supportedFanSpeeds = ["low","medium-low","medium","high"]
	def fanSpeedsAttr = new groovy.json.JsonBuilder(supportedFanSpeeds)
	sendEvent(name: supportedFanSpeeds, value: value)
	logData << [supportedFanSpeeds: supportedFanSpeeds]
	state.NOTICE = "The driver is in a TEST state and requires YOUR assistance.  Try ON, OFF, and Set Speed functions and report back to developer with log data"
	logData << [commonInst: commonInstalled()]
	logInfo(logData)
	runIn(1, updated)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}

def setSpeed(fanspeed) {
	Map logData = [method: "setSpeed", fanspeed: fanspeed]
	int level = 99
	switch(fanspeed) {
		case "low": level = 1; break
		case "medium-low": level = 2; break
		case "medium": level = 3; break
		case "medium-high":
			level = 4
			logData << [ALERT: "MED-HIGH NOT SUPPORTED.  SET TO HIGH"]
			break
		case "high": level = 4; break
		case "on":
			level = device.currentValue("level")
			logData << [switch: "on"]
			break
		case "off":
			off()
			logData << [switch: "off"]
			break
		case "auto":
			logData << [ALERT: "AUTO NOT SUPPORTED.  REQUEST IGNORED"]
			break
		default: 
			logData << [ALERT: "UNKNOWN SetSpeed ERROR"]
	}
	logData << [level: level]
	if (level <= 4) {
		List requests = [[
			method: "set_device_info",
			params: [device_on: true, fan_speed_level: level]]]
		requests << [method: "get_device_info"]
		Map cmdBody = [method: "control_child",
					   params: [device_id: getDataValue("deviceId"),
							requestData: createMultiCmd(requests)]]
		sendDevCmd(requests, "setSpeed", "parseUpdates")
	}
	if (logData.ALERT == null) {
		logDebug(logData)
	} else {
		logWarn(logData)
	}
}

def cycleSpeed() {
	def currSpeed = device.currentValue("speed")
	List cycles = ["low","medium-low","medium","high", "off"]
	def nextIndex = cycles.findIndexValues { it == currSpeed }[0] + 1
	if (nextIndex > 3) { nextIndex = 0 }
	setSpeed(cycles[nextIndex.toInteger()])
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
		def level = devData.fan_speed_level
		updateAttr("level", level)
		def speed
		switch(level) {
			case 1: speed = "low"; break
			case 2: speed = "medium-low"; break
			case 3: speed = "medium"; break
			case 4: speed = "high"; break
			default: speed = level
		}
		updateAttr("speed", speed)
		logData << [onOff: onOff, fanSpeed: devData.fan_speed_level, status: "OK"]
		logDebug(logData)
	} catch (err) {
		logData << [status: "FAILED", error: err]
		logWarn(logData)
	}
}





// ~~~~~ start include (81) davegut.tpLinkCapSwitch ~~~~~
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

// ~~~~~ end include (81) davegut.tpLinkCapSwitch ~~~~~

// ~~~~~ start include (83) davegut.tpLinkChildCommon ~~~~~
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

// ~~~~~ end include (83) davegut.tpLinkChildCommon ~~~~~

// ~~~~~ start include (79) davegut.Logging ~~~~~
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

// ~~~~~ end include (79) davegut.Logging ~~~~~
