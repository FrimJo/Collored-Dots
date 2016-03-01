package com.fredrikux.collordotts;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fredrikux.collordotts.models.GameManager;
import com.fredrikux.collordotts.opengl.OpenGLSurfaceView;
import com.fredrikux.collordotts.utils.IActionListener;

import java.io.IOException;
import java.io.InputStream;

public class OpenGLActivity extends Activity {

    private OpenGLSurfaceView mGLView;
    private TextView mScoreView;
    private Button mRetryButton;
    private GameManager mGameManager;
    private TextView mGameOverView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gl);

        mGameManager = setUpGameManager();
        mGLView = (OpenGLSurfaceView) findViewById(R.id.opengl_view);
        mGLView.setGameManager(mGameManager);

        mScoreView = (TextView) findViewById(R.id.score_label);
        mScoreView.setText("" + mGameManager.getScore());

        mRetryButton = (Button) findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retry the game
                mGameManager.restart();

                // Reset the score text
                mScoreView.setText("0");

                // Hide text and button
                mGameOverView.setVisibility(View.INVISIBLE);
                mRetryButton.setVisibility(View.INVISIBLE);
            }
        });

        mGameOverView = (TextView) findViewById(R.id.Game_over_label);

        mGameManager.startGame();
    }

    private GameManager setUpGameManager() {

        GameManager gameManager = new GameManager() {

            @Override
            public void onActionPerformed(final ActionEvent event) {

                // Update the UI from the UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (event.action){
                            case GameManager.SCORE_CHANGED:

                                int score = (int) event.source;
                                mScoreView.setText("" + score);
                                break;
                            case GameManager.GAME_OVER:

                                mGameOverView.setVisibility(View.VISIBLE);
                                mRetryButton.setVisibility(View.VISIBLE);

                            case GameManager.STEP:

                                mGLView.step();
                                break;

                            case GameManager.RESTART:
                                mGLView.restart();
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
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

}
