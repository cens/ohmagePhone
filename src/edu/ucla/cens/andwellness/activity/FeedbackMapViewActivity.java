package edu.ucla.cens.andwellness.activity;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.feedback.visualization.MapViewItemizedOverlay;

public class FeedbackMapViewActivity extends MapActivity {

	MapViewItemizedOverlay itemizedoverlay = null;
	MapView mMapView;
	MapController mControl;
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.mapview);
	    
	    mMapView = (MapView) findViewById(R.id.mapview);
	    mMapView.setBuiltInZoomControls(true);
	    
	    mControl = mMapView.getController();
	    mControl.setZoom(15);
	    
	    //Set MapCenter to current location
	    LocationManager locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    String provider = locMan.getBestProvider(new Criteria(), true);
	    Location location = locMan.getLastKnownLocation(provider);
	    
	    GeoPoint point = new GeoPoint(34065009, -118443413);
	    if(location != null) //If location is not available, then set the map center to UCLA
	    	point = new GeoPoint((int)(location.getLatitude()*1e6), (int)(location.getLongitude()*1e6));	    	
	    mControl.setCenter(point);
	    
	    //Add overlays to the map
	    List<Overlay> mapOverlays = mMapView.getOverlays();
	    Drawable drawable = this.getResources().getDrawable(R.drawable.darkgreen_marker_a);
	    Context mContext = this;
	    itemizedoverlay= new MapViewItemizedOverlay(drawable, mContext);
	    
	    String temp = "When was your last snack?\n" + " Mid-morning\n" +
	    "What did you eat?\n" + " hot cheetos\n" +
	    "How healthy was the snack? (1 very unhealthy, 5 very healthy)\n" + " 1\n" +
	    "Where did you eat?\n" + " School\n" +
	    "Who were you with?\n" + " Classmates\n" +
	    "Why did you eat?\n" + " hungry\n" +
	    "Why did you choose this snack instead of something else?\n" + " I wanted some chips\n" + 
	    "How much did the snack cost?\n" + " Less than $1.00\n" +
	    "How much did the snack cost?\n" + " Less than $1.00\n" +
	    "How much did the snack cost?\n" + " Less than $1.00\n" +
	    "How much did the snack cost?\n" + " Less than $1.00\n" +
	    "How much did the snack cost?\n" + " Less than $1.00\n" +
	    "How much did the snack cost?\n" + " Less than $1.00\n";
	    
	    addNewItemToMap(34065009,-118443413, "Response #1", temp);
	    addNewItemToMap(34067414,-118441887, "Response #2", "This is the detail response of yours.");
	    addNewItemToMap(34070898,-118448410, "Response #3", "This is the detail response of yours.");
	    addNewItemToMap(34072249,-118443389, "Response #4", "This is the detail response of yours.");
	    addNewItemToMap(34074524,-118451972, "Response #5", "This is the detail response of yours.");
	    	    
	    mapOverlays.add(itemizedoverlay);
	}
	
	private void addNewItemToMap(int lat, int lon, String Title, String Content){
		GeoPoint point = new GeoPoint(lat, lon);
		OverlayItem overlayitem = new OverlayItem(point, Title, Content);
		itemizedoverlay.addOverlay(overlayitem);
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
}
