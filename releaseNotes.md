Version 2.4.3 updates

	1.	Added support for Tapo Camera, Camera Pan,Tilt, Zoom and Doorbel.  Includes support for Battery versions.
	2.	Application: Added list of not supported devices that were discovered.  (data is deviceType and reason only.)

Known Issues:

	a.	Tapo has a new transport/encryption protocol: TPAP. This is not yet supported.  Currently RV30 is only impacted device.
	b.	Battery Cameras on Battery only.  I developed special polling; however, motion polling will still SIGNIFCANTLY reduce battery life.
	c.	Battery Camera - wired.  No device.  Assume normal polling.  Not validated.
	d.	Still no camera video streaming control/processing integrated into driver.
	e. 	No support for TapoChime, H200 nor H500 hubs. (in work).



 Version Update Instructions:
 
 	HPM:  Use HPM Update function.  Note: this may create some duplicate tpLink Plug drivers.  This is an artifact of reducing driver and will not impact performance.
  	Manual:
   	  a.  App: use the driver edit page "import" function using the default file path.
	  b.  Drivers:  Use the driver edit pahge "import" function using the file paths from 'https://github.com/DaveGut/tpLink_Hubitat/tree/main/Drivers'
  

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





(New Kasa devicemodels and New Kasa HW Versions after approximately 6/12/24 have been remodeled to the TpLInk (smart) protocol).
				If failure
					LOG ERROR with details.
					Request user to check LAN Configuration.
	Total path successful, run remainder of configure.	

