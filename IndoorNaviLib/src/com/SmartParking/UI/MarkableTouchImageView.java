package com.SmartParking.UI;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Tuple3;
import com.SmartParking.Util.Tuple4;
import com.SmartParking.Util.Tuple5;
import com.SmartParking.Util.Util;
import com.ortiz.touch.TouchImageView;
import com.ortiz.touch.TouchImageView.OnTouchImageViewListener;

public class MarkableTouchImageView extends TouchImageView {
	@Override
	public void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		// only load at the first time the image show, this is the
		// actual bitmap size you see on screen
		// since we use CenterCrop, NOTE, the size include the part
		// that didn't fully show in one screen.
		if (MarkableTouchImageView.this.onImageLoadHeight == 0) {
			final int[] onImageLoadCoors = Util
					.getBitmapPositionInsideImageView(MarkableTouchImageView.this);
			MarkableTouchImageView.this.onImageLoadHeight = onImageLoadCoors[3];
			MarkableTouchImageView.this.onImageLoadWidth = onImageLoadCoors[2];
		}
	}

	private static final String LOG_TAG = "MarkableTouchImageView";
	private Paint circlesPaint;
	private Paint circlesTextPaint;

	private final int defaultCircleRadius = 25;
	// Circle default set to a light blue.
	private int defaultCircleColor = Color.rgb(100, 100, 255);
	// Circle default set to full black.
	private int defaultCircleTextColor = Color.rgb(0, 0, 0);
	private int defaultCircleTextSize = 14;

	// private boolean customColor = false;
	private int selectedBitmapId = -1;
	// <X,Y,R>,Comments
	List<DrawCircle> drawCircles = new ArrayList<DrawCircle>();
	// <X, Y, bitmap, comments, id>
	List<DrawImage> drawImages = new ArrayList<DrawImage>();
	Bitmap selectedArrowBitmap = null;
	RelativeLayout parentRelativeLayout = null;
	View selectedView = null;
	private int onImageLoadHeight;
	private int onImageLoadWidth;

	// private float lastClickedX = 0;
	// private float lastClickedY = 0;
	// private RectF zoomedRect = null;
	// private float currentZoom = 1;
	// private boolean isZoomed = false;

	public int getOnImageLoadHeight() {
		return this.onImageLoadHeight;
	}

	public int getOnImageLoadWidth() {
		return this.onImageLoadWidth;
	}

	/*
	 * set the selected arrow bitmap. otherwise, the selected icon function
	 * would not working.
	 */
	public void setHighlightSelectedBitmap(Bitmap selectedArrowBitmap) {
		this.selectedArrowBitmap = selectedArrowBitmap;
	}

	/**
	 * support for show a button above selected bitmap. pass in this imageview's
	 * parent relativeLayout object, and the view(typically a button) want to show above the
	 * selected bitmap.
	 */
	public void setParentRelativeLayoutAndMenuView(
			RelativeLayout parentRelativeLayout, View selectedView) {
		this.parentRelativeLayout = parentRelativeLayout;
		this.selectedView = selectedView;
		// overwrite the potential id since we need use that internally
		this.selectedView.setId(-1);
		this.selectedView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (OnBitmapInTouchImageClickedListener l : MarkableTouchImageView.this.listeners) {
					l.onBitMapClicked(MarkableTouchImageView.this.selectedBitmapId);
				}
			}
		});
	}

	public MarkableTouchImageView(Context context) {
		super(context);

		// new antialised Paint
		circlesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		// circlesPaint.setColor(defaultCircleColor);
		// text size in pixels
		circlesPaint.setTextSize(defaultCircleTextSize);
		// text shadow
		circlesPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

		// new antialised Paint
		circlesTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlesTextPaint.setColor(defaultCircleTextColor);
		// text size in pixels
		circlesTextPaint.setTextSize(defaultCircleTextSize);
		// text shadow
		circlesTextPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
	}

	// Constructor for inflating via XML
	public MarkableTouchImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// new antialised Paint
		circlesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		// circlesPaint.setColor(defaultCircleColor);
		// text size in pixels
		circlesPaint.setTextSize(defaultCircleTextSize);
		circlesPaint.setTypeface(Typeface.create(Typeface.DEFAULT,
				Typeface.BOLD));
		// text shadow
		circlesPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

		// new antialised Paint
		circlesTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		// text color - #3D3D3D
		circlesTextPaint.setColor(defaultCircleTextColor);
		// text size in pixels
		circlesTextPaint.setTextSize(defaultCircleTextSize);
		// text shadow
		circlesTextPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
		circlesTextPaint.setUnderlineText(true);
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				RectF zoomedRect = MarkableTouchImageView.this.getZoomedRect();
				float currentZoom = MarkableTouchImageView.this
						.getCurrentZoom();
				boolean isZoomed = MarkableTouchImageView.this.isZoomed();
				float x = event.getX();
				float y = event.getY();
				if (zoomedRect != null) {
					if (Float.isNaN(zoomedRect.top))
						zoomedRect.top = 0f;
					if (Float.isNaN(zoomedRect.left))
						zoomedRect.left = 0f;
				}

				// remove it anyway
				MarkableTouchImageView.this.parentRelativeLayout
						.removeView(MarkableTouchImageView.this.selectedView);
				// assume you clicked nothing by this touch
				boolean doesThisTimeSelectedOnSth = false;
				for (DrawImage imageWithText : MarkableTouchImageView.this.drawImages) {
					Tuple<Float, Float> relativeXandY = Util
							.GeRelativeXAndYFromAbsolute(imageWithText.X,
									imageWithText.Y, zoomedRect, currentZoom,
									MarkableTouchImageView.this);
					Bitmap zoomedSelectableBitmap = Bitmap.createScaledBitmap(
							imageWithText.Bitmap,
							(int) (imageWithText.Bitmap.getWidth() * currentZoom),
							(int) (imageWithText.Bitmap.getHeight() * currentZoom),
							false);

					/*
					 * Log.e(LOG_TAG, "touched on ImageView, touched x: " + x +
					 * ", touched y: " + y + "img.x: " + imageWithText.first +
					 * ", img.y: " + imageWithText.second + ", imgWidHei: " +
					 * imageWithText.third.getWidth() + "-" +
					 * imageWithText.third.getHeight());
					 */
					// Check if the x and y position of the touch is inside
					// the
					// bitmap, if so, treat this click as selection
					if (x > (relativeXandY.first * 1)
							&& x < (1 * (zoomedSelectableBitmap.getWidth() + relativeXandY.first))
							&& y > (relativeXandY.second * 1)
							&& y < (1 * (zoomedSelectableBitmap.getHeight() + relativeXandY.second))) {
						Log.e(LOG_TAG, "you touched bitmap: "+ imageWithText.Id);
						doesThisTimeSelectedOnSth = true;
						MarkableTouchImageView.this.selectedBitmapId = imageWithText.Id;
						MarkableTouchImageView.this.invalidate();

						// one time only one bitmap could be clicked.
						break;
					}
				}

				if (!doesThisTimeSelectedOnSth) {
					// redraw(remove selection) if previous time have sth
					// selected
					if (selectedBitmapId != -1) {
						selectedBitmapId = -1;
						MarkableTouchImageView.this.invalidate();
					}
				}

				return false;
			}
		});
	}

	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.draw(canvas);
		RectF zoomedRect = this.getZoomedRect();
		float currentZoom = this.getCurrentZoom();
		boolean isZoomed = this.isZoomed();
		circlesTextPaint.setTextSize(defaultCircleTextSize * currentZoom);
		if (this.drawCircles != null && this.drawCircles.size() > 0) {
			// int colorChangingStep = 255 / this.drawCircles.size();
			// int previsouColor = 0;
			for (DrawCircle circleWithText : this.drawCircles) {
				if (Float.isNaN(zoomedRect.top))
					zoomedRect.top = 0f;
				if (Float.isNaN(zoomedRect.left))
					zoomedRect.left = 0f;

				Tuple<Float, Float> relativeXandY = Util
						.GeRelativeXAndYFromAbsolute(circleWithText.X,
								circleWithText.Y, zoomedRect, currentZoom,
								MarkableTouchImageView.this);

				if (circleWithText.Color == 0) {
					circlesPaint.setColor(this.defaultCircleColor);
				} else {
					circlesPaint.setColor(circleWithText.Color);
				}

				if (circleWithText.Radius <= 0)
					circleWithText.Radius = defaultCircleRadius;
				canvas.drawCircle(relativeXandY.first, relativeXandY.second,
						circleWithText.Radius * currentZoom, circlesPaint);
				if (circleWithText.Text != null && circleWithText.Text != "")
					canvas.drawText(circleWithText.Text, relativeXandY.first
							+ circleWithText.X * currentZoom / 2,
							relativeXandY.second - circleWithText.Y
									* currentZoom, circlesTextPaint);
			}
		}

		if (this.drawImages.size() > 0) {
			for (DrawImage imageWithText : this.drawImages) {
				Tuple<Float, Float> relativeXandY = Util
						.GeRelativeXAndYFromAbsolute(imageWithText.X,
								imageWithText.Y, zoomedRect, currentZoom,
								MarkableTouchImageView.this);

				// Log.e(LOG_TAG,"selectedBitmapId: "+selectedBitmapId+"");
				// give the selected bitmap a arrow icon
				if (selectedArrowBitmap != null
						&& selectedBitmapId == imageWithText.Id) {
					// dynamic create a button if selected and not draw yet.
					if (this.parentRelativeLayout != null
							&& this.selectedView != null
							&& this.parentRelativeLayout
									.findViewById(selectedBitmapId) == null
									) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
								LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT);
						params.leftMargin = Math
								.round(relativeXandY.first
										- (this.selectedView.getWidth() / 2)
										+ (selectedArrowBitmap.getWidth() * currentZoom)
										/ 2);
						params.topMargin = (int) Math
								.round((relativeXandY.second
										- (this.selectedView.getHeight()) - (imageWithText.Bitmap
										.getHeight() * currentZoom * 0.7)));
						this.selectedView.setId(selectedBitmapId);
						this.parentRelativeLayout.addView(this.selectedView,
								params);
					}

					Bitmap zoomedBitmap = Bitmap
							.createScaledBitmap(
									selectedArrowBitmap,
									(int) (selectedArrowBitmap.getWidth() * currentZoom),
									(int) (selectedArrowBitmap.getHeight() * currentZoom),
									false);
					// canvas.draw
					// 0.7 is for make arrow more closer to car icon.
					canvas.drawBitmap(
							zoomedBitmap,
							relativeXandY.first,
							(float) (relativeXandY.second - (imageWithText.Bitmap
									.getHeight() * currentZoom * 0.7)), null);
				}

				Bitmap zoomedBitmap = Bitmap.createScaledBitmap(
						imageWithText.Bitmap,
						(int) (imageWithText.Bitmap.getWidth() * currentZoom),
						(int) (imageWithText.Bitmap.getHeight() * currentZoom),
						false);
				/*
				 * Log.e(LOG_TAG, "draw bitmap, thisHeight:" + this.getHeight()
				 * + "thisWidth" + this.getWidth() + ", zoomedRect.top: " +
				 * zoomedRect.top + ", zoomedRect.left:" + zoomedRect.left +
				 * ", coor.first(X,left):" + imageWithText.first + "-" +
				 * relativeLeft + ", coor.second(Y,top):" + imageWithText.second
				 * + "-" + relativeTop + ", curZoom:" + currentZoom);
				 */

				canvas.drawBitmap(zoomedBitmap, relativeXandY.first,
						relativeXandY.second, null);
				if (imageWithText.Text != null && imageWithText.Text != "")
					canvas.drawText(
							imageWithText.Text,
							relativeXandY.first
									+ (int) (zoomedBitmap.getWidth() * 0.8),
							relativeXandY.second - zoomedBitmap.getHeight() / 8,
							circlesTextPaint);
			}
		}

		canvas.save();
	}

	/**
	 * Draw a single circle with all other visible circles removed.
	 */
	public void drawSingleCircle(DrawCircle circle) {
		// always empty all the circles.
		this.drawCircles = new ArrayList<DrawCircle>();
		this.drawCircles.add(circle);
		this.invalidate();
	}

	/**
	 * Draw a list of circles with all other visible circles removed.
	 */
	public void drawMultipleCircles(List<DrawCircle> multiple) {
		this.drawCircles = multiple;
		this.invalidate();
	}

	/**
	 * remove(invisible) all visible circles.
	 */
	public void cleanAllCircles() {
		this.drawCircles.clear();
		this.invalidate();
	}

	/**
	 * Draw circles and images on the ImageView. This will overwrite all existed
	 * circles and images.
	 * <p>
	 * leave argument 'multipleCircles' to 'null' if don't want to effect any
	 * existed circles.
	 */
	public void drawMultipleCirclesAndImages(List<DrawCircle> multipleCircles,
			List<DrawImage> multipleImage) {
		if (multipleCircles != null && multipleCircles.size() > 0) {
			this.drawCircles = multipleCircles;
		}

		this.drawImages = multipleImage;
		// this.circlesPaint.setColor(circlesColor);
		// if (circlesColor != Color.rgb(61, 61, 61)) {
		// this.customColor = true;
		// }

		this.invalidate();
	}

	ArrayList<OnBitmapInTouchImageClickedListener> listeners = new ArrayList<OnBitmapInTouchImageClickedListener>();

	public void AddBitmapInTouchImageClickedListener(
			OnBitmapInTouchImageClickedListener listener) {
		listeners.add(listener);
	}

	public boolean RemoveBitmapInTouchImageClickedListener(
			OnBitmapInTouchImageClickedListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * ret[0] = left; ret[1] = top; ret[2] = actWidth; ret[3] = actHeight;
	 * 
	 * @return PointF representing the scroll position of the zoomed image.
	 */
	public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
		int[] ret = new int[4];

		if (imageView == null || imageView.getDrawable() == null)
			return ret;

		// Get image dimensions
		// Get image matrix values and place them in an array
		float[] f = new float[9];
		imageView.getImageMatrix().getValues(f);

		// Extract the scale values using the constants (if aspect ratio
		// maintained, scaleX == scaleY)
		final float scaleX = f[Matrix.MSCALE_X];
		final float scaleY = f[Matrix.MSCALE_Y];

		// Get the drawable (could also get the bitmap behind the drawable and
		// getWidth/getHeight)
		final Drawable d = imageView.getDrawable();
		final int origW = d.getIntrinsicWidth();
		final int origH = d.getIntrinsicHeight();

		// Calculate the actual dimensions
		final int actW = Math.round(origW * scaleX);
		final int actH = Math.round(origH * scaleY);

		ret[2] = actW;
		ret[3] = actH;

		// Get image position
		// We assume that the image is centered into ImageView
		int imgViewW = imageView.getWidth();
		int imgViewH = imageView.getHeight();

		int top = (int) (imgViewH - actH) / 2;
		int left = (int) (imgViewW - actW) / 2;

		ret[0] = left;
		ret[1] = top;

		return ret;
	}
}
