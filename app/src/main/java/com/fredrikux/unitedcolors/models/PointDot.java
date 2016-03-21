package com.fredrikux.unitedcolors.models;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import com.fredrikux.unitedcolors.utils.Animation;
import com.fredrikux.unitedcolors.utils.IActionListener;

/**
 * This dot extends the Dot-class and is a staic dot without the movement
 * of the rgual Dot-class.
 */
public class PointDot
        extends
            Dot
        implements
            GameRules,
            Parcelable {

    public final int value;
    private final long mCreateStep;
    private final Animation mSizeAnimation = new SizeAnimation() {

        @Override
        public void updateSize(float size) {
            setSize(size);
        }
    };

    private final Animation mPulseAnimation
            = new PulseAnimation(POINT_DOT_PULSE_SPEED) {
        @Override
        public void updateSize(float size) {
            setSize(size);
        }
    };

    private IActionListener pulseAnimationHandler = new IActionListener() {
        @Override
        public void onActionPerformed(ActionEvent event) {
            if (event.action == Animation.ACTION_COMPLETE) {

                // Start the new animation
                mPulseAnimation.startAnimation(System.nanoTime(),
                        getSize(), 0.85f * getSize());

            }
        }
    };

    /**
     * Creates and returns a PointDot-object.
     *
     * @param timeStep the current time step of creation.
     * @param position the position for this dot.
     * @param color the color to use for this dot.
     * @param size the size to set for this dot-
     * @param value the value for players to receive for consuming one of these.
     */
    public PointDot(final long timeStep, PointF position, int color, float
            size, int value) {
        super(position, new PointF(0.0f, 0.0f), color, 0.0f, 0.0f);
        this.value = value;

        mCreateStep = timeStep;

        // Start a zoom-in-animation
        mSizeAnimation.startAnimation(mCreateStep, POINT_DOT_SIZE_ANIMATION_TIME,
                0.0f, size);
        mSizeAnimation.setCompletionHandler(pulseAnimationHandler);

    }

    protected PointDot(Parcel in) {
        super(in);
        value = in.readInt();
        mCreateStep = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(value);
        dest.writeLong(mCreateStep);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PointDot> CREATOR = new Creator<PointDot>() {
        @Override
        public PointDot createFromParcel(Parcel in) {
            return new PointDot(in);
        }

        @Override
        public PointDot[] newArray(int size) {
            return new PointDot[size];
        }
    };

    /**
     * Updates the point dots animations and flags it for removal is enough
     * time has past since creation.
     *
     * @param timeStep the current time step of the game loop.
     */
    @Override
    public void update(long timeStep) {

        // If the dot is animating
        if(mSizeAnimation.isAnimating()){

            // Update the animation
            mSizeAnimation.updateAnimation((long)timeStep);
        }else if (mPulseAnimation.isAnimating()){

            mPulseAnimation.updateAnimation((long)timeStep);
        }

        /*
         * If five seconds has passed since created and the dot isn't flaged
         * for removal, flag for removal. (5000000000l nano sec)
         */
        if(timeStep - mCreateStep >= POINT_DOT_FADE_TIME && !isFlaggedForRemoval()){

            // Flag dot for removal
            flagForRemoval((long) timeStep);
        }
    }

    /**
     * Flags this point dot for removal and start it's zoom-out animation.
     * @param timeStep the current game loop time step
     */
    @Override
    public void flagForRemoval(long timeStep) {

        if(mPulseAnimation.isAnimating()){
            mPulseAnimation.stopAnimation();
        }

        // If the dot isn't already animating
        if(!mSizeAnimation.isAnimating()) {

            // Start zoom-out-animation
            mSizeAnimation.startAnimation(timeStep,
                    POINT_DOT_SIZE_ANIMATION_TIME, getSize(), 0.0f);
        }

        super.flagForRemoval(timeStep);
    }

    /**
     * Flags this point dot for instant removal, without animations as in
     * {@code flagForRemoval(long)}
     * @param timeStep the time step of removal.
     */
    public void flagForForceRemoval(long timeStep) {

        // Flag for removal
        super.flagForRemoval(timeStep);
    }

    /**
     * Checks to see if this player dot is flaged for removal.
     *
     * @return tru if this player dot is flaged for removal false otherwise.
     */
    @Override
    public boolean isFlaggedForRemoval() {

        /*
         * If the point dot is animating or isn't set for removal,
         * don't flagForRemoval.
         */
        return !(mSizeAnimation.isAnimating()
                || !super.isFlaggedForRemoval());
    }
}
