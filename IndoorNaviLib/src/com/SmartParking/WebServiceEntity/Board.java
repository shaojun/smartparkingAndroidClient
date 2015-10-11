package com.SmartParking.WebServiceEntity;

import java.io.Serializable;

/**
 * Created by Think on 9/19/2015.
 */
public class Board implements Serializable {
    public String OwnedByBuildingUrl;
    public Boolean IsCovered;
    public Integer CoordinateX;
    public Integer CoordinateY;
    public String Description;
    public String DetailUrl;
}
