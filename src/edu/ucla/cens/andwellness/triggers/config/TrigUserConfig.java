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
package edu.ucla.cens.andwellness.triggers.config;

/*
 * Class containing the compile time constants which define the 
 * behavior of the user interface in non-admin mode
 */
public class TrigUserConfig {
	
	/*
	 * The admin password. Currently, the admin mode
	 * is a mechanism to prevent the user from accidentally
	 * changing the trigger settings.  
	 */
	public static final String adminPass = "0000";
	
	/* UI options in trigger list affected by admin mode */
	public static boolean addTrigers = false;
	public static boolean removeTrigers = false;
	public static boolean editTriggerActions = false;	
	public static boolean editNotificationSettings = false;
	public static boolean editTriggerSettings = true;
	
	/* UI options in location triggers affected by admin mode */
	public static boolean editLocationTrigger = false;
	public static boolean editLocationTriggerSettings = true;
	public static boolean editLocationTriggerPlace = false;
	
	/* UI options in time triggers affected by admin mode */
	public static boolean editTimeTrigger = true;
	public static boolean editTimeTriggerTime = true;
	public static boolean editTimeTriggerRepeat = false;
	public static boolean editTimeTriggerRange = false;
}
