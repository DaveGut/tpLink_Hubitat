/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md
=================================================================================================*/
metadata {
	definition (name: "TpLink Parent", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_parent.groovy")
	{
	}
	preferences {
		commonPreferences()
		input ("installChild", "bool", title: "Install Child Devices", defaultValue: true)
	}
}

def installed() {
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	logInfo(logData)
}

def updated() { 
	Map logData = [method: updated, installChild: installChild,
				   commonUpdated: commonUpdated()]
	if (installChild) {
		runIn(5, installChildDevices)
		pauseExecution(5000)
	}
	logInfo(logData)
}

def parse_get_device_info(result, data) {
	Map logData = [method: "parse_get_device_info", data: data]
	logDebug(logData)
}

//	===== Include Libraries =====










// ~~~~~ start include (380) davegut.tpLinkCapEngMon ~~~~~
library ( // library marker davegut.tpLinkCapEngMon, line 1
	name: "tpLinkCapEngMon", // library marker davegut.tpLinkCapEngMon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapEngMon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapEngMon, line 4
	description: "Hubitat Energy Monitor methods", // library marker davegut.tpLinkCapEngMon, line 5
	category: "utilities", // library marker davegut.tpLinkCapEngMon, line 6
	documentationLink: "" // library marker davegut.tpLinkCapEngMon, line 7
) // library marker davegut.tpLinkCapEngMon, line 8

capability "EnergyMeter" // library marker davegut.tpLinkCapEngMon, line 10
capability "PowerMeter" // library marker davegut.tpLinkCapEngMon, line 11
attribute "past30Energy", "number" // library marker davegut.tpLinkCapEngMon, line 12
attribute "past7Energy", "number" // library marker davegut.tpLinkCapEngMon, line 13

def emUpdated() { // library marker davegut.tpLinkCapEngMon, line 15
	Map logData = [emDataUpdate: "30 mins"] // library marker davegut.tpLinkCapEngMon, line 16
	runEvery30Minutes(getEmData) // library marker davegut.tpLinkCapEngMon, line 17
	getEmData() // library marker davegut.tpLinkCapEngMon, line 18
	return logData // library marker davegut.tpLinkCapEngMon, line 19
} // library marker davegut.tpLinkCapEngMon, line 20

def powerPoll() { // library marker davegut.tpLinkCapEngMon, line 22
	List requests = [[method: "get_current_power"]] // library marker davegut.tpLinkCapEngMon, line 23
	sendDevCmd(requests, "powerPoll", "parseUpdates") // library marker davegut.tpLinkCapEngMon, line 24
} // library marker davegut.tpLinkCapEngMon, line 25

def parse_get_current_power(result, data) { // library marker davegut.tpLinkCapEngMon, line 27
	Map logData = [method: "parse_get_current_power", data: data, // library marker davegut.tpLinkCapEngMon, line 28
				   power: result.current_power] // library marker davegut.tpLinkCapEngMon, line 29
	updateAttr("power", result.current_power) // library marker davegut.tpLinkCapEngMon, line 30
	logDebug(logData) // library marker davegut.tpLinkCapEngMon, line 31
} // library marker davegut.tpLinkCapEngMon, line 32

def getEmData() { // library marker davegut.tpLinkCapEngMon, line 34
	List requests = [[method: "get_device_usage"]] // library marker davegut.tpLinkCapEngMon, line 35
	sendDevCmd(requests, "getEmData", "parseUpdates") // library marker davegut.tpLinkCapEngMon, line 36
} // library marker davegut.tpLinkCapEngMon, line 37

def parse_get_device_usage(result, data) { // library marker davegut.tpLinkCapEngMon, line 39
	Map logData = [method: "parse_get_device_usage", data: data] // library marker davegut.tpLinkCapEngMon, line 40
	def usage = result.power_usage // library marker davegut.tpLinkCapEngMon, line 41
	updateAttr("energy", usage.today) // library marker davegut.tpLinkCapEngMon, line 42
	updateAttr("past30Energy", usage.past30) // library marker davegut.tpLinkCapEngMon, line 43
	updateAttr("past7Energy", usage.past7) // library marker davegut.tpLinkCapEngMon, line 44
	logData << [energy:usage.today, past30Energy: usage.past30, past7Energy: usage.past7] // library marker davegut.tpLinkCapEngMon, line 45
	logDebug(logData) // library marker davegut.tpLinkCapEngMon, line 46
} // library marker davegut.tpLinkCapEngMon, line 47

// ~~~~~ end include (380) davegut.tpLinkCapEngMon ~~~~~

// ~~~~~ start include (392) davegut.tpLinkCapConfiguration ~~~~~
library ( // library marker davegut.tpLinkCapConfiguration, line 1
	name: "tpLinkCapConfiguration", // library marker davegut.tpLinkCapConfiguration, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapConfiguration, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapConfiguration, line 4
	description: "Hubitat capability Configuration.", // library marker davegut.tpLinkCapConfiguration, line 5
	category: "utilities", // library marker davegut.tpLinkCapConfiguration, line 6
	documentationLink: "" // library marker davegut.tpLinkCapConfiguration, line 7
)  // library marker davegut.tpLinkCapConfiguration, line 8

capability "Configuration" // library marker davegut.tpLinkCapConfiguration, line 10

def configure() { // library marker davegut.tpLinkCapConfiguration, line 12
	String devIp = getDataValue("devIp") // library marker davegut.tpLinkCapConfiguration, line 13
	Map logData = [method: "configure", devIp: devIp] // library marker davegut.tpLinkCapConfiguration, line 14
	def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.tpLinkCapConfiguration, line 15
	try { // library marker davegut.tpLinkCapConfiguration, line 16
		sendFindCmd(devIp, "20002", cmdData, "configure2", timeout) // library marker davegut.tpLinkCapConfiguration, line 17
		logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 18
	} catch (err) { // library marker davegut.tpLinkCapConfiguration, line 19
		def parentChecked = parent.tpLinkCheckForDevices(5) // library marker davegut.tpLinkCapConfiguration, line 20
		logData << [status: "FAILED", error: err, parentChecked: parentChecked] // library marker davegut.tpLinkCapConfiguration, line 21
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 22
		configure3() // library marker davegut.tpLinkCapConfiguration, line 23
	} // library marker davegut.tpLinkCapConfiguration, line 24
} // library marker davegut.tpLinkCapConfiguration, line 25

def configure2(response) { // library marker davegut.tpLinkCapConfiguration, line 27
	Map logData = [method: "configure2"] // library marker davegut.tpLinkCapConfiguration, line 28
	def respData = parseLanMessage(response) // library marker davegut.tpLinkCapConfiguration, line 29
	String hubDni = device.getDeviceNetworkId() // library marker davegut.tpLinkCapConfiguration, line 30
	logData << [dni: respData.mac, hubDni: hubDni] // library marker davegut.tpLinkCapConfiguration, line 31
	def parentChecked = false // library marker davegut.tpLinkCapConfiguration, line 32
	if (respData.mac != hubDni) { // library marker davegut.tpLinkCapConfiguration, line 33
		logData << [status: "device/ip not found", action: "parentCheck", // library marker davegut.tpLinkCapConfiguration, line 34
				    parentChecked: parent.tpLinkCheckForDevices(5)] // library marker davegut.tpLinkCapConfiguration, line 35
	} else { // library marker davegut.tpLinkCapConfiguration, line 36
		logData << [status: "device/ip found"] // library marker davegut.tpLinkCapConfiguration, line 37
	} // library marker davegut.tpLinkCapConfiguration, line 38
	configure3() // library marker davegut.tpLinkCapConfiguration, line 39
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 40
} // library marker davegut.tpLinkCapConfiguration, line 41
def configure3() { // library marker davegut.tpLinkCapConfiguration, line 42
	Map logData = [method: "configure3"] // library marker davegut.tpLinkCapConfiguration, line 43
	logData <<[updateDeviceData: updateDeviceData(true)] // library marker davegut.tpLinkCapConfiguration, line 44
	logData << [deviceHandshake: deviceHandshake()] // library marker davegut.tpLinkCapConfiguration, line 45
	runEvery3Hours("deviceHandshake") // library marker davegut.tpLinkCapConfiguration, line 46
	logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCapConfiguration, line 47
	runIn(5, refresh) // library marker davegut.tpLinkCapConfiguration, line 48
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 49
} // library marker davegut.tpLinkCapConfiguration, line 50

def deviceHandshake() { // library marker davegut.tpLinkCapConfiguration, line 52
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkCapConfiguration, line 53
	Map logData = [method: "deviceHandshake", protocol: protocol] // library marker davegut.tpLinkCapConfiguration, line 54
	if (protocol == "KLAP") { // library marker davegut.tpLinkCapConfiguration, line 55
		klapHandshake(getDataValue("baseUrl"), parent.localHash) // library marker davegut.tpLinkCapConfiguration, line 56
	} else if (protocol == "camera") { // library marker davegut.tpLinkCapConfiguration, line 57
		Map hsInput = [url: getDataValue("baseUrl"), user: parent.userName, // library marker davegut.tpLinkCapConfiguration, line 58
					   pwd: parent.encPasswordCam] // library marker davegut.tpLinkCapConfiguration, line 59
		cameraHandshake(hsInput) // library marker davegut.tpLinkCapConfiguration, line 60
	} else if (protocol == "AES") { // library marker davegut.tpLinkCapConfiguration, line 61
		aesHandshake() // library marker davegut.tpLinkCapConfiguration, line 62
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkCapConfiguration, line 63
		vacAesHandshake(getDataValue("baseUrl"), parent.userName, parent.encPasswordVac) // library marker davegut.tpLinkCapConfiguration, line 64
	} else { // library marker davegut.tpLinkCapConfiguration, line 65
		logData << [ERROR: "Protocol not supported"] // library marker davegut.tpLinkCapConfiguration, line 66
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 67
	} // library marker davegut.tpLinkCapConfiguration, line 68
	return logData // library marker davegut.tpLinkCapConfiguration, line 69
} // library marker davegut.tpLinkCapConfiguration, line 70

// ~~~~~ end include (392) davegut.tpLinkCapConfiguration ~~~~~

// ~~~~~ start include (384) davegut.tpLinkCommon ~~~~~
library ( // library marker davegut.tpLinkCommon, line 1
	name: "tpLinkCommon", // library marker davegut.tpLinkCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCommon, line 4
	description: "Common driver methods including capability Refresh and Configuration methods", // library marker davegut.tpLinkCommon, line 5
	category: "utilities", // library marker davegut.tpLinkCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkCommon, line 7
) // library marker davegut.tpLinkCommon, line 8

capability "Refresh" // library marker davegut.tpLinkCommon, line 10
attribute "commsError", "string" // library marker davegut.tpLinkCommon, line 11

def commonPreferences() { // library marker davegut.tpLinkCommon, line 13
	List pollOptions = ["5 sec", "10 sec", "1 min", "5 min", "15 min", "30 min"] // library marker davegut.tpLinkCommon, line 14
	input ("pollInterval", "enum", title: "Poll Interval", // library marker davegut.tpLinkCommon, line 15
		   options: pollOptions, defaultValue: "30 min") // library marker davegut.tpLinkCommon, line 16
	if (getDataValue("hasLed") == "true") { // library marker davegut.tpLinkCommon, line 17
		input ("ledRule", "enum", title: "LED Mode (if night mode, set type and times in phone app)", // library marker davegut.tpLinkCommon, line 18
			   options: ["always", "never", "night_mode"], defaultValue: "always") // library marker davegut.tpLinkCommon, line 19
	} // library marker davegut.tpLinkCommon, line 20
	input ("rebootDev", "bool", title: "Reboot Device", defaultValue: false) // library marker davegut.tpLinkCommon, line 21
	input ("syncName", "enum", title: "Update Device Names and Labels",  // library marker davegut.tpLinkCommon, line 22
		   options: ["hubMaster", "tapoAppMaster", "notSet"], defaultValue: "notSet") // library marker davegut.tpLinkCommon, line 23
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkCommon, line 24
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkCommon, line 25
} // library marker davegut.tpLinkCommon, line 26

def commonInstalled() { // library marker davegut.tpLinkCommon, line 28
	Map logData = [method: "commonInstalled"] // library marker davegut.tpLinkCommon, line 29
	updateAttr("commsError", "false") // library marker davegut.tpLinkCommon, line 30
	state.errorCount = 0 // library marker davegut.tpLinkCommon, line 31
	logData << [configure: configure()] // library marker davegut.tpLinkCommon, line 32
	return logData // library marker davegut.tpLinkCommon, line 33
} // library marker davegut.tpLinkCommon, line 34

def commonUpdated() { // library marker davegut.tpLinkCommon, line 36
	unschedule() // library marker davegut.tpLinkCommon, line 37
	sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkCommon, line 38
	state.errorCount = 0 // library marker davegut.tpLinkCommon, line 39
	Map logData = [commsError: "cleared"] // library marker davegut.tpLinkCommon, line 40
	if (rebootDev == true) { // library marker davegut.tpLinkCommon, line 41
		List requests = [[method: "device_reboot"]] // library marker davegut.tpLinkCommon, line 42
		sendDevCmd(requests, "rebootDevice", "parseUpdates")  // library marker davegut.tpLinkCommon, line 43
		logData << [rebootDevice: "device reboot being attempted"] // library marker davegut.tpLinkCommon, line 44
	} else { // library marker davegut.tpLinkCommon, line 45
		runEvery3Hours(deviceHandshake) // library marker davegut.tpLinkCommon, line 46
		logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCommon, line 47
		logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCommon, line 48
		logData << [logging: setLogsOff()] // library marker davegut.tpLinkCommon, line 49
		logData << [updateDevSettings: updDevSettings()] // library marker davegut.tpLinkCommon, line 50
		runIn(2, refresh) // library marker davegut.tpLinkCommon, line 51
		if (getDataValue("isEm") == "true") { // library marker davegut.tpLinkCommon, line 52
			runIn(7, emUpdated) // library marker davegut.tpLinkCommon, line 53
		} // library marker davegut.tpLinkCommon, line 54
	} // library marker davegut.tpLinkCommon, line 55
	return logData // library marker davegut.tpLinkCommon, line 56
} // library marker davegut.tpLinkCommon, line 57

def finishReboot(respData) { // library marker davegut.tpLinkCommon, line 59
	Map logData = [method: "finishReboot", respData: respData] // library marker davegut.tpLinkCommon, line 60
	logData << [wait: "<b>20s for device to reconnect to LAN</b>", action: "executing deviceHandshake"] // library marker davegut.tpLinkCommon, line 61
	device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.tpLinkCommon, line 62
	runIn(20, configure) // library marker davegut.tpLinkCommon, line 63
	logDebug(logData) // library marker davegut.tpLinkCommon, line 64
} // library marker davegut.tpLinkCommon, line 65

def updDevSettings() { // library marker davegut.tpLinkCommon, line 67
	List requests = [] // library marker davegut.tpLinkCommon, line 68
	if (syncName == "hubMaster") { // library marker davegut.tpLinkCommon, line 69
		String nickname = device.getLabel().bytes.encodeBase64().toString() // library marker davegut.tpLinkCommon, line 70
		requests << [method: "set_device_info", params: [nickname: nickname]] // library marker davegut.tpLinkCommon, line 71
	} // library marker davegut.tpLinkCommon, line 72
	if (ledRule) { // library marker davegut.tpLinkCommon, line 73
		requests << [method: "get_led_info"] // library marker davegut.tpLinkCommon, line 74
	} // library marker davegut.tpLinkCommon, line 75
	if (getDataValue("isEm") == "true") { // library marker davegut.tpLinkCommon, line 76
		requests << [method: "get_energy_usage"] // library marker davegut.tpLinkCommon, line 77
	} // library marker davegut.tpLinkCommon, line 78
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCommon, line 79
	sendDevCmd(requests, "updateDevSettings", "parseUpdates") // library marker davegut.tpLinkCommon, line 80
	return "Updated" // library marker davegut.tpLinkCommon, line 81
} // library marker davegut.tpLinkCommon, line 82

def setPollInterval(pInterval = pollInterval) { // library marker davegut.tpLinkCommon, line 84
	if (pInterval.contains("sec")) { // library marker davegut.tpLinkCommon, line 85
		logWarn("<b>Poll intervals of less than 1 minute may overload the Hub</b>") // library marker davegut.tpLinkCommon, line 86
		def interval = pInterval.replace(" sec", "").toInteger() // library marker davegut.tpLinkCommon, line 87
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 88
		schedule("${start}/${interval} * * * * ?", "refresh") // library marker davegut.tpLinkCommon, line 89
	} else { // library marker davegut.tpLinkCommon, line 90
		def interval= pInterval.replace(" min", "").toInteger() // library marker davegut.tpLinkCommon, line 91
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 92
		schedule("${start} */${interval} * * * ?", "refresh") // library marker davegut.tpLinkCommon, line 93
	} // library marker davegut.tpLinkCommon, line 94
	return pInterval // library marker davegut.tpLinkCommon, line 95
} // library marker davegut.tpLinkCommon, line 96

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkCommon, line 98
def parseUpdates(resp, data = null) { // library marker davegut.tpLinkCommon, line 99
	Map logData = [method: "parseUpdates", data: data] // library marker davegut.tpLinkCommon, line 100
	def respData = parseData(resp, getDataValue("protocol"), data) // library marker davegut.tpLinkCommon, line 101
	if (resp.status == 200 && respData.cryptoStatus == "OK") { // library marker davegut.tpLinkCommon, line 102
		def cmdResp = respData.cmdResp.result.responses // library marker davegut.tpLinkCommon, line 103
		if (respData.cmdResp.result.responses != null) { // library marker davegut.tpLinkCommon, line 104
			respData.cmdResp.result.responses.each { // library marker davegut.tpLinkCommon, line 105
				if (it.error_code == 0) { // library marker davegut.tpLinkCommon, line 106
					distGetData(it, data) // library marker davegut.tpLinkCommon, line 107
				} else { // library marker davegut.tpLinkCommon, line 108
					logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCommon, line 109
					logDebug(logData) // library marker davegut.tpLinkCommon, line 110
				} // library marker davegut.tpLinkCommon, line 111
			} // library marker davegut.tpLinkCommon, line 112
		} // library marker davegut.tpLinkCommon, line 113
		if (respData.cmdResp.result.responseData != null) { // library marker davegut.tpLinkCommon, line 114
			respData.cmdResp.result.responseData.result.responses.each { // library marker davegut.tpLinkCommon, line 115
				if (it.error_code == 0) { // library marker davegut.tpLinkCommon, line 116
					distChildGetData(it, data) // library marker davegut.tpLinkCommon, line 117
				} else { // library marker davegut.tpLinkCommon, line 118
					logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCommon, line 119
					logDebug(logData) // library marker davegut.tpLinkCommon, line 120
				} // library marker davegut.tpLinkCommon, line 121
			} // library marker davegut.tpLinkCommon, line 122
		} // library marker davegut.tpLinkCommon, line 123
	} else { // library marker davegut.tpLinkCommon, line 124
		logData << [errorMsg: "Misc Error"] // library marker davegut.tpLinkCommon, line 125
		logDebug(logData) // library marker davegut.tpLinkCommon, line 126
	} // library marker davegut.tpLinkCommon, line 127
} // library marker davegut.tpLinkCommon, line 128

def distGetData(devResp, data) { // library marker davegut.tpLinkCommon, line 130
	switch(devResp.method) { // library marker davegut.tpLinkCommon, line 131
		case "get_device_info": // library marker davegut.tpLinkCommon, line 132
			parse_get_device_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 133
			parseNameUpdate(devResp.result) // library marker davegut.tpLinkCommon, line 134
			break // library marker davegut.tpLinkCommon, line 135
		case "get_current_power": // library marker davegut.tpLinkCommon, line 136
			parse_get_current_power(devResp.result, data) // library marker davegut.tpLinkCommon, line 137
			break // library marker davegut.tpLinkCommon, line 138
		case "get_device_usage": // library marker davegut.tpLinkCommon, line 139
			parse_get_device_usage(devResp.result, data) // library marker davegut.tpLinkCommon, line 140
			break // library marker davegut.tpLinkCommon, line 141
		case "get_child_device_list": // library marker davegut.tpLinkCommon, line 142
			parse_get_child_device_list(devResp.result, data) // library marker davegut.tpLinkCommon, line 143
			break // library marker davegut.tpLinkCommon, line 144
		case "get_alarm_configure": // library marker davegut.tpLinkCommon, line 145
			parse_get_alarm_configure(devResp.result, data) // library marker davegut.tpLinkCommon, line 146
			break // library marker davegut.tpLinkCommon, line 147
		case "get_led_info": // library marker davegut.tpLinkCommon, line 148
			parse_get_led_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 149
			break // library marker davegut.tpLinkCommon, line 150
		case "device_reboot": // library marker davegut.tpLinkCommon, line 151
			finishReboot(devResp) // library marker davegut.tpLinkCommon, line 152
			break // library marker davegut.tpLinkCommon, line 153
		//	RoboVac // library marker davegut.tpLinkCommon, line 154
		case "getBatteryInfo": // library marker davegut.tpLinkCommon, line 155
			parse_getBatteryInfo(devResp.result, data) // library marker davegut.tpLinkCommon, line 156
			break // library marker davegut.tpLinkCommon, line 157
		case "getCleanNumber": // library marker davegut.tpLinkCommon, line 158
			parse_getCleanNumber(devResp.result, data) // library marker davegut.tpLinkCommon, line 159
			break // library marker davegut.tpLinkCommon, line 160
		case "getSwitchClean": // library marker davegut.tpLinkCommon, line 161
			parse_getSwitchClean(devResp, data) // library marker davegut.tpLinkCommon, line 162
			break // library marker davegut.tpLinkCommon, line 163
		case "getMopState": // library marker davegut.tpLinkCommon, line 164
			parse_getMopState(devResp, data) // library marker davegut.tpLinkCommon, line 165
			break // library marker davegut.tpLinkCommon, line 166
		case "getSwitchCharge": // library marker davegut.tpLinkCommon, line 167
			updateAttr("docking", devResp.switch_charge, data) // library marker davegut.tpLinkCommon, line 168
			break // library marker davegut.tpLinkCommon, line 169
		case "getVacStatus": // library marker davegut.tpLinkCommon, line 170
			parse_getVacStatus(devResp.result, data) // library marker davegut.tpLinkCommon, line 171
			break // library marker davegut.tpLinkCommon, line 172
		case "getMapInfo": // library marker davegut.tpLinkCommon, line 173
			parse_getMapInfo(devResp.result, data) // library marker davegut.tpLinkCommon, line 174
			break // library marker davegut.tpLinkCommon, line 175
		case "getMapData": // library marker davegut.tpLinkCommon, line 176
			parse_getMapData(devResp.result, data) // library marker davegut.tpLinkCommon, line 177
			break // library marker davegut.tpLinkCommon, line 178
		case "getLastAlarmInfo":  // library marker davegut.tpLinkCommon, line 179
			parse_getLastAlarmInfo(devResp.result, data) // library marker davegut.tpLinkCommon, line 180
			break // library marker davegut.tpLinkCommon, line 181
		default: // library marker davegut.tpLinkCommon, line 182
			if (!devResp.method.contains("set_")) { // library marker davegut.tpLinkCommon, line 183
				Map logData = [method: "distGetData", data: data, // library marker davegut.tpLinkCommon, line 184
							   devMethod: devResp.method, status: "unprocessed"] // library marker davegut.tpLinkCommon, line 185
				logDebug(logData) // library marker davegut.tpLinkCommon, line 186
			} // library marker davegut.tpLinkCommon, line 187
	} // library marker davegut.tpLinkCommon, line 188
} // library marker davegut.tpLinkCommon, line 189

def parse_get_led_info(result, data) { // library marker davegut.tpLinkCommon, line 191
	Map logData = [method: "parse_get_led_info", data: data] // library marker davegut.tpLinkCommon, line 192
	if (ledRule != result.led_rule) { // library marker davegut.tpLinkCommon, line 193
		Map request = [ // library marker davegut.tpLinkCommon, line 194
			method: "set_led_info", // library marker davegut.tpLinkCommon, line 195
			params: [ // library marker davegut.tpLinkCommon, line 196
				led_rule: ledRule, // library marker davegut.tpLinkCommon, line 197
				night_mode: [ // library marker davegut.tpLinkCommon, line 198
					night_mode_type: result.night_mode.night_mode_type, // library marker davegut.tpLinkCommon, line 199
					sunrise_offset: result.night_mode.sunrise_offset,  // library marker davegut.tpLinkCommon, line 200
					sunset_offset:result.night_mode.sunset_offset, // library marker davegut.tpLinkCommon, line 201
					start_time: result.night_mode.start_time, // library marker davegut.tpLinkCommon, line 202
					end_time: result.night_mode.end_time // library marker davegut.tpLinkCommon, line 203
				]]] // library marker davegut.tpLinkCommon, line 204
		asyncSend(request, "delayedUpdates", "parseUpdates") // library marker davegut.tpLinkCommon, line 205
		device.updateSetting("ledRule", [type:"enum", value: ledRule]) // library marker davegut.tpLinkCommon, line 206
		logData << [status: "updatingLedRule"] // library marker davegut.tpLinkCommon, line 207
	} // library marker davegut.tpLinkCommon, line 208
	logData << [ledRule: ledRule] // library marker davegut.tpLinkCommon, line 209
	logDebug(logData) // library marker davegut.tpLinkCommon, line 210
} // library marker davegut.tpLinkCommon, line 211

def parseNameUpdate(result) { // library marker davegut.tpLinkCommon, line 213
	if (syncName != "notSet") { // library marker davegut.tpLinkCommon, line 214
		Map logData = [method: "parseNameUpdate"] // library marker davegut.tpLinkCommon, line 215
		byte[] plainBytes = result.nickname.decodeBase64() // library marker davegut.tpLinkCommon, line 216
		def newLabel = new String(plainBytes) // library marker davegut.tpLinkCommon, line 217
		device.setLabel(newLabel) // library marker davegut.tpLinkCommon, line 218
		device.updateSetting("syncName",[type:"enum", value: "notSet"]) // library marker davegut.tpLinkCommon, line 219
		logData << [label: newLabel] // library marker davegut.tpLinkCommon, line 220
		logDebug(logData) // library marker davegut.tpLinkCommon, line 221
	} // library marker davegut.tpLinkCommon, line 222
} // library marker davegut.tpLinkCommon, line 223

//	===== Capability Refresh ===== // library marker davegut.tpLinkCommon, line 225
def refresh() { // library marker davegut.tpLinkCommon, line 226
	def type = getDataValue("type") // library marker davegut.tpLinkCommon, line 227
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 228
	if (type == "Hub" || type == "Parent") { // library marker davegut.tpLinkCommon, line 229
		requests << [method:"get_child_device_list"] // library marker davegut.tpLinkCommon, line 230
	} // library marker davegut.tpLinkCommon, line 231
	if (getDataValue("isEm") == "true") { // library marker davegut.tpLinkCommon, line 232
		requests << [method: "get_current_power"] // library marker davegut.tpLinkCommon, line 233
	} // library marker davegut.tpLinkCommon, line 234
	if (type == "Robovac") { // library marker davegut.tpLinkCommon, line 235
		requests = [[method: "getBatteryInfo"], // library marker davegut.tpLinkCommon, line 236
					[method: "getCleanNumber"], // library marker davegut.tpLinkCommon, line 237
					[method: "getSwitchClean"], // library marker davegut.tpLinkCommon, line 238
					[method: "getVacStatus"], // library marker davegut.tpLinkCommon, line 239
					[method: "getMopState"], // library marker davegut.tpLinkCommon, line 240
					[method: "getSwitchCharge"]] // library marker davegut.tpLinkCommon, line 241
	} // library marker davegut.tpLinkCommon, line 242
	sendDevCmd(requests, "refresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 243
} // library marker davegut.tpLinkCommon, line 244

def plugEmRefresh() { refresh() } // library marker davegut.tpLinkCommon, line 246
def parentRefresh() { refresh() } // library marker davegut.tpLinkCommon, line 247
def minRefresh() { refresh() } // library marker davegut.tpLinkCommon, line 248

def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkCommon, line 250
	Map cmdBody = [ // library marker davegut.tpLinkCommon, line 251
		method: "multipleRequest", // library marker davegut.tpLinkCommon, line 252
		params: [requests: requests]] // library marker davegut.tpLinkCommon, line 253
	asyncSend(cmdBody, data, action) // library marker davegut.tpLinkCommon, line 254
} // library marker davegut.tpLinkCommon, line 255

def nullParse(resp, data) { } // library marker davegut.tpLinkCommon, line 257

//	===== Check/Update device data ===== // library marker davegut.tpLinkCommon, line 259
def updateDeviceData(fromConfig = false) { // library marker davegut.tpLinkCommon, line 260
	def devData = parent.getDeviceData(device.getDeviceNetworkId()) // library marker davegut.tpLinkCommon, line 261
	updateChild(devData, fromConfig) // library marker davegut.tpLinkCommon, line 262
	return [updateDeviceData: "updating with app data"] // library marker davegut.tpLinkCommon, line 263
} // library marker davegut.tpLinkCommon, line 264

def updateChild(devData, fromConfig = false) { // library marker davegut.tpLinkCommon, line 266
	def currVersion = getDataValue("version") // library marker davegut.tpLinkCommon, line 267
	Map logData = [method: "updateChild", devData: devData] // library marker davegut.tpLinkCommon, line 268
	if (devData != null) { // library marker davegut.tpLinkCommon, line 269
		devData.each { // library marker davegut.tpLinkCommon, line 270
			if (it.key != "deviceType" && it.key != "model" && it.key != "alias") { // library marker davegut.tpLinkCommon, line 271
				updateDataValue(it.key, it.value.toString()) // library marker davegut.tpLinkCommon, line 272
			} // library marker davegut.tpLinkCommon, line 273
		} // library marker davegut.tpLinkCommon, line 274
		if (currVersion != version()) { // library marker davegut.tpLinkCommon, line 275
			updateDataValue("version", version()) // library marker davegut.tpLinkCommon, line 276
			logData << [updateVersion: version()] // library marker davegut.tpLinkCommon, line 277
			runIn(20, updated) // library marker davegut.tpLinkCommon, line 278
		} // library marker davegut.tpLinkCommon, line 279
	} else { // library marker davegut.tpLinkCommon, line 280
		logData << [Note: "DEVICE DATA IS NULL"] // library marker davegut.tpLinkCommon, line 281
	} // library marker davegut.tpLinkCommon, line 282
	if (!fromConfig) { deviceHandshake() } // library marker davegut.tpLinkCommon, line 283
	logInfo(logData) // library marker davegut.tpLinkCommon, line 284
} // library marker davegut.tpLinkCommon, line 285

// ~~~~~ end include (384) davegut.tpLinkCommon ~~~~~

// ~~~~~ start include (387) davegut.tpLinkParentCommon ~~~~~
library ( // library marker davegut.tpLinkParentCommon, line 1
	name: "tpLinkParentCommon", // library marker davegut.tpLinkParentCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkParentCommon, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkParentCommon, line 4
	description: "Parent common methods", // library marker davegut.tpLinkParentCommon, line 5
	category: "utilities", // library marker davegut.tpLinkParentCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkParentCommon, line 7
) // library marker davegut.tpLinkParentCommon, line 8

def installChildDevices() { // library marker davegut.tpLinkParentCommon, line 10
	Map request = [method: "get_child_device_list"] // library marker davegut.tpLinkParentCommon, line 11
	asyncSend(request, "installChildDevices", "installChildren") // library marker davegut.tpLinkParentCommon, line 12
} // library marker davegut.tpLinkParentCommon, line 13

def installChildren(resp, data=null) { // library marker davegut.tpLinkParentCommon, line 15
	Map logData = [method: "installChildren", currentChildren: getChildDevices()] // library marker davegut.tpLinkParentCommon, line 16
	logInfo(logData) // library marker davegut.tpLinkParentCommon, line 17
	def respData = parseData(resp, getDataValue("protocol"), data) // library marker davegut.tpLinkParentCommon, line 18
	if (respData.cmdResp != null) { // library marker davegut.tpLinkParentCommon, line 19
		def children = respData.cmdResp.result.child_device_list // library marker davegut.tpLinkParentCommon, line 20
		Integer position = 0 // library marker davegut.tpLinkParentCommon, line 21
		children.each { // library marker davegut.tpLinkParentCommon, line 22
			position += 1 // library marker davegut.tpLinkParentCommon, line 23
			String  childDni = "${it.mac}-${position.toString()}" // library marker davegut.tpLinkParentCommon, line 24
			def isChild = getChildDevice(childDni) // library marker davegut.tpLinkParentCommon, line 25
			byte[] plainBytes = it.nickname.decodeBase64() // library marker davegut.tpLinkParentCommon, line 26
			String alias = new String(plainBytes) // library marker davegut.tpLinkParentCommon, line 27
			Map instData = [alias: alias, childDni: childDni] // library marker davegut.tpLinkParentCommon, line 28
			if (isChild) { // library marker davegut.tpLinkParentCommon, line 29
				instData << [status: "device already installed"] // library marker davegut.tpLinkParentCommon, line 30
				logInfo(instData) // library marker davegut.tpLinkParentCommon, line 31
			} else { // library marker davegut.tpLinkParentCommon, line 32
				def isEm = false // library marker davegut.tpLinkParentCommon, line 33
				if (it.protection_power != null) { // library marker davegut.tpLinkParentCommon, line 34
					isEm = true // library marker davegut.tpLinkParentCommon, line 35
				} // library marker davegut.tpLinkParentCommon, line 36
				String devType = getDeviceType(it.category) // library marker davegut.tpLinkParentCommon, line 37
				instData << [label: alias, name: it.model, type: devType, deviceId:  // library marker davegut.tpLinkParentCommon, line 38
							 it.device_id, category: it.category] // library marker davegut.tpLinkParentCommon, line 39
				if (devType == "Child Undefined") { // library marker davegut.tpLinkParentCommon, line 40
					instData << [status: "notInstalled", error: "Currently Unsupported"] // library marker davegut.tpLinkParentCommon, line 41
					logInfo(instData) // library marker davegut.tpLinkParentCommon, line 42
				} else { // library marker davegut.tpLinkParentCommon, line 43
					try { // library marker davegut.tpLinkParentCommon, line 44
						addChildDevice( // library marker davegut.tpLinkParentCommon, line 45
							nameSpace(),  // library marker davegut.tpLinkParentCommon, line 46
							"TpLink ${devType}", // library marker davegut.tpLinkParentCommon, line 47
							childDni, // library marker davegut.tpLinkParentCommon, line 48
							[ // library marker davegut.tpLinkParentCommon, line 49
								"label": alias, // library marker davegut.tpLinkParentCommon, line 50
								"name": it.model, // library marker davegut.tpLinkParentCommon, line 51
								category: it.category, // library marker davegut.tpLinkParentCommon, line 52
								deviceId: it.device_id, // library marker davegut.tpLinkParentCommon, line 53
								type: devType, // library marker davegut.tpLinkParentCommon, line 54
								isEm: isEm // library marker davegut.tpLinkParentCommon, line 55
							]) // library marker davegut.tpLinkParentCommon, line 56
						instData << [status: "Installed"] // library marker davegut.tpLinkParentCommon, line 57
						logInfo(instData) // library marker davegut.tpLinkParentCommon, line 58
					} catch (err) { // library marker davegut.tpLinkParentCommon, line 59
						instData << [status: "FAILED", error: err, driverNotInstalled: "TpLink ${devType}"] // library marker davegut.tpLinkParentCommon, line 60
						logWarn(instData) // library marker davegut.tpLinkParentCommon, line 61
						logWarn("\n\r<b>Driver TpLink ${devType} likely not installed.\n\r") // library marker davegut.tpLinkParentCommon, line 62
					} // library marker davegut.tpLinkParentCommon, line 63
				} // library marker davegut.tpLinkParentCommon, line 64
			} // library marker davegut.tpLinkParentCommon, line 65
		} // library marker davegut.tpLinkParentCommon, line 66
		device.updateSetting("installChild", [type: "bool", value: "false"]) // library marker davegut.tpLinkParentCommon, line 67
	} // library marker davegut.tpLinkParentCommon, line 68
} // library marker davegut.tpLinkParentCommon, line 69

def getDeviceType(category) { // library marker davegut.tpLinkParentCommon, line 71
	String deviceType // library marker davegut.tpLinkParentCommon, line 72
	switch(category) { // library marker davegut.tpLinkParentCommon, line 73
		case "subg.trigger.contact-sensor": // library marker davegut.tpLinkParentCommon, line 74
			deviceType = "Hub Contact" // library marker davegut.tpLinkParentCommon, line 75
			break // library marker davegut.tpLinkParentCommon, line 76

		case "subg.trigger.motion-sensor": // library marker davegut.tpLinkParentCommon, line 78
			deviceType = "Hub Motion" // library marker davegut.tpLinkParentCommon, line 79
			break // library marker davegut.tpLinkParentCommon, line 80

		case "subg.trigger.button": // library marker davegut.tpLinkParentCommon, line 82
			deviceType = "Hub Button" // library marker davegut.tpLinkParentCommon, line 83
			break // library marker davegut.tpLinkParentCommon, line 84

		case "subg.trigger.temp-hmdt-sensor": // library marker davegut.tpLinkParentCommon, line 86
			deviceType = "Hub TempHumidity" // library marker davegut.tpLinkParentCommon, line 87
			break // library marker davegut.tpLinkParentCommon, line 88

		case "subg.trigger.water-leak-sensor": // library marker davegut.tpLinkParentCommon, line 90
			deviceType = "Hub Leak" // library marker davegut.tpLinkParentCommon, line 91
			break // library marker davegut.tpLinkParentCommon, line 92

		case "subg.trv": // library marker davegut.tpLinkParentCommon, line 94
			deviceType = "Hub Trv" // library marker davegut.tpLinkParentCommon, line 95
			break // library marker davegut.tpLinkParentCommon, line 96

		case "subg.plugswitch.switch": // library marker davegut.tpLinkParentCommon, line 98
		case "subg.plugswitch.plug": // library marker davegut.tpLinkParentCommon, line 99
			deviceType = "Hub Plug" // library marker davegut.tpLinkParentCommon, line 100
			break // library marker davegut.tpLinkParentCommon, line 101

		//	===== PARENT CONNECTED ===== // library marker davegut.tpLinkParentCommon, line 103
		case "plug.powerstrip.sub-plug": // library marker davegut.tpLinkParentCommon, line 104
			deviceType = "Child Plug" // library marker davegut.tpLinkParentCommon, line 105
			break // library marker davegut.tpLinkParentCommon, line 106

 		case "kasa.switch.outlet.sub-fan": // library marker davegut.tpLinkParentCommon, line 108
			deviceType = "Child Fan" // library marker davegut.tpLinkParentCommon, line 109
			break // library marker davegut.tpLinkParentCommon, line 110

 		case "kasa.switch.outlet.sub-dimmer": // library marker davegut.tpLinkParentCommon, line 112
		case "plug.powerstrip.sub-bulb": // library marker davegut.tpLinkParentCommon, line 113
			deviceType = "Child Dimmer" // library marker davegut.tpLinkParentCommon, line 114
			break // library marker davegut.tpLinkParentCommon, line 115

		default: // library marker davegut.tpLinkParentCommon, line 117
			deviceType = "Child Undefined" // library marker davegut.tpLinkParentCommon, line 118
	} // library marker davegut.tpLinkParentCommon, line 119
	return deviceType // library marker davegut.tpLinkParentCommon, line 120
} // library marker davegut.tpLinkParentCommon, line 121

//	Data Distribution // library marker davegut.tpLinkParentCommon, line 123
def distChildGetData(devResp, data) { // library marker davegut.tpLinkParentCommon, line 124
	def child = getChildDevice(data.data) // library marker davegut.tpLinkParentCommon, line 125
	switch(devResp.method) { // library marker davegut.tpLinkParentCommon, line 126
		case "get_device_info": // library marker davegut.tpLinkParentCommon, line 127
			child.parse_get_device_info(devResp.result, data) // library marker davegut.tpLinkParentCommon, line 128
			child.parseNameUpdate(devResp.result) // library marker davegut.tpLinkParentCommon, line 129
			break // library marker davegut.tpLinkParentCommon, line 130
		case "get_current_power": // library marker davegut.tpLinkParentCommon, line 131
			child.parse_get_current_power(devResp.result, data) // library marker davegut.tpLinkParentCommon, line 132
			break // library marker davegut.tpLinkParentCommon, line 133
		case "get_device_usage": // library marker davegut.tpLinkParentCommon, line 134
			child.parse_get_device_usage(devResp.result, data) // library marker davegut.tpLinkParentCommon, line 135
			break // library marker davegut.tpLinkParentCommon, line 136
		case "get_trigger_logs": // library marker davegut.tpLinkParentCommon, line 137
			child.parse_get_trigger_log(devResp.result, data) // library marker davegut.tpLinkParentCommon, line 138
			break // library marker davegut.tpLinkParentCommon, line 139
		default: // library marker davegut.tpLinkParentCommon, line 140
			if (!devResp.method.contains("set_")) { // library marker davegut.tpLinkParentCommon, line 141
				Map logData = [method: "distGetData", data: data, // library marker davegut.tpLinkParentCommon, line 142
							   devMethod: devResp.method, status: "unprocessed"] // library marker davegut.tpLinkParentCommon, line 143
				logDebug(logData) // library marker davegut.tpLinkParentCommon, line 144
			} // library marker davegut.tpLinkParentCommon, line 145
	} // library marker davegut.tpLinkParentCommon, line 146
} // library marker davegut.tpLinkParentCommon, line 147

def parse_get_child_device_list(result, data) { // library marker davegut.tpLinkParentCommon, line 149
	Map logData = [method: "get_child_device_list", data: data] // library marker davegut.tpLinkParentCommon, line 150
	def children = getChildDevices() // library marker davegut.tpLinkParentCommon, line 151
	children.each { child -> // library marker davegut.tpLinkParentCommon, line 152
		def devId = child.getDataValue("deviceId") // library marker davegut.tpLinkParentCommon, line 153
		def childData = result.child_device_list.find{ it.device_id == devId } // library marker davegut.tpLinkParentCommon, line 154
		child.parse_get_device_info(childData, data) // library marker davegut.tpLinkParentCommon, line 155
	} // library marker davegut.tpLinkParentCommon, line 156
	logData << [status: "OK"] // library marker davegut.tpLinkParentCommon, line 157
	logDebug(logData) // library marker davegut.tpLinkParentCommon, line 158
} // library marker davegut.tpLinkParentCommon, line 159

// ~~~~~ end include (387) davegut.tpLinkParentCommon ~~~~~

// ~~~~~ start include (385) davegut.tpLinkComms ~~~~~
library ( // library marker davegut.tpLinkComms, line 1
	name: "tpLinkComms", // library marker davegut.tpLinkComms, line 2
	namespace: "davegut", // library marker davegut.tpLinkComms, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkComms, line 4
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
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkComms, line 17
	Map reqParams = [:] // library marker davegut.tpLinkComms, line 18
	if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 19
		reqParams = getKlapParams(cmdBody) // library marker davegut.tpLinkComms, line 20
	} else if (protocol == "camera") { // library marker davegut.tpLinkComms, line 21
		reqParams = getCameraParams(cmdBody, reqData) // library marker davegut.tpLinkComms, line 22
	} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 23
		reqParams = getAesParams(cmdBody) // library marker davegut.tpLinkComms, line 24
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 25
		reqParams = getVacAesParams(cmdBody, "${getDataValue("baseUrl")}/?token=${token}") // library marker davegut.tpLinkComms, line 26
	} // library marker davegut.tpLinkComms, line 27
	if (state.errorCount == 0) { // library marker davegut.tpLinkComms, line 28
		state.lastCommand = cmdData // library marker davegut.tpLinkComms, line 29
	} // library marker davegut.tpLinkComms, line 30
	logDebug([method: "asyncSend", reqData: reqData]) // library marker davegut.tpLinkComms, line 31
	asynchttpPost(action, reqParams, [data: reqData]) // library marker davegut.tpLinkComms, line 32
} // library marker davegut.tpLinkComms, line 33

