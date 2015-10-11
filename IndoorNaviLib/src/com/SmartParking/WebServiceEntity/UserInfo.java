package com.SmartParking.WebServiceEntity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Think on 9/9/2015.
 */
public class UserInfo implements Serializable {
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
    public UserInfo()
    {
        this.Orders = new ArrayList<>();
        this.Groups = new ArrayList<>();
    }


}
