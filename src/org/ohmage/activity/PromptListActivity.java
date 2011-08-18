package org.ohmage.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ohmage.CampaignXmlHelper;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.feedback.visualization.FeedbackTextChart;
import org.ohmage.feedback.visualization.FeedbackTimeLineChart;
import org.ohmage.feedback.visualization.FeedbackTimeScatterChart;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import org.ohmage.prompt.multichoice.MultiChoicePrompt;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import org.ohmage.prompt.number.NumberPrompt;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.ohmage.prompt.remoteactivity.RemoteActivityPrompt;
import org.ohmage.prompt.singlechoice.SingleChoicePrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import org.ohmage.prompt.text.TextPrompt;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import edu.ucla.cens.systemlog.Log;

public class PromptListActivity extends ListActivity{
	private static final String TAG = "PromptListActivity";
	
	private List<Prompt> mPrompts;
	private String mCampaignUrn;
	private String mSurveyId;
	private String mSurveyTitle;
	private String mSurveySubmitText;
	private ArrayList<PromptListElement> arPromptsTitle;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
        mCampaignUrn = getIntent().getStringExtra("campaign_urn");
        mSurveyId = getIntent().getStringExtra("survey_id");
        mSurveyTitle = getIntent().getStringExtra("survey_title");
        mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");

        try {
			mPrompts = PromptXmlParser.parsePrompts(CampaignXmlHelper.loadCampaignXmlFromDb(this, mCampaignUrn), mSurveyId);
			Log.i(TAG, "Parsed XML: " + mPrompts.toString());
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		}
		
		arPromptsTitle = new ArrayList<PromptListElement>();
		Iterator<Prompt> ite = mPrompts.iterator();
		
		while(ite.hasNext()){
			AbstractPrompt prompt = (AbstractPrompt)ite.next();
			arPromptsTitle.add(new PromptListElement(prompt.getPromptText(), prompt.getId()));
		}
		
		ArrayAdapter<PromptListElement> adapter;
		adapter = new ArrayAdapter<PromptListElement>(this, R.layout.prompt_list, arPromptsTitle);
		setListAdapter(adapter);
		getListView().setBackgroundColor(Color.WHITE);
		getListView().setCacheColorHint(Color.WHITE);
		
		setTitle("Response Chart Summary - Choose a prompt.");
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id){
		
		Intent intent = null;
		PromptListElement curPrompt = arPromptsTitle.get(position);
		
		if (mPrompts.get(position) instanceof SingleChoicePrompt) {
			FeedbackTimeLineChart chart = new FeedbackTimeLineChart(curPrompt.getTitle(), mCampaignUrn, mSurveyId, curPrompt.getID(), mPrompts);
			intent = chart.execute(this); //If there is no response, it returns NULL
			if(intent == null){
				Toast.makeText(this, "No response has been made.", Toast.LENGTH_SHORT).show();	
			}
		} else if (mPrompts.get(position) instanceof MultiChoicePrompt) {
			Toast.makeText(this, "MultiChoicePrompt type is not supported yet.", Toast.LENGTH_SHORT).show();
		} else if (mPrompts.get(position) instanceof MultiChoiceCustomPrompt) {
			Toast.makeText(this, "MultiChoiceCustomPrompt type is not supported yet.", Toast.LENGTH_SHORT).show();
		} else if (mPrompts.get(position) instanceof SingleChoiceCustomPrompt) {
			Toast.makeText(this, "SingleChoiceCustomPrompt type is not supported yet.", Toast.LENGTH_SHORT).show();
		} else if (mPrompts.get(position) instanceof NumberPrompt) {
			FeedbackTimeLineChart chart = new FeedbackTimeLineChart(curPrompt.getTitle(), mCampaignUrn, mSurveyId, curPrompt.getID(), mPrompts);
			intent = chart.execute(this);
			if(intent == null){
				Toast.makeText(this, "No response has been made.", Toast.LENGTH_SHORT).show();	
			}
		} else if (mPrompts.get(position) instanceof HoursBeforeNowPrompt) {
			Toast.makeText(this, "HoursBeforeNowPrompt type is not supported yet.", Toast.LENGTH_SHORT).show();
		} else if (mPrompts.get(position) instanceof TextPrompt) {
			intent = new Intent(this, FeedbackTextChart.class);
			intent.putExtra("campaign_urn", mCampaignUrn);
			intent.putExtra("survey_id", mSurveyId);
			intent.putExtra("prompt_id", curPrompt.getID());
			intent.putExtra("prompt_title", curPrompt.getTitle());
		} else if (mPrompts.get(position) instanceof PhotoPrompt) {
			Toast.makeText(this, "PhotoPrompt type is not supported yet.", Toast.LENGTH_SHORT).show();
		} else if (mPrompts.get(position) instanceof RemoteActivityPrompt){
			FeedbackTimeLineChart chart = new FeedbackTimeLineChart(curPrompt.getTitle(), mCampaignUrn, mSurveyId, curPrompt.getID(), mPrompts);
			intent = chart.execute(this);
			if(intent == null){
				Toast.makeText(this, "No response has been made.", Toast.LENGTH_SHORT).show();	
			}			
		}
		else{
			Toast.makeText(this, "It is not supported yet.", Toast.LENGTH_SHORT).show();
		}
		
		if(intent != null){
			startActivity(intent);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.prompt_list_menu, menu);
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.mapview:
			intent = new Intent(this, FeedbackMapViewActivity.class);
			intent.putExtra("campaign_urn", mCampaignUrn);
			intent.putExtra("survey_id", mSurveyId);
			startActivityForResult(intent, 1);
			return true;
		case R.id.participationstat:
			FeedbackTimeScatterChart chart = new FeedbackTimeScatterChart("Participation Summary", mCampaignUrn, mSurveyId, this);
			intent = chart.execute(this);		
			startActivityForResult(intent, 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// utility class to store the ID along with the title of the prompts
	private class PromptListElement {
		private String mTitle;
		private String mID;

		public PromptListElement(String title, String id) {
			setTitle(title);
			setID(id);
		}
		
		private void setTitle(String mTitle) {
			this.mTitle = mTitle;
		}

		private String getTitle() {
			return mTitle;
		}

		private void setID(String mID) {
			this.mID = mID;
		}

		private String getID() {
			return mID;
		}

		public String toString() {
			return getTitle();
		}
	}
	
	private class promptListAdapter extends BaseAdapter{
		Context mContext;
		LayoutInflater mInflater;
		ArrayList<PromptListElement> arPrompts;
		int mLayout;

		public promptListAdapter(Context context, int alayout, ArrayList<PromptListElement> aarPrompts){
			mContext = context;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			arPrompts = aarPrompts;
			mLayout = alayout;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return arPrompts.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return arPrompts.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}