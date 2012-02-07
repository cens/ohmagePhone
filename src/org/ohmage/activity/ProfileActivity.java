package org.ohmage.activity;

import org.ohmage.AccountHelper;
import org.ohmage.R;
import org.ohmage.ui.BaseInfoActivity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ProfileActivity extends BaseInfoActivity {
	
	private AccountHelper mAccountHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// make the background of the profile striped to indicate it's not used yet
		setContentView(R.layout.profile_layout);
		
		mAccountHelper = new AccountHelper(this);

		// set up some generic stuff for the profile, since it's not databound in any respect
		mIconView.setImageResource(R.drawable.entity_profile);
		mHeadertext.setText(mAccountHelper.getUsername());
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
						mAccountHelper.logout();
						break;
					case R.id.profile_info_button_update_password:
						mAccountHelper.updatePassword();
						break;
				}
			}
		};
		
		// ...and then hook them both up to it
		Button updatePasswordButton = (Button) findViewById(R.id.profile_info_button_update_password);
		Button logoutWipeButton = (Button) findViewById(R.id.profile_info_button_logout_wipe);
		
		updatePasswordButton.setOnClickListener(profileActionListener);
		logoutWipeButton.setOnClickListener(profileActionListener);
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = mAccountHelper.onCreateDialog(id);
		if(dialog == null)
			dialog = super.onCreateDialog(id);
		return dialog;
	}
}