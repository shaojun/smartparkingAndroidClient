package com.SmartParking.WebServiceEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Think on 9/9/2015.
 */
public class Order extends EntityBase implements Serializable {
    public String Url;
    public String ToBoardUrl;
    public String CreationTime;
    public String Status;
    public String OwnerUrl;
    public boolean IsActive;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        Order order = new Order();
        order.Url = jsonObject.getString("url");
        order.ToBoardUrl = jsonObject.getString("to_Board");
        order.CreationTime = jsonObject.getString("creation_Time");
        order.Status = jsonObject.getString("status");
        order.OwnerUrl = jsonObject.getString("owner");
        order.IsActive = jsonObject.getBoolean("isActive");
        return order;
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject jso = new JSONObject();
        try {
            jso.put("owner", this.OwnerUrl);
            jso.put("status", this.Status);
            jso.put("to_Board", this.ToBoardUrl);
            jso.put("isActive", this.IsActive);
        } catch (Exception ex) {
            super.setErrorMessage(ex.toString());
            return null;
        }

        return jso;
    }
}
