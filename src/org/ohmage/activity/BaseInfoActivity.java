package org.ohmage.activity;

import org.ohmage.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A base activity for entity info screens that includes the entity info header, and provides a view
 * below the header that scrolls independently.
 * 
 * @author faisal
 *
 */
public abstract class BaseInfoActivity extends BaseActivity {
	// fields in the entity info header, populated by onContentChanged()
	protected View mEntityHeader;
	protected TextView mHeadertext;
	protected TextView mSubtext;
	protected TextView mNotetext;
	protected ImageView mIconView;
	protected LinearLayout mButtonTray;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.base_info_activity, container, false);
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
				int currentVis = child.getVisibility();
				child.setVisibility((currentVis == View.INVISIBLE || currentVis == View.GONE)?View.VISIBLE:View.GONE);
			}
		});
	}
	
	/**
	 * Sets whether the progress spinner overlay is covering the content area (e.g. if the view is still loading).
	 * You're encouraged to call setLoadingVisiblity(true) in your onCreate() and then setLoadingVisibility(false) when
	 * your content is ready.
	 * 
	 * @param isLoading true if the progress spinner overlay should cover the content, false to show the content underneath.
	 */
	protected void setLoadingVisibility(boolean isLoading) {
		View pv = (View) findViewById(R.id.info_loading_bar);
		boolean wasVisible = pv.getVisibility() == View.VISIBLE;
		
		// do the appropriate animation if the view is disappearing
		// do nothing if it's not transitioning or if it's being displayed
		if (wasVisible && !isLoading) {
			pv.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		}
		
		// and finally set the actual visibility of the thing
		pv.setVisibility(isLoading?View.VISIBLE:View.GONE);
	}
}
