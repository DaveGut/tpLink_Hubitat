/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

=================================================================================================*/
//	=====	NAMESPACE	in library davegut.Logging	============
//import org.json.JSONObject
//import groovy.json.JsonSlurper
//import groovy.json.JsonOutput
//import groovy.json.JsonBuilder

metadata {
	definition (name: "TpLink Robovac", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_robovac.groovy")
	{
		capability "Refresh"
		capability "Configuration"
		attribute "commsError", "string"
		capability "Battery"
		capability "Actuator"
		command "setCleanPrefs", [
			[name: "cleanPasses", type: "ENUM", description: "Number of Vacuum Passes",
			 constraints: [1, 2, 3]],
			[name: "vacSuction", type: "ENUM", description: "Vacuum Suction", 
			 constraints: ["quiet", "standard", "turbo", "max"]],
			[name: "waterLevel", type: "ENUM", description: "Vacuum Suction", 
			 constraints: ["none", "low", "moderate", "high"]]]
		attribute "cleanPasses", "number"
		attribute "vacuumSuction", "string"
		attribute "waterLevel", "string"
		command "cleanStart"
		command "cleanPause"
		command "cleanResume"
		command "dockVacuum"
		attribute "docking", "string"
		attribute "cleanOn", "string"
		attribute "vacuumStatus", "string"
		attribute "prompt", "string"
		attribute "promptCode", "promptCode"
		attribute "mopState", "string"
		attribute "waterLevel", "number"
	}
	preferences {
		List pollOptions = ["5 sec", "10 sec", "30 sec", "5 min", "10 min", "15 min", "30 min"]
		input ("pollInterval", "enum", title: "Poll/Refresh Interval",
			   options: pollOptions, defaultValue: "30 min")
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() {
	updateAttr("commsError", "false")
	state.errorCount = 0
	state.lastCmd = ""
	state.eventType = "digital"
	Map logData = [configure: configure(false)]
	return logData
}

def configure(checkApp = true) {
	Map logData = [method: "configure"]
	if (checkApp == true) {
		logData << [updateData: parent.tpLinkCheckForDevices(5)]
	}
	unschedule()
	logData << [handshake: deviceHandshake()]
	runEvery3Hours(deviceHandshake)
	logData << [handshakeInterval: "3 Hours"]
	logData << [pollInterval: setPollInterval()]
	logData << [logging: setLogsOff()]
//	runIn(10, initSettings)
	logInfo(logData)
	return logData
}

def xxinitSettings() {
	Map logData = [method: "initSettings"]
	List requests = []
	if (ledRule) { requests << [method: "get_led_info"] }
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "initSettings", "parseUpdates")
	return logData
}

def updated() {
	Map logData = [method: updated]
	def commsErr = device.currentValue("commsError")
	logData << [commsError: commsErr]
	if (commsErr == "true") {
		logData << [configure: configure(true)]
	}
	updateAttr("commsError", "false")
	state.lastCmd = ""
	logData << [pollInterval: setPollInterval()]
	logData << [logging: setLogsOff()]
	runIn(3, refesh)
	logInfo(logData)
}

//	===== Cleaning Control =====
def cleanStart() {
	logDebug([method: "cleanStart"])
	def cmdBody = [method: "setSwitchClean", params: [clean_on: true, clean_mode: 0]]
	asyncSend(cmdBody,"cleanStart", "controlParse")
}

def cleanPause() {
	logDebug([method: "cleanPause"])
	def cmdBody = [method: "setRobotPause", params: [pause: true]]
	asyncSend(cmdBody,"cleanPause", "controlParse")
}

def cleanResume() {
	logDebug([method: "cleanResume"])
	def cmdBody = [method: "setRobotPause", params: [pause: false]]
	asyncSend(cmdBody,"dcleanResume", "controlParse")
}

def dockVacuum() {
	logDebug([method: "dockVacuum"])
	def cmdBody = [method: "setSwitchCharge", params: [switch_charge: true]]
	asyncSend(cmdBody,"dockVacuum", "controlParse")
}

def controlParse(resp, data=null) {
	Map logData = [method: "controlParse", control: data]
	try {
		def respData = parseData(resp)
		logData << [respData: respData]
		if(respData.cmdResp != "ERROR" && respData.cmdResp.error_code == 0) {
			runIn(8, getCleanData)
			logDebug(logData)
		} else {
			logData << [resp: resp.properties]
			logWarn(logData)
		}
	} catch (err) {
		logData << [errorData: err]
		logWarn(logData)
	}
}

def getCleanData() {
	logDebug([method: "getCleanData"])
	List requests = [
		[method: "getSwitchClean"],
		[method: "getVacStatus"],
		[method: "getMopState"],
		[method: "getSwitchCharge"]]
	asyncSend(createMultiCmd(requests),"getCleanData", "parseUpdates")
}

def parse_getVacStatus(vacData) {
	Map logData = [method: "parse_getVacStatus"]
	String vacuumStatus
	switch (vacData.status) {
		case 0: vacuumStatus = "OffDock/notCleaning"; break
		case 1: vacuumStatus = "cleaning"; break
		case 2: vacuumStatus = "2"; break
		case 3: vacuumStatus = "3"; break
		case 4: vacuumStatus = "docking"; break
		case 5: vacuumStatus = "docked/charging"; break
		case 6: vacuumStatus = "docked/charged"; break
		case 7: vacuumStatus = "paused"; break
		default: vacuumStatus = "${vacData.status}"
	}
	updateAttr("vacuumStatus", vacuumStatus)
	updateAttr("prompt", vacData.prompt)
	updateAttr("promptCode", vacData.promptCode_id)
	logData << [vacuumStatus: vacuumStatus, cleanOn: cleanOn, docking: docking,
			   mopState: mopState]
	if (vacData.status != 6 && vacData.status != 5) {
		runIn(60, getCleanData)
	}
	logDebug(logData)
}

//	==== Clean Preferences ====
def setCleanPrefs(passes, suction, water) {
	def logData = [method: "setCleanPrefs", passes: passes, suction: suction, waterLevel: water]
	Integer sucNo
	switch(suction) {
		case "standard": sucNo = 2; break
		case "turbo": sucNo = 3; break
		case "max": sucNo = 4; break
		default: sucNo = 1
	}
	Integer watLev
	switch(water) {
		case "low": watLev = 2; break
		case "moderate": watLev = 3; break
		case "high": watLev = 4; break
		default: watLev = 1
	}
	List requests = [
		[method:"setCleanNumber", params:[suction_level: sucNo, 
										  clean_number: passes.toInteger(), 
										  cistern: watLev]],
		[method: "getCleanNumber"]]
	asyncSend(createMultiCmd(requests), "setCleanPrefs", "parseUpdates")
	logDebug(logData)
}

def parse_getCleanNumber(result) {
	logDebug([method: "parse_getCleanNumber", result: result])
	updateAttr("cleanPasses", result.clean_number)
	String vacuumSuction
	switch(result.suction) {
		case 2: vacuumSuction = "standard"; break
		case 3: vacuumSuction = "turbo"; break
		case 4: vacuumSuction = "max"; break
		default: vacuumSuction = "quiet"
	}
	updateAttr("vacuumSuction", vacuumSuction)
	String waterLevel
	switch(result.cistern) {
		case 2: waterLevel = "low"; break
		case 3: waterLevel = "moderate"; break
		case 4: waterLevel = "high"; break
		default: waterLevel = "none"
	}
	updateAttr("waterLevel", waterLevel)
}

//	===== Refresh =====
def refresh() {
	getCleanData()
	List requests = [
		[method: "getBatteryInfo"],
		[method: "getCleanNumber"]	]
	asyncSend(createMultiCmd(requests), "refresh", "parseUpdates")
}

//	===== Login =====
def deviceHandshake() { 
	Map reqData = [:]
	Map cmdBody = [method: "login",
				   params: [hashed: true, 
							password: parent.encPasswordVac,
							username: parent.userName]]
	Map reqParams = [uri: getDataValue("baseUrl"),
					 ignoreSSLIssues: true,
					 body: cmdBody,
					 contentType: "application/json",
					 requestContentType: "application/json",
					 timeout: 10]
	asyncPost(reqParams, "parseVacAesLogin", reqData)
}

def parseVacAesLogin(resp, data) {
	Map logData = [method: "parseVacAesLogin", oldToken: token]
	if (resp.status == 200 && resp.json != null) {
		logData << [status: "OK"]
		def newToken = resp.json.result.token
		device.updateSetting("token", [type: "string", value: newToken])
		logData << [token: newToken]
//		state.errorCount = 0
//		if (device && device.currentValue("commsError") == "true") {
			setCommsError(false)
//		}
		logDebug(logData)
	} else {
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties]
		logWarn(logData)
	}
}

//	===== Communications =====
def getVacAesParams(cmdBody) {
	Map reqParams = [uri: "${getDataValue("baseUrl")}/?token=${token}",
					 body: cmdBody,
					 contentType: "application/json",
					 requestContentType: "application/json",
					 ignoreSSLIssues: true,
					 timeout: 10]
	return reqParams	
}

def parseVacAesData(resp) {
	Map parseData = [parseMethod: "parseVacAesData"]
	try {
		parseData << [status: "OK", cmdResp: resp.json]
//		state.errorCount = 0
//		if (device.currentValue("commsError") == "true") {
//			setCommsError(false)
//		}
		logDebug(parseData)
	} catch (err) {
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()]
		logWarn(parseData)
		handleCommsError()
	}
	return parseData
}

