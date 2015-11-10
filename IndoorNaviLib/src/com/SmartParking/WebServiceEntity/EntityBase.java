package com.SmartParking.WebServiceEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 11/8/2015.
 */
public abstract class EntityBase {
    public EntityBase() {
    }

    private String errorMessage = null;

    public abstract EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException;

    public List<EntityBase> loadMultipleFromJson(JSONArray jsonArray) throws JSONException {
        List<EntityBase> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject singleJSONObject = (JSONObject) (jsonArray.get(i));
            list.add(this.loadSingleFromJson(singleJSONObject));
        }

        return list;
    }

    public abstract String toJson();

    public void setErrorMessage(String errorMsg) {
        this.errorMessage = errorMsg;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
