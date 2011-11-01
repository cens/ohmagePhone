package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Pair;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class TestActivity extends Activity {
	private ContentResolver mCR;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// save context for children to use
		mContext = this;
		
		// create an action bar for testing purposes
		final ActionBarControl actionbar = new ActionBarControl(this);
		actionbar.setTitle("Test Activity");
		
		// and add a command to it for fun
		actionbar.addActionBarCommand(1, "refresh", R.drawable.dashboard_title_refresh);
		
		// and attach our listener
		actionbar.setOnActionListener(new ActionListener() {
			@Override
			public void onActionClicked(int commandID) {
				switch (commandID) {
					case 1:
						Toast.makeText(mContext , "refreshed!", Toast.LENGTH_SHORT).show();
						break;
				}
			}
		});
		
		// instantiate a filter just for fun and populate it from the campaigns list
		// also create a filter that will be populated by the survey list
		final FilterControl campaignFilter = new FilterControl(this);
		final FilterControl surveyFilter = new FilterControl(this);
		final DateFilterControl dateFilter = new DateFilterControl(this);
		
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		layout.addView(actionbar, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(campaignFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(surveyFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(dateFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		campaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				Cursor surveys;
				
				if (!curValue.equalsIgnoreCase("all"))
					surveys = mCR.query(Campaigns.buildSurveysUri(curValue), null, null, null, null);
				else
					surveys = mCR.query(Surveys.CONTENT_URI, null, null, null, null);

				ArrayList<Pair<String,String>> items = new ArrayList<Pair<String,String>>();
				
				while (surveys.moveToNext()) {
					items.add(
							Pair.create(
									surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_TITLE)),
									surveys.getString(surveys.getColumnIndex(Surveys.CAMPAIGN_URN)) +
									":" + surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_ID))
									)
							);
				}
				
				surveyFilter.populate(items);
				surveyFilter.add(0, Pair.create("All Surveys", "all"));
				
				surveys.close();
			}
		});
		
		mCR = getContentResolver();
		Cursor campaigns = mCR.query(Campaigns.CONTENT_URI, null, Campaigns.CAMPAIGN_STATUS + "!=" + Campaign.STATUS_REMOTE, null, null);
		campaignFilter.populate(campaigns, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
		campaignFilter.add(0, Pair.create("All Campaigns", "all"));
	}
}
