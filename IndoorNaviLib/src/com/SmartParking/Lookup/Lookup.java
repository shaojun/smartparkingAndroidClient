package com.SmartParking.Lookup;

import com.SmartParking.Sampling.BleFingerprintCollector;

public class Lookup {
	private static final Lookup defaultInstance = new Lookup();
	
	private Lookup()
	{
		// load all algorithm...
	}
	
	public static Lookup getDefault()
	{
		return defaultInstance;
	}
}
