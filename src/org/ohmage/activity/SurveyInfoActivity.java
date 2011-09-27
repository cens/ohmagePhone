package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.imageloader.ImageLoader;

public class SurveyInfoActivity extends BaseInfoActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	// helpers
	private FragmentActivity mContext;
	private SharedPreferencesHelper mSharedPreferencesHelper;
	private ImageLoader mImageLoader;

	// handles to views we'll be manipulating
	private TextView mErrorBox;
	private TextView mDescView;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	
	// state vars
	private int mCampaignStatus; // status code for campaign as of last refresh
	private Handler mHandler;
	private ContentObserver mResponsesObserver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// save the context so the action bar can use it to fire off intents
		mContext = this;
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		mImageLoader = ImageLoader.get(this);
		// and create a handler attached to this thread for contentobserver events
		mHandler = new Handler();

		// inflate the campaign-specific info page into the scrolling framelayout
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.survey_info_details, getContentArea(), true);
		
		// and inflate all the possible commands into the button tray
		inflater.inflate(R.layout.survey_info_buttons, mButtonTray, true);
		
		// clear some things to their default values
		mNotetext.setVisibility(View.GONE);
		
		// nab references to things we'll be populating
		mErrorBox = (TextView)findViewById(R.id.survey_info_errorbox);
		mDescView = (TextView)findViewById(R.id.survey_info_desc);
		mStatusValue = (TextView)findViewById(R.id.survey_info_status_value);
		mResponsesValue = (TextView)findViewById(R.id.survey_info_responses_value);
		
		// and attach some handlers + populate some html data
		// status
		TextView statusDetails = (TextView)findViewById(R.id.survey_info_status_details);
		statusDetails.setText(Html.fromHtml(getString(R.string.survey_info_status_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.survey_info_status_row),
				statusDetails);
		
		// responses
		TextView responsesDetails = (TextView)findViewById(R.id.survey_info_responses_details);
		responsesDetails.setText(Html.fromHtml(getString(R.string.survey_info_responses_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.survey_info_responses_row),
				responsesDetails);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(1, null, this);
	}
	
	protected void populateCommands(final String surveyID, final String campaignUrn, final String surveyTitle, final String surveySubmitText, int campaignStatus) {
		// gather up the commands in the command tray so we can hide/show them
		Button takeSurveyButton = (Button)findViewById(R.id.survey_info_button_takesurvey);

		// now, depending on the context, we can regenerate our commands
		// this applies both to the action bar and to the command tray
		if (campaignStatus == Campaign.STATUS_READY) {
			takeSurveyButton.setEnabled(true);
			
			// attach a remove handler
			takeSurveyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// fire off the survey intent
					Intent intent = new Intent(mContext, SurveyActivity.class);
					intent.putExtra("campaign_urn", campaignUrn);
					intent.putExtra("survey_id", surveyID);
					intent.putExtra("survey_title", surveyTitle);
					intent.putExtra("survey_submit_text", surveySubmitText);
					startActivity(intent);
				}
			});
		}
		else {
			takeSurveyButton.setEnabled(false);
		}
	}
	
	// ========================================================
	// === view databinding below,
	// === describes how this info view shows its data
	// ========================================================

	private interface QueryParams {
		String[] PROJECTION = {
					Survey.SURVEY_ID,
					Survey.CAMPAIGN_URN,
					Survey.TITLE,
					Survey.DESCRIPTION,
					Survey.SUBMIT_TEXT,
					Campaign.NAME,
					Campaign.STATUS,
					Campaign.ICON
				};
		
		final int SURVEY_ID = 0;
		final int CAMPAIGN_URN = 1;
		final int TITLE = 2;
		final int DESCRIPTION = 3;
		final int SUBMIT_TEXT = 4;
		final int CAMPAIGN_NAME = 5;
		final int CAMPAIGN_STATUS = 6;
		final int CAMPAIGN_ICON = 7;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(this, getIntent().getData(), QueryParams.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// ensure that we're on the first record in the cursor
		if (!data.moveToFirst())
			return;

		// populate the views
		final String surveyID = data.getString(QueryParams.SURVEY_ID);
		final String campaignUrn = data.getString(QueryParams.CAMPAIGN_URN);
		String submitText = data.getString(QueryParams.SUBMIT_TEXT);

		// set the header fields first
		mHeadertext.setText(data.getString(QueryParams.TITLE));
		mSubtext.setText(data.getString(QueryParams.CAMPAIGN_NAME));
		mNotetext.setVisibility(View.INVISIBLE);
		
		final String iconUrl = data.getString(QueryParams.CAMPAIGN_ICON);
		if(iconUrl == null || mImageLoader.bind(mIconView, iconUrl, null) != ImageLoader.BindResult.OK) {
			mIconView.setImageResource(R.drawable.apple_logo);
		}
		
		// fill in the description
		mDescView.setText(data.getString(QueryParams.DESCRIPTION));
		
		// hide our error box; it'll become visible below (and filled w/text) if the status is appropriate
		mErrorBox.setVisibility(View.GONE);
		
		// set many things on the view according to the campaign status, too
		mStatusValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // start out with nothing drawn
		mCampaignStatus = data.getInt(QueryParams.CAMPAIGN_STATUS);
		switch (mCampaignStatus) {
			case Campaign.STATUS_READY:
				mStatusValue.setText("participating");
				mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_running, 0, 0, 0);
				break;
			case Campaign.STATUS_VAGUE:
				mStatusValue.setText("not available");
				break;
			case Campaign.STATUS_REMOTE:
				mStatusValue.setText("available");
				break;
			case Campaign.STATUS_OUT_OF_DATE:
				mStatusValue.setText("out of date");
				break;
			case Campaign.STATUS_DELETED:
				mStatusValue.setText("deleted on server");
				break;
			case Campaign.STATUS_STOPPED:
				mStatusValue.setText("stopped");
				mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_stopped, 0, 0, 0);
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_stopped)));
				break;
			case Campaign.STATUS_INVALID_USER_ROLE:
				mStatusValue.setText("invalid role");
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_invalid_role)));
				break;
			case Campaign.STATUS_DOWNLOADING:
				mStatusValue.setText("downloading...");
				break;
			default:
				mStatusValue.setText("unknown status");
				break;
		}
		
		mResponsesObserver = new ContentObserver(mHandler) {
			@Override
			public void onChange(boolean selfChange) {
				// TODO Auto-generated method stub
				super.onChange(selfChange);

				// set the responses by querying the response table
				// and getting the number of responses submitted for this campaign
				Cursor responses = getContentResolver().query(Response.getResponsesByCampaignAndSurvey(campaignUrn, surveyID), null, null, null, null);
				mResponsesValue.setText(responses.getCount() + " response(s) submitted");
			}
		};
		
		// register it to listen for newly submitted responses
		getContentResolver().registerContentObserver(Response.CONTENT_URI, true, mResponsesObserver);
		// and trigger it once to refresh right now
		mResponsesObserver.onChange(false);
		
		// and finally populate the action bar + command tray
		populateCommands(surveyID, campaignUrn, data.getString(QueryParams.TITLE), submitText, mCampaignStatus);

		// and make the entity header visible (although i assume it already was)
		mEntityHeader.setVisibility(View.VISIBLE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// FIXME should we hide the entity header like cameron does?
	}
}
