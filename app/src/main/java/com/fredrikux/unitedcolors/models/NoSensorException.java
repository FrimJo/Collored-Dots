package com.fredrikux.unitedcolors.models;

/**
 * A sensor exception which indicates that no sensor is available.
 */
public class NoSensorException extends Exception {
    public NoSensorException(String s) {
        super(s);
    }
}
