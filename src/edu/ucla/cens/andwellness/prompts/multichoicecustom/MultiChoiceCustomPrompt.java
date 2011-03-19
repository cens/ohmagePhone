package edu.ucla.cens.andwellness.prompts.multichoicecustom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.Utilities;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.Utilities.KVPair;
import edu.ucla.cens.andwellness.prompts.AbstractPrompt;

import android.content.Context;
import android.content.Intent;
//import android.util.Log;
import edu.ucla.cens.systemlog.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter.ViewBinder;

public class MultiChoiceCustomPrompt extends AbstractPrompt {
	
private static final String TAG = "MultiChoiceCustomPrompt";
	
	private List<KVLTriplet> mChoices;
	private ArrayList<Integer> mSelectedIndexes;
	
	public MultiChoiceCustomPrompt() {
		super();
		mSelectedIndexes = new ArrayList<Integer>();
	}
	
	public void setChoices(List<KVLTriplet> choices) {
		mChoices = choices;
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		if (mSelectedIndexes == null) {
			mSelectedIndexes = new ArrayList<Integer>();
		} else {
			mSelectedIndexes.clear();
		}
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		JSONArray jsonArray = new JSONArray();
		for (int index : mSelectedIndexes) {
			if (index >= 0 && index < mChoices.size())
				jsonArray.put(Integer.decode(mChoices.get(index).key));
		}
		return jsonArray;
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
		return jsonArray;
	}

	@Override
	public View getView(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ListView listView = (ListView) inflater.inflate(R.layout.prompt_multi_choice, null);
		
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		String [] from = new String [] {"value"};
		int [] to = new int [] {android.R.id.text1};
		
		List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < mChoices.size(); i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("key", mChoices.get(i).key);
			map.put("value", mChoices.get(i).label);
			data.add(map);
		}
		
		SimpleAdapter adapter = new SimpleAdapter(context, data, R.layout.multi_choice_list_item, from, to);
		
		adapter.setViewBinder(new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				((CheckedTextView) view).setText((String) data);
				return true;
			}
		});
		
		listView.setAdapter(adapter);
		
		if (mSelectedIndexes.size() > 0) {
			for (int index : mSelectedIndexes) {
				if (index >= 0 && index < mChoices.size())
					listView.setItemChecked(index, true);
			}
		}
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				//SparseBooleanArray checkItemPositions = ((ListView)parent).getCheckedItemPositions();
				if (((ListView)parent).isItemChecked(position)) {
					mSelectedIndexes.add(Integer.valueOf(position));
				} else {
					mSelectedIndexes.remove(Integer.valueOf(position));
				}
			}
		});
		
		return listView;
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
	}

}
