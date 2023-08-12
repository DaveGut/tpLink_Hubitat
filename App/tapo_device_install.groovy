/*	tpLinkSmart Device Installation Application
	Copyright Dave Gutheinz
===================================================================================================*/
def appName() { return "tapo_device_install" }
def appVersion() { return "1.1" }
def nameSpace() { return "davegut" }
def gitPath() { return "DaveGut/tpLink_Hubitat/main/App/" }
import groovy.json.JsonSlurper
import java.security.MessageDigest

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
	importUrl: "https://raw.githubusercontent.com/${gitPath()}${appName()}.groovy"
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
	runEvery15Minutes(updateIps)
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
	if (!state.devices) { state.devices = [:] }
	state.findingDevices = "ready"
	unschedule()
	if (!state.encCreds) { state.encCreds = [encUsername: "notSet", encPassword: "notSet"] }
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
			def twoFactor = "<b>The installations function may not work if " +
				"two-factor authentication is enabled in the Tapo phone app.</b>"
			paragraph twoFactor
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
			
			if (!encUsername || !encPassword) {
				paragraph "<b>Credentials require attention</b>"
			}
			href "enterCredentialsPage",
				title: "<b>Enter/Update tpLink Credentials</b>",
				description: "Credentials are used by app and each Tapo devices during periodic login."
			
			if (encUsername && encPassword) {
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

//	===== Enter Credentials =====
def enterCredentialsPage() {
	logInfo("enterCredentialsPage")
	return dynamicPage (name: "enterCredentialsPage", 
    					title: "Enter TP-Link (Tapo) Credentials",
						nextPage: startPage,
                        install: false) {
		section() {
			paragraph "Current Encoded Credentials: ${state.encCreds} \n\r"
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
	Map logData = [:]
	String encUsername = mdEncode(userName).bytes.encodeBase64().toString()
	app?.updateSetting("encUsername", [type: "password", value: encUsername])
	Map credentials = [encUsername: encUsername]
	String encPassword = userPassword.bytes.encodeBase64().toString()
	app?.updateSetting("encPassword", [type: "password", value: encPassword])
	credentials << [encPassword: encPassword]
	logData << [creds: credentials]
	if (encUsername != state.encCreds.encUsername ||
		encPassword != state.encCreds.encPassword) {
		runIn(2, updateDeviceData)
		logData << [status: "NewCredentials", action: "updatingChildCredentials"]
	} else {
		logData << [status: "NoCredentialChanges"]
	}
	state.encCreds = credentials
	logInfo("processCredentials: ${logData}")
	return startPage()
}

private String mdEncode(String message) {
	MessageDigest md = MessageDigest.getInstance("SHA-1")
	md.update(message.getBytes())
	byte[] digest = md.digest()
	return digest.encodeHex()
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
		logInfo("addDevicesPage: <b>FINDING DEVICES</b>.  ${findDevices("getSmartLanData", 10)}")
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
		runIn(35, updateDeviceData)
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
	logInfo("sendLanCmd: ${data}")
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

//	App interface to Lib code
def parseSmartDeviceData(devData) {
	def dni = devData.mac.replaceAll("-", "")
	Map deviceData = [dni: dni]
	String deviceType = devData.type
	byte[] plainBytes = devData.nickname.decodeBase64()
	String alias = new String(plainBytes)
	deviceData << [alias: alias]
	deviceData << [model: devData.model]
	deviceData << [ip: devData.ip]
	deviceData << [deviceId: devData.device_id]

	String capability = "newType"
	String feature
	if (deviceType == "SMART.TAPOBULB") {
		capability = "bulb_dimmer"
		if (devData.color_temp_range) {
			deviceData << [ctLow: devData.color_temp_range[0]]
			deviceData << [ctHigh: devData.color_temp_range[1]]
			if (devData.color_temp_range[0] < devData.color_temp_range[1]) {
				capability = "bulb_color"
			} else if (devData.lighting_effect) {
				capability = "bulb_lightStrip"
			}
		}
	} else if (deviceType == "SMART.TAPOSWITCH" || deviceType == "SMART.TAPOPLUG") {
		capability = "plug"
		if (devData.brightness) {
			capability = "plug_dimmer"
		}
		if (devData.power_protection_status) {
			capability = "plug_em"
		}
	} else if (deviceType == "SMART.TAPOHUB") {
		capability = "hub"
	}
	String driver = "tpLink_${capability}"
	deviceData << [driver: driver]
	deviceData << [capability: capability]

	state.devices << ["${dni}": deviceData]
	logDebug("parseDeviceData: [${dni}: ${deviceData}]")
}

def updateDeviceData() {
	def logData = [:]
	if (state.devices != [:]) {
		Map devices = state.devices
		devices.each {
			def child = getChildDevice(it.key)
			if (child) {
				Map childData = [:]
				def deviceIp = child.getDataValue("deviceIp")
				def devEncUsername = child.getDataValue("encUsername")
				def devEncPassword = child.getDataValue("encPassword")
				if (deviceIp == it.value.ip &&
					devEncPassword == encPassword &&
					devEncUsername == encUsername) {
					childData << [Status: "No Changes"]
				} else {
					child.updateDataValue("deviceIp", it.value.ip)
					child.updateDataValue("encPassword", encPassword)
					child.updateDataValue("encUsername", encUsername)
					childData << [status: "Updated", LOGIN: child.deviceLogin()]
				}
				logData << [child: childData]
			} else {
				logData << ["${it.key}": "notChild"]
			}
		}
	} else {
		logData << [status: "noChanges", data: "state.devices is null"]
	}
	logInfo("updateDeviceData: ${logData}")
	return logData
}

//	===== Add wifi devices to Hubitat =====
def addDevices() {
	Map addedDevices = [:]
	Map failedAdds = [:]
	def devices = state.devices
	selectedAddDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = devices.find { it.value.dni == dni }
			def alias = device.value.alias.replaceAll("[\u201C\u201D]", "\"").replaceAll("[\u2018\u2019]", "'").replaceAll("[^\\p{ASCII}]", "")
			Map deviceData = [deviceIP: device.value.ip]
			deviceData << [deviceId: device.value.deviceId]
			deviceData << [encUsername: encUsername]
			deviceData << [encPassword: encPassword]
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
				addedDevices << ["${device.key}": [label: alias, ip: device.value.ip]]
			} catch (error) {
				log.debug error
				status = "ERROR"
				failedAdds << ["${device.key}": [label: alias, driver: device.value.driver]]
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

//	===== Remove Devices =====
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

//	===== Methods to check and update Children IPs =====
def updateIps() {
	Map logData = [action: "checking and updating IP Addresses"]
	logData << [findDevices: findDevices("distSmartFindResp", 5)]
	logDebug("updateIp: ${logData}")
	return logData
}

def distSmartFindResp(response) {
	Map logData = [:]
	Map ipList = [:]
	def brand = "KASA"
	if (appName() == "tapo_device_install") { brand = "TAPO" }
	if (response instanceof Map) {
		def resp = parseLanMessage(response.description)
		if (resp.type == "LAN_TYPE_UDPCLIENT") {
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload.drop(32)) 
			String payloadString = new String(payloadByte)
			Map payload = new JsonSlurper().parseText(payloadString).result
			if (payloadData.device_type.contains(brand)) {
				def dni = payloadData.mac.replaceAll("-", "")
				def devIp = payloadData.ip
				ipList << ["${dni}": devIp]
			}
		}
	} else {
		response.each {
			def resp = parseLanMessage(it.description)
			if (resp.type == "LAN_TYPE_UDPCLIENT") {
				byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload.drop(32)) 
				String payloadString = new String(payloadByte)
				Map payload = new JsonSlurper().parseText(payloadString).result
				if (payload.device_type.contains(brand)) {
					def dni = payload.mac.replaceAll("-", "")
					def devIp = payload.ip
					ipList << ["${dni}": devIp]
				}
			}
		}
	}
	ipList.each {
		def child = getChildDevice(it.key)
		if (child) {
			if (child.getDataValue("deviceIP") == it.value) {
				logData << ["${child}_IP": "noChange"]
			} else {
				child.updateDataValue("deviceIP", it.value)
				logData << ["${child}_IP": it.value]
			}
		} else {
			logData << ["${it.key}": "notChild"]
		}
	}
	logDebug("distSmartFindResp: ${logData}")
}

//	===== Logging Methods =====
def debugOff() { app.updateSetting("debugLog", false) }
def logTrace(msg) { log.trace "TapoInst-${appVersion()}: ${msg}" }
def logDebug(msg){
	if(debugLog == true) { log.debug "TapoInst-${appVersion()}: ${msg}" }
}
def logInfo(msg) { log.info "TapoInst-${appVersion()}: ${msg}" }
def logWarn(msg) { log.warn "TapoInst-${appVersion()}: ${msg}" }
def logError(msg) { log.error "TapoInst-${appVersion()}: ${msg}" }





// ~~~~~ start include (1327) davegut.lib_tpLink_comms ~~~~~
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

def asyncPassthrough(cmdBody, method, action) { // library marker davegut.lib_tpLink_comms, line 21
	if (devIp == null) { devIp = getDataValue("deviceIP") }	//	used for Kasa Compatibility // library marker davegut.lib_tpLink_comms, line 22
	Map cmdData = [cmdBody: cmdBody, method: method, action: action] // library marker davegut.lib_tpLink_comms, line 23
	state.lastCmd = cmdData // library marker davegut.lib_tpLink_comms, line 24
	logDebug("asyncPassthrough: ${cmdData}") // library marker davegut.lib_tpLink_comms, line 25
	def uri = "http://${getDataValue("deviceIP")}/app?token=${getDataValue("deviceToken")}" // library marker davegut.lib_tpLink_comms, line 26
	Map reqBody = createReqBody(cmdBody) // library marker davegut.lib_tpLink_comms, line 27
	asyncPost(uri, reqBody, action, getDataValue("deviceCookie"), method) // library marker davegut.lib_tpLink_comms, line 28
} // library marker davegut.lib_tpLink_comms, line 29

def syncPassthrough(cmdBody) { // library marker davegut.lib_tpLink_comms, line 31
	if (devIp == null) { devIp = getDataValue("deviceIP") }	//	used for Kasa Compatibility // library marker davegut.lib_tpLink_comms, line 32
	Map logData = [cmdBody: cmdBody] // library marker davegut.lib_tpLink_comms, line 33
	def uri = "http://${getDataValue("deviceIP")}/app?token=${getDataValue("deviceToken")}" // library marker davegut.lib_tpLink_comms, line 34
	Map reqBody = createReqBody(cmdBody) // library marker davegut.lib_tpLink_comms, line 35
	def resp = syncPost(uri, reqBody, getDataValue("deviceCookie")) // library marker davegut.lib_tpLink_comms, line 36
	def cmdResp = "ERROR" // library marker davegut.lib_tpLink_comms, line 37
	if (resp.status == "OK") { // library marker davegut.lib_tpLink_comms, line 38
		try { // library marker davegut.lib_tpLink_comms, line 39
			cmdResp = new JsonSlurper().parseText(decrypt(resp.resp.data.result.response)) // library marker davegut.lib_tpLink_comms, line 40
			logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 41
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 42
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 43
		} // library marker davegut.lib_tpLink_comms, line 44
	} else { // library marker davegut.lib_tpLink_comms, line 45
		logData << [status: "postJsonError", postJsonData: resp] // library marker davegut.lib_tpLink_comms, line 46
	} // library marker davegut.lib_tpLink_comms, line 47
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 48
		logDebug("syncPassthrough: ${logData}") // library marker davegut.lib_tpLink_comms, line 49
	} else { // library marker davegut.lib_tpLink_comms, line 50
		logWarn("syncPassthrough: ${logData}") // library marker davegut.lib_tpLink_comms, line 51
	} // library marker davegut.lib_tpLink_comms, line 52
	return cmdResp // library marker davegut.lib_tpLink_comms, line 53
} // library marker davegut.lib_tpLink_comms, line 54

def createReqBody(cmdBody) { // library marker davegut.lib_tpLink_comms, line 56
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_comms, line 57
	Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_comms, line 58
				   params: [request: encrypt(cmdStr)]] // library marker davegut.lib_tpLink_comms, line 59
	return reqBody // library marker davegut.lib_tpLink_comms, line 60
} // library marker davegut.lib_tpLink_comms, line 61

//	===== Sync comms for device update ===== // library marker davegut.lib_tpLink_comms, line 63
def syncPost(uri, reqBody, cookie=null) { // library marker davegut.lib_tpLink_comms, line 64
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 65
		uri: uri, // library marker davegut.lib_tpLink_comms, line 66
		headers: [ // library marker davegut.lib_tpLink_comms, line 67
			Cookie: cookie // library marker davegut.lib_tpLink_comms, line 68
		], // library marker davegut.lib_tpLink_comms, line 69
		body : new JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_comms, line 70
	] // library marker davegut.lib_tpLink_comms, line 71
	logDebug("syncPost: [cmdParams: ${reqParams}]") // library marker davegut.lib_tpLink_comms, line 72
	Map respData = [:] // library marker davegut.lib_tpLink_comms, line 73
	try { // library marker davegut.lib_tpLink_comms, line 74
		httpPostJson(reqParams) {resp -> // library marker davegut.lib_tpLink_comms, line 75
			if (resp.status == 200 && resp.data.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 76
				respData << [status: "OK", resp: resp] // library marker davegut.lib_tpLink_comms, line 77
			} else { // library marker davegut.lib_tpLink_comms, line 78
				respData << [status: "lanDataError", respStatus: resp.status,  // library marker davegut.lib_tpLink_comms, line 79
					errorCode: resp.data.error_code] // library marker davegut.lib_tpLink_comms, line 80
			} // library marker davegut.lib_tpLink_comms, line 81
		} // library marker davegut.lib_tpLink_comms, line 82
	} catch (err) { // library marker davegut.lib_tpLink_comms, line 83
		respData << [status: "HTTP Failed", data: err] // library marker davegut.lib_tpLink_comms, line 84
	} // library marker davegut.lib_tpLink_comms, line 85
	return respData // library marker davegut.lib_tpLink_comms, line 86
} // library marker davegut.lib_tpLink_comms, line 87

