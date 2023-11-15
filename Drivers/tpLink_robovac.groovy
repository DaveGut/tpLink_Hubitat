/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Initial Version.  Contains all major functions except clean specific area (RV30 model)
=================================================================================================*/
import org.json.JSONObject
import groovy.json.JsonSlurper

metadata {
	definition (name: "tpLink_robovac", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_robovac.groovy")
	{
		capability "Refresh"
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
		attribute "childLock", "string"
		attribute "waterLevel", "number"
		attribute "connected", "string"
		attribute "commsError", "string"
	}
	preferences {
		input ("nameSync", "enum", title: "Synchronize Names",
				   options: ["none": "Don't synchronize",
						 "device" : "TP-Link device name master",
						 "Hubitat" : "Hubitat label master"],
			   defaultValue: "none")
		input ("developerData", "bool", title: "Get Data for Developer", defaultValue: false)
		input ("rebootDev", "bool", title: "Reboot Device then run Save Preferences", defaultValue: false)
		input ("childLock", "bool", title: "Enable Child Lock")
		input ("carpetClean", "enum", title: "Carpet Clean", options: ["normal", "boost"])
		input ("areaUnit", "enum", title: "Area Unit", options: ["meters", "feet"])
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging", defaultValue: true)
		input ("token", "string", title: "AES token. Do not edit.")
	}
}

def installed() {
	runIn(5, updated)
}

def updated() {
	unschedule()
	Map logData = [method: "updated"]
	if (rebootDev == true) {
		runInMillis(50, rebootDevice)
		device.updateSetting("rebootDev",[type:"bool", value: false])
		pauseExecution(20000)
	}
	logData << [deviceLogin: deviceLogin()]
	runEvery15Minutes(refresh)
	logData << [refresh: "15 minutes", debugLog: logEnable, infoLog: infoLog]
	if (logEnable) { runIn(1800, debugLogOff) }
	logData << [loginInterval: setLoginInterval()]
	state.errorCount = 0
	state.lastCmd = ""
	updateAttr("commsError", "false")
	runIn(5, delayedUpdates)
	logInfo(logData)
}

def delayedUpdates() {
	Map logData = [method: "delayedUpdates", syncName: syncName()]
	logData << [settings: setSettings()]
	if (developerData) { runIn(4, getDeveloperData) }
	refresh()
	logInfo logData
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
		if(respData != "ERROR" && respData.error_code == 0) {
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
	asyncSend(createMultiCmd(requests),"getCleanData", "cleanDataParse")
}

def cleanDataParse(resp, data=null) {
	Map logData = [method: "cleanDataParse", callMethod: data]
	try {
		def respData = parseData(resp)
		logData << [respData: respData]
		if(respData != "ERROR" && respData.error_code == 0) {
			def cmdData = respData.result.responses
			def docking = cmdData.find {it.method == "getSwitchCharge"}.result.switch_charge
			updateAttr("docking", docking)
			def cleanOn = cmdData.find {it.method == "getSwitchClean"}.result.clean_on
			updateAttr("cleanOn", cleanOn)
			def mopState = cmdData.find {it.method == "getMopState"}.result.mop_state
			updateAttr("mopState", mopState)
			def vacData = cmdData.find {it.method == "getVacStatus"}.result
			String vacuumStatus
			switch (vacData.status) {
				case 0: vacuumStatus = "OffDock/notCleaning"; break
				case 1: vacuumStaus = "cleaning"; break
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
			updateAttr("vacuumStatus", vacuumStatus)
			logData << [vacuumStatus: vacuumStatus, cleanOn: cleanOn, docking: docking,
					   mopState: mopState]
			if (vacData.status != 6 && vacData.status != 5) {
				runIn(60, getCleanData)
			}
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
	asyncSend(createMultiCmd(requests), "setCleanPrefs", "parseCleanPrefs")
	logDebug(logData)
}

def parseCleanPrefs(resp, data = null) {
		Map logData = [method: "parseCleanPrefs"]
	try {
		Map respData = parseData(resp)
		logData << [respData: respData]
		if(respData != "ERROR" && respData.error_code == 0) {
			def cmdData = respData.result.responses
			def cleanData = cmdData.find {it.method == "getCleanNumber"}.result
			updateAttr("cleanPasses", cleanData.clean_number)
			String vacuumSuction
			switch(cleanData.suction) {
				case 2: vacuumSuction = "standard"; break
				case 3: vacuumSuction = "turbo"; break
				case 4: vacuumSuction = "max"; break
			default: vacuumSuction = "quiet"
			}
			updateAttr("vacuumSuction", vacuumSuction)
			String waterLevel
			switch(cleanData.cistern) {
				case 2: waterLevel = "low"; break
				case 3: waterLevel = "moderate"; break
				case 4: waterLevel = "high"; break
				default: waterLevel = "none"
			}
			updateAttr("waterLevel", waterLevel)
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

//	===== Refresh =====
def refresh() {
	getCleanData()
	List requests = [
		[method: "getBatteryInfo"],
		[method: "getCleanNumber"]	]
	asyncSend(createMultiCmd(requests), "refresh", "refreshParse")
}

def refreshParse(resp, data=null) {
	Map logData = [method: "refreshParse"]
	try {
		def respData = parseData(resp)
		logData << [respData: respData]
		if(respData != "ERROR" && respData.error_code == 0) {
			def cmdData = respData.result.responses
			def batteryInfo = cmdData.find {it.method == "getBatteryInfo"}.result
			updateAttr("battery", batteryInfo.battery_percentage)
			logData << [battery: batteryInfo.battery_percentage]
			logDebug(logData)
		} else {
			logData << [resp: resp.properties]
			logWarn(logData)
		}
	} catch (err) {
		logData << [errorData: err]
		logWarn(logData)
	}
	parseCleanPrefs(resp, data)
}

//	===== Login =====
def deviceLogin() {
	Map logData = [method: "deviceLogin", oldToken: token]
	def uri = getDataValue("baseUrl")
	Map cmdBody = [method: "login",
				   params: [hashed: true, 
							password: parent.encPasswordVac,
							username: parent.userName]]
	def loginResp = aesSyncPost(uri, cmdBody)
	if (loginResp.status == 200) {
		logData << [status: "OK"]
		def newToken = loginResp.data.result.token
		device.updateSetting("token", [type: "string", value: newToken])
		logData << [token: newToken]
	} else {
		logData << [status: "FAILED", token: "notUpdated", reason: "HTTP Response Status"]
	}
	logDebug(logData)
	return logData
}

//	==== Device Preferences =====		   
def syncName() {
	def logData = [syncName: nameSync]
	if (nameSync == "none") {
		logData << [status: "Label Not Updated"]
	} else {
		def cmdResp
		String nickname
		if (nameSync == "device") {
			cmdResp = syncSend([method: "get_device_info"])
			nickname = cmdResp.result.nickname
		} else if (nameSync == "Hubitat") {
			nickname = device.getLabel().bytes.encodeBase64().toString()
			List requests = [[method: "set_device_info",params: [nickname: nickname]]]
			requests << [method: "get_device_info"]
			cmdResp = syncSend(createMultiCmd(requests))
			cmdResp = cmdResp.result.responses.find { it.method == "get_device_info" }
			nickname = cmdResp.result.nickname
		}
		byte[] plainBytes = nickname.decodeBase64()
		String label = new String(plainBytes)
		device.setLabel(label)
		logData << [nickname: nickname, label: label, status: "Label Updated"]
	}
	device.updateSetting("nameSync", [type: "enum", value: "none"])
	return logData
}

def rebootDevice() {
	logWarn("rebootDevice: Rebooting device per preference request")
	def result = syncSend([method: "device_reboot"])
	logWarn("rebootDevice: ${result}")
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}

def setSettings() {
	Integer areaUnitNo = 0
	if (areaUnit == "feet") { areaUnitNo = 1 }
	List requests = [
		[method: "setCarpetClean", params:[carpet_clean_prefer: carpetClean]],
		[method: "setChildLockInfo", params:[child_lock_status: childLock]],
		[method: "setAreaUnit", params:[area_unit: areaUnitNo]]]
	def respData = syncSend(createMultiCmd(requests))
	pauseExecution(2000)
	requests = [
		[method: "getCarpetClean"],
		[method: "getChildLockInfo"],
		[method: "getAreaUnit"]]
	respData = syncSend(createMultiCmd(requests)).result.responses
	def new_carpetClean = respData.find {it.method == "getCarpetClean"}.result.carpet_clean_prefer
	device.updateSetting("carpetClean", [type:"enum", value: new_carpetClean])
	Map logData = [carpetClean: new_carpetClean]

	def new_childLock = respData.find {it.method == "getChildLockInfo"}.result.child_lock_status
	device.updateSetting("childLock", [type:"enum", value: new_childLock])
	logData << [childLock: new_childLock]

	def areaUnitValue = respData.find {it.method == "getAreaUnit"}.result.area_unit
	def new_areaUnit = "meters"
	if (areaUnitValue.toInteger() == 1) {new_areaUnit = "feet" }
	device.updateSetting("areaUnit", [type:"enum", value: new_areaUnit])
	logData << [areaUnit: new_areaUnit]
	return logData
}

def setLoginInterval() {
	def startS = Math.round((59) * Math.random()).toInteger()
	def startM = Math.round((59) * Math.random()).toInteger()
	def startH = Math.round((11) * Math.random()).toInteger()
	schedule("${startS} ${startM} ${startH}/8 * * ?", "deviceLogin")
	return "8 hrs"
}

def getDeveloperData() {
	device.updateSetting("developerData",[type:"bool", value: false])
	def attrs = listAttributes()
	Date date = new Date()
	Map devData = [
		currentTime: date.toString(),
		name: device.getName(),
		status: device.getStatus(),
		dataValues: device.getData(),
		attributes: attrs,
		cmdResp: syncSend([method: "get_device_info"]),
	]
	logWarn("<b>DeveloperData</b>: ${devData}")
}

//	===== Communications =====
def createMultiCmd(requests) {
	Map cmdBody = [
		method: "multipleRequest",
		params: [requests: requests]]
	return cmdBody
}

def syncSend(cmdBody) {
	Map logData = [method: "syncSend", cmdBody: cmdBody]
	def uri = "${getDataValue("baseUrl")}/?token=${token}"
	Map cmdResp = [responses: "ERROR"]
	def resp = aesSyncPost(uri, cmdBody)
	if (resp.status == 200) {
		try {
			cmdResp = resp.data
			logData << [status: "OK"]
		} catch (err) {
			logData << [status: "responseError", error: "return data incomplete", data: err]
		}
	} else {
		logData << [status: "postJsonError", postJsonData: resp.properties]
	}
	if (logData.status == "OK") {
	} else {
		logWarn(logData)
	}
	return cmdResp
}

def aesSyncPost(uri, reqBody) {
	def reqParams = [
		uri: uri,
		ignoreSSLIssues: true,
		body: reqBody,
		timeout: 4
	]
	Map respData = [method: "aesSyncPost", reqBody: reqBody, uri: uri, cookie: cookie]
	try {
		httpPostJson(reqParams) {resp ->
			respData << [status: resp.status]
			if (resp.status == 200 && resp.data.error_code == 0) {
				respData << [data: resp.data, headers: resp.headers]
			} else {
				respData << [properties: resp.properties]
			}
		}
	} catch (err) {
		respData << [status: "HTTP Failed", data: err]
	}
	return respData
}

def asyncSend(cmdBody, method, action) {
	Map cmdData = [cmdBody: cmdBody, method: method, action: action]
	Map logData = [method: "asyncSend", cmdData: cmdData]
	state.lastCmd = cmdData
	def reqParams = [
		uri: "${getDataValue("baseUrl")}/?token=${token}",
		body: cmdBody,
		contentType: "application/json",
		requestContentType: "application/json",
		ignoreSSLIssues: true,
		timeout: 4
	]
	try {
		asynchttpPost(action, reqParams, [data: method])
		logData << [status: "OK"]
		logDebug(logData)
	} catch (err) {
		logData << [status: "FAILED", error: err, ]
		logWarn(logData)
	}
}

def parseData(resp) {
	def logData = [method: "parseData"]
	def cmdResp = "ERROR"
	if (resp.status == 200) {
		cmdResp = resp.json
		logData << [status: "OK", cmdResp: cmdResp]
		if (device.currentValue("commsError") == "true") {
			logData << [resetError: setCommsError(false)]
		}
		state.errorCount = 0
	} else {
		logData << [status: "httpFailure(timeout)", data: resp.properties]
//		runIn(1, handleCommsError, [data: "httpFailure"])
		handleCommsError("httpFailure")
	}
	logDebug(logData)
	return cmdResp
}

//	===== Error Handling =====
def handleCommsError(retryReason) {
	Map logData = [method: "handleCommsError", retryReason: retryReason]
	if (state.lastCmd != "") {
		def count = state.errorCount + 1
		state.errorCount = count
		def cmdData = new JSONObject(state.lastCmd)
		def cmdBody = parseJson(cmdData.cmdBody.toString())
		Map data = [cmdBody: cmdBody, method: cmdData.method, action:cmdData.action]
		logData << [count: count, command: cmdData]
		switch (count) {
			case 1:
//				setPollInterval("5 min")
				runIn(1, delayedPassThrough, [data: data])
				logData << [action: "retryCommand"]
				break
			case 2:
				pauseExecution(5000)
				Map loginData = deviceLogin()
				logData << [loginStatus: loginData.loginStatus]
				if (loginData.loginStatus == "OK") {
					logData << [action: "retryCommand"]
					runIn(2, delayedPassThrough, [data:data])
				} else {
					logData << parent.checkDevices()
					logData << [action: "retryCommand"]
					runIn(15, delayedPassThrough, [data:data])
				}
				break
			case 3:
				logData << [loginStatus: setCommsError(true)]
				logWarn(logData)
				break

			default:
				logData << [status: "retriesDisabled"]
				break
		}
	} else {
		logData << [status: "noCommandToRetry"]
	}
	logDebug(logData)
}

def delayedPassThrough(data) {
	asyncSend(data.cmdBody, data.method, data.action)
}

def setCommsError(status, errorData = null) {
	Map logData = [method: "setCommsError", status: status]
	if (status == false) {
		updateAttr("commsError", "false")
		runIn(5, setPollInterval)
		logData << [commsError: false, pollInterval: pollInterval]
	} else {
		logData << [errorData: errorData]
		logData << [pollInterval: "Temporarily set to 5 minutes"]
		updateAttr("commsError", "true")
		logData << [commsError: true]
	}
	return logData
}



// ~~~~~ start include (1405) davegut.Logging ~~~~~
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

// ~~~~~ end include (1405) davegut.Logging ~~~~~
