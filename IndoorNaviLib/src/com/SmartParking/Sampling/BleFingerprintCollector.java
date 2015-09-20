package com.SmartParking.Sampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.SmartParking.Util.Util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.OperationCanceledException;
import android.util.Log;
import android.widget.Toast;

public class BleFingerprintCollector {
	// specify which UUID should be parsed in advertising data. leave it null if
	// want to capture all.
	public byte[] UuidMatcher;
	// public boolean isRunning = false;
	private BluetoothAdapter mBluetoothAdapter;
	// the interval to notifying scanned ble data, by ms.
	// private Integer reportInterval = 0;
	private boolean shouldStartSampling = false;
	private static final String LOG_TAG = "BleFingerprintCollector";
	// Device scan callback.
	private ScanCallback mLeScanCallback;
	private BluetoothLeScanner mLescanner;
	// this value was get by massive testing, do not change it unless you know what are u doing.
	private int startAndStopScanInterval = 1100;
	private static final BleFingerprintCollector defaultInstance = new BleFingerprintCollector();

	/**
	 * Indicator for if the Ble Scan is TurnedOn.
	 * <p>
	 * TurnedOn does not mean the ble data would pop up via
	 * OnBleSampleCollectedListener.
	 */
	public AtomicBoolean IsStarted = new AtomicBoolean(false);
	private Lock syncLock = new ReentrantLock();

	private List<ScannedBleDevice> fingerprints = new ArrayList<ScannedBleDevice>();

	private List<OnBleSampleCollectedListener> listeners = new ArrayList<OnBleSampleCollectedListener>();

	public void AddOnBleSampleCollectedListener(OnBleSampleCollectedListener l) {
		this.listeners.add(l);
	}

//	public void setLeScanStartAndStopInterval(int sleepInterval) {
//		this.startAndStopScanInterval = sleepInterval;
//	}

	public Integer getLeScanStartAndStopInterval() {
		return this.startAndStopScanInterval;
	}

	public boolean RemoveOnBleSampleCollectedListener(
			OnBleSampleCollectedListener l) {
		return this.listeners.remove(l);
	}

