package edu.ucla.cens.wifigpslocation;

/* Goes with WiFiGPSLocation-v3.1.apk */

oneway interface ILocationChangedCallback {
	/**
	 * Is called when the service detects that location has changed.
	 *
	 */
	void locationChanged ();

}