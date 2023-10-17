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
def type() {return "tpLink_hub_button" }

metadata {
	definition (name: "tpLink_hub_button", namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/${gitPath()}${type()}.groovy")
	{
		capability "Sensor"
		attribute "lastTrigger", "string"
		attribute "lastTriggerNo", "number"
		attribute "triggerTimestamp", "number"
		attribute "lowBattery", "string"
	}
	preferences {
		input ("nameSync", "enum", title: "Synchronize Names",
			   options: ["none": "Don't synchronize",
						 "device" : "TP-Link device name master",
						 "Hubitat" : "Hubitat label master"],
			   defaultValue: "none")
		input ("developerData", "bool", title: "Get Data for Developer", defaultValue: false)
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { 
	updateAttr("commsError", "OK")
	runIn(1, updated)
}

def updated() {
	unschedule()
	def logData = [:]
	logData << setLogsOff()
	if (developerData) { getDeveloperData() }
	logData << [status: "OK"]
	if (logData.status == "ERROR") {
		logError("updated: ${logData}")
	} else {
		logInfo("updated: ${logData}")
	}
}

//	Parse Methods
def devicePollParse(childData, data=null) {
	childData = childData.find{ it.mac == device.getDeviceNetworkId() }
	if (device.currentValue("lowBattery") != childData.atLowBattery.toString()) {
		updateAttr("lowBattery", childData.at_low_battery.toString())
	}
	getTriggerLog()
}

def parseTriggerLog(triggerData, data=null) {
	if (triggerData.status == "OK" && triggerData.cmdResp.result.responseData.error_code == 0) {
		def triggerLog = triggerData.cmdResp.result.responseData.result
		if (device.currentValue("lastTriggerNo") != triggerLog.start_id) {
			updateAttr("lastTriggerNo", triggerLog.start_id)
			def thisTrigger = triggerLog.logs.find{ it.id == triggerLog.start_id }
			def trigger = thisTrigger.event
			if (trigger == "rotation") {
				trigger = thisTrigger.params.rotate_deg
			}
			sendEvent(name: "lastTrigger", value: trigger, isStateChange: true)
			updateAttr("triggerTimestamp", thisTrigger.timestamp)
		}
	} else {
		logWarn("parseTriggerLog: ${triggerData}")
	}
}

//	Library Inclusion



// ~~~~~ start include (1338) davegut.lib_tpLink_sensors ~~~~~
library ( // library marker davegut.lib_tpLink_sensors, line 1
	name: "lib_tpLink_sensors", // library marker davegut.lib_tpLink_sensors, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_sensors, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_sensors, line 4
	description: "Common Tapo Sensor Methods", // library marker davegut.lib_tpLink_sensors, line 5
	category: "utilities", // library marker davegut.lib_tpLink_sensors, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_sensors, line 7
) // library marker davegut.lib_tpLink_sensors, line 8
capability "Refresh" // library marker davegut.lib_tpLink_sensors, line 9
def version() { return parent.version() } // library marker davegut.lib_tpLink_sensors, line 10
def label() { return device.displayName } // library marker davegut.lib_tpLink_sensors, line 11

def refresh() { // library marker davegut.lib_tpLink_sensors, line 13
	parent.refresh() // library marker davegut.lib_tpLink_sensors, line 14
} // library marker davegut.lib_tpLink_sensors, line 15

def getTriggerLog() { // library marker davegut.lib_tpLink_sensors, line 17
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 18
		method: "control_child", // library marker davegut.lib_tpLink_sensors, line 19
		params: [ // library marker davegut.lib_tpLink_sensors, line 20
			device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 21
			requestData: [ // library marker davegut.lib_tpLink_sensors, line 22
				method: "get_trigger_logs", // library marker davegut.lib_tpLink_sensors, line 23
				params: [page_size: 5,"start_id": 0] // library marker davegut.lib_tpLink_sensors, line 24
			] // library marker davegut.lib_tpLink_sensors, line 25
		] // library marker davegut.lib_tpLink_sensors, line 26
	] // library marker davegut.lib_tpLink_sensors, line 27
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "distTriggerLog") // library marker davegut.lib_tpLink_sensors, line 28
} // library marker davegut.lib_tpLink_sensors, line 29

def getDeveloperData() { // library marker davegut.lib_tpLink_sensors, line 31
	device.updateSetting("developerData",[type:"bool", value: false]) // library marker davegut.lib_tpLink_sensors, line 32
	def attrData = device.getCurrentStates() // library marker davegut.lib_tpLink_sensors, line 33
	Map attrs = [:] // library marker davegut.lib_tpLink_sensors, line 34
	attrData.each { // library marker davegut.lib_tpLink_sensors, line 35
		attrs << ["${it.name}": it.value] // library marker davegut.lib_tpLink_sensors, line 36
	} // library marker davegut.lib_tpLink_sensors, line 37
	Date date = new Date() // library marker davegut.lib_tpLink_sensors, line 38
	Map devData = [ // library marker davegut.lib_tpLink_sensors, line 39
		currentTime: date.toString(), // library marker davegut.lib_tpLink_sensors, line 40
		name: device.getName(), // library marker davegut.lib_tpLink_sensors, line 41
		status: device.getStatus(), // library marker davegut.lib_tpLink_sensors, line 42
		dataValues: device.getData(), // library marker davegut.lib_tpLink_sensors, line 43
		attributes: attrs, // library marker davegut.lib_tpLink_sensors, line 44
		devInfo: getChildDeviceInfo(), // library marker davegut.lib_tpLink_sensors, line 45
		compList: getDeviceComponents() // library marker davegut.lib_tpLink_sensors, line 46
	] // library marker davegut.lib_tpLink_sensors, line 47
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_sensors, line 48
} // library marker davegut.lib_tpLink_sensors, line 49

def getDeviceComponents() { // library marker davegut.lib_tpLink_sensors, line 51
	Map logData = [:] // library marker davegut.lib_tpLink_sensors, line 52
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 53
		device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 54
		method: "get_child_device_component_list" // library marker davegut.lib_tpLink_sensors, line 55
	] // library marker davegut.lib_tpLink_sensors, line 56
	def compList = parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_sensors, line 57
	if (compList == "ERROR") { // library marker davegut.lib_tpLink_sensors, line 58
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]") // library marker davegut.lib_tpLink_sensors, line 59
	} // library marker davegut.lib_tpLink_sensors, line 60
	return compList // library marker davegut.lib_tpLink_sensors, line 61
} // library marker davegut.lib_tpLink_sensors, line 62

