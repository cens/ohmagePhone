package org.ohmage.db.test;

import org.ohmage.db.DbContract;

import android.content.UriMatcher;

public class OhmageUriMatcher {

	private static UriMatcher matcher;

	public static final int CAMPAIGNS = 0;
	public static final int CAMPAIGN_BY_URN = 1;
	public static final int CAMPAIGN_RESPONSES = 2;
	public static final int CAMPAIGN_SURVEYS = 3;
	public static final int SURVEY_BY_ID = 4;
	public static final int SURVEY_SURVEYPROMPTS = 5;
	public static final int CAMPAIGN_SURVEY_RESPONSES = 6;
	public static final int CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID = 7;
	public static final int CAMPAIGN_RESPONSES_PROMPTS_BY_ID = 8;
	public static final int SURVEYS = 9;
	public static final int SURVEYPROMPTS = 10;
	public static final int RESPONSES = 11;
	public static final int RESPONSE_BY_PID = 12;
	public static final int RESPONSE_PROMPTS = 13;
	public static final int PROMPTS = 14;
	public static final int PROMPT_BY_PID = 15;



	public static UriMatcher getMatcher() {
		if(matcher == null) {
			matcher = new UriMatcher(UriMatcher.NO_MATCH);

			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns", CAMPAIGNS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*", CAMPAIGN_BY_URN);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/responses", CAMPAIGN_RESPONSES);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys", CAMPAIGN_SURVEYS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*", SURVEY_BY_ID);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/prompts", SURVEY_SURVEYPROMPTS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses", CAMPAIGN_SURVEY_RESPONSES);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses/prompts/*", CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/responses/prompts/*", CAMPAIGN_RESPONSES_PROMPTS_BY_ID);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys", SURVEYS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys/prompts", SURVEYPROMPTS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses", RESPONSES);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#", RESPONSE_BY_PID);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#/prompts", RESPONSE_PROMPTS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts", PROMPTS);
			matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts/#", PROMPT_BY_PID);
		}
		return matcher;
	}
}
