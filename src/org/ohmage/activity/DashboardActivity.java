package org.ohmage.activity;

import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class DashboardActivity extends Activity {
	private static final String TAG = "DashboardActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// first, ensure that they're logged in
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		// if they're not, redirect them to the login screen
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launching LoginActivity");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		}
		
		setContentView(R.layout.dashboard_activity);
		
		// nab a reference to the action bar so we can set it up a little bit
		// ActionBarControl actionBar = (ActionBarControl)findViewById(R.id.action_bar);

		// gather up all the buttons and tie them to the dashboard button listener
		// you'll specify what the buttons do in DashboardButtonListener rather than here
		Button campaignBtn = (Button) findViewById(R.id.dash_campaigns_btn);
		Button surveysBtn = (Button) findViewById(R.id.dash_surveys_btn);
		Button feedbackBtn = (Button) findViewById(R.id.dash_feedback_btn);
		Button uploadQueueBtn = (Button) findViewById(R.id.dash_uploadqueue_btn);
		Button profileBtn = (Button) findViewById(R.id.dash_profile_btn);
		Button settingsBtn = (Button) findViewById(R.id.dash_settings_btn);
		
		DashboardButtonListener buttonListener = new DashboardButtonListener();
		
		campaignBtn.setOnClickListener(buttonListener);
		surveysBtn.setOnClickListener(buttonListener);
		feedbackBtn.setOnClickListener(buttonListener);
		uploadQueueBtn.setOnClickListener(buttonListener);
		profileBtn.setOnClickListener(buttonListener);
		settingsBtn.setOnClickListener(buttonListener);
	}
	
	protected class DashboardButtonListener implements OnClickListener {		
		@Override
		public void onClick(View v) {
			Context c = v.getContext();
			
			switch (v.getId()) {
				case R.id.dash_campaigns_btn:
					startActivity(new Intent(c, CampaignListActivity.class));
					break;
					
				case R.id.dash_surveys_btn:
					break;
					
				case R.id.dash_feedback_btn:
					break;
					
				case R.id.dash_uploadqueue_btn:
					break;
					
				case R.id.dash_profile_btn:
					startActivity(new Intent(c, StatusActivity.class));
					break;
					
				case R.id.dash_settings_btn:
					// print out the version of sqlite we're using for debugging purposes
					Cursor cursor = SQLiteDatabase.openOrCreateDatabase(":memory:", null).rawQuery("select sqlite_version() AS sqlite_version", null);
					String sqliteVersion = "";
					while(cursor.moveToNext()){
					   sqliteVersion += cursor.getString(0);
					}
					
					Toast.makeText(c, "SQLite Version: " + sqliteVersion, Toast.LENGTH_SHORT).show();
					
					break;
			}
		}
	}
}
