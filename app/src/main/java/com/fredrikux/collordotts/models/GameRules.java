package com.fredrikux.collordotts.models;

public interface GameRules {

    int TIME_BETWEEN_BIG_POINT = 10000;
    float SMALL_DOT_SPEED_RATIO = 1.3f;
    float SMALL_DOT_SIZE_RATIO = 15.0f;
    float POINT_DOT_SIZE_RATIO = 30.0f;
    int POINT_DOT_VALUE = 5;
    long POINT_DOT_ANIMATION_TIME = 500000000l;
    float PLAYER_DOT_SPEED_RATIO = 50.0f;
    float PLAYER_DOT_SIZE_RATIO = 30.0f;
    long SMALL_DOT_CREATE_INTERVAL = 2000000000l;
    long MAX_TIME_DIFFICULTY = 80000000000l;
    int DOT_LIMIT = 200;
}
