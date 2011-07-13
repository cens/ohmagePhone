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
package edu.ucla.cens.andwellness.prompt.singlechoicecustom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.activity.SurveyActivity;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
import edu.ucla.cens.systemlog.Log;

public class SingleChoiceCustomPrompt extends AbstractPrompt {
	
	private static final String TAG = "SingleChoiceCustomPrompt";
	
	private List<KVLTriplet> mChoices;
	private List<KVLTriplet> mCustomChoices;
	private int mSelectedIndex;
	
	public SingleChoiceCustomPrompt() {
		super();
		mSelectedIndex = -1;
		mCustomChoices = new ArrayList<KVLTriplet>();
		mEnteredText = "";
		mIsAddingNewItem = false;
	}
	
	public void setChoices(List<KVLTriplet> choices) {
		mChoices = choices;
	}
	
	/**
	 * Returns true if the selected index falls within the range of possible
	 * indices within either the preset list of items or the new list of items.
	 */
	@Override
	public boolean isPromptAnswered() {
		return((mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) ||
			   (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size() + mCustomChoices.size()));
	}
	
	@Override
	protected Object getTypeSpecificResponseObject() {
		
		if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) {
			return Integer.decode(mChoices.get(mSelectedIndex).key);
		} else if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size() + mCustomChoices.size()) {
			return Integer.decode(mCustomChoices.get(mSelectedIndex - mChoices.size()).key);
		} else {
			return null;
		}
	}
	
	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please select an item or add your own.");
	}
	
	@Override
	protected void clearTypeSpecificResponseData() {
		mSelectedIndex = -1;
	}
	
	@Override
	protected Object getTypeSpecificExtrasObject() {
		JSONArray jsonArray = new JSONArray();
		for (KVLTriplet choice : mChoices) {
			JSONObject jsonChoice = new JSONObject();
			try {
				jsonChoice.put("choice_id", Integer.decode(choice.key));
				jsonChoice.put("choice_value", choice.label);
			} catch (NumberFormatException e) {
				Log.e(TAG, "NumberFormatException when trying to parse custom choice key", e);
				return null;
			} catch (JSONException e) {
				Log.e(TAG, "JSONException when trying to generate custom_choices json", e);
				return null;
			}
			jsonArray.put(jsonChoice);
		}
		for (KVLTriplet choice : mCustomChoices) {
			JSONObject jsonChoice = new JSONObject();
			try {
				jsonChoice.put("choice_id", Integer.decode(choice.key));
				jsonChoice.put("choice_value", choice.label);
			} catch (NumberFormatException e) {
				Log.e(TAG, "NumberFormatException when trying to parse custom choice key", e);
				return null;
			} catch (JSONException e) {
				Log.e(TAG, "JSONException when trying to generate custom_choices json", e);
				return null;
			}
			jsonArray.put(jsonChoice);
		}
		return jsonArray;
	}
	
	private View mFooterView;
	private ListView mListView;
	private boolean mIsAddingNewItem;
	private String mEnteredText;
	private int mLastIndex;
	private int mLastTop;

	@Override
	public View getView(final Context context) {
		
		mCustomChoices.clear();
		SingleChoiceCustomDbAdapter dbAdapter = new SingleChoiceCustomDbAdapter(context);
		String surveyId = ((SurveyActivity)context).getSurveyId();
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
		String campaignUrn = ((SurveyActivity)context).getCampaignUrn();
		String username = prefs.getUsername();
		if (dbAdapter.open()) {
			Cursor c = dbAdapter.getCustomChoices(username, campaignUrn, surveyId, SingleChoiceCustomPrompt.this.getId());
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				//c.getLong(c.getColumnIndex(SingleChoiceCustomDbAdapter.KEY_ID));
				int key = c.getInt(c.getColumnIndex(SingleChoiceCustomDbAdapter.KEY_CHOICE_ID));
				String label = c.getString(c.getColumnIndex(SingleChoiceCustomDbAdapter.KEY_CHOICE_VALUE));
				mCustomChoices.add(new KVLTriplet(String.valueOf(key), null, label));
				c.moveToNext();
			}
			c.close();
			dbAdapter.close();
		}
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = (ListView) inflater.inflate(R.layout.prompt_single_choice_custom, null);
		
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mListView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		
		mFooterView = inflater.inflate(R.layout.custom_choice_footer, null);
		
		EditText mEditText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
		ImageButton mButton = (ImageButton) mFooterView.findViewById(R.id.ok_button);
		ImageButton mCancelButton = (ImageButton) mFooterView.findViewById(R.id.cancel_button);
        
        showAddItemControls(context, mIsAddingNewItem);
        
        mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (mEnteredText != null && !mEnteredText.equals("")) {
					SingleChoiceCustomDbAdapter dbAdapter = new SingleChoiceCustomDbAdapter(context);
					String surveyId = ((SurveyActivity)context).getSurveyId();
					SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
					String campaignUrn = ((SurveyActivity)context).getCampaignUrn();
					String username = prefs.getUsername();
					
					int choiceId = 100;
					ArrayList<String> keys = new ArrayList<String>(); 
					for (KVLTriplet choice : mChoices) {
						keys.add(choice.key.trim());
					}
					for (KVLTriplet choice : mCustomChoices) {
						keys.add(choice.key.trim());
					}
					while ( keys.contains(String.valueOf(choiceId))) {
						choiceId++;
					}
					
					if (dbAdapter.open()) {
						dbAdapter.addCustomChoice(choiceId, mEnteredText, username, campaignUrn, surveyId, SingleChoiceCustomPrompt.this.getId());
						dbAdapter.close();
					}
					
					showAddItemControls(context, false);
					
					mSelectedIndex = mListView.getCount() - 1;
					
					((SurveyActivity)context).reloadCurrentPrompt();
				} else {
					Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        mCancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				EditText edit = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
				showAddItemControls(context, false);	
			}
		});
        
        mEditText.addTextChangedListener(new TextWatcher() {
			
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
				// TODO Auto-generated method stub
				mEnteredText = s.toString();
//				mListView.setSelectionFromTop(mLastIndex, mLastTop);
//				EditText editText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
//				editText.requestFocus();
			}
		});
        
		mListView.addFooterView(mFooterView);
		
		String [] from = new String [] {"value"};
		int [] to = new int [] {android.R.id.text1};
		
		List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < mChoices.size(); i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("key", mChoices.get(i).key);
			map.put("value", mChoices.get(i).label);
			data.add(map);
		}
		for (int i = 0; i < mCustomChoices.size(); i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("key", mCustomChoices.get(i).key);
			map.put("value", mCustomChoices.get(i).label);
			data.add(map);
		}
		
		SimpleAdapter adapter = new SimpleAdapter(context, data, R.layout.single_choice_list_item, from, to);
		
		adapter.setViewBinder(new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				((CheckedTextView) view).setText((String) data);
				return true;
			}
		});
		
		mListView.setAdapter(adapter);
		
		if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size() + mCustomChoices.size()) {
			mListView.setItemChecked(mSelectedIndex, true);
		}
		
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				if (position == mListView.getCount() - 1) {
					showAddItemControls(context, true);
					mSelectedIndex = -1;
					mLastIndex = mListView.getLastVisiblePosition();
					View v = mListView.getChildAt(mLastIndex);
					mLastTop = (v == null) ? 0 : v.getTop();
					mListView.setSelectionFromTop(mLastIndex, mLastTop);
				} else {
					//Map<String, String> map = (HashMap<String, String>) parent.getItemAtPosition(position);
					//mSelectedKey = map.get("key");
					mSelectedIndex = position;
					//((SurveyActivity)context).setResponse(index, id, value)
				}
			}
		});
		
		mListView.setSelectionFromTop(mLastIndex, mLastTop);
		
		return mListView;
	}
	
	private void showAddItemControls(Context context, boolean show) {
		ImageView imageView = (ImageView) mFooterView.findViewById(R.id.image);
		TextView textView = (TextView) mFooterView.findViewById(R.id.text);
		EditText editText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
		ImageButton mButton = (ImageButton) mFooterView.findViewById(R.id.ok_button);
		ImageButton mCancelButton = (ImageButton) mFooterView.findViewById(R.id.cancel_button);
        
        //editText.setText("");
        
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        
        if (show) {
        	editText.setText(mEnteredText);
			mIsAddingNewItem = true;
        	imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            editText.requestFocus();
            imm.showSoftInput(editText, 0);
        } else {
        	mEnteredText = "";
			mIsAddingNewItem = false;
        	imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
            mButton.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
	}
}