//	===== Capability Configuration =====
def createMultiCmd(requests) {
	Map cmdBody = [
		method: "multipleRequest",
		params: [requests: requests]]
	return cmdBody
}

def setPollInterval(pInterval = pollInterval) {
	if (pInterval.contains("sec")) {
		def interval = pInterval.replace(" sec", "").toInteger()
		def start = Math.round((interval-1) * Math.random()).toInteger()
		schedule("${start}/${interval} * * * * ?", "refresh")
		logWarn("setPollInterval: Polling intervals of less than one minute " +
				"can take high resources and may impact hub performance.")
	} else {
		def interval= pInterval.replace(" min", "").toInteger()
		def start = Math.round(59 * Math.random()).toInteger()
		schedule("${start} */${interval} * * * ?", "refresh")
	}
	return pInterval
}

//	===== Preferences Methods =====
def parseUpdates(resp, data= null) {
	Map logData = [method: "parseUpdates"]
	def respData = parseData(resp).cmdResp
	if (respData.error_code == 0) {
		respData.result.responses.each {
			if (it != null && it.error_code == 0) {
				switchParse(it)
			} else {
				logData << ["${it.method}": [status: "cmdFailed", data: it]]
				logWarn(logData)				
			}
		}
	} else {
		logData << [status: "invalidRequest", resp: resp.properties, respData: respData]
		logDebug(logData)				
	}
}
	
