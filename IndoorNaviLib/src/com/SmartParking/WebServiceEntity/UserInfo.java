package com.SmartParking.WebServiceEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 9/9/2015.
 */
public class UserInfo extends EntityBase implements Serializable {
    public static UserInfo CurrentUserInfo;
    public String UserName;
    public Boolean IsActive;
    public ArrayList<Order> Orders;
    public String UUID;
    public String MajorId;
    public String MinorId;
    public String MacAddress;
    public String CreationTime;
    public ArrayList<String> Groups;

    public UserInfo() {
        this.Orders = new ArrayList<>();
        this.Groups = new ArrayList<>();
    }

    @Override
    public EntityBase loadSingleFromJson(JSONObject jsonObject) throws JSONException {
        UserInfo userInfo = new UserInfo();
        userInfo.UserName = jsonObject.getString("user_name");
        userInfo.UUID = jsonObject.getString("uuid");
        userInfo.MajorId = jsonObject.getString("major_Id");
        userInfo.MinorId = jsonObject.getString("minor_Id");
        userInfo.MacAddress = jsonObject.getString("mac_address");
        userInfo.CreationTime = jsonObject.getString("creation_Time");
        userInfo.IsActive = jsonObject.getBoolean("is_active");
        JSONArray userInfoGroupsJSONArray = jsonObject.getJSONArray("groups");
        for (int i = 0; i < userInfoGroupsJSONArray.length(); i++) {
            userInfo.Groups.add(userInfoGroupsJSONArray.getString(i));
        }

        return userInfo;
    }

    @Override
    public String toJson() {
        return null;
    }
}
