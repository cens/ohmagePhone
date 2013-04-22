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

import org.ohmage.mobilizingcs.R;
import org.ohmage.activity.AdminPincodeActivity;
import org.ohmage.activity.ProfileActivity;

/**
 * <p>
 * This class contains tests for the {@link ProfileActivity}
 * </p>
 * 
 * @author cketcham
 */
public class AdminPincodeActivityTest extends ActivityInstrumentationTestCase2<AdminPincodeActivity> {

    private static final String BAD_ADMIN_PIN_STRING = "1000";

    private Solo solo;

    public AdminPincodeActivityTest() {
        super(AdminPincodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    protected void tearDown() throws Exception {

        try {
            solo.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        getActivity().finish();
        super.tearDown();
    }

    public void testInvalidPin() {
        // Make sure the Ok button isn't enabled until they enter a pin
        solo.enterText(0, BAD_ADMIN_PIN_STRING);

        solo.clickOnText("OK");
        solo.searchText("Wrong pin code.");

        assertTrue(getActivity().isFinishing());
    }

    public void testOkDisabledAtFirst() {
        // Make sure the Ok button isn't enabled until they enter a pin
        assertFalse(solo.getButton(0).isEnabled());
        solo.enterText(0, BAD_ADMIN_PIN_STRING);
        assertTrue(solo.getButton(0).isEnabled());
    }

    public void testCorrectPin() {
        fail("Make sure the correct pin works");
    }

    public void testPreconditions() {
        if (getActivity().getResources().getBoolean(R.bool.admin_mode))
            fail("Make sure to do these tests with admin mode off");
    }
}
