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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.CampaignXmlHelper;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.db.DbContract;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Prompt;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity is used to display Information for an individual response. It
 * is called with {@link Intent#ACTION_VIEW} on the URI specified by
 * {@link DbContract.Response#getResponseUri(long)}
 * 
 * @author cketcham
 * 
 */
public class ResponseInfoActivity extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private TextView mSurveyName;
	private TextView mCampaignName;
	private TextView mCompletedDate;
	private View mEntityHeader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.response_info_layout);

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(R.id.root_container) == null) {

			ResponseInfoListFragment list = new ResponseInfoListFragment();

			fm.beginTransaction().add(R.id.root_container, list, "list")
					.commit();
		}
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mEntityHeader = findViewById(R.id.entity_header_content);
		mEntityHeader.setVisibility(View.GONE);
		mSurveyName = (TextView) findViewById(R.id.entity_header);
		mCampaignName = (TextView) findViewById(R.id.entity_header_sub1);
		mCompletedDate = (TextView) findViewById(R.id.entity_header_sub2);
	}

	private interface ResponseQuery {
		String[] PROJECTION = { DbContract.Response.CAMPAIGN_URN,
				DbContract.Response.RESPONSE, DbContract.Response.SURVEY_ID,
				DbContract.Response.TIME };

		int CAMPAIGN_URN = 0;
		int RESPONSE = 1;
		int SURVEY_ID = 2;
		int TIME = 3;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(this, getIntent().getData(),
				ResponseQuery.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (!data.moveToFirst()) {
			return;
		}

		final String surveyName = data.getString(ResponseQuery.SURVEY_ID);
		final String completedDate = data.getString(ResponseQuery.TIME);

		try {
			Map<String, String> campaignInfo = PromptXmlParser
					.parseCampaignInfo(CampaignXmlHelper.loadCampaignXmlFromDb(
							this, data.getString(ResponseQuery.CAMPAIGN_URN)));

			mSurveyName.setText(surveyName);
			mCampaignName.setText(campaignInfo.get("campaign_name"));
			SimpleDateFormat df = new SimpleDateFormat();
			mCompletedDate.setText(df.format(new Date((Long
					.valueOf(completedDate)))));

			mEntityHeader.setVisibility(View.VISIBLE);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FragmentManager fm = getSupportFragmentManager();
		ResponseInfoListFragment list = (ResponseInfoListFragment) fm
				.findFragmentByTag("list");
		if (list != null)
			list.onLoadFinished(loader, data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mEntityHeader.setVisibility(View.GONE);

		FragmentManager fm = getSupportFragmentManager();
		ResponseInfoListFragment list = (ResponseInfoListFragment) fm
				.findFragmentByTag("list");
		if (list != null)
			list.onLoaderReset(loader);
	}

	public static class ResponseInfoListFragment extends ListFragment implements
			LoaderManager.LoaderCallbacks<Cursor> {

		private static final String TAG = "ResponseInfoListFragment";

		// This is the Adapter being used to display the list's data.
		JSONArrayAdapter mAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			Log.d("lm", "LoaderManager=" + getLoaderManager());

			// We have no menu items to show in action bar.
			setHasOptionsMenu(false);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new JSONArrayAdapter(getActivity(),
					android.R.layout.simple_list_item_2, new String[] {
							"label", "display_value" }, new int[] {
							android.R.id.text1, android.R.id.text2 });
			setListAdapter(mAdapter);

			// Start out with a progress indicator.
			setListShown(false);
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			// Insert desired behavior here.
			Log.i("FragmentComplexList", "Item clicked: " + id);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (!data.moveToFirst()) {
				return;
			}

			List<Prompt> prompts = null;
			try {
				prompts = PromptXmlParser.parsePrompts(CampaignXmlHelper
						.loadCampaignXmlFromDb(getActivity()
								.getApplicationContext(), data
								.getString(ResponseQuery.CAMPAIGN_URN)), data
						.getString(ResponseQuery.SURVEY_ID));
			} catch (NotFoundException e) {
				Log.e(TAG, "Error parsing prompts from xml", e);
			} catch (XmlPullParserException e) {
				Log.e(TAG, "Error parsing prompts from xml", e);
			} catch (IOException e) {
				Log.e(TAG, "Error parsing prompts from xml", e);
			}

			// Swap the new data in
			try {
				final JSONArray jsonData = new JSONArray(
						data.getString(ResponseQuery.RESPONSE));
				for (int i = 0; i < jsonData.length(); i++) {
					JSONObject jsonObject = jsonData.getJSONObject(i);
					String prompt_id = jsonObject.getString("prompt_id");
					for (Prompt prompt : prompts) {
						if (((AbstractPrompt) prompt).getId().equals(prompt_id)) {
							jsonObject
									.put("label", ((AbstractPrompt) prompt)
											.getDisplayLabel());
							jsonObject.put("display_value", AbstractPrompt
									.getDisplayValue((AbstractPrompt) prompt,
											jsonObject.getString("value")));
							break;
						}
					}
				}
				mAdapter.setData(jsonData);

				// The list should now be shown.
				if (isResumed()) {
					setListShown(true);
				} else {
					setListShownNoAnimation(true);
				}
			} catch (JSONException e) {
				// Unable to parse data
				Log.e(TAG, "Unable to parse response json");
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.setData(null);
		}

		/**
		 * JSONArrayAdapter that renders the response JSON for a
		 * {@link ResponseQuery}.
		 */
		private class JSONArrayAdapter extends BaseAdapter {
			private JSONArray mData;
			private final Context mContext;
			private final int mLayout;
			private final String[] mFrom;
			private final int[] mTo;

			public JSONArrayAdapter(Context context, int layout, String[] from,
					int[] to) {
				super();
				mContext = context;
				mLayout = layout;
				mFrom = from;
				mTo = to;
			}

			@Override
			public int getCount() {
				return (mData == null) ? 0 : mData.length();
			}

			@Override
			public JSONObject getItem(int position) {
				try {
					return (mData == null) ? null : mData
							.getJSONObject(position);
				} catch (JSONException e) {
					return null;
				}
			}

			@Override
			public long getItemId(int position) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView[] views;
				if (convertView == null) {
					LayoutInflater layoutInflater = (LayoutInflater) mContext
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = layoutInflater.inflate(mLayout, null);
					views = new TextView[mTo.length];
					for (int i = 0; i < mTo.length; i++) {
						views[i] = (TextView) convertView.findViewById(mTo[i]);
					}
					convertView.setTag(views);
				} else {
					views = (TextView[]) convertView.getTag();
				}

				JSONObject item = getItem(position);
				Log.d(TAG, "item=" + item.toString());

				for (int i = 0; i < views.length; i++) {
					try {
						views[i].setText(item.getString(mFrom[i]));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				return convertView;
			}

			public void setData(JSONArray data) {
				mData = data;
				this.notifyDataSetInvalidated();
			}
		}
	}
}