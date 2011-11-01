package org.ohmage.fragments;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

import org.ohmage.R;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public class OhmageMapFragment extends MapFragment {

	private Button mMapZoomIn;
	private Button mMapZoomOut;

	private MapController mControl;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ohmage_map_layout, container, false);

		addMapView((FrameLayout) view.findViewById(R.id.mapview));
		getMapView().setBuiltInZoomControls(false);

		mControl = getMapView().getController();
		mControl.setZoom(11);

		setMapCenterToCurrentLocation();

		mMapZoomIn = (Button) view.findViewById(R.id.map_zoom_in);
		mMapZoomOut = (Button) view.findViewById(R.id.map_zoom_out);

		mMapZoomIn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getMapView().getController().zoomIn();
			}
		});

		mMapZoomOut.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				getMapView().getController().zoomOut();
			}
		});

		return view;
	}

	protected MapController getMapControl() {
		return mControl;
	}

	private void setMapCenterToCurrentLocation(){
		//Set MapCenter to current location
		LocationManager locMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		String provider = locMan.getBestProvider(new Criteria(), true);
		Location currentLocation = locMan.getLastKnownLocation(provider);

		GeoPoint point = new GeoPoint(34065009, -118443413);
		if(currentLocation != null) //If location is not available, then set the map center to UCLA
			point = new GeoPoint((int)(currentLocation.getLatitude()*1e6), (int)(currentLocation.getLongitude()*1e6));	    	
		mControl.setCenter(point);		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(0,1,0,"Map");
		menu.add(0,2,0,"Satellite");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
			case 1:
				getMapView().setSatellite(false);
				return true;
			case 2:
				getMapView().setSatellite(true);
				return true;
		}
		return false;
	}

	@Override
	protected String getMapsApiKey() {
		return getString(R.string.maps_api_key);
	}

}
