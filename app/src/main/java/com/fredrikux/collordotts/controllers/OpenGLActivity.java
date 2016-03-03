package com.fredrikux.collordotts.controllers;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fredrikux.collordotts.R;
import com.fredrikux.collordotts.models.GameManager;
import com.fredrikux.collordotts.opengl.GLSurfaceView;

public class OpenGLActivity extends Activity {

    private GLSurfaceView mGLView;
    private TextView mScoreView;
    private Button mButton;
    private GameManager mGameManager;
    private TextView mTextView;

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Hide text and button
            mTextView.setVisibility(View.INVISIBLE);
            mButton.setVisibility(View.INVISIBLE);
            mButton.setOnClickListener(retryClick);

            // Start the game
            mGameManager.startGame();

        }
    };

    private View.OnClickListener retryClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            startActivity(getIntent());
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gl);

        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        mGameManager = setUpGameManager();
        mGLView = (GLSurfaceView) findViewById(R.id.opengl_view);
        mGLView.setGameManager(mGameManager);

        mScoreView = (TextView) findViewById(R.id.score_label);
        mScoreView.setText("" + mGameManager.getScore());

        mButton = (Button) findViewById(R.id.opengl_button);
        mButton.setOnClickListener(startClick);

        mTextView = (TextView) findViewById(R.id.opengl_label);


    }

    private GameManager setUpGameManager() {

        SensorManager sensorManager
                = (SensorManager) getSystemService(Context
                .SENSOR_SERVICE);

        GameManager gameManager = new GameManager(sensorManager) {

            @Override
            public void onActionPerformed(final ActionEvent event) {

                // Update the UI from the UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    switch (event.action){
                        case GameManager.ACTION_SCORE_CHANGED:

                            int score = (int) event.source;
                            mScoreView.setText("" + score);
                            break;
                        case GameManager.ACTION_GAME_OVER:

                            mTextView.setText("Wanna go again?");
                            mTextView.setVisibility(View.VISIBLE);

                            mButton.setText("Retry");
                            mButton.setVisibility(View.VISIBLE);


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
        mGLView.onResume();
        mGameManager.onResume();
        super.onResume();
    }

    @Override
    protected void onPause(){
        mGLView.onPause();
        mGameManager.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mGameManager.onStop();
        mGLView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
