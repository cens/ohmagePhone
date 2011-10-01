/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.activity;

import org.ohmage.R;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

/**
 * Help activity uses a view pager to show different help screens
 * @author cketcham
 *
 */
public class HelpActivity extends FragmentActivity {
	static final int NUM_ITEMS = 3;
	static final String URLS[] =  {
		"file:///android_asset/about_dashboard.html",
		"file:///android_asset/about_filter.html",
		"file:///android_asset/about_lists.html"
	};

	MyAdapter mAdapter;

	ViewPager mPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_layout);

		mAdapter = new MyAdapter(getSupportFragmentManager());

		mPager = (ViewPager)findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		// Watch for button clicks.
		// this generic handler can be used for each of the buttons
		OnClickListener pagerListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.goto_dashboard:
						mPager.setCurrentItem(0); break;
					case R.id.goto_filter:
						mPager.setCurrentItem(1); break;
					case R.id.goto_list:
						mPager.setCurrentItem(2); break;
				}
			}
		};
		
		// gather references to our tab buttons
		final Button dashButton = (Button)findViewById(R.id.goto_dashboard);
		final Button filterButton = (Button)findViewById(R.id.goto_filter);
		final Button listButton = (Button)findViewById(R.id.goto_list);
		
		// set all the buttons to use the generic pager listener
		dashButton.setOnClickListener(pagerListener);
		filterButton.setOnClickListener(pagerListener);
		listButton.setOnClickListener(pagerListener);
		
		// update the button selection based on the current page
		mPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// set the current item to be selected and all others to be deselected
				dashButton.setBackgroundResource((position == 0)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
				filterButton.setBackgroundResource((position == 1)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
				listButton.setBackgroundResource((position == 2)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
			}
			
			// the below two aren't used, but they're abstract so we have to define them
			@Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
			@Override public void onPageScrollStateChanged(int state) { }
		});
		
		// set the buttons to their default states on load
		int position = 0;
		dashButton.setBackgroundResource((position == 0)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
		filterButton.setBackgroundResource((position == 1)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
		listButton.setBackgroundResource((position == 2)?R.drawable.tab_bg_selected:R.drawable.tab_bg_unselected);
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		public MyAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}

		@Override
		public Fragment getItem(int position) {
			return WebViewFragment.newInstance(URLS[position]);
		}
	}

	public static class WebViewFragment extends Fragment {

		String mUrl;
		
		/**
		 * Create a new instance of WebViewFragment, providing the url as an argument
		 */
		static WebViewFragment newInstance(String url) {
			WebViewFragment f = new WebViewFragment();

			Bundle args = new Bundle();
			args.putString("url", url);
			f.setArguments(args);

			return f;
		}

		/**
		 * When creating, retrieve this instance's number from its arguments.
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			if(getArguments() == null)
				throw new RuntimeException("Url must be specified for this fragment");
			mUrl = getArguments().getString("url");
		}

		/**
		 * The Fragment's UI is just a simple webview
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			WebView webView = new WebView(getActivity());
			webView.loadUrl(mUrl);
			return webView;
		}
	}
}
