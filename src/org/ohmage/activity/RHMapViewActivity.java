package org.ohmage.activity;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import org.ohmage.R;
import org.ohmage.activity.ResponseListFragment.OnResponseActionListener;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.DateFilterControl.DateFilterChangeListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.feedback.visualization.MapOverlayItem;
import org.ohmage.feedback.visualization.MapViewItemizedOverlay;
import org.ohmage.feedback.visualization.ResponseHistory;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.photo.PhotoPrompt;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

public class RHMapViewActivity extends ResponseHistory {

	static final String TAG = "MapActivityLog"; 
	private MapViewItemizedOverlay mItemizedoverlay = null;
	private MapView mMapView;
	private MapController mControl;
	private String mCampaignUrn;
	private String mSurveyId;
	private List<Prompt> mPrompts;
	private FilterControl mCampaignFilter;
	private FilterControl mSurveyFilter;
	private DateFilterControl mDateFilter;
	private Button mMapPinNext;
	private Button mMapPinPrevious;
	private int pinIndex;

	
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
	    displayItemsOnMap();
	    
	}
	
	public void displayItemsOnMap(){
		
		//Clear current overlay items
		mMapView.getOverlays().clear();
		mMapView.removeAllViews();
		
		//Get the currently selected CampaignUrn and SurveyID
	    String curSurveyValue = mSurveyFilter.getValue();
		mCampaignUrn = curSurveyValue.substring(0, curSurveyValue.lastIndexOf(":"));
		mSurveyId = curSurveyValue.substring(curSurveyValue.lastIndexOf(":")+1, curSurveyValue.length());

		//Retrieve data from CP
	    ContentResolver cr = this.getContentResolver();
		
	    Uri queryUri;
		if(mCampaignUrn.equals("all")){
			if(mSurveyId.equals("all")){
				queryUri = Response.getResponses();
			}
			else{
				queryUri = Response.getResponsesByCampaignAndSurvey(mCampaignUrn, mSurveyId);
			}
		}
		else{
			if(mSurveyId.equals("all")){
				queryUri = Response.getResponsesByCampaign(mCampaignUrn);
			}
			else{
				queryUri = Response.getResponsesByCampaignAndSurvey(mCampaignUrn, mSurveyId);
			}
		}
		

		Calendar cal = mDateFilter.getValue();
		GregorianCalendar greCalStart = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
		GregorianCalendar greCalEnd = new GregorianCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		
		String[] projection = {
				DbHelper.Tables.RESPONSES+"."+Response._ID,
				DbHelper.Tables.RESPONSES+"."+Response.LOCATION_LATITUDE, 
				DbHelper.Tables.RESPONSES+"."+Response.LOCATION_LONGITUDE,
				DbHelper.Tables.RESPONSES+"."+Response.SURVEY_ID, 
				DbHelper.Tables.RESPONSES+"."+Response.CAMPAIGN_URN, 
				DbHelper.Tables.RESPONSES+"."+Response.DATE 
				};
		
		String selection = 
				Response.TIME + " > " + greCalStart.getTime().getTime() +
				" AND " + 
				Response.TIME + " < " + greCalEnd.getTime().getTime() + 
				" AND " +
				Response.LOCATION_STATUS + "=" + "'valid'";
		
	    Cursor cursor = cr.query(queryUri, projection, selection, null, null);

	    //Init the map center to current location
	    setMapCenterToCurrentLocation();

	    //Add overlays to the map
	    List<Overlay> mapOverlays = mMapView.getOverlays();
	    Drawable drawable = this.getResources().getDrawable(R.drawable.darkgreen_marker_a);
	    mItemizedoverlay= new MapViewItemizedOverlay(drawable, mMapView);

	    for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
		    Double lat = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LATITUDE));
		    Double lon = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LONGITUDE));
		    GeoPoint point = new GeoPoint((int)(lat.doubleValue()*1e6), (int)(lon.doubleValue()*1e6));
		    String title = cursor.getString(cursor.getColumnIndex(Response.SURVEY_ID));
		    String text = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_URN)) + "\n" + 
		    cursor.getString(cursor.getColumnIndex(Response.DATE));
		    String id = cursor.getString(cursor.getColumnIndex(Response._ID));
		    
			MapOverlayItem overlayItem = new MapOverlayItem(point, title, text, id);
			mItemizedoverlay.setBalloonBottomOffset(40);
			mItemizedoverlay.addOverlay(overlayItem);
	    }
	    cursor.close();
	    if(mItemizedoverlay.size() > 0){
		    mapOverlays.add(mItemizedoverlay);
		    mControl.setCenter(mItemizedoverlay.getCenter());
	    }

		mMapPinNext = (Button) mMapView.getRootView().findViewById(R.id.map_pin_next);
		mMapPinPrevious = (Button) mMapView.getRootView().findViewById(R.id.map_pin_previous);
		
		
		pinIndex = 0;
		if(mMapPinNext != null){
			mMapPinNext.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					int overlayListSize = mItemizedoverlay.size();
					if(overlayListSize > 0){
						mItemizedoverlay.onTap(pinIndex % overlayListSize);
						if(pinIndex < (overlayListSize-1)){
							pinIndex++;	
						}
					}
				}
			});			
		}
		
		if(mMapPinPrevious != null){
			mMapPinPrevious.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					int overlayListSize = mItemizedoverlay.size();
					if(overlayListSize > 0){
						mItemizedoverlay.onTap(pinIndex % overlayListSize);
						if(pinIndex > 0){
							pinIndex--;	
						}
					}
				}
			});			
		}
		
	}
	
	public void setupFilters(){
		//Set filters
		mDateFilter = (DateFilterControl)findViewById(R.id.date_filter);
		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);
	
		final ContentResolver cr = getContentResolver();
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curCampaignValue) {
				Cursor surveyCursor;
				
				String[] projection = {Survey.TITLE, Survey.CAMPAIGN_URN, Survey.SURVEY_ID};
				
				//Create Cursor
				if(curCampaignValue.equals("all")){
					surveyCursor = cr.query(Survey.getSurveys(), projection, null, null, Survey.TITLE);
				}
				else{
					surveyCursor = cr.query(Survey.getSurveysByCampaignURN(curCampaignValue), projection, null, null, null);
				}
	
				//Update SurveyFilter
				//Concatenate Campain_URN and Survey_ID with a colon for survey filer values,
				//in order to handle 'All Campaign' case.
				mSurveyFilter.clearAll();
				for(surveyCursor.moveToFirst();!surveyCursor.isAfterLast();surveyCursor.moveToNext()){
					mSurveyFilter.add(new Pair<String, String>(
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.TITLE)),
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.CAMPAIGN_URN)) + 
							":" +
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.SURVEY_ID))
							));
				}
				mSurveyFilter.add(0, new Pair<String, String>("All Surveys", mCampaignFilter.getValue() + ":" + "all"));
				surveyCursor.close();
				
				displayItemsOnMap();
			}
		});
	
		mSurveyFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				displayItemsOnMap();
			}
		});
		
		mDateFilter.setOnChangeListener(new DateFilterChangeListener() {
			
			@Override
			public void onFilterChanged(Calendar curValue) {
				displayItemsOnMap();
			}
		});
		
		String select = Campaign.STATUS + "=" + Campaign.STATUS_READY;
		String[] projection = {Campaign.NAME, Campaign.URN};

		Cursor campaigns = cr.query(Campaign.getCampaigns(), projection, select, null, null);
		mCampaignFilter.populate(campaigns, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", "all"));	
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		RHTabHost.setCampaignFilterIndex(mCampaignFilter.getIndex());
		RHTabHost.setSurveyFilterIndex(mSurveyFilter.getIndex());
		RHTabHost.setDateFilterValue(mDateFilter.getValue());
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		mCampaignFilter.setIndex(RHTabHost.getCampaignFilterIndex());
		mSurveyFilter.setIndex(RHTabHost.getSurveyFilterIndex());
		mDateFilter.setDate(RHTabHost.getDateFilterValue());
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
}
