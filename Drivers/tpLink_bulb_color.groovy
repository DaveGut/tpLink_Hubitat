/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

=================================================================================================*/
//	=====	NAMESPACE	============
def nameSpace() { return "davegut" }
//	================================

metadata {
	definition (name: "tpLink_bulb_color", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_bulb_color.groovy")
	{
		capability "Light"
		capability "Switch"
		capability "Switch Level"
		capability "Change Level"
		capability "Color Temperature"
		capability "Color Mode"
		capability "Color Control"
		attribute "commsError", "string"
	}
	preferences {
		commonPreferences()
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
	Map logData = [method: "delayedUpdates"]
	logData << [gradualOnOff: gradualOnOff]
	List requests = [[method: "set_on_off_gradually_info",
					  params: [enable: gradualOnOff]]]
	requests << [method: "get_on_off_gradually_info"]
	
	logData << [nameSync: nameSync]
	def nickname = device.getLabel().bytes.encodeBase64().toString()
	requests << [method: "set_device_info", params: [nickname: nickname]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "delayedUpdates", "parseUpdates")
	logInfo(logData)
	runIn(5, refresh)
}

def setColorTemperature(colorTemp, level = device.currentValue("level"), transTime=null) {
	//	Note: Tapo Devices do not support transition time.  Set preference "Set Bulb to Gradual ON/OFF"
	if (transTime != null) {
		logWarn("setColorTemperature: Dimmer level duration not supported.  Use Preference Set Gradual On/Off.")
	}
	logDebug("setColorTemperature: [level: ${level}, colorTemp: ${colorTemp}]")
	def lowCt = getDataValue("ctLow").toInteger()
	def highCt = getDataValue("ctHigh").toInteger()
	if (colorTemp < lowCt) { colorTemp = lowCt }
	else if (colorTemp > highCt) { colorTemp = highCt }
	List requests = [[
		method: "set_device_info",
		params: [
			device_on: true,
			brightness: level,
			color_temp: colorTemp
		]]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "setColorTemperature", "deviceParse")
}

def setHue(hue){
	logDebug("setHue: ${hue}")
	hue = (3.6 * hue).toInteger()
	logDebug("setHue: ${hue}")
	List requests = [[
		method: "set_device_info",
		params: [
			device_on: true,
			hue: hue,
			color_temp: 0
		]]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "setHue", "deviceParse")
}

def setSaturation(saturation) {
	logDebug("setSatiratopm: ${saturation}")
	List requests = [[
		method: "set_device_info",
		params: [
			device_on: true,
			saturation: saturation,
			color_temp: 0
		]]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "setSaturation", "deviceParse")
}

def setColor(color) {
	def hue = (3.6 * color.hue).toInteger()
	logDebug("setColor: ${color}")
	List requests = [[
		method: "set_device_info",
		params: [
			device_on: true,
			hue: hue,
			saturation: color.saturation,
			brightness: color.level,
			color_temp: 0
		]]]
	requests << [method: "get_device_info"]
	asyncSend(createMultiCmd(requests), "setColor", "deviceParse")
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
			if (devData.color_temp == 0) {
				updateAttr("colorMode", "COLOR")
				def hubHue = (devData.hue / 3.6).toInteger()
				updateAttr("hue", hubHue)
				updateAttr("saturation", devData.saturation)
				updateAttr("color", "[hue: ${hubHue}, saturation: ${devData.saturation}]")
				updateAttr("colorName", colorName = convertHueToGenericColorName(hubHue))
				def rgb = hubitat.helper.ColorUtils.hsvToRGB([hubHue,
															  devData.saturation,
															  devData.brightness])
				updateAttr("RGB", rgb)
				updateAttr("colorTemperature", 0)
			} else {
				updateAttr("colorMode", "CT")
				updateAttr("colorTemperature", devData.color_temp)
				updateAttr("colorName", convertTemperatureToGenericColorName(devData.color_temp.toInteger()))
				updateAttr("color", "[:]")
				updateAttr("RGB", "[]")
			}
		}
	}
	logDebug(logData)
}

//	Library Inclusion




// ~~~~~ start include (23) davegut.lib_tpLink_CapSwitch ~~~~~
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
	if (transTime != null) { // library marker davegut.lib_tpLink_CapSwitch, line 33
		logWarn("setLevel: Dimmer level duration not supported.  Use Preference Set Gradual On/Off.") // library marker davegut.lib_tpLink_CapSwitch, line 34
	} // library marker davegut.lib_tpLink_CapSwitch, line 35
	logDebug("setLevel: [brightness: ${level}]") // library marker davegut.lib_tpLink_CapSwitch, line 36
	List requests = [[ // library marker davegut.lib_tpLink_CapSwitch, line 37
		method: "set_device_info", // library marker davegut.lib_tpLink_CapSwitch, line 38
		params: [ // library marker davegut.lib_tpLink_CapSwitch, line 39
			device_on: true, // library marker davegut.lib_tpLink_CapSwitch, line 40
			brightness: level // library marker davegut.lib_tpLink_CapSwitch, line 41
		]]] // library marker davegut.lib_tpLink_CapSwitch, line 42
	requests << [method: "get_device_info"] // library marker davegut.lib_tpLink_CapSwitch, line 43
	asyncSend(createMultiCmd(requests), "setLevel", "deviceParse") // library marker davegut.lib_tpLink_CapSwitch, line 44
} // library marker davegut.lib_tpLink_CapSwitch, line 45

def startLevelChange(direction) { // library marker davegut.lib_tpLink_CapSwitch, line 47
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]") // library marker davegut.lib_tpLink_CapSwitch, line 48
	if (direction == "up") { levelUp() } // library marker davegut.lib_tpLink_CapSwitch, line 49
	else { levelDown() } // library marker davegut.lib_tpLink_CapSwitch, line 50
} // library marker davegut.lib_tpLink_CapSwitch, line 51

def stopLevelChange() { // library marker davegut.lib_tpLink_CapSwitch, line 53
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]") // library marker davegut.lib_tpLink_CapSwitch, line 54
	unschedule(levelUp) // library marker davegut.lib_tpLink_CapSwitch, line 55
	unschedule(levelDown) // library marker davegut.lib_tpLink_CapSwitch, line 56
} // library marker davegut.lib_tpLink_CapSwitch, line 57

def levelUp() { // library marker davegut.lib_tpLink_CapSwitch, line 59
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapSwitch, line 60
	if (curLevel != 100) { // library marker davegut.lib_tpLink_CapSwitch, line 61
		def newLevel = curLevel + 4 // library marker davegut.lib_tpLink_CapSwitch, line 62
		if (newLevel > 100) { newLevel = 100 } // library marker davegut.lib_tpLink_CapSwitch, line 63
		setLevel(newLevel) // library marker davegut.lib_tpLink_CapSwitch, line 64
		runIn(1, levelUp) // library marker davegut.lib_tpLink_CapSwitch, line 65
	} // library marker davegut.lib_tpLink_CapSwitch, line 66
} // library marker davegut.lib_tpLink_CapSwitch, line 67

def levelDown() { // library marker davegut.lib_tpLink_CapSwitch, line 69
	def curLevel = device.currentValue("level").toInteger() // library marker davegut.lib_tpLink_CapSwitch, line 70
	if (device.currentValue("switch") == "on") { // library marker davegut.lib_tpLink_CapSwitch, line 71
		def newLevel = curLevel - 4 // library marker davegut.lib_tpLink_CapSwitch, line 72
		if (newLevel <= 0) { off() } // library marker davegut.lib_tpLink_CapSwitch, line 73
		else { // library marker davegut.lib_tpLink_CapSwitch, line 74
			setLevel(newLevel) // library marker davegut.lib_tpLink_CapSwitch, line 75
			runIn(1, levelDown) // library marker davegut.lib_tpLink_CapSwitch, line 76
		} // library marker davegut.lib_tpLink_CapSwitch, line 77
	} // library marker davegut.lib_tpLink_CapSwitch, line 78
} // library marker davegut.lib_tpLink_CapSwitch, line 79

// ~~~~~ end include (23) davegut.lib_tpLink_CapSwitch ~~~~~

