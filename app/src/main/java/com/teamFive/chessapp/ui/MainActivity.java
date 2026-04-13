package com.teamFive.chessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.teamFive.chessapp.R;

/**
 * Màn hình chính – hiện tại chỉ có nút "Chơi ngay" để vào GameActivity.
 * Bạn có thể mở rộng thêm: chọn chế độ chơi, xem lịch sử, cài đặt…
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPlay = findViewById(R.id.btnPlay);
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                Intent intent = new Intent(this, GameActivity.class);
                startActivity(intent);
            });
        }
    }
}
