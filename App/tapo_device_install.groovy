/*	Multi-TP-Link Product Integration Application
	Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md
===== Link to Documentation =====
	https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/Documentation.pdf

The code below is adaptable between four cases to allow the developer a single environment
supporting the range of TP-LINK Kasa and Tapo devices.  This is necessitated by the
manufacturer's transition of their KASA devices from their IOT API to the API used for
their TAPO Product line (herein called TP-LINK API for clarity).

Four Options for NAME in Definition:
a.	Kasa Integration: Integrates Legacy (IOT API) Kasa plugs and switches and bulbs
b.	KasaSmart Integration.  Integrates Legacy devices AND TP-LINK API plugs,
	switches, and hub.  The hub has a child Tnermostat Radiator Valve (TRV) child.
	Future-proofed to include Bulbs if they later are Remodeled from Tapo.
c.	Tapo Integration.  Integrates all TAPO labeled plugs, switches, bulbs, and hubs
	(including hub child devices).  For backward compatibility with the Community
	integration, adds support for current KASA TP-LInk API devices.
d.	TpLink Integration.  All of the above except the Tapo robovac (this will be available
	as a community DRIVER (No app required).

The integration does not currently support Cameras, Doorbells, wifi Thermostat, nor
the RoboVac.  I anticipate some level of support for the Thermostat when it finally is
released by TP-Link.

Identification of the current KASA TP-LINK API devices (from the Android APK File).
This field will grow as Kasa manuactures new device versions (it appears they are
just updating the hardware packaging and firmware name for TAPO devices to Kasa).
1.	SMART_TAPO_REMODEL_KASA_HUB_KH100 (works with Kasa TRV)
2.	SMART_TAPO_REMODEL_KASA_PLUG_EP25
3.	SMART_TAPO_REMODEL_KASA_PLUG_KP125M (Matter)
4.	SMART_TAPO_REMODEL_KASA_POWER_STRIP_EP40M
5.	SMART_TAPO_REMODEL_KASA_SWITCH_HS200
6.	SMART_TAPO_REMODEL_KASA_SWITCH_HS220 (Dimmer)
7.	SMART_TAPO_REMODEL_KASA_SWITCH_KS205 (Matter)
8.	SMART_TAPO_REMODEL_KASA_SWITCH_KS225 (Dimmer, Matter)
9.	SMART_TAPO_REMODEL_KASA_SWITCH_KS240 (Dual Fan Control/Dimmer)

Application Changes.  Essentially a new app with most code coming from the EXISITING
built-in App as well as the Community Tapo integration.  Uses Libraries to break out
common methods from methods specific to the IOT and TP-LINK APIs.  Major changes:
a.	Common data in the atomicState.devices for all devices.
b.	Updated device polling from the drivers (part of error handling) with separate polling
	for each API.
c.	Reorganized Start Page to accommodate accommodate this drivers multi-integration.
	(This is readily modified if the TpLink version is settled on for all devices.)
d.	Two if conditions uses sparingly: if !"Kasa Integration" and if !"Tapo Integration".
e.	product lookup method to select products (type) for each app NAME.

Kasa (Legacy) drivers:  Minor change to error handling to work properly (was broken in
						determining new IP address). "Configure" improvements (will checl
						app to update IP addresses on LAN devices).
New TP-LINK drivers:	Based on community integration.  Added Fan Control.
========================================*/
def appVer() { return version() }
//#include davegut.appKasaIOT				//	name = Kasa, KasaSmart, TpLink Integration
			//	name = Tapo, Kasa Smart, TpLink Integration
			//	name = Tapo, Kasa Smart, TpLink Integration
			//	name = Tapo, Kasa Smart, TpLink Integration


import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.json.JSONObject	//	Check all imports after testing is complete.

definition(
//	name: "Kasa Integration",
//	name: "KasaSmart Integration",
	name: "Tapo Integration",
//	name: "TpLink Integration",
	namespace: nameSpace(),
	author: "Dave Gutheinz",
	description: "Application to install TP-Link bulbs, plugs, and switches.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	installOnOpen: true,
	singleInstance: true,
	documentationLink: "https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/README.md",
	importUrl: "https://raw.githubusercontent.com/DaveGut/HubitatActive/master/KasaDevices/Application/KasaIntegrationApp.groovy"
)

preferences {
	page(name: "startPage")
	page(name: "enterCredentialsPage")
	page(name: "processCredentials")
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
	app?.updateSetting("ports", [type:"string", value: "9999"])
	app?.updateSetting("hostLimits", [type:"string", value: "2, 254"])
	if (app.getLabel() != "Tapo Integration") {
		app?.updateSetting("kasaToken", "INVALID")
		app?.updateSetting("useKasaCloud", false)
	}
	if (app.getLabel() != "Kasa Integration") {
		app?.updateSetting("encPassword", "INVALID")
		app?.updateSetting("encUsername", "INVALID")
		app?.updateSetting("localHash", "INVALID")
	}
	logInfo([method: "installed", status: "Initialized settings"])
}

def updated() {
	app?.removeSetting("selectedAddDevices")
	app?.removeSetting("selectedRemoveDevices")
	app?.updateSetting("logEnable", false)
	app?.updateSetting("appSetup", false)
	app?.updateSetting("startApp", true)
	app?.updateSetting("scheduled", false)
	state.needCreds = false
	scheduleItems()
	logInfo([method: "updated", status: "setting updated for new session"])
}

def scheduleItems() {
	Map logData = [method: "scheduleItems"]
	unschedule()
	if (useKasaCloud) {
		schedule("0 30 2 ? * MON,WED,SAT", getKasaToken)
		logData << [kasaCloudLogin: "scheduled MON, WED, SAT"]
	}
	app?.updateSetting("scheduled", false)
	logData << setLogsOff()
	//	Clean databases
	logData << [databases: "trimmed"]
	def devicesData = atomicState.devices
	Map childDevices = [:]
	devicesData.each { device ->
		if (getChildDevice(device.key)) { childDevices << device }
	}
	atomicState.devices = childDevices
//	atomicState.devices = [:]
	atomicState.iotDevices = [:]
	atomicState.tpLinkDevices = [:]
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
	if (appVersion != appVer()) {
		app?.updateSetting("appVersion", [type:"string", value: appVer()])
		if (!atomicState.devices) { atomicState.devices = [:] }
		if (!atomicState.tpLinkDevices) { atomicState.tpLinkDevices = [:] }
		if (!atomicState.iotDevices) { atomicState.iotDevices = [:] }
		if (!state.needCreds) { state.needCreds = false }
		if (app.getLabel() != "Tapo Integration") {	state.kasaChecked = false }
		if (app.getLabel() != "Kasa Integration") { state.tpLinkChecked = false }
		if (app.getLabel() != "Tapo Integration" && !useKasaCloud) {
			def kasaTokenUpd = "INVALID"
			def useCloud = false
			if (kasaToken && kasaToken != "INVALID") {
				useCloud = true
				kasTokenUpd = kasaToken
			}
			app?.updateSetting("useKasaCloud", useCloud)
			app?.updateSetting("kasaToken", kasaTokenUpd)
		}
		setSegments()
		logInfo([method: "initInstance", status: "App data updated for appVersion ${appVer()}"])
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
		state.portArray = ports.split('\\,')
		def rangeArray = hostLimits.split('\\,')
		def array0 = rangeArray[0].toInteger()
		def array1 = array0 + 2
		if (rangeArray.size() > 1) {
			array1 = rangeArray[1].toInteger()
		}
	state.hostArray = [array0, array1]
	} catch (e) {
		logWarn("startPage: Invalid entry for Lan Segements, Host Array Range, or Ports. Resetting to default!")
		def hub = location.hubs[0]
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
		app?.updateSetting("ports", [type:"string", value: "9999"])
		app?.updateSetting("hostLimits", [type:"string", value: "1, 254"])
	}
}

