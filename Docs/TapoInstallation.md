# Tapo Integration Installation

## Install app and driver code
	a.	Use Hubitat Package Manager
 		1.	Search for Tag Lights and Switches or name tapo
   		2.	Select driver for your device based on description in HPM.
   	b.	Manual Installation
		1.	Use links in list at bottom of page to get to raw code.
  		2.	Copy and paste this data to a new app or new driver page
		3.	Install drivers for device type.
    
## Install device into the Tapo phone application (iPhone / Android)
	a.	Use manufacturer's instruction.
 	b.	Matter devices.  Need verification all functions work if installed vi Matter 
  		to non-Tapo Application
  	c.	After installation, CREATE a STATIC IP (DHCP Reservation) for the device on 
   		your WiFi Router.

## Install devices via the Tapo Application
	a.	Open a Log Page to view messages/errors during the installation process.
 	b.	Create/Open the App in Hubitat using "add user app"
 	c.	If you use non-standard IP segments, update by selecting Modify LAN
  		Configuraiton and update the segment.
	d.	Select Enter/Update tpLink Credentials.  This is required for logging onto
 		the individual devices.  After the credentials created, additional commands
   		will appear in the app.
	 	NOTE: Credentials are used during installation.
   	e.	Select "Add Tapo Devices".  It will take around 30 seconds to obtain the 
    		device data.
	f.	From the Add Tapo Devices to Hubitat page, select the devices to install 
 		from the drop-down list.  Then select Next.
   	g.	Exit the app (press done on the Tapo Device Installation page.
	h.	Go to the Hubitat Devices Page and insure all devices installed and working.
 		Note:  The log page has logs of success and failures.
   ## Tapo Device Children (Hub-connected or Multi-Plug)
   	a.	Open the Device's edit page,
    	b.	Select prefernece "Install Child Devices"
    	b.	Save preferences.

## Link to driver and app code.

  Application: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/App/tapo_device_install.groovy

  ### Wifi Connected Devices
  
  Plug-Switch: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug.groovy

  Dimming Plug-Switch: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_dimmer.groovy

  Energy Monitor Plug: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_em.groovy

  Multiple Plug Parent:  https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_multi.groovy

  Multiple Plug Child: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_plug_multi_child.groovy

  Color Bulb: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_bulb_color.groovy

  Dimming (Mono) Bulb: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_bulb_dimmer.groovy

  Light Strip: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_bulb_lightStrip.groovy

  Tapo Hub: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub.groovy

  ### Tapo Hub Connected Devices

  Button (no dimmer): https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_button.groovy

  Contact Sensor: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_contact.groovy

  Motion Sensor: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_motion.groovy

  Temp-Humidity Sensor: https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_hub_newType.groovy
  
