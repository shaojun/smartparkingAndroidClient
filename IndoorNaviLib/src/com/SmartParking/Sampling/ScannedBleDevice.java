package com.SmartParking.Sampling;

import java.io.Serializable;
import java.util.Date;

import com.SmartParking.Util.Util;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ScannedBleDevice implements Serializable {
	// public BluetoothDevice BLEDevice;

	/**
	 * Returns the hardware address of this BluetoothDevice.
	 * <p>
	 * For example, "00:11:22:AA:BB:CC".
	 * 
	 * @return Bluetooth hardware address as string
	 */
	public String MacAddress;

	public String DeviceName;
	public double RSSI;
	public double Distance;

	public byte[] CompanyId;
	public byte[] IbeaconProximityUUID;
	public byte[] Major;
	public byte[] Minor;
	public byte Tx;

	public long ScannedTime;

	@Override
	public String toString() {
		String displayText = "====" + "MacAdrs: " + this.MacAddress + "===="
				+ "\r\n";
		displayText += "Name: " + this.DeviceName + "\r\n";
		displayText += "RSSI: " + this.RSSI + "\r\n";
		displayText += "CompanyId: "
				+ Util.BytesToHexString(this.CompanyId, " ") + "\r\n";
		displayText += "IbeaconUUID: "
				+ Util.BytesToHexString(this.IbeaconProximityUUID, " ")
				+ "\r\n";
		displayText += "Major: " + Util.BytesToHexString(this.Major, " ")
				+ "\r\n";
		displayText += "Minor: " + Util.BytesToHexString(this.Minor, " ")
				+ "\r\n";
		displayText += "Tx: " + Util.BytesToHexString(this.Tx) + "\r\n";
		displayText += "Distance(m): " + Util.CalculateAccuracy(this.Tx, RSSI);

		displayText += "\r\n";
		displayText += "---------------------------";

		return displayText;
	}

	/*
	 * like: MacAdrs: xxx, Name: xxxx, RSSI: xxxx, Major: xxxx, Minor: xxxx,
	 * Distance(m): xxxx
	 */
	public String toSimpleString() {
		String displayText = "UUID: "
				+ Util.BytesToHexString(this.IbeaconProximityUUID, "");
		// + "\r\n";
		displayText += ", Major: " + Util.BytesToHexString(this.Major);
		displayText += ", Minor: " + Util.BytesToHexString(this.Minor);
		// displayText += "Tx: " + Util.BytesToHexString(this.Tx) + "\r\n";
		// displayText += ", Distance(m): "
		// + Util.CalculateAccuracy(this.Tx, RSSI);
		displayText += ", RSSI: " + RSSI;
		displayText += ", Mac: " + this.MacAddress;
		// displayText += "CompanyId: "
		// + Util.BytesToHexString(this.CompanyId, " ") + "\r\n";

		// displayText += "\r\n";
		// displayText += "---------------------------";

		return displayText;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ScannedBleDevice))
			return false;

		ScannedBleDevice target = (ScannedBleDevice) obj;

		// NOTE the Java string's equal trap.
		if (this.TwoBytesEqual(this.IbeaconProximityUUID,
				target.IbeaconProximityUUID)
				&& this.TwoBytesEqual(this.Major, target.Major)
				&& this.TwoBytesEqual(this.Minor, target.Minor)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (int i = 0; i < this.IbeaconProximityUUID.length - 1; i = i + 2) {
			result += IbeaconProximityUUID[i] ^ IbeaconProximityUUID[i + 1];
		}

		return result ^ this.Major[0] ^ this.Major[1] ^ this.Minor[0]
				^ this.Minor[1];
	}

	/**
	 * tests if two bytes are value equal.
	 */
	private boolean TwoBytesEqual(byte[] left, byte[] right) {
		if (left == null || right == null)
			return false;
		if (left.length != right.length)
			return false;
		for (int i = 0; i < left.length; i++) {
			if (left[i] != right[i])
				return false;
		}

		return true;
	}
}
