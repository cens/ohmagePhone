package edu.ucla.cens.andwellness.triggers.config;

/*
 * Class containing the compile time constants which define the 
 * behavior of location triggers
 */
public class LocTrigConfig {
	//Minimum allowed gap between re-entries in order to be triggered
	public static final int MIN_REENTRY = 120; //minutes
	//The default radius of any location
	public static final float LOC_RADIUS_DEFAULT = 70; //m
	//The minimum allowed radius of any location
	public static final float LOC_RADIUS_MIN = 50;//m
	//The maximum allowed radius of any location
	public static final float LOC_RADIUS_MAX = 200;//m
	//Minimum allowed gap between any two locations
	public static final float MIN_LOC_GAP = 200;//m
	//Whether fall back to network based provider
	//when GPS is available
	public static final boolean useNetworkLocation = true;
	//Whether to use motion detection
	public static final boolean useMotionDetection = true;
}
