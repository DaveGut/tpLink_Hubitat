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
	definition (name: "tpLink_plug_multi_child", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_multi_child.groovy")
	{
		capability "Refresh"
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

def refresh() {
	Map logData = [method: "refresh"]
	try {
		def cmdResp = getChildDeviceInfo().result.responseData
		logData << [status: parseDevData(cmdResp.result)]
	} catch (err) {
		logData << [status: "ERROR", error: err]
	}
	logDebug(logData)
}

def syncName() {
	Map logData = [syncName: nameSync]
	if (nameSync == "none") {
		logData << [status: "Label Not Updated"]
	} else {
		def cmdResp
		String nickname
		if (nameSync == "Hubitat") {
			nickname = device.getLabel().bytes.encodeBase64().toString()
			List requests = [[method: "set_device_info", params: [nickname: nickname]]]
			requests << [method: "get_device_info"]
			Map cmdBody = [method: "control_child",
						   params: [device_id: getDataValue("deviceId"),
									requestData: createMultiCmd(requests)]]
			def multiResp = parent.syncSend(cmdBody)
			cmdResp = getDeviceInfoData(multiResp).result
		} else {
			cmdResp = getChildDeviceInfo().result.responseData.result
		}
		nickname = cmdResp.nickname
		byte[] plainBytes = nickname.decodeBase64()
		String label = new String(plainBytes)
		device.setLabel(label)
		logData << [nickname: nickname, label: label, status: "Label Updated"]
	}
	device.updateSetting("nameSync", [type: "enum", value: "none"])
	return logData
}

def setDefaultState() {
	Map logData = [defState: defState]
	def type = "last_states"
	def state = []
	if (defState == "on") {
		type = "custom"
		state = [on: true]
	} else if (defState == "off") {
		type = "custom"
		state = [on: false]
	}
	List requests = [[method: "set_device_info", 
					  params: [default_states:[type: type, state: state]]]]
	requests << [method: "get_device_info"]
	Map cmdBody = [method: "control_child",
				   params: [device_id: getDataValue("deviceId"),
							requestData: createMultiCmd(requests)]]
	def cmdResp = parent.syncSend(cmdBody)
	cmdResp = getDeviceInfoData(cmdResp).result
	def defaultStates = cmdResp.default_states
	def newState = "lastState"
	if (defaultStates.type == "custom"){
		newState = "off"
		if (defaultStates.state.on == true) {
			newState = "on"
		}
	}
	device.updateSetting("defState", [type: "enum", value: newState])
	if (newState == defState) {
		logData << [status: "OK"]
	} else {
		logData << [status: "FAILED"]
	}
	return logData
}

def createMultiCmd(requests) {
	Map cmdBody = [
		method: "multipleRequest",
		params: [requests: requests]]
	return cmdBody
}

def getDeviceInfoData(cmdResp) {
	def responses = cmdResp.result.responseData.result.responses
	Map devInfo = responses.find { it.method == "get_device_info" }
	return devInfo
}	
	
def getDeveloperData() {
	def attrData = device.getCurrentStates()
	Map attrs = [:]
	attrData.each {
		attrs << ["${it.name}": it.value]
	}
	Date date = new Date()
	Map devData = [
		currentTime: date.toString(),
		name: device.getName(),
		status: device.getStatus(),
		dataValues: device.getData(),
		attributes: attrs,
		devInfo: getChildDeviceInfo(),
		compList: getDeviceComponents()
	]
	logWarn("DEVELOPER DATA: ${devData}")
}

def getDeviceComponents() {
	device.updateSetting("developerData",[type:"bool", value: false])
	Map logData = [:]
	Map cmdBody = [
		device_id: getDataValue("deviceId"),
		method: "get_child_device_component_list"
	]
	def compList = parent.syncSend(cmdBody)
	if (compList == "ERROR") {
		logWarn("getDeviceComponents: [ERROR: Error in Sysn Comms]")
	}
	return compList
}

def getChildDeviceInfo() {
	Map cmdBody = [method: "control_child",
				   params: [device_id: getDataValue("deviceId"),
							requestData: [method: "get_device_info"]]]
	return parent.syncSend(cmdBody)
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}



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
