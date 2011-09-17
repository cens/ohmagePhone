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

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MapViewItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<RHMapViewActivity.FeedbackMapOverlayItems> mOverlays = new ArrayList<RHMapViewActivity.FeedbackMapOverlayItems>();
	private Context mContext;
	
	public MapViewItemizedOverlay(Drawable defaultMarker){
		super(boundCenterBottom(defaultMarker));
	}
	
	public MapViewItemizedOverlay(Drawable defaultMarker, Context context) {
		  super(boundCenterBottom(defaultMarker));
		  mContext = context;
	}

	public void addOverlay(RHMapViewActivity.FeedbackMapOverlayItems overlay){
		mOverlays.add(overlay);
		populate();
	}
	
	@Override
	protected RHMapViewActivity.FeedbackMapOverlayItems createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	@Override
	protected boolean onTap(int index){
		RHMapViewActivity.FeedbackMapOverlayItems item = mOverlays.get(index);
		//AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		final Dialog responseDialog = new Dialog(mContext);
		
		responseDialog.setContentView(R.layout.mapview_custom_dialog);
		responseDialog.setTitle(item.getTitle());
		
		
		//TODO Add image if there image files
		Bitmap img = item.getImage();
		ImageView image = (ImageView)responseDialog.findViewById(R.id.mapview_dialog_image);
		if(img != null){
			image.setVisibility(View.VISIBLE);
			//image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.apple_logo));
			image.setImageBitmap(img);
		}
		else{
			image.setVisibility(View.GONE);
		}
		
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
