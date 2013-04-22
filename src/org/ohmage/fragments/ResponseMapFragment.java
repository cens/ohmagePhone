package org.ohmage.fragments;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import org.ohmage.ConfigHelper;
import org.ohmage.mobilizingcs.R;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.feedback.visualization.MapOverlayItem;
import org.ohmage.feedback.visualization.MapViewItemizedOverlay;
import org.ohmage.service.SurveyGeotagService;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;


public class ResponseMapFragment extends FilterableMapFragment  {

	private static final String RESPONSE_ID = "response_id";
	
	private Button mMapPinNext;
	private Button mMapPinPrevious;
	private TextView mMapPinIdxButton;
	private int mPinIndex;
	private MapViewItemizedOverlay mItemizedOverlay;

	private Long mResponseId;

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

		if(getArguments() != null && getArguments().containsKey(RESPONSE_ID))
			mResponseId = getArguments().getLong(RESPONSE_ID);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		inflater.inflate(R.layout.response_map_navigator_layout, (ViewGroup) view);

		mMapPinNext = (Button) view.findViewById(R.id.map_pin_next);
		mMapPinPrevious = (Button) view.findViewById(R.id.map_pin_previous);
		mMapPinIdxButton = (TextView) view.findViewById(R.id.map_pin_index);

		mMapPinNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int overlayListSize = mItemizedOverlay.size();
				if(overlayListSize > 0){
					if(mPinIndex < (overlayListSize-1)){
						mPinIndex = (mPinIndex + 1) % overlayListSize;
						mItemizedOverlay.onTap(mPinIndex);
						setNavigatorButtons();
					}
				}
			}
		});			

		mMapPinPrevious.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int overlayListSize = mItemizedOverlay.size();
				if(overlayListSize > 0){
					if(mPinIndex > 0){
						mPinIndex = (mPinIndex - 1) % overlayListSize;
						mItemizedOverlay.onTap(mPinIndex);
						setNavigatorButtons();
					}
				}
			}
		});			

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// When the map comes back it could have data from another map on it, so we need to restart our loader
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if(mResponseId == null) {
			return new ResponseLoader(this, ResponseMapQuery.PROJECTION, Responses.RESPONSE_LOCATION_STATUS + "='" + SurveyGeotagService.LOCATION_VALID + "'").onCreateLoader(id, args);
		} else
			return new CursorLoader(getActivity(), Responses.buildResponseUri(mResponseId), ResponseMapQuery.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		//Add overlays to the map
		List<Overlay> mapOverlays = getMapView().getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.bg_map_marker);
		if(mItemizedOverlay != null)
			mItemizedOverlay.clearBalloon();
		mItemizedOverlay = new MapViewItemizedOverlay(drawable, getMapView());
		mItemizedOverlay.setOnFocusChangeListener(new ItemizedOverlay.OnFocusChangeListener() {

			@Override
			public void onFocusChanged(ItemizedOverlay overlay, OverlayItem newFocus) {
				mPinIndex = overlay.getLastFocusedIndex();
				if(newFocus == null) {
					mPinIndex = -1;
					mItemizedOverlay.hideBalloon();
				}
				setNavigatorButtons();
			}
		});
		mItemizedOverlay.setBalloonBottomOffset(40);

		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
			Double lat = cursor.getDouble(ResponseMapQuery.LOCATION_LATITUDE);
			Double lon = cursor.getDouble(ResponseMapQuery.LOCATION_LONGITUDE);
			GeoPoint point = new GeoPoint((int)(lat.doubleValue()*1e6), (int)(lon.doubleValue()*1e6));
			String title = cursor.getString(ResponseMapQuery.TITLE);
			StringBuilder text = new StringBuilder();
			// Only show the campaign urn if we aren't in single campaign mode
			if(!ConfigHelper.isSingleCampaignMode())
				text.append(cursor.getString(ResponseMapQuery.CAMPAIGN_URN) + "\n");
			text.append(cursor.getString(ResponseMapQuery.DATE));
			String id = cursor.getString(ResponseMapQuery.ID);

			MapOverlayItem overlayItem = new MapOverlayItem(point, title, text.toString(), (mResponseId != null) ? null : id);
			mItemizedOverlay.addOverlay(overlayItem);
		}

		mapOverlays.clear();
		getMapView().invalidate();
		if(mItemizedOverlay.size() > 0){
			mapOverlays.add(mItemizedOverlay);

			int maxLatitude = mItemizedOverlay.getMaxLatitude();
			int minLatitude = mItemizedOverlay.getMinLatitude();

			int maxLongitude = mItemizedOverlay.getMaxLongitude();
			int minLongitude = mItemizedOverlay.getMinLongitude();

			getMapControl().animateTo(new GeoPoint((maxLatitude+minLatitude)/2, (maxLongitude+minLongitude)/2));
			getMapControl().zoomToSpan(Math.abs(maxLatitude-minLatitude), Math.abs(maxLongitude-minLongitude));
		}

		//Set Map Pin Navigators.
		if(mResponseId == null) {
			mMapPinIdxButton.setVisibility(View.VISIBLE);
			mMapPinNext.setVisibility(View.VISIBLE);
			mMapPinPrevious.setVisibility(View.VISIBLE);
		} else {
			mMapPinIdxButton.setVisibility(View.GONE);
			mMapPinNext.setVisibility(View.GONE);
			mMapPinPrevious.setVisibility(View.GONE);
		}

		mPinIndex = -1;
		mMapPinIdxButton.setText("");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		//
	}

	protected static class ResponseMapQuery {
		public static final String[] PROJECTION = new String[] {
			Responses._ID,
			Responses.RESPONSE_LOCATION_STATUS,
			Responses.RESPONSE_LOCATION_LATITUDE,
			Responses.RESPONSE_LOCATION_LONGITUDE,
			Surveys.SURVEY_TITLE,
			Responses.CAMPAIGN_URN,
			Responses.RESPONSE_DATE
		};

		public static final int ID = 0;
		public static final int LOCATION_STATUS = 1;
		public static final int LOCATION_LATITUDE = 2;
		public static final int LOCATION_LONGITUDE = 3;
		public static final int TITLE = 4;
		public static final int CAMPAIGN_URN = 5;
		public static final int DATE = 6;
	}

	public void setNavigatorButtons() {
		if(mPinIndex == -1)
			mMapPinIdxButton.setText(null);
		else
			mMapPinIdxButton.setText(""+(mPinIndex+1)+"/"+mItemizedOverlay.size());
	}
}
