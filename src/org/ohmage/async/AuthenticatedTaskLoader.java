package org.ohmage.async;

import org.ohmage.AccountHelper;

import android.content.Context;

/**
 * A custom Loader that uses a username and password
 */
public abstract class AuthenticatedTaskLoader<T> extends PauseableTaskLoader<T> {
	private AccountHelper mAccountHelper;
	private static final String TAG = "AuthenticatedTask";

	public AuthenticatedTaskLoader(Context context) {
		super(context);
	}

	public AuthenticatedTaskLoader(Context context, AccountHelper accountHelper) {
		super(context);
		mAccountHelper = accountHelper;
		pause(!hasAuthentication());
	}

	public void setCredentials(AccountHelper accountHelper) {
		mAccountHelper = accountHelper;
		pause(!hasAuthentication());
	}

	public String getUsername() {
		return mAccountHelper.getUsername();
	}

	public String getHashedPassword() {
		return mAccountHelper.getAuthToken();
	}
	
	protected boolean hasAuthentication() {
		return mAccountHelper != null && mAccountHelper.getUsername() != null;
	}

	public void clearCredentials() {
		mAccountHelper = null;
		pause(true);
	}

	public AccountHelper getAccountHelper() {
		return mAccountHelper;
	}
}