package com.fredrikux.unitedcolors.models;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing a single dot, the dot contains information about its
 * position, size, velocity and direction.
 */
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
    public float velocity;
    private boolean remove = false;

    /**
     * Create a dot int preferred size, with a set direction, color, size and
     * velocity.
     *
     * @param position the position of this Dot-object.
     * @param direction the direction this dot will move.
     * @param color what color the dot should have.
     * @param size the size of the dot.
     * @param velocity the speed of the dot.
     */
    public Dot(final PointF position, final PointF direction, final int color,
               final float size, final float velocity){
        super(position.x, position.y);
        this.vx = direction.x;
        this.vy = direction.y;
        this.color = color;
        this.size = size;
        this.velocity = velocity;
    }

    protected Dot(Parcel in) {
        super.readFromParcel(in);
        vx = in.readFloat();
        vy = in.readFloat();
        color = in.readInt();
        size = in.readFloat();
        velocity = in.readFloat();
    }

    /**
     * Write this point to the specified parcel. To restore a point from
     * a parcel, use readFromParcel()
     * @param dest The parcel to write the point's coordinates into
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(y);
        dest.writeFloat(vx);
        dest.writeFloat(vy);
        dest.writeInt(color);
        dest.writeFloat(size);
        dest.writeFloat(velocity);
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

    /**
     * Update the dot with its new position.
     *
     * @param timeStep the current time step of the game loop.
     */
    public void update(long timeStep){

        this.x += vx * velocity;
        this.y += vy * velocity;

    }

    /**
     * Sets the size of the dot.
     *
     * @param size the size to set the dot to.
     */
    public void setSize(float size){

        this.size = size;
    }

    /**
     * Fetches the curretn size of this dot.
     *
     * @return the dots size.
     */
    public float getSize(){
        return size;
    }

    /**
     * Flags this dot for removal, so the game loop know when to remove this
     * dot.
     * @param timeStep the current game loop time step
     */
    public void flagForRemoval(long timeStep){
        remove = true;
    }

    /**
     * Checks to see if the dot it ready to be removed.
     *
     * @return true if the element is ready to be removed, false otherwise.
     */
    public boolean isFlaggedForRemoval(){
        return remove;
    }
}
