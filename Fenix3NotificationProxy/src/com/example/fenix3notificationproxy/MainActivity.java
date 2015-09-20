package com.example.fenix3notificationproxy;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	private String logTag = "MyNotificationProxyServiceMainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey("notificationId")) {
				String action = extras.getString("action");
				int notificationId = extras.getInt("notificationId");
				Log.e(logTag, "intent with notificationId: "+Integer.toString(notificationId)+", action: "+action);
				finish();
				return;
			}
		}

		Button setPriviledgeButton = (Button) findViewById(R.id.setPriviledgeButton);
		setPriviledgeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e(logTag, "trying to set priviledge");
				
				Intent intent = new Intent(
						"android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
				startActivity(intent);
			}
		});
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
