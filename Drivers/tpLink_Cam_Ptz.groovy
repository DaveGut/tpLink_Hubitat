/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Supports:  Tapo PTZ Cameras

Known issues:
1.	Not all setting functions available on Hubitat.  User should request.
2.	Driver does not work for Tapo H200/H500 cameras.

Note on Battery Capable Devices: These devices are either solely battery powered or
connected to a power source to continually charge the battery.  To achieve the maximum
battery life, the functions are somewhat limited.  Some limitations
a.	Non- ONIF and RSTP video format.
b.	If not wired, the use of polling will have sever impact on battery time between
	recharge.  Consider using other means to bring data into Hubitat and turn off
	polling.
	1)	Link to Amazon and create rules linked to Hubitat virtual device.
	2)	Use Tapo Smart Actions linking to a bulb/dimmer in your home and use bulb-level
		to set motion/ring attributes plus set inactive (I use 10-20 to set and 0 to
		set inactive).
=================================================================================================*/
metadata {
	definition (name: "TpLink Cam Ptz", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_Cam_Ptz.groovy")
	{
		command "patrolMode", [
			[name: "Patrol between defined viewPoints. ",
			 constraints: ["on", "off"], type: "ENUM"],
			[name: "view duration (secs), ",
			 constraints: [10, 20, 30], type: "ENUM"]]
		attribute "patrolMode", "string"
		command "setViewpoint", [[
			name: "Go to camera viewpoint. ",
			constraints: [1, 2, 3, 4, 5, 6, 7, 8], type: "ENUM"]]
		attribute "currViewpoint", "string"
	}
	preferences {
		commonPreferences()
	}
}

def installed() {
	state.viewPoints = []
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	logInfo(logData)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}

def updDevSettings() {
	List requests = [
	]
	comUpdDevSettings(requests)
	return "Device Settings Updated"
}	

def refresh() {
	List requests = [
		[method:"getPresetConfig", params: [preset:[name: "preset"]]]
	]
	comRefresh(requests)
	logDebug([method: "refresh"])
}

def parse_getPresetConfig(devResp) {
	state.viewPoints = devResp.preset.preset.id
	return [viewPoints: vPoints]
}

def patrolMode(onOff = "on", vTime = 10) {
	Map logData = [method: "patrolMode", onOff: onOff, lag: lag, viewPoints: state.viewPoints]
	if (state.viewPoints.size() < 2) {
		logData << [error: "Must have at least 2 viewPoints set for patrol mode"]
		logWarn(logData)
		return
	}
	if (onOff == "off") {
		stopPatrol()
	} else {
		moveToPreset(state.viewPoints[0])
		schedule("*/${vTime} * * * * ?", "runPatrol")
		runIn(300, stopPatrol)
	}
	sendEvent(name: "patrolMode", value: onOff)
	logData << [patrolMode: onOff]
	logDebug(logData)
}

def runPatrol() {
	def nextVpIndex = state.viewPoints.indexOf(state.lastViewpoint) + 1
	if (nextVpIndex >= state.viewPoints.size()) { nextVpIndex = 0 }
	moveToPreset(state.viewPoints[nextVpIndex])
}

def stopPatrol() {
	unschedule("runPatrol")
	sendEvent(name: "patrolMode", value: "off")
	logDebug([method: "patrolMode", patrolMode: "off"])
}

def setViewpoint(presetId) {
	Map logData = [method: "setViewpoint", viewpointNo: presetId, vpIds: state.viewPoints]
	if (state.viewPoints.contains(presetId)) {
		logData << [lastViewpoint: presetId]
		moveToPreset(presetId)
		logDebug(logData)
	} else {
		logData << [status: "ERROR", data: "Viewpoint ${presetId} is not defined."]
		logWarn(logData)
	}
}

def moveToPreset(presetId) {
	List requests = [[method: "motorMoveToPreset", params: [preset: [goto_preset: [id:presetId]]]]]
	state.lastViewpoint = presetId.toString()
	sendDevCmd(requests, "moveToPreset", "parseUpdates")
}








// ~~~~~ start include (377) davegut.tpLinkCamCommon ~~~~~
library ( // library marker davegut.tpLinkCamCommon, line 1
	name: "tpLinkCamCommon", // library marker davegut.tpLinkCamCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCamCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCamCommon, line 4
	description: "Common Camera driver methods", // library marker davegut.tpLinkCamCommon, line 5
	category: "utilities", // library marker davegut.tpLinkCamCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkCamCommon, line 7
) // library marker davegut.tpLinkCamCommon, line 8

capability "Polling" // library marker davegut.tpLinkCamCommon, line 10
capability "Refresh" // library marker davegut.tpLinkCamCommon, line 11
capability "Battery" // library marker davegut.tpLinkCamCommon, line 12
capability "Motion Sensor" // library marker davegut.tpLinkCamCommon, line 13
command "displayPrivacy", [[name: "Display Privacy", constraints: ["on", "off"], type: "ENUM"]] // library marker davegut.tpLinkCamCommon, line 14
attribute "privacy", "string" // library marker davegut.tpLinkCamCommon, line 15
command "deviceHandshake" // library marker davegut.tpLinkCamCommon, line 16
attribute "batteryStatus", "string" // library marker davegut.tpLinkCamCommon, line 17
attribute "commsError", "string" // library marker davegut.tpLinkCamCommon, line 18

def commonPreferences() { // library marker davegut.tpLinkCamCommon, line 20
	input ("motionDetect", "enum", title: "Motion Detection and Sensitivity", options: ["off", "low", "medium", "high"]) // library marker davegut.tpLinkCamCommon, line 21
	if (getDataValue("alert") == "true") { // library marker davegut.tpLinkCamCommon, line 22
		input ("alertConf", "enum", title: "Camera Alert Type", options: ["off", "both", "sound", "light"]) // library marker davegut.tpLinkCamCommon, line 23
	} // library marker davegut.tpLinkCamCommon, line 24
	List ledOpts = ["on", "off"] // library marker davegut.tpLinkCamCommon, line 25
	if (getDataValue("ledVer") == "2") { ledOpts = ["auto", "off"] } // library marker davegut.tpLinkCamCommon, line 26
	input ("ledRule", "enum", title: "LED Mode", options: ledOpts) // library marker davegut.tpLinkCamCommon, line 27
	input ("pollInterval", "enum", title: "Motion Poll Interval",   // library marker davegut.tpLinkCamCommon, line 28
		   options: ["off", "5", "10", "15", "30"], defaultValue: "15") // library marker davegut.tpLinkCamCommon, line 29
	input ("rebootDev", "bool", title: "Reboot Device", defaultValue: false) // library marker davegut.tpLinkCamCommon, line 30
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkCamCommon, line 31
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkCamCommon, line 32
} // library marker davegut.tpLinkCamCommon, line 33

def commonInstalled() { // library marker davegut.tpLinkCamCommon, line 35
	sendEvent(name: "motion", value: "inactive") // library marker davegut.tpLinkCamCommon, line 36
	updateAttr("commsError", "false") // library marker davegut.tpLinkCamCommon, line 37
	state.lastAlarmTime = 0 // library marker davegut.tpLinkCamCommon, line 38
	state.errorCount = 0 // library marker davegut.tpLinkCamCommon, line 39
	state.pollInterval = "30" // library marker davegut.tpLinkCamCommon, line 40
	Map logData = [configure: configure()] // library marker davegut.tpLinkCamCommon, line 41
	runIn(7, updated) // library marker davegut.tpLinkCamCommon, line 42
	return logData // library marker davegut.tpLinkCamCommon, line 43
} // library marker davegut.tpLinkCamCommon, line 44

def commonUpdated() { // library marker davegut.tpLinkCamCommon, line 46
	unschedule() // library marker davegut.tpLinkCamCommon, line 47
	sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkCamCommon, line 48
	sendEvent(name: "motion", value: "inactive") // library marker davegut.tpLinkCamCommon, line 49
	state.errorCount = 0 // library marker davegut.tpLinkCamCommon, line 50
	if (state.pollInterval == null) {state.pollInterval = "30" } // library marker davegut.tpLinkCamCommon, line 51
	Map logData = [commsError: "cleared"] // library marker davegut.tpLinkCamCommon, line 52
	logData << setLogsOff() // library marker davegut.tpLinkCamCommon, line 53
	if (rebootDev == true) { // library marker davegut.tpLinkCamCommon, line 54
		List requests = [[method: "rebootDevice", params: [system: [reboot: "" ]]]] // library marker davegut.tpLinkCamCommon, line 55
		sendDevCmd(requests, "rebootDev", "finishReboot") // library marker davegut.tpLinkCamCommon, line 56
		logData << [rebootDevice: "device reboot being attempted"] // library marker davegut.tpLinkCamCommon, line 57
	} else { // library marker davegut.tpLinkCamCommon, line 58
		logData << setPollInterval(pollInterval) // library marker davegut.tpLinkCamCommon, line 59
		if (getDataValue("power") != "BATTERY") { // library marker davegut.tpLinkCamCommon, line 60
			runEvery1Hour(refresh) // library marker davegut.tpLinkCamCommon, line 61
			logData << [refreshInterval: "1 Hour"] // library marker davegut.tpLinkCamCommon, line 62
			runEvery1Hour(deviceHandshake) // library marker davegut.tpLinkCamCommon, line 63
			logData << [deviceHandshake: "1 Hour"] // library marker davegut.tpLinkCamCommon, line 64
		} else { // library marker davegut.tpLinkCamCommon, line 65
			runEvery1Hour(udpRefresh) // library marker davegut.tpLinkCamCommon, line 66
			logData << [ udpRefreshInterval: "1 hour" ] // library marker davegut.tpLinkCamCommon, line 67
		} // library marker davegut.tpLinkCamCommon, line 68
		updDevSettings() // library marker davegut.tpLinkCamCommon, line 69
		runIn(10, refresh) // library marker davegut.tpLinkCamCommon, line 70
	} // library marker davegut.tpLinkCamCommon, line 71
	return logData // library marker davegut.tpLinkCamCommon, line 72
} // library marker davegut.tpLinkCamCommon, line 73

def finishReboot(resp, data = null) { // library marker davegut.tpLinkCamCommon, line 75
	Map logData = [finishReboot: "Takes 35 Seconds to finish"] // library marker davegut.tpLinkCamCommon, line 76
	logData << [wait: "<b>20s for device to reconnect to LAN</b>", action: "executing deviceHandshake"] // library marker davegut.tpLinkCamCommon, line 77
	device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.tpLinkCamCommon, line 78
	runIn(30, configure) // library marker davegut.tpLinkCamCommon, line 79
	logInfo(logData) // library marker davegut.tpLinkCamCommon, line 80
} // library marker davegut.tpLinkCamCommon, line 81

def comUpdDevSettings(requests) { // library marker davegut.tpLinkCamCommon, line 83
	def motSens = "medium" // library marker davegut.tpLinkCamCommon, line 84
	def motDet = motionDetect // library marker davegut.tpLinkCamCommon, line 85
	if (motionDetect != "off") { motDet = "on"; motSens = motionDetect } // library marker davegut.tpLinkCamCommon, line 86
	requests << [method:"setDetectionConfig",params:[motion_detection:[ // library marker davegut.tpLinkCamCommon, line 87
		motion_det:[sensitivity: motSens, enabled: motDet]]]] // library marker davegut.tpLinkCamCommon, line 88
	if (state.ledStatus != ledRule) { // library marker davegut.tpLinkCamCommon, line 89
		requests << [method:"setLedStatus", params:[led:[config:[enabled:ledRule]]]] // library marker davegut.tpLinkCamCommon, line 90
	} // library marker davegut.tpLinkCamCommon, line 91
	if (getDataValue("alert") == "true") { // library marker davegut.tpLinkCamCommon, line 92
		if (alertConf == "off") { // library marker davegut.tpLinkCamCommon, line 93
			requests << [method:"setAlertConfig",params: [msg_alarm: [chn1_msg_alarm_info: [enabled: "off"]]]] // library marker davegut.tpLinkCamCommon, line 94
		} else { // library marker davegut.tpLinkCamCommon, line 95
			def alarmMode = [alertConf] // library marker davegut.tpLinkCamCommon, line 96
			if (alertConf == "both") { alarmMode = ["sound", "light"] } // library marker davegut.tpLinkCamCommon, line 97
			requests << [method:"setAlertConfig",params: [msg_alarm:[chn1_msg_alarm_info: [alarm_mode: alarmMode, enabled: "on"]]]] // library marker davegut.tpLinkCamCommon, line 98
		} // library marker davegut.tpLinkCamCommon, line 99
	} // library marker davegut.tpLinkCamCommon, line 100
	if (alertConf== "off") { // library marker davegut.tpLinkCamCommon, line 101
		requests << [method:"setAlertConfig",params: [msg_alarm: [chn1_msg_alarm_info: [enabled: "off"]]]] // library marker davegut.tpLinkCamCommon, line 102
	} else { // library marker davegut.tpLinkCamCommon, line 103
		def alarmMode = [config] // library marker davegut.tpLinkCamCommon, line 104
		if (alertConf == "both") { alarmMode = ["sound", "light"] } // library marker davegut.tpLinkCamCommon, line 105
		requests << [method:"setAlertConfig",params: [msg_alarm:[chn1_msg_alarm_info: [alarm_mode: alarmMode, enabled: "on"]]]] // library marker davegut.tpLinkCamCommon, line 106
	} // library marker davegut.tpLinkCamCommon, line 107
	sendDevCmd(requests, "updateDevSettings", "parseUpdates") // library marker davegut.tpLinkCamCommon, line 108
} // library marker davegut.tpLinkCamCommon, line 109

def comRefresh(requests) { // library marker davegut.tpLinkCamCommon, line 111
	state.udpRefresh = true // library marker davegut.tpLinkCamCommon, line 112
	requests << [method:"getLedStatus",params:[led:[name:["config"]]]] // library marker davegut.tpLinkCamCommon, line 113
	requests << [method:"getDetectionConfig",params:[motion_detection:[name:["motion_det"]]]] // library marker davegut.tpLinkCamCommon, line 114
	requests << [method:"getLensMaskConfig",params:[lens_mask:[name:["lens_mask_info"]]]] // library marker davegut.tpLinkCamCommon, line 115
	if (getDataValue("alert") == "true") { // library marker davegut.tpLinkCamCommon, line 116
		requests << [method:"getAlertConfig", params:[msg_alarm:[name:"chn1_msg_alarm_info"]]] // library marker davegut.tpLinkCamCommon, line 117
	} // library marker davegut.tpLinkCamCommon, line 118
	logDebug([refresh: requests]) // library marker davegut.tpLinkCamCommon, line 119
	sendDevCmd(requests, "refresh", "parseUpdates") // library marker davegut.tpLinkCamCommon, line 120
} // library marker davegut.tpLinkCamCommon, line 121

def setPollInterval(interval) { // library marker davegut.tpLinkCamCommon, line 123
	Map params = [:] // library marker davegut.tpLinkCamCommon, line 124
	unschedule("encrPoll") // library marker davegut.tpLinkCamCommon, line 125
	unschedule("udpPoll") // library marker davegut.tpLinkCamCommon, line 126
	def pollMethod = "encrPoll" // library marker davegut.tpLinkCamCommon, line 127
	if (getDataValue("power")) { pollMethod = "udpPoll" } // library marker davegut.tpLinkCamCommon, line 128
	if (interval != "off") { // library marker davegut.tpLinkCamCommon, line 129
		if (interval == "error") { // library marker davegut.tpLinkCamCommon, line 130
		//	Called if commsError asserted to reduce error handling. // library marker davegut.tpLinkCamCommon, line 131
		//	Will reset to state.pollInterval when error cleared. // library marker davegut.tpLinkCamCommon, line 132
			runEvery5Minutes("${pollMethod}") // library marker davegut.tpLinkCamCommon, line 133
			interval = "5 minutes" // library marker davegut.tpLinkCamCommon, line 134
		} else { // library marker davegut.tpLinkCamCommon, line 135
			schedule("3/${interval} * * * * ?", "${pollMethod}") // library marker davegut.tpLinkCamCommon, line 136
		} // library marker davegut.tpLinkCamCommon, line 137
	} // library marker davegut.tpLinkCamCommon, line 138
	return [MotionPollData: [method: pollMethod, interval: interval]] // library marker davegut.tpLinkCamCommon, line 139
} // library marker davegut.tpLinkCamCommon, line 140

def poll() { // library marker davegut.tpLinkCamCommon, line 142
	if (getDataValue("power")) { udpPoll() } // library marker davegut.tpLinkCamCommon, line 143
	else { encrPoll() } // library marker davegut.tpLinkCamCommon, line 144
} // library marker davegut.tpLinkCamCommon, line 145

def encrPoll() { // library marker davegut.tpLinkCamCommon, line 147
	requests = [[method:"getLastAlarmInfo", params:[system:[name:["last_alarm_info"]]]]] // library marker davegut.tpLinkCamCommon, line 148
	sendDevCmd(requests, "encrPoll", "parseUpdates") // library marker davegut.tpLinkCamCommon, line 149
} // library marker davegut.tpLinkCamCommon, line 150

def udpPoll() { // library marker davegut.tpLinkCamCommon, line 152
	def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.tpLinkCamCommon, line 153
	sendFindCmd(getDataValue("devIp"), "20004", cmdData, "parseUdpPoll") // library marker davegut.tpLinkCamCommon, line 154
} // library marker davegut.tpLinkCamCommon, line 155

def parseUdpPoll(response) { // library marker davegut.tpLinkCamCommon, line 157
	def respData = parseLanMessage(response) // library marker davegut.tpLinkCamCommon, line 158
	if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.tpLinkCamCommon, line 159
		byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.tpLinkCamCommon, line 160
		String payloadString = new String(payloadByte) // library marker davegut.tpLinkCamCommon, line 161
		Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.tpLinkCamCommon, line 162
		if (payload.last_alarm_time > state.lastAlarmTime) { // library marker davegut.tpLinkCamCommon, line 163
			encrPoll() // library marker davegut.tpLinkCamCommon, line 164
		} // library marker davegut.tpLinkCamCommon, line 165
		if (state.udpRefresh == true) { // library marker davegut.tpLinkCamCommon, line 166
			udpUpdate(payload) // library marker davegut.tpLinkCamCommon, line 167
			state.udpRefresh = false // library marker davegut.tpLinkCamCommon, line 168
		} // library marker davegut.tpLinkCamCommon, line 169
	} else { // library marker davegut.tpLinkCamCommon, line 170
		logWarn([parseUdpPoll: [status: "Invalid UDP Poll Return", msg: "try configure"]]) // library marker davegut.tpLinkCamCommon, line 171
	} // library marker davegut.tpLinkCamCommon, line 172
} // library marker davegut.tpLinkCamCommon, line 173

