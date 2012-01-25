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