def startPage() {
	logInfo([method: "startPage", status: "Starting ${app.getLabel()} Setup"])
	def action = initInstance()
	if (selectedRemoveDevices) { removeDevices() } 
	else if (selectedAddDevices) { addDevices() }
	return dynamicPage(name:"startPage",
					   title:"<b>${app.getLabel()}</b>",
					   uninstall: true,
					   install: true) {
		section() {
			Map lanParams = [LanSegments: state.segArray, Ports: state.portArray, hostRange: 
						   state.hostArray]
			String params = "<b>Application Setup Parameters</b>"
			if (app.getLabel() != "Tapo Integration") {
				params += "\n\t<b>useKasaCloud</b>: ${useKasaCloud}"
				params += ", <b>kasaToken</b>: ${kasaToken}"
			}
			params += "\n\t<b>lanDiscoveryParams</b>: ${lanParams}"
			paragraph params
			input "appSetup", "bool", title: "<b>Modify Application Setup</b> (useCloud, LanDiscParams)",
				submitOnChange: true, defaultValue: false
			if (appSetup) {
				if (app.getLabel() != "Tapo Integration") {
					input "useKasaCloud", "bool", title: "<b>Use the Kasa Cloud</b>",
						description:  "Use the Kasa Cloud to control legacy devices.  Usually used only with discovery issues",
						submitOnChange: true
				}
				if (!useKasaCloud) { app?.updateSetting("kasaToken", "INVALID") }
				input "lanSegment", "string",
					title: "<b>Lan Segments</b> (ex: 192.168.50, 192,168.01)", submitOnChange: true
				input "hostLimits", "string",
					title: "<b>Host Address Range</b> (ex: 5, 100)", submitOnChange: true
				input "ports", "string",
					title: "<b>Ports for Port Forwarding</b> (ex: 9999, 8000)", submitOnChange: true
			}
			def credDesc = "Credentials: userName: ${userName}, password set/redacted"
			if (useKasaCloud) { credDesc += ", CloudToken: ${kasaToken}" }
			def addDesc = "Install ALL Bulbs and Switches"
			if (!userName || !userPassword) {
				credDesc = "<b>Credentials not set.  Enter credentials to proceed.</b>"
				addDesc = "Install only legacy Kasa Bulbs and Switches"
				if (app.getLabel() != "Kasa Integration") { state.needCreds = true }
			} else {
				if (app.getLabel() != "Kasa Integration") { state.needCreds = false }
			}
			if (kasaToken == "INVALID" && useKasaCloud) {
				credDesc += "\n<b>Token requires update for kasaCloud control of devices."
				credDesc += " Two Factor identification MUST be disabled in Kasa App</b>."
			}
			if ((useKasaCloud && app.getLabel() == "Kasa Integration") ||
				app.getLabel() != "Kasa Integration") {
				href "enterCredentialsPage",
					title: "<b>Enter/Update Username and Password</b>",
					description: credDesc
			}
			if (!state.needCreds) {
				href "addDevicesPage",
					title: "<b>Scan for devices and add</b>",
					description: addDesc
			} else {
				paragraph "<b>Credentials are required to scan for to find devices</b>"
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
			input "showPassword", "bool",
				title: "<b>Show Password</b>",
				submitOnChange: true,
				defaultValue: false
			def pwdType = "password"
			if (showPassword) { pwdType = "string" }
			input ("userName", "email",
            		title: "Email Address", 
                    required: false,
                    submitOnChange: false)
			input ("userPassword", pwdType,
            		title: "Account Password",
                    required: false,
                    submitOnChange: false)
			paragraph "<b>You must select Update Credentials before exiting this page.</b>"
			input "updateCredentials", "bool",
				title: "<b>Update Credentials</b>",
				submitOnChange: true,
				defaultValue: false
			if (updateCredentials && userName && userPassword && 
				userName != null && userPassword != null) {
				credData << processCredentials()
			}
			paragraph "Current derived credData: ${credData}"
		}
	}
}

private processCredentials() {
	Map logData = [method: "processCredentials", userName: userName, userPassword: userPassword]
	Map credData = [:]
	if (app.getLabel() != "Kasa Integration") {
		credData << createTpLinkCreds()
	}
	if (useKasaCloud) {
		credData << getKasaToken()
	}
	logData << credData
	logInfo(logData)
	app?.updateSetting("updateCredentials", false)
	return credData
}

//	===== Add selected newdevices =====
def addDevicesPage() {
	logDebug("addDevicesPage")
	app?.removeSetting("selectedAddDevices")
	def action = findDevices(8)
	def addDevicesData = atomicState.devices
	def uninstalledDevices = [:]
	addDevicesData.each {
		def isChild = getChildDevice(it.key)
		if (!isChild) {
			if (it.value.prefs.toString().contains("matter")) {
				uninstalledDevices["${it.key}"] = "${it.value.alias}, ${it.value.type}, (matterDevice)"
			} else {
				uninstalledDevices["${it.key}"] = "${it.value.alias}, ${it.value.type}"
			}
		}
	}
	uninstalledDevices.sort()
	
	def deviceList = []
	if (addDevicesData == null) {
		deviceList << "<b>No Devices in addDevicesData.</b>]"
	} else {
		addDevicesData.each{
			def dni = it.key
			def result = ["Failed", "n/a"]
			def installed = "No"
			def isChild = getChildDevice(it.key)
			if (isChild) {
				installed = "Yes"
			}
			if (it.value.ip) {
				deviceList << "<b>${it.value.alias}</b>: ${it.value.ip}:${it.value.port}, ${installed}"
			} else if (it.value.baseUrl) {
				deviceList << "<b>${it.value.alias}</b>: ${it.value.baseUrl}, ${installed}"
			}
		}
	}
	deviceList.sort()
	def theList = ""
	deviceList.each {
		theList += "${it}\n"
	}

	return dynamicPage(name:"addDevicesPage",
					   title: "Add Devices to Hubitat",
					   nextPage: startPage,
					   install: false) {
	 	section() {
			input ("selectedAddDevices", "enum",
				   required: false,
				   multiple: true,
				   title: "Devices to add (${uninstalledDevices.size() ?: 0} available).\n\t" +
				   "Total Devices: ${addDevicesData.size()}",
				   description: "Use the dropdown to select devices.  Then select 'Done'.",
				   options: uninstalledDevices)
			paragraph "<b>Found Devices: (Alias: Ip:Port, Installed?)</b>\r<p style='font-size:14px'>${theList}</p>"
			href "addDevicesPage",
				title: "<b>Rescan for Additional Devices</b>",
				description: "<b>Perform scan again to try to capture missing devices.</b>"
		}
	}
}

def findDevices(timeout) {
	def allDevices = atomicState.devices
	Map logData = [method: "findDevices", intType: app.getLabel()]
	if (app.getLabel() != "Tapo Integration") {
		logInfo("findDevices: Finding KASA LAN Devices")
		logData << [findIOTDevices: findIOTDevices("getIOTLanData", timeout)]
		if (useKasaCloud) {
			logInfo("findDevices: Found ${atomicState.iotDevices.size()} KASA Devices")
			logData << [findCloudIOTDevices: findCloudIOTDevices()]
		}
	}
	if (app.getLabel() != "Kasa Integration") {
		logInfo("findDevices: Finding TP-Link LAN Devices")
		logData << [findTpLinkDevices: findTpLinkDevices("getTpLinkLanData", timeout)]
	}
	allDevices = allDevices + atomicState.iotDevices + atomicState.tpLinkDevices
	atomicState.devices = allDevices
//	if (!legacyUpdated) {
//		logData << [updateLegacyDevices: updateLegacyDevices()]
//	}
	logInfo(logData)
	return
}

def supportedProducts() {
	List supported = []
	if (app.getLabel() == "Kasa Integration") {
		supported = ["IOT.SMARTBULB", "IOT.SMARTPLUGSWITCH"]
	} else if (app.getLabel() == "KasaSmart Integration") {
		supported = ["IOT.SMARTBULB", "IOT.SMARTPLUGSWITCH", "SMART.KASAPLUG",
					 "SMART.KASASWITCH", "SMART.KASAHUB"]
	} else if (app.getLabel() == "Tapo Integration") {
		supported = ["SMART.TAPOBULB", "SMART.TAPOPLUG", "SMART.TAPOSWITCH",
					 "SMART.KASAHUB", "SMART.TAPOHUB", "SMART.KASAPLUG",
					 "SMART.KASASWITCH", "SMART.TAPOROBOVAC"]
	} else if (app.getLabel() == "TpLink Integration") {
		supported = ["IOT.SMARTBULB", "IOT.SMARTPLUGSWITCH", "SMART.KASAPLUG", "SMART.KASASWITCH",
					"SMART.TAPOBULB", "SMART.TAPOPLUG", "SMART.TAPOSWITCH", 
					"SMART.KASAHUB", "SMART.TAPOHUB", "SMART.TAPOROBOVAC"]
	}
	return supported
}

def updateLegacyDevices() {
//	Update basic data on legacy devices
//	app?.updateSetting("legacyUpdated", true) //	move to updated.  Allows multiple discoveries.
	
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
		if (device.value.protocol == "IOT") {
			deviceData << [deviceIP: device.value.ip,
						   deviceId: device.value.deviceId,
						   devicePort:  device.value.port,
						   feature: feature]
		} else {
			deviceData << [baseUrl: device.value.baseUrl,
						   model: device.value.model,
						   type: device.value.type]
		}

		if (device.value.plugNo != null) {
			deviceData << [plugNo: device.value.plugNo,
						   plugId: device.value.plugId]
			}
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
					"name" : device.value.deviceType,
					"data" : deviceData
				]
			)
			logData << [status: "added"]
			logInfo(logData)
		} catch (err) {
			logData << [status: "failedToAdd", driver: device.value.type, errorMsg: error]
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
	logData << [childDevices: installedDevices]
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
	def devicesData = atomicState.devices
	selectedRemoveDevices.each { dni ->
		def device = devicesData.find { it.key == dni }
		def isChild = getChildDevice(dni)
		if (isChild) {
			try {
				deleteChildDevice(dni)
				logData << ["${dni}": [status: "ok", alias: device.value.alias]]
			} catch (error) {
				logData << ["${dni}": [status: "FAILED", alias: device.value.alias, error: error]]
				logWarn(logData)
			}
		}
	}
	app?.removeSetting("selectedRemoveDevices")
	logInfo(logData)
}

