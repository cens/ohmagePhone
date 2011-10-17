package org.ohmage.ui;

import org.ohmage.R;
import org.ohmage.controls.ActionBarControl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * A base activity for all screens. It handles showing the action bar and setting the title correctly as
 * the manifest specifies.
 * 
 * @author Cameron Ketcham
 *
 */
public abstract class BaseActivity extends FragmentActivity {

	private ActionBarControl mActionBar;
	private FrameLayout mContainer;

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		getActionBar().setTitle(getTitle());
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
		mContainer = (FrameLayout) baseLayout.findViewById(R.id.root_container);
		return baseLayout;
	}

	@Override
	public View findViewById(int id) {
		// There is no reason for people to be searching through the whole hierarchy
		// since they have access to getActionBar()
		return mContainer.findViewById(id);
	}

	protected ActionBarControl getActionBar() {
		return mActionBar;
	}
	
	protected FrameLayout getContainer() {
		return mContainer;
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
}
