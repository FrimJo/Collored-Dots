package com.fredrikux.collordotts.opengl;

import android.graphics.PointF;

public class GLPointDot extends GLDot {

    public static GLPointDot theOne;

    public final int value;

    public static boolean isExists(){
        return theOne != null;
    }

    public GLPointDot(PointF position, int color, float size, int value) {
        super(position, color, new PointF(0.0f, 0.0f), size);
        this.value = value;

        if(theOne != null){
            theOne.kill();
        }

        theOne = this;
    }

    @Override
    public void kill() {
        super.kill();
        theOne = null;
    }
}
