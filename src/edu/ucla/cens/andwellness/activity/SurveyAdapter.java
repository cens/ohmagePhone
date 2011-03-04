package edu.ucla.cens.andwellness.activity;

import java.util.ArrayList;
import java.util.List;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.Survey;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SurveyAdapter extends BaseAdapter {
	
	private Context mContext;
	private int mSurveyLayoutId;
	private int mHeaderLayoutId;
	private List<Survey> mSurveys;
	private List<String> mActiveSurveyTitles;
	private List<Survey> mPending;
	private List<Survey> mAvailable;
	private List<Survey> mUnavailable;
	private LayoutInflater mInflater;
	
	private static final int VIEW_SURVEY = 0;
	private static final int VIEW_HEADER = 1;
	
	private static final int GROUP_PENDING = 0;
	private static final int GROUP_AVAILABLE = 1;
	private static final int GROUP_UNAVAILABLE = 2;
	
	public SurveyAdapter(Context context, List<Survey> surveys, List<String> activeSurveyTitles, int surveyLayoutResource, int headerLayoutResource) {
		mContext = context;
		mSurveyLayoutId = surveyLayoutResource;
		mHeaderLayoutId = headerLayoutResource;
		mSurveys = surveys;
		mActiveSurveyTitles = activeSurveyTitles;
		mPending = new ArrayList<Survey>();
		mAvailable = new ArrayList<Survey>();
		mUnavailable = new ArrayList<Survey>();
		
		updateStates();
				
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		int count = 0;
		count += mSurveys.size();
		count += mPending.size() > 0 ? 1 : 0;
		count += mAvailable.size() > 0 ? 1 : 0;
		count += mUnavailable.size() > 0 ? 1 : 0;
		return count;
	}

	@Override
	public Object getItem(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (getItemViewType(position) == VIEW_SURVEY) {
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				if (position > 0 && position < sizeOfPendingSection) {
					return mPending.get(position - 1);
				}
				break;
				
			case GROUP_AVAILABLE:
				if (position - sizeOfPendingSection < sizeOfAvailableSection) {
					return mAvailable.get(position - sizeOfPendingSection - 1);
				}
				break;
				
			case GROUP_UNAVAILABLE:
				if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
					return mUnavailable.get(position - sizeOfPendingSection - sizeOfAvailableSection - 1);
				}
				break;
			}
		}
		
		return null;
		
		/*try {
			if (position == 0) {
				if (sizeOfPendingSection > 0) {
					return GROUP_PENDING;
				} else if (sizeOfAvailableSection > 0) {
					return GROUP_AVAILABLE;
				} else {
					return GROUP_UNAVAILABLE;
				}
			} else if (position < sizeOfPendingSection) {
				return mPending.get(position - 1);
			} else if (position == sizeOfPendingSection) {
				if (sizeOfAvailableSection > 0) {
					return GROUP_AVAILABLE;
				} else {
					return GROUP_UNAVAILABLE;
				}
			} else if (position - sizeOfPendingSection < sizeOfAvailableSection) {
				return mAvailable.get(position - sizeOfPendingSection - 1);
			} else if (position - sizeOfPendingSection == sizeOfAvailableSection) {
				return GROUP_UNAVAILABLE;
			} else if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
				return mUnavailable.get(position - sizeOfPendingSection - 1);
			} else {
				throw new IndexOutOfBoundsException();
			}
		} catch (IndexOutOfBoundsException e) {
			throw e;
		}*/
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	private int getItemGroup(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0) {
			if (sizeOfPendingSection > 0) {
				return GROUP_PENDING;
			} else if (sizeOfAvailableSection > 0) {
				return GROUP_AVAILABLE;
			} else {
				return GROUP_UNAVAILABLE;
			}
		} else if (position < sizeOfPendingSection) {
			return GROUP_PENDING;
		} else if (position == sizeOfPendingSection) {
			if (sizeOfAvailableSection > 0) {
				return GROUP_AVAILABLE;
			} else {
				return GROUP_UNAVAILABLE;
			}
		} else if (position - sizeOfPendingSection < sizeOfAvailableSection) {
			return GROUP_AVAILABLE;
		} else if (position - sizeOfPendingSection == sizeOfAvailableSection) {
			return GROUP_UNAVAILABLE;
		} else if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
			return GROUP_UNAVAILABLE;
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0 || position == sizeOfPendingSection || position - sizeOfPendingSection == sizeOfAvailableSection) {
			return VIEW_HEADER;
		} else {
			return VIEW_SURVEY;
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
		
		updateStates();
		
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (getItemViewType(position) == VIEW_SURVEY) {
			SurveyViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mSurveyLayoutId, parent, false);
				holder = new SurveyViewHolder();
				holder.stateView = convertView.findViewById(R.id.survey_state_view);
				holder.titleText = (TextView) convertView.findViewById(R.id.survey_title_text);
				holder.lastTakenDateText = (TextView) convertView.findViewById(R.id.survey_last_taken_date_text);
				holder.lastTakenTimeText = (TextView) convertView.findViewById(R.id.survey_last_taken_time_text);
				convertView.setTag(holder);
			} else {
				holder = (SurveyViewHolder) convertView.getTag();
			}
			
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				holder.stateView.setBackgroundColor(Color.rgb(1, 173, 73));
				convertView.setEnabled(true);
				holder.titleText.setTextColor(Color.BLACK);
				holder.lastTakenDateText.setTextColor(Color.DKGRAY);
				holder.lastTakenTimeText.setTextColor(Color.DKGRAY);
				break;

			case GROUP_AVAILABLE:
				holder.stateView.setBackgroundColor(Color.rgb(56, 160, 220));
				convertView.setEnabled(true);
				holder.titleText.setTextColor(Color.BLACK);
				holder.lastTakenDateText.setTextColor(Color.DKGRAY);
				holder.lastTakenTimeText.setTextColor(Color.DKGRAY);
				break;
				
			case GROUP_UNAVAILABLE:
				convertView.setEnabled(false);
				holder.stateView.setBackgroundColor(Color.LTGRAY);
				holder.titleText.setTextColor(Color.GRAY);
				holder.lastTakenDateText.setTextColor(Color.GRAY);
				holder.lastTakenTimeText.setTextColor(Color.GRAY);
				holder.stateView.setBackgroundColor(Color.LTGRAY);
				break;
			}
			
			holder.titleText.setText(((Survey)getItem(position)).getTitle());
			
			holder.lastTakenDateText.setText("Yesterday");
			holder.lastTakenTimeText.setText("9:03 AM");
			
		} else {
			HeaderViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mHeaderLayoutId, parent, false);
				holder = new HeaderViewHolder();
				holder.headerText = (TextView) convertView.findViewById(R.id.header_text);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			
			
			
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				convertView.setBackgroundColor(Color.rgb(1, 173, 73));
				holder.headerText.setText("Pending");
				break;

			case GROUP_AVAILABLE:
				convertView.setBackgroundColor(Color.rgb(56, 160, 220));
				holder.headerText.setText("Available");
				break;
				
			case GROUP_UNAVAILABLE:
				convertView.setBackgroundColor(Color.LTGRAY);
				holder.headerText.setText("Unavailable");
				break;
			}
		}
		
		/*SurveyViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(mLayoutId, parent, false);
			holder = new SurveyViewHolder();
			holder.stateView = convertView.findViewById(R.id.survey_state_view);
			holder.titleText = (TextView) convertView.findViewById(R.id.survey_title_text);
			holder.lastTakenDateText = (TextView) convertView.findViewById(R.id.survey_last_taken_date_text);
			holder.lastTakenTimeText = (TextView) convertView.findViewById(R.id.survey_last_taken_time_text);
			convertView.setTag(holder);
		} else {
			holder = (SurveyViewHolder) convertView.getTag();
		}
		
		getItem(position);
		
		if (mSurveys.get(position).isTriggered()) {
			holder.stateView.setBackgroundColor(Color.rgb(1, 173, 73));
		} else {
			holder.stateView.setBackgroundColor(Color.rgb(56, 160, 220));
		}
		holder.titleText.setText(mSurveys.get(position).getTitle());
		
		holder.lastTakenDateText.setText("Yesterday");
		holder.lastTakenTimeText.setText("9:03 AM");
		
		if (position != 1) {
			convertView.setEnabled(true);
			holder.titleText.setTextColor(Color.BLACK);
			holder.lastTakenDateText.setTextColor(Color.DKGRAY);
			holder.lastTakenTimeText.setTextColor(Color.DKGRAY);
		} else {
			convertView.setEnabled(false);
			holder.titleText.setTextColor(Color.GRAY);
			holder.lastTakenDateText.setTextColor(Color.GRAY);
			holder.lastTakenTimeText.setTextColor(Color.GRAY);
			holder.stateView.setBackgroundColor(Color.LTGRAY);
		}*/
		
		return convertView;
	}

	static class SurveyViewHolder {
		View stateView;
		TextView titleText;
		TextView lastTakenDateText;
		TextView lastTakenTimeText;
	}
	
	static class HeaderViewHolder {
		TextView headerText;
	}
	
	private void updateStates() {
		
		mPending.clear();
		mAvailable.clear();
		mUnavailable.clear();
		
		for (Survey survey : mSurveys) {
			if (mActiveSurveyTitles.contains(survey.getTitle())) {
				mPending.add(survey);
			} else if (survey.isAnytime()) {
				mAvailable.add(survey);
			} else {
				mUnavailable.add(survey);
			}
		}
	}
}
