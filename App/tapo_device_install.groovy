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
	def action = findTpLinkDevices("getTpLinkLanData", 5)
	def addDevicesData = atomicState.devices
	Map uninstalledDevices = [:]
	List installedDrivers = getInstalledDrivers()
	Map foundDevices = [:]
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
	String missingDevices = "<b>Exercise missing devices through the Tapo Phone "
	missingDevices += "App. Then select this function.</b>"
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
			paragraph devicesFound
			href "addDevicesPage",
				title: "<b>Rescan for Additional Devices</b>",
				description: missingDevices
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

def supportedProducts() {
	return ["SMART.TAPOBULB", "SMART.TAPOPLUG", "SMART.TAPOSWITCH","SMART.KASAHUB", 
			"SMART.TAPOHUB", "SMART.KASAPLUG", "SMART.KASASWITCH", "SMART.TAPOROBOVAC",
		    "SMART.MATTERBULB", "SMART.MATTERPLUG", "SMART.IPCAMERA"]
//		    "SMART.MATTERBULB", "SMART.MATTERPLUG", "SMART.IPCAMERA", "SMART.TAPODOORBELL"]
}

//	===== Add Devices =====
def addDevices() {
	Map logData = [method: "addDevices", selectedAddDevices: selectedAddDevices]
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
	Map logData = [method: "addDevice", dni: dni, alias: device.value.alias]
	try {
		Map deviceData = [devIp: device.value.devIp,
						  protocol: device.value.protocol,
						  baseUrl: device.value.baseUrl,
						  type: device.value.type]
		if (device.value.hasLed) { deviceData << [hasLed: device.value.hasLed] }
		if (device.value.ledVer) { deviceData << [ledVer: device.value.ledVer] }
		if (device.value.isEm) { deviceData << [isEm: device.value.isEm] }
		if (device.value.gradOnOff) { deviceData << [gradOnOff: device.value.gradOnOff] }
		if (device.value.ptz) { deviceData << [ptz: device.value.ptz] }
		if (device.value.patrol) { deviceData << [patrol: device.value.patrol] }
		if (device.value.tamperDetect) { deviceData << [tamperDetect: device.value.tamperDetect] }
		if (device.value.targetTrack) { deviceData << [targetTrack: device.value.targetTrack] }
		if (device.value.ctLow) { deviceData << [ctLow: device.value.ctLow] }
		if (device.value.ctHigh) { deviceData << [ctHigh: device.value.ctHigh] }
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
		logData << [status: "failedToAdd", device: device, errorMsg: err]
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

// ~~~~~ start include (252) davegut.appTpLinkSmart ~~~~~
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
	Map SMARTCredData = [u: userName, p: userPassword] // library marker davegut.appTpLinkSmart, line 15
	//	User Creds (username/password hashed) // library marker davegut.appTpLinkSmart, line 16
	String encUsername = mdEncode("SHA-1", userName.bytes).encodeHex().encodeAsBase64().toString() // library marker davegut.appTpLinkSmart, line 17
	app?.updateSetting("encUsername", [type: "string", value: encUsername]) // library marker davegut.appTpLinkSmart, line 18
	SMARTCredData << [encUsername: encUsername] // library marker davegut.appTpLinkSmart, line 19
	String encPassword = userPassword.trim().bytes.encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 20
	app?.updateSetting("encPassword", [type: "string", value: encPassword]) // library marker davegut.appTpLinkSmart, line 21
	SMARTCredData << [encPassword: encPassword] // library marker davegut.appTpLinkSmart, line 22
	//	vacAes Creds (password only) // library marker davegut.appTpLinkSmart, line 23
	String encPasswordVac = mdEncode("MD5", userPassword.trim().bytes).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 24
	app?.updateSetting("encPasswordVac", [type: "string", value: encPasswordVac]) // library marker davegut.appTpLinkSmart, line 25
	SMARTCredData << [encPasswordVac: encPasswordVac] // library marker davegut.appTpLinkSmart, line 26
	//	Camera Creds (password only) // library marker davegut.appTpLinkSmart, line 27
	String encPasswordCam = mdEncode("SHA-256", userPassword.trim().bytes).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 28
	app?.updateSetting("encPasswordCam", [type: "string", value: encPasswordCam]) // library marker davegut.appTpLinkSmart, line 29
	SMARTCredData << [encPasswordVac: encPasswordVac] // library marker davegut.appTpLinkSmart, line 30
	//	KLAP Local Hash // library marker davegut.appTpLinkSmart, line 31
	byte[] userHashByte = mdEncode("SHA-1", encodeUtf8(userName).getBytes()) // library marker davegut.appTpLinkSmart, line 32
	byte[] passwordHashByte = mdEncode("SHA-1", encodeUtf8(userPassword.trim()).getBytes()) // library marker davegut.appTpLinkSmart, line 33
	byte[] authHashByte = [userHashByte, passwordHashByte].flatten() // library marker davegut.appTpLinkSmart, line 34
	String authHash = mdEncode("SHA-256", authHashByte).encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 35
	app?.updateSetting("localHash", [type: "string", value: authHash]) // library marker davegut.appTpLinkSmart, line 36
	SMARTCredData << [localHash: localHash] // library marker davegut.appTpLinkSmart, line 37
	logDebug(SMARTCredData) // library marker davegut.appTpLinkSmart, line 38
	return [SMARTDevCreds: SMARTCredData] // library marker davegut.appTpLinkSmart, line 39
} // library marker davegut.appTpLinkSmart, line 40

def findTpLinkDevices(action, timeout = 10) { // library marker davegut.appTpLinkSmart, line 42
	Map logData = [method: "findTpLinkDevices", action: action, timeOut: timeout] // library marker davegut.appTpLinkSmart, line 43
	def start = state.hostArray.min().toInteger() // library marker davegut.appTpLinkSmart, line 44
	def finish = state.hostArray.max().toInteger() + 1 // library marker davegut.appTpLinkSmart, line 45
	logData << [hostArray: state.hostArray, pollSegments: state.segArray] // library marker davegut.appTpLinkSmart, line 46
	List deviceIPs = [] // library marker davegut.appTpLinkSmart, line 47
	state.segArray.each { // library marker davegut.appTpLinkSmart, line 48
		def pollSegment = it.trim() // library marker davegut.appTpLinkSmart, line 49
		logData << [pollSegment: pollSegment] // library marker davegut.appTpLinkSmart, line 50
		for(int i = start; i < finish; i++) { // library marker davegut.appTpLinkSmart, line 51
			deviceIPs.add("${pollSegment}.${i.toString()}") // library marker davegut.appTpLinkSmart, line 52
		} // library marker davegut.appTpLinkSmart, line 53
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.appTpLinkSmart, line 54
		await = sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeout) // library marker davegut.appTpLinkSmart, line 55
		atomicState.finding = true // library marker davegut.appTpLinkSmart, line 56
		int i // library marker davegut.appTpLinkSmart, line 57
		for(i = 0; i < 60; i+=5) { // library marker davegut.appTpLinkSmart, line 58
			pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 59
			if (atomicState.finding == false) { // library marker davegut.appTpLinkSmart, line 60
				logInfo("<b>FindingDevices: Finished Finding</b>") // library marker davegut.appTpLinkSmart, line 61
				pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 62
				i = 61 // library marker davegut.appTpLinkSmart, line 63
				break // library marker davegut.appTpLinkSmart, line 64
			} // library marker davegut.appTpLinkSmart, line 65
			logInfo("<b>FindingDevices: ${i} seconds</b>") // library marker davegut.appTpLinkSmart, line 66
		} // library marker davegut.appTpLinkSmart, line 67
	} // library marker davegut.appTpLinkSmart, line 68
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 69
	state.tpLinkChecked = true // library marker davegut.appTpLinkSmart, line 70
	runIn(570, resetTpLinkChecked) // library marker davegut.appTpLinkSmart, line 71
	return logData // library marker davegut.appTpLinkSmart, line 72
} // library marker davegut.appTpLinkSmart, line 73

