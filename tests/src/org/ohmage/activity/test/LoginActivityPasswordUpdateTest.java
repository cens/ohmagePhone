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

import org.ohmage.activity.LoginActivity;
import org.ohmage.test.helper.SharedPreferencesHelper;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * <p>This class contains tests for the {@link LoginActivity}</p>
 * 
 * <p>These tests are similar to those contained in {@link LoginActivityTest} except these tests simulate the case
 * where the user is still logged into the system and is just updating their password</p>
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
public class LoginActivityPasswordUpdateTest extends ActivityInstrumentationTestCase2<LoginActivity> {

	private Solo solo;
	private org.ohmage.SharedPreferencesHelper mPrefsHelper;

	private String userName;
	private String hashedPass;

	public LoginActivityPasswordUpdateTest() {
		super(LoginActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityIntent(new Intent().putExtra(LoginActivity.EXTRA_UPDATE_CREDENTIALS, true));

		mPrefsHelper = new org.ohmage.SharedPreferencesHelper(getActivity());
		userName = mPrefsHelper.getUsername();
		hashedPass = mPrefsHelper.getHashedPassword();

		if(userName == null && SharedPreferencesHelper.USERNAME != null) {
			mPrefsHelper.putUsername(SharedPreferencesHelper.USERNAME);

			// the activity was started with the wrong shared preferences. We should 
			// restart it
			getActivity().finish();
			setActivity(null);
		}

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

	public void testInitialLogin() throws Throwable {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && userName == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		assertFalse(solo.getEditText(0).isEnabled());
		assertEquals((SharedPreferencesHelper.USERNAME == null) ? userName : SharedPreferencesHelper.USERNAME, solo.getEditText(0).getText().toString());
		solo.enterText(1, SharedPreferencesHelper.PASSWORD);
		solo.clickOnText("Login");

		// Wait for the dialog to close
		solo.waitForDialogToClose(30000);

		// Should finish
		assertTrue(getActivity().isFinishing());

		// TODO:
		// In this case we expect it to go back to the activity that started it

		// We should have a username and password in the shared prefs
		assertTrue(mPrefsHelper.getUsername() != null);
		assertTrue(mPrefsHelper.getHashedPassword() != null);
	}

	/**
	 * Test that an invalid login causes the unable to authenticate error to be shown
	 */
	public void testInvalidLogin() {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && userName == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		assertFalse(solo.getEditText(0).isEnabled());
		assertEquals((SharedPreferencesHelper.USERNAME == null) ? userName : SharedPreferencesHelper.USERNAME, solo.getEditText(0).getText().toString());
		solo.enterText(1, SharedPreferencesHelper.PASSWORD+"bad");
		solo.clickOnText("Login");

		solo.searchText("Error");
		solo.searchText("Unable to authenticate");
		solo.clickOnText("OK");
		solo.assertCurrentActivity("Expected to stay on login", LoginActivity.class);

		// Even though we failed to update our credentials, we should keep what we had before
		assertTrue(mPrefsHelper.getUsername() != null);
		assertTrue(mPrefsHelper.getHashedPassword() != null);
	}
}
