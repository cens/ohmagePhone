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
	Campaign[] mCampaigns;
	private final CountDownLatch latch;
	private String mLastSelection;

	public CampaignContentProvider(Application application, String name) {
		super(application, name);
		latch = new CountDownLatch(1);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		mLastSelection = selection;
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		switch(OhmageUriMatcher.getMatcher().match(uri)) {
			case OhmageUriMatcher.CAMPAIGNS:
				return new CampaignCursor(projection, mCampaigns);
			case OhmageUriMatcher.CAMPAIGN_BY_URN:
				return new CampaignCursor(projection, mCampaigns[0]);
			default:
				return new EmptyMockCursor();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if(mCampaigns != null && values != null && values.containsKey(Campaigns.CAMPAIGN_STATUS)) {
			for(Campaign campaign : mCampaigns) {
				campaign.mStatus = values.getAsInteger(Campaigns.CAMPAIGN_STATUS);
				OhmageApplication.getContext().getContentResolver().notifyChange(uri, null, false);
			}
			return mCampaigns.length;
		}
		return 0;
	}

	public void setCampaigns(Campaign... campaigns) {
		mCampaigns = campaigns;
		latch.countDown();
	}

	public Campaign[] getCampaigns() {
		return mCampaigns;
	}

	public String getLastSelection() {
		return mLastSelection;
	}
}