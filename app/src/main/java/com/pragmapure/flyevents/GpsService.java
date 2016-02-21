package com.pragmapure.flyevents;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class GpsService extends Service  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{

    private static final String TAG = "GpsServiceFlyEvents";
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    public final IBinder mBinder = new LocalBinder();
    SharedPreferences sharedPreferences;
    Context c;
    Handler handler = new Handler();

    public GpsService() {

    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            new LocationServerTask().execute();
            // Repeat this the same runnable code block again another 2 seconds
            handler.postDelayed(runnableCode, Constants.TIME_GPS_SEARCH);
        }
    };

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onCreate(){
        c = this;
        sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate in Service");
        buildGoogleApiClient();
        createLocationRequest();
        handler.post(runnableCode);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        Log.d(TAG, "" + sharedPreferences.getString(Constants.GPS_LAT_KEY, ""));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            sharedPreferences.edit().putString(Constants.GPS_LAT_KEY, "" + mLastLocation.getLatitude()).apply();
            sharedPreferences.edit().putString(Constants.GPS_LONG_KEY, "" + mLastLocation.getLongitude()).apply();
        } else {
            Log.d(TAG, "SIN UBICACION");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mLastLocation != null) {
            sharedPreferences.edit().putString(Constants.GPS_LAT_KEY, "" + mLastLocation.getLatitude()).apply();
            sharedPreferences.edit().putString(Constants.GPS_LONG_KEY, "" + mLastLocation.getLongitude()).apply();
        } else {
            Log.d(TAG, "SIN UBICACION2");
        }
    }


    public class LocalBinder extends Binder {
        public GpsService getService() {
            return GpsService.this;
        }
    }


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.TIME_LOCATION);
        mLocationRequest.setFastestInterval(Constants.TIME_LOCATION_MIN);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
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
