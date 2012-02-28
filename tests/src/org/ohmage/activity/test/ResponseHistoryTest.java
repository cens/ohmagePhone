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

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.jayway.android.robotium.solo.Solo;

import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.ResponseHistoryActivity;
import org.ohmage.activity.ResponseInfoActivity;
import org.ohmage.activity.ResponseListActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.db.Models.Survey;
import org.ohmage.db.test.CampaignCursor;
import org.ohmage.db.test.DelegatingMockContentProvider;
import org.ohmage.db.test.EmptyMockCursor;
import org.ohmage.db.test.OhmageUriMatcher;
import org.ohmage.db.test.ResponseCursor;
import org.ohmage.db.test.SurveyCursor;
import org.ohmage.feedback.visualization.MapViewItemizedOverlay;
import org.ohmage.fragments.ResponseMapFragment;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;
import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;
import org.ohmage.ui.OhmageFilterable.TimeFilter;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContentResolver;
import android.text.format.DateUtils;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * <p>This class contains tests for the {@link ResponseHistoryActivity}</p>
 * 
 * <p> TODO: check starting with the campaign and survey specified in the intent </p>
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
 *  <li>4 responses with the time of today, yesterday, the day before that and the day before that with a campaign filter</li>
 *  <li>2 responses with the time of today, and yesterday with a campaign and survey filter</li>
 *  <li>2 responses a month ago with a campaign and Survey #3</li>
 * </ul>
 * 
 * @author cketcham
 *
 */
public class ResponseHistoryTest extends ActivityInstrumentationTestCase2<ResponseHistoryActivity> {

	private Solo solo;
	private DelegatingMockContentProvider provider;

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

	Response[] responses = new Response[4];
	{
		responses[0] = new Response();
		responses[0].locationStatus = SurveyGeotagService.LOCATION_UNAVAILABLE;
	}

	/** responses specifically for a Campaign and Survey Filter */
	Response[] responses2 = new Response[2];
	{
		for(int i=0; i< responses2.length; i++) {
			responses2[i] = new Response();
			responses2[i].time = Calendar.getInstance().getTimeInMillis() - DateUtils.DAY_IN_MILLIS * i;
		}
	}

	/** responses specifically for a Campaign Filter */
	Response[] responses4 = new Response[4];
	{
		for(int i=0; i< responses4.length; i++) {
			responses4[i] = new Response();
			responses4[i].time = Calendar.getInstance().getTimeInMillis() - DateUtils.DAY_IN_MILLIS * i;
		}
	}

	public ResponseHistoryTest() {
		super(ResponseHistoryActivity.class);
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
						return new ResponseCursor(projection, new Response[8]);
					case OhmageUriMatcher.CAMPAIGN_RESPONSES:
						return new ResponseCursor(projection, responses4);
					case OhmageUriMatcher.CAMPAIGN_SURVEY_RESPONSES:
						if(Surveys.getSurveyId(uri).equals("Survey #3") && selection != null)
							return new EmptyMockCursor();
						return new ResponseCursor(projection, responses2);
					case OhmageUriMatcher.RESPONSE_BY_PID:
						return new ResponseCursor(projection);
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
		solo.assertCurrentActivity("expected response history", ResponseHistoryActivity.class);
	}

