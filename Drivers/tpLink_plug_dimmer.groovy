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
	definition (name: "tpLink_plug_dimmer", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_dimmer.groovy")
	{
		capability "Switch"
		capability "Switch Level"
		capability "Change Level"
		attribute "connected", "string"
		attribute "commsError", "string"
	}
	preferences {
		commonPreferences()
		input ("ledRule", "enum", title: "LED Mode (if night mode, set type and times in phone app)",
			   options: ["device", "always", "never", "night_mode"], defaultValue: "device")
		input ("autoOffEnable", "bool", title: "Enable Auto Off", defaultValue: false)
		input ("autoOffTime", "NUMBER", title: "Auto Off Time (minutes)", defaultValue: 120)
		input ("defState", "enum", title: "Power Loss Default State",
			   options: ["lastState", "on", "off"], defaultValue: "lastState")
		input ("gradualOnOff", "bool", title: "Set Gradual ON/OFF", defaultValue: false)
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { 
	runIn(5, updated)
}

def updated() { commonUpdated() }

def delayedUpdates() {
	Map logData = [setGradualOnOff: setGradualOnOff()]
	logData << [setAutoOff: setAutoOff()]
	logData << [setDefaultState: setDefaultState()]
	logData << [setLedRule: setLedRule()]
	logData << [common: commonDelayedUpdates()]
	logInfo("delayedUpdates: ${logData}")
}

def deviceParse(resp, data=null) {
	def respData = parseData(resp)
	Map logData = [method: "deviceParse"]
	if (respData.status == "OK") {
		def devData = respData.cmdResp
		if (devData.result.responses) {
			devData = devData.result.responses.find{it.method == "get_device_info"}
		}
		logData << [devData: devData]
		if (devData != null && devData.error_code == 0) {
			devData = devData.result
			def onOff = "off"
			if (devData.device_on == true) { onOff = "on" }
			if (device.currentValue("switch") != onOff) {
				sendEvent(name: "switch", value: onOff, type: state.eventType)
				state.eventType = "physical"
			}
			updateAttr("level", devData.brightness)
		}
	}
	logDebug(logData)
}

//	Library Inclusion






// ~~~~~ start include (1354) davegut.lib_tpLink_CapSwitch ~~~~~
library ( // library marker davegut.lib_tpLink_CapSwitch, line 1
	name: "lib_tpLink_CapSwitch", // library marker davegut.lib_tpLink_CapSwitch, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_CapSwitch, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_CapSwitch, line 4
	description: "Hubitat Capability Switch Methods for TPLink SMART devices.", // library marker davegut.lib_tpLink_CapSwitch, line 5
	category: "utilities", // library marker davegut.lib_tpLink_CapSwitch, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_CapSwitch, line 7
) // library marker davegut.lib_tpLink_CapSwitch, line 8

def on() { // library marker davegut.lib_tpLink_CapSwitch, line 10
	setPower(true) // library marker davegut.lib_tpLink_CapSwitch, line 11
} // library marker davegut.lib_tpLink_CapSwitch, line 12

def off() { // library marker davegut.lib_tpLink_CapSwitch, line 14
	setPower(false) // library marker davegut.lib_tpLink_CapSwitch, line 15
} // library marker davegut.lib_tpLink_CapSwitch, line 16

def setPower(onOff) { // library marker davegut.lib_tpLink_CapSwitch, line 18
	state.eventType = "digital" // library marker davegut.lib_tpLink_CapSwitch, line 19
	logDebug("setPower: [device_on: ${onOff}]") // library marker davegut.lib_tpLink_CapSwitch, line 20
	List requests = [[ // library marker davegut.lib_tpLink_CapSwitch, line 21
		method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 22
		params: [device_on: onOff]]] // library marker davegut.lib_tpLink_CapSwitch, line 23
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 24
	if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_CapSwitch, line 25
		requests << [method:"get_energy_usage"] // library marker davegut.lib_tpLink_CapSwitch, line 26
	} // library marker davegut.lib_tpLink_CapSwitch, line 27
	asyncSend(createMultiCmd(requests), "setPower", "deviceParse") // library marker davegut.lib_tpLink_CapSwitch, line 28
} // library marker davegut.lib_tpLink_CapSwitch, line 29

def setLevel(level, transTime=null) { // library marker davegut.lib_tpLink_CapSwitch, line 31
	//	Note: Tapo Devices do not support transition time.  Set preference "Set Bulb to Gradual ON/OFF" // library marker davegut.lib_tpLink_CapSwitch, line 32
	logDebug("setLevel: [brightness: ${level}]") // library marker davegut.lib_tpLink_CapSwitch, line 33
	List requests = [[ // library marker davegut.lib_tpLink_CapSwitch, line 34
		method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 35
		params: [ // library marker davegut.lib_tpLink_CapSwitch, line 36
			device_on: true, // library marker davegut.lib_tpLink_CapSwitch, line 37
			brightness: level // library marker davegut.lib_tpLink_CapSwitch, line 38
		]]] // library marker davegut.lib_tpLink_CapSwitch, line 39
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 40
	asyncSend(createMultiCmd(requests), "setLevel", "deviceParse") // library marker davegut.lib_tpLink_CapSwitch, line 41
} // library marker davegut.lib_tpLink_CapSwitch, line 42

def startLevelChange(direction) { // library marker davegut.lib_tpLink_CapSwitch, line 44
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]") // library marker davegut.lib_tpLink_CapSwitch, line 45
	if (direction == "up") { levelUp() } // library marker davegut.lib_tpLink_CapSwitch, line 46
	else { levelDown() } // library marker davegut.lib_tpLink_CapSwitch, line 47
} // library marker davegut.lib_tpLink_CapSwitch, line 48

def stopLevelChange() { // library marker davegut.lib_tpLink_CapSwitch, line 50
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]") // library marker davegut.lib_tpLink_CapSwitch, line 51
	unschedule(levelUp) // library marker davegut.lib_tpLink_CapSwitch, line 52
	unschedule(levelDown) // library marker davegut.lib_tpLink_CapSwitch, line 53
} // library marker davegut.lib_tpLink_CapSwitch, line 54

def levelUp() { // library marker davegut.lib_tpLink_CapSwitch, line 56
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapSwitch, line 57
	if (curLevel != 100) { // library marker davegut.lib_tpLink_CapSwitch, line 58
		def newLevel = curLevel + 4 // library marker davegut.lib_tpLink_CapSwitch, line 59
		if (newLevel > 100) { newLevel = 100 } // library marker davegut.lib_tpLink_CapSwitch, line 60
		setLevel(newLevel) // library marker davegut.lib_tpLink_CapSwitch, line 61
		runIn(1, levelUp) // library marker davegut.lib_tpLink_CapSwitch, line 62
	} // library marker davegut.lib_tpLink_CapSwitch, line 63
} // library marker davegut.lib_tpLink_CapSwitch, line 64

def levelDown() { // library marker davegut.lib_tpLink_CapSwitch, line 66
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapSwitch, line 67
	if (device.currentValue("switch") == "on") { // library marker davegut.lib_tpLink_CapSwitch, line 68
		def newLevel = curLevel - 4 // library marker davegut.lib_tpLink_CapSwitch, line 69
		if (newLevel <= 0) { off() } // library marker davegut.lib_tpLink_CapSwitch, line 70
		else { // library marker davegut.lib_tpLink_CapSwitch, line 71
			setLevel(newLevel) // library marker davegut.lib_tpLink_CapSwitch, line 72
			runIn(1, levelDown) // library marker davegut.lib_tpLink_CapSwitch, line 73
		} // library marker davegut.lib_tpLink_CapSwitch, line 74
	} // library marker davegut.lib_tpLink_CapSwitch, line 75
} // library marker davegut.lib_tpLink_CapSwitch, line 76

