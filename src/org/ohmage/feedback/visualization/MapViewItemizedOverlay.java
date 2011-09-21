package org.ohmage.feedback.visualization;

import java.net.URI;
import java.util.ArrayList;

import org.ohmage.db.DbContract;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MapViewItemizedOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private ArrayList<MapOverlayItem> mOverlays = new ArrayList<MapOverlayItem>();
	private Context mContext;
	
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
	
		long id = Long.valueOf(item.getResponseID()).longValue();
		Uri uri = DbContract.Response.getResponseByID(id);

		mContext.startActivity(
				new Intent(Intent.ACTION_VIEW, uri));
		return true;
	}
}
