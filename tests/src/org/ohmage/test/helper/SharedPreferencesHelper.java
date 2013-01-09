package org.ohmage.test.helper;

import org.ohmage.authenticator.AuthenticatorActivity;

public class SharedPreferencesHelper {

	/**
	 * This field can be set to force the {@link AuthenticatorActivity} to login with certain credentials. If the
	 * USERNAME is null, we will look at the saved login information.
	 */
	public static final String USERNAME = null;
	/**
	 * This field must be set to allow the {@link AuthenticatorActivity} to log in. This password can be the password
	 * of the current user or the password for the user with the given {@link USERNAME}
	 */
	public static final String PASSWORD = "Cameron.00";
}
