package com.fredrikux.collordotts.models;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import com.fredrikux.collordotts.utils.Animation;
import com.fredrikux.collordotts.utils.IActionListener;

public class PointDot
        extends
            Dot
        implements
            GameRules,
            Parcelable {

    public final int value;
    private final long mCreateTime = System.nanoTime();
    private final Animation mSizeAnimation = new SizeAnimation() {

        @Override
        public void updateSize(float size) {
            setSize(size);
        }
    };

    private final Animation mPulseAnimation = new PulseAnimation(200000000.0) {
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

    public PointDot(PointF position, int color, float size, int value) {
        super(position, new PointF(0.0f, 0.0f), color, 0.0f, 0.0f);
        this.value = value;

        // Start a zoom-in-animation
        mSizeAnimation.startAnimation(mCreateTime, POINT_DOT_ANIMATION_TIME,
                0.0f, size);
        mSizeAnimation.setCompletionHandler(pulseAnimationHandler);

    }

    protected PointDot(Parcel in) {
        super(in);
        value = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(value);
        dest.writeLong(mCreateTime);
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

    @Override
    public void update(double now) {
        super.update(now);

        // If the dot is animating
        if(mSizeAnimation.isAnimating()){

            // Update the animation
            mSizeAnimation.updateAnimation(now);
        }else if (mPulseAnimation.isAnimating()){

            mPulseAnimation.updateAnimation(now);
        }

        /*
         * If five seconds has passed since created and the dot isn't flaged
         * for removal, flag for removal.
         */
        if(now - mCreateTime >= 5000000000l && !isFlaggedForRemoval()){

            // Flag dot for removal
            flagForRemoval();
        }
    }

    @Override
    public void flagForRemoval() {

        if(mPulseAnimation.isAnimating()){
            mPulseAnimation.stopAnimation();
        }

        // If the dot isn't already animating
        if(!mSizeAnimation.isAnimating()) {

            // Start zoom-out-animation
            mSizeAnimation.startAnimation(System.nanoTime(),
                    POINT_DOT_ANIMATION_TIME, getSize(), 0.0f);
        }

        super.flagForRemoval();
    }

    public void flagForForceRemoval() {

        // Flag for removal
        super.flagForRemoval();
    }

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