def parseData(resp, protocol = getDataValue("protocol"), data = null) { // library marker davegut.tpLinkComms, line 35
	Map logData = [method: "parseData", status: resp.status, protocol: protocol, // library marker davegut.tpLinkComms, line 36
				   sourceMethod: data] // library marker davegut.tpLinkComms, line 37
	def message = "OK" // library marker davegut.tpLinkComms, line 38
	if (resp.status != 200) { message = resp.errorMessage } // library marker davegut.tpLinkComms, line 39
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 40
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 41
			logData << parseKlapData(resp, data) // library marker davegut.tpLinkComms, line 42
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 43
			logData << parseAesData(resp, data) // library marker davegut.tpLinkComms, line 44
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 45
			logData << parseVacAesData(resp, data) // library marker davegut.tpLinkComms, line 46
		} else if (protocol == "camera") { // library marker davegut.tpLinkComms, line 47
			logData << parseCameraData(resp, data) // library marker davegut.tpLinkComms, line 48
		} // library marker davegut.tpLinkComms, line 49
	} else { // library marker davegut.tpLinkComms, line 50
		String userMessage = "unspecified" // library marker davegut.tpLinkComms, line 51
		if (resp.status == 403) { // library marker davegut.tpLinkComms, line 52
			userMessage = "<b>Try again. If error persists, check your credentials</b>" // library marker davegut.tpLinkComms, line 53
		} else if (resp.status == 408) { // library marker davegut.tpLinkComms, line 54
			userMessage = "<b>Your router connection to ${getDataValue("baseUrl")} failed.  Run Configure.</b>" // library marker davegut.tpLinkComms, line 55
		} else { // library marker davegut.tpLinkComms, line 56
			userMessage = "<b>Unhandled error Lan return</b>" // library marker davegut.tpLinkComms, line 57
		} // library marker davegut.tpLinkComms, line 58
		logData << [respMessage: message, userMessage: userMessage] // library marker davegut.tpLinkComms, line 59
		logDebug(logData) // library marker davegut.tpLinkComms, line 60
	} // library marker davegut.tpLinkComms, line 61
	handleCommsError(resp.status, message) // library marker davegut.tpLinkComms, line 62
	return logData // library marker davegut.tpLinkComms, line 63
} // library marker davegut.tpLinkComms, line 64

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 66
//	Try a getToken??????????? // library marker davegut.tpLinkComms, line 67
def handleCommsError(status, msg = "") { // library marker davegut.tpLinkComms, line 68
	//	Retransmit all comms error except Switch and Level related (Hub retries for these). // library marker davegut.tpLinkComms, line 69
	//	This is determined by state.digital // library marker davegut.tpLinkComms, line 70
	if (status == 200) { // library marker davegut.tpLinkComms, line 71
		setCommsError(status, "OK") // library marker davegut.tpLinkComms, line 72
	} else { // library marker davegut.tpLinkComms, line 73
		Map logData = [method: "handleCommsError", status: code, msg: msg] // library marker davegut.tpLinkComms, line 74
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 75
		logData << [count: count, status: status, msg: msg] // library marker davegut.tpLinkComms, line 76
		switch(count) { // library marker davegut.tpLinkComms, line 77
			case 1: // library marker davegut.tpLinkComms, line 78
			case 2: // library marker davegut.tpLinkComms, line 79
				//	errors 1 and 2, retry immediately // library marker davegut.tpLinkComms, line 80
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 81
				break // library marker davegut.tpLinkComms, line 82
			case 3: // library marker davegut.tpLinkComms, line 83
				//	error 3, login or scan find device on the lan // library marker davegut.tpLinkComms, line 84
				//	then retry // library marker davegut.tpLinkComms, line 85
				if (status == 403) { // library marker davegut.tpLinkComms, line 86
					logData << [action: "attemptLogin"] // library marker davegut.tpLinkComms, line 87
//	await device handshake result???? // library marker davegut.tpLinkComms, line 88
					deviceHandshake() // library marker davegut.tpLinkComms, line 89
					runIn(4, delayedPassThrough) // library marker davegut.tpLinkComms, line 90
				} else { // library marker davegut.tpLinkComms, line 91
					logData << [action: "Find on LAN then login"] // library marker davegut.tpLinkComms, line 92
					configure() // library marker davegut.tpLinkComms, line 93
//	await configure result????? // library marker davegut.tpLinkComms, line 94
					runIn(10, delayedPassThrough) // library marker davegut.tpLinkComms, line 95
				} // library marker davegut.tpLinkComms, line 96
				break // library marker davegut.tpLinkComms, line 97
			case 4: // library marker davegut.tpLinkComms, line 98
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 99
				break // library marker davegut.tpLinkComms, line 100
			default: // library marker davegut.tpLinkComms, line 101
				//	Set comms error first time errros are 5 or more. // library marker davegut.tpLinkComms, line 102
				logData << [action: "SetCommsErrorTrue"] // library marker davegut.tpLinkComms, line 103
				setCommsError(status, msg, 5) // library marker davegut.tpLinkComms, line 104
		} // library marker davegut.tpLinkComms, line 105
		state.errorCount = count // library marker davegut.tpLinkComms, line 106
		logInfo(logData) // library marker davegut.tpLinkComms, line 107
	} // library marker davegut.tpLinkComms, line 108
} // library marker davegut.tpLinkComms, line 109

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 111
	def cmdData = new JSONObject(state.lastCommand) // library marker davegut.tpLinkComms, line 112
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 113
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 114
} // library marker davegut.tpLinkComms, line 115

