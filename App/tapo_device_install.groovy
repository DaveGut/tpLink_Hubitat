/*	Multi-TP-Link Product Integration Application
	Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md
===== Link to Documentation =====
	https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/Documentation.pdf
========================================*/





import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.json.JSONObject

definition(
	name: "Tapo Integration",
	namespace: nameSpace(),
	author: "Dave Gutheinz",
	description: "Application to install Tapo protocol TP-Link bulbs, plugs, and switches.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	installOnOpen: true,
	singleInstance: true,
	documentationLink: "https://github.com/DaveGut/tpLink_Hubitat",
	importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/refs/heads/main/App/tapo_device_install.groovy"
)

preferences {
	page(name: "startPage")
	page(name: "enterCredentialsPage")
	page(name: "addDevicesPage")
	page(name: "removeDevicesPage")
}

def installed() {
	app?.updateSetting("logEnable", false)
	app?.updateSetting("infoLog", true)
	def hub = location.hubs[0]
	def hubIpArray = hub.localIP.split('\\.')
	def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
	app?.updateSetting("lanSegment", [type:"string", value: segments])
	app?.updateSetting("hostLimits", [type:"string", value: "2, 254"])
	app?.updateSetting("encPassword", "INVALID")
	app?.updateSetting("encUsername", "INVALID")
	app?.updateSetting("localHash", "INVALID")
	logInfo([method: "installed", status: "Initialized settings"])
}
def updated() {
	app?.removeSetting("selectedAddDevices")
	app?.removeSetting("selectedRemoveDevices")
	app?.updateSetting("logEnable", false)
	app?.updateSetting("appSetup", false)
	app?.updateSetting("scheduled", false)
	state.needCreds = false
	scheduleItems()
	logInfo([method: "updated", status: "setting updated for new session"])
}

def scheduleItems() {
	Map logData = [method: "scheduleItems"]
	unschedule()
	runIn(570, resetTpLinkChecked)
	app?.updateSetting("scheduled", false)
	logData << setLogsOff()
	logInfo(logData)
}

def uninstalled() {
    getAllChildDevices().each { 
        deleteChildDevice(it.deviceNetworkId)
    }
	logInfo([method: "uninstalled", status: "Devices and App uninstalled"])
}

def initInstance() {
	if (!scheduled) {
		unschedule()
		runIn(1800, scheduleItems)
		app?.updateSetting("scheduled", true)
	}
	if (!state.needCreds) { state.needCreds = false }
	state.tpLinkChecked = false
	setSegments()
	if (state.appVersion != version()) {
		state.appVersion = version()
		app.removeSetting("appVer")		//	ver 2.4.1 only
		app.removeSetting("ports")		//	ver 2.4.1 only
		state.remove("portArray")		//	ver 2.4.1 only
		app.removeSetting("showFound")	//	ver 2.4.1 only
		app.removeSetting("startApp")	//	ver 2.4.1 only
		app.removeSetting("finding")	//	ver 2.4.1 only
		atomicState.devices = [:]	//	assures update of devices.
		logInfo([method: "initInstance", status: "App data updated for appVersion ${version()}"])
	}
	if (appSetup) {
		setSegments()
		logInfo([method: "initInstance", status: "Updated App Setup Data"])
	}
	return
}

def setSegments() {
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
		logWarn("startPage: Invalid entry for Lan Segements or Host Array Range. Resetting to default!")
		def hub = location.hubs[0]
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
		app?.updateSetting("hostLimits", [type:"string", value: "1, 254"])
	}
}

def startPage() {
	logInfo([method: "startPage", status: "Starting ${app.getLabel()} Setup"])
	def action = initInstance()
	if (selectedRemoveDevices) { removeDevices() } 
	else if (selectedAddDevices) { addDevices() }
	return dynamicPage(name:"startPage",
					   uninstall: true,
					   install: true) {
		section() {
			Map lanParams = [LanSegments: state.segArray, hostRange: state.hostArray]
			String params = "<b>Application Setup Parameters</b>"
			params += "\n\t<b>lanDiscoveryParams</b>: ${lanParams}"
			paragraph params
			input "appSetup", "bool", title: "<b>Modify Application Setup</b> (LanDiscParams)",
				submitOnChange: true, defaultValue: false
			if (appSetup) {
				input "lanSegment", "string",
					title: "<b>Lan Segments</b> (ex: 192.168.50, 192,168.01)", submitOnChange: true
				input "hostLimits", "string",
					title: "<b>Host Address Range</b> (ex: 5, 100)", submitOnChange: true
			}
			def credDesc = "Credentials: userName: ${userName}, password set/redacted."
			if (!userName || !userPassword) {
				credDesc = "<b>Credentials not set.  Enter credentials to proceed.</b>"
				state.needCreds = true
			} else {
				def wait = createTpLinkCreds()
				logDebug(wait)
				credDesc += "\nEncoded password and username set based on credentials."
				state.needCreds = false
			}

			href "enterCredentialsPage",
				title: "<b>Enter/Update Username and Password</b>",
				description: credDesc
			if (!state.needCreds) {
				href "addDevicesPage",
					title: "<b>Scan for devices and add</b>",
					description: "It will take 30+ seconds to find devices."
			} else {
				paragraph "<b>Credentials are required to scan for to find devices.</b>"
			}
			href "removeDevicesPage",
				title: "<b>Remove Devices</b>",
				description: "Select to remove selected Device from Hubitat."
			input "logEnable", "bool",
				   title: "<b>Debug logging</b>",
				   submitOnChange: true
		}
	}
}

def enterCredentialsPage() {
	Map credData = [:]
	return dynamicPage (name: "enterCredentialsPage", 
    					title: "Enter  Credentials",
						nextPage: startPage,
                        install: false) {
		section() {
			input "hidePassword", "bool",
				title: "<b>Hide Password</b>",
				submitOnChange: true,
				defaultValue: false
			paragraph "<b>Password and Username are both case sensitive.</b>"
			def pwdType = "string"
			if (hidePassword) { pwdType = "password" }
			input ("userName", "email",
            		title: "Email Address", 
                    required: false,
                    submitOnChange: false)
			input ("userPassword", pwdType,
            		title: "Account Password",
                    required: false,
                    submitOnChange: false)
		}
	}
}

//	===== Add selected newdevices =====
def addDevicesPage() {
	logDebug("addDevicesPage")
	app?.removeSetting("selectedAddDevices")
	def action = findDevices(5)
	def addDevicesData = atomicState.devices
	Map uninstalledDevices = [:]
	List installedDrivers = getInstalledDrivers()
	Map foundDevices = [:]
	def matterDev = false
	addDevicesData.each {
		def isChild = getChildDevice(it.key)
		if (!isChild) {
			uninstalledDevices["${it.key}"] = "${it.value.alias}, ${it.value.type}"
			def driver = "TpLink ${it.value.type}"
			if (!installedDrivers.find{ it == driver }) {
				foundDevices << ["${it.value.alias}":  "<b>Not installed.  Needs driver ${driver}</b>"]
			} else {
				foundDevices << ["${it.value.alias}":  "<b>Not installed.</b>  Driver found."]
			}
		} else {
			foundDevices << ["${it.value.alias}":  "Installed."]
		}
	}
	foundDevices = foundDevices.sort()
	String devicesFound = "<b>Found Devices</b>"
	foundDevices.each{ devicesFound += "\n\t${it}" }

	return dynamicPage(name:"addDevicesPage",
					   title: "Add Devices to Hubitat",
					   nextPage: startPage,
					   install: false) {
	 	section() {
			input ("selectedAddDevices", "enum",
				   required: false,
				   multiple: true,
				   title: "<b>Devices to add</b> (${uninstalledDevices.size() ?: 0} available).\n\t" +
				   "Total Devices: ${addDevicesData.size()}",
				   description: "Use the dropdown to select devices.  Then select 'Done'.",
				   options: uninstalledDevices)
			if (matterDev == true) {
				paragraph "<b>Caution.</b> Do not install Matter device here if already installed using the Hubitat Matter Ingegration."
			}
			paragraph devicesFound
			href "addDevicesPage",
				title: "<b>Rescan for Additional Devices</b>",
				description: "<b>Perform scan again to try to capture missing devices.</b>"
		}
	}
}

def getInstalledDrivers() {
	List installedDrivers = []
	Map params = [
		uri: "https://127.0.0.1:8443",
		ignoreSSLIssues: true,
		path: "/hub2/userDeviceTypes",
	  ]
	try {
		httpGet(params) { resp ->
			resp.data.each {
				if (it.namespace == nameSpace()) {
					installedDrivers << it.name
				}
			}
		}
		logDebug([method: "getInstalledDrivers", drivers: installedDrivers])
	} catch (err) {
		logWarn([method: "getInstalledDrivers", err: err,
				 message: "Unable to get installed driver list"])
	}
	return installedDrivers
}

