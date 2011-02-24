package edu.ucla.cens.andwellness.triggers.types.location;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.triggers.base.TriggerBase;

public class LocationTrigger extends TriggerBase {

	private static final String DEBUG_TAG = "LocationTrigger";
	
	private static final String TRIGGER_TYPE = "LocationTrigger";	
	//TODO localize
	private static final String DISP_NAME = "Location Trigger";
	
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
		
		return true;
	}
	
	@Override
	public String getDisplaySummary(Context context, String trigDesc) {
		LocTrigDesc desc = new LocTrigDesc();
		
		if(!desc.loadString(trigDesc)) {
			return null;
		}
		
		String ret = "";
		if(!desc.isRangeEnabled()) {
			ret = "Always";
		}
		else {
			ret = desc.getStartTime().toString()
					+ " - "
					+ desc.getEndTime().toString();
		}
		
		return ret;
	}

	@Override
	public String getDisplayTitle(Context context, String trigDesc) {
		LocTrigDesc desc = new LocTrigDesc();
		
		if(!desc.loadString(trigDesc)) {
			return null;
		}
		
		return desc.getLocation();
	}

	@Override
	public int getIcon() {
		return R.drawable.map;
	}
	
	@Override
	public void launchTriggerCreateActivity(Context context) {

		LocTrigEditActivity.setOnExitListener(
					new LocTrigEditActivity.ExitListener() {
			
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				addNewTrigger(context, trigDesc);
			}
		});
		
		
		context.startActivity(new Intent(context, 
							  LocTrigEditActivity.class));
	}

	@Override
	public void launchSettingsEditActivity(Context context, boolean adminMode) {
		Intent i = new Intent(context, LocTrigSettingsActivity.class);
		i.putExtra(LocTrigSettingsActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);
	}

	@Override
	public void launchTriggerEditActivity(Context context, int trigId, 
										  String trigDesc, boolean adminMode) {
		LocTrigEditActivity.setOnExitListener(
				new LocTrigEditActivity.ExitListener() {
		
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				updateTrigger(context, trigId, trigDesc);
			}
		});
	
	
		Intent i = new Intent(context, LocTrigEditActivity.class);
		i.putExtra(LocTrigEditActivity.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigEditActivity.KEY_TRIG_DESC, trigDesc);
		i.putExtra(LocTrigEditActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);	
	}

	@Override
	public void removeTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: removeTrigger(" + trigId +
										", " + trigDesc + ")");
		
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_REMOVE_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void resetTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: resetTrigger(" + trigId +
				", " + trigDesc + ")");
		
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_RESET_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void startTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: startTrigger(" + trigId +
				", " + trigDesc + ")");
		
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_START_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

}
