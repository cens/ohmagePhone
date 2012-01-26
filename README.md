ohmagePhone
============

ohmage (http://ohmage.org) is an open-source, mobile to web platform that records, 
analyzes, and visualizes data from both prompted experience samples entered by the 
user, as well as continuous streams of data passively collected from sensors or 
applications onboard the mobile device. 

Branches
--------

* master - the latest stable build (might be older)
* cuttingedge - Contains the newest changes
* NIH - Defaults to the single campaign version and includes the newest changes and changes
specific to the NIH campaign including charts and the food/stress button

Dependencies
------------

All the external libraries which are needed are included in the libs directory of the project,
but you will need the android SDK to build.

1. Download and install the the android SDK. Instructions on downloading and installing the
SDK can be found here: http://developer.android.com/sdk/installing.html. You can skip the parts
related to eclipse unless you want to setup eclipse as well.
2. Start the android configuration tool using `/path/to/android-sdk/tools/android`
3. Select SDK tools and SDK Platform-tools (and the USB driver if you are running on windows).
4. Under the Android 2.2 (API 8) section, select SDK Platform and Google APIs by Google Inc.
5. Click Install and install the packages
6. You are ready to start building!

Build Instructions
------------------

1. Edit any of the config information in `ant.properties` such as the server, or campaign mode.
2. Go to the ohmage repository directory and run `android update project --path .`
3. Run the project in eclipse or build the project with `ant debug` or `ant release` if
you want to sign it with a release key.
4. Push the application to the phone with `adb install bin/<apk_name>.apk` (where `<apk_name>` is
the name of the apk which was shown by the ant command).

If you are trying to build in eclipse and have a problem where the Config.java file can't be
found, make sure you clean the project as this will cause the Config.java file to be generated
with the values in ant.properties