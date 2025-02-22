Version 2.4.1 Changes

Application Changes:
a.	Needed Driver List.  Now check for Hub installed drivers and drivers 
	required for found devices.  Creates list of drivers NOT installed
	and inform user of need to install on the Add Devices page.

All Devices:
	a.	Added Sync Name with Tapo App capability
	b.	Simplified setPollInterval and new Refresh mapping.

EM Plugs/Switches/Outlets
	a.	Added power poll option separate from system refresh (on-off Poll). 
		Caution: using 5 seconds on both will impact hub performance.
	b.	Separated Power polling from EM Data Polling (consumption today, 
		past 7 days,  past 30 days. This is scheduled for every 30 minutes.

New Device Type Support (Beta Versions)
	a.	Child Fan.  Example KS240 dimming switch/Fan Control. Requires Parent Driver.
	b.	Hub TRV.  Example: Kasa KE100.  Requires Hub Driver.
	c.	Hub Plug/Switch:  Example: Tapo S210/Tapo S220.  Requires Hub Driver.
	d.	Hub Temp/Humidity Sensor: Example: Tapo S310.  Requires Hub Driver.

Currently not supported:  Tapo H200 Hub, Tapo H500 Hub, Any Camera, legacy Kasa Devices.
