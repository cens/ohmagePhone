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
package org.ohmage.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import android.test.suitebuilder.TestSuiteBuilder;

/**
 * <p>A test suite containing all tests for Ohmage.</p>
 *
 * <p>To run all suites found in this apk:
 * <pre>$ adb shell am instrument -w \
 *   org.ohmage.test/android.test.InstrumentationTestRunner</pre></p>
 *
 * <p>To run just this suite from the command line:
 * <pre>$ adb shell am instrument -w \
 *   -e class org.ohmage.test.AllTests \
 *   org.ohmage.test/android.test.InstrumentationTestRunner</pre></p>
 *
 * <p>To run an individual test case, e.g. {@link org.ohmage.activity.test.CampaignInfoActivityTest}:
 * <pre>$ adb shell am instrument -w \
 *   -e class org.ohmage.activity.test.CampaignInfoActivityTest \
 *   org.ohmage.test/android.test.InstrumentationTestRunner</pre></p>
 *
 * <p>To run an individual test, e.g. {@link org.ohmage.activity.test.CampaignInfoActivityTest#testHeaderText()}:
 * <pre>$ adb shell am instrument -w \
 *   -e class com.example.android.apis.os.MorseCodeConverterTest#testHeaderText \
 *   org.ohmage.test/android.test.InstrumentationTestRunner</pre></p>
 */
public class AllTests extends TestSuite {

    public static Test suite() {
        return new TestSuiteBuilder(AllTests.class)
        		.includePackages("org.ohmage.activity.test")
                .includeAllPackagesUnderHere()
                .build();
    }
}
