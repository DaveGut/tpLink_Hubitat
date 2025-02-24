/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md 
=================================================================================================*/

metadata {
	definition (name: "TpLink Child Plug", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_child_plug.groovy")
	{ }
	preferences {
		if (getDataValue("isEm") == "true") {
			emPreferences()
		}
		commonPreferences()
	}
}

def installed() {
	Map logData = [method: "installed"]
	logData << [commonInst: commonInstalled()]
	state.eventType = "digital"
	logInfo(logData)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}

def parse_get_device_info(result, data = null) {
	Map logData = [method: "parse_get_device_info", data: data, result: result]
	switchParse(result)
	logDebug(logData)
}






// ~~~~~ start include (198) davegut.tpLinkCapSwitch ~~~~~
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
	sendDevCmd(requests, device.getDeviceNetworkId(), "parseUpdates") // library marker davegut.tpLinkCapSwitch, line 23
} // library marker davegut.tpLinkCapSwitch, line 24

def switchParse(result) { // library marker davegut.tpLinkCapSwitch, line 26
	Map logData = [method: "switchParse"] // library marker davegut.tpLinkCapSwitch, line 27
	if (result.device_on != null) { // library marker davegut.tpLinkCapSwitch, line 28
		def onOff = "off" // library marker davegut.tpLinkCapSwitch, line 29
		if (result.device_on == true) { onOff = "on" } // library marker davegut.tpLinkCapSwitch, line 30
		if (device.currentValue("switch") != onOff) { // library marker davegut.tpLinkCapSwitch, line 31
			sendEvent(name: "switch", value: onOff, type: state.eventType) // library marker davegut.tpLinkCapSwitch, line 32
			if (getDataValue("isEm") == "true" && !pollInterval.contains("sec")) { // library marker davegut.tpLinkCapSwitch, line 33
				runIn(4, refresh) // library marker davegut.tpLinkCapSwitch, line 34
			} // library marker davegut.tpLinkCapSwitch, line 35
		} // library marker davegut.tpLinkCapSwitch, line 36
		state.eventType = "physical" // library marker davegut.tpLinkCapSwitch, line 37
		logData << [switch: onOff] // library marker davegut.tpLinkCapSwitch, line 38
	} // library marker davegut.tpLinkCapSwitch, line 39
	logDebug(logData) // library marker davegut.tpLinkCapSwitch, line 40
} // library marker davegut.tpLinkCapSwitch, line 41

// ~~~~~ end include (198) davegut.tpLinkCapSwitch ~~~~~

// ~~~~~ start include (197) davegut.tpLinkCapEngMon ~~~~~
library ( // library marker davegut.tpLinkCapEngMon, line 1
	name: "tpLinkCapEngMon", // library marker davegut.tpLinkCapEngMon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapEngMon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapEngMon, line 4
	description: "Hubitat Energy Monitor methods", // library marker davegut.tpLinkCapEngMon, line 5
	category: "utilities", // library marker davegut.tpLinkCapEngMon, line 6
	documentationLink: "" // library marker davegut.tpLinkCapEngMon, line 7
) // library marker davegut.tpLinkCapEngMon, line 8

capability "EnergyMeter" // library marker davegut.tpLinkCapEngMon, line 10
capability "PowerMeter" // library marker davegut.tpLinkCapEngMon, line 11
attribute "past30Energy", "number" // library marker davegut.tpLinkCapEngMon, line 12
attribute "past7Energy", "number" // library marker davegut.tpLinkCapEngMon, line 13

def emUpdated() { // library marker davegut.tpLinkCapEngMon, line 15
	Map logData = [emDataUpdate: "30 mins"] // library marker davegut.tpLinkCapEngMon, line 16
	runEvery30Minutes(getEmData) // library marker davegut.tpLinkCapEngMon, line 17
	getEmData() // library marker davegut.tpLinkCapEngMon, line 18
	return logData // library marker davegut.tpLinkCapEngMon, line 19
} // library marker davegut.tpLinkCapEngMon, line 20

