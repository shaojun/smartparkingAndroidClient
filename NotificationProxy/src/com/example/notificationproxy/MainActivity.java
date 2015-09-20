package com.example.notificationproxy;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private String logTag = "MyNotificationProxyServiceMainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//onNewIntent(getIntent());
		finish();
	}

	@Override
	public void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.containsKey("notificationId")) {
				setContentView(R.layout.activity_main);
				String action = extras.getString("action");
				if (action != null && action.equalsIgnoreCase("dismiss")) {
					NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					manager.cancel(extras.getInt("notificationId", -1));
					finish(); // since finish() is called in onCreate(),
								// onDestroy()
								// will be
					// called immediately
				} else if (action != null
						&& action.equalsIgnoreCase("readMore")) {
					TextView tv = (TextView) findViewById(R.id.allContentTextView);
					String from = extras.getString("from");
					String fullText = extras.getString("text");
					tv.setText("from: " + from + "/r/n" + fullText);
				}

				Log.e(logTag,
						"action: " + action + ", from: "
								+ extras.getString("from") + ", text: "
								+ extras.getString("text"));
				Log.e(logTag,
						Integer.toString(extras.getInt("notificationId", -1)));

			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
