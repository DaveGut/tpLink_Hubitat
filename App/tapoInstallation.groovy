/*	Kasa Integration Application
	Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md
===== Link to list of changes =====
	https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/Changes.pdf
===== Link to Documentation =====
	https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/Documentation.pdf

Version 2.3.5-1
No changes.
===================================================================================================*/
def nameSpace() { return "davegut" }
def appVersion() { return "2.3.5-1" }
import groovy.json.JsonSlurper
import java.security.MessageDigest

definition(
	name: "Tapo Installation",
	namespace: nameSpace(),
	author: "Dave Gutheinz",
	description: "Application to install TP-Link Tapo bulbs, plugs, and switches.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	installOnOpen: true,
	singleInstance: true,
	documentationLink: "",
	importUrl: ""
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
	if (!lanSegment) {
		def hub = location.hub
		def hubIpArray = hub.localIP.split('\\.')
		def segments = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
		app?.updateSetting("lanSegment", [type:"string", value: segments])
	}
	if (!hostLimits) {
		app?.updateSetting("hostLimits", [type:"string", value: "1, 254"])
	}
	startPage()
}

def startPage() {
	logInfo("starting Tapo Setup and Installation")
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
		logWarn("startPage: Invalid entry for Lan Segements, or Host Array Range. Resetting to default!")
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
				description: "Credentials are used by app and each Tapo devices during periodic login.  Take 30 seconds to pop to next page."
			
			if (encUsername && encPassword) {
				href "addDevicesPage",
					title: "<b>Add Tapo Devices</b>",
					description: "Searches LAN for devices and offers new devices for Hubitat installation"

				href "removeDevicesPage",
					title: "<b>Remove Tapo Devices</b>",
					description: "Provides interface to remove installed devices from Hubitat."
				
				href "<b>listDevicesPage",
					title: "<b>List the Tapo Devices</b>",
					description: "List the current Tapo Devices in the App database."
			}
			
//	Use default RSA Key (if personal keys are entered - error fallback.
//	Enter personal keys?????????

			paragraph " "
			input "debugLog", "bool",
				   title: "<b>Enable debug logging for 30 minutes</b>",
				   submitOnChange: true,
				   defaultValue: false
		}
	}
}

//	===== Enter Creentials =====
def enterCredentialsPage() {
	logInfo("enterCredentialsPage")
	return dynamicPage (name: "enterCredentialsPage", 
    					title: "Enter TP-Link (Tapo) Credentials",
						nextPage: startPage,
                        install: false) {
		section() {
			String currState = "<b>Current Credentials</b> = "
			if (state.userCredentials) {
				currState += "${state.userCredentials}"
			} else {
				currState += "NOT SET"
			}
			paragraph currState
			input ("username", "email",
            		title: "TP-Link Tapo Email Address", 
                    required: false,
                    submitOnChange: true)
			input ("password", "password",
            		title: "TP-Link Tapo Account Password",
                    required: false,
                    submitOnChange: true)
			if (username && password && username != null && password != null) {
				logDebug("enterCredentialsPage: [username: ${username}, pwdLen: ${password.length()}]")
				href "processCredentials", title: "Create Encoded Credentials",
					description: "You may have to press this twice."
			}
		}
	}
}
private processCredentials() {
	state.remove("userCredentials")
	String encUsername = mdEncode(username).bytes.encodeBase64().toString()
	app?.updateSetting("encUsername", [type: "password", value: encUsername])
	Map logData = [encUsername: encUsername]
	String encPassword = password.bytes.encodeBase64().toString()
	app?.updateSetting("encPassword", [type: "password", value: encPassword])
	logData << [encPassword: encPassword]
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
	Map logData = [devices: getDevices()]
	def devices = logData.devices
	state.devices = devices
	Map uninstalledDevices = [:]
	List installedDevices = []
	devices.each { device ->
		def isChild = getChildDevice(device.key)
		if (!isChild) {
			uninstalledDevices["${device.key}"] = "${device.value.alias}, ${device.value.driver}"
		} else {
			installedDevices << isChild
		}
	}
	uninstalledDevices.sort()
	logData << [uninstalledDevices: uninstalledDevices]
	logData << [installedDevices: installedDevices]
	logInfo("addDevicesPage: ${logData}")

	return dynamicPage(name:"addDevicesPage",
					   title: "Add Tapo Devices to Hubitat",
					   nextPage: startPage,
					   install: false) {
	 	section() {
			input ("selectedAddDevices", "enum",
				   required: false,
				   multiple: true,
				   title: "Devices to add (${uninstalledDevices.size() ?: 0} available).\n\t" +
				   "Total Devices: ${devices.size()}",
				   description: "Use the dropdown to select devices.  Then select 'Done'.",
				   options: uninstalledDevices)
			paragraph "<b>InstalledDevices: ${installedDevices}</b>"
		}
	}
}

