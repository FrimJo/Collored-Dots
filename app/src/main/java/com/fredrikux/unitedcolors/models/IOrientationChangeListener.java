package com.fredrikux.unitedcolors.models;

/**
 * A listener for the change in orientaion
 */
public interface IOrientationChangeListener {
    void onUpdate(float x, float y);
    void onAccuraryChanged(int accuracy);
}
