package com.rw6hhl.androidudpmic;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLog {
    private static final String TAG = "AndroidUdpMic";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    
    // Интерфейс для передачи логов в UI
    public interface LogListener {
        void onNewLog(String message);
    }
    
    private static LogListener listener;

    public static void setListener(LogListener l) {
        listener = l;
    }

    public static void add(String message) {
        String time = timeFormat.format(new Date());
        String logLine = time + " " + message;
        Log.d(TAG, logLine);
        
        if (listener != null) {
            listener.onNewLog(logLine);
        }
    }
}