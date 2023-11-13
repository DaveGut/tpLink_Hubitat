/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Updates from previous version.
a.	Added Klap protocol with associated auto-use logic.
b.	Streamlined comms error processing and synched process to app find device.
c.	Added driver for Multi-plug to set.
d.	Added additional preferences (as appropriate) to child devices (sensors, multi-plug outlets)
e.	Added battery state attribute to sensors.
f.	Added setLed capability to preferences for plugs/switches/hub.
=================================================================================================*/

metadata {
	definition (name: "tpLink_hub_contact", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_contact.groovy")
	{
		capability "Contact Sensor"
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
	logData << [status: "OK"]
	if (developerData) { getDeveloperData() }
	if (logData.status == "ERROR") {
		logError("updated: ${logData}")
	} else {
		logInfo("updated: ${logData}")
	}
}

//	Parse Methods
def devicePollParse(childData, data=null) {
	childData = childData.find{ it.mac == device.getDeviceNetworkId() }
	def contact = "closed"
	if (childData.open) { contact = "open" }
	if (device.currentValue("contact") != contact) {
		updateAttr("contact", contact)
	}
	if (device.currentValue("lowBattery") != childData.atLowBattery.toString()) {
		updateAttr("lowBattery", childData.at_low_battery.toString())
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
capability "Refresh" // library marker davegut.lib_tpLink_sensors, line 9

def refresh() { // library marker davegut.lib_tpLink_sensors, line 11
	parent.refresh() // library marker davegut.lib_tpLink_sensors, line 12
} // library marker davegut.lib_tpLink_sensors, line 13

def getTriggerLog() { // library marker davegut.lib_tpLink_sensors, line 15
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 16
		method: "control_child", // library marker davegut.lib_tpLink_sensors, line 17
		params: [ // library marker davegut.lib_tpLink_sensors, line 18
			device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 19
			requestData: [ // library marker davegut.lib_tpLink_sensors, line 20
				method: "get_trigger_logs", // library marker davegut.lib_tpLink_sensors, line 21
				params: [page_size: 5,"start_id": 0] // library marker davegut.lib_tpLink_sensors, line 22
			] // library marker davegut.lib_tpLink_sensors, line 23
		] // library marker davegut.lib_tpLink_sensors, line 24
	] // library marker davegut.lib_tpLink_sensors, line 25
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "distTriggerLog") // library marker davegut.lib_tpLink_sensors, line 26
} // library marker davegut.lib_tpLink_sensors, line 27

def getDeveloperData() { // library marker davegut.lib_tpLink_sensors, line 29
	device.updateSetting("developerData",[type:"bool", value: false]) // library marker davegut.lib_tpLink_sensors, line 30
	def attrData = device.getCurrentStates() // library marker davegut.lib_tpLink_sensors, line 31
	Map attrs = [:] // library marker davegut.lib_tpLink_sensors, line 32
	attrData.each { // library marker davegut.lib_tpLink_sensors, line 33
		attrs << ["${it.name}": it.value] // library marker davegut.lib_tpLink_sensors, line 34
	} // library marker davegut.lib_tpLink_sensors, line 35
	Date date = new Date() // library marker davegut.lib_tpLink_sensors, line 36
	Map devData = [ // library marker davegut.lib_tpLink_sensors, line 37
		currentTime: date.toString(), // library marker davegut.lib_tpLink_sensors, line 38
		name: device.getName(), // library marker davegut.lib_tpLink_sensors, line 39
		status: device.getStatus(), // library marker davegut.lib_tpLink_sensors, line 40
		dataValues: device.getData(), // library marker davegut.lib_tpLink_sensors, line 41
		attributes: attrs, // library marker davegut.lib_tpLink_sensors, line 42
		devInfo: getChildDeviceInfo(), // library marker davegut.lib_tpLink_sensors, line 43
		compList: getDeviceComponents() // library marker davegut.lib_tpLink_sensors, line 44
	] // library marker davegut.lib_tpLink_sensors, line 45
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_sensors, line 46
} // library marker davegut.lib_tpLink_sensors, line 47

def getDeviceComponents() { // library marker davegut.lib_tpLink_sensors, line 49
	Map logData = [:] // library marker davegut.lib_tpLink_sensors, line 50
	Map cmdBody = [ // library marker davegut.lib_tpLink_sensors, line 51
		device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 52
		method: "get_child_device_component_list" // library marker davegut.lib_tpLink_sensors, line 53
	] // library marker davegut.lib_tpLink_sensors, line 54
	def compList = parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_sensors, line 55
	if (compList == "ERROR") { // library marker davegut.lib_tpLink_sensors, line 56
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]") // library marker davegut.lib_tpLink_sensors, line 57
	} // library marker davegut.lib_tpLink_sensors, line 58
	return compList // library marker davegut.lib_tpLink_sensors, line 59
} // library marker davegut.lib_tpLink_sensors, line 60

def getChildDeviceInfo() { // library marker davegut.lib_tpLink_sensors, line 62
	Map cmdBody = [method: "control_child", // library marker davegut.lib_tpLink_sensors, line 63
				   params: [device_id: getDataValue("deviceId"), // library marker davegut.lib_tpLink_sensors, line 64
							requestData: [method: "get_device_info"]]] // library marker davegut.lib_tpLink_sensors, line 65
	return parent.syncSend(cmdBody) // library marker davegut.lib_tpLink_sensors, line 66
} // library marker davegut.lib_tpLink_sensors, line 67

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_sensors, line 69
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_sensors, line 70
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_sensors, line 71
	} // library marker davegut.lib_tpLink_sensors, line 72
} // library marker davegut.lib_tpLink_sensors, line 73

