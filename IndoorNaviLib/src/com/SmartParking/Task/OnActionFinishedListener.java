package com.SmartParking.Task;

/**
 * Created by Think on 10/20/2015.
 */
public interface OnActionFinishedListener<TResult> {
    void Finished(Task task, Action<TResult> finishedAction);
}