def appCheckDevices() {
	Map logData = [method: "appCheckDevices"]
	if (app.getLabel() != "Tapo Integration") {
		if (state.kasaChecked == false) {
			logData << [kasaCheck: findIOTDevices("getIOTLanData", 5)]
			state.kasaChecked = true
			runIn(570, resetKasaChecked)
			logData << [kasaCheck: "Running"]
		} else {
			logData << [kasaCheck: "notRun. Ran within last 10 minutes"]
		}
	}
	if (app.getLabel() != "Kasa Integration") {
		if (state.tpLinkChecked == false) {
			logData << [tpLinkCheck: findTpLinkDevices("parseTpLinkCheck", 5)]
			state.tpLinkChecked = true
			runIn(570, resetTpLinkChecked)
			logData << [tpLinkCheck: "Running"]
		} else {
			logData << [kasaCheck: "notRun. Ran within last 10 minutes"]
		}
	}
	logInfo(logData)
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

// ~~~~~ start include (23) davegut.appTpLinkSmart ~~~~~
library ( // library marker davegut.appTpLinkSmart, line 1
	name: "appTpLinkSmart", // library marker davegut.appTpLinkSmart, line 2
	namespace: "davegut", // library marker davegut.appTpLinkSmart, line 3
	author: "Dave Gutheinz", // library marker davegut.appTpLinkSmart, line 4
	description: "Discovery library for Application support the Kasa IOT devices.", // library marker davegut.appTpLinkSmart, line 5
	category: "utilities", // library marker davegut.appTpLinkSmart, line 6
	documentationLink: "" // library marker davegut.appTpLinkSmart, line 7
) // library marker davegut.appTpLinkSmart, line 8
import org.json.JSONObject // library marker davegut.appTpLinkSmart, line 9
import groovy.json.JsonOutput // library marker davegut.appTpLinkSmart, line 10
import groovy.json.JsonBuilder // library marker davegut.appTpLinkSmart, line 11
import groovy.json.JsonSlurper // library marker davegut.appTpLinkSmart, line 12

//	SMART Credentials // library marker davegut.appTpLinkSmart, line 14
def createTpLinkCreds() { // library marker davegut.appTpLinkSmart, line 15
	Map SMARTCredData = [:] // library marker davegut.appTpLinkSmart, line 16

	String encUsername = mdEncode("SHA-1", userName.bytes).encodeHex().encodeAsBase64().toString() // library marker davegut.appTpLinkSmart, line 18
	app?.updateSetting("encUsername", [type: "password", value: encUsername]) // library marker davegut.appTpLinkSmart, line 19
	SMARTCredData << [encUsername: encUsername] // library marker davegut.appTpLinkSmart, line 20

	String encPassword = userPassword.bytes.encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 22
	app?.updateSetting("encPassword", [type: "password", value: encPassword]) // library marker davegut.appTpLinkSmart, line 23
	SMARTCredData << [encPassword: encPassword] // library marker davegut.appTpLinkSmart, line 24

	String encPasswordVac = mdEncode("MD5", userPassword.bytes).encodeHex().toString().toUpperCase() // library marker davegut.appTpLinkSmart, line 26
	app?.updateSetting("encPasswordVac", [type: "password", value: encPasswordVac]) // library marker davegut.appTpLinkSmart, line 27
	SMARTCredData << [encPasswordVac: encPasswordVac] // library marker davegut.appTpLinkSmart, line 28

	def userHash = mdEncode("SHA-1", encodeUtf8(userName).getBytes()) // library marker davegut.appTpLinkSmart, line 30
	def passwordHash = mdEncode("SHA-1", encodeUtf8(userPassword).getBytes()) // library marker davegut.appTpLinkSmart, line 31
	byte[] klapLocalHash = [userHash, passwordHash].flatten() // library marker davegut.appTpLinkSmart, line 32
	String localHash = mdEncode("SHA-256", klapLocalHash).encodeBase64().toString() // library marker davegut.appTpLinkSmart, line 33
	app?.updateSetting("localHash", [type: "password", value: localHash]) // library marker davegut.appTpLinkSmart, line 34
	SMARTCredData << [localHash: localHash] // library marker davegut.appTpLinkSmart, line 35

	return [SMARTDevCreds: SMARTCredData] // library marker davegut.appTpLinkSmart, line 37
} // library marker davegut.appTpLinkSmart, line 38

def findTpLinkDevices(action, timeout = 10) { // library marker davegut.appTpLinkSmart, line 40
	Map logData = [method: "findTpLinkDevices", action: action, timeOut: timeout] // library marker davegut.appTpLinkSmart, line 41
	def start = state.hostArray.min().toInteger() // library marker davegut.appTpLinkSmart, line 42
	def finish = state.hostArray.max().toInteger() + 1 // library marker davegut.appTpLinkSmart, line 43
	logData << [hostArray: state.hostArray, pollSegment: state.segArray] // library marker davegut.appTpLinkSmart, line 44
	List deviceIPs = [] // library marker davegut.appTpLinkSmart, line 45
	state.segArray.each { // library marker davegut.appTpLinkSmart, line 46
		def pollSegment = it.trim() // library marker davegut.appTpLinkSmart, line 47
		logData << [pollSegment: pollSegment] // library marker davegut.appTpLinkSmart, line 48
           for(int i = start; i < finish; i++) { // library marker davegut.appTpLinkSmart, line 49
			deviceIPs.add("${pollSegment}.${i.toString()}") // library marker davegut.appTpLinkSmart, line 50
		} // library marker davegut.appTpLinkSmart, line 51
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.appTpLinkSmart, line 52
		await = sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeout) // library marker davegut.appTpLinkSmart, line 53

		logDebug("findTpLinkDevices: [startFinding:[segment: ${pollSegment}]]") // library marker davegut.appTpLinkSmart, line 55
		app?.updateSetting("finding", true) // library marker davegut.appTpLinkSmart, line 56
		int i // library marker davegut.appTpLinkSmart, line 57
		for(i = 0; i < 30; i++) { // library marker davegut.appTpLinkSmart, line 58
			if (i == 29) { // library marker davegut.appTpLinkSmart, line 59
				logWarn("findTpLinkDevices: [findingError:[segment: ${pollSegment}]]") // library marker davegut.appTpLinkSmart, line 60
			} // library marker davegut.appTpLinkSmart, line 61
			pauseExecution(1000) // library marker davegut.appTpLinkSmart, line 62
			if (finding == false) { // library marker davegut.appTpLinkSmart, line 63
				pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 64
				i = 31 // library marker davegut.appTpLinkSmart, line 65
			} // library marker davegut.appTpLinkSmart, line 66
		} // library marker davegut.appTpLinkSmart, line 67
		logDebug("<b>findTpLinkDevices: [segment: ${pollSegment}, finding: complete]</b>") // library marker davegut.appTpLinkSmart, line 68
	} // library marker davegut.appTpLinkSmart, line 69
	app?.updateSetting("finding", false) // library marker davegut.appTpLinkSmart, line 70
	return logData // library marker davegut.appTpLinkSmart, line 71
} // library marker davegut.appTpLinkSmart, line 72

