package com.SmartParking.Demo.Sampling;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Demo.Sampling.Helper.ParkingPositionStatus;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.BleFingerprintCollector;
import com.SmartParking.Sampling.OnBleSampleCollectedListener;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.DrawCircle;
import com.SmartParking.UI.DrawImage;
import com.SmartParking.UI.ExpandableListViewAdapter;
import com.SmartParking.UI.ExpandableListViewItem;
import com.SmartParking.UI.MarkableTouchImageView;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Tuple5;
import com.SmartParking.Util.Util;
import com.ortiz.touch.TouchImageView.OnTouchImageViewListener;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class MainActivity extends Activity implements
		OnBleSampleCollectedListener {
	private static final String LOG_TAG = "SmarkParking.Demo.Main";
	private BluetoothAdapter mBluetoothAdapter = null;
	private PowerManager.WakeLock screenOnLock = null;
	private MarkableTouchImageView image;
	private TextView currentCoorTextView;
	private TextView logTextView;
	// private EditText commentsEditText;
	// private Button buttonSave;
	private Button buttonLoad;
	private Button buttonPersist;
	private Button buttonSampling;
	private ExpandableListView pendingSampleExpandableListView = null;

	private ProgressDialog samplingProgressDialog;
	private int defaultSamplingTime = 15000;

	private DecimalFormat df;
	// boolean enableSampling = false;
	// pan and zooming related...
	private RectF zoomedRect = null;
	private float currentZoom = 1;
	// private float currentAbsoluteY = 0;
	// private float currentAbsoluteX = 0;
	private Tuple<Float, Float> currentAbsoluteXandY;
	private float lastClickedX = 0;
	private float lastClickedY = 0;

	private List<ScannedBleDevice> collectedSample = new ArrayList<ScannedBleDevice>();
	public static ArrayList<PositionDescriptor> InMemPositionDescriptors = new ArrayList<PositionDescriptor>();
	private Lock syncLock = new ReentrantLock();
	private static int RESULT_LOAD_IMAGE = 1;
	private Handler waitSometimeHandler = new Handler();

	// the low selectivity detected, user might want to re-do sampling and
	// ignore this one.
	private boolean shouldIgnoreCurrentSample = false;
	private boolean keepPollingAllParkingPositionsFromWeb = true;
	// 100 px equal how many real life meters, like say the value is 5, means
	// 100px in bitmap equal 5meters. default set to 2.
	private float mapScale = 2;
	public static ScanSettings BleScanSettings = new ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
			// .setReportDelay(20)
			.build();

	@Override
	protected void onStop() {
		super.onStop(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = false;
		this.screenOnLock.release();
		// Log.e(LOG_TAG, "MainActivity stopping the BleFingerprintCollector");
		// BleFingerprintCollector.getDefault().Stop();
		// BleFingerprintCollector.getDefault().RemoveOnBleSampleCollectedListener(this);

	}

	protected void onRestart() {

		super.onRestart(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = true;
		this.screenOnLock.acquire();
		if (!BleFingerprintCollector.getDefault().IsStarted.get()
				&& false == BleFingerprintCollector.getDefault().TurnOn(
						mBluetoothAdapter, BleScanSettings)) {
			Toast.makeText(getBaseContext(), "Failed to start collect FP",
					android.widget.Toast.LENGTH_LONG).show();
			return;
		}
	}

	@Override
	protected void onPause() {
		super.onPause(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = false;
	}

	@Override
	protected void onResume() {
		super.onResume(); // Always call the superclass method first
	}

	@Override
	protected void onStart() {
		super.onStart(); // Always call the superclass method first
		this.keepPollingAllParkingPositionsFromWeb = true;

		// load the parking positions from web
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
							image.drawMultipleCirclesAndImages(null, drawImages);
						}
					});
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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

		/*
		 * if (GooglePlayServicesUtil
		 * .isGooglePlayServicesAvailable(getBaseContext()) !=
		 * ConnectionResult.SUCCESS) { Toast.makeText(getBaseContext(),
		 * "Connecting to google service failed",
		 * android.widget.Toast.LENGTH_LONG).show(); }
		 */

		/*
		 * make sure the BLE collector is on running, but we don't need the data
		 * for now.
		 */
		if (BleFingerprintCollector.getDefault().IsStarted.get()) {
			Toast.makeText(getBaseContext(),
					"FingerprintCollector already on running",
					android.widget.Toast.LENGTH_LONG).show();
		} else if (false == BleFingerprintCollector.getDefault().TurnOn(
				mBluetoothAdapter, BleScanSettings)) {
			Toast.makeText(getBaseContext(),
					"Failed to start FingerprintCollector, quit",
					android.widget.Toast.LENGTH_LONG).show();
			return;
		}

		/* does this device support BLE advertisement */
		if (!this.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
			Toast.makeText(getBaseContext(), "Unlikely support for advertisement",
					android.widget.Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getBaseContext(),
					"Congratulation you support ble advertisement~~~~",
					android.widget.Toast.LENGTH_LONG).show();
		}

		/******************************
		 * make sure the screen always on.
		 */
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		this.screenOnLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				LOG_TAG);
		this.screenOnLock.acquire();

		//
		// DecimalFormat rounds to 2 decimal places.
		//
		df = new DecimalFormat("#.##");
		// scrollPositionTextView = (TextView)
		// findViewById(R.id.scroll_position);
		// zoomedRectTextView = (TextView) findViewById(R.id.zoomed_rect);
		// currentZoomTextView = (TextView) findViewById(R.id.current_zoom);

		// =======================================
		Intent intent = getIntent();
		String mapScaleValue = intent.getStringExtra("mapScale");
		if (mapScaleValue != null && mapScaleValue != "") {
			this.mapScale = Float.parseFloat(mapScaleValue);
		}

		image = (MarkableTouchImageView) findViewById(R.id.imgControl);
		image.setScaleType(ScaleType.CENTER_CROP);
		Drawable mapDrawable = new BitmapDrawable(getResources(),
				EntryActivity.SelectedBuildingMap);
		image.setImageDrawable(mapDrawable);
		//
		// Set the OnTouchImageViewListener which updates the global zoom object
		//
		image.setOnTouchImageViewListener(new OnTouchImageViewListener() {
			@Override
			public void onMove() {
				image.getScrollPosition();
				zoomedRect = image.getZoomedRect();
				currentZoom = image.getCurrentZoom();
				image.isZoomed();
			}
		});

		image.setOnTouchListener(new OnTouchListener() {
			// the GetX() and GetY() here is the coordinate in image area. not
			// the screen absolute.
			// more important, the GetX() and GetY() will be impact on image's
			// zooming.
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN: {

					// Log.e(LOG_TAG,
					// "image OnTouchListener, image.getHeight():"
					// + image.getHeight() + ", image.getWidth():"
					// + image.getWidth() + ", zoomedR.top: "
					// + zoomedRect.top + ", zoomedR.left:"
					// + zoomedRect.left + ", getX():"
					// + event.getX() + ", getY():" + event.getY()
					// + ", curZoom:" + currentZoom
					// + ", image.GetTop: " + image.getTop()
					// + ", image.GetLeft: " + image.getLeft());
					lastClickedX = event.getX();
					lastClickedY = event.getY();
					break;
				}
				case MotionEvent.ACTION_UP: {
					break;
				}
				}

				return true;
			}
		});

		image.setOnLongClickListener(new OnLongClickListener() {
			// draw a circle on the position you touched, the 'LongClick' did
			// not provide coordinate info, so have to use
			// the one in Touch event.
			@Override
			public boolean onLongClick(View v) {
				if (zoomedRect != null) {
					if (Float.isNaN(zoomedRect.top))
						zoomedRect.top = 0f;
					if (Float.isNaN(zoomedRect.left))
						zoomedRect.left = 0f;

					// we use the bitMap's actual size as the base to calculate
					// the absolute coordinate
					// currentAbsoluteY = zoomedRect.top
					// * image.getOnImageLoadHeight() + lastClickedY
					// / currentZoom;
					// currentAbsoluteX = zoomedRect.left
					// * image.getOnImageLoadWidth() + lastClickedX
					// / currentZoom;
					MainActivity.this.currentAbsoluteXandY = Util
							.GetAbsoluteXAndYFromRelative(lastClickedX,
									lastClickedY, zoomedRect, currentZoom,
									image);
					// we want the circle has 0.5m radius.
					image.drawSingleCircle(new DrawCircle(
							MainActivity.this.currentAbsoluteXandY.first,
							MainActivity.this.currentAbsoluteXandY.second,
							Helper.GetCircleRadiusByMapScale(MainActivity.this.mapScale),
							"", Color.BLACK));
					if (zoomedRect != null) {

						// Log.e(LOG_TAG, "image OnLongClick, RawX:"
						// + lastClickedX + ", absX:" + currentAbsoluteX
						// + ", RawY:" + lastClickedY + ", absY:"
						// + currentAbsoluteY + ", scaledHeight: "
						// + scaledHeight + ", scaledWidth: "
						// + scaledWidth + ", zoLeft:" + zoomedRect.left
						// + ", zoTop:" + zoomedRect.top + ", zoBot:" +
						// zoomedRect.bottom+", curZoom: "
						// + currentZoom + ", actX: "
						// + additionalCoor[0] + ", actY: "
						// + additionalCoor[1] + ", actHeight: "
						// + additionalCoor[3] + ", actWidth: "
						// +
						// additionalCoor[2]+", imgLoadHeight: "+image.onImageLoadHeight
						// +", imgLoadWidth: "+image.onImageLoadWidth);
						image.drawSingleCircle(new DrawCircle(
								MainActivity.this.currentAbsoluteXandY.first,
								MainActivity.this.currentAbsoluteXandY.second,
								Helper.GetCircleRadiusByMapScale(MainActivity.this.mapScale),
								"", Color.BLACK));
					}
				}
				return true;
			}
		});
		// =================================

		currentCoorTextView = (TextView) findViewById(R.id.current_coordinate);
		logTextView = (TextView) findViewById(R.id.logTextView);
		// commentsEditText = (EditText) findViewById(R.id.commentsEditText);
		// buttonSave = (Button) findViewById(R.id.buttonSave);
		buttonLoad = (Button) findViewById(R.id.buttonLoad);
		buttonPersist = (Button) findViewById(R.id.buttonPersist);
		buttonSampling = (Button) findViewById(R.id.buttonSampling);
		pendingSampleExpandableListView = (ExpandableListView) findViewById(R.id.pendingSampleExpandableListView);

		Button buttonViewSampleData = (Button) findViewById(R.id.buttonViewSampleData);
		buttonViewSampleData.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this,
						ViewSamplingDataActivity.class);
				i.putExtra("TestIntent", "useless");
				startActivity(i);
			}
		});

		Button buttonStartNavi = (Button) findViewById(R.id.buttonStartNavi);
		buttonStartNavi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, NaviActivity.class));
			}
		});

		Button buttonSelectPic = (Button) findViewById(R.id.buttonSelectPic);
		buttonSelectPic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				{
					// for (int i = 0; i < testingLoopTimes; i++) {
					testBle();
					// }

					// for (int k : testingResult.keySet()) {
					// Log.e(LOG_TAG,
					// "Interval: " + k + ": " + testingResult.get(k));
					// }
				}
			}
		});

		Object data = Helper.ReadObjectFromFile(Helper.privateDataFileName,
				getBaseContext());
		if (data != null) {
			this.InMemPositionDescriptors = (ArrayList<PositionDescriptor>) data;
			Toast.makeText(
					getBaseContext(),
					"existed data loaded(total "
							+ this.InMemPositionDescriptors.size() + ")",
					android.widget.Toast.LENGTH_SHORT).show();
		}

		/******************************
		 * init the zoom and pan variables.
		 */
		zoomedRect = image.getZoomedRect();
		currentZoom = image.getCurrentZoom();
		image.isZoomed();

		// ============================
		BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(
				MainActivity.this);
		buttonSampling.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN: {
					if (MainActivity.this.currentAbsoluteXandY == null) {
						Toast.makeText(getBaseContext(), "No point selected",
								android.widget.Toast.LENGTH_SHORT).show();
						return true;
					}

					// MainActivity.this.collectedCycleCount.set(0);
					BleFingerprintCollector.getDefault().StartSampling();
					waitSometimeHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							// one round of sampling will be averaged here, will
							// be empty
							// when persisted
							// or abandoned.
							HashSet<ScannedBleDevice> averagedFingerprints = null;
							samplingProgressDialog.dismiss();
							MainActivity.this.toneG.stopTone();
							// BleFingerprintCollector.getDefault()
							// .RemoveOnBleSampleCollectedListener(
							// MainActivity.this);
							// MainActivity.this.collectedCycleCount.set(0);
							BleFingerprintCollector.getDefault().StopSampling();
							MainActivity.this.shouldIgnoreCurrentSample = false;
							MainActivity.this.syncLock.lock();
							try {
								if (MainActivity.this.collectedSample.size() == 0) {
									Toast.makeText(getBaseContext(),
											"Nothing was collected",
											android.widget.Toast.LENGTH_SHORT)
											.show();
									return;
								}

								// Log.e(LOG_TAG,
								// "Scanning stopped, raw content listed below: ");
								// for (ScannedBleDevice s : collected) {
								// Log.e(LOG_TAG, "	" + s.toSimpleString());
								// }
								Log.e(LOG_TAG,
										"DistinctAndAvgFingerprint, from: \r\n"
												+ Util.ToLogString(MainActivity.this.collectedSample));

								averagedFingerprints = Util
										.DistinctAndAvgFingerprint(MainActivity.this.collectedSample);
								Log.e(LOG_TAG,
										"DistinctAndAvgFingerprint, to: \r\n"
												+ Util.ToLogString(averagedFingerprints));
								// empty the list for next round of sampling.
								MainActivity.this.collectedSample = new ArrayList<ScannedBleDevice>();

								ArrayList<PositionDescriptor> loadedBuildInSampleData = Helper
										.LoadSamplingData(
												Helper.privateDataFileName,
												getBaseContext());
								if (loadedBuildInSampleData != null
										&& loadedBuildInSampleData.size() != 0) {
									final ArrayList<Tuple<Double, PositionDescriptor>> sortedSimilarityList = new ArrayList<Tuple<Double, PositionDescriptor>>();
									for (PositionDescriptor pd : loadedBuildInSampleData) {
										sortedSimilarityList
												.add(new Tuple<Double, PositionDescriptor>(
														Util.CaculateSimilarity(
																averagedFingerprints,
																pd.Fingerprints),
														pd));
									}

									Collections.sort(sortedSimilarityList,
											new SimilarityComparator());
									// hard code with 94, if higher or equal it,
									// show a prompt, all based on experience
									if (sortedSimilarityList
											.get(sortedSimilarityList.size() - 1).first >= 94) {
										new AlertDialog.Builder(
												MainActivity.this)
												.setIcon(
														android.R.drawable.ic_dialog_alert)
												.setTitle("sure?")
												.setMessage(
														"Poor selectivity, keep it anyway?")
												.setPositiveButton("Yes", null)
												.setNegativeButton(
														"No",
														new DialogInterface.OnClickListener() {
															@Override
															public void onClick(
																	DialogInterface dialog,
																	int which) {
																MainActivity.this.shouldIgnoreCurrentSample = true;
															}

														}).show();
									}
								}

							} finally {
								MainActivity.this.syncLock.unlock();
							}

							if (!MainActivity.this.shouldIgnoreCurrentSample) {
								String positionComments = "";
								// set a default value, they're p0,p1,p2...
								positionComments = "s"
										+ InMemPositionDescriptors.size();

								InMemPositionDescriptors
										.add(new PositionDescriptor(
												positionComments,
												MainActivity.this.currentAbsoluteXandY.first,
												MainActivity.this.currentAbsoluteXandY.second,
												averagedFingerprints, image
														));
								Log.e(LOG_TAG,
										"one sampling position was ready for persist");
								buttonPersist.setEnabled(true);

								List<ExpandableListViewItem> itemList = new ArrayList<ExpandableListViewItem>();
								ExpandableListViewItem parentNode = new ExpandableListViewItem(
										"["
												+ "0"
												+ "]"
												+ " X: "
												+ MainActivity.this.currentAbsoluteXandY.first
												+ ", Y: "
												+ MainActivity.this.currentAbsoluteXandY.second
												+ "->" + positionComments);
								for (ScannedBleDevice sDevice : averagedFingerprints) {
									parentNode
											.addChildItem(new ExpandableListViewItem(
													sDevice.DeviceName
															+ ", mac:"
															+ sDevice.MacAddress
															+ ", rssi:"
															+ sDevice.RSSI));
								}

								itemList.add(parentNode);
								ExpandableListViewAdapter adapter = new ExpandableListViewAdapter(
										getBaseContext(), itemList);
								pendingSampleExpandableListView
										.setAdapter(adapter);
							}
						}
					}, defaultSamplingTime);
					samplingProgressDialog = ProgressDialog.show(
							MainActivity.this, "On sampling...",
							"wait for sampling finished(" + defaultSamplingTime
									/ 1000 + "s)");
					break;
				}

				case MotionEvent.ACTION_UP:
					break;
				}

				return true;

			}
		});

		buttonPersist.setEnabled(false);
		buttonPersist.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Helper.WriteObjectToFile(InMemPositionDescriptors,
						Helper.privateDataFileName, getBaseContext());
				Toast.makeText(
						getBaseContext(),
						"Succeed to persist " + InMemPositionDescriptors.size()
								+ " pieces of samples",
						android.widget.Toast.LENGTH_SHORT).show();
				buttonPersist.setEnabled(false);
			}
		});

		buttonLoad.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Helper.DeleteFromFile(Helper.privateDataFileName, getBaseContext());
				Object data = Helper.ReadObjectFromFile(
						Helper.privateDataFileName, getBaseContext());
				final List<DrawCircle> allPersistedCirclesCoordinates = new ArrayList<DrawCircle>();
				if (data != null
						&& ((ArrayList<PositionDescriptor>) data).size() > 0) {
					ArrayList<PositionDescriptor> loadData = (ArrayList<PositionDescriptor>) data;
					for (PositionDescriptor desc : loadData) {
						allPersistedCirclesCoordinates.add(new DrawCircle(
								desc.getTransferredXByImageView(image), desc.getTransferredYByImageView(image), desc.Description, Color.RED));
					}
					// return simpleClass;
					Toast.makeText(
							getBaseContext(),
							"Succeed to load " + loadData.size()
									+ " pieces of samples",
							android.widget.Toast.LENGTH_SHORT).show();
					image.drawMultipleCircles(allPersistedCirclesCoordinates);
				} else {
					image.cleanAllCircles();
				}
			}
		});

	}

	// 9intervals per each group, repeat 30 times.
	int[] differentSleepTimes = new int[] { 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600,
			1800, 2000, 2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000,
			2200, 500, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800,
			1000, 1200, 1400, 1600, 1800, 2000, 2200, 500, 800, 1000, 1200,
			1400, 1600, 1800, 2000, 2200,

	};
	private int testRunTimes = 0;
	// private int testingLoopTimes = 2;
	private Hashtable<Integer, Integer> testingResult = new Hashtable<Integer, Integer>();

	private void testBle() {
		if (testRunTimes == differentSleepTimes.length) {
			// samplingProgressDialog.dismiss();
			// MainActivity.this.toneG.stopTone();
			testRunTimes = 0;
			Iterator<Hashtable.Entry<Integer, Integer>> it = testingResult
					.entrySet().iterator();
			while (it.hasNext()) {
				Hashtable.Entry<Integer, Integer> entry = it.next();
				Log.e(LOG_TAG,
						"FInal testing result, Interval: " + entry.getKey()
								+ ", total: " + entry.getValue() + ", avg: "
								+ (float) (entry.getValue())
								/ differentSleepTimes.length / 9);
			}
			return;
		}
		// BleFingerprintCollector.getDefault().setLeScanStartAndStopInterval(
		// differentSleepTimes[testRunTimes]);
		// empty the list for next round of sampling.
		MainActivity.this.collectedSample = new ArrayList<ScannedBleDevice>();

		BleFingerprintCollector.getDefault().StartSampling();
		waitSometimeHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// one round of sampling will be averaged here,
				// will
				// be empty
				// when persisted
				// or abandoned.
				// HashSet<ScannedBleDevice>
				// averagedFingerprints =
				// null;
				samplingProgressDialog.dismiss();
				MainActivity.this.toneG.stopTone();
				// BleFingerprintCollector.getDefault()
				// .RemoveOnBleSampleCollectedListener(
				// MainActivity.this);
				// MainActivity.this.collectedCycleCount.set(0);
				BleFingerprintCollector.getDefault().StopSampling();
				// MainActivity.this.shouldIgnoreCurrentSample =
				// false;

				try {
					if (testingResult.containsKey(BleFingerprintCollector
							.getDefault().getLeScanStartAndStopInterval())) {
						int ex = testingResult.get(BleFingerprintCollector
								.getDefault().getLeScanStartAndStopInterval());
						testingResult.remove(BleFingerprintCollector
								.getDefault().getLeScanStartAndStopInterval());
						testingResult.put(BleFingerprintCollector.getDefault()
								.getLeScanStartAndStopInterval(), ex
								+ MainActivity.this.collectedSample.size());
					} else {
						testingResult.put(BleFingerprintCollector.getDefault()
								.getLeScanStartAndStopInterval(),
								MainActivity.this.collectedSample.size());
					}

					Log.e(LOG_TAG,
							"For LeScanStartAndStopInterval: "
									+ BleFingerprintCollector.getDefault()
											.getLeScanStartAndStopInterval()
									+ ", total collected: "
									+ Integer
											.toString(MainActivity.this.collectedSample
													.size())
									+ ", collected per cycle: "
									+ (float) ((float) MainActivity.this.collectedSample
											.size()
											/ defaultSamplingTime
											/ 1000
											/ BleFingerprintCollector
													.getDefault()
													.getLeScanStartAndStopInterval() / 1000));
					testRunTimes++;
					testBle();

				} finally {
					// MainActivity.this.syncLock.unlock();
				}
			}
		}, defaultSamplingTime);
		samplingProgressDialog = ProgressDialog.show(MainActivity.this,
				"On sampling test: " + differentSleepTimes[testRunTimes],
				"wait for sampling finished(" + defaultSamplingTime / 1000
						+ "s)");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK
				&& null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			cursor.getString(columnIndex);
			cursor.close();

			// ImageView imageView = (ImageView) findViewById(R.id.imgView);
			// MainActivityBitmapSource = BitmapFactory.decodeFile(picturePath);
			// this.image.setImageBitmap(MainActivityBitmapSource);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// private AtomicInteger collectedCycleCount = new AtomicInteger();

	protected void onDestroy() {
		super.onDestroy();// Always call the superclass method first
		BleFingerprintCollector.getDefault().TurnOff();
	}

	private ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM,
			10);

	@Override
	public void onSampleCollected(List<ScannedBleDevice> samples) {
		this.toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
		this.syncLock.lock();
		try {
			// Log.e(LOG_TAG, "onSampleCollected, total count for this time: "
			// + samples.size() + " ble signals");
			this.collectedSample.addAll(samples);
		} finally {
			this.syncLock.unlock();
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				// MainActivity.this.promptActionTextView
				// .setText("-> "
				// + MainActivity.this.collectedCycleCount
				// .incrementAndGet() + " <- "
				// + " cycles elapsed");
			}
		});
	}

}
