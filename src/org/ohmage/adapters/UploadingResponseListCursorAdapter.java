package org.ohmage.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import org.ohmage.R;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Response;
import org.ohmage.logprobe.Analytics;

public class UploadingResponseListCursorAdapter extends ResponseListCursorAdapter {
	
	private final SubActionClickListener mListener;
	
	public UploadingResponseListCursorAdapter(Context context, Cursor c, SubActionClickListener listener, int flags){
		super(context, c, flags);
		
		mListener = listener;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor c) {
		super.bindView(view, context, c);
		
		final long responseId = c.getLong(c.getColumnIndex(Responses._ID));
		final String uuid = c.getString(c.getColumnIndex(Responses.RESPONSE_UUID));
		
		view.findViewById(R.id.action_separator).setVisibility(View.VISIBLE);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		actionButton.setVisibility(View.VISIBLE);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Analytics.widget(v, null, uuid);
				mListener.onSubActionClicked(Responses.buildResponseUri(responseId));
			}
		});
		
		int status = c.getInt(c.getColumnIndex(Responses.RESPONSE_STATUS));
		
		// first off, clear animation for any non-animating states
		if (status != Response.STATUS_UPLOADING &&
			status != Response.STATUS_WAITING_FOR_LOCATION &&
			status != Response.STATUS_QUEUED)
			actionButton.clearAnimation();
		
		switch (status) {
		case Response.STATUS_STANDBY:
			actionButton.setContentDescription(context.getString(R.string.response_list_item_action_button_upload_description));
			actionButton.setImageResource(R.drawable.subaction_upload_response);
			break;
			
		case Response.STATUS_QUEUED:
			actionButton.setContentDescription(context.getString(R.string.response_list_item_action_button_queued_description));
			actionButton.setImageResource(R.drawable.subaction_queued_response);
			
			// makes the queued indicator fade in and out gently
			Animation queuedPulse = AnimationUtils.loadAnimation(context, R.anim.gentle_pulse);
			queuedPulse.setRepeatCount(Animation.INFINITE);
			actionButton.startAnimation(queuedPulse);
			
			break;
			
		case Response.STATUS_UPLOADING:
			actionButton.setContentDescription(context.getString(R.string.response_list_item_action_button_uploading_description));

			actionButton.setImageResource(R.drawable.spinner_white_48);

			// makes the progress indicator rotate
			// this will be stopped next state change, assuming we're not still uploading
			Animation rotation = AnimationUtils.loadAnimation(context, R.anim.clockwise_rotation);
			rotation.setRepeatCount(Animation.INFINITE);
			actionButton.startAnimation(rotation);
			
			break;
			
		case Response.STATUS_WAITING_FOR_LOCATION:
			actionButton.setContentDescription(context.getString(R.string.response_list_item_action_button_waiting_description));

			actionButton.setImageResource(R.drawable.subaction_location_pending_question);
			
			// makes the missing location indicator fade in and out gently
			Animation pulse = AnimationUtils.loadAnimation(context, R.anim.gentle_pulse);
			pulse.setRepeatCount(Animation.INFINITE);
			actionButton.startAnimation(pulse);
			
			break;
			
		case Response.STATUS_ERROR_AUTHENTICATION:
		case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
		case Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE:
		case Response.STATUS_ERROR_CAMPAIGN_STOPPED:
		case Response.STATUS_ERROR_INVALID_USER_ROLE:
		case Response.STATUS_ERROR_HTTP:
		case Response.STATUS_ERROR_OTHER:
			actionButton.setContentDescription(context.getString(R.string.response_list_item_action_button_error_description));
			actionButton.setImageResource(R.drawable.subaction_campaign_broken);
			break;
			
		case Response.STATUS_UPLOADED:
		case Response.STATUS_DOWNLOADED:
			//should never be in this state in this view
			break;
			
		default:
			//campaign is in some unknown state!
			break;
		}
	}
	
	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {
		return super.newView(context, c, parent);
	}
}
