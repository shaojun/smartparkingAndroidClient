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
        float bitmapWidth = bitmap.getWidth();
        float localX = remoteX * (onImageLoadWidth / bitmapWidth);
        Log.v(LOG_TAG, "getLocalXByRemoteX, onImageLoadWidth: " + onImageLoadWidth + ", bitmapWidth: " + bitmapWidth + ", RemoteX: " + remoteX + ", caculated LocalX: " + localX);
        return localX;
    }

    public static float getLocalYByRemoteY(float remoteY, Bitmap bitmap, MarkableTouchImageView localImageView) {
        int onImageLoadHeight = localImageView.getOnImageLoadHeight();
        float bitmapHeight = bitmap.getHeight();
        float localY = remoteY * (onImageLoadHeight / bitmapHeight);
        Log.v(LOG_TAG, "getLocalYByRemoteY, onImageLoadHeight: " + onImageLoadHeight + ", bitmapHeight: " + bitmapHeight + ", RemoteY: " + remoteY + ", caculated LocalY: " + localY);
        return localY;
    }

    public static float getRemoteXByLocalX(float localX, Bitmap bitmap, MarkableTouchImageView localImageView) {
        int onImageLoadWidth = localImageView.getOnImageLoadWidth();
        float bitmapWidth = bitmap.getWidth();
        float remoteX = localX * (bitmapWidth / onImageLoadWidth);
        Log.v(LOG_TAG, "getRemoteXByLocalX, onImageLoadWidth: " + onImageLoadWidth + ", bitmapWidth: " + bitmapWidth + ", LocalX: " + localX + ", caculated RemoteX: " + remoteX);
        // the original bitmap which from web.
        return remoteX;
    }

    public static float getRemoteYByLocalY(float localY, Bitmap bitmap, MarkableTouchImageView localImageView) {
        int onImageLoadHeight = localImageView.getOnImageLoadHeight();
        float bitmapHeight = bitmap.getHeight();
        float remoteY = localY * (bitmapHeight / onImageLoadHeight);
        Log.v(LOG_TAG, "getRemoteYByLocalY, onImageLoadHeight: " + onImageLoadHeight + ", bitmapHeight: " + bitmapHeight + ", LocalY: " + localY + ", caculated RemoteY: " + remoteY);
        return remoteY;
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

    //// TODO: 11/22/2015 2 constructor need refine!!!
    public LocalPositionDescriptor(String desc, float remoteX,
                                   float remoteY, HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.remoteX = remoteX;
        this.remoteY = remoteY;
        this.localImageView = localImageView;
        this.drawable = (BitmapDrawable) localImageView.getDrawable();
        this.localX = getLocalXByRemoteX(this.remoteX, this.drawable.getBitmap(), this.localImageView);
        this.localY = getLocalYByRemoteY(this.remoteY, this.drawable.getBitmap(), this.localImageView);
        this.Description = desc;
        this.Fingerprints = fingerprints;
    }

    public LocalPositionDescriptor(float localX,
                                   float localY, String desc,
                                   HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.localX = localX;
        this.localY = localY;
        this.localImageView = localImageView;
        this.drawable = (BitmapDrawable) localImageView.getDrawable();
        this.remoteX = getRemoteXByLocalX(this.localX, this.drawable.getBitmap(), this.localImageView);
        this.remoteY = getRemoteYByLocalY(this.localY, this.drawable.getBitmap(), this.localImageView);
        this.Description = desc;
        this.Fingerprints = fingerprints;
    }
}