def switchParse(devResp) {
	Map logData = [method: switchParse]
	def doLog = true
	def prefs = getDataValue("prefs")
	switch(devResp.method) {
		case "get_device_info":
			logData << [deviceMethod: devResp.method]
log.trace devResp.result
//			parse_get_device_info(devResp.result, "switchParse")
			break
		case "get_led_info":
			logData << [deviceMethod: devResp.method]
log.trace devResp.result
			if (ledRule != devResp.result.led_rule) {
				Map requests = [
					method: "set_led_info",
					params: [
						led_rule: ledRule,
						//	Uses mode data from device.  This driver does not update these.
						night_mode: [
							night_mode_type: devResp.result.night_mode.night_mode_type,
							sunrise_offset: devResp.result.night_mode.sunrise_offset, 
							sunset_offset:devResp.result.night_mode.sunset_offset,
							start_time: devResp.result.night_mode.start_time,
							end_time: devResp.result.night_mode.end_time
						]]]
				asyncSend(requests, "delayedUpdates", "parseUpdates")
				device.updateSetting("ledRule", [type:"enum", value: ledRule])
				logData << [status: "updatingLedRule"]
			}
			logData << [ledRule: ledRule]
			break
		case "get_battery_info":
			updateAttr("battery", devResp.result.battery_percentage)
			break
		case "getCleanNumber":
			parse_getCleanNumber(devResp.result)
			break
		case "getSwitchClean":
			updateAttr("cleanOn", devResp.clean_on)
			break
		case "getMopState":
			updateAttr("mopState", devResp.mop_state)
			break
		case "getSwitchCharge":
			updateAttr("docking", devResp.switch_charge)
			break
		case "getVacStatus":
			parse_getVacStatus(devResp.result)
			break
		case "set_led_info":
		case "set_device_info":
		case "set_auto_off_config":
		case "set_on_off_gradually_info":
			doLog = false
			break
		default:
			logData << [status: "unhandled", devResp: devResp]
	}
	if (doLog) { logDebug(logData) }
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}




