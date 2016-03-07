package com.fredrikux.collordotts.utils;

import android.app.Notification;

public abstract class Animation {

    public static final int ACTION_COMPLETE = 1;

    private long mAnimationStart;
    private long mAnimationLength;
    private boolean animating = false;
    private IActionListener completionHandler;

    protected abstract void setFromToValues(Object from, Object to);

    public abstract void updateAnimation(double now);

    final public void startAnimation(long now, Object from, Object to){
        mAnimationStart = now;
        mAnimationLength = -1l;
        animating = true;
        setFromToValues(from, to);
    }

    final public void startAnimation(long now, long length, Object from, Object to){
        mAnimationStart = now;
        mAnimationLength = length;
        animating = true;
        setFromToValues(from, to);
    }

    final public void stopAnimation(){
        animating = false;
        performeAction(
                new IActionListener.ActionEvent(ACTION_COMPLETE, this,
                        "Animation is complete, or has stopped for other " +
                                "reasons.")
        );

    }

    final private void performeAction(IActionListener.ActionEvent event){
        if(completionHandler != null){
            completionHandler.onActionPerformed(event);
        }

    }

    final public void setCompletionHandler(IActionListener handler){
        completionHandler = handler;
    }

    final public boolean isAnimating(){
        return animating;
    }

    final protected double getPercentage(double now){

        if(mAnimationLength == -1l){
            return 0.0;
        }

        // Get elapsed percentage of the animation
        double elapsed = now - mAnimationStart;
        double percent = elapsed/ (double) mAnimationLength;

        //Cap percent between 0.0 and 1.0 (0% and 100%)
        percent = percent > 1.0? 1.0 : percent;
        percent = percent < 0.0? 0.0 : percent;

        // If the full percentage of the animation is done, stop animate.
        if(percent >= 1.0){
            stopAnimation();
        }

        return percent;
    }
}
