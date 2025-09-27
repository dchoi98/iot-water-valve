# App Installation

1. If you have not yet done so, go to https://github.com/dchoi98/iot-water-valve, click the green Code button, and click Download ZIP to download the repository. Or download it directly from https://github.com/dchoi98/iot-water-valve/archive/refs/heads/main.zip. Extract the compressed .zip file.
2. Download and install Android Studio from https://developer.android.com/studio.
3. Click the Open button on the Welcom to Android Studio window, or go to ```File > Open```, and open the extracted repository.
4. If you have not yet done so, create an Adafruit IO account (instructions for doing so are found in ```adafruit_io_configuration.md```). Click the yellow button with the key icon in the header (left of the New Device button) and note your username and active key.
5. Create a ```local.properties``` file in the ```iot-water-valve-main/``` directory if it does not exist. Add the following lines to the file:

   ```
   secretAioUser="adafruit_io_username"
   secretAioKey="adafruit_io_key"
   ```

   Replace ```adafruit_io_username``` and ```adafruit_io_key``` with your username and key.
6. In Android Studio, click the Build Variants tab on the tool window bar on the left side of the IDE. Change the active build variant to "release."
7. To run the app on a virtual device within Android Studio, click the Device Manager tab on the left side of the IDE, go to ```Add a new device > Create Virtual Device```, select one of the devices from the list, click Next, then click Finish.
8. Alternatively, to run the app on your own Android phone, go to ```Settings > About phone > Software information``` and tap ```Build number``` seven times to enable developer options.
9. If Auto Blocker is enabled on your phone, go to ```Settings > Security and privacy > Additional security settings``` and disable it. Re-enable it after installing the app.
10. Connect your phone to the computer running Android Studio via USB. Go to ```Settings > Developer options``` and enable USB debugging.
11. Select the device that you would like install the app on in the Available Devices tab at the top of the IDE.
12. Click the Run button at the top of the IDE to build and install the app onto your device. If using a physical phone, it can be disconnected once the installation has finished.
##
© 2025. This work is openly licensed via [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).