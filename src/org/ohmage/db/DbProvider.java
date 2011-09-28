package org.ohmage.db;

import org.ohmage.OhmageCache;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbHelper.Subqueries;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.utils.SelectionBuilder;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

/**
 * A ContentProvider which makes the contents of the campaign, survey, and response
 * database available to other parts of the application. If you intend to modify the database,
 * you should do so through this class or else ContentObservers and CursorLoaders won't automatically
 * update to reflect your changes.
 * 
 * A list of URIs which the content provider supports is below, along with the operations that
 * are supported for each (i.e. query, insert, update, delete):
 * 
 * campaigns
 * -- query: returns all campaigns
 * -- insert: adds a campaign (populates surveys and survey prompts accordingly)
 * 
 * campaigns/{urn}
 * -- query: returns the campaign with the URN specified by {urn}
 * -- delete: removes the campaign with URN {urn} (and deletes from surveys, survey prompts, responses, and prompt responses accordingly)
 * 
 * surveys
 * -- query: returns all surveys
 * 
 * campaigns/{urn}/surveys
 * -- query: returns all surveys for the campaign specified by {urn}
 * 
 * campaigns/{urn}/surveys/{id}
 * -- query: returns the survey with the ID specified by {id}, belonging to campaign with urn {urn}
 * 
 * campaigns/{urn}/surveys/{id}/prompts
 * -- query: returns all survey prompts associated with the survey having id {id}, belonging to campaign with urn {urn}
 * 
 *  surveys/prompts
 * -- query: returns all survey prompts, irrespective of survey (mostly for testing)
 * 
 * responses
 * -- query: returns all responses
 * -- insert: adds a response (populates prompt responses, too)
 * 
 * responses/#
 * -- query: returns the response specified by the primary key "#"
 * 
 * responses/#/prompts
 * -- query: returns all prompt responses for the response having the primary key "#"
 * 
 * prompts
 * -- query: returns all prompt responses (mostly for testing)
 * 
 * prompts/#
 * -- query: returns the prompt response specified by the primary key "#"
 * 
 * campaigns/{urn}/responses
 * -- query: returns all responses for the campaign specified by {urn}
 * 
 * campaigns/{urn}/surveys/{sid}/responses
 * -- query: returns all responses for the survey specified by {sid} within the campaign specified by {urn}
 * 
 * campaigns/{urn}/surveys/{sid}/responses/prompts/{pid}
 * -- query: returns all prompts of the given {pid} for the survey specified by {sid} within the campaign specified by {urn}
 * 
 * campaigns/{urn}/surveys/{sid}/responses/prompts/{pid}/{agg}
 * -- query: returns an aggregate function {agg} (one of "avg", "count", "max", "min, "total") for
 * -- the prompts of the given {pid} for the survey specified by {sid} within the campaign specified by {urn}
 * 
 * @author faisal
 *
 */
public class DbProvider extends ContentProvider {		
	private static UriMatcher sUriMatcher = buildUriMatcher();
	private DbHelper dbHelper;
	
	// enum of the URIs we can match using sUriMatcher
	private interface MatcherTypes {
		int RESPONSES = 1;
		int RESPONSE_BY_PID = 2;
		int CAMPAIGN_RESPONSES = 3;
		int CAMPAIGN_SURVEY_RESPONSES = 4;
		int SURVEYS = 5;
		int CAMPAIGN_SURVEYS = 6;
		int SURVEY_BY_ID = 7;
		int RESPONSE_PROMPTS = 8;
		int CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID = 9;
		int PROMPTS = 10;
		int PROMPT_BY_PID = 11;
		int CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE = 12;
		int CAMPAIGNS = 13;
		int CAMPAIGN_BY_URN = 14;
		int SURVEY_SURVEYPROMPTS = 15;
		int SURVEYPROMPTS = 16;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        	
        	// CAMPAIGNS
        	case MatcherTypes.CAMPAIGNS:
        		return Campaigns.CONTENT_TYPE;
        	case MatcherTypes.CAMPAIGN_BY_URN:
        		return Campaigns.CONTENT_ITEM_TYPE;
        		
        	// SURVEYS
        	case MatcherTypes.SURVEYS:
        	case MatcherTypes.CAMPAIGN_SURVEYS:
        		return Surveys.CONTENT_TYPE;
        	case MatcherTypes.SURVEY_BY_ID:
        		return Surveys.CONTENT_ITEM_TYPE;
        		
