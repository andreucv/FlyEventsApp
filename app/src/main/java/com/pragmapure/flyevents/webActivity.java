package com.pragmapure.flyevents;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pragmapure.flyevents.classes.Photo;

import android.os.Handler;

public class webActivity extends AppCompatActivity {

    String lat = null;
    String longi = null;

    SharedPreferences prefs;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        SharedPreferences prefs = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
        String imei = prefs.getString(Constants.IMEI_KEY, null);
        lat = prefs.getString(Constants.GPS_LAT_KEY, null);
        longi = prefs.getString(Constants.GPS_LONG_KEY, null);

        String url = Constants.HOME_URL;
        if (prefs.getBoolean(Constants.EVENTS_NOTIFICATION, false)) {
            prefs.edit().putBoolean(Constants.EVENTS_NOTIFICATION, false).apply();
            url = Constants.WEB_EVENTS_URL;
        }
        myWebView.loadUrl(url);

        handler = new Handler();
        handler.post(runnableCode);
        myWebView.loadUrl(url+"?imei="+imei+"&latitude="+lat+"&longitude="+longi);
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();

            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            lat = prefs.getString(Constants.GPS_LAT_KEY, null);
            longi = prefs.getString(Constants.GPS_LONG_KEY, null);
            // Repeat this the same runnable code block again another 2 seconds
            handler.postDelayed(runnableCode, Constants.TIME_GPS_SEARCH);
        }
    };

}
