package org.ohmage.feedback.visualization;

import java.util.ArrayList;

import org.ohmage.R;
import org.ohmage.activity.RHMapViewActivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
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
		
		Toast.makeText(mContext, "ResponseInfo Activity will start. \nResponse ID:\n" + item.getResponseID() 
		, Toast.LENGTH_SHORT).show();
		
		return true;
	}
}
