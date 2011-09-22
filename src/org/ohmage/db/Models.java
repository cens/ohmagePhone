package org.ohmage.db;

import org.ohmage.db.DbContract.Campaigns;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class Models {

	public final static class Campaign {

		public static final int STATUS_READY = 0;
		public static final int STATUS_REMOTE = 1;
		public static final int STATUS_STOPPED = 2;
		public static final int STATUS_OUT_OF_DATE = 3;
		public static final int STATUS_INVALID_USER_ROLE = 4;
		public static final int STATUS_DELETED = 5;
		public static final int STATUS_VAGUE = 6;
		public static final int STATUS_DOWNLOADING = 7;

		public long _id;
		public String mUrn;
		public String mName;
		public String mDescription;
		public String mCreationTimestamp;
		public String mDownloadTimestamp;
		public String mXml;
		public int mStatus;
		public String mIcon;
		public String mPrivacy;

		/**
		 * Returns a list of Campaign objects from the given cursor.
		 * 
		 * @param cursor a cursor containing the fields specified in the Campaign schema, which is closed when this method returns.
		 * @return a List of Campaign objects
		 */
		public static List<Campaign> fromCursor(Cursor cursor) {
			List<Campaign> campaigns = new ArrayList<Campaign>();

			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {

				Campaign c = new Campaign();
				c._id = cursor.getLong(cursor.getColumnIndex(Campaigns._ID));
				c.mUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
				c.mName = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_NAME));
				c.mDescription = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_DESCRIPTION));
				c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
				c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_DOWNLOADED));
				c.mXml = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CONFIGURATION_XML));
				c.mStatus = cursor.getInt(cursor.getColumnIndex(Campaigns.CAMPAIGN_STATUS));
				c.mIcon = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_ICON));
				c.mPrivacy = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_PRIVACY));
				campaigns.add(c);

				cursor.moveToNext();
			}

			cursor.close();

			return campaigns;
		}

		public ContentValues toCV() {
			ContentValues values = new ContentValues();

			values.put(Campaigns.CAMPAIGN_URN, mUrn);
			values.put(Campaigns.CAMPAIGN_NAME, mName);
			values.put(Campaigns.CAMPAIGN_DESCRIPTION, mDescription);
			values.put(Campaigns.CAMPAIGN_CREATED, mCreationTimestamp);
			values.put(Campaigns.CAMPAIGN_DOWNLOADED, mDownloadTimestamp);
			values.put(Campaigns.CAMPAIGN_CONFIGURATION_XML, mXml);
			values.put(Campaigns.CAMPAIGN_STATUS, mStatus);
			values.put(Campaigns.CAMPAIGN_ICON, mIcon);
			values.put(Campaigns.CAMPAIGN_PRIVACY, mPrivacy);
			return values;
		}
	}
}
