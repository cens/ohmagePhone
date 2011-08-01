package edu.ucla.cens.andwellness.prompt.timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import edu.ucla.cens.andwellness.NumberPicker;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;

public class TimestampPrompt extends AbstractPrompt {

	private Calendar mTime;
	
	@Override
	protected Object getTypeSpecificResponseObject() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dateFormat.format(mTime.getTime());
	}

	@Override
	protected Object getTypeSpecificExtrasObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		mTime = Calendar.getInstance();
	}
	
	@Override
	public String getUnansweredPromptText() {
		return "Please enter a valid time.";
	}

	@Override
	public boolean isPromptAnswered() {
		// TODO Auto-generated method stub
		if (mTime == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub

	}

	@Override
	public View getView(final Context context) {
		// TODO Auto-generated method stub
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.prompt_timestamp, null);
		
		TimePicker timePicker = (TimePicker) layout.findViewById(R.id.time_picker);
		final Button dateButton = (Button) layout.findViewById(R.id.date_button);
		
		if (mTime == null) {
			mTime = Calendar.getInstance();
		}
		
		timePicker.setCurrentHour(mTime.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(mTime.get(Calendar.MINUTE));
		
		final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
		dateButton.setText(dateFormat.format(mTime.getTime()));
		
		timePicker.setOnTimeChangedListener(new OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				mTime.set(Calendar.MINUTE, minute);
				mTime.set(Calendar.SECOND, 0);
			}
		});
		
		final OnDateSetListener listener = new OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				// TODO Auto-generated method stub
				mTime.set(year, monthOfYear, dayOfMonth);
				dateButton.setText(dateFormat.format(mTime.getTime()));
			}
		};
		
		dateButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new DatePickerDialog(context, listener, mTime.get(Calendar.YEAR), mTime.get(Calendar.MONTH), mTime.get(Calendar.DAY_OF_MONTH)).show();
			}
		});
		
		return layout;
	}
}
