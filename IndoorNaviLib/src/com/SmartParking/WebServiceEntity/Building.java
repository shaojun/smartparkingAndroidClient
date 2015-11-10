package com.SmartParking.WebServiceEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Think on 9/18/2015.
 */
public class Building extends EntityBase implements Serializable {
    public Integer Id;
    public String MapUrl;
    public double Latitude;
    public double Longitude;
    public String Description;
    public String CreationTime;
    public ArrayList<Board> Boards;
    public String DetailUrl;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        Building __building = new Building();
        __building.Id = Integer.parseInt(jsonObject.getString("id"));
        __building.MapUrl = jsonObject.getString("mapUrl");
        __building.Latitude = Double.parseDouble(jsonObject.getString("latitude"));
        __building.Longitude = Double.parseDouble(jsonObject.getString("longitude"));
        __building.Description = jsonObject.getString("description");
        __building.CreationTime = jsonObject.getString("creationTime");
        __building.DetailUrl = jsonObject.getString("url");
        return __building;
    }

    @Override
    public String toJson() {
        return null;
    }
}
