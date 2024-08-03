/*	TP-LInk TAPO Device Installation Application
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Ver 2.3.7C
=================================================================================================*/
//	=====	NAMESPACE	============
def nameSpace() { return "davegut" }
//	================================

definition(
	name: "tapo_device_install",
	namespace: nameSpace(),
	author: "Dave Gutheinz",
	description: "Application to install TP-Link Tapo bulbs, plugs, and switches.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	installOnOpen: true,
	singleInstance: true,
	documentationLink: "https://github.com/DaveGut/tapoHubitat/blob/main/README.md",
	importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/App/tapo_device_install.groovy"
)

preferences {
	page(name: "startPage")
	page(name: "enterCredentialsPage")
	page(name: "processCredentials")
	page(name: "addDevicesPage")
	page(name: "removeDevicesPage")
	page(name: "listDevicesPage")
}

def installed() { 
	app?.updateSetting("logEnable", false)
	app?.updateSetting("infoLog", true)
	app?.updateSetting("hostLimits", [type:"string", value: "2, 254"])
	def hub = location.hub
	def hubIpArray = hub.localIP.split('\\.')
	def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
	app?.updateSetting("lanSegment", [type:"string", value: segments])
	state.devices = [:]
}

def updated() {
	app?.removeSetting("selectedAddDevices")
	app?.removeSetting("selectedRemoveDevices")
	app?.updateSetting("logEnable", false)
}

def uninstalled() {
    getAllChildDevices().each { 
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initInstance() {
	logInfo("initInstance: Getting external data for the app.")
	state.manualChecked = false
	unschedule()
	runIn(600, scheduleItems)
	return
}

def scheduleItems() {
	runEvery3Hours(appCheckDevices)
}

def startPage() {
	logInfo("starting Tapo Setup and Installation, kasaMatter = ${kasaMatter}")
	def action = initInstance()
	if (selectedRemoveDevices) { removeDevices() }
	if (selectedAddDevices) { addDevices() }
	if (devSort) { listDevices() }
	if (logEnable) { runIn(1800, debugLogOff) }
	def lanPrompt = "<b>Modify LAN Configuration. Current "
	try {
		state.segArray = lanSegment.split('\\,')
		def rangeArray = hostLimits.split('\\,')
		def array0 = rangeArray[0].toInteger()
		def array1 = array0 + 2
		if (rangeArray.size() > 1) {
			array1 = rangeArray[1].toInteger()
		}
		state.hostArray = [array0, array1]
	} catch (e) {
		logWarn("startPage: Invalid entry for Lan Segements, or Host Array Range. Resetting to default!")
		lanPrompt += "(Reset to Default) "
		def hub = location.hubs[0]
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
		app?.updateSetting("hostLimits", [type:"string", value: "1, 254"])
	}
	lanPrompt += ": [lanSegments: ${lanSegment}, hostRange: ${hostLimits}]"

	return dynamicPage(name:"startPage",
					   title:"<b>Tapo Device Installation</b>",
					   uninstall: true,
					   install: true) {
		section() {
			input "kasaMatter", "bool",
				title: "<b>Install Kasa Matter and new protocol devices</b>",
				submitOnChange: true,
				defaultValue: false
			input "appSetup", "bool",
				title: lanPrompt,
				submitOnChange: true,
				defaultalue: false
			if (appSetup) {
				input "lanSegment", "string",
					title: "<b>Lan Segments</b> (ex: 192.168.50, 192,168.01)",
					submitOnChange: true
				input "hostLimits", "string",
					title: "<b>Host Address Range</b> (ex: 5, 100)",
					submitOnChange: true
			}
			
			def credDesc = "Credentials: userName: ${userName}, password set."
			if (!userName || !userPassword) {
				credDesc = "<b>Credentials not set.</b>"
			}
			href "enterCredentialsPage",
				title: "<b>Enter/Update tpLink Credentials</b>",
				description: credDesc
			paragraph " "
			def addDesc = "Discovery including Kasa Devices"
			if (!userName || !userPassword) {
				addDesc = "<b>CREDENTIALS ARE NOT ENTERED</b>"
			} else if (!kasaMatter) {
				addDesc = "Discovery without Kasa Devices"
			}
			href "addDevicesPage",
				title: "<b>Add TpLink Devices</b>",
				description: addDesc
			
			href "removeDevicesPage",
				title: "<b>Remove TpLink Devices</b>",
				description: "You can also remove via the device's edit page."
			paragraph " "
			input "logEnable", "bool",
				   title: "<b>Enable debug logging for 30 minutes</b>",
				   submitOnChange: true,
				   defaultValue: false
		}
	}
}

def enterCredentialsPage() {
	logInfo("enterCredentialsPage")
	return dynamicPage (name: "enterCredentialsPage", 
    					title: "Enter TP-Link (Tapo) Credentials",
						nextPage: startPage,
                        install: false) {
		section() {
			paragraph "Current Credentials: [userName: ${userName}, userPassword: ${userPassword}] \n\r"
			input ("userName", "email",
            		title: "TP-Link Tapo Email Address", 
                    required: false,
                    submitOnChange: true)
			input ("userPassword", "password",
            		title: "TP-Link Tapo Account Password",
                    required: false,
                    submitOnChange: true)
			if (userName && userPassword && userName != null && userPassword != null) {
				logDebug("enterCredentialsPage: [username: ${userName}, pwdLen: ${userPassword.length()}]")
				href "processCredentials", title: "Create Encoded Credentials",
					description: "You may have to press this twice."
			}
		}
	}
}

private processCredentials() {
	Map logData = [method: "processCredentials", userName: userName, userPassword: userPassword]
	String encUsername = mdEncode("SHA-1", userName.bytes).encodeHex().encodeAsBase64().toString()
	app?.updateSetting("encUsername", [type: "password", value: encUsername])

	String encPassword = userPassword.bytes.encodeBase64().toString()
	app?.updateSetting("encPassword", [type: "password", value: encPassword])

	String encPasswordVac = mdEncode("MD5", userPassword.bytes).encodeHex().toString().toUpperCase()
	app?.updateSetting("encPasswordVac", [type: "password", value: encPasswordVac])

	def userHash = mdEncode("SHA-1", encodeUtf8(userName).getBytes())
	def passwordHash = mdEncode("SHA-1", encodeUtf8(userPassword).getBytes())
	byte[] klapLocalHash = [userHash, passwordHash].flatten()
	String localHash = mdEncode("SHA-256", klapLocalHash).encodeBase64().toString()
	app?.updateSetting("localHash", [type: "password", value: localHash])
	logData << [status: "credentials updated"]
	logInfo(logData)
	return startPage()
}

//	===== Add Devices =====
def addDevicesPage() {
	logInfo("<b>Currently Finding Devices</b>")
	Map logData = [method: "addDevicesPage"]
	if (!userName || !userPassword) {
		logData << [error: "userName or userPassword is null"]
		logWarn(logData)
		return startPage()
	} else {
		logData << [findInfo: findDevices(15)]
	}
	def devices = state.devices
	Map uninstalledDevices = [:]
	Map requiredDrivers = [:]
	List installedDevices = []
	List reqDrivers = []
	devices.each { device ->
		def isChild = getChildDevice(device.key)
		if (!isChild) {
			uninstalledDevices["${device.key}"] = "${device.value.alias}, ${device.value.type}"
			requiredDrivers["${device.value.type}"] = "${device.value.type}"
		} else {
			installedDevices << isChild
		}
	}
	requiredDrivers.each { reqDrivers << it.key }
	logData << [installedDevices: installedDevices,
				uninstalledDevices: uninstalledDevices,
				requiredDrivers: reqDrivers]
	logInfo(logData)

	addTitle = "Devices available to add "
	addTitle += "${uninstalledDevices.size() ?: 0}.  "
	addTitle += "Total devices: ${devices.size()}.  "
	String notes = "\t<b>InstalledDevices</b>: ${installedDevices}"
	notes += "\n\t<b>MissingDevices</b>: Some devices go into deep sleep."
	notes += "\n\t\tIf a device is missing try exercising (on/off) the "
	notes += "\n\t\tdevice and then execute discovery again."
	return dynamicPage(name:"addDevicesPage",
					   title: "Add Tapo Devices to Hubitat",
					   nextPage: startPage,
					   install: false) {
	 	section() {
			input ("selectedAddDevices", "enum",
				   required: false,
				   multiple: true,
				   title: addTitle,
				   description: "Use the dropdown to select devices.  Then select 'Done'.",
				   options: uninstalledDevices)
			paragraph "<b>Notes</b>: \n${notes}"
		}
	}
}

def findDevices(timeout) {
	state.devices = [:]
	def findData = findTpLinkDevices("getSmartLanData", timeout)
	updateDevices(discData)
	return findData
}

private sendLanCmd(ip, port, cmdData, action, commsTo = 5) {
	Map data = [port: port, action: action]
	logDebug("sendLanCmd: ${data}")
	def myHubAction = new hubitat.device.HubAction(
		cmdData,
		hubitat.device.Protocol.LAN,
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
		 destinationAddress: "${ip}:${port}",
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
		 parseWarning: true,
		 timeout: commsTo,
		 callback: action])
	try {
		sendHubCommand(myHubAction)
	} catch (error) {
		logWarn("sendLanCmd: command failed. Error = ${error}")
	}
}

def addDevices() {
	Map addedDevices = [:]
	Map failedAdds = [:]
	def devices = state.devices
	selectedAddDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = devices.find { it.key == dni }
			def alias = device.value.alias.replaceAll("[\u201C\u201D]", "\"").replaceAll("[\u2018\u2019]", "'").replaceAll("[^\\p{ASCII}]", "")
			Map deviceData = [protocol: device.value.protocol]
			deviceData << [baseUrl: device.value.baseUrl]
			deviceData << [capability: device.value.capability]
			if (device.value.ctLow) {
				deviceData << [ctLow: device.value.ctLow]
				deviceData << [ctHigh: device.value.ctHigh]
			}
			try {
				addChildDevice(
					nameSpace(),
					device.value.type,
					device.key,
					[
						"label": alias,
						"name" : device.value.model,
						"data" : deviceData
					]
				)
				addedDevices << ["${device.key}": [label: alias, baseUrl: device.value.baseUrl]]
			} catch (error) {
				failedAdds << ["${device.key}": [label: alias, type: device.value.type, error: error]]
			}
		}
		pauseExecution(3000)
	}
	logInfo("addDevices: [installed: ${addedDevices}]")
	if (failedAdds != [:]) {
		logWarn("addDevices: [failedToAdd: <b>${failedAdds}</b>]")
	}
	app?.removeSetting("selectedAddDevices")
}

def removeDevicesPage() {
	logInfo("removeDevicesPage")
	def devices = state.devices
	def installedDevices = [:]
	devices.each {
		def installed = false
		def isChild = getChildDevice(it.key)
		if (isChild) {
			installedDevices["${it.key}"] = "${it.value.alias}, type = ${it.value.type}"
		}
	}
	logDebug("removeDevicesPage: installedDevices = ${installedDevices}")
	return dynamicPage(name:"removedDevicesPage",
					   title:"<b>Remove Tapo Devices from Hubitat</b>",
					   nextPage: startPage,
					   install: false) {
		section("Select Devices to Remove from Hubitat") {
			input ("selectedRemoveDevices", "enum",
				   required: false,
				   multiple: true,
				   title: "Devices to remove (${installedDevices.size() ?: 0} available)",
				   description: "Use the dropdown to select devices.  Then select 'Done'.",
				   options: installedDevices)
		}
	}
}

