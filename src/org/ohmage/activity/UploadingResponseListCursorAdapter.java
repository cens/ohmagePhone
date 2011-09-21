package org.ohmage.activity;

import org.ohmage.R;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

public class UploadingResponseListCursorAdapter extends ResponseListCursorAdapter {
	
	public UploadingResponseListCursorAdapter(Context context, Cursor c, int flags){
		super(context, c, flags);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor c) {
		super.bindView(view, context, c);
		
//		ImageView iconImage = (ImageView) view.findViewById(R.id.icon_image);
//		TextView surveyText = (TextView) view.findViewById(R.id.main_text);
//		TextView campaignText = (TextView) view.findViewById(R.id.sub_text);
//		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);		
//		
//		iconImage.setVisibility(View.GONE);
//		actionButton.setVisibility(View.GONE);
//		surveyText.setText("Survey title");
//		campaignText.setText("Campaign name");
//		
//		actionButton.setImageResource(R.drawable.ic_menu_edit);
	}
	
	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {
		View view = super.newView(context, c, parent);
		view.findViewById(R.id.action_button).setVisibility(View.VISIBLE);
		return view;
	}
}
