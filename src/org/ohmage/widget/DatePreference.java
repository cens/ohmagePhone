
package org.ohmage.widget;

import org.ohmage.R;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DatePreference extends DialogPreference {
	private DatePicker picker = null;

	public DatePreference(Context ctxt) {
		this(ctxt, null);
	}

	public DatePreference(Context ctxt, AttributeSet attrs) {
		this(ctxt, attrs, 0);
	}

	public DatePreference(Context ctxt, AttributeSet attrs, int defStyle) {
		super(ctxt, attrs, defStyle);

		setPositiveButtonText(R.string.set);
		setNegativeButtonText(R.string.cancel);
	}

	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);

		long time = getPersistedLong(0);
		if (time != 0) {
			setSummaryTime(time);
		}
	}

	@Override
	protected View onCreateDialogView() {
		picker = new DatePicker(getContext());
		return picker;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);

		long time = getPersistedLong(0);
		if (time != 0) {
			setTime(time);
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			long time = getTime();

			if (callChangeListener(time)) {
				persistLong(time);
				setSummaryTime(time);
			}
		}
	}

	private void setSummaryTime(long time) {
		setSummary(SimpleDateFormat.getDateInstance().format(time));
	}
	
	public long getTime() {
		if (picker != null) {
			Calendar cal = Calendar.getInstance();
			cal.set(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
			return cal.getTimeInMillis();
		}
		return 0;
	}

	public void setTime(long time) {
		if (picker != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(time);
			picker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY),
					cal.get(Calendar.DAY_OF_MONTH));
			setSummaryTime(cal.getTimeInMillis());
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (picker == null) {
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.time = getTime();
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		setTime(myState.time);
	}

	private static class SavedState extends BaseSavedState {
		long time;

		public SavedState(Parcel source) {
			super(source);
			time = source.readLong();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeLong(time);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}
	}

}