////////////////////DELETE??????? // library marker davegut.tpLinkComms, line 117
def ping(baseUrl = getDataValue("baseUrl"), count = 1) { // library marker davegut.tpLinkComms, line 118
	def ip = baseUrl.replace("""http://""", "").replace(":80/app", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 119
	ip = ip.replace("""https://""", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 120
	hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(ip, count) // library marker davegut.tpLinkComms, line 121
	Map pingReturn = [method: "ping", ip: ip] // library marker davegut.tpLinkComms, line 122
	if (pingData.packetsReceived == count) { // library marker davegut.tpLinkComms, line 123
		pingReturn << [pingStatus: "success"] // library marker davegut.tpLinkComms, line 124
		logDebug(pingReturn) // library marker davegut.tpLinkComms, line 125
	} else { // library marker davegut.tpLinkComms, line 126
		pingReturn << [pingData: pingData, pingStatus: "<b>FAILED</b>.  There may be issues with your LAN."] // library marker davegut.tpLinkComms, line 127
		logWarn(pingReturn) // library marker davegut.tpLinkComms, line 128
	} // library marker davegut.tpLinkComms, line 129
	return pingReturn // library marker davegut.tpLinkComms, line 130
} // library marker davegut.tpLinkComms, line 131

def setCommsError(status, msg = "OK", count = state.commsError) { // library marker davegut.tpLinkComms, line 133
	Map logData = [method: "setCommsError", status: status, errorMsg: msg, count: count] // library marker davegut.tpLinkComms, line 134
	if (device && status == 200) { // library marker davegut.tpLinkComms, line 135
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 136
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 137
			sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkComms, line 138
			setPollInterval() // library marker davegut.tpLinkComms, line 139
			unschedule("errorDeviceHandshake") // library marker davegut.tpLinkComms, line 140
			logInfo(logData) // library marker davegut.tpLinkComms, line 141
		} // library marker davegut.tpLinkComms, line 142
	} else if (device) { // library marker davegut.tpLinkComms, line 143
		if (device.currentValue("commsError") == "false" && count > 4) { // library marker davegut.tpLinkComms, line 144
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 145
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 146
			runEvery10Minutes(errorConfigure) // library marker davegut.tpLinkComms, line 147
			logData << [pollInterval: "30 Min", errorDeviceHandshake: "ever 10 min"] // library marker davegut.tpLinkComms, line 148
			logWarn(logData) // library marker davegut.tpLinkComms, line 149
			if (status == 403) { // library marker davegut.tpLinkComms, line 150
				logWarn(logInErrorAction()) // library marker davegut.tpLinkComms, line 151
			} else { // library marker davegut.tpLinkComms, line 152
				logWarn(lanErrorAction()) // library marker davegut.tpLinkComms, line 153
			} // library marker davegut.tpLinkComms, line 154
		} else { // library marker davegut.tpLinkComms, line 155
			logData << [error: "Unspecified Error"] // library marker davegut.tpLinkComms, line 156
			logWarn(logData) // library marker davegut.tpLinkComms, line 157
		} // library marker davegut.tpLinkComms, line 158
	} // library marker davegut.tpLinkComms, line 159
} // library marker davegut.tpLinkComms, line 160

def lanErrorAction() { // library marker davegut.tpLinkComms, line 162
	def action = "Likely cause of this error is YOUR LAN device configuration: " // library marker davegut.tpLinkComms, line 163
	action += "a. VERIFY your device is on the DHCP list in your router, " // library marker davegut.tpLinkComms, line 164
	action += "b. VERIFY your device is in the active device list in your router, and " // library marker davegut.tpLinkComms, line 165
	action += "c. TRY controlling your device from the TAPO phone app." // library marker davegut.tpLinkComms, line 166
	return action // library marker davegut.tpLinkComms, line 167
} // library marker davegut.tpLinkComms, line 168

def logInErrorAction() { // library marker davegut.tpLinkComms, line 170
	def action = "Likely cause is your login credentials are incorrect or the login has expired. " // library marker davegut.tpLinkComms, line 171
	action += "a. RUN command Configure. b. If error persists, check your credentials in the App" // library marker davegut.tpLinkComms, line 172
	return action // library marker davegut.tpLinkComms, line 173
} // library marker davegut.tpLinkComms, line 174

/////////used///// // library marker davegut.tpLinkComms, line 176
def errorConfigure() { // library marker davegut.tpLinkComms, line 177
	logDebug([method: "errorConfigure"]) // library marker davegut.tpLinkComms, line 178
	configure() // library marker davegut.tpLinkComms, line 179
} // library marker davegut.tpLinkComms, line 180

//////////////////////////	CHECK IF ACTUALLY USED/WORKS. // library marker davegut.tpLinkComms, line 182
//	===== Common UDP Communications for checking if device at IP is device in Hubitat ===== // library marker davegut.tpLinkComms, line 183
private sendFindCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) { // library marker davegut.tpLinkComms, line 184
	def myHubAction = new hubitat.device.HubAction( // library marker davegut.tpLinkComms, line 185
		cmdData, // library marker davegut.tpLinkComms, line 186
		hubitat.device.Protocol.LAN, // library marker davegut.tpLinkComms, line 187
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, // library marker davegut.tpLinkComms, line 188
		 destinationAddress: "${ip}:${port}", // library marker davegut.tpLinkComms, line 189
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING, // library marker davegut.tpLinkComms, line 190
		 ignoreResponse: ignore, // library marker davegut.tpLinkComms, line 191
		 parseWarning: true, // library marker davegut.tpLinkComms, line 192
		 timeout: commsTo, // library marker davegut.tpLinkComms, line 193
		 callback: action]) // library marker davegut.tpLinkComms, line 194
	try { // library marker davegut.tpLinkComms, line 195
		sendHubCommand(myHubAction) // library marker davegut.tpLinkComms, line 196
	} catch (error) { // library marker davegut.tpLinkComms, line 197
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}") // library marker davegut.tpLinkComms, line 198
	} // library marker davegut.tpLinkComms, line 199
	return // library marker davegut.tpLinkComms, line 200
} // library marker davegut.tpLinkComms, line 201

