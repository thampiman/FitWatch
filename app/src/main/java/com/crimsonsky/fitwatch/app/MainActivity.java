package com.crimsonsky.fitwatch.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Ajay Thampi
 */

public class MainActivity extends Activity {

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String stepsCount = bundle.getString(StepCounterService.STEPS_COUNT);

                TextView stepsCountView = (TextView) findViewById(R.id.stepsView);
                stepsCountView.setText(stepsCount);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        updateView();
    }

    private void updateView() {
        // Display current date
        TextView dateView = (TextView) findViewById(R.id.dateView);
        DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy");
        Date date = new Date();
        dateView.setText(dateFormat.format(date));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(StepCounterService.NOTIFICATION));

        Intent intent = new Intent(this, StepCounterService.class);
        TextView dateView = (TextView) findViewById(R.id.dateView);
        intent.putExtra(StepCounterService.TODAYS_DATE,dateView.getText());
        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    public void onWatchClick(View view) {
        Button watchButton = (Button) view;
        if (watchButton != null) {
            if (watchButton.getText().equals(getResources().getString(R.string.watch))) {
                watchButton.setText(getResources().getString(R.string.unwatch));
            } else {
                watchButton.setText(getResources().getString(R.string.watch));
            }

            Intent intent = new Intent(this, StepCounterService.class);
            intent.putExtra(StepCounterService.WATCH,StepCounterService.WATCH);
            startService(intent);
        }
    }
}
