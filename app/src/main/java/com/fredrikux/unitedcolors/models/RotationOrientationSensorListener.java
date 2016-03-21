package com.fredrikux.unitedcolors.models;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * A senors listener which utilizes the rotaion vector.
 */
public class RotationOrientationSensorListener implements IOrientationSensorListener {

    private float[] mRotationMatrix = new float[16];
    private float[] mOrientationValues = new float[3];

    private SensorEventListener eventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event
                    .values);
            SensorManager.getOrientation(mRotationMatrix, mOrientationValues);

            listener.onUpdate(mOrientationValues[2], -mOrientationValues[1]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            listener.onAccuraryChanged(accuracy);
        }
    };

    private final Sensor sensor;
    private final SensorManager manager;
    private IOrientationChangeListener listener;

    /**
     * Creates a orientation listener for the rotation vector listener.
     * @param m
     */
    public RotationOrientationSensorListener(final SensorManager m){
        manager = m;
        sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // If the sensor can't be fetched, throw run time exception
        if(sensor == null) throw new RuntimeException("Exception geting " +
                "sensor TYPE_ROTATION_VECTOR");
    }

    /**
     * Set the listener for on receive sensor event.
     * @param l
     */
    @Override
    public void setOrientationChangeListener(IOrientationChangeListener l) {
        listener = l;
    }

    /**
     * Register this listener with the sensor manager.
     */
    @Override
    public void registerListener() {

        manager.registerListener(eventListener, sensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Unregister this listener with the sensor manager.
     */
    @Override
    public void unregisterListener() {
        manager.unregisterListener(eventListener, sensor);
    }
}
