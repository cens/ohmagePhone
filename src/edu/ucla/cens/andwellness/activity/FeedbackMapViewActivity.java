package edu.ucla.cens.andwellness.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
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

import edu.ucla.cens.andwellness.CampaignXmlHelper;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.feedback.FeedbackContract;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackResponses;
import edu.ucla.cens.andwellness.feedback.visualization.MapViewItemizedOverlay;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilderFactory;
import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPromptBuilder;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPromptBuilder;
import edu.ucla.cens.andwellness.prompt.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompt.number.NumberPromptBuilder;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPromptBuilder;
import edu.ucla.cens.andwellness.prompt.remoteactivity.RemoteActivityPrompt;
import edu.ucla.cens.andwellness.prompt.remoteactivity.RemoteActivityPromptBuilder;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPromptBuilder;
import edu.ucla.cens.andwellness.prompt.text.TextPrompt;
import edu.ucla.cens.andwellness.prompt.text.TextPromptBuilder;
import edu.ucla.cens.systemlog.Log;

public class FeedbackMapViewActivity extends MapActivity {

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
	    
	    mCampaignUrn = getIntent().getStringExtra("campaign_urn");
	    mSurveyId = getIntent().getStringExtra("survey_id");
        try {
			mPrompts = PromptXmlParser.parsePrompts(CampaignXmlHelper.loadCampaignXmlFromDb(this, mCampaignUrn), mSurveyId);
			Log.i(TAG, "Parsed XML: " + mPrompts.toString());
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		}
	    setContentView(R.layout.mapview);
	    
	    mMapView = (MapView) findViewById(R.id.mapview);
	    mMapView.setBuiltInZoomControls(true);
	    
	    mControl = mMapView.getController();
	    mControl.setZoom(15);
	    
	    
	    //Retrieve data from CP
	    ContentResolver cr = this.getContentResolver();
	    Uri queryUri = FeedbackContract.getBaseUri().buildUpon()
	    .appendPath(mCampaignUrn)
	    .appendPath(mSurveyId)
	    .appendPath("responses")
	    .build();
	    
	    Cursor cursor = cr.query(queryUri, null, null, null, null);
    
	    List<Responses> listResponses = new ArrayList<Responses>();
	    while(cursor.moveToNext()){
	    	String hashcode = cursor.getString(cursor.getColumnIndex(FeedbackResponses.HASHCODE));
	    	String locationStatus = cursor.getString(cursor.getColumnIndex(FeedbackResponses.LOCATION_STATUS));
	    	String latitude = cursor.getString(cursor.getColumnIndex(FeedbackResponses.LOCATION_LATITUDE));
	    	String longitude = cursor.getString(cursor.getColumnIndex(FeedbackResponses.LOCATION_LONGITUDE));
	    	String response = cursor.getString(cursor.getColumnIndex(FeedbackResponses.RESPONSE));
	    	
	    	listResponses.add(new Responses(hashcode, locationStatus, latitude, longitude, response));	    	
	    }
	    cursor.close();
	    
	    setMapCenterToCurrentLocation();
	    	    	
	    //Add overlays to the map
	    List<Overlay> mapOverlays = mMapView.getOverlays();
	    Drawable drawable = this.getResources().getDrawable(R.drawable.darkgreen_marker_a);
	    itemizedoverlay= new MapViewItemizedOverlay(drawable, this);
	    
	    for(Responses i : listResponses){
	    	if(i.getLocationStatus().equalsIgnoreCase("valid")){
	    		addNewItemToMap(i.getLatitude(), i.getLongitude(), "Response", i.getResponses());
	    	}
	    }
	    if(itemizedoverlay.size() > 0){
		    mapOverlays.add(itemizedoverlay);
	    }
	    Toast.makeText(this, "Displaying " + itemizedoverlay.size() + " points", Toast.LENGTH_LONG).show();
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
		
