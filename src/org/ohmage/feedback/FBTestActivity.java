package org.ohmage.feedback;

import org.ohmage.R;
import org.ohmage.db.DbContract;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class FBTestActivity extends Activity {
	private static final int CONTEXT_COPY = 1;
	private Button mQueryButton;
	private EditText mQueryTextBox;
	private TextView mResultTextBox;
	private TableLayout mResultTableLayout;
	private Activity me;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fbtest);
		
		// set ourselves for the click listener
		me = this;
		
		// collect form elements
		mQueryButton = (Button)findViewById(R.id.button_queryfbcp);
		mQueryTextBox = (EditText)findViewById(R.id.edittext_queryfbcp);
		mResultTextBox = (TextView)findViewById(R.id.textview_queryresult);
		mResultTableLayout = (TableLayout)findViewById(R.id.table_queryresult);
		
		// and associate handlers
		mQueryButton.setOnClickListener(mClickListener);
	}
	
	private OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.button_queryfbcp:
					// get text, perform query, and go about our businesses
					String query = mQueryTextBox.getText().toString();
					
					Cursor result = null;
					
					// clear the tablelayout, to start
					mResultTableLayout.removeAllViews();
					
					try {
						ContentResolver cr = me.getContentResolver();
						
						result = cr.query(Uri.parse("content://" + DbContract.CONTENT_AUTHORITY + "/" + query), null, null, null, null);						
						result.moveToFirst();
						
						// first print out the column names
						TableRow candidateColRow = new TableRow(me);
						candidateColRow.setBackgroundColor(Color.DKGRAY);
						for (int col = 0; col < result.getColumnCount(); ++col) {
							TextView candidateText = new TextView(me);
							candidateText.setText(result.getColumnName(col));
							candidateText.setPadding(3, 3, 5, 5);
							candidateText.setTextColor(Color.WHITE);
							candidateText.setTextSize(14.0f);
							candidateText.setTypeface(Typeface.DEFAULT_BOLD);
							candidateColRow.addView(candidateText);
						}
						mResultTableLayout.addView(candidateColRow);
						
						// now add each row to the tablelayout
						for (int i = 0; i < result.getCount(); ++i) {
							TableRow candidateRow = new TableRow(me);
							
							// for each row, iterate through the columns
							for (int col = 0; col < result.getColumnCount(); ++col) {
								// display the data at this row/column
								TextView candidateText = new TextView(me);
								candidateText.setText(result.getString(col));
								candidateText.setTextSize(14.0f);
								candidateText.setPadding(3, 3, 5, 5);
								candidateRow.addView(candidateText);
								
								// allow cell to be copied
								registerForContextMenu(candidateText);
							}
							
							// and move on
							mResultTableLayout.addView(candidateRow);
							result.moveToNext();
						}
					}
					catch (Exception e) {
						// show the exception in a toast
						Toast.makeText(me, e.getMessage(), Toast.LENGTH_SHORT).show();
					}
					finally {
						if (result != null && !result.isClosed())
							result.close();
					}
					
					break;
			}
		}
	};
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.fbtest_menu, menu);
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fbtest_forcesync:
				WakefulIntentService.sendWakefulWork(this, FeedbackService.class);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private TextView myTargetTextView = null;
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v instanceof TextView) {
		    //user has long pressed your TextView
		    menu.add(0, CONTEXT_COPY, 0, "Copy");
		    TextView txView = (TextView)v;
		    myTargetTextView = txView;
		    txView.setBackgroundColor(Color.YELLOW);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case CONTEXT_COPY:
				// first check if there's anything there
				if (myTargetTextView == null)
					return false;
				
			    //place your TextView's text in clipboard
			    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE); 
			    clipboard.setText(myTargetTextView.getText());
			    myTargetTextView.setBackgroundColor(Color.TRANSPARENT);
			    			    
			    return true;
		}
		
		return false;
	}
	
	@Override
	public void onContextMenuClosed(Menu menu) {
		if (myTargetTextView != null) {
			// remove the yellow background when the menu closes
			myTargetTextView.setBackgroundColor(Color.TRANSPARENT);
			myTargetTextView = null;
		}
		
	}
}
