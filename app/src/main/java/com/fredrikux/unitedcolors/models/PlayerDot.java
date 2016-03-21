package com.fredrikux.unitedcolors.models;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents the player conntroled dot.
 */
public class PlayerDot
        extends
            Dot
        implements
            Parcelable,
            GameRules {

    private float cX, cY;
    private long calibrateTimer = System.currentTimeMillis();
    private boolean calibrating = SHOULD_CALIBRATE_SENSOR;
    private int counter = 0;

    private final PointF mSensorPoint = new PointF();
    private float[] mRotationMatrix = new float[16];


    /**
     * Returns a player dot object with provided position, coor, size and speed
     * @param position the position of the dot
     * @param color the color of the dot
     * @param size thw size of the dot
     * @param speed the speed of the dot
     */
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

    /**
     * Update this player dot by adding the senorvalues to the x- and y-axis.
     * Also, checks to see if the player dot is on screen and change the
     * x and/or y to make the player dot not able to get outside the screen.
     * @param timeStep the current time step of the game loop.
     */
    @Override
    public void update(long timeStep) {

        synchronized (mSensorPoint){

            // The new position
            PointF sPosition = new PointF(
                super.x + mSensorPoint.x,
                super.y + mSensorPoint.y
            );

            // Will the new position be on the screen?
            boolean[] flags = GameManager.isPositionOnScreen(sPosition, getSize());

            // If the new position is outside in the x-axis
            if(flags[GameManager.OUT_OF_LEFT] || flags[GameManager.OUT_OF_RIGHT]){

                // Set the senor x-data to zero.
                mSensorPoint.x = 0.0f;
            }

            // If the new position is outside in the y-axis
            if(flags[GameManager.OUT_OF_TOP] || flags[GameManager.OUT_OF_BOTTOM]){

                // Set the senor y-data to zero.
                mSensorPoint.y = 0.0f;
            }

            // Update player dot position
            x += mSensorPoint.x;
            y += mSensorPoint.y;
        }
    }

    float[] mOrientationValues = new float[3];

    @Override
    public void setSize(float size) {
        super.setSize(size);
    }

    public void updateOrientation(float x, float y) {

        // Save the sensor data to be used in the game loop
        synchronized (mSensorPoint){
            mSensorPoint.x = x * super.velocity;
            mSensorPoint.y = y * super.velocity;
        }

    }

    public void updateAccuracy(int ignore) {}
}
