package com.fredrikux.unitedcolors.views;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fredrikux.unitedcolors.R;
import com.fredrikux.unitedcolors.models.GameManager;
import com.fredrikux.unitedcolors.models.NoSensorException;
import com.fredrikux.unitedcolors.models.ScoreGuard;
import com.fredrikux.unitedcolors.opengl.GLRenderer;
import com.fredrikux.unitedcolors.utils.IActionListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;

import java.io.IOException;
import java.io.InputStream;

/**
 * The core activity of the application.
 */
public class OpenGLActivity
        extends
            Activity
        implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

    /* Static fields */
    public static int screenDensity;


    /* Private final fields*/
    private final View.OnClickListener mNewGameHandler = new View
            .OnClickListener() {
        @Override
        public void onClick(View v) {mGameManager.startNewGame(false); }
    };

    private final View.OnClickListener mResumeGameHandler = new View
            .OnClickListener() {
        @Override
        public void onClick(View v) { mGameManager.unPause(); }
    };

    private View.OnClickListener mBackButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mGameManager.startNewGame(true);
        }
    };

    private final IActionListener mGameWorldListener = new IActionListener() {
        @Override
        public void onActionPerformed(final ActionEvent event) {

        // Update the UI from the UI Thread
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                switch (event.action) {
                    case GameManager.ACTION_SCORE_CHANGED:

                        int score = (int) event.source;
                        mScoreView.setText("" + score);
                        break;

                    case GameManager.ACTION_STATE_CHANGED:

                        updateUserInterface(mGameManager.getGameState());
                        break;

                    case GameManager.ACTION_STEP:
                        mGLView.requestRender();
                        break;


                    default:
                        break;
                }
            }
        });
        }
    };
    /* Private fields*/
    private GLSurfaceView mGLView;
    private GameManager mGameManager;
    private GLRenderer mRenderer;
    private ConnectivityManager mConnectivityManager;
    private TextView mScoreView;
    private TextView mTextView;
    private Button mButton;
    private Button mLeaderBoardButton;
    private Button mBackButton;
    private Button mResumeButton;
    private ImageView mHomeImage;
    private String mLeaderBoardId;
    private boolean highScoreSynced = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gl);

        screenDensity = getDensityDpi();

        mConnectivityManager = setUpConnectivityManager();

        // Get the score guard and activate it
        try {
            ScoreGuard.sharedInstance().init(this.getApplicationContext());
        } catch (Exception ignore) { /* Score guard all ready active. */ }

        // Set the screen to not go to sleep while this window is showing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
         * Find all views
         */
        findViews();

        if (savedInstanceState != null){
            loadFromInstance(savedInstanceState);
        } else {

            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.preference_file_key), Context
                            .MODE_PRIVATE);
            highScoreSynced = sharedPref.getBoolean(
                    getString(R.string.saved_score_sync),
                    true
            );

            mGameManager = setUpGameManager();
            mScoreView.setText("" + mGameManager.getScore());

            // Set the renderer for drawing on the OpenGLSurfaceView
            mRenderer = new GLRenderer(
                    getBitmap(R.raw.dot),
                    getBitmap(R.raw.dot_glare_top_left)
            );
        }

        mRenderer.setGameManager(mGameManager);

        SensorManager sensorManager = getSensorManager();

        try {
            mGameManager.setUpSensor(sensorManager);
        } catch (NoSensorException ignore) { }

        // Load the media player
        SoundPool soundPool = loadSoundPool();
        int blopSoundId = soundPool.load(this, R.raw.blop, 1);
        int jumpSoundId = soundPool.load(this, R.raw.jump, 1);
        mGameManager.setSoundPool(soundPool, blopSoundId, jumpSoundId);

        mGLView = setUpGLView();
        mGoogleApiClient = setUpGoogleApiClient();

        setUpSystemVisibility();

        mLeaderBoardButton = setUpLeaderBoardButton();

        // Start the game in kiosk mode
        mGameManager.startNewGame(true);

    }

    /**
     * Finad and save all fields that will be used in this view.
     */
    private void findViews() {
        mTextView = (TextView) findViewById(R.id.opengl_label);
        mButton = (Button) findViewById(R.id.opengl_button);
        mButton.setOnClickListener(mNewGameHandler);
        mResumeButton = (Button) findViewById(R.id.resume_button);
        mResumeButton.setOnClickListener(mResumeGameHandler);
        mBackButton = (Button) findViewById(R.id.main_menu_button);
        mBackButton.setOnClickListener(mBackButtonHandler);
        mResumeButton.setOnClickListener(mResumeGameHandler);
        mScoreView = (TextView) findViewById(R.id.score_label);
        mLeaderBoardId = getString(R.string.leaderboard_highest_score_id);
        mHomeImage = (ImageView) findViewById(R.id.home_screen_image);
        mHomeImage.setImageResource(R.drawable.united_colors_top_down);
    }

    /**
     * If instance are available, load all instances.
     * @param savedInstanceState
     */
    private void loadFromInstance(Bundle savedInstanceState) {
        mResolvingError = savedInstanceState.getBoolean
                (STATE_RESOLVING_ERROR, false);
        mGoogleApiClient = setUpGoogleApiClient();

        mGameManager = savedInstanceState.getParcelable("mGameManager");

        assert mGameManager != null;

        mGameManager.setListener(mGameWorldListener);

        // Set the renderer for drawing on the OpenGLSurfaceView
        mRenderer = savedInstanceState.getParcelable("mRenderer");

        highScoreSynced = savedInstanceState.getBoolean("highScoreSynced");

        int state = mGameManager.getGameState();
        updateUserInterface(state);
    }

    /**
     * Set up the connectivity manager which is used for monitoring the
     * change int the internet connection.</br>
     * </br>
     * If the build version is more then LOLIPOP, then a method is
     * used for listening on network becoming avaliable.
     *
     * @return a ConnectivityManager-object.
     */
    private ConnectivityManager setUpConnectivityManager() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context
                        .CONNECTIVITY_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.registerNetworkCallback(
                    new NetworkRequest.Builder().build(),
                    new ConnectivityManager.NetworkCallback(){
                        @Override
                        public void onAvailable(Network network) {
                            super.onAvailable(network);

                            connectGooglePlay();
                        }
                    }
            );
        }

        return connectivityManager;
    }

    /**
     * Gets the sensor manager.
     * @return the sensor manager.
     */
    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(Context
        .SENSOR_SERVICE);
    }

    /**
     * Get the dpi of the devices screen, used to caluclate equal size for the
     * dots across different devices.
     * @return
     */
    private int getDensityDpi() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.densityDpi;
    }

    /**
     * Set up the leader board button.
     * @return the new leader board button.
     */
    private Button setUpLeaderBoardButton() {
        Button leaderBoardButton = (Button) findViewById(R.id
                .high_score_button);

        leaderBoardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mGoogleApiClient.isConnected()) {

                    showHighScore();
                } else {
                    mGoogleApiClient.connect();
                }
            }
        });

        return leaderBoardButton;
    }

    /**
     * Using the Google Game Services API, show a popup with information abount
     * rank and so on,
     */
    private void showHighScore() {
        startActivityForResult(Games.Leaderboards
                .getLeaderboardIntent(mGoogleApiClient,
                        mLeaderBoardId), REQUEST_LEADERBOARD);
    }

    /**
     * Make the system show in fullscreen.
     */
    private void setUpSystemVisibility() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /** Set up the google api client
     *
     * @return the new GoogleApiClient.
     */
    private GoogleApiClient setUpGoogleApiClient() {
        GoogleApiClient client = new GoogleApiClient.Builder
                (this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        return client;
    }

    /**
     * Checks to see if the device has internet connectino.
     *
     * @return tru if internet connection exists, false otherwise.
     */
    private boolean hasInternetConnection() {

        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Set up the gl view.
     *
     * @return thw view.
     */
    private GLSurfaceView setUpGLView() {
        GLSurfaceView glView = (GLSurfaceView) findViewById(R.id.opengl_view);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        glView.setRenderer(mRenderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glView.setFocusable(true);

        return glView;
    }

    /**
     * Loads the spund pool, uses {@code new SoundPool(int, int, int)} for
     * older version of the android API.
     *
     * @return the newly created SoundPool-object.
     */
    private SoundPool loadSoundPool() {
        SoundPool soundPool;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes atrib = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(atrib)
                    .build();
        } else {
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);

        }
        return soundPool;
    }

    /**
     * Updates the user interface depending on privded state.
     *
     * @param state in what state the game world is in.
     */
    private void updateUserInterface(int state) {
        switch (state){
            case GameManager.STATE_GAME_OVER:

                // Show restart view
                mTextView.setVisibility(View.VISIBLE);
                mTextView.setText("Score: " + mGameManager.getScore());
                mResumeButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.GONE);
                mButton.setText("RETRY");
                mButton.setVisibility(View.VISIBLE);
                mScoreView.setText("" + mGameManager.getScore());
                mBackButton.setVisibility(View.VISIBLE);
                mHomeImage.setVisibility(View.INVISIBLE);

                int score = mGameManager.getScore();
                int savedScore = ScoreGuard.sharedInstance().getScore();

                // If the new score is bigger then the saved one
                if(score > savedScore)
                    ScoreGuard.sharedInstance().saveScore(score);

                if(mGoogleApiClient.isConnected()){
                    uploadScore(score);
                }else{

                    // Try to upload score next time
                    highScoreSynced = false;
                }
                break;

            case GameManager.STATE_NEW_GAME:
            case GameManager.STATE_KIOSK_MODE:

                // Show start new game view
                mBackButton.setVisibility(View.GONE);
                mTextView.setVisibility(View.INVISIBLE);
                mResumeButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.GONE);
                mButton.setText("START");
                mButton.setVisibility(View.VISIBLE);
                mScoreView.setText("0");
                mScoreView.setVisibility(View.INVISIBLE);
                mLeaderBoardButton.setVisibility(View.VISIBLE);
                mHomeImage.setVisibility(View.VISIBLE);
                break;

            case GameManager.STATE_PAUSED:

                if(!mGameManager.isInKioskMode()){
                    mResumeButton.setVisibility(View.VISIBLE);
                }
                break;

            case GameManager.STATE_RUNNING:

                // Do nothing
                mTextView.setVisibility(View.INVISIBLE);
                mResumeButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.GONE);
                mButton.setText("START");
                mButton.setVisibility(View.INVISIBLE);
                mScoreView.setText("0");
                mScoreView.setVisibility(View.VISIBLE);
                mLeaderBoardButton.setVisibility(View.INVISIBLE);
                mHomeImage.setVisibility(View.INVISIBLE);
                break;

            default:
                break;
        }
    }

    /**
     * Uploads the players score in to Googel Play High Score.
     *
     * @param score the score to upload.
     */
    private void uploadScore(int score) {
        Games.Leaderboards.submitScore(mGoogleApiClient,
            mLeaderBoardId, score);
    }

    /**
     * Fetches bitmaps from provided recource id.
     *
     * @param resourceId the id of the resource.
     * @return a Bitmap.
     */
    private Bitmap getBitmap(int resourceId){

        Bitmap bitmap;
        try (InputStream is = getResources().openRawResource(resourceId)){
            bitmap = BitmapFactory.decodeStream(is);
            return bitmap;
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * Set up the game manager.
     *
     * @return the newly set up game manager.
     */
    private GameManager setUpGameManager() {

        GameManager gameManager = new GameManager();

        gameManager.setListener(mGameWorldListener);

        return gameManager;
    }

    /**
     * Unpauses the gmae.
     */
    @Override
    protected void onResume(){
        super.onResume();
        if(mGameManager.isInKioskMode()){
            mGameManager.unPause();
        }

        connectGooglePlay();
    }

    /**
     * Connects to the google play api.
     */
    private void connectGooglePlay() {

        if(hasInternetConnection() && hasPlayServices()
            && !(mGoogleApiClient.isConnected()
                || mGoogleApiClient.isConnecting())){
            // Connect
            mGoogleApiClient.connect();
        }
    }

    /**
     * Checks to see that the devices has access to the google
     * play service API.
     *
     * @return tru if is has, false otherwise.
     */
    private boolean hasPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        return result == ConnectionResult.SUCCESS;
    }

    /**
     * Pauses the game.
     */
    @Override
    protected void onPause(){
        if (mGameManager.isGameRunning()) {
            mGameManager.pause();
        }

        super.onPause();
    }

    /**
     *  Stop the game.
     */
    @Override
    protected void onStop() {
        // Save status of high score synced
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context
                        .MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.saved_score_sync),
                highScoreSynced);
        editor.commit();

        super.onStop();
    }

    /**
     * Destroy this Game Manager.
     */
    @Override
    protected void onDestroy() {
        mGameManager.destroy();
        super.onDestroy();
    }

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int REQUEST_LEADERBOARD = 1002;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    public void onDialogDismissed() {
        mResolvingError = false;
    }


    public static class ErrorDialogFragment extends DialogFragment {

        public ErrorDialogFragment() { }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((OpenGLActivity) getActivity()).onDialogDismissed();
        }

    }
    /* A fragment to display an error dialog */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {

            if (resultCode == RESULT_OK) {
                mResolvingError = false;
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        } else if(resultCode == GamesActivityResultCodes
                .RESULT_RECONNECT_REQUIRED){
            mGoogleApiClient.disconnect();
        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putParcelable("mGameManager", mGameManager);
        outState.putParcelable("mRenderer", mRenderer);
        outState.putBoolean("highScoreSynced", highScoreSynced);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onConnected(Bundle bundle) {

        if(!highScoreSynced){
            uploadScore(ScoreGuard.sharedInstance().getScore());
            highScoreSynced = true;
        }
    }


    @Override
    public void onConnectionSuspended(int ignore) {}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                if(mGameManager.isGameRunning() && !mGameManager.isInKioskMode()){
                    mGameManager.startNewGame(true);

                    return true;
                }
            default:

                return super.onKeyDown(keyCode, event);

        }
    }
}
