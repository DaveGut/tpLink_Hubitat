/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Updates from previous version.
a.	Added Klap protocol with associated auto-use logic.
b.	Streamlined comms error processing and synched process to app find device.
c.	Added driver for Multi-plug to set.
d.	Added additional preferences (as appropriate) to child devices (sensors, multi-plug outlets)
e.	Added battery state attribute to sensors.
=================================================================================================*/
def gitPath() { return "DaveGut/tpLink_Hubitat/main/Drivers/" }
def type() { return "tpLink_plug_multi_child" }

metadata {
	definition (name: "tpLink_plug_multi_child", namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/${gitPath()}${type()}.groovy")
	{
		capability "Switch"
	}
	preferences {
		input ("nameSync", "enum", title: "Synchronize Names",
			   options: ["none": "Don't synchronize",
						 "device" : "TP-Link device name master",
						 "Hubitat" : "Hubitat label master"],
			   defaultValue: "none")
		input ("defState", "enum", title: "Power Loss Default State",
			   options: ["lastState", "on", "off"], defaultValue: "lastState")
		input ("developerData", "bool", title: "Get Data for Developer", defaultValue: false)
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { runIn(5, updated) }

def updated() {
	unschedule()
	Map logData = [method: "updated"]
	logData << [syncName: syncName()]
	logData << [setDefaultState: setDefaultState()]
	if (developerData) { getDeveloperData() }
	refresh()
	logInfo(logData)
}

def on() { setPower(true) }

def off() { setPower(false) }

def setPower(onOff) {
	Map logData = [method: "setPower", onOff: onOff]
	state.eventType = "digital"
	List requests = [[method: "set_device_info", params: [device_on: onOff]]]
	requests << [method: "get_device_info"]
	Map cmdBody = [method: "control_child",
				   params: [device_id: getDataValue("deviceId"),
							requestData: createMultiCmd(requests)]]
	try {
		def cmdResp = parent.syncSend(cmdBody)
		logData << [status: parseDevData(getDeviceInfoData(cmdResp).result)]
	} catch (err) {
		logData << [status: "ERROR", error: err]
	}
	logDebug(logData)
}

def parseDevData(devData) {
	Map logData = [method: "parseDevData"]
	try {
		def onOff = "off"
		if (devData.device_on == true) { onOff = "on" }
		if (device.currentValue("switch") != onOff) {
			sendEvent(name: "switch", value: onOff, type: state.eventType)
			state.eventType = "physical"
		}
		logData << [onOff: onOff, status: "OK"]
	} catch (err) {
		logData << [status: "FAILED", error: err]
	}
	return logData
}




// ~~~~~ start include (1380) davegut.lib_tpLink_child_common ~~~~~
library ( // library marker davegut.lib_tpLink_child_common, line 1
	name: "lib_tpLink_child_common", // library marker davegut.lib_tpLink_child_common, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_child_common, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_child_common, line 4
	description: "Method common to tpLink child device drivers", // library marker davegut.lib_tpLink_child_common, line 5
	category: "utilities", // library marker davegut.lib_tpLink_child_common, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_child_common, line 7
) // library marker davegut.lib_tpLink_child_common, line 8
capability "Refresh" // library marker davegut.lib_tpLink_child_common, line 9
def version() { return parent.version() } // library marker davegut.lib_tpLink_child_common, line 10
def label() { return device.displayName } // library marker davegut.lib_tpLink_child_common, line 11

def refresh() { // library marker davegut.lib_tpLink_child_common, line 13
	Map logData = [method: "refresh"] // library marker davegut.lib_tpLink_child_common, line 14
	try { // library marker davegut.lib_tpLink_child_common, line 15
		def cmdResp = getChildDeviceInfo().result.responseData // library marker davegut.lib_tpLink_child_common, line 16
		logData << [status: parseDevData(cmdResp.result)] // library marker davegut.lib_tpLink_child_common, line 17
	} catch (err) { // library marker davegut.lib_tpLink_child_common, line 18
		logData << [status: "ERROR", error: err] // library marker davegut.lib_tpLink_child_common, line 19
	} // library marker davegut.lib_tpLink_child_common, line 20
	logDebug(logData) // library marker davegut.lib_tpLink_child_common, line 21
} // library marker davegut.lib_tpLink_child_common, line 22

def syncName() { // library marker davegut.lib_tpLink_child_common, line 24
	Map logData = [syncName: nameSync] // library marker davegut.lib_tpLink_child_common, line 25
	if (nameSync == "none") { // library marker davegut.lib_tpLink_child_common, line 26
		logData << [status: "Label Not Updated"] // library marker davegut.lib_tpLink_child_common, line 27
	} else { // library marker davegut.lib_tpLink_child_common, line 28
		def cmdResp // library marker davegut.lib_tpLink_child_common, line 29
		String nickname // library marker davegut.lib_tpLink_child_common, line 30
		if (nameSync == "Hubitat") { // library marker davegut.lib_tpLink_child_common, line 31
			nickname = device.getLabel().bytes.encodeBase64().toString() // library marker davegut.lib_tpLink_child_common, line 32
			List requests = [[method: "set_device_info", params: [nickname: nickname]]] // library marker davegut.lib_tpLink_child_common, line 33
			requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_child_common, line 34
			Map cmdBody = [method: "control_child", // library marker davegut.lib_tpLink_child_common, line 35
						   params: [device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_child_common, line 36
									requestData: createMultiCmd(requests)]] // library marker davegut.lib_tpLink_child_common, line 37
			def multiResp = parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_child_common, line 38
			cmdResp = getDeviceInfoData(multiResp).result // library marker davegut.lib_tpLink_child_common, line 39
		} else { // library marker davegut.lib_tpLink_child_common, line 40
			cmdResp = getChildDeviceInfo().result.responseData.result // library marker davegut.lib_tpLink_child_common, line 41
		} // library marker davegut.lib_tpLink_child_common, line 42
		nickname = cmdResp.nickname // library marker davegut.lib_tpLink_child_common, line 43
		byte[] plainBytes = nickname.decodeBase64() // library marker davegut.lib_tpLink_child_common, line 44
		String label = new String(plainBytes) // library marker davegut.lib_tpLink_child_common, line 45
		device.setLabel(label) // library marker davegut.lib_tpLink_child_common, line 46
		logData << [nickname: nickname, label: label, status: "Label Updated"] // library marker davegut.lib_tpLink_child_common, line 47
	} // library marker davegut.lib_tpLink_child_common, line 48
	device.updateSetting("nameSync", [type: "enum", value: "none"]) // library marker davegut.lib_tpLink_child_common, line 49
	return logData // library marker davegut.lib_tpLink_child_common, line 50
} // library marker davegut.lib_tpLink_child_common, line 51

def setDefaultState() { // library marker davegut.lib_tpLink_child_common, line 53
	Map logData = [defState: defState] // library marker davegut.lib_tpLink_child_common, line 54
	def type = "last_states" // library marker davegut.lib_tpLink_child_common, line 55
	def state = [] // library marker davegut.lib_tpLink_child_common, line 56
	if (defState == "on") { // library marker davegut.lib_tpLink_child_common, line 57
		type = "custom" // library marker davegut.lib_tpLink_child_common, line 58
		state = [on: true] // library marker davegut.lib_tpLink_child_common, line 59
	} else if (defState == "off") { // library marker davegut.lib_tpLink_child_common, line 60
		type = "custom" // library marker davegut.lib_tpLink_child_common, line 61
		state = [on: false] // library marker davegut.lib_tpLink_child_common, line 62
	} // library marker davegut.lib_tpLink_child_common, line 63
	List requests = [[method: "set_device_info",  // library marker davegut.lib_tpLink_child_common, line 64
					  params: [default_states:[type: type, state: state]]]] // library marker davegut.lib_tpLink_child_common, line 65
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_child_common, line 66
	Map cmdBody = [method: "control_child", // library marker davegut.lib_tpLink_child_common, line 67
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_child_common, line 68
							requestData: createMultiCmd(requests)]] // library marker davegut.lib_tpLink_child_common, line 69
	def cmdResp = parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_child_common, line 70
	cmdResp = getDeviceInfoData(cmdResp).result // library marker davegut.lib_tpLink_child_common, line 71
	def defaultStates = cmdResp.default_states // library marker davegut.lib_tpLink_child_common, line 72
	def newState = "lastState" // library marker davegut.lib_tpLink_child_common, line 73
	if (defaultStates.type == "custom"){ // library marker davegut.lib_tpLink_child_common, line 74
		newState = "off" // library marker davegut.lib_tpLink_child_common, line 75
		if (defaultStates.state.on == true) { // library marker davegut.lib_tpLink_child_common, line 76
			newState = "on" // library marker davegut.lib_tpLink_child_common, line 77
		} // library marker davegut.lib_tpLink_child_common, line 78
	} // library marker davegut.lib_tpLink_child_common, line 79
	device.updateSetting("defState", [type: "enum", value: newState]) // library marker davegut.lib_tpLink_child_common, line 80
	if (newState == defState) { // library marker davegut.lib_tpLink_child_common, line 81
		logData << [status: "OK"] // library marker davegut.lib_tpLink_child_common, line 82
	} else { // library marker davegut.lib_tpLink_child_common, line 83
		logData << [status: "FAILED"] // library marker davegut.lib_tpLink_child_common, line 84
	} // library marker davegut.lib_tpLink_child_common, line 85
	return logData // library marker davegut.lib_tpLink_child_common, line 86
} // library marker davegut.lib_tpLink_child_common, line 87

def createMultiCmd(requests) { // library marker davegut.lib_tpLink_child_common, line 89
	Map cmdBody = [ // library marker davegut.lib_tpLink_child_common, line 90
		method: "multipleRequest", // library marker davegut.lib_tpLink_child_common, line 91
		params: [requests: requests]] // library marker davegut.lib_tpLink_child_common, line 92
	return cmdBody // library marker davegut.lib_tpLink_child_common, line 93
} // library marker davegut.lib_tpLink_child_common, line 94

def getDeviceInfoData(cmdResp) { // library marker davegut.lib_tpLink_child_common, line 96
	def responses = cmdResp.result.responseData.result.responses // library marker davegut.lib_tpLink_child_common, line 97
	Map devInfo = responses.find { it.method == "get_device_info" } // library marker davegut.lib_tpLink_child_common, line 98
	return devInfo // library marker davegut.lib_tpLink_child_common, line 99
}	 // library marker davegut.lib_tpLink_child_common, line 100

def getDeveloperData() { // library marker davegut.lib_tpLink_child_common, line 102
	def attrData = device.getCurrentStates() // library marker davegut.lib_tpLink_child_common, line 103
	Map attrs = [:] // library marker davegut.lib_tpLink_child_common, line 104
	attrData.each { // library marker davegut.lib_tpLink_child_common, line 105
		attrs << ["${it.name}": it.value] // library marker davegut.lib_tpLink_child_common, line 106
	} // library marker davegut.lib_tpLink_child_common, line 107
	Date date = new Date() // library marker davegut.lib_tpLink_child_common, line 108
	Map devData = [ // library marker davegut.lib_tpLink_child_common, line 109
		currentTime: date.toString(), // library marker davegut.lib_tpLink_child_common, line 110
		name: device.getName(), // library marker davegut.lib_tpLink_child_common, line 111
		status: device.getStatus(), // library marker davegut.lib_tpLink_child_common, line 112
		dataValues: device.getData(), // library marker davegut.lib_tpLink_child_common, line 113
		attributes: attrs, // library marker davegut.lib_tpLink_child_common, line 114
		devInfo: getChildDeviceInfo(), // library marker davegut.lib_tpLink_child_common, line 115
		compList: getDeviceComponents() // library marker davegut.lib_tpLink_child_common, line 116
	] // library marker davegut.lib_tpLink_child_common, line 117
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_child_common, line 118
} // library marker davegut.lib_tpLink_child_common, line 119

def getDeviceComponents() { // library marker davegut.lib_tpLink_child_common, line 121
	device.updateSetting("developerData",[type:"bool", value: false]) // library marker davegut.lib_tpLink_child_common, line 122
	Map logData = [:] // library marker davegut.lib_tpLink_child_common, line 123
	Map cmdBody = [ // library marker davegut.lib_tpLink_child_common, line 124
		device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_child_common, line 125
		method: "get_child_device_component_list" // library marker davegut.lib_tpLink_child_common, line 126
	] // library marker davegut.lib_tpLink_child_common, line 127
	def compList = parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_child_common, line 128
	if (compList == "ERROR") { // library marker davegut.lib_tpLink_child_common, line 129
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]") // library marker davegut.lib_tpLink_child_common, line 130
	} // library marker davegut.lib_tpLink_child_common, line 131
	return compList // library marker davegut.lib_tpLink_child_common, line 132
} // library marker davegut.lib_tpLink_child_common, line 133

def getChildDeviceInfo() { // library marker davegut.lib_tpLink_child_common, line 135
	Map cmdBody = [method: "control_child", // library marker davegut.lib_tpLink_child_common, line 136
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_child_common, line 137
							requestData: [method: "get_device_info"]]] // library marker davegut.lib_tpLink_child_common, line 138
	return parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_child_common, line 139
} // library marker davegut.lib_tpLink_child_common, line 140

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_child_common, line 142
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_child_common, line 143
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_child_common, line 144
	} // library marker davegut.lib_tpLink_child_common, line 145
} // library marker davegut.lib_tpLink_child_common, line 146