        	// SURVEY PROMPTS
        	case MatcherTypes.SURVEY_SURVEYPROMPTS:
        		return SurveyPrompts.CONTENT_TYPE;
        	
        	// RESPONSES
            case MatcherTypes.RESPONSES:
            case MatcherTypes.CAMPAIGN_RESPONSES:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES:
            	return Responses.CONTENT_TYPE;
            case MatcherTypes.RESPONSE_BY_PID:
            	return Responses.CONTENT_ITEM_TYPE;
            
            // PROMPTS
            case MatcherTypes.PROMPTS:
            case MatcherTypes.RESPONSE_PROMPTS:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE:
            	return PromptResponses.CONTENT_TYPE;
            case MatcherTypes.PROMPT_BY_PID:
            	return PromptResponses.CONTENT_ITEM_TYPE;
            	
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// get a handle to our db
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		// feed the uri to our selection builder, which will
		// nab the appropriate rows from the right table.
		SelectionBuilder builder = buildSelection(uri, false);

		builder.where(selection, selectionArgs);
		
		Cursor result = builder.query(db, projection, sortOrder);
		result.setNotificationUri(getContext().getContentResolver(), uri);
		
		return result;
	}
	
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long insertID = -1;
		Uri resultingUri = null;
		String campaignUrn, surveyID;
		
		ContentResolver cr = getContext().getContentResolver();
		
		switch (sUriMatcher.match(uri)) {
			case MatcherTypes.RESPONSES:
				insertID = dbHelper.addResponseRow(db, values);
				campaignUrn = values.getAsString(Responses.CAMPAIGN_URN);
				surveyID = values.getAsString(Responses.SURVEY_ID);
				resultingUri = Responses.buildResponseUri(insertID);
				
				// notify on the related entity URIs
				cr.notifyChange(Responses.CONTENT_URI, null);
				cr.notifyChange(PromptResponses.CONTENT_URI, null);
				
				break;
			case MatcherTypes.CAMPAIGNS:
				insertID = dbHelper.addCampaign(db, values);
				campaignUrn = values.getAsString(Campaigns.CAMPAIGN_URN);
				resultingUri = Campaigns.buildCampaignUri(campaignUrn);

				// notify on the related entity URIs
				cr.notifyChange(Campaigns.CONTENT_URI, null);
				cr.notifyChange(Surveys.CONTENT_URI, null);
				cr.notifyChange(SurveyPrompts.CONTENT_URI, null);
				
				break;
			default:
				throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
		}
		
