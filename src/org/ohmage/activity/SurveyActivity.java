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
package org.ohmage.activity;


import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.systemlog.OhmageAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.OhmageApplication;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.conditionevaluator.DataPoint;
import org.ohmage.conditionevaluator.DataPoint.PromptType;
import org.ohmage.conditionevaluator.DataPointConditionEvaluator;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Message;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.RepeatableSetHeader;
import org.ohmage.prompt.RepeatableSetTerminator;
import org.ohmage.prompt.SurveyElement;
import org.ohmage.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import org.ohmage.prompt.multichoice.MultiChoicePrompt;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import org.ohmage.prompt.number.NumberPrompt;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.ohmage.prompt.singlechoice.SingleChoicePrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import org.ohmage.prompt.text.TextPrompt;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.WakefulService;
import org.ohmage.triggers.glue.TriggerFramework;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public class SurveyActivity extends Activity implements LocationListener {

	private static final String TAG = "SurveyActivity";

	private static final int DIALOG_CANCEL_ID = 0;

	private TextView mSurveyTitleText;
	private ProgressBar mProgressBar;
	private TextView mPromptText;
	private FrameLayout mPromptFrame;
	private Button mPrevButton;
	private Button mSkipButton;
	private Button mNextButton;

	private List<SurveyElement> mSurveyElements;
	//private List<PromptResponse> mResponses;
	private int mCurrentPosition;
	private String mCampaignUrn;
	private String mSurveyId;
	private String mSurveyTitle;
	private String mSurveySubmitText;
	private long mLaunchTime;
	private boolean mReachedEnd;
	private boolean mSurveyFinished = false;

	private String mLastSeenRepeatableSetId;

	private LocationManager mLocManager;

	private final Handler mHandler = new Handler();

	public String getSurveyId() {
		return mSurveyId;
	}

	public String getCampaignUrn() {
		return mCampaignUrn;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getIntent().hasExtra("campaign_urn")) {
			mCampaignUrn = getIntent().getStringExtra("campaign_urn");
		} else if(Config.IS_SINGLE_CAMPAIGN) {
			mCampaignUrn = Campaign.getSingleCampaign(this);
		} else {
			throw new RuntimeException("The campaign urn must be passed to the Survey Activity");
		}

		mSurveyId = getIntent().getStringExtra("survey_id");
		mSurveyTitle = getIntent().getStringExtra("survey_title");
		mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");

		// Create the location manager and start listening to the GPS
		mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		NonConfigurationInstance instance = (NonConfigurationInstance) getLastNonConfigurationInstance();

		if (instance == null) {

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar now = Calendar.getInstance();
			mLaunchTime = now.getTimeInMillis();

			final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);

			if (preferencesHelper.isUserDisabled()) {
				((OhmageApplication) getApplication()).resetAll();
			}

			if (!preferencesHelper.isAuthenticated()) {
				Log.i(TAG, "no credentials saved, so launch Login");
				startActivity(new Intent(this, LoginActivity.class));
				finish();
				return;
			} else {
				mSurveyElements = null;

				try {
					mSurveyElements = PromptXmlParser.parseSurveyElements(Campaign.loadCampaignXml(this, mCampaignUrn), mSurveyId);
				} catch (NotFoundException e) {
					Log.e(TAG, "Error parsing prompts from xml", e);
				} catch (XmlPullParserException e) {
					Log.e(TAG, "Error parsing prompts from xml", e);
				} catch (IOException e) {
					Log.e(TAG, "Error parsing prompts from xml", e);
				}

				if(mSurveyElements == null) {
					// If there are no survey elements, something is wrong
					finish();
					Toast.makeText(this, R.string.invalid_survey, Toast.LENGTH_SHORT).show();
					return;
				}

				mCurrentPosition = 0;
				mReachedEnd = false;
				mLastSeenRepeatableSetId = "";
			}
		} else {
			mSurveyElements = instance.surveyElements;
			mCurrentPosition = instance.index;
			mLaunchTime = instance.launchTime;
			mReachedEnd = instance.reachedEnd;
			mLastSeenRepeatableSetId = instance.lastSeenRepeatableSetId;
			mLastElement = instance.lastElement;
		}

		setContentView(R.layout.survey_activity);

		mSurveyTitleText = (TextView) findViewById(R.id.survey_title_text);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mPromptText = (TextView) findViewById(R.id.prompt_text);
		mPromptText.setMovementMethod(ScrollingMovementMethod.getInstance());
		mPromptFrame = (FrameLayout) findViewById(R.id.prompt_frame);
		mPrevButton = (Button) findViewById(R.id.prev_button);
		mSkipButton = (Button) findViewById(R.id.skip_button);
		mNextButton = (Button) findViewById(R.id.next_button);

		mPrevButton.setOnClickListener(mClickListener);
		mSkipButton.setOnClickListener(mClickListener);
		mNextButton.setOnClickListener(mClickListener);
	}

	/**
	 * Stops the gps from running
	 */
	Runnable stopUpdates = new Runnable() {
		@Override
		public void run() {
			mLocManager.removeUpdates(SurveyActivity.this);
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);

		mSurveyTitleText.setText(mSurveyTitle);
		if (mReachedEnd == false) {
			showElement(mCurrentPosition);
		} else {
			showSubmitScreen();
		}

		// Start the gps location listener to just listen until it gets a lock or until a minute passes and then turn off
		// This is just to warm up the gps for when the response is actually submitted
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mHandler.removeCallbacks(stopUpdates);
		mHandler.postDelayed(stopUpdates, DateUtils.MINUTE_IN_MILLIS);
	}

	@Override
	public void onLocationChanged(Location location) {
		if(SurveyGeotagService.locationValid(location)) {
			// We got a good enough location so lets stop the gps
			mLocManager.removeUpdates(this);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NonConfigurationInstance(mSurveyElements, mCurrentPosition, mLaunchTime, mReachedEnd, mLastSeenRepeatableSetId, mLastElement);
	}

	private class NonConfigurationInstance {
		List<SurveyElement> surveyElements;
		int index;
		long launchTime;
		boolean reachedEnd;
		String lastSeenRepeatableSetId;
		SurveyElement lastElement;

		public NonConfigurationInstance(List<SurveyElement> surveyElements, int index, long launchTime, boolean reachedEnd, String lastSeenRepeatableSetId, SurveyElement element) {
			this.surveyElements = surveyElements;
			this.index = index;
			this.launchTime = launchTime;
			this.reachedEnd = reachedEnd;
			this.lastSeenRepeatableSetId = lastSeenRepeatableSetId;
			this.lastElement = element;
		}
	}

	private final OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// We have special logic for logging the submit button
			if(v.getId() != R.id.next_button || !mReachedEnd)
				Analytics.widget(v);

			switch (v.getId()) {
				case R.id.next_button:
					if (mReachedEnd) {
						mSurveyFinished = true;
						String uuid = storeResponse();
						Analytics.widget(v, null, uuid);
						TriggerFramework.notifySurveyTaken(SurveyActivity.this, mCampaignUrn, mSurveyTitle);
						SharedPreferencesHelper prefs = new SharedPreferencesHelper(SurveyActivity.this);
						prefs.putLastSurveyTimestamp(mSurveyId, System.currentTimeMillis());
						finish();
					} else {
						if (mSurveyElements.get(mCurrentPosition) instanceof Prompt || mSurveyElements.get(mCurrentPosition) instanceof Message) {
							//show toast if not answered
							if (mSurveyElements.get(mCurrentPosition) instanceof Message || ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).isPromptAnswered()) {
								while (mCurrentPosition < mSurveyElements.size()) {
									//increment position
									mCurrentPosition++;

									//if survey end reached, show submit screen
									if (mCurrentPosition == mSurveyElements.size()) {
										mReachedEnd = true;
										showSubmitScreen();

									} else {
										if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
											//if new position is prompt, check condition
											String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
											if (condition == null)
												condition = "";
											if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
												//if true, show new prompt
												showPrompt(mCurrentPosition);
												break;
											} else {
												//if false, loop up and increment
												((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
											}
										} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader) {
											//if new position is repeat header, check condition
											String condition = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getCondition();
											if (condition == null)
												condition = "";
											if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
												//if true, increment position, show prompt
												((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(true);
												continue;
											} else {
												//set repeatable set to NOT_DISPLAYED
												((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
												//if false, increment past repeat set prompts and terminator
												int promptCount = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getPromptCount();
												mCurrentPosition += promptCount + 1;
												//if new position is header with same id, remove items from list ???
												continue;
											}
										} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
											//if new position is a repeat terminator, show terminator
											showTerminator(mCurrentPosition);
											break;
										} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
											String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
											if (condition == null)
												condition = "";
											if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
												//if true, show message
												showMessage(mCurrentPosition);
												break;
											}
										} else {
											//something is wrong!
										}									
									}
								}
							} else {
								Toast.makeText(SurveyActivity.this, ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getUnansweredPromptText(), Toast.LENGTH_LONG).show();
							}
						} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
							//"next" maps to "terminate"
							while (mCurrentPosition < mSurveyElements.size()) {
								//increment position
								mCurrentPosition++;
								//if survey end reached, show submit screen
								if (mCurrentPosition == mSurveyElements.size()) {
									mReachedEnd = true;
									showSubmitScreen();

								} else {
									//if element is repeat header with same repeat set id, remove header, prompts, and terminator
									while (mCurrentPosition < mSurveyElements.size() && mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader 
											&& ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getId().equals(((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition-1)).getId())) {
										int count = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getPromptCount();
										mSurveyElements.remove(mCurrentPosition); //remove header
										for (int i = 0; i < count; i++) {
											mSurveyElements.remove(mCurrentPosition); //remove prompts
										}
										mSurveyElements.remove(mCurrentPosition); //remove terminator
									} //repeat until above is not true

									if (mCurrentPosition == mSurveyElements.size()) {
										mReachedEnd = true;
										showSubmitScreen();

									} else if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
										//if new position is prompt, check condition
										String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
										if (condition == null)
											condition = "";
										if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
											//if true, show new prompt
											showPrompt(mCurrentPosition);
											break;
										} else {
											//if false, loop up and increment
											((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
										}
									} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader) {
										//if new position is repeat header, check condition
										String condition = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getCondition();
										if (condition == null)
											condition = "";
										if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
											//if true, increment position, show prompt
											((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(true);
											continue;
										} else {
											//if false, increment past repeat set prompts and terminator
											//set repeatable set to NOT_DISPLAYED
											((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
											int promptCount = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getPromptCount();
											mCurrentPosition += promptCount + 1;
											//if new position is header with same id, remove items from list ???

											continue;
										}
									} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
										//if new position is a repeat terminator, show terminator
										showTerminator(mCurrentPosition);
										break;
									} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
										String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
										if (condition == null)
											condition = "";
										if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
											//if true, show message
											showMessage(mCurrentPosition);
											break;
										}
									} else {
										//something is wrong!
									}
								}
							}
						} 
					}

					break;

				case R.id.skip_button:
					if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
						((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setSkipped(true);
						//Log.i(TAG, mSurveyElements.get(mCurrentPosition).getResponseJson());

						while (mCurrentPosition < mSurveyElements.size()) {
							//increment position
							mCurrentPosition++;

							//if survey end reached, show submit screen
							if (mCurrentPosition == mSurveyElements.size()) {
								mReachedEnd = true;
								showSubmitScreen();

							} else {
								if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
									//if new position is prompt, check condition
									String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
									if (condition == null)
										condition = "";
									if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
										//if true, show new prompt
										showPrompt(mCurrentPosition);
										break;
									} else {
										//if false, loop up and increment
										((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
									}
								} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader) {
									//if new position is repeat header, check condition
									String condition = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getCondition();
									if (condition == null)
										condition = "";
									if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
										//if true, increment position, show prompt
										((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(true);
										continue;
									} else {
										//if false, increment past repeat set prompts and terminator
										int promptCount = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getPromptCount();
										mCurrentPosition += promptCount + 1;
										//if new position is header with same id, remove items from list ???
										//set repeatable set to NOT_DISPLAYED
										((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
										continue;
									}
								} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
									//if new position is a repeat terminator, show terminator
									showTerminator(mCurrentPosition);
									break;
								} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
									String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
									if (condition == null)
										condition = "";
									if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
										//if true, show message
										showMessage(mCurrentPosition);
										break;
									}
								} else {
									//something is wrong!
								}									
							}
						}
					} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
						//handle skip for repeatable sets
					}
					break;

				case R.id.prev_button:
					if (mReachedEnd || mSurveyElements.get(mCurrentPosition) instanceof Prompt || mSurveyElements.get(mCurrentPosition) instanceof Message) {
						mReachedEnd = false;
						while (mCurrentPosition > 0) {
							//decrement position
							mCurrentPosition--;

							if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
								//if element is prompt, check condition
								String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show prompt
									showPrompt(mCurrentPosition);
									break;
								} else {
									//if false, decrement again and loop
									((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
								}
							} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader) {
								//if element is a repeat header, decrement position and loop
								continue;
							} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
								//if element is repeat terminator, check condition
								String condition = ((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, decrement position, show prompt
									continue;
								} else {
									//if false, decrement past all prompts in set and header
									int promptCount = ((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition)).getPromptCount();
									mCurrentPosition -= promptCount + 1;
									continue;
								}
							} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
								String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show message
									showMessage(mCurrentPosition);
									break;
								}
							}						
						}
					} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
						//"previous" maps to "repeat"
						int promptCount = ((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition)).getPromptCount();

						mCurrentPosition++;

						if (mCurrentPosition < mSurveyElements.size() && mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetHeader 
								&& ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition)).getId().equals(((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition-1)).getId())) {
							//if next position is header with same repeat set id, increment position

							mCurrentPosition++;

							if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
								//if new position is prompt, check condition
								String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show new prompt
									showPrompt(mCurrentPosition);
									break;
								} else {
									//if false, loop up and increment
									((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
								}
							} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
								//if new position is a repeat terminator, show terminator
								showTerminator(mCurrentPosition);
								break;
							} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
								String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show message
									showMessage(mCurrentPosition);
									break;
								}
							} else {
								//something is wrong!
							}

						} else {
							//else, after current position in list, add repeat header, prompts, and terminator, then increment position
							RepeatableSetHeader newHeader = ((RepeatableSetHeader)mSurveyElements.get(mCurrentPosition - 1 - promptCount - 1)).getCopy();
							List<Prompt> newPrompts = new ArrayList<Prompt>();
							for (int i = promptCount; i > 0; i--) {
								Prompt newPrompt = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition - i - 1)).getCopy();
								newPrompts.add(newPrompt);
							}
							RepeatableSetTerminator newTerminator = ((RepeatableSetTerminator)mSurveyElements.get(mCurrentPosition - 1)).getCopy();
							mSurveyElements.add(mCurrentPosition, newTerminator);
							mSurveyElements.addAll(mCurrentPosition, newPrompts);
							mSurveyElements.add(mCurrentPosition, newHeader);

							mCurrentPosition++;

							if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
								//if new position is prompt, check condition
								String condition = ((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show new prompt
									showPrompt(mCurrentPosition);
									break;
								} else {
									//if false, loop up and increment
									((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).setDisplayed(false);
								}
							} else if (mSurveyElements.get(mCurrentPosition) instanceof RepeatableSetTerminator) {
								//if new position is a repeat terminator, show terminator
								showTerminator(mCurrentPosition);
								break;
							} else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
								String condition = ((Message)mSurveyElements.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									//if true, show message
									showMessage(mCurrentPosition);
									break;
								}
							} else {
								//something is wrong!
							}

						}

					} 
					break;
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
			((AbstractPrompt)mSurveyElements.get(mCurrentPosition)).handleActivityResult(this, requestCode, resultCode, data);
		}
	}

	public void reloadCurrentPrompt() {
		showPrompt(mCurrentPosition);
	}

	private void showSubmitScreen() {
		handlePromptChangeLogging(null);

		mNextButton.setText(R.string.submit);
		mPrevButton.setText(R.string.previous);
		mPrevButton.setVisibility(View.VISIBLE);
		mSkipButton.setVisibility(View.INVISIBLE);

		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

		mPromptText.setText(R.string.survey_complete);
		mProgressBar.setProgress(mProgressBar.getMax());

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ScrollView layout = (ScrollView) inflater.inflate(R.layout.submit, null);
		TextView submitText = (TextView) layout.findViewById(R.id.submit_text);
		//submitText.setText("Thank you for completing the survey!");
		submitText.setText(mSurveySubmitText);

		mPromptFrame.removeAllViews();
		mPromptFrame.addView(layout);
	}

	private void showElement(int index) {
		if (mSurveyElements.get(index) instanceof AbstractPrompt) {
			showPrompt(index);
		} else if (mSurveyElements.get(index) instanceof Message) {
			showMessage(index);
		}
	}

	private void showMessage(int index) {
		if (mSurveyElements.get(index) instanceof Message) {
			Message message = (Message)mSurveyElements.get(index);
			handlePromptChangeLogging(message);

			mNextButton.setText(R.string.next);
			mPrevButton.setText(R.string.previous);
			mSkipButton.setVisibility(View.INVISIBLE);

			if (index == 0) {
				mPrevButton.setVisibility(View.INVISIBLE);
			} else {
				mPrevButton.setVisibility(View.VISIBLE);
			}

			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

			mPromptText.setText(R.string.survey_message_title);
			mProgressBar.setProgress(index * mProgressBar.getMax() / mSurveyElements.size());

			mPromptFrame.removeAllViews();
			mPromptFrame.addView(message.getView(this));
		} else {
			Log.e(TAG, "trying to showMessage for element that is not a message!");
		}
	}

	private void showPrompt(int index) {

		if (mSurveyElements.get(index) instanceof AbstractPrompt) {

			AbstractPrompt prompt = (AbstractPrompt)mSurveyElements.get(index);
			handlePromptChangeLogging(prompt);

			mNextButton.setText(R.string.next);
			mPrevButton.setText(R.string.previous);

			if (index == 0) {
				mPrevButton.setVisibility(View.INVISIBLE);
			} else {
				mPrevButton.setVisibility(View.VISIBLE);
			}

			// someone needs to check condition before showing prompt

			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

			prompt.setDisplayed(true);
			prompt.setSkipped(false);

			// TODO for now I'm casting, but maybe I should move getters/setters to interface?
			// or just use a list of AbstractPrompt
			mPromptText.setText(prompt.getPromptText());
			mProgressBar.setProgress(index * mProgressBar.getMax() / mSurveyElements.size());

			if (prompt.getSkippable().equals("true")) {
				mSkipButton.setVisibility(View.VISIBLE);
				mSkipButton.setText(prompt.getSkipLabel());
				mSkipButton.invalidate();
			} else {
				mSkipButton.setVisibility(View.INVISIBLE);
			}

			//If its a photo prompt we need to recycle the image
			if(mSurveyElements.get(index) instanceof PhotoPrompt)
				PhotoPrompt.clearView(mPromptFrame);

			mPromptFrame.removeAllViews();
			mPromptFrame.addView(prompt.getView(this));
			//mPromptFrame.invalidate();
		} else {
			Log.e(TAG, "trying to showPrompt for element that is not a prompt!");
		}
	}

	private SurveyElement mLastElement;

	private void handlePromptChangeLogging(SurveyElement element) {
		// Don't log anything if its the same element
		if(element == mLastElement)
			return;

		if(mLastElement instanceof AbstractPrompt) {
			OhmageAnalytics.prompt(this, (AbstractPrompt) mLastElement, Status.OFF);
		}
		if(element  instanceof AbstractPrompt) {
			OhmageAnalytics.prompt(this, (AbstractPrompt) element, Status.ON);
		}
		mLastElement = element;
	}

	private void showTerminator(int index) {

		if (mSurveyElements.get(index) instanceof RepeatableSetTerminator) {

			RepeatableSetTerminator terminator = (RepeatableSetTerminator)mSurveyElements.get(index);

			String terminateText = terminator.getTrueLabel();
			String repeatText = terminator.getFalseLabel();

			if (terminateText == null || terminateText.equals("")) {
				terminateText = getString(R.string.survey_repeatable_set_terminate);
			}

			if (repeatText == null || repeatText.equals("")) {
				repeatText = getString(R.string.survey_repeatable_set_repeat);
			}

			mNextButton.setText(terminateText);
			mPrevButton.setText(repeatText);
			mSkipButton.setVisibility(View.INVISIBLE);

			if (index == 0) {
				//this could(should) never happen
			}

			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

			//terminator.setDisplayed(true);
			//terminator.setSkipped(false);

			// TODO for now I'm casting, but maybe I should move getters/setters to interface?
			// or just use a list of AbstractPrompt
			mPromptText.setText(R.string.survey_repeatable_set_title);
			mProgressBar.setProgress(index * mProgressBar.getMax() / mSurveyElements.size());

			//			if (terminator.getSkippable().equals("true")) {
			//				mSkipButton.setVisibility(View.VISIBLE);
			//			} else {
			//				mSkipButton.setVisibility(View.INVISIBLE);
			//			}

			mPromptFrame.removeAllViews();
			mPromptFrame.addView(terminator.getView(this));
			//mPromptFrame.invalidate();
		} else {
			Log.e(TAG, "trying to showTerminator for element that is not a RepeatableSetTerminator!");
		}
	}

	/*public void setResponse(int index, String id, String value) {
		// prompt doesn't know it's own index... :(
		mResponses.set(index, new PromptResponse(id, value));
	}*/

	private List<DataPoint> getPreviousResponses() {
		ArrayList<DataPoint> previousResponses = new ArrayList<DataPoint>();
		for (int i = 0; i < mCurrentPosition; i++) {
			if (mSurveyElements.get(i) instanceof AbstractPrompt) {
				AbstractPrompt prompt = ((AbstractPrompt)mSurveyElements.get(i));

				DataPoint dataPoint = new DataPoint(prompt.getId());
				dataPoint.setDisplayType(prompt.getDisplayType());

				if (prompt instanceof SingleChoicePrompt) {
					dataPoint.setPromptType("single_choice");
				} else if (prompt instanceof MultiChoicePrompt) {
					dataPoint.setPromptType("multi_choice");
				} else if (prompt instanceof MultiChoiceCustomPrompt) {
					dataPoint.setPromptType("multi_choice_custom");
				} else if (prompt instanceof SingleChoiceCustomPrompt) {
					dataPoint.setPromptType("single_choice_custom");
				} else if (prompt instanceof NumberPrompt) {
					dataPoint.setPromptType("number");
				} else if (prompt instanceof HoursBeforeNowPrompt) {
					dataPoint.setPromptType("hours_before_now");
				} else if (prompt instanceof TextPrompt) {
					dataPoint.setPromptType("text");
				} else if (prompt instanceof PhotoPrompt) {
					dataPoint.setPromptType("photo");
				} 

				if (prompt.isSkipped()) {
					dataPoint.setSkipped();
				} else if (!prompt.isDisplayed()) { 
					dataPoint.setNotDisplayed();
				} else {
					if (PromptType.single_choice.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					} else if (PromptType.single_choice_custom.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					} else if (PromptType.multi_choice.equals(dataPoint.getPromptType())) {
						JSONArray jsonArray;
						ArrayList<Integer> dataPointValue = new ArrayList<Integer>();
						try {
							jsonArray = (JSONArray)prompt.getResponseObject();
							for (int j = 0; j < jsonArray.length(); j++) {
								dataPointValue.add((Integer)jsonArray.get(j));
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						dataPoint.setValue(dataPointValue);
					} else if (PromptType.multi_choice_custom.equals(dataPoint.getPromptType())) {
						JSONArray jsonArray;
						ArrayList<String> dataPointValue = new ArrayList<String>();
						try {
							jsonArray = (JSONArray)prompt.getResponseObject();
							for (int j = 0; j < jsonArray.length(); j++) {
								dataPointValue.add((String)jsonArray.get(j));
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						dataPoint.setValue(dataPointValue);
					} else if (PromptType.number.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					} else if (PromptType.hours_before_now.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					} else if (PromptType.text.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					} else if (PromptType.photo.equals(dataPoint.getPromptType())) {
						dataPoint.setValue(prompt.getResponseObject());
					}
				}

				previousResponses.add(dataPoint);
			}
		}
		return previousResponses;
	}

	private String storeResponse() {
		return storeResponse(this, mSurveyId, mLaunchTime, mCampaignUrn, mSurveyTitle, mSurveyElements);
	}

	public static String storeResponse(Context context, String surveyId, long launchTime, String campaignUrn, String surveyTitle, List<SurveyElement> surveyElements) {

		SharedPreferencesHelper helper = new SharedPreferencesHelper(context);
		String username = helper.getUsername();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		String date = dateFormat.format(now.getTime());
		long time = now.getTimeInMillis();
		String timezone = TimeZone.getDefault().getID();

		//get launch context from trigger glue
		JSONObject surveyLaunchContextJson = new JSONObject();
		try {
			surveyLaunchContextJson.put("launch_time", launchTime);
			surveyLaunchContextJson.put("launch_timezone", timezone);
			surveyLaunchContextJson.put("active_triggers", TriggerFramework.getActiveTriggerInfo(context, campaignUrn, surveyTitle));
		} catch (JSONException e) {
			Log.e(TAG, "JSONException when trying to generate survey launch context json", e);
			throw new RuntimeException(e);
		}
		String surveyLaunchContext = surveyLaunchContextJson.toString();

		JSONArray responseJson = new JSONArray();
		JSONArray repeatableSetResponseJson = new JSONArray();
		JSONArray iterationResponseJson = new JSONArray();
		JSONObject itemJson = null;
		boolean inRepeatableSet = false;

		for (int i = 0; i < surveyElements.size(); i++) {
			if (surveyElements.get(i) instanceof Prompt) {
				if (!inRepeatableSet) {
					itemJson = new JSONObject();
					try {
						itemJson.put("prompt_id", ((AbstractPrompt)surveyElements.get(i)).getId());
						itemJson.put("value", ((AbstractPrompt)surveyElements.get(i)).getResponseObject());
						Object extras = ((AbstractPrompt)surveyElements.get(i)).getExtrasObject();
						if (extras != null) {
							// as of now we don't actually have "extras" we only have "custom_choices" for the custom types
							// so this is currently only used by single_choice_custom and multi_choice_custom
							itemJson.put("custom_choices", extras);
						}
					} catch (JSONException e) {
						Log.e(TAG, "JSONException when trying to generate response json", e);
						throw new RuntimeException(e);
					}
					responseJson.put(itemJson);
				} else {
					JSONObject subItemJson = new JSONObject();
					try {
						subItemJson.put("prompt_id", ((AbstractPrompt)surveyElements.get(i)).getId());
						subItemJson.put("value", ((AbstractPrompt)surveyElements.get(i)).getResponseObject());
						Object extras = ((AbstractPrompt)surveyElements.get(i)).getExtrasObject();
						if (extras != null) {
							// as of now we don't actually have "extras" we only have "custom_choices" for the custom types
							// so this is currently only used by single_choice_custom and multi_choice_custom
							subItemJson.put("custom_choices", extras);
						}
					} catch (JSONException e) {
						Log.e(TAG, "JSONException when trying to generate response json", e);
						throw new RuntimeException(e);
					}
					iterationResponseJson.put(subItemJson);
				}
			} else if (surveyElements.get(i) instanceof RepeatableSetHeader) {
				inRepeatableSet = true;
				if (i != 0 && (surveyElements.get(i-1) instanceof RepeatableSetTerminator) && ((RepeatableSetHeader)surveyElements.get(i)).getId().equals(((RepeatableSetTerminator)surveyElements.get(i-1)).getId())) {
					//continue existing set
					iterationResponseJson = new JSONArray();
				} else {
					//start new set
					itemJson = new JSONObject();
					try {
						itemJson.put("repeatable_set_id", ((RepeatableSetHeader)surveyElements.get(i)).getId());
						itemJson.put("skipped", "false");
						itemJson.put("not_displayed", ((RepeatableSetHeader)surveyElements.get(i)).isDisplayed() ? "false" : "true");
						repeatableSetResponseJson = new JSONArray();
						iterationResponseJson = new JSONArray();
					} catch (JSONException e) {
						Log.e(TAG, "JSONException when trying to generate response json", e);
						throw new RuntimeException(e);
					}
				}
			} else if (surveyElements.get(i) instanceof RepeatableSetTerminator) {
				inRepeatableSet = false;
				repeatableSetResponseJson.put(iterationResponseJson);
				try {
					itemJson.put("responses", repeatableSetResponseJson);
				} catch (JSONException e) {
					Log.e(TAG, "JSONException when trying to generate response json", e);
					throw new RuntimeException(e);
				}
				if (!(i+1 < surveyElements.size() && (surveyElements.get(i+1) instanceof RepeatableSetHeader) && ((RepeatableSetHeader)surveyElements.get(i+1)).getId().equals(((RepeatableSetTerminator)surveyElements.get(i)).getId()))) {
					responseJson.put(itemJson);
				}
			}
		}
		String response = responseJson.toString();

		// insert the response, which indirectly populates the prompt response tables, etc.
		Response candidate = new Response();

		candidate.uuid = UUID.randomUUID().toString();
		candidate.campaignUrn = campaignUrn;
		candidate.username = username;
		candidate.date = date;
		candidate.time = time;
		candidate.timezone = timezone;
		candidate.surveyId = surveyId;
		candidate.surveyLaunchContext = surveyLaunchContext;
		candidate.response = response;
		candidate.locationStatus = SurveyGeotagService.LOCATION_UNAVAILABLE;
		candidate.locationLatitude = -1;
		candidate.locationLongitude = -1;
		candidate.locationProvider = null;
		candidate.locationAccuracy = -1;
		candidate.locationTime = -1;
		candidate.status = Response.STATUS_WAITING_FOR_LOCATION;

		ContentResolver cr = context.getContentResolver();
		Uri responseUri = cr.insert(Responses.CONTENT_URI, candidate.toCV());

		Intent intent = new Intent(context, SurveyGeotagService.class);
		intent.setData(responseUri);
		WakefulService.sendWakefulWork(context, intent);

		// finalize photos now that we have the responseUri
		// the photos are initially in the campaign dir, until the response is saved
		for (int i = 0; i < surveyElements.size(); i++) {
			if (surveyElements.get(i) instanceof PhotoPrompt) {
				PhotoPrompt photoPrompt = (PhotoPrompt)surveyElements.get(i);
				if (photoPrompt.isPromptAnswered()) {
					photoPrompt.saveImageFile(Responses.getResponseId(responseUri));
				}
			}
		}

		// create an intent and broadcast it to any interested receivers
		Intent i = new Intent("org.ohmage.SURVEY_COMPLETE");

		i.putExtra(Responses.CAMPAIGN_URN, campaignUrn);
		i.putExtra(Responses.RESPONSE_USERNAME, username);
		i.putExtra(Responses.RESPONSE_DATE, date);
		i.putExtra(Responses.RESPONSE_TIME, time);
		i.putExtra(Responses.RESPONSE_TIMEZONE, timezone);
		i.putExtra(Responses.RESPONSE_LOCATION_STATUS, SurveyGeotagService.LOCATION_UNAVAILABLE);
		i.putExtra(Responses.RESPONSE_STATUS, Response.STATUS_WAITING_FOR_LOCATION);
		i.putExtra(Responses.SURVEY_ID, surveyId);
		i.putExtra(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
		i.putExtra(Responses.RESPONSE_JSON, response);

		context.sendBroadcast(i);

		return candidate.uuid;
	}

	@Override
	public void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);

		// If we are finishing
		if(isFinishing()) {
			// Stop listenting to the gps
			mLocManager.removeUpdates(this);

			//clean up the survey photo prompt
			if(!mSurveyFinished) {
				for(SurveyElement element : mSurveyElements)
					if (element instanceof PhotoPrompt)
						((PhotoPrompt) element).clearImage();
			}
		}
	}

	@Override
	public void onBackPressed() {
		showDialog(DIALOG_CANCEL_ID);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
			case DIALOG_CANCEL_ID:
				dialogBuilder.setTitle(R.string.discard_survey_title)
				.setMessage(R.string.discard_survey_message)
				.setCancelable(true)
				.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setNegativeButton(R.string.cancel, null);
				dialog = dialogBuilder.create();
				break;
		}
		return dialog;
	}
}
