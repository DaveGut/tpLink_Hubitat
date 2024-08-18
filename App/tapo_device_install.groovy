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
	if (app.getLabel() != "Kasa Integration") { runIn(570, resetTpLinkChecked) }
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
	if (appVer != version()) {
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
		logInfo([method: "initInstance", status: "App data updated for appVer ${version()}"])
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
			paragraph "<b>Password and Username are both case sensitive.</b>"
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
	action = updateLegacyDevices()
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
	Map logData = [method: "updateLegacyDevices", appVer: appVer, 
				   version: version()]
	if (appVer != version()) {
		List children = getChildDevices()
		logData << [children: children]
		children.each { child ->
//			child.updateDeviceData()
			child.configure(false)
		}
		app?.updateSetting("appVer", [type:"string", value: version()])
		logData << [appVer: version()]
	}
	logInfo(logData)
	return
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
			//	FUTURE: Add tpLinkType and type to deviceData
			deviceData << [deviceIP: device.value.ip,
						   deviceId: device.value.deviceId,
						   devicePort:  device.value.port,
						   feature: feature]
		} else {
			deviceData << [baseUrl: device.value.baseUrl,
						   tpLinkType: device.value.deviceType,
						   type: device.value.type]
		}

		if (device.value.plugNo != null) {
			//	Add to devices on update.
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
					"name" : device.value.model,
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

// ~~~~~ start include (104) davegut.appTpLinkSmart ~~~~~
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
	logData << [hostArray: state.hostArray, pollSegment: state.segArray] // library marker davegut.appTpLinkSmart, line 43
	List deviceIPs = [] // library marker davegut.appTpLinkSmart, line 44
	state.segArray.each { // library marker davegut.appTpLinkSmart, line 45
		def pollSegment = it.trim() // library marker davegut.appTpLinkSmart, line 46
		logData << [pollSegment: pollSegment] // library marker davegut.appTpLinkSmart, line 47
           for(int i = start; i < finish; i++) { // library marker davegut.appTpLinkSmart, line 48
			deviceIPs.add("${pollSegment}.${i.toString()}") // library marker davegut.appTpLinkSmart, line 49
		} // library marker davegut.appTpLinkSmart, line 50
		def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d" // library marker davegut.appTpLinkSmart, line 51
		await = sendLanCmd(deviceIPs.join(','), "20002", cmdData, action, timeout) // library marker davegut.appTpLinkSmart, line 52

		logDebug("findTpLinkDevices: [startFinding:[segment: ${pollSegment}]]") // library marker davegut.appTpLinkSmart, line 54
		app?.updateSetting("finding", true) // library marker davegut.appTpLinkSmart, line 55
		int i // library marker davegut.appTpLinkSmart, line 56
		for(i = 0; i < 30; i++) { // library marker davegut.appTpLinkSmart, line 57
			if (i == 29) { // library marker davegut.appTpLinkSmart, line 58
				logWarn("findTpLinkDevices: [findingError:[segment: ${pollSegment}]]") // library marker davegut.appTpLinkSmart, line 59
			} // library marker davegut.appTpLinkSmart, line 60
			pauseExecution(1000) // library marker davegut.appTpLinkSmart, line 61
			if (finding == false) { // library marker davegut.appTpLinkSmart, line 62
				pauseExecution(5000) // library marker davegut.appTpLinkSmart, line 63
				i = 31 // library marker davegut.appTpLinkSmart, line 64
			} // library marker davegut.appTpLinkSmart, line 65
		} // library marker davegut.appTpLinkSmart, line 66
		logDebug("<b>findTpLinkDevices: [segment: ${pollSegment}, finding: complete]</b>") // library marker davegut.appTpLinkSmart, line 67
	} // library marker davegut.appTpLinkSmart, line 68
	app?.updateSetting("finding", false) // library marker davegut.appTpLinkSmart, line 69
	return logData // library marker davegut.appTpLinkSmart, line 70
} // library marker davegut.appTpLinkSmart, line 71

def getTpLinkLanData(response) { // library marker davegut.appTpLinkSmart, line 73
	Map logData = [method: "getTpLinkLanData", response: response.size()] // library marker davegut.appTpLinkSmart, line 74
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 75
	List discData = [] // library marker davegut.appTpLinkSmart, line 76
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 77
		Map devData = getDiscData(response) // library marker davegut.appTpLinkSmart, line 78
		if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 79
			discData << devData // library marker davegut.appTpLinkSmart, line 80
		} // library marker davegut.appTpLinkSmart, line 81
	} else { // library marker davegut.appTpLinkSmart, line 82
		response.each { // library marker davegut.appTpLinkSmart, line 83
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 84
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 85
				discData << devData // library marker davegut.appTpLinkSmart, line 86
			} // library marker davegut.appTpLinkSmart, line 87
		} // library marker davegut.appTpLinkSmart, line 88
	} // library marker davegut.appTpLinkSmart, line 89
	getAllTpLinkDeviceData(discData) // library marker davegut.appTpLinkSmart, line 90

	app?.updateSetting("finding", false) // library marker davegut.appTpLinkSmart, line 92
	runIn(5, updateTpLinkDevices, [data: discData]) // library marker davegut.appTpLinkSmart, line 93
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 94
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
				def protocol = payload.mgt_encrypt_schm.encrypt_type // library marker davegut.appTpLinkSmart, line 110
				def port = payload.mgt_encrypt_schm.http_port // library marker davegut.appTpLinkSmart, line 111
				def dni = payload.mac.replaceAll("-", "") // library marker davegut.appTpLinkSmart, line 112
				def baseUrl = "http://${payload.ip}:${payload.mgt_encrypt_schm.http_port}/app" // library marker davegut.appTpLinkSmart, line 113
				if (payload.device_type == "SMART.TAPOROBOVAC") { // library marker davegut.appTpLinkSmart, line 114
					baseUrl = "https://${payload.ip}:${payload.mgt_encrypt_schm.http_port}" // library marker davegut.appTpLinkSmart, line 115
					protocol = "vacAes" // library marker davegut.appTpLinkSmart, line 116
				} // library marker davegut.appTpLinkSmart, line 117
				devData << [ // library marker davegut.appTpLinkSmart, line 118
					type: payload.device_type, model: payload.device_model, // library marker davegut.appTpLinkSmart, line 119
					baseUrl: baseUrl, dni: dni, devId: payload.device_id,  // library marker davegut.appTpLinkSmart, line 120
					ip: payload.ip, port: port, protocol: protocol, status: "OK"] // library marker davegut.appTpLinkSmart, line 121
			} else { // library marker davegut.appTpLinkSmart, line 122
				devData << [type: payload.device_type, model: payload.device_model,  // library marker davegut.appTpLinkSmart, line 123
							status: "INVALID", reason: "Device not supported."] // library marker davegut.appTpLinkSmart, line 124
			} // library marker davegut.appTpLinkSmart, line 125
		} // library marker davegut.appTpLinkSmart, line 126
		logInfo(devData) // library marker davegut.appTpLinkSmart, line 127
	} catch (err) { // library marker davegut.appTpLinkSmart, line 128
		devData << [status: "INVALID", respData: repsData, error: err] // library marker davegut.appTpLinkSmart, line 129
		logDebug(devData) // library marker davegut.appTpLinkSmart, line 130
	} // library marker davegut.appTpLinkSmart, line 131
	return devData // library marker davegut.appTpLinkSmart, line 132
} // library marker davegut.appTpLinkSmart, line 133