def getTpLinkLanData(response) { // library marker davegut.appTpLinkSmart, line 74
	Map logData = [method: "getTpLinkLanData", responses: response.size()] // library marker davegut.appTpLinkSmart, line 75
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 76
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


	app?.updateSetting("finding", false) // library marker davegut.appTpLinkSmart, line 94
	runIn(30, updateTpLinkDevices, [data: discData]) // library marker davegut.appTpLinkSmart, line 95
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 96
} // library marker davegut.appTpLinkSmart, line 97

def getDiscData(response) { // library marker davegut.appTpLinkSmart, line 99
	Map devData = [method: "getDiscData"] // library marker davegut.appTpLinkSmart, line 100
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
			if (supported.contains(payload.device_type)) { // library marker davegut.appTpLinkSmart, line 111
				def protocol = payload.mgt_encrypt_schm.encrypt_type // library marker davegut.appTpLinkSmart, line 112
				def port = payload.mgt_encrypt_schm.http_port // library marker davegut.appTpLinkSmart, line 113
				def dni = payload.mac.replaceAll("-", "") // library marker davegut.appTpLinkSmart, line 114
				def baseUrl = "http://${payload.ip}:${payload.mgt_encrypt_schm.http_port}/app" // library marker davegut.appTpLinkSmart, line 115
				if (payload.device_type == "SMART.TAPOROBOVAC") { // library marker davegut.appTpLinkSmart, line 116
					baseUrl = "https://${payload.ip}:${payload.mgt_encrypt_schm.http_port}" // library marker davegut.appTpLinkSmart, line 117
					protocol = "vacAes" // library marker davegut.appTpLinkSmart, line 118
				} // library marker davegut.appTpLinkSmart, line 119
				devData << [ // library marker davegut.appTpLinkSmart, line 120
					type: payload.device_type, model: payload.device_model, // library marker davegut.appTpLinkSmart, line 121
					baseUrl: baseUrl, dni: dni, devId: payload.device_id,  // library marker davegut.appTpLinkSmart, line 122
					ip: payload.ip, port: port, protocol: protocol, status: "OK"] // library marker davegut.appTpLinkSmart, line 123
			} else { // library marker davegut.appTpLinkSmart, line 124
				devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.appTpLinkSmart, line 125
							status: "INVALID", reason: "Device not supported."] // library marker davegut.appTpLinkSmart, line 126
//				logInfo(devData) // library marker davegut.appTpLinkSmart, line 127
			} // library marker davegut.appTpLinkSmart, line 128
		} // library marker davegut.appTpLinkSmart, line 129
		logInfo(devData) // library marker davegut.appTpLinkSmart, line 130
	} catch (err) { // library marker davegut.appTpLinkSmart, line 131
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.appTpLinkSmart, line 132
		logDebug(devData) // library marker davegut.appTpLinkSmart, line 133
	} // library marker davegut.appTpLinkSmart, line 134
	return devData // library marker davegut.appTpLinkSmart, line 135
} // library marker davegut.appTpLinkSmart, line 136

def getAllTpLinkDeviceData(List discData) { // library marker davegut.appTpLinkSmart, line 138
	Map logData = [method: "getAllTpLinkDeviceData"] // library marker davegut.appTpLinkSmart, line 139
	discData.each { Map devData -> // library marker davegut.appTpLinkSmart, line 140
		if (devData.protocol == "KLAP") { // library marker davegut.appTpLinkSmart, line 141
			klapHandshake(devData.baseUrl, localHash, devData) // library marker davegut.appTpLinkSmart, line 142
		} else if (devData.protocol == "AES") { // library marker davegut.appTpLinkSmart, line 143
			aesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 144
		} else if (devData.protocol == "vacAes") { // library marker davegut.appTpLinkSmart, line 145
			vacAesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 146
		} // library marker davegut.appTpLinkSmart, line 147
		pauseExecution(500) // library marker davegut.appTpLinkSmart, line 148
	} // library marker davegut.appTpLinkSmart, line 149
} // library marker davegut.appTpLinkSmart, line 150

def getDataCmd() { // library marker davegut.appTpLinkSmart, line 152
	List requests = [[method: "get_device_info"]] // library marker davegut.appTpLinkSmart, line 153
	requests << [method: "component_nego"] // library marker davegut.appTpLinkSmart, line 154
	Map cmdBody = [ // library marker davegut.appTpLinkSmart, line 155
		method: "multipleRequest", // library marker davegut.appTpLinkSmart, line 156
		params: [requests: requests]] // library marker davegut.appTpLinkSmart, line 157
	return cmdBody // library marker davegut.appTpLinkSmart, line 158
} // library marker davegut.appTpLinkSmart, line 159

