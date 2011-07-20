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
	private static final String AUTHENTICATE_TOKEN_PATH = "app/user/auth_token";
	private static final String MOBILITY_UPLOAD_PATH = "app/mobility/upload";
	private static final String SURVEY_UPLOAD_PATH = "app/survey/upload";
	private static final String IMAGE_UPLOAD_PATH = "app/image/upload";
	private static final String CAMPAIGN_READ_PATH = "app/campaign/read";
	private static final String SURVEYRESPONSE_READ_PATH = "app/survey_response/read";
	private static final String IMAGE_READ_PATH = "app/image/read";
	
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
	
	public static abstract class Response {
		protected Result mResult;
		protected String[] mErrorCodes;
		
		public Response() {
			// do-nothing constructor so we can create instances via reflection
		}
		
		public Response(Result result, String[] errorCodes) {
			mResult = result;
			mErrorCodes = errorCodes;
		}
		
		public void setResponseStatus(Result result, String[] errorCodes) {
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

		public abstract void populateFromJSON(JSONObject rootJson) throws JSONException;
	}
	
	public static class AuthenticateResponse extends Response {
		private String mHashedPassword;
		private String mToken;
		
		public AuthenticateResponse(Result result, String hashedPassword, String token, String[] errorCodes) {
			super(result, errorCodes);
			mHashedPassword = hashedPassword;
			mToken = token;
		}
		
		public String getHashedPassword() {
			return mHashedPassword;
		}
		
		public void setHashedPassword(String hashedPassword) {
			this.mHashedPassword = hashedPassword;
		}

		public void setToken(String mToken) {
			this.mToken = mToken;
		}

		public String getToken() {
			return mToken;
		}

		@Override
		public void populateFromJSON(JSONObject rootJson) throws JSONException {
			if (rootJson.has("hashed_password"))
				mHashedPassword = rootJson.getString("hashed_password");
			
			if (rootJson.has("token"))
				mToken = rootJson.getString("token");
		}
	}
	
	public static class UploadResponse extends Response {
		public UploadResponse(Result result, String[] errorCodes) {
			super(result, errorCodes);
		}

		@Override
		public void populateFromJSON(JSONObject rootJson) {
			// does nothing, since we don't use the response
		}
	}
	
	public static class CampaignReadResponse extends Response {
		protected JSONObject mData;
		protected JSONObject mMetadata;

		public JSONObject getData() {
			return mData;
		}
		
		public JSONObject getMetadata() {
			return mMetadata;
		}
		
		public void setData(JSONObject data) {
			this.mData = data;
		}
		
		public void setMetadata(JSONObject metadata) {
			this.mMetadata = metadata;
		}

		@Override
		public void populateFromJSON(JSONObject rootJson) throws JSONException {
			if (rootJson.has("data"))
				mData = rootJson.getJSONObject("data");
			if (rootJson.has("metadata"))
				mMetadata = rootJson.getJSONObject("metadata");
		}
	}
	
	public static class SurveyReadResponse extends Response {
		protected JSONArray mData;
		protected JSONObject mMetadata;

		public JSONArray getData() {
			return mData;
		}
		
		public JSONObject getMetadata() {
			return mMetadata;
		}
		
		public void setData(JSONArray data) {
			this.mData = data;
		}
		
		public void setMetadata(JSONObject metadata) {
			this.mMetadata = metadata;
		}

		@Override
		public void populateFromJSON(JSONObject rootJson) throws JSONException {
			if (rootJson.has("data"))
				mData = rootJson.getJSONArray("data");
			if (rootJson.has("metadata"))
				mMetadata = rootJson.getJSONObject("metadata");
		}
	}
	
	public class ImageReadResponse extends Response {
		private byte[] data;
		
		@Override
		public void populateFromJSON(JSONObject rootJson) throws JSONException {
			// do nothing, b/c there's no json data
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		public byte[] getData() {
			return data;
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
			return new AuthenticateResponse(Result.INTERNAL_ERROR, null, null, null);
		}
	}
	
	public AuthenticateResponse authenticateToken(String serverUrl, String username, String password, String client) {

		final boolean GZIP = false;
		
		String url = serverUrl + AUTHENTICATE_TOKEN_PATH;
		
		try {
        	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            nameValuePairs.add(new BasicNameValuePair("client", client));
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseAuthenticateResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			return new AuthenticateResponse(Result.INTERNAL_ERROR, null, null, null);
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
	
	public CampaignReadResponse campaignRead(String serverUrl, String username, String hashedPassword, String client, String outputFormat, String campaignUrnList) {
		
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
			
			return (CampaignReadResponse)parseReadResponse(doHttpPost(url, formEntity, GZIP), CampaignReadResponse.class);
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			CampaignReadResponse candidate = new CampaignReadResponse();
			candidate.setResponseStatus(Result.INTERNAL_ERROR, null);
			return candidate;
		}
	}
	
	/**
	 * Returns survey responses for the specified campaign, with a number of parameters to filter the result.<br><br>
	 * 
	 * Note that all results are subject to the specified user's permissions; even if results for other
	 * users are requested, they won't be returned if the specified user does not have sufficient permission to view them.<br><br>
	 * 
	 * Consult <a href="http://www.lecs.cs.ucla.edu/wikis/andwellness/index.php/AndWellness_Survey_Manipulation_2.4#Survey_Response__Read">the documentation on survey_response_read</a> for more information.
	 * @param serverUrl the url of the server to contact for the list
	 * @param username username of a valid user; will constrain the result in keeping with the user's permissions
	 * @param hashedPassword hashed password of the aforementioned user
	 * @param client the client used to retrieve the results, generally "android"
	 * @param campaignUrn the urn of the campaign for which to retrieve survey results
	 * @param userList a comma-separated list of usernames for which to return responses, or "urn:ohmage:special:all" (if null, "all" is assumed)
	 * @param surveyIdList a comma-separated list of surveys for which to return results, or "urn:ohmage:special:all" (if null, "all" is assumed)
	 * @param columnList a comma-separated lits of column values as specified in the docuemntation, or "urn:ohmage:special:all" (if null, "all" is assumed)
	 * @param outputFormat one of json-rows, json-columns, or csv (FIXME: this method can't handle csv yet)
	 * @param startDate must be present if end_date is present: allows querying against a date range. 
	 * @param endDate must be present if start_date is present; allows querying against a date range 
	 * @return an instance of type {@link ReadResponse} containing the resulting data; note that the Object returned by getData() is a JSONArray, not a JSONObject
	 */
	public SurveyReadResponse surveyResponseRead(String serverUrl,
			String username,
			String hashedPassword,
			String client,
			String campaignUrn,
			String userList,
			String surveyIdList,
			String columnList,
			String outputFormat,
			String startDate,
			String endDate) {
		
		final boolean GZIP = false;
		
		String url = serverUrl + SURVEYRESPONSE_READ_PATH;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("user", username));
	        nameValuePairs.add(new BasicNameValuePair("password", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("client", client));
	        nameValuePairs.add(new BasicNameValuePair("campaign_urn", campaignUrn));
	        nameValuePairs.add(new BasicNameValuePair("user_list", (userList != null)?userList:"urn:ohmage:special:all"));
	        nameValuePairs.add(new BasicNameValuePair("survey_id_list", (surveyIdList != null)?surveyIdList:"urn:ohmage:special:all"));
	        nameValuePairs.add(new BasicNameValuePair("column_list", (columnList != null)?columnList:"urn:ohmage:special:all"));
	        nameValuePairs.add(new BasicNameValuePair("output_format", outputFormat));
	        
	        if (startDate != null && endDate != null) {
	        	nameValuePairs.add(new BasicNameValuePair("start_date", startDate));
	        	nameValuePairs.add(new BasicNameValuePair("end_date", endDate));
	        }
	        
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return (SurveyReadResponse)parseReadResponse(doHttpPost(url, formEntity, GZIP), SurveyReadResponse.class);
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			SurveyReadResponse candidate = new SurveyReadResponse();
			candidate.setResponseStatus(Result.INTERNAL_ERROR, null);
			return candidate;
		}
	}
	
	// same as above, except some parameters are substituted with their default values.
	// the defaults here retrieve all surveys for any users to whom we have access
	public SurveyReadResponse surveyResponseRead(String serverUrl,
			String username,
			String hashedPassword,
			String client,
			String campaignUrn,
			String columnList,
			String outputFormat) {
		return surveyResponseRead(serverUrl, username, hashedPassword, client, campaignUrn, null, null, columnList, outputFormat, null, null);
	}
	
	/**
	 * Returns the image data associated with a given image id.
	 * 
	 * @param serverUrl the url of the server to contact for the image data
	 * @param username username of a valid user; will constrain the result in keeping with the user's permissions
	 * @param hashedPassword hashed password of the aforementioned user
	 * @param client the client used to retrieve the results, generally "android"
	 * @param campaignUrn the urn of the campaign for which to retrieve survey results
	 * @param owner the owner of the image for which we want the data
	 * @param id the UUID of the image
	 * @param size optional; if specified, must be "small" (if null, not passed)
	 * @return
	 */
	public ImageReadResponse imageRead(String serverUrl,
			String username,
			String hashedPassword,
			String client,
			String campaignUrn,
			String owner,
			String id,
			String size) {
		
		final boolean GZIP = false;
		
		String url = serverUrl + IMAGE_READ_PATH;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("user", username));
	        nameValuePairs.add(new BasicNameValuePair("password", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("client", client));
	        nameValuePairs.add(new BasicNameValuePair("campaign_urn", campaignUrn));
	        nameValuePairs.add(new BasicNameValuePair("owner", owner));
	        nameValuePairs.add(new BasicNameValuePair("id", id));
	        if (size != null) nameValuePairs.add(new BasicNameValuePair("size", size));
	        
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
			
			return parseImageReadResponse(doHttpPost(url, formEntity, GZIP));
		} catch (IOException e) {
			Log.e(TAG, "IOException while creating http entity", e);
			ImageReadResponse candidate = new ImageReadResponse();
			candidate.setResponseStatus(Result.INTERNAL_ERROR, null);
			return candidate;
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
		String authToken = null;
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
							
							if (rootJson.has("token")) {
								authToken = rootJson.getString("token");
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
		
		return new AuthenticateResponse(result, hashedPassword, authToken, errorCodes);
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
	
	private Response parseReadResponse(HttpResponse response, Class<? extends Response> outputType) {
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		// the response object that will be returned; its type is decided by outputType
		// it's also populated by a call to populateFromJSON() which transforms the server response into the Response object
		Response candidate;
		
		try {
			candidate = outputType.newInstance();
		}
		catch (Exception e) {
			Log.e(TAG, "Problem instantiating response type", e);
			return null;
		}
		
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
							
							// allow the output type to determine how to extract data from the json collection
							candidate.populateFromJSON(rootJson);
							
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
		
		candidate.setResponseStatus(result, errorCodes);

		return candidate;
	}
	
	private ImageReadResponse parseImageReadResponse(HttpResponse response) {
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		ImageReadResponse candidate = new ImageReadResponse();
		
		if (response != null) {
        	Log.i(TAG, response.getStatusLine().toString());
        	if (response.getStatusLine().getStatusCode() == 200) {
        		HttpEntity responseEntity = response.getEntity();
        		if (responseEntity != null) {
        			try {
        				if (true) {
        					// it's the image data!
        					result = Result.SUCCESS;
            				
            				// dealing with raw image data here. hmm.
    	        			byte[] content = EntityUtils.toByteArray(responseEntity);
    	        			candidate.setData(content);
        				}
        				else
        				{
        					// it was a JSON error instead
        					result = Result.FAILURE;

							try {
								JSONObject rootJson = new JSONObject(EntityUtils.toString(responseEntity));								
								JSONArray errorsJsonArray = rootJson.getJSONArray("errors");
								
								int errorCount = errorsJsonArray.length();
								errorCodes = new String[errorCount];
								for (int i = 0; i < errorCount; i++) {
									errorCodes[i] = errorsJsonArray.getJSONObject(i).getString("code");
								}
							}
							catch (JSONException e) {
								Log.e(TAG, "Problem parsing response json", e);
								result = Result.INTERNAL_ERROR;
							}
        				}
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
		
		candidate.setResponseStatus(result, errorCodes);

		return candidate;
	}
}
