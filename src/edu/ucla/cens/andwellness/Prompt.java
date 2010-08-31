package edu.ucla.cens.andwellness;

import android.content.Context;
import android.view.View;

public interface Prompt {

	// TODO document these!
	// move getters into interface?
	
	View getView(Context context);

	String getResponseJson();

	String getResponseValue();

}