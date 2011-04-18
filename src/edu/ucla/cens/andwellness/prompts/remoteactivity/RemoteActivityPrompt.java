package edu.ucla.cens.andwellness.prompts.remoteactivity;

import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompts.AbstractPrompt;
import edu.ucla.cens.systemlog.Log;

/**
 * Prompt that will launch a remote Activity that can either be part of this
 * application or another application installed on the system. The remote 
 * Activity can be called as soon as this prompt loads by setting the
 * 'autolaunch' field or later via a "Replay" button. The number of replays
 * can also be set.
 * 
 * @author John Jenkins
 * @version 1.0
 */
public class RemoteActivityPrompt extends AbstractPrompt implements OnClickListener
{
	private static final String TAG = "RemoteActivityPrompt";
	
	private static final String FEEDBACK_STRING = "feedback";

	private String packageName;
	private String activityName;
	private String actionName;
	private JSONArray responseArray;
	
	private TextView feedbackText;
	private Button launchButton;
	
	private Activity callingActivity;
	
	private boolean launched;
	private boolean autolaunch;
	private int retries;
	
	/**
	 * Basic default constructor.
	 */
	public RemoteActivityPrompt()
	{
		super();
		
		launched = false;
	}

	/**
	 * Creates the View from an XML file and sets up the local variables for
	 * the Views contained within. Then, if automatic launch is turned on it
	 * will attempt to automatically launch the remote Activity.
	 */
	@Override
	public View getView(Context context)
	{
		try
		{
			callingActivity = (Activity) context;
		}
		catch(ClassCastException e)
		{
			callingActivity = null;
			Log.e(TAG, "getView() recieved a Context that wasn't an Activity.");
			// Should we error out here?
		}
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.prompt_remote_activity, null);
		
		feedbackText = (TextView) layout.findViewById(R.id.prompt_remote_activity_feedback);
		launchButton = (Button) layout.findViewById(R.id.prompt_remote_activity_replay_button);
		launchButton.setOnClickListener(this);
		launchButton.setText((!launched && !autolaunch) ? "Launch" : "Relaunch");
		
		if(retries > 0)
		{
			launchButton.setVisibility(View.VISIBLE);
		}
		else
		{
			if(!launched && !autolaunch)
			{
				launchButton.setVisibility(View.VISIBLE);
			}
			else
			{
				launchButton.setVisibility(View.GONE);
			}
		}
		
		if(autolaunch && !launched)
		{
			launchActivity();
		}
		
