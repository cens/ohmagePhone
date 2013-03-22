package org.ohmage.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mobilizingcs.R;
import org.ohmage.ConfigHelper;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ResponseListCursorAdapter extends CursorAdapter {

	private final LayoutInflater mInflater;
	
	public ResponseListCursorAdapter(Context context, Cursor c, int flags){
		super(context, c, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView surveyText = (TextView) view.findViewById(R.id.main_text);
		TextView campaignText = (TextView) view.findViewById(R.id.sub_text);
		TextView timeText = (TextView) view.findViewById(R.id.extra_text_1);
		TextView dateText = (TextView) view.findViewById(R.id.extra_text_2);
		
		view.findViewById(R.id.icon_image).setVisibility(View.GONE);
		view.findViewById(R.id.action_separator).setVisibility(View.GONE);
		view.findViewById(R.id.action_button).setVisibility(View.GONE);
		
		surveyText.setText(cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_TITLE)));
		campaignText.setText(cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_NAME)));
		// Only show campaign name if we aren't in single campaign mode
		campaignText.setVisibility((ConfigHelper.isSingleCampaignMode()) ? View.GONE : View.VISIBLE);
		
		long millis = cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_TIME));
		TimeZone timezone = TimeZone.getTimeZone(cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_TIMEZONE)));

		DateFormat date = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
		date.setTimeZone(timezone);

		DateFormat time = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
		time.setTimeZone(timezone);

		timeText.setText(date.format(millis));
		dateText.setText(time.format(millis));
		timeText.setVisibility(View.VISIBLE);
		dateText.setVisibility(View.VISIBLE);
	}

	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {		
		return  mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
