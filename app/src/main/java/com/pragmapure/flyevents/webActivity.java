package com.pragmapure.flyevents;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pragmapure.flyevents.classes.Photo;

public class webActivity extends AppCompatActivity {

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
        String lat = prefs.getString(Constants.GPS_LAT_KEY, null);
        String longi = prefs.getString(Constants.GPS_LONG_KEY, null);

        String url = Constants.SERVER_URL;
        if (prefs.getBoolean(Constants.EVENTS_NOTIFICATION, false)) {
            prefs.edit().putBoolean(Constants.EVENTS_NOTIFICATION, false).apply();
            url = Constants.WEB_EVENTS_URL;
        }
        myWebView.loadUrl(url+"?imei="+imei+"&latitude="+lat+"&longitude="+longi);
    }

}
