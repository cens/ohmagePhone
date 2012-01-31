package org.ohmage.db.test;

import org.ohmage.OhmageApplication;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.concurrent.CountDownLatch;

public class CampaignContentProvider extends DelegatingMockContentProvider {
	Campaign mCampaign;
	private final CountDownLatch latch;

	public CampaignContentProvider(Application application, String name) {
		super(application, name);
		latch = new CountDownLatch(1);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch(OhmageUriMatcher.getMatcher().match(uri)) {
			case OhmageUriMatcher.CAMPAIGNS:
			case OhmageUriMatcher.CAMPAIGN_BY_URN:
				try {
					latch.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return new CampaignCursor(projection, mCampaign);
			default:
				return new EmptyMockCursor();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if(mCampaign != null && values != null && values.containsKey(Campaigns.CAMPAIGN_STATUS)) {
			mCampaign.mStatus = values.getAsInteger(Campaigns.CAMPAIGN_STATUS);

			OhmageApplication.getContext().getContentResolver().notifyChange(uri, null);
			return 1;
		}
		return 0;
	}

	public void setCampaign(Campaign campaign) {
		mCampaign = campaign;
		latch.countDown();
	}
}