	public void testFilterBar() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		assertTrue(solo.searchText("Campaign #2"));
		solo.clickOnText("All Surveys");
	}

	public void testCampaignSurveyArrows() {
		// click left Campaign button
		solo.clickOnButton(0);
		assertTrue(solo.searchText("All Campaigns"));

		// click right Campaign button
		solo.clickOnButton(2);
		assertTrue(solo.searchText("Campaign #0"));

		// click left Survey button
		solo.clickOnButton(3);
		assertTrue(solo.searchText("All Surveys"));

		// click right Survey button
		solo.clickOnButton(5);
		assertTrue(solo.searchText("Survey #0"));
	}

	public void testMonthArrows() {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("MMMM yyy");

		assertTrue(solo.searchText(format.format(c.getTime())));

		c.add(Calendar.MONTH, -1);

		solo.clickOnButton(6);
		assertTrue(solo.searchText(format.format(c.getTime())));

		c.add(Calendar.MONTH, 1);

		solo.clickOnButton(8);
		assertTrue(solo.searchText(format.format(c.getTime())));

		c.add(Calendar.MONTH, 1);

		solo.clickOnButton(8);
		assertTrue(solo.searchText(format.format(c.getTime())));
	}

	public void testCampaignFilterContainsAllCampaigns() {
		solo.clickOnText("All Campaigns");
		for(Campaign c : campaigns)
			assertTrue(solo.searchText(c.mName));
	}

	/**
	 * First chooses campaign #0 and checks for the correct surveys
	 * Then chooses campaign #1 and checks for the correct surveys
	 * @throws InterruptedException 
	 */
	public void testSurveyFilterContainsCorrectSurveys() throws InterruptedException {
		solo.clickOnButton(2);
		assertTrue(solo.searchText("Campaign #0"));

		solo.clickOnText("All Surveys");
		for(Survey s : surveys)
			assertTrue(solo.searchText(s.mTitle));

		solo.goBack();
		solo.clickOnButton(2);
		assertTrue(solo.searchText("Campaign #1"));

		solo.clickOnText("All Surveys");
		for(Survey s : surveys1)
			assertTrue(solo.searchText(s.mTitle));
		for(Survey s : surveys)
			assertFalse(solo.searchText(s.mTitle));
	}

	/**
	 * Tests that the filters persist when changing from calendar to mapview or back
	 */
	public void testFilterPersistance() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		solo.clickOnText("All Surveys");
		solo.clickOnText("Survey #2");
		assertTrue(solo.searchText("Survey #2"));

		solo.clickOnText("MAP");
		assertTrue(solo.searchText("Campaign #2"));
		assertTrue(solo.searchText("Survey #2"));
		solo.clickOnText("CALENDAR");
		assertTrue(solo.searchText("Campaign #2"));
		assertTrue(solo.searchText("Survey #2"));
	}

	public void testResponseCounts() {
		// Search for the totals for this month and totals
		assertTrue(solo.searchText("8 / Total: 8"));

		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		assertTrue(solo.searchText("Campaign #2"));
		assertTrue(solo.searchText("4 / Total: 4"));

		solo.clickOnText("All Surveys");
		solo.clickOnText("Survey #2");
		assertTrue(solo.searchText("Survey #2"));
		assertTrue(solo.searchText("2 / Total: 2"));

		solo.clickOnButton(5);
		assertTrue(solo.searchText("Survey #3"));
		assertTrue(solo.searchText("0 / Total: 2"));
	}

	public void testShowResponseList() {
		// Look for the 8 responses on the 31st
		solo.clickOnText("^31$");
		solo.assertCurrentActivity("Expected Response List", ResponseListActivity.class);
		Bundle extras = solo.getCurrentActivity().getIntent().getExtras();
		assertEquals(31, extras.getInt(TimeFilter.EXTRA_DAY, -1));
		assertEquals(Calendar.getInstance().get(Calendar.MONTH), extras.getInt(TimeFilter.EXTRA_MONTH, -1));
		assertEquals(Calendar.getInstance().get(Calendar.YEAR), extras.getInt(TimeFilter.EXTRA_YEAR, -1));
		assertNull(extras.getString(CampaignFilter.EXTRA_CAMPAIGN_URN));
		assertNull(extras.getString(CampaignSurveyFilter.EXTRA_SURVEY_ID));
		solo.goBack();
	}

	public void testShowResponseListWithCampaign() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		assertTrue(solo.searchText("Campaign #2"));

		// Click on today
		solo.clickOnText("^" + Calendar.getInstance().get(Calendar.DATE) + "$");
		solo.assertCurrentActivity("Expected Response List", ResponseListActivity.class);
		Bundle extras = solo.getCurrentActivity().getIntent().getExtras();
		assertEquals(Calendar.getInstance().get(Calendar.DATE), extras.getInt(TimeFilter.EXTRA_DAY, -1));
		assertEquals(Calendar.getInstance().get(Calendar.MONTH), extras.getInt(TimeFilter.EXTRA_MONTH, -1));
		assertEquals(Calendar.getInstance().get(Calendar.YEAR), extras.getInt(TimeFilter.EXTRA_YEAR, -1));
		assertEquals("urn:campaign:2", extras.getString(CampaignFilter.EXTRA_CAMPAIGN_URN));
		assertNull(extras.getString(CampaignSurveyFilter.EXTRA_SURVEY_ID));
		solo.goBack();
	}

	public void testShowResponseListWithCampaignAndSurvey() {
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		solo.clickOnText("All Surveys");
		solo.clickOnText("Survey #2");

		// Click on today
		solo.clickOnText("^" + Calendar.getInstance().get(Calendar.DATE) + "$");
		solo.assertCurrentActivity("Expected Response List", ResponseListActivity.class);
		Bundle extras = solo.getCurrentActivity().getIntent().getExtras();
		assertEquals(Calendar.getInstance().get(Calendar.DATE), extras.getInt(TimeFilter.EXTRA_DAY, -1));
		assertEquals(Calendar.getInstance().get(Calendar.MONTH), extras.getInt(TimeFilter.EXTRA_MONTH, -1));
		assertEquals(Calendar.getInstance().get(Calendar.YEAR), extras.getInt(TimeFilter.EXTRA_YEAR, -1));
		assertEquals("urn:campaign:2", extras.getString(CampaignFilter.EXTRA_CAMPAIGN_URN));
		assertEquals("Survey #2", extras.getString(CampaignSurveyFilter.EXTRA_SURVEY_ID));
		solo.goBack();
	}

	/**
	 * A few random dates with no responses on them. It would take a lot longer to check all of them...
	 */
	public void testDontShowResponseList() {
		solo.clickOnText("^1$");
		solo.assertCurrentActivity("Expected to stay on Response History", ResponseHistoryActivity.class);
		solo.clickOnText("^7$");
		solo.assertCurrentActivity("Expected to stay on Response History", ResponseHistoryActivity.class);
	}

	private MapView getFragmentMapView() {
		Fragment fragment = ((ResponseHistoryActivity) solo.getCurrentActivity()).getCurrentFragment();
		assertEquals(ResponseMapFragment.class, fragment.getClass());
		ResponseMapFragment mapFragment = (ResponseMapFragment) fragment;
		return ((MapView)((FrameLayout)mapFragment.getView().findViewById(R.id.mapview)).getChildAt(0));
	}

	private int countMapPins() {
		int count = 0;
		for(Overlay overlay: getFragmentMapView().getOverlays()) {
			count += ((MapViewItemizedOverlay)overlay).size();
		}
		return count;
	}

	public void testMapShowsPins() {
		solo.clickOnText("MAP");
		assertEquals(8, countMapPins());
	}

	public void testMapShowsPins2() {
		solo.clickOnText("MAP");
		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");
		assertEquals(4, countMapPins());
		solo.clickOnText("All Surveys");
		solo.clickOnText("Survey #2");
		assertEquals(2, countMapPins());
	}

	public void testRightArrowShowsPopup() {
		solo.clickOnText("MAP");

		// click on the right arrow
		solo.clickOnText(">", 4);
		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("1/8", true));
		// click on the X
		solo.clickOnImage(4);
		assertFalse(solo.searchText("urn:mock:campaign", true));
		assertFalse(solo.searchText("1/8", true));
	}

	public void testRightArrowShowsPopup2() {
		solo.clickOnText("MAP");

		// click on the right arrow
		solo.clickOnText(">", 4);
		solo.clickOnText(">", 4);
		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("2/8", true));
		// click on the X
		solo.clickOnImage(4);
		assertFalse(solo.searchText("urn:mock:campaign", true));
		assertFalse(solo.searchText("2/8", true));
	}

	public void testLeftArrowShowsNothing() {
		solo.clickOnText("MAP");

		// click on the left arrow
		solo.clickOnText("<", 4);
		assertFalse(solo.searchText("urn:mock:campaign"));
		assertFalse(solo.searchText("1/8"));
	}

	public void testRightArrowAll() {
		solo.clickOnText("MAP");

		// click on the right arrow
		for(int i=0;i<8;i++)
			solo.clickOnText(">", 4);

		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("8/8", true));
		solo.clickOnText(">", 4);
		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("8/8", true));

		for(int i=0;i<7;i++)
			solo.clickOnText("<", 4);

		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("1/8", true));
		solo.clickOnText("<", 4);
		assertTrue(solo.searchText("urn:mock:campaign", true));
		assertTrue(solo.searchText("1/8", true));
	}

	public void testIndexCount() {
		solo.clickOnText("MAP");

		solo.clickOnText("All Campaigns");
		solo.clickOnText("Campaign #2");

		// click on the right arrow
		solo.clickOnText(">", 4);
		assertTrue(solo.searchText("1/4", true));
	}

	public void testClickBalloon() {
		solo.clickOnText("MAP");

		// click on the right arrow
		solo.clickOnText(">", 4);
		solo.clickOnText("urn:mock:campaign");

		solo.assertCurrentActivity("Expected response info activity", ResponseInfoActivity.class);
		assertEquals(1, ContentUris.parseId(solo.getCurrentActivity().getIntent().getData()));
		solo.goBack();
	}

	public void testZoom() {
		solo.clickOnText("MAP");
		getInstrumentation().waitForIdleSync();

		int zoomLevel = getFragmentMapView().getZoomLevel();
		solo.clickOnText("\\-");
		getInstrumentation().waitForIdleSync();
		assertTrue(zoomLevel > getFragmentMapView().getZoomLevel());
		solo.clickOnText("\\+");
		assertEquals(zoomLevel, getFragmentMapView().getZoomLevel());
	}

	public void testHomeButton() {
		solo.clickOnImageButton(0);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
		solo.goBack();
	}
}
