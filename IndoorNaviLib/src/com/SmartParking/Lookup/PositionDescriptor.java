package com.SmartParking.Lookup;

import java.io.Serializable;
import java.util.HashSet;

import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.MarkableTouchImageView;

public class PositionDescriptor implements Serializable {
	public String Description;

	/**
	 * absolute X and Y.
	 */
	private final float X;
	private final float Y;

	public float getOriginalMesuredOnWidth() {
		return originalMesuredOnWidth;
	}

	public float getOriginalMesuredOnHeight() {
		return originalMesuredOnHeight;
	}

	public float getTransferredXByImageView(
			MarkableTouchImageView imageViewWithDifferentSize) {
		float scaledRate = originalMesuredOnWidth
				/ imageViewWithDifferentSize.getOnImageLoadWidth();
		return X / scaledRate;
	}

	public float getTransferredYByImageView(
			MarkableTouchImageView imageViewWithDifferentSize) {
		float scaledRate = originalMesuredOnHeight
				/ imageViewWithDifferentSize.getOnImageLoadHeight();
		return Y / scaledRate;
	}

	public final HashSet<ScannedBleDevice> Fingerprints;

	/**
	 * the coordinate was measured on a fixed size of image, if you want to
	 * replay a position in a different size of the same imageView, the
	 * 'ImageLoadWidth' and 'ImageLoadHeight' are key info to show a correct
	 * position of a point.
	 */
	private transient MarkableTouchImageView imageViewMesuredOn;

	private float originalMesuredOnWidth;
	private float originalMesuredOnHeight;

	public PositionDescriptor(String desc, float absoluteLeft,
			float absoluteTop, HashSet<ScannedBleDevice> fingerprints,
			MarkableTouchImageView imageViewMesuredOn) {
		this.Description = desc;
		this.X = absoluteLeft;
		this.Y = absoluteTop;
		this.Fingerprints = fingerprints;
		this.originalMesuredOnWidth = imageViewMesuredOn.getOnImageLoadWidth();
		this.originalMesuredOnHeight = imageViewMesuredOn.getOnImageLoadHeight();
	}

	public PositionDescriptor(float absoluteLeft, float absoluteTop,
			HashSet<ScannedBleDevice> fingerprints,
			MarkableTouchImageView imageViewMesuredOn) {
		this.X = absoluteLeft;
		this.Y = absoluteTop;
		this.Fingerprints = fingerprints;
		this.originalMesuredOnWidth = imageViewMesuredOn.getOnImageLoadWidth();
		this.originalMesuredOnHeight = imageViewMesuredOn.getOnImageLoadHeight();
	}
}
