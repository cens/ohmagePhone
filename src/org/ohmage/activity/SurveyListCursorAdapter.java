package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class SurveyListCursorAdapter extends CursorAdapter{
	
	private LayoutInflater mInflater;
	private SubActionClickListener mListener;
	
	public SurveyListCursorAdapter(Context context, Cursor c, SubActionClickListener listener, int flags) {
		super(context, c, flags);
		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListener = listener;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ImageView iconImage = (ImageView) view.findViewById(R.id.icon_image);
		TextView titleText = (TextView) view.findViewById(R.id.main_text);
		TextView campaignText = (TextView) view.findViewById(R.id.sub_text);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		final String campaignUrn = cursor.getString(cursor.getColumnIndex(Survey.CAMPAIGN_URN));
		final String surveyId = cursor.getString(cursor.getColumnIndex(Survey.SURVEY_ID));
		
		iconImage.setVisibility(View.GONE);
		titleText.setText(cursor.getString(cursor.getColumnIndex(Survey.TITLE)));
		campaignText.setText(campaignUrn);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mListener.onSubActionClicked(Survey.getSurveyByID(campaignUrn, surveyId));
			}
		});
		
		actionButton.setImageResource(R.drawable.ic_menu_edit);
		
//		int status = cursor.getInt(cursor.getColumnIndex(Campaign.STATUS));
//		
//		switch (status) {
//		case Campaign.STATUS_REMOTE:
//			actionButton.setImageResource(R.drawable.ic_menu_add);
//			break;
//			
//		case Campaign.STATUS_READY:
//			actionButton.setImageResource(R.drawable.ic_menu_compose);
//			break;
//			
//		default:
//			//campaign is in some unknown state!
//			actionButton.setVisibility(View.INVISIBLE);
//			break;
//		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
