/*	TP-LInk TAPO Device Installation Application
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

Updates from previous version.
a.	Added Klap protocol with associated auto-use logic.
b.	Updated Check for devices to reduce processing load.
c.	Modeved credential call for devices to app from device data.
d.	Added support for the multi-plug.
e.	Added support for Kasa Matter Plugs and Switches.  Other Kasa Devices will not be discovered.
=================================================================================================*/
def label() { return app.getLabel() }
def version() { return "2.3.6a" }
def nameSpace() { return "davegut" }
def gitPath() { return "DaveGut/tpLink_Hubitat/main/App/" }
import groovy.json.JsonSlurper
import java.security.MessageDigest

definition(
	name: "tapo_device_install",
	namespace: "davegut",
	author: "Dave Gutheinz",
	description: "Application to install TP-Link Tapo bulbs, plugs, and switches.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	installOnOpen: true,
	singleInstance: true,
	documentationLink: "https://github.com/DaveGut/tapoHubitat/blob/main/README.md",
	importUrl: "https://raw.githubusercontent.com/${gitPath()}${label()}.groovy"
)

preferences {
	page(name: "startPage")
	page(name: "enterCredentialsPage")
	page(name: "processCredentials")
	page(name: "addDevicesPage")
	page(name: "removeDevicesPage")
	page(name: "listDevicesPage")
}

def installed() { }

def updated() {
	unschedule()
	runEvery15Minutes(appCheckDevices)
	app?.removeSetting("selectedAddDevices")
	app?.removeSetting("selectedRemoveDevices")
}

