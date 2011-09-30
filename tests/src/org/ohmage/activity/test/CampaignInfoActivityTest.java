/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.activity.test;

import org.ohmage.R;
import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.triggers.base.TriggerDB;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

/**
 * <p>This class contains tests for the {@link CampaignInfoActivity}</p>
 * 
 * <p>There are Helper methods which are used for dealing with a Loader. I try and
 * destroy a loader in between tests, and I try and wait for the loader to finish when
 * I expect it to be loading data. There are some problems where the loader with say it
 * is done loading, but the new data wont be there. I am guessing it is because
 * there was an old loading request which happened to finish right before the new one?
 * Or I start to wait too soon and it decides it is finished since it hasn't got another
 * load request yet.. I'm not sure.</p>
 * 
 * @author cketcham
 *
 */
public class CampaignInfoActivityTest extends ActivityInstrumentationTestCase2<CampaignInfoActivity> {

	private static final String CAMPAIGN_URN = "urn:mo:chipts";

	private CampaignInfoActivity mActivity;
	private Campaign campaign;

	private View mEntityHeader;
	private ImageView mIconView;
	private TextView mHeaderText;
	private TextView mSubtext;

	private Button surveysButton;
	private Button participateButton;
	private Button removeButton;

	private TextView mErrorBox;
	private TextView mPrivacyValue;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	private TextView mTriggersValue;

	public CampaignInfoActivityTest() {
		super(CampaignInfoActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityIntent(new Intent(Intent.ACTION_VIEW, Campaigns.buildCampaignUri(CAMPAIGN_URN)));

		mActivity = getActivity();

		// Stop the loading for now since we may want to test to see what happens before any data is loaded.
		// Once we want to start loading data we can do waitForLoader()
		stopLoading();

		mEntityHeader = mActivity.findViewById(R.id.entity_header_content);
		mIconView = (ImageView) mActivity.findViewById(R.id.entity_icon);
		mHeaderText = (TextView) mActivity.findViewById(R.id.entity_header);
		mSubtext = (TextView) mActivity.findViewById(R.id.entity_header_sub1);

		surveysButton = (Button) mActivity.findViewById(R.id.campaign_info_button_surveys);
		participateButton = (Button) mActivity.findViewById(R.id.campaign_info_button_particpate);
		removeButton = (Button) mActivity.findViewById(R.id.campaign_info_button_remove);

		mErrorBox = (TextView) mActivity.findViewById(R.id.campaign_info_errorbox);
		mPrivacyValue = (TextView) mActivity.findViewById(R.id.campaign_info_privacy_value);
		mStatusValue = (TextView) mActivity.findViewById(R.id.campaign_info_status_value);
		mResponsesValue = (TextView) mActivity.findViewById(R.id.campaign_info_responses_value);
		mTriggersValue = (TextView) mActivity.findViewById(R.id.campaign_info_triggers_value);

		//		campaign = Campaign.fromCursor(getEntity()).get(0);
	}

	@Override
	protected void tearDown() throws Exception {
		getInstrumentation().waitForIdleSync();

		// We need to make sure the loader is destroyed before we start the next test
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			runTestOnUiThread(new Runnable() {

				@Override
				public void run() {
					mActivity.getSupportLoaderManager().destroyLoader(0);
					mActivity.finish();
					signal.countDown();
				}
			});
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		signal.await();
		mActivity = null;


		super.tearDown();
	}

	@SmallTest
	public void testPreconditions() {
		assertNotNull(mEntityHeader);
		assertNotNull(mIconView);
		assertNotNull(mHeaderText);
		assertNotNull(mSubtext);

		assertNotNull(surveysButton);
		assertNotNull(participateButton);
		assertNotNull(removeButton);

		assertNotNull(mErrorBox);
		assertNotNull(mPrivacyValue);
		assertNotNull(mStatusValue);
		assertNotNull(mResponsesValue);
		assertNotNull(mTriggersValue);
	}

	@SmallTest
	public void testLoadingState() {
		assertEquals(false, mEntityHeader.getVisibility() == View.VISIBLE);
		waitForLoader();
		assertEquals(true, mEntityHeader.getVisibility() == View.VISIBLE);
	}

	@SmallTest
	public void testHeaderText() {
		waitForLoader();
		assertEquals("CHIPTS (Mo)", mHeaderText.getText());
		assertEquals("urn:mo:chipts", mSubtext.getText());
	}

	@MediumTest
	public void testDeletedState() {

		waitForLoader();

		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_DELETED);
		setEntityContentValues(values);

		assertEquals("deleted on server", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStoppedState() {

		waitForLoader();

		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_STOPPED);
		setEntityContentValues(values);

		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals("stopped", mStatusValue.getText());
		assertEquals("warning: this campaign is stopped, meaning that you can no longer submit surveys for it.", mErrorBox.getText().toString());
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testCampaignStates() {

		waitForLoader();

		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_DELETED);
		setEntityContentValues(values);
		assertEquals("deleted on server", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_DOWNLOADING);
		setEntityContentValues(values);
		assertEquals("downloading...", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_INVALID_USER_ROLE);
		setEntityContentValues(values);
		assertEquals("invalid role", mStatusValue.getText());
		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_OUT_OF_DATE);
		setEntityContentValues(values);
		assertEquals("out of date", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		setEntityContentValues(values);
		assertEquals("ready", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(true, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE);
		setEntityContentValues(values);
		assertEquals("available", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(true, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(false, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_STOPPED);
		setEntityContentValues(values);
		assertEquals("stopped", mStatusValue.getText());
		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_VAGUE);
		setEntityContentValues(values);
		assertEquals("not available", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testCampaignPrivacyStates() {

		waitForLoader();

		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_PRIVACY, Campaign.PRIVACY_PRIVATE);
		setEntityContentValues(values);
		assertEquals("private", mPrivacyValue.getText());

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_PRIVACY, Campaign.PRIVACY_UNKNOWN);
		setEntityContentValues(values);
		assertEquals("unknown", mPrivacyValue.getText());

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_PRIVACY, "not real privacy state");
		setEntityContentValues(values);
		assertEquals("unknown", mPrivacyValue.getText());

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_PRIVACY, 8);
		setEntityContentValues(values);
		assertEquals("unknown", mPrivacyValue.getText());

		values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_PRIVACY, Campaign.PRIVACY_SHARED);
		setEntityContentValues(values);
		assertEquals("shared", mPrivacyValue.getText());
	}

