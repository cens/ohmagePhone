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

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.LoginActivity;
import org.ohmage.test.helper.SharedPreferencesHelper;

/**
 * <p>This class contains tests for the {@link LoginActivity}</p>
 * 
 * <p>These tests are similar to those contained in {@link LoginActivityPasswordUpdateTest} except these tests simulate the case
 * where the user is logging into the app for the first time</p>
 * 
 * TODO:
 * Other Testing
 * <ul>
 * 	<li>Test that the network error is shown if there is no connectivity</li>
 * </ul>
 * 
 * @author cketcham
 *
 */
public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {

	private Solo solo;
	private UserPreferencesHelper mPrefsHelper;

	private String userName;
	private String hashedPass;

	public LoginActivityTest() {
		super(LoginActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// We need to get the username and hashed pass from the preferences and clear them with a context object
		// but also before the activity starts. So the first time we setUp, we will create an activity, change the
		// data we need to change and close the activity. The next time we start the activity for realz.
		mPrefsHelper = new UserPreferencesHelper(getActivity());
		userName = mPrefsHelper.getUsername();
		hashedPass = mPrefsHelper.getHashedPassword();
        setActivityIntent(new Intent().putExtra(LoginActivity.PARAM_USERNAME, userName));

		// Clear the user information
		mPrefsHelper.clearCredentials();

		// Stop this instance of the activity where the username and password exist
		getActivity().finish();
		setActivityIntent(null);
		setActivity(null);

		// Start and instance of the activity that we want to test
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	protected void tearDown() throws Exception{

		// When we tear down, we want to replace the username and hashed pass
		mPrefsHelper.putUsername(userName);
		mPrefsHelper.putHashedPassword(hashedPass);

		try {
			solo.finalize();
		} catch (Throwable e) { 
			e.printStackTrace();
		}
		getActivity().finish(); 
		super.tearDown();
	}

	public void testInitialLogin() {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && userName == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		solo.enterText(0, (SharedPreferencesHelper.USERNAME == null) ? userName : SharedPreferencesHelper.USERNAME);
		solo.enterText(1, SharedPreferencesHelper.PASSWORD);
		solo.clickOnText("Login");
		solo.assertCurrentActivity("Expected to go to dash", DashboardActivity.class);
		solo.goBack();
	}

	public void testInvalidLogin() {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && userName == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		solo.enterText(0, (SharedPreferencesHelper.USERNAME == null) ? userName : SharedPreferencesHelper.USERNAME);
		solo.enterText(1, SharedPreferencesHelper.PASSWORD+"bad");
		solo.clickOnText("Login");

		solo.searchText("Error");
		solo.searchText("Unable to authenticate");
		solo.clickOnText("OK");
		solo.assertCurrentActivity("Expected to stay on login", LoginActivity.class);
	}
}
