package com.teamFive.chessapp.timer;

import android.os.CountDownTimer;

import com.teamFive.chessapp.model.PlayerColor;

/**
 * Quản lý 2 đồng hồ đếm ngược cho Trắng và Đen.
 * Chỉ chạy đồng hồ của người đang có lượt.
 */
public class ChessTimerManager {

    // ----------------------------------------------------------------
    //  Callback
    // ----------------------------------------------------------------
    public interface TimerCallback {
        /** Gọi mỗi giây: millisLeft là thời gian còn lại của người đang đi */
        void onTick(PlayerColor color, long millisLeft);
        /** Gọi khi hết giờ */
        void onTimeOut(PlayerColor color);
    }

    // ----------------------------------------------------------------
    //  Fields
    // ----------------------------------------------------------------
    private final TimerCallback callback;

    private long whiteMillisLeft;
    private long blackMillisLeft;

    private PlayerColor activeColor = PlayerColor.WHITE;
    private CountDownTimer activeTimer;
    private boolean isPaused = false;

    private static final long TICK_MS = 1000L;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public ChessTimerManager(long initialMillis, TimerCallback callback) {
        this.whiteMillisLeft = initialMillis;
        this.blackMillisLeft = initialMillis;
        this.callback        = callback;
    }

    // ----------------------------------------------------------------
    //  Public API
    // ----------------------------------------------------------------

    /** Bắt đầu đồng hồ cho lượt đầu tiên (Trắng đi trước) */
    public void start() {
        startTimer(PlayerColor.WHITE);
    }

    /** Chuyển lượt: dừng đồng hồ hiện tại, khởi động đồng hồ cho phe kia */
    public void switchTurn(PlayerColor newActiveColor) {
        cancelActive();
        activeColor = newActiveColor;
        if (!isPaused) {
            startTimer(activeColor);
        }
    }

    /** Tạm dừng (khi app vào background hoặc menu) */
    public void pause() {
        isPaused = true;
        cancelActive();
    }

    /** Tiếp tục */
    public void resume() {
        isPaused = false;
        startTimer(activeColor);
    }

    /** Dừng hoàn toàn */
    public void stop() {
        cancelActive();
    }

    /** Lấy thời gian còn lại (millis) */
    public long getMillisLeft(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? whiteMillisLeft : blackMillisLeft;
    }

    /** Format millis → "MM:SS" */
    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes      = totalSeconds / 60;
        long seconds      = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ----------------------------------------------------------------
    //  Internal
    // ----------------------------------------------------------------
    private void startTimer(PlayerColor color) {
        long millisLeft = (color == PlayerColor.WHITE) ? whiteMillisLeft : blackMillisLeft;

        activeTimer = new CountDownTimer(millisLeft, TICK_MS) {

            @Override
            public void onTick(long millisUntilFinished) {
                if (color == PlayerColor.WHITE) {
                    whiteMillisLeft = millisUntilFinished;
                } else {
                    blackMillisLeft = millisUntilFinished;
                }
                if (callback != null) {
                    callback.onTick(color, millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                if (color == PlayerColor.WHITE) {
                    whiteMillisLeft = 0;
                } else {
                    blackMillisLeft = 0;
                }
                if (callback != null) {
                    callback.onTimeOut(color);
                }
            }
        }.start();
    }

    private void cancelActive() {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
    }
}
