package com.fredrikux.unitedcolors.models;

import android.hardware.SensorManager;

/**
 * A interface to goup up all sensors for orientation in to one.
 */
public interface IOrientationSensorListener {
    void setOrientationChangeListener(IOrientationChangeListener l);
    void registerListener();
    void unregisterListener();
}