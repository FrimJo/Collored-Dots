package com.fredrikux.collordotts.models;

import android.graphics.PointF;

public class Dot extends PointF{

    private static int ID = 0;

    public final int id = ID++;
    public float vx = .0f;
    public float vy = .0f;
    public float ox;
    public float oy;
    public int color;
    public float size;
    public float speed;
    private boolean remove = false;

    public Dot(final PointF position, final PointF velocity, final int color,
               final float size, final float speed){
        super(position.x, position.y);
        this.ox = super.x;
        this.oy = super.y;
        this.vx = velocity.x;
        this.vy = velocity.y;
        this.color = color;
        this.size = size;
        this.speed = speed;
    }

    public void update(double now){

        this.x += vx * speed;
        this.y += vy * speed;

    }

    public void setSize(float size){
        this.size = size;
    }

    public void flagForRemoval(){
        remove = true;
    }

    public boolean isFlaggedForRemoval(){
        return remove;
    }
}
