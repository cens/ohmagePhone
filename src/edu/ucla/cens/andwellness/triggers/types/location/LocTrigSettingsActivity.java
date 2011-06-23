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
package edu.ucla.cens.andwellness.triggers.types.location;



import java.util.HashSet;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.config.TrigUserConfig;
import edu.ucla.cens.andwellness.triggers.utils.TrigTextInput;

/*
 * Location triggers settings activity.
 * Displays the list of categories (places). The number of locations
 * under each category as well the number of surveys associated with
 * each place are also displayed. Provides options to manage the
 * categories (add, delete, rename and modify surveys)
 */
public class LocTrigSettingsActivity extends ListActivity 
			 implements OnClickListener, 
			 			TextWatcher {

	private static final String DEBUG_TAG = "LocationTrigger";
	
	/* Menu ids */
	private static final int MENU_DELETE_CATEG = Menu.FIRST;
	private static final int MENU_RENAME_CATEG = Menu.FIRST + 1;
	
	private static final int DIALOG_DELETE = 0;
	private static final int DIALOG_RENAME = 1;
	
	private static final String KEY_SAVE_DIALOG_CATEG = "dialog_category";
	private static final String KEY_SAVE_DIALOG_TEXT = "dialog_text";
	public  static final String KEY_ADMIN_MODE = "admin_mode";
	
	//Db instance
	private LocTrigDB mDb;
	//The list cursor
	private Cursor mCursor;
	private HashSet<String> mCategNames;
	private LocationTrigger mLocTrigger = new LocationTrigger();
	
	private int mDialogCategId =  -1;
	private String mDialogText = null;
	private boolean mAdminMode = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(DEBUG_TAG, "Main: onCreate");
    	
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.trigger_loc_settings);
        
        Button bAdd = (Button) findViewById(R.id.trigger_button_add_categ);
        bAdd.setOnClickListener(this);
        bAdd.setEnabled(false);
        
        EditText et = (EditText) findViewById(R.id.trigger_text_add_categ);
        et.addTextChangedListener(this);
        
        mAdminMode = getIntent().getBooleanExtra(KEY_ADMIN_MODE, false);
    	if(!mAdminMode && !TrigUserConfig.editLocationTriggerPlace) {
    		et.setEnabled(false);
    		et.setClickable(false);
    		et.setFocusable(false);
    	}
        
        mDb = new LocTrigDB(this);
        mDb.open();
        
        mCategNames = new HashSet<String>();
        populateCategoryNames();
        
      
        initializeList();
        registerForContextMenu(getListView()); 
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	outState.putInt(KEY_SAVE_DIALOG_CATEG, mDialogCategId);
    	if(mDialogText != null && mDialogText.length() != 0) {
    		outState.putString(KEY_SAVE_DIALOG_TEXT, mDialogText);
    	}
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
    	
    	mDialogCategId = state.getInt(KEY_SAVE_DIALOG_CATEG, -1);
    	mDialogText = state.getString(KEY_SAVE_DIALOG_TEXT);
    }
   

    @Override
    public void onDestroy() {
    	Log.i(DEBUG_TAG, "Main: onDestroy");
    	
    	mCategNames.clear();
    	
    	mCursor.close();
    	mDb.close();
    	System.gc();
    	
    	super.onDestroy();
    }
    
    private void populateCategoryNames() {
    	mCategNames.clear();
    	
    	Cursor c = mDb.getAllCategories();
    	
    	if(c.moveToFirst()) {
    		do {
    			
    			String name = c.getString(
    					      c.getColumnIndexOrThrow(LocTrigDB.KEY_NAME));
    			mCategNames.add(name.toLowerCase());
    			
    		} while(c.moveToNext());
    	}
    	
    	c.close();
    }
    
    private boolean checkIfCategNameValid(String name) {
    	if(name == null) {
    		return false;
    	}
    	
    	String text = name.trim();
		if(text.length() == 0 || mCategNames.contains(text.toLowerCase())) {
			return false;
		}
		
		return true;
    }
    
    private void updateTriggerDescriptions(String oldName, String newName) {
    	LinkedList<Integer> trigIds = new LinkedList<Integer>();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getCampaigns()) {
			trigIds.addAll(mLocTrigger.getAllActiveTriggerIds(this, c.mUrn));
		}
    	
    	for(int trigId : trigIds) {
    		LocTrigDesc desc = new LocTrigDesc();
    		
    		desc.loadString(mLocTrigger.getTrigger(this, trigId));
    		if(desc.getLocation().equals(oldName)) {
    			desc.setLocation(newName);
    			mLocTrigger.updateTrigger(this, trigId, desc.toString());
    		}
    	}
    	
    	trigIds.clear();
    }
    
    private void removeTriggers(String categName) {
    	LinkedList<Integer> trigIds = new LinkedList<Integer>();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getCampaigns()) {
			trigIds.addAll(mLocTrigger.getAllActiveTriggerIds(this, c.mUrn));
		}
    	
    	for(int trigId : trigIds) {
    		LocTrigDesc desc = new LocTrigDesc();
    		
    		desc.loadString(mLocTrigger.getTrigger(this, trigId));
    		if(desc.getLocation().equals(categName)) {
    			mLocTrigger.deleteTrigger(this, trigId);
    		}
    	}
    	
    	trigIds.clear();
    }
    
    /* Populate the categories listview */
    private void initializeList() {
    	
    	//The viewbinder class to define each list item
        class CategListViewBinder 
			  implements SimpleCursorAdapter.ViewBinder {

			@Override
			public boolean setViewValue(View view, Cursor c, int colIndex) {
			
				switch(view.getId()) {
				
				case R.id.text2: //locations count
					TextView tv = (TextView) view;
					
					Cursor cLocs = mDb.getLocations(c.getInt(colIndex));
					int nLocs = cLocs.getCount();
					cLocs.close();
					
					String locStr = (nLocs == 1) ? " " +
									 getString(R.string.location_on_map) : " " +
									 getString(R.string.locations_on_map);
									 
					tv.setText("" + nLocs + locStr);
					return true;
				}
			
				return false;
			}
        }
        
    	mCursor = mDb.getAllCategories();
  
    	mCursor.moveToFirst();
    	startManagingCursor(mCursor);
    	
    	String[] from = new String[] {LocTrigDB.KEY_NAME, LocTrigDB.KEY_ID};
    	
    	int[] to = new int[] {R.id.text1, R.id.text2};
    	SimpleCursorAdapter categories = 
    		new SimpleCursorAdapter(this, R.layout.trigger_loc_settings_row, 
    								mCursor, from, to);
    	
    	categories.setViewBinder(new CategListViewBinder());
    	setListAdapter(categories);
    }
    
    private void refreshList() {
    	mCursor.requery();
    	populateCategoryNames();
    }
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, 
									ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		int builtInStatus = mCursor.getInt(
					  		mCursor.getColumnIndexOrThrow(LocTrigDB.KEY_BUILT_IN));
		
		boolean isBuiltIn = (builtInStatus == 1) ? true : false;
		
		menu.add(0, MENU_DELETE_CATEG, 0, R.string.menu_delete)
			.setEnabled(!isBuiltIn && 
						(mAdminMode || TrigUserConfig.editLocationTriggerPlace));
		menu.add(0, MENU_RENAME_CATEG, 0, R.string.menu_rename)
			.setEnabled(!isBuiltIn && 
						(mAdminMode || TrigUserConfig.editLocationTriggerPlace));
	}

    private void deleteCategory(int categId) {
    	String categName = mDb.getCategoryName(categId);
    	mDb.removeCategory(categId);
		refreshList();
		removeTriggers(categName);
		
		Intent i = new Intent(this, LocTrigService.class);
    	i.setAction(LocTrigService.ACTION_UPDATE_LOCATIONS);
    	startService(i);
    }
    
    private void renameCategory(int trigId, String newName) {
    	if(newName == null || newName.length() == 0) {
    		return;
    	}
    	
    	String oldName = mDb.getCategoryName(mDialogCategId);
    	mDb.renameCategory(mDialogCategId, newName);
    	refreshList();
    	updateTriggerDescriptions(oldName, newName);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	
    	switch(id) {
    	case DIALOG_DELETE:
    		AlertDialog dialog = 
    			new AlertDialog.Builder(this)
	    	    .setPositiveButton(R.string.yes, 
	    	    				   new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == AlertDialog.BUTTON_POSITIVE) {
							deleteCategory(mDialogCategId);
						}
					}
				})
	    	    .setNegativeButton(R.string.no, null)
	    	    .setTitle("Delete " + mDb.getCategoryName(mDialogCategId) + "?")
	    	    .setMessage("All the locations and triggers " +
						  	"associated with this place will be " +
				  			"removed")
	    	    .create();
    		return dialog;
    		
    	case DIALOG_RENAME:
    		
    		TrigTextInput ti = new TrigTextInput(this);
    		ti.setPositiveButtonText(getString(R.string.done));
			ti.setNegativeButtonText(getString(R.string.cancel));
			String name = mDb.getCategoryName(mDialogCategId);
    		ti.setTitle("Rename " + name);
    		if(mDialogText != null) {
    			ti.setText(mDialogText);

    		}
    		else {
    			ti.setText(name);
    		}
    		
			ti.setOnClickListener(new TrigTextInput.onClickListener() {
				
				@Override
				public void onClick(TrigTextInput ti, int which) {
					if(which == TrigTextInput.BUTTON_POSITIVE) {
						renameCategory(mDialogCategId, ti.getText());
					}
					
				}
			});
			ti.setOnTextChangedListener(new TrigTextInput.onTextChangedListener() {
				
				@Override
				public boolean onTextChanged(TrigTextInput ti, String text) {
					mDialogText = text;
					//If the text change to the same name, return true
					if(text.trim().equalsIgnoreCase(mDb.getCategoryName(mDialogCategId))) {
						return true;
					}
					return checkIfCategNameValid(text);
				}
			});
    		
    		return ti.createDialog();
    		
    	default:
    		return null;
    	}
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	
    	int pos = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
		mCursor.moveToPosition(pos);

		int categId = mCursor.getInt(
				mCursor.getColumnIndexOrThrow(LocTrigDB.KEY_ID));
		
    	switch(item.getItemId()) {
    		
    	case MENU_DELETE_CATEG:
    		mDialogCategId = categId;
    		mDialogText = null;
    		removeDialog(DIALOG_DELETE);
    		showDialog(DIALOG_DELETE);
    		return true;
    		
    	case MENU_RENAME_CATEG: //Rename category
    		mDialogCategId = categId; 
    		removeDialog(DIALOG_RENAME);
    		showDialog(DIALOG_RENAME);
    		return true;
    		
    	default:
    		break;
    	}
    	
		return super.onContextItemSelected(item);
	}
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        if(!mCursor.moveToPosition(position)) {
			//TODO this should not happen. log
			return;
		}
        
        //start the maps activity
        Intent i = new Intent(this, LocTrigMapsActivity.class);
        i.putExtra(LocTrigDB.KEY_ID, (int)id);
		startActivity(i);
    }
	
	private void addNewCategory() {
    	EditText et = (EditText) findViewById(R.id.trigger_text_add_categ);
    	
    	mDb.addCategory(et.getText().toString().trim());
    	refreshList();
    	
	    //Hide the onscreen keyboard
	    InputMethodManager imm = (InputMethodManager)getSystemService(
	    		Context.INPUT_METHOD_SERVICE); 
	    imm.hideSoftInputFromWindow(et.getWindowToken(), 0); 
	    
	    //Clear the edit text
	    et.setText("");
    }
    
	@Override
	public void onClick(View v) {
		
		if(v.getId() == R.id.trigger_button_add_categ) {
			addNewCategory();
		}
	}


	@Override
	public void afterTextChanged(Editable s) {

		boolean buttonStatus = true;
		if(!checkIfCategNameValid(s.toString())) {
			buttonStatus = false;
		}
		
		
		Button bAdd = (Button) findViewById(R.id.trigger_button_add_categ);
		bAdd.setEnabled(buttonStatus);
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
}