// ~~~~~ start include (24) davegut.lib_tpLink_common ~~~~~
library ( // library marker davegut.lib_tpLink_common, line 1
	name: "lib_tpLink_common", // library marker davegut.lib_tpLink_common, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_common, line 3
	author: "Compied by Dave Gutheinz", // library marker davegut.lib_tpLink_common, line 4
	description: "Method common to tpLink device DRIVERS", // library marker davegut.lib_tpLink_common, line 5
	category: "utilities", // library marker davegut.lib_tpLink_common, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_common, line 7
) // library marker davegut.lib_tpLink_common, line 8
import org.json.JSONObject // library marker davegut.lib_tpLink_common, line 9
import groovy.json.JsonOutput // library marker davegut.lib_tpLink_common, line 10
import groovy.json.JsonBuilder // library marker davegut.lib_tpLink_common, line 11
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_common, line 12
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.lib_tpLink_common, line 13
import javax.crypto.spec.SecretKeySpec // library marker davegut.lib_tpLink_common, line 14
import javax.crypto.spec.IvParameterSpec // library marker davegut.lib_tpLink_common, line 15
import javax.crypto.Cipher // library marker davegut.lib_tpLink_common, line 16
import java.security.KeyFactory // library marker davegut.lib_tpLink_common, line 17
import java.util.Random // library marker davegut.lib_tpLink_common, line 18
import java.security.MessageDigest // library marker davegut.lib_tpLink_common, line 19

capability "Refresh" // library marker davegut.lib_tpLink_common, line 21
capability "Configuration" // library marker davegut.lib_tpLink_common, line 22

def commonPreferences() { // library marker davegut.lib_tpLink_common, line 24
	input ("nameSync", "enum", title: "Synchronize Names", // library marker davegut.lib_tpLink_common, line 25
		   options: ["none": "Don't synchronize", // library marker davegut.lib_tpLink_common, line 26
					 "device" : "TP-Link device name master", // library marker davegut.lib_tpLink_common, line 27
					 "Hubitat" : "Hubitat label master"], // library marker davegut.lib_tpLink_common, line 28
		   defaultValue: "none") // library marker davegut.lib_tpLink_common, line 29
	input ("pollInterval", "enum", title: "Poll Interval (< 1 min can cause issues)", // library marker davegut.lib_tpLink_common, line 30
		   options: ["5 sec", "10 sec", "30 sec", "1 min", "10 min", "15 min", "30 min"], // library marker davegut.lib_tpLink_common, line 31
		   defaultValue: "30 min") // library marker davegut.lib_tpLink_common, line 32
	input ("rebootDev", "bool", title: "Reboot Device then run Save Preferences", defaultValue: false) // library marker davegut.lib_tpLink_common, line 33

	input ("encKey", "password", title: "Crypto key. Do not edit.") // library marker davegut.lib_tpLink_common, line 35
	input ("encIv", "password", title: "Crypto vector. Do not edit.") // library marker davegut.lib_tpLink_common, line 36
	input ("cookie", "password", title: "Session cookie. Do not edit.") // library marker davegut.lib_tpLink_common, line 37
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 38
		input ("encSig", "password", title: "KLAP signature. Do not edit.") // library marker davegut.lib_tpLink_common, line 39
	} else { // library marker davegut.lib_tpLink_common, line 40
		input ("token", "password", title: "AES token. Do not edit.") // library marker davegut.lib_tpLink_common, line 41
	} // library marker davegut.lib_tpLink_common, line 42
} // library marker davegut.lib_tpLink_common, line 43