def powerPoll() { // library marker davegut.tpLinkCapEngMon, line 22
	List requests = [[method: "get_current_power"]] // library marker davegut.tpLinkCapEngMon, line 23
	sendDevCmd(requests, device.getDeviceNetworkId(), "parseUpdates") // library marker davegut.tpLinkCapEngMon, line 24
} // library marker davegut.tpLinkCapEngMon, line 25

def parse_get_current_power(result, data) { // library marker davegut.tpLinkCapEngMon, line 27
	Map logData = [method: "parse_get_current_power", data: data, // library marker davegut.tpLinkCapEngMon, line 28
				   power: result.current_power] // library marker davegut.tpLinkCapEngMon, line 29
	updateAttr("power", result.current_power) // library marker davegut.tpLinkCapEngMon, line 30
	logDebug(logData) // library marker davegut.tpLinkCapEngMon, line 31
} // library marker davegut.tpLinkCapEngMon, line 32

def getEmData() { // library marker davegut.tpLinkCapEngMon, line 34
	List requests = [[method: "get_device_usage"]] // library marker davegut.tpLinkCapEngMon, line 35
	sendDevCmd(requests, device.getDeviceNetworkId(), "parseUpdates") // library marker davegut.tpLinkCapEngMon, line 36
} // library marker davegut.tpLinkCapEngMon, line 37

def parse_get_device_usage(result, data) { // library marker davegut.tpLinkCapEngMon, line 39
	Map logData = [method: "parse_get_device_usage", data: data] // library marker davegut.tpLinkCapEngMon, line 40
	def usage = result.power_usage // library marker davegut.tpLinkCapEngMon, line 41
	updateAttr("energy", usage.today) // library marker davegut.tpLinkCapEngMon, line 42
	updateAttr("past30Energy", usage.past30) // library marker davegut.tpLinkCapEngMon, line 43
	updateAttr("past7Energy", usage.past7) // library marker davegut.tpLinkCapEngMon, line 44
	logData << [energy:usage.today, past30Energy: usage.past30, past7Energy: usage.past7] // library marker davegut.tpLinkCapEngMon, line 45
	logDebug(logData) // library marker davegut.tpLinkCapEngMon, line 46
} // library marker davegut.tpLinkCapEngMon, line 47

// ~~~~~ end include (197) davegut.tpLinkCapEngMon ~~~~~

// ~~~~~ start include (200) davegut.tpLinkChildCommon ~~~~~
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
	input ("syncName", "enum", title: "Update Device Names and Labels",  // library marker davegut.tpLinkChildCommon, line 13
		   options: ["hubMaster", "tapoAppMaster", "notSet"], defaultValue: "notSet") // library marker davegut.tpLinkChildCommon, line 14
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkChildCommon, line 15
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkChildCommon, line 16
} // library marker davegut.tpLinkChildCommon, line 17

def commonInstalled() { // library marker davegut.tpLinkChildCommon, line 19
	runIn(1, updated) // library marker davegut.tpLinkChildCommon, line 20
	return [eventType: "digital"] // library marker davegut.tpLinkChildCommon, line 21
} // library marker davegut.tpLinkChildCommon, line 22

def commonUpdated() { // library marker davegut.tpLinkChildCommon, line 24
	unschedule() // library marker davegut.tpLinkChildCommon, line 25
	Map logData = [logging: setLogsOff()] // library marker davegut.tpLinkChildCommon, line 26
	updDevSettings() // library marker davegut.tpLinkChildCommon, line 27
	if (getDataValue("isEm") == "true") { // library marker davegut.tpLinkChildCommon, line 28
		logData << [emUpdated: emUpdated()] // library marker davegut.tpLinkChildCommon, line 29
	} // library marker davegut.tpLinkChildCommon, line 30
	pauseExecution(5000) // library marker davegut.tpLinkChildCommon, line 31
	return logData // library marker davegut.tpLinkChildCommon, line 32
} // library marker davegut.tpLinkChildCommon, line 33

