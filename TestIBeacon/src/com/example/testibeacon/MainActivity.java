package com.example.testibeacon;

import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;

import android.os.Bundle;
import android.os.RemoteException;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements IBeaconConsumer {
	TextView debugLabel = null;
	protected static final String TAG = "RangingActivity";
	private IBeaconManager iBeaconManager = IBeaconManager
			.getInstanceForApplication(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.debugLabel = (TextView) findViewById(R.id.DebugLabel);
		iBeaconManager.bind(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onIBeaconServiceConnect() {
		iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
			@Override
			public void didEnterRegion(Region region) {
				debugLabel.setText("I just saw an iBeacon for the firt time!");
				Log.i(TAG, "I just saw an iBeacon for the firt time!");
			}

			@Override
			public void didExitRegion(Region region) {
				debugLabel.setText("I no longer see an iBeacon");
				Log.i(TAG, "I no longer see an iBeacon");
			}

			@Override
			public void didDetermineStateForRegion(int state, Region region) {
				debugLabel
						.setText("I have just switched from seeing/not seeing iBeacons: "
								+ state);
				Log.i(TAG,
						"I have just switched from seeing/not seeing iBeacons: "
								+ state);
			}
		});

		try {
			iBeaconManager.startMonitoringBeaconsInRegion(new Region(
					"myMonitoringUniqueId", null, null, null));
		} catch (RemoteException e) {
		}

	}

}
