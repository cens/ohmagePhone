package org.ohmage.ui;

import org.ohmage.R;
import org.ohmage.controls.ActionBarControl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A base activity for all screens. It handles the action bar
 * 
 * @author Cameron Ketcham
 *
 */
public abstract class BaseActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(onCreateView(getLayoutInflater(), (ViewGroup) getWindow().getDecorView(), savedInstanceState));
	}

	/**
	 * Returns the fixed content area which should be populated with the details pertaining to this entity.
	 * 
	 * @return a FrameLayout to which other layouts (e.g. a linearlayout) can be added
	 */
	protected FrameLayout getContentArea() {
		return (FrameLayout)findViewById(R.id.root_container);
	}

	protected ActionBarControl getActionBar() {
		return (ActionBarControl)findViewById(R.id.action_bar);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		if(getActionBar() == null)
			throw new RuntimeException("An ActionBarControl must be in the layout with the id R.id.action_bar");

		if(getContentArea() == null)
			throw new RuntimeException("An FrameLayout must be in the layout with the id R.id.root_container");

		// Set the title for this activity
		getActionBar().setTitle(getTitle());
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.base_activity, container, false);
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
