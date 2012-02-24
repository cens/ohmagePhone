package org.ohmage.service;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * A wakeful intent service based on {@link WakefulIntentService} but which
 * allows more flexiblity related to when the lock is release.
 * 
 * @see {@link WakefulService#releaseLock() }
 * @author cketcham
 */
public abstract class WakefulService extends Service {

	abstract protected void doWakefulWork(Intent intent);

	static final String NAME="org.ohmage.service.OhmageWakefulIntentService";

	private static volatile PowerManager.WakeLock lockStatic=null;

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);

			lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					NAME);
			lockStatic.setReferenceCounted(true);
		}

		return(lockStatic);
	}

	public static void sendWakefulWork(Context ctxt, Intent i) {
		getLock(ctxt.getApplicationContext()).acquire();
		ctxt.startService(i);
	}

	public static void sendWakefulWork(Context ctxt, Class<?> clsService) {
		sendWakefulWork(ctxt, new Intent(ctxt, clsService));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ((flags & START_FLAG_REDELIVERY)!=0) { // if crash restart...
			getLock(this.getApplicationContext()).acquire();  // ...then quick grab the lock
		}
		super.onStartCommand(intent, flags, startId);

		doWakefulWork(intent);

		return(START_REDELIVER_INTENT);
	}

	/**
	 * Should be called in a try catch block before {@link Service#stopSelf}
	 * 
	 * <pre>
	 * try {
	 * 	...code here
	 * } finally {
	 * 	releaseLock();
	 * 	stopSelf();
	 * }
	 * </pre>
	 */
	protected void releaseLock() {
		getLock(this.getApplicationContext()).release();
	}
}