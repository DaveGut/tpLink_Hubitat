/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md

=================================================================================================*/
//	=====	NAMESPACE	============
def nameSpace() { return "davegut" }
//	================================

metadata {
	definition (name: "tpLink_hub_contact", namespace: nameSpace(), author: "Dave Gutheinz", 
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_contact.groovy")
	{
		capability "Refresh"
		capability "Contact Sensor"
		attribute "lowBattery", "string"
	}
	preferences {
		input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
	}
}

def installed() { 
	updateAttr("commsError", "OK")
	runIn(1, updated)
}

def updated() {
	unschedule()
	def logData = [:]
	logData << setLogsOff()
	logData << [status: "OK"]
	if (logData.status == "ERROR") {
		logError("updated: ${logData}")
	} else {
		logInfo("updated: ${logData}")
	}
}

def refresh() { parent.refresh() }

//	Parse Methods
def devicePollParse(childData) {
	try {
		def contact = "closed"
		if (childData.open) { contact = "open" }
		updateAttr("contact", contact)
		updateAttr("lowBattery", childData.at_low_battery.toString())
	} catch (err) {
		Map logData = [method: "devicePollParse", status: "ERROR",
					   childData: childData, error: err]
		logWarn(logData)
	}
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}

//	Library Inclusion


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