def addToDevices(devData, cmdResp) { // library marker davegut.appTpLinkSmart, line 161
	Map logData = [method: "addToDevices"] // library marker davegut.appTpLinkSmart, line 162
	String dni = devData.dni // library marker davegut.appTpLinkSmart, line 163
	def components = cmdResp.find { it.method == "component_nego" } // library marker davegut.appTpLinkSmart, line 164
	cmdResp = cmdResp.find { it.method == "get_device_info" } // library marker davegut.appTpLinkSmart, line 165
	cmdResp = cmdResp.result // library marker davegut.appTpLinkSmart, line 166
	byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.appTpLinkSmart, line 167
	def alias = new String(plainBytes) // library marker davegut.appTpLinkSmart, line 168
	if (alias == "") { alias = cmdResp.model } // library marker davegut.appTpLinkSmart, line 169
	def ctHigh // library marker davegut.appTpLinkSmart, line 170
	def ctLow // library marker davegut.appTpLinkSmart, line 171
	def driver = "Plug" // library marker davegut.appTpLinkSmart, line 172
	def comps = components.result.component_list // library marker davegut.appTpLinkSmart, line 173
	comps.each { cap -> // library marker davegut.appTpLinkSmart, line 174
		switch(cap.id) { // library marker davegut.appTpLinkSmart, line 175
			case "control_child": // library marker davegut.appTpLinkSmart, line 176
				if (devData.type.contains("HUB")) { // library marker davegut.appTpLinkSmart, line 177
					driver = "Hub" // library marker davegut.appTpLinkSmart, line 178
				} else if (devData.type.contains("PLUG") || devData.type.contains("SWITCH")) { // library marker davegut.appTpLinkSmart, line 179
					driver = "Parent" // library marker davegut.appTpLinkSmart, line 180
				} // library marker davegut.appTpLinkSmart, line 181
				break // library marker davegut.appTpLinkSmart, line 182
			case "energy_monitoring": // library marker davegut.appTpLinkSmart, line 183
				driver = "Plug EM" // library marker davegut.appTpLinkSmart, line 184
				break // library marker davegut.appTpLinkSmart, line 185
			case "brightness":  // library marker davegut.appTpLinkSmart, line 186
				if (!cmdResp.color_temp_range) { // library marker davegut.appTpLinkSmart, line 187
					driver = "Dimmer" // library marker davegut.appTpLinkSmart, line 188
				} // library marker davegut.appTpLinkSmart, line 189
				break // library marker davegut.appTpLinkSmart, line 190
			case "color": // library marker davegut.appTpLinkSmart, line 191
				if (comps.find {it.id == "light_strip" }) { // library marker davegut.appTpLinkSmart, line 192
					driver = "Lightstrip" // library marker davegut.appTpLinkSmart, line 193
				} else { // library marker davegut.appTpLinkSmart, line 194
					driver = "Color Bulb" // library marker davegut.appTpLinkSmart, line 195
				} // library marker davegut.appTpLinkSmart, line 196
				break // library marker davegut.appTpLinkSmart, line 197
			case "color_temperature": // library marker davegut.appTpLinkSmart, line 198
				ctHigh = cmdResp.color_temp_range[1] // library marker davegut.appTpLinkSmart, line 199
				ctLow = cmdResp.color_temp_range[0] // library marker davegut.appTpLinkSmart, line 200
				break // library marker davegut.appTpLinkSmart, line 201
			case "clean": // library marker davegut.appTpLinkSmart, line 202
				driver = "Robovac" // library marker davegut.appTpLinkSmart, line 203
				break // library marker davegut.appTpLinkSmart, line 204
		} // library marker davegut.appTpLinkSmart, line 205
	} // library marker davegut.appTpLinkSmart, line 206
	def devicesData = atomicState.tpLinkDevices // library marker davegut.appTpLinkSmart, line 207
	Map deviceData = [deviceType: devData.type, protocol: devData.protocol,  // library marker davegut.appTpLinkSmart, line 208
					  model: devData.model, baseUrl: devData.baseUrl, alias: alias,  // library marker davegut.appTpLinkSmart, line 209
					  type: driver] // library marker davegut.appTpLinkSmart, line 210
	if (ctHigh != null) { // library marker davegut.appTpLinkSmart, line 211
		deviceData << [ctHigh: ctHigh, ctLow: ctLow] // library marker davegut.appTpLinkSmart, line 212
	} // library marker davegut.appTpLinkSmart, line 213
	devicesData << ["${dni}": deviceData] // library marker davegut.appTpLinkSmart, line 214

	atomicState.tpLinkDevices = devicesData // library marker davegut.appTpLinkSmart, line 216
	logData << ["${deviceData.alias}": deviceData, dni: dni] // library marker davegut.appTpLinkSmart, line 217
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 218
} // library marker davegut.appTpLinkSmart, line 219

//	===== get Smart KLAP Protocol Data ===== // library marker davegut.appTpLinkSmart, line 221
def sendKlapDataCmd(handshakeData, data) { // library marker davegut.appTpLinkSmart, line 222
	if (handshakeData.respStatus != "Login OK") { // library marker davegut.appTpLinkSmart, line 223
		Map logData = [method: "sendKlapDataCmd", handshake: handshakeData] // library marker davegut.appTpLinkSmart, line 224
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 225
	} else { // library marker davegut.appTpLinkSmart, line 226
		Map reqParams = [timeout: 10, headers: ["Cookie": data.data.cookie]] // library marker davegut.appTpLinkSmart, line 227
		def seqNo = data.data.seqNo + 1 // library marker davegut.appTpLinkSmart, line 228
		String cmdBodyJson = new groovy.json.JsonBuilder(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 229
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), data.data.encKey,  // library marker davegut.appTpLinkSmart, line 230
										data.data.encIv, data.data.encSig, seqNo) // library marker davegut.appTpLinkSmart, line 231
		reqParams << [uri: "${data.data.baseUrl}/request?seq=${encryptedData.seqNumber}", // library marker davegut.appTpLinkSmart, line 232
					  body: encryptedData.cipherData, // library marker davegut.appTpLinkSmart, line 233
					  contentType: "application/octet-stream", // library marker davegut.appTpLinkSmart, line 234
					  requestContentType: "application/octet-stream"] // library marker davegut.appTpLinkSmart, line 235
		asyncPost(reqParams, "parseKlapResp", data.data) // library marker davegut.appTpLinkSmart, line 236
	} // library marker davegut.appTpLinkSmart, line 237
} // library marker davegut.appTpLinkSmart, line 238

def parseKlapResp(resp, data) { // library marker davegut.appTpLinkSmart, line 240
	Map logData = [method: "parseKlapResp"] // library marker davegut.appTpLinkSmart, line 241
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 242
		try { // library marker davegut.appTpLinkSmart, line 243
			byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.appTpLinkSmart, line 244
			def clearResp = klapDecrypt(cipherResponse, data.data.encKey, // library marker davegut.appTpLinkSmart, line 245
										data.data.encIv, data.data.seqNo + 1) // library marker davegut.appTpLinkSmart, line 246
			Map cmdResp =  new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 247
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 248
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 249
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 250
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 251
			} else { // library marker davegut.appTpLinkSmart, line 252
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 253
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 254
			} // library marker davegut.appTpLinkSmart, line 255
		} catch (err) { // library marker davegut.appTpLinkSmart, line 256
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 257
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 258
		} // library marker davegut.appTpLinkSmart, line 259
	} else { // library marker davegut.appTpLinkSmart, line 260
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 261
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 262
	} // library marker davegut.appTpLinkSmart, line 263
} // library marker davegut.appTpLinkSmart, line 264

//	===== get Smart AES Protocol Data ===== // library marker davegut.appTpLinkSmart, line 266
def getAesToken(resp, data) { // library marker davegut.appTpLinkSmart, line 267
	Map logData = [method: "getAesToken"] // library marker davegut.appTpLinkSmart, line 268
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 269
		if (resp.json.error_code == 0) { // library marker davegut.appTpLinkSmart, line 270
			try { // library marker davegut.appTpLinkSmart, line 271
				def clearResp = aesDecrypt(resp.json.result.response, data.encKey, data.encIv) // library marker davegut.appTpLinkSmart, line 272
				Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 273
				if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 274
					def token = cmdResp.result.token // library marker davegut.appTpLinkSmart, line 275
					logData << [respStatus: "OK", token: token] // library marker davegut.appTpLinkSmart, line 276
					logDebug(logData) // library marker davegut.appTpLinkSmart, line 277
					sendAesDataCmd(token, data) // library marker davegut.appTpLinkSmart, line 278
				} else { // library marker davegut.appTpLinkSmart, line 279
					logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.appTpLinkSmart, line 280
								error_code: cmdResp.error_code, // library marker davegut.appTpLinkSmart, line 281
								check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.appTpLinkSmart, line 282
					logWarn(logData) // library marker davegut.appTpLinkSmart, line 283
				} // library marker davegut.appTpLinkSmart, line 284
			} catch (err) { // library marker davegut.appTpLinkSmart, line 285
				logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.appTpLinkSmart, line 286
							error: err] // library marker davegut.appTpLinkSmart, line 287
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 288
			} // library marker davegut.appTpLinkSmart, line 289
		} else { // library marker davegut.appTpLinkSmart, line 290
			logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.appTpLinkSmart, line 291
						respJson: resp.json] // library marker davegut.appTpLinkSmart, line 292
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 293
		} // library marker davegut.appTpLinkSmart, line 294
	} else { // library marker davegut.appTpLinkSmart, line 295
		logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.appTpLinkSmart, line 296
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 297
	} // library marker davegut.appTpLinkSmart, line 298
} // library marker davegut.appTpLinkSmart, line 299

def sendAesDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 301
	def cmdStr = JsonOutput.toJson(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 302
	Map reqBody = [method: "securePassthrough", // library marker davegut.appTpLinkSmart, line 303
				   params: [request: aesEncrypt(cmdStr, data.encKey, data.encIv)]] // library marker davegut.appTpLinkSmart, line 304
	Map reqParams = [uri: "${data.baseUrl}?token=${token}", // library marker davegut.appTpLinkSmart, line 305
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.appTpLinkSmart, line 306
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 307
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 308
					 timeout: 10,  // library marker davegut.appTpLinkSmart, line 309
					 headers: ["Cookie": data.cookie]] // library marker davegut.appTpLinkSmart, line 310
	asyncPost(reqParams, "parseAesResp", data) // library marker davegut.appTpLinkSmart, line 311
} // library marker davegut.appTpLinkSmart, line 312

