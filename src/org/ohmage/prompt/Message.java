package org.ohmage.prompt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.ohmage.R;

public class Message implements SurveyElement, Displayable {
	
	private String mMessageText;
	private String mCondition;
	
	public Message(String messageText, String condition) {
		mMessageText = messageText;
		mCondition = condition;
	}

	public String getMessageText() {
		return mMessageText;
	}

	public void setMessageText(String messageText) {
		this.mMessageText = messageText;
	}

	public String getCondition() {
		return mCondition;
	}

	public void setCondition(String condition) {
		this.mCondition = condition;
	}

	@Override
	public View inflateView(Context context, ViewGroup parent) {
		// TODO Auto-generated method stub
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.message, parent);
		TextView messageText = (TextView) layout.findViewById(R.id.message_text);
		messageText.setText(mMessageText);
		
		return layout;
	}
}
