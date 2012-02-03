package org.ohmage.db.test;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

public class CampaignCursor extends MockArrayCursor<Campaign> {

	private static final int COLUMN_CAMPAIGN_URN = 0;
	private static final int COLUMN_CAMPAIGN_NAME = 1;
	private static final int COLUMN_CAMPAIGN_STATUS = 2;
	private static final int COLUMN_CAMPAIGN_PRIVACY = 3;
	private static final int COLUMN_CAMPAIGN_CREATED = 4;

	public static final String DEFAULT_CAMPAIGN_URN = "urn:fake:campaign";
	public static final String DEFAULT_CAMPAIGN_NAME = "Fake Campaign";

	public CampaignCursor(String[] projection, Campaign... campaigns) {
		super(projection, campaigns);
	}

	@Override
	protected int getLocalColumnIndex(String columnName) {
		if (Campaigns.CAMPAIGN_URN.equals(columnName))
			return COLUMN_CAMPAIGN_URN;
		else if (Campaigns.CAMPAIGN_NAME.equals(columnName))
			return COLUMN_CAMPAIGN_NAME;
		else if (Campaigns.CAMPAIGN_STATUS.equals(columnName))
			return COLUMN_CAMPAIGN_STATUS;
		else if (Campaigns.CAMPAIGN_PRIVACY.equals(columnName))
			return COLUMN_CAMPAIGN_PRIVACY;
		else if (Campaigns.CAMPAIGN_CREATED.equals(columnName))
			return COLUMN_CAMPAIGN_CREATED;
		return COLUMN_IGNORE;
	}

	@Override
	protected String getStringValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_CAMPAIGN_URN:
				return getObject().mUrn;
			case COLUMN_CAMPAIGN_NAME:
				return getObject().mName;
			case COLUMN_CAMPAIGN_PRIVACY:
				return getObject().mPrivacy;
			case COLUMN_CAMPAIGN_CREATED:
				return getObject().mCreationTimestamp;
			default:
				return "";
		}
	}

	@Override
	protected String getStringDefault(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_CAMPAIGN_URN:
				return DEFAULT_CAMPAIGN_URN;
			case COLUMN_CAMPAIGN_NAME:
				return DEFAULT_CAMPAIGN_NAME;
			default:
				return "";
		}
	}

	@Override
	protected long getLongValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected double getDoubleValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected float getFloatValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getIntValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_CAMPAIGN_STATUS:
				return getObject().mStatus;
			default:
				return 0;
		}
	}

	public static Campaign cloneCampaign(Campaign c) {
		Campaign c2 = new Campaign();
		c2.mUrn = c.mUrn;
		c2.mName = c.mName;
		c2.mCreationTimestamp = c.mCreationTimestamp;
		c2.mStatus = c.mStatus;
		return c2;
	}
}
