ohmagePhone
============

ohmage (http://ohmage.org) is an open-source, mobile to web platform that records, 
analyzes, and visualizes data from both prompted experience samples entered by the 
user, as well as continuous streams of data passively collected from sensors or 
applications onboard the mobile device. 


Build Instructions
------------------

1. Edit any of the config information in `ant.properties` such as the server, or campaign mode
2. Generate the local.properties file by running (`android update project -p <project_path>`)
3. Run the project in eclipse or build the project with (`ant debug`) or (`ant release`) if
you want to sign it with a release key.

If you are trying to build in eclipse and have a problem where the Config.java file can't be
found, make sure you clean the project as this will cause the Config.java file to be generated
with the values in ant.properties


Dependencies
------------

All the external libraries which are needed are included in the libs directory of the project,
but you will need the android SDK to build. Instructions on downloading and installing the
sdk can be found here: http://developer.android.com/sdk/installing.html. We are actively
developing the apk for Android 2.2 so you should download version 8 of the sdk.