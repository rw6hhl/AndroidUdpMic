package com.rw6hhl.androidudpmic;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etCmdPort, etTargetIp, etTargetPort, etClientId;
    private TextView tvStatus, tvVersion;
    private SettingsManager settings;

    // Приемник статусов от службы
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status_text");
            if (status != null) {
                tvStatus.setText("Статус: " + status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new SettingsManager(this);

        etCmdPort = findViewById(R.id.etCmdPort);
        etTargetIp = findViewById(R.id.etTargetIp);
        etTargetPort = findViewById(R.id.etTargetPort);
        etClientId = findViewById(R.id.etClientId);
        tvStatus = findViewById(R.id.tvStatus);
        tvVersion = findViewById(R.id.tvVersion);

        // Устанавливаем версию из BuildConfig
        tvVersion.setText("Версия: " + BuildConfig.VERSION_NAME);

        // Загрузка сохраненных настроек
        etCmdPort.setText(String.valueOf(settings.getCmdPort()));
        etTargetIp.setText(settings.getTargetIp());
        etTargetPort.setText(String.valueOf(settings.getTargetPort()));
        etClientId.setText(String.valueOf(settings.getClientId()));

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveSettings());

        Button btnStartService = findViewById(R.id.btnStartService);
        btnStartService.setOnClickListener(v -> checkPermissionsAndStartService());

        Button btnLogs = findViewById(R.id.btnLogs);
        btnLogs.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Регистрируем приемник статусов
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, new IntentFilter("MIC_SERVICE_STATUS"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, new IntentFilter("MIC_SERVICE_STATUS"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(statusReceiver);
    }

    private void saveSettings() {
        try {
            settings.setCmdPort(Integer.parseInt(etCmdPort.getText().toString()));
            settings.setTargetIp(etTargetIp.getText().toString());
            settings.setTargetPort(Integer.parseInt(etTargetPort.getText().toString()));
            settings.setClientId(Integer.parseInt(etClientId.getText().toString()));
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
            AppLog.add("MainActivity: Настройки сохранены");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка: проверьте числовые поля", Toast.LENGTH_SHORT).show();
            AppLog.add("MainActivity: Ошибка сохранения настроек");
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        }
    }

    private void checkPermissionsAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет разрешения на микрофон!", Toast.LENGTH_LONG).show();
            checkPermissions();
            return;
        }
        
        Intent intent = new Intent(this, MicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        tvStatus.setText("Статус: Служба запущена. Ожидание команд.");
        Toast.makeText(this, "Служба запущена в фоне", Toast.LENGTH_SHORT).show();
        AppLog.add("MainActivity: Служба запущена пользователем");
    }
}