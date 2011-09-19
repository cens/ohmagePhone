package org.ohmage.db;

import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.db.DbContract.SurveyPrompt;
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
        		return Campaign.CONTENT_TYPE;
        	case MatcherTypes.CAMPAIGN_BY_URN:
        		return Campaign.CONTENT_ITEM_TYPE;
        		
        	// SURVEYS
        	case MatcherTypes.SURVEYS:
        	case MatcherTypes.CAMPAIGN_SURVEYS:
        		return Survey.CONTENT_TYPE;
        	case MatcherTypes.SURVEY_BY_ID:
        		return Survey.CONTENT_ITEM_TYPE;
        		
        	// SURVEY PROMPTS
        	case MatcherTypes.SURVEY_SURVEYPROMPTS:
        		return SurveyPrompt.CONTENT_TYPE;
        	
        	// RESPONSES
            case MatcherTypes.RESPONSES:
            case MatcherTypes.CAMPAIGN_RESPONSES:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES:
            	return Response.CONTENT_TYPE;
            case MatcherTypes.RESPONSE_BY_PID:
            	return Response.CONTENT_ITEM_TYPE;
            
            // PROMPTS
            case MatcherTypes.PROMPTS:
            case MatcherTypes.RESPONSE_PROMPTS:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE:
            	return PromptResponse.CONTENT_TYPE;
            case MatcherTypes.PROMPT_BY_PID:
            	return PromptResponse.CONTENT_ITEM_TYPE;
            	
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
				campaignUrn = values.getAsString(Response.CAMPAIGN_URN);
				surveyID = values.getAsString(Response.SURVEY_ID);
				resultingUri = Response.getResponseByID(insertID);
				
				// notify on the related entity URIs
				cr.notifyChange(Response.CONTENT_URI, null);
				cr.notifyChange(PromptResponse.CONTENT_URI, null);
				
				break;
			case MatcherTypes.CAMPAIGNS:
				insertID = dbHelper.addCampaign(db, values);
				campaignUrn = values.getAsString(Campaign.URN);
				resultingUri = Campaign.getCampaignByURN(campaignUrn);

				// notify on the related entity URIs
				cr.notifyChange(Campaign.CONTENT_URI, null);
				cr.notifyChange(Survey.CONTENT_URI, null);
				cr.notifyChange(SurveyPrompt.CONTENT_URI, null);
				
				break;
			default:
				throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
		}
		
		db.close();
		
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
					cr.notifyChange(Response.CONTENT_URI, null);
					cr.notifyChange(PromptResponse.CONTENT_URI, null);
					break;
					
				case MatcherTypes.CAMPAIGN_BY_URN:
				case MatcherTypes.CAMPAIGNS:
					// if it was a campaign, we may need to update the campaign's xml
					if (values.containsKey(Campaign.URN) && values.containsKey(Campaign.CONFIGURATION_XML))
						dbHelper.populateSurveysFromCampaignXML(db, values.getAsString(Campaign.URN), values.getAsString(Campaign.CONFIGURATION_XML));
					
					// notify on the related entity URIs
					cr.notifyChange(Campaign.CONTENT_URI, null);
					cr.notifyChange(Survey.CONTENT_URI, null);
					cr.notifyChange(SurveyPrompt.CONTENT_URI, null);
					cr.notifyChange(Response.CONTENT_URI, null);
					cr.notifyChange(PromptResponse.CONTENT_URI, null);
					break;
			}
			
			// we should always notify on our own uri regardless
			cr.notifyChange(uri, null);
		}
		
		db.close();
		
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
		
		// we assume we've matched it correctly, so proceed with the delete
		count = builder.delete(db);
		
		db.close();
		
		if (count > 0) {
			ContentResolver cr = getContext().getContentResolver();
			
			// depending on the type of the thing deleted, we have to notify potentially many URIs
			switch (sUriMatcher.match(uri)) {
				case MatcherTypes.RESPONSE_BY_PID:
				case MatcherTypes.RESPONSES:
					// notify on the related entity URIs
					cr.notifyChange(Response.CONTENT_URI, null);
					cr.notifyChange(PromptResponse.CONTENT_URI, null);
					break;
					
				case MatcherTypes.CAMPAIGN_BY_URN:
				case MatcherTypes.CAMPAIGNS:			
					// notify on the related entity URIs
					cr.notifyChange(Campaign.CONTENT_URI, null);
					cr.notifyChange(Survey.CONTENT_URI, null);
					cr.notifyChange(SurveyPrompt.CONTENT_URI, null);
					cr.notifyChange(Response.CONTENT_URI, null);
					cr.notifyChange(PromptResponse.CONTENT_URI, null);
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
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys", MatcherTypes.SURVEYS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys", MatcherTypes.CAMPAIGN_SURVEYS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*", MatcherTypes.SURVEY_BY_ID);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/prompts", MatcherTypes.SURVEY_SURVEYPROMPTS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "surveys/prompts", MatcherTypes.SURVEYPROMPTS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses", MatcherTypes.RESPONSES);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#", MatcherTypes.RESPONSE_BY_PID);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "responses/#/prompts", MatcherTypes.RESPONSE_PROMPTS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts", MatcherTypes.PROMPTS);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "prompts/#", MatcherTypes.PROMPT_BY_PID);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/responses", MatcherTypes.CAMPAIGN_RESPONSES);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses/prompts/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID);
        matcher.addURI(DbContract.CONTENT_AUTHORITY, "campaigns/*/surveys/*/responses/prompts/*/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE);
        
        return matcher;
    }
	
	private SelectionBuilder buildSelection(Uri uri, boolean nonQuery) {
		final SelectionBuilder builder = new SelectionBuilder();
		
		// vars used below
		// defined here because case statements are reasonably not scoped
		String campaignUrn, surveyID, responseID, promptID;
		
		final int match = sUriMatcher.match(uri);
		
		switch (match) {
			// CAMPAIGNS
			case MatcherTypes.CAMPAIGNS:
				return builder.table(Tables.CAMPAIGNS);
				
			case MatcherTypes.CAMPAIGN_BY_URN:
				campaignUrn = uri.getPathSegments().get(1);
				
				return builder.table(Tables.CAMPAIGNS)
					.where(Campaign.URN + "=?", campaignUrn);
				
			// SURVEYS
			case MatcherTypes.SURVEYS:
				return builder.table(Tables.SURVEYS);
				
			case MatcherTypes.SURVEY_BY_ID:
				campaignUrn = uri.getPathSegments().get(1);
				surveyID = uri.getPathSegments().get(3);
				
				return builder.table(Tables.SURVEYS)
					.where(Survey.CAMPAIGN_URN + "=?", campaignUrn)
					.where(Survey.SURVEY_ID + "=?", surveyID);
				
			case MatcherTypes.CAMPAIGN_SURVEYS:
				campaignUrn = uri.getPathSegments().get(1);
				
				return builder.table(Tables.SURVEYS)
					.where(Survey.CAMPAIGN_URN + "=?", campaignUrn);
				
			// SURVEY PROMPTS
			case MatcherTypes.SURVEYPROMPTS:
				return builder.table(Tables.SURVEY_PROMPTS);
				
			case MatcherTypes.SURVEY_SURVEYPROMPTS:
				campaignUrn = uri.getPathSegments().get(1);
				surveyID = uri.getPathSegments().get(3);
				
				return builder.table(Tables.SURVEY_PROMPTS_JOIN_SURVEYS)
					.mapToTable(Survey.CAMPAIGN_URN, Tables.SURVEYS)
					.where(Tables.SURVEYS + "." + Survey.CAMPAIGN_URN + "=?", campaignUrn)
					.where(Tables.SURVEY_PROMPTS + "." + SurveyPrompt.SURVEY_ID + "=?", surveyID);
				
			// RESPONSES
			case MatcherTypes.RESPONSES:
				if (nonQuery)
					return builder.table(Tables.RESPONSES);
				return builder.table(Tables.RESPONSES_JOIN_CAMPAIGNS)
					.mapToTable(Campaign.NAME, Tables.CAMPAIGNS);
				
			case MatcherTypes.RESPONSE_BY_PID:
				responseID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.RESPONSES)
					.where(Response._ID + "=?", responseID);
				
			case MatcherTypes.CAMPAIGN_RESPONSES:
				campaignUrn = uri.getPathSegments().get(1);
				
				return builder.table(Tables.RESPONSES)
					.where(Response.CAMPAIGN_URN + "=?", campaignUrn);
				
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES:
				campaignUrn = uri.getPathSegments().get(1);
				surveyID = uri.getPathSegments().get(3);
				
				return builder.table(Tables.RESPONSES)
					.where(Response.CAMPAIGN_URN + "=?", campaignUrn)
					.where(Response.SURVEY_ID + "=?", surveyID);
			
			// PROMPTS
			case MatcherTypes.PROMPTS:
				if (nonQuery)
					return builder.table(Tables.PROMPT_RESPONSES);
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
					.where("SQ." + SurveyPrompt.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.COMPOSITE_ID);
				
			case MatcherTypes.PROMPT_BY_PID:
				promptID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
					.where("SQ." + SurveyPrompt.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.COMPOSITE_ID)
					.where(PromptResponse._ID + "=?", promptID);
				
			case MatcherTypes.RESPONSE_PROMPTS:
				responseID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
					.where("SQ." + SurveyPrompt.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.COMPOSITE_ID)
					.where("SQ." + SurveyPrompt.PROMPT_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.PROMPT_ID)
					.mapToTable(PromptResponse._ID, Tables.PROMPT_RESPONSES)
					.mapToTable(PromptResponse.RESPONSE_ID, Tables.PROMPT_RESPONSES)
					.where(Tables.RESPONSES + "." + Response._ID + "=?", responseID);
				
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID:
				campaignUrn = uri.getPathSegments().get(1);
				surveyID = uri.getPathSegments().get(3);
				promptID = uri.getPathSegments().get(6);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
					.where("SQ." + SurveyPrompt.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.COMPOSITE_ID)
					.mapToTable(PromptResponse._ID, Tables.PROMPT_RESPONSES)
					.mapToTable(PromptResponse.RESPONSE_ID, Tables.PROMPT_RESPONSES)
					.where(Response.CAMPAIGN_URN + "=?", campaignUrn)
					.where(Response.SURVEY_ID + "=?", surveyID)
					.where(Tables.PROMPT_RESPONSES + "." + PromptResponse.PROMPT_ID + "=?", promptID);
				
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE:
				campaignUrn = uri.getPathSegments().get(1);
				surveyID = uri.getPathSegments().get(3);
				promptID = uri.getPathSegments().get(6);
				String aggregate = uri.getPathSegments().get(7);
				
				String toClause;
				
				switch (DbContract.PromptResponse.AggregateTypes.valueOf(aggregate)) {
					case AVG: toClause = "avg(" + PromptResponse.PROMPT_VALUE + ")"; break;
					case COUNT: toClause = "count(" + PromptResponse.PROMPT_VALUE + ")"; break;
					case MAX: toClause = "max(" + PromptResponse.PROMPT_VALUE + ")"; break;
					case MIN: toClause = "min(" + PromptResponse.PROMPT_VALUE + ")"; break;
					case TOTAL: toClause = "total(" + PromptResponse.PROMPT_VALUE + ")"; break;
					default:
						throw new IllegalArgumentException("Specified aggregate was not one of AggregateTypes");
				}
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS + ", " + Subqueries.PROMPTS_GET_TYPES + " SQ")
					.where("SQ." + SurveyPrompt.COMPOSITE_ID + "=" + Tables.PROMPT_RESPONSES + "." + PromptResponse.COMPOSITE_ID)
					.mapToTable(PromptResponse._ID, Tables.PROMPT_RESPONSES)
					.mapToTable(PromptResponse.RESPONSE_ID, Tables.PROMPT_RESPONSES)
					.map("aggregate", toClause)
					.where(Response.CAMPAIGN_URN + "=?", campaignUrn)
					.where(Response.SURVEY_ID + "=?", surveyID)
					.where(Tables.PROMPT_RESPONSES + "." + PromptResponse.PROMPT_ID + "=?", promptID);
				
			default:
				throw new UnsupportedOperationException("buildSelection(): Unknown URI: " + uri);
		}
	}
}