def setAutoOff() { // library marker davegut.lib_tpLink_CapSwitch, line 78
	List requests =  [[method: "set_auto_off_config", // library marker davegut.lib_tpLink_CapSwitch, line 79
					   params: [enable:autoOffEnable, // library marker davegut.lib_tpLink_CapSwitch, line 80
								delay_min: autoOffTime.toInteger()]]] // library marker davegut.lib_tpLink_CapSwitch, line 81
	requests << [method: "get_auto_off_config"] // library marker davegut.lib_tpLink_CapSwitch, line 82
	def devData = syncSend(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapSwitch, line 83
	Map retData = [cmdResp: "ERROR"] // library marker davegut.lib_tpLink_CapSwitch, line 84
	if (cmdResp != "ERROR") { // library marker davegut.lib_tpLink_CapSwitch, line 85
		def data = devData.result.responses.find { it.method == "get_auto_off_config" } // library marker davegut.lib_tpLink_CapSwitch, line 86
		device.updateSetting("autoOffTime", [type: "number", value: data.result.delay_min]) // library marker davegut.lib_tpLink_CapSwitch, line 87
		device.updateSetting("autoOffEnable", [type: "bool", value: data.result.enable]) // library marker davegut.lib_tpLink_CapSwitch, line 88
		retData = [enable: data.result.enable, time: data.result.delay_min] // library marker davegut.lib_tpLink_CapSwitch, line 89
	} // library marker davegut.lib_tpLink_CapSwitch, line 90
	return retData // library marker davegut.lib_tpLink_CapSwitch, line 91
} // library marker davegut.lib_tpLink_CapSwitch, line 92

def setDefaultState() { // library marker davegut.lib_tpLink_CapSwitch, line 94
	def type = "last_states" // library marker davegut.lib_tpLink_CapSwitch, line 95
	def state = [] // library marker davegut.lib_tpLink_CapSwitch, line 96
	if (defState == "on") { // library marker davegut.lib_tpLink_CapSwitch, line 97
		type = "custom" // library marker davegut.lib_tpLink_CapSwitch, line 98
		state = [on: true] // library marker davegut.lib_tpLink_CapSwitch, line 99
	} else if (defState == "off") { // library marker davegut.lib_tpLink_CapSwitch, line 100
		type = "custom" // library marker davegut.lib_tpLink_CapSwitch, line 101
		state = [on: false] // library marker davegut.lib_tpLink_CapSwitch, line 102
	} // library marker davegut.lib_tpLink_CapSwitch, line 103
	List requests = [[method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 104
					  params: [default_states: [type: type, state: state]]]] // library marker davegut.lib_tpLink_CapSwitch, line 105
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 106
	def devData = syncSend(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapSwitch, line 107
	Map retData = [cmdResp: "ERROR"] // library marker davegut.lib_tpLink_CapSwitch, line 108
	if (cmdResp != "ERROR") { // library marker davegut.lib_tpLink_CapSwitch, line 109
		def data = devData.result.responses.find { it.method == "get_device_info" } // library marker davegut.lib_tpLink_CapSwitch, line 110
		def defaultStates = data.result.default_states // library marker davegut.lib_tpLink_CapSwitch, line 111
		def newState = "lastState" // library marker davegut.lib_tpLink_CapSwitch, line 112
		if (defaultStates.type == "custom"){ // library marker davegut.lib_tpLink_CapSwitch, line 113
			newState = "off" // library marker davegut.lib_tpLink_CapSwitch, line 114
			if (defaultStates.state.on == true) { // library marker davegut.lib_tpLink_CapSwitch, line 115
				newState = "on" // library marker davegut.lib_tpLink_CapSwitch, line 116
			} // library marker davegut.lib_tpLink_CapSwitch, line 117
		} // library marker davegut.lib_tpLink_CapSwitch, line 118
		device.updateSetting("defState", [type: "enum", value: newState]) // library marker davegut.lib_tpLink_CapSwitch, line 119
		retData = [defState: newState] // library marker davegut.lib_tpLink_CapSwitch, line 120
	} // library marker davegut.lib_tpLink_CapSwitch, line 121
	return retData // library marker davegut.lib_tpLink_CapSwitch, line 122
} // library marker davegut.lib_tpLink_CapSwitch, line 123

def setGradualOnOff() { // library marker davegut.lib_tpLink_CapSwitch, line 125
	List requests = [[method: "set_on_off_gradually_info", params: [enable: gradualOnOff]]] // library marker davegut.lib_tpLink_CapSwitch, line 126
	requests << [method: "get_on_off_gradually_info"] // library marker davegut.lib_tpLink_CapSwitch, line 127
	def cmdResp = syncSend(createMultiCmd(requests)) // library marker davegut.lib_tpLink_CapSwitch, line 128
	def gradOnOffData = cmdResp.result.responses.find { it.method == "get_on_off_gradually_info" } // library marker davegut.lib_tpLink_CapSwitch, line 129
	def newGradualOnOff = gradOnOffData.result.enable // library marker davegut.lib_tpLink_CapSwitch, line 130
	device.updateSetting("gradualOnOff",[type:"bool", value: newGradualOnOff]) // library marker davegut.lib_tpLink_CapSwitch, line 131
	return newGradualOnOff // library marker davegut.lib_tpLink_CapSwitch, line 132
} // library marker davegut.lib_tpLink_CapSwitch, line 133


// ~~~~~ end include (1354) davegut.lib_tpLink_CapSwitch ~~~~~

// ~~~~~ start include (1376) davegut.lib_tpLink_common ~~~~~
library ( // library marker davegut.lib_tpLink_common, line 1
	name: "lib_tpLink_common", // library marker davegut.lib_tpLink_common, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_common, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_common, line 4
	description: "Method common to tpLink device DRIVERS", // library marker davegut.lib_tpLink_common, line 5
	category: "utilities", // library marker davegut.lib_tpLink_common, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_common, line 7
) // library marker davegut.lib_tpLink_common, line 8

capability "Refresh" // library marker davegut.lib_tpLink_common, line 10

def commonPreferences() { // library marker davegut.lib_tpLink_common, line 12
	input ("nameSync", "enum", title: "Synchronize Names", // library marker davegut.lib_tpLink_common, line 13
		   options: ["none": "Don't synchronize", // library marker davegut.lib_tpLink_common, line 14
					 "device" : "TP-Link device name master", // library marker davegut.lib_tpLink_common, line 15
					 "Hubitat" : "Hubitat label master"], // library marker davegut.lib_tpLink_common, line 16
		   defaultValue: "none") // library marker davegut.lib_tpLink_common, line 17
	input ("pollInterval", "enum", title: "Poll Interval (< 1 min can cause issues)", // library marker davegut.lib_tpLink_common, line 18
		   options: ["5 sec", "10 sec", "30 sec", "1 min", "10 min", "15 min"], // library marker davegut.lib_tpLink_common, line 19
		   defaultValue: "15 min") // library marker davegut.lib_tpLink_common, line 20
	input ("developerData", "bool", title: "Get Data for Developer", defaultValue: false) // library marker davegut.lib_tpLink_common, line 21
	input ("rebootDev", "bool", title: "Reboot Device then run Save Preferences", defaultValue: false) // library marker davegut.lib_tpLink_common, line 22

	input ("encKey", "password", title: "Crypto key. Do not edit.") // library marker davegut.lib_tpLink_common, line 24
	input ("encIv", "password", title: "Crypto vector. Do not edit.") // library marker davegut.lib_tpLink_common, line 25
	input ("cookie", "password", title: "Session cookie. Do not edit.") // library marker davegut.lib_tpLink_common, line 26
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 27
		input ("encSig", "password", title: "KLAP signature. Do not edit.") // library marker davegut.lib_tpLink_common, line 28
	} else { // library marker davegut.lib_tpLink_common, line 29
		input ("token", "password", title: "AES token. Do not edit.") // library marker davegut.lib_tpLink_common, line 30
	} // library marker davegut.lib_tpLink_common, line 31
} // library marker davegut.lib_tpLink_common, line 32

def commonUpdated() { // library marker davegut.lib_tpLink_common, line 34
	unschedule() // library marker davegut.lib_tpLink_common, line 35
	Map logData = [:] // library marker davegut.lib_tpLink_common, line 36
	if (rebootDev == true) { // library marker davegut.lib_tpLink_common, line 37
		runInMillis(50, rebootDevice) // library marker davegut.lib_tpLink_common, line 38
		device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.lib_tpLink_common, line 39
		pauseExecution(20000) // library marker davegut.lib_tpLink_common, line 40
	} // library marker davegut.lib_tpLink_common, line 41
	updateAttr("commsError", "false") // library marker davegut.lib_tpLink_common, line 42
	updateAttr("connected", "true") // library marker davegut.lib_tpLink_common, line 43
	state.errorCount = 0 // library marker davegut.lib_tpLink_common, line 44
	state.lastCmd = "" // library marker davegut.lib_tpLink_common, line 45
	logData << [pollInterval: setPollInterval()] // library marker davegut.lib_tpLink_common, line 46
	logData << [loginInterval: setLoginInterval()] // library marker davegut.lib_tpLink_common, line 47
	logData << setLogsOff() // library marker davegut.lib_tpLink_common, line 48
	logData << deviceLogin() // library marker davegut.lib_tpLink_common, line 49
	if (logData.status == "ERROR") { // library marker davegut.lib_tpLink_common, line 50
		logWarn("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 51
	} else { // library marker davegut.lib_tpLink_common, line 52
		logInfo("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 53
	} // library marker davegut.lib_tpLink_common, line 54
	runIn(10, delayedUpdates) // library marker davegut.lib_tpLink_common, line 55
} // library marker davegut.lib_tpLink_common, line 56

def commonDelayedUpdates() { // library marker davegut.lib_tpLink_common, line 58
	Map logData = [syncName: syncName()] // library marker davegut.lib_tpLink_common, line 59
	if (developerData) { getDeveloperData() } // library marker davegut.lib_tpLink_common, line 60
	refresh() // library marker davegut.lib_tpLink_common, line 61
	return logData // library marker davegut.lib_tpLink_common, line 62
} // library marker davegut.lib_tpLink_common, line 63

def rebootDevice() { // library marker davegut.lib_tpLink_common, line 65
	logWarn("rebootDevice: Rebooting device per preference request") // library marker davegut.lib_tpLink_common, line 66
	def result = syncSend([method: "device_reboot"]) // library marker davegut.lib_tpLink_common, line 67
	logWarn("rebootDevice: ${result}") // library marker davegut.lib_tpLink_common, line 68
} // library marker davegut.lib_tpLink_common, line 69

def setPollInterval(pInterval = pollInterval) { // library marker davegut.lib_tpLink_common, line 71
	def method = "commonRefresh" // library marker davegut.lib_tpLink_common, line 72
	if (getDataValue("capability") == "hub" || // library marker davegut.lib_tpLink_common, line 73
	    getDataValue("capability") == "plug_multi") { // library marker davegut.lib_tpLink_common, line 74
		method = "parentRefresh" // library marker davegut.lib_tpLink_common, line 75
	} else if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_common, line 76
		method = "emRefresh" // library marker davegut.lib_tpLink_common, line 77
	} // library marker davegut.lib_tpLink_common, line 78
	if (pInterval.contains("sec")) { // library marker davegut.lib_tpLink_common, line 79
		def interval = pInterval.replace(" sec", "").toInteger() // library marker davegut.lib_tpLink_common, line 80
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 81
		schedule("${start}/${interval} * * * * ?", method) // library marker davegut.lib_tpLink_common, line 82
		logInfo("setPollInterval: Polling intervals of less than one minute " + // library marker davegut.lib_tpLink_common, line 83
				"can take high resources and may impact hub performance.") // library marker davegut.lib_tpLink_common, line 84
	} else { // library marker davegut.lib_tpLink_common, line 85
		def interval= pInterval.replace(" min", "").toInteger() // library marker davegut.lib_tpLink_common, line 86
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 87
		schedule("${start} */${interval} * * * ?", method) // library marker davegut.lib_tpLink_common, line 88
	} // library marker davegut.lib_tpLink_common, line 89
	return pInterval // library marker davegut.lib_tpLink_common, line 90
} // library marker davegut.lib_tpLink_common, line 91

def setLoginInterval() { // library marker davegut.lib_tpLink_common, line 93
	def startS = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 94
	def startM = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 95
	def startH = Math.round((11) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 96
	schedule("${startS} ${startM} ${startH}/8 * * ?", "deviceLogin") // library marker davegut.lib_tpLink_common, line 97
	return "8 hrs" // library marker davegut.lib_tpLink_common, line 98
} // library marker davegut.lib_tpLink_common, line 99

def syncName() { // library marker davegut.lib_tpLink_common, line 101
	def logData = [syncName: nameSync] // library marker davegut.lib_tpLink_common, line 102
	if (nameSync == "none") { // library marker davegut.lib_tpLink_common, line 103
		logData << [status: "Label Not Updated"] // library marker davegut.lib_tpLink_common, line 104
	} else { // library marker davegut.lib_tpLink_common, line 105
		def cmdResp // library marker davegut.lib_tpLink_common, line 106
		String nickname // library marker davegut.lib_tpLink_common, line 107
		if (nameSync == "device") { // library marker davegut.lib_tpLink_common, line 108
			cmdResp = syncSend([method: "get_device_info"]) // library marker davegut.lib_tpLink_common, line 109
			nickname = cmdResp.result.nickname // library marker davegut.lib_tpLink_common, line 110
		} else if (nameSync == "Hubitat") { // library marker davegut.lib_tpLink_common, line 111
			nickname = device.getLabel().bytes.encodeBase64().toString() // library marker davegut.lib_tpLink_common, line 112
			List requests = [[method: "set_device_info",params: [nickname: nickname]]] // library marker davegut.lib_tpLink_common, line 113
			requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_common, line 114
			cmdResp = syncSend(createMultiCmd(requests)) // library marker davegut.lib_tpLink_common, line 115
			cmdResp = cmdResp.result.responses.find { it.method == "get_device_info" } // library marker davegut.lib_tpLink_common, line 116
			nickname = cmdResp.result.nickname // library marker davegut.lib_tpLink_common, line 117
		} // library marker davegut.lib_tpLink_common, line 118
		byte[] plainBytes = nickname.decodeBase64() // library marker davegut.lib_tpLink_common, line 119
		String label = new String(plainBytes) // library marker davegut.lib_tpLink_common, line 120
		device.setLabel(label) // library marker davegut.lib_tpLink_common, line 121
		logData << [nickname: nickname, label: label, status: "Label Updated"] // library marker davegut.lib_tpLink_common, line 122
	} // library marker davegut.lib_tpLink_common, line 123
	device.updateSetting("nameSync", [type: "enum", value: "none"]) // library marker davegut.lib_tpLink_common, line 124
	return logData // library marker davegut.lib_tpLink_common, line 125
} // library marker davegut.lib_tpLink_common, line 126

def setLedRule() { // library marker davegut.lib_tpLink_common, line 128
	def respData = syncSend([method: "get_led_info"]).result // library marker davegut.lib_tpLink_common, line 129
	Map logData = [method: "setLed", ledRule: ledRule, currentRule: respData.led_rule] // library marker davegut.lib_tpLink_common, line 130
	if (ledRule != respData.led_rule) { // library marker davegut.lib_tpLink_common, line 131
		Map requests = [ // library marker davegut.lib_tpLink_common, line 132
			method: "set_led_info", // library marker davegut.lib_tpLink_common, line 133
			params: [ // library marker davegut.lib_tpLink_common, line 134
				led_rule: ledRule, // library marker davegut.lib_tpLink_common, line 135
				night_mode: [ // library marker davegut.lib_tpLink_common, line 136
					night_mode_type: respData.night_mode.night_mode_type, // library marker davegut.lib_tpLink_common, line 137
					sunrise_offset: respData.night_mode.sunrise_offset,  // library marker davegut.lib_tpLink_common, line 138
					sunset_offset:respData.night_mode.sunset_offset, // library marker davegut.lib_tpLink_common, line 139
					start_time: respData.night_mode.start_time, // library marker davegut.lib_tpLink_common, line 140
					end_time: respData.night_mode.end_time // library marker davegut.lib_tpLink_common, line 141
				]]] // library marker davegut.lib_tpLink_common, line 142
		respData = syncSend(requests).result // library marker davegut.lib_tpLink_common, line 143
		respData = syncSend([method: "get_led_info"]).result // library marker davegut.lib_tpLink_common, line 144
		logData << [status: "Updated"] // library marker davegut.lib_tpLink_common, line 145
	} else { // library marker davegut.lib_tpLink_common, line 146
		logData << [status: "NoChange"] // library marker davegut.lib_tpLink_common, line 147
	} // library marker davegut.lib_tpLink_common, line 148
	device.updateSetting("ledRule", [type:"enum", value: respData.led_rule]) // library marker davegut.lib_tpLink_common, line 149
	return logData // library marker davegut.lib_tpLink_common, line 150

} // library marker davegut.lib_tpLink_common, line 152

def getDeveloperData() { // library marker davegut.lib_tpLink_common, line 154
	device.updateSetting("developerData",[type:"bool", value: false]) // library marker davegut.lib_tpLink_common, line 155
	def attrs = listAttributes() // library marker davegut.lib_tpLink_common, line 156
	Date date = new Date() // library marker davegut.lib_tpLink_common, line 157
	Map devData = [ // library marker davegut.lib_tpLink_common, line 158
		currentTime: date.toString(), // library marker davegut.lib_tpLink_common, line 159
		name: device.getName(), // library marker davegut.lib_tpLink_common, line 160
		status: device.getStatus(), // library marker davegut.lib_tpLink_common, line 161
		dataValues: device.getData(), // library marker davegut.lib_tpLink_common, line 162
		attributes: attrs, // library marker davegut.lib_tpLink_common, line 163
		cmdResp: syncSend([method: "get_device_info"]), // library marker davegut.lib_tpLink_common, line 164
		childData: getChildDevData() // library marker davegut.lib_tpLink_common, line 165
	] // library marker davegut.lib_tpLink_common, line 166
	logWarn("DEVELOPER DATA: ${devData}") // library marker davegut.lib_tpLink_common, line 167
} // library marker davegut.lib_tpLink_common, line 168

def getChildDevData(){ // library marker davegut.lib_tpLink_common, line 170
	Map cmdBody = [ // library marker davegut.lib_tpLink_common, line 171
		method: "get_child_device_list" // library marker davegut.lib_tpLink_common, line 172
	] // library marker davegut.lib_tpLink_common, line 173
	def childData = syncSend(cmdBody) // library marker davegut.lib_tpLink_common, line 174
	if (childData.error_code == 0) { // library marker davegut.lib_tpLink_common, line 175
		return childData.result.child_device_list // library marker davegut.lib_tpLink_common, line 176
	} else { // library marker davegut.lib_tpLink_common, line 177
		return "noChildren" // library marker davegut.lib_tpLink_common, line 178
	} // library marker davegut.lib_tpLink_common, line 179
} // library marker davegut.lib_tpLink_common, line 180

def deviceLogin() { // library marker davegut.lib_tpLink_common, line 182
	Map logData = [method: "deviceLogin"] // library marker davegut.lib_tpLink_common, line 183
	Map sessionData =[:] // library marker davegut.lib_tpLink_common, line 184
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 185
		sessionData = klapLogin(getDataValue("baseUrl"), // library marker davegut.lib_tpLink_common, line 186
								parent.localHash.decodeBase64()) // library marker davegut.lib_tpLink_common, line 187
		logData << [localHash: parent.localHash.decodeBase64()] // library marker davegut.lib_tpLink_common, line 188
	} else { // library marker davegut.lib_tpLink_common, line 189
		sessionData = aesLogin(getDataValue("baseUrl"), parent.encPassword, // library marker davegut.lib_tpLink_common, line 190
							   parent.encUsername) // library marker davegut.lib_tpLink_common, line 191
		logData << [encPwd: parent.encPassword, encUser: parent.encUsername] // library marker davegut.lib_tpLink_common, line 192
	} // library marker davegut.lib_tpLink_common, line 193
	logData << [sessionData: sessionData] // library marker davegut.lib_tpLink_common, line 194
	if (sessionData.status == "OK") { // library marker davegut.lib_tpLink_common, line 195
		state.seqNo = sessionData.seqNo // library marker davegut.lib_tpLink_common, line 196
		device.updateSetting("encKey",[type:"password", value: sessionData.encKey]) // library marker davegut.lib_tpLink_common, line 197
		device.updateSetting("encIv",[type:"password", value: sessionData.encIv]) // library marker davegut.lib_tpLink_common, line 198
		device.updateSetting("cookie",[type:"password", value: sessionData.cookie]) // library marker davegut.lib_tpLink_common, line 199
		if (sessionData.protocol == "KLAP") { // library marker davegut.lib_tpLink_common, line 200
			device.updateSetting("encSig",[type:"password", value: sessionData.encSig]) // library marker davegut.lib_tpLink_common, line 201
		} else { // library marker davegut.lib_tpLink_common, line 202
			device.updateSetting("token",[type:"password", value: sessionData.token]) // library marker davegut.lib_tpLink_common, line 203
		} // library marker davegut.lib_tpLink_common, line 204
	} else { // library marker davegut.lib_tpLink_common, line 205
		logWarn(logData) // library marker davegut.lib_tpLink_common, line 206
	} // library marker davegut.lib_tpLink_common, line 207
	return [method: "deviceLogin", loginStatus: sessionData.status] // library marker davegut.lib_tpLink_common, line 208
} // library marker davegut.lib_tpLink_common, line 209

def refresh() { // library marker davegut.lib_tpLink_common, line 211
	if (getDataValue("capability") == "hub" || // library marker davegut.lib_tpLink_common, line 212
	    getDataValue("capability") == "plug_parent") { // library marker davegut.lib_tpLink_common, line 213
		parentRefresh() // library marker davegut.lib_tpLink_common, line 214
	} else if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_common, line 215
		emRefresh() // library marker davegut.lib_tpLink_common, line 216
	} else { // library marker davegut.lib_tpLink_common, line 217
		asyncSend([method: "get_device_info"], "refresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 218
	} // library marker davegut.lib_tpLink_common, line 219
} // library marker davegut.lib_tpLink_common, line 220

def commonRefresh() { // library marker davegut.lib_tpLink_common, line 222
	asyncSend([method: "get_device_info"], "refresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 223
} // library marker davegut.lib_tpLink_common, line 224

def emRefresh() { // library marker davegut.lib_tpLink_common, line 226
	List requests = [[method: "get_device_info"]] // library marker davegut.lib_tpLink_common, line 227
	requests << [method:"get_energy_usage"] // library marker davegut.lib_tpLink_common, line 228
	asyncSend(createMultiCmd(requests), "emRefresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 229
} // library marker davegut.lib_tpLink_common, line 230

def parentRefresh() { // library marker davegut.lib_tpLink_common, line 232
	List requests = [[method: "get_device_info"]] // library marker davegut.lib_tpLink_common, line 233
	requests << [method:"get_child_device_list"] // library marker davegut.lib_tpLink_common, line 234
	asyncSend(createMultiCmd(requests), "parentRefresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 235
} // library marker davegut.lib_tpLink_common, line 236

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_common, line 238
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_common, line 239
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_common, line 240
	} // library marker davegut.lib_tpLink_common, line 241
} // library marker davegut.lib_tpLink_common, line 242

// ~~~~~ end include (1376) davegut.lib_tpLink_common ~~~~~

// ~~~~~ start include (1377) davegut.lib_tpLink_comms ~~~~~
library ( // library marker davegut.lib_tpLink_comms, line 1
	name: "lib_tpLink_comms", // library marker davegut.lib_tpLink_comms, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_comms, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_comms, line 4
	description: "Tapo Communications", // library marker davegut.lib_tpLink_comms, line 5
	category: "utilities", // library marker davegut.lib_tpLink_comms, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_comms, line 7
) // library marker davegut.lib_tpLink_comms, line 8
import org.json.JSONObject // library marker davegut.lib_tpLink_comms, line 9
import groovy.json.JsonOutput // library marker davegut.lib_tpLink_comms, line 10
import groovy.json.JsonBuilder // library marker davegut.lib_tpLink_comms, line 11
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_comms, line 12

def createMultiCmd(requests) { // library marker davegut.lib_tpLink_comms, line 14
	Map cmdBody = [ // library marker davegut.lib_tpLink_comms, line 15
		method: "multipleRequest", // library marker davegut.lib_tpLink_comms, line 16
		params: [requests: requests]] // library marker davegut.lib_tpLink_comms, line 17
	return cmdBody // library marker davegut.lib_tpLink_comms, line 18
} // library marker davegut.lib_tpLink_comms, line 19

def syncSend(cmdBody) { // library marker davegut.lib_tpLink_comms, line 21
	Map cmdResp = [:] // library marker davegut.lib_tpLink_comms, line 22
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_comms, line 23
		cmdResp = klapSyncSend(cmdBody) // library marker davegut.lib_tpLink_comms, line 24
	} else { // library marker davegut.lib_tpLink_comms, line 25
		cmdResp = aesSyncSend(cmdBody) // library marker davegut.lib_tpLink_comms, line 26
	} // library marker davegut.lib_tpLink_comms, line 27
	return cmdResp // library marker davegut.lib_tpLink_comms, line 28
} // library marker davegut.lib_tpLink_comms, line 29

def klapSyncSend(cmdBody) { // library marker davegut.lib_tpLink_comms, line 31
	Map logData = [method: "klapSyncSend", cmdBody: cmdBody] // library marker davegut.lib_tpLink_comms, line 32
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_comms, line 33
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_comms, line 34
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.lib_tpLink_comms, line 35
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 36
	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_comms, line 37
	def uri = "${getDataValue("baseUrl")}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_comms, line 38
	def resp = klapSyncPost(uri, encryptedData.cipherData, cookie) // library marker davegut.lib_tpLink_comms, line 39
	Map cmdResp = [status: "ERROR"] // library marker davegut.lib_tpLink_comms, line 40
	if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 41
		try { // library marker davegut.lib_tpLink_comms, line 42
			byte[] cipherResponse = resp.data[32..-1] // library marker davegut.lib_tpLink_comms, line 43
			def clearResp =  klapDecrypt(cipherResponse, encKey, encIv) // library marker davegut.lib_tpLink_comms, line 44
			cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_comms, line 45
			logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 46
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 47
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 48
		} // library marker davegut.lib_tpLink_comms, line 49
	} else { // library marker davegut.lib_tpLink_comms, line 50
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_comms, line 51
	} // library marker davegut.lib_tpLink_comms, line 52
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 53
		logDebug(logData) // library marker davegut.lib_tpLink_comms, line 54
	} else { // library marker davegut.lib_tpLink_comms, line 55
		logWarn(logData) // library marker davegut.lib_tpLink_comms, line 56
	} // library marker davegut.lib_tpLink_comms, line 57
	return cmdResp // library marker davegut.lib_tpLink_comms, line 58
} // library marker davegut.lib_tpLink_comms, line 59

