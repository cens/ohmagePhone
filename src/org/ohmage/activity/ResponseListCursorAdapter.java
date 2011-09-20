package org.ohmage.activity;

import org.ohmage.R;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ResponseListCursorAdapter extends CursorAdapter {

	private LayoutInflater mInflater;
	
	public ResponseListCursorAdapter(Context context, Cursor c, int flags){
		super(context, c, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor c) {
		ImageView iconImage = (ImageView) view.findViewById(R.id.icon_image);
		TextView surveyText = (TextView) view.findViewById(R.id.main_text);
		TextView campaignText = (TextView) view.findViewById(R.id.sub_text);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);		
		
		iconImage.setVisibility(View.GONE);
		actionButton.setVisibility(View.GONE);
		surveyText.setText("Survey title");
		campaignText.setText("Campaign name");
		
		actionButton.setImageResource(R.drawable.ic_menu_edit);
	}

	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
