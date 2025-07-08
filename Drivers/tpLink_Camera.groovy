/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Supports:  Tapo Plugs and Switches, New and Matter Kasa Plugs and Switches.
=================================================================================================*/
metadata {
	definition (name: "TpLink Camera", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_camera.groovy")
	{ }
	preferences {
		cameraPreferences()
		commonPreferences()
	}
}

def installed() {
	state.lastAlarmTime = 0
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	logInfo(logData)
}

def updated() {
	sendEvent(name: "motion", value: "inactive")
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
}









// ~~~~~ start include (276) davegut.tpLinkCapCamera ~~~~~
library ( // library marker davegut.tpLinkCapCamera, line 1
	name: "tpLinkCapCamera", // library marker davegut.tpLinkCapCamera, line 2
	namespace: "davegut", // library marker davegut.tpLinkCapCamera, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCapCamera, line 4
	description: "Tapo basic camera methodss", // library marker davegut.tpLinkCapCamera, line 5
	category: "utilities", // library marker davegut.tpLinkCapCamera, line 6
	documentationLink: "" // library marker davegut.tpLinkCapCamera, line 7
) // library marker davegut.tpLinkCapCamera, line 8

capability "Polling" // library marker davegut.tpLinkCapCamera, line 10
capability "Refresh" // library marker davegut.tpLinkCapCamera, line 11
capability "Configuration" // library marker davegut.tpLinkCapCamera, line 12
capability "Video Camera" // library marker davegut.tpLinkCapCamera, line 13
command "on", [[name: "Video enabled"]] // library marker davegut.tpLinkCapCamera, line 14
command "off", [[name: "Video disabled"]] // library marker davegut.tpLinkCapCamera, line 15
command "unmute" // library marker davegut.tpLinkCapCamera, line 16
command "mute" // library marker davegut.tpLinkCapCamera, line 17
attribute "flip", "string" // library marker davegut.tpLinkCapCamera, line 18
capability "Motion Sensor" // library marker davegut.tpLinkCapCamera, line 19

def cameraPreferences() { // library marker davegut.tpLinkCapCamera, line 21
	List pollOptions = ["5 sec", "10 sec", "30 sec", "off"] // library marker davegut.tpLinkCapCamera, line 22
	input ("pollInterval", "enum", title: "Poll Interval", // library marker davegut.tpLinkCapCamera, line 23
		   options: pollOptions, defaultValue: "30 sec") // library marker davegut.tpLinkCapCamera, line 24
	input ("motionDetect", "enum", title: "Motion Detection", options: ["on", "on"]) // library marker davegut.tpLinkCapCamera, line 25
	input ("personDetect", "enum", title: "Person Detection", options: ["on", "off"]) // library marker davegut.tpLinkCapCamera, line 26
	input ("babyCryDetect", "enum", title: "Baby Cry Detection", options: ["on", "off"]) // library marker davegut.tpLinkCapCamera, line 27
	if (getDataValue("targetTrack") == "true") { // library marker davegut.tpLinkCapCamera, line 28
		input ("targetTrack", "enum", title: "Target Tracking", options: ["on", "off"]) // library marker davegut.tpLinkCapCamera, line 29
	} // library marker davegut.tpLinkCapCamera, line 30
	if (getDataValue("tamperDetect") == "true") { // library marker davegut.tpLinkCapCamera, line 31
		input ("tamperDet", "enum", title: "Tamper Detection", options: ["on", "off"]) // library marker davegut.tpLinkCapCamera, line 32
	} // library marker davegut.tpLinkCapCamera, line 33
	input ("alarmSetting", "enum", title: "Device Detection Alarm", // library marker davegut.tpLinkCapCamera, line 34
		   options: ["off", "light", "sound", "sound & light"]) // library marker davegut.tpLinkCapCamera, line 35
	if (getDataValue("hasLed") == "true") { // library marker davegut.tpLinkCapCamera, line 36
		input ("ledRule", "enum", title: "LED Mode", options: ["on", "off"]) // library marker davegut.tpLinkCapCamera, line 37
	} // library marker davegut.tpLinkCapCamera, line 38
} // library marker davegut.tpLinkCapCamera, line 39

def updDevSettings() { // library marker davegut.tpLinkCapCamera, line 41
	List requests = [] // library marker davegut.tpLinkCapCamera, line 42
	if (syncName == "hubMaster") { // library marker davegut.tpLinkCapCamera, line 43
		requests << [method:"setDeviceAlias", params:[system:[sys:[dev_alias: device.getLabel()]]]] // library marker davegut.tpLinkCapCamera, line 44
		requests << [method:"getDeviceAlias", params:[system:[name: "sys"]]] // library marker davegut.tpLinkCapCamera, line 45
	} else if (syncName == "tapoAppMaster") { // library marker davegut.tpLinkCapCamera, line 46
		requests << [method:"getDeviceAlias", params:[system:[name: "sys"]]] // library marker davegut.tpLinkCapCamera, line 47
	} // library marker davegut.tpLinkCapCamera, line 48
	if (alarmSetting == "off") { // library marker davegut.tpLinkCapCamera, line 49
		requests << [method:"setAlertConfig", params: [ // library marker davegut.tpLinkCapCamera, line 50
			msg_alarm: [chn1_msg_alarm_info: [enabled: "off"]]]] // library marker davegut.tpLinkCapCamera, line 51
	} else { // library marker davegut.tpLinkCapCamera, line 52
		List alarmMode = ["sound", "light"] // library marker davegut.tpLinkCapCamera, line 53
		if (alarmSetting == "light") { // library marker davegut.tpLinkCapCamera, line 54
			alarmMode = ["light"] // library marker davegut.tpLinkCapCamera, line 55
		} else if (alarmSetting == "sound") { // library marker davegut.tpLinkCapCamera, line 56
			alarmMode = ["sound"] // library marker davegut.tpLinkCapCamera, line 57
		} // library marker davegut.tpLinkCapCamera, line 58
		requests << [method:"setAlertConfig", params: [ // library marker davegut.tpLinkCapCamera, line 59
			msg_alarm: [chn1_msg_alarm_info: [alarm_mode: alarmMode, enabled: "on"]]]] // library marker davegut.tpLinkCapCamera, line 60
	} // library marker davegut.tpLinkCapCamera, line 61
	requests << [method:"setDetectionConfig",params:[motion_detection:[motion_det:[ // library marker davegut.tpLinkCapCamera, line 62
		enabled: motionDetect, people_enabled: "on", non_vehicle_enabled: "on", vehicle_enabled: "on"]]]] // library marker davegut.tpLinkCapCamera, line 63
	requests << [method:"setPersonDetectionConfig", params:[ // library marker davegut.tpLinkCapCamera, line 64
		people_detection:[detection:[enabled: personDetect]]]] // library marker davegut.tpLinkCapCamera, line 65
	requests << [method:"setBCDConfig", params:[sound_detection:[bcd:[enabled: babyCryDetect]]]] // library marker davegut.tpLinkCapCamera, line 66
	requests << [method:"setTargetTrackConfig", params:[ // library marker davegut.tpLinkCapCamera, line 67
		target_track:[target_track_info:[enabled: targetTrack]]]] // library marker davegut.tpLinkCapCamera, line 68
	requests << [method:"setTamperDetectionConfig", params:[ // library marker davegut.tpLinkCapCamera, line 69
		tamper_detection:[tamper_det:[enabled: tamperDet]]]] // library marker davegut.tpLinkCapCamera, line 70
	requests << [method:"setLedStatus", params:[led:[config:[enabled:ledRule]]]] // library marker davegut.tpLinkCapCamera, line 71
	sendDevCmd(requests, "updateDevSettings", "parseUpdates") // library marker davegut.tpLinkCapCamera, line 72
	return "Device Settings Updated" // library marker davegut.tpLinkCapCamera, line 73
} // library marker davegut.tpLinkCapCamera, line 74

def setPollInterval(interval = pollInterval) { // library marker davegut.tpLinkCapCamera, line 76
	unschedule("poll") // library marker davegut.tpLinkCapCamera, line 77
	if (interval != "off") { // library marker davegut.tpLinkCapCamera, line 78
		interval = interval.replace(" sec", "").toInteger() // library marker davegut.tpLinkCapCamera, line 79
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.tpLinkCapCamera, line 80
		schedule("${start}/${interval} * * * * ?", "poll") // library marker davegut.tpLinkCapCamera, line 81
	} // library marker davegut.tpLinkCapCamera, line 82
	return interval // library marker davegut.tpLinkCapCamera, line 83
} // library marker davegut.tpLinkCapCamera, line 84

def poll() { // library marker davegut.tpLinkCapCamera, line 86
	requests = [[method:"getLastAlarmInfo", params:[system:[name:["last_alarm_info"]]]]] // library marker davegut.tpLinkCapCamera, line 87
	sendDevCmd(requests, "poll", "parsePoll") // library marker davegut.tpLinkCapCamera, line 88
} // library marker davegut.tpLinkCapCamera, line 89
def parsePoll(resp, data = null) { // library marker davegut.tpLinkCapCamera, line 90
	try { // library marker davegut.tpLinkCapCamera, line 91
		def respData = parseData(resp, "camera", data) // library marker davegut.tpLinkCapCamera, line 92
		Map alarmData = respData.cmdResp.result.responses[0].result.system.last_alarm_info // library marker davegut.tpLinkCapCamera, line 93
		Integer currTime = alarmData.last_alarm_time.toInteger() // library marker davegut.tpLinkCapCamera, line 94
		if (state.lastAlarmTime < currTime) { // library marker davegut.tpLinkCapCamera, line 95
			state.lastAlarmTime = currTime // library marker davegut.tpLinkCapCamera, line 96
			sendEvent(name: "motion", value: "active") // library marker davegut.tpLinkCapCamera, line 97
//			logDebug([method: "parsePoll", detectionType: alarmData.last_alarm_type]) // library marker davegut.tpLinkCapCamera, line 98
			runIn(30, setAlertInactive) // library marker davegut.tpLinkCapCamera, line 99
		} // library marker davegut.tpLinkCapCamera, line 100
	} catch (err) { // library marker davegut.tpLinkCapCamera, line 101
		logWarn([method: "parsePoll", error: err, resp: resp.properties]) // library marker davegut.tpLinkCapCamera, line 102
	} // library marker davegut.tpLinkCapCamera, line 103
} // library marker davegut.tpLinkCapCamera, line 104

def setAlertInactive() { // library marker davegut.tpLinkCapCamera, line 106
	sendEvent(name: "motion", value: "inactive") // library marker davegut.tpLinkCapCamera, line 107
} // library marker davegut.tpLinkCapCamera, line 108

def refresh() { // library marker davegut.tpLinkCapCamera, line 110
	List requests = [ // library marker davegut.tpLinkCapCamera, line 111
		[method:"getLedStatus", params:[led:[name:["config"]]]], // library marker davegut.tpLinkCapCamera, line 112
		[method:"getAudioConfig", params:[audio_config:[name:["microphone"]]]], // library marker davegut.tpLinkCapCamera, line 113
		[method:"getTamperDetectionConfig", params:[tamper_detection:[name:"tamper_det"]]], // library marker davegut.tpLinkCapCamera, line 114
		[method:"getTargetTrackConfig", params:[target_track:[name:["target_track_info"]]]], // library marker davegut.tpLinkCapCamera, line 115
		[method:"getBCDConfig", params:[sound_detection:[name:["bcd"]]]], // library marker davegut.tpLinkCapCamera, line 116
		[method:"getPersonDetectionConfig", params:[people_detection:[name:["detection"]]]], // library marker davegut.tpLinkCapCamera, line 117
		[method:"getDetectionConfig", params:[motion_detection:[name:["motion_det"]]]], // library marker davegut.tpLinkCapCamera, line 118
		[method:"getAlertConfig", params:[msg_alarm:[name:"chn1_msg_alarm_info"]]], // library marker davegut.tpLinkCapCamera, line 119
		[method:"getLensMaskConfig", params:[lens_mask:[name:["lens_mask_info"]]]], // library marker davegut.tpLinkCapCamera, line 120
		[method:"getRotationStatus", params:[image:[name:["switch"]]]], // library marker davegut.tpLinkCapCamera, line 121
	] // library marker davegut.tpLinkCapCamera, line 122
	sendDevCmd(requests, "refresh", "parseUpdates") // library marker davegut.tpLinkCapCamera, line 123
} // library marker davegut.tpLinkCapCamera, line 124

def distGetData(devResp) { // library marker davegut.tpLinkCapCamera, line 126
	Map update = [:] // library marker davegut.tpLinkCapCamera, line 127
	if (devResp.result != [:]) { // library marker davegut.tpLinkCapCamera, line 128
		switch(devResp.method) { // library marker davegut.tpLinkCapCamera, line 129
			case "getLensMaskConfig": // library marker davegut.tpLinkCapCamera, line 130
				String camera = "on" // library marker davegut.tpLinkCapCamera, line 131
				if (devResp.result.lens_mask.lens_mask_info.enabled == "on") { // library marker davegut.tpLinkCapCamera, line 132
					camera = "off" // library marker davegut.tpLinkCapCamera, line 133
				} // library marker davegut.tpLinkCapCamera, line 134
				sendEvent(name: "camera", value: camera) // library marker davegut.tpLinkCapCamera, line 135
				update << ["${devResp.method}":[camera: camera]] // library marker davegut.tpLinkCapCamera, line 136
				break // library marker davegut.tpLinkCapCamera, line 137
			case "getAudioConfig": // library marker davegut.tpLinkCapCamera, line 138
				String mute = "off" // library marker davegut.tpLinkCapCamera, line 139
				if (devResp.result.audio_config.microphone.volume.toInteger() == 0) { // library marker davegut.tpLinkCapCamera, line 140
					mute = "on" // library marker davegut.tpLinkCapCamera, line 141
				} // library marker davegut.tpLinkCapCamera, line 142
				sendEvent(name: "mute", value: mute) // library marker davegut.tpLinkCapCamera, line 143
				update << ["${devResp.method}":[mute: mute]] // library marker davegut.tpLinkCapCamera, line 144
				break				 // library marker davegut.tpLinkCapCamera, line 145
			case "getRotationStatus": // library marker davegut.tpLinkCapCamera, line 146
				sendEvent(name: "flip", value: devResp.result.image.switch.flip_type) // library marker davegut.tpLinkCapCamera, line 147
				update << ["${devResp.method}": [flip: flip]] // library marker davegut.tpLinkCapCamera, line 148
				break // library marker davegut.tpLinkCapCamera, line 149
			case "getLedStatus": // library marker davegut.tpLinkCapCamera, line 150
				device.updateSetting("ledRule", [type: "enum", value: devResp.result.led.config.enabled]) // library marker davegut.tpLinkCapCamera, line 151
				update << ["${devResp.method}":[ledRule: devResp.result.led.config.enabled]] // library marker davegut.tpLinkCapCamera, line 152
				break // library marker davegut.tpLinkCapCamera, line 153
			case "getDeviceAlias": // library marker davegut.tpLinkCapCamera, line 154
				String alias = devResp.result.system.sys.dev_alias // library marker davegut.tpLinkCapCamera, line 155
				device.setLabel(alias) // library marker davegut.tpLinkCapCamera, line 156
				device.updateSetting("syncName", [type: "enum", value: "notSet"]) // library marker davegut.tpLinkCapCamera, line 157
				update << ["${devResp.method}": [label: alias]] // library marker davegut.tpLinkCapCamera, line 158
				break // library marker davegut.tpLinkCapCamera, line 159
			case "getDetectionConfig": // library marker davegut.tpLinkCapCamera, line 160
				device.updateSetting("motionDetect", [ // library marker davegut.tpLinkCapCamera, line 161
					type: "enum", // library marker davegut.tpLinkCapCamera, line 162
					value: devResp.result.motion_detection.motion_det.enabled]) // library marker davegut.tpLinkCapCamera, line 163
				update << ["${devResp.method}": [motionDet: devResp.result.motion_detection.motion_det.enabled]] // library marker davegut.tpLinkCapCamera, line 164
				break // library marker davegut.tpLinkCapCamera, line 165
			case "getBCDConfig": // library marker davegut.tpLinkCapCamera, line 166
				device.updateSetting("babyCryDetect", [ // library marker davegut.tpLinkCapCamera, line 167
					type: "enum", // library marker davegut.tpLinkCapCamera, line 168
					value: devResp.result.sound_detection.bcd.enabled]) // library marker davegut.tpLinkCapCamera, line 169
				update << ["${devResp.method}": [babyCryDetece: devResp.result.sound_detection.bcd.enabled]] // library marker davegut.tpLinkCapCamera, line 170
				break // library marker davegut.tpLinkCapCamera, line 171
			case "getTamperDetectionConfig": // library marker davegut.tpLinkCapCamera, line 172
				device.updateSetting("tamperDet", [ // library marker davegut.tpLinkCapCamera, line 173
					type: "enum", // library marker davegut.tpLinkCapCamera, line 174
					value: devResp.result.tamper_detection.tamper_det.enabled]) // library marker davegut.tpLinkCapCamera, line 175
				update << ["${devResp.method}": [tamperDet: devResp.result.tamper_detection.tamper_det.enabled]] // library marker davegut.tpLinkCapCamera, line 176
				break // library marker davegut.tpLinkCapCamera, line 177
			case "getPersonDetectionConfig": // library marker davegut.tpLinkCapCamera, line 178
				device.updateSetting("personDetect", [ // library marker davegut.tpLinkCapCamera, line 179
					type: "enum", // library marker davegut.tpLinkCapCamera, line 180
					value: devResp.result.people_detection.detection.enabled]) // library marker davegut.tpLinkCapCamera, line 181
				update << ["${devResp.method}": [personDetect: devResp.result.people_detection.detection.enabled]] // library marker davegut.tpLinkCapCamera, line 182
				break // library marker davegut.tpLinkCapCamera, line 183
			case "getTargetTrackConfig": // library marker davegut.tpLinkCapCamera, line 184
				device.updateSetting("targetTrack", [ // library marker davegut.tpLinkCapCamera, line 185
					type: "enum", // library marker davegut.tpLinkCapCamera, line 186
					value: devResp.result.target_track.target_track_info.enabled]) // library marker davegut.tpLinkCapCamera, line 187
				update << ["${devResp.method}": [targetTrack: devResp.result.target_track.target_track_info.enabled]] // library marker davegut.tpLinkCapCamera, line 188
				break // library marker davegut.tpLinkCapCamera, line 189
			case "getAlertConfig": // library marker davegut.tpLinkCapCamera, line 190
				Map alarmInfo = devResp.result.msg_alarm.chn1_msg_alarm_info // library marker davegut.tpLinkCapCamera, line 191
				String alarmMode = alarmInfo.enabled // library marker davegut.tpLinkCapCamera, line 192
				if (alarmMode == "on") { // library marker davegut.tpLinkCapCamera, line 193
					if (alarmInfo.alarm_mode.size() == 2) { // library marker davegut.tpLinkCapCamera, line 194
						alarmMode = "sound & light" // library marker davegut.tpLinkCapCamera, line 195
					} else if (alarmInfo.alarm_mode[0] == "sound") { // library marker davegut.tpLinkCapCamera, line 196
						alarmMode = "sound" // library marker davegut.tpLinkCapCamera, line 197
					} else if (alarmInfo.alarm_mode[0] == "light") { // library marker davegut.tpLinkCapCamera, line 198
						alarmMode = "light" // library marker davegut.tpLinkCapCamera, line 199
					} // library marker davegut.tpLinkCapCamera, line 200
				} // library marker davegut.tpLinkCapCamera, line 201
				device.updateSetting("alarmSetting", [type: "enum", value: alarmMode]) // library marker davegut.tpLinkCapCamera, line 202
				update << ["${devResp.method}": [alarmSetting: alarmMode]] // library marker davegut.tpLinkCapCamera, line 203
				break // library marker davegut.tpLinkCapCamera, line 204
			default: // library marker davegut.tpLinkCapCamera, line 205
				if (!devResp.method.contains("set_")) { // library marker davegut.tpLinkCapCamera, line 206
					update << ["${devResp.method}": [result: devResp.result, ERROR: "Unprocessed Response"]] // library marker davegut.tpLinkCapCamera, line 207
				} // library marker davegut.tpLinkCapCamera, line 208
			} // library marker davegut.tpLinkCapCamera, line 209
	} // library marker davegut.tpLinkCapCamera, line 210
	return update // library marker davegut.tpLinkCapCamera, line 211
} // library marker davegut.tpLinkCapCamera, line 212

def on() { setPrivacy("off") } // library marker davegut.tpLinkCapCamera, line 214

def off() { setPrivacy("on") } // library marker davegut.tpLinkCapCamera, line 216
def setPrivacy(enabled) { // library marker davegut.tpLinkCapCamera, line 217
	List requests = [ // library marker davegut.tpLinkCapCamera, line 218
		[method:"setLensMaskConfig", params:[lens_mask:[lens_mask_info:[enabled: enabled]]]], // library marker davegut.tpLinkCapCamera, line 219
		[method:"getLensMaskConfig", params:[lens_mask:[name:["lens_mask_info"]]]] // library marker davegut.tpLinkCapCamera, line 220
	] // library marker davegut.tpLinkCapCamera, line 221
	sendDevCmd(requests, "setPrivacy", "parseUpdates") // library marker davegut.tpLinkCapCamera, line 222
} // library marker davegut.tpLinkCapCamera, line 223

def mute() { setMute(0) } // library marker davegut.tpLinkCapCamera, line 225
def unmute() { setMute(65) } // library marker davegut.tpLinkCapCamera, line 226
def setMute(vol) { // library marker davegut.tpLinkCapCamera, line 227
	List requests = [ // library marker davegut.tpLinkCapCamera, line 228
		[method:"setMicrophoneVolume", params:[ // library marker davegut.tpLinkCapCamera, line 229
			method:"set",audio_config:[microphone:[volume: vol.toInteger()]]]], // library marker davegut.tpLinkCapCamera, line 230
		[method:"getAudioConfig", params:[audio_config:[name:["microphone"]]]] // library marker davegut.tpLinkCapCamera, line 231
] // library marker davegut.tpLinkCapCamera, line 232
	sendDevCmd(requests, "setMute", "parseUpdates") // library marker davegut.tpLinkCapCamera, line 233
} // library marker davegut.tpLinkCapCamera, line 234

def flip() { // library marker davegut.tpLinkCapCamera, line 236
	def newFlip = "off" // library marker davegut.tpLinkCapCamera, line 237
	if (device.currentValue("flip") == "off") { newFlip = "center" } // library marker davegut.tpLinkCapCamera, line 238
	List requests = [ // library marker davegut.tpLinkCapCamera, line 239
		[method:"setRotationStatus", params:[image:[switch: [flip_type: newFlip]]]], // library marker davegut.tpLinkCapCamera, line 240
		[method:"getRotationStatus", params:[image:[name:["switch"]]]] // library marker davegut.tpLinkCapCamera, line 241
	] // library marker davegut.tpLinkCapCamera, line 242
	sendDevCmd(requests, "flip", "parseUpdates") // library marker davegut.tpLinkCapCamera, line 243
} // library marker davegut.tpLinkCapCamera, line 244

def setStatusMsg(statusMessage) { // library marker davegut.tpLinkCapCamera, line 246
	//	Not currently implemented! // library marker davegut.tpLinkCapCamera, line 247
	sendEvent(name: "statusMessage", value: statusMessage) // library marker davegut.tpLinkCapCamera, line 248
} // library marker davegut.tpLinkCapCamera, line 249


// ~~~~~ end include (276) davegut.tpLinkCapCamera ~~~~~

// ~~~~~ start include (279) davegut.tpLinkCapConfiguration ~~~~~
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
/* // library marker davegut.tpLinkCapConfiguration, line 13
Resets device communications.  Also resets device to the configuration of the DEVICE. // library marker davegut.tpLinkCapConfiguration, line 14
*/ // library marker davegut.tpLinkCapConfiguration, line 15
//	def ip = getDataValue("baseUrl").replace("""https://""", "").replace(":443/app", "") // library marker davegut.tpLinkCapConfiguration, line 16
	String devIp = getDataValue("devIp") // library marker davegut.tpLinkCapConfiguration, line 17
	if (devIp == null) { // library marker davegut.tpLinkCapConfiguration, line 18
		//	For legacy code if the update is not done immediately. // library marker davegut.tpLinkCapConfiguration, line 19
		devIp = getDataValue("baseUrl").replace("""https://""","").replace("""http://""","") // library marker davegut.tpLinkCapConfiguration, line 20
		devIp = devIp.replace(":443/app","").replace(":80/app", "").replace(":4433/app","") // library marker davegut.tpLinkCapConfiguration, line 21
		devIp = devIp.replace(":4443","") // library marker davegut.tpLinkCapConfiguration, line 22
	} // library marker davegut.tpLinkCapConfiguration, line 23
	Map logData = [method: "configure", devIp: devIp] // library marker davegut.tpLinkCapConfiguration, line 24
	def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.tpLinkCapConfiguration, line 25
	try { // library marker davegut.tpLinkCapConfiguration, line 26
		sendFindCmd(devIp, "20002", cmdData, "configure2", timeout) // library marker davegut.tpLinkCapConfiguration, line 27
		logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 28
	} catch (err) { // library marker davegut.tpLinkCapConfiguration, line 29
		def parentChecked = parent.tpLinkCheckForDevices(5) // library marker davegut.tpLinkCapConfiguration, line 30
		logData << [status: "FAILED", error: err, parentChecked: parentChecked] // library marker davegut.tpLinkCapConfiguration, line 31
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 32
		configure3() // library marker davegut.tpLinkCapConfiguration, line 33
	} // library marker davegut.tpLinkCapConfiguration, line 34
} // library marker davegut.tpLinkCapConfiguration, line 35

