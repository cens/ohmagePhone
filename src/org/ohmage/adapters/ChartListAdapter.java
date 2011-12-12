package org.ohmage.adapters;

import org.achartengine.GraphicalView;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.Histogram.HistogramRenderer;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.NumberFormat;

public class ChartListAdapter extends CursorAdapter{

	private final int[] colors = new int[] {
		mContext.getResources().getColor(R.color.light_purple),
		mContext.getResources().getColor(R.color.light_red),
		mContext.getResources().getColor(R.color.light_blue),
		mContext.getResources().getColor(R.color.light_green),
		mContext.getResources().getColor(R.color.light_yellow)
	};

	private final LayoutInflater mInflater;

	public ChartListAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		FrameLayout chart = (FrameLayout) view.findViewById(R.id.chart);
		TextView title = (TextView) view.findViewById(R.id.title);
		TextView info = (TextView) view.findViewById(R.id.info);

		double[] data = Utilities.randomData(30, 9);

		title.setText("Hey there title");
		info.setText(NumberFormat.getInstance().format(Utilities.stats(data)[0]) + " hours a week on average");
		chart.removeAllViews();
		HistogramRenderer renderer = new HistogramRenderer(context);
		renderer.setInScroll(true);
		renderer.getSeriesRendererAt(0).setColor(colors[cursor.getPosition()%colors.length]);
		chart.addView(new GraphicalView(context, new Histogram(context, renderer, data)));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.ohmage_chart_list_item, null);
	}
}
