package org.ohmage.db.test;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

public class CampaignCursor extends MockArrayCursor<Campaign> {

	private static final int COLUMN_CAMPAIGN_URN = 0;
	private static final int COLUMN_CAMPAIGN_NAME = 1;
	private static final int COLUMN_CAMPAIGN_STATUS = 2;

	public static final String DEFAULT_CAMPAIGN_URN = "urn:fake:campaign";

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
		return COLUMN_IGNORE;
	}

	@Override
	protected String getStringValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_CAMPAIGN_URN:
				return getObject().mUrn;
			case COLUMN_CAMPAIGN_NAME:
				return getObject().mName;
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
				return "Fake Campaign";
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
}
