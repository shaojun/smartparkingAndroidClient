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
    public String OrderDetailUrl;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        Board b = new Board();
        b.OwnedByBuildingUrl = jsonObject.getString("ownerBuilding");
        b.IsCovered = jsonObject.getBoolean("isCovered");
        b.CoordinateX = jsonObject.getInt("coordinateX");
        b.CoordinateY = jsonObject.getInt("coordinateY");
        b.Description = jsonObject.getString("description");
        b.DetailUrl = jsonObject.getString("url");
        b.BoardIdentity = jsonObject.getString("boardIdentity");
        JSONArray orderDetailUrls = jsonObject.getJSONArray("orderDetail");
        if (orderDetailUrls != null && orderDetailUrls.length() >= 1) {
            b.OrderDetailUrl = orderDetailUrls.getString(0);
        }

        return b;
    }


    @Override
    public JSONObject toJsonObject() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Board))
            return false;

        Board target = (Board) obj;
        if (target.BoardIdentity.equals(this.BoardIdentity))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return this.BoardIdentity.hashCode();
    }

}