def configure2(response) { // library marker davegut.tpLinkCapConfiguration, line 37
	Map logData = [method: "configure2"] // library marker davegut.tpLinkCapConfiguration, line 38
	def respData = parseLanMessage(response) // library marker davegut.tpLinkCapConfiguration, line 39
	String hubDni = device.getDeviceNetworkId() // library marker davegut.tpLinkCapConfiguration, line 40
	logData << [dni: respData.mac, hubDni: hubDni] // library marker davegut.tpLinkCapConfiguration, line 41
	def parentChecked = false // library marker davegut.tpLinkCapConfiguration, line 42
	if (respData.mac != hubDni) { // library marker davegut.tpLinkCapConfiguration, line 43
		logData << [status: "device/ip not found", action: "parentCheck", // library marker davegut.tpLinkCapConfiguration, line 44
				    parentChecked: parent.tpLinkCheckForDevices(5)] // library marker davegut.tpLinkCapConfiguration, line 45
	} else { // library marker davegut.tpLinkCapConfiguration, line 46
		logData << [status: "device/ip found"] // library marker davegut.tpLinkCapConfiguration, line 47
	} // library marker davegut.tpLinkCapConfiguration, line 48
	configure3() // library marker davegut.tpLinkCapConfiguration, line 49
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 50
} // library marker davegut.tpLinkCapConfiguration, line 51
def configure3() { // library marker davegut.tpLinkCapConfiguration, line 52
	Map logData = [method: "configure3"] // library marker davegut.tpLinkCapConfiguration, line 53
	logData <<[updateDeviceData: updateDeviceData(true)] // library marker davegut.tpLinkCapConfiguration, line 54
	logData << [deviceHandshake: deviceHandshake()] // library marker davegut.tpLinkCapConfiguration, line 55
	runEvery3Hours("deviceHandshake") // library marker davegut.tpLinkCapConfiguration, line 56
	logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCapConfiguration, line 57
	runIn(5, refresh) // library marker davegut.tpLinkCapConfiguration, line 58
	logInfo(logData) // library marker davegut.tpLinkCapConfiguration, line 59
} // library marker davegut.tpLinkCapConfiguration, line 60

