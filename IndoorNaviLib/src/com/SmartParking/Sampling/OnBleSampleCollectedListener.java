package com.SmartParking.Sampling;

import java.util.List;

import android.view.View;

public interface OnBleSampleCollectedListener {

	void onSampleCollected(List<ScannedBleDevice> samples);

}