def getChildDeviceInfo() { // library marker davegut.lib_tpLink_sensors, line 64
	Map cmdBody = [method: "control_child", // library marker davegut.lib_tpLink_sensors, line 65
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 66
							requestData: [method: "get_device_info"]]] // library marker davegut.lib_tpLink_sensors, line 67
	return parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_sensors, line 68
} // library marker davegut.lib_tpLink_sensors, line 69

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_sensors, line 71
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_sensors, line 72
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_sensors, line 73
	} // library marker davegut.lib_tpLink_sensors, line 74
} // library marker davegut.lib_tpLink_sensors, line 75

/*	Future. // library marker davegut.lib_tpLink_sensors, line 77
attribute "lowBattery", "string" // library marker davegut.lib_tpLink_sensors, line 78
attribute "status", "string" // library marker davegut.lib_tpLink_sensors, line 79
def deviceRefreshParse(childData, data=null) { // library marker davegut.lib_tpLink_sensors, line 80
	try { // library marker davegut.lib_tpLink_sensors, line 81
		def devData = childData.find {it.mac = device.getDeviceNetworkId()} // library marker davegut.lib_tpLink_sensors, line 82
		updateAttr("lowBattery", devData.atLowBattery) // library marker davegut.lib_tpLink_sensors, line 83
		updateAttr("status", status) // library marker davegut.lib_tpLink_sensors, line 84
	} catch (error) { // library marker davegut.lib_tpLink_sensors, line 85
	} // library marker davegut.lib_tpLink_sensors, line 86
} // library marker davegut.lib_tpLink_sensors, line 87
*/ // library marker davegut.lib_tpLink_sensors, line 88

// ~~~~~ end include (1338) davegut.lib_tpLink_sensors ~~~~~

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