// ~~~~~ end include (385) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (386) davegut.tpLinkCrypto ~~~~~
library ( // library marker davegut.tpLinkCrypto, line 1
	name: "tpLinkCrypto", // library marker davegut.tpLinkCrypto, line 2
	namespace: "davegut", // library marker davegut.tpLinkCrypto, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkCrypto, line 4
	description: "Handshake methods for TP-Link Integration", // library marker davegut.tpLinkCrypto, line 5
	category: "utilities", // library marker davegut.tpLinkCrypto, line 6
	documentationLink: "" // library marker davegut.tpLinkCrypto, line 7
) // library marker davegut.tpLinkCrypto, line 8
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tpLinkCrypto, line 9
import javax.crypto.Cipher // library marker davegut.tpLinkCrypto, line 10
import java.security.KeyFactory // library marker davegut.tpLinkCrypto, line 11
import java.util.Random // library marker davegut.tpLinkCrypto, line 12
import javax.crypto.spec.SecretKeySpec // library marker davegut.tpLinkCrypto, line 13
import javax.crypto.spec.IvParameterSpec // library marker davegut.tpLinkCrypto, line 14
import java.security.MessageDigest // library marker davegut.tpLinkCrypto, line 15

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 17
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 18
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 19
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 20
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 21
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 22
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 23
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 24
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 25

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 27
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 28
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 29
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 30
} // library marker davegut.tpLinkCrypto, line 31

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 33
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 34
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 35
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 36
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 37
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 38
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 39
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 40
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 41
} // library marker davegut.tpLinkCrypto, line 42

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 44
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 45
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 46
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 47
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 48
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 49
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 50
} // library marker davegut.tpLinkCrypto, line 51

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 53
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 54
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 55
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 56
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 57
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 58
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 59
} // library marker davegut.tpLinkCrypto, line 60

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 62
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 63
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 64
	md.update(data) // library marker davegut.tpLinkCrypto, line 65
	return md.digest() // library marker davegut.tpLinkCrypto, line 66
} // library marker davegut.tpLinkCrypto, line 67

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 69
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 70
	return new String(arr) // library marker davegut.tpLinkCrypto, line 71
} // library marker davegut.tpLinkCrypto, line 72

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 74
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 75
	try { // library marker davegut.tpLinkCrypto, line 76
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 77
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 78
	} catch (error) { // library marker davegut.tpLinkCrypto, line 79
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 80
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 81
	} // library marker davegut.tpLinkCrypto, line 82
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 83
} // library marker davegut.tpLinkCrypto, line 84

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 86
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 87
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 88
	return byteValue // library marker davegut.tpLinkCrypto, line 89
} // library marker davegut.tpLinkCrypto, line 90