//	===== Get Devices (created state.devices =====
def getDevices() {
	Map devices = [:]
	Map logData = [findDevices: findDevices()]
	Map devIps = state.ipList
	logData << [devIps: devIps]
	
	devIps.each { ipData ->
		def loginData = deviceLogin(ipData.value)
		def devData = getDeviceData(ipData.value, loginData.token, loginData.aesKey, loginData.cookie)
		if (devData.error_code == 0) {
			devData = devData.result
			def dni = devData.mac.replaceAll("-", "")
			def deviceData = parseDeviceData(dni, devData)
			if (deviceData.error) {
				logWarn("getDevices: [ERROR: ${deviceData}]")
				logData << [failedToAdd: dni]
			} else {
				devices << ["${dni}": deviceData]
				logData << ["${dni}": added]
			}
			pauseExecution(200)
		} else {
			logData << ["${ipData.key}": [ERROR: devData]]
		}
	}
	logData << [updateDeviceIps: updateDeviceIps()]
	logInfo("getDevices: ${logData}")
	return devices	
}

//	create map of dni/ip addresses
def findDevices() {
	Map logData = [segArray: state.segArray, hostArray: state.hostArray]
	def start = state.hostArray.min().toInteger()
	def finish = state.hostArray.max().toInteger() + 1
	logDebug("findDevices: [hostArray: ${state.hostArray}, portArray: pollSegment: ${state.segArray}]")
	state.ipList = []
	List deviceIPs = []
	state.segArray.each {
		def pollSegment = it.trim()
		logInfo("findDevices: Searching for LAN deivces on IP Segment = ${pollSegment}")
		for(int i = start; i < finish; i++) {
			deviceIPs.add("${pollSegment}.${i.toString()}")
		}
	}
	sendLanCmd(deviceIPs.join(','))
	pauseExecution(25000)
	return logData
}

def sendLanCmd(ip) {
	logDebug("sendLanCmd: ${ip}")
	def myHubAction = new hubitat.device.HubAction(
		"0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d",
		hubitat.device.Protocol.LAN,
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
		 destinationAddress: "${ip}:20002",
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
		 parseWarning: true,
		 timeout: 15,
		 ignoreResponse: false,
		 callback: "genIpList"])
	try {
		sendHubCommand(myHubAction)
	} catch (e) {
		logWarn("sendLanCmd: LAN Error = ${e}.\n\rNo retry on this error.")
	}
}

def genIpList(response) {
	Map ipList = [:]
	if (response instanceof Map) {
		def lanData = parseLanData(response.description)
		ipList << ["${lanData.mac}": convertHexToIp(lanData.ip)]
	} else {
		response.each {
			def lanData = parseLanMessage(it.description)
			ipList << ["${lanData.mac}": convertHexToIp(lanData.ip)]
		}
	}
	state.ipList = ipList
}

private String convertHexToIp(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) { Integer.parseInt(hex,16) }

//	get individual device data and create database
def getDeviceData(devIp, token, aesKey, cookie) {
	Map logData = [ip: devIp, token: token, aeskey: aesKey, cookie: cookie]
	def uri = "http://${devIp}/app?token=${token}"
	def cmdBody = [method: "get_device_info"]
	cmdBody = JsonOutput.toJson(cmdBody).toString()
	Map reqBody = [method: "securePassthrough",
				   params: [request: encrypt(cmdBody, aesKey)]]
	def respData = syncPost(uri, reqBody, cookie)
	def cmdResp = "ERROR"
	if (respData.respStatus == "OK") {
		logData << [respStatus: "OK"]
		respData = respData.data.result.response
		cmdResp = new JsonSlurper().parseText(decrypt(respData, aesKey))
	} else {
		logData << respData
	}
	if (logData.respStatus == "OK") {
		logDebug("getDeviceData: ${logData}")
	} else {
		logWarn("getDeviceData: ${logData}")
	}
	return cmdResp
}

