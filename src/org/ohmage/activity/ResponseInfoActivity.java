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
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.db.DbContract.SurveyPrompt;
import org.ohmage.prompt.photo.PhotoPrompt;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This Activity is used to display Information for an individual response. It
 * is called with {@link Intent#ACTION_VIEW} on the URI specified by
 * {@link DbContract.Response#getResponseUri(long)}
 *
 * @author cketcham
 *
 */
public class ResponseInfoActivity extends BaseInfoActivity implements
LoaderManager.LoaderCallbacks<Cursor> {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setTitle("Response");

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(R.id.root_container) == null) {

			ResponsePromptsFragment list = new ResponsePromptsFragment();

			fm.beginTransaction().add(R.id.root_container, list, "list").commit();
		}
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mEntityHeader.setVisibility(View.GONE);
	}

	private interface ResponseQuery {
		String[] PROJECTION = { Campaign.NAME,
				Survey.TITLE,
				Response.TIME };

		int CAMPAIGN_NAME = 0;
		int SURVEY_TITLE = 1;
		int TIME = 2;
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

		mEntityHeader.setVisibility(View.VISIBLE);
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
				SurveyPrompt.PROMPT_TEXT, PromptResponse.PROMPT_VALUE }, new int[] {
				android.R.id.text1, R.id.prompt_value }, 0);

			setListAdapter(mAdapter);

			// Start out with a progress indicator.
			setListShown(false);

			getLoaderManager().initLoader(0, null, this);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
			return new CursorLoader(getActivity(),
					DbContract.PromptResponse
					.getPromptsByResponseID(ContentUris
							.parseId(getActivity().getIntent()
									.getData())),
									null, DbContract.PromptResponse.PROMPT_VALUE + " !=?", new String[] { "NOT_DISPLAYED" }, null);
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
				return cursor.getString(cursor.getColumnIndex(SurveyPrompt.PROMPT_TYPE));
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

				if(cursor.getColumnName(columnIndex).equals(PromptResponse.PROMPT_VALUE)) {
					String value = cursor.getString(columnIndex);

					if(view.getTag() instanceof ImageView) {
						String campaignUrn = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_URN));
						File photoDir = new File(PhotoPrompt.IMAGE_PATH + "/" + campaignUrn.replace(':', '_'));
						File photo = new File(photoDir, value + ".jpg");
						Bitmap img = BitmapFactory.decodeFile(photo.getAbsolutePath());

						((ImageView) view.getTag()).setImageBitmap(img);
					} else if(view.getTag() instanceof TextView) {
						String prompt_type = getItemPromptType(cursor);
						if("multi_choice".equals(prompt_type)) {
							value = "¥ Choice number 1\n¥ Much more tired\n¥ I went to the park and played";
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