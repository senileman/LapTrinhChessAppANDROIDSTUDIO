package com.teamFive.chessapp.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.teamFive.chessapp.R;
import com.teamFive.chessapp.engine.CheckDetector;
import com.teamFive.chessapp.engine.GameEngine;
import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;
import com.teamFive.chessapp.timer.ChessTimerManager;
import com.teamFive.chessapp.ui.adapters.ChessBoardAdapter;
import com.teamFive.chessapp.ui.dialogs.GameResultDialog;

public class GameActivity extends AppCompatActivity {

    // ----------------------------------------------------------------
    //  Hằng số
    // ----------------------------------------------------------------
    private static final long INITIAL_TIME_MS = 10 * 60 * 1000L; // 10 phút
    private static final long AI_DELAY_MS     = 600L;             // AI "suy nghĩ" 0.6 giây

    // ----------------------------------------------------------------
    //  Engine & Timer
    // ----------------------------------------------------------------
    private GameEngine        gameEngine;
    private ChessTimerManager timerManager;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());

    // ----------------------------------------------------------------
    //  UI
    // ----------------------------------------------------------------
    private ImageView imgBlackAvatar, imgBlackTurnIndicator;
    private TextView  tvPlayerBlackName, tvBlackStatus, tvTimerBlack;
    private TextView  tvMatchStatus;
    private GridView  chessBoard;
    private ChessBoardAdapter boardAdapter;
    private ImageView imgWhiteAvatar, imgWhiteTurnIndicator;
    private TextView  tvPlayerWhiteName, tvWhiteStatus, tvTimerWhite;
    private Button    btnUndoMove, btnHint, btnResign;

    // ----------------------------------------------------------------
    //  Trạng thái
    // ----------------------------------------------------------------
    private boolean isGameOver    = false;
    private boolean isAiThinking  = false;

    // ================================================================
    //  Lifecycle
    // ================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        bindViews();
        initGame();
    }

    @Override protected void onPause()   { super.onPause();   if (!isGameOver && timerManager != null) timerManager.pause(); }
    @Override protected void onResume()  { super.onResume();  if (!isGameOver && timerManager != null) timerManager.resume(); }
    @Override protected void onDestroy() { super.onDestroy(); if (timerManager != null) timerManager.stop(); mainHandler.removeCallbacksAndMessages(null); }

    // ================================================================
    //  Bind views
    // ================================================================
    private void bindViews() {
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

    // ================================================================
    //  Khởi tạo / Reset game
    // ================================================================
    private void initGame() {
        isGameOver   = false;
        isAiThinking = false;
        mainHandler.removeCallbacksAndMessages(null);

        // Tên người chơi
        tvPlayerWhiteName.setText("Bạn (Trắng)");
        tvPlayerBlackName.setText("Máy (Đen)");

        // Engine
        gameEngine = new GameEngine();

        // Adapter
        boardAdapter = new ChessBoardAdapter(
                this,
                gameEngine.getBoard(),
                gameEngine.getTurn()
        );
        boardAdapter.setOnMoveSelectedListener(this::onHumanMoveSelected);
        chessBoard.setAdapter(boardAdapter);

        // Timer
        if (timerManager != null) timerManager.stop();
        timerManager = new ChessTimerManager(INITIAL_TIME_MS, new ChessTimerManager.TimerCallback() {
            @Override public void onTick(PlayerColor color, long millisLeft) {
                runOnUiThread(() -> updateTimerDisplay(color, millisLeft));
            }
            @Override public void onTimeOut(PlayerColor color) {
                runOnUiThread(() -> handleTimeOut(color));
            }
        });
        timerManager.start();

        // Reset timer display
        String timeStr = ChessTimerManager.formatTime(INITIAL_TIME_MS);
        tvTimerWhite.setText(timeStr);
        tvTimerBlack.setText(timeStr);

        // Controls
        btnUndoMove.setEnabled(false);
        btnHint.setOnClickListener(v -> showHint());
        btnResign.setOnClickListener(v -> handleResign());

        refreshUI();
    }

    // ================================================================
    //  Người chơi chọn nước đi
    // ================================================================
    private void onHumanMoveSelected(Position from, Position to) {
        if (isGameOver || isAiThinking) return;
        if (gameEngine.getTurn() != gameEngine.getHumanColor()) return;

        boolean success = gameEngine.makeMove(new Move(from, to));
        if (!success) {
            Toast.makeText(this, "Nước đi không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUndoMove.setEnabled(false); // Vô hiệu Undo khi AI chưa đi

        // Chuyển timer sang AI
        timerManager.switchTurn(gameEngine.getAiColor());

        refreshUI();

        // Kiểm tra kết thúc ngay sau khi người đi
        if (gameEngine.isGameOver()) {
            endGame(GameResultDialog.ResultType.CHECKMATE, "Bạn");
            return;
        }

        // AI suy nghĩ
        scheduleAiMove();
    }

    // ================================================================
    //  AI thực hiện nước đi (có delay để cảm giác tự nhiên)
    // ================================================================
    private void scheduleAiMove() {
        isAiThinking = true;
        boardAdapter.clearSelection(); // Khoá board khi AI đang đi

        tvMatchStatus.setText("🤖 Máy đang suy nghĩ...");
        tvMatchStatus.setTextColor(0xFF90CAF9);

        mainHandler.postDelayed(() -> {
            if (isGameOver) { isAiThinking = false; return; }

            Move aiMove = gameEngine.makeAiMove();
            isAiThinking = false;

            if (aiMove == null) {
                // AI không có nước → người thắng
                endGame(GameResultDialog.ResultType.CHECKMATE, "Bạn");
                return;
            }

            // Chuyển timer về người
            timerManager.switchTurn(gameEngine.getHumanColor());

            // Flash ô AI vừa đi
            flashAiMove(aiMove);

            refreshUI();

            if (gameEngine.isGameOver()) {
                endGame(GameResultDialog.ResultType.CHECKMATE, "Máy");
            }

        }, AI_DELAY_MS);
    }

    /** Highlight ngắn ô AI vừa đi để người chơi dễ nhận ra */
    private void flashAiMove(Move move) {
        // Cập nhật adapter với vị trí đích của AI (dùng checkKingIndex tạm thời để flash)
        // → Sẽ bị clear ngay sau refreshUI(), chỉ tồn tại ~300ms
        int flashIndex = move.to.row * 8 + move.to.col;
        boardAdapter.update(gameEngine.getBoard(), gameEngine.getTurn(), flashIndex);
        mainHandler.postDelayed(() -> refreshUI(), 300);
    }

    // ================================================================
    //  Cập nhật UI
    // ================================================================
    private void refreshUI() {
        PlayerColor turn     = gameEngine.getTurn();
        boolean     myTurn   = (turn == gameEngine.getHumanColor());
        int         checkIdx = findCheckKingIndex();

        boardAdapter.update(gameEngine.getBoard(), turn, checkIdx);

        // Match status bar
        if (!isAiThinking) {
            if (CheckDetector.isKingInCheck(gameEngine.getBoard(), turn)) {
                String who = myTurn ? "Vua của bạn" : "Vua máy";
                tvMatchStatus.setText("⚠ " + who + " đang bị Chiếu!");
                tvMatchStatus.setTextColor(0xFFFF5252);
                pulseView(tvMatchStatus);
            } else {
                tvMatchStatus.setText(myTurn ? "⚔ Lượt của bạn" : "🤖 Lượt của máy");
                tvMatchStatus.setTextColor(0xFFFFD700);
            }
        }

        // Player bars
        updatePlayerBars(myTurn);
    }

    private void updatePlayerBars(boolean isHumanTurn) {
        if (isHumanTurn) {
            // Trắng (người) active
            imgWhiteAvatar.setBackgroundResource(R.drawable.avatar_bg_active);
            imgWhiteTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_active);
            tvWhiteStatus.setText("Đang đi");
            tvWhiteStatus.setTextColor(0xFF4CAF50);
            tvTimerWhite.setBackgroundResource(R.drawable.timer_bg_active);
            tvTimerWhite.setTextColor(0xFF1A1A2E);

            imgBlackAvatar.setBackgroundResource(R.drawable.avatar_bg);
            imgBlackTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_inactive);
            tvBlackStatus.setText("Đang chờ");
            tvBlackStatus.setTextColor(0xFF8899AA);
            tvTimerBlack.setBackgroundResource(R.drawable.timer_bg_inactive);
            tvTimerBlack.setTextColor(0xFFFFFFFF);
        } else {
            // Đen (AI) active
            imgBlackAvatar.setBackgroundResource(R.drawable.avatar_bg_active);
            imgBlackTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_active);
            tvBlackStatus.setText("Đang suy nghĩ...");
            tvBlackStatus.setTextColor(0xFF90CAF9);
            tvTimerBlack.setBackgroundResource(R.drawable.timer_bg_active);
            tvTimerBlack.setTextColor(0xFF1A1A2E);

            imgWhiteAvatar.setBackgroundResource(R.drawable.avatar_bg);
            imgWhiteTurnIndicator.setBackgroundResource(R.drawable.turn_indicator_inactive);
            tvWhiteStatus.setText("Đang chờ");
            tvWhiteStatus.setTextColor(0xFF8899AA);
            tvTimerWhite.setBackgroundResource(R.drawable.timer_bg_inactive);
            tvTimerWhite.setTextColor(0xFFFFFFFF);
        }
    }

    private void updateTimerDisplay(PlayerColor color, long millisLeft) {
        String fmt = ChessTimerManager.formatTime(millisLeft);
        if (color == PlayerColor.WHITE) {
            tvTimerWhite.setText(fmt);
            if (millisLeft <= 30_000) tvTimerWhite.setTextColor(0xFFFF5252);
        } else {
            tvTimerBlack.setText(fmt);
            if (millisLeft <= 30_000) tvTimerBlack.setTextColor(0xFFFF5252);
        }
    }

    private int findCheckKingIndex() {
        PlayerColor turn = gameEngine.getTurn();
        if (!CheckDetector.isKingInCheck(gameEngine.getBoard(), turn)) return -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = gameEngine.getBoard().board[r][c];
                if (p != null && p.type == PieceType.KING && p.color == turn) {
                    return r * 8 + c;
                }
            }
        }
        return -1;
    }

    // ================================================================
    //  Gợi ý
    // ================================================================
    private void showHint() {
        if (isAiThinking || isGameOver) return;
        Toast.makeText(this, "💡 Tính năng gợi ý đang phát triển!", Toast.LENGTH_SHORT).show();
    }

    // ================================================================
    //  Đầu hàng
    // ================================================================
    private void handleResign() {
        if (isGameOver) return;
        endGame(GameResultDialog.ResultType.RESIGN, "Máy");
    }

    // ================================================================
    //  Hết giờ
    // ================================================================
    private void handleTimeOut(PlayerColor loser) {
        if (isGameOver) return;
        String winner = (loser == PlayerColor.WHITE) ? "Máy" : "Bạn";
        endGame(GameResultDialog.ResultType.TIME_OUT, winner);
    }

    // ================================================================
    //  Kết thúc game
    // ================================================================
    private void endGame(GameResultDialog.ResultType type, String winnerName) {
        isGameOver   = false; // reset trước
        isAiThinking = false;
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

    // ================================================================
    //  Animation
    // ================================================================
    private void pulseView(View v) {
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.08f, 1f).setDuration(300).start();
    }
}