def parseDeviceData(dni, devData) {
	Map deviceData = [dni: dni]
	if (devData.type.contains("TAPO")) {
		try {
			byte[] plainBytes = devData.nickname.decodeBase64()
			String alias = new String(plainBytes)
			deviceData << [alias: alias]
			deviceData << [type: devData.type]
			deviceData << [model: devData.model]
			deviceData << [ip: devData.ip]
			deviceData << [deviceId: devData.device_id]
			if (devData.color_temp_range) {
				deviceData << [ctLow: devData.color_temp_range[0]]
				deviceData << [ctHigh: devData.color_temp_range[1]]
			}
			deviceData << [driver: getDeviceDriver(devData.model)]
		} catch (err) {
			deviceData << [error: "Error creating deviceData"]
			logWarn("parseDeviceData: [ERROR: [msg: ${err}, data: ${devData}]]")
		}
	} else {
		deviceData << [error: "Not a Tapo Device"]
		logDebug("parseDeviceData: [notTapoDevice: ${deviceData}]")
	}
	return deviceData
}

def getDeviceDriver(model) {
	def driver = "tapoNewWifiDevice"
	switch(model) {
		case "P100":
		case "P105":
		case "P125":
		case "P125M":
			driver = "tapoPlug"; break
		case "P110":
		case "P115":
			driver = "tapoEMPlug"; break
		case "L510":
		case "L520":
		case "L620":
		case "TL31":
			driver = "tapoDimmableBulb"; break
		case "L530":
		case "L630":
		case "TL33":
			driver = "tapoColorBulb"; break
		case "L900":
		case "L920":
		case "L930":
			driver = "tapoLightStrip"; break
		case "S500":
		case "S505":
			driver = "tapoSwitch"; break
		case "S500D":
		case "S505D":
			driver = "tapoDimmableSwitch"; break
		case "H100":
		case "H200":
			driver = "tapoHub"; break
		default:
			driver = "tapoNewWifiDevice"
	}
	return driver
}

//	===== Add wifi devices to Hubitat =====
def addDevices() {
	def logData = [selectedAddDevices: selectedAddDevices]
	Map addedDevices = [:]
	Map failedAdds = [:]
	def devices = state.devices
	selectedAddDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = devices.find { it.value.dni == dni }
			def alias = device.value.alias.replaceAll("[\u201C\u201D]", "\"").replaceAll("[\u2018\u2019]", "'").replaceAll("[^\\p{ASCII}]", "")
			Map deviceData = [deviceIp: device.value.ip]
			deviceData << [deviceId: device.value.deviceId]
			deviceData << [tapoType: device.value.type]
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
				failedAdds << ["${device.key}": [label: alias, driver: device.value.driver]]
			}
		}
		logData << [installed: addedDevices, failedToInstall: failedAdds]
		pauseExecution(3000)
	}
	logInfo("addDevices: ${logData}")
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

//	===== List Tapo Devices =====
def listDevicesPage() {
	Map devices = state.devices
	List deviceList = []
	devices.each {
		def isChild = getChildDevice(it.value.dni)
		def installed = false
		if (isChild) { installed = true }
		deviceList << "<b>${it.value.alias}</b>: [${it.value.ip}, ${it.value.dni}, <b>${installed}</b>]"
	}
	deviceList.sort()
	def theListTitle = "<b>Total Tapo devices: ${deviceList.size() ?: 0}</b>\n"
	theListTitle += "<b>ALIAS: [IP, DNI, Installed?]</b>\n"
	theListTitle +=  "(Alias may not match the name on the device.)\n"
	def theList = ""
	deviceList.each {
		theList += "${it}\n"
	}
	return dynamicPage(name:"listDevicesPage",
					   title: "List Tapo Devices",
					   nextPage: startPage,
					   install: false) {
	 	section() {
			paragraph theListTitle
			paragraph "<p style='font-size:14px'>${theList}</p>"
		}
	}
}