def getAllTpLinkDeviceData(List discData) { // library marker davegut.appTpLinkSmart, line 135
	//	Modified to only get detailed data from new Hubitat devices. // library marker davegut.appTpLinkSmart, line 136
	Map logData = [method: "getAllTpLinkDeviceData", discData: discData.size()] // library marker davegut.appTpLinkSmart, line 137
	discData.each { Map devData -> // library marker davegut.appTpLinkSmart, line 138
		def childDev = getChildDevice(devData.dni) // library marker davegut.appTpLinkSmart, line 139
		if (!childDev) { // library marker davegut.appTpLinkSmart, line 140
			logData << ["${devData.dni}": [model: devData.model, status: "addingToDevices"]] // library marker davegut.appTpLinkSmart, line 141
			if (devData.protocol == "KLAP") { // library marker davegut.appTpLinkSmart, line 142
				klapHandshake(devData.baseUrl, localHash, devData) // library marker davegut.appTpLinkSmart, line 143
			} else if (devData.protocol == "AES") { // library marker davegut.appTpLinkSmart, line 144
				aesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 145
			} else if (devData.protocol == "vacAes") { // library marker davegut.appTpLinkSmart, line 146
				vacAesHandshake(devData.baseUrl, devData) // library marker davegut.appTpLinkSmart, line 147
			} // library marker davegut.appTpLinkSmart, line 148
			pauseExecution(500) // library marker davegut.appTpLinkSmart, line 149
		} else { // library marker davegut.appTpLinkSmart, line 150
			logData << ["${devData.dni}": [model: devData.model, status: "alreadyInstalled"]] // library marker davegut.appTpLinkSmart, line 151
		} // library marker davegut.appTpLinkSmart, line 152
	} // library marker davegut.appTpLinkSmart, line 153
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 154
} // library marker davegut.appTpLinkSmart, line 155

def getDataCmd() { // library marker davegut.appTpLinkSmart, line 157
	List requests = [[method: "get_device_info"]] // library marker davegut.appTpLinkSmart, line 158
	requests << [method: "component_nego"] // library marker davegut.appTpLinkSmart, line 159
	Map cmdBody = [ // library marker davegut.appTpLinkSmart, line 160
		method: "multipleRequest", // library marker davegut.appTpLinkSmart, line 161
		params: [requests: requests]] // library marker davegut.appTpLinkSmart, line 162
	return cmdBody // library marker davegut.appTpLinkSmart, line 163
} // library marker davegut.appTpLinkSmart, line 164

def addToDevices(devData, cmdResp) { // library marker davegut.appTpLinkSmart, line 166
	Map logData = [method: "addToDevices"] // library marker davegut.appTpLinkSmart, line 167
	String dni = devData.dni // library marker davegut.appTpLinkSmart, line 168
	def components = cmdResp.find { it.method == "component_nego" } // library marker davegut.appTpLinkSmart, line 169
	cmdResp = cmdResp.find { it.method == "get_device_info" } // library marker davegut.appTpLinkSmart, line 170
	cmdResp = cmdResp.result // library marker davegut.appTpLinkSmart, line 171
	byte[] plainBytes = cmdResp.nickname.decodeBase64() // library marker davegut.appTpLinkSmart, line 172
	def alias = new String(plainBytes) // library marker davegut.appTpLinkSmart, line 173
	if (alias == "") { alias = cmdResp.model } // library marker davegut.appTpLinkSmart, line 174
	def ctHigh // library marker davegut.appTpLinkSmart, line 175
	def ctLow // library marker davegut.appTpLinkSmart, line 176
	def driver = "Plug" // library marker davegut.appTpLinkSmart, line 177
	def comps = components.result.component_list // library marker davegut.appTpLinkSmart, line 178

	//	Priority order for driver type: [parent, lightStrip, // library marker davegut.appTpLinkSmart, line 180
	//	colorBulb, EMPlug, dimmer, vacuum // library marker davegut.appTpLinkSmart, line 181
	if (comps.find { it.id == "control_child" } ) { // library marker davegut.appTpLinkSmart, line 182
		if (comps.find { it.id == "alarm" } ) { // library marker davegut.appTpLinkSmart, line 183
			driver = "Hub" // library marker davegut.appTpLinkSmart, line 184
		} else { // library marker davegut.appTpLinkSmart, line 185
			driver = "Parent" // library marker davegut.appTpLinkSmart, line 186
		} // library marker davegut.appTpLinkSmart, line 187
	} else if (comps.find { it.id == "light_strip" } ) { // library marker davegut.appTpLinkSmart, line 188
		driver = "Lightstrip" // library marker davegut.appTpLinkSmart, line 189
	} else if (comps.find { it.id == "color" } ) { // library marker davegut.appTpLinkSmart, line 190
		driver = "Color Bulb" // library marker davegut.appTpLinkSmart, line 191
	} else if (comps.find { it.id == "energy_monitoring" } ) { // library marker davegut.appTpLinkSmart, line 192
		driver = "Plug EM" // library marker davegut.appTpLinkSmart, line 193
	} else if (comps.find { it.id == "brightness" } ) { // library marker davegut.appTpLinkSmart, line 194
		driver = "Dimmer" // library marker davegut.appTpLinkSmart, line 195
	} else if (comps.find { it.id == "clean" } ) { // library marker davegut.appTpLinkSmart, line 196
		driver = "Robovac" // library marker davegut.appTpLinkSmart, line 197
	} // library marker davegut.appTpLinkSmart, line 198
	if (comps.find { it.id == "color_temperature" } ) { // library marker davegut.appTpLinkSmart, line 199
		ctHigh = cmdResp.color_temp_range[1] // library marker davegut.appTpLinkSmart, line 200
		ctLow = cmdResp.color_temp_range[0] // library marker davegut.appTpLinkSmart, line 201
	} // library marker davegut.appTpLinkSmart, line 202


/*	comps.each { cap -> // library marker davegut.appTpLinkSmart, line 205
		switch(cap.id) { // library marker davegut.appTpLinkSmart, line 206
			case "control_child":		/////////////////////////// // library marker davegut.appTpLinkSmart, line 207
				if (devData.type.contains("HUB")) { // library marker davegut.appTpLinkSmart, line 208
					driver = "Hub" // library marker davegut.appTpLinkSmart, line 209
				} else if (devData.type.contains("PLUG") || devData.type.contains("SWITCH")) { // library marker davegut.appTpLinkSmart, line 210
					driver = "Parent" // library marker davegut.appTpLinkSmart, line 211
				} // library marker davegut.appTpLinkSmart, line 212
				break // library marker davegut.appTpLinkSmart, line 213
			case "energy_monitoring":			///////////////////////// // library marker davegut.appTpLinkSmart, line 214
				driver = "Plug EM" // library marker davegut.appTpLinkSmart, line 215
				break // library marker davegut.appTpLinkSmart, line 216
			case "brightness": 					///////////////////////// // library marker davegut.appTpLinkSmart, line 217
				if (!cmdResp.color_temp_range) { // library marker davegut.appTpLinkSmart, line 218
					driver = "Dimmer" // library marker davegut.appTpLinkSmart, line 219
				} // library marker davegut.appTpLinkSmart, line 220
				break // library marker davegut.appTpLinkSmart, line 221
			case "color":		/////////////////////////////////// // library marker davegut.appTpLinkSmart, line 222
				if (comps.find {it.id == "light_strip" }) { // library marker davegut.appTpLinkSmart, line 223
					driver = "Lightstrip" // library marker davegut.appTpLinkSmart, line 224
				} else { // library marker davegut.appTpLinkSmart, line 225
					driver = "Color Bulb" // library marker davegut.appTpLinkSmart, line 226
				} // library marker davegut.appTpLinkSmart, line 227
				break // library marker davegut.appTpLinkSmart, line 228
			case "color_temperature":	////////////////////// // library marker davegut.appTpLinkSmart, line 229
				ctHigh = cmdResp.color_temp_range[1] // library marker davegut.appTpLinkSmart, line 230
				ctLow = cmdResp.color_temp_range[0] // library marker davegut.appTpLinkSmart, line 231
				break // library marker davegut.appTpLinkSmart, line 232
			case "clean":	///////////////////////// // library marker davegut.appTpLinkSmart, line 233
				driver = "Robovac" // library marker davegut.appTpLinkSmart, line 234
				break // library marker davegut.appTpLinkSmart, line 235
		} // library marker davegut.appTpLinkSmart, line 236
	}*/ // library marker davegut.appTpLinkSmart, line 237
	def devicesData = atomicState.tpLinkDevices // library marker davegut.appTpLinkSmart, line 238
	Map deviceData = [deviceType: devData.type, protocol: devData.protocol,  // library marker davegut.appTpLinkSmart, line 239
					  model: devData.model, baseUrl: devData.baseUrl, alias: alias,  // library marker davegut.appTpLinkSmart, line 240
					  type: driver] // library marker davegut.appTpLinkSmart, line 241
	if (ctHigh != null) { // library marker davegut.appTpLinkSmart, line 242
		deviceData << [ctHigh: ctHigh, ctLow: ctLow] // library marker davegut.appTpLinkSmart, line 243
	} // library marker davegut.appTpLinkSmart, line 244
	devicesData << ["${dni}": deviceData] // library marker davegut.appTpLinkSmart, line 245

	atomicState.tpLinkDevices = devicesData // library marker davegut.appTpLinkSmart, line 247
	logData << ["${deviceData.alias}": deviceData, dni: dni] // library marker davegut.appTpLinkSmart, line 248
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 249
logTrace(logData) // library marker davegut.appTpLinkSmart, line 250
} // library marker davegut.appTpLinkSmart, line 251

