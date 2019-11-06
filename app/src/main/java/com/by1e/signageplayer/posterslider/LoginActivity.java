package com.by1e.signageplayer.posterslider;

import android.Manifest;

import com.by1e.signageplayer.posterslider.BuildConfig;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LoginActivity extends AppCompatActivity {
    Button btnConnect;
    EditText storeId;
    EditText serverAddress;
    EditText serverKey;
    private String android_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        btnConnect = findViewById(R.id.btn_connect);
        storeId = (EditText) findViewById(R.id.input_storeId);
        serverAddress = (EditText) findViewById(R.id.input_serverAddress);
        serverKey = (EditText) findViewById(R.id.input_serverKey);
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        isStoragePermissionGranted();
        btnConnect.setOnClickListener(new View.OnClickListener() {
            String storeIdVal;
            String serverAddressVal;
            String serverKeyVal;
            @Override
            public void onClick(View view) {
                if (validateFields()){
                    AsyncTaskRunner runner = new AsyncTaskRunner();
                    runner.execute(storeIdVal,serverAddressVal,serverKeyVal);
                } else {
                    Toast.makeText(getApplicationContext(), "Please input all fields", Toast.LENGTH_LONG).show();
                }

            }

            boolean validateFields(){
                if(!isEmpty(storeId) && !isEmpty(serverAddress) && !isEmpty(serverKey)){
                    storeIdVal = storeId.getText().toString();
                    serverAddressVal = serverAddress.getText().toString();
                    serverKeyVal = serverKey.getText().toString();

                    return true;
                }
                return false;
            }

            private boolean isEmpty(EditText etText) {
                return etText.getText().toString().trim().length() == 0;
            }
        });




        // Set up the user interaction to manually show or hide the system UI.

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }


    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String resp;
        ProgressDialog progressDialog;
        String storeIdVal;
        String serverAddressVal;
        String serverKeyVal;

        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping..."); // Calls onProgressUpdate()

            try {
                storeIdVal = params[0];
                serverAddressVal = params[1];
                serverKeyVal = params[2];


                JSONObject requestData = new JSONObject();
                requestData.put("serverKey",serverKeyVal);
                requestData.put("hardwareKey", android_id);
                requestData.put("displayName", storeIdVal+"-"+android_id);
                requestData.put("clientType", "AndroidDisplay");
                requestData.put("clientVersion", Integer.toString(BuildConfig.VERSION_CODE));
                requestData.put("clientCode", 1);
                requestData.put("operatingSystem", "v7.1.1");

                requestData.put("serverAddress",serverAddressVal);

                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPrefsFile), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.hardwareKey), android_id);
                editor.putString(getString(R.string.displayName), storeIdVal+"-"+android_id);
                editor.putString(getString(R.string.storeId), storeIdVal);
                editor.putString(getString(R.string.serverAddress), serverAddressVal);
                editor.putString(getString(R.string.serverKey), serverKeyVal);
                editor.commit();

                if(makePostRequest(serverAddressVal,requestData.toString())){
                    resp = "Successful";
                    Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(activityIntent);
                    finish();
                } else {
                    resp = "Failed";
                }

            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }

        public boolean makePostRequest(String stringUrl, String payload) throws IOException {
            URL url = new URL(stringUrl+"/rd");
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            String line;
            StringBuffer jsonString = new StringBuffer();
            System.out.println(payload);
            uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            uc.setRequestMethod("POST");
            uc.setDoInput(true);
            uc.setInstanceFollowRedirects(false);
            uc.connect();
            OutputStreamWriter writer = new OutputStreamWriter(uc.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            try {
                if (uc.getResponseCode() == 200){
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedPrefsFile), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();


                    editor.putBoolean(getString(R.string.isLoggedIn), true);
                    editor.commit();
                    uc.disconnect();
                    return true;
                }
//                BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
//                while((line = br.readLine()) != null){
//                    jsonString.append(line);
//                }

//                br.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            uc.disconnect();

            return false;
        }


        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), result , Toast.LENGTH_LONG).show();
        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(LoginActivity.this,
                    "Registering the device",
                    "Please wait for few seconds");
        }


        @Override
        protected void onProgressUpdate(String... text) {
            Toast.makeText(getApplicationContext(), "In Progress!", Toast.LENGTH_LONG).show();

        }
    }




}
