
package org.ohmage;

import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChartFragment extends Fragment {

	private static final String CHART = "chart";

	private AbstractChart mChart;
	private GraphicalView mView;

	public static ChartFragment newInstance(AbstractChart chart) {
		ChartFragment fragment = new ChartFragment();
		Bundle args = new Bundle();
		args.putSerializable(CHART, chart);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() == null || !getArguments().containsKey(CHART))
			throw new RuntimeException("A chart fragment must contain a specified chart");

		mChart = (AbstractChart) getArguments().getSerializable(CHART);
		mView = new GraphicalView(getActivity(), mChart);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(mView.getParent() != null)
			((ViewGroup)mView.getParent()).removeView(mView);
		return mView;
	}
}