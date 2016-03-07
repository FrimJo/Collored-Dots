package com.fredrikux.collordotts.models;

import com.fredrikux.collordotts.utils.Animation;

public abstract class PulseAnimation extends Animation {

    private float mFrom;
    private float mTo;
    private final double mSpeed;

    public PulseAnimation(double speed){
        mSpeed = speed;
    }

    @Override
    protected void setFromToValues(Object from, Object to) {
        mFrom = (float) from;
        mTo = (float) to;
    }

    @Override
    public void updateAnimation(double now) {

        double value = Math.sin(now/mSpeed);

        value += 1.0;
        value /= 2.0;
        float diff = mTo - mFrom;

        updateSize(mFrom + diff * (float) value);
    }

    public abstract void updateSize(float size);
}