def getTpLinkLanData(response) { // library marker davegut.appTpLinkSmart, line 75
	Map logData = [method: "getTpLinkLanData",  // library marker davegut.appTpLinkSmart, line 76
				   action: "Completed LAN Discovery", // library marker davegut.appTpLinkSmart, line 77
				   smartDevicesFound: response.size()] // library marker davegut.appTpLinkSmart, line 78
	logInfo(logData) // library marker davegut.appTpLinkSmart, line 79
	List discData = [] // library marker davegut.appTpLinkSmart, line 80
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 81
		Map devData = getDiscData(response) // library marker davegut.appTpLinkSmart, line 82
		if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 83
			discData << devData // library marker davegut.appTpLinkSmart, line 84
		} // library marker davegut.appTpLinkSmart, line 85
	} else { // library marker davegut.appTpLinkSmart, line 86
		response.each { // library marker davegut.appTpLinkSmart, line 87
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 88
			if (devData.status == "OK" && !discData.toString().contains(devData.dni)) { // library marker davegut.appTpLinkSmart, line 89
				discData << devData // library marker davegut.appTpLinkSmart, line 90
			} // library marker davegut.appTpLinkSmart, line 91
		} // library marker davegut.appTpLinkSmart, line 92
	} // library marker davegut.appTpLinkSmart, line 93
	getAllTpLinkDeviceData(discData) // library marker davegut.appTpLinkSmart, line 94
//	atomicState.finding = false // library marker davegut.appTpLinkSmart, line 95
	runIn(5, updateTpLinkDevices, [data: discData]) // library marker davegut.appTpLinkSmart, line 96
} // library marker davegut.appTpLinkSmart, line 97

def getDiscData(response) { // library marker davegut.appTpLinkSmart, line 99
	Map devData = [:] // library marker davegut.appTpLinkSmart, line 100
	try { // library marker davegut.appTpLinkSmart, line 101
		def respData = parseLanMessage(response.description) // library marker davegut.appTpLinkSmart, line 102
		if (respData.type == "LAN_TYPE_UDPCLIENT") { // library marker davegut.appTpLinkSmart, line 103
			byte[] payloadByte = hubitat.helper.HexUtils.hexStringToByteArray(respData.payload.drop(32))  // library marker davegut.appTpLinkSmart, line 104
			String payloadString = new String(payloadByte) // library marker davegut.appTpLinkSmart, line 105
			if (payloadString.length() > 1007) { // library marker davegut.appTpLinkSmart, line 106
				payloadString = payloadString + """"}}}""" // library marker davegut.appTpLinkSmart, line 107
			} // library marker davegut.appTpLinkSmart, line 108
			Map payload = new JsonSlurper().parseText(payloadString).result // library marker davegut.appTpLinkSmart, line 109
			List supported = supportedProducts() // library marker davegut.appTpLinkSmart, line 110
			String devType = payload.device_type // library marker davegut.appTpLinkSmart, line 111
			String model = payload.device_model // library marker davegut.appTpLinkSmart, line 112
			String devIp = payload.ip // library marker davegut.appTpLinkSmart, line 113
			//	Currently exculde hub model H200 - (uses camera protocol and not tested.) // library marker davegut.appTpLinkSmart, line 114
			if (supported.contains(devType) && model != "H200") { // library marker davegut.appTpLinkSmart, line 115
				//	Determine basic data based on discovery data // library marker davegut.appTpLinkSmart, line 116
				String dni = payload.mac.replaceAll("-", "") // library marker davegut.appTpLinkSmart, line 117
				String protocol = payload.mgt_encrypt_schm.encrypt_type // library marker davegut.appTpLinkSmart, line 118
				String port = payload.mgt_encrypt_schm.http_port // library marker davegut.appTpLinkSmart, line 119
				String httpStr = "http://" // library marker davegut.appTpLinkSmart, line 120
				String httpPath = "/app" // library marker davegut.appTpLinkSmart, line 121
				if (payload.mgt_encrypt_schm.is_support_https) { // library marker davegut.appTpLinkSmart, line 122
					httpStr = "https://" // library marker davegut.appTpLinkSmart, line 123
				} // library marker davegut.appTpLinkSmart, line 124
				if (devType == "SMART.IPCAMERA" || devType == "SMART.TAPODOORBELL" // library marker davegut.appTpLinkSmart, line 125
				    || model == "H200") { // library marker davegut.appTpLinkSmart, line 126
					protocol = "camera" // library marker davegut.appTpLinkSmart, line 127
					port = "443" // library marker davegut.appTpLinkSmart, line 128
				} else if (devType == "SMART.TAPOROBOVAC" && protocol == "AES") { // library marker davegut.appTpLinkSmart, line 129
					protocol = "vacAes" // library marker davegut.appTpLinkSmart, line 130
					httpPath = "" // library marker davegut.appTpLinkSmart, line 131
				} // library marker davegut.appTpLinkSmart, line 132
				String baseUrl = httpStr + devIp + ":" + port + httpPath // library marker davegut.appTpLinkSmart, line 133
				devData << [type: devType, model: model, baseUrl: baseUrl, dni: dni,  // library marker davegut.appTpLinkSmart, line 134
							ip: devIp, port: port, protocol: protocol, status: "OK"] // library marker davegut.appTpLinkSmart, line 135
			} else { // library marker davegut.appTpLinkSmart, line 136
				devData << [devIp: devIp, type: devType, model: model, status: "INVALID",  // library marker davegut.appTpLinkSmart, line 137
							reason: "Device type not supported."] // library marker davegut.appTpLinkSmart, line 138
				logWarn(devData) // library marker davegut.appTpLinkSmart, line 139
			} // library marker davegut.appTpLinkSmart, line 140
		} // library marker davegut.appTpLinkSmart, line 141
		logDebug(devData) // library marker davegut.appTpLinkSmart, line 142
	} catch (err) { // library marker davegut.appTpLinkSmart, line 143
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.appTpLinkSmart, line 144
		logWarn(devData) // library marker davegut.appTpLinkSmart, line 145
	} // library marker davegut.appTpLinkSmart, line 146
	return devData // library marker davegut.appTpLinkSmart, line 147
} // library marker davegut.appTpLinkSmart, line 148

