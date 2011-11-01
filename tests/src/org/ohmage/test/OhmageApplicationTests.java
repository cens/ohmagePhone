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

import com.google.android.imageloader.ImageLoader;

import org.ohmage.OhmageApplication;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * <p>This is a simple framework for a test of the OhmageApplication</p>
 * 
 * <p>To run this test, you can type:
 * <pre>adb shell am instrument -w \
 *   -e class org.ohmage.test.OhmageApplicationTests \
 *   org.ohmage.test/android.test.InstrumentationTestRunner</pre></p>
 */
public class OhmageApplicationTests extends ApplicationTestCase<OhmageApplication> {

	public OhmageApplicationTests() {
		super(OhmageApplication.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@SmallTest
	public void testPreconditions() {
	}

	/**
	 * Test basic startup/shutdown of Application
	 */
	@MediumTest
	public void testSimpleCreate() {
		createApplication();
	}

	/**
	 * Test that image loader exists
	 */
	@SmallTest
	public void testImageLoaderExists() {
		createApplication();
		ImageLoader loader = (ImageLoader) getApplication().getSystemService(ImageLoader.IMAGE_LOADER_SERVICE);
		assertNotNull(loader);
	}

}
