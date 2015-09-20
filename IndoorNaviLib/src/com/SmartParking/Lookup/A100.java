package com.SmartParking.Lookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import com.SmartParking.Sampling.ScannedBleDevice;

public class A100 implements LookupAlgorithm {

	// 20%, say sampling RSSI is 100, then 80-120 is still consider in its
	// range.
	private float maxDrift = 20;

	@Override
	public PositionDescriptor Lookup(HashSet<ScannedBleDevice> fingerprint,
			Hashtable<PositionDescriptor, HashSet<ScannedBleDevice>> buildInSamples) {

		for (ScannedBleDevice current : fingerprint) {
			if (!buildInSamples.contains(current)) {
				return null;
			}
		}

		for (ScannedBleDevice current : fingerprint) {
			int sampleRSSI = 0;// = buildInSamples.get(current);
			float floor = sampleRSSI * (1 - maxDrift / 100);
		}
		return null;

	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 100;
	}

}
