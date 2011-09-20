package org.ohmage.activity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaign;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

public class CampaignInfoActivity extends BaseInfoActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	// helpers
	private FragmentActivity mContext;
	
	// action bar commands
	private static final int ACTION_TAKE_SURVEY = 1;
	private static final int ACTION_VIEW_RESPHISTORY = 2;
	
	// handles to views we'll be manipulating
	private TextView mErrorBox;
	private TextView mDescView;
	private TextView mPrivacyValue;
	private TextView mStatusValue;
	
	// state vars
	private int mCampaignStatus; // status code for campaign as of last refresh

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// save the context so the action bar can use it to fire offi ntents
		mContext = this;
		
		getActionBar().setTitle("Campaign Info");
		
		// inflate the campaign-specific info page into the scrolling framelayout
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.campaign_info_details, getContentArea(), true);
		
		// clear some things to their default values
		mNotetext.setVisibility(View.GONE);
		
		// nab references to things we'll be populating
		mErrorBox = (TextView)findViewById(R.id.campaign_info_errorbox);
		mDescView = (TextView)findViewById(R.id.campaign_info_desc);
		
		mPrivacyValue = (TextView)findViewById(R.id.campaign_info_privacy_value);
		mStatusValue = (TextView)findViewById(R.id.campaign_info_status_value);
		
		// and attach some handlers + populate some html data
		TextView privacyDetails = (TextView)findViewById(R.id.campaign_info_privacy_details);
		privacyDetails.setText(Html.fromHtml(getString(R.string.campaign_info_privacy_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_privacy_row),
				privacyDetails);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
	}
	
	protected void populateCommands(final String campaignUrn, int campaignStatus) {
		// step 1. action bar
		
		// first remove all the commands from the page
		ActionBarControl actionBar = getActionBar();
		actionBar.clearActionBarCommands();
		
		// now, depending on the context, we can regenerate our commands
		if (campaignStatus != Campaign.STATUS_REMOTE) {
			actionBar.addActionBarCommand(ACTION_TAKE_SURVEY, "take survey", R.drawable.dashboard_title_survey);
			actionBar.addActionBarCommand(ACTION_VIEW_RESPHISTORY, "take survey", R.drawable.dashboard_title_resphist);
			
			// route the actions to the appropriate places
			actionBar.setOnActionListener(new ActionListener() {
				@Override
				public void onActionClicked(int commandID) {
					Intent intent;
					
					switch (commandID) {
						case ACTION_TAKE_SURVEY:
							intent = new Intent(mContext, SurveyListActivity.class);
							intent.putExtra("campaign_urn", campaignUrn);
							startActivity(intent);
							break;
						case ACTION_VIEW_RESPHISTORY:
							intent = new Intent(mContext, SurveyListActivity.class);
							intent.putExtra("campaign_urn", campaignUrn);
							startActivity(intent);
							break;
					}
				}
			});
		}
		
		// step 2. command tray
		
		// in this case, we just conditionally display the right thing
	}
	
	// ========================================================
	// === view databinding below,
	// === describes how this info view shows its data
	// ========================================================

	private interface QueryParams {
		String[] PROJECTION = {
					Campaign.URN,
					Campaign.NAME,
					Campaign.CONFIGURATION_XML,
					Campaign.DESCRIPTION,
					Campaign.STATUS,
					Campaign.PRIVACY
				};
		
		final int URN = 0;
		final int NAME = 1;
		final int CONFIGURATION_XML = 2;
		final int DESCRIPTION = 3;
		final int STATUS = 4;
		final int PRIVACY = 5;
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
		try {
			String campaignUrn = data.getString(QueryParams.URN);
			
			// nab the data from the xml associated with this campaign
			Map<String, String> campaignInfo = PromptXmlParser.parseCampaignInfo(
					new ByteArrayInputStream(data.getString(QueryParams.CONFIGURATION_XML).getBytes("UTF-8"))
					);

			// set the header fields first
			mHeadertext.setText(data.getString(QueryParams.NAME));
			mSubtext.setText(campaignUrn);
			mNotetext.setVisibility(View.INVISIBLE);
			
			// fill in the description
			mDescView.setText(data.getString(QueryParams.DESCRIPTION));
			
			// set the appropriate text and icon for the privacy state
			String privacy = data.getString(QueryParams.PRIVACY);
			mPrivacyValue.setText(privacy);
			if (privacy.equalsIgnoreCase("private"))
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);
			else if (privacy.equalsIgnoreCase("shared"))
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_shared, 0, 0, 0);
			else
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);
			
			// hide our error box; it'll become visible below (and filled w/text) if the status is appropriate
			mErrorBox.setVisibility(View.GONE);
			
			// set many things on the view according to the campaign status, too
			mStatusValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // start out with nothing drawn
			mCampaignStatus = data.getInt(QueryParams.STATUS);
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
			
			// and finally populate the action bar + command tray
			populateCommands(campaignUrn, mCampaignStatus);

			// and make the entity header visible (although i assume it already was)
			mEntityHeader.setVisibility(View.VISIBLE);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// FIXME should we hide the entity header like cameron does?
	}
}
