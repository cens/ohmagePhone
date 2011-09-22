package org.ohmage.activity;

import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.triggers.config.TrigUserConfig;
import org.ohmage.triggers.utils.TrigTextInput;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ProfileActivity extends BaseInfoActivity {
	private static final int DIALOG_CLEAR_USER_CONFIRM = 1;
	private static final int DIALOG_CLEAR_USER_PINCODE = 2;
	private FragmentActivity mContext;
	private SharedPreferencesHelper mSharedPreferencesHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// set up some generic stuff for the profile, since it's not databound in any respect
		mContext = this;
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		mIconView.setImageResource(R.drawable.entity_profile);
		mHeadertext.setText(mSharedPreferencesHelper.getUsername());
		/*
		mSubtext.setVisibility(View.INVISIBLE);
		mNotetext.setVisibility(View.INVISIBLE);
		*/
		
		// fill up the button tray with the profile's regular buttons
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.profile_info_buttons, mButtonTray, true);
		
		// create a catch-all handler that we'll pass to both buttons...
		OnClickListener profileActionListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.profile_info_button_logout_wipe:
						showDialog(DIALOG_CLEAR_USER_CONFIRM);
						break;
					case R.id.profile_info_button_update_password:
						// i suppose we'll just go straight to the login screen after clearing the back stack
						Intent intent = new Intent(mContext, LoginActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						finish();
						break;
				}
			}
		};
		
		// ...and then hook them both up to it
		Button updatePasswordButton = (Button) findViewById(R.id.profile_info_button_update_password);
		Button logoutWipeButton = (Button) findViewById(R.id.profile_info_button_logout_wipe);
		
		updatePasswordButton.setOnClickListener(profileActionListener);
		logoutWipeButton.setOnClickListener(profileActionListener);
		
		// make the background of the profile striped to indicate it's not used yet
		getContentArea().setBackgroundResource(R.drawable.unused_bkgnd);
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
			case DIALOG_CLEAR_USER_CONFIRM:			
	        	dialogBuilder.setTitle("Confirm")
				.setMessage("Are you sure you wish to clear all user data? Any data that has not been uploaded will be lost, and the app will be restored to its initial state.")
				.setNegativeButton("No", null)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (SharedPreferencesHelper.REQUIRE_PIN_ON_CLEAR_USER) {
							showDialog(DIALOG_CLEAR_USER_PINCODE);
						} else {
							((OhmageApplication)getApplication()).resetAll();
							setResult(125);
							finish();
						}
					}
	
				});
	        	dialog = dialogBuilder.create();        	
	        	break;
			case DIALOG_CLEAR_USER_PINCODE:
				dialog = createClearUserPincodeDialog();
				break;
        }
		
		return dialog;
	}
	
	private Dialog createClearUserPincodeDialog() {
		TrigTextInput ti = new TrigTextInput(this);
		ti.setNumberMode(true);
		ti.setPasswordMode(true);
		ti.setAllowEmptyText(false);
		ti.setPositiveButtonText("Ok");
		ti.setNegativeButtonText("Cancel");
		ti.setTitle("Enter pin code:");
		
		ti.setText("");
		
		ti.setOnClickListener(new TrigTextInput.onClickListener() {
			@Override
			public void onClick(TrigTextInput ti, int which) {
				if (which == TrigTextInput.BUTTON_POSITIVE) {
					if(ti.getText().equals(TrigUserConfig.adminPass)) {
						((OhmageApplication)getApplication()).resetAll();
						setResult(125);
						finish();
					}
					else {
						removeDialog(DIALOG_CLEAR_USER_PINCODE);
						Toast.makeText(ProfileActivity.this, "Wrong pin code.", Toast.LENGTH_SHORT).show();
					}
				} else {
					removeDialog(DIALOG_CLEAR_USER_PINCODE);
				}
			}
		});
		
		return ti.createDialog();
	}
}