def commonUpdated() { // library marker davegut.lib_tpLink_common, line 45
	unschedule() // library marker davegut.lib_tpLink_common, line 46
	Map logData = [:] // library marker davegut.lib_tpLink_common, line 47
	if (rebootDev == true) { // library marker davegut.lib_tpLink_common, line 48
		runIn(1, rebootDevice) // library marker davegut.lib_tpLink_common, line 49
		device.updateSetting("rebootDev",[type:"bool", value: false]) // library marker davegut.lib_tpLink_common, line 50
		pauseExecution(15000) // library marker davegut.lib_tpLink_common, line 51
	} // library marker davegut.lib_tpLink_common, line 52
	updateAttr("commsError", "false") // library marker davegut.lib_tpLink_common, line 53
	state.errorCount = 0 // library marker davegut.lib_tpLink_common, line 54
	state.lastCmd = "" // library marker davegut.lib_tpLink_common, line 55
	logData << [pollInterval: setPollInterval()] // library marker davegut.lib_tpLink_common, line 56
	logData << [loginInterval: setLoginInterval()] // library marker davegut.lib_tpLink_common, line 57
	logData << setLogsOff() // library marker davegut.lib_tpLink_common, line 58
	logData << [deviceLogin: deviceLogin()] // library marker davegut.lib_tpLink_common, line 59
	if (logData.status == "ERROR") { // library marker davegut.lib_tpLink_common, line 60
		logWarn("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 61
	} else { // library marker davegut.lib_tpLink_common, line 62
		logInfo("updated: ${logData}") // library marker davegut.lib_tpLink_common, line 63
	} // library marker davegut.lib_tpLink_common, line 64
	runIn(10, delayedUpdates) // library marker davegut.lib_tpLink_common, line 65
} // library marker davegut.lib_tpLink_common, line 66

def rebootDevice() { // library marker davegut.lib_tpLink_common, line 68
	asyncSend([method: "device_reboot"], "rebootDevice", "rebootParse") // library marker davegut.lib_tpLink_common, line 69
} // library marker davegut.lib_tpLink_common, line 70
def rebootParse(resp, data=null) { // library marker davegut.lib_tpLink_common, line 71
	def respData = parseData(resp).cmdResp // library marker davegut.lib_tpLink_common, line 72
	Map logData = [method: "rebootParse", data: data, respData: respData] // library marker davegut.lib_tpLink_common, line 73
	logInfo(logData) // library marker davegut.lib_tpLink_common, line 74
} // library marker davegut.lib_tpLink_common, line 75

def setPollInterval(pInterval = pollInterval) { // library marker davegut.lib_tpLink_common, line 77
	def method = "commonRefresh" // library marker davegut.lib_tpLink_common, line 78
	if (getDataValue("capability") == "plug_multi") { // library marker davegut.lib_tpLink_common, line 79
		method = "parentRefresh" // library marker davegut.lib_tpLink_common, line 80
	} else if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_common, line 81
		method = "emRefresh" // library marker davegut.lib_tpLink_common, line 82
	} // library marker davegut.lib_tpLink_common, line 83
	if (pInterval.contains("sec")) { // library marker davegut.lib_tpLink_common, line 84
		def interval = pInterval.replace(" sec", "").toInteger() // library marker davegut.lib_tpLink_common, line 85
		def start = Math.round((interval-1) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 86
		schedule("${start}/${interval} * * * * ?", method) // library marker davegut.lib_tpLink_common, line 87
		logInfo("setPollInterval: Polling intervals of less than one minute " + // library marker davegut.lib_tpLink_common, line 88
				"can take high resources and may impact hub performance.") // library marker davegut.lib_tpLink_common, line 89
	} else { // library marker davegut.lib_tpLink_common, line 90
		def interval= pInterval.replace(" min", "").toInteger() // library marker davegut.lib_tpLink_common, line 91
		def start = Math.round(59 * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 92
		schedule("${start} */${interval} * * * ?", method) // library marker davegut.lib_tpLink_common, line 93
	} // library marker davegut.lib_tpLink_common, line 94
	if (getDataValue("capability") == "hub") { // library marker davegut.lib_tpLink_common, line 95
		def sInterval = pInterval // library marker davegut.lib_tpLink_common, line 96
		if (sInterval == pollInterval) { // library marker davegut.lib_tpLink_common, line 97
			sInterval = sensorPollInterval // library marker davegut.lib_tpLink_common, line 98
		} // library marker davegut.lib_tpLink_common, line 99
		setSensorPollInterval(sInterval) // library marker davegut.lib_tpLink_common, line 100
	} // library marker davegut.lib_tpLink_common, line 101
	return pInterval // library marker davegut.lib_tpLink_common, line 102
} // library marker davegut.lib_tpLink_common, line 103

def setLoginInterval() { // library marker davegut.lib_tpLink_common, line 105
	def startS = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 106
	def startM = Math.round((59) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 107
	def startH = Math.round((11) * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 108
	schedule("${startS} ${startM} ${startH}/12 * * ?", "deviceLogin") // library marker davegut.lib_tpLink_common, line 109
	return "12 hrs" // library marker davegut.lib_tpLink_common, line 110
} // library marker davegut.lib_tpLink_common, line 111

def parseUpdates(resp, data= null) { // library marker davegut.lib_tpLink_common, line 113
	Map logData = [method: "parseUpdates"] // library marker davegut.lib_tpLink_common, line 114
	def respData = parseData(resp).cmdResp // library marker davegut.lib_tpLink_common, line 115
	if (respData.error_code == 0) { // library marker davegut.lib_tpLink_common, line 116
		respData.result.responses.each { // library marker davegut.lib_tpLink_common, line 117
			if (it.error_code == 0) { // library marker davegut.lib_tpLink_common, line 118
				switchParse(it) // library marker davegut.lib_tpLink_common, line 119
			} else { // library marker davegut.lib_tpLink_common, line 120
				logData << ["${it.method}": [status: "cmdFailed", data: it]] // library marker davegut.lib_tpLink_common, line 121
			} // library marker davegut.lib_tpLink_common, line 122
		} // library marker davegut.lib_tpLink_common, line 123
	} else { // library marker davegut.lib_tpLink_common, line 124
		logData << [status: "invalidRequest", data: respData] // library marker davegut.lib_tpLink_common, line 125
	} // library marker davegut.lib_tpLink_common, line 126
	logInfo(logData) // library marker davegut.lib_tpLink_common, line 127
} // library marker davegut.lib_tpLink_common, line 128

def switchParse(devResp) { // library marker davegut.lib_tpLink_common, line 130
	Map logData = [method: switchParse] // library marker davegut.lib_tpLink_common, line 131
	def doLog = true // library marker davegut.lib_tpLink_common, line 132
	switch(devResp.method) { // library marker davegut.lib_tpLink_common, line 133
		case "get_device_info": // library marker davegut.lib_tpLink_common, line 134
			if (devResp.result.default_states) { // library marker davegut.lib_tpLink_common, line 135
				def defaultStates = devResp.result.default_states // library marker davegut.lib_tpLink_common, line 136
				def newState = "lastState" // library marker davegut.lib_tpLink_common, line 137
				if (defaultStates.type == "custom"){ // library marker davegut.lib_tpLink_common, line 138
					newState = "off" // library marker davegut.lib_tpLink_common, line 139
					if (defaultStates.state.on == true) { // library marker davegut.lib_tpLink_common, line 140
						newState = "on" // library marker davegut.lib_tpLink_common, line 141
					} // library marker davegut.lib_tpLink_common, line 142
				} // library marker davegut.lib_tpLink_common, line 143
				device.updateSetting("defState", [type: "enum", value: newState]) // library marker davegut.lib_tpLink_common, line 144
				logData << [deviceMethod: devResp.method, defState: newState] // library marker davegut.lib_tpLink_common, line 145
			} // library marker davegut.lib_tpLink_common, line 146

			if (nameSync == "device") { // library marker davegut.lib_tpLink_common, line 148
				byte[] plainBytes = devResp.result.nickname.decodeBase64() // library marker davegut.lib_tpLink_common, line 149
				String label = new String(plainBytes) // library marker davegut.lib_tpLink_common, line 150
				device.setLabel(label) // library marker davegut.lib_tpLink_common, line 151
				logData << [deviceMethod: devResp.method,  // library marker davegut.lib_tpLink_common, line 152
							nickname: devResp.result.nickname, label: label] // library marker davegut.lib_tpLink_common, line 153
			} // library marker davegut.lib_tpLink_common, line 154
			device.updateSetting("nameSync", [type: "enum", value: "none"]) // library marker davegut.lib_tpLink_common, line 155
			break // library marker davegut.lib_tpLink_common, line 156

		case "get_led_info": // library marker davegut.lib_tpLink_common, line 158
			logData << [deviceMethod: devResp.method] // library marker davegut.lib_tpLink_common, line 159
			if (ledRule != devResp.result.led_rule) { // library marker davegut.lib_tpLink_common, line 160
				Map requests = [ // library marker davegut.lib_tpLink_common, line 161
					method: "set_led_info", // library marker davegut.lib_tpLink_common, line 162
					params: [ // library marker davegut.lib_tpLink_common, line 163
						led_rule: ledRule, // library marker davegut.lib_tpLink_common, line 164
						//	Uses mode data from device.  This driver does not update these. // library marker davegut.lib_tpLink_common, line 165
						night_mode: [ // library marker davegut.lib_tpLink_common, line 166
							night_mode_type: devResp.result.night_mode.night_mode_type, // library marker davegut.lib_tpLink_common, line 167
							sunrise_offset: devResp.result.night_mode.sunrise_offset,  // library marker davegut.lib_tpLink_common, line 168
							sunset_offset:devResp.result.night_mode.sunset_offset, // library marker davegut.lib_tpLink_common, line 169
							start_time: devResp.result.night_mode.start_time, // library marker davegut.lib_tpLink_common, line 170
							end_time: devResp.result.night_mode.end_time // library marker davegut.lib_tpLink_common, line 171
						]]] // library marker davegut.lib_tpLink_common, line 172
				asyncSend(requests, "delayedUpdates", "parseUpdates") // library marker davegut.lib_tpLink_common, line 173
				device.updateSetting("ledRule", [type:"enum", value: ledRule]) // library marker davegut.lib_tpLink_common, line 174
				logData << [status: "updatingLedRule"] // library marker davegut.lib_tpLink_common, line 175
			} // library marker davegut.lib_tpLink_common, line 176
			logData << [ledRule: ledRule] // library marker davegut.lib_tpLink_common, line 177
			break // library marker davegut.lib_tpLink_common, line 178

		case "get_auto_off_config": // library marker davegut.lib_tpLink_common, line 180
			device.updateSetting("autoOffTime", [type: "number", value: devResp.result.delay_min]) // library marker davegut.lib_tpLink_common, line 181
			device.updateSetting("autoOffEnable", [type: "bool", value: devResp.result.enable]) // library marker davegut.lib_tpLink_common, line 182
			logData << [deviceMethod: devResp.method, autoOffTime: devResp.result.delay_min, // library marker davegut.lib_tpLink_common, line 183
						autoOffEnable: devResp.result.enable] // library marker davegut.lib_tpLink_common, line 184
			break // library marker davegut.lib_tpLink_common, line 185

		case "get_on_off_gradually_info": // library marker davegut.lib_tpLink_common, line 187
			def newGradualOnOff = devResp.result.enable // library marker davegut.lib_tpLink_common, line 188
			device.updateSetting("gradualOnOff",[type:"bool", value: newGradualOnOff]) // library marker davegut.lib_tpLink_common, line 189
			logData << [deviceMethod: devResp.method, gradualOnOff: newGradualOnOff] // library marker davegut.lib_tpLink_common, line 190
			break // library marker davegut.lib_tpLink_common, line 191

		case "get_protection_power": // library marker davegut.lib_tpLink_common, line 193
			def protectPower = devResp.result.protection_power // library marker davegut.lib_tpLink_common, line 194
			def enabled = devResp.result.enabled // library marker davegut.lib_tpLink_common, line 195
			device.updateSetting("pwrProtectWatts", [type: "number",  // library marker davegut.lib_tpLink_common, line 196
													 value: protectPower]) // library marker davegut.lib_tpLink_common, line 197
			device.updateSetting("powerProtect", [type: "bool", value: enabled]) // library marker davegut.lib_tpLink_common, line 198
			logData << [powerProtect: enabled, pwrProtectWatts: protectPower] // library marker davegut.lib_tpLink_common, line 199
			break // library marker davegut.lib_tpLink_common, line 200

		case "get_alarm_configure": // library marker davegut.lib_tpLink_common, line 202
			updateAttr("alarmConfig", devResp.result) // library marker davegut.lib_tpLink_common, line 203
			logData << [alarmConfig: devResp.result] // library marker davegut.lib_tpLink_common, line 204
			break // library marker davegut.lib_tpLink_common, line 205

		case "set_led_info": // library marker davegut.lib_tpLink_common, line 207
		case "set_device_info": // library marker davegut.lib_tpLink_common, line 208
		case "set_auto_off_config": // library marker davegut.lib_tpLink_common, line 209
		case "set_on_off_gradually_info": // library marker davegut.lib_tpLink_common, line 210
			doLog = false // library marker davegut.lib_tpLink_common, line 211
			break // library marker davegut.lib_tpLink_common, line 212
		default: // library marker davegut.lib_tpLink_common, line 213
			logData << [status: "unhandled", devResp: devResp] // library marker davegut.lib_tpLink_common, line 214
	} // library marker davegut.lib_tpLink_common, line 215

	if (doLog) { logInfo(logData) } // library marker davegut.lib_tpLink_common, line 217
} // library marker davegut.lib_tpLink_common, line 218

//	===== Capability Refresh ===== // library marker davegut.lib_tpLink_common, line 220
def refresh() { // library marker davegut.lib_tpLink_common, line 221
	if (getDataValue("capability") == "plug_multi") { // library marker davegut.lib_tpLink_common, line 222
		parentRefresh() // library marker davegut.lib_tpLink_common, line 223
	} else if (getDataValue("capability") == "plug_em") { // library marker davegut.lib_tpLink_common, line 224
		emRefresh() // library marker davegut.lib_tpLink_common, line 225
	} else { // library marker davegut.lib_tpLink_common, line 226
		commonRefresh() // library marker davegut.lib_tpLink_common, line 227
	} // library marker davegut.lib_tpLink_common, line 228
} // library marker davegut.lib_tpLink_common, line 229

def commonRefresh() { // library marker davegut.lib_tpLink_common, line 231
	asyncSend([method: "get_device_info"], "commonRefresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 232
} // library marker davegut.lib_tpLink_common, line 233

def emRefresh() { // library marker davegut.lib_tpLink_common, line 235
	List requests = [[method: "get_device_info"]] // library marker davegut.lib_tpLink_common, line 236
	requests << [method:"get_energy_usage"] // library marker davegut.lib_tpLink_common, line 237
	asyncSend(createMultiCmd(requests), "emRefresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 238
} // library marker davegut.lib_tpLink_common, line 239

def parentRefresh() { // library marker davegut.lib_tpLink_common, line 241
	List requests = [[method: "get_device_info"]] // library marker davegut.lib_tpLink_common, line 242
	requests << [method:"get_child_device_list"] // library marker davegut.lib_tpLink_common, line 243
	asyncSend(createMultiCmd(requests), "parentRefresh", "deviceParse") // library marker davegut.lib_tpLink_common, line 244
} // library marker davegut.lib_tpLink_common, line 245

//	===== Capability Configuration ===== // library marker davegut.lib_tpLink_common, line 247
def configure() { // library marker davegut.lib_tpLink_common, line 248
	Map logData = [method: "configure", updateData: parent.tpLinkCheckForDevices(5)] // library marker davegut.lib_tpLink_common, line 249
	device.updateSetting("rebootDev",[type:"bool", value: true]) // library marker davegut.lib_tpLink_common, line 250
	updated() // library marker davegut.lib_tpLink_common, line 251
	logData << [action: "updated"] // library marker davegut.lib_tpLink_common, line 252
	logInfo(logData) // library marker davegut.lib_tpLink_common, line 253
} // library marker davegut.lib_tpLink_common, line 254

def updateAttr(attr, value) { // library marker davegut.lib_tpLink_common, line 256
	if (device.currentValue(attr) != value) { // library marker davegut.lib_tpLink_common, line 257
		sendEvent(name: attr, value: value) // library marker davegut.lib_tpLink_common, line 258
	} // library marker davegut.lib_tpLink_common, line 259
} // library marker davegut.lib_tpLink_common, line 260

//	===== Login ===== // library marker davegut.lib_tpLink_common, line 262
def deviceLogin() { // library marker davegut.lib_tpLink_common, line 263
	Map logData = [protocol: getDataValue("protocol")] // library marker davegut.lib_tpLink_common, line 264
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 265
		def uri = "${getDataValue("baseUrl")}/handshake1" // library marker davegut.lib_tpLink_common, line 266
		byte[] localSeed = new byte[16] // library marker davegut.lib_tpLink_common, line 267
		new Random().nextBytes(localSeed) // library marker davegut.lib_tpLink_common, line 268
		def body = localSeed.encodeBase64().toString() // library marker davegut.lib_tpLink_common, line 269
		Map staticData = [localSeed: localSeed] // library marker davegut.lib_tpLink_common, line 270
		asyncPost(uri, localSeed, "application/octet-stream",  // library marker davegut.lib_tpLink_common, line 271
				  "parseKlapHandshake", null, staticData) // library marker davegut.lib_tpLink_common, line 272
	} else { // library marker davegut.lib_tpLink_common, line 273
		def rsaKeys = getRsaKeys() // library marker davegut.lib_tpLink_common, line 274
		Map reqData = [rsaKeyNo: rsaKeys.keyNo] // library marker davegut.lib_tpLink_common, line 275
		def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKeys.public}-----END PUBLIC KEY-----\n" // library marker davegut.lib_tpLink_common, line 276
		Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.lib_tpLink_common, line 277
		String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_common, line 278
		asyncPost(getDataValue("baseUrl"), cmdBodyJson, "application/json",  // library marker davegut.lib_tpLink_common, line 279
				  "parseAesHandshake", null, reqData) // library marker davegut.lib_tpLink_common, line 280
	} // library marker davegut.lib_tpLink_common, line 281
	return logData // library marker davegut.lib_tpLink_common, line 282
} // library marker davegut.lib_tpLink_common, line 283

//	===== AES Handshake and Login ===== // library marker davegut.lib_tpLink_common, line 285
def parseAesHandshake(resp, data){ // library marker davegut.lib_tpLink_common, line 286
	Map logData = [method: "parseAesHandshake"] // library marker davegut.lib_tpLink_common, line 287
	def respStatus = "ERROR" // library marker davegut.lib_tpLink_common, line 288
	if (resp.status == 200 && resp.data != null) { // library marker davegut.lib_tpLink_common, line 289
		Map cmdResp =  new JsonSlurper().parseText(resp.data) // library marker davegut.lib_tpLink_common, line 290
		Map rsaKeys = keyData().find { it.keyNo == data.data.rsaKeyNo } // library marker davegut.lib_tpLink_common, line 291
		String deviceKey = cmdResp.result.key // library marker davegut.lib_tpLink_common, line 292
		def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.lib_tpLink_common, line 293
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_common, line 294
		logData << [cookie: cookie] // library marker davegut.lib_tpLink_common, line 295
		device.updateSetting("cookie",[type:"password", value: cookie]) // library marker davegut.lib_tpLink_common, line 296
		Map aesArray = readAesDeviceKey(deviceKey, rsaKeys.private) // library marker davegut.lib_tpLink_common, line 297
		logData << [aesArray: aesArray] // library marker davegut.lib_tpLink_common, line 298
		if (aesArraystatus == "ERROR") { // library marker davegut.lib_tpLink_common, line 299
			logData << [check: "privateKey"] // library marker davegut.lib_tpLink_common, line 300
			logWarn(logData) // library marker davegut.lib_tpLink_common, line 301
		} else { // library marker davegut.lib_tpLink_common, line 302
			respStatus = "OK" // library marker davegut.lib_tpLink_common, line 303
			byte[] encKey = aesArray.cryptoArray[0..15] // library marker davegut.lib_tpLink_common, line 304
			device.updateSetting("encKey",[type:"password", value: encKey]) // library marker davegut.lib_tpLink_common, line 305
			byte[] encIv = aesArray.cryptoArray[16..31] // library marker davegut.lib_tpLink_common, line 306
			device.updateSetting("encIv",[type:"password", value: encIv]) // library marker davegut.lib_tpLink_common, line 307
			logData << [encKey: encKey, encIv: encIv] // library marker davegut.lib_tpLink_common, line 308

			Map cmdBody = [method: "login_device", // library marker davegut.lib_tpLink_common, line 310
						   params: [password: parent.encPassword, // library marker davegut.lib_tpLink_common, line 311
									username: parent.encUsername], // library marker davegut.lib_tpLink_common, line 312
						   requestTimeMils: 0] // library marker davegut.lib_tpLink_common, line 313
			def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_common, line 314
			def encrString = aesEncrypt(cmdStr, encKey, encIv) // library marker davegut.lib_tpLink_common, line 315
			Map reqBody = [method: "securePassthrough",  // library marker davegut.lib_tpLink_common, line 316
						   params: [request: encrString]] // library marker davegut.lib_tpLink_common, line 317
			asyncPost(getDataValue("baseUrl"), reqBody, "application/json",  // library marker davegut.lib_tpLink_common, line 318
					  "parseAesLogin", cookie, null) // library marker davegut.lib_tpLink_common, line 319
		} // library marker davegut.lib_tpLink_common, line 320
	} else { // library marker davegut.lib_tpLink_common, line 321
		logData << [errorData: respData] // library marker davegut.lib_tpLink_common, line 322
		logWarn(logData) // library marker davegut.lib_tpLink_common, line 323
	} // library marker davegut.lib_tpLink_common, line 324
	logData << [respStatus: respStatus] // library marker davegut.lib_tpLink_common, line 325
	logDebug(logData) // library marker davegut.lib_tpLink_common, line 326
} // library marker davegut.lib_tpLink_common, line 327

def readAesDeviceKey(deviceKey, privateKey) { // library marker davegut.lib_tpLink_common, line 329
	def status = "ERROR" // library marker davegut.lib_tpLink_common, line 330
	Map logData = [:] // library marker davegut.lib_tpLink_common, line 331
	try { // library marker davegut.lib_tpLink_common, line 332
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.lib_tpLink_common, line 333
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.lib_tpLink_common, line 334
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.lib_tpLink_common, line 335
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.lib_tpLink_common, line 336
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.lib_tpLink_common, line 337
		logData << [cryptoArray: cryptoArray] // library marker davegut.lib_tpLink_common, line 338
		status = "OK" // library marker davegut.lib_tpLink_common, line 339
	} catch (err) { // library marker davegut.lib_tpLink_common, line 340
		logData << [errorData: err] // library marker davegut.lib_tpLink_common, line 341
	} // library marker davegut.lib_tpLink_common, line 342
	logData << [keyStatus: status] // library marker davegut.lib_tpLink_common, line 343
	return logData // library marker davegut.lib_tpLink_common, line 344
} // library marker davegut.lib_tpLink_common, line 345

def parseAesLogin(resp, data) { // library marker davegut.lib_tpLink_common, line 347
	Map logData = [method: "parseAesLogin"] // library marker davegut.lib_tpLink_common, line 348
	if (resp.status == 200) { // library marker davegut.lib_tpLink_common, line 349
		if (resp.json.error_code == 0) { // library marker davegut.lib_tpLink_common, line 350
			try { // library marker davegut.lib_tpLink_common, line 351
				byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_common, line 352
				byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_common, line 353
				def clearResp = aesDecrypt(resp.json.result.response, encKey, encIv) // library marker davegut.lib_tpLink_common, line 354
				Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_common, line 355
				if (cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_common, line 356
					def token = cmdResp.result.token // library marker davegut.lib_tpLink_common, line 357
					logData << [respStatus: "OK", token: token] // library marker davegut.lib_tpLink_common, line 358
					device.updateSetting("token",[type:"password", value: token]) // library marker davegut.lib_tpLink_common, line 359
				} else { // library marker davegut.lib_tpLink_common, line 360
					logData << [respStatus: "ERROR", error_code: cmdResp.error_code, // library marker davegut.lib_tpLink_common, line 361
								  check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.lib_tpLink_common, line 362
					logWarn(logData) // library marker davegut.lib_tpLink_common, line 363
				} // library marker davegut.lib_tpLink_common, line 364
			} catch (err) { // library marker davegut.lib_tpLink_common, line 365
				logData << [respStatus: "ERROR", error: err] // library marker davegut.lib_tpLink_common, line 366
				logWarn(logData) // library marker davegut.lib_tpLink_common, line 367
			} // library marker davegut.lib_tpLink_common, line 368
		} else { // library marker davegut.lib_tpLink_common, line 369
			logData << [respStatus: "ERROR", data: respData.data] // library marker davegut.lib_tpLink_common, line 370
			logWarn(logData) // library marker davegut.lib_tpLink_common, line 371
		} // library marker davegut.lib_tpLink_common, line 372
	} else { // library marker davegut.lib_tpLink_common, line 373
		logData << [respStatus: "ERROR", data: respData] // library marker davegut.lib_tpLink_common, line 374
	} // library marker davegut.lib_tpLink_common, line 375
	logDebug(logData) // library marker davegut.lib_tpLink_common, line 376
} // library marker davegut.lib_tpLink_common, line 377

//	===== KLAP Handshake and Login ===== // library marker davegut.lib_tpLink_common, line 379
def parseKlapHandshake(resp, data=null) { // library marker davegut.lib_tpLink_common, line 380
	Map logData = [method: "parseKlapHandshake"] // library marker davegut.lib_tpLink_common, line 381
	def validated = false // library marker davegut.lib_tpLink_common, line 382
	if (resp.status == 200 && resp.data != null) { // library marker davegut.lib_tpLink_common, line 383
		def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.lib_tpLink_common, line 384
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_common, line 385
		//	Validate data // library marker davegut.lib_tpLink_common, line 386
		byte[] localSeed = data.data.localSeed // library marker davegut.lib_tpLink_common, line 387
		byte[] seedData = resp.data.decodeBase64() // library marker davegut.lib_tpLink_common, line 388
		byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.lib_tpLink_common, line 389
		byte[] serverHash = seedData[16 .. 47] // library marker davegut.lib_tpLink_common, line 390
		byte[] localHash = parent.localHash.decodeBase64() // library marker davegut.lib_tpLink_common, line 391
		byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_common, line 392
		byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.lib_tpLink_common, line 393
		if (localAuthHash == serverHash) { // library marker davegut.lib_tpLink_common, line 394
			validated = true // library marker davegut.lib_tpLink_common, line 395
			logData << [sessionData: createKlapSessionData(localSeed, remoteSeed, // library marker davegut.lib_tpLink_common, line 396
														   localHash, cookie)] // library marker davegut.lib_tpLink_common, line 397
			def uri = "${getDataValue("baseUrl")}/handshake2" // library marker davegut.lib_tpLink_common, line 398
			byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.lib_tpLink_common, line 399
			byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.lib_tpLink_common, line 400

			asyncPost(uri, body, "application/octet-stream", // library marker davegut.lib_tpLink_common, line 402
					  "parseKlapLogin", cookie, null) // library marker davegut.lib_tpLink_common, line 403
		} // library marker davegut.lib_tpLink_common, line 404
	} else { // library marker davegut.lib_tpLink_common, line 405
		logData << [status: "ERROR", resp: resp, data: data] // library marker davegut.lib_tpLink_common, line 406
	} // library marker davegut.lib_tpLink_common, line 407
	logData << [validated: validated] // library marker davegut.lib_tpLink_common, line 408
	logDebug(logData) // library marker davegut.lib_tpLink_common, line 409
} // library marker davegut.lib_tpLink_common, line 410

def createKlapSessionData(localSeed, remoteSeed, localHash, cookie) { // library marker davegut.lib_tpLink_common, line 412
	Map sessionData = [:] // library marker davegut.lib_tpLink_common, line 413
	//	seqNo and encIv // library marker davegut.lib_tpLink_common, line 414
	byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_common, line 415
	byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_common, line 416
	byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.lib_tpLink_common, line 417
	int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.lib_tpLink_common, line 418
	state.seqNo = seqNo // library marker davegut.lib_tpLink_common, line 419
	sessionData << [seqNo: seqNo] // library marker davegut.lib_tpLink_common, line 420
	device.updateSetting("encIv",[type:"password", value: fullIv[0..11]]) // library marker davegut.lib_tpLink_common, line 421
	device.updateSetting("cookie",[type:"password", value: cookie]) // library marker davegut.lib_tpLink_common, line 422
	sessionData << [encIv: fullIv[0..11], cookie: cookie] // library marker davegut.lib_tpLink_common, line 423
	//	KEY // library marker davegut.lib_tpLink_common, line 424
	payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_common, line 425
	byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.lib_tpLink_common, line 426
	device.updateSetting("encKey",[type:"password", value: encKey]) // library marker davegut.lib_tpLink_common, line 427
	sessionData << [encKey: encKey] // library marker davegut.lib_tpLink_common, line 428
	//	SIG // library marker davegut.lib_tpLink_common, line 429
	payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_common, line 430
	byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.lib_tpLink_common, line 431
	device.updateSetting("encSig",[type:"password", value: encSig]) // library marker davegut.lib_tpLink_common, line 432
	sessionData << [encSig: encSig] // library marker davegut.lib_tpLink_common, line 433
	return sessionData // library marker davegut.lib_tpLink_common, line 434
} // library marker davegut.lib_tpLink_common, line 435

def parseKlapLogin(resp, data) { // library marker davegut.lib_tpLink_common, line 437
	Map logData = [method: "parseKlapLogin"] // library marker davegut.lib_tpLink_common, line 438
	def loginSuccess = false // library marker davegut.lib_tpLink_common, line 439
	if (resp.status == 200) { // library marker davegut.lib_tpLink_common, line 440
		loginSuccess = true  // library marker davegut.lib_tpLink_common, line 441
	} else { // library marker davegut.lib_tpLink_common, line 442
		LogData << [errorData: resp.properties] // library marker davegut.lib_tpLink_common, line 443
	} // library marker davegut.lib_tpLink_common, line 444
	logData << [loginSuccess: loginSuccess] // library marker davegut.lib_tpLink_common, line 445
	logDebug(logData) // library marker davegut.lib_tpLink_common, line 446
} // library marker davegut.lib_tpLink_common, line 447

//	===== Protocol specific encrytion/decryption ===== // library marker davegut.lib_tpLink_common, line 449
def klapEncrypt(byte[] request, encKey, encIv, encSig) { // library marker davegut.lib_tpLink_common, line 450
	int seqNo = state.seqNo + 1 // library marker davegut.lib_tpLink_common, line 451
	state.seqNo = seqNo // library marker davegut.lib_tpLink_common, line 452
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.lib_tpLink_common, line 453
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.lib_tpLink_common, line 454

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_common, line 456
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_common, line 457
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_common, line 458
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_common, line 459
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.lib_tpLink_common, line 460

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.lib_tpLink_common, line 462
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_common, line 463
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.lib_tpLink_common, line 464
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.lib_tpLink_common, line 465
} // library marker davegut.lib_tpLink_common, line 466

def aesEncrypt(request, encKey, encIv) { // library marker davegut.lib_tpLink_common, line 468
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_common, line 469
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_common, line 470
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_common, line 471
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_common, line 472
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.lib_tpLink_common, line 473
	return result.replace("\r\n","") // library marker davegut.lib_tpLink_common, line 474
} // library marker davegut.lib_tpLink_common, line 475

def klapDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_common, line 477
	byte[] encSeq = integerToByteArray(state.seqNo) // library marker davegut.lib_tpLink_common, line 478
	byte[] ivEnc = [encIv, encSeq].flatten() // library marker davegut.lib_tpLink_common, line 479

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_common, line 481
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_common, line 482
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_common, line 483
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_common, line 484
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.lib_tpLink_common, line 485
	return new String(byteResponse, "UTF-8") // library marker davegut.lib_tpLink_common, line 486
} // library marker davegut.lib_tpLink_common, line 487

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_common, line 489
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.lib_tpLink_common, line 490
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_common, line 491
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_common, line 492
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_common, line 493
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_common, line 494
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.lib_tpLink_common, line 495
	return result // library marker davegut.lib_tpLink_common, line 496
} // library marker davegut.lib_tpLink_common, line 497

//	===== RSA Key Methods ===== // library marker davegut.lib_tpLink_common, line 499
def getRsaKeys() { // library marker davegut.lib_tpLink_common, line 500
	def keyNo = Math.round(5 * Math.random()).toInteger() // library marker davegut.lib_tpLink_common, line 501
	def keyData = keyData() // library marker davegut.lib_tpLink_common, line 502
	def RSAKeys = keyData.find { it.keyNo == keyNo } // library marker davegut.lib_tpLink_common, line 503
	return RSAKeys // library marker davegut.lib_tpLink_common, line 504
} // library marker davegut.lib_tpLink_common, line 505

def keyData() { // library marker davegut.lib_tpLink_common, line 507
	//	Keys used for discovery. // library marker davegut.lib_tpLink_common, line 508
	return [ // library marker davegut.lib_tpLink_common, line 509
		[ // library marker davegut.lib_tpLink_common, line 510
			keyNo: 0, // library marker davegut.lib_tpLink_common, line 511
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.lib_tpLink_common, line 512
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw" // library marker davegut.lib_tpLink_common, line 513
		],[ // library marker davegut.lib_tpLink_common, line 514
			keyNo: 1, // library marker davegut.lib_tpLink_common, line 515
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCshy+qBKbJNefcyJUZ/3i+3KyLji6XaWEWvebUCC2r9/0jE6hc89AufO41a13E3gJ2es732vaxwZ1BZKLy468NnL+tg6vlQXaPkDcdunQwjxbTLNL/yzDZs9HRju2lJnupcksdJWBZmjtztMWQkzBrQVeSKzSTrKYK0s24EEXmtQIDAQAB", // library marker davegut.lib_tpLink_common, line 516
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKyHL6oEpsk159zIlRn/eL7crIuOLpdpYRa95tQILav3/SMTqFzz0C587jVrXcTeAnZ6zvfa9rHBnUFkovLjrw2cv62Dq+VBdo+QNx26dDCPFtMs0v/LMNmz0dGO7aUme6lySx0lYFmaO3O0xZCTMGtBV5IrNJOspgrSzbgQRea1AgMBAAECgYBSeiX9H1AkbJK1Z2ZwEUNF6vTJmmUHmScC2jHZNzeuOFVZSXJ5TU0+jBbMjtE65e9DeJ4suw6oF6j3tAZ6GwJ5tHoIy+qHRV6AjA8GEXjhSwwVCyP8jXYZ7UZyHzjLQAK+L0PvwJY1lAtns/Xmk5GH+zpNnhEmKSZAw23f7wpj2QJBANVPQGYT7TsMTDEEl2jq/ZgOX5Djf2VnKpPZYZGsUmg1hMwcpN/4XQ7XOaclR5TO/CJBJl3UCUEVjdrR1zdD8g8CQQDPDoa5Y5UfhLz4Ja2/gs2UKwO4fkTqqR6Ad8fQlaUZ55HINHWFd8FeERBFgNJzszrzd9BBJ7NnZM5nf2OPqU77AkBLuQuScSZ5HL97czbQvwLxVMDmLWyPMdVykOvLC9JhPgZ7cvuwqnlWiF7mEBzeHbBx9JDLJDd4zE8ETBPLgapPAkAHhCR52FaSdVQSwfNjr1DdHw6chODlj8wOp8p2FOiQXyqYlObrOGSpkH8BtuJs1sW+DsxdgR5vE2a2tRYdIe0/AkEAoQ5MzLcETQrmabdVCyB9pQAiHe4yY9e1w7cimsLJOrH7LMM0hqvBqFOIbSPrZyTp7Ie8awn4nTKoZQtvBfwzHw==" // library marker davegut.lib_tpLink_common, line 517
		],[ // library marker davegut.lib_tpLink_common, line 518
			keyNo: 2, // library marker davegut.lib_tpLink_common, line 519
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBeqRy4zAOs63Sc5yc0DtlFXG1stmdD6sEfUiGjlsy0S8aS8X+Qcjcu5AK3uBBrkVNIa8djXht1bd+pUof5/txzWIMJw9SNtNYqzSdeO7cCtRLzuQnQWP7Am64OBvYkXn2sUqoaqDE50LbSQWbuvZw0Vi9QihfBYGQdlrqjCPUsQIDAQAB", // library marker davegut.lib_tpLink_common, line 520
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIF6pHLjMA6zrdJznJzQO2UVcbWy2Z0PqwR9SIaOWzLRLxpLxf5ByNy7kAre4EGuRU0hrx2NeG3Vt36lSh/n+3HNYgwnD1I201irNJ147twK1EvO5CdBY/sCbrg4G9iRefaxSqhqoMTnQttJBZu69nDRWL1CKF8FgZB2WuqMI9SxAgMBAAECgYBBi2wkHI3/Y0Xi+1OUrnTivvBJIri2oW/ZXfKQ6w+PsgU+Mo2QII0l8G0Ck8DCfw3l9d9H/o2wTDgPjGzxqeXHAbxET1dS0QBTjR1zLZlFyfAs7WO8tDKmHVroUgqRkJgoQNQlBSe1E3e7pTgSKElzLuALkRS6p1jhzT2wu9U04QJBAOFr/G36PbQ6NmDYtVyEEr3vWn46JHeZISdJOsordR7Wzbt6xk6/zUDHq0OGM9rYrpBy7PNrbc0JuQrhfbIyaHMCQQCTCvETjXCMkwyUrQT6TpxVzKEVRf1rCitnNQCh1TLnDKcCEAnqZT2RRS3yNXTWFoJrtuEHMGmwUrtog9+ZJBlLAkEA2qxdkPY621XJIIO404mPgM7rMx4F+DsE7U5diHdFw2fO5brBGu13GAtZuUQ7k2W1WY0TDUO+nTN8XPDHdZDuvwJABu7TIwreLaKZS0FFJNAkCt+VEL22Dx/xn/Idz4OP3Nj53t0Guqh/WKQcYHkowxdYmt+KiJ49vXSJJYpiNoQ/NQJAM1HCl8hBznLZLQlxrCTdMvUimG3kJmA0bUNVncgUBq7ptqjk7lp5iNrle5aml99foYnzZeEUW6jrCC7Lj9tg+w==" // library marker davegut.lib_tpLink_common, line 521
		],[ // library marker davegut.lib_tpLink_common, line 522
			keyNo: 3, // library marker davegut.lib_tpLink_common, line 523
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFYaoMvv5kBxUUbp4PQyd7RoZlPompsupXP2La0qGGxacF98/88W4KNUqLbF4X5BPqxoEA+VeZy75qqyfuYbGQ4fxT6usE/LnzW8zDY/PjhVBht8FBRyAUsoYAt3Ip6sDyjd9YzRzUL1Q/OxCgxz5CNETYxcNr7zfMshBHDmZXMQIDAQAB", // library marker davegut.lib_tpLink_common, line 524
			private: "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIVhqgy+/mQHFRRung9DJ3tGhmU+iamy6lc/YtrSoYbFpwX3z/zxbgo1SotsXhfkE+rGgQD5V5nLvmqrJ+5hsZDh/FPq6wT8ufNbzMNj8+OFUGG3wUFHIBSyhgC3cinqwPKN31jNHNQvVD87EKDHPkI0RNjFw2vvN8yyEEcOZlcxAgMBAAECgYA3NxjoMeCpk+z8ClbQRqJ/e9CC9QKUB4bPG2RW5b8MRaJA7DdjpKZC/5CeavwAs+Ay3n3k41OKTTfEfJoJKtQQZnCrqnZfq9IVZI26xfYo0cgSYbi8wCie6nqIBdu9k54nqhePPshi22VcFuOh97xxPvY7kiUaRbbKqxn9PFwrYQJBAMsO3uOnYSJxN/FuxksKLqhtNei2GUC/0l7uIE8rbRdtN3QOpcC5suj7id03/IMn2Ks+Vsrmi0lV4VV/c8xyo9UCQQCoKDlObjbYeYYdW7/NvI6cEntgHygENi7b6WFk+dbRhJQgrFH8Z/Idj9a2E3BkfLCTUM1Z/Z3e7D0iqPDKBn/tAkBAHI3bKvnMOhsDq4oIH0rj+rdOplAK1YXCW0TwOjHTd7ROfGFxHDCUxvacVhTwBCCw0JnuriPEH81phTg2kOuRAkAEPR9UrsqLImUTEGEBWqNto7mgbqifko4T1QozdWjI10K0oCNg7W3Y+Os8o7jNj6cTz5GdlxsHp4TS/tczAH7xAkBY6KPIlF1FfiyJAnBC8+jJr2h4TSPQD7sbJJmYw7mvR+f1T4tsWY0aGux69hVm8BoaLStBVPdkaENBMdP+a07u" // library marker davegut.lib_tpLink_common, line 525
		],[ // library marker davegut.lib_tpLink_common, line 526
			keyNo: 4, // library marker davegut.lib_tpLink_common, line 527
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClF0yuCpo3r1ZpYlGcyI5wy5nnvZdOZmxqz5U2rklt2b8+9uWhmsGdpbTv5+qJXlZmvUKbpoaPxpJluBFDJH2GSpq3I0whh0gNq9Arzpp/TDYaZLb6iIqDMF6wm8yjGOtcSkB7qLQWkXpEN9T2NsEzlfTc+GTKc07QXHnzxoLmwQIDAQAB", // library marker davegut.lib_tpLink_common, line 528
			private: "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKUXTK4KmjevVmliUZzIjnDLmee9l05mbGrPlTauSW3Zvz725aGawZ2ltO/n6oleVma9Qpumho/GkmW4EUMkfYZKmrcjTCGHSA2r0CvOmn9MNhpktvqIioMwXrCbzKMY61xKQHuotBaRekQ31PY2wTOV9Nz4ZMpzTtBcefPGgubBAgMBAAECgYB4wCz+05RvDFk45YfqFCtTRyg//0UvO+0qxsBN6Xad2XlvlWjqJeZd53kLTGcYqJ6rsNyKOmgLu2MS8Wn24TbJmPUAwZU+9cvSPxxQ5k6bwjg1RifieIcbTPC5wHDqVy0/Ur7dt+JVMOHFseR/pElDw471LCdwWSuFHAKuiHsaUQJBANHiPdSU3s1bbJYTLaS1tW0UXo7aqgeXuJgqZ2sKsoIEheEAROJ5rW/f2KrFVtvg0ITSM8mgXNlhNBS5OE4nSD0CQQDJXYJxKvdodeRoj+RGTCZGZanAE1naUzSdfcNWx2IMnYUD/3/2eB7ZIyQPBG5fWjc3bGOJKI+gy/14bCwXU7zVAkAdnsE9HBlpf+qOL3y0jxRgpYxGuuNeGPJrPyjDOYpBwSOnwmL2V1e7vyqTxy/f7hVfeU7nuKMB5q7z8cPZe7+9AkEAl7A6aDe+wlE069OhWZdZqeRBmLC7Gi1d0FoBwahW4zvyDM32vltEmbvQGQP0hR33xGeBH7yPXcjtOz75g+UPtQJBAL4gknJ/p+yQm9RJB0oq/g+HriErpIMHwrhNoRY1aOBMJVl4ari1Ch2RQNL9KQW7yrFDv7XiP3z5NwNDKsp/QeU=" // library marker davegut.lib_tpLink_common, line 529
		],[ // library marker davegut.lib_tpLink_common, line 530
			keyNo: 5, // library marker davegut.lib_tpLink_common, line 531
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChN8Xc+gsSuhcLVM1W1E+e1o+celvKlOmuV6sJEkJecknKFujx9+T4xvyapzyePpTBn0lA9EYbaF7UDYBsDgqSwgt0El3gV+49O56nt1ELbLUJtkYEQPK+6Pu8665UG17leCiaMiFQyoZhD80PXhpjehqDu2900uU/4DzKZ/eywwIDAQAB", // library marker davegut.lib_tpLink_common, line 532
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKE3xdz6CxK6FwtUzVbUT57Wj5x6W8qU6a5XqwkSQl5yScoW6PH35PjG/JqnPJ4+lMGfSUD0RhtoXtQNgGwOCpLCC3QSXeBX7j07nqe3UQtstQm2RgRA8r7o+7zrrlQbXuV4KJoyIVDKhmEPzQ9eGmN6GoO7b3TS5T/gPMpn97LDAgMBAAECgYAy+uQCwL8HqPjoiGR2dKTI4aiAHuEv6m8KxoY7VB7QputWkHARNAaf9KykawXsNHXt1GThuV0CBbsW6z4U7UvCJEZEpv7qJiGX8UWgEs1ISatqXmiIMVosIJJvoFw/rAoScadCYyicskjwDFBVNU53EAUD3WzwEq+dRYDn52lqQQJBAMu30FEReAHTAKE/hvjAeBUyWjg7E4/lnYvb/i9Wuc+MTH0q3JxFGGMb3n6APT9+kbGE0rinM/GEXtpny+5y3asCQQDKl7eNq0NdIEBGAdKerX4O+nVDZ7PXz1kQ2ca0r1tXtY/9sBDDoKHP2fQAH/xlOLIhLaH1rabSEJYNUM0ohHdJAkBYZqhwNWtlJ0ITtvSEB0lUsWfzFLe1bseCBHH16uVwygn7GtlmupkNkO9o548seWkRpnimhnAE8xMSJY6aJ6BHAkEAuSFLKrqGJGOEWHTx8u63cxiMb7wkK+HekfdwDUzxO4U+v6RUrW/sbfPNdQ/FpPnaTVdV2RuGhg+CD0j3MT9bgQJARH86hfxp1bkyc7f1iJQT8sofdqqVz5grCV5XeGY77BNmCvTOGLfL5pOJdgALuOoP4t3e94nRYdlW6LqIVugRBQ==" // library marker davegut.lib_tpLink_common, line 533
		] // library marker davegut.lib_tpLink_common, line 534
	] // library marker davegut.lib_tpLink_common, line 535
} // library marker davegut.lib_tpLink_common, line 536

//	===== Encoding Methods ===== // library marker davegut.lib_tpLink_common, line 538
def mdEncode(hashMethod, byte[] data) { // library marker davegut.lib_tpLink_common, line 539
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.lib_tpLink_common, line 540
	md.update(data) // library marker davegut.lib_tpLink_common, line 541
	return md.digest() // library marker davegut.lib_tpLink_common, line 542
} // library marker davegut.lib_tpLink_common, line 543

String encodeUtf8(String message) { // library marker davegut.lib_tpLink_common, line 545
	byte[] arr = message.getBytes("UTF8") // library marker davegut.lib_tpLink_common, line 546
	return new String(arr) // library marker davegut.lib_tpLink_common, line 547
} // library marker davegut.lib_tpLink_common, line 548

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.lib_tpLink_common, line 550
	int arrayASInteger // library marker davegut.lib_tpLink_common, line 551
	try { // library marker davegut.lib_tpLink_common, line 552
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.lib_tpLink_common, line 553
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.lib_tpLink_common, line 554
	} catch (error) { // library marker davegut.lib_tpLink_common, line 555
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.lib_tpLink_common, line 556
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.lib_tpLink_common, line 557
	} // library marker davegut.lib_tpLink_common, line 558
	return arrayAsInteger // library marker davegut.lib_tpLink_common, line 559
} // library marker davegut.lib_tpLink_common, line 560

byte[] integerToByteArray(value) { // library marker davegut.lib_tpLink_common, line 562
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.lib_tpLink_common, line 563
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.lib_tpLink_common, line 564
	return byteValue // library marker davegut.lib_tpLink_common, line 565
} // library marker davegut.lib_tpLink_common, line 566

//	===== Communications ===== // library marker davegut.lib_tpLink_common, line 568
def createMultiCmd(requests) { // library marker davegut.lib_tpLink_common, line 569
	Map cmdBody = [ // library marker davegut.lib_tpLink_common, line 570
		method: "multipleRequest", // library marker davegut.lib_tpLink_common, line 571
		params: [requests: requests]] // library marker davegut.lib_tpLink_common, line 572
	return cmdBody // library marker davegut.lib_tpLink_common, line 573
} // library marker davegut.lib_tpLink_common, line 574

//	===== ASYNC Post ===== // library marker davegut.lib_tpLink_common, line 576
def asyncSend(cmdBody, reqData, action) { // library marker davegut.lib_tpLink_common, line 577
	Map cmdData = [cmdBody: cmdBody, reqData: reqData, action: action] // library marker davegut.lib_tpLink_common, line 578
	state.lastCmd = cmdData // library marker davegut.lib_tpLink_common, line 579
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_common, line 580
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_common, line 581
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 582
		byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.lib_tpLink_common, line 583
		String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_common, line 584
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_common, line 585
		def uri = "${getDataValue("baseUrl")}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_common, line 586
		asyncPost(uri, encryptedData.cipherData, "application/octet-stream", // library marker davegut.lib_tpLink_common, line 587
				  action, cookie, reqData) // library marker davegut.lib_tpLink_common, line 588
	} else { // library marker davegut.lib_tpLink_common, line 589
		def uri = "${getDataValue("baseUrl")}?token=${token}" // library marker davegut.lib_tpLink_common, line 590
		def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_common, line 591
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_common, line 592
					   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_common, line 593
		def body = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_common, line 594
		asyncPost(uri, body, "application/json", // library marker davegut.lib_tpLink_common, line 595
				  action, cookie, reqData) // library marker davegut.lib_tpLink_common, line 596
	} // library marker davegut.lib_tpLink_common, line 597
} // library marker davegut.lib_tpLink_common, line 598
def asyncPost(uri, body, contentType, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_common, line 599
	def reqParams = [ // library marker davegut.lib_tpLink_common, line 600
		uri: uri, // library marker davegut.lib_tpLink_common, line 601
		body: body, // library marker davegut.lib_tpLink_common, line 602
		contentType: contentType, // library marker davegut.lib_tpLink_common, line 603
		requestContentType: contentType, // library marker davegut.lib_tpLink_common, line 604
		headers: [ // library marker davegut.lib_tpLink_common, line 605
			"Cookie": cookie, // library marker davegut.lib_tpLink_common, line 606
		], // library marker davegut.lib_tpLink_common, line 607
		timeout: 8 // library marker davegut.lib_tpLink_common, line 608
	] // library marker davegut.lib_tpLink_common, line 609
	Map logData = [method: "asyncPost", uri: uri, contentType: contentType, // library marker davegut.lib_tpLink_common, line 610
				   parseMethod: parseMethod, cookie: cookie, reqData: reqData] // library marker davegut.lib_tpLink_common, line 611
	try { // library marker davegut.lib_tpLink_common, line 612
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_common, line 613
		logData << [status: "OK"] // library marker davegut.lib_tpLink_common, line 614
		logDebug(logData) // library marker davegut.lib_tpLink_common, line 615
	} catch (err) { // library marker davegut.lib_tpLink_common, line 616
		logData << [status: "FAILED", error: err, ] // library marker davegut.lib_tpLink_common, line 617
		logWarn(logData) // library marker davegut.lib_tpLink_common, line 618
	} // library marker davegut.lib_tpLink_common, line 619
} // library marker davegut.lib_tpLink_common, line 620
def parseData(resp) { // library marker davegut.lib_tpLink_common, line 621
	def logData = [method: "parseData"] // library marker davegut.lib_tpLink_common, line 622
	if (resp.status == 200) { // library marker davegut.lib_tpLink_common, line 623
		try { // library marker davegut.lib_tpLink_common, line 624
			Map cmdResp // library marker davegut.lib_tpLink_common, line 625
			byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_common, line 626
			byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_common, line 627
			if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_common, line 628
				byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.lib_tpLink_common, line 629
				cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, encIv)) // library marker davegut.lib_tpLink_common, line 630
			} else { // library marker davegut.lib_tpLink_common, line 631
				cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, encKey, encIv)) // library marker davegut.lib_tpLink_common, line 632
			} // library marker davegut.lib_tpLink_common, line 633
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_common, line 634
			state.errorCount = 0 // library marker davegut.lib_tpLink_common, line 635
			setCommsError(false) // library marker davegut.lib_tpLink_common, line 636
		} catch (err) { // library marker davegut.lib_tpLink_common, line 637
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.lib_tpLink_common, line 638
			runIn(1, handleCommsError, [data: "deviceDataParseError"]) // library marker davegut.lib_tpLink_common, line 639
		} // library marker davegut.lib_tpLink_common, line 640
	} else { // library marker davegut.lib_tpLink_common, line 641
		logData << [status: "httpFailure(timeout)", data: resp.properties] // library marker davegut.lib_tpLink_common, line 642
		runIn(1, handleCommsError, [data: "httpFailure(timeout)"]) // library marker davegut.lib_tpLink_common, line 643
	} // library marker davegut.lib_tpLink_common, line 644
	logDebug(logData) // library marker davegut.lib_tpLink_common, line 645
	return logData // library marker davegut.lib_tpLink_common, line 646
} // library marker davegut.lib_tpLink_common, line 647

