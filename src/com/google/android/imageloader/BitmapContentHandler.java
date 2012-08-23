/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.CacheResponse;
import java.net.ContentHandler;
import java.net.URLConnection;

/**
 * A {@link ContentHandler} that decodes a {@link Bitmap} from a
 * {@link URLConnection}.
 * <p>
 * The implementation includes a work-around for <a
 * href="http://code.google.com/p/android/issues/detail?id=6066">Issue 6066</a>.
 * <p>
 * An {@link IOException} is thrown if there is a decoding exception.
 */
public class BitmapContentHandler extends ContentHandler {
    @Override
    public Bitmap getContent(URLConnection connection) throws IOException {
    	InputStream input = null;
        try {
            input = connection.getInputStream();
        	input = new BlockingFilterInputStream(input);
            return createBitmap(input);
        } catch(NullPointerException e) {
    		// There is a bug with the implementation of caching requests in the HttpsURLConnectionImpl object
    		// I try to recover from it here using reflection
    		try {
    			Field httpsEngineField = connection.getClass().getDeclaredField("httpsEngine");
    			if(!httpsEngineField.isAccessible())
    				httpsEngineField.setAccessible(true);
    			Object httpsEngine = httpsEngineField.get(connection);
    			Field cacheResponse = httpsEngine.getClass().getSuperclass().getDeclaredField("cacheResponse");
    			if(!cacheResponse.isAccessible())
    				cacheResponse.setAccessible(true);
    			CacheResponse response = (CacheResponse) cacheResponse.get(httpsEngine);

    			InputStream input2 = response.getBody();
    			try {
    				return createBitmap(input2);
    			} finally {
    				input2.close();
    			}
    		} catch (SecurityException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		} catch (NoSuchFieldException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		} catch (IllegalArgumentException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		} catch (IllegalAccessException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
    		return null;
        } finally {
        	if(input != null)
        		input.close();
        }
    }

    /**
     * Actually creates the bitmap from the imput stream
     * @param input
     * @return the bitmap
     * @throws IOException
     */
    private Bitmap createBitmap(InputStream input) throws IOException {
		Bitmap bitmap = BitmapFactory.decodeStream(input);
		if (bitmap == null) {
			throw new IOException("Image could not be decoded");
		}
		return bitmap;
    }
}