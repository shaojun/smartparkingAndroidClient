package com.amapv2.apis.util;

import com.amap.api.maps.model.LatLng;

public class Constants {

	public static final int ERROR = 1001;// 网络异常
	public static final int ROUTE_START_SEARCH = 2000;
	public static final int ROUTE_END_SEARCH = 2001;
	public static final int ROUTE_BUS_RESULT = 2002;// 路径规划中公交模�?
	public static final int ROUTE_DRIVING_RESULT = 2003;// 路径规划中驾车模�?
	public static final int ROUTE_WALK_RESULT = 2004;// 路径规划中步行模�?
	public static final int ROUTE_NO_RESULT = 2005;// 路径规划没有�?�索到结果

	public static final int GEOCODER_RESULT = 3000;// 地�?�编�?或者逆地�?�编�?�?功
	public static final int GEOCODER_NO_RESULT = 3001;// 地�?�编�?或者逆地�?�编�?没有数�?�

	public static final int POISEARCH = 4000;// poi�?�索到结果
	public static final int POISEARCH_NO_RESULT = 4001;// poi没有�?�索到结果
	public static final int POISEARCH_NEXT = 5000;// poi�?�索下一页

	public static final int BUSLINE_LINE_RESULT = 6001;// 公交线路查询
	public static final int BUSLINE_id_RESULT = 6002;// 公交id查询
	public static final int BUSLINE_NO_RESULT = 6003;// 异常情况

	public static final LatLng BEIJING = new LatLng(39.90403, 116.407525);// 北京市�?纬度
	public static final LatLng ZHONGGUANCUN = new LatLng(39.983456, 116.3154950);// 北京市中关�?��?纬度
	public static final LatLng SHANGHAI = new LatLng(31.238068, 121.501654);// 上海市�?纬度
	public static final LatLng FANGHENG = new LatLng(39.989614, 116.481763);// 方�?�国际中心�?纬度
	public static final LatLng CHENGDU = new LatLng(30.679879, 104.064855);// �?都市�?纬度
	public static final LatLng XIAN = new LatLng(34.341568, 108.940174);// 西安市�?纬度
	public static final LatLng ZHENGZHOU = new LatLng(34.7466, 113.625367);// 郑州市�?纬度
}
