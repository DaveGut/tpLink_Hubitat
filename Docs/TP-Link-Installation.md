# Tapo Integration Installation
The below instructions cover the installation of compable Tapo and Kasa devices (see readme for compatibility).

## Note on Matter Devices
Tapo and Kasa Matter devices can usually (not always) installed into Hubitat as a Matter device.  I recommend using the Hubitat Matter installation in preference to this when supported.

## Install Tapo Integration App and Drivers into Hubitat

Installation packages (HPM Name: File Name)
* Tapo Device Install: App/tapo_device_install.groovy
* Color Bulb: Drivers/tpLink_color_bulb.groovy
* Light Strip: Drivers/tpLink_lightStrip.groovy
* Plug, Switch, or EM Plug: Drivers/tpLink_plug.groovy
* Dimmer (Plug/Switch/Bulb): Drivers/tpLink_dimmer.groovy
* Parent (for multi-plugs/outlets/switches}: tpLink_parent.groovy
* _____Child Plug: Drivers/tpLink_child_dimmer.groovy
* _____Child Fan: Drivers/tpLink_child_fan.groovy
* Hub (Tapo or Kasa): Drivers/tpLink_hub.groovy
* _____Hub Button: Drivers/tpLink_hub_button.groovy
* _____Hub Contact Sensor: Drivers/tpLink_hub_contact.groovy
* _____Hub Motion Sensor: Drivers/tpLink_hub_motion.groovy
* _____Hub TempHumidity Sensor: Drivers/tpLink_hub_tempHumidity.groovy
* _____Hub Leak Detector: Drivers/tpLink_hub_leak.groovy
* _____Hub Thermostatic Radiator Valve: Drivers/tpLink_hub_trv.groovy
* _____Hub Plug/Switch: tpLink_hub_plug.groovy
* Robovac: Drivers/tpLink_robovac.groovy
	
Installation via Hubitat Package Manager:
* Search Term: Tapo
* Title: Tapo Plugs, Switches and Bulbs

Installation via manual direct links:
* Application: https://github.com/DaveGut/tpLink_Hubitat/tree/main/App
* Drivers: https://github.com/DaveGut/tpLink_Hubitat/tree/main/Drivers

## Before Hubitat Installation
The device must be installed into the Tapo (or Kasa) phone application and SET UP on you LAN prior to installing into Hubitat.
* Using the Tapo Phone App (or Kasa), install the device via the manufacturer's instructions.
* In your Router, set a DHCP Reservation (Static IP Address) for the device.  This assures the IP does not change periodically causing potential failure in Hubitat control and status.
* If devices are alreay installed, TEST the device via the Tapo phone app.  This assures the device is currently on your LAN.

## Run the Tapo Integration Application
* If not already installed as a Hubitat App, install the application.  See the Application Instructions for images of application pages and details on the application.
* Open the "Tapo Integration" application
* Check the following:
  * Application Setup Parameters: lanDiscoveryParams: Assure the Lan Segment is the same as you Hubitat device's segment (unless you have set up other segments).
  * Check that the Credentials are set.  If not select the Enter/Update Username and Password and add your credentials.  NOTE:  This Application does not access the Tapo/Kasa webserver.  The Credentials are used for local security of the devices.
* Select Scan for devices and add.
* From the Add Devices to Hubitat page
  * Check Found Devices list to assure your device was found and the DRIVER for the device was found on your Hubitat server.  Note that child Hub and Parent devices are not discovered in this app.  They are discovered as a part of the parent device installation process,
  * If the device is not on the Found Devices list, exercise the device via the Tapo (Kasa) application (at least two cycles with the device being ON when done).  The select Rescan for Additional Devices.
  * Using the drop-down, select the devices you wish to add,
  * Select Next
* From the Application Setup Page, select Done.
* Check that your device is installed in Hubitat and can be exercised (on/Off)
