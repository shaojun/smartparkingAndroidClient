package com.SmartParking.Lookup;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.io.Serializable;
import java.util.HashSet;

import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.MarkableTouchImageView;

public class LocalPositionDescriptor implements Serializable {
    private MarkableTouchImageView localImageView;
    public String Description;

    public float getLocalXByRemoteX(float remoteX) {
        BitmapDrawable drawable = (BitmapDrawable) localImageView.getDrawable();
        // the original bitmap which from web.
        Bitmap bitmap = drawable.getBitmap();
        return remoteX * (localImageView.getOnImageLoadWidth() / bitmap.getWidth());
    }

    public float getLocalYByRemoteY(float remoteY) {
        BitmapDrawable drawable = (BitmapDrawable) localImageView.getDrawable();
        // the original bitmap which from web.
        Bitmap bitmap = drawable.getBitmap();
        return remoteY * (localImageView.getOnImageLoadHeight() / bitmap.getHeight());
    }

    public float getRemoteXByLocalX(float localX) {
        BitmapDrawable drawable = (BitmapDrawable) localImageView.getDrawable();
        // the original bitmap which from web.
        Bitmap bitmap = drawable.getBitmap();
        return localX * (bitmap.getWidth() / localImageView.getOnImageLoadWidth());
    }

    public float getRemoteYByLocalY(float localY) {
        BitmapDrawable drawable = (BitmapDrawable) localImageView.getDrawable();
        // the original bitmap which from web.
        Bitmap bitmap = drawable.getBitmap();
        return localY * (bitmap.getHeight() / localImageView.getOnImageLoadHeight());
    }

    public final HashSet<ScannedBleDevice> Fingerprints;

    public LocalPositionDescriptor(String desc, float absoluteLeftOnWebBitmap,
                                   float absoluteTopOnWebBitmap, HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.localImageView = localImageView;
        this.Description = desc;
        this.Fingerprints = fingerprints;
    }

    public LocalPositionDescriptor(float absoluteLeftOnWebBitmap,
                                   float absoluteTopOnWebBitmap,
                                   HashSet<ScannedBleDevice> fingerprints,
                                   MarkableTouchImageView localImageView) {
        this.Fingerprints = fingerprints;
    }
}
