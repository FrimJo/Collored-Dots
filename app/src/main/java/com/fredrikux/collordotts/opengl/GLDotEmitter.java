package com.fredrikux.collordotts.opengl;

import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

import com.fredrikux.collordotts.utils.IActionListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GLDotEmitter {

    public static final int[] colors = {
            Color.parseColor("#E91E63"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#8BC34A")
    };

    public static final int COLLISION_POINT = 0;
    public static final int COLLISION_END_GAME = 1;
    public static final int COLLISION_BIG_POINT = 2;

    private static final int SIZE_OF_COORD = 2;
    private static final int SIZE_OF_COLOR = 3;


    private final String TAG = "GLDotEmitter";

    private final Random RANDOM = new Random(System.currentTimeMillis());

    private final String vertexShaderCode =
        "uniform mat4 u_MVPMatrix;" +
        "attribute vec4 a_Position;" +
        "attribute vec4 a_Color;" +
        "attribute float a_Size;" +
        "varying vec4 v_Color;" +
        "void main() {" +
        "  v_Color = a_Color;" +
        "    gl_Position = u_MVPMatrix * a_Position;" +
        "    gl_PointSize = a_Size;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 v_Color;" +
        "uniform sampler2D u_Texture;" +
        "void main() {" +
        "  gl_FragColor = (v_Color * texture2D(u_Texture, gl_PointCoord));" +
        "}";

    private final GLDotContainer dotList = new GLDotContainer();

    private final int mProgram;

    // Attribute handles
    private final int a_Color;
    private final int a_Position;
    private final int a_Size;

    // Uniform handles
    private final int u_MVPMatrix;
    private final int u_Texture;
    private int mTextureData;

    private FloatBuffer mPositionBuffer;
    private FloatBuffer mColorBuffer;
    private FloatBuffer mSizeBuffer;

    private IActionListener mListener;

    private GLPlayerDot mPlayerDot;


    public GLDotEmitter() {

        // Create program
        mProgram = buildProgram();

        // Attributes
        a_Color = GLES20.glGetAttribLocation(mProgram, "a_Color");
        a_Position = GLES20.glGetAttribLocation(mProgram, "a_Position");
        a_Size = GLES20.glGetAttribLocation(mProgram, "a_Size");

        // Uniforms
        u_MVPMatrix = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        u_Texture = GLES20.glGetUniformLocation(mProgram, "u_Texture");

    }

    private int buildProgram(){

        int vertexShader = GLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = GLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        int programHandle = GLES20.glCreateProgram();

        // Add the vertex shader to program
        GLES20.glAttachShader(programHandle, vertexShader);

        // Add the fragment shader to program
        GLES20.glAttachShader(programHandle, fragmentShader);

        // Creates OpenGL ES program executables
        GLES20.glLinkProgram(programHandle);

        // Check for errors
        int[] linkSuccess = new int[1];
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
                linkSuccess, 0);

        if (linkSuccess[0] == GLES20.GL_FALSE) {

            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(mProgram));

            String errorMsg = GLU.gluErrorString(GLES20.glGetError());
            Log.e(TAG, "Error: " + errorMsg);

            GLES20.glDeleteProgram(mProgram);
            throw new RuntimeException("Exception: " + errorMsg);

        }

        // Delete shaders
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programHandle;

    }

    public void createPointDot(final int maxX, final int maxY,
                               final float size, final int value){

        float x = RANDOM.nextInt(maxX - (int) size*2) + size;
        float y = RANDOM.nextInt(maxY - (int) size*2) + size;


        GLDot pDot = new GLPointDot(new PointF(x, y), mPlayerDot.getColor(), size,
                value);

        initDot(pDot);

    }

    public void createRandomDot(final float velocity,
                                 final int maxX, final int maxY,
                                 final float size){

        // Default values for position and direction
        float y, x = y =  0.0f;
        float vy, vx = vy = 1.0f;

        // For sides, switch 0-3
        switch (RANDOM.nextInt(4)){

            // Left
            case 0:
                y = RANDOM.nextInt(maxY);
                vy = (RANDOM.nextFloat()*2.0f) - 1.0f;
                break;

            // Top
            case 1:
                x = RANDOM.nextInt(maxX);
                vx = (RANDOM.nextFloat()*2.0f) - 1.0f;
                break;

            // Right
            case 2:
                x = maxX;
                y = RANDOM.nextInt(maxY);

                vx = - 1.0f;
                vy = (RANDOM.nextFloat()*2.0f) - 1.0f;
                break;

            // Bottom
            case 3:
                x = RANDOM.nextInt(maxX);
                y = maxY;

                vx = (RANDOM.nextFloat()*2.0f) - 1.0f;
                vy = - 1.0f;
                break;
        }

        PointF position = new PointF(x, y);

        double normal = Math.sqrt(Math.pow(vx, 2.0) + Math.pow(vy, 2.0));

        PointF velocityVector = new PointF(
                vx/(float)normal*velocity,
                vy/(float)normal*velocity
        );

        int index = RANDOM.nextInt(colors.length);
        int color = colors[index];

        GLDot dot = new GLDot(position, color, velocityVector, size);

        initDot(dot);

    }

    public void createPlayerDot(final PointF position, final float size){

        int index = RANDOM.nextInt(colors.length);

        mPlayerDot = new GLPlayerDot(position, colors[index], size);
        initDot(mPlayerDot);


    }

    private void initDot(final GLDot dot){

        PointF position = dot.getPos();

        mPositionBuffer.put(dotList.size()* SIZE_OF_COORD, position.x);
        mPositionBuffer.put(dotList.size() * SIZE_OF_COORD + 1, position.y);

        int color = dot.getColor();

        mColorBuffer.put(dotList.size() * SIZE_OF_COLOR,
                Color.red(color) / 255.0f);
        mColorBuffer.put(dotList.size()*SIZE_OF_COLOR + 1,
                Color.green(color) / 255.0f);
        mColorBuffer.put(dotList.size()*SIZE_OF_COLOR + 2,
                Color.blue(color) / 255.0f);

        mSizeBuffer.put(dotList.size(), dot.getSize());
        dotList.add(dot);
    }

    /**
     * Initialize this after all dots has een created.
     */
    private static final int BUFFER_SIZE = 10000;
    public void prepareBuffers( final int textureIndex ){

        mTextureData = textureIndex;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * SIZE_OF_COORD * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * SIZE_OF_COLOR * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer colorBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer sizeBuffer = byteBuffer.asFloatBuffer();

        positionBuffer.position(0);
        colorBuffer.position(0);
        sizeBuffer.position(0);

        mPositionBuffer = positionBuffer;
        mColorBuffer = colorBuffer;
        mSizeBuffer = sizeBuffer;

    }

    private void updatePositions(){

        for (int i = 0; i < dotList.size(); i++){
            GLDot dot = dotList.get(i);
            dot.updatePosition();
            PointF pos = dot.getPos();

            mPositionBuffer.put(i * SIZE_OF_COORD, pos.x);
            mPositionBuffer.put(i * SIZE_OF_COORD + 1, pos.y);

            if ( didCollide(mPlayerDot, dot) ) {

                // Remove the dot
                dot.kill();
                mSizeBuffer.put(i, 0.0f);

            }
        }
    }

    public void setListener(final IActionListener listener){
        mListener = listener;
    }

    private boolean didCollide(final GLPlayerDot pDot, final GLDot dot){

        // Don't compare same dots with it self
        if(!pDot.isAlive() || !dot.isAlive() || pDot.id == dot.id){
            return false;
        }

        PointF pos1 = pDot.getPos();
        PointF pos2 = dot.getPos();
        PointF p = new PointF(
            pos1.x - pos2.x,
            pos1.y - pos2.y
        );

        double dist_pow = Math.pow(p.x, 2.0) + Math.pow(p.y, 2.0);
        double dim = Math.pow(pDot.getSize(), 2.0) + Math.pow
                (dot.getSize(), 2.0);

        // Collision occurred
        if(dist_pow <= dim/4.0){


            if(dot instanceof GLPointDot){

                // Increment points
                mListener.onActionPerformed(new IActionListener.ActionEvent
                        (COLLISION_BIG_POINT, dot, "Collision with " +
                                "big point (GLPointDot) occurred, increment " +
                                "points"));


                // Switch color on model
                boolean done = false;
                while (!done){
                    int i = RANDOM.nextInt(colors.length);
                    if(pDot.getColor() != colors[i]){
                        done = true;
                        pDot.setColor(colors[i]);
                    }
                }

                // Switch color on view
                int c = pDot.getColor();
                int index = dotList.indexOf(pDot);

                mColorBuffer.put(index * SIZE_OF_COLOR, Color.red(c) / 255.0f);
                mColorBuffer.put(index * SIZE_OF_COLOR + 1, Color.green(c) / 255.0f);
                mColorBuffer.put(index * SIZE_OF_COLOR + 2, Color.blue(c) / 255.0f);

            }

            // If color is equal to the color of player
            else if(pDot.getColor() == dot.getColor()){

                mListener.onActionPerformed(new IActionListener.ActionEvent
                        (COLLISION_POINT, null, "Collision occurred, " +
                                "increment points"));

            } else {

                mListener.onActionPerformed(new IActionListener.ActionEvent
                        (COLLISION_END_GAME, null, "Collision occurred, game " +
                                "over"));
            }
            return true;
        }

        return false;
    }

    public void draw(float[] mMVPMatrix){

        GLES20.glUseProgram(mProgram);

        updatePositions();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glEnableVertexAttribArray(a_Position);
        GLES20.glVertexAttribPointer(
                a_Position,
                SIZE_OF_COORD,
                GLES20.GL_FLOAT,
                false,
                0,
                mPositionBuffer
        );

        GLES20.glEnableVertexAttribArray(a_Color);
        GLES20.glVertexAttribPointer(
                a_Color,
                SIZE_OF_COLOR,
                GLES20.GL_FLOAT,
                false,
                0,
                mColorBuffer
        );

        GLES20.glEnableVertexAttribArray(a_Size);
        GLES20.glVertexAttribPointer(
                a_Size,
                1,
                GLES20.GL_FLOAT,
                false,
                0,
                mSizeBuffer
        );

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureData);
        GLES20.glUniform1i(u_Texture, 0);

        GLES20.glUniformMatrix4fv(u_MVPMatrix, 1, false, mMVPMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, dotList.size());

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(a_Position);
        GLES20.glDisableVertexAttribArray(a_Color);
    }

    public GLDot getPlayerDot() {
        return mPlayerDot;
    }

    public boolean hasPointDot() {
        return GLPointDot.isExists();
    }

    public void removeBigPointDot() {

        int i = dotList.indexOf(GLPointDot.theOne);
        mSizeBuffer.put(i, 0.0f);
        GLPointDot.theOne.kill();
    }

    public void restart() {

        dotList.clear();


        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * SIZE_OF_COORD * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * SIZE_OF_COLOR * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer colorBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer sizeBuffer = byteBuffer.asFloatBuffer();

        positionBuffer.position(0);
        colorBuffer.position(0);
        sizeBuffer.position(0);

        mPositionBuffer = positionBuffer;
        mColorBuffer = colorBuffer;
        mSizeBuffer = sizeBuffer;

        initDot(mPlayerDot);
    }
}
