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
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CountDownTimer timer;
    private Button startButton;
    private TextView countdown;
    private TextView action;

    long preparation_time = 3000;
    long listening_time = 7000;

    private SensorManager sensorManager;
    private List<Float> AccX;
    private List<Float> AccY;
    private List<Float> AccZ;
    private List<Float> RotX;
    private List<Float> RotY;
    private List<Float> RotZ;
    private int counter = 0;

    private SensorListenerRunnable accel;
    private SensorListenerRunnable gyro;

    private boolean stopListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: Initializing...");

        startButton = findViewById(R.id.button);
        countdown = findViewById(R.id.Countdown);
        action = findViewById(R.id.Action);

        action.setText("None");
        countdown.setText("07:00");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accel = new SensorListenerRunnable(Sensor.TYPE_LINEAR_ACCELERATION, sensorManager) {
            @Override
            public void run() {
                while (!stopListening){}
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                AccX.add(sensorEvent.values[0]);
                AccY.add(sensorEvent.values[1]);
                AccZ.add(sensorEvent.values[2]);
            }
        };
        gyro = new SensorListenerRunnable(Sensor.TYPE_GYROSCOPE, sensorManager) {
            @Override
            public void run() {
                while (!stopListening){}
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                RotX.add(sensorEvent.values[0]);
                RotY.add(sensorEvent.values[1]);
                RotZ.add(sensorEvent.values[2]);
            }
        };

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACTIVITY_RECOGNITION }, 0);

            Log.d(TAG, "onCreate: Done");
    }

    public void StartButtonClicked(View view){
        Log.d(TAG, "StartButtonClicked: Function Started");

        startButton.setEnabled(false);

        AccX = new ArrayList<>();
        AccY = new ArrayList<>();
        AccZ = new ArrayList<>();
        RotX = new ArrayList<>();
        RotY = new ArrayList<>();
        RotZ = new ArrayList<>();

        timer = new CountDownTimer(preparation_time, 10) {
            @Override
            public void onTick(long l) {
                preparation_time = l;
                startButton.setText(updateTimer(preparation_time));
            }
            @Override
            public void onFinish() {
                StartListening();
            }
        }.start();

        Log.d(TAG, "StartButtonClicked: Function Finished");

    }

    public void StartListening(){
        Log.d(TAG, "StartListening: Function Started");

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE)); //vibration
        }
        accel.listen();
        gyro.listen();
        Thread accThread = new Thread(accel);
        Thread rotThread = new Thread(gyro);
        rotThread.start();
        accThread.start();

        timer = new CountDownTimer(listening_time, 10) {
            @Override
            public void onTick(long l) {
                listening_time = l;
                countdown.setText(updateTimer(listening_time));
            }

            @Override
            public void onFinish() {
                stopListening = true;
                accel.stopListening();
                gyro.stopListening();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE));
                }

                accThread.interrupt();
                rotThread.interrupt();

                preparation_time = 3000;
                listening_time = 7000;
                countdown.setText("07:00");
                startButton.setText("Start");
                startButton.setEnabled(true);


                System.out.println(AccX.size() + " " + AccY.size() + " " + AccZ.size() + " " + RotX.size() + " " + RotY.size() + " " + RotZ.size());
                System.out.println(AccX);
                System.out.println(RotX);
                finishListening();
            }
        }.start();

        Log.d(TAG, "StartListening: Function Started");
    }

    public String updateTimer(long timeLeft){
        int seconds = (int) (timeLeft/1000);
        int centiseconds = (int) (timeLeft % 1000 / 10);

        String timeLeftText = "0" + seconds;
        timeLeftText += ":";
        if (centiseconds<10) timeLeftText += "0";
        timeLeftText += centiseconds;

        return timeLeftText;
    }

    public void finishListening(){
        MIS_RFD algo = new MIS_RFD();
        String result = algo.getDetectedAction(AccX, AccY, AccZ, RotX, RotY, RotZ);
        action.setText(result);
    }
}