def getAllTpLinkDeviceData(List discData) { // library marker davegut.appTpLinkSmart, line 150
	Map logData = [method: "getAllTpLinkDeviceData", discData: discData.size()] // library marker davegut.appTpLinkSmart, line 151
	discData.each { Map devData -> // library marker davegut.appTpLinkSmart, line 152
		if (devData.protocol == "KLAP") { // library marker davegut.appTpLinkSmart, line 153
			klapHandshake(devData.baseUrl, localHash, devData) // library marker davegut.appTpLinkSmart, line 154
		} else if (devData.protocol == "AES") { // library marker davegut.appTpLinkSmart, line 155
			aesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 156
		} else if (devData.protocol == "vacAes") { // library marker davegut.appTpLinkSmart, line 157
			vacAesHandshake(devData.baseUrl, userName, encPasswordVac, devData) // library marker davegut.appTpLinkSmart, line 158
		} else if (devData.protocol == "camera") { // library marker davegut.appTpLinkSmart, line 159
			cameraHandshake(devData.baseUrl, userName, encPasswordCam, devData) // library marker davegut.appTpLinkSmart, line 160
		} else {  // library marker davegut.appTpLinkSmart, line 161
			logData << [ERROR: "Unknown Protocol", discData: discData] // library marker davegut.appTpLinkSmart, line 162
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 163
		} // library marker davegut.appTpLinkSmart, line 164
		pauseExecution(1000) // library marker davegut.appTpLinkSmart, line 165
	} // library marker davegut.appTpLinkSmart, line 166
	atomicState.finding = false // library marker davegut.appTpLinkSmart, line 167
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 168
} // library marker davegut.appTpLinkSmart, line 169

def getDataCmd() { // library marker davegut.appTpLinkSmart, line 171
	List requests = [[method: "get_device_info"]] // library marker davegut.appTpLinkSmart, line 172
	requests << [method: "component_nego"] // library marker davegut.appTpLinkSmart, line 173
	Map cmdBody = [ // library marker davegut.appTpLinkSmart, line 174
		method: "multipleRequest", // library marker davegut.appTpLinkSmart, line 175
		params: [requests: requests]] // library marker davegut.appTpLinkSmart, line 176
	return cmdBody // library marker davegut.appTpLinkSmart, line 177
} // library marker davegut.appTpLinkSmart, line 178

