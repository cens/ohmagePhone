package org.ohmage.controls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.ohmage.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.util.Pair;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class DateFilterControl extends LinearLayout {
	private DateFilterChangeListener mFilterChangeListener;
	private Calendar mSelectedDate;
	private Button mCurrentBtn;
	private Button mPrevBtn;
	private Button mNextBtn;
	private Activity mActivity; // stores a reference to our calling activity
	private AlertDialog mItemListDialog; // stores a dialog containing a list of items, updated by populate() and add()
	private SimpleDateFormat mDateFormatter;
	
	public DateFilterControl(Context context) {
		super(context);
		
		mActivity = (Activity)context;
		
		// just construct the base control
		initControl(context);
	}

	public DateFilterControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// construct the base control
		initControl(context);
		
		// apply the xml-specified attributes, too
		initStyles(attrs);
	}
	
	/**
	 * Constructs the parts of the control that provide actual functionality. Declarative styling is handled by initStyles().
	 * @param context the context of whoever's creating this control, usually passed into the class constructor
	 */
	protected void initControl(Context context) {
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);
		this.setOrientation(HORIZONTAL);
		this.setPadding(0, 0, 0, dpToPixels(1));
		this.setBackgroundResource(R.drawable.controls_filter_bkgnd);
		
		// load up the elements of the actionbar from controls_filter.xml
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.controls_filter, this, true);
		
		// and init an empty list so we don't crash and burn when we try to read before populating
		mSelectedDate = Calendar.getInstance();
		
		mPrevBtn = (Button) findViewById(R.id.controls_filter_prev);
		mCurrentBtn = (Button) findViewById(R.id.controls_filter_current);
		mNextBtn = (Button) findViewById(R.id.controls_filter_next);
		
		FilterClickHandler handler = new FilterClickHandler();
		
		mPrevBtn.setOnClickListener(handler);
		mCurrentBtn.setOnClickListener(handler);
		mNextBtn.setOnClickListener(handler);
		
		mCurrentBtn.setSelected(true);
		
		// make a simple date formatter to display the thing (this may change depending on the selection)
		mDateFormatter = new SimpleDateFormat("MMMM yyyy");
		
		// also update the list dialog once (just going from current date)
		syncState();
		updateListDialog();
	}
	
	/**
	 * Initializes the appearance of the control based on the attributes passed from the xml.
	 * @param attrs the collection of attributes, usually provided in the control's constructor
	 */
	protected void initStyles(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ActivityBarControl);
	}
	
	/**
	 * Sets the currently selected date.
	 * @param newdate the date to select
	 */
	public void setDate(Calendar newDate) {
		mSelectedDate = newDate;
		syncState();
	}
	
	/**
	 * Gets the displayed text for the currently selected item (i.e. the text representation of the date)
	 * @return the displayed text as a string
	 */
	public String getText() {
		return mSelectedDate.toString();
	}
	
	/**
	 * Gets the defined value for the currently selected item (i.e. the actual Calendar object)
	 * @return the value as a Calendar
	 */
	public Calendar getValue() {
		return mSelectedDate;
	}
	
	/**
	 * Attaches a {@link DateFilterChangeListener} to the filter which will be called when the user navigates between items.
	 * 
	 * @param listener an object implementing {@link DateFilterChangeListener} which will be called when the list index is changed.
	 */
	public void setOnChangeListener(DateFilterChangeListener listener) {
		mFilterChangeListener = listener;
	}
	
	/**
	 * Exposes a callback to allow custom processing to occur when the filter is changed (either by navigation or population).
	 */
	public static interface DateFilterChangeListener {
		public void onFilterChanged(Calendar curValue);
	}

	/**
	 * Handles the next, previous, and current buttons in the view.
	 * 
	 * @param v the view which generated the click; this method uses the id of the view to determine what to do
	 */
	private class FilterClickHandler implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.controls_filter_prev:
					mSelectedDate.add(Calendar.MONTH, -1);
					syncState();
					break;
				case R.id.controls_filter_current:
					// the dialog changes with the current selection, so update it before we display it
					updateListDialog();
					mItemListDialog.show();
					break;
				case R.id.controls_filter_next:
					mSelectedDate.add(Calendar.MONTH, 1);
					syncState();
					break;
			}
		}
	}
	
	// keeps the middle text button in sync with the current index
	private void syncState() {
		mCurrentBtn.setText(mDateFormatter.format(mSelectedDate.getTime()));
		
		if (mFilterChangeListener != null)
			mFilterChangeListener.onFilterChanged(mSelectedDate);
	}
	
	// helper method for constructing a list dialog based on the current item list
	private void updateListDialog() {
		// build a list of items for us to choose from
		final int PICKER_RANGE = 6;
		// RANGE months before, current month, RANGE after, "today"
		final CharSequence items[] = new CharSequence[PICKER_RANGE*2 + 1];
		
		int idx = 0;
		for (int i = -PICKER_RANGE; i <= PICKER_RANGE; i++) {
			Calendar curDate = (Calendar)mSelectedDate.clone();
			curDate.add(Calendar.MONTH, i);
			items[idx++] = mDateFormatter.format(curDate.getTime());
		}

		// and construct a dialog that displays the list
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle("Choose an item");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        try {
		        	if (item == 0)
		        		mSelectedDate = Calendar.getInstance();
		        	else
		        		mSelectedDate.setTime(mDateFormatter.parse(items[item].toString()));
					
		        	syncState();
				}
				catch (ParseException e) {
					// i know it's bad form, but just ignore it...
				}
		    }
		});
		
		mItemListDialog = builder.create();
	}
	
	// utility method for converting dp to pixels, since the setters only take pixel values :\
	private int dpToPixels(int padding_in_dp) {
		final float scale = getResources().getDisplayMetrics().density;
	    return (int) (padding_in_dp * scale + 0.5f);
	}
}
