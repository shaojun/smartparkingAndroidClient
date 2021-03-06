package com.SmartParking.Demo.Sampling;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Lookup.LocalPositionDescriptor;
import com.SmartParking.Sampling.BleFingerprintCollector;
import com.SmartParking.Sampling.OnBleSampleCollectedListener;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.Task.Action;
import com.SmartParking.Task.OnActionFinishedListener;
import com.SmartParking.Task.Task;
import com.SmartParking.UI.DrawCircle;
import com.SmartParking.UI.DrawImage;
import com.SmartParking.UI.ExpandableListViewAdapter;
import com.SmartParking.UI.ExpandableListViewItem;
import com.SmartParking.UI.MarkableTouchImageView;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Util;
import com.SmartParking.WebService.BulkRestClient;
import com.SmartParking.Task.RestAction;
import com.SmartParking.WebService.RestEntityResultDumper;
import com.SmartParking.WebServiceEntity.Board;
import com.SmartParking.WebServiceEntity.Building;
import com.SmartParking.WebServiceEntity.Order;
import com.SmartParking.WebServiceEntity.Sample;
import com.SmartParking.WebServiceEntity.SampleDescriptor;
import com.ortiz.touch.TouchImageView.OnTouchImageViewListener;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
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
import android.graphics.Color;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements
        OnBleSampleCollectedListener {
    private static final String LOG_TAG = "SmarkParking.Demo.Main";
    private String userName = "";
    private String password = "";
    private BluetoothAdapter mBluetoothAdapter = null;
    //private PowerManager.WakeLock screenOnLock = null;
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
    ProgressDialog progress;
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
    public static ArrayList<LocalPositionDescriptor> InMemPositionDescriptors = new ArrayList<>();
    private Lock syncLock = new ReentrantLock();
    private static int RESULT_LOAD_IMAGE = 1;
    private Handler waitSometimeHandler = new Handler();
    private Building currentBuilding;
    // the low selectivity detected, user might want to re-do sampling and
    // ignore this one.
    private boolean shouldIgnoreCurrentSample = false;
    private boolean keepPollingAllParkingPositionsFromWeb = true;
    // 100 px equal how many real life meters, like say the value is 5, means
    // 100px in bitmap equal 5meters. default set to 2.
    private float mapScale = 2;


    @Override
    protected void onStop() {
        super.onStop(); // Always call the superclass method first
        this.keepPollingAllParkingPositionsFromWeb = false;
        //this.screenOnLock.release();
        // Log.e(LOG_TAG, "MainActivity stopping the BleFingerprintCollector");
        // BleFingerprintCollector.getDefault().Stop();
        // BleFingerprintCollector.getDefault().RemoveOnBleSampleCollectedListener(this);

    }

    protected void onRestart() {
        super.onRestart(); // Always call the superclass method first
        //this.screenOnLock.acquire();
        if (!BleFingerprintCollector.getDefault().IsStarted.get()
                && false == BleFingerprintCollector.getDefault().TurnOn(
                mBluetoothAdapter)) {
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
        this.keepPollingAllParkingPositionsFromWeb = true;
        //loadAllWebBoardAndShowOnUI();
    }

    @Override
    protected void onStart() {
        super.onStart(); // Always call the superclass method first

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
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
                mBluetoothAdapter)) {
            Toast.makeText(getBaseContext(),
                    "Failed to start FingerprintCollector, quit",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }

		/* does this device support BLE advertisement */
//        if (!this.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
//            Toast.makeText(getBaseContext(), "Unlikely support for advertisement",
//                    android.widget.Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(getBaseContext(),
//                    "Congratulation you support ble advertisement~~~~",
//                    android.widget.Toast.LENGTH_LONG).show();
//        }

        /******************************
         * make sure the screen always on.
         */
        PowerManager pm = (PowerManager) this
                .getSystemService(Context.POWER_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        this.screenOnLock = pm.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,//PowerManager.SCREEN_DIM_WAKE_LOCK,
//                LOG_TAG);
//        this.screenOnLock.acquire();

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
        // like "http://rest.shaojun.xyz:8090/buildings/1/"
        currentBuilding = (Building) (intent.getSerializableExtra("Building"));
        SharedPreferences logOnSharedPreferences = this.getSharedPreferences("LogOn", 0);
        userName = logOnSharedPreferences.getString("UserName", null);
        password = logOnSharedPreferences.getString("Password", null);

        if (mapScaleValue != null && mapScaleValue != "") {
            this.mapScale = Float.parseFloat(mapScaleValue);
        }

//        image = (MarkableTouchImageView) findViewById(R.id.imgControl);
//        image.setScaleType(ScaleType.CENTER_CROP);
        Log.e(LOG_TAG, "Loading the indoor map from url: " + currentBuilding.MapUrl);
        progress = ProgressDialog.show(this, "获取中...",
                "获取室内地图信息", true);
        Task.Create(new Action<Bitmap>("getImageBitmapFromUrlAction") {
            @Override
            public Bitmap execute(Task ownerTask) throws Exception {
                return Helper.GetImageBitmapFromUrl(currentBuilding.MapUrl);
            }
        }).Start(new OnActionFinishedListener<String>() {
            @Override
            public void Finished(Task task, Action<String> finishedAction) {
                progress.dismiss();
                if (task.isFaulted()) {
                    new AlertDialog.Builder(
                            MainActivity.this)
                            .setIcon(
                                    android.R.drawable.ic_dialog_alert)
                            .setTitle("失败")
                            .setMessage(
                                    "加载当前停车场地图失败")
                            .setPositiveButton("Ok", null).show();
                } else {
                    final Drawable mapDrawable = new BitmapDrawable(getResources(),
                            (Bitmap) task.getSingleResult());
                    image = new MarkableTouchImageView(MainActivity.this);//(MarkableTouchImageView) findViewById(R.id.imgControl);
                    image.setScaleType(ScaleType.CENTER_CROP);
                    image.setImageDrawable(mapDrawable);
                    image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    ((LinearLayout) findViewById(R.id.imgControlHost)).addView(image);
                    setupImageControl();
                    progress.dismiss();
                    loadAllWebBoardAndShowOnUI();
                    loadSamplesDetailFromWeb();
                }
            }
        });

        //loadingIndoorMapThread.start();
        currentCoorTextView = (TextView) findViewById(R.id.current_coordinate);
        logTextView = (TextView) findViewById(R.id.logTextView);
        // commentsEditText = (EditText) findViewById(R.id.commentsEditText);
        // buttonSave = (Button) findViewById(R.id.buttonSave);
        buttonLoad = (Button) findViewById(R.id.buttonLoad);
        buttonPersist = (Button) findViewById(R.id.buttonPersist);
        buttonSampling = (Button) findViewById(R.id.buttonSampling);
        pendingSampleExpandableListView = (ExpandableListView) findViewById(R.id.pendingSampleExpandableListView);
        Button buttonViewSampleData = (Button) findViewById(R.id.buttonViewSampleData);
        buttonViewSampleData.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(MainActivity.this, ViewSamplingDataActivity.class);
                        i.putExtra("Building", currentBuilding);
                        startActivity(i);
                    }
                }
        );

        Button buttonStartNavi = (Button) findViewById(R.id.buttonStartNavi);
        buttonStartNavi.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(MainActivity.this, NaviActivity.class);
                        i.putExtra("Building", currentBuilding);
                        startActivity(i);
                    }
                }
        );
        Button buttonSelectPic = (Button) findViewById(R.id.buttonSelectPic);
        buttonSelectPic.setOnClickListener(
                new OnClickListener() {
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
                }
        );

        // ============================
        BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(MainActivity.this);
        buttonSampling.setOnTouchListener(
                new OnTouchListener() {
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
                                            MainActivity.this.collectedSample = new ArrayList<>();

//                                            ArrayList<PositionDescriptor> loadedBuildInSampleData = Helper
//                                                    .LoadSamplingData(
//                                                            Helper.privateDataFileName,
//                                                            getBaseContext());
                                            if (InMemPositionDescriptors != null
                                                    && InMemPositionDescriptors.size() != 0) {
                                                final ArrayList<Tuple<Double, LocalPositionDescriptor>> sortedSimilarityList = new ArrayList<>();
                                                for (LocalPositionDescriptor pd : InMemPositionDescriptors) {
                                                    sortedSimilarityList
                                                            .add(new Tuple<>(
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
                                            // set a default value, they're p0,p1,p2...
                                            String positionComments = "s" + InMemPositionDescriptors.size();
                                            InMemPositionDescriptors
                                                    .add(new LocalPositionDescriptor(
                                                            MainActivity.this.currentAbsoluteXandY.first,
                                                            MainActivity.this.currentAbsoluteXandY.second,
                                                            positionComments,
                                                            averagedFingerprints, image
                                                    ));
                                            Log.e(LOG_TAG,
                                                    "one sampling position was ready for persist");
                                            buttonPersist.setEnabled(true);

                                            List<ExpandableListViewItem> itemList = new ArrayList<ExpandableListViewItem>();
                                            ExpandableListViewItem parentNode = new ExpandableListViewItem("[0] X: "
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
                }
        );

        //buttonPersist.setEnabled(false);
        buttonPersist.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // have to use a reference type.
                        Boolean haveSthToSave = false;
                        for (LocalPositionDescriptor _ : InMemPositionDescriptors) {
                            if (!_.FlushedToWeb) {
                                haveSthToSave = true;
                                break;
                            }
                        }

                        if (!haveSthToSave) {
                            new AlertDialog.Builder(
                                    MainActivity.this)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle("无效操作")
                                    .setMessage(
                                            "所有本地采样数据已经保存到服务端，无需再次保存！")
                                    .setPositiveButton("Ok", null).show();
                            return;
                        }

                        persistLocalPositionsToWebWithProgressShown(InMemPositionDescriptors);
                        //buttonPersist.setEnabled(false);
                    }
                }
        );
        buttonLoad.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<DrawCircle> positions = new ArrayList<DrawCircle>();
                        for (LocalPositionDescriptor pd : InMemPositionDescriptors) {
                            positions.add(new DrawCircle(
                                    pd.getLocalX(),
                                    pd.getLocalY(),
                                    Helper.GetCircleRadiusByMapScale(MainActivity.this.mapScale),
                                    pd.Description, Color.BLACK));
                        }

                        image.drawMultipleCircles(positions);
                    }
                }
        );
    }

    ProgressDialog persistLocalPositionsToWebProgress;

    private void persistLocalPositionsToWebWithProgressShown(ArrayList<LocalPositionDescriptor> target) {
        for (LocalPositionDescriptor pd : target
                ) {
            if (pd.FlushedToWeb) continue;
            if (pd.Fingerprints.size() == 0) {
                new AlertDialog.Builder(
                        MainActivity.this)
                        .setIcon(
                                android.R.drawable.ic_dialog_alert)
                        .setTitle("无效的采样点")
                        .setMessage(
                                "采样点:" + pd.Description + " 没有包含采样点描述信息，请重新采样此点再试！")
                        .setPositiveButton("Ok", null).show();
                return;
            }

            persistLocalPositionsToWebProgress = ProgressDialog.show(MainActivity.this, "保存中...",
                    "保存采样点:" + pd.Description + " 到服务器", true);
            //http://rest.shaojun.xyz:8090/samples/?ownerBuildingId=1&coordinateX=66&coordinateY=78
            Task.Create(new RestAction("samples/?ownerBuildingId=" + currentBuilding.Id
                    + "&coordinateX=" + (int) (pd.getRemoteX()) + "&coordinateY=" + (int) (pd.getRemoteY()),
                    userName, password, "GET", pd)).Start(
                    new OnActionFinishedListener<String>() {
                        @Override
                        public void Finished(Task task, Action<String> finishedAction) {
                            if (task.isFaulted()) {
                                persistLocalPositionsToWebProgress.dismiss();
                                new AlertDialog.Builder(
                                        MainActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("失败")
                                        .setMessage(
                                                "上传采样点: " + ((LocalPositionDescriptor) (finishedAction.getStateObject())).Description + " 数据失败(stage 0)")
                                        .setPositiveButton("Ok", null).show();
                                return;
                            } else {
                                final List<Sample> existedSamplesOnWeb = new ArrayList<>();
                                try {
                                    existedSamplesOnWeb.addAll(RestEntityResultDumper.dump(task.getSingleResult().toString(), Sample.class));
                                } catch (Exception e) {
                                    persistLocalPositionsToWebProgress.dismiss();
                                    e.printStackTrace();
                                    new AlertDialog.Builder(
                                            MainActivity.this)
                                            .setIcon(
                                                    android.R.drawable.ic_dialog_alert)
                                            .setTitle("Resolve underlying Sample failed")
                                            .setMessage("!!!")
                                            .setPositiveButton("Failed", null)
                                            .show();
                                    return;
                                }

                                // this sample didn't exist in server side, then need create it.
                                if (existedSamplesOnWeb.size() == 0) {
                                    final RestAction addingSampleAction = new RestAction("samples/",
                                            userName, password, "POST", finishedAction.getStateObject());
                                    Sample postSample = new Sample();
                                    postSample.OwnedByBuildingUrl = currentBuilding.DetailUrl;
                                    postSample.CoordinateX = (int) (
                                            ((LocalPositionDescriptor) (finishedAction.getStateObject())).getRemoteX());
                                    postSample.CoordinateY = (int) (
                                            ((LocalPositionDescriptor) (finishedAction.getStateObject())).getRemoteY());
                                    postSample.Description = ((LocalPositionDescriptor) (finishedAction.getStateObject())).Description;
                                    addingSampleAction.AddPostJsonObject(postSample.toJsonObject());
                                    Task.Create(addingSampleAction).Start(new OnActionFinishedListener<String>() {
                                        @Override
                                        public void Finished(Task task, Action<String> finishedAction) {
                                            if (task.isFaulted()) {
                                                persistLocalPositionsToWebProgress.dismiss();
                                                new AlertDialog.Builder(
                                                        MainActivity.this)
                                                        .setIcon(
                                                                android.R.drawable.ic_dialog_alert)
                                                        .setTitle("失败")
                                                        .setMessage(
                                                                "上传(create)新单个采样点数据失败(stage 1)")
                                                        .setPositiveButton("Ok", null).show();
                                                return;
                                            } else {
                                                persistLocalPositionsToWebProgress.dismiss();
                                                Sample postedSample;
                                                try {
                                                    postedSample = RestEntityResultDumper.dump(task.getSingleResult().toString(), Sample.class).get(0);
                                                } catch (Exception e) {
                                                    persistLocalPositionsToWebProgress.dismiss();
                                                    e.printStackTrace();
                                                    new AlertDialog.Builder(
                                                            MainActivity.this)
                                                            .setIcon(
                                                                    android.R.drawable.ic_dialog_alert)
                                                            .setTitle("Resolve underlying new added Sample failed")
                                                            .setMessage("!!!")
                                                            .setPositiveButton("Failed", null)
                                                            .show();
                                                    return;
                                                }

                                                persistLocalPositionsToWebProgress = ProgressDialog.show(MainActivity.this, "保存中...",
                                                        "保存新采样点描述信息到服务器", true);
                                                Task<String> addingSampleDescTask = null;
                                                // uploading all correlated SampleDescriptors.
                                                LocalPositionDescriptor localPD = ((LocalPositionDescriptor) (finishedAction.getStateObject()));
                                                for (ScannedBleDevice fp : localPD.Fingerprints
                                                        ) {
                                                    RestAction addingSampleDescAction = new RestAction("sampleDescriptors/",
                                                            userName, password, "POST", localPD);
                                                    SampleDescriptor postSampleDescriptor = new SampleDescriptor();
                                                    postSampleDescriptor.OwnedSampleUrl = postedSample.DetailUrl;
                                                    postSampleDescriptor.UUID = Util.BytesToHexString(fp.IbeaconProximityUUID);
                                                    postSampleDescriptor.MajorId = Util.BytesToHexString(fp.Major);
                                                    postSampleDescriptor.MinorId = Util.BytesToHexString(fp.Minor);
                                                    postSampleDescriptor.MacAddress = fp.MacAddress;
                                                    postSampleDescriptor.Rssi = (int) (fp.RSSI);
                                                    addingSampleDescAction.AddPostJsonObject(postSampleDescriptor.toJsonObject());
                                                    if (addingSampleDescTask == null)
                                                        addingSampleDescTask = Task.Create(addingSampleDescAction);
                                                    else
                                                        addingSampleDescTask.continueWith(addingSampleDescAction);
                                                }

                                                assert addingSampleDescTask != null;
                                                addingSampleDescTask.Start(new OnActionFinishedListener<String>() {
                                                    @Override
                                                    public void Finished(Task task, Action<String> finishedAction) {
                                                        if (task.isFaulted()) {
                                                            persistLocalPositionsToWebProgress.dismiss();
                                                            new AlertDialog.Builder(
                                                                    MainActivity.this)
                                                                    .setIcon(
                                                                            android.R.drawable.ic_dialog_alert)
                                                                    .setTitle("失败")
                                                                    .setMessage(
                                                                            "上传新创建的采样点描述数据失败(stage 2),但采样点已经上传成功")
                                                                    .setPositiveButton("Ok", null).show();
                                                            return;
                                                        } else if (task.isCompleted()) {
                                                            persistLocalPositionsToWebProgress.dismiss();
                                                            // here indicate the sample and its SampleDescriptor had been all uploaded to Web.
                                                            ((LocalPositionDescriptor) (finishedAction.getStateObject())).FlushedToWeb = true;
                                                            // move to next sample
                                                            persistLocalPositionsToWebWithProgressShown(InMemPositionDescriptors);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                } else if (existedSamplesOnWeb.size() == 1) {
                                    /* here indicate the uploading Sample already existed in Web, so just delete all its SampleDescriptions, and re-add*/
                                    List<String> deletingExistedSampleDescUrls = new ArrayList<>();
                                    for (SampleDescriptor sd : existedSamplesOnWeb.get(0).SampleDescriptors) {
                                        deletingExistedSampleDescUrls.add(sd.DetailUrl);
                                    }

                                    Task.Create(new BulkRestClient(deletingExistedSampleDescUrls,
                                            userName, password, "DELETE", finishedAction.getStateObject()))
                                            .Start(new OnActionFinishedListener<String>() {
                                                       @Override
                                                       public void Finished(Task task, Action<String> finishedAction) {
                                                           if (task.isFaulted()) {
                                                               persistLocalPositionsToWebProgress.dismiss();
                                                               new AlertDialog.Builder(
                                                                       MainActivity.this)
                                                                       .setIcon(
                                                                               android.R.drawable.ic_dialog_alert)
                                                                       .setTitle("失败")
                                                                       .setMessage(
                                                                               "删除已有采样点描述数据失败(stage 4)")
                                                                       .setPositiveButton("Ok", null).show();
                                                               return;
                                                           }

                                                           if (task.isCompleted()) {
                                                               LocalPositionDescriptor ppd = (LocalPositionDescriptor) (finishedAction.getStateObject());
                                                               String targetSampleUrl = existedSamplesOnWeb.get(0).DetailUrl;

                                                               Task<String> addingSampleDescTask = null;
                                                               // uploading all correlated SampleDescriptors.
                                                               for (ScannedBleDevice fp : ppd.Fingerprints
                                                                       ) {
                                                                   RestAction addingSampleDescAction = new RestAction("sampleDescriptors/",
                                                                           userName, password, "POST", finishedAction.getStateObject());
                                                                   SampleDescriptor postSampleDescriptor = new SampleDescriptor();
                                                                   postSampleDescriptor.OwnedSampleUrl = targetSampleUrl;
                                                                   postSampleDescriptor.UUID = Util.BytesToHexString(fp.IbeaconProximityUUID);
                                                                   postSampleDescriptor.MajorId = Util.BytesToHexString(fp.Major);
                                                                   postSampleDescriptor.MinorId = Util.BytesToHexString(fp.Minor);
                                                                   postSampleDescriptor.MacAddress = fp.MacAddress;
                                                                   postSampleDescriptor.Rssi = (int) (fp.RSSI);
                                                                   addingSampleDescAction.AddPostJsonObject(postSampleDescriptor.toJsonObject());
                                                                   if (addingSampleDescTask == null)
                                                                       addingSampleDescTask = Task.Create(addingSampleDescAction);
                                                                   else
                                                                       addingSampleDescTask.continueWith(addingSampleDescAction);
                                                               }

                                                               assert addingSampleDescTask != null;
                                                               addingSampleDescTask.Start(new OnActionFinishedListener<String>() {
                                                                   @Override
                                                                   public void Finished(Task task, Action<String> finishedAction) {
                                                                       persistLocalPositionsToWebProgress.dismiss();
                                                                       if (task.isFaulted()) {

                                                                           new AlertDialog.Builder(
                                                                                   MainActivity.this)
                                                                                   .setIcon(
                                                                                           android.R.drawable.ic_dialog_alert)
                                                                                   .setTitle("失败")
                                                                                   .setMessage(
                                                                                           "上传新创建的采样点描述数据失败(stage 5)")
                                                                                   .setPositiveButton("Ok", null).show();
                                                                       } else if (task.isCompleted()) {
                                                                           // here indicate the sample and its SampleDescriptor had been all uploaded to Web.
                                                                           ((LocalPositionDescriptor) (finishedAction.getStateObject())).FlushedToWeb = true;
                                                                           // move to next sample
                                                                           persistLocalPositionsToWebWithProgressShown(InMemPositionDescriptors);
                                                                       }
                                                                   }
                                                               });
                                                           }
                                                       }
                                                   }
                                            );
                                }
                            }
                        }
                    }
            );
            // always break after send one sample, will recursive call once previous done successfully.
            break;
        }
    }

    private void loadAllWebBoardAndShowOnUI() {
        //http://rest.shaojun.xyz:8090/boards/
        RestAction scanBoardsStatusAction = new RestAction("boards/?ownerBuildingId=" + currentBuilding.Id, userName, password, "GET", "scanBoardsStatus");
        Task.Create(scanBoardsStatusAction).Start(
                new OnActionFinishedListener<String>() {
                    @Override
                    public void Finished(Task task, Action<String> lastFinishedAction) {
                        if (task.isFaulted()) {
                            findViewById(R.id.editTextPwd).setEnabled(true);
                            new AlertDialog.Builder(
                                    MainActivity.this)
                                    .setIcon(
                                            android.R.drawable.ic_dialog_alert)
                                    .setTitle("scanBoardsStatus failed")
                                    .setMessage(task.getSingleException().toString())
                                    .setPositiveButton("Failed", null)
                                    .show();
                        } else {
                            List<Board> boards = null;
                            try {
                                boards = RestEntityResultDumper.dump(task.getSingleResult().toString(), Board.class);
                            } catch (Exception e) {
                                new AlertDialog.Builder(
                                        MainActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("Resolve boards failed")
                                        .setMessage("!!!")
                                        .setPositiveButton("Failed", null)
                                        .show();
                                return;
                            }

                            List<DrawImage> drawImages = Helper
                                    .ConvertRestBoardsToDrawImages(boards,
                                            ((BitmapDrawable) image.getDrawable()).getBitmap(),
                                            image,
                                            getResources(), currentBuilding.MapScale, 3);
                            image.drawMultipleCirclesAndImages(null, drawImages);
                            waitSometimeHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (keepPollingAllParkingPositionsFromWeb)
                                        loadAllWebBoardAndShowOnUI();
                                }
                            }, 8000);
                        }
                    }
                });
    }

    private void setupImageControl() {
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

                    if (zoomedRect != null) {
                        // we want the circle has 1.5m radius.
                        Bitmap shoePrintIcon = Helper.getScaledBitmapByMapScale(BitmapFactory.decodeResource(
                                getResources(), R.drawable.shoeprints), currentBuilding.MapScale, 2, false);

                        image.drawImageNewOrUpdate(new DrawImage(
                                        MainActivity.this.currentAbsoluteXandY.first - shoePrintIcon.getWidth() / 2,
                                        MainActivity.this.currentAbsoluteXandY.second - shoePrintIcon.getHeight() / 2,
                                        shoePrintIcon, "", "sampleSelectedSinglePoint")
                        );
                    }
                }
                return true;
            }
        });
        // =================================


        /******************************
         * init the zoom and pan variables.
         */
        zoomedRect = image.getZoomedRect();
        currentZoom = image.getCurrentZoom();
        //image.isZoomed();
    }

    // 9intervals per each group, repeat 30 times.
    int[] differentSleepTimes = new int[]{500, 800, 1000, 1200, 1400, 1600,
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

    /**
     * will clear InMemPositionDescriptors, so make sure persisted local un-flushed data to web first.
     */
    private void loadSamplesDetailFromWeb() {
        final ProgressDialog progressSample = ProgressDialog.show(MainActivity.this, "获取中...",
                "获取采样点整体信息", true);
        Action<String> getRestSamplesForABuildingAction = new RestAction("samples/?ownerBuildingId=" + currentBuilding.Id, userName, password, "GET", "getRestSamplesForABuilding");
        Task.Create(getRestSamplesForABuildingAction).Start(
                new OnActionFinishedListener<String>() {
                    @Override
                    public void Finished(Task task, Action<String> finishedAction) {
                        progressSample.dismiss();
                        if (task.isFaulted()) {
                            new AlertDialog.Builder(
                                    MainActivity.this)
                                    .setIcon(
                                            android.R.drawable.ic_dialog_alert)
                                    .setTitle("Get samples failed")
                                    .setMessage(task.getSingleException().toString())
                                    .setPositiveButton("Failed", null)
                                    .show();
                        } else {
                            List<String> furtherActionUrls = new ArrayList<>();
                            final List<Sample> samplesForBuilding;
                            try {
                                samplesForBuilding = RestEntityResultDumper.dump(task.getSingleResult().toString(), Sample.class);
                                // add all sampleDescriptor's url to a list to further 'GET'
                                for (Sample __sample : samplesForBuilding) {
                                    for (SampleDescriptor sd : __sample.SampleDescriptors) {
                                        String sampleDescriptionUrl = sd.DetailUrl;
                                        furtherActionUrls.add(sampleDescriptionUrl);
                                    }
                                }

                                Log.e(LOG_TAG, "Samples total " + samplesForBuilding.size()
                                        + ", and SampleDescriptor total " + furtherActionUrls.size() + " loaded from Web");

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                new AlertDialog.Builder(
                                        MainActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("Resolve underlying sample failed")
                                        .setMessage("!!!")
                                        .setPositiveButton("Failed", null)
                                        .show();
                                return;
                            }


                            final ProgressDialog progressSampleDesc = ProgressDialog.show(MainActivity.this, "获取中...",
                                    "获取所有具体采样点信息", true);
                            Task.Create(new BulkRestClient(furtherActionUrls, userName, password, "GET", "furtherTask")).Start(
                                    new OnActionFinishedListener<String>() {
                                        @Override
                                        public void Finished(Task task, Action<String> finishedAction) {
                                            progressSampleDesc.dismiss();
                                            if (!task.isCompleted()) return;
                                            if (task.isFaulted()) {
                                                new AlertDialog.Builder(
                                                        MainActivity.this)
                                                        .setIcon(
                                                                android.R.drawable.ic_dialog_alert)
                                                        .setTitle("Get sample descriptor faulted")
                                                        .setMessage("get sample descriptor faulted")
                                                        .setPositiveButton("Failed", null)
                                                        .show();
                                                return;
                                            }

                                            List<SampleDescriptor> allSampleDescs = new ArrayList<>();
                                            try {
                                                List<String> rawWebResults = (List<String>) (task.getAggreatedResult("furtherTask").get(0));
                                                for (String oneSampleDescRawWebResult : rawWebResults)
                                                    allSampleDescs.add(RestEntityResultDumper.dump(oneSampleDescRawWebResult, SampleDescriptor.class).get(0));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                new AlertDialog.Builder(
                                                        MainActivity.this)
                                                        .setIcon(
                                                                android.R.drawable.ic_dialog_alert)
                                                        .setTitle("Parsing sampleDescs failed")
                                                        .setMessage("get sample descriptor faulted")
                                                        .setPositiveButton("Failed", null)
                                                        .show();
                                                return;
                                            }

                                            for (Sample sa : samplesForBuilding) {
                                                // clear the uncompleted SampleDescriptors which created at parsing Sample from raw rest web result.
                                                sa.SampleDescriptors.clear();
                                                for (SampleDescriptor s : allSampleDescs) {
                                                    if (s.OwnedSampleUrl.equals(sa.DetailUrl)) {
                                                        sa.SampleDescriptors.add(s);
                                                    }
                                                }
                                            }

                                            InMemPositionDescriptors.clear();
                                            for (Sample sp : samplesForBuilding) {
                                                HashSet<ScannedBleDevice> fingerprintsLoadedFromWeb = new HashSet<>();
                                                for (SampleDescriptor sd
                                                        : sp.SampleDescriptors) {
                                                    ScannedBleDevice _ = new ScannedBleDevice(sd.UUID, sd.MajorId, sd.MinorId, sd.MacAddress, sd.Tx, sd.Rssi, sd.Distance);
                                                    fingerprintsLoadedFromWeb.add(_);
                                                }
                                                LocalPositionDescriptor __ = new
                                                        LocalPositionDescriptor(sp.Description, sp.CoordinateX, sp.CoordinateY, fingerprintsLoadedFromWeb, image);
                                                // since it from web, mark it as true;
                                                __.FlushedToWeb = true;
                                                InMemPositionDescriptors.add(__);
                                            }

                                            Log.e(LOG_TAG, "Samples total " + MainActivity.this.InMemPositionDescriptors.size() + " loaded from Web");

                                            Toast.makeText(
                                                    getBaseContext(),
                                                    "existed data loaded from web(total " + MainActivity.this.InMemPositionDescriptors.size() + ")",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        }
                    }
                }

        );
    }

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
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

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
