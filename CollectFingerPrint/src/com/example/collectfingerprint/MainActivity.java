package com.example.collectfingerprint;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final ImageView buildingImageView = (ImageView) findViewById(R.id.BuildingIV);
		// buildingImageView.setLeft(0);
		// buildingImageView.setTop(0);
		final TextView tx = (TextView) findViewById(R.id.Tx);
		tx.setTop(400);
		buildingImageView.setImageResource(R.drawable.ejbm);
		buildingImageView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					tx.setText("getX: " + event.getX() + ", getY: "
							+ event.getY());
					break;

				}
				Bitmap newBT = Util.DrawCircleOnBitmap(BitmapFactory
						.decodeResource(getResources(), R.drawable.ejbm), 59,
						50, 20);
				buildingImageView.setImageBitmap(newBT);
				//buildingImageView.set
				return false;
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
