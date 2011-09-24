package org.ohmage.activity;

import org.ohmage.db.DbContract.Survey;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.triggers.glue.TriggerFramework;
import org.ohmage.triggers.notif.Notifier;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class TriggerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(TriggerFramework.ACTION_ACTIVE_SURVEY_LIST_CHANGED)) {
			
			String campaignUrn = intent.getStringExtra(Notifier.KEY_CAMPAIGN_URN);
			
			String [] surveyTitles = TriggerFramework.getActiveSurveys(context, campaignUrn);
			
			ContentResolver cr = context.getContentResolver();
			
			String where = Tables.SURVEYS + "." + Survey.CAMPAIGN_URN + " = '" + campaignUrn + "'";
			
			ContentValues cv = new ContentValues();
			cv.put(Survey.STATUS, Survey.STATUS_NORMAL);
			
			cr.update(Survey.CONTENT_URI, cv, where, null);
			
			cv.remove(Survey.STATUS);
			cv.put(Survey.STATUS, Survey.STATUS_TRIGGERED);
			
			where = Tables.SURVEYS + "." + Survey.CAMPAIGN_URN + " = '" + campaignUrn + "' AND " + Tables.SURVEYS + "." + Survey.TITLE + " = ?";
			
			for (String surveyTitle : surveyTitles) {
				
				cr.update(Survey.CONTENT_URI, cv, where, new String [] {surveyTitle});
			}			
		}
	}

}
