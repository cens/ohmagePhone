package org.ohmage.activity;

import com.google.android.imageloader.ImageLoader;

import edu.ucla.cens.systemlog.Analytics;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.async.CampaignXmlDownloadTask;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.triggers.base.TriggerDB;
import org.ohmage.ui.BaseInfoActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CampaignInfoActivity extends BaseInfoActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TRIGGER_UPDATE_FINISHED = 0;

	// helpers
	private FragmentActivity mContext;
	private SharedPreferencesHelper mSharedPreferencesHelper;
	private ImageLoader mImageLoader;
	
	// action bar commands
	private static final int ACTION_TAKE_SURVEY = 1;
	private static final int ACTION_VIEW_RESPHISTORY = 2;
	private static final int ACTION_SETUP_TRIGGERS = 3;
	
	// handles to views we'll be manipulating
	private TextView mErrorBox;
	private TextView mDescView;
	private TextView mPrivacyValue;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	private TextView mTriggersValue;
	
	// state vars
	private int mCampaignStatus; // status code for campaign as of last refresh
	private int mLocalResponses;

	private String mCampaignUrn;

	private int mTriggerCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// save the context so the action bar can use it to fire off intents
		mContext = this;
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		mImageLoader = ImageLoader.get(this);
		
		setContentView(R.layout.campaign_info_details);

		// and inflate all the possible commands into the button tray
		getLayoutInflater().inflate(R.layout.campaign_info_buttons, mButtonTray, true);
		
		// clear some things to their default values
		mNotetext.setVisibility(View.GONE);
		
		// nab references to things we'll be populating
		mErrorBox = (TextView)findViewById(R.id.campaign_info_errorbox);
		mDescView = (TextView)findViewById(R.id.campaign_info_desc);
		
		mPrivacyValue = (TextView)findViewById(R.id.campaign_info_privacy_value);
		mStatusValue = (TextView)findViewById(R.id.campaign_info_status_value);
		mResponsesValue = (TextView)findViewById(R.id.campaign_info_responses_value);
		mTriggersValue = (TextView)findViewById(R.id.campaign_info_triggers_value);
		
		// and attach some handlers + populate some html data
		// privacy
		TextView privacyDetails = (TextView)findViewById(R.id.campaign_info_privacy_details);
		privacyDetails.setText(Html.fromHtml(getString(R.string.campaign_info_privacy_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_privacy_row),
				privacyDetails);
		
		// status
		TextView statusDetails = (TextView)findViewById(R.id.campaign_info_status_details);
		statusDetails.setText(Html.fromHtml(getString(R.string.campaign_info_status_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_status_row),
				statusDetails);
		
		// responses
		TextView responsesDetails = (TextView)findViewById(R.id.campaign_info_responses_details);
		responsesDetails.setText(Html.fromHtml(getString(R.string.campaign_info_responses_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_responses_row),
				responsesDetails);
		
		// triggers
		TextView triggersDetails = (TextView)findViewById(R.id.campaign_info_triggers_details);
		triggersDetails.setText(Html.fromHtml(getString(R.string.campaign_info_triggers_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_triggers_row),
				triggersDetails);
		
		// hide our content behind the overlay before we load
		setLoadingVisibility(true);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
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

	protected void populateCommands(final String campaignUrn, final int campaignStatus) {
		// first remove all the commands from the action bar...
		ActionBarControl actionBar = getActionBar();
		actionBar.clearActionBarCommands();
		
		// ...and gather up the commands in the command tray so we can hide/show them
		Button surveysButton = (Button)findViewById(R.id.campaign_info_button_surveys);
		Button participateButton = (Button)findViewById(R.id.campaign_info_button_particpate);
		Button removeButton = (Button)findViewById(R.id.campaign_info_button_remove);
		
		// now, depending on the context, we can regenerate our commands
		// this applies both to the action bar and to the command tray
		if (campaignStatus != Campaign.STATUS_REMOTE) {
			// only show data-related actions if it's ready
			if (campaignStatus == Campaign.STATUS_READY) {
				// FIXME: temporarily removed "take survey" button and moved it to the entity info header button tray
				// actionBar.addActionBarCommand(ACTION_TAKE_SURVEY, "take survey", R.drawable.dashboard_title_survey);
				actionBar.addActionBarCommand(ACTION_VIEW_RESPHISTORY, getString(R.string.response_history_action_button_description), R.drawable.dashboard_title_resphist);
				actionBar.addActionBarCommand(ACTION_SETUP_TRIGGERS, getString(R.string.reminder_action_button_description), R.drawable.dashboard_title_trigger);
				
				// route the actions to the appropriate places
				actionBar.setOnActionListener(new ActionListener() {
					@Override
					public void onActionClicked(int commandID) {
						Intent intent;
						
						switch (commandID) {
							case ACTION_TAKE_SURVEY:
								intent = new Intent(mContext, SurveyListActivity.class);
								intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
								startActivity(intent);
								break;
							case ACTION_VIEW_RESPHISTORY:
								intent = new Intent(mContext, ResponseHistoryActivity.class);
								intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
								startActivity(intent);
								break;
							case ACTION_SETUP_TRIGGERS:
								Intent triggerIntent = Campaign.launchTriggerIntent(mContext, campaignUrn);
								startActivityForResult(triggerIntent, TRIGGER_UPDATE_FINISHED);
								return;
						}
					}
				});
				
				// also show the take surveys button
				surveysButton.setVisibility(View.VISIBLE);
				// and attach a handler for it
				surveysButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Analytics.widget(v);
						Intent intent = new Intent(mContext, SurveyListActivity.class);
						intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
						startActivity(intent);
					}
				});
			}
			
			// and set the command tray buttons accordingly
			participateButton.setVisibility(View.GONE);
			removeButton.setVisibility(View.VISIBLE);
			
			// attach a remove handler
			removeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Analytics.widget(v);
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.campaign_info_remove_text));

					if(mTriggerCount != 0) {
						message.append(" ");
						message.append(getString(R.string.campaign_info_remove_campaign_reminders));
					}
					if(mLocalResponses != 0) {
						if(mTriggerCount != 0) {
							message.append(" ");
							message.append(Html.fromHtml(getResources().getQuantityString(R.plurals.campaign_info_remove_campaign_responses_w_reminders, mLocalResponses, mLocalResponses)));
						} else {
							message.append(" ");
							message.append(Html.fromHtml(getResources().getQuantityString(R.plurals.campaign_info_remove_campaign_responses, mLocalResponses, mLocalResponses)));
						}
						builder.setNeutralButton(R.string.upload, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								Intent uploadQueue = new Intent(CampaignInfoActivity.this, UploadQueueActivity.class);
								uploadQueue.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
								startActivity(uploadQueue);
							}
						});
					}
					if(mTriggerCount != 0 || mLocalResponses != 0)
						message.append(".");
					builder.setMessage(message);

					builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// set this campaign as "remote" and exit out of here
							Campaign.setRemote(CampaignInfoActivity.this, campaignUrn);
							mContext.finish();
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
			});
		}
		else {
			// show commands for a remote campaign (e.g. "participate")
			surveysButton.setVisibility(View.GONE);
			participateButton.setVisibility(View.VISIBLE);
			removeButton.setVisibility(View.GONE);
			
			// attach a participation handler
			participateButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Analytics.widget(v);
					// when clicked, it fires off a download task,
					// waits for it to finish, then goes back to the list when it's done
					new CampaignXmlDownloadTask(CampaignInfoActivity.this, campaignUrn, mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword()).startLoading();
				}
			});
		}
	}
	
	// ========================================================
	// === view databinding below,
	// === describes how this info view shows its data
	// ========================================================

	private interface QueryParams {
		String[] PROJECTION = {
					Campaigns.CAMPAIGN_URN,
					Campaigns.CAMPAIGN_NAME,
					Campaigns.CAMPAIGN_DESCRIPTION,
					Campaigns.CAMPAIGN_STATUS,
					Campaigns.CAMPAIGN_PRIVACY,
					Campaigns.CAMPAIGN_ICON
				};
		
		final int URN = 0;
		final int NAME = 1;
		final int DESCRIPTION = 2;
		final int STATUS = 3;
		final int PRIVACY = 4;
		final int ICON = 5;
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

		mCampaignUrn = data.getString(QueryParams.URN);

		// set the header fields first
		mHeadertext.setText(data.getString(QueryParams.NAME));
		mSubtext.setText(mCampaignUrn);

		final String iconUrl = data.getString(QueryParams.ICON);
		if(iconUrl == null || mImageLoader.bind(mIconView, iconUrl, null) != ImageLoader.BindResult.OK) {
			mIconView.setImageResource(R.drawable.apple_logo);
		}

		// fill in the description
		mDescView.setText(data.getString(QueryParams.DESCRIPTION));

		// set the appropriate text and icon for the privacy state
		String privacy = data.getString(QueryParams.PRIVACY);
		mPrivacyValue.setText(privacy);
		if ("private".equalsIgnoreCase(privacy))
			mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);
		else if ("shared".equalsIgnoreCase(privacy))
			mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_shared, 0, 0, 0);
		else
			mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);

		// hide our error box; it'll become visible below (and filled w/text) if the status is appropriate
		mErrorBox.setVisibility(View.GONE);

		// set many things on the view according to the campaign status, too
		mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_stopped, 0, 0, 0); // start out a default gray sphere
		mCampaignStatus = data.getInt(QueryParams.STATUS);
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

		// set the responses by querying the response table
		// and getting the number of responses submitted for this campaign
		Cursor responses = getContentResolver().query(Campaigns.buildResponsesUri(mCampaignUrn), null, null, null, null);
		mResponsesValue.setText(getResources().getQuantityString(R.plurals.campaign_info_response_count, responses.getCount(), responses.getCount()));

		mLocalResponses = Campaign.localResponseCount(this, mCampaignUrn);

		// get the number of triggers for this survey
		setTriggerCount();

		// and finally populate the action bar + command tray
		populateCommands(mCampaignUrn, mCampaignStatus);

		// and make the entity header visible (although i assume it already was)
		mEntityHeader.setVisibility(View.VISIBLE);

		// finally, show our content
		setLoadingVisibility(false);
	}

	private void setTriggerCount() {
		// get the number of triggers for this campaign
		TriggerDB trigDB = new TriggerDB(mContext);
		if (trigDB.open()) {
			Cursor triggers = trigDB.getAllTriggers(mCampaignUrn);
			mTriggerCount = triggers.getCount();
			mTriggersValue.setText(getResources().getQuantityString(R.plurals.campaign_info_trigger_count, mTriggerCount, mTriggerCount));
			triggers.close();
			trigDB.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// FIXME should we hide the entity header like cameron does?
	}
}
