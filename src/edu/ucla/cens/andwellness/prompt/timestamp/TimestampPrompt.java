package edu.ucla.cens.andwellness.prompt.timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
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
	public View getView(Context context) {
		// TODO Auto-generated method stub
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.prompt_timestamp, null);
		
		TimePicker timePicker = (TimePicker) layout.findViewById(R.id.time_picker);
		DatePicker datePicker = (DatePicker) layout.findViewById(R.id.date_picker);
		
		if (mTime == null) {
			mTime = Calendar.getInstance();
		}
		
		timePicker.setCurrentHour(mTime.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(mTime.get(Calendar.MINUTE));
		
		datePicker.init(mTime.get(Calendar.YEAR), mTime.get(Calendar.MONTH), mTime.get(Calendar.DAY_OF_MONTH), new OnDateChangedListener() {
			
			@Override
			public void onDateChanged(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				// TODO Auto-generated method stub
				mTime.set(year, monthOfYear, dayOfMonth);
			}
		});
		
		timePicker.setOnTimeChangedListener(new OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				mTime.set(Calendar.MINUTE, minute);
				mTime.set(Calendar.SECOND, 0);
			}
		});
		
		
		
		return layout;
	}
}
