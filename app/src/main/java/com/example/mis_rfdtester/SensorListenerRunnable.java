package com.example.mis_rfdtester;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class SensorListenerRunnable implements Runnable, SensorEventListener {
    private final SensorManager sensormanager;
    private final Sensor sensor;

    public SensorListenerRunnable(int sensortype, SensorManager sm){
       sensormanager = sm;
       sensor = sensormanager.getDefaultSensor(sensortype);
    }

    @Override
    public abstract void run();

    @Override
    public abstract void onSensorChanged(SensorEvent sensorEvent);

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public void stopListening(){
        this.sensormanager.unregisterListener(this, sensor);
    }

    public void listen(){
        sensormanager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }
}