def findDevices(timeout) {
	Map logData = [method: "findDevices", action: "findingDevices"]
	logInfo(logData)
	def await = findTpLinkDevices("getTpLinkLanData", timeout)
	return
}

def supportedProducts() {
	return ["SMART.TAPOBULB", "SMART.TAPOPLUG", "SMART.TAPOSWITCH","SMART.KASAHUB", 
			"SMART.TAPOHUB", "SMART.KASAPLUG", "SMART.KASASWITCH", "SMART.TAPOROBOVAC",
		    "SMART.MATTERBULB", "SMART.MATTERPLUG"]
}

//	===== Add Devices =====
def addDevices() {
	Map logData = [method: "addDevices", selectedDevices: selectedDevices]
	def hub = location.hubs[0]
	def devicesData = atomicState.devices
	selectedAddDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = devicesData.find { it.key == dni }
			addDevice(device, dni)
		}
		pauseExecution(3000)
	}
	logInfo(logData)
	app?.removeSetting("selectedAddDevices")
}

def addDevice(device, dni) {
	Map logData = [method: "addDevice", dni: dni]
	try {
		Map deviceData = [protocol: device.value.protocol]
		deviceData << [baseUrl: device.value.baseUrl,
					   tpLinkType: device.value.deviceType,
					   type: device.value.type,
					   isEm: device.value.isEm,
					   hasLed: device.value.hasLed]
		if (device.value.ctLow != null) {
			deviceData << [ctLow: device.value.ctLow,
						   ctHigh: device.value.ctHigh]
		}
		try {
			addChildDevice(
				nameSpace(),
				"TpLink ${device.value.type}",
				dni,
				[
					"label": device.value.alias,
					"name" : device.value.model,
					"data" : deviceData
				]
			)
			logData << [status: "added"]
			logInfo(logData)
		} catch (err) {
			logData << [status: "failedToAdd", DRIVER: "TpLink ${device.value.type}",
						errorMsg: error, mostLikelyCause: "DRIVER NOT INSTALLED"]
			logWarn(logData)
		}
	} catch (error) {
		logData << [status: "failedToAdd", device: device, errorMsg: error]
		logWarn(logData)
	}
	return
}

//	===== Remove Devices =====
def removeDevicesPage() {
	Map logData = [method: "removeDevicesPage"]
	Map installedDevices = [:]
	getChildDevices().each {
		installedDevices << ["${it.device.deviceNetworkId}": it.device.label]
	}
	logData << [installedDevices: installedDevices]
	logInfo(logData)
	return dynamicPage(name:"removedDevicesPage",
					   title:"<b>Remove Devices from Hubitat</b>",
					   nextPage: startPage,
					   install: false) {
		section() {
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
	Map logData = [method: "removeDevices", selectedRemoveDevices: selectedRemoveDevices]
	selectedRemoveDevices.each { dni ->
		def isChild = getChildDevice(dni)
		deleteChildDevice(dni)
		logData << ["${dni}": [status: "deleted"]]
	}
	app?.removeSetting("selectedRemoveDevices")
	logInfo(logData)
}

def getDeviceData(dni) {
	Map devices = atomicState.devices
	def device = devices.find { it.key == dni }
	Map devData = device.value
	return devData
}

//	===== Common UDP Communications =====
private sendLanCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) {
	def myHubAction = new hubitat.device.HubAction(
		cmdData,
		hubitat.device.Protocol.LAN,
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
		 destinationAddress: "${ip}:${port}",
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
		 ignoreResponse: ignore,
		 parseWarning: true,
		 timeout: commsTo,
		 callback: action])
	try {
		sendHubCommand(myHubAction)
	} catch (error) {
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}")
	}
	return
}

def pingTest() {
	Map devices = atomicState.devices
	devices.each {device ->
		def baseUrl = device.value.baseUrl
		ping(baseUrl)
	}
}

// ~~~~~ start include (194) davegut.appTpLinkSmart ~~~~~
library ( // library marker davegut.appTpLinkSmart, line 1
	name: "appTpLinkSmart", // library marker davegut.appTpLinkSmart, line 2
	namespace: "davegut", // library marker davegut.appTpLinkSmart, line 3
	author: "Dave Gutheinz", // library marker davegut.appTpLinkSmart, line 4
	description: "Discovery library for Application support the Tapo protocol devices.", // library marker davegut.appTpLinkSmart, line 5
	category: "utilities", // library marker davegut.appTpLinkSmart, line 6
	documentationLink: "" // library marker davegut.appTpLinkSmart, line 7
) // library marker davegut.appTpLinkSmart, line 8
import org.json.JSONObject // library marker davegut.appTpLinkSmart, line 9
import groovy.json.JsonOutput // library marker davegut.appTpLinkSmart, line 10
import groovy.json.JsonBuilder // library marker davegut.appTpLinkSmart, line 11
import groovy.json.JsonSlurper // library marker davegut.appTpLinkSmart, line 12

def createTpLinkCreds() { // library marker davegut.appTpLinkSmart, line 14
	Map SMARTCredData = [:] // library marker davegut.appTpLinkSmart, line 15

	String encUsername = mdEncode("SHA-1", userName.bytes).encodeHex().encodeAsBase64().toString() // library marker davegut.appTpLinkSmart, line 17
	app?.updateSetting("encUsername", [type: "password", value: encUsername]) // library marker davegut.appTpLinkSmart, line 18
	SMARTCredData << [encUsername: encUsername] // library marker davegut.appTpLinkSmart, line 19

	String encPassword = userPassword.bytes.encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 21
	app?.updateSetting("encPassword", [type: "password", value: encPassword]) // library marker davegut.appTpLinkSmart, line 22
	SMARTCredData << [encPassword: encPassword] // library marker davegut.appTpLinkSmart, line 23

	String encPasswordVac = mdEncode("MD5", userPassword.bytes).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 25
	app?.updateSetting("encPasswordVac", [type: "password", value: encPasswordVac]) // library marker davegut.appTpLinkSmart, line 26
	SMARTCredData << [encPasswordVac: encPasswordVac] // library marker davegut.appTpLinkSmart, line 27

	def userHash = mdEncode("SHA-1", encodeUtf8(userName).getBytes()) // library marker davegut.appTpLinkSmart, line 29
	def passwordHash = mdEncode("SHA-1", encodeUtf8(userPassword).getBytes()) // library marker davegut.appTpLinkSmart, line 30
	byte[] klapLocalHash = [userHash, passwordHash].flatten() // library marker davegut.appTpLinkSmart, line 31
	String localHash = mdEncode("SHA-256", klapLocalHash).encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 32
	app?.updateSetting("localHash", [type: "password", value: localHash]) // library marker davegut.appTpLinkSmart, line 33
	SMARTCredData << [localHash: localHash] // library marker davegut.appTpLinkSmart, line 34

	return [SMARTDevCreds: SMARTCredData] // library marker davegut.appTpLinkSmart, line 36
} // library marker davegut.appTpLinkSmart, line 37

def findTpLinkDevices(action, timeout = 10) { // library marker davegut.appTpLinkSmart, line 39
	Map logData = [method: "findTpLinkDevices", action: action, timeOut: timeout] // library marker davegut.appTpLinkSmart, line 40
	def start = state.hostArray.min().toInteger() // library marker davegut.appTpLinkSmart, line 41
	def finish = state.hostArray.max().toInteger() + 1 // library marker davegut.appTpLinkSmart, line 42
	logData << [hostArray: state.hostArray, pollSegments: state.segArray] // library marker davegut.appTpLinkSmart, line 43
	List deviceIPs = [] // library marker davegut.appTpLinkSmart, line 44
	state.segArray.each { // library marker davegut.appTpLinkSmart, line 45
		def pollSegment = it.trim() // library marker davegut.appTpLinkSmart, line 46
		logData << [pollSegment: pollSegment] // library marker davegut.appTpLinkSmart, line 47
           for(int i = start; i < finish; i++) { // library marker davegut.appTpLinkSmart, line 48
			deviceIPs.add("${pollSegment}.${i.toString()}") // library marker davegut.appTpLinkSmart, line 49
		} // library marker davegut.appTpLinkSmart, line 50
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.appTpLinkSmart, line 51
		await = sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeout) // library marker davegut.appTpLinkSmart, line 52
		atomicState.finding = true // library marker davegut.appTpLinkSmart, line 53
		int i // library marker davegut.appTpLinkSmart, line 54
//		for(i = 0; i < 60; i++) { // library marker davegut.appTpLinkSmart, line 55
//			pauseExecution(1000) // library marker davegut.appTpLinkSmart, line 56
		for(i = 0; i < 60; i+=5) { // library marker davegut.appTpLinkSmart, line 57
			pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 58
			if (atomicState.finding == false) { // library marker davegut.appTpLinkSmart, line 59
				logInfo("<b>FindingDevices: Finished Finding</b>") // library marker davegut.appTpLinkSmart, line 60
				pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 61
				i = 61 // library marker davegut.appTpLinkSmart, line 62
				break // library marker davegut.appTpLinkSmart, line 63
			} // library marker davegut.appTpLinkSmart, line 64
			logInfo("<b>FindingDevices: ${i} seconds</b>") // library marker davegut.appTpLinkSmart, line 65
		} // library marker davegut.appTpLinkSmart, line 66
	} // library marker davegut.appTpLinkSmart, line 67
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 68
	return logData // library marker davegut.appTpLinkSmart, line 69
} // library marker davegut.appTpLinkSmart, line 70

