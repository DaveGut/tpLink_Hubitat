# Tapo Integration Installation
The below instructions cover the installation of compable Tapo and Kasa devices (see readme for compatibility).

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

## Install Tapo/Kasa Devices
* Using the Tapo Phone App (or Kasa), install the device via the manufacturer's instructions.
* In your Router, set a DHCP Reservation (Static IP Address) for the device.  This assures the IP does not change periodically causing potential failure in Hubitat control and status.
* If devices are alreay installed, TEST the device via the Tapo phone app.  This assures the device is currently on your LAN.

## Tapo Integration Application
Initial installation of application:
* Add the application to the Hubitat Applications.
* Open the "Tapo Integration" application
  * See Application 
* 




Kasa or Tapo Phone App

* Insure the devices are installed in either the Kasa or Tapo phone app (the Tapo phone app also handles all Kasa Devices).

Using HPM, install the Tapo integration.  Search term is Tapo
* Select the drivers for your Tapo devices.
* Close HPM.

Open the app.
![image|615x464](upload://yp0DwbWZpkSzOxtf4yJQMmAbgqX.png)

* Check the segement.  It should be the same segment as your hub.  If not, select Midify Application Setup to change the segment.
* Enter/Update Username and password.  Used for LOCAL LAN security of the device.  Assure no spaces before/after fields.  When done, select Next.
* Scan for devices and add
![image|532x500](upload://iF21LNFoygtRJlVa1DwMN3BMqIu.png)
  * Sometimes a device is not discovered (usually temporarily disconnected).  If you are missing devices, turn the device ON via the phone app the select Rescan for Additional devices.
  * Assure all desired devices have drivers.  If not, use HPM to add the missing driver.
  * Select the devices from the dropdown and then next.
    * Note: be careful to not install already installed MATTER capable devices.  (This app will allow, it will likely cause no issues except confusion.)
* Select done from the main page.# TPLINK HUBITAT
Supports the following device products from TP-Link:

	a.	Tapo Bulbs, Plugs, and Switches
 	b.	Tapo and Kasa K100 and H100 Hubs.   DOES NOT SUPPORT THE H200/H500 HUBS/
  	c.	Hub-connected sensors and the S200B Button (no support for Hub connected
   		switches nor dimmer (need cooperative tester).
    	d.	KASA Matter Devices.  Intention is an alternative to the Hubitat-native
      		Matter implementation in case it does not support the device being installed.
	e.	New KASA Models and NEW KASA Model Hardware versions.  Models and hardware
 		hardware versions released in 2024 and later.  Depending on device, these
   		devices are either supported by this integration or the Hubitat Build-In
     	integration

### Wifi connected devices not currently supported:
	a.	Smart Cameras.  Not in Support Plan.
 	b.	Tapo H200 and H500 Hubs (require camera security profile).

## Documentation:

App description:  https://github.com/DaveGut/tpLink_Hubitat/blob/main/Docs/TapoApplication.md

Installation: https://github.com/DaveGut/tpLink_Hubitat/blob/main/Docs/TP-Link-Installation.md

Installed Device Information: [https://github.com/DaveGut/tpLink_Hubitat/blob/main/Docs/TapoDevices.md](https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Docs/TapoDevices.md)

GitHub data used in development.  I greatly appreciate the data provided in the below repositories in getting this implementation completed.

	https://github.com/dickydoouk/tp-link-tapo-connect
	https://github.com/mihai-dinculescu/tapo
 
