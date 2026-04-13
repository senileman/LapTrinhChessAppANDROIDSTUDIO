package com.teamFive.chessapp.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.teamFive.chessapp.R;
import com.teamFive.chessapp.engine.BoardManager;
import com.teamFive.chessapp.engine.CheckDetector;
import com.teamFive.chessapp.engine.GameEngine;
import com.teamFive.chessapp.engine.HintEngine;
import com.teamFive.chessapp.engine.RandomBoardGenerator;
import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;
import com.teamFive.chessapp.timer.ChessTimerManager;
import com.teamFive.chessapp.ui.adapters.ChessBoardAdapter;
import com.teamFive.chessapp.ui.dialogs.GameResultDialog;

public class GameActivity extends AppCompatActivity {

    private static final long INITIAL_TIME_MS = 10 * 60 * 1000L;
    private static final long AI_DELAY_MS     = 600L;

    // ----------------------------------------------------------------
    //  Targets read once from Intent and reused across rematches
    // ----------------------------------------------------------------
    private int intentWhiteTarget;
    private int intentBlackTarget;

    private GameEngine        gameEngine;
    private ChessTimerManager timerManager;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());

    private View         rootLayout;
    private LinearLayout layoutPlayerBlack;
    private LinearLayout layoutControls;

    private ImageView imgBlackAvatar, imgBlackTurnIndicator;
    private TextView  tvPlayerBlackName, tvBlackStatus, tvTimerBlack;
    private TextView  tvMatchStatus;
    private GridView  chessBoard;
    private ChessBoardAdapter boardAdapter;
    private ImageView imgWhiteAvatar, imgWhiteTurnIndicator;
    private TextView  tvPlayerWhiteName, tvWhiteStatus, tvTimerWhite;
    private Button    btnUndoMove, btnHint, btnResign;

    private boolean isGameOver    = false;
    private boolean isAiThinking  = false;
    private boolean isHintRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_game);

        // Read targets from Intent (fall back to default if missing)
        intentWhiteTarget = getIntent().getIntExtra(
                MainActivity.EXTRA_WHITE_TARGET, RandomBoardGenerator.DEFAULT_TARGET);
        intentBlackTarget = getIntent().getIntExtra(
                MainActivity.EXTRA_BLACK_TARGET, RandomBoardGenerator.DEFAULT_TARGET);

        bindViews();
        applyWindowInsets();
        initGame();
    }

    @Override protected void onPause()   { super.onPause();   if (!isGameOver && timerManager != null) timerManager.pause(); }
    @Override protected void onResume()  { super.onResume();  if (!isGameOver && timerManager != null) timerManager.resume(); }
    @Override protected void onDestroy() { super.onDestroy(); if (timerManager != null) timerManager.stop(); mainHandler.removeCallbacksAndMessages(null); }

    private void bindViews() {
        rootLayout            = findViewById(R.id.rootLayout);
        layoutPlayerBlack     = findViewById(R.id.layoutPlayerBlack);
        layoutControls        = findViewById(R.id.layoutControls);
        imgBlackAvatar        = findViewById(R.id.imgBlackAvatar);
        imgBlackTurnIndicator = findViewById(R.id.imgBlackTurnIndicator);
        tvPlayerBlackName     = findViewById(R.id.tvPlayerBlackName);
        tvBlackStatus         = findViewById(R.id.tvBlackStatus);
        tvTimerBlack          = findViewById(R.id.tvTimerBlack);
        tvMatchStatus         = findViewById(R.id.tvMatchStatus);
        chessBoard            = findViewById(R.id.chessBoard);
        imgWhiteAvatar        = findViewById(R.id.imgWhiteAvatar);
        imgWhiteTurnIndicator = findViewById(R.id.imgWhiteTurnIndicator);
        tvPlayerWhiteName     = findViewById(R.id.tvPlayerWhiteName);
        tvWhiteStatus         = findViewById(R.id.tvWhiteStatus);
        tvTimerWhite          = findViewById(R.id.tvTimerWhite);
        btnUndoMove           = findViewById(R.id.btnUndoMove);
        btnHint               = findViewById(R.id.btnHint);
        btnResign             = findViewById(R.id.btnResign);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout()
            );
            layoutPlayerBlack.setPadding(
                    layoutPlayerBlack.getPaddingLeft(),
                    layoutPlayerBlack.getPaddingTop() + insets.top,
                    layoutPlayerBlack.getPaddingRight(),
                    layoutPlayerBlack.getPaddingBottom()
            );
            layoutControls.setPadding(
                    layoutControls.getPaddingLeft(),
                    layoutControls.getPaddingTop(),
                    layoutControls.getPaddingRight(),
                    layoutControls.getPaddingBottom() + insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ----------------------------------------------------------------
    //  Game initialisation — uses the Intent targets every time
    // ----------------------------------------------------------------
    private void initGame() {
        isGameOver    = false;
        isAiThinking  = false;
        isHintRunning = false;
        mainHandler.removeCallbacksAndMessages(null);

        tvPlayerWhiteName.setText("Bạn (Trắng)");
        tvPlayerBlackName.setText("Máy (Đen)");

        // Create engine with the targets passed from MainActivity
        gameEngine = new GameEngine(intentWhiteTarget, intentBlackTarget);

        boardAdapter = new ChessBoardAdapter(this, gameEngine.getBoard(), gameEngine.getTurn());
        boardAdapter.setOnMoveSelectedListener(this::onHumanMoveSelected);
        chessBoard.setAdapter(boardAdapter);

        if (timerManager != null) timerManager.stop();
        timerManager = new ChessTimerManager(INITIAL_TIME_MS, new ChessTimerManager.TimerCallback() {
            @Override public void onTick(PlayerColor color, long millisLeft) { runOnUiThread(() -> updateTimerDisplay(color, millisLeft)); }
            @Override public void onTimeOut(PlayerColor color)               { runOnUiThread(() -> handleTimeOut(color)); }
        });
        timerManager.start();

        String t = ChessTimerManager.formatTime(INITIAL_TIME_MS);
        tvTimerWhite.setText(t);
        tvTimerBlack.setText(t);

        btnUndoMove.setEnabled(false);
        btnHint.setText("💡 Gợi ý");
        btnHint.setEnabled(true);
        btnHint.setOnClickListener(v -> showHint());
        btnResign.setOnClickListener(v -> handleResign());

        refreshUI();
    }

    private void onHumanMoveSelected(Position from, Position to) {
        if (isGameOver || isAiThinking || isHintRunning) return;
        if (gameEngine.getTurn() != gameEngine.getHumanColor()) return;

        boolean success = gameEngine.makeMove(new Move(from, to));
        if (!success) { Toast.makeText(this, "Nước đi không hợp lệ!", Toast.LENGTH_SHORT).show(); return; }

        btnUndoMove.setEnabled(false);
        timerManager.switchTurn(gameEngine.getAiColor());
        refreshUI();

        if (gameEngine.isGameOver()) { endGame(GameResultDialog.ResultType.CHECKMATE, "Bạn"); return; }
        scheduleAiMove();
    }

    private void scheduleAiMove() {
        isAiThinking = true;
        boardAdapter.clearSelection();
        tvMatchStatus.setText("🤖 Máy đang suy nghĩ...");
        tvMatchStatus.setTextColor(0xFF90CAF9);

        mainHandler.postDelayed(() -> {
            if (isGameOver) { isAiThinking = false; return; }
            Move aiMove = gameEngine.makeAiMove();
            isAiThinking = false;
            if (aiMove == null) { endGame(GameResultDialog.ResultType.CHECKMATE, "Bạn"); return; }
            timerManager.switchTurn(gameEngine.getHumanColor());
            flashAiMove(aiMove);
            refreshUI();
            if (gameEngine.isGameOver()) endGame(GameResultDialog.ResultType.CHECKMATE, "Máy");
        }, AI_DELAY_MS);
    }

    private void flashAiMove(Move move) {
        int flashIndex = move.to.row * 8 + move.to.col;
        boardAdapter.update(gameEngine.getBoard(), gameEngine.getTurn(), flashIndex);
        mainHandler.postDelayed(() -> refreshUI(), 300);
    }

    private void showHint() {
        if (isAiThinking || isGameOver || isHintRunning) return;
        if (gameEngine.getTurn() != gameEngine.getHumanColor()) return;

        isHintRunning = true;
        btnHint.setEnabled(false);
        btnHint.setText("💡 Đang tính...");

        final BoardManager snap = deepCopyBoard(gameEngine.getBoard());
        final PlayerColor color = gameEngine.getHumanColor();

        new Thread(() -> {
            Move hint = HintEngine.getBestMove(snap, color);
            mainHandler.post(() -> {
                isHintRunning = false;
                btnHint.setEnabled(true);
                btnHint.setText("💡 Gợi ý");
                if (hint == null) { Toast.makeText(this, "Không có nước đi!", Toast.LENGTH_SHORT).show(); return; }
                boardAdapter.showHint(hint.from, hint.to);
                Toast.makeText(this, "💡 Gợi ý: " + positionToAlgebraic(hint.from) + " → " + positionToAlgebraic(hint.to), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private BoardManager deepCopyBoard(BoardManager orig) {
        BoardManager copy = new BoardManager();
        copy.board = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = orig.board[r][c];
                if (p != null) copy.board[r][c] = new Piece(p.type, p.color);
            }
        return copy;
    }

    private String positionToAlgebraic(Position pos) { return "" + (char)('a' + pos.col) + (8 - pos.row); }

    private void refreshUI() {
        PlayerColor turn   = gameEngine.getTurn();
        boolean     myTurn = (turn == gameEngine.getHumanColor());
        int         checkIdx = findCheckKingIndex();
        boardAdapter.update(gameEngine.getBoard(), turn, checkIdx);

        if (!isAiThinking) {
            if (CheckDetector.isKingInCheck(gameEngine.getBoard(), turn)) {
                tvMatchStatus.setText("⚠ " + (myTurn ? "Vua của bạn" : "Vua máy") + " đang bị Chiếu!");
                tvMatchStatus.setTextColor(0xFFFF5252);
                pulseView(tvMatchStatus);
            } else {
                tvMatchStatus.setText(myTurn ? "⚔ Lượt của bạn" : "🤖 Lượt của máy");
                tvMatchStatus.setTextColor(0xFFFFD700);
            }
        }
        updatePlayerBars(myTurn);
    }

    private void updatePlayerBars(boolean isHumanTurn) {
        if (isHumanTurn) {
            imgWhiteAvatar.setBackgroundResource(R.drawable.avatar_bg_active);
            imgWhiteTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_active);
            tvWhiteStatus.setText("Đang đi"); tvWhiteStatus.setTextColor(0xFF4CAF50);
            tvTimerWhite.setBackgroundResource(R.drawable.timer_bg_active); tvTimerWhite.setTextColor(0xFF1A1A2E);
            imgBlackAvatar.setBackgroundResource(R.drawable.avatar_bg);
            imgBlackTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_inactive);
            tvBlackStatus.setText("Đang chờ"); tvBlackStatus.setTextColor(0xFF8899AA);
            tvTimerBlack.setBackgroundResource(R.drawable.timer_bg_inactive); tvTimerBlack.setTextColor(0xFFFFFFFF);
        } else {
            imgBlackAvatar.setBackgroundResource(R.drawable.avatar_bg_active);
            imgBlackTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_active);
            tvBlackStatus.setText("Đang suy nghĩ..."); tvBlackStatus.setTextColor(0xFF90CAF9);
            tvTimerBlack.setBackgroundResource(R.drawable.timer_bg_active); tvTimerBlack.setTextColor(0xFF1A1A2E);
            imgWhiteAvatar.setBackgroundResource(R.drawable.avatar_bg);
            imgWhiteTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_inactive);
            tvWhiteStatus.setText("Đang chờ"); tvWhiteStatus.setTextColor(0xFF8899AA);
            tvTimerWhite.setBackgroundResource(R.drawable.timer_bg_inactive); tvTimerWhite.setTextColor(0xFFFFFFFF);
        }
    }

    private void updateTimerDisplay(PlayerColor color, long millisLeft) {
        String fmt = ChessTimerManager.formatTime(millisLeft);
        if (color == PlayerColor.WHITE) { tvTimerWhite.setText(fmt); if (millisLeft <= 30_000) tvTimerWhite.setTextColor(0xFFFF5252); }
        else                            { tvTimerBlack.setText(fmt); if (millisLeft <= 30_000) tvTimerBlack.setTextColor(0xFFFF5252); }
    }

    private int findCheckKingIndex() {
        PlayerColor turn = gameEngine.getTurn();
        if (!CheckDetector.isKingInCheck(gameEngine.getBoard(), turn)) return -1;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = gameEngine.getBoard().board[r][c];
            if (p != null && p.type == PieceType.KING && p.color == turn) return r * 8 + c;
        }
        return -1;
    }

    private void handleResign()                    { if (!isGameOver) endGame(GameResultDialog.ResultType.RESIGN, "Máy"); }
    private void handleTimeOut(PlayerColor loser)  { if (!isGameOver) endGame(GameResultDialog.ResultType.TIME_OUT, loser == PlayerColor.WHITE ? "Máy" : "Bạn"); }

    private void endGame(GameResultDialog.ResultType type, String winnerName) {
        isGameOver = false; isAiThinking = false; isHintRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        if (timerManager != null) timerManager.stop();
        isGameOver = true;
        GameResultDialog dialog = new GameResultDialog(this, type, winnerName);
        dialog.setOnResultActionListener(new GameResultDialog.OnResultActionListener() {
            @Override public void onPlayAgain() { initGame(); }
            @Override public void onMainMenu()  { finish(); }
        });
        dialog.show();
    }

    private void pulseView(View v) { ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.08f, 1f).setDuration(300).start(); }
}
