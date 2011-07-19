package edu.ucla.cens.andwellness.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.achartengine.chart.LineChart;
import org.achartengine.chart.ScatterChart;
import org.achartengine.chartdemo.demo.chart.AverageTemperatureChart;
import org.achartengine.chartdemo.demo.chart.BudgetDoughnutChart;
import org.achartengine.chartdemo.demo.chart.CombinedTemperatureChart;
import org.achartengine.chartdemo.demo.chart.MultipleTemperatureChart;
import org.achartengine.chartdemo.demo.chart.ProjectStatusBubbleChart;
import org.achartengine.chartdemo.demo.chart.SalesComparisonChart;
import org.achartengine.chartdemo.demo.chart.SensorValuesChart;
import org.achartengine.chartdemo.demo.chart.TrigonometricFunctionsChart;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.ucla.cens.andwellness.CampaignXmlHelper;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.Survey;
import edu.ucla.cens.andwellness.feedback.visualization.AbstractChart;
import edu.ucla.cens.andwellness.feedback.visualization.FeedbackLineChart;
import edu.ucla.cens.andwellness.feedback.visualization.FeedbackTimeChart;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.text.TextPrompt;
import edu.ucla.cens.andwellness.triggers.glue.LocationTriggerAPI;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
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
		adapter = new ArrayAdapter<PromptListElement>(this, android.R.layout.simple_list_item_1, arPromptsTitle);
		setListAdapter(adapter);
		
		setTitle("Feedback");
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id){
		
		Intent intent = null;
		PromptListElement curPrompt = arPromptsTitle.get(position);
		
		if (mPrompts.get(position) instanceof SingleChoicePrompt) {
			FeedbackTimeChart chart = new FeedbackTimeChart(curPrompt.getTitle(), mCampaignUrn, mSurveyId, curPrompt.getID());
			intent = chart.execute(this);
		} else if (mPrompts.get(position) instanceof MultiChoicePrompt) {
			BudgetDoughnutChart chart = new BudgetDoughnutChart();
			intent = chart.execute(this);			
		} else if (mPrompts.get(position) instanceof MultiChoiceCustomPrompt) {
			CombinedTemperatureChart chart = new CombinedTemperatureChart();
			intent = chart.execute(this);		
		} else if (mPrompts.get(position) instanceof SingleChoiceCustomPrompt) {
			ProjectStatusBubbleChart chart = new ProjectStatusBubbleChart();
			intent = chart.execute(this);
		} else if (mPrompts.get(position) instanceof NumberPrompt) {
			FeedbackTimeChart chart = new FeedbackTimeChart(curPrompt.getTitle(), mCampaignUrn, mSurveyId, curPrompt.getID());
			intent = chart.execute(this);			
		} else if (mPrompts.get(position) instanceof HoursBeforeNowPrompt) {
			SalesComparisonChart chart = new SalesComparisonChart();
			intent = chart.execute(this);
		} else if (mPrompts.get(position) instanceof TextPrompt) {
			SensorValuesChart chart = new SensorValuesChart();
			intent = chart.execute(this);
		} else if (mPrompts.get(position) instanceof PhotoPrompt) {
			TrigonometricFunctionsChart chart = new TrigonometricFunctionsChart();
			intent = chart.execute(this);
		} else{
			TrigonometricFunctionsChart chart = new TrigonometricFunctionsChart();
			intent = chart.execute(this);			
		}
		
		startActivity(intent);
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
			FeedbackTimeChart chart = new FeedbackTimeChart("Participation Stats", mCampaignUrn, mSurveyId, null);
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
}