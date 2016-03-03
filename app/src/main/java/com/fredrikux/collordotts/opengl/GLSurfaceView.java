package com.fredrikux.collordotts.opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.fredrikux.collordotts.models.GameManager;
import com.fredrikux.collordotts.utils.IActionListener;

public class GLSurfaceView extends android.opengl.GLSurfaceView {

    private GLRenderer mRenderer;
    private final Context context;

    public GLSurfaceView(Context context) {
        super(context);
        this.context = context;

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        // Set the renderer for drawing on the OpenGLSurfaceView
        mRenderer = new GLRenderer(context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // make sure we get key events
        setFocusable(true);

    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        // Set the renderer for drawing on the OpenGLSurfaceView
        mRenderer = new GLRenderer(context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

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


    public void onStop() {
        mRenderer.finish();
    }

}
