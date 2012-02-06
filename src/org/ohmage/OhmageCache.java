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

import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * The {@link OhmageCache} handles storing campaign icons and response images on the sdcard.
 * 
 * <p> Response images are stored on the external sdcard at {@link Context#getExternalCacheDir()}.
 * Other icons are stored on the internal cache directory given by {@link Context#getCacheDir()} </p>
 * 
 * @author cketcham
 *
 */
public class OhmageCache extends FileResponseCache {

	private static final String TAG = "OhmageCache";

	private final Context mContext;

	public OhmageCache(Context context) {
		mContext = context;
	}

	public static void install(Context context) {
		ResponseCache responseCache = ResponseCache.getDefault();
		if (responseCache instanceof OhmageCache) {
			Log.d(TAG, "Cache has already been installed.");
		} else if (responseCache == null) {
			OhmageCache dropCache = new OhmageCache(context);
			ResponseCache.setDefault(dropCache);
		} else {
			Class<? extends ResponseCache> type = responseCache.getClass();
			Log.e(TAG, "Another ResponseCache has already been installed: " + type);
		}
	}

	public static boolean isResponseImageRequest(URI uri) {
		URI responseUri = URI.create(Config.DEFAULT_SERVER_URL + OhmageApi.IMAGE_READ_PATH);
		return uri.getHost().equals(responseUri.getHost()) && uri.getPath().startsWith(responseUri.getPath());
	}

	public static File getCachedFile(Context context, URI uri) {
		try {
			File parent = (isResponseImageRequest(uri)) ? context.getExternalCacheDir() : context.getCacheDir();
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(String.valueOf(uri).getBytes("UTF-8"));
			byte[] output = digest.digest();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < output.length; i++) {
				builder.append(Integer.toHexString(0xFF & output[i]));
			}
			return new File(parent, builder.toString());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected File getFile(URI uri, String requestMethod, Map<String, List<String>> requestHeaders, Object cookie) {
		return getCachedFile(mContext, uri);
	}

	/**
	 * This content handler fixes the bug caused by the HttpsURLConnectionImpl class
	 * where it throws a null pointer after it gets the cached file
	 * @author cketcham
	 *
	 */
	public static class OhmageCacheBitmapContentHandler extends BitmapContentHandler {

		@Override
		public Bitmap getContent(URLConnection connection) throws IOException {
			// First we will try to use the default behavior of the BitmapContentHandler
			try {
				return super.getContent(connection);
			} catch(NullPointerException e) {
				// There is a bug with the implementation of caching requests in the HttpsURLConnectionImpl object
				// I try to recover from it here using reflection
				Log.d(TAG, "caught bug from HttpsURLConnectionImpl " + e);
				try {
					Field httpsEngineField = connection.getClass().getDeclaredField("httpsEngine");
					if(!httpsEngineField.isAccessible())
						httpsEngineField.setAccessible(true);
					Object httpsEngine = httpsEngineField.get(connection);
					Field cacheResponse = httpsEngine.getClass().getSuperclass().getDeclaredField("cacheResponse");
					if(!cacheResponse.isAccessible())
						cacheResponse.setAccessible(true);
					CacheResponse response = (CacheResponse) cacheResponse.get(httpsEngine);

					InputStream input = response.getBody();
					try {
						Bitmap bitmap = BitmapFactory.decodeStream(input);
						if (bitmap == null) {
							throw new IOException("Image could not be decoded");
						}
						return bitmap;
					} finally {
						input.close();
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
			}
		}
	}
}
