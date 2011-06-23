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
package edu.ucla.cens.andwellness.activity;


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
import edu.ucla.cens.andwellness.AndWellnessApplication;
import edu.ucla.cens.andwellness.CampaignXmlHelper;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.conditionevaluator.DataPoint;
import edu.ucla.cens.andwellness.conditionevaluator.DataPoint.PromptType;
import edu.ucla.cens.andwellness.conditionevaluator.DataPointConditionEvaluator;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.text.TextPrompt;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
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
	
	private List<Prompt> mPrompts;
	//private List<PromptResponse> mResponses;
	private int mCurrentPosition;
	private String mCampaignUrn;
	private String mSurveyId;
	private String mSurveyTitle;
	private String mSurveySubmitText;
	private String mLaunchTime;
	private boolean mReachedEnd;
	
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
            	((AndWellnessApplication) getApplication()).resetAll();
            }
    		
    		if (!preferencesHelper.isAuthenticated()) {
    			Log.i(TAG, "no credentials saved, so launch Login");
    			startActivity(new Intent(this, LoginActivity.class));
    			finish();
    		} else {
    			mPrompts = null;
                
                try {
        			mPrompts = PromptXmlParser.parsePrompts(CampaignXmlHelper.loadCampaignXmlFromDb(this, mCampaignUrn), mSurveyId);
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
    		}
        } else {
        	mPrompts = instance.prompts;
        	mCurrentPosition = instance.index;
        	mLaunchTime = instance.launchTime;
        	mReachedEnd = instance.reachedEnd;
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
        	showPrompt(mCurrentPosition);
        } else {
        	showSubmitScreen();
        }
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NonConfigurationInstance(mPrompts, mCurrentPosition, mLaunchTime, mReachedEnd);
	}

	private class NonConfigurationInstance {
		List<Prompt> prompts;
		int index;
		String launchTime;
		boolean reachedEnd;
		
		public NonConfigurationInstance(List<Prompt> prompts, int index, String launchTime, boolean reachedEnd) {
			this.prompts = prompts;
			this.index = index;
			this.launchTime = launchTime;
			this.reachedEnd = reachedEnd;
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
					AbstractPrompt currPrompt = (AbstractPrompt) mPrompts.get(mCurrentPosition);
					if(! currPrompt.isPromptAnswered()) {
						Toast.makeText(SurveyActivity.this, currPrompt.getUnansweredPromptText(), Toast.LENGTH_LONG).show();
					}
					// This can probably be removed but is being kept for now
					// as a fallback.
					else if (currPrompt.getResponseObject() == null) {
						Toast.makeText(SurveyActivity.this, "You must respond to this question before proceding.", Toast.LENGTH_SHORT).show();
					} else {
						Log.i(TAG, mPrompts.get(mCurrentPosition).getResponseJson());
						
						while (mCurrentPosition < mPrompts.size()) {
							mCurrentPosition++;
							if (mCurrentPosition == mPrompts.size()) {
								mReachedEnd = true;
								showSubmitScreen();
								
							} else {
								String condition = ((AbstractPrompt)mPrompts.get(mCurrentPosition)).getCondition();
								if (condition == null)
									condition = "";
								if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
									showPrompt(mCurrentPosition);
									break;
								} else {
									((AbstractPrompt)mPrompts.get(mCurrentPosition)).setDisplayed(false);
								}
							}
						}
					}
				}
				
				break;
			
			case R.id.skip_button:
				((AbstractPrompt)mPrompts.get(mCurrentPosition)).setSkipped(true);
				Log.i(TAG, mPrompts.get(mCurrentPosition).getResponseJson());
				
				while (mCurrentPosition < mPrompts.size()) {
					mCurrentPosition++;
					if (mCurrentPosition == mPrompts.size()) {
						mReachedEnd = true;
						showSubmitScreen();
						
					} else {
						String condition = ((AbstractPrompt)mPrompts.get(mCurrentPosition)).getCondition();
						if (condition == null)
							condition = "";
						if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
							showPrompt(mCurrentPosition);
							break;
						} else {
							((AbstractPrompt)mPrompts.get(mCurrentPosition)).setDisplayed(false);
						}
					}
				}
				break;
				
			case R.id.prev_button:
				mReachedEnd = false;
				while (mCurrentPosition > 0) {
					mCurrentPosition--;
					String condition = ((AbstractPrompt)mPrompts.get(mCurrentPosition)).getCondition();
					if (condition == null)
						condition = "";
					if (DataPointConditionEvaluator.evaluateCondition(condition, getPreviousResponses())) {
						showPrompt(mCurrentPosition);
						break;
					} else {
						((AbstractPrompt)mPrompts.get(mCurrentPosition)).setDisplayed(false);
					}
				}
				break;
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mPrompts.get(mCurrentPosition).handleActivityResult(this, requestCode, resultCode, data);
	}

	public void reloadCurrentPrompt() {
		showPrompt(mCurrentPosition);
	}
	
	private void showSubmitScreen() {
		mNextButton.setText("Submit");
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

	private void showPrompt(int index) {
		
		mNextButton.setText("Next");
		
		if (index == 0) {
			mPrevButton.setVisibility(View.INVISIBLE);
		} else {
			mPrevButton.setVisibility(View.VISIBLE);
		}
		
		// someone needs to check condition before showing prompt
		
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);
		
		((AbstractPrompt)mPrompts.get(index)).setDisplayed(true);
		((AbstractPrompt)mPrompts.get(index)).setSkipped(false);
		
		// TODO for now I'm casting, but maybe I should move getters/setters to interface?
		// or just use a list of AbstractPrompt
		mPromptText.setText(((AbstractPrompt)mPrompts.get(index)).getPromptText());
		mProgressBar.setProgress(index * mProgressBar.getMax() / mPrompts.size());
		
		if (((AbstractPrompt)mPrompts.get(index)).getSkippable().equals("true")) {
			mSkipButton.setVisibility(View.VISIBLE);
		} else {
			mSkipButton.setVisibility(View.INVISIBLE);
		}
		
		mPromptFrame.removeAllViews();
		mPromptFrame.addView(mPrompts.get(index).getView(this));
		//mPromptFrame.invalidate();
	}
	
	/*public void setResponse(int index, String id, String value) {
		// prompt doesn't know it's own index... :(
		mResponses.set(index, new PromptResponse(id, value));
	}*/
	
	private List<DataPoint> getPreviousResponses() {
		ArrayList<DataPoint> previousResponses = new ArrayList<DataPoint>();
		for (int i = 0; i < mCurrentPosition; i++) {
			DataPoint dataPoint = new DataPoint(((AbstractPrompt)mPrompts.get(i)).getId());
			dataPoint.setDisplayType(((AbstractPrompt)mPrompts.get(i)).getDisplayType());
			
			if (mPrompts.get(i) instanceof SingleChoicePrompt) {
				dataPoint.setPromptType("single_choice");
			} else if (mPrompts.get(i) instanceof MultiChoicePrompt) {
				dataPoint.setPromptType("multi_choice");
			} else if (mPrompts.get(i) instanceof MultiChoiceCustomPrompt) {
				dataPoint.setPromptType("multi_choice_custom");
			} else if (mPrompts.get(i) instanceof SingleChoiceCustomPrompt) {
				dataPoint.setPromptType("single_choice_custom");
			} else if (mPrompts.get(i) instanceof NumberPrompt) {
				dataPoint.setPromptType("number");
			} else if (mPrompts.get(i) instanceof HoursBeforeNowPrompt) {
				dataPoint.setPromptType("hours_before_now");
			} else if (mPrompts.get(i) instanceof TextPrompt) {
				dataPoint.setPromptType("text");
			} else if (mPrompts.get(i) instanceof PhotoPrompt) {
				dataPoint.setPromptType("photo");
			} 
			
			if (((AbstractPrompt)mPrompts.get(i)).isSkipped()) {
				dataPoint.setSkipped();
			} else if (!((AbstractPrompt)mPrompts.get(i)).isDisplayed()) { 
				dataPoint.setNotDisplayed();
			} else {
				if (PromptType.single_choice.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((Integer)mPrompts.get(i).getResponseObject());
				} else if (PromptType.single_choice_custom.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((Integer)mPrompts.get(i).getResponseObject());
				} else if (PromptType.multi_choice.equals(dataPoint.getPromptType())) {
					JSONArray jsonArray;
					ArrayList<Integer> dataPointValue = new ArrayList<Integer>();
					try {
						jsonArray = (JSONArray)mPrompts.get(i).getResponseObject();
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
						jsonArray = (JSONArray)mPrompts.get(i).getResponseObject();
						for (int j = 0; j < jsonArray.length(); j++) {
							dataPointValue.add((Integer)jsonArray.get(j));
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					dataPoint.setValue(dataPointValue);
				} else if (PromptType.number.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((Integer)mPrompts.get(i).getResponseObject());
				} else if (PromptType.hours_before_now.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((Integer)mPrompts.get(i).getResponseObject());
				} else if (PromptType.text.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((String)mPrompts.get(i).getResponseObject());
				} else if (PromptType.photo.equals(dataPoint.getPromptType())) {
					dataPoint.setValue((String)mPrompts.get(i).getResponseObject());
				}
			}
			
			previousResponses.add(dataPoint);
		}
		return previousResponses;
	}
	
	private void storeResponse() {
		
		//finalize photos
		for (int i = 0; i < mPrompts.size(); i++) {
			if (mPrompts.get(i) instanceof PhotoPrompt) {
				PhotoPrompt photoPrompt = (PhotoPrompt)mPrompts.get(i);
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
						f.renameTo(new File(PhotoPrompt.IMAGE_PATH + "/" + mCampaignUrn.replace(':', '_') + "/" + uuid + ".jpg"));;
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
		for (int i = 0; i < mPrompts.size(); i++) {
			JSONObject itemJson = new JSONObject();
			try {
				itemJson.put("prompt_id", ((AbstractPrompt)mPrompts.get(i)).getId());
				itemJson.put("value", ((AbstractPrompt)mPrompts.get(i)).getResponseObject());
				Object extras = ((AbstractPrompt)mPrompts.get(i)).getExtrasObject();
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
		}
		String response = responseJson.toString();
		
		DbHelper dbHelper = new DbHelper(this);
		if (loc != null) {
			dbHelper.addResponseRow(mCampaignUrn, username, date, time, timezone, SurveyGeotagService.LOCATION_VALID, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime(), surveyId, surveyLaunchContext, response);
		} else {
			dbHelper.addResponseRowWithoutLocation(mCampaignUrn, username, date, time, timezone, surveyId, surveyLaunchContext, response);
		}
	}
}
