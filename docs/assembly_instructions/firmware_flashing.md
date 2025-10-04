# Firmware Flashing

1. If you have not yet done so, go to https://github.com/dchoi98/iot-water-valve, click the green Code button, and click Download ZIP to download the repository. Or download it directly from https://github.com/dchoi98/iot-water-valve/archive/refs/heads/main.zip. Extract the compressed .zip file.
2. Create an Adafruit IO account if you have not done so (instructions for doing so are found in ```adafruit_io_configuration.md```). Click the yellow button with the key icon in the header (left of the New Device button) and note your username and active key.
3. In the ```esp32-firmware/``` directory, create an ```esp32_secrets.h``` file and add the following lines to it:

   ```
   #define SECRET_AIO_USER "adafruit_io_username"
   #define SECRET_AIO_KEY "adafruit_io_key"

   constexpr const char* SECRET_WIFI_SSID = "wifi_ssid";
   constexpr const char* SECRET_WIFI_PASS = "wifi_password";
   ```

   Replace ```adafruit_io_username```, ```adafruit_io_key```, ```wifi_ssid```, and ```wifi_password``` with your Adafruit IO username, Adafruit IO key, network name, and network password.

4. Download and install the Arduino IDE from https://www.arduino.cc/en/software/.
5. In the Arduino IDE, go to ```File > Open``` and open the ```esp32-firmware.ino``` file in the ```esp32-firmware/``` directory.
6. Go to ```Files > Preferences``` to open the Preferences window. Paste the following link into the ```Additional boards manager URLs``` field: ```https://espressif.github.io/arduino-esp32/package_esp32_index.json```.
7. Open the Boards Manager tab on the left side of the IDE. Search for and install the esp32 platform by Espressif Systems.
8. Open the Library Manager tab on the left side of the IDE. Search for and install the following libraries:
   - Adafruit MQTT Library
   - ArduinoOTA
   - ESP32Servo
9. Connect the ESP32-C3 SuperMini to your computer via USB.
10. Open the board dropdown menu at the top of the IDE and click ```Select other board and port```. The Select Other Board and Port window will appear. In the Boards field, select the MakerGO ESP32 C3 SuperMini. In the Ports field, select the port that the ESP32 is connected to. Click OK.
11. Click the Upload button at the top of the IDE. Once the firmware is finished uploading, the board can be disconnected.
12. The firmware includes support for over-the-air updates. You can upload firmware to the board without physically connecting to it as long as the board is connected to the same WiFi network as the computer and the firmware running on the board includes the ArduinoOTA library and the ```ArduinoOTA.handle()``` line in the ```loop()``` function body.
##
© 2025. This work is openly licensed via [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).