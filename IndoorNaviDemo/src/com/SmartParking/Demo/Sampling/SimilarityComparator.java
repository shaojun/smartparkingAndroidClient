package com.SmartParking.Demo.Sampling;

import java.util.Comparator;

import com.SmartParking.Lookup.LocalPositionDescriptor;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Util.Tuple;

public class SimilarityComparator implements
		Comparator<Tuple<Double, LocalPositionDescriptor>> {

	@Override
	public int compare(Tuple<Double, LocalPositionDescriptor> lhs,
			Tuple<Double, LocalPositionDescriptor> rhs) {
		if (lhs.first < rhs.first) {
			return -1;
		} else {
			if (lhs.first == rhs.first) {
				return 0;
			}

			return 1;
		}
	}
}
