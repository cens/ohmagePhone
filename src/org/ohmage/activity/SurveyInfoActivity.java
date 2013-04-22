package org.ohmage.activity;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.imageloader.ImageLoader;

import org.ohmage.ConfigHelper;
import org.ohmage.mobilizingcs.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Survey;
import org.ohmage.logprobe.Analytics;
import org.ohmage.triggers.base.TriggerDB;
import org.ohmage.ui.BaseInfoActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;
import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SurveyInfoActivity extends BaseInfoActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	protected static final int TRIGGER_UPDATE_FINISHED = 0;

	// action bar commands
	private static final int ACTION_VIEW_RESPHISTORY = 1;
	private static final int ACTION_SETUP_TRIGGERS = 2;
	
	// helpers
	private FragmentActivity mContext;
	private ConfigHelper mSharedPreferencesHelper;
	private ImageLoader mImageLoader;

	// handles to views we'll be manipulating
	private TextView mErrorBox;
	private TextView mDescView;
	private TextView mCampaignUrnValue;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	private TextView mLastResponseValue;
	private TextView mTriggersValue;
	
	// state vars
	private int mCampaignStatus; // status code for campaign as of last refresh
	private Handler mHandler;
	private ContentObserver mResponsesObserver;

	private String mSurveyTitle;
	private String mCampaignUrn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// save the context so the action bar can use it to fire off intents
		mContext = this;
		mSharedPreferencesHelper = new ConfigHelper(this);
		mImageLoader = ImageLoader.get(this);
		// and create a handler attached to this thread for contentobserver events
		mHandler = new Handler();

		// set the campaign-specific info page
		setContentView(R.layout.survey_info_details);
		
		// and inflate all the possible commands into the button tray
		getLayoutInflater().inflate(R.layout.survey_info_buttons, mButtonTray, true);
		
		// clear some things to their default values
		mNotetext.setVisibility(View.GONE);
		
		// nab references to things we'll be populating
		mErrorBox = (TextView)findViewById(R.id.survey_info_errorbox);
		mDescView = (TextView)findViewById(R.id.survey_info_desc);
		mCampaignUrnValue = (TextView)findViewById(R.id.survey_info_campaign_urn_value);
		mStatusValue = (TextView)findViewById(R.id.survey_info_status_value);
		mResponsesValue = (TextView)findViewById(R.id.survey_info_responses_value);
		mLastResponseValue = (TextView)findViewById(R.id.survey_info_last_response_value);
		mTriggersValue = (TextView)findViewById(R.id.survey_info_triggers_value);
		
		// and attach some handlers + populate some html data
		// status
		TextView campaignUrnDetails = (TextView)findViewById(R.id.survey_info_campaign_urn_details);
		campaignUrnDetails.setText(Html.fromHtml(getString(R.string.survey_info_campaign_urn_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.survey_info_campaign_urn_row),
				campaignUrnDetails);
		// If we aren't in single campaign mode, show the campaign urn details
		findViewById(R.id.survey_info_campaign_urn_row).setVisibility((ConfigHelper.isSingleCampaignMode()) ? View.GONE : View.VISIBLE);

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

		// last response
		TextView lastResponseDetails = (TextView)findViewById(R.id.survey_info_last_response_details);
		lastResponseDetails.setText(Html.fromHtml(getString(R.string.survey_info_last_response_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.survey_info_last_response_row),
				lastResponseDetails);
		
		// triggers
		TextView triggersDetails = (TextView)findViewById(R.id.survey_info_triggers_details);
		triggersDetails.setText(Html.fromHtml(getString(R.string.survey_info_triggers_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.survey_info_triggers_row),
				triggersDetails);
		
		// hide our content behind the overlay before we load
		setLoadingVisibility(true);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(1, null, this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case TRIGGER_UPDATE_FINISHED:
				//Triggers might have changed so we update it here
				setTriggerCount();
				break;
		}
	}
	
	protected void populateCommands(final String surveyID, final String campaignUrn, final String surveyTitle, final String surveySubmitText, int campaignStatus) {
		// first remove all the commands from the action bar...
		ActionBarControl actionBar = getActionBarControl();
		actionBar.clearActionBarCommands();
		
		// gather up the commands in the command tray so we can hide/show them
		Button takeSurveyButton = (Button)findViewById(R.id.survey_info_button_takesurvey);

		// now, depending on the context, we can regenerate our commands
		// this applies both to the action bar and to the command tray
		if (campaignStatus == Campaign.STATUS_READY) {
			// only add response history if show feedback is true
			if(new UserPreferencesHelper(this).showFeedback())
				actionBar.addActionBarCommand(ACTION_VIEW_RESPHISTORY, getString(R.string.response_history_action_button_description), R.drawable.btn_title_resphist);
			actionBar.addActionBarCommand(ACTION_SETUP_TRIGGERS, getString(R.string.reminder_action_button_description), R.drawable.btn_title_trigger);
			
			// route the actions to the appropriate places
			actionBar.setOnActionListener(new ActionListener() {
				@Override
				public void onActionClicked(int commandID) {
					Intent intent;
					
					switch (commandID) {
						case ACTION_VIEW_RESPHISTORY:
							intent = new Intent(mContext, ResponseHistoryActivity.class);
							intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
							intent.putExtra(CampaignSurveyFilter.EXTRA_SURVEY_ID, surveyID);
							startActivity(intent);
							break;
						case ACTION_SETUP_TRIGGERS:
							Intent triggerIntent = Survey.launchTriggerIntent(mContext, campaignUrn, new String[] { surveyTitle });
							startActivityForResult(triggerIntent, TRIGGER_UPDATE_FINISHED);
							return;
					}
				}
			});
			
			takeSurveyButton.setEnabled(true);
			
			// attach a remove handler
			takeSurveyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Analytics.widget(v);
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
					Surveys.SURVEY_ID,
					Surveys.CAMPAIGN_URN,
					Surveys.SURVEY_TITLE,
					Surveys.SURVEY_DESCRIPTION,
					Surveys.SURVEY_SUBMIT_TEXT,
					Campaigns.CAMPAIGN_NAME,
					Campaigns.CAMPAIGN_STATUS,
					Campaigns.CAMPAIGN_ICON,
					Surveys.SURVEY_TITLE
				};
		
		final int SURVEY_ID = 0;
		final int CAMPAIGN_URN = 1;
		final int TITLE = 2;
		final int DESCRIPTION = 3;
		final int SUBMIT_TEXT = 4;
		final int CAMPAIGN_NAME = 5;
		final int CAMPAIGN_STATUS = 6;
		final int CAMPAIGN_ICON = 7;
		final int SURVEY_TITLE = 8;
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
		mSurveyTitle = data.getString(QueryParams.SURVEY_TITLE);
		mCampaignUrn = data.getString(QueryParams.CAMPAIGN_URN);
		String submitText = data.getString(QueryParams.SUBMIT_TEXT);

		// set the header fields first
		mHeadertext.setText(data.getString(QueryParams.TITLE));
		mSubtext.setText(data.getString(QueryParams.CAMPAIGN_NAME));
		// If we aren't in single campaign mode, show the campaign name
		mSubtext.setVisibility((ConfigHelper.isSingleCampaignMode()) ? View.GONE : View.VISIBLE);
		
		final String iconUrl = data.getString(QueryParams.CAMPAIGN_ICON);
		if(iconUrl == null || mImageLoader.bind(mIconView, iconUrl, null) != ImageLoader.BindResult.OK) {
			mIconView.setImageResource(R.drawable.apple_logo);
		}
		
		// fill in the description
		mDescView.setText(data.getString(QueryParams.DESCRIPTION));
		
		// hide our error box; it'll become visible below (and filled w/text) if the status is appropriate
		mErrorBox.setVisibility(View.GONE);
		
		mCampaignUrnValue.setText(mCampaignUrn);

		// set many things on the view according to the campaign status, too
		mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_status_other, 0, 0, 0); // start out a default gray sphere
		mCampaignStatus = data.getInt(QueryParams.CAMPAIGN_STATUS);
		switch (mCampaignStatus) {
			case Campaign.STATUS_READY:
				mStatusValue.setText(R.string.campaign_status_ready);
				mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_running, 0, 0, 0);
				break;
			case Campaign.STATUS_VAGUE:
				mStatusValue.setText(R.string.campaign_status_vague);
				break;
			case Campaign.STATUS_REMOTE:
				mStatusValue.setText(R.string.campaign_status_remote);
				break;
			case Campaign.STATUS_OUT_OF_DATE:
				mStatusValue.setText(R.string.campaign_status_out_of_date);
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_outofdate)));
				break;
			case Campaign.STATUS_NO_EXIST:
				mStatusValue.setText(R.string.campaign_status_no_exist);
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_no_exist)));
				break;
			case Campaign.STATUS_STOPPED:
				mStatusValue.setText(R.string.campaign_status_stopped);
				mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_stopped, 0, 0, 0);
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_stopped)));
				break;
			case Campaign.STATUS_INVALID_USER_ROLE:
				mStatusValue.setText(R.string.campaign_status_invalid_user_role);
				mErrorBox.setVisibility(View.VISIBLE);
				mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_invalid_role)));
				break;
			case Campaign.STATUS_DOWNLOADING:
				mStatusValue.setText(R.string.campaign_status_downloading);
				break;
			default:
				mStatusValue.setText(R.string.campaign_status_unknown);
				break;
		}
		
		// create an observer to watch the responses table for changes (hopefully)
		mResponsesObserver = new ContentObserver(mHandler) {
			@Override
			public void onChange(boolean selfChange) {
				// TODO Auto-generated method stub
				super.onChange(selfChange);

				// set the responses by querying the response table
				// and getting the number of responses submitted for this campaign
				Cursor responses = getContentResolver().query(Campaigns.buildResponsesUri(mCampaignUrn, surveyID), null, null, null, Responses.RESPONSE_DATE);
				mResponsesValue.setText(getResources().getQuantityString(R.plurals.campaign_info_response_count, responses.getCount(), responses.getCount()));
				
				// also get the date of the last submitted response and populate the last response field
				if (responses.moveToLast()) {
					String lastResponseDate = responses.getString(responses.getColumnIndex(Responses.RESPONSE_DATE));
					SimpleDateFormat fromUTC = new SimpleDateFormat("yyyy-MM-dd k:mm:ss");
					SimpleDateFormat toNice = new SimpleDateFormat("EEE MMM d yyyy, h:mm a");
					
					try {
						mLastResponseValue.setText(toNice.format(fromUTC.parse(lastResponseDate)));
					}
					catch (ParseException e) {
						mLastResponseValue.setText("(not available)");
					}
				}
				else
					mLastResponseValue.setText("(not available)");

				
				responses.close();
			}
		};
		
		// register it to listen for newly submitted responses
		getContentResolver().registerContentObserver(Responses.CONTENT_URI, true, mResponsesObserver);
		// and trigger it once to refresh right now
		mResponsesObserver.onChange(false);
		
		// get the number of triggers for this survey
		setTriggerCount();
		
		// and finally populate the action bar + command tray
		populateCommands(surveyID, mCampaignUrn, data.getString(QueryParams.TITLE), submitText, mCampaignStatus);

		// and make the entity header visible (although i assume it already was)
		mEntityHeader.setVisibility(View.VISIBLE);
		
		// finally, show our content
		setLoadingVisibility(false);
	}

	public void setTriggerCount() {
		TriggerDB trigDB = new TriggerDB(mContext);
		if (trigDB.open()) {
			Cursor triggers = trigDB.getSurveyTriggers(mCampaignUrn, mSurveyTitle);
			mTriggersValue.setText(getResources().getQuantityString(R.plurals.campaign_info_trigger_count, triggers.getCount(), triggers.getCount()));
			triggers.close();
			trigDB.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// FIXME should we hide the entity header like cameron does?
	}
}
