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
package edu.ucla.cens.andwellness.prompt.photo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.activity.SurveyActivity;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;

public class PhotoPrompt extends AbstractPrompt {
	
	public static final String IMAGE_PATH = "/sdcard/aw_images"; //use Environment.getExternalStorageDirectory()
	
	String mResolution;
	String uuid;
	
	//ImageView imageView;
	//Bitmap mBitmap;

	public PhotoPrompt() {
		super();
	}
	
	public void setResolution(String res) {
		this.mResolution = res;
	}
	
	@Override
	protected void clearTypeSpecificResponseData() {
		uuid = null;
		//mBitmap = null;
	}
	
	/**
	 * Returns true if the UUID is not null meaning that we have at least some
	 * image that we are referencing.
	 */
	@Override
	public boolean isPromptAnswered() {
		return(uuid != null);
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		return uuid;
	}
	
	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please take a picture of something before continuing.");
	}
	
	@Override
	protected Object getTypeSpecificExtrasObject() {
		return null;
	}
	
	@Override
	public void handleActivityResult(Context context, int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			
			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
			}
			
			final String campaignUrn = ((SurveyActivity) context).getCampaignUrn();
			
			Bitmap source = BitmapFactory.decodeFile(IMAGE_PATH + "/" + campaignUrn.replace(':', '_') + "/temp.jpg");
			Bitmap scaled;
			
			if (source.getWidth() > source.getHeight()) {
				scaled = Bitmap.createScaledBitmap(source, 800, 600, false);
			} else {
				scaled = Bitmap.createScaledBitmap(source, 600, 800, false);
			}			
			
			try {
		       FileOutputStream out = new FileOutputStream(IMAGE_PATH + "/" + campaignUrn.replace(':', '_') + "/temp" + uuid + ".jpg");
		       scaled.compress(Bitmap.CompressFormat.JPEG, 80, out);
		       out.flush();
		       out.close();
			} catch (Exception e) {
		       e.printStackTrace();
			}
			
			new File(IMAGE_PATH + "/" + campaignUrn.replace(':', '_') + "/temp.jpg").delete();
			
			//new File(IMAGE_PATH + "/temp.jpg").renameTo(new File(IMAGE_PATH + "/temp" + uuid + ".jpg"));
			
			((SurveyActivity) context).reloadCurrentPrompt();
		} 
	}

	@Override
	public View getView(Context context) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.prompt_photo, null);
		
		ImageButton button = (ImageButton) layout.findViewById(R.id.photo_button);
		ImageView imageView = (ImageView) layout.findViewById(R.id.image_view);
		
		final String campaignUrn = ((SurveyActivity) context).getCampaignUrn();
		
		new File(PhotoPrompt.IMAGE_PATH + "/" + campaignUrn.replace(':', '_')).mkdirs();
		
		if (uuid != null) {
			imageView.setImageBitmap(BitmapFactory.decodeFile(IMAGE_PATH + "/" + campaignUrn.replace(':', '_') + "/temp" + uuid + ".jpg"));
		}
		
		final Activity act = (Activity) context;
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMAGE_PATH + "/" + campaignUrn.replace(':', '_') + "/temp.jpg")));
				act.startActivityForResult(intent, 1);

			}
		});
		
		return layout;
	}

}
