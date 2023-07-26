/*	TP-Link SMART API / PROTOCOL DRIVER SERIES for plugs, switches, bulbs, hubs and Hub-connected devices.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

This driver is part of a set of drivers for TP-LINK SMART API plugs, switches, hubs,
sensors and thermostats.  The set encompasses the following:
a.	ALL TAPO devices except cameras and Robot Vacuum Cleaners.
b.	NEWER KASA devices using the SMART API except cameras.
	1.	MATTER devices
	2.	Kasa Hub
	3.	Kasa TRV
=================================================================================================*/
def type() {return "tpLink_hub_tempHumidity" }
def gitPath() { return "DaveGut/tpLink_Hubitat/main/Drivers/" }
def driverVer() { return parent.driverVer() }

metadata {
	definition (name: type(), namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/${gitPath()}${type()}.groovy")
	{
		capability "Sensor"
		capability "Temperature Measurement"
		attribute "humidity", "number"
		attribute "lowBattery", "string"
		attribute "status", "string"
	}
	preferences {
		input ("sensorReportInt", "enum", title: "Sensor report interval (secs) (impacts battery life)",
			   options: ["4", "8", "12", "16"], 
			   defaultValue: "8")
	}
}

def installed() { 
	updateAttr("commsError", "OK")
	runIn(1, updated)
}

def updated() {
	unschedule()
	def logData = [:]
	logData << [sensorReportInt: setReportInterval()]
	logData << setLogsOff()
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
	if (device.currentValue("temperature") != childData.current_temperature ||
		device.currentValue("humidity") != childData.current_humidity) {
		sendEvent(name: "temperature", 
				  value: childData.current_temperature,
				  unit: childData.current_temp_unit)
		sendEvent(name: "humidity", childData.current_humidity)
	}
}

def parseTriggerLog(resp, data) {
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

def deviceRefreshParse(childData, data=null) { // library marker davegut.lib_tpLink_sensors, line 10
	try { // library marker davegut.lib_tpLink_sensors, line 11
		def devData = childData.find {it.mac = device.getDeviceNetworkId()} // library marker davegut.lib_tpLink_sensors, line 12
		logDebug("deviceInfoParse: ${devData}") // library marker davegut.lib_tpLink_sensors, line 13
		updateAttr("lowBattery", devData.atLowBattery) // library marker davegut.lib_tpLink_sensors, line 14
		updateAttr("status", status) // library marker davegut.lib_tpLink_sensors, line 15
	} catch (error) { // library marker davegut.lib_tpLink_sensors, line 16
		logWarn("deviceRefreshParse: Failed to capture deviceData from ChildData") // library marker davegut.lib_tpLink_sensors, line 17
	} // library marker davegut.lib_tpLink_sensors, line 18
} // library marker davegut.lib_tpLink_sensors, line 19

def getTriggerLog() { // library marker davegut.lib_tpLink_sensors, line 21
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 22
		method: "control_child", // library marker davegut.lib_tpLink_sensors, line 23
		params: [ // library marker davegut.lib_tpLink_sensors, line 24
			device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 25
			requestData: [ // library marker davegut.lib_tpLink_sensors, line 26
				method: "get_trigger_logs", // library marker davegut.lib_tpLink_sensors, line 27
				params: [page_size: 5,"start_id": 0] // library marker davegut.lib_tpLink_sensors, line 28
			] // library marker davegut.lib_tpLink_sensors, line 29
		] // library marker davegut.lib_tpLink_sensors, line 30
	] // library marker davegut.lib_tpLink_sensors, line 31
	parent.asyncPassthrough(cmdBody, device.getDeviceNetworkId(), "distTriggerLog") // library marker davegut.lib_tpLink_sensors, line 32
} // library marker davegut.lib_tpLink_sensors, line 33

command "getDeveloperData" // library marker davegut.lib_tpLink_sensors, line 35
def getDeveloperData() { // library marker davegut.lib_tpLink_sensors, line 36
	def attrData = device.getCurrentStates() // library marker davegut.lib_tpLink_sensors, line 37
	Map attrs = [:] // library marker davegut.lib_tpLink_sensors, line 38
	attrData.each { // library marker davegut.lib_tpLink_sensors, line 39
		attrs << ["${it.name}": it.value] // library marker davegut.lib_tpLink_sensors, line 40
	} // library marker davegut.lib_tpLink_sensors, line 41
	Date date = new Date() // library marker davegut.lib_tpLink_sensors, line 42
	Map devData = [ // library marker davegut.lib_tpLink_sensors, line 43
		currentTime: date.toString(), // library marker davegut.lib_tpLink_sensors, line 44
		name: device.getName(), // library marker davegut.lib_tpLink_sensors, line 45
		status: device.getStatus(), // library marker davegut.lib_tpLink_sensors, line 46
		dataValues: device.getData(), // library marker davegut.lib_tpLink_sensors, line 47
		attributes: attrs, // library marker davegut.lib_tpLink_sensors, line 48
		devInfo: getDeviceInfo(), // library marker davegut.lib_tpLink_sensors, line 49
		compList: getDeviceComponents() // library marker davegut.lib_tpLink_sensors, line 50
	] // library marker davegut.lib_tpLink_sensors, line 51
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_sensors, line 52
} // library marker davegut.lib_tpLink_sensors, line 53

def getDeviceComponents() { // library marker davegut.lib_tpLink_sensors, line 55
	Map logData = [:] // library marker davegut.lib_tpLink_sensors, line 56
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 57
		device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 58
		method: "get_child_device_component_list" // library marker davegut.lib_tpLink_sensors, line 59
	] // library marker davegut.lib_tpLink_sensors, line 60
	def compList = parent.syncPassthrough(cmdBody) // library marker davegut.lib_tpLink_sensors, line 61
	if (compList == "ERROR") { // library marker davegut.lib_tpLink_sensors, line 62
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]") // library marker davegut.lib_tpLink_sensors, line 63
	} // library marker davegut.lib_tpLink_sensors, line 64
	return compList // library marker davegut.lib_tpLink_sensors, line 65
} // library marker davegut.lib_tpLink_sensors, line 66

