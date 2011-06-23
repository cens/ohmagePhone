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
package edu.ucla.cens.andwellness;

import java.io.File;
import java.io.IOException;


public class Utilities {

	public static class KVPair {
		
		public String key;
		public String value;
		
		public KVPair(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
	
	public static class KVLTriplet {
		
		public String key;
		public String value;
		public String label;
		
		public KVLTriplet(String key, String value, String label) {
			this.key = key;
			this.value = value;
			this.label = label;
		}
	}
	
	public static String stringArrayToString(String [] array, String separator) {
		StringBuilder result = new StringBuilder();
	    if (array.length > 0) {
	        result.append(array[0]);
	        for (int i=1; i<array.length; i++) {
	            result.append(separator);
	            result.append(array[i]);
	        }
	    }
	    return result.toString();
	}
	
	public static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}
	}
}
