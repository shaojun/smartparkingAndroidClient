package com.SmartParking.Demo.Sampling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Lookup.LocalPositionDescriptor;
import com.SmartParking.Sampling.BleFingerprintCollector;
//import com.SmartParking.Sampling.MovementDetector;
import com.SmartParking.Sampling.OnBleSampleCollectedListener;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.Task.Action;
import com.SmartParking.Task.OnActionFinishedListener;
import com.SmartParking.Task.Task;
import com.SmartParking.UI.DrawCircle;
import com.SmartParking.UI.DrawImage;
import com.SmartParking.UI.MarkableTouchImageView;
import com.SmartParking.UI.OnBitmapInTouchImageClickedListener;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.SmartParking.WebService.BulkRestClient;
import com.SmartParking.WebService.OnAsyncRestTaskFinishedListener;
import com.SmartParking.Task.RestAction;
import com.SmartParking.WebService.RestEntityResultDumper;
import com.SmartParking.WebService.Service;
import com.SmartParking.WebServiceEntity.Board;
import com.SmartParking.WebServiceEntity.Building;
import com.SmartParking.WebServiceEntity.Order;
import com.SmartParking.WebServiceEntity.Sample;
import com.SmartParking.WebServiceEntity.SampleDescriptor;
import com.SmartParking.WebServiceEntity.UserInfo;

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
    private static final String LOG_TAG_LOCATION = "Navi.Location";
    private MarkableTouchImageView image = null;
    //private TextView logTextView = null;
    private CheckBox exposeMeCheckBox = null;
    // every 4 scan cycle will trigger a position draw.
    private Integer bufferTimes = 4;
    private String userName = "";
    private String password = "";
    private BluetoothAdapter mBluetoothAdapter = null;
    private AdvertiseCallback mAdvertiseCallback = null;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;

    private EditText uuidEditText = null;
    private EditText uuid2EditText = null;
    private EditText uuid3EditText = null;

    private ArrayList<LocalPositionDescriptor> loadedBuildInSampleData = new ArrayList<>();
    // only similarity >= this value get UI refresh
    private int similarityThreashold = 75;
    private PowerManager.WakeLock screenOnLock = null;
    // X,Y and its comments text.
    private List<Tuple<Tuple<Float, Float>, String>> allCandidatesWithText = new ArrayList<Tuple<Tuple<Float, Float>, String>>();

    private boolean keepPollingAllParkingPositionsFromWeb = true;
    // 100 px equal how many real life meters, like say the value is 5, means
    // 100px in bitmap equal 5meters, default set to 2.
    private float mapScale = 2;
    ProgressDialog progress;
    private Building currentBuilding;
    // all boards in current building, will be updated periodically to reflect the
    // potential status change(ordered by someone and etc.)
    private HashSet<Board> boardsInBuilding = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
        Intent intent = getIntent();
        currentBuilding = (Building) (intent.getSerializableExtra("Building"));
        SharedPreferences logOnSharedPreferences = this.getSharedPreferences("LogOn", 0);
        userName = logOnSharedPreferences.getString("UserName", null);
        password = logOnSharedPreferences.getString("Password", null);
//        this.exposeMeCheckBox = (CheckBox) findViewById(R.id.ExposeMeBtn);
//        this.uuidEditText = (EditText) findViewById(R.id.UuidEditText);
//        this.uuid2EditText = (EditText) findViewById(R.id.Uuid2EditText);
//        this.uuid3EditText = (EditText) findViewById(R.id.Uuid3EditText);

        String mapScaleValue = intent.getStringExtra("mapScale");
        if (mapScaleValue != null && mapScaleValue != "") {
            this.mapScale = Float.parseFloat(mapScaleValue);
        }

        //this.image = (MarkableTouchImageView) findViewById(R.id.imgControl);

        Log.i(LOG_TAG, "Loading the indoor map from url: " + currentBuilding.MapUrl);
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
                            NaviActivity.this)
                            .setIcon(
                                    android.R.drawable.ic_dialog_alert)
                            .setTitle("失败")
                            .setMessage(
                                    "加载当前停车场地图失败")
                            .setPositiveButton("Ok", null).show();
                } else {
                    final Drawable mapDrawable = new BitmapDrawable(getResources(),
                            (Bitmap) task.getSingleResult());
                    image = new MarkableTouchImageView(NaviActivity.this);//(MarkableTouchImageView) findViewById(R.id.imgControl);
                    image.setScaleType(ScaleType.CENTER_CROP);
                    image.setImageDrawable(mapDrawable);
                    image.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                    ((RelativeLayout) findViewById(R.id.imageViewHostInNavi)).addView(image);
                    image.setImageDrawable(mapDrawable);
                    image.AddBitmapInTouchImageClickedListener(NaviActivity.this);
                    setupImageView();
                    loadAllWebBoardAndShowOnUI();
                    loadSamplesDetailFromWeb();
                }
            }
        });

