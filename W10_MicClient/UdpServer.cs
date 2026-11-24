using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace W10_MicClient
{
    public class UdpServer
    {
        private UdpClient server;
        private CancellationTokenSource cts;
        private IPEndPoint lastClientEndPoint;
        private readonly int port;

        public event Action<byte[]> OnAudioReceived;
        public event Action<string, IPEndPoint> OnClientConnected;

        public UdpServer(int port)
        {
            this.port = port;
        }

        public void Start()
        {
            cts = new CancellationTokenSource();
            server = new UdpClient(port);
            Logger.Log("UdpServer: Сервер запущен на порту " + port);

            Task.Run(() => ReceiveLoop(cts.Token));
        }

        public void Stop()
        {
            if (cts != null) cts.Cancel();
            if (server != null) server.Close();
            Logger.Log("UdpServer: Сервер остановлен");
        }

        public void SendCommand(string command)
        {
            if (lastClientEndPoint == null)
            {
                Logger.Log("UdpServer: Нет подключенного клиента для отправки команды.");
                return;
            }

            try
            {
                byte[] bytes = Encoding.ASCII.GetBytes(command);
                server.Send(bytes, bytes.Length, lastClientEndPoint);
                Logger.Log($"UdpServer: Отправлена команда '{command}' клиенту {lastClientEndPoint}");
            }
            catch (Exception ex)
            {
                Logger.Log("UdpServer: Ошибка отправки команды: " + ex.Message);
            }
        }

        private async Task ReceiveLoop(CancellationToken token)
        {
            try
            {
                while (!token.IsCancellationRequested)
                {
                    var result = await server.ReceiveAsync();
                    byte[] data = result.Buffer;
                    IPEndPoint remoteEP = result.RemoteEndPoint;

                    // Если это пинг от клиента (текст "PING")
                    if (data.Length == 4 && Encoding.ASCII.GetString(data) == "PING")
                    {
                        if (lastClientEndPoint == null || !lastClientEndPoint.Equals(remoteEP))
                        {
                            lastClientEndPoint = remoteEP;
                            Logger.Log("UdpServer: Новый клиент подключился: " + remoteEP);
                            OnClientConnected?.Invoke("Клиент подключен", remoteEP);
                        }
                        continue; // Пинг не передаем в аудио
                    }

                    // Если это аудио-пакет (начинается с команды 21 и длина > 4)
                    if (data.Length > 4)
                    {
                        // Передаем пакет в MainForm для воспроизведения
                        OnAudioReceived?.Invoke(data);
                    }
                }
            }
            catch (ObjectDisposedException) { }
            catch (Exception ex)
            {
                Logger.Log("UdpServer: Ошибка приема: " + ex.Message);
            }
        }
    }
}