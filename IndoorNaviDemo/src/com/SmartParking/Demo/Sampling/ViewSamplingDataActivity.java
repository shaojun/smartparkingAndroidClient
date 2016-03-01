package com.SmartParking.Demo.Sampling;

import java.util.ArrayList;
import java.util.List;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Lookup.LocalPositionDescriptor;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.Task.Action;
import com.SmartParking.Task.OnActionFinishedListener;
import com.SmartParking.Task.RestAction;
import com.SmartParking.Task.Task;
import com.SmartParking.UI.ExpandableListViewAdapter;
import com.SmartParking.UI.ExpandableListViewItem;
import com.SmartParking.Util.Util;
import com.SmartParking.WebService.BulkRestClient;
import com.SmartParking.WebService.RestClient;
import com.SmartParking.WebService.RestEntityResultDumper;
import com.SmartParking.WebServiceEntity.Building;
import com.SmartParking.WebServiceEntity.Sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ViewSamplingDataActivity extends Activity {
    private ExpandableListView sampleExpandableListView = null;
    private ArrayList<LocalPositionDescriptor> loadedData;
    private int selectGroupIndex = -1;
    private String userName = "";
    private String password = "";
    private ProgressDialog deletingFromWebProgress;
    private Building currentBuilding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewsamplingdata);
        SharedPreferences logOnSharedPreferences = this.getSharedPreferences("LogOn", 0);
        userName = logOnSharedPreferences.getString("UserName", null);
        password = logOnSharedPreferences.getString("Password", null);
        Intent intent = getIntent();
        this.currentBuilding = (Building) (intent.getSerializableExtra("Building"));
        sampleExpandableListView = (ExpandableListView) findViewById(R.id.sampleExpandableListView);
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        final EditText deleteEditText = (EditText) findViewById(R.id.deleteEditText);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int deletingIndex = Integer.parseInt(deleteEditText.getText().toString());
                // make sure we have the data to be deleting.
                if (ViewSamplingDataActivity.this.loadedData.size() < deletingIndex + 1) return;
                new AlertDialog.Builder(
                        ViewSamplingDataActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("sure?")
                        .setMessage(
                                "Delete Sample with line No.(0 based): " + deletingIndex
                                        + "?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton(
                                "Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        deletingFromWebProgress = ProgressDialog.show(ViewSamplingDataActivity.this, "删除中...",
                                                "删除中", true);
                                        LocalPositionDescriptor deletingLPD = ViewSamplingDataActivity.this.loadedData.get(deletingIndex);
                                        Task.Create(new RestAction("samples/?ownerBuildingId=" + currentBuilding.Id
                                                + "&coordinateX=" + (int) (deletingLPD.getRemoteX()) + "&coordinateY=" + (int) (deletingLPD.getRemoteY()),
                                                userName, password, "GET", deletingLPD)).Start(
                                                new OnActionFinishedListener<String>() {
                                                    @Override
                                                    public void Finished(Task task, Action<String> finishedAction) {
                                                        if (task.isFaulted()) {
                                                            deletingFromWebProgress.dismiss();
                                                            new AlertDialog.Builder(
                                                                    ViewSamplingDataActivity.this)
                                                                    .setIcon(
                                                                            android.R.drawable.ic_dialog_alert)
                                                                    .setTitle("失败")
                                                                    .setMessage(
                                                                            "获取待删除采样点数据失败(stage 0)")
                                                                    .setPositiveButton("Ok", null).show();
                                                            return;
                                                        } else {
                                                            final List<Sample> samples = new ArrayList<>();
                                                            try {
                                                                samples.addAll(RestEntityResultDumper.dump(task.getSingleResult().toString(), Sample.class));
                                                                if (samples.size() != 1) {
                                                                    new AlertDialog.Builder(
                                                                            ViewSamplingDataActivity.this)
                                                                            .setIcon(
                                                                                    android.R.drawable.ic_dialog_alert)
                                                                            .setTitle("失败")
                                                                            .setMessage(
                                                                                    "待删除采样点数据个数为：" + samples.size() + ", 应该仅为1个！")
                                                                            .setPositiveButton("Ok", null).show();
                                                                    return;
                                                                }
                                                            } catch (Exception e) {
                                                                deletingFromWebProgress.dismiss();
                                                                e.printStackTrace();
                                                                new AlertDialog.Builder(
                                                                        ViewSamplingDataActivity.this)
                                                                        .setIcon(
                                                                                android.R.drawable.ic_dialog_alert)
                                                                        .setTitle("Resolve underlying Sample failed")
                                                                        .setMessage("!!!")
                                                                        .setPositiveButton("Failed", null)
                                                                        .show();
                                                                return;
                                                            }

                                                            Task.Create(new RestAction(samples.get(0).DetailUrl, userName, password, "DELETE", deletingIndex))
                                                                    .Start(new OnActionFinishedListener<String>() {
                                                                        @Override
                                                                        public void Finished(Task task, Action<String> finishedAction) {
                                                                            deletingFromWebProgress.dismiss();
                                                                            if (task.isFaulted()) {
                                                                                new AlertDialog.Builder(
                                                                                        ViewSamplingDataActivity.this)
                                                                                        .setIcon(
                                                                                                android.R.drawable.ic_dialog_alert)
                                                                                        .setTitle("失败")
                                                                                        .setMessage(
                                                                                                "删除采样点失败(stage 4)")
                                                                                        .setPositiveButton("Ok", null).show();
                                                                                return;
                                                                            } else if (task.isCompleted()) {
                                                                                ViewSamplingDataActivity.this.loadedData
                                                                                        .remove(deletingIndex);
                                                                                BindUI();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                }).show();
            }
        });
        this.BindUI();
    }
//        sampleExpandableListView
//                .setOnItemLongClickListener(new OnItemLongClickListener() {
//                    @Override
//                    public boolean onItemLongClick(AdapterView<?> parent,
//                                                   View view, int position, long id) {
//                        int itemType = ExpandableListView
//                                .getPackedPositionType(id);
//
//                        if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
//                            int childPosition = ExpandableListView
//                                    .getPackedPositionChild(id);
//                            int groupPosition = ExpandableListView
//                                    .getPackedPositionGroup(id);
//
//                            // do your per-item callback here
//                            return false; // true if we consumed the click,
//                            // false if not
//
//                        } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
//                            // int groupPosition = ExpandableListView
//                            // .getPackedPositionGroup(id);
//                            selectGroupIndex = position;
//                            new AlertDialog.Builder(
//                                    ViewSamplingDataActivity.this)
//                                    .setIcon(android.R.drawable.ic_dialog_alert)
//                                    .setTitle("sure?")
//                                    .setMessage(
//                                            "Want to delete group " + position
//                                                    + "?")
//                                    .setPositiveButton(
//                                            "Yes",
//                                            new DialogInterface.OnClickListener() {
//
//                                                @Override
//                                                public void onClick(
//                                                        DialogInterface dialog,
//                                                        int which) {
//                                                    ViewSamplingDataActivity.this.loadedData
//                                                            .remove(selectGroupIndex);
//                                                    Helper.WriteObjectToFile(
//                                                            ViewSamplingDataActivity.this.loadedData,
//                                                            Helper.privateDataFileName,
//                                                            getBaseContext());
//                                                    BindUI();
//                                                }
//
//                                            }).setNegativeButton("No", null)
//                                    .show();
//                            return true; // true if we consumed the click, false
//                            // if not
//
//                        } else {
//                            // null item; we don't consume the click
//                            return false;
//                        }
//                    }
//                });
    // Toast.makeText(getBaseContext(),
    // "Succeed to load " + loadData.size() + " pieces of samples",
    // android.widget.Toast.LENGTH_SHORT).show();
//            }

    private void BindUI() {
        this.loadedData = MainActivity.InMemPositionDescriptors;
        // if (this.loadedData == null)
        // return;
        List<ExpandableListViewItem> itemList = new ArrayList<ExpandableListViewItem>();
        if (this.loadedData != null) {
            int itemCounter = 0;
            for (int i = 0; i < this.loadedData.size(); i++) {
                LocalPositionDescriptor pd = this.loadedData.get(i);
                String flushedToWebStr = "";
                if (pd.FlushedToWeb)
                    flushedToWebStr = "fl";
                else
                    flushedToWebStr = "unfl";
                ExpandableListViewItem parentNode = new ExpandableListViewItem(
                        "[" + itemCounter + "," + flushedToWebStr + "]" + "X:Y-> lo " + pd.getLocalX() + ":" + pd.getLocalY() +
                                ", re " + pd.getRemoteX() + ":" + pd.getRemoteY()
                                + "->" + pd.Description);
                for (ScannedBleDevice sDevice : pd.Fingerprints) {
                    parentNode.addChildItem(new ExpandableListViewItem(
                            sDevice.DeviceName + ", uuid:" + Util.BytesToHexString(sDevice.IbeaconProximityUUID)
                                    + ", major:" + Util.BytesToHexString(sDevice.Major)
                                    + ", minor:" + Util.BytesToHexString(sDevice.Minor)
                                    + ", rssi:" + sDevice.RSSI));
                }

                itemList.add(parentNode);
                itemCounter++;
            }
        }

        ExpandableListViewAdapter adapter = new ExpandableListViewAdapter(this,
                itemList);
        sampleExpandableListView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
