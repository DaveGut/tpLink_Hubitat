# TAPO Application Description:

## Main Page

![image](https://github.com/user-attachments/assets/0ece2d63-1fb2-40dd-baa1-0046eb223e98)

### Application Setup Parameters.
Provides the current setup parameters for the TAPO integration.

### Modify Application Setup (switch)
USE ONLY IF AN EXPERT.  Allows modification of the default LanSegments, Host Address Range, and Port Forwarding.

### Enter/Update Username and Password
Displays the Enter Credentials page (see below).

### Scan for devices and add
This button only appears if Credentials are already set.  If selected, it will

	a.	Start a scan of the LAN for available TP-Link devices (takes about 30 seconds).
 	b.	Display the add devices page (see below).

### Remove Devices
Displays the Remove devices page.

### Debug Logging switch
Turns debug (enhanced) logging on or off for purposes of troubleshooting.

## Enter Credentials Page
![image](https://github.com/user-attachments/assets/c252f769-7923-446c-a025-6c875c1a7f13)

	a.	Show Password.  Changes the password attribute so the password displays. Warning:  Deselect this before continuing.
 	b.	Update Credentials:  Select this to update the credentials before leaving this page.  These are stored as a password within Hubitat.
	c.	Current derived credData is displayed after creating the credential hashes.

## Add Devices Page
![image](https://github.com/user-attachments/assets/9da0128e-693f-433f-89b0-e442fae7d61c)

 	a.	Found Devices: Displays all found devices for this discovery.
	b.	Rescan for Additional Devices.  Does an immediate rescan for the devices.
 	c.	Devices to add (drop Down).  Displays the devices found for adding.

 Selecting a device from the drop-down and then "Next" (at bottom) will attempt to add the devices.
