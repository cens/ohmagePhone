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
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.ResponseInfoActivity;
import org.ohmage.activity.ResponseListActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.db.Models.Survey;
import org.ohmage.db.test.CampaignCursor;
import org.ohmage.db.test.DelegatingMockContentProvider;
import org.ohmage.db.test.EmptyMockCursor;
import org.ohmage.db.test.OhmageUriMatcher;
import org.ohmage.db.test.ResponseCursor;
import org.ohmage.db.test.SurveyCursor;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContentResolver;
import android.text.format.DateUtils;

import java.util.Calendar;

/**
 * <p>This class contains tests for the {@link ResponseListActivity}</p>
 *
 * <h2>The data passed by the content provider</h2>
 * <p>for campaigns is always 4 Campaigns with name=Campaign #X and urn=urn:campaign:X</p>
 * 
 * for surveys
 * <ul>
 * 	<li>4 Surveys with title=Survey #X and id=Survey #X for all surveys except for Campaign #1</li>
 *  <li>4 Surveys with title=Campaign 1 S#X for all surveys for Campaign #1</li>
 * </ul>
 * 
 * for responses
 * <ul>
 * 	<li>8 responses with the first response having no location</li>
 *  <li>4 responses with the time of today, yesterday, the day before that and the day before that for Campaign #2</li>
 *  <li>2 responses with the time of today, and yesterday for Campaign #2 with Survey #2</li>
 *  <li>0 responses for Campaign #3</li>
 * </ul>
 * 
 * @author cketcham
 *
 */
public class ResponseListTest extends ActivityInstrumentationTestCase2<ResponseListActivity> {

	private Solo solo;
	private DelegatingMockContentProvider provider;
	private final Calendar today = Calendar.getInstance();
	private String mLastResponseSelection;

	Campaign[] campaigns = new Campaign[4];
	{
		for(int i=0; i< campaigns.length; i++) {
			campaigns[i] = new Campaign();
			campaigns[i].mName = "Campaign #" + i;
			campaigns[i].mUrn = "urn:campaign:" + i;
		}
	}

	Survey[] surveys = new Survey[4];
	{
		for(int i=0; i< surveys.length; i++) {
			surveys[i] = new Survey();
			surveys[i].mTitle = "Survey #" + i;
			surveys[i].mSurveyID = "Survey #" + i;
		}
	}

	/** Surveys specifically for Campaign #1 */
	Survey[] surveys1 = new Survey[4];
	{
		for(int i=0; i< surveys.length; i++) {
			surveys1[i] = new Survey();
			surveys1[i].mTitle = "Campaign 1 S#" + i;
		}
	}

	Response[] responses = new Response[12];
	{
		for(int i=0; i< responses.length; i++) {
			responses[i] = new Response();
			responses[i].time = today.getTimeInMillis() - DateUtils.DAY_IN_MILLIS * i;
			responses[i].status = statuses[i%statuses.length];
		}
	}

	static int[] statuses = new int[] {
		Response.STATUS_DOWNLOADED,
		Response.STATUS_QUEUED,
		Response.STATUS_STANDBY,
		Response.STATUS_UPLOADED,
		Response.STATUS_UPLOADING,
		Response.STATUS_WAITING_FOR_LOCATION,
		Response.STATUS_ERROR_AUTHENTICATION,
		Response.STATUS_ERROR_CAMPAIGN_NO_EXIST,
		Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE,
		Response.STATUS_ERROR_CAMPAIGN_STOPPED,
		Response.STATUS_ERROR_HTTP,
		Response.STATUS_ERROR_INVALID_USER_ROLE,
		Response.STATUS_ERROR_OTHER,
	};

	/** responses specifically for Campaign #2 */
	Response[] responses2 = new Response[2];
	{
		for(int i=0; i< responses2.length; i++) {
			responses2[i] = new Response();
			responses2[i].time = Calendar.getInstance().getTimeInMillis() - DateUtils.DAY_IN_MILLIS * i;
		}
	}

	/** responses specifically for Survey #2 */
	Response[] responses4 = new Response[4];
	{
		for(int i=0; i< responses4.length; i++) {
			responses4[i] = new Response();
			responses4[i].time = Calendar.getInstance().getTimeInMillis() - DateUtils.DAY_IN_MILLIS * i;
		}
	}

	public ResponseListTest() {
		super(ResponseListActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		getInstrumentation().waitForIdleSync();

		MockContentResolver fake = new MockContentResolver();
		provider = new DelegatingMockContentProvider(OhmageApplication.getContext(), DbContract.CONTENT_AUTHORITY)  {

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				switch(OhmageUriMatcher.getMatcher().match(uri)) {
					case OhmageUriMatcher.CAMPAIGNS:
						return new CampaignCursor(projection, campaigns);
					case OhmageUriMatcher.CAMPAIGN_SURVEYS:
						if(Campaigns.getCampaignUrn(uri).equals("urn:campaign:1"))
							return new SurveyCursor(projection, surveys1);
						return new SurveyCursor(projection, surveys);
					case OhmageUriMatcher.RESPONSES:
						mLastResponseSelection = selection;
						if(selectionArgs != null && selectionArgs.length > 0) {
							if(selectionArgs[0].equals("urn:campaign:2")) {
								if(selectionArgs.length > 1 && selectionArgs[1].equals("Survey #2"))
									return new ResponseCursor(projection, responses2);
								return new ResponseCursor(projection, responses4);
							} else if(selectionArgs[0].contains("urn:campaign:3")) {
								return new EmptyMockCursor();
							}
						}
						return new ResponseCursor(projection, responses);
					default:
						return new EmptyMockCursor();
				}
			}
		};
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
		solo.assertCurrentActivity("expected response list", ResponseListActivity.class);
	}

	public void testHomeButton() {
		solo.clickOnImageButton(0);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
		solo.goBack();
	}

	public void testResponsesWithCorrectStateAreDisplayed() {
		// All statuses are shown so we should not see a selection with status in it
		assertFalse(mLastResponseSelection.contains("status"));
	}

	public void testListItemInfoIsCorrect() {
		assertTrue(solo.searchText("^Survey Title$"));
		assertTrue(solo.searchText("^Campaign Name$"));
		assertTrue(solo.searchText(DateUtils.formatDateTime(getActivity(), today.getTimeInMillis(), DateUtils.FORMAT_NUMERIC_DATE)));
		assertTrue(solo.searchText(DateUtils.formatDateTime(getActivity(), today.getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME)));
		assertTrue(solo.searchText("^\\d{1,2}:\\d{1,2}(am|pm)"));
	}

	public void testClickListItem() {
		solo.clickOnText("Survey Title");
		solo.assertCurrentActivity("Expected Response Info", ResponseInfoActivity.class);
		assertEquals(0, ContentUris.parseId(solo.getCurrentActivity().getIntent().getData()));
		solo.goBack();
	}

	public void testEmptyList() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #3");
		assertTrue(solo.searchText("^No responses$"));
	}

	public void testFilterAll() {
		solo.searchText("Survey Title");
		assertEquals(responses.length, solo.getCurrentListViews().get(0).getCount());
	}

	public void testFilterWithCampaign() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");		
		solo.searchText("Survey Title");
		assertEquals(responses4.length, solo.getCurrentListViews().get(0).getCount());
	}

	public void testFilterWithCampaignAndSurvey() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		solo.clickOnText("All Surveys");
		solo.clickOnText("Survey #2");
		solo.searchText("Survey Title");
		assertEquals(responses2.length, solo.getCurrentListViews().get(0).getCount());
	}
}
