package org.ohmage.activity;


import org.ohmage.MobilityHelper;
import org.ohmage.R;
import org.ohmage.fragments.MobilityControlFragment;
import org.ohmage.fragments.RecentMobilityChartFragment;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.widget.TabHost;
import android.widget.TextView;


public class MobilityActivity extends BaseActivity {

	private static final String TAG = "MobilityActivity";

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!MobilityHelper.isMobilityInstalled(this)) {
			TextView view = new TextView(this);
			view.setText("Please make sure the Mobility and AccelService packages are installed.");
			view.setTextAppearance(this, android.R.attr.textAppearanceLarge);
			view.setGravity(Gravity.CENTER);
			setContentView(view);
		} else if (!MobilityHelper.isMobilityVersionCorrect(this)) {
			TextView view = new TextView(this);
			view.setText("Please make sure Mobility is up to date.");
			view.setTextAppearance(this, android.R.attr.textAppearanceLarge);
			view.setGravity(Gravity.CENTER);
			setContentView(view);
		} else {
			setContentView(R.layout.tab_layout);
			setActionBarShadowVisibility(false);

			mTabHost = (TabHost) findViewById(android.R.id.tabhost);
			mTabHost.setup();

			mViewPager = (ViewPager) findViewById(R.id.pager);

			mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

			mTabsAdapter.addTab("Analytics", RecentMobilityChartFragment.class, null);
			mTabsAdapter.addTab("Control", MobilityControlFragment.class, null);

			if (savedInstanceState != null) {
				mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mTabHost != null)
			outState.putString("tab", mTabHost.getCurrentTabTag());
	}
}