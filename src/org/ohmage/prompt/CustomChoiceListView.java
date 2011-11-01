package org.ohmage.prompt;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

public class CustomChoiceListView extends ListView {

	public CustomChoiceListView(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    final int proposedheight = MeasureSpec.getSize(heightMeasureSpec);
	    final int actualHeight = getHeight();

	    if (actualHeight > proposedheight){
	        // Keyboard is shown
//	    	int mLastIndex = getLastVisiblePosition();
//			View v = getChildAt(mLastIndex);
//			int mLastTop = (v == null) ? 0 : v.getTop();
//			setSelectionFromTop(mLastIndex, mLastTop);
	    	post(new Runnable(){
	    		  public void run() {
	    		    setSelection(getCount() - 1);
	    		  }});
	    } else {
	        // Keyboard is hidden
	    }

	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