def asyncPost(uri, reqBody, parseMethod, cookie=null, reqData=null) { // library marker davegut.lib_tpLink_comms, line 89
	Map logData = [:] // library marker davegut.lib_tpLink_comms, line 90
	def reqParams = [ // library marker davegut.lib_tpLink_comms, line 91
		uri: uri, // library marker davegut.lib_tpLink_comms, line 92
		requestContentType: 'application/json', // library marker davegut.lib_tpLink_comms, line 93
		contentType: 'application/json', // library marker davegut.lib_tpLink_comms, line 94
		headers: [ // library marker davegut.lib_tpLink_comms, line 95
			Cookie: cookie // library marker davegut.lib_tpLink_comms, line 96
		], // library marker davegut.lib_tpLink_comms, line 97
		timeout: 4, // library marker davegut.lib_tpLink_comms, line 98
		body : new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.lib_tpLink_comms, line 99
	] // library marker davegut.lib_tpLink_comms, line 100
	try { // library marker davegut.lib_tpLink_comms, line 101
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.lib_tpLink_comms, line 102
		logData << [status: "OK"] // library marker davegut.lib_tpLink_comms, line 103
	} catch (e) { // library marker davegut.lib_tpLink_comms, line 104
		logData << [status: e, reqParams: reqParams] // library marker davegut.lib_tpLink_comms, line 105
	} // library marker davegut.lib_tpLink_comms, line 106
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 107
		logDebug("asyncPost: ${logData}") // library marker davegut.lib_tpLink_comms, line 108
	} else { // library marker davegut.lib_tpLink_comms, line 109
		logWarn("asyncPost: ${logData}") // library marker davegut.lib_tpLink_comms, line 110
		handleCommsError() // library marker davegut.lib_tpLink_comms, line 111
	} // library marker davegut.lib_tpLink_comms, line 112
} // library marker davegut.lib_tpLink_comms, line 113

