ohmagePhone
============

ohmage (http://ohmage.org) is an open-source, mobile to web platform that records, 
analyzes, and visualizes data from both prompted experience samples entered by the 
user, as well as continuous streams of data passively collected from sensors or 
applications onboard the mobile device. 

Branches
--------

* master - Contains the newest changes
* NIH - Defaults to the single campaign version and includes the newest changes and changes
specific to the NIH campaign including charts and the food/stress button
* mobilize - mobilize version of ohmage which has mobilize branding
* ptsd - ptsd version of ohmage

Dependencies
------------

You will need to download [ProbeLog](https://github.com/cens/ProbeLog) and make it available to
ohmage as a library apk. (Alternatively you could just change all logging functionality to use
the default android logs instead of using ProbeLog)

All other external libraries which are needed are included in the libs directory of the project,
but you will need the android SDK to build.

1. Download and install the the android SDK. Instructions on downloading and installing the
SDK can be found here: http://developer.android.com/sdk/installing.html. You can skip the parts
related to eclipse unless you want to setup eclipse as well.
2. Start the android configuration tool using `/path/to/android-sdk/tools/android`
3. Select **SDK tools** and **SDK Platform-tools** (and the USB driver if you are running on windows).
4. Under the **Android 2.2 (API 8)** section, select **SDK Platform** and **Google APIs by Google Inc**.
5. Click Install and install the packages
6. You are ready to start building!

Build Instructions
------------------

1. Make sure the Android SDK dependencies are installed on your system.
2. Download/clone the ohmagePhone repository form github.
3. Edit any of the config information in `res/values/config.xml` such as the server, or campaign mode.
4. Go to the ohmagePhone directory and run `/path/to/android-sdk/tools/android update project --path .`
5. Run the project in eclipse or build the project from the command line using `ant debug`. Alternatively
you can use `ant release` if you want to sign it with a release key.
6. If compile succeeds, the .apk (application package) is in the ohmagePhone/bin directory. 
Copy it to the phone and open it to install. Alternatively, if the phone is plugged in you 
can use `adb install bin/<apk_name>.apk` (where `<apk_name>` is the name of the apk which was 
shown by the ant command).

Testing
-------

We are using a combination of [robotium](http://code.google.com/p/robotium/) and
[calabash-android](https://github.com/calabash/calabash-android) (which is basically an android
implementation of [cucumber](https://github.com/cucumber/cucumber)). Robotium tests are in the test folder
and can be run as unit tests. The cucumber tests requires calabash-android to be installed. At this point
this [fork](https://github.com/cketcham/calabash-android) must be used to do the testing as it includes
additional functionality not available in the main branch. Clone the fork, change into the `ruby-gem`
directory and run `rake install` (you might need `sudo rake install` depending on how your gems are
installed.) Then you can run `calabash-android build` to build the testing apk, and finally
`calabash-android run` to start the tests.