def uninstalled() {
    getAllChildDevices().each { 
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initInstance() {
	logDebug("initInstance: Getting external data for the app.")
	if (!debugLog) { app.updateSetting("debugLog", false) }
	if (!infoLog) { app.updateSetting("infoLog", true) }
	if (!state.devices) { state.devices = [:] }
	state.findingDevices = "ready"
	unschedule()
	if (!lanSegment) {
		def hub = location.hub
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
	}
	if (!hostLimits) {
		app?.updateSetting("hostLimits", [type:"string", value: "2, 254"])
	}
	return
}

def startPage() {
	logInfo("starting Tapo Setup and Installation")
	def action = initInstance()
	if (selectedRemoveDevices) { removeDevices() }
	if (selectedAddDevices) { addDevices() }
	if (devSort) { listDevices() }
	if (debugLog) { runIn(1800, debugOff) }
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
		logInfo("startPage: Invalid entry for Lan Segements, or Host Array Range. Resetting to default!")
		def hub = location.hubs[0]
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
		app?.updateSetting("hostLimits", [type:"string", value: "1, 254"])
	}

	return dynamicPage(name:"startPage",
					   title:"<b>Tapo Device Installation</b>",
					   uninstall: true,
					   install: true) {
		section() {
			def lanConfig = "[lanSegments: ${lanSegment}, hostRange: ${hostLimits}]"
			paragraph "<b>Current LAN Configuration: ${lanConfig}</b>"
			input "appSetup", "bool",
				title: "<b>Modify LAN Configuration</b>",
				description: "Used for setting non-standard IP segemet search",
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
			
			if (!encUsername || !encPassword || !localHash) {
				paragraph "<b>Credentials require attention</b>"
			}
			href "enterCredentialsPage",
				title: "<b>Enter/Update tpLink Credentials</b>",
				description: "Credentials are used by app and each Tapo devices during periodic login."
			
			if (encUsername && encPassword && localHash) {
				href "addDevicesPage",
					title: "<b>Add Tapo Devices</b>",
					description: "Searches LAN for devices and offers new devices for Hubitat installation.  Take 30 seconds to pop to next page."

				href "removeDevicesPage",
					title: "<b>Remove Tapo Devices</b>",
					description: "Provides interface to remove installed devices from Hubitat."
			}
			paragraph " "
			input "debugLog", "bool",
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
	Map logData = [method: "processCredentials"]
	String encUsername = mdEncode("SHA-1", userName.bytes).encodeHex().encodeAsBase64().toString()
	app?.updateSetting("encUsername", [type: "password", value: encUsername])
	String encPassword = userPassword.bytes.encodeBase64().toString()
	app?.updateSetting("encPassword", [type: "password", value: encPassword])
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
	Map logData = [:]
	Map uninstalledDevices = [:]
	Map requiredDrivers = [:]
	List installedDevices = []
	def notes = ""
	def addTitle = "<b>Currently Finding Devices</b>.  This can take several minutes."
	if (state.findingDevices == "ready") {
		state.devices = [:]
		logInfo("addDevicesPage: <b>FINDING DEVICES</b>. ${findDevices("getSmartLanData", 5)}")
	} else if (state.findingDevices == "looking") {
		logInfo("addDevicesPage: <b>WORKING</b>.  Next update in 30 seconds.")
	} else {
		def devices = state.devices
		devices.each { device ->
			def isChild = getChildDevice(device.key)
			if (!isChild) {
				uninstalledDevices["${device.key}"] = "${device.value.alias}, ${device.value.driver}"
				requiredDrivers["${device.value.driver}"] = "${device.value.driver}"
			} else {
				installedDevices << isChild
			}
		}
		logData << [installedDevices: installedDevices]
		uninstalledDevices.sort()
		logData << [uninstalledDevices: uninstalledDevices]
		def reqDrivers = []
		requiredDrivers.each {
			reqDrivers << it.key
		}
		logData << [requiredDrivers: reqDrivers]
		addTitle = "<b>Device Discovery Complete</b>.  Devices available to add "
		addTitle += "${uninstalledDevices.size() ?: 0}.  "
		addTitle += "Total devices: ${devices.size()}.  "
		logInfo("addDevicesPage: <b>WAITING ON YOU</b>.  ${logData}")
		notes = "\t<b>InstalledDevices</b>: ${installedDevices}"
		if (state.findingDevices == "failed") {
			logWarn("addDevicesPage: <b>Discovery did not end properly</b>.  Check Lan Segments validity and your LAN configuration.")
			notes += "\n\t<b>Discovery did not end properly</b>.  Check LAN and LAN Segments"
		}
	}
	return dynamicPage(name:"addDevicesPage",
					   title: "Add Tapo Devices to Hubitat",
					   refreshInterval: 30,
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

def findDevices(action, timeOut) {
	state.findingDevices = "looking"
	def logData = [action: action, timeOut: timeOut]
	def start = state.hostArray.min().toInteger()
	def finish = state.hostArray.max().toInteger() + 1
	logData << [hostArray: state.hostArray, pollSegment: state.segArray]
	List deviceIPs = []
	state.segArray.each {
		def pollSegment = it.trim()
		logDebug("findDevices: Searching for TP-Link SMART LAN deivces on IP Segment = ${pollSegment}")
		for(int i = start; i < finish; i++) {
			deviceIPs.add("${pollSegment}.${i.toString()}")
		}
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d"
		sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeOut)
	}
	runIn(120, forceEnd)
	return logData
}

def forceEnd() {
	if (state.findingDevices != "done") {
		state.findingDevices = "failed"
	}
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
					device.value.driver,
					device.key,
					[
						"label": alias,
						"name" : device.value.model,
						"data" : deviceData
					]
				)
				addedDevices << ["${device.key}": [label: alias, baseUrl: device.value.baseUrl]]
			} catch (error) {
				failedAdds << ["${device.key}": [label: alias, driver: device.value.driver, error: error]]
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
			installedDevices["${it.key}"] = "${it.value.alias}, driver = ${it.value.driver}"
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

def checkDevices() {
	//	Used by children to check for devices
	Map logData = [method: "parent.checkDevices"]
	if (state.manualChecked == true) {
		logData << [status: "noCheck", reason: "Already done within interval"]
	} else {
		logData << [checkForDevices: checkForDevices()]
		state.manualChecked = true
	}
	return logData
}
def appCheckDevices() {
	Map logData = [method: "appCheckDevices"]
	state.manualChecked = false
	logData << [checkForDevices: checkForDevices()]
	logDebug(logData)
}

def checkForDevices() {
	Map logData = [action: "Checking device connectivity and updating data for devices",
				   findDevices: findDevices("sendDataToDevice", 4)]
	return logData
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
			    childDev.getDataValue("protocol") == devData.protocol &&
			    childDev.currentValue("connected") == "true") {
				childData << [status: "noChanges"]
			} else {
				childDev.updateDataValue("baseUrl", devData.baseUrl)
				childDev.updateDataValue("protocol", devData.protocol)
				childDev.updateAttr("connected", "true")
				childData << ["baseUrl": devData.protocol,
							  "protocol": devData.protocol,
							  "connected": "true"]
				childDev.deviceLogin()
			}
		} else {
			childDev.updateAttr("connected", "false")
			childData << [connected: "false"]
		}
		logData << ["${childDev}": childData]
	}
	logDebug(logData)
}

def supportedProducts() {
	List supported = ["SMART.TAPOBULB", "SMART.TAPOHUB", "SMART.TAPOPLUG", "SMART.TAPOSWITCH",
					  "SMART.KASAPLUG", "SMART.KASASWITCH"]
	return supported
}







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
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 123
	] // library marker davegut.lib_tpLink_comms, line 124
	Map respData = [method: "klapSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_comms, line 125
	try { // library marker davegut.lib_tpLink_comms, line 126
		httpPost(reqParams) { resp -> // library marker davegut.lib_tpLink_comms, line 127
			respData << [status: resp.status] // library marker davegut.lib_tpLink_comms, line 128
			if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 129
				byte[] data = [] // library marker davegut.lib_tpLink_comms, line 130
				if (resp.data != null) { // library marker davegut.lib_tpLink_comms, line 131
					data = parseInputStream(resp.data) // library marker davegut.lib_tpLink_comms, line 132
				} // library marker davegut.lib_tpLink_comms, line 133
				respData << [data: data, headers: resp.headers] // library marker davegut.lib_tpLink_comms, line 134
			} else { // library marker davegut.lib_tpLink_comms, line 135
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_comms, line 136
			} // library marker davegut.lib_tpLink_comms, line 137
		} // library marker davegut.lib_tpLink_comms, line 138
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 139
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 140
	} // library marker davegut.lib_tpLink_comms, line 141
	return respData // library marker davegut.lib_tpLink_comms, line 142
} // library marker davegut.lib_tpLink_comms, line 143
def parseInputStream(data) { // library marker davegut.lib_tpLink_comms, line 144
	def dataSize = data.available() // library marker davegut.lib_tpLink_comms, line 145
	byte[] dataArr = new byte[dataSize] // library marker davegut.lib_tpLink_comms, line 146
	data.read(dataArr, 0, dataSize) // library marker davegut.lib_tpLink_comms, line 147
	return dataArr // library marker davegut.lib_tpLink_comms, line 148
} // library marker davegut.lib_tpLink_comms, line 149

