package org.ohmage.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

/**
 * A simple test case of {@link FilterableFragment} which uses a {@link ResponseLoader}
 * @author cketcham
 *
 */
public class TestResponseFragment extends FilterableFragment {

	private TextView mText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mText = new TextView(getActivity());
		updateTextView();
		return mText;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		updateTextView();
	}

	private void updateTextView() {
		StringBuilder text = new StringBuilder();
		text.append("campaign="+getCampaignUrn()+"\n");
		text.append("survey="+ getSurveyId()+"\n");
		text.append("data="+ getStartBounds()+","+getEndBounds()+"\n");
		text.append("start="+ new Date(getStartBounds())+"\n");
		text.append("end="+ new Date(getEndBounds())+"\n");

		mText.setText(text);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new ResponseLoader(this, null).onCreateLoader(arg0, arg1);
	}
}