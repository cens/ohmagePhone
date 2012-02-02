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

import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.activity.CampaignInfoActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.db.test.CampaignContentProvider;
import org.ohmage.db.test.CampaignCursor;
import org.ohmage.db.test.NotifyingMockContentResolver;
import org.ohmage.db.test.OhmageUriMatcher;
import org.ohmage.db.test.ResponseCursor;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * <p>This class contains tests for the {@link CampaignInfoActivity}</p>
 * 
 * <p>TODO: mock the trigger db</p>
 * 
 * @author cketcham
 *
 */
public class CampaignInfoActivityTest extends ActivityInstrumentationTestCase2<CampaignInfoActivity> {
	private static final String FAKE_TITLE = "This Campaign";
	private static final String CAMPAIGN_URN_W_ONE_RESPONSE = "urn:one:response";

	private View mLoadingView;
	private View mEntityHeader;
	private ImageView mIconView;
	private TextView mHeaderText;
	private TextView mSubtext;

	private Button surveysButton;
	private Button participateButton;
	private Button removeButton;

	private TextView mErrorBox;
	private TextView mPrivacyValue;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	private TextView mTriggersValue;

	private CampaignContentProvider provider;
	private Solo solo;

	private CampaignInfoActivity mActivity;

	private final Response[] responses = new Response[9];

