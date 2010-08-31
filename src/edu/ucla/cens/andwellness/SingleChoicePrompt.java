package edu.ucla.cens.andwellness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.cens.andwellness.Utilities.KVPair;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter.ViewBinder;

public class SingleChoicePrompt extends AbstractPrompt {
	
	private static final String TAG = "SingleChoicePrompt";
	
	private List<KVPair> mChoices;
	//private String mSelectedKey;
	private int mSelectedIndex;
	
	public SingleChoicePrompt() {
		super();
		mSelectedIndex = -1;
	}
	
	public void setChoices(List<KVPair> choices) {
		mChoices = choices;
	}
	
	/*public SingleChoicePrompt( 	String id, String displayType, String displayLabel,
								String promptText, String abbreviatedText, String explanationText,
								String defaultValue, String condition, 
								String skippable, String skipLabel,
								List<KVPair> properties) {
		
		super(id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel);
		
		mKVPairs = properties;
		//mSelectedKey = "RESPONSE_SKIPPED";
		mSelectedIndex = -1;
	}*/

	@Override
	public String getResponseJson() {
		
		JSONObject responseJson = new JSONObject();
		try {
			responseJson.put("prompt_id", this.getId());
			//responseJson.put("value", mSelectedKey);
			responseJson.put("value", getResponseValue());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseJson.toString();
	}
	
	@Override
	public String getResponseValue() {
		//return mSelectedKey;
		if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) {
			return mChoices.get(mSelectedIndex).key;
		} else {
			return "RESPONSE_SKIPPED";
		}
	}

	@Override
	public View getView(Context context) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ListView listView = (ListView) inflater.inflate(R.layout.prompt_single_choice, null);
		
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		String [] from = new String [] {"value"};
		int [] to = new int [] {android.R.id.text1};
		
		List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < mChoices.size(); i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("key", mChoices.get(i).key);
			map.put("value", mChoices.get(i).value);
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
		
		listView.setAdapter(adapter);
		
		if (mSelectedIndex >= 0 && mSelectedIndex < listView.getCount()) {
			listView.setItemChecked(mSelectedIndex, true);
		}
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				//Map<String, String> map = (HashMap<String, String>) parent.getItemAtPosition(position);
				//mSelectedKey = map.get("key");
				mSelectedIndex = position;
			}
		});
		
		return listView;
	}

}
