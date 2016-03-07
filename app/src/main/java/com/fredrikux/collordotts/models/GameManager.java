package com.fredrikux.collordotts.models;

import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.fredrikux.collordotts.opengl.GLRenderer;
import com.fredrikux.collordotts.utils.IActionListener;
import com.fredrikux.collordotts.utils.IActionListener.ActionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GameManager
        implements
            GameRules,
            Parcelable {

    private static final double GAME_HERTZ = 30.0;
    private static final double TIME_BETWEEN_UPDATES
            = 1000000000.0 / GAME_HERTZ;
    private static final int MAX_UPDATES_BEFORE_RENDER = 5;
    private static final double TARGET_FPS = 60.0;
    private static final double TARGET_TIME_BETWEEN_RENDERS
            = 1000000000 / TARGET_FPS;


    public static final int[] colorAlt = {
            Color.parseColor("#E91E63"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#8BC34A")
    };

    public static final int ACTION_SCORE_CHANGED = 1;

    public static final int ACTION_STEP = 2;
    public static final int ACTION_STATE_CHANGED = 3;
    public static final int STATE_RUNNING = 11;

    public static final int STATE_GAME_OVER = 12;
    public static final int STATE_NEW_GAME = 13;
    public static final int STATE_NOT_STARTED = 14;
    public static final int STATE_PAUSED = 15;

    private static final int SIZE_OF_POSITION = 2;
    private static final int SIZE_OF_COLOR = 3;
    private static final int SIZE_OF_DOT_SIZE = 1;

    private final Random RANDOM = new Random(System.currentTimeMillis());

    private final float[] positions = new float[DOT_LIMIT * SIZE_OF_POSITION];
    private final float[] colors = new float[DOT_LIMIT * SIZE_OF_COLOR];
    private final float[] sizes = new float[DOT_LIMIT * SIZE_OF_DOT_SIZE];

    private final List<Dot> dotList = new ArrayList<>(DOT_LIMIT);

    private IActionListener listener;
    private Thread loop;
    private PlayerDot mPlayerDot;
    private PointDot mPointDot;
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    private int score = 0;
    private boolean running = false;
    private boolean paused = false;
    private int gameState = STATE_NOT_STARTED;

    public GameManager(){
        mPlayerDot = createPlayerDot(new PointF(GLRenderer.screenW / 2.0f,
                GLRenderer.screenH / 2.0f), PLAYER_DOT_SIZE_RATIO);

    }

    protected GameManager(Parcel in) {

        Dot[] array  = (Dot[]) in.readParcelableArray(Dot.class.getClassLoader());
        dotList.addAll(Arrays.asList(array));
        score = in.readInt();
        mPlayerDot = in.readParcelable(PlayerDot.class.getClassLoader());
        mPointDot = in.readParcelable(PointDot.class.getClassLoader());
        paused = in.readInt() == 1 ? true : false;
        gameState = in.readInt();

        mPlayerDot = createPlayerDot(new PointF(GLRenderer.screenW / 2.0f,
                GLRenderer.screenH / 2.0f), PLAYER_DOT_SIZE_RATIO);

    }

    public void startNewGame(){

        if(loop == null || !loop.isAlive()) {

            // Start the game loop
            loop = new Thread(new Runnable() {
                @Override
                public void run() {
                    gameLoop();
                }
            }, "Game Loop");
            loop.setDaemon(true);
            loop.start();

        } else {

            // Destroy this run thread.
            destroy();

            // If loop still is alive throw exception
            if(loop.isAlive()){
                throw new RuntimeException("Game loop is still " +
                        "running!");
            }

            // Try to start the game again.
            startNewGame();
        }
    }

    public static final Creator<GameManager> CREATOR = new Creator<GameManager>() {
        @Override
        public GameManager createFromParcel(Parcel in) {
            return new GameManager(in);
        }

        @Override
        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }
    };

    public PointDot createPointDot(final float size, final int value){

        float x = RANDOM.nextInt(GLRenderer.screenW - (int) size*2) + size;
        float y = RANDOM.nextInt(GLRenderer.screenH - (int) size*2) + size;

        // Check to see that the point dot doesn't get created on the player dot

        if( isPointOnDot(mPlayerDot, new PointF(x, y), size) ){

            return createPointDot(size, value);
        }

        PointDot pDot = new PointDot(new PointF(x, y), mPlayerDot.color, size,
                value);

        addDot(pDot);

        return pDot;
    }

    public boolean isPointOnDot(Dot d1, PointF point, float size){
        PointF p = new PointF(
                d1.x - point.x,
                d1.y - point.y
        );

        double distance = Math.pow(p.x, 2.0) + Math.pow(p.y, 2.0)
                - Math.pow(d1.getSize() + size, 2.0)/4.0;

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

    private double mDotCreateTimer, mPointTimer


    = mDotCreateTimer = System.nanoTime();
    private long mStartTime;
    private void gameLoop() {



        // Always clear the dot list for a new game
        dotList.clear();

        // Re add the player dot
        dotList.add(mPlayerDot);

        // Reset player dot
        mPlayerDot.x = GLRenderer.screenW / 2.0f;
        mPlayerDot.y = GLRenderer.screenH / 2.0f;
        mPlayerDot.setSize(PLAYER_DOT_SIZE_RATIO);

        // Register the senor listener
        registerSensorListener(mPlayerDot);


        // Clear the point dot
        mPointDot = null;

        // Reset the score
        score = 0;

        double mLastUpdateTime = mStartTime = System.nanoTime();
        double lastRenderTime;

        long stepCount = 0l;

        running = true;
        setGameState(STATE_RUNNING);
        while(running) {

            double now = System.nanoTime();
            int updateCount = 0;

            synchronized (loop){
                try {
                    while (paused) {

                        loop.wait();

                    }
                } catch (InterruptedException ignored) {}
            }

            /*
             * Do as many game updates as we need to, potentially playing
             * catchup.
             */
            while ( now - mLastUpdateTime > TIME_BETWEEN_UPDATES &&
                    updateCount < MAX_UPDATES_BEFORE_RENDER){

                updateGame(now);

                mLastUpdateTime += TIME_BETWEEN_UPDATES;
                updateCount++;
            }

            /*
             * If for some reason an update takes forever, we don't want
             * to do an insane number of catchups.
             */
            if ( now - mLastUpdateTime > TIME_BETWEEN_UPDATES){
                mLastUpdateTime = now - TIME_BETWEEN_UPDATES;
            }

            /*
             * Render. To do so, we need to calculate interpolation for a
             * smooth render.
             */
            float interpolation = Math.min(1.0f, (float) ((now -
                    mLastUpdateTime) / TIME_BETWEEN_UPDATES) );

            // Announce that a step has taken place
            performAction(ACTION_STEP, null, "Interpolation (float)");

            synchronized (lock){

                // Wait for the draw thread to onStop
                while (!condition && running){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {}
                }
                condition = false;
            }

            lastRenderTime = now;

            /*
             * Yield until it has benn at least the target time between
             * renders.
             */
            while ( now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS
                    && now - mLastUpdateTime < TIME_BETWEEN_UPDATES){
                Thread.yield();

                /*
                 * This stops the app from consuming all your CPU. It
                 * makes this slightly less accurate, but is worth it.
                 */
                try {Thread.sleep(1);}catch (Exception e) {}

                now = System.nanoTime();
            }
            stepCount++;
        }

        //mSensorManager.unregisterListener(mPlayerDot, mRotationSensor);
    }

    private double getSpawnWaitTime(double now) {

        long diff = (long) now - mStartTime;

        double percent = (double) diff / (double) MAX_TIME_DIFFICULTY;

        double time = SMALL_DOT_CREATE_INTERVAL * (1.0 - Math.sqrt(percent) );

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

        long interval = RANDOM.nextInt(POINT_DOT_TIME_BETWEEN);
        long halfInterval = POINT_DOT_TIME_BETWEEN /2;
        long randomNanoTime = (interval + halfInterval)*1000000l;

        // If we have an existing dot point
        if (mPointDot != null) {

            // Se if it is to be removed
            if(mPointDot.isFlaggedForRemoval()){

                // Check to see that enough time has pasted since it got removed
                if (now - mPointTimer > randomNanoTime) {

                    // Create a new point dot
                    mPointDot = createPointDot(POINT_DOT_SIZE_RATIO, POINT_DOT_VALUE);
                    mPointTimer = now;
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
            mPointDot = createPointDot(POINT_DOT_SIZE_RATIO, POINT_DOT_VALUE);
            mPointTimer = now;
        }

    }

    public Object lock = new Object();

    public boolean condition = false;
    private void updateGame(double now){

        createRandomDot(now);
        createBigPoint(now);

        Dot dot;
        for(int i = 0; i < dotList.size(); i++){
            dot = dotList.get(i);

            // First check to see of the dot is flagged for removal
            if(dot.isFlaggedForRemoval()){
                removeDot(dot);
                i--;

            } else {

                // Update the dot with new position etc.
                dot.update(now);

                boolean[] flags = GameManager.isPositionOnScreen(dot, 0);
                if(flags[GameManager.OUT_OF_LEFT] || flags[GameManager.OUT_OF_RIGHT]){

                    // Bounce dot x-axis
                    dot.vx *= -1.0f;
                }

                // If the new position is outside in the y-axis
                if(flags[GameManager.OUT_OF_TOP] || flags[GameManager.OUT_OF_BOTTOM]){

                    // Bounce dot y-axis
                    dot.vy *= -1.0f;
                }

                // If the dot isn't on screen or it has collided, flagForRemoval
                if (didCollide(mPlayerDot, dot)) {

                    if(dot instanceof PointDot){
                        PointDot pointDot = (PointDot) dot;
                        pointDot.flagForForceRemoval();

                    } else if (dot instanceof Dot) {
                        dot.flagForRemoval();
                    }

                } else {

                    // Update the float arrays
                    positions[i * SIZE_OF_POSITION] = dot.x;
                    positions[i * SIZE_OF_POSITION + 1] = dot.y;

                    colors[i * SIZE_OF_COLOR] = Color.red(dot.color) / 255.0f;
                    colors[i * SIZE_OF_COLOR + 1] = Color.green(dot.color) / 255.0f;
                    colors[i * SIZE_OF_COLOR + 2] = Color.blue(dot.color) / 255.0f;

                    sizes[i * SIZE_OF_DOT_SIZE] = dot.getSize();
                }
            }
        }

    }
    public void removeDot(Dot dot){
        dotList.remove(dot);
    }

    private boolean didCollide(final PlayerDot playerDot, final Dot dot){

        if(playerDot.id == dot.id) {
            return false;
        }

        // If collision occurred
        if(isPointOnDot(playerDot, new PointF(dot.x, dot.y), dot.getSize())){

            if(dot instanceof PointDot){

                performAction(ACTION_SCORE_CHANGED, score += 5, "Score " +
                        "Updated");

                mPlayerDot.setSize(mPlayerDot.getSize() + 7.0f);

                // Switch color on model
                switchColorOnDot(playerDot);

            }

            // If color is equal to the color of player
            else if(playerDot.color == dot.color){

                performAction(ACTION_SCORE_CHANGED, ++score, "Score " +
                        "Updated");

                mPlayerDot.setSize(mPlayerDot.getSize() + 2.0f);

            }

            // Collision occurred wrong color
            else {

                gameOver();
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

    private void addDot(final Dot dot){

        // If the list is full, don't add any more.
        if (dotList.size() < DOT_LIMIT){
            dotList.add(dot);
        }
    }

    public int getGameState(){
        return gameState;
    }

    private void setGameState(int state){
        gameState = state;
        performAction(ACTION_STATE_CHANGED, state, "Game state changed.");
    }


    public void setSensorManager(SensorManager manager){
        mSensorManager = manager;
        mRotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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

    public boolean isGameRunning(){
        return running;
    }

    public void setListener(IActionListener listener){
        this.listener = listener;
    }

    private void performAction(final int action, final Object source, final
            String message){
        if(listener != null){
            listener.onActionPerformed(new ActionEvent(action, source,
                    message));
        }
    }

    private boolean registerSensorListener(SensorEventListener eventListener) {

        // Fix for not being able to properly unregister sensor

        try {
            boolean success = mSensorManager.registerListener(eventListener, mRotationSensor,
                    SensorManager.SENSOR_DELAY_GAME);
            return success;
        }catch (NullPointerException e) {
            return  false;
        }

    }

    public int getDotCount() {
        return dotList.size();
    }

    private void gameOver(){
        destroy();
        setGameState(STATE_GAME_OVER);
    }

    public void pause() {

        // Pause game
        //mSensorManager.unregisterListener(mPlayerDot, mRotationSensor);
        if(loop != null){
            synchronized (loop){
                paused = true;
            }
        }
        setGameState(STATE_PAUSED);
    }

    public void unPause(){
        //registerSensorListener(mPlayerDot);
        if(loop != null){
            synchronized (loop){
                paused = false;
                loop.notify();
            }
        }

        setGameState(STATE_RUNNING);
    }

    public void destroy() {
        if(loop != null){

            synchronized (loop){
                running = false;
                paused = false;
                loop.notify();
            }

            // If it isn't the game loop thread, wait for it to end
            if ( !Thread.currentThread().equals(loop) ){
                try {
                    loop.join();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // Save the list of dots
        dest.writeParcelableArray(dotList.toArray(new Parcelable[dotList.size()]),
                flags);

        dest.writeInt(score);
        dest.writeParcelable(mPlayerDot, flags);
        dest.writeParcelable(mPointDot, flags);
        dest.writeInt(paused ? 1 : 0);
        dest.writeInt(gameState);

    }

}
