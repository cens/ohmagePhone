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

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.ConfigHelper;
import org.mobilizingcs.R;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.OhmagePreferenceActivity;

/**
 * <p>This class contains tests for the {@link OhmagePreferenceActivity}</p>
 * 
 * @author cketcham
 *
 */
public class AdminSettingsDashboardTest extends ActivityInstrumentationTestCase2<DashboardActivity> {

	private Solo solo;

	public AdminSettingsDashboardTest() {
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

	public void testPreconditions() {
		if(ConfigHelper.isSingleCampaignMode() && !getActivity().getResources().getBoolean(R.bool.admin_mode)) {
			fail("Make sure to do these tests single campaign mode off and admin mode on");
		} else if(ConfigHelper.isSingleCampaignMode()) {
			fail("Make sure to do these tests single campaign mode off");
		} else if(!getActivity().getResources().getBoolean(R.bool.admin_mode)) {
			fail("Make sure to do these tests with admin mode on");
		}
	}

	public void testCampaigns() {
		if(!ConfigHelper.isSingleCampaignMode()) {
			assertTrue(solo.searchText("Campaigns", true));
		}
	}

	public void testFeedback() {
		assertTrue(solo.searchText("Response\nHistory", true));
		clickOnDashPref("Show Feedback");
		assertFalse(solo.searchText("Response\nHistory", true));
		clickOnDashPref("Show Feedback");
	}

	public void testProfile() {
		assertTrue(solo.searchText("Profile", true));
		clickOnDashPref("Show Profile");
		assertFalse(solo.searchText("Profile", true));
		clickOnDashPref("Show Profile");
	}

	public void testUploadQueue() {
		assertTrue(solo.searchText("Upload\nQueue", true));
		clickOnDashPref("Show Upload Queue");
		assertFalse(solo.searchText("Upload\nQueue", true));
		clickOnDashPref("Show Upload Queue");
	}

	public void testMobility() {
		assertTrue(solo.searchText("Mobility", true));
		clickOnDashPref("Show Mobility");
		assertFalse(solo.searchText("Mobility", true));
		clickOnDashPref("Show Mobility");
	}

	private void clickOnDashPref(String pref) {
		solo.clickOnMenuItem("Settings");
		solo.clickOnText("Admin Settings");
		solo.clickOnText(pref);
		solo.goBack();
		solo.goBack();
	}

}