def aesSyncSend(cmdBody) { // library marker davegut.lib_tpLink_comms, line 61
	Map logData = [method: "aesSyncSend", cmdBody: cmdBody] // library marker davegut.lib_tpLink_comms, line 62
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_comms, line 63
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_comms, line 64
	def uri = "${getDataValue("baseUrl")}?token=${token}" // library marker davegut.lib_tpLink_comms, line 65
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 66
	Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_comms, line 67
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_comms, line 68
	def resp = aesSyncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_comms, line 69
	Map cmdResp = [status: "ERROR"] // library marker davegut.lib_tpLink_comms, line 70
	if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 71
		try { // library marker davegut.lib_tpLink_comms, line 72
			def clearResp = aesDecrypt(resp.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_comms, line 73
			cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_comms, line 74
			logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 75
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 76
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 77
		} // library marker davegut.lib_tpLink_comms, line 78
	} else { // library marker davegut.lib_tpLink_comms, line 79
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_comms, line 80
	} // library marker davegut.lib_tpLink_comms, line 81
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 82
		logDebug(logData) // library marker davegut.lib_tpLink_comms, line 83
	} else { // library marker davegut.lib_tpLink_comms, line 84
		logWarn(logData) // library marker davegut.lib_tpLink_comms, line 85
	} // library marker davegut.lib_tpLink_comms, line 86
	return cmdResp // library marker davegut.lib_tpLink_comms, line 87
} // library marker davegut.lib_tpLink_comms, line 88

