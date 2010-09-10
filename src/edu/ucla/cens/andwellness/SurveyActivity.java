package edu.ucla.cens.andwellness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.andwellness.xml.datagenerator.custom.DataPointConditionEvaluator;
import org.andwellness.xml.datagenerator.model.DataPoint;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SurveyActivity extends Activity {
	
	private static final String TAG = "SurveyActivity";
	
	private TextView mSurveyTitleText;
	private ProgressBar mProgressBar;
	private TextView mPromptText;
	private FrameLayout mPromptFrame;
	private Button mPrevButton;
	private Button mSkipButton;
	private Button mNextButton;
	
	private List<Prompt> mPrompts;
	private List<PromptResponse> mResponses;
	private int mCurrentIndex;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.survey_activity);
        
        mSurveyTitleText = (TextView) findViewById(R.id.survey_title_text);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mPromptText = (TextView) findViewById(R.id.prompt_text);
        mPromptFrame = (FrameLayout) findViewById(R.id.prompt_frame);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mSkipButton = (Button) findViewById(R.id.skip_button);
        mNextButton = (Button) findViewById(R.id.next_button);
        
        mPrevButton.setOnClickListener(mClickListener);
        mSkipButton.setOnClickListener(mClickListener);
        mNextButton.setOnClickListener(mClickListener);
        
        mPrompts = null;
        
        try {
			mPrompts = PromptXmlParser.parse(getResources().openRawResource(R.raw.chipts_general_feeling));
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mResponses = new ArrayList<PromptResponse>(mPrompts.size());
		
		mCurrentIndex = 0;
		
		showPrompt(mCurrentIndex);
		
		mProgressBar.setProgress(0);
    }
	
	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			switch (v.getId()) {
			case R.id.next_button:
				Log.i(TAG, mPrompts.get(mCurrentIndex).getResponseJson());
				if (((AbstractPrompt)mPrompts.get(mCurrentIndex)).getResponseString() == null) {
					Toast.makeText(SurveyActivity.this, "You must respond to this question before proceding.", Toast.LENGTH_SHORT).show();
				} else {
					while (mCurrentIndex + 1 < mPrompts.size()) {
						mCurrentIndex++;
						if (DataPointConditionEvaluator.evaluateCondition(((AbstractPrompt)mPrompts.get(mCurrentIndex)).getCondition(), getPreviousResponses())) {
							showPrompt(mCurrentIndex);
							break;
						} else {
							((AbstractPrompt)mPrompts.get(mCurrentIndex)).setDisplayed(false);
						}
					}
				}
				break;
			
			case R.id.skip_button:
				((AbstractPrompt)mPrompts.get(mCurrentIndex)).setSkipped(true);
				while (mCurrentIndex + 1 < mPrompts.size()) {
					mCurrentIndex++;
					if (DataPointConditionEvaluator.evaluateCondition(((AbstractPrompt)mPrompts.get(mCurrentIndex)).getCondition(), getPreviousResponses())) {
						showPrompt(mCurrentIndex);
						break;
					} else {
						((AbstractPrompt)mPrompts.get(mCurrentIndex)).setDisplayed(false);
					}
				}
				break;
				
			case R.id.prev_button:
				while (mCurrentIndex > 0) {
					mCurrentIndex--;
					if (DataPointConditionEvaluator.evaluateCondition(((AbstractPrompt)mPrompts.get(mCurrentIndex)).getCondition(), getPreviousResponses())) {
						showPrompt(mCurrentIndex);
						break;
					} else {
						((AbstractPrompt)mPrompts.get(mCurrentIndex)).setDisplayed(false);
					}
				}
				break;
			}
			
		}
	};

	private void showPrompt(int index) {
		
		// someone needs to check condition before showing prompt
				
		((AbstractPrompt)mPrompts.get(index)).setDisplayed(true);
		((AbstractPrompt)mPrompts.get(index)).setSkipped(false);
		
		// TODO for now I'm casting, but maybe I should move getters/setters to interface?
		// or just use a list of AbstractPrompt
		mPromptText.setText(((AbstractPrompt)mPrompts.get(index)).getPromptText());
		mProgressBar.setProgress(index * 100 / mPrompts.size());
		
		if (((AbstractPrompt)mPrompts.get(index)).getSkippable().equals("true")) {
			mSkipButton.setVisibility(View.VISIBLE);
		} else {
			mSkipButton.setVisibility(View.INVISIBLE);
		}
		
		mPromptFrame.removeAllViews();
		mPromptFrame.addView(mPrompts.get(index).getView(this));
		//mPromptFrame.invalidate();
	}
	
	/*public void setResponse(int index, String id, String value) {
		// prompt doesn't know it's own index... :(
		mResponses.set(index, new PromptResponse(id, value));
	}*/
	
	private List<DataPoint> getPreviousResponses() {
		ArrayList<DataPoint> previousResponses = new ArrayList<DataPoint>();
		for (int i = 0; i < mCurrentIndex; i++) {
			DataPoint dataPoint = new DataPoint(((AbstractPrompt)mPrompts.get(i)).getId());
			dataPoint.setDisplayType(((AbstractPrompt)mPrompts.get(i)).getDisplayType());
			//dataPoint.setId();
			if (mPrompts.get(i) instanceof SingleChoicePrompt)
				dataPoint.setPromptType("single_choice");
			if (((AbstractPrompt)mPrompts.get(i)).isSkipped()) {
				dataPoint.setSkipped();
			} else if (!((AbstractPrompt)mPrompts.get(i)).isDisplayed()) { 
				dataPoint.setNotDisplayed();
			} else {
				dataPoint.setValue(Integer.decode(mPrompts.get(i).getResponseString()));
			}
			previousResponses.add(dataPoint);
		}
		return previousResponses;
	}
}
