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

import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.ResponseHistoryActivity;
import org.ohmage.activity.SurveyListActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.test.CampaignContentProvider;
import org.ohmage.db.test.CampaignCursor;
import org.ohmage.db.test.NotifyingMockContentResolver;
import org.ohmage.triggers.ui.TriggerListActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.content.ContentValues;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * <p>This class contains tests for the {@link CampaignInfoActivity}</p>
 * 
 * <p>TODO: add tests which check what happens for different network connectivity states when trying to participate</p>
 * 
 * @author cketcham
 *
 */
public class CampaignInfoActivityFlowTest extends ActivityInstrumentationTestCase2<CampaignInfoActivity> {

	private static final int INDEX_IMAGE_BUTTON_OHMAGE_HOME = 0;
	private static final int INDEX_IMAGE_BUTTON_RESPONSE_HISTORY = 1;
	private static final int INDEX_IMAGE_BUTTON_TRIGGERS = 2;

	private Solo solo;
	private CampaignContentProvider provider;

	public CampaignInfoActivityFlowTest() {
		super(CampaignInfoActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityIntent(new Intent(Intent.ACTION_VIEW, Campaigns.buildCampaignUri("blah")));

		getInstrumentation().waitForIdleSync();

		NotifyingMockContentResolver fake = new NotifyingMockContentResolver(this);

		provider = new CampaignContentProvider(OhmageApplication.getContext(), DbContract.CONTENT_AUTHORITY);
		provider.addToContentResolver(fake);

		OhmageApplication.setFakeContentResolver(fake);
		solo = new Solo(getInstrumentation(), getActivity());
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

	private Campaign getBasicCampaign() {
		Campaign c = new Campaign();
		c.mStatus = Campaign.STATUS_READY;
		c.mUrn = CampaignCursor.DEFAULT_CAMPAIGN_URN;
		return c;
	}

	@Smoke
	public void testFlowHomeButtonActionBar() {
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_OHMAGE_HOME);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testFlowResponseHistoryActionBar() {
		provider.setCampaigns(getBasicCampaign());

		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_RESPONSE_HISTORY);
		solo.assertCurrentActivity("Expected Response History", ResponseHistoryActivity.class);
		assertEquals(CampaignCursor.DEFAULT_CAMPAIGN_URN, solo.getCurrentActivity().getIntent().getStringExtra(CampaignFilter.EXTRA_CAMPAIGN_URN));
		solo.goBack();
	}

	@Smoke
	public void testFlowTriggerButtonActionBar() {
		provider.setCampaigns(getBasicCampaign());

		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_TRIGGERS);
		solo.assertCurrentActivity("Expected Triggers list", TriggerListActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testViewSurveys() {
		provider.setCampaigns(getBasicCampaign());

		solo.clickOnText("View Surveys");
		solo.assertCurrentActivity("Expected Surveys list", SurveyListActivity.class);
		assertEquals(CampaignCursor.DEFAULT_CAMPAIGN_URN, solo.getCurrentActivity().getIntent().getStringExtra(CampaignFilter.EXTRA_CAMPAIGN_URN));
		solo.goBack();
	}

	@Smoke
	public void testRemove() {
		provider.setCampaigns(getBasicCampaign());

		solo.clickOnText("Remove");
		solo.clickOnText("Remove");
		solo.searchText("available");
	}

	@Smoke
	public void testParticipate() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_REMOTE;
		provider.setCampaigns(c);

		// Wait for view to be shown
		solo.searchText("Participate");

		// Override the action when the participate button is pressed to simulate the campaign xml download task started
		// TODO: verify that the button would start the xml download task?
		Button participateButton = (Button)getActivity().findViewById(R.id.campaign_info_button_particpate);
		participateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_DOWNLOADING);
				provider.update(Campaigns.CONTENT_URI, values, null, null);
			}
		});

		solo.clickOnText("Participate");
		solo.assertCurrentActivity("Expected to stay on CampaignInfoActivity", CampaignInfoActivity.class);
		assertTrue(solo.searchText("downloading"));

		// Simulate download successful
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_READY);
		provider.update(Campaigns.CONTENT_URI, values, null, null);

		assertTrue(solo.searchText("participating"));
	}

	@Smoke
	public void testCampaignPrivacyInfo() {
		Campaign c = getBasicCampaign();
		provider.setCampaigns(c);

		assertFalse(solo.searchText("The privacy of the campaign determines who can view the shared data", true));
		solo.clickOnText("privacy");
		assertTrue(solo.searchText("The privacy of the campaign determines who can view the shared data", true));
	}

	@Smoke
	public void testCampaignStatusInfo() {
		Campaign c = getBasicCampaign();
		provider.setCampaigns(c);

		assertFalse(solo.searchText("The above displays the status of the campaign.", true));
		solo.clickOnText("status");
		assertTrue(solo.searchText("The above displays the status of the campaign.", true));
	}

	@Smoke
	public void testCampaignResponsesInfo() {
		Campaign c = getBasicCampaign();
		provider.setCampaigns(c);

		assertFalse(solo.searchText("The above count is the number of responses you have submitted", true));
		solo.clickOnText("responses");
		assertTrue(solo.searchText("The above count is the number of responses you have submitted", true));
	}

	@Smoke
	public void testCampaignRemindersInfo() {
		Campaign c = getBasicCampaign();
		provider.setCampaigns(c);

		assertFalse(solo.searchText("The above count is the number of reminders configured for this campaign", true));
		solo.clickOnText("reminders");
		assertTrue(solo.searchText("The above count is the number of reminders configured for this campaign", true));
	}
}
