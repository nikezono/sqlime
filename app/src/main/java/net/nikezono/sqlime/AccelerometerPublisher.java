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
    private int tiltRightTimes = 0;
    private int tiltLeftTimes = 0;
    private long lastCallTimeRight = 0;
    private long lastCallTimeLeft = 0;
    private SQLime mService;

    public AccelerometerPublisher(SQLime service) {
        mService = service;
        mSensorManager = (SensorManager)mService.getSystemService(mService.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //@DebugLog
    public void onSensorChanged(SensorEvent event) {
        //sensor value
        float x = event.values[0];

        //when tilt left
        if (x >= 2) {
            if(tiltLeftTimes == 0){
                mService.handleMoveLeft();
                lastCallTimeRight = System.currentTimeMillis();
            }

            //連続で傾けている
            if(System.currentTimeMillis() - lastCallTimeRight < 100){
                if(tiltLeftTimes > 10) {
                    //一秒以上連続で傾けている
                    mService.handleMoveLeft();
                    lastCallTimeRight = System.currentTimeMillis();
                } else {
                    tiltLeftTimes++;
                    lastCallTimeRight = System.currentTimeMillis();
                }
            }else{
                tiltLeftTimes = 0;
                return;
            }

        //when tilt right
        } else if (x <= -2) {
            if(tiltRightTimes == 0){
                mService.handleMoveRight();
                lastCallTimeRight = System.currentTimeMillis();
            }

            //連続で傾けている
            if(System.currentTimeMillis() - lastCallTimeRight < 100){
                if(tiltRightTimes > 10) {
                    //一秒以上連続で傾けている
                    mService.handleMoveRight();
                    lastCallTimeRight = System.currentTimeMillis();
                } else {
                    tiltRightTimes++;
                    lastCallTimeRight = System.currentTimeMillis();
                }
            }else{
                tiltRightTimes = 0;
                return;
            }
        }

        //@todo y方向のaccelも取って上下に動かしたいよぉ
    }

    @DebugLog
    public void onShaked(SensorEvent event){
        float x = event.values[0];
        System.out.println(event.values.length);
    }
}
