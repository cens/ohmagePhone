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
import org.ohmage.activity.CampaignAddActivity;
import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.activity.CampaignListActivity;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.SurveyListActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.test.CampaignContentProvider;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContentResolver;
import android.test.suitebuilder.annotation.Smoke;

/**
 * <p>This class contains tests for the {@link CampaignListActivity}</p>
 * 
 * @author cketcham
 *
 */
public class CampaignListActivityTest extends ActivityInstrumentationTestCase2<CampaignListActivity> {

	private Solo solo;
	private CampaignContentProvider provider;

	public CampaignListActivityTest() {
		super(CampaignListActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		getInstrumentation().waitForIdleSync();

		MockContentResolver fake = new MockContentResolver();
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

	public void testPreconditions() {
		solo.assertCurrentActivity("expected response list", CampaignListActivity.class);
	}

	@Smoke
	public void testHomeButton() {
		solo.clickOnImageButton(0);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testAddButton() {
		solo.clickOnImageButton(1);
		solo.assertCurrentActivity("Expected AddCampaign", CampaignAddActivity.class);
		solo.goBack();
	}

	public void testCampaignsWithCorrectStateAreDisplayed() {
		setDefaultCampaigns();

		// Any campaigns which aren't remote or downloading are shown
		assertTrue(provider.getLastSelection().contains("campaign_status != " + Campaign.STATUS_REMOTE));
		assertTrue(provider.getLastSelection().contains("AND"));
		assertTrue(provider.getLastSelection().contains("campaign_status != " + Campaign.STATUS_DOWNLOADING));
	}

	public void testListItemInfoIsCorrect() {
		setDefaultCampaigns();

		assertTrue(solo.searchText(provider.getCampaigns()[0].mName, true));
		assertTrue(solo.searchText(provider.getCampaigns()[0].mUrn, true));
	}

	public void testClickListItem() {
		setDefaultCampaigns();

		solo.clickOnText(provider.getCampaigns()[0].mName);
		solo.assertCurrentActivity("Expected Campaign Info", CampaignInfoActivity.class);
		assertTrue(solo.getCurrentActivity().getIntent().getData().getPath().endsWith(provider.getCampaigns()[0].mUrn));
		solo.goBack();
	}

	public void testEmptyList() {
		// set no campaigns
		provider.setCampaigns(null);

		assertTrue(solo.searchText("You are not participating in any campaigns", true));
		assertTrue(solo.searchText("Hit the \\+ icon on the top right to view and download available campaigns\\.", true));
	}

	public void testCampaignStateInvalidUserRole() {
		Campaign campaign = getCampaign(0);
		campaign.mStatus = Campaign.STATUS_INVALID_USER_ROLE;
		provider.setCampaigns(campaign);

		solo.clickOnImage(7);
		assertTrue(solo.searchText("Invalid user role.", true));
	}

	public void testCampaignStateDeleted() {
		Campaign campaign = getCampaign(0);
		campaign.mStatus = Campaign.STATUS_NO_EXIST;
		provider.setCampaigns(campaign);

		solo.clickOnImage(7);
		assertTrue(solo.searchText("This campaign no longer exists", true));
	}

	public void testCampaignStateOutOfDate() {
		Campaign campaign = getCampaign(0);
		campaign.mStatus = Campaign.STATUS_OUT_OF_DATE;
		provider.setCampaigns(campaign);

		solo.clickOnImage(7);
		assertTrue(solo.searchText("This campaign is out of date", true));
	}

	public void testCampaignStateStopped() {
		Campaign campaign = getCampaign(0);
		campaign.mStatus = Campaign.STATUS_STOPPED;
		provider.setCampaigns(campaign);

		solo.clickOnImage(7);
		assertTrue(solo.searchText("This campaign is stopped.", true));
	}

	public void testCampaignStateReady() {
		Campaign campaign = getCampaign(0);
		campaign.mStatus = Campaign.STATUS_READY;
		provider.setCampaigns(campaign);

		solo.clickOnImage(7);
		solo.assertCurrentActivity("Expected Survey List", SurveyListActivity.class);
		assertEquals(campaign.mUrn, solo.getCurrentActivity().getIntent().getExtras().getString(CampaignFilter.EXTRA_CAMPAIGN_URN));
		solo.goBack();
	}

	private void setDefaultCampaigns() {
		Campaign[] campaigns = new Campaign[4];
		for(int i=0; i< campaigns.length; i++) {
			campaigns[i] = getCampaign(i);
		}
		provider.setCampaigns(campaigns);
	}

	private Campaign getCampaign(int index) {
		Campaign c = new Campaign();
		c.mName = "Campaign #" + index;
		c.mUrn = "urn:campaign:" + index;
		return c;
	}
}