package com.example.myfirstandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class NavigationInputActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_input);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation_input, menu);
		return true;
	}
	
	public void button_DestinationEntered(View view)
	{
		Intent intent = new Intent(this, OverviewParkingPositionActivity.class);
	    EditText editText = (EditText) findViewById(R.id.destEditText);
	    String message = editText.getText().toString();
	    intent.putExtra("Dest", message);
	    startActivity(intent);
	}

}
