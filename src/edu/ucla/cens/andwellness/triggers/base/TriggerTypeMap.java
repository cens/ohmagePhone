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
