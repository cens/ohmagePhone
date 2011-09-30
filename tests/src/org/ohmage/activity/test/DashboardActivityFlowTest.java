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

import org.ohmage.activity.CampaignListActivity;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.HelpActivity;
import org.ohmage.activity.ProfileActivity;
import org.ohmage.activity.RHTabHost;
import org.ohmage.activity.SurveyListActivity;
import org.ohmage.activity.UploadQueueActivity;
import org.ohmage.test.helper.FilterBarHelper;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;

/**
 * <p>This class contains tests for the {@link DashboardActivity}</p>
 * 
 * <p>Here we make sure that all the actions you can perform on the {@link DashboardActivity} work
 * as expected.</p>
 * 
 * @author cketcham
 *
 */
public class DashboardActivityFlowTest extends ActivityInstrumentationTestCase2<DashboardActivity> {

	private Solo solo;

	public DashboardActivityFlowTest() {
		super(DashboardActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
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
	public void testFlowCampaigns() {
		solo.clickOnText("Campaigns");
		solo.assertCurrentActivity("Expected Campaigns List", CampaignListActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testFlowSurveys() {
		solo.clickOnText("Surveys");
		solo.assertCurrentActivity("Expected Survey Activity", SurveyListActivity.class);
		solo.searchText(FilterBarHelper.ALL_CAMPAIGNS, true);
		solo.goBack();
	}

	@Smoke
	public void testFlowResponseHistory() {
		solo.clickOnText("Response[ \\n]+History");
		solo.assertCurrentActivity("Expected Response History", RHTabHost.class);
		solo.searchText(FilterBarHelper.ALL_CAMPAIGNS, true);
		solo.searchText(FilterBarHelper.ALL_SURVEYS, true);
		solo.goBack();
	}

	@Smoke
	public void testFlowUploadQueue() {
		solo.clickOnText("Upload[ \\n]+Queue");
		solo.assertCurrentActivity("Expected Upload Queue", UploadQueueActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testFlowProfile() {
		solo.clickOnText("Profile");
		solo.assertCurrentActivity("Expected Profile Activity", ProfileActivity.class);
		solo.goBack();
	}

	@Smoke
	public void testFlowHelp() {
		solo.clickOnText("Help");
		solo.assertCurrentActivity("Expected Help Activity", HelpActivity.class);
		solo.goBack();
	}
}