def getSeed(size) { // library marker davegut.tpLinkCrypto, line 92
	byte[] temp = new byte[size] // library marker davegut.tpLinkCrypto, line 93
	new Random().nextBytes(temp) // library marker davegut.tpLinkCrypto, line 94
	return temp // library marker davegut.tpLinkCrypto, line 95
} // library marker davegut.tpLinkCrypto, line 96

// ~~~~~ end include (386) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (388) davegut.tpLinkTransAes ~~~~~
library ( // library marker davegut.tpLinkTransAes, line 1
	name: "tpLinkTransAes", // library marker davegut.tpLinkTransAes, line 2
	namespace: "davegut", // library marker davegut.tpLinkTransAes, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkTransAes, line 4
	description: "Handshake methods for TP-Link Integration", // library marker davegut.tpLinkTransAes, line 5
	category: "utilities", // library marker davegut.tpLinkTransAes, line 6
	documentationLink: "" // library marker davegut.tpLinkTransAes, line 7
) // library marker davegut.tpLinkTransAes, line 8

def aesHandshake(baseUrl = getDataValue("baseUrl"), devData = null) { // library marker davegut.tpLinkTransAes, line 10
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.tpLinkTransAes, line 11
	Map rsaKey = getRsaKey() // library marker davegut.tpLinkTransAes, line 12
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKey.public}-----END PUBLIC KEY-----\n" // library marker davegut.tpLinkTransAes, line 13
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.tpLinkTransAes, line 14
	Map reqParams = [uri: baseUrl, // library marker davegut.tpLinkTransAes, line 15
					 body: new groovy.json.JsonBuilder(cmdBody).toString(), // library marker davegut.tpLinkTransAes, line 16
					 requestContentType: "application/json", // library marker davegut.tpLinkTransAes, line 17
					 timeout: 10] // library marker davegut.tpLinkTransAes, line 18
	asynchttpPost("parseAesHandshake", reqParams, [data: reqData]) // library marker davegut.tpLinkTransAes, line 19
} // library marker davegut.tpLinkTransAes, line 20

