package org.ohmage.async;

import android.content.Context;

/**
 * A custom Loader that uses a username and password
 */
public abstract class AuthenticatedTaskLoader<T> extends PauseableTaskLoader<T> {
	private String mUsername;
	private String mHashedPassword;

	private static final String TAG = "AuthenticatedTask";

	public AuthenticatedTaskLoader(Context context) {
		super(context);
	}

	public AuthenticatedTaskLoader(Context context, String username, String hashedPassword) {
		super(context);
		mUsername = username;
		mHashedPassword = hashedPassword;
		pause(!hasAuthentication());
	}

	public void setCredentials(String username, String hashedPassword) {
		mUsername = username;
		mHashedPassword = hashedPassword;
		pause(!hasAuthentication());
	}

	public String getUsername() {
		return mUsername;
	}

	public String getHashedPassword() {
		return mHashedPassword;
	}
	
	protected boolean hasAuthentication() {
		return mUsername != null && mHashedPassword != null;
	}

	public void clearCredentials() {
		mUsername = null;
		mHashedPassword = null;
		pause(true);
	}
}