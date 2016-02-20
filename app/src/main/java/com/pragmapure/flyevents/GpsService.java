package com.pragmapure.flyevents;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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
    Context c;

    Handler handler = new Handler();

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            new LocationServerTask().execute();
            // Repeat this the same runnable code block again another 2 seconds
            handler.postDelayed(runnableCode, Constants.TIME_GPS_SEARCH);
        }
    };

    @Override
    public void onCreate(){
        c = this;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "onCreate in Service");
        sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "" + location.getLatitude());
                sharedPreferences.edit().putString(Constants.GPS_LAT_KEY, "" + location.getLatitude()).apply();
                sharedPreferences.edit().putString(Constants.GPS_LONG_KEY, "" + location.getLongitude()).apply();

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
        handler.post(runnableCode);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnableCode);
    }

    private JSONObject uploadLocation() {
        HashMap<String, String> data = new HashMap<>();
        data.put("imei", sharedPreferences.getString(Constants.IMEI_KEY, ""));
        data.put("latitude", ""+sharedPreferences.getString(Constants.GPS_LAT_KEY, ""));
        data.put("longitude", ""+sharedPreferences.getString(Constants.GPS_LONG_KEY, ""));
        HttpConnection h = new HttpConnection(Constants.SEARCH_EVENTS);
        return h.makePostText(data);
    }

    private class LocationServerTask extends AsyncTask<Void, Void, JSONObject> {


        @Override
        protected JSONObject doInBackground(Void... params) {
            HashMap<String, String> data = new HashMap<>();
            data.put("imei", sharedPreferences.getString(Constants.IMEI_KEY, ""));
            HttpConnection h = new HttpConnection(Constants.ACTUAL_EVENT);
            JSONObject resp =  h.makePostText(data);

            boolean hasEvent = !resp.isNull("event");

            if (hasEvent) {
                JSONObject ev = null;
                try {
                ev = resp.getJSONObject("event");
                sharedPreferences.edit().putString(Constants.EVENT_KEY, ""+ev.getInt("id")).apply();
                sharedPreferences.edit().putString(Constants.DATEON_KEY, ev.getString("time_1")).apply();
                sharedPreferences.edit().putString(Constants.DATEOFF_KEY, ev.getString("time_2")).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            } else {
                return uploadLocation();
            }
        }

        @Override
        protected void onPostExecute(JSONObject ev) {
            if (ev != null) {
                sharedPreferences.edit().remove(Constants.EVENT_KEY).apply();
                try {
                    JSONArray events = ev.getJSONArray("events");
                    if (events.length()>0 && !sharedPreferences.getBoolean(Constants.EVENTS_NOTIFICATION, false)) {
                        sharedPreferences.edit().putBoolean(Constants.EVENTS_FOUND_KEY, true).apply();
                        sharedPreferences.edit().putBoolean(Constants.EVENTS_NOTIFICATION, true).apply();
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(c)
                                        .setSmallIcon(R.drawable.bullseye)
                                        .setContentTitle("New Events")
                                        .setContentText("There are events near of you");
                        Intent resultIntent = new Intent(c, webActivity.class);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
                        stackBuilder.addParentStack(webActivity.class);
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent =
                                stackBuilder.getPendingIntent(
                                        0,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                );
                        mBuilder.setContentIntent(resultPendingIntent);
                        mBuilder.setAutoCancel(true);
                        NotificationManager mNotificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(1, mBuilder.build());

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
