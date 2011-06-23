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
 * behavior of trigger notification
 */
public class NotifConfig {

	//Default notification description
	public static final String defaultConfig = 
		"{\"duration\": 60, \"suppression\": 30}";
	
	//The default value of repeat reminder
	public static final int defaultRepeat = 5; //minutes
	//Maximum value of notification duration
	public static final int maxDuration = 60; //minutes
	//Maximum value of suppression window. If the survey
	//has already been taken within this window, the 
	//notification will be suppressed
	public static final int maxSuppression = 60; //minutes
}
