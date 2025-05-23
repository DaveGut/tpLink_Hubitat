NEW FILE NAMES FOR MANUAL UPDATE (HPM did this automatically)
Some driver file names have changed to simplify the product structure and reduce the total number of drivers.  Below is a mapping of the changes:

 	tpLink_bulb_color : tpLink_color_bulb
	tpLink_bulb_dimmer : tpLink_dimmer
	tpLink_bulb_lightStrip : tpLink_lightStrip
	tpLink_plug_dimmer : tpLink_dimmer
	tpLink_plug_em : tpLink_plug 
	tpLink_plug_multi : tpLink_parent
	tpLink_plug_multi_child : tpLink_child_plug

 Version Update Instructions:
 
 	HPM:  Use HPM Update function.  Note: this may create some duplicate tpLink Plug drivers.  This is an artifact of reducing driver and will not impact performance.
  	Manual:
   	  a.  App: use the driver edit page "import" function using the default file path.
	  b.  Drivers:  Use the driver edit pahge "import" function using the file paths from 'https://github.com/DaveGut/tpLink_Hubitat/tree/main/Drivers'
  

App Changes:
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

Application Changes:

a.	Needed Driver List.  Now check for Hub installed drivers and drivers 
	required for found devices.  Creates list of drivers NOT installed
	and inform user of need to install on the Add Devices page.


Version 2.4.1 Changes:
All Devices:

	a.	Added Sync Name with Tapo App capability
	b.	Simplified setPollInterval and new Refresh mapping.
EM Plugs/Switches/Outlets

	a.	Added power poll option separate from system refresh (on-off Poll). Caution: using 5 seconds on both will impact hub performance.
	b.	Separated Power polling from EM Data Polling (consumption today, past 7 days,  past 30 days. This is scheduled for every 30 minutes.
New Device Type Support (Beta Versions)

	a.	Child Fan.  Example KS240 dimming switch/Fan Control.  Requires Parent Driver.
	b.	Hub TRV.  Example: Kasa KE100.  Requires Hub Driver.
	c.	Hub Plug/Switch:  Example: Tapo S210/Tapo S220.  Requires Hub Driver.
	d.	Hub Temp/Humidity Sensor: Example: Tapo S310.  Requires Hub Driver.
 
Device Energy Monitor:  Now integrated into the basic device where the capability exists in the device.

Plug EM Driver.  Now deprecated.  Existing installations will continue to work - but strongly recommend changing driver to Plug.

Dimmers: Added capability "Light" to dimmer and child dimmer.

New Tapo S6 Series Switches.  No information avaialble on these device.  Assume is that basic

Switch, Dimming Switch, and fan/dimming Switch functions will work using this integration.

RoboVac: Updated code to use common library.

Currently not supported:  Tapo H200 Hub, Tapo H500 Hub, Any Camera, legacy Kasa Devices.

(New Kasa devicemodels and New Kasa HW Versions after approximately 6/12/24 have been remodeled to the TpLInk (smart) protocol).
				If failure
					LOG ERROR with details.
					Request user to check LAN Configuration.
	Total path successful, run remainder of configure.	
