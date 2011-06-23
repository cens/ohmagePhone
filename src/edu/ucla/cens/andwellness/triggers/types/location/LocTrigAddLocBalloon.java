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
package edu.ucla.cens.andwellness.triggers.types.location;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import edu.ucla.cens.andwellness.R;

/*
 * Class which draws and manages the 'add this location' 
 * balloon which is displayed as a view on top of the 
 * map view. This class can be instantiated and the show
 * method can be called to display the balloon at a given
 * geo-point. The address corresponding to that geo-point
 * will be fetched and displayed. It also displays a button
 * in the balloon view and a callback is called when the 
 * user clicks on the button. 
 */
public class LocTrigAddLocBalloon implements OnClickListener{

	//Number of times the addressing fetching 
	//to be tried
	private static final int GEOCODING_RETRIES = 3;
	//Interval between each retry
	private static final long GEOCODING_RETRY_INTERVAL = 300; //ms
	
	//Maximum height of the balloon
	private static final int MAX_H = 150;
	//Maximum width of the balloon
	private static final int MAX_W = 350;
	
	private Context mContext;
	//The mapview instance
	private MapView mMapView;
	//The point on which this balloon is to be displayed
	private GeoPoint mLocGP;
	private LinearLayout mLayout;
	private ActionListener mListener = null;
	//Flag to check if this balloon is visible
	private boolean mVisible = false;
	
	
	public LocTrigAddLocBalloon(Context context, MapView mv) {
		this.mContext = context;
		this.mMapView = mv;
		
		LayoutInflater  layoutInflater = 
        	(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
		//Inflate the balloon layout
		mLayout = (LinearLayout) layoutInflater.inflate(
        										R.layout.trigger_loc_maps_balloon, null);
		
		mLayout.setVisibility(View.VISIBLE);
		mLayout.setClickable(true);
	}
	
	/* 
	 * Display this balloon at the given geo-point. 
	 * 
	 */
	public void show(GeoPoint gp, String action, String addr) { 
		
		if(mVisible) {
			hide();
		}
		
		mLocGP = gp;
		mVisible = true;
		
		Button button = (Button) mLayout.findViewById(R.id.balloon_add_loc);
		button.setOnClickListener(this);
		//Set the given text to the button
		button.setText(action);
		button.setHeight(mMapView.getHeight() / 2 - 10);
		button.invalidate();
	    
		TextView tv = (TextView) mLayout.findViewById(R.id.balloon_address);
		tv.setTextColor(Color.WHITE);
		
		mMapView.getController().animateTo(gp);
		//Add the view to the map
		mMapView.addView(mLayout, 
	    		   new MapView.LayoutParams(
	    		   Math.min(mMapView.getWidth() - 20, MAX_W),
	    		   Math.min(mMapView.getHeight() / 2 - 10, MAX_H), gp, 
	    		   MapView.LayoutParams.BOTTOM_CENTER));
	    
		//If no address passed, fetch the address
		if(addr.equals("")) {
			tv.setText("Loading approximate address...");
			ProgressBar pb = (ProgressBar) mLayout.findViewById(R.id.balloon_progress);
			pb.setVisibility(ProgressBar.VISIBLE);
				
			//Fetch the address in background
			new FetchAddressTask().execute(mLocGP);
		}
		else {
			//Address provided as argument, simply display it
			onAddressFetched(addr);
		}
	}
	
	/* Hide this balloon */
	public void hide() {
		if(mVisible) {
			mMapView.removeView(mLayout);
			mVisible = false;
		}
	}
	
	/* Set the action listener interface */
	public void setActionListener(ActionListener listener) {
		this.mListener = listener;
	}
	
	/* Handle address fetch completion */
	private void onAddressFetched(String addr) {
		TextView tv = (TextView) mLayout.findViewById(R.id.balloon_address);
		tv.setText(addr);
		
		ProgressBar pb = (ProgressBar) mLayout.findViewById(R.id.balloon_progress);
		pb.setVisibility(ProgressBar.INVISIBLE);
	}
	
	@Override
	public void onClick(View v) {
		
		if(v.getId() == R.id.balloon_add_loc) {
			//User pressed the button
			
			if(mListener != null) {
				//Invoke the callback
				mListener.onAddLocationClick(mLocGP);
			}
		}
	}

	
	/********************** INNER CLASSES ****************************/
	
	/* Call back to notify the 'add location' button press */
	public interface ActionListener {
		public void onAddLocationClick(GeoPoint gp);
	}
	
	/* Async task to fetch the address */
	private class FetchAddressTask extends AsyncTask<GeoPoint, Void, String> {
		
		protected void onPreExecute() {
		}
		
		protected String doInBackground(GeoPoint... params) {
			
			//Get the address
			Geocoder geoCoder = new Geocoder(mContext, Locale.getDefault());
		    for(int cTry = 0; cTry < GEOCODING_RETRIES; cTry++) {
				try {
			    	String addr = "";
			    	List<Address> addresses = geoCoder.getFromLocation(
			    					params[0].getLatitudeE6()/1E6, 
			    					params[0].getLongitudeE6()/1E6, 2);
			
			    	//Get 2 address lines
			        if(addresses.size() > 0) {
			        	int addrLines = addresses.get(0).getMaxAddressLineIndex();
			        	if(addrLines > 0) {
			        		addr = "";
			        	}
			        	
			        	for (int i=0; i<Math.min(2, addrLines); i++) {
			                   addr += addresses.get(0).getAddressLine(i) + "\n";
			        	}
			        	
			        	if(addr.length() > 0) {
			        		return addr;
			        	}
			        }
			    }
			    catch (Exception e) {
			    }
			    
			    try {
					Thread.sleep(GEOCODING_RETRY_INTERVAL);
				} catch (InterruptedException e) {
				}
		    }
		    
		    return new String("Unable to fetch the address!\n");
		}
		
		protected void onPostExecute(String addr) {
			onAddressFetched(addr);
	    }
	}
	
	/* The balloon tip layout class */
	public static class BalloonTipLayout extends LinearLayout {
	    public BalloonTipLayout(Context context) {
			super(context);
		}
	    
	    public BalloonTipLayout(Context  context, AttributeSet  attrs) {
	    	super(context, attrs);
	    }

		@Override
	    protected void dispatchDraw(Canvas canvas) {       
	        Paint p  = new Paint();
	        p.setARGB(200, 100, 100, 100);    
	        p.setStyle(Style.FILL);
	        p.setAntiAlias(true); 
	        
	        //Draw the balloon tip
	        Path balloonTip = new Path();
	        balloonTip.moveTo(getMeasuredWidth()/2 - getMeasuredWidth() * 1 / 20,
	        				  0);
	        balloonTip.lineTo(getMeasuredWidth()/2, getMeasuredHeight());
	        balloonTip.lineTo(getMeasuredWidth()/2 + getMeasuredWidth() * 1 / 20, 
	        				  0);
	        canvas.drawPath(balloonTip, p);
	              
	        super.dispatchDraw(canvas);
	    }
	}
}
