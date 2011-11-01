package org.ohmage.fragments;

import org.ohmage.R;
import org.ohmage.triggers.config.TrigUserConfig;
import org.ohmage.triggers.utils.TrigTextInput;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class AdminDialogFragment extends DialogFragment {

	public interface AdminCodeListener {
		public void onAdminCodeEntered(boolean success);
	}

	private AdminCodeListener mListener;
	private boolean mSuccess = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (AdminCodeListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement AdminCodeListener");
		}
	}

	@Override
	public void onDismiss(DialogInterface dialogInterface) {
		super.onDismiss(dialogInterface);
		mListener.onAdminCodeEntered(mSuccess);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		TrigTextInput ti = new TrigTextInput(getActivity());
		ti.setNumberMode(true);
		ti.setPasswordMode(true);
		ti.setAllowEmptyText(false);
		ti.setPositiveButtonText(getString(R.string.ok));
		ti.setNegativeButtonText(getString(R.string.cancel));
		ti.setTitle(getActivity().getString(R.string.admin_dialog_title));

		ti.setText("");

		ti.setOnClickListener(new TrigTextInput.onClickListener() {

			@Override
			public void onClick(TrigTextInput ti, int which) {
				if (which == TrigTextInput.BUTTON_POSITIVE) {
					if(ti.getText().equals(TrigUserConfig.adminPass)) {
						mSuccess = true;
					} else {
						Toast.makeText(getActivity(), R.string.admin_dialog_invalid_pin, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		return ti.createDialog();
	}
}
