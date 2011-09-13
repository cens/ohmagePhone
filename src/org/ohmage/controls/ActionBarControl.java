package org.ohmage.controls;

import org.ohmage.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActionBarControl extends LinearLayout {
	public ActionBarControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, 50);
		this.setLayoutParams(params);
		this.setBackgroundResource(R.drawable.title_bkgnd);
		
		// load up the elements of the actionbar from controls_actionbar.xml
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.controls_actionbar, this, true);
		
		// apply the xml-specified attributes, too
		initStyles(attrs);
	}
	
	/**
	 * Initializes the appearance of the control based on the attributes passed from the xml.
	 * @param attrs the collection of attributes, usually provided in the control's constructor
	 */
	protected void initStyles(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ActivityBarControl);
		
		boolean showlogo = a.getBoolean(R.styleable.ActivityBarControl_showlogo, true);
		String titleText = a.getString(R.styleable.ActivityBarControl_title);
		
		ImageView logo = (ImageView) findViewById(R.id.title_logo);
		ImageView logo_home = (ImageView) findViewById(R.id.title_logo_home);
		TextView tx = (TextView) findViewById(R.id.title_text);
		tx.setText(titleText);
		
		if (showlogo) {
			logo.setVisibility(VISIBLE);
			logo_home.setVisibility(GONE);
			tx.setVisibility(INVISIBLE);
		}
		else {
			logo.setVisibility(GONE);
			logo_home.setVisibility(VISIBLE);
			tx.setVisibility(VISIBLE);
		}
	}
	
	public void setText(String text) {
		TextView tx = (TextView) findViewById(R.id.title_text);
		tx.setText(text);
	}
}
