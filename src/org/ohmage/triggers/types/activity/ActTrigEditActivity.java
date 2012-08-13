package org.ohmage.triggers.types.activity;
//TODO: 

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;

import org.ohmage.NumberPicker;
import org.ohmage.R;
import org.ohmage.triggers.base.TriggerActionDesc;
import org.ohmage.triggers.config.TrigUserConfig;
import org.ohmage.triggers.ui.ActionSelectorView;
import org.ohmage.triggers.ui.TriggerListActivity;
import org.ohmage.triggers.utils.DurationPickerPreference;
import org.ohmage.triggers.utils.SimpleTime;
import org.ohmage.triggers.utils.TimePickerPreference;
import org.ohmage.triggers.utils.TrigListPreference;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.systemlog.Analytics.Status;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ActTrigEditActivity extends PreferenceActivity 
								implements OnPreferenceClickListener, 
									OnPreferenceChangeListener, 
									OnClickListener,DialogInterface.OnMultiChoiceClickListener, 
									   DialogInterface.OnClickListener{

	
	private static final String TAG = "LocTrigEditActivity"; 

	public static final String KEY_TRIG_DESC = 
			ActTrigEditActivity.class.getName() + ".trigger_descriptor";
	public static final String KEY_TRIG_ID = 
			ActTrigEditActivity.class.getName() + ".trigger_id";
	public static final String KEY_ADMIN_MODE = 
			ActTrigEditActivity.class.getName() + ".admin_mode";
	public static final String KEY_ACT_DESC = 
			ActTrigEditActivity.class.getName() + ".act_desc";
	
	// duration? state?
	private static final String PREF_KEY_DURATION = "duration";
	private static final String PREF_KEY_ACTIVITY_STATE = "state";
	private static final String PREF_KEY_ENABLE_RANGE = "enable_time_range";
	private static final String PREF_KEY_START_TIME = "interval_start_time";
	private static final String PREF_KEY_END_TIME = "interval_end_time";
	private static final String PREF_KEY_TRIGGER_ALWAYS = "trigger_always";
	private static final String PREF_KEY_ACTIONS = "actions";
	private static final String PREF_KEY_DAYS = "days";
	
	private static final int DIALOG_ID_INVALID_TIME_ALERT = 0;
	private static final int DIALOG_ID_ACTION_SEL = 2;
	private static final int DIALOG_ID_NO_SURVEYS_SELECTED = 3;
	private static final int DIALOG_ID_INVALID_STATE = 4;
	private static final int DIALOG_ID_ZERO_DURATION = 5;
	private static final int DIALOG_ID_OUT_OF_RANGE = 6;
	private static final int DIALOG_ID_DAY_SEL = 7;
	
	private boolean mAdminMode = false;
	
	public interface ExitListener {
		
		public void onDone(Context context, int trigId, String trigDesc, String actDesc);
	}
	
	private ActTrigDesc mTrigDesc;                     //yes
	private TriggerActionDesc mActDesc;					//yes
	private static ExitListener mExitListener = null;	//yes	
	private int mTrigId = 0;							//yes
	private String[] mActions;							//yes - surveys to add trigger to
	private boolean[] mActSelected = null;		
	private AlertDialog mRepeatDialog = null;
	private String[] mDays;
	private boolean[] mRepeatStatus;

	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.trig_act_edit_preferences);
		setContentView(R.layout.trigger_editor);
		
		mTrigDesc = new ActTrigDesc();
		mActDesc = new TriggerActionDesc();
		
		if(getIntent().hasExtra(TriggerListActivity.KEY_ACTIONS)) {
			mActions = getIntent().getStringArrayExtra(TriggerListActivity.KEY_ACTIONS);
		} else {
			Log.e(TAG, "LocTrigEditActivity: Invoked with out passing surveys");
			finish();
			return;
		}
		Log.d(TAG, "onCreate ActTRigEditActivity");
		PreferenceScreen screen = getPreferenceScreen();
		int prefCount = screen.getPreferenceCount();
		for(int i = 0; i < prefCount; i++) {
			screen.getPreference(i).setOnPreferenceClickListener(this);
			screen.getPreference(i).setOnPreferenceChangeListener(this);
		}
		
		((Button) findViewById(R.id.trig_edit_done)).setOnClickListener(this);
		((Button) findViewById(R.id.trig_edit_cancel)).setOnClickListener(this);
		
		
		mAdminMode = getIntent().getBooleanExtra(KEY_ADMIN_MODE, false);
		
		String config = null;
		String action = null;
		
		if(savedInstanceState == null) {
			config = getIntent().getStringExtra(KEY_TRIG_DESC);
			action = getIntent().getStringExtra(KEY_ACT_DESC);
		}
		else {
			config = savedInstanceState.getString(KEY_TRIG_DESC);
			action = savedInstanceState.getString(KEY_ACT_DESC);
		}
		Log.d(TAG, "step 1");
		if(config != null) {	
			mTrigId = getIntent().getIntExtra(KEY_TRIG_ID, 0);
			
			if(mTrigDesc.loadString(config) && mActDesc.loadString(action)) {
				Log.d(TAG, "config and action loaded successfully");
				LinkedHashMap<String, Boolean> repeatList = mTrigDesc.getRepeat();	
				mDays = repeatList.keySet()
				   					.toArray(new String[repeatList.size()]);
				mRepeatStatus = new boolean[mDays.length];
				updateRepeatStatusArray();
				initializeGUI();
			}
			else {
				Log.d(TAG, "step 1");
				getPreferenceScreen().setEnabled(false);
				Log.d(TAG, "step 1");
				Toast.makeText(this, R.string.trigger_invalid_settings,
								Toast.LENGTH_SHORT).show();
			}
		}
		
		// if there are any preselected actions specified when the activity is first created
				// and there's currently nothing in the action description, load the selected options
				// into the action description as if they were previously selected
				if (savedInstanceState == null && mActDesc.getCount() <= 0 && getIntent().hasExtra(TriggerListActivity.KEY_PRESELECTED_ACTIONS)) {
					LinkedHashMap<String, Boolean> repeatList = mTrigDesc.getRepeat();	
					mDays = repeatList.keySet()
					   					.toArray(new String[repeatList.size()]);
					mRepeatStatus = new boolean[mDays.length];
					updateRepeatStatusArray();
					String[] preselectedActions = getIntent().getStringArrayExtra(TriggerListActivity.KEY_PRESELECTED_ACTIONS);

					for (int i = 0; i < preselectedActions.length; ++i) {
						mActDesc.addSurvey(preselectedActions[i]);
					}
					
					updateActionsPrefStatus();
				}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		updateTriggerDesc();
		outState.putString(KEY_TRIG_DESC, mTrigDesc.toString());
		outState.putString(KEY_ACT_DESC, mActDesc.toString());
	}
	
	
	private void initializeGUI() {
		
		ListPreference statePref = (ListPreference) getPreferenceScreen()
		 					.findPreference(PREF_KEY_ACTIVITY_STATE);
		Preference repeatPref = (Preference) getPreferenceScreen()
							.findPreference(PREF_KEY_DAYS);
		DurationPickerPreference durPref = (DurationPickerPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_DURATION);
		CheckBoxPreference rangePref = (CheckBoxPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_ENABLE_RANGE);
		CheckBoxPreference alwaysPref = (CheckBoxPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_TRIGGER_ALWAYS);
		TimePickerPreference startPref = (TimePickerPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_START_TIME);
		TimePickerPreference endPref = (TimePickerPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_END_TIME);
		
		Preference actionsPref = getPreferenceScreen().findPreference(PREF_KEY_ACTIONS);

		if(!mAdminMode && !TrigUserConfig.editTriggerActions) {
		
			actionsPref.setEnabled(false);
		}
		
		updateActionsPrefStatus();
		updateRepeatPrefStatus();
		
		durPref.set(mTrigDesc.getDurationHours(),mTrigDesc.getDurationMin());
		
		statePref.setValueIndex(mTrigDesc.getState());
		statePref.setSummary(statePref.getEntries()[mTrigDesc.getState()]);
		
		if(mTrigDesc.isRangeEnabled()) {
			rangePref.setChecked(true);
			
			if(mTrigDesc.shouldTriggerAlways()) {
				alwaysPref.setChecked(true);
			}
			
			startPref.setTime(mTrigDesc.getStartTime());
			endPref.setTime(mTrigDesc.getEndTime());
		}
		
		statePref.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
		durPref.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
		alwaysPref.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
		startPref.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
		endPref.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
		((Button) findViewById(R.id.trig_edit_done))
				.setEnabled(mAdminMode || TrigUserConfig.editActTrigger);
	}
	
	private void updateActionsPrefStatus() {
		Preference actionsPref = getPreferenceScreen()
		 						  	.findPreference(PREF_KEY_ACTIONS);
		if (mActDesc.getSurveys().length > 0) {
			actionsPref.setSummary(stringArrayToString(mActDesc.getSurveys()));
		} else {
			actionsPref.setSummary("None");
		}
		
	}
	
	private String stringArrayToString(String [] strings) {
		if (strings.length == 0) {
			return "";
		}
		String string = "";
		for (String s : strings) {
			string = string.concat(s).concat(", ");
		}
		return string.substring(0, string.length() - 2);
	}
	
	private void updateTriggerDesc() {
		
		
		ListPreference statePref = (ListPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_ACTIVITY_STATE);
		DurationPickerPreference durPref = (DurationPickerPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_DURATION);
		CheckBoxPreference rangePref = (CheckBoxPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_ENABLE_RANGE);
		CheckBoxPreference alwaysPref = (CheckBoxPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_TRIGGER_ALWAYS);
		TimePickerPreference startPref = (TimePickerPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_START_TIME);
		TimePickerPreference endPref = (TimePickerPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_END_TIME);
		Log.d(TAG, "activity state picked: " + statePref.getValue());
		
		if (statePref.getValue() != null){
			mTrigDesc.setState(Integer.parseInt(statePref.getValue()));
		}
		else{
			mTrigDesc.setState(0);
		}
		long duration = durPref.getDurationinMillis();
		mTrigDesc.setDuration(duration);
		
		if(rangePref.isChecked()) {
			mTrigDesc.setRangeEnabled(true);
			mTrigDesc.setTriggerAlways(alwaysPref.isChecked());
			
			mTrigDesc.setStartTime(startPref.getTime());
			mTrigDesc.setEndTime(endPref.getTime());
		}
		else {
			Calendar instance = Calendar.getInstance();
			mTrigDesc.setStartTime(new SimpleTime(instance.get(Calendar.HOUR_OF_DAY), instance.get(Calendar.MINUTE)));
			mTrigDesc.setRangeEnabled(false);
			mTrigDesc.setTriggerAlways(false);
		}
	}


	private Dialog createRepeatSelDialog() {
		Log.d(TAG, "createRepeatSelDialog()");
		updateRepeatStatusArray();
		mRepeatDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.trigger_time_select_days)
					.setPositiveButton(R.string.done, this)
					.setNegativeButton(R.string.cancel, this)
					.setMultiChoiceItems(mDays, mRepeatStatus, this)
					.create();
		
		Log.d(TAG, "about to return from createRepeatSelDialog()");
		return mRepeatDialog;
	}
	private void updateRepeatStatusArray() {
		LinkedHashMap<String, Boolean> repeatList = mTrigDesc.getRepeat();
		
		for(int i = 0; i < mDays.length; i++) {
			mRepeatStatus[i] = repeatList.get(mDays[i]);
		}
	}
	
	private void updateRepeatPrefStatus() {
		Preference repeatPref = getPreferenceScreen()
		 						  	.findPreference(PREF_KEY_DAYS);
		repeatPref.setSummary(mTrigDesc.getRepeatDescription());
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_DAY_SEL:
			return createRepeatSelDialog();
		case DIALOG_ID_ACTION_SEL:
			return createEditActionDialog();
		
		case DIALOG_ID_INVALID_TIME_ALERT: 
			final String msgErr = 
				"Make sure that the End Time is after the Start Time";

			return new AlertDialog.Builder(this)
				.setTitle("Invalid time settings!")
				.setNegativeButton("Cancel", null)
				.setMessage(msgErr)
				.create();
			
		case DIALOG_ID_INVALID_STATE:
			final String msgErrState = "Please pick an Activity State";
			
			return new AlertDialog.Builder(this)
				.setTitle("Invalid Activity State")
				.setNegativeButton("Cancel", null)
				.setMessage(msgErrState)
				.create();
			
		case DIALOG_ID_ZERO_DURATION:
			final String msgErrNoDur = "Please set the Duration";
			
			return new AlertDialog.Builder(this)
				.setTitle("Invalid Duration")
				.setNegativeButton("Cancel", null)
				.setMessage(msgErrNoDur)
				.create();
			
		case DIALOG_ID_OUT_OF_RANGE:
			final String msgErrRange = "The set duration is too big for the time range! Please lessen the duration or increase the time range";
			
			return new AlertDialog.Builder(this)
				.setTitle("Invalid Duration")
				.setNegativeButton("Cancel", null)
				.setMessage(msgErrRange)
				.create();
			

		case DIALOG_ID_NO_SURVEYS_SELECTED:
			final String msgErrNoSurveys = 
				"Make sure that at least one survey is selected";

			return new AlertDialog.Builder(this)
				.setTitle("No survey selected!")
				.setNegativeButton("Cancel", null)
				.setMessage(msgErrNoSurveys)
				.create();
		}
		return null;
	}
	
	private Dialog createEditActionDialog() {
		
		if(mActSelected == null) {
			mActSelected = new boolean[mActions.length];
			for(int i = 0; i < mActSelected.length; i++) {
				mActSelected[i] = mActDesc.hasSurvey(mActions[i]);
			}
		}
		
		AlertDialog.Builder builder = 
	 			new AlertDialog.Builder(this)
			   .setTitle(R.string.trigger_select_actions)
			   .setNegativeButton(R.string.cancel, null)
			   .setView(new ActionSelectorView(getBaseContext(), mActions, mActSelected));
		
	

		if(mAdminMode || TrigUserConfig.editTriggerActions) {
			 builder.setPositiveButton("Done", 
					 new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mActDesc.clearAllSurveys();
					
					for(int i = 0; i < mActSelected.length; i++) {
						if(mActSelected[i]) {
							mActDesc.addSurvey(mActions[i]);
						}
					}
					dialog.dismiss();
					updateActionsPrefStatus();
//					handleActionSelection(mDialogTrigId, desc);
				}
			});
		}

	
		return builder.create();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		
		case R.id.trig_edit_done:
			if(mExitListener != null) {
				ListPreference statePref = (ListPreference) this.getPreferenceScreen().findPreference(PREF_KEY_ACTIVITY_STATE);
				DurationPickerPreference durPref = (DurationPickerPreference) this.getPreferenceScreen().findPreference(PREF_KEY_DURATION);
				TimePickerPreference start = (TimePickerPreference) getPreferenceScreen().findPreference(PREF_KEY_START_TIME);
				TimePickerPreference end = (TimePickerPreference) getPreferenceScreen().findPreference(PREF_KEY_END_TIME);
				CheckBoxPreference rangePref = (CheckBoxPreference) getPreferenceScreen().findPreference(PREF_KEY_ENABLE_RANGE);

				
				
				if (statePref.getValue() == null){
					showDialog(DIALOG_ID_INVALID_STATE);
					return;
				}
				if (durPref.getDurationinMillis() == 0L){
					showDialog(DIALOG_ID_ZERO_DURATION);
					return;
				}
				if (rangePref.isChecked()){
					if(start.getTime().differenceInMinutes(end.getTime()) * 60 * 1000 <= durPref.getDurationinMillis()){
						Log.d(TAG, "start Time: " + start);
						Log.d(TAG, "end: " + end);
						Log.d(TAG, "duration: " + durPref.getDurationinMillis());
						showDialog(DIALOG_ID_OUT_OF_RANGE);
						return;
					}
				}
				updateTriggerDesc();
				
				if(!mTrigDesc.validate()) {
					showDialog(DIALOG_ID_INVALID_TIME_ALERT);
					return;
				}
				else if (mActDesc.getSurveys().length <= 0) {
					// if no surveys were selected, tell the user and abort
					showDialog(DIALOG_ID_NO_SURVEYS_SELECTED);
					return;
				}
				
				mExitListener.onDone(this, mTrigId, mTrigDesc.toString(), mActDesc.toString());
				
			}
			break;
		}
		
		finish();
	}
	@Override
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
		
		mRepeatStatus[which] = isChecked;
		
		int repeatCount = 0;
		for(int i = 0; i < mRepeatStatus.length; i++) {
			if(mRepeatStatus[i]) {
				repeatCount++;
			}
		}
		
		if(mRepeatDialog != null) {
			if(mRepeatDialog.isShowing()) {
				mRepeatDialog.getButton(AlertDialog.BUTTON_POSITIVE)
							 .setEnabled(repeatCount != 0);
			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		if(which == DialogInterface.BUTTON_POSITIVE) {
			
			for(int i = 0; i < mDays.length; i++) {
				mTrigDesc.setRepeatStatus(mDays[i], mRepeatStatus[i]);
			}
			
		}
		
		dialog.dismiss();
		updateRepeatPrefStatus();
	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object val) {
		Log.d(TAG,"onPreferenceChange()");
		Log.d(TAG, "Key: " + pref.getKey() + " val: " + val.toString());
		if(pref.getKey().equals(PREF_KEY_ACTIVITY_STATE)) {
			ListPreference statePref = (ListPreference) pref;
			int index = statePref.findIndexOfValue((String) val);
			CharSequence entry = statePref.getEntries()[index];
			pref.setSummary(entry);
		}
		
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference pref) {
		Log.d(TAG, "onPreferenceClick");
		if(pref.getKey().equals(PREF_KEY_ACTIONS)) {
			removeDialog(DIALOG_ID_ACTION_SEL);
			showDialog(DIALOG_ID_ACTION_SEL);
		} 
		else if(pref.getKey().equals(PREF_KEY_DAYS)) {
			removeDialog(DIALOG_ID_DAY_SEL);
			showDialog(DIALOG_ID_DAY_SEL);
		}
		return false;
	}
	
	public static void setOnExitListener(ExitListener listener) {
		
		mExitListener = listener;
	}

}
