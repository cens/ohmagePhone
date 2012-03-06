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

import edu.ucla.cens.systemlog.Log;

import android.content.Context;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.ResponseCache;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
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
			Log.i(TAG, "Cache has already been installed.");
		} else if (responseCache == null) {
			OhmageCache dropCache = new OhmageCache(context);
			ResponseCache.setDefault(dropCache);
		} else {
			Class<? extends ResponseCache> type = responseCache.getClass();
			Log.e(TAG, "Another ResponseCache has already been installed: " + type);
		}
	}

	public static boolean isResponseImageRequest(URI uri) {
		if(uri == null || uri.getHost() == null || uri.getPath() == null)
			return false;
		URI responseUri = URI.create(Config.DEFAULT_SERVER_URL + OhmageApi.IMAGE_READ_PATH);
		return uri.getHost().equals(responseUri.getHost()) && uri.getPath().startsWith(responseUri.getPath());
	}

	public static File getCachedFile(Context context, URI uri) {
		try {
			File parent = (isResponseImageRequest(uri)) ? context.getExternalCacheDir() : context.getCacheDir();
			if(parent == null)
				return null;
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(String.valueOf(uri).getBytes("UTF-8"));
			byte[] output = digest.digest();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < output.length; i++) {
				builder.append(Integer.toHexString(0xFF & output[i]));
			}
			return new File(parent, builder.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.d(TAG, "MD5 algorithm not found");
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			Log.d(TAG, "Unsupported Encoding Exception");
			throw new RuntimeException(e);
		}
	}

	@Override
	protected File getFile(URI uri, String requestMethod, Map<String, List<String>> requestHeaders, Object cookie) {
		return getCachedFile(mContext, uri);
	}

	/**
	 * Check the current cache useage and delete files if needed
	 * @param context
	 * @param maxDiskCacheSize
	 */
	public static void checkCacheUsage(Context context, int maxDiskCacheSize) {
		long size = 0;
		File cacheDir = context.getExternalCacheDir();
		if(cacheDir == null) {
			//sdcard is not available for some reason
			return;
		}
		final File[] fileList = cacheDir.listFiles();
		Arrays.sort(fileList, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				return Long.valueOf(f2.lastModified()).compareTo(
						f1.lastModified());
			}
		});
		for (File file : fileList) {
			size += file.length();
			if (size > maxDiskCacheSize) {
				file.delete();
			}
		}
	}
}
