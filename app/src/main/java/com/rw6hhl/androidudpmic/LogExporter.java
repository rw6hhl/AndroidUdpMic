package com.rw6hhl.androidudpmic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogExporter {

    public static void exportAndSend(Context context, String logText) {
        try {
            // Имя файла: версия_номер_дата.txt
            String version = "1.1"; // Обновите при смене версии
            String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = version + "_" + date + ".txt";

            // Сохраняем файл во временную папку кэша
            File cacheDir = new File(context.getCacheDir(), "logs");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File logFile = new File(cacheDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(logFile)) {
                fos.write(logText.getBytes());
            }

            // Получаем Uri через FileProvider для безопасной передачи
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", logFile);

            // Формируем Intent для отправки файла
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Лог AndroidUdpMic v" + version);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Во вложении лог работы программы (версия " + version + ").");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Запускаем выбор мессенджера
            context.startActivity(Intent.createChooser(sendIntent, "Отправить лог через..."));
            AppLog.add("LogExporter: Лог сохранен в " + fileName + " и отправлен в мессенджер");

        } catch (IOException e) {
            AppLog.add("LogExporter: Ошибка создания файла лога: " + e.getMessage());
        } catch (Exception e) {
            AppLog.add("LogExporter: Ошибка отправки: " + e.getMessage());
        }
    }
}