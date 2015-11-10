package com.SmartParking.UI;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

/**
 * A image with absolute coordinate, comments, and an id for support click.
 *
 */
public class DrawImage {
	public DrawImage(float x, float y, Bitmap bitmap, String text, String id)
	{
		this.X = x;
		this.Y = y;
		this.Bitmap = bitmap;
		this.Text = text;
		this.Id = id;
	}
	
	public float X;
	public float Y;
	public Bitmap Bitmap;
	public String Text;	
	/**
	 * will fire this id to click event
	 *
	 */
	public String Id;
}
