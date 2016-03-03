package com.fredrikux.collordotts.models;

import com.fredrikux.collordotts.utils.Animation;

public abstract class SizeAnimation extends Animation {

    private float mStartSize;
    private float mGoalSize;

    public abstract void updateSize(float size);

    @Override
    protected void setFromToValues(Object from, Object to) {
        mStartSize = (float) from;
        mGoalSize = (float) to - mStartSize;
    }

    @Override
    public void updateAnimation(double now){

        double percent = getPercentage(now);

        // Set the size of the dot to a percent of the goal size
        float size = (float) (mStartSize + mGoalSize * percent);
        updateSize(size);

        // If the full percentage of the animation is done, stop animate.
        if(percent >= 1.0){
            stopAnimation();
        }
    }

}
