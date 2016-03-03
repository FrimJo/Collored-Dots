package com.fredrikux.collordotts.models;

import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.fredrikux.collordotts.opengl.GLRenderer;
import com.fredrikux.collordotts.utils.IActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class GameManager implements IActionListener, GameRules{

    private static final double GAME_HERTZ = 30.0;
    private static final double TIME_BETWEEN_UPDATES
            = 1000000000.0 / GAME_HERTZ;
    private static final int MAX_UPDATES_BEFORE_RENDER = 5;
    private static final double TARGET_FPS = 60.0;
    private static final double TARGET_TIME_BETWEEN_RENDERS
            = 1000000000 / TARGET_FPS;

    private final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int[] colorAlt = {
            Color.parseColor("#E91E63"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#8BC34A")
    };

    public static final int ACTION_SCORE_CHANGED = 1;
    public static final int ACTION_GAME_OVER = 3;
    public static final int ACTION_STEP = 4;

    private static final int SIZE_OF_POSITION = 2;
    private static final int SIZE_OF_COLOR = 3;
    private static final int SIZE_OF_DOT_SIZE = 1;
    
    private final float[] positions = new float[DOT_LIMIT * SIZE_OF_POSITION];
    private final float[] colors = new float[DOT_LIMIT * SIZE_OF_COLOR];
    private final float[] sizes = new float[DOT_LIMIT * SIZE_OF_DOT_SIZE];

    private final List<Dot> dotLis = new ArrayList<>(DOT_LIMIT);

    private int score = 0;

    private boolean running = true;
    private Thread loop;

    private PlayerDot mPlayerDot;
    private PointDot pointDot;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private boolean paused = false;

    public GameManager(SensorManager sensorManager){

        this.mSensorManager = sensorManager;

    }

    public PointDot createPointDot(final float size, final int value){

        float x = RANDOM.nextInt(GLRenderer.screenW - (int) size*2) + size;
        float y = RANDOM.nextInt(GLRenderer.screenH - (int) size*2) + size;

        // Check to see that the point dot doesn't get created on the player dot
        PointDot pDot = new PointDot(new PointF(x, y), mPlayerDot.color, size,
                value);

        if( !isDotOnDot(mPlayerDot, pDot) ){
            addDot(pDot);
            return pDot;
        }

        return null;
    }

    public boolean isDotOnDot(Dot d1, Dot d2){
        PointF p = new PointF(
                d1.x - d2.x,
                d1.y - d2.y
        );

        double distance = Math.pow(p.x, 2.0) + Math.pow(p.y, 2.0)
                - Math.pow(d1.size + d2.size, 2.0)/4.0;

        return distance <= 0;
    }

    public PlayerDot createPlayerDot(final PointF position, final float size){

        int index = RANDOM.nextInt(colorAlt.length);
        PlayerDot playerDot = new PlayerDot(position, colorAlt[index],
                size, PLAYER_DOT_SPEED_RATIO);
        addDot(playerDot);
        return playerDot;

    }

    public void createRandomDot(final float size,
                                final int maxX, final int maxY){

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

        PointF velocity = new PointF(
                vx/(float)normal,
                vy/(float)normal
        );

        int index = RANDOM.nextInt(colorAlt.length);
        int color = colorAlt[index];

        Dot dot = new Dot(position, velocity, color, size, SMALL_DOT_SPEED_RATIO);
        addDot(dot);

    }

    private void addDot(final Dot dot){

        // If the list is full, don't add any more.
        if (dotLis.size() < DOT_LIMIT){
            dotLis.add(dot);
        }
    }


    public void startGame(){

        mPlayerDot = createPlayerDot(new PointF(GLRenderer.screenW / 2.0f,
                GLRenderer
                        .screenH / 2.0f), PLAYER_DOT_SIZE_RATIO);

        // Register the listener
        boolean success = registerSensorListener(mPlayerDot);

        score = 0;
        loop = new Thread(new Runnable() {
            @Override
            public void run() {
                gameLoop();
            }
        }, "Game Loop");
        loop.setDaemon(true);
        loop.start();

    }


    private double mDotCreateTimer, mPointTimer
            = mDotCreateTimer = System.nanoTime();
    private long mStartTime;

    private void gameLoop() {

        double lastUpdateTime = mStartTime = System.nanoTime();
        double lastRenderTime;


        while(running){

            double now = System.nanoTime();
            int updateCount = 0;

            if(!paused){

                /*
                 * Do as many game updates as we need to, potentially playing
                 * catchup.
                 */
                while ( now - lastUpdateTime > TIME_BETWEEN_UPDATES &&
                        updateCount < MAX_UPDATES_BEFORE_RENDER){

                    createRandomDot(now);
                    createBigPoint(now);

                    lastUpdateTime += TIME_BETWEEN_UPDATES;
                    updateCount++;
                }

                /*
                 * If for some reason an update takes forever, we don't want
                 * to do an insane number of catchups.
                 */
                if ( now - lastUpdateTime > TIME_BETWEEN_UPDATES){
                    lastUpdateTime = now - TIME_BETWEEN_UPDATES;
                }

                /*
                 * Render. To do so, we need to calculate interpolation for a
                 * smooth render.
                 */
                float interpolation = Math.min(1.0f, (float) ((now -
                        lastUpdateTime) / TIME_BETWEEN_UPDATES) );

                updateGame(now);
                lastRenderTime = now;

                /*
                 * Yield until it has benn at least the target time between
                 * renders.
                 */
                while ( now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS
                        && now - lastUpdateTime < TIME_BETWEEN_UPDATES){
                    Thread.yield();

                    /*
                     * This stops the app from consuming all your CPU. It
                     * makes this slightly less accurate, but is worth it.
                     */
                    try {Thread.sleep(1);}catch (Exception e) {}

                    now = System.nanoTime();
                }
            }
        }
    }

    private double getSpawnWaitTime(double now) {

        long diff = (long) now - mStartTime;

        double percent = (double) diff / (double) MAX_TIME_DIFFICULTY;

        double time = SMALL_DOT_CREATE_INTERVAL * (1.0 - percent );

        // Cap the output to zero at minimum
        return time < 0.0? 0.0 : time;

    }

    private void createRandomDot(double now) {
        if(now - mDotCreateTimer > getSpawnWaitTime(now)){

            mDotCreateTimer = now;

            createRandomDot(SMALL_DOT_SIZE_RATIO, GLRenderer.screenW,
                    GLRenderer.screenH);
        }
    }

    private void createBigPoint(double now) {

        long interval = RANDOM.nextInt(TIME_BETWEEN_BIG_POINT);
        long halfInterval = TIME_BETWEEN_BIG_POINT/2;
        long randomNanoTime = (interval + halfInterval)*1000000l;

        // If we have an existing dot point
        if (pointDot != null) {

            // Se if it is to be removed
            if(pointDot.isFlaggedForRemoval()){

                // Check to see that enough time has pasted since it got removed
                if (now - mPointTimer > randomNanoTime) {

                    // Create a new point dot
                    pointDot = createPointDot(POINT_DOT_SIZE_RATIO, POINT_DOT_VALUE);
                }
            }

            // Else push the point timer forward
            else {
                mPointTimer = now;
            }

        }

        // Else if enough time has past since game start
        else if (now - mStartTime > randomNanoTime) {

            // Create a mew point dot
            pointDot = createPointDot(POINT_DOT_SIZE_RATIO, POINT_DOT_VALUE);
        }

    }

    public Object lock = new Object();
    public boolean condition = false;
    private void updateGame(double now){

        Dot dot;
        for(int i = 0; i < dotLis.size(); i++){
            dot = dotLis.get(i);

            // First check to see of the dot is flagged for removal
            if(dot.isFlaggedForRemoval()){
                removeDot(dot);
                i--;

            } else {

                // Update the dot with new position etc.
                dot.update(now);

                boolean[] flags = GameManager.isPositionOnScreen(dot, 0);

                // If the dot isn't on screen or it has collided, flagForRemoval
                if (!flags[IS_ON_SCREEN] || didCollide(mPlayerDot, dot)) {

                    if(dot instanceof PointDot){
                        PointDot pointDot = (PointDot) dot;
                        pointDot.flagForForceRemoval();

                    } else if (dot instanceof Dot) {
                        dot.flagForRemoval();
                    }

                    i--;

                } else {

                    // Update the float arrays
                    positions[i * SIZE_OF_POSITION] = dot.x;
                    positions[i * SIZE_OF_POSITION + 1] = dot.y;

                    colors[i * SIZE_OF_COLOR] = Color.red(dot.color) / 255.0f;
                    colors[i * SIZE_OF_COLOR + 1] = Color.green(dot.color) / 255.0f;
                    colors[i * SIZE_OF_COLOR + 2] = Color.blue(dot.color) / 255.0f;

                    sizes[i * SIZE_OF_DOT_SIZE] = dot.size;
                }
            }
        }

        // Announce that a step has taken place
        performAction(ACTION_STEP, null, "Interpolation (float)");

        synchronized (lock){

            // Wait for the draw thread to finish
            while (!condition){
                try {
                    lock.wait();
                } catch (InterruptedException e) {}
            }
            condition = false;
        }
    }

    public void removeDot(Dot dot){
        dotLis.remove(dot);
    }

    private boolean didCollide(final PlayerDot playerDot, final Dot dot){

        if(playerDot.id == dot.id) {
            return false;
        }

        // If collision occurred
        if(isDotOnDot(playerDot, dot)){

            if(dot instanceof PointDot){

                performAction(ACTION_SCORE_CHANGED, score += 5, "Score " +
                        "Updated");

                mPlayerDot.size += 8.0f;

                // Switch color on model
                switchColorOnDot(playerDot);

            }

            // If color is equal to the color of player
            else if(playerDot.color == dot.color){

                performAction(ACTION_SCORE_CHANGED, ++score, "Score " +
                        "Updated");

                mPlayerDot.size += 2.0f;

            }

            // Collision occurred wrong color
            else {

                stopGame();
            }
            return true;
        }

        return false;
    }

    private void switchColorOnDot(PlayerDot pDot) {
        int i;
        for(i = RANDOM.nextInt(colorAlt.length);
            pDot.color == colorAlt[i];
            i = RANDOM.nextInt(colorAlt.length)){}

        pDot.color = colorAlt[i];
    }

    public static final int OUT_OF_LEFT = 0;
    public static final int OUT_OF_TOP = 1;
    public static final int OUT_OF_RIGHT = 2;
    public static final int OUT_OF_BOTTOM = 3;
    public static final int IS_ON_SCREEN = 4;

    public static boolean[] isPositionOnScreen(PointF p, float size){

        boolean[] flags = new boolean[5];

        flags[OUT_OF_LEFT] = p.x-size/2.0f < 0;
        flags[OUT_OF_TOP] = p.y-size/2.0f < 0;
        flags[OUT_OF_RIGHT] = p.x+size/2.0f > GLRenderer.screenW;
        flags[OUT_OF_BOTTOM] = p.y+size/2.0f > GLRenderer.screenH;
        flags[IS_ON_SCREEN] = !(flags[OUT_OF_LEFT] || flags[OUT_OF_TOP]
                || flags[OUT_OF_RIGHT] || flags[OUT_OF_BOTTOM]);

        return flags;
    }

    public float[] getPositions(){
        return positions;
    }

    public float[] getColors(){
        return colors;
    }

    public float[] getSizes(){
        return sizes;
    }

    public int getScore(){
        return score;
    }

    public void stopGame(){
        performAction(ACTION_GAME_OVER, score, "The game is over, score (int)");
        running = false;
    }

    private void performAction(final int action, final Object source, final
            String message){
        onActionPerformed(new ActionEvent(action, source, message));
    }

    private boolean registerSensorListener(SensorEventListener eventListener) {

        return mSensorManager.registerListener(eventListener,
                mSensorManager
                        .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);
    }

    public int getDotCount() {
        return dotLis.size();
    }

    public void onStop(){
        dotLis.clear();
        running = false;
    }

    public void onPause() {
        mSensorManager.unregisterListener(mSensorEventListener);

        synchronized (loop){
            try {
                while(paused){
                    loop.wait();
                }
            } catch (InterruptedException e) {}
        }
    }

    public void onResume(){
        paused = false;
        registerSensorListener(mPlayerDot);
        if(loop != null && loop.isAlive()){
            synchronized (loop){
                loop.notify();
            }
        }
    }

}
