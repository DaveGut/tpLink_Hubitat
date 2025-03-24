# Tapo Integration Application
This description is based on version 2.4.1 of the Tapo Integration.

## Application Main Page
![image](https://github.com/user-attachments/assets/caa66280-1f6b-4082-9c20-8ecfde72f4aa)

* Application Setup Parameters: Lists the current Lan Segments and Host Range that will be used during the discovery.
* Modify Application Setup (switch):  Allow user to cannge the Application Setup Parameters based on ADVANCED Router setup.
  * ![image](https://github.com/user-attachments/assets/4719b078-3899-483a-b98b-ca9cd506a43c)
  * These are EXPERT Setting requiring intimate knowledge of YOUR network.  Do not change unless required.
  * Lan Segments.  You can enter multiple segments by simply separating by a comma between (I.e., "192.168.50, 192.168.51".
  * Host Address Range.  Single range for ALL segments.  App will search all IPs on the segment within this range.
* Enter/Update Username and Password
  * Typically needs only accomplished on initial setup or if you changed your Tapo/Kasa credentials.
    * ![image](https://github.com/user-attachments/assets/90bcfd1a-cdf9-4a63-a9ae-850d9116d7a4)
    * Hide Password will secure the password in an unreadable format.
    * Email Address and Account Password.  Must not have any spaces.
    * Press Next to update password and go to previous page.
  * Scan for devices and add
    * Will not appear unles a properly formated username and password were entered in the previous function.
    * Takes about 30 seconds to complete.  Time can vary to up to 60 seconds.
    * For Hub and multi-plug switches, discovers only the parent.  The children are discovered via the device.
    * ![image](https://github.com/user-attachments/assets/b9c0c459-2736-4ce4-bb26-7e2430916553)
    * Found Devices
      * Lists state of devices found AND if the driver is installed in Hubitat (if not installed).
      * If a device is NOT on the list, exercise the device in the Tapo(Kasa) phone app and the select Rescan for Additional Devices
    * Devices to Add
      * ![image](https://github.com/user-attachments/assets/804532db-75fe-41b6-85b4-e599994ee655)
      * If the driver is installed, select the device
      * Select Next at bottom of page to install and exit return to the Main Page
* Remove Devices
  * ![image](https://github.com/user-attachments/assets/b8ed42e9-031c-41b2-ba3c-71b3cd628202)
  * Select devices to remove then Next to return to Main Page
* Debug Logging (switch).  Sets app to debug logging for 30 minutes.
* Remove: Removes the Application and all Application-child devices.
* Done: Normal exit of the Application.  Note:  Not exiting with Done can have adverse effects on the Application/Device performance.




        