def deviceHandshake() { // library marker davegut.tpLinkCapConfiguration, line 62
	def protocol = getDataValue("protocol") // library marker davegut.tpLinkCapConfiguration, line 63
	Map logData = [method: "deviceHandshake", protocol: protocol] // library marker davegut.tpLinkCapConfiguration, line 64
	if (protocol == "KLAP") { // library marker davegut.tpLinkCapConfiguration, line 65
		klapHandshake(getDataValue("baseUrl"), parent.localHash) // library marker davegut.tpLinkCapConfiguration, line 66
	} else if (protocol == "camera") { // library marker davegut.tpLinkCapConfiguration, line 67
		cameraHandshake(getDataValue("baseUrl"), parent.userName, parent.encPasswordCam) // library marker davegut.tpLinkCapConfiguration, line 68
	} else if (protocol == "AES") { // library marker davegut.tpLinkCapConfiguration, line 69
		aesHandshake() // library marker davegut.tpLinkCapConfiguration, line 70
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkCapConfiguration, line 71
		vacAesHandshake(getDataValue("baseUrl"), parent.userName, parent.encPasswordVac) // library marker davegut.tpLinkCapConfiguration, line 72
	} else { // library marker davegut.tpLinkCapConfiguration, line 73
		logData << [ERROR: "Protocol not supported"] // library marker davegut.tpLinkCapConfiguration, line 74
		logWarn(logData) // library marker davegut.tpLinkCapConfiguration, line 75
	} // library marker davegut.tpLinkCapConfiguration, line 76
	return logData // library marker davegut.tpLinkCapConfiguration, line 77
} // library marker davegut.tpLinkCapConfiguration, line 78

