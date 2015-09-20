package com.example.detectnearbyibeacon;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class AsyncWebRequest extends AsyncTask<Integer,Integer,Integer> {

	@Override
	protected Integer doInBackground(Integer... params) {
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpGet httpget = new HttpGet(
				"http://192.168.123.1:8090/AuthorizeNXT.aspx");

		// Execute the request
		HttpResponse response = null;
		try {
			response = httpclient
					.execute(httpget);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated
			// catch
			// block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated
			// catch
			// block
			e.printStackTrace();
		}
		// Examine the response
		// status
		Log.i("Praeda", response
				.getStatusLine()
				.toString());
		return null;
	}
 
}
