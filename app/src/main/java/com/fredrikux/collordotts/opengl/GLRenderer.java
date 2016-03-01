package com.fredrikux.collordotts.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    private final Random RANDOM = new Random(System.currentTimeMillis());

    private static final String TAG = "GLRenderer";
    private static final int TIME_BETWEEN_BIG_POINT = 10000;
    private static final float SMALL_DOT_SPEED_RATIO = 200.0f;
    private static final float SMALL_DOT_SIZE_RATIO = 25.0f;
    private static final float POINT_DOT_SIZE = 60.0f;
    private static final int POINT_DOT_VALUE = 5;
    private static final float PLAYER_DOT_SPEED_RATIO = 6.0f;
    private static final float PLAYER_DOT_SIZE_RATIO = 60.0f;

    private static final long INITIAL_DOT_DELAY_MILLISECONDS = 1000;
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];


    private final float[] mViewMatrix = new float[16];

    private final Context context;

    private final SensorManager mSensorManager;

    private float smallDotSpeed;
    private float smallDotSize;
    private float playerDotSize;
    private float playerDotSpeed;

    private int screenW;

    private int screenH;
    private GameManager mGameManager;
    private int mTextureIndex;
    private GLDotEmitter mDotEmitter;


    public GLRenderer(Context context){
        this.context = context;

        mSensorManager
                = (SensorManager) context.getSystemService(Context
                .SENSOR_SERVICE);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mTextureIndex = loadTexture(R.raw.white_point);
        mDotEmitter = new GLDotEmitter();
        mDotEmitter.setListener(new IActionListener() {
            @Override
            public void onActionPerformed(ActionEvent event) {
                switch (event.action) {
                    case GLDotEmitter.COLLISION_POINT:
                        mGameManager.incrementScore(1);
                        break;

                    case GLDotEmitter.COLLISION_END_GAME:
                        endGame();
                        break;

                    case GLDotEmitter.COLLISION_BIG_POINT:
                        GLPointDot dot = (GLPointDot) event.source;
                        mGameManager.incrementScore(dot.value);
                        break;
                }
            }
        });

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
        float screenR = height/width;

        smallDotSpeed = SMALL_DOT_SPEED_RATIO * screenR;
        smallDotSize = SMALL_DOT_SIZE_RATIO * screenR;
        playerDotSize = PLAYER_DOT_SIZE_RATIO * screenR;
        playerDotSpeed = PLAYER_DOT_SPEED_RATIO * screenR;

        // Set up the MVPMatrix
        setUpModelViewProjectionMatrix();

        // Create the sensor event listener
        SensorEventListener eventListener = getSensorEventListener();

        // Register the listener
        boolean success = registerSensorListener(eventListener);

        // If it wasn't success
        if(!success){

            // Throw a unchecked exception if the event listener wasnt
            // correctly registered.
//            throw new RuntimeException("Couldn't register " +
// "SensorEventListener");
        }

        mDotEmitter.prepareBuffers(mTextureIndex);

        mDotEmitter.createPlayerDot(new PointF(screenW / 2.0f,
                        screenH / 2.0f),
                playerDotSize);

    }


    private float cX, cY;
    private long calibrateTimer = System.currentTimeMillis();
    private boolean calibrating = true;
    private int counter = 0;
    private SensorEventListener getSensorEventListener() {
        return new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                if(!mGameManager.isRunning() || event.sensor.getType() != Sensor
                        .TYPE_GRAVITY){
                    return;
                }

                float x = event.values[0]*playerDotSpeed;
                float y = event.values[1]*playerDotSpeed;

                if(calibrating){
                    cX += x;
                    cY += y;
                    counter++;
                    if (System.currentTimeMillis() - calibrateTimer >= 100) {
                        cX /= counter;
                        cY /= counter;
                        calibrating = false;
                    }
                    return;
                }


                PointF point = new PointF(-(x-cX), y-cY);
                GLDot playerDot = mDotEmitter.getPlayerDot();

                PointF pPos = playerDot.getPos();

                PointF sPosition = new PointF(
                        pPos.x + point.x,
                        pPos.y + point.y
                );


                boolean[] flags = isDotOnScreen(sPosition, playerDot.getSize());

                if(flags[OUT_OF_LEFT] || flags[OUT_OF_RIGHT]){
                    point.x = 0.0f;
                }

                if(flags[OUT_OF_TOP] || flags[OUT_OF_BOTTOM]){
                    point.y = 0.0f;
                }

                playerDot.incrementPosition(point.x, point.y);

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }

    private boolean registerSensorListener(SensorEventListener eventListener) {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor
                .TYPE_GRAVITY);


        return mSensorManager.registerListener(eventListener,
                sensor, SensorManager
                        .SENSOR_DELAY_GAME);
    }

    private void setUpModelViewProjectionMatrix() {
        GLES20.glViewport(0, 0, screenW, screenH);
        Matrix.orthoM(mProjectionMatrix, 0, 0, -screenW, screenH, 0, -10, 10);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20
                .GL_DEPTH_BUFFER_BIT);

        // Draw dots
        mDotEmitter.draw(mMVPMatrix);
    }

    private static final int OUT_OF_LEFT = 0;
    private static final int OUT_OF_TOP = 1;
    private static final int OUT_OF_RIGHT = 2;
    private static final int OUT_OF_BOTTOM = 3;

    private boolean[] isDotOnScreen(PointF position, float size){

        boolean[] flags = new boolean[4];

        flags[OUT_OF_LEFT] = position.x-size/2.0f < 0;
        flags[OUT_OF_TOP] = position.y-size/2.0f < 0;
        flags[OUT_OF_RIGHT] = position.x+size/2.0f > screenW;
        flags[OUT_OF_BOTTOM] = position.y+size/2.0f > screenH;

        return flags;
    }

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

    private void endGame() {
        mGameManager.stopGame();
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
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

    public void setGameManager(GameManager gameManager){
        mGameManager = gameManager;
    }


    private long difficultyTimer, emitterTimer, pointTimer = emitterTimer
            = difficultyTimer = System.currentTimeMillis();
    private long delayTime = INITIAL_DOT_DELAY_MILLISECONDS;
    private long difficulty = 0;
    private long bigPointDotCreated;

    public void step() {

        long now = System.currentTimeMillis();

        // Every Second minus the difficulty level, create a new dot
        if(mDotEmitter != null &&  now - emitterTimer > delayTime - difficulty) {
            emitterTimer = now;
            mDotEmitter.createRandomDot(smallDotSpeed, screenW, screenH, smallDotSize);
        }

        // Every Ten Second, create a new big point dot if there isn't one
        if(mDotEmitter != null){
          if(mDotEmitter.hasPointDot()){
              pointTimer = now;

              // If five seconds has passed since created, remove dot
              if(System.currentTimeMillis() - bigPointDotCreated >= 5000){

                  // Remove dot.
                  mDotEmitter.removeBigPointDot();
              }

          }else if (now - pointTimer > (RANDOM.nextInt(TIME_BETWEEN_BIG_POINT) +
                  TIME_BETWEEN_BIG_POINT/2)){
              mDotEmitter.createPointDot(screenW, screenH, POINT_DOT_SIZE,
                      POINT_DOT_VALUE);
              bigPointDotCreated = now;
          }
        }

        // Every hundredth of a second, increase the difficulty
        if(now - difficultyTimer > 100) {
            difficultyTimer = now;
            difficulty += 1;
        }
    }

    public void restart() {
        mDotEmitter.restart();
    }
}