def getTpLinkLanData(response) { // library marker davegut.appTpLinkSmart, line 72
	Map logData = [method: "getTpLinkLanData",  // library marker davegut.appTpLinkSmart, line 73
				   action: "Completed LAN Discovery", // library marker davegut.appTpLinkSmart, line 74
				   smartDevicesFound: response.size()] // library marker davegut.appTpLinkSmart, line 75
	logInfo(logData) // library marker davegut.appTpLinkSmart, line 76
	List discData = [] // library marker davegut.appTpLinkSmart, line 77
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 78
		Map devData = getDiscData(response) // library marker davegut.appTpLinkSmart, line 79
		if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 80
			discData << devData // library marker davegut.appTpLinkSmart, line 81
		} // library marker davegut.appTpLinkSmart, line 82
	} else { // library marker davegut.appTpLinkSmart, line 83
		response.each { // library marker davegut.appTpLinkSmart, line 84
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 85
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 86
				discData << devData // library marker davegut.appTpLinkSmart, line 87
			} // library marker davegut.appTpLinkSmart, line 88
		} // library marker davegut.appTpLinkSmart, line 89
	} // library marker davegut.appTpLinkSmart, line 90
	getAllTpLinkDeviceData(discData) // library marker davegut.appTpLinkSmart, line 91

	app?.updateSetting("finding", false) // library marker davegut.appTpLinkSmart, line 93
	runIn(5, updateTpLinkDevices, [data: discData]) // library marker davegut.appTpLinkSmart, line 94
} // library marker davegut.appTpLinkSmart, line 95

def getDiscData(response) { // library marker davegut.appTpLinkSmart, line 97
	Map devData = [method: "getDiscData"] // library marker davegut.appTpLinkSmart, line 98
	try { // library marker davegut.appTpLinkSmart, line 99
		def respData = parseLanMessage(response.description) // library marker davegut.appTpLinkSmart, line 100
		if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.appTpLinkSmart, line 101
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.appTpLinkSmart, line 102
			String payloadString = new String(payloadByte) // library marker davegut.appTpLinkSmart, line 103
			if (payloadString.length() > 1007) { // library marker davegut.appTpLinkSmart, line 104
				payloadString = payloadString + """"}}}""" // library marker davegut.appTpLinkSmart, line 105
			} // library marker davegut.appTpLinkSmart, line 106
			Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.appTpLinkSmart, line 107
			List supported = supportedProducts() // library marker davegut.appTpLinkSmart, line 108
			if (supported.contains(payload.device_type)) { // library marker davegut.appTpLinkSmart, line 109

				if (!payload.mgt_encrypt_schm.encrypt_type) { // library marker davegut.appTpLinkSmart, line 111
					String mssg = "<b>The ${payload.device_model} is not supported " // library marker davegut.appTpLinkSmart, line 112
					mssg += "by this integration version.</b>" // library marker davegut.appTpLinkSmart, line 113
					devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.appTpLinkSmart, line 114
								status: "INVALID", reason: "Device not supported."] // library marker davegut.appTpLinkSmart, line 115
					logWarn(mssg) // library marker davegut.appTpLinkSmart, line 116
					return devData // library marker davegut.appTpLinkSmart, line 117
				} // library marker davegut.appTpLinkSmart, line 118

				def protocol = payload.mgt_encrypt_schm.encrypt_type // library marker davegut.appTpLinkSmart, line 120
				def port = payload.mgt_encrypt_schm.http_port // library marker davegut.appTpLinkSmart, line 121
				def dni = payload.mac.replaceAll("-", "") // library marker davegut.appTpLinkSmart, line 122
				def baseUrl = "http://${payload.ip}:${payload.mgt_encrypt_schm.http_port}/app" // library marker davegut.appTpLinkSmart, line 123
				if (payload.device_type == "SMART.TAPOROBOVAC") { // library marker davegut.appTpLinkSmart, line 124
					baseUrl = "https://${payload.ip}:${payload.mgt_encrypt_schm.http_port}" // library marker davegut.appTpLinkSmart, line 125
					protocol = "vacAes" // library marker davegut.appTpLinkSmart, line 126
				} // library marker davegut.appTpLinkSmart, line 127
				devData << [ // library marker davegut.appTpLinkSmart, line 128
					type: payload.device_type, model: payload.device_model, // library marker davegut.appTpLinkSmart, line 129
					baseUrl: baseUrl, dni: dni, devId: payload.device_id,  // library marker davegut.appTpLinkSmart, line 130
					ip: payload.ip, port: port, protocol: protocol, status: "OK"] // library marker davegut.appTpLinkSmart, line 131
			} else { // library marker davegut.appTpLinkSmart, line 132
				devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.appTpLinkSmart, line 133
							status: "INVALID", reason: "Device not supported."] // library marker davegut.appTpLinkSmart, line 134
				logWarn(devData) // library marker davegut.appTpLinkSmart, line 135
			} // library marker davegut.appTpLinkSmart, line 136
		} // library marker davegut.appTpLinkSmart, line 137
		logDebug(devData) // library marker davegut.appTpLinkSmart, line 138
	} catch (err) { // library marker davegut.appTpLinkSmart, line 139
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.appTpLinkSmart, line 140
		logWarn(devData) // library marker davegut.appTpLinkSmart, line 141
	} // library marker davegut.appTpLinkSmart, line 142
	return devData // library marker davegut.appTpLinkSmart, line 143
} // library marker davegut.appTpLinkSmart, line 144

def getAllTpLinkDeviceData(List discData) { // library marker davegut.appTpLinkSmart, line 146
	Map logData = [method: "getAllTpLinkDeviceData", discData: discData.size()] // library marker davegut.appTpLinkSmart, line 147
	discData.each { Map devData -> // library marker davegut.appTpLinkSmart, line 148
		if (devData.protocol == "KLAP") { // library marker davegut.appTpLinkSmart, line 149
			klapHandshake(devData.baseUrl, localHash, devData) // library marker davegut.appTpLinkSmart, line 150
		} else if (devData.protocol == "AES") { // library marker davegut.appTpLinkSmart, line 151
			aesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 152
		} else if (devData.protocol == "vacAes") { // library marker davegut.appTpLinkSmart, line 153
			vacAesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 154
		} // library marker davegut.appTpLinkSmart, line 155
		pauseExecution(1000) // library marker davegut.appTpLinkSmart, line 156
	} // library marker davegut.appTpLinkSmart, line 157
	atomicState.finding = false // library marker davegut.appTpLinkSmart, line 158
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 159
} // library marker davegut.appTpLinkSmart, line 160

def getDataCmd() { // library marker davegut.appTpLinkSmart, line 162
	List requests = [[method: "get_device_info"]] // library marker davegut.appTpLinkSmart, line 163
	requests << [method: "component_nego"] // library marker davegut.appTpLinkSmart, line 164
	Map cmdBody = [ // library marker davegut.appTpLinkSmart, line 165
		method: "multipleRequest", // library marker davegut.appTpLinkSmart, line 166
		params: [requests: requests]] // library marker davegut.appTpLinkSmart, line 167
	return cmdBody // library marker davegut.appTpLinkSmart, line 168
} // library marker davegut.appTpLinkSmart, line 169