// ~~~~~ end include (1380) davegut.lib_tpLink_child_common ~~~~~

// ~~~~~ start include (1339) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def listAttributes() { // library marker davegut.Logging, line 10
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 11
	Map attrs = [:] // library marker davegut.Logging, line 12
	attrData.each { // library marker davegut.Logging, line 13
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 14
	} // library marker davegut.Logging, line 15
	return attrs // library marker davegut.Logging, line 16
} // library marker davegut.Logging, line 17

def setLogsOff() { // library marker davegut.Logging, line 19
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 20
	if (logEnable) { // library marker davegut.Logging, line 21
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 22
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 23
	} // library marker davegut.Logging, line 24
	return logData // library marker davegut.Logging, line 25
} // library marker davegut.Logging, line 26

def logTrace(msg){ // library marker davegut.Logging, line 28
	log.trace "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 29
} // library marker davegut.Logging, line 30

def logInfo(msg) {  // library marker davegut.Logging, line 32
	if (textEnable || infoLog) { // library marker davegut.Logging, line 33
		log.info "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 34
	} // library marker davegut.Logging, line 35
} // library marker davegut.Logging, line 36

def debugLogOff() { // library marker davegut.Logging, line 38
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 39
	logInfo("debugLogOff") // library marker davegut.Logging, line 40
} // library marker davegut.Logging, line 41

def logDebug(msg) { // library marker davegut.Logging, line 43
	if (logEnable || debugLog) { // library marker davegut.Logging, line 44
		log.debug "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 45
	} // library marker davegut.Logging, line 46
} // library marker davegut.Logging, line 47

def logWarn(msg) { // library marker davegut.Logging, line 49
	log.warn "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 50
} // library marker davegut.Logging, line 51

def logError(msg) { // library marker davegut.Logging, line 53
	log.error "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 54
} // library marker davegut.Logging, line 55

// ~~~~~ end include (1339) davegut.Logging ~~~~~
