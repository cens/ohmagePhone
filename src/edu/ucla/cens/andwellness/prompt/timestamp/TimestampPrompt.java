package edu.ucla.cens.andwellness.prompt.timestamp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;

public class TimestampPrompt extends AbstractPrompt {

	@Override
	public String getUnansweredPromptText() {
		return "Please enter a valid time.";
	}

	@Override
	public boolean isPromptAnswered() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub

	}

	@Override
	public View getView(Context context) {
		// TODO Auto-generated method stub
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.submit, null);
		TextView submitText = (TextView) layout.findViewById(R.id.submit_text);
		submitText.setText("Timestamp prompt type is not supported in this version. Response value will be set to NOT_DISPLAYED.");
		
		return layout;
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		// TODO Auto-generated method stub
		return "NOT_DISPLAYED";
	}

	@Override
	protected Object getTypeSpecificExtrasObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		// TODO Auto-generated method stub

	}

}
