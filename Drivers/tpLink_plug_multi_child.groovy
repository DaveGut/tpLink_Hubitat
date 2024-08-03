/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

=================================================================================================*/
//	=====	NAMESPACE	============
def nameSpace() { return "davegut" }
//	================================

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
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { runIn(5, updated) }

def updated() {
	unschedule()
	Map logData = [method: "updated"]
	setDevPreferences()
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
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "childRespDist")
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
	logDebug(logData)
}

def refresh() { parent.refresh() }

//	===== Device Preferences =====
def setDevPreferences() {
	Map logData = [method: "setDevPreferences"]
	List requests = []
	logData << [defState: defState]
	def type = "last_states"
	def state = []
	if (defState == "on") {
		type = "custom"
		state = [on: true]
	} else if (defState == "off") {
		type = "custom"
		state = [on: false]
	}
	logData << [nameSync: nameSync]
	if (nameSync == "Hubitat") {
		def nickname = device.getLabel().bytes.encodeBase64().toString()
		requests << [method: "set_device_info",
					 params: [
						 default_states: [type: type, state: state],
						 nickname: nickname]]
	} else {
		requests << [method: "set_device_info",
					 params: [
						 default_states: [type: type, state: state]]]
	}
	requests << [method: "get_device_info"]
	Map cmdBody = [method: "control_child",
				   params: [device_id: getDataValue("deviceId"),
							requestData: createMultiCmd(requests)]]
	parent.asyncSend(cmdBody, device.getDeviceNetworkId(), "childSettingsDist")
	logInfo(logData)
}

def childSettingsParse(devData) {
	Map logData = [method: "childSettingsParse"]
	def defaultStates = devData.default_states
	def newState = "lastState"
	if (defaultStates.type == "custom"){
		newState = "off"
		if (defaultStates.state.on == true) {
			newState = "on"
		}
	}
	device.updateSetting("defState", [type: "enum", value: newState])
	logData << [defState: newState]

	if (nameSync == "device") {
		byte[] plainBytes = devData.nickname.decodeBase64()
		String label = new String(plainBytes)
		device.setLabel(label)
		logData << [nickname: devData.nickname, label: label]
	}
	device.updateSetting("nameSync", [type: "enum", value: "none"])
	logInfo(logData)
	parseDevData(devData)
}

def createMultiCmd(requests) {
	Map cmdBody = [
		method: "multipleRequest",
		params: [requests: requests]]
	return cmdBody
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}



// ~~~~~ start include (15) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8
//	Updated for Kasa // library marker davegut.Logging, line 9
def label() { // library marker davegut.Logging, line 10
	if (device) { return device.displayName }  // library marker davegut.Logging, line 11
	else { return app.getLabel() } // library marker davegut.Logging, line 12
} // library marker davegut.Logging, line 13

def listAttributes() { // library marker davegut.Logging, line 15
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 16
	Map attrs = [:] // library marker davegut.Logging, line 17
	attrData.each { // library marker davegut.Logging, line 18
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 19
	} // library marker davegut.Logging, line 20
	return attrs // library marker davegut.Logging, line 21
} // library marker davegut.Logging, line 22

def setLogsOff() { // library marker davegut.Logging, line 24
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 25
	if (logEnable) { // library marker davegut.Logging, line 26
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 27
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 28
	} // library marker davegut.Logging, line 29
	return logData // library marker davegut.Logging, line 30
} // library marker davegut.Logging, line 31

def logTrace(msg){ log.trace "${label()}: ${msg}" } // library marker davegut.Logging, line 33

def logInfo(msg) {  // library marker davegut.Logging, line 35
	if (infoLog) { log.info "${label()}: ${msg}" } // library marker davegut.Logging, line 36
} // library marker davegut.Logging, line 37

def debugLogOff() { // library marker davegut.Logging, line 39
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 40
	logInfo("debugLogOff") // library marker davegut.Logging, line 41
} // library marker davegut.Logging, line 42

def logDebug(msg) { // library marker davegut.Logging, line 44
	if (logEnable) { log.debug "${label()}: ${msg}" } // library marker davegut.Logging, line 45
} // library marker davegut.Logging, line 46

def logWarn(msg) { log.warn "${label()}: ${msg}" } // library marker davegut.Logging, line 48

def logError(msg) { log.error "${label()}: ${msg}" } // library marker davegut.Logging, line 50

// ~~~~~ end include (15) davegut.Logging ~~~~~