def asyncSend(cmdBody, method, action) { // library marker davegut.lib_tpLink_comms, line 90
	Map cmdData = [cmdBody: cmdBody, method: method, action: action] // library marker davegut.lib_tpLink_comms, line 91
	state.lastCmd = cmdData // library marker davegut.lib_tpLink_comms, line 92
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_comms, line 93
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_comms, line 94
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_comms, line 95
		byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.lib_tpLink_comms, line 96
		String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 97
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_comms, line 98
		def uri = "${getDataValue("baseUrl")}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_comms, line 99
		asyncPost(uri, encryptedData.cipherData, "application/octet-stream", // library marker davegut.lib_tpLink_comms, line 100
					  action, cookie, method) // library marker davegut.lib_tpLink_comms, line 101
	} else { // library marker davegut.lib_tpLink_comms, line 102
		def uri = "${getDataValue("baseUrl")}?token=${token}" // library marker davegut.lib_tpLink_comms, line 103
		def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 104
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_comms, line 105
					   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_comms, line 106
		def body = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_comms, line 107
		asyncPost(uri, body, "application/json",  // library marker davegut.lib_tpLink_comms, line 108
					  action, cookie, method) // library marker davegut.lib_tpLink_comms, line 109
	} // library marker davegut.lib_tpLink_comms, line 110
} // library marker davegut.lib_tpLink_comms, line 111

//	===== HTTP POST Methods ===== // library marker davegut.lib_tpLink_comms, line 113
def klapSyncPost(uri, byte[] body, cookie = null) { // library marker davegut.lib_tpLink_comms, line 114
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 115
		uri: uri, // library marker davegut.lib_tpLink_comms, line 116
		body: body, // library marker davegut.lib_tpLink_comms, line 117
		contentType: "application/octet-stream", // library marker davegut.lib_tpLink_comms, line 118
		requestContentType: "application/octet-stream", // library marker davegut.lib_tpLink_comms, line 119
		headers: [ // library marker davegut.lib_tpLink_comms, line 120
			"Cookie": cookie, // library marker davegut.lib_tpLink_comms, line 121
		], // library marker davegut.lib_tpLink_comms, line 122
		ignoreSSLIssues: true, // library marker davegut.lib_tpLink_comms, line 123
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 124
	] // library marker davegut.lib_tpLink_comms, line 125
	Map respData = [method: "klapSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_comms, line 126
	try { // library marker davegut.lib_tpLink_comms, line 127
		httpPost(reqParams) { resp -> // library marker davegut.lib_tpLink_comms, line 128
			respData << [status: resp.status] // library marker davegut.lib_tpLink_comms, line 129
			if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 130
				byte[] data = [] // library marker davegut.lib_tpLink_comms, line 131
				if (resp.data != null) { // library marker davegut.lib_tpLink_comms, line 132
					data = parseInputStream(resp.data) // library marker davegut.lib_tpLink_comms, line 133
				} // library marker davegut.lib_tpLink_comms, line 134
				respData << [data: data, headers: resp.headers] // library marker davegut.lib_tpLink_comms, line 135
			} else { // library marker davegut.lib_tpLink_comms, line 136
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_comms, line 137
			} // library marker davegut.lib_tpLink_comms, line 138
		} // library marker davegut.lib_tpLink_comms, line 139
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 140
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 141
	} // library marker davegut.lib_tpLink_comms, line 142
	return respData // library marker davegut.lib_tpLink_comms, line 143
} // library marker davegut.lib_tpLink_comms, line 144
def parseInputStream(data) { // library marker davegut.lib_tpLink_comms, line 145
	def dataSize = data.available() // library marker davegut.lib_tpLink_comms, line 146
	byte[] dataArr = new byte[dataSize] // library marker davegut.lib_tpLink_comms, line 147
	data.read(dataArr, 0, dataSize) // library marker davegut.lib_tpLink_comms, line 148
	return dataArr // library marker davegut.lib_tpLink_comms, line 149
} // library marker davegut.lib_tpLink_comms, line 150

def aesSyncPost(uri, reqBody, cookie=null) { // library marker davegut.lib_tpLink_comms, line 152
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 153
		uri: uri, // library marker davegut.lib_tpLink_comms, line 154
		headers: [ // library marker davegut.lib_tpLink_comms, line 155
			Cookie: cookie, // library marker davegut.lib_tpLink_comms, line 156
		], // library marker davegut.lib_tpLink_comms, line 157
		//	body: reqBody, // library marker davegut.lib_tpLink_comms, line 158
		body : new JsonBuilder(reqBody).toString(), // library marker davegut.lib_tpLink_comms, line 159
		ignoreSSLIssues: true, // library marker davegut.lib_tpLink_comms, line 160
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 161
	] // library marker davegut.lib_tpLink_comms, line 162
	Map respData = [method: "aesSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_comms, line 163
	try { // library marker davegut.lib_tpLink_comms, line 164
		httpPostJson(reqParams) {resp -> // library marker davegut.lib_tpLink_comms, line 165
			respData << [status: resp.status] // library marker davegut.lib_tpLink_comms, line 166
			if (resp.status == 200 && resp.data.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 167
				respData << [data: resp.data, headers: resp.headers] // library marker davegut.lib_tpLink_comms, line 168
			} else { // library marker davegut.lib_tpLink_comms, line 169
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_comms, line 170
			} // library marker davegut.lib_tpLink_comms, line 171
		} // library marker davegut.lib_tpLink_comms, line 172
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 173
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 174
	} // library marker davegut.lib_tpLink_comms, line 175
	return respData // library marker davegut.lib_tpLink_comms, line 176
} // library marker davegut.lib_tpLink_comms, line 177

