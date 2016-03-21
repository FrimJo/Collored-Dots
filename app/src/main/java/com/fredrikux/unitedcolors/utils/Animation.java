package com.fredrikux.unitedcolors.utils;


/**
 * This is the base abstract animation class, extending this method
 * it is easy to create your own animation.
 */
public abstract class Animation {

    public static final int ACTION_COMPLETE = 1;

    private long mAnimationStart;
    private long mAnimationLength;
    private boolean animating = false;
    private IActionListener completionHandler;

    protected abstract void setFromToValues(Object from, Object to);

    public abstract void updateAnimation(long timeStep);

    /**
     * Starts the animation for it to loop until closed.
     * @param timeStep the current game loop time stamp.
     * @param from from what value should the animation start at.
     * @param to to what value is the animation aiming for.
     */
    final public void startAnimation(long timeStep, Object from, Object to){
        startAnimation(timeStep, -1l, from, to);
    }

    /**
     * Starts the animation with a set time to end.
     * @param timeStep the current game loop time stamp.
     * @param from from what value should the animation start at.
     * @param to to what value is the animation aiming for.
     * @param length how long the animation will go on.
     */
    final public void startAnimation(long timeStep, long length, Object from, Object to){
        mAnimationStart = timeStep;
        mAnimationLength = length;
        animating = true;
        setFromToValues(from, to);
    }

    /**
     * Stops the animation.
     */
    final public void stopAnimation(){
        animating = false;
        performeAction(
                new IActionListener.ActionEvent(ACTION_COMPLETE, this,
                        "Animation is complete, or has stopped for other " +
                                "reasons.")
        );

    }

    /**
     * Notify the listener.
     * @param event the event to send.
     */
    final private void performeAction(IActionListener.ActionEvent event){
        if(completionHandler != null){
            completionHandler.onActionPerformed(event);
        }

    }

    /**
     * Set up a handler to trigger on completion.
     * @param handler
     */
    final public void setCompletionHandler(IActionListener handler){
        completionHandler = handler;
    }

    /**
     * Checks to see if the animation is currently animating.
     *
     * @return return true if the animation is running, false otherwise.
     */
    final public boolean isAnimating(){
        return animating;
    }

    /**
     * Gets the percentage of how far the animation has come. The animation
     * is automatic stopped if the percentage reaches 100%
     * @param timeStep the current time step.
     * @return the percentage as a double ranging from 0.0 to 1.0
     */
    final protected double getPercentage(long timeStep){

        if(mAnimationLength == -1l){
            return 0.0;
        }

        // Get elapsed percentage of the animation
        double elapsed = timeStep - mAnimationStart;
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
