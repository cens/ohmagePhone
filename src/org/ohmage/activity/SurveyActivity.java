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


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.CampaignXmlHelper;
import org.ohmage.OhmageApplication;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.conditionevaluator.DataPoint;
import org.ohmage.conditionevaluator.DataPointConditionEvaluator;
import org.ohmage.conditionevaluator.DataPoint.PromptType;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Response;
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
import org.ohmage.triggers.glue.TriggerFramework;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucla.cens.systemlog.Log;

public class SurveyActivity extends Activity {
	
	private static final String TAG = "SurveyActivity";
	
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
	private String mLaunchTime;
	private boolean mReachedEnd;
	
	private String mLastSeenRepeatableSetId;
	
	public String getSurveyId() {
		return mSurveyId;
	}
	
	public String getCampaignUrn() {
		return mCampaignUrn;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mCampaignUrn = getIntent().getStringExtra("campaign_urn");
        mSurveyId = getIntent().getStringExtra("survey_id");
        mSurveyTitle = getIntent().getStringExtra("survey_title");
        mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");
        
        NonConfigurationInstance instance = (NonConfigurationInstance) getLastNonConfigurationInstance();
        
        if (instance == null) {
        
        	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		Calendar now = Calendar.getInstance();
    		mLaunchTime = dateFormat.format(now.getTime());
    		
    		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
    		
    		if (preferencesHelper.isUserDisabled()) {
            	((OhmageApplication) getApplication()).resetAll();
            }
    		
    		if (!preferencesHelper.isAuthenticated()) {
    			Log.i(TAG, "no credentials saved, so launch Login");
    			startActivity(new Intent(this, LoginActivity.class));
    			finish();
    		} else {
    			mSurveyElements = null;
                
                try {
        			mSurveyElements = PromptXmlParser.parseSurveyElements(CampaignXmlHelper.loadCampaignXmlFromDb(this, mCampaignUrn), mSurveyId);
        		} catch (NotFoundException e) {
        			Log.e(TAG, "Error parsing prompts from xml", e);
        		} catch (XmlPullParserException e) {
        			Log.e(TAG, "Error parsing prompts from xml", e);
        		} catch (IOException e) {
        			Log.e(TAG, "Error parsing prompts from xml", e);
        		}
        		
        		//mResponses = new ArrayList<PromptResponse>(mPrompts.size());
    			startService(new Intent(this, SurveyGeotagService.class));

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
        }
        
        setContentView(R.layout.survey_activity);
        
        mSurveyTitleText = (TextView) findViewById(R.id.survey_title_text);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mPromptText = (TextView) findViewById(R.id.prompt_text);
        mPromptFrame = (FrameLayout) findViewById(R.id.prompt_frame);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mSkipButton = (Button) findViewById(R.id.skip_button);
        mNextButton = (Button) findViewById(R.id.next_button);
        
        mPrevButton.setOnClickListener(mClickListener);
        mSkipButton.setOnClickListener(mClickListener);
        mNextButton.setOnClickListener(mClickListener);
        
        mSurveyTitleText.setText(mSurveyTitle);
        if (mReachedEnd == false) {
        	showElement(mCurrentPosition);
        } else {
        	showSubmitScreen();
        }
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NonConfigurationInstance(mSurveyElements, mCurrentPosition, mLaunchTime, mReachedEnd, mLastSeenRepeatableSetId);
	}

	private class NonConfigurationInstance {
		List<SurveyElement> surveyElements;
		int index;
		String launchTime;
		boolean reachedEnd;
		String lastSeenRepeatableSetId;
		
		public NonConfigurationInstance(List<SurveyElement> surveyElements, int index, String launchTime, boolean reachedEnd, String lastSeenRepeatableSetId) {
			this.surveyElements = surveyElements;
			this.index = index;
			this.launchTime = launchTime;
			this.reachedEnd = reachedEnd;
			this.lastSeenRepeatableSetId = lastSeenRepeatableSetId;
		}
	}

	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			switch (v.getId()) {
			case R.id.next_button:
				if (mReachedEnd) {
					storeResponse();
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
		mNextButton.setText("Submit");
		mPrevButton.setText("Previous");
		mPrevButton.setVisibility(View.VISIBLE);
		mSkipButton.setVisibility(View.INVISIBLE);
		
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);
		
		mPromptText.setText("Survey Complete!");
		mProgressBar.setProgress(mProgressBar.getMax());
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.submit, null);
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
			
			mNextButton.setText("Next");
			mPrevButton.setText("Previous");
			mSkipButton.setVisibility(View.INVISIBLE);
			
			if (index == 0) {
				mPrevButton.setVisibility(View.INVISIBLE);
			} else {
				mPrevButton.setVisibility(View.VISIBLE);
			}
			
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);
			