//	===== get Smart KLAP Protocol Data ===== // library marker davegut.appTpLinkSmart, line 253
def sendKlapDataCmd(handshakeData, data) { // library marker davegut.appTpLinkSmart, line 254
	if (handshakeData.respStatus != "Login OK") { // library marker davegut.appTpLinkSmart, line 255
		Map logData = [method: "sendKlapDataCmd", handshake: handshakeData] // library marker davegut.appTpLinkSmart, line 256
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 257
	} else { // library marker davegut.appTpLinkSmart, line 258
		Map reqParams = [timeout: 10, headers: ["Cookie": data.data.cookie]] // library marker davegut.appTpLinkSmart, line 259
		def seqNo = data.data.seqNo + 1 // library marker davegut.appTpLinkSmart, line 260
		String cmdBodyJson = new groovy.json.JsonBuilder(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 261
		Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), data.data.encKey,  // library marker davegut.appTpLinkSmart, line 262
										data.data.encIv, data.data.encSig, seqNo) // library marker davegut.appTpLinkSmart, line 263
		reqParams << [uri: "${data.data.baseUrl}/request?seq=${encryptedData.seqNumber}", // library marker davegut.appTpLinkSmart, line 264
					  body: encryptedData.cipherData, // library marker davegut.appTpLinkSmart, line 265
					  contentType: "application/octet-stream", // library marker davegut.appTpLinkSmart, line 266
					  requestContentType: "application/octet-stream"] // library marker davegut.appTpLinkSmart, line 267
		asyncPost(reqParams, "parseKlapResp", data.data) // library marker davegut.appTpLinkSmart, line 268
	} // library marker davegut.appTpLinkSmart, line 269
} // library marker davegut.appTpLinkSmart, line 270

def parseKlapResp(resp, data) { // library marker davegut.appTpLinkSmart, line 272
	Map logData = [method: "parseKlapResp"] // library marker davegut.appTpLinkSmart, line 273
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 274
		try { // library marker davegut.appTpLinkSmart, line 275
			byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.appTpLinkSmart, line 276
			def clearResp = klapDecrypt(cipherResponse, data.data.encKey, // library marker davegut.appTpLinkSmart, line 277
										data.data.encIv, data.data.seqNo + 1) // library marker davegut.appTpLinkSmart, line 278
			Map cmdResp =  new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 279
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 280
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 281
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 282
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 283
			} else { // library marker davegut.appTpLinkSmart, line 284
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 285
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 286
			} // library marker davegut.appTpLinkSmart, line 287
		} catch (err) { // library marker davegut.appTpLinkSmart, line 288
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 289
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 290
		} // library marker davegut.appTpLinkSmart, line 291
	} else { // library marker davegut.appTpLinkSmart, line 292
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 293
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 294
	} // library marker davegut.appTpLinkSmart, line 295
} // library marker davegut.appTpLinkSmart, line 296

//	===== get Smart AES Protocol Data ===== // library marker davegut.appTpLinkSmart, line 298
def getAesToken(resp, data) { // library marker davegut.appTpLinkSmart, line 299
	Map logData = [method: "getAesToken"] // library marker davegut.appTpLinkSmart, line 300
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 301
		if (resp.json.error_code == 0) { // library marker davegut.appTpLinkSmart, line 302
			try { // library marker davegut.appTpLinkSmart, line 303
				def clearResp = aesDecrypt(resp.json.result.response, data.encKey, data.encIv) // library marker davegut.appTpLinkSmart, line 304
				Map cmdResp = new JsonSlurper().parseText(clearResp) // library marker davegut.appTpLinkSmart, line 305
				if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 306
					def token = cmdResp.result.token // library marker davegut.appTpLinkSmart, line 307
					logData << [respStatus: "OK", token: token] // library marker davegut.appTpLinkSmart, line 308
					logDebug(logData) // library marker davegut.appTpLinkSmart, line 309
					sendAesDataCmd(token, data) // library marker davegut.appTpLinkSmart, line 310
				} else { // library marker davegut.appTpLinkSmart, line 311
					logData << [respStatus: "ERROR code in cmdResp",  // library marker davegut.appTpLinkSmart, line 312
								error_code: cmdResp.error_code, // library marker davegut.appTpLinkSmart, line 313
								check: "cryptoArray, credentials", data: cmdResp] // library marker davegut.appTpLinkSmart, line 314
					logWarn(logData) // library marker davegut.appTpLinkSmart, line 315
				} // library marker davegut.appTpLinkSmart, line 316
			} catch (err) { // library marker davegut.appTpLinkSmart, line 317
				logData << [respStatus: "ERROR parsing respJson", respJson: resp.json, // library marker davegut.appTpLinkSmart, line 318
							error: err] // library marker davegut.appTpLinkSmart, line 319
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 320
			} // library marker davegut.appTpLinkSmart, line 321
		} else { // library marker davegut.appTpLinkSmart, line 322
			logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code, // library marker davegut.appTpLinkSmart, line 323
						respJson: resp.json] // library marker davegut.appTpLinkSmart, line 324
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 325
		} // library marker davegut.appTpLinkSmart, line 326
	} else { // library marker davegut.appTpLinkSmart, line 327
		logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties] // library marker davegut.appTpLinkSmart, line 328
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 329
	} // library marker davegut.appTpLinkSmart, line 330
} // library marker davegut.appTpLinkSmart, line 331

def sendAesDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 333
	def cmdStr = JsonOutput.toJson(getDataCmd()).toString() // library marker davegut.appTpLinkSmart, line 334
	Map reqBody = [method: "securePassthrough", // library marker davegut.appTpLinkSmart, line 335
				   params: [request: aesEncrypt(cmdStr, data.encKey, data.encIv)]] // library marker davegut.appTpLinkSmart, line 336
	Map reqParams = [uri: "${data.baseUrl}?token=${token}", // library marker davegut.appTpLinkSmart, line 337
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.appTpLinkSmart, line 338
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 339
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 340
					 timeout: 10,  // library marker davegut.appTpLinkSmart, line 341
					 headers: ["Cookie": data.cookie]] // library marker davegut.appTpLinkSmart, line 342
	asyncPost(reqParams, "parseAesResp", data) // library marker davegut.appTpLinkSmart, line 343
} // library marker davegut.appTpLinkSmart, line 344

def parseAesResp(resp, data) { // library marker davegut.appTpLinkSmart, line 346
	Map logData = [method: "parseAesResp"] // library marker davegut.appTpLinkSmart, line 347
	if (resp.status == 200) { // library marker davegut.appTpLinkSmart, line 348
		try { // library marker davegut.appTpLinkSmart, line 349
			Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.appTpLinkSmart, line 350
																 data.data.encKey, data.data.encIv)) // library marker davegut.appTpLinkSmart, line 351
			logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 352
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 353
				addToDevices(data.data.devData, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 354
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 355
			} else { // library marker davegut.appTpLinkSmart, line 356
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 357
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 358
			} // library marker davegut.appTpLinkSmart, line 359
		} catch (err) { // library marker davegut.appTpLinkSmart, line 360
			logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 361
			logWarn(logData) // library marker davegut.appTpLinkSmart, line 362
		} // library marker davegut.appTpLinkSmart, line 363
	} else { // library marker davegut.appTpLinkSmart, line 364
		logData << [status: "httpFailure", data: resp.properties] // library marker davegut.appTpLinkSmart, line 365
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 366
	} // library marker davegut.appTpLinkSmart, line 367
} // library marker davegut.appTpLinkSmart, line 368

