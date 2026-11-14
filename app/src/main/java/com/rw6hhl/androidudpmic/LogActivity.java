package com.rw6hhl.androidudpmic;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LogActivity extends AppCompatActivity implements AppLog.LogListener {

    private TextView tvLogText;
    private ScrollView svLogs;
    private final StringBuilder logHistory = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        tvLogText = findViewById(R.id.tvLogText);
        svLogs = findViewById(R.id.svLogs);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnSendLog = findViewById(R.id.btnSendLog);

        btnBack.setOnClickListener(v -> finish());
        btnSendLog.setOnClickListener(v -> {
            String currentLog = logHistory.toString();
            if (currentLog.isEmpty()) {
                AppLog.add("LogActivity: Лог пуст, отправлять нечего");
                return;
            }
            LogExporter.exportAndSend(this, currentLog);
        });
        
        AppLog.setListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLog.setListener(null);
    }

    @Override
    public void onNewLog(String message) {
        runOnUiThread(() -> {
            logHistory.append(message).append("\n");
            tvLogText.setText(logHistory.toString());
            svLogs.post(() -> svLogs.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}