//	===== Login Process =====
def deviceLogin(devIp) {
	Map logData = [:]
	def handshakeData = handshake(devIp)
	if (handshakeData.respStatus == "OK") {
		def tokenData = loginDevice(handshakeData.cookie, handshakeData.aesKey, devIp)
		if (tokenData.respStatus == "OK") {
			logData << [cookie: handshakeData.cookie,
						aesKey: handshakeData.aesKey,
						token: tokenData.token]
		} else {
			logData << [tokenData: tokenData]
		}
	} else {
		logData << [handshakeData: handshakeData]
	}
	return logData
}

def handshake(devIp) {
	Map handshakeData = [method: "handshakeData"]
	def pubKey = "-----BEGIN PUBLIC KEY-----\n${publicKey()}-----END PUBLIC KEY-----\n"
	Map cmdBody = [ method: "handshake", params: [ key: pubKey]]
	def uri = "http://${devIp}/app"
	def respData = syncPost(uri, cmdBody)
	if (respData.respStatus == "OK") {
		if (respData.data.error_code == 0) {
			String deviceKey = respData.data.result.key
			def aesArray = readDeviceKey(deviceKey)
			if (aesArray == "ERROR") {
				logData: [readDeviceKey: "FAILED"]
				handshakeData << [respStatus: "Failed decoding deviceKey",
								  deviceKey: deviceKey]
			} else {
				handshakeData << [respStatus: "OK"]
				def cookieHeader = respData.headers["set-cookie"].toString()
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";"))
				handshakeData << [cookie: cookie, aesKey: aesArray]
			}
		} else {
			handshakeData << [respStatus: "Command Error", data: respData.data]
		}
	} else {
		handshakeData << respData
	}
	return handshakeData
}

def readDeviceKey(deviceKey) {
	String privateKey = privateKey()
	Map logData = [privateKey: privateKey]
	def response = "ERROR"
	try {
		byte[] privateKeyBytes = privateKey.decodeBase64()
		byte[] deviceKeyBytes = deviceKey.getBytes("UTF-8").decodeBase64()
    	Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding")
		instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)))
		byte[] cryptoArray = instance.doFinal(deviceKeyBytes)
		response = cryptoArray
		logData << [cryptoArray: "REDACTED for logs", status: "OK"]
		logDebug("readDeviceKey: ${logData}")
	} catch (e) {
		logData << [status: "READ ERROR", data: e]
		logWarn("readDeviceKey: ${logData}")
	}
	return response
}

def loginDevice(cookie, cryptoArray, devIp) {
	Map tokenData = [method: "loginDevice"]
	def credentials = getCredentials()
	def uri = "http://${devIp}/app"
	String cmdBody = """{"method": "login_device", "params": {"password": "${credentials.encPassword}", "username": "${credentials.encUsername}"}, "requestTimeMils": 0}"""
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdBody, cryptoArray)]]
	def respData = syncPost(uri, reqBody, cookie)
	if (respData.respStatus == "OK") {
		if (respData.data.error_code == 0) {
			def cmdResp = decrypt(respData.data.result.response, cryptoArray)
			cmdResp = new JsonSlurper().parseText(cmdResp)
			if (cmdResp.error_code == 0) {
				tokenData << [respStatus: "OK", token: cmdResp.result.token]
			} else {
				tokenData << [respStatus: "Error in cmdResp", data: cmdResp]
			}
		} else {
			tokenData << [respStatus: "Error in respData,data", data: respData.data]
		}
	} else {
		tokenData << [respStatus: "Error in respData", data: respData]
	}
	return tokenData
}

