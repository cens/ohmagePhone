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

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.ResponseHistoryActivity;
import org.ohmage.activity.SurveyListActivity;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.test.helper.FilterBarHelper;
import org.ohmage.test.helper.LoaderHelper;
import org.ohmage.triggers.ui.TriggerListActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;

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
public class CampaignInfoActivityFlowTest extends ActivityInstrumentationTestCase2<CampaignInfoActivity> {

	private static final String CAMPAIGN_URN = "urn:andwellness:moms";
	private static final String CAMPAIGN_NAME = "Moms";

	private static final int INDEX_IMAGE_BUTTON_OHMAGE_HOME = 0;
	private static final int INDEX_IMAGE_BUTTON_RESPONSE_HISTORY = 1;
	private static final int INDEX_IMAGE_BUTTON_TRIGGERS = 2;

	private Solo solo;
	private LoaderHelper mLoaderHelper;

	public CampaignInfoActivityFlowTest() {
		super(CampaignInfoActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityIntent(new Intent(Intent.ACTION_VIEW, Campaigns.buildCampaignUri(CAMPAIGN_URN)));

		solo = new Solo(getInstrumentation(), getActivity());
		mLoaderHelper = new LoaderHelper(getActivity(), this);
	}

	@Override
	protected void tearDown() throws Exception{
		try {
			solo.finalize();
		} catch (Throwable e) { 
			e.printStackTrace();
		}
		getActivity().finish(); 
		super.tearDown();
	}
	
	public void testPreconditions() {
		Cursor entity = mLoaderHelper.getEntity();
		assertTrue("We should be getting one item from the db", entity.getCount() == 1);
	}

	@Smoke
	public void testFlowHomeButtonActionBar() {
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_OHMAGE_HOME);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
	}

	@Smoke
	public void testFlowResponseHistoryActionBar() {
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		mLoaderHelper.setEntityContentValues(values);
		
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_RESPONSE_HISTORY);
		solo.assertCurrentActivity("Expected Response History", ResponseHistoryActivity.class);
		solo.searchText(CAMPAIGN_NAME, true);
		solo.searchText(FilterBarHelper.ALL_SURVEYS, true);
		solo.goBack();
	}

	@Smoke
	public void testFlowTriggerButtonActionBar() {
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		mLoaderHelper.setEntityContentValues(values);
		
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_TRIGGERS);
		solo.assertCurrentActivity("Expected Triggers list", TriggerListActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testViewSurveys() {
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		mLoaderHelper.setEntityContentValues(values);
		solo.clickOnText("View Surveys");
		solo.assertCurrentActivity("Expected Surveys list", SurveyListActivity.class);
		solo.searchText(CAMPAIGN_NAME, true);
		solo.goBack();
	}

	@Smoke
	public void testRemove() {
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		mLoaderHelper.setEntityContentValues(values);
		solo.clickOnText("Remove");
		solo.clickOnText("Yes");
		solo.assertCurrentActivity("Expected to stay on CampaignInfoActivity", CampaignInfoActivity.class);
		solo.searchText("available");
	}

	@Smoke
	public void testParticipate() {
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE );
		mLoaderHelper.setEntityContentValues(values);
		solo.clickOnText("Participate");
		solo.assertCurrentActivity("Expected to stay on CampaignInfoActivity", CampaignInfoActivity.class);
		solo.searchText("ready");
	}
}