def udpRefresh() { state.udpRefresh = true } // library marker davegut.tpLinkCamCommon, line 175

def udpUpdate(payload) { // library marker davegut.tpLinkCamCommon, line 177
	String batStatus = "normal" // library marker davegut.tpLinkCamCommon, line 178
	if (payload.battery_charging == true) { batStatus = "charging" } // library marker davegut.tpLinkCamCommon, line 179
	else if (payload.low_battery == true) { batStatus = "low" } // library marker davegut.tpLinkCamCommon, line 180
	sendEvent(name: "batteryStatus", value: batStatus) // library marker davegut.tpLinkCamCommon, line 181
	sendEvent(name: "battery", value: payload.battery_percent) // library marker davegut.tpLinkCamCommon, line 182
	String privacy = "private" // library marker davegut.tpLinkCamCommon, line 183
	if (payload.lens_mask == "off") { privacy = "notPrivate" } // library marker davegut.tpLinkCamCommon, line 184
	sendEvent(name: "privacy", value: privacy) // library marker davegut.tpLinkCamCommon, line 185
	Map logData = [udpUpdate: [batteryStatus: batStatus, // library marker davegut.tpLinkCamCommon, line 186
				   battery: payload.battery_percent, privacy: "privacy"]] // library marker davegut.tpLinkCamCommon, line 187
	logDebug(logData) // library marker davegut.tpLinkCamCommon, line 188
} // library marker davegut.tpLinkCamCommon, line 189

