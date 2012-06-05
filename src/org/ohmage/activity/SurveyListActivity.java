package org.ohmage.activity;

import org.ohmage.ConfigHelper;
import org.ohmage.R;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.fragments.SurveyListFragment;
import org.ohmage.fragments.SurveyListFragment.OnSurveyActionListener;
import org.ohmage.ui.CampaignFilterActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilterable;
import org.ohmage.ui.TabsAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;
import android.widget.Toast;

public class SurveyListActivity extends CampaignFilterActivity implements OnSurveyActionListener {

	static final String TAG = "SurveyListActivity";
	public static final String EXTRA_SHOW_PENDING = "extra_show_pending";
	private static final int DIALOG_ERROR_ID = 0;

	TabHost mTabHost;
	ViewPager  mViewPager;
	TabsAdapter mTabsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.survey_list_layout);

		if(ConfigHelper.isSingleCampaignMode())
			setActionBarShadowVisibility(false);

		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mViewPager = (ViewPager)findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

		Bundle args = intentToFragmentArguments(getIntent());
		args.putBoolean(SurveyListFragment.KEY_PENDING, false);
		mTabsAdapter.addTab(getString(R.string.surveys_all), SurveyListFragment.class, args);

		args = intentToFragmentArguments(getIntent());
		args.putBoolean(SurveyListFragment.KEY_PENDING, true);
		mTabsAdapter.addTab(getString(R.string.surveys_pending), SurveyListFragment.class, args);

		boolean showPending = getIntent().getBooleanExtra(EXTRA_SHOW_PENDING, false);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		} else {
			mTabHost.setCurrentTab(showPending ? 1 : 0);
		}
	}

	@Override
	protected void onCampaignFilterChanged(String filter) {
		super.onCampaignFilterChanged(filter);
		for(int i=0;i<mTabsAdapter.getCount();i++)
			((CampaignFilterable) mTabsAdapter.getItem(i)).setCampaignUrn(filter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
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
			Toast.makeText(this, R.string.survey_list_invalid_survey, Toast.LENGTH_SHORT).show();
		}
		cursor.close();
	}

	@Override
	public void onSurveyActionUnavailable(Uri surveyUri) {
		Toast.makeText(this, R.string.survey_list_must_trigger, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSurveyActionError(Uri surveyUri, int status) {
		showDialog(DIALOG_ERROR_ID);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
	 */
	@Override
	protected Dialog onCreateDialog(final int id, Bundle args) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
			case DIALOG_ERROR_ID:
				builder.setMessage(R.string.survey_list_campaign_error);
				break;
		}

		builder.setCancelable(true).setNegativeButton(R.string.ok, null);


		return builder.create();
	}
}
