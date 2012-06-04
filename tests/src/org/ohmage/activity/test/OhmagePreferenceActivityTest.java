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
import org.ohmage.activity.AdminPincodeActivity;
import org.ohmage.activity.AdminSettingsActivity;
import org.ohmage.activity.OhmagePreferenceActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * <p>This class contains tests for the {@link OhmagePreferenceActivity}</p>
 * 
 * @author cketcham
 *
 */
public class OhmagePreferenceActivityTest extends ActivityInstrumentationTestCase2<OhmagePreferenceActivity> {

	private Solo solo;

	public OhmagePreferenceActivityTest() {
		super(OhmagePreferenceActivity.class);
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

	public void testAdminPreconditions() {
		fail("Make sure to do these tests with admin mode on and off");
	}

	public void testAdminSettings() {
		solo.clickOnText("Admin Settings");

		if(Config.ADMIN_MODE) {
			solo.assertCurrentActivity("Expected Admin settings", AdminSettingsActivity.class);
		} else {
			solo.assertCurrentActivity("Expected Admin Pincode", AdminPincodeActivity.class);
		}
		solo.goBack();
	}

	public void testSinglePreconditions() {
		fail("Make sure to do these tests single campaign mode on and off");
	}

	public void testCampaignStatus() {
		if(Config.IS_SINGLE_CAMPAIGN) {
			assertTrue(solo.searchText("Single-Campaign Mode", true));
		} else {
			assertTrue(solo.searchText("Multi-Campaign Mode", true));
		}
	}

	public void testUrlShown() {
		assertTrue(solo.searchText(Config.serverUrl(), true));
	}
}