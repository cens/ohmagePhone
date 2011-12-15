
package org.ohmage;

import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class ChartFragment extends Fragment {

	private static final String CHART = "chart";

	private GraphicalView mGraph;

	/**
	 * Creates a new instance of a chart fragment for the given chart
	 * @param chart
	 * @return the chart fragment
	 */
	public static ChartFragment newInstance(AbstractChart chart) {
		ChartFragment fragment = new ChartFragment();
		Bundle args = new Bundle();
		args.putSerializable(CHART, chart);
		fragment.setArguments(args);

		return fragment;
	}

	/**
	 * Creates a new instance of the chart fragment which will be in a loading state
	 * @return the chart fragment
	 */
	public static ChartFragment newInstance() {
		return new ChartFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Was the chart passed in the bundle or should we expect it later
		if (getArguments() != null && getArguments().containsKey(CHART)) {
			setChart((AbstractChart) getArguments().getSerializable(CHART));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		FrameLayout view = new FrameLayout(getActivity());
		if(mGraph != null) {
			view.addView(mGraph);
		}
		return view;
	}

	/**
	 * Provide the chart for this fragment
	 */
	public void setChart(AbstractChart chart) {
		mGraph = new GraphicalView(getActivity(), chart);
		if(getView() != null) {
			((ViewGroup) getView()).removeAllViews();
			((ViewGroup) getView()).addView(mGraph);
		}
	}
}