//	===== Parent to device utilities =====
//	IP update
def updatIps(dni) {
	Map logData = [callingDni: dni]
	logData << [findDevices: findDevices()]
	logData << [updateDeviceIps: updateDeviceIps()]
	logInfo("updateIp: ${logData}")
}
def updateDeviceIps() {
	Map ipList = state.ipList
	logData = [ipList: ipList]
	ipList.each {
		def child = getChildDevice(it.key)
		if (child) {
			def deviceIp = child.getDataValue("deviceIp")
			if (deviceIp != it.value)  {
				child.updateDataValue("deviceIp", it.value)
				logData << ["${child}": [oldIp: deviceIp, newIp: it.value]]
			} else {
				logData << ["${child}": "noUpdated"]
			}
		} else {
			logData << ["${it.key}": "notChild"]
		}
	}
	logInfo("updateDeviceIps: ${logData}")
	return logData
}

//	credentials
def getCredentials() {
	Map credentials = [
		encUsername: encUsername,
		encPassword: encPassword
	]
	return credentials
}

//	RSA keys

//	===== TP-Link (Tapo) Credential Pages and Methods =====
//	https://www.devglan.com/online-tools/rsa-encryption-decryption
//	RSA Key Size: 1024 bit
//	Need to add pem header for TAPO
def publicKey() {
	return "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCSo13gEGRLUz7aY0eN6HStrxOTVEDkykTGJozwILdM85NSXeEceIuMH9DcVlJlxgcRZplfMRbWp4xtvTHNuF+9GdW2+Eanj0QFYs7HxKnN/Rk2Z7YaujzYF/0q5TkBu5GFfapnht5d9aDIyrJtO7uGQleEKuXoXV/abn//2Of4fwIDAQAB"
//????	state.keyReqActive = true
//	return state.publicKey
}