			mPromptText.setText("Message");
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
			
			mNextButton.setText("Next");
			mPrevButton.setText("Previous");
						
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
			
			mPromptFrame.removeAllViews();
			mPromptFrame.addView(prompt.getView(this));
			//mPromptFrame.invalidate();
		} else {
			Log.e(TAG, "trying to showPrompt for element that is not a prompt!");
		}
	}
	
	private void showTerminator(int index) {
		
		if (mSurveyElements.get(index) instanceof RepeatableSetTerminator) {
			
			RepeatableSetTerminator terminator = (RepeatableSetTerminator)mSurveyElements.get(index);
			
			String terminateText = terminator.getTrueLabel();
			String repeatText = terminator.getFalseLabel();
			
			if (terminateText == null || terminateText.equals("")) {
				terminateText = "Terminate";
			}
			
			if (repeatText == null || repeatText.equals("")) {
				repeatText = "Repeat";
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
			mPromptText.setText("End of repeatable set");
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
						dataPoint.setValue((Integer)prompt.getResponseObject());
					} else if (PromptType.single_choice_custom.equals(dataPoint.getPromptType())) {
						dataPoint.setValue((Integer)prompt.getResponseObject());
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
					} else if (PromptType.number.equals(dataPoint.getPromptType())) {
						dataPoint.setValue((Integer)prompt.getResponseObject());
					} else if (PromptType.hours_before_now.equals(dataPoint.getPromptType())) {
						dataPoint.setValue((Integer)prompt.getResponseObject());
					} else if (PromptType.text.equals(dataPoint.getPromptType())) {
						dataPoint.setValue((String)prompt.getResponseObject());
					} else if (PromptType.photo.equals(dataPoint.getPromptType())) {
						dataPoint.setValue((String)prompt.getResponseObject());
					}
				}
				
				previousResponses.add(dataPoint);
			}
		}
		return previousResponses;
	}
	
	private void storeResponse() {
		
		//finalize photos
		for (int i = 0; i < mSurveyElements.size(); i++) {
			if (mSurveyElements.get(i) instanceof PhotoPrompt) {
				PhotoPrompt photoPrompt = (PhotoPrompt)mSurveyElements.get(i);
				final String uuid = (String) photoPrompt.getResponseObject();
				
				if (photoPrompt.isDisplayed() && !photoPrompt.isSkipped()) {
					File [] files = new File(PhotoPrompt.IMAGE_PATH + "/" + mCampaignUrn.replace(':', '_')).listFiles(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String filename) {
							if (filename.contains("temp" + uuid)) {
								return true;
							} else {
								return false;
							}
						}
					});
					
					for (File f : files) {
						f.renameTo(new File(PhotoPrompt.IMAGE_PATH + "/" + mCampaignUrn.replace(':', '_') + "/" + uuid + ".jpg"));
						
						// TODO: add thumbnail generation, oddly enough as a png
					}
				}
			}
		}
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String username = helper.getUsername();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		String date = dateFormat.format(now.getTime());
		long time = now.getTimeInMillis();
		String timezone = TimeZone.getDefault().getID();
		
		LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (loc == null || System.currentTimeMillis() - loc.getTime() > SurveyGeotagService.LOCATION_STALENESS_LIMIT || loc.getAccuracy() > SurveyGeotagService.LOCATION_ACCURACY_THRESHOLD) {
			Log.w(TAG, "gps provider disabled or location stale or inaccurate");
			loc = null;
		}
		
		String surveyId = mSurveyId;
		
		//get launch context from trigger glue
		JSONObject surveyLaunchContextJson = new JSONObject();
		try {
			surveyLaunchContextJson.put("launch_time", mLaunchTime);
			surveyLaunchContextJson.put("active_triggers", TriggerFramework.getActiveTriggerInfo(this, mCampaignUrn, mSurveyTitle));
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
		
		for (int i = 0; i < mSurveyElements.size(); i++) {
			if (mSurveyElements.get(i) instanceof Prompt) {
				if (!inRepeatableSet) {
					itemJson = new JSONObject();
					try {
						itemJson.put("prompt_id", ((AbstractPrompt)mSurveyElements.get(i)).getId());
						itemJson.put("value", ((AbstractPrompt)mSurveyElements.get(i)).getResponseObject());
						Object extras = ((AbstractPrompt)mSurveyElements.get(i)).getExtrasObject();
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
						subItemJson.put("prompt_id", ((AbstractPrompt)mSurveyElements.get(i)).getId());
						subItemJson.put("value", ((AbstractPrompt)mSurveyElements.get(i)).getResponseObject());
						Object extras = ((AbstractPrompt)mSurveyElements.get(i)).getExtrasObject();
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
			} else if (mSurveyElements.get(i) instanceof RepeatableSetHeader) {
				inRepeatableSet = true;
				if (i != 0 && (mSurveyElements.get(i-1) instanceof RepeatableSetTerminator) && ((RepeatableSetHeader)mSurveyElements.get(i)).getId().equals(((RepeatableSetTerminator)mSurveyElements.get(i-1)).getId())) {
					//continue existing set
					iterationResponseJson = new JSONArray();
				} else {
					//start new set
					itemJson = new JSONObject();
					try {
						itemJson.put("repeatable_set_id", ((RepeatableSetHeader)mSurveyElements.get(i)).getId());
						itemJson.put("skipped", "false");
						itemJson.put("not_displayed", ((RepeatableSetHeader)mSurveyElements.get(i)).isDisplayed() ? "false" : "true");
						repeatableSetResponseJson = new JSONArray();
						iterationResponseJson = new JSONArray();
					} catch (JSONException e) {
						Log.e(TAG, "JSONException when trying to generate response json", e);
						throw new RuntimeException(e);
					}
				}
			} else if (mSurveyElements.get(i) instanceof RepeatableSetTerminator) {
				inRepeatableSet = false;
				repeatableSetResponseJson.put(iterationResponseJson);
				try {
					itemJson.put("responses", repeatableSetResponseJson);
				} catch (JSONException e) {
					Log.e(TAG, "JSONException when trying to generate response json", e);
					throw new RuntimeException(e);
				}
				if (!(i+1 < mSurveyElements.size() && (mSurveyElements.get(i+1) instanceof RepeatableSetHeader) && ((RepeatableSetHeader)mSurveyElements.get(i+1)).getId().equals(((RepeatableSetTerminator)mSurveyElements.get(i)).getId()))) {
					responseJson.put(itemJson);
				}
			}
		}
		String response = responseJson.toString();
		
		DbHelper dbHelper = new DbHelper(this);
		if (loc != null) {
			dbHelper.addResponseRow(mCampaignUrn, username, date, time, timezone, SurveyGeotagService.LOCATION_VALID, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime(), surveyId, surveyLaunchContext, response, "local");
		} else {
			dbHelper.addResponseRowWithoutLocation(mCampaignUrn, username, date, time, timezone, surveyId, surveyLaunchContext, response, "local");
		}
		
		// create an intent and broadcast it to any interested receivers
		Intent i = new Intent("org.ohmage.SURVEY_COMPLETE");
		
		i.putExtra(Response.CAMPAIGN_URN, mCampaignUrn);
		i.putExtra(Response.USERNAME, username);
		i.putExtra(Response.DATE, date);
		i.putExtra(Response.TIME, time);
		i.putExtra(Response.TIMEZONE, timezone);
		
		if (loc != null) {
			i.putExtra(Response.LOCATION_STATUS, SurveyGeotagService.LOCATION_VALID);
			i.putExtra(Response.LOCATION_LATITUDE, loc.getLatitude());
			i.putExtra(Response.LOCATION_LONGITUDE, loc.getLongitude());
			i.putExtra(Response.LOCATION_PROVIDER, loc.getProvider());
			i.putExtra(Response.LOCATION_ACCURACY, loc.getAccuracy());
			i.putExtra(Response.LOCATION_TIME, loc.getTime());
		}
		else
		{
			i.putExtra(Response.LOCATION_STATUS, SurveyGeotagService.LOCATION_UNAVAILABLE);
		}

		i.putExtra(Response.SURVEY_ID, surveyId);
		i.putExtra(Response.SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
		i.putExtra(Response.RESPONSE, response);

		this.sendBroadcast(i);
	}
}
