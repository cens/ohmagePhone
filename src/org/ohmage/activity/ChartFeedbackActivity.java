
package org.ohmage.activity;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.fragments.ChartListFragment;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;

public class ChartFeedbackActivity extends BaseActivity {

	private TabHost mTabHost;
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;

	private final String[] mTabs = {
			"All",
			"Diet",
			"Stress",
			"Exercise"
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_layout);

		setActionBarShadowVisibility(false);

		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mViewPager = (ViewPager)findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

		for(int i=0;i<mTabs.length;i++) {
			Bundle b = new Bundle();
			b.putStringArray("prompts", getPrompts(i));
			mTabsAdapter.addTab(mTabs[i],ChartListFragment.class, b);
		}
	}

	private String[] getPrompts(int position) {
		switch(position) {
			case 0:
				return new String[] { NIHConfig.SQL.FOOD_QUALITY_ID, NIHConfig.SQL.FOOD_QUANTITY_ID, NIHConfig.SQL.HOW_STRESSED_ID, NIHConfig.SQL.TIME_TO_YOURSELF_ID, NIHConfig.SQL.DID_EXERCISE_ID };
			case 1:
				return new String[] { NIHConfig.SQL.FOOD_QUALITY_ID, NIHConfig.SQL.FOOD_QUANTITY_ID };
			case 2:
				return new String[] { NIHConfig.SQL.HOW_STRESSED_ID, NIHConfig.SQL.TIME_TO_YOURSELF_ID };
			case 3:
				return new String[] { NIHConfig.SQL.DID_EXERCISE_ID };
			default:
				throw new RuntimeException("Invalid position");
		}
	}
}
