package com.pragmapure.flyevents;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class GpsService extends Service {

    private static final String TAG = "GpsServiceFlyEvents";
    public GpsService() {}

    public final IBinder mBinder = new LocalBinder();

    LocationManager locationManager;
    LocationListener locationListener;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "onCreate in Service");
        sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "" + location.getLatitude());
                sharedPreferences.edit().putString(Constants.GPS_LAT_KEY, "" + location.getLatitude()).apply();
                sharedPreferences.edit().putString(Constants.GPS_LONG_KEY, "" + location.getLongitude()).apply();
                new LocationServerTask().execute(location);
                Log.d(TAG, "1");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "2");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "3");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "4");
            }
        };

        Log.d(TAG, locationListener.toString());
        Log.d(TAG, locationManager.toString());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "Error in checkSelfPermission");
            // TODO THE HANDLER
        }
        // 0 time in milliseconds between changes and 0 distance in meters between change
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.minTime, Constants.minDistance, locationListener);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(Constants.GPS_LAT_KEY, "" + location.getLatitude()).apply();
            sharedPreferences.edit().putString(Constants.GPS_LONG_KEY, "" + location.getLongitude()).apply();
            new LocationServerTask().execute(location);
        }
        Log.d(TAG, "" + sharedPreferences.getString(Constants.GPS_LAT_KEY, ""));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public GpsService getService() {
            return GpsService.this;
        }
    }

    private JSONObject uploadLocation(Location l) {
        HashMap<String, String> data = new HashMap<>();
        data.put("imei", sharedPreferences.getString(Constants.IMEI_KEY, ""));
        data.put("latitude", ""+l.getLatitude());
        data.put("longitude", ""+l.getLongitude());
        HttpConnection h = new HttpConnection(Constants.SEARCH_EVENTS);
        return h.makePostText(data);
    }

    private class LocationServerTask extends AsyncTask<Location, Void, JSONObject> {


        @Override
        protected JSONObject doInBackground(Location... params) {
            boolean hasEvent = true;
            int idevent = 1;
            if (hasEvent) {
                sharedPreferences.edit().putString(Constants.EVENT_KEY, ""+idevent).apply();
                return null;
            } else {
                return uploadLocation(params[0]);
            }
        }

        @Override
        protected void onPostExecute(JSONObject ev) {
            locationManager.removeUpdates(locationListener);
            if (ev != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.minTime, Constants.minDistance, locationListener);
                sharedPreferences.edit().remove(Constants.EVENT_KEY).apply();
                try {
                    JSONArray events = ev.getJSONArray("events");
                    if (events.length()>0) {
                        Log.d(TAG, "HAY EVENTOS");
                    } else {
                        Log.d(TAG, "NO HAY EVENTOS");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
