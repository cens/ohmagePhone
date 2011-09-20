package org.ohmage.feedback.visualization;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MapOverlayItem extends OverlayItem {

	private String mResponseID;
	
	public MapOverlayItem(GeoPoint point, String title, String snippet, String responseID) {
		super(point, title, snippet);
		mResponseID = responseID;
	}

	public String getResponseID(){
		return mResponseID;
	}
}
