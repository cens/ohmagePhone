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

import org.ohmage.Config;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.activity.AdminSettingsActivity;
import org.ohmage.activity.CampaignListActivity;
import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.OhmagePreferenceActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * <p>This class contains tests for the {@link OhmagePreferenceActivity}</p>
 * 
 * @author cketcham
 *
 */
public class AdminSettingsActivityTest extends ActivityInstrumentationTestCase2<AdminSettingsActivity> {

	private Solo solo;
	private UserPreferencesHelper mPrefs;

	public AdminSettingsActivityTest() {
		super(AdminSettingsActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		solo = new Solo(getInstrumentation(), getActivity());
		mPrefs = new UserPreferencesHelper(getActivity());
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

	public void testSinglePreconditions() {
		if(!Config.IS_SINGLE_CAMPAIGN) {
			fail("Make sure to do these tests single campaign mode on");
		}
	}

	public void testCampaignManagement() {
		if(Config.IS_SINGLE_CAMPAIGN) {
			solo.clickOnText("Campaign Management");
			solo.assertCurrentActivity("Expected Campaign List", CampaignListActivity.class);
			solo.goBack();
		}
	}

	public void testFeedback() {
		boolean old = mPrefs.showFeedback();
		checkboxState(old, "Feedback");
		assertFalse(mPrefs.showFeedback() == old);
		checkboxState(old, "Feedback");
		assertTrue(mPrefs.showFeedback() == old);
	}

	public void testProfile() {
		boolean old = mPrefs.showProfile();
		checkboxState(old, "Profile");
		assertFalse(mPrefs.showProfile() == old);
		checkboxState(old, "Profile");
		assertTrue(mPrefs.showProfile() == old);
	}

	public void testUploadQueue() {
		boolean old = mPrefs.showUploadQueue();
		checkboxState(old, "Upload Queue");
		assertFalse(mPrefs.showUploadQueue() == old);
		checkboxState(old, "Upload Queue");
		assertTrue(mPrefs.showUploadQueue() == old);
	}

	public void testMobility() {
		boolean old = mPrefs.showMobility();
		checkboxState(old, "Mobility");
		assertFalse(mPrefs.showMobility() == old);
		checkboxState(old, "Mobility");
		assertTrue(mPrefs.showMobility() == old);
	}

	private void checkboxState(boolean old, String name) {
		solo.clickOnText("Show " + name);
		solo.searchText(((!old) ? "Hide " : "Show ") + name);
	}

	public void testUpdatePassword() {
		solo.clickOnText("Update Password");
		solo.assertCurrentActivity("Expected Login Activity", LoginActivity.class);
		assertFalse(solo.getEditText(0).isEnabled());
		solo.goBack();
		solo.goBack();
		solo.assertCurrentActivity("Expected Admin Settings Activity", AdminSettingsActivity.class);
	}

	public void testLogout() {
		solo.clickOnText("Logout and Wipe");
		assertTrue(solo.searchText("Are you sure"));
		solo.clickOnText("Cancel");
		solo.assertCurrentActivity("Expected Admin Settings Activity", AdminSettingsActivity.class);
	}
}
