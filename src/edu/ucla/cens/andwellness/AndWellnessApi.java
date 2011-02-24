package edu.ucla.cens.andwellness;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.apache.http.entity.AbstractHttpEntity;
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

import android.os.Environment;
import android.util.Log;

public class AndWellnessApi {
	
	private static final String TAG = "AndWellnessApi";
	
	private static final String SERVER_URL = "https://dev.andwellness.org/";
	
	public static enum Result {
		SUCCESS,
		FAILURE,
		HTTP_ERROR,
		INTERNAL_ERROR
	}
	
	public static enum Error {
		
	}
	
	public static class ServerResponse {
		private Result mResult;
		private String[] mErrorCodes;
		
		public ServerResponse(Result result, String[] errorCodes) {
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

	public static ServerResponse authenticate(String username, String hashedPassword, String client) {

		final boolean GZIP = false;
		
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		String url = SERVER_URL + "app/auth";
		
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
	    
	    
	    
	    try {  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("u", username));
	        nameValuePairs.add(new BasicNameValuePair("p", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("ci", client));
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
	        
	        if (GZIP) {
	        	InputStream is = formEntity.getContent();
		        
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        OutputStream zipper = new GZIPOutputStream(baos);
		        int bufferSize = 1024;
		        byte [] buffer = new byte [bufferSize];
		        int len = 0;
		        while ((len = is.read(buffer, 0, bufferSize)) != -1) {
		        	zipper.write(buffer, 0, len);
		        }
		        zipper.flush();
		        zipper.close();
		        
		        ByteArrayEntity byteEntity = new ByteArrayEntity(baos.toByteArray());
		        byteEntity.setContentEncoding("gzip");
		        		        
		        httpPost.setEntity(byteEntity);
		        
	        } else {
	        	httpPost.setEntity(formEntity);
	        }
	        
	        HttpResponse response = httpClient.execute(httpPost);
	        
	        if (response != null) {
	        	Log.i(TAG, response.getStatusLine().toString());
	        	if (response.getStatusLine().getStatusCode() == 200) {
	        		HttpEntity entity = response.getEntity();
	        		if (entity != null) {
	        			String content = EntityUtils.toString(entity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
						try {
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
							// TODO Auto-generated catch block
							e.printStackTrace();
							result = Result.INTERNAL_ERROR;
						}
						
	        		} else {
	        			result = Result.HTTP_ERROR;
	        		}
	        		
	        	} else {
	        		result = Result.HTTP_ERROR;
	        	}
	        	
	        } else {
	        	result = Result.HTTP_ERROR;
	        }
	        
	    } catch (ClientProtocolException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    }  
	    
		return new ServerResponse(result, errorCodes);
	}
	
	public static ServerResponse mobilityUpload(String username, String hashedPassword, String client, String data) {

		final boolean GZIP = true;
		
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		String url = SERVER_URL + "app/u/mobility";
		
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
	    
	    try {  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("u", username));
	        nameValuePairs.add(new BasicNameValuePair("p", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("ci", client));
	        nameValuePairs.add(new BasicNameValuePair("d", data));
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
	        
	        if (GZIP) {
	        	InputStream is = formEntity.getContent();
		        
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        //BufferedOutputStream zipper = new BufferedOutputStream(new GZIPOutputStream(baos));
		        GZIPOutputStream zipper = new GZIPOutputStream(baos);
		        
		        formEntity.writeTo(zipper);
		        
		        
		        
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
		        
	        } else {
	        	httpPost.setEntity(formEntity);
	        }
	        
	        HttpResponse response = httpClient.execute(httpPost);
	        
	        if (response != null) {
	        	Log.i(TAG, response.getStatusLine().toString());
	        	if (response.getStatusLine().getStatusCode() == 200) {
	        		HttpEntity entity = response.getEntity();
	        		if (entity != null) {
	        			String content = EntityUtils.toString(entity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
						try {
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
							// TODO Auto-generated catch block
							e.printStackTrace();
							result = Result.INTERNAL_ERROR;
						}
						
	        		} else {
	        			result = Result.HTTP_ERROR;
	        		}
	        		
	        	} else {
	        		result = Result.HTTP_ERROR;
	        	}
	        	
	        } else {
	        	result = Result.HTTP_ERROR;
	        }
	        
	    } catch (ClientProtocolException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    }  
	    
		return new ServerResponse(result, errorCodes);
	}
	
	public static ServerResponse surveyUpload(String username, String hashedPassword, String client, String campaignName, String campaignVersion, String data) {
		
		final boolean GZIP = true;
		
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		String url = SERVER_URL + "app/u/survey";
		
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
	    
	    
	    
	    try {  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("c", campaignName));  
	        nameValuePairs.add(new BasicNameValuePair("cv", campaignVersion));
	        nameValuePairs.add(new BasicNameValuePair("u", username));
	        nameValuePairs.add(new BasicNameValuePair("p", hashedPassword));
	        nameValuePairs.add(new BasicNameValuePair("ci", client));
	        nameValuePairs.add(new BasicNameValuePair("d", data));
	        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairs);
	        
	        if (GZIP) {
	        	InputStream is = formEntity.getContent();
		        
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        //BufferedOutputStream zipper = new BufferedOutputStream(new GZIPOutputStream(baos));
		        GZIPOutputStream zipper = new GZIPOutputStream(baos);
		        
		        formEntity.writeTo(zipper);
		        
		        
		        
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
		        
	        } else {
	        	httpPost.setEntity(formEntity);
	        }
	        
	        HttpResponse response = httpClient.execute(httpPost);
	        
	        if (response != null) {
	        	Log.i(TAG, response.getStatusLine().toString());
	        	if (response.getStatusLine().getStatusCode() == 200) {
	        		HttpEntity entity = response.getEntity();
	        		if (entity != null) {
	        			String content = EntityUtils.toString(entity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
						try {
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
							// TODO Auto-generated catch block
							e.printStackTrace();
							result = Result.INTERNAL_ERROR;
						}
						
	        		} else {
	        			result = Result.HTTP_ERROR;
	        		}
	        		
	        	} else {
	        		result = Result.HTTP_ERROR;
	        	}
	        	
	        } else {
	        	result = Result.HTTP_ERROR;
	        }
	        
	    } catch (ClientProtocolException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    }  
	    
		return new ServerResponse(result, errorCodes);
	}
	
	public static ServerResponse mediaUpload(String username, String hashedPassword, String client, String campaignName, String uuid, File data) {
		Result result = Result.HTTP_ERROR;
		String[] errorCodes = null;
		
		String url = SERVER_URL + "app/u/image";
		
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
	    
	    try {
	    	MultipartEntity multipartEntity = new MultipartEntity();
	    	multipartEntity.addPart("c", new StringBody(campaignName));
	    	multipartEntity.addPart("u", new StringBody(username));
	    	multipartEntity.addPart("p", new StringBody(hashedPassword));
	    	multipartEntity.addPart("ci", new StringBody(client));
	    	multipartEntity.addPart("i", new StringBody(uuid));
	    	multipartEntity.addPart("f", new FileBody(data, "image/jpeg"));
	        httpPost.setEntity(multipartEntity);
	    
	        HttpResponse response = httpClient.execute(httpPost);
	        
	        if (response != null) {
	        	Log.i(TAG, response.getStatusLine().toString());
	        	if (response.getStatusLine().getStatusCode() == 200) {
	        		HttpEntity entity = response.getEntity();
	        		if (entity != null) {
	        			String content = EntityUtils.toString(entity);
	        			Log.i(TAG, content);
	        			
	        			JSONObject rootJson;
						try {
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
							// TODO Auto-generated catch block
							e.printStackTrace();
							result = Result.INTERNAL_ERROR;
						}
						
	        		} else {
	        			result = Result.HTTP_ERROR;
	        		}
	        		
	        	} else {
	        		result = Result.HTTP_ERROR;
	        	}
	        	
	        } else {
	        	result = Result.HTTP_ERROR;
	        }
	        
	    } catch (ClientProtocolException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block
	    	result = Result.HTTP_ERROR;
	    }  
	    
		return new ServerResponse(result, errorCodes);
	}
	
	private static void doHttpPost() {
		
		
	}
}
