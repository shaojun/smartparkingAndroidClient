package com.SmartParking.WebServiceEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Think on 10/11/2015.
 */
public class Sample extends EntityBase implements Serializable {
    public String DetailUrl;
    public String OwnedByBuildingUrl;
    public Integer CoordinateX;
    public Integer CoordinateY;
    public String CreationTime;
    public ArrayList<SampleDescriptor> SampleDescriptors = new ArrayList<>();
    public String Description;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        Sample __sample = new Sample();
        __sample.OwnedByBuildingUrl = jsonObject.getString("ownerBuilding");
        __sample.CoordinateX = Integer.parseInt(jsonObject.getString("coordinateX"));
        __sample.CoordinateY = Integer.parseInt(jsonObject.getString("coordinateY"));
        __sample.CreationTime = jsonObject.getString("creation_Time");
        __sample.Description = jsonObject.getString("description");
        __sample.DetailUrl = jsonObject.getString("url");
        JSONArray sampleDescriptorJSONArray = jsonObject.getJSONArray("sampleDescriptors");
        for (int j = 0; j < sampleDescriptorJSONArray.length(); j++) {
            String sampleDescriptionUrl = (String) sampleDescriptorJSONArray.get(j);
            SampleDescriptor sd = new SampleDescriptor();
            sd.DetailUrl = sampleDescriptionUrl;
            __sample.SampleDescriptors.add(sd);
        }

        return __sample;
    }

    @Override
    public JSONObject toJsonObject() {
        try {
            JSONObject jso = new JSONObject();
            jso.put("ownerBuilding", this.OwnedByBuildingUrl);
            jso.put("coordinateX", this.CoordinateX);
            jso.put("coordinateY", this.CoordinateY);
            jso.put("description", this.Description);
            return jso;
        } catch (Exception ex) {
            super.setErrorMessage(ex.toString());
        }

        return null;
    }
}
