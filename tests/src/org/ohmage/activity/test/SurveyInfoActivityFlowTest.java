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

import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.RHTabHost;
import org.ohmage.activity.SurveyActivity;
import org.ohmage.activity.SurveyInfoActivity;
import org.ohmage.db.DbContract.Campaigns;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;

/**
 * <p>This class contains tests for the {@link SurveyActivity}</p>
 * 
 * @author cketcham
 *
 */
public class SurveyInfoActivityFlowTest extends ActivityInstrumentationTestCase2<SurveyInfoActivity> {

	private static final String CAMPAIGN_URN = "urn:mo:chipts";
	private static final String CAMPAIGN_NAME = "CHIPTS (Mo)";
	private static final String SURVEY_ID = "alcohol";
	private static final String SURVEY_NAME = "Alcohol";

	private static final int INDEX_IMAGE_BUTTON_OHMAGE_HOME = 0;
	private static final int INDEX_IMAGE_BUTTON_RESPONSE_HISTORY = 1;

	private Solo solo;

	public SurveyInfoActivityFlowTest() {
		super(SurveyInfoActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityIntent(new Intent(Intent.ACTION_VIEW, Campaigns.buildSurveysUri(CAMPAIGN_URN, SURVEY_ID)));

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

	@Smoke
	public void testFlowHomeButtonActionBar() {
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_OHMAGE_HOME);
		solo.assertCurrentActivity("Expected Dashboard", DashboardActivity.class);
	}

	@Smoke
	public void testFlowResponseHistoryActionBar() {
		solo.clickOnImageButton(INDEX_IMAGE_BUTTON_RESPONSE_HISTORY);

		solo.assertCurrentActivity("Expected Response History", RHTabHost.class);
		solo.searchText(CAMPAIGN_NAME, true);
		solo.searchText(SURVEY_NAME, true);

		solo.goBack();
		solo.assertCurrentActivity("Expected Survey Info Activity", SurveyInfoActivity.class);
	}

	@Smoke
	public void testTakeSurvey() {
		solo.clickOnText("Take Survey");

		solo.assertCurrentActivity("Expected Survey Screen", SurveyActivity.class);
		assertTrue(solo.getCurrentActivity().getIntent().getStringExtra("campaign_urn").equals(CAMPAIGN_URN));
		assertTrue(solo.getCurrentActivity().getIntent().getStringExtra("survey_id").equals(SURVEY_ID));

		solo.goBack();
		solo.assertCurrentActivity("Expected Survey Info Activity", SurveyInfoActivity.class);
	}
}
