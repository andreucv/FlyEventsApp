package com.pragmapure.flyevents;

import android.Manifest;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    GpsService mServiceGps;
    boolean mBoundGps = false;

    UploadService mServiceUpload;
    boolean mBoundUpload = false;

    FileService mServiceFile;
    boolean mBoundFile = false;

    private static final String TAG = "MAIN";
    ProgressDialog progress;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Button breg = (Button) findViewById(R.id.buttonRegister);
        final EditText namereg = (EditText)findViewById(R.id.nameRegister);
        final EditText mailreg = (EditText)findViewById(R.id.emailRegister);
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        progress = new ProgressDialog(this);
        prefs = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);


        progress.setTitle(getString(R.string.loadigpdtitle));
        progress.setMessage(getString(R.string.loadingpd));

        final MainActivity me = this;

        breg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String snamereg = namereg.getText().toString();
                String smailreg = mailreg.getText().toString();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                if (snamereg.trim().length() > 0 && isEmailValid(smailreg)) {
                    breg.setEnabled(false);
                    HashMap<String, String> data = new HashMap<String, String>();
                    data.put("name", snamereg);
                    data.put("email", smailreg);
                    data.put("imei", prefs.getString(Constants.IMEI_KEY, ""));
                    new LoginTask(me).execute(data);

                } else if(!isEmailValid(smailreg)) {
                    Snackbar.make(view, "The mail hasn't a valid format", Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    Snackbar.make(view, "There is any empty field", Snackbar.LENGTH_LONG)
                            .show();
                }


            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    protected void onStart() {
        super.onStart();

        ArrayList<String> perms = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

       if (perms.size()>0) {
           ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), Constants.PERMISSIONS_QUERY);
       } else {
            initAll();
       }

    }

    private void initAll() {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        prefs.edit().putString(Constants.IMEI_KEY, telephonyManager.getDeviceId()).apply();

        Intent intentGps = new Intent(this, GpsService.class);
        bindService(intentGps, mConnectionGps, Context.BIND_AUTO_CREATE);

        Intent intentUpload = new Intent(this, UploadService.class);
        bindService(intentUpload, mConnectionUpload, Context.BIND_AUTO_CREATE);

        Intent intentFile = new Intent(this, FileService.class);
        bindService(intentFile, mConnectionFile, Context.BIND_AUTO_CREATE);

        Boolean isRegistered = prefs.getBoolean(Constants.REGISTERED_KEY, false);

        if (isRegistered) {
            Intent i = new Intent(this, webActivity.class);
            startActivity(i);

            return;
        }

        // Bind to LocalService

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean allGrant = true;
        switch (requestCode) {
            case Constants.PERMISSIONS_QUERY: {
                // If request is cancelled, the result arrays are empty.
                for (int i: grantResults) {
                    allGrant = allGrant && (i == PackageManager.PERMISSION_GRANTED);
                }
            }
        }

        if (allGrant) {
            initAll();
        } else {
            //TODO: REQUEST PERMISSIONS ONE MORE TIME.
        }
    }


    private ServiceConnection mConnectionGps = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mServiceGps = ((GpsService.LocalBinder) service).getService();
            Intent intent = new Intent(getApplicationContext(), GpsService.class);
            mServiceGps.startService(intent);
            mBoundGps = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundGps = false;
            mServiceGps.stopSelf();
        }
    };

    private ServiceConnection mConnectionUpload = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mServiceUpload = ((UploadService.LocalBinder) service).getService();
            Intent intent = new Intent(getApplicationContext(), UploadService.class);
            mServiceUpload.startService(intent);
            mBoundUpload = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundUpload = false;
            mServiceUpload.stopSelf();
        }
    };

    private ServiceConnection mConnectionFile = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mServiceFile = ((FileService.LocalBinder) service).getService();
            Intent intent = new Intent(getApplicationContext(), FileService.class);
            mServiceFile.startService(intent);
            mBoundFile = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundFile = false;
            mServiceFile.stopSelf();
        }
    };

    public boolean isEmailValid(String email)
    {
        String regExpn =
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        +"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        +"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        +"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if(matcher.matches())
            return true;
        else
            return false;
    }

    private class LoginTask extends AsyncTask<HashMap<String, String>, Void, Boolean> {


        MainActivity parent;

        public  LoginTask(MainActivity m) {
            parent = m;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                progress.hide();
                prefs.edit().putBoolean(Constants.REGISTERED_KEY, true).apply();
                Intent i = new Intent(parent, webActivity.class);
                startActivity(i);
            } else {
                progress.hide();
            }
        }


        @Override
        protected Boolean doInBackground(HashMap<String, String>... params) {
            HttpConnection req = new HttpConnection(Constants.CREATE_USER);
            JSONObject resp = req.makePostText(params[0]);
            try {
               String id = resp.getString("id");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.show();
        }
    }


}
