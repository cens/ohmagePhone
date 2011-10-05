package org.ohmage.fragments;

import com.google.android.maps.MapView;

import org.ohmage.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MapFragment extends Fragment {

	private MapView mMapView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if(mMapView == null) {
			mMapView = new MapView(getActivity(), getMapsApiKey());
		} else {
			((ViewGroup)mMapView.getParent()).removeView(mMapView);
		}

		return mMapView;
	}

	/**
	 * Returns the map view for this fragment
	 * @return the map view
	 */
	protected MapView getMapView() {
		if(mMapView == null)
			mMapView = (MapView) getView().findViewById(R.id.mapview);
		if(mMapView == null)
			throw new RuntimeException("The layout provided to create the map fragment should have a mapview with the id mapview");
		return mMapView;
	}

	/**
	 * Extending classes should override this to give the mapview access to the api key
	 * @return the api key
	 */
	protected String getMapsApiKey() {
		return getString(R.string.maps_api_key);
	}
}
