package com.SmartParking.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 10/25/2015.
 */
public class AggregatedContent<T> {
    List<T> contents = new ArrayList<>();

    public void add(T content)
    {
        this.contents.add(content);
    }

    public void remove(T content)
    {
        this.contents.remove(content);
    }

    //public void get

    @Override
    public String toString() {
        return super.toString();
    }
}
