package com.fredrikux.collordotts.opengl;


import android.graphics.PointF;

public class GLPlayerDot extends GLDot{

    public GLPlayerDot(PointF position, int color, float size) {
        super(position, color, new PointF(0.0f, 0.0f), size);
    }

    @Override
    public void updatePosition() {

        // Do nothing on update
    }

}
