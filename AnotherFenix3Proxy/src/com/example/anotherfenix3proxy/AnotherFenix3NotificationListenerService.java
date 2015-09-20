package com.example.anotherfenix3proxy;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//import net.sourceforge.pinyin4j.PinyinHelper;
//import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
//import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
//import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
//import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
//import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AnotherFenix3NotificationListenerService extends
		NotificationListenerService {
	private boolean dialerRetrieved = false;
	private Lock lock = new ReentrantLock();
	private String logTag = "AnotherFenix3NotificationListenerService";
	// packagename:nickname
	private Map<String, String> InterstedNotificationPackageNames = new HashMap<String, String>();
	private final int notificationId = 19831004;
	private List<Notification> safeNotficationQueue;
	private Thread timelyPopupNotificationThread;

	public void OnCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	NotificationManager notificationManager;

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {

	}

	Runnable removeSelfNotificationRunable = new Runnable() {
		@Override
		public void run() {
			notificationManager.cancel(notificationId);
		}
	};

	private Handler waitSometimeHandler = new Handler();

	private int getNotificationIcon() {
		boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
		return whiteIcon ? R.drawable.small_icon0 : R.drawable.ic_launcher;
	}

	private List<String> GetAllDialerPackageName() {
		// Ask the PackageManager to return a list of Activities that support
		// ACTION_DIAL
		PackageManager pm = getPackageManager();
		Intent intent = new Intent(Intent.ACTION_DIAL);

		List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
		List<String> packageList = new ArrayList<String>();
		if (list != null) {
			// For each entry in the returned list, get the package name and add
			// that to a list (ignore duplicates)
			for (ResolveInfo r : list) {
				String packageName = r.activityInfo.packageName;
				if (!packageList.contains(packageName)) {
					packageList.add(packageName);
				}
			}
		}

		return packageList;
	}

	private String GetDefaultSmsPackageName() {
		// Intent smsIntent = new Intent(Intent.ACTION_SEND);
		String defaultSmsPackageName = Telephony.Sms
				.getDefaultSmsPackage(getBaseContext()); // Need to change the
															// build to API 19
		// smsIntent.setType("text/plain");
		// smsIntent.putExtra(Intent.EXTRA_TEXT,"content");
		// //if no default app is configured, then choose any app that support
		// this intent.
		// if (defaultSmsPackageName != null) {
		// smsIntent.setPackage(defaultSmsPackageName);
		// }
		Log.e(logTag, "retrieved SMS packageName: " + defaultSmsPackageName);
		return defaultSmsPackageName;

	}

	private String getFullPinYin(String source) {
		return "hello world!";
//		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
//		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
//		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//		format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
//		char[] input = source.trim().toCharArray();
//		StringBuffer output = new StringBuffer("");
//
//		try {
//			for (int i = 0; i < input.length; i++) {
//				if (Character.toString(input[i]).matches("[\u4E00-\u9FA5]+")) {
//					String[] temp = PinyinHelper.toHanyuPinyinStringArray(
//							input[i], format);
//					output.append(temp[0]);
//					output.append(" ");
//				} else
//					output.append(Character.toString(input[i]));
//			}
//		} catch (BadHanyuPinyinOutputFormatCombination e) {
//			e.printStackTrace();
//		}
//
//		try {
//			byte[] asciiBytes = output.toString().getBytes("US-ASCII");
//			String asciiEncodeString = new String(asciiBytes, "US-ASCII");
//			return asciiEncodeString;
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return "Error!";
	}

	private void popupTranslatedNotificationFromQueue() {
		if (this.safeNotficationQueue.size() == 0)
			return;

		if (notificationManager != null) {
			Notification nextNoti = this.safeNotficationQueue.remove(0);
			notificationManager.notify(notificationId, nextNoti);
			waitSometimeHandler
					.removeCallbacks(this.removeSelfNotificationRunable);
			waitSometimeHandler.postDelayed(removeSelfNotificationRunable,
					90000);
		}
	}

	public static boolean isNotificationAccessEnabled = false;

	// @Override
	// public IBinder onBind(Intent mIntent) {
	// IBinder mIBinder = super.onBind(mIntent);
	// isNotificationAccessEnabled = true;
	// return mIBinder;
	// }
	//
	// @Override
	// public boolean onUnbind(Intent mIntent) {
	// boolean mOnUnbind = super.onUnbind(mIntent);
	// isNotificationAccessEnabled = false;
	// return mOnUnbind;
	// }

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		// TAG = "onNotificationRemoved";
		// Log.d(TAG, "id = " + sbn.getId() + "Package Name" +
		// sbn.getPackageName() +
		// "Post time = " + sbn.getPostTime() + "Tag = " + sbn.getTag());

	}
}
