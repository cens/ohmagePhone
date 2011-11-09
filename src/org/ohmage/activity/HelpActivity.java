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
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TabHost;

/**
 * Help activity uses a view pager to show different help screens
 * @author cketcham
 *
 */
public class HelpActivity extends BaseActivity {
	static final String URLS[] =  {
		"file:///android_asset/about_dashboard.html",
		"file:///android_asset/about_filter.html",
		"file:///android_asset/about_lists.html"
	};

    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help_layout);
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager)findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab("Dashboard",WebViewFragment.class, WebViewFragment.instanceBundle(URLS[0]));
        mTabsAdapter.addTab("Filter",WebViewFragment.class, WebViewFragment.instanceBundle(URLS[1]));
        mTabsAdapter.addTab("List",WebViewFragment.class, WebViewFragment.instanceBundle(URLS[2]));

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

	public static class WebViewFragment extends Fragment {

		String mUrl;
		
		/**
		 * Create a new instance of WebViewFragment, providing the url as an argument
		 */
		static WebViewFragment newInstance(String url) {
			WebViewFragment f = new WebViewFragment();
			f.setArguments(instanceBundle(url));
			return f;
		}

		/**
		 * Create the bundle for a new instance of WebViewFragment
		 * @param url
		 * @return the bundle which will show this url
		 */
		static Bundle instanceBundle(String url) {
			Bundle args = new Bundle();
			args.putString("url", url);
			return args;
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
			webView.getSettings().setSupportZoom(false);
			return webView;
		}
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }
}