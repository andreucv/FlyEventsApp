package com.pragmapure.flyevents;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.pragmapure.flyevents.classes.Photo;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileService extends Service {
    public static final String TAG = "FileService";
    public FileService() {
    }

    public final IBinder mBinder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
        String eventId = sharedPreferences.getString(Constants.EVENT_KEY, null);
        String dateOn = sharedPreferences.getString(Constants.DATEON_KEY, null);
        String dateOff = sharedPreferences.getString(Constants.DATEOFF_KEY, null);

        File path = Environment.getExternalStorageDirectory();
        String[] photos = path.list();
        if(eventId != null && dateOn != null && dateOff != null){
            // Then we're going to schedule an upload
            File photoToSchedule = new File(photos[0]);
            Long datePictureLong = photoToSchedule.lastModified();
            Date datePictureDate = new Date(datePictureLong);
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_SERVER);
            String dateText = dateFormat.format(datePictureDate);
            Date datePicture = null;
            try {
                datePicture = dateFormat.parse(dateText);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.d(TAG, datePicture.toString());
        }
        return 0;
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
}