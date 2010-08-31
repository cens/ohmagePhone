package edu.ucla.cens.andwellness;

import java.io.IOException;
import java.util.List;

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
		
		mCurrentIndex = 0;
		
		showCurrentPrompt();
		
		mProgressBar.setProgress(0);
    }
	
	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			switch (v.getId()) {
			case R.id.next_button:
				Log.i(TAG, mPrompts.get(mCurrentIndex).getResponseJson());
				if (mCurrentIndex + 1 < mPrompts.size()) {
					mCurrentIndex++;
					showCurrentPrompt();
				}
				break;
			
			case R.id.skip_button:
				
				break;
				
			case R.id.prev_button:
				if (mCurrentIndex > 0) {
					mCurrentIndex--;
					showCurrentPrompt();
				}
				break;
			}
			
		}
	};

	private void showCurrentPrompt() {
		
		// TODO for now I'm casting, but maybe I should move to interface
		mPromptText.setText(((AbstractPrompt)mPrompts.get(mCurrentIndex)).getPromptText());
		mProgressBar.setProgress(mCurrentIndex * 100 / mPrompts.size());
		
		if (((AbstractPrompt)mPrompts.get(mCurrentIndex)).getSkippable().equals("true")) {
			mSkipButton.setVisibility(View.INVISIBLE);
		} else {
			mSkipButton.setVisibility(View.VISIBLE);
		}
		
		mPromptFrame.removeAllViews();
		mPromptFrame.addView(mPrompts.get(mCurrentIndex).getView(this));
		//mPromptFrame.invalidate();
	}
}
