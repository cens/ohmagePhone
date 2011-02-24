package edu.ucla.cens.andwellness.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.andwellness.xml.datagenerator.custom.DataPointConditionEvaluator;
import org.andwellness.xml.datagenerator.model.DataPoint;
import org.andwellness.xml.datagenerator.model.DataPoint.PromptType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import edu.ucla.cens.andwellness.AndWellnessApi;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.AndWellnessApi.Result;
import edu.ucla.cens.andwellness.AndWellnessApi.ServerResponse;
import edu.ucla.cens.andwellness.R.id;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.R.raw;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.db.Response;
import edu.ucla.cens.andwellness.prompts.AbstractPrompt;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompts.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompts.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompts.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompts.text.TextPrompt;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
//import edu.ucla.cens.systemlog.*;
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
	private String mSurveyId;
	private String mSurveyTitle;
	private String mSurveySubmitText;
	private String mLaunchTime;
	private boolean mReachedEnd;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*bindService(new Intent(ISystemLog.class.getName()), Log.SystemLogConnection, BIND_AUTO_CREATE);
        Log.setAppName("AndWellness");*/
        
        mSurveyId = getIntent().getStringExtra("survey_id");
        mSurveyTitle = getIntent().getStringExtra("survey_title");
        mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");
        
        NonConfigurationInstance instance = (NonConfigurationInstance) getLastNonConfigurationInstance();
        
        if (instance == null) {
        
        	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		Calendar now = Calendar.getInstance();
    		mLaunchTime = dateFormat.format(now.getTime());
    		
        	mPrompts = null;
            
            try {
    			mPrompts = PromptXmlParser.parsePrompts(getResources().openRawResource(R.raw.nih_all), mSurveyId);
    		} catch (NotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (XmlPullParserException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		
    		//mResponses = new ArrayList<PromptResponse>(mPrompts.size());
			startService(new Intent(this, SurveyGeotagService.class));

    		mCurrentPosition = 0;
    		mReachedEnd = false;
        } else {
        	mPrompts = instance.prompts;
        	mCurrentPosition = instance.index;
        	mLaunchTime = instance.launchTime;
        	mReachedEnd = instance.reachedEnd;
        }
        
        if (mSurveyId.equals("stressButton")) {
        	//not sure this is in the right place
        	//auto-fill stressButton survey and submit
        	submitStressButtonSurvey();
        } else {
        
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
	}
	
	private void submitStressButtonSurvey() {
		
		if (((AbstractPrompt)mPrompts.get(0)).getResponseObject() == null) {
			Toast.makeText(SurveyActivity.this, "There is a bug: default value not being set!", Toast.LENGTH_SHORT).show();
		} else {
			((AbstractPrompt)mPrompts.get(0)).setDisplayed(true);
			((AbstractPrompt)mPrompts.get(0)).setSkipped(false);
			Log.i(TAG, mPrompts.get(mCurrentPosition).getResponseJson());
			storeResponse();
			finish();
			Toast.makeText(SurveyActivity.this, "Registered stressful event.", Toast.LENGTH_SHORT).show();
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
					TriggerFramework.notifySurveyTaken(SurveyActivity.this, mSurveyTitle);
					finish();
				} else {
					if (((AbstractPrompt)mPrompts.get(mCurrentPosition)).getResponseObject() == null) {
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
					File [] files = new File(PhotoPrompt.IMAGE_PATH).listFiles(new FilenameFilter() {
						
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
						f.renameTo(new File(PhotoPrompt.IMAGE_PATH + "/" + uuid + ".jpg"));;
					}
				}
			}
		}
		
		String campaign = "NIH";
		String campaignVersion = "1.0";
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
			surveyLaunchContextJson.put("active_triggers", TriggerFramework.getActiveTriggerInfo(this, mSurveyTitle));
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		}
		String surveyLaunchContext = surveyLaunchContextJson.toString();
		
		JSONArray responseJson = new JSONArray();
		for (int i = 0; i < mPrompts.size(); i++) {
			JSONObject itemJson = new JSONObject();
			try {
				itemJson.put("prompt_id", ((AbstractPrompt)mPrompts.get(i)).getId());
				itemJson.put("value", ((AbstractPrompt)mPrompts.get(i)).getResponseObject());
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			responseJson.put(itemJson);
		}
		String response = responseJson.toString();
		
		DbHelper dbHelper = new DbHelper(this);
		if (loc != null) {
			dbHelper.addResponseRow(campaign, campaignVersion, username, date, time, timezone, SurveyGeotagService.LOCATION_VALID, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime(), surveyId, surveyLaunchContext, response);
		} else {
			dbHelper.addResponseRowWithoutLocation(campaign, campaignVersion, username, date, time, timezone, surveyId, surveyLaunchContext, response);
		}
	}
	
	private static final int DIALOG_UPLOAD_PROGRESS = 1;
	private static final int DIALOG_NETWORK_ERROR = 2;
	private static final int DIALOG_INTERNAL_ERROR = 3;
	private static final int DIALOG_AUTHENTICATION_ERROR = 4;
	
	private static final String BCRYPT_KEY = "$2a$04$r8zKliEptVkzoiQgD833Oe";
	
	private UploadTask mTask;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_UPLOAD_PROGRESS:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setMessage("Uploading data...");
			pDialog.setCancelable(false);
			//pDialog.setIndeterminate(true);
			dialog = pDialog;
        	break;
        	
		case DIALOG_NETWORK_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to communicate with server. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();
        	break;
        
		case DIALOG_INTERNAL_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("The server returned an unexpected response. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();
        	break;
        	
		case DIALOG_AUTHENTICATION_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to authenticate. Please check username and re-enter password.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
        }
		
		return dialog;
	}
	
	private void doUpload() {
		mTask = new UploadTask(SurveyActivity.this);
		mTask.execute();
	}
	
	private void onUploadTaskDone(AndWellnessApi.ServerResponse response) {
		
		mTask = null;
		
		try {
			dismissDialog(DIALOG_UPLOAD_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.");
			e.printStackTrace();
		}
		
		switch (response.getResult()) {
		case SUCCESS:
			Log.d(TAG, "Uploaded!");
			break;
		case FAILURE:
			Log.e(TAG, "Upload FAILED!!");
			
			boolean isAuthenticationError = false;
			
			for (String code : response.getErrorCodes()) {
				if (code.charAt(1) == '2') {
					isAuthenticationError = true;
				}
			}
			
			if (isAuthenticationError) {
				showDialog(DIALOG_AUTHENTICATION_ERROR);
			} else {
				showDialog(DIALOG_INTERNAL_ERROR);
			}
			break;
		case HTTP_ERROR:
			Log.e(TAG, "Upload FAILED!!");
			showDialog(DIALOG_NETWORK_ERROR);
			break;
		case INTERNAL_ERROR:
			Log.e(TAG, "Upload FAILED!!");
			showDialog(DIALOG_INTERNAL_ERROR);
			break;
		}
	}
	
	private static class UploadTask extends AsyncTask<Void, Void, AndWellnessApi.ServerResponse> {
		
		private SurveyActivity mActivity;
		private boolean mIsDone = false;
		private SharedPreferences mPreferences;
		private String mUsername;
		private String mHashedPassword;
		private AndWellnessApi.ServerResponse mResponse = null;

		private UploadTask(SurveyActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(SurveyActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.i(TAG, "Upload Task Started.");
			mActivity.showDialog(DIALOG_UPLOAD_PROGRESS);
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			mUsername = helper.getUsername();//"xerox.gimp";
			mHashedPassword = helper.getHashedPassword();//BCrypt.hashpw("mama.quanta", BCRYPT_KEY);
		}

		@Override
		protected AndWellnessApi.ServerResponse doInBackground(Void... params) {
			
			AndWellnessApi.ServerResponse response = new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
			
			response = uploadResponses();
			
			return response;
		}
		
		@Override
		protected void onPostExecute(AndWellnessApi.ServerResponse response) {
			super.onPostExecute(response);
			
			Log.i(TAG, "Upload Task Finished.");
			
			mResponse = response;
			mIsDone = true;
			notifyTaskDone();			
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onUploadTaskDone(mResponse);
			}
		}
		
		private AndWellnessApi.ServerResponse uploadResponses() {
			
			DbHelper dbHelper = new DbHelper(mActivity);
			
			long cutoffTime = System.currentTimeMillis() - SurveyGeotagService.LOCATION_STALENESS_LIMIT;
			
			//List<Response> responseRows = dbHelper.getSurveyResponses();
			List<Response> responseRows = dbHelper.getSurveyResponsesBefore(cutoffTime);
			
			JSONArray responsesJsonArray = new JSONArray(); 
			
			for (int i = 0; i < responseRows.size(); i++) {
				JSONObject responseJson = new JSONObject();
				
				try {
					responseJson.put("date", responseRows.get(i).date);
					responseJson.put("time", responseRows.get(i).time);
					responseJson.put("timezone", responseRows.get(i).timezone);
					responseJson.put("location_status", responseRows.get(i).locationStatus);
					if (! responseRows.get(i).locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
						JSONObject locationJson = new JSONObject();
						locationJson.put("latitude", responseRows.get(i).locationLatitude);
						locationJson.put("longitude", responseRows.get(i).locationLongitude);
						locationJson.put("provider", responseRows.get(i).locationProvider);
						locationJson.put("accuracy", responseRows.get(i).locationAccuracy);
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String locationTimestamp = dateFormat.format(new Date(responseRows.get(i).locationTime));
						locationJson.put("timestamp", locationTimestamp);
						responseJson.put("location", locationJson);
					}
					responseJson.put("survey_id", responseRows.get(i).surveyId);
					responseJson.put("survey_launch_context", new JSONObject(responseRows.get(i).surveyLaunchContext));;
					responseJson.put("responses", new JSONArray(responseRows.get(i).response));
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				
				responsesJsonArray.put(responseJson);
			}
			
			AndWellnessApi.ServerResponse response = AndWellnessApi.surveyUpload(mUsername, mHashedPassword, "android", "TEST", "0.1", responsesJsonArray.toString());
			//AndWellnessApi.ServerResponse response = AndWellnessApi.mediaUpload(mUsername, mHashedPassword, "android", "TEST", "5ab872fe-3e3f-477b-8a09-7a8c29ab4879", new File(Environment.getExternalStorageDirectory(), "5ab872fe-3e3f-477b-8a09-7a8c29ab4879.jpg"));
			//AndWellnessApi.ServerResponse response = AndWellnessApi.mobilityUpload(mUsername, mHashedPassword, "android", getMobilityJsonString());
			//AndWellnessApi.ServerResponse response = AndWellnessApi.authenticate(mUsername, mHashedPassword, "android");
			
			if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
				for (int i = 0; i < responseRows.size(); i++) {
					dbHelper.removeResponseRow(responseRows.get(i)._id);
				}
			}
			
			return response;
		}
		
		private String getMobilityJsonString() {
			
			String campaign = "TEST";
			String username = "xerox.gimp";
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar now = Calendar.getInstance();
			String timezone = TimeZone.getDefault().getID();
			double latitude = 0;
			double longitude = 0;
			
			JSONArray responsesJsonArray = new JSONArray(); 
			
			for (int i = 0; i < 5; i++) {
				JSONObject responseJson = new JSONObject();
				
				now.add(Calendar.MINUTE, 1);
				
				try {
					responseJson.put("date", dateFormat.format(now.getTime()));
					responseJson.put("time", now.getTimeInMillis());
					responseJson.put("timezone", timezone);
					JSONObject locationJson = new JSONObject();
					locationJson.put("latitude", latitude);
					locationJson.put("longitude", longitude);
					locationJson.put("provider", "wifigpslocationservice");
					locationJson.put("accuracy", 0);
					responseJson.put("location", locationJson);
					responseJson.put("subtype", "mode_only");
					responseJson.put("mode", "still");
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				
				responsesJsonArray.put(responseJson);
			}
			
			for (int i = 0; i < 5; i++) {
				JSONObject responseJson = new JSONObject();
				
				now.add(Calendar.MINUTE, 1);
				
				try {
					responseJson.put("date", dateFormat.format(now.getTime()));
					responseJson.put("time", now.getTimeInMillis());
					responseJson.put("timezone", timezone);
					JSONObject locationJson = new JSONObject();
					locationJson.put("latitude", latitude);
					locationJson.put("longitude", longitude);
					locationJson.put("provider", "wifigpslocationservice");
					locationJson.put("accuracy", 0);
					responseJson.put("location", locationJson);
					responseJson.put("subtype", "mode_features");
					responseJson.put("mode", "still");
					JSONObject featuresJson = new JSONObject();
					featuresJson.put("speed", 0.0);
					featuresJson.put("variance", 0.0);
					featuresJson.put("average", 0.0);
					featuresJson.put("fft", "0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0");
					responseJson.put("features", featuresJson);
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				
				responsesJsonArray.put(responseJson);
			}
			
			return responsesJsonArray.toString();
		}
	}
}
