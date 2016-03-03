package com.fredrikux.collordotts.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.fredrikux.collordotts.R;
import com.fredrikux.collordotts.models.GameManager;
import com.fredrikux.collordotts.utils.IActionListener;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GLRenderer";

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private final Context context;

    public static int screenW;
    public static int screenH;

    private GameManager mGameManager;
    private GLDotEmitter mDotEmitter;


    public GLRenderer(Context context){
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int dotTextureIndex = loadTexture(R.raw.white_point);
        mDotEmitter = new GLDotEmitter(dotTextureIndex);

        mDotEmitter.prepareBuffers(
                mGameManager.getPositions(),
                mGameManager.getColors(),
                mGameManager.getSizes()
        );

        // The camera is located at (0,0,-3),
        // it's looking toward (0,0,0)
        // its top is pointing along (0,1,0) aka. Y-axis.
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        // Save the width and height for later use.
        screenW = width;
        screenH = height;

        // Set up the MVPMatrix
        setUpModelViewProjectionMatrix();
    }

    private void setUpModelViewProjectionMatrix() {
        GLES20.glViewport(0, 0, screenW, screenH);
        Matrix.orthoM(mProjectionMatrix, 0, 0, -screenW, screenH, 0, -10, 10);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        // CLear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
                | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update the buffer to draw
        mDotEmitter.updateBuffers(mGameManager.getDotCount());

        // Draw dots
        mDotEmitter.draw(mMVPMatrix);

        synchronized (mGameManager.lock){
            mGameManager.condition = true;
            mGameManager.lock.notify();
        }
    }

    /**
     * Loads a texture using it's resource id.
     *
     * @param resourceId the resource id.
     * @return a int handler representing the texture.
     */
    public int loadTexture(final int resourceId) {

        // One texture
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;	// No pre-scaling

            // Read in the resource
            final Bitmap bitmap = getBitmap(resourceId);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    private Bitmap getBitmap(int resourceId){
        InputStream is = context.getResources().openRawResource(resourceId);
        Bitmap bitmap;

        try {
            bitmap = BitmapFactory.decodeStream(is);
        }
        finally {

            //Always clear and close
            try {
                is.close();
            }
            catch (IOException e) {}
        }

        return bitmap;

    }

    /**
     * A static method to load shaders.
     *
     * @param type what type of shader to load.
     * @param shaderCode the GLSL code to send to the GPU.
     * @return a int handler targeting the created shader.
     */
    public static int loadShader(int type, String shaderCode){

        int shaderHandel = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shaderHandel, shaderCode);
        GLES20.glCompileShader(shaderHandel);

        // Check for errors
        int[] linkSuccess = new int[1];
        GLES20.glGetShaderiv(shaderHandel, GLES20.GL_COMPILE_STATUS,
                linkSuccess, 0);

        if (linkSuccess[0] == GLES20.GL_FALSE) {
            Log.e(TAG, "Could not link shader: ");
            Log.e(TAG, GLES20.glGetShaderInfoLog(shaderHandel));
            String errorMsg = GLU.gluErrorString(GLES20.glGetError());
            Log.e(TAG, "Error: " + errorMsg);
            GLES20.glDeleteShader(shaderHandel);
            throw new RuntimeException("Exception: " + errorMsg);
        }

        return shaderHandel;
    }

    /**
     * Sets the game manager for the renderer.
     *
     * @param gameManager the game manager to set.
     */
    public void setGameManager(GameManager gameManager){
        mGameManager = gameManager;
    }

    /**
     * Finish this GLRenderer object.
     */
    public void finish() {
        mDotEmitter.finish();
    }

}
