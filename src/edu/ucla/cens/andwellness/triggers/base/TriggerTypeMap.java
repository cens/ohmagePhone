package edu.ucla.cens.andwellness.triggers.base;

import java.util.Collection;
import java.util.LinkedHashMap;

import edu.ucla.cens.andwellness.triggers.types.location.LocationTrigger;
import edu.ucla.cens.andwellness.triggers.types.time.TimeTrigger;

/*
 * The map of available trigger types to the concrete class instances.
 * When a new trigger type is defined, it must be registered here. Each
 * <key, value> entry in the map has the form <String, TriggerBase> where 
 * the key corresponds to the trigger type (String) and the value corresponds
 * to the instance of the concrete trigger (which extends TriggerBase).
 */
public class TriggerTypeMap {
	
	private static LinkedHashMap<String, TriggerBase> mTrigTypeMap = null;
	
	public TriggerTypeMap() {
		
		mTrigTypeMap = new LinkedHashMap<String, TriggerBase>();
		
		//Time trigger
		TriggerBase timeTrig = new TimeTrigger();
		mTrigTypeMap.put(timeTrig.getTriggerType(), timeTrig);
		
		//Location trigger
		TriggerBase locTrig = new LocationTrigger();
		mTrigTypeMap.put(locTrig.getTriggerType(), locTrig);
	}

	public TriggerBase getTrigger(String trigType) {
		if(mTrigTypeMap == null) {
			return null;
		}
		
		return mTrigTypeMap.get(trigType);
	}
	
	public Collection<TriggerBase> getAllTriggers() {
		return mTrigTypeMap.values();
	}
	
	public Collection<String> getAllTriggerTypes() {
		return mTrigTypeMap.keySet();
	}
	
}