def parseData(resp) { // library marker davegut.lib_tpLink_comms, line 115
	def logData = [:] // library marker davegut.lib_tpLink_comms, line 116
	if (resp.status == 200 && resp.json.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 117
		def cmdResp // library marker davegut.lib_tpLink_comms, line 118
		try { // library marker davegut.lib_tpLink_comms, line 119
			cmdResp = new JsonSlurper().parseText(decrypt(resp.json.result.response)) // library marker davegut.lib_tpLink_comms, line 120
			setCommsError(false) // library marker davegut.lib_tpLink_comms, line 121
		} catch (err) { // library marker davegut.lib_tpLink_comms, line 122
			logData << [status: "cryptoError", error: "Error decrypting response", data: err] // library marker davegut.lib_tpLink_comms, line 123
		} // library marker davegut.lib_tpLink_comms, line 124
		if (cmdResp != null && cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_comms, line 125
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 126
		} else { // library marker davegut.lib_tpLink_comms, line 127
			logData << [status: "deviceDataError", cmdResp: cmdResp] // library marker davegut.lib_tpLink_comms, line 128
		} // library marker davegut.lib_tpLink_comms, line 129
	} else { // library marker davegut.lib_tpLink_comms, line 130
		logData << [status: "lanDataError"] // library marker davegut.lib_tpLink_comms, line 131
	} // library marker davegut.lib_tpLink_comms, line 132
	if (logData.status == "OK") { // library marker davegut.lib_tpLink_comms, line 133
		logDebug("parseData: ${logData}") // library marker davegut.lib_tpLink_comms, line 134
	} else { // library marker davegut.lib_tpLink_comms, line 135
		logWarn("parseData: ${logData}") // library marker davegut.lib_tpLink_comms, line 136
		handleCommsError() // library marker davegut.lib_tpLink_comms, line 137
	} // library marker davegut.lib_tpLink_comms, line 138
	return logData // library marker davegut.lib_tpLink_comms, line 139
} // library marker davegut.lib_tpLink_comms, line 140

