package com.SmartParking.Demo.Sampling;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.SmartParking.Demo.Mapping.R;
import com.SmartParking.WebService.AsyncRestTask;
import com.SmartParking.WebService.OnAsyncRestTaskFinishedListener;
import com.SmartParking.WebServiceEntity.UserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LogOnActivity extends Activity {
    private static final String LOG_TAG = "SmarkParking.Demo.LogOn";
    private static SharedPreferences logOnSharedPreferences = null;
    public static UserInfo userInfo;
    private boolean test = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_on);
        logOnSharedPreferences = getSharedPreferences("LogOn", 0);
        Button btnLogOn = (Button) findViewById(R.id.buttonLogOn);
        btnLogOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.editTextPwd).setEnabled(false);
                if (test) {
                    SharedPreferences.Editor Ed = logOnSharedPreferences.edit();
                    Ed.putString("UserName", "shawn");
                    Ed.putString("Password", "19138177");
                    Ed.commit();
                    UserInfo userInfo = new UserInfo();
                    userInfo.UserName = "shawn";
                    userInfo.UUID = "00000000000UUID";
                    userInfo.MajorId = "1111111111MajorId";
                    userInfo.MinorId = "222222MinorId";
                    userInfo.MacAddress = "3333MacAddress";
                    userInfo.CreationTime = "1983";
                    userInfo.IsActive = true;
                    userInfo.Groups.add("SuperUsers");
                    LogOnActivity.userInfo = userInfo;
                    findViewById(R.id.editTextPwd).setEnabled(true);
                    Intent i = new Intent(LogOnActivity.this, OverallMapActivity.class);
                    startActivity(i);
                } else {
                    EditText uet = (EditText) findViewById(R.id.editTextUserName);
                    EditText pet = (EditText) findViewById(R.id.editTextPwd);

                    AsyncRestTask.Create("usersInfo/", uet.getText().toString(), pet.getText().toString(), "GET",
                            new OnAsyncRestTaskFinishedListener() {
                                @Override
                                public void OnError(String errorMsg) {
                                    findViewById(R.id.editTextPwd).setEnabled(true);
                                    new AlertDialog.Builder(
                                            LogOnActivity.this)
                                            .setIcon(
                                                    android.R.drawable.ic_dialog_alert)
                                            .setTitle("Get usersInfo failed")
                                            .setMessage(errorMsg)
                                            .setPositiveButton("Failed", null)
                                            .show();
                                }

                                @Override
                                public void OnFinished(Object json) {
                                    findViewById(R.id.editTextPwd).setEnabled(true);
                                    if (json instanceof JSONObject) {
                                        new AlertDialog.Builder(
                                                LogOnActivity.this)
                                                .setIcon(
                                                        android.R.drawable.ic_dialog_alert)
                                                .setTitle("Get usersInfo failed")
                                                .setMessage("Single usersInfo returned")
                                                .setPositiveButton("Failed", null)
                                                .show();
                                    } else if (json instanceof JSONArray) {
                                        try {
                                            JSONObject targetUser = (JSONObject) (((JSONArray) json).get(0));
                                            UserInfo userInfo = new UserInfo();
                                            userInfo.UserName = targetUser.getString("user_name");
                                            userInfo.UUID = targetUser.getString("uuid");
                                            userInfo.MajorId = targetUser.getString("major_Id");
                                            userInfo.MinorId = targetUser.getString("minor_Id");
                                            userInfo.MacAddress = targetUser.getString("mac_address");
                                            userInfo.CreationTime = targetUser.getString("creation_Time");
                                            userInfo.IsActive = targetUser.getBoolean("is_active");
                                            JSONArray userInfoGroupsJSONArray = targetUser.getJSONArray("groups");
                                            for (int i = 0; i < userInfoGroupsJSONArray.length(); i++) {
                                                userInfo.Groups.add(userInfoGroupsJSONArray.getString(i));
                                            }

                                            LogOnActivity.userInfo = userInfo;
                                            SharedPreferences.Editor Ed = logOnSharedPreferences.edit();
                                            Ed.putString("UserName", userInfo.UserName);
                                            Ed.putString("Password", ((EditText) findViewById(R.id.editTextPwd)).getText().toString());
                                            Ed.commit();
                                            Intent i = new Intent(LogOnActivity.this, OverallMapActivity.class);
                                            i.putExtra("userInfoName",
                                                    userInfo.UserName);
                                            startActivity(i);
                                        } catch (JSONException e) {
                                            new AlertDialog.Builder(
                                                    LogOnActivity.this)
                                                    .setIcon(
                                                            android.R.drawable.ic_dialog_alert)
                                                    .setTitle("Resolve Buildings failed")
                                                    .setMessage("!!!")
                                                    .setPositiveButton("Failed", null)
                                                    .show();
                                        }
                                    }
                                }
                            }).Start();
                }
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
