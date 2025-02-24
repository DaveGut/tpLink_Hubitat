/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Name Change to TpLink Parent.

Verified on TP25(US) and P306(US)
=================================================================================================*/

metadata {
	definition (name: "TpLink Parent", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_parent.groovy")
	{
	}
	preferences {
		input ("ledRule", "enum", title: "LED Mode (if night mode, set type and times in phone app)",
			   options: ["always", "never", "night_mode"], defaultValue: "always")
		input ("installChild", "bool", title: "Install Child Devices", defaultValue: true)
		commonPreferences()
	}
}

def installed() {
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	if (installChild) {
		logData << [children: "installing"]
		runIn(5, installChildDevices)
		pauseExecution(5000)
	}
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

//	===== Child Command Response =====
def parse_get_child_device_list(result, data) {
	Map logData = [method: "get_child_device_list",data: data]
	def children = getChildDevices()
	children.each { child ->
		def devId = child.getDataValue("deviceId")
		def childData = result.child_device_list.find{ it.device_id == devId }
		child.parse_get_device_info(childData, data)
	}
	logData << [status: "OK"]
	logDebug(logData)
}

def childRespDist(resp, data) {
	def respData = parseData(resp).cmdResp
	if (respData.error_code== 0) {
		def child = getChildDevice(data.data)
		if (child != null) {
			child.distChildData(respData.result.responseData.result.responses, data)
		} else {
			logWarn([method: "childRespDist", data: data, status: "notChild"])
		}
	} else {
		logWarn([method: "childRespDist", data: data, error: respData.error_code, status: "errorInResp"])
	}
}

//	===== Include Libraries =====






// ~~~~~ start include (64) davegut.tpLinkCommon ~~~~~
library ( // library marker davegut.tpLinkCommon, line 1
	name: "tpLinkCommon", // library marker davegut.tpLinkCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCommon, line 4
	description: "Common driver methods including capability Refresh and Configuration methods", // library marker davegut.tpLinkCommon, line 5
	category: "utilities", // library marker davegut.tpLinkCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkCommon, line 7
) // library marker davegut.tpLinkCommon, line 8

capability "Refresh" // library marker davegut.tpLinkCommon, line 10
capability "Configuration" // library marker davegut.tpLinkCommon, line 11
attribute "commsError", "string" // library marker davegut.tpLinkCommon, line 12

def commonPreferences() { // library marker davegut.tpLinkCommon, line 14
	List pollOptions = ["5 sec", "10 sec", "30 sec", "5 min", "10 min", "15 min", "30 min"] // library marker davegut.tpLinkCommon, line 15
	input ("pollInterval", "enum", title: "Poll/Refresh Interval", // library marker davegut.tpLinkCommon, line 16
		   options: pollOptions, defaultValue: "30 min") // library marker davegut.tpLinkCommon, line 17
	input ("rebootDev", "bool", title: "Reboot Device then run Save Preferences", defaultValue: false) // library marker davegut.tpLinkCommon, line 18
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkCommon, line 19
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkCommon, line 20
} // library marker davegut.tpLinkCommon, line 21

def commonInstalled() { // library marker davegut.tpLinkCommon, line 23
	updateAttr("commsError", "false") // library marker davegut.tpLinkCommon, line 24
	state.errorCount = 0 // library marker davegut.tpLinkCommon, line 25
	state.lastCmd = "" // library marker davegut.tpLinkCommon, line 26
	state.eventType = "digital" // library marker davegut.tpLinkCommon, line 27
	Map logData = [configure: configure(false)] // library marker davegut.tpLinkCommon, line 28
	return logData // library marker davegut.tpLinkCommon, line 29
} // library marker davegut.tpLinkCommon, line 30

def commonUpdated() { // library marker davegut.tpLinkCommon, line 32
	def commsErr = device.currentValue("commsError") // library marker davegut.tpLinkCommon, line 33
	Map logData = [commsError: commsErr] // library marker davegut.tpLinkCommon, line 34
	if (commsErr == "true") { // library marker davegut.tpLinkCommon, line 35
		logData << [configure: configure(true)] // library marker davegut.tpLinkCommon, line 36
	} // library marker davegut.tpLinkCommon, line 37
	if (rebootDev == true) { // library marker davegut.tpLinkCommon, line 38
		runIn(1, rebootDevice) // library marker davegut.tpLinkCommon, line 39
		device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.tpLinkCommon, line 40
		pauseExecution(15000) // library marker davegut.tpLinkCommon, line 41
	} // library marker davegut.tpLinkCommon, line 42
	updateAttr("commsError", "false") // library marker davegut.tpLinkCommon, line 43
	state.lastCmd = "" // library marker davegut.tpLinkCommon, line 44
	logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCommon, line 45
	logData << [logging: setLogsOff()] // library marker davegut.tpLinkCommon, line 46
	logData << [updateDevSettings: updDevSettings()] // library marker davegut.tpLinkCommon, line 47
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 48
	return logData // library marker davegut.tpLinkCommon, line 49
} // library marker davegut.tpLinkCommon, line 50

def rebootDevice() { // library marker davegut.tpLinkCommon, line 52
	asyncSend([method: "device_reboot"], "rebootDevice", "rebootParse") // library marker davegut.tpLinkCommon, line 53
} // library marker davegut.tpLinkCommon, line 54
def rebootParse(resp, data=null) { // library marker davegut.tpLinkCommon, line 55
	def respData = parseData(resp).cmdResp // library marker davegut.tpLinkCommon, line 56
	Map logData = [method: "rebootParse", data: data, respData: respData] // library marker davegut.tpLinkCommon, line 57
	logInfo(logData) // library marker davegut.tpLinkCommon, line 58
} // library marker davegut.tpLinkCommon, line 59

def updDevSettings() { // library marker davegut.tpLinkCommon, line 61
	Map logData = [method: "updDevSettings"] // library marker davegut.tpLinkCommon, line 62
	List requests = [] // library marker davegut.tpLinkCommon, line 63
	if (ledRule != null) { // library marker davegut.tpLinkCommon, line 64
		logData << [ledRule: ledRule] // library marker davegut.tpLinkCommon, line 65
		requests << [method: "get_led_info"] // library marker davegut.tpLinkCommon, line 66
	} // library marker davegut.tpLinkCommon, line 67
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCommon, line 68
	asyncSend(createMultiCmd(requests), "updDevSettings", "parseUpdates") // library marker davegut.tpLinkCommon, line 69
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 70
	return logData // library marker davegut.tpLinkCommon, line 71
} // library marker davegut.tpLinkCommon, line 72

//	===== Capability Configuration ===== // library marker davegut.tpLinkCommon, line 74
def configure(checkApp = true) { // library marker davegut.tpLinkCommon, line 75
	Map logData = [method: "configure", checkApp: checkApp] // library marker davegut.tpLinkCommon, line 76
	if (checkApp == true) { // library marker davegut.tpLinkCommon, line 77
		logData << [updateData: parent.tpLinkCheckForDevices(5)] // library marker davegut.tpLinkCommon, line 78
	} // library marker davegut.tpLinkCommon, line 79
	def action = updateDeviceData() // library marker davegut.tpLinkCommon, line 80
	unschedule() // library marker davegut.tpLinkCommon, line 81
	logData << [handshake: deviceHandshake()] // library marker davegut.tpLinkCommon, line 82
	runEvery3Hours(deviceHandshake) // library marker davegut.tpLinkCommon, line 83
	logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCommon, line 84
	logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCommon, line 85
	logData << [logging: setLogsOff()] // library marker davegut.tpLinkCommon, line 86
	runIn(2, initSettings) // library marker davegut.tpLinkCommon, line 87
	logInfo(logData) // library marker davegut.tpLinkCommon, line 88
	return logData // library marker davegut.tpLinkCommon, line 89
} // library marker davegut.tpLinkCommon, line 90

def initSettings() { // library marker davegut.tpLinkCommon, line 92
	Map logData = [method: "initSettings"] // library marker davegut.tpLinkCommon, line 93
	Map prefs = state.compData // library marker davegut.tpLinkCommon, line 94
	List requests = [] // library marker davegut.tpLinkCommon, line 95
	if (ledRule) { // library marker davegut.tpLinkCommon, line 96
		requests << [method: "get_led_info"] // library marker davegut.tpLinkCommon, line 97
	} // library marker davegut.tpLinkCommon, line 98
	if (getDataValue("type") == "Plug EM") { requests << [method: "get_energy_usage"] } // library marker davegut.tpLinkCommon, line 99
	requests << [method: "get_device_info"] // library marker davegut.tpLinkCommon, line 100
	asyncSend(createMultiCmd(requests), "initSettings", "parseUpdates") // library marker davegut.tpLinkCommon, line 101
	return logData // library marker davegut.tpLinkCommon, line 102
} // library marker davegut.tpLinkCommon, line 103

def setPollInterval(pInterval = pollInterval) { // library marker davegut.tpLinkCommon, line 105
	String devType = getDataValue("type") // library marker davegut.tpLinkCommon, line 106
	def pollMethod = "minRefresh" // library marker davegut.tpLinkCommon, line 107
	if (devType == "Plug EM") { // library marker davegut.tpLinkCommon, line 108
		pollMethod = "plugEmRefresh" // library marker davegut.tpLinkCommon, line 109
	} else if (devType == "Hub"|| devType == "Parent") { // library marker davegut.tpLinkCommon, line 110
		pollMethod = "parentRefresh" // library marker davegut.tpLinkCommon, line 111
	} // library marker davegut.tpLinkCommon, line 112

	if (pInterval.contains("sec")) { // library marker davegut.tpLinkCommon, line 114
		def interval = pInterval.replace(" sec", "").toInteger() // library marker davegut.tpLinkCommon, line 115
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 116
		schedule("${start}/${interval} * * * * ?", pollMethod) // library marker davegut.tpLinkCommon, line 117
	} else { // library marker davegut.tpLinkCommon, line 118
		def interval= pInterval.replace(" min", "").toInteger() // library marker davegut.tpLinkCommon, line 119
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.tpLinkCommon, line 120
		schedule("${start} */${interval} * * * ?", pollMethod) // library marker davegut.tpLinkCommon, line 121
	} // library marker davegut.tpLinkCommon, line 122
	return pInterval // library marker davegut.tpLinkCommon, line 123
} // library marker davegut.tpLinkCommon, line 124

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkCommon, line 126
def parseUpdates(resp, data = null) { // library marker davegut.tpLinkCommon, line 127
	Map logData = [method: "parseUpdates", data: data] // library marker davegut.tpLinkCommon, line 128
	def respData = parseData(resp) // library marker davegut.tpLinkCommon, line 129
	def cmdResp = parseData(resp).cmdResp // library marker davegut.tpLinkCommon, line 130
	if (cmdResp != null && cmdResp.error_code == 0) { // library marker davegut.tpLinkCommon, line 131
		cmdResp.result.responses.each { // library marker davegut.tpLinkCommon, line 132
			if (it.error_code == 0) { // library marker davegut.tpLinkCommon, line 133
				if (!it.method.contains("set_")) { // library marker davegut.tpLinkCommon, line 134
					distGetData(it, data) // library marker davegut.tpLinkCommon, line 135
				} else { // library marker davegut.tpLinkCommon, line 136
					logData << [devMethod: it.method] // library marker davegut.tpLinkCommon, line 137
					logDebug(logData) // library marker davegut.tpLinkCommon, line 138
				} // library marker davegut.tpLinkCommon, line 139
			} else { // library marker davegut.tpLinkCommon, line 140
				logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCommon, line 141
				logWarn(logData) // library marker davegut.tpLinkCommon, line 142
			} // library marker davegut.tpLinkCommon, line 143
		} // library marker davegut.tpLinkCommon, line 144
	} else { // library marker davegut.tpLinkCommon, line 145
		logData << [status: "invalidRequest", respData: respData, // library marker davegut.tpLinkCommon, line 146
					respProps: [headers: resp.headers, status: resp.status, // library marker davegut.tpLinkCommon, line 147
								warningMessages: resp.warningMessages]] // library marker davegut.tpLinkCommon, line 148
		logWarn(logData)				 // library marker davegut.tpLinkCommon, line 149
	} // library marker davegut.tpLinkCommon, line 150
} // library marker davegut.tpLinkCommon, line 151

def distGetData(devResp, data) { // library marker davegut.tpLinkCommon, line 153
	switch(devResp.method) { // library marker davegut.tpLinkCommon, line 154
		case "get_device_info": // library marker davegut.tpLinkCommon, line 155
			parse_get_device_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 156
			break // library marker davegut.tpLinkCommon, line 157
		case "get_energy_usage": // library marker davegut.tpLinkCommon, line 158
			parse_get_energy_usage(devResp.result, data) // library marker davegut.tpLinkCommon, line 159
			break // library marker davegut.tpLinkCommon, line 160
		case "get_child_device_list": // library marker davegut.tpLinkCommon, line 161
			parse_get_child_device_list(devResp.result, data) // library marker davegut.tpLinkCommon, line 162
			break // library marker davegut.tpLinkCommon, line 163
		case "get_alarm_configure": // library marker davegut.tpLinkCommon, line 164
			parse_get_alarm_configure(devResp.result, data) // library marker davegut.tpLinkCommon, line 165
			break // library marker davegut.tpLinkCommon, line 166
		case "get_led_info": // library marker davegut.tpLinkCommon, line 167
			parse_get_led_info(devResp.result, data) // library marker davegut.tpLinkCommon, line 168
			break // library marker davegut.tpLinkCommon, line 169
		default: // library marker davegut.tpLinkCommon, line 170
			Map logData = [method: "distGetData", data: data, // library marker davegut.tpLinkCommon, line 171
						   devMethod: devResp.method, status: "unprocessed"] // library marker davegut.tpLinkCommon, line 172
			logDebug(logData) // library marker davegut.tpLinkCommon, line 173
	} // library marker davegut.tpLinkCommon, line 174
} // library marker davegut.tpLinkCommon, line 175

def parse_get_led_info(result, data) { // library marker davegut.tpLinkCommon, line 177
	Map logData = [method: "parse_get_led_info", data: data] // library marker davegut.tpLinkCommon, line 178
	if (ledRule != result.led_rule) { // library marker davegut.tpLinkCommon, line 179
		Map request = [ // library marker davegut.tpLinkCommon, line 180
			method: "set_led_info", // library marker davegut.tpLinkCommon, line 181
			params: [ // library marker davegut.tpLinkCommon, line 182
				led_rule: ledRule, // library marker davegut.tpLinkCommon, line 183
				night_mode: [ // library marker davegut.tpLinkCommon, line 184
					night_mode_type: result.night_mode.night_mode_type, // library marker davegut.tpLinkCommon, line 185
					sunrise_offset: result.night_mode.sunrise_offset,  // library marker davegut.tpLinkCommon, line 186
					sunset_offset:result.night_mode.sunset_offset, // library marker davegut.tpLinkCommon, line 187
					start_time: result.night_mode.start_time, // library marker davegut.tpLinkCommon, line 188
					end_time: result.night_mode.end_time // library marker davegut.tpLinkCommon, line 189
				]]] // library marker davegut.tpLinkCommon, line 190
		asyncSend(request, "delayedUpdates", "parseUpdates") // library marker davegut.tpLinkCommon, line 191
		device.updateSetting("ledRule", [type:"enum", value: ledRule]) // library marker davegut.tpLinkCommon, line 192
		logData << [status: "updatingLedRule"] // library marker davegut.tpLinkCommon, line 193
	} // library marker davegut.tpLinkCommon, line 194
	logData << [ledRule: ledRule] // library marker davegut.tpLinkCommon, line 195
	logDebug(logData) // library marker davegut.tpLinkCommon, line 196
} // library marker davegut.tpLinkCommon, line 197

//	===== Capability Refresh ===== // library marker davegut.tpLinkCommon, line 199
def refresh() { // library marker davegut.tpLinkCommon, line 200
	def type = getDataValue("type") // library marker davegut.tpLinkCommon, line 201
	if (type == "Plug EM") { // library marker davegut.tpLinkCommon, line 202
		plugEmRefresh() // library marker davegut.tpLinkCommon, line 203
	} else if (type == "Hub" || type == "Parent") { // library marker davegut.tpLinkCommon, line 204
		parentRefresh() // library marker davegut.tpLinkCommon, line 205
	} else { // library marker davegut.tpLinkCommon, line 206
		minRefresh() // library marker davegut.tpLinkCommon, line 207
	} // library marker davegut.tpLinkCommon, line 208
} // library marker davegut.tpLinkCommon, line 209

def plugEmRefresh() { // library marker davegut.tpLinkCommon, line 211
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 212
	requests << [method:"get_energy_usage"] // library marker davegut.tpLinkCommon, line 213
	asyncSend(createMultiCmd(requests), "plugEmRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 214
} // library marker davegut.tpLinkCommon, line 215

def parentRefresh() { // library marker davegut.tpLinkCommon, line 217
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 218
	requests << [method:"get_child_device_list"] // library marker davegut.tpLinkCommon, line 219
	asyncSend(createMultiCmd(requests), "parentRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 220
} // library marker davegut.tpLinkCommon, line 221

def minRefresh() { // library marker davegut.tpLinkCommon, line 223
	List requests = [[method: "get_device_info"]] // library marker davegut.tpLinkCommon, line 224
	asyncSend(createMultiCmd(requests), "minRefresh", "parseUpdates") // library marker davegut.tpLinkCommon, line 225
} // library marker davegut.tpLinkCommon, line 226

def emUpdate() { } // library marker davegut.tpLinkCommon, line 228
def emRefresh() { plugEmRefresh() } // library marker davegut.tpLinkCommon, line 229
def deviceLogin() { deviceHandshake() } // library marker davegut.tpLinkCommon, line 230

def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkCommon, line 232
	asyncSend(createMultiCmd(requests), data, action) // library marker davegut.tpLinkCommon, line 233
} // library marker davegut.tpLinkCommon, line 234

def sendSingleCmd(request, data, action) { // library marker davegut.tpLinkCommon, line 236
	asyncSend(request, data, action) // library marker davegut.tpLinkCommon, line 237
} // library marker davegut.tpLinkCommon, line 238

def createMultiCmd(requests) { // library marker davegut.tpLinkCommon, line 240
	Map cmdBody = [ // library marker davegut.tpLinkCommon, line 241
		method: "multipleRequest", // library marker davegut.tpLinkCommon, line 242
		params: [requests: requests]] // library marker davegut.tpLinkCommon, line 243
	return cmdBody // library marker davegut.tpLinkCommon, line 244
} // library marker davegut.tpLinkCommon, line 245

def nullParse(resp, data) { } // library marker davegut.tpLinkCommon, line 247

def updateAttr(attr, value) { // library marker davegut.tpLinkCommon, line 249
	if (device.currentValue(attr) != value) { // library marker davegut.tpLinkCommon, line 250
		sendEvent(name: attr, value: value) // library marker davegut.tpLinkCommon, line 251
	} // library marker davegut.tpLinkCommon, line 252
} // library marker davegut.tpLinkCommon, line 253

//	===== Check/Update device data ===== // library marker davegut.tpLinkCommon, line 255
//	Called if Driver/App version has changed from app or from configure. // library marker davegut.tpLinkCommon, line 256
def updateDeviceData() { // library marker davegut.tpLinkCommon, line 257
	def currVer = getDataValue("version") // library marker davegut.tpLinkCommon, line 258
	Map logData = [method: "updateDeviceData", currentVersion: currVer,  // library marker davegut.tpLinkCommon, line 259
				   newVersion: version()] // library marker davegut.tpLinkCommon, line 260
	if (currVer != version()) { // library marker davegut.tpLinkCommon, line 261
	//	The below data must be updated on each major version change. // library marker davegut.tpLinkCommon, line 262
		def devData = parent.getChildDevice(device.getDeviceNetworkId()) // library marker davegut.tpLinkCommon, line 263
		logData << [capability: devData.data.capability] // library marker davegut.tpLinkCommon, line 264
		if (devData != null && devData.data.capability != null) { // library marker davegut.tpLinkCommon, line 265
			String tpLinkType // library marker davegut.tpLinkCommon, line 266
			String type // library marker davegut.tpLinkCommon, line 267
			switch (devData.data.capability) { // library marker davegut.tpLinkCommon, line 268
				case "bulb_dimmer": // library marker davegut.tpLinkCommon, line 269
					tpLinkType = "SMART.TAPOBULB" // library marker davegut.tpLinkCommon, line 270
					type = "Dimmer" // library marker davegut.tpLinkCommon, line 271
					break // library marker davegut.tpLinkCommon, line 272
				case "bulb_color": // library marker davegut.tpLinkCommon, line 273
					tpLinkType = "SMART.TAPOBULB" // library marker davegut.tpLinkCommon, line 274
					type = "Color Bulb" // library marker davegut.tpLinkCommon, line 275
					break // library marker davegut.tpLinkCommon, line 276
				case "bulb_lightStrip": // library marker davegut.tpLinkCommon, line 277
					tpLinkType = "SMART.TAPOBULB" // library marker davegut.tpLinkCommon, line 278
					type = "Light Strip" // library marker davegut.tpLinkCommon, line 279
					break // library marker davegut.tpLinkCommon, line 280
				case "plug": // library marker davegut.tpLinkCommon, line 281
					tpLinkType = "SMART.TAPOPLUG" // library marker davegut.tpLinkCommon, line 282
					type = "Plug" // library marker davegut.tpLinkCommon, line 283
					break // library marker davegut.tpLinkCommon, line 284
				case "plug_dimmer": // library marker davegut.tpLinkCommon, line 285
					tpLinkType = "SMART.TAPOPLUG" // library marker davegut.tpLinkCommon, line 286
					type = "Dimmer" // library marker davegut.tpLinkCommon, line 287
					break // library marker davegut.tpLinkCommon, line 288
				case "plug_multi": // library marker davegut.tpLinkCommon, line 289
					tpLinkType = "SMART.TAPOPLUG" // library marker davegut.tpLinkCommon, line 290
					type = "Parent" // library marker davegut.tpLinkCommon, line 291
					break // library marker davegut.tpLinkCommon, line 292
				case "plug_em": // library marker davegut.tpLinkCommon, line 293
					tpLinkType = "SMART.TAPOPLUG" // library marker davegut.tpLinkCommon, line 294
					type = "Plug EM" // library marker davegut.tpLinkCommon, line 295
					break // library marker davegut.tpLinkCommon, line 296
				case "hub": // library marker davegut.tpLinkCommon, line 297
					tpLinkType = "SMART.TAPOHUB" // library marker davegut.tpLinkCommon, line 298
					type = "Hub" // library marker davegut.tpLinkCommon, line 299
					break // library marker davegut.tpLinkCommon, line 300
				case "robovac": // library marker davegut.tpLinkCommon, line 301
					tpLinkType = "SMART.TAPOROBOVAC" // library marker davegut.tpLinkCommon, line 302
					type = "Robovac" // library marker davegut.tpLinkCommon, line 303
					break // library marker davegut.tpLinkCommon, line 304
				default: // library marker davegut.tpLinkCommon, line 305
					break // library marker davegut.tpLinkCommon, line 306
			} // library marker davegut.tpLinkCommon, line 307
			updateDataValue("tpLinkType", tpLinkType) // library marker davegut.tpLinkCommon, line 308
			updateDataValue("type", type) // library marker davegut.tpLinkCommon, line 309
			removeDataValue("capability") // library marker davegut.tpLinkCommon, line 310
		} else { // library marker davegut.tpLinkCommon, line 311
			logData << [status: "noUpdates"] // library marker davegut.tpLinkCommon, line 312
		} // library marker davegut.tpLinkCommon, line 313
		updateDataValue("version", version())	 // library marker davegut.tpLinkCommon, line 314
	} // library marker davegut.tpLinkCommon, line 315
	logInfo(logData) // library marker davegut.tpLinkCommon, line 316
	return // library marker davegut.tpLinkCommon, line 317
} // library marker davegut.tpLinkCommon, line 318

//	===== Device Handshake ===== // library marker davegut.tpLinkCommon, line 320
def deviceHandshake() { // library marker davegut.tpLinkCommon, line 321
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkCommon, line 322
	Map logData = [method: "deviceHandshake", protocol: protocol] // library marker davegut.tpLinkCommon, line 323
	if (protocol == "KLAP") { // library marker davegut.tpLinkCommon, line 324
		klapHandshake() // library marker davegut.tpLinkCommon, line 325
	} else if (protocol == "AES") { // library marker davegut.tpLinkCommon, line 326
		aesHandshake() // library marker davegut.tpLinkCommon, line 327
	} else { // library marker davegut.tpLinkCommon, line 328
		logData << [ERROR: "Protocol not supported"] // library marker davegut.tpLinkCommon, line 329
	} // library marker davegut.tpLinkCommon, line 330
	pauseExecution(5000) // library marker davegut.tpLinkCommon, line 331
	logDebug(logData) // library marker davegut.tpLinkCommon, line 332
	return logData // library marker davegut.tpLinkCommon, line 333
} // library marker davegut.tpLinkCommon, line 334

// ~~~~~ end include (64) davegut.tpLinkCommon ~~~~~

// ~~~~~ start include (63) davegut.tpLinkChildInst ~~~~~
library ( // library marker davegut.tpLinkChildInst, line 1
	name: "tpLinkChildInst", // library marker davegut.tpLinkChildInst, line 2
	namespace: "davegut", // library marker davegut.tpLinkChildInst, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkChildInst, line 4
	description: "Child Installation Methods", // library marker davegut.tpLinkChildInst, line 5
	category: "utilities", // library marker davegut.tpLinkChildInst, line 6
	documentationLink: "" // library marker davegut.tpLinkChildInst, line 7
) // library marker davegut.tpLinkChildInst, line 8

def installChildDevices() { // library marker davegut.tpLinkChildInst, line 10
	Map request = [method: "get_child_device_list"] // library marker davegut.tpLinkChildInst, line 11
	asyncSend(request, "installChildDevices", "installChildren") // library marker davegut.tpLinkChildInst, line 12
} // library marker davegut.tpLinkChildInst, line 13

def installChildren(resp, data=null) { // library marker davegut.tpLinkChildInst, line 15
	Map logData = [method: "installChildren"] // library marker davegut.tpLinkChildInst, line 16
	def respData = parseData(resp) // library marker davegut.tpLinkChildInst, line 17
	def children = respData.cmdResp.result.child_device_list // library marker davegut.tpLinkChildInst, line 18
	children.each { // library marker davegut.tpLinkChildInst, line 19
		String childDni = it.mac // library marker davegut.tpLinkChildInst, line 20
		if (it.position) { // library marker davegut.tpLinkChildInst, line 21
			childDni = childDni + "-" + it.position // library marker davegut.tpLinkChildInst, line 22
		} // library marker davegut.tpLinkChildInst, line 23
		def isChild = getChildDevice(childDni) // library marker davegut.tpLinkChildInst, line 24
		byte[] plainBytes = it.nickname.decodeBase64() // library marker davegut.tpLinkChildInst, line 25
		String alias = new String(plainBytes) // library marker davegut.tpLinkChildInst, line 26
		Map instData = [alias: alias, childDni: childDni] // library marker davegut.tpLinkChildInst, line 27
		if (isChild) { // library marker davegut.tpLinkChildInst, line 28
			instData << [status: "device already installed"] // library marker davegut.tpLinkChildInst, line 29
		} else { // library marker davegut.tpLinkChildInst, line 30
			String devType = getDeviceType(it.category) // library marker davegut.tpLinkChildInst, line 31
			instData << [label: alias, name: it.model, type: devType, deviceId:  // library marker davegut.tpLinkChildInst, line 32
						 it.device_id, category: it.category] // library marker davegut.tpLinkChildInst, line 33
			if (devType == "Child Undefined") { // library marker davegut.tpLinkChildInst, line 34
				instData << [status: "notInstalled", error: "Currently Unsupported"] // library marker davegut.tpLinkChildInst, line 35
				logWarn(instData) // library marker davegut.tpLinkChildInst, line 36
			} else { // library marker davegut.tpLinkChildInst, line 37
				try { // library marker davegut.tpLinkChildInst, line 38
					addChildDevice( // library marker davegut.tpLinkChildInst, line 39
						nameSpace(),  // library marker davegut.tpLinkChildInst, line 40
						"TpLink ${devType}", // library marker davegut.tpLinkChildInst, line 41
						childDni, // library marker davegut.tpLinkChildInst, line 42
						[ // library marker davegut.tpLinkChildInst, line 43
							"label": alias, // library marker davegut.tpLinkChildInst, line 44
							"name": it.model, // library marker davegut.tpLinkChildInst, line 45
							category: it.category, // library marker davegut.tpLinkChildInst, line 46
							deviceId: it.device_id, // library marker davegut.tpLinkChildInst, line 47
							type: devType // library marker davegut.tpLinkChildInst, line 48
						]) // library marker davegut.tpLinkChildInst, line 49
					instData << [status: "Installed"] // library marker davegut.tpLinkChildInst, line 50
				} catch (e) { // library marker davegut.tpLinkChildInst, line 51
					instData << [status: "FAILED", error: err] // library marker davegut.tpLinkChildInst, line 52
					logWarn(instData) // library marker davegut.tpLinkChildInst, line 53
				} // library marker davegut.tpLinkChildInst, line 54
			} // library marker davegut.tpLinkChildInst, line 55
		} // library marker davegut.tpLinkChildInst, line 56
		logData << ["${alias}": instData] // library marker davegut.tpLinkChildInst, line 57
		pauseExecution(2000) // library marker davegut.tpLinkChildInst, line 58
	} // library marker davegut.tpLinkChildInst, line 59
	device.updateSetting("installChild", [type: "bool", value: "false"]) // library marker davegut.tpLinkChildInst, line 60
	logInfo(logData) // library marker davegut.tpLinkChildInst, line 61
} // library marker davegut.tpLinkChildInst, line 62

def getDeviceType(category) { // library marker davegut.tpLinkChildInst, line 64
	String deviceType // library marker davegut.tpLinkChildInst, line 65
	switch(category) { // library marker davegut.tpLinkChildInst, line 66
		case "subg.trigger.contact-sensor": // library marker davegut.tpLinkChildInst, line 67
			deviceType = "Hub Contact"; break // library marker davegut.tpLinkChildInst, line 68
		case "subg.trigger.motion-sensor": // library marker davegut.tpLinkChildInst, line 69
			deviceType = "Hub Motion"; break // library marker davegut.tpLinkChildInst, line 70
		case "subg.trigger.button": // library marker davegut.tpLinkChildInst, line 71
			deviceType = "Hub Button"; break // library marker davegut.tpLinkChildInst, line 72
		case "subg.trigger.temp-hmdt-sensor": // library marker davegut.tpLinkChildInst, line 73
logWarn("TEMP-HUMIDITY Sensor not supported.  Requires TEST Volunteer to finish") // library marker davegut.tpLinkChildInst, line 74
			deviceType = "Child Undefined"; break // library marker davegut.tpLinkChildInst, line 75
//			deviceType = "Hub TempHumidity"; break // library marker davegut.tpLinkChildInst, line 76
		case "subg.trigger.water-leak-sensor": // library marker davegut.tpLinkChildInst, line 77
			deviceType = "Hub Leak"; break // library marker davegut.tpLinkChildInst, line 78
		case "subg.trv": // library marker davegut.tpLinkChildInst, line 79
logWarn("TRV not supported.  Requires TEST Volunteer to finish") // library marker davegut.tpLinkChildInst, line 80
			deviceType = "Child Undefined"; break // library marker davegut.tpLinkChildInst, line 81
//			deviceType = "Hub Trv"; break // library marker davegut.tpLinkChildInst, line 82
		case "plug.powerstrip.sub-plug": // library marker davegut.tpLinkChildInst, line 83
			deviceType = "Child Plug"; break // library marker davegut.tpLinkChildInst, line 84
		case "kasa.switch.outlet.sub-fan": // library marker davegut.tpLinkChildInst, line 85
			deviceType = "Child Fan"; break // library marker davegut.tpLinkChildInst, line 86
		case "kasa.switch.outlet.sub-dimmer": // library marker davegut.tpLinkChildInst, line 87
		case "plug.powerstrip.sub-bulb": // library marker davegut.tpLinkChildInst, line 88
			deviceType = "Child Dimmer"; break // library marker davegut.tpLinkChildInst, line 89
		default: // library marker davegut.tpLinkChildInst, line 90
			deviceType = "Child Undefined" // library marker davegut.tpLinkChildInst, line 91
	} // library marker davegut.tpLinkChildInst, line 92
	return deviceType // library marker davegut.tpLinkChildInst, line 93
} // library marker davegut.tpLinkChildInst, line 94

// ~~~~~ end include (63) davegut.tpLinkChildInst ~~~~~

// ~~~~~ start include (56) davegut.tpLinkComms ~~~~~
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
		runIn(1, handleCommsError) // library marker davegut.tpLinkComms, line 37
	} // library marker davegut.tpLinkComms, line 38
	logDebug(logData) // library marker davegut.tpLinkComms, line 39
} // library marker davegut.tpLinkComms, line 40

def parseData(resp, protocol = getDataValue("protocol")) { // library marker davegut.tpLinkComms, line 42
	Map logData = [method: "parseData"] // library marker davegut.tpLinkComms, line 43
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 44
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 45
			logData << parseKlapData(resp) // library marker davegut.tpLinkComms, line 46
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 47
			logData << parseAesData(resp) // library marker davegut.tpLinkComms, line 48
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 49
			logData << parseVacAesData(resp) // library marker davegut.tpLinkComms, line 50
		} // library marker davegut.tpLinkComms, line 51
	} else { // library marker davegut.tpLinkComms, line 52
		logData << [status: "httpFailure"] // library marker davegut.tpLinkComms, line 53
		runIn(1, handleCommsError) // library marker davegut.tpLinkComms, line 54
	} // library marker davegut.tpLinkComms, line 55
	return logData // library marker davegut.tpLinkComms, line 56
} // library marker davegut.tpLinkComms, line 57

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 59
def handleCommsError() { // library marker davegut.tpLinkComms, line 60
	Map logData = [method: "handleCommsError"] // library marker davegut.tpLinkComms, line 61
	if (state.lastCmd != "") { // library marker davegut.tpLinkComms, line 62
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 63
		logData << [count: count, lastCmd: state.lastCmd] // library marker davegut.tpLinkComms, line 64
		switch (count) { // library marker davegut.tpLinkComms, line 65
			case 1: // library marker davegut.tpLinkComms, line 66
				logData << [action: "resendCommand"] // library marker davegut.tpLinkComms, line 67
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 68
				break // library marker davegut.tpLinkComms, line 69
			case 2: // library marker davegut.tpLinkComms, line 70
				logData << [attemptHandshake: deviceHandshake(), // library marker davegut.tpLinkComms, line 71
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 72
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 73
				break // library marker davegut.tpLinkComms, line 74
			case 3: // library marker davegut.tpLinkComms, line 75
				logData << [configure: configure(true), // library marker davegut.tpLinkComms, line 76
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 77
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 78
			default: // library marker davegut.tpLinkComms, line 79
				if (device.currentValue("commsError") == "false") { // library marker davegut.tpLinkComms, line 80
					logData << [setCommsError: setCommsError(true)] // library marker davegut.tpLinkComms, line 81
				} // library marker davegut.tpLinkComms, line 82
				logData << [retries: "disabled"] // library marker davegut.tpLinkComms, line 83
				break // library marker davegut.tpLinkComms, line 84
		} // library marker davegut.tpLinkComms, line 85
		state.errorCount = count // library marker davegut.tpLinkComms, line 86
	} else { // library marker davegut.tpLinkComms, line 87
		logData << [status: "noCommandToRetry"] // library marker davegut.tpLinkComms, line 88
	} // library marker davegut.tpLinkComms, line 89
	logInfo(logData) // library marker davegut.tpLinkComms, line 90
} // library marker davegut.tpLinkComms, line 91

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 93
	def cmdData = new JSONObject(state.lastCmd) // library marker davegut.tpLinkComms, line 94
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 95
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 96
} // library marker davegut.tpLinkComms, line 97

def setCommsError(status) { // library marker davegut.tpLinkComms, line 99
	if (device.currentValue("commsError") == "true" && status == false) { // library marker davegut.tpLinkComms, line 100
		updateAttr("commsError", "false") // library marker davegut.tpLinkComms, line 101
		setPollInterval() // library marker davegut.tpLinkComms, line 102
		unschedule(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 103
		return "false" // library marker davegut.tpLinkComms, line 104
	} else if (device.currentValue("commsError") == "false" && status == true) { // library marker davegut.tpLinkComms, line 105
		updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 106
		setPollInterval("30 min") // library marker davegut.tpLinkComms, line 107
		runEvery5Minutes(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 108
		return "true" // library marker davegut.tpLinkComms, line 109
	} // library marker davegut.tpLinkComms, line 110
} // library marker davegut.tpLinkComms, line 111

def errorDeviceHandshake() {  // library marker davegut.tpLinkComms, line 113
	logInfo([method: "errorDeviceHandshake"]) // library marker davegut.tpLinkComms, line 114
	deviceHandshake() // library marker davegut.tpLinkComms, line 115
} // library marker davegut.tpLinkComms, line 116

// ~~~~~ end include (56) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (65) davegut.tpLinkCrypto ~~~~~
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

//	===== AES Handshake and Login ===== // library marker davegut.tpLinkCrypto, line 17
def aesHandshake(baseUrl = getDataValue("baseUrl"), devData = null) { // library marker davegut.tpLinkCrypto, line 18
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.tpLinkCrypto, line 19
	Map rsaKey = getRsaKey() // library marker davegut.tpLinkCrypto, line 20
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKey.public}-----END PUBLIC KEY-----\n" // library marker davegut.tpLinkCrypto, line 21
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.tpLinkCrypto, line 22
	Map reqParams = [uri: baseUrl, // library marker davegut.tpLinkCrypto, line 23
					 body: new groovy.json.JsonBuilder(cmdBody).toString(), // library marker davegut.tpLinkCrypto, line 24
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 25
					 timeout: 10] // library marker davegut.tpLinkCrypto, line 26
	asyncPost(reqParams, "parseAesHandshake", reqData) // library marker davegut.tpLinkCrypto, line 27
} // library marker davegut.tpLinkCrypto, line 28

def parseAesHandshake(resp, data){ // library marker davegut.tpLinkCrypto, line 30
	Map logData = [method: "parseAesHandshake"] // library marker davegut.tpLinkCrypto, line 31
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 32
		try { // library marker davegut.tpLinkCrypto, line 33
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 34
			Map cmdResp =  new JsonSlurper().parseText(resp.data) // library marker davegut.tpLinkCrypto, line 35
			//	cookie // library marker davegut.tpLinkCrypto, line 36
			def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 37
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 38
			//	keys // library marker davegut.tpLinkCrypto, line 39
			byte[] privateKeyBytes = getRsaKey().private.decodeBase64() // library marker davegut.tpLinkCrypto, line 40
			byte[] deviceKeyBytes = cmdResp.result.key.getBytes("UTF-8").decodeBase64() // library marker davegut.tpLinkCrypto, line 41
    		Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.tpLinkCrypto, line 42
			instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.tpLinkCrypto, line 43
			byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.tpLinkCrypto, line 44
			byte[] encKey = cryptoArray[0..15] // library marker davegut.tpLinkCrypto, line 45
			byte[] encIv = cryptoArray[16..31] // library marker davegut.tpLinkCrypto, line 46
			logData << [respStatus: "Cookies/Keys Updated", cookie: cookie, // library marker davegut.tpLinkCrypto, line 47
						encKey: encKey, encIv: encIv] // library marker davegut.tpLinkCrypto, line 48
			String password = encPassword // library marker davegut.tpLinkCrypto, line 49
			String username = encUsername // library marker davegut.tpLinkCrypto, line 50
			if (device) { // library marker davegut.tpLinkCrypto, line 51
				password = parent.encPassword // library marker davegut.tpLinkCrypto, line 52
				username = parent.encUsername // library marker davegut.tpLinkCrypto, line 53
				device.updateSetting("cookie",[type:"password", value: cookie]) // library marker davegut.tpLinkCrypto, line 54
				device.updateSetting("encKey",[type:"password", value: encKey]) // library marker davegut.tpLinkCrypto, line 55
				device.updateSetting("encIv",[type:"password", value: encIv]) // library marker davegut.tpLinkCrypto, line 56
			} else { // library marker davegut.tpLinkCrypto, line 57
				reqData << [cookie: cookie, encIv: encIv, encKey: encKey] // library marker davegut.tpLinkCrypto, line 58
			} // library marker davegut.tpLinkCrypto, line 59
			Map cmdBody = [method: "login_device", // library marker davegut.tpLinkCrypto, line 60
						   params: [password: password, // library marker davegut.tpLinkCrypto, line 61
									username: username], // library marker davegut.tpLinkCrypto, line 62
						   requestTimeMils: 0] // library marker davegut.tpLinkCrypto, line 63
			def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 64
			Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 65
						   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 66
			Map reqParams = [uri: reqData.baseUrl, // library marker davegut.tpLinkCrypto, line 67
							  body: reqBody, // library marker davegut.tpLinkCrypto, line 68
							  timeout:10,  // library marker davegut.tpLinkCrypto, line 69
							  headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 70
							  contentType: "application/json", // library marker davegut.tpLinkCrypto, line 71
							  requestContentType: "application/json"] // library marker davegut.tpLinkCrypto, line 72
			asyncPost(reqParams, "parseAesLogin", reqData) // library marker davegut.tpLinkCrypto, line 73
			logDebug(logData) // library marker davegut.tpLinkCrypto, line 74
		} catch (err) { // library marker davegut.tpLinkCrypto, line 75
			logData << [respStatus: "ERROR parsing HTTP resp.data", // library marker davegut.tpLinkCrypto, line 76
						respData: resp.data, error: err] // library marker davegut.tpLinkCrypto, line 77
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 78
		} // library marker davegut.tpLinkCrypto, line 79
	} else { // library marker davegut.tpLinkCrypto, line 80
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 81
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 82
	} // library marker davegut.tpLinkCrypto, line 83
} // library marker davegut.tpLinkCrypto, line 84

def parseAesLogin(resp, data) { // library marker davegut.tpLinkCrypto, line 86
	if (device) { // library marker davegut.tpLinkCrypto, line 87
		Map logData = [method: "parseAesLogin"] // library marker davegut.tpLinkCrypto, line 88
		if (resp.status == 200) { // library marker davegut.tpLinkCrypto, line 89
			if (resp.json.error_code == 0) { // library marker davegut.tpLinkCrypto, line 90
				try { // library marker davegut.tpLinkCrypto, line 91
					byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 92
					byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 93
					def clearResp = aesDecrypt(resp.json.result.response, encKey, encIv) // library marker davegut.tpLinkCrypto, line 94
					Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.tpLinkCrypto, line 95
					if (cmdResp.error_code == 0) { // library marker davegut.tpLinkCrypto, line 96
						def token = cmdResp.result.token // library marker davegut.tpLinkCrypto, line 97
						logData << [respStatus: "OK", token: token] // library marker davegut.tpLinkCrypto, line 98
						device.updateSetting("token",[type:"password", value: token]) // library marker davegut.tpLinkCrypto, line 99
						state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 100
						if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkCrypto, line 101
							logData << [setCommsError: setCommsError(false)] // library marker davegut.tpLinkCrypto, line 102
						} // library marker davegut.tpLinkCrypto, line 103
						logDebug(logData) // library marker davegut.tpLinkCrypto, line 104
					} else { // library marker davegut.tpLinkCrypto, line 105
						logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.tpLinkCrypto, line 106
									error_code: cmdResp.error_code, // library marker davegut.tpLinkCrypto, line 107
									check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.tpLinkCrypto, line 108
						logWarn(logData) // library marker davegut.tpLinkCrypto, line 109
					} // library marker davegut.tpLinkCrypto, line 110
				} catch (err) { // library marker davegut.tpLinkCrypto, line 111
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.tpLinkCrypto, line 112
								error: err] // library marker davegut.tpLinkCrypto, line 113
					logWarn(logData) // library marker davegut.tpLinkCrypto, line 114
				} // library marker davegut.tpLinkCrypto, line 115
			} else { // library marker davegut.tpLinkCrypto, line 116
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.tpLinkCrypto, line 117
							respJson: resp.json] // library marker davegut.tpLinkCrypto, line 118
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 119
			} // library marker davegut.tpLinkCrypto, line 120
		} else { // library marker davegut.tpLinkCrypto, line 121
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.tpLinkCrypto, line 122
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 123
		} // library marker davegut.tpLinkCrypto, line 124
	} else { // library marker davegut.tpLinkCrypto, line 125
		getAesToken(resp, data.data) // library marker davegut.tpLinkCrypto, line 126
	} // library marker davegut.tpLinkCrypto, line 127
} // library marker davegut.tpLinkCrypto, line 128

