package com.teamFive.chessapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.teamFive.chessapp.R;
import com.teamFive.chessapp.engine.RandomBoardGenerator;

/**
 * Home screen with Play button and expandable Board Settings panel.
 *
 * Settings stored in SharedPreferences so they survive app restarts.
 */
public class MainActivity extends AppCompatActivity {

    // ----------------------------------------------------------------
    //  SharedPreferences keys
    // ----------------------------------------------------------------
    private static final String PREF_NAME = "chess_board_settings";
    private static final String KEY_BASE  = "base_value";
    private static final String KEY_ADV   = "advantage";

    // ----------------------------------------------------------------
    //  Intent extras for GameActivity
    // ----------------------------------------------------------------
    public static final String EXTRA_WHITE_TARGET = "extra_white_target";
    public static final String EXTRA_BLACK_TARGET = "extra_black_target";

    // ----------------------------------------------------------------
    //  Views
    // ----------------------------------------------------------------
    private Button       btnPlay;
    private Button       btnToggleSettings;
    private LinearLayout layoutSettings;
    private SeekBar      seekBase;
    private SeekBar      seekAdvantage;
    private TextView     tvBaseValue;
    private TextView     tvAdvantageDisplay;
    private TextView     tvBoardSummary;

    // ----------------------------------------------------------------
    //  State
    // ----------------------------------------------------------------
    /** Sum of all 15 non-king pieces per side (shared baseline). */
    private int currentBase;

    /**
     * Point advantage given to one side.
     * Positive = White advantage, Negative = Black advantage.
     * White target = base + max(0, advantage)
     * Black target = base + max(0, -advantage)
     */
    private int currentAdvantage;

    // ----------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadPreferences();
        bindViews();
        initSeekBars();
        setupListeners();
        updateSummaryUI();
    }

    // ----------------------------------------------------------------
    //  Setup
    // ----------------------------------------------------------------
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentBase      = prefs.getInt(KEY_BASE, RandomBoardGenerator.DEFAULT_TARGET);
        currentAdvantage = prefs.getInt(KEY_ADV, 0);
    }

    private void savePreferences() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_BASE, currentBase)
                .putInt(KEY_ADV, currentAdvantage)
                .apply();
    }

    private void bindViews() {
        btnPlay            = findViewById(R.id.btnPlay);
        btnToggleSettings  = findViewById(R.id.btnToggleSettings);
        layoutSettings     = findViewById(R.id.layoutSettings);
        seekBase           = findViewById(R.id.seekBase);
        seekAdvantage      = findViewById(R.id.seekAdvantage);
        tvBaseValue        = findViewById(R.id.tvBaseValue);
        tvAdvantageDisplay = findViewById(R.id.tvAdvantageDisplay);
        tvBoardSummary     = findViewById(R.id.tvBoardSummary);
    }

    /**
     * Base SeekBar: progress 0..120 → actual base = 15 + progress.
     * Advantage SeekBar: progress 0..2*maxAdv → actual adv = progress - maxAdv.
     *   maxAdv = 135 - currentBase  (so advantaged side never exceeds 135).
     */
    private void initSeekBars() {
        // Base
        seekBase.setMax(120);
        seekBase.setProgress(currentBase - 15);

        // Advantage — max is derived from base
        int maxAdv = 135 - currentBase;
        seekAdvantage.setMax(2 * maxAdv);
        int advProgress = clampProgress(maxAdv + currentAdvantage, 0, 2 * maxAdv);
        seekAdvantage.setProgress(advProgress);
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> startGame());
        btnToggleSettings.setOnClickListener(v -> toggleSettings());

        seekBase.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBase = 15 + progress;          // range 15..135
                refreshAdvantageSeekBar();
                updateSummaryUI();
                savePreferences();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekAdvantage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int maxAdv = seekAdvantage.getMax() / 2;
                currentAdvantage = progress - maxAdv;
                updateSummaryUI();
                savePreferences();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    // ----------------------------------------------------------------
    //  Advantage SeekBar refresh (called when base changes)
    // ----------------------------------------------------------------
    private void refreshAdvantageSeekBar() {
        int newMaxAdv = 135 - currentBase;

        // Clamp the stored advantage to the new valid range
        currentAdvantage = Math.max(-newMaxAdv, Math.min(newMaxAdv, currentAdvantage));

        // Update SeekBar range & reposition thumb — suppress listener noise by
        // setting max before progress so Android doesn't fire with stale max.
        seekAdvantage.setMax(2 * newMaxAdv);
        seekAdvantage.setProgress(newMaxAdv + currentAdvantage);
    }

    // ----------------------------------------------------------------
    //  UI
    // ----------------------------------------------------------------
    private void toggleSettings() {
        boolean visible = layoutSettings.getVisibility() == View.VISIBLE;
        layoutSettings.setVisibility(visible ? View.GONE : View.VISIBLE);
        btnToggleSettings.setText(visible ? "⚙ Board Settings ▼" : "⚙ Board Settings ▲");
    }

    private void updateSummaryUI() {
        // Base label
        tvBaseValue.setText(currentBase + " pts");

        // Advantage label
        if (currentAdvantage == 0) {
            tvAdvantageDisplay.setText("Balanced");
            tvAdvantageDisplay.setTextColor(0xFF4CAF50);  // green
        } else if (currentAdvantage > 0) {
            tvAdvantageDisplay.setText("White +" + currentAdvantage);
            tvAdvantageDisplay.setTextColor(0xFFFFFFFF);  // white
        } else {
            tvAdvantageDisplay.setText("Black +" + (-currentAdvantage));
            tvAdvantageDisplay.setTextColor(0xFF90CAF9);  // light blue
        }

        // Summary line
        int whiteValue = currentBase + Math.max(0, currentAdvantage);
        int blackValue = currentBase + Math.max(0, -currentAdvantage);
        tvBoardSummary.setText("⬜ White: " + whiteValue + " pts   ⚔   ⬛ Black: " + blackValue + " pts");
    }

    // ----------------------------------------------------------------
    //  Navigation
    // ----------------------------------------------------------------
    private void startGame() {
        int whiteTarget = currentBase + Math.max(0, currentAdvantage);
        int blackTarget = currentBase + Math.max(0, -currentAdvantage);

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(EXTRA_WHITE_TARGET, whiteTarget);
        intent.putExtra(EXTRA_BLACK_TARGET, blackTarget);
        startActivity(intent);
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------
    private static int clampProgress(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
