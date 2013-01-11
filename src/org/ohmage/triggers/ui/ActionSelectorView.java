package org.ohmage.triggers.ui;

import org.mobilizingcs.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout;

public class ActionSelectorView extends RelativeLayout {
	static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	
	public ActionSelectorView(Context context, final String[] actions, final boolean[] actSelected) {
		super(context);

		// set us up first
		this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		this.setBackgroundColor(0xFFFFFFFF);

		// add an action list and set it up, too
		ListView triggerActionList = new ListView(context);
		triggerActionList.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		triggerActionList.setBackgroundColor(0xFFFFFFFF);
		triggerActionList.setCacheColorHint(0xFFFFFFFF);
		this.addView(triggerActionList);
		
		// set up the inner list and bind it to our data
		ArrayAdapter<String> surveyArrayAdapater = new ArrayAdapter<String>(context, R.layout.multi_choice_list_item, actions) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				CheckedTextView cv = (CheckedTextView)v;
				cv.setChecked(actSelected[position]);
				cv.setCheckMarkDrawable(R.drawable.btn_check_ohmage);
				return v;
			}
		};
		
		triggerActionList.setAdapter(surveyArrayAdapater);
		
		// set up the click handler, too
		triggerActionList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				// toggle the view's checked state and the array element that backs it
				CheckedTextView cv = (CheckedTextView)view;
				actSelected[position] = !cv.isChecked();
				cv.setChecked(actSelected[position]);
			}
		});
	}
}