//	===== get Smart VAC Protocol Data ===== // library marker davegut.appTpLinkSmart, line 370
def vacAesHandshake(baseUrl, devData) { // library marker davegut.appTpLinkSmart, line 371
	Map reqData = [baseUrl: baseUrl, devData: devData] // library marker davegut.appTpLinkSmart, line 372
	Map cmdBody = [method: "login", // library marker davegut.appTpLinkSmart, line 373
				   params: [hashed: true,  // library marker davegut.appTpLinkSmart, line 374
							password: encPasswordVac, // library marker davegut.appTpLinkSmart, line 375
							username: userName]] // library marker davegut.appTpLinkSmart, line 376
	Map reqParams = [uri: baseUrl, // library marker davegut.appTpLinkSmart, line 377
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 378
					 body: cmdBody, // library marker davegut.appTpLinkSmart, line 379
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 380
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 381
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 382
	asyncPost(reqParams, "parseVacAesLogin", reqData) // library marker davegut.appTpLinkSmart, line 383
} // library marker davegut.appTpLinkSmart, line 384

def parseVacAesLogin(resp, data) { // library marker davegut.appTpLinkSmart, line 386
	Map logData = [method: "parseVacAesLogin", oldToken: token] // library marker davegut.appTpLinkSmart, line 387
	if (resp.status == 200 && resp.json != null) { // library marker davegut.appTpLinkSmart, line 388
		logData << [status: "OK"] // library marker davegut.appTpLinkSmart, line 389
		logData << [token: resp.json.result.token] // library marker davegut.appTpLinkSmart, line 390
		sendVacDataCmd(resp.json.result.token, data) // library marker davegut.appTpLinkSmart, line 391
		logDebug(logData) // library marker davegut.appTpLinkSmart, line 392
	} else { // library marker davegut.appTpLinkSmart, line 393
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.appTpLinkSmart, line 394
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 395
	} // library marker davegut.appTpLinkSmart, line 396
} // library marker davegut.appTpLinkSmart, line 397

def sendVacDataCmd(token, data) { // library marker davegut.appTpLinkSmart, line 399
	Map devData = data.data.devData // library marker davegut.appTpLinkSmart, line 400
	Map reqParams = [uri: "${data.data.baseUrl}/?token=${token}", // library marker davegut.appTpLinkSmart, line 401
					 body: getDataCmd(), // library marker davegut.appTpLinkSmart, line 402
					 contentType: "application/json", // library marker davegut.appTpLinkSmart, line 403
					 requestContentType: "application/json", // library marker davegut.appTpLinkSmart, line 404
					 ignoreSSLIssues: true, // library marker davegut.appTpLinkSmart, line 405
					 timeout: 10] // library marker davegut.appTpLinkSmart, line 406
	asyncPost(reqParams, "parseVacResp", devData) // library marker davegut.appTpLinkSmart, line 407
} // library marker davegut.appTpLinkSmart, line 408

def parseVacResp(resp, devData) { // library marker davegut.appTpLinkSmart, line 410
	Map logData = [parseMethod: "parseVacResp"] // library marker davegut.appTpLinkSmart, line 411
	try { // library marker davegut.appTpLinkSmart, line 412
		Map cmdResp = resp.json // library marker davegut.appTpLinkSmart, line 413
		logData << [status: "OK", cmdResp: cmdResp] // library marker davegut.appTpLinkSmart, line 414
			if (cmdResp.error_code == 0) { // library marker davegut.appTpLinkSmart, line 415
				addToDevices(devData.data, cmdResp.result.responses) // library marker davegut.appTpLinkSmart, line 416
				logDebug(logData) // library marker davegut.appTpLinkSmart, line 417
			} else { // library marker davegut.appTpLinkSmart, line 418
				logData << [status: "errorInCmdResp"] // library marker davegut.appTpLinkSmart, line 419
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 420
			} // library marker davegut.appTpLinkSmart, line 421
	} catch (err) { // library marker davegut.appTpLinkSmart, line 422
		logData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.appTpLinkSmart, line 423
		logWarn(logData) // library marker davegut.appTpLinkSmart, line 424
	} // library marker davegut.appTpLinkSmart, line 425
	return parseData	 // library marker davegut.appTpLinkSmart, line 426
} // library marker davegut.appTpLinkSmart, line 427

def tpLinkCheckForDevices(timeout = 5) { // library marker davegut.appTpLinkSmart, line 429
	Map logData = [method: "tpLinkCheckForDevices"] // library marker davegut.appTpLinkSmart, line 430
	if (state.tpLinkChecked == true) { // library marker davegut.appTpLinkSmart, line 431
		logData << [status: "noCheck", reason: "Completed within last 10 minutes"] // library marker davegut.appTpLinkSmart, line 432
	} else { // library marker davegut.appTpLinkSmart, line 433
		def findData = findTpLinkDevices("parseTpLinkCheck", timeout) // library marker davegut.appTpLinkSmart, line 434
		logData << [status: "checking"] // library marker davegut.appTpLinkSmart, line 435
	} // library marker davegut.appTpLinkSmart, line 436
	return logData // library marker davegut.appTpLinkSmart, line 437
} // library marker davegut.appTpLinkSmart, line 438

def resetTpLinkChecked() { state.tpLinkChecked = false } // library marker davegut.appTpLinkSmart, line 440

def parseTpLinkCheck(response) { // library marker davegut.appTpLinkSmart, line 442
	List discData = [] // library marker davegut.appTpLinkSmart, line 443
	if (response instanceof Map) { // library marker davegut.appTpLinkSmart, line 444
		Map devdata = getDiscData(response) // library marker davegut.appTpLinkSmart, line 445
		if (devData.status != "INVALID") { // library marker davegut.appTpLinkSmart, line 446
			discData << devData // library marker davegut.appTpLinkSmart, line 447
		} // library marker davegut.appTpLinkSmart, line 448
	} else { // library marker davegut.appTpLinkSmart, line 449
		response.each { // library marker davegut.appTpLinkSmart, line 450
			Map devData = getDiscData(it) // library marker davegut.appTpLinkSmart, line 451
			if (devData.status == "OK") { // library marker davegut.appTpLinkSmart, line 452
				discData << devData // library marker davegut.appTpLinkSmart, line 453
			} // library marker davegut.appTpLinkSmart, line 454
		} // library marker davegut.appTpLinkSmart, line 455
	} // library marker davegut.appTpLinkSmart, line 456
	updateTpLinkDevices(discData) // library marker davegut.appTpLinkSmart, line 457
} // library marker davegut.appTpLinkSmart, line 458

def updateTpLinkDevices(discData) { // library marker davegut.appTpLinkSmart, line 460
	Map logData = [method: "updateTpLinkDevices"] // library marker davegut.appTpLinkSmart, line 461
	state.tpLinkChecked = true // library marker davegut.appTpLinkSmart, line 462
	runIn(570, resetTpLinkChecked) // library marker davegut.appTpLinkSmart, line 463
	List children = getChildDevices() // library marker davegut.appTpLinkSmart, line 464
	children.each { childDev -> // library marker davegut.appTpLinkSmart, line 465
		Map childData = [:] // library marker davegut.appTpLinkSmart, line 466
		def dni = childDev.deviceNetworkId // library marker davegut.appTpLinkSmart, line 467
		def connected = "false" // library marker davegut.appTpLinkSmart, line 468
		Map devData = discData.find{ it.dni == dni } // library marker davegut.appTpLinkSmart, line 469
		if (childDev.getDataValue("baseUrl")) { // library marker davegut.appTpLinkSmart, line 470
			if (devData != null) { // library marker davegut.appTpLinkSmart, line 471
				if (childDev.getDataValue("baseUrl") == devData.baseUrl && // library marker davegut.appTpLinkSmart, line 472
				    childDev.getDataValue("protocol") == devData.protocol) { // library marker davegut.appTpLinkSmart, line 473
					childData << [status: "noChanges"] // library marker davegut.appTpLinkSmart, line 474
				} else { // library marker davegut.appTpLinkSmart, line 475
					childDev.updateDataValue("baseUrl", devData.baseUrl) // library marker davegut.appTpLinkSmart, line 476
					childDev.updateDataValue("protocol", devData.protocol) // library marker davegut.appTpLinkSmart, line 477
					childData << ["baseUrl": devData.baseUrl, // library marker davegut.appTpLinkSmart, line 478
								  "protocol": devData.protocol, // library marker davegut.appTpLinkSmart, line 479
								  "connected": "true"] // library marker davegut.appTpLinkSmart, line 480
				} // library marker davegut.appTpLinkSmart, line 481
			} else { // library marker davegut.appTpLinkSmart, line 482
				childData << [connected: "false", reason: "not Discovered By App"] // library marker davegut.appTpLinkSmart, line 483
				logWarn(logData) // library marker davegut.appTpLinkSmart, line 484
			} // library marker davegut.appTpLinkSmart, line 485
			pauseExecution(500) // library marker davegut.appTpLinkSmart, line 486
		} // library marker davegut.appTpLinkSmart, line 487
		logData << ["${childDev}": childData] // library marker davegut.appTpLinkSmart, line 488
	} // library marker davegut.appTpLinkSmart, line 489
	logDebug(logData) // library marker davegut.appTpLinkSmart, line 490
logInfo(logData) // library marker davegut.appTpLinkSmart, line 491
} // library marker davegut.appTpLinkSmart, line 492

// ~~~~~ end include (104) davegut.appTpLinkSmart ~~~~~

// ~~~~~ start include (86) davegut.tpLinkComms ~~~~~
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
	} // library marker davegut.tpLinkComms, line 37
	logDebug(logData) // library marker davegut.tpLinkComms, line 38
} // library marker davegut.tpLinkComms, line 39

def parseData(resp, protocol = getDataValue("protocol")) { // library marker davegut.tpLinkComms, line 41
	Map logData = [method: "parseData"] // library marker davegut.tpLinkComms, line 42
	if (resp.status == 200) { // library marker davegut.tpLinkComms, line 43
		if (protocol == "KLAP") { // library marker davegut.tpLinkComms, line 44
			logData << parseKlapData(resp) // library marker davegut.tpLinkComms, line 45
		} else if (protocol == "AES") { // library marker davegut.tpLinkComms, line 46
			logData << parseAesData(resp) // library marker davegut.tpLinkComms, line 47
		} else if (protocol == "vacAes") { // library marker davegut.tpLinkComms, line 48
			logData << parseVacAesData(resp) // library marker davegut.tpLinkComms, line 49
		} // library marker davegut.tpLinkComms, line 50
		if (logData.status == "OK") { // library marker davegut.tpLinkComms, line 51
			setCommsError(false) // library marker davegut.tpLinkComms, line 52
		} else { // library marker davegut.tpLinkComms, line 53
			handleCommsError() // library marker davegut.tpLinkComms, line 54
		} // library marker davegut.tpLinkComms, line 55
	} else { // library marker davegut.tpLinkComms, line 56
		logData << [status: "httpFailure"] // library marker davegut.tpLinkComms, line 57
		handleCommsError() // library marker davegut.tpLinkComms, line 58
	} // library marker davegut.tpLinkComms, line 59
	return logData // library marker davegut.tpLinkComms, line 60
} // library marker davegut.tpLinkComms, line 61