	private BleFingerprintCollector() {
		/* setup the scanning callback... */
		this.mLeScanCallback = new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				// Official doc said: callbackType Determines how this callback
				// was triggered. Currently could only be
				// CALLBACK_TYPE_ALL_MATCHES.
				BluetoothDevice device = result.getDevice();
				// Log.e(LOG_TAG, "Wow   in onScanResult, " + device.getName()
				// + " is coming...");

				ScanRecord scanRecord = result.getScanRecord();
				// scanRecord.
				byte[] rawScanRecord = scanRecord.getBytes();
				ScannedBleDevice d = ParseRawScanRecord(device,
						result.getRssi(), rawScanRecord, UuidMatcher);
				// could be the one filtered or exceptioned.
				if (d == null) {
					return;
				}

				// Log.e(LOG_TAG,
				// "onLeScan(...), rssi: " + rssi + ", distance: "
				// + Util.CalculateAccuracy(-59, rssi) * 100
				// + ", scanRecord: "
				// + Util.BytesToHexString(scanRecord, " "));
				syncLock.lock();
				try {
					// Log.e(LOG_TAG, "Add inlock");
					fingerprints.add(d);
					// Log.e(LOG_TAG, "releasing Add inlock");
				} finally {
					syncLock.unlock();
					// Log.e(LOG_TAG, "released Add inlock");
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				Log.e(LOG_TAG, "in onBatchScanResults, " + results.size()
						+ " results are coming...");
			}

			@Override
			public void onScanFailed(int errorCode) {
			}

			// old 4.3 one, no used, but leave it here for now.
			public synchronized void onScan(final BluetoothDevice device,
					int rssi, byte[] scanRecord) {

				ScannedBleDevice d = ParseRawScanRecord(device, rssi,
						scanRecord, UuidMatcher);
				// could be the one filtered or exceptioned.
				if (d == null) {
					return;
				}

				// Log.e(LOG_TAG,
				// "onLeScan(...), rssi: " + rssi + ", distance: "
				// + Util.CalculateAccuracy(-59, rssi) * 100
				// + ", scanRecord: "
				// + Util.BytesToHexString(scanRecord, " "));
				syncLock.lock();
				try {
					fingerprints.add(d);
				} finally {
					syncLock.unlock();
				}

				// + " in fingerprints hashtable.");
			}
		};
	}

	private ScannedBleDevice ParseRawScanRecord(BluetoothDevice device,
			int rssi, byte[] advertisedData, byte[] uuidMatcher) {
		try {
			/*
			 * raw data is like this 02 01 1A 1A FF 4C 00 02 15 E2 C5 6D B5 DF
			 * FB 48 D2 B0 60 D0 F5 A7 10 96 E0 00 0A 00 08 C5 09 09 42 79 74 65
			 * 72 65 61 6C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
			 * 00 00 00 00
			 */

			/*
			 * 02 (Number of bytes that follow in first AD structure) 01 (Flags
			 * AD type) 1A (Flags value 0x1A = 000011010 ) 1A (Number of bytes
			 * that follow in second (and last) AD structure) FF (Manufacturer
			 * specific data AD type) 4C 00 (Company identifier code (0x004C ==
			 * Apple)) 02 (Byte 0 of iBeacon advertisement indicator) 15 (Byte 1
			 * of iBeacon advertisement indicator) E2 C5 6D B5 DF FB 48 D2 B0 60
			 * D0 F5 A7 10 96 E0 (iBeacon proximity uuid) 00 0A (major) 00 08
			 * (minor) C5 (Tx power) 09 09 42 79 74 (checksum) 65 72 65 61 6C 00
			 * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
			 * (unknown yet!!!)
			 */
			if (advertisedData[7] != 2 || advertisedData[8] != 21) {
				Log.e(LOG_TAG, device.getName()
						+ " was skipped due to unknown bit 7 and 21");
				return null;
			}

			ScannedBleDevice parsedObj = new ScannedBleDevice();
			// parsedObj.BLEDevice = device;
			parsedObj.DeviceName = device.getName();
			parsedObj.MacAddress = device.getAddress();
			parsedObj.RSSI = rssi;
			List<UUID> uuids = new ArrayList<UUID>();

			int skippedByteCount = advertisedData[0];
			// start from 1A
			int magicStartIndex = skippedByteCount + 1;
			// end at C5 and include C5.
			int magicEndIndex = magicStartIndex
					+ advertisedData[magicStartIndex] + 1;
			ArrayList<Byte> magic = new ArrayList<Byte>();
			for (int i = magicStartIndex; i < magicEndIndex; i++) {
				magic.add(advertisedData[i]);
			}

			byte[] companyId = new byte[2];
			companyId[0] = magic.get(2);
			companyId[1] = magic.get(3);
			parsedObj.CompanyId = companyId;

			byte[] ibeaconProximityUUID = new byte[16];
			for (int i = 0; i < 16; i++) {
				ibeaconProximityUUID[i] = magic.get(i + 6);
			}

			if (uuidMatcher != null) {
				if (ibeaconProximityUUID.length != uuidMatcher.length) {
					Log.e(LOG_TAG,
							"Scanned UUID: "
									+ Util.BytesToHexString(
											ibeaconProximityUUID, " ")
									+ " filtered by UUID Matcher "
									+ Util.BytesToHexString(uuidMatcher, " ")
									+ " with length requirment.");
					return null;
				}

				for (int i = 0; i < 16; i++) {
					if (ibeaconProximityUUID[i] != uuidMatcher[i]) {
						Log.e(LOG_TAG,
								"Scanned UUID: "
										+ Util.BytesToHexString(
												ibeaconProximityUUID, " ")
										+ " filtered by UUID Matcher "
										+ Util.BytesToHexString(uuidMatcher,
												" "));
						return null;
					}
				}

			}

			parsedObj.IbeaconProximityUUID = ibeaconProximityUUID;

			byte[] major = new byte[2];
			major[0] = magic.get(22);
			major[1] = magic.get(23);
			parsedObj.Major = major;

			byte[] minor = new byte[2];
			minor[0] = magic.get(24);
			minor[1] = magic.get(25);
			parsedObj.Minor = minor;

			byte tx = 0;
			tx = magic.get(26);
			parsedObj.Tx = tx;

			parsedObj.ScannedTime = new Date().getTime();
			return parsedObj;
		} catch (Exception ex) {
			Log.e(LOG_TAG, "skip one unknow format data...");
			// Log.e(LOG_TAG,
			// "Exception in ParseRawScanRecord with advertisedData: "
			// + Util.BytesToHexString(advertisedData, " ")
			// + ", detail: " + ex.getMessage());
			return null;
		}
	}

	// singleton instance
	public static BleFingerprintCollector getDefault() {
		return defaultInstance;
	}

	private Thread intervalThread;
	private Thread autoOnAndOffScanThread;

	/**
	 * just simply turn on the ble scan on device, the underlying ble scan is on
	 * processing, but data would not notifying by OnBleSampleCollectedListener
	 * until call the: void StartSampling(int reportInterval).
	 * <p>
	 * Typically Call this function only onetime, it's life cycle should same
	 * with your APP.
	 */
	public boolean TurnOn(BluetoothAdapter bluetoothAdapter,
			final ScanSettings settings) {
		if (this.IsStarted.compareAndSet(false, true)) {
			if (bluetoothAdapter == null) {
				throw new IllegalArgumentException(
						"'BluetoothAdapter' must be specified.");
			}

			// Ensures Bluetooth is available on the device and it is
			// enabled.
			if (!bluetoothAdapter.isEnabled()) {
				Log.e(LOG_TAG,
						"Bluetooth is disabled in device, failed to TurnOn...");
				return false;
			}

			this.mBluetoothAdapter = bluetoothAdapter;
			this.mLescanner = this.mBluetoothAdapter.getBluetoothLeScanner();

			try {
				syncLock.lock();
				this.fingerprints.clear();
			} finally {
				syncLock.unlock();
			}
			Log.e(LOG_TAG,
					"!!!!!!sbody wants to start a scanning(but no data popup)...");
			final List<ScanFilter> filters = new ArrayList<ScanFilter>();

			BleFingerprintCollector.this.autoOnAndOffScanThread = new Thread(
					new Runnable() {
						@Override
						public void run() {
							while (BleFingerprintCollector.this.IsStarted.get()) {
								try {
									// Spin check, sleep a while for avoid too
									// much CPU cost.
									Thread.sleep(100);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								// sampling report interval time is specified
								// valid,
								// now notify the listeners.
								while (BleFingerprintCollector.this.shouldStartSampling) {
									// due to device limitation, need do this
									// open and shut.
									try {
										BleFingerprintCollector.this.mLescanner
												.startScan(filters, settings,
														mLeScanCallback);
										try {
											// give a chance to quit early once
											// the
											// StopSampling() called during the
											// reportInterval waiting.
											boolean shouldBreakEarlier = false;
											// 100ms as a step
											int splitPieceCount = BleFingerprintCollector.this.startAndStopScanInterval / 100;
											int splitSleepTime = BleFingerprintCollector.this.startAndStopScanInterval
													/ splitPieceCount;
											for (int i = 0; i < splitPieceCount; i++) {
												Thread.sleep(splitSleepTime);
												if (!BleFingerprintCollector.this.shouldStartSampling) {
													shouldBreakEarlier = true;
													break;
												}
											}

											CopyOnWriteArrayList<ScannedBleDevice> syncedFingerprints = new CopyOnWriteArrayList<ScannedBleDevice>(
													fingerprints);
											for (OnBleSampleCollectedListener l : listeners) {
												l.onSampleCollected(syncedFingerprints);
											}

											fingerprints.clear();
											if (shouldBreakEarlier) {
												break;
											}
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									} finally {
										BleFingerprintCollector.this.mLescanner
												.stopScan(mLeScanCallback);
									}
								}
							}
						}
					});
			autoOnAndOffScanThread.start();
			return true;
		} else {
			Log.e(LOG_TAG,
					"Previous process is still running, stop it first and then re-try");
			return false;
			// throw new UnsupportedOperationException(
			// "Previous process is still running, stop it and then retry");
		}
	}

	public void TurnOff() {
		Log.e(LOG_TAG, "#############sbody wants to TurnOff the scanning...");
		if (this.mBluetoothAdapter != null && this.mLescanner != null)
			this.mLescanner.stopScan(this.mLeScanCallback);
		this.listeners.clear();
		this.IsStarted.set(false);
	}

	/**
	 * The OnBleSampleCollectedListener will be fired in each ble scan cycle
	 * 
	 */
	public void StartSampling() {
		if (!this.IsStarted.get()) {
			throw new OperationCanceledException(
					"BleFingerprintCollector is not turned on yet, StartSampling failed.");
		}

		syncLock.lock();
		fingerprints.clear();
		syncLock.unlock();

		this.shouldStartSampling = true;
	}

	/**
	 * stop the scanning, but the latest scanned data still could be fired out
	 * one time after you called this function
	 * 
	 */
	public void StopSampling() {
		if (!this.IsStarted.get()) {
			return;
			// throw new OperationCanceledException(
			// "BleFingerprintCollector is not turned on yet, StopSampling failed.");
		}

		this.shouldStartSampling = false;
	}
}
