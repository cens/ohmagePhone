package org.ohmage.fragments;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import org.ohmage.R;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.feedback.visualization.MapOverlayItem;
import org.ohmage.feedback.visualization.MapViewItemizedOverlay;
import org.ohmage.service.SurveyGeotagService;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

public class ResponseMapFragment extends MapFragment implements LoaderCallbacks<Cursor> {

	private static final String RESPONSE_ID = "response_id";

	private long responseId;

	private Double mLatitude;
	private Double mLongitude;
	private String mTitle;
	private String mText;

	/**
	 * Create an instance of {@link ResponseMapFragment} which creates a point
	 * for a single response
	 * @param the response id
	 */
	public static ResponseMapFragment newInstance(long responseId) {
		ResponseMapFragment f = new ResponseMapFragment();

		Bundle args = new Bundle();
		args.putLong(RESPONSE_ID, responseId);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle args) {
		super.onCreate(args);

		if(getArguments() == null || !getArguments().containsKey(RESPONSE_ID))
			throw new RuntimeException("The response ID must be passed to the ViewResponseMapFragment");

		responseId = getArguments().getLong(RESPONSE_ID);

		getLoaderManager().initLoader(0, null, this);
	}

	private static class ResponseMapQuery {
		public static final String[] PROJECTION = new String[] {
			Responses.RESPONSE_LOCATION_STATUS,
			Responses.RESPONSE_LOCATION_LATITUDE,
			Responses.RESPONSE_LOCATION_LONGITUDE,
			Surveys.SURVEY_TITLE,
			Responses.CAMPAIGN_URN,
			Responses.RESPONSE_DATE
		};

		public static final int LOCATION_STATUS = 0;
		public static final int LOCATION_LATITUDE = 1;
		public static final int LOCATION_LONGITUDE = 2;
		public static final int TITLE = 3;
		public static final int CAMPAIGN_URN = 4;
		public static final int DATE = 5;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapView v = (MapView) super.onCreateView(inflater, container, savedInstanceState);
		v.setBuiltInZoomControls(true);
		v.setClickable(true);
		return v;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(getActivity(), Responses.buildResponseUri(responseId), ResponseMapQuery.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if(!data.moveToFirst()) {
			Toast.makeText(getActivity(), "Response does not exist", Toast.LENGTH_SHORT).show();
			getActivity().finish();
			return;
		}

		if(!SurveyGeotagService.LOCATION_VALID.equals(data.getString(ResponseMapQuery.LOCATION_STATUS))) {
			Toast.makeText(getActivity(), "No location for response", Toast.LENGTH_SHORT).show();
			getActivity().finish();
			return;
		}

		mLatitude = data.getDouble(ResponseMapQuery.LOCATION_LATITUDE);
		mLongitude = data.getDouble(ResponseMapQuery.LOCATION_LONGITUDE);
		mTitle = data.getString(ResponseMapQuery.TITLE);
		mText = data.getString(ResponseMapQuery.CAMPAIGN_URN) + "\n" + data.getString(ResponseMapQuery.DATE);	    

		Drawable drawable = getResources().getDrawable(R.drawable.pens1);
		MapViewItemizedOverlay itemizedoverlay = new MapViewItemizedOverlay(drawable, getMapView());

		GeoPoint point = new GeoPoint((int)(mLatitude*1e6), (int)(mLongitude*1e6));
		MapOverlayItem overlayItem = new MapOverlayItem(point, mTitle, mText, null);
		itemizedoverlay.setBalloonBottomOffset(40);
		itemizedoverlay.addOverlay(overlayItem);

		List<Overlay> overlays = getMapView().getOverlays();
		overlays.clear();
		overlays.add(itemizedoverlay);

		MapController mapControl = getMapView().getController();
		mapControl.animateTo(overlayItem.getPoint());
		mapControl.setZoom(16);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
}
