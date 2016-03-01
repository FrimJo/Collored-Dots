package com.fredrikux.collordotts.models;

import android.graphics.PointF;

public class Dot {

    private static int ID = 0;

    public final int id = ID++;
    public final int index;
    private final PointF position;
    private final PointF velocity;
    private int color;
    private int size;

    public Dot(final PointF position, final PointF velocity, final int color,
               final int size, final int index){
        this.index = index;
        this.position = new PointF(position.x position.y);
        this.velocity = new PointF(velocity.x velocity.y);
        this.color = color;
        this.size = size;
    }

    public int index(){
        return this.index;
    }

    public PointF position(){
        return new PointF(this.position.x, this.position.y);
    }
}
