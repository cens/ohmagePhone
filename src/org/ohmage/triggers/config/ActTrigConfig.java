package org.ohmage.triggers.config;

public class ActTrigConfig {
	
	/*
	 * The timeout between sampling when there are valid triggers to be checked for.
	 */
	public static final long COUNT_TIMEOUT = 1000L * 60L * 1L; // 1min.

	/*
	 * The time after which "open time range" triggers should not go off
	 */
	public static int OPEN_TIME_RANGE_SLEEP_HOUR = 21;
	public static int OPEN_TIME_RANGE_SLEEP_MINUTE = 0;
	
	/*
	 * The time after which "open time range" triggers are automatically put back on, even if 
	 * service has not sensed that user is awake.
	 */
	
	public static int OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR = 9;
	public static int OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE = 0;
	
	/*
	 * The time from which the service will start scanning to see if the user is awake
	 */
	public static int WAKEUP_SCAN_HOUR = 6;
	public static int WAKEUP_SCAN_MINUTE = 0;
	/*
	 * The interval between wakeup scans. (if the wakeup is set at 6am, and the interval is 
	 * 1 hour, it will scan at 6am , 7am, 8am, and at 9am, it will just turn the triggers back on
	 */
	public static long WAKEUP_SCAN_INTERVAL = 1000L * 60L * 60L; //1 hour
}