def addToDevices(devData, cmdData) { // library marker davegut.appTpLinkSmart, line 180
	Map logData = [method: "addToDevices"] // library marker davegut.appTpLinkSmart, line 181
	String dni = devData.dni // library marker davegut.appTpLinkSmart, line 182
	def devicesData = atomicState.devices // library marker davegut.appTpLinkSmart, line 183
	devicesData.remove(dni) // library marker davegut.appTpLinkSmart, line 184
	def comps // library marker davegut.appTpLinkSmart, line 185
	def cmdResp // library marker davegut.appTpLinkSmart, line 186
	String alias // library marker davegut.appTpLinkSmart, line 187
	String tpType = devData.type // library marker davegut.appTpLinkSmart, line 188
	String model = devData.model // library marker davegut.appTpLinkSmart, line 189
	if (devData.protocol != "camera") { // library marker davegut.appTpLinkSmart, line 190
		comps = cmdData.find { it.method == "component_nego" } // library marker davegut.appTpLinkSmart, line 191
		comps = comps.result.component_list // library marker davegut.appTpLinkSmart, line 192
		cmdResp = cmdData.find { it.method == "get_device_info" } // library marker davegut.appTpLinkSmart, line 193
		cmdResp = cmdResp.result // library marker davegut.appTpLinkSmart, line 194
		byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.appTpLinkSmart, line 195
		alias = new String(plainBytes) // library marker davegut.appTpLinkSmart, line 196
		if (alias == "") { alias = model } // library marker davegut.appTpLinkSmart, line 197
	} else { // library marker davegut.appTpLinkSmart, line 198
		comps = cmdData.find { it.method == "getAppComponentList" } // library marker davegut.appTpLinkSmart, line 199
		comps = comps.result.app_component.app_component_list // library marker davegut.appTpLinkSmart, line 200
		cmdResp = cmdData.find { it.method == "getDeviceInfo" } // library marker davegut.appTpLinkSmart, line 201
		cmdResp = cmdResp.result.device_info.basic_info // library marker davegut.appTpLinkSmart, line 202
		alias = cmdResp.device_alias // library marker davegut.appTpLinkSmart, line 203
		if (alias == "") { alias = model } // library marker davegut.appTpLinkSmart, line 204
	} // library marker davegut.appTpLinkSmart, line 205
	def type = "Unknown" // library marker davegut.appTpLinkSmart, line 206
	def ctHigh // library marker davegut.appTpLinkSmart, line 207
	def ctLow // library marker davegut.appTpLinkSmart, line 208
	Map deviceData = [devIp: devData.ip, deviceType: tpType, protocol: devData.protocol, // library marker davegut.appTpLinkSmart, line 209
					  model: model, baseUrl: devData.baseUrl, alias: alias] // library marker davegut.appTpLinkSmart, line 210
	//	Determine Driver to Load // library marker davegut.appTpLinkSmart, line 211
	if (tpType.contains("PLUG") || tpType.contains("SWITCH")) { // library marker davegut.appTpLinkSmart, line 212
		type = "Plug" // library marker davegut.appTpLinkSmart, line 213
		if (comps.find { it.id == "control_child" }) { // library marker davegut.appTpLinkSmart, line 214
			type = "Parent" // library marker davegut.appTpLinkSmart, line 215
		} else if (comps.find { it.id == "dimmer" }) { // library marker davegut.appTpLinkSmart, line 216
			type = "Dimmer" // library marker davegut.appTpLinkSmart, line 217
		} // library marker davegut.appTpLinkSmart, line 218
	} else if (tpType.contains("HUB")) { // library marker davegut.appTpLinkSmart, line 219
		type = "Hub" // library marker davegut.appTpLinkSmart, line 220
	} else if (tpType.contains("BULB")) { // library marker davegut.appTpLinkSmart, line 221
		type = "Dimmer" // library marker davegut.appTpLinkSmart, line 222
		if (comps.find { it.id == "light_strip" }) { // library marker davegut.appTpLinkSmart, line 223
			type = "Lightstrip" // library marker davegut.appTpLinkSmart, line 224
		} else if (comps.find { it.id == "color" }) { // library marker davegut.appTpLinkSmart, line 225
			type = "Color Bulb" // library marker davegut.appTpLinkSmart, line 226
		} // library marker davegut.appTpLinkSmart, line 227
		if (type != "Dimmer" && comps.find { it.id == "color_temperature" } ) { // library marker davegut.appTpLinkSmart, line 228
			ctHigh = cmdResp.color_temp_range[1] // library marker davegut.appTpLinkSmart, line 229
			ctLow = cmdResp.color_temp_range[0] // library marker davegut.appTpLinkSmart, line 230
			deviceData << [ctHigh: ctHigh, ctLow: ctLow] // library marker davegut.appTpLinkSmart, line 231
		} // library marker davegut.appTpLinkSmart, line 232
	} else if (tpType.contains("ROBOVAC")) { // library marker davegut.appTpLinkSmart, line 233
		type = "Robovac" // library marker davegut.appTpLinkSmart, line 234
	} else if (tpType.contains("CAMERA") || tpType.contains("DOORBELL")) { // library marker davegut.appTpLinkSmart, line 235
		type = "Camera" // library marker davegut.appTpLinkSmart, line 236
	} // library marker davegut.appTpLinkSmart, line 237
	deviceData << [type: type] // library marker davegut.appTpLinkSmart, line 238
	//	Tapo "capabilities" imporation to drivers // library marker davegut.appTpLinkSmart, line 239
	if (comps.find { it.id == "led" } ) {  // library marker davegut.appTpLinkSmart, line 240
		String ledVer = comps.find {it.id == "led"}.ver_code // library marker davegut.appTpLinkSmart, line 241
		deviceData << [hasLed: "true", ledVer: ledVer] // library marker davegut.appTpLinkSmart, line 242
	} // library marker davegut.appTpLinkSmart, line 243
	if (comps.find { it.id == "energy_monitoring" } ) { deviceData << [isEm: "true"] } // library marker davegut.appTpLinkSmart, line 244
	if (comps.find { it.id == "on_off_gradually" } ) { deviceData << [gradOnOff: "true"] } // library marker davegut.appTpLinkSmart, line 245
	if (comps.find { it.name == "ptz" } ) { deviceData << [ptz: "true"] } // library marker davegut.appTpLinkSmart, line 246
	if (comps.find { it.name == "patrol" } ) { deviceData << [patrol: "true"] } // library marker davegut.appTpLinkSmart, line 247
	if (comps.find { it.name == "tamperDetection" } ) { deviceData << [tamperDetect: "true"] } // library marker davegut.appTpLinkSmart, line 248
	if (comps.find { it.name == "targetTrack" } ) { deviceData << [targetTrack: "true"] } // library marker davegut.appTpLinkSmart, line 249
	if (comps.find { it.name == "led"}) { // library marker davegut.appTpLinkSmart, line 250
		String ledVer = comps.find { it.name == "led" }.version // library marker davegut.appTpLinkSmart, line 251
		deviceData << [hasLed: "true", ledVer: ledVer] // library marker davegut.appTpLinkSmart, line 252
	} // library marker davegut.appTpLinkSmart, line 253
//	TEST ONLY		 // library marker davegut.appTpLinkSmart, line 254
	if (comps.find { it.name == "whiteLamp" } ) { deviceData << [whiteLamp: "true"] } // library marker davegut.appTpLinkSmart, line 255
	//	Add to devices and close out method // library marker davegut.appTpLinkSmart, line 256
	devicesData << ["${dni}": deviceData] // library marker davegut.appTpLinkSmart, line 257
	atomicState.devices = devicesData // library marker davegut.appTpLinkSmart, line 258
	logData << ["${deviceData.alias}": deviceData, dni: dni] // library marker davegut.appTpLinkSmart, line 259
	Map InfoData = ["${deviceData.alias}": "added to device data"] // library marker davegut.appTpLinkSmart, line 260
	logInfo("${deviceData.alias}: added to device data") // library marker davegut.appTpLinkSmart, line 261
	updateChild(dni, deviceData) // library marker davegut.appTpLinkSmart, line 262
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 263
//log.debug deviceData // library marker davegut.appTpLinkSmart, line 264
} // library marker davegut.appTpLinkSmart, line 265

