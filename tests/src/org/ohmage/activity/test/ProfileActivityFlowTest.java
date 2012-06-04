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

import org.ohmage.ConfigHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.ProfileActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;

/**
 * <p>This class contains tests for the {@link ProfileActivity}</p>
 * 
 * @author cketcham
 *
 */
public class ProfileActivityFlowTest extends ActivityInstrumentationTestCase2<ProfileActivity> {

	private static final int INDEX_IMAGE_BUTTON_OHMAGE_HOME = 0;

	private Solo solo;
	private ConfigHelper mPrefsHelper;

	private String userName;
	private String hashedPass;

	public ProfileActivityFlowTest() {
		super(ProfileActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		solo = new Solo(getInstrumentation(), getActivity());
		
		mPrefsHelper = new ConfigHelper(getActivity());
		userName = mPrefsHelper.getUsername();
		hashedPass = mPrefsHelper.getHashedPassword();
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
		solo.goBack();
	}

	@Smoke
	public void testUpdatePasswordFlow() {
		solo.clickOnText("Update Password");
		solo.assertCurrentActivity("Expected Login Screen", LoginActivity.class);

		// Hide the keyboard
		solo.goBack();

		// And go back
		solo.goBack();
		solo.assertCurrentActivity("Expected to go back to profile", ProfileActivity.class);
	}

	@Smoke
	public void testDontLogout() {
		solo.clickOnText("Log Out");
		solo.clickOnText("No");
		solo.assertCurrentActivity("Expected to stay on profile", ProfileActivity.class);
	}

	@Smoke
	public void testLogout() {
		solo.clickOnText("Log Out");
		solo.clickOnText("Yes");

		solo.assertCurrentActivity("Expected Login Screen", LoginActivity.class);

		resetCredentials();

		// We need to close the keyboard and leave the login activity for the
		// next tests to run successfully
		solo.goBack();
		solo.goBack();
	}

	@Smoke
	public void testBackButtonAfterLogout() {
		fail("Test the back button to make sure it is not possible to get to the app after logout");
	}

	private void resetCredentials() {
		mPrefsHelper.putUsername(userName);
		mPrefsHelper.putHashedPassword(hashedPass);
	}
}
