package edu.ucla.cens.andwellness.feedback;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FeedbackService extends WakefulIntentService {

	public FeedbackService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// our task is to determine what's left in the cache
		// and repopulate it if need be...
	}

}
