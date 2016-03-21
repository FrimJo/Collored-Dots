package com.fredrikux.unitedcolors.models;

import com.fredrikux.unitedcolors.utils.Animation;

/**
 * This animation class handles the changes the size once for a dot.
 */
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
    public void updateAnimation(long timeStep){

        double percent = getPercentage(timeStep);

        // Set the size of the dot to a percent of the goal size
        float size = (float) (mStartSize + mGoalSize * percent);
        updateSize(size);

    }

}
