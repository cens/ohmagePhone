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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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
		Button button = (Button)findViewById(R.id.goto_dashboard);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPager.setCurrentItem(0);
			}
		});
		button = (Button)findViewById(R.id.goto_filter);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPager.setCurrentItem(1);
			}
		});
		button = (Button)findViewById(R.id.goto_list);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPager.setCurrentItem(2);
			}
		});
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