def removeDevices() {
	Map logData = [devicesToRemove: selectedRemoveDevices]
	def devices = state.devices
	selectedRemoveDevices.each { dni ->
		def device = state.devices.find { it.key == dni }
		def isChild = getChildDevice(dni)
		if (isChild) {
			try {
				deleteChildDevice(dni)
				logData << ["${dni}": [status: "ok", alias: device.value.alias]]
			} catch (error) {
				logData << ["${dni}": [status: "FAILED", alias: device.value.alias, error: error]]
			}
		}
	}
	if (logData.toString().contains("FAILED")) {
		logWarn("removeDevices: ${logData}")
	} else {
		logInfo("removeDevices: ${logData}")
	}
}

def supportedProducts() {
	List supported = ["SMART.TAPOBULB", "SMART.TAPOHUB", "SMART.TAPOPLUG",
					  "SMART.TAPOSWITCH", "SMART.TAPOROBOVAC"]
	if (kasaMatter) {
		supported = ["SMART.TAPOBULB", "SMART.TAPOHUB", "SMART.TAPOPLUG",
					 "SMART.TAPOSWITCH", "SMART.TAPOROBOVAC",
					 "SMART.KASAPLUG", "SMART.KASASWITCH", "SMART.KASAHUB"]
	}
	return supported
}

//	===== Periodic Device Connectivity Check =====
def tpLinkCheckForDevices(timeout = 5) {
	Map logData = [method: "tpLinkCheckForDevices"]
	if (state.manualChecked == true) {
		logData << [status: "noCheck", reason: "Already done within interval"]
	} else {
		def findData = findTpLinkDevices("sendDataToDevice", timeout)
		logData << [status: "checking"]
		state.manualChecked = true
		runIn(120, resetManualChecked)
	}
	return logData
}

def resetManualChecked() { state.manualChecked = false }
	
def appCheckDevices() {
	Map logData = [method: "appCheckDevices"]
	state.manualChecked = false
	logData << [checkForDevices: tpLinkCheckForDevices(10)]
	logDebug(logData)
}

def sendDataToDevice(response) {
	List discData = []
	if (response instanceof Map) {
		Map devdata = getDiscData(response)
		if (devData.status != "INVALID") {
			discData << devData
		}
	} else {
		response.each {
			Map devData = getDiscData(it)
			if (devData.status == "OK") {
				discData << devData
			}
		}
	}
	updateDevices(discData)
}

def updateDevices(discData) {
	Map logData = [method: "updateDevices"]
	List children = getChildDevices()
	children.each { childDev ->
		Map childData = [:]
		def dni = childDev.deviceNetworkId
		def connected = "false"
		Map devData = discData.find{ it.dni == dni }
		if (devData != null) {
			if (childDev.getDataValue("baseUrl") == devData.baseUrl &&
			    childDev.getDataValue("protocol") == devData.protocol) {
				childData << [status: "noChanges"]
			} else {
				childDev.updateDataValue("baseUrl", devData.baseUrl)
				childDev.updateDataValue("protocol", devData.protocol)
				childData << ["baseUrl": devData.baseUrl,
							  "protocol": devData.protocol,
							  "connected": "true"]
				childDev.deviceLogin()
			}
		} else {
			childData << [connected: "false", reason: "not Discovered By App"]
		}
		logData << ["${childDev}": childData]
	}
	logDebug(logData)
logInfo(logData)
}




// ~~~~~ start include (26) davegut.lib_tpLink_discovery ~~~~~
library ( // library marker davegut.lib_tpLink_discovery, line 1
	name: "lib_tpLink_discovery", // library marker davegut.lib_tpLink_discovery, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_discovery, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_discovery, line 4
	description: "Common tpLink Smart Discovery Methods", // library marker davegut.lib_tpLink_discovery, line 5
	category: "utilities", // library marker davegut.lib_tpLink_discovery, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_discovery, line 7
) // library marker davegut.lib_tpLink_discovery, line 8
import org.json.JSONObject // library marker davegut.lib_tpLink_discovery, line 9
import groovy.json.JsonOutput // library marker davegut.lib_tpLink_discovery, line 10
import groovy.json.JsonBuilder // library marker davegut.lib_tpLink_discovery, line 11
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_discovery, line 12
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.lib_tpLink_discovery, line 13
import javax.crypto.spec.SecretKeySpec // library marker davegut.lib_tpLink_discovery, line 14
import javax.crypto.spec.IvParameterSpec // library marker davegut.lib_tpLink_discovery, line 15
import javax.crypto.Cipher // library marker davegut.lib_tpLink_discovery, line 16
import java.security.KeyFactory // library marker davegut.lib_tpLink_discovery, line 17
import java.util.Random // library marker davegut.lib_tpLink_discovery, line 18
import java.security.MessageDigest // library marker davegut.lib_tpLink_discovery, line 19

//	===== UPDATED ===== // library marker davegut.lib_tpLink_discovery, line 21
def findTpLinkDevices(action, timeout) { // library marker davegut.lib_tpLink_discovery, line 22
	Map logData = [method: "findTpLinkDevices", action: action, timeOut: timeout] // library marker davegut.lib_tpLink_discovery, line 23
	def start = state.hostArray.min().toInteger() // library marker davegut.lib_tpLink_discovery, line 24
	def finish = state.hostArray.max().toInteger() + 1 // library marker davegut.lib_tpLink_discovery, line 25
	logData << [hostArray: state.hostArray, pollSegment: state.segArray] // library marker davegut.lib_tpLink_discovery, line 26
	List deviceIPs = [] // library marker davegut.lib_tpLink_discovery, line 27
	state.segArray.each { // library marker davegut.lib_tpLink_discovery, line 28
		def pollSegment = it.trim() // library marker davegut.lib_tpLink_discovery, line 29
		for(int i = start; i < finish; i++) { // library marker davegut.lib_tpLink_discovery, line 30
			deviceIPs.add("${pollSegment}.${i.toString()}") // library marker davegut.lib_tpLink_discovery, line 31
		} // library marker davegut.lib_tpLink_discovery, line 32
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.lib_tpLink_discovery, line 33
		sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeout) // library marker davegut.lib_tpLink_discovery, line 34
	} // library marker davegut.lib_tpLink_discovery, line 35
	pauseExecution(2000 * timeout) // library marker davegut.lib_tpLink_discovery, line 36
	return logData // library marker davegut.lib_tpLink_discovery, line 37
} // library marker davegut.lib_tpLink_discovery, line 38

def getSmartLanData(response) { // library marker davegut.lib_tpLink_discovery, line 40
	logDebug("getSmartLanData: responses returned from devices") // library marker davegut.lib_tpLink_discovery, line 41
	List discData = [] // library marker davegut.lib_tpLink_discovery, line 42
	if (response instanceof Map) { // library marker davegut.lib_tpLink_discovery, line 43
		Map devData = getDiscData(response) // library marker davegut.lib_tpLink_discovery, line 44
		if (devData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 45
			discData << devData // library marker davegut.lib_tpLink_discovery, line 46
		} // library marker davegut.lib_tpLink_discovery, line 47
	} else { // library marker davegut.lib_tpLink_discovery, line 48
		response.each { // library marker davegut.lib_tpLink_discovery, line 49
			Map devData = getDiscData(it) // library marker davegut.lib_tpLink_discovery, line 50
			if (devData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 51
				discData << devData // library marker davegut.lib_tpLink_discovery, line 52
			} // library marker davegut.lib_tpLink_discovery, line 53
		} // library marker davegut.lib_tpLink_discovery, line 54
	} // library marker davegut.lib_tpLink_discovery, line 55
	getAllSmartDeviceData(discData) // library marker davegut.lib_tpLink_discovery, line 56
} // library marker davegut.lib_tpLink_discovery, line 57

def getDiscData(response) { // library marker davegut.lib_tpLink_discovery, line 59
	Map devData = [method: "getDiscData"] // library marker davegut.lib_tpLink_discovery, line 60
	try { // library marker davegut.lib_tpLink_discovery, line 61
		def respData = parseLanMessage(response.description) // library marker davegut.lib_tpLink_discovery, line 62
		if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.lib_tpLink_discovery, line 63
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.lib_tpLink_discovery, line 64
			String payloadString = new String(payloadByte) // library marker davegut.lib_tpLink_discovery, line 65
			Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.lib_tpLink_discovery, line 66
			List supported = supportedProducts() // library marker davegut.lib_tpLink_discovery, line 67
			if (supported.contains(payload.device_type)) { // library marker davegut.lib_tpLink_discovery, line 68
				def protocol = payload.mgt_encrypt_schm.encrypt_type // library marker davegut.lib_tpLink_discovery, line 69
				def port = payload.mgt_encrypt_schm.http_port // library marker davegut.lib_tpLink_discovery, line 70
				def dni = payload.mac.replaceAll("-", "") // library marker davegut.lib_tpLink_discovery, line 71
				def baseUrl = "http://${payload.ip}:${payload.mgt_encrypt_schm.http_port}/app" // library marker davegut.lib_tpLink_discovery, line 72
				if (payload.device_type == "SMART.TAPOROBOVAC") { // library marker davegut.lib_tpLink_discovery, line 73
					baseUrl = "https://${payload.ip}:${payload.mgt_encrypt_schm.http_port}" // library marker davegut.lib_tpLink_discovery, line 74
					protocol = "vacAes" // library marker davegut.lib_tpLink_discovery, line 75
				} // library marker davegut.lib_tpLink_discovery, line 76
				devData << [ // library marker davegut.lib_tpLink_discovery, line 77
					type: payload.device_type, model: payload.device_model, // library marker davegut.lib_tpLink_discovery, line 78
					baseUrl: baseUrl, dni: dni, devId: payload.device_id,  // library marker davegut.lib_tpLink_discovery, line 79
					ip: payload.ip, port: port, protocol: protocol, status: "OK"] // library marker davegut.lib_tpLink_discovery, line 80
			} else { // library marker davegut.lib_tpLink_discovery, line 81
				devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.lib_tpLink_discovery, line 82
							status: "INVALID", reason: "Device not supported."] // library marker davegut.lib_tpLink_discovery, line 83
			} // library marker davegut.lib_tpLink_discovery, line 84
		} // library marker davegut.lib_tpLink_discovery, line 85
		logDebug(devData) // library marker davegut.lib_tpLink_discovery, line 86
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 87
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.lib_tpLink_discovery, line 88
		logWarn(devData) // library marker davegut.lib_tpLink_discovery, line 89
	} // library marker davegut.lib_tpLink_discovery, line 90
	return devData // library marker davegut.lib_tpLink_discovery, line 91
} // library marker davegut.lib_tpLink_discovery, line 92

