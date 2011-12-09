
package org.ohmage.activity;

import com.astuetz.viewpagertabs.ViewPagerTabProvider;
import com.astuetz.viewpagertabs.ViewPagerTabs;

import org.ohmage.R;
import org.ohmage.ui.BaseActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChartFeedbackActivity extends BaseActivity {

	private ViewPager mPager;
	private ViewPagerTabs mTabs;
	private ExampleAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chart_feedback_layout);

		mAdapter = new ExampleAdapter(this);

		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		mTabs = (ViewPagerTabs) findViewById(R.id.tabs);
		mTabs.setViewPager(mPager);
	}

	public static class ExampleAdapter extends PagerAdapter implements ViewPagerTabProvider {

		protected transient Activity mContext;

		private final String[] mData = {
				"zero", "one", "two", "three"
		};

		private final String[] mTitles = {
				"Recent",
				"Diet",
				"Stress",
				"Exercise"
		};

		public ExampleAdapter(Activity context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mData.length;
		}

		@Override
		public Object instantiateItem(View container, int position) {

			RelativeLayout v = new RelativeLayout(mContext);

			TextView t = new TextView(mContext);
			t.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			t.setText(mData[position]);
			t.setTextSize(30);
			t.setGravity(Gravity.CENTER);

			v.addView(t);

			((ViewPager) container).addView(v, 0);

			return v;
		}

		@Override
		public void destroyItem(View container, int position, Object view) {
			((ViewPager) container).removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((View) object);
		}

		@Override
		public void finishUpdate(View container) {
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View container) {
		}

		@Override
		public String getTitle(int position) {
			if (position >= 0 && position < mTitles.length)
				return mTitles[position].toUpperCase();
			else
				return "";
		}
	}
}
