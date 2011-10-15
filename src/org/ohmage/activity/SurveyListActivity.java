package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.fragments.SurveyListFragment;
import org.ohmage.fragments.SurveyListFragment.OnSurveyActionListener;
import org.ohmage.ui.CampaignFilterActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SurveyListActivity extends CampaignFilterActivity implements OnSurveyActionListener {

	static final String TAG = "SurveyListActivity";

	private Button mAllButton;
	private Button mPendingButton;
	private boolean mShowPending = false;
	
	public static final String EXTRA_SHOW_PENDING = "extra_show_pending";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mShowPending = getIntent().getBooleanExtra(EXTRA_SHOW_PENDING, false);
		
		setContentView(R.layout.survey_list);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            SurveyListFragment newFragment = new SurveyListFragment();
            newFragment.setShowPending(mShowPending);
            newFragment.setArguments(intentToFragmentArguments(getIntent()));
            ft.add(R.id.surveys, newFragment);
            ft.commit();
        } else {
			((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setShowPending(mShowPending);
        }

		mAllButton = (Button) findViewById(R.id.all_surveys_button);
		mPendingButton = (Button) findViewById(R.id.pending_surveys_button);

		mAllButton.setOnClickListener(mPendingListener);
		mPendingButton.setOnClickListener(mPendingListener);

		setPendingButtons();
	}

	View.OnClickListener mPendingListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mShowPending = v.getId() == R.id.pending_surveys_button;
			((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setShowPending(mShowPending);
			setPendingButtons();
		}
	};

	@Override
	protected void onCampaignFilterChanged(String filter) {
		((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setCampaignUrn(filter);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		super.onLoadFinished(loader, data);
		ensureButtons();
	}

	private void ensureButtons() {
		mAllButton.setVisibility(View.VISIBLE);
		mPendingButton.setVisibility(View.VISIBLE);
	}

	private void setPendingButtons() {
		mAllButton.setBackgroundResource(mShowPending ? R.drawable.tab_bg_unselected : R.drawable.tab_bg_selected);
		mPendingButton.setBackgroundResource(mShowPending ? R.drawable.tab_bg_selected : R.drawable.tab_bg_unselected);
	}

	@Override
	public void onSurveyActionView(Uri surveyUri) {
		Intent i = new Intent(this, SurveyInfoActivity.class);
		i.setData(surveyUri);
		startActivity(i);
	}

	@Override
	public void onSurveyActionStart(Uri surveyUri) {
		Cursor cursor = getContentResolver().query(surveyUri, null, null, null, null);
		if (cursor.moveToFirst()) {
			Intent intent = new Intent(this, SurveyActivity.class);
			intent.putExtra("campaign_urn", cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN)));
			intent.putExtra("survey_id", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID)));
			intent.putExtra("survey_title", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_TITLE)));
			intent.putExtra("survey_submit_text", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_SUBMIT_TEXT)));
			startActivity(intent);
		} else {
			Toast.makeText(this, "onSurveyActionStart: Error: Empty cursor returned.", Toast.LENGTH_SHORT).show();
		}
		cursor.close();
	}

	@Override
	public void onSurveyActionUnavailable(Uri surveyUri) {
		Toast.makeText(this, "This survey can only be taken when triggered.", Toast.LENGTH_SHORT).show();
	}

}
