package com.SmartParking.Task;

import java.security.Policy;
import java.util.ArrayList;

/**
 * Created by Think on 10/20/2015.
 */
public abstract class Action<TResult> {
    //private ArrayList<OnActionFinishedListener<TResult>> listeners = new ArrayList<>();
    private Object state = null;

    private Action() {
    }

    public Action(Object state) {
        this.state = state;
    }

    public abstract TResult execute(Task ownerTask) throws Exception;

//    public void addOnActionFinishedListener(OnActionFinishedListener<TResult> listener) {
//        this.listeners.add(listener);
//    }
//
//    public boolean removeOnActionFinishedListener(OnActionFinishedListener<TResult> listener)
//    {
//       return this.listeners.remove(listener);
//    }
//
//    public ArrayList<OnActionFinishedListener<TResult>> getOnActionFinishedListeners()
//    {
//        return this.listeners;
//    }

    public Object getStateObject() {
        return this.state;
    }
//    public void exctue(Object parameter0, Object parameter1, Object state) {
//
//    }
//
//    public void exctue(Object parameter0, Object parameter1, Object parameter2, Object state) {
//
//    }
//
//    public void exctue(Object parameter0, Object parameter1, Object parameter2, Object parameter3, Object state) {
//
//    }
}

