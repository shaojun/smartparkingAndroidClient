package com.SmartParking.WebService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Think on 9/18/2015.
 */
public interface OnAsyncRestTaskFinishedListener {
    void OnFinished(Object jsonObjectOrArrayResult);
    void OnError(String errorMsg);
}
