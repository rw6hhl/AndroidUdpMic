using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Web.Script.Serialization;

namespace W10_MicClient
{
    public partial class MainForm : Form
    {
        private List<PhoneEntry> phones = new List<PhoneEntry>();
        private UdpServer udpServer;
        private readonly string dataFilePath = "phones.json";
        
        private PhoneEntry activePhone = null;
        private AudioPlayer audioPlayer;

        public MainForm()
        {
            InitializeComponent();
            SetupGrid();
            audioPlayer = new AudioPlayer();
            
            // Создаем сервер на порту 5254
            udpServer = new UdpServer(5254);
            udpServer.OnAudioReceived += UdpServer_OnAudioReceived;
            udpServer.OnClientConnected += UdpServer_OnClientConnected;
        }

        private void MainForm_Load(object sender, EventArgs e)
        {
            LoadPhones();
            UpdateGrid();
            
            lblLocalIp.Text = "IP компьютера: " + GetLocalIpAddress();
            
            // Запускаем сервер
            udpServer.Start();
        }

        private void UdpServer_OnAudioReceived(byte[] data)
        {
            if (activePhone != null)
            {
                try
                {
                    if (data.Length > 4)
                    {
                        byte[] audioData = new byte[data.Length - 4];
                        Array.Copy(data, 4, audioData, 0, audioData.Length);
                        audioPlayer.PlayChunk(audioData);
                    }
                }
                catch (Exception ex)
                {
                    Logger.Log("Ошибка воспроизведения: " + ex.Message);
                }

                Invoke(new Action(() =>
                {
                    lblActivity.Text = string.Format("Звук от: {0} ({1} байт)", activePhone.Id, data.Length);
                }));
            }
        }

        private void UdpServer_OnClientConnected(string message, IPEndPoint ep)
        {
            Invoke(new Action(() =>
            {
                lblStatus.Text = "Статус: Клиент подключен (" + ep.Address + ")";
            }));
        }

        private string GetLocalIpAddress()
        {
            try
            {
                var host = Dns.GetHostEntry(Dns.GetHostName());
                foreach (var ip in host.AddressList)
                {
                    if (ip.AddressFamily == AddressFamily.InterNetwork)
                        return ip.ToString();
                }
            }
            catch { }
            return "Не удалось определить";
        }

        private void btnStartListen_Click(object sender, EventArgs e)
        {
            if (phones.Count == 0) { MessageBox.Show("Добавьте телефон."); return; }
            if (dgvPhones.SelectedRows.Count == 0) { MessageBox.Show("Выберите телефон."); return; }
            
            int index = dgvPhones.SelectedRows[0].Index;
            activePhone = phones[index];

            btnStartListen.Enabled = false;
            btnStopListen.Enabled = true;
            lblStatus.Text = "Статус: Отправка команды YES...";
            Logger.Log("Начало прослушивания для " + activePhone.Id);

            // Отправляем команду через сервер
            udpServer.SendCommand("yes");
        }

        private void btnStopListen_Click(object sender, EventArgs e)
        {
            StopListening();
        }

        private void StopListening()
        {
            btnStartListen.Enabled = true;
            btnStopListen.Enabled = false;
            lblStatus.Text = "Статус: Остановлено";
            lblActivity.Text = "";
            
            udpServer.SendCommand("no");
            Logger.Log("Прослушивание остановлено.");
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            udpServer.Stop();
            SavePhones();
        }

        // ... остальные методы (LoadPhones, SavePhones, btnAdd, btnEdit и т.д.) остаются без изменений
        private void SetupGrid()
        {
            dgvPhones.AutoGenerateColumns = false;
            dgvPhones.Columns.Clear();
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "Id", HeaderText = "ID", DataPropertyName = "Id", Width = 50 });
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "Ip", HeaderText = "IP-адрес телефона", DataPropertyName = "Ip", Width = 120 });
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "CmdPort", HeaderText = "Порт команд", DataPropertyName = "CmdPort", Width = 80 });
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "AudioPort", HeaderText = "Порт звука", DataPropertyName = "AudioPort", Width = 80 });
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "ClientId", HeaderText = "Client ID", DataPropertyName = "ClientId", Width = 80 });
            dgvPhones.Columns.Add(new DataGridViewCheckBoxColumn { Name = "IsActive", HeaderText = "Активен", DataPropertyName = "IsActive", Width = 60 });
            dgvPhones.Columns.Add(new DataGridViewTextBoxColumn { Name = "OnlineStatus", HeaderText = "На связи", DataPropertyName = "OnlineStatus", Width = 80 });
        }
        private void LoadPhones()
        {
            if (File.Exists(dataFilePath))
            {
                try { phones = new JavaScriptSerializer().Deserialize<List<PhoneEntry>>(File.ReadAllText(dataFilePath)) ?? new List<PhoneEntry>(); }
                catch (Exception ex) { MessageBox.Show("Ошибка загрузки: " + ex.Message); }
            }
        }
        private void SavePhones()
        {
            try { File.WriteAllText(dataFilePath, new JavaScriptSerializer().Serialize(phones)); }
            catch (Exception ex) { MessageBox.Show("Ошибка сохранения: " + ex.Message); }
        }
        private void UpdateGrid()
        {
            dgvPhones.Rows.Clear();
            foreach (var phone in phones) dgvPhones.Rows.Add(phone.Id, phone.Ip, phone.CmdPort, phone.AudioPort, phone.ClientId, phone.IsActive, phone.OnlineStatus);
        }
        private void btnAdd_Click(object sender, EventArgs e)
        {
            using (var form = new AddPhoneForm())
            {
                if (form.ShowDialog() == DialogResult.OK) { phones.Add(form.NewPhone); UpdateGrid(); }
            }
        }
        private void btnEdit_Click(object sender, EventArgs e)
        {
            if (dgvPhones.SelectedRows.Count > 0)
            {
                int index = dgvPhones.SelectedRows[0].Index;
                if (index >= 0 && index < phones.Count)
                {
                    var selected = phones[index];
                    using (var form = new AddPhoneForm(selected))
                    {
                        if (form.ShowDialog() == DialogResult.OK)
                        {
                            selected.Id = form.NewPhone.Id; selected.Ip = form.NewPhone.Ip; selected.CmdPort = form.NewPhone.CmdPort;
                            selected.AudioPort = form.NewPhone.AudioPort; selected.ClientId = form.NewPhone.ClientId; UpdateGrid();
                        }
                    }
                }
            }
        }
        private void btnDelete_Click(object sender, EventArgs e)
        {
            if (dgvPhones.SelectedRows.Count > 0)
            {
                int index = dgvPhones.SelectedRows[0].Index;
                if (index >= 0 && index < phones.Count) { phones.RemoveAt(index); UpdateGrid(); }
            }
        }
        private void btnLogs_Click(object sender, EventArgs e) { using (var logForm = new LogForm()) logForm.ShowDialog(); }
        private void btnHelp_Click(object sender, EventArgs e) { using (var helpForm = new HelpForm()) helpForm.ShowDialog(); }
    }
}