def parseAesResp(resp, data) { // library marker davegut.appTpLinkSmart, line 314
	Map logData = [method: "parseAesResp"] // library marker davegut.appTpLinkSmart, line 315
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 316
		try { // library marker davegut.appTpLinkSmart, line 317
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.appTpLinkSmart, line 318
																 data.data.encKey, data.data.encIv)) // library marker davegut.appTpLinkSmart, line 319
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 320
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 321
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 322
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 323
			} else { // library marker davegut.appTpLinkSmart, line 324
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 325
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 326
			} // library marker davegut.appTpLinkSmart, line 327
		} catch (err) { // library marker davegut.appTpLinkSmart, line 328
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 329
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 330
		} // library marker davegut.appTpLinkSmart, line 331
	} else { // library marker davegut.appTpLinkSmart, line 332
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 333
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 334
	} // library marker davegut.appTpLinkSmart, line 335
} // library marker davegut.appTpLinkSmart, line 336

//	===== get Smart VAC Protocol Data ===== // library marker davegut.appTpLinkSmart, line 338
def vacAesHandshake(baseUrl, devData) { // library marker davegut.appTpLinkSmart, line 339
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.appTpLinkSmart, line 340
	Map cmdBody = [method: "login", // library marker davegut.appTpLinkSmart, line 341
				   params: [hashed: true,  // library marker davegut.appTpLinkSmart, line 342
							password: encPasswordVac, // library marker davegut.appTpLinkSmart, line 343
							username: userName]] // library marker davegut.appTpLinkSmart, line 344
	Map reqParams = [uri: baseUrl, // library marker davegut.appTpLinkSmart, line 345
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 346
					 body: cmdBody, // library marker davegut.appTpLinkSmart, line 347
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 348
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 349
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 350
	asyncPost(reqParams, "parseVacAesLogin", reqData) // library marker davegut.appTpLinkSmart, line 351
} // library marker davegut.appTpLinkSmart, line 352

def parseVacAesLogin(resp, data) { // library marker davegut.appTpLinkSmart, line 354
	Map logData = [method: "parseVacAesLogin", oldToken: token] // library marker davegut.appTpLinkSmart, line 355
	if (resp.status == 200 && resp.json != null) { // library marker davegut.appTpLinkSmart, line 356
		logData << [status: "OK"] // library marker davegut.appTpLinkSmart, line 357
		logData << [token: resp.json.result.token] // library marker davegut.appTpLinkSmart, line 358
		sendVacDataCmd(resp.json.result.token, data) // library marker davegut.appTpLinkSmart, line 359
		logDebug(logData) // library marker davegut.appTpLinkSmart, line 360
	} else { // library marker davegut.appTpLinkSmart, line 361
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.appTpLinkSmart, line 362
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 363
	} // library marker davegut.appTpLinkSmart, line 364
} // library marker davegut.appTpLinkSmart, line 365

def sendVacDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 367
	Map devData = data.data.devData // library marker davegut.appTpLinkSmart, line 368
	Map reqParams = [uri: "${data.data.baseUrl}/?token=${token}", // library marker davegut.appTpLinkSmart, line 369
					 body: getDataCmd(), // library marker davegut.appTpLinkSmart, line 370
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 371
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 372
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 373
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 374
	asyncPost(reqParams, "parseVacResp", devData) // library marker davegut.appTpLinkSmart, line 375
} // library marker davegut.appTpLinkSmart, line 376

def parseVacResp(resp, devData) { // library marker davegut.appTpLinkSmart, line 378
	Map logData = [parseMethod: "parseVacResp"] // library marker davegut.appTpLinkSmart, line 379
	try { // library marker davegut.appTpLinkSmart, line 380
		Map cmdResp = resp.json // library marker davegut.appTpLinkSmart, line 381
		logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 382
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 383
				addToDevices(devData.data, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 384
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 385
			} else { // library marker davegut.appTpLinkSmart, line 386
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 387
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 388
			} // library marker davegut.appTpLinkSmart, line 389
	} catch (err) { // library marker davegut.appTpLinkSmart, line 390
		logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 391
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 392
	} // library marker davegut.appTpLinkSmart, line 393
	return parseData	 // library marker davegut.appTpLinkSmart, line 394
} // library marker davegut.appTpLinkSmart, line 395

//	===== Update TpLink Child Data =====F // library marker davegut.appTpLinkSmart, line 397
def tpLinkCheckForDevices(timeout = 5) { // library marker davegut.appTpLinkSmart, line 398
	Map logData = [method: "tpLinkCheckForDevices"] // library marker davegut.appTpLinkSmart, line 399
	if (state.tpLinkChecked == true) { // library marker davegut.appTpLinkSmart, line 400
		logData << [status: "noCheck", reason: "Completed within last 10 minutes"] // library marker davegut.appTpLinkSmart, line 401
	} else { // library marker davegut.appTpLinkSmart, line 402
		def findData = findTpLinkDevices("parseTpLinkCheck", timeout) // library marker davegut.appTpLinkSmart, line 403
		logData << [status: "checking"] // library marker davegut.appTpLinkSmart, line 404
	} // library marker davegut.appTpLinkSmart, line 405
	return logData // library marker davegut.appTpLinkSmart, line 406
} // library marker davegut.appTpLinkSmart, line 407

def resetTpLinkChecked() { state.tpLinkChecked = false } // library marker davegut.appTpLinkSmart, line 409

def parseTpLinkCheck(response) { // library marker davegut.appTpLinkSmart, line 411
	List discData = [] // library marker davegut.appTpLinkSmart, line 412
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 413
		Map devdata = getDiscData(response) // library marker davegut.appTpLinkSmart, line 414
		if (devData.status != "INVALID") { // library marker davegut.appTpLinkSmart, line 415
			discData << devData // library marker davegut.appTpLinkSmart, line 416
		} // library marker davegut.appTpLinkSmart, line 417
	} else { // library marker davegut.appTpLinkSmart, line 418
		response.each { // library marker davegut.appTpLinkSmart, line 419
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 420
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 421
				discData << devData // library marker davegut.appTpLinkSmart, line 422
			} // library marker davegut.appTpLinkSmart, line 423
		} // library marker davegut.appTpLinkSmart, line 424
	} // library marker davegut.appTpLinkSmart, line 425
	updateTpLinkDevices(discData) // library marker davegut.appTpLinkSmart, line 426
} // library marker davegut.appTpLinkSmart, line 427

def updateTpLinkDevices(discData) { // library marker davegut.appTpLinkSmart, line 429
	Map logData = [method: "updateTpLinkDevices"] // library marker davegut.appTpLinkSmart, line 430
	state.tpLinkChecked = true // library marker davegut.appTpLinkSmart, line 431
	runIn(570, resetTpLinkChecked) // library marker davegut.appTpLinkSmart, line 432
	List children = getChildDevices() // library marker davegut.appTpLinkSmart, line 433
	children.each { childDev -> // library marker davegut.appTpLinkSmart, line 434
		Map childData = [:] // library marker davegut.appTpLinkSmart, line 435
		def dni = childDev.deviceNetworkId // library marker davegut.appTpLinkSmart, line 436
		def connected = "false" // library marker davegut.appTpLinkSmart, line 437
		Map devData = discData.find{ it.dni == dni } // library marker davegut.appTpLinkSmart, line 438
		if (childDev.getDataValue("baseUrl")) { // library marker davegut.appTpLinkSmart, line 439
			if (devData != null) { // library marker davegut.appTpLinkSmart, line 440
				if (childDev.getDataValue("baseUrl") == devData.baseUrl && // library marker davegut.appTpLinkSmart, line 441
				    childDev.getDataValue("protocol") == devData.protocol) { // library marker davegut.appTpLinkSmart, line 442
					childData << [status: "noChanges"] // library marker davegut.appTpLinkSmart, line 443
				} else { // library marker davegut.appTpLinkSmart, line 444
					childDev.updateDataValue("baseUrl", devData.baseUrl) // library marker davegut.appTpLinkSmart, line 445
					childDev.updateDataValue("protocol", devData.protocol) // library marker davegut.appTpLinkSmart, line 446
					childData << ["baseUrl": devData.baseUrl, // library marker davegut.appTpLinkSmart, line 447
								  "protocol": devData.protocol, // library marker davegut.appTpLinkSmart, line 448
								  "connected": "true"] // library marker davegut.appTpLinkSmart, line 449
				} // library marker davegut.appTpLinkSmart, line 450
			} else { // library marker davegut.appTpLinkSmart, line 451
				childData << [connected: "false", reason: "not Discovered By App"] // library marker davegut.appTpLinkSmart, line 452
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 453
			} // library marker davegut.appTpLinkSmart, line 454
			pauseExecution(500) // library marker davegut.appTpLinkSmart, line 455
		} // library marker davegut.appTpLinkSmart, line 456
		logData << ["${childDev}": childData] // library marker davegut.appTpLinkSmart, line 457
	} // library marker davegut.appTpLinkSmart, line 458
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 459
} // library marker davegut.appTpLinkSmart, line 460