def getAllSmartDeviceData(List discData) { // library marker davegut.lib_tpLink_discovery, line 94
	Map logData = [method: "getAllSmartDeviceData"] // library marker davegut.lib_tpLink_discovery, line 95
	discData.each { Map devData -> // library marker davegut.lib_tpLink_discovery, line 96
		Map cmdResp = getSmartDeviceData(devData.baseUrl, devData.protocol) // library marker davegut.lib_tpLink_discovery, line 97
		if (cmdResp.error || cmdResp == null) { // library marker davegut.lib_tpLink_discovery, line 98
			logData << [status: "respError", data: cmdResp] // library marker davegut.lib_tpLink_discovery, line 99
			logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 100
		} else { // library marker davegut.lib_tpLink_discovery, line 101
			addToDevices(devData, cmdResp.result) // library marker davegut.lib_tpLink_discovery, line 102
		} // library marker davegut.lib_tpLink_discovery, line 103
		pauseExecution(200) // library marker davegut.lib_tpLink_discovery, line 104
	} // library marker davegut.lib_tpLink_discovery, line 105
} // library marker davegut.lib_tpLink_discovery, line 106

def getSmartDeviceData(baseUrl, protocol) { // library marker davegut.lib_tpLink_discovery, line 108
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 109
	if (protocol == "KLAP") { // library marker davegut.lib_tpLink_discovery, line 110
		cmdResp = getKlapDeviceData(baseUrl) // library marker davegut.lib_tpLink_discovery, line 111
	} else if (protocol == "AES") { // library marker davegut.lib_tpLink_discovery, line 112
		cmdResp = getAesDeviceData(baseUrl) // library marker davegut.lib_tpLink_discovery, line 113
	} else if (protocol == "vacAes") { // library marker davegut.lib_tpLink_discovery, line 114
		cmdResp = getVacDeviceData(baseUrl) // library marker davegut.lib_tpLink_discovery, line 115
	} // library marker davegut.lib_tpLink_discovery, line 116
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 117
} // library marker davegut.lib_tpLink_discovery, line 118

def getKlapDeviceData(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 120
	Map logData = [method: "getKlapDeviceData", baseUrl: baseUrl] // library marker davegut.lib_tpLink_discovery, line 121
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 122
	Map sessionData = klapLogin(baseUrl, localHash.decodeBase64()) // library marker davegut.lib_tpLink_discovery, line 123
	logData << [sessionData: sessionData] // library marker davegut.lib_tpLink_discovery, line 124
	if (sessionData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 125
		logData << [sessionDataStatus: sessionData.status] // library marker davegut.lib_tpLink_discovery, line 126
		def cmdStr = JsonOutput.toJson([method: "get_device_info"]).toString() // library marker davegut.lib_tpLink_discovery, line 127
		state.seqNo = sessionData.seqNo // library marker davegut.lib_tpLink_discovery, line 128
		byte[] encKey = sessionData.encKey // library marker davegut.lib_tpLink_discovery, line 129
		byte[] encIv = sessionData.encIv // library marker davegut.lib_tpLink_discovery, line 130
		byte[] encSig = sessionData.encSig // library marker davegut.lib_tpLink_discovery, line 131
		Map encryptedData = klapEncrypt(cmdStr.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_discovery, line 132
		def uri = "${baseUrl}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_discovery, line 133
		Map resp = klapSyncPost(uri, encryptedData.cipherData, sessionData.cookie) // library marker davegut.lib_tpLink_discovery, line 134
		if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 135
			try { // library marker davegut.lib_tpLink_discovery, line 136
				byte[] cipherResponse = resp.data[32..-1] // library marker davegut.lib_tpLink_discovery, line 137
				def clearResp =  klapDecrypt(cipherResponse, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 138
				cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 139
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 140
				cmdResp = [error: "cryptoError", method: "getKlapDeviceData"] // library marker davegut.lib_tpLink_discovery, line 141
				logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 142
				logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 143
			} // library marker davegut.lib_tpLink_discovery, line 144
		} else { // library marker davegut.lib_tpLink_discovery, line 145
			cmdResp = [error: "postError", method: "getKlapDeviceData"] // library marker davegut.lib_tpLink_discovery, line 146
			logData << [status: "postError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 147
			logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 148
		} // library marker davegut.lib_tpLink_discovery, line 149
	} else { // library marker davegut.lib_tpLink_discovery, line 150
		cmdResp = [error: "credentialError", method: "getKlapDeviceData"] // library marker davegut.lib_tpLink_discovery, line 151
		logData << [respStatus: "FAILED", reason: "Login process failure.  Check credentials."] // library marker davegut.lib_tpLink_discovery, line 152
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 153
	} // library marker davegut.lib_tpLink_discovery, line 154
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 155
} // library marker davegut.lib_tpLink_discovery, line 156

def getAesDeviceData(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 158
	Map logData = [method: "getAesDeviceData", baseUrl: baseUrl] // library marker davegut.lib_tpLink_discovery, line 159
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 160
	Map sessionData = aesLogin(baseUrl, encPassword, encUsername) // library marker davegut.lib_tpLink_discovery, line 161
	if (sessionData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 162
		byte[] encKey = sessionData.encKey // library marker davegut.lib_tpLink_discovery, line 163
		byte[] encIv = sessionData.encIv // library marker davegut.lib_tpLink_discovery, line 164
		def cmdStr = JsonOutput.toJson([method: "get_device_info"]).toString() // library marker davegut.lib_tpLink_discovery, line 165
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_discovery, line 166
					   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_discovery, line 167
		def uri = "${baseUrl}?token=${sessionData.token}" // library marker davegut.lib_tpLink_discovery, line 168
		Map resp = aesSyncPost(uri, reqBody, sessionData.cookie) // library marker davegut.lib_tpLink_discovery, line 169
		if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 170
			try { // library marker davegut.lib_tpLink_discovery, line 171
				def clearResp = aesDecrypt(resp.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 172
				cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 173
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 174
				cmdResp = [error: "cryptoError", method: "getAesDeviceData"] // library marker davegut.lib_tpLink_discovery, line 175
				logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 176
				logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 177
			} // library marker davegut.lib_tpLink_discovery, line 178
		} else { // library marker davegut.lib_tpLink_discovery, line 179
			cmdResp = [error: "postError", method: "getAesDeviceData"] // library marker davegut.lib_tpLink_discovery, line 180
			logData << [status: "postError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 181
			logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 182
		} // library marker davegut.lib_tpLink_discovery, line 183
	} else { // library marker davegut.lib_tpLink_discovery, line 184
		cmdResp = [error: "credentialError", method: "getAesDeviceData"] // library marker davegut.lib_tpLink_discovery, line 185
		logData << [respStatus: "FAILED", reason: "Check Credentials"] // library marker davegut.lib_tpLink_discovery, line 186
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 187
	} // library marker davegut.lib_tpLink_discovery, line 188
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 189
} // library marker davegut.lib_tpLink_discovery, line 190

def getVacDeviceData(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 192
	Map logData = [method: "getVacDeviceData", baseUrl: baseUrl] // library marker davegut.lib_tpLink_discovery, line 193
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 194
	Map loginData = vacLogin(baseUrl) // library marker davegut.lib_tpLink_discovery, line 195
	logData << [loginData: loginData] // library marker davegut.lib_tpLink_discovery, line 196
	if (loginData.token != "ERROR") { // library marker davegut.lib_tpLink_discovery, line 197
		Map reqBody = [method: "get_device_info"] // library marker davegut.lib_tpLink_discovery, line 198
		def uri = "${baseUrl}/?token=${loginData.token}" // library marker davegut.lib_tpLink_discovery, line 199
		Map resp = aesSyncPost(uri, reqBody) // library marker davegut.lib_tpLink_discovery, line 200
		if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 201
			try { // library marker davegut.lib_tpLink_discovery, line 202
				if (resp.data.error_code == 0) { // library marker davegut.lib_tpLink_discovery, line 203
					cmdResp = resp.data // library marker davegut.lib_tpLink_discovery, line 204
				} else { // library marker davegut.lib_tpLink_discovery, line 205
					cmdResp = [error: "respError", method: "getVacDeviceData"] // library marker davegut.lib_tpLink_discovery, line 206
					logData << [status: "responseError", error: "most like credentials", data: resp.data] // library marker davegut.lib_tpLink_discovery, line 207
					logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 208
				} // library marker davegut.lib_tpLink_discovery, line 209
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 210
				cmdResp = [error: "response", method: "getVacDeviceData"] // library marker davegut.lib_tpLink_discovery, line 211
				logData << [status: "responseError", error: "return data incomplete", data: err] // library marker davegut.lib_tpLink_discovery, line 212
				logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 213
			} // library marker davegut.lib_tpLink_discovery, line 214
		} else { // library marker davegut.lib_tpLink_discovery, line 215
			cmdResp = [error: "response", method: "getVacDeviceData"] // library marker davegut.lib_tpLink_discovery, line 216
			logData << [status: "postError", postJsonData: resp.properties] // library marker davegut.lib_tpLink_discovery, line 217
			logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 218
		} // library marker davegut.lib_tpLink_discovery, line 219
	} else { // library marker davegut.lib_tpLink_discovery, line 220
		cmdResp = [error: "tokenError", method: "getVacDeviceData"] // library marker davegut.lib_tpLink_discovery, line 221
		logData << [status: "tokenError"] // library marker davegut.lib_tpLink_discovery, line 222
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 223
	} // library marker davegut.lib_tpLink_discovery, line 224
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 225
} // library marker davegut.lib_tpLink_discovery, line 226

def vacLogin(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 228
	Map logData = [method: "deviceLogin", uri: baseUrl] // library marker davegut.lib_tpLink_discovery, line 229
	Map cmdBody = [method: "login", // library marker davegut.lib_tpLink_discovery, line 230
				   params: [hashed: true,  // library marker davegut.lib_tpLink_discovery, line 231
							password: encPasswordVac, // library marker davegut.lib_tpLink_discovery, line 232
							username: userName]] // library marker davegut.lib_tpLink_discovery, line 233
	def loginResp = aesSyncPost(baseUrl, cmdBody) // library marker davegut.lib_tpLink_discovery, line 234
	def token = [token: "ERROR"] // library marker davegut.lib_tpLink_discovery, line 235
	if (loginResp.status == 200 && loginResp.data && loginResp.data.error_code == 0) { // library marker davegut.lib_tpLink_discovery, line 236
		logData << [status: loginResp.status] // library marker davegut.lib_tpLink_discovery, line 237
		token = loginResp.data.result.token // library marker davegut.lib_tpLink_discovery, line 238
	} else { // library marker davegut.lib_tpLink_discovery, line 239
		logData << [status: "FAILED", reason: "HTTP Response Status"] // library marker davegut.lib_tpLink_discovery, line 240
	} // library marker davegut.lib_tpLink_discovery, line 241
	logData << [token: token] // library marker davegut.lib_tpLink_discovery, line 242
	return logData // library marker davegut.lib_tpLink_discovery, line 243
} // library marker davegut.lib_tpLink_discovery, line 244

def addToDevices(devData, cmdResp) { // library marker davegut.lib_tpLink_discovery, line 246
	String dni = devData.dni // library marker davegut.lib_tpLink_discovery, line 247
	Map deviceData = [:] // library marker davegut.lib_tpLink_discovery, line 248
	String deviceType = devData.type // library marker davegut.lib_tpLink_discovery, line 249
	byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.lib_tpLink_discovery, line 250
	def alias = new String(plainBytes) // library marker davegut.lib_tpLink_discovery, line 251
	if (alias == "") { // library marker davegut.lib_tpLink_discovery, line 252
		alias = devData.model // library marker davegut.lib_tpLink_discovery, line 253
	} // library marker davegut.lib_tpLink_discovery, line 254
	deviceData << [protocol: devData.protocol] // library marker davegut.lib_tpLink_discovery, line 255
	deviceData << [ip: devData.ip] // library marker davegut.lib_tpLink_discovery, line 256
	deviceData << [port: devData.port] // library marker davegut.lib_tpLink_discovery, line 257
	deviceData << [alias: alias] // library marker davegut.lib_tpLink_discovery, line 258
	deviceData << [model: devData.model] // library marker davegut.lib_tpLink_discovery, line 259
	deviceData << [baseUrl: devData.baseUrl] // library marker davegut.lib_tpLink_discovery, line 260
	String capability = "newType" // library marker davegut.lib_tpLink_discovery, line 261
	String feature // library marker davegut.lib_tpLink_discovery, line 262
	if (deviceType.contains("BULB")) { // library marker davegut.lib_tpLink_discovery, line 263
		capability = "bulb_dimmer" // library marker davegut.lib_tpLink_discovery, line 264
		if (cmdResp.color_temp_range) { // library marker davegut.lib_tpLink_discovery, line 265
			deviceData << [ctLow: cmdResp.color_temp_range[0]] // library marker davegut.lib_tpLink_discovery, line 266
			deviceData << [ctHigh: cmdResp.color_temp_range[1]] // library marker davegut.lib_tpLink_discovery, line 267
			if (cmdResp.color_temp_range[0] < cmdResp.color_temp_range[1]) { // library marker davegut.lib_tpLink_discovery, line 268
				capability = "bulb_color" // library marker davegut.lib_tpLink_discovery, line 269
			} else if (cmdResp.lighting_effect) { // library marker davegut.lib_tpLink_discovery, line 270
				capability = "bulb_lightStrip" // library marker davegut.lib_tpLink_discovery, line 271
			} // library marker davegut.lib_tpLink_discovery, line 272
		} // library marker davegut.lib_tpLink_discovery, line 273
	} else if (deviceType.contains("SWITCH") || deviceType.contains("PLUG")) { // library marker davegut.lib_tpLink_discovery, line 274
		capability = "plug" // library marker davegut.lib_tpLink_discovery, line 275
		if (cmdResp.brightness) { // library marker davegut.lib_tpLink_discovery, line 276
			capability = "plug_dimmer" // library marker davegut.lib_tpLink_discovery, line 277
		} // library marker davegut.lib_tpLink_discovery, line 278
		if (cmdResp.power_protection_status) { // library marker davegut.lib_tpLink_discovery, line 279
			capability = "plug_em" // library marker davegut.lib_tpLink_discovery, line 280
		} // library marker davegut.lib_tpLink_discovery, line 281
		if (!cmdResp.default_states) {		// parent plug does not have default_states // library marker davegut.lib_tpLink_discovery, line 282
			capability = "plug_multi" // library marker davegut.lib_tpLink_discovery, line 283
		} // library marker davegut.lib_tpLink_discovery, line 284
	} else if (deviceType.contains("HUB")) { // library marker davegut.lib_tpLink_discovery, line 285
		capability = "hub" // library marker davegut.lib_tpLink_discovery, line 286
	} else if (deviceType.contains("ROBOVAC")) { // library marker davegut.lib_tpLink_discovery, line 287
		capability = "robovac" // library marker davegut.lib_tpLink_discovery, line 288
	} // library marker davegut.lib_tpLink_discovery, line 289
	deviceData << [dni: dni] // library marker davegut.lib_tpLink_discovery, line 290
	deviceData << [type: "tpLink_${capability}"] // library marker davegut.lib_tpLink_discovery, line 291
	deviceData << [capability: capability] // library marker davegut.lib_tpLink_discovery, line 292
	state.devices << ["${dni}": deviceData] // library marker davegut.lib_tpLink_discovery, line 293
	logInfo("[method: addToDevices, <b>${deviceData.alias}</b>: [${dni}: ${deviceData}]]") // library marker davegut.lib_tpLink_discovery, line 294
} // library marker davegut.lib_tpLink_discovery, line 295

//	===== tpLink Communications // library marker davegut.lib_tpLink_discovery, line 297
def createMultiCmd(requests) { // library marker davegut.lib_tpLink_discovery, line 298
	Map cmdBody = [ // library marker davegut.lib_tpLink_discovery, line 299
		method: "multipleRequest", // library marker davegut.lib_tpLink_discovery, line 300
		params: [requests: requests]] // library marker davegut.lib_tpLink_discovery, line 301
	return cmdBody // library marker davegut.lib_tpLink_discovery, line 302
} // library marker davegut.lib_tpLink_discovery, line 303

def syncSend(cmdBody) { // library marker davegut.lib_tpLink_discovery, line 305
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 306
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_discovery, line 307
		cmdResp = klapSyncSend(cmdBody) // library marker davegut.lib_tpLink_discovery, line 308
	} else { // library marker davegut.lib_tpLink_discovery, line 309
		cmdResp = aesSyncSend(cmdBody) // library marker davegut.lib_tpLink_discovery, line 310
	} // library marker davegut.lib_tpLink_discovery, line 311
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 312
} // library marker davegut.lib_tpLink_discovery, line 313

def klapSyncSend(cmdBody) { // library marker davegut.lib_tpLink_discovery, line 315
	Map logData = [method: "klapSyncSend", cmdBody: cmdBody] // library marker davegut.lib_tpLink_discovery, line 316
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_discovery, line 317
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_discovery, line 318
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.lib_tpLink_discovery, line 319
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 320
	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_discovery, line 321
	def uri = "${getDataValue("baseUrl")}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_discovery, line 322
	def resp = klapSyncPost(uri, encryptedData.cipherData, cookie) // library marker davegut.lib_tpLink_discovery, line 323
	Map cmdResp = [status: "ERROR"] // library marker davegut.lib_tpLink_discovery, line 324
	if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 325
		try { // library marker davegut.lib_tpLink_discovery, line 326
			byte[] cipherResponse = resp.data[32..-1] // library marker davegut.lib_tpLink_discovery, line 327
			def clearResp =  klapDecrypt(cipherResponse, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 328
			cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 329
			logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 330
		} catch (err) { // library marker davegut.lib_tpLink_discovery, line 331
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 332
		} // library marker davegut.lib_tpLink_discovery, line 333
	} else { // library marker davegut.lib_tpLink_discovery, line 334
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 335
	} // library marker davegut.lib_tpLink_discovery, line 336
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 337
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 338
	} else { // library marker davegut.lib_tpLink_discovery, line 339
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 340
	} // library marker davegut.lib_tpLink_discovery, line 341
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 342
} // library marker davegut.lib_tpLink_discovery, line 343

