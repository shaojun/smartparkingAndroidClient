package com.example.notificationproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.R.string;
import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyNotificationProxyService extends AccessibilityService {
	private String logTag = "MyNotificationProxyService";
	Map<String, String> InterstedNotificationPackageNames = new HashMap<String, String>();

	public MyNotificationProxyService() {
		// packagename:nickname
		InterstedNotificationPackageNames.put("com.tencent.mobileqq", "QQ");
		InterstedNotificationPackageNames.put("com.tencent.mm", "WeChat");
		InterstedNotificationPackageNames.put("com.android.dialer", "Call");
		InterstedNotificationPackageNames.put("com.android.mms", "SMS");
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		// Log.e(logTag, "incoming: " + event.getEventType());
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			// nodeInfo.getText()
			final String packagename = String.valueOf(event.getPackageName());
			final String fullText = String.valueOf(event.getText());

			String friendPackagename = "";
			// skip the one send by self
			if (packagename.contains("com.example.notificationproxy")
					|| !InterstedNotificationPackageNames
							.containsKey(packagename)
					|| (fullText.equals("[]") && !packagename
							.equals("com.android.dialer"))) {
				Log.e(logTag, "Ignore notification with content: " + fullText
						+ " from packageName: " + packagename);
				return;
			}

			friendPackagename = InterstedNotificationPackageNames
					.get(packagename);
			Log.e(logTag, "capatured notfication with content: " + fullText
					+ " from packageName: " + packagename + "("
					+ friendPackagename + ")");
			int notificationId = 19831004;

			String fullPinyin = getFullPinYin(fullText);
			Log.e(logTag, "fullPinyin: " + fullPinyin);

			Intent dismissIntent = new Intent(this, MainActivity.class);
			// dismissIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
			// | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			dismissIntent.putExtra("action", "dismiss");
			dismissIntent.putExtra("notificationId", notificationId);
			dismissIntent.putExtra("from", friendPackagename);
			dismissIntent.putExtra("text", fullPinyin);
			PendingIntent dismissPendingIntent = PendingIntent.getActivity(
					this, 0, dismissIntent, 0);

			// Intent readMoreIntent = new Intent(this, MainActivity.class);
			// readMoreIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
			// | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			// readMoreIntent.putExtra("action", "readMore");
			// readMoreIntent.putExtra("notificationId", notificationId);
			// readMoreIntent.putExtra("from", packagename);
			// readMoreIntent.putExtra("text", String.valueOf(event.getText()));
			// PendingIntent readMorePendingIntent = PendingIntent.getActivity(
			// this, 0, readMoreIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			// build notification
			// the addAction re-use the same intent to keep the example short
			Notification n = new NotificationCompat.Builder(this)
					.setContentTitle(friendPackagename)
					.setContentText(fullPinyin)
					.setSmallIcon(getNotificationIcon())
					// .setNumber(5)
					.setContentIntent(dismissPendingIntent).setAutoCancel(true)
					// .addAction(R.drawable.small_icon0, "More",
					// readMorePendingIntent)
					// .addAction(R.drawable.small_icon0, "Dismiss all",
					// dismissPendingIntent)
					.build();
			n.flags |= Notification.FLAG_AUTO_CANCEL;
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.notify(notificationId, n);
		}
	}

	private int getNotificationIcon() {
		boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
		return whiteIcon ? R.drawable.small_icon0 : R.drawable.ic_launcher;
	}
	
	@Override
    public void onInterrupt()
    {
		Log.e(logTag, "***** onInterrupt");
    	Intent dismissIntent = new Intent(this, MainActivity.class);
		PendingIntent dismissPendingIntent = PendingIntent.getActivity(
				this, 0, dismissIntent, 0);
		Notification n = new NotificationCompat.Builder(this)
				.setContentTitle("Disconnected")
				.setContentText("Fenix3 msg proxy disconnected")
				.setSmallIcon(getNotificationIcon())
				.setContentIntent(dismissPendingIntent).setAutoCancel(true)
				.build();
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(1919191919, n);
    }

    @Override
    public void onServiceConnected()
    {
    	Log.e(logTag, "***** onServiceConnected");
    	Intent dismissIntent = new Intent(this, MainActivity.class);
		PendingIntent dismissPendingIntent = PendingIntent.getActivity(
				this, 0, dismissIntent, 0);
		Notification n = new NotificationCompat.Builder(this)
				.setContentTitle("Connected")
				.setContentText("Fenix3 msg proxy connected")
				.setSmallIcon(getNotificationIcon())
				.setContentIntent(dismissPendingIntent).setAutoCancel(true)
				.build();
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(1919191919, n);
    }

	public String getFullPinYin(String source) {
		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
		char[] input = source.trim().toCharArray();
		StringBuffer output = new StringBuffer("");

		try {
			for (int i = 0; i < input.length; i++) {
				if (Character.toString(input[i]).matches("[\u4E00-\u9FA5]+")) {
					String[] temp = PinyinHelper.toHanyuPinyinStringArray(
							input[i], format);
					output.append(temp[0]);
					output.append(" ");
				} else
					output.append(Character.toString(input[i]));
			}
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			e.printStackTrace();
		}

		return output.toString();

	}

	// Access a web url by Get method, return the HttpResponse.
	public static String GetHttpResponseByUrl(String fullUrlWithAllParameters) {
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpGet httpget = new HttpGet(fullUrlWithAllParameters);

		// Execute the request
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			// Examine the response status
			Log.i("Praeda", response.getStatusLine().toString());

			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {

				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String result = convertStreamToString(instream);
				// now you have the string representation of the HTML request
				instream.close();
				return result;
			}

		} catch (Exception e) {
		}
		return null;

	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