def asyncPost(uri, body, contentType, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_comms, line 179
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 180
		uri: uri, // library marker davegut.lib_tpLink_comms, line 181
		body: body, // library marker davegut.lib_tpLink_comms, line 182
		contentType: contentType, // library marker davegut.lib_tpLink_comms, line 183
		requestContentType: contentType, // library marker davegut.lib_tpLink_comms, line 184
		headers: [ // library marker davegut.lib_tpLink_comms, line 185
			"Cookie": cookie, // library marker davegut.lib_tpLink_comms, line 186
		], // library marker davegut.lib_tpLink_comms, line 187
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 188
	] // library marker davegut.lib_tpLink_comms, line 189
	Map logData = [method: "asyncPost", uri: uri,  // library marker davegut.lib_tpLink_comms, line 190
				   parseMethod: parseMethod, cookie: cookie, reqData: reqData] // library marker davegut.lib_tpLink_comms, line 191
	try { // library marker davegut.lib_tpLink_comms, line 192
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_comms, line 193
		logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 194
		logDebug(logData) // library marker davegut.lib_tpLink_comms, line 195
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 196
		logData << [status: "FAILED", error: err, ] // library marker davegut.lib_tpLink_comms, line 197
		logWarn(logData) // library marker davegut.lib_tpLink_comms, line 198
	} // library marker davegut.lib_tpLink_comms, line 199
} // library marker davegut.lib_tpLink_comms, line 200
def parseData(resp) { // library marker davegut.lib_tpLink_comms, line 201
	def logData = [method: "parseData"] // library marker davegut.lib_tpLink_comms, line 202
	if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 203
		try { // library marker davegut.lib_tpLink_comms, line 204
			Map cmdResp // library marker davegut.lib_tpLink_comms, line 205
			byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_comms, line 206
			byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_comms, line 207
			if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_comms, line 208
				byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.lib_tpLink_comms, line 209
				cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, encIv)) // library marker davegut.lib_tpLink_comms, line 210
			} else { // library marker davegut.lib_tpLink_comms, line 211
				cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, encKey, encIv)) // library marker davegut.lib_tpLink_comms, line 212
			} // library marker davegut.lib_tpLink_comms, line 213
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 214
			if (device.currentValue("commsError") == "true") { // library marker davegut.lib_tpLink_comms, line 215
				logData << [resetError: setCommsError(false)] // library marker davegut.lib_tpLink_comms, line 216
			} // library marker davegut.lib_tpLink_comms, line 217
			state.errorCount = 0 // library marker davegut.lib_tpLink_comms, line 218
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 219
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.lib_tpLink_comms, line 220
			runIn(1, handleCommsError, [data: "deviceDataParseError"]) // library marker davegut.lib_tpLink_comms, line 221
		} // library marker davegut.lib_tpLink_comms, line 222
	} else { // library marker davegut.lib_tpLink_comms, line 223
		logData << [status: "httpFailure(timeout)", data: resp.properties] // library marker davegut.lib_tpLink_comms, line 224
		runIn(1, handleCommsError, [data: "httpFailure(timeout)"]) // library marker davegut.lib_tpLink_comms, line 225
	} // library marker davegut.lib_tpLink_comms, line 226
	logDebug(logData) // library marker davegut.lib_tpLink_comms, line 227
	return logData // library marker davegut.lib_tpLink_comms, line 228
} // library marker davegut.lib_tpLink_comms, line 229

