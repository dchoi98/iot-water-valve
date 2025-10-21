# IoT Water Valve
This project includes an ESP32-based water-valve controller and its Android companion app. It utilizes secure MQTT communication via Adafruit IO for real-time control of the valve from anywhere, with built-in leak detection and emergency shutoff. Design decisions and FMEA are explained in ```design_decisions.md``` and ```reliability_and_safety.md``` in ```docs/```.

## Key Features
- ESP32-C3 SuperMini microcontroller
- Remote control via Android app over WiFi
- Adafruit IO MQTT integration over TLS
- OTA (Over-The-Air) firmware updates
- Water leak detection with push notifications and automatic shutoff
- Solar-powered with 18650 Li-ion battery
- Low-power design for continuous operation
- 30-minute auto-shutoff timer when valve opened
- Soft-latching circuit with manual power-off capability
- Weatherproof enclosure with sunshade

## Specifications
- ~60 mA average current draw
- 200 mA maximum charge rate from solar panel; 1 A charge rate from USB cable
- 2500 mAh battery
- Operating temperature: 4–TODO °C

## Quick Start Guide
### 1. Adafruit IO Configuration
1. Create an account at https://io.adafruit.com
2. Navigate to Feeds and create three new feeds:
    - ```valve-ack```
    - ```valve-control```
    - ```water-sensor```
3. Note your username and active key (yellow key icon in header)

### 2. Hardware Assembly
1. Order PCB from JLCPCB using provided gerber files
2. 3D print required parts (frame, valve coupler, sunshade, gasket)
3. Drill enclosure holes per specifications
4. Assemble components following detailed instructions in hardware_assembly.md

### 3. Firmware Flashing
1. Create ```esp32-firmware/esp32_secrets.h```
    ```
    #define SECRET_AIO_USER "your_username"
    #define SECRET_AIO_KEY "your_key"
    constexpr const char* SECRET_WIFI_SSID = "your_ssid";
    constexpr const char* SECRET_WIFI_PASS = "your_password";
    ```
2. Configure Arduino IDE:
    - Add ESP32 board URL: ```https://espressif.github.io/arduino-esp32/package_esp32_index.json```
    - Install __esp32__ platform by Espressif Systems
    - Install required libraries: Adafruit MQTT Library, ArduinoOTA, ESP32Servo
3. Connect ESP32-C3 SuperMini via USB
4. Select board: __MakerGO ESP32 C3 SuperMini__
5. Upload ```esp32-firmware.ino```

### 4. App Installation
1. Open project in Android Studio
2. Create ```local.properties``` in project root:
    ```
    secretAioUser="your_username"
    secretAioKey="your_key"
    ```
3. Change build variant to __release__
4. Set up device:
    - __Virtual:__ Create device via Device Manager
    - __Physical:__ Enable USB debugging in Developer Options
5. Select device and click __Run__

### Verification
- Device should connect to WiFi and Adafruit IO
- App should display "Water Sensor Active" notification
- Test valve control using app buttons
- Verify sensor readings in Adafruit IO dashboard

### Support
For detailed assembly and troubleshooting, refer to the full documentation files.

## Future Enhancements
- Battery-level monitoring
- Charge-rate adjustment based on battery temperature
- Cut power to water sensor when not measuring to save battery
- Improve water sensor positioning for more robust leak detection

## Licensing

The contents of `hardware/` are licensed under CERN-OHL-S-2.0. This document and the contents of `docs/` are licensed under CC BY-SA 4.0. All other content in this repository is licensed under GPL-3.0-or-later.
##
© 2025. This work is openly licensed via [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).