def setInactive() { sendEvent(name: "motion", value: "inactive") } // library marker davegut.tpLinkCamCommon, line 191

def displayPrivacy(onOff) { // library marker davegut.tpLinkCamCommon, line 193
	List requests = [ // library marker davegut.tpLinkCamCommon, line 194
		[method:"setLensMaskConfig", params:[lens_mask:[lens_mask_info:[enabled: onOff]]]], // library marker davegut.tpLinkCamCommon, line 195
		[method:"getLensMaskConfig", params:[lens_mask:[name:["lens_mask_info"]]]] // library marker davegut.tpLinkCamCommon, line 196
	] // library marker davegut.tpLinkCamCommon, line 197
	sendDevCmd(requests, "setPrivacy", "parseUpdates") // library marker davegut.tpLinkCamCommon, line 198
} // library marker davegut.tpLinkCamCommon, line 199

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkCamCommon, line 201
def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkCamCommon, line 202
	Map cmdBody = [ // library marker davegut.tpLinkCamCommon, line 203
		method: "multipleRequest", // library marker davegut.tpLinkCamCommon, line 204
		params: [requests: requests]] // library marker davegut.tpLinkCamCommon, line 205
	asyncSend(cmdBody, data, action) // library marker davegut.tpLinkCamCommon, line 206
} // library marker davegut.tpLinkCamCommon, line 207

def parseUpdates(resp, data = null) { // library marker davegut.tpLinkCamCommon, line 209
	def respData = parseCameraData(resp, data) // library marker davegut.tpLinkCamCommon, line 210
	Map logData = [:] // library marker davegut.tpLinkCamCommon, line 211
	if (respData.parseStatus == "OK") { // library marker davegut.tpLinkCamCommon, line 212
		if (respData.cmdResp.result.responses != null) { // library marker davegut.tpLinkCamCommon, line 213
			respData.cmdResp.result.responses.each { // library marker davegut.tpLinkCamCommon, line 214
				try { // library marker davegut.tpLinkCamCommon, line 215
					if (it.error_code == 0) { // library marker davegut.tpLinkCamCommon, line 216
						if (it.method.contains("get")) {  // library marker davegut.tpLinkCamCommon, line 217
							logData << "parse_${it.method}"(it.result) // library marker davegut.tpLinkCamCommon, line 218
						} // library marker davegut.tpLinkCamCommon, line 219
					} else { // library marker davegut.tpLinkCamCommon, line 220
						Map errData = ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCamCommon, line 221
						logWarn([parseUpdates: errData]) // library marker davegut.tpLinkCamCommon, line 222
					} // library marker davegut.tpLinkCamCommon, line 223
				} catch (err) { // library marker davegut.tpLinkCamCommon, line 224
					Map errData = ["${it.method}": [result: it.result, ERROR: err]] // library marker davegut.tpLinkCamCommon, line 225
					logWarn([parseUpdates: errData]) // library marker davegut.tpLinkCamCommon, line 226
				} // library marker davegut.tpLinkCamCommon, line 227
			} // library marker davegut.tpLinkCamCommon, line 228
			logDebug([parseUpdates: logData]) // library marker davegut.tpLinkCamCommon, line 229
		} else { // library marker davegut.tpLinkCamCommon, line 230
			logData << [parseLog: respData, errorMsg: "Unknown Return from Device"] // library marker davegut.tpLinkCamCommon, line 231
			logWarn([parseUpdates: logData]) // library marker davegut.tpLinkCamCommon, line 232
		} // library marker davegut.tpLinkCamCommon, line 233
	} else { // library marker davegut.tpLinkCamCommon, line 234
		logData << [parseLog: respData, errorMsg: "No cmdResp in return"] // library marker davegut.tpLinkCamCommon, line 235
		logWarn([parseUpdates: logData]) // library marker davegut.tpLinkCamCommon, line 236
	} // library marker davegut.tpLinkCamCommon, line 237
} // library marker davegut.tpLinkCamCommon, line 238

def parse_getLastAlarmInfo(devResp) { // library marker davegut.tpLinkCamCommon, line 240
	Map alarmData = devResp.system.last_alarm_info // library marker davegut.tpLinkCamCommon, line 241
	if (alarmData.last_alarm_time.toInteger() > state.lastAlarmTime) { // library marker davegut.tpLinkCamCommon, line 242
		state.lastAlarmTime = alarmData.last_alarm_time.toInteger() // library marker davegut.tpLinkCamCommon, line 243
		sendEvent(name: "motion", value: "active") // library marker davegut.tpLinkCamCommon, line 244
		runIn(30, setInactive) // library marker davegut.tpLinkCamCommon, line 245
	} // library marker davegut.tpLinkCamCommon, line 246
	return [alarmType: alarmData.last_alarm_type] // library marker davegut.tpLinkCamCommon, line 247
} // library marker davegut.tpLinkCamCommon, line 248

def parse_getLensMaskConfig(devResp) { // library marker davegut.tpLinkCamCommon, line 250
	String privacy = "private" // library marker davegut.tpLinkCamCommon, line 251
	if (devResp.lens_mask.lens_mask_info.enabled == "off") { // library marker davegut.tpLinkCamCommon, line 252
		privacy = "notPrivate" // library marker davegut.tpLinkCamCommon, line 253
	} // library marker davegut.tpLinkCamCommon, line 254
	sendEvent(name: "privacy", value: privacy) // library marker davegut.tpLinkCamCommon, line 255
	return [privacy: privacy] // library marker davegut.tpLinkCamCommon, line 256
} // library marker davegut.tpLinkCamCommon, line 257

def parse_getLedStatus(devResp) { // library marker davegut.tpLinkCamCommon, line 259
	device.updateSetting("ledRule", [type: "enum", value: devResp.led.config.enabled]) // library marker davegut.tpLinkCamCommon, line 260
	state.ledStatus = devResp.led.config.enabled // library marker davegut.tpLinkCamCommon, line 261
	return [ledRule: devResp.led.config.enabled] // library marker davegut.tpLinkCamCommon, line 262
} // library marker davegut.tpLinkCamCommon, line 263

def parse_getDeviceAlias(devResp) { // library marker davegut.tpLinkCamCommon, line 265
	String alias = devRespsystem.sys.dev_alias // library marker davegut.tpLinkCamCommon, line 266
	device.setLabel(alias) // library marker davegut.tpLinkCamCommon, line 267
	device.updateSetting("syncName", [type: "enum", value: "notSet"]) // library marker davegut.tpLinkCamCommon, line 268
	return [label: alias] // library marker davegut.tpLinkCamCommon, line 269
} // library marker davegut.tpLinkCamCommon, line 270

def parse_getDetectionConfig(devResp) { // library marker davegut.tpLinkCamCommon, line 272
	def detData = devResp.motion_detection.motion_det // library marker davegut.tpLinkCamCommon, line 273
	def motDet = detData.enabled // library marker davegut.tpLinkCamCommon, line 274
	if (motDet == "on") { motDet = detData.sensitivity } // library marker davegut.tpLinkCamCommon, line 275
	device.updateSetting("motionDetect", [type: "enum", value: motDet]) // library marker davegut.tpLinkCamCommon, line 276
	return [motionDetect: motDet, peopleEnable: detData.people_enabled, // library marker davegut.tpLinkCamCommon, line 277
			vehEnable: detData.vehicle_enabled, nonVehEnable: detData.non_vehicle_enabled] // library marker davegut.tpLinkCamCommon, line 278
} // library marker davegut.tpLinkCamCommon, line 279

def parse_getTargetTrackConfig(devResp) { // library marker davegut.tpLinkCamCommon, line 281
	device.updateSetting("targetTrack", [type: "enum", value: devResp.target_track.target_track_info.enabled]) // library marker davegut.tpLinkCamCommon, line 282
	return [targetTrack: devResp.target_track.target_track_info.enabled] // library marker davegut.tpLinkCamCommon, line 283
} // library marker davegut.tpLinkCamCommon, line 284

def parse_getAlertConfig(devResp) { // library marker davegut.tpLinkCamCommon, line 286
	Map alarmInfo = devResp.msg_alarm.chn1_msg_alarm_info // library marker davegut.tpLinkCamCommon, line 287
	def alertConfig  = "off" // library marker davegut.tpLinkCamCommon, line 288
	if (alarmInfo.enabled == "on") { // library marker davegut.tpLinkCamCommon, line 289
		List alarmMode = alarmInfo.alarm_mode // library marker davegut.tpLinkCamCommon, line 290
		if (alarmMode.size() > 1|| alarmMode == []) { alertConfig = "both" }  // library marker davegut.tpLinkCamCommon, line 291
		else { alertConfig = alarmMode[0] } // library marker davegut.tpLinkCamCommon, line 292
	} // library marker davegut.tpLinkCamCommon, line 293
	device.updateSetting("alertConf", [type: "enum", value: alertConfig]) // library marker davegut.tpLinkCamCommon, line 294
	return [alertConf: alertConfig] // library marker davegut.tpLinkCamCommon, line 295
} // library marker davegut.tpLinkCamCommon, line 296

