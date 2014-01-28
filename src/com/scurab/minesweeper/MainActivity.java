package com.scurab.minesweeper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * User: jbruchanov
 * Date: 25/11/13
 * Time: 22:18
 */
public class MainActivity extends Activity implements PlayArea.OnFinishGameListener {

    private PlayArea mPlayArea;
    private Button mNewGame;
    private ImageButton mValidation;
    private ToggleButton mCheat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        bind();
    }

    private void init() {
        mPlayArea = (PlayArea) findViewById(R.id.play_area);
        mNewGame = (Button) findViewById(R.id.new_game);
        mValidation = (ImageButton) findViewById(R.id.validate);
        mCheat = (ToggleButton) findViewById(R.id.cheat);
    }

    private void bind() {
        mPlayArea.setOnFinishGameListener(this);

        mNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNewGameClick();
            }
        });
        mCheat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheatClick(((ToggleButton) view).isChecked());
            }
        });

        mValidation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onValidate();
            }
        });
    }

    public void onValidate() {
        mPlayArea.finishGame();
    }

    public void onNewGameClick() {
        mPlayArea.startNewGame();
        mCheat.setChecked(false);
        mValidation.setImageResource(R.drawable.smile_happy);
    }

    public void onCheatClick(boolean show) {
        mPlayArea.showCheat(show);
    }

    @Override
    public void onFinishGame(boolean success) {
        if (success) {
            Toast.makeText(this, R.string.finish_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.finish_unsuccess, Toast.LENGTH_LONG).show();
            mValidation.setImageResource(R.drawable.smile_sad);
        }
    }
}
