package edu.ucla.cens.andwellness.prompts.text;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.R.id;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.prompts.AbstractPrompt;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

public class TextPrompt extends AbstractPrompt {
	
	private String mText;

	public TextPrompt() {
		super();
		mText = "";
	}
	
	@Override
	protected void clearTypeSpecificResponseData() {
		if (mDefaultValue != null) {
			mText = getDefaultValue();
		} else {
			mText = "";
		}
	}

	@Override
	protected String getTypeSpecificResponseObject() {
		if (mText.equals("")) {
			return null;
		}
		else {
			return mText;
		}
	}

	@Override
	public View getView(Context context) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.prompt_text, null);
		
		EditText editText = (EditText) layout.findViewById(R.id.text);
		
		editText.setText(mText);
		
		editText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				mText = s.toString();
			}
		});
		
		return layout;
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
	}

}