def addToDevices(devData, cmdResp) { // library marker davegut.appTpLinkSmart, line 171
	Map logData = [method: "addToDevices"] // library marker davegut.appTpLinkSmart, line 172
	String dni = devData.dni // library marker davegut.appTpLinkSmart, line 173
	def devicesData = atomicState.devices // library marker davegut.appTpLinkSmart, line 174
	def components = cmdResp.find { it.method == "component_nego" } // library marker davegut.appTpLinkSmart, line 175
	cmdResp = cmdResp.find { it.method == "get_device_info" } // library marker davegut.appTpLinkSmart, line 176
	cmdResp = cmdResp.result // library marker davegut.appTpLinkSmart, line 177
	byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.appTpLinkSmart, line 178
	def alias = new String(plainBytes) // library marker davegut.appTpLinkSmart, line 179
	if (alias == "") { alias = cmdResp.model } // library marker davegut.appTpLinkSmart, line 180
	def comps = components.result.component_list // library marker davegut.appTpLinkSmart, line 181
	String tpType = devData.type // library marker davegut.appTpLinkSmart, line 182
	def type = "Unknown" // library marker davegut.appTpLinkSmart, line 183
	def ctHigh // library marker davegut.appTpLinkSmart, line 184
	def ctLow // library marker davegut.appTpLinkSmart, line 185
	//	Creat map deviceData // library marker davegut.appTpLinkSmart, line 186
	Map deviceData = [deviceType: tpType, protocol: devData.protocol, // library marker davegut.appTpLinkSmart, line 187
				   model: devData.model, baseUrl: devData.baseUrl, alias: alias] // library marker davegut.appTpLinkSmart, line 188
	//	Determine Driver to Load // library marker davegut.appTpLinkSmart, line 189
	if (tpType.contains("PLUG") || tpType.contains("SWITCH")) { // library marker davegut.appTpLinkSmart, line 190
		type = "Plug" // library marker davegut.appTpLinkSmart, line 191
		if (comps.find { it.id == "control_child" }) { // library marker davegut.appTpLinkSmart, line 192
			type = "Parent" // library marker davegut.appTpLinkSmart, line 193
		} else if (comps.find { it.id == "dimmer" }) { // library marker davegut.appTpLinkSmart, line 194
			type = "Dimmer" // library marker davegut.appTpLinkSmart, line 195
		} // library marker davegut.appTpLinkSmart, line 196
	} else if (tpType.contains("HUB")) { // library marker davegut.appTpLinkSmart, line 197
		type = "Hub" // library marker davegut.appTpLinkSmart, line 198
	} else if (tpType.contains("BULB")) { // library marker davegut.appTpLinkSmart, line 199
		type = "Dimmer" // library marker davegut.appTpLinkSmart, line 200
		if (comps.find { it.id == "light_strip" }) { // library marker davegut.appTpLinkSmart, line 201
			type = "Lightstrip" // library marker davegut.appTpLinkSmart, line 202
		} else if (comps.find { it.id == "color" }) { // library marker davegut.appTpLinkSmart, line 203
			type = "Color Bulb" // library marker davegut.appTpLinkSmart, line 204
		} // library marker davegut.appTpLinkSmart, line 205
		//	Get color temp range for Bulb and Lightstrip // library marker davegut.appTpLinkSmart, line 206
		if (type != "Dimmer" && comps.find { it.id == "color_temperature" } ) { // library marker davegut.appTpLinkSmart, line 207
			ctHigh = cmdResp.color_temp_range[1] // library marker davegut.appTpLinkSmart, line 208
			ctLow = cmdResp.color_temp_range[0] // library marker davegut.appTpLinkSmart, line 209
			deviceData << [ctHigh: ctHigh, ctLow: ctLow] // library marker davegut.appTpLinkSmart, line 210
		} // library marker davegut.appTpLinkSmart, line 211
	} else if (tpType.contains("ROBOVAC")) { // library marker davegut.appTpLinkSmart, line 212
		type = "Robovac" // library marker davegut.appTpLinkSmart, line 213
	} // library marker davegut.appTpLinkSmart, line 214
	//	Determine device-specific data relative to device settings // library marker davegut.appTpLinkSmart, line 215
	def hasLed = "false" // library marker davegut.appTpLinkSmart, line 216
	if (comps.find { it.id == "led" } ) { hasLed = "true" } // library marker davegut.appTpLinkSmart, line 217
	def isEm = "false" // library marker davegut.appTpLinkSmart, line 218
	if (comps.find { it.id == "energy_monitoring" } ) { isEm = "true" } // library marker davegut.appTpLinkSmart, line 219
	def gradOnOff = "false" // library marker davegut.appTpLinkSmart, line 220
	if (comps.find { it.id == "on_off_gradually" } ) { gradOnOff = "true" } // library marker davegut.appTpLinkSmart, line 221
	deviceData << [type: type, hasLed: hasLed, isEm: isEm, gradOnOff: gradOnOff] // library marker davegut.appTpLinkSmart, line 222
	//	Add to devices and close out method // library marker davegut.appTpLinkSmart, line 223
	devicesData << ["${dni}": deviceData] // library marker davegut.appTpLinkSmart, line 224
	atomicState.devices = devicesData // library marker davegut.appTpLinkSmart, line 225
	logData << ["${deviceData.alias}": deviceData, dni: dni] // library marker davegut.appTpLinkSmart, line 226
	Map InfoData = ["${deviceData.alias}": "added to device data"] // library marker davegut.appTpLinkSmart, line 227
	logInfo("${deviceData.alias}: added to device data") // library marker davegut.appTpLinkSmart, line 228
	updateChild(dni,deviceData) // library marker davegut.appTpLinkSmart, line 229
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 230
} // library marker davegut.appTpLinkSmart, line 231

def updateChild(dni, deviceData) { // library marker davegut.appTpLinkSmart, line 233
	def child = getChildDevice(dni) // library marker davegut.appTpLinkSmart, line 234
	if (child) { // library marker davegut.appTpLinkSmart, line 235
		child.updateChild(deviceData) // library marker davegut.appTpLinkSmart, line 236
	} // library marker davegut.appTpLinkSmart, line 237
} // library marker davegut.appTpLinkSmart, line 238

//	===== get Smart KLAP Protocol Data ===== // library marker davegut.appTpLinkSmart, line 240
def sendKlapDataCmd(handshakeData, data) { // library marker davegut.appTpLinkSmart, line 241
	if (handshakeData.respStatus != "Login OK") { // library marker davegut.appTpLinkSmart, line 242
		Map logData = [method: "sendKlapDataCmd", handshake: handshakeData] // library marker davegut.appTpLinkSmart, line 243
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 244
	} else { // library marker davegut.appTpLinkSmart, line 245
		Map reqParams = [timeout: 10, headers: ["Cookie": data.data.cookie]] // library marker davegut.appTpLinkSmart, line 246
		def seqNo = data.data.seqNo + 1 // library marker davegut.appTpLinkSmart, line 247
		String cmdBodyJson = new groovy.json.JsonBuilder(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 248
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), data.data.encKey,  // library marker davegut.appTpLinkSmart, line 249
										data.data.encIv, data.data.encSig, seqNo) // library marker davegut.appTpLinkSmart, line 250
		reqParams << [uri: "${data.data.baseUrl}/request?seq=${encryptedData.seqNumber}", // library marker davegut.appTpLinkSmart, line 251
					  body: encryptedData.cipherData, // library marker davegut.appTpLinkSmart, line 252
					  contentType: "application/octet-stream", // library marker davegut.appTpLinkSmart, line 253
					  requestContentType: "application/octet-stream"] // library marker davegut.appTpLinkSmart, line 254
		asynchttpPost("parseKlapResp", reqParams, [data: data.data]) // library marker davegut.appTpLinkSmart, line 255
	} // library marker davegut.appTpLinkSmart, line 256
} // library marker davegut.appTpLinkSmart, line 257

def parseKlapResp(resp, data) { // library marker davegut.appTpLinkSmart, line 259
	Map logData = [method: "parseKlapResp"] // library marker davegut.appTpLinkSmart, line 260
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 261
		try { // library marker davegut.appTpLinkSmart, line 262
			byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.appTpLinkSmart, line 263
			def clearResp = klapDecrypt(cipherResponse, data.data.encKey, // library marker davegut.appTpLinkSmart, line 264
										data.data.encIv, data.data.seqNo + 1) // library marker davegut.appTpLinkSmart, line 265
			Map cmdResp =  new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 266
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 267
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 268
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 269
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 270
			} else { // library marker davegut.appTpLinkSmart, line 271
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 272
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 273
			} // library marker davegut.appTpLinkSmart, line 274
		} catch (err) { // library marker davegut.appTpLinkSmart, line 275
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 276
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 277
		} // library marker davegut.appTpLinkSmart, line 278
	} else { // library marker davegut.appTpLinkSmart, line 279
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 280
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 281
	} // library marker davegut.appTpLinkSmart, line 282
} // library marker davegut.appTpLinkSmart, line 283

