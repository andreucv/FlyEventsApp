package com.pragmapure.flyevents;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.pragmapure.flyevents.classes.Photo;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class UploadService extends Service {

    private static final String TAG = "UploadService";
    public final IBinder mBinder = new LocalBinder();

    ConnectivityManager connectivityManager;
    NetworkInfo wifiConnected;

    Handler handler;

    public UploadService() {}

    @Override
    public void onCreate() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        handler = new Handler();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(wifiConnected.isConnected()){
            handler.post(runnableCode);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean uploadPhoto(){

        HashMap<String, String> params = null;
        JSONObject response = null;
        // Search file in ORM
        List<Photo> listToUpload = Photo.find(Photo.class, "uploaded = ?", "0");
        if(!listToUpload.isEmpty()){
            Log.d(TAG, "Uploading a photo");
            Photo photoToUpload = listToUpload.get(0);
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
            params = new HashMap<>();
            params.put("imei", sharedPreferences.getString(Constants.IMEI_KEY, ""));
            params.put("event", photoToUpload.getIdEvent());
            HttpConnection httpConnection = new HttpConnection(Constants.UPLOAD_URL);
            httpConnection.makePostImage(params, photoToUpload.getFilename());
            photoToUpload.markUploaded();
        }

        return true;
    }

    public class LocalBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            new UploadAsyncTask().execute();
            handler.postDelayed(runnableCode, Constants.minTime);
        }
    };


    private class UploadAsyncTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            uploadPhoto();
            return null;
        }
    }
}