def updateChild(dni, deviceData) { // library marker davegut.appTpLinkSmart, line 267
	def child = getChildDevice(dni) // library marker davegut.appTpLinkSmart, line 268
	if (child) { // library marker davegut.appTpLinkSmart, line 269
		child.updateChild(deviceData) // library marker davegut.appTpLinkSmart, line 270
	} // library marker davegut.appTpLinkSmart, line 271
} // library marker davegut.appTpLinkSmart, line 272

//	===== get Smart KLAP Protocol Data ===== // library marker davegut.appTpLinkSmart, line 274
def sendKlapDataCmd(handshakeData, data) { // library marker davegut.appTpLinkSmart, line 275
	if (handshakeData.respStatus != "Login OK") { // library marker davegut.appTpLinkSmart, line 276
		Map logData = [method: "sendKlapDataCmd", handshake: handshakeData] // library marker davegut.appTpLinkSmart, line 277
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 278
	} else { // library marker davegut.appTpLinkSmart, line 279
		Map reqParams = [timeout: 10, headers: ["Cookie": data.data.cookie]] // library marker davegut.appTpLinkSmart, line 280
		def seqNo = data.data.seqNo + 1 // library marker davegut.appTpLinkSmart, line 281
		String cmdBodyJson = new groovy.json.JsonBuilder(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 282
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), data.data.encKey,  // library marker davegut.appTpLinkSmart, line 283
										data.data.encIv, data.data.encSig, seqNo) // library marker davegut.appTpLinkSmart, line 284
		reqParams << [ // library marker davegut.appTpLinkSmart, line 285
			uri: "${data.data.baseUrl}/request?seq=${encryptedData.seqNumber}", // library marker davegut.appTpLinkSmart, line 286
			body: encryptedData.cipherData, // library marker davegut.appTpLinkSmart, line 287
			ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 288
			timeout:10, // library marker davegut.appTpLinkSmart, line 289
			contentType: "application/octet-stream", // library marker davegut.appTpLinkSmart, line 290
			requestContentType: "application/octet-stream"] // library marker davegut.appTpLinkSmart, line 291
		asynchttpPost("parseKlapResp", reqParams, [data: data.data]) // library marker davegut.appTpLinkSmart, line 292
	} // library marker davegut.appTpLinkSmart, line 293
} // library marker davegut.appTpLinkSmart, line 294

def parseKlapResp(resp, data) { // library marker davegut.appTpLinkSmart, line 296
	Map logData = [method: "parseKlapResp"] // library marker davegut.appTpLinkSmart, line 297
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 298
		try { // library marker davegut.appTpLinkSmart, line 299
			byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.appTpLinkSmart, line 300
			def clearResp = klapDecrypt(cipherResponse, data.data.encKey, // library marker davegut.appTpLinkSmart, line 301
										data.data.encIv, data.data.seqNo + 1) // library marker davegut.appTpLinkSmart, line 302
			Map cmdResp =  new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 303
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 304
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 305
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 306
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 307
			} else { // library marker davegut.appTpLinkSmart, line 308
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 309
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 310
			} // library marker davegut.appTpLinkSmart, line 311
		} catch (err) { // library marker davegut.appTpLinkSmart, line 312
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 313
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 314
		} // library marker davegut.appTpLinkSmart, line 315
	} else { // library marker davegut.appTpLinkSmart, line 316
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 317
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 318
	} // library marker davegut.appTpLinkSmart, line 319
} // library marker davegut.appTpLinkSmart, line 320

