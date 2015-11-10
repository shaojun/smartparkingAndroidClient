package com.SmartParking.Demo.Sampling;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.Task.Action;
import com.SmartParking.Task.OnActionFinishedListener;
import com.SmartParking.Task.Task;
import com.SmartParking.Task.RestAction;
import com.SmartParking.WebService.RestEntityResultDumper;
import com.SmartParking.WebServiceEntity.UserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LogOnActivity extends Activity {
    private static final String LOG_TAG = "SmarkParking.Demo.LogOn";
    private static SharedPreferences logOnSharedPreferences = null;
    private boolean test = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_on);

//        Action<String> getWebUserAction = new Action<String>(1) {
//            @Override
//            public String exectue(Task ownerTask, Object state) {
//                return "getWebUserAction";
//            }
//        };
//
//        Action<Integer> getWebUserAction1 = new Action<Integer>(2) {
//            @Override
//            public Integer exectue(Task ownerTask, Object state) {
//                List<Object> result1 = ownerTask.getAggreatedResult(1);
//                if (result1.get(0).toString().equals("getWebUserAction"))
//                    return 8888888;
//                return 9999999;
//            }
//        };
//
//        Action getWebUserAction2 = new Action(3) {
//            @Override
//            public Object exectue(Task ownerTask, Object state) {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "getWebUserAction2";
//            }
//        };


//        final Task t = Task.Create(getWebUserAction);
//        t.continueWith(getWebUserAction1)
//                .continueWith(getWebUserAction2).Start(new OnActionFinishedListener() {
//            @Override
//            public void Finished(Task task, Action<?> finishedAction) {
//                if (task.isCompleted()) {
//                    final boolean waitResult = t.waitUntil(5000);
//                    final boolean isFFaulted = t.isFaulted();
//                    if (waitResult) {
//                        final Object result1 = t.getSingleResult(1);
//                        final Object result2 = t.getSingleResult(2);
//                        final Object result3 = t.getSingleResult(3);
//                        final Boolean isCompleted = t.isCompleted();
//                        final Boolean isFaulted = t.isFaulted();
//                        final Hashtable<Object, List<Exception>> exceptions = t.getAggreatedException();
//
//                        new AlertDialog.Builder(
//                                LogOnActivity.this)
//                                .setIcon(
//                                        android.R.drawable.ic_dialog_alert)
//                                .setTitle("check is complete?")
//                                .setMessage(
//                                        "result1: " + result1.toString() + ", result2: " + result2.toString()
//                                                + "result3: " + result3.toString() + ", isComp: " + isCompleted.toString()
//                                                + ", Isfaulted: " + isFaulted.toString() + ", exception count: " + exceptions.size())
//                                .setPositiveButton("Yes", null)
//                                .setNegativeButton(
//                                        "No", null
//                                ).show();
//
//                    }
//                }
//            }
//        });
        logOnSharedPreferences = getSharedPreferences("LogOn", 0);
        Button btnLogOn = (Button) findViewById(R.id.buttonLogOn);
        btnLogOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progress = ProgressDialog.show(LogOnActivity.this, "登陆中...",
                        "获取用户登陆信息", true);
                findViewById(R.id.editTextPwd).setEnabled(false);
                EditText uet = (EditText) findViewById(R.id.editTextUserName);
                EditText pet = (EditText) findViewById(R.id.editTextPwd);
                if (test) {
                    uet.setText("shawn");
                    pet.setText("19138177");
                }

                Task.Create(new RestAction("usersInfo/",
                        uet.getText().toString(),
                        pet.getText().toString(), "GET", "getRestUser")).Start(
                        new OnActionFinishedListener<String>() {
                            @Override
                            public void Finished(Task task, Action<String> finishedAction) {
                                progress.dismiss();
                                if (task.isFaulted()) {
                                    findViewById(R.id.editTextPwd).setEnabled(true);
                                    new AlertDialog.Builder(
                                            LogOnActivity.this)
                                            .setIcon(
                                                    android.R.drawable.ic_dialog_alert)
                                            .setTitle("Get usersInfo failed")
                                            .setMessage(task.getSingleException().toString())
                                            .setPositiveButton("Failed", null)
                                            .show();
                                } else {
                                    findViewById(R.id.editTextPwd).setEnabled(true);
                                    try {
                                        UserInfo.CurrentUserInfo = RestEntityResultDumper.dump(task.getSingleResult().toString(), UserInfo.class).get(0);
                                        SharedPreferences.Editor Ed = logOnSharedPreferences.edit();
                                        Ed.putString("UserName", UserInfo.CurrentUserInfo.UserName);
                                        Ed.putString("Password", ((EditText) findViewById(R.id.editTextPwd)).getText().toString());
                                        Ed.commit();
                                        Intent i = new Intent(LogOnActivity.this, OverallMapActivity.class);
                                        i.putExtra("userInfoName",
                                                UserInfo.CurrentUserInfo.UserName);
                                        startActivity(i);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        new AlertDialog.Builder(
                                                LogOnActivity.this)
                                                .setIcon(
                                                        android.R.drawable.ic_dialog_alert)
                                                .setTitle("Resolve underlying User failed")
                                                .setMessage("!!!")
                                                .setPositiveButton("Failed", null)
                                                .show();
                                        return;
                                    }
                                }
                            }
                        }
                );
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_on, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
