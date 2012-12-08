package org.ohmage.ui;

import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ohmage.R;
import org.ohmage.logprobe.Analytics;

/**
 * A base activity for entity info screens that includes the entity info header, and provides a view
 * below the header that scrolls independently.
 * 
 * @author faisal
 *
 */
public abstract class BaseInfoActivity extends BaseSingleFragmentActivity {
	// fields in the entity info header, populated by onContentChanged()
	protected View mEntityHeader;
	protected TextView mHeadertext;
	protected TextView mSubtext;
	protected TextView mNotetext;
	protected ImageView mIconView;
	protected LinearLayout mButtonTray;
	private FrameLayout mContainer;

	@Override
	public void setContentView(final int layoutResID) {
		View rootView = initLayout();
		getLayoutInflater().inflate(layoutResID, mContainer, true);
		super.setContentView(rootView);
	}

	@Override
	public void setContentView(View view) {
		View rootView = initLayout();
		mContainer.addView(view);
		super.setContentView(rootView);
	}

	@Override
	public void setContentView() {
		super.setContentView(initLayout());
	}

	/**
	 * We should automatically add the info header view
	 * @return the root view
	 */
	private View initLayout() {
		LinearLayout baseLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.base_info_activity, null);
		mContainer = (FrameLayout) baseLayout.findViewById(R.id.info_container);
		return baseLayout;
	}

	@Override
	protected FrameLayout getContainer() {
		return mContainer;
	}

	public Fragment getFragment() {
		return getSupportFragmentManager().findFragmentById(R.id.info_container);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mEntityHeader = findViewById(R.id.entity_header_content);
		// mEntityHeader.setVisibility(View.GONE);
		mIconView = (ImageView) findViewById(R.id.entity_icon);
		mHeadertext = (TextView) findViewById(R.id.entity_header);
		mSubtext = (TextView) findViewById(R.id.entity_header_sub1);
		mNotetext = (TextView) findViewById(R.id.entity_header_sub2);
		mButtonTray = (LinearLayout) findViewById(R.id.entity_header_tray);
	}
	
	// utility functions and classes to implement togglable views
	
	/**
	 * Attaches a handler to the parent view's onclick event that causes the
	 * child view to toggle its appearance whenever the parent is clicked.
	 * 
	 * @param parent the view which will be clicked to toggle the child
	 * @param child the view which will toggle when the parent is clicked
	 */
	protected void setDetailsExpansionHandler(View parent, final View child) {
		parent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Analytics.widget(v);
				int currentVis = child.getVisibility();
				child.setVisibility((currentVis == View.INVISIBLE || currentVis == View.GONE)?View.VISIBLE:View.GONE);
			}
		});
	}
}
