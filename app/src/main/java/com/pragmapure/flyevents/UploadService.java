package com.pragmapure.flyevents;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.pragmapure.flyevents.classes.Photo;

import java.util.HashMap;
import java.util.List;

public class UploadService extends Service {

    private static final String TAG = "UploadService";
    public final IBinder mBinder = new LocalBinder();

    public UploadService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if(wifiConnected.isConnected()){
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
        HashMap<String, String> params = null;
        // Search file in ORM
        List<Photo> listToUpload = Photo.find(Photo.class, "uploaded = ?", "false");
        if(!listToUpload.isEmpty()){
            Photo photoToUpload = listToUpload.get(0);
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
            params = new HashMap<>();
            params.put("imei", sharedPreferences.getString(Constants.IMEI_KEY, ""));
            params.put("event", photoToUpload.getIdEvent());
            HttpConnection httpConnection = new HttpConnection(Constants.UPLOAD_URL);
            httpConnection.makePostImage(params, photoToUpload.getFilename());
        }

        return true;
    }

    public class LocalBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }
}