		return layout;
	}

	/**
	 * If the 'resultCode' indicates failure then we treat it as if the user
	 * has skipped the prompt. If skipping is not allowed, we log it as an
	 * error.
	 * 
	 * If the 'resultCode' indicates success then we check to see what was
	 * returned via the parameterized 'data' object. If 'data' is null, we put
	 * nothing into the 'responseObject'. If not, we check all the keys in the
	 * Bundle attached to 'data' and add their key-value pairs to the 
	 * 'responseObject' JSONObject. In the case of any invalid value, we 
	 * report the error but continue working on the rest of the key-value
	 * pairs. Finally, it will check to see if 'data', not the bundle it was
	 * associated with, had the key "feedback" in it. If so, it will populate
	 * the text of the 'feedback' TextBox with that value provided it is a 
	 * String. 
	 */
	@Override
	public void handleActivityResult(Context context, int requestCode, int resultCode, Intent data) 
	{
		if(resultCode == Activity.RESULT_CANCELED)
		{
			if(mSkippable.equalsIgnoreCase("true"))
			{
				this.setSkipped(true);
			}
			else if(mSkippable.equalsIgnoreCase("false"))
			{
				// The Activity was canceled for some reason, but it shouldn't
				// have been.
				Log.e(TAG, "The Activity was canceled, but the prompt isn't set as skippable.");
			}
			else
			{
				// This should _never_ happen!
				Log.e(TAG, "Invalid 'skippable' value: " + mSkippable);
			}
		}
		else if(resultCode == Activity.RESULT_OK)
		{
			if(data != null)
			{
				if(responseArray == null)
				{
					responseArray = new JSONArray();
				}
				
				JSONObject currResponse = new JSONObject();
				Bundle extras = data.getExtras();
				Set<String> keysList = extras.keySet();
				Iterator<String> keysIter = keysList.iterator();
				while(keysIter.hasNext())
				{
					String nextKey = keysIter.next();
					try
					{
						currResponse.put(nextKey, extras.get(nextKey).toString());
					}
					catch(JSONException e)
					{
						Log.e(TAG, "Invalid return value from remote Activity for key: " + nextKey);
					}
				}
				responseArray.put(currResponse);
				
				String feedback = data.getStringExtra(FEEDBACK_STRING);
				if(feedback != null)
				{
					feedbackText.setText(feedback);
				}
			}
		}
		// TODO: Possibly support user-defined Activity results:
		//			resultCode > Activity.RESULT_FIRST_USER
	}

	/**
	 * Returns the JSONObject that it has created from the values Bundled in
	 * the return of the remote Activity.
	 */
	@Override
	protected Object getTypeSpecificResponseObject() 
	{
		return responseArray;
	}
	
	/**
	 * There are no extras for this object.
	 */
	@Override
	protected Object getTypeSpecificExtrasObject()
	{
		return null;
	}

	/**
	 * Clears the local variable by recreating and reinstantiating it to a new
	 * one.
	 */
	@Override
	protected void clearTypeSpecificResponseData()
	{
		responseArray = new JSONArray();
	}
	
	/**
	 * Called when the "Replay" button is clicked. If autolaunch is not on and
	 * the remote Activity hasn't been launched yet, it will switch the text
	 * back to "Replay" from "Play" and launch the Activity. If there weren't
	 * any replays allowed, it will also remove the "Replay" button.
	 * 
	 * If the remote Activity has been launched be it from this button or from
	 * the autolaunch, it will check the number of retries left. If it is out
	 * of retries then it is an error to be in this state, but it will simply
	 * hide the button and leave the function. If there are retries left, it
	 * will decrement the number left, check if the user is out of replays in
	 * which case it will hide the "Replay" button, and will launch the remote
	 * Activity.
	 */
	@Override
	public void onClick(View v)
	{
		if(launched)
		{
			if(retries > 0)
			{
				retries--;
				
				if(retries <= 0)
				{
					launchButton.setVisibility(View.GONE);
				}
				
				launchActivity();
			}
			else
			{
				launchButton.setVisibility(View.GONE);
			}
		}
		else if(!autolaunch)
		{
			launchButton.setText("Relaunch");
			
			if(retries <= 0)
			{
				launchButton.setVisibility(View.GONE);
			}
			
			launchActivity();
		}
		else
		{
			Log.e(TAG, "Autolaunch is turned on, but I received a click on the \"Replay\" button before ever launching the remote Activity.");
		}
	}
	
	/**
	 * Sets the name of the Package to which the remote Activity belongs.
	 * 
	 * @param packageName The name of the Package to which the remote Activity
	 * 					  belongs.
	 * 
	 * @throws IllegalArgumentException Thrown if the 'packageName' is null or
	 * 									an empty string.
	 * 
	 * @see {@link #setActivity(String)}
	 * @see {@link #setAction(String)}
	 */
	public void setPackage(String packageName) throws IllegalArgumentException
	{
		if((packageName == null) || packageName.equals(""))
		{
			throw new IllegalArgumentException("Invalid Package name.");
		}
		this.packageName = packageName;
	}
	
	/**
	 * Sets the Activity to be called within the remote Package.
	 * 	
	 * @param activityName The name of the Activity to be called within the
	 * 					   remote Package.
	 * 
	 * @throws IllegalArgumentException Thrown if 'activityName' is null or an
	 * 									empty string.
	 * 
	 * @see {@link #setPackage(String)}
	 * @see {@link #setAction(String)}
	 */
	public void setActivity(String activityName) throws IllegalArgumentException
	{
		if((activityName == null) || packageName.equals(""))
		{
			throw new IllegalArgumentException("Invalid Activity name.");
		}
		this.activityName = activityName;
	}
	
	/**
	 * Sets the name of the Action as it is defined in the intent-filter of
	 * its Activity definition in the Manifest of its remote application.
	 * 
	 * @param activityName The String representing the Action to be set in the
	 * 					   Intent to the remote Activity.
	 * 
	 * @throws IllegalArgumentException Thrown if 'actionName' is "null" or
	 * 									an empty string.
	 * 
	 * @see {@link #setPackage(String)}
	 * @see {@link #setActivity(String)}
	 */
	public void setAction(String actionName) throws IllegalArgumentException
	{
		if((actionName == null) || (actionName.equals("")))
		{
			throw new IllegalArgumentException("Invalid Action name.");
		}
		this.actionName = actionName;
	}
	
	/**
	 * Sets the number of times that a user can relaunch the remote Activity.
	 * 
	 * @param numRetries The number of times a user can relaunch the remote
	 * 					 Activity.
	 */
	public void setRetries(int numRetries)
	{
		retries = numRetries;
	}
	
	/**
	 * Returns the number of remaining retries.
	 * 
	 * @return The number of times the remote Activity can be relaunched via
	 * 		   the "Replay" button, not counting the first replay if 
	 * 		   'autolaunch' is off.
	 */
	public int getNumRetriesRemaining()
	{
		return retries;
	}
	
	/**
	 * Sets whether or not the Activity will launch automatically when the
	 * prompt is displayed. If it is not set to automatically launch then the
	 * "Replay" button will be shown and have its text set to "Play". On
	 * subsequent displays of this Activity, the button will have its text
	 * switched back to "Replay".
	 * 
	 * @param autolaunch Whether or not the remote Activity should be launched
	 * 					 automatically when this view loads.
	 */
	public void setAutolaunch(boolean autolaunch)
	{
		this.autolaunch = autolaunch;
	}
	
	/**
	 * Returns whether or not the remote Activity has ever been launched from
	 * this prompt. This includes both the 'autolaunch' and the "Replay"
	 * button.
	 * 
	 * @return Whether or not the remote Activity has ever been launched from
	 * 		   this prompt.
	 */
	public boolean hasLaunchedRemoteActivity()
	{
		return launched;
	}
	
	/**
	 * Creates an Intent from the given 'activityName' and then launches the
	 * Intent.
	 */
	private void launchActivity()
	{
		if(callingActivity != null)
		{
			Intent activityToLaunch = new Intent(actionName);
			Activity activityContext = (Activity) callingActivity;
			activityToLaunch.setComponent(new ComponentName(packageName, activityName));
			activityContext.startActivityForResult(activityToLaunch, 0);
			
			launched = true;
		}
		else
		{
			Log.e(TAG, "The calling Activity was null.");
		}
	}
}
