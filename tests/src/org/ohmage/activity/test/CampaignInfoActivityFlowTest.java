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

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApplication;
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

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;

import java.util.concurrent.CountDownLatch;

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
		provider.setCampaign(getBasicCampaign());

		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_RESPONSE_HISTORY);
		solo.assertCurrentActivity("Expected Response History", ResponseHistoryActivity.class);
		assertEquals(CampaignCursor.DEFAULT_CAMPAIGN_URN, solo.getCurrentActivity().getIntent().getStringExtra(CampaignFilter.EXTRA_CAMPAIGN_URN));
		solo.goBack();
	}

	@Smoke
	public void testFlowTriggerButtonActionBar() {
		provider.setCampaign(getBasicCampaign());

		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_TRIGGERS);
		solo.assertCurrentActivity("Expected Triggers list", TriggerListActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testViewSurveys() {
		provider.setCampaign(getBasicCampaign());

		solo.clickOnText("View Surveys");
		solo.assertCurrentActivity("Expected Surveys list", SurveyListActivity.class);
		assertEquals(CampaignCursor.DEFAULT_CAMPAIGN_URN, solo.getCurrentActivity().getIntent().getStringExtra(CampaignFilter.EXTRA_CAMPAIGN_URN));
		solo.goBack();
	}

	@Smoke
	public void testRemove() {
		provider.setCampaign(getBasicCampaign());

		solo.clickOnText("Remove");
		solo.clickOnText("Remove");
		solo.searchText("available");
	}

	@Smoke
	public void testParticipate() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_REMOTE;
		provider.setCampaign(c);
		final CountDownLatch downloadWait = new CountDownLatch(1);

		// TODO: instead of mocking the ohmage api, we should mock the campaign xml download loader to just return fake data
		// The xml downloader can then be tested seperately with a mocked OhmageApi
//		OhmageApplication.setOhmageApi(new OhmageApi(OhmageApplication.getContext()) {
//			@Override
//			public CampaignReadResponse campaignRead(String serverUrl, String username, String hashedPassword, String client, String outputFormat, String campaignUrnList) {
//				CampaignReadResponse response = new CampaignReadResponse();
//				response.setResponseStatus(Result.SUCCESS, null);
//
//				JSONObject data = new JSONObject();
//				JSONObject campaignObject = new JSONObject();
//				try {
//					campaignObject.put("creation_timestamp", "0");
//					data.put(CampaignCursor.DEFAULT_CAMPAIGN_URN, campaignObject);
//				} catch (JSONException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				response.setData(data);
//				return response;
//			}
//
//			@Override
//			public CampaignXmlResponse campaignXmlRead(String serverUrl, String username, String hashedPassword, String client, String campaignUrn) {
//				CampaignXmlResponse response = new CampaignXmlResponse();
//				response.setResponseStatus(Result.SUCCESS, null);
//				try {
//					downloadWait.await();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return response;
//			}
//		});

		solo.clickOnText("Participate");
		solo.assertCurrentActivity("Expected to stay on CampaignInfoActivity", CampaignInfoActivity.class);
		assertTrue(solo.searchText("downloading"));
		downloadWait.countDown();
		assertTrue(solo.searchText("participating"));
	}
}
