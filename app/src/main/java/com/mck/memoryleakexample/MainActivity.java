package com.mck.memoryleakexample;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    static Activity staticActivityLeak;
    static View staticViewLeak;
    static Object staticInnerClassLeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View bNextActivity = findViewById(R.id.bNextActivity);
        bNextActivity.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                nextActivity();
            }
        });

        View bStaticActivity = findViewById(R.id.bStaticActivityLeak);
        bStaticActivity.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                staticActivityLeak = MainActivity.this;
                nextActivity();
            }
        });

        View bStaticViewLeak = findViewById(R.id.bStaticViewLeak);
        bStaticViewLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                staticViewLeak = findViewById(R.id.bStaticViewLeak);
                nextActivity();
            }
        });

        View bStaticInnerClassLeak = findViewById(R.id.bStaticInnerClassLeak);
        bStaticInnerClassLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                staticInnerClassLeak = new InnerClass();
                nextActivity();
            }
        });

        View bAnonymousClassLeak = findViewById(R.id.bAnonymousClassLeak);
        bAnonymousClassLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>(){

                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(20000);
                        return null;
                    }
                }.execute();
                nextActivity();
            }
        });

        View bHandlerLeak = findViewById(R.id.bHandlerLeak);
        bHandlerLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MyHandler().postDelayed(new MyRunnable(), Long.MAX_VALUE >> 1);
                nextActivity();
            }
        });

        View bThreadLeak = findViewById(R.id.bThreadLeak);
        bThreadLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        while(true);
                    }
                }.start();
                nextActivity();
            }
        });

        View bTimerTaskLeak = findViewById(R.id.bTimerTaskLeak);
        bTimerTaskLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        while (true);
                    }
                }, Long.MAX_VALUE >> 1);
            }
        });

        View bSensorManagerLeak = findViewById(R.id.bSensorManagerLeak);
        bSensorManagerLeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
                Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ALL);
                manager.registerListener(new MySensorListener(), sensor, SensorManager.SENSOR_DELAY_FASTEST);
                nextActivity();
            }
        });
    }


    class MySensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.v("fdsa", "onSensorChanged()");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.v("fdsa", "onAccuracyChanged()");
        }

    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    class MyRunnable implements Runnable {
        @Override
        public void run() {
            while(true);
        }
    }

    private void nextActivity() {
        Intent intent = new Intent(this,SecondActivity.class);
        startActivity(intent);
        SystemClock.sleep(600);
        finish();
    }

    class InnerClass{

    }

}
