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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.ucla.cens.systemlog.Log;

public class AndWellnessApi {
	
	private static final String TAG = "AndWellnessApi";
	
	//private static final String SERVER_URL = "https://dev.andwellness.org/";
	//private String serverUrl;
	//private String mBaseServerUrl = "https://dev1.andwellness.org/";
	//public static final String BASE_SERVER_URL = "https://dev1.andwellness.org/";
	private static final String AUTHENTICATE_PATH = "app/user/auth";
	private static final String MOBILITY_UPLOAD_PATH = "app/mobility/upload";
	private static final String SURVEY_UPLOAD_PATH = "app/survey/upload";
	private static final String IMAGE_UPLOAD_PATH = "app/image/upload";
	private static final String CAMPAIGN_READ_PATH = "app/campaign/read";
	
	public AndWellnessApi(Context context) {
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
//		serverUrl = prefs.getServerUrl();
	}
	
	public static enum Result {
		SUCCESS,
		FAILURE,
		HTTP_ERROR,
		INTERNAL_ERROR
	}
	
	public static enum Error {
		
	}
	
	public static class AuthenticateResponse {
		private Result mResult;
		private String mHashedPassword;
		private String[] mErrorCodes;
		
		public AuthenticateResponse(Result result, String hashedPassword, String[] errorCodes) {
			mResult = result;
			mHashedPassword = hashedPassword;
			mErrorCodes = errorCodes;
		}
		
		public Result getResult() {
			return mResult;
		}
		
		public String getHashedPassword() {
			return mHashedPassword;
		}
		
		public String[] getErrorCodes() {
			return mErrorCodes;
		}

		public void setResult(Result result) {
			this.mResult = result;
		}
		
		public void setHashedPassword(String hashedPassword) {
			this.mHashedPassword = hashedPassword;
		}

		public void setErrorCodes(String[] errorCodes) {
			this.mErrorCodes = errorCodes;
		}
	}
	
	public static class UploadResponse {
		private Result mResult;
		private String[] mErrorCodes;
		
		public UploadResponse(Result result, String[] errorCodes) {
			mResult = result;
			mErrorCodes = errorCodes;
		}
		
		public Result getResult() {
			return mResult;
		}
		
		public String[] getErrorCodes() {
			return mErrorCodes;
		}

		public void setResult(Result result) {
			this.mResult = result;
		}

		public void setErrorCodes(String[] errorCodes) {
			this.mErrorCodes = errorCodes;
		}
	}
	
	public static class ReadResponse {
		private Result mResult;
		private JSONObject mData;
		private JSONObject mMetadata;
		private String[] mErrorCodes;
		
		public ReadResponse(Result result, JSONObject data, JSONObject metadata, String[] errorCodes) {
			mResult = result;
			mData = data;
			mMetadata = metadata;
			mErrorCodes = errorCodes;
		}
		
		public Result getResult() {
			return mResult;
		}
		
		public JSONObject getData() {
			return mData;
		}
		
		public JSONObject getMetadata() {
			return mMetadata;
		}
		
		public String[] getErrorCodes() {
			return mErrorCodes;
		}

		public void setResult(Result result) {
			this.mResult = result;
		}
		
		public void setData(JSONObject data) {
			this.mData = data;
		}
		
		public void setMetadata(JSONObject metadata) {
			this.mMetadata = metadata;
		}

		public void setErrorCodes(String[] errorCodes) {
			this.mErrorCodes = errorCodes;
		}
	}

	public AuthenticateResponse authenticate(String serverUrl, String username, String password, String client) {

		final boolean GZIP = false;
		
		String url = serverUrl + AUTHENTICATE_PATH;
		
		try {
        	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            nameValuePairs.add(new BasicNameValuePair("client", client));
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseAuthenticateResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new AuthenticateResponse(Result.INTERNAL_ERROR, null, null);
		}
	}
	
