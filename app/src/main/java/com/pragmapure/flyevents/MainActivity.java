package com.pragmapure.flyevents;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        SharedPreferences prefs = getSharedPreferences(Constants.SP_FE, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.IMEI_KEY, telephonyManager.getDeviceId());
        prefs.edit().apply();
        Boolean isRegistered = prefs.getBoolean(Constants.REGISTERED_KEY, false);

        if (isRegistered) {
            Intent i = new Intent(this, webActivity.class);
            startActivity(i);
            return;
        }


        final Button breg = (Button) findViewById(R.id.buttonRegister);
        final EditText namereg = (EditText)findViewById(R.id.nameRegister);
        final EditText mailreg = (EditText)findViewById(R.id.emailRegister);
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final ProgressDialog progress = new ProgressDialog(this);



        progress.setTitle(getString(R.string.loadigpdtitle));
        progress.setMessage(getString(R.string.loadingpd));

        breg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String snamereg = namereg.getText().toString();
                String smailreg = mailreg.getText().toString();

                if (snamereg.trim().length() > 0 && smailreg.trim().length() > 0) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    breg.setEnabled(false);
                    progress.show();

                } else {
                    Snackbar.make(view, "There is any empty field", Snackbar.LENGTH_LONG)
                            .show();
                }


            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
