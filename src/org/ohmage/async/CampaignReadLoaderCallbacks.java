package org.ohmage.async;

import org.ohmage.AccountHelper;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.ui.BaseActivity;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;

public class CampaignReadLoaderCallbacks implements LoaderManager.LoaderCallbacks<CampaignReadResponse> {

	private final BaseActivity mActivity;
	private AccountHelper mAccount;
	private UserPreferencesHelper mSharedPreferencesHelper;
	private String mHashedPassword;

	public CampaignReadLoaderCallbacks(BaseActivity activity) {
		mActivity = activity;
	}

	public void onCreate() {
	    mAccount = new AccountHelper(mActivity);
	    mSharedPreferencesHelper = new UserPreferencesHelper(mActivity);
		mActivity.getSupportLoaderManager().initLoader(0, null, this);
	}

	public void onResume(){
		Loader<Object> loader = mActivity.getSupportLoaderManager().getLoader(0);

		if(mHashedPassword != mAccount.getAuthToken() && !((PauseableTaskLoader) loader).isPaused()) {
			forceLoad();
		}
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putString("hashedPassword", mHashedPassword);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		mHashedPassword = savedInstanceState.getString("hashedPassword");
	}

	@Override
	public Loader<CampaignReadResponse> onCreateLoader(int id, Bundle args) {
		// We should pause the task if it is within 5 minutes of the last request
		boolean pause = mSharedPreferencesHelper.getLastCampaignRefreshTime() + DateUtils.MINUTE_IN_MILLIS * 5 > System.currentTimeMillis();
		mActivity.getActionBarControl().setProgressVisible(!pause);
		mHashedPassword = mAccount.getAuthToken();
		CampaignReadTask loader = new CampaignReadTask(mActivity, mAccount.getUsername(), mHashedPassword);
		loader.pause(pause);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<CampaignReadResponse> loader, CampaignReadResponse data) {
		if(data.getResult() == Result.SUCCESS) {
			((PauseableTaskLoader<CampaignReadResponse>) loader).pause(true);
			mSharedPreferencesHelper.setLastCampaignRefreshTime(System.currentTimeMillis());
		} else
			((AuthenticatedTaskLoader<CampaignReadResponse>) loader).clearCredentials();

		mActivity.getActionBarControl().setProgressVisible(false);
	}

	@Override
	public void onLoaderReset(Loader<CampaignReadResponse> loader) {}

	public void forceLoad() {
		Loader<Object> loader = mActivity.getSupportLoaderManager().getLoader(0);
		mActivity.getActionBarControl().setProgressVisible(true);
		mHashedPassword = mAccount.getAuthToken();
		((AuthenticatedTaskLoader)loader).setCredentials(mAccount.getUsername(), mHashedPassword);
		loader.forceLoad();
	}
}
