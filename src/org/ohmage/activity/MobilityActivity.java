package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.service.UploadService;
import org.ohmage.ui.BaseActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.mobility.glue.IMobility;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Log;

public class MobilityActivity extends BaseActivity implements LoaderCallbacks<Cursor> {
	
	private static final String TAG = "MobilityActivity";
	
	private static final int RECENT_LOADER = 0;
	private static final int ALL_LOADER = 1;
	private static final int UPLOAD_LOADER = 2;
	
	private ListView mMobilityList;
	private TextView mTotalCountText;
	private TextView mUploadCountText;
	private TextView mLastUploadText;
	private Button mUploadButton;
	private RadioButton mOffRadio;
	private RadioButton mInterval1Radio;
	private RadioButton mInterval5Radio;
	
	private SimpleCursorAdapter mAdapter;
	
	private SharedPreferencesHelper mPrefHelper;
	private IMobility mMobility = null;
	private boolean isBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		boolean isMobilityInstalled = false;
		
		try {
			ApplicationInfo info = getPackageManager().getApplicationInfo("edu.ucla.cens.mobility", 0 );
			ApplicationInfo info2 = getPackageManager().getApplicationInfo("edu.ucla.cens.accelservice", 0 );
			
			if (info != null && info2 != null) {
				isMobilityInstalled = true;
			}
		} catch( PackageManager.NameNotFoundException e ) {
			isMobilityInstalled = false;
		}
		    
		if (! isMobilityInstalled) {
			TextView view = new TextView(this);
			view.setText("Please make sure the Mobility and AccelService packages are installed.");
			view.setTextAppearance(this, android.R.attr.textAppearanceLarge);
			view.setGravity(Gravity.CENTER);
			setContentView(view);
		} else {
			setContentView(R.layout.mobility2);
			
			mMobilityList = (ListView) findViewById(R.id.mobility_list);
			mTotalCountText = (TextView) findViewById(R.id.mobility_total);
			mUploadCountText = (TextView) findViewById(R.id.mobility_count);
			mLastUploadText = (TextView) findViewById(R.id.last_upload);
			mUploadButton = (Button) findViewById(R.id.upload_button);
			mOffRadio = (RadioButton) findViewById(R.id.radio_off);
			mInterval1Radio = (RadioButton) findViewById(R.id.radio_1min);
			mInterval5Radio = (RadioButton) findViewById(R.id.radio_5min);
			
			mPrefHelper = new SharedPreferencesHelper(this);
			
			TextView emptyView = (TextView) findViewById(R.id.empty);
			mMobilityList.setEmptyView(emptyView);
			
//			mMobilityList.setEnabled(false);
			
			String [] from = new String[] {MobilityInterface.KEY_MODE, MobilityInterface.KEY_TIME};
			int [] to = new int[] {R.id.text1, R.id.text2};
			
			mAdapter = new SimpleCursorAdapter(this, R.layout.mobility_list_item, null, from, to, 0);
			
			mAdapter.setViewBinder( new android.support.v4.widget.SimpleCursorAdapter.ViewBinder() {
				
				@Override
				public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
					switch (view.getId()) {
					case R.id.text1:
						((TextView)view).setText(cursor.getString(columnIndex));
						return true;
					
					case R.id.text2:
						long time = cursor.getLong(columnIndex);
						((TextView)view).setText(DateFormat.format("h:mmaa", time));
						return true;
					}
					return false;
				}
			});
			
			mMobilityList.setAdapter(mAdapter);
			
			mTotalCountText.setText("-");
			mUploadCountText.setText("-");
			long lastMobilityUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
			if (lastMobilityUploadTimestamp == 0) {
				mLastUploadText.setText("-");
			} else {
				mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss", lastMobilityUploadTimestamp));
			}
			
			getSupportLoaderManager().initLoader(RECENT_LOADER, null, this);
			getSupportLoaderManager().initLoader(ALL_LOADER, null, this);
			getSupportLoaderManager().initLoader(UPLOAD_LOADER, null, this);
			
			mOffRadio.setChecked(true);
			
			mUploadButton.setOnClickListener(mUploadListener);
			
			bindService(new Intent(IMobility.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
			isBound = true;
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(mMobilityUploadReceiver, new IntentFilter(UploadService.MOBILITY_UPLOAD_STARTED));
		registerReceiver(mMobilityUploadReceiver, new IntentFilter(UploadService.MOBILITY_UPLOAD_FINISHED));
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mMobilityUploadReceiver);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (isBound) {
			unbindService(mConnection);
		}
	}
	
