package com.SmartParking.WebServiceEntity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Think on 9/18/2015.
 */
public class Building implements Serializable {
    public Integer Id;
    public String MapUrl;
    public double Latitude;
    public double Longitude;
    public String Description;
    public String CreationTime;
    public ArrayList<Board> Boards;
    public String DetailUrl;
}