def aesSyncPost(uri, reqBody, cookie=null) { // library marker davegut.lib_tpLink_comms, line 151
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 152
		uri: uri, // library marker davegut.lib_tpLink_comms, line 153
		headers: [ // library marker davegut.lib_tpLink_comms, line 154
			Cookie: cookie, // library marker davegut.lib_tpLink_comms, line 155
		], // library marker davegut.lib_tpLink_comms, line 156
		body : new JsonBuilder(reqBody).toString(), // library marker davegut.lib_tpLink_comms, line 157
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 158
	] // library marker davegut.lib_tpLink_comms, line 159
	Map respData = [method: "aesSyncPost", uri: uri, cookie: cookie] // library marker davegut.lib_tpLink_comms, line 160
	try { // library marker davegut.lib_tpLink_comms, line 161
		httpPostJson(reqParams) {resp -> // library marker davegut.lib_tpLink_comms, line 162
			respData << [status: resp.status] // library marker davegut.lib_tpLink_comms, line 163
			if (resp.status == 200 && resp.data.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 164
				respData << [data: resp.data, headers: resp.headers] // library marker davegut.lib_tpLink_comms, line 165
			} else { // library marker davegut.lib_tpLink_comms, line 166
				respData << [properties: resp.properties] // library marker davegut.lib_tpLink_comms, line 167
			} // library marker davegut.lib_tpLink_comms, line 168
		} // library marker davegut.lib_tpLink_comms, line 169
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 170
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 171
	} // library marker davegut.lib_tpLink_comms, line 172
	return respData // library marker davegut.lib_tpLink_comms, line 173
} // library marker davegut.lib_tpLink_comms, line 174

def asyncPost(uri, body, contentType, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_comms, line 176
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 177
		uri: uri, // library marker davegut.lib_tpLink_comms, line 178
		body: body, // library marker davegut.lib_tpLink_comms, line 179
		contentType: contentType, // library marker davegut.lib_tpLink_comms, line 180
		requestContentType: contentType, // library marker davegut.lib_tpLink_comms, line 181
		headers: [ // library marker davegut.lib_tpLink_comms, line 182
			"Cookie": cookie, // library marker davegut.lib_tpLink_comms, line 183
		], // library marker davegut.lib_tpLink_comms, line 184
		timeout: 4 // library marker davegut.lib_tpLink_comms, line 185
	] // library marker davegut.lib_tpLink_comms, line 186
	Map logData = [method: "asyncPost", uri: uri,  // library marker davegut.lib_tpLink_comms, line 187
				   parseMethod: parseMethod, cookie: cookie, reqData: reqData] // library marker davegut.lib_tpLink_comms, line 188
	try { // library marker davegut.lib_tpLink_comms, line 189
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_comms, line 190
		logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 191
		logDebug(logData) // library marker davegut.lib_tpLink_comms, line 192
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 193
		logData << [status: "FAILED", error: err, ] // library marker davegut.lib_tpLink_comms, line 194
		logWarn(logData) // library marker davegut.lib_tpLink_comms, line 195
	} // library marker davegut.lib_tpLink_comms, line 196
} // library marker davegut.lib_tpLink_comms, line 197
def parseData(resp) { // library marker davegut.lib_tpLink_comms, line 198
	def logData = [method: "parseData"] // library marker davegut.lib_tpLink_comms, line 199
	if (resp.status == 200) { // library marker davegut.lib_tpLink_comms, line 200
		try { // library marker davegut.lib_tpLink_comms, line 201
			Map cmdResp // library marker davegut.lib_tpLink_comms, line 202
			byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.lib_tpLink_comms, line 203
			byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.lib_tpLink_comms, line 204
			if (getDataValue("protocol") == "KLAP") { // library marker davegut.lib_tpLink_comms, line 205
				byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.lib_tpLink_comms, line 206
				cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, encIv)) // library marker davegut.lib_tpLink_comms, line 207
			} else { // library marker davegut.lib_tpLink_comms, line 208
				cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, encKey, encIv)) // library marker davegut.lib_tpLink_comms, line 209
			} // library marker davegut.lib_tpLink_comms, line 210
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 211
			if (device.currentValue("commsError") == "true") { // library marker davegut.lib_tpLink_comms, line 212
				logData << [resetError: setCommsError(false)] // library marker davegut.lib_tpLink_comms, line 213
			} // library marker davegut.lib_tpLink_comms, line 214
			state.errorCount = 0 // library marker davegut.lib_tpLink_comms, line 215
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 216
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.lib_tpLink_comms, line 217
			runIn(1, handleCommsError, [data: "deviceDataParseError"]) // library marker davegut.lib_tpLink_comms, line 218
		} // library marker davegut.lib_tpLink_comms, line 219
	} else { // library marker davegut.lib_tpLink_comms, line 220
		logData << [status: "httpFailure(timeout)", data: resp.properties] // library marker davegut.lib_tpLink_comms, line 221
		runIn(1, handleCommsError, [data: "httpFailure(timeout)"]) // library marker davegut.lib_tpLink_comms, line 222
	} // library marker davegut.lib_tpLink_comms, line 223
	logDebug(logData) // library marker davegut.lib_tpLink_comms, line 224
	return logData // library marker davegut.lib_tpLink_comms, line 225
} // library marker davegut.lib_tpLink_comms, line 226

