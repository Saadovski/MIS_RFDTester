package com.example.mis_rfdtester;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private int vibration_time = 5000;
    private final Float threshold = new Float(15.0);
    private final int numberOfAnalysis = 1;
    private final int idxFall = 200;

    private Button startButton;
    private Button stopButton;
    private TextView action;

    private SensorManager sensorManager;
    private List<Float> AccX;
    private List<Float> AccY;
    private List<Float> AccZ;
    private List<Float> RotX;
    private List<Float> RotY;
    private List<Float> RotZ;

    private SensorListenerRunnable accel;
    private SensorListenerRunnable gyro;
    private SensorListenerRunnable handler;

    Thread accThread;
    Thread rotThread;
    Thread handlerThread;

    private boolean stopListening = false;
    private boolean peakDetected = false;

    MIS_RFD algo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: Initializing...");

        startButton = findViewById(R.id.button);
        stopButton = findViewById(R.id.button2);
        action = findViewById(R.id.Action);

        stopButton.setEnabled(false);
        action.setText("");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        algo = new MIS_RFD(MainActivity.this);

        gyro = new SensorListenerRunnable(Sensor.TYPE_GYROSCOPE, sensorManager) {
            @Override
            public void run() {
                while (!stopListening){}
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(RotX.size() >= 350){
                    RotX.remove(0);
                    RotY.remove(0);
                    RotZ.remove(0);
                }
                RotX.add(sensorEvent.values[0]);
                RotY.add(sensorEvent.values[1]);
                RotZ.add(sensorEvent.values[2]);
                //System.out.println("RotX" + RotX.size());
            }
        };
        accel = new SensorListenerRunnable(Sensor.TYPE_LINEAR_ACCELERATION, sensorManager) {
            @Override
            public void run() {
                while (!stopListening) {
                }
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (AccX.size() >= 350) {
                    AccX.remove(0);
                    AccY.remove(0);
                    AccZ.remove(0);
                }
                AccX.add(sensorEvent.values[0]);
                AccY.add(sensorEvent.values[1]);
                AccZ.add(sensorEvent.values[2]);

                //System.out.println("AccX" + AccX.size());
            }
        };
        handler = new SensorListenerRunnable(Sensor.TYPE_LINEAR_ACCELERATION, sensorManager) {
            @Override
            public void run() {
                while(!stopListening){}
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (AccX.size() >= 350 && !peakDetected && square(AccX.get(idxFall)) + square(AccY.get(idxFall)) + square(AccZ.get(idxFall)) >= square(threshold)) {
                    this.stopListening();
                    try {
                        analyseMovement();
                        Thread.sleep(5000);
                        this.listen();
                        peakDetected = false;
                        action.setText("");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACTIVITY_RECOGNITION }, 0);
        Log.d(TAG, "onCreate: Done");

    }

    public void StartButtonClicked(View view){
        Log.d(TAG, "StartButtonClicked: Function Started");

        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        AccX = new ArrayList<>();
        AccY = new ArrayList<>();
        AccZ = new ArrayList<>();
        RotX = new ArrayList<>();
        RotY = new ArrayList<>();
        RotZ = new ArrayList<>();

        accel.listen();
        gyro.listen();
        handler.listen();

        accThread = new Thread(accel);
        rotThread = new Thread(gyro);
        handlerThread = new Thread(handler);

        rotThread.start();
        accThread.start();
        handlerThread.start();

        Log.d(TAG, "StartButtonClicked: Threads created");

        Log.d(TAG, "StartButtonClicked: Function Finished");
    }

    public void stopButtonClicked(View view){
        Log.d(TAG, "StopButtonClicked: Function Started");

        stopListening();

        Log.d(TAG, "StopButtonClicked: Function finished");
    }

    private void stopListening(){
        stopListening = true;
        peakDetected = false;

        action.setText("");
        accel.stopListening();
        gyro.stopListening();

        accThread.interrupt();
        rotThread.interrupt();

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    public void analyseMovement() {
        boolean result = false;
        if(!peakDetected) {
            peakDetected = true;
            result = algo.getDetectedAction(AccX, AccY, AccZ, RotX, RotY, RotZ);
            if(result){
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(vibration_time, VibrationEffect.DEFAULT_AMPLITUDE)); //vibration
                }
                Handler h = new Handler(Looper.getMainLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        stopListening();
                        handler.stopListening();
                        handlerThread.interrupt();

                        getUserComment();
                    }
                });
            }
        }
    }

    public void getUserComment(){
        Intent intent = new Intent(this, CommentActivity.class);

        intent.putExtra("AccX", (Serializable) AccX);
        intent.putExtra("AccY", (Serializable) AccY);
        intent.putExtra("AccZ", (Serializable) AccZ);
        intent.putExtra("RotX", (Serializable) RotX);
        intent.putExtra("RotY", (Serializable) RotY);
        intent.putExtra("RotZ", (Serializable) RotZ);

        startActivity(intent);
    }

    public Float square(Float a){
        return a*a;
    }


}