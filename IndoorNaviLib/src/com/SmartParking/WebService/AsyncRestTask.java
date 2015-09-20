package com.SmartParking.WebService;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;

/**
 * Created by Think on 9/18/2015.
 */
public class AsyncRestTask {
    private static String LOG_TAG = "AsyncRestTask";
    private ArrayList<OnAsyncRestTaskFinishedListener> listeners = new ArrayList<>();
    private RestClient restClient;
    private String userName;
    private String password;
    private String action;

    private AsyncRestTask() {
    }

    /**
     * @param serviceSubUrl will combine with Service.ServiceUrl, expected value is like "users/", "buildings/" and etc...
     * @param userName      userName and pwd are for authenticate accessed Web rest API.
     * @param action        "GET", "POST"
     */
    public static AsyncRestTask Create(String serviceSubUrl, String userName, String pwd, String action, OnAsyncRestTaskFinishedListener listener) {
        return new AsyncRestTask(serviceSubUrl, userName, pwd, action, listener);
    }

    private AsyncRestTask(String serviceSubUrl, String userName, String pwd, String action, OnAsyncRestTaskFinishedListener listener) {
        this.restClient = new RestClient(Service.ServiceUrl + serviceSubUrl);
        this.userName = userName;
        this.password = pwd;
        this.action = action;
        if (listener != null) {
            this.AddOnAsyncRestTaskFinishedListener(listener);
        }
    }

    public void AddOnAsyncRestTaskFinishedListener(OnAsyncRestTaskFinishedListener listener) {
        listeners.add(listener);
    }

    public void RemoveOnAsyncRestTaskFinishedListener(OnAsyncRestTaskFinishedListener listener) {
        listeners.remove(listener);
    }

    public void RemoveAllOnAsyncRestTaskFinishedListener(OnAsyncRestTaskFinishedListener listener) {
        listeners.clear();
    }

    public void Start() {
        innerTask i = new innerTask();
        i.execute("");
    }

    private class innerTask extends AsyncTask<String, Integer, String> {
        private String errorStr = "";

        protected String doInBackground(String... params) {
            restClient.AddHeader("content-type", "application/json");
            String auth = new String(Base64.encode((AsyncRestTask.this.userName + ":" + AsyncRestTask.this.password).getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));
            restClient.AddHeader("Authorization", "Basic " + auth);
            try {
                Log.e(LOG_TAG, "AsyncRestTask start against " + restClient.getRequestedUrl() + " with action: " + AsyncRestTask.this.action);
                restClient.Execute(AsyncRestTask.this.action);
                String responseString = restClient.getResponse();
                Log.e(LOG_TAG, "AsyncRestTask finished with response: " + responseString);
                return responseString;
            } catch (Exception e) {
                Log.e(LOG_TAG, "AsyncRestTask finished with exception: " + e.getMessage());
                this.errorStr = e.getMessage();
                return this.errorStr;
            }
        }

        protected void onPostExecute(String result) {
            if (this.errorStr != null && this.errorStr != "") {
                for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                    l.OnError(result);
                }

                this.errorStr = "";
                return;
            } else {
                Object json = null;
                try {
                    json = new JSONTokener(result).nextValue();
                    // parsing passed does not mean finished with success, need further checking the inner content.
                } catch (JSONException e) {
                    for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                        l.OnError("Parsing Json String to JSONObject(or JSONArray) failed");
                    }

                    this.errorStr = "";
                    return;
                }

                if (json instanceof JSONObject) {
                    JSONObject node = (JSONObject) json;
                    if (node.has("detail")) {
                        String errorMsgInJson = null;
                        try {
                            errorMsgInJson = node.getString("detail");
                            for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                                l.OnError(errorMsgInJson);
                            }

                            this.errorStr = "";
                            return;
                        } catch (JSONException e) {
                            for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                                l.OnError("Read error msg in Json String failed");
                            }

                            this.errorStr = "";
                            return;
                        }
                    } else {
                        for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                            l.OnFinished(node);
                        }
                    }
                } else if (json instanceof JSONArray) {
                    JSONArray jsArray = (JSONArray) json;
                    for (OnAsyncRestTaskFinishedListener l : AsyncRestTask.this.listeners) {
                        l.OnFinished(jsArray);
                    }
                }
            }
        }
    }
}

