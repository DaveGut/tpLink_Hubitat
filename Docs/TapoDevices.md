# TAPO Integration Device Type

### All non-children Devices
     a.  Hubitat Capabilities: Refresh, Switch
     b.  Custom Attributes:
         1.  connected.  From App.  Indicates is device was found in latest 15 minute device search.
         2.  commsError.  From device.  Indicates three consecutive comms Errors with associated 
                          correction attempts.
     c.  Preferences
         1.  nameSync.  Synchronize name between Hubitat and the Tapo device/app.
         2.  pollInterval. Sets interval for polling device.  Should only be used if you are using a
                           external (manual) action to execute Hubitat events.  Intervals of less 
                           than 1 minute can have impact on Hub performance and may cause warnings
                           and errors.
         3.  developerData.  Generates data for developer (when requested).
         4.  rebootDev.  Reboots the device then runs the updated method.  Use as a troubleshooting
                         step occassionally.
         5.  encKey, encIv, encCookie, encSit, token.  Data used to operate the communications protocol.
         6.  logEnable:  Enables debug logging for 30 minutes (troubleshooting)
         7.  infoLog:  Enable / Disable information logging in Hubitat Logs.
         
### Color Bulb
    a.  Verified on TAPO L530E(US) using tplink_bulb_color driver.
    b.  Added Capabilities: Light, Switch Level, Change Level, Color Temperature, Color Mode,
        Color Control
    c.  Added Preference. gradualOnOff. sets the device to provide the gradual on off 
        function.  Note: THE TAPO DEVICES DO NOT SUPPORT THE HUBITAT TRANSITION TIME 
        FEATURE.
    d.  Driver: tpLink_bulb_color
    
### Dimming Bulb
    a.  Verified on TAPO L530E(US) using tplink_bulb_dimmer driver.
    b.  Added Capabilities: Light, Switch Level, Change Level
    c.  Added Preference. gradualOnOff. sets the device to provide the gradual on off 
        function.  Note: THE TAPO DEVICES DO NOT SUPPORT THE HUBITAT TRANSITION TIME 
        FEATURE.
    d.  Driver: tpLink_bulb_dimmer

### Light Strip
    a.  Verified on TAPO L900-5(US) using tpLink_bulb_lightStrip driver.
    b.  Added Capabilities: Light, Switch Level, Change Level, Color Mode, Color Control
    c.  Custom commands:
        1.  Set White: Sets the bulb to a pure white light of color temp 9000.  NOTE THAT
            THE LIGHTSTRIP DOES NOT SUPPORT COLOR TEMPERATURE FUNCTIONS.
        2.  Set Lighting Effect:  Allows to set the lighting effect using pull-down.
    d.  Added Preference. gradualOnOff. sets the device to provide the gradual on off 
        function.  Note: THE TAPO DEVICES DO NOT SUPPORT THE HUBITAT TRANSITION TIME 
        FEATURE.
    e.  Driver: tpLink_bulb_lightStrip.

### HUB
    a.  Verified on TAPO H100(US) using tpLink_hub driver.
    b.  Capability SWITCH note.  For this device, the Capability Switch is used to indicate
        that the alarm has been activated and is active.  Setting the switch to ON will
        cause the configured alarm to play for the configured duration.  OFF stops the alarm.
    c.  Custom Commands: 
        1.  Configure Alarm:  Alows selection of alarm (via pull-down), volume, and duration
            when the alarm is activated.
        2.  Play Alarm Configuration:  Same as Configure Alarm, but then plays the alarm.
    d.  Custom Attribute: alarmConfigure. The current alarm Configuraiton.
    e.  Added Preference: installChild.  For adding child devices to the hub.
    f.  Driver: tpLink_bulb_lightStrip.

### Plug - Switch
    a.  Verified on TAPO P125M(US) using the tplink_plug driver.
    b.  Added Preferences:
        1.  Enable Auto Off.  Configure device's auto-off capabilities to on.
        2.  Auto Off Time:  Set the time for the auto-off function.
    c.  Driver: tplink_plug

### Dimming Plug - Switch
    a.  Verified:
        1.  TAPO P125M(US) using the tpLink_plug driver (plug functions)
        2.  TAPO L530E(US) using tplink_bulb_dimmer driver (dimmer functions)
    b.  Added Capabilities: Switch Level, Change Level
    c.  Added Preferences:
        1.  Enable Auto Off.  Configure device's auto-off capabilities to on.
        2.  Auto Off Time:  Set the time for the auto-off function.
        3.  gradualOnOff: sets the device to provide the gradual on off function.  Note: 
            THE TAPO DEVICES DO NOT SUPPORT THE HUBITAT TRANSITION TIME FEATURE.
    d.  Driver: tplink_plug_dimmer

### Energy Monitor Plug
    a.  Verified on KASA KP125M(US) (MATTER) energy monitor plug using the tplink_plug_em
        driver.  The KASA Matter devices use the same API and protocols as the Tapo
        devices.
    b.  Added Capabilities: Energy Monitor, Power Monitor
    c.  Added Attribute: energyThisMonth.
    d.  Added Preferences:
        1.  Enable Auto Off.  Configure device's auto-off capabilities to on.
        2.  Auto Off Time:  Set the time for the auto-off function.
        3.  Enable Power Protect.  Configure devices power protect function to on.
        4.  Power Protect Watts:  Set the wattage to use for power protect.
    e.  Driver: tplink_plug_em

### Multiple Plug
    a.  Verified on TP25(US) multi-plug using tpLink_plug_multi and 
        tpLink_plug_multi_child drivers.
    b.  Added Preference: Install Child Device.  Used to reinstall a deleted device.
    c.  Drivers:  tpLink_plug_multi and tpLink_plug_multi_child

### Tapo Robovac
    a.  Verified on Tapo RV10 vacuum.
    b.  Limitation:  No map-related functions.  (need co-developer for testing)
    c.  Capabilities:  Refresh, Battery, Actuator
    d.  Added commands:
        1. SetCleanPrefs. Set dynamic clean preferences: cleanPasses, vacuumSuction, waterLevel
        2.  cleanStart, cleanPause, cleanResume, dockVacuum
    e.  Added attributes:
        1.  cleanPasses
        2.  vacuumSuction
        3.  waterLevel
        4.  docking (vac is docking)
        5.  cleanOn
        6.  vacuumStatus
        7.  prompt (prompt from vacuum status related to errors.
        8.  promptCode
        9.  mopState
        10.  childLock
        11.  waterLevel
    f.  Added Attribute: energyThisMonth.
    g.  Added Preferences:
        1.  childLock
        2.  carpetClean.  Normal (standard) or Boost.
        3.  areaUnit
    h.  Driver: tplink_robovac

    
    