// ~~~~~ end include (279) davegut.tpLinkCapConfiguration ~~~~~

// ~~~~~ start include (277) davegut.tpLinkCamCommon ~~~~~
library ( // library marker davegut.tpLinkCamCommon, line 1
	name: "tpLinkCamCommon", // library marker davegut.tpLinkCamCommon, line 2
	namespace: "davegut", // library marker davegut.tpLinkCamCommon, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.tpLinkCamCommon, line 4
	description: "Common Camera driver methods", // library marker davegut.tpLinkCamCommon, line 5
	category: "utilities", // library marker davegut.tpLinkCamCommon, line 6
	documentationLink: "" // library marker davegut.tpLinkCamCommon, line 7
) // library marker davegut.tpLinkCamCommon, line 8

attribute "commsError", "string" // library marker davegut.tpLinkCamCommon, line 10

def commonPreferences() { // library marker davegut.tpLinkCamCommon, line 12
	input ("syncName", "enum", title: "Update Device Names and Labels",  // library marker davegut.tpLinkCamCommon, line 13
		   options: ["hubMaster", "tapoAppMaster", "notSet"], defaultValue: "notSet") // library marker davegut.tpLinkCamCommon, line 14
	input ("rebootDev", "bool", title: "Reboot Device", defaultValue: false) // library marker davegut.tpLinkCamCommon, line 15
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false) // library marker davegut.tpLinkCamCommon, line 16
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true) // library marker davegut.tpLinkCamCommon, line 17
} // library marker davegut.tpLinkCamCommon, line 18

def commonInstalled() { // library marker davegut.tpLinkCamCommon, line 20
	Map logData = [method: "commonInstalled"] // library marker davegut.tpLinkCamCommon, line 21
	updateAttr("commsError", "false") // library marker davegut.tpLinkCamCommon, line 22
	state.errorCount = 0 // library marker davegut.tpLinkCamCommon, line 23
	logData << [configure: configure()] // library marker davegut.tpLinkCamCommon, line 24
	return logData // library marker davegut.tpLinkCamCommon, line 25
} // library marker davegut.tpLinkCamCommon, line 26

def commonUpdated() { // library marker davegut.tpLinkCamCommon, line 28
	unschedule() // library marker davegut.tpLinkCamCommon, line 29
	sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkCamCommon, line 30
	state.errorCount = 0 // library marker davegut.tpLinkCamCommon, line 31
	Map logData = [commsError: "cleared"] // library marker davegut.tpLinkCamCommon, line 32
	logData << [logging: setLogsOff()] // library marker davegut.tpLinkCamCommon, line 33
	if (rebootDev == true) { // library marker davegut.tpLinkCamCommon, line 34
		List requests = [[method: "rebootDevice", params: [system: [reboot: "" ]]]] // library marker davegut.tpLinkCamCommon, line 35
		sendDevCmd(requests, "rebootDev", "finishReboot") // library marker davegut.tpLinkCamCommon, line 36
		logData << [rebootDevice: "device reboot being attempted"] // library marker davegut.tpLinkCamCommon, line 37
	} else { // library marker davegut.tpLinkCamCommon, line 38
		runEvery3Hours(deviceHandshake) // library marker davegut.tpLinkCamCommon, line 39
		logData << [handshakeInterval: "3 Hours"] // library marker davegut.tpLinkCamCommon, line 40
		runEvery30Minutes(refresh) // library marker davegut.tpLinkCamCommon, line 41
		logData << [refreshInterval: "30 minutes"] // library marker davegut.tpLinkCamCommon, line 42
		logData << [pollInterval: setPollInterval()] // library marker davegut.tpLinkCamCommon, line 43
		logData << [updateDevSettings: updDevSettings()] // library marker davegut.tpLinkCamCommon, line 44
		runIn(2, refresh) // library marker davegut.tpLinkCamCommon, line 45
	} // library marker davegut.tpLinkCamCommon, line 46
	return logData // library marker davegut.tpLinkCamCommon, line 47
} // library marker davegut.tpLinkCamCommon, line 48

def finishReboot(resp, data = null) { // library marker davegut.tpLinkCamCommon, line 50
	Map logData = [method: "finishReboot"] // library marker davegut.tpLinkCamCommon, line 51
	logData << [wait: "<b>20s for device to reconnect to LAN</b>", action: "executing deviceHandshake"] // library marker davegut.tpLinkCamCommon, line 52
	device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.tpLinkCamCommon, line 53
	runIn(30, configure) // library marker davegut.tpLinkCamCommon, line 54
	logInfo(logData) // library marker davegut.tpLinkCamCommon, line 55
} // library marker davegut.tpLinkCamCommon, line 56