//	===== get Smart Camera Protocol Data ===== // library marker davegut.appTpLinkSmart, line 322
def sendCameraDataCmd(handshakeData, data) { // library marker davegut.appTpLinkSmart, line 323
	if (handshakeData.respStatus != "OK") { // library marker davegut.appTpLinkSmart, line 324
		logData << [status: "loginError"] // library marker davegut.appTpLinkSmart, line 325
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 326
	} else { // library marker davegut.appTpLinkSmart, line 327
		List requests = [[method:"getDeviceInfo", params:[device_info:[name:["basic_info"]]]], // library marker davegut.appTpLinkSmart, line 328
						 [method:"getAppComponentList", params:[app_component:[name:"app_component_list"]]]] // library marker davegut.appTpLinkSmart, line 329
	Map cmdBody = [ // library marker davegut.appTpLinkSmart, line 330
		method: "multipleRequest", // library marker davegut.appTpLinkSmart, line 331
		params: [requests: requests]] // library marker davegut.appTpLinkSmart, line 332
		def cmdStr = JsonOutput.toJson(cmdBody) // library marker davegut.appTpLinkSmart, line 333
		Map reqBody = [method: "securePassthrough", // library marker davegut.appTpLinkSmart, line 334
					   params: [request: aesEncrypt(cmdStr, handshakeData.lsk, handshakeData.ivb)]] // library marker davegut.appTpLinkSmart, line 335
		String cmdData = new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.appTpLinkSmart, line 336
		Integer seqNumber = handshakeData.seqNo // library marker davegut.appTpLinkSmart, line 337
		String initTagHex = encPasswordCam + handshakeData.cnonce // library marker davegut.appTpLinkSmart, line 338
		String initTag = mdEncode("SHA-256", initTagHex.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 339
		String tagString = initTag + cmdData + seqNumber // library marker davegut.appTpLinkSmart, line 340
		String tag =  mdEncode("SHA-256", tagString.getBytes()).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 341
		Map reqParams = [uri: handshakeData.apiUrl, // library marker davegut.appTpLinkSmart, line 342
						 body: cmdData, // library marker davegut.appTpLinkSmart, line 343
						 headers: ["Tapo_tag": tag, Seq: seqNumber], // library marker davegut.appTpLinkSmart, line 344
						 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 345
						 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 346
						 timeout: 10, // library marker davegut.appTpLinkSmart, line 347
						 ignoreSSLIssues: true] // library marker davegut.appTpLinkSmart, line 348
		asynchttpPost("parseCameraResp", reqParams, [hsData: handshakeData, devData: data]) // library marker davegut.appTpLinkSmart, line 349
	} // library marker davegut.appTpLinkSmart, line 350
} // library marker davegut.appTpLinkSmart, line 351

def parseCameraResp(resp, data) { // library marker davegut.appTpLinkSmart, line 353
	Map logData = [method: "parseCameraResp"] // library marker davegut.appTpLinkSmart, line 354
	if (resp.json.error_code == 0) { // library marker davegut.appTpLinkSmart, line 355
		resp = resp.json // library marker davegut.appTpLinkSmart, line 356
		try { // library marker davegut.appTpLinkSmart, line 357
			def clearResp = aesDecrypt(resp.result.response, data.hsData.lsk, data.hsData.ivb) // library marker davegut.appTpLinkSmart, line 358
			Map cmdResp =  new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 359
			logData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 360
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 361
				addToDevices(data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 362
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 363
			} else { // library marker davegut.appTpLinkSmart, line 364
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 365
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 366
			} // library marker davegut.appTpLinkSmart, line 367
		} catch (err) { // library marker davegut.appTpLinkSmart, line 368
			logData << [cryptoStatus: "decryptDataError", error: err] // library marker davegut.appTpLinkSmart, line 369
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 370
		} // library marker davegut.appTpLinkSmart, line 371
	} else { // library marker davegut.appTpLinkSmart, line 372
		logData << [cryptoStatus: "rerurnDataErrorCode", resp: resp] // library marker davegut.appTpLinkSmart, line 373
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 374
	} // library marker davegut.appTpLinkSmart, line 375
} // library marker davegut.appTpLinkSmart, line 376

//	===== get Smart AES Protocol Data ===== // library marker davegut.appTpLinkSmart, line 378
def getAesToken(resp, data) { // library marker davegut.appTpLinkSmart, line 379
	Map logData = [method: "getAesToken"] // library marker davegut.appTpLinkSmart, line 380
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 381
		if (resp.json.error_code == 0) { // library marker davegut.appTpLinkSmart, line 382
			try { // library marker davegut.appTpLinkSmart, line 383
				def clearResp = aesDecrypt(resp.json.result.response, data.encKey, data.encIv) // library marker davegut.appTpLinkSmart, line 384
				Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 385
				if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 386
					def token = cmdResp.result.token // library marker davegut.appTpLinkSmart, line 387
					logData << [respStatus: "OK", token: token] // library marker davegut.appTpLinkSmart, line 388
					logDebug(logData) // library marker davegut.appTpLinkSmart, line 389
					sendAesDataCmd(token, data) // library marker davegut.appTpLinkSmart, line 390
				} else { // library marker davegut.appTpLinkSmart, line 391
					logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.appTpLinkSmart, line 392
								error_code: cmdResp.error_code, // library marker davegut.appTpLinkSmart, line 393
								check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.appTpLinkSmart, line 394
					logWarn(logData) // library marker davegut.appTpLinkSmart, line 395
				} // library marker davegut.appTpLinkSmart, line 396
			} catch (err) { // library marker davegut.appTpLinkSmart, line 397
				logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.appTpLinkSmart, line 398
							error: err] // library marker davegut.appTpLinkSmart, line 399
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 400
			} // library marker davegut.appTpLinkSmart, line 401
		} else { // library marker davegut.appTpLinkSmart, line 402
			logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.appTpLinkSmart, line 403
						respJson: resp.json] // library marker davegut.appTpLinkSmart, line 404
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 405
		} // library marker davegut.appTpLinkSmart, line 406
	} else { // library marker davegut.appTpLinkSmart, line 407
		logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.appTpLinkSmart, line 408
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 409
	} // library marker davegut.appTpLinkSmart, line 410
} // library marker davegut.appTpLinkSmart, line 411

def sendAesDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 413
	def cmdStr = JsonOutput.toJson(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 414
	Map reqBody = [method: "securePassthrough", // library marker davegut.appTpLinkSmart, line 415
				   params: [request: aesEncrypt(cmdStr, data.encKey, data.encIv)]] // library marker davegut.appTpLinkSmart, line 416
	Map reqParams = [uri: "${data.baseUrl}?token=${token}", // library marker davegut.appTpLinkSmart, line 417
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.appTpLinkSmart, line 418
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 419
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 420
					 timeout: 10,  // library marker davegut.appTpLinkSmart, line 421
					 headers: ["Cookie": data.cookie]] // library marker davegut.appTpLinkSmart, line 422
	asynchttpPost("parseAesResp", reqParams, [data: data]) // library marker davegut.appTpLinkSmart, line 423
} // library marker davegut.appTpLinkSmart, line 424

def parseAesResp(resp, data) { // library marker davegut.appTpLinkSmart, line 426
	Map logData = [method: "parseAesResp"] // library marker davegut.appTpLinkSmart, line 427
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 428
		try { // library marker davegut.appTpLinkSmart, line 429
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.appTpLinkSmart, line 430
																 data.data.encKey, data.data.encIv)) // library marker davegut.appTpLinkSmart, line 431
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 432
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 433
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 434
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 435
			} else { // library marker davegut.appTpLinkSmart, line 436
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 437
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 438
			} // library marker davegut.appTpLinkSmart, line 439
		} catch (err) { // library marker davegut.appTpLinkSmart, line 440
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 441
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 442
		} // library marker davegut.appTpLinkSmart, line 443
	} else { // library marker davegut.appTpLinkSmart, line 444
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 445
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 446
	} // library marker davegut.appTpLinkSmart, line 447
} // library marker davegut.appTpLinkSmart, line 448

//	===== get Smart vacAes Protocol Data ===== // library marker davegut.appTpLinkSmart, line 450
def sendVacAesDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 451
	Map devData = data.data.devData // library marker davegut.appTpLinkSmart, line 452
	Map reqParams = getVacAesParams(getDataCmd(), "${data.data.baseUrl}/?token=${token}") // library marker davegut.appTpLinkSmart, line 453
	asynchttpPost("parseVacAesResp", reqParams, [data: devData]) // library marker davegut.appTpLinkSmart, line 454
} // library marker davegut.appTpLinkSmart, line 455

def parseVacAesResp(resp, devData) { // library marker davegut.appTpLinkSmart, line 457
	Map logData = [parseMethod: "parseVacAesResp"] // library marker davegut.appTpLinkSmart, line 458
	try { // library marker davegut.appTpLinkSmart, line 459
		Map cmdResp = resp.json // library marker davegut.appTpLinkSmart, line 460
		logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 461
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 462
				addToDevices(devData.data, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 463
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 464
			} else { // library marker davegut.appTpLinkSmart, line 465
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 466
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 467
			} // library marker davegut.appTpLinkSmart, line 468
	} catch (err) { // library marker davegut.appTpLinkSmart, line 469
		logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 470
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 471
	} // library marker davegut.appTpLinkSmart, line 472
	return parseData // library marker davegut.appTpLinkSmart, line 473
} // library marker davegut.appTpLinkSmart, line 474

