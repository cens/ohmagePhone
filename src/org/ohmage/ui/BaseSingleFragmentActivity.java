package org.ohmage.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * A base activity which makes it easy to set a single fragment as the content. Just call
 * {@link #setContentFragment(Fragment)}. It will show the action bar.
 * 
 * @author Cameron Ketcham
 *
 */
public abstract class BaseSingleFragmentActivity extends BaseActivity {

	public void setContentFragment(Fragment fragment) {
		setContentView();

		int containerId = getContainer().getId();
		
		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(containerId) == null) {
			fragment.setArguments(intentToFragmentArguments(getIntent()));
			fm.beginTransaction().add(containerId, fragment).commit();
		}
	}
}
