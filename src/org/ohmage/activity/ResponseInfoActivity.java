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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.google.android.imageloader.ImageLoader.Callback;

import edu.ucla.cens.systemlog.Analytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.ohmage.ConfigHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApplication;
import org.ohmage.OhmageMarkdown;
import org.ohmage.R;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.ui.BaseInfoActivity;
import org.ohmage.ui.ResponseActivityHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	private TextView uploadButton;
	private ResponseActivityHelper mResponseHelper;
	private int mStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentFragment(new ResponsePromptsFragment());
		
		mResponseHelper = new ResponseActivityHelper(this);

		mImageLoader = ImageLoader.get(this);
		
		// inflate the campaign-specific info page into the scrolling framelayout
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		// and inflate all the possible commands into the button tray
		inflater.inflate(R.layout.response_info_buttons, mButtonTray, true);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
		
		mapViewButton = (TextView) findViewById(R.id.response_info_button_view_map);
		mapViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Analytics.widget(v);
				startActivity(new Intent(OhmageApplication.VIEW_MAP, getIntent().getData()));
			}
		});

		uploadButton = (TextView) findViewById(R.id.response_info_button_upload);
		uploadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Analytics.widget(v);
				if(mStatus == Response.STATUS_STANDBY)
					mResponseHelper.queueForUpload(getIntent().getData());
				else {
					Bundle bundle = new Bundle();
					bundle.putParcelable(ResponseActivityHelper.KEY_URI, getIntent().getData());
					showDialog(mStatus, bundle);
				}
			}
		});
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mEntityHeader.setVisibility(View.GONE);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		mResponseHelper.onPrepareDialog(id, dialog, args);
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return mResponseHelper.onCreateDialog(id, args);
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
			Toast.makeText(this, R.string.response_info_response_deleted, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		final String surveyName = data.getString(ResponseQuery.SURVEY_TITLE);
		final Long completedDate = data.getLong(ResponseQuery.TIME);
		mStatus = data.getInt(ResponseQuery.STATUS);

		if(mStatus == Response.STATUS_STANDBY)
			uploadButton.setContentDescription(getString(R.string.response_info_entity_action_button_upload_description));
		else if(mStatus == Response.STATUS_WAITING_FOR_LOCATION)
			uploadButton.setContentDescription(getString(R.string.response_info_entity_action_button_upload_force_description));
		else
			uploadButton.setContentDescription(getString(R.string.response_info_entity_action_button_upload_error_description));

		mHeadertext.setText(surveyName);
		mSubtext.setText(data.getString(ResponseQuery.CAMPAIGN_NAME));
		// If we aren't in single campaign mode, show the campaign name
		mSubtext.setVisibility((ConfigHelper.isSingleCampaignMode()) ? View.GONE : View.VISIBLE);
		SimpleDateFormat df = new SimpleDateFormat();
		mNotetext.setText(df.format(new Date(completedDate)));
		
		final String iconUrl = data.getString(ResponseQuery.CAMPAIGN_ICON);
		if(iconUrl == null || mImageLoader.bind(mIconView, iconUrl, null) != ImageLoader.BindResult.OK) {
			mIconView.setImageResource(R.drawable.apple_logo);
		}

		mEntityHeader.setVisibility(View.VISIBLE);
		
		// Make the map view button status aware so it can provide some useful info about the gps state
		if(mStatus == Response.STATUS_WAITING_FOR_LOCATION) {
			mapViewButton.setText(R.string.response_info_gps_wait);
			mapViewButton.setEnabled(false);
		} else if(!(SurveyGeotagService.LOCATION_VALID.equals(data.getString(ResponseQuery.LOCATION_STATUS)))) {
			mapViewButton.setText(R.string.response_info_no_location);
			mapViewButton.setEnabled(false);
		} else {
			mapViewButton.setText(R.string.response_info_view_map);
			mapViewButton.setEnabled(true);
		}

		// Make upload button visible if applicable
		uploadButton.setVisibility(View.VISIBLE);
		switch(mStatus) {
			case Response.STATUS_DOWNLOADED:
			case Response.STATUS_UPLOADED:
				uploadButton.setVisibility(View.GONE);
				break;
			case Response.STATUS_STANDBY:
			case Response.STATUS_WAITING_FOR_LOCATION:
				uploadButton.setText(R.string.response_info_upload);
				uploadButton.setEnabled(true);
				break;
			case Response.STATUS_QUEUED:
			case Response.STATUS_UPLOADING:
				uploadButton.setText(R.string.response_info_uploading);
				uploadButton.setEnabled(false);
				break;
			default:
				// Error
				uploadButton.setText(R.string.response_info_upload_error);
				uploadButton.setEnabled(true);
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
				android.R.id.text1, R.id.prompt_value }, 0, getResponseId());

			setListAdapter(mAdapter);

			// Start out with a progress indicator.
			setListShown(false);

			getLoaderManager().initLoader(0, null, this);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
			return new CursorLoader(getActivity(),
					Responses.buildPromptResponsesUri(Long.valueOf(getResponseId())),
					null, PromptResponses.PROMPT_RESPONSE_VALUE + " !=?", new String[] { "NOT_DISPLAYED" }, null);
		}

		public String getResponseId() {
			return Responses.getResponseId(getActivity().getIntent().getData());
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

			public static final int UNKNOWN_RESPONSE = -1;
			public static final int TEXT_RESPONSE = 0;
			public static final int IMAGE_RESPONSE = 1;
			public static final int HOURSBEFORENOW_RESPONSE = 2;
			public static final int TIMESTAMP_RESPONSE = 3;
			public static final int MULTICHOICE_RESPONSE = 4;
			public static final int MULTICHOICE_CUSTOM_RESPONSE = 5;
			public static final int SINGLECHOICE_RESPONSE = 6;
			public static final int SINGLECHOICE_CUSTOM_RESPONSE = 7;
			public static final int NUMBER_RESPONSE = 8;
			public static final int REMOTE_RESPONSE = 9;
			public static final int VIDEO_RESPONSE = 10;

			private final String mResponseId;
			private final ImageLoader mImageLoader;

			public PromptResponsesAdapter(Context context, Cursor c, String[] from,
					int[] to, int flags, String responseId) {
				super(context, R.layout.response_prompt_list_item, c, from, to, flags);
				mImageLoader = ImageLoader.get(context);
				setViewBinder(this);
				mResponseId = responseId;
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
				String promptType = getItemPromptType(cursor);
				
				if("photo".equals(promptType))
					return IMAGE_RESPONSE;
				else if ("video".equals(promptType))
					return VIDEO_RESPONSE;
				else if ("text".equals(promptType))
					return TEXT_RESPONSE;
				else if ("hours_before_now".equals(promptType))
					return HOURSBEFORENOW_RESPONSE;
				else if ("timestamp".equals(promptType))
					return TIMESTAMP_RESPONSE;
				else if ("single_choice".equals(promptType))
					return SINGLECHOICE_RESPONSE;
				else if ("single_choice_custom".equals(promptType))
					return SINGLECHOICE_CUSTOM_RESPONSE;
				else if ("multi_choice".equals(promptType))
					return MULTICHOICE_RESPONSE;
				else if ("multi_choice_custom".equals(promptType))
					return MULTICHOICE_CUSTOM_RESPONSE;
				else if ("number".equals(promptType))
					return NUMBER_RESPONSE;
				else if ("remote_activity".equals(promptType))
					return REMOTE_RESPONSE;
				else
					return UNKNOWN_RESPONSE;
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
				View progress = view.findViewById(R.id.prompt_image_progress);
				View text = view.findViewById(R.id.prompt_text_value);
				View value = view.findViewById(R.id.prompt_value);
				ImageView icon = (ImageView) view.findViewById(R.id.prompt_icon);
				
				int itemViewType = getItemViewType(cursor);
				
				// set the icon for each prompt type
				switch(itemViewType) {
					case VIDEO_RESPONSE:
					case IMAGE_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_photo);
						break;
					case HOURSBEFORENOW_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_hoursbeforenow);
						break;
					case TIMESTAMP_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_timestamp);
						break;
					case MULTICHOICE_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_multichoice);
						break;
					case MULTICHOICE_CUSTOM_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_multichoice_custom);
						break;
					case SINGLECHOICE_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_singlechoice);
						break;
					case SINGLECHOICE_CUSTOM_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_singlechoice_custom);
						break;
					case NUMBER_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_number);
						break;
					case REMOTE_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_remote);
						break;
					case TEXT_RESPONSE:
						icon.setImageResource(R.drawable.prompttype_text);
						break;
				}
				
				// now set up how they're actually displayed
				// there are only two categories: image and non-image (i.e. text)
				if (itemViewType != IMAGE_RESPONSE
						// also if the image was skipped we are showing the text view
						|| AbstractPrompt.SKIPPED_VALUE.equals(cursor.getString(cursor.getColumnIndex(PromptResponses.PROMPT_RESPONSE_VALUE)))) {
					progress.setVisibility(View.GONE);
					image.setVisibility(View.GONE);
					text.setVisibility(View.VISIBLE);
					value.setTag(text);
				} else {
					progress.setVisibility(View.VISIBLE);
					image.setVisibility(View.VISIBLE);
					text.setVisibility(View.GONE);
					value.setTag(image);
					value.setBackgroundResource(R.drawable.prompt_response_image_item_bg);
				}
				
				return view;
			}

			@Override
			public int getViewTypeCount() {
				return 11;
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
						final File file = Response.getTemporaryResponsesMedia(value);
						final ImageView imageView = (ImageView) view.getTag();

						if(file != null && file.exists()) {
							try {
								Bitmap img = BitmapFactory.decodeStream(new FileInputStream(file));
								imageView.setImageBitmap(img);
								imageView.setOnClickListener(new View.OnClickListener() {

									@Override
									public void onClick(View v) {
										Analytics.widget(v, "View Local Fullsize Image");
										Intent intent = new Intent(Intent.ACTION_VIEW);
										intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
										mContext.startActivity(intent);
									}
								});
								return true;
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						String url = OhmageApi.defaultImageReadUrl(value, campaignUrn, "small");
						final String largeUrl = OhmageApi.defaultImageReadUrl(value, campaignUrn, null);
						imageView.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								Analytics.widget(v, "View Fullsize Image");
								Intent intent = new Intent(OhmageApplication.ACTION_VIEW_REMOTE_IMAGE, Uri.parse(largeUrl));
								mContext.startActivity(intent);
							}
						});

						mImageLoader.clearErrors();
						BindResult bindResult = mImageLoader.bind((ImageView)view.getTag(), url, new Callback() {

							@Override
							public void onImageLoaded(ImageView view, String url) {
								imageView.setVisibility(View.VISIBLE);
								imageView.setClickable(true);
								imageView.setFocusable(true);
							}

							@Override
							public void onImageError(ImageView view, String url, Throwable error) {
								imageView.setVisibility(View.VISIBLE);
								imageView.setImageResource(android.R.drawable.ic_dialog_alert);
								imageView.setClickable(false);
								imageView.setFocusable(false);
							}
						});
						if(bindResult == ImageLoader.BindResult.ERROR) {
							imageView.setImageResource(android.R.drawable.ic_dialog_alert);
							imageView.setClickable(false);
							imageView.setFocusable(false);
						} else  if(bindResult == ImageLoader.BindResult.LOADING){
							imageView.setVisibility(View.GONE);
						}
					} else if(view.getTag() instanceof TextView) {
						String prompt_type = getItemPromptType(cursor);
						if("multi_choice_custom".equals(prompt_type) || "multi_choice".equals(prompt_type)) {
							try {
								JSONArray choices = new JSONArray(value);
								StringBuilder builder = new StringBuilder();
								for(int i=0;i<choices.length();i++) {
									if(i != 0)
										builder.append("<br\\>");
									builder.append("&bull; ");
									builder.append(OhmageMarkdown.parseHtml(choices.get(i).toString()));
								}
								((TextView) view.getTag()).setText(Html.fromHtml(builder.toString()));
								return true;
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else if("single_choice_custom".equals(prompt_type) || "single_choice".equals(prompt_type)) {
								((TextView) view.getTag()).setText(OhmageMarkdown.parse(value));
								return true;
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