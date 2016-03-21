package com.fredrikux.unitedcolors.models;

import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.SoundPool;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fredrikux.unitedcolors.views.OpenGLActivity;
import com.fredrikux.unitedcolors.opengl.GLRenderer;
import com.fredrikux.unitedcolors.utils.IActionListener;
import com.fredrikux.unitedcolors.utils.IActionListener.ActionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This is the main core and class of the model. It contains all information
 * of the game.
 */
public class GameManager
        implements
            GameRules,
            Parcelable {

    /*
     * STATIC FINAL FIELDS
     */
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
    public static final int STATE_KIOSK_MODE = 14;
    public static final int STATE_PAUSED = 15;

    private static final int SIZE_OF_POSITION = 2;
    private static final int SIZE_OF_COLOR = 3;
    private static final int SIZE_OF_DOT_SIZE = 1;


    /*
     * FINAL FIELDS
     */
    private final Random RANDOM = new Random(System.currentTimeMillis());

    private final float[] positions = new float[DOT_LIMIT * SIZE_OF_POSITION];
    private final float[] colors = new float[DOT_LIMIT * SIZE_OF_COLOR];
    private final float[] sizes = new float[DOT_LIMIT * SIZE_OF_DOT_SIZE];

    private final List<Dot> drawList = new ArrayList<>(DOT_LIMIT);

    private final IOrientationChangeListener ocl = new IOrientationChangeListener() {
        public void onUpdate(float x, float y) {
            mPlayerDot.updateOrientation(x, y);
        }

        public void onAccuraryChanged(int accuracy) {
            mPlayerDot.updateAccuracy(accuracy);
        }
    };

    /*
     * OTHER FIELDS
     */
    private IOrientationSensorListener mSensorListener;
    private IActionListener listener;
    private Thread gameLoop;
    private PlayerDot mPlayerDot;
    private PointDot mPointDot;
    private SoundPool mSoundPool;
    private boolean running = false;
    private boolean paused = false;
    private boolean mKioskMode = false;
    private double mPointStep = System.nanoTime();
    private long mDotCreateStep;
    private int score = 0;
    private int gameState = STATE_KIOSK_MODE;
    private int mColorCounter = RANDOM.nextInt(colorAlt.length);
    private int mSoundBlopId;
    private int mSoundJumpId;

    /**
     * Creates a GameManager-object.
     */
    public GameManager(){

        // Create the player dot.
        mPlayerDot = spawnPlayerDot(new PointF(GLRenderer.screenW / 2.0f,
                GLRenderer.screenH / 2.0f),
                PLAYER_DOT_SIZE_RATIO*OpenGLActivity.screenDensity);

    }

    /**
     * Create a GameManager-object from saved parcel.
     *
     * @param in the saved parcel.
     */
    protected GameManager(Parcel in) {

        Dot[] array  = (Dot[]) in.readParcelableArray(Dot.class.getClassLoader());
        drawList.addAll(Arrays.asList(array));
        score = in.readInt();
        mPlayerDot = in.readParcelable(PlayerDot.class.getClassLoader());
        mPointDot = in.readParcelable(PointDot.class.getClassLoader());
        paused = in.readInt() == 1;
        gameState = in.readInt();
        mPlayerDot = spawnPlayerDot(new PointF(GLRenderer.screenW / 2.0f,
                GLRenderer.screenH / 2.0f)
                , PLAYER_DOT_SIZE_RATIO * OpenGLActivity.screenDensity);

    }

    /**
     * Starts a new game. Can be started in kiosk mode for view purpuses only.
     *
     * @param kioskMode tru for kiosk mode.
     */
    public void startNewGame(final boolean kioskMode){

        // Save the kiosk mode.
        mKioskMode = kioskMode;

        // If we don't have started the game loop or it isn't alive
        if(gameLoop == null || !gameLoop.isAlive()) {

            // Start the game gameLoop, with provided parameter for kiosk mode
            gameLoop = new Thread(new Runnable() {
                @Override
                public void run() {
                    gameLoop(kioskMode);
                }
            }, "Game Loop");

            // Set it up terminate when all other non-deamon thread stops
            gameLoop.setDaemon(true);

            // Start it
            gameLoop.start();

        }

        // Else if there exists a game loop annd it's dead
        else {

            // Destroy this current game loop.
            destroy();

            // If game loop still is alive throw exception
            if(gameLoop.isAlive()){
                throw new RuntimeException("Game gameLoop is still " +
                        "running!");
            }

            // Try to start the game again.
            startNewGame(kioskMode);
        }
    }

    /**
     * This function is to be run within a game loop.
     * @param kioskMode if the game loop should run in kiosk mode or not.
     */
    private void gameLoop(boolean kioskMode) {

        // Always clear the dot list for a new game
        drawList.clear();

        // If the game loop shouldn't start in kiosk mode
        if(!kioskMode) {

            // Re add the player dot
            drawList.add(mPlayerDot);

            // Reset player dot
            mPlayerDot.x = GLRenderer.screenW / 2.0f;
            mPlayerDot.y = GLRenderer.screenH / 2.0f;
            mPlayerDot.setSize(PLAYER_DOT_SIZE_RATIO * OpenGLActivity.screenDensity);

            setGameState(STATE_RUNNING);
        }

        // Else if in kiosk mode.
        else {

            setGameState(STATE_KIOSK_MODE);
        }

        // Clear the point dot
        mPointDot = null;

        // Reset the score
        score = 0;

        double mLastUpdateTime = System.nanoTime();
        double lastRenderTime;

        long mTimeStep = 0l;
        mDotCreateStep = 0l;
        running = true;

        while(running) {

            double now = System.nanoTime();
            int updateCount = 0;

            synchronized (gameLoop){
                try {

                    // While game is paused
                    while (paused) {

                        // Wait for unpause
                        gameLoop.wait();

                    }
                } catch (InterruptedException ignored) {}
            }

            /*
             * Do as many game updates as we need to, potentially playing
             * catchup.
             */
            while ( now - mLastUpdateTime > TIME_BETWEEN_UPDATES &&
                    updateCount < MAX_UPDATES_BEFORE_RENDER){

                // Update the game world
                updateGame(mTimeStep++);

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
            synchronized (lock){

                performAction(ACTION_STEP, null, "Interpolation (float)");

                // Wait for the draw thread to draw
                while (!condition && running){
                    try {
                        lock.wait(500);
                    } catch (InterruptedException ignore) {}
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
        }
        Log.d("GameManager", "Loop died");

    }

    /**
     * Spawns a static point dot with provided size and with value.
     *
     * @param timeStep in which time step the game loop is in.
     * @param size the size of the small dot.
     * @param value the worth value which the player receives for consuming
     *              the dot.
     * @return a PointDot-object
     */
    public PointDot spawnPointDot(final long timeStep, final float size,
                                  final int value){

        float x = RANDOM.nextInt(GLRenderer.screenW - (int) size*2) + size;
        float y = RANDOM.nextInt(GLRenderer.screenH - (int) size*2) + size;

        // Check to see that the point dot doesn't spawn on the player dot
        if( isPointOnDot(mPlayerDot, new PointF(x, y), size*2.0f) ){

            return spawnPointDot(timeStep, size, value);
        }

        PointDot pDot = new PointDot(timeStep, new PointF(x, y), mPlayerDot
                .color, size,
                value);
        mSoundPool.play(mSoundJumpId, 1, 1, 1, 0, 1);

        // Add the dot to the draw list
        addDotToDrawList(pDot);

        return pDot;
    }

    /**
     * Spawn the player dot, can only be one.
     *
     * @param position the position of the player dot.
     * @param size the size of the player dot
     * @return a PlayerDot-object.
     */
    public PlayerDot spawnPlayerDot(final PointF position, final float size){

        // Get a random color
        int index = RANDOM.nextInt(colorAlt.length);

        // Create a PlayerDot-object
        PlayerDot playerDot = new PlayerDot(position, colorAlt[index],
                size, PLAYER_DOT_SPEED_RATIO*OpenGLActivity.screenDensity);

        // Add the dot to the draw list
        addDotToDrawList(playerDot);

        return playerDot;

    }

    /**
     * Create a point dot which spawn at the edge of the screen, moving in to
     * the screen.
     *
     * @param timeStep the game loops time step.
     */
    private void spawnRandomDot(long timeStep) {

        // Get the spawn time step
        double dif = getSpawnWaitTime(timeStep);

        /*
         * If there has passed enough time steps from last random dot spawn and
         * we are eater not in kiosk mode or there exists no more than 10
         * random dots.
         */

        if(timeStep - mDotCreateStep > dif
                && (!mKioskMode || drawList.size() < 10)){

            // Save current time step
            mDotCreateStep = timeStep;

            // Create a random dot.
            createRandomDot(SMALL_DOT_SIZE_RATIO * OpenGLActivity.screenDensity,
                    GLRenderer.screenW,
                    GLRenderer.screenH);

        }
    }

    /**
     * Creates the random dot usin som math sp calculate start position and
     * direction of the dot.
     *
     * @param size the size of the dot to create.
     * @param width the maximum with in where the dot can sapwn.
     * @param height the maximum height in where the dot can spaw.
     */
    public void createRandomDot(final float size,
                                final int width, final int height){

        // Default values for position and direction
        float maxX = width - size;
        float maxY = height - size;
        float y, x = y =  (float) Math.ceil(size/2.0);
        float k, vy, vx = vy = 1.0f;
        float halfY = maxY/2.0f + size/2.0f;
        float halfX = maxX/2.0f + size/2.0f;

        // Four sides, switch 0-3
        switch (RANDOM.nextInt(4)){

            // Left
            case 0:
                y = RANDOM.nextFloat() * maxY + size;
                k = y/halfY - 1.0f;
                vx = Math.abs(k);
                vy = RANDOM.nextFloat() - k;
                break;

            // Right
            case 1:
                x = (float) Math.floor(maxX + size/2.0f);
                y = RANDOM.nextFloat() * maxY + size;
                k = y/halfY - 1.0f;
                vx = - Math.abs(k);
                vy = RANDOM.nextFloat() - k;
                break;

            // Top
            case 2:
                x = RANDOM.nextFloat() * maxX + size;
                k = x/halfX - 1.0f;
                vx = RANDOM.nextFloat() - k;
                vy = Math.abs(k);
                break;

            // Bottom
            case 3:
                x = RANDOM.nextFloat() * maxX + size;
                y = (float) Math.floor(maxY + size/2.0f);
                k = x/halfX - 1.0f;
                vx = RANDOM.nextFloat() - k;
                vy = - Math.abs(k);
                break;
        }

        // Create the dots position
        PointF position = new PointF(x, y);

        // Normalize the vector
        double normal = Math.sqrt(Math.pow(vx, 2.0) + Math.pow(vy, 2.0));
        PointF velocity = new PointF(
                vx/(float)normal,
                vy/(float)normal
        );

        // Get next color in line
        int color = colorAlt[mColorCounter++ % colorAlt.length];

        // Create the Dot-object
        Dot dot = new Dot(position, velocity, color, size,
                SMALL_DOT_SPEED_RATIO*OpenGLActivity.screenDensity);

        // Add it to the list of objects to be drawn on to the screen
        addDotToDrawList(dot);

    }

    private void spawnBigPoint(long timeStep) {

        long interval = RANDOM.nextInt( (int) POINT_DOT_TIME_BETWEEN);
        long halfInterval = POINT_DOT_TIME_BETWEEN /2;
        long randomNanoTime = (interval + halfInterval);

        // If we have an existing dot point
        if (mPointDot != null) {

            // Se if it is to be removed
            if(mPointDot.isFlaggedForRemoval()){

                // Check to see that enough time has pasted since it got removed
                if (timeStep - mPointStep > randomNanoTime) {

                    // Create a new point dot
                    mPointDot = spawnPointDot(
                            timeStep,
                            POINT_DOT_SIZE_RATIO*OpenGLActivity.screenDensity,
                            POINT_DOT_VALUE
                    );
                    mPointStep = timeStep;
                }
            }

            // Else push the point timer forward
            else {
                mPointStep = timeStep;
            }

        }

        // Else if enough time has past since game start
        else if (timeStep > randomNanoTime) {

            // Create a mew point dot
            mPointDot = spawnPointDot(timeStep,
                    POINT_DOT_SIZE_RATIO*OpenGLActivity.screenDensity,
                    POINT_DOT_VALUE);
            mPointStep = timeStep;
        }

    }

    /**
     * Checks to see if a specific point has collided with a coordinate.
     *
     * @param d1 the dot to check.
     * @param point the point to see if the dot has collided with.
     * @param size the diameter around the {@code point} in which to return
     *             true for collision.
     * @return tru of the dot touches the point, false otherwise
     */
    public boolean isPointOnDot(Dot d1, PointF point, float size){


        PointF p = new PointF(
                d1.x - point.x,
                d1.y - point.y
        );

        /* Without using coustfull squar root functions, check to see if the
         * distance between the point and the dot is smaller the or equal to
         * zero.
         */
        double distance = Math.pow(p.x, 2.0) + Math.pow(p.y, 2.0)
                - Math.pow(d1.getSize() + size, 2.0)/4.0;

        return distance <= 0;
    }


    // A lock for the game loop
    public final Object lock = new Object();
    public boolean condition = false;

    private void updateGame(final long timeStep){

        spawnRandomDot(timeStep);
        spawnBigPoint(timeStep);

        Dot dot;
        for(int i = 0; i < drawList.size(); i++){
            dot = drawList.get(i);

            // First check to see of the dot is flagged for removal
            if(dot.isFlaggedForRemoval()){
                removeDot(dot);
                i--;

            } else {

                // Update the dot with new position etc.
                dot.update(timeStep);

                boolean[] flags = GameManager.isPositionOnScreen(dot, dot.getSize());
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
                        pointDot.flagForForceRemoval(timeStep);

                    } else if (dot instanceof Dot) {
                        dot.flagForRemoval(timeStep);
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

    /**
     * Removes a dot from the dot draw list.
     * @param dot the dot to remove.
     */
    public void removeDot(Dot dot){
        drawList.remove(dot);
    }

    /**
     * Checks to see if the player dot has collide with any of the other dots.
     *
     * @param playerDot the dot controlled by the player
     * @param dot the dot to check if collided.
     * @return true for collision, false otherwise
     */
    private boolean didCollide(final PlayerDot playerDot, final Dot dot){

        /*
         * If we are in kiosk mode or we'r comparing the player dot with it
         * self, return false
         */
        if(mKioskMode || playerDot.id == dot.id) {
            return false;
        }

        // If collision occurred
        if(isPointOnDot(playerDot, new PointF(dot.x, dot.y), dot.getSize())){

            // If the player collided with a point dot
            if(dot instanceof PointDot){

                // Increment the score
                performAction(ACTION_SCORE_CHANGED,
                        score += 5,
                        "Score Updated");

                // Switch color on model
                switchColorOnDot(playerDot);

            }

            // If color is equal to the color of player
            else if(playerDot.color == dot.color){
                performAction(ACTION_SCORE_CHANGED, ++score, "Score " +
                        "Updated");
            }

            // Collision occurred wrong color
            else {

                // End the game
                gameOver();
            }

            // Play a sound for collection a dot
            mSoundPool.play(mSoundBlopId, 1, 1, 1, 0, 1);
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

    /**
     * Checks to se if provided point and size is outside the screen.
     * @param p the pint.
     * @param size the size of the point.
     * @return a boolean array containing tru or false for each of the
     * four sides. <br>
     *      boolean[0] -> left side of the screen.<br>
     *      boolean[1] -> top side of the screen.<br>
     *      boolean[2] -> right side of the screen.<br>
     *      boolean[3] -> bottom side of the screen.<br>
     *      boolean[4] -> is true if all above is false.<br>
     */
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

    private void addDotToDrawList(final Dot dot){

        // If the list is full, don't add any more.
        if (drawList.size() < DOT_LIMIT){
            drawList.add(dot);
        }
    }

    private void setGameState(int state){
        gameState = state;
        performAction(ACTION_STATE_CHANGED, state, "Game state changed.");
    }

    /**
     * Sets up the senors to use in this applikatione.
     *
     * @param manager the sensor manager to use
     * @throws NoSensorException is thrown if none of the required senors are
     * available.
     */
    public void setUpSensor(SensorManager manager) throws NoSensorException {

        // Does this device support gyro
        boolean hasGyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;

        // Does this device support rotaion vector
        boolean hasRotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                != null;

        if(hasRotation) {
            mSensorListener = new RotationOrientationSensorListener(manager);
        } else if ( hasGyro ){
            mSensorListener = new GyroOrientationSensorListener(manager);
        } else {
            throw new NoSensorException("No suitable sensor found");
        }

        mSensorListener.setOrientationChangeListener(ocl);

        mSensorListener.registerListener();

    }

    /**
     * Set the sound pool to use in this Game World
     * @param soundPool the soud pool to use.
     * @param blopId the sound consuming a dot.
     * @param jumpId the sound of big point spawn.
     */
    public void setSoundPool(SoundPool soundPool, int blopId, int jumpId) {
        mSoundPool = soundPool;
        mSoundBlopId = blopId;
        mSoundJumpId = jumpId;
    }

    /**
     * Getches the time until next random small dot should spawn.
     *
     * @param timeStep the game loops current time stamp.
     * @return the time to wait.
     */
    private double getSpawnWaitTime(long timeStep) {

        if(mKioskMode){
            return SMALL_DOT_CREATE_INTERVAL;
        }

        double percent = (double) timeStep / (double)
                MAX_TIME_DIFFICULTY;

        percent = percent > 1.0? 1.0 : percent;

        double time = SMALL_DOT_CREATE_INTERVAL * (1.0 - percent );

        // Cap the output to zero at minimum
        return time;

    }

    /**
     * Fetches the current game state.
     * @return
     */
    public int getGameState(){
        return gameState;
    }

    /**
     * Get an array of positions for all dots in thisworld.
     * @return an array of positions as floats [x, y, x, y, z, y . . .]
     */
    public float[] getPositions(){
        return positions;
    }

    /**
     * Gets an array of colors all colors for each element to draw.
     * @return an array of colors as floats [rgb, rgb, rgb . . .]
     */
    public float[] getColors(){
        return colors;
    }

    /**
     *  Gets an array of all size all elements in the draw list has.
     * @return an array of size as float [size, size, size . . .]
     */
    public float[] getSizes(){
        return sizes;
    }

    /**
     * Gets the current score.
     * @return current score.
     */
    public int getScore(){
        return score;
    }

    /**
     * Check to see if the game is all ready running.
     * @return thr running status of the game loop.
     */
    public boolean isGameRunning(){
        return running;
    }

    /**
     * Gets the number of dots currently in the draw list.
     *
     * @return the number of dots.
     */
    public int getDotCount() {
        return drawList.size();
    }

    /**
     * Sets a listener to listener for actions in this class.
     * @param listener the listener to set.
     */
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

    /**
     * The game is over, announces it to the listener.
     */
    public void gameOver(){
        destroy();
        setGameState(STATE_GAME_OVER);
    }

    /**
     * Pause the game, register the senors.
     */
    public void pause() {

        mSensorListener.unregisterListener();

        // Pause game
        if(gameLoop != null){
            synchronized (gameLoop){
                paused = true;
            }
        }
        setGameState(STATE_PAUSED);
    }

    /**
     * Resume the game, reregister the sensors.
     */
    public void unPause(){

        mSensorListener.registerListener();

        if(paused && gameLoop != null){
            synchronized (gameLoop){
                paused = false;
                gameLoop.notify();
            }
        }

        setGameState(mKioskMode? STATE_KIOSK_MODE : STATE_RUNNING);

    }

    /**
     * Checks to see if the game loop is running in kiosk mode.
     * @return
     */
    public boolean isInKioskMode(){
        return mKioskMode;
    }

    /**
     * Destroy this game loop thread.
     */
    public void destroy() {
        if(gameLoop != null){

            synchronized (gameLoop){
                running = false;
                paused = false;
                gameLoop.notify();
            }

            // If it isn't the game gameLoop thread, wait for it to end
            if ( !Thread.currentThread().equals(gameLoop) ){
                try {
                    gameLoop.join();
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
        dest.writeParcelableArray(drawList.toArray(new Parcelable[drawList.size()]),
                flags);

        dest.writeInt(score);
        dest.writeParcelable(mPlayerDot, flags);
        dest.writeParcelable(mPointDot, flags);
        dest.writeInt(paused ? 1 : 0);
        dest.writeInt(gameState);

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
}
