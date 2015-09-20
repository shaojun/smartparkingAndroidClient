package com.SmartParking.Demo.Sampling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Lookup.PositionDescriptor;
import com.SmartParking.Sampling.ScannedBleDevice;
import com.SmartParking.UI.DrawImage;
import com.SmartParking.Util.Tuple;
import com.SmartParking.Util.Tuple3;
import com.SmartParking.Util.Tuple4;
import com.SmartParking.Util.Tuple5;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class Helper {
	public static String privateDataFileName = "SmartParking22.data";
	private static final String LOG_TAG = "SmarkParking.Demo.Helper";

	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public static File getDocumentsStorageDir(String newDocName) {
		// Get the directory for the user's public DIRECTORY_DOCUMENTS
		// directory.
		File file = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
				newDocName);
		if (!file.mkdirs()) {
			Log.e(LOG_TAG, "Directory not created");
		}

		return file;
	}

	public static void WriteObjectToFile(Object ob, String filePath,
			Context context) {
		ObjectOutputStream objectOutputStream = null;
		try {
			/*
			 * always delete previous one and create a new one
			 */

			File f = new File(context.getFilesDir(), filePath);
			if (!f.delete()) {
				// Toast.makeText(context, "Failed to delete samples",
				// android.widget.Toast.LENGTH_SHORT).show();
			}

			if (ob != null) {
				FileOutputStream fileOutputStream = context.openFileOutput(
						privateDataFileName, Context.MODE_PRIVATE);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(ob);
				objectOutputStream.flush();
			}

		} catch (Exception e) {
			Toast.makeText(context, "Failed to save samples",
					android.widget.Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} finally {
			try {
				if (objectOutputStream != null) {
					objectOutputStream.close();
				}
			} catch (IOException e) {
				// TODO
				// Auto-generated
				// catch block
				e.printStackTrace();
			}
		}
	}

	public static Object ReadObjectFromFile(String filePath, Context context) {
		FileInputStream fileInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			File f = new File(context.getFilesDir(), filePath);
			if (!f.exists())
				return null;
			fileInputStream = context.openFileInput(filePath);
			objectInputStream = new ObjectInputStream(fileInputStream);
			Object loadData = objectInputStream.readObject();
			return loadData;
		} catch (Exception e) {
			Toast.makeText(context, "Failed to load data",
					android.widget.Toast.LENGTH_LONG).show();
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (objectInputStream != null) {
					objectInputStream.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	public static void DeleteFromFile(String filePath, Context context) {
		try {
			File f = new File(context.getFilesDir(), filePath);
			if (f.exists())
				f.delete();
		} catch (Exception e) {
			Toast.makeText(context, "Failed to delete data",
					android.widget.Toast.LENGTH_LONG).show();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static ArrayList<PositionDescriptor> LoadSamplingData(
			String filePath, Context context) {
		Object data = Helper.ReadObjectFromFile(filePath, context);
		if (data != null) {
			return (ArrayList<PositionDescriptor>) data;
		} else {
			return null;
		}
	}

	public static String ToLogString0(Collection<ScannedBleDevice> target) {
		String logString = "";
		for (ScannedBleDevice single : target) {
			logString += "		f" + single.toSimpleString() + "\r\n";
		}

		return logString;
	}

	public static String ToLogString0(Collection<ScannedBleDevice> target,
			boolean noPrefix) {
		String logString = "";
		for (ScannedBleDevice single : target) {
			if (noPrefix) {
				logString += "		" + single.toSimpleString() + "\r\n";
			}
		}

		return logString;
	}

	public static String ToLogString1(
			Collection<Tuple<Double, PositionDescriptor>> target) {
		String logString = "";
		for (Tuple<Double, PositionDescriptor> single : target) {
			logString += "Similarity: " + single.first + ", X-Y: ("
					+ single.second.getOriginalMesuredOnWidth() + ", "
					+ single.second.getOriginalMesuredOnHeight()
					+ ") based on build-in sample: " + "\r\n"
					+ ToLogString0(single.second.Fingerprints, true) + "\r\n";
		}

		return logString;
	}

	public enum ParkingPositionStatus {
		Busy, Idle, Ordered, Unknown
	}

	/*
	 * order or revert an owned position.
	 */
	public static String OrderOneParkingPosition(String userName,
			String targetBoardId, boolean revertOrder) {
		String orderFullUrl = "http://www.shaojun.xyz/smartparking/publicservice/orderPosition/?username="
				+ userName + "&toboard=" + targetBoardId;
		if (revertOrder)
			orderFullUrl += "&revert=true";
		String content = "";
		try {
			content = Helper
					.AccessWebUrlAndGetStringResponseContent(orderFullUrl);
			return content;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public static List<DrawImage> ConvertParkingPostionsFromWebToDrawImages(
			List<Tuple5<Float, Float, String, ParkingPositionStatus, Integer>> parkingPositionsFromWeb,
			Resources res) {
		List<DrawImage> drawImages = new ArrayList<DrawImage>();
		for (Tuple5<Float, Float, String, ParkingPositionStatus, Integer> oneParkingPosition : parkingPositionsFromWeb) {
			DrawImage t = new DrawImage(oneParkingPosition.first,
					oneParkingPosition.second, null, oneParkingPosition.third,
					oneParkingPosition.fifth);
			/* draw all parking position icons */
			if (oneParkingPosition.fourth == ParkingPositionStatus.Busy) {
				t.Bitmap = BitmapFactory.decodeResource(res,
						R.drawable.car_busy);
			} else if (oneParkingPosition.fourth == ParkingPositionStatus.Idle) {
				t.Bitmap = BitmapFactory.decodeResource(res,
						R.drawable.car_idle);
			} else if (oneParkingPosition.fourth == ParkingPositionStatus.Ordered) {
				t.Bitmap = BitmapFactory.decodeResource(res,
						R.drawable.car_ordered);
			} else if (oneParkingPosition.fourth == ParkingPositionStatus.Unknown) {
				t.Bitmap = BitmapFactory.decodeResource(res,
						R.drawable.car_unknown);
			}

			drawImages.add(t);
		}

		return drawImages;
	}

	// <X, Y, comments, busyOrIdleOrOrdered(0,1,2), parkingPositionId>,
	// buildingId is the unique id for
	// a whole parking
	// area, for
	// now, no use since we only for demo purpose.
	public static List<Tuple5<Float, Float, String, ParkingPositionStatus, Integer>> GetAllParkingPositionsFromWeb(
			int buildingId) {
		String getAllParkingPostionStatusFullUrl = "http://www.shaojun.xyz/smartparking/publicservice/GetParkingStates/";
		List<Tuple5<Float, Float, String, ParkingPositionStatus, Integer>> parkingPositionCoordinates = new ArrayList<Tuple5<Float, Float, String, ParkingPositionStatus, Integer>>();
		try {
			String content = Helper
					.AccessWebUrlAndGetStringResponseContent(getAllParkingPostionStatusFullUrl);
			// Log.e(LOG_TAG, "Got AllParkingPositionsFromWeb: " + content);
			if (content != null && !content.isEmpty()) {
				String[] positions = content.trim().split(";");
				// Log.e(LOG_TAG, "positions length: " + positions.length);
				for (String onePosition : positions) {
					String[] fields = onePosition.split(",");
					// Log.e(LOG_TAG, "fields length: " + fields.length
					// + " for onePosition: " + onePosition);
					int id = Integer.parseInt(fields[0]);
					ParkingPositionStatus status = null;
					if (fields[1].hashCode() == "Busy".hashCode()) {
						status = ParkingPositionStatus.Busy;
					} else if (fields[1].hashCode() == "Idle".hashCode()) {
						status = ParkingPositionStatus.Idle;
					} else if (fields[1].hashCode() == "Ordered".hashCode()) {
						status = ParkingPositionStatus.Ordered;
					} else {
						status = ParkingPositionStatus.Unknown;
					}

					float coor_X = Float.parseFloat(fields[2]);
					float coor_Y = Float.parseFloat(fields[3]);
					String comments = "";
					if (fields.length >= 5)
						comments = fields[4];
					parkingPositionCoordinates.add(Tuple5.create(coor_X,
							coor_Y, comments, status, id));
				}
			}
			/*
			 * parkingPositionCoordinates.add(Tuple5.create((float) 100, (float)
			 * 50, "pk0", ParkingPositionStatus.Busy, 0));
			 * parkingPositionCoordinates.add(Tuple5.create((float) 200, (float)
			 * 100, "pk1", ParkingPositionStatus.Idle, 1));
			 * parkingPositionCoordinates.add(Tuple5.create((float) 300, (float)
			 * 190, "pk2", ParkingPositionStatus.Ordered, 2));
			 * parkingPositionCoordinates.add(Tuple5.create((float) 400, (float)
			 * 330, "pk3", ParkingPositionStatus.Ordered, 3));
			 */

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return parkingPositionCoordinates;
	}

	// Access a web url by Get method, return the HttpResponse.
	public static HttpResponse GetHttpResponseByUrl(
			String fullUrlWithAllParameters) throws ClientProtocolException,
			IOException {
		try {
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is
			// established.
			// The default value is zero, that means the timeout is not used.
			int timeoutConnection = 8000;
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 5000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			HttpGet httpGet = new HttpGet(fullUrlWithAllParameters);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			return httpResponse;
			// }
			// catch (UnsupportedEncodingException e) {
			// e.printStackTrace();
			// } catch (ClientProtocolException e) {
			// e.printStackTrace();
			// } catch (IOException e) {
			// e.printStackTrace();
			// } catch (Exception e) {

		} finally {

		}

	}

	// Access a web url by Get method, return true if http response code is 200
	public static Boolean AccessWebUrl(String fullUrlWithAllParameters)
			throws ClientProtocolException, IOException {
		int responseCode = GetHttpResponseByUrl(fullUrlWithAllParameters)
				.getStatusLine().getStatusCode();
		if (responseCode == 200)
			return true;
		return false;
	}

	// Access a web url by Get method, return the response content as a string.
	public static String AccessWebUrlAndGetStringResponseContent(
			String fullUrlWithAllParameters) throws IllegalStateException,
			IOException {
		BufferedReader in = null;
		String fullText = "";
		try {
			in = new BufferedReader(new InputStreamReader(GetHttpResponseByUrl(
					fullUrlWithAllParameters).getEntity().getContent()));
			String oneLine;
			while ((oneLine = in.readLine()) != null)
				fullText += oneLine;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		return fullText;
	}

	// get a bitmap via a web url.
	public static Bitmap GetImageBitmapFromUrl(String url) throws IOException {
		Bitmap bm = null;
		URL aURL = new URL(url);
		URLConnection conn = aURL.openConnection();
		conn.connect();
		InputStream is = conn.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		bm = BitmapFactory.decodeStream(bis);
		bis.close();
		is.close();
		return bm;
	}

	// get url list which parsed from listingPageUrl
	public static String[] GetWebImageFullUrlsFromListingPageUrl(
			String listingPageUrl) throws IOException {
		String fullContent = AccessWebUrlAndGetStringResponseContent(listingPageUrl);
		if (fullContent != null && !fullContent.isEmpty()) {
			return fullContent.trim().split("</br>");
		}

		return null;
	}

	/**
	 * Caculate the draw on bitmap circle radius based on passed in map scale,
	 * the scale is for: 100 px in bitmap equal how many real life meters, like
	 * say the value is 5, means 100px in bitmap equal 5meters.
	 * 
	 * @return half of a meter as default implementation.
	 */
	public static int GetCircleRadiusByMapScale(float mapScale) {
		// return half of a meter.
		return Math.round(100 / mapScale / 2);
	}
}