// ~~~~~ start include (86) davegut.tpLinkComms ~~~~~
library ( // library marker davegut.tpLinkComms, line 1
	name: "tpLinkComms", // library marker davegut.tpLinkComms, line 2
	namespace: "davegut", // library marker davegut.tpLinkComms, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkComms, line 4
	description: "Communication methods for TP-Link Integration", // library marker davegut.tpLinkComms, line 5
	category: "utilities", // library marker davegut.tpLinkComms, line 6
	documentationLink: "" // library marker davegut.tpLinkComms, line 7
) // library marker davegut.tpLinkComms, line 8
import org.json.JSONObject // library marker davegut.tpLinkComms, line 9
import groovy.json.JsonOutput // library marker davegut.tpLinkComms, line 10
import groovy.json.JsonBuilder // library marker davegut.tpLinkComms, line 11
import groovy.json.JsonSlurper // library marker davegut.tpLinkComms, line 12

//	===== Async Commsunications Methods ===== // library marker davegut.tpLinkComms, line 14
def asyncSend(cmdBody, reqData, action) { // library marker davegut.tpLinkComms, line 15
	Map cmdData = [cmdBody: cmdBody, reqData: reqData, action: action] // library marker davegut.tpLinkComms, line 16
	state.lastCmd = cmdData // library marker davegut.tpLinkComms, line 17
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkComms, line 18
	Map reqParams = [:] // library marker davegut.tpLinkComms, line 19
	if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 20
		reqParams = getKlapParams(cmdBody) // library marker davegut.tpLinkComms, line 21
	} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 22
		reqParams = getAesParams(cmdBody) // library marker davegut.tpLinkComms, line 23
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 24
		reqParams = getVacAesParams(cmdBody) // library marker davegut.tpLinkComms, line 25
	} // library marker davegut.tpLinkComms, line 26
	asyncPost(reqParams, action, reqData) // library marker davegut.tpLinkComms, line 27
} // library marker davegut.tpLinkComms, line 28

def asyncPost(reqParams, parseMethod, reqData=null) { // library marker davegut.tpLinkComms, line 30
	Map logData = [method: "asyncPost", parseMethod: parseMethod, data:reqData] // library marker davegut.tpLinkComms, line 31
	try { // library marker davegut.tpLinkComms, line 32
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.tpLinkComms, line 33
		logData << [status: "OK"] // library marker davegut.tpLinkComms, line 34
	} catch (err) { // library marker davegut.tpLinkComms, line 35
		logData << [status: "FAILED", reqParams: reqParams, error: err] // library marker davegut.tpLinkComms, line 36
	} // library marker davegut.tpLinkComms, line 37
	logDebug(logData) // library marker davegut.tpLinkComms, line 38
} // library marker davegut.tpLinkComms, line 39