//	===== KLAP Handshake ===== // library marker davegut.tpLinkCrypto, line 130
def klapHandshake(baseUrl = getDataValue("baseUrl"), localHash = parent.localHash, devData = null) { // library marker davegut.tpLinkCrypto, line 131
	byte[] localSeed = new byte[16] // library marker davegut.tpLinkCrypto, line 132
	new Random().nextBytes(localSeed) // library marker davegut.tpLinkCrypto, line 133
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkCrypto, line 134
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkCrypto, line 135
					 body: localSeed, // library marker davegut.tpLinkCrypto, line 136
					 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 137
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 138
					 timeout:10] // library marker davegut.tpLinkCrypto, line 139
	asyncPost(reqParams, "parseKlapHandshake", reqData) // library marker davegut.tpLinkCrypto, line 140
} // library marker davegut.tpLinkCrypto, line 141

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkCrypto, line 143
	Map logData = [method: "parseKlapHandshake", data: data] // library marker davegut.tpLinkCrypto, line 144
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 145
		try { // library marker davegut.tpLinkCrypto, line 146
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 147
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkCrypto, line 148
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkCrypto, line 149
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkCrypto, line 150
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkCrypto, line 151
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkCrypto, line 152
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 153
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkCrypto, line 154
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkCrypto, line 155
				//	cookie // library marker davegut.tpLinkCrypto, line 156
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 157
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 158
				logData << [cookie: cookie] // library marker davegut.tpLinkCrypto, line 159
				//	seqNo and encIv // library marker davegut.tpLinkCrypto, line 160
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 161
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 162
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkCrypto, line 163

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkCrypto, line 165
				atomicState.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 166

//				if (device) {  // library marker davegut.tpLinkCrypto, line 168
//				} // library marker davegut.tpLinkCrypto, line 169

				logData << [seqNo: seqNo, encIv: fullIv[0..11]] // library marker davegut.tpLinkCrypto, line 171
				//	encKey // library marker davegut.tpLinkCrypto, line 172
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 173
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkCrypto, line 174
				logData << [encKey: encKey] // library marker davegut.tpLinkCrypto, line 175
				//	encSig // library marker davegut.tpLinkCrypto, line 176
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 177
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkCrypto, line 178
				if (device) { // library marker davegut.tpLinkCrypto, line 179
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkCrypto, line 180
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkCrypto, line 181
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkCrypto, line 182
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkCrypto, line 183
				} else { // library marker davegut.tpLinkCrypto, line 184
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkCrypto, line 185
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkCrypto, line 186
				} // library marker davegut.tpLinkCrypto, line 187
				logData << [encSig: encSig] // library marker davegut.tpLinkCrypto, line 188
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 189
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkCrypto, line 190
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkCrypto, line 191
								 body: body, // library marker davegut.tpLinkCrypto, line 192
								 timeout:10, // library marker davegut.tpLinkCrypto, line 193
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 194
								 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 195
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 196
				asyncPost(reqParams, "parseKlapHandshake2", reqData) // library marker davegut.tpLinkCrypto, line 197
			} else { // library marker davegut.tpLinkCrypto, line 198
				logData << [respStatus: "ERROR: locakAuthHash != serverHash", // library marker davegut.tpLinkCrypto, line 199
							localAuthHash: localAuthHash, serverHash: serverHash] // library marker davegut.tpLinkCrypto, line 200
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 201
			} // library marker davegut.tpLinkCrypto, line 202
		} catch (err) { // library marker davegut.tpLinkCrypto, line 203
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkCrypto, line 204
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 205
		} // library marker davegut.tpLinkCrypto, line 206
	} else { // library marker davegut.tpLinkCrypto, line 207
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 208
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 209
	} // library marker davegut.tpLinkCrypto, line 210
} // library marker davegut.tpLinkCrypto, line 211

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkCrypto, line 213
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkCrypto, line 214
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkCrypto, line 215
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkCrypto, line 216
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 217
		if (device && device.currentValue("commsError") == "true") { // library marker davegut.tpLinkCrypto, line 218
			logData << [setCommsError: setCommsError(false)] // library marker davegut.tpLinkCrypto, line 219
		} // library marker davegut.tpLinkCrypto, line 220
		logDebug(logData) // library marker davegut.tpLinkCrypto, line 221
	} else { // library marker davegut.tpLinkCrypto, line 222
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkCrypto, line 223
					resp: resp.properties] // library marker davegut.tpLinkCrypto, line 224
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 225
	} // library marker davegut.tpLinkCrypto, line 226
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkCrypto, line 227
} // library marker davegut.tpLinkCrypto, line 228