//	===== Error Handling ===== // library marker davegut.lib_tpLink_common, line 649
def handleCommsError(retryReason) { // library marker davegut.lib_tpLink_common, line 650
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.lib_tpLink_common, line 651
	if (state.lastCmd != "") { // library marker davegut.lib_tpLink_common, line 652
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_common, line 653
		state.errorCount = count // library marker davegut.lib_tpLink_common, line 654
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_common, line 655
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_common, line 656
		Map data = [cmdBody: cmdBody, method: cmdBody.method, action: cmdBody.action] // library marker davegut.lib_tpLink_common, line 657
		logData << [count: count, command: cmdBody] // library marker davegut.lib_tpLink_common, line 658
		switch (count) { // library marker davegut.lib_tpLink_common, line 659
			case 1: // library marker davegut.lib_tpLink_common, line 660
				pauseExecution(2000) // library marker davegut.lib_tpLink_common, line 661
				Map loginData = deviceLogin() // library marker davegut.lib_tpLink_common, line 662
				logData << [retryLogin: loginData.loginStatus, action: "retryCommand"] // library marker davegut.lib_tpLink_common, line 663
				runIn(1, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_common, line 664
				break // library marker davegut.lib_tpLink_common, line 665
			case 2: // library marker davegut.lib_tpLink_common, line 666
				logData << [updateData: parent.tpLinkCheckForDevices(5), action: "retryCommand"] // library marker davegut.lib_tpLink_common, line 667
				runIn(3, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_common, line 668
			case 3: // library marker davegut.lib_tpLink_common, line 669
				logData << [status: setCommsError(true)] // library marker davegut.lib_tpLink_common, line 670
				logWarn(logData) // library marker davegut.lib_tpLink_common, line 671
				break // library marker davegut.lib_tpLink_common, line 672
			default: // library marker davegut.lib_tpLink_common, line 673
				logData << [status: "retriesDisabled"] // library marker davegut.lib_tpLink_common, line 674
				break // library marker davegut.lib_tpLink_common, line 675
		} // library marker davegut.lib_tpLink_common, line 676
	} else { // library marker davegut.lib_tpLink_common, line 677
		logData << [status: "noCommandToRetry"] // library marker davegut.lib_tpLink_common, line 678
	} // library marker davegut.lib_tpLink_common, line 679
	logInfo(logData) // library marker davegut.lib_tpLink_common, line 680
} // library marker davegut.lib_tpLink_common, line 681

def delayedPassThrough(data) { // library marker davegut.lib_tpLink_common, line 683
	asyncSend(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_common, line 684
} // library marker davegut.lib_tpLink_common, line 685

def setCommsError(status) { // library marker davegut.lib_tpLink_common, line 687
	if (device.currentValue("commsError") == true && status == false) { // library marker davegut.lib_tpLink_common, line 688
		updateAttr("commsError", false) // library marker davegut.lib_tpLink_common, line 689
		setPollInterval("25 min") // library marker davegut.lib_tpLink_common, line 690
	} else if (device.currentValue("commsError") == false && status == true) { // library marker davegut.lib_tpLink_common, line 691
		updateAttr("commsError", true) // library marker davegut.lib_tpLink_common, line 692
		setPollInterval() // library marker davegut.lib_tpLink_common, line 693
	} // library marker davegut.lib_tpLink_common, line 694
} // library marker davegut.lib_tpLink_common, line 695

// ~~~~~ end include (24) davegut.lib_tpLink_common ~~~~~

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