def aesSyncSend(cmdBody) { // library marker davegut.lib_tpLink_discovery, line 345
	Map logData = [method: "aesSyncSend", cmdBody: cmdBody] // library marker davegut.lib_tpLink_discovery, line 346
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_discovery, line 347
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_discovery, line 348
	def uri = "${getDataValue("baseUrl")}?token=${token}" // library marker davegut.lib_tpLink_discovery, line 349
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 350
	Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_discovery, line 351
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_discovery, line 352
	def resp = aesSyncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_discovery, line 353
	Map cmdResp = [status: "ERROR"] // library marker davegut.lib_tpLink_discovery, line 354
	if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 355
		try { // library marker davegut.lib_tpLink_discovery, line 356
			def clearResp = aesDecrypt(resp.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 357
			cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 358
			logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 359
		} catch (err) { // library marker davegut.lib_tpLink_discovery, line 360
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 361
		} // library marker davegut.lib_tpLink_discovery, line 362
	} else { // library marker davegut.lib_tpLink_discovery, line 363
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 364
	} // library marker davegut.lib_tpLink_discovery, line 365
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 366
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 367
	} else { // library marker davegut.lib_tpLink_discovery, line 368
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 369
	} // library marker davegut.lib_tpLink_discovery, line 370
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 371
} // library marker davegut.lib_tpLink_discovery, line 372

def xxxxxxxxasyncSend(cmdBody, method, action) { // library marker davegut.lib_tpLink_discovery, line 374
	Map cmdData = [cmdBody: cmdBody, method: method, action: action] // library marker davegut.lib_tpLink_discovery, line 375
	state.lastCmd = cmdData // library marker davegut.lib_tpLink_discovery, line 376
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_discovery, line 377
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_discovery, line 378
	if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_discovery, line 379
		byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.lib_tpLink_discovery, line 380
		String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 381
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_discovery, line 382
		def uri = "${getDataValue("baseUrl")}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_discovery, line 383
		asyncPost(uri, encryptedData.cipherData, "application/octet-stream", // library marker davegut.lib_tpLink_discovery, line 384
					  action, cookie, method) // library marker davegut.lib_tpLink_discovery, line 385
	} else { // library marker davegut.lib_tpLink_discovery, line 386
		def uri = "${getDataValue("baseUrl")}?token=${token}" // library marker davegut.lib_tpLink_discovery, line 387
		def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 388
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_discovery, line 389
					   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_discovery, line 390
		def body = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_discovery, line 391
		asyncPost(uri, body, "application/json",  // library marker davegut.lib_tpLink_discovery, line 392
					  action, cookie, method) // library marker davegut.lib_tpLink_discovery, line 393
	} // library marker davegut.lib_tpLink_discovery, line 394
} // library marker davegut.lib_tpLink_discovery, line 395

//	===== HTTP POST Methods ===== // library marker davegut.lib_tpLink_discovery, line 397
def klapSyncPost(uri, byte[] body, cookie = null) { // library marker davegut.lib_tpLink_discovery, line 398
	def reqParams = [ // library marker davegut.lib_tpLink_discovery, line 399
		uri: uri, // library marker davegut.lib_tpLink_discovery, line 400
		body: body, // library marker davegut.lib_tpLink_discovery, line 401
		contentType: "application/octet-stream", // library marker davegut.lib_tpLink_discovery, line 402
		requestContentType: "application/octet-stream", // library marker davegut.lib_tpLink_discovery, line 403
		headers: [ // library marker davegut.lib_tpLink_discovery, line 404
			"Cookie": cookie, // library marker davegut.lib_tpLink_discovery, line 405
		], // library marker davegut.lib_tpLink_discovery, line 406
		ignoreSSLIssues: true, // library marker davegut.lib_tpLink_discovery, line 407
		timeout: 8 // library marker davegut.lib_tpLink_discovery, line 408
	] // library marker davegut.lib_tpLink_discovery, line 409
	Map respData = [method: "klapSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_discovery, line 410
	try { // library marker davegut.lib_tpLink_discovery, line 411
		httpPost(reqParams) { resp -> // library marker davegut.lib_tpLink_discovery, line 412
			respData << [status: resp.status] // library marker davegut.lib_tpLink_discovery, line 413
			if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 414
				byte[] data = [] // library marker davegut.lib_tpLink_discovery, line 415
				if (resp.data != null) { // library marker davegut.lib_tpLink_discovery, line 416
					data = parseInputStream(resp.data) // library marker davegut.lib_tpLink_discovery, line 417
				} // library marker davegut.lib_tpLink_discovery, line 418
				respData << [data: data, headers: resp.headers] // library marker davegut.lib_tpLink_discovery, line 419
			} else { // library marker davegut.lib_tpLink_discovery, line 420
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_discovery, line 421
			} // library marker davegut.lib_tpLink_discovery, line 422
		} // library marker davegut.lib_tpLink_discovery, line 423
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 424
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_discovery, line 425
	} // library marker davegut.lib_tpLink_discovery, line 426
	return respData // library marker davegut.lib_tpLink_discovery, line 427
} // library marker davegut.lib_tpLink_discovery, line 428
def parseInputStream(data) { // library marker davegut.lib_tpLink_discovery, line 429
	def dataSize = data.available() // library marker davegut.lib_tpLink_discovery, line 430
	byte[] dataArr = new byte[dataSize] // library marker davegut.lib_tpLink_discovery, line 431
	data.read(dataArr, 0, dataSize) // library marker davegut.lib_tpLink_discovery, line 432
	return dataArr // library marker davegut.lib_tpLink_discovery, line 433
} // library marker davegut.lib_tpLink_discovery, line 434

