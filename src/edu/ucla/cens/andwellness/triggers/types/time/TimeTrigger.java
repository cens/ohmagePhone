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
package edu.ucla.cens.andwellness.triggers.types.time;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.triggers.base.TriggerBase;


public class TimeTrigger extends TriggerBase {
	
	private static final String DEBUG_TAG = "TimeTrigger";
	
	private static final String TRIGGER_TYPE = "TimeTrigger";	
	//TODO localize
	private static final String DISP_NAME = "Time Trigger";
	
	@Override
	public String getTriggerTypeDisplayName() {
		
		return DISP_NAME;
	}

	@Override
	public String getTriggerType() {
		
		return TRIGGER_TYPE;
	}

	@Override
	public boolean hasSettings() {
		
		return false;
	}

	@Override
	public void stopTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context, TimeTrigService.class);
		i.setAction(TimeTrigService.ACTION_REMOVE_TRIGGER);
		i.putExtra(TimeTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(TimeTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void resetTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context, TimeTrigService.class);
		i.setAction(TimeTrigService.ACTION_RESET_TRIGGER);
		i.putExtra(TimeTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(TimeTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void startTrigger(Context context, int trigId, String trigDesc) {
		Intent i = new Intent(context, TimeTrigService.class);
		i.setAction(TimeTrigService.ACTION_SET_TRIGGER);
		i.putExtra(TimeTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(TimeTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}
	
	@Override
	public void launchTriggerCreateActivity(Context context, final String campaignUrn, boolean adminMode) {
		
		TimeTrigEditActivity.setOnExitListener(
					new TimeTrigEditActivity.ExitListener() {
			
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				Log.i(DEBUG_TAG, "TimeTrigger: Saving new trigger: " + trigDesc);
				addNewTrigger(context, campaignUrn, trigDesc);
			}
		});
		
		
		context.startActivity(new Intent(context, 
							  TimeTrigEditActivity.class));
	}

	@Override
	public void launchTriggerEditActivity(Context context, int trigId, String trigDesc,
										  boolean adminMode) {
		
		TimeTrigEditActivity.setOnExitListener(
				new TimeTrigEditActivity.ExitListener() {
		
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				updateTrigger(context, trigId, trigDesc);
			}
		});
	
	
		Intent i = new Intent(context, TimeTrigEditActivity.class);
		i.putExtra(TimeTrigEditActivity.KEY_TRIG_ID, trigId);
		i.putExtra(TimeTrigEditActivity.KEY_TRIG_DESC, trigDesc);
		i.putExtra(TimeTrigEditActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);
	}

	@Override
	public void launchSettingsEditActivity(Context context, boolean adminMode) {
		
	}
	
	@Override
	public void resetSettings(Context context) {
		
	}

	@Override
	public String getDisplaySummary(Context context, String trigDesc) {
		TimeTrigDesc conf = new TimeTrigDesc();
		
		if(!conf.loadString(trigDesc)) {
			return null;
		}
		
		return conf.getRepeatDescription();
	}

	@Override
	public String getDisplayTitle(Context context, String trigDesc) {
		TimeTrigDesc conf = new TimeTrigDesc();
		
		if(!conf.loadString(trigDesc)) {
			return null;
		}
		
		String ret = conf.getTriggerTime().toString();
		
		if(conf.isRandomized()) {
			ret = conf.getRangeStart().toString() 
				  + " - "
				  + conf.getRangeEnd().toString();
		}
		
		return ret;
	}

	@Override
	public int getIcon() {
		return R.drawable.clock;
	}

	@Override
	public JSONObject getPreferences(Context context) {
		return new JSONObject();
	}
}
