package com.rw6hhl.androidudpmic;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "udp_mic_prefs";
    private static final String KEY_CMD_PORT = "cmd_port";
    private static final String KEY_TARGET_IP = "target_ip";
    private static final String KEY_TARGET_PORT = "target_port";
    private static final String KEY_CLIENT_ID = "client_id";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getCmdPort() {
        return prefs.getInt(KEY_CMD_PORT, 44444);
    }

    public void setCmdPort(int port) {
        prefs.edit().putInt(KEY_CMD_PORT, port).apply();
    }

    public String getTargetIp() {
        return prefs.getString(KEY_TARGET_IP, "127.0.0.1");
    }

    public void setTargetIp(String ip) {
        prefs.edit().putString(KEY_TARGET_IP, ip).apply();
    }

    public int getTargetPort() {
        return prefs.getInt(KEY_TARGET_PORT, 55555);
    }

    public void setTargetPort(int port) {
        prefs.edit().putInt(KEY_TARGET_PORT, port).apply();
    }

    public int getClientId() {
        return prefs.getInt(KEY_CLIENT_ID, 10001);
    }

    public void setClientId(int id) {
        prefs.edit().putInt(KEY_CLIENT_ID, id).apply();
    }
}