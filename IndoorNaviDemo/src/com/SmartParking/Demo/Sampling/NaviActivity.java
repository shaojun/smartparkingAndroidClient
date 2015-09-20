package com.SmartParking.Demo.Sampling;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Demo.Sampling.Helper.ParkingPositionStatus;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.BleFingerprintCollector;
//import com.SmartParking.Sampling.MovementDetector;
import com.SmartParking.Sampling.MovementState;
import com.SmartParking.Sampling.OnBleSampleCollectedListener;
import com.SmartParking.Sampling.OnMovementListener;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.DrawCircle;
import com.SmartParking.UI.DrawImage;
import com.SmartParking.UI.MarkableTouchImageView;
import com.SmartParking.UI.OnBitmapInTouchImageClickedListener;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Tuple3;
import com.SmartParking.Util.Tuple5;
import com.SmartParking.Util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.ActionBar.LayoutParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseSettings.Builder;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.ortiz.touch.TouchImageView.State;

public class NaviActivity extends Activity implements
		OnBleSampleCollectedListener, OnBitmapInTouchImageClickedListener {
	// private int samplingIntervalInIdle = 5000;
	// private int samplingIntervalInWalking = 2000;
	// private int samplingIntervalInRunning = 1000;
	/*
	 * <acc, samplingBuffer> private Tuple<Integer, Integer> IdleState = new
	 * Tuple<Integer, Integer>(1, 5000); private Tuple<Integer, Integer>
	 * SlowWalkingState = new Tuple<Integer, Integer>( 2, 4000); private
	 * Tuple<Integer, Integer> WalkingState = new Tuple<Integer, Integer>( 3,
	 * 3000); private Tuple<Integer, Integer> SlowRunningState = new
	 * Tuple<Integer, Integer>( 4, 2000); private Tuple<Integer, Integer>
	 * FastRunningState = new Tuple<Integer, Integer>( 5, 1000); private
	 * ArrayList<Tuple<Integer, Integer>> states = new ArrayList<Tuple<Integer,
	 * Integer>>();
	 */
	private static final String LOG_TAG = "SmarkParking.Demo.Navi";
	private MarkableTouchImageView image = null;
	private TextView logTextView = null;
	private CheckBox exposeMeCheckBox = null;
	// every 4 scan cycle will trigger a position draw.
	private Integer bufferTimes = 4;

	private BluetoothAdapter mBluetoothAdapter = null;
	private AdvertiseCallback mAdvertiseCallback = null;
	private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;

	private EditText uuidEditText = null;
	private EditText uuid2EditText = null;
	private EditText uuid3EditText = null;

	private ArrayList<PositionDescriptor> loadedBuildInSampleData = null;
	// only similarity >= this value get UI refresh
	private int similarityThreashold = 75;
	private PowerManager.WakeLock screenOnLock = null;
	// X,Y and its comments text.
	private List<Tuple<Tuple<Float, Float>, String>> allCandidatesWithText = new ArrayList<Tuple<Tuple<Float, Float>, String>>();

	private boolean keepPollingAllParkingPositionsFromWeb = true;
	// 100 px equal how many real life meters, like say the value is 5, means
	// 100px in bitmap equal 5meters, default set to 2.
	private float mapScale = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navi);
		this.exposeMeCheckBox = (CheckBox) findViewById(R.id.ExposeMeBtn);
		this.uuidEditText = (EditText) findViewById(R.id.UuidEditText);
		this.uuid2EditText = (EditText) findViewById(R.id.Uuid2EditText);
		this.uuid3EditText = (EditText) findViewById(R.id.Uuid3EditText);

		Intent intent = getIntent();
		String mapScaleValue = intent.getStringExtra("mapScale");
		if (mapScaleValue != null && mapScaleValue != "") {
			this.mapScale = Float.parseFloat(mapScaleValue);
		}

		this.image = (MarkableTouchImageView) findViewById(R.id.imgControl);
		this.image.AddBitmapInTouchImageClickedListener(this);
		this.image.setHighlightSelectedBitmap(BitmapFactory.decodeResource(
				getResources(), R.drawable.car_selected_arrow));
		Button myButton = new Button(NaviActivity.this);
		myButton.setText("Order?");

		this.image.setParentRelativeLayoutAndMenuView(
				(RelativeLayout) findViewById(R.id.imageViewRelativeLayout),
				myButton);
		this.image.setScaleType(ScaleType.CENTER_CROP);
		Drawable mapDrawable = new BitmapDrawable(getResources(),
				EntryActivity.SelectedBuildingMap);
		this.image.setImageDrawable(mapDrawable);
		// ============================

		this.logTextView = (TextView) findViewById(R.id.logTextView);
		this.mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
				.getAdapter();
		if (this.mBluetoothAdapter == null
				|| !this.mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 0);
		}

		Log.e(LOG_TAG, "BleFingerprintCollector is running: "
				+ BleFingerprintCollector.getDefault().IsStarted.get());
		if (!BleFingerprintCollector.getDefault().IsStarted.get()
				&& false == BleFingerprintCollector.getDefault().TurnOn(
						mBluetoothAdapter, MainActivity.BleScanSettings)) {
			Toast.makeText(getBaseContext(),
					"Failed to start BleFingerprintCollector",
					android.widget.Toast.LENGTH_LONG).show();
			Log.e(LOG_TAG, "Failed to start BleFingerprintCollector, quit");
			return;
		}

		this.mBluetoothLeAdvertiser = this.mBluetoothAdapter
				.getBluetoothLeAdvertiser();

		this.mAdvertiseCallback = new AdvertiseCallback() {
			@Override
			public void onStartSuccess(AdvertiseSettings settingsInEffect) {
				super.onStartSuccess(settingsInEffect);
				Log.e(LOG_TAG, "BLE STARTED ADVERTISING BROADCAST succeed "
						+ settingsInEffect);
			}

			@Override
			public void onStartFailure(int errorCode) {
				super.onStartFailure(errorCode);
				NaviActivity.this.uuidEditText.setEnabled(true);
				NaviActivity.this.uuid2EditText.setEnabled(true);
				NaviActivity.this.uuid3EditText.setEnabled(true);
				NaviActivity.this.exposeMeCheckBox.setChecked(false);
				Log.e(LOG_TAG, "BLE ADVERTISING FAILED TO START: " + errorCode);
				Toast.makeText(getBaseContext(),
						"Failed to send ble advertising",
						android.widget.Toast.LENGTH_SHORT).show();
			}
		};
		final Builder advSettingsBuilder = new AdvertiseSettings.Builder()
				.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
				.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
				.setConnectable(false);
		this.exposeMeCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (NaviActivity.this.mBluetoothLeAdvertiser == null) {
							Log.e(LOG_TAG,
									"No support for ble advertising due to null mBluetoothLeAdvertiser.");
							Toast.makeText(getBaseContext(),
									"No Support for ble advertising",
									android.widget.Toast.LENGTH_LONG).show();
							return;
						}

						if (isChecked) {
							NaviActivity.this.uuidEditText.setEnabled(false);
							NaviActivity.this.uuid2EditText.setEnabled(false);
							NaviActivity.this.uuid3EditText.setEnabled(false);
							Log.e(LOG_TAG, "startAdvertising...");
							NaviActivity.this.mBluetoothLeAdvertiser.startAdvertising(
									advSettingsBuilder.build(),
									getAdvertiseData(),
									NaviActivity.this.mAdvertiseCallback);
						} else {
							Log.e(LOG_TAG, "stopAdvertising...");
							NaviActivity.this.uuidEditText.setEnabled(true);
							NaviActivity.this.uuid2EditText.setEnabled(true);
							NaviActivity.this.uuid3EditText.setEnabled(true);
							NaviActivity.this.mBluetoothLeAdvertiser
									.stopAdvertising(NaviActivity.this.mAdvertiseCallback);
						}

					}

				});

		/******************************
		 * make sure the screen always on.
		 */
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		this.screenOnLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, LOG_TAG);
		this.screenOnLock.acquire();

		Button buttonShowAllSamplingPoint = (Button) findViewById(R.id.buttonShowAllSamplingPoint);
		buttonShowAllSamplingPoint.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (loadedBuildInSampleData == null
						|| loadedBuildInSampleData.size() == 0) {
					image.cleanAllCircles();
					return;
				}

				List<DrawCircle> positions = new ArrayList<DrawCircle>();
				for (PositionDescriptor pd : loadedBuildInSampleData) {
					positions.add(new DrawCircle(
							pd.getTransferredXByImageView(image),
							pd.getTransferredYByImageView(image),
							Helper.GetCircleRadiusByMapScale(NaviActivity.this.mapScale),
							pd.Description, Color.BLACK));

				}

				image.drawMultipleCircles(positions);
			}
		});

		BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(
				this);
		// default choose Idle sampling interval
		BleFingerprintCollector.getDefault().StartSampling();

		this.loadedBuildInSampleData = Helper.LoadSamplingData(
				Helper.privateDataFileName, this);
		if (loadedBuildInSampleData == null
				|| loadedBuildInSampleData.size() == 0) {
			Toast.makeText(
					getBaseContext(),
					"Failed to load buildIn sample data, positioning will not work",
					android.widget.Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG,
					"Failed to load buildIn sample data, but won't quit.");
			BleFingerprintCollector.getDefault().StopSampling();
			// return;
		}

	}

	@Override
	protected void onStart() {
		super.onStart(); // Always call the superclass method first
		// polling to show the parking position from web.
		new Thread(new Runnable() {
			public void run() {
				while (keepPollingAllParkingPositionsFromWeb) {
					List<Tuple5<Float, Float, String, ParkingPositionStatus, Integer>> allParkingPositions = Helper
							.GetAllParkingPositionsFromWeb(9999);
					final List<DrawImage> drawImages = Helper
							.ConvertParkingPostionsFromWebToDrawImages(
									allParkingPositions, getResources());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// long gap = System.currentTimeMillis()
							// -
							// NaviActivity.this.lastSuccessfullyRefreshUiMillis;
							// logTextView.setText("from last refresh (by ms): "
							// + gap);
							image.drawMultipleCirclesAndImages(null, drawImages);
						}
					});

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// break;
					// ========================================================
				}
			}
		}).start();
	}

	private AdvertiseData getAdvertiseData() {
		final byte[] manufacturerData = new byte[23];
		ByteBuffer byteBuffer = ByteBuffer.wrap(manufacturerData);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		// 02 (Byte 0 of iBeacon advertisement indicator)
		// 15 (Byte 1 of iBeacon advertisement indicator)
		byteBuffer.put((byte) 0x02);
		byteBuffer.put((byte) 0x15);

		// proximity uuid
		final UUID uuid = UUID.fromString(this.uuidEditText.getText()
				.toString());
		// most 64 append
		byteBuffer.putLong(uuid.getMostSignificantBits());
		// least 64 append
		byteBuffer.putLong(uuid.getLeastSignificantBits());

		// major
		byteBuffer
				.putShort((short) Util.hexStringToByteArray(this.uuid2EditText
						.getText().toString())[0]);
		// minor
		byteBuffer
				.putShort((short) Util.hexStringToByteArray(this.uuid3EditText
						.getText().toString())[0]);
		// Tx Power
		byteBuffer.put((byte) 0x99);

		// 4C 00 (Company identifier code (0x004C == Apple))
		final int appleManufactureId = 0x004C;

		final AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.addManufacturerData(appleManufactureId, manufacturerData);
		return dataBuilder.build();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = false;
		this.screenOnLock.release();
		Log.e(LOG_TAG, "NaviActivity stopping the BleFingerprintCollector");
		BleFingerprintCollector.getDefault().StopSampling();
		BleFingerprintCollector.getDefault()
				.RemoveOnBleSampleCollectedListener(this);
		// BleFingerprintCollector.getDefault().Stop();
	}

	protected void onRestart() {
		super.onRestart(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = true;
		this.screenOnLock.acquire();
		BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(
				this);
		BleFingerprintCollector.getDefault().StartSampling();
		if (!BleFingerprintCollector.getDefault().IsStarted.get()
				&& false == BleFingerprintCollector.getDefault().TurnOn(
						mBluetoothAdapter, MainActivity.BleScanSettings)) {
			Toast.makeText(getBaseContext(), "Failed to start collect FP",
					android.widget.Toast.LENGTH_LONG).show();
			return;
		}
	}

	@Override
	protected void onPause() {
		super.onPause(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = false;
		BleFingerprintCollector.getDefault().StopSampling();
	}

	List<ScannedBleDevice> rawFingerprints = new ArrayList<ScannedBleDevice>();
	Integer currentBufferTimes = 0;

	@Override
	public void onSampleCollected(List<ScannedBleDevice> samples) {
		if (samples != null)
			this.rawFingerprints.addAll(samples);
		this.currentBufferTimes++;
		if (this.currentBufferTimes >= this.bufferTimes) {
			if (this.rawFingerprints.size() == 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(
								getBaseContext(),
								"Nothing was collected, try extend the scanning bufferTime",
								android.widget.Toast.LENGTH_SHORT).show();
					}
				});
				return;
			}
			
			this.HandleScannedRawFingerprints(this.rawFingerprints);
			this.currentBufferTimes = 0;
			this.rawFingerprints = new ArrayList<ScannedBleDevice>();
		}
	}

	private int selectedBitMapId = -1;
	ProgressDialog orderingProgressDialog = null;

	@Override
	public void onBitMapClicked(final int touchedBitMapId) {
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibe.vibrate(100);
		selectedBitMapId = touchedBitMapId;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(NaviActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle("see?")
						.setMessage(
								"Want to order position " + selectedBitMapId
										+ "?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										SendOrderOrRevertParkingPositionRequest(false);
									}
								})
						.setNegativeButton("No", null)
						.setNeutralButton("revert",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										SendOrderOrRevertParkingPositionRequest(true);
									}
								}).show();
				logTextView.setText("touched bitmap: " + touchedBitMapId);

			}
		});
	}

	private void HandleScannedRawFingerprints(
			List<ScannedBleDevice> rawFingerprints) {
		HashSet<ScannedBleDevice> averagedFingerprints = Util
				.DistinctAndAvgFingerprint(rawFingerprints);
		Log.e(LOG_TAG, "averagedFingerprints collected:");
		Log.e(LOG_TAG, Helper.ToLogString0(averagedFingerprints));
		Log.e(LOG_TAG, "averagedFingerprints collected finished.");
		// need calculate the similarity against all the BuildInSampleData
		final ArrayList<Tuple<Double, PositionDescriptor>> sortedSimilarityList = new ArrayList<Tuple<Double, PositionDescriptor>>();
		if (this.loadedBuildInSampleData != null
				&& this.loadedBuildInSampleData.size() > 0) {
			for (PositionDescriptor pd : this.loadedBuildInSampleData) {
				sortedSimilarityList.add(new Tuple<Double, PositionDescriptor>(
						Util.CaculateSimilarity(averagedFingerprints,
								pd.Fingerprints), pd));
			}
		} else {
			return;
		}

		Collections.sort(sortedSimilarityList, new SimilarityComparator());
		Log.e(LOG_TAG, "similarity caculated:");
		Log.e(LOG_TAG, Helper.ToLogString1(sortedSimilarityList));
		Log.e(LOG_TAG, "similarity caculated finished");
		allCandidatesWithText.clear();

		// check the similarity to avoid 'false fly jumping'.
		final int currentBestSimilarity = (sortedSimilarityList
				.get(sortedSimilarityList.size() - 1).first).intValue();
		if (currentBestSimilarity < this.similarityThreashold) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(
							getBaseContext(),
							"Low selectivity(" + currentBestSimilarity
									+ "), won't show",
							android.widget.Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}
		System.currentTimeMillis();
		for (Tuple<Double, PositionDescriptor> single : sortedSimilarityList) {
			allCandidatesWithText.add(new Tuple<Tuple<Float, Float>, String>(
					new Tuple<Float, Float>(single.second.getTransferredXByImageView(image), single.second.getTransferredYByImageView(image)),
					single.second.Description + ", s: "
							+ single.first.intValue()));
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (!allCandidatesWithText.isEmpty()) {

					// Calendar c = Calendar.getInstance();
					// int seconds = c.get(Calendar.SECOND);
					// logTextView.setText("time: " + seconds + "\r\n" +
					// logString);
					// logTextView.setText("from last refresh (by ms): " + gap);
					// draw all points
					// image.drawCirclesByXandY(allCandidatesWithText);
					// only draw the highest similarity point
					image.drawSingleCircle(new DrawCircle(allCandidatesWithText
							.get(allCandidatesWithText.size() - 1).first.first,
							allCandidatesWithText.get(allCandidatesWithText
									.size() - 1).first.second,
							allCandidatesWithText.get(allCandidatesWithText
									.size() - 1).second, Color.BLACK));
				}
			}
		});
	}

	private void SendOrderOrRevertParkingPositionRequest(boolean revert) {
		final boolean isRevert = revert;
		NaviActivity.this.orderingProgressDialog = ProgressDialog.show(
				NaviActivity.this, "requesting...",
				"wait for put your order...");
		new Thread(new Runnable() {
			public void run() {
				final String webResponseContent = Helper
						.OrderOneParkingPosition(
								((EditText) findViewById(R.id.userNameEditText))
										.getText().toString(), Integer
										.toString(selectedBitMapId), isRevert);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						orderingProgressDialog.dismiss();
						new AlertDialog.Builder(NaviActivity.this)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.setTitle("operation result")
								.setMessage(webResponseContent)
								.setPositiveButton("OK", null).show();
					}
				});

			}
		}).start();
	}
}