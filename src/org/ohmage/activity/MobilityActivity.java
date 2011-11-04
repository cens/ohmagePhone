package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.service.UploadService;
import org.ohmage.ui.BaseActivity;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.mobility.glue.MobilityInterface;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MobilityActivity extends BaseActivity {
	
	private ListView mMobilityList;
	private TextView mTotalCountText;
	private TextView mUploadCountText;
	private TextView mLastUploadText;
	private Button mUploadButton;
	private Button mSettingsButton;
	
	private SharedPreferencesHelper mPrefHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.mobility);
		
		mMobilityList = (ListView) findViewById(R.id.mobility_list);
		mTotalCountText = (TextView) findViewById(R.id.mobility_total);
		mUploadCountText = (TextView) findViewById(R.id.mobility_count);
		mLastUploadText = (TextView) findViewById(R.id.last_upload);
		mUploadButton = (Button) findViewById(R.id.upload_button);
		mSettingsButton = (Button) findViewById(R.id.settings_button);
		
		mPrefHelper = new SharedPreferencesHelper(this);
		
		getActionBar().addActionBarCommand(1, "refresh", R.drawable.dashboard_title_refresh);
		
		getActionBar().setOnActionListener(new ActionListener() {
			@Override
			public void onActionClicked(int commandID) {
				switch(commandID) {
					case 1:
						updateViews();
						break;
				}
			}
		});
		
		TextView emptyView = new TextView(this);
		emptyView.setText("No mobility points recorded in last 10 mins.");
		mMobilityList.setEmptyView(emptyView);
		
		mMobilityList.setEnabled(false);
		
		long timestamp = System.currentTimeMillis() - 5 * 60 * 1000;
		
		Cursor c = MobilityInterface.getMobilityCursor(this, timestamp);
		
		String [] from = new String[] {MobilityInterface.KEY_MODE, MobilityInterface.KEY_TIME};
		int [] to = new int[] {R.id.text1, R.id.text2};
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.mobility_list_item, c, from, to, 0);
		
		mMobilityList.setAdapter(adapter);
		
		updateViews();
		
		mUploadButton.setOnClickListener(mUploadListener);
		mSettingsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MobilityInterface.showMobilityOptions(MobilityActivity.this);
			}
		});
	}

	private void updateViews() {
		mTotalCountText.setText(String.valueOf(getMobilityCount()));
		
		Long lastMobilityUploadTimestamp = mPrefHelper.getLastMobilityUploadTimestamp();
		
		mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss", lastMobilityUploadTimestamp));
		mUploadCountText.setText(String.valueOf(getMobilityCount(lastMobilityUploadTimestamp)));
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

	private BroadcastReceiver mMobilityUploadReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (UploadService.MOBILITY_UPLOAD_STARTED.equals(action)) {
				MobilityActivity.this.getActionBar().setProgressVisible(true);
				MobilityActivity.this.mUploadButton.setEnabled(false);
			} else if (UploadService.MOBILITY_UPLOAD_FINISHED.equals(action)) {
				MobilityActivity.this.getActionBar().setProgressVisible(false);
				MobilityActivity.this.mUploadButton.setEnabled(true);
				MobilityActivity.this.updateViews();
			}
		}
	};

	private int getMobilityCount() {
		Cursor c = MobilityInterface.getMobilityCursor(this, new Long(0));
		if (c == null) {
			return 0;
		} else {
			int count = c.getCount();
			c.close();
			return count;
		}
	}
	
	private int getMobilityCount(Long timestamp) {
		
		Cursor c = MobilityInterface.getMobilityCursor(this, timestamp);
		if (c == null) {
			return 0;
		} else {
			int count = c.getCount();
			c.close();
			return count;
		}
	}
	
	private final OnClickListener mUploadListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(MobilityActivity.this, UploadService.class);
			intent.setData(Responses.CONTENT_URI);
			intent.putExtra("upload_mobility", true);
			WakefulIntentService.sendWakefulWork(MobilityActivity.this, intent);
		}
	};
}