def privateKey() {
	return "MIICdAIBADANBgkqhkiG9w0BAQEFAASCAl4wggJaAgEAAoGBAJKjXeAQZEtTPtpjR43odK2vE5NUQOTKRMYmjPAgt0zzk1Jd4Rx4i4wf0NxWUmXGBxFmmV8xFtanjG29Mc24X70Z1bb4RqePRAVizsfEqc39GTZnthq6PNgX/SrlOQG7kYV9qmeG3l31oMjKsm07u4ZCV4Qq5ehdX9puf//Y5/h/AgMBAAECgYA4FkpyyHJEKWwSBgU5bx8py5xWLtS/bepOTDJ+KlVSFpxT1dqjlCv0BbtSe6X6jXromfCx60nMArwAwWvKPupHCK+v/FIUJI/wqQ6LwITUl+F7yni2Nlw+BYjpA1ZQ886bx+Pv5ONNwKdHpgd9Oa8dV5qXP04/ZEnrp8XR7mRaSQJBAOYct/Cpj+bSDcRI9a86xVlSH5XTnAILMxDN/b9i3WEkIBEeIMOB3klJXvQwcNt6nRta98BZVrvTAwtaM8H+V2sCQQCjIpPmL+JPlZDzoff2MpLmuxzyqYMCEURGoDbgvqPJAQOFSSvioWBZ/InSGs4XMF2zorMFYLLRDl5kJMTORGw9AkAQLtrcyP/+yqz/LeZhYW+5nWXtQomJN0JrHyGGUSyihUjgC09gkISSgN91quZ0+QWNg/NCisXnxapEJR7YuMtrAj8OqAyhoauzeryLJwIgHGsWT7lKE/CxPtvjfIFxr68HYA/w6aIHunftncLEHaRmcPH6MaPj18w4/BF+7c7AuWUCQHWQT/04gMGGmvb2+hvtgcVLCXAXCVhUo/XHSEFH5AVGQ6zFzxN5I5uArVL0AbaIWlLyyjX6/dKW9XLDo5cWJ4Q="
//????	state.keyReqActive = false
//	return state.privateKey
}
//	===== Utilities =====
def publicPem() {
	return "-----BEGIN PUBLIC KEY-----\n${publicKey()}-----END PUBLIC KEY-----\n"
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



// ~~~~~ start include (1327) davegut.tapoComms ~~~~~
library ( // library marker davegut.tapoComms, line 1
	name: "tapoComms", // library marker davegut.tapoComms, line 2
	namespace: "davegut", // library marker davegut.tapoComms, line 3
	author: "Dave Gutheinz", // library marker davegut.tapoComms, line 4
	description: "Tapo Communications", // library marker davegut.tapoComms, line 5
	category: "utilities", // library marker davegut.tapoComms, line 6
	documentationLink: "" // library marker davegut.tapoComms, line 7
) // library marker davegut.tapoComms, line 8
import groovy.json.JsonOutput // library marker davegut.tapoComms, line 9
import groovy.json.JsonBuilder // library marker davegut.tapoComms, line 10
import groovy.json.JsonSlurper // library marker davegut.tapoComms, line 11
import java.security.spec.PKCS8EncodedKeySpec // library marker davegut.tapoComms, line 12
import javax.crypto.spec.SecretKeySpec // library marker davegut.tapoComms, line 13
import javax.crypto.spec.IvParameterSpec // library marker davegut.tapoComms, line 14
import javax.crypto.Cipher // library marker davegut.tapoComms, line 15
import java.security.KeyFactory // library marker davegut.tapoComms, line 16

def sendMultiCmd(request, async, method=null, action="distData") { // library marker davegut.tapoComms, line 18
	Map cmdBody = [ // library marker davegut.tapoComms, line 19
		method: "multipleRequest", // library marker davegut.tapoComms, line 20
		params: [ // library marker davegut.tapoComms, line 21
			requests:[ // library marker davegut.tapoComms, line 22
				request, // library marker davegut.tapoComms, line 23
				[method: "get_device_info"]]]] // library marker davegut.tapoComms, line 24
	securePassthrough(cmdBody, async, method, action) // library marker davegut.tapoComms, line 25
} // library marker davegut.tapoComms, line 26

def securePassthrough(cmdBody, async=true, reqData=null, action = "distData") { // library marker davegut.tapoComms, line 28
	logDebug("securePassthrough: [cmdBody: ${cmdBody}, async: ${async}]") // library marker davegut.tapoComms, line 29
	cmdBody = JsonOutput.toJson(cmdBody).toString() // library marker davegut.tapoComms, line 30
	def uri = "http://${getDataValue("deviceIp")}/app?token=${deviceToken}" // library marker davegut.tapoComms, line 31
	Map reqBody = [method: "securePassthrough", params: [request: encrypt(cmdBody, getAesKey())]] // library marker davegut.tapoComms, line 32
	if (async) { // library marker davegut.tapoComms, line 33
		asyncPost(uri, reqBody, action, deviceCookie, reqData)	 // library marker davegut.tapoComms, line 34
	} else { // library marker davegut.tapoComms, line 35
		def respData = syncPost(uri, reqBody, deviceCookie) // library marker davegut.tapoComms, line 36
		Map logData = [reqDni: reqDni] // library marker davegut.tapoComms, line 37
		def cmdResp = "ERROR" // library marker davegut.tapoComms, line 38
		if (respData.respStatus == "OK") { // library marker davegut.tapoComms, line 39
			logData << [respStatus: "OK"] // library marker davegut.tapoComms, line 40
			respData = respData.data.result.response // library marker davegut.tapoComms, line 41
			cmdResp = new JsonSlurper().parseText(decrypt(respData, getAesKey())) // library marker davegut.tapoComms, line 42
		} else { // library marker davegut.tapoComms, line 43
			logData << respData // library marker davegut.tapoComms, line 44
		} // library marker davegut.tapoComms, line 45
		if (logData.respStatus == "OK") { // library marker davegut.tapoComms, line 46
			logDebug("securePassthrough - SYNC: ${logData}") // library marker davegut.tapoComms, line 47
		} else { // library marker davegut.tapoComms, line 48
			logWarn("securePassthrough - SYNC: ${logData}") // library marker davegut.tapoComms, line 49
		} // library marker davegut.tapoComms, line 50
		return cmdResp // library marker davegut.tapoComms, line 51
	} // library marker davegut.tapoComms, line 52
} // library marker davegut.tapoComms, line 53

//	===== AES Methods ===== // library marker davegut.tapoComms, line 55
def encrypt(plainText, keyData) { // library marker davegut.tapoComms, line 56
	byte[] keyenc = keyData[0..15] // library marker davegut.tapoComms, line 57
	byte[] ivenc = keyData[16..31] // library marker davegut.tapoComms, line 58

	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tapoComms, line 60
	SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.tapoComms, line 61
	IvParameterSpec iv = new IvParameterSpec(ivenc) // library marker davegut.tapoComms, line 62
	cipher.init(Cipher.ENCRYPT_MODE, key, iv) // library marker davegut.tapoComms, line 63
	String result = cipher.doFinal(plainText.getBytes("UTF-8")).encodeBase64().toString() // library marker davegut.tapoComms, line 64
	return result.replace("\r\n","") // library marker davegut.tapoComms, line 65
} // library marker davegut.tapoComms, line 66

def decrypt(cypherText, keyData) { // library marker davegut.tapoComms, line 68
	byte[] keyenc = keyData[0..15] // library marker davegut.tapoComms, line 69
	byte[] ivenc = keyData[16..31] // library marker davegut.tapoComms, line 70

    byte[] decodedBytes = cypherText.decodeBase64() // library marker davegut.tapoComms, line 72
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // library marker davegut.tapoComms, line 73
    SecretKeySpec key = new SecretKeySpec(keyenc, "AES") // library marker davegut.tapoComms, line 74
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivenc)) // library marker davegut.tapoComms, line 75
	String result = new String(cipher.doFinal(decodedBytes), "UTF-8") // library marker davegut.tapoComms, line 76
	return result // library marker davegut.tapoComms, line 77
} // library marker davegut.tapoComms, line 78