def nullParse(resp, data) { logDebug "nullParse" } // library marker davegut.tpLinkCamCommon, line 298

//	===== Check/Update device data ===== // library marker davegut.tpLinkCamCommon, line 300
def updateDeviceData(fromConfig = false) { // library marker davegut.tpLinkCamCommon, line 301
	def devData = parent.getDeviceData(device.getDeviceNetworkId()) // library marker davegut.tpLinkCamCommon, line 302
	updateChild(devData, fromConfig) // library marker davegut.tpLinkCamCommon, line 303
	return [updateDeviceData: "updating with app data"] // library marker davegut.tpLinkCamCommon, line 304
} // library marker davegut.tpLinkCamCommon, line 305

def updateChild(devData, fromConfig = false) { // library marker davegut.tpLinkCamCommon, line 307
	def currVersion = getDataValue("version") // library marker davegut.tpLinkCamCommon, line 308
	Map logData = [devData: devData, fromConfig: fromConfig] // library marker davegut.tpLinkCamCommon, line 309
	if (devData != null) { // library marker davegut.tpLinkCamCommon, line 310
		devData.each { // library marker davegut.tpLinkCamCommon, line 311
			if (it.key != "deviceType" && it.key != "model" && it.key != "alias") { // library marker davegut.tpLinkCamCommon, line 312
				updateDataValue(it.key, it.value.toString()) // library marker davegut.tpLinkCamCommon, line 313
			} // library marker davegut.tpLinkCamCommon, line 314
		} // library marker davegut.tpLinkCamCommon, line 315
		if (currVersion != version()) { // library marker davegut.tpLinkCamCommon, line 316
			updateDataValue("version", version()) // library marker davegut.tpLinkCamCommon, line 317
			logData << [newVersion: version()] // library marker davegut.tpLinkCamCommon, line 318
			runIn(20, updated) // library marker davegut.tpLinkCamCommon, line 319
		} // library marker davegut.tpLinkCamCommon, line 320
	} else { // library marker davegut.tpLinkCamCommon, line 321
		logData << [Note: "DEVICE DATA IS NULL"] // library marker davegut.tpLinkCamCommon, line 322
	} // library marker davegut.tpLinkCamCommon, line 323
	if (!fromConfig) { deviceHandshake() } // library marker davegut.tpLinkCamCommon, line 324
	logInfo([updateChild: logData]) // library marker davegut.tpLinkCamCommon, line 325
} // library marker davegut.tpLinkCamCommon, line 326

// ~~~~~ end include (377) davegut.tpLinkCamCommon ~~~~~

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
//	try { // library marker davegut.tpLinkCapConfiguration, line 16
//		if (getDataValue("power")) { // library marker davegut.tpLinkCapConfiguration, line 17
//			sendFindCmd(devIp, "20004", cmdData, "configure2", 5) // library marker davegut.tpLinkCapConfiguration, line 18
//		} else { // library marker davegut.tpLinkCapConfiguration, line 19
//			sendFindCmd(devIp, "20002", cmdData, "configure2", 5) // library marker davegut.tpLinkCapConfiguration, line 20
//		} // library marker davegut.tpLinkCapConfiguration, line 21
//		logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 22
//	} catch (err) { // library marker davegut.tpLinkCapConfiguration, line 23
		def parentChecked = parent.tpLinkCheckForDevices(5) // library marker davegut.tpLinkCapConfiguration, line 24
		logData << [status: "FAILED", error: err, parentChecked: parentChecked] // library marker davegut.tpLinkCapConfiguration, line 25
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 26
		configure3() // library marker davegut.tpLinkCapConfiguration, line 27
//	} // library marker davegut.tpLinkCapConfiguration, line 28
} // library marker davegut.tpLinkCapConfiguration, line 29

def configure2(response) { // library marker davegut.tpLinkCapConfiguration, line 31
	Map logData = [method: "configure2"] // library marker davegut.tpLinkCapConfiguration, line 32
	def respData = parseLanMessage(response) // library marker davegut.tpLinkCapConfiguration, line 33
	String hubDni = device.getDeviceNetworkId() // library marker davegut.tpLinkCapConfiguration, line 34
	logData << [dni: respData.mac, hubDni: hubDni] // library marker davegut.tpLinkCapConfiguration, line 35
	def parentChecked = false // library marker davegut.tpLinkCapConfiguration, line 36
	if (respData.mac != hubDni) { // library marker davegut.tpLinkCapConfiguration, line 37
		logData << [status: "device/ip not found", action: "parentCheck", // library marker davegut.tpLinkCapConfiguration, line 38
				    parentChecked: parent.tpLinkCheckForDevices(5)] // library marker davegut.tpLinkCapConfiguration, line 39
	} else { // library marker davegut.tpLinkCapConfiguration, line 40
		logData << [status: "device/ip found"] // library marker davegut.tpLinkCapConfiguration, line 41
	} // library marker davegut.tpLinkCapConfiguration, line 42
	configure3() // library marker davegut.tpLinkCapConfiguration, line 43
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 44
} // library marker davegut.tpLinkCapConfiguration, line 45
def configure3() { // library marker davegut.tpLinkCapConfiguration, line 46
	Map logData = [method: "configure3"] // library marker davegut.tpLinkCapConfiguration, line 47
	logData <<[updateDeviceData: updateDeviceData(true)] // library marker davegut.tpLinkCapConfiguration, line 48
	logData << [deviceHandshake: deviceHandshake()] // library marker davegut.tpLinkCapConfiguration, line 49
	if (getDataValue("protocol") != "camera") { // library marker davegut.tpLinkCapConfiguration, line 50
		runEvery3Hours("deviceHandshake") // library marker davegut.tpLinkCapConfiguration, line 51
		logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCapConfiguration, line 52
	} // library marker davegut.tpLinkCapConfiguration, line 53
	runIn(5, refresh) // library marker davegut.tpLinkCapConfiguration, line 54
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 55
} // library marker davegut.tpLinkCapConfiguration, line 56

def deviceHandshake() { // library marker davegut.tpLinkCapConfiguration, line 58
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkCapConfiguration, line 59
	Map logData = [method: "deviceHandshake", protocol: protocol] // library marker davegut.tpLinkCapConfiguration, line 60
	if (protocol == "KLAP") { // library marker davegut.tpLinkCapConfiguration, line 61
		klapHandshake(getDataValue("baseUrl"), parent.localHash) // library marker davegut.tpLinkCapConfiguration, line 62
	} else if (protocol == "camera") { // library marker davegut.tpLinkCapConfiguration, line 63
		Map hsInput = [url: getDataValue("baseUrl"), user: parent.userName, // library marker davegut.tpLinkCapConfiguration, line 64
					   pwd: parent.encPasswordCam] // library marker davegut.tpLinkCapConfiguration, line 65
		cameraHandshake(hsInput) // library marker davegut.tpLinkCapConfiguration, line 66
	} else if (protocol == "AES") { // library marker davegut.tpLinkCapConfiguration, line 67
		aesHandshake() // library marker davegut.tpLinkCapConfiguration, line 68
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkCapConfiguration, line 69
		vacAesHandshake(getDataValue("baseUrl"), parent.userName, parent.encPasswordVac) // library marker davegut.tpLinkCapConfiguration, line 70
	} else { // library marker davegut.tpLinkCapConfiguration, line 71
		logData << [ERROR: "Protocol not supported"] // library marker davegut.tpLinkCapConfiguration, line 72
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 73
	} // library marker davegut.tpLinkCapConfiguration, line 74
	return logData // library marker davegut.tpLinkCapConfiguration, line 75
} // library marker davegut.tpLinkCapConfiguration, line 76

// ~~~~~ end include (392) davegut.tpLinkCapConfiguration ~~~~~

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

//	===== Communications Methods ===== // library marker davegut.tpLinkComms, line 14
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
	if (reqParams != [:]) { // library marker davegut.tpLinkComms, line 28
		if (state.errorCount == 0) { state.lastCommand = cmdData } // library marker davegut.tpLinkComms, line 29
		asynchttpPost(action, reqParams, [data: reqData]) // library marker davegut.tpLinkComms, line 30
		logDebug([method: "asyncSend", reqData: reqData]) // library marker davegut.tpLinkComms, line 31
	} else { // library marker davegut.tpLinkComms, line 32
		unknownProt(reqData) // library marker davegut.tpLinkComms, line 33
	} // library marker davegut.tpLinkComms, line 34
} // library marker davegut.tpLinkComms, line 35

def unknownProt(reqData) { // library marker davegut.tpLinkComms, line 37
	Map warnData = ["<b>UnknownProtocol</b>": [data: reqData, // library marker davegut.tpLinkComms, line 38
				    msg: "Device will not install or if installed will not work"]] // library marker davegut.tpLinkComms, line 39
	logWarn(warnData) // library marker davegut.tpLinkComms, line 40
} // library marker davegut.tpLinkComms, line 41

