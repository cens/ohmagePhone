package edu.ucla.cens.andwellness.triggers.config;

/*
 * Class containing the compile time constants which define the 
 * behavior of the user interface in non-admin mode
 */
public class TrigUserConfig {
	
	public static final String adminPass = "0000";
	
	public static boolean addTrigers = false;
	public static boolean removeTrigers = false;
	public static boolean editTriggerActions = false;	
	
	public static boolean editLocationTrigger = false;
	/* */
	
	public static boolean editTimeTrigger = true;
	public static boolean editTimeTriggerTime = true;
	public static boolean editTimeTriggerRepeat = false;
	public static boolean editTimeTriggerRange = false;
	
	public static boolean editNotificationSettings = false;
	/* */

	public static boolean editTriggerSettings = true;
	public static boolean editLocationTriggerSettings = true;
	public static boolean editLocationTriggerPlace = false;
}
