package org.ohmage.fragments;

import com.google.android.maps.MapView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class MapFragment extends Fragment {

	private MapView mMapView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return addMapView(null);
	}

	/**
	 * Adds the mapview to the container if one is provided
	 * @param container
	 * @return the mapview instance
	 */
	protected MapView addMapView(ViewGroup container) {
		if(mMapView == null) {
			mMapView = new MapView(getActivity(), getMapsApiKey());
			mMapView.setClickable(true);
		} else {
			((ViewGroup)mMapView.getParent()).removeView(mMapView);
		}
		if(container != null)
			container.addView(mMapView);
		return mMapView;
	}

	/**
	 * Returns the map view for this fragment
	 * @return the map view
	 */
	protected MapView getMapView() {
		if(mMapView == null)
			throw new RuntimeException("A mapview must be added to the layout using the addMapView function");
		return mMapView;
	}

	/**
	 * Extending classes should override this to give the mapview access to the api key
	 * @return the api key
	 */
	protected abstract String getMapsApiKey();
}