//	===== get Smart AES Protocol Data ===== // library marker davegut.appTpLinkSmart, line 285
def getAesToken(resp, data) { // library marker davegut.appTpLinkSmart, line 286
	Map logData = [method: "getAesToken"] // library marker davegut.appTpLinkSmart, line 287
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 288
		if (resp.json.error_code == 0) { // library marker davegut.appTpLinkSmart, line 289
			try { // library marker davegut.appTpLinkSmart, line 290
				def clearResp = aesDecrypt(resp.json.result.response, data.encKey, data.encIv) // library marker davegut.appTpLinkSmart, line 291
				Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 292
				if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 293
					def token = cmdResp.result.token // library marker davegut.appTpLinkSmart, line 294
					logData << [respStatus: "OK", token: token] // library marker davegut.appTpLinkSmart, line 295
					logDebug(logData) // library marker davegut.appTpLinkSmart, line 296
					sendAesDataCmd(token, data) // library marker davegut.appTpLinkSmart, line 297
				} else { // library marker davegut.appTpLinkSmart, line 298
					logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.appTpLinkSmart, line 299
								error_code: cmdResp.error_code, // library marker davegut.appTpLinkSmart, line 300
								check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.appTpLinkSmart, line 301
					logWarn(logData) // library marker davegut.appTpLinkSmart, line 302
				} // library marker davegut.appTpLinkSmart, line 303
			} catch (err) { // library marker davegut.appTpLinkSmart, line 304
				logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.appTpLinkSmart, line 305
							error: err] // library marker davegut.appTpLinkSmart, line 306
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 307
			} // library marker davegut.appTpLinkSmart, line 308
		} else { // library marker davegut.appTpLinkSmart, line 309
			logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.appTpLinkSmart, line 310
						respJson: resp.json] // library marker davegut.appTpLinkSmart, line 311
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 312
		} // library marker davegut.appTpLinkSmart, line 313
	} else { // library marker davegut.appTpLinkSmart, line 314
		logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.appTpLinkSmart, line 315
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 316
	} // library marker davegut.appTpLinkSmart, line 317
} // library marker davegut.appTpLinkSmart, line 318

def sendAesDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 320
	def cmdStr = JsonOutput.toJson(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 321
	Map reqBody = [method: "securePassthrough", // library marker davegut.appTpLinkSmart, line 322
				   params: [request: aesEncrypt(cmdStr, data.encKey, data.encIv)]] // library marker davegut.appTpLinkSmart, line 323
	Map reqParams = [uri: "${data.baseUrl}?token=${token}", // library marker davegut.appTpLinkSmart, line 324
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.appTpLinkSmart, line 325
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 326
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 327
					 timeout: 10,  // library marker davegut.appTpLinkSmart, line 328
					 headers: ["Cookie": data.cookie]] // library marker davegut.appTpLinkSmart, line 329
	asynchttpPost("parseAesResp", reqParams, [data: data]) // library marker davegut.appTpLinkSmart, line 330
} // library marker davegut.appTpLinkSmart, line 331

def parseAesResp(resp, data) { // library marker davegut.appTpLinkSmart, line 333
	Map logData = [method: "parseAesResp"] // library marker davegut.appTpLinkSmart, line 334
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 335
		try { // library marker davegut.appTpLinkSmart, line 336
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.appTpLinkSmart, line 337
																 data.data.encKey, data.data.encIv)) // library marker davegut.appTpLinkSmart, line 338
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 339
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 340
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 341
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 342
			} else { // library marker davegut.appTpLinkSmart, line 343
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 344
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 345
			} // library marker davegut.appTpLinkSmart, line 346
		} catch (err) { // library marker davegut.appTpLinkSmart, line 347
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 348
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 349
		} // library marker davegut.appTpLinkSmart, line 350
	} else { // library marker davegut.appTpLinkSmart, line 351
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 352
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 353
	} // library marker davegut.appTpLinkSmart, line 354
} // library marker davegut.appTpLinkSmart, line 355

//	===== get Smart VAC Protocol Data ===== // library marker davegut.appTpLinkSmart, line 357
def vacAesHandshake(baseUrl, devData) { // library marker davegut.appTpLinkSmart, line 358
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.appTpLinkSmart, line 359
	Map cmdBody = [method: "login", // library marker davegut.appTpLinkSmart, line 360
				   params: [hashed: true,  // library marker davegut.appTpLinkSmart, line 361
							password: encPasswordVac, // library marker davegut.appTpLinkSmart, line 362
							username: userName]] // library marker davegut.appTpLinkSmart, line 363
	Map reqParams = [uri: baseUrl, // library marker davegut.appTpLinkSmart, line 364
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 365
					 body: cmdBody, // library marker davegut.appTpLinkSmart, line 366
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 367
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 368
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 369
	asynchttpPost("parseVacAesLogin", reqParams, [data: reqData]) // library marker davegut.appTpLinkSmart, line 370
} // library marker davegut.appTpLinkSmart, line 371

def parseVacAesLogin(resp, data) { // library marker davegut.appTpLinkSmart, line 373
	Map logData = [method: "parseVacAesLogin", oldToken: token] // library marker davegut.appTpLinkSmart, line 374
	if (resp.status == 200 && resp.json != null) { // library marker davegut.appTpLinkSmart, line 375
		logData << [status: "OK"] // library marker davegut.appTpLinkSmart, line 376
		logData << [token: resp.json.result.token] // library marker davegut.appTpLinkSmart, line 377
		sendVacDataCmd(resp.json.result.token, data) // library marker davegut.appTpLinkSmart, line 378
		logDebug(logData) // library marker davegut.appTpLinkSmart, line 379
	} else { // library marker davegut.appTpLinkSmart, line 380
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.appTpLinkSmart, line 381
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 382
	} // library marker davegut.appTpLinkSmart, line 383
} // library marker davegut.appTpLinkSmart, line 384

def sendVacDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 386
	Map devData = data.data.devData // library marker davegut.appTpLinkSmart, line 387
	Map reqParams = [uri: "${data.data.baseUrl}/?token=${token}", // library marker davegut.appTpLinkSmart, line 388
					 body: getDataCmd(), // library marker davegut.appTpLinkSmart, line 389
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 390
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 391
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 392
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 393
	asynchttpPost("parseVacResp", reqParams, [data: devData]) // library marker davegut.appTpLinkSmart, line 394
} // library marker davegut.appTpLinkSmart, line 395

def parseVacResp(resp, devData) { // library marker davegut.appTpLinkSmart, line 397
	Map logData = [parseMethod: "parseVacResp"] // library marker davegut.appTpLinkSmart, line 398
	try { // library marker davegut.appTpLinkSmart, line 399
		Map cmdResp = resp.json // library marker davegut.appTpLinkSmart, line 400
		logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 401
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 402
				addToDevices(devData.data, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 403
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 404
			} else { // library marker davegut.appTpLinkSmart, line 405
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 406
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 407
			} // library marker davegut.appTpLinkSmart, line 408
	} catch (err) { // library marker davegut.appTpLinkSmart, line 409
		logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 410
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 411
	} // library marker davegut.appTpLinkSmart, line 412
	return parseData	 // library marker davegut.appTpLinkSmart, line 413
} // library marker davegut.appTpLinkSmart, line 414

def tpLinkCheckForDevices(timeout = 3) { // library marker davegut.appTpLinkSmart, line 416
	Map logData = [method: "tpLinkCheckForDevices"] // library marker davegut.appTpLinkSmart, line 417
	def checked = true // library marker davegut.appTpLinkSmart, line 418
	if (state.tpLinkChecked == true) { // library marker davegut.appTpLinkSmart, line 419
		checked = false // library marker davegut.appTpLinkSmart, line 420
		logData << [status: "noCheck", reason: "Completed within last 10 minutes"] // library marker davegut.appTpLinkSmart, line 421
	} else { // library marker davegut.appTpLinkSmart, line 422
		def findData = findTpLinkDevices("parseTpLinkCheck", timeout) // library marker davegut.appTpLinkSmart, line 423
		logData << [status: "checking"] // library marker davegut.appTpLinkSmart, line 424
		pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 425
	} // library marker davegut.appTpLinkSmart, line 426
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 427
	return checked // library marker davegut.appTpLinkSmart, line 428
} // library marker davegut.appTpLinkSmart, line 429

def resetTpLinkChecked() { state.tpLinkChecked = false } // library marker davegut.appTpLinkSmart, line 431

def parseTpLinkCheck(response) { // library marker davegut.appTpLinkSmart, line 433
	List discData = [] // library marker davegut.appTpLinkSmart, line 434
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 435
		Map devdata = getDiscData(response) // library marker davegut.appTpLinkSmart, line 436
		if (devData.status != "INVALID") { // library marker davegut.appTpLinkSmart, line 437
			discData << devData // library marker davegut.appTpLinkSmart, line 438
		} // library marker davegut.appTpLinkSmart, line 439
	} else { // library marker davegut.appTpLinkSmart, line 440
		response.each { // library marker davegut.appTpLinkSmart, line 441
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 442
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 443
				discData << devData // library marker davegut.appTpLinkSmart, line 444
			} // library marker davegut.appTpLinkSmart, line 445
		} // library marker davegut.appTpLinkSmart, line 446
	} // library marker davegut.appTpLinkSmart, line 447
	atomicState.finding = false // library marker davegut.appTpLinkSmart, line 448
	updateTpLinkDevices(discData) // library marker davegut.appTpLinkSmart, line 449
} // library marker davegut.appTpLinkSmart, line 450

