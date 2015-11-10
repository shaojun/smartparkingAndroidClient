package com.SmartParking.Task;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.SmartParking.Util.Tuple;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by Think on 10/20/2015.
 */
public class Task<TResult> {
    private static final String LOG_TAG = "SmarkParking.Task";

    /**
     * blocking current thread and wait all concurrently running Tasks done until timeout reached.
     *
     * @param tasks
     * @param timeout
     */
    public static boolean waitAll(ArrayList<Task> tasks, int timeout) {
        int totalSpendTime = 0;
        for (Task t : tasks
                ) {
            t.Start();
        }

        while (true) {
            boolean allCompleted = true;
            for (Task t : tasks
                    ) {
                if (!t.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) return true;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }

            if ((totalSpendTime += 500) >= timeout) {
                return false;
            }
        }
    }

    private Task() {
    }

    private Action<TResult> action;
    private Object state;

    public static <TResult> Task Create(Action<TResult> action) {
        return new Task<TResult>(action);
    }

    private Task(Action<TResult> action) {
        this.action = action;
        this.state = action.getStateObject();
    }

    private ArrayList<Action<TResult>> continuationActions = new ArrayList<>();

    public Task continueWith(Action<TResult> continuationAction) {
        this.continuationActions.add(continuationAction);
        return this;
    }

    private innerAsyncTask asyncTask;

    public void Start() {
        this.Start(null);
    }

    private List<OnActionFinishedListener<TResult>> listeners = new ArrayList<>();

    /**
     * Start the task asynchronously, any action in task exceptioned will cause all stopped to executing.
     *
     * @param listener all action in this task execute finished will call this listener, and the calling is running on UI thread.
     */
    public void Start(OnActionFinishedListener listener) {
        Log.i(LOG_TAG, "Task starting...");
        if (listener != null)
            listeners.add(listener);
        this.isCompleted = false;
        this.asyncTask = new innerAsyncTask(this.action, this);
        this.asyncTask.execute("");
    }

    private Hashtable<Object, List<Exception>> aggregatedException = new Hashtable<>();
    private Hashtable<Object, List<TResult>> results = new Hashtable<>();

    /**
     * Get the result list by input an action's state object, since this action with same state object
     * may be executed multiple times, so a List of result will be returned.
     *
     * @param actionState
     * @return an action may be executed multiple times(by continuation function of 'continueWith(...)'), so multiple result is possible.
     */
    public List<TResult> getAggreatedResult(Object actionState) {
        List<TResult> temp = results.get(actionState);
        return temp;
    }

    /**
     * Get the first result in result list.
     *
     * @return the first result in result list.
     */
    public TResult getSingleResult() {
        TResult temp = results.elements().nextElement().get(0);
        return temp;
    }

    /**
     * Gets whether this System.Threading.Tasks.Task has completed.
     *
     * @return true if the task has completed; otherwise false.
     */
    public boolean isCompleted() {
        return this.isCompleted;
    }

    /**
     * Gets whether the Task completed due to an unhandled exception.
     * Any action in task exceptioned will cause all stopped to executing.
     * combine with getAggreatedException() to get detail exception.
     *
     * @return true if the task has thrown an unhandled exception; otherwise false.
     */
    public boolean isFaulted() {
        return this.isFaulted;
    }

    /**
     * Gets all unhandled exception for all executed actions.
     *
     * @return state object and its exceptions, an action may be executed multiple times(by continuation), so multiple exception is possible.
     */
    public Hashtable<Object, List<Exception>> getAggreatedException() {
        return this.aggregatedException;
    }

    /**
     * Get the first exception string in exception list.
     *
     * @return first exception string in exception list.
     */
    public Exception getSingleException() {
        return this.aggregatedException.elements().nextElement().get(0);
    }

    /**
     * cancel the task, the rest of actions will not be executed.
     */
    public void cancel() {
        this.isCancelling = true;
    }

    /**
     * blocking the current calling thread until the Task is finished or timeout reached
     *
     * @return True for finished successfully, otherwise timeout occured.
     */
    public boolean waitUntil(int timeout) {
        Log.i(LOG_TAG, "waitUntil was called with timeout: " + timeout);
        int totalSpendTime = 0;
        while (!this.isCompleted()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }

            if ((totalSpendTime += 500) >= timeout) {
                {
                    Log.i(LOG_TAG, "waitUntil timed out");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isCompleted = false;
    private boolean isFaulted = false;
    private boolean isCancelling = false;
    private boolean isCancelled = false;

    private void onActionFinished(Action<TResult> finishedAction, TResult result, boolean errorOccured) {
        Log.i(LOG_TAG, "An action was finished, continuationActions count: " + continuationActions.size() + ", errorOccured: " + errorOccured);
        if (this.isCancelling) {
            this.isCancelled = true;
            this.isCompleted = true;
        } else if (!errorOccured) {
            if (this.continuationActions.size() > 0) {
                Action<TResult> nextContinuationAction = this.continuationActions.get(0);
                this.continuationActions.remove(0);
                this.asyncTask = new innerAsyncTask(nextContinuationAction, this);
                this.asyncTask.execute("");
            } else {
                Log.i(LOG_TAG, "Task set to IsCommpleted with true ");
                this.isCompleted = true;
            }
        } else {
            Log.e(LOG_TAG, "Task set to IsCommpleted with true ");
            this.isFaulted = true;
            this.isCompleted = true;
        }

        if (this.results.get(finishedAction.getStateObject()) == null) {
            this.results.put(finishedAction.getStateObject(), new ArrayList<TResult>());
        }

        this.results.get(finishedAction.getStateObject()).add(result);
    }

    private class innerAsyncTask extends AsyncTask<String, Integer, TResult> {
        Action<TResult> targetAction;
        Task ownerTask;

        public innerAsyncTask(Action<TResult> targetAction, Task ownerTask) {
            this.targetAction = targetAction;
            this.ownerTask = ownerTask;
        }

        @Override
        protected TResult doInBackground(String... params) {
            TResult result = null;
            try {
                result = this.targetAction.execute(this.ownerTask);
                onActionFinished(this.targetAction, result, false);
                return result;
            } catch (Exception ex) {
                Log.e(LOG_TAG, "An action executed with exception");
                if (aggregatedException.get(this.targetAction.getStateObject()) == null) {
                    aggregatedException.put(this.targetAction.getStateObject(), new ArrayList<Exception>());
                }

                aggregatedException.get(this.targetAction.getStateObject()).add(ex);
                onActionFinished(this.targetAction, result, true);
                return null;
            }
        }

        protected void onPostExecute(TResult result) {
            for (OnActionFinishedListener l : listeners
                    ) {
                l.Finished(this.ownerTask, this.targetAction);
            }
        }
    }
}