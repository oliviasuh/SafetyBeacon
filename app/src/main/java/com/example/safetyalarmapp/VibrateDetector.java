package com.example.safetyalarmapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;

public class VibrateDetector implements SensorEventListener {
private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
private static final int SHAKE_TIMEOUT_MS = 300; // Time between shakes
private static final int SHAKE_COUNT_RESET_TIME_MS = 3000; // Reset count after 3 seconds

private long mVibrateTimestamp;
private int mVibrateCount = 0;
private OnShakeListener vibrateListener;
private long lastShakeTimestamp;
LocationManager locationManager;
public interface OnShakeListener {
    void onVibrate(int mVibrateCount);
}

    public VibrateDetector(OnShakeListener listener) {
        this.vibrateListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        // Calculate G-Force
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();
            if (lastShakeTimestamp + SHAKE_TIMEOUT_MS > now) {
               return;
            }
            lastShakeTimestamp = now;

            mVibrateCount++;
            if (mVibrateCount == 6){
                mVibrateCount = 0;
            }

            // Notify the listener with the current vibration count
            vibrateListener.onVibrate(mVibrateCount);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No need to implement for this example
    }
}