package com.fredrikux.unitedcolors.models;

public interface GameRules {

    // Rules for the small dots
    float   SMALL_DOT_SPEED_RATIO = 0.02f;
    float   SMALL_DOT_SIZE_RATIO = 0.0625f;
    long    SMALL_DOT_CREATE_INTERVAL = 120l; //4000000000l;

    // Rules for the point dots
    float   POINT_DOT_SIZE_RATIO = 0.125f;
    int     POINT_DOT_VALUE = 5;
    long    POINT_DOT_SIZE_ANIMATION_TIME = 10l;
    long    POINT_DOT_TIME_BETWEEN = 300l; // Bigger equals longer
    long    POINT_DOT_FADE_TIME = 150l; // Bigger equals longer
    long    POINT_DOT_PULSE_SPEED = 12l; // Bigger equals slower

    // Rules for the player dot
    float   PLAYER_DOT_SPEED_RATIO = (float) (0.3f / (Math.PI/2.0f));
    float   PLAYER_DOT_SIZE_RATIO = 0.125f;
    float   PLAYER_DOT_SIZE_INCREMENT_SMALL = 0.008f;
    float   PLAYER_DOT_SIZE_INCREMENT_POINT = 0.02f;

    // Sensor settings
    boolean SHOULD_CALIBRATE_SENSOR = true;

    // Other settings
    long    MAX_TIME_DIFFICULTY = 7200l;
    int     DOT_LIMIT = 200;
}
