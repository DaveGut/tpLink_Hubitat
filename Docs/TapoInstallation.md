# Tapo Integration Installation

## Install app and driver code

	a.	Use Hubitat Package Manager
 		NOTE: HPM INSTALL NOT CURRENTLY ACTIVE
 		1.	Search for Keyword
   		2.	Will automatically load App, 'tapoNewWifiDevice" and 'tapoHub-NewType' drivers.
	 	3.	Select driver for your device based on description in HPM.
   	b.	Manual Installation
		1.	Use links in list at bottom of page to get to raw code.
  		2.	Copy and paste this data to a new app or new driver page
		3.	Install Application and two manditory drivers: 'tapoNewWifiDevice" 
  			and 'tapoHub-NewType'
  		4.	Install drivers for device type.

##	Install device into the Tapo phone application (iPhone / Android)

	a.  Use manufacturer's instruction.
 	b.	Matter devices.  Need verification all functions work if installed vi Matter to 
  		non-Tapo Application
  	c.	After installation, CREATE a STATIC IP (DHCP Reservation) for the device on 
   		your WiFi Router.

##	Install devices via the Tapo Application

	a.	Open a Log Page to view messages/errors during the installation process.
 	b.  Create/Open the App in Hubitat using "add user app"
 	c.	If you use non-standard IP segments, update by selecting Modify LAN Configuraiton
  		and update the segment.
	d.	Select Enter/Update tpLink Credentials.  This is required for logging onto the
 		individual devices.  After the credentials created, additional commands will appear
   		in the app.
	 	NOTE: Credentials are used during installation. If incorrect, install will fail.
   	e.	Select "Add Tapo Devices".  It will take around 30 seconds to obtain the device data.
	f.	From the Add Tapo Devices to Hubitat page, select the devices to install from the 
 		drop-down list.  Then select Next.
   	g.	Exit the app (press done on the Tapo Device Installation page.
	h.	Go th the Hubitat Devices Page and insure all devices installed and basically working.
 		Note:  The log page has logs that validate your selections and errors encountered.

## Link to driver and app code.

  Application: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/App/tapoInstallation.groovy

  ### Manditory Drivers

  NEW Wifi Device: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoNewWifiDevice.groovy

  NEW tapoHub-connected Device: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub_NewType.groovy

  ### Wifi Connected Devices
  
  Plug: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoPlug.groovy

  Plug - Dimmable: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoDimmableSwitch.groovy

  PLug - Energy Monitor: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoEMPlug.groovy

  Bulb - Color: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoColorBulb.groovy

  Bulb - Dimmable: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoDimmableBulb.groovy

  Light Strip: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoLightStrip.groovy

  Light Switch: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoSwitch.groovy

  Light Switch - Dimmable: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoDimmableSwitch.groovy

  Tapo Hub: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub.groovy

  ### Tapo Hub Connected Devices

  Button (no dimmer): https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub_Button.groovy

  Contact Sensor: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub_Contact.groovy

  Motion Sensor: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub_Motion.groovy

  Temp-Humidity Sensor: https://raw.githubusercontent.com/DaveGut/tapoHubitat/main/Drivers/tapoHub_TempHumidity.groovy
  