//	===== Sync comms for device update ===== // library marker davegut.tapoComms, line 80
def syncPost(uri, reqBody, cookie=null) { // library marker davegut.tapoComms, line 81
	def reqParams = [ // library marker davegut.tapoComms, line 82
		uri: uri, // library marker davegut.tapoComms, line 83
		headers: [ // library marker davegut.tapoComms, line 84
			Cookie: cookie // library marker davegut.tapoComms, line 85
		], // library marker davegut.tapoComms, line 86
		body : new JsonBuilder(reqBody).toString() // library marker davegut.tapoComms, line 87
	] // library marker davegut.tapoComms, line 88
	logDebug("syncPost: [cmdParams: ${reqParams}]") // library marker davegut.tapoComms, line 89
	Map respData = [:] // library marker davegut.tapoComms, line 90
	try { // library marker davegut.tapoComms, line 91
		httpPostJson(reqParams) {resp -> // library marker davegut.tapoComms, line 92
			respData << [status: resp.status, data: resp.data] // library marker davegut.tapoComms, line 93
			if (resp.status == 200) { // library marker davegut.tapoComms, line 94
				respData << [respStatus: "OK", headers: resp.headers] // library marker davegut.tapoComms, line 95
			} else { // library marker davegut.tapoComms, line 96
				respData << [respStatus: "Return Error"] // library marker davegut.tapoComms, line 97
			} // library marker davegut.tapoComms, line 98
		} // library marker davegut.tapoComms, line 99
	} catch (e) { // library marker davegut.tapoComms, line 100
		respData << [status: "HTTP Failed", data: e] // library marker davegut.tapoComms, line 101
	} // library marker davegut.tapoComms, line 102
	return respData // library marker davegut.tapoComms, line 103
} // library marker davegut.tapoComms, line 104

def asyncPost(uri, reqBody, parseMethod, cookie=null, reqData=null) { // library marker davegut.tapoComms, line 106
	Map logData = [:] // library marker davegut.tapoComms, line 107
	def reqParams = [ // library marker davegut.tapoComms, line 108
		uri: uri, // library marker davegut.tapoComms, line 109
		requestContentType: 'application/json', // library marker davegut.tapoComms, line 110
		contentType: 'application/json', // library marker davegut.tapoComms, line 111
		headers: [ // library marker davegut.tapoComms, line 112
			Cookie: cookie // library marker davegut.tapoComms, line 113
		], // library marker davegut.tapoComms, line 114
		timeout: 5, // library marker davegut.tapoComms, line 115
		body : new groovy.json.JsonBuilder(reqBody).toString() // library marker davegut.tapoComms, line 116
	] // library marker davegut.tapoComms, line 117
	try { // library marker davegut.tapoComms, line 118
		asynchttpPost(parseMethod, reqParams, [data: reqData]) // library marker davegut.tapoComms, line 119
//		asynchttpPost(parseMethod, reqParams, reqData) // library marker davegut.tapoComms, line 120
		logData << [status: "OK"] // library marker davegut.tapoComms, line 121
	} catch (e) { // library marker davegut.tapoComms, line 122
		logData << [status: e, reqParams: reqParams] // library marker davegut.tapoComms, line 123
	} // library marker davegut.tapoComms, line 124
	if (logData.status == "OK") { // library marker davegut.tapoComms, line 125
		logDebug("asyncPost: ${logData}") // library marker davegut.tapoComms, line 126
	} else { // library marker davegut.tapoComms, line 127
		logWarn("asyncPost: ${logData}") // library marker davegut.tapoComms, line 128
	} // library marker davegut.tapoComms, line 129
} // library marker davegut.tapoComms, line 130