def updateTpLinkDevices(discData) { // library marker davegut.appTpLinkSmart, line 452
	Map logData = [method: "updateTpLinkDevices"] // library marker davegut.appTpLinkSmart, line 453
	state.tpLinkChecked = true // library marker davegut.appTpLinkSmart, line 454
	runIn(570, resetTpLinkChecked) // library marker davegut.appTpLinkSmart, line 455
	List children = getChildDevices() // library marker davegut.appTpLinkSmart, line 456
	children.each { childDev -> // library marker davegut.appTpLinkSmart, line 457
		Map childData = [:] // library marker davegut.appTpLinkSmart, line 458
		def dni = childDev.deviceNetworkId // library marker davegut.appTpLinkSmart, line 459
		def connected = "false" // library marker davegut.appTpLinkSmart, line 460
		Map devData = discData.find{ it.dni == dni } // library marker davegut.appTpLinkSmart, line 461
		if (childDev.getDataValue("baseUrl")) { // library marker davegut.appTpLinkSmart, line 462
			if (devData != null) { // library marker davegut.appTpLinkSmart, line 463
				if (childDev.getDataValue("baseUrl") == devData.baseUrl && // library marker davegut.appTpLinkSmart, line 464
				    childDev.getDataValue("protocol") == devData.protocol) { // library marker davegut.appTpLinkSmart, line 465
					childData << [status: "noChanges"] // library marker davegut.appTpLinkSmart, line 466
				} else { // library marker davegut.appTpLinkSmart, line 467
					childDev.updateDataValue("baseUrl", devData.baseUrl) // library marker davegut.appTpLinkSmart, line 468
					childDev.updateDataValue("protocol", devData.protocol) // library marker davegut.appTpLinkSmart, line 469
					childData << ["baseUrl": devData.baseUrl, // library marker davegut.appTpLinkSmart, line 470
								  "protocol": devData.protocol, // library marker davegut.appTpLinkSmart, line 471
								  "connected": "true"] // library marker davegut.appTpLinkSmart, line 472
				} // library marker davegut.appTpLinkSmart, line 473
			} else { // library marker davegut.appTpLinkSmart, line 474
				Map warnData = [method: "updateTpLinkDevices", device: childDev, // library marker davegut.appTpLinkSmart, line 475
								connected: "false", reason: "not Discovered By App"] // library marker davegut.appTpLinkSmart, line 476
				logWarn(warnData) // library marker davegut.appTpLinkSmart, line 477
			} // library marker davegut.appTpLinkSmart, line 478
			pauseExecution(500) // library marker davegut.appTpLinkSmart, line 479
		} // library marker davegut.appTpLinkSmart, line 480
		logData << ["${childDev}": childData] // library marker davegut.appTpLinkSmart, line 481
	} // library marker davegut.appTpLinkSmart, line 482
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 483
} // library marker davegut.appTpLinkSmart, line 484

// ~~~~~ end include (194) davegut.appTpLinkSmart ~~~~~

// ~~~~~ start include (203) davegut.tpLinkComms ~~~~~
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
	} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 21
		reqParams = getAesParams(cmdBody) // library marker davegut.tpLinkComms, line 22
	} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 23
		reqParams = getVacAesParams(cmdBody) // library marker davegut.tpLinkComms, line 24
	} // library marker davegut.tpLinkComms, line 25
	if (state.errorCount == 0) { // library marker davegut.tpLinkComms, line 26
		state.lastCommand = cmdData // library marker davegut.tpLinkComms, line 27
	} // library marker davegut.tpLinkComms, line 28
	asynchttpPost(action, reqParams, [data: reqData]) // library marker davegut.tpLinkComms, line 29
} // library marker davegut.tpLinkComms, line 30

def parseData(resp, protocol = getDataValue("protocol")) { // library marker davegut.tpLinkComms, line 32
	Map logData = [method: "parseData", status: resp.status] // library marker davegut.tpLinkComms, line 33
	def message = "OK" // library marker davegut.tpLinkComms, line 34
	if (resp.status != 200) { message = resp.errorMessage } // library marker davegut.tpLinkComms, line 35
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 36
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 37
			logData << parseKlapData(resp) // library marker davegut.tpLinkComms, line 38
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 39
			logData << parseAesData(resp) // library marker davegut.tpLinkComms, line 40
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 41
			logData << parseVacAesData(resp) // library marker davegut.tpLinkComms, line 42
		} // library marker davegut.tpLinkComms, line 43
	} else { // library marker davegut.tpLinkComms, line 44
		String userMessage = "unspecified" // library marker davegut.tpLinkComms, line 45
		if (resp.status == 403) { // library marker davegut.tpLinkComms, line 46
			userMessage = "<b>Try again. If error persists, check your credentials</b>" // library marker davegut.tpLinkComms, line 47
		} else if (resp.status == 408) { // library marker davegut.tpLinkComms, line 48
			userMessage = "<b>Your router connection to ${getDataValue("baseUrl")} failed.  Run Configure.</b>" // library marker davegut.tpLinkComms, line 49
		} else { // library marker davegut.tpLinkComms, line 50
			userMessage = "<b>Unhandled error Lan return</b>" // library marker davegut.tpLinkComms, line 51
		} // library marker davegut.tpLinkComms, line 52
		logData << [respMessage: message, userMessage: userMessage] // library marker davegut.tpLinkComms, line 53
		logDebug(logData) // library marker davegut.tpLinkComms, line 54
	} // library marker davegut.tpLinkComms, line 55
	handleCommsError(resp.status, message) // library marker davegut.tpLinkComms, line 56
	return logData // library marker davegut.tpLinkComms, line 57
} // library marker davegut.tpLinkComms, line 58

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 60
def handleCommsError(status, msg = "") { // library marker davegut.tpLinkComms, line 61
	//	Retransmit all comms error except Switch and Level related (Hub retries for these). // library marker davegut.tpLinkComms, line 62
	//	This is determined by state.digital // library marker davegut.tpLinkComms, line 63
	if (status == 200) { // library marker davegut.tpLinkComms, line 64
		setCommsError(status, "OK") // library marker davegut.tpLinkComms, line 65
	} else { // library marker davegut.tpLinkComms, line 66
		Map logData = [method: "handleCommsError", status: code, msg: msg] // library marker davegut.tpLinkComms, line 67
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 68
		logData << [count: count, status: status, msg: msg] // library marker davegut.tpLinkComms, line 69
		switch(count) { // library marker davegut.tpLinkComms, line 70
			case 1: // library marker davegut.tpLinkComms, line 71
			case 2: // library marker davegut.tpLinkComms, line 72
				//	errors 1 and 2, retry immediately // library marker davegut.tpLinkComms, line 73
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 74
				break // library marker davegut.tpLinkComms, line 75
			case 3: // library marker davegut.tpLinkComms, line 76
				//	error 3, login or scan find device on the lan // library marker davegut.tpLinkComms, line 77
				//	then retry // library marker davegut.tpLinkComms, line 78
				if (status == 403) { // library marker davegut.tpLinkComms, line 79
					logData << [action: "attemptLogin"] // library marker davegut.tpLinkComms, line 80
					deviceHandshake() // library marker davegut.tpLinkComms, line 81
					runIn(4, delayedPassThrough) // library marker davegut.tpLinkComms, line 82
				} else { // library marker davegut.tpLinkComms, line 83
					logData << [action: "Find on LAN then login"] // library marker davegut.tpLinkComms, line 84
					configure() // library marker davegut.tpLinkComms, line 85
					runIn(10, delayedPassThrough) // library marker davegut.tpLinkComms, line 86
				} // library marker davegut.tpLinkComms, line 87
				break // library marker davegut.tpLinkComms, line 88
			case 4: // library marker davegut.tpLinkComms, line 89
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 90
				break // library marker davegut.tpLinkComms, line 91
			default: // library marker davegut.tpLinkComms, line 92
				//	Set comms error first time errros are 5 or more. // library marker davegut.tpLinkComms, line 93
				logData << [action: "SetCommsErrorTrue"] // library marker davegut.tpLinkComms, line 94
				setCommsError(status, msg, 5) // library marker davegut.tpLinkComms, line 95
		} // library marker davegut.tpLinkComms, line 96
		state.errorCount = count // library marker davegut.tpLinkComms, line 97
		logInfo(logData) // library marker davegut.tpLinkComms, line 98
	} // library marker davegut.tpLinkComms, line 99
} // library marker davegut.tpLinkComms, line 100

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 102
	//	Do a single packet ping to check LAN connectivity.  This does // library marker davegut.tpLinkComms, line 103
	//	not stop the sending of the retry message. // library marker davegut.tpLinkComms, line 104
	def await = ping(getDataValue("baseUrl"), 1) // library marker davegut.tpLinkComms, line 105
	def cmdData = new JSONObject(state.lastCommand) // library marker davegut.tpLinkComms, line 106
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 107
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 108
} // library marker davegut.tpLinkComms, line 109