def aesSyncPost(uri, reqBody, cookie=null) { // library marker davegut.lib_tpLink_discovery, line 436
	def reqParams = [ // library marker davegut.lib_tpLink_discovery, line 437
		uri: uri, // library marker davegut.lib_tpLink_discovery, line 438
		headers: [ // library marker davegut.lib_tpLink_discovery, line 439
			Cookie: cookie, // library marker davegut.lib_tpLink_discovery, line 440
		], // library marker davegut.lib_tpLink_discovery, line 441
		//	body: reqBody, // library marker davegut.lib_tpLink_discovery, line 442
		body : new JsonBuilder(reqBody).toString(), // library marker davegut.lib_tpLink_discovery, line 443
		ignoreSSLIssues: true, // library marker davegut.lib_tpLink_discovery, line 444
		timeout: 8 // library marker davegut.lib_tpLink_discovery, line 445
	] // library marker davegut.lib_tpLink_discovery, line 446
	Map respData = [method: "aesSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_discovery, line 447
	try { // library marker davegut.lib_tpLink_discovery, line 448
		httpPostJson(reqParams) {resp -> // library marker davegut.lib_tpLink_discovery, line 449
			respData << [status: resp.status] // library marker davegut.lib_tpLink_discovery, line 450
			if (resp.status == 200 && resp.data.error_code == 0) { // library marker davegut.lib_tpLink_discovery, line 451
				respData << [data: resp.data, headers: resp.headers] // library marker davegut.lib_tpLink_discovery, line 452
			} else { // library marker davegut.lib_tpLink_discovery, line 453
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_discovery, line 454
			} // library marker davegut.lib_tpLink_discovery, line 455
		} // library marker davegut.lib_tpLink_discovery, line 456
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 457
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_discovery, line 458
	} // library marker davegut.lib_tpLink_discovery, line 459
	return respData // library marker davegut.lib_tpLink_discovery, line 460
} // library marker davegut.lib_tpLink_discovery, line 461

def xxxxxasyncPost(uri, body, contentType, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_discovery, line 463
	def reqParams = [ // library marker davegut.lib_tpLink_discovery, line 464
		uri: uri, // library marker davegut.lib_tpLink_discovery, line 465
		body: body, // library marker davegut.lib_tpLink_discovery, line 466
		contentType: contentType, // library marker davegut.lib_tpLink_discovery, line 467
		requestContentType: contentType, // library marker davegut.lib_tpLink_discovery, line 468
		headers: [ // library marker davegut.lib_tpLink_discovery, line 469
			"Cookie": cookie, // library marker davegut.lib_tpLink_discovery, line 470
		], // library marker davegut.lib_tpLink_discovery, line 471
		timeout: 8 // library marker davegut.lib_tpLink_discovery, line 472
	] // library marker davegut.lib_tpLink_discovery, line 473
	Map logData = [method: "asyncPost", uri: uri,  // library marker davegut.lib_tpLink_discovery, line 474
				   parseMethod: parseMethod, cookie: cookie, reqData: reqData] // library marker davegut.lib_tpLink_discovery, line 475
	try { // library marker davegut.lib_tpLink_discovery, line 476
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_discovery, line 477
		logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 478
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 479
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 480
		logData << [status: "FAILED", error: err, ] // library marker davegut.lib_tpLink_discovery, line 481
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 482
	} // library marker davegut.lib_tpLink_discovery, line 483
} // library marker davegut.lib_tpLink_discovery, line 484
def xxxxxparseData(resp) { // library marker davegut.lib_tpLink_discovery, line 485
	def logData = [method: "parseData"] // library marker davegut.lib_tpLink_discovery, line 486
	if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 487
		try { // library marker davegut.lib_tpLink_discovery, line 488
			Map cmdResp // library marker davegut.lib_tpLink_discovery, line 489
			byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_discovery, line 490
			byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_discovery, line 491
			if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_discovery, line 492
				byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.lib_tpLink_discovery, line 493
				cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, encIv)) // library marker davegut.lib_tpLink_discovery, line 494
			} else { // library marker davegut.lib_tpLink_discovery, line 495
				cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, encKey, encIv)) // library marker davegut.lib_tpLink_discovery, line 496
			} // library marker davegut.lib_tpLink_discovery, line 497
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_discovery, line 498
			state.errorCount = 0 // library marker davegut.lib_tpLink_discovery, line 499
			setCommsError(false) // library marker davegut.lib_tpLink_discovery, line 500
		} catch (err) { // library marker davegut.lib_tpLink_discovery, line 501
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.lib_tpLink_discovery, line 502
			runIn(1, handleCommsError, [data: "deviceDataParseError"]) // library marker davegut.lib_tpLink_discovery, line 503
		} // library marker davegut.lib_tpLink_discovery, line 504
	} else { // library marker davegut.lib_tpLink_discovery, line 505
		logData << [status: "httpFailure(timeout)", data: resp.properties] // library marker davegut.lib_tpLink_discovery, line 506
		runIn(1, handleCommsError, [data: "httpFailure(timeout)"]) // library marker davegut.lib_tpLink_discovery, line 507
	} // library marker davegut.lib_tpLink_discovery, line 508
	logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 509
	return logData // library marker davegut.lib_tpLink_discovery, line 510
} // library marker davegut.lib_tpLink_discovery, line 511