//	===== Error Handling ===== // library marker davegut.lib_tpLink_comms, line 231
def handleCommsError(retryReason) { // library marker davegut.lib_tpLink_comms, line 232
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.lib_tpLink_comms, line 233
	if (state.lastCmd != "") { // library marker davegut.lib_tpLink_comms, line 234
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_comms, line 235
		state.errorCount = count // library marker davegut.lib_tpLink_comms, line 236
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_comms, line 237
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_comms, line 238
		Map data = [cmdBody: cmdBody, method: cmdData.method, action:cmdData.action] // library marker davegut.lib_tpLink_comms, line 239
		logData << [count: count, command: cmdData] // library marker davegut.lib_tpLink_comms, line 240
		switch (count) { // library marker davegut.lib_tpLink_comms, line 241
			case 1: // library marker davegut.lib_tpLink_comms, line 242
				setPollInterval("5 min") // library marker davegut.lib_tpLink_comms, line 243
				runIn(1, delayedPassThrough, [data: data]) // library marker davegut.lib_tpLink_comms, line 244
				logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 245
				break // library marker davegut.lib_tpLink_comms, line 246
			case 2: // library marker davegut.lib_tpLink_comms, line 247
				pauseExecution(5000) // library marker davegut.lib_tpLink_comms, line 248
				Map loginData = deviceLogin() // library marker davegut.lib_tpLink_comms, line 249
				logData << [loginStatus: loginData.loginStatus] // library marker davegut.lib_tpLink_comms, line 250
				if (loginData.loginStatus == "OK") { // library marker davegut.lib_tpLink_comms, line 251
					logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 252
					runIn(2, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 253
				} else { // library marker davegut.lib_tpLink_comms, line 254
					logData << parent.checkDevices() // library marker davegut.lib_tpLink_comms, line 255
					logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 256
					runIn(15, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 257
				} // library marker davegut.lib_tpLink_comms, line 258
				break // library marker davegut.lib_tpLink_comms, line 259
			case 3: // library marker davegut.lib_tpLink_comms, line 260
				logData << [loginStatus: setCommsError(true)] // library marker davegut.lib_tpLink_comms, line 261
				logWarn(logData) // library marker davegut.lib_tpLink_comms, line 262
				break // library marker davegut.lib_tpLink_comms, line 263

			default: // library marker davegut.lib_tpLink_comms, line 265
				logData << [status: "retriesDisabled"] // library marker davegut.lib_tpLink_comms, line 266
				break // library marker davegut.lib_tpLink_comms, line 267
		} // library marker davegut.lib_tpLink_comms, line 268
	} else { // library marker davegut.lib_tpLink_comms, line 269
		logData << [status: "noCommandToRetry"] // library marker davegut.lib_tpLink_comms, line 270
	} // library marker davegut.lib_tpLink_comms, line 271
	logDebug(logData) // library marker davegut.lib_tpLink_comms, line 272
} // library marker davegut.lib_tpLink_comms, line 273

def delayedPassThrough(data) { // library marker davegut.lib_tpLink_comms, line 275
	asyncSend(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_comms, line 276
} // library marker davegut.lib_tpLink_comms, line 277

def setCommsError(status, errorData = null) { // library marker davegut.lib_tpLink_comms, line 279
	Map logData = [method: "setCommsError", status: status] // library marker davegut.lib_tpLink_comms, line 280
	if (status == false) { // library marker davegut.lib_tpLink_comms, line 281
		updateAttr("commsError", "false") // library marker davegut.lib_tpLink_comms, line 282
		runIn(5, setPollInterval) // library marker davegut.lib_tpLink_comms, line 283
		logData << [commsError: false, pollInterval: pollInterval] // library marker davegut.lib_tpLink_comms, line 284
	} else { // library marker davegut.lib_tpLink_comms, line 285
		logData << [errorData: errorData] // library marker davegut.lib_tpLink_comms, line 286
		logData << [pollInterval: "Temporarily set to 5 minutes"] // library marker davegut.lib_tpLink_comms, line 287
		updateAttr("commsError", "true") // library marker davegut.lib_tpLink_comms, line 288
		logData << [commsError: true] // library marker davegut.lib_tpLink_comms, line 289
	} // library marker davegut.lib_tpLink_comms, line 290
	return logData // library marker davegut.lib_tpLink_comms, line 291
} // library marker davegut.lib_tpLink_comms, line 292

// ~~~~~ end include (1377) davegut.lib_tpLink_comms ~~~~~

// ~~~~~ start include (1378) davegut.lib_tpLink_security ~~~~~
library ( // library marker davegut.lib_tpLink_security, line 1
	name: "lib_tpLink_security", // library marker davegut.lib_tpLink_security, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_security, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_security, line 4
	description: "tpLink security methods", // library marker davegut.lib_tpLink_security, line 5
	category: "utilities", // library marker davegut.lib_tpLink_security, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_security, line 7
) // library marker davegut.lib_tpLink_security, line 8
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_security, line 9
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.lib_tpLink_security, line 10
import javax.crypto.spec.SecretKeySpec // library marker davegut.lib_tpLink_security, line 11
import javax.crypto.spec.IvParameterSpec // library marker davegut.lib_tpLink_security, line 12
import javax.crypto.Cipher // library marker davegut.lib_tpLink_security, line 13
import java.security.KeyFactory // library marker davegut.lib_tpLink_security, line 14
import java.util.Random // library marker davegut.lib_tpLink_security, line 15
import java.security.MessageDigest // library marker davegut.lib_tpLink_security, line 16

//	===== KLAP Handshake and Login ===== // library marker davegut.lib_tpLink_security, line 18
def klapLogin(baseUrl, localHash) { // library marker davegut.lib_tpLink_security, line 19
	Map logData = [method: "klapLogin"] // library marker davegut.lib_tpLink_security, line 20
	Map sessionData = [protocol: "KLAP"] // library marker davegut.lib_tpLink_security, line 21
	byte[] localSeed = new byte[16] // library marker davegut.lib_tpLink_security, line 22
	new Random().nextBytes(localSeed) // library marker davegut.lib_tpLink_security, line 23
	def status = "ERROR" // library marker davegut.lib_tpLink_security, line 24
	Map handshakeData = klapHandshake(localSeed, localHash, "${baseUrl}/handshake1") // library marker davegut.lib_tpLink_security, line 25
	logData << [handshake: handshakeData] // library marker davegut.lib_tpLink_security, line 26
	sessionData << [handshakeValidated: handshakeData.validated] // library marker davegut.lib_tpLink_security, line 27
	if (handshakeData.validated == true) { // library marker davegut.lib_tpLink_security, line 28
		sessionData << klapCreateSessionData(localSeed, handshakeData.remoteSeed, // library marker davegut.lib_tpLink_security, line 29
											 localHash, handshakeData.cookie) // library marker davegut.lib_tpLink_security, line 30
		Map loginData = klapLoginDevice("${baseUrl}/handshake2", localHash, localSeed,  // library marker davegut.lib_tpLink_security, line 31
										handshakeData.remoteSeed, handshakeData.cookie) // library marker davegut.lib_tpLink_security, line 32
		logData << [loginData: loginData] // library marker davegut.lib_tpLink_security, line 33
		if (loginData.loginSuccess == true) { // library marker davegut.lib_tpLink_security, line 34
			status = "OK" // library marker davegut.lib_tpLink_security, line 35
		} // library marker davegut.lib_tpLink_security, line 36
	} // library marker davegut.lib_tpLink_security, line 37
	sessionData << [status: status] // library marker davegut.lib_tpLink_security, line 38
	if (status != "OK") { // library marker davegut.lib_tpLink_security, line 39
		logInfo(logData) // library marker davegut.lib_tpLink_security, line 40
	} // library marker davegut.lib_tpLink_security, line 41
	return sessionData // library marker davegut.lib_tpLink_security, line 42
} // library marker davegut.lib_tpLink_security, line 43

def klapHandshake(localSeed, localHash, uri) { // library marker davegut.lib_tpLink_security, line 45
	Map handshakeData = [method: "klapHandshake", localSeed: localSeed, uri: uri, localHash: localHash] // library marker davegut.lib_tpLink_security, line 46
	def validated = false // library marker davegut.lib_tpLink_security, line 47
	Map respData = klapSyncPost(uri, localSeed) // library marker davegut.lib_tpLink_security, line 48
	if (respData.status == 200 && respData.data != null) { // library marker davegut.lib_tpLink_security, line 49
		byte[] data = respData.data // library marker davegut.lib_tpLink_security, line 50
		def cookieHeader = respData.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_security, line 51
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_security, line 52
		//	Validate data // library marker davegut.lib_tpLink_security, line 53
		byte[] remoteSeed = data[0 .. 15] // library marker davegut.lib_tpLink_security, line 54
		byte[] serverHash = data[16 .. 47] // library marker davegut.lib_tpLink_security, line 55
		byte[] authHashes = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_security, line 56
		byte[] localAuthHash = mdEncode("SHA-256", authHashes) // library marker davegut.lib_tpLink_security, line 57
		if (localAuthHash == serverHash) { // library marker davegut.lib_tpLink_security, line 58
			validated = true // library marker davegut.lib_tpLink_security, line 59
			handshakeData << [cookie : cookie] // library marker davegut.lib_tpLink_security, line 60
			handshakeData << [remoteSeed: remoteSeed] // library marker davegut.lib_tpLink_security, line 61
		} else { // library marker davegut.lib_tpLink_security, line 62
			handshakeData << [errorData: "Failed Hash Validation"] // library marker davegut.lib_tpLink_security, line 63
		} // library marker davegut.lib_tpLink_security, line 64
	} else { // library marker davegut.lib_tpLink_security, line 65
		handshakeData << [errorData: respData] // library marker davegut.lib_tpLink_security, line 66
	} // library marker davegut.lib_tpLink_security, line 67
	handshakeData << [validated: validated] // library marker davegut.lib_tpLink_security, line 68
	return handshakeData // library marker davegut.lib_tpLink_security, line 69
} // library marker davegut.lib_tpLink_security, line 70

def klapCreateSessionData(localSeed, remoteSeed, localHash, cookie) { // library marker davegut.lib_tpLink_security, line 72
	Map sessionData = [method: "klapCreateSessionData"] // library marker davegut.lib_tpLink_security, line 73
	//	seqNo and encIv // library marker davegut.lib_tpLink_security, line 74
	byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_security, line 75
	byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_security, line 76
	byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.lib_tpLink_security, line 77
	int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.lib_tpLink_security, line 78
	sessionData << [seqNo: seqNo] // library marker davegut.lib_tpLink_security, line 79
	sessionData << [encIv: fullIv[0..11], cookie: cookie] // library marker davegut.lib_tpLink_security, line 80
	//	KEY // library marker davegut.lib_tpLink_security, line 81
	payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_security, line 82
	sessionData << [encKey: mdEncode("SHA-256", payload)[0..15]] // library marker davegut.lib_tpLink_security, line 83
	//	SIG // library marker davegut.lib_tpLink_security, line 84
	payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_security, line 85
	sessionData << [encSig: mdEncode("SHA-256", payload)[0..27]] // library marker davegut.lib_tpLink_security, line 86
	return sessionData // library marker davegut.lib_tpLink_security, line 87
} // library marker davegut.lib_tpLink_security, line 88

def klapLoginDevice(uri, localHash, localSeed, remoteSeed, cookie) { // library marker davegut.lib_tpLink_security, line 90
	Map loginData = [method: "klapLoginDevice"] // library marker davegut.lib_tpLink_security, line 91
	byte[] authHashes = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.lib_tpLink_security, line 92
	byte[] body = mdEncode("SHA-256", authHashes) // library marker davegut.lib_tpLink_security, line 93
	Map respData = klapSyncPost(uri, body, cookie) // library marker davegut.lib_tpLink_security, line 94
	def loginSuccess = false // library marker davegut.lib_tpLink_security, line 95
	if (respData.status == 200) { // library marker davegut.lib_tpLink_security, line 96
		loginSuccess = true  // library marker davegut.lib_tpLink_security, line 97
	} else { // library marker davegut.lib_tpLink_security, line 98
		LoginData << [errorData: respData] // library marker davegut.lib_tpLink_security, line 99
	} // library marker davegut.lib_tpLink_security, line 100
	loginData << [loginSuccess: loginSuccess] // library marker davegut.lib_tpLink_security, line 101
	return loginData // library marker davegut.lib_tpLink_security, line 102
} // library marker davegut.lib_tpLink_security, line 103

//	===== Legacy (AES) Handshake and Login ===== // library marker davegut.lib_tpLink_security, line 105
def aesLogin(baseUrl, encPassword, encUsername) { // library marker davegut.lib_tpLink_security, line 106
	Map logData = [method: "aesLogin"] // library marker davegut.lib_tpLink_security, line 107
	Map sessionData = [protocol: "AES"] // library marker davegut.lib_tpLink_security, line 108
	Map handshakeData = aesHandshake(baseUrl) // library marker davegut.lib_tpLink_security, line 109
	def status = "ERROR" // library marker davegut.lib_tpLink_security, line 110
	logData << [handshakeData: handshakeData] // library marker davegut.lib_tpLink_security, line 111
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 112
		byte[] encKey = handshakeData.encKey // library marker davegut.lib_tpLink_security, line 113
		byte[] encIv = handshakeData.encIv // library marker davegut.lib_tpLink_security, line 114
		def tokenData = aesLoginDevice(baseUrl, handshakeData.cookie,  // library marker davegut.lib_tpLink_security, line 115
									   encKey, encIv, // library marker davegut.lib_tpLink_security, line 116
									   encPassword, encUsername) // library marker davegut.lib_tpLink_security, line 117
		logData << [tokenData: tokenData] // library marker davegut.lib_tpLink_security, line 118
		if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 119
			sessionData << [encKey: handshakeData.encKey, // library marker davegut.lib_tpLink_security, line 120
							encIv: handshakeData.encIv, // library marker davegut.lib_tpLink_security, line 121
							token: tokenData.token, // library marker davegut.lib_tpLink_security, line 122
							cookie: handshakeData.cookie, // library marker davegut.lib_tpLink_security, line 123
						    status: "OK"] // library marker davegut.lib_tpLink_security, line 124
			status = "OK" // library marker davegut.lib_tpLink_security, line 125
		} else { // library marker davegut.lib_tpLink_security, line 126
			sessionData << [status: "ERROR"] // library marker davegut.lib_tpLink_security, line 127
		} // library marker davegut.lib_tpLink_security, line 128
	} else { // library marker davegut.lib_tpLink_security, line 129
		sessionData << [status: "ERROR"] // library marker davegut.lib_tpLink_security, line 130
	} // library marker davegut.lib_tpLink_security, line 131
	logData << [status: status] // library marker davegut.lib_tpLink_security, line 132
	if (logData.status != "OK") { // library marker davegut.lib_tpLink_security, line 133
		logInfo(logData) // library marker davegut.lib_tpLink_security, line 134
	} // library marker davegut.lib_tpLink_security, line 135
	return sessionData // library marker davegut.lib_tpLink_security, line 136
} // library marker davegut.lib_tpLink_security, line 137

def aesHandshake(baseUrl) { // library marker davegut.lib_tpLink_security, line 139
	def rsaKeys = getRsaKeys() // library marker davegut.lib_tpLink_security, line 140
	Map handshakeData = [method: "aesHandshake", rsaKeyNo: rsaKeys.keyNo] // library marker davegut.lib_tpLink_security, line 141
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKeys.public}-----END PUBLIC KEY-----\n" // library marker davegut.lib_tpLink_security, line 142
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.lib_tpLink_security, line 143
	def respStatus = "ERROR" // library marker davegut.lib_tpLink_security, line 144
	Map respData = aesSyncPost(baseUrl, cmdBody) // library marker davegut.lib_tpLink_security, line 145
	if (respData.status == 200 && respData.data != null) { // library marker davegut.lib_tpLink_security, line 146
		String deviceKey = respData.data.result.key // library marker davegut.lib_tpLink_security, line 147
		def cookieHeader = respData.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_security, line 148
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_security, line 149
		Map aesArray = aesReadDeviceKey(deviceKey, rsaKeys.private) // library marker davegut.lib_tpLink_security, line 150
		if (aesArraystatus == "ERROR") { // library marker davegut.lib_tpLink_security, line 151
			handshakeData << [check: "privateKey"] // library marker davegut.lib_tpLink_security, line 152
		} else { // library marker davegut.lib_tpLink_security, line 153
			respStatus = "OK" // library marker davegut.lib_tpLink_security, line 154
			handshakeData << [encKey: aesArray.cryptoArray[0..15],  // library marker davegut.lib_tpLink_security, line 155
							  encIv: aesArray.cryptoArray[16..31], cookie: cookie] // library marker davegut.lib_tpLink_security, line 156
		} // library marker davegut.lib_tpLink_security, line 157
	} else { // library marker davegut.lib_tpLink_security, line 158
		handshakeData << [errorData: respData] // library marker davegut.lib_tpLink_security, line 159
	} // library marker davegut.lib_tpLink_security, line 160
	handshakeData << [respStatus: respStatus] // library marker davegut.lib_tpLink_security, line 161
	return handshakeData // library marker davegut.lib_tpLink_security, line 162
} // library marker davegut.lib_tpLink_security, line 163

def aesReadDeviceKey(deviceKey, privateKey) { // library marker davegut.lib_tpLink_security, line 165
	def status = "ERROR" // library marker davegut.lib_tpLink_security, line 166
	def respData = [method: "aesReadDeviceKey"] // library marker davegut.lib_tpLink_security, line 167
	try { // library marker davegut.lib_tpLink_security, line 168
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.lib_tpLink_security, line 169
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.lib_tpLink_security, line 170
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.lib_tpLink_security, line 171
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.lib_tpLink_security, line 172
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.lib_tpLink_security, line 173
		respData << [cryptoArray: cryptoArray] // library marker davegut.lib_tpLink_security, line 174
		status = "OK" // library marker davegut.lib_tpLink_security, line 175
	} catch (err) { // library marker davegut.lib_tpLink_security, line 176
		respData << [errorData: err] // library marker davegut.lib_tpLink_security, line 177
	} // library marker davegut.lib_tpLink_security, line 178
	respData << [keyStatus: status] // library marker davegut.lib_tpLink_security, line 179
	return respData // library marker davegut.lib_tpLink_security, line 180
} // library marker davegut.lib_tpLink_security, line 181