def handleCommsError() { // library marker davegut.lib_tpLink_comms, line 142
	Map logData = [:] // library marker davegut.lib_tpLink_comms, line 143
	if (state.lastCommand != "") { // library marker davegut.lib_tpLink_comms, line 144
		def count = state.errorCount + 1 // library marker davegut.lib_tpLink_comms, line 145
		state.errorCount = count // library marker davegut.lib_tpLink_comms, line 146
		def cmdData = new JSONObject(state.lastCmd) // library marker davegut.lib_tpLink_comms, line 147
		def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.lib_tpLink_comms, line 148
		logData << [count: count, command: cmdData] // library marker davegut.lib_tpLink_comms, line 149
		switch (count) { // library marker davegut.lib_tpLink_comms, line 150
			case 1: // library marker davegut.lib_tpLink_comms, line 151
				asyncPassthrough(cmdBody, cmdData.method, cmdData.action) // library marker davegut.lib_tpLink_comms, line 152
				logData << [status: "commandRetry"] // library marker davegut.lib_tpLink_comms, line 153
				logDebug("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 154
				break // library marker davegut.lib_tpLink_comms, line 155
			case 2: // library marker davegut.lib_tpLink_comms, line 156
				logData << [deviceLogin: deviceLogin()] // library marker davegut.lib_tpLink_comms, line 157
				Map data = [cmdBody: cmdBody, method: cmdData.method, action:cmdData.action] // library marker davegut.lib_tpLink_comms, line 158
				runIn(2, delayedPassThrough, [data:data]) // library marker davegut.lib_tpLink_comms, line 159
				logData << [status: "newLogin and commandRetry"] // library marker davegut.lib_tpLink_comms, line 160
				logWarn("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 161
				break // library marker davegut.lib_tpLink_comms, line 162
			case 3: // library marker davegut.lib_tpLink_comms, line 163
				logData << [setCommsError: setCommsError(true), status: "retriesDisabled"] // library marker davegut.lib_tpLink_comms, line 164
				logError("handleCommsError: ${logData}") // library marker davegut.lib_tpLink_comms, line 165
				break // library marker davegut.lib_tpLink_comms, line 166
			default: // library marker davegut.lib_tpLink_comms, line 167
				break // library marker davegut.lib_tpLink_comms, line 168
		} // library marker davegut.lib_tpLink_comms, line 169
	} // library marker davegut.lib_tpLink_comms, line 170
} // library marker davegut.lib_tpLink_comms, line 171

def delayedPassThrough(data) { // library marker davegut.lib_tpLink_comms, line 173
	asyncPassthrough(data.cmdBody, data.method, data.action) // library marker davegut.lib_tpLink_comms, line 174
} // library marker davegut.lib_tpLink_comms, line 175

def setCommsError(status) { // library marker davegut.lib_tpLink_comms, line 177
	if (!status) { // library marker davegut.lib_tpLink_comms, line 178
		updateAttr("commsError", false) // library marker davegut.lib_tpLink_comms, line 179
		state.errorCount = 0 // library marker davegut.lib_tpLink_comms, line 180
	} else { // library marker davegut.lib_tpLink_comms, line 181
		updateAttr("commsError", true) // library marker davegut.lib_tpLink_comms, line 182
		return "commsErrorSet" // library marker davegut.lib_tpLink_comms, line 183
	} // library marker davegut.lib_tpLink_comms, line 184
} // library marker davegut.lib_tpLink_comms, line 185

// ~~~~~ end include (1327) davegut.lib_tpLink_comms ~~~~~

// ~~~~~ start include (1337) davegut.lib_tpLink_security ~~~~~
library ( // library marker davegut.lib_tpLink_security, line 1
	name: "lib_tpLink_security", // library marker davegut.lib_tpLink_security, line 2
	namespace: "davegut", // library marker davegut.lib_tpLink_security, line 3
	author: "Dave Gutheinz", // library marker davegut.lib_tpLink_security, line 4
	description: "tpLink RSA and AES security measures", // library marker davegut.lib_tpLink_security, line 5
	category: "utilities", // library marker davegut.lib_tpLink_security, line 6
	documentationLink: "" // library marker davegut.lib_tpLink_security, line 7
) // library marker davegut.lib_tpLink_security, line 8
import groovy.json.JsonSlurper // library marker davegut.lib_tpLink_security, line 9
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.lib_tpLink_security, line 10
import javax.crypto.spec.SecretKeySpec // library marker davegut.lib_tpLink_security, line 11
import javax.crypto.spec.IvParameterSpec // library marker davegut.lib_tpLink_security, line 12
import javax.crypto.Cipher // library marker davegut.lib_tpLink_security, line 13
import java.security.KeyFactory // library marker davegut.lib_tpLink_security, line 14

def securityPreferences() { // library marker davegut.lib_tpLink_security, line 16
	input ("aesKey", "password", title: "Storage for the AES Key") // library marker davegut.lib_tpLink_security, line 17
} // library marker davegut.lib_tpLink_security, line 18

//	===== Device Login Core ===== // library marker davegut.lib_tpLink_security, line 20
def handshake(devIp) { // library marker davegut.lib_tpLink_security, line 21
	def rsaKeys = getRsaKeys() // library marker davegut.lib_tpLink_security, line 22
	Map handshakeData = [method: "handshakeData", rsaKeys: rsaKeys.keyNo] // library marker davegut.lib_tpLink_security, line 23
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKeys.public}-----END PUBLIC KEY-----\n" // library marker davegut.lib_tpLink_security, line 24
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]] // library marker davegut.lib_tpLink_security, line 25
	def uri = "http://${devIp}/app" // library marker davegut.lib_tpLink_security, line 26
	def respData = syncPost(uri, cmdBody) // library marker davegut.lib_tpLink_security, line 27
	if (respData.status == "OK") { // library marker davegut.lib_tpLink_security, line 28
		String deviceKey = respData.resp.data.result.key // library marker davegut.lib_tpLink_security, line 29
		try { // library marker davegut.lib_tpLink_security, line 30
			def cookieHeader = respData.resp.headers["set-cookie"].toString() // library marker davegut.lib_tpLink_security, line 31
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.lib_tpLink_security, line 32
			handshakeData << [cookie: cookie] // library marker davegut.lib_tpLink_security, line 33
		} catch (err) { // library marker davegut.lib_tpLink_security, line 34
			handshakeData << [respStatus: "FAILED", check: "respData.headers", error: err] // library marker davegut.lib_tpLink_security, line 35
		} // library marker davegut.lib_tpLink_security, line 36
		def aesArray = readDeviceKey(deviceKey, rsaKeys.private) // library marker davegut.lib_tpLink_security, line 37
		handshakeData << [aesKey: aesArray] // library marker davegut.lib_tpLink_security, line 38
		if (aesArray == "ERROR") { // library marker davegut.lib_tpLink_security, line 39
			handshakeData << [respStatus: "FAILED", check: "privateKey"] // library marker davegut.lib_tpLink_security, line 40
		} else { // library marker davegut.lib_tpLink_security, line 41
			handshakeData << [respStatus: "OK"] // library marker davegut.lib_tpLink_security, line 42
		} // library marker davegut.lib_tpLink_security, line 43
	} else { // library marker davegut.lib_tpLink_security, line 44
		handshakeData << [respStatus: "FAILED", check: "pubPem. devIp", respData: respData] // library marker davegut.lib_tpLink_security, line 45
	} // library marker davegut.lib_tpLink_security, line 46
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 47
		logDebug("handshake: ${handshakeData}") // library marker davegut.lib_tpLink_security, line 48
	} else { // library marker davegut.lib_tpLink_security, line 49
		logWarn("handshake: ${handshakeData}") // library marker davegut.lib_tpLink_security, line 50
	} // library marker davegut.lib_tpLink_security, line 51
	return handshakeData // library marker davegut.lib_tpLink_security, line 52
} // library marker davegut.lib_tpLink_security, line 53

def readDeviceKey(deviceKey, privateKey) { // library marker davegut.lib_tpLink_security, line 55
	def response = "ERROR" // library marker davegut.lib_tpLink_security, line 56
	def logData = [:] // library marker davegut.lib_tpLink_security, line 57
	try { // library marker davegut.lib_tpLink_security, line 58
		byte[] privateKeyBytes = privateKey.decodeBase64() // library marker davegut.lib_tpLink_security, line 59
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64() // library marker davegut.lib_tpLink_security, line 60
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding") // library marker davegut.lib_tpLink_security, line 61
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes))) // library marker davegut.lib_tpLink_security, line 62
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes) // library marker davegut.lib_tpLink_security, line 63
		response = cryptoArray // library marker davegut.lib_tpLink_security, line 64
		logData << [cryptoArray: "REDACTED for logs", status: "OK"] // library marker davegut.lib_tpLink_security, line 65
		logDebug("readDeviceKey: ${logData}") // library marker davegut.lib_tpLink_security, line 66
	} catch (err) { // library marker davegut.lib_tpLink_security, line 67
		logData << [status: "READ ERROR", data: err] // library marker davegut.lib_tpLink_security, line 68
		logWarn("readDeviceKey: ${logData}") // library marker davegut.lib_tpLink_security, line 69
	} // library marker davegut.lib_tpLink_security, line 70
	return response // library marker davegut.lib_tpLink_security, line 71
} // library marker davegut.lib_tpLink_security, line 72

def loginDevice(cookie, cryptoArray, credentials, devIp) { // library marker davegut.lib_tpLink_security, line 74
	Map tokenData = [method: "loginDevice"] // library marker davegut.lib_tpLink_security, line 75
	def uri = "http://${devIp}/app" // library marker davegut.lib_tpLink_security, line 76
	Map cmdBody = [method: "login_device", // library marker davegut.lib_tpLink_security, line 77
				   params: [password: credentials.encPassword, // library marker davegut.lib_tpLink_security, line 78
							username: credentials.encUsername], // library marker davegut.lib_tpLink_security, line 79
				   requestTimeMils: 0] // library marker davegut.lib_tpLink_security, line 80
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_security, line 81
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdStr, cryptoArray)]] // library marker davegut.lib_tpLink_security, line 82
	def respData = syncPost(uri, reqBody, cookie) // library marker davegut.lib_tpLink_security, line 83
	if (respData.status == "OK") { // library marker davegut.lib_tpLink_security, line 84
		if (respData.resp.data.error_code == 0) { // library marker davegut.lib_tpLink_security, line 85
			try { // library marker davegut.lib_tpLink_security, line 86
				def cmdResp = decrypt(respData.resp.data.result.response, cryptoArray) // library marker davegut.lib_tpLink_security, line 87
				cmdResp = new JsonSlurper().parseText(cmdResp) // library marker davegut.lib_tpLink_security, line 88
				if (cmdResp.error_code == 0) { // library marker davegut.lib_tpLink_security, line 89
					tokenData << [respStatus: "OK", token: cmdResp.result.token] // library marker davegut.lib_tpLink_security, line 90
				} else { // library marker davegut.lib_tpLink_security, line 91
					tokenData << [respStatus: "Error from device",  // library marker davegut.lib_tpLink_security, line 92
								  check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.lib_tpLink_security, line 93
				} // library marker davegut.lib_tpLink_security, line 94
			} catch (err) { // library marker davegut.lib_tpLink_security, line 95
				tokenData << [respStatus: "Error parsing", error: err] // library marker davegut.lib_tpLink_security, line 96
			} // library marker davegut.lib_tpLink_security, line 97
		} else { // library marker davegut.lib_tpLink_security, line 98
			tokenData << [respStatus: "Error in respData.data", data: respData.data] // library marker davegut.lib_tpLink_security, line 99
		} // library marker davegut.lib_tpLink_security, line 100
	} else { // library marker davegut.lib_tpLink_security, line 101
		tokenData << [respStatus: "Error in respData", data: respData] // library marker davegut.lib_tpLink_security, line 102
	} // library marker davegut.lib_tpLink_security, line 103
	if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_security, line 104
		logDebug("handshake: ${tokenData}") // library marker davegut.lib_tpLink_security, line 105
	} else { // library marker davegut.lib_tpLink_security, line 106
		logWarn("handshake: ${tokenData}") // library marker davegut.lib_tpLink_security, line 107
	} // library marker davegut.lib_tpLink_security, line 108
	return tokenData // library marker davegut.lib_tpLink_security, line 109
} // library marker davegut.lib_tpLink_security, line 110