	/**
	 * Just tests to make sure the count is what we are getting from the db. May be too tightly coupled
	 * to the db for the test to tell us much... But these aren't supposed to be testing the content
	 * provider so its probably ok.
	 */
	@MediumTest
	public void testResponseCount() {
		waitForLoader();

		Cursor responses = mActivity.getContentResolver().query(Campaigns.buildResponsesUri(CAMPAIGN_URN), null, null, null, null);
		assertEquals(responses.getCount() + " response(s) submitted", mResponsesValue.getText());
		responses.close();
	}

	/**
	 * Just tests to make sure the count is what we are getting from the db
	 */
	@MediumTest
	public void testTriggerCount() {
		waitForLoader();

		// get the number of triggers for this campaign
		TriggerDB trigDB = new TriggerDB(mActivity);
		if (trigDB.open()) {
			Cursor triggers = trigDB.getAllTriggers(CAMPAIGN_URN);
			assertEquals(triggers.getCount() + " trigger(s) configured", mTriggersValue.getText());
			triggers.close();
			trigDB.close();
		}
	}

	private void setEntityContentValues(final ContentValues values) {
		final CursorLoader loader = (CursorLoader) mActivity.onCreateLoader(0, null);
		mActivity.getContentResolver().update(loader.getUri(), values, loader.getSelection(), loader.getSelectionArgs());

		// Wait for the activity to be idle so we know its not processing other loader requests.
		getInstrumentation().waitForIdleSync();

		// Then wait for the loader
		waitForLoader();
	}

	private Cursor getEntity() {
		CursorLoader loader = (CursorLoader) mActivity.onCreateLoader(0, null);
		return mActivity.getContentResolver().query(loader.getUri(), null, loader.getSelection(), loader.getSelectionArgs(), loader.getSortOrder());
	}

	private void restartLoader() {
		mActivity.getSupportLoaderManager().restartLoader(0, null, mActivity);
	}

	private AsyncTaskLoader<Object> getDataLoader() {
		return (AsyncTaskLoader<Object>) mActivity.getSupportLoaderManager().getLoader(0);
	}

	private void waitForLoader() {
		startLoading();
		getDataLoader().waitForLoader();
	}

	private void startLoading() {
		getDataLoader().startLoading();
	}

	private void stopLoading() {
		getDataLoader().stopLoading();
	}
}
