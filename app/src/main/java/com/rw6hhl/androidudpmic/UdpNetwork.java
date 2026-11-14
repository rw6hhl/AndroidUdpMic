package com.rw6hhl.androidudpmic;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UdpNetwork {

    private static final String TAG = "UdpNetwork";

    private final int cmdPort;
    private final String targetIp;
    private final int targetPort;
    private final int clientId;

    private DatagramSocket cmdSocket;
    private DatagramSocket sendSocket;
    private InetAddress targetAddress;
    private Thread listenThread;
    private boolean isListening = false;
    private CommandListener listener;

    public interface CommandListener {
        void onStartCommand();
        void onStopCommand();
    }

    public UdpNetwork(int cmdPort, String targetIp, int targetPort, int clientId) {
        this.cmdPort = cmdPort;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.clientId = clientId;
    }

    public void startListening(CommandListener listener) {
        this.listener = listener;
        isListening = true;

        try {
            cmdSocket = new DatagramSocket(cmdPort);
            sendSocket = new DatagramSocket();
            targetAddress = InetAddress.getByName(targetIp);
            AppLog.add("UdpNetwork: Ожидание команд на порту " + cmdPort + ", передача на " + targetIp + ":" + targetPort);
        } catch (SocketException | UnknownHostException e) {
            AppLog.add("UdpNetwork: ОШИБКА создания сокетов: " + e.getMessage());
            return;
        }

        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buf = new byte[1024];
                while (isListening) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        cmdSocket.receive(packet);
                        String cmd = new String(packet.getData(), 0, packet.getLength()).trim().toLowerCase();
                        String ip = packet.getAddress().getHostAddress();
                        
                        AppLog.add("UdpNetwork: Получена команда '" + cmd + "' от " + ip);
                        
                        if (cmd.equals("yes")) {
                            if (listener != null) listener.onStartCommand();
                        } else if (cmd.equals("no")) {
                            if (listener != null) listener.onStopCommand();
                        }
                    } catch (IOException e) {
                        if (isListening) {
                            AppLog.add("UdpNetwork: Ошибка приема команды: " + e.getMessage());
                        }
                    }
                }
            }
        });
        listenThread.start();
    }

    public void stopListening() {
        isListening = false;
        if (cmdSocket != null && !cmdSocket.isClosed()) {
            cmdSocket.close();
        }
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (listenThread != null) {
            listenThread.interrupt();
        }
        AppLog.add("UdpNetwork: Служба остановлена");
    }

    public void sendAudio(byte[] audioData, int length) {
        if (targetAddress == null || sendSocket == null || sendSocket.isClosed()) return;

        try {
            ByteBuffer buffer = ByteBuffer.allocate(4 + length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put((byte) 21); // command WAV_16000_16
            buffer.put((byte) 0);  // kanal
            buffer.putShort((short) clientId); // client
            
            buffer.put(audioData, 0, length);

            DatagramPacket packet = new DatagramPacket(
                    buffer.array(), buffer.position(), targetAddress, targetPort);
            sendSocket.send(packet);
        } catch (IOException e) {
            AppLog.add("UdpNetwork: Ошибка отправки аудио: " + e.getMessage());
        }
    }
}