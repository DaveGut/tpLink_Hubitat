/*	Tapo Hub Trigger Contact Sensor Child Device
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

=================================================================================================*/
def type() {return "tapoHub-NewType" }
def driverVer() { return parent.driverVer() }

metadata {
	definition (name: type(), namespace: "davegut", author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/HubitatActive/master/TapoDevices/DeviceDrivers/${type()}.groovy") 
	{
		capability "Contact Sensor"
		attribute "reportInterval", "number"
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
}

def parseTriggerLog(resp, data) {
}

//	Library Inclusion



// ~~~~~ start include (1329) davegut.tapoSensorCommon ~~~~~
library ( // library marker davegut.tapoSensorCommon, line 1
	name: "tapoSensorCommon", // library marker davegut.tapoSensorCommon, line 2
	namespace: "davegut", // library marker davegut.tapoSensorCommon, line 3
	author: "Dave Gutheinz", // library marker davegut.tapoSensorCommon, line 4
	description: "Common Tapo Sensor Methods", // library marker davegut.tapoSensorCommon, line 5
	category: "utilities", // library marker davegut.tapoSensorCommon, line 6
	documentationLink: "" // library marker davegut.tapoSensorCommon, line 7
) // library marker davegut.tapoSensorCommon, line 8

def deviceRefreshParse(childData, data=null) { // library marker davegut.tapoSensorCommon, line 10
	try { // library marker davegut.tapoSensorCommon, line 11
		def devData = childData.find {it.mac = device.getDeviceNetworkId()} // library marker davegut.tapoSensorCommon, line 12
		logDebug("deviceInfoParse: ${devData}") // library marker davegut.tapoSensorCommon, line 13
		updateAttr("lowBattery", devData.atLowBattery) // library marker davegut.tapoSensorCommon, line 14
		updateAttr("status", status) // library marker davegut.tapoSensorCommon, line 15
	} catch (error) { // library marker davegut.tapoSensorCommon, line 16
		logWarn("deviceRefreshParse: Failed to capture deviceData from ChildData") // library marker davegut.tapoSensorCommon, line 17
	} // library marker davegut.tapoSensorCommon, line 18
} // library marker davegut.tapoSensorCommon, line 19

def getTriggerLog() { // library marker davegut.tapoSensorCommon, line 21
	Map cmdBody = [ // library marker davegut.tapoSensorCommon, line 22
		method: "control_child", // library marker davegut.tapoSensorCommon, line 23
		params: [ // library marker davegut.tapoSensorCommon, line 24
			device_id: getDataValue("deviceId"), // library marker davegut.tapoSensorCommon, line 25
			requestData: [ // library marker davegut.tapoSensorCommon, line 26
				method: "get_trigger_logs", // library marker davegut.tapoSensorCommon, line 27
				params: [page_size: 5,"start_id": 0] // library marker davegut.tapoSensorCommon, line 28
			] // library marker davegut.tapoSensorCommon, line 29
		] // library marker davegut.tapoSensorCommon, line 30
	] // library marker davegut.tapoSensorCommon, line 31
	parent.securePassthrough(cmdBody, true, device.getDeviceNetworkId(), "distTriggerLog") // library marker davegut.tapoSensorCommon, line 32
} // library marker davegut.tapoSensorCommon, line 33

command "getDeveloperData" // library marker davegut.tapoSensorCommon, line 35
def getDeveloperData() { // library marker davegut.tapoSensorCommon, line 36
	def attrData = device.getCurrentStates() // library marker davegut.tapoSensorCommon, line 37
	Map attrs = [:] // library marker davegut.tapoSensorCommon, line 38
	attrData.each { // library marker davegut.tapoSensorCommon, line 39
		attrs << ["${it.name}": it.value] // library marker davegut.tapoSensorCommon, line 40
	} // library marker davegut.tapoSensorCommon, line 41
	Date date = new Date() // library marker davegut.tapoSensorCommon, line 42
	Map devData = [ // library marker davegut.tapoSensorCommon, line 43
		currentTime: date.toString(), // library marker davegut.tapoSensorCommon, line 44
		name: device.getName(), // library marker davegut.tapoSensorCommon, line 45
		status: device.getStatus(), // library marker davegut.tapoSensorCommon, line 46
		dataValues: device.getData(), // library marker davegut.tapoSensorCommon, line 47
		attributes: attrs, // library marker davegut.tapoSensorCommon, line 48
		devInfo: getDeviceInfo(), // library marker davegut.tapoSensorCommon, line 49
		compList: getDeviceComponents() // library marker davegut.tapoSensorCommon, line 50
	] // library marker davegut.tapoSensorCommon, line 51
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.tapoSensorCommon, line 52
} // library marker davegut.tapoSensorCommon, line 53

def getDeviceComponents() { // library marker davegut.tapoSensorCommon, line 55
	Map logData = [:] // library marker davegut.tapoSensorCommon, line 56
	Map cmdBody = [ // library marker davegut.tapoSensorCommon, line 57
		device_id: getDataValue("deviceId"), // library marker davegut.tapoSensorCommon, line 58
		method: "get_child_device_component_list" // library marker davegut.tapoSensorCommon, line 59
	] // library marker davegut.tapoSensorCommon, line 60
	def compList = parent.securePassthrough(cmdBody, false) // library marker davegut.tapoSensorCommon, line 61
	if (compList == "ERROR") { // library marker davegut.tapoSensorCommon, line 62
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]") // library marker davegut.tapoSensorCommon, line 63
	} // library marker davegut.tapoSensorCommon, line 64
	return compList // library marker davegut.tapoSensorCommon, line 65
} // library marker davegut.tapoSensorCommon, line 66

def getDeviceInfo() { // library marker davegut.tapoSensorCommon, line 68
	logDebug("getChildDeviceInfo") // library marker davegut.tapoSensorCommon, line 69
	Map cmdBody = [ // library marker davegut.tapoSensorCommon, line 70
		method: "control_child", // library marker davegut.tapoSensorCommon, line 71
		params: [ // library marker davegut.tapoSensorCommon, line 72
			device_id: getDataValue("deviceId"), // library marker davegut.tapoSensorCommon, line 73
			requestData: [ // library marker davegut.tapoSensorCommon, line 74
				method: "get_device_info" // library marker davegut.tapoSensorCommon, line 75
			] // library marker davegut.tapoSensorCommon, line 76
		] // library marker davegut.tapoSensorCommon, line 77
	] // library marker davegut.tapoSensorCommon, line 78
	def devInfo = parent.securePassthrough(cmdBody, false) // library marker davegut.tapoSensorCommon, line 79
	if (devInfo == "ERROR") { // library marker davegut.tapoSensorCommon, line 80
		logWarn("getDeviceInfo: [ERROR: Error in Sysn Comms]") // library marker davegut.tapoSensorCommon, line 81
	} // library marker davegut.tapoSensorCommon, line 82
	return devInfo // library marker davegut.tapoSensorCommon, line 83
} // library marker davegut.tapoSensorCommon, line 84

def setReportInterval() { // library marker davegut.tapoSensorCommon, line 86
	def repInt = sensorReportInt.toInteger() // library marker davegut.tapoSensorCommon, line 87
	Map cmdBody = [ // library marker davegut.tapoSensorCommon, line 88
		method: "control_child", // library marker davegut.tapoSensorCommon, line 89
		params: [ // library marker davegut.tapoSensorCommon, line 90
			device_id: getDataValue("deviceId"), // library marker davegut.tapoSensorCommon, line 91
			requestData: [ // library marker davegut.tapoSensorCommon, line 92
				method: "multipleRequest", // library marker davegut.tapoSensorCommon, line 93
				params: [ // library marker davegut.tapoSensorCommon, line 94
					requests: [ // library marker davegut.tapoSensorCommon, line 95
						[method: "set_device_info", // library marker davegut.tapoSensorCommon, line 96
						 params: [report_interval: repInt]], // library marker davegut.tapoSensorCommon, line 97
						[method: "get_device_info"] // library marker davegut.tapoSensorCommon, line 98
					]]]]] // library marker davegut.tapoSensorCommon, line 99
	def devInfo = parent.securePassthrough(cmdBody, false) // library marker davegut.tapoSensorCommon, line 100
	devInfo = devInfo.result.responseData.result.responses.find{it.method == "get_device_info"}.result // library marker davegut.tapoSensorCommon, line 101
	updateAttr("reportInterval", devInfo.report_interval) // library marker davegut.tapoSensorCommon, line 102

	return buttonReportInt // library marker davegut.tapoSensorCommon, line 104
} // library marker davegut.tapoSensorCommon, line 105

// ~~~~~ end include (1329) davegut.tapoSensorCommon ~~~~~

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
