package org.ohmage.controls;

import org.ohmage.R;
import org.ohmage.activity.DashboardActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ActionBarControl extends LinearLayout {
	private final Activity mActivity;
	
	// view references
	private final TextView mTitleText;
	private final ImageButton mHomeButton;
	private final ProgressBar mProgressSpinner;
	private ImageView mHomeSeparator;
	
	// functionality
	private final List<ImageButton> mActionButtons;
	private final List<ImageView> mSeparators;
	private ActionListener mActionBarClickedListener;
	private final OnClickListener mActionButtonClickListener;
	
	// style flags
	private boolean mShowLogo;
	private boolean mShowHome;	

	public ActionBarControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mActivity = (Activity)context;
		
		// load up the elements of the actionbar from controls_actionbar.xml
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.controls_actionbar, this, true);
		
		// set up some basic layout parameters, like width x height and background
		this.setBackgroundResource(R.drawable.title_bkgnd);
		
		// gather member references
		mTitleText = (TextView) findViewById(R.id.controls_actionbar_title);
		mHomeButton = (ImageButton) findViewById(R.id.controls_actionbar_home);
		mProgressSpinner = (ProgressBar) findViewById(R.id.controls_actionbar_progress);
		
		// hook up buttons
		mHomeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.startActivity(new Intent(mActivity, DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		
		// serves as a single handler for routing all action button presses to a user-supplied callback
		mActionButtonClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// route clicks on the action bar to the user-supplied action handler, if present
				if (mActionBarClickedListener != null)
					mActionBarClickedListener.onActionClicked(v.getId());
			}
		};
		
		// holds a list of buttons that we manage
		mActionButtons = new ArrayList<ImageButton>();
		mSeparators = new ArrayList<ImageView>();
		
		// apply the xml-specified attributes, too
		initStyles(attrs);
	}
	
	public ActionBarControl(Context context) {
		this(context, null);
	}
	
	/**
	 * Initializes the appearance of the control based on the attributes passed from the xml.
	 * @param attrs the collection of attributes, usually provided in the control's constructor
	 */
	protected void initStyles(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ActivityBarControl);
		
		mShowLogo = a.getBoolean(R.styleable.ActivityBarControl_showlogo, false);
		String titleText = a.getString(R.styleable.ActivityBarControl_title);
		
		mHomeSeparator = (ImageView) findViewById(R.id.controls_actionbar_homebutton_separator);
		mTitleText.setText(titleText);
		
		setShowLogo(mShowLogo);
		
		// and set whether or not the home button appears
		setHomeVisibility(a.getBoolean(R.styleable.ActivityBarControl_showhome, true));
	}
	
	public void setShowLogo(boolean showLogo) {
		ImageView logo = (ImageView) findViewById(R.id.controls_actionbar_logo);
		if (showLogo) {
			logo.setVisibility(VISIBLE);
			mHomeSeparator.setVisibility(GONE);
			mHomeButton.setVisibility(GONE);
			mTitleText.setVisibility(INVISIBLE);
		} else {
			logo.setVisibility(GONE);
			mHomeSeparator.setVisibility(VISIBLE);
			mHomeButton.setVisibility(VISIBLE);
			mTitleText.setVisibility(VISIBLE);
		}
	}
	
	/**
	 * Set the visibility of the home button. Note that this is overridden by "showlogo";
	 * if the logo is visible, the home button will never appear.
	 * 
	 * @param isVisible if true, home button is shown; if false, home button is hidden
	 */
	public void setHomeVisibility(boolean isVisible) {
		if (!isVisible || mShowLogo) {
			mHomeSeparator.setVisibility(GONE);
			mHomeButton.setVisibility(GONE);
			mShowHome = false;
		}
		else {
			mHomeSeparator.setVisibility(VISIBLE);
			mHomeButton.setVisibility(VISIBLE);
			mShowHome = true;
		}
	}
	
	/**
	 * Sets the title for the action bar, which is displayed when showlogo is false.
	 * 
	 * @param text the text to display in the action bar
	 */
	public void setTitle(CharSequence text) {
		mTitleText.setText(text);
	}
	
	/**
	 * Shows or hides a progress bar "spinner" depending on the value of isVisible. Generally shown
	 * to indicate that a lengthy background operation is in progress and hidden once it's done.
	 * 
	 * @param isVisible shows spinner if true, hides it if false
	 */
	public void setProgressVisible(boolean isVisible) {
		mProgressSpinner.setVisibility(isVisible?VISIBLE:GONE);
	}
	
	/**
	 * Adds a command to the action bar, identified in callbacks by buttonID. Note that buttonID will be used
	 * as the ID of the ImageButton that this method creates.
	 * 
	 * @param buttonID an id which should uniquely identify this command, up to the caller to create
	 * @param description a string which defines this command, used for accessibility
	 * @param resid a drawable to render as the icon. should be the same size as the action bar (i.e. 40x40px hdpi)
	 */
	public void addActionBarCommand(int buttonID, String description, int resID) {
		// inflate a new separator and button from the dashboard templates
		// we're doing this mostly to add a predefined style to the view/button, which is currently impossible without xml
		ImageView newSeparator = (ImageView) mActivity.getLayoutInflater().inflate(R.layout.controls_actionbar_separator, null);
		ImageButton newButton = (ImageButton) mActivity.getLayoutInflater().inflate(R.layout.controls_actionbar_button, null);
		
		newButton.setId(buttonID);
		newButton.setContentDescription(description);
		newButton.setImageResource(resID);
		
		// attach to our existing listener for routing clicks through the callback
		newButton.setOnClickListener(mActionButtonClickListener);

		mSeparators.add(newSeparator);
		mActionButtons.add(newButton);
		
		// add to the parent layout now
		this.addView(newSeparator, new LayoutParams(1, LayoutParams.FILL_PARENT));
		this.addView(newButton, new LayoutParams(dpToPixels(45), LayoutParams.FILL_PARENT));
	}
	
	/**
	 * Removes all the action bar commands. This is somewhat useful if you want to recompose the action bar from scratch.
	 */
	public void clearActionBarCommands() {
		for (ImageButton button : mActionButtons)
			removeView(button);
		for (ImageView sep : mSeparators)
			removeView(sep);
	}
	
	/**
	 * Set a callback to be invoked when the user presses any button in the action bar.
	 * 
	 * @param listener a listener whose {@link ActionListener#onActionClicked(int)} method will be called when a click occurs.
	 */
	public void setOnActionListener(ActionListener listener) {
		mActionBarClickedListener = listener;
	}
	
	/**
	 * Callback that receives action bar button presses when registered with {@link ActionBarControl#setOnActionListener(ActionListener)}
	 * @author faisal
	 *
	 */
	public interface ActionListener {
		void onActionClicked(int commandID);
	}
	
	// utility method for converting dp to pixels, since the setters only take pixel values :\
	private int dpToPixels(int dp) {
		final float scale = getResources().getDisplayMetrics().density;
	    return (int) (dp * scale + 0.5f);
	}
}
