package com.pragmapure.flyevents;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

public class UploadService extends Service {

    private static final String TAG = "UploadService";
    public final IBinder mBinder = new LocalBinder();

    public UploadService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        while(wifiConnected.isConnected()){
            uploadPhoto();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean uploadPhoto(){
        Log.d(TAG, "Uploading a photo");
        return true;
    }

    public class LocalBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }
}
