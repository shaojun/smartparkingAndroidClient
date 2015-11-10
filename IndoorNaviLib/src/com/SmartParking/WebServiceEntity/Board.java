package com.SmartParking.WebServiceEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Think on 9/19/2015.
 */
public class Board extends EntityBase implements Serializable {
    public String OwnedByBuildingUrl;
    public Boolean IsCovered;
    public Integer CoordinateX;
    public Integer CoordinateY;
    public String Description;
    public String DetailUrl;
    public String BoardIdentity;
    public Order OrderDetail;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        Board b = new Board();
        b.OwnedByBuildingUrl = jsonObject.getString("ownerBuilding");
        b.IsCovered = jsonObject.getBoolean("isCovered");
        b.CoordinateX = jsonObject.getInt("coordinateX");
        b.CoordinateY = jsonObject.getInt("coordinateY");
        b.Description = jsonObject.getString("description");
        b.DetailUrl = jsonObject.getString("url");
        return b;
    }


    @Override
    public String toJson() {
        return null;
    }
}
