package org.ohmage.prompt;

import org.ohmage.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
	public View getView(Context context) {
		// TODO Auto-generated method stub
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.message, null);
		TextView messageText = (TextView) layout.findViewById(R.id.message_text);
		messageText.setText(mMessageText);
		
		return layout;
	}

}