//	===== AES Methods ===== // library marker davegut.lib_tpLink_security, line 112
//def encrypt(plainText, keyData) { // library marker davegut.lib_tpLink_security, line 113
def encrypt(plainText, keyData = null) { // library marker davegut.lib_tpLink_security, line 114
	if (keyData == null) { // library marker davegut.lib_tpLink_security, line 115
		keyData = new JsonSlurper().parseText(aesKey) // library marker davegut.lib_tpLink_security, line 116
	} // library marker davegut.lib_tpLink_security, line 117
	byte[] keyenc = keyData[0..15] // library marker davegut.lib_tpLink_security, line 118
	byte[] ivenc = keyData[16..31] // library marker davegut.lib_tpLink_security, line 119

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 121
	SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.lib_tpLink_security, line 122
	IvParameterSpec iv = new IvParameterSpec(ivenc) // library marker davegut.lib_tpLink_security, line 123
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.lib_tpLink_security, line 124
	String result = cipher.doFinal(plainText.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.lib_tpLink_security, line 125
	return result.replace("\r\n","") // library marker davegut.lib_tpLink_security, line 126
} // library marker davegut.lib_tpLink_security, line 127

def decrypt(cypherText, keyData = null) { // library marker davegut.lib_tpLink_security, line 129
	if (keyData == null) { // library marker davegut.lib_tpLink_security, line 130
		keyData = new JsonSlurper().parseText(aesKey) // library marker davegut.lib_tpLink_security, line 131
	} // library marker davegut.lib_tpLink_security, line 132
	byte[] keyenc = keyData[0..15] // library marker davegut.lib_tpLink_security, line 133
	byte[] ivenc = keyData[16..31] // library marker davegut.lib_tpLink_security, line 134

    byte[] decodedBytes = cypherText.decodeBase64() // library marker davegut.lib_tpLink_security, line 136
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.lib_tpLink_security, line 137
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.lib_tpLink_security, line 138
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivenc)) // library marker davegut.lib_tpLink_security, line 139
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.lib_tpLink_security, line 140
	return result // library marker davegut.lib_tpLink_security, line 141
} // library marker davegut.lib_tpLink_security, line 142

//	===== RSA Key Methods ===== // library marker davegut.lib_tpLink_security, line 144
def getRsaKeys() { // library marker davegut.lib_tpLink_security, line 145
	def keyNo = Math.round(5 * Math.random()).toInteger() // library marker davegut.lib_tpLink_security, line 146
	def keyData = keyData() // library marker davegut.lib_tpLink_security, line 147
	def RSAKeys = keyData.find { it.keyNo == keyNo } // library marker davegut.lib_tpLink_security, line 148
	return RSAKeys // library marker davegut.lib_tpLink_security, line 149
} // library marker davegut.lib_tpLink_security, line 150