//	===== Support device data update request ===== // library marker davegut.appTpLinkSmart, line 476
def tpLinkCheckForDevices(timeout = 3) { // library marker davegut.appTpLinkSmart, line 477
	Map logData = [method: "tpLinkCheckForDevices"] // library marker davegut.appTpLinkSmart, line 478
	def checked = true // library marker davegut.appTpLinkSmart, line 479
	if (state.tpLinkChecked == true) { // library marker davegut.appTpLinkSmart, line 480
		checked = false // library marker davegut.appTpLinkSmart, line 481
		logData << [status: "noCheck", reason: "Completed within last 10 minutes"] // library marker davegut.appTpLinkSmart, line 482
	} else { // library marker davegut.appTpLinkSmart, line 483
		def findData = findTpLinkDevices("parseTpLinkCheck", timeout) // library marker davegut.appTpLinkSmart, line 484
		logData << [status: "checking"] // library marker davegut.appTpLinkSmart, line 485
		pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 486
	} // library marker davegut.appTpLinkSmart, line 487
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 488
	return checked // library marker davegut.appTpLinkSmart, line 489
} // library marker davegut.appTpLinkSmart, line 490

def resetTpLinkChecked() { state.tpLinkChecked = false } // library marker davegut.appTpLinkSmart, line 492

def parseTpLinkCheck(response) { // library marker davegut.appTpLinkSmart, line 494
	List discData = [] // library marker davegut.appTpLinkSmart, line 495
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 496
		Map devdata = getDiscData(response) // library marker davegut.appTpLinkSmart, line 497
		if (devData.status != "INVALID") { // library marker davegut.appTpLinkSmart, line 498
			discData << devData // library marker davegut.appTpLinkSmart, line 499
		} // library marker davegut.appTpLinkSmart, line 500
	} else { // library marker davegut.appTpLinkSmart, line 501
		response.each { // library marker davegut.appTpLinkSmart, line 502
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 503
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 504
				discData << devData // library marker davegut.appTpLinkSmart, line 505
			} // library marker davegut.appTpLinkSmart, line 506
		} // library marker davegut.appTpLinkSmart, line 507
	} // library marker davegut.appTpLinkSmart, line 508
	atomicState.finding = false // library marker davegut.appTpLinkSmart, line 509
	updateTpLinkDevices(discData) // library marker davegut.appTpLinkSmart, line 510
} // library marker davegut.appTpLinkSmart, line 511

def updateTpLinkDevices(discData) { // library marker davegut.appTpLinkSmart, line 513
	Map logData = [method: "updateTpLinkDevices"] // library marker davegut.appTpLinkSmart, line 514
	state.tpLinkChecked = true // library marker davegut.appTpLinkSmart, line 515
	runIn(570, resetTpLinkChecked) // library marker davegut.appTpLinkSmart, line 516
	List children = getChildDevices() // library marker davegut.appTpLinkSmart, line 517
	children.each { childDev -> // library marker davegut.appTpLinkSmart, line 518
		Map childData = [:] // library marker davegut.appTpLinkSmart, line 519
		def dni = childDev.deviceNetworkId // library marker davegut.appTpLinkSmart, line 520
		def connected = "false" // library marker davegut.appTpLinkSmart, line 521
		Map devData = discData.find{ it.dni == dni } // library marker davegut.appTpLinkSmart, line 522
		if (childDev.getDataValue("baseUrl")) { // library marker davegut.appTpLinkSmart, line 523
			if (devData != null) { // library marker davegut.appTpLinkSmart, line 524
				if (childDev.getDataValue("baseUrl") == devData.baseUrl && // library marker davegut.appTpLinkSmart, line 525
				    childDev.getDataValue("protocol") == devData.protocol) { // library marker davegut.appTpLinkSmart, line 526
					childData << [status: "noChanges"] // library marker davegut.appTpLinkSmart, line 527
				} else { // library marker davegut.appTpLinkSmart, line 528
					childDev.updateDataValue("baseUrl", devData.baseUrl) // library marker davegut.appTpLinkSmart, line 529
					childDev.updateDataValue("protocol", devData.protocol) // library marker davegut.appTpLinkSmart, line 530
					childData << ["baseUrl": devData.baseUrl, // library marker davegut.appTpLinkSmart, line 531
								  "protocol": devData.protocol, // library marker davegut.appTpLinkSmart, line 532
								  "connected": "true"] // library marker davegut.appTpLinkSmart, line 533
				} // library marker davegut.appTpLinkSmart, line 534
			} else { // library marker davegut.appTpLinkSmart, line 535
				Map warnData = [method: "updateTpLinkDevices", device: childDev, // library marker davegut.appTpLinkSmart, line 536
								connected: "false", reason: "not Discovered By App"] // library marker davegut.appTpLinkSmart, line 537
				logWarn(warnData) // library marker davegut.appTpLinkSmart, line 538
			} // library marker davegut.appTpLinkSmart, line 539
			pauseExecution(500) // library marker davegut.appTpLinkSmart, line 540
		} // library marker davegut.appTpLinkSmart, line 541
		logData << ["${childDev}": childData] // library marker davegut.appTpLinkSmart, line 542
	} // library marker davegut.appTpLinkSmart, line 543
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 544
} // library marker davegut.appTpLinkSmart, line 545

// ~~~~~ end include (252) davegut.appTpLinkSmart ~~~~~

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

// ~~~~~ start include (266) davegut.tpLinkTransAes ~~~~~
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
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkTransAes, line 131
	return reqParams // library marker davegut.tpLinkTransAes, line 132
} // library marker davegut.tpLinkTransAes, line 133

def parseAesData(resp, data) { // library marker davegut.tpLinkTransAes, line 135
	Map parseData = [parseMethod: "parseAesData", sourceMethod: data.data] // library marker davegut.tpLinkTransAes, line 136
	try { // library marker davegut.tpLinkTransAes, line 137
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkTransAes, line 138
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkTransAes, line 139
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkTransAes, line 140
														 encKey, encIv)) // library marker davegut.tpLinkTransAes, line 141
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkTransAes, line 142
	} catch (err) { // library marker davegut.tpLinkTransAes, line 143
		parseData << [cryptoStatus: "decryptDataError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkTransAes, line 144
	} // library marker davegut.tpLinkTransAes, line 145
	return parseData // library marker davegut.tpLinkTransAes, line 146
} // library marker davegut.tpLinkTransAes, line 147

