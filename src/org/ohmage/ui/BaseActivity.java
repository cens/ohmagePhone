package org.ohmage.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;

import org.ohmage.AccountHelper;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.controls.ActionBarControl;

/**
 * A base activity for all screens. It handles showing the action bar and setting the title correctly as
 * the manifest specifies. This activity also has the {@link #setLoadingVisibility(boolean)} function
 * which makes it easy to show a loading spinner while you wait for data from a loader or somewhere else.
 * 
 * @author Cameron Ketcham
 *
 */
public abstract class BaseActivity extends FragmentActivity {

	private static final String TAG = "BaseActivity";

	private ActionBarControl mActionBar;
	private FrameLayout mContainer;

	private View mActionBarShadow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UserPreferencesHelper preferencesHelper = new UserPreferencesHelper(this);

		if (preferencesHelper.isUserDisabled()) {
			((OhmageApplication) getApplication()).resetAll();
		}

		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(AccountHelper.getLoginIntent(this));
			finish();
			return;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Analytics.widget(this, "Menu Button");
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		getActionBarControl().setTitle(getTitle());
	}

	@Override
	public void setContentView(final int layoutResID) {
		View rootView = initLayout();
		getLayoutInflater().inflate(layoutResID, getContainer(), true);
		super.setContentView(rootView);
	}

	@Override
	public void setContentView(View view) {
		View rootView = initLayout();
		mContainer.addView(view);
		super.setContentView(rootView);
	}

	/**
	 * Sets the content view but doesn't add anything to the container
	 */
	public void setContentView() {
		super.setContentView(initLayout());
	}

	/**
	 * We should automatically add the action bar to the view. This function inflates the base activity layout
	 * which contains the action bar. It also sets the {@link #mActionBar} and {@link #mContainer} fields
	 * @return the root view
	 */
	private View initLayout() {
		LinearLayout baseLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.base_activity, null);
		mActionBar = (ActionBarControl) baseLayout.findViewById(R.id.action_bar);
		mActionBarShadow = baseLayout.findViewById(R.id.action_bar_shadow);
		mContainer = (FrameLayout) baseLayout.findViewById(R.id.root_container);
		return baseLayout;
	}

	@Override
	public View findViewById(int id) {
		// There is no reason for people to be searching through the whole hierarchy
		// since they have access to getActionBarControl()
		return mContainer.findViewById(id);
	}

	public ActionBarControl getActionBarControl() {
		return mActionBar;
	}

	protected FrameLayout getContainer() {
		return mContainer;
	}

	/**
	 * Sets whether the progress spinner overlay is covering the content area (e.g. if the view is still loading).
	 * You're encouraged to call setLoadingVisiblity(true) in your onCreate() and then setLoadingVisibility(false) when
	 * your content is ready.
	 * 
	 * @param isLoading true if the progress spinner overlay should cover the content, false to show the content underneath.
	 */
	protected void setLoadingVisibility(boolean isLoading) {
		View pv = super.findViewById(R.id.info_loading_bar);
		boolean wasVisible = pv.getVisibility() == View.VISIBLE;

		// do the appropriate animation if the view is disappearing
		// do nothing if it's not transitioning or if it's being displayed
		if (wasVisible && !isLoading) {
			pv.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		} else
			pv.clearAnimation();

		// and finally set the actual visibility of the thing
		pv.setVisibility(isLoading?View.VISIBLE:View.GONE);
	}

	protected void setActionBarShadowVisibility(boolean visible) {
		mActionBarShadow.setVisibility(visible?View.VISIBLE:View.GONE);
	}

	/**
	 * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
	 */
	public static Bundle intentToFragmentArguments(Intent intent) {
		Bundle arguments = new Bundle();
		if (intent == null) {
			return arguments;
		}

		final Uri data = intent.getData();
		if (data != null) {
			arguments.putParcelable("_uri", data);
		}

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			arguments.putAll(intent.getExtras());
		}

		return arguments;
	}

	@Override
	public ContentResolver getContentResolver() {
		// The Ohmage Application has code which makes it possible to switch content resolvers during testing.
		// We need to make sure to get the right one here.
		return OhmageApplication.getContext().getContentResolver();
	}
}