def updDevSettings() { // library marker davegut.tpLinkChildCommon, line 35
	Map params = [:] // library marker davegut.tpLinkChildCommon, line 36
	String tempUnit = "celsius" // library marker davegut.tpLinkChildCommon, line 37
	if (tempScale != null && tempScale != "C")  { // library marker davegut.tpLinkChildCommon, line 38
		tempUnit = "fahrenheit" // library marker davegut.tpLinkChildCommon, line 39
		params << [temp_unit: tempUnit] // library marker davegut.tpLinkChildCommon, line 40
	} // library marker davegut.tpLinkChildCommon, line 41
	if (maxCtrlTemp != null) { // library marker davegut.tpLinkChildCommon, line 42
		params << [frost_protection_on: frostProtect, // library marker davegut.tpLinkChildCommon, line 43
				   max_control_temp: maxCtrlTemp, // library marker davegut.tpLinkChildCommon, line 44
				   min_control_temp: minCtrlTemp]	 // library marker davegut.tpLinkChildCommon, line 45
	} // library marker davegut.tpLinkChildCommon, line 46
	if (syncName == "hubMaster") { // library marker davegut.tpLinkChildCommon, line 47
		String nickname = device.getLabel().bytes.encodeBase64().toString() // library marker davegut.tpLinkChildCommon, line 48
		params << [nickname: nickname] // library marker davegut.tpLinkChildCommon, line 49
		device.updateSetting("syncName",[type:"enum", value: "notSet"]) // library marker davegut.tpLinkChildCommon, line 50
	} // library marker davegut.tpLinkChildCommon, line 51
	List requests = [ // library marker davegut.tpLinkChildCommon, line 52
		[method: "set_device_info", params: params], // library marker davegut.tpLinkChildCommon, line 53
		[method: "get_device_info"] // library marker davegut.tpLinkChildCommon, line 54
	] // library marker davegut.tpLinkChildCommon, line 55
	sendDevCmd(requests, "updDevSettings", "parseUpdates") // library marker davegut.tpLinkChildCommon, line 56
} // library marker davegut.tpLinkChildCommon, line 57

def refresh() { // library marker davegut.tpLinkChildCommon, line 59
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkChildCommon, line 60
	if (getDataValue("isEm") == "true") { // library marker davegut.tpLinkChildCommon, line 61
		requests << [method: "get_current_power"] // library marker davegut.tpLinkChildCommon, line 62
	} // library marker davegut.tpLinkChildCommon, line 63
	sendDevCmd(requests, device.getDeviceNetworkId(), "parseUpdates") // library marker davegut.tpLinkChildCommon, line 64
} // library marker davegut.tpLinkChildCommon, line 65

def parseNameUpdate(result) { // library marker davegut.tpLinkChildCommon, line 67
	if (syncName != "notSet") { // library marker davegut.tpLinkChildCommon, line 68
		Map logData = [method: "parseNameUpdate"] // library marker davegut.tpLinkChildCommon, line 69
		byte[] plainBytes = result.nickname.decodeBase64() // library marker davegut.tpLinkChildCommon, line 70
		def newLabel = new String(plainBytes) // library marker davegut.tpLinkChildCommon, line 71
		device.setLabel(newLabel) // library marker davegut.tpLinkChildCommon, line 72
		device.updateSetting("syncName",[type:"enum", value: "notSet"]) // library marker davegut.tpLinkChildCommon, line 73
		logData << [label: newLabel] // library marker davegut.tpLinkChildCommon, line 74
		logDebug(logData) // library marker davegut.tpLinkChildCommon, line 75
	} // library marker davegut.tpLinkChildCommon, line 76
} // library marker davegut.tpLinkChildCommon, line 77

