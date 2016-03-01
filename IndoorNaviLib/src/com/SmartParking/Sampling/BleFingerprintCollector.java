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

import android.annotation.TargetApi;
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
import android.os.Build;
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
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback note2mLeScanCallback;
    private BluetoothLeScanner mLescanner;
    // this value was get by massive testing, do not change it unless you know what are u doing.
    private int startAndStopScanInterval = 1100;
    private static final BleFingerprintCollector defaultInstance = new BleFingerprintCollector();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings getBleScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        // .setReportDelay(20)
                .build();
    }

    /**
     * Indicator for if the Ble Scan is TurnedOn.
     * <p/>
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
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            setupLollipopmLeScanCallback();
        } else {
            setupNote2mLeScanCallback();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupLollipopmLeScanCallback() {
        /* setup the scanning callback... */
        this.mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // Official doc said: callbackType Determines how this callback
                // was triggered. Currently could only be
                // CALLBACK_TYPE_ALL_MATCHES.
                BluetoothDevice device = result.getDevice();
                // Log.i(LOG_TAG, "Wow   in onScanResult, " + device.getName()
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

                // Log.i(LOG_TAG,
                // "onLeScan(...), rssi: " + rssi + ", distance: "
                // + Util.CalculateAccuracy(-59, rssi) * 100
                // + ", scanRecord: "
                // + Util.BytesToHexString(scanRecord, " "));
                syncLock.lock();
                try {
                    // Log.i(LOG_TAG, "Add inlock");
                    fingerprints.add(d);
                    // Log.i(LOG_TAG, "releasing Add inlock");
                } finally {
                    syncLock.unlock();
                    // Log.i(LOG_TAG, "released Add inlock");
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.i(LOG_TAG, "in onBatchScanResults, " + results.size()
                        + " results are coming...");
            }

            @Override
            public void onScanFailed(int errorCode) {
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setupNote2mLeScanCallback() {
        this.note2mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi,
                                 byte[] scanRecord) {
                Log.v(LOG_TAG, "in onLeScan(NOTE2), " + Util.BytesToHexString(scanRecord) + " is coming...");
                ScannedBleDevice d = ParseRawScanRecord(device, rssi,
                        scanRecord, UuidMatcher);
                if (d != null) {
                    syncLock.lock();
                    try {
                        fingerprints.add(d);
                        Log.v(LOG_TAG, "Parsed one IBeacon package in NOTE2 Scan: " + d.toSimpleString());
                    } finally {
                        syncLock.unlock();
                    }
                }
            }
        };
    }

    private ScannedBleDevice ParseRawScanRecord(BluetoothDevice device,
                                                int rssi, byte[] advertisedData, byte[] uuidMatcher) {
        try {
            Log.v(LOG_TAG, "Try parsing advertise data: " + Util.BytesToHexString(advertisedData));
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
                Log.v(LOG_TAG, device.getName()
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
                    Log.i(LOG_TAG,
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
                        Log.i(LOG_TAG,
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
            Log.i(LOG_TAG, "skip one unknow format data...");
            // Log.i(LOG_TAG,
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createAutoOnAndOffScanThread() {
        this.autoOnAndOffScanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (BleFingerprintCollector.this.IsStarted.get()) {
                    try {
                        // Spin check, sleep a while, avoid too much CPU cost.
                        Thread.sleep(100);
                        while (BleFingerprintCollector.this.shouldStartSampling) {
                            // due to device limitation, need do this
                            // open and shut, this is no need for NOTE2
                            BleFingerprintCollector.this.mLescanner
                                    .startScan(new ArrayList<ScanFilter>(), getBleScanSettings(),
                                            mLeScanCallback);
                            // give a chance to quit early once the StopSampling() called in during the reportInterval waiting.
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

                                    /* for avoid blocking access to 'fingerprints' to long in LeScancallback, we copy the list here and send to
                                    * Listeners
                                    */
                            List<ScannedBleDevice> copiedFingerprints;
                            syncLock.lock();
                            try {
                                copiedFingerprints = new ArrayList<>(fingerprints);
                                fingerprints.clear();
                            } finally {
                                syncLock.unlock();
                            }

                            for (OnBleSampleCollectedListener l : listeners) {
                                l.onSampleCollected(copiedFingerprints);
                            }

                            if (shouldBreakEarlier) break;

                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        BleFingerprintCollector.this.mLescanner
                                .stopScan(mLeScanCallback);
                    }
                }
            }
        }
        );
    }

    private Thread autoOnAndOffScanThread;

    /*
    * the Note2 scan no need stop and start the ble device.
    **/
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createNote2ScanThread() {
        note2ScanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (BleFingerprintCollector.this.IsStarted.get()) {
                    try {
                        // Spin check, sleep a while for avoid too much CPU cost.
                        Thread.sleep(100);
                        // sampling report interval time is specified
                        // valid,
                        // now notify the listeners.
                        while (BleFingerprintCollector.this.shouldStartSampling) {
                            // give a chance to quit early once the StopSampling() called in during the reportInterval waiting.
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

                           /* for avoid blocking access to 'fingerprints' to long in LeScancallback, we copy the list here and send to
                            * Listeners
                            */
                            List<ScannedBleDevice> copiedFingerprints;
                            syncLock.lock();
                            try {
                                copiedFingerprints = new ArrayList<>(fingerprints);
                                fingerprints.clear();
                            } finally {
                                syncLock.unlock();
                            }
                            for (OnBleSampleCollectedListener l : listeners) {
                                l.onSampleCollected(copiedFingerprints);
                            }

                            if (shouldBreakEarlier) break;
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        );
    }

    private Thread note2ScanThread;

    /**
     * just simply turn on the ble scan on device, the underlying ble scan is on
     * processing, but data would not notifying by OnBleSampleCollectedListener
     * until call the: void StartSampling(int reportInterval).
     * <p/>
     * Typically Call this function only onetime, it's life cycle should same
     * with your APP.
     */
    public boolean TurnOn(BluetoothAdapter bluetoothAdapter) {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion <= Build.VERSION_CODES.KITKAT) {
            return turnOnForNote2(bluetoothAdapter);
        } else {
            return turnOn(bluetoothAdapter, getBleScanSettings());
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean turnOn(BluetoothAdapter bluetoothAdapter,
                           final ScanSettings settings) {
        if (this.IsStarted.compareAndSet(false, true)) {
            if (bluetoothAdapter == null) {
                throw new IllegalArgumentException(
                        "'BluetoothAdapter' must be specified.");
            }

            // Ensures Bluetooth is available on the device and it is
            // enabled.
            if (!bluetoothAdapter.isEnabled()) {
                Log.i(LOG_TAG,
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
            Log.i(LOG_TAG,
                    "!!!!!!sbody wants to start a scanning(but no data popup)...");
            this.createAutoOnAndOffScanThread();
            autoOnAndOffScanThread.start();
            return true;
        } else {
            Log.i(LOG_TAG,
                    "Previous process is still running, stop it first and then re-try");
            return false;
            // throw new UnsupportedOperationException(
            // "Previous process is still running, stop it and then retry");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean turnOnForNote2(BluetoothAdapter bluetoothAdapter) {
        if (this.IsStarted.compareAndSet(false, true)) {
            if (bluetoothAdapter == null) {
                throw new IllegalArgumentException(
                        "'BluetoothAdapter' must be specified.");
            }

            // Ensures Bluetooth is available on the device and it is
            // enabled.
            if (!bluetoothAdapter.isEnabled()) {
                Log.i(LOG_TAG,
                        "Bluetooth is disabled in device, failed to TurnOn...");
                return false;
            }

            this.mBluetoothAdapter = bluetoothAdapter;
            try {
                syncLock.lock();
                this.fingerprints.clear();
            } finally {
                syncLock.unlock();
            }

            this.mBluetoothAdapter.startLeScan(this.note2mLeScanCallback);
            Log.i(LOG_TAG,
                    "!!!!!!sbody wants to start a (note2) scanning...");
            this.createNote2ScanThread();
            this.note2ScanThread.start();
            return true;
        } else {
            Log.i(LOG_TAG,
                    "Previous process is still running, stop it first and then re-try");
            return false;
            // throw new UnsupportedOperationException(
            // "Previous process is still running, stop it and then retry");
        }
    }


    public void TurnOff() {
        Log.i(LOG_TAG, "#############sbody wants to TurnOff the scanning...");
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion <= Build.VERSION_CODES.KITKAT) {
            turnOffNote2();
        } else {
            turnOff();
        }
        this.listeners.clear();
        this.IsStarted.set(false);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void turnOffNote2() {
        this.mBluetoothAdapter.stopLeScan(this.note2mLeScanCallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void turnOff() {
        if (this.mBluetoothAdapter != null && this.mLescanner != null)
            this.mLescanner.stopScan(this.mLeScanCallback);
    }

    /**
     * The OnBleSampleCollectedListener will be fired in each ble scan cycle
     */
    public void StartSampling() {
        if (!this.IsStarted.get()) {
            throw new OperationCanceledException(
                    "BleFingerprintCollector is not turned on yet, StartSampling failed.");
        }

        syncLock.lock();
        fingerprints.clear();
        syncLock.unlock();
        this.startAndStopScanInterval = 1100;
        this.shouldStartSampling = true;
    }

    /**
     * For now, the Sangsum NOTE2 works perfectly for ble scan on Android 4.4, no need to stop and start, so created a dedicated function for this device.
     * The OnBleSampleCollectedListener will be fired in each ble scan cycle
     */
    public void StartNote2Sampling(int reportingInterval) {
        if (!this.IsStarted.get()) {
            throw new OperationCanceledException(
                    "BleFingerprintCollector is not turned on yet, StartSampling failed.");
        }

        syncLock.lock();
        fingerprints.clear();
        syncLock.unlock();
        this.startAndStopScanInterval = reportingInterval;
        this.shouldStartSampling = true;
    }

    /**
     * stop the scanning, but the latest scanned data still could be fired out
     * one time after you called this function
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