	public CampaignInfoActivityTest() {
		super(CampaignInfoActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityIntent(new Intent(Intent.ACTION_VIEW, Campaigns.buildCampaignUri("blah")));

		getInstrumentation().waitForIdleSync();

		NotifyingMockContentResolver fake = new NotifyingMockContentResolver(this);

		provider = new CampaignContentProvider(OhmageApplication.getContext(), DbContract.CONTENT_AUTHORITY) {

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				switch(OhmageUriMatcher.getMatcher().match(uri)) {
					case OhmageUriMatcher.CAMPAIGN_RESPONSES:
						if(Campaigns.getCampaignUrn(uri).equals(CAMPAIGN_URN_W_ONE_RESPONSE))
							return new ResponseCursor(projection, new Response());
						return new ResponseCursor(projection, responses);
					default:
						return super.query(uri, projection, selection, selectionArgs, sortOrder);
				}
			}
		};
		provider.addToContentResolver(fake);

		OhmageApplication.setFakeContentResolver(fake);
		solo = new Solo(getInstrumentation(), getActivity());

		mActivity = getActivity();
		mLoadingView = mActivity.getWindow().getDecorView().findViewById(R.id.info_loading_bar);
		mEntityHeader = mActivity.findViewById(R.id.entity_header_content);
		mIconView = (ImageView) mActivity.findViewById(R.id.entity_icon);
		mHeaderText = (TextView) mActivity.findViewById(R.id.entity_header);
		mSubtext = (TextView) mActivity.findViewById(R.id.entity_header_sub1);

		surveysButton = (Button) mActivity.findViewById(R.id.campaign_info_button_surveys);
		participateButton = (Button) mActivity.findViewById(R.id.campaign_info_button_particpate);
		removeButton = (Button) mActivity.findViewById(R.id.campaign_info_button_remove);

		mErrorBox = (TextView) mActivity.findViewById(R.id.campaign_info_errorbox);
		mPrivacyValue = (TextView) mActivity.findViewById(R.id.campaign_info_privacy_value);
		mStatusValue = (TextView) mActivity.findViewById(R.id.campaign_info_status_value);
		mResponsesValue = (TextView) mActivity.findViewById(R.id.campaign_info_responses_value);
		mTriggersValue = (TextView) mActivity.findViewById(R.id.campaign_info_triggers_value);
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

	private Campaign getBasicCampaign() {
		Campaign c = new Campaign();
		c.mName = FAKE_TITLE;
		c.mStatus = Campaign.STATUS_READY;
		c.mUrn = CampaignCursor.DEFAULT_CAMPAIGN_URN;
		c.mPrivacy = "unknown";
		return c;
	}

	@SmallTest
	public void testPreconditions() {
		assertNotNull(mLoadingView);
		assertNotNull(mEntityHeader);
		assertNotNull(mIconView);
		assertNotNull(mHeaderText);
		assertNotNull(mSubtext);

		assertNotNull(surveysButton);
		assertNotNull(participateButton);
		assertNotNull(removeButton);

		assertNotNull(mErrorBox);
		assertNotNull(mPrivacyValue);
		assertNotNull(mStatusValue);
		assertNotNull(mResponsesValue);
		assertNotNull(mTriggersValue);
	}

	@SmallTest
	public void testLoadingState() {
		assertEquals(true, mLoadingView.getVisibility() == View.VISIBLE);
		provider.setCampaign(getBasicCampaign());
		solo.searchText(FAKE_TITLE);
		assertEquals(false, mLoadingView.getVisibility() == View.VISIBLE);
	}

	@SmallTest
	public void testHeaderText() {
		provider.setCampaign(getBasicCampaign());
		solo.searchText(FAKE_TITLE);
		assertEquals(FAKE_TITLE, mHeaderText.getText());
		assertEquals(CampaignCursor.DEFAULT_CAMPAIGN_URN, mSubtext.getText());
	}

	@MediumTest
	public void testDeletedState() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_NO_EXIST;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("deleted on server", mStatusValue.getText());
		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateStopped() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_STOPPED;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals("stopped", mStatusValue.getText());
		assertEquals("warning: this campaign is stopped, meaning that you can no longer submit surveys for it.", mErrorBox.getText().toString());
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateDownloading() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_DOWNLOADING;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("downloading...", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateInvalidUserRole() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_INVALID_USER_ROLE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("invalid role", mStatusValue.getText());
		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateOutOfDate() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_OUT_OF_DATE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("out of date", mStatusValue.getText());
		assertEquals(true, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateReady() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_READY;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("participating", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(true, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateRemote() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_REMOTE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("available for participation", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(true, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(false, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testStateVague() {
		Campaign c = getBasicCampaign();
		c.mStatus = Campaign.STATUS_VAGUE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("not available", mStatusValue.getText());
		assertEquals(false, mErrorBox.getVisibility() == View.VISIBLE);
		assertEquals(false, surveysButton.getVisibility() == View.VISIBLE);
		assertEquals(false, participateButton.getVisibility() == View.VISIBLE);
		assertEquals(true, removeButton.getVisibility() == View.VISIBLE);
	}

	@MediumTest
	public void testCampaignPrivacyStatePrivate() {
		Campaign c = getBasicCampaign();
		c.mPrivacy = Campaign.PRIVACY_PRIVATE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("private", mPrivacyValue.getText());
	}

	@MediumTest
	public void testCampaignPrivacyStateUnknown() {
		Campaign c = getBasicCampaign();
		c.mPrivacy = Campaign.PRIVACY_UNKNOWN;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("unknown", mPrivacyValue.getText());
	}

	@MediumTest
	public void testCampaignPrivacyStateInvalid() {
		Campaign c = getBasicCampaign();
		c.mPrivacy = "not real privacy state";
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("unknown", mPrivacyValue.getText());
	}

	@MediumTest
	public void testCampaignPrivacyStateInvalid2() {
		Campaign c = getBasicCampaign();
		c.mPrivacy = "8";
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("unknown", mPrivacyValue.getText());
	}

	@MediumTest
	public void testCampaignPrivacyStateShared() {
		Campaign c = getBasicCampaign();
		c.mPrivacy = Campaign.PRIVACY_SHARED;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("shared", mPrivacyValue.getText());
	}

	@MediumTest
	public void testResponseCount() {
		provider.setCampaign(getBasicCampaign());
		solo.searchText(FAKE_TITLE);

		assertEquals(responses.length + " responses submitted", mResponsesValue.getText());
	}

	@MediumTest
	public void testResponseCount1() {
		Campaign c = getBasicCampaign();
		c.mUrn = CAMPAIGN_URN_W_ONE_RESPONSE;
		provider.setCampaign(c);
		solo.searchText(FAKE_TITLE);

		assertEquals("1 response submitted", mResponsesValue.getText());
	}

//	/**
//	 * Just tests to make sure the count is what we are getting from the db
//	 */
//	@MediumTest
//	public void testTriggerCount() {
//		mLoaderHelper.waitForLoader();
//
//		// get the number of triggers for this campaign
//		TriggerDB trigDB = new TriggerDB(mActivity);
//		if (trigDB.open()) {
//			Cursor triggers = trigDB.getAllTriggers(CAMPAIGN_URN);
//			assertEquals(triggers.getCount() + " trigger(s) configured", mTriggersValue.getText());
//			triggers.close();
//			trigDB.close();
//		}
//	}
}
