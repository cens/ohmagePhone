package edu.ucla.cens.andwellness.appwidget;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.R.id;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.activity.SurveyActivity;
import edu.ucla.cens.andwellness.activity.SurveyListActivity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class AndWellnessAppWidgetProvider extends AppWidgetProvider {

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.andwellness_appwidget);
            
            // Create an Intent to launch SurveyListActivity
            Intent awIntent = new Intent(context, SurveyListActivity.class);
            PendingIntent awPendingIntent = PendingIntent.getActivity(context, 0, awIntent, 0);
            // Get the layout for the App Widget and attach an on-click listener to the button
            views.setOnClickPendingIntent(R.id.widget_aw_button, awPendingIntent);
            
            // Create an Intent to launch foodButton survey
            Intent foodIntent = new Intent(context, SurveyActivity.class);
            foodIntent.putExtra("survey_id", "foodButton");
    		foodIntent.putExtra("survey_title", "Food");
    		foodIntent.setAction("edu.ucla.cens.andwellnessphone.buttons.foodButton");
            PendingIntent foodPendingIntent = PendingIntent.getActivity(context, 0, foodIntent, 0);
            // Get the layout for the App Widget and attach an on-click listener to the button
            views.setOnClickPendingIntent(R.id.widget_food_button, foodPendingIntent);
            
            // Create an Intent to launch stressButton survey
            Intent stressIntent = new Intent(context, StressButtonService.class);
            PendingIntent stressPendingIntent = PendingIntent.getService(context, 0, stressIntent, 0);
            /*Intent stressIntent = new Intent(context, SurveyActivity.class);
            stressIntent.putExtra("survey_id", "stressButton");
    		stressIntent.putExtra("survey_title", "Stress");
    		stressIntent.setAction("edu.ucla.cens.andwellnessphone.buttons.stressButton");
            PendingIntent stressPendingIntent = PendingIntent.getActivity(context, 0, stressIntent, 0);*/
            // Get the layout for the App Widget and attach an on-click listener to the button
            views.setOnClickPendingIntent(R.id.widget_stress_button, stressPendingIntent);

            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