def parseAesHandshake(resp, data){ // library marker davegut.tpLinkTransAes, line 22
	Map logData = [method: "parseAesHandshake"] // library marker davegut.tpLinkTransAes, line 23
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkTransAes, line 24
		try { // library marker davegut.tpLinkTransAes, line 25
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkTransAes, line 26
			Map cmdResp =  new JsonSlurper().parseText(resp.data) // library marker davegut.tpLinkTransAes, line 27
			//	cookie // library marker davegut.tpLinkTransAes, line 28
			def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkTransAes, line 29
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkTransAes, line 30
			//	keys // library marker davegut.tpLinkTransAes, line 31
			byte[] privateKeyBytes = getRsaKey().private.decodeBase64() // library marker davegut.tpLinkTransAes, line 32
			byte[] deviceKeyBytes = cmdResp.result.key.getBytes("UTF-8").decodeBase64() // library marker davegut.tpLinkTransAes, line 33
    		Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.tpLinkTransAes, line 34
			instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.tpLinkTransAes, line 35
			byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.tpLinkTransAes, line 36
			byte[] encKey = cryptoArray[0..15] // library marker davegut.tpLinkTransAes, line 37
			byte[] encIv = cryptoArray[16..31] // library marker davegut.tpLinkTransAes, line 38
			logData << [respStatus: "Cookies/Keys Updated", cookie: cookie, // library marker davegut.tpLinkTransAes, line 39
						encKey: encKey, encIv: encIv] // library marker davegut.tpLinkTransAes, line 40
			String password = encPassword // library marker davegut.tpLinkTransAes, line 41
			String username = encUsername // library marker davegut.tpLinkTransAes, line 42
			if (device) { // library marker davegut.tpLinkTransAes, line 43
				password = parent.encPassword // library marker davegut.tpLinkTransAes, line 44
				username = parent.encUsername // library marker davegut.tpLinkTransAes, line 45
				device.updateSetting("cookie",[type:"password", value: cookie]) // library marker davegut.tpLinkTransAes, line 46
				device.updateSetting("encKey",[type:"password", value: encKey]) // library marker davegut.tpLinkTransAes, line 47
				device.updateSetting("encIv",[type:"password", value: encIv]) // library marker davegut.tpLinkTransAes, line 48
			} else { // library marker davegut.tpLinkTransAes, line 49
				reqData << [cookie: cookie, encIv: encIv, encKey: encKey] // library marker davegut.tpLinkTransAes, line 50
			} // library marker davegut.tpLinkTransAes, line 51
			Map cmdBody = [method: "login_device", // library marker davegut.tpLinkTransAes, line 52
						   params: [password: password, // library marker davegut.tpLinkTransAes, line 53
									username: username], // library marker davegut.tpLinkTransAes, line 54
						   requestTimeMils: 0] // library marker davegut.tpLinkTransAes, line 55
			def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkTransAes, line 56
			Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkTransAes, line 57
						   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkTransAes, line 58
			Map reqParams = [uri: reqData.baseUrl, // library marker davegut.tpLinkTransAes, line 59
							  body: reqBody, // library marker davegut.tpLinkTransAes, line 60
							  timeout:10,  // library marker davegut.tpLinkTransAes, line 61
							  headers: ["Cookie": cookie], // library marker davegut.tpLinkTransAes, line 62
							  contentType: "application/json", // library marker davegut.tpLinkTransAes, line 63
							  requestContentType: "application/json"] // library marker davegut.tpLinkTransAes, line 64
			asynchttpPost("parseAesLogin", reqParams, [data: reqData]) // library marker davegut.tpLinkTransAes, line 65
			logDebug(logData) // library marker davegut.tpLinkTransAes, line 66
		} catch (err) { // library marker davegut.tpLinkTransAes, line 67
			logData << [respStatus: "ERROR parsing HTTP resp.data", // library marker davegut.tpLinkTransAes, line 68
						respData: resp.data, error: err] // library marker davegut.tpLinkTransAes, line 69
			logWarn(logData) // library marker davegut.tpLinkTransAes, line 70
		} // library marker davegut.tpLinkTransAes, line 71
	} else { // library marker davegut.tpLinkTransAes, line 72
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkTransAes, line 73
		logWarn(logData) // library marker davegut.tpLinkTransAes, line 74
	} // library marker davegut.tpLinkTransAes, line 75
} // library marker davegut.tpLinkTransAes, line 76