// ~~~~~ end include (23) davegut.appTpLinkSmart ~~~~~

// ~~~~~ start include (36) davegut.tpLinkComms ~~~~~
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
		runIn(1, handleCommsError, [data: logData]) // library marker davegut.tpLinkComms, line 37
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
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.tpLinkComms, line 53
		logWarn(logData) // library marker davegut.tpLinkComms, line 54
		handleCommsError("httpFailure") // library marker davegut.tpLinkComms, line 55
	} // library marker davegut.tpLinkComms, line 56
	return logData // library marker davegut.tpLinkComms, line 57
} // library marker davegut.tpLinkComms, line 58

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 60
def handleCommsError(retryReason) { // library marker davegut.tpLinkComms, line 61
	Map logData = [method: "handleCommsError", retryReason: retryReason] // library marker davegut.tpLinkComms, line 62
	if (state.lastCmd != "") { // library marker davegut.tpLinkComms, line 63
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 64
		state.errorCount = count // library marker davegut.tpLinkComms, line 65
		logData << [count: count, lastCmd: state.lastCmd] // library marker davegut.tpLinkComms, line 66
		switch (count) { // library marker davegut.tpLinkComms, line 67
			case 1: // library marker davegut.tpLinkComms, line 68
				logData << [action: "resendCommand"] // library marker davegut.tpLinkComms, line 69
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 70
				break // library marker davegut.tpLinkComms, line 71
			case 2: // library marker davegut.tpLinkComms, line 72
				logData << [attemptHandshake: deviceHandshake(), // library marker davegut.tpLinkComms, line 73
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 74
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 75
				break // library marker davegut.tpLinkComms, line 76
			case 3: // library marker davegut.tpLinkComms, line 77
				logData << [configure: configure(), // library marker davegut.tpLinkComms, line 78
						    action: "resendCommand"] // library marker davegut.tpLinkComms, line 79
				runIn(1, delayedPassThrough) // library marker davegut.tpLinkComms, line 80
			case 4: // library marker davegut.tpLinkComms, line 81
				logData << [setError: setCommsError(true), retries: "disabled"] // library marker davegut.tpLinkComms, line 82
				break // library marker davegut.tpLinkComms, line 83
			default: // library marker davegut.tpLinkComms, line 84
				logData << [retries: "disabled"] // library marker davegut.tpLinkComms, line 85
				break // library marker davegut.tpLinkComms, line 86
		} // library marker davegut.tpLinkComms, line 87
	} else { // library marker davegut.tpLinkComms, line 88
		logData << [status: "noCommandToRetry"] // library marker davegut.tpLinkComms, line 89
	} // library marker davegut.tpLinkComms, line 90
	logDebug(logData) // library marker davegut.tpLinkComms, line 91
} // library marker davegut.tpLinkComms, line 92

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 94
	def cmdData = new JSONObject(state.lastCmd) // library marker davegut.tpLinkComms, line 95
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 96
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 97
} // library marker davegut.tpLinkComms, line 98

def setCommsError(status) { // library marker davegut.tpLinkComms, line 100
	if (device.currentValue("commsError") == true && status == false) { // library marker davegut.tpLinkComms, line 101
		updateAttr("commsError", false) // library marker davegut.tpLinkComms, line 102
		setPollInterval() // library marker davegut.tpLinkComms, line 103
		logInfo([method: "setCommsError", result: "Comms Error set to false"]) // library marker davegut.tpLinkComms, line 104
	} else if (device.currentValue("commsError") == false && status == true) { // library marker davegut.tpLinkComms, line 105
		updateAttr("commsError", true) // library marker davegut.tpLinkComms, line 106
		setPollInterval("30 min") // library marker davegut.tpLinkComms, line 107
		logWarn([method: "setCommsError", result: "Comms Error Set to true"]) // library marker davegut.tpLinkComms, line 108
	} // library marker davegut.tpLinkComms, line 109
} // library marker davegut.tpLinkComms, line 110

// ~~~~~ end include (36) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (37) davegut.tpLinkCrypto ~~~~~
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
						logDebug(logData) // library marker davegut.tpLinkCrypto, line 100
					} else { // library marker davegut.tpLinkCrypto, line 101
						logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.tpLinkCrypto, line 102
									error_code: cmdResp.error_code, // library marker davegut.tpLinkCrypto, line 103
									check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.tpLinkCrypto, line 104
						logWarn(logData) // library marker davegut.tpLinkCrypto, line 105
					} // library marker davegut.tpLinkCrypto, line 106
				} catch (err) { // library marker davegut.tpLinkCrypto, line 107
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.tpLinkCrypto, line 108
								error: err] // library marker davegut.tpLinkCrypto, line 109
					logWarn(logData) // library marker davegut.tpLinkCrypto, line 110
				} // library marker davegut.tpLinkCrypto, line 111
			} else { // library marker davegut.tpLinkCrypto, line 112
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.tpLinkCrypto, line 113
							respJson: resp.json] // library marker davegut.tpLinkCrypto, line 114
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 115
			} // library marker davegut.tpLinkCrypto, line 116
		} else { // library marker davegut.tpLinkCrypto, line 117
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.tpLinkCrypto, line 118
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 119
		} // library marker davegut.tpLinkCrypto, line 120
	} else { // library marker davegut.tpLinkCrypto, line 121
		getAesToken(resp, data.data) // library marker davegut.tpLinkCrypto, line 122
	} // library marker davegut.tpLinkCrypto, line 123
} // library marker davegut.tpLinkCrypto, line 124

