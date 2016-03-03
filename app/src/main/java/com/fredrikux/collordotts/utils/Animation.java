package com.fredrikux.collordotts.utils;

public abstract class Animation {

    private long mAnimationStart;
    private long mAnimationLength;
    private boolean animating = false;

    protected abstract void setFromToValues(Object from, Object to);

    public abstract void updateAnimation(double now);

    final public void startAnimation(long now, long length, Object from, Object to){
        mAnimationStart = now;
        mAnimationLength = length;
        animating = true;
        setFromToValues(from, to);
    }

    final protected void stopAnimation(){
        animating = false;
    }

    final public boolean isAnimating(){
        return animating;
    }

    final protected double getPercentage(double now){

        // Get elapsed percentage of the animation
        double elapsed = now - mAnimationStart;
        double percent = elapsed/ (double) mAnimationLength;

        //Cap percent between 0.0 and 1.0 (0% and 100%)
        percent = percent > 1.0? 1.0 : percent;
        percent = percent < 0.0? 0.0 : percent;

        return percent;
    }
}