def keyData() { // library marker davegut.lib_tpLink_security, line 152
/*	User Note.  You can update these keys at you will using the site: // library marker davegut.lib_tpLink_security, line 153
		https://www.devglan.com/online-tools/rsa-encryption-decryption // library marker davegut.lib_tpLink_security, line 154
	with an RSA Key Size: 1024 bit // library marker davegut.lib_tpLink_security, line 155
	This is at your risk.*/ // library marker davegut.lib_tpLink_security, line 156
	return [ // library marker davegut.lib_tpLink_security, line 157
		[ // library marker davegut.lib_tpLink_security, line 158
			keyNo: 0, // library marker davegut.lib_tpLink_security, line 159
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.lib_tpLink_security, line 160
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw" // library marker davegut.lib_tpLink_security, line 161
		],[ // library marker davegut.lib_tpLink_security, line 162
			keyNo: 1, // library marker davegut.lib_tpLink_security, line 163
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCshy+qBKbJNefcyJUZ/3i+3KyLji6XaWEWvebUCC2r9/0jE6hc89AufO41a13E3gJ2es732vaxwZ1BZKLy468NnL+tg6vlQXaPkDcdunQwjxbTLNL/yzDZs9HRju2lJnupcksdJWBZmjtztMWQkzBrQVeSKzSTrKYK0s24EEXmtQIDAQAB", // library marker davegut.lib_tpLink_security, line 164
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKyHL6oEpsk159zIlRn/eL7crIuOLpdpYRa95tQILav3/SMTqFzz0C587jVrXcTeAnZ6zvfa9rHBnUFkovLjrw2cv62Dq+VBdo+QNx26dDCPFtMs0v/LMNmz0dGO7aUme6lySx0lYFmaO3O0xZCTMGtBV5IrNJOspgrSzbgQRea1AgMBAAECgYBSeiX9H1AkbJK1Z2ZwEUNF6vTJmmUHmScC2jHZNzeuOFVZSXJ5TU0+jBbMjtE65e9DeJ4suw6oF6j3tAZ6GwJ5tHoIy+qHRV6AjA8GEXjhSwwVCyP8jXYZ7UZyHzjLQAK+L0PvwJY1lAtns/Xmk5GH+zpNnhEmKSZAw23f7wpj2QJBANVPQGYT7TsMTDEEl2jq/ZgOX5Djf2VnKpPZYZGsUmg1hMwcpN/4XQ7XOaclR5TO/CJBJl3UCUEVjdrR1zdD8g8CQQDPDoa5Y5UfhLz4Ja2/gs2UKwO4fkTqqR6Ad8fQlaUZ55HINHWFd8FeERBFgNJzszrzd9BBJ7NnZM5nf2OPqU77AkBLuQuScSZ5HL97czbQvwLxVMDmLWyPMdVykOvLC9JhPgZ7cvuwqnlWiF7mEBzeHbBx9JDLJDd4zE8ETBPLgapPAkAHhCR52FaSdVQSwfNjr1DdHw6chODlj8wOp8p2FOiQXyqYlObrOGSpkH8BtuJs1sW+DsxdgR5vE2a2tRYdIe0/AkEAoQ5MzLcETQrmabdVCyB9pQAiHe4yY9e1w7cimsLJOrH7LMM0hqvBqFOIbSPrZyTp7Ie8awn4nTKoZQtvBfwzHw==" // library marker davegut.lib_tpLink_security, line 165
		],[ // library marker davegut.lib_tpLink_security, line 166
			keyNo: 2, // library marker davegut.lib_tpLink_security, line 167
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBeqRy4zAOs63Sc5yc0DtlFXG1stmdD6sEfUiGjlsy0S8aS8X+Qcjcu5AK3uBBrkVNIa8djXht1bd+pUof5/txzWIMJw9SNtNYqzSdeO7cCtRLzuQnQWP7Am64OBvYkXn2sUqoaqDE50LbSQWbuvZw0Vi9QihfBYGQdlrqjCPUsQIDAQAB", // library marker davegut.lib_tpLink_security, line 168
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIF6pHLjMA6zrdJznJzQO2UVcbWy2Z0PqwR9SIaOWzLRLxpLxf5ByNy7kAre4EGuRU0hrx2NeG3Vt36lSh/n+3HNYgwnD1I201irNJ147twK1EvO5CdBY/sCbrg4G9iRefaxSqhqoMTnQttJBZu69nDRWL1CKF8FgZB2WuqMI9SxAgMBAAECgYBBi2wkHI3/Y0Xi+1OUrnTivvBJIri2oW/ZXfKQ6w+PsgU+Mo2QII0l8G0Ck8DCfw3l9d9H/o2wTDgPjGzxqeXHAbxET1dS0QBTjR1zLZlFyfAs7WO8tDKmHVroUgqRkJgoQNQlBSe1E3e7pTgSKElzLuALkRS6p1jhzT2wu9U04QJBAOFr/G36PbQ6NmDYtVyEEr3vWn46JHeZISdJOsordR7Wzbt6xk6/zUDHq0OGM9rYrpBy7PNrbc0JuQrhfbIyaHMCQQCTCvETjXCMkwyUrQT6TpxVzKEVRf1rCitnNQCh1TLnDKcCEAnqZT2RRS3yNXTWFoJrtuEHMGmwUrtog9+ZJBlLAkEA2qxdkPY621XJIIO404mPgM7rMx4F+DsE7U5diHdFw2fO5brBGu13GAtZuUQ7k2W1WY0TDUO+nTN8XPDHdZDuvwJABu7TIwreLaKZS0FFJNAkCt+VEL22Dx/xn/Idz4OP3Nj53t0Guqh/WKQcYHkowxdYmt+KiJ49vXSJJYpiNoQ/NQJAM1HCl8hBznLZLQlxrCTdMvUimG3kJmA0bUNVncgUBq7ptqjk7lp5iNrle5aml99foYnzZeEUW6jrCC7Lj9tg+w==" // library marker davegut.lib_tpLink_security, line 169
		],[ // library marker davegut.lib_tpLink_security, line 170
			keyNo: 3, // library marker davegut.lib_tpLink_security, line 171
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFYaoMvv5kBxUUbp4PQyd7RoZlPompsupXP2La0qGGxacF98/88W4KNUqLbF4X5BPqxoEA+VeZy75qqyfuYbGQ4fxT6usE/LnzW8zDY/PjhVBht8FBRyAUsoYAt3Ip6sDyjd9YzRzUL1Q/OxCgxz5CNETYxcNr7zfMshBHDmZXMQIDAQAB", // library marker davegut.lib_tpLink_security, line 172
			private: "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIVhqgy+/mQHFRRung9DJ3tGhmU+iamy6lc/YtrSoYbFpwX3z/zxbgo1SotsXhfkE+rGgQD5V5nLvmqrJ+5hsZDh/FPq6wT8ufNbzMNj8+OFUGG3wUFHIBSyhgC3cinqwPKN31jNHNQvVD87EKDHPkI0RNjFw2vvN8yyEEcOZlcxAgMBAAECgYA3NxjoMeCpk+z8ClbQRqJ/e9CC9QKUB4bPG2RW5b8MRaJA7DdjpKZC/5CeavwAs+Ay3n3k41OKTTfEfJoJKtQQZnCrqnZfq9IVZI26xfYo0cgSYbi8wCie6nqIBdu9k54nqhePPshi22VcFuOh97xxPvY7kiUaRbbKqxn9PFwrYQJBAMsO3uOnYSJxN/FuxksKLqhtNei2GUC/0l7uIE8rbRdtN3QOpcC5suj7id03/IMn2Ks+Vsrmi0lV4VV/c8xyo9UCQQCoKDlObjbYeYYdW7/NvI6cEntgHygENi7b6WFk+dbRhJQgrFH8Z/Idj9a2E3BkfLCTUM1Z/Z3e7D0iqPDKBn/tAkBAHI3bKvnMOhsDq4oIH0rj+rdOplAK1YXCW0TwOjHTd7ROfGFxHDCUxvacVhTwBCCw0JnuriPEH81phTg2kOuRAkAEPR9UrsqLImUTEGEBWqNto7mgbqifko4T1QozdWjI10K0oCNg7W3Y+Os8o7jNj6cTz5GdlxsHp4TS/tczAH7xAkBY6KPIlF1FfiyJAnBC8+jJr2h4TSPQD7sbJJmYw7mvR+f1T4tsWY0aGux69hVm8BoaLStBVPdkaENBMdP+a07u" // library marker davegut.lib_tpLink_security, line 173
		],[ // library marker davegut.lib_tpLink_security, line 174
			keyNo: 4, // library marker davegut.lib_tpLink_security, line 175
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClF0yuCpo3r1ZpYlGcyI5wy5nnvZdOZmxqz5U2rklt2b8+9uWhmsGdpbTv5+qJXlZmvUKbpoaPxpJluBFDJH2GSpq3I0whh0gNq9Arzpp/TDYaZLb6iIqDMF6wm8yjGOtcSkB7qLQWkXpEN9T2NsEzlfTc+GTKc07QXHnzxoLmwQIDAQAB", // library marker davegut.lib_tpLink_security, line 176
			private: "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKUXTK4KmjevVmliUZzIjnDLmee9l05mbGrPlTauSW3Zvz725aGawZ2ltO/n6oleVma9Qpumho/GkmW4EUMkfYZKmrcjTCGHSA2r0CvOmn9MNhpktvqIioMwXrCbzKMY61xKQHuotBaRekQ31PY2wTOV9Nz4ZMpzTtBcefPGgubBAgMBAAECgYB4wCz+05RvDFk45YfqFCtTRyg//0UvO+0qxsBN6Xad2XlvlWjqJeZd53kLTGcYqJ6rsNyKOmgLu2MS8Wn24TbJmPUAwZU+9cvSPxxQ5k6bwjg1RifieIcbTPC5wHDqVy0/Ur7dt+JVMOHFseR/pElDw471LCdwWSuFHAKuiHsaUQJBANHiPdSU3s1bbJYTLaS1tW0UXo7aqgeXuJgqZ2sKsoIEheEAROJ5rW/f2KrFVtvg0ITSM8mgXNlhNBS5OE4nSD0CQQDJXYJxKvdodeRoj+RGTCZGZanAE1naUzSdfcNWx2IMnYUD/3/2eB7ZIyQPBG5fWjc3bGOJKI+gy/14bCwXU7zVAkAdnsE9HBlpf+qOL3y0jxRgpYxGuuNeGPJrPyjDOYpBwSOnwmL2V1e7vyqTxy/f7hVfeU7nuKMB5q7z8cPZe7+9AkEAl7A6aDe+wlE069OhWZdZqeRBmLC7Gi1d0FoBwahW4zvyDM32vltEmbvQGQP0hR33xGeBH7yPXcjtOz75g+UPtQJBAL4gknJ/p+yQm9RJB0oq/g+HriErpIMHwrhNoRY1aOBMJVl4ari1Ch2RQNL9KQW7yrFDv7XiP3z5NwNDKsp/QeU=" // library marker davegut.lib_tpLink_security, line 177
		],[ // library marker davegut.lib_tpLink_security, line 178
			keyNo: 5, // library marker davegut.lib_tpLink_security, line 179
			public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChN8Xc+gsSuhcLVM1W1E+e1o+celvKlOmuV6sJEkJecknKFujx9+T4xvyapzyePpTBn0lA9EYbaF7UDYBsDgqSwgt0El3gV+49O56nt1ELbLUJtkYEQPK+6Pu8665UG17leCiaMiFQyoZhD80PXhpjehqDu2900uU/4DzKZ/eywwIDAQAB", // library marker davegut.lib_tpLink_security, line 180
			private: "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKE3xdz6CxK6FwtUzVbUT57Wj5x6W8qU6a5XqwkSQl5yScoW6PH35PjG/JqnPJ4+lMGfSUD0RhtoXtQNgGwOCpLCC3QSXeBX7j07nqe3UQtstQm2RgRA8r7o+7zrrlQbXuV4KJoyIVDKhmEPzQ9eGmN6GoO7b3TS5T/gPMpn97LDAgMBAAECgYAy+uQCwL8HqPjoiGR2dKTI4aiAHuEv6m8KxoY7VB7QputWkHARNAaf9KykawXsNHXt1GThuV0CBbsW6z4U7UvCJEZEpv7qJiGX8UWgEs1ISatqXmiIMVosIJJvoFw/rAoScadCYyicskjwDFBVNU53EAUD3WzwEq+dRYDn52lqQQJBAMu30FEReAHTAKE/hvjAeBUyWjg7E4/lnYvb/i9Wuc+MTH0q3JxFGGMb3n6APT9+kbGE0rinM/GEXtpny+5y3asCQQDKl7eNq0NdIEBGAdKerX4O+nVDZ7PXz1kQ2ca0r1tXtY/9sBDDoKHP2fQAH/xlOLIhLaH1rabSEJYNUM0ohHdJAkBYZqhwNWtlJ0ITtvSEB0lUsWfzFLe1bseCBHH16uVwygn7GtlmupkNkO9o548seWkRpnimhnAE8xMSJY6aJ6BHAkEAuSFLKrqGJGOEWHTx8u63cxiMb7wkK+HekfdwDUzxO4U+v6RUrW/sbfPNdQ/FpPnaTVdV2RuGhg+CD0j3MT9bgQJARH86hfxp1bkyc7f1iJQT8sofdqqVz5grCV5XeGY77BNmCvTOGLfL5pOJdgALuOoP4t3e94nRYdlW6LqIVugRBQ==" // library marker davegut.lib_tpLink_security, line 181
		] // library marker davegut.lib_tpLink_security, line 182
	] // library marker davegut.lib_tpLink_security, line 183
} // library marker davegut.lib_tpLink_security, line 184