//	===== KLAP Handshake ===== // library marker davegut.tpLinkCrypto, line 126
def klapHandshake(baseUrl = getDataValue("baseUrl"), localHash = parent.localHash, devData = null) { // library marker davegut.tpLinkCrypto, line 127
	byte[] localSeed = new byte[16] // library marker davegut.tpLinkCrypto, line 128
	new Random().nextBytes(localSeed) // library marker davegut.tpLinkCrypto, line 129
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkCrypto, line 130
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkCrypto, line 131
					 body: localSeed, // library marker davegut.tpLinkCrypto, line 132
					 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 133
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 134
					 timeout:10] // library marker davegut.tpLinkCrypto, line 135
	asyncPost(reqParams, "parseKlapHandshake", reqData) // library marker davegut.tpLinkCrypto, line 136
} // library marker davegut.tpLinkCrypto, line 137

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkCrypto, line 139
	Map logData = [method: "parseKlapHandshake", data: data] // library marker davegut.tpLinkCrypto, line 140
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 141
		try { // library marker davegut.tpLinkCrypto, line 142
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 143
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkCrypto, line 144
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkCrypto, line 145
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkCrypto, line 146
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkCrypto, line 147
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkCrypto, line 148
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 149
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkCrypto, line 150
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkCrypto, line 151
				//	cookie // library marker davegut.tpLinkCrypto, line 152
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 153
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 154
				logData << [cookie: cookie] // library marker davegut.tpLinkCrypto, line 155
				//	seqNo and encIv // library marker davegut.tpLinkCrypto, line 156
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 157
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 158
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkCrypto, line 159

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkCrypto, line 161
				atomicState.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 162

//				if (device) {  // library marker davegut.tpLinkCrypto, line 164
//				} // library marker davegut.tpLinkCrypto, line 165

				logData << [seqNo: seqNo, encIv: fullIv[0..11]] // library marker davegut.tpLinkCrypto, line 167
				//	encKey // library marker davegut.tpLinkCrypto, line 168
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 169
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkCrypto, line 170
				logData << [encKey: encKey] // library marker davegut.tpLinkCrypto, line 171
				//	encSig // library marker davegut.tpLinkCrypto, line 172
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 173
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkCrypto, line 174
				if (device) { // library marker davegut.tpLinkCrypto, line 175
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkCrypto, line 176
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkCrypto, line 177
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkCrypto, line 178
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkCrypto, line 179
				} else { // library marker davegut.tpLinkCrypto, line 180
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkCrypto, line 181
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkCrypto, line 182
				} // library marker davegut.tpLinkCrypto, line 183
				logData << [encSig: encSig] // library marker davegut.tpLinkCrypto, line 184
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 185
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkCrypto, line 186
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkCrypto, line 187
								 body: body, // library marker davegut.tpLinkCrypto, line 188
								 timeout:10, // library marker davegut.tpLinkCrypto, line 189
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 190
								 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 191
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 192
				asyncPost(reqParams, "parseKlapHandshake2", reqData) // library marker davegut.tpLinkCrypto, line 193
			} else { // library marker davegut.tpLinkCrypto, line 194
				logData << [respStatus: "ERROR: locakAuthHash != serverHash", // library marker davegut.tpLinkCrypto, line 195
							localAuthHash: localAuthHash, serverHash: serverHash] // library marker davegut.tpLinkCrypto, line 196
				logWarn(logData) // library marker davegut.tpLinkCrypto, line 197
			} // library marker davegut.tpLinkCrypto, line 198
		} catch (err) { // library marker davegut.tpLinkCrypto, line 199
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkCrypto, line 200
			logWarn(logData) // library marker davegut.tpLinkCrypto, line 201
		} // library marker davegut.tpLinkCrypto, line 202
	} else { // library marker davegut.tpLinkCrypto, line 203
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 204
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 205
	} // library marker davegut.tpLinkCrypto, line 206
} // library marker davegut.tpLinkCrypto, line 207

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkCrypto, line 209
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkCrypto, line 210
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkCrypto, line 211
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkCrypto, line 212
		logDebug(logData) // library marker davegut.tpLinkCrypto, line 213
	} else { // library marker davegut.tpLinkCrypto, line 214
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkCrypto, line 215
					resp: resp.properties] // library marker davegut.tpLinkCrypto, line 216
		logWarn(logData) // library marker davegut.tpLinkCrypto, line 217
	} // library marker davegut.tpLinkCrypto, line 218
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkCrypto, line 219
} // library marker davegut.tpLinkCrypto, line 220

//	===== Comms Support ===== // library marker davegut.tpLinkCrypto, line 222
def getKlapParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 223
	Map reqParams = [timeout: 10, headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 224
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkCrypto, line 225
	state.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 226
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 227
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 228
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkCrypto, line 229
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 230

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkCrypto, line 232
									encSig, seqNo) // library marker davegut.tpLinkCrypto, line 233
	reqParams << [uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkCrypto, line 234
				  body: encryptedData.cipherData, // library marker davegut.tpLinkCrypto, line 235
				  contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 236
				  requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 237
	return reqParams // library marker davegut.tpLinkCrypto, line 238
} // library marker davegut.tpLinkCrypto, line 239

def getAesParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 241
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 242
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 243
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 244
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 245
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 246
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkCrypto, line 247
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkCrypto, line 248
					 contentType: "application/json", // library marker davegut.tpLinkCrypto, line 249
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 250
					 timeout: 10, // library marker davegut.tpLinkCrypto, line 251
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 252
	return reqParams // library marker davegut.tpLinkCrypto, line 253
} // library marker davegut.tpLinkCrypto, line 254

def parseKlapData(resp) { // library marker davegut.tpLinkCrypto, line 256
	Map parseData = [parseMethod: "parseKlapData"] // library marker davegut.tpLinkCrypto, line 257
	try { // library marker davegut.tpLinkCrypto, line 258
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 259
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 260
		int seqNo = state.seqNo // library marker davegut.tpLinkCrypto, line 261
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkCrypto, line 262
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkCrypto, line 263
														   encIv, seqNo)) // library marker davegut.tpLinkCrypto, line 264
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 265
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 266
		setCommsError(false) // library marker davegut.tpLinkCrypto, line 267
		logDebug(parseData) // library marker davegut.tpLinkCrypto, line 268
	} catch (err) { // library marker davegut.tpLinkCrypto, line 269
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 270
		logWarn(parseData) // library marker davegut.tpLinkCrypto, line 271
		handleCommsError("deviceDataParseError") // library marker davegut.tpLinkCrypto, line 272
	} // library marker davegut.tpLinkCrypto, line 273
	return parseData // library marker davegut.tpLinkCrypto, line 274
} // library marker davegut.tpLinkCrypto, line 275

def parseAesData(resp) { // library marker davegut.tpLinkCrypto, line 277
	Map parseData = [parseMethod: "parseAesData"] // library marker davegut.tpLinkCrypto, line 278
	try { // library marker davegut.tpLinkCrypto, line 279
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 280
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 281
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCrypto, line 282
														 encKey, encIv)) // library marker davegut.tpLinkCrypto, line 283
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 284
		state.errorCount = 0 // library marker davegut.tpLinkCrypto, line 285
		setCommsError(false) // library marker davegut.tpLinkCrypto, line 286
		logDebug(parseData) // library marker davegut.tpLinkCrypto, line 287
	} catch (err) { // library marker davegut.tpLinkCrypto, line 288
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 289
		logWarn(parseData) // library marker davegut.tpLinkCrypto, line 290
		handleCommsError("deviceDataParseError") // library marker davegut.tpLinkCrypto, line 291
	} // library marker davegut.tpLinkCrypto, line 292
	return parseData // library marker davegut.tpLinkCrypto, line 293
} // library marker davegut.tpLinkCrypto, line 294

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 296
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 297
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 298
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 299
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 300
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 301
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 302
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 303
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 304

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 306
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 307
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 308
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 309
} // library marker davegut.tpLinkCrypto, line 310

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 312
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 313
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 314
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 315
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 316
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 317
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 318
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 319
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 320
} // library marker davegut.tpLinkCrypto, line 321

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 323
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 324
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 325
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 326
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 327
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 328
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 329
} // library marker davegut.tpLinkCrypto, line 330

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 332
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 333
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 334
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 335
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 336
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 337
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 338
} // library marker davegut.tpLinkCrypto, line 339

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 341
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 342
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 343
	md.update(data) // library marker davegut.tpLinkCrypto, line 344
	return md.digest() // library marker davegut.tpLinkCrypto, line 345
} // library marker davegut.tpLinkCrypto, line 346

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 348
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 349
	return new String(arr) // library marker davegut.tpLinkCrypto, line 350
} // library marker davegut.tpLinkCrypto, line 351

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 353
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 354
	try { // library marker davegut.tpLinkCrypto, line 355
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 356
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 357
	} catch (error) { // library marker davegut.tpLinkCrypto, line 358
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 359
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 360
	} // library marker davegut.tpLinkCrypto, line 361
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 362
} // library marker davegut.tpLinkCrypto, line 363

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 365
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 366
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 367
	return byteValue // library marker davegut.tpLinkCrypto, line 368
} // library marker davegut.tpLinkCrypto, line 369

def getRsaKey() { // library marker davegut.tpLinkCrypto, line 371
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkCrypto, line 372
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkCrypto, line 373
} // library marker davegut.tpLinkCrypto, line 374

// ~~~~~ end include (37) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (30) davegut.Logging ~~~~~
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

// ~~~~~ end include (30) davegut.Logging ~~~~~