//	===== Comms Support ===== // library marker davegut.tpLinkCrypto, line 230
def getKlapParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 231
	Map reqParams = [timeout: 10, headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 232
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkCrypto, line 233
	state.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 234
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 235
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 236
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkCrypto, line 237
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 238

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkCrypto, line 240
									encSig, seqNo) // library marker davegut.tpLinkCrypto, line 241
	reqParams << [uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkCrypto, line 242
				  body: encryptedData.cipherData, // library marker davegut.tpLinkCrypto, line 243
				  contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 244
				  requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 245
	return reqParams // library marker davegut.tpLinkCrypto, line 246
} // library marker davegut.tpLinkCrypto, line 247

def getAesParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 249
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 250
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 251
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 252
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 253
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 254
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkCrypto, line 255
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkCrypto, line 256
					 contentType: "application/json", // library marker davegut.tpLinkCrypto, line 257
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 258
					 timeout: 10, // library marker davegut.tpLinkCrypto, line 259
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 260
	return reqParams // library marker davegut.tpLinkCrypto, line 261
} // library marker davegut.tpLinkCrypto, line 262

def parseKlapData(resp) { // library marker davegut.tpLinkCrypto, line 264
	Map parseData = [parseMethod: "parseKlapData"] // library marker davegut.tpLinkCrypto, line 265
	try { // library marker davegut.tpLinkCrypto, line 266
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 267
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 268
		int seqNo = state.seqNo // library marker davegut.tpLinkCrypto, line 269
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkCrypto, line 270
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkCrypto, line 271
														   encIv, seqNo)) // library marker davegut.tpLinkCrypto, line 272
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 273
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 274
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkCrypto, line 275
			parseData << [setCommsError: setCommsError(false)] // library marker davegut.tpLinkCrypto, line 276
		} // library marker davegut.tpLinkCrypto, line 277
	} catch (err) { // library marker davegut.tpLinkCrypto, line 278
		parseData << [status: "deviceDataParseError", error: err] // library marker davegut.tpLinkCrypto, line 279
		handleCommsError() // library marker davegut.tpLinkCrypto, line 280
	} // library marker davegut.tpLinkCrypto, line 281
	return parseData // library marker davegut.tpLinkCrypto, line 282
} // library marker davegut.tpLinkCrypto, line 283