		// return the path to our new URI
		return resultingUri;
	}

	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// get a handle to our db
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		
		// TODO: should we reject entities that shouldn't be updated?
		
		// feed the uri to our selection builder, which will
		// nab the appropriate rows from the right table.
		SelectionBuilder builder = buildSelection(uri, true);
		
		// we should also add on the client's selection
		builder.where(selection, selectionArgs);
		
		// we assume we've matched it correctly, so proceed with the update
		count = builder.update(db, values);
		
		if (count > 0) {
			ContentResolver cr = getContext().getContentResolver();
			
			// depending on the type of the thing deleted, we have to notify potentially many URIs
			switch (sUriMatcher.match(uri)) {
				case MatcherTypes.RESPONSE_BY_PID:
				case MatcherTypes.RESPONSES:
					// notify on the related entity URIs
					cr.notifyChange(Responses.CONTENT_URI, null);
					cr.notifyChange(PromptResponses.CONTENT_URI, null);
					break;
					
				case MatcherTypes.CAMPAIGN_BY_URN:
				case MatcherTypes.CAMPAIGNS:
					// if it was a campaign, we may need to update the campaign's xml
					if (values.containsKey(Campaigns.CAMPAIGN_URN) && values.containsKey(Campaigns.CAMPAIGN_CONFIGURATION_XML))
						dbHelper.populateSurveysFromCampaignXML(db, values.getAsString(Campaigns.CAMPAIGN_URN), values.getAsString(Campaigns.CAMPAIGN_CONFIGURATION_XML));
					
					// notify on the related entity URIs
					cr.notifyChange(Campaigns.CONTENT_URI, null);
					cr.notifyChange(Surveys.CONTENT_URI, null);
					cr.notifyChange(SurveyPrompts.CONTENT_URI, null);
					cr.notifyChange(Responses.CONTENT_URI, null);
					cr.notifyChange(PromptResponses.CONTENT_URI, null);
					break;
			}
			
			// we should always notify on our own uri regardless
			cr.notifyChange(uri, null);
		}
		
		return count;
	}
	
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		// get a handle to our db
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		
		// TODO: should we reject entities that shouldn't be deleted?
		
		// feed the uri to our selection builder, which will
		// nab the appropriate rows from the right table.
		SelectionBuilder builder = buildSelection(uri, true);
		
		// we should also add on the client's selection
		builder.where(selection, selectionArgs);
		
		// Depending on the type of the thing deleted, we may have to delete the icon associated with it
		// We need to know what the url is before we delete it
		HashSet<String> iconUrls = new HashSet<String>();
		switch (sUriMatcher.match(uri)) {
			case MatcherTypes.CAMPAIGN_BY_URN:
			case MatcherTypes.CAMPAIGNS:
				Cursor c = builder.query(db, new String [] { Campaigns.CAMPAIGN_ICON }, null);
				if(c.moveToFirst()) {
					while(c.moveToNext()) {
						if(c.getString(0) != null)
							iconUrls.add(c.getString(0));
					}
				}
				c.close();
		}
		
		// we assume we've matched it correctly, so proceed with the delete
		count = builder.delete(db);
		
		if (count > 0) {
			ContentResolver cr = getContext().getContentResolver();
			
			// depending on the type of the thing deleted, we have to notify potentially many URIs
			switch (sUriMatcher.match(uri)) {
				case MatcherTypes.RESPONSE_BY_PID:
				case MatcherTypes.RESPONSES:
					// notify on the related entity URIs
					cr.notifyChange(Responses.CONTENT_URI, null);
					cr.notifyChange(PromptResponses.CONTENT_URI, null);
					break;
					
				case MatcherTypes.CAMPAIGN_BY_URN:
				case MatcherTypes.CAMPAIGNS:
					// Delete the icon if it is on the sdcard and no other campaigns reference that url
					for(String iconUrl : iconUrls) {
						db.beginTransaction();
						try {
							Cursor c = db.query(Tables.CAMPAIGNS, new String [] { "_id" }, Campaigns.CAMPAIGN_ICON + "=?", new String[] { iconUrl }, null, null, null);
							if(c.getCount() == 0) {
								try {
									OhmageCache.getCachedFile(getContext(), new URI(iconUrl)).delete();
								} catch (URISyntaxException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								c.close();
							}
							db.setTransactionSuccessful();
						} finally {
							db.endTransaction();
						}
					}	

					// notify on the related entity URIs
					cr.notifyChange(Campaigns.CONTENT_URI, null);
					cr.notifyChange(Surveys.CONTENT_URI, null);
					cr.notifyChange(SurveyPrompts.CONTENT_URI, null);
					cr.notifyChange(Responses.CONTENT_URI, null);
					cr.notifyChange(PromptResponses.CONTENT_URI, null);
					break;
			}
			
			// we should always notify on our own uri regardless
			cr.notifyChange(uri, null);
		}
		
		return count;
	}

	// ====================================
	// === definitions for URI resolver and entity type maps
	// ====================================

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns", MatcherTypes.CAMPAIGNS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*", MatcherTypes.CAMPAIGN_BY_URN);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/responses", MatcherTypes.CAMPAIGN_RESPONSES);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys", MatcherTypes.CAMPAIGN_SURVEYS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*", MatcherTypes.SURVEY_BY_ID);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/prompts", MatcherTypes.SURVEY_SURVEYPROMPTS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses/prompts/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses/prompts/*/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys", MatcherTypes.SURVEYS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys/prompts", MatcherTypes.SURVEYPROMPTS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses", MatcherTypes.RESPONSES);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#", MatcherTypes.RESPONSE_BY_PID);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#/prompts", MatcherTypes.RESPONSE_PROMPTS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts", MatcherTypes.PROMPTS);
		matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts/#", MatcherTypes.PROMPT_BY_PID);

		return matcher;
	}

	private SelectionBuilder buildSelection(Uri uri, boolean nonQuery) {
		final SelectionBuilder builder = new SelectionBuilder();

		final int match = sUriMatcher.match(uri);

		switch (match) {
			case MatcherTypes.CAMPAIGNS: {
				return builder.table(Tables.CAMPAIGNS);
			}
			case MatcherTypes.CAMPAIGN_BY_URN: {
				final String campaignUrn = Campaigns.getCampaignUrn(uri);

				return builder.table(Tables.CAMPAIGNS)
						.where(Campaigns.CAMPAIGN_URN + "=?", campaignUrn);
			}
			case MatcherTypes.CAMPAIGN_RESPONSES: {
				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				
				if (nonQuery)
					return builder.table(Tables.RESPONSES)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn);
				
				return builder.table(Tables.RESPONSES_JOIN_CAMPAIGNS_SURVEYS)
						.mapToTable(Responses._ID, Tables.RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn);
			}
			case MatcherTypes.CAMPAIGN_SURVEYS: {
				final String campaignUrn = Campaigns.getCampaignUrn(uri);

				return builder.table(Tables.SURVEYS)
						.where(Surveys.CAMPAIGN_URN + "=?", campaignUrn);
			}
			case MatcherTypes.SURVEY_BY_ID: {
				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				final String surveyId = Surveys.getSurveyId(uri);
				
				if (nonQuery)
					return builder.table(Tables.SURVEYS)
						.where(Qualified.SURVEYS_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Surveys.SURVEY_ID + "=?", surveyId);

				return builder.table(Tables.SURVEYS)
						.join(Tables.CAMPAIGNS, "%t." + Campaigns.CAMPAIGN_URN + "=" + "%s." + Surveys.CAMPAIGN_URN)
						.mapToTable(Surveys._ID, Tables.SURVEYS)
						.mapToTable(Surveys.CAMPAIGN_URN, Tables.SURVEYS)
						.where(Qualified.SURVEYS_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Surveys.SURVEY_ID + "=?", surveyId);
			}
			case MatcherTypes.SURVEY_SURVEYPROMPTS: {
				if (nonQuery)
					throw new UnsupportedOperationException("buildSelection(): update/delete attempted on a URI which does not support it: " + uri.toString());

				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				final String surveyId = Surveys.getSurveyId(uri);

				return builder.table(Tables.SURVEY_PROMPTS_JOIN_SURVEYS)
						.mapToTable(SurveyPrompts._ID, Tables.SURVEY_PROMPTS)
						.mapToTable(SurveyPrompts.SURVEY_ID, Tables.SURVEY_PROMPTS)
						.where(Surveys.CAMPAIGN_URN + "=?", campaignUrn)
						.where(Qualified.SURVEY_PROMPTS_SURVEY_ID + "=?", surveyId);
			}
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES: {
				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				final String surveyId = Surveys.getSurveyId(uri);
				
				if (nonQuery)
					return builder.table(Tables.RESPONSES)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Qualified.RESPONSES_SURVEY_ID + "=?", surveyId);

				return builder.table(Tables.RESPONSES_JOIN_CAMPAIGNS_SURVEYS)
						.mapToTable(Responses._ID, Tables.RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Qualified.RESPONSES_SURVEY_ID + "=?", surveyId);
			}
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID: {
				if (nonQuery)
					throw new UnsupportedOperationException("buildSelection(): update/delete attempted on a URI which does not support it: " + uri.toString());

				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				final String surveyId = Surveys.getSurveyId(uri);
				final String promptId = PromptResponses.getSurveyPromptId(uri);

				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
						.where("SQ." + SurveyPrompts.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.COMPOSITE_ID)
						.mapToTable(PromptResponses._ID, Tables.PROMPT_RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Qualified.RESPONSES_SURVEY_ID + "=?", surveyId)
						.where(PromptResponses.PROMPT_ID + "=?", promptId);
			}	
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE: {
				if (nonQuery)
					throw new UnsupportedOperationException("buildSelection(): update/delete attempted on a URI which does not support it: " + uri.toString());

				final String campaignUrn = Campaigns.getCampaignUrn(uri);
				final String surveyId = Surveys.getSurveyId(uri);
				final String promptId = PromptResponses.getSurveyPromptId(uri);
				String aggregate = uri.getPathSegments().get(7);

				String toClause;

				switch (DbContract.PromptResponses.AggregateTypes.valueOf(aggregate)) {
					case AVG: toClause = "avg(" + PromptResponses.PROMPT_RESPONSE_VALUE + ")"; break;
					case COUNT: toClause = "count(" + PromptResponses.PROMPT_RESPONSE_VALUE + ")"; break;
					case MAX: toClause = "max(" + PromptResponses.PROMPT_RESPONSE_VALUE + ")"; break;
					case MIN: toClause = "min(" + PromptResponses.PROMPT_RESPONSE_VALUE + ")"; break;
					case TOTAL: toClause = "total(" + PromptResponses.PROMPT_RESPONSE_VALUE + ")"; break;
					default:
						throw new IllegalArgumentException("Specified aggregate was not one of AggregateTypes");
				}

				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
						.where("SQ." + SurveyPrompts.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.COMPOSITE_ID)
						.mapToTable(PromptResponses._ID, Tables.PROMPT_RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.map("aggregate", toClause)
						.where(Qualified.RESPONSES_CAMPAIGN_URN + "=?", campaignUrn)
						.where(Qualified.RESPONSES_SURVEY_ID + "=?", surveyId)
						.where(PromptResponses.PROMPT_ID + "=?", promptId);
			}
			case MatcherTypes.SURVEYS: {
				return builder.table(Tables.SURVEYS);
			}
			case MatcherTypes.SURVEYPROMPTS: {
				return builder.table(Tables.SURVEY_PROMPTS);
			}
			case MatcherTypes.RESPONSES: {
				if (nonQuery)
					return builder.table(Tables.RESPONSES);
				
				return builder.table(Tables.RESPONSES_JOIN_CAMPAIGNS_SURVEYS)
						.mapToTable(Responses._ID, Tables.RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES);
			}
			case MatcherTypes.RESPONSE_BY_PID: {
				final String responseId = Responses.getResponseId(uri);
				
				if (nonQuery)
					return builder.table(Tables.RESPONSES)
						.where(Qualified.RESPONSES_ID + "=?", responseId);

				return builder.table(Tables.RESPONSES_JOIN_CAMPAIGNS_SURVEYS)
						.mapToTable(Responses._ID, Tables.RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.where(Qualified.RESPONSES_ID + "=?", responseId);
			}
			case MatcherTypes.RESPONSE_PROMPTS: {
				final String responseId = Responses.getResponseId(uri);
				
				if (nonQuery)
					throw new UnsupportedOperationException("buildSelection(): update/delete attempted on a URI which does not support it: " + uri.toString());

				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
						.where("SQ." + SurveyPrompts.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.COMPOSITE_ID)
						.where("SQ." + SurveyPrompts.PROMPT_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.PROMPT_ID)
						.mapToTable(PromptResponses._ID, Tables.PROMPT_RESPONSES)
						.mapToTable(PromptResponses.RESPONSE_ID, Tables.PROMPT_RESPONSES)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES)
						.where(Tables.RESPONSES + "." + Responses._ID + "=?", responseId);
			}
			case MatcherTypes.PROMPTS: {
				if (nonQuery)
					return builder.table(Tables.PROMPT_RESPONSES);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
						.where("SQ." + SurveyPrompts.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.COMPOSITE_ID)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES);
			}
			case MatcherTypes.PROMPT_BY_PID: {
				final String promptId = SurveyPrompts.getSurveyPromptId(uri);
				
				if (nonQuery)
					return builder.table(Tables.PROMPT_RESPONSES)
						.where(PromptResponses._ID + "=?", promptId);

				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
						.where("SQ." + SurveyPrompts.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponses.COMPOSITE_ID)
						.where(PromptResponses._ID + "=?", promptId)
						.mapToTable(Responses.CAMPAIGN_URN, Tables.RESPONSES)
						.mapToTable(Responses.SURVEY_ID, Tables.RESPONSES);
			}
			default:
				throw new UnsupportedOperationException("buildSelection(): Unknown URI: " + uri);
		}
	}

	/**
	 * {@link DbContract} fields that are fully qualified with a specific
	 * parent {@link Tables}. Used when needed to work around SQL ambiguity.
	 */
	private interface Qualified {
		String SURVEYS_CAMPAIGN_URN = Tables.SURVEYS + "." + Surveys.CAMPAIGN_URN;
		String RESPONSES_CAMPAIGN_URN = Tables.RESPONSES + "." + Responses.CAMPAIGN_URN;
		String RESPONSES_SURVEY_ID = Tables.RESPONSES + "." + Responses.SURVEY_ID;
		String SURVEY_PROMPTS_SURVEY_ID = Tables.SURVEY_PROMPTS + "." + SurveyPrompts.SURVEY_ID;
		String RESPONSES_ID = Tables.RESPONSES + "." + Responses._ID;
	}
}