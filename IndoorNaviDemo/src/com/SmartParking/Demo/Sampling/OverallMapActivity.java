package com.SmartParking.Demo.Sampling;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.WebService.AsyncRestTask;
import com.SmartParking.WebServiceEntity.Board;
import com.SmartParking.WebServiceEntity.Building;
import com.SmartParking.WebService.OnAsyncRestTaskFinishedListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;


//import com.SmartParking.Demo.Mapping.R;

public class OverallMapActivity extends Activity implements LocationSource,
        AMapLocationListener, OnGeocodeSearchListener, AMap.OnMarkerClickListener,
        AMap.OnInfoWindowClickListener, AMap.OnMapClickListener {
    private MapView mapView;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private LocationManagerProxy mAMapLocationManager;
    private static final String LOG_TAG = "OverallMap";
    public ArrayList<Building> Buildings = new ArrayList<>();

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(Location aLocation) {
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation aLocation) {
        if (mListener != null && aLocation != null) {
            mListener.onLocationChanged(aLocation);// 显示系统小蓝点
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mAMapLocationManager == null) {
            mAMapLocationManager = LocationManagerProxy.getInstance(this);
            /*
             * mAMapLocManager.setGpsEnable(false);
			 * 1.0.2版本新增方法，设置true表示混合定位中包含gps定位，false表示纯网络定位，默认是true Location
			 * API定位采用GPS和网络混合定位方式
			 * ，第一个参数是定位provider，第二个参数时间最短是2000毫秒，第三个参数距离间隔单位是米，第四个参数是定位监听者
			 */
            mAMapLocationManager.requestLocationData(
                    LocationProviderProxy.AMapNetwork, 2000, 10, this);

            aMap.setOnMarkerClickListener(this);// 设置点击marker事件监听器
            aMap.setOnInfoWindowClickListener(this);// 设置点击infoWindow事件监听器
            //aMap.setInfoWindowAdapter(this);// 设置自定义InfoWindow样式
//            this.addMarkersToMap();
            // LongYang Road,latitude: 31.203506, longitude: 121.510544
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mAMapLocationManager != null) {
            mAMapLocationManager.removeUpdates(this);
            mAMapLocationManager.destroy();
        }

        mAMapLocationManager = null;
    }

    /**
     * 响应地理编码
     */
    public void getLatlon(final String name) {
        // showDialog();
        GeocodeQuery query = new GeocodeQuery(name, "010");// 第一个参数表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode，
        geocoderSearch.getFromLocationNameAsyn(query);// 设置同步地理编码请求
    }

    /**
     * 响应逆地理编码
     */
    public void getAddress(final LatLonPoint latLonPoint) {
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200,
                GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    /**
     * 地理编码查询回调
     */
    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        if (rCode == 0) {
            if (result != null && result.getGeocodeAddressList() != null
                    && result.getGeocodeAddressList().size() > 0) {
                GeocodeAddress address = result.getGeocodeAddressList().get(0);
                double latitude = address.getLatLonPoint().getLatitude();
                double longitude = address.getLatLonPoint().getLongitude();
                Log.e(LOG_TAG, "onGeocodeSearched result, latitude: " + Double.toString(latitude) + ", longitude: " + Double.toString(longitude));
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(latitude, longitude), 15));
//                //设置图片的显示区域。
//                LatLngBounds bounds = new LatLngBounds.Builder()
//                        .include(new LatLng(latitude - 0.005 / 9.36,
//                                longitude - 0.005 / 7.8))
//                        .include(new LatLng(latitude + 0.005 / 9.36,
//                                longitude + 0.005 / 7.8)).build();
//                GroundOverlay groundoverlay = aMap.addGroundOverlay(new GroundOverlayOptions()
//                        .anchor(0.5f, 0.5f).transparency(0.2f)
//                        .image(BitmapDescriptorFactory.fromResource(R.drawable.car_busy))
//                        .positionFromBounds(bounds));
            }

        } else if (rCode == 27) {
            //ToastUtil.show(GeocoderActivity.this, R.string.error_network);
        } else if (rCode == 32) {
            //ToastUtil.show(GeocoderActivity.this, R.string.error_key);
        } else {
            //ToastUtil.show(GeocoderActivity.this,
            //getString(R.string.error_other) + rCode);
        }
    }

    /**
     * 逆地理编码回调
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
    }

    private GeocodeSearch geocoderSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overall_map);
        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 必须要写
        aMap = mapView.getMap();
        aMap.setOnMapClickListener(this);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        final GeocodeSearch geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);
        Button buttonGoSearch = (Button) findViewById(R.id.buttonGoSearch);
        final EditText editTextTargetPlace = (EditText) findViewById(R.id.editTextTargetPlace);

        SharedPreferences logOnSharedPreferences = this.getSharedPreferences("LogOn", 0);
        final String userName = logOnSharedPreferences.getString("UserName", null);
        final String password = logOnSharedPreferences.getString("Password", null);
        Thread pollingBuilding = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    AsyncRestTask.Create("buildings/", userName, password, "GET", new OnAsyncRestTaskFinishedListener() {
                        @Override
                        public void OnError(String errorMsg) {
                            new AlertDialog.Builder(
                                    OverallMapActivity.this)
                                    .setIcon(
                                            android.R.drawable.ic_dialog_alert)
                                    .setTitle("Get Buildings failed")
                                    .setMessage(errorMsg)
                                    .setPositiveButton("Failed", null)
                                    .show();
                        }

                        @Override
                        public void OnFinished(Object json) {
                            if (json instanceof JSONObject) {
                                JSONObject node = (JSONObject) json;
                                new AlertDialog.Builder(
                                        OverallMapActivity.this)
                                        .setIcon(
                                                android.R.drawable.ic_dialog_alert)
                                        .setTitle("Get Buildings failed")
                                        .setMessage("Single building returned")
                                        .setPositiveButton("Failed", null)
                                        .show();
                            } else if (json instanceof JSONArray) {
                                OverallMapActivity.this.Buildings.clear();
                                JSONArray buildingsJSArray = (JSONArray) json;
                                for (int i = 0; i < buildingsJSArray.length(); i++) {
                                    Building __building = new Building();
                                    try {
                                        JSONObject buildingJSObject = (JSONObject) (buildingsJSArray.get(i));
                                        __building.MapUrl = buildingJSObject.getString("mapUrl");
                                        __building.Latitude = Double.parseDouble(buildingJSObject.getString("latitude"));
                                        __building.Longitude = Double.parseDouble(buildingJSObject.getString("longitude"));
                                        __building.Description = buildingJSObject.getString("description");
                                        __building.CreationTime = buildingJSObject.getString("creationTime");
                                        __building.DetailUrl = buildingJSObject.getString("url");
                                    } catch (JSONException e) {
                                        new AlertDialog.Builder(
                                                OverallMapActivity.this)
                                                .setIcon(
                                                        android.R.drawable.ic_dialog_alert)
                                                .setTitle("Resolve Buildings failed")
                                                .setMessage("!!!")
                                                .setPositiveButton("Failed", null)
                                                .show();
                                    }

                                    OverallMapActivity.this.Buildings.add(__building);
                                }

                                OverallMapActivity.this.addAndRefreshMarkersToMap();

                                AsyncRestTask.Create("boards/", userName, password, "GET", new OnAsyncRestTaskFinishedListener() {
                                    @Override
                                    public void OnError(String errorMsg) {
                                        new AlertDialog.Builder(
                                                OverallMapActivity.this)
                                                .setIcon(
                                                        android.R.drawable.ic_dialog_alert)
                                                .setTitle("Get boards failed")
                                                .setMessage(errorMsg)
                                                .setPositiveButton("Failed", null)
                                                .show();
                                    }

                                    @Override
                                    public void OnFinished(Object json) {
                                        if (json instanceof JSONObject) {
                                            JSONObject node = (JSONObject) json;
                                            new AlertDialog.Builder(
                                                    OverallMapActivity.this)
                                                    .setIcon(
                                                            android.R.drawable.ic_dialog_alert)
                                                    .setTitle("Get Boards failed")
                                                    .setMessage("Single board returned")
                                                    .setPositiveButton("Failed", null)
                                                    .show();

                                        } else if (json instanceof JSONArray) {
                                            JSONArray boardsJSArray = (JSONArray) json;
                                            for (int i = 0; i < boardsJSArray.length(); i++) {
                                                Board b = new Board();
                                                try {
                                                    JSONObject boardJSObject = (JSONObject) (boardsJSArray.get(i));
                                                    b.OwnedByBuildingUrl = boardJSObject.getString("ownerBuilding");
                                                    b.IsCovered = boardJSObject.getBoolean("isCovered");
                                                    b.CoordinateX = boardJSObject.getInt("coordinateX");
                                                    b.CoordinateY = boardJSObject.getInt("coordinateY");
                                                    b.Description = boardJSObject.getString("description");
                                                    b.DetailUrl = boardJSObject.getString("url");
                                                    for (Building oneBuilding : OverallMapActivity.this.Buildings) {
                                                        if (oneBuilding.DetailUrl.equals(b.OwnedByBuildingUrl)) {
                                                            if (oneBuilding.Boards == null)
                                                                oneBuilding.Boards = new ArrayList<>();
                                                            oneBuilding.Boards.add(b);
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    new AlertDialog.Builder(
                                                            OverallMapActivity.this)
                                                            .setIcon(
                                                                    android.R.drawable.ic_dialog_alert)
                                                            .setTitle("Resolve Boards failed")
                                                            .setMessage("!!!")
                                                            .setPositiveButton("Failed", null)
                                                            .show();
                                                }

                                                OverallMapActivity.this.addAndRefreshMarkersToMap();
                                            }
                                        }
                                    }
                                }).Start();
                            }
                        }
                    }).Start();
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        pollingBuilding.start();

        buttonGoSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchStr = editTextTargetPlace.getText().toString();
                if (searchStr != null && searchStr != "") {
                    GeocodeQuery query = new GeocodeQuery(searchStr, "021");
                    geocoderSearch.getFromLocationNameAsyn(query);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_overall_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private MarkerOptions markerOption;

    private void addAndRefreshMarkersToMap() {
        this.aMap.clear();
        this.markerAndBuildingRelationship.clear();
//        List<LatLng> markersFromWeb = new ArrayList<>();
//        markersFromWeb.add(new LatLng(31.203506, 121.510544));
//        markersFromWeb.add(new LatLng(31.195986, 121.55941));
//        markersFromWeb.add(new LatLng(30.820332, 121.51949));
//        markersFromWeb.add(new LatLng(31.189659, 121.500504));
        // LongYang Road,latitude: 31.203506, longitude: 121.510544
        // fanghua road, latitude: 31.195986, longitude: 121.55941
        // ShiJiGongYuan,latitude: 30.820332, longitude: 121.51949
        // ShiBoYuan 4 hao lou,latitude: 31.189659, longitude: 121.500504
        //文字显示标注，可以设置显示内容，位置，字体大小颜色，背景色旋转角度,Z值等
//        TextOptions textOptions = new TextOptions().position(new LatLng(31.203506, 121.520544))
//                .text("Parkingfirst support！").fontColor(Color.BLACK)
//                .backgroundColor(Color.WHITE).fontSize(80).rotate(0).align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
//                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
//        aMap.addText(textOptions);
        for (Building building : this.Buildings) {
            LatLng lat = new LatLng(building.Latitude, building.Longitude);
            MarkerOptions markerOption = new MarkerOptions();
            markerOption.position(lat);
            if (building.Boards != null) {
                Integer totalPosition = 0;
                Integer freePosition = 0;
                for (Board b : building.Boards) {
                    totalPosition++;
                    if (!b.IsCovered) {
                        freePosition++;
                    }
                }

                markerOption.title(building.Description).snippet("空闲数/总数：" + freePosition.toString() + "/" + totalPosition.toString());
            } else {
                markerOption.title(building.Description).snippet("空闲数/总数：~/~");
            }
            markerOption.draggable(false);
            markerOption.icon(BitmapDescriptorFactory
                    .fromResource(R.drawable.parking_area));
            this.aMap.addMarker(markerOption);
            this.markerAndBuildingRelationship.put(markerOption, building);
        }
    }

    private Hashtable<MarkerOptions, Building> markerAndBuildingRelationship
            = new Hashtable<>();

    @Override
    public boolean onMarkerClick(Marker marker) {
//        LatLng markerLatLng = marker.getPosition();
//        LatLng hintTextLatLng = new LatLng(markerLatLng.latitude * 0.9999, markerLatLng.longitude * 1.001);
//        TextOptions textOptions = new TextOptions().position(hintTextLatLng)
//                .text("点击说明部分开始预订！").fontColor(Color.RED)
//                .backgroundColor(Color.TRANSPARENT).fontSize(30).rotate(0).align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
//                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
//        aMap.addText(textOptions);
        //jumpPoint(marker);
//        Toast.makeText(
//                getBaseContext(),
//                "You clicked a marker",
//                android.widget.Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final Building selectedBuilding = this.markerAndBuildingRelationship.get(marker);
        if (selectedBuilding == null) return;
        if (LogOnActivity.userInfo.Groups.contains("Technicians")
                || LogOnActivity.userInfo.Groups.contains("SuperUsers")) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("进入后台模式?")
                    .setMessage(
                            "进入 '" + marker.getTitle() + "' 的后台模式进行操作吗？")
                    .setPositiveButton("进入后台",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Intent i = new Intent(OverallMapActivity.this, MainActivity.class);
                                    i.putExtra("BuildingDetailUrl", selectedBuilding.DetailUrl);
                                    startActivity(i);
                                }
                            })
                    .setNegativeButton("取消", null)
                    .setNeutralButton("进入普通模式",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Intent i = new Intent(OverallMapActivity.this, NaviActivity.class);
                                    i.putExtra("BuildingDetailUrl", selectedBuilding.DetailUrl);
                                    startActivity(i);
                                }
                            }).show();
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("查看详细?")
                    .setMessage(
                            "进入并预订 '" + marker.getTitle() + "' 的车位吗？")
                    .setPositiveButton("是",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Intent i = new Intent(OverallMapActivity.this, NaviActivity.class);
                                    i.putExtra("BuildingDetailUrl", selectedBuilding.DetailUrl);
                                    startActivity(i);
                                }
                            })
                    .setNegativeButton("取消", null).show();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        this.addAndRefreshMarkersToMap();
    }
}