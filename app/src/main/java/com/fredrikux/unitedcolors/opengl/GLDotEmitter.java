package com.fredrikux.unitedcolors.opengl;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * The main class for drawing dots, using Open GL ES 2.0
 */
public class GLDotEmitter {

    private static final int SIZE_OF_COORD = 2;
    private static final int SIZE_OF_COLOR = 3;

    private final String TAG = "GLDotEmitter";

    private final String vertexShaderCode =
        "uniform mat4 u_MVPMatrix;" +
        "attribute vec4 a_Position;" +
        "attribute vec4 a_Color;" +
        "attribute float a_Size;" +
        "varying vec4 v_Color;" +
        "void main() {" +
        "  v_Color = a_Color;" +
        "  gl_Position = u_MVPMatrix * a_Position;" +
        "  gl_PointSize = a_Size;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 v_Color;" +
        "uniform sampler2D u_Texture;" +
        "uniform sampler2D u_GlareTexture;" +
        "void main() {" +
        "  gl_FragColor = (v_Color * texture2D(u_Texture, gl_PointCoord))" +
                "+ texture2D(u_GlareTexture, gl_PointCoord);" +
        "}";

    private final int mProgram;

    // Attribute handles
    private final int a_Color;
    private final int a_Position;
    private final int a_Size;

    // Uniform handles
    private final int u_MVPMatrix;
    private final int u_Texture;
    private final int u_GlareTexture;
    private int mTextureData;
    private int mGlareTextureData;

    private FloatBuffer mPositionBuffer;
    private FloatBuffer mColorBuffer;
    private FloatBuffer mSizeBuffer;
    private int mDotCount;


    public GLDotEmitter(final int textureIndex, final int glareTextureIndex) {

        mTextureData = textureIndex;
        mGlareTextureData = glareTextureIndex;

        // Create program
        mProgram = buildProgram();

        // Attributes
        a_Color = GLES20.glGetAttribLocation(mProgram, "a_Color");
        a_Position = GLES20.glGetAttribLocation(mProgram, "a_Position");
        a_Size = GLES20.glGetAttribLocation(mProgram, "a_Size");

        // Uniforms
        u_MVPMatrix = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        u_Texture = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        u_GlareTexture = GLES20.glGetUniformLocation(mProgram, "u_GlareTexture");

    }

    /**
     * Builds the program which is used for drawing dots.
     *
     * @return a handle to the program
     */
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

    float[] mFloatPositions;
    float[] mFloatColors;
    float[] mFloatSizes;

    /**
     * Initialize this after all dots has been created.
     */
    public void prepareBuffers( float[] positions, float[] colors,
                                float[] sizes ){

        mFloatPositions = positions;
        mFloatColors = colors;
        mFloatSizes = sizes;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(positions.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(colors.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer colorBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(sizes.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer sizeBuffer = byteBuffer.asFloatBuffer();

        positionBuffer.position(0);
        colorBuffer.position(0);
        sizeBuffer.position(0);

        mPositionBuffer = positionBuffer;
        mColorBuffer = colorBuffer;
        mSizeBuffer = sizeBuffer;

    }

    /**
     * Update the buffers for later use.
     * @param count
     */
    public void updateBuffers(int count){

        mDotCount = count;

        mPositionBuffer.position(0);
        mPositionBuffer.put(mFloatPositions);
        mPositionBuffer.position(0);

        mColorBuffer.position(0);
        mColorBuffer.put(mFloatColors);
        mColorBuffer.position(0);

        mSizeBuffer.position(0);
        mSizeBuffer.put(mFloatSizes);
        mSizeBuffer.position(0);

    }

    public void draw(float[] mMVPMatrix){

        GLES20.glUseProgram(mProgram);

        // Removes the black borders for the transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20
        // .GL_ONE_MINUS_SRC_ALPHA);

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



        GLES20.glUniform1i(u_Texture, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureData);

        GLES20.glUniform1i(u_GlareTexture, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGlareTextureData);

        GLES20.glUniformMatrix4fv(u_MVPMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mDotCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(a_Position);
        GLES20.glDisableVertexAttribArray(a_Color);
        GLES20.glDisableVertexAttribArray(a_Size);
    }

    public void finish() {
        mPositionBuffer.clear();
        mColorBuffer.clear();
        mSizeBuffer.clear();
    }
}