//	===== Communications Error Handling ===== // library marker davegut.tpLinkComms, line 63
def handleCommsError() { // library marker davegut.tpLinkComms, line 64
	Map logData = [method: "handleCommsError"] // library marker davegut.tpLinkComms, line 65
	if (state.lastCmd != "") { // library marker davegut.tpLinkComms, line 66
		def count = state.errorCount + 1 // library marker davegut.tpLinkComms, line 67
		logData << [count: count, lastCmd: state.lastCmd] // library marker davegut.tpLinkComms, line 68
		switch (count) { // library marker davegut.tpLinkComms, line 69
			case 1: // library marker davegut.tpLinkComms, line 70
				logData << [action: "resendCommand"] // library marker davegut.tpLinkComms, line 71
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 72
				break // library marker davegut.tpLinkComms, line 73
			case 2: // library marker davegut.tpLinkComms, line 74
				logData << [attemptHandshake: deviceHandshake(), // library marker davegut.tpLinkComms, line 75
						    action: "deviceHandshake"] // library marker davegut.tpLinkComms, line 76
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 77
				break // library marker davegut.tpLinkComms, line 78
			case 3: // library marker davegut.tpLinkComms, line 79
				logData << [configure: configure(true), // library marker davegut.tpLinkComms, line 80
						    action: "configure"] // library marker davegut.tpLinkComms, line 81
				runIn(2, delayedPassThrough) // library marker davegut.tpLinkComms, line 82
				break // library marker davegut.tpLinkComms, line 83
			default: // library marker davegut.tpLinkComms, line 84
				setCommsError(true) // library marker davegut.tpLinkComms, line 85
				break // library marker davegut.tpLinkComms, line 86
		} // library marker davegut.tpLinkComms, line 87
		state.errorCount = count // library marker davegut.tpLinkComms, line 88
	} else { // library marker davegut.tpLinkComms, line 89
		logData << [status: "noCommandToRetry"] // library marker davegut.tpLinkComms, line 90
	} // library marker davegut.tpLinkComms, line 91
	logDebug(logData) // library marker davegut.tpLinkComms, line 92
logInfo(logData) // library marker davegut.tpLinkComms, line 93
} // library marker davegut.tpLinkComms, line 94

def delayedPassThrough() { // library marker davegut.tpLinkComms, line 96
	def cmdData = new JSONObject(state.lastCmd) // library marker davegut.tpLinkComms, line 97
	def cmdBody = parseJson(cmdData.cmdBody.toString()) // library marker davegut.tpLinkComms, line 98
	asyncSend(cmdBody, cmdData.reqData, cmdData.action) // library marker davegut.tpLinkComms, line 99
} // library marker davegut.tpLinkComms, line 100

def setCommsError(status) { // library marker davegut.tpLinkComms, line 102
	if (device && status == false) { // library marker davegut.tpLinkComms, line 103
		state.errorCount = 0 // library marker davegut.tpLinkComms, line 104
		if (device.currentValue("commsError") == "true") { // library marker davegut.tpLinkComms, line 105
			updateAttr("commsError", "false") // library marker davegut.tpLinkComms, line 106
			setPollInterval() // library marker davegut.tpLinkComms, line 107
			unschedule(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 108
			logInfo([method: "setCommsError", action: "setFalse"]) // library marker davegut.tpLinkComms, line 109
		} // library marker davegut.tpLinkComms, line 110
	} else if (device && status == true) { // library marker davegut.tpLinkComms, line 111
		if (device.currentValue("commsError") == "false") { // library marker davegut.tpLinkComms, line 112
			updateAttr("commsError", "true") // library marker davegut.tpLinkComms, line 113
			setPollInterval("30 min") // library marker davegut.tpLinkComms, line 114
			runEvery10Minutes(errorDeviceHandshake) // library marker davegut.tpLinkComms, line 115
			logWarn([method: "setCommsError", errorCount: state.errorCount, action: "setTrue"]) // library marker davegut.tpLinkComms, line 116
		} else { // library marker davegut.tpLinkComms, line 117
			logWarn([method: "setCommsError", errorCount: state.errorCount]) // library marker davegut.tpLinkComms, line 118
		} // library marker davegut.tpLinkComms, line 119
	} // library marker davegut.tpLinkComms, line 120
} // library marker davegut.tpLinkComms, line 121

def errorDeviceHandshake() {  // library marker davegut.tpLinkComms, line 123
	logDebug([method: "errorDeviceHandshake"]) // library marker davegut.tpLinkComms, line 124
	deviceHandshake() // library marker davegut.tpLinkComms, line 125
} // library marker davegut.tpLinkComms, line 126

// ~~~~~ end include (86) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (111) davegut.tpLinkCrypto ~~~~~
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
						setCommsError(false) // library marker davegut.tpLinkCrypto, line 100
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
		getAesToken(resp, data.data) // library marker davegut.tpLinkCrypto, line 123
	} // library marker davegut.tpLinkCrypto, line 124
} // library marker davegut.tpLinkCrypto, line 125

//	===== KLAP Handshake ===== // library marker davegut.tpLinkCrypto, line 127
def klapHandshake(baseUrl = getDataValue("baseUrl"), localHash = parent.localHash, devData = null) { // library marker davegut.tpLinkCrypto, line 128
	byte[] localSeed = new byte[16] // library marker davegut.tpLinkCrypto, line 129
	new Random().nextBytes(localSeed) // library marker davegut.tpLinkCrypto, line 130
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData] // library marker davegut.tpLinkCrypto, line 131
	Map reqParams = [uri: "${baseUrl}/handshake1", // library marker davegut.tpLinkCrypto, line 132
					 body: localSeed, // library marker davegut.tpLinkCrypto, line 133
					 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 134
					 requestContentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 135
					 timeout:10] // library marker davegut.tpLinkCrypto, line 136
	asyncPost(reqParams, "parseKlapHandshake", reqData) // library marker davegut.tpLinkCrypto, line 137
} // library marker davegut.tpLinkCrypto, line 138

