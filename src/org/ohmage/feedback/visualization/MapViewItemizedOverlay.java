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
		
//		final Dialog responseDialog = new Dialog(mContext);
//		
//		responseDialog.setContentView(R.layout.mapview_custom_dialog);
//		responseDialog.setTitle(item.getTitle());
//		
//		TextView text = (TextView)responseDialog.findViewById(R.id.mapview_dialog_text);
//		text.setText(item.getSnippet());
//		
//		Button closeButton = (Button)responseDialog.findViewById(R.id.mapview_dialog_button); 
//		closeButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				responseDialog.dismiss();
//			}
//		});
//		responseDialog.show();
		long id = Long.valueOf(item.getResponseID()).longValue();
		Uri uri = DbContract.Response.getResponseByID(id);
		//Uri uri = Uri.parse("content://org.ohmage.db/responses/25");
		//URI uri = new URI("content://org.ohmage.db/responses/25");
		mContext.startActivity(
				new Intent(
						Intent.ACTION_VIEW,
						uri));
		Toast.makeText(mContext, "ResponseInfo Activity will start. \nResponse ID:\n" + item.getResponseID() 
		, Toast.LENGTH_SHORT).show();
		
		return true;
	}
}