def parseData(resp, protocol = getDataValue("protocol"), data = null) { // library marker davegut.tpLinkComms, line 43
	Map logData = [method: "parseData", status: resp.status, protocol: protocol, // library marker davegut.tpLinkComms, line 44
				   sourceMethod: data.data] // library marker davegut.tpLinkComms, line 45
	def message = "OK" // library marker davegut.tpLinkComms, line 46
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 47
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 48
			logData << parseKlapData(resp, data) // library marker davegut.tpLinkComms, line 49
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 50
			logData << parseAesData(resp, data) // library marker davegut.tpLinkComms, line 51
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 52
			logData << parseVacAesData(resp, data) // library marker davegut.tpLinkComms, line 53
		} else if (protocol == "camera") { // library marker davegut.tpLinkComms, line 54
			logData << parseCameraData(resp, data) // library marker davegut.tpLinkComms, line 55
		} // library marker davegut.tpLinkComms, line 56
	} else { // library marker davegut.tpLinkComms, line 57
		message = resp.errorMessage // library marker davegut.tpLinkComms, line 58
		String userMessage = "unspecified" // library marker davegut.tpLinkComms, line 59
		if (resp.status == 403) { // library marker davegut.tpLinkComms, line 60
			userMessage = "<b>Try again. If error persists, check your credentials</b>" // library marker davegut.tpLinkComms, line 61
		} else if (resp.status == 408) { // library marker davegut.tpLinkComms, line 62
			userMessage = "<b>Your router connection to ${getDataValue("baseUrl")} failed.  Run Configure.</b>" // library marker davegut.tpLinkComms, line 63
		} else { // library marker davegut.tpLinkComms, line 64
			userMessage = "<b>Unhandled error Lan return</b>" // library marker davegut.tpLinkComms, line 65
		} // library marker davegut.tpLinkComms, line 66
		logData << [respMessage: message, userMessage: userMessage] // library marker davegut.tpLinkComms, line 67
		logDebug(logData) // library marker davegut.tpLinkComms, line 68
	} // library marker davegut.tpLinkComms, line 69
	handleCommsError(resp.status, message) // library marker davegut.tpLinkComms, line 70
	return logData // library marker davegut.tpLinkComms, line 71
} // library marker davegut.tpLinkComms, line 72

private sendFindCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) { // library marker davegut.tpLinkComms, line 74
	def myHubAction = new hubitat.device.HubAction( // library marker davegut.tpLinkComms, line 75
		cmdData, // library marker davegut.tpLinkComms, line 76
		hubitat.device.Protocol.LAN, // library marker davegut.tpLinkComms, line 77
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, // library marker davegut.tpLinkComms, line 78
		 destinationAddress: "${ip}:${port}", // library marker davegut.tpLinkComms, line 79
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING, // library marker davegut.tpLinkComms, line 80
		 ignoreResponse: ignore, // library marker davegut.tpLinkComms, line 81
		 parseWarning: true, // library marker davegut.tpLinkComms, line 82
		 timeout: commsTo, // library marker davegut.tpLinkComms, line 83
		 callback: action]) // library marker davegut.tpLinkComms, line 84
	try { // library marker davegut.tpLinkComms, line 85
		sendHubCommand(myHubAction) // library marker davegut.tpLinkComms, line 86
	} catch (error) { // library marker davegut.tpLinkComms, line 87
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}") // library marker davegut.tpLinkComms, line 88
	} // library marker davegut.tpLinkComms, line 89
	return // library marker davegut.tpLinkComms, line 90
} // library marker davegut.tpLinkComms, line 91

//	Unknown Protocol method // library marker davegut.tpLinkComms, line 93
//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 94
def handleCommsError(status, msg = "") { // library marker davegut.tpLinkComms, line 95
	//	Retransmit all comms error except Switch and Level related (Hub retries for these). // library marker davegut.tpLinkComms, line 96
	//	This is determined by state.digital // library marker davegut.tpLinkComms, line 97
	if (status == 200) { // library marker davegut.tpLinkComms, line 98
		setCommsError(status, "OK") // library marker davegut.tpLinkComms, line 99
	} else { // library marker davegut.tpLinkComms, line 100
		Map logData = [method: "handleCommsError", status: code, msg: msg] // library marker davegut.tpLinkComms, line 101
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 102
		logData << [count: count, status: status, msg: msg] // library marker davegut.tpLinkComms, line 103
		switch(count) { // library marker davegut.tpLinkComms, line 104
			case 1: // library marker davegut.tpLinkComms, line 105
			case 2: // library marker davegut.tpLinkComms, line 106
				//	errors 1 and 2, retry immediately // library marker davegut.tpLinkComms, line 107
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 108
				break // library marker davegut.tpLinkComms, line 109
			case 3: // library marker davegut.tpLinkComms, line 110
				//	error 3, login or scan find device on the lan // library marker davegut.tpLinkComms, line 111
				//	then retry // library marker davegut.tpLinkComms, line 112
				if (status == 403) { // library marker davegut.tpLinkComms, line 113
					logData << [action: "attemptLogin"] // library marker davegut.tpLinkComms, line 114
//	await device handshake result???? // library marker davegut.tpLinkComms, line 115
					deviceHandshake() // library marker davegut.tpLinkComms, line 116
					runIn(4, delayedPassThrough) // library marker davegut.tpLinkComms, line 117
				} else { // library marker davegut.tpLinkComms, line 118
					logData << [action: "Find on LAN then login"] // library marker davegut.tpLinkComms, line 119
					configure() // library marker davegut.tpLinkComms, line 120
//	await configure result????? // library marker davegut.tpLinkComms, line 121
					runIn(10, delayedPassThrough) // library marker davegut.tpLinkComms, line 122
				} // library marker davegut.tpLinkComms, line 123
				break // library marker davegut.tpLinkComms, line 124
			case 4: // library marker davegut.tpLinkComms, line 125
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 126
				break // library marker davegut.tpLinkComms, line 127
			default: // library marker davegut.tpLinkComms, line 128
				//	Set comms error first time errros are 5 or more. // library marker davegut.tpLinkComms, line 129
				logData << [action: "SetCommsErrorTrue"] // library marker davegut.tpLinkComms, line 130
				setCommsError(status, msg, 5) // library marker davegut.tpLinkComms, line 131
		} // library marker davegut.tpLinkComms, line 132
		state.errorCount = count // library marker davegut.tpLinkComms, line 133
		logInfo(logData) // library marker davegut.tpLinkComms, line 134
	} // library marker davegut.tpLinkComms, line 135
} // library marker davegut.tpLinkComms, line 136

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 138
	def cmdData = new JSONObject(state.lastCommand) // library marker davegut.tpLinkComms, line 139
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 140
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 141
} // library marker davegut.tpLinkComms, line 142

def setCommsError(status, msg = "OK", count = state.commsError) { // library marker davegut.tpLinkComms, line 144
	Map logData = [method: "setCommsError", status: status, errorMsg: msg, count: count] // library marker davegut.tpLinkComms, line 145
	if (device && status == 200) { // library marker davegut.tpLinkComms, line 146
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 147
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 148
			sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkComms, line 149
			setPollInterval() // library marker davegut.tpLinkComms, line 150
			unschedule("errorConfigure") // library marker davegut.tpLinkComms, line 151
			logInfo(logData) // library marker davegut.tpLinkComms, line 152
		} // library marker davegut.tpLinkComms, line 153
	} else if (device) { // library marker davegut.tpLinkComms, line 154
		if (device.currentValue("commsError") == "false" && count > 4) { // library marker davegut.tpLinkComms, line 155
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 156
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 157
			runEvery10Minutes(errorConfigure) // library marker davegut.tpLinkComms, line 158
			logData << [pollInterval: "30 Min", errorConfigure: "ever 10 min"] // library marker davegut.tpLinkComms, line 159
			logWarn(logData) // library marker davegut.tpLinkComms, line 160
			if (status == 403) { // library marker davegut.tpLinkComms, line 161
				logWarn(logInErrorAction()) // library marker davegut.tpLinkComms, line 162
			} else { // library marker davegut.tpLinkComms, line 163
				logWarn(lanErrorAction()) // library marker davegut.tpLinkComms, line 164
			} // library marker davegut.tpLinkComms, line 165
		} else { // library marker davegut.tpLinkComms, line 166
			logData << [error: "Unspecified Error"] // library marker davegut.tpLinkComms, line 167
			logWarn(logData) // library marker davegut.tpLinkComms, line 168
		} // library marker davegut.tpLinkComms, line 169
	} // library marker davegut.tpLinkComms, line 170
} // library marker davegut.tpLinkComms, line 171

def errorConfigure() { // library marker davegut.tpLinkComms, line 173
	logDebug([method: "errorConfigure"]) // library marker davegut.tpLinkComms, line 174
	if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 175
		configure() // library marker davegut.tpLinkComms, line 176
	} else { // library marker davegut.tpLinkComms, line 177
		unschedule("errorConfigure") // library marker davegut.tpLinkComms, line 178
	} // library marker davegut.tpLinkComms, line 179
} // library marker davegut.tpLinkComms, line 180

def lanErrorAction() { // library marker davegut.tpLinkComms, line 182
	def action = "Likely cause of this error is YOUR LAN device configuration: " // library marker davegut.tpLinkComms, line 183
	action += "a. VERIFY your device is on the DHCP list in your router, " // library marker davegut.tpLinkComms, line 184
	action += "b. VERIFY your device is in the active device list in your router, and " // library marker davegut.tpLinkComms, line 185
	action += "c. TRY controlling your device from the TAPO phone app." // library marker davegut.tpLinkComms, line 186
	return action // library marker davegut.tpLinkComms, line 187
} // library marker davegut.tpLinkComms, line 188

def logInErrorAction() { // library marker davegut.tpLinkComms, line 190
	def action = "Likely cause is your login credentials are incorrect or the login has expired. " // library marker davegut.tpLinkComms, line 191
	action += "a. RUN command Configure. b. If error persists, check your credentials in the App" // library marker davegut.tpLinkComms, line 192
	return action // library marker davegut.tpLinkComms, line 193
} // library marker davegut.tpLinkComms, line 194

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

