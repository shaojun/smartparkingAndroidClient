package com.SmartParking.Task;

/**
 * Created by Think on 10/20/2015.
 */
public interface OnActionFinishedListener<TResult> {
    /* will be called for each Action (which in a Task action chain by continuation) finished its execution
     */
    void Finished(Task task, Action<TResult> lastFinishedAction);
}
