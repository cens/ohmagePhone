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
package org.ohmage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


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

	public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
	}

	public static Bitmap decodeFile(File f, int maxSize){
		Bitmap b = null;
		try {
			//Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;

			FileInputStream fis = new FileInputStream(f);
			BitmapFactory.decodeStream(fis, null, o);
			fis.close();

			int scale = 1;
			if (o.outHeight > maxSize || o.outWidth > maxSize) {
				scale = (int)Math.pow(2, (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
			}

			//Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			fis = new FileInputStream(f);
			b = BitmapFactory.decodeStream(fis, null, o2);
			fis.close();
		} catch (IOException e) {
		}
		return b;
	}
}
