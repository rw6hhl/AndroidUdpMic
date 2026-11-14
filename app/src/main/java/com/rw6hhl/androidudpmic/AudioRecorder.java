package com.rw6hhl.androidudpmic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
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
            AppLog.add("AudioRecorder: ОШИБКА инициализации AudioRecord");
            return;
        }

        isRecording = true;
        audioRecord.startRecording();
        AppLog.add("AudioRecorder: Запись началась");

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[BUFFER_SIZE_BYTES];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        udpNetwork.sendAudio(buffer, read);
                    }
                }
            }
        });
        recordingThread.start();
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        AppLog.add("AudioRecorder: Запись остановлена");

        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException e) {
                AppLog.add("AudioRecorder: Ошибка остановки: " + e.getMessage());
            }
            audioRecord.release();
            audioRecord = null;
        }

        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                AppLog.add("AudioRecorder: Ошибка ожидания потока: " + e.getMessage());
            }
            recordingThread = null;
        }
    }
}