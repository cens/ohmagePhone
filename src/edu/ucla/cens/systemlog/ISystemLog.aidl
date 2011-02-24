package edu.ucla.cens.systemlog;

interface ISystemLog 
{

    /**
     * Registers the given tag with the application name.
     * All logs with the given tag will be recorded with the given
     * application name.
     * 
     *
     * @param       tag         tag that will be used for logging
     * @param       appName     Application name 
     * @return                  registration result. True if succeeds.
     */
    boolean registerLogger (in String tag, in String dbTable);


    /**
     * Returns true of the given tag has been registered.
     *
     * @param       tag         tag to check for registeration status
     * @return                  true if the tag has been registered
     */
    boolean isRegistered (in String tag);

	/**
	 * Sends the given verbose-level log message to be logged with 
	 * the given tag.
	 *
	 * @param		tag			tag associated with the log message
	 * @param		message		log message
	 */
	boolean verbose (in String tag, in String message);

	/**
	 * Sends the given info-level log message to be logged with 
	 * the given tag.
	 *
	 * @param		tag			tag associated with the log message
	 * @param		message		log message
	 */
	boolean info (in String tag, in String message);
	
	
	/**
	 * Sends the given debug-level log message to be logged with 
	 * the given tag.
	 *
	 * @param		tag			tag associated with the log message
	 * @param		message		log message
	 */
	boolean debug (in String tag, in String message);	
	

	/**
	 * Sends the given warning-level log message to be logged with 
	 * the given tag.
	 *
	 * @param		tag			tag associated with the log message
	 * @param		message		log message
	 */
	boolean warning (in String tag, in String message);
	

	/**
	 * Sends the given error-level log message to be logged with 
	 * the given tag.
	 *
	 * @param		tag			tag associated with the log message
	 * @param		message		log message
	 */
	boolean error (in String tag, in String message);
	

	
}