def ping(baseUrl = getDataValue("baseUrl"), count = 1) { // library marker davegut.tpLinkComms, line 111
	def ip = baseUrl.replace("""http://""", "").replace(":80/app", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 112
	ip = ip.replace("""https://""", "").replace(":4433", "") // library marker davegut.tpLinkComms, line 113
	hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(ip, count) // library marker davegut.tpLinkComms, line 114
	Map pingReturn = [method: "ping", ip: ip] // library marker davegut.tpLinkComms, line 115
	if (pingData.packetsReceived == count) { // library marker davegut.tpLinkComms, line 116
		pingReturn << [pingStatus: "success"] // library marker davegut.tpLinkComms, line 117
		logDebug(pingReturn) // library marker davegut.tpLinkComms, line 118
	} else { // library marker davegut.tpLinkComms, line 119
		pingReturn << [pingData: pingData, pingStatus: "<b>FAILED</b>.  There may be issues with your LAN."] // library marker davegut.tpLinkComms, line 120
		logWarn(pingReturn) // library marker davegut.tpLinkComms, line 121
	} // library marker davegut.tpLinkComms, line 122
	return pingReturn // library marker davegut.tpLinkComms, line 123
} // library marker davegut.tpLinkComms, line 124

def setCommsError(status, msg = "OK", count = state.commsError) { // library marker davegut.tpLinkComms, line 126
	Map logData = [method: "setCommsError", status: status, errorMsg: msg, count: count] // library marker davegut.tpLinkComms, line 127
	if (device && status == 200) { // library marker davegut.tpLinkComms, line 128
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 129
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 130
			updateAttr("commsError", "false") // library marker davegut.tpLinkComms, line 131
			setPollInterval() // library marker davegut.tpLinkComms, line 132
			unschedule(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 133
			logInfo(logData) // library marker davegut.tpLinkComms, line 134
		} // library marker davegut.tpLinkComms, line 135
	} else if (device) { // library marker davegut.tpLinkComms, line 136
		if (device.currentValue("commsError") == "false" && count > 4) { // library marker davegut.tpLinkComms, line 137
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 138
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 139
			runEvery10Minutes(errorConfigure) // library marker davegut.tpLinkComms, line 140
			logData << [pollInterval: "30 Min", errorDeviceHandshake: "ever 10 min"] // library marker davegut.tpLinkComms, line 141
			logWarn(logData) // library marker davegut.tpLinkComms, line 142
			if (status == 403) { // library marker davegut.tpLinkComms, line 143
				logWarn(logInErrorAction()) // library marker davegut.tpLinkComms, line 144
			} else { // library marker davegut.tpLinkComms, line 145
				logWarn(lanErrorAction()) // library marker davegut.tpLinkComms, line 146
			} // library marker davegut.tpLinkComms, line 147
		} else { // library marker davegut.tpLinkComms, line 148
			logData << [error: "Unspecified Error"] // library marker davegut.tpLinkComms, line 149
			logWarn(logData) // library marker davegut.tpLinkComms, line 150
		} // library marker davegut.tpLinkComms, line 151
	} // library marker davegut.tpLinkComms, line 152
} // library marker davegut.tpLinkComms, line 153

def lanErrorAction() { // library marker davegut.tpLinkComms, line 155
	def action = "Likely cause of this error is YOUR LAN device configuration: " // library marker davegut.tpLinkComms, line 156
	action += "a. VERIFY your device is on the DHCP list in your router, " // library marker davegut.tpLinkComms, line 157
	action += "b. VERIFY your device is in the active device list in your router, and " // library marker davegut.tpLinkComms, line 158
	action += "c. TRY controlling your device from the TAPO phone app." // library marker davegut.tpLinkComms, line 159
	return action // library marker davegut.tpLinkComms, line 160
} // library marker davegut.tpLinkComms, line 161

def logInErrorAction() { // library marker davegut.tpLinkComms, line 163
	def action = "Likely cause is your login credentials are incorrect or the login has expired. " // library marker davegut.tpLinkComms, line 164
	action += "a. RUN command Configure. b. If error persists, check your credentials in the App" // library marker davegut.tpLinkComms, line 165
	return action // library marker davegut.tpLinkComms, line 166
} // library marker davegut.tpLinkComms, line 167

def errorConfigure() { // library marker davegut.tpLinkComms, line 169
	logDebug([method: "errorConfigure"]) // library marker davegut.tpLinkComms, line 170
	configure() // library marker davegut.tpLinkComms, line 171
} // library marker davegut.tpLinkComms, line 172

//	===== Common UDP Communications for checking if device at IP is device in Hubitat ===== // library marker davegut.tpLinkComms, line 174
private sendFindCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) { // library marker davegut.tpLinkComms, line 175
	def myHubAction = new hubitat.device.HubAction( // library marker davegut.tpLinkComms, line 176
		cmdData, // library marker davegut.tpLinkComms, line 177
		hubitat.device.Protocol.LAN, // library marker davegut.tpLinkComms, line 178
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, // library marker davegut.tpLinkComms, line 179
		 destinationAddress: "${ip}:${port}", // library marker davegut.tpLinkComms, line 180
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING, // library marker davegut.tpLinkComms, line 181
		 ignoreResponse: ignore, // library marker davegut.tpLinkComms, line 182
		 parseWarning: true, // library marker davegut.tpLinkComms, line 183
		 timeout: commsTo, // library marker davegut.tpLinkComms, line 184
		 callback: action]) // library marker davegut.tpLinkComms, line 185
	try { // library marker davegut.tpLinkComms, line 186
		sendHubCommand(myHubAction) // library marker davegut.tpLinkComms, line 187
	} catch (error) { // library marker davegut.tpLinkComms, line 188
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}") // library marker davegut.tpLinkComms, line 189
	} // library marker davegut.tpLinkComms, line 190
	return // library marker davegut.tpLinkComms, line 191
} // library marker davegut.tpLinkComms, line 192

// ~~~~~ end include (203) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (204) davegut.tpLinkCrypto ~~~~~
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
	asynchttpPost("parseAesHandshake", reqParams, [data: reqData]) // library marker davegut.tpLinkCrypto, line 27
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
			asynchttpPost("parseAesLogin", reqParams, [data: reqData]) // library marker davegut.tpLinkCrypto, line 73
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
						setCommsError(200) // library marker davegut.tpLinkCrypto, line 100
						logDebug(logData) // library marker davegut.tpLinkCrypto, line 101
					} else { // library marker davegut.tpLinkCrypto, line 102
						logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.tpLinkCrypto, line 103
									error_code: cmdResp.error_code, // library marker davegut.tpLinkCrypto, line 104
									check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.tpLinkCrypto, line 105
						logInfo(logData) // library marker davegut.tpLinkCrypto, line 106
					} // library marker davegut.tpLinkCrypto, line 107
				} catch (err) { // library marker davegut.tpLinkCrypto, line 108
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.tpLinkCrypto, line 109
								error: err] // library marker davegut.tpLinkCrypto, line 110
					logInfo(logData) // library marker davegut.tpLinkCrypto, line 111
				} // library marker davegut.tpLinkCrypto, line 112
			} else { // library marker davegut.tpLinkCrypto, line 113
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.tpLinkCrypto, line 114
							respJson: resp.json] // library marker davegut.tpLinkCrypto, line 115
				logInfo(logData) // library marker davegut.tpLinkCrypto, line 116
			} // library marker davegut.tpLinkCrypto, line 117
		} else { // library marker davegut.tpLinkCrypto, line 118
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.tpLinkCrypto, line 119
			logInfo(logData) // library marker davegut.tpLinkCrypto, line 120
		} // library marker davegut.tpLinkCrypto, line 121
	} else { // library marker davegut.tpLinkCrypto, line 122
		//	Code used in application only. // library marker davegut.tpLinkCrypto, line 123
		getAesToken(resp, data.data) // library marker davegut.tpLinkCrypto, line 124
	} // library marker davegut.tpLinkCrypto, line 125
} // library marker davegut.tpLinkCrypto, line 126

