package org.ohmage.feedback.visualization;

import java.util.ArrayList;

import android.content.Context;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import org.ohmage.R;

public class MapViewItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	
	public MapViewItemizedOverlay(Drawable defaultMarker){
		super(boundCenterBottom(defaultMarker));
	}
	
	public MapViewItemizedOverlay(Drawable defaultMarker, Context context) {
		  super(boundCenterBottom(defaultMarker));
		  mContext = context;
	}

	public void addOverlay(OverlayItem overlay){
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
	protected boolean onTap(int index){
		OverlayItem item = mOverlays.get(index);
		//AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		final Dialog responseDialog = new Dialog(mContext);
		
		responseDialog.setContentView(R.layout.mapview_custom_dialog);
		responseDialog.setTitle(item.getTitle());
		
		
		//TODO Add image if there image files
		//ImageView image = (ImageView)responseDialog.findViewById(R.id.mapview_dialog_image);
		//image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.apple_logo));
		
		TextView text = (TextView)responseDialog.findViewById(R.id.mapview_dialog_text);
		text.setText(item.getSnippet());
		
		Button closeButton = (Button)responseDialog.findViewById(R.id.mapview_dialog_button); 
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				responseDialog.dismiss();
			}
		});
//		responseDialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {
//			
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				
//			}
//		});
		responseDialog.show();
		return true;
	}
}
