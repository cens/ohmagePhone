package org.ohmage.loader;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

public class CampaignLoader extends CursorLoader {

	private String mCampaignUrn;

	public CampaignLoader(Context context) {
		super(context, Campaigns.CONTENT_URI, new String[] { Campaigns.CAMPAIGN_URN },
				Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, Campaigns.CAMPAIGN_CREATED + " DESC");
	}

	@Override
	public Cursor loadInBackground() {

		Cursor cursor = super.loadInBackground();
		if (cursor != null && cursor.moveToFirst()) {
			mCampaignUrn = cursor.getString(0);
		}
		return cursor;
	}

	public String getCampaignUrn() {
		return mCampaignUrn;
	}
}