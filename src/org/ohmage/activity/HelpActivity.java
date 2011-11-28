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
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
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

import java.io.IOException;

/**
 * Help activity uses a view pager to show different help screens
 * @author cketcham
 *
 */
public class HelpActivity extends BaseActivity {
	static final String URLS[] =  {
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

		mTabsAdapter.addTab("Dashboard",DashboardWebViewFragment.class, null);
		mTabsAdapter.addTab("Filter",WebViewFragment.class, WebViewFragment.instanceBundle(URLS[0]));
		mTabsAdapter.addTab("List",WebViewFragment.class, WebViewFragment.instanceBundle(URLS[1]));

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

	/**
	 * Each item on the dashboard has an associated entry in the help which will only be shown if that
	 * item is visible on the dashboard
	 * @author cketcham
	 *
	 */
	public static class DashboardWebViewFragment extends DataWebViewFragment {

		@Override
		protected StringBuilder createData() {
			try {
				return new StringBuilder(Utilities.convertStreamToString(getActivity().getResources().getAssets().open("about_sectioned.html")));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void loadData(StringBuilder data) {
			data.insert(data.length() - 8, getString(R.string.help_dashboard_header_text));

			if(!SharedPreferencesHelper.IS_SINGLE_CAMPAIGN)
				addSection(data, "dash_campaigns.png", R.string.help_dashboard_campaigns_title, R.string.help_dashboard_campaigns_text);

			UserPreferencesHelper userPrefs = new UserPreferencesHelper(getActivity());

			addSection(data, "dash_surveys.png", R.string.help_dashboard_surveys_title, R.string.help_dashboard_surveys_text);

			if(userPrefs.showFeedback())
				addSection(data, "dash_resphistory.png", R.string.help_dashboard_response_history_title, R.string.help_dashboard_response_history_text);

			if(userPrefs.showUploadQueue())
				addSection(data, "dash_upqueue.png", R.string.help_dashboard_upload_queue_title, R.string.help_dashboard_upload_queue_text);

			if(userPrefs.showProfile())
				addSection(data, "dash_profile.png", R.string.help_dashboard_profile_title, R.string.help_dashboard_profile_text);

			if(userPrefs.showMobility())
				addSection(data, "dash_mobility.png", R.string.help_dashboard_mobility_title, R.string.help_dashboard_mobility_text);
		}

		private void addSection(StringBuilder builder, String icon, int title, int text) {
			builder.insert(builder.length() - 8, getString(R.string.help_dashboard_section, "file:///android_res/drawable/" + icon, getString(title), getString(text)));
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

	public abstract static class DataWebViewFragment extends Fragment {

		/**
		 * The Fragment's UI is a webview with optional extra data
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			WebView webView = new WebView(getActivity());
			StringBuilder data = createData();
			if(data == null)
				throw new RuntimeException("No initial data was supplied to the DataWebFragment");

			loadData(data);
			webView.loadDataWithBaseURL("/", data.toString(), "text/html", null, null);
			webView.getSettings().setSupportZoom(false);
			return webView;
		}

		/**
		 * The content can come only from the url, or we can create some sort of datastream here
		 * @return
		 */
		protected StringBuilder createData() {
			return null;
		}

		/**
		 * The data stream for this webview can be manipulated here
		 * @param data
		 */
		protected void loadData(StringBuilder data) {}
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }
}