// ~~~~~ end include (1337) davegut.lib_tpLink_security ~~~~~

// ~~~~~ start include (1370) davegut.lib_tpLink_discovery ~~~~~
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
	def devIp // library marker davegut.lib_tpLink_discovery, line 12
	List ipList = [] // library marker davegut.lib_tpLink_discovery, line 13
	def respData // library marker davegut.lib_tpLink_discovery, line 14
	if (response instanceof Map) { // library marker davegut.lib_tpLink_discovery, line 15
		devIp = getDeviceIp(response) // library marker davegut.lib_tpLink_discovery, line 16
		if (devIp != "INVALID") { // library marker davegut.lib_tpLink_discovery, line 17
			ipList << devIp // library marker davegut.lib_tpLink_discovery, line 18
		} // library marker davegut.lib_tpLink_discovery, line 19
	} else { // library marker davegut.lib_tpLink_discovery, line 20
		response.each { // library marker davegut.lib_tpLink_discovery, line 21
			devIp = getDeviceIp(it) // library marker davegut.lib_tpLink_discovery, line 22
			if (devIp != "INVALID") { // library marker davegut.lib_tpLink_discovery, line 23
				ipList << devIp // library marker davegut.lib_tpLink_discovery, line 24
			} // library marker davegut.lib_tpLink_discovery, line 25
			pauseExecution(100) // library marker davegut.lib_tpLink_discovery, line 26
		} // library marker davegut.lib_tpLink_discovery, line 27
	} // library marker davegut.lib_tpLink_discovery, line 28
	getAllSmartDeviceData(ipList) // library marker davegut.lib_tpLink_discovery, line 29
} // library marker davegut.lib_tpLink_discovery, line 30

def getDeviceIp(response) { // library marker davegut.lib_tpLink_discovery, line 32
	def brand = "KASA" // library marker davegut.lib_tpLink_discovery, line 33
	if (appName() == "tapo_device_install") { brand = "TAPO" } // library marker davegut.lib_tpLink_discovery, line 34
	def devIp = "INVALID" // library marker davegut.lib_tpLink_discovery, line 35
	try { // library marker davegut.lib_tpLink_discovery, line 36
		def respData = parseLanMessage(response.description) // library marker davegut.lib_tpLink_discovery, line 37
		if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.lib_tpLink_discovery, line 38
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.lib_tpLink_discovery, line 39
			String payloadString = new String(payloadByte) // library marker davegut.lib_tpLink_discovery, line 40
			Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.lib_tpLink_discovery, line 41
			Map payloadData = [type: payload.device_type, model: payload.device_model,  // library marker davegut.lib_tpLink_discovery, line 42
							   mac: payload.mac, ip: payload.ip] // library marker davegut.lib_tpLink_discovery, line 43
			if (payload.device_type.contains(brand)) { // library marker davegut.lib_tpLink_discovery, line 44
				devIp = payload.ip // library marker davegut.lib_tpLink_discovery, line 45
				logInfo("getDeviceIp: [TAPOdevice: ${payloadData}]") // library marker davegut.lib_tpLink_discovery, line 46
			} else { // library marker davegut.lib_tpLink_discovery, line 47
				logInfo("getDeviceIp: [KASAdevice: ${payloadData}]") // library marker davegut.lib_tpLink_discovery, line 48
			} // library marker davegut.lib_tpLink_discovery, line 49
		} // library marker davegut.lib_tpLink_discovery, line 50
	} catch (err) { // library marker davegut.lib_tpLink_discovery, line 51
		logWarn("getDevIp: [status: ERROR, respData: ${resData}, error: ${err}]") // library marker davegut.lib_tpLink_discovery, line 52
	} // library marker davegut.lib_tpLink_discovery, line 53
	return devIp // library marker davegut.lib_tpLink_discovery, line 54
} // library marker davegut.lib_tpLink_discovery, line 55

