package edu.ucla.cens.andwellness.prompts.photo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.R.id;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.activity.SurveyActivity;
import edu.ucla.cens.andwellness.prompts.AbstractPrompt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

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

	@Override
	protected Object getTypeSpecificResponseObject() {
		return uuid;
	}
	
	@Override
	public void handleActivityResult(Context context, int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			
			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
			}
			
			Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(IMAGE_PATH + "/temp.jpg"), 800, 600, false);
			
			try {
			       FileOutputStream out = new FileOutputStream(IMAGE_PATH + "/temp" + uuid + ".jpg");
			       bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
			       out.flush();
			       out.close();
			} catch (Exception e) {
			       e.printStackTrace();
			}
			
			new File(IMAGE_PATH + "/temp.jpg").delete();
			
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
		
		if (uuid != null) {
			imageView.setImageBitmap(BitmapFactory.decodeFile(IMAGE_PATH + "/temp" + uuid + ".jpg"));
		}
		
		final Activity act = (Activity) context;
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMAGE_PATH + "/temp.jpg")));
				act.startActivityForResult(intent, 1);

			}
		});
		
		return layout;
	}

}