def parseData(resp, protocol = getDataValue("protocol")) { // library marker davegut.tpLinkComms, line 41
	Map logData = [method: "parseData"] // library marker davegut.tpLinkComms, line 42
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 43
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 44
			logData << parseKlapData(resp) // library marker davegut.tpLinkComms, line 45
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 46
			logData << parseAesData(resp) // library marker davegut.tpLinkComms, line 47
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 48
			logData << parseVacAesData(resp) // library marker davegut.tpLinkComms, line 49
		} // library marker davegut.tpLinkComms, line 50
		if (logData.status == "OK") { // library marker davegut.tpLinkComms, line 51
			setCommsError(false) // library marker davegut.tpLinkComms, line 52
		} else { // library marker davegut.tpLinkComms, line 53
			handleCommsError() // library marker davegut.tpLinkComms, line 54
		} // library marker davegut.tpLinkComms, line 55
	} else { // library marker davegut.tpLinkComms, line 56
		logData << [status: "httpFailure"] // library marker davegut.tpLinkComms, line 57
		handleCommsError() // library marker davegut.tpLinkComms, line 58
	} // library marker davegut.tpLinkComms, line 59
	return logData // library marker davegut.tpLinkComms, line 60
} // library marker davegut.tpLinkComms, line 61

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 63
def handleCommsError() { // library marker davegut.tpLinkComms, line 64
	Map logData = [method: "handleCommsError"] // library marker davegut.tpLinkComms, line 65
	if (state.lastCmd != "") { // library marker davegut.tpLinkComms, line 66
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 67
		logData << [count: count, lastCmd: state.lastCmd] // library marker davegut.tpLinkComms, line 68
		switch (count) { // library marker davegut.tpLinkComms, line 69
			case 1: // library marker davegut.tpLinkComms, line 70
				logData << [action: "resendCommand"] // library marker davegut.tpLinkComms, line 71
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 72
				break // library marker davegut.tpLinkComms, line 73
			case 2: // library marker davegut.tpLinkComms, line 74
				logData << [attemptHandshake: deviceHandshake(), // library marker davegut.tpLinkComms, line 75
						    action: "deviceHandshake"] // library marker davegut.tpLinkComms, line 76
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 77
				break // library marker davegut.tpLinkComms, line 78
			case 3: // library marker davegut.tpLinkComms, line 79
				logData << [configure: configure(true), // library marker davegut.tpLinkComms, line 80
						    action: "configure"] // library marker davegut.tpLinkComms, line 81
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 82
				break // library marker davegut.tpLinkComms, line 83
			default: // library marker davegut.tpLinkComms, line 84
				setCommsError(true) // library marker davegut.tpLinkComms, line 85
				break // library marker davegut.tpLinkComms, line 86
		} // library marker davegut.tpLinkComms, line 87
		state.errorCount = count // library marker davegut.tpLinkComms, line 88
	} else { // library marker davegut.tpLinkComms, line 89
		logData << [status: "noCommandToRetry"] // library marker davegut.tpLinkComms, line 90
	} // library marker davegut.tpLinkComms, line 91
	logDebug(logData) // library marker davegut.tpLinkComms, line 92
logInfo(logData) // library marker davegut.tpLinkComms, line 93
} // library marker davegut.tpLinkComms, line 94

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 96
	def cmdData = new JSONObject(state.lastCmd) // library marker davegut.tpLinkComms, line 97
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 98
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 99
} // library marker davegut.tpLinkComms, line 100

def setCommsError(status) { // library marker davegut.tpLinkComms, line 102
	if (device && status == false) { // library marker davegut.tpLinkComms, line 103
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 104
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 105
			updateAttr("commsError", "false") // library marker davegut.tpLinkComms, line 106
			setPollInterval() // library marker davegut.tpLinkComms, line 107
			unschedule(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 108
			logInfo([method: "setCommsError", action: "setFalse"]) // library marker davegut.tpLinkComms, line 109
		} // library marker davegut.tpLinkComms, line 110
	} else if (device && status == true) { // library marker davegut.tpLinkComms, line 111
		if (device.currentValue("commsError") == "false") { // library marker davegut.tpLinkComms, line 112
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 113
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 114
			runEvery10Minutes(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 115
			logWarn([method: "setCommsError", errorCount: state.errorCount, action: "setTrue"]) // library marker davegut.tpLinkComms, line 116
		} else { // library marker davegut.tpLinkComms, line 117
			logWarn([method: "setCommsError", errorCount: state.errorCount]) // library marker davegut.tpLinkComms, line 118
		} // library marker davegut.tpLinkComms, line 119
	} // library marker davegut.tpLinkComms, line 120
} // library marker davegut.tpLinkComms, line 121

def errorDeviceHandshake() {  // library marker davegut.tpLinkComms, line 123
	logDebug([method: "errorDeviceHandshake"]) // library marker davegut.tpLinkComms, line 124
	deviceHandshake() // library marker davegut.tpLinkComms, line 125
} // library marker davegut.tpLinkComms, line 126

// ~~~~~ end include (86) davegut.tpLinkComms ~~~~~

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
