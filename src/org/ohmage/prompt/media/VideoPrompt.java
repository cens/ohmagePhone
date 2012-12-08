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
package org.ohmage.prompt.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.activity.SurveyActivity;
import org.ohmage.logprobe.Log;

import java.io.File;
import java.lang.reflect.Field;

public class VideoPrompt extends MediaPrompt {

	private static final String TAG = "VideoPrompt";
	private int mDuration = 3 * 60;

	public VideoPrompt() {
		super();
	}

	public void setMaxDuration(int duration) {
		mDuration = duration;
	}

	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please take a video of something before continuing.");
	}

	@Override
	public void handleActivityResult(Context context, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
				File videoFile = Utilities.fileForMediaStore(data.getData());
				if(videoFile != null && videoFile.exists()) {
					Log.i(TAG, "Video size = " + videoFile.length() + " bytes");
					if(videoFile.length() / 1024 / 1024 > 300) {
						Log.e(TAG, "Video exceeded 300 MB. It was " + videoFile.length() + " bytes");
						Toast.makeText(context, "Video size exceeds 300 MB. The file will be stored on the sdcard, but not uploaded to the server. Record less video or lower the quality.", Toast.LENGTH_LONG).show();
					} else {
						Utilities.moveMediaStoreFile(data.getData(), getMedia());
					}
				}
			} else {
				Log.e(TAG, "Video was not found!");
			}
			((SurveyActivity) context).reloadCurrentPrompt();
		} 
	}

	public static class PositionedMediaController extends MediaController {

		private final WindowManager mWindowManager;
		private final VideoView mVideo;

		public PositionedMediaController(Context context, VideoView video) {
			super(context);
			mVideo = video;
			video.setMediaController(this);

			mWindowManager = (WindowManager)context.getSystemService("window");
		}

		@Override
		public void show(int timeout) {
			super.show(timeout);

			try {
				Field decorField = getClass().getSuperclass().getDeclaredField("mDecor");
				boolean accessable = decorField.isAccessible();
				decorField.setAccessible(true);	
				View decor = (View) decorField.get(this);
				decorField.setAccessible(accessable);

				int [] anchorpos = new int[2];
				mVideo.getLocationOnScreen(anchorpos);

				if(getMeasuredHeight() == 0)
					measure(mVideo.getWidth(), LayoutParams.WRAP_CONTENT);

				WindowManager.LayoutParams p = new WindowManager.LayoutParams();
				p.gravity = Gravity.LEFT | Gravity.TOP;
				p.width = mVideo.getWidth();
				p.height = LayoutParams.WRAP_CONTENT;
				p.x = anchorpos[0];
				p.y = anchorpos[1] - getMeasuredHeight() + mVideo.getHeight();
				p.format = PixelFormat.TRANSLUCENT;
				p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
				p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
				p.token = null;
				p.windowAnimations = 0;

				mWindowManager.updateViewLayout(decor, p);

			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	Handler mHandler = new Handler();

	@Override
	public View inflateView(Context context, ViewGroup parent) {
		super.inflateView(context, parent);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.prompt_video, parent);

		ImageButton button = (ImageButton) layout.findViewById(R.id.video_button);
		VideoView video = (VideoView) layout.findViewById(R.id.video_view);
		TextView instructions = (TextView) layout.findViewById(R.id.video_instructions);

		instructions.setVisibility(View.VISIBLE);
		video.setVisibility(View.GONE);
		if (isPromptAnswered()) {
			instructions.setVisibility(View.GONE);
			video.setVisibility(View.VISIBLE);
			video.setVideoPath(getMedia().getAbsolutePath());
			MediaController ctlr = new PositionedMediaController(context, video);
			video.requestFocus();
		}

		final Activity act = (Activity) context;

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
				.putExtra(MediaStore.EXTRA_DURATION_LIMIT, mDuration);
				act.startActivityForResult(intent, REQUEST_CODE);
			}
		});

		return layout;
	}
}
