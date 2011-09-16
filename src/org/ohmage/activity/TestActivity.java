package org.ohmage.activity;

import java.util.ArrayList;

import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Pair;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TestActivity extends Activity {
	private ContentResolver mCR;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// instantiate a filter just for fun and populate it from the campaigns list
		// also create a filter that will be populated by the survey list
		final FilterControl campaignFilter = new FilterControl(this);
		final FilterControl surveyFilter = new FilterControl(this);
		final DateFilterControl dateFilter = new DateFilterControl(this);
		
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		layout.addView(campaignFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(surveyFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(dateFilter, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		campaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(String curValue) {
				Cursor surveys;
				
				if (!curValue.equalsIgnoreCase("all"))
					surveys = mCR.query(Survey.getSurveysByCampaignURN(curValue), null, null, null, null);
				else
					surveys = mCR.query(Survey.getSurveys(), null, null, null, null);

				ArrayList<Pair<String,String>> items = new ArrayList<Pair<String,String>>();
				
				while (surveys.moveToNext()) {
					items.add(
							Pair.create(
									surveys.getString(surveys.getColumnIndex(Survey.TITLE)),
									surveys.getString(surveys.getColumnIndex(Survey.CAMPAIGN_URN)) +
									":" + surveys.getString(surveys.getColumnIndex(Survey.SURVEY_ID))
									)
							);
				}
				
				surveyFilter.populate(items);
				surveyFilter.add(0, Pair.create("All Surveys", "all"));
				
				surveys.close();
			}
		});
		
		mCR = getContentResolver();
		Cursor campaigns = mCR.query(Campaign.getCampaigns(), null, Campaign.STATUS + "!=" + Campaign.STATUS_REMOTE, null, null);
		campaignFilter.populate(campaigns, Campaign.NAME, Campaign.URN);
		campaignFilter.add(0, Pair.create("All Campaigns", "all"));
	}
}
