package edu.ucla.cens.mobility;

import edu.ucla.cens.accelservice.IAccelService;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class Mobility
{
	
	static Location globalLoc;
	static boolean setInterval = false;
	private static PendingIntent startPI = null;
	
//	private static PendingIntent stopPI = null;
	static NotificationManager nm;
	static Notification notification;
	public static final String TAG = "Mobility";
	public static final String SERVICE_TAG = "Mobility";
	public static final String MOBILITY = "mobility";
	public static final String SAMPLE_RATE = "sample rate";
	public static final String ACC_START = "edu.ucla.cens.mobility.record";
	
	static AlarmManager mgr;
	static long sampleRate;
	
	public static boolean mConnected = false;
	public static ServiceConnection AccelServiceConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			setmAccel(IAccelService.Stub.asInterface(service));
			mConnected = true;
			try
			{
				getmAccel().start(SERVICE_TAG);
				getmAccel().suggestRate(SERVICE_TAG, SensorManager.SENSOR_DELAY_GAME);
				getmAccel().suggestInterval(SERVICE_TAG, (int) sampleRate);
				
				Log.d(TAG, "START WAS CALLED ON ACCEL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			} catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG, "Connected to accel service");
		}

		public void onServiceDisconnected(ComponentName className)
		{
			Log.d(TAG, "onServiceDisconnected was called!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			try
			{
				getmAccel().stop(SERVICE_TAG);
				Log.i(TAG, "Successfully stopped service!");
			} catch (RemoteException e)
			{
				Log.e(TAG, "Failed to stop service!");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setmAccel(null);
			mConnected = false;
		}
	};
	private static IAccelService mAccel;
	
	public static void start(Context context)
	{
		nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.mobility, null, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_NO_CLEAR;
//		bindService(new Intent(ISystemLog.class.getName()), Logg.SystemLogConnection, Context.BIND_AUTO_CREATE);

		PendingIntent pi = PendingIntent.getActivity(context.getApplicationContext(), 1, new Intent(), 1);
		notification.setLatestEventInfo(context.getApplicationContext(), "Mobility", "Service Running", pi);
		nm.notify(123, notification);

		SharedPreferences settings = context.getSharedPreferences(MOBILITY, Context.MODE_PRIVATE);
		sampleRate = (long) settings.getInt(SAMPLE_RATE, 60) * 1000;

		mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

//		context.registerReceiver(accReceiver, new IntentFilter(ACC_START));
		startGPS(context, sampleRate);
		startAcc(context, sampleRate);
		Toast.makeText(context, R.string.mobilityservicestarted, Toast.LENGTH_SHORT).show();
		Log.d(TAG, "Starting transport mode service with sampleRate: " + sampleRate);
		GarbageCollectReceiver.scheduleGC(context, mgr);
		
	}
	
	

	public static void stop(Context context)
	{
		Log.i(TAG, "Stopping mobility");
		if (mgr != null)
		{
			Intent i = new Intent(ACC_START);
			startPI = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
			mgr.cancel(startPI);
		}
		stopAcc(context, sampleRate);
		stopGPS(context);
//		try
//		{
//			context.unregisterReceiver(accReceiver);
//		} catch (Exception e1)
//		{
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		// unregisterReceiver(stopAccReceiver);
		// lManager.removeUpdates(lListener);
		try
		{
			if (getmWiFiGPS() != null)
				getmWiFiGPS().stop(SERVICE_TAG);
			try
			{
				context.unbindService(mConnection);
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			
//			unbindService(Logg.SystemLogConnection);
		} catch (Exception e)
		{
			// If it's not running then we don't care if this can't be unbound.
			// Why does it want to crash?
			e.printStackTrace();
		}
		if (nm != null)
			nm.cancel(123);
	}
	
	private static void startAcc(Context context, long milliseconds)
	{
		Intent i = new Intent(ACC_START);
		startPI = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		mgr.setRepeating(AlarmManager.RTC_WAKEUP, 0, milliseconds, startPI);
		context.bindService(new Intent(IAccelService.class.getName()), AccelServiceConnection, Context.BIND_AUTO_CREATE);

	}

	private static void stopAcc(Context context, long milliseconds)
	{
		if (startPI != null)
			mgr.cancel(startPI);
//		if (stopPI != null)
//			mgr.cancel(stopPI);
		try
		{
			getmAccel().stop(SERVICE_TAG);
			context.unbindService(AccelServiceConnection);
			Log.i(TAG, "Successfully unbound accel service");
		} 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void startGPS(Context context, long milliseconds)
	{
		Log.d(TAG, String.format("Sampling GPS at %d.", milliseconds / 1000));
		context.bindService(new Intent(edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
		
	}

	private static void stopGPS(Context context)
	{
		try
		{
			mWiFiGPS.stop(SERVICE_TAG);
			context.unbindService(mConnection);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}

	}
	public static void setmWiFiGPS(IWiFiGPSLocationService mWiFiGPS)
	{
		Mobility.mWiFiGPS = mWiFiGPS;
	}

	public static IWiFiGPSLocationService getmWiFiGPS()
	{
		return mWiFiGPS;
	}
	public static void setmAccel(IAccelService mAccel)
	{
		Mobility.mAccel = mAccel;
	}

	public static IAccelService getmAccel()
	{
		return mAccel;
	}
	private static IWiFiGPSLocationService mWiFiGPS;
	private static ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			setmWiFiGPS(IWiFiGPSLocationService.Stub.asInterface(service));
			Log.i(TAG, "Connected to WiFiGPSLocation Service");
			try
			{
				mWiFiGPS.start(SERVICE_TAG);
			} catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// As part of the sample, tell the user what happened.
			Log.i(TAG, "Connected");
			
		}

		public void onServiceDisconnected(ComponentName className)
		{
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			setmWiFiGPS(null);

			Log.i(TAG, "Disconnected from WiFiGPSLocation Service");
			
			// As part of the sample, tell the user what happened.

		}
	};
	
	public static void unbindServices(Context context)
	{
		try
		{
			context.unbindService(mConnection);
			context.unbindService(AccelServiceConnection);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}
	
//	static BroadcastReceiver accReceiver = new BroadcastReceiver() // put this sucker in its own file and register in manifest
//	{
//
//		@Override
//		public void onReceive(Context context, Intent intent)
//		{
//			Log.e(TAG, "Start mobility service");
//			// Old way
////			context.startService(new Intent(context, ClassifierService.class));
//			// wakeful intent service
//			WakefulIntentService.sendWakefulWork(context, ClassifierService.class);
//		}
//	};
//	static BroadcastReceiver GCReceiver = new BroadcastReceiver() // this one too
//	{
//
//		@Override
//		public void onReceive(Context context, Intent intent)
//		{
//			Log.d(TAG, "Collect garbage");
//			// Need context!
//			MobilityDbAdapter mda = new MobilityDbAdapter(context, "mobility", "mobility", "mobility");
//			mda.open();
//			mda.deleteSomeRows(System.currentTimeMillis() - gctime * 24 * 3600 * 1000);
//			mda.close();
//		}
//	};
}