def aesLoginDevice(uri, cookie, encKey, encIv, encPassword, encUsername) { // library marker davegut.lib_tpLink_security, line 183
	Map tokenData = [protocol: "aes"] // library marker davegut.lib_tpLink_security, line 184
	Map logData = [method: "aesLoginDevice"] // library marker davegut.lib_tpLink_security, line 185
	Map cmdBody = [method: "login_device", // library marker davegut.lib_tpLink_security, line 186
				   params: [password: encPassword, // library marker davegut.lib_tpLink_security, line 187
							username: encUsername], // library marker davegut.lib_tpLink_security, line 188
				   requestTimeMils: 0] // library marker davegut.lib_tpLink_security, line 189
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_security, line 190
	def encrString = aesEncrypt(cmdStr, encKey, encIv) // library marker davegut.lib_tpLink_security, line 191
	Map reqBody = [method: "securePassthrough", params: [request: encrString]] // library marker davegut.lib_tpLink_security, line 192
	def respData = aesSyncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_security, line 193
	if (respData.status == 200) { // library marker davegut.lib_tpLink_security, line 194
		if (respData.data.error_code == 0) { // library marker davegut.lib_tpLink_security, line 195
			try { // library marker davegut.lib_tpLink_security, line 196
				def cmdResp = aesDecrypt(respData.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_security, line 197
				cmdResp = new JsonSlurper().parseText(cmdResp) // library marker davegut.lib_tpLink_security, line 198
				if (cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_security, line 199
					tokenData << [respStatus: "OK", token: cmdResp.result.token] // library marker davegut.lib_tpLink_security, line 200
				} else { // library marker davegut.lib_tpLink_security, line 201
					tokenData << [respStatus: "ERROR", error_code: cmdResp.error_code, // library marker davegut.lib_tpLink_security, line 202
								  check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.lib_tpLink_security, line 203
				} // library marker davegut.lib_tpLink_security, line 204
			} catch (err) { // library marker davegut.lib_tpLink_security, line 205
				tokenData << [respStatus: "ERROR", error: err] // library marker davegut.lib_tpLink_security, line 206
			} // library marker davegut.lib_tpLink_security, line 207
		} else { // library marker davegut.lib_tpLink_security, line 208
			tokenData << [respStatus: "ERROR", data: respData.data] // library marker davegut.lib_tpLink_security, line 209
		} // library marker davegut.lib_tpLink_security, line 210
	} else { // library marker davegut.lib_tpLink_security, line 211
		tokenData << [respStatus: "ERROR", data: respData] // library marker davegut.lib_tpLink_security, line 212
	} // library marker davegut.lib_tpLink_security, line 213
	logData << [tokenData: tokenData] // library marker davegut.lib_tpLink_security, line 214
	if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 215
		logDebug(logData) // library marker davegut.lib_tpLink_security, line 216
	} else { // library marker davegut.lib_tpLink_security, line 217
		logWarn(logData) // library marker davegut.lib_tpLink_security, line 218
	} // library marker davegut.lib_tpLink_security, line 219
	return tokenData // library marker davegut.lib_tpLink_security, line 220
} // library marker davegut.lib_tpLink_security, line 221

//	===== Protocol specific encrytion/decryption ===== // library marker davegut.lib_tpLink_security, line 223
def klapEncrypt(byte[] request, encKey, encIv, encSig) { // library marker davegut.lib_tpLink_security, line 224
	int seqNo = state.seqNo + 1 // library marker davegut.lib_tpLink_security, line 225
	state.seqNo = seqNo // library marker davegut.lib_tpLink_security, line 226
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.lib_tpLink_security, line 227
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.lib_tpLink_security, line 228

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 230
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_security, line 231
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_security, line 232
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 233
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.lib_tpLink_security, line 234

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.lib_tpLink_security, line 236
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_security, line 237
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.lib_tpLink_security, line 238
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.lib_tpLink_security, line 239
} // library marker davegut.lib_tpLink_security, line 240

def aesEncrypt(request, encKey, encIv) { // library marker davegut.lib_tpLink_security, line 242
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 243
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_security, line 244
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_security, line 245
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 246
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.lib_tpLink_security, line 247
	return result.replace("\r\n","") // library marker davegut.lib_tpLink_security, line 248
} // library marker davegut.lib_tpLink_security, line 249

def klapDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_security, line 251
	byte[] encSeq = integerToByteArray(state.seqNo) // library marker davegut.lib_tpLink_security, line 252
	byte[] ivEnc = [encIv, encSeq].flatten() // library marker davegut.lib_tpLink_security, line 253

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 255
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_security, line 256
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_security, line 257
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 258
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.lib_tpLink_security, line 259
	return new String(byteResponse, "UTF-8") // library marker davegut.lib_tpLink_security, line 260
} // library marker davegut.lib_tpLink_security, line 261

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_security, line 263
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.lib_tpLink_security, line 264
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 265
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_security, line 266
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_security, line 267
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 268
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.lib_tpLink_security, line 269
	return result // library marker davegut.lib_tpLink_security, line 270
} // library marker davegut.lib_tpLink_security, line 271

//	===== RSA Key Methods ===== // library marker davegut.lib_tpLink_security, line 273
def getRsaKeys() { // library marker davegut.lib_tpLink_security, line 274
	def keyNo = Math.round(5 * Math.random()).toInteger() // library marker davegut.lib_tpLink_security, line 275
	def keyData = keyData() // library marker davegut.lib_tpLink_security, line 276
	def RSAKeys = keyData.find { it.keyNo == keyNo } // library marker davegut.lib_tpLink_security, line 277
	return RSAKeys // library marker davegut.lib_tpLink_security, line 278
} // library marker davegut.lib_tpLink_security, line 279

