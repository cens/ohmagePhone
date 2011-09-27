package org.ohmage.activity;

import com.google.android.imageloader.ImageLoader;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

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

public class CampaignListCursorAdapter extends CursorAdapter{
	
	private final LayoutInflater mInflater;
	private final SubActionClickListener mListener;
	private final ImageLoader mImageLoader;
	
	public CampaignListCursorAdapter(Context context, Cursor c, SubActionClickListener listener, int flags) {
		super(context, c, flags);
		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListener = listener;
        mImageLoader = ImageLoader.get(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ImageView iconImage = (ImageView) view.findViewById(R.id.icon_image);
		TextView nameText = (TextView) view.findViewById(R.id.main_text);
		TextView urnText = (TextView) view.findViewById(R.id.sub_text);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		final String campaignUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
		
		final String iconUrl = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_ICON));
		if(iconUrl == null || mImageLoader.bind(this, iconImage, iconUrl) != ImageLoader.BindResult.OK) {
			iconImage.setImageResource(R.drawable.apple_logo);
		}

		nameText.setText(cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_NAME)));
		urnText.setText(campaignUrn);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mListener.onSubActionClicked(Campaigns.buildCampaignUri(campaignUrn));
			}
		});
		
		int status = cursor.getInt(cursor.getColumnIndex(Campaigns.CAMPAIGN_STATUS));
		
		switch (status) {
		case Campaign.STATUS_REMOTE:
			actionButton.setImageResource(R.drawable.ic_menu_add);
			break;
			
		case Campaign.STATUS_READY:
			actionButton.setImageResource(R.drawable.ic_menu_compose);
			break;
			
		case Campaign.STATUS_STOPPED:
		case Campaign.STATUS_OUT_OF_DATE:
		case Campaign.STATUS_INVALID_USER_ROLE:
		case Campaign.STATUS_DELETED:
		case Campaign.STATUS_VAGUE:
			actionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
			break;
			
		case Campaign.STATUS_DOWNLOADING:
			actionButton.setImageResource(R.drawable.spinner_black_48);
			break;
			
		default:
			//campaign is in some unknown state!
			actionButton.setVisibility(View.INVISIBLE);
			break;
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_list_item, null);
	}
}
