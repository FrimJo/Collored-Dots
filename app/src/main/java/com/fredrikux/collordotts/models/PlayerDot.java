package com.fredrikux.collordotts.models;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.fredrikux.collordotts.utils.Animation;

public class PlayerDot
        extends
            Dot
        implements
            SensorEventListener,
            Parcelable,
            GameRules {

    private float cX, cY;
    private long calibrateTimer = System.currentTimeMillis();
    private boolean calibrating = SHOULD_CALIBRATE_SENSOR;
    private int counter = 0;

    private final PointF mSensorPoint = new PointF();
    private float[] mRotationMatrix = new float[16];

    private Animation mSizeAnimation = new SizeAnimation() {
        @Override
        public void updateSize(float size) {
            PlayerDot.super.setSize(size);
        }
    };

    public PlayerDot(PointF position, int color, float size, float speed) {
        super(position, new PointF(.0f, .0f), color, size, speed);

    }

    protected PlayerDot(Parcel in) {
        super(in);
        cX = in.readFloat();
        cY = in.readFloat();
        calibrateTimer = in.readLong();
        counter = in.readInt();
        mSensorPoint.x = in.readFloat();
        mSensorPoint.y = in.readFloat();
        mRotationMatrix = in.createFloatArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(cX);
        dest.writeFloat(cX);
        dest.writeFloat(cY);
        dest.writeLong(calibrateTimer);
        dest.writeInt(counter);
        dest.writeParcelable(mSensorPoint, flags);
        dest.writeFloatArray(mRotationMatrix);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayerDot> CREATOR = new Creator<PlayerDot>() {
        @Override
        public PlayerDot createFromParcel(Parcel in) {
            return new PlayerDot(in);
        }

        @Override
        public PlayerDot[] newArray(int size) {
            return new PlayerDot[size];
        }
    };

    @Override
    public void update(double now) {

        if(mSizeAnimation.isAnimating()){
            mSizeAnimation.updateAnimation(now);
        }

        synchronized (mSensorPoint){

            // Update player dot position
            x += mSensorPoint.x;
            y += mSensorPoint.y;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if( event.sensor.getType() != Sensor
                .TYPE_ROTATION_VECTOR){
            return;
        }

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

        float[] orientationValues = new float[3];
        SensorManager.getOrientation(mRotationMatrix, orientationValues);

        double pitch = Math.toDegrees(orientationValues[1]);
        double roll = Math.toDegrees(orientationValues[2]);

        // Divide by 180 gives -1 to 1
        double y1 = (pitch/180.0);
        double x1 = (roll/180.0);

        float x = (float) (x1 * super.speed);
        float y = (float) -(y1 * super.speed);

        // If the device hasn't been calibrated
        if(calibrating){
            cX += x;
            cY += y;
            counter++;

            /* For 100 milliseconds gather all sensor data and
             * divide the value gathered with the number of times
             * that sensor data was provided during calibration.
             */
            if (System.currentTimeMillis() - calibrateTimer >= 100) {
                cX /= counter;
                cY /= counter;

                // We are done calibrating.
                calibrating = false;
            }
            return;
        }

        PointF sensorPoint = new PointF(x-cX, y-cY);

        // The new position
        PointF sPosition = new PointF(
                super.x + sensorPoint.x,
                super.y + sensorPoint.y
        );

        // Will the new position be on the screen?
        boolean[] flags = GameManager.isPositionOnScreen(sPosition, getSize());

        // If the new position is outside in the x-axis
        if(flags[GameManager.OUT_OF_LEFT] || flags[GameManager.OUT_OF_RIGHT]){

            // Set the senor x-data to zero.
            sensorPoint.x = 0.0f;
        }

        // If the new position is outside in the y-axis
        if(flags[GameManager.OUT_OF_TOP] || flags[GameManager.OUT_OF_BOTTOM]){

            // Set the senor y-data to zero.
            sensorPoint.y = 0.0f;
        }

        // Save the sensor data to be used in the game loop
        synchronized (mSensorPoint){
            mSensorPoint.x = sensorPoint.x;
            mSensorPoint.y = sensorPoint.y;
        }
    }

    @Override
    public void setSize(float size) {
        mSizeAnimation.startAnimation(System.nanoTime(), 100000000l, super
                .getSize(), size );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
