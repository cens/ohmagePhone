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

import org.ohmage.AccountHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.authenticator.AuthenticatorActivity;
import org.ohmage.test.helper.SharedPreferencesHelper;

/**
 * <p>This class contains tests for the {@link AuthenticatorActivity}</p>
 * 
 * <p>These tests are similar to those contained in {@link AuthenticatorActivityTest} except these tests simulate the case
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
public class AuthenticatorActivityPasswordUpdateTest extends ActivityInstrumentationTestCase2<AuthenticatorActivity> {

	private Solo solo;
	private AccountHelper mAccountHelper;

	private String userName;

	public AuthenticatorActivityPasswordUpdateTest() {
		super(AuthenticatorActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();


		mAccountHelper = new AccountHelper(getActivity());
		userName = mAccountHelper.getUsername();
		
		getActivity().finish();
		setActivityIntent(null);
		setActivity(null);
		
		setActivityIntent(new Intent().putExtra(AuthenticatorActivity.PARAM_CONFIRMCREDENTIALS,
				true).putExtra(AuthenticatorActivity.PARAM_USERNAME, userName));

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
		solo.assertCurrentActivity("Expected to go to dash", DashboardActivity.class);
		solo.goBack();

		// We should have a username and password in the shared prefs
		assertTrue(mAccountHelper.getUsername() != null);
		assertTrue(mAccountHelper.getAuthToken() != null);
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
		solo.assertCurrentActivity("Expected to stay on login", AuthenticatorActivity.class);
		solo.goBack();

		// Even though we failed to update our credentials, we should keep what we had before
		assertTrue(mAccountHelper.getUsername() != null);
		assertTrue(mAccountHelper.getAuthToken() != null);
	}
}