//	===== Error Handling ===== // library marker davegut.lib_tpLink_discovery, line 513
def xxxxxhandleCommsError(retryReason) { // library marker davegut.lib_tpLink_discovery, line 514
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.lib_tpLink_discovery, line 515
	if (state.lastCmd != "") { // library marker davegut.lib_tpLink_discovery, line 516
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_discovery, line 517
		state.errorCount = count // library marker davegut.lib_tpLink_discovery, line 518
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_discovery, line 519
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_discovery, line 520
		Map data = [cmdBody: cmdBody, method: cmdData.method, action: cmdData.action] // library marker davegut.lib_tpLink_discovery, line 521
		logData << [count: count, command: cmdData] // library marker davegut.lib_tpLink_discovery, line 522
		switch (count) { // library marker davegut.lib_tpLink_discovery, line 523
			case 1: // library marker davegut.lib_tpLink_discovery, line 524
				pauseExecution(2000) // library marker davegut.lib_tpLink_discovery, line 525
				Map loginData = deviceLogin() // library marker davegut.lib_tpLink_discovery, line 526
				logData << [retryLogin: loginData.loginStatus, action: "retryCommand"] // library marker davegut.lib_tpLink_discovery, line 527
				runIn(1, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_discovery, line 528
				break // library marker davegut.lib_tpLink_discovery, line 529
			case 2: // library marker davegut.lib_tpLink_discovery, line 530
				logData << [updateData: parent.tpLinkCheckForDevices(5), action: "retryCommand"] // library marker davegut.lib_tpLink_discovery, line 531
				runIn(3, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_discovery, line 532
			case 3: // library marker davegut.lib_tpLink_discovery, line 533
				logData << [status: setCommsError(true)] // library marker davegut.lib_tpLink_discovery, line 534
				logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 535
				break // library marker davegut.lib_tpLink_discovery, line 536
			default: // library marker davegut.lib_tpLink_discovery, line 537
				logData << [status: "retriesDisabled"] // library marker davegut.lib_tpLink_discovery, line 538
				break // library marker davegut.lib_tpLink_discovery, line 539
		} // library marker davegut.lib_tpLink_discovery, line 540
	} else { // library marker davegut.lib_tpLink_discovery, line 541
		logData << [status: "noCommandToRetry"] // library marker davegut.lib_tpLink_discovery, line 542
	} // library marker davegut.lib_tpLink_discovery, line 543
	logInfo(logData) // library marker davegut.lib_tpLink_discovery, line 544
} // library marker davegut.lib_tpLink_discovery, line 545

def xxxxxxxdelayedPassThrough(data) { // library marker davegut.lib_tpLink_discovery, line 547
	asyncSend(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_discovery, line 548
} // library marker davegut.lib_tpLink_discovery, line 549

def xxxxxsetCommsError(status) { // library marker davegut.lib_tpLink_discovery, line 551
	Map logData = [method: "setCommsError", status: status] // library marker davegut.lib_tpLink_discovery, line 552
	if (device.currentValue("commsError") != status) { // library marker davegut.lib_tpLink_discovery, line 553
		sendEvent(name: "commsError", value: status) // library marker davegut.lib_tpLink_discovery, line 554
		if (status == true) { // library marker davegut.lib_tpLink_discovery, line 555
			logData << [pollInterval: setPollInterval("15 min")] // library marker davegut.lib_tpLink_discovery, line 556
		} else { // library marker davegut.lib_tpLink_discovery, line 557
			logData << [pollInterval: setPollInterval()] // library marker davegut.lib_tpLink_discovery, line 558
		} // library marker davegut.lib_tpLink_discovery, line 559
	} // library marker davegut.lib_tpLink_discovery, line 560
	return logData // library marker davegut.lib_tpLink_discovery, line 561
} // library marker davegut.lib_tpLink_discovery, line 562

//	===== tpLink Security ===== // library marker davegut.lib_tpLink_discovery, line 564
//	===== KLAP Handshake and Login ===== // library marker davegut.lib_tpLink_discovery, line 565
def klapLogin(baseUrl, localHash) { // library marker davegut.lib_tpLink_discovery, line 566
	Map logData = [method: "klapLogin"] // library marker davegut.lib_tpLink_discovery, line 567
	Map sessionData = [protocol: "KLAP"] // library marker davegut.lib_tpLink_discovery, line 568
	byte[] localSeed = new byte[16] // library marker davegut.lib_tpLink_discovery, line 569
	new Random().nextBytes(localSeed) // library marker davegut.lib_tpLink_discovery, line 570
	def status = "ERROR" // library marker davegut.lib_tpLink_discovery, line 571
	Map handshakeData = klapHandshake(localSeed, localHash, "${baseUrl}/handshake1") // library marker davegut.lib_tpLink_discovery, line 572
	logData << [handshake: handshakeData] // library marker davegut.lib_tpLink_discovery, line 573
	sessionData << [handshakeValidated: handshakeData.validated] // library marker davegut.lib_tpLink_discovery, line 574
	if (handshakeData.validated == true) { // library marker davegut.lib_tpLink_discovery, line 575
		sessionData << klapCreateSessionData(localSeed, handshakeData.remoteSeed, // library marker davegut.lib_tpLink_discovery, line 576
											 localHash, handshakeData.cookie) // library marker davegut.lib_tpLink_discovery, line 577
		Map loginData = klapLoginDevice("${baseUrl}/handshake2", localHash, localSeed,  // library marker davegut.lib_tpLink_discovery, line 578
										handshakeData.remoteSeed, handshakeData.cookie) // library marker davegut.lib_tpLink_discovery, line 579
		logData << [loginData: loginData] // library marker davegut.lib_tpLink_discovery, line 580
		if (loginData.loginSuccess == true) { // library marker davegut.lib_tpLink_discovery, line 581
			status = "OK" // library marker davegut.lib_tpLink_discovery, line 582
		} // library marker davegut.lib_tpLink_discovery, line 583
	} // library marker davegut.lib_tpLink_discovery, line 584
	sessionData << [status: status] // library marker davegut.lib_tpLink_discovery, line 585
	if (status != "OK") { // library marker davegut.lib_tpLink_discovery, line 586
		logInfo(logData) // library marker davegut.lib_tpLink_discovery, line 587
	} // library marker davegut.lib_tpLink_discovery, line 588
	return sessionData // library marker davegut.lib_tpLink_discovery, line 589
} // library marker davegut.lib_tpLink_discovery, line 590

def klapHandshake(localSeed, localHash, uri) { // library marker davegut.lib_tpLink_discovery, line 592
	Map handshakeData = [method: "klapHandshake", localSeed: localSeed, uri: uri, localHash: localHash] // library marker davegut.lib_tpLink_discovery, line 593
	def validated = false // library marker davegut.lib_tpLink_discovery, line 594
	Map respData = klapSyncPost(uri, localSeed) // library marker davegut.lib_tpLink_discovery, line 595
	if (respData.status == 200 && respData.data != null) { // library marker davegut.lib_tpLink_discovery, line 596
		byte[] data = respData.data // library marker davegut.lib_tpLink_discovery, line 597
		def cookieHeader = respData.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_discovery, line 598
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_discovery, line 599
		//	Validate data // library marker davegut.lib_tpLink_discovery, line 600
		byte[] remoteSeed = data[0 .. 15] // library marker davegut.lib_tpLink_discovery, line 601
		byte[] serverHash = data[16 .. 47] // library marker davegut.lib_tpLink_discovery, line 602
		byte[] authHashes = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_discovery, line 603
		byte[] localAuthHash = mdEncode("SHA-256", authHashes) // library marker davegut.lib_tpLink_discovery, line 604
		if (localAuthHash == serverHash) { // library marker davegut.lib_tpLink_discovery, line 605
			validated = true // library marker davegut.lib_tpLink_discovery, line 606
			handshakeData << [cookie : cookie] // library marker davegut.lib_tpLink_discovery, line 607
			handshakeData << [remoteSeed: remoteSeed] // library marker davegut.lib_tpLink_discovery, line 608
		} else { // library marker davegut.lib_tpLink_discovery, line 609
			handshakeData << [errorData: "Failed Hash Validation"] // library marker davegut.lib_tpLink_discovery, line 610
		} // library marker davegut.lib_tpLink_discovery, line 611
	} else { // library marker davegut.lib_tpLink_discovery, line 612
		handshakeData << [errorData: respData] // library marker davegut.lib_tpLink_discovery, line 613
	} // library marker davegut.lib_tpLink_discovery, line 614
	handshakeData << [validated: validated] // library marker davegut.lib_tpLink_discovery, line 615
	return handshakeData // library marker davegut.lib_tpLink_discovery, line 616
} // library marker davegut.lib_tpLink_discovery, line 617

def klapCreateSessionData(localSeed, remoteSeed, localHash, cookie) { // library marker davegut.lib_tpLink_discovery, line 619
	Map sessionData = [method: "klapCreateSessionData"] // library marker davegut.lib_tpLink_discovery, line 620
	//	seqNo and encIv // library marker davegut.lib_tpLink_discovery, line 621
	byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_discovery, line 622
	byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_discovery, line 623
	byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.lib_tpLink_discovery, line 624
	int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.lib_tpLink_discovery, line 625
	sessionData << [seqNo: seqNo] // library marker davegut.lib_tpLink_discovery, line 626
	sessionData << [encIv: fullIv[0..11], cookie: cookie] // library marker davegut.lib_tpLink_discovery, line 627
	//	KEY // library marker davegut.lib_tpLink_discovery, line 628
	payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_discovery, line 629
	sessionData << [encKey: mdEncode("SHA-256", payload)[0..15]] // library marker davegut.lib_tpLink_discovery, line 630
	//	SIG // library marker davegut.lib_tpLink_discovery, line 631
	payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.lib_tpLink_discovery, line 632
	sessionData << [encSig: mdEncode("SHA-256", payload)[0..27]] // library marker davegut.lib_tpLink_discovery, line 633
	return sessionData // library marker davegut.lib_tpLink_discovery, line 634
} // library marker davegut.lib_tpLink_discovery, line 635

def klapLoginDevice(uri, localHash, localSeed, remoteSeed, cookie) { // library marker davegut.lib_tpLink_discovery, line 637
	Map loginData = [method: "klapLoginDevice"] // library marker davegut.lib_tpLink_discovery, line 638
	byte[] authHashes = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.lib_tpLink_discovery, line 639
	byte[] body = mdEncode("SHA-256", authHashes) // library marker davegut.lib_tpLink_discovery, line 640
	Map respData = klapSyncPost(uri, body, cookie) // library marker davegut.lib_tpLink_discovery, line 641
	def loginSuccess = false // library marker davegut.lib_tpLink_discovery, line 642
	if (respData.status == 200) { // library marker davegut.lib_tpLink_discovery, line 643
		loginSuccess = true  // library marker davegut.lib_tpLink_discovery, line 644
	} else { // library marker davegut.lib_tpLink_discovery, line 645
		LoginData << [errorData: respData] // library marker davegut.lib_tpLink_discovery, line 646
	} // library marker davegut.lib_tpLink_discovery, line 647
	loginData << [loginSuccess: loginSuccess] // library marker davegut.lib_tpLink_discovery, line 648
	return loginData // library marker davegut.lib_tpLink_discovery, line 649
} // library marker davegut.lib_tpLink_discovery, line 650

//	===== Legacy (AES) Handshake and Login ===== // library marker davegut.lib_tpLink_discovery, line 652
def aesLogin(baseUrl, encPassword, encUsername) { // library marker davegut.lib_tpLink_discovery, line 653
	Map logData = [method: "aesLogin"] // library marker davegut.lib_tpLink_discovery, line 654
	Map sessionData = [protocol: "AES"] // library marker davegut.lib_tpLink_discovery, line 655
	Map handshakeData = aesHandshake(baseUrl) // library marker davegut.lib_tpLink_discovery, line 656
	def status = "ERROR" // library marker davegut.lib_tpLink_discovery, line 657
	logData << [handshakeData: handshakeData] // library marker davegut.lib_tpLink_discovery, line 658
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 659
		byte[] encKey = handshakeData.encKey // library marker davegut.lib_tpLink_discovery, line 660
		byte[] encIv = handshakeData.encIv // library marker davegut.lib_tpLink_discovery, line 661
		def tokenData = aesLoginDevice(baseUrl, handshakeData.cookie,  // library marker davegut.lib_tpLink_discovery, line 662
									   encKey, encIv, // library marker davegut.lib_tpLink_discovery, line 663
									   encPassword, encUsername) // library marker davegut.lib_tpLink_discovery, line 664
		logData << [tokenData: tokenData] // library marker davegut.lib_tpLink_discovery, line 665
		if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 666
			sessionData << [encKey: handshakeData.encKey, // library marker davegut.lib_tpLink_discovery, line 667
							encIv: handshakeData.encIv, // library marker davegut.lib_tpLink_discovery, line 668
							token: tokenData.token, // library marker davegut.lib_tpLink_discovery, line 669
							cookie: handshakeData.cookie, // library marker davegut.lib_tpLink_discovery, line 670
						    status: "OK"] // library marker davegut.lib_tpLink_discovery, line 671
			status = "OK" // library marker davegut.lib_tpLink_discovery, line 672
		} else { // library marker davegut.lib_tpLink_discovery, line 673
			sessionData << [status: "ERROR"] // library marker davegut.lib_tpLink_discovery, line 674
		} // library marker davegut.lib_tpLink_discovery, line 675
	} else { // library marker davegut.lib_tpLink_discovery, line 676
		sessionData << [status: "ERROR"] // library marker davegut.lib_tpLink_discovery, line 677
	} // library marker davegut.lib_tpLink_discovery, line 678
	logData << [status: status] // library marker davegut.lib_tpLink_discovery, line 679
	if (logData.status != "OK") { // library marker davegut.lib_tpLink_discovery, line 680
		logInfo(logData) // library marker davegut.lib_tpLink_discovery, line 681
	} // library marker davegut.lib_tpLink_discovery, line 682
	return sessionData // library marker davegut.lib_tpLink_discovery, line 683
} // library marker davegut.lib_tpLink_discovery, line 684

def aesHandshake(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 686
	def rsaKeys = getRsaKeys() // library marker davegut.lib_tpLink_discovery, line 687
	Map handshakeData = [method: "aesHandshake", rsaKeyNo: rsaKeys.keyNo] // library marker davegut.lib_tpLink_discovery, line 688
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKeys.public}-----END PUBLIC KEY-----\n" // library marker davegut.lib_tpLink_discovery, line 689
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.lib_tpLink_discovery, line 690
	def respStatus = "ERROR" // library marker davegut.lib_tpLink_discovery, line 691
	Map respData = aesSyncPost(baseUrl, cmdBody) // library marker davegut.lib_tpLink_discovery, line 692
	if (respData.status == 200 && respData.data != null) { // library marker davegut.lib_tpLink_discovery, line 693
		String deviceKey = respData.data.result.key // library marker davegut.lib_tpLink_discovery, line 694
		def cookieHeader = respData.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_discovery, line 695
		def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_discovery, line 696
		Map aesArray = aesReadDeviceKey(deviceKey, rsaKeys.private) // library marker davegut.lib_tpLink_discovery, line 697
		if (aesArraystatus == "ERROR") { // library marker davegut.lib_tpLink_discovery, line 698
			handshakeData << [check: "privateKey"] // library marker davegut.lib_tpLink_discovery, line 699
		} else { // library marker davegut.lib_tpLink_discovery, line 700
			respStatus = "OK" // library marker davegut.lib_tpLink_discovery, line 701
			handshakeData << [encKey: aesArray.cryptoArray[0..15],  // library marker davegut.lib_tpLink_discovery, line 702
							  encIv: aesArray.cryptoArray[16..31], cookie: cookie] // library marker davegut.lib_tpLink_discovery, line 703
		} // library marker davegut.lib_tpLink_discovery, line 704
	} else { // library marker davegut.lib_tpLink_discovery, line 705
		handshakeData << [errorData: respData] // library marker davegut.lib_tpLink_discovery, line 706
	} // library marker davegut.lib_tpLink_discovery, line 707
	handshakeData << [respStatus: respStatus] // library marker davegut.lib_tpLink_discovery, line 708
	return handshakeData // library marker davegut.lib_tpLink_discovery, line 709
} // library marker davegut.lib_tpLink_discovery, line 710