//        Drawable mapDrawable = new BitmapDrawable(getResources(),
//                EntryActivity.SelectedBuildingMap);
//        this.image.setImageDrawable(mapDrawable);
        // ============================

//        this.logTextView = (TextView) findViewById(R.id.logTextView);
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
                mBluetoothAdapter)) {
            Toast.makeText(getBaseContext(),
                    "Failed to start BleFingerprintCollector",
                    android.widget.Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Failed to start BleFingerprintCollector, quit");
            return;
        }


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

                Bitmap shoePrintIcon = Helper.getScaledBitmapByMapScale(BitmapFactory.decodeResource(
                        getResources(), R.drawable.shoeprints), currentBuilding.MapScale, 2, false);
                List<DrawImage> positions = new ArrayList<>();
                for (LocalPositionDescriptor pd : loadedBuildInSampleData) {
//                    positions.add(new DrawCircle(
//                            pd.getLocalX(),
//                            pd.getLocalY(),
//                            Helper.GetCircleRadiusByMapScale(NaviActivity.this.mapScale),
//                            pd.Description, Color.BLACK));
                    positions.add(new DrawImage(
                            pd.getLocalX(),
                            pd.getLocalY(),
                            shoePrintIcon,
                            pd.Description, Float.toString(pd.getLocalX()) + Float.toString(pd.getLocalY())));
                }

                image.drawImages(positions, false);
            }
        });

        BleFingerprintCollector.getDefault().AddOnBleSampleCollectedListener(
                this);
        // default choose Idle sampling interval
        BleFingerprintCollector.getDefault().StartSampling();


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startBleAdvertising() {
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
    }

    private void setupImageView() {
        this.image.setHighlightSelectedBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.car_selected_arrow));
        Button myButton = new Button(NaviActivity.this);
        myButton.setText("预订?");

        this.image.setParentRelativeLayoutAndMenuView(
                (RelativeLayout) findViewById(R.id.imageViewHostInNavi),
                myButton);
        this.image.setScaleType(ScaleType.CENTER_CROP);
    }

    private void loadSamplesDetailFromWeb() {
        final ProgressDialog progressSample = ProgressDialog.show(NaviActivity.this, "获取中...",
                "获取采样点整体信息", true);
        Action<String> getRestSamplesForABuildingAction = new RestAction("samples/?ownerBuildingId=" + currentBuilding.Id, userName, password, "GET", "getRestSamplesForABuilding");
        Task.Create(getRestSamplesForABuildingAction).Start(
                new OnActionFinishedListener<String>() {
                    @Override
                    public void Finished(Task task, Action<String> finishedAction) {
                        progressSample.dismiss();
                        if (task.isFaulted()) {
                            new AlertDialog.Builder(
                                    NaviActivity.this)
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

                                Log.i(LOG_TAG, "Samples total " + samplesForBuilding.size()
                                        + ", and SampleDescriptor total " + furtherActionUrls.size() + " loaded from Web");

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                new AlertDialog.Builder(
                                        NaviActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("Resolve underlying sample failed")
                                        .setMessage("!!!")
                                        .setPositiveButton("Failed", null)
                                        .show();
                                return;
                            }


                            final ProgressDialog progressSampleDesc = ProgressDialog.show(NaviActivity.this, "获取中...",
                                    "获取所有具体采样点信息", true);
                            Task.Create(new BulkRestClient(furtherActionUrls, userName, password, "GET", "furtherTask")).Start(
                                    new OnActionFinishedListener<String>() {
                                        @Override
                                        public void Finished(Task task, Action<String> finishedAction) {
                                            progressSampleDesc.dismiss();
                                            if (!task.isCompleted()) return;
                                            if (task.isFaulted()) {
                                                new AlertDialog.Builder(
                                                        NaviActivity.this)
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
                                                        NaviActivity.this)
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

                                            NaviActivity.this.loadedBuildInSampleData.clear();
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
                                                NaviActivity.this.loadedBuildInSampleData.add(__);
                                            }

                                            //Log.e(LOG_TAG, "Samples total " + NaviActivity.this.loadedBuildInSampleData.size() + " loaded from Web");

                                            Toast.makeText(
                                                    getBaseContext(),
                                                    "existed data loaded from web(total " + NaviActivity.this.loadedBuildInSampleData.size() + ")",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        }
                    }
                }

        );
    }

    private Handler waitSometimeHandler = new Handler();

    @Override
    protected void onStart() {
        super.onStart(); // Always call the superclass method first

    }

    private void loadAllWebBoardAndShowOnUI() {
        //http://rest.shaojun.xyz:8090/boards/
        RestAction scanBoardsStatusAction = new RestAction("boards/?ownerBuildingId=" + currentBuilding.Id, userName, password, "GET", "scanBoardsStatus");
        Task.Create(scanBoardsStatusAction).Start(
                new OnActionFinishedListener<String>() {
                    @Override
                    public void Finished(Task task, Action<String> lastFinishedAction) {
                        if (task.isFaulted()) {
                            //findViewById(R.id.editTextPwd).setEnabled(true);
                            new AlertDialog.Builder(
                                    NaviActivity.this)
                                    .setIcon(
                                            android.R.drawable.ic_dialog_alert)
                                    .setTitle("scanBoardsStatus failed")
                                    .setMessage(task.getSingleException().toString())
                                    .setPositiveButton("Failed", null)
                                    .show();
                        } else {
                            // we only want to get the add or updated board and put them into changeTracker, this is for minimize
                            // the call for UI draw
                            ArrayList<Tuple<Board, String>> changeTracker = new ArrayList<>();
                            try {
                                List<Board> remoteBoards = RestEntityResultDumper.dump(task.getSingleResult().toString(), Board.class);
                                Log.d(LOG_TAG, "Boards total " + remoteBoards.size() + " loaded from web");
                                for (Board remoteBoard : remoteBoards) {
                                    if (!NaviActivity.this.boardsInBuilding.contains(remoteBoard)) {
                                        changeTracker.add(new Tuple<>(remoteBoard, "add"));
                                        Log.v(LOG_TAG, "Board:" + remoteBoard.BoardIdentity + " is marked as 'add'");
                                    } else {
                                        Board existed = null;
                                        for (Board _ : NaviActivity.this.boardsInBuilding) {
                                            if (_.BoardIdentity.equals(remoteBoard.BoardIdentity))
                                                existed = _;
                                        }

                                        if (existed.IsCovered != remoteBoard.IsCovered
                                                || !existed.CoordinateX.equals(remoteBoard.CoordinateX)
                                                || !existed.CoordinateY.equals(remoteBoard.CoordinateY)
                                                || !existed.Description.equals(remoteBoard.Description)
                                                || !existed.DetailUrl.equals(remoteBoard.DetailUrl)
                                                || (existed.OrderDetailUrl == null && remoteBoard.OrderDetailUrl != null)
                                                || (existed.OrderDetailUrl != null && remoteBoard.OrderDetailUrl == null)
                                                || (existed.OrderDetailUrl != null && remoteBoard.OrderDetailUrl != null && !existed.OrderDetailUrl.equals(remoteBoard.OrderDetailUrl))
                                                ) {
                                            changeTracker.add(new Tuple<>(remoteBoard, "update"));
                                            Log.v(LOG_TAG, "Board:" + remoteBoard.BoardIdentity + " is marked as 'update'");
                                        }
                                    }
                                }

                                for (Board existedBoard : NaviActivity.this.boardsInBuilding) {
                                    if (!remoteBoards.contains(existedBoard)) {
                                        changeTracker.add(new Tuple<>(existedBoard, "delete"));
                                        Log.v(LOG_TAG, "Board:" + existedBoard.BoardIdentity + " is marked as 'delete'");
                                    }
                                }

                                NaviActivity.this.boardsInBuilding.clear();
                                NaviActivity.this.boardsInBuilding.addAll(remoteBoards);
                            } catch (Exception e) {
                                e.printStackTrace();
                                new AlertDialog.Builder(
                                        NaviActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("Resolve boards failed")
                                        .setMessage(e.getMessage())
                                        .setPositiveButton("Failed", null)
                                        .show();
                                Log.e(LOG_TAG, "Resolve boards failed: " + e.getMessage());
                                return;
                            }

                            if (changeTracker.size() > 0) {
                                List<Board> addOrUpdateBoards = new ArrayList<>();
                                List<String> deleteBoardIds = new ArrayList<>();
                                for (Tuple<Board, String> change : changeTracker) {
                                    if (change.second.equals("delete"))
                                        deleteBoardIds.add(change.first.BoardIdentity);
                                    else
                                        addOrUpdateBoards.add(change.first);
                                }

                                image.drawImagesWithRemove(deleteBoardIds);
                                List<DrawImage> drawImages = Helper
                                        .ConvertRestBoardsToDrawImages(addOrUpdateBoards,
                                                ((BitmapDrawable) image.getDrawable()).getBitmap(),
                                                image,
                                                getResources(), currentBuilding.MapScale, 3);
                                image.drawMultipleCirclesAndImages(null, drawImages);
                            }

                            waitSometimeHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (keepPollingAllParkingPositionsFromWeb)
                                        loadAllWebBoardAndShowOnUI();
                                }
                            }, 6000);
                        }
                    }
                }

        );
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
        BleFingerprintCollector.getDefault().StopSampling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // load the parking positions from web
        this.keepPollingAllParkingPositionsFromWeb = true;
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

    private String selectedBitMapId = "-1";
    ProgressDialog orderingProgressDialog = null;

    @Override
    public void onBitMapClicked(final String touchedBitMapId) {
        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibe.vibrate(100);
        selectedBitMapId = touchedBitMapId;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(NaviActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("预订")
                        .setMessage(
                                "确定预订车位：" + selectedBitMapId
                                        + " 吗?")
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        SendOrderOrRevertParkingPositionRequest(selectedBitMapId, false);
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .setNeutralButton("取消已有预订",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        SendOrderOrRevertParkingPositionRequest(selectedBitMapId, true);
                                    }
                                }).show();
            }
        });
    }

    private void HandleScannedRawFingerprints(
            List<ScannedBleDevice> rawFingerprints) {
        HashSet<ScannedBleDevice> averagedFingerprints = Util
                .DistinctAndAvgFingerprint(rawFingerprints);
        Log.v(LOG_TAG_LOCATION, "averagedFingerprints collected:");
        Log.v(LOG_TAG_LOCATION, Helper.ToLogString0(averagedFingerprints));
        Log.v(LOG_TAG_LOCATION, "averagedFingerprints collected finished.");
        // need calculate the similarity against all the BuildInSampleData
        final ArrayList<Tuple<Double, LocalPositionDescriptor>> sortedSimilarityList = new ArrayList<>();
        if (this.loadedBuildInSampleData != null
                && this.loadedBuildInSampleData.size() > 0) {
            for (LocalPositionDescriptor pd : this.loadedBuildInSampleData) {
                sortedSimilarityList.add(new Tuple<>(
                        Util.CaculateSimilarity(averagedFingerprints,
                                pd.Fingerprints), pd));
            }
        } else {
            return;
        }

        Collections.sort(sortedSimilarityList, new SimilarityComparator());
        Log.v(LOG_TAG_LOCATION, "similarity caculated:");
        Log.v(LOG_TAG_LOCATION, Helper.ToLogString1(sortedSimilarityList));
        Log.v(LOG_TAG_LOCATION, "similarity caculated finished");
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
        for (Tuple<Double, LocalPositionDescriptor> single : sortedSimilarityList) {
            allCandidatesWithText.add(new Tuple<>(
                    new Tuple<Float, Float>(single.second.getLocalX(), single.second.getLocalY()),
                    single.second.Description + ", s: "
                            + single.first.intValue()));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!allCandidatesWithText.isEmpty()) {
                    // we want the circle has 2m radius.
                    Bitmap shoePrintIcon = Helper.getScaledBitmapByMapScale(BitmapFactory.decodeResource(
                            getResources(), R.drawable.shoeprints), currentBuilding.MapScale, 2, false);
                    // only draw the highest similarity point
                    image.drawImageNewOrUpdate(new DrawImage(
                            allCandidatesWithText
                                    .get(allCandidatesWithText.size() - 1).first.first,
                            allCandidatesWithText.get(allCandidatesWithText
                                    .size() - 1).first.second,
                            shoePrintIcon, "", "you get here by your feet"));
                }
            }
        });
    }

    private void SendOrderOrRevertParkingPositionRequest(String boardId, boolean revert) {
        if (!revert) {
            NaviActivity.this.orderingProgressDialog = ProgressDialog.show(
                    NaviActivity.this, "requesting...",
                    "wait for create your order...");

            RestAction addOrderAction = new RestAction("orders/",
                    userName, password, "POST", boardId);
//            {
//                    "owner": "http://rest.shaojun.xyz:8090/usersInfo/7/",
//                    "status": "new",
//                    "to_Board": "http://rest.shaojun.xyz:8090/boards/macmac/",
//                    "isActive": false
//            }
            Order newOrder = new Order();
            newOrder.Status = "test";
            newOrder.ToBoardUrl = Service.ServiceUrl + "boards/" + boardId + "/";
            newOrder.OwnerUrl = UserInfo.CurrentUserInfo.Url;
            addOrderAction.AddPostJsonObject(newOrder.toJsonObject());
            Task.Create(addOrderAction).Start(
                    new OnActionFinishedListener<String>() {
                        @Override
                        public void Finished(Task task, Action<String> finishedAction) {
                            NaviActivity.this.orderingProgressDialog.dismiss();
                            if (task.isFaulted()) {
                                if (task.getFirstErrorHttpResponseCode() == 409) {
                                    new AlertDialog.Builder(
                                            NaviActivity.this)
                                            .setIcon(
                                                    android.R.drawable.ic_dialog_alert)
                                            .setTitle("失败")
                                            .setMessage(
                                                    "此车位已经被预订过！")
                                            .setPositiveButton("Ok", null).show();
                                    return;
                                }

                                new AlertDialog.Builder(
                                        NaviActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("失败")
                                        .setMessage(
                                                "创建订单失败")
                                        .setPositiveButton("Ok", null).show();
                                return;
                            } else

                            {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(NaviActivity.this)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .setTitle("预订成功")
                                                .setMessage("成功预订了车位")
                                                .setPositiveButton("OK", null).show();
                                    }
                                });
                            }
                        }
                    }
            );
        } else

        {
            NaviActivity.this.orderingProgressDialog = ProgressDialog.show(
                    NaviActivity.this, "requesting...",
                    "wait for revert your order...");
            RestAction queryDeletingOrderAction = new RestAction("orders/?to_BoardId=" + boardId, userName, password, "GET", "queryDeletingOrderAction");
            Task.Create(queryDeletingOrderAction)
                    .Start(new OnActionFinishedListener<String>() {
                               @Override
                               public void Finished(Task task, Action<String> finishedAction) {
                                   if (task.isFaulted()) {
                                       NaviActivity.this.orderingProgressDialog.dismiss();
                                       new AlertDialog.Builder(
                                               NaviActivity.this)
                                               .setIcon(
                                                       android.R.drawable.ic_dialog_alert)
                                               .setTitle("失败")
                                               .setMessage(
                                                       "查询待删除的订单失败，请重试")
                                               .setPositiveButton("Ok", null).show();
                                       return;
                                   } else {
                                       NaviActivity.this.orderingProgressDialog.dismiss();
                                       Order deletingOrder;
                                       try {
                                           deletingOrder = RestEntityResultDumper.dump(task.getSingleResult().toString(), Order.class).get(0);
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                           new AlertDialog.Builder(
                                                   NaviActivity.this)
                                                   .setIcon(
                                                           android.R.drawable.ic_dialog_alert)
                                                   .setTitle("Resolve underlying new added Sample failed")
                                                   .setMessage("resolving deleting order detail failed")
                                                   .setPositiveButton("Failed", null)
                                                   .show();
                                           return;
                                       }

                                       RestAction delOrderAction = new RestAction(deletingOrder.Url,
                                               userName, password, "DELETE", "DELETE");
                                       Task.Create(delOrderAction).Start(new OnActionFinishedListener<String>() {
                                           @Override
                                           public void Finished(Task task, Action<String> finishedAction) {
                                               NaviActivity.this.orderingProgressDialog.dismiss();
                                               if (task.isFaulted()) {
                                                   new AlertDialog.Builder(
                                                           NaviActivity.this)
                                                           .setIcon(
                                                                   android.R.drawable.ic_dialog_alert)
                                                           .setTitle("失败")
                                                           .setMessage(
                                                                   "删除订单失败，请重试")
                                                           .setPositiveButton("Ok", null).show();
                                                   return;
                                               } else {
                                                   new AlertDialog.Builder(
                                                           NaviActivity.this)
                                                           .setIcon(
                                                                   android.R.drawable.ic_dialog_alert)
                                                           .setTitle("成功")
                                                           .setMessage(
                                                                   "删除订单成功")
                                                           .setPositiveButton("Ok", null).show();
                                               }
                                           }
                                       });
//            {
//                    "owner": "http://rest.shaojun.xyz:8090/usersInfo/7/",
//                    "status": "new",
//                    "to_Board": "http://rest.shaojun.xyz:8090/boards/macmac/",
//                    "isActive": false
//            }


                                   }


                               }
                           }
                    );
        }
    }
}
