package edu.ucla.cens.andwellness.prompt;

import android.content.Context;
import android.content.Intent;
import android.view.View;

public interface Prompt {

	// TODO document these!
	// move getters into interface?
	
	View getView(Context context);

	String getResponseJson();
	String getUnansweredPromptText();
	boolean isPromptAnswered();

	Object getResponseObject();
	
	void handleActivityResult(Context context, int requestCode, int resultCode, Intent data);

}