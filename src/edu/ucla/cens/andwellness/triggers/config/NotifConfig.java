package edu.ucla.cens.andwellness.triggers.config;

/*
 * Class containing the compile time constants which define the 
 * behavior of trigger notification
 */
public class NotifConfig {

	/* Default notif config */
	public static final String defaultConfig = 
		"{\"duration\": 60, \"suppression\": 30}";
	
	public static final int defaultRepeat = 5; //minutes
	public static final int maxDuration = 60; //minutes
	public static final int maxSuppression = 60; //minutes
}
