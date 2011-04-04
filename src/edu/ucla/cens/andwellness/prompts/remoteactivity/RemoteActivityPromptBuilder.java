package edu.ucla.cens.andwellness.prompts.remoteactivity;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class RemoteActivityPromptBuilder implements PromptBuilder
{
	public static final String PACKAGE_ID = "package";
	public static final String ACTIVITY_ID = "activity";
	public static final String ACTION_ID = "action";
	public static final String AUTOLAUNCH_ID = "autolaunch";
	public static final String NUM_RETRIES_ID = "retries";
	
	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties)
	{
		RemoteActivityPrompt remoteActivityPrompt = (RemoteActivityPrompt) prompt;
		remoteActivityPrompt.setId(id);
		remoteActivityPrompt.setDisplayType(displayType);
		remoteActivityPrompt.setDisplayLabel(displayLabel);
		remoteActivityPrompt.setPromptText(promptText);
		remoteActivityPrompt.setAbbreviatedText(abbreviatedText);
		remoteActivityPrompt.setExplanationText(explanationText);
		remoteActivityPrompt.setDefaultValue(defaultValue);
		remoteActivityPrompt.setCondition(condition);
		remoteActivityPrompt.setSkippable(skippable);
		remoteActivityPrompt.setSkipLabel(skipLabel);
		
		for(KVLTriplet property : properties)
		{
			if(property.key.equals(PACKAGE_ID))
			{
				remoteActivityPrompt.setPackage(property.label);
			}
			else if(property.key.equals(ACTIVITY_ID))
			{
				remoteActivityPrompt.setActivity(property.label);
			}
			else if(property.key.equals(ACTION_ID))
			{
				remoteActivityPrompt.setAction(property.label);
			}
			else if(property.key.equals(AUTOLAUNCH_ID))
			{
				remoteActivityPrompt.setAutolaunch(property.label.equalsIgnoreCase("true"));
			}
			else if(property.key.equals(NUM_RETRIES_ID))
			{
				remoteActivityPrompt.setRetries(Integer.parseInt(property.label));
			}
		}

		remoteActivityPrompt.clearTypeSpecificResponseData();
	}
}