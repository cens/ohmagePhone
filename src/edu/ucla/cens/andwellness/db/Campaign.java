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
package edu.ucla.cens.andwellness.db;

public class Campaign {
	
	public static final String _ID = "_id";
	public static final String URN = "urn";
	public static final String NAME = "name";
	public static final String CREATION_TIMESTAMP = "creationTimestamp";
	public static final String DOWNLOAD_TIMESTAMP = "downloadTimestamp";
	public static final String CONFIGURATION_XML = "configuration_xml";

	public long _id;
	public String mUrn;
	public String mName;
	public String mCreationTimestamp;
	public String mDownloadTimestamp;
	public String mXml;
}