def parseKlapHandshake(resp, data) { // library marker davegut.tpLinkCrypto, line 140
	Map logData = [method: "parseKlapHandshake", data: data] // library marker davegut.tpLinkCrypto, line 141
	if (resp.status == 200 && resp.data != null) { // library marker davegut.tpLinkCrypto, line 142
		try { // library marker davegut.tpLinkCrypto, line 143
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl] // library marker davegut.tpLinkCrypto, line 144
			byte[] localSeed = data.data.localSeed // library marker davegut.tpLinkCrypto, line 145
			byte[] seedData = resp.data.decodeBase64() // library marker davegut.tpLinkCrypto, line 146
			byte[] remoteSeed = seedData[0 .. 15] // library marker davegut.tpLinkCrypto, line 147
			byte[] serverHash = seedData[16 .. 47] // library marker davegut.tpLinkCrypto, line 148
			byte[] localHash = data.data.localHash.decodeBase64() // library marker davegut.tpLinkCrypto, line 149
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 150
			byte[] localAuthHash = mdEncode("SHA-256", authHash) // library marker davegut.tpLinkCrypto, line 151
			if (localAuthHash == serverHash) { // library marker davegut.tpLinkCrypto, line 152
				//	cookie // library marker davegut.tpLinkCrypto, line 153
				def cookieHeader = resp.headers["Set-Cookie"].toString() // library marker davegut.tpLinkCrypto, line 154
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";")) // library marker davegut.tpLinkCrypto, line 155
				logData << [cookie: cookie] // library marker davegut.tpLinkCrypto, line 156
				//	seqNo and encIv // library marker davegut.tpLinkCrypto, line 157
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 158
				byte[] fullIv = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 159
				byte[] byteSeqNo = fullIv[-4..-1] // library marker davegut.tpLinkCrypto, line 160

				int seqNo = byteArrayToInteger(byteSeqNo) // library marker davegut.tpLinkCrypto, line 162
				atomicState.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 163

				logData << [seqNo: seqNo, encIv: fullIv[0..11]] // library marker davegut.tpLinkCrypto, line 165
				//	encKey // library marker davegut.tpLinkCrypto, line 166
				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 167
				byte[] encKey = mdEncode("SHA-256", payload)[0..15] // library marker davegut.tpLinkCrypto, line 168
				logData << [encKey: encKey] // library marker davegut.tpLinkCrypto, line 169
				//	encSig // library marker davegut.tpLinkCrypto, line 170
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 171
				byte[] encSig = mdEncode("SHA-256", payload)[0..27] // library marker davegut.tpLinkCrypto, line 172
				if (device) { // library marker davegut.tpLinkCrypto, line 173
					device.updateSetting("cookie",[type:"password", value: cookie])  // library marker davegut.tpLinkCrypto, line 174
					device.updateSetting("encKey",[type:"password", value: encKey])  // library marker davegut.tpLinkCrypto, line 175
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]])  // library marker davegut.tpLinkCrypto, line 176
					device.updateSetting("encSig",[type:"password", value: encSig])  // library marker davegut.tpLinkCrypto, line 177
				} else { // library marker davegut.tpLinkCrypto, line 178
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11],  // library marker davegut.tpLinkCrypto, line 179
								encSig: encSig, encKey: encKey] // library marker davegut.tpLinkCrypto, line 180
				} // library marker davegut.tpLinkCrypto, line 181
				logData << [encSig: encSig] // library marker davegut.tpLinkCrypto, line 182
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten() // library marker davegut.tpLinkCrypto, line 183
				byte[] body = mdEncode("SHA-256", loginHash) // library marker davegut.tpLinkCrypto, line 184
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2", // library marker davegut.tpLinkCrypto, line 185
								 body: body, // library marker davegut.tpLinkCrypto, line 186
								 timeout:10, // library marker davegut.tpLinkCrypto, line 187
								 headers: ["Cookie": cookie], // library marker davegut.tpLinkCrypto, line 188
								 contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 189
								 requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 190
				asyncPost(reqParams, "parseKlapHandshake2", reqData) // library marker davegut.tpLinkCrypto, line 191
			} else { // library marker davegut.tpLinkCrypto, line 192
				logData << [respStatus: "ERROR: locakAuthHash != serverHash", // library marker davegut.tpLinkCrypto, line 193
							localAuthHash: localAuthHash, serverHash: serverHash] // library marker davegut.tpLinkCrypto, line 194
				logInfo(logData) // library marker davegut.tpLinkCrypto, line 195
			} // library marker davegut.tpLinkCrypto, line 196
		} catch (err) { // library marker davegut.tpLinkCrypto, line 197
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err] // library marker davegut.tpLinkCrypto, line 198
			logInfo(logData) // library marker davegut.tpLinkCrypto, line 199
		} // library marker davegut.tpLinkCrypto, line 200
	} else { // library marker davegut.tpLinkCrypto, line 201
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties] // library marker davegut.tpLinkCrypto, line 202
		logInfo(logData) // library marker davegut.tpLinkCrypto, line 203
	} // library marker davegut.tpLinkCrypto, line 204
} // library marker davegut.tpLinkCrypto, line 205

def parseKlapHandshake2(resp, data) { // library marker davegut.tpLinkCrypto, line 207
	Map logData = [method: "parseKlapHandshake2"] // library marker davegut.tpLinkCrypto, line 208
	if (resp.status == 200 && resp.data == null) { // library marker davegut.tpLinkCrypto, line 209
		logData << [respStatus: "Login OK"] // library marker davegut.tpLinkCrypto, line 210
		setCommsError(false) // library marker davegut.tpLinkCrypto, line 211
		logDebug(logData) // library marker davegut.tpLinkCrypto, line 212
	} else { // library marker davegut.tpLinkCrypto, line 213
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response", // library marker davegut.tpLinkCrypto, line 214
					resp: resp.properties] // library marker davegut.tpLinkCrypto, line 215
		logInfo(logData) // library marker davegut.tpLinkCrypto, line 216
	} // library marker davegut.tpLinkCrypto, line 217
	if (!device) { sendKlapDataCmd(logData, data) } // library marker davegut.tpLinkCrypto, line 218
} // library marker davegut.tpLinkCrypto, line 219

//	===== Comms Support ===== // library marker davegut.tpLinkCrypto, line 221
def getKlapParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 222
	Map reqParams = [timeout: 10, headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 223
	int seqNo = state.seqNo + 1 // library marker davegut.tpLinkCrypto, line 224
	state.seqNo = seqNo // library marker davegut.tpLinkCrypto, line 225
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 226
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 227
	byte[] encSig = new JsonSlurper().parseText(encSig) // library marker davegut.tpLinkCrypto, line 228
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 229

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv, // library marker davegut.tpLinkCrypto, line 231
									encSig, seqNo) // library marker davegut.tpLinkCrypto, line 232
	reqParams << [uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}", // library marker davegut.tpLinkCrypto, line 233
				  body: encryptedData.cipherData, // library marker davegut.tpLinkCrypto, line 234
				  contentType: "application/octet-stream", // library marker davegut.tpLinkCrypto, line 235
				  requestContentType: "application/octet-stream"] // library marker davegut.tpLinkCrypto, line 236
	return reqParams // library marker davegut.tpLinkCrypto, line 237
} // library marker davegut.tpLinkCrypto, line 238

def getAesParams(cmdBody) { // library marker davegut.tpLinkCrypto, line 240
	byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 241
	byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 242
	def cmdStr = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tpLinkCrypto, line 243
	Map reqBody = [method: "securePassthrough", // library marker davegut.tpLinkCrypto, line 244
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]] // library marker davegut.tpLinkCrypto, line 245
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}", // library marker davegut.tpLinkCrypto, line 246
					 body: new groovy.json.JsonBuilder(reqBody).toString(), // library marker davegut.tpLinkCrypto, line 247
					 contentType: "application/json", // library marker davegut.tpLinkCrypto, line 248
					 requestContentType: "application/json", // library marker davegut.tpLinkCrypto, line 249
					 timeout: 10, // library marker davegut.tpLinkCrypto, line 250
					 headers: ["Cookie": cookie]] // library marker davegut.tpLinkCrypto, line 251
	return reqParams // library marker davegut.tpLinkCrypto, line 252
} // library marker davegut.tpLinkCrypto, line 253

