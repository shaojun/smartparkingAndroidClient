package com.SmartParking.Task;

import android.util.Base64;
import android.util.Log;

import com.SmartParking.Task.Action;
import com.SmartParking.Task.Task;
import com.SmartParking.WebService.RestClient;
import com.SmartParking.WebService.Service;
import com.SmartParking.WebServiceEntity.EntityBase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public class RestAction extends Action<String> {
    private static String LOG_TAG = "RestAction";
    private RestClient restClient;
    private String password;
    private String userName;
    private String httpAction;
    private Object state;

    /**
     * @param serviceSubUrl will combine with Service.ServiceUrl depends on it's a full or sub url, expected value is like "users/", "buildings/" or "http://rest.shaojun.xyz:8090/buildings/1/"
     * @param userName      userName and pwd are for authenticate accessed Web rest API.
     * @param pwd           combined with userName to authenticate Web API.
     * @param httpAction    "GET", "POST"
     */
    public RestAction(String serviceSubUrl, String userName, String pwd, String httpAction, Object state) {
        super(state);
        if (serviceSubUrl.contains(Service.ServiceUrl)) {
            // input is a full url.
            this.restClient = new RestClient(serviceSubUrl);
        } else {
            this.restClient = new RestClient(Service.ServiceUrl + serviceSubUrl);
        }

        Log.v(LOG_TAG, "Starting RestAction " + httpAction + " to " + serviceSubUrl);
        this.userName = userName;
        this.password = pwd;
        this.httpAction = httpAction;
        this.state = state;
        this.restClient.AddHeader("content-type", "application/json");
        String auth = new String(Base64.encode((this.userName + ":" + this.password).getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));
        this.restClient.AddHeader("Authorization", "Basic " + auth);
    }

    public void AddParam(String name, String value) {
        this.restClient.AddParam(name, value);
    }

    public void AddPostJsonObject(JSONObject jsonObject) {
        this.restClient.AddJsonObject(jsonObject);
    }

    @Override
    public String execute(Task ownerTask) throws Exception {
        this.restClient.Execute(this.httpAction);
        return this.restClient.getResponse();
    }
}
