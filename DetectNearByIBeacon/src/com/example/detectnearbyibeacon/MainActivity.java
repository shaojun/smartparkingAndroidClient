package com.example.detectnearbyibeacon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.BleFingerprintCollector;
import com.SmartParking.Sampling.OnBleSampleCollectedListener;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.ExpandableListViewAdapter;
import com.SmartParking.UI.ExpandableListViewItem;
import com.SmartParking.Util.Util;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
		OnBleSampleCollectedListener {
	private static final String LOG_TAG = "DetectNearbyIBeacon";
	private BluetoothAdapter mBluetoothAdapter = null;
	private PowerManager.WakeLock screenOnLock = null;
	private TextView contentTextView = null;
	private ExpandableListView nearByIBeaconExpandableListView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// this.contentTextView = (TextView) findViewById(R.id.contentTextView);
		this.nearByIBeaconExpandableListView = (ExpandableListView) findViewById(R.id.nearByIBeaconExpandableListView);

		/******************************
		 * make sure the Phone support and enabled the BLE.
		 */
		if (!this.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new UnsupportedOperationException(
					"Target device didn't support BLE.");
		}
		// Initializes Bluetooth adapter.
		this.mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
				.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 0);
		}

		if (!BleFingerprintCollector.getDefault().isRunning
				&& false == BleFingerprintCollector.getDefault().Start(
						mBluetoothAdapter, 2000)) {
			Toast.makeText(getBaseContext(), "Failed to start collect FP",
					android.widget.Toast.LENGTH_LONG).show();
			return;
		}

		BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(
				this);

		/******************************
		 * make sure the screen always on.
		 */
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		this.screenOnLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, LOG_TAG);
		this.screenOnLock.acquire();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	String logString = "";
	ExpandableListViewAdapter adapter = null;
	List<ExpandableListViewItem> itemList = new ArrayList<ExpandableListViewItem>();
	private Boolean onUserConfirmation = false;

	@Override
	public void onSampleCollected(List<ScannedBleDevice> samples) {
		Log.e(LOG_TAG, "onSampleCollected");
		// this.logString = "";
		if (onUserConfirmation)
			return;
		HashSet<ScannedBleDevice> averagedFingerprints = Util
				.DistinctAndAvgFingerprint(samples);

		int itemCounter = 0;
		for (ScannedBleDevice d : averagedFingerprints) {
			d.Distance = Util.CalculateAccuracy(d.Tx, d.RSSI) * 100;
			// Log.e(LOG_TAG, "Avg Rssi: " + d.RSSI + ", Avg Distance: "
			// + d.Distance);
			ExpandableListViewItem pendingParentNode = new ExpandableListViewItem(
					d.DeviceName + ", Mac: " + d.MacAddress);
			if (itemList.contains(pendingParentNode)) {
				ExpandableListViewItem existed = itemList.get(itemList
						.indexOf(pendingParentNode));
				existed.addChildItem(new ExpandableListViewItem("His RSSI: "
						+ Util.round(d.RSSI, 2) + ", Distance(cm): "
						+ Util.round(d.Distance, 2)));
				// Log.e(LOG_TAG, "one child added...");
				if (existed.getChildItemList().size() > 20) {
					// always remove the oldest one
					existed.getChildItemList().remove(0);
				}
			} else {
				itemList.add(pendingParentNode);
			}

			itemCounter++;
		}

		if (itemCounter > 0) {
			if (this.adapter == null) {
				adapter = new ExpandableListViewAdapter(this, itemList);
			} else {
				// adapter has been updated.
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					nearByIBeaconExpandableListView.setAdapter(adapter);
					//nearByIBeaconExpandableListView.expandGroup(0);
					Log.e(LOG_TAG, "itemList size: "
							+ MainActivity.this.itemList.size());
					for (int i = 0; i < MainActivity.this.itemList.size(); i++) {
						nearByIBeaconExpandableListView.expandGroup(i);
						Log.e(LOG_TAG, "expanded " + i);
					}
				}
			});
			for (ScannedBleDevice d : averagedFingerprints) {
				if (d.Distance < 5) {
					MainActivity.this.onUserConfirmation = true;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {

							// MainActivity.this.contentTextView.setText(logString);
							new AlertDialog.Builder(MainActivity.this)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.setTitle("authorize?")
									.setMessage("Want to authorize pump?")
									.setPositiveButton(
											"Yes",
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													AsyncTask<Integer, Integer, Integer> nxtWebRequest = new AsyncWebRequest();
													nxtWebRequest.execute(1, 2);
												}

											}).setNegativeButton("No", null)
									.show();
							MainActivity.this.onUserConfirmation = false;
						}
					});
				}
			}
		}
		if (!BleFingerprintCollector.getDefault().isRunning
				&& false == BleFingerprintCollector.getDefault().Start(
						mBluetoothAdapter, 1000)) {
			Toast.makeText(getBaseContext(), "Failed to start collect FP",
					android.widget.Toast.LENGTH_LONG).show();
			return;
		}
	}

	protected void onDestroy() {
		super.onDestroy();// Always call the superclass method first
		BleFingerprintCollector.getDefault().Stop();
	}

}
