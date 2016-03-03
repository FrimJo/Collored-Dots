package com.fredrikux.collordotts.models;

import android.graphics.PointF;

import com.fredrikux.collordotts.utils.Animation;

public class PointDot extends Dot implements GameRules{

    public final int value;
    private final long mCreateTime = System.nanoTime();
    private final Animation mSizeAnimation = new SizeAnimation() {

        @Override
        public void updateSize(float size) {
            setSize(size);
        }
    };

    public PointDot(PointF position, int color, float size, int value) {
        super(position, new PointF(0.0f, 0.0f), color, 0.0f, 0.0f);
        this.value = value;

        // Start a zoom-in-animation
        mSizeAnimation.startAnimation(mCreateTime, POINT_DOT_ANIMATION_TIME,
                0.0f, size);
    }

    @Override
    public void update(double now) {
        super.update(now);

        // If the dot is animating
        if(mSizeAnimation.isAnimating()){

            // Update the animation
            mSizeAnimation.updateAnimation(now);
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

        // If the dot isn't already animating
        if(!mSizeAnimation.isAnimating()) {

            // Start zoom-out-animation
            mSizeAnimation.startAnimation(System.nanoTime(),
                    POINT_DOT_ANIMATION_TIME, size, 0.0f);
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
