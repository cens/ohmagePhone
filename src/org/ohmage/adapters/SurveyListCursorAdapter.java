package org.ohmage.adapters;

import org.ohmage.R;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Survey;

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
	
	private final LayoutInflater mInflater;
	private final SubActionClickListener mListener;
	
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
		
		final String campaignUrn = cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN));
		final int campaignStatus = cursor.getInt(cursor.getColumnIndex(Campaigns.CAMPAIGN_STATUS));
		final String surveyId = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID));
		
		iconImage.setVisibility(View.GONE);
		titleText.setText(cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_TITLE)));
		campaignText.setText(campaignUrn);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(campaignStatus == Campaign.STATUS_READY) {
					mListener.onSubActionClicked(Campaigns.buildSurveysUri(campaignUrn, surveyId));
				} else {
					mListener.onSubActionClicked(null);
				}
			}
		});
		
		actionButton.setImageResource(R.drawable.ic_menu_edit);
		
		int status = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_STATUS));
		boolean anytime = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_ANYTIME)) == 0 ? false : true;
		
		if(campaignStatus == Campaign.STATUS_READY) {
			switch (status) {
				case Survey.STATUS_NORMAL:
					if (anytime) {
						// actionButton.setImageResource(R.drawable.ic_menu_pencil_yellow);
						actionButton.setImageResource(R.drawable.subaction_survey_ready);
					} else {
						// actionButton.setImageResource(R.drawable.ic_menu_pencil_grey);
						actionButton.setImageResource(R.drawable.subaction_survey_disabled);
						actionButton.setEnabled(false);
					}
					break;

				case Survey.STATUS_TRIGGERED:
					// actionButton.setImageResource(R.drawable.ic_menu_pencil_green);
					actionButton.setImageResource(R.drawable.subaction_survey_pending);
					break;

				default:
					//campaign is in some unknown state!
					actionButton.setVisibility(View.INVISIBLE);
					break;
			}
		} else {
			actionButton.setImageResource(R.drawable.subaction_survey_disabled);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
