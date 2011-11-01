package org.ohmage.feedback.visualization;

import java.util.ArrayList;

import org.ohmage.db.DbContract.Responses;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MapViewItemizedOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private final ArrayList<MapOverlayItem> mOverlays = new ArrayList<MapOverlayItem>();
	private final Context mContext;
	
	public MapViewItemizedOverlay(Drawable defaultMarker, MapView mapView) {
		  super(boundCenterBottom(defaultMarker), mapView);
		  mContext = mapView.getContext();
	}

	public void addOverlay(MapOverlayItem overlay){
		mOverlays.add(overlay);
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	@Override
	protected boolean onBalloonTap(int index){

		MapOverlayItem item = mOverlays.get(index);
	
		if(item.getResponseID() == null)
			return false;
		
		long id = Long.valueOf(item.getResponseID()).longValue();
		Uri uri = Responses.buildResponseUri(id);

		mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		return true;
	}
	
	public int getMaxLatitude(){
		int maxLatitude = Integer.MIN_VALUE;
		for(int i=0; i < mOverlays.size() ;i++){
			maxLatitude = Math.max(mOverlays.get(i).getPoint().getLatitudeE6(), maxLatitude);
		}
		return maxLatitude;
	}
	
	public int getMinLatitude(){
		int minLatitude = Integer.MAX_VALUE;
		for(int i=0; i < mOverlays.size() ;i++){
			minLatitude = Math.min(mOverlays.get(i).getPoint().getLatitudeE6(), minLatitude);
		}
		return minLatitude;
	}
	
	public int getMaxLongitude(){
		int maxLongitude = Integer.MIN_VALUE;
		for(int i=0; i < mOverlays.size() ;i++){
			maxLongitude = Math.max(mOverlays.get(i).getPoint().getLongitudeE6(), maxLongitude);
		}
		return maxLongitude;
	}
	
	public int getMinLongitude(){
		int minLongitude = Integer.MAX_VALUE;
		for(int i=0; i < mOverlays.size() ;i++){
			minLongitude = Math.min(mOverlays.get(i).getPoint().getLongitudeE6(), minLongitude);
		}
		return minLongitude;	}
	
}
