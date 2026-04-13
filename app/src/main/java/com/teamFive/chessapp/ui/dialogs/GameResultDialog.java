package com.teamFive.chessapp.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.teamFive.chessapp.R;

public class GameResultDialog extends Dialog {

    // ----------------------------------------------------------------
    //  Dữ liệu
    // ----------------------------------------------------------------
    public enum ResultType {
        CHECKMATE,   // Chiếu bí
        RESIGN,      // Đầu hàng
        TIME_OUT,    // Hết giờ
        DRAW         // Hoà
    }

    private final ResultType resultType;
    private final String     winnerName;   // null nếu hoà

    // ----------------------------------------------------------------
    //  Callback
    // ----------------------------------------------------------------
    public interface OnResultActionListener {
        void onPlayAgain();
        void onMainMenu();
    }

    private OnResultActionListener actionListener;

    public void setOnResultActionListener(OnResultActionListener l) {
        this.actionListener = l;
    }

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public GameResultDialog(Context context, ResultType resultType, String winnerName) {
        super(context);
        this.resultType = resultType;
        this.winnerName = winnerName;
    }

    // ----------------------------------------------------------------
    //  onCreate
    // ----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_game_result);

        // Bo tròn góc dialog
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        setCancelable(false); // Không tắt bằng cách bấm ngoài

        TextView tvEmoji  = findViewById(R.id.tvResultEmoji);
        TextView tvTitle  = findViewById(R.id.tvResultTitle);
        TextView tvDetail = findViewById(R.id.tvResultDetail);
        Button   btnAgain = findViewById(R.id.btnPlayAgain);
        Button   btnMenu  = findViewById(R.id.btnMainMenu);

        // --- Nội dung theo loại kết quả ---
        switch (resultType) {
            case CHECKMATE:
                tvEmoji.setText("🏆");
                tvTitle.setText("Chiếu bí!");
                tvDetail.setText(winnerName + " chiến thắng\nbằng nước Chiếu bí!");
                break;

            case RESIGN:
                tvEmoji.setText("🏳");
                tvTitle.setText("Đầu hàng!");
                tvDetail.setText(winnerName + " chiến thắng\ndo đối thủ đầu hàng.");
                break;

            case TIME_OUT:
                tvEmoji.setText("⏰");
                tvTitle.setText("Hết giờ!");
                tvDetail.setText(winnerName + " chiến thắng\ndo đối thủ hết thời gian.");
                break;

            case DRAW:
                tvEmoji.setText("🤝");
                tvTitle.setText("Hoà cờ!");
                tvDetail.setText("Hai bên không ai có nước đi.\nKết quả: Hoà!");
                break;
        }

        // --- Nút Chơi lại ---
        btnAgain.setOnClickListener(v -> {
            dismiss();
            if (actionListener != null) actionListener.onPlayAgain();
        });

        // --- Nút Màn hình chính ---
        btnMenu.setOnClickListener(v -> {
            dismiss();
            if (actionListener != null) actionListener.onMainMenu();
        });
    }
}
