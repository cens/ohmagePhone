/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.andwellness.prompt.remoteactivity;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.widget.Toast;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
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
	private static final String SINGLE_VALUE_STRING = "score";

	private String packageName;
	private String activityName;
	private String actionName;
	private String input;
	private JSONArray responseArray;
	
	private TextView feedbackText;
	private Button launchButton;
	
	private Activity callingActivity;
	
	private boolean launched;
	private boolean autolaunch;
	
	private int runs;
	private int minRuns;
	private int retries;
	
	/**
	 * Basic default constructor.
	 */
	public RemoteActivityPrompt()
	{
		super();
		
		launched = false;
		runs = 0;
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
	 * error, but we do not make an entry in the results array to prevent
	 * corrupting it nor do we set as skipped to prevent us from corrupting
	 * the entire survey.
	 * 
	 * If the 'resultCode' indicates success then we check to see what was
	 * returned via the parameterized 'data' object. If 'data' is null, we put
	 * an empty JSONObject in the array to indicate that something went wrong.
	 * If 'data' is not null, we get all the key-value pairs from the data's
	 * extras and place them in a JSONObject. If the keys for these extras are
	 * certain "special" return codes, some of which are required, then we
	 * handle those as well which may or may not include putting them in the
	 * JSONObject. Finally, we put the JSONObject in the JSONArray that is the
	 * return value for this prompt type.
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
				
				boolean singleValueFound = false;
				JSONObject currResponse = new JSONObject();
				Bundle extras = data.getExtras();
				Iterator<String> keysIter = extras.keySet().iterator();
				while(keysIter.hasNext())
				{
					String nextKey = keysIter.next();
					if(FEEDBACK_STRING.equals(nextKey))
					{
						feedbackText.setText(extras.getString(nextKey));
					}
					else
					{
						try
						{
							currResponse.put(nextKey, extras.get(nextKey));
						}
						catch(JSONException e)
						{
							Log.e(TAG, "Invalid return value from remote Activity for key: " + nextKey);
						}
						
						if(SINGLE_VALUE_STRING.equals(nextKey))
						{
							singleValueFound = true;
						}
					}
				}
				
				if(singleValueFound)
				{
					responseArray.put(currResponse);
				}
				else
				{
					// We cannot add this to the list of responses because it
					// will be rejected for not containing the single-value
					// value.
					Log.e(TAG, "The remote Activity is not returning a single value which is required for CSV export.");
				}
			}
			else
			{
				// If the data is null, we put an empty JSONObject in the
				// array to indicate that the data was null.
				responseArray.put(new JSONObject());
				Log.e(TAG, "The data returned by the remote Activity was null.");
			}
		}
		// TODO: Possibly support user-defined Activity results:
		//			resultCode > Activity.RESULT_FIRST_USER
		//
		// One obvious possibility is some sort of "SKIPPED" return code.
	}
	
	/**
	 * Returns true if the number of runs is greater than the minimum required
	 * number of runs.
	 */
	@Override
	public boolean isPromptAnswered() {
		return(runs >= minRuns);
	}

	/**
	 * Returns the JSONObject that it has created from the values Bundled in
	 * the return of the remote Activity if the remote Activity has been run
	 * at least the minimum number of times. If not, it returns null as an
	 * indicator that the prompt isn't sufficiently "answered".
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
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please launch the remote Activity at least " + (minRuns - runs) + " more times.");
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
			if((retries + 1 - runs) > 0)
			{	
				launchActivity();
				
				if((retries + 1 - runs) <= 0)
				{
					launchButton.setVisibility(View.GONE);
				}
			}
			else
			{
				launchButton.setVisibility(View.GONE);
			}
		}
		else if(!autolaunch)
		{
			launchButton.setText("Relaunch");
			
			if((retries + 1 - runs) <= 0)
			{
				launchButton.setVisibility(View.GONE);
			}
			
			launchActivity();
		}
		else
		{
			Log.e(TAG, "Autolaunch is turned on, but I received a click on the \"Replay\" button before ever launching the remote Activity.");
			launchActivity();
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
	 * Sets the minimum number of times the remote Activity must be run in
	 * order to consider this prompt "answered".
	 * 
	 * @param minRuns The minimum number of times the remote Activity must be
	 * 				  launched.
	 */
	public void setMinRuns(int minRuns)
	{
		this.minRuns = minRuns;
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
	 * Sets the input for the remote Activity that is being called.
	 * 
	 * @param input The input to be passed into the remote Activity when it is
	 * 				launched.
	 */
	public void setInput(String input)
	{
		this.input = input;
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
			activityToLaunch.setComponent(new ComponentName(packageName, activityName));
			activityToLaunch.putExtra("input", input);
			
			try {
				callingActivity.startActivityForResult(activityToLaunch, 0);
				launched = true;
				runs++;
			} catch (ActivityNotFoundException e) {
				Toast.makeText(callingActivity, "Required component is not installed", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Log.e(TAG, "The calling Activity was null.");
		}
	}
}
