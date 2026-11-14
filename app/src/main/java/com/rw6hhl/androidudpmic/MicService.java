package com.rw6hhl.androidudpmic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class MicService extends Service {

    private static final String CHANNEL_ID = "mic_service_channel";
    private AudioRecorder audioRecorder;
    private UdpNetwork udpNetwork;
    private SettingsManager settings;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        settings = new SettingsManager(this);
        AppLog.add("MicService: onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UDP Микрофон")
                .setContentText("Ожидание команды по сети...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        AppLog.add("MicService: Служба запущена в foreground");
        sendStatusBroadcast("Ожидание команд");

        udpNetwork = new UdpNetwork(
                settings.getCmdPort(),
                settings.getTargetIp(),
                settings.getTargetPort(),
                settings.getClientId()
        );

        audioRecorder = new AudioRecorder(udpNetwork);

        udpNetwork.startListening(new UdpNetwork.CommandListener() {
            @Override
            public void onStartCommand() {
                AppLog.add("MicService: Получена команда YES, запуск записи");
                sendStatusBroadcast("Команда получена. Отправка звука начата.");
                audioRecorder.startRecording();
            }

            @Override
            public void onStopCommand() {
                AppLog.add("MicService: Получена команда NO, остановка записи");
                sendStatusBroadcast("Команда получена. Отправка звука остановлена.");
                audioRecorder.stopRecording();
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLog.add("MicService: onDestroy, служба остановлена");
        if (udpNetwork != null) {
            udpNetwork.stopListening();
        }
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "UDP Mic Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendStatusBroadcast(String statusText) {
        Intent intent = new Intent("MIC_SERVICE_STATUS");
        intent.putExtra("status_text", statusText);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }
}