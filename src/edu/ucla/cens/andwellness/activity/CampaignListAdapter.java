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
package edu.ucla.cens.andwellness.activity;

import java.util.List;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.db.Campaign;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CampaignListAdapter extends BaseAdapter {
	
	private Context mContext;
	private int mItemsLayoutId;
	private int mHeaderLayoutId;
	private List<Campaign> mAvailable;
	private List<Campaign> mUnavailable;
	private LayoutInflater mInflater;
	
	private static final int VIEW_ITEM = 0;
	private static final int VIEW_HEADER = 1;
	
	public static final int GROUP_AVAILABLE = 0;
	public static final int GROUP_UNAVAILABLE = 1;
	
	public CampaignListAdapter(Context context, List<Campaign> available, List<Campaign> unavailable, int itemLayoutResource, int headerLayoutResource) {
		mContext = context;
		mItemsLayoutId = itemLayoutResource;
		mHeaderLayoutId = headerLayoutResource;
		mAvailable = available;
		mUnavailable = unavailable;
				
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		int count = 0;
		count += mAvailable.size();
		count += mAvailable.size() > 0 ? 1 : 0;
		count += mUnavailable.size();
		count += mUnavailable.size() > 0 ? 1 : 0;
		return count;
	}

	@Override
	public Campaign getItem(int position) {
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (getItemViewType(position) == VIEW_ITEM) {
			switch (getItemGroup(position)) {
			case GROUP_AVAILABLE:
				if (position > 0 && position < sizeOfAvailableSection) {
					return mAvailable.get(position - 1);
				}
				break;
				
			case GROUP_UNAVAILABLE:
				if (position - sizeOfAvailableSection < sizeOfUnavailableSection) {
					return mUnavailable.get(position - sizeOfAvailableSection - 1);
				}
				break;
				
			}
		}
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public int getItemGroup(int position) {
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0) {
			if (sizeOfAvailableSection > 0) {
				return GROUP_AVAILABLE;
			} else if (sizeOfUnavailableSection > 0) {
				return GROUP_UNAVAILABLE;
			}
		} else if (position < sizeOfAvailableSection) {
			return GROUP_AVAILABLE;
		} else if (position == sizeOfAvailableSection) {
			if (sizeOfUnavailableSection > 0) {
				return GROUP_UNAVAILABLE;
			}
		} else if (position - sizeOfAvailableSection < sizeOfUnavailableSection) {
			return GROUP_UNAVAILABLE;
		} else if (position - sizeOfAvailableSection == sizeOfUnavailableSection) {
			return GROUP_UNAVAILABLE;
		} else {
			throw new IndexOutOfBoundsException();
		}
		return -1;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0 || position == sizeOfAvailableSection || position - sizeOfAvailableSection == sizeOfUnavailableSection) {
			return VIEW_HEADER;
		} else {
			return VIEW_ITEM;
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		
		if (getItemViewType(position) == VIEW_HEADER) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void notifyDataSetChanged() {		
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (getItemViewType(position) == VIEW_ITEM) {
			ItemViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mItemsLayoutId, parent, false);
				holder = new ItemViewHolder();
				holder.titleText = (TextView) convertView.findViewById(R.id.text1);
				holder.subText = (TextView) convertView.findViewById(R.id.text2);
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				convertView.setTag(holder);
			} else {
				holder = (ItemViewHolder) convertView.getTag();
			}
			
			switch (getItemGroup(position)) {

			case GROUP_AVAILABLE:
				//holder.titleText.setTextColor(Color.BLACK);
				//holder.image.setImageResource(R.drawable.arrow_right);
				holder.image.setVisibility(View.GONE);
				break;
				
			case GROUP_UNAVAILABLE:
				//holder.titleText.setTextColor(Color.GRAY);
				holder.image.setImageResource(R.drawable.add_new);
				holder.image.setVisibility(View.VISIBLE);
				break;
			}
			
			holder.titleText.setText(getItem(position).mName);
			holder.subText.setText(getItem(position).mUrn);

		} else {
			HeaderViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mHeaderLayoutId, parent, false);
				holder = new HeaderViewHolder();
				holder.headerText = (TextView) convertView.findViewById(R.id.text);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			
			switch (getItemGroup(position)) {

			case GROUP_AVAILABLE:
//				convertView.setBackgroundColor(Color.rgb(1, 173, 73));
				holder.headerText.setText("My Campaigns");
				break;
				
			case GROUP_UNAVAILABLE:
//				convertView.setBackgroundColor(Color.rgb(1, 173, 73));
				holder.headerText.setText("Available Campaigns");
				break;
			}
		}
		
		return convertView;
	}

	static class ItemViewHolder {
		TextView titleText;
		TextView subText;
		ImageView image;
	}
	
	static class HeaderViewHolder {
		TextView headerText;
	}
}
