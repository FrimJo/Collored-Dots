package com.fredrikux.collordotts.models;

public interface GameRules {

    // Rules for the small dots
    float   SMALL_DOT_SPEED_RATIO = 0.02f;
    float   SMALL_DOT_SIZE_RATIO = 0.0625f;
    long    SMALL_DOT_CREATE_INTERVAL = 4000000000l;

    // Rules for the point dots
    float   POINT_DOT_SIZE_RATIO = 0.125f;
    int     POINT_DOT_VALUE = 5;
    long    POINT_DOT_ANIMATION_TIME = 300000000l;
    int     POINT_DOT_TIME_BETWEEN = 5000;

    // Rules for the player dot
    float   PLAYER_DOT_SPEED_RATIO = 0.375f;
    float   PLAYER_DOT_SIZE_RATIO = 0.125f;
    float   PLAYER_DOT_SIZE_INCREMENT_SMALL = 0.008f;
    float   PLAYER_DOT_SIZE_INCREMENT_POINT = 0.02f;

    // Sensor settings
    boolean SHOULD_CALIBRATE_SENSOR = false;

    // Other settings
    long    MAX_TIME_DIFFICULTY = 300000000000l;
    int     DOT_LIMIT = 200;
}
