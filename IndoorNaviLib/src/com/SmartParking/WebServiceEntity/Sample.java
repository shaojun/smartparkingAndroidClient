package com.SmartParking.WebServiceEntity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Think on 10/11/2015.
 */
public class Sample implements Serializable {
    public String OwnedByBuildingUrl;
    public Integer CoordinateX;
    public Integer CoordinateY;
    public String CreationTime;
    public ArrayList<SampleDescriptor> SampleDescriptors = new ArrayList<>();
    public String Description;
}
