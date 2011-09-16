package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.CampaignListFragment.OnCampaignClickListener;
import org.ohmage.db.DbContract.Campaign;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class CampaignListCursorAdapter extends CursorAdapter{
	
	private static final String TAG = "CampaignListCursorAdapter";
	
//	private Context mContext;
//	private Cursor mCursor;
	private LayoutInflater mInflater;
	private OnCampaignClickListener mListener;
	
	public CampaignListCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		
//		mContext = context;
//		mCursor = c;
		
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListener = (OnCampaignClickListener) context;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ImageView iconImage = (ImageView) view.findViewById(R.id.icon_image);
		TextView nameText = (TextView) view.findViewById(R.id.name_text);
		TextView urnText = (TextView) view.findViewById(R.id.urn_text);
		ImageButton actionButton = (ImageButton) view.findViewById(R.id.action_button);
		
		final String campaignUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
		
		iconImage.setImageResource(R.drawable.apple_logo);
		nameText.setText(cursor.getString(cursor.getColumnIndex(Campaign.NAME)));
		urnText.setText(campaignUrn);
		
		int status = cursor.getInt(cursor.getColumnIndex(Campaign.STATUS));
		
		switch (status) {
		case Campaign.STATUS_REMOTE:
			actionButton.setImageResource(R.drawable.ic_menu_add_small);
			actionButton.setFocusable(false);
			actionButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mListener.onCampaignActionClick(campaignUrn);
				}
			});
			break;

		default:
			break;
		}
		
//		view.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				mListener.onCampaignItemClick(campaignUrn);
//			}
//		});
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.campaign_list_item, null);
	}
}