def parseAesData(resp) { // library marker davegut.tpLinkCrypto, line 285
	Map parseData = [parseMethod: "parseAesData"] // library marker davegut.tpLinkCrypto, line 286
	try { // library marker davegut.tpLinkCrypto, line 287
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 288
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 289
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCrypto, line 290
														 encKey, encIv)) // library marker davegut.tpLinkCrypto, line 291
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 292
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 293
		if (device && device.currentValue("commsError") == "true") { // library marker davegut.tpLinkCrypto, line 294
			parseData << [setCommsError: setCommsError(false)] // library marker davegut.tpLinkCrypto, line 295
		} // library marker davegut.tpLinkCrypto, line 296
	} catch (err) { // library marker davegut.tpLinkCrypto, line 297
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 298
		handleCommsError() // library marker davegut.tpLinkCrypto, line 299
	} // library marker davegut.tpLinkCrypto, line 300
	return parseData // library marker davegut.tpLinkCrypto, line 301
} // library marker davegut.tpLinkCrypto, line 302

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 304
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 305
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 306
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 307
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 308
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 309
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 310
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 311
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 312

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 314
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 315
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 316
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 317
} // library marker davegut.tpLinkCrypto, line 318

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 320
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 321
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 322
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 323
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 324
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 325
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 326
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 327
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 328
} // library marker davegut.tpLinkCrypto, line 329

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 331
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 332
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 333
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 334
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 335
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 336
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 337
} // library marker davegut.tpLinkCrypto, line 338

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 340
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 341
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 342
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 343
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 344
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 345
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 346
} // library marker davegut.tpLinkCrypto, line 347

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 349
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 350
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 351
	md.update(data) // library marker davegut.tpLinkCrypto, line 352
	return md.digest() // library marker davegut.tpLinkCrypto, line 353
} // library marker davegut.tpLinkCrypto, line 354

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 356
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 357
	return new String(arr) // library marker davegut.tpLinkCrypto, line 358
} // library marker davegut.tpLinkCrypto, line 359

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 361
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 362
	try { // library marker davegut.tpLinkCrypto, line 363
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 364
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 365
	} catch (error) { // library marker davegut.tpLinkCrypto, line 366
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 367
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 368
	} // library marker davegut.tpLinkCrypto, line 369
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 370
} // library marker davegut.tpLinkCrypto, line 371

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 373
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 374
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 375
	return byteValue // library marker davegut.tpLinkCrypto, line 376
} // library marker davegut.tpLinkCrypto, line 377

def getRsaKey() { // library marker davegut.tpLinkCrypto, line 379
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkCrypto, line 380
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkCrypto, line 381
} // library marker davegut.tpLinkCrypto, line 382

// ~~~~~ end include (65) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (49) davegut.Logging ~~~~~
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

// ~~~~~ end include (49) davegut.Logging ~~~~~
