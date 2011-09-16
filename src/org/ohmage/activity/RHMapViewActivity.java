package org.ohmage.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ohmage.CampaignXmlHelper;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.feedback.FeedbackService;
import org.ohmage.feedback.visualization.MapViewItemizedOverlay;
import org.ohmage.feedback.visualization.ResponseHistory;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import edu.ucla.cens.systemlog.Log;

public class RHMapViewActivity extends ResponseHistory {

	static final String TAG = "MapActivityLog"; 
	private MapViewItemizedOverlay itemizedoverlay = null;
	private MapView mMapView;
	private MapController mControl;
	private String mCampaignUrn;
	private String mSurveyId;
	private List<Prompt> mPrompts;
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setTitle("Response Map Summary");
	    setContentView(R.layout.mapview);
	    
	    mMapView = (MapView) findViewById(R.id.mapview);
	    mMapView.setBuiltInZoomControls(true);
	    
	    mControl = mMapView.getController();
	    mControl.setZoom(11);
	    
	    setupFilters();
	    
	    mCampaignFilter.setIndex(RHTabHost.getCampaignFilterIndex());
	    mSurveyFilter.setIndex(RHTabHost.getSurveyFilterIndex());
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		RHTabHost.setCampaignFilterIndex(mCampaignFilter.getIndex());
		RHTabHost.setSurveyFilterIndex(mSurveyFilter.getIndex());
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		mCampaignFilter.setIndex(RHTabHost.getCampaignFilterIndex());
		mSurveyFilter.setIndex(RHTabHost.getSurveyFilterIndex());
	}
		
	private void setMapCenterToCurrentLocation(){
	    //Set MapCenter to current location
	    LocationManager locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    String provider = locMan.getBestProvider(new Criteria(), true);
	    Location currentLocation = locMan.getLastKnownLocation(provider);
	    
	    GeoPoint point = new GeoPoint(34065009, -118443413);
	    if(currentLocation != null) //If location is not available, then set the map center to UCLA
	    	point = new GeoPoint((int)(currentLocation.getLatitude()*1e6), (int)(currentLocation.getLongitude()*1e6));	    	
	    mControl.setCenter(point);		
	}
	
	private void addNewItemToMap(int lat, int lon, String title, String jsonResponse){
		GeoPoint point = new GeoPoint(lat, lon);
		String photoUUID = null;
		
		//Build propt text
		String resultResponse = "";
		try{
			JSONArray arrResponse = new JSONArray(jsonResponse);
			
			for(int i=0; i<arrResponse.length(); i++){
				JSONObject jobjectResponse = arrResponse.getJSONObject(i);
				String promptId = jobjectResponse.get("prompt_id").toString();
				String value = jobjectResponse.get("value").toString();
				resultResponse += getPromptLabel(promptId)+ "\n";
				resultResponse += "   :" + getPropertiesLabel(promptId, value) + "\n";
				if(isPhotoPrompt(promptId)==true){
					photoUUID = value;
				}
			}
		}
		catch(Exception e){
			resultResponse = jsonResponse;
		}
		
		Bitmap img = null;
		if(photoUUID != null
				&& !photoUUID.equalsIgnoreCase("NOT_DISPLAYED")
				&& FeedbackService.ensurePhotoExists(this, mCampaignUrn, photoUUID)){
			File photoDir = new File(PhotoPrompt.IMAGE_PATH + "_cache/" + mCampaignUrn.replace(':', '_'));
			File photo = new File(photoDir, photoUUID + ".png");
			img = BitmapFactory.decodeFile(photo.getAbsolutePath());
		} 
		
		FeedbackMapOverlayItems overlayitem = new FeedbackMapOverlayItems(point, title, resultResponse, img);
		itemizedoverlay.addOverlay(overlayitem);
	}	
	
	private boolean isPhotoPrompt(String promptId){
		Iterator<Prompt> ite = mPrompts.iterator();
		while(ite.hasNext()){
			AbstractPrompt allPromptList = (AbstractPrompt)ite.next();
			if(promptId.equals(allPromptList.getId())){
				if(allPromptList instanceof PhotoPrompt){
					return true;
				}
			}
		}
		return false;
	}
	
	private String getPropertiesLabel(String promptId, String value){
		
		Iterator<Prompt> ite = mPrompts.iterator();
		while(ite.hasNext()){
			AbstractPrompt allPromptList = (AbstractPrompt)ite.next();
			
			if(promptId.equals(allPromptList.getId())){
				return AbstractPrompt.getDisplayValue(allPromptList, value);
			}
		}
		return value;
	}
	
	private String getPromptLabel(String promptId){
		Iterator<Prompt> ite = mPrompts.iterator();
		String searchedLable = "";
		while(ite.hasNext()){
			AbstractPrompt prompt = (AbstractPrompt)ite.next();
			if(promptId.equalsIgnoreCase(prompt.getId())){
				searchedLable = prompt.getPromptText();
			}
		}
		return searchedLable;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.add(0,1,0,"Map");
		menu.add(0,2,0,"Satellite");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
		case 1:
			mMapView.setSatellite(false);
			return true;
		case 2:
			mMapView.setSatellite(true);
			return true;
		}
		return false;
	}
	
	public class FeedbackMapOverlayItems extends OverlayItem{
		Bitmap mImage;
		public FeedbackMapOverlayItems(GeoPoint point, String title, String snippet, Bitmap img){
			super(point, title, snippet);
			if(img != null){
				mImage = Bitmap.createBitmap(img);
			}
		}
		
		public Bitmap getImage(){
			return mImage;
		}
	}
	
	//Class to store responses
	private class Responses{
		private String mHashcode; 
		private String mLocation_status;
		private String mResponses;
		private String mDate;
		
		//latitude and longitude are multuplied by 1e6
		private int mLocation_latitude;
		private int mLocation_longitude;
		
		public Responses(String hashcode, String locationStatus, String latitude, String longitude, String responses
				, String date){
			this.mHashcode = hashcode;
			this.mLocation_status = locationStatus;
			this.mDate = date;
			
			if(mLocation_status.equalsIgnoreCase("valid")){
				this.mLocation_latitude = locationStringToInteger(latitude);
				this.mLocation_longitude = locationStringToInteger(longitude);
			}
			this.mResponses = responses;
		}
		
		private int locationStringToInteger(String strCoordinate){
			//Multiplies coordinates by 1e6 and return it 
			double dblCoordinate = Double.valueOf(strCoordinate).doubleValue();
			return (int)(dblCoordinate*1e6);
		}
		
		public String getDate(){
			return mDate;
		}
		
		public String getHashcode(){
			return mHashcode;
		}
		
		public String getLocationStatus(){
			return mLocation_status;
		}
		
		public int getLatitude(){
			return mLocation_latitude;
		}
		
		public int getLongitude(){
			return mLocation_longitude;
		}
		
		public String getResponses(){
			return mResponses;
		}
	}
}