def parseKlapData(resp) { // library marker davegut.tpLinkCrypto, line 255
	Map parseData = [parseMethod: "parseKlapData"] // library marker davegut.tpLinkCrypto, line 256
	try { // library marker davegut.tpLinkCrypto, line 257
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 258
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 259
		int seqNo = state.seqNo // library marker davegut.tpLinkCrypto, line 260
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1] // library marker davegut.tpLinkCrypto, line 261
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey, // library marker davegut.tpLinkCrypto, line 262
														   encIv, seqNo)) // library marker davegut.tpLinkCrypto, line 263
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 264
	} catch (err) { // library marker davegut.tpLinkCrypto, line 265
		parseData << [status: "deviceDataParseError", error: err] // library marker davegut.tpLinkCrypto, line 266
	} // library marker davegut.tpLinkCrypto, line 267
	return parseData // library marker davegut.tpLinkCrypto, line 268
} // library marker davegut.tpLinkCrypto, line 269

def parseAesData(resp) { // library marker davegut.tpLinkCrypto, line 271
	Map parseData = [parseMethod: "parseAesData"] // library marker davegut.tpLinkCrypto, line 272
	try { // library marker davegut.tpLinkCrypto, line 273
		byte[] encKey = new JsonSlurper().parseText(encKey) // library marker davegut.tpLinkCrypto, line 274
		byte[] encIv = new JsonSlurper().parseText(encIv) // library marker davegut.tpLinkCrypto, line 275
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response, // library marker davegut.tpLinkCrypto, line 276
														 encKey, encIv)) // library marker davegut.tpLinkCrypto, line 277
		parseData << [status: "OK", cmdResp: cmdResp] // library marker davegut.tpLinkCrypto, line 278
	} catch (err) { // library marker davegut.tpLinkCrypto, line 279
		parseData << [status: "deviceDataParseError", error: err, dataLength: resp.data.length()] // library marker davegut.tpLinkCrypto, line 280
	} // library marker davegut.tpLinkCrypto, line 281
	return parseData // library marker davegut.tpLinkCrypto, line 282
} // library marker davegut.tpLinkCrypto, line 283

//	===== Crypto Methods ===== // library marker davegut.tpLinkCrypto, line 285
def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) { // library marker davegut.tpLinkCrypto, line 286
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 287
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 288
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 289
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 290
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 291
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 292
	byte[] cipherRequest = cipher.doFinal(request) // library marker davegut.tpLinkCrypto, line 293

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 295
	byte[] signature = mdEncode("SHA-256", payload) // library marker davegut.tpLinkCrypto, line 296
	cipherRequest = [signature, cipherRequest].flatten() // library marker davegut.tpLinkCrypto, line 297
	return [cipherData: cipherRequest, seqNumber: seqNo] // library marker davegut.tpLinkCrypto, line 298
} // library marker davegut.tpLinkCrypto, line 299

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) { // library marker davegut.tpLinkCrypto, line 301
	byte[] encSeqNo = integerToByteArray(seqNo) // library marker davegut.tpLinkCrypto, line 302
	byte[] ivEnc = [encIv, encSeqNo].flatten() // library marker davegut.tpLinkCrypto, line 303
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 304
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 305
	IvParameterSpec iv = new IvParameterSpec(ivEnc) // library marker davegut.tpLinkCrypto, line 306
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 307
	byte[] byteResponse = cipher.doFinal(cipherResponse) // library marker davegut.tpLinkCrypto, line 308
	return new String(byteResponse, "UTF-8") // library marker davegut.tpLinkCrypto, line 309
} // library marker davegut.tpLinkCrypto, line 310

def aesEncrypt(request, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 312
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 313
	SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 314
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 315
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 316
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tpLinkCrypto, line 317
	return result.replace("\r\n","") // library marker davegut.tpLinkCrypto, line 318
} // library marker davegut.tpLinkCrypto, line 319

def aesDecrypt(cipherResponse, encKey, encIv) { // library marker davegut.tpLinkCrypto, line 321
    byte[] decodedBytes = cipherResponse.decodeBase64() // library marker davegut.tpLinkCrypto, line 322
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tpLinkCrypto, line 323
    SecretKeySpec key = new SecretKeySpec(encKey, "AES") // library marker davegut.tpLinkCrypto, line 324
	IvParameterSpec iv = new IvParameterSpec(encIv) // library marker davegut.tpLinkCrypto, line 325
    cipher.init(Cipher.DECRYPT_MODE, key, iv) // library marker davegut.tpLinkCrypto, line 326
	return new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tpLinkCrypto, line 327
} // library marker davegut.tpLinkCrypto, line 328

//	===== Encoding Methods ===== // library marker davegut.tpLinkCrypto, line 330
def mdEncode(hashMethod, byte[] data) { // library marker davegut.tpLinkCrypto, line 331
	MessageDigest md = MessageDigest.getInstance(hashMethod) // library marker davegut.tpLinkCrypto, line 332
	md.update(data) // library marker davegut.tpLinkCrypto, line 333
	return md.digest() // library marker davegut.tpLinkCrypto, line 334
} // library marker davegut.tpLinkCrypto, line 335

String encodeUtf8(String message) { // library marker davegut.tpLinkCrypto, line 337
	byte[] arr = message.getBytes("UTF8") // library marker davegut.tpLinkCrypto, line 338
	return new String(arr) // library marker davegut.tpLinkCrypto, line 339
} // library marker davegut.tpLinkCrypto, line 340

int byteArrayToInteger(byte[] byteArr) { // library marker davegut.tpLinkCrypto, line 342
	int arrayASInteger // library marker davegut.tpLinkCrypto, line 343
	try { // library marker davegut.tpLinkCrypto, line 344
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) + // library marker davegut.tpLinkCrypto, line 345
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF) // library marker davegut.tpLinkCrypto, line 346
	} catch (error) { // library marker davegut.tpLinkCrypto, line 347
		Map errLog = [byteArr: byteArr, ERROR: error] // library marker davegut.tpLinkCrypto, line 348
		logWarn("byteArrayToInteger: ${errLog}") // library marker davegut.tpLinkCrypto, line 349
	} // library marker davegut.tpLinkCrypto, line 350
	return arrayAsInteger // library marker davegut.tpLinkCrypto, line 351
} // library marker davegut.tpLinkCrypto, line 352

byte[] integerToByteArray(value) { // library marker davegut.tpLinkCrypto, line 354
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4) // library marker davegut.tpLinkCrypto, line 355
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue) // library marker davegut.tpLinkCrypto, line 356
	return byteValue // library marker davegut.tpLinkCrypto, line 357
} // library marker davegut.tpLinkCrypto, line 358

def getRsaKey() { // library marker davegut.tpLinkCrypto, line 360
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB", // library marker davegut.tpLinkCrypto, line 361
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"] // library marker davegut.tpLinkCrypto, line 362
} // library marker davegut.tpLinkCrypto, line 363

// ~~~~~ end include (111) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (79) davegut.Logging ~~~~~
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

// ~~~~~ end include (79) davegut.Logging ~~~~~