def aesReadDeviceKey(deviceKey, privateKey) { // library marker davegut.lib_tpLink_discovery, line 712
	def status = "ERROR" // library marker davegut.lib_tpLink_discovery, line 713
	def respData = [method: "aesReadDeviceKey"] // library marker davegut.lib_tpLink_discovery, line 714
	try { // library marker davegut.lib_tpLink_discovery, line 715
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.lib_tpLink_discovery, line 716
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.lib_tpLink_discovery, line 717
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.lib_tpLink_discovery, line 718
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.lib_tpLink_discovery, line 719
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.lib_tpLink_discovery, line 720
		respData << [cryptoArray: cryptoArray] // library marker davegut.lib_tpLink_discovery, line 721
		status = "OK" // library marker davegut.lib_tpLink_discovery, line 722
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 723
		respData << [errorData: err] // library marker davegut.lib_tpLink_discovery, line 724
	} // library marker davegut.lib_tpLink_discovery, line 725
	respData << [keyStatus: status] // library marker davegut.lib_tpLink_discovery, line 726
	return respData // library marker davegut.lib_tpLink_discovery, line 727
} // library marker davegut.lib_tpLink_discovery, line 728

def aesLoginDevice(uri, cookie, encKey, encIv, encPassword, encUsername) { // library marker davegut.lib_tpLink_discovery, line 730
	Map tokenData = [protocol: "aes"] // library marker davegut.lib_tpLink_discovery, line 731
	Map logData = [method: "aesLoginDevice"] // library marker davegut.lib_tpLink_discovery, line 732
	Map cmdBody = [method: "login_device", // library marker davegut.lib_tpLink_discovery, line 733
				   params: [password: encPassword, // library marker davegut.lib_tpLink_discovery, line 734
							username: encUsername], // library marker davegut.lib_tpLink_discovery, line 735
				   requestTimeMils: 0] // library marker davegut.lib_tpLink_discovery, line 736
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 737
	def encrString = aesEncrypt(cmdStr, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 738
	Map reqBody = [method: "securePassthrough", params: [request: encrString]] // library marker davegut.lib_tpLink_discovery, line 739
	def respData = aesSyncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_discovery, line 740
	if (respData.status == 200) { // library marker davegut.lib_tpLink_discovery, line 741
		if (respData.data.error_code == 0) { // library marker davegut.lib_tpLink_discovery, line 742
			try { // library marker davegut.lib_tpLink_discovery, line 743
				def cmdResp = aesDecrypt(respData.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 744
				cmdResp = new JsonSlurper().parseText(cmdResp) // library marker davegut.lib_tpLink_discovery, line 745
				if (cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_discovery, line 746
					tokenData << [respStatus: "OK", token: cmdResp.result.token] // library marker davegut.lib_tpLink_discovery, line 747
				} else { // library marker davegut.lib_tpLink_discovery, line 748
					tokenData << [respStatus: "ERROR", error_code: cmdResp.error_code, // library marker davegut.lib_tpLink_discovery, line 749
								  check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.lib_tpLink_discovery, line 750
				} // library marker davegut.lib_tpLink_discovery, line 751
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 752
				tokenData << [respStatus: "ERROR", error: err] // library marker davegut.lib_tpLink_discovery, line 753
			} // library marker davegut.lib_tpLink_discovery, line 754
		} else { // library marker davegut.lib_tpLink_discovery, line 755
			tokenData << [respStatus: "ERROR", data: respData.data] // library marker davegut.lib_tpLink_discovery, line 756
		} // library marker davegut.lib_tpLink_discovery, line 757
	} else { // library marker davegut.lib_tpLink_discovery, line 758
		tokenData << [respStatus: "ERROR", data: respData] // library marker davegut.lib_tpLink_discovery, line 759
	} // library marker davegut.lib_tpLink_discovery, line 760
	logData << [tokenData: tokenData] // library marker davegut.lib_tpLink_discovery, line 761
	if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 762
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 763
	} else { // library marker davegut.lib_tpLink_discovery, line 764
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 765
	} // library marker davegut.lib_tpLink_discovery, line 766
	return tokenData // library marker davegut.lib_tpLink_discovery, line 767
} // library marker davegut.lib_tpLink_discovery, line 768

//	===== Protocol specific encrytion/decryption ===== // library marker davegut.lib_tpLink_discovery, line 770
def klapEncrypt(byte[] request, encKey, encIv, encSig) { // library marker davegut.lib_tpLink_discovery, line 771
	int seqNo = state.seqNo + 1 // library marker davegut.lib_tpLink_discovery, line 772
	state.seqNo = seqNo // library marker davegut.lib_tpLink_discovery, line 773
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.lib_tpLink_discovery, line 774
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.lib_tpLink_discovery, line 775

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_discovery, line 777
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_discovery, line 778
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_discovery, line 779
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_discovery, line 780
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.lib_tpLink_discovery, line 781

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.lib_tpLink_discovery, line 783
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.lib_tpLink_discovery, line 784
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.lib_tpLink_discovery, line 785
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.lib_tpLink_discovery, line 786
} // library marker davegut.lib_tpLink_discovery, line 787

def aesEncrypt(request, encKey, encIv) { // library marker davegut.lib_tpLink_discovery, line 789
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_discovery, line 790
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_discovery, line 791
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_discovery, line 792
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_discovery, line 793
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.lib_tpLink_discovery, line 794
	return result.replace("\r\n","") // library marker davegut.lib_tpLink_discovery, line 795
} // library marker davegut.lib_tpLink_discovery, line 796

def klapDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_discovery, line 798
	byte[] encSeq = integerToByteArray(state.seqNo) // library marker davegut.lib_tpLink_discovery, line 799
	byte[] ivEnc = [encIv, encSeq].flatten() // library marker davegut.lib_tpLink_discovery, line 800

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_discovery, line 802
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_discovery, line 803
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.lib_tpLink_discovery, line 804
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_discovery, line 805
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.lib_tpLink_discovery, line 806
	return new String(byteResponse, "UTF-8") // library marker davegut.lib_tpLink_discovery, line 807
} // library marker davegut.lib_tpLink_discovery, line 808

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.lib_tpLink_discovery, line 810
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.lib_tpLink_discovery, line 811
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_discovery, line 812
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.lib_tpLink_discovery, line 813
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.lib_tpLink_discovery, line 814
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_discovery, line 815
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.lib_tpLink_discovery, line 816
	return result // library marker davegut.lib_tpLink_discovery, line 817
} // library marker davegut.lib_tpLink_discovery, line 818

//	===== RSA Key Methods ===== // library marker davegut.lib_tpLink_discovery, line 820
def getRsaKeys() { // library marker davegut.lib_tpLink_discovery, line 821
	def keyNo = Math.round(5 * Math.random()).toInteger() // library marker davegut.lib_tpLink_discovery, line 822
	def keyData = keyData() // library marker davegut.lib_tpLink_discovery, line 823
	def RSAKeys = keyData.find { it.keyNo == keyNo } // library marker davegut.lib_tpLink_discovery, line 824
	return RSAKeys // library marker davegut.lib_tpLink_discovery, line 825
} // library marker davegut.lib_tpLink_discovery, line 826