//	===== Data Distribution (and parse) ===== // library marker davegut.tpLinkCamCommon, line 58
def parseUpdates(resp, data = null) { // library marker davegut.tpLinkCamCommon, line 59
	Map logData = [method: "parseUpdates", sourceMethod: data] // library marker davegut.tpLinkCamCommon, line 60
	if (resp.json.error_code != 0) { // library marker davegut.tpLinkCamCommon, line 61
		logData << [ERROR: "Invalid Device Command", errorCode: resp.json.error_code, // library marker davegut.tpLinkCamCommon, line 62
				    action: "RUN CONFIGURE"] // library marker davegut.tpLinkCamCommon, line 63
		logWarn(logData) // library marker davegut.tpLinkCamCommon, line 64
		return // library marker davegut.tpLinkCamCommon, line 65
	} // library marker davegut.tpLinkCamCommon, line 66
	def respData = parseData(resp, getDataValue("protocol"), data) // library marker davegut.tpLinkCamCommon, line 67
	if (resp.status == 200 && respData.cryptoStatus == "OK") { // library marker davegut.tpLinkCamCommon, line 68
		Map updates = [:] // library marker davegut.tpLinkCamCommon, line 69
		if (respData.cmdResp.result.responses != null) { // library marker davegut.tpLinkCamCommon, line 70
			respData.cmdResp.result.responses.each { // library marker davegut.tpLinkCamCommon, line 71
				if (it.error_code == 0) { // library marker davegut.tpLinkCamCommon, line 72
					updates << distGetData(it) // library marker davegut.tpLinkCamCommon, line 73
				} else { // library marker davegut.tpLinkCamCommon, line 74
					logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCamCommon, line 75
				} // library marker davegut.tpLinkCamCommon, line 76
			} // library marker davegut.tpLinkCamCommon, line 77
		} else if (respData.cmdResp.result.responseData != null) { // library marker davegut.tpLinkCamCommon, line 78
			respData.cmdResp.result.responseData.result.responses.each { // library marker davegut.tpLinkCamCommon, line 79
				if (it.error_code == 0) { // library marker davegut.tpLinkCamCommon, line 80
					updates << distChildGetData(it, data) // library marker davegut.tpLinkCamCommon, line 81
				} else { // library marker davegut.tpLinkCamCommon, line 82
					logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.tpLinkCamCommon, line 83
					logDebug(logData) // library marker davegut.tpLinkCamCommon, line 84
				} // library marker davegut.tpLinkCamCommon, line 85
			} // library marker davegut.tpLinkCamCommon, line 86
		} else { // library marker davegut.tpLinkCamCommon, line 87
			logData << [errorMsg: "Unknown Return from Device", respData: respData] // library marker davegut.tpLinkCamCommon, line 88
			logWarn(logData) // library marker davegut.tpLinkCamCommon, line 89
		} // library marker davegut.tpLinkCamCommon, line 90
		logData << [updates: updates] // library marker davegut.tpLinkCamCommon, line 91
		logDebug(logData) // library marker davegut.tpLinkCamCommon, line 92
	} else { // library marker davegut.tpLinkCamCommon, line 93
		logData << [errorMsg: "Misc Error"] // library marker davegut.tpLinkCamCommon, line 94
		logWarn(logData) // library marker davegut.tpLinkCamCommon, line 95
	} // library marker davegut.tpLinkCamCommon, line 96
} // library marker davegut.tpLinkCamCommon, line 97

def sendDevCmd(requests, data, action) { // library marker davegut.tpLinkCamCommon, line 99
	Map cmdBody = [ // library marker davegut.tpLinkCamCommon, line 100
		method: "multipleRequest", // library marker davegut.tpLinkCamCommon, line 101
		params: [requests: requests]] // library marker davegut.tpLinkCamCommon, line 102
	asyncSend(cmdBody, data, action) // library marker davegut.tpLinkCamCommon, line 103
} // library marker davegut.tpLinkCamCommon, line 104

def nullParse(resp, data) { logDebug "nullParse" } // library marker davegut.tpLinkCamCommon, line 106

//	===== Check/Update device data ===== // library marker davegut.tpLinkCamCommon, line 108
def updateDeviceData(fromConfig = false) { // library marker davegut.tpLinkCamCommon, line 109
	def devData = parent.getDeviceData(device.getDeviceNetworkId()) // library marker davegut.tpLinkCamCommon, line 110
	updateChild(devData, fromConfig) // library marker davegut.tpLinkCamCommon, line 111
	return [updateDeviceData: "updating with app data"] // library marker davegut.tpLinkCamCommon, line 112
} // library marker davegut.tpLinkCamCommon, line 113

def updateChild(devData, fromConfig = false) { // library marker davegut.tpLinkCamCommon, line 115
	def currVersion = getDataValue("version") // library marker davegut.tpLinkCamCommon, line 116
	Map logData = [method: "updateChild", devData: devData] // library marker davegut.tpLinkCamCommon, line 117
	if (devData != null) { // library marker davegut.tpLinkCamCommon, line 118
		devData.each { // library marker davegut.tpLinkCamCommon, line 119
			if (it.key != "deviceType" && it.key != "model" && it.key != "alias") { // library marker davegut.tpLinkCamCommon, line 120
				updateDataValue(it.key, it.value) // library marker davegut.tpLinkCamCommon, line 121
			} // library marker davegut.tpLinkCamCommon, line 122
		} // library marker davegut.tpLinkCamCommon, line 123
		if (currVersion != version()) { // library marker davegut.tpLinkCamCommon, line 124
			updateDataValue("version", version()) // library marker davegut.tpLinkCamCommon, line 125
			logData << [updateVersion: version()] // library marker davegut.tpLinkCamCommon, line 126
			runIn(20, updated) // library marker davegut.tpLinkCamCommon, line 127
		} // library marker davegut.tpLinkCamCommon, line 128
	} else { // library marker davegut.tpLinkCamCommon, line 129
		logData << [Note: "DEVICE DATA IS NULL"] // library marker davegut.tpLinkCamCommon, line 130
	} // library marker davegut.tpLinkCamCommon, line 131
	if (!fromConfig) { deviceHandshake() } // library marker davegut.tpLinkCamCommon, line 132
	logInfo(logData) // library marker davegut.tpLinkCamCommon, line 133
} // library marker davegut.tpLinkCamCommon, line 134

// ~~~~~ end include (277) davegut.tpLinkCamCommon ~~~~~

// ~~~~~ start include (261) davegut.tpLinkComms ~~~~~
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
def handleCommsError(status, msg = "") { // library marker davegut.tpLinkComms, line 67
	//	Retransmit all comms error except Switch and Level related (Hub retries for these). // library marker davegut.tpLinkComms, line 68
	//	This is determined by state.digital // library marker davegut.tpLinkComms, line 69
	if (status == 200) { // library marker davegut.tpLinkComms, line 70
		setCommsError(status, "OK") // library marker davegut.tpLinkComms, line 71
	} else { // library marker davegut.tpLinkComms, line 72
		Map logData = [method: "handleCommsError", status: code, msg: msg] // library marker davegut.tpLinkComms, line 73
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 74
		logData << [count: count, status: status, msg: msg] // library marker davegut.tpLinkComms, line 75
		switch(count) { // library marker davegut.tpLinkComms, line 76
			case 1: // library marker davegut.tpLinkComms, line 77
			case 2: // library marker davegut.tpLinkComms, line 78
				//	errors 1 and 2, retry immediately // library marker davegut.tpLinkComms, line 79
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 80
				break // library marker davegut.tpLinkComms, line 81
			case 3: // library marker davegut.tpLinkComms, line 82
				//	error 3, login or scan find device on the lan // library marker davegut.tpLinkComms, line 83
				//	then retry // library marker davegut.tpLinkComms, line 84
				if (status == 403) { // library marker davegut.tpLinkComms, line 85
					logData << [action: "attemptLogin"] // library marker davegut.tpLinkComms, line 86
					deviceHandshake() // library marker davegut.tpLinkComms, line 87
					runIn(4, delayedPassThrough) // library marker davegut.tpLinkComms, line 88
				} else { // library marker davegut.tpLinkComms, line 89
					logData << [action: "Find on LAN then login"] // library marker davegut.tpLinkComms, line 90
					configure() // library marker davegut.tpLinkComms, line 91
					runIn(10, delayedPassThrough) // library marker davegut.tpLinkComms, line 92
				} // library marker davegut.tpLinkComms, line 93
				break // library marker davegut.tpLinkComms, line 94
			case 4: // library marker davegut.tpLinkComms, line 95
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 96
				break // library marker davegut.tpLinkComms, line 97
			default: // library marker davegut.tpLinkComms, line 98
				//	Set comms error first time errros are 5 or more. // library marker davegut.tpLinkComms, line 99
				logData << [action: "SetCommsErrorTrue"] // library marker davegut.tpLinkComms, line 100
				setCommsError(status, msg, 5) // library marker davegut.tpLinkComms, line 101
		} // library marker davegut.tpLinkComms, line 102
		state.errorCount = count // library marker davegut.tpLinkComms, line 103
		logInfo(logData) // library marker davegut.tpLinkComms, line 104
	} // library marker davegut.tpLinkComms, line 105
} // library marker davegut.tpLinkComms, line 106

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 108
	def cmdData = new JSONObject(state.lastCommand) // library marker davegut.tpLinkComms, line 109
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 110
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 111
} // library marker davegut.tpLinkComms, line 112

