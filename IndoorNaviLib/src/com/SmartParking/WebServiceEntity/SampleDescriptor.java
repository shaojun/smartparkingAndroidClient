package com.SmartParking.WebServiceEntity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Think on 10/11/2015.
 */
public class SampleDescriptor extends EntityBase implements Serializable {
    public String DetailUrl;
    public String OwnedSampleUrl;
    public String UUID;
    public String MajorId;
    public String MinorId;
    public String MacAddress;
    public Integer Tx;
    public Integer Rssi;
    public Float Distance = 0F;
    public String CreationTime;

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        SampleDescriptor __sampleDescriptor = new SampleDescriptor();
        __sampleDescriptor.OwnedSampleUrl = jsonObject.getString("ownerSample");
        __sampleDescriptor.UUID = jsonObject.getString("uuid");
        __sampleDescriptor.MajorId = jsonObject.getString("major_Id");
        __sampleDescriptor.MinorId = jsonObject.getString("minor_Id");
        __sampleDescriptor.MacAddress = jsonObject.getString("mac_address");
        __sampleDescriptor.Tx = Integer.parseInt(jsonObject.getString("tx_value"));
        __sampleDescriptor.Rssi = Integer.parseInt(jsonObject.getString("rssi_value"));
        __sampleDescriptor.Distance = Float.parseFloat(jsonObject.getString("caculated_distance"));
        __sampleDescriptor.CreationTime = jsonObject.getString("creation_Time");
        return __sampleDescriptor;
    }

    @Override
    public JSONObject toJsonObject() {
        try {
            JSONObject jso = new JSONObject();
            jso.put("ownerSample", this.OwnedSampleUrl);
            jso.put("uuid", this.UUID);
            jso.put("major_Id", this.MajorId);
            jso.put("minor_Id", this.MinorId);
            jso.put("mac_address", this.MacAddress.replace(":", ""));
            jso.put("rssi_value", this.Rssi);
            jso.put("caculated_distance", this.Distance.toString());
            return jso;
        } catch (Exception ex) {
            super.setErrorMessage(ex.toString());
        }

        return null;
    }
}