def getDeviceInfo() { // library marker davegut.lib_tpLink_sensors, line 68
	logDebug("getChildDeviceInfo") // library marker davegut.lib_tpLink_sensors, line 69
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 70
		method: "control_child", // library marker davegut.lib_tpLink_sensors, line 71
		params: [ // library marker davegut.lib_tpLink_sensors, line 72
			device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 73
			requestData: [ // library marker davegut.lib_tpLink_sensors, line 74
				method: "get_device_info" // library marker davegut.lib_tpLink_sensors, line 75
			] // library marker davegut.lib_tpLink_sensors, line 76
		] // library marker davegut.lib_tpLink_sensors, line 77
	] // library marker davegut.lib_tpLink_sensors, line 78
	def devInfo = parent.syncPassthrough(cmdBody) // library marker davegut.lib_tpLink_sensors, line 79
	if (devInfo == "ERROR") { // library marker davegut.lib_tpLink_sensors, line 80
		logWarn("getDeviceInfo: [ERROR: Error in Sysn Comms]") // library marker davegut.lib_tpLink_sensors, line 81
	} // library marker davegut.lib_tpLink_sensors, line 82
	return devInfo // library marker davegut.lib_tpLink_sensors, line 83
} // library marker davegut.lib_tpLink_sensors, line 84

