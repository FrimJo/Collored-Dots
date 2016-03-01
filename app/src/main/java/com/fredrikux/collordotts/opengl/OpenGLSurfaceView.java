package com.fredrikux.collordotts.opengl;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.fredrikux.collordotts.models.GameManager;

public class OpenGLSurfaceView extends GLSurfaceView {

    private GLRenderer mRenderer;
    private final Context context;

    public OpenGLSurfaceView(Context context) {
        super(context);
        this.context = context;

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        // Set the renderer for drawing on the OpenGLSurfaceView
        mRenderer = new GLRenderer(context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(OpenGLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // make sure we get key events
        setFocusable(true);

    }

    public OpenGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        // Set the renderer for drawing on the OpenGLSurfaceView
        mRenderer = new GLRenderer(context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(OpenGLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // make sure we get key events
        setFocusable(true);
    }

    public void setGameManager(final GameManager gameManager){
        mRenderer.setGameManager(gameManager);
    }

    @Override
    public void onResume(){
        super.onResume();

    }

    @Override
    public void onPause(){
        super.onPause();

    }

    @Override
    public boolean performClick(){
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public void step() {
        mRenderer.step();
    }

    public void restart() {
        mRenderer.restart();
    }
}