//	===== Comms, etc ===== // library marker davegut.tpLinkChildCommon, line 79
def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkChildCommon, line 80
	Map multiCmdBody = [ // library marker davegut.tpLinkChildCommon, line 81
		method: "multipleRequest", // library marker davegut.tpLinkChildCommon, line 82
		params: [requests: requests]] // library marker davegut.tpLinkChildCommon, line 83
	Map cmdBody = [method: "control_child", // library marker davegut.tpLinkChildCommon, line 84
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.tpLinkChildCommon, line 85
							requestData: multiCmdBody]] // library marker davegut.tpLinkChildCommon, line 86
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), action) // library marker davegut.tpLinkChildCommon, line 87
} // library marker davegut.tpLinkChildCommon, line 88

def sendSingleCmd(request, data, action) { // library marker davegut.tpLinkChildCommon, line 90
	Map cmdBody = [method: "control_child", // library marker davegut.tpLinkChildCommon, line 91
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.tpLinkChildCommon, line 92
							requestData: request]] // library marker davegut.tpLinkChildCommon, line 93
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), action) // library marker davegut.tpLinkChildCommon, line 94
} // library marker davegut.tpLinkChildCommon, line 95

// ~~~~~ end include (200) davegut.tpLinkChildCommon ~~~~~

// ~~~~~ start include (195) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def nameSpace() { return "davegut" } // library marker davegut.Logging, line 10

def version() { return "2.4.1a" } // library marker davegut.Logging, line 12

def label() { // library marker davegut.Logging, line 14
	if (device) {  // library marker davegut.Logging, line 15
		return device.displayName + "-${version()}" // library marker davegut.Logging, line 16
	} else {  // library marker davegut.Logging, line 17
		return app.getLabel() + "-${version()}" // library marker davegut.Logging, line 18
	} // library marker davegut.Logging, line 19
} // library marker davegut.Logging, line 20

def updateAttr(attr, value) { // library marker davegut.Logging, line 22
	if (device.currentValue(attr) != value) { // library marker davegut.Logging, line 23
		sendEvent(name: attr, value: value) // library marker davegut.Logging, line 24
	} // library marker davegut.Logging, line 25
} // library marker davegut.Logging, line 26

def listAttributes() { // library marker davegut.Logging, line 28
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 29
	Map attrs = [:] // library marker davegut.Logging, line 30
	attrData.each { // library marker davegut.Logging, line 31
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 32
	} // library marker davegut.Logging, line 33
	return attrs // library marker davegut.Logging, line 34
} // library marker davegut.Logging, line 35

def setLogsOff() { // library marker davegut.Logging, line 37
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 38
	if (logEnable) { // library marker davegut.Logging, line 39
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 40
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 41
	} // library marker davegut.Logging, line 42
	return logData // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def logTrace(msg){ log.trace "${label()}: ${msg}" } // library marker davegut.Logging, line 46

def logInfo(msg) {  // library marker davegut.Logging, line 48
	if (infoLog) { log.info "${label()}: ${msg}" } // library marker davegut.Logging, line 49
} // library marker davegut.Logging, line 50

def debugLogOff() { // library marker davegut.Logging, line 52
	if (device) { // library marker davegut.Logging, line 53
		device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 54
	} else { // library marker davegut.Logging, line 55
		app.updateSetting("logEnable", false) // library marker davegut.Logging, line 56
	} // library marker davegut.Logging, line 57
	logInfo("debugLogOff") // library marker davegut.Logging, line 58
} // library marker davegut.Logging, line 59

def logDebug(msg) { // library marker davegut.Logging, line 61
	if (logEnable) { log.debug "${label()}: ${msg}" } // library marker davegut.Logging, line 62
} // library marker davegut.Logging, line 63

def logWarn(msg) { log.warn "${label()}: ${msg}" } // library marker davegut.Logging, line 65

def logError(msg) { log.error "${label()}: ${msg}" } // library marker davegut.Logging, line 67

// ~~~~~ end include (195) davegut.Logging ~~~~~