def setReportInterval() { // library marker davegut.lib_tpLink_sensors, line 86
	def repInt = sensorReportInt.toInteger() // library marker davegut.lib_tpLink_sensors, line 87
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 88
		method: "control_child", // library marker davegut.lib_tpLink_sensors, line 89
		params: [ // library marker davegut.lib_tpLink_sensors, line 90
			device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 91
			requestData: [ // library marker davegut.lib_tpLink_sensors, line 92
				method: "multipleRequest", // library marker davegut.lib_tpLink_sensors, line 93
				params: [ // library marker davegut.lib_tpLink_sensors, line 94
					requests: [ // library marker davegut.lib_tpLink_sensors, line 95
						[method: "set_device_info", // library marker davegut.lib_tpLink_sensors, line 96
						 params: [report_interval: repInt]], // library marker davegut.lib_tpLink_sensors, line 97
						[method: "get_device_info"] // library marker davegut.lib_tpLink_sensors, line 98
					]]]]] // library marker davegut.lib_tpLink_sensors, line 99
	def devInfo = parent.syncPassthrough(cmdBody) // library marker davegut.lib_tpLink_sensors, line 100
	devInfo = devInfo.result.responseData.result.responses.find{it.method == "get_device_info"}.result // library marker davegut.lib_tpLink_sensors, line 101
	updateAttr("reportInterval", devInfo.report_interval) // library marker davegut.lib_tpLink_sensors, line 102
	return buttonReportInt // library marker davegut.lib_tpLink_sensors, line 103
} // library marker davegut.lib_tpLink_sensors, line 104

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_sensors, line 106
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_sensors, line 107
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_sensors, line 108
	} // library marker davegut.lib_tpLink_sensors, line 109
} // library marker davegut.lib_tpLink_sensors, line 110

// ~~~~~ end include (1338) davegut.lib_tpLink_sensors ~~~~~

// ~~~~~ start include (1339) davegut.Logging ~~~~~
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

def setLogsOff() { // library marker davegut.Logging, line 16
	def logData = [logEnagle: logEnable, infoLog: infoLog, traceLog:traceLog] // library marker davegut.Logging, line 17
	if (logEnable) { // library marker davegut.Logging, line 18
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 19
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 20
	} // library marker davegut.Logging, line 21
	if (traceLog) { // library marker davegut.Logging, line 22
		runIn(1800, traceLogOff) // library marker davegut.Logging, line 23
		logData << [traceLogOff: "scheduled"] // library marker davegut.Logging, line 24
	} // library marker davegut.Logging, line 25
	return logData // library marker davegut.Logging, line 26
} // library marker davegut.Logging, line 27

def logTrace(msg){ // library marker davegut.Logging, line 29
	if (traceLog == true) { // library marker davegut.Logging, line 30
		log.trace "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 31
	} // library marker davegut.Logging, line 32
} // library marker davegut.Logging, line 33

def traceLogOff() { // library marker davegut.Logging, line 35
	device.updateSetting("traceLog", [type:"bool", value: false]) // library marker davegut.Logging, line 36
	logInfo("traceLogOff") // library marker davegut.Logging, line 37
} // library marker davegut.Logging, line 38

def logInfo(msg) {  // library marker davegut.Logging, line 40
	if (textEnable || infoLog) { // library marker davegut.Logging, line 41
		log.info "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 42
	} // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def debugLogOff() { // library marker davegut.Logging, line 46
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 47
	logInfo("debugLogOff") // library marker davegut.Logging, line 48
} // library marker davegut.Logging, line 49

def logDebug(msg) { // library marker davegut.Logging, line 51
	if (logEnable || debugLog) { // library marker davegut.Logging, line 52
		log.debug "${device.displayName}-${driverVer()}: ${msg}" // library marker davegut.Logging, line 53
	} // library marker davegut.Logging, line 54
} // library marker davegut.Logging, line 55

def logWarn(msg) { log.warn "${device.displayName}-${driverVer()}: ${msg}" } // library marker davegut.Logging, line 57

def logError(msg) { log.error "${device.displayName}-${driverVer()}: ${msg}" } // library marker davegut.Logging, line 59

// ~~~~~ end include (1339) davegut.Logging ~~~~~
