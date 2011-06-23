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
 * Class containing the compile time constants which decides
 * which all trigger types are to be present in a given build.
 * 
 * New trigger types can add flags here and use them in
 * TriggerTypeMap. This class is only a convenience to 
 * selectively disable and enable trigger types in various
 * releases. A trigger type can be disabled also by commenting
 * the registration in the TriggerTypeMap
 */
public class TriggerTypesConfig {

	//Flag which decides if location triggers must be 
	//present in the build
	public static final boolean locationTriggers = true;
	//Flag which decides if time triggers must be 
	//present in the build
	public static final boolean timeTriggers = true;
}
