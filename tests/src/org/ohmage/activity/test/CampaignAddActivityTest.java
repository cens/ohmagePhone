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

import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContentResolver;
import android.test.suitebuilder.annotation.Smoke;

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.OhmageApplication;
import org.ohmage.activity.CampaignAddActivity;
import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.test.CampaignContentProvider;

/**
 * <p>This class contains tests for the {@link CampaignAddActivity}</p>
 * 
 * @author cketcham
 *
 */
public class CampaignAddActivityTest extends ActivityInstrumentationTestCase2<CampaignAddActivity> {

	private Solo solo;
	private CampaignContentProvider provider;

	public CampaignAddActivityTest() {
		super(CampaignAddActivity.class);
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
		solo.assertCurrentActivity("expected response list", CampaignAddActivity.class);
	}

	@Smoke
	public void testHomeButton() {
		solo.clickOnImageButton(0);
		solo.waitForActivity("DashboardActivity");
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
		solo.goBack();
	}

	public void testCampaignsWithCorrectStateAreDisplayed() {
		setDefaultCampaigns();

		// Any campaigns which are remote or downloading are shown
		assertTrue(provider.getLastSelection().contains("campaign_status = " + Campaign.STATUS_REMOTE));
		assertTrue(provider.getLastSelection().contains("OR"));
		assertTrue(provider.getLastSelection().contains("campaign_status = " + Campaign.STATUS_DOWNLOADING));
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

		assertTrue(solo.searchText("No campaigns available at this time\\.", true));
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
		c.mStatus = Campaign.STATUS_REMOTE;
		return c;
	}
}