def ping(baseUrl = getDataValue("baseUrl"), count = 1) { // library marker davegut.tpLinkComms, line 114
	def ip = baseUrl.replace("""http://""", "").replace(":80/app", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 115
	ip = ip.replace("""https://""", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 116
	hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(ip, count) // library marker davegut.tpLinkComms, line 117
	Map pingReturn = [method: "ping", ip: ip] // library marker davegut.tpLinkComms, line 118
	if (pingData.packetsReceived == count) { // library marker davegut.tpLinkComms, line 119
		pingReturn << [pingStatus: "success"] // library marker davegut.tpLinkComms, line 120
		logDebug(pingReturn) // library marker davegut.tpLinkComms, line 121
	} else { // library marker davegut.tpLinkComms, line 122
		pingReturn << [pingData: pingData, pingStatus: "<b>FAILED</b>.  There may be issues with your LAN."] // library marker davegut.tpLinkComms, line 123
		logWarn(pingReturn) // library marker davegut.tpLinkComms, line 124
	} // library marker davegut.tpLinkComms, line 125
	return pingReturn // library marker davegut.tpLinkComms, line 126
} // library marker davegut.tpLinkComms, line 127

def setCommsError(status, msg = "OK", count = state.commsError) { // library marker davegut.tpLinkComms, line 129
	Map logData = [method: "setCommsError", status: status, errorMsg: msg, count: count] // library marker davegut.tpLinkComms, line 130
	if (device && status == 200) { // library marker davegut.tpLinkComms, line 131
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 132
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 133
			sendEvent(name: "commsError", value: "false") // library marker davegut.tpLinkComms, line 134
			setPollInterval() // library marker davegut.tpLinkComms, line 135
			unschedule("errorDeviceHandshake") // library marker davegut.tpLinkComms, line 136
			logInfo(logData) // library marker davegut.tpLinkComms, line 137
		} // library marker davegut.tpLinkComms, line 138
	} else if (device) { // library marker davegut.tpLinkComms, line 139
		if (device.currentValue("commsError") == "false" && count > 4) { // library marker davegut.tpLinkComms, line 140
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 141
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 142
			runEvery10Minutes(errorConfigure) // library marker davegut.tpLinkComms, line 143
			logData << [pollInterval: "30 Min", errorDeviceHandshake: "ever 10 min"] // library marker davegut.tpLinkComms, line 144
			logWarn(logData) // library marker davegut.tpLinkComms, line 145
			if (status == 403) { // library marker davegut.tpLinkComms, line 146
				logWarn(logInErrorAction()) // library marker davegut.tpLinkComms, line 147
			} else { // library marker davegut.tpLinkComms, line 148
				logWarn(lanErrorAction()) // library marker davegut.tpLinkComms, line 149
			} // library marker davegut.tpLinkComms, line 150
		} else { // library marker davegut.tpLinkComms, line 151
			logData << [error: "Unspecified Error"] // library marker davegut.tpLinkComms, line 152
			logWarn(logData) // library marker davegut.tpLinkComms, line 153
		} // library marker davegut.tpLinkComms, line 154
	} // library marker davegut.tpLinkComms, line 155
} // library marker davegut.tpLinkComms, line 156

def lanErrorAction() { // library marker davegut.tpLinkComms, line 158
	def action = "Likely cause of this error is YOUR LAN device configuration: " // library marker davegut.tpLinkComms, line 159
	action += "a. VERIFY your device is on the DHCP list in your router, " // library marker davegut.tpLinkComms, line 160
	action += "b. VERIFY your device is in the active device list in your router, and " // library marker davegut.tpLinkComms, line 161
	action += "c. TRY controlling your device from the TAPO phone app." // library marker davegut.tpLinkComms, line 162
	return action // library marker davegut.tpLinkComms, line 163
} // library marker davegut.tpLinkComms, line 164

def logInErrorAction() { // library marker davegut.tpLinkComms, line 166
	def action = "Likely cause is your login credentials are incorrect or the login has expired. " // library marker davegut.tpLinkComms, line 167
	action += "a. RUN command Configure. b. If error persists, check your credentials in the App" // library marker davegut.tpLinkComms, line 168
	return action // library marker davegut.tpLinkComms, line 169
} // library marker davegut.tpLinkComms, line 170

def errorConfigure() { // library marker davegut.tpLinkComms, line 172
	logDebug([method: "errorConfigure"]) // library marker davegut.tpLinkComms, line 173
	configure() // library marker davegut.tpLinkComms, line 174
} // library marker davegut.tpLinkComms, line 175

//	===== Common UDP Communications for checking if device at IP is device in Hubitat ===== // library marker davegut.tpLinkComms, line 177
private sendFindCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) { // library marker davegut.tpLinkComms, line 178
	def myHubAction = new hubitat.device.HubAction( // library marker davegut.tpLinkComms, line 179
		cmdData, // library marker davegut.tpLinkComms, line 180
		hubitat.device.Protocol.LAN, // library marker davegut.tpLinkComms, line 181
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, // library marker davegut.tpLinkComms, line 182
		 destinationAddress: "${ip}:${port}", // library marker davegut.tpLinkComms, line 183
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING, // library marker davegut.tpLinkComms, line 184
		 ignoreResponse: ignore, // library marker davegut.tpLinkComms, line 185
		 parseWarning: true, // library marker davegut.tpLinkComms, line 186
		 timeout: commsTo, // library marker davegut.tpLinkComms, line 187
		 callback: action]) // library marker davegut.tpLinkComms, line 188
	try { // library marker davegut.tpLinkComms, line 189
		sendHubCommand(myHubAction) // library marker davegut.tpLinkComms, line 190
	} catch (error) { // library marker davegut.tpLinkComms, line 191
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}") // library marker davegut.tpLinkComms, line 192
	} // library marker davegut.tpLinkComms, line 193
	return // library marker davegut.tpLinkComms, line 194
} // library marker davegut.tpLinkComms, line 195

// ~~~~~ end include (261) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (262) davegut.tpLinkCrypto ~~~~~
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

// ~~~~~ end include (262) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (245) davegut.tpLinkTransCamera ~~~~~
library ( // library marker davegut.tpLinkTransCamera, line 1
	name: "tpLinkTransCamera", // library marker davegut.tpLinkTransCamera, line 2
	namespace: "davegut", // library marker davegut.tpLinkTransCamera, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkTransCamera, line 4
	description: "TP-Link Camera Protocol Implementation", // library marker davegut.tpLinkTransCamera, line 5
	category: "utilities", // library marker davegut.tpLinkTransCamera, line 6
	documentationLink: "" // library marker davegut.tpLinkTransCamera, line 7
) // library marker davegut.tpLinkTransCamera, line 8
import java.util.Random // library marker davegut.tpLinkTransCamera, line 9
import java.security.MessageDigest // library marker davegut.tpLinkTransCamera, line 10
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tpLinkTransCamera, line 11
import javax.crypto.Cipher // library marker davegut.tpLinkTransCamera, line 12
import java.security.KeyFactory // library marker davegut.tpLinkTransCamera, line 13
import javax.crypto.spec.SecretKeySpec // library marker davegut.tpLinkTransCamera, line 14
import javax.crypto.spec.IvParameterSpec // library marker davegut.tpLinkTransCamera, line 15