// ~~~~~ start include (389) davegut.tpLinkCamTransport ~~~~~
library ( // library marker davegut.tpLinkCamTransport, line 1
	name: "tpLinkCamTransport", // library marker davegut.tpLinkCamTransport, line 2
	namespace: "davegut", // library marker davegut.tpLinkCamTransport, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkCamTransport, line 4
	description: "TP-Link Camera Protocol Implementation", // library marker davegut.tpLinkCamTransport, line 5
	category: "utilities", // library marker davegut.tpLinkCamTransport, line 6
	documentationLink: "" // library marker davegut.tpLinkCamTransport, line 7
) // library marker davegut.tpLinkCamTransport, line 8
import java.util.Random // library marker davegut.tpLinkCamTransport, line 9
import java.security.MessageDigest // library marker davegut.tpLinkCamTransport, line 10
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tpLinkCamTransport, line 11
import javax.crypto.Cipher // library marker davegut.tpLinkCamTransport, line 12
import java.security.KeyFactory // library marker davegut.tpLinkCamTransport, line 13
import javax.crypto.spec.SecretKeySpec // library marker davegut.tpLinkCamTransport, line 14
import javax.crypto.spec.IvParameterSpec // library marker davegut.tpLinkCamTransport, line 15

def cameraHandshake(hsInput, devData = [:]) { // library marker davegut.tpLinkCamTransport, line 17
//log.debug devData // library marker davegut.tpLinkCamTransport, line 18
	Map logData = [url: hsInput.url] // library marker davegut.tpLinkCamTransport, line 19
	if (devData != [:]) { // library marker davegut.tpLinkCamTransport, line 20
		logData << [model: devData.model, ip: devData.ip] // library marker davegut.tpLinkCamTransport, line 21
	}			 // library marker davegut.tpLinkCamTransport, line 22
	def warn = true // library marker davegut.tpLinkCamTransport, line 23
//	Step 1.	Determine secureUser (userName or "admin"). // library marker davegut.tpLinkCamTransport, line 24
	def isSecure = state.isSecure // library marker davegut.tpLinkCamTransport, line 25
	if (isSecure != true) { // library marker davegut.tpLinkCamTransport, line 26
		Map secResp = checkSecure(hsInput) // library marker davegut.tpLinkCamTransport, line 27
		isSecure = secResp.secure // library marker davegut.tpLinkCamTransport, line 28
//		isSecure = checkSecure(hsInput) // library marker davegut.tpLinkCamTransport, line 29
		state.isSecure = isSecure // library marker davegut.tpLinkCamTransport, line 30
		logData << [checkSecure: secResp] // library marker davegut.tpLinkCamTransport, line 31
	} // library marker davegut.tpLinkCamTransport, line 32
	if (isSecure == true) { // library marker davegut.tpLinkCamTransport, line 33
//	Step 2.	Confirm Device. // library marker davegut.tpLinkCamTransport, line 34
		Map confData = confirmDevice(hsInput) // library marker davegut.tpLinkCamTransport, line 35
		logData << [confirmDevice: confData] // library marker davegut.tpLinkCamTransport, line 36
		if (confData.confirmed == true) { // library marker davegut.tpLinkCamTransport, line 37
//	Step 3.	Get token data. // library marker davegut.tpLinkCamTransport, line 38
			Map tokenData = getToken(hsInput, confData) // library marker davegut.tpLinkCamTransport, line 39
			logData << [tokenData: tokenData] // library marker davegut.tpLinkCamTransport, line 40
			if (tokenData.status == "OK") { // library marker davegut.tpLinkCamTransport, line 41
//	Step 4. next action. // library marker davegut.tpLinkCamTransport, line 42
				if (app) { // library marker davegut.tpLinkCamTransport, line 43
					camCmdIn = [lsk: tokenData.lsk, ivb: tokenData.ivb,  // library marker davegut.tpLinkCamTransport, line 44
								seqNo: tokenData.seqNo, apiUrl: tokenData.apiUrl, // library marker davegut.tpLinkCamTransport, line 45
								encPwd: hsInput.pwd, cnonce: confData.cnonce] // library marker davegut.tpLinkCamTransport, line 46
					sendCameraDataCmd(devData, camCmdIn) // library marker davegut.tpLinkCamTransport, line 47
				} else if (device) { // library marker davegut.tpLinkCamTransport, line 48
					device.updateSetting("nonce", [type:"password", value: confData.nonce]) // library marker davegut.tpLinkCamTransport, line 49
					device.updateSetting("cnonce", [type:"password", value: confData.cnonce]) // library marker davegut.tpLinkCamTransport, line 50
					device.updateSetting("lsk",[type:"password", value: tokenData.lsk]) // library marker davegut.tpLinkCamTransport, line 51
					device.updateSetting("ivb",[type:"password", value: tokenData.ivb]) // library marker davegut.tpLinkCamTransport, line 52
					device.updateSetting("encPwd",[type:"password", value: hsInput.encPwd]) // library marker davegut.tpLinkCamTransport, line 53
					device.updateSetting("apiUrl",[type:"password", value: tokenData.apiUrl]) // library marker davegut.tpLinkCamTransport, line 54
					state.seqNo = tokenData.seqNo // library marker davegut.tpLinkCamTransport, line 55
				} // library marker davegut.tpLinkCamTransport, line 56
				warn = false // library marker davegut.tpLinkCamTransport, line 57
			} // library marker davegut.tpLinkCamTransport, line 58
		} // library marker davegut.tpLinkCamTransport, line 59
	} // library marker davegut.tpLinkCamTransport, line 60
	if (warn == true) { // library marker davegut.tpLinkCamTransport, line 61
		logWarn([cameraHandshake: logData]) // library marker davegut.tpLinkCamTransport, line 62
	} // library marker davegut.tpLinkCamTransport, line 63
	else { // library marker davegut.tpLinkCamTransport, line 64
		logDebug([cameraHandshake: logData]) // library marker davegut.tpLinkCamTransport, line 65
	} // library marker davegut.tpLinkCamTransport, line 66
	return logData // library marker davegut.tpLinkCamTransport, line 67
} // library marker davegut.tpLinkCamTransport, line 68

def checkSecure(hsInput) { // library marker davegut.tpLinkCamTransport, line 70
	Map secResp = [:] // library marker davegut.tpLinkCamTransport, line 71
	secure = false // library marker davegut.tpLinkCamTransport, line 72
	Map cmdBody = [method: "login", params: [encrypt_type: "3",  username: hsInput.user]] // library marker davegut.tpLinkCamTransport, line 73
	Map respData = postSync(cmdBody, hsInput.url) // library marker davegut.tpLinkCamTransport, line 74
	if (respData.error_code == -40413 && respData.result && respData.result.data // library marker davegut.tpLinkCamTransport, line 75
		&& respData.result.data.encrypt_type.contains("3")) { // library marker davegut.tpLinkCamTransport, line 76
		secure = true // library marker davegut.tpLinkCamTransport, line 77
	} else { // library marker davegut.tpLinkCamTransport, line 78
		secResp << [invalidUser: getNote("checkUser")] // library marker davegut.tpLinkCamTransport, line 79
	} // library marker davegut.tpLinkCamTransport, line 80
	secResp << [secure: secure] // library marker davegut.tpLinkCamTransport, line 81
	return secResp // library marker davegut.tpLinkCamTransport, line 82
} // library marker davegut.tpLinkCamTransport, line 83

