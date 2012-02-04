package org.ohmage.widget;

import org.ohmage.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Scales the bounds of the image view by a given amount specified by 
 * @author cketcham
 *
 */
public class ScaledImageView extends ImageView {

	private float mScaleWidth;
	private float mScaleHeight;

	public ScaledImageView(Context context) {
		this(context, null);
	}

	public ScaledImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScaledImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScaledImageView);

		setScaleWidth(a.getFloat(R.styleable.ScaledImageView_scaleWidth, 1));
		setScaleHeight(a.getFloat(R.styleable.ScaledImageView_scaleHeight, 1));

		a.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(Float.valueOf(getMeasuredWidth()*mScaleWidth).intValue(), Float.valueOf(getMeasuredHeight()*mScaleHeight).intValue());
	}

	/**
	 * Set the scale value for the width
	 * @param scaleWidth
	 */
	public void setScaleWidth(float scaleWidth) {
		if(scaleWidth < 0)
			throw new IllegalArgumentException("the scale value must be a positive value");
		mScaleWidth = scaleWidth;
	}

	public float getScaleWidth() {
		return mScaleWidth;
	}

	/**
	 * Set the scale value for the height
	 * @param scaleHeight
	 */
	public void setScaleHeight(float scaleHeight) {
		if(scaleHeight < 0)
			throw new IllegalArgumentException("the scale value must be a positive value");
		mScaleHeight = scaleHeight;
	}

	public float getScaleHeight() {
		return mScaleHeight;
	}
}