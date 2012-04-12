/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.activity;


import edu.ucla.cens.systemlog.Log;

import org.ohmage.NIHConfig;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Displays a post survey summary which highlights the positive aspects of the
 * last response. This activity will be called by the {@link SurveyActivity}. It
 * first shows a progress dialog while trying to find if there is any positive
 * feedback to show. Then it shows a pop up with a gold star and the positive
 * feedback message.
 * 
 * @author cketcham
 */
public class PostSurveyActivity extends FragmentActivity {
	private static final String TAG = "PostSurveyActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getIntent().getData() == null) {
			Log.e(TAG, "PostSurveyActivity called without a response uri");
			finish();
			return;
		}

		if(savedInstanceState == null) {
			CheckFeedbackDialogFragment.newInstance(ContentUris.parseId(getIntent().getData())).show(getSupportFragmentManager(), "dialog");
		}
	}

	/**
	 * Compares the value in this response with the baseline values to see if
	 * feedback should be shown or not
	 * 
	 * @author cketcham
	 */
	public static class CheckFeedbackDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
		private static final String TAG = "CheckFeedbackDialogFragment";

		private static final String EXTRA_RESPONSE_URI = "extra_response_uri";

		private static final int LOADER_BASELINE_ID = 0;

		private static final int LOADER_RESPONSE_ID = 1;

		Handler handler = new Handler();

		private Cursor mBaselineData;
		private Cursor mResponseData;

		public static CheckFeedbackDialogFragment newInstance(long responseId) {
			CheckFeedbackDialogFragment f = new CheckFeedbackDialogFragment();
			Bundle args = new Bundle();
			args.putLong(EXTRA_RESPONSE_URI, responseId);
			f.setArguments(args);
			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			getLoaderManager().initLoader(LOADER_BASELINE_ID, null, this);
			getLoaderManager().initLoader(LOADER_RESPONSE_ID, null, this);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			ProgressDialog dialog = new ProgressDialog(getActivity());
			dialog.setIndeterminate(true);
			dialog.setMessage("Saving Response");
			dialog.setCancelable(false);
			return dialog;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
			switch(id) {
				case LOADER_BASELINE_ID:
					return new CursorLoader(getActivity(),
							PromptResponses.buildPromptsAggregatesUri(Campaign.getSingleCampaign(getActivity())),
							new String[] {
						PromptResponses.PROMPT_ID, PromptResponses.AggregateTypes.SUM.toString(),
						PromptResponses.AggregateTypes.COUNT.toString()
					},
					Responses.RESPONSE_TIME + " < " + UserPreferencesHelper.getBaseLineEndTime(getActivity()),
					null, null);
				case LOADER_RESPONSE_ID:
					return new CursorLoader(getActivity(),
							Responses.buildPromptResponsesUri(getArguments().getLong(EXTRA_RESPONSE_URI)),
							null, PromptResponses.PROMPT_RESPONSE_VALUE + " !=? AND " + PromptResponses.PROMPT_RESPONSE_VALUE + " !=?", new String[] { "NOT_DISPLAYED", "SKIPPED" }, null);
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

			switch(loader.getId()) {
				case LOADER_BASELINE_ID:
					mBaselineData = data;
					break;
				case LOADER_RESPONSE_ID:
					mResponseData = data;
					break;
			}

			if(mBaselineData == null || mResponseData == null)
				return;

			if (mResponseData.getCount() == 0) {
				Log.e(TAG, "PostSurveyActivity called with a response that does not exist");
				getActivity().finish();
				return;
			}

			HashMap<String, Double> baseLine = calculateBaseLineValues(mBaselineData);

			int promptIdIdx = mResponseData.getColumnIndex(PromptResponses.PROMPT_ID);
			int promptValueIdx = mResponseData.getColumnIndex(PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE);
			final StringBuilder goodJob = new StringBuilder();

			while(mResponseData.moveToNext()) {
				String promptId = mResponseData.getString(promptIdIdx);
				// If we have a baseline value for this prompt, we can try to compare it to the response
				if(baseLine.containsKey(NIHConfig.getPrompt(promptId))) {
					ExtraPromptData extraData = NIHConfig.getExtraPromptData(promptId);
					Double responseValue = mResponseData.getDouble(promptValueIdx);
					Double baselineValue = baseLine.get(NIHConfig.getPrompt(promptId));
					if(extraData != null && extraData.compare(responseValue, baselineValue) <= 0) {
						if(goodJob.length() != 0)
							goodJob.append("<br/>");
						goodJob.append("&bull; ");
						goodJob.append(extraData.goodJobString());
					}
				}
			}

			// If there is feedback to show
			if(goodJob.length() != 0) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						dismiss();
						PostSurveyFeedbackFragment.newInstance(goodJob.toString()).show(getFragmentManager(), "feedback");
					}
				});
			} else {
				// Otherwise finish
				getActivity().finish();
			}

			mBaselineData = null;
			mResponseData = null;
		}


		private HashMap<String, Double> calculateBaseLineValues(Cursor c) {

			HashMap<String, Double> sum = new HashMap<String, Double>();
			HashMap<String, Double> counts = new HashMap<String, Double>();

			while(c.moveToNext()) {
				String prompt = NIHConfig.getPrompt(c.getString(0));
				if(prompt != null) {
					Double s = sum.get(prompt);
					if(s == null) {
						s = 0.0;
					}
					Double count = counts.get(prompt);
					if(count == null) {
						count = 0.0;
					}

					s += c.getDouble(1);
					count += c.getDouble(2);

					sum.put(prompt, s);
					counts.put(prompt, count);
				}
			}
			c.close();

			HashMap<String, Double> baseLine = new HashMap<String, Double>();
			for(String k : sum.keySet()) {
				baseLine.put(k, sum.get(k) / counts.get(k));
			}
			return baseLine;
		}


		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// Nothing to do
		}
	}

	/**
	 * Fragment to show the feedback provided by the {@link CheckFeedbackDialogFragment}
	 * @author cketcham
	 *
	 */
	public static class PostSurveyFeedbackFragment extends DialogFragment {

		private static final String EXTRA_FEEDBACK_TEXT = "extra_feedback_text";

		public static PostSurveyFeedbackFragment newInstance(String text) {
			PostSurveyFeedbackFragment f = new PostSurveyFeedbackFragment();
			f.setStyle(STYLE_NO_TITLE, 0);
			Bundle args = new Bundle();
			args.putString(EXTRA_FEEDBACK_TEXT, text);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.post_survey_layout, container, false);

			TextView summary = (TextView) view.findViewById(R.id.summary_text);
			summary.setText(Html.fromHtml(getArguments().getString(EXTRA_FEEDBACK_TEXT)));

			Button cont = (Button) view.findViewById(R.id.continue_button);
			cont.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					dismiss();
					getActivity().finish();
				}
			});

			return view;
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			super.onDismiss(dialog);
			getActivity().finish();
		}
	}
}