		//Build propt text
		String resultResponse = "";
		try{
			JSONArray arrResponse = new JSONArray(jsonResponse);
			
			for(int i=0; i<arrResponse.length(); i++){
				JSONObject jobjectResponse = arrResponse.getJSONObject(i);
				String promptId = jobjectResponse.get("prompt_id").toString();
				String value = jobjectResponse.get("value").toString();
				resultResponse += getPromptLabel(promptId)+ "\n";
				resultResponse += ": " + getPropertiesLabel(promptId, value) + "\n";
			}
		}
		catch(Exception e){
			resultResponse = jsonResponse;
		}
		
		OverlayItem overlayitem = new OverlayItem(point, title, resultResponse);
		itemizedoverlay.addOverlay(overlayitem);
	}
	
	private String getPropertiesLabel(String promptId, String value){
		
		Iterator<Prompt> ite = mPrompts.iterator();
		while(ite.hasNext()){
			AbstractPrompt allPromptList = (AbstractPrompt)ite.next();
			
			//CFind 
			if(promptId.equals(allPromptList.getId())){
				
				if(allPromptList instanceof SingleChoicePrompt){
					SingleChoicePrompt prompt = (SingleChoicePrompt)allPromptList;
					List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
					for(KVLTriplet i : choiceKVLTriplet){
						if(i.key.equals(value)){
							return i.label;
						}
					}
				} 
				else if(allPromptList instanceof SingleChoiceCustomPrompt){
					SingleChoiceCustomPrompt prompt = (SingleChoiceCustomPrompt)allPromptList;
					List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
					for(KVLTriplet i : choiceKVLTriplet){
						if(i.key.equals(value)){
							return i.label;
						}
					}
				} 
				else if(allPromptList instanceof MultiChoicePrompt){
					MultiChoicePrompt prompt = (MultiChoicePrompt)allPromptList;
					List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
					String result = "";
					try{
						JSONArray jsonValue = new JSONArray(value);
						for(int k=0; k<jsonValue.length(); k++){
							String answer = jsonValue.get(k).toString(); 
							for(KVLTriplet i : choiceKVLTriplet){
								if(i.key.equals(answer)){
									result += i.label + "  ";
								}
							}
						}
					}
					catch(Exception e){
						result = value;
					}
					return result;
				} 
				else if(allPromptList instanceof MultiChoiceCustomPrompt){
					MultiChoiceCustomPrompt prompt = (MultiChoiceCustomPrompt)allPromptList;
					List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
					String result = "";
					try{
						JSONArray jsonValue = new JSONArray(value);
						for(int k=0; k<jsonValue.length(); k++){
							String answer = jsonValue.get(k).toString(); 
							for(KVLTriplet i : choiceKVLTriplet){
								if(i.key.equals(answer)){
									result += i.label + "  ";
								}
							}
						}
					}
					catch(Exception e){
						result = value;
					}
					return result;
				} 
				else if(allPromptList instanceof NumberPrompt){
					NumberPrompt prompt = (NumberPrompt)allPromptList;
					return String.valueOf(prompt.getValue());
				} 
				else if(allPromptList instanceof HoursBeforeNowPrompt){
					HoursBeforeNowPrompt prompt = (HoursBeforeNowPrompt)allPromptList;
					return String.valueOf(prompt.getValue());
				} 
				else if(allPromptList instanceof TextPrompt){
					TextPrompt prompt = (TextPrompt)allPromptList;
					return prompt.getText();
				} 
				else if(allPromptList instanceof PhotoPrompt){
					PhotoPrompt prompt = (PhotoPrompt)allPromptList;
					//TODO Add a feature to display Photo
					return "";
				} 
				else if(allPromptList instanceof RemoteActivityPrompt){
					RemoteActivityPrompt prompt = (RemoteActivityPrompt)allPromptList;
					//TODO Add a feature to handle remote activity prompt
					return value;
				}
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
	
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.add(0,1,0,"Map");
		menu.add(0,2,0,"Satellite");
		return true;
	}
	
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
	
	
	//Class to store responses
	private class Responses{
		private String mHashcode; 
		private String mLocation_status;
		private String mResponses;
		
		//latitude and longitude are multuplied by 1e6
		private int mLocation_latitude;
		private int mLocation_longitude;
		
		public Responses(String hashcode, String locationStatus, String latitude, String longitude, String responses){
			this.mHashcode = hashcode;
			this.mLocation_status = locationStatus;
			
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
