package com.fredrikux.unitedcolors.models;

import com.fredrikux.unitedcolors.utils.Animation;

/**
 * A animation which uses the sinus-curv to animate a pulsating
 * dot.
 */
public abstract class PulseAnimation extends Animation {

    private float mFrom;
    private float mTo;
    private final double mSpeed;

    /**
     * Creates a PulseAnimation-object form provided parameter.
     * @param speed the speed of the animation, how fast it will pulse.
     */
    public PulseAnimation(long speed){
        mSpeed = speed;
    }

    /**
     * Sets the start and end values of the animation.
     * @param from from what value should the animation start.
     * @param to to what value is the animation headed after stared.
     */
    @Override
    protected void setFromToValues(Object from, Object to) {
        mFrom = (float) from;
        mTo = (float) to;
    }

    /**
     * Update the animation using the game loops time step.
     * @param timeStep
     */
    @Override
    public void updateAnimation(long timeStep) {

        double value = Math.sin((timeStep / mSpeed) % 2*Math.PI);

        value += 1.0;
        value /= 2.0;
        float diff = mTo - mFrom;

        updateSize(mFrom + diff * (float) value);
    }

    public abstract void updateSize(float size);
}
