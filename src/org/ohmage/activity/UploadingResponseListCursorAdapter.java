package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Response;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class UploadingResponseListCursorAdapter extends ResponseListCursorAdapter {
	
	private SubActionClickListener mListener;
	
	public UploadingResponseListCursorAdapter(Context context, Cursor c, SubActionClickListener listener, int flags){
		super(context, c, flags);
		
		mListener = listener;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor c) {
		super.bindView(view, context, c);
		
		final long responseId = c.getLong(c.getColumnIndex(Response._ID));
		
		view.findViewById(R.id.action_separator).setVisibility(View.VISIBLE);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		actionButton.setVisibility(View.VISIBLE);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mListener.onSubActionClicked(Response.getResponseByID(responseId));
			}
		});
		
		int status = c.getInt(c.getColumnIndex(Response.STATUS));
		
		switch (status) {
		case Response.STATUS_STANDBY:
			actionButton.setImageResource(R.drawable.ic_menu_upload);
			break;
			
		case Response.STATUS_QUEUED:
			actionButton.setImageResource(R.drawable.ic_menu_upload_you_tube);
			break;
		case Response.STATUS_UPLOADING:
			actionButton.setImageResource(R.drawable.spinner_black_48);
			break;
			
		case Response.STATUS_WAITING_FOR_LOCATION:
			actionButton.setImageResource(R.drawable.ic_menu_recent_history);
			break;
			
		case Response.STATUS_ERROR_AUTHENTICATION:
		case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
		case Response.STATUS_ERROR_INVALID_USER_ROLE:
		case Response.STATUS_ERROR_OTHER:
			actionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
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
