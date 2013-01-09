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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.OhmageApplication;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.authenticator.Authenticator;
import org.ohmage.authenticator.AuthenticatorActivity;
import org.ohmage.test.helper.SharedPreferencesHelper;

import java.util.concurrent.CountDownLatch;

/**
 * <p>This class contains tests for the {@link AuthenticatorActivity}</p>
 * 
 * <p>These tests are similar to those contained in {@link AuthenticatorActivityPasswordUpdateTest} except these tests simulate the case
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
public class AuthenticatorActivityTest extends ActivityInstrumentationTestCase2<AuthenticatorActivity> {

	private Solo solo;

	private Account oldAccount;
	private AccountManager am;
	private String oldPassword;

	public AuthenticatorActivityTest() {
		super(AuthenticatorActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		solo = new Solo(getInstrumentation(), getActivity());

		// First get a reference to the old account, and then clear it
		am = AccountManager.get(getActivity());
		Account[] accounts = am.getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
		if(accounts.length > 0) {
			oldAccount = accounts[0];
			oldPassword = am.peekAuthToken(oldAccount, OhmageApplication.AUTHTOKEN_TYPE);
			final CountDownLatch latch = new CountDownLatch(1);
			Authenticator.setAllowRemovingAccounts(true);
			am.removeAccount(accounts[0], new AccountManagerCallback<Boolean>() {
				
				@Override
				public void run(AccountManagerFuture<Boolean> future) {
					latch.countDown();
				}
			}, null);
			latch.await();

			// Stop this instance of the activity where the account exists
			solo.goBack();
			setActivity(null);

			// Start and instance of the activity that we want to test
			solo = new Solo(getInstrumentation(), getActivity());
		}
	}

	@Override
	protected void tearDown() throws Exception{

		// When we tear down, we want to replace the username and pass
		am.addAccountExplicitly(oldAccount, oldPassword, null);

		try {
			solo.finalize();
		} catch (Throwable e) { 
			e.printStackTrace();
		}
		getActivity().finish(); 
		super.tearDown();
	}

	public void testInitialLogin() {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && oldAccount.name == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		solo.enterText(0, (SharedPreferencesHelper.USERNAME == null) ? oldAccount.name : SharedPreferencesHelper.USERNAME);
		solo.enterText(1, SharedPreferencesHelper.PASSWORD);
		solo.clickOnText("Login");
		solo.assertCurrentActivity("Expected to go to dash", DashboardActivity.class);
		solo.goBack();
	}

	public void testInvalidLogin() {
		if(SharedPreferencesHelper.PASSWORD == null || (SharedPreferencesHelper.USERNAME == null && oldAccount.name == null))
			throw new RuntimeException("The username and password constants must be set in order to log in");

		solo.enterText(0, (SharedPreferencesHelper.USERNAME == null) ? oldAccount.name : SharedPreferencesHelper.USERNAME);
		solo.enterText(1, SharedPreferencesHelper.PASSWORD+"bad");
		solo.clickOnText("Login");

		solo.searchText("Error");
		solo.searchText("Unable to authenticate");
		solo.clickOnText("OK");
		solo.assertCurrentActivity("Expected to stay on login", AuthenticatorActivity.class);
	}
}
