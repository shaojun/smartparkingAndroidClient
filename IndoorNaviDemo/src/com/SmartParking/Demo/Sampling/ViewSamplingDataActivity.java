package com.SmartParking.Demo.Sampling;

import java.util.ArrayList;
import java.util.List;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.ExpandableListViewAdapter;
import com.SmartParking.UI.ExpandableListViewItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ViewSamplingDataActivity extends Activity {
	private ExpandableListView sampleExpandableListView = null;
	private ArrayList<PositionDescriptor> loadedData;
	private int selectGroupIndex = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewsamplingdata);
		TextView logTextView = (TextView) findViewById(R.id.logTextView);
		Button cleanAllButton = (Button) findViewById(R.id.buttonCleanAll);
		sampleExpandableListView = (ExpandableListView) findViewById(R.id.sampleExpandableListView);
		this.BindUI();
		cleanAllButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Helper.WriteObjectToFile(null, Helper.privateDataFileName,
						getBaseContext());
				MainActivity.InMemPositionDescriptors.clear();
				BindUI();
			}
		});
		sampleExpandableListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						int itemType = ExpandableListView
								.getPackedPositionType(id);

						if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
							int childPosition = ExpandableListView
									.getPackedPositionChild(id);
							int groupPosition = ExpandableListView
									.getPackedPositionGroup(id);

							// do your per-item callback here
							return false; // true if we consumed the click,
											// false if not

						} else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
							// int groupPosition = ExpandableListView
							// .getPackedPositionGroup(id);
							selectGroupIndex = position;
							new AlertDialog.Builder(
									ViewSamplingDataActivity.this)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.setTitle("sure?")
									.setMessage(
											"Want to delete group " + position
													+ "?")
									.setPositiveButton(
											"Yes",
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													ViewSamplingDataActivity.this.loadedData
															.remove(selectGroupIndex);
													Helper.WriteObjectToFile(
															ViewSamplingDataActivity.this.loadedData,
															Helper.privateDataFileName,
															getBaseContext());
													BindUI();
												}

											}).setNegativeButton("No", null)
									.show();
							return true; // true if we consumed the click, false
											// if not

						} else {
							// null item; we don't consume the click
							return false;
						}
					}
				});
		// Toast.makeText(getBaseContext(),
		// "Succeed to load " + loadData.size() + " pieces of samples",
		// android.widget.Toast.LENGTH_SHORT).show();
	}

	private void BindUI() {
		this.loadedData = Helper.LoadSamplingData(Helper.privateDataFileName,
				getBaseContext());
		// if (this.loadedData == null)
		// return;
		List<ExpandableListViewItem> itemList = new ArrayList<ExpandableListViewItem>();
		if (this.loadedData != null) {
			int itemCounter = 0;
			for (PositionDescriptor pd : this.loadedData) {
				ExpandableListViewItem parentNode = new ExpandableListViewItem(
						"[" + itemCounter + "]" + " X: " + pd.getOriginalMesuredOnWidth() + ", Y: "
								+ pd.getOriginalMesuredOnHeight() + "->" + pd.Description);
				for (ScannedBleDevice sDevice : pd.Fingerprints) {
					parentNode.addChildItem(new ExpandableListViewItem(
							sDevice.DeviceName + ", mac:" + sDevice.MacAddress
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
