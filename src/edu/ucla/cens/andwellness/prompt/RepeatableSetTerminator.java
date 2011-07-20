package edu.ucla.cens.andwellness.prompt;

import edu.ucla.cens.andwellness.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RepeatableSetTerminator implements SurveyElement, Displayable {
	
	String id;
	String condition;
	String terminationQuestion;
	String trueLabel;
	String falseLabel;
	String skipLabel;
	boolean skipEnabled;
	int promptCount;
	
	public RepeatableSetTerminator(String id, String condition, String terminationQuestion,
			String trueLabel, String falseLabel, String skipLabel,
			String skipEnabled, int promptCount) {
		this.id = id;
		this.condition = condition;
		this.terminationQuestion = terminationQuestion;
		this.trueLabel = trueLabel;
		this.falseLabel = falseLabel;
		this.skipLabel = skipLabel;
		this.skipEnabled = skipEnabled.equalsIgnoreCase("true") ? true : false;
		this.promptCount = promptCount;
	}

	@Override
	public View getView(Context context) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.submit, null);
		TextView questionText = (TextView) layout.findViewById(R.id.submit_text);
		//submitText.setText("Thank you for completing the survey!");
		questionText.setText(terminationQuestion);
		
		return layout;
	}
	
	public String getId() {
		return id;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public int getPromptCount() {
		return promptCount;
	}

	public RepeatableSetTerminator getCopy() {
		return new RepeatableSetTerminator(id, condition, terminationQuestion, trueLabel, falseLabel, skipLabel, skipEnabled ? "true" : "false", promptCount);
	}
}
