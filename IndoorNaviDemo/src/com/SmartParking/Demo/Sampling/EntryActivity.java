package com.SmartParking.Demo.Sampling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Util.Tuple;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class EntryActivity extends Activity {

	public static Bitmap SelectedBuildingMap = null;
	private Spinner allAvailableBuildingMapsSpinner;
	private Button confirmBtnAndGoToMain = null;
	private Button confirmBtnAndGoToNavi = null;
	private ProgressDialog progressDialog = null;
	private static final String LOG_TAG = "SmarkParking.Demo.Entry";
	private ImageView selectedMapImageView = null;
	private HashMap<String, Float> mapUrlAndScale = new HashMap<String, Float>();
	private String lastSelectedMapRelativeUrl = "";
	private TextView mapScaleTextView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entry);
		this.selectedMapImageView = (ImageView) findViewById(R.id.selectedMapImageView);
		this.mapScaleTextView = (TextView) findViewById(R.id.mapScaleTextView);
		this.allAvailableBuildingMapsSpinner = (Spinner) findViewById(R.id.allAvailableBuildingMapsSpinner);
		this.progressDialog = ProgressDialog.show(EntryActivity.this,
				"loading...", "wait for loading available building maps");
		AsynLoadAllAvailableBuildingMapsNames();
		this.allAvailableBuildingMapsSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						lastSelectedMapRelativeUrl = (String) parent
								.getItemAtPosition(position);
						if (lastSelectedMapRelativeUrl != "pls Select") {
							EntryActivity.this.progressDialog = ProgressDialog
									.show(EntryActivity.this, "loading...",
											"wait for loading selected map");
							EntryActivity.this
									.AsynGetBitmapFromWeb("http://www.shaojun.xyz"
											+ lastSelectedMapRelativeUrl);

						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						// TODO Auto-generated method stub
					}
				});

		this.confirmBtnAndGoToMain = (Button) findViewById(R.id.buttonConfirmMapAndGoToMain);
		confirmBtnAndGoToMain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(EntryActivity.this, MainActivity.class);
				i.putExtra("mapScale",
						mapUrlAndScale.get(lastSelectedMapRelativeUrl).toString());
				startActivity(i);
			}
		});

		this.confirmBtnAndGoToNavi = (Button) findViewById(R.id.buttonConfirmMapAndGoToNavi);
		confirmBtnAndGoToNavi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(EntryActivity.this, NaviActivity.class);
				i.putExtra("mapScale",
						mapUrlAndScale.get(lastSelectedMapRelativeUrl).toString());
				startActivity(i);
			}
		});

		confirmBtnAndGoToMain.setEnabled(false);
		confirmBtnAndGoToNavi.setEnabled(false);
	}

	private void AsynLoadAllAvailableBuildingMapsNames() {
		new Thread(new Runnable() {
			public void run() {
				String[] items = null;
				try {
					items = Helper
							.GetWebImageFullUrlsFromListingPageUrl("http://www.shaojun.xyz/smartparking/publicservice/upload/?listing='noUse'");
					// String[] items = new String[] { "select", "1", "2",
					// "three"
					// };

					ArrayList<String> al = new ArrayList<String>();
					al.add("pls Select");
					for (String s : items) {
						if (s.contains(",")) {
							String[] urlAndScale = s.split(",");
							EntryActivity.this.mapUrlAndScale.put(
									urlAndScale[0],
									Float.parseFloat(urlAndScale[1]));

							al.add(urlAndScale[0]);
						} else {
							al.add(s);
						}
					}

					items = al.toArray(items);
					Log.e(LOG_TAG, "received building maps from web, count: "
							+ items.length);
					final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							EntryActivity.this,
							android.R.layout.simple_spinner_item, items);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							EntryActivity.this.allAvailableBuildingMapsSpinner
									.setAdapter(adapter);
						}
					});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					// Toast.makeText(getBaseContext(),
					// "failed to load WebImageFullUrls",
					// android.widget.Toast.LENGTH_SHORT).show();
				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							EntryActivity.this.progressDialog.dismiss();
						}
					});
				}
			}
		}).start();
	}

	private void AsynGetBitmapFromWeb(final String singleBitmapUrl) {
		new Thread(new Runnable() {
			public void run() {
				try {
					EntryActivity.SelectedBuildingMap = Helper
							.GetImageBitmapFromUrl(singleBitmapUrl);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getBaseContext(),
							"failed to load WebImageFullUrls",
							android.widget.Toast.LENGTH_SHORT).show();
				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (EntryActivity.SelectedBuildingMap != null) {
								Toast.makeText(
										getBaseContext(),
										"bitmap get, width: "
												+ EntryActivity.SelectedBuildingMap
														.getWidth()
												+ ", height: "
												+ EntryActivity.SelectedBuildingMap
														.getHeight(),
										android.widget.Toast.LENGTH_LONG)
										.show();

								mapScaleTextView.setText(mapUrlAndScale.get(lastSelectedMapRelativeUrl).toString());
								EntryActivity.this.confirmBtnAndGoToMain
										.setEnabled(true);
								EntryActivity.this.confirmBtnAndGoToNavi
										.setEnabled(true);
								// give a preview image to user.
								EntryActivity.this.selectedMapImageView
										.setImageBitmap(EntryActivity.SelectedBuildingMap);
							}

							EntryActivity.this.progressDialog.dismiss();
						}
					});
				}
			}
		}).start();
	}
}
