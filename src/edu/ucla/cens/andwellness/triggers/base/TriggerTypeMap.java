package edu.ucla.cens.andwellness.triggers.base;

import java.util.Collection;
import java.util.LinkedHashMap;

import edu.ucla.cens.andwellness.triggers.config.TriggerTypesConfig;
import edu.ucla.cens.andwellness.triggers.types.location.LocationTrigger;
import edu.ucla.cens.andwellness.triggers.types.time.TimeTrigger;

/*
 * The map of available trigger types to their concrete class instances.
 * 
 * When a new trigger type is defined, it must be registered here. Each
 * <key, value> entry in the map has the form <String, TriggerBase> where 
 * the key corresponds to the trigger type (String) and the value corresponds
 * to the instance of the concrete trigger (which extends TriggerBase).
 */
public class TriggerTypeMap {
	
	private static LinkedHashMap<String, TriggerBase> mTrigTypeMap = null;
	
	/*
	 * A new trigger type must be registered here. 
	 * 
	 * In order to disable a type of trigger altogether, 
	 * the corresponding boolean flag in ..\config\TriggerTypesConfig
	 * can be set to false. 
	 */
	public TriggerTypeMap() {
		
		mTrigTypeMap = new LinkedHashMap<String, TriggerBase>();
		
		//Time trigger
		if(TriggerTypesConfig.timeTriggers) {
			TriggerBase timeTrig = new TimeTrigger();
			mTrigTypeMap.put(timeTrig.getTriggerType(), timeTrig);
		}
		
		//Location trigger
		if(TriggerTypesConfig.locationTriggers) {
			TriggerBase locTrig = new LocationTrigger();
			mTrigTypeMap.put(locTrig.getTriggerType(), locTrig);
		}
	}

	/*
	 * Get the TriggerBase instance corresponding to a 
	 * type
	 */
	public TriggerBase getTrigger(String trigType) {
		if(mTrigTypeMap == null) {
			return null;
		}
		
		return mTrigTypeMap.get(trigType);
	}
	
	/*
	 * Get TriggerBase instances of all types
	 */
	public Collection<TriggerBase> getAllTriggers() {
		return mTrigTypeMap.values();
	}
	
	/*
	 * Get the list of all registered types
	 */
	public Collection<String> getAllTriggerTypes() {
		return mTrigTypeMap.keySet();
	}
	
}