//	===== KLAP Handshake ===== // library marker davegut.tpLinkCrypto, line 128
def klapHandshake(baseUrl = getDataValue("baseUrl"), localHash = parent.localHash, devData = null) { // library marker davegut.tpLinkCrypto, line 129
	byte[] localSeed = new byte[16] // library marker davegut.tpLinkCrypto, line 130
	new Random().nextBytes(localSeed) // library marker davegut.tpLinkCrypto, line 131
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkCrypto, line 132
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkCrypto, line 133
					 body: localSeed, // library marker davegut.tpLinkCrypto, line 134
					 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 135
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 136
					 timeout:10] // library marker davegut.tpLinkCrypto, line 137
	asynchttpPost("parseKlapHandshake", reqParams, [data: reqData]) // library marker davegut.tpLinkCrypto, line 138
} // library marker davegut.tpLinkCrypto, line 139

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkCrypto, line 141
	Map logData = [method: "parseKlapHandshake"] // library marker davegut.tpLinkCrypto, line 142
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 143
		try { // library marker davegut.tpLinkCrypto, line 144
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 145
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkCrypto, line 146
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkCrypto, line 147
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkCrypto, line 148
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkCrypto, line 149
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkCrypto, line 150
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 151
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkCrypto, line 152
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkCrypto, line 153
				//	cookie // library marker davegut.tpLinkCrypto, line 154
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 155
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 156
				//	seqNo and encIv // library marker davegut.tpLinkCrypto, line 157
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 158
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 159
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkCrypto, line 160

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkCrypto, line 162
				atomicState.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 163

				//	encKey // library marker davegut.tpLinkCrypto, line 165
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 166
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkCrypto, line 167
				//	encSig // library marker davegut.tpLinkCrypto, line 168
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 169
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkCrypto, line 170
				if (device) { // library marker davegut.tpLinkCrypto, line 171
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkCrypto, line 172
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkCrypto, line 173
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkCrypto, line 174
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkCrypto, line 175
				} else { // library marker davegut.tpLinkCrypto, line 176
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkCrypto, line 177
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkCrypto, line 178
				} // library marker davegut.tpLinkCrypto, line 179
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 180
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkCrypto, line 181
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkCrypto, line 182
								 body: body, // library marker davegut.tpLinkCrypto, line 183
								 timeout:10, // library marker davegut.tpLinkCrypto, line 184
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 185
								 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 186
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 187
				asynchttpPost("parseKlapHandshake2", reqParams, [data: reqData]) // library marker davegut.tpLinkCrypto, line 188
			} else { // library marker davegut.tpLinkCrypto, line 189
				logData << [respStatus: "ERROR: localAuthHash != serverHash", // library marker davegut.tpLinkCrypto, line 190
							action: "<b>Check credentials and try again</b>"] // library marker davegut.tpLinkCrypto, line 191
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 192
			} // library marker davegut.tpLinkCrypto, line 193
		} catch (err) { // library marker davegut.tpLinkCrypto, line 194
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkCrypto, line 195
			logData << [action: "<b>Try Configure command</b>"] // library marker davegut.tpLinkCrypto, line 196
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 197
		} // library marker davegut.tpLinkCrypto, line 198
	} else { // library marker davegut.tpLinkCrypto, line 199
		logData << [respStatus: resp.status, message: resp.errorMessage] // library marker davegut.tpLinkCrypto, line 200
		logData << [action: "<b>Try Configure command</b>"] // library marker davegut.tpLinkCrypto, line 201
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 202
	} // library marker davegut.tpLinkCrypto, line 203
} // library marker davegut.tpLinkCrypto, line 204

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkCrypto, line 206
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkCrypto, line 207
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkCrypto, line 208
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkCrypto, line 209
		setCommsError(200) // library marker davegut.tpLinkCrypto, line 210
		logDebug(logData) // library marker davegut.tpLinkCrypto, line 211
	} else { // library marker davegut.tpLinkCrypto, line 212
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkCrypto, line 213
					resp: resp.properties] // library marker davegut.tpLinkCrypto, line 214
		logInfo(logData) // library marker davegut.tpLinkCrypto, line 215
	} // library marker davegut.tpLinkCrypto, line 216
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkCrypto, line 217
} // library marker davegut.tpLinkCrypto, line 218

//	===== Comms Support ===== // library marker davegut.tpLinkCrypto, line 220
def getKlapParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 221
	Map reqParams = [timeout: 10, headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 222
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkCrypto, line 223
	state.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 224
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 225
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 226
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkCrypto, line 227
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 228

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkCrypto, line 230
									encSig, seqNo) // library marker davegut.tpLinkCrypto, line 231
	reqParams << [uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkCrypto, line 232
				  body: encryptedData.cipherData, // library marker davegut.tpLinkCrypto, line 233
				  contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 234
				  requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 235
	return reqParams // library marker davegut.tpLinkCrypto, line 236
} // library marker davegut.tpLinkCrypto, line 237

def getAesParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 239
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 240
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 241
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 242
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 243
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 244
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkCrypto, line 245
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkCrypto, line 246
					 contentType: "application/json", // library marker davegut.tpLinkCrypto, line 247
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 248
					 timeout: 10, // library marker davegut.tpLinkCrypto, line 249
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 250
	return reqParams // library marker davegut.tpLinkCrypto, line 251
} // library marker davegut.tpLinkCrypto, line 252

def parseKlapData(resp) { // library marker davegut.tpLinkCrypto, line 254
	Map parseData = [parseMethod: "parseKlapData"] // library marker davegut.tpLinkCrypto, line 255
	try { // library marker davegut.tpLinkCrypto, line 256
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 257
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 258
		int seqNo = state.seqNo // library marker davegut.tpLinkCrypto, line 259
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkCrypto, line 260
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkCrypto, line 261
														   encIv, seqNo)) // library marker davegut.tpLinkCrypto, line 262
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 263
	} catch (err) { // library marker davegut.tpLinkCrypto, line 264
		parseData << [cryptoStatus: "decryptDataError", error: err] // library marker davegut.tpLinkCrypto, line 265
	} // library marker davegut.tpLinkCrypto, line 266
	return parseData // library marker davegut.tpLinkCrypto, line 267
} // library marker davegut.tpLinkCrypto, line 268

def parseAesData(resp) { // library marker davegut.tpLinkCrypto, line 270
	Map parseData = [parseMethod: "parseAesData"] // library marker davegut.tpLinkCrypto, line 271
	try { // library marker davegut.tpLinkCrypto, line 272
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 273
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 274
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCrypto, line 275
														 encKey, encIv)) // library marker davegut.tpLinkCrypto, line 276
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 277
	} catch (err) { // library marker davegut.tpLinkCrypto, line 278
		parseData << [cryptoStatus: "decryptDataError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 279
	} // library marker davegut.tpLinkCrypto, line 280
	return parseData // library marker davegut.tpLinkCrypto, line 281
} // library marker davegut.tpLinkCrypto, line 282

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 284
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 285
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 286
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 287
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 288
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 289
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 290
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 291
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 292

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 294
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 295
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 296
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 297
} // library marker davegut.tpLinkCrypto, line 298

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 300
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 301
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 302
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 303
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 304
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 305
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 306
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 307
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 308
} // library marker davegut.tpLinkCrypto, line 309

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 311
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 312
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 313
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 314
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 315
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 316
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 317
} // library marker davegut.tpLinkCrypto, line 318

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 320
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 321
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 322
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 323
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 324
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 325
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 326
} // library marker davegut.tpLinkCrypto, line 327

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 329
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 330
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 331
	md.update(data) // library marker davegut.tpLinkCrypto, line 332
	return md.digest() // library marker davegut.tpLinkCrypto, line 333
} // library marker davegut.tpLinkCrypto, line 334

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 336
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 337
	return new String(arr) // library marker davegut.tpLinkCrypto, line 338
} // library marker davegut.tpLinkCrypto, line 339

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 341
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 342
	try { // library marker davegut.tpLinkCrypto, line 343
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 344
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 345
	} catch (error) { // library marker davegut.tpLinkCrypto, line 346
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 347
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 348
	} // library marker davegut.tpLinkCrypto, line 349
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 350
} // library marker davegut.tpLinkCrypto, line 351

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 353
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 354
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 355
	return byteValue // library marker davegut.tpLinkCrypto, line 356
} // library marker davegut.tpLinkCrypto, line 357

def getRsaKey() { // library marker davegut.tpLinkCrypto, line 359
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkCrypto, line 360
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkCrypto, line 361
} // library marker davegut.tpLinkCrypto, line 362

// ~~~~~ end include (204) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (195) davegut.Logging ~~~~~
library ( // library marker davegut.Logging, line 1
	name: "Logging", // library marker davegut.Logging, line 2
	namespace: "davegut", // library marker davegut.Logging, line 3
	author: "Dave Gutheinz", // library marker davegut.Logging, line 4
	description: "Common Logging and info gathering Methods", // library marker davegut.Logging, line 5
	category: "utilities", // library marker davegut.Logging, line 6
	documentationLink: "" // library marker davegut.Logging, line 7
) // library marker davegut.Logging, line 8

def nameSpace() { return "davegut" } // library marker davegut.Logging, line 10

def version() { return "2.4.1a" } // library marker davegut.Logging, line 12

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

// ~~~~~ end include (195) davegut.Logging ~~~~~