def confirmDevice(hsInput) { // library marker davegut.tpLinkCamTransport, line 85
	String cnonce = getSeed(8).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 86
	Map cmdBody = [method: "login", // library marker davegut.tpLinkCamTransport, line 87
				   params: [cnonce: cnonce, encrypt_type: "3",  username: hsInput.user]] // library marker davegut.tpLinkCamTransport, line 88
	Map respData = postSync(cmdBody, hsInput.url) // library marker davegut.tpLinkCamTransport, line 89
	def confirmed = false // library marker davegut.tpLinkCamTransport, line 90
	Map confData = [:] // library marker davegut.tpLinkCamTransport, line 91
	if (respData.result) { // library marker davegut.tpLinkCamTransport, line 92
		Map results = respData.result.data // library marker davegut.tpLinkCamTransport, line 93
		if (respData.error_code == -40413 && results.code == -40401 &&  // library marker davegut.tpLinkCamTransport, line 94
			results.encrypt_type.toString().contains("3")) { // library marker davegut.tpLinkCamTransport, line 95
			String noncesPwdHash = cnonce + hsInput.pwd + results.nonce // library marker davegut.tpLinkCamTransport, line 96
			String testHash = mdEncode("SHA-256", noncesPwdHash.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 97
			String checkData = testHash + results.nonce + cnonce // library marker davegut.tpLinkCamTransport, line 98
			if (checkData == results.device_confirm) { // library marker davegut.tpLinkCamTransport, line 99
				confData << [nonce: results.nonce, cnonce: cnonce] // library marker davegut.tpLinkCamTransport, line 100
				confirmed = true // library marker davegut.tpLinkCamTransport, line 101
			} else { // library marker davegut.tpLinkCamTransport, line 102
				confData << [error: "checkData and deviceData mismatch"] // library marker davegut.tpLinkCamTransport, line 103
			} // library marker davegut.tpLinkCamTransport, line 104
		} else { // library marker davegut.tpLinkCamTransport, line 105
			confData << [error_code: respData.error_code, code: results.code, // library marker davegut.tpLinkCamTransport, line 106
						 encrypt_type: results.encrypt_type, error: "invalid data to continue"] // library marker davegut.tpLinkCamTransport, line 107
		} // library marker davegut.tpLinkCamTransport, line 108
	} else { // library marker davegut.tpLinkCamTransport, line 109
		confData << [respData: respData, error: "no respData.results in return."] // library marker davegut.tpLinkCamTransport, line 110
	} // library marker davegut.tpLinkCamTransport, line 111
	confData << [confirmed: confirmed] // library marker davegut.tpLinkCamTransport, line 112
	if (confirmed == false) { // library marker davegut.tpLinkCamTransport, line 113
		confData << [checkPwd: getNote("checkPwd"), thirdParty: getNote("thirdParty")] // library marker davegut.tpLinkCamTransport, line 114
	} // library marker davegut.tpLinkCamTransport, line 115
return confData // library marker davegut.tpLinkCamTransport, line 116
} // library marker davegut.tpLinkCamTransport, line 117

def getToken(hsInput, confData) { // library marker davegut.tpLinkCamTransport, line 119
	Map tokenData = [:] // library marker davegut.tpLinkCamTransport, line 120
	String digestPwdHex = hsInput.pwd + confData.cnonce + confData.nonce // library marker davegut.tpLinkCamTransport, line 121
	String digestPwd = mdEncode("SHA-256", digestPwdHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 122
	String fullDigestPwdHex = digestPwd + confData.cnonce + confData.nonce // library marker davegut.tpLinkCamTransport, line 123
	String fullDigestPwd = new String(fullDigestPwdHex.getBytes(), "UTF-8") // library marker davegut.tpLinkCamTransport, line 124
	Map cmdBody = [ // library marker davegut.tpLinkCamTransport, line 125
		method: "login", // library marker davegut.tpLinkCamTransport, line 126
		params: [cnonce: confData.cnonce,  // library marker davegut.tpLinkCamTransport, line 127
				 encrypt_type: "3", // library marker davegut.tpLinkCamTransport, line 128
				 digest_passwd: fullDigestPwd, // library marker davegut.tpLinkCamTransport, line 129
				 username: hsInput.user // library marker davegut.tpLinkCamTransport, line 130
			]] // library marker davegut.tpLinkCamTransport, line 131
	Map respData = postSync(cmdBody, hsInput.url) // library marker davegut.tpLinkCamTransport, line 132
	Map logData = [errorCode: respData.error_code] // library marker davegut.tpLinkCamTransport, line 133
	if (respData.error_code == 0) { // library marker davegut.tpLinkCamTransport, line 134
		Map result = respData.result // library marker davegut.tpLinkCamTransport, line 135
		if (result != null) { // library marker davegut.tpLinkCamTransport, line 136
			if (result.start_seq != null) { // library marker davegut.tpLinkCamTransport, line 137
				if (result.user_group == "root") { // library marker davegut.tpLinkCamTransport, line 138
					byte[] lsk = genEncryptToken("lsk", hsInput.pwd, confData.nonce, confData.cnonce) // library marker davegut.tpLinkCamTransport, line 139
					byte[] ivb = genEncryptToken("ivb", hsInput.pwd, confData.nonce, confData.cnonce) // library marker davegut.tpLinkCamTransport, line 140
					String apiUrl = "${hsInput.url}/stok=${result.stok}/ds" // library marker davegut.tpLinkCamTransport, line 141
					tokenData << [seqNo: result.start_seq, lsk: lsk, ivb: ivb, // library marker davegut.tpLinkCamTransport, line 142
								  apiUrl: apiUrl, status: "OK"] // library marker davegut.tpLinkCamTransport, line 143
				} else { // library marker davegut.tpLinkCamTransport, line 144
					tokenData << [status: "invalidUserGroup"] // library marker davegut.tpLinkCamTransport, line 145
				} // library marker davegut.tpLinkCamTransport, line 146
			} else { // library marker davegut.tpLinkCamTransport, line 147
				tokenData << [status: "nullStartSeq"] // library marker davegut.tpLinkCamTransport, line 148
			} // library marker davegut.tpLinkCamTransport, line 149
		} else { // library marker davegut.tpLinkCamTransport, line 150
			tokenData << [status: "nullDataFrom Device", respData: respData] // library marker davegut.tpLinkCamTransport, line 151
		} // library marker davegut.tpLinkCamTransport, line 152
	} else { // library marker davegut.tpLinkCamTransport, line 153
		tokenData << [status: "credentialError"] // library marker davegut.tpLinkCamTransport, line 154
	} // library marker davegut.tpLinkCamTransport, line 155
	if (tokenData.status != "OK") { // library marker davegut.tpLinkCamTransport, line 156
		tokenData << [respData: respData, tokenErr: getNote("tokenErr")] // library marker davegut.tpLinkCamTransport, line 157
//		logData << [tokenData: tokenData, respData: respData] // library marker davegut.tpLinkCamTransport, line 158
//		logWarn([getToken: logData]) // library marker davegut.tpLinkCamTransport, line 159
	} // library marker davegut.tpLinkCamTransport, line 160
	return tokenData // library marker davegut.tpLinkCamTransport, line 161
} // library marker davegut.tpLinkCamTransport, line 162

def genEncryptToken(tokenType, pwd, nonce, cnonce) { // library marker davegut.tpLinkCamTransport, line 164
	String hashedKeyHex = cnonce + pwd + nonce // library marker davegut.tpLinkCamTransport, line 165
	String hashedKey = mdEncode("SHA-256", hashedKeyHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 166
	String tokenHex = tokenType + cnonce + nonce + hashedKey // library marker davegut.tpLinkCamTransport, line 167
	byte[] tokenFull = mdEncode("SHA-256", tokenHex.getBytes()) // library marker davegut.tpLinkCamTransport, line 168
	return tokenFull[0..15] // library marker davegut.tpLinkCamTransport, line 169
} // library marker davegut.tpLinkCamTransport, line 170

def getNote(noteId) { // library marker davegut.tpLinkCamTransport, line 172
	String note = "Undefined note." // library marker davegut.tpLinkCamTransport, line 173
	if (noteId == "checkPwd") { // library marker davegut.tpLinkCamTransport, line 174
		note = "<b>Check password. Must not have spaces. Certain special characters also cause failure.</b>" // library marker davegut.tpLinkCamTransport, line 175
	} else if (noteId == "thirdParty") { // library marker davegut.tpLinkCamTransport, line 176
		note = "<b>Check Tapo app setting Third-Party Services for on.  If on toggle off then on.</b>" // library marker davegut.tpLinkCamTransport, line 177
	} else if (noteId == "checkUser") { // library marker davegut.tpLinkCamTransport, line 178
		note = "<b>Check username. No spaces. Alternate username admin may also work.</b>" // library marker davegut.tpLinkCamTransport, line 179
	} else if (noteId == "tokenErr") { // library marker davegut.tpLinkCamTransport, line 180
		note = "<b>Try again in 10 minutes. If error persists, contact developer.</b>" // library marker davegut.tpLinkCamTransport, line 181
	} // library marker davegut.tpLinkCamTransport, line 182
	return note // library marker davegut.tpLinkCamTransport, line 183
} // library marker davegut.tpLinkCamTransport, line 184

def shortHandshake() { // library marker davegut.tpLinkCamTransport, line 186
	String pwd = parent.encPasswordCam // library marker davegut.tpLinkCamTransport, line 187
	String url = getDataValue("baseUrl") // library marker davegut.tpLinkCamTransport, line 188
	Map logData = [:] // library marker davegut.tpLinkCamTransport, line 189
	String digestPwdHex = pwd + cnonce + nonce // library marker davegut.tpLinkCamTransport, line 190
	String digestPwd = mdEncode("SHA-256", digestPwdHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 191
	String fullDigestPwdHex = digestPwd + cnonce + nonce // library marker davegut.tpLinkCamTransport, line 192
	String fullDigestPwd = new String(fullDigestPwdHex.getBytes(), "UTF-8") // library marker davegut.tpLinkCamTransport, line 193
	Map cmdBody = [ // library marker davegut.tpLinkCamTransport, line 194
		method: "login", // library marker davegut.tpLinkCamTransport, line 195
		params: [cnonce: cnonce,  // library marker davegut.tpLinkCamTransport, line 196
				 encrypt_type: "3", // library marker davegut.tpLinkCamTransport, line 197
				 digest_passwd: fullDigestPwd, // library marker davegut.tpLinkCamTransport, line 198
				 username: parent.userName // library marker davegut.tpLinkCamTransport, line 199
			]] // library marker davegut.tpLinkCamTransport, line 200
	Map respData = postSync(cmdBody, url) // library marker davegut.tpLinkCamTransport, line 201

	logData << [errorCode: respData.error_code] // library marker davegut.tpLinkCamTransport, line 203
	String tokenStatus = "OK" // library marker davegut.tpLinkCamTransport, line 204
	if (respData.error_code == 0) { // library marker davegut.tpLinkCamTransport, line 205
		Map result = respData.result // library marker davegut.tpLinkCamTransport, line 206
		if (result != null) { // library marker davegut.tpLinkCamTransport, line 207
			if (result.start_seq != null) { // library marker davegut.tpLinkCamTransport, line 208
				if (result.user_group == "root") { // library marker davegut.tpLinkCamTransport, line 209
					byte[] lsk = genEncryptToken("lsk", pwd, nonce, cnonce) // library marker davegut.tpLinkCamTransport, line 210
					byte[] ivb = genEncryptToken("ivb", pwd, nonce, cnonce) // library marker davegut.tpLinkCamTransport, line 211
					String apiUrl = "${url}/stok=${result.stok}/ds" // library marker davegut.tpLinkCamTransport, line 212
logData << [seqNo: result.start_seq, lsk: lsk, ivb: ivb, apiUrl: apiUrl] // library marker davegut.tpLinkCamTransport, line 213
					device.updateSetting("lsk",[type:"password", value: lsk]) // library marker davegut.tpLinkCamTransport, line 214
					device.updateSetting("ivb",[type:"password", value: ivb]) // library marker davegut.tpLinkCamTransport, line 215
					device.updateSetting("apiUrl",[type:"password", value: apiUrl]) // library marker davegut.tpLinkCamTransport, line 216
					state.seqNo = result.start_seq // library marker davegut.tpLinkCamTransport, line 217
				} else { // library marker davegut.tpLinkCamTransport, line 218
					tokenStatus = "invalidUserGroup" // library marker davegut.tpLinkCamTransport, line 219
				} // library marker davegut.tpLinkCamTransport, line 220
			} else { // library marker davegut.tpLinkCamTransport, line 221
				tokenStatus = "nullStartSeq" // library marker davegut.tpLinkCamTransport, line 222
			} // library marker davegut.tpLinkCamTransport, line 223
		} else { // library marker davegut.tpLinkCamTransport, line 224
			tokenStatus = "nullDataFrom Device" // library marker davegut.tpLinkCamTransport, line 225
		} // library marker davegut.tpLinkCamTransport, line 226
	} else { // library marker davegut.tpLinkCamTransport, line 227
		tokenStatus ="credentialError" // library marker davegut.tpLinkCamTransport, line 228
	} // library marker davegut.tpLinkCamTransport, line 229
	if (tokenStatus != "OK") { // library marker davegut.tpLinkCamTransport, line 230
		Map hsInput = [url: url, user: parent.userName, pwd: pwd] // library marker davegut.tpLinkCamTransport, line 231
		logData << [respData: respData, cameraHandshake: cameraHandshake(hsInput)] // library marker davegut.tpLinkCamTransport, line 232
		logWarn([shortHandshake: logData]) // library marker davegut.tpLinkCamTransport, line 233
	} // library marker davegut.tpLinkCamTransport, line 234
	return logData // library marker davegut.tpLinkCamTransport, line 235
} // library marker davegut.tpLinkCamTransport, line 236

//	===== Sync Communications ===== // library marker davegut.tpLinkCamTransport, line 238
def getCamHeaders() { // library marker davegut.tpLinkCamTransport, line 239
	Map headers = [ // library marker davegut.tpLinkCamTransport, line 240
		"Accept": "application/json", // library marker davegut.tpLinkCamTransport, line 241
		"Accept-Encoding": "gzip, deflate", // library marker davegut.tpLinkCamTransport, line 242
		"User-Agent": "Tapo CameraClient Android", // library marker davegut.tpLinkCamTransport, line 243
		"Connection": "close", // library marker davegut.tpLinkCamTransport, line 244
		"requestByApp": "true", // library marker davegut.tpLinkCamTransport, line 245
		"Content-Type": "application/json; charset=UTF-8" // library marker davegut.tpLinkCamTransport, line 246
		] // library marker davegut.tpLinkCamTransport, line 247
	return headers // library marker davegut.tpLinkCamTransport, line 248
} // library marker davegut.tpLinkCamTransport, line 249

def postSync(cmdBody, baseUrl) { // library marker davegut.tpLinkCamTransport, line 251
	Map respData = [:] // library marker davegut.tpLinkCamTransport, line 252
	Map heads = getCamHeaders() // library marker davegut.tpLinkCamTransport, line 253
	Map httpParams = [uri: baseUrl, // library marker davegut.tpLinkCamTransport, line 254
					 body: JsonOutput.toJson(cmdBody), // library marker davegut.tpLinkCamTransport, line 255
					 contentType: "application/json", // library marker davegut.tpLinkCamTransport, line 256
					 requestContentType: "application/json", // library marker davegut.tpLinkCamTransport, line 257
					 timeout: 10, // library marker davegut.tpLinkCamTransport, line 258
					 ignoreSSLIssues: true, // library marker davegut.tpLinkCamTransport, line 259
					 headers: heads // library marker davegut.tpLinkCamTransport, line 260
					 ] // library marker davegut.tpLinkCamTransport, line 261
	try { // library marker davegut.tpLinkCamTransport, line 262
		httpPostJson(httpParams) { resp -> // library marker davegut.tpLinkCamTransport, line 263
			if (resp.status == 200) { // library marker davegut.tpLinkCamTransport, line 264
				respData << resp.data // library marker davegut.tpLinkCamTransport, line 265
			} else { // library marker davegut.tpLinkCamTransport, line 266
				respData << [status: resp.status, errorData: resp.properties, // library marker davegut.tpLinkCamTransport, line 267
							 action: "<b>Check IP Address</b>"] // library marker davegut.tpLinkCamTransport, line 268
				logWarn(respData) // library marker davegut.tpLinkCamTransport, line 269
			} // library marker davegut.tpLinkCamTransport, line 270
		} // library marker davegut.tpLinkCamTransport, line 271
	} catch (err) { // library marker davegut.tpLinkCamTransport, line 272
		respData << [status: "httpPostJson error", error: err] // library marker davegut.tpLinkCamTransport, line 273
		logWarn(respData) // library marker davegut.tpLinkCamTransport, line 274
	} // library marker davegut.tpLinkCamTransport, line 275
	return respData // library marker davegut.tpLinkCamTransport, line 276
} // library marker davegut.tpLinkCamTransport, line 277

def getCameraParams(cmdBody, reqData) { // library marker davegut.tpLinkCamTransport, line 279
	byte[] encKey = new JsonSlurper().parseText(lsk) // library marker davegut.tpLinkCamTransport, line 280
	byte[] encIv = new JsonSlurper().parseText(ivb) // library marker davegut.tpLinkCamTransport, line 281
	def cmdStr = JsonOutput.toJson(cmdBody) // library marker davegut.tpLinkCamTransport, line 282
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCamTransport, line 283
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCamTransport, line 284
	String cmdData = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.tpLinkCamTransport, line 285
	Integer seqNumber = state.seqNo // library marker davegut.tpLinkCamTransport, line 286
	String initTagHex = parent.encPasswordCam + cnonce // library marker davegut.tpLinkCamTransport, line 287
	String initTag = mdEncode("SHA-256", initTagHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 288
	String tagString = initTag + cmdData + seqNumber // library marker davegut.tpLinkCamTransport, line 289
	String tag =  mdEncode("SHA-256", tagString.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkCamTransport, line 290
	Map heads = getCamHeaders() // library marker davegut.tpLinkCamTransport, line 291
	heads << ["Tapo_tag": tag, Seq: seqNumber] // library marker davegut.tpLinkCamTransport, line 292
	Map reqParams = [uri: apiUrl, // library marker davegut.tpLinkCamTransport, line 293
					 body: cmdData, // library marker davegut.tpLinkCamTransport, line 294
					 contentType: "application/json", // library marker davegut.tpLinkCamTransport, line 295
					 requestContentType: "application/json", // library marker davegut.tpLinkCamTransport, line 296
					 timeout: 15, // library marker davegut.tpLinkCamTransport, line 297
					 ignoreSSLIssues: true, // library marker davegut.tpLinkCamTransport, line 298
					 headers: heads // library marker davegut.tpLinkCamTransport, line 299
					] // library marker davegut.tpLinkCamTransport, line 300
	return reqParams // library marker davegut.tpLinkCamTransport, line 301
} // library marker davegut.tpLinkCamTransport, line 302

def parseCameraData(resp, data) { // library marker davegut.tpLinkCamTransport, line 304
	Map parseData = [sourceMethod: data.data, jsonErrCode: resp.json.error_code] // library marker davegut.tpLinkCamTransport, line 305
	state.seqNo += 1 // library marker davegut.tpLinkCamTransport, line 306
	if (resp.json.error_code == 0) { // library marker davegut.tpLinkCamTransport, line 307
		try { // library marker davegut.tpLinkCamTransport, line 308
			byte[] encKey = new JsonSlurper().parseText(lsk) // library marker davegut.tpLinkCamTransport, line 309
			byte[] encIv = new JsonSlurper().parseText(ivb) // library marker davegut.tpLinkCamTransport, line 310
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCamTransport, line 311
															 	encKey, encIv)) // library marker davegut.tpLinkCamTransport, line 312
			parseData << [parseStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCamTransport, line 313
			state.protoError = false // library marker davegut.tpLinkCamTransport, line 314
		} catch (err) { // library marker davegut.tpLinkCamTransport, line 315
			parseData << [parseStatus: "Decrypt Error", error: err] // library marker davegut.tpLinkCamTransport, line 316
       } // library marker davegut.tpLinkCamTransport, line 317
	} else { // library marker davegut.tpLinkCamTransport, line 318
		parseData << [parseStatus: "Protocol Error"] // library marker davegut.tpLinkCamTransport, line 319
	} // library marker davegut.tpLinkCamTransport, line 320
	if (parseData.parseStatus != "OK") { // library marker davegut.tpLinkCamTransport, line 321
		if (state.protoError == false) { // library marker davegut.tpLinkCamTransport, line 322
			parseData << [nextMeth: "resolveProtocolError"] // library marker davegut.tpLinkCamTransport, line 323
			resolveProtocolError() // library marker davegut.tpLinkCamTransport, line 324
		} // library marker davegut.tpLinkCamTransport, line 325
	} // library marker davegut.tpLinkCamTransport, line 326
	return parseData // library marker davegut.tpLinkCamTransport, line 327
} // library marker davegut.tpLinkCamTransport, line 328

//	Run deviceHandshake then retry command // library marker davegut.tpLinkCamTransport, line 330
def resolveProtocolError() { // library marker davegut.tpLinkCamTransport, line 331
	Map logData = [method: "resolveProtocolError", lastCmd: state.lastCommand] // library marker davegut.tpLinkCamTransport, line 332
	state.protoError = true // library marker davegut.tpLinkCamTransport, line 333
	deviceHandshake() // library marker davegut.tpLinkCamTransport, line 334
	runIn(4, delayedPassThrough) // library marker davegut.tpLinkCamTransport, line 335
	logDebug(logData) // library marker davegut.tpLinkCamTransport, line 336
} // library marker davegut.tpLinkCamTransport, line 337


// ~~~~~ end include (389) davegut.tpLinkCamTransport ~~~~~

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
	def logData = [infoLog: infoLog, logEnable: logEnable] // library marker davegut.Logging, line 38
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