	public UploadResponse mobilityUpload(String serverUrl, String username, String hashedPassword, String client, String data) {

		final boolean GZIP = true;
		
		String url = serverUrl + MOBILITY_UPLOAD_PATH;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("user", username));
	        nameValuePairs.add(new BasicNameValuePair("password", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("client", client));
	        nameValuePairs.add(new BasicNameValuePair("data", data));
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseUploadResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new UploadResponse(Result.INTERNAL_ERROR, null);
		}
	}
	
	public UploadResponse surveyUpload(String serverUrl, String username, String hashedPassword, String client, String campaignUrn, String campaignCreationTimestamp, String data) {
		
		final boolean GZIP = true;
		
		String url = serverUrl + SURVEY_UPLOAD_PATH;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("campaign_urn", campaignUrn));
	        nameValuePairs.add(new BasicNameValuePair("campaign_creation_timestamp", campaignCreationTimestamp));  
	        nameValuePairs.add(new BasicNameValuePair("user", username));
	        nameValuePairs.add(new BasicNameValuePair("password", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("client", client));
	        nameValuePairs.add(new BasicNameValuePair("data", data));
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseUploadResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new UploadResponse(Result.INTERNAL_ERROR, null);
		}
	}
	
	public UploadResponse mediaUpload(String serverUrl, String username, String hashedPassword, String client, String campaignUrn, String campaignCreationTimestamp, String uuid, File data) {
		
		final boolean GZIP = false;
		
		String url = serverUrl + IMAGE_UPLOAD_PATH;
		
		try {
			MultipartEntity multipartEntity = new MultipartEntity();
	    	multipartEntity.addPart("campaign_urn", new StringBody(campaignUrn));
	    	multipartEntity.addPart("campaign_creation_timestamp", new StringBody(campaignCreationTimestamp));
	    	multipartEntity.addPart("user", new StringBody(username));
	    	multipartEntity.addPart("password", new StringBody(hashedPassword));
	    	multipartEntity.addPart("client", new StringBody(client));
	    	multipartEntity.addPart("id", new StringBody(uuid));
	    	multipartEntity.addPart("data", new FileBody(data, "image/jpeg"));
			
			return parseUploadResponse(doHttpPost(url, multipartEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new UploadResponse(Result.INTERNAL_ERROR, null);
		}
	}
	
	public ReadResponse campaignRead(String serverUrl, String username, String hashedPassword, String client, String outputFormat, String campaignUrnList) {
		
		final boolean GZIP = false;
		
		String url = serverUrl + CAMPAIGN_READ_PATH;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("user", username));
	        nameValuePairs.add(new BasicNameValuePair("password", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("client", client));
	        nameValuePairs.add(new BasicNameValuePair("output_format", outputFormat));
	        if (campaignUrnList != null) {
	        	nameValuePairs.add(new BasicNameValuePair("campaign_urn_list", campaignUrnList));
	        }
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseReadResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new ReadResponse(Result.INTERNAL_ERROR, null, null, null);
		}
	}
	
	private HttpResponse doHttpPost(String url, HttpEntity requestEntity, boolean gzip) {
		
		HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setStaleCheckingEnabled(params, false);
	    HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
	    HttpConnectionParams.setSoTimeout(params, 20 * 1000);
	    HttpConnectionParams.setSocketBufferSize(params, 8192);
	    HttpClientParams.setRedirecting(params, false);
	    SchemeRegistry schemeRegistry = new SchemeRegistry();
	    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	    schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
	    ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
	    
	    HttpClient httpClient = new DefaultHttpClient(manager, params);
	    HttpPost httpPost = new HttpPost(url);
		
	    
	    	if (gzip) {
	    		try {
		    		//InputStream is = requestEntity.getContent();
			        
		        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        //BufferedOutputStream zipper = new BufferedOutputStream(new GZIPOutputStream(baos));
			        GZIPOutputStream zipper = new GZIPOutputStream(baos);
			        
			        requestEntity.writeTo(zipper);
			        
			       /* byte [] data2 = new byte[(int)formEntity.getContentLength()]; 
			        is.read(data2);
			        
			        String testing = new String(data2);
			        Log.i("api", testing);
			        zipper.write(data2);*/
			        
			        /*int bufferSize = 1024;
			        byte [] buffer = new byte [bufferSize];
			        int len = 0;
			        String fullString = "";
			        while ((len = is.read(buffer, 0, bufferSize)) != -1) {
			        	zipper.write(buffer, 0, len);
			        	//fullString += new String(buffer, 0, len);
			        	Log.i("api", new String(buffer, 0, len));
			        }
			        Log.i("api", fullString);
			        
			        is.close();*/
			        //zipper.flush();
			        //zipper.finish();
			        zipper.close();
			        
			        
			        ByteArrayEntity byteEntity = new ByteArrayEntity(baos.toByteArray());
			        //baos.close();
			        byteEntity.setContentEncoding("gzip");
			        		        
			        httpPost.setEntity(byteEntity);
	    		} catch (IOException e) {
	    			Log.e(TAG, "Unable to gzip entity, using unzipped entity", e);
	    			httpPost.setEntity(requestEntity);
	    		}
		        
	        } else {
	        	httpPost.setEntity(requestEntity);
	        }
	    
		        
        try {
			return httpClient.execute(httpPost);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException while executing httpPost", e);
	    	return null;
		} catch (IOException e) {
			Log.e(TAG, "IOException while executing httpPost", e);
	    	return null;
		}
	}
	
	private AuthenticateResponse parseAuthenticateResponse(HttpResponse response) {
		Result result = Result.HTTP_ERROR;
		String hashedPassword = null;
		String[] errorCodes = null;
		
		if (response != null) {
        	Log.i(TAG, response.getStatusLine().toString());
        	if (response.getStatusLine().getStatusCode() == 200) {
        		HttpEntity responseEntity = response.getEntity();
        		if (responseEntity != null) {
        			try {
	        			String content = EntityUtils.toString(responseEntity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
					
						rootJson = new JSONObject(content);
						if (rootJson.getString("result").equals("success")) {
							result = Result.SUCCESS;
							if (rootJson.has("hashed_password")) {
								hashedPassword = rootJson.getString("hashed_password");
							}
							
						} else {
							result = Result.FAILURE;
							JSONArray errorsJsonArray = rootJson.getJSONArray("errors");
							int errorCount = errorsJsonArray.length();
							errorCodes = new String[errorCount];
							for (int i = 0; i < errorCount; i++) {
								errorCodes[i] = errorsJsonArray.getJSONObject(i).getString("code");
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, "Problem parsing response json", e);
						result = Result.INTERNAL_ERROR;
					} catch (IOException e) {
						Log.e(TAG, "Problem reading response body", e);
						result = Result.INTERNAL_ERROR;
					}
				} else {
					Log.e(TAG, "No response entity in response");
        			result = Result.HTTP_ERROR;
        		}
        		
        	} else {
        		Log.e(TAG, "Returned status code: " + String.valueOf(response.getStatusLine().getStatusCode()));
        		result = Result.HTTP_ERROR;
        	}
        	
        } else {
        	Log.e(TAG, "Response is null");
        	result = Result.HTTP_ERROR;
        }
		
		return new AuthenticateResponse(result, hashedPassword, errorCodes);
	}
	
	private UploadResponse parseUploadResponse(HttpResponse response) {
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		if (response != null) {
        	Log.i(TAG, response.getStatusLine().toString());
        	if (response.getStatusLine().getStatusCode() == 200) {
        		HttpEntity responseEntity = response.getEntity();
        		if (responseEntity != null) {
        			try {
	        			String content = EntityUtils.toString(responseEntity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
					
						rootJson = new JSONObject(content);
						if (rootJson.getString("result").equals("success")) {
							result = Result.SUCCESS;
						} else {
							result = Result.FAILURE;
							JSONArray errorsJsonArray = rootJson.getJSONArray("errors");
							int errorCount = errorsJsonArray.length();
							errorCodes = new String[errorCount];
							for (int i = 0; i < errorCount; i++) {
								errorCodes[i] = errorsJsonArray.getJSONObject(i).getString("code");
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, "Problem parsing response json", e);
						result = Result.INTERNAL_ERROR;
					} catch (IOException e) {
						Log.e(TAG, "Problem reading response body", e);
						result = Result.INTERNAL_ERROR;
					}
				} else {
					Log.e(TAG, "No response entity in response");
        			result = Result.HTTP_ERROR;
        		}
        		
        	} else {
        		Log.e(TAG, "Returned status code: " + String.valueOf(response.getStatusLine().getStatusCode()));
        		result = Result.HTTP_ERROR;
        	}
        	
        } else {
        	Log.e(TAG, "Response is null");
        	result = Result.HTTP_ERROR;
        }
		
		return new UploadResponse(result, errorCodes);
	}
	
	private ReadResponse parseReadResponse(HttpResponse response) {
		Result result = Result.HTTP_ERROR;
		JSONObject data = null;
		JSONObject metadata = null;
		String[] errorCodes = null;
		
		if (response != null) {
        	Log.i(TAG, response.getStatusLine().toString());
        	if (response.getStatusLine().getStatusCode() == 200) {
        		HttpEntity responseEntity = response.getEntity();
        		if (responseEntity != null) {
        			try {
	        			String content = EntityUtils.toString(responseEntity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
					
						rootJson = new JSONObject(content);
						if (rootJson.getString("result").equals("success")) {
							result = Result.SUCCESS;
							if (rootJson.has("data")) {
								data = rootJson.getJSONObject("data");
							}
							if (rootJson.has("metadata")) {
								metadata = rootJson.getJSONObject("metadata");
							}
							
						} else {
							result = Result.FAILURE;
							JSONArray errorsJsonArray = rootJson.getJSONArray("errors");
							int errorCount = errorsJsonArray.length();
							errorCodes = new String[errorCount];
							for (int i = 0; i < errorCount; i++) {
								errorCodes[i] = errorsJsonArray.getJSONObject(i).getString("code");
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, "Problem parsing response json", e);
						result = Result.INTERNAL_ERROR;
					} catch (IOException e) {
						Log.e(TAG, "Problem reading response body", e);
						result = Result.INTERNAL_ERROR;
					}
				} else {
					Log.e(TAG, "No response entity in response");
        			result = Result.HTTP_ERROR;
        		}
        		
        	} else {
        		Log.e(TAG, "Returned status code: " + String.valueOf(response.getStatusLine().getStatusCode()));
        		result = Result.HTTP_ERROR;
        	}
        	
        } else {
        	Log.e(TAG, "Response is null");
        	result = Result.HTTP_ERROR;
        }
		
		return new ReadResponse(result, data, metadata, errorCodes);
	}
}
