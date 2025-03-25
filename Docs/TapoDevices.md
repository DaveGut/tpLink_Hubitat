# Tapo Integrated Devices

### Supported Devices
The integration is designed to support the following devices.  Because of the large number of HW and FW versions, it is impractable to test all models of all device types.
* All Tapo plugs, switches, bulbs, Light Strips and Robovac
* Tapo H100 and Kasa KE100 Hubs
* Hub Sensors
* Later model Kasa switches and plugs (Kasa is transitioning new hardware versions to the Tapo API and Security protocols.

### Hubitat Capabilities Supported
The devices support the below Hubitat standard capabilities as applicable to the device type.  Hubitat standard capabilities are defined in 
    https://docs2.hubitat.com/en/developer/driver/capability-list.
This support includes both Commands and Attributes.  Data on command inputs and attribute format can be found in the above document.
* Refresh:  All devices.
* Configuration:  All non-child devices
* Switch: Plug/Switch, Dimmer, Color Bulb, Light Strip, Child Plug/Switch, Child Dimmer, Hub Plug
* Switch Level: Dimmer, Color Bulb, Light Strip
* Change Level: Dimmer, Color Bulb, Light Strip
* Light: Dimmer, Color Bulb, Light Strip, Child Dimmer
* Energy Meter: Energy Monitoring Devices
* Power Meter: Energy Monitoring Devices
* Color Control: Color Bulb, Light Strip
* Color Temperature: Color Bulb, Light Strip
* Color Mode: Color Bulb, Light Strip
* Alarm: Hub
* Fan Control: Child Fan
* Sensor: Hub Button, Hub Contact, Hub Leak, Hub Plug, Hub TempJumidity
* ContactSensor: Hub Contact
* Water Sensor: Hub Leak
* Battery: Hub Leak, Hub Plug, Hub TRV
* Temperature Measurement: Hub TempMumidity, Hub TRV
* Relative Humidity Measurement: Hub TempHumidity, Hub TRV

### Custom Capabilities
The following capabilities (command/input plus associated attributes) are incorporated into the various drivers: 
The integration incorporates the following custom commands
* setLightingEffect
  * name: Lighting Effect
  * constraints: effectList() (coded list of canned effects)
  * Attributes:
    * Effect Name
    * Effect Enable: Indicates if the effect is currently executing. 
  * driver: Light Strip
* Configure Alarm
  * name: Alarm Type, Constraints: alarmTypes (list of available default alarms)
  * name: Volume, constraints: [low, normal, high]
  * name: Duration (number)
  * Attribute alarmConfig: JSON formated of the last setting of the command
*  

### Custom Attributes
* level
  * Indicates Kasa-defined level of the fan
  * Driver: Child Fan
* lowBattery: All Hub-connected devices
* lasTriggerNo: Hub Button
* button (indicates button press/rotate value): Hub Button
* inAlarm: Hub Leak

### Common Preferences (non-Child / non-Sensor devices)
* pollInterval: set polling interval (WARNING: setting too many devices to sub-1 min poll intervals may impact your Hub's performance.  Do not change unless you need for a rule.)
* ledRule (devices with status LEDs):  Allows to set the LED to always on, never on or night mode.  (Note: Led Rule specifics can be set in Tapo Phone App).
* syncName: Synchronize the name between Hubitat and the Tapo App.  Can chose hubMaster or tapoAppMaster.
* rebootDev: Attempts to Reboot the device.
* logEnable:  Enables DEBUG logging for 30 minutes.
* infoLog:  Enable/disable info logging.

### Common Preferences (Child / Sensor devices)
* syncName: Synchronize the name between Hubitat and the Tapo App.  Can chose hubMaster or tapoAppMaster.
* logEnable:  Enables DEBUG logging for 30 minutes.
* infoLog:  Enable/disable info logging.
