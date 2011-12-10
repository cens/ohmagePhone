
package org.ohmage.activity;

import org.ohmage.ChartFragment;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.SparkLine;
import org.ohmage.ui.BaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

public class FeedbackActivity extends BaseActivity {

	public static final double[] FAKE_DATA1 = new double[] {
			1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0
	};

	public static final double[] FAKE_DATA2 = new double[] {
			1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);

		if (getSupportFragmentManager().findFragmentById(R.id.chart1) == null) {
			setChart("Exercise Frequency", FAKE_DATA1, R.id.chart1_title, R.id.chart1);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart2) == null) {
			setChart("Healthy Diet", FAKE_DATA2, R.id.chart2_title, R.id.chart2);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart3) == null) {
			setChart("Random Graph", Utilities.randomData(30, 9), R.id.chart3_title, R.id.chart3);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_response_graph) == null) {
			ChartFragment f = ChartFragment.newInstance(buildResponseFrequencyGraph());
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_response_graph, f)
					.commit();
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		Button b = (Button) findViewById(R.id.feedback_charts_more);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ChartFeedbackActivity.class));
			}
		});
	}

	private void setChart(String title, double[] data, int chartTitleId, int chartId) {
		SparkLine chart = new SparkLine(this, title, data);
		chart.getRenderer().setInScroll(true);
		((TextView) findViewById(chartTitleId)).setText(chart.getRenderer().getChartTitle());
		ChartFragment f = ChartFragment.newInstance(chart);
		getSupportFragmentManager().beginTransaction().add(chartId, f).commit();
	}

	private Histogram buildResponseFrequencyGraph() {

		double[] values = new double[10];

		double max = 0;
		Random r = new Random();
		for (int i = 0; i < values.length; i++) {
			values[i] = Math.abs(r.nextInt() % 6);
			if (values[i] > max)
				max = values[i];
		}

		Histogram.HistogramRenderer renderer = new Histogram.HistogramRenderer(this);
		renderer.setMargins(new int[] {
				30, 35, 15, 30
		});
		renderer.setYLabels(5);
		renderer.setYTitle("# of Responses");
		renderer.setInScroll(true);

		return new Histogram(this, renderer, values);
	}
}