	private BroadcastReceiver mMobilityUploadReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (UploadService.MOBILITY_UPLOAD_STARTED.equals(action)) {
				MobilityActivity.this.getActionBar().setProgressVisible(true);
				MobilityActivity.this.mUploadButton.setEnabled(false);
				MobilityActivity.this.mUploadButton.setText("Uploading...");
			} else if (UploadService.MOBILITY_UPLOAD_FINISHED.equals(action)) {
				MobilityActivity.this.getActionBar().setProgressVisible(false);
				MobilityActivity.this.mUploadButton.setEnabled(true);
				MobilityActivity.this.mUploadButton.setText("Upload Now");
				Long lastMobilityUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
				mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss", lastMobilityUploadTimestamp));
				getSupportLoaderManager().restartLoader(UPLOAD_LOADER, null, MobilityActivity.this);
			}
		}
	};
	
	private final OnClickListener mUploadListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(MobilityActivity.this, UploadService.class);
			intent.setData(Responses.CONTENT_URI);
			intent.putExtra(UploadService.EXTRA_UPLOAD_MOBILITY, true);
			WakefulIntentService.sendWakefulWork(MobilityActivity.this, intent);
		}
	};
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			
			mMobility = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			
			mMobility = IMobility.Stub.asInterface(service);
			
			int interval = -1;
			boolean isMobilityOn = false;
			
			try {
				isMobilityOn = mMobility.isMobilityOn();
				interval = mMobility.getMobilityInterval();
			} catch (RemoteException e) {
				Log.e(TAG, "Unable to read mobility state", e);
			}
			
			if (! isMobilityOn) {
				mOffRadio.setChecked(true);
			} else if (interval == 60) {
				mInterval1Radio.setChecked(true);
			} else if (interval == 300) {
				mInterval5Radio.setChecked(true);
			}
			
			mOffRadio.setOnClickListener(mRadioListener);			
			mInterval1Radio.setOnClickListener(mRadioListener);
			mInterval5Radio.setOnClickListener(mRadioListener);
		}
		
		OnClickListener mRadioListener = new OnClickListener() {
			
			@Override
			public void onClick(final View v) {
				// do the update in an asynctask so the UI doesn't block
				(new AsyncTask<Void, Void, Void>() {
					protected void onPreExecute() {
						mOffRadio.setEnabled(false);
						mInterval1Radio.setEnabled(false);
						mInterval5Radio.setEnabled(false);
					};
					
					@Override
					protected Void doInBackground(Void... params) {
						int newInterval = -1;
						int oldInterval = -1;
						
						switch (v.getId()) {
						case R.id.radio_off:
							try {
								mMobility.stopMobility();
							} catch (RemoteException e) {
								Log.e(TAG, "Unable to stop mobility", e);
							}
							return null;
							
						case R.id.radio_1min:
							newInterval = 60;
							break;

						case R.id.radio_5min:
							newInterval = 300;
							break;
						}
						
						try {
							oldInterval = mMobility.getMobilityInterval();
							if (newInterval != oldInterval) {
								mMobility.changeMobilityRate(newInterval);
							}
							
							if (! mMobility.isMobilityOn()) {
								mMobility.startMobility();
							}
						} catch (RemoteException e) {
							Log.e(TAG, "Unable to change mobility interval", e);
						}
						
						return null;
					};
					
					protected void onPostExecute(Void result) {
						mOffRadio.setEnabled(true);
						mInterval1Radio.setEnabled(true);
						mInterval5Radio.setEnabled(true);
					};
				}).execute();
			}
		};
	};
	
	public interface MobilityQuery {
		String[] PROJECTION = { 
				MobilityInterface.KEY_ROWID, 
				MobilityInterface.KEY_MODE, 
				MobilityInterface.KEY_TIME
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		long loginTimestamp = mPrefHelper.getLoginTimestamp();

		switch (id) {
		case RECENT_LOADER:
			return new CursorLoader(this, MobilityInterface.CONTENT_URI, MobilityQuery.PROJECTION, MobilityInterface.KEY_TIME + " > strftime('%s','now','-20 minutes') || '500' AND " + MobilityInterface.KEY_TIME + " > ?", new String[] {String.valueOf(loginTimestamp)}, MobilityInterface.KEY_TIME + " DESC");

		case ALL_LOADER:
			return new CursorLoader(this, MobilityInterface.CONTENT_URI, MobilityQuery.PROJECTION, MobilityInterface.KEY_TIME + " > ?", new String[] {String.valueOf(loginTimestamp)}, null);
			
		case UPLOAD_LOADER:
			long uploadAfterTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
			if (uploadAfterTimestamp == 0) {
				uploadAfterTimestamp = loginTimestamp;
			}
			return new CursorLoader(this, MobilityInterface.CONTENT_URI, MobilityQuery.PROJECTION, MobilityInterface.KEY_TIME + " > ?", new String[] {String.valueOf(uploadAfterTimestamp)}, null);
			
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		
		switch (loader.getId()) {
		case RECENT_LOADER:
			mAdapter.swapCursor(data);
			break;

		case ALL_LOADER:
			mTotalCountText.setText(String.valueOf(data.getCount()));
			break;
			
		case UPLOAD_LOADER:
			mUploadCountText.setText(String.valueOf(data.getCount()));
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
		switch (loader.getId()) {
		case RECENT_LOADER:
			mAdapter.swapCursor(null);
			break;

		case ALL_LOADER:
			mTotalCountText.setText("-");
			break;
			
		case UPLOAD_LOADER:
			mUploadCountText.setText("-");
			break;
		}
	}
}
