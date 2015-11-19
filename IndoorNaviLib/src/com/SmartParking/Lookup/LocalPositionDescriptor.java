package com.SmartParking.Lookup;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.io.Serializable;
import java.util.HashSet;

import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.MarkableTouchImageView;

public class LocalPositionDescriptor implements Serializable {
    private static String LOG_TAG = "LocalPositionDescriptor";
    private MarkableTouchImageView localImageView;
    private BitmapDrawable drawable;
    private float localX = -1;
    private float localY = -1;
    private float remoteX = -1;
    private float remoteY = -1;

    public String Description;
    public boolean FlushedToWeb = false;


    /**
     * Transfer(caculate) the remote X coordinate based on local image view.
     *
     * @param remoteX        X coordinate measured on remote original bitmap.
     * @param bitmap         remote original bitmap of a map.
     * @param localImageView local image view to show the bitmap.
     */
    public static float getLocalXByRemoteX(float remoteX, Bitmap bitmap, MarkableTouchImageView localImageView) {
        int onImageLoadWidth = localImageView.getOnImageLoadWidth();
        int bitmapWidth = bitmap.getWidth();
        Log.v(LOG_TAG, "getLocalXByRemoteX, onImageLoadWidth: " + onImageLoadWidth + ", bitmapWidth: " + bitmapWidth);
        return remoteX * (onImageLoadWidth / bitmapWidth);
    }

    public static float getLocalYByRemoteY(float remoteY, Bitmap bitmap, MarkableTouchImageView localImageView) {
        return remoteY * (localImageView.getOnImageLoadHeight() / bitmap.getHeight());
    }

    public static float getRemoteXByLocalX(float localX, Bitmap bitmap, MarkableTouchImageView localImageView) {
        int onImageLoadWidth = localImageView.getOnImageLoadWidth();
        int bitmapWidth = bitmap.getWidth();
        Log.v(LOG_TAG, "getRemoteXByLocalX, onImageLoadWidth: " + onImageLoadWidth + ", bitmapWidth: " + bitmapWidth);
        // the original bitmap which from web.
        return localX * (bitmapWidth / onImageLoadWidth);
    }

    public static float getRemoteYByLocalY(float localY, Bitmap bitmap, MarkableTouchImageView localImageView) {
        // the original bitmap which from web.
        return localY * (bitmap.getHeight() / localImageView.getOnImageLoadHeight());
    }

    public float getRemoteX() {
        return this.remoteX;
    }

    public float getRemoteY() {
        return this.remoteY;
    }

    public float getLocalX() {
        return this.localX;
    }

    public float getLocalY() {
        return this.localY;
    }

    public final HashSet<ScannedBleDevice> Fingerprints;

    public LocalPositionDescriptor(String desc, float absoluteLeftOnWebBitmap,
                                   float absoluteTopOnWebBitmap, HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.remoteX = absoluteLeftOnWebBitmap;
        this.remoteY = absoluteTopOnWebBitmap;
        this.localImageView = localImageView;
        this.drawable = (BitmapDrawable) localImageView.getDrawable();
        this.localX = getLocalXByRemoteX(this.remoteX, this.drawable.getBitmap(), this.localImageView);
        this.localY = getLocalYByRemoteY(this.remoteY, this.drawable.getBitmap(), this.localImageView);
        this.Description = desc;
        this.Fingerprints = fingerprints;
    }

    public LocalPositionDescriptor(float absoluteLeftOnLocalBitmap,
                                   float absoluteTopOnLocalBitmap,
                                   HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.localX = absoluteLeftOnLocalBitmap;
        this.localY = absoluteTopOnLocalBitmap;
        this.localImageView = localImageView;
        this.drawable = (BitmapDrawable) localImageView.getDrawable();
        this.remoteX = getRemoteXByLocalX(this.localX, this.drawable.getBitmap(), this.localImageView);
        this.remoteY = getRemoteYByLocalY(this.localY, this.drawable.getBitmap(), this.localImageView);
        this.Fingerprints = fingerprints;
    }
}
