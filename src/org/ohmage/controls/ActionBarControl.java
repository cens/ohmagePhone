package org.ohmage.controls;

import org.ohmage.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class ActionBarControl extends LinearLayout {
	public ActionBarControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setBackgroundColor(R.color.title_background);
		
		// load up the elements of the actionbar from controls_actionbar.xml
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.controls_actionbar, this, true);
	}
}