//	===== Initial Parse Methods ===== // library marker davegut.tapoComms, line 132

def distData(resp, data) { // library marker davegut.tapoComms, line 134
	def cmdResp = parseData(resp) // library marker davegut.tapoComms, line 135
	if (cmdResp.status == "OK") { // library marker davegut.tapoComms, line 136
		switch(data.data) { // library marker davegut.tapoComms, line 137
			case "pollChildren": // library marker davegut.tapoComms, line 138
				childPollParse(cmdResp.cmdResp.result.child_device_list) // library marker davegut.tapoComms, line 139
				break // library marker davegut.tapoComms, line 140
			case "refreshChildren": // library marker davegut.tapoComms, line 141
				childRefreshParse(cmdResp.cmdResp.result.child_device_list) // library marker davegut.tapoComms, line 142
				break // library marker davegut.tapoComms, line 143
			default: // library marker davegut.tapoComms, line 144
				def devData = cmdResp.cmdResp.result // library marker davegut.tapoComms, line 145
				if (devData.responses) { // library marker davegut.tapoComms, line 146
					devData = devData.responses.find{it.method == "get_device_info"}.result // library marker davegut.tapoComms, line 147
					deviceParse(devData, data.data) // library marker davegut.tapoComms, line 148
				} // library marker davegut.tapoComms, line 149
		} // library marker davegut.tapoComms, line 150
	} // library marker davegut.tapoComms, line 151
} // library marker davegut.tapoComms, line 152

def parseData(resp) { // library marker davegut.tapoComms, line 154
	def logData = [:] // library marker davegut.tapoComms, line 155
	try { // library marker davegut.tapoComms, line 156
		logData << [respDataLength: resp.data.length()] // library marker davegut.tapoComms, line 157
		resp = new JsonSlurper().parseText(resp.data) // library marker davegut.tapoComms, line 158
		def cmdResp = new JsonSlurper().parseText(decrypt(resp.result.response, getAesKey())) // library marker davegut.tapoComms, line 159
		logData << [cmdResp: cmdResp] // library marker davegut.tapoComms, line 160
		if (cmdResp.error_code == 0) { // library marker davegut.tapoComms, line 161
			logData << [status: "OK"] // library marker davegut.tapoComms, line 162
		} else { // library marker davegut.tapoComms, line 163
			logData << [status: "Error from device response"] // library marker davegut.tapoComms, line 164
		} // library marker davegut.tapoComms, line 165
		state.commsCheck = false // library marker davegut.tapoComms, line 166
	} catch (e) { // library marker davegut.tapoComms, line 167
		logData << [status: e, data: resp] // library marker davegut.tapoComms, line 168
	} // library marker davegut.tapoComms, line 169
	if (logData.status == "OK") { // library marker davegut.tapoComms, line 170
		logDebug("parseData: ${logData}") // library marker davegut.tapoComms, line 171
	} else { // library marker davegut.tapoComms, line 172
		logWarn("parseData: ${logData}") // library marker davegut.tapoComms, line 173
	} // library marker davegut.tapoComms, line 174
	return logData // library marker davegut.tapoComms, line 175
} // library marker davegut.tapoComms, line 176

// ~~~~~ end include (1327) davegut.tapoComms ~~~~~
