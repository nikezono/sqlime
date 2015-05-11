package net.nikezono.sqlime;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import hugo.weaving.DebugLog;

public class AccelerometerPublisher implements SensorEventListener {
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private SQLime mService;

    public AccelerometerPublisher(SQLime service) {
        mService = service;
        mSensorManager = (SensorManager)mService.getSystemService(mService.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @DebugLog
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        if (x >= 2) {
            mService.handleMoveLeft();
        } else if (x <= -2) {
            mService.handleMoveRight();
        }

        //@todo y方向のaccelも取って上下に動かしたいよぉ
    }
}