//	===== Device Confirm ===== // library marker davegut.tpLinkTransCamera, line 17
def cameraHandshake(baseUrl, userName, encPasswordCam, devData = [:]) { // library marker davegut.tpLinkTransCamera, line 18
	String cnonce = getSeed(8).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 19
	def deviceConfirmed = false // library marker davegut.tpLinkTransCamera, line 20
	Map cmdBody = [ // library marker davegut.tpLinkTransCamera, line 21
	method: "login", // library marker davegut.tpLinkTransCamera, line 22
	params: [cnonce: cnonce, // library marker davegut.tpLinkTransCamera, line 23
			 encrypt_type: "3", // library marker davegut.tpLinkTransCamera, line 24
			 username: userName // library marker davegut.tpLinkTransCamera, line 25
			]] // library marker davegut.tpLinkTransCamera, line 26
	Map respData = postSync(cmdBody, baseUrl) // library marker davegut.tpLinkTransCamera, line 27
	if (respData.status) { return deviceConfirmed } // library marker davegut.tpLinkTransCamera, line 28
	Map logData = [method: "cameraHandshake", errorCode: respData.error_code] // library marker davegut.tpLinkTransCamera, line 29
	if (respData.error_code == -40413) { // library marker davegut.tpLinkTransCamera, line 30
		def results = respData.result.data // library marker davegut.tpLinkTransCamera, line 31
		if (results.code == -40401 && results.encrypt_type.toString().contains("3")) { // library marker davegut.tpLinkTransCamera, line 32
			String deviceConfirm = results.device_confirm // library marker davegut.tpLinkTransCamera, line 33
			String nonce = results.nonce // library marker davegut.tpLinkTransCamera, line 34
			String noncesPwdHash = cnonce + encPasswordCam + nonce // library marker davegut.tpLinkTransCamera, line 35
			String testHash = mdEncode("SHA-256", noncesPwdHash.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 36
			String checkData = testHash + nonce + cnonce // library marker davegut.tpLinkTransCamera, line 37
			if (checkData == deviceConfirm) {  // library marker davegut.tpLinkTransCamera, line 38
				deviceConfirmed = true // library marker davegut.tpLinkTransCamera, line 39
				if(device) { // library marker davegut.tpLinkTransCamera, line 40
					device.updateSetting("nonce", [type:"password", value: nonce]) // library marker davegut.tpLinkTransCamera, line 41
					device.updateSetting("cnonce", [type:"password", value: cnonce]) // library marker davegut.tpLinkTransCamera, line 42
				} // library marker davegut.tpLinkTransCamera, line 43
				Map handshakeData = [baseUrl: baseUrl, encPasswordCam: encPasswordCam, // library marker davegut.tpLinkTransCamera, line 44
									 nonce: nonce, cnonce: cnonce] // library marker davegut.tpLinkTransCamera, line 45
				logData << [methodStatus: "OK", deviceConfirmed: deviceConfirmed] // library marker davegut.tpLinkTransCamera, line 46
				getToken(handshakeData, devData) // library marker davegut.tpLinkTransCamera, line 47
			} else { // library marker davegut.tpLinkTransCamera, line 48
				logData << [methodStatus: "deviceConfirmFailed", deviceConfirmed: deviceConfirmed] // library marker davegut.tpLinkTransCamera, line 49
			} // library marker davegut.tpLinkTransCamera, line 50
		} else { // library marker davegut.tpLinkTransCamera, line 51
			logData << [methodStatus: "unknownError", respData: respData, // library marker davegut.tpLinkTransCamera, line 52
					    action: "Check username and password"] // library marker davegut.tpLinkTransCamera, line 53
		} // library marker davegut.tpLinkTransCamera, line 54
	} else if (respData.error_code == -40401) { // library marker davegut.tpLinkTransCamera, line 55
		if (respData.data.code == -40404) { // library marker davegut.tpLinkTransCamera, line 56
			logData << [methodStatus: "retryLimitExceeded",  // library marker davegut.tpLinkTransCamera, line 57
						timeToRetry: "${respData.data.sec_left} seconds", // library marker davegut.tpLinkTransCamera, line 58
					    action: "Try again later"] // library marker davegut.tpLinkTransCamera, line 59
		} else { // library marker davegut.tpLinkTransCamera, line 60
			logData << [methodStatus: "unknownError", respData: respData] // library marker davegut.tpLinkTransCamera, line 61
		} // library marker davegut.tpLinkTransCamera, line 62
	} else { // library marker davegut.tpLinkTransCamera, line 63
		logData << [methodStatus: "unknownError", respData: respData] // library marker davegut.tpLinkTransCamera, line 64
	} // library marker davegut.tpLinkTransCamera, line 65

	if (logData.methodStatus == "OK") { // library marker davegut.tpLinkTransCamera, line 67
		logDebug(logData) // library marker davegut.tpLinkTransCamera, line 68
	} else { // library marker davegut.tpLinkTransCamera, line 69
		logWarn(logData) // library marker davegut.tpLinkTransCamera, line 70
	} // library marker davegut.tpLinkTransCamera, line 71
	return deviceConfirmed // library marker davegut.tpLinkTransCamera, line 72
} // library marker davegut.tpLinkTransCamera, line 73

def getCamToken() { // library marker davegut.tpLinkTransCamera, line 75
	Map handshakeData = [baseUrl: getDataValue("baseUrl"), encPasswordCam: parent.encPasswordCam, // library marker davegut.tpLinkTransCamera, line 76
						 cnonce: cnonce, nonce: nonce, cnonce: cnonce] // library marker davegut.tpLinkTransCamera, line 77
    def tokenResp = getToken(handshakeData) // library marker davegut.tpLinkTransCamera, line 78
    logInfo([getCamToken: tokenResp]) // library marker davegut.tpLinkTransCamera, line 79
    return tokenResp // library marker davegut.tpLinkTransCamera, line 80
} // library marker davegut.tpLinkTransCamera, line 81
def getToken(handshakeData, devData = [:]) { // library marker davegut.tpLinkTransCamera, line 82
	String digestPwdHex = handshakeData.encPasswordCam + handshakeData.cnonce + handshakeData.nonce // library marker davegut.tpLinkTransCamera, line 83
	String digestPwd = mdEncode("SHA-256", digestPwdHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 84
	String fullDigestPwdHex = digestPwd + handshakeData.cnonce + handshakeData.nonce // library marker davegut.tpLinkTransCamera, line 85
	String fullDigestPwd = new String(fullDigestPwdHex.getBytes(), "UTF-8") // library marker davegut.tpLinkTransCamera, line 86
	def hasToken = false // library marker davegut.tpLinkTransCamera, line 87

	Map cmdBody = [ // library marker davegut.tpLinkTransCamera, line 89
	method: "login", // library marker davegut.tpLinkTransCamera, line 90
	params: [cnonce: cnonce,  // library marker davegut.tpLinkTransCamera, line 91
			 encrypt_type: "3", // library marker davegut.tpLinkTransCamera, line 92
			 digest_passwd: fullDigestPwd, // library marker davegut.tpLinkTransCamera, line 93
			 username: userName // library marker davegut.tpLinkTransCamera, line 94
			]] // library marker davegut.tpLinkTransCamera, line 95
	Map respData = postSync(cmdBody, handshakeData.baseUrl) // library marker davegut.tpLinkTransCamera, line 96
	Map logData = [method: "getToken", errorCode: respData.error_code, respData: respData] // library marker davegut.tpLinkTransCamera, line 97
	if (respData.error_code == 0) { // library marker davegut.tpLinkTransCamera, line 98
		Map result = respData.result // library marker davegut.tpLinkTransCamera, line 99
		if (result != null) { // library marker davegut.tpLinkTransCamera, line 100
			Integer startSeq = result.start_seq // library marker davegut.tpLinkTransCamera, line 101
			String stok = result.stok // library marker davegut.tpLinkTransCamera, line 102
			String userGroup = result.user_group // library marker davegut.tpLinkTransCamera, line 103
			if (startSeq != null) { // library marker davegut.tpLinkTransCamera, line 104
				if (userGroup == "root") { // library marker davegut.tpLinkTransCamera, line 105
					hasToken = true // library marker davegut.tpLinkTransCamera, line 106
					byte[] lsk = genEncryptToken("lsk", handshakeData) // library marker davegut.tpLinkTransCamera, line 107
					byte[] ivb = genEncryptToken("ivb", handshakeData) // library marker davegut.tpLinkTransCamera, line 108
					String apiUrl = "${handshakeData.baseUrl}/stok=${stok}/ds" // library marker davegut.tpLinkTransCamera, line 109
					handshakeData << [seqNo: startSeq, lsk: lsk, ivb: ivb, apiUrl: apiUrl, respStatus: "OK"] // library marker davegut.tpLinkTransCamera, line 110
					if (device) { // library marker davegut.tpLinkTransCamera, line 111
						device.updateSetting("lsk",[type:"password", value: lsk]) // library marker davegut.tpLinkTransCamera, line 112
						device.updateSetting("ivb",[type:"password", value: ivb]) // library marker davegut.tpLinkTransCamera, line 113
						device.updateSetting("apiUrl",[type:"password", value: apiUrl]) // library marker davegut.tpLinkTransCamera, line 114
						state.seqNo = startSeq // library marker davegut.tpLinkTransCamera, line 115
					} else { // library marker davegut.tpLinkTransCamera, line 116
						sendCameraDataCmd(handshakeData, devData) // library marker davegut.tpLinkTransCamera, line 117
					} // library marker davegut.tpLinkTransCamera, line 118
					logData << [status: "success", data: data] // library marker davegut.tpLinkTransCamera, line 119
				} else { // library marker davegut.tpLinkTransCamera, line 120
					logData << [status: "invalidUserGroup", respData: respData] // library marker davegut.tpLinkTransCamera, line 121
				} // library marker davegut.tpLinkTransCamera, line 122
			} else { // library marker davegut.tpLinkTransCamera, line 123
				logData << [status: "nullStartSeq", respData: respData] // library marker davegut.tpLinkTransCamera, line 124
			} // library marker davegut.tpLinkTransCamera, line 125
		} else { // library marker davegut.tpLinkTransCamera, line 126
			logData << [status: "nullDataFrom Device", respData: respData] // library marker davegut.tpLinkTransCamera, line 127
			status = "nullDataFrom Device" // library marker davegut.tpLinkTransCamera, line 128
		} // library marker davegut.tpLinkTransCamera, line 129
	} else { // library marker davegut.tpLinkTransCamera, line 130
		logData << [status: "credentialError", respData: respData] // library marker davegut.tpLinkTransCamera, line 131
		status = "credentialError" // library marker davegut.tpLinkTransCamera, line 132
	} // library marker davegut.tpLinkTransCamera, line 133
	if (logData.status == "success") { // library marker davegut.tpLinkTransCamera, line 134
		logDebug("getToken OK") // library marker davegut.tpLinkTransCamera, line 135
	} else { // library marker davegut.tpLinkTransCamera, line 136
		logData << [message: "<b>Try configure()</b>"] // library marker davegut.tpLinkTransCamera, line 137
		logWarn(logData) // library marker davegut.tpLinkTransCamera, line 138
	} // library marker davegut.tpLinkTransCamera, line 139
	return hasToken // library marker davegut.tpLinkTransCamera, line 140
} // library marker davegut.tpLinkTransCamera, line 141
def genEncryptToken(tokenType, handshakeData) { // library marker davegut.tpLinkTransCamera, line 142
	String hashedKeyHex = handshakeData.cnonce + handshakeData.encPasswordCam + handshakeData.nonce // library marker davegut.tpLinkTransCamera, line 143
	String hashedKey = mdEncode("SHA-256", hashedKeyHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 144
	String tokenHex = tokenType + handshakeData.cnonce + handshakeData.nonce + hashedKey // library marker davegut.tpLinkTransCamera, line 145
	byte[] tokenFull = mdEncode("SHA-256", tokenHex.getBytes()) // library marker davegut.tpLinkTransCamera, line 146
	return tokenFull[0..15] // library marker davegut.tpLinkTransCamera, line 147
} // library marker davegut.tpLinkTransCamera, line 148

//	===== Sync Communications ===== // library marker davegut.tpLinkTransCamera, line 150
def postSync(cmdBody, baseUrl) { // library marker davegut.tpLinkTransCamera, line 151
	Map respData = [:] // library marker davegut.tpLinkTransCamera, line 152
	Map httpParams = [uri: baseUrl, // library marker davegut.tpLinkTransCamera, line 153
					 body: JsonOutput.toJson(cmdBody), // library marker davegut.tpLinkTransCamera, line 154
					 contentType: "application/json", // library marker davegut.tpLinkTransCamera, line 155
					 requestContentType: "application/json", // library marker davegut.tpLinkTransCamera, line 156
					 timeout: 10, // library marker davegut.tpLinkTransCamera, line 157
					 ignoreSSLIssues: true // library marker davegut.tpLinkTransCamera, line 158
					 ] // library marker davegut.tpLinkTransCamera, line 159
	try { // library marker davegut.tpLinkTransCamera, line 160
		httpPostJson(httpParams) { resp -> // library marker davegut.tpLinkTransCamera, line 161
			if (resp.status == 200) { // library marker davegut.tpLinkTransCamera, line 162
				respData << resp.data // library marker davegut.tpLinkTransCamera, line 163
			} else { // library marker davegut.tpLinkTransCamera, line 164
				respData << [status: resp.status, errorData: resp.properties, // library marker davegut.tpLinkTransCamera, line 165
							 action: "<b>Check IP Address</b>"] // library marker davegut.tpLinkTransCamera, line 166
				logWarn(respData) // library marker davegut.tpLinkTransCamera, line 167
			} // library marker davegut.tpLinkTransCamera, line 168
		} // library marker davegut.tpLinkTransCamera, line 169
	} catch (err) { // library marker davegut.tpLinkTransCamera, line 170
		respData << [status: "httpPostJson error", error: err] // library marker davegut.tpLinkTransCamera, line 171
		logWarn(respData) // library marker davegut.tpLinkTransCamera, line 172
	} // library marker davegut.tpLinkTransCamera, line 173
	return respData // library marker davegut.tpLinkTransCamera, line 174
} // library marker davegut.tpLinkTransCamera, line 175

def getCameraParams(cmdBody, reqData) { // library marker davegut.tpLinkTransCamera, line 177
	byte[] encKey = new JsonSlurper().parseText(lsk) // library marker davegut.tpLinkTransCamera, line 178
	byte[] encIv = new JsonSlurper().parseText(ivb) // library marker davegut.tpLinkTransCamera, line 179
	def cmdStr = JsonOutput.toJson(cmdBody) // library marker davegut.tpLinkTransCamera, line 180
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkTransCamera, line 181
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkTransCamera, line 182
	String cmdData = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.tpLinkTransCamera, line 183
	Integer seqNumber = state.seqNo // library marker davegut.tpLinkTransCamera, line 184
	String initTagHex = parent.encPasswordCam + cnonce // library marker davegut.tpLinkTransCamera, line 185
	String initTag = mdEncode("SHA-256", initTagHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 186
	String tagString = initTag + cmdData + seqNumber // library marker davegut.tpLinkTransCamera, line 187
	String tag =  mdEncode("SHA-256", tagString.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.tpLinkTransCamera, line 188
	Map reqParams = [uri: apiUrl, // library marker davegut.tpLinkTransCamera, line 189
					 body: cmdData, // library marker davegut.tpLinkTransCamera, line 190
					 headers: ["Tapo_tag": tag, Seq: seqNumber], // library marker davegut.tpLinkTransCamera, line 191
					 contentType: "application/json", // library marker davegut.tpLinkTransCamera, line 192
					 requestContentType: "application/json", // library marker davegut.tpLinkTransCamera, line 193
					 timeout: 10, // library marker davegut.tpLinkTransCamera, line 194
					 ignoreSSLIssues: true // library marker davegut.tpLinkTransCamera, line 195
					 ] // library marker davegut.tpLinkTransCamera, line 196
	return reqParams // library marker davegut.tpLinkTransCamera, line 197
} // library marker davegut.tpLinkTransCamera, line 198

def parseCameraData(resp, data) { // library marker davegut.tpLinkTransCamera, line 200
	Map parseData = [Method: "parseCameraData", sourceMethod: data.data] // library marker davegut.tpLinkTransCamera, line 201
	state.seqNo += 1 // library marker davegut.tpLinkTransCamera, line 202
	if (resp.json.error_code == 0) { // library marker davegut.tpLinkTransCamera, line 203
		resp = resp.json // library marker davegut.tpLinkTransCamera, line 204
		try { // library marker davegut.tpLinkTransCamera, line 205
			byte[] encKey = new JsonSlurper().parseText(lsk) // library marker davegut.tpLinkTransCamera, line 206
			byte[] encIv = new JsonSlurper().parseText(ivb) // library marker davegut.tpLinkTransCamera, line 207
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.result.response, // library marker davegut.tpLinkTransCamera, line 208
															 	encKey, encIv)) // library marker davegut.tpLinkTransCamera, line 209
			parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkTransCamera, line 210
		} catch (err) { // library marker davegut.tpLinkTransCamera, line 211
			parseData << [cryptoStatus: "decryptDataError", error: err, dataLength: resp.result.response.length()] // library marker davegut.tpLinkTransCamera, line 212
			logWarn(parseData) // library marker davegut.tpLinkTransCamera, line 213
        } // library marker davegut.tpLinkTransCamera, line 214
	} else { // library marker davegut.tpLinkTransCamera, line 215
		parseData << [cryptoStatus: "rerurnDataErrorCode", resp: resp.json] // library marker davegut.tpLinkTransCamera, line 216
        logWarn(parseData) // library marker davegut.tpLinkTransCamera, line 217
	} // library marker davegut.tpLinkTransCamera, line 218
	return parseData // library marker davegut.tpLinkTransCamera, line 219
} // library marker davegut.tpLinkTransCamera, line 220

// ~~~~~ end include (245) davegut.tpLinkTransCamera ~~~~~

// ~~~~~ start include (253) davegut.Logging ~~~~~
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

// ~~~~~ end include (253) davegut.Logging ~~~~~