//	===== Error Handling ===== // library marker davegut.lib_tpLink_comms, line 228
def handleCommsError(retryReason) { // library marker davegut.lib_tpLink_comms, line 229
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.lib_tpLink_comms, line 230
	if (state.lastCommand != "") { // library marker davegut.lib_tpLink_comms, line 231
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_comms, line 232
		state.errorCount = count // library marker davegut.lib_tpLink_comms, line 233
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_comms, line 234
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_comms, line 235
		Map data = [cmdBody: cmdBody, method: cmdData.method, action:cmdData.action] // library marker davegut.lib_tpLink_comms, line 236
		logData << [count: count, command: cmdData] // library marker davegut.lib_tpLink_comms, line 237
		switch (count) { // library marker davegut.lib_tpLink_comms, line 238
			case 1: // library marker davegut.lib_tpLink_comms, line 239
				setPollInterval("5 min") // library marker davegut.lib_tpLink_comms, line 240
				runIn(1, delayedPassThrough, [data: data]) // library marker davegut.lib_tpLink_comms, line 241
				logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 242
				break // library marker davegut.lib_tpLink_comms, line 243
			case 2: // library marker davegut.lib_tpLink_comms, line 244
				pauseExecution(5000) // library marker davegut.lib_tpLink_comms, line 245
				Map loginData = deviceLogin() // library marker davegut.lib_tpLink_comms, line 246
				logData << [loginStatus: loginData.loginStatus] // library marker davegut.lib_tpLink_comms, line 247
				if (loginData.loginStatus == "OK") { // library marker davegut.lib_tpLink_comms, line 248
					logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 249
					runIn(2, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 250
				} else { // library marker davegut.lib_tpLink_comms, line 251
					logData << parent.checkDevices() // library marker davegut.lib_tpLink_comms, line 252
					logData << [action: "retryCommand"] // library marker davegut.lib_tpLink_comms, line 253
					runIn(15, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 254
				} // library marker davegut.lib_tpLink_comms, line 255
				break // library marker davegut.lib_tpLink_comms, line 256
			case 3: // library marker davegut.lib_tpLink_comms, line 257
				logData << [loginStatus: setCommsError(true)] // library marker davegut.lib_tpLink_comms, line 258
				logWarn(logData) // library marker davegut.lib_tpLink_comms, line 259
				break // library marker davegut.lib_tpLink_comms, line 260

			default: // library marker davegut.lib_tpLink_comms, line 262
				logData << [status: "retriesDisabled"] // library marker davegut.lib_tpLink_comms, line 263
				break // library marker davegut.lib_tpLink_comms, line 264
		} // library marker davegut.lib_tpLink_comms, line 265
	} else { // library marker davegut.lib_tpLink_comms, line 266
		logData << [status: "noCommandToRetry"] // library marker davegut.lib_tpLink_comms, line 267
	} // library marker davegut.lib_tpLink_comms, line 268
	logDebug(logData) // library marker davegut.lib_tpLink_comms, line 269
} // library marker davegut.lib_tpLink_comms, line 270

def delayedPassThrough(data) { // library marker davegut.lib_tpLink_comms, line 272
	asyncSend(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_comms, line 273
} // library marker davegut.lib_tpLink_comms, line 274

def setCommsError(status, errorData = null) { // library marker davegut.lib_tpLink_comms, line 276
	Map logData = [method: "setCommsError", status: status] // library marker davegut.lib_tpLink_comms, line 277
	if (status == false) { // library marker davegut.lib_tpLink_comms, line 278
		updateAttr("commsError", "false") // library marker davegut.lib_tpLink_comms, line 279
		runIn(5, setPollInterval) // library marker davegut.lib_tpLink_comms, line 280
		logData << [commsError: false, pollInterval: pollInterval] // library marker davegut.lib_tpLink_comms, line 281
	} else { // library marker davegut.lib_tpLink_comms, line 282
		logData << [errorData: errorData] // library marker davegut.lib_tpLink_comms, line 283
		logData << [pollInterval: "Temporarily set to 5 minutes"] // library marker davegut.lib_tpLink_comms, line 284
		updateAttr("commsError", "true") // library marker davegut.lib_tpLink_comms, line 285
		logData << [commsError: true] // library marker davegut.lib_tpLink_comms, line 286
	} // library marker davegut.lib_tpLink_comms, line 287
	return logData // library marker davegut.lib_tpLink_comms, line 288
} // library marker davegut.lib_tpLink_comms, line 289

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
	if (respData.status == 200) { // library marker davegut.lib_tpLink_security, line 146
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

// ~~~~~ start include (1379) davegut.lib_tpLink_discovery ~~~~~
library ( // library marker davegut.lib_tpLink_discovery, line 1
	name: "lib_tpLink_discovery", // library marker davegut.lib_tpLink_discovery, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_discovery, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_discovery, line 4
	description: "Common tpLink Smart Discovery Methods", // library marker davegut.lib_tpLink_discovery, line 5
	category: "utilities", // library marker davegut.lib_tpLink_discovery, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_discovery, line 7
) // library marker davegut.lib_tpLink_discovery, line 8

def getSmartLanData(response) { // library marker davegut.lib_tpLink_discovery, line 10
	logDebug("getSmartLanData: responses returned from devices") // library marker davegut.lib_tpLink_discovery, line 11
	List discData = [] // library marker davegut.lib_tpLink_discovery, line 12
	if (response instanceof Map) { // library marker davegut.lib_tpLink_discovery, line 13
		Map devData = getDiscData(response) // library marker davegut.lib_tpLink_discovery, line 14
		if (devData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 15
			discData << devData // library marker davegut.lib_tpLink_discovery, line 16
		} // library marker davegut.lib_tpLink_discovery, line 17
	} else { // library marker davegut.lib_tpLink_discovery, line 18
		response.each { // library marker davegut.lib_tpLink_discovery, line 19
			Map devData = getDiscData(it) // library marker davegut.lib_tpLink_discovery, line 20
			if (devData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 21
				discData << devData // library marker davegut.lib_tpLink_discovery, line 22
			} // library marker davegut.lib_tpLink_discovery, line 23
		} // library marker davegut.lib_tpLink_discovery, line 24
	} // library marker davegut.lib_tpLink_discovery, line 25
	getAllSmartDeviceData(discData) // library marker davegut.lib_tpLink_discovery, line 26
	updateDevices(discData) // library marker davegut.lib_tpLink_discovery, line 27
} // library marker davegut.lib_tpLink_discovery, line 28

def getDiscData(response) { // library marker davegut.lib_tpLink_discovery, line 30
	Map devData = [method: "getDiscData"] // library marker davegut.lib_tpLink_discovery, line 31
	def brand = "KASA" // library marker davegut.lib_tpLink_discovery, line 32
	if (label().contains("tapo")) { brand = "TAPO" } // library marker davegut.lib_tpLink_discovery, line 33
	def status = "INVALID" // library marker davegut.lib_tpLink_discovery, line 34
	try { // library marker davegut.lib_tpLink_discovery, line 35
		def respData = parseLanMessage(response.description) // library marker davegut.lib_tpLink_discovery, line 36
		if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.lib_tpLink_discovery, line 37
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.lib_tpLink_discovery, line 38
			String payloadString = new String(payloadByte) // library marker davegut.lib_tpLink_discovery, line 39
			Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.lib_tpLink_discovery, line 40
			List supported = supportedProducts() // library marker davegut.lib_tpLink_discovery, line 41
			if (supported.contains(payload.device_type)) { // library marker davegut.lib_tpLink_discovery, line 42
				def dni = payload.mac.replaceAll("-", "") // library marker davegut.lib_tpLink_discovery, line 43
				def baseUrl = "http://${payload.ip}:${payload.mgt_encrypt_schm.http_port}/app" // library marker davegut.lib_tpLink_discovery, line 44
				devData << [ // library marker davegut.lib_tpLink_discovery, line 45
					type: payload.device_type, model: payload.device_model, // library marker davegut.lib_tpLink_discovery, line 46
					baseUrl: baseUrl, dni: dni, devId: payload.device_id,  // library marker davegut.lib_tpLink_discovery, line 47
					protocol: payload.mgt_encrypt_schm.encrypt_type, status: "OK"] // library marker davegut.lib_tpLink_discovery, line 48
			} else { // library marker davegut.lib_tpLink_discovery, line 49
				devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.lib_tpLink_discovery, line 50
							status: "INVALID", reason: "Device not supported."] // library marker davegut.lib_tpLink_discovery, line 51
			} // library marker davegut.lib_tpLink_discovery, line 52
		} // library marker davegut.lib_tpLink_discovery, line 53
		logDebug(devData) // library marker davegut.lib_tpLink_discovery, line 54
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 55
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.lib_tpLink_discovery, line 56
		logWarn(devData) // library marker davegut.lib_tpLink_discovery, line 57
	} // library marker davegut.lib_tpLink_discovery, line 58
	return devData // library marker davegut.lib_tpLink_discovery, line 59
} // library marker davegut.lib_tpLink_discovery, line 60

def getAllSmartDeviceData(List discData) { // library marker davegut.lib_tpLink_discovery, line 62
	Map logData = [method: "getAllSmartDeviceData"] // library marker davegut.lib_tpLink_discovery, line 63
	discData.each { Map devData -> // library marker davegut.lib_tpLink_discovery, line 64
		Map cmdResp = getSmartDeviceData(devData.baseUrl, devData.protocol) // library marker davegut.lib_tpLink_discovery, line 65
		if (cmdResp == "ERROR" || cmdResp == null) { // library marker davegut.lib_tpLink_discovery, line 66
			logData << [status: "ERROR", data: "response is ERROR or null"] // library marker davegut.lib_tpLink_discovery, line 67
		} else { // library marker davegut.lib_tpLink_discovery, line 68
			logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 69
			addToDevices(devData, cmdResp.result) // library marker davegut.lib_tpLink_discovery, line 70
		} // library marker davegut.lib_tpLink_discovery, line 71
		pauseExecution(200) // library marker davegut.lib_tpLink_discovery, line 72
	} // library marker davegut.lib_tpLink_discovery, line 73
	if (!logData.toString().contains("ERROR")) { // library marker davegut.lib_tpLink_discovery, line 74
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 75
	} else { // library marker davegut.lib_tpLink_discovery, line 76
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 77
	} // library marker davegut.lib_tpLink_discovery, line 78
	pauseExecution(2000) // library marker davegut.lib_tpLink_discovery, line 79
	state.findingDevices = "done" // library marker davegut.lib_tpLink_discovery, line 80
} // library marker davegut.lib_tpLink_discovery, line 81

def getSmartDeviceData(baseUrl, protocol) { // library marker davegut.lib_tpLink_discovery, line 83
	Map cmdResp = [:] // library marker davegut.lib_tpLink_discovery, line 84
	if (protocol == "KLAP") { // library marker davegut.lib_tpLink_discovery, line 85
		cmdResp = getKlapDeviceData(baseUrl) // library marker davegut.lib_tpLink_discovery, line 86
	} else { // library marker davegut.lib_tpLink_discovery, line 87
		cmdResp = getAesDeviceData(baseUrl) // library marker davegut.lib_tpLink_discovery, line 88
	} // library marker davegut.lib_tpLink_discovery, line 89
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 90
} // library marker davegut.lib_tpLink_discovery, line 91

def getKlapDeviceData(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 93
	Map logData = [method: "getKlapDeviceData", baseUrl: baseUrl] // library marker davegut.lib_tpLink_discovery, line 94
	def cmdResp = "ERROR" // library marker davegut.lib_tpLink_discovery, line 95
	Map sessionData = klapLogin(baseUrl, localHash.decodeBase64()) // library marker davegut.lib_tpLink_discovery, line 96
	logData << [sessionData: sessionData] // library marker davegut.lib_tpLink_discovery, line 97
	if (sessionData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 98
		logData << [sessionDataStatus: sessionData.status] // library marker davegut.lib_tpLink_discovery, line 99
		def cmdStr = JsonOutput.toJson([method: "get_device_info"]).toString() // library marker davegut.lib_tpLink_discovery, line 100
		state.seqNo = sessionData.seqNo // library marker davegut.lib_tpLink_discovery, line 101
		byte[] encKey = sessionData.encKey // library marker davegut.lib_tpLink_discovery, line 102
		byte[] encIv = sessionData.encIv // library marker davegut.lib_tpLink_discovery, line 103
		byte[] encSig = sessionData.encSig // library marker davegut.lib_tpLink_discovery, line 104
		Map encryptedData = klapEncrypt(cmdStr.getBytes(), encKey, encIv, encSig) // library marker davegut.lib_tpLink_discovery, line 105
		def uri = "${baseUrl}/request?seq=${encryptedData.seqNumber}" // library marker davegut.lib_tpLink_discovery, line 106
		Map resp = klapSyncPost(uri, encryptedData.cipherData, sessionData.cookie) // library marker davegut.lib_tpLink_discovery, line 107
		if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 108
			try { // library marker davegut.lib_tpLink_discovery, line 109
				byte[] cipherResponse = resp.data[32..-1] // library marker davegut.lib_tpLink_discovery, line 110
				def clearResp =  klapDecrypt(cipherResponse, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 111
				cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 112
				logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 113
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 114
				logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 115
			} // library marker davegut.lib_tpLink_discovery, line 116
		} else { // library marker davegut.lib_tpLink_discovery, line 117
			logData << [status: "postError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 118
		} // library marker davegut.lib_tpLink_discovery, line 119
	} else { // library marker davegut.lib_tpLink_discovery, line 120
		logData << [respStatus: "FAILED", reason: "Login process failure.  Check credentials."] // library marker davegut.lib_tpLink_discovery, line 121
	} // library marker davegut.lib_tpLink_discovery, line 122

	if (logData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 124
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 125
	} else { // library marker davegut.lib_tpLink_discovery, line 126
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 127
	} // library marker davegut.lib_tpLink_discovery, line 128
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 129
} // library marker davegut.lib_tpLink_discovery, line 130

def getAesDeviceData(baseUrl) { // library marker davegut.lib_tpLink_discovery, line 132
	Map logData = [method: "getAesDeviceData", baseUrl: baseUrl] // library marker davegut.lib_tpLink_discovery, line 133
	def cmdResp = "ERROR" // library marker davegut.lib_tpLink_discovery, line 134
	Map sessionData = aesLogin(baseUrl, encPassword, encUsername) // library marker davegut.lib_tpLink_discovery, line 135
	if (sessionData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 136
		byte[] encKey = sessionData.encKey // library marker davegut.lib_tpLink_discovery, line 137
		byte[] encIv = sessionData.encIv // library marker davegut.lib_tpLink_discovery, line 138
		def cmdStr = JsonOutput.toJson([method: "get_device_info"]).toString() // library marker davegut.lib_tpLink_discovery, line 139
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_discovery, line 140
					   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.lib_tpLink_discovery, line 141
		def uri = "${baseUrl}?token=${sessionData.token}" // library marker davegut.lib_tpLink_discovery, line 142
		Map resp = aesSyncPost(uri, reqBody, sessionData.cookie) // library marker davegut.lib_tpLink_discovery, line 143
		if (resp.status == 200) { // library marker davegut.lib_tpLink_discovery, line 144
			try { // library marker davegut.lib_tpLink_discovery, line 145
				def clearResp = aesDecrypt(resp.data.result.response, encKey, encIv) // library marker davegut.lib_tpLink_discovery, line 146
				cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.lib_tpLink_discovery, line 147
				logData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 148
			} catch (err) { // library marker davegut.lib_tpLink_discovery, line 149
				logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_discovery, line 150
			} // library marker davegut.lib_tpLink_discovery, line 151
		} else { // library marker davegut.lib_tpLink_discovery, line 152
			logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_discovery, line 153
		} // library marker davegut.lib_tpLink_discovery, line 154
	} else { // library marker davegut.lib_tpLink_discovery, line 155
		logData << [respStatus: "FAILED", reason: "Check Credentials"] // library marker davegut.lib_tpLink_discovery, line 156
	} // library marker davegut.lib_tpLink_discovery, line 157
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 158
		logDebug(logData) // library marker davegut.lib_tpLink_discovery, line 159
	} else { // library marker davegut.lib_tpLink_discovery, line 160
		logWarn(logData) // library marker davegut.lib_tpLink_discovery, line 161
	} // library marker davegut.lib_tpLink_discovery, line 162
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 163
} // library marker davegut.lib_tpLink_discovery, line 164

def addToDevices(devData, cmdResp) { // library marker davegut.lib_tpLink_discovery, line 166
	String dni = devData.dni // library marker davegut.lib_tpLink_discovery, line 167
	Map deviceData = [:] // library marker davegut.lib_tpLink_discovery, line 168
	String deviceType = devData.type // library marker davegut.lib_tpLink_discovery, line 169
	byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.lib_tpLink_discovery, line 170
	def alias = new String(plainBytes) // library marker davegut.lib_tpLink_discovery, line 171
	if (alias == "") { // library marker davegut.lib_tpLink_discovery, line 172
		alias = devData.model // library marker davegut.lib_tpLink_discovery, line 173
	} // library marker davegut.lib_tpLink_discovery, line 174
	deviceData << [protocol: devData.protocol] // library marker davegut.lib_tpLink_discovery, line 175
	deviceData << [alias: alias] // library marker davegut.lib_tpLink_discovery, line 176
	deviceData << [model: devData.model] // library marker davegut.lib_tpLink_discovery, line 177
	deviceData << [baseUrl: devData.baseUrl] // library marker davegut.lib_tpLink_discovery, line 178
	String capability = "newType" // library marker davegut.lib_tpLink_discovery, line 179
	String feature // library marker davegut.lib_tpLink_discovery, line 180
	if (deviceType.contains("BULB")) { // library marker davegut.lib_tpLink_discovery, line 181
		capability = "bulb_dimmer" // library marker davegut.lib_tpLink_discovery, line 182
		if (cmdResp.color_temp_range) { // library marker davegut.lib_tpLink_discovery, line 183
			deviceData << [ctLow: cmdResp.color_temp_range[0]] // library marker davegut.lib_tpLink_discovery, line 184
			deviceData << [ctHigh: cmdResp.color_temp_range[1]] // library marker davegut.lib_tpLink_discovery, line 185
			if (cmdResp.color_temp_range[0] < cmdResp.color_temp_range[1]) { // library marker davegut.lib_tpLink_discovery, line 186
				capability = "bulb_color" // library marker davegut.lib_tpLink_discovery, line 187
			} else if (cmdResp.lighting_effect) { // library marker davegut.lib_tpLink_discovery, line 188
				capability = "bulb_lightStrip" // library marker davegut.lib_tpLink_discovery, line 189
			} // library marker davegut.lib_tpLink_discovery, line 190
		} // library marker davegut.lib_tpLink_discovery, line 191
	} else if (deviceType.contains("SWITCH") || deviceType.contains("PLUG")) { // library marker davegut.lib_tpLink_discovery, line 192
		capability = "plug" // library marker davegut.lib_tpLink_discovery, line 193
		if (cmdResp.brightness) { // library marker davegut.lib_tpLink_discovery, line 194
			capability = "plug_dimmer" // library marker davegut.lib_tpLink_discovery, line 195
		} // library marker davegut.lib_tpLink_discovery, line 196
		if (cmdResp.power_protection_status) { // library marker davegut.lib_tpLink_discovery, line 197
			capability = "plug_em" // library marker davegut.lib_tpLink_discovery, line 198
		} // library marker davegut.lib_tpLink_discovery, line 199
		if (!cmdResp.default_states) {		// parent plug does not have default_states // library marker davegut.lib_tpLink_discovery, line 200
			capability = "plug_multi" // library marker davegut.lib_tpLink_discovery, line 201
		} // library marker davegut.lib_tpLink_discovery, line 202
	} else if (deviceType.contains("HUB")) { // library marker davegut.lib_tpLink_discovery, line 203
		capability = "hub" // library marker davegut.lib_tpLink_discovery, line 204
	} // library marker davegut.lib_tpLink_discovery, line 205
	deviceData << [driver: "tpLink_${capability}"] // library marker davegut.lib_tpLink_discovery, line 206
	deviceData << [capability: capability] // library marker davegut.lib_tpLink_discovery, line 207
	state.devices << ["${dni}": deviceData] // library marker davegut.lib_tpLink_discovery, line 208
	logInfo("addToDevices <b>${deviceData.alias}</b>: [${dni}: ${deviceData}]") // library marker davegut.lib_tpLink_discovery, line 209
} // library marker davegut.lib_tpLink_discovery, line 210

// ~~~~~ end include (1379) davegut.lib_tpLink_discovery ~~~~~

// ~~~~~ start include (1339) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def listAttributes() { // library marker davegut.Logging, line 10
	def attrData = device.getCurrentStates() // library marker davegut.Logging, line 11
	Map attrs = [:] // library marker davegut.Logging, line 12
	attrData.each { // library marker davegut.Logging, line 13
		attrs << ["${it.name}": it.value] // library marker davegut.Logging, line 14
	} // library marker davegut.Logging, line 15
	return attrs // library marker davegut.Logging, line 16
} // library marker davegut.Logging, line 17

def setLogsOff() { // library marker davegut.Logging, line 19
	def logData = [logEnable: logEnable] // library marker davegut.Logging, line 20
	if (logEnable) { // library marker davegut.Logging, line 21
		runIn(1800, debugLogOff) // library marker davegut.Logging, line 22
		logData << [debugLogOff: "scheduled"] // library marker davegut.Logging, line 23
	} // library marker davegut.Logging, line 24
	return logData // library marker davegut.Logging, line 25
} // library marker davegut.Logging, line 26

def logTrace(msg){ // library marker davegut.Logging, line 28
	log.trace "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 29
} // library marker davegut.Logging, line 30

def logInfo(msg) {  // library marker davegut.Logging, line 32
	if (textEnable || infoLog) { // library marker davegut.Logging, line 33
		log.info "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 34
	} // library marker davegut.Logging, line 35
} // library marker davegut.Logging, line 36

def debugLogOff() { // library marker davegut.Logging, line 38
	device.updateSetting("logEnable", [type:"bool", value: false]) // library marker davegut.Logging, line 39
	logInfo("debugLogOff") // library marker davegut.Logging, line 40
} // library marker davegut.Logging, line 41

def logDebug(msg) { // library marker davegut.Logging, line 43
	if (logEnable || debugLog) { // library marker davegut.Logging, line 44
		log.debug "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 45
	} // library marker davegut.Logging, line 46
} // library marker davegut.Logging, line 47

def logWarn(msg) { // library marker davegut.Logging, line 49
	log.warn "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 50
} // library marker davegut.Logging, line 51

def logError(msg) { // library marker davegut.Logging, line 53
	log.error "${label()}-${version()}: ${msg}" // library marker davegut.Logging, line 54
} // library marker davegut.Logging, line 55

// ~~~~~ end include (1339) davegut.Logging ~~~~~
