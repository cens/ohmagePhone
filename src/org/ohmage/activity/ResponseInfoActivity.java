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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.service.SurveyGeotagService;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.imageloader.ImageLoader;

/**
 * This Activity is used to display Information for an individual response. It
 * is called with {@link Intent#ACTION_VIEW} on the URI specified by
 * {@link DbContract.Responses#getResponseUri(long)}
 *
 * @author cketcham
 *
 */
public class ResponseInfoActivity extends BaseInfoActivity implements
LoaderManager.LoaderCallbacks<Cursor> {
	
	private ImageLoader mImageLoader;
	private TextView mapViewButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mImageLoader = ImageLoader.get(this);
		
		// inflate the campaign-specific info page into the scrolling framelayout
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		// and inflate all the possible commands into the button tray
		inflater.inflate(R.layout.response_info_buttons, mButtonTray, true);

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(R.id.root_container) == null) {

			ResponsePromptsFragment list = new ResponsePromptsFragment();

			fm.beginTransaction().add(R.id.root_container, list, "list").commit();
		}
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
		
		mapViewButton = (TextView) findViewById(R.id.response_info_button_view_map);
		mapViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(OhmageApplication.VIEW_MAP, getIntent().getData()));
			}
		});
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mEntityHeader.setVisibility(View.GONE);
	}

	private interface ResponseQuery {
		String[] PROJECTION = { Campaigns.CAMPAIGN_NAME,
				Surveys.SURVEY_TITLE,
				Responses.RESPONSE_TIME,
				Campaigns.CAMPAIGN_ICON,
				Responses.RESPONSE_LOCATION_STATUS,
				Responses.RESPONSE_STATUS};

		int CAMPAIGN_NAME = 0;
		int SURVEY_TITLE = 1;
		int TIME = 2;
		int CAMPAIGN_ICON = 3;
		int LOCATION_STATUS = 4;
		int STATUS = 5;
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

		final String surveyName = data.getString(ResponseQuery.SURVEY_TITLE);
		final Long completedDate = data.getLong(ResponseQuery.TIME);

		mHeadertext.setText(surveyName);
		mSubtext.setText(data.getString(ResponseQuery.CAMPAIGN_NAME));
		SimpleDateFormat df = new SimpleDateFormat();
		mNotetext.setText(df.format(new Date(completedDate)));
		
		final String iconUrl = data.getString(ResponseQuery.CAMPAIGN_ICON);
		if(iconUrl == null || mImageLoader.bind(mIconView, iconUrl, null) != ImageLoader.BindResult.OK) {
			mIconView.setImageResource(R.drawable.apple_logo);
		}

		mEntityHeader.setVisibility(View.VISIBLE);
		
		// Make the map view button status aware so it can provide some useful info about the gps state
		if(data.getInt(ResponseQuery.STATUS) == Response.STATUS_WAITING_FOR_LOCATION) {
			mapViewButton.setText("Waiting for Location");
			mapViewButton.setEnabled(false);
		} else if(!(SurveyGeotagService.LOCATION_VALID.equals(data.getString(ResponseQuery.LOCATION_STATUS)))) {
			mapViewButton.setText("Location Not Available");
			mapViewButton.setEnabled(false);
		} else {
			mapViewButton.setText("View Map");
			mapViewButton.setEnabled(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mEntityHeader.setVisibility(View.GONE);
	}

	public static class ResponsePromptsFragment extends ListFragment implements
	LoaderManager.LoaderCallbacks<Cursor> {

		private static final String TAG = "ResponseInfoListFragment";

		// This is the Adapter being used to display the list's data.
		PromptResponsesAdapter mAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// We have no menu items to show in action bar.
			setHasOptionsMenu(false);

			getListView().setDivider(null);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new PromptResponsesAdapter(getActivity(), null,  new String[] {
				SurveyPrompts.SURVEY_PROMPT_TEXT, PromptResponses.PROMPT_RESPONSE_VALUE }, new int[] {
				android.R.id.text1, R.id.prompt_value }, 0);

			setListAdapter(mAdapter);

			// Start out with a progress indicator.
			setListShown(false);

			getLoaderManager().initLoader(0, null, this);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
			return new CursorLoader(getActivity(),
					Responses.buildPromptResponsesUri(ContentUris.parseId(getActivity().getIntent().getData())),
					null, PromptResponses.PROMPT_RESPONSE_VALUE + " !=?", new String[] { "NOT_DISPLAYED" }, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);

			// The list should now be shown.
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}

		private static class PromptResponsesAdapter extends SimpleCursorAdapter implements ViewBinder {

			public static final int TEXT_RESPONSE = 0;
			public static final int IMAGE_RESPONSE = 1;

			public PromptResponsesAdapter(Context context, Cursor c, String[] from,
					int[] to, int flags) {
				super(context, R.layout.response_prompt_list_item, c, from, to, flags);
				setViewBinder(this);
			}

			@Override
			public int getItemViewType(int position) {
				if(getCursor().moveToPosition(position))
					return getItemViewType(getCursor());
				return 0;
			}

			/**
			 * Gets the view type for this item by the cursor. The cursor needs to be moved to the correct position prior to calling
			 * this function
			 * @param cursor
			 * @return the view type
			 */
			public int getItemViewType(Cursor cursor) {
				if("photo".equals(getItemPromptType(cursor)))
					return IMAGE_RESPONSE;
				else
					return TEXT_RESPONSE;
			}

			/**
			 * Gets the prompt type for this item by the cursor. The cursor needs to be moved to the correct position prior to calling
			 * this function
			 * @param cursor
			 * @return
			 */
			public String getItemPromptType(Cursor cursor) {
				return cursor.getString(cursor.getColumnIndex(SurveyPrompts.SURVEY_PROMPT_TYPE));
			}

			@Override
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
				View view = super.newView(context, cursor, parent);
				View image = view.findViewById(R.id.prompt_image_value);
				View text = view.findViewById(R.id.prompt_text_value);
				View value = view.findViewById(R.id.prompt_value);
				switch(getItemViewType(cursor)) {
					case IMAGE_RESPONSE:
						image.setVisibility(View.VISIBLE);
						text.setVisibility(View.GONE);
						value.setTag(image);
						value.setBackgroundResource(R.drawable.prompt_response_image_item_bg);
						break;
					case TEXT_RESPONSE:
						image.setVisibility(View.GONE);
						text.setVisibility(View.VISIBLE);
						value.setTag(text);
						break;
				}
				return view;
			}

			@Override
			public int getViewTypeCount() {
				// One view type for text, another for images
				return 2;
			}

			@Override
			public boolean areAllItemsEnabled() {
				return false;
			}

			@Override
			public boolean isEnabled(int position) {
				return false;
			}

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

				if(cursor.getColumnName(columnIndex).equals(PromptResponses.PROMPT_RESPONSE_VALUE)) {
					final String value = cursor.getString(columnIndex);

					if(view.getTag() instanceof ImageView) {
						String campaignUrn = cursor.getString(cursor.getColumnIndex(Responses.CAMPAIGN_URN));
						// We don't know if the file is .jpg or .png
						// This list will match the first part of the filename which should be unique
						File [] files = Campaign.getCampaignImageDir(mContext, campaignUrn).listFiles(new FilenameFilter() {
							
							@Override
							public boolean accept(File dir, String filename) {
								return filename.startsWith(value);
							}
						});

						if(files.length > 0 && files[0].exists()) {
							try {
								Bitmap img = BitmapFactory.decodeStream(new FileInputStream(files[0]));
								((ImageView) view.getTag()).setImageBitmap(img);
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					} else if(view.getTag() instanceof TextView) {
						String prompt_type = getItemPromptType(cursor);
						if("multi_choice_custom".equals(prompt_type)) {
							try {
								JSONArray choices = new JSONArray(value);
								StringBuilder builder = new StringBuilder();
								for(int i=0;i<choices.length();i++) {
									if(i != 0)
										builder.append("\n");
									builder.append("¥ ");
									builder.append(choices.get(i));
								}
								((TextView) view.getTag()).setText(builder.toString());
								return true;
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else if("timestamp".equals(prompt_type)) {
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							try {
								long time = format.parse(value).getTime();
								StringBuilder timeDisplay =  new StringBuilder(DateUtils.formatDateTime(mContext, time, DateUtils.FORMAT_SHOW_YEAR));
								timeDisplay.append(" at ");
								timeDisplay.append(DateUtils.formatDateTime(mContext, time, DateUtils.FORMAT_SHOW_TIME));

								((TextView) view.getTag()).setText(timeDisplay);
								return true;
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						((TextView) view.getTag()).setText(value);
					}
					return true;
				}

				return false;
			}
		}
	}
}