/*	Future. // library marker davegut.lib_tpLink_sensors, line 75
attribute "lowBattery", "string" // library marker davegut.lib_tpLink_sensors, line 76
attribute "status", "string" // library marker davegut.lib_tpLink_sensors, line 77
def deviceRefreshParse(childData, data=null) { // library marker davegut.lib_tpLink_sensors, line 78
	try { // library marker davegut.lib_tpLink_sensors, line 79
		def devData = childData.find {it.mac = device.getDeviceNetworkId()} // library marker davegut.lib_tpLink_sensors, line 80
		updateAttr("lowBattery", devData.atLowBattery) // library marker davegut.lib_tpLink_sensors, line 81
		updateAttr("status", status) // library marker davegut.lib_tpLink_sensors, line 82
	} catch (error) { // library marker davegut.lib_tpLink_sensors, line 83
	} // library marker davegut.lib_tpLink_sensors, line 84
} // library marker davegut.lib_tpLink_sensors, line 85
*/ // library marker davegut.lib_tpLink_sensors, line 86

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

//	===== Common Data Elements ===== // library marker davegut.Logging, line 10
def nameSpace() { return "davegut" } // library marker davegut.Logging, line 11

def version() { return "2.3.7b" } // library marker davegut.Logging, line 13
def label() { // library marker davegut.Logging, line 14
	if (device) { return device.displayName }  // library marker davegut.Logging, line 15
	else { return app.getLabel() } // library marker davegut.Logging, line 16
} // library marker davegut.Logging, line 17

def listAttributes() { // library marker davegut.Logging, line 19
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 20
	Map attrs = [:] // library marker davegut.Logging, line 21
	attrData.each { // library marker davegut.Logging, line 22
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 23
	} // library marker davegut.Logging, line 24
	return attrs // library marker davegut.Logging, line 25
} // library marker davegut.Logging, line 26

def setLogsOff() { // library marker davegut.Logging, line 28
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 29
	if (logEnable) { // library marker davegut.Logging, line 30
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 31
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 32
	} // library marker davegut.Logging, line 33
	return logData // library marker davegut.Logging, line 34
} // library marker davegut.Logging, line 35

def logTrace(msg){ // library marker davegut.Logging, line 37
	log.trace "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 38
} // library marker davegut.Logging, line 39

def logInfo(msg) {  // library marker davegut.Logging, line 41
	if (infoLog) { // library marker davegut.Logging, line 42
		log.info "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 43
	} // library marker davegut.Logging, line 44
} // library marker davegut.Logging, line 45

def debugLogOff() { // library marker davegut.Logging, line 47
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 48
	logInfo("debugLogOff") // library marker davegut.Logging, line 49
} // library marker davegut.Logging, line 50

def logDebug(msg) { // library marker davegut.Logging, line 52
	if (logEnable) { // library marker davegut.Logging, line 53
		log.debug "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 54
	} // library marker davegut.Logging, line 55
} // library marker davegut.Logging, line 56

def logWarn(msg) { log.warn "${label()}-${version()}: ${msg}" } // library marker davegut.Logging, line 58

def logError(msg) { log.error "${label()}-${version()}: ${msg}" } // library marker davegut.Logging, line 60

// ~~~~~ end include (1339) davegut.Logging ~~~~~
