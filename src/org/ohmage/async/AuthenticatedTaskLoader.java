package org.ohmage.async;

import android.content.Context;

import org.ohmage.OhmageApi.Response;
import org.ohmage.UserPreferencesHelper;

/**
 * A custom Loader that uses a username and password
 */
public abstract class AuthenticatedTaskLoader<T extends Response> extends PauseableTaskLoader<T> {
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

	public void setCredentials() {
		UserPreferencesHelper sharedPrefs = new UserPreferencesHelper(getContext());
		setCredentials(sharedPrefs.getUsername(), sharedPrefs.getHashedPassword());
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

	@Override
	public void deliverResult(T response) {
		response.handleError(getContext());
		super.deliverResult(response);
	}
}