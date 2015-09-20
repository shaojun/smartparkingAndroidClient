package com.SmartParking.Lookup;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import com.SmartParking.Sampling.ScannedBleDevice;

public interface LookupAlgorithm {
	// the higher priority, the earlier to test.
	public int getPriority();
	public PositionDescriptor Lookup(HashSet<ScannedBleDevice> fingerprint, Hashtable<PositionDescriptor, HashSet<ScannedBleDevice>> buildInSamples);
}
