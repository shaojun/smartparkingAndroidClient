package com.SmartParking.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.MarkableTouchImageView;

public class Util {
	private static final String LOG_TAG = "SmarkParking.Util";

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}

		return data;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static String BytesToHexString(byte[] bytes) {
		String result = "";
		for (byte bb : bytes) {
			result += String.format("%02X", bb);
		}

		return result;
	}

	public static String BytesToHexString(byte[] bytes, String spliter) {
		String result = "";
		for (byte bb : bytes) {
			result += String.format("%02X", bb) + spliter;
		}

		return result;
	}

	public static String BytesToHexString(byte b) {
		return String.format("%02X", b);
	}

	public static double CalculateAccuracy(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}

		// hard code txPower to -63 since all my hardware didn't get calibrated.
		txPower = -46;

		double ratio = rssi * 1.0 / txPower;
		if (ratio < 1.0) {
			return Math.pow(ratio, 10);
		} else {
			double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
			return accuracy;
		}
	}

	public static double CalculateAverageRssi(List<ScannedBleDevice> sources) {
		if (sources == null || sources.size() == 0) {
			return 0;
		}

		double accu = 0;
		for (int i = 0; i < sources.size(); i++) {
			accu += sources.get(i).RSSI;
		}

		return accu / sources.size();
	}

	// public static Hashtable<Position, HashSet<ScannedBleDevice>>
	// FindCountAndNameCharacteristicMatchSubGroup(
	// HashSet<ScannedBleDevice> fingerprint,
	// Hashtable<Position, HashSet<ScannedBleDevice>> buildInSamples,
	// int allowingCountDrift) {
	// Hashtable<Position, HashSet<ScannedBleDevice>>
	// countAndNameCharacteristicMatchSubset = new Hashtable<Position,
	// HashSet<ScannedBleDevice>>();
	// for (Position sampleKey : buildInSamples.keySet()) {
	// if (allowingCountDrift == 0
	// && buildInSamples.get(sampleKey).size() == fingerprint
	// .size()) {
	// boolean match = false;
	// for (ScannedBleDevice s : buildInSamples.get(sampleKey)) {
	// if (!fingerprint.contains(s)) {
	// match = false;
	// break;
	// }
	//
	// match = true;
	// }
	//
	// if (match) {
	// countAndNameCharacteristicMatchSubset.put(sampleKey,
	// buildInSamples.get(sampleKey));
	// }
	// } else {
	// int ceilingCount = fingerprint.size()
	// * (1 + allowingCountDrift / 100);
	// int floorCount = fingerprint.size()
	// * (1 - allowingCountDrift / 100);
	// }
	// }
	//
	// return countAndNameCharacteristicMatchSubset;
	// }
	public static String ToLogString(HashSet<ScannedBleDevice> set) {
		String result = "";
		for (ScannedBleDevice ble : set) {
			result += ble.toSimpleString() + "\r\n";
		}

		return result;
	}

	public static String ToLogString(List<ScannedBleDevice> list) {
		String result = "";
		for (ScannedBleDevice ble : list) {
			result += ble.toSimpleString() + "\r\n";
		}

		return result;
	}

	public static HashSet<ScannedBleDevice> DistinctAndAvgFingerprint(
			List<ScannedBleDevice> fingerprint) {
		// scannedDevice, <samplingCount, samplingRssiAccumulator>
		Hashtable<ScannedBleDevice, Tuple<Integer, Double>> accu = new Hashtable<ScannedBleDevice, Tuple<Integer, Double>>();

		for (int i = 0; i < fingerprint.size(); i++) {
			@SuppressWarnings("unused")
			int testHashCode = fingerprint.get(i).hashCode();
			// note, 2 duplcated item would be counted as
			Tuple<Integer, Double> existed = accu.get(fingerprint.get(i));
			if (existed != null) {
				existed.first++;
				existed.second += fingerprint.get(i).RSSI;
				Log.e(LOG_TAG,
						"-----------Major:"
								+ Util.BytesToHexString(fingerprint.get(i).Major)
								+ ",Minor:"
								+ Util.BytesToHexString(fingerprint.get(i).Minor)
								+ ", will add rssi: " + fingerprint.get(i).RSSI
								+ ", and now accu: " + existed.second);
			} else {
				Tuple<Integer, Double> newAdd = new Tuple<Integer, Double>(1,
						fingerprint.get(i).RSSI);
				Log.e(LOG_TAG,
						"-----------Major:"
								+ Util.BytesToHexString(fingerprint.get(i).Major)
								+ ",Minor:"
								+ Util.BytesToHexString(fingerprint.get(i).Minor)
								+ ", will put new rssi: "
								+ fingerprint.get(i).RSSI);
				accu.put(fingerprint.get(i), newAdd);
			}
		}

		HashSet<ScannedBleDevice> result = new HashSet<ScannedBleDevice>();
		for (ScannedBleDevice d : accu.keySet()) {
			Tuple<Integer, Double> t = accu.get(d);
			// since RSSI is not counted into getHashCode(), so it's safe to
			// overwrite it.

			d.RSSI = t.second / t.first;
			result.add(d);
		}

		return result;
	}

	// public static List<TKey,TSource> GroupBy(List<TSource> target, )

	// name and RSSI share 100 similarity points.
	private static double nameWeight = 30;
	// 'n' name match earn = n/samplesNameCount * nameWeight.
	// 'm' name mismatch * power 0.9

	private static double rssiWeight = 70;

	/**
	 * Calculate the similarity between the 2 sets of scanned ble signal. the
	 * higher the more similar.
	 */
	public static double CaculateSimilarity(
			HashSet<ScannedBleDevice> fingerprint,
			HashSet<ScannedBleDevice> buildInSamples
	// , int allowingCountDrift,
	// int allowingRssiDrift
	) {
		/**
		 * think about 2 circles with limited area overlapped...
		 * 
		 */

		int nameMatchedCount = 0;
		int nameMismatchedCount = 0;
		for (ScannedBleDevice f : fingerprint) {
			if (buildInSamples.contains(f)) {
				nameMatchedCount++;
			} else {
				nameMismatchedCount++;
			}
		}

		for (ScannedBleDevice s : buildInSamples) {
			if (!fingerprint.contains(s)) {
				nameMismatchedCount++;
			}
		}

		double nameScore = 0;
		if (nameMismatchedCount == 0) {
			nameScore = nameWeight;
		} else {
			double debugPow = 1;// Math.pow(0.9, nameMismatchedCount);
			// Log.e(LOG_TAG, "debugPow: " + debugPow
			// + ", buildInSamples.size(): " + buildInSamples.size());
			// every one mismatch cause a * 0.9
			nameScore = (double) nameMatchedCount / buildInSamples.size()
					* nameWeight * debugPow;
		}

		Log.e(LOG_TAG, "nameMatchedCount: " + nameMatchedCount
				+ ", nameMismatchedCount: " + nameMismatchedCount
				+ ", buildInSamplesCount: " + buildInSamples.size()
				+ ", nameScore: " + nameScore);

		double rssiOffsetPercentage = 0;
		int ignoreCount = 0;
		boolean matchedAtLeastOnce = false;
		for (ScannedBleDevice f : fingerprint) {
			for (ScannedBleDevice s : buildInSamples)
				// only test the rssi for name matched scannedDevice
				if (f.equals(s)) {
					matchedAtLeastOnce = true;
					double offset = Math.abs((f.RSSI - s.RSSI)
							/ (double) (s.RSSI));
					// ignore the big offset values, consider as meaningless.
					if (offset < 1) {
						rssiOffsetPercentage += offset;
					} else {
						ignoreCount++;
					}
				}
		}

		double rssiScore = 0;
		if (matchedAtLeastOnce) {
			rssiScore = (1 - rssiOffsetPercentage
					/ (nameMatchedCount - ignoreCount))
					* rssiWeight;
		}

		Log.e(LOG_TAG, "matchedAtLeastOnce: " + matchedAtLeastOnce
				+ ", total rssiOffsetPercentage: " + rssiOffsetPercentage
				+ ", ignore rssi count: " + ignoreCount + ", rssiScore: "
				+ rssiScore);
		return nameScore + rssiScore;
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

	/**
	 * for calculate the ABSOLUTE x,y coordinate in MarkableTouchImageView.
	 * 
	 * @return Tuple<Float, Float> for transfered ABSOLUTE coordinate of X and
	 *         Y.
	 */
	public static Tuple<Float, Float> GetAbsoluteXAndYFromRelative(
			float relativeX, float relativeY, RectF zoomedRect,
			float currentZoom, MarkableTouchImageView imageView) {
		float absY = zoomedRect.top * imageView.getOnImageLoadHeight()
				+ relativeY / currentZoom;
		float absX = zoomedRect.left * imageView.getOnImageLoadWidth()
				+ relativeX / currentZoom;
		return new Tuple<Float, Float>(absX, absY);
	}

	/**
	 * for calculate the RELATIVE x,y coordinate in MarkableTouchImageView.
	 * 
	 * @return Tuple<Float, Float> for transfered RELATIVE coordinate of X and
	 *         Y.
	 */
	public static Tuple<Float, Float> GeRelativeXAndYFromAbsolute(float absX,
			float absY, RectF zoomedRect, float currentZoom,
			MarkableTouchImageView imageView) {
		float relativeY = (absY - zoomedRect.top
				* imageView.getOnImageLoadHeight())
				* currentZoom;
		float relativeX = (absX - zoomedRect.left
				* imageView.getOnImageLoadWidth())
				* currentZoom;
		return new Tuple<Float, Float>(relativeX, relativeY);
	}

}
