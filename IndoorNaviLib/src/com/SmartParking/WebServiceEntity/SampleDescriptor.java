package com.SmartParking.WebServiceEntity;

import java.io.Serializable;

/**
 * Created by Think on 10/11/2015.
 */
public class SampleDescriptor implements Serializable {
    public String OwnedSampleUrl;
    public String UUID;
    public String MajorId;
    public String MinorId;
    public String MacAddress;
    public Integer Tx;
    public Integer Rssi;
    public Float Distance;
    public String CreationTime;
}
