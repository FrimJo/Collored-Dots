package com.fredrikux.collordotts.models;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

public class Dot
        extends
            PointF
        implements
            Parcelable{

    private static int ID = 0;

    public final int id = ID++;
    public float vx = .0f;
    public float vy = .0f;
    public int color;
    private float size;
    public float speed;
    private boolean remove = false;

    public Dot(final PointF position, final PointF velocity, final int color,
               final float size, final float speed){
        super(position.x, position.y);
        this.vx = velocity.x;
        this.vy = velocity.y;
        this.color = color;
        this.size = size;
        this.speed = speed;
    }

    protected Dot(Parcel in) {
        super.readFromParcel(in);
        vx = in.readFloat();
        vy = in.readFloat();
        color = in.readInt();
        size = in.readFloat();
        speed = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(y);
        dest.writeFloat(vx);
        dest.writeFloat(vy);
        dest.writeInt(color);
        dest.writeFloat(size);
        dest.writeFloat(speed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Dot> CREATOR = new Creator<Dot>() {
        @Override
        public Dot createFromParcel(Parcel in) {
            return new Dot(in);
        }

        @Override
        public Dot[] newArray(int size) {
            return new Dot[size];
        }
    };

    public void update(double now){

        this.x += vx * speed;
        this.y += vy * speed;

    }

    public void setSize(float size){

        this.size = size;
    }

    public float getSize(){
        return size;
    }

    public void flagForRemoval(){
        remove = true;
    }

    public boolean isFlaggedForRemoval(){
        return remove;
    }
}