def keyData() { // library marker davegut.lib_tpLink_discovery, line 828
	//	Keys used for discovery. // library marker davegut.lib_tpLink_discovery, line 829
	return [ // library marker davegut.lib_tpLink_discovery, line 830
		[ // library marker davegut.lib_tpLink_discovery, line 831
			keyNo: 0, // library marker davegut.lib_tpLink_discovery, line 832
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.lib_tpLink_discovery, line 833
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw" // library marker davegut.lib_tpLink_discovery, line 834
		],[ // library marker davegut.lib_tpLink_discovery, line 835
			keyNo: 1, // library marker davegut.lib_tpLink_discovery, line 836
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCshy+qBKbJNefcyJUZ/3i+3KyLji6XaWEWvebUCC2r9/0jE6hc89AufO41a13E3gJ2es732vaxwZ1BZKLy468NnL+tg6vlQXaPkDcdunQwjxbTLNL/yzDZs9HRju2lJnupcksdJWBZmjtztMWQkzBrQVeSKzSTrKYK0s24EEXmtQIDAQAB", // library marker davegut.lib_tpLink_discovery, line 837
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKyHL6oEpsk159zIlRn/eL7crIuOLpdpYRa95tQILav3/SMTqFzz0C587jVrXcTeAnZ6zvfa9rHBnUFkovLjrw2cv62Dq+VBdo+QNx26dDCPFtMs0v/LMNmz0dGO7aUme6lySx0lYFmaO3O0xZCTMGtBV5IrNJOspgrSzbgQRea1AgMBAAECgYBSeiX9H1AkbJK1Z2ZwEUNF6vTJmmUHmScC2jHZNzeuOFVZSXJ5TU0+jBbMjtE65e9DeJ4suw6oF6j3tAZ6GwJ5tHoIy+qHRV6AjA8GEXjhSwwVCyP8jXYZ7UZyHzjLQAK+L0PvwJY1lAtns/Xmk5GH+zpNnhEmKSZAw23f7wpj2QJBANVPQGYT7TsMTDEEl2jq/ZgOX5Djf2VnKpPZYZGsUmg1hMwcpN/4XQ7XOaclR5TO/CJBJl3UCUEVjdrR1zdD8g8CQQDPDoa5Y5UfhLz4Ja2/gs2UKwO4fkTqqR6Ad8fQlaUZ55HINHWFd8FeERBFgNJzszrzd9BBJ7NnZM5nf2OPqU77AkBLuQuScSZ5HL97czbQvwLxVMDmLWyPMdVykOvLC9JhPgZ7cvuwqnlWiF7mEBzeHbBx9JDLJDd4zE8ETBPLgapPAkAHhCR52FaSdVQSwfNjr1DdHw6chODlj8wOp8p2FOiQXyqYlObrOGSpkH8BtuJs1sW+DsxdgR5vE2a2tRYdIe0/AkEAoQ5MzLcETQrmabdVCyB9pQAiHe4yY9e1w7cimsLJOrH7LMM0hqvBqFOIbSPrZyTp7Ie8awn4nTKoZQtvBfwzHw==" // library marker davegut.lib_tpLink_discovery, line 838
		],[ // library marker davegut.lib_tpLink_discovery, line 839
			keyNo: 2, // library marker davegut.lib_tpLink_discovery, line 840
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBeqRy4zAOs63Sc5yc0DtlFXG1stmdD6sEfUiGjlsy0S8aS8X+Qcjcu5AK3uBBrkVNIa8djXht1bd+pUof5/txzWIMJw9SNtNYqzSdeO7cCtRLzuQnQWP7Am64OBvYkXn2sUqoaqDE50LbSQWbuvZw0Vi9QihfBYGQdlrqjCPUsQIDAQAB", // library marker davegut.lib_tpLink_discovery, line 841
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIF6pHLjMA6zrdJznJzQO2UVcbWy2Z0PqwR9SIaOWzLRLxpLxf5ByNy7kAre4EGuRU0hrx2NeG3Vt36lSh/n+3HNYgwnD1I201irNJ147twK1EvO5CdBY/sCbrg4G9iRefaxSqhqoMTnQttJBZu69nDRWL1CKF8FgZB2WuqMI9SxAgMBAAECgYBBi2wkHI3/Y0Xi+1OUrnTivvBJIri2oW/ZXfKQ6w+PsgU+Mo2QII0l8G0Ck8DCfw3l9d9H/o2wTDgPjGzxqeXHAbxET1dS0QBTjR1zLZlFyfAs7WO8tDKmHVroUgqRkJgoQNQlBSe1E3e7pTgSKElzLuALkRS6p1jhzT2wu9U04QJBAOFr/G36PbQ6NmDYtVyEEr3vWn46JHeZISdJOsordR7Wzbt6xk6/zUDHq0OGM9rYrpBy7PNrbc0JuQrhfbIyaHMCQQCTCvETjXCMkwyUrQT6TpxVzKEVRf1rCitnNQCh1TLnDKcCEAnqZT2RRS3yNXTWFoJrtuEHMGmwUrtog9+ZJBlLAkEA2qxdkPY621XJIIO404mPgM7rMx4F+DsE7U5diHdFw2fO5brBGu13GAtZuUQ7k2W1WY0TDUO+nTN8XPDHdZDuvwJABu7TIwreLaKZS0FFJNAkCt+VEL22Dx/xn/Idz4OP3Nj53t0Guqh/WKQcYHkowxdYmt+KiJ49vXSJJYpiNoQ/NQJAM1HCl8hBznLZLQlxrCTdMvUimG3kJmA0bUNVncgUBq7ptqjk7lp5iNrle5aml99foYnzZeEUW6jrCC7Lj9tg+w==" // library marker davegut.lib_tpLink_discovery, line 842
		],[ // library marker davegut.lib_tpLink_discovery, line 843
			keyNo: 3, // library marker davegut.lib_tpLink_discovery, line 844
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFYaoMvv5kBxUUbp4PQyd7RoZlPompsupXP2La0qGGxacF98/88W4KNUqLbF4X5BPqxoEA+VeZy75qqyfuYbGQ4fxT6usE/LnzW8zDY/PjhVBht8FBRyAUsoYAt3Ip6sDyjd9YzRzUL1Q/OxCgxz5CNETYxcNr7zfMshBHDmZXMQIDAQAB", // library marker davegut.lib_tpLink_discovery, line 845
			private: "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIVhqgy+/mQHFRRung9DJ3tGhmU+iamy6lc/YtrSoYbFpwX3z/zxbgo1SotsXhfkE+rGgQD5V5nLvmqrJ+5hsZDh/FPq6wT8ufNbzMNj8+OFUGG3wUFHIBSyhgC3cinqwPKN31jNHNQvVD87EKDHPkI0RNjFw2vvN8yyEEcOZlcxAgMBAAECgYA3NxjoMeCpk+z8ClbQRqJ/e9CC9QKUB4bPG2RW5b8MRaJA7DdjpKZC/5CeavwAs+Ay3n3k41OKTTfEfJoJKtQQZnCrqnZfq9IVZI26xfYo0cgSYbi8wCie6nqIBdu9k54nqhePPshi22VcFuOh97xxPvY7kiUaRbbKqxn9PFwrYQJBAMsO3uOnYSJxN/FuxksKLqhtNei2GUC/0l7uIE8rbRdtN3QOpcC5suj7id03/IMn2Ks+Vsrmi0lV4VV/c8xyo9UCQQCoKDlObjbYeYYdW7/NvI6cEntgHygENi7b6WFk+dbRhJQgrFH8Z/Idj9a2E3BkfLCTUM1Z/Z3e7D0iqPDKBn/tAkBAHI3bKvnMOhsDq4oIH0rj+rdOplAK1YXCW0TwOjHTd7ROfGFxHDCUxvacVhTwBCCw0JnuriPEH81phTg2kOuRAkAEPR9UrsqLImUTEGEBWqNto7mgbqifko4T1QozdWjI10K0oCNg7W3Y+Os8o7jNj6cTz5GdlxsHp4TS/tczAH7xAkBY6KPIlF1FfiyJAnBC8+jJr2h4TSPQD7sbJJmYw7mvR+f1T4tsWY0aGux69hVm8BoaLStBVPdkaENBMdP+a07u" // library marker davegut.lib_tpLink_discovery, line 846
		],[ // library marker davegut.lib_tpLink_discovery, line 847
			keyNo: 4, // library marker davegut.lib_tpLink_discovery, line 848
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClF0yuCpo3r1ZpYlGcyI5wy5nnvZdOZmxqz5U2rklt2b8+9uWhmsGdpbTv5+qJXlZmvUKbpoaPxpJluBFDJH2GSpq3I0whh0gNq9Arzpp/TDYaZLb6iIqDMF6wm8yjGOtcSkB7qLQWkXpEN9T2NsEzlfTc+GTKc07QXHnzxoLmwQIDAQAB", // library marker davegut.lib_tpLink_discovery, line 849
			private: "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKUXTK4KmjevVmliUZzIjnDLmee9l05mbGrPlTauSW3Zvz725aGawZ2ltO/n6oleVma9Qpumho/GkmW4EUMkfYZKmrcjTCGHSA2r0CvOmn9MNhpktvqIioMwXrCbzKMY61xKQHuotBaRekQ31PY2wTOV9Nz4ZMpzTtBcefPGgubBAgMBAAECgYB4wCz+05RvDFk45YfqFCtTRyg//0UvO+0qxsBN6Xad2XlvlWjqJeZd53kLTGcYqJ6rsNyKOmgLu2MS8Wn24TbJmPUAwZU+9cvSPxxQ5k6bwjg1RifieIcbTPC5wHDqVy0/Ur7dt+JVMOHFseR/pElDw471LCdwWSuFHAKuiHsaUQJBANHiPdSU3s1bbJYTLaS1tW0UXo7aqgeXuJgqZ2sKsoIEheEAROJ5rW/f2KrFVtvg0ITSM8mgXNlhNBS5OE4nSD0CQQDJXYJxKvdodeRoj+RGTCZGZanAE1naUzSdfcNWx2IMnYUD/3/2eB7ZIyQPBG5fWjc3bGOJKI+gy/14bCwXU7zVAkAdnsE9HBlpf+qOL3y0jxRgpYxGuuNeGPJrPyjDOYpBwSOnwmL2V1e7vyqTxy/f7hVfeU7nuKMB5q7z8cPZe7+9AkEAl7A6aDe+wlE069OhWZdZqeRBmLC7Gi1d0FoBwahW4zvyDM32vltEmbvQGQP0hR33xGeBH7yPXcjtOz75g+UPtQJBAL4gknJ/p+yQm9RJB0oq/g+HriErpIMHwrhNoRY1aOBMJVl4ari1Ch2RQNL9KQW7yrFDv7XiP3z5NwNDKsp/QeU=" // library marker davegut.lib_tpLink_discovery, line 850
		],[ // library marker davegut.lib_tpLink_discovery, line 851
			keyNo: 5, // library marker davegut.lib_tpLink_discovery, line 852
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChN8Xc+gsSuhcLVM1W1E+e1o+celvKlOmuV6sJEkJecknKFujx9+T4xvyapzyePpTBn0lA9EYbaF7UDYBsDgqSwgt0El3gV+49O56nt1ELbLUJtkYEQPK+6Pu8665UG17leCiaMiFQyoZhD80PXhpjehqDu2900uU/4DzKZ/eywwIDAQAB", // library marker davegut.lib_tpLink_discovery, line 853
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKE3xdz6CxK6FwtUzVbUT57Wj5x6W8qU6a5XqwkSQl5yScoW6PH35PjG/JqnPJ4+lMGfSUD0RhtoXtQNgGwOCpLCC3QSXeBX7j07nqe3UQtstQm2RgRA8r7o+7zrrlQbXuV4KJoyIVDKhmEPzQ9eGmN6GoO7b3TS5T/gPMpn97LDAgMBAAECgYAy+uQCwL8HqPjoiGR2dKTI4aiAHuEv6m8KxoY7VB7QputWkHARNAaf9KykawXsNHXt1GThuV0CBbsW6z4U7UvCJEZEpv7qJiGX8UWgEs1ISatqXmiIMVosIJJvoFw/rAoScadCYyicskjwDFBVNU53EAUD3WzwEq+dRYDn52lqQQJBAMu30FEReAHTAKE/hvjAeBUyWjg7E4/lnYvb/i9Wuc+MTH0q3JxFGGMb3n6APT9+kbGE0rinM/GEXtpny+5y3asCQQDKl7eNq0NdIEBGAdKerX4O+nVDZ7PXz1kQ2ca0r1tXtY/9sBDDoKHP2fQAH/xlOLIhLaH1rabSEJYNUM0ohHdJAkBYZqhwNWtlJ0ITtvSEB0lUsWfzFLe1bseCBHH16uVwygn7GtlmupkNkO9o548seWkRpnimhnAE8xMSJY6aJ6BHAkEAuSFLKrqGJGOEWHTx8u63cxiMb7wkK+HekfdwDUzxO4U+v6RUrW/sbfPNdQ/FpPnaTVdV2RuGhg+CD0j3MT9bgQJARH86hfxp1bkyc7f1iJQT8sofdqqVz5grCV5XeGY77BNmCvTOGLfL5pOJdgALuOoP4t3e94nRYdlW6LqIVugRBQ==" // library marker davegut.lib_tpLink_discovery, line 854
		] // library marker davegut.lib_tpLink_discovery, line 855
	] // library marker davegut.lib_tpLink_discovery, line 856
} // library marker davegut.lib_tpLink_discovery, line 857

//	===== Encoding Methods ===== // library marker davegut.lib_tpLink_discovery, line 859
def mdEncode(hashMethod, byte[] data) { // library marker davegut.lib_tpLink_discovery, line 860
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.lib_tpLink_discovery, line 861
	md.update(data) // library marker davegut.lib_tpLink_discovery, line 862
	return md.digest() // library marker davegut.lib_tpLink_discovery, line 863
} // library marker davegut.lib_tpLink_discovery, line 864

String encodeUtf8(String message) { // library marker davegut.lib_tpLink_discovery, line 866
	byte[] arr = message.getBytes("UTF8") // library marker davegut.lib_tpLink_discovery, line 867
	return new String(arr) // library marker davegut.lib_tpLink_discovery, line 868
} // library marker davegut.lib_tpLink_discovery, line 869

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.lib_tpLink_discovery, line 871
	int arrayASInteger // library marker davegut.lib_tpLink_discovery, line 872
	try { // library marker davegut.lib_tpLink_discovery, line 873
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.lib_tpLink_discovery, line 874
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.lib_tpLink_discovery, line 875
	} catch (error) { // library marker davegut.lib_tpLink_discovery, line 876
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.lib_tpLink_discovery, line 877
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.lib_tpLink_discovery, line 878
	} // library marker davegut.lib_tpLink_discovery, line 879
	return arrayAsInteger // library marker davegut.lib_tpLink_discovery, line 880
} // library marker davegut.lib_tpLink_discovery, line 881

byte[] integerToByteArray(value) { // library marker davegut.lib_tpLink_discovery, line 883
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.lib_tpLink_discovery, line 884
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.lib_tpLink_discovery, line 885
	return byteValue // library marker davegut.lib_tpLink_discovery, line 886
} // library marker davegut.lib_tpLink_discovery, line 887

// ~~~~~ end include (26) davegut.lib_tpLink_discovery ~~~~~

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
