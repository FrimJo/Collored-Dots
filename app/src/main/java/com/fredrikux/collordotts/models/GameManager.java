package com.fredrikux.collordotts.models;

import android.graphics.Color;
import android.graphics.PointF;

import com.fredrikux.collordotts.utils.IActionListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class GameManager implements IActionListener{

    public static final int SCORE_CHANGED = 1;
    public static final int IS_RUNNING_CHANGED = 2;
    public static final int GAME_OVER = 3;
    public static final int STEP = 4;
    public static final int RESTART = 5;

    private static final int DOT_LIMIT = 10000;
    private static final int SIZE_OF_POSITION = 2;
    private static final int SIZE_OF_COLOR = 3;
    private static final int SIZE_OF_DOT_SIZE = 1;
    private static final int SIZE_OF_VELOCITY = 2;

    private final FloatBuffer mPositionBuffer;
    private final FloatBuffer mColorBuffer;
    private final FloatBuffer mSizeBuffer;
    
    private final float[] velocities = new float[DOT_LIMIT * SIZE_OF_VELOCITY];

    private int score = 0;
    private Object scoreLock = new Object();

    private boolean running = true;
    private Object runningLock = new Object();
    private Thread gameThread;

    private long lastFpsTime = 0;

    public GameManager(){
        mPositionBuffer = initFloatBuffer(new float[DOT_LIMIT *
                SIZE_OF_POSITION]);
        mColorBuffer = initFloatBuffer(new float[DOT_LIMIT * SIZE_OF_COLOR]);
        mSizeBuffer = initFloatBuffer(new float[DOT_LIMIT * SIZE_OF_DOT_SIZE]);
    }

    public int getNumberOfDots(){
        return mPositionBuffer.position();
    }

    private FloatBuffer initFloatBuffer(final float[] data){

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.position(0);
        return floatBuffer;
    }

    public void createDot(final PointF position, final int color, final float
            size){

        mPositionBuffer.put(position.x);
        mPositionBuffer.put(position.y);

        mColorBuffer.put(Color.red(color) / 255.0f);
        mColorBuffer.put(Color.green(color) / 255.0f);
        mColorBuffer.put(Color.blue(color) / 255.0f);

        mSizeBuffer.put(size);
    }


    public void startGame(){
        score = 0;
        gameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                gameLoop();
            }
        }, "Game Loop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    public void gameLoop() {
        long lastLoopTime = System.nanoTime();
        final int TARGET_FPS = 60;
        final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;

        setRunning(true);

        // keep looping round til the game ends
        while (isRunning())
        {
            // work out how long its been since the last update, this
            // will be used to calculate how far the entities should
            // move this loop
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            lastLoopTime = now;
            //double delta = updateLength / ((double)OPTIMAL_TIME);

            // update the frame counter
            lastFpsTime += updateLength;

            for(int i = 0; i < mPositionBuffer.position()/SIZE_OF_POSITION; i++){
                float x = mPositionBuffer.get(i * SIZE_OF_POSITION)
                        + velocities[i * SIZE_OF_VELOCITY]
                        * updateLength * 0.001f;

                float y = mPositionBuffer.get(i * SIZE_OF_POSITION + 1)
                        + velocities[i * SIZE_OF_VELOCITY + 1]
                        * updateLength * 0.001f;

                mPositionBuffer.put(i * SIZE_OF_POSITION, x);
                mPositionBuffer.put(i * SIZE_OF_POSITION + 1, y);
            }

            // update our FPS counter if a second has passed since
            // we last recorded
            /*if (lastFpsTime >= 1000000000)
            {
                System.out.println("(FPS: "+fps+")");
                lastFpsTime = 0;
                fps = 0;
            }*/

            performAction(STEP, null, "A time step has taken place");


            // we want each frame to take 10 milliseconds, to do this
            // we've recorded when we started the frame. We add 10 milliseconds
            // to this and then factor in the current time to give
            // us our final value to wait for
            // remember this is in ms, whereas our lastLoopTime etc. vars are in ns.
            try{Thread.sleep( (lastLoopTime-System.nanoTime() + OPTIMAL_TIME)
                    /1000000 );} catch (InterruptedException |
                    IllegalArgumentException e) {}
        }
    }

    public int getScore(){
        synchronized (scoreLock){
            return score;
        }
    }

    public int incrementScore(int value){
        synchronized (scoreLock){
            score += value;
            performAction(SCORE_CHANGED, score, "The score (int) has changed.");
            return score;
        }
    }

    public boolean isRunning(){
        synchronized (runningLock){
            return running;
        }
    }

    public boolean toggleRunning(){
        synchronized (runningLock) {
            running = !running;
            performAction(IS_RUNNING_CHANGED, running, "The running (boolean)" +
                    " flag has changed");
            return running;
        }
    }

    public void setRunning(final boolean value){
        synchronized (runningLock){
            running = value;
        }
    }

    public void stopGame(){
        performAction(GAME_OVER, score, "The game is over, score (int)");
        synchronized (runningLock){
            running = false;
        }
    }

    public void restart(){
        startGame();
        performAction(RESTART, null, "The game has restarted.");
    }

    private void performAction(final int action, final Object source, final
            String message){
        onActionPerformed(new ActionEvent(action, source, message));
    }
}
