package com.SmartParking.UI;

public class DrawCircle {
	
	/* Constructe a DrawCircle object with absolute X, Y.
	 * radius for specify the size of the circle.
	 * text for displayed on right top corner.
	 * color for circle color.
	 */
	public DrawCircle(float x, float y, Integer radius, String text, int color) {
		this.X = x;
		this.Y = y;
		this.Radius = radius;
		this.Text = text;
		this.Color = color;
	}

	/* Constructe a DrawCircle object with absolute X, Y.
	 * radius leave for use the default which defined in MarkableTouchImageView.defaultCircleRadius
	 * text for displayed on right top corner.
	 * color for circle color.
	 */
	public DrawCircle(float x, float y, String text, int color) {
		this.X = x;
		this.Y = y;
		this.Text = text;
		this.Color = color;
	}

	public float X;
	public float Y;
	public Integer Radius = 0;
	public String Text;
	public int Color;
}
