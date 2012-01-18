package org.ohmage.async;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * A custom Loader that uses a username and password
 */
public abstract class AuthenticatedTaskLoader<T> extends AsyncTaskLoader<T> {
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
	}

	@Override
    protected void onStartLoading() {
		if(hasAuthentication())
		forceLoad();
	}

	public void setCredentials(String username, String hashedPassword) {
		mUsername = username;
		mHashedPassword = hashedPassword;
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
}