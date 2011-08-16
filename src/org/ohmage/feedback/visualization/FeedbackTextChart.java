package org.ohmage.feedback.visualization;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.ohmage.R;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class FeedbackTextChart extends ListActivity {
	private static final String TAG = "FeedbackTextListActivity";
	private String mCampaignUrn;
	private String mSurveyID;
	private String mPromptID;
	private ArrayList<String> arTextAnswers;
	
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mCampaignUrn = getIntent().getStringExtra("campaign_urn");
		mSurveyID = getIntent().getStringExtra("survey_id");
		mPromptID = getIntent().getStringExtra("prompt_id");
		
		ContentResolver cr = this.getContentResolver();
		Uri queryUri = DbContract.getBaseUri().buildUpon()
			.appendPath(mCampaignUrn)
			.appendPath(mSurveyID)
			.appendPath("responses")
			.appendPath("prompts") 
			.appendPath(mPromptID)
			.build();
		
		String[] projection = new String[] { Response.TIME, PromptResponse.PROMPT_VALUE };
		
		Cursor cursor = cr.query(queryUri, projection, null, null, null);
		if(cursor.getCount() == 0){
			Toast.makeText(this, "No response has been made.", Toast.LENGTH_SHORT);
			finish();
		}
		
		arTextAnswers = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		while(cursor.moveToNext()){
			Date date = new Date(cursor.getLong(0));
			arTextAnswers.add("Date: " + sdf.format(date).toString() + "\n" + "Answer: " + cursor.getString(1));
		}
		cursor.close();
		
		ArrayAdapter<String> adapter;
		adapter = new ArrayAdapter<String>(this, R.layout.feedback_text_chart_list, arTextAnswers);
		setListAdapter(adapter);
		
		getListView().setCacheColorHint(Color.WHITE);
		getListView().setBackgroundColor(Color.WHITE);
	}
}
