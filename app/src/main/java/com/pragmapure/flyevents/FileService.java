package com.pragmapure.flyevents;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import com.pragmapure.flyevents.classes.Photo;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.os.Handler;

public class FileService extends Service {
    public static final String TAG = "FileService";
    public FileService() {
    }

    public final IBinder mBinder = new LocalBinder();
    Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        handler.post(runnableCode);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind FileService");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public FileService getService() {
            return FileService.this;
        }
    }

    public Date parseLong(long date){
        return new Date(date);
    }

    public Date parseString(String date){
        if (date == null) return null;
        Date datePicture = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_SERVER);
        try {
            datePicture = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return datePicture;
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
            String eventId = sharedPreferences.getString(Constants.EVENT_KEY, null);
            Date dateOn = parseString(sharedPreferences.getString(Constants.DATEON_KEY, null));
            Date dateOff = parseString(sharedPreferences.getString(Constants.DATEOFF_KEY, null));

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File pathCamera = new File(path.getAbsolutePath()+"/Camera/");

            String[] photosPath = pathCamera.list();
            Log.d(TAG, photosPath.toString());

            if(eventId != null && dateOn != null && dateOff != null){
                // Then we're going to schedule an upload
                for(String photoPath: photosPath){
                    Log.d(TAG, "PhotoPath of foto processed now: " + photoPath);
                    File photoToSchedule = new File(pathCamera+"/"+photoPath);
                    if (!photoToSchedule.isDirectory()) {
                        Date pictureDate = parseLong(photoToSchedule.lastModified());
                        Log.d(TAG, "Date of the picture: "+pictureDate.toString());
                        if(!pictureDate.before(dateOn) && !pictureDate.after(dateOff)){
                            // Add to the Photos Records
                            List<Photo> result = Photo.find(Photo.class, "filename = ?", pathCamera+"/"+photoPath);
                            if(result.isEmpty()){
                                Log.d(TAG, "We have saved the foto");
                                Photo photo = new Photo(eventId, pathCamera+"/"+photoPath);
                                photo.save();
                            }
                        }
                    }

                }
            }

            handler.postDelayed(runnableCode, Constants.TIME_GPS_SEARCH);
        }
    };
}
