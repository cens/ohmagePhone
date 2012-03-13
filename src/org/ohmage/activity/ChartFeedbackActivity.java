
package org.ohmage.activity;

import com.astuetz.viewpagertabs.ViewPagerTabProvider;
import com.astuetz.viewpagertabs.ViewPagerTabs;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.fragments.ChartListFragment;
import org.ohmage.ui.BaseActivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class ChartFeedbackActivity extends BaseActivity {

	private ViewPager mPager;
	private ViewPagerTabs mTabs;
	private ChartFeedbackAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chart_feedback_layout);

		mAdapter = new ChartFeedbackAdapter(getSupportFragmentManager());

		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		mTabs = (ViewPagerTabs) findViewById(R.id.tabs);
		mTabs.setViewPager(mPager);
	}

	public static class ChartFeedbackAdapter extends FragmentPagerAdapter implements ViewPagerTabProvider {

		public final int RECENT = 0;
		public final int DIET = 1;
		public final int STRESS = 2;
		public final int EXERCISE = 3;
		private final int mLength = 4;

		private final String[] mTitles = {
				"All",
				"Diet",
				"Stress",
				"Exercise"
		};

		public ChartFeedbackAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public int getCount() {
			return mLength;
		}

		@Override
		public String getTitle(int position) {
			if (position >= 0 && position < mTitles.length)
				return mTitles[position].toUpperCase();
			else
				return "";
		}

		@Override
		public Fragment getItem(int position) {
			return ChartListFragment.newInstance(getPrompts(position));
		}

		private String[] getPrompts(int position) {
			switch(position) {
				case 0:
					return new String[] { NIHConfig.Prompt.FOOD_QUALITY_ID, NIHConfig.Prompt.FOOD_QUANTITY_ID, NIHConfig.Prompt.HOW_STRESSED_ID, NIHConfig.Prompt.TIME_TO_YOURSELF_ID, NIHConfig.Prompt.DID_EXERCISE_ID };
				case 1:
					return new String[] { NIHConfig.Prompt.FOOD_QUALITY_ID, NIHConfig.Prompt.FOOD_QUANTITY_ID };
				case 2:
					return new String[] { NIHConfig.Prompt.HOW_STRESSED_ID, NIHConfig.Prompt.TIME_TO_YOURSELF_ID };
				case 3:
					return new String[] { NIHConfig.Prompt.DID_EXERCISE_ID };
				default:
					throw new RuntimeException("Invalid position");
			}
		}
	}
}
