package com.rw6hhl.androidudpmic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecorder {

    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // 320 сэмплов * 2 байта = 640 байт (как в pmr.c)
    private static final int BUFFER_SIZE_BYTES = 640;

    private final UdpNetwork udpNetwork;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;

    public AudioRecorder(UdpNetwork udpNetwork) {
        this.udpNetwork = udpNetwork;
    }

    public void startRecording() {
        if (isRecording) return;

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBufferSize < BUFFER_SIZE_BYTES) {
            minBufferSize = BUFFER_SIZE_BYTES * 2;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Ошибка инициализации AudioRecord");
            return;
        }

        isRecording = true;
        audioRecord.startRecording();

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[BUFFER_SIZE_BYTES];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        // Передаем 640 байт чистого звука
                        udpNetwork.sendAudio(buffer, read);
                    }
                }
            }
        });
        recordingThread.start();
        Log.d(TAG, "Запись началась");
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Ошибка остановки записи: " + e.getMessage());
            }
            audioRecord.release();
            audioRecord = null;
        }

        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Ошибка ожидания потока: " + e.getMessage());
            }
            recordingThread = null;
        }
        Log.d(TAG, "Запись остановлена");
    }
}