package org.ohmage.controls;

import java.util.ArrayList;

import org.ohmage.R;

import android.content.Context;
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

public class FilterControl extends LinearLayout {
	private ArrayList<Pair<String, String>> mItemList;
	private FilterChangeListener mFilterChangeListener;
	private int mSelectionIndex;
	private Button mCurrentBtn;
	private Button mPrevBtn;
	private Button mNextBtn;
	
	public FilterControl(Context context) {
		super(context);
		
		// just construct the base control
		initControl(context);
	}

	public FilterControl(Context context, AttributeSet attrs) {
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
		
		// load up the elements of the actionbar from controls_filter.xml
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.controls_filter, this, true);
		
		// and init an empty list so we don't crash and burn when we try to read before populating
		mItemList = new ArrayList<Pair<String,String>>();
		mSelectionIndex = 0;
		
		mPrevBtn = (Button) findViewById(R.id.controls_filter_prev);
		mCurrentBtn = (Button) findViewById(R.id.controls_filter_current);
		mNextBtn = (Button) findViewById(R.id.controls_filter_next);
		
		FilterClickHandler handler = new FilterClickHandler();
		
		mPrevBtn.setOnClickListener(handler);
		mCurrentBtn.setOnClickListener(handler);
		mNextBtn.setOnClickListener(handler);
	}
	
	/**
	 * Initializes the appearance of the control based on the attributes passed from the xml.
	 * @param attrs the collection of attributes, usually provided in the control's constructor
	 */
	protected void initStyles(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ActivityBarControl);
	}
	
	/**
	 * Inserts items into the filter's list from a cursor.
	 * 
	 * @param data the cursor from which to load the data; will be closed when the method returns.
	 * @param textColumn the column of the cursor to use as the displayed text on the filter
	 * @param valueColumn the column of the cursor to return from the {@link populate} method
	 */
	public void populate(Cursor data, String textColumn, String valueColumn) {
		int textColIdx = data.getColumnIndex(textColumn);
		int valueColIdx = data.getColumnIndex(valueColumn);
		
		// read the cursor into our internal paging list
		mItemList.clear();
		mSelectionIndex = 0;

		while (data.moveToNext()) {
			mItemList.add(Pair.create(data.getString(textColIdx), data.getString(valueColIdx)));
		}
		
		data.close();
		
		// and do a final sync
		syncState();
	}
	
	/**
	 * Replaces the items in the filter's list with an ArrayList of Pair<String,String>.
	 * 
	 * @param itemList the list of items to populate the list
	 */
	public void populate(ArrayList<Pair<String,String>> itemList) {
		// just copy the arraylist into our internal list
		mItemList = itemList;
		mSelectionIndex = 0;
		
		// and do a final sync
		syncState();
	}
	
	/**
	 * Adds an item into the existing item list before the specified index.
	 * 
	 * @param index the index which the new item will have
	 * @param item the item to insert
	 */
	public void add(int index, Pair<String,String> item) {
		mItemList.add(index, item);
		
		// shift the current index up if we're inserting before
		if (index <= mSelectionIndex)
			mSelectionIndex += 1;
		
		// and make sure we're displaying the right thing
		syncState();
	}
	
	/**
	 * Adds an item to the end of the existing item list.
	 */
	public void add(Pair<String,String> item) {
		mItemList.add(item);
		syncState();
	}
	
	public int getIndex() {
		return mSelectionIndex;
	}
	
	public int size() {
		return mItemList.size();
	}
	
	public String getText() {
		return mItemList.get(mSelectionIndex).first;
	}
	
	public String getValue() {
		return mItemList.get(mSelectionIndex).second;
	}
	
	public void clearAll(){
		mItemList.clear();
		mSelectionIndex = 0;
	}

	
	/**
	 * Attaches a {@link FilterChangeListener} to the filter which will be called when the user navigates between items.
	 * 
	 * @param listener an object implementing {@link FilterChangeListener} which will be called when the list index is changed.
	 */
	public void setOnChangeListener(FilterChangeListener listener) {
		mFilterChangeListener = listener;
	}
	
	public static interface FilterChangeListener {
		public void onFilterChanged(String curValue);
	}

	/**
	 * Handles the next, previous, and current buttons in the view.
	 * 
	 * Done here so that it has access to the private lists, current index, etc.
	 * @param v the view which generated the click; this method uses the id of the view to determine what to do
	 */
	private class FilterClickHandler implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.controls_filter_prev:
					if (mSelectionIndex > 0) {
						mSelectionIndex -= 1;
						syncState();
					}
					break;
				case R.id.controls_filter_current:
					break;
				case R.id.controls_filter_next:
					if (mSelectionIndex < mItemList.size()-1) {
						mSelectionIndex += 1;
						syncState();
					}
					break;
			}
		}
	}
	
	// keeps the middle text button in sync with the current index
	private void syncState() {
		if (mItemList.size() <= 0) {
			mCurrentBtn.setText("");
			return;
		}
		
		// depending on where we are in the list, dim the controls
		mPrevBtn.setTextColor((mSelectionIndex == 0)?Color.LTGRAY:Color.BLACK);
		mNextBtn.setTextColor((mSelectionIndex == mItemList.size() - 1)?Color.LTGRAY:Color.BLACK);
		
		Pair<String,String> curItem = mItemList.get(mSelectionIndex);
		
		mCurrentBtn.setText(curItem.first);
		
		if (mFilterChangeListener != null)
			mFilterChangeListener.onFilterChanged(curItem.second);
	}
	
	// utility method for converting dp to pixels, since the setters only take pixel values :\
	private int dpToPixels(int padding_in_dp) {
		final float scale = getResources().getDisplayMetrics().density;
	    return (int) (padding_in_dp * scale + 0.5f);
	}
}
