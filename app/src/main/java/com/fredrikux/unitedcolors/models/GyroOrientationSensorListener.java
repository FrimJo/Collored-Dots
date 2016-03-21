package com.fredrikux.unitedcolors.models;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * A senors listener which utilizes the gyro.
 */
public class GyroOrientationSensorListener implements IOrientationSensorListener {

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 1.0f;

    private final float[] deltaRotationVector = new float[4];
    private float timestamp;


    private SensorEventListener eventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular velocity of the sample
                double omegaMagnitude = Math.sqrt(axisX * axisX + axisY *
                        axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular velocity by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = (float) omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
            listener.onUpdate(deltaRotationMatrix[0], deltaRotationMatrix[1]);
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            listener.onAccuraryChanged(accuracy);
        }
    };

    private final Sensor sensor;
    private final SensorManager manager;
    private IOrientationChangeListener listener;

    public GyroOrientationSensorListener(final SensorManager m){
        manager = m;
        sensor = m.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // If the sensor can't be fetched, throw run time exception
        if(sensor == null) throw new RuntimeException("Exception geting " +
                "sensor TYPE_ROTATION_VECTOR");
    }

    @Override
    public void setOrientationChangeListener(IOrientationChangeListener l) {
        listener = l;
    }

    @Override
    public void registerListener() {

        manager.registerListener(eventListener, sensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void unregisterListener() {
        manager.unregisterListener(eventListener, sensor);
    }
}