def parseAesLogin(resp, data) { // library marker davegut.tpLinkTransAes, line 78
	if (device) { // library marker davegut.tpLinkTransAes, line 79
		Map logData = [method: "parseAesLogin"] // library marker davegut.tpLinkTransAes, line 80
		if (resp.status == 200) { // library marker davegut.tpLinkTransAes, line 81
			if (resp.json.error_code == 0) { // library marker davegut.tpLinkTransAes, line 82
				try { // library marker davegut.tpLinkTransAes, line 83
					byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransAes, line 84
					byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransAes, line 85
					def clearResp = aesDecrypt(resp.json.result.response, encKey, encIv) // library marker davegut.tpLinkTransAes, line 86
					Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.tpLinkTransAes, line 87
					if (cmdResp.error_code == 0) { // library marker davegut.tpLinkTransAes, line 88
						def token = cmdResp.result.token // library marker davegut.tpLinkTransAes, line 89
						logData << [respStatus: "OK", token: token] // library marker davegut.tpLinkTransAes, line 90
						device.updateSetting("token",[type:"password", value: token]) // library marker davegut.tpLinkTransAes, line 91
						setCommsError(200) // library marker davegut.tpLinkTransAes, line 92
						logDebug(logData) // library marker davegut.tpLinkTransAes, line 93
					} else { // library marker davegut.tpLinkTransAes, line 94
						logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.tpLinkTransAes, line 95
									error_code: cmdResp.error_code, // library marker davegut.tpLinkTransAes, line 96
									check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.tpLinkTransAes, line 97
						logInfo(logData) // library marker davegut.tpLinkTransAes, line 98
					} // library marker davegut.tpLinkTransAes, line 99
				} catch (err) { // library marker davegut.tpLinkTransAes, line 100
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.tpLinkTransAes, line 101
								error: err] // library marker davegut.tpLinkTransAes, line 102
					logInfo(logData) // library marker davegut.tpLinkTransAes, line 103
				} // library marker davegut.tpLinkTransAes, line 104
			} else { // library marker davegut.tpLinkTransAes, line 105
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.tpLinkTransAes, line 106
							respJson: resp.json] // library marker davegut.tpLinkTransAes, line 107
				logInfo(logData) // library marker davegut.tpLinkTransAes, line 108
			} // library marker davegut.tpLinkTransAes, line 109
		} else { // library marker davegut.tpLinkTransAes, line 110
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.tpLinkTransAes, line 111
			logInfo(logData) // library marker davegut.tpLinkTransAes, line 112
		} // library marker davegut.tpLinkTransAes, line 113
	} else { // library marker davegut.tpLinkTransAes, line 114
		//	Code used in application only. // library marker davegut.tpLinkTransAes, line 115
		getAesToken(resp, data.data) // library marker davegut.tpLinkTransAes, line 116
	} // library marker davegut.tpLinkTransAes, line 117
} // library marker davegut.tpLinkTransAes, line 118

def getAesParams(cmdBody) { // library marker davegut.tpLinkTransAes, line 120
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransAes, line 121
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransAes, line 122
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkTransAes, line 123
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkTransAes, line 124
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkTransAes, line 125
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkTransAes, line 126
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkTransAes, line 127
					 contentType: "application/json", // library marker davegut.tpLinkTransAes, line 128
					 requestContentType: "application/json", // library marker davegut.tpLinkTransAes, line 129
					 timeout: 10, // library marker davegut.tpLinkTransAes, line 130
					 ignoreSSLIssues: true, // library marker davegut.tpLinkTransAes, line 131
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkTransAes, line 132
	return reqParams // library marker davegut.tpLinkTransAes, line 133
} // library marker davegut.tpLinkTransAes, line 134

def parseAesData(resp, data) { // library marker davegut.tpLinkTransAes, line 136
	Map parseData = [parseMethod: "parseAesData", sourceMethod: data.data] // library marker davegut.tpLinkTransAes, line 137
	try { // library marker davegut.tpLinkTransAes, line 138
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransAes, line 139
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransAes, line 140
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkTransAes, line 141
														 encKey, encIv)) // library marker davegut.tpLinkTransAes, line 142
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkTransAes, line 143
	} catch (err) { // library marker davegut.tpLinkTransAes, line 144
		parseData << [cryptoStatus: "decryptDataError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkTransAes, line 145
	} // library marker davegut.tpLinkTransAes, line 146
	return parseData // library marker davegut.tpLinkTransAes, line 147
} // library marker davegut.tpLinkTransAes, line 148

def getRsaKey() { // library marker davegut.tpLinkTransAes, line 150
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkTransAes, line 151
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkTransAes, line 152
} // library marker davegut.tpLinkTransAes, line 153

// ~~~~~ end include (388) davegut.tpLinkTransAes ~~~~~

// ~~~~~ start include (390) davegut.tpLinkTransKlap ~~~~~
library ( // library marker davegut.tpLinkTransKlap, line 1
	name: "tpLinkTransKlap", // library marker davegut.tpLinkTransKlap, line 2
	namespace: "davegut", // library marker davegut.tpLinkTransKlap, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkTransKlap, line 4
	description: "Handshake methods for TP-Link Integration", // library marker davegut.tpLinkTransKlap, line 5
	category: "utilities", // library marker davegut.tpLinkTransKlap, line 6
	documentationLink: "" // library marker davegut.tpLinkTransKlap, line 7
) // library marker davegut.tpLinkTransKlap, line 8

