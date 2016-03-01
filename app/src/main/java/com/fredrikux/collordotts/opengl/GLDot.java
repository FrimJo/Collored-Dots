package com.fredrikux.collordotts.opengl;

import android.graphics.PointF;


public class GLDot {

    private static final String TAG = "GLDot";
    private static int ID = 0;

    private final PointF pos;
    private final PointF vel;
    private float size;

    public final int id = ID++;
    private int color;

    private final long mInitTime = System.currentTimeMillis();
    private final PointF mStartPos;
    private boolean alive = true;

    public GLDot(final PointF position, final int color,
                 final PointF velocity, final float size) {

        pos = position;
        vel = velocity;
        this.color = color;
        this.size = size;
        mStartPos = new PointF(pos.x, pos.y);

    }

    public void updatePosition() {
        long mElapsed = System.currentTimeMillis() - mInitTime;

        float t = (mElapsed * 0.001f);
        pos.x = mStartPos.x + vel.x*t;
        pos.y = mStartPos.y + vel.y*t;
    }

    public void incrementPosition(float x, float y) {
        pos.x += x;
        pos.y += y;
    }

    public PointF getPos() {
        return new PointF(pos.x, pos.y);
    }

    public float getSize() {
        return size;
    }

    public PointF getVel() {
        return new PointF(vel.x, vel.y);
    }

    public void setSize(final float size) {
        this.size = size;
    }

    public boolean isAlive(){
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public void setPosition(float x, float y) {
        pos.x = x;
        pos.y = y;
    }

    public int getColor(){
        return color;
    }

    public void setColor(final int color){
        this.color = color;
    }
}
