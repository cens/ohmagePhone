/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.triggers.ui;

import org.ohmage.Config;
import org.ohmage.R;
import org.ohmage.activity.AdminPincodeActivity;
import org.ohmage.triggers.base.TriggerActionDesc;
import org.ohmage.triggers.base.TriggerBase;
import org.ohmage.triggers.base.TriggerDB;
import org.ohmage.triggers.base.TriggerTypeMap;
import org.ohmage.triggers.config.TrigUserConfig;
import org.ohmage.triggers.notif.NotifDesc;
import org.ohmage.triggers.notif.NotifEditActivity;
import org.ohmage.triggers.notif.NotifSettingsActivity;
import org.ohmage.triggers.notif.Notifier;
import org.ohmage.triggers.utils.TrigPrefManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class TriggerListActivity extends ListActivity 
			implements OnClickListener {
	
	private static final String DEBUG_TAG = "TriggerFramework";
		
	private static final String PREF_FILE_NAME = 
		TriggerListActivity.class.getName();
	
	public static final String KEY_CAMPAIGN_URN = 
		TriggerListActivity.class.getName() + ".campain_urn";
	public static final String KEY_ACTIONS = 
		TriggerListActivity.class.getName() + ".actions";
	// pass survey names through here to preselect them in the trigger creation page
	public static final String KEY_PRESELECTED_ACTIONS = 
			TriggerListActivity.class.getName() + ".preselected_actions";
	public static final String KEY_ADMIN_MODE = 
		TriggerListActivity.class.getName() + ".admin_mode";
	private static final String KEY_SAVE_DIALOG_TRIG_ID = 
		TriggerListActivity.class.getName() + ".dialog_trig_id";
	private static final String KEY_SAVE_SEL_ACTIONS = 
		TriggerListActivity.class.getName() + ".selected_actions";
	private static final String KEY_SAVE_DIALOG_TEXT = 
		TriggerListActivity.class.getName() + ".dialog_text";
	
	private static final int MENU_ID_DELETE_TRIGGER = Menu.FIRST;
	private static final int MENU_ID_NOTIF_SETTINGS = Menu.FIRST + 1;
	private static final int MENU_ID_SETTINGS = Menu.FIRST + 2;
	private static final int MENU_ID_ADMIN_LOGIN = Menu.FIRST + 3;
	private static final int MENU_ID_ADMIN_LOGOFF = Menu.FIRST + 4;
	private static final int MENU_ID_RINGTONE_SETTINGS = Menu.FIRST + 5;
	
	private static final int DIALOG_ID_ADD_NEW = 0;
	private static final int DIALOG_ID_PREFERENCES = 1;
	private static final int DIALOG_ID_ACTION_SEL = 2;
	private static final int DIALOG_ID_DELETE = 3;
	
	private static final int REQ_EDIT_NOTIF = 0;
	private static final int ADMIN_REQUESTED = 1;
	
	private Cursor mCursor;
	private TriggerDB mDb;
	private TriggerTypeMap mTrigMap;
	private String[] mActions;
	private String[] mPreselectedActions; // actions which will be preselected in a new trigger window
	private String mCampaignUrn;
	private int mDialogTrigId = -1;
	private String mDialogText = null; 
	private boolean[] mActSelected = null;

	/**
	 * Instead of having a shared preference admin mode, we want admin mode to end once the user leaves the activity
	 */
	private boolean mAdminMode = TRIGGER_ADMIN_MODE;

	/**
	 * Set the default admin mode. If it is true, we don't need to show the admin menu
	 */
	public static boolean TRIGGER_ADMIN_MODE = Config.ADMIN_MODE;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.trigger_main_list);
			
		mTrigMap = new TriggerTypeMap();
		
		TextView tv = (TextView) findViewById(R.id.add_new_label);
		tv.setText(R.string.triggers_title);
		
		ImageButton bAdd = (ImageButton) findViewById(R.id.button_add_new);
		bAdd.setOnClickListener(this);

		getListView().setHeaderDividersEnabled(true);
		Intent i = getIntent();
		if(i.hasExtra(KEY_ACTIONS)) {
			mActions = i.getStringArrayExtra(KEY_ACTIONS);
		}
		else {
			Log.e(DEBUG_TAG, "TriggerListActivity: Invoked with out passing surveys");
			finish();
			return;
		}
		
		// gather any preselected actions that were specified
		// we'll feed these to the trigger create activity later to preselect certain actions
		if (i.hasExtra(KEY_PRESELECTED_ACTIONS)) {
			mPreselectedActions = i.getStringArrayExtra(KEY_PRESELECTED_ACTIONS);
		}
		
		if(i.hasExtra(KEY_CAMPAIGN_URN)) {
			mCampaignUrn = i.getStringExtra(KEY_CAMPAIGN_URN);
		}
		else {
			Log.e(DEBUG_TAG, "TriggerListActivity: Invoked with out passing campaign urn");
			finish();
			return;
		}
		
		mDb = new TriggerDB(this);
        mDb.open();
        
		populateTriggerList();
		registerForContextMenu(getListView());
		
		updateGUIWithAdminStatus(isAdminLoggedIn());
		
		//Display message and exit if there are no supported 
		//trigger types
		if(mTrigMap.getAllTriggers().size() == 0) {
			Toast.makeText(this, R.string.trigger_nothing_supported,
					Toast.LENGTH_SHORT).show();
			
			finish();
		}
		
		TrigPrefManager.registerPreferenceFile(this, mCampaignUrn, PREF_FILE_NAME);
		TrigPrefManager.registerPreferenceFile(this, "GLOBAL", PREF_FILE_NAME);
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mCursor.close();
		mDb.close();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(KEY_SAVE_DIALOG_TRIG_ID, mDialogTrigId);
		outState.putBooleanArray(KEY_SAVE_SEL_ACTIONS, mActSelected);
		outState.putString(KEY_SAVE_DIALOG_TEXT, mDialogText);
		outState.putBoolean(KEY_ADMIN_MODE, mAdminMode);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		
		mDialogTrigId = state.getInt(KEY_SAVE_DIALOG_TRIG_ID, -1);
		mActSelected = state.getBooleanArray(KEY_SAVE_SEL_ACTIONS);
		mDialogText = state.getString(KEY_SAVE_DIALOG_TEXT);
		mAdminMode = state.getBoolean(KEY_ADMIN_MODE);
	}
	
	private void updateGUIWithAdminStatus(boolean status) {
		ImageButton bAdd = (ImageButton) findViewById(R.id.button_add_new);
		bAdd.setEnabled(isAdminLoggedIn() || TrigUserConfig.addTrigers);
	}
	
	private String getDisplayTitle(String trigType, String trigDesc) {
		
		TriggerBase trig = mTrigMap.getTrigger(trigType);
		
		if(trig == null) {
			return null;
		}
		
		return trig.getDisplayTitle(this, trigDesc);
	}
	
	private int getTrigTypeIcon(String trigType) {
		
		TriggerBase trig = mTrigMap.getTrigger(trigType);
		
		if(trig == null) {
			return R.drawable.icon;
		}
		
		return trig.getIcon();
	}

	private String getDisplaySummary(String trigType, String trigDesc) {
		
		TriggerBase trig = mTrigMap.getTrigger(trigType);
		
		if(trig == null) {
			return null;
		}
		
		return trig.getDisplaySummary(this, trigDesc);
	}

	
	private void toggleTrigger(int trigId, boolean enable) {
		Cursor c = mDb.getTrigger(trigId);
		
		if(c.moveToFirst()) {
			String trigType = c.getString(
							  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
			String trigDesc = c.getString(
					  		  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
			
			TriggerBase trig = mTrigMap.getTrigger(trigType);
			
			if(trig != null) {
				
				if(enable) {
					trig.startTrigger(this, trigId, trigDesc);
				}
				else {
					trig.stopTrigger(this, trigId, trigDesc);
				}
			}
		}
		
		c.close();
	}

	private void editTrigger(String trigType, int trigId, String trigDesc, String actDesc) {
		
		TriggerBase trig = mTrigMap.getTrigger(trigType);
		
		if(trig != null) {
			trig.launchTriggerEditActivity(this,trigId, trigDesc, actDesc, mActions, isAdminLoggedIn());
		}
	}
	
	private void deleteTrigger(int trigId) {
		
		TriggerBase trig = mTrigMap.getTrigger(
								mDb.getTriggerType(trigId));
		
		trig.deleteTrigger(this, trigId);
	}

	private void populateTriggerList() {
		
		//The viewbinder class to define each list item
        class CategListViewBinder 
			  implements SimpleCursorAdapter.ViewBinder {

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
			
				String trigType = c.getString(
							   	  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
				
				String trigDesc = c.getString(
					   	  		  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
		
				switch(view.getId()) {
			
				case R.id.text1:
					String title = getDisplayTitle(trigType, trigDesc);
					((TextView) view).setText(title == null ? "" : title);
					return true;
					
				case R.id.text2:
					String summary = getDisplaySummary(trigType, trigDesc);
					((TextView) view).setText(summary == null ? "" : summary);
					return true;
					
				case  R.id.button_actions_edit: //edit surveys button
					int trigId = c.getInt(
							 	 c.getColumnIndexOrThrow(TriggerDB.KEY_ID));
					
					String actDesc = c.getString(
				   	  		  		 c.getColumnIndexOrThrow(
				   	  		  		 TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
				
					Button bAct = (Button) view;
					bAct.setFocusable(false);
					
					TriggerActionDesc desc = new TriggerActionDesc();
					desc.loadString(actDesc);
					bAct.setText("(" + desc.getCount() + ")");
					
					bAct.setTag(new Integer(trigId));
					
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							mDialogTrigId = (Integer) v.getTag();
							mActSelected = null;
							
							removeDialog(DIALOG_ID_ACTION_SEL);
							showDialog(DIALOG_ID_ACTION_SEL);
						}
					});
					return true;
					
				case R.id.icon_trigger_type:
					ImageView iv = (ImageView) view;
					iv.setImageResource(getTrigTypeIcon(trigType));
					return true;
				}
			
				return false;
			}
        }
        
        mCursor = mDb.getAllTriggers(mCampaignUrn);
        
    	mCursor.moveToFirst();
    	startManagingCursor(mCursor);
    	
    	String[] from = new String[] {TriggerDB.KEY_ID, 
    			TriggerDB.KEY_ID, TriggerDB.KEY_ID, TriggerDB.KEY_ID};
    	
    	int[] to = new int[] {R.id.text1, R.id.text2, 
    						  R.id.button_actions_edit, R.id.icon_trigger_type};
    	
    	SimpleCursorAdapter triggers = 
    		new SimpleCursorAdapter(this, R.layout.trigger_main_list_row, 
    		mCursor, from, to);
    	
    	
    	triggers.setViewBinder(new CategListViewBinder());
    	setListAdapter(triggers);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
        
		if(!mCursor.moveToPosition(position)) {
			//TODO this should not happen. log
			return;
		}
	
		String trigDesc = mCursor.getString(
						  mCursor.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
		int trigId = mCursor.getInt(
				  	 mCursor.getColumnIndexOrThrow(TriggerDB.KEY_ID));
		
		String trigType = mCursor.getString(
				  		  mCursor.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
		
		String actDesc = mCursor.getString(mCursor.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
		
		editTrigger(trigType, trigId, trigDesc, actDesc);
    }
	
	private boolean isAdminLoggedIn() {
		return mAdminMode;
	}
	
	private void setAdminMode(boolean enable) {
		if(mAdminMode != enable) {
			mAdminMode  = enable;
			updateGUIWithAdminStatus(enable);
		}
	}

	private Dialog createDeleteConfirmDialog(int trigId) {
		
		return new AlertDialog.Builder(this)
					.setNegativeButton(R.string.cancel, null)
					.setTitle(R.string.trigger_delete_title)
					.setMessage(R.string.trigger_delete_text)
					.setPositiveButton(R.string.delete,
								new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteTrigger(mDialogTrigId);
				    		mCursor.requery();
						}
					})
					.create();
					
					
					
	}
	
	private Dialog createEditActionDialog(int trigId) {
		
		if(mActSelected == null) {
			String actDesc = mDb.getActionDescription(trigId);
			
			TriggerActionDesc desc = new TriggerActionDesc();
			desc.loadString(actDesc);
			
			mActSelected = new boolean[mActions.length];
			for(int i = 0; i < mActSelected.length; i++) {
				mActSelected[i] = desc.hasSurvey(mActions[i]);
			}
		}
		
		AlertDialog.Builder builder = 
	 			new AlertDialog.Builder(this)
			   .setTitle(R.string.trigger_select_actions)
			   .setNegativeButton(R.string.cancel, null)
			   .setView(new ActionSelectorView(getBaseContext(), mActions, mActSelected));
		
		/*
		AlertDialog.Builder builder = 
	 			new AlertDialog.Builder(this)
			   .setTitle(R.string.trigger_select_actions)
			   .setNegativeButton(R.string.cancel, null)
			   .setMultiChoiceItems(mActions, mActSelected, 
					   new DialogInterface.OnMultiChoiceClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which, 
										boolean isChecked) {
					
					mActSelected[which] = isChecked;
				}
			});
		*/

		if(isAdminLoggedIn() || TrigUserConfig.editTriggerActions) {
			 builder.setPositiveButton(R.string.done,
					 new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					TriggerActionDesc desc = new TriggerActionDesc();
					
					for(int i = 0; i < mActSelected.length; i++) {
						if(mActSelected[i]) {
							desc.addSurvey(mActions[i]);
						}
					}
					dialog.dismiss();
					handleActionSelection(mDialogTrigId, desc);
				}
			});
		}

	
		return builder.create();
	}
	
	private Dialog createAddNewSelDialog() {
		TriggerTypeSelector typeSel = new TriggerTypeSelector(this);
		typeSel.setTitle(R.string.trigger_create);
		typeSel.setOnClickListener(new TriggerTypeSelector.OnClickListener() {
			
			@Override
			public void onClick(String trigType) {
				mTrigMap.getTrigger(trigType)
						.launchTriggerCreateActivity(TriggerListActivity.this, mCampaignUrn, mActions, mPreselectedActions,
													 isAdminLoggedIn());
			}
		});
		
		return typeSel;
	}
	
	private Dialog createEditPrefSelDialog() {
		TriggerTypeSelector typeSel = new TriggerTypeSelector(this);
		typeSel.setTitle(R.string.trigger_preferences);
		typeSel.setOnClickListener(new TriggerTypeSelector.OnClickListener() {
			
			@Override
			public void onClick(String trigType) {
				mTrigMap.getTrigger(trigType)
						.launchSettingsEditActivity(TriggerListActivity.this, 
													isAdminLoggedIn());
			}
		});
		
		typeSel.setOnListItemChangeListener(
				new TriggerTypeSelector.OnListItemChangeListener() {
			
			@Override
			public boolean onAddItem(String trigType) {
				return mTrigMap.getTrigger(trigType).hasSettings();
			}
		});
		
		return typeSel;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_ADD_NEW: 
			return createAddNewSelDialog();
		
		case DIALOG_ID_PREFERENCES: 
			return createEditPrefSelDialog();
			
		case DIALOG_ID_ACTION_SEL:
			return createEditActionDialog(mDialogTrigId);
			
		case DIALOG_ID_DELETE:
			return createDeleteConfirmDialog(mDialogTrigId);
		}
		
		return null;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean ret = super.onPrepareOptionsMenu(menu);	
		 
		menu.removeItem(MENU_ID_ADMIN_LOGOFF);
		menu.removeItem(MENU_ID_ADMIN_LOGIN);
		menu.removeItem(MENU_ID_NOTIF_SETTINGS);
		menu.removeItem(MENU_ID_RINGTONE_SETTINGS);
		menu.removeItem(MENU_ID_SETTINGS);
		
		boolean adminMode = isAdminLoggedIn();
		if(!TRIGGER_ADMIN_MODE) {
			if(!adminMode) {
				menu.add(0, MENU_ID_ADMIN_LOGIN, 0, R.string.trigger_menu_admin_turn_on)
				.setIcon(R.drawable.ic_menu_login);
			}
			else {
				menu.add(0, MENU_ID_ADMIN_LOGOFF, 0, R.string.trigger_menu_admin_turn_off)
				.setIcon(R.drawable.ic_menu_login);
			}
		}
		
		//Add 'preferences' menu item only if there is at least
		//one trigger type registered which has settings
		for(TriggerBase trig : mTrigMap.getAllTriggers()) {
			
			if(trig.hasSettings()) {
				menu.add(0, MENU_ID_SETTINGS, 0, R.string.trigger_menu_preferences)
					.setIcon(R.drawable.ic_menu_preferences)
					.setEnabled(adminMode || TrigUserConfig.editTriggerSettings);
				
				break;
			}
		}
	    
		menu.add(0, MENU_ID_NOTIF_SETTINGS, 0, R.string.trigger_menu_notifications)
			.setIcon(R.drawable.ic_menu_notification)
			.setEnabled(adminMode || TrigUserConfig.editNotificationSettings);
		
		menu.add(0, MENU_ID_RINGTONE_SETTINGS, 0, R.string.trigger_menu_ringtone)
		.setIcon(R.drawable.ic_menu_ringtone)
		.setEnabled(true);
	    
     	return ret;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    	
		switch(item.getItemId()) {
	    		
	    	case MENU_ID_NOTIF_SETTINGS:
	    		
	    		Intent i = new Intent(this, NotifEditActivity.class);
	    		i.putExtra(NotifEditActivity.KEY_NOTIF_CONFIG, 
	    				   NotifDesc.getGlobalDesc(this));
	    		startActivityForResult(i, REQ_EDIT_NOTIF);
	    		return true;
	    		
	    	case MENU_ID_RINGTONE_SETTINGS:
	    		startActivity(new Intent(TriggerListActivity.this, NotifSettingsActivity.class));
	    		return true;
	    		
	    	case MENU_ID_SETTINGS:
	    		
	    		showDialog(DIALOG_ID_PREFERENCES);
	    		return true;
	    	
	    	case MENU_ID_ADMIN_LOGIN:
	    		
	    		mDialogText = null;
				startActivityForResult(new Intent(this, AdminPincodeActivity.class), ADMIN_REQUESTED);
	    		return true;
	    		
	    	case MENU_ID_ADMIN_LOGOFF:
	    		
	    		setAdminMode(false);
				Toast.makeText(this, R.string.trigger_admin_logged_off, Toast.LENGTH_SHORT)
	    			 .show();
	    		return true;
	    }
	    	
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch(requestCode) {
			case ADMIN_REQUESTED:
				setAdminMode(resultCode == RESULT_OK);
			case REQ_EDIT_NOTIF:
				if(data != null) {
					String desc = data.getStringExtra(NotifEditActivity.KEY_NOTIF_CONFIG);

					if(desc != null) {
						NotifDesc.setGlobalDesc(this, desc);
						mDb.updateAllNotificationDescriptions(desc);


						//Update any notification if required
						Notifier.refreshNotification(this, mCampaignUrn, true);
					}
				}
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);

		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(0, MENU_ID_DELETE_TRIGGER, 0, R.string.trigger_menu_delete)
			.setEnabled(isAdminLoggedIn() || TrigUserConfig.removeTrigers);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		int pos = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
		mCursor.moveToPosition(pos);

		int trigId = mCursor.getInt(
					 mCursor.getColumnIndexOrThrow(TriggerDB.KEY_ID));
		
		
    	switch(item.getItemId()) {
    		
    	case MENU_ID_DELETE_TRIGGER: 
    		mDialogTrigId = trigId;
    		showDialog(DIALOG_ID_DELETE);
    		return true;
    		
    	default:
    		break;
    	}
    	
		return super.onContextItemSelected(item);
	}

	public void handleActionSelection(int trigId, TriggerActionDesc desc) {

		String prevActDesc = mDb.getActionDescription(trigId);
		TriggerActionDesc prevDesc = new TriggerActionDesc();
		prevDesc.loadString(prevActDesc);
		
		mDb.updateActionDescription(trigId, desc.toString());
		mCursor.requery();
		
		Notifier.refreshNotification(this, mCampaignUrn, true);
		
		if(desc.getCount() == 0 && prevDesc.getCount() !=0) {
			toggleTrigger(trigId, false);
		}
		
		if(desc.getCount() != 0 && prevDesc.getCount() == 0) {
			toggleTrigger(trigId, true);
		}
		
	}

	@Override
	public void onClick(View v) {
		
		if(v.getId() == R.id.button_add_new) {
			
			showDialog(DIALOG_ID_ADD_NEW);
		}
	}

}