def getRsaKey() { // library marker davegut.tpLinkTransAes, line 149
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkTransAes, line 150
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkTransAes, line 151
} // library marker davegut.tpLinkTransAes, line 152

// ~~~~~ end include (266) davegut.tpLinkTransAes ~~~~~

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

// ~~~~~ start include (267) davegut.tpLinkTransKlap ~~~~~
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

// ~~~~~ end include (267) davegut.tpLinkTransKlap ~~~~~

// ~~~~~ start include (268) davegut.tpLinkTransVacAes ~~~~~
library ( // library marker davegut.tpLinkTransVacAes, line 1
	name: "tpLinkTransVacAes", // library marker davegut.tpLinkTransVacAes, line 2
	namespace: "davegut", // library marker davegut.tpLinkTransVacAes, line 3
	author: "Compiled by Dave Gutheinz", // library marker davegut.tpLinkTransVacAes, line 4
	description: "Handshake methods for TP-Link Integration", // library marker davegut.tpLinkTransVacAes, line 5
	category: "utilities", // library marker davegut.tpLinkTransVacAes, line 6
	documentationLink: "" // library marker davegut.tpLinkTransVacAes, line 7
) // library marker davegut.tpLinkTransVacAes, line 8

//	===== Login ===== // library marker davegut.tpLinkTransVacAes, line 10
def vacAesHandshake(baseUrl, userName, encPasswordVac, devData = null) {  // library marker davegut.tpLinkTransVacAes, line 11
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.tpLinkTransVacAes, line 12
	if (device) { // library marker davegut.tpLinkTransVacAes, line 13
		userName = parent.userName // library marker davegut.tpLinkTransVacAes, line 14
		encPasswordVac = parent.encPasswordVac // library marker davegut.tpLinkTransVacAes, line 15
	} // library marker davegut.tpLinkTransVacAes, line 16
	Map cmdBody = [method: "login", // library marker davegut.tpLinkTransVacAes, line 17
				   params: [hashed: true,  // library marker davegut.tpLinkTransVacAes, line 18
							password: encPasswordVac, // library marker davegut.tpLinkTransVacAes, line 19
							username: userName]] // library marker davegut.tpLinkTransVacAes, line 20
	Map reqParams = getVacAesParams(cmdBody, baseUrl) // library marker davegut.tpLinkTransVacAes, line 21
	asynchttpPost("parseVacAesLogin", reqParams, [data: reqData]) // library marker davegut.tpLinkTransVacAes, line 22
} // library marker davegut.tpLinkTransVacAes, line 23

def parseVacAesLogin(resp, data) { // library marker davegut.tpLinkTransVacAes, line 25
	Map logData = [method: "parseVacAesLogin", oldToken: token] // library marker davegut.tpLinkTransVacAes, line 26
	if (resp.status == 200 && resp.json != null) { // library marker davegut.tpLinkTransVacAes, line 27
		def newToken = resp.json.result.token // library marker davegut.tpLinkTransVacAes, line 28
		logData << [status: "OK", token: newToken] // library marker davegut.tpLinkTransVacAes, line 29
		if (device) { // library marker davegut.tpLinkTransVacAes, line 30
			device.updateSetting("token", [type: "string", value: newToken]) // library marker davegut.tpLinkTransVacAes, line 31
			setCommsError(200) // library marker davegut.tpLinkTransVacAes, line 32
		} else { // library marker davegut.tpLinkTransVacAes, line 33
			sendVacAesDataCmd(newToken, data) // library marker davegut.tpLinkTransVacAes, line 34
		}			 // library marker davegut.tpLinkTransVacAes, line 35
		logDebug(logData) // library marker davegut.tpLinkTransVacAes, line 36
	} else { // library marker davegut.tpLinkTransVacAes, line 37
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkTransVacAes, line 38
		logWarn(logData) // library marker davegut.tpLinkTransVacAes, line 39
	} // library marker davegut.tpLinkTransVacAes, line 40
} // library marker davegut.tpLinkTransVacAes, line 41

def getVacAesParams(cmdBody, url) { // library marker davegut.tpLinkTransVacAes, line 43
	Map reqParams = [uri: url, // library marker davegut.tpLinkTransVacAes, line 44
					 body: cmdBody, // library marker davegut.tpLinkTransVacAes, line 45
					 contentType: "application/json", // library marker davegut.tpLinkTransVacAes, line 46
					 requestContentType: "application/json", // library marker davegut.tpLinkTransVacAes, line 47
					 ignoreSSLIssues: true, // library marker davegut.tpLinkTransVacAes, line 48
					 timeout: 10] // library marker davegut.tpLinkTransVacAes, line 49
	return reqParams	 // library marker davegut.tpLinkTransVacAes, line 50
} // library marker davegut.tpLinkTransVacAes, line 51

def parseVacAesData(resp, data) { // library marker davegut.tpLinkTransVacAes, line 53
	Map parseData = [parseMethod: "parseVacAesData", sourceMethod: data.data] // library marker davegut.tpLinkTransVacAes, line 54
	try { // library marker davegut.tpLinkTransVacAes, line 55
		parseData << [cryptoStatus: "OK", cmdResp: resp.json] // library marker davegut.tpLinkTransVacAes, line 56
		logDebug(parseData) // library marker davegut.tpLinkTransVacAes, line 57
	} catch (err) { // library marker davegut.tpLinkTransVacAes, line 58
		parseData << [cryptoStatus: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkTransVacAes, line 59
		logWarn(parseData) // library marker davegut.tpLinkTransVacAes, line 60
		handleCommsError() // library marker davegut.tpLinkTransVacAes, line 61
	} // library marker davegut.tpLinkTransVacAes, line 62
	return parseData // library marker davegut.tpLinkTransVacAes, line 63
} // library marker davegut.tpLinkTransVacAes, line 64


// ~~~~~ end include (268) davegut.tpLinkTransVacAes ~~~~~

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
