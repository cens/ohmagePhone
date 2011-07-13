package edu.ucla.cens.andwellness.feedback;

import edu.ucla.cens.andwellness.db.Response;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackResponses;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

public class SurveyCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// check if it's the SURVEY_COMPLETE intent
		if (intent.getAction().equals("edu.ucla.cens.andwellness.SURVEY_COMPLETE"))
		{
			// it is, so unpack it and insert it into our database
			FeedbackDatabase feedbackDB = new FeedbackDatabase(context);
			
			Bundle data = intent.getExtras();

			if (data.getString(Response.LOCATION_STATUS).equals(SurveyGeotagService.LOCATION_VALID))
			{
				feedbackDB.addResponseRow(
						data.getString(Response.CAMPAIGN_URN),
						data.getString(Response.USERNAME),
						data.getString(Response.DATE),
						data.getLong(Response.TIME),
						data.getString(Response.TIMEZONE),
						data.getString(Response.LOCATION_STATUS),
						data.getDouble(Response.LOCATION_LATITUDE),
						data.getDouble(Response.LOCATION_LONGITUDE),
						data.getString(Response.LOCATION_PROVIDER),
						data.getFloat(Response.LOCATION_ACCURACY),
						data.getLong(Response.LOCATION_TIME),
						data.getString(Response.SURVEY_ID),
						data.getString(Response.SURVEY_LAUNCH_CONTEXT),
						data.getString(Response.RESPONSE),
						"local");
			}
			else
			{
				feedbackDB.addResponseRow(
						data.getString(Response.CAMPAIGN_URN),
						data.getString(Response.USERNAME),
						data.getString(Response.DATE),
						data.getLong(Response.TIME),
						data.getString(Response.TIMEZONE),
						data.getString(Response.SURVEY_ID),
						data.getString(Response.SURVEY_LAUNCH_CONTEXT),
						data.getString(Response.RESPONSE),
						"local");
			}
			
			// let's attempt to use a content resolver, too
			ContentResolver cr = context.getContentResolver();
			
			@SuppressWarnings("unused")
			String[] columns = new String[]{"_id"};
			
			Cursor result = cr.query(FeedbackResponses.CONTENT_URI, null, null, null, null);

			Toast.makeText(context, "total saved responses: " + result.getCount(), Toast.LENGTH_SHORT).show();
			
			result.close();
		}
	}
}
