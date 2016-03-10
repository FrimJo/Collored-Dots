package com.fredrikux.collordotts.controllers;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fredrikux.collordotts.R;
import com.fredrikux.collordotts.models.GameManager;
import com.fredrikux.collordotts.opengl.GLRenderer;
import com.fredrikux.collordotts.opengl.GLSurfaceView;
import com.fredrikux.collordotts.utils.IActionListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class OpenGLActivity
        extends
            Activity
        implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

    private GLSurfaceView mGLView;
    private TextView mScoreView;
    private Button mButton;
    private GameManager mGameManager;
    private TextView mTextView;
    private Button mLeaderBoardButton;

    private View.OnClickListener mNewGameHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Start the game
            mGameManager.startNewGame(false);

        }
    };

    private View.OnClickListener mResumeGameHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Start the game
            mGameManager.unPause();

        }
    };

    final IActionListener mGameWorldListener = new IActionListener() {
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

    private String mLeaderBoardId;
    private GLRenderer mRenderer;
    private Button mResumeButton;
    private MediaPlayer mMediaPlayer;
    public static int screenDensity;
    private SoundPool mSoundPool;
    private AudioManager mAudioManager;
    private HashMap mSoundPoolMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gl);


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        screenDensity = metrics.densityDpi;

        mTextView = (TextView) findViewById(R.id.opengl_label);
        mButton = (Button) findViewById(R.id.opengl_button);
        mButton.setOnClickListener(mNewGameHandler);
        mResumeButton = (Button) findViewById(R.id.resume_button);
        mResumeButton.setOnClickListener(mResumeGameHandler);
        mScoreView = (TextView) findViewById(R.id.score_label);

        if (savedInstanceState != null){
            mResolvingError = savedInstanceState.getBoolean
                    (STATE_RESOLVING_ERROR, false);
            mGoogleApiClient = (GoogleApiClient) savedInstanceState.get
                    ("mGoogleApiClient");

            mGameManager = savedInstanceState.getParcelable("mGameManager");
            mGameManager.setListener(mGameWorldListener);

            mTextView.setText(savedInstanceState.getString("mTextViewStr"));
            mTextView.setVisibility(savedInstanceState.getInt
                    ("mTextViewVis") == 0 ? View.VISIBLE : View.INVISIBLE);
            mButton.setVisibility(savedInstanceState.getInt
                    ("mButtonVis") == 0 ? View.VISIBLE : View.INVISIBLE);
            mButton.setText(savedInstanceState.getString("mButtonStr"));

            mScoreView.setText(savedInstanceState.getString("mScoreViewStr"));

            // Set the renderer for drawing on the OpenGLSurfaceView
            mRenderer = savedInstanceState.getParcelable("mRenderer");

            int state = mGameManager.getGameState();
            updateUserInterface(state);
        } else {

            mGameManager = setUpGameManager();
            mScoreView.setText("" + mGameManager.getScore());

            // Set the renderer for drawing on the OpenGLSurfaceView
            mRenderer = new GLRenderer(getBitmap(R.raw.white_point));
            mRenderer.setGameManager(mGameManager);

        }


        SensorManager sensorManager
                = (SensorManager) getSystemService(Context
                .SENSOR_SERVICE);
        mGameManager.setSensorManager(sensorManager);


        // Load the media player
        SoundPool soundPool = loadSoundPool();
        int blopSoundId = soundPool.load(this, R.raw.blop, 1);
        int jumpSoundId = soundPool.load(this, R.raw.jump, 1);
        mGameManager.setSoundPool(soundPool, blopSoundId, jumpSoundId);

        mGLView = setUpGLView();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        mGoogleApiClient.connect();


        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);



        mLeaderBoardId = getString(R.string.leaderboard_highest_score_id);

        mLeaderBoardButton = (Button) findViewById(R.id.high_score_button);
        mLeaderBoardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mGoogleApiClient.isConnected()){

                    startActivityForResult(Games.Leaderboards
                            .getLeaderboardIntent(mGoogleApiClient,
                                    mLeaderBoardId), REQUEST_LEADERBOARD);
                } else {
                    mGoogleApiClient.connect();
                }
            }
        });

        // Start the game in kiosk mode
        mGameManager.startNewGame(true);

    }

    private GLSurfaceView setUpGLView() {
        GLSurfaceView glView = (GLSurfaceView) findViewById(R.id.opengl_view);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        glView.setRenderer(mRenderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glView.setFocusable(true);

        return glView;
    }

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

    private void updateUserInterface(int state) {
        switch (state){
            case GameManager.STATE_GAME_OVER:

                // Show restart view
                mTextView.setVisibility(View.VISIBLE);
                mTextView.setText("Score: " + mGameManager.getScore());
                mResumeButton.setVisibility(View.GONE);
                mButton.setText("RETRY");
                mButton.setVisibility(View.VISIBLE);
                mScoreView.setText("" + mGameManager.getScore());
                mLeaderBoardButton.setVisibility(View.VISIBLE);

                if(mGoogleApiClient.isConnected()){
                    Games.Leaderboards.submitScore(mGoogleApiClient,
                        mLeaderBoardId, mGameManager.getScore());
                }
                break;

            case GameManager.STATE_NEW_GAME:
            case GameManager.STATE_NOT_STARTED:

                // Show start new game view
                mTextView.setVisibility(View.INVISIBLE);
                mResumeButton.setVisibility(View.GONE);
                mButton.setText("START");
                mButton.setVisibility(View.VISIBLE);
                mScoreView.setText("0");
                mScoreView.setVisibility(View.INVISIBLE);
                mLeaderBoardButton.setVisibility(View.VISIBLE);
                break;

            case GameManager.STATE_PAUSED:

                mResumeButton.setVisibility(View.VISIBLE);
                break;

            case GameManager.STATE_RUNNING:

                // Do nothing
                mTextView.setVisibility(View.INVISIBLE);
                mResumeButton.setVisibility(View.GONE);
                mButton.setText("START");
                mButton.setVisibility(View.INVISIBLE);
                mScoreView.setText("0");
                mScoreView.setVisibility(View.VISIBLE);
                mLeaderBoardButton.setVisibility(View.INVISIBLE);
                break;

            default:
                break;
        }
    }

    private Bitmap getBitmap(int resourceId){

        Bitmap bitmap;
        try (InputStream is = getResources().openRawResource(resourceId)){
            bitmap = BitmapFactory.decodeStream(is);
            return bitmap;
        } catch (IOException ignored) {}

        return null;
    }

    private GameManager setUpGameManager() {

        GameManager gameManager = new GameManager();

        gameManager.setListener(mGameWorldListener);

        return gameManager;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_open_gl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(mGameManager.isInKioskMode()){
            mGameManager.unPause();
        }
        /*if (mGameManager.isGameRunning() && mGameManager.isGamePaused()) {
            mGameManager.unPause();
        }*/
    }

    @Override
    protected void onPause(){
        if (mGameManager.isGameRunning()) {
            mGameManager.pause();
        }
        super.onPause();
    }

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
            mResolvingError = false;
            if (resultCode == RESULT_OK) {

                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        } else if (requestCode == REQUEST_LEADERBOARD) {

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putParcelable("mGameManager", mGameManager);
        outState.putString("mTextViewStr", mTextView.getText().toString());
        outState.putInt("mTextViewVis", mButton.getVisibility());
        outState.putInt("mButtonVis", mButton.getVisibility());
        outState.putString("mButtonStr", mButton.getText().toString());
        outState.putString("mScoreViewStr", mScoreView.getText().toString());
        outState.putParcelable("mRenderer", mRenderer);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("onConnectionSuspended");
    }
}
