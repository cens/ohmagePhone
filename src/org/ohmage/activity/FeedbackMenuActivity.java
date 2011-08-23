package org.ohmage.activity;

import java.util.ArrayList;

import org.ohmage.R;
import org.ohmage.feedback.visualization.FeedbackParticipationSummaryChart;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FeedbackMenuActivity extends ListActivity {
	
	private ArrayList<String> mMenus;
	final static String MENU_1 = "Participation Summary";
	final static String MENU_2 = "Response Location";
	final static String MENU_3 = "Response Chart";
	
	private String mCampaignUrn;
	private String mSurveyId;
	private String mSurveyTitle;
	private String mSurveySubmitText;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mCampaignUrn = getIntent().getStringExtra("campaign_urn");
		mSurveyId = getIntent().getStringExtra("survey_id");
		mSurveyTitle = getIntent().getStringExtra("survey_title");
		mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");

		mMenus = new ArrayList<String>();
		mMenus.add(MENU_1);
		mMenus.add(MENU_2);
		mMenus.add(MENU_3);
		
		ArrayAdapter<String> adapter;
		adapter = new ArrayAdapter<String>(this, R.layout.feedback_menu_list, mMenus);
		setListAdapter(adapter);
		getListView().setBackgroundColor(Color.WHITE);
		getListView().setCacheColorHint(Color.WHITE);
		setTitle("Feedback - Choose a summary.");
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id){
		
		Intent intent = null;
		if(mMenus.get(position).equals(MENU_1)){
			FeedbackParticipationSummaryChart chart = new FeedbackParticipationSummaryChart("Participation Summary", mCampaignUrn, mSurveyId, this);
			intent = chart.execute(this);
			if(intent == null){
				Toast.makeText(this, "No response in this campaign has been made.", Toast.LENGTH_SHORT).show();
				return;
			}
			startActivityForResult(intent, 1);
		}
		else if(mMenus.get(position).equals(MENU_2)){
			intent = new Intent(this, FeedbackMapViewActivity.class);
			intent.putExtra("campaign_urn", mCampaignUrn);
			intent.putExtra("survey_id", mSurveyId);
			startActivityForResult(intent, 1);
		}
		else if(mMenus.get(position).equals(MENU_3)){
			intent = new Intent(this, PromptListActivity.class);
			intent.putExtra("campaign_urn", mCampaignUrn);
			intent.putExtra("survey_id", mSurveyId);
			this.startActivity(intent);			
		}
		
	}
	
	private class feedbackMenuListAdapter extends BaseAdapter{
	
		@Override
		public int getCount() {
			return mMenus.size();
		}
	
		@Override
		public Object getItem(int position) {
			return mMenus.get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
