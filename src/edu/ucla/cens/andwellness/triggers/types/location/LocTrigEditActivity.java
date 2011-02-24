package edu.ucla.cens.andwellness.triggers.types.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
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
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.triggers.config.TrigUserConfig;
import edu.ucla.cens.andwellness.triggers.utils.TimePickerPreference;

public class LocTrigEditActivity extends PreferenceActivity 
								implements OnPreferenceClickListener, 
								OnPreferenceChangeListener, 
								OnClickListener {

	public static final String KEY_TRIG_DESC = 
			LocTrigEditActivity.class.getName() + ".trigger_descriptor";
	public static final String KEY_TRIG_ID = 
			LocTrigEditActivity.class.getName() + ".trigger_id";
	public static final String KEY_ADMIN_MODE = 
			LocTrigEditActivity.class.getName() + ".admin_mode";
	
	private static final String PREF_KEY_LOCATION = "trigger_location";
	private static final String PREF_KEY_ENABLE_RANGE = "enable_time_range";
	private static final String PREF_KEY_START_TIME = "interval_start_time";
	private static final String PREF_KEY_END_TIME = "interval_end_time";
	private static final String PREF_KEY_TRIGGER_ALWAYS = "trigger_always";
	private static final String PREF_KEY_RENETRY_INTERVAL = "minimum_reentry";
	
	private static final int DIALOG_ID_INVALID_TIME_ALERT = 0;
	
	private boolean mAdminMode = false;
	
	public interface ExitListener {
		
		public void onDone(Context context, int trigId, String trigDesc);
	}
	
	private LocTrigDesc mTrigDesc;
	private static ExitListener mExitListener = null;
	private int mTrigId = 0;
	private String[] mCategories;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.trig_loc_edit_preferences);
		setContentView(R.layout.trigger_editor);
		
		mTrigDesc = new LocTrigDesc();
		
		initializeCategories();
		ListPreference locPref = (ListPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_LOCATION);
		locPref.setEntries(mCategories);
		locPref.setEntryValues(mCategories);
		if(mCategories.length == 0) {
			locPref.setEnabled(false);
		}
		else {
			locPref.setValue(mCategories[0]);
			locPref.setSummary(mCategories[0]);
		}
		
		PreferenceScreen screen = getPreferenceScreen();
		int prefCount = screen.getPreferenceCount();
		for(int i = 0; i < prefCount; i++) {
			screen.getPreference(i).setOnPreferenceClickListener(this);
			screen.getPreference(i).setOnPreferenceChangeListener(this);
		}
		
		((Button) findViewById(R.id.trig_edit_done)).setOnClickListener(this);
		((Button) findViewById(R.id.trig_edit_cancel)).setOnClickListener(this);
		
		
		//Set global value for minimum reentry interval
		Preference minIntervalPref = getPreferenceScreen()
									.findPreference(PREF_KEY_RENETRY_INTERVAL);
		minIntervalPref.setSummary(LocTrigDesc.getGlobalMinReentryInterval(this) + 
									" minutes");

		String config = null;
		
		if(savedInstanceState == null) {
			config = getIntent().getStringExtra(KEY_TRIG_DESC);
		}
		else {
			config = savedInstanceState.getString(KEY_TRIG_DESC);
		}
		
		if(config != null) {	
			mTrigId = getIntent().getIntExtra(KEY_TRIG_ID, 0);
			mAdminMode = getIntent().getBooleanExtra(KEY_ADMIN_MODE, false);
			
			if(mTrigDesc.loadString(config)) {
				initializeGUI();
			}
			else {
				getPreferenceScreen().setEnabled(false);
				Toast.makeText(this, "Invalid trigger settings!", 
								Toast.LENGTH_SHORT).show();
			}
		}
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		updateTriggerDesc();
		outState.putString(KEY_TRIG_DESC, mTrigDesc.toString());
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == DIALOG_ID_INVALID_TIME_ALERT) {
			final String msg = "Make sure that the End Time is after the Start Time";

			return new AlertDialog.Builder(this)
				.setTitle("Invalid time settings!")
				.setNegativeButton("Cancel", null)
				.setMessage(msg)
				.create();			
		}
	
		return null;
	}
	
	private void initializeCategories() {
		LocTrigDB db = new LocTrigDB(this);
		db.open();
		
		Cursor c = db.getAllCategories();
		mCategories = new String[c.getCount()];
		
		int i = 0;
		if(c.moveToFirst()) {
			do {
				mCategories[i++] = c.getString(
								   c.getColumnIndexOrThrow(LocTrigDB.KEY_NAME));
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
	}
	
	
	public static void setOnExitListener(ExitListener listener) {
		
		mExitListener = listener;
	}
	
	private void initializeGUI() {
		ListPreference locPref = (ListPreference) getPreferenceScreen()
		 					.findPreference(PREF_KEY_LOCATION);
		CheckBoxPreference rangePref = (CheckBoxPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_ENABLE_RANGE);
		CheckBoxPreference alwaysPref = (CheckBoxPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_TRIGGER_ALWAYS);
		TimePickerPreference startPref = (TimePickerPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_START_TIME);
		TimePickerPreference endPref = (TimePickerPreference) getPreferenceScreen()
							.findPreference(PREF_KEY_END_TIME);
		
		locPref.setValue(mTrigDesc.getLocation());
		locPref.setSummary(mTrigDesc.getLocation());
		
		if(mTrigDesc.isRangeEnabled()) {
			rangePref.setChecked(true);
			
			if(mTrigDesc.shouldTriggerAlways()) {
				alwaysPref.setChecked(true);
			}
			
			startPref.setTime(mTrigDesc.getStartTime());
			endPref.setTime(mTrigDesc.getEndTime());
		}
		
		locPref.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
		rangePref.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
		alwaysPref.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
		startPref.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
		endPref.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
		((Button) findViewById(R.id.trig_edit_done))
				.setEnabled(mAdminMode || TrigUserConfig.editLocationTrigger);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object val) {
		
		if(pref.getKey().equals(PREF_KEY_LOCATION)) {
			
			if(val instanceof String) {
				pref.setSummary((String) val);
			}
		}
		
		return true;
	}
	
	private void updateTriggerDesc() {
		ListPreference locPref = (ListPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_LOCATION);
		CheckBoxPreference rangePref = (CheckBoxPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_ENABLE_RANGE);
		CheckBoxPreference alwaysPref = (CheckBoxPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_TRIGGER_ALWAYS);
		TimePickerPreference startPref = (TimePickerPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_START_TIME);
		TimePickerPreference endPref = (TimePickerPreference) getPreferenceScreen()
									.findPreference(PREF_KEY_END_TIME);
		
		mTrigDesc.setLocation(locPref.getValue());
		//Currently, there is only a global setting for the reentry interval
		mTrigDesc.setMinReentryInterval(LocTrigDesc.getGlobalMinReentryInterval(this));
		
		if(rangePref.isChecked()) {
			mTrigDesc.setRangeEnabled(true);
			mTrigDesc.setTriggerAlways(alwaysPref.isChecked());
			
			mTrigDesc.setStartTime(startPref.getTime());
			mTrigDesc.setEndTime(endPref.getTime());
		}
		else {
			mTrigDesc.setRangeEnabled(false);
			mTrigDesc.setTriggerAlways(false);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		
		case R.id.trig_edit_done:
			if(mExitListener != null) {
				updateTriggerDesc();
				
				if(mTrigDesc.validate()) {
					mExitListener.onDone(this, mTrigId, mTrigDesc.toString());
				}
				else {
					showDialog(DIALOG_ID_INVALID_TIME_ALERT);
					return;
				}
			}
			break;
		}
		
		finish();
	}

}
