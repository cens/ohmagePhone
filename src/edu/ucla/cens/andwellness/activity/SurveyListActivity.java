package edu.ucla.cens.andwellness.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Survey;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import edu.ucla.cens.mobility.glue.MobilityInterface;

public class SurveyListActivity extends ListActivity {
	
	private static final String TAG = "SurveyListActivity";
	
	/*public static final String [] SURVEYS = {	"Survey 1",
												"Survey 2",
												"Survey 3",
												"Survey 4" };*/
	
	private List<Survey> mSurveys;
	private List<Map<String,?>> data;
	private SimpleAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		String username = preferencesHelper.getUsername();
			
		//check password instead (in addition to?) the username  
		if (username.length() < 1) {
			Log.i(TAG, "no username saved, must be first run, so launch Login");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		}
		
		mSurveys = null; 
		
		try {
			mSurveys = PromptXmlParser.parseSurveys(getResources().openRawResource(R.raw.nih_all));
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//List<Map<String,?>> data = new ArrayList<Map<String,?>>();
		data = new ArrayList<Map<String,?>>();
		
		/*List<String> activeList = Arrays.asList(TriggerFramework.getActiveSurveys(this));
		
		for (int i = 0; i < mSurveys.size(); i++) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("title", mSurveys.get(i).getTitle());
			if (activeList.contains(mSurveys.get(i).getTitle())) {
				map.put("active", "active");
			} else {
				map.put("active", "inactive");
			}
			data.add(map);
		}*/
		
		adapter = new SimpleAdapter(this, data, R.layout.survey_list_item, new String [] {"title", "active"}, new int [] {R.id.survey_title, R.id.survey_active});
		
		//ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.survey_list_item, R.id.survey_title, SURVEYS);
		
		setListAdapter(adapter);
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		data.clear();
		
		List<String> activeList = Arrays.asList(TriggerFramework.getActiveSurveys(this));
		
		for (int i = 0; i < mSurveys.size(); i++) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("title", mSurveys.get(i).getTitle());
			if (activeList.contains(mSurveys.get(i).getTitle())) {
				map.put("active", "active");
			} else {
				map.put("active", "inactive");
			}
			data.add(map);
		}
		
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent intent = new Intent(this, SurveyActivity.class);
		intent.putExtra("survey_id", mSurveys.get(position).getId());
		intent.putExtra("survey_title", mSurveys.get(position).getTitle());
		intent.putExtra("survey_submit_text", mSurveys.get(position).getSubmitText());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.trigger_settings:
			List<String> surveyTitles = new ArrayList<String>();
			for(Survey survey: mSurveys) {
				surveyTitles.add(survey.getTitle());
			}
			TriggerFramework.launchTriggersActivity(this, surveyTitles.toArray(new String[surveyTitles.size()]));
			return true;
		case R.id.mobility_settings:
			MobilityInterface.showMobilityOptions(this);
			//Toast.makeText(this, "Mobility is not available.", Toast.LENGTH_SHORT).show();
			return true;
			
		case R.id.status:
			//WakefulIntentService.sendWakefulWork(this, UploadService.class);
			Intent intent = new Intent(this, StatusActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