def keyData() { // library marker davegut.lib_tpLink_security, line 281
	return [ // library marker davegut.lib_tpLink_security, line 282
		[ // library marker davegut.lib_tpLink_security, line 283
			keyNo: 0, // library marker davegut.lib_tpLink_security, line 284
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.lib_tpLink_security, line 285
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw" // library marker davegut.lib_tpLink_security, line 286
		],[ // library marker davegut.lib_tpLink_security, line 287
			keyNo: 1, // library marker davegut.lib_tpLink_security, line 288
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCshy+qBKbJNefcyJUZ/3i+3KyLji6XaWEWvebUCC2r9/0jE6hc89AufO41a13E3gJ2es732vaxwZ1BZKLy468NnL+tg6vlQXaPkDcdunQwjxbTLNL/yzDZs9HRju2lJnupcksdJWBZmjtztMWQkzBrQVeSKzSTrKYK0s24EEXmtQIDAQAB", // library marker davegut.lib_tpLink_security, line 289
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKyHL6oEpsk159zIlRn/eL7crIuOLpdpYRa95tQILav3/SMTqFzz0C587jVrXcTeAnZ6zvfa9rHBnUFkovLjrw2cv62Dq+VBdo+QNx26dDCPFtMs0v/LMNmz0dGO7aUme6lySx0lYFmaO3O0xZCTMGtBV5IrNJOspgrSzbgQRea1AgMBAAECgYBSeiX9H1AkbJK1Z2ZwEUNF6vTJmmUHmScC2jHZNzeuOFVZSXJ5TU0+jBbMjtE65e9DeJ4suw6oF6j3tAZ6GwJ5tHoIy+qHRV6AjA8GEXjhSwwVCyP8jXYZ7UZyHzjLQAK+L0PvwJY1lAtns/Xmk5GH+zpNnhEmKSZAw23f7wpj2QJBANVPQGYT7TsMTDEEl2jq/ZgOX5Djf2VnKpPZYZGsUmg1hMwcpN/4XQ7XOaclR5TO/CJBJl3UCUEVjdrR1zdD8g8CQQDPDoa5Y5UfhLz4Ja2/gs2UKwO4fkTqqR6Ad8fQlaUZ55HINHWFd8FeERBFgNJzszrzd9BBJ7NnZM5nf2OPqU77AkBLuQuScSZ5HL97czbQvwLxVMDmLWyPMdVykOvLC9JhPgZ7cvuwqnlWiF7mEBzeHbBx9JDLJDd4zE8ETBPLgapPAkAHhCR52FaSdVQSwfNjr1DdHw6chODlj8wOp8p2FOiQXyqYlObrOGSpkH8BtuJs1sW+DsxdgR5vE2a2tRYdIe0/AkEAoQ5MzLcETQrmabdVCyB9pQAiHe4yY9e1w7cimsLJOrH7LMM0hqvBqFOIbSPrZyTp7Ie8awn4nTKoZQtvBfwzHw==" // library marker davegut.lib_tpLink_security, line 290
		],[ // library marker davegut.lib_tpLink_security, line 291
			keyNo: 2, // library marker davegut.lib_tpLink_security, line 292
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBeqRy4zAOs63Sc5yc0DtlFXG1stmdD6sEfUiGjlsy0S8aS8X+Qcjcu5AK3uBBrkVNIa8djXht1bd+pUof5/txzWIMJw9SNtNYqzSdeO7cCtRLzuQnQWP7Am64OBvYkXn2sUqoaqDE50LbSQWbuvZw0Vi9QihfBYGQdlrqjCPUsQIDAQAB", // library marker davegut.lib_tpLink_security, line 293
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIF6pHLjMA6zrdJznJzQO2UVcbWy2Z0PqwR9SIaOWzLRLxpLxf5ByNy7kAre4EGuRU0hrx2NeG3Vt36lSh/n+3HNYgwnD1I201irNJ147twK1EvO5CdBY/sCbrg4G9iRefaxSqhqoMTnQttJBZu69nDRWL1CKF8FgZB2WuqMI9SxAgMBAAECgYBBi2wkHI3/Y0Xi+1OUrnTivvBJIri2oW/ZXfKQ6w+PsgU+Mo2QII0l8G0Ck8DCfw3l9d9H/o2wTDgPjGzxqeXHAbxET1dS0QBTjR1zLZlFyfAs7WO8tDKmHVroUgqRkJgoQNQlBSe1E3e7pTgSKElzLuALkRS6p1jhzT2wu9U04QJBAOFr/G36PbQ6NmDYtVyEEr3vWn46JHeZISdJOsordR7Wzbt6xk6/zUDHq0OGM9rYrpBy7PNrbc0JuQrhfbIyaHMCQQCTCvETjXCMkwyUrQT6TpxVzKEVRf1rCitnNQCh1TLnDKcCEAnqZT2RRS3yNXTWFoJrtuEHMGmwUrtog9+ZJBlLAkEA2qxdkPY621XJIIO404mPgM7rMx4F+DsE7U5diHdFw2fO5brBGu13GAtZuUQ7k2W1WY0TDUO+nTN8XPDHdZDuvwJABu7TIwreLaKZS0FFJNAkCt+VEL22Dx/xn/Idz4OP3Nj53t0Guqh/WKQcYHkowxdYmt+KiJ49vXSJJYpiNoQ/NQJAM1HCl8hBznLZLQlxrCTdMvUimG3kJmA0bUNVncgUBq7ptqjk7lp5iNrle5aml99foYnzZeEUW6jrCC7Lj9tg+w==" // library marker davegut.lib_tpLink_security, line 294
		],[ // library marker davegut.lib_tpLink_security, line 295
			keyNo: 3, // library marker davegut.lib_tpLink_security, line 296
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFYaoMvv5kBxUUbp4PQyd7RoZlPompsupXP2La0qGGxacF98/88W4KNUqLbF4X5BPqxoEA+VeZy75qqyfuYbGQ4fxT6usE/LnzW8zDY/PjhVBht8FBRyAUsoYAt3Ip6sDyjd9YzRzUL1Q/OxCgxz5CNETYxcNr7zfMshBHDmZXMQIDAQAB", // library marker davegut.lib_tpLink_security, line 297
			private: "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIVhqgy+/mQHFRRung9DJ3tGhmU+iamy6lc/YtrSoYbFpwX3z/zxbgo1SotsXhfkE+rGgQD5V5nLvmqrJ+5hsZDh/FPq6wT8ufNbzMNj8+OFUGG3wUFHIBSyhgC3cinqwPKN31jNHNQvVD87EKDHPkI0RNjFw2vvN8yyEEcOZlcxAgMBAAECgYA3NxjoMeCpk+z8ClbQRqJ/e9CC9QKUB4bPG2RW5b8MRaJA7DdjpKZC/5CeavwAs+Ay3n3k41OKTTfEfJoJKtQQZnCrqnZfq9IVZI26xfYo0cgSYbi8wCie6nqIBdu9k54nqhePPshi22VcFuOh97xxPvY7kiUaRbbKqxn9PFwrYQJBAMsO3uOnYSJxN/FuxksKLqhtNei2GUC/0l7uIE8rbRdtN3QOpcC5suj7id03/IMn2Ks+Vsrmi0lV4VV/c8xyo9UCQQCoKDlObjbYeYYdW7/NvI6cEntgHygENi7b6WFk+dbRhJQgrFH8Z/Idj9a2E3BkfLCTUM1Z/Z3e7D0iqPDKBn/tAkBAHI3bKvnMOhsDq4oIH0rj+rdOplAK1YXCW0TwOjHTd7ROfGFxHDCUxvacVhTwBCCw0JnuriPEH81phTg2kOuRAkAEPR9UrsqLImUTEGEBWqNto7mgbqifko4T1QozdWjI10K0oCNg7W3Y+Os8o7jNj6cTz5GdlxsHp4TS/tczAH7xAkBY6KPIlF1FfiyJAnBC8+jJr2h4TSPQD7sbJJmYw7mvR+f1T4tsWY0aGux69hVm8BoaLStBVPdkaENBMdP+a07u" // library marker davegut.lib_tpLink_security, line 298
		],[ // library marker davegut.lib_tpLink_security, line 299
			keyNo: 4, // library marker davegut.lib_tpLink_security, line 300
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClF0yuCpo3r1ZpYlGcyI5wy5nnvZdOZmxqz5U2rklt2b8+9uWhmsGdpbTv5+qJXlZmvUKbpoaPxpJluBFDJH2GSpq3I0whh0gNq9Arzpp/TDYaZLb6iIqDMF6wm8yjGOtcSkB7qLQWkXpEN9T2NsEzlfTc+GTKc07QXHnzxoLmwQIDAQAB", // library marker davegut.lib_tpLink_security, line 301
			private: "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKUXTK4KmjevVmliUZzIjnDLmee9l05mbGrPlTauSW3Zvz725aGawZ2ltO/n6oleVma9Qpumho/GkmW4EUMkfYZKmrcjTCGHSA2r0CvOmn9MNhpktvqIioMwXrCbzKMY61xKQHuotBaRekQ31PY2wTOV9Nz4ZMpzTtBcefPGgubBAgMBAAECgYB4wCz+05RvDFk45YfqFCtTRyg//0UvO+0qxsBN6Xad2XlvlWjqJeZd53kLTGcYqJ6rsNyKOmgLu2MS8Wn24TbJmPUAwZU+9cvSPxxQ5k6bwjg1RifieIcbTPC5wHDqVy0/Ur7dt+JVMOHFseR/pElDw471LCdwWSuFHAKuiHsaUQJBANHiPdSU3s1bbJYTLaS1tW0UXo7aqgeXuJgqZ2sKsoIEheEAROJ5rW/f2KrFVtvg0ITSM8mgXNlhNBS5OE4nSD0CQQDJXYJxKvdodeRoj+RGTCZGZanAE1naUzSdfcNWx2IMnYUD/3/2eB7ZIyQPBG5fWjc3bGOJKI+gy/14bCwXU7zVAkAdnsE9HBlpf+qOL3y0jxRgpYxGuuNeGPJrPyjDOYpBwSOnwmL2V1e7vyqTxy/f7hVfeU7nuKMB5q7z8cPZe7+9AkEAl7A6aDe+wlE069OhWZdZqeRBmLC7Gi1d0FoBwahW4zvyDM32vltEmbvQGQP0hR33xGeBH7yPXcjtOz75g+UPtQJBAL4gknJ/p+yQm9RJB0oq/g+HriErpIMHwrhNoRY1aOBMJVl4ari1Ch2RQNL9KQW7yrFDv7XiP3z5NwNDKsp/QeU=" // library marker davegut.lib_tpLink_security, line 302
		],[ // library marker davegut.lib_tpLink_security, line 303
			keyNo: 5, // library marker davegut.lib_tpLink_security, line 304
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChN8Xc+gsSuhcLVM1W1E+e1o+celvKlOmuV6sJEkJecknKFujx9+T4xvyapzyePpTBn0lA9EYbaF7UDYBsDgqSwgt0El3gV+49O56nt1ELbLUJtkYEQPK+6Pu8665UG17leCiaMiFQyoZhD80PXhpjehqDu2900uU/4DzKZ/eywwIDAQAB", // library marker davegut.lib_tpLink_security, line 305
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKE3xdz6CxK6FwtUzVbUT57Wj5x6W8qU6a5XqwkSQl5yScoW6PH35PjG/JqnPJ4+lMGfSUD0RhtoXtQNgGwOCpLCC3QSXeBX7j07nqe3UQtstQm2RgRA8r7o+7zrrlQbXuV4KJoyIVDKhmEPzQ9eGmN6GoO7b3TS5T/gPMpn97LDAgMBAAECgYAy+uQCwL8HqPjoiGR2dKTI4aiAHuEv6m8KxoY7VB7QputWkHARNAaf9KykawXsNHXt1GThuV0CBbsW6z4U7UvCJEZEpv7qJiGX8UWgEs1ISatqXmiIMVosIJJvoFw/rAoScadCYyicskjwDFBVNU53EAUD3WzwEq+dRYDn52lqQQJBAMu30FEReAHTAKE/hvjAeBUyWjg7E4/lnYvb/i9Wuc+MTH0q3JxFGGMb3n6APT9+kbGE0rinM/GEXtpny+5y3asCQQDKl7eNq0NdIEBGAdKerX4O+nVDZ7PXz1kQ2ca0r1tXtY/9sBDDoKHP2fQAH/xlOLIhLaH1rabSEJYNUM0ohHdJAkBYZqhwNWtlJ0ITtvSEB0lUsWfzFLe1bseCBHH16uVwygn7GtlmupkNkO9o548seWkRpnimhnAE8xMSJY6aJ6BHAkEAuSFLKrqGJGOEWHTx8u63cxiMb7wkK+HekfdwDUzxO4U+v6RUrW/sbfPNdQ/FpPnaTVdV2RuGhg+CD0j3MT9bgQJARH86hfxp1bkyc7f1iJQT8sofdqqVz5grCV5XeGY77BNmCvTOGLfL5pOJdgALuOoP4t3e94nRYdlW6LqIVugRBQ==" // library marker davegut.lib_tpLink_security, line 306
		] // library marker davegut.lib_tpLink_security, line 307
	] // library marker davegut.lib_tpLink_security, line 308
} // library marker davegut.lib_tpLink_security, line 309

//	===== Encoding Methods ===== // library marker davegut.lib_tpLink_security, line 311
def mdEncode(hashMethod, byte[] data) { // library marker davegut.lib_tpLink_security, line 312
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.lib_tpLink_security, line 313
	md.update(data) // library marker davegut.lib_tpLink_security, line 314
	return md.digest() // library marker davegut.lib_tpLink_security, line 315
} // library marker davegut.lib_tpLink_security, line 316

String encodeUtf8(String message) { // library marker davegut.lib_tpLink_security, line 318
	byte[] arr = message.getBytes("UTF8") // library marker davegut.lib_tpLink_security, line 319
	return new String(arr) // library marker davegut.lib_tpLink_security, line 320
} // library marker davegut.lib_tpLink_security, line 321

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.lib_tpLink_security, line 323
	int arrayASInteger // library marker davegut.lib_tpLink_security, line 324
	try { // library marker davegut.lib_tpLink_security, line 325
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.lib_tpLink_security, line 326
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.lib_tpLink_security, line 327
	} catch (error) { // library marker davegut.lib_tpLink_security, line 328
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.lib_tpLink_security, line 329
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.lib_tpLink_security, line 330
	} // library marker davegut.lib_tpLink_security, line 331
	return arrayAsInteger // library marker davegut.lib_tpLink_security, line 332
} // library marker davegut.lib_tpLink_security, line 333

byte[] integerToByteArray(value) { // library marker davegut.lib_tpLink_security, line 335
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.lib_tpLink_security, line 336
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.lib_tpLink_security, line 337
	return byteValue // library marker davegut.lib_tpLink_security, line 338
} // library marker davegut.lib_tpLink_security, line 339

// ~~~~~ end include (1378) davegut.lib_tpLink_security ~~~~~

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