def getAllSmartDeviceData(List ipList) { // library marker davegut.lib_tpLink_discovery, line 57
	Map logData = [:] // library marker davegut.lib_tpLink_discovery, line 58
	ipList.each { devIp -> // library marker davegut.lib_tpLink_discovery, line 59
		Map devData = [:] // library marker davegut.lib_tpLink_discovery, line 60
		def cmdResp = getSmartDeviceData([method: "get_device_info"], devIp) // library marker davegut.lib_tpLink_discovery, line 61
		if (cmdResp == "ERROR") { // library marker davegut.lib_tpLink_discovery, line 62
			devData << [status: "ERROR", data: "Failure in getSmartDeviceData"] // library marker davegut.lib_tpLink_discovery, line 63
		} else { // library marker davegut.lib_tpLink_discovery, line 64
			if (cmdResp.result.type.contains("SMART")) { // library marker davegut.lib_tpLink_discovery, line 65
				devData << [status: "OK"] // library marker davegut.lib_tpLink_discovery, line 66
				parseSmartDeviceData(cmdResp.result) // library marker davegut.lib_tpLink_discovery, line 67
			} else { // library marker davegut.lib_tpLink_discovery, line 68
				if (cmdResp.result.type) { // library marker davegut.lib_tpLink_discovery, line 69
					devData << [status: "OK", devType: cmdResp.result.type, devIp: cmdResp.result.ip] // library marker davegut.lib_tpLink_discovery, line 70
				} else { // library marker davegut.lib_tpLink_discovery, line 71
					devData << [status: "ERROR", data: cmdResp] // library marker davegut.lib_tpLink_discovery, line 72
				} // library marker davegut.lib_tpLink_discovery, line 73
			} // library marker davegut.lib_tpLink_discovery, line 74
		} // library marker davegut.lib_tpLink_discovery, line 75
		logData << [devIp: devData] // library marker davegut.lib_tpLink_discovery, line 76
		pauseExecution(200) // library marker davegut.lib_tpLink_discovery, line 77
	} // library marker davegut.lib_tpLink_discovery, line 78
	if (!logData.toString().contains("ERROR")) { // library marker davegut.lib_tpLink_discovery, line 79
		logDebug("getSmartDeviceData: ${logData}") // library marker davegut.lib_tpLink_discovery, line 80
	} else { // library marker davegut.lib_tpLink_discovery, line 81
		logWarn("getSmartDeviceData: ${logData}") // library marker davegut.lib_tpLink_discovery, line 82
	} // library marker davegut.lib_tpLink_discovery, line 83
	pauseExecution(5000) // library marker davegut.lib_tpLink_discovery, line 84
	state.findingDevices = "done" // library marker davegut.lib_tpLink_discovery, line 85
} // library marker davegut.lib_tpLink_discovery, line 86

def deviceLogin(devIp) { // library marker davegut.lib_tpLink_discovery, line 88
	Map logData = [:] // library marker davegut.lib_tpLink_discovery, line 89
	def handshakeData = handshake(devIp) // library marker davegut.lib_tpLink_discovery, line 90
	if (handshakeData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 91
		Map credentials = [encUsername: encUsername, encPassword: encPassword] // library marker davegut.lib_tpLink_discovery, line 92
		def tokenData = loginDevice(handshakeData.cookie, handshakeData.aesKey,  // library marker davegut.lib_tpLink_discovery, line 93
									credentials, devIp) // library marker davegut.lib_tpLink_discovery, line 94
		if (tokenData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 95
			logData << [rsaKeys: handshakeData.rsaKeys, // library marker davegut.lib_tpLink_discovery, line 96
						cookie: handshakeData.cookie, // library marker davegut.lib_tpLink_discovery, line 97
						aesKey: handshakeData.aesKey, // library marker davegut.lib_tpLink_discovery, line 98
						token: tokenData.token] // library marker davegut.lib_tpLink_discovery, line 99
		} else { // library marker davegut.lib_tpLink_discovery, line 100
			logData << [tokenData: tokenData] // library marker davegut.lib_tpLink_discovery, line 101
		} // library marker davegut.lib_tpLink_discovery, line 102
	} else { // library marker davegut.lib_tpLink_discovery, line 103
		logData << [handshakeData: handshakeData] // library marker davegut.lib_tpLink_discovery, line 104
	} // library marker davegut.lib_tpLink_discovery, line 105
	return logData // library marker davegut.lib_tpLink_discovery, line 106
} // library marker davegut.lib_tpLink_discovery, line 107

def getSmartDeviceData(cmdBody, devIp) { // library marker davegut.lib_tpLink_discovery, line 109
	def cmdResp = "ERROR" // library marker davegut.lib_tpLink_discovery, line 110
	def loginData = deviceLogin(devIp) // library marker davegut.lib_tpLink_discovery, line 111
	Map logData = [cmdBody: cmdBody, devIp: devIp, token: loginData.token, aeskey: loginData.aesKey, cookie: loginData.cookie] // library marker davegut.lib_tpLink_discovery, line 112
	if (loginData.token == null) { // library marker davegut.lib_tpLink_discovery, line 113
		logData << [respStatus: "FAILED", reason: "Check Credentials"] // library marker davegut.lib_tpLink_discovery, line 114
	} else { // library marker davegut.lib_tpLink_discovery, line 115
		def uri = "http://${devIp}/app?token=${loginData.token}" // library marker davegut.lib_tpLink_discovery, line 116
		cmdBody = JsonOutput.toJson(cmdBody).toString() // library marker davegut.lib_tpLink_discovery, line 117
		Map reqBody = [method: "securePassthrough", // library marker davegut.lib_tpLink_discovery, line 118
					   params: [request: encrypt(cmdBody, loginData.aesKey)]] // library marker davegut.lib_tpLink_discovery, line 119
		def respData = syncPost(uri, reqBody, loginData.cookie) // library marker davegut.lib_tpLink_discovery, line 120
		if (respData.status == "OK") { // library marker davegut.lib_tpLink_discovery, line 121
			logData << [respStatus: "OK"] // library marker davegut.lib_tpLink_discovery, line 122
			respData = respData.resp.data.result.response // library marker davegut.lib_tpLink_discovery, line 123
			cmdResp = new JsonSlurper().parseText(decrypt(respData, loginData.aesKey)) // library marker davegut.lib_tpLink_discovery, line 124
		} else { // library marker davegut.lib_tpLink_discovery, line 125
			logData << respData // library marker davegut.lib_tpLink_discovery, line 126
		} // library marker davegut.lib_tpLink_discovery, line 127
	} // library marker davegut.lib_tpLink_discovery, line 128
	if (logData.respStatus == "OK") { // library marker davegut.lib_tpLink_discovery, line 129
		logDebug("getSmartDeviceData: ${logData}") // library marker davegut.lib_tpLink_discovery, line 130
	} else { // library marker davegut.lib_tpLink_discovery, line 131
		logWarn("getSmartDeviceData: ${logData}") // library marker davegut.lib_tpLink_discovery, line 132
	} // library marker davegut.lib_tpLink_discovery, line 133
	return cmdResp // library marker davegut.lib_tpLink_discovery, line 134
} // library marker davegut.lib_tpLink_discovery, line 135

// ~~~~~ end include (1370) davegut.lib_tpLink_discovery ~~~~~