def klapHandshake(baseUrl, localHash, devData = null) { // library marker davegut.tpLinkTransKlap, line 10
	byte[] localSeed = getSeed(16) // library marker davegut.tpLinkTransKlap, line 11
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkTransKlap, line 12
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkTransKlap, line 13
					 body: localSeed, // library marker davegut.tpLinkTransKlap, line 14
					 contentType: "application/octet-stream", // library marker davegut.tpLinkTransKlap, line 15
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkTransKlap, line 16
					 timeout:10, // library marker davegut.tpLinkTransKlap, line 17
					 ignoreSSLIssues: true] // library marker davegut.tpLinkTransKlap, line 18
	asynchttpPost("parseKlapHandshake", reqParams, [data: reqData]) // library marker davegut.tpLinkTransKlap, line 19
} // library marker davegut.tpLinkTransKlap, line 20

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkTransKlap, line 22
	Map logData = [method: "parseKlapHandshake"] // library marker davegut.tpLinkTransKlap, line 23
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkTransKlap, line 24
		try { // library marker davegut.tpLinkTransKlap, line 25
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkTransKlap, line 26
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkTransKlap, line 27
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkTransKlap, line 28
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkTransKlap, line 29
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkTransKlap, line 30
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkTransKlap, line 31
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkTransKlap, line 32
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkTransKlap, line 33
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkTransKlap, line 34
				//	cookie // library marker davegut.tpLinkTransKlap, line 35
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkTransKlap, line 36
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkTransKlap, line 37
				//	seqNo and encIv // library marker davegut.tpLinkTransKlap, line 38
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkTransKlap, line 39
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkTransKlap, line 40
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkTransKlap, line 41

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkTransKlap, line 43
				if (device) { // library marker davegut.tpLinkTransKlap, line 44
					state.seqNo = seqNo // library marker davegut.tpLinkTransKlap, line 45
				} else { // library marker davegut.tpLinkTransKlap, line 46
					atomicState.seqNo = seqNo // library marker davegut.tpLinkTransKlap, line 47
				} // library marker davegut.tpLinkTransKlap, line 48

				//	encKey // library marker davegut.tpLinkTransKlap, line 50
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkTransKlap, line 51
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkTransKlap, line 52
				//	encSig // library marker davegut.tpLinkTransKlap, line 53
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkTransKlap, line 54
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkTransKlap, line 55
				if (device) { // library marker davegut.tpLinkTransKlap, line 56
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkTransKlap, line 57
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkTransKlap, line 58
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkTransKlap, line 59
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkTransKlap, line 60
				} else { // library marker davegut.tpLinkTransKlap, line 61
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkTransKlap, line 62
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkTransKlap, line 63
				} // library marker davegut.tpLinkTransKlap, line 64
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkTransKlap, line 65
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkTransKlap, line 66
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkTransKlap, line 67
								 body: body, // library marker davegut.tpLinkTransKlap, line 68
								 timeout:10, // library marker davegut.tpLinkTransKlap, line 69
								 ignoreSSLIssues: true, // library marker davegut.tpLinkTransKlap, line 70
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkTransKlap, line 71
								 contentType: "application/octet-stream", // library marker davegut.tpLinkTransKlap, line 72
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkTransKlap, line 73
				asynchttpPost("parseKlapHandshake2", reqParams, [data: reqData]) // library marker davegut.tpLinkTransKlap, line 74
			} else { // library marker davegut.tpLinkTransKlap, line 75
				logData << [respStatus: "ERROR: localAuthHash != serverHash", // library marker davegut.tpLinkTransKlap, line 76
							action: "<b>Check credentials and try again</b>"] // library marker davegut.tpLinkTransKlap, line 77
				logWarn(logData) // library marker davegut.tpLinkTransKlap, line 78
			} // library marker davegut.tpLinkTransKlap, line 79
		} catch (err) { // library marker davegut.tpLinkTransKlap, line 80
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkTransKlap, line 81
			logData << [action: "<b>Try Configure command</b>"] // library marker davegut.tpLinkTransKlap, line 82
			logWarn(logData) // library marker davegut.tpLinkTransKlap, line 83
		} // library marker davegut.tpLinkTransKlap, line 84
	} else { // library marker davegut.tpLinkTransKlap, line 85
		logData << [respStatus: resp.status, message: resp.errorMessage] // library marker davegut.tpLinkTransKlap, line 86
		logData << [action: "<b>Try Configure command</b>"] // library marker davegut.tpLinkTransKlap, line 87
		logWarn(logData) // library marker davegut.tpLinkTransKlap, line 88
	} // library marker davegut.tpLinkTransKlap, line 89
} // library marker davegut.tpLinkTransKlap, line 90

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkTransKlap, line 92
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkTransKlap, line 93
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkTransKlap, line 94
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkTransKlap, line 95
		setCommsError(200) // library marker davegut.tpLinkTransKlap, line 96
		logDebug(logData) // library marker davegut.tpLinkTransKlap, line 97
	} else { // library marker davegut.tpLinkTransKlap, line 98
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkTransKlap, line 99
					resp: resp.properties] // library marker davegut.tpLinkTransKlap, line 100
		logWarn(logData) // library marker davegut.tpLinkTransKlap, line 101
	} // library marker davegut.tpLinkTransKlap, line 102
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkTransKlap, line 103
} // library marker davegut.tpLinkTransKlap, line 104

def getKlapParams(cmdBody) { // library marker davegut.tpLinkTransKlap, line 106
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkTransKlap, line 107
	state.seqNo = seqNo // library marker davegut.tpLinkTransKlap, line 108
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransKlap, line 109
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransKlap, line 110
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkTransKlap, line 111
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkTransKlap, line 112

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkTransKlap, line 114
									encSig, seqNo) // library marker davegut.tpLinkTransKlap, line 115
	Map reqParams = [ // library marker davegut.tpLinkTransKlap, line 116
		uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkTransKlap, line 117
		body: encryptedData.cipherData, // library marker davegut.tpLinkTransKlap, line 118
		headers: ["Cookie": cookie], // library marker davegut.tpLinkTransKlap, line 119
		contentType: "application/octet-stream", // library marker davegut.tpLinkTransKlap, line 120
		requestContentType: "application/octet-stream", // library marker davegut.tpLinkTransKlap, line 121
		timeout: 10, // library marker davegut.tpLinkTransKlap, line 122
		ignoreSSLIssues: true] // library marker davegut.tpLinkTransKlap, line 123
	return reqParams // library marker davegut.tpLinkTransKlap, line 124
} // library marker davegut.tpLinkTransKlap, line 125

def parseKlapData(resp, data) { // library marker davegut.tpLinkTransKlap, line 127
	Map parseData = [Method: "parseKlapData", sourceMethod: data.data] // library marker davegut.tpLinkTransKlap, line 128
	try { // library marker davegut.tpLinkTransKlap, line 129
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransKlap, line 130
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransKlap, line 131
		int seqNo = state.seqNo // library marker davegut.tpLinkTransKlap, line 132
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkTransKlap, line 133
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkTransKlap, line 134
														   encIv, seqNo)) // library marker davegut.tpLinkTransKlap, line 135
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkTransKlap, line 136
	} catch (err) { // library marker davegut.tpLinkTransKlap, line 137
		parseData << [cryptoStatus: "decryptDataError", error: err] // library marker davegut.tpLinkTransKlap, line 138
	} // library marker davegut.tpLinkTransKlap, line 139
	return parseData // library marker davegut.tpLinkTransKlap, line 140
} // library marker davegut.tpLinkTransKlap, line 141

// ~~~~~ end include (390) davegut.tpLinkTransKlap ~~~~~

// ~~~~~ start include (376) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def nameSpace() { return "davegut" } // library marker davegut.Logging, line 10

def version() { return "2.4.2a" } // library marker davegut.Logging, line 12

def label() { // library marker davegut.Logging, line 14
	if (device) {  // library marker davegut.Logging, line 15
		return device.displayName + "-${version()}" // library marker davegut.Logging, line 16
	} else {  // library marker davegut.Logging, line 17
		return app.getLabel() + "-${version()}" // library marker davegut.Logging, line 18
	} // library marker davegut.Logging, line 19
} // library marker davegut.Logging, line 20

def updateAttr(attr, value) { // library marker davegut.Logging, line 22
	if (device.currentValue(attr) != value) { // library marker davegut.Logging, line 23
		sendEvent(name: attr, value: value) // library marker davegut.Logging, line 24
	} // library marker davegut.Logging, line 25
} // library marker davegut.Logging, line 26

def listAttributes() { // library marker davegut.Logging, line 28
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 29
	Map attrs = [:] // library marker davegut.Logging, line 30
	attrData.each { // library marker davegut.Logging, line 31
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 32
	} // library marker davegut.Logging, line 33
	return attrs // library marker davegut.Logging, line 34
} // library marker davegut.Logging, line 35

def setLogsOff() { // library marker davegut.Logging, line 37
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 38
	if (logEnable) { // library marker davegut.Logging, line 39
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 40
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 41
	} // library marker davegut.Logging, line 42
	return logData // library marker davegut.Logging, line 43
} // library marker davegut.Logging, line 44

def logTrace(msg){ log.trace "${label()}: ${msg}" } // library marker davegut.Logging, line 46

def logInfo(msg) {  // library marker davegut.Logging, line 48
	if (infoLog) { log.info "${label()}: ${msg}" } // library marker davegut.Logging, line 49
} // library marker davegut.Logging, line 50

def debugLogOff() { // library marker davegut.Logging, line 52
	if (device) { // library marker davegut.Logging, line 53
		device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 54
	} else { // library marker davegut.Logging, line 55
		app.updateSetting("logEnable", false) // library marker davegut.Logging, line 56
	} // library marker davegut.Logging, line 57
	logInfo("debugLogOff") // library marker davegut.Logging, line 58
} // library marker davegut.Logging, line 59

def logDebug(msg) { // library marker davegut.Logging, line 61
	if (logEnable) { log.debug "${label()}: ${msg}" } // library marker davegut.Logging, line 62
} // library marker davegut.Logging, line 63

def logWarn(msg) { log.warn "${label()}: ${msg}" } // library marker davegut.Logging, line 65

def logError(msg) { log.error "${label()}: ${msg}" } // library marker davegut.Logging, line 67

// ~~~~~ end include (376) davegut.Logging ~~~~~
